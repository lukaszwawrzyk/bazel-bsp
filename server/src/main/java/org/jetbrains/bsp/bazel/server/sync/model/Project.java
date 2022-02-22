package org.jetbrains.bsp.bazel.server.sync.model;

import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import java.net.URI;
import java.util.function.Function;

/** Project is the internal model of the project. Bazel/Aspect Model -> Project -> BSP Model */
public class Project {
  private final Map<URI, Label> sourceToTarget;
  private final List<Module> modules;
  private final Map<Label, Module> moduleMap;

  public Project(List<Module> modules, Map<URI, Label> sourceToTarget) {
    this.sourceToTarget = sourceToTarget;
    this.modules = modules;
    this.moduleMap = modules.toMap(Module::label, Function.identity());
  }

  public List<Module> modules() {
    return modules;
  }

  public Option<Module> findModule(Label label) {
    return moduleMap.get(label);
  }

  public Option<Label> findTargetBySource(URI documentUri) {
    return sourceToTarget.get(documentUri);
  }
}
