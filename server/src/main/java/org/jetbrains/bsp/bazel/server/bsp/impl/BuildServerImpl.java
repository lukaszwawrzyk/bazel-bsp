package org.jetbrains.bsp.bazel.server.bsp.impl;

import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.bsp.services.BuildServerService;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class BuildServerImpl implements BuildServer {

  private static final Logger LOGGER = LogManager.getLogger(BuildServerImpl.class);

  private final BuildServerService buildServerService;
  private final BazelBspServerRequestHelpers serverRequestHelpers;
  private final ProjectSyncService projectSyncService;

  public BuildServerImpl(
      BuildServerService buildServerService,
      BazelBspServerRequestHelpers serverRequestHelpers,
      ProjectSyncService projectSyncService) {
    this.buildServerService = buildServerService;
    this.serverRequestHelpers = serverRequestHelpers;
    this.projectSyncService = projectSyncService;
  }

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(
      InitializeBuildParams initializeBuildParams) {
    return buildServerService.buildInitialize(initializeBuildParams);
  }

  @Override
  public void onBuildInitialized() {
    buildServerService.onBuildInitialized();
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    return buildServerService.buildShutdown();
  }

  @Override
  public void onBuildExit() {
    buildServerService.onBuildExit();
  }

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    LOGGER.info("workspaceBuildTargets call");
    return serverRequestHelpers.executeCommand(
        "workspaceBuildTargets", projectSyncService::workspaceBuildTargets);
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    return serverRequestHelpers.executeCommand(
        "workspaceReload", buildServerService::workspaceReload);
  }

  @Override
  public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
    LOGGER.info("buildTargetSources call with param: {}", sourcesParams);
    return serverRequestHelpers.executeCommand(
        "buildTargetSources", () -> projectSyncService.buildTargetSources(sourcesParams));
  }

  @Override
  public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams inverseSourcesParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetInverseSources",
        () -> projectSyncService.buildTargetInverseSources(inverseSourcesParams));
  }

  @Override
  public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams dependencySourcesParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetDependencySources",
        () -> projectSyncService.buildTargetDependencySources(dependencySourcesParams));
  }

  @Override
  public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams resourcesParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetResources", () -> projectSyncService.buildTargetResources(resourcesParams));
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams compileParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetCompile", () -> buildServerService.buildTargetCompile(compileParams));
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(TestParams testParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetTest", () -> buildServerService.buildTargetTest(testParams));
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(RunParams runParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetRun", () -> buildServerService.buildTargetRun(runParams));
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(
      CleanCacheParams cleanCacheParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetCleanCache", () -> buildServerService.buildTargetCleanCache(cleanCacheParams));
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return serverRequestHelpers.executeCommand(
            "buildTargetDependencyModules", () -> projectSyncService.buildTargetDependencyModules(params));
  }
}
