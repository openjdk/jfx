/*
 * Copyright (c) 2013, 2023, Oracle and/or its affiliates. All rights reserved.
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


#include <WebCore/Attr.h>
#include <WebCore/CDATASection.h>
#include <WebCore/CSSStyleDeclaration.h>
#include <WebCore/Comment.h>
#include <WebCore/DOMException.h>
#include <WebCore/DOMImplementation.h>
#include <WebCore/DOMWindow.h>
#include <WebCore/Document.h>
#include <WebCore/DocumentFragment.h>
#include <WebCore/DocumentType.h>
#include <WebCore/Element.h>
#include <WebCore/Event.h>
#include <WebCore/EventListener.h>
#include <WebCore/FullscreenManager.h>
#include <WebCore/HTMLCollection.h>
#include <WebCore/HTMLElement.h>
#include <WebCore/HTMLHeadElement.h>
#include <WebCore/HTMLScriptElement.h>
#include <WebCore/Node.h>
#include <WebCore/NodeFilter.h>
#include <WebCore/NodeIterator.h>
#include <WebCore/NodeList.h>
#include <WebCore/ProcessingInstruction.h>
#include <WebCore/Range.h>
#include <WebCore/SecurityOrigin.h>
#include <WebCore/ScrollIntoViewOptions.h>
#include <WebCore/StyleSheetList.h>
#include <WebCore/Text.h>
#include <WebCore/TreeWalker.h>
#include <WebCore/XPathExpression.h>
#include <WebCore/XPathNSResolver.h>
#include <WebCore/XPathResult.h>
#include <WebCore/EventNames.h>
#include <WebCore/JSExecState.h>
#include <WebCore/VisibilityState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include "AbstractViewInternal.h"
#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

enum class VisibilityState : bool;

extern "C" {

#define IMPL (static_cast<Document*>(jlong_to_ptr(peer)))

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_isHTMLDocumentImpl(JNIEnv*, jclass, jlong peer)
{
    return IMPL->isHTMLDocument() || IMPL->isXHTMLDocument();
}


// Attributes
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDoctypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentType>(env, WTF::getPtr(IMPL->doctype()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getImplementationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMImplementation>(env, WTF::getPtr(IMPL->implementation()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDocumentElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->documentElement()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getInputEncodingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->characterSetWithUTF8Fallback());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getXmlEncodingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->xmlEncoding());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getXmlVersionImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->xmlVersion());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setXmlVersionImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setXMLVersion(AtomString{String(env, value)});
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_getXmlStandaloneImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->xmlStandalone();
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setXmlStandaloneImpl(JNIEnv*, jclass, jlong peer, jboolean value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setXMLStandalone(value);
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDocumentURIImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->documentURI());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setDocumentURIImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setDocumentURI(AtomString{String(env, value)});
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDefaultViewImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DOMWindow>(env, WTF::getPtr(toDOMWindow(IMPL->windowProxy())));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getStyleSheetsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<StyleSheetList>(env, WTF::getPtr(IMPL->styleSheets()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getContentTypeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->contentType());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getTitleImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->title());
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setTitleImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setTitle(String(env, value));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getReferrerImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->referrer());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDomainImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->domain());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getURLImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->urlForBindings().string());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getCookieImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, raiseOnDOMError(env, IMPL->cookie()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setCookieImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setCookie(AtomString{String(env, value)});
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getBodyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLElement>(env, WTF::getPtr(IMPL->bodyOrFrameset()));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setBodyImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setBodyOrFrameset(static_cast<HTMLElement*>(jlong_to_ptr(value)));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getHeadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLHeadElement>(env, WTF::getPtr(IMPL->head()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getImagesImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->images()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getAppletsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->applets()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getLinksImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->links()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getFormsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->forms()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getAnchorsImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->anchors()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getLastModifiedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->lastModified());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getCharsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->characterSetWithUTF8Fallback());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getDefaultCharsetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->defaultCharsetForLegacyBindings());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getReadyStateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    auto readyState = IMPL->readyState();
    const char* readyStateStr { };
    switch (readyState) {
    case WebCore::Document::Loading:
        readyStateStr = "loading";
        break;
    case WebCore::Document::Interactive:
        readyStateStr = "interactive";
        break;
    case WebCore::Document::Complete:
        readyStateStr = "complete";
        break;
    default:
        ASSERT_NOT_REACHED();
    }
    return JavaReturn<String>(env, String::fromLatin1(readyStateStr));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getCharacterSetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->characterSetWithUTF8Fallback());
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getPreferredStylesheetSetImpl(JNIEnv*, jclass, jlong)
{
    return nullptr;
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getSelectedStylesheetSetImpl(JNIEnv*, jclass, jlong)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setSelectedStylesheetSetImpl(JNIEnv*, jclass, jlong, jstring)
{
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getActiveElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->activeElement()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getCompatModeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->compatMode());
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_getWebkitIsFullScreenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->fullscreenManagerIfExists()->isFullscreen();
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_getWebkitFullScreenKeyboardInputAllowedImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->fullscreenManagerIfExists()->isFullscreenKeyboardInputAllowed();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getWebkitCurrentFullScreenElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->fullscreenManagerIfExists()->currentFullscreenElement()));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_getWebkitFullscreenEnabledImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->fullscreenManagerIfExists()->isFullscreenEnabled();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getWebkitFullscreenElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->fullscreenManagerIfExists()->fullscreenElement()));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getVisibilityStateImpl(JNIEnv* env, jclass, jlong peer)
{
    const char* visibility {};
    switch (IMPL->visibilityState()) {
    case WebCore::VisibilityState::Hidden:
        visibility = "hidden";
        break;
    case WebCore::VisibilityState::Visible:
        visibility = "visible";
        break;
    }
    return JavaReturn<String>(env, String::fromLatin1(visibility));
}

JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_getHiddenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hidden();
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getCurrentScriptImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    WebCore::Element* element = IMPL->currentScript();
    if (!is<WebCore::HTMLScriptElement>(element))
        return 0;
    return JavaReturn<HTMLScriptElement>(env, WTF::getPtr(downcast<WebCore::HTMLScriptElement>(element)));
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOriginImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->securityOrigin().toString());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getScrollingElementImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->scrollingElementForAPI()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnbeforecopyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecopyEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnbeforecopyImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecopyEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnbeforecutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforecutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnbeforecutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforecutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnbeforepasteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().beforepasteEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnbeforepasteImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().beforepasteEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOncopyImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().copyEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOncopyImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().copyEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOncutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().cutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOncutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().cutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnpasteImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pasteEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnpasteImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pasteEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnselectstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnselectstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnselectionchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectionchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnselectionchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectionchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnreadystatechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().readystatechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnreadystatechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().readystatechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnabortImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().abortEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnabortImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().abortEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnblurImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().blurEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnblurImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().blurEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOncanplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplayEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOncanplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplayEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOncanplaythroughImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().canplaythroughEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOncanplaythroughImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().canplaythroughEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().changeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().changeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().clickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().clickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOncontextmenuImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().contextmenuEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOncontextmenuImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().contextmenuEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndblclickImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dblclickEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndblclickImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dblclickEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndragstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dragstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndragstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dragstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndropImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().dropEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndropImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().dropEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOndurationchangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().durationchangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOndurationchangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().durationchangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnemptiedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().emptiedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnemptiedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().emptiedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnendedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().endedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnendedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().endedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnerrorImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().errorEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnerrorImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().errorEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnfocusImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().focusEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnfocusImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().focusEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOninputImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().inputEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOninputImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().inputEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOninvalidImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().invalidEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOninvalidImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().invalidEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnkeydownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keydownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnkeydownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keydownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnkeypressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keypressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnkeypressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keypressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnkeyupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().keyupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnkeyupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().keyupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnloadImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnloadImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnloadeddataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadeddataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnloadeddataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadeddataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnloadedmetadataImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadedmetadataEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnloadedmetadataImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadedmetadataEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnloadstartImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().loadstartEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnloadstartImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().loadstartEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmousedownImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousedownEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmousedownImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousedownEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmouseenterImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseenterEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmouseenterImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseenterEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmouseleaveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseleaveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmouseleaveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseleaveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmousemoveImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousemoveEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmousemoveImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousemoveEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmouseoutImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoutEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmouseoutImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoutEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmouseoverImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseoverEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmouseoverImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseoverEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmouseupImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mouseupEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmouseupImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mouseupEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnmousewheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().mousewheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnmousewheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().mousewheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnpauseImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().pauseEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnpauseImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().pauseEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnplayImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnplayImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnplayingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().playingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnplayingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().playingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnprogressImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().progressEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnprogressImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().progressEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnratechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().ratechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnratechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().ratechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnresetImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resetEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnresetImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resetEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnresizeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().resizeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnresizeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().resizeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnscrollImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().scrollEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnscrollImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().scrollEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnseekedImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekedEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnseekedImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekedEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnseekingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().seekingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnseekingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().seekingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnselectImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().selectEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnselectImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().selectEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnstalledImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().stalledEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnstalledImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().stalledEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnsubmitImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().submitEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnsubmitImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().submitEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnsuspendImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().suspendEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnsuspendImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().suspendEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOntimeupdateImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().timeupdateEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOntimeupdateImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().timeupdateEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnvolumechangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().volumechangeEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnvolumechangeImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().volumechangeEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnwaitingImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().waitingEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnwaitingImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().waitingEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnsearchImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().searchEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnsearchImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().searchEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOnwheelImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<EventListener>(env, WTF::getPtr(IMPL->attributeEventListener(eventNames().wheelEvent, mainThreadNormalWorld())));
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_setOnwheelImpl(JNIEnv*, jclass, jlong peer, jlong value)
{
    WebCore::JSMainThreadNullState state;
    IMPL->setAttributeEventListener(eventNames().wheelEvent, static_cast<EventListener*>(jlong_to_ptr(value)), mainThreadNormalWorld());
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getChildrenImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->children()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getFirstElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->firstElementChild()));
}

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getLastElementChildImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->lastElementChild()));
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_DocumentImpl_getChildElementCountImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->childElementCount();
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createElementImpl(JNIEnv* env, jclass, jlong peer
    , jstring tagName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createElementForBindings(AtomString {String(env, tagName)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createDocumentFragmentImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<DocumentFragment>(env, WTF::getPtr(IMPL->createDocumentFragment()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createTextNodeImpl(JNIEnv* env, jclass, jlong peer
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Text>(env, WTF::getPtr(IMPL->createTextNode(String(env, data))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createCommentImpl(JNIEnv* env, jclass, jlong peer
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<WebCore::Comment>(env, WTF::getPtr(IMPL->createComment(String(env, data))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createCDATASectionImpl(JNIEnv* env, jclass, jlong peer
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CDATASection>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createCDATASection(String(env, data)))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createProcessingInstructionImpl(JNIEnv* env, jclass, jlong peer
    , jstring target
    , jstring data)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<ProcessingInstruction>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createProcessingInstruction(String(env, target)
            , String(env, data)))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createAttributeImpl(JNIEnv* env, jclass, jlong peer
    , jstring name)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Attr>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createAttribute(AtomString {String(env, name)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createEntityReferenceImpl(JNIEnv* env, jclass, jlong, jstring)
{
    raiseNotSupportedErrorException(env);
    return {};
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getElementsByTagNameImpl(JNIEnv* env, jclass, jlong peer
    , jstring tagname)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->getElementsByTagName(AtomString {String(env, tagname)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_importNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong importedNode
    , jboolean deep)
{
    WebCore::JSMainThreadNullState state;
    if (!importedNode) {
        raiseTypeErrorException(env);
        return 0;
    }

    return JavaReturn<Node>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->importNode(*static_cast<Node*>(jlong_to_ptr(importedNode))
            , deep))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createElementNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring qualifiedName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createElementNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, qualifiedName)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createAttributeNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring qualifiedName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Attr>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createAttributeNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, qualifiedName)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getElementsByTagNameNSImpl(JNIEnv* env, jclass, jlong peer
    , jstring namespaceURI
    , jstring localName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->getElementsByTagNameNS(AtomString {String(env, namespaceURI)}
            , AtomString {String(env, localName)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_adoptNodeImpl(JNIEnv* env, jclass, jlong peer
    , jlong source)
{
    WebCore::JSMainThreadNullState state;
    if (!source) {
        raiseTypeErrorException(env);
        return 0;
    }

    return JavaReturn<Node>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->adoptNode(*static_cast<Node*>(jlong_to_ptr(source))))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createEventImpl(JNIEnv* env, jclass, jlong peer
    , jstring eventType)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Event>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createEvent(AtomString {String(env, eventType)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createRangeImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Range>(env, WTF::getPtr(IMPL->createRange()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createNodeIteratorImpl(JNIEnv*, jclass, jlong
    , jlong
    , jint
    , jlong
    , jboolean)
{
#if 0
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeIterator>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createNodeIterator(static_cast<Node*>(jlong_to_ptr(root))
            , whatToShow
            , static_cast<NodeFilter*>(jlong_to_ptr(filter))
            , expandEntityReferences))));
#endif
    return 0L;
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createTreeWalkerImpl(JNIEnv*, jclass, jlong
    , jlong
    , jint
    , jlong
    , jboolean)
{
#if 0
    WebCore::JSMainThreadNullState state;
    if (!root) {
        raiseTypeErrorException(env);
        return 0;
    }

    RefPtr<WebCore::NodeFilter> nativeNodeFilter;
    if (filter)
        nativeNodeFilter = WebCore::NativeNodeFilter::create(WebCore::ObjCNodeFilterCondition::create(filter));
    return JavaReturn<TreeWalker>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createTreeWalker(static_cast<Node*>(jlong_to_ptr(root))
            , whatToShow
            , static_cast<NodeFilter*>(jlong_to_ptr(filter))
            , expandEntityReferences))));
#endif
    return 0L;
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getOverrideStyleImpl(JNIEnv*, jclass, jlong
    , jlong
    , jstring)
{
#if 0
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSStyleDeclaration>(env, WTF::getPtr(IMPL->getOverrideStyle(static_cast<Element*>(jlong_to_ptr(element))
            , AtomString {String(env, pseudoElement)})));
#endif
    return 0L;
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createExpressionImpl(JNIEnv* env, jclass, jlong peer
    , jstring expression
    , jlong resolver)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<XPathExpression>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->createExpression(AtomString {String(env, expression)}
            , static_cast<XPathNSResolver*>(jlong_to_ptr(resolver))))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createNSResolverImpl(JNIEnv* env, jclass, jlong peer
    , jlong nodeResolver)
{
    WebCore::JSMainThreadNullState state;
    if (!nodeResolver)
        return 0;

    return JavaReturn<XPathNSResolver>(env, WTF::getPtr(IMPL->createNSResolver(*static_cast<Node*>(jlong_to_ptr(nodeResolver)))));
}

// - (DOMXPathResult *)evaluate:(NSString *)expression
// contextNode:(DOMNode *)contextNode
// resolver:(id <DOMXPathNSResolver>)resolver
// type:(unsigned short)type
// inResult:(DOMXPathResult *)inResult WEBKIT_AVAILABLE_MAC(10_5);

JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_evaluateImpl(JNIEnv* env, jclass, jlong peer
    , jstring expression
    , jlong contextNode
    , jlong resolver
    , jshort type
    , jlong inResult)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<XPathResult>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->evaluate(AtomString {String(env, expression)}
            , *static_cast<Node*>(jlong_to_ptr(contextNode))
            , static_cast<XPathNSResolver*>(jlong_to_ptr(resolver))
            , type
            , static_cast<XPathResult*>(jlong_to_ptr(inResult))))));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_execCommandImpl(JNIEnv* env, jclass, jlong peer
    , jstring command
    , jboolean userInterface
    , jstring value)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->execCommand(AtomString {String(env, command)}
            , userInterface
            , AtomString {String(env, value)}).returnValue();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_queryCommandEnabledImpl(JNIEnv* env, jclass, jlong peer
    , jstring command)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandEnabled(AtomString {String(env, command)}).returnValue();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_queryCommandIndetermImpl(JNIEnv* env, jclass, jlong peer
    , jstring command)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandIndeterm(AtomString {String(env, command)}).returnValue();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_queryCommandStateImpl(JNIEnv* env, jclass, jlong peer
    , jstring command)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandState(AtomString {String(env, command)}).returnValue();
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_queryCommandSupportedImpl(JNIEnv* env, jclass, jlong peer
    , jstring command)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->queryCommandSupported(AtomString {String(env, command)}).returnValue();
}


JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_DocumentImpl_queryCommandValueImpl(JNIEnv* env, jclass, jlong peer
    , jstring command)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<String>(env, IMPL->queryCommandValue(AtomString {String(env, command)}).returnValue());
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getElementsByNameImpl(JNIEnv* env, jclass, jlong peer
    , jstring elementName)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(IMPL->getElementsByName(AtomString {String(env, elementName)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_elementFromPointImpl(JNIEnv* env, jclass, jlong peer
    , jint x
    , jint y)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->elementFromPoint(x
            , y)));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_caretRangeFromPointImpl(JNIEnv* env, jclass, jlong peer
    , jint x
    , jint y)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Range>(env, WTF::getPtr(IMPL->caretRangeFromPoint(x
            , y)));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_createCSSStyleDeclarationImpl(JNIEnv* env, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<CSSStyleDeclaration>(env, WTF::getPtr(IMPL->createCSSStyleDeclaration()));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getElementsByClassNameImpl(JNIEnv* env, jclass, jlong peer
    , jstring classNames)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<HTMLCollection>(env, WTF::getPtr(IMPL->getElementsByClassName(AtomString {String(env, classNames)})));
}


JNIEXPORT jboolean JNICALL Java_com_sun_webkit_dom_DocumentImpl_hasFocusImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    return IMPL->hasFocus();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_webkitCancelFullScreenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->fullscreenManagerIfExists()->cancelFullscreen();
}


JNIEXPORT void JNICALL Java_com_sun_webkit_dom_DocumentImpl_webkitExitFullscreenImpl(JNIEnv*, jclass, jlong peer)
{
    WebCore::JSMainThreadNullState state;
    IMPL->fullscreenManagerIfExists()->exitFullscreen();
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_getElementByIdImpl(JNIEnv* env, jclass, jlong peer
    , jstring elementId)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(IMPL->getElementById(AtomString {String(env, elementId)})));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_querySelectorImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<Element>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelector(AtomString {String(env, selectors)}))));
}


JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_DocumentImpl_querySelectorAllImpl(JNIEnv* env, jclass, jlong peer
    , jstring selectors)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<NodeList>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->querySelectorAll(AtomString {String(env, selectors)}))));
}


}
