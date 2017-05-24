include(platform/TextureMapper.cmake)

set(WebCore_LIBRARY_TYPE SHARED)

list(APPEND WebCore_INCLUDE_DIRECTORIES
    "${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}"
    "${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}/inspector"
    "${JAVASCRIPTCORE_DIR}"
    "${JAVASCRIPTCORE_DIR}/ForwardingHeaders"
    "${JAVASCRIPTCORE_DIR}/API"
    "${JAVASCRIPTCORE_DIR}/assembler"
    "${JAVASCRIPTCORE_DIR}/bytecode"
    "${JAVASCRIPTCORE_DIR}/bytecompiler"
    "${JAVASCRIPTCORE_DIR}/dfg"
    "${JAVASCRIPTCORE_DIR}/disassembler"
    "${JAVASCRIPTCORE_DIR}/heap"
    "${JAVASCRIPTCORE_DIR}/debugger"
    "${JAVASCRIPTCORE_DIR}/interpreter"
    "${JAVASCRIPTCORE_DIR}/jit"
    "${JAVASCRIPTCORE_DIR}/llint"
    "${JAVASCRIPTCORE_DIR}/parser"
    "${JAVASCRIPTCORE_DIR}/profiler"
    "${JAVASCRIPTCORE_DIR}/runtime"
    "${JAVASCRIPTCORE_DIR}/yarr"
    "${WEBCORE_DIR}/accessibility"
    "${WEBCORE_DIR}/platform/java"
    "${WEBCORE_DIR}/platform/graphics/java"
    "${WEBCORE_DIR}/platform/graphics/freetype"
    "${WEBCORE_DIR}/platform/graphics/opengl"
    "${WEBCORE_DIR}/platform/graphics/opentype"
    "${WEBCORE_DIR}/platform/linux"
    "${WEBCORE_DIR}/platform/mediastream/openwebrtc"
    "${WEBCORE_DIR}/platform/network"
    "${WEBCORE_DIR}/platform/network/java"
    "${WEBCORE_DIR}/bindings/java"
    "${WEBCORE_DIR}/page/java"
    "${WEBCORE_DIR}/platform/graphics"
    "${WEBCORE_DIR}/bridge"
    "${WEBCORE_DIR}/bridge/c"
    "${WEBCORE_DIR}/bridge/jni"
    "${WEBCORE_DIR}/bridge/jni/jsc"
    "${WEBCORE_DIR}/bridge/jsc"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore"
    "${CMAKE_BINARY_DIR}/../../gensrc/headers/javafx.web"
    "${CMAKE_BINARY_DIR}/WebCore/generated"
    "${WTF_DIR}"
    "${WEBKIT_DIR}"
    "${THIRDPARTY_DIR}/sqlite"
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

list(APPEND WebCore_SOURCES
    bridge/jni/JNIUtility.cpp
    bridge/jni/JobjectWrapper.cpp
    bridge/jni/jsc/JavaArrayJSC.cpp
    bridge/jni/jsc/JavaClassJSC.cpp
    bridge/jni/jsc/JavaFieldJSC.cpp
    bridge/jni/jsc/JavaInstanceJSC.cpp
    bridge/jni/jsc/JavaMethodJSC.cpp
    bridge/jni/jsc/JavaRuntimeObject.cpp
    bridge/jni/jsc/JNIUtilityPrivate.cpp
    editing/java/EditorJava.cpp
    platform/java/BridgeUtils.cpp
    platform/java/ColorChooserJava.cpp
    platform/java/ContextMenuClientJava.cpp
    platform/java/ContextMenuJava.cpp
    platform/java/CursorJava.cpp
    platform/java/DragClientJava.cpp
    platform/java/DragDataJava.cpp
    platform/java/EditorClientJava.cpp
    platform/java/EventLoopJava.cpp
    platform/java/FileChooserJava.cpp
    platform/java/FileSystemJava.cpp
    platform/java/FrameLoaderClientJava.cpp
    platform/java/ProgressTrackerClientJava.cpp
    platform/java/VisitedLinkStoreJava.cpp
    platform/java/IDNJava.cpp
    platform/java/InspectorClientJava.cpp
    # platform/java/JavaEnv.cpp
    platform/java/KeyboardEventJava.cpp
    platform/java/KeyedCodingJava.cpp
    platform/java/LanguageJava.cpp
    platform/java/LocalizedStringsJava.cpp
    platform/java/LoggingJava.cpp
    platform/java/MIMETypeRegistryJava.cpp
    platform/java/MouseEventJava.cpp
    platform/java/PasteboardJava.cpp
    platform/java/PasteboardUtilitiesJava.cpp
    platform/java/PlatformScreenJava.cpp
    platform/java/PlatformStrategiesJava.cpp
    platform/KillRingNone.cpp
    platform/java/PluginDataJava.cpp
    platform/java/PluginInfoStoreJava.cpp
    platform/java/PluginViewJava.cpp
    platform/java/PluginWidgetJava.cpp
    platform/java/PopupMenuJava.cpp
    platform/java/RenderThemeJava.cpp
    platform/java/ScrollbarThemeJava.cpp
    platform/java/SharedBufferJava.cpp
    platform/java/MainThreadSharedTimerJava.cpp
    platform/java/SoundJava.cpp
    platform/java/StringJava.cpp
    platform/java/TemporaryLinkStubsJava.cpp
    platform/java/TextBreakIteratorInternalICUJava.cpp #ICU_UNICODE=1 //XXX: make switch for ICU_UNICODE
    # platform/java/TextBreakIteratorJava.cpp #ICU_UNICODE=0
    # platform/java/TextCodecJava.cpp #ICU_UNICODE=0
    # platform/java/TextNormalizerJava.cpp #ICU_UNICODE=0
    platform/java/TouchEventJava.cpp
    platform/java/WebPage.cpp
    platform/java/WheelEventJava.cpp
    platform/java/WidgetJava.cpp
    platform/java/api/BackForwardListJava.cpp
    platform/java/api/PageCacheJava.cpp
    platform/graphics/java/BitmapImageJava.cpp
    platform/graphics/java/BufferImageJava.cpp
    # platform/graphics/java/BufferImageSkiaJava.cpp
    platform/graphics/java/ChromiumBridge.cpp
    platform/graphics/java/FontCacheJava.cpp
    platform/graphics/java/FontCustomPlatformData.cpp
    platform/graphics/java/FontDataJava.cpp
    platform/graphics/java/FontJava.cpp
    platform/graphics/java/FontPlatformDataJava.cpp
    platform/graphics/java/GlyphPageTreeNodeJava.cpp
    platform/graphics/java/GraphicsContextJava.cpp
    platform/graphics/java/IconJava.cpp
    platform/graphics/java/ImageBufferJava.cpp
    platform/graphics/java/ImageJava.cpp
    platform/graphics/java/ImageSourceJava.cpp  #//XXX: contains(DEFINES, IMAGEIO=1) {
    platform/graphics/java/MediaPlayerPrivateJava.cpp
    html/shadow/MediaControlsApple.cpp
    platform/graphics/java/PathJava.cpp
    # platform/graphics/java/PlatformContextSkiaJava.cpp
    platform/graphics/java/RenderingQueue.cpp
    platform/graphics/java/RQRef.cpp

    platform/network/java/SocketStreamHandleJava.cpp
    platform/network/java/SynchronousLoaderClientJava.cpp
    platform/network/java/URLLoader.cpp
    platform/network/NetworkStorageSessionStub.cpp

    # FIXME-java:
    # platform/text/LocaleICU.cpp
    platform/text/LocaleNone.cpp
    platform/text/Hyphenation.cpp

    platform/network/java/CookieJarJava.cpp
    platform/network/java/DNSJava.cpp
    platform/network/java/ResourceHandleJava.cpp
    platform/network/java/ResourceRequestJava.cpp

    bindings/java/JavaDOMUtils.cpp
    bindings/java/JavaEventListener.cpp

    page/java/ChromeClientJava.cpp
    page/java/DragControllerJava.cpp
    page/java/EventHandlerJava.cpp

    # FIXME-java: Move WebKit interface specific files into WebKit dir
    ../WebKit/Storage/StorageAreaImpl.cpp
    ../WebKit/Storage/StorageAreaSync.cpp
    ../WebKit/Storage/StorageNamespaceImpl.cpp
    ../WebKit/Storage/StorageSyncManager.cpp
    ../WebKit/Storage/StorageThread.cpp
    ../WebKit/Storage/StorageTracker.cpp
    ../WebKit/Storage/WebDatabaseProvider.cpp
    ../WebKit/Storage/WebStorageNamespaceProvider.cpp
    ../WebKit/WebCoreSupport/WebResourceLoadScheduler.cpp
)

if (WIN32)
    list(APPEND WebCore_SOURCES
      platform/win/SystemInfo.cpp
    )
elseif(APPLE)
    list(APPEND WebCore_INCLUDE_DIRECTORIES
        ${WEBCORE_DIR}/icu
        ${WEBCORE_DIR}/platform/mac
    )
    list(APPEND WebCore_SOURCES
        editing/SmartReplaceCF.cpp
        # platform/cf/FileSystemCF.cpp
        platform/VNodeTracker.cpp
        platform/cf/SharedBufferCF.cpp
        platform/cf/URLCF.cpp
        platform/cf/CFURLExtras.cpp
    )
    find_library(ACCELERATE_LIBRARY accelerate)
    list(APPEND WebCore_LIBRARIES
        ${ACCELERATE_LIBRARY}
    )
elseif(UNIX)
    list(APPEND WebCore_SOURCES
      platform/linux/MemoryPressureHandlerLinux.cpp
    )
endif()

list(APPEND WebCore_USER_AGENT_STYLE_SHEETS
    ${WEBCORE_DIR}/css/mediaControlsGtk.css
)

set(WebCore_USER_AGENT_SCRIPTS
    ${WEBCORE_DIR}/English.lproj/mediaControlsLocalizedStrings.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsBase.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsGtk.js
)

add_custom_command(
    OUTPUT ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitVersion.h
    MAIN_DEPENDENCY ${WEBKIT_DIR}/scripts/generate-webkitversion.pl
    DEPENDS ${WEBKIT_DIR}/mac/Configurations/Version.xcconfig
    COMMAND ${PERL_EXECUTABLE} ${WEBKIT_DIR}/scripts/generate-webkitversion.pl --config ${WEBKIT_DIR}/mac/Configurations/Version.xcconfig --outputDir ${DERIVED_SOURCES_WEBCORE_DIR}
    VERBATIM)
list(APPEND WebCore_SOURCES ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitVersion.h)

set(WebCore_FORWARDING_HEADERS_FILES
    loader/appcache/ApplicationCacheStorage.h
)

set(WebCore_USER_AGENT_SCRIPTS_DEPENDENCIES ${WEBCORE_DIR}/platform/java/RenderThemeJava.cpp)
list(APPEND WebCore_LIBRARIES
    ${LIBXML2_LIBRARIES}
    ${LIBXSLT_LIBRARIES}
    SqliteJava
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${LIBXML2_INCLUDE_DIR}
    ${LIBXSLT_INCLUDE_DIR}
    # ${SQLITE_INCLUDE_DIR}
    # ${WEBP_INCLUDE_DIRS}
    ${ZLIB_INCLUDE_DIRS}
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

include_directories(
    "${WebCore_INCLUDE_DIRECTORIES}"
    "${DERIVED_SOURCES_DIR}"
)

include_directories(SYSTEM
    ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}
)

add_definitions(-DIMAGEIO=1)

list(APPEND WebCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

list(APPEND WebCore_Java_IDL_FILES
    css/Counter.idl
    css/CSSCharsetRule.idl
    css/CSSFontFaceRule.idl
    css/CSSImportRule.idl
    css/CSSMediaRule.idl
    css/CSSPageRule.idl
    css/CSSPrimitiveValue.idl
    css/CSSRule.idl
    css/CSSRuleList.idl
    css/CSSStyleDeclaration.idl
    css/CSSStyleRule.idl
    css/CSSStyleSheet.idl
    css/CSSUnknownRule.idl
    css/CSSValue.idl
    css/CSSValueList.idl
    css/MediaList.idl
    css/Rect.idl
    css/RGBColor.idl
    css/StyleSheet.idl
    css/StyleSheetList.idl
    dom/Attr.idl
    dom/CDATASection.idl
    dom/CharacterData.idl
    dom/Comment.idl
    dom/DocumentFragment.idl
    dom/Document.idl
    dom/DocumentType.idl
    dom/DOMImplementation.idl
    dom/DOMStringList.idl
    dom/Element.idl
    dom/Entity.idl
    dom/EntityReference.idl
    dom/Event.idl
    dom/EventTarget.idl
    dom/KeyboardEvent.idl
    dom/MouseEvent.idl
    dom/MutationEvent.idl
    dom/NamedNodeMap.idl
    dom/Node.idl
    dom/NodeFilter.idl
    dom/NodeIterator.idl
    dom/NodeList.idl
    # dom/Notation.idl
    dom/ProcessingInstruction.idl
    dom/Range.idl
    dom/Text.idl
    dom/TreeWalker.idl
    dom/UIEvent.idl
    dom/WheelEvent.idl
    html/HTMLAnchorElement.idl
    html/HTMLAppletElement.idl
    html/HTMLAreaElement.idl
    html/HTMLBaseElement.idl
    html/HTMLBaseFontElement.idl
    html/HTMLBodyElement.idl
    html/HTMLBRElement.idl
    html/HTMLButtonElement.idl
    html/HTMLCollection.idl
    html/HTMLDirectoryElement.idl
    html/HTMLDivElement.idl
    html/HTMLDListElement.idl
    html/HTMLDocument.idl
    html/HTMLElement.idl
    html/HTMLFieldSetElement.idl
    html/HTMLFontElement.idl
    html/HTMLFormElement.idl
    html/HTMLFrameElement.idl
    html/HTMLFrameSetElement.idl
    html/HTMLHeadElement.idl
    html/HTMLHeadingElement.idl
    html/HTMLHRElement.idl
    html/HTMLHtmlElement.idl
    html/HTMLIFrameElement.idl
    html/HTMLImageElement.idl
    html/HTMLInputElement.idl
    html/HTMLLabelElement.idl
    html/HTMLLegendElement.idl
    html/HTMLLIElement.idl
    html/HTMLLinkElement.idl
    html/HTMLMapElement.idl
    html/HTMLMenuElement.idl
    html/HTMLMetaElement.idl
    html/HTMLModElement.idl
    html/HTMLObjectElement.idl
    html/HTMLOListElement.idl
    html/HTMLOptGroupElement.idl
    html/HTMLOptionElement.idl
    html/HTMLOptionsCollection.idl
    html/HTMLParagraphElement.idl
    html/HTMLParamElement.idl
    html/HTMLPreElement.idl
    html/HTMLQuoteElement.idl
    html/HTMLScriptElement.idl
    html/HTMLSelectElement.idl
    html/HTMLStyleElement.idl
    html/HTMLTableCaptionElement.idl
    html/HTMLTableCellElement.idl
    html/HTMLTableColElement.idl
    html/HTMLTableElement.idl
    html/HTMLTableRowElement.idl
    html/HTMLTableSectionElement.idl
    html/HTMLTextAreaElement.idl
    html/HTMLTitleElement.idl
    html/HTMLUListElement.idl
    page/DOMWindow.idl
    page/DOMSelection.idl
    xml/XPathExpression.idl
    xml/XPathNSResolver.idl
    xml/XPathResult.idl
)

set(FEATURE_DEFINES_JAVA "LANGUAGE_JAVA=1 ${FEATURE_DEFINES_WITH_SPACE_SEPARATOR}")
string(REPLACE "ENABLE_INDEXED_DATABASE=1" "" FEATURE_DEFINES_JAVA ${FEATURE_DEFINES_JAVA})
string(REPLACE REGEX "ENABLE_SVG[A-Z_]+=1" "" FEATURE_DEFINES_JAVA ${FEATURE_DEFINES_JAVA})

set(ADDITIONAL_BINDINGS_DEPENDENCIES
    ${WINDOW_CONSTRUCTORS_FILE}
    ${WORKERGLOBALSCOPE_CONSTRUCTORS_FILE}
    ${DEDICATEDWORKERGLOBALSCOPE_CONSTRUCTORS_FILE}
)

foreach (_idl ${WebCore_Java_IDL_FILES})
    set(IDL_FILES_LIST "${IDL_FILES_LIST}${WEBCORE_DIR}/${_idl}\n")
endforeach ()


GENERATE_BINDINGS(JavaDOMBindings_SOURCES
    "${WebCore_Java_IDL_FILES}"
    "${WEBCORE_DIR}"
    "${IDL_INCLUDES}"
    "${FEATURE_DEFINES_JAVA}"
    ${DERIVED_SOURCES_WEBCORE_DIR}/nativeJava
    Java Java cpp
    ${IDL_ATTRIBUTES_FILE}
    ${SUPPLEMENTAL_DEPENDENCY_FILE}
    ${ADDITIONAL_BINDINGS_DEPENDENCIES})

list(APPEND WebCore_SOURCES ${JavaDOMBindings_SOURCES})

set (WebCore_FORWARDING_HEADERS_DIRECTORIES
    .
    accessibility
    bindings
    bridge
    contentextensions
    css
    dom
    editing
    history
    html
    inspector
    loader
    page
    platform
    plugins
    rendering
    storage
    style
    svg
    websockets
    workers
    xml

    Modules/geolocation
    Modules/indexeddb
    Modules/indexeddb/legacy
    Modules/indexeddb/shared
    Modules/notifications
    Modules/webdatabase

    accessibility/java

    bindings/generic
    bindings/js

    bridge/c
    bridge/jsc

    html/forms
    html/parser
    html/shadow
    html/track

    loader/appcache
    loader/archive
    loader/cache
    loader/icon

    page/animation
    page/csp
    page/scrolling
    page/java

    platform/animation
    platform/audio
    platform/graphics
    platform/network
    platform/network/java
    platform/sql
    platform/text
    platform/java

    platform/text/transcoder

    rendering/line
    rendering/shapes
    rendering/style
    rendering/svg

    svg/animation
    svg/graphics
    svg/properties

    svg/graphics/filters)

WEBKIT_CREATE_FORWARDING_HEADERS(WebCore DIRECTORIES ${WebCore_FORWARDING_HEADERS_DIRECTORIES})

if (WIN32)
    file(MAKE_DIRECTORY ${DERIVED_SOURCES_DIR}/ForwardingHeaders/WebCore)

    set(WebCore_PRE_BUILD_COMMAND "${CMAKE_BINARY_DIR}/DerivedSources/WebCore/preBuild.cmd")
    file(WRITE "${WebCore_PRE_BUILD_COMMAND}" "@xcopy /y /s /d /f \"${WEBCORE_DIR}/ForwardingHeaders/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/WebCore\" >nul 2>nul\n")
    foreach (_directory ${WebCore_FORWARDING_HEADERS_DIRECTORIES})
        file(APPEND "${WebCore_PRE_BUILD_COMMAND}" "@xcopy /y /d /f \"${WEBCORE_DIR}/${_directory}/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/WebCore\" >nul 2>nul\n")
    endforeach ()

    set(WebCore_POST_BUILD_COMMAND "${CMAKE_BINARY_DIR}/DerivedSources/WebCore/postBuild.cmd")
    file(WRITE "${WebCore_POST_BUILD_COMMAND}" "@xcopy /y /s /d /f \"${DERIVED_SOURCES_WEBCORE_DIR}/*.h\" \"${DERIVED_SOURCES_DIR}/ForwardingHeaders/WebCore\" >nul 2>nul\n")
endif ()
