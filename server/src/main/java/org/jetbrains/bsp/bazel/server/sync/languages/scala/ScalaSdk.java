package org.jetbrains.bsp.bazel.server.sync.languages.scala;

import io.vavr.collection.List;
import java.net.URI;
import java.util.Objects;

public class ScalaSdk {
  private final String organization;
  private final String version;
  private final String binaryVersion;
  private final List<URI> compilerJars;

  public ScalaSdk(
      String organization, String version, String binaryVersion, List<URI> compilerJars) {
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

  public List<URI> compilerJars() {
    return compilerJars;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ScalaSdk scalaSdk = (ScalaSdk) o;
    return organization.equals(scalaSdk.organization)
        && version.equals(scalaSdk.version)
        && binaryVersion.equals(scalaSdk.binaryVersion)
        && compilerJars.equals(scalaSdk.compilerJars);
  }

  @Override
  public int hashCode() {
    return Objects.hash(organization, version, binaryVersion, compilerJars);
  }
}