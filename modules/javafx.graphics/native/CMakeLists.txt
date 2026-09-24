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

# Native build for the javafx.graphics platform libraries. This replicates the
# compiler/linker flags previously defined by the retired Gradle native build.
# Invoked from modules/javafx.graphics/pom.xml via the exec-maven-plugin.
# Platform-specific targets live in win.cmake, linux.cmake and mac.cmake.

cmake_minimum_required(VERSION 3.20)

cmake_policy(SET CMP0091 NEW) # MSVC runtime library via CMAKE_MSVC_RUNTIME_LIBRARY
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded$<$<CONFIG:Debug>:Debug>DLL")

# Minimum macOS version (Gradle: MACOSX_MIN_VERSION); ignored on other
# platforms. Must be set before project().
set(CMAKE_OSX_DEPLOYMENT_TARGET "11.0" CACHE STRING "Minimum macOS deployment version")

project(javafxGraphicsNatives LANGUAGES C CXX)

# ---------------------------------------------------------------------------
# Inputs from Maven (shared across platforms)
# ---------------------------------------------------------------------------
set(JDK_HOME "$ENV{JAVA_HOME}" CACHE PATH "JDK used for JNI headers")
set(GENSRC_DIR "" CACHE PATH "modules/javafx.graphics/target/gensrc")
set(HEADERS_DIR "" CACHE PATH "Directory containing javac -h generated JNI headers")
set(BIN_DIR "" CACHE PATH "Output directory for the built native libraries")
if(WIN32)
    option(INCLUDE_ES2 "Build the prism_es2 (OpenGL) library" OFF)
else()
    option(INCLUDE_ES2 "Build the prism_es2 (OpenGL) library" ON)
endif()
# Linux only (linux.cmake decides; ignored elsewhere): the Monocle (EGL /
# OpenGL ES 2) variant of prism_es2. AUTO = ON when INCLUDE_ES2 is on and
# pkg-config finds glesv2, else OFF.
set(INCLUDE_ES2_MONOCLE "AUTO" CACHE STRING
    "Build the prism_es2_monocle (Monocle EGL / OpenGL ES 2) library on Linux: AUTO, ON or OFF")
set_property(CACHE INCLUDE_ES2_MONOCLE PROPERTY STRINGS AUTO ON OFF)

if(NOT GENSRC_DIR OR NOT HEADERS_DIR OR NOT BIN_DIR)
    message(FATAL_ERROR "GENSRC_DIR, HEADERS_DIR and BIN_DIR must be provided")
endif()

set(GRAPHICS_SRC "${CMAKE_CURRENT_SOURCE_DIR}/../src/main")

if(WIN32)
    include("${CMAKE_CURRENT_SOURCE_DIR}/win.cmake")
elseif(APPLE)
    include("${CMAKE_CURRENT_SOURCE_DIR}/mac.cmake")
elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    include("${CMAKE_CURRENT_SOURCE_DIR}/linux.cmake")
else()
    message(FATAL_ERROR "This CMake build currently supports Windows, macOS and Linux only.")
endif()
