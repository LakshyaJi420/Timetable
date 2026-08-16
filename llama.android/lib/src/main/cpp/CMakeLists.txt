cmake_minimum_required(VERSION 3.22.1)

project("ai-chat" VERSION 1.0.0 LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --------------------------------------------------------------------------
# Android / ABI configuration
# --------------------------------------------------------------------------

if(ANDROID)
    if(NOT DEFINED ANDROID_ABI)
        message(FATAL_ERROR "ANDROID_ABI is not defined")
    endif()

    message(STATUS "Detected Android ABI: ${ANDROID_ABI}")

    if(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GGML_SYSTEM_ARCH "ARM")
        set(GGML_CPU_KLEIDIAI ON)
        set(GGML_OPENMP ON)

    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GGML_SYSTEM_ARCH "x86")
        set(GGML_CPU_KLEIDIAI OFF)
        set(GGML_OPENMP OFF)

    else()
        message(FATAL_ERROR
            "Unsupported Android ABI: ${ANDROID_ABI}"
        )
    endif()
endif()

# --------------------------------------------------------------------------
# Locate llama.cpp
# --------------------------------------------------------------------------
#
# CMakeLists.txt is located at:
#
#   llama.android/lib/src/main/cpp/CMakeLists.txt
#
# llama.cpp is located at:
#
#   Timetable/llama.cpp/
#
# Therefore ../../../../../ brings us to the repository root,
# then /llama.cpp selects the llama.cpp source tree.
#

set(LLAMA_SRC
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../llama.cpp"
)

# Normalize the path
get_filename_component(LLAMA_SRC
    "${LLAMA_SRC}"
    ABSOLUTE
)

message(STATUS "==================================================")
message(STATUS "AI Chat native build")
message(STATUS "LLAMA_SRC = ${LLAMA_SRC}")
message(STATUS "ANDROID_ABI = ${ANDROID_ABI}")
message(STATUS "==================================================")

# Make sure llama.cpp actually exists
if(NOT EXISTS "${LLAMA_SRC}")
    message(FATAL_ERROR
        "llama.cpp directory not found at:\n"
        "  ${LLAMA_SRC}\n\n"
        "Expected repository structure:\n"
        "  Timetable/llama.cpp/CMakeLists.txt"
    )
endif()

# Make sure the llama.cpp CMake file exists
if(NOT EXISTS "${LLAMA_SRC}/CMakeLists.txt")
    message(FATAL_ERROR
        "llama.cpp CMakeLists.txt not found at:\n"
        "  ${LLAMA_SRC}/CMakeLists.txt\n\n"
        "Make sure the llama.cpp source tree is present."
    )
endif()

message(STATUS "Found llama.cpp:")
message(STATUS "  ${LLAMA_SRC}")

# --------------------------------------------------------------------------
# Configure llama.cpp
# --------------------------------------------------------------------------

add_subdirectory(
    "${LLAMA_SRC}"
    "${CMAKE_CURRENT_BINARY_DIR}/build-llama"
)

# --------------------------------------------------------------------------
# AI Chat shared library
# --------------------------------------------------------------------------

add_library(
    ${CMAKE_PROJECT_NAME}
    SHARED
    ai_chat.cpp
)

# --------------------------------------------------------------------------
# Compile definitions
# --------------------------------------------------------------------------

target_compile_definitions(
    ${CMAKE_PROJECT_NAME}
    PRIVATE
        GGML_SYSTEM_ARCH=${GGML_SYSTEM_ARCH}
        GGML_CPU_KLEIDIAI=$<BOOL:${GGML_CPU_KLEIDIAI}>
        GGML_OPENMP=$<BOOL:${GGML_OPENMP}>
)

# --------------------------------------------------------------------------
# Include directories
# --------------------------------------------------------------------------

target_include_directories(
    ${CMAKE_PROJECT_NAME}
    PRIVATE
        "${LLAMA_SRC}"
        "${LLAMA_SRC}/common"
        "${LLAMA_SRC}/include"
        "${LLAMA_SRC}/ggml/include"
        "${LLAMA_SRC}/ggml/src"
)

# --------------------------------------------------------------------------
# Android libraries + llama.cpp
# --------------------------------------------------------------------------

target_link_libraries(
    ${CMAKE_PROJECT_NAME}
    PRIVATE
        llama
        llama-common
        android
        log
)
