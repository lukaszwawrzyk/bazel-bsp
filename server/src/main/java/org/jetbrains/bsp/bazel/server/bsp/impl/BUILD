load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "impl",
    srcs = glob(["*.java"]),
    visibility = ["//server:__subpackages__"],
    deps = [
        "//commons",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/bsp",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/bsp/services",
        "//server/src/main/java/org/jetbrains/bsp/bazel/server/sync",
        "@maven//:org_apache_logging_log4j_log4j_api",
        "@maven//:org_apache_logging_log4j_log4j_core",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_guava_guava",
    ],
)
