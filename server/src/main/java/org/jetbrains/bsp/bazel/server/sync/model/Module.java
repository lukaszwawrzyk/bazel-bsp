package org.jetbrains.bsp.bazel.server.sync.model;

import io.vavr.collection.List;
import io.vavr.collection.Set;
import java.net.URI;

public class Module {
  private final Label label;
  private final List<Label> directDependencies;
  private final Set<Language> languages;
  private final Set<Tag> tags;
  private final URI baseDirectory;
  private final SourceSet sourceSet;

  public Module(
      Label label,
      List<Label> directDependencies,
      Set<Language> languages,
      Set<Tag> tags,
      URI baseDirectory,
      SourceSet sourceSet) {
    this.label = label;
    this.directDependencies = directDependencies;
    this.languages = languages;
    this.tags = tags;
    this.baseDirectory = baseDirectory;
    this.sourceSet = sourceSet;
  }

  public Label label() {
    return label;
  }

  public List<Label> directDependencies() {
    return directDependencies;
  }

  public Set<Language> languages() {
    return languages;
  }

  public Set<Tag> tags() {
    return tags;
  }

  public URI baseDirectory() {
    return baseDirectory;
  }

  public SourceSet sourceSet() {
    return sourceSet;
  }
}
