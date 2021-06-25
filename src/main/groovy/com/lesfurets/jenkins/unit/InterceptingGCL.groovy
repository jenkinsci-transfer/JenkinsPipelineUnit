package com.lesfurets.jenkins.unit

import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration

import static com.lesfurets.jenkins.unit.MethodSignature.method

class InterceptingGCL extends GroovyClassLoader {

    static void interceptClassMethods(MetaClass metaClazz, PipelineTestHelper helper, Binding binding) {
        metaClazz.invokeMethod = helper.getMethodInterceptor()
        metaClazz.static.invokeMethod = helper.getMethodInterceptor()
        metaClazz.methodMissing = helper.getMethodMissingInterceptor()
        metaClazz.propertyMissing = helper.getPropertyMissingInterceptor()
        metaClazz.getEnv = { return binding.env }
        // find and replace script method closure with any matching allowed method closure
        metaClazz.methods.forEach { scriptMethod ->
            def signature = method(scriptMethod.name, scriptMethod.nativeParameterTypes)
            Map.Entry<MethodSignature, Closure> matchingMethod = helper.allowedMethodCallbacks.find { k, v -> k == signature }
            if (matchingMethod) {
                // a matching method was registered, replace script method execution call with the registered closure (mock)
                metaClazz."$scriptMethod.name" = matchingMethod.value
            }
        }
    }

    PipelineTestHelper helper
    Binding binding

    InterceptingGCL(PipelineTestHelper helper,
                    ClassLoader loader,
                    CompilerConfiguration config,
                    Binding binding) {
        super(loader, config)
        this.helper = helper
        this.binding = binding
    }

    @Override
    Class parseClass(GroovyCodeSource codeSource, boolean shouldCacheSource)
            throws CompilationFailedException {
        Class clazz = super.parseClass(codeSource, shouldCacheSource)
        interceptClassMethods(clazz.metaClass, helper, binding)
        return clazz
    }

    @Override
    Class<?> loadClass(String name) throws ClassNotFoundException {
        // Source from: groovy-all-2.4.6-sources.jar!/groovy/lang/GroovyClassLoader.java:710
        Class cls = null
        // try groovy file
        try {
            URL source = resourceLoader.loadGroovySource(name);
            // if recompilation fails, we want cls==null
            cls = recompile(source, name, null);
        } catch (IOException ioe) {
        } finally {
            if (cls == null) {
                removeClassCacheEntry(name);
            } else {
                setClassCacheEntry(cls);
            }
        }

        if (cls == null) {
            // no class found, using parent's method
            return super.loadClass(name)
        }

        interceptClassMethods(cls.metaClass, helper, binding)

        return cls;
    }
}
