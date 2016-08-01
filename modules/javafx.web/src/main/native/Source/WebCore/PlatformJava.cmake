# set(WebCore_OUTPUT_NAME WebCoreJava) #//XXX remove?
set(WebCore_LIBRARY_TYPE SHARED)

add_definitions(-DUSE_PROGRESS_ELEMENT=1)

list(APPEND WebCore_INCLUDE_DIRECTORIES
    ${WEBCORE_DIR}/accessibility/atk
    ${WEBCORE_DIR}/accessibility
    ${WEBCORE_DIR}/platform/java
    ${WEBCORE_DIR}/platform/graphics/java
    ${WEBCORE_DIR}/platform/graphics/freetype
    ${WEBCORE_DIR}/platform/graphics/opengl
    ${WEBCORE_DIR}/platform/graphics/opentype
    ${WEBCORE_DIR}/platform/linux
    ${WEBCORE_DIR}/platform/mediastream/openwebrtc
    ${WEBCORE_DIR}/platform/network
    ${WEBCORE_DIR}/platform/network/java
    ${WEBCORE_DIR}/bindings/java
    ${WEBCORE_DIR}/page/java
    ${WEBCORE_DIR}/platform/graphics
    ${WEBCORE_DIR}/bridge
    ${WEBCORE_DIR}/bridge/c
    ${WEBCORE_DIR}/bridge/jni
    ${WEBCORE_DIR}/bridge/jni/jsc
    ${WEBCORE_DIR}/bridge/jsc
    ${DERIVED_SOURCES_DIR}/ForwardingHeaders/JavaScriptCore
    ${CMAKE_BINARY_DIR}/../../generated-src/headers
    ${CMAKE_BINARY_DIR}/WebCore/generated
    ${WTF_DIR}/wtf/text
    ${WTF_DIR}/wtf/java
    ${WTF_DIR}/wtf/unicode/java
    ${WEBKIT_DIR}
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

# message(STATUS "==== #### WebCore_SYSTEM_INCLUDE_DIRECTORIES ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}")
# message(STATUS "==== #### WebCore_SOURCES  ${WebCore_SOURCES}")

list(APPEND WebCore_HEADERS
    accessibility
    bindings
    bindings/generic
    bindings/java
    bindings/js
    bridge
    bridge/c
    bridge/jni
    bridge/jni/jsc
    bridge/jsc
    css
    cssjit
    crypto/keys
    crypto
    dom
    dom/default
    editing
    fileapi
    history
    html
    html/canvas
    html/parser
    html/shadow
    html/track
    html/forms
    inspector
    loader
    loader/appcache
    loader/archive
    loader/archive/mhtml
    loader/cache
    loader/icon
    mathml
    page
    page/animation
    page/java
    page/scrolling
    platform
    platform/animation
    platform/audio
    platform/java
    platform/graphics
    platform/graphics/filters
    platform/graphics/filters/arm
    platform/graphics/java
    platform/graphics/texmap
    platform/graphics/transforms
    platform/image-decoders
    platform/leveldb
    platform/mock
    platform/network
    platform/network/java
    platform/sql
    platform/text
    platform/text/icu
    plugins
    rendering
    rendering/mathml
    rendering/style
    rendering/svg
    rendering/line
    rendering/shapes
    storage
    style
    svg
    svg/animation
    svg/graphics
    svg/graphics/filters
    svg/properties
    testing
    workers
    xml
    xml/parser
    # Modules/battery
    # Modules/gamepad
    # Modules/geolocation
    # Modules/indexeddb
    # Modules/mediastream
    # Modules/mediasource
    # Modules/networkinfo
    # Modules/notifications
    # Modules/quota
    # Modules/speech
    # Modules/vibration
    # Modules/webdatabase
    # Modules/websockets
    # Modules/plugins
#     bindings/java
#     page/java
#     platform/java
#     platform/graphics
#     platform/graphics/java
#     platform/network
#     platform/network/java
)

# include_directories(${WebCore_HEADERS})

# list(APPEND WebCorePlatformJava_SOURCES
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
    platform/java/ContextMenuItemJava.cpp
    platform/java/ContextMenuJava.cpp
    platform/java/CursorJava.cpp
    platform/java/DragClientJava.cpp
    platform/java/DragDataJava.cpp
    platform/java/EditorClientJava.cpp
    platform/java/EventLoopJava.cpp
    platform/java/FileChooserJava.cpp #//XXX: was off
    platform/java/FileSystemJava.cpp
    platform/java/FrameLoaderClientJava.cpp
    platform/java/VisitedLinkStoreJava.cpp
    platform/java/IDNJava.cpp
    platform/java/InspectorClientJava.cpp
    # platform/java/JavaEnv.cpp
    platform/java/KeyboardEventJava.cpp
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
    platform/java/SharedTimerJava.cpp
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

    # FIXME(arunprasadr):
    # platform/text/LocaleICU.cpp
    platform/text/LocaleNone.cpp

    platform/network/java/CookieJarJava.cpp
    platform/network/java/DNSJava.cpp
    platform/network/java/ResourceHandleJava.cpp
    platform/network/java/ResourceRequestJava.cpp

    bindings/java/JavaDOMUtils.cpp
    bindings/java/JavaEventListener.cpp

    page/java/ChromeClientJava.cpp
    page/java/DragControllerJava.cpp
    page/java/EventHandlerJava.cpp

    # FIXME(arunprasadr): Move WebKit interface specific files into WebKit dir
    ../WebKit/Storage/StorageAreaImpl.cpp
    ../WebKit/Storage/StorageAreaSync.cpp
    ../WebKit/Storage/StorageNamespaceImpl.cpp
    ../WebKit/Storage/StorageSyncManager.cpp
    ../WebKit/Storage/StorageThread.cpp
    ../WebKit/Storage/StorageTracker.cpp
    ../WebKit/Storage/WebDatabaseProvider.cpp
    ../WebKit/Storage/WebStorageNamespaceProvider.cpp
)

set(WebCore_FORWARDING_HEADERS_DIRECTORIES
    accessibility
    bindings
    bridge
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
    svg
    websockets
    workers
    xml

    Modules/geolocation
    Modules/indexeddb
    Modules/notifications
    Modules/webdatabase

    accessibility/win

    bindings/generic
    bindings/js

    bridge/c
    bridge/jsc

    history/cf

    html/forms
    html/parser

    loader/appcache
    loader/archive
    loader/cache
    loader/icon

    loader/archive/cf

    page/animation
    page/win

    platform/animation
    platform/cf
    platform/graphics
    platform/mock
    platform/network
    platform/sql
    platform/text
    platform/win

    platform/cf/win

    platform/graphics/opentype
    platform/graphics/transforms
    platform/graphics/win

    platform/text/transcoder

    rendering/style
    rendering/svg

    svg/animation
    svg/graphics
    svg/properties

    svg/graphics/filters
)

WEBKIT_CREATE_FORWARDING_HEADERS(WebCore DIRECTORIES ${WebCore_FORWARDING_HEADERS_DIRECTORIES})

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
#   platform/cf/FileSystemCF.cpp
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

# if (USE_GEOCLUE2)
#     list(APPEND WebCore_SOURCES
#         ${DERIVED_SOURCES_WEBCORE_DIR}/Geoclue2Interface.c
#     )
#     execute_process(COMMAND pkg-config --variable dbus_interface geoclue-2.0 OUTPUT_VARIABLE GEOCLUE_DBUS_INTERFACE)
#     add_custom_command(
#          OUTPUT ${DERIVED_SOURCES_WEBCORE_DIR}/Geoclue2Interface.c ${DERIVED_SOURCES_WEBCORE_DIR}/Geoclue2Interface.h
#          COMMAND gdbus-codegen --interface-prefix org.freedesktop.GeoClue2. --c-namespace Geoclue --generate-c-code ${DERIVED_SOURCES_WEBCORE_DIR}/Geoclue2Interface ${GEOCLUE_DBUS_INTERFACE}
#     )
# endif ()

list(APPEND WebCore_USER_AGENT_STYLE_SHEETS
    ${WEBCORE_DIR}/css/mediaControlsGtk.css
)

set(WebCore_USER_AGENT_SCRIPTS
    ${WEBCORE_DIR}/English.lproj/mediaControlsLocalizedStrings.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsBase.js
    ${WEBCORE_DIR}/Modules/mediacontrols/mediaControlsGtk.js
)

#//XXX: clean up the list
set(WebCore_FORWARDING_HEADERS_DIRECTORIES
    accessibility
    bridge
    contentextensions
    crypto
    css
    dom
    editing
    fileapi
    history
    html
    inspector
    loader
    page
    platform
    plugins
    rendering
    replay
    storage
    style
    svg

    Modules/geolocation
    Modules/indexeddb
    Modules/notifications
    Modules/webdatabase

    bindings/generic
    bindings/js
    bindings/objc

    bridge/jsc

    html/forms
    html/parser
    html/shadow

    loader/appcache
    loader/archive
    loader/cache

    loader/archive/cf

    page/animation
    page/scrolling

    platform/animation
    platform/audio
    platform/graphics
    platform/mac
    platform/mock
    platform/network
    platform/sql
    platform/text

    platform/graphics/ca
    platform/graphics/cg
    platform/graphics/filters
    platform/graphics/mac
    platform/graphics/transforms

    platform/network/cf
    platform/network/cocoa
    platform/network/mac

    platform/spi/cf
    platform/spi/cg
    platform/spi/cocoa
    platform/spi/mac

    rendering/line
    rendering/style

    svg/graphics
    svg/properties
)

set(WebCore_FORWARDING_HEADERS_FILES
    loader/appcache/ApplicationCacheStorage.h
)

set(WebCore_USER_AGENT_SCRIPTS_DEPENDENCIES ${WEBCORE_DIR}/platform/java/RenderThemeJava.cpp)
message(STATUS "================== SQLITE_LIBRARIES ${SQLITE_LIBRARIES}")
list(APPEND WebCore_LIBRARIES
    ${ATK_LIBRARIES}
    ${CAIRO_LIBRARIES}
#     ${ENCHANT_LIBRARIES}
    ${FONTCONFIG_LIBRARIES}
    ${FREETYPE2_LIBRARIES}
#     ${GEOCLUE_LIBRARIES}
#     ${GLIB_GIO_LIBRARIES}
#     ${GLIB_GMODULE_LIBRARIES}
#     ${GLIB_GOBJECT_LIBRARIES}
#     ${GLIB_LIBRARIES}
#     ${GUDEV_LIBRARIES}
#     ${HARFBUZZ_LIBRARIES}
#     ${JPEG_LIBRARIES}
#     ${LIBSECRET_LIBRARIES}
#     ${LIBSOUP_LIBRARIES}
    ${LIBXML2_LIBRARIES}
    ${LIBXSLT_LIBRARIES}
#     ${HYPHEN_LIBRARIES}
#     ${PNG_LIBRARIES}
    ${SQLITE_LIBRARIES}
#     ${WEBP_LIBRARIES}
    ${X11_X11_LIB}
    ${X11_Xcomposite_LIB}
    ${X11_Xdamage_LIB}
    ${X11_Xrender_LIB}
    ${X11_Xt_LIB}
    ${ZLIB_LIBRARIES}
)

list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    ${ATK_INCLUDE_DIRS}
    ${CAIRO_INCLUDE_DIRS}
    # ${ENCHANT_INCLUDE_DIRS}
    ${FREETYPE2_INCLUDE_DIRS}
    # ${GEOCLUE_INCLUDE_DIRS}
    # ${GIO_UNIX_INCLUDE_DIRS}
    # ${GLIB_INCLUDE_DIRS}
    # ${GUDEV_INCLUDE_DIRS}
    # ${HARFBUZZ_INCLUDE_DIRS}
    # ${LIBSECRET_INCLUDE_DIRS}
    # ${LIBSOUP_INCLUDE_DIRS}
    ${LIBXML2_INCLUDE_DIR}
    ${LIBXSLT_INCLUDE_DIR}
    # ${SQLITE_INCLUDE_DIR}
    # ${WEBP_INCLUDE_DIRS}
    ${ZLIB_INCLUDE_DIRS}
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

message(STATUS "==== #### LIBXML2_INCLUDE_DIR  ${LIBXML2_INCLUDE_DIR}")
message(STATUS "==== #### LIBXML2_LIBRARIES  ${LIBXML2_LIBRARIES}")

# if (ENABLE_VIDEO OR ENABLE_WEB_AUDIO)
    # list(APPEND WebCore_INCLUDE_DIRECTORIES
    #     ${WEBCORE_DIR}/platform/graphics/gstreamer
    # )

    # list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
    #     ${GSTREAMER_INCLUDE_DIRS}
    #     ${GSTREAMER_BASE_INCLUDE_DIRS}
    #     ${GSTREAMER_APP_INCLUDE_DIRS}
    #     ${GSTREAMER_PBUTILS_INCLUDE_DIRS}
    # )

    # list(APPEND WebCore_LIBRARIES
    #     ${GSTREAMER_APP_LIBRARIES}
    #     ${GSTREAMER_BASE_LIBRARIES}
    #     ${GSTREAMER_LIBRARIES}
    #     ${GSTREAMER_PBUTILS_LIBRARIES}
    #     ${GSTREAMER_AUDIO_LIBRARIES}
    # )
    # Avoiding a GLib deprecation warning due to GStreamer API using deprecated classes.
    # set_source_files_properties(platform/audio/gstreamer/WebKitWebAudioSourceGStreamer.cpp PROPERTIES COMPILE_DEFINITIONS "GLIB_DISABLE_DEPRECATION_WARNINGS=1")
# endif ()

# if (ENABLE_VIDEO)
#     list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#         ${GSTREAMER_TAG_INCLUDE_DIRS}
#         ${GSTREAMER_VIDEO_INCLUDE_DIRS}
#     )
#     list(APPEND WebCore_LIBRARIES
#         ${GSTREAMER_TAG_LIBRARIES}
#         ${GSTREAMER_VIDEO_LIBRARIES}
#     )

#     if (USE_GSTREAMER_MPEGTS)
#         list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#             ${GSTREAMER_MPEGTS_INCLUDE_DIRS}
#         )

#         list(APPEND WebCore_LIBRARIES
#             ${GSTREAMER_MPEGTS_LIBRARIES}
#         )
#     endif ()

#     if (USE_GSTREAMER_GL)
#         list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#             ${GSTREAMER_GL_INCLUDE_DIRS}
#         )

#         list(APPEND WebCore_LIBRARIES
#             ${GSTREAMER_GL_LIBRARIES}
#         )
#     endif ()
# endif ()

# if (ENABLE_WEB_AUDIO)
#     list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#         ${WEBCORE_DIR}/platform/audio/gstreamer
#         ${GSTREAMER_AUDIO_INCLUDE_DIRS}
#         ${GSTREAMER_FFT_INCLUDE_DIRS}
#     )
#     list(APPEND WebCore_LIBRARIES
#         ${GSTREAMER_FFT_LIBRARIES}
#     )
# endif ()

# if (ENABLE_MEDIA_STREAM)
#     list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#         ${OPENWEBRTC_INCLUDE_DIRS}
#     )
#     list(APPEND WebCore_LIBRARIES
#         ${OPENWEBRTC_LIBRARIES}
#     )
# endif ()

if (USE_TEXTURE_MAPPER)
    list(APPEND WebCore_INCLUDE_DIRECTORIES
        "${WEBCORE_DIR}/platform/graphics/texmap"
    )
    list(APPEND WebCore_SOURCES
        platform/graphics/texmap/BitmapTexture.cpp
        platform/graphics/texmap/BitmapTexturePool.cpp
        platform/graphics/texmap/GraphicsLayerTextureMapper.cpp
    )
    message(STATUS "========== use USE_TEXTURE_MAPPER ")
else()
    message(STATUS "========== do not use USE_TEXTURE_MAPPER ")
endif ()

if (ENABLE_THREADED_COMPOSITOR)
    list(APPEND WebCore_INCLUDE_DIRECTORIES
        "${WEBCORE_DIR}/page/scrolling/coordinatedgraphics"
        "${WEBCORE_DIR}/platform/graphics/texmap/coordinated"
        "${WEBCORE_DIR}/platform/graphics/texmap/threadedcompositor"
    )
    list(APPEND WebCore_SOURCES
        page/scrolling/ScrollingStateStickyNode.cpp
        page/scrolling/ScrollingThread.cpp
        page/scrolling/ScrollingTreeNode.cpp
        page/scrolling/ScrollingTreeScrollingNode.cpp

        page/scrolling/coordinatedgraphics/ScrollingCoordinatorCoordinatedGraphics.cpp
        page/scrolling/coordinatedgraphics/ScrollingStateNodeCoordinatedGraphics.cpp
        page/scrolling/coordinatedgraphics/ScrollingStateScrollingNodeCoordinatedGraphics.cpp

        platform/graphics/texmap/coordinated/AreaAllocator.cpp
        platform/graphics/texmap/coordinated/CompositingCoordinator.cpp
        platform/graphics/texmap/coordinated/CoordinatedGraphicsLayer.cpp
        platform/graphics/texmap/coordinated/CoordinatedImageBacking.cpp
        platform/graphics/texmap/coordinated/CoordinatedSurface.cpp
        platform/graphics/texmap/coordinated/Tile.cpp
        platform/graphics/texmap/coordinated/TiledBackingStore.cpp
        platform/graphics/texmap/coordinated/UpdateAtlas.cpp
    )
endif ()


if (USE_OPENGL_ES_2)
    list(APPEND WebCore_SOURCES
        platform/graphics/opengl/Extensions3DOpenGLES.cpp
        platform/graphics/opengl/GraphicsContext3DOpenGLES.cpp
    )
endif ()

if (USE_OPENGL)
    list(APPEND WebCore_SOURCES
        platform/graphics/OpenGLShims.cpp

        platform/graphics/opengl/Extensions3DOpenGL.cpp
        platform/graphics/opengl/GraphicsContext3DOpenGL.cpp
    )
endif ()

# if (ENABLE_PLUGIN_PROCESS_GTK2)
#     # WebKitPluginProcess2 needs a version of WebCore compiled against GTK+2, so we've isolated all the GTK+
#     # dependent files into a separate library which can be used to construct a GTK+2 WebCore
#     # for the plugin process.
#     add_library(WebCorePlatformGTK2 ${WebCore_LIBRARY_TYPE} ${WebCorePlatformGTK_SOURCES})
#     add_dependencies(WebCorePlatformGTK2 WebCore)
#     WEBKIT_SET_EXTRA_COMPILER_FLAGS(WebCorePlatformGTK2)
#     set_property(TARGET WebCorePlatformGTK2
#         APPEND
#         PROPERTY COMPILE_DEFINITIONS GTK_API_VERSION_2=1
#     )
#     target_include_directories(WebCorePlatformGTK2 PRIVATE
#         ${WebCore_INCLUDE_DIRECTORIES}
#         ${GTK2_INCLUDE_DIRS}
#         ${GDK2_INCLUDE_DIRS}
#     )
#     target_include_directories(WebCorePlatformGTK2 SYSTEM PRIVATE
#         ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}
#     )
#     target_link_libraries(WebCorePlatformGTK2
#          ${WebCore_LIBRARIES}
#          ${GTK2_LIBRARIES}
#          ${GDK2_LIBRARIES}
#     )
# endif ()

# if (ENABLE_WAYLAND_TARGET)
#     # Wayland protocol extension.
#     add_custom_command(
#         OUTPUT ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitGtkWaylandClientProtocol.c
#         DEPENDS ${WEBCORE_DIR}/platform/graphics/wayland/WebKitGtkWaylandClientProtocol.xml
#         COMMAND wayland-scanner server-header < ${WEBCORE_DIR}/platform/graphics/wayland/WebKitGtkWaylandClientProtocol.xml > ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitGtkWaylandServerProtocol.h
#         COMMAND wayland-scanner client-header < ${WEBCORE_DIR}/platform/graphics/wayland/WebKitGtkWaylandClientProtocol.xml > ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitGtkWaylandClientProtocol.h
#         COMMAND wayland-scanner code < ${WEBCORE_DIR}/platform/graphics/wayland/WebKitGtkWaylandClientProtocol.xml > ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitGtkWaylandClientProtocol.c
#     )

#     list(APPEND WebCore_SOURCES
#         platform/graphics/wayland/PlatformDisplayWayland.cpp
#         platform/graphics/wayland/WaylandEventSource.cpp
#         platform/graphics/wayland/WaylandSurface.cpp

#         ${DERIVED_SOURCES_WEBCORE_DIR}/WebKitGtkWaylandClientProtocol.c
#     )

#     list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#         ${WAYLAND_INCLUDE_DIRS}
#     )
#     list(APPEND WebCore_LIBRARIES
#         ${WAYLAND_LIBRARIES}
#     )
# endif ()

include_directories(
    "${WebCore_INCLUDE_DIRECTORIES}"
#     "${WEBCORE_DIR}/bindings/gobject/"
    "${DERIVED_SOURCES_DIR}"
#     "${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}"
)

# message(STATUS "==== #### WebCore_SYSTEM_INCLUDE_DIRECTORIES ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}")
# message(STATUS "==== #### WebCore_INCLUDE_DIRECTORIES  ${WebCore_INCLUDE_DIRECTORIES}")
# message(STATUS "==== #### WebCore_SOURCES  ${WebCore_SOURCES}")
# message(STATUS "==== #### DERIVED_SOURCES_DIR  ${DERIVED_SOURCES_DIR}")

include_directories(SYSTEM
    ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}
)

# list(APPEND GObjectDOMBindings_SOURCES
#     bindings/gobject/ConvertToUTF8String.cpp
#     bindings/gobject/DOMObjectCache.cpp
#     bindings/gobject/GObjectEventListener.cpp
#     bindings/gobject/GObjectNodeFilterCondition.cpp
#     bindings/gobject/GObjectXPathNSResolver.cpp
#     bindings/gobject/WebKitDOMCustom.cpp
#     bindings/gobject/WebKitDOMDeprecated.cpp
#     bindings/gobject/WebKitDOMEventTarget.cpp
#     bindings/gobject/WebKitDOMHTMLPrivate.cpp
#     bindings/gobject/WebKitDOMNodeFilter.cpp
#     bindings/gobject/WebKitDOMObject.cpp
#     bindings/gobject/WebKitDOMPrivate.cpp
#     bindings/gobject/WebKitDOMXPathNSResolver.cpp
#     ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines.h
#     ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines-unstable.h
#     ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdom.h
# )

# list(APPEND GObjectDOMBindingsStable_IDL_FILES
#     css/CSSRule.idl
#     css/CSSRuleList.idl
#     css/CSSStyleDeclaration.idl
#     css/CSSStyleSheet.idl
#     css/CSSValue.idl
#     css/MediaList.idl
#     css/StyleSheet.idl
#     css/StyleSheetList.idl

#     dom/Attr.idl
#     dom/CDATASection.idl
#     dom/CharacterData.idl
#     dom/Comment.idl
#     dom/DOMImplementation.idl
#     dom/Document.idl
#     dom/DocumentFragment.idl
#     dom/DocumentType.idl
#     dom/Element.idl
#     dom/EntityReference.idl
#     dom/Event.idl
#     dom/KeyboardEvent.idl
#     dom/MouseEvent.idl
#     dom/NamedNodeMap.idl
#     dom/Node.idl
#     dom/NodeIterator.idl
#     dom/NodeList.idl
#     dom/ProcessingInstruction.idl
#     dom/Range.idl
#     dom/Text.idl
#     dom/TreeWalker.idl
#     dom/UIEvent.idl
#     dom/WheelEvent.idl

#     fileapi/Blob.idl
#     fileapi/File.idl
#     fileapi/FileList.idl

#     html/HTMLAnchorElement.idl
#     html/HTMLAppletElement.idl
#     html/HTMLAreaElement.idl
#     html/HTMLBRElement.idl
#     html/HTMLBaseElement.idl
#     html/HTMLBaseFontElement.idl
#     html/HTMLBodyElement.idl
#     html/HTMLButtonElement.idl
#     html/HTMLCanvasElement.idl
#     html/HTMLCollection.idl
#     html/HTMLDListElement.idl
#     html/HTMLDirectoryElement.idl
#     html/HTMLDivElement.idl
#     html/HTMLDocument.idl
#     html/HTMLElement.idl
#     html/HTMLEmbedElement.idl
#     html/HTMLFieldSetElement.idl
#     html/HTMLFontElement.idl
#     html/HTMLFormElement.idl
#     html/HTMLFrameElement.idl
#     html/HTMLFrameSetElement.idl
#     html/HTMLHRElement.idl
#     html/HTMLHeadElement.idl
#     html/HTMLHeadingElement.idl
#     html/HTMLHtmlElement.idl
#     html/HTMLIFrameElement.idl
#     html/HTMLImageElement.idl
#     html/HTMLInputElement.idl
#     html/HTMLLIElement.idl
#     html/HTMLLabelElement.idl
#     html/HTMLLegendElement.idl
#     html/HTMLLinkElement.idl
#     html/HTMLMapElement.idl
#     html/HTMLMarqueeElement.idl
#     html/HTMLMenuElement.idl
#     html/HTMLMetaElement.idl
#     html/HTMLModElement.idl
#     html/HTMLOListElement.idl
#     html/HTMLObjectElement.idl
#     html/HTMLOptGroupElement.idl
#     html/HTMLOptionElement.idl
#     html/HTMLOptionsCollection.idl
#     html/HTMLParagraphElement.idl
#     html/HTMLParamElement.idl
#     html/HTMLPreElement.idl
#     html/HTMLQuoteElement.idl
#     html/HTMLScriptElement.idl
#     html/HTMLSelectElement.idl
#     html/HTMLStyleElement.idl
#     html/HTMLTableCaptionElement.idl
#     html/HTMLTableCellElement.idl
#     html/HTMLTableColElement.idl
#     html/HTMLTableElement.idl
#     html/HTMLTableRowElement.idl
#     html/HTMLTableSectionElement.idl
#     html/HTMLTextAreaElement.idl
#     html/HTMLTitleElement.idl
#     html/HTMLUListElement.idl

#     page/DOMWindow.idl

#     xml/XPathExpression.idl
#     xml/XPathResult.idl
# )

# list(APPEND GObjectDOMBindingsUnstable_IDL_FILES
#     Modules/battery/BatteryManager.idl

#     Modules/gamepad/deprecated/Gamepad.idl
#     Modules/gamepad/deprecated/GamepadList.idl

#     Modules/geolocation/Geolocation.idl

#     Modules/mediasource/VideoPlaybackQuality.idl

#     Modules/mediastream/MediaDevices.idl
#     Modules/mediastream/NavigatorMediaDevices.idl

#     Modules/quota/StorageInfo.idl
#     Modules/quota/StorageQuota.idl

#     Modules/speech/DOMWindowSpeechSynthesis.idl
#     Modules/speech/SpeechSynthesis.idl
#     Modules/speech/SpeechSynthesisEvent.idl
#     Modules/speech/SpeechSynthesisUtterance.idl
#     Modules/speech/SpeechSynthesisVoice.idl

#     Modules/webdatabase/Database.idl

#     css/DOMWindowCSS.idl
#     css/MediaQueryList.idl
#     css/StyleMedia.idl

#     dom/DOMNamedFlowCollection.idl
#     dom/DOMStringList.idl
#     dom/DOMStringMap.idl
#     dom/MessagePort.idl
#     dom/Touch.idl
#     dom/WebKitNamedFlow.idl

#     html/DOMSettableTokenList.idl
#     html/DOMTokenList.idl
#     html/HTMLDetailsElement.idl
#     html/HTMLKeygenElement.idl
#     html/HTMLMediaElement.idl
#     html/MediaController.idl
#     html/MediaError.idl
#     html/TimeRanges.idl
#     html/ValidityState.idl

#     loader/appcache/DOMApplicationCache.idl

#     page/BarProp.idl
#     page/DOMSecurityPolicy.idl
#     page/DOMSelection.idl
#     page/History.idl
#     page/Location.idl
#     page/Navigator.idl
#     page/Performance.idl
#     page/PerformanceEntry.idl
#     page/PerformanceEntryList.idl
#     page/PerformanceNavigation.idl
#     page/PerformanceTiming.idl
#     page/Screen.idl
#     page/UserMessageHandler.idl
#     page/UserMessageHandlersNamespace.idl
#     page/WebKitNamespace.idl
#     page/WebKitPoint.idl

#     plugins/DOMMimeType.idl
#     plugins/DOMMimeTypeArray.idl
#     plugins/DOMPlugin.idl
#     plugins/DOMPluginArray.idl

#     storage/Storage.idl
# )

# if (ENABLE_VIDEO OR ENABLE_WEB_AUDIO)
#     list(APPEND GObjectDOMBindingsUnstable_IDL_FILES
#         html/HTMLAudioElement.idl
#         html/HTMLVideoElement.idl

#         html/track/AudioTrack.idl
#         html/track/AudioTrackList.idl
#         html/track/DataCue.idl
#         html/track/TextTrack.idl
#         html/track/TextTrackCue.idl
#         html/track/TextTrackCueList.idl
#         html/track/TextTrackList.idl
#         html/track/TrackEvent.idl
#         html/track/VTTCue.idl
#         html/track/VideoTrack.idl
#         html/track/VideoTrackList.idl
#     )
# endif ()

# if (ENABLE_QUOTA)
#     list(APPEND GObjectDOMBindingsUnstable_IDL_FILES
#         Modules/quota/DOMWindowQuota.idl
#         Modules/quota/NavigatorStorageQuota.idl
#         Modules/quota/StorageErrorCallback.idl
#         Modules/quota/StorageInfo.idl
#         Modules/quota/StorageQuota.idl
#         Modules/quota/StorageQuotaCallback.idl
#         Modules/quota/StorageUsageCallback.idl
#         Modules/quota/WorkerNavigatorStorageQuota.idl
#     )
# endif ()

# set(GObjectDOMBindings_STATIC_CLASS_LIST Custom Deprecated EventTarget NodeFilter Object XPathNSResolver)

# set(GObjectDOMBindingsStable_CLASS_LIST ${GObjectDOMBindings_STATIC_CLASS_LIST})
# set(GObjectDOMBindingsStable_INSTALLED_HEADERS
#      ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines.h
#      ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdom.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMCustom.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMDeprecated.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMEventTarget.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMNodeFilter.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMObject.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMXPathNSResolver.h
# )

# set(GObjectDOMBindingsUnstable_INSTALLED_HEADERS
#      ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines-unstable.h
#      ${WEBCORE_DIR}/bindings/gobject/WebKitDOMCustomUnstable.h
# )

# foreach (file ${GObjectDOMBindingsStable_IDL_FILES})
#     get_filename_component(classname ${file} NAME_WE)
#     list(APPEND GObjectDOMBindingsStable_CLASS_LIST ${classname})
#     list(APPEND GObjectDOMBindingsStable_INSTALLED_HEADERS ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/WebKitDOM${classname}.h)
#     list(APPEND GObjectDOMBindingsUnstable_INSTALLED_HEADERS ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/WebKitDOM${classname}Unstable.h)
# endforeach ()

# foreach (file ${GObjectDOMBindingsUnstable_IDL_FILES})
#     get_filename_component(classname ${file} NAME_WE)
#     list(APPEND GObjectDOMBindingsUnstable_CLASS_LIST ${classname})
#     list(APPEND GObjectDOMBindingsUnstable_INSTALLED_HEADERS ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/WebKitDOM${classname}.h)
# endforeach ()

# set(GOBJECT_DOM_BINDINGS_FEATURES_DEFINES "LANGUAGE_GOBJECT=1 ${FEATURE_DEFINES_WITH_SPACE_SEPARATOR}")
# string(REPLACE "ENABLE_INDEXED_DATABASE=1" "" GOBJECT_DOM_BINDINGS_FEATURES_DEFINES ${GOBJECT_DOM_BINDINGS_FEATURES_DEFINES})
# string(REPLACE REGEX "ENABLE_SVG[A-Z_]+=1" "" GOBJECT_DOM_BINDINGS_FEATURES_DEFINES ${GOBJECT_DOM_BINDINGS_FEATURES_DEFINES})

# file(MAKE_DIRECTORY ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR})

# add_custom_command(
#     OUTPUT ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines.h
#     DEPENDS ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl
#     COMMAND echo ${GObjectDOMBindingsStable_CLASS_LIST} | ${PERL_EXECUTABLE} ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl defines > ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines.h
# )

# add_custom_command(
#     OUTPUT ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines-unstable.h
#     DEPENDS ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl
#     COMMAND echo ${GObjectDOMBindingsUnstable_CLASS_LIST} | ${PERL_EXECUTABLE} ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl defines-unstable > ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdomdefines-unstable.h
# )

# add_custom_command(
#     OUTPUT ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdom.h
#     DEPENDS ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl
#     COMMAND echo ${GObjectDOMBindingsStable_CLASS_LIST} | ${PERL_EXECUTABLE} ${WEBCORE_DIR}/bindings/scripts/gobject-generate-headers.pl gdom > ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/webkitdom.h
# )

# # Some of the static headers are included by generated public headers with include <webkitdom/WebKitDOMFoo.h>.
# # We need those headers in the derived sources to be in webkitdom directory.
# set(GObjectDOMBindings_STATIC_HEADER_NAMES ${GObjectDOMBindings_STATIC_CLASS_LIST} CustomUnstable)
# foreach (classname ${GObjectDOMBindings_STATIC_HEADER_NAMES})
#     add_custom_command(
#         OUTPUT ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/WebKitDOM${classname}.h
#         DEPENDS ${WEBCORE_DIR}/bindings/gobject/WebKitDOM${classname}.h
#         COMMAND ln -n -s -f ${WEBCORE_DIR}/bindings/gobject/WebKitDOM${classname}.h ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}
#     )
#     list(APPEND GObjectDOMBindings_STATIC_GENERATED_SOURCES ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/WebKitDOM${classname}.h)
# endforeach ()

# add_custom_target(fake-generated-webkitdom-headers
#     DEPENDS ${GObjectDOMBindings_STATIC_GENERATED_SOURCES}
# )

# set(GObjectDOMBindings_IDL_FILES ${GObjectDOMBindingsStable_IDL_FILES} ${GObjectDOMBindingsUnstable_IDL_FILES})
# set(ADDITIONAL_BINDINGS_DEPENDENCIES
#     ${WEBCORE_DIR}/bindings/gobject/webkitdom.symbols
#     ${WINDOW_CONSTRUCTORS_FILE}
#     ${WORKERGLOBALSCOPE_CONSTRUCTORS_FILE}
#     ${DEDICATEDWORKERGLOBALSCOPE_CONSTRUCTORS_FILE}
# )


# list(APPEND JavaBindings_SOURCES
    
# )

# GENERATE_BINDINGS(GObjectDOMBindings_SOURCES
#     "${GObjectDOMBindings_IDL_FILES}"
#     "${WEBCORE_DIR}"
#     "${IDL_INCLUDES}"
#     "${GOBJECT_DOM_BINDINGS_FEATURES_DEFINES}"
#     ${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}
#     WebKitDOM GObject cpp
#     ${IDL_ATTRIBUTES_FILE}
#     ${SUPPLEMENTAL_DEPENDENCY_FILE}
#     ${ADDITIONAL_BINDINGS_DEPENDENCIES})

# add_definitions(-DBUILDING_WEBKIT)
# add_definitions(-DWEBKIT_DOM_USE_UNSTABLE_API)

add_definitions(-DIMAGEIO=1)

# add_definitions(-DWTF_USE_ACCELERATED_COMPOSITING=1)
# add_definitions(-DWTF_USE_LIBXML2=1)

# add_library(GObjectDOMBindings STATIC ${GObjectDOMBindings_SOURCES})

# WEBKIT_SET_EXTRA_COMPILER_FLAGS(GObjectDOMBindings)

# add_dependencies(GObjectDOMBindings
#     WebCore
#     fake-generated-webkitdom-headers
# )

# file(WRITE ${CMAKE_BINARY_DIR}/gtkdoc-webkitdom.cfg
#     "[webkitdomgtk-${WEBKITGTK_API_VERSION}]\n"
#     "pkgconfig_file=${WebKit2_PKGCONFIG_FILE}\n"
#     "namespace=webkit_dom\n"
#     "cflags=-I${CMAKE_SOURCE_DIR}/Source\n"
#     "       -I${WEBCORE_DIR}/bindings\n"
#     "       -I${WEBCORE_DIR}/bindings/gobject\n"
#     "       -I${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}\n"
#     "doc_dir=${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}/docs\n"
#     "source_dirs=${DERIVED_SOURCES_GOBJECT_DOM_BINDINGS_DIR}\n"
#     "            ${WEBCORE_DIR}/bindings/gobject\n"
#     "headers=${GObjectDOMBindingsStable_INSTALLED_HEADERS}\n"
#     "main_sgml_file=webkitdomgtk-docs.sgml\n"
# )

# install(FILES ${GObjectDOMBindingsStable_INSTALLED_HEADERS}
#         DESTINATION "${WEBKITGTK_HEADER_INSTALL_DIR}/webkitdom"
# )

# # Make unstable header optional if they don't exist
# install(FILES ${GObjectDOMBindingsUnstable_INSTALLED_HEADERS}
#         DESTINATION "${WEBKITGTK_HEADER_INSTALL_DIR}/webkitdom"
#         OPTIONAL
# )

# # Some installed headers are not on the list of headers used for gir generation.
# set(GObjectDOMBindings_GIR_HEADERS ${GObjectDOMBindingsStable_INSTALLED_HEADERS})
# list(REMOVE_ITEM GObjectDOMBindings_GIR_HEADERS
#      bindings/gobject/WebKitDOMEventTarget.h
#      bindings/gobject/WebKitDOMNodeFilter.h
#      bindings/gobject/WebKitDOMObject.h
#      bindings/gobject/WebKitDOMXPathNSResolver.h
# )

# # Propagate this variable to the parent scope, so that it can be used in other parts of the build.
# set(GObjectDOMBindings_GIR_HEADERS ${GObjectDOMBindings_GIR_HEADERS} PARENT_SCOPE)

# if (ENABLE_SUBTLE_CRYPTO)
#     list(APPEND WebCore_SOURCES
#         crypto/CryptoAlgorithm.cpp
#         crypto/CryptoAlgorithmDescriptionBuilder.cpp
#         crypto/CryptoAlgorithmRegistry.cpp
#         crypto/CryptoKey.cpp
#         crypto/CryptoKeyPair.cpp
#         crypto/SubtleCrypto.cpp

#         crypto/algorithms/CryptoAlgorithmAES_CBC.cpp
#         crypto/algorithms/CryptoAlgorithmAES_KW.cpp
#         crypto/algorithms/CryptoAlgorithmHMAC.cpp
#         crypto/algorithms/CryptoAlgorithmRSAES_PKCS1_v1_5.cpp
#         crypto/algorithms/CryptoAlgorithmRSASSA_PKCS1_v1_5.cpp
#         crypto/algorithms/CryptoAlgorithmRSA_OAEP.cpp
#         crypto/algorithms/CryptoAlgorithmSHA1.cpp
#         crypto/algorithms/CryptoAlgorithmSHA224.cpp
#         crypto/algorithms/CryptoAlgorithmSHA256.cpp
#         crypto/algorithms/CryptoAlgorithmSHA384.cpp
#         crypto/algorithms/CryptoAlgorithmSHA512.cpp

#         crypto/gnutls/CryptoAlgorithmAES_CBCGnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmAES_KWGnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmHMACGnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmRSAES_PKCS1_v1_5GnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmRSASSA_PKCS1_v1_5GnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmRSA_OAEPGnuTLS.cpp
#         crypto/gnutls/CryptoAlgorithmRegistryGnuTLS.cpp
#         crypto/gnutls/CryptoDigestGnuTLS.cpp
#         crypto/gnutls/CryptoKeyRSAGnuTLS.cpp
#         crypto/gnutls/SerializedCryptoKeyWrapGnuTLS.cpp

#         crypto/keys/CryptoKeyAES.cpp
#         crypto/keys/CryptoKeyDataOctetSequence.cpp
#         crypto/keys/CryptoKeyDataRSAComponents.cpp
#         crypto/keys/CryptoKeyHMAC.cpp
#         crypto/keys/CryptoKeySerializationRaw.cpp
#     )

#     list(APPEND WebCore_SYSTEM_INCLUDE_DIRECTORIES
#         ${GNUTLS_INCLUDE_DIRS}
#     )
#     list(APPEND WebCore_LIBRARIES
#         ${GNUTLS_LIBRARIES}
#     )
# endif ()


list(APPEND WebCore_LIBRARIES
    ${JAVA_JVM_LIBRARY} #//XXX: remove?
)

# target_link_libraries(WTF WebCore)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

# add_library(WebCorePlatformJava SHARED ${WebCorePlatformJava_SOURCES})
# add_dependencies(WebCorePlatformJava WebCore)
# WEBKIT_SET_EXTRA_COMPILER_FLAGS(WebCorePlatformJava)
# target_include_directories(WebCorePlatformJava PRIVATE
#     ${WebCore_INCLUDE_DIRECTORIES}
# )
# target_include_directories(WebCorePlatformJava SYSTEM PRIVATE
#     ${WebCore_SYSTEM_INCLUDE_DIRECTORIES}
# #     ${GTK_INCLUDE_DIRS}
# #     ${GDK_INCLUDE_DIRS}
# )

# set_target_properties(WebCorePlatformJava PROPERTIES LINK_FLAGS "-Xlinker -version-script=${WEBCORE_DIR}/mapfile-vers")
# target_link_libraries(WebCorePlatformJava
#     ${WebCore_LIBRARIES}
# #     ${GTK_LIBRARIES}
# #     ${GDK_LIBRARIES}
# )

