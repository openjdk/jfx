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

# Windows-specific portion of the javafx.graphics native build. Replicates the
# compiler/linker flags previously defined by the retired Gradle native build
# (buildSrc/win.gradle). Included from CMakeLists.txt; shared input variables
# (JDK_HOME, GENSRC_DIR, HEADERS_DIR, BIN_DIR, INCLUDE_ES2, GRAPHICS_SRC) are
# defined there.

enable_language(RC)

# ---------------------------------------------------------------------------
# Windows-only inputs from Maven
# ---------------------------------------------------------------------------
set(SHADER_OBJ_DIR "" CACHE PATH "Output directory for fxc-compiled .obj shader resources")

set(JFX_VER "28" CACHE STRING "JavaFX release version")
set(JFX_FVER "28,0,0,0" CACHE STRING "JavaFX file version quad")
set(JFX_BUILD_ID "28-internal" CACHE STRING "JavaFX build identifier")
set(JFX_COMPANY "N/A" CACHE STRING "Company name embedded in version resources")
set(JFX_PRODUCT "OpenJFX" CACHE STRING "Product name embedded in version resources")

if(NOT SHADER_OBJ_DIR)
    message(FATAL_ERROR "SHADER_OBJ_DIR must be provided")
endif()

string(TIMESTAMP JFX_COPYRIGHT_YEAR "%Y")

# ---------------------------------------------------------------------------
# Locate fxc.exe in the Windows SDK (rc.exe is located by CMake itself)
# ---------------------------------------------------------------------------
get_filename_component(WINSDK_ROOT
    "[HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows Kits\\Installed Roots;KitsRoot10]"
    ABSOLUTE)
if(NOT WINSDK_ROOT OR WINSDK_ROOT MATCHES "registry")
    set(WINSDK_ROOT "C:/Program Files (x86)/Windows Kits/10")
endif()
file(GLOB WINSDK_BIN_DIRS "${WINSDK_ROOT}/bin/10.*/x64")
list(SORT WINSDK_BIN_DIRS)
list(REVERSE WINSDK_BIN_DIRS)
find_program(FXC_EXECUTABLE fxc HINTS ${WINSDK_BIN_DIRS} "${WINSDK_ROOT}/bin/x64")
if(NOT FXC_EXECUTABLE)
    message(FATAL_ERROR "Cannot find fxc.exe in the Windows SDK (looked under ${WINSDK_ROOT}/bin)")
endif()

# ---------------------------------------------------------------------------
# Global flags: exact parity with the retired Gradle Windows toolchain config,
# replacing CMake defaults
# ---------------------------------------------------------------------------
foreach(lang C CXX)
    set(CMAKE_${lang}_FLAGS "")
    set(CMAKE_${lang}_FLAGS_RELEASE "")
    set(CMAKE_${lang}_FLAGS_DEBUG "")
endforeach()
set(CMAKE_SHARED_LINKER_FLAGS "/nologo /manifest /opt:REF /incremental:no /dynamicbase /nxcompat")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "")
set(CMAKE_SHARED_LINKER_FLAGS_DEBUG "/debug")

# Common cc flags (the /MD | /MDd runtime flag comes from CMAKE_MSVC_RUNTIME_LIBRARY)
set(JFX_COMMON_COMPILE_OPTIONS
    /nologo /W3 /EHsc
    /D_DISABLE_CONSTEXPR_MUTEX_CONSTRUCTOR
    /DINLINE=__inline
    /DWIN32 /DIAL /D_LITTLE_ENDIAN /DWIN32_LEAN_AND_MEAN
    "$<$<CONFIG:Release>:/O2;/DNDEBUG>"
    "$<$<CONFIG:Debug>:/Od;/Zi;/DDEBUG;/FS>")

set(JFX_RC_COMMON_DEFINITIONS
    "JFX_COMPANY=${JFX_COMPANY}"
    "JFX_COMPONENT=${JFX_PRODUCT} Platform binary"
    "JFX_NAME=${JFX_PRODUCT} Platform ${JFX_VER}"
    "JFX_VER=${JFX_VER}"
    "JFX_BUILD_ID=${JFX_BUILD_ID}"
    "JFX_COPYRIGHT=Copyright © ${JFX_COPYRIGHT_YEAR}"
    "JFX_FVER=${JFX_FVER}"
    "JFX_FTYPE=0x2L")

# ---------------------------------------------------------------------------
# add_jfx_library(<name>
#     OUTPUT_NAME <dll base name>
#     SOURCE_DIRS <dirs...>          # globbed non-recursively, like Gradle listFiles()
#     EXTRA_SOURCES <files...>
#     COMPILE_OPTIONS <opts...>
#     NO_UNICODE                     # drop /DUNICODE /D_UNICODE (no target uses it now)
#     JNI                            # the JDK headers + HEADERS_DIR (javac -h) on the include path:
#                                    # only the targets that still contain JNI code
#     LINK_LIBS <libs...>
#     LINK_OPTIONS <opts...>
#     RC_SOURCE <file>               # defaults to version.rc
#     RC_INCLUDE_DIRS <dirs...>
# )
# ---------------------------------------------------------------------------
function(add_jfx_library name)
    cmake_parse_arguments(JFX "NO_UNICODE;JNI" "OUTPUT_NAME;RC_SOURCE"
        "SOURCE_DIRS;EXTRA_SOURCES;COMPILE_OPTIONS;LINK_LIBS;LINK_OPTIONS;RC_INCLUDE_DIRS" ${ARGN})

    set(sources)
    foreach(dir ${JFX_SOURCE_DIRS})
        file(GLOB dir_sources "${dir}/*.c" "${dir}/*.cc" "${dir}/*.cpp")
        list(APPEND sources ${dir_sources})
    endforeach()
    list(APPEND sources ${JFX_EXTRA_SOURCES})

    # Each target compiles its own copy of the version resource with its own defines
    if(NOT JFX_RC_SOURCE)
        set(JFX_RC_SOURCE "${GRAPHICS_SRC}/resources/version.rc")
    endif()
    get_filename_component(rc_name "${JFX_RC_SOURCE}" NAME)
    set(rc_copy "${CMAKE_CURRENT_BINARY_DIR}/rc/${name}/${rc_name}")
    configure_file("${JFX_RC_SOURCE}" "${rc_copy}" COPYONLY)
    get_filename_component(rc_src_dir "${JFX_RC_SOURCE}" DIRECTORY)
    set_source_files_properties("${rc_copy}" PROPERTIES
        COMPILE_DEFINITIONS "JFX_FNAME=${JFX_OUTPUT_NAME}.dll;JFX_INTERNAL_NAME=${name};${JFX_RC_COMMON_DEFINITIONS}"
        INCLUDE_DIRECTORIES "${rc_src_dir};${JFX_RC_INCLUDE_DIRS}")
    list(APPEND sources "${rc_copy}")

    add_library(${name} SHARED ${sources})
    set_target_properties(${name} PROPERTIES
        OUTPUT_NAME "${JFX_OUTPUT_NAME}"
        PREFIX ""
        RUNTIME_OUTPUT_DIRECTORY "${BIN_DIR}"
        RUNTIME_OUTPUT_DIRECTORY_RELEASE "${BIN_DIR}"
        RUNTIME_OUTPUT_DIRECTORY_DEBUG "${BIN_DIR}"
        PDB_OUTPUT_DIRECTORY "${BIN_DIR}"
        PDB_OUTPUT_DIRECTORY_RELEASE "${BIN_DIR}"
        PDB_OUTPUT_DIRECTORY_DEBUG "${BIN_DIR}")

    target_compile_options(${name} PRIVATE ${JFX_COMMON_COMPILE_OPTIONS})
    if(NOT JFX_NO_UNICODE)
        target_compile_options(${name} PRIVATE /DUNICODE /D_UNICODE)
    endif()
    target_compile_options(${name} PRIVATE ${JFX_COMPILE_OPTIONS})

    # JNI targets see jni.h / jni_md.h and the generated com_sun_*.h; the others compile with no
    # JDK header at all, which is what proves them JNI-free.
    if(JFX_JNI)
        target_include_directories(${name} PRIVATE
            "${JDK_HOME}/include" "${JDK_HOME}/include/win32"
            "${HEADERS_DIR}")
    endif()
    target_include_directories(${name} PRIVATE ${JFX_SOURCE_DIRS})

    target_link_libraries(${name} PRIVATE ${JFX_LINK_LIBS})
    target_link_options(${name} PRIVATE
        "/map:$<TARGET_FILE_DIR:${name}>/${JFX_OUTPUT_NAME}.map"
        ${JFX_LINK_OPTIONS})
endfunction()

# ---------------------------------------------------------------------------
# glass.dll
# ---------------------------------------------------------------------------
add_jfx_library(glass
    OUTPUT_NAME glass
    JNI
    SOURCE_DIRS "${GRAPHICS_SRC}/native-glass/win"
    RC_SOURCE "${GRAPHICS_SRC}/native-glass/win/GlassResources.rc"
    RC_INCLUDE_DIRS "${GRAPHICS_SRC}/resources"
    LINK_LIBS delayimp.lib gdi32.lib urlmon.lib Comdlg32.lib imm32.lib
        shell32.lib Uiautomationcore.lib dwmapi.lib version.lib
    LINK_OPTIONS /DELAYLOAD:user32.dll /DELAYLOAD:urlmon.dll
        /DELAYLOAD:shell32.dll /DELAYLOAD:Uiautomationcore.dll /DELAYLOAD:dwmapi.dll
        /DELAYLOAD:version.dll)

# ---------------------------------------------------------------------------
# prism_sw.dll
# ---------------------------------------------------------------------------
add_jfx_library(prismSW
    OUTPUT_NAME prism_sw
    SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-sw")

# ---------------------------------------------------------------------------
# prism_d3d.dll (needs fxc-generated shader headers)
# ---------------------------------------------------------------------------
set(D3D_HEADER_DIR "${CMAKE_CURRENT_BINARY_DIR}/headers/PrismD3D")
set(D3D_SRC "${GRAPHICS_SRC}/native-prism-d3d")
set(PS_3D_SRC "${D3D_SRC}/hlsl/Mtl1PS.hlsl")
set(VS_3D_SRC "${D3D_SRC}/hlsl/Mtl1VS.hlsl")
set(PASSTHROUGH_VS_SRC "${D3D_SRC}/PassThroughVS.hlsl")
set(D3D_GENERATED_HEADERS)

function(add_fxc_header out_file profile src)
    # Remaining arguments are fxc /D or /E options
    add_custom_command(OUTPUT "${out_file}"
        COMMAND "${FXC_EXECUTABLE}" /nologo /T ${profile} /Fh "${out_file}" ${ARGN} "${src}"
        DEPENDS "${src}"
        VERBATIM)
    set(D3D_GENERATED_HEADERS ${D3D_GENERATED_HEADERS} "${out_file}" PARENT_SCOPE)
endfunction()

add_fxc_header("${D3D_HEADER_DIR}/PassThroughVS.h" vs_3_0 "${PASSTHROUGH_VS_SRC}" /E passThrough)
add_fxc_header("${D3D_HEADER_DIR}/hlsl/Mtl1PS.h" ps_3_0 "${PS_3D_SRC}" /DSpec=0 /DSType=0)
add_fxc_header("${D3D_HEADER_DIR}/hlsl/Mtl1PS_i.h" ps_3_0 "${PS_3D_SRC}" /DSpec=0 /DSType=0 /DIllumMap=1)
# Pixel shader permutations: s = specular only, b = specular + bump map (/DBump=1);
# 1..3 = /DSpec level; n/t/c/m = /DSType 0..3; trailing i = /DIllumMap=1
foreach(bump_prefix s b)
    if(bump_prefix STREQUAL "b")
        set(bump_args /DBump=1)
    else()
        set(bump_args)
    endif()
    foreach(spec RANGE 1 3)
        set(stype_index 0)
        foreach(stype_letter n t c m)
            add_fxc_header("${D3D_HEADER_DIR}/hlsl/Mtl1PS_${bump_prefix}${spec}${stype_letter}.h"
                ps_3_0 "${PS_3D_SRC}" /DSpec=${spec} /DSType=${stype_index} ${bump_args})
            add_fxc_header("${D3D_HEADER_DIR}/hlsl/Mtl1PS_${bump_prefix}${spec}${stype_letter}i.h"
                ps_3_0 "${PS_3D_SRC}" /DSpec=${spec} /DSType=${stype_index} ${bump_args} /DIllumMap=1)
            math(EXPR stype_index "${stype_index} + 1")
        endforeach()
    endforeach()
endforeach()
add_fxc_header("${D3D_HEADER_DIR}/hlsl/Mtl1VS_Obj.h" vs_3_0 "${VS_3D_SRC}" /DVertexType=VsInput)

add_custom_target(generateD3DHeaders DEPENDS ${D3D_GENERATED_HEADERS})

add_jfx_library(prismD3D
    OUTPUT_NAME prism_d3d
    SOURCE_DIRS "${D3D_SRC}" "${D3D_SRC}/hlsl"
    COMPILE_OPTIONS "/I${D3D_HEADER_DIR}"
    LINK_LIBS user32.lib)
add_dependencies(prismD3D generateD3DHeaders)

# ---------------------------------------------------------------------------
# prism_es2.dll (optional, mirrors IS_INCLUDE_ES2)
# ---------------------------------------------------------------------------
if(INCLUDE_ES2)
    add_jfx_library(prismES2
        OUTPUT_NAME prism_es2
        SOURCE_DIRS "${GRAPHICS_SRC}/native-prism-es2"
            "${GRAPHICS_SRC}/native-prism-es2/GL"
            "${GRAPHICS_SRC}/native-prism-es2/windows"
        COMPILE_OPTIONS /Ob1 /GF /Gy /GS
        LINK_LIBS opengl32.lib gdi32.lib user32.lib kernel32.lib
        LINK_OPTIONS /SUBSYSTEM:WINDOWS)
endif()

# ---------------------------------------------------------------------------
# javafx_iio.dll
# ---------------------------------------------------------------------------
add_jfx_library(iio
    OUTPUT_NAME javafx_iio
    SOURCE_DIRS "${GRAPHICS_SRC}/native-iio" "${GRAPHICS_SRC}/native-iio/libjpeg")

# ---------------------------------------------------------------------------
# Pixel shader .obj resources bundled into javafx-graphics.jar
# ---------------------------------------------------------------------------
set(SHADER_OBJS)
foreach(entry "jsl-decora=com/sun/scenario/effect/impl/hw" "jsl-prism=com/sun/prism")
    string(REPLACE "=" ";" parts "${entry}")
    list(GET parts 0 gendir)
    list(GET parts 1 pkg)
    file(GLOB hlsl_files "${GENSRC_DIR}/${gendir}/${pkg}/d3d/hlsl/*.hlsl")
    foreach(hlsl ${hlsl_files})
        get_filename_component(base "${hlsl}" NAME_WE)
        set(obj "${SHADER_OBJ_DIR}/${pkg}/d3d/hlsl/${base}.obj")
        add_custom_command(OUTPUT "${obj}"
            COMMAND "${FXC_EXECUTABLE}" /nologo /T ps_3_0 /Fo "${obj}" "${hlsl}"
            DEPENDS "${hlsl}"
            VERBATIM)
        list(APPEND SHADER_OBJS "${obj}")
    endforeach()
endforeach()
add_custom_target(shaders ALL DEPENDS ${SHADER_OBJS})
