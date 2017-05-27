/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package com.lesfurets.jenkins

import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.junit.Before
import org.junit.ComparisonFailure
import org.junit.Test

import com.lesfurets.jenkins.unit.cps.BasePipelineTestCPS

import static org.assertj.core.api.Assertions.assertThat

class TestFailingJobs extends BasePipelineTestCPS {

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'src/test/jenkins'
        super.setUp()
    }

    @Test(expected = GroovyCastException)
    void should_fail_nonCpsCallingCps() throws Exception {
        def script = loadScript("job/shouldFail/nonCpsCallingCps.jenkins")
        printCallStack()
    }

    /**
     * java.lang.UnsupportedOperationException: Calling public static java.util.List
     * org.codehaus.groovy.runtime.DefaultGroovyMethods.each(java.util.List,groovy.lang.Closure)
     * on a CPS-transformed closure is not yet supported (JENKINS-26481);
     * encapsulate in a @NonCPS method, or use Java-style loops
     */
    @Test(expected = ComparisonFailure)
    void should_fail_forEach() throws Exception {
        def script = loadScript("job/shouldFail/forEach.jenkins")
        printCallStack()
        assertThat(helper.callStack.count { it.toString().contains("println")}).isEqualTo(3)
    }
}
