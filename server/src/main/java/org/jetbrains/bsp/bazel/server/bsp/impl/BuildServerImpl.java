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
import java.util.concurrent.CompletableFuture;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerLifetime;
import org.jetbrains.bsp.bazel.server.bsp.BspRequestsRunner;
import org.jetbrains.bsp.bazel.server.sync.ExecuteService;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class BuildServerImpl implements BuildServer {

  private final BspRequestsRunner runner;
  private final ProjectSyncService projectSyncService;
  private final BazelBspServerLifetime serverLifetime;
  private final ExecuteService executeService;

  public BuildServerImpl(
      BspRequestsRunner runner,
      ProjectSyncService projectSyncService,
      BazelBspServerLifetime serverLifetime,
      ExecuteService executeService) {
    this.runner = runner;
    this.projectSyncService = projectSyncService;
    this.serverLifetime = serverLifetime;
    this.executeService = executeService;
  }

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(
      InitializeBuildParams initializeBuildParams) {
    return runner.runCommand(
        "buildInitialize", projectSyncService::initialize, runner::serverIsNotFinished);
  }

  @Override
  public void onBuildInitialized() {
    runner.runCommand("onBuildInitialized", serverLifetime::setInitializedComplete);
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    return runner.runCommand(
        "buildShutdown",
        () -> {
          serverLifetime.setFinishedComplete();
          return new Object();
        },
        runner::serverIsInitialized);
  }

  @Override
  public void onBuildExit() {
    runner.runCommand("onBuildExit", serverLifetime::forceFinish);
  }

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    return runner.runCommand("workspaceBuildTargets", projectSyncService::workspaceBuildTargets);
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    return runner.runCommand("workspaceReload", projectSyncService::workspaceReload);
  }

  @Override
  public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams params) {
    return runner.runCommand("buildTargetSources", projectSyncService::buildTargetSources, params);
  }

  @Override
  public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams params) {
    return runner.runCommand(
        "buildTargetInverseSources", projectSyncService::buildTargetInverseSources, params);
  }

  @Override
  public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams params) {
    return runner.runCommand(
        "buildTargetDependencySources", projectSyncService::buildTargetDependencySources, params);
  }

  @Override
  public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams params) {
    return runner.runCommand(
        "buildTargetResources", projectSyncService::buildTargetResources, params);
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams params) {
    return runner.runCommand("buildTargetCompile", executeService::compile, params);
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(TestParams params) {
    return runner.runCommand("buildTargetTest", executeService::test, params);
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(RunParams params) {
    return runner.runCommand("buildTargetRun", executeService::run, params);
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams params) {
    return runner.runCommand("buildTargetCleanCache", executeService::clean, params);
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return runner.runCommand(
        "buildTargetDependencyModules", projectSyncService::buildTargetDependencyModules, params);
  }
}
