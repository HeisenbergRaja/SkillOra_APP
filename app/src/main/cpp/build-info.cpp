#include "build-info.h"

int llama_build_number() { return 1; }
char const * llama_commit() { return "unknown"; }
char const * llama_compiler() { return "clang"; }
char const * llama_build_target() { return "android"; }
char const * llama_build_info() { return "skillora-llm-v1"; }
void llama_print_build_info(char const * prefix) { }

#include "llama.h"
#include <cstdio>
