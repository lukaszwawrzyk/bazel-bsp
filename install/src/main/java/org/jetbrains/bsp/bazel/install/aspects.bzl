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

    sources = [
        file_location(f)
        for t in getattr(ctx.rule.attr, "srcs", [])
        for f in t.files.to_list()
    ]

    jars = map_not_none(to_jvm_outputs, java_outputs)

    generated_jars = get_generated_jars(provider)

    java_info = struct(
        sources = sources,
        jars = jars,
        generated_jars = generated_jars,
    )
    result["java_target_info"] = java_info

def _bsp_target_info_aspect_impl(target, ctx):
    result = dict(
        id = str(target.label),
    )

    extract_java_info(target, ctx, result)

    info_file = ctx.actions.declare_file("%s-bsp-info.textproto" % target.label.name)
    ctx.actions.write(info_file, proto.encode_text(struct(**result)))

    return [
        OutputGroupInfo(bsp_target_info_file = [info_file]),
    ]

bsp_target_info_aspect = aspect(
    # TODO figure out attr_aspects attribute to handle transitive deps https://docs.bazel.build/versions/main/skylark/aspects.html
    implementation = _bsp_target_info_aspect_impl,
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
