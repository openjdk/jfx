include($$PWD/../WebKitJava.pri)

!contains(TEMPLATE, lib) {
    TEMPLATE = app
    SOURCES += DerivedSourcesJava.cpp
    CONFIG += console ordered
    CONFIG -= windows
}

GENERATED_SOURCES_DIR = generated
OBJECTS_DIR = obj
SOURCE_DIR = $$BASE_DIR
DESTDIR=../lib
QMAKE_LIBDIR += $$DESTDIR

FEATURE_DEFINES += \
    ENABLE_CHANNEL_MESSAGING=0 \
    ENABLE_DATABASE=0 \
    ENABLE_DATAGRID=0 \
    ENABLE_DATALIST=1 \
    ENABLE_DETAILS=1 \
    ENABLE_DOM_STORAGE=0 \
    ENABLE_EVENTSOURCE=1 \
    ENABLE_ICONDATABASE=0 \
    ENABLE_INPUT_COLOR=0 \
    ENABLE_INPUT_TYPE_DATE=1\
    ENABLE_INPUT_TYPE_DATETIME=1\
    ENABLE_INPUT_TYPE_DATETIMELOCAL=1\
    ENABLE_INPUT_TYPE_MONTH=1\
    ENABLE_INPUT_TYPE_TIME=1\
    ENABLE_INPUT_TYPE_WEEK=1\
    ENABLE_INSPECTOR=1 \
    ENABLE_JAVASCRIPT_DEBUGGER=1 \
    ENABLE_JAVA_BRIDGE=1 \
    ENABLE_MATHML=1 \
    ENABLE_METER_TAG=1 \
    ENABLE_MICRODATA=0 \
    ENABLE_NETSCAPE_PLUGIN_API=0 \
    ENABLE_OFFLINE_WEB_APPLICATIONS=0 \
    ENABLE_ORIENTATION_EVENTS=0 \
    ENABLE_PAGE_VISIBILITY_API=1 \
    ENABLE_POINTER_LOCK=0 \
    ENABLE_PROGRESS_TAG=1 \
    ENABLE_RUBY=0 \
    ENABLE_SHARED_WORKERS=1 \
    ENABLE_SQL_DATABASE=0 \
    ENABLE_SQLITE=0 \
    ENABLE_SVG=1 \
    ENABLE_SVG_ANIMATION=1 \
    ENABLE_SVG_AS_IMAGE=1 \
    ENABLE_SVG_FILTERS=1 \
    ENABLE_SVG_FONTS=1 \
    ENABLE_SVG_FOREIGN_OBJECT=1 \
    ENABLE_SVG_USE=1 \
    ENABLE_TOUCH_EVENTS=0 \
    ENABLE_VIDEO=1 \
    ENABLE_WCSS=0 \
    ENABLE_WEB_SOCKETS=1 \
    ENABLE_WORKERS=1 \
    ENABLE_XHR_RESPONSE_BLOB=1 \
    ENABLE_XHTMLMP=0 \
    ENABLE_XPATH=1 \
    ENABLE_XSLT=1 \

DEFINES += \
    BUILDING_WebCore \
    BUILD_WEBKIT \
    WTF_USE_LIBXML2=1 \
    $$FEATURE_DEFINES \

## Derived source generators
MATHML_NAMES = $$PWD/mathml/mathtags.in

SVG_NAMES = $$PWD/svg/svgtags.in

XLINK_NAMES = $$PWD/svg/xlinkattrs.in

TOKENIZER = $$PWD/css/tokenizer.flex

DOCTYPESTRINGS_GPERF = $$PWD/html/DocTypeStrings.gperf

CSSBISON = $$PWD/css/CSSGrammar.y

contains(DEFINES, ENABLE_XSLT=1) {
    XMLVIEWER_CSS = $$PWD/xml/XMLViewer.css
    XMLVIEWER_JS = $$PWD/xml/XMLViewer.js
}

FONT_NAMES = $$PWD/css/WebKitFontFamilyNames.in

HTML_NAMES = $$PWD/html/HTMLTagNames.in

XML_NAMES = $$PWD/xml/xmlattrs.in

XMLNS_NAMES = $$PWD/xml/xmlnsattrs.in

HTML_ENTITIES = $$PWD/html/parser/HTMLEntityNames.in

EVENTS_NAMES = $$PWD/dom/EventNames.in

EVENT_TARGET_FACTORY = $$PWD/dom/EventTargetFactory.in

DOM_EXCEPTIONS = $$PWD/dom/DOMExceptions.in

COLORDATA_GPERF = $$PWD/platform/ColorData.gperf

WALDOCSSPROPS = $$PWD/css/CSSPropertyNames.in

WALDOCSSVALUES = $$PWD/css/CSSValueKeywords.in

INSPECTOR_JSON = $$PWD/inspector/Inspector.json

# INSPECTOR_BACKEND_STUB_QRC = $$PWD/inspector/front-end/InspectorBackendCommands.qrc

INJECTED_SCRIPT_SOURCE = $$PWD/inspector/InjectedScriptSource.js

DEBUGGER_SCRIPT_SOURCE = $$PWD/bindings/v8/DebuggerScript.js

contains(DEFINES, ENABLE_DASHBOARD_SUPPORT=1): DASHBOARDSUPPORTCSSPROPERTIES = $$PWD/css/DashboardSupportCSSPropertyNames.in

XPATHBISON = $$PWD/xml/XPathGrammar.y

contains(DEFINES, ENABLE_SVG=1) {
    EXTRACSSPROPERTIES += $$PWD/css/SVGCSSPropertyNames.in
    EXTRACSSVALUES += $$PWD/css/SVGCSSValueKeywords.in
}

STYLESHEETS_EMBED = \
    $$PWD/css/html.css \
    $$PWD/css/quirks.css \
    $$PWD/css/mathml.css \
    $$PWD/css/svg.css \
    $$PWD/css/view-source.css \
    $$PWD/css/fullscreen.css \
    $$PWD/css/mediaControls.css \
    $$PWD/css/mediaControlsJava.css \
    $$PWD/css/themeQtNoListboxes.css

IDL_BINDINGS_JAVA = \
    $$PWD/css/Counter.idl \
    $$PWD/css/CSSCharsetRule.idl \
    $$PWD/css/CSSFontFaceRule.idl \
    $$PWD/css/CSSImportRule.idl \
    $$PWD/css/CSSMediaRule.idl \
    $$PWD/css/CSSPageRule.idl \
    $$PWD/css/CSSPrimitiveValue.idl \
    $$PWD/css/CSSRule.idl \
    $$PWD/css/CSSRuleList.idl \
    $$PWD/css/CSSStyleDeclaration.idl \
    $$PWD/css/CSSStyleRule.idl \
    $$PWD/css/CSSStyleSheet.idl \
    $$PWD/css/CSSUnknownRule.idl \
    $$PWD/css/CSSValue.idl \
    $$PWD/css/CSSValueList.idl \
    $$PWD/css/MediaList.idl \
    $$PWD/css/Rect.idl \
    $$PWD/css/RGBColor.idl \
    $$PWD/css/StyleSheet.idl \
    $$PWD/css/StyleSheetList.idl \
    $$PWD/dom/Attr.idl \
    $$PWD/dom/CDATASection.idl \
    $$PWD/dom/CharacterData.idl \
    $$PWD/dom/Comment.idl \
    $$PWD/dom/DocumentFragment.idl \
    $$PWD/dom/Document.idl \
    $$PWD/dom/DocumentType.idl \
    $$PWD/dom/DOMImplementation.idl \
    $$PWD/dom/DOMStringList.idl \
    $$PWD/dom/Element.idl \
    $$PWD/dom/Entity.idl \
    $$PWD/dom/EntityReference.idl \
    $$PWD/dom/Event.idl \
    $$PWD/dom/KeyboardEvent.idl \
    $$PWD/dom/MouseEvent.idl \
    $$PWD/dom/MutationEvent.idl \
    $$PWD/dom/NamedNodeMap.idl \
    $$PWD/dom/Node.idl \
    $$PWD/dom/NodeFilter.idl \
    $$PWD/dom/NodeIterator.idl \
    $$PWD/dom/NodeList.idl \
    $$PWD/dom/Notation.idl \
    $$PWD/dom/ProcessingInstruction.idl \
    $$PWD/dom/Range.idl \
    $$PWD/dom/Text.idl \
    $$PWD/dom/TreeWalker.idl \
    $$PWD/dom/UIEvent.idl \
    $$PWD/dom/WheelEvent.idl \
    $$PWD/html/HTMLAnchorElement.idl \
    $$PWD/html/HTMLAppletElement.idl \
    $$PWD/html/HTMLAreaElement.idl \
    $$PWD/html/HTMLBaseElement.idl \
    $$PWD/html/HTMLBaseFontElement.idl \
    $$PWD/html/HTMLBodyElement.idl \
    $$PWD/html/HTMLBRElement.idl \
    $$PWD/html/HTMLButtonElement.idl \
    $$PWD/html/HTMLCollection.idl \
    $$PWD/html/HTMLDirectoryElement.idl \
    $$PWD/html/HTMLDivElement.idl \
    $$PWD/html/HTMLDListElement.idl \
    $$PWD/html/HTMLDocument.idl \
    $$PWD/html/HTMLElement.idl \
    $$PWD/html/HTMLFieldSetElement.idl \
    $$PWD/html/HTMLFontElement.idl \
    $$PWD/html/HTMLFormElement.idl \
    $$PWD/html/HTMLFrameElement.idl \
    $$PWD/html/HTMLFrameSetElement.idl \
    $$PWD/html/HTMLHeadElement.idl \
    $$PWD/html/HTMLHeadingElement.idl \
    $$PWD/html/HTMLHRElement.idl \
    $$PWD/html/HTMLHtmlElement.idl \
    $$PWD/html/HTMLIFrameElement.idl \
    $$PWD/html/HTMLImageElement.idl \
    $$PWD/html/HTMLInputElement.idl \
    $$PWD/html/HTMLLabelElement.idl \
    $$PWD/html/HTMLLegendElement.idl \
    $$PWD/html/HTMLLIElement.idl \
    $$PWD/html/HTMLLinkElement.idl \
    $$PWD/html/HTMLMapElement.idl \
    $$PWD/html/HTMLMenuElement.idl \
    $$PWD/html/HTMLMetaElement.idl \
    $$PWD/html/HTMLModElement.idl \
    $$PWD/html/HTMLObjectElement.idl \
    $$PWD/html/HTMLOListElement.idl \
    $$PWD/html/HTMLOptGroupElement.idl \
    $$PWD/html/HTMLOptionElement.idl \
    $$PWD/html/HTMLOptionsCollection.idl \
    $$PWD/html/HTMLParagraphElement.idl \
    $$PWD/html/HTMLParamElement.idl \
    $$PWD/html/HTMLPreElement.idl \
    $$PWD/html/HTMLQuoteElement.idl \
    $$PWD/html/HTMLScriptElement.idl \
    $$PWD/html/HTMLSelectElement.idl \
    $$PWD/html/HTMLStyleElement.idl \
    $$PWD/html/HTMLTableCaptionElement.idl \
    $$PWD/html/HTMLTableCellElement.idl \
    $$PWD/html/HTMLTableColElement.idl \
    $$PWD/html/HTMLTableElement.idl \
    $$PWD/html/HTMLTableRowElement.idl \
    $$PWD/html/HTMLTableSectionElement.idl \
    $$PWD/html/HTMLTextAreaElement.idl \
    $$PWD/html/HTMLTitleElement.idl \
    $$PWD/html/HTMLUListElement.idl \
    $$PWD/page/DOMWindow.idl \
    $$PWD/page/DOMSelection.idl \
    $$PWD/xml/XPathExpression.idl \
    $$PWD/xml/XPathNSResolver.idl \
    $$PWD/xml/XPathResult.idl

IDL_BINDINGS += \
    $$PWD/Modules/filesystem/DOMFileSystem.idl \
    $$PWD/Modules/filesystem/DOMFileSystemSync.idl \
    $$PWD/Modules/filesystem/DOMWindowFileSystem.idl \
    $$PWD/Modules/filesystem/DirectoryEntry.idl \
    $$PWD/Modules/filesystem/DirectoryEntrySync.idl \
    $$PWD/Modules/filesystem/DirectoryReader.idl \
    $$PWD/Modules/filesystem/DirectoryReaderSync.idl \
    $$PWD/Modules/filesystem/EntriesCallback.idl \
    $$PWD/Modules/filesystem/Entry.idl \
    $$PWD/Modules/filesystem/EntryArray.idl \
    $$PWD/Modules/filesystem/EntryArraySync.idl \
    $$PWD/Modules/filesystem/EntryCallback.idl \
    $$PWD/Modules/filesystem/EntrySync.idl \
    $$PWD/Modules/filesystem/ErrorCallback.idl \
    $$PWD/Modules/filesystem/FileCallback.idl \
    $$PWD/Modules/filesystem/FileEntry.idl \
    $$PWD/Modules/filesystem/FileEntrySync.idl \
    $$PWD/Modules/filesystem/FileSystemCallback.idl \
    $$PWD/Modules/filesystem/FileWriter.idl \
    $$PWD/Modules/filesystem/FileWriterCallback.idl \
    $$PWD/Modules/filesystem/Metadata.idl \
    $$PWD/Modules/filesystem/MetadataCallback.idl \
    $$PWD/Modules/filesystem/WorkerContextFileSystem.idl \
    $$PWD/Modules/geolocation/Geolocation.idl \
    $$PWD/Modules/geolocation/Geoposition.idl \
    $$PWD/Modules/geolocation/NavigatorGeolocation.idl \
    $$PWD/Modules/geolocation/PositionCallback.idl \
    $$PWD/Modules/geolocation/PositionError.idl \
    $$PWD/Modules/geolocation/PositionErrorCallback.idl \
    $$PWD/Modules/indexeddb/DOMWindowIndexedDatabase.idl \
    $$PWD/Modules/indexeddb/IDBAny.idl \
    $$PWD/Modules/indexeddb/IDBCursor.idl \
    $$PWD/Modules/indexeddb/IDBDatabaseException.idl \
    $$PWD/Modules/indexeddb/IDBDatabase.idl \
    $$PWD/Modules/indexeddb/IDBFactory.idl \
    $$PWD/Modules/indexeddb/IDBIndex.idl \
    $$PWD/Modules/indexeddb/IDBKey.idl \
    $$PWD/Modules/indexeddb/IDBKeyRange.idl \
    $$PWD/Modules/indexeddb/IDBObjectStore.idl \
    $$PWD/Modules/indexeddb/IDBRequest.idl \
    $$PWD/Modules/indexeddb/IDBTransaction.idl \
    $$PWD/Modules/indexeddb/WorkerContextIndexedDatabase.idl \
    $$PWD/Modules/notifications/DOMWindowNotifications.idl \
    $$PWD/Modules/notifications/Notification.idl \
    $$PWD/Modules/notifications/NotificationCenter.idl \
    $$PWD/Modules/notifications/NotificationPermissionCallback.idl \
    $$PWD/Modules/notifications/WorkerContextNotifications.idl \
    $$PWD/Modules/quota/DOMWindowQuota.idl \
    $$PWD/Modules/quota/StorageInfo.idl \
    $$PWD/Modules/quota/StorageInfoErrorCallback.idl \
    $$PWD/Modules/quota/StorageInfoQuotaCallback.idl \
    $$PWD/Modules/quota/StorageInfoUsageCallback.idl \
    $$PWD/Modules/webaudio/AudioBuffer.idl \
    $$PWD/Modules/webaudio/AudioBufferSourceNode.idl \
    $$PWD/Modules/webaudio/AudioChannelMerger.idl \
    $$PWD/Modules/webaudio/AudioChannelSplitter.idl \
    $$PWD/Modules/webaudio/AudioContext.idl \
    $$PWD/Modules/webaudio/AudioDestinationNode.idl \
    $$PWD/Modules/webaudio/AudioGain.idl \
    $$PWD/Modules/webaudio/AudioGainNode.idl \
    $$PWD/Modules/webaudio/AudioListener.idl \
    $$PWD/Modules/webaudio/AudioNode.idl \
    $$PWD/Modules/webaudio/AudioPannerNode.idl \
    $$PWD/Modules/webaudio/AudioParam.idl \
    $$PWD/Modules/webaudio/AudioProcessingEvent.idl \
    $$PWD/Modules/webaudio/AudioSourceNode.idl \
    $$PWD/Modules/webaudio/ConvolverNode.idl \
    $$PWD/Modules/webaudio/DelayNode.idl \
    $$PWD/Modules/webaudio/DOMWindowWebAudio.idl \
    $$PWD/Modules/webaudio/JavaScriptAudioNode.idl \
    $$PWD/Modules/webaudio/RealtimeAnalyserNode.idl \
    $$PWD/Modules/webdatabase/DOMWindowWebDatabase.idl \
    $$PWD/Modules/webdatabase/Database.idl \
    $$PWD/Modules/webdatabase/DatabaseCallback.idl \
    $$PWD/Modules/webdatabase/DatabaseSync.idl \
    $$PWD/Modules/webdatabase/SQLError.idl \
    $$PWD/Modules/webdatabase/SQLException.idl \
    $$PWD/Modules/webdatabase/SQLResultSet.idl \
    $$PWD/Modules/webdatabase/SQLResultSetRowList.idl \
    $$PWD/Modules/webdatabase/SQLStatementCallback.idl \
    $$PWD/Modules/webdatabase/SQLStatementErrorCallback.idl \
    $$PWD/Modules/webdatabase/SQLTransaction.idl \
    $$PWD/Modules/webdatabase/SQLTransactionCallback.idl \
    $$PWD/Modules/webdatabase/SQLTransactionErrorCallback.idl \
    $$PWD/Modules/webdatabase/SQLTransactionSync.idl \
    $$PWD/Modules/webdatabase/SQLTransactionSyncCallback.idl \
    $$PWD/Modules/webdatabase/WorkerContextWebDatabase.idl \
    $$PWD/Modules/websockets/CloseEvent.idl \
    $$PWD/Modules/websockets/DOMWindowWebSocket.idl \
    $$PWD/Modules/websockets/WebSocket.idl \
    $$PWD/Modules/websockets/WorkerContextWebSocket.idl \
    $$PWD/css/Counter.idl \
    $$PWD/css/CSSCharsetRule.idl \
    $$PWD/css/CSSFontFaceRule.idl \
    $$PWD/css/CSSImportRule.idl \
    $$PWD/css/CSSMediaRule.idl \
    $$PWD/css/CSSPageRule.idl \
    $$PWD/css/CSSPrimitiveValue.idl \
    $$PWD/css/CSSRule.idl \
    $$PWD/css/CSSRuleList.idl \
    $$PWD/css/CSSStyleDeclaration.idl \
    $$PWD/css/CSSStyleRule.idl \
    $$PWD/css/CSSStyleSheet.idl \
    $$PWD/css/CSSValue.idl \
    $$PWD/css/CSSValueList.idl \
    $$PWD/css/MediaList.idl \
    $$PWD/css/MediaQueryList.idl \
    $$PWD/css/Rect.idl \
    $$PWD/css/RGBColor.idl \
    $$PWD/css/StyleMedia.idl \
    $$PWD/css/StyleSheet.idl \
    $$PWD/css/StyleSheetList.idl \
    $$PWD/css/WebKitCSSFilterValue.idl \
    $$PWD/css/WebKitCSSKeyframeRule.idl \
    $$PWD/css/WebKitCSSKeyframesRule.idl \
    $$PWD/css/WebKitCSSMatrix.idl \
    $$PWD/css/WebKitCSSRegionRule.idl \
    $$PWD/css/WebKitCSSTransformValue.idl \
    $$PWD/dom/Attr.idl \
    $$PWD/dom/BeforeLoadEvent.idl \
    $$PWD/dom/CharacterData.idl \
    $$PWD/dom/ClientRect.idl \
    $$PWD/dom/ClientRectList.idl \
    $$PWD/dom/Clipboard.idl \
    $$PWD/dom/CDATASection.idl \
    $$PWD/dom/Comment.idl \
    $$PWD/dom/CompositionEvent.idl \
    $$PWD/dom/CustomEvent.idl \
    $$PWD/dom/DataTransferItem.idl \
    $$PWD/dom/DataTransferItemList.idl \
    $$PWD/dom/DeviceMotionEvent.idl \
    $$PWD/dom/DeviceOrientationEvent.idl \
    $$PWD/dom/DocumentFragment.idl \
    $$PWD/dom/Document.idl \
    $$PWD/dom/DocumentType.idl \
    $$PWD/dom/DOMCoreException.idl \
    $$PWD/dom/DOMError.idl \
    $$PWD/dom/DOMImplementation.idl \
    $$PWD/dom/DOMStringList.idl \
    $$PWD/dom/DOMStringMap.idl \
    $$PWD/dom/Element.idl \
    $$PWD/dom/Entity.idl \
    $$PWD/dom/EntityReference.idl \
    $$PWD/dom/ErrorEvent.idl \
    $$PWD/dom/Event.idl \
    $$PWD/dom/EventException.idl \
#    $$PWD/dom/EventListener.idl \
    $$PWD/dom/EventTarget.idl \
    $$PWD/dom/HashChangeEvent.idl \
    $$PWD/dom/KeyboardEvent.idl \
    $$PWD/dom/MouseEvent.idl \
    $$PWD/dom/MessageChannel.idl \
    $$PWD/dom/MessageEvent.idl \
    $$PWD/dom/MessagePort.idl \
    $$PWD/dom/MutationEvent.idl \
    $$PWD/dom/NamedNodeMap.idl \
    $$PWD/dom/Node.idl \
    $$PWD/dom/NodeFilter.idl \
    $$PWD/dom/NodeIterator.idl \
    $$PWD/dom/NodeList.idl \
    $$PWD/dom/Notation.idl \
    $$PWD/dom/OverflowEvent.idl \
    $$PWD/dom/PageTransitionEvent.idl \
    $$PWD/dom/PopStateEvent.idl \
    $$PWD/dom/ProcessingInstruction.idl \
    $$PWD/dom/ProgressEvent.idl \
    $$PWD/dom/RangeException.idl \
    $$PWD/dom/Range.idl \
    $$PWD/dom/RequestAnimationFrameCallback.idl \
    $$PWD/dom/ShadowRoot.idl \
    $$PWD/dom/StringCallback.idl \
    $$PWD/dom/Text.idl \
    $$PWD/dom/TextEvent.idl \
    $$PWD/dom/Touch.idl \
    $$PWD/dom/TouchEvent.idl \
    $$PWD/dom/TouchList.idl \
    $$PWD/dom/TreeWalker.idl \
    $$PWD/dom/UIEvent.idl \
    $$PWD/dom/WebKitAnimationEvent.idl \
    $$PWD/dom/WebKitNamedFlow.idl \
    $$PWD/dom/WebKitTransitionEvent.idl \
    $$PWD/dom/WheelEvent.idl \
    $$PWD/editing/UndoManager.idl \
    $$PWD/fileapi/Blob.idl \
    $$PWD/fileapi/File.idl \
    $$PWD/fileapi/FileError.idl \
    $$PWD/fileapi/FileException.idl \
    $$PWD/fileapi/FileList.idl \
    $$PWD/fileapi/FileReader.idl \
    $$PWD/fileapi/FileReaderSync.idl \
    $$PWD/fileapi/WebKitBlobBuilder.idl \
    $$PWD/html/canvas/ArrayBufferView.idl \
    $$PWD/html/canvas/ArrayBuffer.idl \
    $$PWD/html/canvas/DataView.idl \
    $$PWD/html/canvas/Int8Array.idl \
    $$PWD/html/canvas/Float32Array.idl \
    $$PWD/html/canvas/Float64Array.idl \
    $$PWD/html/canvas/CanvasGradient.idl \
    $$PWD/html/canvas/Int32Array.idl \
    $$PWD/html/canvas/CanvasPattern.idl \
    $$PWD/html/canvas/CanvasRenderingContext.idl \
    $$PWD/html/canvas/CanvasRenderingContext2D.idl \
    $$PWD/html/canvas/EXTTextureFilterAnisotropic.idl \
    $$PWD/html/canvas/OESStandardDerivatives.idl \
    $$PWD/html/canvas/OESTextureFloat.idl \
    $$PWD/html/canvas/OESVertexArrayObject.idl \
    $$PWD/html/canvas/WebGLActiveInfo.idl \
    $$PWD/html/canvas/WebGLBuffer.idl \
    $$PWD/html/canvas/WebGLCompressedTextureS3TC.idl \
    $$PWD/html/canvas/WebGLContextAttributes.idl \
    $$PWD/html/canvas/WebGLContextEvent.idl \
    $$PWD/html/canvas/WebGLDebugRendererInfo.idl \
    $$PWD/html/canvas/WebGLDebugShaders.idl \
    $$PWD/html/canvas/WebGLDepthTexture.idl \
    $$PWD/html/canvas/WebGLFramebuffer.idl \
    $$PWD/html/canvas/WebGLLoseContext.idl \
    $$PWD/html/canvas/WebGLProgram.idl \
    $$PWD/html/canvas/WebGLRenderbuffer.idl \
    $$PWD/html/canvas/WebGLRenderingContext.idl \
    $$PWD/html/canvas/WebGLShader.idl \
    $$PWD/html/canvas/WebGLShaderPrecisionFormat.idl \
    $$PWD/html/canvas/Int16Array.idl \
    $$PWD/html/canvas/WebGLTexture.idl \
    $$PWD/html/canvas/WebGLUniformLocation.idl \
    $$PWD/html/canvas/WebGLVertexArrayObjectOES.idl \
    $$PWD/html/canvas/Uint8Array.idl \
    $$PWD/html/canvas/Uint8ClampedArray.idl \
    $$PWD/html/canvas/Uint32Array.idl \
    $$PWD/html/canvas/Uint16Array.idl \
    $$PWD/html/DOMFormData.idl \
    $$PWD/html/DOMSettableTokenList.idl \
    $$PWD/html/DOMTokenList.idl \
    $$PWD/html/DOMURL.idl \
    $$PWD/html/HTMLAllCollection.idl \
    $$PWD/html/HTMLAudioElement.idl \
    $$PWD/html/HTMLAnchorElement.idl \
    $$PWD/html/HTMLAppletElement.idl \
    $$PWD/html/HTMLAreaElement.idl \
    $$PWD/html/HTMLBaseElement.idl \
    $$PWD/html/HTMLBaseFontElement.idl \
    $$PWD/html/HTMLBodyElement.idl \
    $$PWD/html/HTMLBRElement.idl \
    $$PWD/html/HTMLButtonElement.idl \
    $$PWD/html/HTMLCanvasElement.idl \
    $$PWD/html/HTMLCollection.idl \
    $$PWD/html/HTMLDataListElement.idl \
    $$PWD/html/HTMLDetailsElement.idl \
    $$PWD/html/HTMLDialogElement.idl \
    $$PWD/html/HTMLDirectoryElement.idl \
    $$PWD/html/HTMLDivElement.idl \
    $$PWD/html/HTMLDListElement.idl \
    $$PWD/html/HTMLDocument.idl \
    $$PWD/html/HTMLElement.idl \
    $$PWD/html/HTMLEmbedElement.idl \
    $$PWD/html/HTMLFieldSetElement.idl \
    $$PWD/html/HTMLFontElement.idl \
    $$PWD/html/HTMLFormElement.idl \
    $$PWD/html/HTMLFrameElement.idl \
    $$PWD/html/HTMLFrameSetElement.idl \
    $$PWD/html/HTMLHeadElement.idl \
    $$PWD/html/HTMLHeadingElement.idl \
    $$PWD/html/HTMLHRElement.idl \
    $$PWD/html/HTMLHtmlElement.idl \
    $$PWD/html/HTMLIFrameElement.idl \
    $$PWD/html/HTMLImageElement.idl \
    $$PWD/html/HTMLInputElement.idl \
    $$PWD/html/HTMLKeygenElement.idl \
    $$PWD/html/HTMLLabelElement.idl \
    $$PWD/html/HTMLLegendElement.idl \
    $$PWD/html/HTMLLIElement.idl \
    $$PWD/html/HTMLLinkElement.idl \
    $$PWD/html/HTMLMapElement.idl \
    $$PWD/html/HTMLMarqueeElement.idl \
    $$PWD/html/HTMLMediaElement.idl \
    $$PWD/html/HTMLMenuElement.idl \
    $$PWD/html/HTMLMetaElement.idl \
    $$PWD/html/HTMLMeterElement.idl \
    $$PWD/html/HTMLModElement.idl \
    $$PWD/html/HTMLObjectElement.idl \
    $$PWD/html/HTMLOListElement.idl \
    $$PWD/html/HTMLOptGroupElement.idl \
    $$PWD/html/HTMLOptionElement.idl \
    $$PWD/html/HTMLOptionsCollection.idl \
    $$PWD/html/HTMLOutputElement.idl \
    $$PWD/html/HTMLParagraphElement.idl \
    $$PWD/html/HTMLParamElement.idl \
    $$PWD/html/HTMLPreElement.idl \
    $$PWD/html/HTMLProgressElement.idl \
    $$PWD/html/HTMLPropertiesCollection.idl \
    $$PWD/html/HTMLQuoteElement.idl \
    $$PWD/html/HTMLScriptElement.idl \
    $$PWD/html/HTMLSelectElement.idl \
    $$PWD/html/HTMLSourceElement.idl \
    $$PWD/html/HTMLSpanElement.idl \
    $$PWD/html/HTMLStyleElement.idl \
    $$PWD/html/HTMLTableCaptionElement.idl \
    $$PWD/html/HTMLTableCellElement.idl \
    $$PWD/html/HTMLTableColElement.idl \
    $$PWD/html/HTMLTableElement.idl \
    $$PWD/html/HTMLTableRowElement.idl \
    $$PWD/html/HTMLTableSectionElement.idl \
    $$PWD/html/HTMLTextAreaElement.idl \
    $$PWD/html/HTMLTitleElement.idl \
    $$PWD/html/HTMLTrackElement.idl \
    $$PWD/html/HTMLUListElement.idl \
    $$PWD/html/HTMLUnknownElement.idl \
    $$PWD/html/HTMLVideoElement.idl \
    $$PWD/html/ImageData.idl \
    $$PWD/html/MediaController.idl \
    $$PWD/html/MediaError.idl \
    $$PWD/html/RadioNodeList.idl \
    $$PWD/html/TextMetrics.idl \
    $$PWD/html/TimeRanges.idl \
    $$PWD/html/ValidityState.idl \
    $$PWD/html/VoidCallback.idl \
    $$PWD/html/shadow/HTMLContentElement.idl \
    $$PWD/html/shadow/HTMLShadowElement.idl \
    $$PWD/inspector/InjectedScriptHost.idl \
    $$PWD/inspector/InspectorFrontendHost.idl \
    $$PWD/inspector/JavaScriptCallFrame.idl \
    $$PWD/inspector/ScriptProfile.idl \
    $$PWD/inspector/ScriptProfileNode.idl \
    $$PWD/loader/appcache/DOMApplicationCache.idl \
    $$PWD/page/BarInfo.idl \
    $$PWD/page/Console.idl \
    $$PWD/page/Coordinates.idl \
    $$PWD/page/Crypto.idl \
    $$PWD/page/DOMSelection.idl \
    $$PWD/page/DOMWindow.idl \
    $$PWD/page/EventSource.idl \
    $$PWD/page/History.idl \
    $$PWD/page/Location.idl \
    $$PWD/page/MemoryInfo.idl \
    $$PWD/page/Navigator.idl \
    $$PWD/page/Performance.idl \
    $$PWD/page/PerformanceEntry.idl \
    $$PWD/page/PerformanceEntryList.idl \
    $$PWD/page/PerformanceNavigation.idl \
    $$PWD/page/PerformanceResourceTiming.idl \
    $$PWD/page/PerformanceTiming.idl \
    $$PWD/page/Screen.idl \
    $$PWD/page/SpeechInputEvent.idl \
    $$PWD/page/SpeechInputResult.idl \
    $$PWD/page/SpeechInputResultList.idl \
    $$PWD/page/WebKitAnimation.idl \
    $$PWD/page/WebKitAnimationList.idl \
    $$PWD/page/WebKitPoint.idl \
    $$PWD/page/WorkerNavigator.idl \
    $$PWD/plugins/DOMPlugin.idl \
    $$PWD/plugins/DOMMimeType.idl \
    $$PWD/plugins/DOMPluginArray.idl \
    $$PWD/plugins/DOMMimeTypeArray.idl \
    $$PWD/storage/Storage.idl \
    $$PWD/storage/StorageEvent.idl \
    $$PWD/testing/Internals.idl \
    $$PWD/testing/InternalSettings.idl \
    $$PWD/workers/AbstractWorker.idl \
    $$PWD/workers/DedicatedWorkerContext.idl \
    $$PWD/workers/SharedWorker.idl \
    $$PWD/workers/SharedWorkerContext.idl \
    $$PWD/workers/Worker.idl \
    $$PWD/workers/WorkerContext.idl \
    $$PWD/workers/WorkerLocation.idl \
    $$PWD/xml/DOMParser.idl \
    $$PWD/xml/XMLHttpRequest.idl \
    $$PWD/xml/XMLHttpRequestException.idl \
    $$PWD/xml/XMLHttpRequestProgressEvent.idl \
    $$PWD/xml/XMLHttpRequestUpload.idl \
    $$PWD/xml/XMLSerializer.idl \
    $$PWD/xml/XPathNSResolver.idl \
    $$PWD/xml/XPathException.idl \
    $$PWD/xml/XPathExpression.idl \
    $$PWD/xml/XPathResult.idl \
    $$PWD/xml/XPathEvaluator.idl \
    $$PWD/xml/XSLTProcessor.idl

contains(DEFINES, ENABLE_SVG=1) {
  IDL_BINDINGS += \
    $$PWD/svg/SVGAElement.idl \
    $$PWD/svg/SVGAltGlyphDefElement.idl \
    $$PWD/svg/SVGAltGlyphElement.idl \
    $$PWD/svg/SVGAltGlyphItemElement.idl \
    $$PWD/svg/SVGAngle.idl \
    $$PWD/svg/SVGAnimateColorElement.idl \
    $$PWD/svg/SVGAnimateMotionElement.idl \
    $$PWD/svg/SVGAnimatedAngle.idl \
    $$PWD/svg/SVGAnimatedBoolean.idl \
    $$PWD/svg/SVGAnimatedEnumeration.idl \
    $$PWD/svg/SVGAnimatedInteger.idl \
    $$PWD/svg/SVGAnimatedLength.idl \
    $$PWD/svg/SVGAnimatedLengthList.idl \
    $$PWD/svg/SVGAnimatedNumber.idl \
    $$PWD/svg/SVGAnimatedNumberList.idl \
    $$PWD/svg/SVGAnimatedPreserveAspectRatio.idl \
    $$PWD/svg/SVGAnimatedRect.idl \
    $$PWD/svg/SVGAnimatedString.idl \
    $$PWD/svg/SVGAnimatedTransformList.idl \
    $$PWD/svg/SVGAnimateElement.idl \
    $$PWD/svg/SVGAnimateTransformElement.idl \
    $$PWD/svg/SVGAnimationElement.idl \
    $$PWD/svg/SVGCircleElement.idl \
    $$PWD/svg/SVGClipPathElement.idl \
    $$PWD/svg/SVGColor.idl \
    $$PWD/svg/SVGComponentTransferFunctionElement.idl \
    $$PWD/svg/SVGCursorElement.idl \
    $$PWD/svg/SVGDefsElement.idl \
    $$PWD/svg/SVGDescElement.idl \
    $$PWD/svg/SVGDocument.idl \
    $$PWD/svg/SVGElement.idl \
    $$PWD/svg/SVGElementInstance.idl \
    $$PWD/svg/SVGElementInstanceList.idl \
    $$PWD/svg/SVGEllipseElement.idl \
    $$PWD/svg/SVGException.idl \
    $$PWD/svg/SVGFEBlendElement.idl \
    $$PWD/svg/SVGFEColorMatrixElement.idl \
    $$PWD/svg/SVGFEComponentTransferElement.idl \
    $$PWD/svg/SVGFECompositeElement.idl \
    $$PWD/svg/SVGFEConvolveMatrixElement.idl \
    $$PWD/svg/SVGFEDiffuseLightingElement.idl \
    $$PWD/svg/SVGFEDisplacementMapElement.idl \
    $$PWD/svg/SVGFEDistantLightElement.idl \
    $$PWD/svg/SVGFEDropShadowElement.idl \
    $$PWD/svg/SVGFEFloodElement.idl \
    $$PWD/svg/SVGFEFuncAElement.idl \
    $$PWD/svg/SVGFEFuncBElement.idl \
    $$PWD/svg/SVGFEFuncGElement.idl \
    $$PWD/svg/SVGFEFuncRElement.idl \
    $$PWD/svg/SVGFEGaussianBlurElement.idl \
    $$PWD/svg/SVGFEImageElement.idl \
    $$PWD/svg/SVGFEMergeElement.idl \
    $$PWD/svg/SVGFEMergeNodeElement.idl \
    $$PWD/svg/SVGFEMorphologyElement.idl \
    $$PWD/svg/SVGFEOffsetElement.idl \
    $$PWD/svg/SVGFEPointLightElement.idl \
    $$PWD/svg/SVGFESpecularLightingElement.idl \
    $$PWD/svg/SVGFESpotLightElement.idl \
    $$PWD/svg/SVGFETileElement.idl \
    $$PWD/svg/SVGFETurbulenceElement.idl \
    $$PWD/svg/SVGFilterElement.idl \
    $$PWD/svg/SVGFontElement.idl \
    $$PWD/svg/SVGFontFaceElement.idl \
    $$PWD/svg/SVGFontFaceFormatElement.idl \
    $$PWD/svg/SVGFontFaceNameElement.idl \
    $$PWD/svg/SVGFontFaceSrcElement.idl \
    $$PWD/svg/SVGFontFaceUriElement.idl \
    $$PWD/svg/SVGForeignObjectElement.idl \
    $$PWD/svg/SVGGElement.idl \
    $$PWD/svg/SVGGlyphElement.idl \
    $$PWD/svg/SVGGlyphRefElement.idl \
    $$PWD/svg/SVGGradientElement.idl \
    $$PWD/svg/SVGHKernElement.idl \
    $$PWD/svg/SVGImageElement.idl \
    $$PWD/svg/SVGLength.idl \
    $$PWD/svg/SVGLengthList.idl \
    $$PWD/svg/SVGLinearGradientElement.idl \
    $$PWD/svg/SVGLineElement.idl \
    $$PWD/svg/SVGMarkerElement.idl \
    $$PWD/svg/SVGMaskElement.idl \
    $$PWD/svg/SVGMatrix.idl \
    $$PWD/svg/SVGMetadataElement.idl \
    $$PWD/svg/SVGMissingGlyphElement.idl \
    $$PWD/svg/SVGMPathElement.idl \
    $$PWD/svg/SVGNumber.idl \
    $$PWD/svg/SVGNumberList.idl \
    $$PWD/svg/SVGPaint.idl \
    $$PWD/svg/SVGPathElement.idl \
    $$PWD/svg/SVGPathSegArcAbs.idl \
    $$PWD/svg/SVGPathSegArcRel.idl \
    $$PWD/svg/SVGPathSegClosePath.idl \
    $$PWD/svg/SVGPathSegCurvetoCubicAbs.idl \
    $$PWD/svg/SVGPathSegCurvetoCubicRel.idl \
    $$PWD/svg/SVGPathSegCurvetoCubicSmoothAbs.idl \
    $$PWD/svg/SVGPathSegCurvetoCubicSmoothRel.idl \
    $$PWD/svg/SVGPathSegCurvetoQuadraticAbs.idl \
    $$PWD/svg/SVGPathSegCurvetoQuadraticRel.idl \
    $$PWD/svg/SVGPathSegCurvetoQuadraticSmoothAbs.idl \
    $$PWD/svg/SVGPathSegCurvetoQuadraticSmoothRel.idl \
    $$PWD/svg/SVGPathSeg.idl \
    $$PWD/svg/SVGPathSegLinetoAbs.idl \
    $$PWD/svg/SVGPathSegLinetoHorizontalAbs.idl \
    $$PWD/svg/SVGPathSegLinetoHorizontalRel.idl \
    $$PWD/svg/SVGPathSegLinetoRel.idl \
    $$PWD/svg/SVGPathSegLinetoVerticalAbs.idl \
    $$PWD/svg/SVGPathSegLinetoVerticalRel.idl \
    $$PWD/svg/SVGPathSegList.idl \
    $$PWD/svg/SVGPathSegMovetoAbs.idl \
    $$PWD/svg/SVGPathSegMovetoRel.idl \
    $$PWD/svg/SVGPatternElement.idl \
    $$PWD/svg/SVGPoint.idl \
    $$PWD/svg/SVGPointList.idl \
    $$PWD/svg/SVGPolygonElement.idl \
    $$PWD/svg/SVGPolylineElement.idl \
    $$PWD/svg/SVGPreserveAspectRatio.idl \
    $$PWD/svg/SVGRadialGradientElement.idl \
    $$PWD/svg/SVGRectElement.idl \
    $$PWD/svg/SVGRect.idl \
    $$PWD/svg/SVGRenderingIntent.idl \
    $$PWD/svg/SVGScriptElement.idl \
    $$PWD/svg/SVGSetElement.idl \
    $$PWD/svg/SVGStopElement.idl \
    $$PWD/svg/SVGStringList.idl \
    $$PWD/svg/SVGStyleElement.idl \
    $$PWD/svg/SVGSVGElement.idl \
    $$PWD/svg/SVGSwitchElement.idl \
    $$PWD/svg/SVGSymbolElement.idl \
    $$PWD/svg/SVGTextContentElement.idl \
    $$PWD/svg/SVGTextElement.idl \
    $$PWD/svg/SVGTextPathElement.idl \
    $$PWD/svg/SVGTextPositioningElement.idl \
    $$PWD/svg/SVGTitleElement.idl \
    $$PWD/svg/SVGTransform.idl \
    $$PWD/svg/SVGTransformList.idl \
    $$PWD/svg/SVGTRefElement.idl \
    $$PWD/svg/SVGTSpanElement.idl \
    $$PWD/svg/SVGUnitTypes.idl \
    $$PWD/svg/SVGUseElement.idl \
    $$PWD/svg/SVGViewElement.idl \
    $$PWD/svg/SVGVKernElement.idl \
    $$PWD/svg/SVGViewSpec.idl \
    $$PWD/svg/SVGZoomAndPan.idl \
    $$PWD/svg/SVGZoomEvent.idl
}

contains(DEFINES, ENABLE_TOUCH_EVENTS=1) {
  IDL_BINDINGS_JAVA += \
    $$PWD/dom/Touch.idl \
    $$PWD/dom/TouchEvent.idl \
    $$PWD/dom/TouchList.idl
}

contains(DEFINES, ENABLE_VIDEO_TRACK=1) {
  IDL_BINDINGS += \
    $$PWD/html/track/TextTrack.idl \
    $$PWD/html/track/TextTrackCue.idl \
    $$PWD/html/track/TextTrackCueList.idl \
    $$PWD/html/track/TextTrackList.idl \
    $$PWD/html/track/TrackEvent.idl \
}

wrapperFactoryArg = --wrapperFactory

contains(DEFINES, ENABLE_MATHML=1) {
    mathmlnames.output = MathMLNames.cpp
    mathmlnames.input = MATHML_NAMES
    mathmlnames.depends = $$PWD/mathml/mathattrs.in
    mathmlnames.script = $$PWD/dom/make_names.pl
    mathmlnames.commands = perl -I$$PWD/bindings/scripts $$mathmlnames.script --tags $$PWD/mathml/mathtags.in --attrs $$PWD/mathml/mathattrs.in --extraDefines \"$${DEFINES}\" $${MOC_PREPROCESSOR} --factory $$wrapperFactoryArg --outputDir $${GENERATED_SOURCES_DIR}
    mathmlnames.extra_sources = MathMLElementFactory.cpp
    GENERATORS += mathmlnames
}

contains(DEFINES, ENABLE_SVG=1) {
    # GENERATOR 5-C:
    svgnames.output = SVGNames.cpp
    svgnames.input = SVG_NAMES
    svgnames.depends = $$PWD/svg/svgattrs.in
    svgnames.script = $$PWD/dom/make_names.pl
    svgnames.commands = perl -I$$PWD/bindings/scripts $$svgnames.script --tags $$PWD/svg/svgtags.in --attrs $$PWD/svg/svgattrs.in --extraDefines \"$${DEFINES}\" $${MOC_PREPROCESSOR} --factory $$wrapperFactoryArg --outputDir $${GENERATED_SOURCES_DIR}
    svgnames.extra_sources = SVGElementFactory.cpp
    svgnames.extra_sources += JSSVGElementWrapperFactory.cpp
    GENERATORS += svgnames
}

# GENERATOR 5-D:
xlinknames.output = XLinkNames.cpp
xlinknames.script = $$PWD/dom/make_names.pl
xlinknames.commands = perl -I$$PWD/bindings/scripts $$xlinknames.script --attrs $$PWD/svg/xlinkattrs.in $${MOC_PREPROCESSOR} --outputDir $${GENERATED_SOURCES_DIR}
xlinknames.input = XLINK_NAMES
GENERATORS += xlinknames

# GENERATOR 6-A:
cssprops.script = $$PWD/css/makeprop.pl
cssprops.output = CSSPropertyNames.cpp
cssprops.input = WALDOCSSPROPS
cssprops.commands = perl -ne \"print $1\" ${QMAKE_FILE_NAME} $${DASHBOARDSUPPORTCSSPROPERTIES} $${EXTRACSSPROPERTIES} > $${GENERATED_SOURCES_DIR}/${QMAKE_FILE_BASE}.in && cd $${GENERATED_SOURCES_DIR} && perl -I$$PWD/bindings/scripts $$cssprops.script --defines \"$${FEATURE_DEFINES}\" $${MOC_PREPROCESSOR} && $(DEL_FILE) ${QMAKE_FILE_BASE}.in ${QMAKE_FILE_BASE}.gperf
cssprops.depends = ${QMAKE_FILE_NAME} $${DASHBOARDSUPPORTCSSPROPERTIES} $${EXTRACSSPROPERTIES} $$cssprops.script
GENERATORS += cssprops

# GENERATOR 6-B:
cssvalues.script = $$PWD/css/makevalues.pl
cssvalues.output = CSSValueKeywords.cpp
cssvalues.input = WALDOCSSVALUES
cssvalues.commands = perl -ne \"print $1\" ${QMAKE_FILE_NAME} $$EXTRACSSVALUES > $${GENERATED_SOURCES_DIR}/${QMAKE_FILE_BASE}.in && cd $${GENERATED_SOURCES_DIR} && perl -I$$PWD/bindings/scripts $$cssvalues.script $${MOC_PREPROCESSOR} && $(DEL_FILE) ${QMAKE_FILE_BASE}.in ${QMAKE_FILE_BASE}.gperf
cssvalues.depends = ${QMAKE_FILE_NAME} $${EXTRACSSVALUES} $$cssvalues.script
cssvalues.clean = ${QMAKE_FILE_OUT} $${GENERATED_SOURCES_DIR}/${QMAKE_FILE_BASE}.h
GENERATORS += cssvalues

# GENERATOR 0: Resolve [Supplemental] dependency in IDLs
SUPPLEMENTAL_DEPENDENCY_FILE = $${GENERATED_SOURCES_DIR}/supplemental_dependency.tmp
IDL_FILES_TMP = $${GENERATED_SOURCES_DIR}/idl_files.tmp
PREPROCESS_IDLS_SCRIPT = $$PWD/bindings/scripts/preprocess-idls.pl
IDL_ATTRIBUTES_FILE = $$PWD/bindings/scripts/IDLAttributes.txt

preprocessIdls.input = IDL_ATTRIBUTES_FILE
preprocessIdls.script = $$PREPROCESS_IDLS_SCRIPT
# FIXME : We need to use only perl at some point.
EOC = $$escape_expand(\\n\\t)
win32-*: preprocessIdls.commands = type nul > $$IDL_FILES_TMP $$EOC
else: preprocessIdls.commands = cat /dev/null > $$IDL_FILES_TMP $$EOC
for(binding, IDL_BINDINGS) {
    # We need "$$binding" instead of "$$binding ", because Windows' echo writes trailing whitespaces. (http://wkb.ug/88304)
    preprocessIdls.commands += echo $$binding>> $$IDL_FILES_TMP $$EOC
}
preprocessIdls.commands += perl -I$$PWD/bindings/scripts $$preprocessIdls.script \
                               --defines \"LANGUAGE_JAVASCRIPT=1 $${FEATURE_DEFINES}\" \
                               --idlFilesList $$IDL_FILES_TMP \
                               --supplementalDependencyFile $$SUPPLEMENTAL_DEPENDENCY_FILE \
                               --idlAttributesFile $${IDL_ATTRIBUTES_FILE} \
                               $${MOC_PREPROCESSOR}
preprocessIdls.output = $$SUPPLEMENTAL_DEPENDENCY_FILE
preprocessIdls.add_output_to_sources = false
preprocessIdls.output_function = true
preprocessIdls.depends = $$PWD/bindings/scripts/IDLParser.pm $$IDL_BINDINGS
GENERATORS += preprocessIdls

# GENERATOR 1: IDL compiler
generateBindings.input = IDL_BINDINGS
generateBindings.script = $$PWD/bindings/scripts/generate-bindings.pl
generator = JS
generateBindings.commands = perl -I$$PWD/bindings/scripts $$generateBindings.script \
               --defines \"LANGUAGE_JAVASCRIPT=1 $${FEATURE_DEFINES}\" \
               --generator $$generator \
#               --include $$PWD/Modules/filesystem \
#               --include $$PWD/Modules/geolocation \
#               --include $$PWD/Modules/indexeddb \
#               --include $$PWD/Modules/quota \
               --include $$PWD/Modules/webaudio \
#               --include $$PWD/Modules/webdatabase \
#               --include $$PWD/Modules/websockets \
               --include $$PWD/dom \
               --include $$PWD/fileapi \
               --include $$PWD/html \
               --include $$PWD/xml \
               --include $$PWD/svg \
               --include $$PWD/storage \
               --include $$PWD/css \
               --include $$PWD/testing \
               --include $$PWD/workers \
               --outputDir $${GENERATED_SOURCES_DIR} \
               --supplementalDependencyFile $$SUPPLEMENTAL_DEPENDENCY_FILE \
               $${MOC_PREPROCESSOR} ${QMAKE_FILE_NAME}
generateBindings.output = JS${QMAKE_FILE_BASE}.cpp
generateBindings.depends = $$PWD/bindings/scripts/CodeGenerator.pm \
              $$PWD/bindings/scripts/CodeGeneratorJS.pm \
              $$PWD/bindings/scripts/IDLParser.pm \
              $$PWD/bindings/scripts/IDLStructure.pm \
              $$PWD/bindings/scripts/InFilesParser.pm \
              $$PWD/bindings/scripts/preprocessor.pm \
              $$SUPPLEMENTAL_DEPENDENCY_FILE
GENERATORS += generateBindings

# GENERATOR 1-A: IDL compiler for Java
idljava.input = IDL_BINDINGS_JAVA
idljava.script = $$PWD/bindings/scripts/generate-bindings.pl
generator = Java
idljava.commands = perl -I$$PWD/bindings/scripts $$idljava.script \
               --defines \"$${FEATURE_DEFINES}\" \
               --generator $$generator \
#               --include $$PWD/Modules/filesystem \
#               --include $$PWD/Modules/geolocation \
#               --include $$PWD/Modules/indexeddb \
#               --include $$PWD/Modules/quota \
               --include $$PWD/Modules/webaudio \
#               --include $$PWD/Modules/webdatabase \
#               --include $$PWD/Modules/websockets \
               --include $$PWD/dom \
               --include $$PWD/fileapi \
               --include $$PWD/html \
               --include $$PWD/xml \
               --include $$PWD/svg \
               --include $$PWD/storage \
               --include $$PWD/css \
               --include $$PWD/testing \
               --include $$PWD/workers \
               --outputDir $${GENERATED_SOURCES_DIR} \
               --supplementalDependencyFile $$SUPPLEMENTAL_DEPENDENCY_FILE \
               $${MOC_PREPROCESSOR} ${QMAKE_FILE_NAME}
idljava.depends = $$PWD/bindings/scripts/CodeGenerator.pm \
              $$PWD/bindings/scripts/CodeGeneratorJava.pm \
              $$PWD/bindings/scripts/IDLParser.pm \
              $$PWD/bindings/scripts/IDLStructure.pm \
              $$PWD/bindings/scripts/InFilesParser.pm \
              $$PWD/bindings/scripts/preprocessor.pm \
              $$SUPPLEMENTAL_DEPENDENCY_FILE
idljava.output = Java${QMAKE_FILE_BASE}.cpp
GENERATORS += idljava

# GENERATOR 2: inspector idl compiler
# Java port doesn't need these as it turns inspector off
#inspectorValidate.output = InspectorProtocolVersion.h
#inspectorValidate.input = INSPECTOR_JSON
#inspectorValidate.script = $$PWD/inspector/generate-inspector-protocol-version
#inspectorValidate.commands = python $$inspectorValidate.script -o ${QMAKE_FILE_OUT} ${QMAKE_FILE_IN}
#inspectorValidate.depends = $$PWD/inspector/generate-inspector-protocol-version
#inspectorValidate.add_output_to_sources = false
#GENERATORS += inspectorValidate

inspectorJSON.output = InspectorFrontend.cpp InspectorBackendDispatcher.cpp InspectorTypeBuilder.cpp
inspectorJSON.input = INSPECTOR_JSON
inspectorJSON.script = $$PWD/inspector/CodeGeneratorInspector.py
inspectorJSON.commands = python $$inspectorJSON.script $$PWD/inspector/Inspector.json --output_h_dir $${GENERATED_SOURCES_DIR} --output_cpp_dir $${GENERATED_SOURCES_DIR}
inspectorJSON.depends = $$inspectorJSON.script
GENERATORS += inspectorJSON

#inspectorBackendCommands.output = InspectorBackendCommands.qrc
#inspectorBackendCommands.input = INSPECTOR_BACKEND_COMMANDS_QRC
#inspectorBackendCommands.commands = $$QMAKE_COPY $$toSystemPath($$INSPECTOR_BACKEND_COMMANDS_QRC) ${QMAKE_FUNC_FILE_OUT_PATH}$${QMAKE_DIR_SEP}InspectorBackendCommands.qrc
#inspectorBackendCommands.add_output_to_sources = false
#GENERATORS += inspectorBackendCommands

# GENERATOR 2-a: inspector injected script source compiler
injectedScriptSource.output = InjectedScriptSource.h
injectedScriptSource.input = INJECTED_SCRIPT_SOURCE
injectedScriptSource.commands = perl $$PWD/inspector/xxd.pl InjectedScriptSource_js ${QMAKE_FILE_IN} ${QMAKE_FILE_OUT}
injectedScriptSource.add_output_to_sources = false
GENERATORS += injectedScriptSource

# GENERATOR 2-b: inspector webgl injected script source compiler
#InjectedScriptWebGLModuleSource.output = InjectedScriptWebGLModuleSource.h
#InjectedScriptWebGLModuleSource.input = INJECTED_SCRIPT_WEBGL_MODULE_SOURCE
#InjectedScriptWebGLModuleSource.commands = perl $$PWD/inspector/xxd.pl InjectedScriptWebGLModuleSource_js ${QMAKE_FILE_IN} ${QMAKE_FILE_OUT}
#InjectedScriptWebGLModuleSource.add_output_to_sources = false
#GENERATORS += InjectedScriptWebGLModuleSource
# GENERATOR 2-c: inspector debugger script source compiler
#debuggerScriptSource.output = DebuggerScriptSource.h
#debuggerScriptSource.input = DEBUGGER_SCRIPT_SOURCE
#debuggerScriptSource.commands = perl $$PWD/inspector/xxd.pl DebuggerScriptSource_js ${QMAKE_FILE_IN} ${QMAKE_FILE_OUT}
#debuggerScriptSource.add_output_to_sources = false
#GENERATORS += debuggerScriptSource


# GENERATOR 5-Java:
#eventfactoryjava.output = ../java/com/sun/webkit/dom/EventFactory.java
#eventfactoryjava.input = EVENTS_NAMES
#eventfactoryjava.script = $$PWD/dom/make_event_factory_java.pl
#eventfactoryjava.commands = perl -I$$PWD/bindings/scripts $$eventfactoryjava.script \
#                --defines \"$${FEATURE_DEFINES}\" \
#                --input $$EVENTS_NAMES \
#                --outputDir $${GENERATED_SOURCES_DIR}/../java/com/sun/webkit/dom
#eventfactoryjava.depends = $$PWD/dom/make_event_factory_java.pl $$EVENTS_NAMES
#GENERATORS += eventfactoryjava

# GENERATOR 3: tokenizer (flex)
#tokenizer.output = ${QMAKE_FILE_BASE}.cpp
#tokenizer.input = TOKENIZER
#tokenizer.script = $$PWD/css/maketokenizer
#tokenizer.commands = flex -t < ${QMAKE_FILE_NAME} | perl $$tokenizer.script > ${QMAKE_FILE_OUT}
# tokenizer.cpp is included into CSSParser.cpp
#tokenizer.add_output_to_sources = false
#GENERATORS += tokenizer

# GENERATOR 4: CSS grammar
cssbison.output = ${QMAKE_FILE_BASE}.cpp
cssbison.input = CSSBISON
cssbison.script = $$PWD/css/makegrammar.pl
cssbison.commands = perl $$cssbison.script ${QMAKE_FILE_NAME} $${GENERATED_SOURCES_DIR}/${QMAKE_FILE_BASE}
cssbison.depends = ${QMAKE_FILE_NAME}
GENERATORS += cssbison

# GENERATOR 5-A:
htmlnames.output = HTMLNames.cpp
htmlnames.input = HTML_NAMES
htmlnames.script = $$PWD/dom/make_names.pl
htmlnames.depends = $$PWD/html/HTMLAttributeNames.in
htmlnames.commands = perl -I$$PWD/bindings/scripts $$htmlnames.script --tags $$PWD/html/HTMLTagNames.in --attrs $$PWD/html/HTMLAttributeNames.in --extraDefines \"$${DEFINES}\" $${MOC_PREPROCESSOR}  --factory $$wrapperFactoryArg --outputDir $${GENERATED_SOURCES_DIR}
htmlnames.extra_sources = HTMLElementFactory.cpp
htmlnames.extra_sources += JSHTMLElementWrapperFactory.cpp
GENERATORS += htmlnames

# GENERATOR 5-B:
xmlnsnames.output = XMLNSNames.cpp
xmlnsnames.input = XMLNS_NAMES
xmlnsnames.script = $$PWD/dom/make_names.pl
xmlnsnames.commands = perl -I$$PWD/bindings/scripts $$xmlnsnames.script --attrs $$PWD/xml/xmlnsattrs.in $${MOC_PREPROCESSOR} --outputDir $${GENERATED_SOURCES_DIR}
GENERATORS += xmlnsnames

# GENERATOR 5-C:
xmlnames.output = XMLNames.cpp
xmlnames.input = XML_NAMES
xmlnames.script = $$PWD/dom/make_names.pl
xmlnames.commands = perl -I$$PWD/bindings/scripts $$xmlnames.script --attrs $$PWD/xml/xmlattrs.in $${MOC_PREPROCESSOR} --outputDir $${GENERATED_SOURCES_DIR}
GENERATORS += xmlnames

# GENERATOR 5-D:
fontnames.output = WebKitFontFamilyNames.cpp
fontnames.input = FONT_NAMES
fontnames.script = $$PWD/dom/make_names.pl
fontnames.commands = perl -I$$PWD/bindings/scripts $$fontnames.script --fonts $$FONT_NAMES --outputDir $${GENERATED_SOURCES_DIR}
entities.depends = $$PWD/dom/make_names.pl $$FONT_NAMES
GENERATORS += fontnames

# GENERATOR 5-E:
eventfactory.output = EventFactory.cpp
eventfactory.input = EVENTS_NAMES
eventfactory.script = $$PWD/dom/make_event_factory.pl
eventfactory.commands = perl -I$$PWD/bindings/scripts $$eventfactory.script --input $$EVENTS_NAMES --outputDir $${GENERATED_SOURCES_DIR}
eventfactory.depends = $$PWD/dom/make_event_factory.pl $$EVENTS_NAMES
GENERATORS += eventfactory

# GENERATOR 5-F:
eventtargetfactory.output = EventTargetInterfaces.h
eventtargetfactory.add_output_to_sources = false
eventtargetfactory.input = EVENT_TARGET_FACTORY
eventtargetfactory.script = $$PWD/dom/make_event_factory.pl
eventtargetfactory.commands = perl -I$$PWD/bindings/scripts $$eventfactory.script --input $$EVENT_TARGET_FACTORY --outputDir $${GENERATED_SOURCES_DIR}
eventtargetfactory.depends = $$PWD/dom/make_event_factory.pl $$EVENT_TARGET_FACTORY
GENERATORS += eventtargetfactory

# GENERATOR 5-G:
exceptioncodedescription.output = ExceptionCodeDescription.cpp
exceptioncodedescription.input = DOM_EXCEPTIONS
exceptioncodedescription.script = $$PWD/dom/make_dom_exceptions.pl
exceptioncodedescription.commands = perl -I$$PWD/bindings/scripts $$exceptioncodedescription.script --input $$DOM_EXCEPTIONS --outputDir $${GENERATED_SOURCES_DIR}
exceptioncodedescription.depends = $$PWD/dom/make_dom_exceptions.pl $$DOM_EXCEPTIONS
GENERATORS += exceptioncodedescription

# GENERATOR 8-A:
entities.output = HTMLEntityTable.cpp
entities.input = HTML_ENTITIES
entities.script = $$PWD/html/parser/create-html-entity-table
entities.commands = python $$entities.script -o ${QMAKE_FILE_OUT} $$HTML_ENTITIES
entities.clean = ${QMAKE_FILE_OUT}
entities.depends = $$PWD/html/parser/create-html-entity-table
GENERATORS += entities

# GENERATOR 8-B:
colordata.output = ColorData.cpp
colordata.input = COLORDATA_GPERF
colordata.script = $$PWD/make-hash-tools.pl
colordata.commands = perl $$colordata.script $${GENERATED_SOURCES_DIR} $$COLORDATA_GPERF
colordata.clean = ${QMAKE_FILE_OUT}
colordata.depends = $$PWD/make-hash-tools.pl
GENERATORS += colordata

contains(DEFINES, ENABLE_XSLT=1) {
    # GENERATOR 8-C:
    xmlviewercss.output = XMLViewerCSS.h
    xmlviewercss.input = XMLVIEWER_CSS
    xmlviewercss.script = $$PWD/inspector/xxd.pl
    xmlviewercss.commands = perl $$xmlviewercss.script XMLViewer_css $$XMLVIEWER_CSS ${QMAKE_FILE_OUT}
    xmlviewercss.clean = ${QMAKE_FILE_OUT}
    xmlviewercss.depends = $$PWD/inspector/xxd.pl
    xmlviewercss.add_output_to_sources = false
    GENERATORS += xmlviewercss

    # GENERATOR 8-D:
    xmlviewerjs.output = XMLViewerJS.h
    xmlviewerjs.input = XMLVIEWER_JS
    xmlviewerjs.script = $$PWD/inspector/xxd.pl
    xmlviewerjs.commands = perl $$xmlviewerjs.script XMLViewer_js $$XMLVIEWER_JS ${QMAKE_FILE_OUT}
    xmlviewerjs.clean = ${QMAKE_FILE_OUT}
    xmlviewerjs.depends = $$PWD/inspector/xxd.pl
    xmlviewerjs.add_output_to_sources = false
    GENERATORS += xmlviewerjs
}

# GENERATOR 9:
stylesheets.script = $$PWD/css/make-css-file-arrays.pl
stylesheets.output = UserAgentStyleSheetsData.cpp
stylesheets.input = stylesheets.script
stylesheets.commands = perl -I$$PWD/bindings/scripts $$stylesheets.script \
                        --defines \"$${FEATURE_DEFINES}\" \
                        $${MOC_PREPROCESSOR} \
                        $${GENERATED_SOURCES_DIR}/UserAgentStyleSheets.h \
                        ${QMAKE_FILE_OUT} \
                        $$STYLESHEETS_EMBED
stylesheets.depends = $$STYLESHEETS_EMBED
stylesheets.clean = ${QMAKE_FILE_OUT} $${GENERATED_SOURCES_DIR}/UserAgentStyleSheets.h
GENERATORS += stylesheets

# GENERATOR 10: XPATH grammar
xpathbison.output = ${QMAKE_FILE_BASE}.cpp
xpathbison.input = XPATHBISON
xpathbison.commands = bison -d -p xpathyy ${QMAKE_FILE_NAME} -o $${GENERATED_SOURCES_DIR}/${QMAKE_FILE_BASE}.tab.c && $(MOVE) $${GENERATED_SOURCES_DIR}$${QMAKE_DIR_SEP}${QMAKE_FILE_BASE}.tab.c $${GENERATED_SOURCES_DIR}$${QMAKE_DIR_SEP}${QMAKE_FILE_BASE}.cpp && $(MOVE) $${GENERATED_SOURCES_DIR}$${QMAKE_DIR_SEP}${QMAKE_FILE_BASE}.tab.h $${GENERATED_SOURCES_DIR}$${QMAKE_DIR_SEP}${QMAKE_FILE_BASE}.h
xpathbison.depends = ${QMAKE_FILE_NAME}
GENERATORS += xpathbison

# GENERATOR 11: WebKit Version
# The appropriate Apple-maintained Version.xcconfig file for WebKit version information is in Source/WebKit/mac/Configurations/.
webkitversion.script = $$PWD/../WebKit/scripts/generate-webkitversion.pl
webkitversion.output = WebKitVersion.h
webkitversion.input = webkitversion.script
webkitversion.commands = perl $$webkitversion.script --config $$PWD/../WebKit/mac/Configurations/Version.xcconfig --outputDir $${GENERATED_SOURCES_DIR}/
webkitversion.clean = $${GENERATED_SOURCES_DIR}/WebKitVersion.h
webkitversion.add_output_to_sources = false
GENERATORS += webkitversion

# GENERATOR 12: build.properties
javalibs.script = $$PWD/platform/java/javalibs.pl
javalibs.output = $$GENERATED_SOURCES_DIR/java/com/sun/webkit/build.properties
javalibs.input = javalibs.script
javalibs.commands = perl $$javalibs.script $$DEFINES > $$javalibs.output
javalibs.add_output_to_sources = false
javalibs.output_function = true
GENERATORS += javalibs

defineTest(prependEach) {
    unset(variable)
    unset(prefix)

    variable = $$1
    prefix = $$2

    original_values = $$unique($$variable)

    for(value, original_values) {
        values += $${prefix}$${value}
    }

    eval($$variable = $$values)
    export($$variable)

    return(true)
}

!contains(TEMPLATE, lib) {
    CONFIG -= debug_and_release
    for(generator, GENERATORS) {
        eval($${generator}.CONFIG = target_predeps no_link)
        eval($${generator}.dependency_type = TYPE_C)

        isEmpty($${generator}.output_function) {
            prependEach($${generator}.output, $${GENERATED_SOURCES_DIR}/)
        }

        script = $$eval($${generator}.script)
        eval($${generator}.depends += $$script)

        !isEmpty($${generator}.input) {
                # Compiler-style generator
                QMAKE_EXTRA_COMPILERS += $${generator}
                DEFAULT_TARGETS += compiler_$${generator}_make_all
        } else {
                # Regular target generator
                QMAKE_EXTRA_TARGETS += $${generator}
                DEFAULT_TARGETS += $${generator}
        }
        generated_files.depends += compiler_$${generator}_make_all
    }
} else {
    for(generator, GENERATORS) {
        isEqual($${generator}.add_output_to_sources, false): next()

        isEmpty($${generator}.output_function) {
            prependEach($${generator}.output, $${GENERATED_SOURCES_DIR}/)
        }

        output = $$eval($${generator}.output)
        input = $$eval($${generator}.input)
        input_files = $$eval($$input)

        for(input_file, input_files) {
            base = $$basename(input_file)
            base ~= s/\\..+//
            output_file = $$replace(output, \\$\\{QMAKE_FILE_BASE\\}, $$base)
            SOURCES += $$output_file
        }

        isEmpty($${generator}.output_function) {
            prependEach($${generator}.extra_sources, $${GENERATED_SOURCES_DIR}/)
        }

        SOURCES += $$eval($${generator}.extra_sources)
    }
}

