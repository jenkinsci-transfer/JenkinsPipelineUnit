package com.lesfurets.jenkins.unit.declarative

class ParallelDeclaration {

    Map<String, StageDeclaration> stages = [:]
    boolean failFast

    ParallelDeclaration(boolean failFast) {
        this.failFast = failFast
    }

    ParallelDeclaration() {
        this.failFast = false
    }

    def stage(String name, Closure closure) {
        def stageDeclaration = new StageDeclaration(name);
        GenericPipelineDeclaration.executeWith(stageDeclaration, closure);
        this.stages.put(name, stageDeclaration)
    }

    def execute(Script script) {
        this.stages.entrySet().forEach { e ->
            e.value.execute(script)
        }
    }

}
