package com.lesfurets.jenkins.unit

import static com.lesfurets.jenkins.unit.MethodSignature.method

import java.lang.reflect.Method
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.function.Consumer
import java.util.function.Function

import org.apache.commons.io.IOUtils
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.codehaus.groovy.runtime.InvokerHelper
import org.codehaus.groovy.runtime.MetaClassHelper

import com.lesfurets.jenkins.unit.global.lib.LibraryAnnotationTransformer
import com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration
import com.lesfurets.jenkins.unit.global.lib.LibraryLoader

class PipelineTestHelper {

    protected static Method SCRIPT_SET_BINDING = Script.getMethod('setBinding', Binding.class)

    /**
     * Simple container for holding mock script output.
     */
    class MockScriptOutput {
        String stdout = null
        int exitValue = -1
        Closure callback = null

        MockScriptOutput(String stdout, int exitValue) {
            this.stdout = stdout
            this.exitValue = exitValue
        }

        MockScriptOutput(Closure callback) {
            this.callback = callback
        }
    }

    /** Holds configured mock output values for the `sh` command. */
    Map<String, MockScriptOutput> mockScriptOutputs = [:]

    /**
     * Search paths for scripts
     */
    String[] scriptRoots

    /**
     * Base path for script roots.
     * Usually the path to the project.
     */
    String baseScriptRoot

    /**
     * Extension for script files.
     * Ex. jenkins
     */
    String scriptExtension

    /**
     * Base class for instantiated scripts
     */
    Class scriptBaseClass = MockPipelineScript.class

    /**
     * Classloader to instantiate scripts
     */
    ClassLoader baseClassloader

    /**
     * Default imports for scripts loaded by this helper
     */
    Map<String, String> imports = ['Library': 'com.lesfurets.jenkins.unit.global.lib.Library']

    /**
     * Global Shared Libraries to be loaded with scripts if necessary
     * @see LibraryLoader
     */
    Map<String, LibraryConfiguration> libraries = [:]

    /**
     * Stack of method calls of scripts loaded by this helper
     */
    List<MethodCall> callStack = []

    /**
     * Controls whether method call arguments are cloned when pushed onto the
     * call stack. Disabling cloning can be useful for arguments of types that
     * are not {@code Cloneable}. Beware, however, arguments that mutate
     * during a test may appear with incorrect values in the call stack if
     * you disable cloning.
     */
    Boolean cloneArgsOnMethodCallRegistration = true

    /**
     * Internal script engine
     */
    protected GroovyScriptEngine gse

    /**
     * Loader for shared global libraries
     */
    LibraryLoader libLoader

    /** Let scripts and library classes access global vars (env, currentBuild) */
    protected Binding binding

    Map<String, Boolean> mockFileExistsResults = [:]

    Map<String, String> mockReadFileOutputs = [:]

    /**
    * Get library loader object
    *
    * @return return library loader
    * @see LibraryLoader
    */
    LibraryLoader getLibLoader() {
        return this.libLoader
    }

    /**
     * Method interceptor for method 'load' to load scripts via encapsulated GroovyScriptEngine
     */
    protected loadInterceptor = { args ->
        String name = args
        // The script is loaded by its normal name :
        def relativize = Paths.get(baseScriptRoot).relativize(Paths.get(name)).normalize()
        if (relativize.toFile().exists()) {
            name = relativize.toString()
        } else {
            // The script is loaded from its full name :
            scriptRoots.eachWithIndex { it, i ->
                def resolved = Paths.get(baseScriptRoot, it).resolve(name).normalize()
                if (resolved.toFile().exists()) {
                    name = resolved.toString()
                }
            }
        }
        return this.runScript(name, ((Script) delegate).binding)
    }

    /**
     * Method interceptor for method 'parallel'
     */
    protected parallelInterceptor = { Map m ->
        // If you have many steps in parallel and one of the step in Jenkins fails, the other tasks keep runnning in Jenkins.
        // Since here the parallel steps are executed sequentially, we are hiding the error to let other steps run
        // and we make the job failing at the end.
        List<String> exceptions = []
        m.entrySet().stream()
                        .filter { Map.Entry<String, Closure> entry -> entry.key != 'failFast' }
                        .forEachOrdered { Map.Entry<String, Closure> entry ->
            String parallelName = entry.key
            Closure closure = entry.value
            def result = null
            try {
                result = callClosure(closure)
            } catch (e) {
                delegate.binding.currentBuild.result = 'FAILURE'
                exceptions.add("$parallelName - ${e.getMessage()}")
            }
            return result
        }
        if (exceptions) {
            throw new RuntimeException(exceptions.join(','))
        }
    }

    /**
     * Method interceptor for any method called in executing script.
     * Calls are logged on the call stack.
     */
    public methodInterceptor = { String name, Object[] args ->
        // register method call to stack
        int depth = Thread.currentThread().stackTrace.findAll { it.className == delegate.class.name }.size()
        this.registerMethodCall(delegate, depth, name, args)
        // check if it is to be intercepted
        def intercepted = this.getAllowedMethodEntry(name, args)
        if (intercepted != null && intercepted.value) {
            intercepted.value.delegate = delegate
            return callClosure(intercepted.value, args)
        }
        // if not search for the method declaration
        MetaMethod metaMethod = delegate.metaClass.getMetaMethod(name, args)
        // ...and call it. If we cannot find it, delegate call to methodMissing
         def result = (metaMethod ? this.callMethod(metaMethod, delegate, args) : delegate.metaClass.invokeMissingMethod(delegate, name, args))
        return result
    }

    /**
     * Call given method on delegate object with args parameters
     *
     * @param method method to call
     * @param delegate object of the method call
     * @param args method call parameters
     * @return return value of the object
     */
    protected Object callMethod(MetaMethod method, Object delegate, Object[] args) {
        return method.doMethodInvoke(delegate, args)
    }

    def getMethodInterceptor() {
        return methodInterceptor
    }

    /**
     * Method for calling custom allowed methods
     */
    def methodMissingInterceptor = { String name, args ->
        if (this.isMethodAllowed(name, args)) {
            def result = null
            if (args != null) {
                for (argument in args) {
                    result = this.callIfClosure(argument, result)
                    if (argument instanceof Map) {
                        argument.each { k, v ->
                            result = this.callIfClosure(k, result)
                            result = this.callIfClosure(v, result)
                        }
                    }
                }
            }
            return result
        } else {
            throw new MissingMethodException(name, delegate.class, args)
        }
    }

    def getMethodMissingInterceptor() {
        return methodMissingInterceptor
    }

    def propertyMissingInterceptor = { String propertyName ->
        if (binding.hasVariable("params") && (binding.getVariable("params") as Map).containsKey(propertyName)) {
            return (binding.getVariable("params") as Map).get(propertyName)
        }
        if (binding.getVariable("env") && (binding.getVariable("env") as Map).containsKey(propertyName)) {
            return (binding.getVariable("env") as Map).get(propertyName)
        }
        throw new MissingPropertyException(propertyName)
    }

    def getPropertyMissingInterceptor() {
        return propertyMissingInterceptor
    }

    def callIfClosure(Object closure, Object currentResult) {
        if (closure instanceof Closure) {
            currentResult = callClosure(closure)
        }
        return currentResult
    }

    /**
     * Method interceptor for 'libraryResource' in Shared libraries
     * The resource from shared library should have been added to the url classloader in advance
     */
    def libraryResourceInterceptor = { m ->
        def stream = gse.groovyClassLoader.getResourceAsStream(m as String)
        if (stream) {
            def string = IOUtils.toString(stream, Charset.forName("UTF-8"))
            IOUtils.closeQuietly(stream)
            return string
        } else {
            throw new GroovyRuntimeException("Library Resource not found with path $m")
        }
    }

    /**
     * List of allowed methods with default interceptors.
     * Complete this list in need with {@link #registerAllowedMethod}
     */
    protected Map<MethodSignature, Closure> allowedMethodCallbacks = [
                    (method("load", String.class))           : loadInterceptor,
                    (method("parallel", Map.class))          : parallelInterceptor,
                    (method("libraryResource", String.class)): libraryResourceInterceptor,
    ]

    PipelineTestHelper() {
    }

    PipelineTestHelper(String[] scriptRoots,
                       String scriptExtension,
                       Class scriptBaseClass,
                       Map<String, String> imports,
                       ClassLoader baseClassloader, String baseScriptRoot) {
        this.scriptRoots = scriptRoots
        this.scriptExtension = scriptExtension
        this.scriptBaseClass = scriptBaseClass
        this.imports = imports
        this.baseClassloader = baseClassloader
        this.baseScriptRoot = baseScriptRoot
    }

    PipelineTestHelper init() {
        CompilerConfiguration configuration = new CompilerConfiguration()
        GroovyClassLoader cLoader = new InterceptingGCL(this, baseClassloader, configuration, binding)

        libLoader = new LibraryLoader(cLoader, libraries)
        LibraryAnnotationTransformer libraryTransformer = new LibraryAnnotationTransformer(libLoader)
        configuration.addCompilationCustomizers(libraryTransformer)

        ImportCustomizer importCustomizer = new ImportCustomizer()
        imports.each { k, v -> importCustomizer.addImport(k, v) }
        configuration.addCompilationCustomizers(importCustomizer)

        configuration.setDefaultScriptExtension(scriptExtension)
        configuration.setScriptBaseClass(scriptBaseClass.getName())

        gse = new GroovyScriptEngine(scriptRoots, cLoader)
        gse.setConfig(configuration)

        mockScriptOutputs.clear()
        mockFileExistsResults.clear()
        mockReadFileOutputs.clear()
        return this
    }

    /**
     *
     * @return true if internal GroovyScriptEngine is set
     */
    boolean isInitialized() {
        return gse != null
    }

    /**
     * Clone argments for registerMethodCall so they are recorded
     * at the time the method is called in case of changes to an
     * arg variable later in the pipeline.
     *
     * @param args original args passed to the method call
     * @return cloned args
     */
    private Object[] cloneArgs(Object[] args) {

        List argsCloned = []
        args.each {
            try {
                // Try the clone
                argsCloned << it?.clone()
            }
            catch(e) {
                // Cannot clone it, get a string representation at this point.
                argsCloned << it.toString()
            }
        }
        return argsCloned as Object[]
    }

    /**
     * Register method call to call stack
     * @param target target object
     * @param stackDepth depth in stack
     * @param name method name
     * @param args method arguments
     */
    protected void registerMethodCall(Object target, int stackDepth, String name, Object... args) {
        MethodCall call = new MethodCall()
        call.target = target
        call.methodName = name
        if (cloneArgsOnMethodCallRegistration) {
            call.args = cloneArgs(args)
        } else {
            call.args = args
        }
        call.stackDepth = stackDepth
        callStack.add(call)
    }

    /**
     * Search for the allowed method entry <MethodSignature, Closure>
     *     A null Closure will mean that the method is allowed but not intercepted.
     * @param name method name
     * @param args parameter objects
     * @return Map.Entry corresponding to the method <MethodSignature, Closure>
     */
    protected Map.Entry<MethodSignature, Closure> getAllowedMethodEntry(String name, Object... args) {
        Class[] paramTypes = MetaClassHelper.castArgumentsToClassArray(args)
        MethodSignature signature = method(name, paramTypes)
        return allowedMethodCallbacks.find { k, v -> k == signature }
    }

    /**
     *
     * @param name method name
     * @param args parameter objects
     * @return true if method is allowed in this helper
     */
    protected boolean isMethodAllowed(String name, args) {
        return getAllowedMethodEntry(name, args) != null
    }

    /**
     * Load the script with name and empty binding, returning the Script
     * @param name path of the script
     * @return Script object
     */
    Script loadScript(String name) {
        return this.loadScript(name, new Binding())
    }

    /**
     * Load the script with given binding context without running, returning the Script
     * @param scriptName
     * @param binding
     * @return Script object
     */
    Script loadScript(String scriptName, Binding binding) {
        Objects.requireNonNull(binding, "Binding cannot be null.")
        Objects.requireNonNull(gse, "GroovyScriptEngine is not initialized: Initialize the helper by calling init().")
        Class scriptClass = gse.loadScriptByName(scriptName)
        setGlobalVars(binding)
        Script script = InvokerHelper.createScript(scriptClass, binding)
        InterceptingGCL.interceptClassMethods(script.metaClass, this, binding)
        return script
    }

    /**
     * Load and run the script, returning the result value;
     * @param scriptName
     * @param binding
     * @return the return value of the script
     */
    Object runScript(String scriptName, Binding binding) {
        return runScriptInternal(loadScript(scriptName, binding))
    }

    /**
     * Load and run the script, returning the result value;
     * @param scriptName
     * @return the return value of the script
     */
    Object runScript(String scriptName) {
        return runScriptInternal(loadScript(scriptName, new Binding()))
    }

    /**
     * Run the given script object
     * @param Script object
     * @return the return value of the script
     */
    Object runScript(Script script) {
        return this.runScriptInternal(script)
    }

    /**
     * Run the script
     * @param script
     * @return the return value of the script
     */
    protected Object runScriptInternal(Script script) {
        return script.run()
    }

    /**
     * Sets global variables defined in loaded libraries on the binding
     * @param binding
     */
    public void setGlobalVars(Binding binding) {
        libLoader.libRecords.values().stream()
                        .flatMap { it.definedGlobalVars.entrySet().stream() }
                        .forEach { e ->
            if (e.value instanceof Script) {
                Script script = Script.cast(e.value)
                // invoke setBinding from method to avoid interception
                SCRIPT_SET_BINDING.invoke(script, binding)
                script.metaClass.getMethods().findAll { it.name == 'call' }.forEach { m ->
                    this.registerAllowedMethod(method(e.value.class.name, m.getNativeParameterTypes()),
                        { args ->
                            // When calling a one argument method with a null argument the
                            // Groovy doMethodInvoke appears to incorrectly assume a zero
                            // argument call signature for the method yielding an IllegalArgumentException
                            if (args == null && m.getNativeParameterTypes().size() == 1) {
                                m.doMethodInvoke(e.value, MetaClassHelper.ARRAY_WITH_NULL)
                            } else {
                                m.doMethodInvoke(e.value, args)
                            }
                        })
                }
            }
            binding.setVariable(e.key, e.value)
        }
    }

    /**
     * @param name method name
     * @param closure method implementation, can be null
     */
    void registerAllowedMethod(String name, Closure closure = null) {
        allowedMethodCallbacks.put(method(name), closure)
    }

    /**
     * @param name method name
     * @param args parameter types
     * @param closure method implementation, can be null
     */
    void registerAllowedMethod(String name, List<Class> args, Closure closure = null) {
        allowedMethodCallbacks.put(method(name, args.toArray(new Class[args?.size()])), closure)
    }

    /**
     * Register a callback implementation for a method
     * Calls from the loaded scripts to allowed methods will call the given implementation
     * Null callbacks will only log the call and do nothing
     * @param methodSignature method signature
     * @param closure method implementation, can be null
     */
    void registerAllowedMethod(MethodSignature methodSignature, Closure closure = null) {
        allowedMethodCallbacks.put(methodSignature, closure)
    }

    /**
     *
     * @param methodSignature
     * @param callback
     */
    void registerAllowedMethod(MethodSignature methodSignature, Function callback) {
        this.registerAllowedMethod(methodSignature,
                        callback != null ? { params -> return callback.apply(params) } : null)
    }

    /**
     *
     * @param methodSignature
     * @param callback
     */
    void registerAllowedMethod(MethodSignature methodSignature, Consumer callback) {
        this.registerAllowedMethod(methodSignature,
                        callback != null ? { params -> return callback.accept(params) } : null)
    }

    /**
     * Register library description
     * See {@link LibraryConfiguration} for its description
     * @param libraryDescription to add
     */
    void registerSharedLibrary(LibraryConfiguration libraryDescription) {
        Objects.requireNonNull(libraryDescription)
        Objects.requireNonNull(libraryDescription.name)
        this.libraries.put(libraryDescription.name, libraryDescription)
    }

    /**
     * Clear call stack
     */
    void clearCallStack() {
        callStack.clear()
    }

    /**
     * Count the number of calls to the method with name
     * @param name method name
     * @return call number
     */
    long methodCallCount(String name) {
        callStack.stream().filter { call ->
            call.methodName == name
        }.count()
    }

    /**
     * Call closure by handling spreading of parameter default values
     *
     * @param closure to call
     * @param args array of arguments passed to this closure call. Is null by default.
     * @return result of the closure call
     */
    Object callClosure(Closure closure, Object[] args = null) {
        // When we use a library method, we should not spread the argument because we define a closure with a single
        // argument. The arguments will be spread in this closure (See PipelineTestHelper#setGlobalVars)
        // For other cases, we spread it before calling
        // Note : InvokerHelper.invokeClosure(intercepted.value, args) is similar to closure.call(*args)
        if (!args) {
            return closure.call()
        } else if (args.size() > closure.maximumNumberOfParameters) {
            return closure.call(args)
        } else {
            return closure.call(*args)
        }
    }

    void addFileExistsMock(String file, Boolean result) {
        mockFileExistsResults[file] = result
    }

    Boolean fileExists(String arg) {
        return mockFileExistsResults[arg] ?: false
    }

    void addReadFileMock(String file, String contents) {
        mockReadFileOutputs[file] = contents
    }

    String readFile(def args) {
        String file = null
        if (args instanceof String || args instanceof GString) {
            file = args
        } else if (args instanceof Map) {
            file = args['file']
        }
        assert file

        return mockReadFileOutputs[file] ?: ''
    }

    /**
     * Configure mock output for the `sh` command. This function should be called before
     * attempting to call `JenkinsMocks.sh()`.
     * @param script Script command to mock.
     * @param stdout Standard output text to return for the given command.
     * @param exitValue Exit value for the command.
     */
    void addShMock(String script, String stdout, int exitValue) {
        mockScriptOutputs[script] = new MockScriptOutput(stdout, exitValue)
    }

    /**
     * Configure mock callback for the `sh` command. This function should be called before
     * attempting to call `JenkinsMocks.sh()`.
     * @param script Script command to mock.
     * @param callback Closure to be called when the mock is executed. This closure will be
     *                 passed the script call which is being executed, and
     *                 <strong>must</strong> return a {@code Map} with the following
     *                 key/value pairs:
     *                 <ul>
     *                   <li>{@code stdout}: {@code String} with the mocked output.</li>
     *                   <li>{@code exitValue}: {@code int} with the mocked exit value.</li>
     *                 </ul>
     */
    void addShMock(String script, Closure callback) {
        mockScriptOutputs[script] = new MockScriptOutput(callback)
    }

    @SuppressWarnings('ThrowException')
    def runSh(def args) {
        String script = null
        boolean returnStdout = false
        boolean returnStatus = false

        // The `sh` function can be called with either a string, or a map of key/value pairs.
        if (args instanceof String || args instanceof GString) {
            script = args
        } else if (args instanceof Map) {
            script = args['script']
            returnStatus = args['returnStatus'] ?: false
            returnStdout = args['returnStdout'] ?: false
            if (returnStatus && returnStdout) {
                throw new IllegalArgumentException('returnStatus and returnStdout are mutually exclusive options')
            }
        }
        assert script

        MockScriptOutput output = mockScriptOutputs[script]
        if (!output) {
            if (returnStatus) {
                return 0
            }
            // If no output is given, we return these strings for backwards-compatibility. Ideally at some point in the
            // future, we should make a breaking change and remove this special use-case and either raise an exception
            // here or return an empty string.
            if(returnStdout && script.contains("git rev-parse HEAD")) {
                return 'abcd123\n'
            } else {
                return '\nbbb\nccc\n'
            }
        }

        String stdout
        int exitValue

        // If the callback closure is not null, execute it and grab the output.
        if (output.callback) {
            Map callbackOutput
            try {
                callbackOutput = output.callback(script)
            } catch (GroovyCastException) {
                throw new IllegalArgumentException("Mocked sh callback for ${script} was not a map")
            }
            if (!callbackOutput.containsKey('stdout') || !(callbackOutput['stdout'] instanceof String)) {
                throw new IllegalArgumentException("Mocked sh callback for ${script} did not contain a valid value for the stdout key")
            }
            if (!callbackOutput.containsKey('exitValue') || !(callbackOutput['exitValue'] instanceof Integer)) {
                throw new IllegalArgumentException("Mocked sh callback for ${script} did not contain a valid value for the exitValue key")
            }
            stdout = callbackOutput['stdout']
            exitValue = callbackOutput['exitValue']
        } else {
            stdout = output.stdout
            exitValue = output.exitValue
        }

        // Jenkins also prints the output from sh when returnStdout is true if the script fails
        if (!returnStdout || exitValue != 0) {
            println stdout
        }

        if (returnStdout && exitValue == 0) {
            return stdout
        }
        if (returnStatus) {
            return exitValue
        }
        if (exitValue != 0) {
            throw new Exception('Script returned error code: ' + exitValue)
        }
        return null
    }

}
