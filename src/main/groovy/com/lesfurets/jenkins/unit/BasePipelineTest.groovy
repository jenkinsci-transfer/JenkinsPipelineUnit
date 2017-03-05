package com.lesfurets.jenkins.unit

import static org.assertj.core.api.Assertions.assertThat

abstract class BasePipelineTest {

    PipelineTestHelper helper

    String[] scriptRoots = ['src/main/jenkins', './.']

    String scriptExtension = 'jenkins'

    Map<String, String> imports = ['NonCPS': 'com.cloudbees.groovy.cps.NonCPS']

    String baseScriptRoot = '.'

    Binding binding = new Binding()

    def stringInterceptor = { m -> m.variable }

    def withCredentialsInterceptor = { list, closure ->
        list.forEach {
            binding.setVariable(it, "$it")
        }
        def res = closure.call()
        list.forEach {
            binding.setVariable(it, null)
        }
        return res
    }

    BasePipelineTest() {
        helper = new PipelineTestHelper()
        helper.setScriptRoots scriptRoots
        helper.setScriptExtension scriptExtension
        helper.setBaseClassloader this.class.classLoader
        helper.setImports imports
        helper.setBaseScriptRoot baseScriptRoot
    }

    void setUp() throws Exception {
        helper.build()
        helper.registerAllowedMethod('stage', [String.class, Closure.class], null)
        helper.registerAllowedMethod('stage', [String.class, Closure.class], null)
        helper.registerAllowedMethod('node', [String.class, Closure.class], null)
        helper.registerAllowedMethod('node', [Closure.class], null)
        helper.registerAllowedMethod('sh', [String.class], null)
        helper.registerAllowedMethod('sh', [Map.class], null)
        helper.registerAllowedMethod('checkout', [Map.class], null)
        helper.registerAllowedMethod('echo', [String.class], null)
        helper.registerAllowedMethod('timeout', [Map.class, Closure.class], null)
        helper.registerAllowedMethod('step', [Map.class], null)
        helper.registerAllowedMethod('input', [String.class], null)
        helper.registerAllowedMethod('gitlabCommitStatus', [String.class, Closure.class], null)
        helper.registerAllowedMethod('gitlabBuilds', [Map.class, Closure.class], null)
        helper.registerAllowedMethod('logRotator', [Map.class], null)
        helper.registerAllowedMethod('buildDiscarder', [Object.class], null)
        helper.registerAllowedMethod('pipelineTriggers', [List.class], null)
        helper.registerAllowedMethod('properties', [List.class], null)
        helper.registerAllowedMethod('dir', [String.class, Closure.class], null)
        helper.registerAllowedMethod('archiveArtifacts', [Map.class], null)
        helper.registerAllowedMethod('junit', [String.class], null)
        helper.registerAllowedMethod('readFile', [String.class], null)
        helper.registerAllowedMethod('disableConcurrentBuilds', [], null)
        helper.registerAllowedMethod('gatlingArchive', [], null)
        helper.registerAllowedMethod('string', [Map.class], stringInterceptor)
        helper.registerAllowedMethod('withCredentials', [List.class, Closure.class], withCredentialsInterceptor)
        helper.registerAllowedMethod('error', [String.class], { updateBuildStatus('FAILURE')})

        binding.setVariable('currentBuild', [result: 'SUCCESS'])
    }

    /**
     * Updates the build status.
     * Can be useful when mocking a jenkins method.
     * @param status job status to set
     */
    void updateBuildStatus(String status){
        binding.getVariable('currentBuild').result = status
    }

    Script loadScript(String scriptName) {
        return helper.loadScript(scriptName, this.binding)
    }

    void printCallStack() {
        if (!Boolean.parseBoolean(System.getProperty('printstack.disabled'))) {
            helper.callStack.each {
                println it
            }
        }
    }

    /**
     * Asserts the job status is FAILURE.
     * Please check the mocks update this status when necessary.
     * @See #updateBuildStatus(String)
     */
    void assertJobStatusFailure() {
        assertJobStatus('FAILURE')
    }

    /**
     * Asserts the job status is UNSTABLE.
     * Please check the mocks update this status when necessary
     * @See #updateBuildStatus(String)
     */
    void assertJobStatusUnstable() {
        assertJobStatus('UNSTABLE')
    }

    /**
     * Asserts the job status is SUCCESS.
     * Please check the mocks update this status when necessary
     * @See #updateBuildStatus(String)
     */
    void assertJobStatusSuccess() {
        assertJobStatus('SUCCESS')
    }

    private assertJobStatus(String status) {
        assertThat(binding.getVariable('currentBuild').result).isEqualTo(status)
    }

}
