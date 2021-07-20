# generates header with JavaFX version
configure_file(java/WebCoreSupport/WebPageConfig.h.in ${DERIVED_SOURCES_WEBKITLEGACY_DIR}/WebPageConfig.h)

# Remove unused files
list(REMOVE_ITEM WebKitLegacy_SOURCES
    WebCoreSupport/WebViewGroup.cpp
)

list(APPEND WebKitLegacy_SOURCES
    # java/DOM/JavaDOMSelection.cpp
    # java/DOM/JavaWheelEvent.cpp
    java/DOM/JavaAttr.cpp
    java/DOM/JavaCDATASection.cpp
    java/DOM/JavaCSSCharsetRule.cpp
    java/DOM/JavaCSSFontFaceRule.cpp
    java/DOM/JavaCSSImportRule.cpp
    java/DOM/JavaCSSMediaRule.cpp
    java/DOM/JavaCSSPageRule.cpp
    java/DOM/JavaCSSPrimitiveValue.cpp
    java/DOM/JavaCSSRule.cpp
    java/DOM/JavaCSSRuleList.cpp
    java/DOM/JavaCSSStyleDeclaration.cpp
    java/DOM/JavaCSSStyleRule.cpp
    java/DOM/JavaCSSStyleSheet.cpp
    java/DOM/JavaCSSUnknownRule.cpp
    java/DOM/JavaCSSValue.cpp
    java/DOM/JavaCSSValueList.cpp
    java/DOM/JavaCharacterData.cpp
    java/DOM/JavaComment.cpp
    java/DOM/JavaCounter.cpp
    java/DOM/JavaDOMImplementation.cpp
    java/DOM/JavaDOMStringList.cpp
    java/DOM/JavaDOMWindow.cpp
    java/DOM/JavaDocument.cpp
    java/DOM/JavaDocumentFragment.cpp
    java/DOM/JavaDocumentType.cpp
    java/DOM/JavaElement.cpp
    java/DOM/JavaEntity.cpp
    java/DOM/JavaEntityReference.cpp
    java/DOM/JavaEvent.cpp
    java/DOM/JavaEventTarget.cpp
    java/DOM/JavaHTMLAnchorElement.cpp
    java/DOM/JavaHTMLAppletElement.cpp
    java/DOM/JavaHTMLAreaElement.cpp
    java/DOM/JavaHTMLBRElement.cpp
    java/DOM/JavaHTMLBaseElement.cpp
    java/DOM/JavaHTMLBaseFontElement.cpp
    java/DOM/JavaHTMLBodyElement.cpp
    java/DOM/JavaHTMLButtonElement.cpp
    java/DOM/JavaHTMLCollection.cpp
    java/DOM/JavaHTMLDListElement.cpp
    java/DOM/JavaHTMLDirectoryElement.cpp
    java/DOM/JavaHTMLDivElement.cpp
    java/DOM/JavaHTMLDocument.cpp
    java/DOM/JavaHTMLElement.cpp
    java/DOM/JavaHTMLFieldSetElement.cpp
    java/DOM/JavaHTMLFontElement.cpp
    java/DOM/JavaHTMLFormElement.cpp
    java/DOM/JavaHTMLFrameElement.cpp
    java/DOM/JavaHTMLFrameSetElement.cpp
    java/DOM/JavaHTMLHRElement.cpp
    java/DOM/JavaHTMLHeadElement.cpp
    java/DOM/JavaHTMLHeadingElement.cpp
    java/DOM/JavaHTMLHtmlElement.cpp
    java/DOM/JavaHTMLIFrameElement.cpp
    java/DOM/JavaHTMLImageElement.cpp
    java/DOM/JavaHTMLInputElement.cpp
    java/DOM/JavaHTMLLIElement.cpp
    java/DOM/JavaHTMLLabelElement.cpp
    java/DOM/JavaHTMLLegendElement.cpp
    java/DOM/JavaHTMLLinkElement.cpp
    java/DOM/JavaHTMLMapElement.cpp
    java/DOM/JavaHTMLMenuElement.cpp
    java/DOM/JavaHTMLMetaElement.cpp
    java/DOM/JavaHTMLModElement.cpp
    java/DOM/JavaHTMLOListElement.cpp
    java/DOM/JavaHTMLObjectElement.cpp
    java/DOM/JavaHTMLOptGroupElement.cpp
    java/DOM/JavaHTMLOptionElement.cpp
    java/DOM/JavaHTMLOptionsCollection.cpp
    java/DOM/JavaHTMLParagraphElement.cpp
    java/DOM/JavaHTMLParamElement.cpp
    java/DOM/JavaHTMLPreElement.cpp
    java/DOM/JavaHTMLQuoteElement.cpp
    java/DOM/JavaHTMLScriptElement.cpp
    java/DOM/JavaHTMLSelectElement.cpp
    java/DOM/JavaHTMLStyleElement.cpp
    java/DOM/JavaHTMLTableCaptionElement.cpp
    java/DOM/JavaHTMLTableCellElement.cpp
    java/DOM/JavaHTMLTableColElement.cpp
    java/DOM/JavaHTMLTableElement.cpp
    java/DOM/JavaHTMLTableRowElement.cpp
    java/DOM/JavaHTMLTableSectionElement.cpp
    java/DOM/JavaHTMLTextAreaElement.cpp
    java/DOM/JavaHTMLTitleElement.cpp
    java/DOM/JavaHTMLUListElement.cpp
    java/DOM/JavaKeyboardEvent.cpp
    java/DOM/JavaMediaList.cpp
    java/DOM/JavaMouseEvent.cpp
    java/DOM/JavaMutationEvent.cpp
    java/DOM/JavaNamedNodeMap.cpp
    java/DOM/JavaNode.cpp
    java/DOM/JavaNodeFilter.cpp
    java/DOM/JavaNodeIterator.cpp
    java/DOM/JavaNodeList.cpp
    java/DOM/JavaProcessingInstruction.cpp
    java/DOM/JavaRGBColor.cpp
    java/DOM/JavaRange.cpp
    java/DOM/JavaRect.cpp
    java/DOM/JavaStyleSheet.cpp
    java/DOM/JavaStyleSheetList.cpp
    java/DOM/JavaText.cpp
    java/DOM/JavaTreeWalker.cpp
    java/DOM/JavaUIEvent.cpp
    java/DOM/JavaXPathExpression.cpp
    java/DOM/JavaXPathNSResolver.cpp
    java/DOM/JavaXPathResult.cpp

    java/WebCoreSupport/ColorChooserJava.cpp
    java/WebCoreSupport/ContextMenuClientJava.cpp
    java/WebCoreSupport/PopupMenuJava.cpp
    java/WebCoreSupport/SearchPopupMenuJava.cpp
    java/WebCoreSupport/DragClientJava.cpp
    java/WebCoreSupport/EditorClientJava.cpp
    java/WebCoreSupport/FrameLoaderClientJava.cpp
    java/WebCoreSupport/ProgressTrackerClientJava.cpp
    java/WebCoreSupport/VisitedLinkStoreJava.cpp
    java/WebCoreSupport/InspectorClientJava.cpp
    java/WebCoreSupport/WebPage.cpp
    java/WebCoreSupport/PlatformStrategiesJava.cpp
    java/WebCoreSupport/ChromeClientJava.cpp
    java/WebCoreSupport/BackForwardList.cpp
    java/WebCoreSupport/PageCacheJava.cpp
)

# for DRT
list(APPEND WebKitLegacy_PRIVATE_LIBRARIES
    WebKit::WebCoreTestSupport
    ${ICU_I18N_LIBRARIES}
    ${ICU_DATA_LIBRARIES}
    ${ICU_LIBRARIES}
)

add_definitions(-DSTATICALLY_LINKED_WITH_JavaScriptCore)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

list(APPEND WebKitLegacy_PRIVATE_DEFINITIONS
    STATICALLY_LINKED_WITH_PAL
    STATICALLY_LINKED_WITH_WebCore
)

set(WebKitLegacy_OUTPUT_NAME "jfxwebkit")

if (MSVC)
    WEBKIT_ADD_PRECOMPILED_HEADER("WebKitPrefix.h" "java/WebCoreSupport/WebKitPrefix.cpp" WebKitLegacy_SOURCES)
else ()
    add_definitions("-include WebKitPrefix.h")
endif ()

if (APPLE)
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "-exported_symbols_list ${WEBCORE_DIR}/mapfile-macosx")
    set(WebKitLegacy_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-macosx")
elseif (UNIX)
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "-Xlinker -version-script=${WEBCORE_DIR}/mapfile-vers -Wl,--no-undefined")
    set(WebKitLegacy_EXTERNAL_DEP "${WEBCORE_DIR}/mapfile-vers")
elseif (WIN32)
    # Adds version information to jfxwebkit.dll created by Gradle build, see JDK-8166265
    set_target_properties(WebKitLegacy PROPERTIES LINK_FLAGS "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
    set(WebKitLegacy_EXTERNAL_DEP "${CMAKE_BINARY_DIR}/WebCore/obj/version.res")
endif ()

# Create a dummy depency c file to relink when mapfile changes
get_filename_component(STAMP_NAME ${WebKitLegacy_EXTERNAL_DEP} NAME)
set(WebKitLegacy_EXTERNAL_DEP_STAMP "${CMAKE_BINARY_DIR}/${STAMP_NAME}.stamp.cpp")
add_custom_command(
    OUTPUT "${WebKitLegacy_EXTERNAL_DEP_STAMP}"
    DEPENDS "${WebKitLegacy_EXTERNAL_DEP}"
    COMMAND ${CMAKE_COMMAND} -E touch "${WebKitLegacy_EXTERNAL_DEP_STAMP}"
    VERBATIM
)
list(APPEND WebKitLegacy_SOURCES ${WebKitLegacy_EXTERNAL_DEP_STAMP})

add_custom_command(
    OUTPUT ${DERIVED_SOURCES_WEBKITLEGACY_DIR}/WebKitVersion.h
    MAIN_DEPENDENCY ${WEBKITLEGACY_DIR}/scripts/generate-webkitversion.pl
    DEPENDS ${WEBKITLEGACY_DIR}/mac/Configurations/Version.xcconfig
    COMMAND ${PERL_EXECUTABLE} ${WEBKITLEGACY_DIR}/scripts/generate-webkitversion.pl --config ${WEBKITLEGACY_DIR}/mac/Configurations/Version.xcconfig --outputDir ${DERIVED_SOURCES_WEBKITLEGACY_DIR}
    VERBATIM)
list(APPEND WebKitLegacy_SOURCES ${DERIVED_SOURCES_WEBKITLEGACY_DIR}/WebKitVersion.h)


list(APPEND WebKitLegacy_INCLUDE_DIRECTORIES
    "${DERIVED_SOURCES_WEBKITLEGACY_DIR}"
    "${WEBKITLEGACY_DIR}/java/DOM"
    "${WEBKITLEGACY_DIR}/java/WebCoreSupport"
)
