package org.jetbrains.bsp.bazel.server.sync;

import org.jetbrains.bsp.bazel.server.sync.model.Project;

public class ProjectStore {
  private Project project;
  private final ProjectResolver projectResolver;

  public ProjectStore(ProjectResolver projectResolver) {
    this.projectResolver = projectResolver;
  }

  public synchronized Project refreshAndGet() {
    loadFromBazel();
    return project;
  }

  public synchronized Project get() {
    if (project == null) {
      loadFromDisk();
    }
    if (project == null) {
      loadFromBazel();
    }

    return project;
  }

  private void loadFromBazel() {
    project = projectResolver.resolve();
    storeOnDisk();
  }

  private void loadFromDisk() {
    // TODO implement; do nothing if no project cache data is present
  }

  private void storeOnDisk() {
    // TODO save project data to disk
  }
}
