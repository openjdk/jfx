include(platform/TextureMapper.cmake)

set(WebCore_OUTPUT_NAME WebCore)

# JDK-9 +
set(JAVA_JNI_GENSRC_PATH "${CMAKE_BINARY_DIR}/../gensrc/headers/javafx.web")
if (NOT EXISTS ${JAVA_JNI_GENSRC_PATH})
    # JDK-8
    set(JAVA_JNI_GENSRC_PATH "${CMAKE_BINARY_DIR}/../generated-src/headers")
endif ()

list(REMOVE_ITEM  WebCore_PRIVATE_FRAMEWORK_HEADERS
    bridge/objc/WebScriptObject.h
    bridge/objc/WebScriptObjectPrivate.h
)

list(APPEND WebCore_INCLUDE_DIRECTORIES
    "${WEBCORE_DIR}/platform/java"
    "${WEBCORE_DIR}/platform/graphics/java"
    "${WEBCORE_DIR}/platform/linux"
    "${WEBCORE_DIR}/platform/network"
    "${WEBCORE_DIR}/platform/network/java"
    "${WEBCORE_DIR}/bindings/java"
    "${WEBCORE_DIR}/page/java"
    "${WEBCORE_DIR}/bridge/jni"
    "${WEBKITLEGACY_DIR}"
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
    list(APPEND WebCore_PRIVATE_FRAMEWORK_HEADERS
        platform/win/SystemInfo.h
    )
elseif (APPLE)
    list(APPEND WebCore_PRIVATE_INCLUDE_DIRECTORIES
        ${WEBCORE_DIR}/platform/mac
    )
    list(APPEND WebCore_SOURCES
        editing/SmartReplaceCF.cpp
        platform/cf/SharedBufferCF.cpp
    )
    find_library(ACCELERATE_LIBRARY Accelerate)
    list(APPEND WebCore_LIBRARIES
        ${ACCELERATE_LIBRARY}
    )
endif ()

#FIXME: Workaround
list(APPEND WebCoreTestSupport_LIBRARIES ${SQLite3_LIBRARIES})


list(APPEND WebCore_USER_AGENT_STYLE_SHEETS
    ${WEBCORE_DIR}/css/themeAdwaita.css
    ${WebCore_DERIVED_SOURCES_DIR}/ModernMediaControls.css
)


set(WebCore_USER_AGENT_SCRIPTS
    ${WebCore_DERIVED_SOURCES_DIR}/ModernMediaControls.js
)

add_definitions(-DMAX_DOM_TREE_DEPTH=2000)

set(WebCore_USER_AGENT_SCRIPTS_DEPENDENCIES ${WEBCORE_DIR}/platform/java/RenderThemeJava.cpp)

add_definitions(-DIMAGEIO=1)

list(APPEND WebCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)
if (USE_SYSTEM_MALLOC)
    message(STATUS "Using system malloc")
    add_definitions(-DUSE_SYSTEM_MALLOC)
endif ()

list(APPEND WebCore_PRIVATE_FRAMEWORK_HEADERS
    bindings/java/JavaDOMUtils.h
    bindings/java/JavaEventListener.h
    bindings/java/EventListenerManager.h
    bindings/java/JavaNodeFilterCondition.h
    bridge/jni/jsc/BridgeUtils.h
    dom/DOMStringList.h
    platform/graphics/java/ImageBufferJavaBackend.h
    platform/graphics/java/ImageJava.h
    platform/graphics/java/PlatformContextJava.h
    platform/graphics/java/PathJava.h
    platform/graphics/java/RQRef.h
    platform/graphics/java/RenderingQueue.h
    platform/graphics/texmap/BitmapTextureJava.h
    platform/graphics/texmap/TextureMapperJava.h
    platform/java/DataObjectJava.h
    platform/java/PageSupplementJava.h
    platform/java/PlatformJavaClasses.h
    platform/java/PluginWidgetJava.h
    platform/mock/GeolocationClientMock.h
    platform/network/java/AuthenticationChallenge.h
    platform/network/java/CertificateInfo.h
    platform/network/java/ResourceError.h
    platform/network/java/ResourceRequest.h
    platform/network/java/ResourceResponse.h
    testing/js/WebCoreTestSupport.h
)

list(APPEND WebCore_UNIFIED_SOURCE_LIST_FILES
    "SourcesJava.txt"
)
