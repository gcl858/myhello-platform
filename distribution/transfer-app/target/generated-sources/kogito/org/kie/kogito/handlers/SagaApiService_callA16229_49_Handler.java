package org.kie.kogito.handlers;

import org.kie.kogito.internal.process.workitem.WorkItemExecutionException;

@jakarta.enterprise.context.ApplicationScoped()
public class SagaApiService_callA16229_49_Handler extends org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler {

    com.example.transfer.workflow.SagaApiService service;

    public SagaApiService_callA16229_49_Handler() {
        this(new com.example.transfer.workflow.SagaApiService());
    }

    @jakarta.inject.Inject()
    public SagaApiService_callA16229_49_Handler(com.example.transfer.workflow.SagaApiService service) {
        this.service = service;
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> activateWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        java.lang.String Account_A = (java.lang.String) workItem.getParameter("Account_A");
        java.lang.Integer AMT = (java.lang.Integer) workItem.getParameter("AMT");
        java.lang.Boolean EC = (java.lang.Boolean) workItem.getParameter("EC");
        org.kie.kogito.internal.process.workitem.WorkItemRecordParameters.recordInputParameters(workItem, "Account_A", "AMT", "EC");
        java.util.Map<java.lang.String, java.lang.Object> result;
        result = java.util.Collections.singletonMap("Result", service.callA16229(Account_A, AMT, EC));
        return java.util.Optional.of(this.workItemLifeCycle.newTransition("complete", workItem.getPhaseStatus(), result));
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> abortWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        return java.util.Optional.empty();
    }

    public String getName() {
        return "com.example.transfer.workflow.SagaApiService_callA16229_49_Handler";
    }
}
