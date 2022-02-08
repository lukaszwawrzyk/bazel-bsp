package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class JvmBuildServerImpl implements JvmBuildServer {

  private final ProjectSyncService projectSyncService;
  private final BazelBspServerRequestHelpers serverRequestHelpers;

  public JvmBuildServerImpl(
      ProjectSyncService projectSyncService, BazelBspServerRequestHelpers serverRequestHelpers) {
    this.projectSyncService = projectSyncService;
    this.serverRequestHelpers = serverRequestHelpers;
  }

  @Override
  public CompletableFuture<JvmRunEnvironmentResult> jvmRunEnvironment(
      JvmRunEnvironmentParams params) {
    return serverRequestHelpers.executeCommand(
        "jvmRunEnvironment", () -> projectSyncService.jvmRunEnvironment(params));
  }

  @Override
  public CompletableFuture<JvmTestEnvironmentResult> jvmTestEnvironment(
      JvmTestEnvironmentParams params) {
    return serverRequestHelpers.executeCommand(
        "jvmTestEnvironment", () -> projectSyncService.jvmTestEnvironment(params));
  }
}
