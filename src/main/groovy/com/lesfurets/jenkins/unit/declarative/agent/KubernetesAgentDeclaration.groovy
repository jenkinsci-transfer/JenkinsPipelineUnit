package com.lesfurets.jenkins.unit.declarative.agent

import com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration
import com.lesfurets.jenkins.unit.declarative.kubernetes.ContainerTemplateDeclaration
import com.lesfurets.jenkins.unit.declarative.kubernetes.WorkspaceVolumeDeclaration
import groovy.transform.Memoized
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith
import static com.lesfurets.jenkins.unit.declarative.ObjectUtils.printNonNullProperties

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class KubernetesAgentDeclaration {

    String label;
    String customWorkspace;
    String cloud;
    String inheritFrom;
    Integer idleMinutes;
    Integer instanceCap;
    String serviceAccount;
    String nodeSelector;
    String namespace;
    String workingDir;
    Integer activeDeadlineSeconds;
    Integer slaveConnectTimeout;
    String podRetention;
    ContainerTemplateDeclaration containerTemplate;
    List<ContainerTemplateDeclaration> containerTemplates;
    String defaultContainer;
    String yaml;
    String yamlFile;
    String yamlMergeStrategy;
    WorkspaceVolumeDeclaration workspaceVolume;
    String supplementalGroups;

    def label(final String label) {
        this.label = label
    }

    def customWorkspace(final String customWorkspace) {
        this.customWorkspace = customWorkspace
    }

    def cloud(final String cloud) {
        this.cloud = cloud
    }

    def inheritFrom(final String inheritFrom) {
        this.inheritFrom = inheritFrom
    }

    def idleMinutes(final int idleMinutes) {
        this.idleMinutes = idleMinutes
    }

    def instanceCap(final int instanceCap) {
        this.instanceCap = instanceCap
    }

    def serviceAccount(final String serviceAccount) {
        this.serviceAccount = serviceAccount
    }

    def nodeSelector(final String nodeSelector) {
        this.nodeSelector = nodeSelector
    }

    def namespace(final String namespace) {
        this.namespace = namespace
    }

    def workingDir(final String workingDir) {
        this.workingDir = workingDir
    }

    def activeDeadlineSeconds(final int activeDeadlineSeconds) {
        this.activeDeadlineSeconds = activeDeadlineSeconds
    }

    def slaveConnectTimeout(final int slaveConnectTimeout) {
        this.slaveConnectTimeout = slaveConnectTimeout
    }

    def podRetention(final String podRetention) {
        this.podRetention = podRetention
    }

    def containerTemplate(Closure closure) {
        this.containerTemplate = new ContainerTemplateDeclaration();
        executeWith(this.containerTemplate, closure)
    }

    def containerTemplates(List<Closure> closures) {
        this.containerTemplates = closures.collect { ctClosure ->
            def containerTemplateDeclaration = new ContainerTemplateDeclaration();
            executeWith(containerTemplateDeclaration, ctClosure)
            return containerTemplateDeclaration;
        } as List<ContainerTemplateDeclaration>
    }

    def defaultContainer(final String defaultContainer) {
        this.defaultContainer = defaultContainer
    }

    def yaml(final String yaml) {
        this.yaml = yaml
    }

    def yamlFile(final String yamlFile) {
        this.yamlFile = yamlFile
    }

    def yamlMergeStrategy(final String yamlMergeStrategy) {
        this.yamlMergeStrategy = yamlMergeStrategy
    }

    def workspaceVolume(Closure closure) {
        this.workspaceVolume = new WorkspaceVolumeDeclaration();
        executeWith(this.workspaceVolume, closure)
    }

    def supplementalGroups(final String supplementalGroups) {
        this.supplementalGroups = supplementalGroups
    }

    @Memoized
    String toString() {
        return printNonNullProperties(this)
    }
}
