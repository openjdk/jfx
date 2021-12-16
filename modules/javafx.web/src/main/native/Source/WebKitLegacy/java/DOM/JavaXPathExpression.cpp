/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/Node.h>
#include <WebCore/XPathExpression.h>
#include <WebCore/XPathResult.h>
#include <WebCore/JSExecState.h>

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<XPathExpression*>(jlong_to_ptr(peer)))

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_XPathExpressionImpl_dispose(JNIEnv*, jclass, jlong peer)
{
    IMPL->deref();
}


// Functions
JNIEXPORT jlong JNICALL Java_com_sun_webkit_dom_XPathExpressionImpl_evaluateImpl(JNIEnv* env, jclass, jlong peer
    , jlong contextNode
    , jshort type
    , jlong inResult)
{
    WebCore::JSMainThreadNullState state;
    return JavaReturn<XPathResult>(env, WTF::getPtr(raiseOnDOMError(env, IMPL->evaluate(*static_cast<Node*>(jlong_to_ptr(contextNode))
            , type
            , static_cast<XPathResult*>(jlong_to_ptr(inResult))))));
}


}
