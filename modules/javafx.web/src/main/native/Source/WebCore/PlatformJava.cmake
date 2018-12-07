set(WebCore_OUTPUT_NAME WebCore)

include(platform/TextureMapper.cmake)

set(WebCore_OUTPUT_NAME WebCore)

# JDK-9 +
set(JAVA_JNI_GENSRC_PATH "${CMAKE_BINARY_DIR}/../../gensrc/headers/javafx.web")
if (NOT EXISTS ${JAVA_JNI_GENSRC_PATH})
    # JDK-8
    set(JAVA_JNI_GENSRC_PATH "${CMAKE_BINARY_DIR}/../../generated-src/headers")
endif ()

list(APPEND WebCore_INCLUDE_DIRECTORIES
    "${WEBCORE_DIR}/platform/java"
    "${WEBCORE_DIR}/platform/graphics/java"
    "${WEBCORE_DIR}/platform/linux"
    "${WEBCORE_DIR}/platform/network"
    "${WEBCORE_DIR}/platform/network/java"
    "${WEBCORE_DIR}/bindings/java"
    "${WEBCORE_DIR}/page/java"
    "${WEBCORE_DIR}/bridge/jni"
    "${WEBKIT_DIR}"
    # JNI headers
    "${JAVA_JNI_GENSRC_PATH}"
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

if (WIN32)
    list(APPEND WebCore_SOURCES
        platform/win/SystemInfo.cpp
    )
elseif (APPLE)
    list(APPEND WebCore_PRIVATE_INCLUDE_DIRECTORIES
        ${WEBCORE_DIR}/platform/mac
    )
    list(APPEND WebCore_SOURCES
        editing/SmartReplaceCF.cpp
        platform/cf/SharedBufferCF.cpp
        platform/cf/URLCF.cpp
        platform/cf/CFURLExtras.cpp
    )
# find_library(OPENGL_LIBRARY OpenGL)
    find_library(ACCELERATE_LIBRARY Accelerate)
    list(APPEND WebCore_LIBRARIES
        ${ACCELERATE_LIBRARY}
        # ${OPENGL_LIBRARY}
    )
endif ()

#FIXME: Workaround
list(APPEND WebCoreTestSupport_LIBRARIES ${SQLITE_LIBRARIES})

list(APPEND WebCore_USER_AGENT_STYLE_SHEETS
    ${WEBCORE_DIR}/css/mediaControlsGtk.css
)

set(WebCore_USER_AGENT_SCRIPTS
    ${WEBCORE_DIR}/English.lproj/mediaControlsLocalizedStrings.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsBase.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsGtk.js
)

add_definitions(-DMAX_DOM_TREE_DEPTH=2000)

add_custom_command(
    OUTPUT ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitVersion.h
    MAIN_DEPENDENCY ${WEBKITLEGACY_DIR}/scripts/generate-webkitversion.pl
    DEPENDS ${WEBKITLEGACY_DIR}/mac/Configurations/Version.xcconfig
    COMMAND ${PERL_EXECUTABLE} ${WEBKITLEGACY_DIR}/scripts/generate-webkitversion.pl --config ${WEBKITLEGACY_DIR}/mac/Configurations/Version.xcconfig --outputDir ${DERIVED_SOURCES_WEBCORE_DIR}
    VERBATIM)
list(APPEND WebCore_SOURCES ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitVersion.h)

# To make use of files present in WebKit/WebCoreSupport
set(WebCore_FORWARDING_HEADERS_FILES
    loader/appcache/ApplicationCacheStorage.h
    loader/FrameLoaderTypes.h
    loader/LoaderStrategy.h
    loader/FrameLoaderTypes.h
    loader/LoaderStrategy.h
    loader/ResourceLoaderOptions.h

    platform/CookiesStrategy.h
    platform/FileSystem.h
    platform/Logging.h
    platform/LogInitialization.h
    platform/Timer.h
    platform/ThreadCheck.h
    platform/PlatformExportMacros.h
    platform/PlatformStrategies.h
    platform/SharedStringHash.h
    platform/SuddenTermination.h
    platform/URL.h

    platform/network/BlobRegistryImpl.h
    platform/network/NetworkStorageSession.h
    platform/network/PlatformCookieJar.h
    platform/network/PingHandle.h
    platform/network/ResourceLoadPriority.h
    platform/network/java/ResourceRequest.h

    svg/SVGTests.h
)

set(WebCore_USER_AGENT_SCRIPTS_DEPENDENCIES ${WEBCORE_DIR}/platform/java/RenderThemeJava.cpp)

add_definitions(-DIMAGEIO=1)

list(APPEND WebCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

set(WebCore_FORWARDING_HEADERS_DIRECTORIES
    bridge
    bridge/jni
    bridge/jni/jsc

    bindings/js

    css
    dom
    editing
    html
    history
    loader
    page
    rendering
    platform/mediastream/libwebrtc
    platform/graphics
    platform/sql
    platform/text
    storage

    xml
    inspector
)


WEBKIT_CREATE_FORWARDING_HEADERS(WebCore DIRECTORIES ${WebCore_FORWARDING_HEADERS_DIRECTORIES} FILES ${WebCore_FORWARDING_HEADERS_FILES})

configure_file(platform/java/WebPageConfig.h.in ${CMAKE_BINARY_DIR}/WebPageConfig.h)

list(APPEND WebCore_UNIFIED_SOURCE_LIST_FILES
    "SourcesPlatformJava.txt"
)

list(APPEND JFXWebKit_UNIFIED_SOURCE_LIST_FILES
    "SourcesJava.txt"
)

set(JFXWebKit_SOURCES
    platform/java/ColorChooserJava.cpp
    platform/java/ContextMenuClientJava.cpp
    platform/java/ContextMenuJava.cpp
    platform/java/DragClientJava.cpp
    platform/java/EditorClientJava.cpp
    platform/java/FrameLoaderClientJava.cpp
    platform/java/ProgressTrackerClientJava.cpp
    platform/java/VisitedLinkStoreJava.cpp
    platform/java/InspectorClientJava.cpp
    platform/java/WebPage.cpp
    platform/java/PlatformStrategiesJava.cpp
    platform/java/ChromeClientJava.cpp
    platform/java/BackForwardList.cpp
    platform/java/api/PageCacheJava.cpp
)

set(JFXWebKit_LIBRARIES
    PRIVATE JavaScriptCore${DEBUG_SUFFIX}
    PRIVATE WebCore${DEBUG_SUFFIX}
    PRIVATE PAL${DEBUG_SUFFIX}
    PRIVATE WebCoreTestSupport${DEBUG_SUFFIX}
)

set(WEBKIT_LEGACY_FILES
    # FIXME-java: Move WebKit interface specific files into WebKit dir
    ../WebKitLegacy/Storage/StorageAreaImpl.cpp
    ../WebKitLegacy/Storage/StorageAreaSync.cpp
    ../WebKitLegacy/Storage/StorageNamespaceImpl.cpp
    ../WebKitLegacy/Storage/StorageSyncManager.cpp
    ../WebKitLegacy/Storage/StorageThread.cpp
    ../WebKitLegacy/Storage/StorageTracker.cpp
    ../WebKitLegacy/Storage/WebDatabaseProvider.cpp
    ../WebKitLegacy/Storage/WebStorageNamespaceProvider.cpp
    ../WebKitLegacy/WebCoreSupport/WebResourceLoadScheduler.cpp
)

list(APPEND JFXWebKit_SOURCES ${WEBKIT_LEGACY_FILES})

set(JFXWebKit_OUTPUT_NAME "jfxwebkit")
set(JFXWebKit_LIBRARY_TYPE SHARED)

if (MSVC)
    set_source_files_properties(${WEBKIT_LEGACY_FILES} PROPERTIES COMPILE_FLAGS "/FI config.h")
else ()
    set_source_files_properties(${WEBKIT_LEGACY_FILES} PROPERTIES COMPILE_FLAGS "-include config.h")
endif()


WEBKIT_COMPUTE_SOURCES(JFXWebKit)
WEBKIT_WRAP_SOURCELIST(${JFXWebKit_SOURCES})
WEBKIT_FRAMEWORK_DECLARE(JFXWebKit)

if (MSVC)
    WEBKIT_ADD_PRECOMPILED_HEADER("WebKitPrefix.h" "platform/java/WebKitPrefix.cpp" JFXWebKit_SOURCES)
endif ()

if (APPLE)
    set_target_properties(JFXWebKit PROPERTIES LINK_FLAGS "-exported_symbols_list ${WEBCORE_DIR}/mapfile-macosx")
    set(JFXWebKit_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-macosx")
elseif (UNIX)
    set_target_properties(JFXWebKit PROPERTIES LINK_FLAGS "-Xlinker -version-script=${WEBCORE_DIR}/mapfile-vers -Wl,--no-undefined")
    set(JFXWebKit_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-vers")
elseif (WIN32)
    # Adds version information to jfxwebkit.dll created by Gradle build, see JDK-8166265
    set_target_properties(JFXWebKit PROPERTIES LINK_FLAGS "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
    set(JFXWebKit_EXTERNAL_DEP "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
endif ()

# Create a dummy depency c file to relink when mapfile changes
get_filename_component(STAMP_NAME ${JFXWebKit_EXTERNAL_DEP} NAME)
set(JFXWebKit_EXTERNAL_DEP_STAMP "${CMAKE_BINARY_DIR}/${STAMP_NAME}.stamp.cpp")
add_custom_command(
    OUTPUT "${JFXWebKit_EXTERNAL_DEP_STAMP}"
    DEPENDS "${JFXWebKit_EXTERNAL_DEP}"
    COMMAND ${CMAKE_COMMAND} -E touch "${JFXWebKit_EXTERNAL_DEP_STAMP}"
    VERBATIM
)
list(APPEND JFXWebKit_SOURCES ${JFXWebKit_EXTERNAL_DEP_STAMP})

WEBKIT_FRAMEWORK(JFXWebKit)
