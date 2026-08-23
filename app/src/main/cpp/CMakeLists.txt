cmake_minimum_required(VERSION 3.22.1)

project("skillora-llm" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 17)

# Set llama.cpp options
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "llama: build examples" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "llama: build tests" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "llama: build server" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "llama: curl" FORCE)
set(LLAMA_LLGUIDANCE OFF CACHE BOOL "llama: llguidance" FORCE)

# Optimization for Android
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(GGML_OPENMP OFF CACHE BOOL "ggml: use OpenMP" FORCE)
endif()

# Include llama.cpp as a subdirectory
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
add_subdirectory(${LLAMA_DIR} llama_build)

# Essential common utility sources for inference and quiz generation
set(COMMON_DIR ${LLAMA_DIR}/common)
set(COMMON_SOURCES
        ${COMMON_DIR}/common.cpp
        ${COMMON_DIR}/sampling.cpp
        ${COMMON_DIR}/log.cpp
        ${COMMON_DIR}/chat.cpp
        ${COMMON_DIR}/unicode.cpp
        ${COMMON_DIR}/json-schema-to-grammar.cpp
        ${COMMON_DIR}/reasoning-budget.cpp
        ${COMMON_DIR}/trie.cpp
        ${COMMON_DIR}/chat-peg-parser.cpp
        ${COMMON_DIR}/peg-parser.cpp
        ${COMMON_DIR}/fit.cpp
        ${COMMON_DIR}/chat-auto-parser-generator.cpp
        ${COMMON_DIR}/chat-auto-parser-helpers.cpp
        ${COMMON_DIR}/chat-diff-analyzer.cpp
        ${COMMON_DIR}/jinja/lexer.cpp
        ${COMMON_DIR}/jinja/parser.cpp
        ${COMMON_DIR}/jinja/runtime.cpp
        ${COMMON_DIR}/jinja/string.cpp
        ${COMMON_DIR}/jinja/value.cpp
        ${COMMON_DIR}/jinja/caps.cpp
        # Build information implementation
        build-info.cpp
)

# Add our JNI wrapper library
add_library(llama-android SHARED
        llama-android.cpp
        ${COMMON_SOURCES}
)

# Include directories
target_include_directories(llama-android PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/common
        ${LLAMA_DIR}/ggml/include
        ${LLAMA_DIR}/vendor
)

# Link against llama and ggml libraries
target_link_libraries(llama-android
        llama
        ggml
        log
        android
)
