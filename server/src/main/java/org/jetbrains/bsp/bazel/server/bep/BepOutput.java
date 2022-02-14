package org.jetbrains.bsp.bazel.server.bep;

import com.google.common.collect.Queues;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.OutputGroup;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.NamedSetOfFiles;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.BuildEventId;
import io.vavr.API;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BepOutput {
  private final Map<String, Set<String>> outputGroups = new HashMap<>();
  private final Map<String, NamedSetOfFiles> fileSets = new HashMap<>();
  private final List<String> rootTargets = new ArrayList<>();

  public void reset() {
    fileSets.clear();
    outputGroups.clear();
    rootTargets.clear();
  }

  public void addNamedSet(String id, NamedSetOfFiles namedSetOfFiles) {
    this.fileSets.put(id, namedSetOfFiles);
  }

  public void storeTargetOutputGroups(String target, List<OutputGroup> outputGroups) {
    rootTargets.add(target);

    for (var group : outputGroups) {
      var fileSets =
          group.getFileSetsList().stream()
              .map(BuildEventId.NamedSetOfFilesId::getId)
              .collect(Collectors.toList());
      this.outputGroups.computeIfAbsent(group.getName(), key -> new HashSet<>()).addAll(fileSets);
    }
  }

  public Stream<URI> getFilesFromOutputGroup(OutputGroup group) {
    return group.getFileSetsList().stream()
        .flatMap(fileSetId -> fileSets.get(fileSetId.getId()).getFilesList().stream())
        .map(file -> URI.create(file.getUri()));
  }

  public Set<URI> getFilesByOutputGroupNameTransitive(String outputGroup) {
    var result = new HashSet<URI>();

    var rootIds = this.outputGroups.getOrDefault(outputGroup, Set.of());
    if (rootIds.isEmpty()) {
      return result;
    }

    var toVisit = Queues.newArrayDeque(rootIds);
    var visited = new HashSet<String>();

    rootIds.forEach(id -> visit(id, visited, result));
    while (!toVisit.isEmpty()) {
      var fileSetId = toVisit.remove();
      var fileSet = fileSets.get(fileSetId);
      var children = fileSet.getFileSetsList();
      children.stream()
          .map(BuildEventId.NamedSetOfFilesId::getId)
          .filter(child -> !visited.contains(child))
          .forEach(
              child ->{
                visit(child, visited, result);
                toVisit.add(child);
              }
          );
    }

    return result;
  }

  private void visit(String id, Set<String> visited, Set<URI> result) {
    fileSets.get(id).getFilesList().stream()
            .map(API.unchecked(s -> new URI(s.getUri())))
            .forEach(result::add);
    visited.add(id);
  }

  public List<String> getRootTargets() {
    return rootTargets;
  }
}