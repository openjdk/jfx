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


#include <WebCore/DOMException.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/HTMLTableCaptionElement.h>
#include <WebCore/HTMLTableElement.h>
#include <WebCore/HTMLTableSectionElement.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLTableElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getCaptionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLTableCaptionElement>(env, WTF::getPtr(IMPL->caption()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setCaptionImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCaption(static_cast<HTMLTableCaptionElement*>(jlong_to_ptr(value)));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getTHeadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLTableSectionElement>(env, WTF::getPtr(IMPL->tHead()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setTHeadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setTHead(static_cast<HTMLTableSectionElement*>(jlong_to_ptr(value)));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getTFootImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLTableSectionElement>(env, WTF::getPtr(IMPL->tFoot()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setTFootImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setTFoot(static_cast<HTMLTableSectionElement*>(jlong_to_ptr(value)));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getRowsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->rows()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getTBodiesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->tBodies()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getAlignImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::alignAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setAlignImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::alignAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getBgColorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::bgcolorAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setBgColorImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::bgcolorAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getBorderImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::borderAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setBorderImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::borderAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getCellPaddingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::cellpaddingAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setCellPaddingImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::cellpaddingAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getCellSpacingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::cellspacingAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setCellSpacingImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::cellspacingAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getFrameImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::frameAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setFrameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::frameAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getRulesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::rulesAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setRulesImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::rulesAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getSummaryImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::summaryAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setSummaryImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::summaryAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_getWidthImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::widthAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_setWidthImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::widthAttr, String(env, value));
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_createTHeadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(IMPL->createTHead()));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_deleteTHeadImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->deleteTHead();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_createTFootImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(IMPL->createTFoot()));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_deleteTFootImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->deleteTFoot();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_createTBodyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(IMPL->createTBody()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_createCaptionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(IMPL->createCaption()));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_deleteCaptionImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->deleteCaption();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_insertRowImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->insertRow(index))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLTableElementImpl_deleteRowImpl(JNIEnv* env, jclass, jlong peer
    , jint index)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->deleteRow(index));
}


}
