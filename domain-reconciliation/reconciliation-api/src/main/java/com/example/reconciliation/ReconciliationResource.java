package com.example.reconciliation;

import com.example.reconciliation.ReconciliationCsvExporter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * 對帳單匯出 REST 端點
 *
 * 提供手動觸發匯出 SQLite View {@code vw_saga_reconciliation} 至
 * {@code data/reconciliation-YYYYMMDD-HHmmss.csv} 的介面。
 *
 * 用法:
 *   GET  /reconciliation/export-csv                       — 匯出全部紀錄
 *   POST /reconciliation/export-csv  (application/json)   — body 指定過濾條件
 *   GET  /reconciliation/export-csv?status=SUCCESS        — 只匯出 SUCCESS 紀錄
 *   GET  /reconciliation/export-csv?businessKey=A123      — 只匯出特定業務鍵
 */
@Path("/reconciliation")
@Produces(MediaType.APPLICATION_JSON)
public class ReconciliationResource {

    @Inject
    ReconciliationCsvExporter exporter;

    /**
     * GET 版本:用 query param 指定過濾條件,適合瀏覽器或排程工具呼叫
     */
    @GET
    @Path("/export-csv")
    public Response exportViaGet(
            @QueryParam("workflowId") String workflowId,
            @QueryParam("businessKey") String businessKey,
            @QueryParam("status") String status) {
        JsonNode result = exporter.exportToCsv(workflowId, businessKey, status);
        int statusCode = result.path("ok").asBoolean(false) ? 200 : 500;
        return Response.status(statusCode).entity(result.toString()).build();
    }

    /**
     * POST 版本:用 JSON body 指定過濾條件,適合程式化呼叫
     * Body 範例: {"workflowId":"saga2-transfer-workflow","status":"ROLLED_BACK"}
     */
    @POST
    @Path("/export-csv")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportViaPost(JsonNode body) {
        String workflowId = null;
        String businessKey = null;
        String status = null;
        if (body != null) {
            if (body.hasNonNull("workflowId")) workflowId = body.get("workflowId").asText();
            if (body.hasNonNull("businessKey")) businessKey = body.get("businessKey").asText();
            if (body.hasNonNull("status")) status = body.get("status").asText();
        }
        JsonNode result = exporter.exportToCsv(workflowId, businessKey, status);
        int statusCode = result.path("ok").asBoolean(false) ? 200 : 500;
        return Response.status(statusCode).entity(result.toString()).build();
    }
}
