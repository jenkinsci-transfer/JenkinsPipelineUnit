package com.lesfurets.jenkins.unit.declarative

import com.lesfurets.jenkins.unit.BasePipelineTest

import static com.lesfurets.jenkins.unit.MethodSignature.method

@groovy.transform.InheritConstructors
abstract class DeclarativePipelineTest extends BasePipelineTest {

    def pipelineInterceptor = { Closure closure ->
        GenericPipelineDeclaration.binding = binding
        GenericPipelineDeclaration.createComponent(DeclarativePipeline, closure).execute(delegate)
    }

    def stringInterceptor = { Map param ->
        // we are in context of withCredentials([string()..]) { }
        if(param?.variable) {
            addParam(param.name, param.defaultValue, false)
            return param.variable
        } else {
            return param
        }
    }

    @Override
    void setUp() throws Exception {
        super.setUp()
        helper.registerAllowedMethod('booleanParam', [Map], { booleanParam -> booleanParam })
        helper.registerAllowedMethod('checkout', [Closure])
        helper.registerAllowedMethod('credentials', [String], { String credName ->
            return binding.getVariable('credentials')[credName]
        })
        helper.registerAllowedMethod('cron', [String])
        helper.registerAllowedMethod('input', [Closure])
        helper.registerAllowedMethod('message', [String])
        helper.registerAllowedMethod(method("pipeline", Closure), pipelineInterceptor)
        helper.registerAllowedMethod('pollSCM', [String])
        helper.registerAllowedMethod('script', [Closure])
        helper.registerAllowedMethod('skipDefaultCheckout')
        helper.registerAllowedMethod('string', [Map], stringInterceptor)
        helper.registerAllowedMethod('timeout', [Integer, Closure])
        helper.registerAllowedMethod('timestamps')
        helper.registerAllowedMethod('password', [Map], { Map param ->
            String obscuredValue = param.containsKey("value") ? param.get("value").replaceAll(".","*") : null
            param.put("value", obscuredValue)
            return param
        })
        binding.setVariable('credentials', [:])
        binding.setVariable('params', [:])
    }
}
