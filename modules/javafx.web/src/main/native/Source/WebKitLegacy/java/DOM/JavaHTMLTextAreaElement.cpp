/*
 * Copyright (c) 2013, 2026, Oracle and/or its affiliates. All rights reserved.
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

#undef IMPL


#include <WebCore/DOMException.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLTextAreaElement.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTextAreaElement*>(wkj_to_ptr(peer)))


// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAutofocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setAutofocus(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDirName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::dirnameAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDirName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::dirnameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDisabled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDisabled(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

WKJ_EXPORT int64_t wkj_dom_HTMLTextAreaElement_getForm(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLFormElement>(WTF::getPtr(IMPL->form()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getMaxLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->maxLength();
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setMaxLength(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setMaxLength(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getNameAttribute());
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getPlaceholder(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::placeholderAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setPlaceholder(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::placeholderAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getReadOnly(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::readonlyAttr);
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setReadOnly(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::readonlyAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getRequired(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRequired(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getRows(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->rows();
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRows(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setRows(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getCols(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->cols();
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setCols(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCols(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getWrap(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::wrapAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setWrap(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::wrapAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getDefaultValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->defaultValue());
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setDefaultValue(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDefaultValue(WKJString(value, value_length));
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->value());
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getTextLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->textLength();
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getWillValidate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->validationMessage());
}

WKJ_EXPORT int64_t wkj_dom_HTMLTextAreaElement_getLabels(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->labels()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionStart(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->selectionStart();
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionStart(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionStart(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionEnd(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->selectionEnd();
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionEnd(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionEnd(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getSelectionDirection(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->selectionDirection());
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionDirection(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionDirection(AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->autocomplete());
}


// Functions
WKJ_EXPORT int32_t wkj_dom_HTMLTextAreaElement_checkValidity(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(AtomString{WKJString(error, error_length)});
}


WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_select(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->select();
}


WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRangeText(int64_t peer, const uint16_t* replacement, int32_t replacement_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setRangeText(AtomString{WKJString(replacement, replacement_length)}));
}


WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setRangeTextEx(int64_t peer, const uint16_t* replacement, int32_t replacement_length, int32_t start, int32_t end, const uint16_t* selectionMode, int32_t selectionMode_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->setRangeText(WKJString(replacement, replacement_length)
            , start
            , end
            , AtomString{WKJString(selectionMode, selectionMode_length)}));
}


WKJ_EXPORT void wkj_dom_HTMLTextAreaElement_setSelectionRange(int64_t peer, int32_t start, int32_t end, const uint16_t* direction, int32_t direction_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionRange(start
            , end
            , AtomString{WKJString(direction, direction_length)});
}


}
