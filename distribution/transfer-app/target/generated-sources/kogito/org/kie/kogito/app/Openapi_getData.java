package org.kie.kogito.app;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped()
public class Openapi_getData extends org.kie.kogito.serverless.workflow.openapi.OpenApiWorkItemHandler<org.kie.kogito.openapi.openapi.api.DefaultApi> {

    protected Object internalExecute(org.kie.kogito.openapi.openapi.api.DefaultApi openApiRef, java.util.Map<java.lang.String, java.lang.Object> parameters) {
        return openApiRef.getData();
    }

    protected java.lang.Class<org.kie.kogito.openapi.openapi.api.DefaultApi> getRestClass() {
        return org.kie.kogito.openapi.openapi.api.DefaultApi.class;
    }

    public java.lang.String getName() {
        return "openapi_getData";
    }
}
