package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class JvmBuildServerImpl implements JvmBuildServer {

  private final ProjectSyncService projectSyncService;
  private final BspRequestsRunner runner;

  public JvmBuildServerImpl(ProjectSyncService projectSyncService, BspRequestsRunner runner) {
    this.projectSyncService = projectSyncService;
    this.runner = runner;
  }

  @Override
  public CompletableFuture<JvmRunEnvironmentResult> jvmRunEnvironment(
      JvmRunEnvironmentParams params) {
    return runner.runCommand("jvmRunEnvironment", projectSyncService::jvmRunEnvironment, params);
  }

  @Override
  public CompletableFuture<JvmTestEnvironmentResult> jvmTestEnvironment(
      JvmTestEnvironmentParams params) {
    return runner.runCommand("jvmTestEnvironment", projectSyncService::jvmTestEnvironment, params);
  }
}
