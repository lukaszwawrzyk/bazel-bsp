package org.jetbrains.bsp.bazel.server.sync;

public class ProjectStore {
    private static Project project;

    public static Project get() {
        if (project == null) {
            loadProject();
        }
        return project;
    }

    private static void loadProject() {
        // TODO load from file, fail if not exists
    }

    public static void update(Project project) {
        ProjectStore.project = project;
    }
}
