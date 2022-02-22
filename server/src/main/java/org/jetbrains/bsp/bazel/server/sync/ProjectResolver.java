package org.jetbrains.bsp.bazel.server.sync;

import static java.nio.charset.StandardCharsets.UTF_8;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.google.protobuf.TextFormat;
import io.vavr.API;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.bep.BepOutput;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.sync.model.Project;

/** Responsible for querying bazel and constructing Project instance */
public class ProjectResolver {
  private static final String ASPECT_NAME = "bsp_target_info_aspect";
  private static final String ASPECT_OUTPUT_GROUP = "bsp-target-info-transitive-deps";

  private final BazelBspAspectsManager bazelBspAspectsManager;
  private final ProjectViewProvider projectViewProvider;
  private final BazelProjectMapper bazelProjectMapper;

  public ProjectResolver(
      BazelBspAspectsManager bazelBspAspectsManager,
      ProjectViewProvider projectViewProvider,
      BazelProjectMapper bazelProjectMapper) {
    this.bazelBspAspectsManager = bazelBspAspectsManager;
    this.projectViewProvider = projectViewProvider;
    this.bazelProjectMapper = bazelProjectMapper;
  }

  public Project resolve() {
    var bepOutput = buildProjectWithAspect();
    var aspectOutputs = bepOutput.getFilesByOutputGroupNameTransitive(ASPECT_OUTPUT_GROUP);
    var rootTargets = bepOutput.getRootTargets();
    var targets = readTargetMapFromAspectOutputs(aspectOutputs);
    return bazelProjectMapper.createProject(targets, HashSet.ofAll(rootTargets));
  }

  private BepOutput buildProjectWithAspect() {
    var projectView = projectViewProvider.current();
    // TODO handle excludes
    var projectTargetRoots =
        List.ofAll(projectView.getTargets().getIncludedValues()).map(BuildTargetIdentifier::new);
    return bazelBspAspectsManager.fetchFilesFromOutputGroup(
        projectTargetRoots.asJava(), ASPECT_NAME, ASPECT_OUTPUT_GROUP);
  }

  private Map<String, TargetInfo> readTargetMapFromAspectOutputs(java.util.Set<URI> files) {
    return List.ofAll(files)
        .map(API.unchecked(this::readTargetInfoFromFile))
        .toMap(TargetInfo::getId, Function.identity());
  }

  private TargetInfo readTargetInfoFromFile(URI uri) throws IOException {
    var builder = TargetInfo.newBuilder();
    var parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build();
    parser.merge(Files.readString(Paths.get(uri), UTF_8), builder);
    return builder.build();
  }
}
