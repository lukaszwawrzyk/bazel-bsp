package org.jetbrains.bsp.bazel.server.bsp;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.SourceItem;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.jetbrains.bsp.bazel.commons.Constants;
import org.jetbrains.bsp.bazel.commons.Lazy;
import org.jetbrains.bsp.bazel.server.bep.BepServer;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspQueryManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspTargetManager;

public class BazelBspServerBuildManager {

  public static final String BAZEL_PRINT_ASPECT = "print_aspect";

  private final BazelBspQueryManager bazelBspQueryManager;
  private final BazelBspCompilationManager bazelBspCompilationManager;
  private final BazelBspTargetManager bazelBspTargetManager;
  private final BazelBspAspectsManager bazelBspAspectsManager;

  private BepServer bepServer;

  public BazelBspServerBuildManager(
          BazelBspCompilationManager bazelBspCompilationManager,
          BazelBspAspectsManager bazelBspAspectsManager,
          BazelBspTargetManager bazelBspTargetManager,
          BazelBspQueryManager bazelBspQueryManager) {
    this.bazelBspCompilationManager = bazelBspCompilationManager;
    this.bazelBspAspectsManager = bazelBspAspectsManager;
    this.bazelBspTargetManager = bazelBspTargetManager;
    this.bazelBspQueryManager = bazelBspQueryManager;
  }

  public List<SourceItem> getSourceItems(Build.Rule rule, BuildTargetIdentifier label) {
    return bazelBspQueryManager.getSourceItems(rule, label);
  }

  public List<String> lookUpTransitiveSourceJars(String target) {
    // TODO(illicitonion): Use an aspect output group, rather than parsing stderr
    // logging
    return bazelBspAspectsManager
        .fetchLinesFromAspect(target, BAZEL_PRINT_ASPECT)
        .filter(path -> path.endsWith(".jar"))
        .map(path -> Constants.EXEC_ROOT_PREFIX + path)
        .collect(Collectors.toList());
  }

  public void setBepServer(BepServer bepServer) {
    this.bepServer = bepServer;
    this.bazelBspQueryManager.setBepServer(bepServer);
    this.bazelBspCompilationManager.setBepServer(bepServer);
    this.bazelBspAspectsManager.setBepServer(bepServer);
  }

  public Either<ResponseError, CompileResult> buildTargetsWithBep(
      List<BuildTargetIdentifier> targets, ArrayList<String> extraFlags) {
    if (bepServer.getBuildTargetsSources().isEmpty()) {
      bazelBspQueryManager.getWorkspaceBuildTargets();
    }
    return bazelBspCompilationManager.buildTargetsWithBep(targets, extraFlags);
  }

  public List<Lazy<?>> getLazyVals() {
    return ImmutableList.of(
        bazelBspTargetManager.getBazelBspJvmTargetManager(),
        bazelBspTargetManager.getBazelBspScalaTargetManager());
  }
}
