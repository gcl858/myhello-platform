package org.kie.kogito.openapi.openapi.api;

import java.util.List;
import java.util.Map;


import org.kie.kogito.openapi.openapi.model.A16220Request;
import org.kie.kogito.openapi.openapi.model.A16220Response;
import org.kie.kogito.openapi.openapi.model.A16229Request;
import org.kie.kogito.openapi.openapi.model.A16229Response;
import org.kie.kogito.openapi.openapi.model.HealthResponse;
/**
  * Combined Transfer API
  * <p>包含 A16229 與 A16220 的合併 OpenAPI 規格</p>
  */
@jakarta.ws.rs.Path("")
@org.eclipse.microprofile.rest.client.inject.RegisterRestClient(baseUri="http://localhost:8080/", configKey="openapi_yaml")
@io.quarkiverse.openapi.generator.annotations.GeneratedClass(value="openapi.yaml", tag = "Default")
@jakarta.enterprise.context.ApplicationScoped
public interface DefaultApi {

     /**
     * 執行 A16220 交易
     *
     * 處理 A16220 交易請求。當 AMT 小於 1000 時交易成功， 否則回傳 403 錯誤。 
     *
     * @param a16220Request 
     */
    @io.quarkiverse.openapi.generator.markers.OperationMarker(name="", openApiSpecId="openapi_yaml", operationId="executeA16220", method="POST", path="/A16220")
    @jakarta.ws.rs.POST
    @jakarta.ws.rs.Path("/A16220")
    @jakarta.ws.rs.Consumes({"application/json"})
    @jakarta.ws.rs.Produces({"application/json"})
    @io.quarkiverse.openapi.generator.annotations.GeneratedMethod("executeA16220")
    public A16220Response executeA16220(
        A16220Request a16220Request
    );

     /**
     * 執行 A16229 交易
     *
     * 處理 A16229 交易請求。當 AMT 大於 50 時交易成功， 否則回傳 403 錯誤。 
     *
     * @param a16229Request 
     */
    @io.quarkiverse.openapi.generator.markers.OperationMarker(name="", openApiSpecId="openapi_yaml", operationId="executeA16229", method="POST", path="/A16229")
    @jakarta.ws.rs.POST
    @jakarta.ws.rs.Path("/A16229")
    @jakarta.ws.rs.Consumes({"application/json"})
    @jakarta.ws.rs.Produces({"application/json"})
    @io.quarkiverse.openapi.generator.annotations.GeneratedMethod("executeA16229")
    public A16229Response executeA16229(
        A16229Request a16229Request
    );

     /**
     */
    @io.quarkiverse.openapi.generator.markers.OperationMarker(name="", openApiSpecId="openapi_yaml", operationId="getData", method="GET", path="/q/health")
    @jakarta.ws.rs.GET
    @jakarta.ws.rs.Path("/q/health")
    @jakarta.ws.rs.Produces({"application/json"})
    @io.quarkiverse.openapi.generator.annotations.GeneratedMethod("getData")
    public HealthResponse getData(
    );

}