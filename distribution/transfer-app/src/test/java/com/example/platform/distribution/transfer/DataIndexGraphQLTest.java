package com.example.platform.distribution.transfer;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@DisplayName("Data Index GraphQL API 測試")
class DataIndexGraphQLTest {

    @Test
    @DisplayName("驗證 ProcessDefinitions 查詢成功")
    void testProcessDefinitionsQuery() {
        String query = "{\"query\":\"{ ProcessDefinitions { id name version } }\"}";

        given()
            .contentType("application/json")
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.ProcessDefinitions", notNullValue());
    }

    @Test
    @DisplayName("驗證 ProcessInstances 查詢成功（無 Error parsing time stamp 錯誤）")
    void testProcessInstancesQuery() {
        // 先觸發一次 workflow 確保資料庫中有 process instance
        given()
            .contentType("application/json")
            .body("{\"Account_A\":\"A123\",\"Account_B\":\"B123\",\"AMT\":100}")
        .when()
            .post("/saga2-transfer-workflow")
        .then()
            .statusCode(200);

        String query = "{\"query\":\"{ ProcessInstances { id processId state start end } }\"}";

        given()
            .contentType("application/json")
            .body(query)
        .when()
            .post("/graphql")
        .then()
            .statusCode(200)
            .body("errors", nullValue())
            .body("data.ProcessInstances", notNullValue());
    }
}
