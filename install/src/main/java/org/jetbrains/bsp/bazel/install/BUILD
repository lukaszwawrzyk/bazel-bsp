load("@rules_java//java:defs.bzl", "java_library")

java_library(
    name = "install",
    srcs = glob(["*.java"]),
    resources = ["aspects.bzl"],
    visibility = ["//install:__subpackages__"],
    deps = [
        "//commons",
        "@maven//:ch_epfl_scala_bsp4j",
        "@maven//:com_google_code_gson_gson",
        "@maven//:com_google_guava_guava",
        "@maven//:commons_cli_commons_cli",
    ],
)
