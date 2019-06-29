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
#include <WebCore/Element.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/HTMLNames.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLElement*>(jlong_to_ptr(peer)))

// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getIdImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getIdAttribute());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setIdImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::idAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getTitleImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::titleAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setTitleImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::titleAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getLangImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::langAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setLangImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::langAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getTranslateImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->translate();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setTranslateImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setTranslate(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getDirImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->dir());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setDirImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDir(String(env, value));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getTabIndexImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->tabIndex();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setTabIndexImpl(JNIEnv*, jclass, jlong peer, jint value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setTabIndex(value);
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getDraggableImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->draggable();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setDraggableImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDraggable(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getWebkitdropzoneImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::webkitdropzoneAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setWebkitdropzoneImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::webkitdropzoneAttr, String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getHiddenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasAttribute(WebCore::HTMLNames::hiddenAttr);
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setHiddenImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBooleanAttribute(WebCore::HTMLNames::hiddenAttr, value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getAccessKeyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->getAttribute(WebCore::HTMLNames::accesskeyAttr));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setAccessKeyImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeWithoutSynchronization(WebCore::HTMLNames::accesskeyAttr, String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getInnerTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->innerText());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setInnerTextImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setInnerText(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getOuterTextImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->outerText());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setOuterTextImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setOuterText(String(env, value));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getChildrenImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->children()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getContentEditableImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->contentEditable());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setContentEditableImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setContentEditable(String(env, value));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getIsContentEditableImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->isContentEditable();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getSpellcheckImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->spellcheck();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_setSpellcheckImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setSpellcheck(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_getTitleDisplayStringImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->title());
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_insertAdjacentElementImpl(JNIEnv* env, jclass, jlong peer
    , jstring where
    , jlong element)
{
    WebCore::JSMainThreadNullState state;
    if (!element) {
        raiseTypeErrorException(env);
        return 0;
    }
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->insertAdjacentElement(String(env, where), *static_cast<Element*>(jlong_to_ptr(element))))));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_insertAdjacentHTMLImpl(JNIEnv* env, jclass, jlong peer
    , jstring where
    , jstring html)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->insertAdjacentHTML(String(env, where)
            , String(env, html)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_insertAdjacentTextImpl(JNIEnv* env, jclass, jlong peer
    , jstring where
    , jstring text)
{
    WebCore::JSMainThreadNullState state;
    raiseOnDOMError(env, IMPL->insertAdjacentText(String(env, where)
            , String(env, text)));
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLElementImpl_clickImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->click();
}


}
