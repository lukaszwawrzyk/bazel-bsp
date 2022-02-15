package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

public class ProjectSyncService {
    private final BspProjectMapper mapper = new BspProjectMapper();
    private final ProjectResolver projectResolver;

    public ProjectSyncService(ProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    public WorkspaceBuildTargetsResult workspaceBuildTargets() {
        var project = projectResolver.resolve();
        ProjectStore.update(project);
        return mapper.workspaceTargets(project);
    }
}
