package com.lesfurets.jenkins

import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource
import static org.junit.Assert.assertTrue

class TestRunClosure extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
    }

    @Test
    void should_execute_without_errors() throws Exception {
        runScript({ script ->
            echo 'Test'
        })
        printCallStack()
    }

    @Test
    void should_print_property_value() {
        runScript({ script ->
            println 'value'
        })

        def value = 'value'
        assertTrue(helper.callStack.findAll { call ->
            call.methodName == 'println'
        }.any { call ->
            callArgsToString(call).contains(value)
        })
    }

    @Test
    void should_use_registered_method() {
        helper.registerAllowedMethod("customMethod", [Map.class], null)
        runScript({ script ->
            customMethod test: 'value'
        })
    }

    @Test
    void should_use_library_sayHello() {
        String sharedLibs = this.class.getResource('/libs/commons@master').getFile()
        def library = library().name('commons')
                .defaultVersion('<notNeeded>')
                .targetPath('<notNeeded>')
                .implicit(true)
                .retriever(projectSource(sharedLibs))
                .build()
        helper.registerSharedLibrary(library)
        runScript({ script ->
            sayHello()
        })
    }
}
