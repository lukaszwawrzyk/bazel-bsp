package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;

public class ProjectSyncService {
  private final BspProjectMapper mapper;
  private final ProjectResolver resolver;
  private final ProjectStore store;

  public ProjectSyncService(
      ProjectResolver projectResolver, BazelData bazelData, ProjectStore store) {
    this.resolver = projectResolver;
    this.mapper = new BspProjectMapper(bazelData);
    this.store = store;
  }

  public Either<ResponseError, WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    var project = resolver.resolve();
    store.update(project);
    return Either.forRight(mapper.workspaceTargets(project));
  }

  public Either<ResponseError, SourcesResult> buildTargetSources(SourcesParams sourcesParams) {
    var project = store.get();
    var sources = mapper.sources(project, toLabels(sourcesParams.getTargets()));
    return Either.forRight(sources);
  }

  private Set<String> toLabels(java.util.List<BuildTargetIdentifier> targets) {
    return HashSet.ofAll(targets).map(BuildTargetIdentifier::getUri);
  }
}
