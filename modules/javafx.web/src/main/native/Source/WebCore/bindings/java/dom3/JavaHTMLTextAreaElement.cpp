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
#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLTextAreaElement.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTextAreaElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getAutofocusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setAutofocusImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getDirNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::dirnameAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setDirNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::dirnameAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getDisabledImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setDisabledImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getFormImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLFormElement>(env, WTF::getPtr(IMPL->form()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getMaxLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->maxLength();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setMaxLengthImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setMaxLength(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getNameAttribute());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getPlaceholderImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::placeholderAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setPlaceholderImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::placeholderAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getReadOnlyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::readonlyAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setReadOnlyImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::readonlyAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getRequiredImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setRequiredImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getRowsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->rows();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setRowsImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setRows(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getColsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->cols();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setColsImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCols(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getWrapImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::wrapAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setWrapImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::wrapAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->type());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getDefaultValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->defaultValue());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setDefaultValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDefaultValue(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->value());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getTextLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->textLength();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getWillValidateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getValidationMessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->validationMessage());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getLabelsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->labels()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getSelectionStartImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->selectionStart();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setSelectionStartImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionStart(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getSelectionEndImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->selectionEnd();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setSelectionEndImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionEnd(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getSelectionDirectionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->selectionDirection());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setSelectionDirectionImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionDirection(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getAccessKeyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setAccessKeyImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_getAutocompleteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->autocomplete());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setAutocompleteImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAutocomplete(String(env, value));
}


// Functions
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_checkValidityImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setCustomValidityImpl(JNIEnv* env, jclass, jlong peer
    , jstring error)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(String(env, error));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_selectImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->select();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setRangeTextImpl(JNIEnv* env, jclass, jlong peer
    , jstring replacement)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setRangeText(String(env, replacement)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setRangeTextExImpl(JNIEnv* env, jclass, jlong peer
    , jstring replacement
    , jint start
    , jint end
    , jstring selectionMode)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setRangeText(String(env, replacement)
            , start
            , end
            , String(env, selectionMode)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTextAreaElementImpl_setSelectionRangeImpl(JNIEnv* env, jclass, jlong peer
    , jint start
    , jint end
    , jstring direction)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelectionRange(start
            , end
            , String(env, direction));
}


}
