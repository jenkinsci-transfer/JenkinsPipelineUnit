package com.lesfurets.jenkins.unit.declarative.kubernetes

import com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration
import groovy.transform.Memoized
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration.executeWith
import static com.lesfurets.jenkins.unit.declarative.ObjectUtils.printNonNullProperties

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class ContainerTemplateDeclaration {

    String name;
    String image;
    boolean privileged;
    Long runAsUser;
    Long runAsGroup;
    boolean alwaysPullImage;
    String workingDir;
    String command;
    String args;
    boolean ttyEnabled;
    String resourceRequestCpu;
    String resourceRequestMemory;
    String resourceLimitCpu;
    String resourceLimitMemory;
    String shell;
    final List<TemplateEnvVarDeclaration> envVars;
    List<PortMappingDeclaration> ports;
    ContainerLivenessProbeDeclaration livenessProbe;

    def name(final String name) {
        this.name = name
    }

    def image(final String image) {
        this.image = image
    }

    def privileged(final boolean privileged) {
        this.privileged = privileged
    }

    def runAsUser(final Long runAsUser) {
        this.runAsUser = runAsUser
    }

    def runAsGroup(final Long runAsGroup) {
        this.runAsGroup = runAsGroup
    }

    def alwaysPullImage(final boolean alwaysPullImage) {
        this.alwaysPullImage = alwaysPullImage
    }

    def workingDir(final String workingDir) {
        this.workingDir = workingDir
    }

    def command(final String command) {
        this.command = command
    }

    def args(final String args) {
        this.args = args
    }

    def ttyEnabled(final boolean ttyEnabled) {
        this.ttyEnabled = ttyEnabled
    }

    def resourceRequestCpu(final String resourceRequestCpu) {
        this.resourceRequestCpu = resourceRequestCpu
    }

    def resourceRequestMemory(final String resourceRequestMemory) {
        this.resourceRequestMemory = resourceRequestMemory
    }

    def resourceLimitCpu(final String resourceLimitCpu) {
        this.resourceLimitCpu = resourceLimitCpu
    }

    def resourceLimitMemory(final String resourceLimitMemory) {
        this.resourceLimitMemory = resourceLimitMemory
    }

    def shell(final String shell) {
        this.shell = shell
    }

    def ports(List<Closure> closures) {
        this.ports = closures.each { ct ->
            def portMappingDeclaration = new PortMappingDeclaration()
            executeWith(portMappingDeclaration, closure)
            return portMappingDeclaration
        } as List<PortMappingDeclaration>
    }

    def livenessProbe(Closure closure) {
        this.livenessProbe = new ContainerLivenessProbeDeclaration();
        executeWith(this.livenessProbe, closure)
    }

    @Memoized
    String toString() {
        return printNonNullProperties(this)
    }
}
