package org.kie.kogito.serverless;

import org.kie.kogito.serverless.workflow.models.JsonNodeModel;

public class OpenAPI_workflowProcessInstance extends org.kie.kogito.process.impl.AbstractProcessInstance<JsonNodeModel> {

    public OpenAPI_workflowProcessInstance(org.kie.kogito.serverless.OpenAPI_workflowProcess process, JsonNodeModel value, org.kie.api.runtime.process.ProcessRuntime processRuntime) {
        super(process, value, processRuntime);
    }

    public OpenAPI_workflowProcessInstance(org.kie.kogito.serverless.OpenAPI_workflowProcess process, JsonNodeModel value, java.lang.String businessKey, org.kie.api.runtime.process.ProcessRuntime processRuntime) {
        super(process, value, businessKey, processRuntime);
    }

    public OpenAPI_workflowProcessInstance(org.kie.kogito.serverless.OpenAPI_workflowProcess process, JsonNodeModel value, org.kie.api.runtime.process.ProcessRuntime processRuntime, org.kie.api.runtime.process.WorkflowProcessInstance wpi) {
        super(process, value, processRuntime, wpi);
    }

    public OpenAPI_workflowProcessInstance(org.kie.kogito.serverless.OpenAPI_workflowProcess process, JsonNodeModel value, org.kie.api.runtime.process.WorkflowProcessInstance wpi) {
        super(process, value, wpi);
    }

    public OpenAPI_workflowProcessInstance(org.kie.kogito.serverless.OpenAPI_workflowProcess process, JsonNodeModel value, java.lang.String businessKey, org.kie.api.runtime.process.ProcessRuntime processRuntime, org.kie.kogito.correlation.CompositeCorrelation correlation) {
        super(process, value, businessKey, processRuntime, correlation);
    }

    protected java.util.Map<String, Object> bind(JsonNodeModel variables) {
        if (null != variables)
            return variables.toMap();
        else
            return new java.util.HashMap();
    }

    protected void unbind(JsonNodeModel variables, java.util.Map<String, Object> vmap) {
        variables.fromMap(this.id(), vmap);
    }
}
