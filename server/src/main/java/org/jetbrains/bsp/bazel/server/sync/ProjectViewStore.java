package org.jetbrains.bsp.bazel.server.sync;

import org.jetbrains.bsp.bazel.projectview.model.ProjectView;

public class ProjectViewStore {

    private final ProjectView projectView;

    public ProjectViewStore(ProjectView projectView) {
        this.projectView = projectView;
    }

    public ProjectView current() {
        return projectView;
    }
}
