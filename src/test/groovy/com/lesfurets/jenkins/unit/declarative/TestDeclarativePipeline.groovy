package com.lesfurets.jenkins.unit.declarative

import org.junit.Before
import org.junit.Test

class TestDeclarativePipeline extends DeclarativePipelineTest {

    @Before
    @Override
    void setUp() throws Exception {
        scriptRoots += 'src/test/jenkins/jenkinsfiles'
        scriptExtension = ''
        super.setUp()
    }

    @Test void jenkinsfile_success() throws Exception {
        def script = runScript('Declarative_Jenkinsfile')
        printCallStack()
        assertCallStackContains('pipeline unit tests PASSED')
        assertCallStackContains('pipeline unit tests completed')
        assertCallStackContains('pipeline unit tests post CLEANUP')
        assertCallStack().doesNotContain('pipeline unit tests UNSUCCESSFUL')
        assertCallStackContains('Skipping stage Checkout')
        assertJobStatusSuccess()
    }

    @Test void jenkinsfile_failure() throws Exception {
        helper.registerAllowedMethod('sh', [String.class], { String cmd ->
            updateBuildStatus('FAILURE')
        })
        runScript('Declarative_Jenkinsfile')
        printCallStack()
        assertJobStatusFailure()
        assertCallStack()
        assertCallStack().contains('pipeline unit tests FAILED')
        assertCallStackContains('pipeline unit tests post CLEANUP')
        assertCallStack().contains('pipeline unit tests UNSUCCESSFUL')
        assertCallStackContains('pipeline unit tests completed')
    }

    @Test void jenkinsfile_aborted() throws Exception {
        helper.registerAllowedMethod('sh', [String.class], { String cmd ->
            updateBuildStatus('ABORTED')
        })
        runScript('Declarative_Jenkinsfile')
        printCallStack()
        assertJobStatusAborted()
        assertCallStack()
        assertCallStack().contains('pipeline unit tests ABORTED')
        assertCallStackContains('pipeline unit tests post CLEANUP')
        assertCallStack().contains('pipeline unit tests UNSUCCESSFUL')
        assertCallStackContains('pipeline unit tests completed')
    }

    @Test void jenkinsfile_fixed() throws Exception {
        helper.registerAllowedMethod('sh', [String.class], { String cmd ->
            addPreviousBuild('FAILURE')
            updateBuildStatus('SUCCESS')
        })
        runScript('Declarative_Jenkinsfile')
        printCallStack()
        assertJobStatusSuccess()
        assertCallStack()
        assertCallStackContains('pipeline unit tests PASSED')
        assertCallStackContains('pipeline unit tests completed')
        assertCallStackContains('pipeline unit tests post CLEANUP')
        assertCallStackContains('pipeline unit tests results have CHANGED')
        assertCallStackContains('pipeline unit tests have been FIXED')
    }

    @Test void jenkinsfile_regression() throws Exception {
        helper.registerAllowedMethod('sh', [String.class], { String cmd ->
            addPreviousBuild('SUCCESS')
            updateBuildStatus('UNSTABLE')
        })
        runScript('Declarative_Jenkinsfile')
        printCallStack()
        assertJobStatusUnstable()
        assertCallStack()

        assertCallStackContains('pipeline unit tests completed')
        assertCallStackContains('pipeline unit tests have gone UNSTABLE')
        assertCallStackContains('pipeline unit tests results have CHANGED')
        assertCallStackContains('pipeline unit tests UNSUCCESSFUL')
        assertCallStackContains('pipeline unit tests post REGRESSION')
        assertCallStackContains('pipeline unit tests post CLEANUP')

    }

    @Test void should_params() throws Exception {
        runScript('Params_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Hello Mr Jenkins')
        assertJobStatusSuccess()
    }

    @Test void when_not_branch_master() throws Exception {
        addEnvVar('BRANCH_NAME', 'dev')
        runScript('not_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Running')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_branch_not() throws Exception {
        addEnvVar('BRANCH_NAME', 'master')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example anyOf branch')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_branch_release() throws Exception {
        addEnvVar('BRANCH_NAME', 'release')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with branch')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_branch_production() throws Exception {
        addEnvVar('BRANCH_NAME', 'production')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with branch')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_branch_pattern_main() throws Exception {
        addEnvVar('BRANCH_NAME', 'main-2')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with branch')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_branch_pattern_main_not() throws Exception {
        addEnvVar('BRANCH_NAME', 'main2')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example anyOf branch')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_tag_version_pattern() throws Exception {
        addEnvVar('TAG_NAME', 'version-X.Y.Z')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with tag')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_tag_release_pattern() throws Exception {
        addEnvVar('TAG_NAME', 'release-X.Y.Z')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with tag')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_tag_release_pattern_main_not() throws Exception {
        addEnvVar('TAG_NAME', 'releaseX.Y.Z')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example anyOf tag')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_expression_not() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'false')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_anyOf_expression() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'true')
        runScript('AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing anyOf with expression')
        assertJobStatusSuccess()
    }

    @Test void when_allOf_expression() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'true')
        addEnvVar('SHOULD_ALSO_EXECUTE', 'true')
        runScript('AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing allOf with expression')
        assertJobStatusSuccess()
    }

    @Test void when_allOf_expression_first_false() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'false')
        addEnvVar('SHOULD_ALSO_EXECUTE', 'true')
        runScript('AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_allOf_expression_second_false() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'true')
        addEnvVar('SHOULD_ALSO_EXECUTE', 'false')
        runScript('AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_allOf_expression_both_false() throws Exception {
        addEnvVar('SHOULD_EXECUTE', 'false')
        addEnvVar('SHOULD_ALSO_EXECUTE', 'false')
        runScript('AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_1() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'false')
        addEnvVar('OPTIONAL_4', 'false')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_2() throws Exception {
        addEnvVar('OPTIONAL_1', 'true')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'false')
        addEnvVar('OPTIONAL_4', 'false')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_3() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'true')
        addEnvVar('OPTIONAL_3', 'false')
        addEnvVar('OPTIONAL_4', 'false')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_4() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'true')
        addEnvVar('OPTIONAL_4', 'false')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_5() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'false')
        addEnvVar('OPTIONAL_4', 'true')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_6() throws Exception {
        addEnvVar('OPTIONAL_1', 'true')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'true')
        addEnvVar('OPTIONAL_4', 'false')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_allOf_anyOf_expression_7() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'true')
        addEnvVar('OPTIONAL_3', 'false')
        addEnvVar('OPTIONAL_4', 'true')
        runScript('Nested_AllOf_And_AnyOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when allOf anyOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_anyOf_allOf_expression() throws Exception {
        addEnvVar('OPTIONAL_1', 'true')
        addEnvVar('OPTIONAL_2', 'true')
        addEnvVar('OPTIONAL_3', 'true')
        runScript('Nested_AnyOf_And_AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when anyOf allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_anyOf_allOf_expression_2() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'true')
        addEnvVar('OPTIONAL_3', 'true')
        runScript('Nested_AnyOf_And_AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when anyOf allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_anyOf_allOf_expression_3() throws Exception {
        addEnvVar('OPTIONAL_1', 'true')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'true')
        runScript('Nested_AnyOf_And_AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when anyOf allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_anyOf_allOf_expression_4() throws Exception {
        addEnvVar('OPTIONAL_1', 'true')
        addEnvVar('OPTIONAL_2', 'true')
        addEnvVar('OPTIONAL_3', 'false')
        runScript('Nested_AnyOf_And_AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Executing nested when anyOf allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_nested_when_anyOf_allOf_expression_5() throws Exception {
        addEnvVar('OPTIONAL_1', 'false')
        addEnvVar('OPTIONAL_2', 'false')
        addEnvVar('OPTIONAL_3', 'false')
        runScript('Nested_AnyOf_And_AllOf_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example nested when anyOf allOf expression')
        assertJobStatusSuccess()
    }

    @Test void when_branch() throws Exception {
        addEnvVar('BRANCH_NAME', 'production')
        runScript('Branch_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Deploying')
        assertJobStatusSuccess()
    }

    @Test void when_branch_not() throws Exception {
        addEnvVar('BRANCH_NAME', 'master')
        runScript('Branch_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example Deploy')
        assertJobStatusSuccess()
    }

    @Test void when_buildingTag() throws Exception {
        addEnvVar('TAG_NAME', 'release-1.0.0')
        runScript('Tag_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Generating Release Notes')
        assertJobStatusSuccess()
    }

    @Test void when_buildingTag_not() throws Exception {
        // no TAG_NAME variable defined
        runScript('Tag_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example Release Notes') // No stage bound to a "buildingTag()" condition
        assertCallStack().contains('Skipping stage Example Deploy') // No stage bound to a "tag <arg>" condition
        assertJobStatusSuccess()
    }

    @Test void when_tag() throws Exception {
        addEnvVar('TAG_NAME', 'v1.1.1')
        runScript('Tag_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Deploying')
        assertJobStatusSuccess()
    }

    @Test void when_tag_not() throws Exception {
        addEnvVar('TAG_NAME', 'someothertag')
        runScript('Tag_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Skipping stage Example Deploy')
        assertJobStatusSuccess()
    }

    @Test void should_agent() throws Exception {
        runScript('Agent_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('openjdk:8-jre')
        assertCallStack().contains('maven:3-alpine')
        assertJobStatusSuccess()
    }

    @Test void should_kubernetes_agent() throws Exception {
        runScript('Kubernetes_Agent_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('namespace: "jenkins"')
        assertCallStack().contains('image: jenkins/slave')
        assertJobStatusSuccess()
    }

    @Test void should_kubernetes_map_agent() throws Exception {
        runScript('Kubernetes_Map_Agent_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('namespace: "jenkins"')
        assertCallStack().contains('image: jenkins/slave')
        assertJobStatusSuccess()
    }

    @Test void should_credentials() throws Exception {
        addCredential('my-prefined-secret-text', 'something_secret')
        runScript('Credentials_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('AN_ACCESS_KEY:something_secret')
        assertJobStatusSuccess()
    }

    @Test void should_parallel() throws Exception {
        runScript('Parallel_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('sh(run-tests.exe)')
        assertCallStack().contains('sh(run-tests.sh)')
        assertJobStatusSuccess()
    }

    @Test void should_parallel_nested_stages() throws Exception {
        runScript('Parallel_NestedStages_Jenkinsfile')
        printCallStack()
        assertJobStatusSuccess()
    }

    @Test void should_sub_stages() throws Exception {
        runScript('ComplexStages_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('mvn build')
        assertCallStack().contains('mvn --version')
        assertCallStack().contains('java -version')
        assertJobStatusSuccess()
    }

    @Test void should_environment() throws Exception {
        runScript('Environment_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('echo(LEVAR1 LE NEW VALUE)')
        assertCallStack().contains('echo(LEVAR2 A COPY OF LE NEW VALUE in build#1)')
        assertJobStatusSuccess()
    }

    @Test void not_running_stage_after_failure() throws Exception {
        runScript('StageFailed_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('Stage "Not executed stage" skipped due to earlier failure(s)')
        assertJobStatusFailure()
    }

    @Test(expected = MissingPropertyException)
    void should_non_valid_fail() throws Exception {
        try {
            runScript('Non_Valid_Jenkinsfile')
        } catch (e) {
            e.printStackTrace()
            throw e
        } finally {
            printCallStack()
        }
    }

    @Test void should_not_leave_environment_dirty() throws Exception {
        runScript('WithEnv_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('echo(SOMEVAR inside closure = SOMEVAL)')
        assertCallStack().contains('echo(SOMEVAR overlapping inside closure = SOMEVAL)')
        assertCallStack().contains('echo(SOMEVAR restored inside closure = SOMEVAL)')
        assertCallStack().contains('echo(SOMEVAR outside closure = null)')
    }

    @Test void agent_with_param_label() throws Exception {
        runScript('AgentParam_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('aSlave')
        assertJobStatusSuccess()
    }

    @Test void agent_with_mock_param_label() throws Exception {
        def params = binding.getVariable('params')
        params['AGENT'] = 'mockSlave'
        runScript('AgentParam_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('mockSlave')
        assertJobStatusSuccess()
    }

    @Test void should_agent_with_environment() throws Exception {
        def env = binding.getVariable('env')
        env['ENV_VAR'] = 'ENV VAR_VALUE'
        env['some_env_var'] = 'some env var_value'
        runScript('Agent_env_Jenkinsfile')
        printCallStack()
        assertJobStatusSuccess()
    }

    @Test void should_agent_with_bindings() throws Exception {
        final def some_var = 'someVar'
        binding.setVariable('var', some_var)
        binding.setVariable('binding_var', some_var)

        runScript('Agent_bindings_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('[label:someVar]')
        assertCallStack().contains('docker:[', ', image:someVar, ', ']')
        assertCallStack().contains('[label:someLabel]')
        assertCallStack().contains('echo(Deploy to someLabel)')
        assertJobStatusSuccess()
    }

    @Test
    void should_log_downstream_var() throws Exception {
        runScript('DownstreamBuild_Jenkinsfile')
        printCallStack()
        assertCallStack().contains('build({job=stop, parameters=[{name=stringVarName, value=stringVarValue}, {name=secretVarName, value=***********}, {name=booleanVarName, value=true}], wait=true}')
        assertJobStatusSuccess()

    }

}
