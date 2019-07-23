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

#include "config.h"
#include "JavaEnv.h"
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>
#include <wtf/java/JavaRef.h>

#include "WorkQueueItem.h"

extern jstring JSStringRef_to_jstring(JSStringRef ref, JNIEnv* env);
extern JSStringRef jstring_to_JSStringRef(jstring str, JNIEnv* env);

bool LoadItem::invoke() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    JLString jUrl(JSStringRef_to_jstring(m_url.get(), env));
    env->CallStaticObjectMethod(getDumpRenderTreeClass(), getLoadURLMID(), (jstring)jUrl);
    CheckAndClearException(env);

    return true;
}

bool ReloadItem::invoke() const
{
    // FIXME: implement
    return true;
}

bool ScriptItem::invoke() const
{
    // FIXME: implement
    return true;
}

bool BackForwardItem::invoke() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    env->CallStaticObjectMethod(getDumpRenderTreeClass(), getGoBackForward(), m_howFar);
    CheckAndClearException(env);

    return true;
}
