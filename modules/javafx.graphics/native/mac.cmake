# Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.

# macOS-specific portion of the javafx.graphics native build. Replicates the
# compiler/linker flags previously defined by the retired Gradle native build
# (buildSrc/mac.gradle). Included from CMakeLists.txt; shared input variables
# (JDK_HOME, GENSRC_DIR, HEADERS_DIR, BIN_DIR, INCLUDE_ES2, GRAPHICS_SRC) are
# defined there.
#
# Notes on Gradle parity:
#   - Gradle compiled with clang and linked everything with clang++ (plus
#     "-dynamiclib -lobjc" and the framework list below); forcing
#     LINKER_LANGUAGE CXX matches that.
#   - The -mmacosx-version-min / -isysroot / -iframework / -arch flags that
#     Gradle passed explicitly are covered by CMAKE_OSX_DEPLOYMENT_TARGET
#     (set in CMakeLists.txt) and CMake's built-in Apple SDK handling; the
#     target architecture defaults to the host architecture, matching the
#     per-arch CI builds (Gradle built x86_64 by default and took TARGET_ARCH
#     to override; universal/lipo builds were not ported).
#   - Gradle stripped the .dylib files (strip -x) only when copying them into
#     the SDK image; the libraries are left unstripped here, same as the
#     build/ output of the Gradle build.
#   - The static (IS_STATIC_BUILD) and parfait toolchain variants of the
#     Gradle build were not ported.

enable_language(OBJC)

# ---------------------------------------------------------------------------
# macOS-only inputs from Maven
# ---------------------------------------------------------------------------
set(SHADER_OBJ_DIR "" CACHE PATH "Output directory for compiled shader resources (jfxshaders.metallib)")
# Mirrors msl.version in build.properties (Gradle: mslVersion / CompileMSLTask)
set(MSL_VERSION "macos-metal2.4" CACHE STRING "Metal Shading Language standard version")

if(NOT SHADER_OBJ_DIR)
    message(FATAL_ERROR "SHADER_OBJ_DIR must be provided")
endif()

# ---------------------------------------------------------------------------
# Locate the Metal Shading Language tools (Gradle: xcrun -f --sdk macosx ...)
# ---------------------------------------------------------------------------
execute_process(COMMAND xcrun -f --sdk macosx metal
    OUTPUT_VARIABLE METAL_COMPILER OUTPUT_STRIP_TRAILING_WHITESPACE
    RESULT_VARIABLE METAL_COMPILER_RESULT)
execute_process(COMMAND xcrun -f --sdk macosx metallib
    OUTPUT_VARIABLE METAL_LINKER OUTPUT_STRIP_TRAILING_WHITESPACE
    RESULT_VARIABLE METAL_LINKER_RESULT)
if(NOT METAL_COMPILER_RESULT EQUAL 0 OR NOT METAL_LINKER_RESULT EQUAL 0
        OR METAL_COMPILER STREQUAL "" OR METAL_LINKER STREQUAL "")
    message(FATAL_ERROR "Cannot find Metal Shader Language (MSL) tools: metal and metallib. "
        "Please make sure that MSL tools metal and metallib are installed and available on PATH.")
endif()

# ---------------------------------------------------------------------------
# Deployment target version split for the glass defines
# (Gradle: MACOS_MIN_VERSION_MAJOR / MACOS_MIN_VERSION_MINOR)
# ---------------------------------------------------------------------------
string(REPLACE "." ";" MACOS_MIN_VERSION_PARTS "${CMAKE_OSX_DEPLOYMENT_TARGET}")
list(GET MACOS_MIN_VERSION_PARTS 0 MACOS_MIN_VERSION_MAJOR)
list(LENGTH MACOS_MIN_VERSION_PARTS MACOS_MIN_VERSION_PARTS_LENGTH)
if(MACOS_MIN_VERSION_PARTS_LENGTH GREATER 1)
    list(GET MACOS_MIN_VERSION_PARTS 1 MACOS_MIN_VERSION_MINOR)
else()
    set(MACOS_MIN_VERSION_MINOR 0)
endif()

# ---------------------------------------------------------------------------
# Global flags: exact parity with the retired Gradle macOS toolchain config,
# replacing CMake defaults
# ---------------------------------------------------------------------------
foreach(lang C CXX OBJC)
    set(CMAKE_${lang}_FLAGS "")
    set(CMAKE_${lang}_FLAGS_RELEASE "")
    set(CMAKE_${lang}_FLAGS_DEBUG "")
endforeach()
set(CMAKE_SHARED_LINKER_FLAGS "")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "")
set(CMAKE_SHARED_LINKER_FLAGS_DEBUG "")

# Gradle ccFlags = ccBaseFlags + "-std=c99" + (-O3 -DNDEBUG | -DDEBUG).
# CCTask only applied -std=c99 when compiling .c and .m files, mirrored here
# with the language generator expression.
set(JFX_CC_OPTIONS
    "$<$<OR:$<COMPILE_LANGUAGE:C>,$<COMPILE_LANGUAGE:OBJC>>:-std=c99>"
    "$<$<CONFIG:Debug>:-DDEBUG>"
    "$<$<NOT:$<CONFIG:Debug>>:-O3;-DNDEBUG>")

# Gradle dynamicLinkFlags (CMake adds -dynamiclib itself); used by glass, iio,
# prismES2, prismMTL and font. prism and prismSW linked with
# dynamicLinkFlagsAlt (no frameworks, no -lobjc), i.e. an empty LINK_LIBS.
set(JFX_FRAMEWORK_LINK_LIBS
    "-framework AppKit"
    "-framework ApplicationServices"
    "-framework Carbon"
    "-framework OpenGL"
    "-framework QuartzCore"
    "-framework Security"
    "-framework Network"
    "-framework Metal"
    objc)

# ---------------------------------------------------------------------------
# add_jfx_library(<name>
#     OUTPUT_NAME <dylib base name>  # produces lib<OUTPUT_NAME>.dylib
#     SOURCE_DIRS <dirs...>          # globbed non-recursively, like Gradle listFiles()
#     INCLUDE_DIRS <dirs...>
#     COMPILE_OPTIONS <opts...>
#     LINK_LIBS <libs...>
# )
# ---------------------------------------------------------------------------
function(add_jfx_library name)
    cmake_parse_arguments(JFX "" "OUTPUT_NAME"
        "SOURCE_DIRS;INCLUDE_DIRS;COMPILE_OPTIONS;LINK_LIBS" ${ARGN})

    set(sources)
    foreach(dir ${JFX_SOURCE_DIRS})
        file(GLOB dir_sources "${dir}/*.c" "${dir}/*.cc" "${dir}/*.cpp" "${dir}/*.m")
        list(APPEND sources ${dir_sources})
    endforeach()

    add_library(${name} SHARED ${sources})
    set_target_properties(${name} PROPERTIES
        OUTPUT_NAME "${JFX_OUTPUT_NAME}"
        LINKER_LANGUAGE CXX
        LIBRARY_OUTPUT_DIRECTORY "${BIN_DIR}"
        LIBRARY_OUTPUT_DIRECTORY_RELEASE "${BIN_DIR}"
        LIBRARY_OUTPUT_DIRECTORY_DEBUG "${BIN_DIR}")

    target_compile_options(${name} PRIVATE ${JFX_COMPILE_OPTIONS})
    target_include_directories(${name} PRIVATE
        "${JDK_HOME}/include" "${JDK_HOME}/include/darwin"
        "${HEADERS_DIR}"
        ${JFX_SOURCE_DIRS} ${JFX_INCLUDE_DIRS})
    target_link_libraries(${name} PRIVATE ${JFX_LINK_LIBS})
endfunction()

# ---------------------------------------------------------------------------
# libglass.dylib
# ---------------------------------------------------------------------------
add_jfx_library(glass
    OUTPUT_NAME glass
    SOURCE_DIRS "${GRAPHICS_SRC}/native-glass/mac" "${GRAPHICS_SRC}/native-glass/mac/a11y"
    COMPILE_OPTIONS ${JFX_CC_OPTIONS}
        -DGL_SILENCE_DEPRECATION
        -DMACOS_MIN_VERSION_MAJOR=${MACOS_MIN_VERSION_MAJOR}
        -DMACOS_MIN_VERSION_MINOR=${MACOS_MIN_VERSION_MINOR}
        -Werror
    LINK_LIBS ${JFX_FRAMEWORK_LINK_LIBS})

# ---------------------------------------------------------------------------
# libprism_sw.dylib
# ---------------------------------------------------------------------------
add_jfx_library(prismSW
    OUTPUT_NAME prism_sw
    SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-sw"
    COMPILE_OPTIONS -O3 -DINLINE=inline)

# ---------------------------------------------------------------------------
# libprism_es2.dylib (optional, mirrors IS_INCLUDE_ES2; default true on macOS)
# ---------------------------------------------------------------------------
if(INCLUDE_ES2)
    add_jfx_library(prismES2
        OUTPUT_NAME prism_es2
        SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-es2"
            "${GRAPHICS_SRC}/native-prism-es2/GL"
            "${GRAPHICS_SRC}/native-prism-es2/macosx"
        COMPILE_OPTIONS ${JFX_CC_OPTIONS} -DGL_SILENCE_DEPRECATION -DMACOSX
        LINK_LIBS ${JFX_FRAMEWORK_LINK_LIBS})
endif()

# ---------------------------------------------------------------------------
# libprism_mtl.dylib (compiles against the JSLC-generated Metal uniform
# structs in ${GENSRC_DIR}/mtl-headers)
# ---------------------------------------------------------------------------
add_jfx_library(prismMTL
    OUTPUT_NAME prism_mtl
    SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-mtl"
    INCLUDE_DIRS "${GENSRC_DIR}/mtl-headers"
    COMPILE_OPTIONS ${JFX_CC_OPTIONS} -DMACOSX -Werror=objc-method-access
    LINK_LIBS ${JFX_FRAMEWORK_LINK_LIBS})

# ---------------------------------------------------------------------------
# libjavafx_font.dylib (platform-independent font sources; the
# platform-specific files self-exclude via #ifdef guards)
# ---------------------------------------------------------------------------
add_jfx_library(font
    OUTPUT_NAME javafx_font
    SOURCE_DIRS "${GRAPHICS_SRC}/native-font"
    COMPILE_OPTIONS ${JFX_CC_OPTIONS} -DJFXFONT_PLUS
    LINK_LIBS ${JFX_FRAMEWORK_LINK_LIBS})

# ---------------------------------------------------------------------------
# libjavafx_iio.dylib
# ---------------------------------------------------------------------------
add_jfx_library(iio
    OUTPUT_NAME javafx_iio
    SOURCE_DIRS "${GRAPHICS_SRC}/native-iio" "${GRAPHICS_SRC}/native-iio/libjpeg"
    COMPILE_OPTIONS ${JFX_CC_OPTIONS}
    LINK_LIBS ${JFX_FRAMEWORK_LINK_LIBS})

# ---------------------------------------------------------------------------
# Metal shader library bundled into javafx-graphics.jar as
# com/sun/prism/mtl/msl/jfxshaders.metallib. Mirrors the Gradle
# compileDecoraMSLShaders / compilePrismMSLShaders (JSLC-generated .metal,
# compiled with -Wdeprecated and the mtl-headers include),
# compileMetalShaders (handwritten .metal from native-prism-mtl/msl) and
# linkMSLShader tasks.
# ---------------------------------------------------------------------------
set(MSL_AIR_DIR "${CMAKE_CURRENT_BINARY_DIR}/msl")
set(MSL_AIR_FILES)

function(add_msl_shader group src)
    # Remaining arguments are extra metal compiler options
    get_filename_component(base "${src}" NAME_WE)
    set(air "${MSL_AIR_DIR}/${group}/${base}.air")
    # The Makefiles generator does not create custom-command output directories
    file(MAKE_DIRECTORY "${MSL_AIR_DIR}/${group}")
    add_custom_command(OUTPUT "${air}"
        COMMAND "${METAL_COMPILER}" ${ARGN} -std=${MSL_VERSION} -c "${src}" -o "${air}"
        DEPENDS "${src}"
        VERBATIM)
    set(MSL_AIR_FILES ${MSL_AIR_FILES} "${air}" PARENT_SCOPE)
endfunction()

foreach(entry "jsl-decora=com/sun/scenario/effect/impl/hw" "jsl-prism=com/sun/prism")
    string(REPLACE "=" ";" parts "${entry}")
    list(GET parts 0 gendir)
    list(GET parts 1 pkg)
    file(GLOB metal_files "${GENSRC_DIR}/${gendir}/${pkg}/mtl/msl/*.metal")
    foreach(metal ${metal_files})
        add_msl_shader("${gendir}" "${metal}" -Wdeprecated -I "${GENSRC_DIR}/mtl-headers")
    endforeach()
endforeach()

file(GLOB native_metal_files "${GRAPHICS_SRC}/native-prism-mtl/msl/*.metal")
foreach(metal ${native_metal_files})
    add_msl_shader(native "${metal}")
endforeach()

set(MSL_LIB "${SHADER_OBJ_DIR}/com/sun/prism/mtl/msl/jfxshaders.metallib")
file(MAKE_DIRECTORY "${SHADER_OBJ_DIR}/com/sun/prism/mtl/msl")
add_custom_command(OUTPUT "${MSL_LIB}"
    COMMAND "${METAL_LINKER}" ${MSL_AIR_FILES} -o "${MSL_LIB}"
    DEPENDS ${MSL_AIR_FILES}
    VERBATIM)
add_custom_target(shaders ALL DEPENDS "${MSL_LIB}")
