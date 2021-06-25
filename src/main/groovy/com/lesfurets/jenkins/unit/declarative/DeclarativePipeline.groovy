package com.lesfurets.jenkins.unit.declarative

class DeclarativePipeline extends GenericPipelineDeclaration {

    def properties = [:]
    List<Closure> options = []

    Closure triggers
    ParametersDeclaration parameters = null

    DeclarativePipeline() {
        properties.put('any', 'any')
        properties.put('none', 'none')
        properties.put('scm', 'scm')
    }

    def propertyMissing(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name)
        } else {
            throw new MissingPropertyException(name)
        }
    }

    def options(Closure closure) {
        options.add(closure)
    }

    def triggers(Closure closure) {
        this.triggers = closure
    }

    def parameters(Object o) {
        this.parameters = new ParametersDeclaration().with { it.label = o; it }
    }

    def parameters(Closure closure) {
        this.parameters = new ParametersDeclaration()
        this.parameters.binding = closure.binding;
        executeWith(this.parameters, closure)
    }

    def execute(Script script) {
        super.execute(script)
        this.options.forEach {
            executeWith(script, it)
        }
        this.agent?.execute(script)
        if (this.triggers) {
            executeWith(script, this.triggers)
        }
        this.stages.entrySet().forEach { e ->
            e.value.execute(script)
        }
        this.post?.execute(script)
    }

}
