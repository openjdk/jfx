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


#include <WebCore/HTMLFormElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLOptionElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLOptionElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getDisabledImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::disabledAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_setDisabledImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::disabledAttr, value);
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getFormImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLFormElement>(env, WTF::getPtr(IMPL->form()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getLabelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->label());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_setLabelImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setLabel(String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getDefaultSelectedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::selectedAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_setDefaultSelectedImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::selectedAttr, value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getSelectedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->selected();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_setSelectedImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSelected(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getValueImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->value());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_setValueImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setValue(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->text());
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLOptionElementImpl_getIndexImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->index();
}

}
