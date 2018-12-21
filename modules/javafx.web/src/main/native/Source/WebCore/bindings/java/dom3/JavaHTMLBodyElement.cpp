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

#include <WebCore/EventListener.h>
#include <WebCore/HTMLBodyElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "JavaDOMUtils.h"
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLBodyElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getALinkImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::alinkAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setALinkImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alinkAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getBackgroundImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::backgroundAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setBackgroundImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::backgroundAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getBgColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::bgcolorAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setBgColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::bgcolorAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getLinkImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::linkAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setLinkImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::linkAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::textAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setTextImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::textAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getVLinkImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::vlinkAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setVLinkImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::vlinkAttr, String(env, value));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnblurImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnblurImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnerrorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnerrorImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnfocusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnfocusImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnfocusinImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnfocusinImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnfocusoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnfocusoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnresizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnresizeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnscrollImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnscrollImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnselectionchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectionchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnselectionchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectionchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnbeforeunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnbeforeunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnhashchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnhashchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnmessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnmessageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnofflineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnofflineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnonlineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnonlineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnpagehideImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnpagehideImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnpageshowImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnpageshowImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnpopstateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnpopstateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnstorageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnstorageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_getOnunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLBodyElementImpl_setOnunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

}
