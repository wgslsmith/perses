package(default_visibility = [
    "//visibility:public",
])

load("//src/org/perses/grammar:antlr_util.bzl", "antlr_codegen_combined")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//antlropt/src/org/perses/antlr/pnf:pnf.bzl", "pnf_convert")

pnf_convert(
    name = "normalize_wgsl_to_pnf",
    src = "WGSL.g4",
    out = "PnfWGSL.g4",
    start_rule_name = "translation_unit",
)

antlr_codegen_combined(
    name = "pnf_wgsl_grammar",
    grammar_file = "PnfWGSL.g4",
    java_pkg_name = "org.perses.grammar.wgsl",
    lexer_java_file_name = "PnfWGSLLexer.java",
    parser_java_file_name = "PnfWGSLParser.java",
)

kt_jvm_library(
    name = "language_kind",
    srcs = ["LanguageWgsl.kt"],
    deps = [
        "//src/org/perses/program:language_kind",
    ],
)

java_library(
    name = "parser_facade",
    srcs = ["WgslParserFacade.java"],
    deps = [
        ":language_kind",
        ":pnf_wgsl_grammar",
        "//:flogger",
        "//:guava",
        "//src/org/perses/antlr:parse_tree_with_parser",
        "//src/org/perses/grammar:grammar_facade",
        "//src/org/perses/program:language_kind",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)
