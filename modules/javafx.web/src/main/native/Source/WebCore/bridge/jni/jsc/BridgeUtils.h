/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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


namespace WebCore {

/* Returns a local reference to a fresh Java String. */
jstring JSValue_to_Java_String(JSValueRef value, JNIEnv* env, JSContextRef ctx);
jobject JSValue_to_Java_Object(JSValueRef value, JNIEnv* env, JSContextRef ctx, JSC::Bindings::RootObject* rootPeer);
JSValueRef Java_Object_to_JSValue(JNIEnv *env, JSContextRef ctx, JSC::Bindings::RootObject* rootObject, jobject val, jobject accessControlContext);
JSStringRef asJSStringRef(JNIEnv *env, jstring str);
JSGlobalContextRef getGlobalContext(WebCore::ScriptController* sc);
jobject executeScript(JNIEnv* env,
                      JSObjectRef object,
                      JSContextRef ctx,
                      JSC::Bindings::RootObject* rootPeer,
                      jstring script);
}  // namespace WebCore
