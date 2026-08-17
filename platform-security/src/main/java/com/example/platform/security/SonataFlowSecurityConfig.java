package com.example.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SonataFlowSecurityConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 1. 全域 Swagger 過濾器：自動將所有 API 路徑的非 POST 方法從文件移除
    public static class SwaggerFilter implements OASFilter {
        @Override
        public PathItem filterPathItem(PathItem pathItem) {
            pathItem.setGET(null);
            pathItem.setDELETE(null);
            pathItem.setPUT(null);
            pathItem.setPATCH(null);
            pathItem.setHEAD(null);
            return pathItem;
        }
    }

    // 2. 全域 HTTP 請求攔截器：非 POST 請求一律在執行期回傳 403 Forbidden，並緩存原始 Input Payload
    @Provider
    @jakarta.enterprise.context.ApplicationScoped
    @jakarta.annotation.Priority(jakarta.ws.rs.Priorities.USER)
    public static class RuntimeFilter implements ContainerRequestFilter {
        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            String path = requestContext.getUriInfo().getPath();
            String method = requestContext.getMethod();

            // 自動排除 Quarkus 系統頁面（如 /q/swagger-ui, /q/dev），其餘 API 若非 POST 則直接封鎖
            if (path.startsWith("q/")) {
                return;
            }

            if (!method.equalsIgnoreCase("POST")) {
                requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                            .entity("{\"error\": \"Only POST method is allowed\"}")
                            .build()
                );
                return;
            }

            // 讀取 Input Stream 並緩存，使用 ByteArrayInputStream 重設 stream 避免 stream 被耗盡
            InputStream entityStream = requestContext.getEntityStream();
            if (entityStream != null) {
                byte[] bytes = entityStream.readAllBytes();
                String inputPayload = new String(bytes, StandardCharsets.UTF_8);

                // 重設 Stream 供後續 SonataFlow 反序列化使用
                requestContext.setEntityStream(new ByteArrayInputStream(bytes));

                // 暫存於 requestContext 供後續 Audit Log 使用
                requestContext.setProperty("rawInputPayload", inputPayload);
                requestContext.setProperty("startTimeNano", System.nanoTime());
            }
        }
    }

    /**
     * 3. Response Filter：
     *   (1) 依 saga2-transfer-workflow body 內 `status` 欄位改寫 HTTP code。
     *       status=VALIDATION_FAILED    → HTTP 400 (客戶端輸入錯)
     *       status=FAILED               → HTTP 422 (業務邏輯拒絕)
     *       status=ROLLBACK_FAILED      → HTTP 409 (資源衝突,需人工介入)
     *       其它 (SUCCESS / ROLLED_BACK) → 保持 200
     *   (2) 記錄完整的 URI Path、HTTP Code、耗時 (ms)、最初 Input Payload 與最終 Output Payload 至 Log。
     */
    @Provider
    @jakarta.enterprise.context.ApplicationScoped
    @jakarta.annotation.Priority(jakarta.ws.rs.Priorities.USER + 10)
    public static class WorkflowStatusFilter implements ContainerResponseFilter {
        private static final Logger LOG = Logger.getLogger(WorkflowStatusFilter.class);
        private static final String WORKFLOW_PATH_PREFIX = "saga2-transfer-workflow";

        @Override
        public void filter(ContainerRequestContext requestContext,
                           ContainerResponseContext responseContext) {
            // 只處理 POST 啟動流程的回應 (GET 狀態查詢、DELETE 取消不適用)
            if (!"POST".equalsIgnoreCase(requestContext.getMethod())) {
                return;
            }

            String path = requestContext.getUriInfo().getPath();
            if (path.startsWith("q/")) {
                return;
            }

            // 1. 若為 saga2-transfer-workflow，執行狀態碼改寫邏輯
            if (path.startsWith(WORKFLOW_PATH_PREFIX)) {
                Object entity = responseContext.getEntity();
                if (entity instanceof Map<?, ?> map) {
                    Object status = map.get("status");
                    if (status == null && map.get("workflowdata") instanceof Map<?, ?> wfMap) {
                        status = wfMap.get("status");
                    }
                    if (status != null) {
                        String s = status.toString();
                        int rewrite = switch (s) {
                            case "VALIDATION_FAILED" -> 400;
                            case "ROLLBACK_FAILED"   -> 409;
                            default                  -> -1;
                        };

                        if (rewrite > 0) {
                            LOG.infof("[WorkflowStatusFilter] %s status=%s → HTTP %d", path, s, rewrite);
                            responseContext.setStatus(rewrite);
                        }
                    }
                }
            }

            // 2. 輸出完整的 Path、Input Payload、Output Payload 與執行耗時
            String inputPayload = (String) requestContext.getProperty("rawInputPayload");
            Long startTime = (Long) requestContext.getProperty("startTimeNano");
            long durationMs = startTime != null ? (System.nanoTime() - startTime) / 1_000_000 : 0;

            String outputPayload = "{}";
            try {
                Object entity = responseContext.getEntity();
                if (entity != null) {
                    outputPayload = (entity instanceof String s) ? s : MAPPER.writeValueAsString(entity);
                }
            } catch (Exception ignored) {
                outputPayload = String.valueOf(responseContext.getEntity());
            }

            String displayPath = path.startsWith("/") ? path : "/" + path;
            LOG.infof("[HTTP-AUDIT] URI: %s | HTTP: %d | Cost: %dms | INPUT: %s | OUTPUT: %s",
                    displayPath,
                    responseContext.getStatus(),
                    durationMs,
                    inputPayload != null ? inputPayload.replaceAll("\\s+", " ").trim() : "{}",
                    outputPayload.replaceAll("\\s+", " ").trim()
            );
        }
    }
}