package org.jetbrains.bsp.bazel.server.sync.languages.java;

public class JavaModule {
  private final Jdk jdk;

  public JavaModule(Jdk jdk) {
    this.jdk = jdk;
  }

  public Jdk jdk() {
    return jdk;
  }
}
