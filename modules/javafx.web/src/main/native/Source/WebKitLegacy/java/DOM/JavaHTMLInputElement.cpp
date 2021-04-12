/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLInputElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/NodeList.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLInputElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAcceptImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::acceptAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAcceptImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::acceptAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAltImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::altAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAltImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::altAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAutocompleteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->autocomplete());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAutocompleteImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAutocomplete(String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAutofocusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::autofocusAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAutofocusImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::autofocusAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getDefaultCheckedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::checkedAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setDefaultCheckedImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::checkedAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getCheckedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->checked();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setCheckedImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setChecked(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getDirNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::dirnameAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setDirNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::dirnameAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getDisabledImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setDisabledImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLFormElement>(env, WTF::getPtr(IMPL->form()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormActionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->formAction());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setFormActionImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setFormAction(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormEnctypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->formEnctype());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setFormEnctypeImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setFormEnctype(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormMethodImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->formMethod());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setFormMethodImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setFormMethod(String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormNoValidateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::formnovalidateAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setFormNoValidateImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::formnovalidateAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getFormTargetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::formtargetAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setFormTargetImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::formtargetAttr, String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->height();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setHeightImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setHeight(value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getIndeterminateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->indeterminate();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setIndeterminateImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setIndeterminate(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getMaxImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::maxAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setMaxImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::maxAttr, String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getMaxLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->maxLength();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setMaxLengthImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setMaxLength(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getMinImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::minAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setMinImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::minAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getMultipleImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::multipleAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setMultipleImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::multipleAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getNameAttribute());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::nameAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getPatternImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::patternAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setPatternImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::patternAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getPlaceholderImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::placeholderAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setPlaceholderImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::placeholderAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getReadOnlyImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::readonlyAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setReadOnlyImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::readonlyAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getRequiredImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::requiredAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setRequiredImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::requiredAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getSizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, String::number(IMPL->size()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setSizeImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSize(String(env, value).toInt());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getSrcImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getURLAttribute(WebCore::HTMLNames::srcAttr).string());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setSrcImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::srcAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getStepImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::stepAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setStepImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::stepAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->type());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setTypeImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setType(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getDefaultValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->defaultValue());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setDefaultValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDefaultValue(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->value());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(String(env, value));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getValueAsDateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->valueAsDate();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setValueAsDateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValueAsDate(value);
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getValueAsNumberImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->valueAsNumber();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setValueAsNumberImpl(JNIEnv*, jclass, jlong peer, jdouble value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValueAsNumber(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->width();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setWidthImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setWidth(value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getWillValidateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->willValidate();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getValidationMessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->validationMessage());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getLabelsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->labels()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAlignImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAlignImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getUseMapImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::usemapAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setUseMapImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::usemapAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getIncrementalImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::incrementalAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setIncrementalImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::incrementalAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_getAccessKeyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setAccessKeyImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, String(env, value));
}


// Functions
JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_stepUpImpl(JNIEnv* env, jclass, jlong peer
    , jint n)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->stepUp(n));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_stepDownImpl(JNIEnv* env, jclass, jlong peer
    , jint n)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->stepDown(n));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_checkValidityImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->checkValidity();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setCustomValidityImpl(JNIEnv* env, jclass, jlong peer
    , jstring error)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCustomValidity(String(env, error));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_selectImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->select();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setRangeTextImpl(JNIEnv* env, jclass, jlong peer
    , jstring replacement)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setRangeText(String(env, replacement)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setRangeTextExImpl(JNIEnv* env, jclass, jlong peer
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


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_clickImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->click();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLInputElementImpl_setValueForUserImpl(JNIEnv* env, jclass, jlong peer
    , jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValueForUser(String(env, value));
}


}
