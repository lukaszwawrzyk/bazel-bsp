package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.ScalaBuildServer;
import ch.epfl.scala.bsp4j.ScalaMainClassesParams;
import ch.epfl.scala.bsp4j.ScalaMainClassesResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesParams;
import ch.epfl.scala.bsp4j.ScalaTestClassesResult;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class ScalaBuildServerImpl implements ScalaBuildServer {

  private final BazelBspServerRequestHelpers serverRequestHelpers;
  private final ProjectSyncService projectSyncService;

  public ScalaBuildServerImpl(
      BazelBspServerRequestHelpers serverRequestHelpers, ProjectSyncService projectSyncService) {
    this.serverRequestHelpers = serverRequestHelpers;
    this.projectSyncService = projectSyncService;
  }

  @Override
  public CompletableFuture<ScalacOptionsResult> buildTargetScalacOptions(
      ScalacOptionsParams scalacOptionsParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetScalacOptions",
        () -> projectSyncService.buildTargetScalacOptions(scalacOptionsParams));
  }

  @Override
  public CompletableFuture<ScalaTestClassesResult> buildTargetScalaTestClasses(
      ScalaTestClassesParams scalaTestClassesParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetScalaTestClasses",
        () -> projectSyncService.buildTargetScalaTestClasses(scalaTestClassesParams));
  }

  @Override
  public CompletableFuture<ScalaMainClassesResult> buildTargetScalaMainClasses(
      ScalaMainClassesParams scalaMainClassesParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetScalaMainClasses",
        () -> projectSyncService.buildTargetScalaMainClasses(scalaMainClassesParams));
  }
}
