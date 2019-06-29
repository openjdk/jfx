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


#include <WebCore/EventListener.h>
#include <WebCore/HTMLFrameSetElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLFrameSetElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getColsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::colsAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setColsImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::colsAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getRowsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::rowsAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setRowsImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::rowsAttr, String(env, value));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnblurImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnblurImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnerrorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnerrorImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnfocusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnfocusImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnfocusinImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnfocusinImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnfocusoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnfocusoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnresizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnresizeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnscrollImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnscrollImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnbeforeunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnbeforeunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnhashchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnhashchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnmessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnmessageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnofflineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnofflineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnonlineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnonlineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnpagehideImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnpagehideImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnpageshowImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnpageshowImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnpopstateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnpopstateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnstorageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnstorageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_getOnunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLFrameSetElementImpl_setOnunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

}
