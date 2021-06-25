package com.lesfurets.jenkins.unit.declarative

abstract class GenericPipelineDeclaration {

    AgentDeclaration agent
    Closure environment
    Closure tools
    PostDeclaration post
    Map<String, StageDeclaration> stages = [:]

    static <T> T executeWith(Object delegate, Closure<T> closure, Integer resolveStrategy = null) {
        if (closure) {
            closure.delegate = delegate;
            if(resolveStrategy) {
                closure.resolveStrategy = resolveStrategy
            }
            return closure.call()
        }
        return null
    }

    def agent(Object o) {
        this.agent = new AgentDeclaration().with { it.label = o; it }
    }

    def agent(Closure closure) {
        this.@agent = new AgentDeclaration();
        executeWith(this.@agent, closure)
    }

    def environment(Closure closure) {
        this.environment = closure
    }

    def tools(Closure closure) {
        this.tools = closure
    }

    def post(Closure closure) {
        this.post = new PostDeclaration();
        executeWith(this.post, closure)
    }

    def stages(Closure closure) {
        closure.call()
    }

    def stage(String stageName, Closure closure) {
        def stageDeclaration = new StageDeclaration(stageName)
        executeWith(stageDeclaration, closure, Closure.DELEGATE_FIRST);
        this.stages.put(stageName, stageDeclaration)
    }

    def execute(Script script) {
        Map envValuestoRestore = [:]

        // set environment
        if (this.environment) {
            envValuestoRestore = initEnvironment(this.environment, script)
        }
        resetEnvironment(envValuestoRestore, script)
    }

    public static Map initEnvironment(Closure environment, Object delegate) {
        Map envValuestoRestore = [:]
        Binding subBinding = new Binding()
        subBinding.metaClass.setProperty = { String propertyName, Object newValue ->
            if (delegate.hasProperty(propertyName)) {
                envValuestoRestore.put(propertyName, delegate.getProperty(propertyName))
            }
            (delegate.env as Map).put(propertyName, newValue)
        }
        def envClosure = environment.rehydrate(subBinding, environment, delegate)
        envClosure.resolveStrategy = Closure.DELEGATE_FIRST
        envClosure.call()
        return envValuestoRestore
    }

    public static resetEnvironment(Map envValuestoRestore, Object delegate) {
        envValuestoRestore.entrySet().forEach { entry ->
            def envMap = delegate.env as Map
            envMap.put(entry.getKey(), entry.getValue())
        }
    }
}
