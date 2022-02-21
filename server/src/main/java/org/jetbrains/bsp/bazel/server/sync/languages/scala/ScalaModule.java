package org.jetbrains.bsp.bazel.server.sync.languages.scala;

import io.vavr.control.Option;
import org.jetbrains.bsp.bazel.server.sync.languages.java.JavaModule;

public class ScalaModule {
  private final ScalaSdk sdk;
  private final Option<JavaModule> javaModule;

  public ScalaModule(ScalaSdk sdk, Option<JavaModule> javaModule) {
    this.sdk = sdk;
    this.javaModule = javaModule;
  }

  public ScalaSdk sdk() {
    return sdk;
  }

  public Option<JavaModule>  javaModule() {
    return javaModule;
  }
}
