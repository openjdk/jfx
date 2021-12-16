/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/RefPtr.h>
#include <wtf/GetPtr.h>

#include <WebCore/JavaDOMUtils.h>
#include <wtf/java/JavaEnv.h>

using namespace WebCore;

extern "C" {

#define IMPL (static_cast<HTMLAppletElement*>(jlong_to_ptr(peer)))


// Attributes
JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getAlignImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setAlignImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getAltImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setAltImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getArchiveImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setArchiveImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getCodeImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setCodeImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getCodeBaseImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setCodeBaseImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getHeightImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setHeightImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getHspaceImpl(JNIEnv*, jclass, jlong peer)
{
    return 0;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setHspaceImpl(JNIEnv*, jclass, jlong peer, jint value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getNameImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setNameImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getObjectImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setObjectImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

JNIEXPORT jint JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getVspaceImpl(JNIEnv*, jclass, jlong peer)
{
    return 0;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setVspaceImpl(JNIEnv*, jclass, jlong peer, jint value)
{
}

JNIEXPORT jstring JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_getWidthImpl(JNIEnv* env, jclass, jlong peer)
{
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_dom_HTMLAppletElementImpl_setWidthImpl(JNIEnv* env, jclass, jlong peer, jstring value)
{
}

}
