include(platform/TextureMapper.cmake)

list(APPEND WebCore_INCLUDE_DIRECTORIES
    "${WEBCORE_DIR}/ForwardingHeaders"
    "${CMAKE_BINARY_DIR}/../include/private"
    "${CMAKE_BINARY_DIR}/../include/private/JavaScriptCore"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/ForwardingHeaders"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/API"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/assembler"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/builtins"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/bytecode"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/bytecompiler"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/dfg"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/disassembler"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/domjit"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/heap"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/debugger"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/interpreter"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/jit"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/llint"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/parser"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/profiler"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/runtime"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore/yarr"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/WTF"
    "${WEBCORE_DIR}/platform/java"
    "${WEBCORE_DIR}/platform/graphics/java"
    "${WEBCORE_DIR}/platform/linux"
    "${WEBCORE_DIR}/platform/network"
    "${WEBCORE_DIR}/platform/network/java"
    "${WEBCORE_DIR}/bindings/java"
    "${WEBCORE_DIR}/page/java"
    "${WEBCORE_DIR}/bridge/jni"
    "${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore"
    "${WTF_DIR}"
    "${WEBKIT_DIR}"
    "${THIRDPARTY_DIR}/sqlite"
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

list(REMOVE_ITEM WebCore_SOURCES
    platform/graphics/WOFFFileFormat.cpp
    platform/network/SocketStreamHandleImpl.cpp
    platform/LocalizedStrings.cpp
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

list(APPEND WebCore_SOURCES
    bridge/jni/JNIUtility.cpp
    bridge/jni/JobjectWrapper.cpp
    bridge/jni/jsc/BridgeUtils.cpp
    bridge/jni/jsc/JavaArrayJSC.cpp
    bridge/jni/jsc/JavaClassJSC.cpp
    bridge/jni/jsc/JavaFieldJSC.cpp
    bridge/jni/jsc/JavaInstanceJSC.cpp
    bridge/jni/jsc/JavaMethodJSC.cpp
    bridge/jni/jsc/JavaRuntimeObject.cpp
    bridge/jni/jsc/JNIUtilityPrivate.cpp
    editing/java/EditorJava.cpp
    platform/java/ColorChooserJava.cpp
    platform/java/ContextMenuClientJava.cpp
    platform/java/ContextMenuJava.cpp
    platform/java/CursorJava.cpp
    platform/java/DragClientJava.cpp
    platform/java/DragImageJava.cpp
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
    platform/java/PluginDataJava.cpp
    platform/java/PluginInfoStoreJava.cpp
    platform/java/PluginViewJava.cpp
    platform/java/PluginWidgetJava.cpp
    platform/java/PopupMenuJava.cpp
    platform/java/RenderThemeJava.cpp
    platform/java/ScrollbarThemeJava.cpp
    platform/java/SharedBufferJava.cpp
    platform/java/MainThreadSharedTimerJava.cpp
    platform/java/StringJava.cpp
    platform/java/TemporaryLinkStubsJava.cpp
    platform/java/TouchEventJava.cpp
    platform/java/WebPage.cpp
    platform/java/WheelEventJava.cpp
    platform/java/WidgetJava.cpp
    platform/java/api/PageCacheJava.cpp
    platform/graphics/java/BitmapImageJava.cpp
    platform/graphics/java/BufferImageJava.cpp
    platform/graphics/java/ChromiumBridge.cpp
    platform/graphics/java/ComplexTextControllerJava.cpp
    platform/graphics/java/FontCacheJava.cpp
    platform/graphics/java/FontCustomPlatformData.cpp
    platform/graphics/java/FontCascadeJava.cpp
    platform/graphics/java/FontJava.cpp
    platform/graphics/java/FontPlatformDataJava.cpp
    platform/graphics/java/GlyphPageTreeNodeJava.cpp
    platform/graphics/java/GraphicsContextJava.cpp
    platform/graphics/java/IconJava.cpp
    platform/graphics/java/ImageBufferJava.cpp
    platform/graphics/java/ImageJava.cpp
    platform/graphics/java/ImageDecoderJava.cpp  # FIXME: Add only if IMAGEIO?
    platform/graphics/java/MediaPlayerPrivateJava.cpp
    platform/graphics/java/NativeImageJava.cpp
    platform/graphics/java/PathJava.cpp
    platform/graphics/java/RenderingQueue.cpp
    platform/graphics/java/RQRef.cpp

    platform/network/java/SocketStreamHandleImplJava.cpp
    platform/network/java/SynchronousLoaderClientJava.cpp
    platform/network/java/URLLoader.cpp
    platform/network/NetworkStorageSessionStub.cpp

    platform/text/LocaleNone.cpp
    platform/text/Hyphenation.cpp

    platform/network/java/CookieJarJava.cpp
    platform/network/java/DNSJava.cpp
    platform/network/java/ResourceHandleJava.cpp
    platform/network/java/ResourceRequestJava.cpp

    bindings/java/JavaDOMUtils.cpp
    bindings/java/JavaEventListener.cpp

    platform/java/ChromeClientJava.cpp
    platform/java/WebKitLogging.cpp
    platform/java/BackForwardList.cpp
    page/java/DragControllerJava.cpp
    page/java/EventHandlerJava.cpp

    ${WEBKIT_LEGACY_FILES}
)

if (NOT WIN32)
    set_source_files_properties(${WEBKIT_LEGACY_FILES} PROPERTIES COMPILE_FLAGS "-include config.h")
endif()

if (WIN32)
    list(APPEND WebCore_SOURCES
        platform/win/SystemInfo.cpp
    )
elseif (APPLE)
    list(APPEND WebCore_INCLUDE_DIRECTORIES
        ${WEBCORE_DIR}/platform/mac
    )
    list(APPEND WebCore_SOURCES
        editing/SmartReplaceCF.cpp
        platform/cf/SharedBufferCF.cpp
        platform/cf/URLCF.cpp
        platform/cf/CFURLExtras.cpp
    )
    find_library(ACCELERATE_LIBRARY accelerate)
    list(APPEND WebCore_LIBRARIES
        ${ACCELERATE_LIBRARY}
    )
endif ()

list(APPEND WebCore_LIBRARIES
    XMLJava
    XSLTJava
)

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

# To make use of files present in WebKit/WebCoreSupport
set(WebCore_FORWARDING_HEADERS_FILES
    loader/appcache/ApplicationCacheStorage.h
    loader/FrameLoaderTypes.h
    loader/LoaderStrategy.h
    loader/FrameLoaderTypes.h
    loader/LoaderStrategy.h
    loader/ResourceLoadPriority.h
    loader/ResourceLoaderOptions.h
    platform/Timer.h
)

set(WebCore_USER_AGENT_SCRIPTS_DEPENDENCIES ${WEBCORE_DIR}/platform/java/RenderThemeJava.cpp)

list(APPEND WebCore_LIBRARIES
    SqliteJava
    ${ICU_I18N_LIBRARIES}
    ${ICU_LIBRARIES}
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

set(JavaDOM3Bindings_SOURCES
      bindings/java/dom3/JavaCounter.cpp
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
      bindings/java/dom3/JavaMediaList.cpp
      bindings/java/dom3/JavaRect.cpp
      bindings/java/dom3/JavaRGBColor.cpp
      bindings/java/dom3/JavaStyleSheet.cpp
      bindings/java/dom3/JavaStyleSheetList.cpp
      bindings/java/dom3/JavaAttr.cpp
      bindings/java/dom3/JavaCDATASection.cpp
      bindings/java/dom3/JavaCharacterData.cpp
      bindings/java/dom3/JavaComment.cpp
      bindings/java/dom3/JavaDocumentFragment.cpp
      bindings/java/dom3/JavaDocument.cpp
      bindings/java/dom3/JavaDocumentType.cpp
      bindings/java/dom3/JavaDOMImplementation.cpp
      bindings/java/dom3/JavaDOMStringList.cpp
      bindings/java/dom3/JavaElement.cpp
      bindings/java/dom3/JavaEntity.cpp
      bindings/java/dom3/JavaEntityReference.cpp
      bindings/java/dom3/JavaEvent.cpp
      bindings/java/dom3/JavaEventTarget.cpp
      bindings/java/dom3/JavaKeyboardEvent.cpp
      bindings/java/dom3/JavaMouseEvent.cpp
      bindings/java/dom3/JavaMutationEvent.cpp
      bindings/java/dom3/JavaNamedNodeMap.cpp
      bindings/java/dom3/JavaNode.cpp
      bindings/java/dom3/JavaNodeFilter.cpp
      bindings/java/dom3/JavaNodeIterator.cpp
      bindings/java/dom3/JavaNodeList.cpp
      bindings/java/dom3/JavaProcessingInstruction.cpp
      bindings/java/dom3/JavaRange.cpp
      bindings/java/dom3/JavaText.cpp
      bindings/java/dom3/JavaTreeWalker.cpp
      bindings/java/dom3/JavaUIEvent.cpp
      # bindings/java/dom3/JavaWheelEvent.cpp
      bindings/java/dom3/JavaHTMLAnchorElement.cpp
      bindings/java/dom3/JavaHTMLAppletElement.cpp
      bindings/java/dom3/JavaHTMLAreaElement.cpp
      bindings/java/dom3/JavaHTMLBaseElement.cpp
      bindings/java/dom3/JavaHTMLBaseFontElement.cpp
      bindings/java/dom3/JavaHTMLBodyElement.cpp
      bindings/java/dom3/JavaHTMLBRElement.cpp
      bindings/java/dom3/JavaHTMLButtonElement.cpp
      bindings/java/dom3/JavaHTMLCollection.cpp
      bindings/java/dom3/JavaHTMLDirectoryElement.cpp
      bindings/java/dom3/JavaHTMLDivElement.cpp
      bindings/java/dom3/JavaHTMLDListElement.cpp
      bindings/java/dom3/JavaHTMLDocument.cpp
      bindings/java/dom3/JavaHTMLElement.cpp
      bindings/java/dom3/JavaHTMLFieldSetElement.cpp
      bindings/java/dom3/JavaHTMLFontElement.cpp
      bindings/java/dom3/JavaHTMLFormElement.cpp
      bindings/java/dom3/JavaHTMLFrameElement.cpp
      bindings/java/dom3/JavaHTMLFrameSetElement.cpp
      bindings/java/dom3/JavaHTMLHeadElement.cpp
      bindings/java/dom3/JavaHTMLHeadingElement.cpp
      bindings/java/dom3/JavaHTMLHRElement.cpp
      bindings/java/dom3/JavaHTMLHtmlElement.cpp
      bindings/java/dom3/JavaHTMLIFrameElement.cpp
      bindings/java/dom3/JavaHTMLImageElement.cpp
      bindings/java/dom3/JavaHTMLInputElement.cpp
      bindings/java/dom3/JavaHTMLLabelElement.cpp
      bindings/java/dom3/JavaHTMLLegendElement.cpp
      bindings/java/dom3/JavaHTMLLIElement.cpp
      bindings/java/dom3/JavaHTMLLinkElement.cpp
      bindings/java/dom3/JavaHTMLMapElement.cpp
      bindings/java/dom3/JavaHTMLMenuElement.cpp
      bindings/java/dom3/JavaHTMLMetaElement.cpp
      bindings/java/dom3/JavaHTMLModElement.cpp
      bindings/java/dom3/JavaHTMLObjectElement.cpp
      bindings/java/dom3/JavaHTMLOListElement.cpp
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
      bindings/java/dom3/JavaDOMWindow.cpp
      # bindings/java/dom3/JavaDOMSelection.cpp
      bindings/java/dom3/JavaXPathExpression.cpp
      bindings/java/dom3/JavaXPathNSResolver.cpp
      bindings/java/dom3/JavaXPathResult.cpp
)

list(APPEND WebCore_SOURCES ${JavaDOM3Bindings_SOURCES})

set(WebCore_FORWARDING_HEADERS_DIRECTORIES
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

    bridge/jni
    bridge/jni/jsc

    platform/mediastream/libwebrtc

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

configure_file(platform/java/WebPageConfig.h.in ${CMAKE_BINARY_DIR}/WebPageConfig.h)
