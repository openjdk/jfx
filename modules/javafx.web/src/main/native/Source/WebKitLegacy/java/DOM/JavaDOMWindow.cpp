/*
 * Copyright (c) 2013, 2024, Oracle and/or its affiliates. All rights reserved.
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


#include <JavaScriptCore/APICast.h>
#include <WebCore/AddEventListenerOptions.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/DOMException.h>
#include <WebCore/DOMSelection.h>
#include <WebCore/DOMWindow.h>
#include <WebCore/Document.h>
#include <WebCore/Element.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventTarget.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/WindowProxy.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>
#include "com_sun_webkit_dom_JSObject.h"

using namespace WebCore;

extern "C" {

#define IMPL (dynamicDowncast<LocalDOMWindow>(static_cast<DOMWindow*>(jlong_to_ptr(peer))))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getFrameElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->frameElement()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOffscreenBufferingImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offscreenBuffering();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOuterHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->outerHeight();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOuterWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->outerWidth();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getInnerHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->innerHeight();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getInnerWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->innerWidth();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScreenXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScreenYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScreenLeftImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenLeft();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScreenTopImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->screenTop();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScrollXImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollX();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getScrollYImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollY();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getPageXOffsetImpl(JNIEnv*, jclass, jlong)
{
    WebCore::JSMainThreadNullState state;
    return {};
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getPageYOffsetImpl(JNIEnv*, jclass, jlong)
{
    WebCore::JSMainThreadNullState state;
    return {};
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getClosedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->closed();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getLengthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->length();
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->name());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setName(AtomString{String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getStatusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->status());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setStatusImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setStatus(AtomString{String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getDefaultStatusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->status());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setDefaultStatusImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setStatus(AtomString{String(env, value)});
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getSelfImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getWindowImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    // DOMWindow::frames() / DOMWindow::window() methods as they are just aliases for DOMWindow::self()
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getFramesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    // DOMWindow::frames() / DOMWindow::window() methods as they are just aliases for DOMWindow::self()
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOpenerImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL->opener()->window()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getParentImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL->parent()->window()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getTopImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(IMPL->top()->window()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getDocumentExImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Document>(env, WTF::getPtr(IMPL->document()));
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getDevicePixelRatioImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->devicePixelRatio();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnanimationendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnanimationendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnanimationiterationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationiterationEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnanimationiterationImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationiterationEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnanimationstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnanimationstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOntransitionendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().transitionendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOntransitionendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().transitionendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwebkitanimationendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationEndEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwebkitanimationendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationEndEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwebkitanimationiterationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationIterationEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwebkitanimationiterationImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationIterationEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwebkitanimationstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationStartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwebkitanimationstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationStartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwebkittransitionendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitTransitionEndEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwebkittransitionendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitTransitionEndEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnabortImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnabortImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnblurImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnblurImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOncanplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOncanplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOncanplaythroughImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOncanplaythroughImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOncontextmenuImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOncontextmenuImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndblclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndblclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndragstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndragstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndropImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndropImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOndurationchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOndurationchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnemptiedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnemptiedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnendedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnendedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnerrorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnerrorImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnfocusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnfocusImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOninputImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOninputImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOninvalidImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOninvalidImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnkeydownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnkeydownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnkeypressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnkeypressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnkeyupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnkeyupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnloadeddataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnloadeddataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnloadedmetadataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnloadedmetadataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnloadstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnloadstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmousedownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmousedownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmouseenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmouseenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmouseleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmouseleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmousemoveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmousemoveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmouseoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmouseoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmouseoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmouseoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmouseupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmouseupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmousewheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmousewheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnpauseImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnpauseImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnplayingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnplayingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnprogressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnprogressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnratechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnratechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnresetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnresetImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnresizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnresizeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnscrollImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnscrollImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnseekedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnseekedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnseekingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnseekingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnselectImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnselectImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnstalledImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnstalledImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnsubmitImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnsubmitImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnsuspendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnsuspendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOntimeupdateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOntimeupdateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnvolumechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnvolumechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwaitingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwaitingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnsearchImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().searchEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnsearchImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().searchEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnwheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnwheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnbeforeunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeunloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnbeforeunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeunloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnhashchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().hashchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnhashchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().hashchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnmessageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().messageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnmessageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().messageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnofflineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().offlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnofflineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().offlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnonlineImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().onlineEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnonlineImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().onlineEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnpagehideImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pagehideEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnpagehideImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pagehideEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnpageshowImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pageshowEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnpageshowImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pageshowEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnpopstateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().popstateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnpopstateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().popstateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnstorageImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().storageEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnstorageImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().storageEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getOnunloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().unloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_setOnunloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().unloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getSelectionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMSelection>(env, WTF::getPtr(IMPL->getSelection()));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_focusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->focus();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_blurImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->blur();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_closeImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->close();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_printImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->print();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_stopImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->stop();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_alertImpl(JNIEnv* env, jclass, jlong peer
    , jstring message)
{
    WebCore::JSMainThreadNullState state;
    IMPL->alert(AtomString{String(env, message)});
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_confirmImpl(JNIEnv* env, jclass, jlong peer
    , jstring message)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->confirmForBindings(AtomString{String(env, message)});
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_promptImpl(JNIEnv* env, jclass, jlong peer
    , jstring message
    , jstring defaultValue)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->prompt(AtomString{String(env, message)}
            , AtomString{String(env, defaultValue)}));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_findImpl(JNIEnv* env, jclass, jlong peer
    , jstring string
    , jboolean caseSensitive
    , jboolean backwards
    , jboolean wrap
    , jboolean wholeWord
    , jboolean searchInFrames
    , jboolean showDialog)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->find(AtomString{String(env, string)}
            , caseSensitive
            , backwards
            , wrap
            , wholeWord
            , searchInFrames
            , showDialog);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_scrollByImpl(JNIEnv*, jclass, jlong peer
    , jint x
    , jint y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->scrollBy(x
            , y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_scrollToImpl(JNIEnv*, jclass, jlong peer
    , jint x
    , jint y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->scrollTo(x
            , y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_scrollImpl(JNIEnv*, jclass, jlong peer
    , jint x
    , jint y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->scrollTo(x, y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_moveByImpl(JNIEnv*, jclass, jlong peer
    , jfloat x
    , jfloat y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->moveBy(x
            , y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_moveToImpl(JNIEnv*, jclass, jlong peer
    , jfloat x
    , jfloat y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->moveTo(x
            , y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_resizeByImpl(JNIEnv*, jclass, jlong peer
    , jfloat x
    , jfloat y)
{
    WebCore::JSMainThreadNullState state;
    IMPL->resizeBy(x
            , y);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_resizeToImpl(JNIEnv*, jclass, jlong peer
    , jfloat width
    , jfloat height)
{
    WebCore::JSMainThreadNullState state;
    IMPL->resizeTo(width
            , height);
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_getComputedStyleImpl(JNIEnv* env, jclass, jlong peer
    , jlong element
    , jstring pseudoElement)
{
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException(env);
        return {};
    }
    return JavaReturn<CSSStyleDeclaration>(env, WTF::getPtr(IMPL->getComputedStyle(*static_cast<Element*>(jlong_to_ptr(element))
            , AtomString{String(env, pseudoElement)})));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_captureEventsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->captureEvents();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_releaseEventsImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->releaseEvents();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_addEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->addEventListenerForBindings(AtomString{String(env, type)}
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_removeEventListenerImpl(JNIEnv* env, jclass, jlong peer
    , jstring type
    , jlong listener
    , jboolean useCapture)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeEventListenerForBindings(AtomString{String(env, type)}
            , static_cast<EventListener*>(jlong_to_ptr(listener))
            , static_cast<bool>(useCapture));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_dispatchEventImpl(JNIEnv* env, jclass, jlong peer
    , jlong event)
{
    WebCore::JSMainThreadNullState state;
    if (!event) {
        raiseTypeErrorException(env);
        return JNI_FALSE;
    }
    return raiseOnDOMError(env, IMPL->dispatchEventForBindings(*static_cast<Event*>(jlong_to_ptr(event))));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_atobImpl(JNIEnv* env, jclass, jlong peer
    , jstring string)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, raiseOnDOMError(env, IMPL->atob(AtomString{String(env, string)})));
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_btoaImpl(JNIEnv* env, jclass, jlong peer
    , jstring string)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, raiseOnDOMError(env, IMPL->btoa(AtomString{String(env, string)})));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_clearTimeoutImpl(JNIEnv*, jclass, jlong peer
    , jint handle)
{
    WebCore::JSMainThreadNullState state;
    IMPL->clearTimeout(handle);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DOMWindowImpl_clearIntervalImpl(JNIEnv*, jclass, jlong peer
    , jint handle)
{
    WebCore::JSMainThreadNullState state;
    IMPL->clearInterval(handle);
}


}
