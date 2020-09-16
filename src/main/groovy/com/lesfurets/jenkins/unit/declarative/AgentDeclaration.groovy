package com.lesfurets.jenkins.unit.declarative

import com.lesfurets.jenkins.unit.declarative.agent.DockerAgentDeclaration
import com.lesfurets.jenkins.unit.declarative.agent.KubernetesAgentDeclaration
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.DeclarativePipeline.createComponent
import static com.lesfurets.jenkins.unit.declarative.DeclarativePipeline.executeWith
import static groovy.lang.Closure.DELEGATE_ONLY

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class AgentDeclaration {

    String label
    DockerAgentDeclaration docker
    KubernetesAgentDeclaration kubernetes
    Boolean dockerfile = null
    String dockerfileDir
    Boolean reuseNode = null
    String customWorkspace
    def binding = null

    def label(String label) {
        this.label = label
    }

    def node(@DelegatesTo(AgentDeclaration) Closure closure) {
        closure.call()
    }

    def customWorkspace(String workspace) {
        this.customWorkspace = workspace
    }

    def reuseNode(boolean reuse) {
        this.reuseNode = reuse
    }

    def docker(String image) {
        this.docker = new DockerAgentDeclaration().with { it.image = image; it }
    }

    def docker(@DelegatesTo(strategy = DELEGATE_ONLY, value = DockerAgentDeclaration) Closure closure) {
        this.docker = createComponent(DockerAgentDeclaration, closure)
    }

    def kubernetes(Object kubernetesAgent) {
        this.@kubernetes = kubernetesAgent as KubernetesAgentDeclaration
    }

    def kubernetes(@DelegatesTo(strategy = DELEGATE_ONLY, value = KubernetesAgentDeclaration) Closure closure) {
        this.@kubernetes = createComponent(KubernetesAgentDeclaration, closure)
    }

    def dockerfile(boolean dockerfile) {
        this.dockerfile = dockerfile
    }

    def dockerfile(@DelegatesTo(AgentDeclaration) Closure closure) {
        closure.call()
    }

    def dir(String dir) {
        this.dockerfileDir = dir
    }

    def getCurrentBuild() {
        return binding?.currentBuild
    }

    def getEnv() {
        return binding?.env
    }

    def getParams() {
        return binding?.params
    }

    def execute(Object delegate) {
        def agentDesc = null

        if (label) {
            agentDesc = '[label:' + label.toString() + ']'
        }
        else if (docker) {
            agentDesc = '[docker:' + docker.toString() + ']'
        }
        else if (dockerfile) {
            agentDesc = '[dockerfile:' + dockerfile.toString() + ']'
        }
        else if (dockerfileDir && dockerfileDir.exists()) {
            agentDesc = '[dockerfileDir:' + dockerfileDir.toString() + ']'
        }
        else if (kubernetes) {
            agentDesc = '[kubernetes:' + kubernetes.toString() + ']'
        }
        else {
            throw new IllegalStateException("No agent description found")
        }
        executeWith(delegate, { echo "Executing on agent $agentDesc" })
    }
}
