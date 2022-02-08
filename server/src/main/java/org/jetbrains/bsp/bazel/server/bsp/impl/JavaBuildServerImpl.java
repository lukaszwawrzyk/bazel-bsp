package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class JavaBuildServerImpl implements JavaBuildServer {

  private final ProjectSyncService projectSyncService;
  private final BazelBspServerRequestHelpers serverRequestHelpers;

  public JavaBuildServerImpl(
      ProjectSyncService projectSyncService, BazelBspServerRequestHelpers serverRequestHelpers) {
    this.projectSyncService = projectSyncService;
    this.serverRequestHelpers = serverRequestHelpers;
  }

  @Override
  public CompletableFuture<JavacOptionsResult> buildTargetJavacOptions(
      JavacOptionsParams javacOptionsParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetJavacOptions",
        () -> projectSyncService.buildTargetJavacOptions(javacOptionsParams));
  }
}
