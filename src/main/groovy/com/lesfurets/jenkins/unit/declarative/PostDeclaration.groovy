package com.lesfurets.jenkins.unit.declarative

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith

class PostDeclaration {

    Closure always
    Closure changed
    Closure success
    Closure unstable
    Closure failure
    Closure aborted
    Closure unsuccessful
    Closure cleanup
    Closure fixed
    Closure regression

    def always(Closure closure) {
        this.always = closure
    }

    def changed(Closure closure) {
        this.changed = closure
    }

    def success(Closure closure) {
        this.success = closure
    }

    def unstable(Closure closure) {
        this.unstable = closure
    }
    
    def unsuccessful(Closure closure) {
        this.unsuccessful = closure
    }

    def failure(Closure closure) {
        this.failure = closure
    }

    def aborted(Closure closure) {
        this.aborted = closure
    }

    def cleanup(Closure closure) {
        this.cleanup = closure
    }

    def fixed(Closure closure) {
        this.fixed = closure
    }

    def regression(Closure closure){
        this.regression = closure
    }

    def execute(Script script) {
        def currentBuild = script.currentBuild.result
        def previousBuild = script.currentBuild?.previousBuild?.result
        if (this.always) {
            executeWith(script, this.always)
        }

        switch (currentBuild) {
            case 'SUCCESS':
                executeWith(script, this.success)
                break
            case 'FAILURE':
                executeWith(script, this.failure)
                break
            case 'ABORTED':
                executeWith(script,this.aborted)
                break
            case 'UNSTABLE':
                executeWith(script,this.unstable)
                break
        }

        if(currentBuild != previousBuild && this.changed)
        {
            executeWith(script,this.changed)
        }
        if(currentBuild != 'SUCCESS' && this.unsuccessful)
        {
            executeWith(script, this.unsuccessful)
        }
        if(this.fixed){
            if(currentBuild == 'SUCCESS' && (previousBuild == 'FAILURE' || previousBuild == 'UNSTABLE'))
            {
                executeWith(script, this.fixed)
            }
        }
        if(this.regression)
        {
            if((currentBuild == 'FAILURE' || currentBuild == 'UNSTABLE') && previousBuild == 'SUCCESS'){
                executeWith(script, this.regression)
            }
        }

        // Cleanup is always performed last
        if(this.cleanup){
            executeWith(script, this.cleanup)
        }
    }

}
