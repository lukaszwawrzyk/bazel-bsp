package org.jetbrains.bsp.bazel.server.sync;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetDataKind;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CppBuildTarget;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalaPlatform;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import java.util.Collections;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.commons.Uri;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public class BspProjectMapper {

  private final TargetKindResolver targetKindResolver = new TargetKindResolver();
  private final BazelData bazelData;

  public BspProjectMapper(BazelData bazelData) {
    this.bazelData = bazelData;
  }

  public WorkspaceBuildTargetsResult workspaceTargets(Project project) {
    var buildTargets =
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
                                  Languages.all()
                                      .flatMap(
                                          lang ->
                                              resolveLanguages(
                                                  source,
                                                  lang.getExtensions(),
                                                  lang.getAllNames())));

                  var capabilities =
                      new BuildTargetCapabilities(
                          true,
                          targetKindResolver.isTestTarget(targetInfo),
                          targetKindResolver.isRunnableTarget(targetInfo));
                  var baseDirectory =
                      Uri.packageDirFromLabel(label.getUri(), bazelData.getWorkspaceRoot())
                          .toString();
                  var buildTarget =
                      new BuildTarget(
                          label,
                          Collections.emptyList(),
                          languages.toJavaList(),
                          dependencies.toJavaList(),
                          capabilities);
                  buildTarget.setDisplayName(label.getUri());
                  buildTarget.setBaseDirectory(baseDirectory);

                  var tag = targetKindResolver.resolveBuildTargetTag(targetInfo);
                  buildTarget.setTags(List.of(tag).toJavaList());

                  if (languages.contains(Languages.SCALA.getName())) {
                    buildTarget.setDataKind(BuildTargetDataKind.SCALA);
                    // TODO resolve from aspect
                    var scalaBuildTarget =
                        new ScalaBuildTarget(
                            "org.scala-lang",
                            "2.13.2",
                            "2.12",
                            ScalaPlatform.JVM,
                            List.<String>of().toJavaList());
                    extractJvmBuildTarget(targetInfo).forEach(scalaBuildTarget::setJvmBuildTarget);
                  } else if (languages.contains(Languages.JAVA.getName())
                      || Languages.KOTLIN.getAllNames().exists(languages::contains)) {
                    buildTarget.setDataKind(BuildTargetDataKind.JVM);
                    extractJvmBuildTarget(targetInfo).forEach(buildTarget::setData);
                  } else if (languages.contains(Languages.CPP.getName())) {
                    buildTarget.setDataKind(BuildTargetDataKind.CPP);
                    // TODO resolve from aspect
                    var cppBuildTarget = new CppBuildTarget(null, "", "", "");
                    buildTarget.setData(cppBuildTarget);
                  }

                  return buildTarget;
                });

    return new WorkspaceBuildTargetsResult(buildTargets.toJavaList());
  }

  private Option<JvmBuildTarget> extractJvmBuildTarget(TargetInfo targetInfo) {
    if (targetInfo.getJavaToolchainInfo() == null) {
      return Option.none();
    }

    var toolchainInfo = targetInfo.getJavaToolchainInfo();
    var javaHome =
        Uri.fromExecPath(
            Constants.EXEC_ROOT_PREFIX + toolchainInfo.getJavaHome().getPath(),
            bazelData.getExecRoot());
    var buildTarget = new JvmBuildTarget(javaHome.toString(), toolchainInfo.getSourceVersion());
    return Option.some(buildTarget);
  }

  private Set<String> resolveLanguages(
      FileLocation file, Set<String> expectedFileExtensions, Set<String> languages) {
    if (expectedFileExtensions.exists(ext -> file.getPath().endsWith(ext))) {
      return languages;
    } else {
      return HashSet.empty();
    }
  }
}
