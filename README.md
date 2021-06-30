# Jenkins Pipeline Unit testing framework

Jenkins Pipeline Unit is a testing framework for unit testing Jenkins pipelines, written in
[Groovy Pipeline DSL](https://jenkins.io/doc/book/pipeline/).

[![Build Status](https://travis-ci.org/jenkinsci/JenkinsPipelineUnit.svg?branch=master)](https://travis-ci.org/jenkinsci/JenkinsPipelineUnit)
[![Build status](https://ci.appveyor.com/api/projects/status/yx76jwkdgjtky9xu?svg=true)](https://ci.appveyor.com/project/ozangunalp/jenkinspipelineunit)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/jenkinsci/JenkinsPipelineUnit?label=changelog)](https://github.com/jenkinsci/JenkinsPipelineUnit/releases)
[![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/JenkinsPipelineUnit)

If you use Jenkins as your CI workhorse (like us @ [lesfurets.com](https://www.lesfurets.com)) and you enjoy writing _pipeline-as-code_,
you already know that pipeline code is very powerful but can get pretty complex.

This testing framework lets you write unit tests on the configuration and conditional logic of the pipeline code, by providing a mock execution of the pipeline.
You can mock built-in Jenkins commands, job configurations, see the stacktrace of the whole execution and even track regressions.

# Table of Contents
1. [Usage](#usage)
1. [Configuration](#configuration)
1. [Declarative Pipeline](#declarative-pipeline)
1. [Testing Shared Libraries](#testing-shared-libraries)
1. [Note On CPS](#note-on-cps)
1. [Contributing](#contributing)
1. [Demos and Examples](#demos-and-examples)

## Usage

### Add to your project as test dependency
**Note:** Starting from `1.2` artifacts are published to `https://repo.jenkins-ci.org/releases`
Maven:
```xml
    <repositories>
        <repository>
        <id>jenkins-ci-releases</id>
        <url>https://repo.jenkins-ci.org/releases/</url>
        </repository>
        ...
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.lesfurets</groupId>
            <artifactId>jenkins-pipeline-unit</artifactId>
            <version>1.3</version>
            <scope>test</scope>
        </dependency>
        ...
    </dependencies>
```

Gradle:
```groovy
repositories {
  maven { url 'https://repo.jenkins-ci.org/releases/' }
  ...
}

dependencies {
    testImplementation "com.lesfurets:jenkins-pipeline-unit:1.3"
    ...
}
```

### Start writing tests

You can write your tests in Groovy or Java 8, using the test framework you prefer.
The easiest entry point is extending the abstract class `BasePipelineTest`, which initializes the framework with JUnit.

Let's say you wrote this awesome pipeline script, which builds and tests your project :

 ```groovy
def execute() {
    node() {
        def utils = load "src/test/jenkins/lib/utils.jenkins"
        String revision = stage('Checkout') {
            checkout scm
            return utils.currentRevision()
        }
        gitlabBuilds(builds: ["build", "test"]) {
            stage("build") {
                gitlabCommitStatus("build") {
                    sh "mvn clean package -DskipTests -DgitRevision=$revision"
                }
            }

            stage("test") {
                gitlabCommitStatus("test") {
                    sh "mvn verify -DgitRevision=$revision"
                }
            }
        }
    }
}

return this
```

Now using the Jenkins Pipeline Unit you can unit test if it does the job :

```groovy
import com.lesfurets.jenkins.unit.BasePipelineTest

class TestExampleJob extends BasePipelineTest {

        //...

        @Test
        void should_execute_without_errors() throws Exception {
            def script = loadScript("job/exampleJob.jenkins")
            script.execute()
            printCallStack()
        }
}

```

This test will print the call stack of the execution :

```text
   exampleJob.run()
   exampleJob.execute()
      exampleJob.node(groovy.lang.Closure)
         exampleJob.load(src/test/jenkins/lib/utils.jenkins)
            utils.run()
         exampleJob.stage(Checkout, groovy.lang.Closure)
            exampleJob.checkout({$class=GitSCM, branches=[{name=feature_test}], extensions=[], userRemoteConfigs=[{credentialsId=gitlab_git_ssh, url=github.com/lesfurets/JenkinsPipelineUnit.git}]})
            utils.currentRevision()
               utils.sh({returnStdout=true, script=git rev-parse HEAD})
         exampleJob.gitlabBuilds({builds=[build, test]}, groovy.lang.Closure)
            exampleJob.stage(build, groovy.lang.Closure)
               exampleJob.gitlabCommitStatus(build, groovy.lang.Closure)
                  exampleJob.sh(mvn clean package -DskipTests -DgitRevision=bcc19744)
            exampleJob.stage(test, groovy.lang.Closure)
               exampleJob.gitlabCommitStatus(test, groovy.lang.Closure)
                  exampleJob.sh(mvn verify -DgitRevision=bcc19744)
```

### Mock Jenkins variables

You can define both environment variables and job execution parameters.

```groovy
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        // Assigns false to a job parameter ENABLE_TEST_STAGE
        binding.setVariable('ENABLE_TEST_STAGE', 'false')
        // Defines the previous execution status
        binding.getVariable('currentBuild').previousBuild = [result: 'UNSTABLE']
    }
```

The test helper already provides basic variables such as a very simple currentBuild definition.
You can redefine them as you wish.

### Mock Jenkins commands

You can register interceptors to mock pipeline methods, including Jenkins commands, which may or may not return a result.

```groovy
    @Override
    @Before
    void setUp() throws Exception {
        super.setUp()
        helper.registerAllowedMethod("sh", [Map.class], {c -> "bcc19744"})
        helper.registerAllowedMethod("timeout", [Map.class, Closure.class], null)
        helper.registerAllowedMethod("timestamps", [], { println 'Printing timestamp' })
        helper.registerAllowedMethod(method("readFile", String.class), { file ->
            return Files.contentOf(new File(file), Charset.forName("UTF-8"))
        })
        helper.registerAllowedMethod("customMethodWithArguments", [String, int, Collection], { String stringArg, int intArg, Collection collectionArg ->
            return println "executing mock closure with arguments (arguments: '${stringArg}', '${intArg}', '${collectionArg}')"
        })
    }
```

The test helper already includes some mocks, but the list is far from complete.
You need to _register allowed methods_ if you want to override these mocks and add others.
Note that you need to provide a method signature and a callback (closure or lambda) in order to allow a method.
Any method call which is not recognized will throw an exception.

You can take a look at the `BasePipelineTest` class to have the short list of allowed methods.

Some tricky methods such as `load` and `parallel` are implemented directly in the helper.
If you want to override those, make sure that you extend the `PipelineTestHelper` class.

### Mocking readFile and fileExists

The `readFile` and `fileExists` steps can be mocked to return a specific result for a given file name. This can be
useful for testing pipelines for which file operations can influence subsequent steps. An example of such a testing
scenario follows:

```groovy
// Jenkinsfile
node {
    stage('Process output') {
        if (fileExists('output') && readFile('output') == 'FAILED!!!') {
            currentBuild.result = 'FAILURE'
            error 'Build failed'
        }
    }
}
```

```groovy
@Test
void exampleReadFileTest() {
    helper.addFileExistsMock('output', true)
    helper.addReadFileMock('output', 'FAILED!!!')
    runScript("Jenkinsfile")
    assertJobStatusFailure()
}
```

### Mocking sh

The `sh` step is used by many pipelines for a variety of tasks. It can be mocked to either (a) statically return:

- A string for standard output
- A return code

..., or (b) execute a closure that will be executed when `sh` is called before returning a `Map` (with `stdout` and
`exitValue` entries) allowing for dynamic behaviour.

Here is a sample pipeline and corresponding unit tests for each of these variants.

```groovy
// Jenkinsfile
node {
    stage('Mock build') {
        def systemType = sh(returnStdout: true, script: 'uname')
        if (systemType == 'Debian') {
            sh './build.sh --release'
            int status = sh(returnStatus: true, script: './test.sh')
            if (status > 0) {
                currentBuild.result = 'UNSTABLE'
            } else {
                def result = sh(returnStdout: true, script: './processTestResults.sh --platform debian')
                if (!result.endsWith('SUCCESS')) {
                    currentBuild.result = 'FAILURE'
                    error 'Build failed!'
                }
            }
        }
    }
}
```

```groovy
@Test
void debianBuildSuccess() {
    helper.addShMock('uname', 'Debian', 0)
    helper.addShMock('./build.sh --release', '', 0)
    helper.addShMock('./test.sh', '', 0)
    // Have the sh mock execute the closure when the corresponding script is run:
    helper.addShMock('./processTestResults.sh --platform debian') { script ->
        // Do something "dynamically" first...
        return [stdout: "Executing ${script}: SUCCESS", exitValue: 0]
    }

    runScript("Jenkinsfile")

    assertJobStatusSuccess()
}

@Test
void debianBuildUnstable() {
    helper.addShMock('uname', 'Debian', 0)
    helper.addShMock('./build.sh --release', '', 0)
    helper.addShMock('./test.sh', '', 1)

    runScript("Jenkinsfile")

    assertJobStatusUnstable()
}
```

Note that in all cases, the `script` executed by `sh` must *exactly* match the string passed to `helper.addShMock`,
including the script arguments, whitespace etc.

### Analyze the mock execution

The helper registers every method call to provide a stacktrace of the mock execution.

```groovy

@Test
void should_execute_without_errors() throws Exception {
    runScript("Jenkinsfile")
    assertThat(helper.callStack.findAll { call ->
        call.methodName == "sh"
    }.any { call ->
        callArgsToString(call).contains("mvn verify")
    }).isTrue()
    assertJobStatusSuccess()
}

```

This will check as well `mvn verify` has been called during the job execution.


### Check Pipeline status
Let's say you have a simple script and you'd like to check it behaviour if a step is failing
```groovy
// Jenkinsfile
// ...
node() {
    git('some_repo_url')
    sh "make"
}
```

You can mock `sh` step to just update the pipeline status to `FAILURE`.
To verify your pipeline is failing you need to check the status with `BasePipelineTest.assertJobStatusFailure()`
```groovy
class TestCase extends BasePipelineTest {
  @Test
  void check_build_status() throws Exception {
      helper.registerAllowedMethod("sh", [String.class], {cmd->
          // cmd.contains is helpful to filter sh call which should fail the pipeline
          if (cmd.contains("make")) {
              binding.getVariable('currentBuild').result = 'FAILURE'
          }
      })
      runScript("Jenkinsfile")
      assertJobStatusFailure()
  }
}
```


### Check Pipeline exceptions
Sometimes it is useful to verify exactly that exception is thrown during the pipeline run.
For example by one of your `SharedLib` module

To do so you can use `org.junit.rules.ExpectedException`
```groovy
import org.junit.Rule
import org.junit.rules.ExpectedException
// ...
@Rule
public ExpectedException thrown = ExpectedException.none();
```

Here is a simple example to verify exception type and the message:
```groovy
import org.junit.Rule
import org.junit.rules.ExpectedException
class TestCase extends BasePipelineTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    void verify_exception() throws Exception {
        thrown.expect(Exception)
        thrown.expectMessage(containsString("error message"))
        runScript("Jenkinsfile")
    }
}
```

### Compare the callstacks with a previous implementation

One other use of the callstacks is to check your pipeline executions for possible regressions.
You have a dedicated method you can call if you extend `BaseRegressionTest`:

```groovy
    @Test
    void testNonReg() throws Exception {
        def script = loadScript("job/exampleJob.jenkins")
        script.execute()
        super.testNonRegression('example')
    }
```

This will compare the current callstack of the job to the one you have in a text callstack reference file.
To update this file with new callstack, just set this JVM argument when running your tests: `-Dpipeline.stack.write=true`

You then can go ahead and commit this change in your SCM to check in the change.

### Preserve original callstack argument references
The default behavior of the callstack capture is to clone each call's arguments
to preserve their values at time of the call should those arguments mutate
downstream. That is a good guard when your scripts are passing ordinary mutable
variables as arguments.

However, argument types that are not `Cloneable` are captured as `String`
values. Most of the time this is a perfect fallback. But for some complex
types, or for types that don't implement `toString()`, it can be tricky
or impossible to validate the call values in a test.

Take the following simple example.

```groovy
pretendArgsFromFarUpstream = [
    foo: "bar",
    foo2: "more bar please",
    aNestedMap: [ aa: 1, bb: 2, ],
    plusAList: [ 1, 2, 3, 4, ],
].asImmutable()

node() {
    doSomethingWithThis(pretendArgsFromFarUpstream)
}
```

`pretendArgsFromFarUpstream` is a type of uncloneable map and will be recorded
as a `String` in the callstack. Your test may want to perform fine grained
validations via map key referencing instead of pattern matching or similar
parsing. For example,

```groovy
assertEquals(arg.aNestedMap.bb, 2)
```

If you want to perform this kind of validation--particularly if your pipelines
pass `final` and/or immutable variables as arguments--you can retain the
direct reference to the variable in the callstack by setting this switch
in your test setup.

```groovy
       helper.cloneArgsOnMethodCallRegistration = false
```

### Run script inline

In case you want to have some script executed directly within a test case rather
than creating a resource file for it, `loadInlineScript` and `runInlineScript` can be used.

```groovy
    @Test
    void testSomeScript() throws Exception {
        def script = loadInlineScript('''
            node {
                stage('Build') {
                    sh 'make'
                }
            }
        ''')

        script.execute()

        printCallStack()
        assertJobStatusSuccess()
    }
```

Note that inline scripts cannot be debugged via breakpoints as there is no backing files to attach to!

## Configuration

The abstract class `BasePipelineTest` configures the helper with useful conventions:

- It looks for pipeline scripts in your project in root (`./.`) and `src/main/jenkins` paths.
- Jenkins pipelines let you load other scripts from a parent script with `load` command.
However `load` takes the full path relative to the project root.
The test helper mock successfully the `load` command to load the scripts.
To make relative paths work, you need to configure the path of the project where your pipeline scripts are,
which defaults to `.`.
- Pipeline script extension, which defaults to jenkins (matches any `*.jenkins` file)

Overriding these default values is easy:

```groovy

class TestExampleJob extends BasePipelineTest {

    @Override
    @Before
    void setUp() throws Exception {
        baseScriptRoot = 'jenkinsJobs'
        scriptRoots += 'src/main/groovy'
        scriptExtension = 'pipeline'
        super.setUp()
    }

}

```

This will work fine for such a project structure:

```
 jenkinsJobs
 └── src
     ├── main
     │   └── groovy
     │       └── ExampleJob.pipeline
     └── test
         └── groovy
             └── TestExampleJob.groovy
```
## Declarative Pipeline
There is an experimental support of declarative pipeline in `1.3`
To try this feature you need to use `DeclarativePipelineTest` class instead of `BasePipelineTest`

```groovy
// Jenkinsfile
pipeline {
    agent none
    stages {
        stage('Example Build') {
            agent { docker 'maven:3-alpine' }
            steps {
                echo 'Hello, Maven'
                sh 'mvn --version'
            }
        }
        stage('Example Test') {
            agent { docker 'openjdk:8-jre' }
            steps {
                echo 'Hello, JDK'
                sh 'java -version'
            }
        }
    }
}
```

```groovy
import com.lesfurets.jenkins.unit.declarative.*

class TestExampleDeclarativeJob extends DeclarativePipelineTest {
        @Test
        void should_execute_without_errors() throws Exception {
            def script = runScript("Jenkinsfile")
            assertJobStatusSuccess()
            printCallStack()
        }
}
```

### DeclarativePipelineTest
It extends `BasePipelineTest` functionality so you can verify your declarative job the same way
like it was a scripted pipeline

## Testing Shared Libraries

With [Shared Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/) Jenkins lets you share common code
on pipelines across different repositories of your organization.
Shared libraries are configured via a settings interface in Jenkins and imported
with `@Library` annotation in your scripts.

Testing pipeline scripts using external libraries is not trivial because the shared library code
is checked in another repository.
JenkinsPipelineUnit lets you test shared libraries and pipelines depending on these libraries.

Here is an example pipeline using a shared library:

```groovy
@Library('commons')
import net.courtanet.jenkins.Utils

sayHello 'World'

node() {
    stage ('Checkout') {
        def utils = new Utils()
        checkout "${utils.gitTools()}"
    }
    stage ('Build') {
        sh './gradlew build'
    }
    stage ('Post Build') {
        String json = libraryResource 'net/courtanet/jenkins/request.json'
        sh "curl -H 'Content-Type: application/json' -X POST -d '$json' ${acme.url}"
    }
}
```

This pipeline is using a shared library called `commons`.
Now let's test it:

```groovy
// You need to import the class first
import static com.lesfurets.jenkins.unit.global.lib.LibraryConfiguration.library

    String clonePath = 'path/to/clone'

    def library = library()
                    .name('commons')
                    .retriever(gitSource('git@gitlab.admin.courtanet.net:devteam/lesfurets-jenkins-shared.git'))
                    .targetPath(clonePath)
                    .defaultVersion("master")
                    .allowOverride(true)
                    .implicit(false)
                    .build()
    helper.registerSharedLibrary(library)

    runScript("job/library/exampleJob.jenkins")
    printCallStack()
```

Notice how we defined the shared library and registered it to the helper.
Library definition is done via a fluent API which lets you set the same configurations as in
[Jenkins Global Pipeline Libraries](https://jenkins.io/doc/book/pipeline/shared-libraries/#using-libraries).

The `retriever` and `targetPath` fields tell the framework how to fetch the sources of the library, in which local path.
The framework comes with two naive but useful retrievers, `gitSource` and `localSource`.
You can write your own retriever by implementing the `SourceRetriever` interface.

Note that properties `defaultVersion`, `allowOverride` and `implicit` are optional with
default values `master`, `true` and `false`.

Now if we execute this test, the framework will fetch the sources from the Git repository and
load classes, scripts, global variables and resources found in the library.
The callstack of this execution will look like the following:

```text
Loading shared library commons with version master
libraryJob.run()
  libraryJob.sayHello(World)
  sayHello.echo(Hello, World.)
  libraryJob.node(groovy.lang.Closure)
     libraryJob.stage(Checkout, groovy.lang.Closure)
        Utils.gitTools()
        libraryJob.checkout({branch=master})
     libraryJob.stage(Build, groovy.lang.Closure)
        libraryJob.sh(./gradlew build)
     libraryJob.stage(Post Build, groovy.lang.Closure)
        libraryJob.libraryResource(net/courtanet/jenkins/request.json)
        libraryJob.sh(curl -H 'Content-Type: application/json' -X POST -d '{"name" : "Ben"}' http://acme.com)
```
### Library Source Retrievers
There are a few types of `SourceRetriever` implementation in addition to previously
discribed `GitSource` you can use for different applications

#### ProjectSource Retriever
`ProjectSource` retriever is useful if you write tests for the library itself.
So it lets you to load the library files directly from project root folder (where `src`, `vars`, ... are loacted)

```
$ tree -L 1 .
.
├── resources
├── src
└── vars
└── test
└── build.gradle
```

Then you need you can use `projectSource` to point library files location
* `projectSource()` with no args looking for files in project root
* `.defaultVersion('<notNeeded>')` means you can load it in pipelines
   using `commons@master` or `commons@features` which would use the same code base
```groovy
    // TestCase file
    // you need to import static method
    import static com.lesfurets.jenkins.unit.global.lib.ProjectSource.projectSource

    class TestCase extends BasePipelineTest {
        ...
        void setUp() throws Exception {
            ...
            def library = library().name('commons')
                            .defaultVersion('<notNeeded>')
                            .allowOverride(true)
                            .implicit(true)
                            .targetPath('<notNeeded>')
                            .retriever(projectSource())
                            .build()
            helper.registerSharedLibrary(library)
            ...
        }
        ...
    }
```

#### LocalSource Retriever
`LocalSource` retriever is useful if you want to verify how well your library integrates
with the pipelines. For example you may use pre-copied library files of different versions.

```groovy
    import static com.lesfurets.jenkins.unit.global.lib.LocalSource.localSource

    class TestCase extends BasePipelineTest {
        ...
        void setUp() throws Exception {
            ...
            def library = library().name('commons')
                            .defaultVersion("master")
                            .allowOverride(true)
                            .implicit(false)
                            .targetPath('<notNeeded>')
                            .retriever(localSource('/var/tmp/'))
                            .build()
            helper.registerSharedLibrary(library)
        }
        ...
```

The retriever assumes that library files are located at
`/var/tmp/commons@master` folder
```
$ tree -L 1 /var/tmp/commons@master
/var/tmp/commons@master
├── resources
├── src
└── vars
```

### Loading library dynamically
There is a partial support of dynamic library loading.
It does't implement all the features, however sometimes it could be useful.

Pipeline example:
```
def lib = library 'commons'

// by the moment you call library method it enables vars
sayHello 'World'

// creates an instance of a library's class
def utils = net.courtanet.jenkins.Utils.new()

```

Test class example:
```groovy
    String clonePath = 'path/to/clone'

    def library = library()
                    .name('commons')
                    .retriever(gitSource('git@gitlab.admin.courtanet.net:devteam/lesfurets-jenkins-shared.git'))
                    .targetPath(clonePath)
                    .defaultVersion("master")
                    .allowOverride(true)
                    .implicit(false)
                    .build()

    helper.registerSharedLibrary(library)

    // Registration fo pipeline method 'library'
    // should be after you register the shared library
    // so unfortenatly you cannot move it to the super class
    helper.registerAllowedMethod("library", [String.class], {String expression ->
        helper.getLibLoader().loadLibrary(expression)
        println helper.getLibLoader().libRecords
        return new LibClassLoader(helper,null)
    })

    loadScript("job/library/exampleJob.jenkins")
    printCallStack()
```

### Library global variables accepting library class instances as arguments: troubleshooting
You might have a library defining global variables that implement custom steps
accepting library class instances as arguments. For example consider the
following library class and global variable.

```groovy
package org.test

class Monster1 {
    String moniker

    Monster1(String m) {
      moniker = m
    }
}
```

```groovy
import org.test.Monster1

void call(Monster1 m1) {
    echo "$m1.moniker is always very scary"
}
```

Your pipeline uses both as follows.
```groovy
vampire = new Monster1("Dracula")
monster1(vampire)

//Expect "Dracula is always very scary"
```

If this does not yield the expected output but instead throws a
`MissingMethodException` with the cause `No signature of method:
JENKINSFILE.monster1() is applicable for argument types: (org.test.Monster1)
values: [org.test.Monster1@45f50182]` you may need to disable library class
preload in your testing. You can do so in your test setup via the following
switch.

```groovy
helper.libLoader.preloadLibraryClasses = false
```

You may need to do this for on a test-by-test basis as disabling class preload
can cause problems in other use cases. For example, when you have library
classes that require access to the `env` global.

## Note on CPS

If you already fiddled with Jenkins pipeline DSL, you experienced strange errors during execution on Jenkins.
This is because Jenkins does not directly execute your pipeline in Groovy,
but transforms the pipeline code into an intermediate format in order to run Groovy code in
[Continuation Passing Style](https://en.wikipedia.org/wiki/Continuation-passing_style) (CPS).

The usual errors are partly due to the
[the sandboxing Jenkins applies](https://wiki.jenkins-ci.org/display/JENKINS/Script+Security+Plugin#ScriptSecurityPlugin-GroovySandboxing)
for security reasons, and partly due to the
[serializability Jenkins imposes](https://github.com/jenkinsci/pipeline-plugin/blob/master/TUTORIAL.md#serializing-local-variables).

Jenkins requires that at each execution step, the whole script context is serializable, in order to stop and resume the job execution.
To simulate this aspect, CPS versions of the helpers transform your scripts into the CPS format and check if at each step your script context is serializable.

To use this _*experimental*_ feature, you can use the abstract class `BasePipelineTestCPS` instead of `BasePipelineTest`.
You may see some changes in the call stacks that the helper registers.
Note also that the serialization used to test is not the same as what Jenkins uses.
You may find some incoherence in that level.

## Contributing

JenkinsPipelineUnit aims to help devops code and test Jenkins pipelines with a shorter development cycle.
It addresses some of the requirements traced in [JENKINS-33925](https://issues.jenkins-ci.org/browse/JENKINS-33925).
If you are willing to contribute please don't hesitate to discuss in issues and open a pull-request.

## Demos and Examples
| URL | Frameworks and Tools | Test Subject | Test Layers |
|-----|----------------------|--------------|-------------|
| https://github.com/macg33zr/pipelineUnit | Spock, Gradle(Groovy)  | JenkinsFile, scripted pipeline, SharedLibrary | UnitTest |
| https://github.com/mkobit/jenkins-pipeline-shared-library-example | Spock, Gradle (Kotlin), Junit | SharedLibrary | Integration, Unit|
| https://github.com/stchar/pipeline-sharedlib-testharness          | Junit, Gradle(Groovy) | SharedLibrary | Integration, Unit |
| https://github.com/stchar/pipeline-dsl-seed                       | Junit, Spock, Gradle(Groovy) | scripted pipeline | Integration(jobdsl), Unit |
| https://github.com/SpencerMalone/JenkinsPipelineIntegration       | Spock, Gradle(Groovy) | SharedLibrary | Integration |
| https://github.com/venosov/jenkins-pipeline-shared-library-example-victor       | Junit, Gradle(Kotlin) | SharedLibrary | Unit |
