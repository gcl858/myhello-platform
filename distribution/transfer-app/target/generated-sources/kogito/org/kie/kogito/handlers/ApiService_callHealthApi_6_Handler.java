package org.kie.kogito.handlers;

import org.kie.kogito.internal.process.workitem.WorkItemExecutionException;

@jakarta.enterprise.context.ApplicationScoped()
public class ApiService_callHealthApi_6_Handler extends org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler {

    com.example.platform.transfer.samples.ApiService service;

    public ApiService_callHealthApi_6_Handler() {
        this(new com.example.platform.transfer.samples.ApiService());
    }

    @jakarta.inject.Inject()
    public ApiService_callHealthApi_6_Handler(com.example.platform.transfer.samples.ApiService service) {
        this.service = service;
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> activateWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        com.fasterxml.jackson.databind.JsonNode Parameter = (com.fasterxml.jackson.databind.JsonNode) workItem.getParameter("Parameter");
        org.kie.kogito.internal.process.workitem.WorkItemRecordParameters.recordInputParameters(workItem, "Parameter");
        java.util.Map<java.lang.String, java.lang.Object> result;
        result = java.util.Collections.singletonMap("Result", service.callHealthApi(Parameter));
        return java.util.Optional.of(this.workItemLifeCycle.newTransition("complete", workItem.getPhaseStatus(), result));
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> abortWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        return java.util.Optional.empty();
    }

    public String getName() {
        return "com.example.platform.transfer.samples.ApiService_callHealthApi_6_Handler";
    }
}
