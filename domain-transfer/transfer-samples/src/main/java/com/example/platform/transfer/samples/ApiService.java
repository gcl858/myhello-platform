package com.example.platform.transfer.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * ===================================================================
 * ApiService - SonataFlow 工作流程專用之 Java Service 類別
 * ===================================================================
 * 
 * 本類別為 Quarkus CDI (Contexts and Dependency Injection) 託管的 Bean。
 * 提供給 SonataFlow 工作流程 (Serverless Workflow) 透過 `type: custom` 方式直接呼叫。
 * 
 * 呼叫機制說明:
 *   - SonataFlow 工作流程可以在 `.sw.yaml` 中定義：
 *     operation: "service:com.example.ApiService::<方法名稱>"
 *   - 當工作流程執行到相應動作 (Action) 時，SonataFlow 引擎會反射尋找此 Bean 並執行指定方法。
 * 
 * 標註說明:
 *   - @ApplicationScoped: 代表此 Bean 於整個微服務生命週期中為單例 (Singleton) 模式。
 */
@ApplicationScoped
public class ApiService {

    /**
     * Jackson JSON 處理器元件。
     * 由 Quarkus CDI 容器自動注入 (Inject)，用於 JSON 物件的組裝、轉換與解析。
     */
    @Inject
    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ===================================================================
     * 呼叫外部 API 服務 (例如：合庫 API Gateway 測試環境)
     * ===================================================================
     * 
     * 本方法接受由工作流程傳入的 3 個業務參數，組裝為 JSON Payload 後發送 HTTP POST 請求。
     * 
     * @param idno 身分證字號 / 統一編號 (IDNO)
     * @param spcd 特店代號 / 業務代號 (SPCD)
     * @param brno 分行代號 (BRNO)
     * @return JsonNode 解析後的 API 回應 JSON 物件，或包含錯誤訊息的 JSON 物件
     */
    public JsonNode callExternalApi(String idno, String spcd, String brno) {
        try {
            // ---------------------------------------------------------------
            // 1. 自動把傳入的三個參數組合成 JSON 物件 (ObjectNode)
            // ---------------------------------------------------------------
            ObjectNode inputJson = objectMapper.createObjectNode();
            inputJson.put("IDNO", idno);
            inputJson.put("SPCD", spcd);
            inputJson.put("BRNO", brno);

            // ---------------------------------------------------------------
            // 2. 設定繞過內網 SSL / TLS 憑證檢查 ( TrustAllCerts )
            //    注意：此處建立了會信任所有 SSL 憑證的 TrustManager，
            //    主要用於開發 / UAT 環境對付自簽憑證或內網未受信 API Gateway。
            // ---------------------------------------------------------------
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    // 忽略簽發者檢查
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    // 忽略客戶端憑證檢查
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    // 忽略伺服器端憑證檢查
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            // 初始化 TLS SSLContext 並套用繞過憑證特性的 TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 建立支援忽略 SSL 檢查的 JDK 原生 HttpClient
            HttpClient client = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            // ---------------------------------------------------------------
            // 3. 發送 HTTP POST 請求
            // ---------------------------------------------------------------
            // 設定目標網址 (合庫 API Gateway UAT 端點與 Client ID)
            String targetUrl = "https://apimgw-uat.tcbbank.com.tw/tcbbank/private/TWS/ESB/TWTXN/TW9118200?client_id=3990bd00-5c7f-424a-b69d-c56cafc902d0";
            
            // 將 inputJson 物件序列化為 JSON 字串
            String requestBody = objectMapper.writeValueAsString(inputJson);

            // 建立 HTTP 請求物件 (指定 URI、標頭 Content-Type 及 POST Body)
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 同步發送請求並獲取回應 (字串格式 Body)
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // ---------------------------------------------------------------
            // 4. 解析 HTTP 回應
            // ---------------------------------------------------------------
            if (response.statusCode() == 200) {
                // HTTP 200 OK：將回應內文 JSON 字串解析為 JsonNode 樹狀物件傳回工作流
                return objectMapper.readTree(response.body());
            } else {
                // 非 200 回應：包裝 HTTP 狀態碼與錯誤細節 JSON 傳回工作流
                return objectMapper.createObjectNode()
                        .put("status", response.statusCode())
                        .put("error", "API 呼叫失敗")
                        .put("details", response.body());
            }

        } catch (Exception e) {
            // ---------------------------------------------------------------
            // 5. 例外處理：補獲網路連線失敗、序列化異常等 Exception
            // ---------------------------------------------------------------
            return objectMapper.createObjectNode()
                    .put("status", 500)
                    .put("error", e.getClass().getName())
                    .put("message", e.getMessage());
        }
    }

    /**
     * ===================================================================
     * 呼叫本機健康檢查 API (Health Check Endpoint)
     * ===================================================================
     * 
     * 接收 java-workflow.sw.yaml 中 callHealthApiService 函式呼叫。
     * 以 GET 方式呼叫本機 Quarkus 健康檢查端點 http://localhost:8080/q/health，
     * 並回傳 JSON 解析結果 (JsonNode)。
     *
     * 重要規範說明 (SonataFlow Service Integration):
     *   - Kogito / SonataFlow 在進行 service work item 動態方法反射尋找時，
     *     會優先匹配包含「單一 JsonNode 參數」的方法簽章，用以傳遞工作流程上文中對應的輸入 JSON 物件。
     *   - 即使目前該端點不需要傳入任何 Payload，參數列中仍建議保留 `JsonNode parameters` 以利相容。
     *
     * @param parameters 工作流程傳入的參數物件 (若無需參數可不採集此物件內容)
     * @return JsonNode 回傳 Quarkus 健康檢查 API 之回應 JSON，或錯誤訊息物件
     */
    public JsonNode callHealthApi(JsonNode parameters) {
        try {
            // 1. 建立預設之 HTTP 客戶端 (本機端點無需特別設定 SSL)
            HttpClient client = HttpClient.newHttpClient();

            // 2. 建立 HTTP GET 請求物件，指向 Quarkus 標準健康檢查端點 /q/health
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/q/health"))
                    .GET()
                    .build();

            // 3. 發送 HTTP 請求並接收字串形式之回應內文
            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            // 4. 解析 HTTP 回應碼
            if (response.statusCode() == 200) {
                // 200 OK -> 將健康檢查結果 (如 { "status": "UP", "checks": [...] }) 解析為 JsonNode 傳回
                return objectMapper.readTree(response.body());
            } else {
                // 非 200 狀態 -> 建立失敗資訊之 JSON 物件
                return objectMapper.createObjectNode()
                        .put("status", response.statusCode())
                        .put("error", "API 呼叫失敗")
                        .put("details", response.body());
            }
        } catch (Exception e) {
            // 5. 補獲網路例外或系統異常，傳回 500 錯誤 JSON
            return objectMapper.createObjectNode()
                    .put("status", 500)
                    .put("error", e.getClass().getName())
                    .put("message", e.getMessage());
        }
    }
}
