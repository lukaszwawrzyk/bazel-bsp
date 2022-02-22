package org.jetbrains.bsp.bazel.server.sync.languages.java;

import io.vavr.control.Option;
import java.net.URI;

public class Jdk {
  private final String version;
  private final Option<URI> javaHome;

  public Jdk(String version, Option<URI> javaHome) {
    this.version = version;
    this.javaHome = javaHome;
  }

  public String javaVersion() {
    return version;
  }

  public Option<URI> javaHome() {
    return javaHome;
  }
}
