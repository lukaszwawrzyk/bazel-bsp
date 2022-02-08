load("@rules_java//java:defs.bzl", "java_library")
load("@rules_java//java:defs.bzl", "java_proto_library")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_library(
    name = "sync",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//bazelrunner",
        "//commons",
        "//projectview",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/bsp/managers",
        ":bsp_target_info_java_proto",
        "@maven//:ch_epfl_scala_bsp4j",
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
