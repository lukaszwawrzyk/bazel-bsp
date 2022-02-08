package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner;
import org.jetbrains.bsp.bazel.info.BspTargetInfo;
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
        projectView.getTargets().getIncludedTargets().stream()
            .map(BuildTargetIdentifier::new)
            .collect(Collectors.toList());
    var files =
        bazelBspAspectsManager.fetchFilesFromOutputGroup(
            projectTargetRoots, "bsp_target_info_aspect", "bsp_target_info_file");
    var targetInfos =
        files.stream()
            .map(
                uri -> {
                  try {
                    var builder = TargetInfo.newBuilder();
                    var parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build();
                    parser.merge(Files.readString(Paths.get(uri), UTF_8), builder);
                    return builder.build();
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    return new Project(
        targetInfos.stream().collect(Collectors.toMap(TargetInfo::getId, Function.identity())));
  }
}
