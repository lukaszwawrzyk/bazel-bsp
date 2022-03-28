package org.jetbrains.bsp.bazel.server.sync;

import com.google.protobuf.TextFormat;
import io.vavr.API;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.bsp.bazel.bazelrunner.utils.Format;
import org.jetbrains.bsp.bazel.bazelrunner.utils.Stopwatch;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.logger.BuildClientLogger;
import org.jetbrains.bsp.bazel.projectview.model.ProjectView;
import org.jetbrains.bsp.bazel.server.bep.BepOutput;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.sync.model.Project;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Responsible for querying bazel and constructing Project instance */
public class ProjectResolver {
  private static final String ASPECT_NAME = "bsp_target_info_aspect";
  private static final String BSP_INFO_OUTPUT_GROUP = "bsp-target-info-transitive-deps";
  private static final String ARTIFACTS_OUTPUT_GROUP = "bsp-ide-resolve-transitive-deps";

  private final BazelBspAspectsManager bazelBspAspectsManager;
  private final ProjectViewProvider projectViewProvider;
  private final BazelProjectMapper bazelProjectMapper;
  private final BuildClientLogger logger;

  public ProjectResolver(
      BazelBspAspectsManager bazelBspAspectsManager,
      ProjectViewProvider projectViewProvider,
      BazelProjectMapper bazelProjectMapper,
      BuildClientLogger logger) {
    this.bazelBspAspectsManager = bazelBspAspectsManager;
    this.projectViewProvider = projectViewProvider;
    this.bazelProjectMapper = bazelProjectMapper;
    this.logger = logger;
  }

  public Project resolve() {
    var projectView = measure("read project view",  projectViewProvider::current);
    var bepOutput = measure("build project with aspect", () -> buildProjectWithAspect(projectView));
    var aspectOutputs = measure("read aspect output paths", () -> bepOutput.filesByOutputGroupNameTransitive(BSP_INFO_OUTPUT_GROUP));
    var rootTargets = bepOutput.rootTargets();
    var targets = measure("parse aspect outputs", () -> readTargetMapFromAspectOutputs(aspectOutputs));
    return measure("map to internal model", () -> bazelProjectMapper.createProject(targets, HashSet.ofAll(rootTargets), projectView));
  }

  private BepOutput buildProjectWithAspect(ProjectView projectView) {
    return bazelBspAspectsManager.fetchFilesFromOutputGroups(
        projectView.targetSpecs(),
        ASPECT_NAME,
        List.of(BSP_INFO_OUTPUT_GROUP, ARTIFACTS_OUTPUT_GROUP));
  }

  private Map<String, TargetInfo> readTargetMapFromAspectOutputs(Set<URI> files) {
    return files
        .map(API.unchecked(this::readTargetInfoFromFile))
        .toMap(TargetInfo::getId, Function.identity());
  }

  private TargetInfo readTargetInfoFromFile(URI uri) throws IOException {
    var builder = TargetInfo.newBuilder();
    var parser = TextFormat.Parser.newBuilder().setAllowUnknownFields(true).build();
    parser.merge(Files.readString(Paths.get(uri), UTF_8), builder);
    return builder.build();
  }

  private <T> T measure(String description, Supplier<T> supplier) {
    var sw = Stopwatch.start();
    T result = supplier.get();
    var duration = sw.stop();
    logger.logMessage(
        String.format("Step '%s' completed in %s.", description, Format.duration(duration)));
    return result;
  }
}
