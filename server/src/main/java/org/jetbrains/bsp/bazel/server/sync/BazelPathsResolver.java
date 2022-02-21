package org.jetbrains.bsp.bazel.server.sync;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.bsp.bazel.bazelrunner.data.BazelData;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.FileLocation;
import org.jetbrains.bsp.bazel.server.sync.model.Label;

public class BazelPathsResolver {
  private final BazelData bazelData;

  public BazelPathsResolver(BazelData bazelData) {
    this.bazelData = bazelData;
  }

  public Path resolve(FileLocation fileLocation) {
    if (isMainWorkspaceSource(fileLocation)) {
      return resolveSource(fileLocation);
    } else {
      return resolveOutput(fileLocation);
    }
  }

  private Path resolveOutput(FileLocation fileLocation) {
    return Paths.get(
        bazelData.getExecRoot(),
        fileLocation.getRootExecutionPathFragment(),
        fileLocation.getRelativePath());
  }

  private Path resolveSource(FileLocation fileLocation) {
    return Paths.get(bazelData.getWorkspaceRoot(), fileLocation.getRelativePath());
  }

  private boolean isMainWorkspaceSource(FileLocation fileLocation) {
    return fileLocation.getIsSource() && !fileLocation.getIsExternal();
  }

  public Path labelToDirectory(Label label) {
    var relativePath = extractRelativePath(label.getValue());
    return Paths.get(bazelData.getWorkspaceRoot(), relativePath);
  }

  private String extractRelativePath(String label) {
    var prefix = "//";
    if (!label.startsWith(prefix)) {
      throw new IllegalArgumentException(String.format("%s didn't start with %s", label, prefix));
    }
    var labelWithoutPrefix = label.substring(prefix.length());
    var parts = labelWithoutPrefix.split(":");
    if (parts.length != 2) {
      throw new IllegalArgumentException(
          String.format("Label %s didn't contain exactly one ':'", label));
    }
    return parts[0];
  }
}
