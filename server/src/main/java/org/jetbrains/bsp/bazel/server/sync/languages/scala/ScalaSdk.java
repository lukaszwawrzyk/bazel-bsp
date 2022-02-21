package org.jetbrains.bsp.bazel.server.sync.languages.scala;

import io.vavr.collection.Set;
import java.net.URI;

public class ScalaSdk {
    private final String organization;
    private final String version;
    private final String binaryVersion;
    private final Set<URI> compilerJars;

    public ScalaSdk(String organization, String version, String binaryVersion, Set<URI> compilerJars) {
        this.organization = organization;
        this.version = version;
        this.binaryVersion = binaryVersion;
        this.compilerJars = compilerJars;
    }

    public String organization() {
        return organization;
    }

    public String version() {
        return version;
    }

    public String binaryVersion() {
        return binaryVersion;
    }

    public Set<URI> compilerJars() {
        return compilerJars;
    }
}
