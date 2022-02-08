package org.jetbrains.bsp.bazel.server.sync.languages.java;

import io.vavr.collection.Seq;
import io.vavr.control.Option;
import java.util.Objects;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;
import org.jetbrains.bsp.bazel.server.sync.BazelPathsResolver;

public class JdkResolver {
  private final BazelPathsResolver bazelPathsResolver;

  public JdkResolver(BazelPathsResolver bazelPathsResolver) {
    this.bazelPathsResolver = bazelPathsResolver;
  }

  public Option<Jdk> resolve(Seq<TargetInfo> targets) {
    var allCandidates = targets.flatMap(this::resolveJdk);
    if (allCandidates.isEmpty()) return Option.none();
    var version = findLatestVersion(allCandidates);
    var candidates = candidatesByVersion(allCandidates, version);
    if (candidates.size() == 1) {
      return Option.some(candidates.single().jdk);
    }
    return pickCandidateFromJvmRuntime(candidates)
        .orElse(() -> pickCandidateWithJavaHome(candidates))
        .orElse(() -> pickAnyCandidate(candidates))
        .map(c -> c.jdk);
  }

  private String findLatestVersion(Seq<JdkCandidate> candidates) {
    var maxVersionCandidate =
        candidates.maxBy(candidate -> Integer.valueOf(candidate.jdk.javaVersion())).get();
    return maxVersionCandidate.jdk.javaVersion();
  }

  private Seq<JdkCandidate> candidatesByVersion(Seq<JdkCandidate> candidates, String version) {
    var byVersion = candidates.filter(candidate -> candidate.jdk.javaVersion().equals(version));
    // sorted for deterministic output
    var sorted = byVersion.sortBy(c -> c.jdk.javaHome().toString());
    return sorted;
  }

  private Option<JdkCandidate> pickCandidateFromJvmRuntime(Seq<JdkCandidate> candidates) {
    return candidates.find(candidate -> candidate.isRuntime);
  }

  private Option<JdkCandidate> pickCandidateWithJavaHome(Seq<JdkCandidate> candidates) {
    return candidates.find(candidate -> candidate.jdk.javaHome().isDefined());
  }

  private Option<JdkCandidate> pickAnyCandidate(Seq<JdkCandidate> candidates) {
    return candidates.headOption();
  }

  private Option<JdkCandidate> resolveJdk(TargetInfo targetInfo) {
    if (!targetInfo.hasJavaToolchainInfo()) {
      return Option.none();
    }
    var toolchainInfo = targetInfo.getJavaToolchainInfo();

    var isRuntime =
        targetInfo.hasJavaRuntimeInfo() && targetInfo.getJavaRuntimeInfo().hasJavaHome();
    var javaHomeFile =
        isRuntime
            ? (targetInfo.getJavaRuntimeInfo().getJavaHome())
            : (toolchainInfo.hasJavaHome() ? toolchainInfo.getJavaHome() : null);

    var javaHome = Option.of(javaHomeFile).map(bazelPathsResolver::resolveUri);

    var version = toolchainInfo.getSourceVersion();
    return Option.some(new JdkCandidate(new Jdk(version, javaHome), isRuntime));
  }

  private static class JdkCandidate {
    final Jdk jdk;
    final boolean isRuntime;

    public JdkCandidate(Jdk jdk, boolean isRuntime) {
      this.jdk = jdk;
      this.isRuntime = isRuntime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      JdkCandidate that = (JdkCandidate) o;
      return isRuntime == that.isRuntime && jdk.equals(that.jdk);
    }

    @Override
    public int hashCode() {
      return Objects.hash(jdk, isRuntime);
    }
  }
}
