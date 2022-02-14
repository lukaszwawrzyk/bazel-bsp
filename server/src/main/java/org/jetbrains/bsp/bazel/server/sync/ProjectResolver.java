package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.google.protobuf.TextFormat;
import io.vavr.API;
import io.vavr.collection.List;
import java.io.IOException;
import java.net.URI;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;

public class ProjectResolver {
  private final BazelBspAspectsManager bazelBspAspectsManager;
  private final ProjectViewStore projectViewStore;
  private final BazelRunner bazelRunner;

  public ProjectResolver(
      BazelBspAspectsManager bazelBspAspectsManager,
      ProjectViewStore projectViewStore,
      BazelRunner bazelRunner) {
    this.bazelBspAspectsManager = bazelBspAspectsManager;
    this.projectViewStore = projectViewStore;
    this.bazelRunner = bazelRunner;
  }

  public Project resolve() {
    var projectView = projectViewStore.current();
    // TODO handle excludes
    var projectTargetRoots =
        List.ofAll(projectView.getTargets().getIncludedTargets()).map(BuildTargetIdentifier::new);

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
    return new Project(rootTargets, targetInfos);
  }

  private TargetInfo readTargetInfoFromFile(URI uri) throws IOException {
    var builder = TargetInfo.newBuilder();
    var parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build();
    parser.merge(Files.readString(Paths.get(uri), UTF_8), builder);
    return builder.build();
  }
}
