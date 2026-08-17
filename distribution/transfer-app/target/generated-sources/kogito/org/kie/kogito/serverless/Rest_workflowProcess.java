package org.kie.kogito.serverless;

import org.kie.kogito.serverless.workflow.models.JsonNodeModel;
import org.kie.api.definition.process.Process;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.drools.core.util.KieFunctions;

@jakarta.enterprise.context.ApplicationScoped()
@jakarta.inject.Named("rest-workflow")
@io.quarkus.runtime.Startup()
public class Rest_workflowProcess extends org.kie.kogito.process.impl.AbstractProcess<org.kie.kogito.serverless.workflow.models.JsonNodeModel> {

    public Rest_workflowProcess(org.kie.kogito.app.Application app, org.kie.kogito.correlation.CorrelationService correlations) {
        this(app, correlations, new org.kie.kogito.handlers.Rest_workflowRestWorkItemHandler());
    }

    @jakarta.inject.Inject()
    public Rest_workflowProcess(org.kie.kogito.app.Application app, org.kie.kogito.correlation.CorrelationService correlations, org.kie.kogito.handlers.Rest_workflowRestWorkItemHandler rest_workflowRestWorkItemHandler) {
        super(app, java.util.Arrays.asList(rest_workflowRestWorkItemHandler), correlations);
        activate();
    }

    public Rest_workflowProcess() {
    }

    @Override()
    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(org.kie.kogito.serverless.workflow.models.JsonNodeModel value) {
        return new org.kie.kogito.serverless.Rest_workflowProcessInstance(this, value, this.createProcessRuntime());
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(java.lang.String businessKey, org.kie.kogito.serverless.workflow.models.JsonNodeModel value) {
        return new org.kie.kogito.serverless.Rest_workflowProcessInstance(this, value, businessKey, this.createProcessRuntime());
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(java.lang.String businessKey, org.kie.kogito.correlation.CompositeCorrelation correlation, org.kie.kogito.serverless.workflow.models.JsonNodeModel value) {
        return new org.kie.kogito.serverless.Rest_workflowProcessInstance(this, value, businessKey, this.createProcessRuntime(), correlation);
    }

    @Override()
    public org.kie.kogito.serverless.workflow.models.JsonNodeModel createModel() {
        return new org.kie.kogito.serverless.workflow.models.JsonNodeModel();
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(org.kie.kogito.Model value) {
        return this.createInstance((org.kie.kogito.serverless.workflow.models.JsonNodeModel) value);
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(java.lang.String businessKey, org.kie.kogito.Model value) {
        return this.createInstance(businessKey, (org.kie.kogito.serverless.workflow.models.JsonNodeModel) value);
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createInstance(org.kie.api.runtime.process.WorkflowProcessInstance wpi) {
        return new org.kie.kogito.serverless.Rest_workflowProcessInstance(this, this.createModel(), this.createProcessRuntime(), wpi);
    }

    public org.kie.kogito.serverless.Rest_workflowProcessInstance createReadOnlyInstance(org.kie.api.runtime.process.WorkflowProcessInstance wpi) {
        return new org.kie.kogito.serverless.Rest_workflowProcessInstance(this, this.createModel(), wpi);
    }

    protected org.kie.api.definition.process.Process process() {
        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("rest-workflow", true);
        factory.variable("workflowdata", org.jbpm.process.core.datatype.DataTypeResolver.fromClass(com.fasterxml.jackson.databind.JsonNode.class), "{}", java.util.Map.of());
        factory.variable("FetchDataState_5", org.jbpm.process.core.datatype.DataTypeResolver.fromClass(com.fasterxml.jackson.databind.JsonNode.class), java.util.Map.of("customTags", "internal"));
        factory.expressionLanguage("jq");
        factory.name("My First REST Workflow");
        factory.packageName("org.kie.kogito.serverless");
        factory.dynamic(false);
        factory.version("1.0.0");
        factory.type("SW");
        factory.visibility("Public");
        factory.inputValidator(new org.kie.kogito.serverless.workflow.actions.JsonSchemaValidator((com.fasterxml.jackson.databind.node.ObjectNode) org.jbpm.process.core.datatype.impl.coverter.TypeConverterRegistry.get().forType("com.fasterxml.jackson.databind.JsonNode").apply("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}},\"required\":[\"name\"]}"), true));
        factory.metaData("jbpm.enable.multi.con", null);
        factory.metaData("Variable", "workflowdata");
        factory.metaData("Description", "A simple workflow that demonstrates calling external REST APIs without OpenAPI spec");
        org.jbpm.ruleflow.core.factory.StartNodeFactory<?> startNode1 = factory.startNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("1"));
        startNode1.name("Start");
        startNode1.interrupting(false);
        startNode1.metaData("UniqueId", "1");
        startNode1.metaData("EventType", "none");
        startNode1.metaData("state", "FetchDataState");
        startNode1.done();
        org.jbpm.ruleflow.core.factory.EndNodeFactory<?> endNode2 = factory.endNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("2"));
        endNode2.name("End");
        endNode2.terminate(false);
        endNode2.metaData("UniqueId", "2");
        endNode2.metaData("state", "FetchDataState");
        endNode2.done();
        org.jbpm.ruleflow.core.factory.CompositeContextNodeFactory<?> compositeContextNode3 = factory.compositeContextNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("3"));
        compositeContextNode3.name("FetchDataState");
        compositeContextNode3.metaData("UniqueId", "3");
        compositeContextNode3.metaData("MetricName", "FetchDataState");
        compositeContextNode3.metaData("state", "FetchDataState");
        compositeContextNode3.variable("FetchDataState_5", org.jbpm.process.core.datatype.DataTypeResolver.fromClass(com.fasterxml.jackson.databind.JsonNode.class), java.util.Map.of("customTags", "internal"));
        compositeContextNode3.autoComplete(true);
        org.jbpm.ruleflow.core.factory.StartNodeFactory<?> startNode4 = compositeContextNode3.startNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("4"));
        startNode4.name("EmbeddedStart");
        startNode4.interrupting(false);
        startNode4.metaData("UniqueId", "4");
        startNode4.metaData("EventType", "none");
        startNode4.done();
        org.jbpm.ruleflow.core.factory.WorkItemNodeFactory<?> workItemNode6 = compositeContextNode3.workItemNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("6"));
        workItemNode6.name("fetchHealthData");
        workItemNode6.workName("Rest_workflowRestWorkItemHandler");
        workItemNode6.workParameter("BodyBuilder", new org.kie.kogito.serverless.workflow.rest.ParamsRestWorkItemHandlerBodyBuilder());
        workItemNode6.workParameter("Host", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.host", java.lang.String.class, "localhost"));
        workItemNode6.workParameter("Method", "get");
        workItemNode6.workParameter("Password", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.password", java.lang.String.class, null));
        workItemNode6.workParameter("Port", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.Integer>("kogito.sw.functions.fetchHealthData.port", java.lang.Integer.class, 8080));
        workItemNode6.workParameter("Protocol", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.protocol", java.lang.String.class, "http"));
        workItemNode6.workParameter("ResultHandler", new org.kie.kogito.serverless.workflow.rest.JsonNodeResultHandler());
        workItemNode6.workParameter("Url", "/q/health");
        workItemNode6.workParameter("Username", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.username", java.lang.String.class, null));
        workItemNode6.workParameter("accessToken", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.access_token", java.lang.String.class, null));
        workItemNode6.workParameter("apiKey", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.api_key", java.lang.String.class, null));
        workItemNode6.workParameter("apiKeyPrefix", new org.kie.kogito.serverless.workflow.workitemparams.ConfigWorkItemResolver<java.lang.String>("kogito.sw.functions.fetchHealthData.api_key_prefix", java.lang.String.class, null));
        workItemNode6.mapDataInputAssociation(new org.jbpm.workflow.core.impl.DataAssociation(java.util.Arrays.asList(new org.jbpm.workflow.core.impl.DataDefinition("workflowdata", "workflowdata", "java.lang.Object", null)), new org.jbpm.workflow.core.impl.DataDefinition("Parameter", "Parameter", "java.lang.Object", null), null, null));
        workItemNode6.mapDataOutputAssociation(new org.jbpm.workflow.core.impl.DataAssociation(java.util.Arrays.asList(new org.jbpm.workflow.core.impl.DataDefinition("Result", "Result", "java.lang.Object", null)), new org.jbpm.workflow.core.impl.DataDefinition("FetchDataState_5", "FetchDataState_5", "java.lang.Object", null), null, null));
        workItemNode6.done();
        workItemNode6.metaData("UniqueId", "6");
        workItemNode6.metaData("Type", "Rest");
        workItemNode6.metaData("action", "callApiAction");
        workItemNode6.metaData("recordArgs", true);
        workItemNode6.metaData("state", "FetchDataState");
        org.jbpm.ruleflow.core.factory.ActionNodeFactory<?> actionNode7 = compositeContextNode3.actionNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("7"));
        actionNode7.name("Script");
        actionNode7.metaData("UniqueId", "7");
        actionNode7.action(new org.kie.kogito.serverless.workflow.actions.ExpressionAction("jq", ".", "FetchDataState_5", "FetchDataState_5"));
        actionNode7.done();
        org.jbpm.ruleflow.core.factory.ActionNodeFactory<?> actionNode8 = compositeContextNode3.actionNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("8"));
        actionNode8.name("Script");
        actionNode8.metaData("UniqueId", "8");
        actionNode8.action(new org.kie.kogito.serverless.workflow.actions.CollectorAction("jq", "${ .apiResponse }", "workflowdata", "FetchDataState_5"));
        actionNode8.done();
        org.jbpm.ruleflow.core.factory.EndNodeFactory<?> endNode9 = compositeContextNode3.endNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("9"));
        endNode9.name("EmbeddedEnd");
        endNode9.terminate(true);
        endNode9.metaData("UniqueId", "9");
        endNode9.done();
        compositeContextNode3.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("4"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("6"), "4_6");
        compositeContextNode3.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("6"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("7"), "6_7");
        compositeContextNode3.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("7"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("8"), "7_8");
        compositeContextNode3.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("8"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("9"), "8_9");
        compositeContextNode3.done();
        org.jbpm.ruleflow.core.factory.ActionNodeFactory<?> actionNode10 = factory.actionNode(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("10"));
        actionNode10.name("Script");
        actionNode10.metaData("UniqueId", "10");
        actionNode10.metaData("MetricName", "FetchDataState");
        actionNode10.metaData("state", "FetchDataState");
        actionNode10.action(new org.kie.kogito.serverless.workflow.actions.ExpressionAction("jq", "{ greeting: \"Hello, \\(.name)!\", data: .apiResponse }", "workflowdata", "workflowdata"));
        actionNode10.done();
        factory.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("10"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("2"), "10_2");
        factory.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("1"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("3"), "1_3");
        factory.connection(org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("3"), org.jbpm.ruleflow.core.WorkflowElementIdentifierFactory.fromExternalFormat("10"), "3_10");
        factory.validate();
        return factory.getProcess();
    }
}
