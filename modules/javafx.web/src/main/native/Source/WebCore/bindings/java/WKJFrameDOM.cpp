/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * WKJFrameDOM.cpp - Frame to Document / owner Element, for com.sun.webkit.WebPage.
 *
 * These two entry points are all that remained of JavaDOMUtils.cpp once the DOM bindings moved
 * to WKJDOMUtils. They kept the JNI file alive long after everything else in it was dead, so
 * they are here instead, under a name that says what they do.
 *
 * What changed, and what deliberately did not:
 *
 *   - The C no longer builds the Java object. Java_com_sun_webkit_WebPage_twkGetDocument
 *     returned a jobject, produced by FindClass("com/sun/webkit/dom/NodeImpl") plus a
 *     CallStaticObjectMethod of NodeImpl.getImpl(long). Now the peer is returned and Java calls
 *     NodeImpl.getImpl itself. That removes this slice's last upcall and last FindClass.
 *
 *   - The reference count is untouched. makeObjectFromNode did peer->ref() immediately before
 *     the upcall, and the disposer inside NodeImpl drops it (getCachedImpl drops it at once
 *     when the node is already cached). The ref is still taken in the same place, so the
 *     NodeImpl hash count that LeakTest asserts is unchanged.
 *
 *   - The Node* that gets converted to a peer is the Node* after the upcast, not the Element*
 *     that entered the function. That was true of the JNI code too, because makeObjectFromNode
 *     took a Node* parameter, and it matters: the two addresses need not be equal.
 */

#include "config.h"

#include "Document.h"
#include "Element.h"
#include "Frame.h"
#include "FrameInlines.h"
#include "HTMLDocument.h"
#include "HTMLElement.h"
#include "HTMLFrameOwnerElement.h"
#include "LocalFrame.h"
#include "LocalFrameInlines.h"
#include "Node.h"

#include "WKJDOMUtils.h"
#include <webkit_java_api.h>

namespace WebCore {

/*
 * The peer of a node, with the one reference Java is expected to own. The wkj prefix is on
 * purpose: WebCore builds these sources unified, so file-scope names are shared.
 */
static int64_t wkjNodePeerForJava(Node* node)
{
    node->ref(); // deref is in the NodeImpl disposer
    return wkj_from_ptr(node);
}

} // namespace WebCore

using namespace WebCore;

extern "C" {

WKJ_EXPORT int64_t wkj_frame_get_document(int64_t frame)
{
    WKJCallScope wkjScope;

    Frame* coreFrame = static_cast<Frame*>(wkj_to_ptr(frame));
    if (!coreFrame)
        return 0;

    auto* localFrame = dynamicDowncast<LocalFrame>(coreFrame);
    /*
     * The JNI version dereferenced this without testing it, so a frame that is not local was
     * a null dereference rather than a null result. Guarding it is a crash fix, not a
     * behaviour change: every path that reached a non-local frame here crashed.
     */
    if (!localFrame)
        return 0;

    Document* document = localFrame->document();
    if (!document)
        return 0;

    return wkjNodePeerForJava(document);
}

WKJ_EXPORT int64_t wkj_frame_get_owner_element(int64_t frame)
{
    WKJCallScope wkjScope;

    Frame* coreFrame = static_cast<Frame*>(wkj_to_ptr(frame));
    if (!coreFrame)
        return 0;

    Element* ownerElement = (Element*) coreFrame->ownerElement();
    if (!ownerElement)
        return 0;

    return wkjNodePeerForJava(ownerElement);
}

}
