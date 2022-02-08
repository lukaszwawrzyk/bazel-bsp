package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.ScalaMainClassesParams;
import ch.epfl.scala.bsp4j.ScalaMainClassesResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesParams;
import ch.epfl.scala.bsp4j.ScalaTestClassesResult;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import java.util.Collections;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;

/** A facade for all project sync related methods */
public class ProjectSyncService {
  private final BspProjectMapper bspMapper;
  private final ProjectProvider projectProvider;

  public ProjectSyncService(BspProjectMapper bspProjectMapper, ProjectProvider projectProvider) {
    this.bspMapper = bspProjectMapper;
    this.projectProvider = projectProvider;
  }

  // We might consider doing the actual project reload in this endpoint
  // i.e. just run projectProvider.refreshAndGet() and in workspaceBuildTargets
  // just run projectProvider.get() although current approach seems to work
  // correctly, so I am not changing anything.
  public Either<ResponseError, Object> workspaceReload() {
    return Either.forRight(new Object());
  }

  public Either<ResponseError, WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    var project = projectProvider.refreshAndGet();
    var result = bspMapper.workspaceTargets(project);
    return Either.forRight(result);
  }

  public Either<ResponseError, SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
    var project = projectProvider.get();
    var result = bspMapper.sources(project, sourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, ResourcesResult> buildTargetResources(
      ResourcesParams resourcesParams) {
    var project = projectProvider.get();
    var result = bspMapper.resources(project, resourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams inverseSourcesParams) {
    var project = projectProvider.get();
    var result = bspMapper.inverseSources(project, inverseSourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams dependencySourcesParams) {
    var project = projectProvider.get();
    var result = bspMapper.dependencySources(project, dependencySourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, JvmRunEnvironmentResult> jvmRunEnvironment(
      JvmRunEnvironmentParams params) {
    var project = projectProvider.get();
    var result = bspMapper.jvmRunEnvironment(project, params);
    return Either.forRight(result);
  }

  public Either<ResponseError, JvmTestEnvironmentResult> jvmTestEnvironment(
      JvmTestEnvironmentParams params) {
    var project = projectProvider.get();
    var result = bspMapper.jvmTestEnvironment(project, params);
    return Either.forRight(result);
  }

  public Either<ResponseError, JavacOptionsResult> buildTargetJavacOptions(
      JavacOptionsParams params) {
    var project = projectProvider.get();
    var result = bspMapper.buildTargetJavacOptions(project, params);
    return Either.forRight(result);
  }

  public Either<ResponseError, ScalacOptionsResult> buildTargetScalacOptions(
      ScalacOptionsParams params) {
    var project = projectProvider.get();
    var result = bspMapper.buildTargetScalacOptions(project, params);
    return Either.forRight(result);
  }

  public Either<ResponseError, ScalaTestClassesResult> buildTargetScalaTestClasses(
      ScalaTestClassesParams params) {
    var project = projectProvider.get();
    var result = bspMapper.buildTargetScalaTestClasses(project, params);
    return Either.forRight(result);
  }

  public Either<ResponseError, ScalaMainClassesResult> buildTargetScalaMainClasses(
      ScalaMainClassesParams params) {
    var project = projectProvider.get();
    var result = bspMapper.buildTargetScalaMainClasses(project, params);
    return Either.forRight(result);
  }

  // TODO implement this endpoint to return libraries with maven coordinates that target depends on
  // this should be helpful for 3rd party shared indexes in IntelliJ, however the endpoint is not
  // yet used in the client
  public Either<ResponseError, DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return Either.forRight(new DependencyModulesResult(Collections.emptyList()));
  }
}
