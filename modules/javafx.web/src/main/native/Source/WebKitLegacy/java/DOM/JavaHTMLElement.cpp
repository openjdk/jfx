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
#include <WebCore/Element.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLElement*>(wkj_to_ptr(peer)))

// Attributes
WKJ_EXPORT int32_t wkj_dom_HTMLElement_getId(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getIdAttribute());
}

WKJ_EXPORT void wkj_dom_HTMLElement_setId(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::idAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTitle(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::titleAttr));
}

WKJ_EXPORT void wkj_dom_HTMLElement_setTitle(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::titleAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getLang(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::langAttr));
}

WKJ_EXPORT void wkj_dom_HTMLElement_setLang(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::langAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTranslate(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->translate();
}

WKJ_EXPORT void wkj_dom_HTMLElement_setTranslate(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setTranslate(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getDir(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->dir());
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getDraggable(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->draggable();
}

WKJ_EXPORT void wkj_dom_HTMLElement_setDraggable(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setDraggable(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getWebkitdropzone(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::webkitdropzoneAttr));
}

WKJ_EXPORT void wkj_dom_HTMLElement_setWebkitdropzone(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::webkitdropzoneAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getHidden(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::hiddenAttr);
}

WKJ_EXPORT void wkj_dom_HTMLElement_setHidden(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::hiddenAttr, value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getAccessKey(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

WKJ_EXPORT void wkj_dom_HTMLElement_setAccessKey(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, AtomString{WKJString(value, value_length)});
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getInnerText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->innerText());
}

WKJ_EXPORT void wkj_dom_HTMLElement_setInnerText(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setInnerText(WKJString(value, value_length));
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getOuterText(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->outerText());
}

WKJ_EXPORT void wkj_dom_HTMLElement_setOuterText(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setOuterText(WKJString(value, value_length));
}

WKJ_EXPORT int64_t wkj_dom_HTMLElement_getChildren(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnPeer<HTMLCollection>(WTF::getPtr(IMPL->children()));
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getContentEditable(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->contentEditable());
}

WKJ_EXPORT void wkj_dom_HTMLElement_setContentEditable(int64_t peer, const uint16_t* value, int32_t value_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setContentEditable(WKJString(value, value_length));
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getIsContentEditable(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->isContentEditable();
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getSpellcheck(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return IMPL->spellcheck();
}

WKJ_EXPORT void wkj_dom_HTMLElement_setSpellcheck(int64_t peer, int32_t value)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->setSpellcheck(value);
}

WKJ_EXPORT int32_t wkj_dom_HTMLElement_getTitleDisplayString(int64_t peer, uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    return WKJReturnString(result_buf, result_cap, result_length, IMPL->title());
}


// Functions
WKJ_EXPORT int64_t wkj_dom_HTMLElement_insertAdjacentElement(int64_t peer, const uint16_t* where, int32_t where_length, int64_t element)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException();
        return 0;
    }
    return WKJReturnPeer<Element>(WTF::getPtr(raiseOnDOMError(IMPL->insertAdjacentElement(WKJString(where, where_length), *static_cast<Element*>(wkj_to_ptr(element))))));
}


WKJ_EXPORT void wkj_dom_HTMLElement_insertAdjacentHTML(int64_t peer, const uint16_t* where, int32_t where_length, const uint16_t* html, int32_t html_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->insertAdjacentHTML(AtomString{WKJString(where, where_length)}
            , AtomString{WKJString(html, html_length)}));
}


WKJ_EXPORT void wkj_dom_HTMLElement_insertAdjacentText(int64_t peer, const uint16_t* where, int32_t where_length, const uint16_t* text, int32_t text_length)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(IMPL->insertAdjacentText(WKJString(where, where_length)
            , WKJString(text, text_length)));
}


WKJ_EXPORT void wkj_dom_HTMLElement_click(int64_t peer)
{
    WKJCallScope wkjScope;
    WebCore::JSMainThreadNullState state;
    IMPL->click();
}


}
