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
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLOptGroupElement.h>
#include <WebCore/HTMLOptionsCollection.h>
#include <WebCore/HTMLSelectElement.h>
#include <WebCore/JSExecState.h>
#include <WebCore/NameNodeList.h>
#include <WebCore/Node.h>
#include <WebCore/NodeList.h>
#include <WebCore/ThreadCheck.h>
#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>
#include <wtf/URL.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLSelectElement*>(wkj_to_ptr(peer)))

// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getAutofocus(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setAutofocus(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getDisabled(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setDisabled(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getForm(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLFormElement>(WTF::getPtr(IMPL->form()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getMultiple(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->multiple();
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setMultiple(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setMultiple(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getName(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getNameAttribute());
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setName(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getRequired(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setRequired(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getSize(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->size();
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setSize(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSize(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getType(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->type());
}

WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getOptions(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLOptionsCollection>(WTF::getPtr(IMPL->options()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getLength(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getSelectedOptions(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->selectedOptions()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getSelectedIndex(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->selectedIndex();
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setSelectedIndex(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectedIndex(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getValue(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->value());
}

WKJ_EXPORT void wkj_dom_HTMLSelectElement_setValue(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(AtomString {WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getWillValidate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getValidationMessage(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->validationMessage());
}

WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_getLabels(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<NodeList>(WTF::getPtr(IMPL->labels()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_getAutocomplete(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->autocomplete());
}

// Functions
WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_item(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->item(index)));
}


WKJ_EXPORT int64_t wkj_dom_HTMLSelectElement_namedItem(int64_t peer, const uint16_t* name, int32_t name_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<Node>(WTF::getPtr(IMPL->namedItem(AtomString {WKJString(name, name_length)})));
}


WKJ_EXPORT void wkj_dom_HTMLSelectElement_add(int64_t peer, int64_t element, int64_t before)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException();
        return;
    }

    auto& coreElement = *static_cast<HTMLElement*>(wkj_to_ptr(element));
    WTF::Variant<RefPtr<WebCore::HTMLOptionElement>, RefPtr<WebCore::HTMLOptGroupElement>> variantElement;
    if (is<WebCore::HTMLOptionElement>(coreElement))
        variantElement = &downcast<WebCore::HTMLOptionElement>(coreElement);
    else if (is<WebCore::HTMLOptGroupElement>(coreElement))
        variantElement = &downcast<WebCore::HTMLOptGroupElement>(coreElement);
    else {
        raiseTypeErrorException();
        return;
    }
    raiseOnDOMError(IMPL->add(WTF::move(variantElement), WebCore::HTMLSelectElement::HTMLElementOrInt(static_cast<HTMLElement*>(wkj_to_ptr(before)))));
}


WKJ_EXPORT void wkj_dom_HTMLSelectElement_remove(int64_t peer, int32_t index)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->remove(index);
}


WKJ_EXPORT int32_t wkj_dom_HTMLSelectElement_checkValidity(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


WKJ_EXPORT void wkj_dom_HTMLSelectElement_setCustomValidity(int64_t peer, const uint16_t* error, int32_t error_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(AtomString{WKJString(error, error_length)});
}


}
