package org.kie.kogito.handlers;

import org.kie.kogito.internal.process.workitem.WorkItemExecutionException;

@jakarta.enterprise.context.ApplicationScoped()
public class AuditLogService_saveLog_53_Handler extends org.kie.kogito.process.workitems.impl.DefaultKogitoWorkItemHandler {

    com.example.reconciliation.AuditLogService service;

    public AuditLogService_saveLog_53_Handler() {
        this(new com.example.reconciliation.AuditLogService());
    }

    @jakarta.inject.Inject()
    public AuditLogService_saveLog_53_Handler(com.example.reconciliation.AuditLogService service) {
        this.service = service;
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> activateWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        java.lang.String workflowId = (java.lang.String) workItem.getParameter("workflowId");
        java.lang.Object businessKey = (java.lang.Object) workItem.getParameter("businessKey");
        java.lang.Object inputData = (java.lang.Object) workItem.getParameter("inputData");
        java.lang.Object outputData = (java.lang.Object) workItem.getParameter("outputData");
        org.kie.kogito.internal.process.workitem.WorkItemRecordParameters.recordInputParameters(workItem, "workflowId", "businessKey", "inputData", "outputData");
        java.util.Map<java.lang.String, java.lang.Object> result;
        result = java.util.Collections.singletonMap("Result", service.saveLog(workflowId, businessKey, inputData, outputData));
        return java.util.Optional.of(this.workItemLifeCycle.newTransition("complete", workItem.getPhaseStatus(), result));
    }

    public java.util.Optional<org.kie.kogito.internal.process.workitem.WorkItemTransition> abortWorkItemHandler(org.kie.kogito.internal.process.workitem.KogitoWorkItemManager workItemManager, org.kie.kogito.internal.process.workitem.KogitoWorkItemHandler workItemHandler, org.kie.kogito.internal.process.workitem.KogitoWorkItem workItem, org.kie.kogito.internal.process.workitem.WorkItemTransition transition) {
        return java.util.Optional.empty();
    }

    public String getName() {
        return "com.example.reconciliation.AuditLogService_saveLog_53_Handler";
    }
}
