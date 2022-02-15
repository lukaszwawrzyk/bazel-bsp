package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import java.util.ArrayList;
import java.util.Collections;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.commons.Uri;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;

public class BspProjectMapper {
  private static final Set<Tuple2<java.util.List<String>, HashSet<String>>>
      fileExtensionsToLanguages =
          HashMap.of(
                  Constants.SCALA_EXTENSIONS, HashSet.of(Constants.SCALA),
                  Constants.JAVA_EXTENSIONS, HashSet.of(Constants.JAVA),
                  Constants.KOTLIN_EXTENSIONS, HashSet.of(Constants.KOTLIN, Constants.JAVA),
                  Constants.CPP_EXTENSIONS, HashSet.of(Constants.CPP))
              .toSet();

  private final TargetKindResolver targetKindResolver = new TargetKindResolver();
  private final BazelData bazelData;

  public BspProjectMapper(BazelData bazelData) {
    this.bazelData = bazelData;
  }

  public WorkspaceBuildTargetsResult workspaceTargets(Project project) {
    var buildTargets = new ArrayList<BuildTarget>();

    project
        .getRootTargets()
        .map(
            targetInfo -> {
              var label = new BuildTargetIdentifier(targetInfo.getId());
              var dependencies =
                  List.ofAll(targetInfo.getDependenciesList())
                      .map(d -> new BuildTargetIdentifier(d.getId()));

              var languages =
                  HashSet.ofAll(targetInfo.getSourcesList())
                      .flatMap(
                          source ->
                              fileExtensionsToLanguages.flatMap(
                                  exts -> resolveLanguages(source, exts._1, exts._2)));

              var capabilities =
                  new BuildTargetCapabilities(
                      true,
                      targetKindResolver.isTestTarget(targetInfo),
                      targetKindResolver.isRunnableTarget(targetInfo));
              var baseDirectory =
                  Uri.packageDirFromLabel(label.getUri(), bazelData.getWorkspaceRoot()).toString();
              var buildTarget =
                  new BuildTarget(
                      label,
                      Collections.emptyList(),
                      languages.toJavaList(),
                      dependencies.toJavaList(),
                      capabilities);
              buildTarget.setDisplayName(label.getUri());
              buildTarget.setBaseDirectory(baseDirectory);
              // todo fill target data
              return buildTarget;
            });

    return new WorkspaceBuildTargetsResult(buildTargets);
  }

  private Set<String> resolveLanguages(
      FileLocation file, java.util.List<String> expectedFileExtensions, Set<String> languages) {
    if (expectedFileExtensions.stream().anyMatch(ext -> file.getPath().endsWith(ext))) {
      return languages;
    } else {
      return HashSet.empty();
    }
  }
}
