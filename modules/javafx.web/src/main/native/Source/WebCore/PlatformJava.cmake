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
    platform/network
    platform/network/java
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

set(JFXWebKit_SOURCES
    # bindings/java/dom3/JavaDOMSelection.cpp
    # bindings/java/dom3/JavaWheelEvent.cpp
    bindings/java/dom3/JavaAttr.cpp
    bindings/java/dom3/JavaCDATASection.cpp
    bindings/java/dom3/JavaCSSCharsetRule.cpp
    bindings/java/dom3/JavaCSSFontFaceRule.cpp
    bindings/java/dom3/JavaCSSImportRule.cpp
    bindings/java/dom3/JavaCSSMediaRule.cpp
    bindings/java/dom3/JavaCSSPageRule.cpp
    bindings/java/dom3/JavaCSSPrimitiveValue.cpp
    bindings/java/dom3/JavaCSSRule.cpp
    bindings/java/dom3/JavaCSSRuleList.cpp
    bindings/java/dom3/JavaCSSStyleDeclaration.cpp
    bindings/java/dom3/JavaCSSStyleRule.cpp
    bindings/java/dom3/JavaCSSStyleSheet.cpp
    bindings/java/dom3/JavaCSSUnknownRule.cpp
    bindings/java/dom3/JavaCSSValue.cpp
    bindings/java/dom3/JavaCSSValueList.cpp
    bindings/java/dom3/JavaCharacterData.cpp
    bindings/java/dom3/JavaComment.cpp
    bindings/java/dom3/JavaCounter.cpp
    bindings/java/dom3/JavaDOMImplementation.cpp
    bindings/java/dom3/JavaDOMStringList.cpp
    bindings/java/dom3/JavaDOMWindow.cpp
    bindings/java/dom3/JavaDocument.cpp
    bindings/java/dom3/JavaDocumentFragment.cpp
    bindings/java/dom3/JavaDocumentType.cpp
    bindings/java/dom3/JavaElement.cpp
    bindings/java/dom3/JavaEntity.cpp
    bindings/java/dom3/JavaEntityReference.cpp
    bindings/java/dom3/JavaEvent.cpp
    bindings/java/dom3/JavaEventTarget.cpp
    bindings/java/dom3/JavaHTMLAnchorElement.cpp
    bindings/java/dom3/JavaHTMLAppletElement.cpp
    bindings/java/dom3/JavaHTMLAreaElement.cpp
    bindings/java/dom3/JavaHTMLBRElement.cpp
    bindings/java/dom3/JavaHTMLBaseElement.cpp
    bindings/java/dom3/JavaHTMLBaseFontElement.cpp
    bindings/java/dom3/JavaHTMLBodyElement.cpp
    bindings/java/dom3/JavaHTMLButtonElement.cpp
    bindings/java/dom3/JavaHTMLCollection.cpp
    bindings/java/dom3/JavaHTMLDListElement.cpp
    bindings/java/dom3/JavaHTMLDirectoryElement.cpp
    bindings/java/dom3/JavaHTMLDivElement.cpp
    bindings/java/dom3/JavaHTMLDocument.cpp
    bindings/java/dom3/JavaHTMLElement.cpp
    bindings/java/dom3/JavaHTMLFieldSetElement.cpp
    bindings/java/dom3/JavaHTMLFontElement.cpp
    bindings/java/dom3/JavaHTMLFormElement.cpp
    bindings/java/dom3/JavaHTMLFrameElement.cpp
    bindings/java/dom3/JavaHTMLFrameSetElement.cpp
    bindings/java/dom3/JavaHTMLHRElement.cpp
    bindings/java/dom3/JavaHTMLHeadElement.cpp
    bindings/java/dom3/JavaHTMLHeadingElement.cpp
    bindings/java/dom3/JavaHTMLHtmlElement.cpp
    bindings/java/dom3/JavaHTMLIFrameElement.cpp
    bindings/java/dom3/JavaHTMLImageElement.cpp
    bindings/java/dom3/JavaHTMLInputElement.cpp
    bindings/java/dom3/JavaHTMLLIElement.cpp
    bindings/java/dom3/JavaHTMLLabelElement.cpp
    bindings/java/dom3/JavaHTMLLegendElement.cpp
    bindings/java/dom3/JavaHTMLLinkElement.cpp
    bindings/java/dom3/JavaHTMLMapElement.cpp
    bindings/java/dom3/JavaHTMLMenuElement.cpp
    bindings/java/dom3/JavaHTMLMetaElement.cpp
    bindings/java/dom3/JavaHTMLModElement.cpp
    bindings/java/dom3/JavaHTMLOListElement.cpp
    bindings/java/dom3/JavaHTMLObjectElement.cpp
    bindings/java/dom3/JavaHTMLOptGroupElement.cpp
    bindings/java/dom3/JavaHTMLOptionElement.cpp
    bindings/java/dom3/JavaHTMLOptionsCollection.cpp
    bindings/java/dom3/JavaHTMLParagraphElement.cpp
    bindings/java/dom3/JavaHTMLParamElement.cpp
    bindings/java/dom3/JavaHTMLPreElement.cpp
    bindings/java/dom3/JavaHTMLQuoteElement.cpp
    bindings/java/dom3/JavaHTMLScriptElement.cpp
    bindings/java/dom3/JavaHTMLSelectElement.cpp
    bindings/java/dom3/JavaHTMLStyleElement.cpp
    bindings/java/dom3/JavaHTMLTableCaptionElement.cpp
    bindings/java/dom3/JavaHTMLTableCellElement.cpp
    bindings/java/dom3/JavaHTMLTableColElement.cpp
    bindings/java/dom3/JavaHTMLTableElement.cpp
    bindings/java/dom3/JavaHTMLTableRowElement.cpp
    bindings/java/dom3/JavaHTMLTableSectionElement.cpp
    bindings/java/dom3/JavaHTMLTextAreaElement.cpp
    bindings/java/dom3/JavaHTMLTitleElement.cpp
    bindings/java/dom3/JavaHTMLUListElement.cpp
    bindings/java/dom3/JavaKeyboardEvent.cpp
    bindings/java/dom3/JavaMediaList.cpp
    bindings/java/dom3/JavaMouseEvent.cpp
    bindings/java/dom3/JavaMutationEvent.cpp
    bindings/java/dom3/JavaNamedNodeMap.cpp
    bindings/java/dom3/JavaNode.cpp
    bindings/java/dom3/JavaNodeFilter.cpp
    bindings/java/dom3/JavaNodeIterator.cpp
    bindings/java/dom3/JavaNodeList.cpp
    bindings/java/dom3/JavaProcessingInstruction.cpp
    bindings/java/dom3/JavaRGBColor.cpp
    bindings/java/dom3/JavaRange.cpp
    bindings/java/dom3/JavaRect.cpp
    bindings/java/dom3/JavaStyleSheet.cpp
    bindings/java/dom3/JavaStyleSheetList.cpp
    bindings/java/dom3/JavaText.cpp
    bindings/java/dom3/JavaTreeWalker.cpp
    bindings/java/dom3/JavaUIEvent.cpp
    bindings/java/dom3/JavaXPathExpression.cpp
    bindings/java/dom3/JavaXPathNSResolver.cpp
    bindings/java/dom3/JavaXPathResult.cpp
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
