load("@rules_java//java:defs.bzl", "java_library")
load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_library(
    name = "sync",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        ":bsp_target_info_java_proto",
        "//bazelrunner",
        "//commons",
        "//projectview",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/bep",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/bsp/managers",
        "@com_google_protobuf//:protobuf_java",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:io_vavr_vavr",
        "@maven//:org_eclipse_lsp4j_org_eclipse_lsp4j_jsonrpc",
    ],
)

java_proto_library(
    name = "bsp_target_info_java_proto",
    visibility = ["//visibility:private"],
    deps = [":bsp_target_info_proto"],
)

proto_library(
    name = "bsp_target_info_proto",
    srcs = ["bsp_target_info.proto"],
    visibility = ["//visibility:private"],
)
