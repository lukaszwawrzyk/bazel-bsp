package org.jetbrains.bsp.bazel.server.sync.languages;

import io.vavr.control.Option;
import org.jetbrains.bsp.bazel.info.BspTargetInfo.TargetInfo;

public interface LanguagePlugin<T> {
    Option<T> resolveModule(TargetInfo targetInfo);
}
