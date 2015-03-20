TEMPLATE = lib
include(DerivedSourcesJava.pri)

CONFIG += plugin depend_includepath precompile_header
CONFIG -= debug_and_release
TARGET = jfxwebkit

VPATH += $$PWD

win32-*|linux* {
    PRECOMPILED_HEADER = $$PWD/webcorejava_pch.h
}

QMAKE_MACOSX_DEPLOYMENT_TARGET = 10.8

mac*|linux* {
    QMAKE_CXXFLAGS += -std=c++11
    QMAKE_CXXFLAGS += -include string.h # todo tav temp
}

*clang* {
    QMAKE_CXXFLAGS += -stdlib=libc++
}

win* {
    # On windows, we have to include the imported icu headers first
    # in order to avoid a file name conflict with wtf files
    # due to case insensitive file system
    INCLUDEPATH += $(WEBKIT_OUTPUTDIR)/import/include/icu
}

mac* {
    # on macosx, we do not have icu headeres for system libraries,
    # so a snapshot of icu headers is used.
    INCLUDEPATH += $$SOURCE_DIR/WebCore/icu
}

INCLUDEPATH += \
    $${GENERATED_SOURCES_DIR} \
    $$SOURCE_DIR \
    $$SOURCE_DIR/WebCore \
    $$SOURCE_DIR/WebCore/accessibility \
    $$SOURCE_DIR/WebCore/bindings \
    $$SOURCE_DIR/WebCore/bindings/generic \
    $$SOURCE_DIR/WebCore/bindings/java \
    $$SOURCE_DIR/WebCore/bindings/js \
    $$SOURCE_DIR/WebCore/bridge \
    $$SOURCE_DIR/WebCore/bridge/c \
    $$SOURCE_DIR/WebCore/bridge/jni \
    $$SOURCE_DIR/WebCore/bridge/jni/jsc \
    $$SOURCE_DIR/WebCore/bridge/jsc \
    $$SOURCE_DIR/WebCore/css \
    $$SOURCE_DIR/WebCore/cssjit \
    $$SOURCE_DIR/WebCore/crypto/keys \
    $$SOURCE_DIR/WebCore/crypto \
    $$SOURCE_DIR/WebCore/dom \
    $$SOURCE_DIR/WebCore/dom/default \
    $$SOURCE_DIR/WebCore/editing \
    $$SOURCE_DIR/WebCore/fileapi \
    $$SOURCE_DIR/WebCore/history \
    $$SOURCE_DIR/WebCore/html \
    $$SOURCE_DIR/WebCore/html/canvas \
    $$SOURCE_DIR/WebCore/html/parser \
    $$SOURCE_DIR/WebCore/html/shadow \
    $$SOURCE_DIR/WebCore/html/track \
    $$SOURCE_DIR/WebCore/html/forms \
    $$SOURCE_DIR/WebCore/inspector \
    $$SOURCE_DIR/WebCore/loader \
    $$SOURCE_DIR/WebCore/loader/appcache \
    $$SOURCE_DIR/WebCore/loader/archive \
    $$SOURCE_DIR/WebCore/loader/archive/mhtml \ 
    $$SOURCE_DIR/WebCore/loader/cache \
    $$SOURCE_DIR/WebCore/loader/icon \
    $$SOURCE_DIR/WebCore/mathml \
    $$SOURCE_DIR/WebCore/page \
    $$SOURCE_DIR/WebCore/page/animation \
    $$SOURCE_DIR/WebCore/page/java \
    $$SOURCE_DIR/WebCore/page/scrolling \
    $$SOURCE_DIR/WebCore/platform \
    $$SOURCE_DIR/WebCore/platform/animation \
    $$SOURCE_DIR/WebCore/platform/audio \
    $$SOURCE_DIR/WebCore/platform/java \
    $$SOURCE_DIR/WebCore/platform/graphics \
    $$SOURCE_DIR/WebCore/platform/graphics/filters \
    $$SOURCE_DIR/WebCore/platform/graphics/filters/arm \
    $$SOURCE_DIR/WebCore/platform/graphics/java \
    $$SOURCE_DIR/WebCore/platform/graphics/texmap \
    $$SOURCE_DIR/WebCore/platform/graphics/transforms \
    $$SOURCE_DIR/WebCore/platform/image-decoders \
    $$SOURCE_DIR/WebCore/platform/leveldb \
    $$SOURCE_DIR/WebCore/platform/mock \
    $$SOURCE_DIR/WebCore/platform/network \
    $$SOURCE_DIR/WebCore/platform/network/java \
    $$SOURCE_DIR/WebCore/platform/sql \
    $$SOURCE_DIR/WebCore/platform/text \
    $$SOURCE_DIR/WebCore/platform/text/icu \
    $$SOURCE_DIR/WebCore/plugins \
    $$SOURCE_DIR/WebCore/rendering \
    $$SOURCE_DIR/WebCore/rendering/mathml \
    $$SOURCE_DIR/WebCore/rendering/style \
    $$SOURCE_DIR/WebCore/rendering/svg \
    $$SOURCE_DIR/WebCore/rendering/line \
    $$SOURCE_DIR/WebCore/rendering/shapes \
    $$SOURCE_DIR/WebCore/storage \
    $$SOURCE_DIR/WebCore/style \
    $$SOURCE_DIR/WebCore/svg \
    $$SOURCE_DIR/WebCore/svg/animation \
    $$SOURCE_DIR/WebCore/svg/graphics \
    $$SOURCE_DIR/WebCore/svg/graphics/filters \
    $$SOURCE_DIR/WebCore/svg/properties \
    $$SOURCE_DIR/WebCore/testing \
    $$SOURCE_DIR/WebCore/workers \
    $$SOURCE_DIR/WebCore/xml \
    $$SOURCE_DIR/WebCore/xml/parser \
    $$SOURCE_DIR/WebCore/Modules/battery \
    $$SOURCE_DIR/WebCore/Modules/gamepad \
    $$SOURCE_DIR/WebCore/Modules/geolocation \
    $$SOURCE_DIR/WebCore/Modules/indexeddb \
    $$SOURCE_DIR/WebCore/Modules/mediastream \
    $$SOURCE_DIR/WebCore/Modules/mediasource \
    $$SOURCE_DIR/WebCore/Modules/networkinfo \
    $$SOURCE_DIR/WebCore/Modules/notifications \
    $$SOURCE_DIR/WebCore/Modules/quota \
    $$SOURCE_DIR/WebCore/Modules/speech \
    $$SOURCE_DIR/WebCore/Modules/vibration \
    $$SOURCE_DIR/WebCore/Modules/webdatabase \
    $$SOURCE_DIR/WebCore/Modules/websockets \
    $$SOURCE_DIR/WebCore/Modules/plugins \
    $$SOURCE_DIR/JavaScriptCore \
    $$SOURCE_DIR/JavaScriptCore/assembler \
    $$SOURCE_DIR/JavaScriptCore/API \
    $$SOURCE_DIR/JavaScriptCore/bindings \
    $$SOURCE_DIR/JavaScriptCore/bytecode \
    $$SOURCE_DIR/JavaScriptCore/dfg \
    $$SOURCE_DIR/JavaScriptCore/disassembler \
    $$SOURCE_DIR/JavaScriptCore/ForwardingHeaders \
    $$SOURCE_DIR/JavaScriptCore/heap \
    $$SOURCE_DIR/JavaScriptCore/interpreter \
    $$SOURCE_DIR/JavaScriptCore/inspector \
    $$SOURCE_DIR/JavaScriptCore/jit \
    $$SOURCE_DIR/JavaScriptCore/llint \
    $$SOURCE_DIR/JavaScriptCore/parser \
    $$SOURCE_DIR/JavaScriptCore/profiler \
    $$SOURCE_DIR/JavaScriptCore/runtime \
    $$SOURCE_DIR/JavaScriptCore/yarr \
    $$SOURCE_DIR/WTF \
    $$SOURCE_DIR/WTF/wtf \
    $$SOURCE_DIR/WTF/wtf/text \
    $$SOURCE_DIR/WTF/wtf/java \
    $$SOURCE_DIR/WTF/wtf/unicode/java \
    $$SOURCE_DIR/../WebKitLibraries/zlib/include \
    ../../../generated-src/headers \
    ../JavaScriptCore \
    ../JavaScriptCore/generated

    


!contains(DEFINES, IMAGEIO=1) {
INCLUDEPATH += \
    $$PWD/platform/image-decoders/bmp \
    $$PWD/platform/image-decoders/gif \
    $$PWD/platform/image-decoders/ico \
    $$PWD/platform/image-decoders/jpeg \
    $$PWD/platform/image-decoders/png \
    $$PWD/platform/image-decoders/webp
}

# INCLUDEPATH += $(WEBKIT_OUTPUTDIR)/import/include # defined in WebKitJava.pri
LIBS += -L$(WEBKIT_OUTPUTDIR)/import/lib -lsqlite3

# The following line ensures that __STDC_FORMAT_MACROS is defined when
# <inttypes.h> is included in the precompiled header. Note that the file
# that actually requires <inttypes.h>'s format macros, SQLiteFileSystem.cpp,
# does include <inttypes.h> and define __STDC_FORMAT_MACROS prior to that.
# The problem is that with certain versions of GCC the SQLiteFileSystem.cpp's
# include has no effect because <inttypes.h> is already included in the
# precompiled header.
QMAKE_CXXFLAGS += -D__STDC_FORMAT_MACROS

win32-* {
    QMAKE_CXXFLAGS += -DLIBXML_STATIC
    # Adds version information created by Gradle build, see RT-27943
    OBJECTS += $$OBJECTS_DIR/*.res
    LIBS += -llibxml2_a -lole32 -ladvapi32 -luser32
    CONFIG(release, debug|release) {
        LIBS += -lsicuuc -lsicuin
    } else {
        LIBS += -lsicuucd -lsicuind
    }
    LIBS += -licudt

    contains(DEFINES, ENABLE_XSLT=1) {
        QMAKE_CXXFLAGS += -DLIBXSLT_STATIC
        LIBS += -llibxslt_a
    }
    !contains(DEFINES, IMAGEIO=1) {
        INCLUDEPATH += $(WEBKIT_OUTPUTDIR)/import/include/image
        LIBS += -llibjpeg -llibpng -lzlib
        QMAKE_LFLAGS += /NODEFAULTLIB:LIBCMTD /NODEFAULTLIB:LIBCMT
    }
# TODO: propagate the fix to Mac OS & Linux
# http://en.wikipedia.org/wiki/Relocation_%28computer_science%29
    QMAKE_LFLAGS += /BASE:0x6F000000 /DEF:$$PWD/WebCoreJava.def
# TODO: remove debug!
#    QMAKE_CFLAGS += -Zi -Od
#    QMAKE_CXXFLAGS += -Zi -Od
}

linux-*|solaris-* {
    CONFIG += link_pkgconfig
    PKGCONFIG += libxml-2.0

    LIBS += -lxml2
    contains(DEFINES, ENABLE_XSLT=1) {
        LIBS += -lxslt
    }
    !contains(DEFINES, IMAGEIO=1) {
        LIBS += -ljpeg -lpng
    }

    linux-*|solaris-g++* {
        QMAKE_LFLAGS += -Xlinker -version-script=$$PWD/mapfile-vers

        # just for build debug: force verboce output from linker 
        QMAKE_LFLAGS +=  -Wl,--verbose

        # statically link with icu libraries in order to avoid version conflict
        QMAKE_LFLAGS += `pkg-config --libs-only-L icu-uc`
        LIBS += -Wl,-Bstatic -licui18n -licuuc -licudata -Wl,-Bdynamic
    }
    solaris-cc {
        QMAKE_LFLAGS += -M$$PWD/mapfile-vers
    }
}

mac* {
    INCLUDEPATH += /usr/include/libxml2 \
                   $$SOURCE_DIR/WebCore/platform/mac
    LIBS += -lc++ -lxml2 -lobjc -framework AppKit
    contains(DEFINES, ENABLE_XSLT=1) {
        LIBS += -lxslt
    }
    contains(DEFINES, ICU_UNICODE=1) {
        LIBS += -licucore
    }
    !contains(DEFINES, IMAGEIO=1) {
        INCLUDEPATH += /usr/X11/include \
                       $(WEBKIT_OUTPUTDIR)/import/include
        LIBS += -L/usr/X11/lib -L$(WEBKIT_OUTPUTDIR)/import/lib
        LIBS += -ljpeg -lpng
    }
    SOURCES +=  \
        platform/mac/PurgeableBufferMac.cpp \
#	platform/cf/FileSystemCF.cpp \
	platform/cf/SharedBufferCF.cpp \
        platform/cf/URLCF.cpp \
        platform/cf/CFURLExtras.cpp \
#	platform/text/cf/HyphenationCF.cpp \

    QMAKE_LFLAGS += -exported_symbols_list $$PWD/mapfile-macosx
}

win32-* {
    POST_TARGETDEPS += ../lib/JavaScriptCoreJava.lib
}
linux-*|solaris-*|mac* {
    POST_TARGETDEPS += ../lib/libJavaScriptCoreJava.a
}
LIBS += -lJavaScriptCoreJava

HEADERS += \
    bindings/java/JavaDOMUtils.h \
    bindings/java/JavaEventListener.h \
    page/java/ChromeClientJava.h \
    platform/java/ClipboardJava.h \
    platform/java/ClipboardUtilitiesJava.h \
    platform/java/DataObjectJava.h \
    platform/java/ContextMenuClientJava.h \
    platform/java/DragClientJava.h \
    platform/java/EditorClientJava.h \
    platform/graphics/java/FontPlatformData.h \
    platform/java/FrameLoaderClientJava.h \
    platform/java/IDNJava.h \
    platform/java/InspectorClientJava.h \
    platform/java/JavaEnv.h \
    platform/java/NotificationClientJava.h \
    platform/java/PlatformStrategiesJava.h \
    platform/java/RenderThemeJava.h \
    platform/java/ScrollbarThemeJava.h \
    platform/java/StringJava.h \
    platform/java/WebPage.h \
    platform/network/ResourceRequestBase.h \
    platform/network/ResourceResponseBase.h \
    platform/network/java/ResourceResponse.h \
    platform/network/java/ResourceRequest.h \
    platform/network/java/URLLoader.h \
    platform/graphics/java/PlatformContextJava.h \
    platform/graphics/java/ImageBufferDataJava.h \
    platform/graphics/GraphicsContext.h \
    platform/graphics/java/GraphicsContextJava.h \
    platform/graphics/java/RenderingQueue.h \
    platform/graphics/java/RQRef.h \
    platform/graphics/java/ChromiumBridge.h \

!contains(DEFINES, ICU_UNICODE=1) {
    HEADERS += \
        platform/java/TextCodecJava.h \
        platform/java/TextNormalizerJava.h
}

SOURCES += \
    dom/QualifiedName.cpp \
    accessibility/AccessibilityImageMapLink.cpp \
    accessibility/AccessibilityMediaControls.cpp \
    accessibility/AccessibilityMenuList.cpp \
    accessibility/AccessibilityMenuListOption.cpp \
    accessibility/AccessibilityMenuListPopup.cpp \
    accessibility/AccessibilityMockObject.cpp \
    accessibility/AccessibilityObject.cpp \
    accessibility/AccessibilityList.cpp \
    accessibility/AccessibilityListBox.cpp \
    accessibility/AccessibilityListBoxOption.cpp \
    accessibility/AccessibilityNodeObject.cpp \
    accessibility/AccessibilityProgressIndicator.cpp \
    accessibility/AccessibilityRenderObject.cpp \
    accessibility/AccessibilityScrollbar.cpp \
    accessibility/AccessibilityScrollView.cpp \
    accessibility/AccessibilitySlider.cpp \
    accessibility/AccessibilitySpinButton.cpp \
    accessibility/AccessibilityARIAGrid.cpp \
    accessibility/AccessibilityARIAGridCell.cpp \
    accessibility/AccessibilityARIAGridRow.cpp \
    accessibility/AccessibilityTable.cpp \
    accessibility/AccessibilityTableCell.cpp \
    accessibility/AccessibilityTableColumn.cpp \
    accessibility/AccessibilityTableHeaderContainer.cpp \
    accessibility/AccessibilityTableRow.cpp \
    accessibility/AXObjectCache.cpp \
    bindings/generic/ActiveDOMCallback.cpp \
    bindings/generic/RuntimeEnabledFeatures.cpp \
    bindings/java/JavaDOMUtils.cpp \
    bindings/java/JavaEventListener.cpp \
    bindings/js/ArrayValue.cpp \
    bindings/js/CallbackFunction.cpp \
    bindings/js/DOMObjectHashTableMap.cpp \
    bindings/js/DOMWrapperWorld.cpp \
    bindings/js/Dictionary.cpp \
    bindings/js/GCController.cpp \
    bindings/js/JSAttrCustom.cpp \
    bindings/js/JSBlobCustom.cpp \
    bindings/js/JSCDATASectionCustom.cpp \
    bindings/js/JSCSSRuleCustom.cpp \
    bindings/js/JSCSSRuleListCustom.cpp \
    bindings/js/JSCSSStyleDeclarationCustom.cpp \
    bindings/js/JSCSSValueCustom.cpp \
    bindings/js/JSCallbackData.cpp \
    bindings/js/JSCanvasRenderingContext2DCustom.cpp \
    bindings/js/JSCanvasRenderingContextCustom.cpp \
    bindings/js/JSClipboardCustom.cpp \
    bindings/js/JSCryptoCustom.cpp \
    bindings/js/JSCustomXPathNSResolver.cpp \
    bindings/js/JSDictionary.cpp \
    bindings/js/JSDOMBinding.cpp \
    bindings/js/JSDOMFormDataCustom.cpp \
    bindings/js/JSDOMGlobalObject.cpp \
    bindings/js/JSDOMGlobalObjectTask.cpp \
    bindings/js/JSDOMMimeTypeArrayCustom.cpp \
    bindings/js/JSDOMPluginArrayCustom.cpp \
    bindings/js/JSDOMPluginCustom.cpp \
    bindings/js/JSDOMStringListCustom.cpp \
    bindings/js/JSDOMStringMapCustom.cpp \
    bindings/js/JSDOMWindowBase.cpp \
    bindings/js/JSDOMWindowCustom.cpp \
    bindings/js/JSDOMWindowShell.cpp \
    bindings/js/JSDOMWrapper.cpp \
    bindings/js/JSCommandLineAPIHostCustom.cpp \
    bindings/js/JSDeviceMotionEventCustom.cpp \
    bindings/js/JSDeviceOrientationEventCustom.cpp \
    bindings/js/JSDocumentCustom.cpp \
    bindings/js/JSElementCustom.cpp \
    bindings/js/JSErrorHandler.cpp \
    bindings/js/JSEventCustom.cpp \
    bindings/js/JSEventListener.cpp \
    bindings/js/JSEventTargetCustom.cpp \
    bindings/js/JSExceptionBase.cpp \
    bindings/js/JSFileReaderCustom.cpp \
    bindings/js/JSGeolocationCustom.cpp \
    bindings/js/JSHTMLAllCollectionCustom.cpp \
    bindings/js/JSHTMLAppletElementCustom.cpp \
    bindings/js/JSHTMLCanvasElementCustom.cpp \
    bindings/js/JSHTMLCollectionCustom.cpp \
    bindings/js/JSHTMLDocumentCustom.cpp \
    bindings/js/JSHTMLElementCustom.cpp \
    bindings/js/JSHTMLEmbedElementCustom.cpp \
    bindings/js/JSHTMLFormControlsCollectionCustom.cpp \
    bindings/js/JSHTMLFormElementCustom.cpp \
    bindings/js/JSHTMLFrameElementCustom.cpp \
    bindings/js/JSHTMLFrameSetElementCustom.cpp \
    bindings/js/JSHTMLInputElementCustom.cpp \
    bindings/js/JSHTMLLinkElementCustom.cpp \
    bindings/js/JSHTMLMediaElementCustom.cpp \
    bindings/js/JSHTMLObjectElementCustom.cpp \
    bindings/js/JSHTMLOptionsCollectionCustom.cpp \
    bindings/js/JSHTMLSelectElementCustom.cpp \
    bindings/js/JSHTMLTemplateElementCustom.cpp \
    bindings/js/JSHistoryCustom.cpp \
    bindings/js/JSImageConstructor.cpp \
    bindings/js/JSImageDataCustom.cpp \
    bindings/js/JSInspectorFrontendHostCustom.cpp \
    bindings/js/JSLazyEventListener.cpp \
    bindings/js/JSLocationCustom.cpp \
    bindings/js/JSMainThreadExecState.cpp \
    bindings/js/JSMessageChannelCustom.cpp \
    bindings/js/JSMessageEventCustom.cpp \
    bindings/js/JSMessagePortCustom.cpp \
    bindings/js/JSMessagePortCustom.h \
    bindings/js/JSMutationCallback.cpp \
    bindings/js/JSMutationObserverCustom.cpp \
    bindings/js/JSNamedNodeMapCustom.cpp \
    bindings/js/JSNodeCustom.cpp \
    bindings/js/JSNodeFilterCondition.cpp \
    bindings/js/JSNodeFilterCustom.cpp \
    bindings/js/JSNodeIteratorCustom.cpp \
    bindings/js/JSNodeListCustom.cpp \
    bindings/js/JSPluginElementFunctions.cpp \
    bindings/js/JSPopStateEventCustom.cpp \
    bindings/js/JSRequestAnimationFrameCallbackCustom.cpp \
    bindings/js/JSRTCStatsResponseCustom.cpp \
    bindings/js/JSStorageCustom.cpp \
    bindings/js/JSStyleSheetCustom.cpp \
    bindings/js/JSStyleSheetListCustom.cpp \
    bindings/js/JSTextCustom.cpp \
    bindings/js/JSTouchCustom.cpp \
    bindings/js/JSTouchListCustom.cpp \
    bindings/js/JSTreeWalkerCustom.cpp \
    bindings/js/JSTrackCustom.cpp \
    bindings/js/JSWebKitPointCustom.cpp \
    bindings/js/JSWorkerCustom.cpp \
    bindings/js/JSDedicatedWorkerGlobalScopeCustom.cpp \
    bindings/js/JSXMLHttpRequestCustom.cpp \
    bindings/js/JSXPathResultCustom.cpp \
    bindings/js/PageScriptDebugServer.cpp \
    bindings/js/ScheduledAction.cpp \
    bindings/js/ScriptCachedFrameData.cpp \
    bindings/js/ScriptController.cpp \
    bindings/js/ScriptGlobalObject.cpp \
    bindings/js/ScriptProfile.cpp \
    bindings/js/ScriptState.cpp \
    bindings/js/SerializedScriptValue.cpp \
    bindings/js/WebCoreTypedArrayController.cpp \
    bridge/IdentifierRep.cpp \
    bridge/NP_jsobject.cpp \
    bridge/c/CRuntimeObject.cpp \
    bridge/c/c_class.cpp \
    bridge/c/c_instance.cpp \
    bridge/c/c_runtime.cpp \
    bridge/c/c_utility.cpp \
    bridge/jsc/BridgeJSC.cpp \
    bridge/jni/JNIUtility.cpp \
    bridge/jni/JobjectWrapper.cpp \
    bridge/jni/jsc/JavaMethodJSC.cpp \
    bridge/jni/jsc/JavaArrayJSC.cpp \
    bridge/jni/jsc/JavaClassJSC.cpp \
    bridge/jni/jsc/JavaFieldJSC.cpp \
    bridge/jni/jsc/JavaInstanceJSC.cpp \
    bridge/jni/jsc/JavaRuntimeObject.cpp \
    bridge/jni/jsc/JNIUtilityPrivate.cpp \
    bridge/npruntime.cpp \
    bridge/runtime_array.cpp \
    bridge/runtime_method.cpp \
    bridge/runtime_object.cpp \
    bridge/runtime_root.cpp \
#    testing/js/WebCoreTestSupport.cpp \
    Modules/navigatorcontentutils/NavigatorContentUtils.cpp \
    Modules/proximity/DeviceProximityController.cpp \
    Modules/proximity/DeviceProximityEvent.cpp \
    Modules/webdatabase/DatabaseAuthorizer.cpp \
    Modules/websockets/ThreadableWebSocketChannel.cpp \
    Modules/websockets/ThreadableWebSocketChannelClientWrapper.cpp \
    Modules/websockets/WebSocket.cpp \
    Modules/websockets/WebSocketHandshake.cpp \
    Modules/websockets/WebSocketChannel.cpp \
    Modules/websockets/WebSocketDeflateFramer.cpp \
    Modules/websockets/WebSocketExtensionDispatcher.cpp \
    Modules/websockets/WebSocketExtensionParser.cpp \
    Modules/websockets/WebSocketFrame.cpp \
    Modules/websockets/WorkerThreadableWebSocketChannel.cpp \
    Modules/notifications/DOMWindowNotifications.cpp \
    Modules/notifications/Notification.cpp \
    Modules/notifications/NotificationCenter.cpp \ 
    Modules/notifications/NotificationController.cpp \
    Modules/notifications/WorkerGlobalScopeNotifications.cpp \
    css/BasicShapeFunctions.cpp \
    css/CSSAspectRatioValue.cpp \
    css/CSSBasicShapes.cpp \
    css/CSSBorderImageSliceValue.cpp \
    css/CSSBorderImage.cpp \
    css/CSSCalculationValue.cpp \
    css/CSSCanvasValue.cpp \
    css/CSSCharsetRule.cpp \
    css/CSSComputedStyleDeclaration.cpp \
    css/CSSCrossfadeValue.cpp \
    css/CSSCursorImageValue.cpp \
    css/CSSFontFace.cpp \
    css/CSSFontFeatureValue.cpp \
    css/CSSFontValue.cpp \
    css/CSSDefaultStyleSheets.cpp \
    css/CSSFontFaceLoadEvent.cpp \
    css/CSSFontFaceRule.cpp \
    css/CSSFontFaceSrcValue.cpp \
    css/CSSFontSelector.cpp \
    css/CSSFontFaceSource.cpp \
    css/CSSFunctionValue.cpp \
    css/CSSGradientValue.cpp \
    css/CSSGroupingRule.cpp \
    css/CSSGridTemplateAreasValue.cpp \
    css/CSSImageValue.cpp \
    css/CSSImageGeneratorValue.cpp \
    css/CSSImageSetValue.cpp \
    css/CSSImportRule.cpp \
    css/CSSInheritedValue.cpp \
    css/CSSInitialValue.cpp \
    css/CSSLineBoxContainValue.cpp \
    css/CSSMediaRule.cpp \
    css/CSSOMUtils.cpp \
    css/CSSPageRule.cpp \
    css/CSSParser.cpp \
    css/CSSParserValues.cpp \
    css/CSSPrimitiveValue.cpp \
    css/CSSProperty.cpp \
    css/CSSPropertySourceData.cpp \
    css/CSSReflectValue.cpp \
    css/CSSRule.cpp \
    css/CSSRuleList.cpp \
    css/CSSSelector.cpp \
    css/CSSSelectorList.cpp \
    css/CSSSegmentedFontFace.cpp \
    css/CSSStyleRule.cpp \
    css/CSSStyleSheet.cpp \
    css/CSSSupportsRule.cpp \
    css/CSSShadowValue.cpp \
    css/CSSTimingFunctionValue.cpp \
    css/CSSToStyleMap.cpp \
    css/CSSUnicodeRangeValue.cpp \
    css/CSSValue.cpp \
    css/CSSValueList.cpp \
    css/CSSValuePool.cpp \
    css/DOMWindowCSS.cpp \
    css/DeprecatedStyleBuilder.cpp \
    css/DocumentRuleSets.cpp \
    css/ElementRuleCollector.cpp \
    css/FontLoader.cpp \
    css/InspectorCSSOMWrappers.cpp \
    css/LengthFunctions.cpp \
    css/MediaFeatureNames.cpp \
    css/MediaList.cpp \
    css/MediaQuery.cpp \
    css/MediaQueryEvaluator.cpp \
    css/MediaQueryExp.cpp \
    css/MediaQueryList.cpp \
    css/MediaQueryMatcher.cpp \
    css/PageRuleCollector.cpp \
    css/PropertySetCSSStyleDeclaration.cpp \
    css/RGBColor.cpp \
    css/RuleFeature.cpp \
    css/RuleSet.cpp \
    css/SelectorChecker.cpp \
    css/SelectorCheckerFastPath.cpp \
    css/SelectorFilter.cpp \
    css/StyleInvalidationAnalysis.cpp \
    css/StyleMedia.cpp \
    css/StyleProperties.cpp \
    css/StylePropertyShorthand.cpp \
    css/StyleResolver.cpp \
    css/StyleRule.cpp \
    css/StyleRuleImport.cpp \
    css/StyleSheet.cpp \
    css/StyleSheetContents.cpp \
    css/StyleSheetList.cpp \
    css/TransformFunctions.cpp \
    css/ViewportStyleResolver.cpp \
    css/WebKitCSSFilterValue.cpp \
    css/WebKitCSSKeyframeRule.cpp \
    css/WebKitCSSKeyframesRule.cpp \
    css/WebKitCSSMatrix.cpp \
    css/WebKitCSSRegionRule.cpp \
    css/WebKitCSSTransformValue.cpp \
    css/WebKitCSSViewportRule.cpp \
    cssjit/SelectorCompiler.cpp \
    dom/ActiveDOMObject.cpp \
    dom/Attr.cpp \
    dom/BeforeTextInsertedEvent.cpp \
    dom/BeforeUnloadEvent.cpp \
    dom/CDATASection.cpp \
    dom/CharacterData.cpp \
    dom/CheckedRadioButtons.cpp \
    dom/ChildListMutationScope.cpp \
    dom/ChildNodeList.cpp \
    dom/ClassNodeList.cpp \
    dom/ClientRect.cpp \
    dom/ClientRectList.cpp \
    dom/Clipboard.cpp \
    dom/ClipboardEvent.cpp \
    dom/Comment.cpp \
    dom/CompositionEvent.cpp \
    dom/ContainerNode.cpp \
    dom/ContainerNodeAlgorithms.cpp \
    dom/ContextDestructionObserver.cpp \
    dom/CustomEvent.cpp \
    dom/DecodedDataDocumentParser.cpp \
    dom/DeviceMotionController.cpp \
    dom/DeviceMotionData.cpp \
    dom/DeviceMotionEvent.cpp \
    dom/DeviceOrientationController.cpp \
    dom/DeviceOrientationData.cpp \
    dom/DeviceOrientationEvent.cpp \
    dom/Document.cpp \
    dom/DocumentEventQueue.cpp \
    dom/DocumentFragment.cpp \
    dom/DocumentMarkerController.cpp \
    dom/DocumentMarker.cpp \
    dom/DocumentOrderedMap.cpp \
    dom/DocumentParser.cpp \
    dom/DocumentSharedObjectPool.cpp \
    dom/DocumentStyleSheetCollection.cpp \
    dom/DocumentType.cpp \
    dom/DOMCoreException.cpp \
    dom/DOMError.cpp \
    dom/DOMImplementation.cpp \
    dom/DOMNamedFlowCollection.cpp \
    dom/DOMStringList.cpp \
    dom/DatasetDOMStringMap.cpp \
    dom/Element.cpp \
    dom/ElementData.cpp \
    dom/ElementRareData.cpp \
    dom/EntityReference.cpp \
    dom/ErrorEvent.cpp \
    dom/Event.cpp \
    dom/EventContext.cpp \
    dom/EventDispatcher.cpp \
    dom/EventException.cpp \
    dom/EventListenerMap.cpp \
    dom/EventNames.cpp \
    dom/EventTarget.cpp \
    dom/ExceptionBase.cpp \
    dom/ExceptionCodePlaceholder.cpp \
    dom/FocusEvent.cpp \
    dom/GenericEventQueue.cpp \
    dom/IconURL.cpp \
    dom/IdTargetObserver.cpp \
    dom/IdTargetObserverRegistry.cpp \
    dom/InlineStyleSheetOwner.cpp \
    dom/LiveNodeList.cpp \
    dom/KeyboardEvent.cpp \
    dom/MessageChannel.cpp \
    dom/MessageEvent.cpp \
    dom/MessagePort.cpp \
    dom/MessagePortChannel.cpp \
    dom/MouseEvent.cpp \
    dom/MouseRelatedEvent.cpp \
    dom/MutationEvent.cpp \
    dom/MutationObserver.cpp \
    dom/MutationObserverInterestGroup.cpp \
    dom/MutationObserverRegistration.cpp \
    dom/MutationRecord.cpp \
    dom/WebKitNamedFlow.cpp \
    dom/NamedFlowCollection.cpp \
    dom/NamedNodeMap.cpp \
    dom/NameNodeList.cpp \
    dom/Node.cpp \
    dom/NodeFilterCondition.cpp \
    dom/NodeFilter.cpp \
    dom/NodeIterator.cpp \
    dom/NodeRareData.cpp \
    dom/NodeRenderingTraversal.cpp \
    dom/NodeTraversal.cpp \
    dom/Notation.cpp \
    dom/OverflowEvent.cpp \
    dom/PageTransitionEvent.cpp \
    dom/PendingScript.cpp \
    dom/PopStateEvent.cpp \
    dom/Position.cpp \
    dom/PositionIterator.cpp \
    dom/ProcessingInstruction.cpp \
    dom/ProgressEvent.cpp \
    dom/PseudoElement.cpp \
#    dom/QualifiedName.cpp \
    dom/Range.cpp \
    dom/RangeException.cpp \
    dom/RawDataDocumentParser.h \
    dom/RegisteredEventListener.cpp \
    dom/ScopedEventQueue.cpp \
    dom/ScriptedAnimationController.cpp \
    dom/ScriptableDocumentParser.cpp \
    dom/ScriptElement.cpp \
    dom/ScriptExecutionContext.cpp \
    dom/ScriptRunner.cpp \
    dom/SecurityContext.cpp \
    dom/SelectorQuery.cpp \
    dom/ShadowRoot.cpp \
    dom/SpaceSplitString.cpp \
    dom/StaticNodeList.cpp \
    dom/StyledElement.cpp \
    dom/TagNodeList.cpp \
    dom/Text.cpp \
    dom/TextEvent.cpp \
    dom/TextNodeTraversal.cpp \
    dom/Touch.cpp \
    dom/TouchEvent.cpp \
    dom/TouchList.cpp \
    dom/TransitionEvent.cpp \
    dom/Traversal.cpp \
    dom/TreeScope.cpp \
    dom/TreeScopeAdopter.cpp \
    dom/TreeWalker.cpp \
    dom/UIEvent.cpp \
    dom/UIEventWithKeyState.cpp \
    dom/UserActionElementSet.cpp \
    dom/UserGestureIndicator.cpp \
    dom/UserTypingGestureIndicator.cpp \
    dom/ViewportArguments.cpp \
    dom/VisitedLinkState.cpp \
    dom/WebKitAnimationEvent.cpp \
    dom/WebKitTransitionEvent.cpp \
    dom/WheelEvent.cpp \
    dom/default/PlatformMessagePortChannel.cpp \
    editing/AlternativeTextController.cpp \
    editing/AppendNodeCommand.cpp \
    editing/ApplyBlockElementCommand.cpp \
    editing/ApplyStyleCommand.cpp \
    editing/BreakBlockquoteCommand.cpp \
    editing/CompositeEditCommand.cpp \
    editing/CreateLinkCommand.cpp \
    editing/DeleteButtonController.cpp \
    editing/DeleteButton.cpp \
    editing/DeleteFromTextNodeCommand.cpp \
    editing/DeleteSelectionCommand.cpp \
    editing/DictationAlternative.cpp \
    editing/DictationCommand.cpp \
    editing/EditCommand.cpp \
    editing/EditingStyle.cpp \
    editing/Editor.cpp \
    editing/EditorCommand.cpp \
    editing/FormatBlockCommand.cpp \
    editing/FrameSelection.cpp \
    editing/htmlediting.cpp \
    editing/HTMLInterchange.cpp \
    editing/IndentOutdentCommand.cpp \
    editing/InsertIntoTextNodeCommand.cpp \
    editing/InsertLineBreakCommand.cpp \
    editing/InsertListCommand.cpp \
    editing/InsertNodeBeforeCommand.cpp \
    editing/InsertParagraphSeparatorCommand.cpp \
    editing/InsertTextCommand.cpp \
    editing/markup.cpp \
    editing/MarkupAccumulator.cpp \
    editing/MergeIdenticalElementsCommand.cpp \
    editing/ModifySelectionListLevel.cpp \
    editing/MoveSelectionCommand.cpp \
    editing/RemoveCSSPropertyCommand.cpp \
    editing/RemoveFormatCommand.cpp \
    editing/RemoveNodeCommand.cpp \
    editing/RemoveNodePreservingChildrenCommand.cpp \
    editing/RenderedPosition.cpp \
    editing/ReplaceNodeWithSpanCommand.cpp \
    editing/ReplaceSelectionCommand.cpp \
    editing/SetNodeAttributeCommand.cpp \
    editing/SetSelectionCommand.cpp \
    editing/SimplifyMarkupCommand.cpp \
    editing/SpellChecker.cpp \
    editing/SpellingCorrectionCommand.cpp \
    editing/SplitElementCommand.cpp \
    editing/SplitTextNodeCommand.cpp \
    editing/SplitTextNodeContainingElementCommand.cpp \
    editing/SmartReplace.cpp \
    editing/TextCheckingHelper.cpp \
    editing/TextInsertionBaseCommand.cpp \
    editing/TextIterator.cpp \
    editing/TypingCommand.cpp \
    editing/UnlinkCommand.cpp \
    editing/VisiblePosition.cpp \
    editing/VisibleSelection.cpp \
    editing/VisibleUnits.cpp \
    editing/WrapContentsInDummySpanCommand.cpp \
    fileapi/AsyncFileStream.cpp \
    fileapi/Blob.cpp \
    fileapi/BlobURL.cpp \
    fileapi/File.cpp \
    fileapi/FileException.cpp \
    fileapi/FileList.cpp \
    fileapi/FileReader.cpp \
    fileapi/FileReaderLoader.cpp \
    fileapi/FileReaderSync.cpp \
    fileapi/FileThread.cpp \
    fileapi/ThreadableBlobRegistry.cpp \
    fileapi/WebKitBlobBuilder.cpp \
    history/BackForwardController.cpp \
    history/BackForwardList.cpp \
    history/CachedFrame.cpp \
    history/CachedPage.cpp \
    history/HistoryItem.cpp \
    history/PageCache.cpp \
    html/BaseButtonInputType.cpp \
    html/BaseCheckableInputType.cpp \
    html/BaseChooserOnlyDateAndTimeInputType.cpp \
    html/BaseClickableWithKeyInputType.cpp \
    html/BaseDateAndTimeInputType.cpp \
    html/BaseTextInputType.cpp \
    html/ButtonInputType.cpp \
    html/CheckboxInputType.cpp \
    html/ClassList.cpp \
    html/ColorInputType.cpp \
    html/DOMFormData.cpp \
    html/DOMSettableTokenList.cpp \
    html/DOMTokenList.cpp \
    html/DOMURL.cpp \
    html/DateInputType.cpp \
    html/DateTimeInputType.cpp \
    html/DateTimeLocalInputType.cpp \
    html/EmailInputType.cpp \
    html/FTPDirectoryDocument.cpp \
    html/FileInputType.cpp \
    html/FormAssociatedElement.cpp \
    html/FormController.cpp \
    html/FormDataList.cpp \
    html/HTMLAllCollection.cpp \
    html/HTMLAnchorElement.cpp \
    html/HTMLAppletElement.cpp \
    html/HTMLAreaElement.cpp \
    html/HTMLBRElement.cpp \
    html/HTMLBaseElement.cpp \
    html/HTMLBaseFontElement.cpp \
    html/HTMLBodyElement.cpp \
    html/HTMLButtonElement.cpp \
    html/HTMLCanvasElement.cpp \
    html/HTMLCollection.cpp \
    html/HTMLDListElement.cpp \
    html/HTMLDataListElement.cpp \
    html/HTMLDirectoryElement.cpp \
    html/HTMLDetailsElement.cpp \
    html/HTMLDivElement.cpp \
    html/HTMLDocument.cpp \
    html/HTMLElement.cpp \
    html/HTMLEmbedElement.cpp \
    html/HTMLFieldSetElement.cpp \
    html/HTMLFontElement.cpp \
    html/HTMLFormControlsCollection.cpp \
    html/HTMLFormControlElement.cpp \
    html/HTMLFormControlElementWithState.cpp \
    html/HTMLFormElement.cpp \
    html/HTMLFrameElement.cpp \
    html/HTMLFrameElementBase.cpp \
    html/HTMLFrameOwnerElement.cpp \
    html/HTMLFrameSetElement.cpp \
    html/HTMLHRElement.cpp \
    html/HTMLHeadElement.cpp \
    html/HTMLHeadingElement.cpp \
    html/HTMLHtmlElement.cpp \
    html/HTMLIFrameElement.cpp \
    html/HTMLImageElement.cpp \
    html/HTMLImageLoader.cpp \
    html/HTMLInputElement.cpp \
    html/HTMLKeygenElement.cpp \
    html/HTMLLIElement.cpp \
    html/HTMLLabelElement.cpp \
    html/HTMLLegendElement.cpp \
    html/HTMLLinkElement.cpp \
    html/HTMLMapElement.cpp \
    html/HTMLMarqueeElement.cpp \
    html/HTMLMenuElement.cpp \
    html/HTMLMetaElement.cpp \
    html/HTMLMeterElement.cpp \
    html/HTMLModElement.cpp \
    html/HTMLNameCollection.cpp \
    html/HTMLOListElement.cpp \
    html/HTMLObjectElement.cpp \
    html/HTMLOptGroupElement.cpp \
    html/HTMLOptionElement.cpp \
    html/HTMLOptionsCollection.cpp \
    html/HTMLOutputElement.cpp \
    html/HTMLParagraphElement.cpp \
    html/HTMLParamElement.cpp \
    html/HTMLPlugInElement.cpp \
    html/HTMLPlugInImageElement.cpp \
    html/HTMLPreElement.cpp \
    html/HTMLProgressElement.cpp \
    html/HTMLQuoteElement.cpp \
    html/HTMLScriptElement.cpp \
    html/HTMLSelectElement.cpp \
    html/HTMLSpanElement.cpp \
    html/HTMLStyleElement.cpp \
    html/HTMLSummaryElement.cpp \
    html/HTMLTableCaptionElement.cpp \
    html/HTMLTableCellElement.cpp \
    html/HTMLTableColElement.cpp \
    html/HTMLTableElement.cpp \
    html/HTMLTablePartElement.cpp \
    html/HTMLTableRowElement.cpp \
    html/HTMLTableRowsCollection.cpp \
    html/HTMLTableSectionElement.cpp \
    html/HTMLTemplateElement.cpp \
    html/HTMLTextAreaElement.cpp \
    html/HTMLTextFormControlElement.cpp \
    html/HTMLTitleElement.cpp \
    html/HTMLUListElement.cpp \
    html/HiddenInputType.cpp \
    html/ImageData.cpp \
    html/ImageDocument.cpp \
    html/ImageInputType.cpp \
    html/InputType.cpp \
    html/InputTypeNames.cpp \
    html/LabelableElement.cpp \
    html/LabelsNodeList.cpp \
    html/LinkRelAttribute.cpp \
    html/MediaDocument.cpp \
    html/MonthInputType.cpp \
    html/NumberInputType.cpp \
    html/PasswordInputType.cpp \
    html/PluginDocument.cpp \
    html/RadioInputType.cpp \
    html/RadioNodeList.cpp \
    html/RangeInputType.cpp \
    html/ResetInputType.cpp \
    html/SearchInputType.cpp \
    html/StepRange.cpp \
    html/SubmitInputType.cpp \
    html/TelephoneInputType.cpp \
    html/TextDocument.cpp \
    html/TextFieldInputType.cpp \
    html/TextInputType.cpp \
    html/TimeInputType.cpp \
    html/TypeAhead.cpp \
    html/URLInputType.cpp \
    html/ValidationMessage.cpp \
    html/WeekInputType.cpp \
    html/canvas/CanvasGradient.cpp \
    html/canvas/CanvasPathMethods.cpp \
    html/canvas/CanvasPattern.cpp \
    html/canvas/CanvasProxy.cpp \
    html/canvas/CanvasRenderingContext.cpp \
    html/canvas/CanvasRenderingContext2D.cpp \
    html/canvas/CanvasStyle.cpp \
    html/parser/CSSPreloadScanner.cpp \
    html/parser/HTMLConstructionSite.cpp \
    html/parser/HTMLDocumentParser.cpp \
    html/parser/HTMLElementStack.cpp \
    html/parser/HTMLEntityParser.cpp \
    html/parser/HTMLEntitySearch.cpp \
    html/parser/HTMLFormattingElementList.cpp \
    html/parser/HTMLMetaCharsetParser.cpp \
    html/parser/HTMLParserIdioms.cpp \
    html/parser/HTMLParserOptions.cpp \
    html/parser/HTMLParserScheduler.cpp \
    html/parser/HTMLPreloadScanner.cpp \
    html/parser/HTMLResourcePreloader.cpp \
    html/parser/HTMLScriptRunner.cpp \
    html/parser/HTMLSourceTracker.cpp \
    html/parser/HTMLTokenizer.cpp \
    html/parser/HTMLTreeBuilder.cpp \
    html/parser/TextDocumentParser.cpp \
    html/parser/XSSAuditor.cpp \
    html/parser/XSSAuditorDelegate.cpp \
    html/shadow/ContentDistributor.cpp \
    html/shadow/DetailsMarkerControl.cpp \
    html/shadow/InsertionPoint.cpp \
    html/shadow/MediaControls.cpp \
    html/shadow/MediaControlsApple.cpp \
    html/shadow/MeterShadowElement.cpp \
    html/shadow/ProgressShadowElement.cpp \
    html/shadow/SliderThumbElement.cpp \
    html/shadow/SpinButtonElement.cpp \
    html/shadow/TextControlInnerElements.cpp \
    html/forms/FileIconLoader.cpp \
    inspector/CommandLineAPIHost.cpp \
    inspector/CommandLineAPIModule.cpp \
    inspector/DOMEditor.cpp \
    inspector/DOMPatchSupport.cpp \
    inspector/InjectedScriptCanvasModule.cpp \
    inspector/InspectorApplicationCacheAgent.cpp \
    inspector/InspectorCSSAgent.cpp \
    inspector/InspectorCanvasAgent.cpp \
    inspector/InspectorClient.cpp \
    inspector/InspectorController.cpp \
    inspector/InspectorCounters.cpp \
    inspector/InspectorDatabaseAgent.cpp \
    inspector/InspectorDatabaseResource.cpp \
    inspector/InspectorDOMAgent.cpp \
    inspector/InspectorDOMDebuggerAgent.cpp \
    inspector/InspectorDOMStorageAgent.cpp \
    inspector/InspectorFrontendClientLocal.cpp \
    inspector/InspectorFrontendHost.cpp \
    inspector/InspectorNodeFinder.cpp \
    inspector/InspectorHeapProfilerAgent.cpp \
    inspector/InspectorHistory.cpp \
    inspector/InspectorInputAgent.cpp \
    inspector/InspectorInstrumentation.cpp \
    inspector/InspectorInstrumentationCookie.cpp \
    inspector/InspectorLayerTreeAgent.cpp \
    inspector/InspectorMemoryAgent.cpp \
    inspector/InspectorOverlay.cpp \
    inspector/InspectorPageAgent.cpp \
    inspector/InspectorProfilerAgent.cpp \
    inspector/InspectorResourceAgent.cpp \
    inspector/InspectorStyleSheet.cpp \
    inspector/InspectorStyleTextEditor.cpp \
    inspector/InspectorTimelineAgent.cpp \
    inspector/InspectorWorkerAgent.cpp \
    inspector/InstrumentingAgents.cpp \
    inspector/NetworkResourcesData.cpp \
    inspector/PageConsoleAgent.cpp \
    inspector/PageDebuggerAgent.cpp \
    inspector/PageRuntimeAgent.cpp \
    inspector/TimelineRecordFactory.cpp \
    inspector/WebInjectedScriptManager.cpp \
    inspector/WebInjectedScriptHost.cpp \
    inspector/WebConsoleAgent.cpp \
    inspector/WebDebuggerAgent.cpp \
    inspector/WorkerConsoleAgent.cpp \
    inspector/WorkerDebuggerAgent.cpp \
    inspector/WorkerInspectorController.cpp \
    inspector/WorkerRuntimeAgent.cpp \
#{JAVA 
    loader/archive/Archive.cpp \
    loader/archive/ArchiveFactory.cpp \
    loader/archive/ArchiveResource.cpp \
    loader/archive/ArchiveResourceCollection.cpp \
    loader/archive/mhtml/MHTMLArchive.cpp \
    loader/archive/mhtml/MHTMLParser.cpp \
#}JAVA
    loader/cache/MemoryCache.cpp \
    loader/cache/CachedCSSStyleSheet.cpp \
    loader/cache/CachedFont.cpp \
    loader/cache/CachedImage.cpp \
    loader/cache/CachedRawResource.cpp \
    loader/cache/CachedResourceHandle.cpp \
    loader/cache/CachedResource.cpp \
    loader/cache/CachedScript.cpp \
    loader/cache/CachedSVGDocument.cpp \
    loader/cache/CachedSVGDocumentReference.cpp \
    loader/cache/CachedXSLStyleSheet.cpp \
    loader/CookieJar.cpp \
    loader/CrossOriginAccessControl.cpp \
    loader/CrossOriginPreflightResultCache.cpp \
    loader/cache/CachedResourceLoader.cpp \
    loader/cache/CachedResourceRequest.cpp \
    loader/cache/CachedResourceRequestInitiators.cpp \
    loader/DocumentLoadTiming.cpp \
    loader/DocumentLoader.cpp \
    loader/DocumentThreadableLoader.cpp \
    loader/DocumentWriter.cpp \
    loader/EmptyClients.cpp \
    loader/FormState.cpp \
    loader/FormSubmission.cpp \
    loader/FrameLoadRequest.cpp \
    loader/FrameLoader.cpp \
    loader/FrameLoaderStateMachine.cpp \
    loader/HistoryController.cpp \
    loader/FTPDirectoryParser.cpp \
# conditional compilation
#    loader/icon/IconController.cpp \
#    loader/icon/IconDatabaseBase.cpp \
#    loader/icon/IconLoader.cpp \
    loader/ImageLoader.cpp \
    loader/LinkLoader.cpp \
    loader/LoaderStrategy.cpp \
    loader/MixedContentChecker.cpp \
    loader/NavigationAction.cpp \
    loader/NetscapePlugInStreamLoader.cpp \
    loader/PingLoader.cpp \
    loader/PolicyCallback.cpp \
    loader/PolicyChecker.cpp \
    loader/ProgressTracker.cpp \
    loader/NavigationScheduler.cpp \
    loader/ResourceBuffer.cpp \
    loader/ResourceLoader.cpp \
    loader/ResourceLoadNotifier.cpp \
    loader/ResourceLoadScheduler.cpp \
    loader/SinkDocument.cpp \
    loader/SubframeLoader.cpp \
    loader/SubresourceLoader.cpp \
    loader/TextResourceDecoder.cpp \
    loader/ThreadableLoader.cpp \
    page/animation/AnimationBase.cpp \
    page/animation/AnimationController.cpp \
    page/animation/CompositeAnimation.cpp \
    page/animation/CSSPropertyAnimation.cpp \
    page/animation/ImplicitAnimation.cpp \
    page/animation/KeyframeAnimation.cpp \
    page/AutoscrollController.cpp \
    page/BarProp.cpp \
    page/CaptionUserPreferences.cpp \
    page/Chrome.cpp \
    page/Console.cpp \
    page/ContentSecurityPolicy.cpp \
    page/ContextMenuController.cpp \
    page/Crypto.cpp \
    page/DeviceController.cpp \
    page/DiagnosticLoggingKeys.cpp \
    page/DOMSelection.cpp \
    page/DOMTimer.cpp \
    page/DOMWindow.cpp \
    page/DOMWindowExtension.cpp \
    page/DOMWindowProperty.cpp \
    page/DragController.cpp \
    page/EventHandler.cpp \
    page/EventSource.cpp \
    page/FeatureObserver.cpp \
    page/FocusController.cpp \
    page/Frame.cpp \
    page/FrameDestructionObserver.cpp \
    page/FrameSnapshotting.cpp \
    page/FrameTree.cpp \
    page/FrameView.cpp \
    page/GestureTapHighlighter.cpp \
    page/GroupSettings.cpp \
    page/History.cpp \
    page/Location.cpp \
    page/MainFrame.cpp \
    page/MouseEventWithHitTestResults.cpp \
    page/Navigator.cpp \
    page/NavigatorBase.cpp \
    page/OriginAccessEntry.cpp \
    page/Page.cpp \
    page/PageActivityAssertionToken.cpp \
    page/PageConsole.cpp \
    page/PageGroup.cpp \
    page/PageGroupLoadDeferrer.cpp \
#{JAVA
    page/PageSerializer.cpp \
#}JAVA
    page/PageVisibilityState.cpp \
    page/PageThrottler.cpp \
    page/Performance.cpp \
    page/PerformanceEntry.cpp \
    page/PerformanceEntryList.cpp \
    page/PerformanceNavigation.cpp \
    page/PerformanceResourceTiming.cpp \
    page/PerformanceTiming.cpp \
    page/PrintContext.cpp \
    page/Screen.cpp \
    page/scrolling/ScrollingConstraints.cpp \
    page/scrolling/ScrollingCoordinator.cpp \
    page/SecurityOrigin.cpp \
    page/SecurityPolicy.cpp \
    page/Settings.cpp \
    page/SpatialNavigation.cpp \
    page/SuspendableTimer.cpp \
    page/UserContentURLPattern.cpp \
    page/UserContentController.cpp \
    page/WindowFeatures.cpp \
    page/WindowFocusAllowedIndicator.cpp \
    page/WheelEventDeltaTracker.cpp \
    page/VisitedLinkProvider.cpp \
    page/java/ChromeClientJava.cpp \
    page/java/DragControllerJava.cpp \
    page/java/EventHandlerJava.cpp \
    plugins/PluginData.cpp \
    plugins/DOMPluginArray.cpp \
    plugins/DOMPlugin.cpp \
    plugins/PluginMainThreadScheduler.cpp \
    plugins/DOMMimeType.cpp \
    plugins/DOMMimeTypeArray.cpp \
    platform/animation/Animation.cpp \
    platform/animation/AnimationList.cpp \
    platform/text/BidiContext.cpp \
    platform/text/DateTimeFormat.cpp \
    platform/text/Hyphenation.cpp \
    platform/text/LocaleNone.cpp \
    platform/text/LocaleToScriptMappingDefault.cpp \
    platform/text/PlatformLocale.cpp \
    platform/text/QuotedPrintable.cpp \
    platform/text/icu/UTextProvider.cpp \
    platform/text/icu/UTextProviderUTF16.cpp \
    platform/text/icu/UTextProviderLatin1.cpp \
    platform/CalculationValue.cpp \
    platform/Clock.cpp \
    platform/ClockGeneric.cpp \
    platform/ContentType.cpp \
    platform/CrossThreadCopier.cpp \
    platform/DatabaseStrategy.cpp \
    platform/DateComponents.cpp \
    platform/Decimal.cpp \
    platform/DragData.cpp \
    platform/DragImage.cpp \
    platform/FileChooser.cpp \
    platform/FileStream.cpp \
    platform/FileSystem.cpp \
    platform/HistogramSupport.cpp \
    platform/graphics/FontDescription.cpp \
    platform/graphics/FontGlyphs.cpp \
    platform/graphics/FontFeatureSettings.cpp \
    platform/graphics/FontGenericFamilies.cpp \
    platform/graphics/BitmapImage.cpp \
    platform/graphics/Color.cpp \
    platform/graphics/CrossfadeGeneratedImage.cpp \
    platform/graphics/FloatPoint3D.cpp \
    platform/graphics/FloatPoint.cpp \
    platform/graphics/FloatPolygon.cpp \
    platform/graphics/FloatQuad.cpp \
    platform/graphics/FloatRect.cpp \
    platform/graphics/FloatSize.cpp \
    platform/graphics/FontData.cpp \
    platform/graphics/Font.cpp \
    platform/graphics/FontCache.cpp \
    platform/graphics/FontFastPath.cpp \
    platform/graphics/GeneratedImage.cpp \
    platform/graphics/GlyphPageTreeNode.cpp \
    platform/graphics/Gradient.cpp \
    platform/graphics/GradientImage.cpp \
    platform/graphics/GraphicsContext.cpp \
    platform/graphics/GraphicsLayer.cpp \
    platform/graphics/GraphicsLayerAnimation.cpp \
    platform/graphics/GraphicsLayerUpdater.cpp \
    platform/graphics/GraphicsLayerTransform.cpp \
    platform/graphics/GraphicsTypes.cpp \
    platform/graphics/Image.cpp \
    platform/graphics/ImageBuffer.cpp \
    platform/graphics/ImageOrientation.cpp \
#   platform/graphics/ImageSource.cpp \
    platform/graphics/IntRect.cpp \
    platform/graphics/IntSize.cpp \
    platform/graphics/IntPoint.cpp \
    platform/graphics/Path.cpp \
    platform/graphics/PathTraversalState.cpp \
    platform/graphics/Pattern.cpp \
    platform/graphics/Region.cpp \
    platform/graphics/RoundedRect.cpp \
    platform/graphics/FloatRoundedRect.cpp \
    platform/graphics/LayoutBoxExtent.cpp \
    platform/graphics/LayoutRect.cpp \
    platform/graphics/SegmentedFontData.cpp \
    platform/graphics/ShadowBlur.cpp \
    platform/graphics/SVGGlyph.cpp \
    platform/graphics/SimpleFontData.cpp \
    platform/graphics/StringTruncator.cpp \
    platform/graphics/surfaces/GraphicsSurface.cpp \
    platform/graphics/SurrogatePairAwareTextIterator.cpp \
    platform/graphics/TextRun.cpp \
    platform/graphics/TiledBackingStore.cpp \
    platform/graphics/java/ChromiumBridge.cpp \
    platform/graphics/java/GlyphPageTreeNodeJava.cpp \
    platform/graphics/java/BitmapImageJava.cpp \
    platform/graphics/java/FontCacheJava.cpp \
    platform/graphics/java/FontPlatformDataJava.cpp \
    platform/graphics/java/FontJava.cpp \
    platform/graphics/java/IconJava.cpp \
    platform/graphics/java/ImageBufferJava.cpp \
    platform/graphics/java/ImageJava.cpp \
    platform/graphics/java/GraphicsContextJava.cpp \
    platform/graphics/java/RenderingQueue.cpp \
    platform/graphics/java/RQRef.cpp \
    platform/graphics/transforms/AffineTransform.cpp \
    platform/graphics/transforms/TransformationMatrix.cpp \
    platform/graphics/transforms/MatrixTransformOperation.cpp \
    platform/graphics/transforms/Matrix3DTransformOperation.cpp \
    platform/graphics/transforms/PerspectiveTransformOperation.cpp \
    platform/graphics/transforms/RotateTransformOperation.cpp \
    platform/graphics/transforms/ScaleTransformOperation.cpp \
    platform/graphics/transforms/SkewTransformOperation.cpp \
    platform/graphics/transforms/TransformOperations.cpp \
    platform/graphics/transforms/TransformState.cpp \
    platform/graphics/transforms/TranslateTransformOperation.cpp \
    platform/graphics/WidthIterator.cpp \
    platform/graphics/WOFFFileFormat.cpp \
#   platform/image-decoders/ImageDecoder.cpp \
#   platform/image-decoders/bmp/BMPImageDecoder.cpp \
#   platform/image-decoders/bmp/BMPImageReader.cpp \
#   platform/image-decoders/gif/GIFImageDecoder.cpp \
#   platform/image-decoders/gif/GIFImageReader.cpp\
    platform/KillRingNone.cpp \
    platform/Language.cpp \
    platform/Length.cpp \
    platform/LengthBox.cpp \
    platform/text/LineEnding.cpp \
    platform/leveldb/LevelDBDatabase.cpp \
    platform/leveldb/LevelDBTransaction.cpp \
    platform/leveldb/LevelDBWriteBatch.cpp \
    platform/LinkHash.cpp \
    platform/Logging.cpp \
    platform/MemoryPressureHandler.cpp \
    platform/MIMETypeRegistry.cpp \
    platform/java/BridgeUtils.cpp \
    platform/java/ClipboardJava.cpp \
    platform/java/ClipboardUtilitiesJava.cpp \
    platform/java/ContextMenuClientJava.cpp \
    platform/java/ContextMenuItemJava.cpp \
    platform/java/ContextMenuJava.cpp \
    platform/java/CursorJava.cpp \
    platform/java/DragClientJava.cpp \
    platform/java/DragDataJava.cpp \
    platform/java/EditorClientJava.cpp \
    platform/java/EventLoopJava.cpp \
#   platform/java/FileChooserJava.cpp \
    platform/java/FileSystemJava.cpp \
    platform/java/FrameLoaderClientJava.cpp \
    platform/java/IDNJava.cpp \
    platform/java/InspectorClientJava.cpp \
    platform/java/JavaEnv.cpp \
    platform/java/KeyboardEventJava.cpp \
    platform/java/LanguageJava.cpp \
    platform/java/LocalizedStringsJava.cpp \
    platform/java/LoggingJava.cpp \
    platform/java/MIMETypeRegistryJava.cpp \
    platform/java/MouseEventJava.cpp \
    platform/java/PasteboardJava.cpp \
    platform/java/PlatformScreenJava.cpp \
    platform/java/PlatformStrategiesJava.cpp \
    platform/java/PluginWidgetJava.cpp \
    platform/java/PopupMenuJava.cpp \
    platform/java/RenderThemeJava.cpp \
    platform/java/ScrollbarThemeJava.cpp \
    platform/java/SharedBufferJava.cpp \
    platform/java/SharedTimerJava.cpp \
    platform/java/SoundJava.cpp \
    platform/java/StringJava.cpp \
    platform/java/TemporaryLinkStubsJava.cpp \
    platform/java/TouchEventJava.cpp \
    platform/java/WebPage.cpp \
    platform/java/WheelEventJava.cpp \
    platform/java/WidgetJava.cpp \
    platform/java/api/BackForwardListJava.cpp \
    platform/java/api/PageCacheJava.cpp \
    platform/mock/DeviceMotionClientMock.cpp \
    platform/mock/DeviceOrientationClientMock.cpp \
    platform/mock/GeolocationClientMock.cpp \
    platform/mock/PlatformSpeechSynthesizerMock.cpp \
    platform/mock/ScrollbarThemeMock.cpp \
    platform/network/AuthenticationChallengeBase.cpp \
    platform/network/BlobData.cpp \
    platform/network/BlobRegistry.cpp \
    platform/network/BlobRegistryImpl.cpp \
    platform/network/BlobResourceHandle.cpp \
    platform/network/Credential.cpp \
    platform/network/CredentialStorage.cpp \
    platform/network/FormData.cpp \
    platform/network/FormDataBuilder.cpp \
    platform/network/HTTPHeaderMap.cpp \
    platform/network/HTTPParsers.cpp \
    platform/network/MIMEHeader.cpp \
    platform/network/NetworkStateNotifier.cpp \
    platform/network/NetworkStorageSessionStub.cpp \
    platform/network/ParsedContentType.cpp \
    platform/network/ProtectionSpace.cpp \
    platform/network/ProxyServer.cpp \
    platform/network/ResourceErrorBase.cpp \
    platform/network/ResourceHandle.cpp \
    platform/network/ResourceHandleClient.cpp \
    platform/network/ResourceLoadTiming.cpp \
    platform/network/ResourceRequestBase.cpp \
    platform/network/ResourceResponseBase.cpp \
    platform/network/java/CookieJarJava.cpp \
    platform/network/java/DNSJava.cpp \
    platform/network/java/ResourceHandleJava.cpp \
    platform/network/java/ResourceRequestJava.cpp \
    platform/network/java/URLLoader.cpp \
    platform/NotImplemented.cpp \
    platform/PlatformEvent.cpp \
    platform/RuntimeApplicationChecks.cpp \
    platform/SchemeRegistry.cpp \
    platform/ScrollableArea.cpp \
    platform/ScrollAnimator.cpp \
    platform/Scrollbar.cpp \
    platform/ScrollbarTheme.cpp \
    platform/ScrollbarThemeComposite.cpp \
    platform/ScrollView.cpp \
    platform/SharedBuffer.cpp \
    platform/SharedBufferChunkReader.cpp \
    platform/sql/SQLValue.cpp \
    platform/sql/SQLiteAuthorizer.cpp \
    platform/sql/SQLiteDatabase.cpp \
    platform/sql/SQLiteDatabaseTracker.cpp \
    platform/sql/SQLiteFileSystem.cpp \
    platform/sql/SQLiteStatement.cpp \
    platform/sql/SQLiteTransaction.cpp \
    platform/text/SegmentedString.cpp \
    platform/text/TextBoundaries.cpp \
    platform/text/TextBreakIterator.cpp \
    platform/text/TextCodec.cpp \
    platform/text/TextCodecLatin1.cpp \
    platform/text/TextCodecUserDefined.cpp \
    platform/text/TextCodecUTF16.cpp \
    platform/text/TextCodecUTF8.cpp \
    platform/text/TextCodecICU.cpp \
    platform/text/TextEncoding.cpp \
    platform/text/TextEncodingDetectorNone.cpp \
    platform/text/TextEncodingRegistry.cpp \
    platform/text/TextStream.cpp \
    platform/ThreadGlobalData.cpp \
    platform/ThreadTimers.cpp \
    platform/Timer.cpp \
    platform/UUID.cpp \
    platform/URL.cpp \
    platform/UserActivity.cpp \
    platform/Widget.cpp \
    platform/PlatformStrategies.cpp \
#{JAVA
    platform/ScrollAnimatorNone.cpp \
#}JAVA
    plugins/PluginDatabase.cpp \
    plugins/PluginDebug.cpp \
#    plugins/PluginPackage.cpp \ tav todo revise
    plugins/PluginStream.cpp \
#    plugins/PluginView.cpp \ tav todo revise
    rendering/AutoTableLayout.cpp \
    rendering/break_lines.cpp \
    rendering/BidiRun.cpp \
    rendering/CounterNode.cpp \
    rendering/EllipsisBox.cpp \
    rendering/FilterEffectRenderer.cpp \
    rendering/FixedTableLayout.cpp \
    rendering/FlowThreadController.cpp \
    rendering/FloatingObjects.cpp \
    rendering/HitTestingTransformState.cpp \
    rendering/HitTestLocation.cpp \
    rendering/HitTestResult.cpp \
    rendering/InlineElementBox.cpp \
    rendering/InlineBox.cpp \
    rendering/InlineFlowBox.cpp \
    rendering/InlineTextBox.cpp \
    rendering/ImageQualityController.cpp \
    rendering/LayoutState.cpp \
    rendering/LayoutRepainter.cpp \
    rendering/OrderIterator.cpp \
    rendering/RenderBlock.cpp \
    rendering/RenderBlockFlow.cpp \
    rendering/RenderBlockLineLayout.cpp \
    rendering/RenderBox.cpp \
    rendering/RenderBoxModelObject.cpp \
    rendering/RenderButton.cpp \
    rendering/RenderCombineText.cpp \
    rendering/RenderCounter.cpp \
    rendering/RenderDeprecatedFlexibleBox.cpp \
    rendering/RenderDetailsMarker.cpp \
    rendering/RenderEmbeddedObject.cpp \
    rendering/RenderElement.cpp \
    rendering/RenderFieldset.cpp \
    rendering/RenderFileUploadControl.cpp \
    rendering/RenderFlexibleBox.cpp \
    rendering/RenderFlowThread.cpp \
    rendering/RenderFrame.cpp \
    rendering/RenderFrameBase.cpp \
    rendering/RenderFrameSet.cpp \
    rendering/RenderGeometryMap.cpp \
    rendering/RenderGrid.cpp \
    rendering/RenderHTMLCanvas.cpp \
    rendering/RenderIFrame.cpp \
    rendering/RenderImage.cpp \
    rendering/RenderImageResource.cpp \
    rendering/RenderImageResourceStyleImage.cpp \
    rendering/RenderInline.cpp \
    rendering/RenderLayer.cpp \
    rendering/RenderLayerBacking.cpp \
    rendering/RenderLayerCompositor.cpp \
    rendering/RenderLayerFilterInfo.cpp \
    rendering/RenderLayerModelObject.cpp \
    rendering/RenderLineBoxList.cpp \
    rendering/RenderLineBreak.cpp \
    rendering/RenderListBox.cpp \
    rendering/RenderListItem.cpp \
    rendering/RenderListMarker.cpp \
    rendering/RenderMarquee.cpp \
    rendering/RenderMenuList.cpp \
    rendering/RenderMeter.cpp \
    rendering/RenderMultiColumnFlowThread.cpp \
    rendering/RenderMultiColumnSet.cpp \
    rendering/RenderNamedFlowThread.cpp \
    rendering/RenderNamedFlowFragment.cpp \
    rendering/RenderObject.cpp \
    rendering/RenderProgress.cpp \
    rendering/RenderQuote.cpp \
    rendering/RenderRegion.cpp \
    rendering/RenderRegionSet.cpp \
    rendering/RenderReplaced.cpp \
    rendering/RenderReplica.cpp \
    rendering/RenderRuby.cpp \
    rendering/RenderRubyBase.cpp \
    rendering/RenderRubyRun.cpp \
    rendering/RenderRubyText.cpp \
    rendering/RenderScrollbar.cpp \
    rendering/RenderScrollbarPart.cpp \
    rendering/RenderScrollbarTheme.cpp \
    rendering/RenderSearchField.cpp \
    rendering/RenderSlider.cpp \
    rendering/RenderSnapshottedPlugIn.cpp \
    rendering/RenderTable.cpp \
    rendering/RenderTableCaption.cpp \
    rendering/RenderTableCell.cpp \
    rendering/RenderTableCol.cpp \
    rendering/RenderTableRow.cpp \
    rendering/RenderTableSection.cpp \
    rendering/RenderText.cpp \
    rendering/RenderTextControl.cpp \
    rendering/RenderTextControlMultiLine.cpp \
    rendering/RenderTextControlSingleLine.cpp \
    rendering/RenderTextFragment.cpp \
    rendering/RenderTextLineBoxes.cpp \
    rendering/RenderTheme.cpp \
    rendering/RenderTreeAsText.cpp \
    rendering/RenderView.cpp \
    rendering/RenderWidget.cpp \
    rendering/RootInlineBox.cpp \
    rendering/ScrollBehavior.cpp \
    rendering/SimpleLineLayout.cpp \
    rendering/SimpleLineLayoutFunctions.cpp \
    rendering/TextPainter.cpp \
    rendering/TextPaintStyle.cpp \
    rendering/style/BasicShapes.cpp \
    rendering/style/ContentData.cpp \
    rendering/style/CounterDirectives.cpp \
    rendering/style/FillLayer.cpp \
    rendering/style/KeyframeList.cpp \
    rendering/style/NinePieceImage.cpp \
    rendering/style/QuotesData.cpp \
    rendering/style/RenderStyle.cpp \
    rendering/style/ShadowData.cpp \
    rendering/style/StyleBackgroundData.cpp \
    rendering/style/StyleBoxData.cpp \
    rendering/style/StyleCachedImage.cpp \
    rendering/style/StyleCachedImageSet.cpp \
    rendering/style/StyleDeprecatedFlexibleBoxData.cpp \
    rendering/style/StyleFilterData.cpp \
    rendering/style/StyleFlexibleBoxData.cpp \
    rendering/style/StyleGeneratedImage.cpp \
    rendering/style/StyleGridData.cpp \
    rendering/style/StyleGridItemData.cpp \
    rendering/style/StyleInheritedData.cpp \
    rendering/style/StyleMarqueeData.cpp \
    rendering/style/StyleMultiColData.cpp \
    rendering/style/StyleRareInheritedData.cpp \
    rendering/style/StyleRareNonInheritedData.cpp \
    rendering/style/StyleSurroundData.cpp \
    rendering/style/StyleTransformData.cpp \
    rendering/style/StyleVisualData.cpp \
    rendering/line/LineBreaker.cpp \
    rendering/line/LineInfo.cpp \
    rendering/line/LineWidth.cpp \
    rendering/line/TrailingObjects.cpp \
    style/StyleFontSizeFunctions.cpp \
    style/StyleResolveForDocument.cpp \
    style/StyleResolveTree.cpp \
    storage/Storage.cpp \
    storage/StorageAreaImpl.cpp \
    storage/StorageAreaSync.cpp \
    storage/StorageEvent.cpp \
    storage/StorageEventDispatcher.cpp \
    storage/StorageMap.cpp \
    storage/StorageNamespace.cpp \
    storage/StorageNamespaceImpl.cpp \
    storage/StorageStrategy.cpp \
    storage/StorageSyncManager.cpp \
    storage/StorageThread.cpp \
    storage/StorageTracker.cpp \
    testing/Internals.cpp \
    testing/InternalSettings.cpp \
    xml/DOMParser.cpp \
    xml/NativeXPathNSResolver.cpp \
    xml/XMLHttpRequest.cpp \
    xml/XMLHttpRequestException.cpp \
    xml/XMLHttpRequestProgressEventThrottle.cpp \
    xml/XMLHttpRequestUpload.cpp \
    xml/XMLErrors.cpp \
    xml/XMLSerializer.cpp

SOURCES += \
    loader/appcache/DOMApplicationCache.cpp \
    loader/appcache/ApplicationCache.cpp \
    loader/appcache/ApplicationCacheHost.cpp \
    loader/appcache/ApplicationCacheResource.cpp \
    loader/appcache/ApplicationCacheGroup.cpp \
    loader/appcache/ManifestParser.cpp \

contains(DEFINES, WTF_USE_CF=1) {
    SOURCES += \
        editing/SmartReplaceCF.cpp
}

contains(DEFINES, ENABLE_XML=1) {
    SOURCES += \
        xml/XPathEvaluator.cpp \
        xml/XPathException.cpp \
        xml/XPathExpression.cpp \
        xml/XPathExpressionNode.cpp \
        xml/XPathFunctions.cpp \
        xml/XPathNodeSet.cpp \
        xml/XPathNSResolver.cpp \
        xml/XPathParser.cpp \
        xml/XPathPath.cpp \
        xml/XPathPredicate.cpp \
        xml/XPathResult.cpp \
        xml/XPathStep.cpp \
        xml/XPathUtil.cpp \
        xml/XPathValue.cpp \
        xml/XPathVariableReference.cpp \
        xml/parser/XMLDocumentParser.cpp
}

SOURCES += \
    platform/graphics/texmap/GraphicsLayerTextureMapper.cpp \
    platform/graphics/texmap/TextureMapper.cpp \
    platform/graphics/texmap/TextureMapperBackingStore.cpp \
    platform/graphics/texmap/TextureMapperFPSCounter.cpp \
    platform/graphics/texmap/TextureMapperImageBuffer.cpp \
    platform/graphics/texmap/TextureMapperLayer.cpp \
    platform/graphics/texmap/TextureMapperSurfaceBackingStore.cpp \
    platform/graphics/texmap/TextureMapperTile.cpp \
    platform/graphics/texmap/TextureMapperTiledBackingStore.cpp \
    platform/network/DNSResolveQueue.cpp \
    platform/network/MIMESniffing.cpp \
    platform/Cursor.cpp \
    platform/ContextMenu.cpp \
    platform/ContextMenuItem.cpp \
    platform/ContextMenuItemNone.cpp \
    platform/ContextMenuNone.cpp

#rewrite it for Java
SOURCES += \
    plugins/PluginPackageNone.cpp \
    plugins/PluginViewNone.cpp \
    plugins/java/PluginDataJava.cpp

contains(DEFINES, ICU_UNICODE=1) {
    SOURCES += \
        platform/java/TextBreakIteratorInternalICUJava.cpp
} else {
    SOURCES += \
        platform/java/TextCodecJava.cpp \
        platform/java/TextBreakIteratorJava.cpp \
        platform/java/TextNormalizerJava.cpp
}

win32-* {
    SOURCES += \
        platform/win/SystemInfo.cpp
}

contains(DEFINES, IMAGEIO=1) {
    SOURCES += \
        platform/graphics/java/ImageSourceJava.cpp
} else {
    SOURCES += \
        platform/graphics/ImageSource.cpp \
        platform/image-decoders/ImageDecoder.cpp \
        platform/image-decoders/bmp/BMPImageDecoder.cpp \
        platform/image-decoders/bmp/BMPImageReader.cpp \
        platform/image-decoders/gif/GIFImageDecoder.cpp \
        platform/image-decoders/gif/GIFImageReader.cpp \
        platform/image-decoders/ico/ICOImageDecoder.cpp \
        platform/image-decoders/jpeg/JPEGImageDecoder.cpp \
        platform/image-decoders/png/PNGImageDecoder.cpp \
        platform/image-decoders/webp/WEBPImageDecoder.cpp
}

SOURCES += \
    platform/graphics/java/PathJava.cpp \
    platform/graphics/java/FontDataJava.cpp \
    platform/graphics/java/FontCustomPlatformData.cpp \
    platform/graphics/java/BufferImageJava.cpp \

contains(DEFINES, ENABLE_ICONDATABASE=1) {
    SOURCES += \
        loader/icon/IconController.cpp \
        loader/icon/IconDatabaseBase.cpp \
        loader/icon/IconLoader.cpp \
        loader/icon/IconDatabase.cpp \
        loader/icon/IconRecord.cpp \
        loader/icon/PageURLRecord.cpp
}

contains(DEFINES, ENABLE_DATA_TRANSFER_ITEMS=1) {
    SOURCES += \
        dom/DataTransferItem.cpp \
        dom/StringCallback.cpp
}

contains(DEFINES, ENABLE_FILE_SYSTEM=1) {
    SOURCES += \
}

contains(DEFINES, ENABLE_WEB_SOCKETS=1) {
    SOURCES += \
        platform/network/SocketStreamErrorBase.cpp \
        platform/network/SocketStreamHandleBase.cpp \
        platform/network/java/SocketStreamHandleJava.cpp
}

contains(DEFINES, ENABLE_WORKERS=1) {
    SOURCES += \
        bindings/js/WorkerScriptController.cpp \
    	bindings/js/WorkerScriptDebugServer.cpp \
        loader/WorkerThreadableLoader.cpp \
        page/WorkerNavigator.cpp \
        workers/AbstractWorker.cpp \
        workers/DedicatedWorkerThread.cpp \
        workers/DedicatedWorkerGlobalScope.cpp \
        workers/Worker.cpp \
        workers/WorkerEventQueue.cpp \
        workers/WorkerLocation.cpp \
        workers/WorkerMessagingProxy.cpp \
        workers/WorkerRunLoop.cpp \
        workers/WorkerThread.cpp \
        workers/WorkerScriptLoader.cpp \
	workers/WorkerGlobalScope.cpp
}

contains(DEFINES, ENABLE_SHARED_WORKERS=1) {
    SOURCES += \
        bindings/js/JSWorkerGlobalScopeCustom.cpp \
	bindings/js/JSWorkerGlobalScopeBase.cpp \
        bindings/js/JSSharedWorkerCustom.cpp \
        workers/DefaultSharedWorkerRepository.cpp \
        workers/SharedWorker.cpp \
        workers/SharedWorkerRepository.cpp \
        workers/SharedWorkerThread.cpp \
	workers/SharedWorkerGlobalScope.cpp
}

contains(DEFINES, ENABLE_INPUT_SPEECH=1) {
    SOURCES += \
        page/SpeechInput.cpp \
        page/SpeechInputEvent.cpp \
        page/SpeechInputResult.cpp \
        page/SpeechInputResultList.cpp \
        rendering/RenderInputSpeech.cpp
}

contains(DEFINES, ENABLE_QUOTA=1) {
    SOURCES += \
        Modules/quota/DOMWindowQuota.cpp \
        Modules/quota/NavigatorStorageQuota.cpp \
        Modules/quota/StorageErrorCallback.cpp \
        Modules/quota/StorageInfo.cpp \
        Modules/quota/StorageQuota.cpp

	contains(DEFINES, ENABLE_WORKERS=1) {
	
	
	
	    SOURCES += \
	        Modules/quota/WorkerNavigatorStorageQuota.h
	}
}

win32-* {
#    MOC_PREPROCESSOR = --preprocessor=\"cl /nologo /EP\"
}

contains(DEFINES, ENABLE_VIDEO=1) {
    SOURCES += \
        html/HTMLAudioElement.cpp \
        html/HTMLMediaElement.cpp \
	html/HTMLMediaSession.cpp \
        html/HTMLSourceElement.cpp \
        html/HTMLVideoElement.cpp \
        html/MediaController.cpp \
        html/MediaFragmentURIParser.cpp \
        html/shadow/MediaControlElementTypes.cpp \
        html/shadow/MediaControlElements.cpp \
        html/TimeRanges.cpp \
        platform/graphics/MediaPlayer.cpp \
	platform/audio/MediaSession.cpp \
	platform/audio/MediaSessionManager.cpp \
        rendering/RenderVideo.cpp \
        rendering/RenderMedia.cpp \
        rendering/RenderMediaControls.cpp \
        rendering/RenderMediaControlElements.cpp


    SOURCES += \
        platform/graphics/java/MediaPlayerPrivateJava.cpp
}

contains(DEFINES, ENABLE_FULLSCREEN_API=1) {
    SOURCES += \
        rendering/RenderFullScreen.cpp
    HEADERS += \
        rendering/RenderFullScreen.h
}

contains(DEFINES, ENABLE_XSLT=1) {
    SOURCES += \
        bindings/js/JSXSLTProcessorCustom.cpp \
        xml/XMLTreeViewer.cpp

    contains(DEFINES, WTF_USE_LIBXML2=1) {
        SOURCES += \
            xml/XSLTProcessor.cpp \
            xml/XSLTProcessorLibxslt.cpp \
            dom/TransformSourceLibxslt.cpp \
            xml/XSLStyleSheetLibxslt.cpp \
            xml/XSLImportRule.cpp \
            xml/XSLTExtensions.cpp \
            xml/XSLTUnicodeSort.cpp \
            xml/parser/XMLDocumentParserLibxml2.cpp \
            xml/parser/XMLDocumentParserScope.cpp
    }
}

contains(DEFINES, ENABLE_CSS_FILTERS=1) {
    SOURCES += \
        css/CSSFilterImageValue.cpp
}

contains(DEFINES, ENABLE_FILTERS=1) {
    SOURCES += \
        platform/graphics/cpu/arm/filters/FELightingNEON.cpp \
        platform/graphics/filters/DistantLightSource.cpp \
        platform/graphics/filters/FEBlend.cpp \
        platform/graphics/filters/FEColorMatrix.cpp \
        platform/graphics/filters/FEComponentTransfer.cpp \
        platform/graphics/filters/FEComposite.cpp \
        platform/graphics/filters/FEConvolveMatrix.cpp \
        platform/graphics/filters/FEDiffuseLighting.cpp \
        platform/graphics/filters/FEDisplacementMap.cpp \
        platform/graphics/filters/FEDropShadow.cpp \
        platform/graphics/filters/FEFlood.cpp \
        platform/graphics/filters/FEGaussianBlur.cpp \
        platform/graphics/filters/FELighting.cpp \
        platform/graphics/filters/FEMerge.cpp \
        platform/graphics/filters/FEMorphology.cpp \
        platform/graphics/filters/FEOffset.cpp \
        platform/graphics/filters/FESpecularLighting.cpp \
        platform/graphics/filters/FETile.cpp \
        platform/graphics/filters/FETurbulence.cpp \
        platform/graphics/filters/FilterOperations.cpp \
        platform/graphics/filters/FilterOperation.cpp \
        platform/graphics/filters/FilterEffect.cpp \
        platform/graphics/filters/PointLightSource.cpp \
        platform/graphics/filters/SpotLightSource.cpp \
        platform/graphics/filters/SourceAlpha.cpp \
        platform/graphics/filters/SourceGraphic.cpp \
}

contains(DEFINES, ENABLE_MATHML=1) {
    SOURCES += \
        mathml/MathMLElement.cpp \
        mathml/MathMLInlineContainerElement.cpp \
        mathml/MathMLMathElement.cpp \
        mathml/MathMLTextElement.cpp \
	mathml/MathMLMencloseElement.cpp \
        mathml/MathMLSelectElement.cpp \
        rendering/mathml/RenderMathMLBlock.cpp \
        rendering/mathml/RenderMathMLFenced.cpp \
        rendering/mathml/RenderMathMLFraction.cpp \
        rendering/mathml/RenderMathMLMath.cpp \
	rendering/mathml/RenderMathMLMenclose.cpp \
        rendering/mathml/RenderMathMLOperator.cpp \
        rendering/mathml/RenderMathMLRoot.cpp \
        rendering/mathml/RenderMathMLRow.cpp \
        rendering/mathml/RenderMathMLScripts.cpp \
        rendering/mathml/RenderMathMLSpace.cpp \
        rendering/mathml/RenderMathMLSquareRoot.cpp \
	rendering/mathml/RenderMathMLToken.cpp \
        rendering/mathml/RenderMathMLUnderOver.cpp
}

contains(DEFINES, ENABLE_SVG=1) {
    SOURCES += \
        bindings/js/JSSVGElementInstanceCustom.cpp \
        bindings/js/JSSVGLengthCustom.cpp \
        bindings/js/JSSVGPathSegCustom.cpp \
        css/SVGCSSComputedStyleDeclaration.cpp \
        css/SVGCSSParser.cpp \
        css/SVGCSSStyleSelector.cpp \
        rendering/style/SVGRenderStyle.cpp \
        rendering/style/SVGRenderStyleDefs.cpp \
        rendering/PointerEventsHitRules.cpp \
        rendering/svg/RenderSVGEllipse.cpp \
        rendering/svg/RenderSVGPath.cpp \
        rendering/svg/RenderSVGRect.cpp \
        rendering/svg/RenderSVGShape.cpp \
            rendering/svg/RenderSVGBlock.cpp \
            rendering/svg/RenderSVGContainer.cpp \
            rendering/svg/RenderSVGForeignObject.cpp \
            rendering/svg/RenderSVGGradientStop.cpp \
            rendering/svg/RenderSVGHiddenContainer.cpp \
            rendering/svg/RenderSVGImage.cpp \
            rendering/svg/RenderSVGInline.cpp \
            rendering/svg/RenderSVGInlineText.cpp \
            rendering/svg/RenderSVGModelObject.cpp \
            rendering/svg/RenderSVGResource.cpp \
            rendering/svg/RenderSVGResourceClipper.cpp \
            rendering/svg/RenderSVGResourceContainer.cpp \
            rendering/svg/RenderSVGResourceFilter.cpp \
            rendering/svg/RenderSVGResourceFilterPrimitive.cpp \
            rendering/svg/RenderSVGResourceGradient.cpp \
            rendering/svg/RenderSVGResourceLinearGradient.cpp \
            rendering/svg/RenderSVGResourceMarker.cpp \
            rendering/svg/RenderSVGResourceMasker.cpp \
            rendering/svg/RenderSVGResourcePattern.cpp \
            rendering/svg/RenderSVGResourceRadialGradient.cpp \
            rendering/svg/RenderSVGResourceSolidColor.cpp \
            rendering/svg/RenderSVGRoot.cpp \
            rendering/svg/RenderSVGText.cpp \
            rendering/svg/RenderSVGTextPath.cpp \
            rendering/svg/RenderSVGTransformableContainer.cpp \
            rendering/svg/RenderSVGViewportContainer.cpp \
            rendering/svg/SVGInlineFlowBox.cpp \
            rendering/svg/SVGInlineTextBox.cpp \
            rendering/svg/SVGPathData.cpp \
            rendering/svg/SVGRenderSupport.cpp \
            rendering/svg/SVGRenderTreeAsText.cpp \
            rendering/svg/SVGRenderingContext.cpp \
            rendering/svg/SVGResources.cpp \
            rendering/svg/SVGResourcesCache.cpp \
            rendering/svg/SVGResourcesCycleSolver.cpp \
            rendering/svg/SVGRootInlineBox.cpp \
            rendering/svg/SVGTextChunk.cpp \
            rendering/svg/SVGTextChunkBuilder.cpp \
            rendering/svg/SVGTextLayoutAttributes.cpp \
            rendering/svg/SVGTextLayoutAttributesBuilder.cpp \
            rendering/svg/SVGTextLayoutEngine.cpp \
            rendering/svg/SVGTextLayoutEngineBaseline.cpp \
            rendering/svg/SVGTextLayoutEngineSpacing.cpp \
            rendering/svg/SVGTextMetrics.cpp \
            rendering/svg/SVGTextMetricsBuilder.cpp \
            rendering/svg/SVGTextQuery.cpp \
            rendering/svg/SVGTextRunRenderingContext.cpp \
        svg/animation/SMILTime.cpp \
        svg/animation/SMILTimeContainer.cpp \
        svg/animation/SVGSMILElement.cpp \
        svg/graphics/filters/SVGFEImage.cpp \
        svg/graphics/filters/SVGFilter.cpp \
        svg/graphics/filters/SVGFilterBuilder.cpp \
        svg/graphics/SVGImage.cpp \
        svg/graphics/SVGImageCache.cpp \
        svg/graphics/SVGImageForContainer.cpp \
        svg/properties/SVGAttributeToPropertyMap.cpp \
	svg/properties/SVGAnimatedProperty.cpp \
        svg/properties/SVGPathSegListPropertyTearOff.cpp \
	svg/properties/SVGPropertyInfo.cpp \
            svg/SVGDocumentExtensions.cpp \
            svg/ColorDistance.cpp \
            svg/SVGAElement.cpp \
            svg/SVGAltGlyphDefElement.cpp \
            svg/SVGAltGlyphElement.cpp \
            svg/SVGAltGlyphItemElement.cpp \
            svg/SVGAngle.cpp \
            svg/SVGAnimateColorElement.cpp \
            svg/SVGAnimatedAngle.cpp \
            svg/SVGAnimatedBoolean.cpp \
            svg/SVGAnimatedColor.cpp \
            svg/SVGAnimatedEnumeration.cpp \
            svg/SVGAnimatedInteger.cpp \
            svg/SVGAnimatedIntegerOptionalInteger.cpp \
            svg/SVGAnimatedLength.cpp \
            svg/SVGAnimatedLengthList.cpp \
            svg/SVGAnimatedNumber.cpp \
            svg/SVGAnimatedNumberList.cpp \
            svg/SVGAnimatedNumberOptionalNumber.cpp \
            svg/SVGAnimatedPath.cpp \
            svg/SVGAnimatedPointList.cpp \
            svg/SVGAnimatedPreserveAspectRatio.cpp \
            svg/SVGAnimatedRect.cpp \
            svg/SVGAnimatedString.cpp \
            svg/SVGAnimatedTransformList.cpp \
            svg/SVGAnimatedType.cpp \
	    svg/SVGAnimatedTypeAnimator.cpp \
            svg/SVGAnimateElement.cpp \
            svg/SVGAnimateMotionElement.cpp \
            svg/SVGAnimateTransformElement.cpp \
            svg/SVGAnimationElement.cpp \
            svg/SVGCircleElement.cpp \
            svg/SVGClipPathElement.cpp \
            svg/SVGColor.cpp \
            svg/SVGComponentTransferFunctionElement.cpp \
            svg/SVGCursorElement.cpp \
            svg/SVGDefsElement.cpp \
            svg/SVGDescElement.cpp \
            svg/SVGDocument.cpp \
            svg/SVGElement.cpp \
            svg/SVGElementInstance.cpp \
            svg/SVGElementInstanceList.cpp \
            svg/SVGEllipseElement.cpp \
            svg/SVGException.cpp \
            svg/SVGExternalResourcesRequired.cpp \
            svg/SVGFEBlendElement.cpp \
            svg/SVGFEColorMatrixElement.cpp \
            svg/SVGFEComponentTransferElement.cpp \
            svg/SVGFECompositeElement.cpp \
            svg/SVGFEConvolveMatrixElement.cpp \
            svg/SVGFEDiffuseLightingElement.cpp \
            svg/SVGFEDisplacementMapElement.cpp \
            svg/SVGFEDistantLightElement.cpp \
            svg/SVGFEDropShadowElement.cpp \
            svg/SVGFEFloodElement.cpp \
            svg/SVGFEFuncAElement.cpp \
            svg/SVGFEFuncBElement.cpp \
            svg/SVGFEFuncGElement.cpp \
            svg/SVGFEFuncRElement.cpp \
            svg/SVGFEGaussianBlurElement.cpp \
            svg/SVGFEImageElement.cpp \
            svg/SVGFELightElement.cpp \
            svg/SVGFEMergeElement.cpp \
            svg/SVGFEMergeNodeElement.cpp \
            svg/SVGFEMorphologyElement.cpp \
            svg/SVGFEOffsetElement.cpp \
            svg/SVGFEPointLightElement.cpp \
            svg/SVGFESpecularLightingElement.cpp \
            svg/SVGFESpotLightElement.cpp \
            svg/SVGFETileElement.cpp \
            svg/SVGFETurbulenceElement.cpp \
            svg/SVGFilterElement.cpp \
            svg/SVGFilterPrimitiveStandardAttributes.cpp \
            svg/SVGFitToViewBox.cpp \
            svg/SVGFontData.cpp \
            svg/SVGFontElement.cpp \
            svg/SVGFontFaceElement.cpp \
            svg/SVGFontFaceFormatElement.cpp \
            svg/SVGFontFaceNameElement.cpp \
            svg/SVGFontFaceSrcElement.cpp \
            svg/SVGFontFaceUriElement.cpp \
            svg/SVGForeignObjectElement.cpp \
            svg/SVGGElement.cpp \
            svg/SVGGlyphElement.cpp \
            svg/SVGGlyphRefElement.cpp \
            svg/SVGGradientElement.cpp \
	    svg/SVGGraphicsElement.cpp \
            svg/SVGHKernElement.cpp \
            svg/SVGImageElement.cpp \
            svg/SVGImageLoader.cpp \
            svg/SVGLangSpace.cpp \
            svg/SVGLength.cpp \
            svg/SVGLengthContext.cpp \
            svg/SVGLengthList.cpp \
            svg/SVGLinearGradientElement.cpp \
            svg/SVGLineElement.cpp \
            svg/SVGLocatable.cpp \
            svg/SVGMarkerElement.cpp \
            svg/SVGMaskElement.cpp \
            svg/SVGMetadataElement.cpp \
            svg/SVGMissingGlyphElement.cpp \
            svg/SVGMPathElement.cpp \
            svg/SVGNumberList.cpp \
            svg/SVGPaint.cpp \
            svg/SVGParserUtilities.cpp \
            svg/SVGPathBlender.cpp \
            svg/SVGPathBuilder.cpp \
            svg/SVGPathByteStreamBuilder.cpp \
            svg/SVGPathByteStreamSource.cpp \
            svg/SVGPathElement.cpp \
            svg/SVGPathParser.cpp \
            svg/SVGPathSegList.cpp \
            svg/SVGPathSegListBuilder.cpp \
            svg/SVGPathSegListSource.cpp \
            svg/SVGPathStringBuilder.cpp \
            svg/SVGPathStringSource.cpp \
            svg/SVGPathTraversalStateBuilder.cpp \
            svg/SVGPathUtilities.cpp \
            svg/SVGPatternElement.cpp \
            svg/SVGPointList.cpp \
            svg/SVGPolyElement.cpp \
            svg/SVGPolygonElement.cpp \
            svg/SVGPolylineElement.cpp \
            svg/SVGPreserveAspectRatio.cpp \
            svg/SVGRadialGradientElement.cpp \
            svg/SVGRectElement.cpp \
            svg/SVGSVGElement.cpp \
            svg/SVGScriptElement.cpp \
            svg/SVGSetElement.cpp \
            svg/SVGStopElement.cpp \
            svg/SVGStringList.cpp \
            svg/SVGStyleElement.cpp \
            svg/SVGSwitchElement.cpp \
            svg/SVGSymbolElement.cpp \
            svg/SVGTRefElement.cpp \
            svg/SVGTSpanElement.cpp \
            svg/SVGTests.cpp \
            svg/SVGTextContentElement.cpp \
            svg/SVGTextElement.cpp \
            svg/SVGTextPathElement.cpp \
            svg/SVGTextPositioningElement.cpp \
            svg/SVGTitleElement.cpp \
            svg/SVGTransform.cpp \
            svg/SVGTransformDistance.cpp \
            svg/SVGTransformList.cpp \
            svg/SVGTransformable.cpp \
            svg/SVGURIReference.cpp \
            svg/SVGUseElement.cpp \
            svg/SVGVKernElement.cpp \
            svg/SVGViewElement.cpp \
            svg/SVGViewSpec.cpp \
            svg/SVGZoomAndPan.cpp \
            svg/SVGZoomEvent.cpp    

    ALL_IN_ONE_SOURCES += \
        rendering/svg/RenderSVGAllInOne.cpp \
        svg/SVGAllInOne.cpp
}

contains(DEFINES, ENABLE_JAVASCRIPT_DEBUGGER=1) {
    SOURCES += \
        bindings/js/ScriptProfiler.cpp
}
