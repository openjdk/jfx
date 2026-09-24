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

# Linux-specific portion of the javafx.graphics native build. Replicates the
# compiler/linker flags previously defined by the retired Gradle native build
# (buildSrc/linux.gradle). Included from CMakeLists.txt; shared input variables
# (JDK_HOME, GENSRC_DIR, HEADERS_DIR, BIN_DIR, INCLUDE_ES2, GRAPHICS_SRC) are
# defined there.
#
# Notes on Gradle parity:
#   - Gradle compiled with gcc and linked everything with g++; forcing
#     LINKER_LANGUAGE CXX below matches that.
#   - Gradle stripped the .so files (strip -x) only when copying them into the
#     SDK image; the libraries are left unstripped here, same as the build/
#     output of the Gradle build.
#   - The static (IS_STATIC_BUILD) and i386/parfait toolchain variants of the
#     Gradle build were not ported.

# ---------------------------------------------------------------------------
# Required system packages (development headers)
# ---------------------------------------------------------------------------
find_package(PkgConfig REQUIRED)
# Glass Gtk is built with GTK+ 3. Requires GTK+ 3.20.0 or newer.
set(GTK3_MIN_MINOR_VERSION 20)
set(GTK3_MIN_MICRO_VERSION 0)
# xtst is deliberately absent: GlassRobot.cpp of commit 033187ad90 was the last C
# to call libXtst; com.sun.glass.ui.gtk.GtkGlassNative binds libXtst.so.6 at run time.
pkg_check_modules(GTK3 REQUIRED IMPORTED_TARGET
    "gtk+-3.0>=3.${GTK3_MIN_MINOR_VERSION}.${GTK3_MIN_MICRO_VERSION}"
    gthread-2.0 gio-unix-2.0)

# ---------------------------------------------------------------------------
# Global flags: exact parity with the retired Gradle Linux toolchain config,
# replacing CMake defaults
# ---------------------------------------------------------------------------
foreach(lang C CXX)
    set(CMAKE_${lang}_FLAGS "")
    set(CMAKE_${lang}_FLAGS_RELEASE "")
    set(CMAKE_${lang}_FLAGS_DEBUG "")
endforeach()
set(CMAKE_SHARED_LINKER_FLAGS "")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "")
set(CMAKE_SHARED_LINKER_FLAGS_DEBUG "")

# Common parameters used for both compiling and linking (Gradle commonFlags)
set(JFX_COMMON_FLAGS
    -fno-strict-aliasing -fPIC -fno-omit-frame-pointer # optimization flags
    -fstack-protector
    -Wextra -Wall -Wformat-security -Wno-unused -Wno-parentheses
    -Werror=trampolines) # warning flags

# Gradle cppFlags (shared C/C++ compile flags)
set(JFX_COMMON_COMPILE_OPTIONS
    ${JFX_COMMON_FLAGS}
    -ffunction-sections -fdata-sections
    "$<$<CONFIG:Debug>:-ggdb;-DVERBOSE>"
    "$<$<NOT:$<CONFIG:Debug>>:-O2;-DNDEBUG>")

# Gradle cFlags = cppFlags + this (only some targets used cFlags; C sources only)
set(JFX_C_STRICT_OPTIONS
    "$<$<COMPILE_LANGUAGE:C>:-Werror=implicit-function-declaration>")

# Gradle dynamicLinkFlags (CMake adds -shared itself)
set(JFX_COMMON_LINK_OPTIONS
    -static-libgcc -static-libstdc++
    ${JFX_COMMON_FLAGS}
    "LINKER:-z,relro"
    "LINKER:--gc-sections"
    "$<$<CONFIG:Debug>:-g>")

# ---------------------------------------------------------------------------
# add_jfx_library(<name>
#     OUTPUT_NAME <so base name>     # produces lib<OUTPUT_NAME>.so
#     SOURCE_DIRS <dirs...>          # globbed non-recursively, like Gradle listFiles()
#     EXTRA_SOURCES <files...>
#     EXCLUDE_REGEX <regex>          # dropped from the globbed sources
#     INCLUDE_DIRS <dirs...>
#     COMPILE_OPTIONS <opts...>
#     LINK_LIBS <libs...>
# )
# ---------------------------------------------------------------------------
function(add_jfx_library name)
    cmake_parse_arguments(JFX "" "OUTPUT_NAME;EXCLUDE_REGEX"
        "SOURCE_DIRS;EXTRA_SOURCES;INCLUDE_DIRS;COMPILE_OPTIONS;LINK_LIBS" ${ARGN})

    set(sources)
    foreach(dir ${JFX_SOURCE_DIRS})
        file(GLOB dir_sources "${dir}/*.c" "${dir}/*.cc" "${dir}/*.cpp")
        list(APPEND sources ${dir_sources})
    endforeach()
    if(JFX_EXCLUDE_REGEX)
        list(FILTER sources EXCLUDE REGEX "${JFX_EXCLUDE_REGEX}")
    endif()
    list(APPEND sources ${JFX_EXTRA_SOURCES})

    add_library(${name} SHARED ${sources})
    set_target_properties(${name} PROPERTIES
        OUTPUT_NAME "${JFX_OUTPUT_NAME}"
        LINKER_LANGUAGE CXX
        LIBRARY_OUTPUT_DIRECTORY "${BIN_DIR}"
        LIBRARY_OUTPUT_DIRECTORY_RELEASE "${BIN_DIR}"
        LIBRARY_OUTPUT_DIRECTORY_DEBUG "${BIN_DIR}")

    target_compile_options(${name} PRIVATE
        ${JFX_COMMON_COMPILE_OPTIONS} ${JFX_COMPILE_OPTIONS})
    target_include_directories(${name} PRIVATE
        ${JFX_SOURCE_DIRS} ${JFX_INCLUDE_DIRS})
    target_link_libraries(${name} PRIVATE ${JFX_LINK_LIBS})
    target_link_options(${name} PRIVATE ${JFX_COMMON_LINK_OPTIONS})
endfunction()

# ---------------------------------------------------------------------------
# libglassgtk3.so (the GTK glass sources). Linux builds no libglass.so: the
# launcher.c of commit 033187ad90 was the whole of it, and the library query it
# answered, GtkApplication._queryLibrary, is done by
# com.sun.glass.ui.gtk.GtkGlassNative in Java. GtkApplication still maps a
# library of that name when the deployment has one, which is how a glassgtk3
# build renamed to libglass.so is found.
#
# The only target of this file that still needs HEADERS_DIR and the JDK
# include directories: the GTK glass sources declare no JNI function and
# include no jni.h, but they do include the javac -h constant headers of
# com.sun.glass.events.* and com.sun.glass.ui.* (glass_key.cpp and
# glass_window.cpp alone read most of them), and every javac -h header starts
# with #include <jni.h>. prismSW, prismES2 and iio need neither, so both sets
# are named here instead of in add_jfx_library.
# ---------------------------------------------------------------------------
add_jfx_library(glassgtk3
    OUTPUT_NAME glassgtk3
    SOURCE_DIRS "${GRAPHICS_SRC}/native-glass/gtk"
    INCLUDE_DIRS "${GRAPHICS_SRC}/native-glass/gtk/libpipewire/include"
        "${HEADERS_DIR}" "${JDK_HOME}/include" "${JDK_HOME}/include/linux"
    COMPILE_OPTIONS -Werror -Wno-deprecated-declarations
        -DGTK_3_MIN_MINOR_VERSION=${GTK3_MIN_MINOR_VERSION}
        -DGTK_3_MIN_MICRO_VERSION=${GTK3_MIN_MICRO_VERSION}
    LINK_LIBS PkgConfig::GTK3)

# ---------------------------------------------------------------------------
# libprism_sw.so
# ---------------------------------------------------------------------------
add_jfx_library(prismSW
    OUTPUT_NAME prism_sw
    SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-sw"
    COMPILE_OPTIONS ${JFX_C_STRICT_OPTIONS} -DINLINE=inline)

# ---------------------------------------------------------------------------
# libprism_es2.so (optional, mirrors IS_INCLUDE_ES2; default true on Linux)
# ---------------------------------------------------------------------------
if(INCLUDE_ES2)
    add_jfx_library(prismES2
        OUTPUT_NAME prism_es2
        SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-es2"
            "${GRAPHICS_SRC}/native-prism-es2/GL"
            "${GRAPHICS_SRC}/native-prism-es2/x11"
        COMPILE_OPTIONS -DLINUX ${JFX_C_STRICT_OPTIONS}
        LINK_LIBS X11 GL)
endif()

# ---------------------------------------------------------------------------
# libprism_es2_monocle.so (optional): the generic prism_es2 sources compiled
# for Monocle (-DIS_EGLFB: EGL / OpenGL ES 2, no X11, no GLX) plus the stub
# lifecycle exports of native-prism-es2/monocle. Java owns the EGL display,
# surface and context (com.sun.glass.ui.monocle.AcceleratedScreen) and hands
# the current context over through es2_context_adopt, so the library links
# neither libEGL nor libX11. Gradle built this library only in the embedded
# armv6hf target this fork dropped; this is its first desktop build.
#
# INCLUDE_ES2_MONOCLE (declared in CMakeLists.txt): AUTO builds it when
# INCLUDE_ES2 is on and pkg-config finds glesv2 (CI's Linux runner has no
# libgles-dev and skips it silently; WSL/Ubuntu with libgles-dev builds it);
# ON insists and fails the configure step when glesv2 is missing; OFF never
# builds it.
#
# Linking -lGLESv2 replaces the JNI-era arrangement, in which the ~25 gl*
# functions the generic sources call directly (glEnable, glGetString,
# glTexImage2D, ...) were left undefined in the .so and bound lazily against
# the libGLESv2.so that AcceleratedScreen had dlopen'ed RTLD_GLOBAL earlier -
# a load-order dependence SymbolLookup.libraryLookup does not reproduce. With
# the soname in NEEDED the dynamic loader resolves them itself.
# ---------------------------------------------------------------------------
if(INCLUDE_ES2)
    pkg_check_modules(GLESV2 IMPORTED_TARGET glesv2)
endif()
string(TOUPPER "${INCLUDE_ES2_MONOCLE}" JFX_ES2_MONOCLE_MODE)
if(JFX_ES2_MONOCLE_MODE STREQUAL "AUTO")
    if(INCLUDE_ES2 AND GLESV2_FOUND)
        set(JFX_BUILD_ES2_MONOCLE ON)
    else()
        set(JFX_BUILD_ES2_MONOCLE OFF)
    endif()
elseif(INCLUDE_ES2_MONOCLE)
    if(NOT INCLUDE_ES2)
        message(FATAL_ERROR
            "INCLUDE_ES2_MONOCLE=${INCLUDE_ES2_MONOCLE} needs INCLUDE_ES2 (the generic prism_es2 sources)")
    endif()
    if(NOT GLESV2_FOUND)
        message(FATAL_ERROR
            "INCLUDE_ES2_MONOCLE=${INCLUDE_ES2_MONOCLE} but pkg-config found no glesv2 (install libgles-dev)")
    endif()
    set(JFX_BUILD_ES2_MONOCLE ON)
else()
    set(JFX_BUILD_ES2_MONOCLE OFF)
endif()
message(STATUS "prism_es2_monocle: ${JFX_BUILD_ES2_MONOCLE} "
    "(INCLUDE_ES2_MONOCLE=${INCLUDE_ES2_MONOCLE}, glesv2 found: ${GLESV2_FOUND})")
if(JFX_BUILD_ES2_MONOCLE)
    add_jfx_library(prismES2Monocle
        OUTPUT_NAME prism_es2_monocle
        SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-es2"
            "${GRAPHICS_SRC}/native-prism-es2/GL"
            "${GRAPHICS_SRC}/native-prism-es2/monocle"
        COMPILE_OPTIONS -DLINUX -DIS_EGLFB ${JFX_C_STRICT_OPTIONS}
        LINK_LIBS PkgConfig::GLESV2)
endif()

# ---------------------------------------------------------------------------
# libjavafx_iio.so
# ---------------------------------------------------------------------------
add_jfx_library(iio
    OUTPUT_NAME javafx_iio
    SOURCE_DIRS "${GRAPHICS_SRC}/native-iio" "${GRAPHICS_SRC}/native-iio/libjpeg"
    COMPILE_OPTIONS ${JFX_C_STRICT_OPTIONS} -fvisibility=hidden)
