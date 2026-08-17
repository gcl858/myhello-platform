package com.example.transfer.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * ===================================================================
 * SagaApiService - 自訂 API 服務,直接用 JDK HttpClient (非同步優化版)
 * ===================================================================
 *
 * 呼叫契約 (由 workflow 用 type: custom 呼叫,SonataFlow 會用反射):
 *   callA16229(String Account_A, Integer AMT, Boolean EC) -> CompletionStage<JsonNode> body
 *   callA16220(String Account_B, Integer AMT, Boolean EC) -> CompletionStage<JsonNode> body
 */
@ApplicationScoped
public class SagaApiService {

    private static final Logger LOG = LoggerFactory.getLogger(SagaApiService.class);
    private static final String BASE_URL = "http://localhost:8080";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 呼叫 A16229 API
     */
    public JsonNode callA16229(String Account_A, Integer AMT, Boolean EC) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("Account_A", Account_A);
        body.put("AMT", AMT);
        body.put("EC", EC);
        return callApiAsync("/A16229", body).join();
    }

    /**
     * 呼叫 A16220 API
     */
    public JsonNode callA16220(String Account_B, Integer AMT, Boolean EC) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("Account_B", Account_B);
        body.put("AMT", AMT);
        body.put("EC", EC);
        return callApiAsync("/A16220", body).join();
    }

    /**
     * 通用非同步 HTTP POST 呼叫
     */
    private CompletableFuture<JsonNode> callApiAsync(String path, JsonNode body) {
        try {
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + path))
                    .timeout(HTTP_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        ObjectNode result;
                        try {
                            result = (ObjectNode) objectMapper.readTree(response.body());
                        } catch (Exception parseEx) {
                            result = objectMapper.createObjectNode();
                            result.put("rawResponse", response.body());
                            result.put("code", response.statusCode() >= 400 ? "999" : "100");
                            result.put("message", "HTTP 狀態碼: " + response.statusCode());
                        }
                        result.put("_httpStatus", response.statusCode());
                        return (JsonNode) result;
                    })
                    .exceptionally(ex -> {
                        LOG.error("API call {} failed: {}", path, ex.getMessage());
                        ObjectNode error = objectMapper.createObjectNode();
                        error.put("code", "999");
                        error.put("message", "API 連線錯誤: " + ex.getMessage());
                        error.put("_httpStatus", 0);
                        return error;
                    });
        } catch (Exception e) {
            LOG.error("建立 API 呼叫 {} 失敗: {}", path, e.getMessage());
            ObjectNode error = objectMapper.createObjectNode();
            error.put("code", "999");
            error.put("message", "API 連線錯誤: " + e.getMessage());
            error.put("_httpStatus", 0);
            return CompletableFuture.completedFuture(error);
        }
    }
}
