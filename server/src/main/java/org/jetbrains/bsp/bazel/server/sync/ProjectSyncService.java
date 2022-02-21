package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import java.util.Collections;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;

/** A facade for all project sync related methods */
public class ProjectSyncService {
  private final BspProjectMapper bspMapper;
  private final ProjectStore projectStore;

  public ProjectSyncService(BspProjectMapper bspProjectMapper, ProjectStore projectStore) {
    this.bspMapper = bspProjectMapper;
    this.projectStore = projectStore;
  }

  public Either<ResponseError, WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    var project = projectStore.refreshAndGet();
    var result = bspMapper.workspaceTargets(project);
    return Either.forRight(result);
  }

  public Either<ResponseError, SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
    var project = projectStore.get();
    var result = bspMapper.sources(project, sourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, ResourcesResult> buildTargetResources(
      ResourcesParams resourcesParams) {
    var project = projectStore.get();
    var result = bspMapper.resources(project, resourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams inverseSourcesParams) {
    var project = projectStore.get();
    var result = bspMapper.inverseSources(project, inverseSourcesParams);
    return Either.forRight(result);
  }

  public Either<ResponseError, DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams dependencySourcesParams) {
    var project = projectStore.get();
    var result = bspMapper.dependencySources(project, dependencySourcesParams);
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
