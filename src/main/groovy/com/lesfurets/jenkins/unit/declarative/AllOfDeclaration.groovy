package com.lesfurets.jenkins.unit.declarative

import org.springframework.util.AntPathMatcher

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith

class AllOfDeclaration extends WhenDeclaration {

    List<String> branches = []
    List<Closure> expressions = []
    List<AnyOfDeclaration> anyOfs = []

    def branch(String name) {
        this.branches.add(name)
    }

    def expression(Closure closure) {
        this.expressions.add(closure)
    }

    def anyOf(Closure closure) {
        AnyOfDeclaration anyOfDeclaration = new AnyOfDeclaration();
        this.anyOfs.add(anyOfDeclaration)
        executeWith(anyOfDeclaration, closure, Closure.DELEGATE_FIRST)
    }

    Boolean execute(Script script) {
        List<Boolean> results = []

        AntPathMatcher antPathMatcher = new AntPathMatcher()

        if (this.branches.size() > 0) {
            branches.each { branch ->
                results.add(antPathMatcher.match(branch, script.env.BRANCH_NAME))
            }
        }

        if (this.expressions.size() > 0) {
            results.add(this.expressions.collect { executeWith(script, it) }.every())
        }

        if (this.anyOfs.size() > 0) {
            results.addAll(this.anyOfs.collect {it.execute(script)})
        }

        return results.every()
    }
}
