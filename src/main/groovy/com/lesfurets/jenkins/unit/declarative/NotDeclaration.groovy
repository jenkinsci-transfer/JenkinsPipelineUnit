package com.lesfurets.jenkins.unit.declarative

class NotDeclaration extends WhenDeclaration {

    Boolean execute(Script script) {
        return !super.execute(script)
    }
}

