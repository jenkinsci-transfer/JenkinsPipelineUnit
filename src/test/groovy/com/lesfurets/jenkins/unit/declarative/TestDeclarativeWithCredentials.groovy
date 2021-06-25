package com.lesfurets.jenkins.unit.declarative

import com.lesfurets.jenkins.unit.declarative.DeclarativePipelineTest
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertTrue

class TestDeclarativeWithCredentials extends DeclarativePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'src/test/jenkins/jenkinsfiles'
        scriptExtension = ''
        super.setUp()
    }

    @Test
    void should_run_script_with_credentials() {
        // when:
        runScript("withCredentials_Jenkinsfile")

        // then:
        assertJobStatusSuccess()
        assertCallStack().contains("echo(User/Pass = user/pass)")
        assertCallStack().contains("echo(Nested User/Pass = user/pass)")
        assertCallStack().contains("echo(Restored User/Pass = user/pass)")
        assertCallStack().contains("echo(Cleared User/Pass = null/null)")

        assertCallStack().contains("echo(Docker = docker_pass)")
        assertCallStack().contains("echo(Nested Docker = docker_pass)")
        assertCallStack().contains("echo(Restored Docker = docker_pass)")
        assertCallStack().contains("echo(Cleared Docker = null)")

        assertCallStack().contains("echo(SSH = ssh_pass)")
        assertCallStack().contains("echo(Nested SSH = ssh_pass)")
        assertCallStack().contains("echo(Restored SSH = ssh_pass)")
        assertCallStack().contains("echo(Cleared SSH = null)")


    }
}
