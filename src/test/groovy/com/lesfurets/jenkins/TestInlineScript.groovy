package com.lesfurets.jenkins

import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library
import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

import org.junit.Before
import org.junit.Test

import com.lesfurets.jenkins.unit.BasePipelineTest

class TestInlineScript extends BasePipelineTest {

    String sharedLibs = this.class.getResource('/libs').getFile()

    @Override
    @Before
    void setUp() throws Exception {
        scriptRoots += 'src/test/jenkins'
        super.setUp()

        def library = library()
                        .name('commons')
                        .defaultVersion('master')
                        .allowOverride(true)
                        .implicit(false)
                        .targetPath(sharedLibs)
                        .retriever(projectSource())
                        .build()

        helper.registerSharedLibrary(library)
    }

    void load_inline_script_with_simple_commands() {
        def script = loadInlineScript('''
            node {
                echo 'Test'
            }
        ''')

        script.run()

        printCallStack()
        assertJobStatusSuccess()
    }

    void run_inline_script_with_simple_commands() {
        runInlineScript('''
            node {
                echo 'Test'
            }
        ''')

        printCallStack()
        assertJobStatusSuccess()
    }

    void load_inline_script_with_shared_library() {
        def script = loadInlineScript('''
            @Library('commons') _

            node {
                sayHello()
            }
        ''')

        script.run()

        printCallStack()
        assertJobStatusSuccess()
    }

    void run_inline_script_with_shared_library() {
        runInlineScript('''
            @Library('commons') _

            node {
                sayHello()
            }
        ''')

        printCallStack()
        assertJobStatusSuccess()
    }
}
