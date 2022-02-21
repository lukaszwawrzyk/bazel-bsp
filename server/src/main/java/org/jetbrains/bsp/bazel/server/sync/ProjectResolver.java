package org.jetbrains.bsp.bazel.server.sync;

import static java.nio.charset.StandardCharsets.UTF_8;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.google.common.collect.Iterables;
import com.google.protobuf.TextFormat;
import io.vavr.API;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;

/** Responsible for querying bazel and constructing Project instance */
public class ProjectResolver {
  private final BazelBspAspectsManager bazelBspAspectsManager;
  private final ProjectViewStore projectViewStore;
  private final BazelPathsResolver bazelPathsResolver;

  public ProjectResolver(
      BazelBspAspectsManager bazelBspAspectsManager,
      ProjectViewStore projectViewStore,
      BazelPathsResolver bazelPathsResolver) {
    this.bazelBspAspectsManager = bazelBspAspectsManager;
    this.projectViewStore = projectViewStore;
    this.bazelPathsResolver = bazelPathsResolver;
  }

  public Project resolve() {
    var projectView = projectViewStore.current();
    // TODO handle excludes
    var projectTargetRoots =
        List.ofAll(projectView.getTargets().getIncludedValues()).map(BuildTargetIdentifier::new);

    var outputGroup = "bsp-target-info-transitive-deps";
    var output =
        bazelBspAspectsManager.fetchFilesFromOutputGroup(
            projectTargetRoots.asJava(), "bsp_target_info_aspect", outputGroup);

    var files = output.getFilesByOutputGroupNameTransitive(outputGroup);
    var rootTargets = output.getRootTargets();

    var targetInfos =
        List.ofAll(files)
            .map(API.unchecked(this::readTargetInfoFromFile))
            .toMap(TargetInfo::getId, Function.identity());

    var sourceToTarget = buildReverseSourceMapping(targetInfos);

    return new Project(HashSet.ofAll(rootTargets), targetInfos, sourceToTarget);
  }

  private Map<String, String> buildReverseSourceMapping(Map<String, TargetInfo> targetInfoMap) {
    var output = new java.util.HashMap<String, String>();
    targetInfoMap
        .values()
        .forEach(
            target ->
                Iterables.concat(target.getSourcesList(), target.getResourcesList())
                    .forEach(
                        source -> {
                          var path = bazelPathsResolver.resolve(source);
                          output.put(path.toUri().toString(), target.getId());
                        }));
    return HashMap.ofAll(output);
  }

  private TargetInfo readTargetInfoFromFile(URI uri) throws IOException {
    var builder = TargetInfo.newBuilder();
    var parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build();
    parser.merge(Files.readString(Paths.get(uri), UTF_8), builder);
    return builder.build();
  }
}
