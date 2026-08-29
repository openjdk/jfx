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

#ifndef EventSender_h
#define EventSender_h

#include <JavaScriptCore/JSObjectRef.h>

#include <drt_java_api.h>

// Attaches the JavaScript `eventSender` object to `windowObject`. `eventSender` is the
// registry id of the Java com.sun.javafx.webkit.drt.EventSender that the object forwards
// to; it is retained for the lifetime of the JavaScript wrapper and released in the
// wrapper's finalizer, which is what the JGObject this parameter used to be did.
void makeEventSender(JSContextRef context, JSObjectRef windowObject,
        wkj_ref eventSender, JSValueRef* exception);

#endif // EventSender_h
