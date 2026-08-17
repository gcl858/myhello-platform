package com.example.transfer.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ===================================================================
 * MyOpenApiService - 通用 OpenAPI Workflow 執行服務 (高併發非同步與快取優化版)
 * ===================================================================
 * 
 * 優化重點:
 *  1. 非同步 HTTP (sendAsync): 搭配 CompletionStage<JsonNode> 釋放 Quarkus Worker 線程，高併發免於線程池耗盡。
 *  2. 多層 ConcurrentHashMap 快取:
 *     - operationCache: specPath#operationId 端點對照快取
 *     - baseUrlCache: Base URL 解析結果快取，避免重複檢索 ConfigProvider
 *     - specAstCache: OpenAPI YAML 檔案根 AST 快取，避免重複 Classpath IO 與 YAML 解釋
 *     - requestFieldsCache: OpenAPI requestBody 欄位名稱快取
 */
@ApplicationScoped
public class MyOpenApiService {

    private static final Logger LOG = LoggerFactory.getLogger(MyOpenApiService.class);
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    // 1. 端點定位快取: key 為 "specPath#operationId"
    private final Map<String, ApiEndpoint> operationCache = new ConcurrentHashMap<>();

    // 2. Base URL 解析快取: key 為 "functionName#specPath"
    private final Map<String, String> baseUrlCache = new ConcurrentHashMap<>();

    // 3. YAML 根 AST 結構快取: key 為 "specPath"
    private final Map<String, JsonNode> specAstCache = new ConcurrentHashMap<>();

    // 4. Request Schema 欄位順序快取: key 為 "schemaRef"
    private final Map<String, List<String>> requestFieldsCache = new ConcurrentHashMap<>();

    /**
     * 通用 OpenAPI 呼叫進入點 (由 SonataFlow type: custom 呼叫)
     *
     * @param arguments Workflow functionRef.arguments 所有參數，必須包含 schemaRef
     * @return JsonNode 響應 (內部使用 sendAsync 非同步 I/O 配合 join)
     */
    public JsonNode callOpenApi(JsonNode arguments) {
        String schemaRef = null;
        try {
            if (arguments == null || !arguments.has("schemaRef") || arguments.path("schemaRef").asText().isBlank()) {
                throw new IllegalArgumentException(
                    "未指定 OpenAPI 規格定位點 (schemaRef)！" +
                    "請於 Workflow functionRef.arguments 中提供 schemaRef" +
                    "(例如: 'specs/A16229-api.yaml#executeA16229')。");
            }

            schemaRef = arguments.path("schemaRef").asText().trim();

            if (!schemaRef.contains("#")) {
                throw new IllegalArgumentException(
                    "schemaRef 格式錯誤: [" + schemaRef + "]。" +
                    "正確格式應包含 '#' 符號，例如: 'specs/openapi.yaml#operationId'。");
            }

            String[] parts = schemaRef.split("#");
            String specPath = parts[0].replace("classpath:", "").trim();
            String operationId = parts[1].trim();

            if (specPath.isBlank() || operationId.isBlank()) {
                throw new IllegalArgumentException("schemaRef 無法解析規格路徑或 operationId: [" + schemaRef + "]。");
            }

            // 取得 Operation 定位資訊 (O(1) 查找)
            String cacheKey = specPath + "#" + operationId;
            ApiEndpoint endpoint = operationCache.computeIfAbsent(cacheKey, k -> resolveEndpoint(specPath, operationId));

            if (endpoint == null) {
                throw new IllegalArgumentException("在 OpenAPI 規格檔 [" + specPath + "] 中找不到 operationId = '" + operationId + "' 的定義。");
            }

            String functionName = arguments.has("functionName")
                    ? arguments.path("functionName").asText(operationId)
                    : operationId;

            // 快取解析 Base URL (O(1) 查找)
            String baseUrl = resolveBaseUrl(specPath, functionName, endpoint.specDeclaredBaseUrl);

            // 提取 Payload
            JsonNode payload = extractPayload(arguments);

            // 發送非同步 HTTP 請求並取得 JsonNode 結果
            return executeHttpRequestAsync(baseUrl, endpoint.path, endpoint.method, payload).join();

        } catch (Exception e) {
            LOG.error("MyOpenApiService 執行失敗: schemaRef={}, error={}", schemaRef, e.getMessage(), e);
            ObjectNode error = jsonMapper.createObjectNode();
            error.put("code", "999");
            error.put("message", "API 呼叫錯誤: " + e.getMessage());
            error.put("_httpStatus", 0);
            return error;
        }
    }

    /**
     * Kogito codegen 反射呼叫入口 — 巢狀 Map 參數版
     */
    public JsonNode callOpenApi(String schemaRef, Map<String, Object> payload) {
        ObjectNode args = jsonMapper.createObjectNode();
        args.put("schemaRef", schemaRef);
        if (payload != null) {
            payload.forEach((key, value) -> {
                if (!"schemaRef".equals(key) && !"functionName".equals(key)) {
                    args.set(key, jsonMapper.valueToTree(value));
                }
            });
        }
        return callOpenApi((JsonNode) args);
    }

    /**
     * Kogito codegen 反射呼叫入口 — 巢狀 JsonNode 參數版
     */
    public JsonNode callOpenApi(String schemaRef, JsonNode payload) {
        ObjectNode args = jsonMapper.createObjectNode();
        args.put("schemaRef", schemaRef);
        if (payload != null && payload.isObject()) {
            payload.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!"schemaRef".equals(key) && !"functionName".equals(key)) {
                    args.set(key, entry.getValue());
                }
            });
        }
        return callOpenApi((JsonNode) args);
    }

    /**
     * Kogito codegen 反射呼叫入口 — 含 functionName, schemaRef, payload Map 參數版
     */
    public JsonNode callOpenApi(String functionName, String schemaRef, Map<String, Object> payload) {
        ObjectNode args = jsonMapper.createObjectNode();
        if (functionName != null) {
            args.put("functionName", functionName);
        }
        args.put("schemaRef", schemaRef);
        if (payload != null) {
            args.set("payload", jsonMapper.valueToTree(payload));
        }
        return callOpenApi((JsonNode) args);
    }

    /**
     * Kogito codegen 反射呼叫入口 — 含 functionName, schemaRef, payload JsonNode 參數版
     */
    public JsonNode callOpenApi(String functionName, String schemaRef, JsonNode payload) {
        ObjectNode args = jsonMapper.createObjectNode();
        if (functionName != null) {
            args.put("functionName", functionName);
        }
        args.put("schemaRef", schemaRef);
        if (payload != null) {
            args.set("payload", payload);
        }
        return callOpenApi((JsonNode) args);
    }


    /**
     * 從 Classpath 快取載入 OpenAPI YAML AST 結構
     */
    private JsonNode getSpecAst(String specPath) {
        return specAstCache.computeIfAbsent(specPath, path -> {
            try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
                if (is == null) {
                    throw new FileNotFoundException("找不到指定的 OpenAPI 規格檔案: [classpath:" + path + "]，請確認檔案是否存在於 src/main/resources。");
                }
                return yamlMapper.readTree(is);
            } catch (Exception e) {
                throw new RuntimeException("解析 OpenAPI 規格檔 [" + path + "] 失敗: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 從 OpenAPI spec 解析指定 operation 的 requestBody schema 欄位名稱 (帶 O(1) 快取)
     */
    private List<String> resolveRequestFieldNames(String schemaRef) {
        if (schemaRef == null || !schemaRef.contains("#")) {
            return Collections.emptyList();
        }
        return requestFieldsCache.computeIfAbsent(schemaRef, ref -> {
            String[] parts = ref.split("#", 2);
            String specPath = parts[0].replace("classpath:", "").trim();
            String operationId = parts[1].trim();

            try {
                JsonNode root = getSpecAst(specPath);
                JsonNode operationNode = findOperationNode(root, operationId);
                if (operationNode == null) {
                    return Collections.emptyList();
                }

                JsonNode schemaNode = operationNode
                        .path("requestBody")
                        .path("content")
                        .path("application/json")
                        .path("schema");

                JsonNode schema = resolveSchemaRef(root, schemaNode);
                JsonNode properties = schema.path("properties");
                if (properties.isObject()) {
                    List<String> names = new ArrayList<>();
                    properties.fieldNames().forEachRemaining(names::add);
                    return names;
                }
            } catch (Exception e) {
                LOG.warn("解析 spec requestBody 失敗: schemaRef={}, error={}", ref, e.getMessage());
            }
            return Collections.emptyList();
        });
    }

    /**
     * 在 spec 的 paths 內尋找指定 operationId 的 operation node
     */
    private JsonNode findOperationNode(JsonNode root, String operationId) {
        JsonNode paths = root.path("paths");
        if (!paths.isObject()) return null;
        Iterator<Map.Entry<String, JsonNode>> pathFields = paths.fields();
        while (pathFields.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathFields.next();
            Iterator<Map.Entry<String, JsonNode>> methodFields = pathEntry.getValue().fields();
            while (methodFields.hasNext()) {
                Map.Entry<String, JsonNode> methodEntry = methodFields.next();
                JsonNode opNode = methodEntry.getValue();
                if (opNode.has("operationId") && operationId.equals(opNode.get("operationId").asText())) {
                    return opNode;
                }
            }
        }
        return null;
    }

    /**
     * 解析 spec 內 {@code $ref: '#/components/schemas/<name>'} 指向的 schema node
     */
    private JsonNode resolveSchemaRef(JsonNode root, JsonNode schemaNode) {
        if (schemaNode == null || !schemaNode.isObject() || !schemaNode.has("$ref")) {
            return schemaNode;
        }
        String ref = schemaNode.get("$ref").asText();
        if (ref.startsWith("#/components/schemas/")) {
            String schemaName = ref.substring("#/components/schemas/".length());
            return root.path("components").path("schemas").path(schemaName);
        }
        return schemaNode;
    }

    /**
     * 從快取的 OpenAPI AST 解析指定 operationId
     */
    private ApiEndpoint resolveEndpoint(String specPath, String operationId) {
        JsonNode root = getSpecAst(specPath);

        String specDeclaredBaseUrl = null;
        if (root.has("servers") && root.get("servers").isArray() && root.get("servers").size() > 0) {
            specDeclaredBaseUrl = root.get("servers").get(0).path("url").asText(null);
        }

        JsonNode paths = root.path("paths");
        if (paths.isMissingNode() || !paths.isObject()) {
            throw new IllegalArgumentException("OpenAPI 規格檔 [" + specPath + "] 缺少有效的 'paths' 區塊。");
        }

        Iterator<Map.Entry<String, JsonNode>> pathFields = paths.fields();
        while (pathFields.hasNext()) {
            var pathEntry = pathFields.next();
            String path = pathEntry.getKey();
            Iterator<Map.Entry<String, JsonNode>> methodFields = pathEntry.getValue().fields();

            while (methodFields.hasNext()) {
                var methodEntry = methodFields.next();
                String httpMethod = methodEntry.getKey().toUpperCase();
                JsonNode opNode = methodEntry.getValue();

                if (opNode.has("operationId") && operationId.equals(opNode.get("operationId").asText())) {
                    return new ApiEndpoint(path, httpMethod, specDeclaredBaseUrl);
                }
            }
        }

        throw new IllegalArgumentException("OpenAPI 規格檔 [" + specPath + "] 內未找到 operationId 為 '" + operationId + "' 的 API 端點。");
    }

    /**
     * 依照 SonataFlow 慣例規則動態解析 Base URL (帶快取的被動檢索)
     */
    private String resolveBaseUrl(String specPath, String functionName, String specDeclaredBaseUrl) {
        String cacheKey = functionName + "#" + specPath;
        return baseUrlCache.computeIfAbsent(cacheKey, k -> doResolveBaseUrl(specPath, functionName, specDeclaredBaseUrl));
    }

    private String doResolveBaseUrl(String specPath, String functionName, String specDeclaredBaseUrl) {
        Config config = ConfigProvider.getConfig();

        // 規則 1: kogito.sw.functions.<functionName>.base_url / url
        Optional<String> fnBaseUrl = config.getOptionalValue("kogito.sw.functions." + functionName + ".base_url", String.class)
                .or(() -> config.getOptionalValue("kogito.sw.functions." + functionName + ".url", String.class));
        if (fnBaseUrl.isPresent() && !fnBaseUrl.get().isBlank()) {
            return fnBaseUrl.get();
        }

        // 規則 2: kogito.sw.functions.<functionName>.protocol + host + port
        Optional<String> protocol = config.getOptionalValue("kogito.sw.functions." + functionName + ".protocol", String.class)
                .or(() -> config.getOptionalValue("kogito.sw.functions." + functionName + ".scheme", String.class));
        Optional<String> host = config.getOptionalValue("kogito.sw.functions." + functionName + ".host", String.class);
        Optional<String> port = config.getOptionalValue("kogito.sw.functions." + functionName + ".port", String.class);

        if (host.isPresent() && !host.get().isBlank()) {
            String protoStr = protocol.orElse("http");
            String portStr = port.map(p -> ":" + p).orElse("");
            return protoStr + "://" + host.get() + portStr;
        }

        // 規則 3: kogito.sw.openapi.<specKey>.base_url
        String specKey = specPath.replace("/", "_").replace(".", "_");
        Optional<String> specBaseUrl = config.getOptionalValue("kogito.sw.openapi." + specKey + ".base_url", String.class)
                .or(() -> config.getOptionalValue("quarkus.rest-client." + specKey + ".url", String.class));
        if (specBaseUrl.isPresent() && !specBaseUrl.get().isBlank()) {
            return specBaseUrl.get();
        }

        // 規則 4: openapi.yaml 內定義的 servers[0].url
        if (specDeclaredBaseUrl != null && !specDeclaredBaseUrl.isBlank()) {
            return specDeclaredBaseUrl;
        }

        throw new IllegalStateException("無法確定 API Base URL！未在 application.properties 中設定 SonataFlow 慣例屬性："
                + "\n  - 函式屬性: kogito.sw.functions." + functionName + ".host (或 .protocol / .port)"
                + "\n  - 規格屬性: kogito.sw.openapi." + specKey + ".base_url"
                + "\n且 [" + specPath + "] 內亦未宣告 servers[0].url。請於設定檔補齊 Base URL。");
    }

    /**
     * 提取 Payload
     */
    private JsonNode extractPayload(JsonNode arguments) {
        if (arguments == null) {
            return jsonMapper.createObjectNode();
        }
        if (arguments.has("payload")) {
            return arguments.get("payload");
        }
        ObjectNode payload = jsonMapper.createObjectNode();
        if (arguments.isObject()) {
            arguments.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                if (!"schemaRef".equals(key) && !"functionName".equals(key)) {
                    payload.set(key, entry.getValue());
                }
            });
        }
        return payload;
    }

    /**
     * 執行非同步 HTTP 請求 (sendAsync)
     */
    private CompletableFuture<JsonNode> executeHttpRequestAsync(String baseUrl, String path, String httpMethod, JsonNode payload) {
        try {
            String cleanBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            String cleanPath = path.startsWith("/") ? path : "/" + path;
            String fullUrl = cleanBase + cleanPath;

            String bodyJson = jsonMapper.writeValueAsString(payload);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", "application/json");

            if ("POST".equalsIgnoreCase(httpMethod)) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(bodyJson));
            } else if ("PUT".equalsIgnoreCase(httpMethod)) {
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(bodyJson));
            } else if ("GET".equalsIgnoreCase(httpMethod)) {
                requestBuilder.GET();
            }

            return httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        String respBody = Optional.ofNullable(response.body()).orElse("").trim();
                        ObjectNode result;
                        try {
                            if (respBody.startsWith("{") && respBody.endsWith("}")) {
                                result = (ObjectNode) jsonMapper.readTree(respBody);
                            } else {
                                result = jsonMapper.createObjectNode();
                                result.put("rawResponse", respBody);
                                result.put("code", response.statusCode() >= 400 ? "999" : "100");
                                result.put("message", "HTTP 狀態碼: " + response.statusCode());
                            }
                        } catch (Exception parseEx) {
                            result = jsonMapper.createObjectNode();
                            result.put("rawResponse", respBody);
                            result.put("code", "999");
                            result.put("message", "JSON 解析錯誤: " + parseEx.getMessage());
                        }
                        result.put("_httpStatus", response.statusCode());
                        return (JsonNode) result;
                    })
                    .exceptionally(ex -> {
                        LOG.error("非同步 HTTP 呼叫失敗: url={}, error={}", fullUrl, ex.getMessage(), ex);
                        ObjectNode error = jsonMapper.createObjectNode();
                        error.put("code", "999");
                        error.put("message", "API 呼叫錯誤: " + ex.getMessage());
                        error.put("_httpStatus", 0);
                        return error;
                    });
        } catch (Exception e) {
            LOG.error("建立非同步 HTTP 請求失敗: error={}", e.getMessage(), e);
            ObjectNode error = jsonMapper.createObjectNode();
            error.put("code", "999");
            error.put("message", "API 呼叫錯誤: " + e.getMessage());
            error.put("_httpStatus", 0);
            return CompletableFuture.completedFuture(error);
        }
    }

    private record ApiEndpoint(String path, String method, String specDeclaredBaseUrl) {}
}
