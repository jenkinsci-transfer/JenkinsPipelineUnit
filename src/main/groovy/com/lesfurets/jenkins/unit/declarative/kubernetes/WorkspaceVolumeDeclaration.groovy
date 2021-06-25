package com.lesfurets.jenkins.unit.declarative.kubernetes

import com.lesfurets.jenkins.unit.declarative.GenericPipelineDeclaration
import groovy.transform.Memoized
import groovy.transform.ToString

import static com.lesfurets.jenkins.unit.declarative.ObjectUtils.printNonNullProperties

@ToString(includePackage = false, includeNames = true, ignoreNulls = true)
class WorkspaceVolumeDeclaration {

    DynamicWorkspaceVolume dynamicPVC
    EmptyDirWorkspaceVolume emptyDirWorkspaceVolume
    HostPathWorkspaceVolume hostPathWorkspaceVolume
    NfsWorkspaceVolume nfsWorkspaceVolume
    PersistentVolumeClaimWorkspaceVolume persistentVolumeClaimWorkspaceVolume

    void dynamicPVC(Closure closure) {
        this.dynamicPVC = new DynamicWorkspaceVolume();
        GenericPipelineDeclaration.executeWith(this.dynamicPVC, closure);
    }

    void emptyDirWorkspaceVolume(Closure closure) {
        this.emptyDirWorkspaceVolume = new EmptyDirWorkspaceVolume();
        closure.delegate= this.emptyDirWorkspaceVolume;
        closure.call();
    }

    void hostPathWorkspaceVolume(Closure closure) {
        this.hostPathWorkspaceVolume = new HostPathWorkspaceVolume();
        GenericPipelineDeclaration.executeWith(this.hostPathWorkspaceVolume, closure);

    }

    void nfsWorkspaceVolume(Closure closure) {
        nfsWorkspaceVolume = new NfsWorkspaceVolume();
        GenericPipelineDeclaration.executeWith(this.nfsWorkspaceVolume, closure);
    }

    void persistentVolumeClaimWorkspaceVolume(Closure closure) {
        persistentVolumeClaimWorkspaceVolume = new PersistentVolumeClaimWorkspaceVolume();
        GenericPipelineDeclaration.executeWith(this.persistentVolumeClaimWorkspaceVolume, closure);
    }

    @Memoized
    String toString() {
        return printNonNullProperties(this)
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
    class DynamicWorkspaceVolume {
        String accessModes
        String requestsSize
        String storageClassName

        def accessModes(final String accessModes) {
            this.accessModes = accessModes
        }

        def requestsSize(final String requestsSize) {
            this.requestsSize = requestsSize
        }

        def storageClassName(final String storageClassName) {
            this.storageClassName = storageClassName
        }

        @Memoized
        String toString() {
            return printNonNullProperties(this)
        }
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
    class EmptyDirWorkspaceVolume {
        boolean memory

        def memory(final boolean memory) {
            this.memory = memory
        }
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
    class HostPathWorkspaceVolume {
        String hostPath

        def hostPath(final String hostPath) {
            this.hostPath = hostPath
        }

        @Memoized
        String toString() {
            return printNonNullProperties(this)
        }
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
    class NfsWorkspaceVolume {
        String serverAddress
        String serverPath
        boolean readOnly

        def serverAddress(final String serverAddress) {
            this.serverAddress = serverAddress
        }

        def serverPath(final String serverPath) {
            this.serverPath = serverPath
        }

        def readOnly(final boolean readOnly) {
            this.readOnly = readOnly
        }

        @Memoized
        String toString() {
            return printNonNullProperties(this)
        }
    }

    @ToString(includePackage = false, includeNames = true, ignoreNulls = true)
    class PersistentVolumeClaimWorkspaceVolume {
        String claimName
        boolean readOnly

        def claimName(final String claimName) {
            this.claimName = claimName
        }

        def readOnly(final boolean readOnly) {
            this.readOnly = readOnly
        }

        @Memoized
        String toString() {
            return printNonNullProperties(this)
        }
    }
}
