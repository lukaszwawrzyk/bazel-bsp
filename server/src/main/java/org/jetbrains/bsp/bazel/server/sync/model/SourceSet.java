package org.jetbrains.bsp.bazel.server.sync.model;

import io.vavr.collection.Set;
import java.net.URI;

public class SourceSet {
  private final Set<URI> sources;
  private final Set<URI> sourceRoots;

  public SourceSet(Set<URI> sources, Set<URI> sourceRoots) {
    this.sources = sources;
    this.sourceRoots = sourceRoots;
  }

  public Set<URI> sources() {
    return sources;
  }

  public Set<URI> sourceRoots() {
    return sourceRoots;
  }
}
