package com.lesfurets.jenkins.unit.declarative.agent

import groovy.transform.Memoized
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.ObjectUtils.printNonNullProperties

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class DockerAgentDeclaration {

    String label
    String args = ""
    String registryUrl
    String registryCredentialsId
    String customWorkspace
    boolean reuseNode
    boolean containerPerStageRoot
    String image

    def label(final String label) {
        this.label = label
    }

    def args(final String args) {
        this.args = args
    }

    def registryUrl(final String registryUrl) {
        this.registryUrl = registryUrl
    }

    def registryCredentialsId(final String registryCredentialsId) {
        this.registryCredentialsId = registryCredentialsId
    }

    def customWorkspace(final String customWorkspace) {
        this.customWorkspace = customWorkspace
    }

    def reuseNode(final boolean reuseNode) {
        this.reuseNode = reuseNode
    }

    def containerPerStageRoot(final boolean containerPerStageRoot) {
        this.containerPerStageRoot = containerPerStageRoot
    }

    def image(String image) {
        this.image = image
    }

    @Memoized
    String toString() {
        return printNonNullProperties(this)
    }
}
