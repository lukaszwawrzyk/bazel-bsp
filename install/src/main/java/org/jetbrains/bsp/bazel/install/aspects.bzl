def _print_aspect_impl(target, ctx):
    if hasattr(ctx.rule.attr, "srcjar"):
        srcjar = ctx.rule.attr.srcjar
        if srcjar != None:
            for f in srcjar.files.to_list():
                if f != None:
                    print(f.path)
    return []

print_aspect = aspect(
    implementation = _print_aspect_impl,
    attr_aspects = ["deps"],
)

def _scala_compiler_classpath_impl(target, ctx):
    files = depset()
    if hasattr(ctx.rule.attr, "jars"):
        for target in ctx.rule.attr.jars:
            files = depset(transitive = [files, target.files])

    compiler_classpath_file = ctx.actions.declare_file("%s.textproto" % target.label.name)
    ctx.actions.write(compiler_classpath_file, struct(files = [file.path for file in files.to_list()]).to_proto())

    return [
        OutputGroupInfo(scala_compiler_classpath_files = [compiler_classpath_file]),
    ]

scala_compiler_classpath_aspect = aspect(
    implementation = _scala_compiler_classpath_impl,
)

def filter(f, xs):
    return [x for x in xs if f(x)]

def map(f, xs):
    return [f(x) for x in xs]

def map_not_none(f, xs):
    rs = [f(x) for x in xs if x != None]
    return [r for r in rs if r != None]

def distinct(xs):
    seen = dict()
    res = []
    for x in xs:
        if x not in seen:
            seen[x] = True
            res.add(x)
    return res

def file_location(file):
    if file == None:
        return None
    return struct(path = file.path)

def get_java_provider(target):
    if hasattr(target, "scala"):
        return target.scala
    if hasattr(target, "kt") and hasattr(target.kt, "outputs"):
        return target.kt
    if JavaInfo in target:
        return target[JavaInfo]
    return None

def get_interface_jars(output):
    if hasattr(output, "compile_jar") and output.compile_jar:
        return [output.compile_jar]
    elif hasattr(output, "ijar") and output.ijar:
        return [output.ijar]
    else:
        return []

def get_source_jars(output):
    if hasattr(output, "source_jars"):
        return output.source_jars
    if hasattr(output, "source_jar"):
        return [output.source_jar]
    return []

def get_generated_jars(provider):
    if (hasattr(provider, "java_outputs")):
        return map_not_none(to_generated_jvm_outputs, provider.java_outputs)

    if hasattr(provider, "annotation_processing") and provider.annotation_processing and provider.annotation_processing.enabled:
        return [struct(
           binary_jars = [file_location(provider.annotation_processing.class_jar)],
           source_jars = [file_location(provider.annotation_processing.source_jar)],
       )]

    return []

def to_generated_jvm_outputs(output):
    if output == None or output.generated_class_jar == None:
        return None

    return struct(
        binary_jars = [file_location(output.generated_class_jar)],
        source_jars = [file_location(output.generated_source_jar)],
    )

def to_jvm_outputs(output):
    if output == None or output.class_jar == None:
        return None

    return struct(
        binary_jars = [file_location(output.class_jar)],
        interface_jars = map(file_location, get_interface_jars(output)),
        source_jars = map(file_location, get_source_jars(output)),
    )

def extract_java_info(target, ctx, result):
    provider = get_java_provider(target)
    if not provider:
        return

    if hasattr(provider, "java_outputs") and provider.java_outputs:
        java_outputs = provider.java_outputs
    elif hasattr(provider, "outputs") and provider.outputs:
        java_outputs = provider.outputs.jars
    else:
        return

    jars = map_not_none(to_jvm_outputs, java_outputs)

    generated_jars = get_generated_jars(provider)

    java_info = struct(
        jars = jars,
        generated_jars = generated_jars,
    )
    result["java_target_info"] = java_info

def get_aspect_ids(ctx, target):
    """Returns the all aspect ids, filtering out self."""
    aspect_ids = None
    if hasattr(ctx, "aspect_ids"):
        aspect_ids = ctx.aspect_ids
    elif hasattr(target, "aspect_ids"):
        aspect_ids = target.aspect_ids
    else:
        return None
    return [aspect_id for aspect_id in aspect_ids if "bsp_target_info_aspect" not in aspect_id]

def abs(num):
    if num < 0:
        return -num
    else:
        return num

def update_sync_output_groups(groups_dict, key, new_set):
    update_set_in_dict(groups_dict, key + "-transitive-deps", new_set)
    update_set_in_dict(groups_dict, key + "-outputs", new_set)
    update_set_in_dict(groups_dict, key + "-direct-deps", new_set)

def update_set_in_dict(input_dict, key, other_set):
    input_dict[key] = depset(transitive = [input_dict.get(key, depset()), other_set])

def _collect_target_from_attr(rule_attrs, attr_name, result):
    """Collects the targets from the given attr into the result."""
    if not hasattr(rule_attrs, attr_name):
        return
    attr_value = getattr(rule_attrs, attr_name)
    type_name = type(attr_value)
    if type_name == "Target":
        result.append(attr_value)
    elif type_name == "list":
        result.extend(attr_value)

def is_valid_aspect_target(target):
    return hasattr(target, "bsp_info")

def collect_targets_from_attrs(rule_attrs, attrs):
    result = []
    for attr_name in attrs:
        _collect_target_from_attr(rule_attrs, attr_name, result)
    return [target for target in result if is_valid_aspect_target(target)]

COMPILE = 0
RUNTIME = 1

def make_dep(dep, dependency_type):
    return struct(
        id = str(dep.bsp_info.id),
        dependency_type = dependency_type,
    )

def make_deps(deps, dependency_type):
    return [make_dep(dep, dependency_type) for dep in deps]

def _is_proto_library_wrapper(target, ctx):
    if not ctx.rule.kind.endswith("proto_library") or ctx.rule.kind == "proto_library":
        return False

    deps = collect_targets_from_attrs(ctx.rule.attr, ["deps"])
    return len(deps) == 1 and deps[0].bsp_info and deps[0].bsp_info.kind == "proto_library"

def _get_forwarded_deps(target, ctx):
    if _is_proto_library_wrapper(target, ctx):
        return collect_targets_from_attrs(ctx.rule.attr, ["deps"])
    return []

def _bsp_target_info_aspect_impl(target, ctx):
    rule_attrs = ctx.rule.attr

    direct_dep_targets = collect_targets_from_attrs(rule_attrs, ["deps", "jars"])
    direct_deps = make_deps(direct_dep_targets, COMPILE)

    exported_deps_from_deps = []
    for dep in direct_dep_targets:
        exported_deps_from_deps = exported_deps_from_deps + dep.bsp_info.export_deps

    compile_deps = direct_deps + exported_deps_from_deps

    runtime_dep_targets = collect_targets_from_attrs(rule_attrs, ["runtime_deps"])
    runtime_deps = make_deps(runtime_dep_targets, RUNTIME)

    all_deps = depset(compile_deps + runtime_deps).to_list()

    # Propagate my own exports
    export_deps = []
    direct_exports = []
    if JavaInfo in target:
        direct_exports = collect_targets_from_attrs(rule_attrs, ["exports"])
        export_deps.extend(make_deps(direct_exports, COMPILE))
        for export in direct_exports:
            export_deps.extend(export.bsp_info.export_deps)
        export_deps = depset(export_deps).to_list()

    forwarded_deps = _get_forwarded_deps(target, ctx) + direct_exports

    dep_targets = direct_dep_targets + runtime_dep_targets + direct_exports
    output_groups = dict()
    for dep in dep_targets:
        for k, v in dep.bsp_info.output_groups.items():
            if dep in forwarded_deps:
                output_groups[k] = output_groups[k] + [v] if k in output_groups else [v]
            elif k.endswith("-direct-deps"):
                pass
            elif k.endswith("-outputs"):
                directs = k[:-len("outputs")] + "direct-deps"
                output_groups[directs] = output_groups[directs] + [v] if directs in output_groups else [v]
            else:
                output_groups[k] = output_groups[k] + [v] if k in output_groups else [v]

    for k, v in output_groups.items():
        output_groups[k] = depset(transitive = v)

    sources = [
        file_location(f)
        for t in getattr(ctx.rule.attr, "srcs", [])
        for f in t.files.to_list()
    ]

    result = dict(
        id = str(target.label),
        kind = ctx.rule.kind,
        tags = rule_attrs.tags,
        dependencies = list(all_deps),
        sources = sources,
    )


    extract_java_info(target, ctx, result)

    file_name = target.label.name
    file_name = file_name + "-" + str(abs(hash(file_name)))
    aspect_ids = get_aspect_ids(ctx, target)
    if aspect_ids:
        file_name = file_name + "-" + str(abs(hash(".".join(aspect_ids))))
    file_name = "%s.bsp-info.textproto" % file_name
    info_file = ctx.actions.declare_file(file_name)
    ctx.actions.write(info_file, proto.encode_text(struct(**result)))
    update_sync_output_groups(output_groups, "bsp-target-info", depset([info_file]))

    return struct(
        bsp_info = struct(
            id = target.label,
            kind = ctx.rule.kind,
            export_deps = export_deps,
            output_groups = output_groups,
        ),
        output_groups = output_groups,
    )


bsp_target_info_aspect = aspect(
    implementation = _bsp_target_info_aspect_impl,
    required_aspect_providers = [[JavaInfo]],
    attr_aspects = ["deps", "runtime_deps", "jars"]
)

def _fetch_cpp_compiler(target, ctx):
    if cc_common.CcToolchainInfo in target:
        toolchain_info = target[cc_common.CcToolchainInfo]
        print(toolchain_info.compiler)
        print(toolchain_info.compiler_executable)
    return []

fetch_cpp_compiler = aspect(
    implementation = _fetch_cpp_compiler,
    fragments = ["cpp"],
    attr_aspects = ["_cc_toolchain"],
    required_aspect_providers = [[CcInfo]],
)

def _fetch_java_target_version(target, ctx):
    if hasattr(ctx.rule.attr, "target_version"):
        print(ctx.rule.attr.target_version)
    return []

fetch_java_target_version = aspect(
    implementation = _fetch_java_target_version,
    attr_aspects = ["_java_toolchain"],
)

def _get_target_info(ctx, field_name):
    fields = getattr(ctx.rule.attr, field_name, [])
    fields = [ctx.expand_location(field) for field in fields]
    fields = [ctx.expand_make_variables(field_name, field, {}) for field in fields]

    return fields

def _print_fields(fields):
    separator = ","
    print(separator.join(fields))

def _get_cpp_target_info(target, ctx):
    if CcInfo not in target:
        return []

    #TODO: Get copts from semantics
    copts = _get_target_info(ctx, "copts")
    defines = _get_target_info(ctx, "defines")
    linkopts = _get_target_info(ctx, "linkopts")

    linkshared = False
    if hasattr(ctx.rule.attr, "linkshared"):
        linkshared = ctx.rule.attr.linkshared

    _print_fields(copts)
    _print_fields(defines)
    _print_fields(linkopts)
    print(linkshared)

    return []

get_cpp_target_info = aspect(
    implementation = _get_cpp_target_info,
    fragments = ["cpp"],
    required_aspect_providers = [[CcInfo]],
)
