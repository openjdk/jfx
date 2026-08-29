/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * GENERATED FILE -- do not edit.
 *
 * Produced by buildtools/ffm-web/spec-to-header.pl from the ABI spec that
 * buildtools/ffm-web/dom-cpp-to-ffm.pl derives from the DOM binding sources.
 * Regenerate rather than editing:
 *
 *   perl buildtools/ffm-web/dom-cpp-to-ffm.pl \
 *        --dir modules/javafx.web/src/main/native/Source/WebKitLegacy/java/DOM \
 *        --spec buildtools/ffm-web/dom-abi.tsv
 *   perl buildtools/ffm-web/spec-to-header.pl --spec buildtools/ffm-web/dom-abi.tsv \
 *        --header <this file>
 *
 * The DOM half of the flat C ABI: 1796 functions over 102 DOM types.
 * Every function takes the node pointer as int64_t and uses only <stdint.h> types.
 *
 * Strings are UTF-16 in both directions and are never library-owned, so there is no
 * lifetime rule to get wrong (FFM-ABI-CONTRACT.md section 13):
 *
 *   in   const uint16_t* s, int32_t s_len
 *        A NULL pointer is Java null. Note that the library collapses null and the
 *        empty string to WTF::emptyString(), exactly as String(JNIEnv*, jstring) did.
 *
 *   out  uint16_t* result_buf, int32_t result_cap, int32_t* result_length
 *        The caller supplies the buffer; the function returns WKJ_STR_OK,
 *        WKJ_STR_NULL (Java null) or WKJ_STR_OVERFLOW, and writes the length -- or,
 *        on overflow, the required capacity -- through result_length. Null and empty
 *        ARE distinguished here: WKJ_STR_NULL versus WKJ_STR_OK with length 0.
 *
 * Every function clears the calling thread's exception slot on entry, so a missed
 * check on the Java side cannot leak an exception into an unrelated later call.
 */

#ifndef WEBKIT_JAVA_API_DOM_H
#define WEBKIT_JAVA_API_DOM_H

#include "webkit_java_api.h"

#ifdef __cplusplus
extern "C" {
#endif


/* --- Attr --- */
WKJ_EXPORT int32_t wkj_dom_Attr_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Attr_getSpecified(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Attr_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Attr_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_Attr_getOwnerElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Attr_isId(int64_t peer);

/* --- CSSCharsetRule --- */
WKJ_EXPORT int32_t wkj_dom_CSSCharsetRule_getEncoding(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSCharsetRule_setEncoding(int64_t arg0, const uint16_t* arg1, int32_t arg1_length);

/* --- CSSFontFaceRule --- */
WKJ_EXPORT int64_t wkj_dom_CSSFontFaceRule_getStyle(int64_t peer);

/* --- CSSImportRule --- */
WKJ_EXPORT int32_t wkj_dom_CSSImportRule_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_CSSImportRule_getMedia(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSImportRule_getStyleSheet(int64_t peer);

/* --- CSSMediaRule --- */
WKJ_EXPORT int64_t wkj_dom_CSSMediaRule_getMedia(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSMediaRule_getCssRules(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSMediaRule_insertRule(int64_t peer, const uint16_t* rule, int32_t rule_length, int32_t index);
WKJ_EXPORT void wkj_dom_CSSMediaRule_deleteRule(int64_t peer, int32_t index);

/* --- CSSPageRule --- */
WKJ_EXPORT int32_t wkj_dom_CSSPageRule_getSelectorText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSPageRule_setSelectorText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_CSSPageRule_getStyle(int64_t peer);

/* --- CSSPrimitiveValue --- */
WKJ_EXPORT int16_t wkj_dom_CSSPrimitiveValue_getPrimitiveType(int64_t peer);
WKJ_EXPORT void wkj_dom_CSSPrimitiveValue_setFloatValue(int64_t peer, int16_t unitType, float floatValue);
WKJ_EXPORT float wkj_dom_CSSPrimitiveValue_getFloatValue(int64_t peer, int16_t unitType);
WKJ_EXPORT void wkj_dom_CSSPrimitiveValue_setStringValue(int64_t peer, int16_t stringType, const uint16_t* stringValue, int32_t stringValue_length);
WKJ_EXPORT int32_t wkj_dom_CSSPrimitiveValue_getStringValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_CSSPrimitiveValue_getCounterValue(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSPrimitiveValue_getRectValue(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSPrimitiveValue_getRGBColorValue(int64_t peer);

/* --- CSSRule --- */
WKJ_EXPORT void wkj_dom_CSSRule_dispose(int64_t peer);
WKJ_EXPORT int16_t wkj_dom_CSSRule_getType(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSRule_getCssText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSRule_setCssText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_CSSRule_getParentStyleSheet(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSRule_getParentRule(int64_t peer);

/* --- CSSRuleList --- */
WKJ_EXPORT void wkj_dom_CSSRuleList_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSRuleList_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSRuleList_item(int64_t peer, int32_t index);

/* --- CSSStyleDeclaration --- */
WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getCssText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_setCssText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSStyleDeclaration_getParentRule(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyValue(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_CSSStyleDeclaration_getPropertyCSSValue(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_removeProperty(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyPriority(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSStyleDeclaration_setProperty(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, const uint16_t* value, int32_t value_length, const uint16_t* priority, int32_t priority_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_item(int64_t peer, int32_t index, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_getPropertyShorthand(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_CSSStyleDeclaration_isPropertyImplicit(int64_t peer, const uint16_t* propertyName, int32_t propertyName_length);

/* --- CSSStyleRule --- */
WKJ_EXPORT int32_t wkj_dom_CSSStyleRule_getSelectorText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSStyleRule_setSelectorText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_CSSStyleRule_getStyle(int64_t peer);

/* --- CSSStyleSheet --- */
WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getOwnerRule(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getCssRules(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSStyleSheet_getRules(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSStyleSheet_insertRule(int64_t peer, const uint16_t* rule, int32_t rule_length, int32_t index);
WKJ_EXPORT void wkj_dom_CSSStyleSheet_deleteRule(int64_t peer, int32_t index);
WKJ_EXPORT int32_t wkj_dom_CSSStyleSheet_addRule(int64_t peer, const uint16_t* selector, int32_t selector_length, const uint16_t* style, int32_t style_length, int32_t index);
WKJ_EXPORT void wkj_dom_CSSStyleSheet_removeRule(int64_t peer, int32_t index);

/* --- CSSValue --- */
WKJ_EXPORT void wkj_dom_CSSValue_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CSSValue_getCssText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CSSValue_setCssText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int16_t wkj_dom_CSSValue_getCssValueType(int64_t peer);

/* --- CSSValueList --- */
WKJ_EXPORT int32_t wkj_dom_CSSValueList_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CSSValueList_item(int64_t peer, int32_t index);

/* --- CharacterData --- */
WKJ_EXPORT int32_t wkj_dom_CharacterData_getData(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CharacterData_setData(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_CharacterData_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CharacterData_getPreviousElementSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_CharacterData_getNextElementSibling(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_CharacterData_substringData(int64_t peer, int32_t offset, int32_t length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_CharacterData_appendData(int64_t peer, const uint16_t* data, int32_t data_length);
WKJ_EXPORT void wkj_dom_CharacterData_insertData(int64_t peer, int32_t offset, const uint16_t* data, int32_t data_length);
WKJ_EXPORT void wkj_dom_CharacterData_deleteData(int64_t peer, int32_t offset, int32_t length);
WKJ_EXPORT void wkj_dom_CharacterData_replaceData(int64_t peer, int32_t offset, int32_t length, const uint16_t* data, int32_t data_length);
WKJ_EXPORT void wkj_dom_CharacterData_remove(int64_t peer);

/* --- Counter --- */
WKJ_EXPORT void wkj_dom_Counter_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Counter_getIdentifier(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Counter_getListStyle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Counter_getSeparator(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

/* --- DOMImplementation --- */
WKJ_EXPORT void wkj_dom_DOMImplementation_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createDocumentType(int64_t peer, const uint16_t* qualifiedName, int32_t qualifiedName_length, const uint16_t* publicId, int32_t publicId_length, const uint16_t* systemId, int32_t systemId_length);
WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createDocument(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length, int64_t doctype);
WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createCSSStyleSheet(int64_t peer, const uint16_t* title, int32_t title_length, const uint16_t* media, int32_t media_length);
WKJ_EXPORT int64_t wkj_dom_DOMImplementation_createHTMLDocument(int64_t peer, const uint16_t* title, int32_t title_length);

/* --- DOMStringList --- */
WKJ_EXPORT void wkj_dom_DOMStringList_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMStringList_getLength(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMStringList_item(int64_t peer, int32_t index, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_DOMStringList_contains(int64_t peer, const uint16_t* string, int32_t string_length);

/* --- DOMWindow --- */
WKJ_EXPORT void wkj_dom_DOMWindow_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getFrameElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOffscreenBuffering(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOuterHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getOuterWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getInnerHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getInnerWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenLeft(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScreenTop(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScrollX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getScrollY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getPageXOffset(int64_t arg0);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getPageYOffset(int64_t arg0);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getClosed(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getLength(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_DOMWindow_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getStatus(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_DOMWindow_setStatus(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_getDefaultStatus(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_DOMWindow_setDefaultStatus(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getSelf(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getWindow(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getFrames(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOpener(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getParent(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getTop(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getDocumentEx(int64_t peer);
WKJ_EXPORT double wkj_dom_DOMWindow_getDevicePixelRatio(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationiteration(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationiteration(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnanimationstart(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnanimationstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOntransitionend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOntransitionend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationiteration(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationiteration(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkitanimationstart(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkitanimationstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwebkittransitionend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwebkittransitionend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnabort(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnabort(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnblur(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnblur(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncanplay(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOncanplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncanplaythrough(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOncanplaythrough(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnchange(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnclick(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOncontextmenu(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOncontextmenu(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndblclick(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndblclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndrag(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndrag(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndragend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragenter(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndragenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragleave(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndragleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragover(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndragover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndragstart(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndragstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndrop(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndrop(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOndurationchange(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOndurationchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnemptied(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnemptied(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnended(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnended(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnerror(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnerror(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnfocus(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnfocus(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOninput(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOninput(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOninvalid(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOninvalid(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeydown(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeydown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeypress(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeypress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnkeyup(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnkeyup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnload(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadeddata(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadeddata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadedmetadata(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadedmetadata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnloadstart(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnloadstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousedown(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousedown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseenter(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseleave(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousemove(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousemove(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseout(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseover(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmouseup(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmouseup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmousewheel(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmousewheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpause(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnpause(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnplay(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnplaying(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnplaying(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnprogress(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnprogress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnratechange(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnratechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnreset(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnreset(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnresize(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnresize(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnscroll(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnscroll(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnseeked(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnseeked(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnseeking(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnseeking(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnselect(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnselect(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnstalled(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnstalled(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnsubmit(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnsubmit(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnsuspend(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnsuspend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOntimeupdate(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOntimeupdate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnvolumechange(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnvolumechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwaiting(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwaiting(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnwheel(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnwheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnbeforeunload(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnbeforeunload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnhashchange(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnhashchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnmessage(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnmessage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnoffline(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnoffline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnonline(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnonline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpagehide(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnpagehide(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpageshow(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnpageshow(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnpopstate(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnpopstate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnstorage(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnstorage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getOnunload(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_setOnunload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getSelection(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_focus(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_blur(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_close(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_print(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_stop(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_alert(int64_t peer, const uint16_t* message, int32_t message_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_confirm(int64_t peer, const uint16_t* message, int32_t message_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_prompt(int64_t peer, const uint16_t* message, int32_t message_length, const uint16_t* defaultValue, int32_t defaultValue_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_find(int64_t peer, const uint16_t* string, int32_t string_length, int32_t caseSensitive, int32_t backwards, int32_t wrap, int32_t wholeWord, int32_t searchInFrames, int32_t showDialog);
WKJ_EXPORT void wkj_dom_DOMWindow_scrollBy(int64_t peer, int32_t x, int32_t y);
WKJ_EXPORT void wkj_dom_DOMWindow_scrollTo(int64_t peer, int32_t x, int32_t y);
WKJ_EXPORT void wkj_dom_DOMWindow_scroll(int64_t peer, int32_t x, int32_t y);
WKJ_EXPORT void wkj_dom_DOMWindow_moveBy(int64_t peer, float x, float y);
WKJ_EXPORT void wkj_dom_DOMWindow_moveTo(int64_t peer, float x, float y);
WKJ_EXPORT void wkj_dom_DOMWindow_resizeBy(int64_t peer, float x, float y);
WKJ_EXPORT void wkj_dom_DOMWindow_resizeTo(int64_t peer, float width, float height);
WKJ_EXPORT int64_t wkj_dom_DOMWindow_getComputedStyle(int64_t peer, int64_t element, const uint16_t* pseudoElement, int32_t pseudoElement_length);
WKJ_EXPORT void wkj_dom_DOMWindow_captureEvents(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_releaseEvents(int64_t peer);
WKJ_EXPORT void wkj_dom_DOMWindow_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT void wkj_dom_DOMWindow_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_dispatchEvent(int64_t peer, int64_t event);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_atob(int64_t peer, const uint16_t* string, int32_t string_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_DOMWindow_btoa(int64_t peer, const uint16_t* string, int32_t string_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_DOMWindow_clearTimeout(int64_t peer, int32_t handle);
WKJ_EXPORT void wkj_dom_DOMWindow_clearInterval(int64_t peer, int32_t handle);

/* --- Document --- */
WKJ_EXPORT int32_t wkj_dom_Document_isHTMLDocument(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getDoctype(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getImplementation(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getDocumentElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getInputEncoding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getXmlEncoding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getXmlVersion(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Document_setXmlVersion(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Document_getXmlStandalone(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setXmlStandalone(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_Document_getDocumentURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Document_setDocumentURI(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_Document_getDefaultView(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getStyleSheets(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getContentType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Document_setTitle(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Document_getReferrer(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getDomain(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getURL(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getCookie(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Document_setCookie(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_Document_getBody(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setBody(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getHead(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getImages(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getApplets(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getLinks(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getForms(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getAnchors(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getLastModified(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getDefaultCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getReadyState(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getCharacterSet(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getPreferredStylesheetSet(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getSelectedStylesheetSet(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Document_setSelectedStylesheetSet(int64_t arg0, const uint16_t* arg1, int32_t arg1_length);
WKJ_EXPORT int64_t wkj_dom_Document_getActiveElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getCompatMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getVisibilityState(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Document_getHidden(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getCurrentScript(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Document_getScrollingElement(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforecopy(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnbeforecopy(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforecut(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnbeforecut(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnbeforepaste(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnbeforepaste(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOncopy(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOncopy(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOncut(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOncut(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnpaste(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnpaste(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnselectstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnselectstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnselectionchange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnselectionchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnreadystatechange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnreadystatechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnabort(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnabort(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnblur(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnblur(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOncanplay(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOncanplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOncanplaythrough(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOncanplaythrough(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnchange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnclick(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOncontextmenu(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOncontextmenu(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndblclick(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndblclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndrag(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndrag(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndragend(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndragend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndragenter(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndragenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndragleave(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndragleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndragover(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndragover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndragstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndragstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndrop(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndrop(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOndurationchange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOndurationchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnemptied(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnemptied(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnended(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnended(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnerror(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnerror(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnfocus(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnfocus(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOninput(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOninput(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOninvalid(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOninvalid(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnkeydown(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnkeydown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnkeypress(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnkeypress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnkeyup(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnkeyup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnload(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnloadeddata(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnloadeddata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnloadedmetadata(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnloadedmetadata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnloadstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnloadstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmousedown(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmousedown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseenter(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmouseenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseleave(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmouseleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmousemove(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmousemove(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseout(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmouseout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseover(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmouseover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmouseup(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmouseup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnmousewheel(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnmousewheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnpause(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnpause(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnplay(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnplaying(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnplaying(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnprogress(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnprogress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnratechange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnratechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnreset(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnreset(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnresize(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnresize(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnscroll(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnscroll(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnseeked(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnseeked(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnseeking(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnseeking(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnselect(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnselect(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnstalled(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnstalled(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnsubmit(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnsubmit(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnsuspend(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnsuspend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOntimeupdate(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOntimeupdate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnvolumechange(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnvolumechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnwaiting(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnwaiting(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getOnwheel(int64_t peer);
WKJ_EXPORT void wkj_dom_Document_setOnwheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Document_getChildren(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getFirstElementChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getLastElementChild(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Document_getChildElementCount(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_createElement(int64_t peer, const uint16_t* tagName, int32_t tagName_length);
WKJ_EXPORT int64_t wkj_dom_Document_createDocumentFragment(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_createTextNode(int64_t peer, const uint16_t* data, int32_t data_length);
WKJ_EXPORT int64_t wkj_dom_Document_createComment(int64_t peer, const uint16_t* data, int32_t data_length);
WKJ_EXPORT int64_t wkj_dom_Document_createCDATASection(int64_t peer, const uint16_t* data, int32_t data_length);
WKJ_EXPORT int64_t wkj_dom_Document_createProcessingInstruction(int64_t peer, const uint16_t* target, int32_t target_length, const uint16_t* data, int32_t data_length);
WKJ_EXPORT int64_t wkj_dom_Document_createAttribute(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int64_t wkj_dom_Document_createEntityReference(int64_t arg0, const uint16_t* arg1, int32_t arg1_length);
WKJ_EXPORT int64_t wkj_dom_Document_getElementsByTagName(int64_t peer, const uint16_t* tagname, int32_t tagname_length);
WKJ_EXPORT int64_t wkj_dom_Document_importNode(int64_t peer, int64_t importedNode, int32_t deep);
WKJ_EXPORT int64_t wkj_dom_Document_createElementNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length);
WKJ_EXPORT int64_t wkj_dom_Document_createAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length);
WKJ_EXPORT int64_t wkj_dom_Document_getElementsByTagNameNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT int64_t wkj_dom_Document_adoptNode(int64_t peer, int64_t source);
WKJ_EXPORT int64_t wkj_dom_Document_createEvent(int64_t peer, const uint16_t* eventType, int32_t eventType_length);
WKJ_EXPORT int64_t wkj_dom_Document_createRange(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_createNodeIterator(int64_t arg0, int64_t arg1, int32_t arg2, int64_t arg3, int32_t arg4);
WKJ_EXPORT int64_t wkj_dom_Document_createTreeWalker(int64_t arg0, int64_t arg1, int32_t arg2, int64_t arg3, int32_t arg4);
WKJ_EXPORT int64_t wkj_dom_Document_getOverrideStyle(int64_t arg0, int64_t arg1, const uint16_t* arg2, int32_t arg2_length);
WKJ_EXPORT int64_t wkj_dom_Document_createExpression(int64_t peer, const uint16_t* expression, int32_t expression_length, int64_t resolver);
WKJ_EXPORT int64_t wkj_dom_Document_createNSResolver(int64_t peer, int64_t nodeResolver);
WKJ_EXPORT int64_t wkj_dom_Document_evaluate(int64_t peer, const uint16_t* expression, int32_t expression_length, int64_t contextNode, int64_t resolver, int16_t type, int64_t inResult);
WKJ_EXPORT int32_t wkj_dom_Document_execCommand(int64_t peer, const uint16_t* command, int32_t command_length, int32_t userInterface, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Document_queryCommandEnabled(int64_t peer, const uint16_t* command, int32_t command_length);
WKJ_EXPORT int32_t wkj_dom_Document_queryCommandIndeterm(int64_t peer, const uint16_t* command, int32_t command_length);
WKJ_EXPORT int32_t wkj_dom_Document_queryCommandState(int64_t peer, const uint16_t* command, int32_t command_length);
WKJ_EXPORT int32_t wkj_dom_Document_queryCommandSupported(int64_t peer, const uint16_t* command, int32_t command_length);
WKJ_EXPORT int32_t wkj_dom_Document_queryCommandValue(int64_t peer, const uint16_t* command, int32_t command_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Document_getElementsByName(int64_t peer, const uint16_t* elementName, int32_t elementName_length);
WKJ_EXPORT int64_t wkj_dom_Document_elementFromPoint(int64_t peer, int32_t x, int32_t y);
WKJ_EXPORT int64_t wkj_dom_Document_caretRangeFromPoint(int64_t peer, int32_t x, int32_t y);
WKJ_EXPORT int64_t wkj_dom_Document_createCSSStyleDeclaration(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getElementsByClassName(int64_t peer, const uint16_t* classNames, int32_t classNames_length);
WKJ_EXPORT int32_t wkj_dom_Document_hasFocus(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Document_getElementById(int64_t peer, const uint16_t* elementId, int32_t elementId_length);
WKJ_EXPORT int64_t wkj_dom_Document_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT int64_t wkj_dom_Document_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length);

/* --- DocumentFragment --- */
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getChildren(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getFirstElementChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getLastElementChild(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_DocumentFragment_getChildElementCount(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_getElementById(int64_t peer, const uint16_t* elementId, int32_t elementId_length);
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT int64_t wkj_dom_DocumentFragment_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length);

/* --- DocumentType --- */
WKJ_EXPORT int32_t wkj_dom_DocumentType_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_DocumentType_getEntities(int64_t arg0);
WKJ_EXPORT int64_t wkj_dom_DocumentType_getNotations(int64_t arg0);
WKJ_EXPORT int32_t wkj_dom_DocumentType_getPublicId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_DocumentType_getSystemId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_DocumentType_getInternalSubset(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_DocumentType_remove(int64_t peer);

/* --- Element --- */
WKJ_EXPORT int32_t wkj_dom_Element_isHTMLElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getTagName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Element_getAttributes(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getStyle(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setId(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT double wkj_dom_Element_getOffsetLeft(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getOffsetTop(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getOffsetWidth(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getOffsetHeight(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getClientLeft(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getClientTop(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getClientWidth(int64_t peer);
WKJ_EXPORT double wkj_dom_Element_getClientHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getScrollLeft(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setScrollLeft(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_Element_getScrollTop(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setScrollTop(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_Element_getScrollWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getScrollHeight(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getOffsetParent(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getInnerHTML(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setInnerHTML(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Element_getOuterHTML(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setOuterHTML(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Element_getClassName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setClassName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforecopy(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnbeforecopy(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforecut(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnbeforecut(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforepaste(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnbeforepaste(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOncopy(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOncopy(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOncut(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOncut(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnpaste(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnpaste(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnselectstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnselectstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnanimationend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationiteration(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnanimationiteration(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnanimationstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnanimationstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOntransitionend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOntransitionend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationiteration(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationiteration(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkitanimationstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwebkitanimationstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwebkittransitionend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwebkittransitionend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnfocusin(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnfocusin(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnfocusout(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnfocusout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnbeforeload(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnbeforeload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnabort(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnabort(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnblur(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnblur(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOncanplay(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOncanplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOncanplaythrough(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOncanplaythrough(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnchange(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnclick(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOncontextmenu(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOncontextmenu(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndblclick(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndblclick(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndrag(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndrag(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndragend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndragend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndragenter(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndragenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndragleave(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndragleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndragover(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndragover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndragstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndragstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndrop(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndrop(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOndurationchange(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOndurationchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnemptied(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnemptied(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnended(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnended(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnerror(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnerror(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnfocus(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnfocus(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOninput(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOninput(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOninvalid(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOninvalid(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnkeydown(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnkeydown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnkeypress(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnkeypress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnkeyup(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnkeyup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnload(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnloadeddata(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnloadeddata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnloadedmetadata(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnloadedmetadata(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnloadstart(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnloadstart(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmousedown(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmousedown(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseenter(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmouseenter(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseleave(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmouseleave(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmousemove(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmousemove(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseout(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmouseout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseover(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmouseover(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmouseup(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmouseup(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnmousewheel(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnmousewheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnpause(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnpause(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnplay(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnplay(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnplaying(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnplaying(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnprogress(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnprogress(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnratechange(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnratechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnreset(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnreset(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnresize(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnresize(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnscroll(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnscroll(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnseeked(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnseeked(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnseeking(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnseeking(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnselect(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnselect(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnstalled(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnstalled(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnsubmit(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnsubmit(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnsuspend(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnsuspend(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOntimeupdate(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOntimeupdate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnvolumechange(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnvolumechange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwaiting(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwaiting(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getOnwheel(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_setOnwheel(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_Element_getPreviousElementSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getNextElementSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getChildren(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getFirstElementChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_getLastElementChild(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getChildElementCount(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getAttribute(int64_t peer, const uint16_t* name, int32_t name_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setAttribute(int64_t peer, const uint16_t* name, int32_t name_length, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_Element_removeAttribute(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int64_t wkj_dom_Element_getAttributeNode(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int64_t wkj_dom_Element_setAttributeNode(int64_t peer, int64_t newAttr);
WKJ_EXPORT int64_t wkj_dom_Element_removeAttributeNode(int64_t peer, int64_t oldAttr);
WKJ_EXPORT int64_t wkj_dom_Element_getElementsByTagName(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int32_t wkj_dom_Element_hasAttributes(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Element_getAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Element_setAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* qualifiedName, int32_t qualifiedName_length, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_Element_removeAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT int64_t wkj_dom_Element_getElementsByTagNameNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT int64_t wkj_dom_Element_getAttributeNodeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT int64_t wkj_dom_Element_setAttributeNodeNS(int64_t peer, int64_t newAttr);
WKJ_EXPORT int32_t wkj_dom_Element_hasAttribute(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int32_t wkj_dom_Element_hasAttributeNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT void wkj_dom_Element_focus(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_blur(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_scrollIntoView(int64_t peer, int32_t alignWithTop);
WKJ_EXPORT void wkj_dom_Element_scrollIntoViewIfNeeded(int64_t peer, int32_t centerIfNeeded);
WKJ_EXPORT int64_t wkj_dom_Element_getElementsByClassName(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int32_t wkj_dom_Element_matches(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT int64_t wkj_dom_Element_closest(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT int32_t wkj_dom_Element_webkitMatchesSelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT void wkj_dom_Element_webkitRequestFullScreen(int64_t peer, int16_t arg0);
WKJ_EXPORT void wkj_dom_Element_webkitRequestFullscreen(int64_t peer);
WKJ_EXPORT void wkj_dom_Element_remove(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Element_querySelector(int64_t peer, const uint16_t* selectors, int32_t selectors_length);
WKJ_EXPORT int64_t wkj_dom_Element_querySelectorAll(int64_t peer, const uint16_t* selectors, int32_t selectors_length);

/* --- Entity --- */
WKJ_EXPORT int32_t wkj_dom_Entity_getPublicId(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Entity_getSystemId(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Entity_getNotationName(int64_t arg0, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

/* --- Event --- */
WKJ_EXPORT void wkj_dom_Event_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getCPPType(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Event_getTarget(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Event_getCurrentTarget(int64_t peer);
WKJ_EXPORT int16_t wkj_dom_Event_getEventPhase(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getBubbles(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getCancelable(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Event_getTimeStamp(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getDefaultPrevented(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getIsTrusted(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Event_getSrcElement(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Event_getReturnValue(int64_t peer);
WKJ_EXPORT void wkj_dom_Event_setReturnValue(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_Event_getCancelBubble(int64_t peer);
WKJ_EXPORT void wkj_dom_Event_setCancelBubble(int64_t peer, int32_t value);
WKJ_EXPORT void wkj_dom_Event_stopPropagation(int64_t peer);
WKJ_EXPORT void wkj_dom_Event_preventDefault(int64_t peer);
WKJ_EXPORT void wkj_dom_Event_initEvent(int64_t peer, const uint16_t* eventTypeArg, int32_t eventTypeArg_length, int32_t canBubbleArg, int32_t cancelableArg);
WKJ_EXPORT void wkj_dom_Event_stopImmediatePropagation(int64_t peer);

/* --- EventTarget --- */
WKJ_EXPORT void wkj_dom_EventTarget_dispose(int64_t peer);
WKJ_EXPORT void wkj_dom_EventTarget_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT void wkj_dom_EventTarget_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT int32_t wkj_dom_EventTarget_dispatchEvent(int64_t peer, int64_t event);

/* --- HTMLAnchorElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setCharset(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getCoords(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setCoords(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getHreflang(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setHreflang(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getPing(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setPing(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getRel(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setRel(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getRev(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setRev(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getShape(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setShape(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setHref(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getProtocol(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setProtocol(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getUsername(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setUsername(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getPassword(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setPassword(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getHost(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setHost(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getHostname(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setHostname(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getPort(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setPort(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getPathname(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setPathname(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getSearch(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setSearch(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAnchorElement_getHash(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAnchorElement_setHash(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLAppletElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getArchive(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setArchive(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getCode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setCode(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getCodeBase(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setCodeBase(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getHspace(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setHspace(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getObject(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setObject(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getVspace(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setVspace(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLAppletElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAppletElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLAreaElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getCoords(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setCoords(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getNoHref(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setNoHref(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getPing(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setPing(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getRel(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setRel(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getShape(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setShape(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setHref(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getProtocol(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setProtocol(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getUsername(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setUsername(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getPassword(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setPassword(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getHost(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setHost(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getHostname(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setHostname(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getPort(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setPort(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getPathname(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setPathname(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getSearch(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setSearch(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLAreaElement_getHash(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLAreaElement_setHash(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLBRElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLBRElement_getClear(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBRElement_setClear(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLBaseElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLBaseElement_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBaseElement_setHref(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBaseElement_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBaseElement_setTarget(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLBaseFontElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLBaseFontElement_getColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBaseFontElement_setColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBaseFontElement_getFace(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBaseFontElement_setFace(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBaseFontElement_getSize(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBaseFontElement_setSize(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLBodyElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getALink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setALink(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getBackground(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setBackground(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getLink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setLink(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLBodyElement_getVLink(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setVLink(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnblur(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnblur(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnerror(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnerror(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocus(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocusin(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocusin(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnfocusout(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnfocusout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnresize(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnresize(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnscroll(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnscroll(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnselectionchange(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnselectionchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnbeforeunload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnbeforeunload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnhashchange(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnhashchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnmessage(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnmessage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnoffline(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnoffline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnonline(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnonline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpagehide(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpagehide(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpageshow(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpageshow(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnpopstate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnpopstate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnstorage(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnstorage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLBodyElement_getOnunload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLBodyElement_setOnunload(int64_t peer, int64_t value);

/* --- HTMLButtonElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getAutofocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setAutofocus(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLButtonElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getFormAction(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getFormEnctype(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getFormMethod(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getFormNoValidate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setFormNoValidate(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getFormTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setFormTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLButtonElement_getLabels(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLButtonElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);
WKJ_EXPORT void wkj_dom_HTMLButtonElement_click(int64_t peer);

/* --- HTMLCollection --- */
WKJ_EXPORT void wkj_dom_HTMLCollection_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLCollection_getCPPType(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLCollection_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLCollection_item(int64_t peer, int32_t index);
WKJ_EXPORT int64_t wkj_dom_HTMLCollection_namedItem(int64_t peer, const uint16_t* name, int32_t name_length);

/* --- HTMLDListElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLDListElement_getCompact(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDListElement_setCompact(int64_t peer, int32_t value);

/* --- HTMLDirectoryElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLDirectoryElement_getCompact(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDirectoryElement_setCompact(int64_t peer, int32_t value);

/* --- HTMLDivElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLDivElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDivElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLDocument --- */
WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getEmbeds(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getPlugins(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLDocument_getScripts(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getDir(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setDir(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getDesignMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setDesignMode(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getCompatMode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getFgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setFgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getAlinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setAlinkColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getLinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setLinkColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLDocument_getVlinkColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_setVlinkColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_open(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDocument_close(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDocument_write(int64_t peer, const uint16_t* text, int32_t text_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_writeln(int64_t peer, const uint16_t* text, int32_t text_length);
WKJ_EXPORT void wkj_dom_HTMLDocument_clear(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDocument_captureEvents(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLDocument_releaseEvents(int64_t peer);

/* --- HTMLElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setId(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setTitle(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getLang(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setLang(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTranslate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLElement_setTranslate(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getDir(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getDraggable(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLElement_setDraggable(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getWebkitdropzone(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setWebkitdropzone(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getHidden(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLElement_setHidden(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getInnerText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setInnerText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getOuterText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setOuterText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLElement_getChildren(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getContentEditable(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLElement_setContentEditable(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getIsContentEditable(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getSpellcheck(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLElement_setSpellcheck(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTitleDisplayString(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLElement_insertAdjacentElement(int64_t peer, const uint16_t* where, int32_t where_length, int64_t element);
WKJ_EXPORT void wkj_dom_HTMLElement_insertAdjacentHTML(int64_t peer, const uint16_t* where, int32_t where_length, const uint16_t* html, int32_t html_length);
WKJ_EXPORT void wkj_dom_HTMLElement_insertAdjacentText(int64_t peer, const uint16_t* where, int32_t where_length, const uint16_t* text, int32_t text_length);
WKJ_EXPORT void wkj_dom_HTMLElement_click(int64_t peer);

/* --- HTMLFieldSetElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFieldSetElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFieldSetElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFieldSetElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFieldSetElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFieldSetElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);

/* --- HTMLFontElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLFontElement_getColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFontElement_setColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFontElement_getFace(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFontElement_setFace(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFontElement_getSize(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFontElement_setSize(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLFormElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getAcceptCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFormElement_setAcceptCharset(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getAction(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFormElement_setAction(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getEncoding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getMethod(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFormElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getNoValidate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFormElement_setNoValidate(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFormElement_setTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLFormElement_getElements(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_getLength(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFormElement_submit(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFormElement_reset(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFormElement_checkValidity(int64_t peer);

/* --- HTMLFrameElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getFrameBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setFrameBorder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getLongDesc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setLongDesc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getMarginHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setMarginHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getMarginWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setMarginWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getNoResize(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setNoResize(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getScrolling(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setScrolling(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameElement_getContentDocument(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameElement_getContentWindow(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getLocation(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameElement_setLocation(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameElement_getHeight(int64_t peer);

/* --- HTMLFrameSetElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLFrameSetElement_getCols(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setCols(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLFrameSetElement_getRows(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setRows(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnblur(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnblur(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnerror(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnerror(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocus(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocusin(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocusin(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnfocusout(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnfocusout(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnresize(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnresize(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnscroll(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnscroll(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnbeforeunload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnbeforeunload(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnhashchange(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnhashchange(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnmessage(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnmessage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnoffline(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnoffline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnonline(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnonline(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpagehide(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpagehide(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpageshow(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpageshow(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnpopstate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnpopstate(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnstorage(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnstorage(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLFrameSetElement_getOnunload(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLFrameSetElement_setOnunload(int64_t peer, int64_t value);

/* --- HTMLHRElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLHRElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHRElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLHRElement_getNoShade(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLHRElement_setNoShade(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLHRElement_getSize(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHRElement_setSize(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLHRElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHRElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLHeadElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLHeadElement_getProfile(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHeadElement_setProfile(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLHeadingElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLHeadingElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHeadingElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLHtmlElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLHtmlElement_getVersion(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLHtmlElement_setVersion(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLIFrameElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getFrameBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setFrameBorder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getLongDesc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setLongDesc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getMarginHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setMarginHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getMarginWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setMarginWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getScrolling(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setScrolling(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getSrcdoc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setSrcdoc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLIFrameElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLIFrameElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLIFrameElement_getContentDocument(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLIFrameElement_getContentWindow(int64_t peer);

/* --- HTMLImageElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setBorder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getCrossOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getIsMap(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setIsMap(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getLongDesc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setLongDesc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getSrcset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setSrcset(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getSizes(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setSizes(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getCurrentSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getUseMap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setUseMap(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getComplete(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getLowsrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLImageElement_setLowsrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getNaturalHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getNaturalWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLImageElement_getY(int64_t peer);

/* --- HTMLInputElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAccept(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setAccept(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAlt(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setAlt(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAutofocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setAutofocus(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDefaultChecked(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setDefaultChecked(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getChecked(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setChecked(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDirName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setDirName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormAction(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormEnctype(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormMethod(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormNoValidate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setFormNoValidate(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getFormTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setFormTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getHeight(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getIndeterminate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setIndeterminate(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMax(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setMax(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMaxLength(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setMaxLength(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setMin(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getMultiple(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setMultiple(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getPattern(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setPattern(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getPlaceholder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setPlaceholder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getReadOnly(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setReadOnly(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getRequired(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setRequired(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getSize(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setSize(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getStep(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setStep(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getValueAsDate(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueAsDate(int64_t peer, int64_t value);
WKJ_EXPORT double wkj_dom_HTMLInputElement_getValueAsNumber(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueAsNumber(int64_t peer, double value);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getWidth(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLInputElement_getLabels(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getUseMap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setUseMap(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_stepUp(int64_t peer, int32_t n);
WKJ_EXPORT void wkj_dom_HTMLInputElement_stepDown(int64_t peer, int32_t n);
WKJ_EXPORT int32_t wkj_dom_HTMLInputElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_select(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setRangeText(int64_t peer, const uint16_t* replacement, int32_t replacement_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setRangeTextEx(int64_t peer, const uint16_t* replacement, int32_t replacement_length, int32_t start, int32_t end, const uint16_t* selectionMode, int32_t selectionMode_length);
WKJ_EXPORT void wkj_dom_HTMLInputElement_click(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLInputElement_setValueForUser(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLLIElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLLIElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLIElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_HTMLLIElement_setValue(int64_t peer, int32_t value);

/* --- HTMLLabelElement --- */
WKJ_EXPORT int64_t wkj_dom_HTMLLabelElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLLabelElement_getHtmlFor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLabelElement_setHtmlFor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLLabelElement_getControl(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLLabelElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLabelElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLLegendElement --- */
WKJ_EXPORT int64_t wkj_dom_HTMLLegendElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLLegendElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLegendElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLegendElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLegendElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLLinkElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setCharset(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setHref(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getHreflang(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setHreflang(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getMedia(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setMedia(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getRel(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setRel(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getRev(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setRev(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setTarget(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLLinkElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLLinkElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLLinkElement_getSheet(int64_t peer);

/* --- HTMLMapElement --- */
WKJ_EXPORT int64_t wkj_dom_HTMLMapElement_getAreas(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLMapElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLMapElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLMenuElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLMenuElement_getCompact(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLMenuElement_setCompact(int64_t peer, int32_t value);

/* --- HTMLMetaElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLMetaElement_getContent(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLMetaElement_setContent(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLMetaElement_getHttpEquiv(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLMetaElement_setHttpEquiv(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLMetaElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLMetaElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLMetaElement_getScheme(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLMetaElement_setScheme(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLModElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLModElement_getCite(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLModElement_setCite(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLModElement_getDateTime(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLModElement_setDateTime(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLOListElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getCompact(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOListElement_setCompact(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getStart(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getReversed(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOListElement_setReversed(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOListElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLOListElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLObjectElement --- */
WKJ_EXPORT int64_t wkj_dom_HTMLObjectElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getCode(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setCode(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getArchive(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setArchive(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setBorder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getCodeBase(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setCodeBase(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getCodeType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setCodeType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getData(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setData(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getDeclare(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setDeclare(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setHspace(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getStandby(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setStandby(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getUseMap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setUseMap(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setVspace(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLObjectElement_getContentDocument(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLObjectElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLObjectElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);

/* --- HTMLOptGroupElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLOptGroupElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptGroupElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOptGroupElement_getLabel(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLOptGroupElement_setLabel(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLOptionElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptionElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLOptionElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getLabel(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getDefaultSelected(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptionElement_setDefaultSelected(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getSelected(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptionElement_setSelected(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionElement_getIndex(int64_t peer);

/* --- HTMLOptionsCollection --- */
WKJ_EXPORT int32_t wkj_dom_HTMLOptionsCollection_getSelectedIndex(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_setSelectedIndex(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLOptionsCollection_getLength(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_setLength(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLOptionsCollection_namedItem(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT void wkj_dom_HTMLOptionsCollection_add(int64_t peer, int64_t option, int32_t index);
WKJ_EXPORT int64_t wkj_dom_HTMLOptionsCollection_item(int64_t peer, int32_t index);

/* --- HTMLParagraphElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLParagraphElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLParagraphElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLParamElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLParamElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLParamElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLParamElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLParamElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLParamElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLParamElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLParamElement_getValueType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLParamElement_setValueType(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLPreElement --- */
WKJ_EXPORT void wkj_dom_HTMLPreElement_setWidth(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLPreElement_getWrap(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLPreElement_setWrap(int64_t peer, int32_t value);

/* --- HTMLQuoteElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLQuoteElement_getCite(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLQuoteElement_setCite(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLScriptElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getHtmlFor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setHtmlFor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getEvent(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setEvent(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getCharset(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setCharset(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getAsync(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setAsync(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getDefer(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setDefer(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getSrc(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setSrc(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLScriptElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLScriptElement_getCrossOrigin(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

/* --- HTMLSelectElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getAutofocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setAutofocus(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getMultiple(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setMultiple(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getRequired(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setRequired(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getSize(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setSize(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getOptions(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getSelectedOptions(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getSelectedIndex(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setSelectedIndex(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getLabels(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_item(int64_t peer, int32_t index);
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_namedItem(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_add(int64_t peer, int64_t element, int64_t before);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_remove(int64_t peer, int32_t index);
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLSelectElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);

/* --- HTMLStyleElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLStyleElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLStyleElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLStyleElement_getMedia(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLStyleElement_setMedia(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLStyleElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLStyleElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLStyleElement_getSheet(int64_t peer);

/* --- HTMLTableCaptionElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTableCaptionElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCaptionElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLTableCellElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getCellIndex(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAxis(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAxis(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getCh(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setCh(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getChOff(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setChOff(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getColSpan(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setColSpan(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getRowSpan(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setRowSpan(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getHeaders(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setHeaders(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getHeight(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setHeight(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getNoWrap(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setNoWrap(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getVAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setVAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getAbbr(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setAbbr(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableCellElement_getScope(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableCellElement_setScope(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLTableColElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getCh(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setCh(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getChOff(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setChOff(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getSpan(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setSpan(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getVAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setVAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableColElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableColElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLTableElement --- */
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getCaption(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setCaption(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTHead(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setTHead(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTFoot(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setTFoot(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getRows(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_getTBodies(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getBorder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setBorder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getCellPadding(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setCellPadding(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getCellSpacing(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setCellSpacing(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getFrame(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setFrame(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getRules(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setRules(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getSummary(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setSummary(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableElement_getWidth(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableElement_setWidth(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTHead(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteTHead(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTFoot(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteTFoot(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createTBody(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_createCaption(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteCaption(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableElement_insertRow(int64_t peer, int32_t index);
WKJ_EXPORT void wkj_dom_HTMLTableElement_deleteRow(int64_t peer, int32_t index);

/* --- HTMLTableRowElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getRowIndex(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getSectionRowIndex(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableRowElement_getCells(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getBgColor(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_setBgColor(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getCh(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_setCh(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getChOff(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_setChOff(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableRowElement_getVAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_setVAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLTableRowElement_insertCell(int64_t peer, int32_t index);
WKJ_EXPORT void wkj_dom_HTMLTableRowElement_deleteCell(int64_t peer, int32_t index);

/* --- HTMLTableSectionElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTableSectionElement_getAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableSectionElement_setAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableSectionElement_getCh(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableSectionElement_setCh(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableSectionElement_getChOff(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableSectionElement_setChOff(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTableSectionElement_getVAlign(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTableSectionElement_setVAlign(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_HTMLTableSectionElement_getRows(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_HTMLTableSectionElement_insertRow(int64_t peer, int32_t index);
WKJ_EXPORT void wkj_dom_HTMLTableSectionElement_deleteRow(int64_t peer, int32_t index);

/* --- HTMLTextAreaElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAutofocus(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setAutofocus(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDirName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDirName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_HTMLTextAreaElement_getForm(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getMaxLength(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setMaxLength(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setName(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getPlaceholder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setPlaceholder(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getReadOnly(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setReadOnly(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getRequired(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRequired(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getRows(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRows(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getCols(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setCols(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getWrap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setWrap(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDefaultValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDefaultValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getTextLength(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getWillValidate(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_HTMLTextAreaElement_getLabels(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionStart(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionStart(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionEnd(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionEnd(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionDirection(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionDirection(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_checkValidity(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_select(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRangeText(int64_t peer, const uint16_t* replacement, int32_t replacement_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRangeTextEx(int64_t peer, const uint16_t* replacement, int32_t replacement_length, int32_t start, int32_t end, const uint16_t* selectionMode, int32_t selectionMode_length);
WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionRange(int64_t peer, int32_t start, int32_t end, const uint16_t* direction, int32_t direction_length);

/* --- HTMLTitleElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLTitleElement_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLTitleElement_setText(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- HTMLUListElement --- */
WKJ_EXPORT int32_t wkj_dom_HTMLUListElement_getCompact(int64_t peer);
WKJ_EXPORT void wkj_dom_HTMLUListElement_setCompact(int64_t peer, int32_t value);
WKJ_EXPORT int32_t wkj_dom_HTMLUListElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_HTMLUListElement_setType(int64_t peer, const uint16_t* value, int32_t value_length);

/* --- KeyboardEvent --- */
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyIdentifier(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getLocation(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyLocation(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getCtrlKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getShiftKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getAltKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getMetaKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getKeyCode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getCharCode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_KeyboardEvent_getModifierState(int64_t peer, const uint16_t* keyIdentifierArg, int32_t keyIdentifierArg_length);
WKJ_EXPORT void wkj_dom_KeyboardEvent_initKeyboardEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, const uint16_t* keyIdentifier, int32_t keyIdentifier_length, int32_t location, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey);
WKJ_EXPORT void wkj_dom_KeyboardEvent_initKeyboardEventEx(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, const uint16_t* keyIdentifier, int32_t keyIdentifier_length, int32_t location, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey);

/* --- MediaList --- */
WKJ_EXPORT void wkj_dom_MediaList_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MediaList_getMediaText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_MediaList_setMediaText(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_MediaList_getLength(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MediaList_item(int64_t peer, int32_t index, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_MediaList_deleteMedium(int64_t peer, const uint16_t* oldMedium, int32_t oldMedium_length);
WKJ_EXPORT void wkj_dom_MediaList_appendMedium(int64_t peer, const uint16_t* newMedium, int32_t newMedium_length);

/* --- MouseEvent --- */
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getScreenX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getScreenY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getClientX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getClientY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getCtrlKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getShiftKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getAltKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getMetaKey(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getButton(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_MouseEvent_getRelatedTarget(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getOffsetX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MouseEvent_getOffsetY(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_MouseEvent_getFromElement(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_MouseEvent_getToElement(int64_t peer);
WKJ_EXPORT void wkj_dom_MouseEvent_initMouseEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, int32_t detail, int32_t screenX, int32_t screenY, int32_t clientX, int32_t clientY, int32_t ctrlKey, int32_t altKey, int32_t shiftKey, int32_t metaKey, int16_t button, int64_t relatedTarget);

/* --- MutationEvent --- */
WKJ_EXPORT int64_t wkj_dom_MutationEvent_getRelatedNode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_MutationEvent_getPrevValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_MutationEvent_getNewValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_MutationEvent_getAttrName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int16_t wkj_dom_MutationEvent_getAttrChange(int64_t peer);
WKJ_EXPORT void wkj_dom_MutationEvent_initMutationEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t relatedNode, const uint16_t* prevValue, int32_t prevValue_length, const uint16_t* newValue, int32_t newValue_length, const uint16_t* attrName, int32_t attrName_length, int16_t attrChange);

/* --- NamedNodeMap --- */
WKJ_EXPORT void wkj_dom_NamedNodeMap_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_NamedNodeMap_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_getNamedItem(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_setNamedItem(int64_t peer, int64_t node);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_removeNamedItem(int64_t peer, const uint16_t* name, int32_t name_length);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_item(int64_t peer, int32_t index);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_getNamedItemNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_setNamedItemNS(int64_t peer, int64_t node);
WKJ_EXPORT int64_t wkj_dom_NamedNodeMap_removeNamedItemNS(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, const uint16_t* localName, int32_t localName_length);

/* --- Node --- */
WKJ_EXPORT void wkj_dom_Node_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Node_getNodeName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Node_getNodeValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Node_setNodeValue(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int16_t wkj_dom_Node_getNodeType(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getParentNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getChildNodes(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getFirstChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getLastChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getPreviousSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getNextSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_getOwnerDocument(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Node_getNamespaceURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Node_getPrefix(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Node_setPrefix(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int32_t wkj_dom_Node_getLocalName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Node_getAttributes(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Node_getBaseURI(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Node_getTextContent(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Node_setTextContent(int64_t peer, const uint16_t* value, int32_t value_length);
WKJ_EXPORT int64_t wkj_dom_Node_getParentElement(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_insertBefore(int64_t peer, int64_t newChild, int64_t refChild);
WKJ_EXPORT int64_t wkj_dom_Node_replaceChild(int64_t peer, int64_t newChild, int64_t oldChild);
WKJ_EXPORT int64_t wkj_dom_Node_removeChild(int64_t peer, int64_t oldChild);
WKJ_EXPORT int64_t wkj_dom_Node_appendChild(int64_t peer, int64_t newChild);
WKJ_EXPORT int32_t wkj_dom_Node_hasChildNodes(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Node_cloneNode(int64_t peer, int32_t deep);
WKJ_EXPORT void wkj_dom_Node_normalize(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Node_hasAttributes(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Node_isSameNode(int64_t peer, int64_t other);
WKJ_EXPORT int32_t wkj_dom_Node_isEqualNode(int64_t peer, int64_t other);
WKJ_EXPORT int32_t wkj_dom_Node_lookupPrefix(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_Node_isDefaultNamespace(int64_t peer, const uint16_t* namespaceURI, int32_t namespaceURI_length);
WKJ_EXPORT int32_t wkj_dom_Node_lookupNamespaceURI(int64_t peer, const uint16_t* prefix, int32_t prefix_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int16_t wkj_dom_Node_compareDocumentPosition(int64_t peer, int64_t other);
WKJ_EXPORT int32_t wkj_dom_Node_contains(int64_t peer, int64_t other);
WKJ_EXPORT void wkj_dom_Node_addEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT void wkj_dom_Node_removeEventListener(int64_t peer, const uint16_t* type, int32_t type_length, int64_t listener, int32_t useCapture);
WKJ_EXPORT int32_t wkj_dom_Node_dispatchEvent(int64_t peer, int64_t event);

/* --- NodeFilter --- */
WKJ_EXPORT void wkj_dom_NodeFilter_dispose(int64_t peer);
WKJ_EXPORT int16_t wkj_dom_NodeFilter_acceptNode(int64_t peer, int64_t n);

/* --- NodeIterator --- */
WKJ_EXPORT void wkj_dom_NodeIterator_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NodeIterator_getRoot(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_NodeIterator_getWhatToShow(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NodeIterator_getFilter(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_NodeIterator_getExpandEntityReferences(int64_t arg0);
WKJ_EXPORT int64_t wkj_dom_NodeIterator_getReferenceNode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_NodeIterator_getPointerBeforeReferenceNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NodeIterator_nextNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NodeIterator_previousNode(int64_t peer);
WKJ_EXPORT void wkj_dom_NodeIterator_detach(int64_t peer);

/* --- NodeList --- */
WKJ_EXPORT void wkj_dom_NodeList_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_NodeList_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_NodeList_item(int64_t peer, int32_t index);

/* --- ProcessingInstruction --- */
WKJ_EXPORT int32_t wkj_dom_ProcessingInstruction_getTarget(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_ProcessingInstruction_getSheet(int64_t peer);

/* --- RGBColor --- */
WKJ_EXPORT void wkj_dom_RGBColor_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_RGBColor_getRed(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_RGBColor_getGreen(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_RGBColor_getBlue(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_RGBColor_getAlpha(int64_t peer);

/* --- Range --- */
WKJ_EXPORT void wkj_dom_Range_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_getStartContainer(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Range_getStartOffset(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_getEndContainer(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Range_getEndOffset(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Range_getCollapsed(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_getCommonAncestorContainer(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Range_getText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Range_setStart(int64_t peer, int64_t refNode, int32_t offset);
WKJ_EXPORT void wkj_dom_Range_setEnd(int64_t peer, int64_t refNode, int32_t offset);
WKJ_EXPORT void wkj_dom_Range_setStartBefore(int64_t peer, int64_t refNode);
WKJ_EXPORT void wkj_dom_Range_setStartAfter(int64_t peer, int64_t refNode);
WKJ_EXPORT void wkj_dom_Range_setEndBefore(int64_t peer, int64_t refNode);
WKJ_EXPORT void wkj_dom_Range_setEndAfter(int64_t peer, int64_t refNode);
WKJ_EXPORT void wkj_dom_Range_collapse(int64_t peer, int32_t toStart);
WKJ_EXPORT void wkj_dom_Range_selectNode(int64_t peer, int64_t refNode);
WKJ_EXPORT void wkj_dom_Range_selectNodeContents(int64_t peer, int64_t refNode);
WKJ_EXPORT int16_t wkj_dom_Range_compareBoundaryPoints(int64_t peer, int16_t how, int64_t sourceRange);
WKJ_EXPORT void wkj_dom_Range_deleteContents(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_extractContents(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_cloneContents(int64_t peer);
WKJ_EXPORT void wkj_dom_Range_insertNode(int64_t peer, int64_t newNode);
WKJ_EXPORT void wkj_dom_Range_surroundContents(int64_t peer, int64_t newParent);
WKJ_EXPORT int64_t wkj_dom_Range_cloneRange(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_Range_toString(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT void wkj_dom_Range_detach(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Range_createContextualFragment(int64_t peer, const uint16_t* html, int32_t html_length);
WKJ_EXPORT int16_t wkj_dom_Range_compareNode(int64_t peer, int64_t refNode);
WKJ_EXPORT int16_t wkj_dom_Range_comparePoint(int64_t peer, int64_t refNode, int32_t offset);
WKJ_EXPORT int32_t wkj_dom_Range_intersectsNode(int64_t peer, int64_t refNode);
WKJ_EXPORT int32_t wkj_dom_Range_isPointInRange(int64_t peer, int64_t refNode, int32_t offset);
WKJ_EXPORT void wkj_dom_Range_expand(int64_t peer, const uint16_t* unit, int32_t unit_length);

/* --- Rect --- */
WKJ_EXPORT void wkj_dom_Rect_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Rect_getTop(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Rect_getRight(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Rect_getBottom(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_Rect_getLeft(int64_t peer);

/* --- StyleSheet --- */
WKJ_EXPORT void wkj_dom_StyleSheet_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getCPPType(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getDisabled(int64_t peer);
WKJ_EXPORT void wkj_dom_StyleSheet_setDisabled(int64_t peer, int32_t value);
WKJ_EXPORT int64_t wkj_dom_StyleSheet_getOwnerNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_StyleSheet_getParentStyleSheet(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getHref(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_StyleSheet_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_StyleSheet_getMedia(int64_t peer);

/* --- StyleSheetList --- */
WKJ_EXPORT void wkj_dom_StyleSheetList_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_StyleSheetList_getLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_StyleSheetList_item(int64_t peer, int32_t index);

/* --- Text --- */
WKJ_EXPORT int32_t wkj_dom_Text_getWholeText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int64_t wkj_dom_Text_splitText(int64_t peer, int32_t offset);
WKJ_EXPORT int64_t wkj_dom_Text_replaceWholeText(int64_t peer, const uint16_t* content, int32_t content_length);

/* --- TreeWalker --- */
WKJ_EXPORT void wkj_dom_TreeWalker_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_getRoot(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_TreeWalker_getWhatToShow(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_getFilter(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_TreeWalker_getExpandEntityReferences(int64_t arg0);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_getCurrentNode(int64_t peer);
WKJ_EXPORT void wkj_dom_TreeWalker_setCurrentNode(int64_t peer, int64_t value);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_parentNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_firstChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_lastChild(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_previousSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_nextSibling(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_previousNode(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_TreeWalker_nextNode(int64_t peer);

/* --- UIEvent --- */
WKJ_EXPORT int64_t wkj_dom_UIEvent_getView(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getDetail(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getKeyCode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getCharCode(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getLayerX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getLayerY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getPageX(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getPageY(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_UIEvent_getWhich(int64_t peer);
WKJ_EXPORT void wkj_dom_UIEvent_initUIEvent(int64_t peer, const uint16_t* type, int32_t type_length, int32_t canBubble, int32_t cancelable, int64_t view, int32_t detail);

/* --- XPathExpression --- */
WKJ_EXPORT void wkj_dom_XPathExpression_dispose(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_XPathExpression_evaluate(int64_t peer, int64_t contextNode, int16_t type, int64_t inResult);

/* --- XPathNSResolver --- */
WKJ_EXPORT void wkj_dom_XPathNSResolver_dispose(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_XPathNSResolver_lookupNamespaceURI(int64_t peer, const uint16_t* prefix, int32_t prefix_length, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);

/* --- XPathResult --- */
WKJ_EXPORT void wkj_dom_XPathResult_dispose(int64_t peer);
WKJ_EXPORT int16_t wkj_dom_XPathResult_getResultType(int64_t peer);
WKJ_EXPORT double wkj_dom_XPathResult_getNumberValue(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_XPathResult_getStringValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length);
WKJ_EXPORT int32_t wkj_dom_XPathResult_getBooleanValue(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_XPathResult_getSingleNodeValue(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_XPathResult_getInvalidIteratorState(int64_t peer);
WKJ_EXPORT int32_t wkj_dom_XPathResult_getSnapshotLength(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_XPathResult_iterateNext(int64_t peer);
WKJ_EXPORT int64_t wkj_dom_XPathResult_snapshotItem(int64_t peer, int32_t index);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_DOM_H */
