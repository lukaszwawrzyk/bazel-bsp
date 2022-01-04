package org.jetbrains.bsp.bazel.projectview.parser;

import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.projectview.model.ProjectView;
import org.jetbrains.bsp.bazel.projectview.model.ProjectViewProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectViewParserProvider implements ProjectViewProvider {

  private static final ProjectViewParser PARSER = new ProjectViewParserImpl();
  private static final Path PROJECT_VIEW_FILE = Paths.get(Constants.DEFAULT_PROJECT_VIEW_FILE);

  @Override
  public ProjectView create() {
    return null;
  }

  private boolean doesProjectViewFileExists(Path projectViewFile) {
    return projectViewFile.toFile().isFile();
  }
}
