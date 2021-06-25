package com.lesfurets.jenkins.unit.declarative

import org.springframework.util.AntPathMatcher

import java.util.regex.Pattern

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith

class WhenDeclaration {

    AllOfDeclaration allOf
    AnyOfDeclaration anyOf
    NotDeclaration not
    Boolean buildingTag
    String branch
    String tag
    Closure<Boolean> expression
    String envName
    String envValue

    private static Pattern getPatternFromGlob(String glob) {
        // from https://stackoverflow.com/a/3619098
        return Pattern.compile('^' + Pattern.quote(glob).replace('*', '\\E.*\\Q').replace('?', '\\E.\\Q') + '$')
    }

    def allOf(Closure closure) {
        this.allOf = new AllOfDeclaration()
        executeWith(this.allOf, closure, Closure.DELEGATE_FIRST)
    }

    def anyOf(Closure closure) {
        this.anyOf = new AnyOfDeclaration();
        executeWith(this.anyOf, closure, Closure.DELEGATE_FIRST);
    }

    def not(Closure closure) {
        this.not = new NotDeclaration();
        executeWith(this.not, closure, Closure.DELEGATE_FIRST)
    }

    def branch (String name) {
        this.branch = name
    }

    def tag (String name) {
        this.tag = getPatternFromGlob(name)
    }

    def buildingTag () {
        this.buildingTag = true
    }

    def expression(Closure closure) {
        this.expression = closure
    }

    def environment(Map args) {
        this.envName = args.name as String
        this.envValue = args.value as String
    }

    Boolean execute(Script script) {
        boolean expressionCheck = true
        boolean branchCheck = true
        boolean tagCheck = true
        boolean envCheck = true
        boolean allOfCheck = true
        boolean anyOfCheck = true
        boolean notCheck = true

        if (allOf) {
            allOfCheck = allOf.execute(script)
        }
        if (anyOf) {
            anyOfCheck = anyOf.execute(script)
        }
        if (not) {
            notCheck = not.execute(script)
        }
        if (expression) {
            expressionCheck = executeWith(script, expression)
        }
        if (branch) {
            AntPathMatcher antPathMatcher = new AntPathMatcher()
            branchCheck = antPathMatcher.match(branch, script.env.BRANCH_NAME)
        }
        if (buildingTag) {
            tagCheck = script?.env?.containsKey("TAG_NAME")
        }
        if (tag) {
            tagCheck = script.env.TAG_NAME =~ tag
        }
        if (envName != null) {
            def val = script?.env[envName]
            envCheck = (val == envValue)
        }

        return expressionCheck && branchCheck && tagCheck && envCheck && allOfCheck && anyOfCheck && notCheck
    }

}
