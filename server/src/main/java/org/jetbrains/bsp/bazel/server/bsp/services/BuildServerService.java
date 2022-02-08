package org.jetbrains.bsp.bazel.server.bsp.services;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileProvider;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunProvider;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestProvider;
import ch.epfl.scala.bsp4j.TestResult;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerLifetime;
import org.jetbrains.bsp.bazel.server.bsp.BazelBspServerRequestHelpers;
import org.jetbrains.bsp.bazel.server.sync.ExecuteService;

public class BuildServerService {

  private static final Logger LOGGER = LogManager.getLogger(BuildServerService.class);

  private final BazelBspServerRequestHelpers serverRequestHelpers;
  private final BazelBspServerLifetime serverLifetime;
  private final ExecuteService executeService;

  public BuildServerService(
      BazelBspServerRequestHelpers serverRequestHelpers,
      BazelBspServerLifetime serverLifetime,
      ExecuteService executeService) {
    this.serverRequestHelpers = serverRequestHelpers;
    this.serverLifetime = serverLifetime;
    this.executeService = executeService;
  }

  public CompletableFuture<InitializeBuildResult> buildInitialize(
      InitializeBuildParams initializeBuildParams) {
    LOGGER.info("buildInitialize call with param: {}", initializeBuildParams);

    return processBuildInitialize("buildInitialize", this::handleBuildInitialize);
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

  private Either<ResponseError, InitializeBuildResult> handleBuildInitialize() {
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    capabilities.setCompileProvider(new CompileProvider(Constants.SUPPORTED_LANGUAGES));
    capabilities.setRunProvider(new RunProvider(Constants.SUPPORTED_LANGUAGES));
    capabilities.setTestProvider(new TestProvider(Constants.SUPPORTED_LANGUAGES));
    capabilities.setDependencySourcesProvider(true);
    capabilities.setInverseSourcesProvider(true);
    capabilities.setResourcesProvider(true);
    capabilities.setJvmRunEnvironmentProvider(true);
    capabilities.setJvmTestEnvironmentProvider(true);
    return Either.forRight(
        new InitializeBuildResult(
            Constants.NAME, Constants.VERSION, Constants.BSP_VERSION, capabilities));
  }

  public void onBuildInitialized() {
    LOGGER.info("onBuildInitialized call");

    serverLifetime.setInitializedComplete();
  }

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

  public void onBuildExit() {
    LOGGER.info("onBuildExit call");

    serverLifetime.forceFinish();
  }

  public Either<ResponseError, CompileResult> buildTargetCompile(CompileParams compileParams) {
    LOGGER.info("buildTargetCompile call with param: {}", compileParams);
    return executeService.compile(compileParams);
  }

  public Either<ResponseError, TestResult> buildTargetTest(TestParams testParams) {
    LOGGER.info("buildTargetTest call with param: {}", testParams);
    return executeService.test(testParams);
  }

  public Either<ResponseError, RunResult> buildTargetRun(RunParams runParams) {
    LOGGER.info("buildTargetRun call with param: {}", runParams);
    return executeService.run(runParams);
  }

  public Either<ResponseError, CleanCacheResult> buildTargetCleanCache(
      CleanCacheParams cleanCacheParams) {
    LOGGER.info("buildTargetCleanCache call with param: {}", cleanCacheParams);
    return executeService.clean(cleanCacheParams);
  }
}
