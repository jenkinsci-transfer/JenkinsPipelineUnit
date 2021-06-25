package com.lesfurets.jenkins.unit.declarative

class StageDeclaration extends GenericPipelineDeclaration {

    String name
    Closure steps
    WhenDeclaration when
    ParallelDeclaration parallel
    boolean failFast = false
    List<Closure> options = []

    StageDeclaration(String name) {
        this.name = name
    }

    def steps(Closure closure) {
        this.steps = closure
    }

    def failFast(boolean failFast) {
        this.failFast = failFast
    }

    def parallel(Closure closure) {
        this.parallel = new ParallelDeclaration(failFast);
        executeWith(this.parallel, closure);
    }

    def when(Closure closure) {
        this.when = new WhenDeclaration();
        executeWith(this.when, closure)
    }

    def options(Closure closure) {
        options.add(closure)
    }

    def execute(Script script) {
        String name = this.name
        this.options.each {
            executeWith(script, it)
        }
        if (parallel) {
            parallel.execute(script)
        }

        if (script.currentBuild.result == "FAILURE") {
            executeWith(script, { echo "Stage \"$name\" skipped due to earlier failure(s)" })
            return
        }

        if (!when || when.execute(script)) {
            Map envValuestoRestore = [:]

            // set environment
            if (this.environment) {
                envValuestoRestore = initEnvironment(this.environment, script)
            }

            // TODO handle credentials
            this.stages.entrySet().forEach { stageEntry ->
                stageEntry.value.execute(script)
            }
            if (steps) {
                Closure rehydratedSteps = steps.rehydrate(script, this, steps);
                rehydratedSteps.setResolveStrategy(Closure.DELEGATE_FIRST)
                Closure stageBody = {
                    agent?.execute(script)
                } >> rehydratedSteps
                Closure cl = {
                    stage("$name", stageBody)
                }
                executeWith(script, cl, Closure.DELEGATE_ONLY)
            }
            if (post) {
                this.post.execute(script)
            }
            resetEnvironment(envValuestoRestore, script)
        } else {
            executeWith(script, { echo "Skipping stage $name" })
        }
    }

}
