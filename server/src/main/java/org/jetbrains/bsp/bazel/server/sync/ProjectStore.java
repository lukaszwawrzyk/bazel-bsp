package org.jetbrains.bsp.bazel.server.sync;

public class ProjectStore {
  private Project project;

  public Project get() {
    if (project == null) {
      loadProject();
    }
    return project;
  }

  private void loadProject() {
    // TODO load from file, fail if not exists
  }

  public void update(Project project) {
    this.project = project;
    storeProject();
  }

  private void storeProject() {
    // TODO save project data to disk
  }
}
