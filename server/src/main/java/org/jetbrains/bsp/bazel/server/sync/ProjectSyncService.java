package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;

public class ProjectSyncService {
  private final BspProjectMapper mapper;
  private final ProjectResolver projectResolver;

  public ProjectSyncService(ProjectResolver projectResolver, BazelData bazelData) {
    this.projectResolver = projectResolver;
    this.mapper = new BspProjectMapper(bazelData);
  }

  public Either<ResponseError, WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    var project = projectResolver.resolve();
    ProjectStore.update(project);
    return Either.forRight(mapper.workspaceTargets(project));
  }
}
