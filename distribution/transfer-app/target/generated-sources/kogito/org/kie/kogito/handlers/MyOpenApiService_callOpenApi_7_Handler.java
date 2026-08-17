package org.kie.kogito.handlers;

import org.kie.kogito.internal.process.workitem.WorkItemExecutionException;

@jakarta.enterprise.context.ApplicationScoped()
public class MyOpenApiService_callOpenApi_7_Handler extends org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler {

    com.example.transfer.workflow.MyOpenApiService service;

    public MyOpenApiService_callOpenApi_7_Handler() {
        this(new com.example.transfer.workflow.MyOpenApiService());
    }

    @jakarta.inject.Inject()
    public MyOpenApiService_callOpenApi_7_Handler(com.example.transfer.workflow.MyOpenApiService service) {
        this.service = service;
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> activateWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        java.lang.String functionName = (java.lang.String) workItem.getParameter("functionName");
        java.lang.String schemaRef = (java.lang.String) workItem.getParameter("schemaRef");
        java.util.Map payload = (java.util.Map) workItem.getParameter("payload");
        org.kie.kogito.internal.process.workitem.WorkItemRecordParameters.recordInputParameters(workItem, "functionName", "schemaRef", "payload");
        java.util.Map<java.lang.String, java.lang.Object> result;
        result = java.util.Collections.singletonMap("Result", service.callOpenApi(functionName, schemaRef, payload));
        return java.util.Optional.of(this.workItemLifeCycle.newTransition("complete", workItem.getPhaseStatus(), result));
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> abortWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        return java.util.Optional.empty();
    }

    public String getName() {
        return "com.example.transfer.workflow.MyOpenApiService_callOpenApi_7_Handler";
    }
}
