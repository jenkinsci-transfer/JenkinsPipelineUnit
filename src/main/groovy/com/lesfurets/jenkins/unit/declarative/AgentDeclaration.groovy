package com.lesfurets.jenkins.unit.declarative

import com.lesfurets.jenkins.unit.declarative.agent.DockerAgentDeclaration
import com.lesfurets.jenkins.unit.declarative.agent.KubernetesAgentDeclaration
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class AgentDeclaration {

    String label
    DockerAgentDeclaration docker
    KubernetesAgentDeclaration kubernetes
    Boolean dockerfile = null
    String dockerfileDir
    Boolean reuseNode = null
    String customWorkspace

    def label(String label) {
        this.label = label
    }

    def node(Closure closure) {
        closure.call()
    }

    def customWorkspace(String workspace) {
        this.customWorkspace = workspace
    }

    def reuseNode(boolean reuse) {
        this.reuseNode = reuse
    }

    def docker(String image) {
        this.docker = new DockerAgentDeclaration().with{  da -> da.image = image; da }
    }

    def docker(Closure closure) {
        this.docker = new DockerAgentDeclaration();
        executeWith(this.docker, closure);
    }

    def kubernetes(Object kubernetesAgent) {
        this.@kubernetes = kubernetesAgent as KubernetesAgentDeclaration
    }

    def kubernetes(Closure closure) {
        this.@kubernetes = new KubernetesAgentDeclaration();
        def kubernetesDecl = this.@kubernetes
        executeWith(kubernetesDecl, closure, Closure.DELEGATE_FIRST)
    }

    def dockerfile(boolean dockerfile) {
        this.dockerfile = dockerfile
    }

    def dockerfile(Closure closure) {
        closure.call()
    }

    def dir(String dir) {
        this.dockerfileDir = dir
    }

    def execute(Script script) {
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
        executeWith(script, { echo "Executing on agent $agentDesc" })
    }
}
