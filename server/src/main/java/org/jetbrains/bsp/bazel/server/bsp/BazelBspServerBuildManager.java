package org.jetbrains.bsp.bazel.server.bsp;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileResult;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.jetbrains.bsp.bazel.commons.Lazy;
import org.jetbrains.bsp.bazel.server.bep.BepServer;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspAspectsManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspCompilationManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspQueryManager;
import org.jetbrains.bsp.bazel.server.bsp.managers.BazelBspTargetManager;

public class BazelBspServerBuildManager {

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
