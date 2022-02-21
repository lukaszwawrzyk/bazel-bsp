package org.jetbrains.bsp.bazel.server.sync.languages;

import ch.epfl.scala.bsp4j.BuildTarget;
import io.vavr.control.Option;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public abstract class LanguagePlugin<T> {
  public abstract Option<T> resolveModule(TargetInfo targetInfo);

  public void setModuleData(Object moduleData, BuildTarget buildTarget) {
    applyModuleData((T) moduleData, buildTarget);
  }

  protected abstract void applyModuleData(T moduleData, BuildTarget buildTarget);
}
