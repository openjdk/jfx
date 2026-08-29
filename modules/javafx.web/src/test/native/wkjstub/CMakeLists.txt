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

# Builds wkjstub, the recording stub implementation of the wkj_* C ABI that
# makes the javafx.web FFM binding layer executable in this repository (this
# repository cannot build WebKit; see modules/javafx.web/FFM-ABI-CONTRACT.md
# section 8). Test-only: it is never shipped and never linked into jfxwebkit.
#
# Driven from modules/javafx.web/pom.xml. Required cache entries:
#   JFX_ROOT  repository root
#   BIN_DIR   where the shared library is written
# Optional:
#   WKJ_API_HEADER  core ABI header; defaults to the real one when it exists,
#                   otherwise to the seed header in this directory
#   WKJ_DOM_SPEC    DOM ABI spec; defaults to buildtools/ffm-web/dom-abi.tsv

cmake_minimum_required(VERSION 3.20)
project(wkjstub C)

if(NOT DEFINED JFX_ROOT)
    message(FATAL_ERROR "wkjstub: JFX_ROOT is required")
endif()
if(NOT DEFINED BIN_DIR)
    message(FATAL_ERROR "wkjstub: BIN_DIR is required")
endif()

find_package(Perl REQUIRED)

set(WKJ_GEN_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated")
file(MAKE_DIRECTORY "${WKJ_GEN_DIR}")

# The core ABI header: the real one when the WebKit tree has it, the seed
# otherwise. The seed carries no DOM declarations - those never come from a
# hand-written header, only from the spec below.
set(WKJ_REAL_HEADER
    "${JFX_ROOT}/modules/javafx.web/src/main/native/Source/WebKitLegacy/java/api/webkit_java_api.h")
if(NOT DEFINED WKJ_API_HEADER OR WKJ_API_HEADER STREQUAL "")
    if(EXISTS "${WKJ_REAL_HEADER}")
        set(WKJ_API_HEADER "${WKJ_REAL_HEADER}")
    else()
        set(WKJ_API_HEADER "${CMAKE_CURRENT_SOURCE_DIR}/seed/webkit_java_api.h")
        message(WARNING
            "wkjstub: ${WKJ_REAL_HEADER} does not exist yet; building against the seed core "
            "header ${WKJ_API_HEADER}. The stub then covers the DOM ABI in full but only a "
            "placeholder core ABI, so the core binding tests are provisional.")
    endif()
endif()
get_filename_component(WKJ_API_HEADER_DIR "${WKJ_API_HEADER}" DIRECTORY)

# The generator reads the whole ABI, not just the core header: the area headers are
# either included by it (_platform, _theme, _wtf, _pal, _events) or include it
# (_page, _bridge). Listing only the core header here is how the stub silently went
# stale against a WKJHost that had grown from 13 groups to 15 -- CMake saw no reason
# to re-run. Glob so a header added later is picked up without editing this file.
file(GLOB WKJ_API_HEADERS "${WKJ_API_HEADER_DIR}/webkit_java_api*.h")

if(NOT DEFINED WKJ_DOM_SPEC OR WKJ_DOM_SPEC STREQUAL "")
    set(WKJ_DOM_SPEC "${JFX_ROOT}/buildtools/ffm-web/dom-abi.tsv")
endif()
if(NOT EXISTS "${WKJ_DOM_SPEC}")
    message(FATAL_ERROR "wkjstub: DOM ABI spec not found: ${WKJ_DOM_SPEC}")
endif()

set(WKJ_SPEC_TO_HEADER "${JFX_ROOT}/buildtools/ffm-web/spec-to-header.pl")
set(WKJ_GEN_STUB "${CMAKE_CURRENT_SOURCE_DIR}/gen-wkjstub.pl")

# 1. The DOM half of the ABI header, from the spec.
add_custom_command(
    OUTPUT "${WKJ_GEN_DIR}/webkit_java_api_dom.h"
    COMMAND "${PERL_EXECUTABLE}" "${WKJ_SPEC_TO_HEADER}"
            --spec "${WKJ_DOM_SPEC}"
            --header "${WKJ_GEN_DIR}/webkit_java_api_dom.h"
    DEPENDS "${WKJ_SPEC_TO_HEADER}" "${WKJ_DOM_SPEC}"
    COMMENT "wkjstub: generating webkit_java_api_dom.h from the DOM ABI spec"
    VERBATIM)

# 2. The stub bodies and the symbol, struct and host-slot tables.
add_custom_command(
    OUTPUT "${WKJ_GEN_DIR}/wkjstub_generated.c"
    COMMAND "${PERL_EXECUTABLE}" "${WKJ_GEN_STUB}"
            --header "${WKJ_API_HEADER}"
            --spec "${WKJ_DOM_SPEC}"
            --out "${WKJ_GEN_DIR}/wkjstub_generated.c"
    DEPENDS "${WKJ_GEN_STUB}" ${WKJ_API_HEADERS} "${WKJ_DOM_SPEC}"
            "${WKJ_GEN_DIR}/webkit_java_api_dom.h"
    COMMENT "wkjstub: generating the recording stubs from the ABI"
    VERBATIM)

add_library(wkjstub SHARED
    "${CMAKE_CURRENT_SOURCE_DIR}/wkjstub_runtime.c"
    "${WKJ_GEN_DIR}/wkjstub_generated.c")

# The generated .c is compiled from WKJ_GEN_DIR, so a quoted include finds the
# generated DOM header there first and the core header through this list.
target_include_directories(wkjstub PRIVATE
    "${WKJ_GEN_DIR}"
    "${CMAKE_CURRENT_SOURCE_DIR}"
    "${WKJ_API_HEADER_DIR}")

set_target_properties(wkjstub PROPERTIES
    C_STANDARD 99
    OUTPUT_NAME wkjstub
    RUNTIME_OUTPUT_DIRECTORY "${BIN_DIR}"
    LIBRARY_OUTPUT_DIRECTORY "${BIN_DIR}")
foreach(config Debug Release RelWithDebInfo MinSizeRel)
    string(TOUPPER "${config}" configUpper)
    set_target_properties(wkjstub PROPERTIES
        RUNTIME_OUTPUT_DIRECTORY_${configUpper} "${BIN_DIR}"
        LIBRARY_OUTPUT_DIRECTORY_${configUpper} "${BIN_DIR}")
endforeach()

if(MSVC)
    target_compile_definitions(wkjstub PRIVATE _CRT_SECURE_NO_WARNINGS)
    target_compile_options(wkjstub PRIVATE /W3 /WX)
else()
    target_compile_options(wkjstub PRIVATE
        -Wall -Wextra -Werror -fvisibility=hidden)
    if(NOT APPLE)
        target_link_libraries(wkjstub PRIVATE pthread)
    endif()
endif()
