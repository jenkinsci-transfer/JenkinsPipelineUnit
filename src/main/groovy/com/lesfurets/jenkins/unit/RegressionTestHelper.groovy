package com.lesfurets.jenkins.unit

import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.fail

import java.nio.charset.StandardCharsets
import java.util.function.Function
import java.util.stream.Collectors

import org.assertj.core.api.SoftAssertions

class RegressionTestHelper {

    public static final String ARGUMENT_MISMATCH = """Method arguments does not match for method '%s' on '%s':
    A : %s
    B : %s
    """
    public static final String METHOD_CALL_COUNT = """Method call '%s' does not have the same number of calls" +
    expected: %d, actual %d."""

    public static final String METHOD_CALL_NOT_EXISTS = "Method call '%s' does not exist on second stack"
    public static final String PIPELINE_STACK_WRITE = 'pipeline.stack.write'

    static void testNonRegression(PipelineTestHelper helper, String targetFileName) {
        targetFileName += '.txt'
        def referenceFile = new File(targetFileName)
        def pipelineStackWrite = System.getProperty(PIPELINE_STACK_WRITE)
        if (!referenceFile.isFile() || (pipelineStackWrite && Boolean.valueOf(pipelineStackWrite))) {
            writeStackToFile(referenceFile, helper)
        }

        String callStack = helper.callStack.join('\n') + '\n'
        final expected = referenceFile.text.normalize()
        final actual = callStack.normalize()
        final actualFileName = targetFileName + ".actual"
        final actualFile = new File(actualFileName)
        if (actualFile.isFile()) {
            actualFile.delete()
        }
        if (!expected.equals(actual)) {
            writeStackToFile(actualFile, helper)
        }
        assertThat(actual)
                        .as('If you intended to update the callstack, use JVM parameter -D%s=true', PIPELINE_STACK_WRITE)
                        .isEqualTo(expected)
    }

    private static writeStackToFile(File referenceFile, PipelineTestHelper helper) {
        println "Saving stack into ${referenceFile.path}"
        if (!new File(referenceFile.parent).exists()) {
            new File(referenceFile.parent).mkdirs()
        }
        referenceFile.withWriter(StandardCharsets.UTF_8.name()) { out ->
            helper.callStack.each {
                out.println(it)
            }
        }
    }

    /**
     * For all method calls in stack a, there is a corresponding method call in stack b
     * @param aCallStack call stack a
     * @param bCallStack call stack b
     */
    static SoftAssertions compareCallStack(List<MethodCall> aCallStack, List<MethodCall> bCallStack) {
        Map<MethodSignature, List<MethodCall>> aSignatureToCall = groupBySignature(aCallStack)
        Map<MethodSignature, List<MethodCall>> bSignatureToCall = groupBySignature(bCallStack)

        SoftAssertions softly = new SoftAssertions()

        aSignatureToCall.entrySet().stream()
                        .filter { e -> e.key.name != "run" }
                        .filter { e -> e.key.name != "load" }
                        .forEach { e ->
            // Strict check method call exists
            assertThat(bSignatureToCall)
                            .overridingErrorMessage(METHOD_CALL_NOT_EXISTS, e.key)
                            .containsKey(e.key)
            // Strict check method call count
            assertThat(e.value.args)
                            .overridingErrorMessage(METHOD_CALL_COUNT,
                            e.key,
                            e.value.args.size(),
                            bSignatureToCall.get(e.key).size())
                            .hasSameSizeAs(bSignatureToCall.get(e.key))
            // Softly check method call argument values
            e.value.eachWithIndex { aCall, i ->
                MethodCall bCall = bSignatureToCall.get(e.key).get(i)
                softly.assertThat(argsToNormalizedString(aCall) == argsToNormalizedString(bCall))
                                .overridingErrorMessage(ARGUMENT_MISMATCH,
                                aCall.methodName,
                                aCall.target,
                                aCall.args.toArrayString(),
                                bCall.args.toArrayString())
                                .isTrue()
            }
        }
        return softly
    }

    private static String argsToNormalizedString(MethodCall c) {
        callArgsToString(c).replaceAll(" +", " ")
    }

    private static Map<MethodSignature, List<MethodCall>> groupBySignature(List<MethodCall> aCallStack) {
        return aCallStack.stream().collect(Collectors.groupingBy((Function) { MethodCall c -> c.toSignature() }))
    }
}
