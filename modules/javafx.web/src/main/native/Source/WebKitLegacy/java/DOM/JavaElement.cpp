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


#include <WebCore/DOMException.h>
#include <WebCore/Attr.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/Element.h>
#include <WebCore/ElementInlines.h>
#include <WebCore/EventListener.h>
#include <WebCore/EventNames.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/NamedNodeMap.h>
#include <WebCore/NodeList.h>
#include <WebCore/ScrollIntoViewOptions.h>
#include <WebCore/StyledElement.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<Element*>(jlong_to_ptr(peer)))

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_isHTMLElementImpl(JNIEnv*, jclass, jlong peer) {
    return IMPL->isHTMLElement();
}


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getTagNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->tagName());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getAttributesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NamedNodeMap>(env, WTF::getPtr(IMPL->attributes()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getStyleImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    auto ret = is<WebCore::StyledElement>(IMPL) ? WTF::getPtr(&downcast<WebCore::StyledElement>(IMPL)->cssomStyle()) : nullptr;
    return JavaReturn<CSSStyleDeclaration>(env, ret);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getIdImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getIdAttribute());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setIdImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::idAttr, AtomString {String(env, value)});
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getOffsetLeftImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetLeftForBindings();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getOffsetTopImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetTopForBindings();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getOffsetWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetWidth();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getOffsetHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->offsetHeight();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getClientLeftImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientLeft();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getClientTopImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientTop();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getClientWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientWidth();
}

JNIEXPORT jdouble JNICALL Java_com_sun_webkit_dom_ElementImpl_getClientHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->clientHeight();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_ElementImpl_getScrollLeftImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollLeft();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setScrollLeftImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setScrollLeft(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_ElementImpl_getScrollTopImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollTop();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setScrollTopImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setScrollTop(value);
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_ElementImpl_getScrollWidthImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollWidth();
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_ElementImpl_getScrollHeightImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->scrollHeight();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOffsetParentImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->offsetParentForBindings()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getInnerHTMLImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->innerHTML());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setInnerHTMLImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setInnerHTML(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getOuterHTMLImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->outerHTML());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOuterHTMLImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setOuterHTML(AtomString {String(env, value)});
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getClassNameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::classAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setClassNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::classAttr, AtomString {String(env, value)});
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnbeforecopyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecopyEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnbeforecopyImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecopyEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnbeforecutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnbeforecutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnbeforepasteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforepasteEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnbeforepasteImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforepasteEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOncopyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().copyEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOncopyImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().copyEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOncutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().cutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOncutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().cutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnpasteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pasteEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnpasteImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pasteEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnselectstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnselectstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnanimationendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnanimationendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnanimationiterationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationiterationEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnanimationiterationImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationiterationEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnanimationstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().animationstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnanimationstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().animationstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOntransitionendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().transitionendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOntransitionendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().transitionendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwebkitanimationendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationEndEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwebkitanimationendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationEndEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwebkitanimationiterationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationIterationEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwebkitanimationiterationImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationIterationEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwebkitanimationstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitAnimationStartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwebkitanimationstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitAnimationStartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwebkittransitionendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().webkitTransitionEndEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwebkittransitionendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().webkitTransitionEndEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnfocusinImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusinEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnfocusinImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusinEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnfocusoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnfocusoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnbeforeloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforeloadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnbeforeloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforeloadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnabortImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnabortImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnblurImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnblurImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOncanplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOncanplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOncanplaythroughImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOncanplaythroughImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOncontextmenuImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOncontextmenuImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndblclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndblclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndragstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndragstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndropImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndropImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOndurationchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOndurationchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnemptiedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnemptiedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnendedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnendedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnerrorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnerrorImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnfocusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnfocusImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOninputImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOninputImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOninvalidImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOninvalidImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnkeydownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnkeydownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnkeypressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnkeypressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnkeyupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnkeyupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnloadeddataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnloadeddataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnloadedmetadataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnloadedmetadataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnloadstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnloadstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmousedownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmousedownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmouseenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmouseenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmouseleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmouseleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmousemoveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmousemoveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmouseoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmouseoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmouseoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmouseoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmouseupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmouseupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnmousewheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnmousewheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnpauseImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnpauseImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnplayingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnplayingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnprogressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnprogressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnratechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnratechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnresetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnresetImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnresizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnresizeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnscrollImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnscrollImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnseekedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnseekedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnseekingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnseekingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnselectImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnselectImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnstalledImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnstalledImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnsubmitImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnsubmitImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnsuspendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnsuspendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOntimeupdateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOntimeupdateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnvolumechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnvolumechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwaitingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwaitingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnsearchImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().searchEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnsearchImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().searchEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getOnwheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setOnwheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getPreviousElementSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->previousElementSibling()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getNextElementSiblingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->nextElementSibling()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getChildrenImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->children()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getFirstElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->firstElementChild()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getLastElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->lastElementChild()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_ElementImpl_getChildElementCountImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getAttributeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(AtomString {String(env, name)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setAttributeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name
    , jstring value)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setAttribute(AtomString {String(env, name)}
            , AtomString {String(env, value)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_removeAttributeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeAttribute(AtomString {String(env, name)});
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getAttributeNodeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Attr>(env, WTF::getPtr(IMPL->getAttributeNode(AtomString {String(env, name)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_setAttributeNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong newAttr)
{
    WebCore::JSMainThreadNullState state;
    if (!newAttr) {
        raiseTypeErrorException(env);
        return 0;
    }

    return JavaReturn<Attr>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->setAttributeNode(*static_cast<Attr*>(jlong_to_ptr(newAttr))))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_removeAttributeNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong oldAttr)
{
    WebCore::JSMainThreadNullState state;
    if (!oldAttr) {
        raiseTypeErrorException(env);
        return 0;
    }

    return JavaReturn<Attr>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->removeAttributeNode(*static_cast<Attr*>(jlong_to_ptr(oldAttr))))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getElementsByTagNameImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    if (!name)
        return 0;
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->getElementsByTagName(AtomString {String(env, name)})));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_hasAttributesImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributes();
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_ElementImpl_getAttributeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttributeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_setAttributeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring qualifiedName
    , jstring value)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->setAttributeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, qualifiedName)}
            , AtomString {String(env, value)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_removeAttributeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    WebCore::JSMainThreadNullState state;
    IMPL->removeAttributeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)});
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getElementsByTagNameNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    if (!localName)
        return 0;
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->getElementsByTagNameNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getAttributeNodeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Attr>(env, WTF::getPtr(IMPL->getAttributeNodeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_setAttributeNodeNSImpl(JNIEnv* env, jclass, jlong peer
    , jlong newAttr)
{
    WebCore::JSMainThreadNullState state;
    if (!newAttr) {
        raiseTypeErrorException(env);
        return 0;
    }
    return JavaReturn<Attr>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->setAttributeNodeNS(*static_cast<Attr*>(jlong_to_ptr(newAttr))))));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_hasAttributeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(AtomString {String(env, name)});
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_hasAttributeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttributeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)});
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_focusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->focus();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_blurImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->blur();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_scrollIntoViewImpl(JNIEnv*, jclass, jlong peer
    , jboolean alignWithTop)
{
    WebCore::JSMainThreadNullState state;
    IMPL->scrollIntoView(alignWithTop);
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_scrollIntoViewIfNeededImpl(JNIEnv*, jclass, jlong peer
    , jboolean centerIfNeeded)
{
    WebCore::JSMainThreadNullState state;
    IMPL->scrollIntoViewIfNeeded(centerIfNeeded);
}



JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_getElementsByClassNameImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->getElementsByClassName(AtomString {String(env, name)})));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_matchesImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(env, IMPL->matches(AtomString {String(env, selectors)}));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_closestImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->closest(AtomString {String(env, selectors)}))));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_ElementImpl_webkitMatchesSelectorImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return raiseOnDOMError(env, IMPL->matches(AtomString {String(env, selectors)}));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_webkitRequestFullScreenImpl(JNIEnv*, jclass, jlong peer
    , jshort)
{
    WebCore::JSMainThreadNullState state;
    IMPL->webkitRequestFullscreen();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_webkitRequestFullscreenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->webkitRequestFullscreen();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_ElementImpl_removeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->remove());
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_querySelectorImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelector(AtomString {String(env, selectors)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_ElementImpl_querySelectorAllImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelectorAll(AtomString {String(env, selectors)}))));
}


}
