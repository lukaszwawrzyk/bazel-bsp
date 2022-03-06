package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class JavaBuildServerImpl implements JavaBuildServer {

  private final ProjectSyncService projectSyncService;
  private final BspRequestsRunner runner;

  public JavaBuildServerImpl(ProjectSyncService projectSyncService, BspRequestsRunner runner) {
    this.projectSyncService = projectSyncService;
    this.runner = runner;
  }

  @Override
  public CompletableFuture<JavacOptionsResult> buildTargetJavacOptions(
      JavacOptionsParams javacOptionsParams) {
    return runner.runCommand(
        "buildTargetJavacOptions", projectSyncService::buildTargetJavacOptions, javacOptionsParams);
  }
}
