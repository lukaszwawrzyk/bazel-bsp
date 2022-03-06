package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.ScalaBuildServer;
import ch.epfl.scala.bsp4j.ScalaMainClassesParams;
import ch.epfl.scala.bsp4j.ScalaMainClassesResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesParams;
import ch.epfl.scala.bsp4j.ScalaTestClassesResult;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class ScalaBuildServerImpl implements ScalaBuildServer {

  private final BspRequestsRunner runner;
  private final ProjectSyncService projectSyncService;

  public ScalaBuildServerImpl(BspRequestsRunner runner, ProjectSyncService projectSyncService) {
    this.runner = runner;
    this.projectSyncService = projectSyncService;
  }

  @Override
  public CompletableFuture<ScalacOptionsResult> buildTargetScalacOptions(
      ScalacOptionsParams params) {
    return runner.runCommand(
        "buildTargetScalacOptions", projectSyncService::buildTargetScalacOptions, params);
  }

  @Override
  public CompletableFuture<ScalaTestClassesResult> buildTargetScalaTestClasses(
      ScalaTestClassesParams params) {
    return runner.runCommand(
        "buildTargetScalaTestClasses", projectSyncService::buildTargetScalaTestClasses, params);
  }

  @Override
  public CompletableFuture<ScalaMainClassesResult> buildTargetScalaMainClasses(
      ScalaMainClassesParams params) {
    return runner.runCommand(
        "buildTargetScalaMainClasses", projectSyncService::buildTargetScalaMainClasses, params);
  }
}
