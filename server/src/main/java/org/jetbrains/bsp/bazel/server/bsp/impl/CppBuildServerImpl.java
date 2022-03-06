package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.CppBuildServer;
import ch.epfl.scala.bsp4j.CppOptionsParams;
import ch.epfl.scala.bsp4j.CppOptionsResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.bsp.services.CppBuildServerService;

public class CppBuildServerImpl implements CppBuildServer {
  private final CppBuildServerService cppBuildServerService;
  private final BspRequestsRunner runner;

  public CppBuildServerImpl(CppBuildServerService cppBuildServerService, BspRequestsRunner runner) {
    this.cppBuildServerService = cppBuildServerService;
    this.runner = runner;
  }

  @Override
  public CompletableFuture<CppOptionsResult> buildTargetCppOptions(CppOptionsParams params) {
    return runner.runCommand(
        "buildTargetCppOptions", cppBuildServerService::buildTargetCppOptions, params);
  }
}
