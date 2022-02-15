package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;

public class ProjectSyncService {
    private final BspProjectMapper mapper = new BspProjectMapper();
    private final ProjectResolver projectResolver;

    public ProjectSyncService(ProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    public Either<ResponseError, WorkspaceBuildTargetsResult> workspaceBuildTargets() {
        var project = projectResolver.resolve();
        ProjectStore.update(project);
        return Either.forRight(mapper.workspaceTargets(project));
    }
}
