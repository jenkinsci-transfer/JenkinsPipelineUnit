package com.lesfurets.jenkins.unit.declarative.kubernetes

class ContainerLivenessProbeDeclaration {

    String execArgs
    int timeoutSeconds
    int initialDelaySeconds
    int failureThreshold
    int periodSeconds
    int successThreshold

    def execArgs(final String execArgs) {
        this.execArgs = execArgs
    }

    def timeoutSeconds(final int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds
    }

    def initialDelaySeconds(final int initialDelaySeconds) {
        this.initialDelaySeconds = initialDelaySeconds
    }

    def failureThreshold(final int failureThreshold) {
        this.failureThreshold = failureThreshold
    }

    def periodSeconds(final int periodSeconds) {
        this.periodSeconds = periodSeconds
    }

    def successThreshold(final int successThreshold) {
        this.successThreshold = successThreshold
    }
}
