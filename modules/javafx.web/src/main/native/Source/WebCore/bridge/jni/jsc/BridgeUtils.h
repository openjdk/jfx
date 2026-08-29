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

#pragma once

#include "ScriptController.h"
#include "JNIUtility.h"
#include "FrameDestructionObserverInlines.h"
#include <JavaScriptCore/JSObjectRef.h>

#include <webkit_java_api_bridge.h>


namespace WebCore {

/* A JavaScript string holding the given UTF-16 characters. The caller releases it. */
JSStringRef asJSStringRef(const uint16_t* s, int32_t length);

/*
 * A Java value that has already been described - by the Java side of an entry point, or by
 * the describe_object callback - as a JavaScript value. This is the second half of what
 * Java_Object_to_JSValue used to do; the first half, deciding what the Java object is, now
 * happens in Java, where it needs no class lookups and no field ids.
 */
JSValueRef WKJValueToJSValue(JSContextRef, JSC::Bindings::RootObject*, const WKJJSValue&,
    wkj_ref accessControlContext);

/*
 * An arbitrary Java object as a JavaScript value: asks Java to describe it, then converts
 * the description. Same name and same meaning as before, one parameter lighter.
 */
JSValueRef Java_Object_to_JSValue(JSContextRef, JSC::Bindings::RootObject*, wkj_ref value,
    wkj_ref accessControlContext);

/*
 * A JavaScript value described for Java, replacing JSValue_to_Java_Object. `out` carries the
 * buffer of the caller for a string result; see WKJJSValue for what happens when it does not
 * fit.
 */
void JSValueToWKJValue(JSValueRef, JSContextRef, JSC::Bindings::RootObject*, WKJJSValue* out);

JSGlobalContextRef getGlobalContext(WebCore::ScriptController* sc);

/* Evaluates a script and describes its result. Returns one of the WKJ_JS_* status codes. */
int32_t executeScript(JSObjectRef object,
                      JSContextRef ctx,
                      JSC::Bindings::RootObject* rootPeer,
                      const uint16_t* script,
                      int32_t scriptLength,
                      WKJJSValue* out);
}  // namespace WebCore
