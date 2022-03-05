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
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerLifetime;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.sync.ExecuteService;
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService;

public class BuildServerImpl implements BuildServer {

  private static final Logger LOGGER = LogManager.getLogger(BuildServerImpl.class);

  private final BazelBspServerRequestHelpers serverRequestHelpers;
  private final ProjectSyncService projectSyncService;
  private final BazelBspServerLifetime serverLifetime;
  private final ExecuteService executeService;

  public BuildServerImpl(
      BazelBspServerRequestHelpers serverRequestHelpers,
      ProjectSyncService projectSyncService,
      BazelBspServerLifetime serverLifetime,
      ExecuteService executeService) {
    this.serverRequestHelpers = serverRequestHelpers;
    this.projectSyncService = projectSyncService;
    this.serverLifetime = serverLifetime;
    this.executeService = executeService;
  }

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(
      InitializeBuildParams initializeBuildParams) {
    LOGGER.info("buildInitialize call with param: {}", initializeBuildParams);
    return processBuildInitialize("buildInitialize", projectSyncService::initialize);
  }

  private <T> CompletableFuture<T> processBuildInitialize(
      String methodName, Supplier<Either<ResponseError, T>> request) {
    if (serverLifetime.isFinished()) {
      return serverRequestHelpers.completeExceptionally(
          methodName,
          new ResponseError(
              ResponseErrorCode.serverErrorEnd, "Server has already shutdown!", false));
    }

    return serverRequestHelpers.getValue(methodName, request);
  }

  @Override
  public void onBuildInitialized() {
    LOGGER.info("onBuildInitialized call");

    serverLifetime.setInitializedComplete();
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    LOGGER.info("buildShutdown call");
    return processBuildShutdown("buildShutdown", this::handleBuildShutdown);
  }

  private <T> CompletableFuture<T> processBuildShutdown(
      String methodName, Supplier<Either<ResponseError, T>> request) {
    if (!serverLifetime.isInitialized()) {
      return serverRequestHelpers.completeExceptionally(
          methodName,
          new ResponseError(
              ResponseErrorCode.serverErrorEnd, "Server has not been initialized yet!", false));
    }

    return serverRequestHelpers.getValue(methodName, request);
  }

  private Either<ResponseError, Object> handleBuildShutdown() {
    serverLifetime.setFinishedComplete();
    return Either.forRight(new Object());
  }

  @Override
  public void onBuildExit() {
    LOGGER.info("onBuildExit call");
    serverLifetime.forceFinish();
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
        "workspaceReload", projectSyncService::workspaceReload);
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
        "buildTargetCompile", () -> executeService.compile(compileParams));
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(TestParams testParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetTest", () -> executeService.test(testParams));
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(RunParams runParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetRun", () -> executeService.run(runParams));
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(
      CleanCacheParams cleanCacheParams) {
    return serverRequestHelpers.executeCommand(
        "buildTargetCleanCache", () -> executeService.clean(cleanCacheParams));
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return serverRequestHelpers.executeCommand(
        "buildTargetDependencyModules",
        () -> projectSyncService.buildTargetDependencyModules(params));
  }
}
