/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "DOMException.h"
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
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
#include <WebCore/URL.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLSelectElement*>(jlong_to_ptr(peer)))

// Attributes
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getAutofocusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setAutofocusImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getDisabledImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setDisabledImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getFormImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLFormElement>(env, WTF::getPtr(IMPL->form()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getMultipleImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->multiple();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setMultipleImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setMultiple(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getNameAttribute());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getRequiredImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setRequiredImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getSizeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->size();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setSizeImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSize(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->type());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getOptionsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLOptionsCollection>(env, WTF::getPtr(IMPL->options()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getSelectedOptionsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->selectedOptions()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getSelectedIndexImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->selectedIndex();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setSelectedIndexImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectedIndex(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->value());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getWillValidateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getValidationMessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->validationMessage());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getLabelsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->labels()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_getAutocompleteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->autocomplete());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setAutocompleteImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAutocomplete(String(env, value));
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_itemImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->item(index)));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_namedItemImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Node>(env, WTF::getPtr(IMPL->namedItem(String(env, name))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_addImpl(JNIEnv* env, jclass, jlong peer
    , jlong element
    , jlong before)
{
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException(env);
        return;
    }

    auto& coreElement = *static_cast<HTMLElement*>(jlong_to_ptr(element));
    Variant<RefPtr<WebCore::HTMLOptionElement>, RefPtr<WebCore::HTMLOptGroupElement>> variantElement;
    if (is<WebCore::HTMLOptionElement>(coreElement))
        variantElement = &downcast<WebCore::HTMLOptionElement>(coreElement);
    else if (is<WebCore::HTMLOptGroupElement>(coreElement))
        variantElement = &downcast<WebCore::HTMLOptGroupElement>(coreElement);
    else {
        raiseTypeErrorException(env);
        return;
    }
    raiseOnDOMError(env, IMPL->add(WTFMove(variantElement), WebCore::HTMLSelectElement::HTMLElementOrInt(static_cast<HTMLElement*>(jlong_to_ptr(before)))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_removeImpl(JNIEnv*, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    IMPL->remove(index);
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_checkValidityImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLSelectElementImpl_setCustomValidityImpl(JNIEnv* env, jclass, jlong peer
    , jstring error)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(String(env, error));
}


}
