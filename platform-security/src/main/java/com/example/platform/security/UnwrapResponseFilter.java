package com.example.platform.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Map;

/**
 * ===================================================================
 * UnwrapResponseFilter - SonataFlow 工作流程回應解包過濾器 (JAX-RS Response Filter)
 * ===================================================================
 * 
 * 作用與目的:
 *   - 預設情況下，SonataFlow (Kogito) 產生的 REST API 回應包裝格式會包含工作流實例元數據，如:
 *     {
 *       "id": "12345-abcde",
 *       "workflowdata": {
 *         "greeting": "Hello, John!",
 *         "data": { ... }
 *       }
 *     }
 *   - 本過濾器攔截所有傳出的 HTTP 回應，若發現 Payload 中包含 "workflowdata" 節點，
 *     會自動進行「解包 (Unwrap)」操作，將外層的 "id" 等元數據剝離，
 *     僅將純粹的業務資料 "workflowdata" 物件直接回傳給前端 Client。
 * 
 * 技術實現標註:
 *   - @Provider: JAX-RS / RESTEasy 標註，自動向 Quarkus 容器註冊此 ContainerResponseFilter 攔截器。
 *   - ContainerResponseFilter: 提供在 HTTP 回應寫回 Socket 之前的過濾與修改能力。
 */
@Provider
@ApplicationScoped
@Priority(Priorities.USER)
public class UnwrapResponseFilter implements ContainerResponseFilter {

    /**
     * 由 Quarkus 自動注入的 Jackson ObjectMapper 元件，用於 POJO 與 JsonNode 之間的物件轉譯。
     */
    @Inject
    ObjectMapper objectMapper;

    /**
     * 攔截並處理所有由 REST 端點傳出的 HTTP 回應物件。
     *
     * @param requestContext 包含目前 HTTP 請求上下文資訊 (如 Headers、URI、Method 等)
     * @param responseContext 包含傳出之 HTTP 回應上下文資訊 (如 Status Code、Entity Payload、Headers 等)
     * @throws IOException 處理過程中發生 I/O 異常時拋出
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // 取得目前準備傳出的回應內文物件 (Entity Payload)
        Object entity = responseContext.getEntity();
        
        // 若回應內文為空 (204 No Content 或空白回應)，直接結束不做處理
        if (entity == null) {
            return;
        }

        // -------------------------------------------------------------------
        // 情況 1：SonataFlow/Kogito 傳回的是 Java Map 結構
        // -------------------------------------------------------------------
        if (entity instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) entity;
            // 檢查 Map 中是否包含 "workflowdata" key
            if (map.containsKey("workflowdata")) {
                // 將 Entity 替換為僅包含 workflowdata 的子物件
                responseContext.setEntity(map.get("workflowdata"));
                // 強制將狀態碼改為 200 OK
                responseContext.setStatus(200);
                return;
            }
        }

        // -------------------------------------------------------------------
        // 情況 2：回應物件已經是 Jackson JsonNode 樹狀結構
        // -------------------------------------------------------------------
        if (entity instanceof JsonNode) {
            JsonNode node = (JsonNode) entity;
            // 檢查 JsonNode 物件中是否包含 "workflowdata" 節點
            if (node.has("workflowdata")) {
                // 提取 "workflowdata" 節點並取代原 Entity
                responseContext.setEntity(node.get("workflowdata"));
                responseContext.setStatus(200);
                return;
            }
        }

        // -------------------------------------------------------------------
        // 情況 3：如果是其他自訂 POJO 或包裝物件，嘗試使用 ObjectMapper 轉成 JsonNode 後判斷
        // -------------------------------------------------------------------
        try {
            // 將自訂 Java POJO 物件動態轉換為 Jackson JsonNode 樹
            JsonNode tree = objectMapper.valueToTree(entity);
            if (tree != null && tree.has("workflowdata")) {
                // 若含有 "workflowdata"，將其提取並設定為最終的 Entity Payload
                responseContext.setEntity(tree.get("workflowdata"));
                responseContext.setStatus(200);
            }
        } catch (Exception ignored) {
            // 若轉換失敗 (非 JSON 可相容物件)，忽略例外並保持原 Entity 回傳
        }
    }
}