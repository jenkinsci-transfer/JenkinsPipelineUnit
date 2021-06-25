package com.lesfurets.jenkins.unit.declarative

import org.springframework.util.AntPathMatcher

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith

class AnyOfDeclaration extends WhenDeclaration {

    List<String> tags = []
    List<String> branches = []
    List<Closure> expressions = []
    List<AllOfDeclaration> allOfs = []

    def tag(String name) {
        this.tags.add(name)
    }

    def branch(String name) {
        this.branches.add(name)
    }

    def expression(Closure closure) {
        this.expressions.add(closure)
    }

    def allOf(Closure closure) {
        AllOfDeclaration allOfDeclaration = new AllOfDeclaration();
        this.allOfs.add(allOfDeclaration)
        executeWith(allOfDeclaration, closure, Closure.DELEGATE_FIRST)
    }

    Boolean execute(Script script) {
        def results = []

        AntPathMatcher antPathMatcher = new AntPathMatcher()

        if (this.tags.size() > 0) {
            tags.each { tag ->
                results.add(antPathMatcher.match(tag, script.env.TAG_NAME))
            }
        }

        if (this.branches.size() > 0) {
            branches.each { branch ->
                results.add(antPathMatcher.match(branch, script.env.BRANCH_NAME))
            }
        }

        if (this.expressions.size() > 0) {
            results.add(this.expressions.collect {executeWith(delegate, it)}.any())
        }

        if (this.allOfs.size() > 0) {
            results.addAll(this.allOfs.collect {it.execute(script)})
        }

        return results.any()
    }
}
