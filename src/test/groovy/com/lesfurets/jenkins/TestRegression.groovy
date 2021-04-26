package com.lesfurets.jenkins

import org.junit.Assert
import org.junit.Before
import org.junit.Test

import com.lesfurets.jenkins.unit.cps.BaseRegressionTestCPS

class TestRegression extends BaseRegressionTestCPS {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'src/test/jenkins'
        super.setUp()
        def scmBranch = "feature_test"
        helper.registerAllowedMethod("sh", [Map.class], {c -> 'bcc19744'})
        binding.setVariable('scm', [
                        $class                           : 'GitSCM',
                        branches                         : [[name: scmBranch]],
                        extensions                       : [],
                        userRemoteConfigs                : [[
                                                                            credentialsId: 'gitlab_git_ssh',
                                                                            url          : 'github.com/lesfurets/JenkinsPipelineUnit.git'
                                                            ]]
        ])
    }

    @Test
    void testNonReg() throws Exception {
        def script = runScript("job/exampleJob.jenkins")
        script.execute()
        super.testNonRegression("example")
    }

    @Test
    void testNonRegression_ExpectedFileIsCreatedIfItDidNotExist() throws Exception {
        final expectedFile = new File("src/test/resources/callstacks/TestRegression_missing.txt")
        if (expectedFile.isFile()) {
            expectedFile.delete()
        }
        def script = runScript("job/exampleJob.jenkins")
        script.execute()

        super.testNonRegression("missing")

        Assert.assertTrue("The file '${expectedFile}' should have been created", expectedFile.isFile())
        expectedFile.delete()
    }

    @Test
    void testNonRegression_writesActualFileOnFailure() throws Exception {
        final expectedFile = new File("src/test/resources/callstacks/TestRegression_example.txt.actual")
        if (expectedFile.isFile()) {
            expectedFile.delete()
        }
        def script = runScript("job/exampleJob.jenkins")
        helper.registerAllowedMethod("sh", [Map.class], {c -> 'bcc19742'})
        script.execute()

        boolean thrown = false
        try {
            super.testNonRegression("example")
        }
        catch (final AssertionError ignored) {
            thrown = true;
        }

        Assert.assertTrue("testNonRegression should have thrown an AssertionError", thrown)
        Assert.assertTrue("The file '${expectedFile}' should have been created", expectedFile.isFile())
        expectedFile.delete()
    }

    @Test
    void testNonRegression_deletesActualFileFirst() throws Exception {
        final expectedFile = new File("src/test/resources/callstacks/TestRegression_example.txt.actual")
        expectedFile.text = "temporary text"
        def script = runScript("job/exampleJob.jenkins")
        script.execute()

        super.testNonRegression("example")

        Assert.assertFalse("The file '${expectedFile}' should have been deleted", expectedFile.isFile())
    }

}
