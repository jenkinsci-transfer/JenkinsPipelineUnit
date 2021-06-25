package com.lesfurets.jenkins.unit.declarative.kubernetes

class TemplateEnvVarDeclaration {

    KeyValueVar containerEnvVar;
    KeyValueVar envVar;
    KeyValueVar podEnvVar;
    KeyValueVar secretEnvVar;

    def containerEnvVar(final KeyValueVar containerEnvVar) {
        this.containerEnvVar = containerEnvVar
    }

    def envVar(final KeyValueVar envVar) {
        this.envVar = envVar
    }

    def podEnvVar(final KeyValueVar podEnvVar) {
        this.podEnvVar = podEnvVar
    }

    def secretEnvVar(final KeyValueVar secretEnvVar) {
        this.secretEnvVar = secretEnvVar
    }

    class KeyValueVar {
        String key
        String value

        def key(final String key) {
            this.key = key
        }

        def value(final String value) {
            this.value = value
        }
    }

    class SecretVar {
        String key
        String secretName
        String secretKey
        boolean optional

        def key(final String key) {
            this.key = key
        }

        def secretName(final String secretName) {
            this.secretName = secretName
        }

        def secretKey(final String secretKey) {
            this.secretKey = secretKey
        }

        def optional(final boolean optional) {
            this.optional = optional
        }
    }
}
