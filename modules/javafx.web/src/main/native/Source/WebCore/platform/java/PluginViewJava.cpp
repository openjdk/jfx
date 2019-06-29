/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include "NotImplemented.h"

#include "PlatformJavaClasses.h"
#include "StringJava.h"
// #include "PluginPackage.h" //XXX: win only?
// #include "PluginView.h" //XXX: win only?

#include "com_sun_webkit_WCPluginWidget.h"

namespace WebCore {

/*
PluginWidgetJava::PluginWidgetJava(jobject wfh, const IntSize& size,
                                   const String& url, const String& mimeType,
                                   const Vector<String>& paramNames,
                                   const Vector<String>& paramValues)
{
    m_size = size;
    m_url = url;
    m_mimeType = mimeType;
    m_paramNames = paramNames;
    m_paramValues = paramValues;

    JNIEnv* env = WTF::GetJavaEnv();
    jstring urlJavaString = url.toJavaString(env);
    jstring mimeTypeJavaString = mimeType.toJavaString(env);

    jclass cls = env->FindClass("com/sun/webkit/WCPluginWidget");
    ASSERT(cls);

    jobjectArray pNames = strVect2JArray(env, paramNames);

    jobjectArray pValues = strVect2JArray(env, paramValues);

    jobject obj = env->CallStaticObjectMethod(cls, pluginWidgetCreateMID, wfh,
        url.toJavaString(env), mimeTypeJavaString, pNames, pValues);
    WTF::CheckAndClearException(env);
    ASSERT(obj);
    if (obj) {
        setJavaObject(obj);
        env->CallVoidMethod(obj, pluginWidgetFWKInitMID, size.width(),
                        size.height(), url.toJavaString(env));
        WTF::CheckAndClearException(env);
        env->DeleteLocalRef(obj);
    }
    env->DeleteLocalRef(cls);
    env->DeleteLocalRef(pNames);
    env->DeleteLocalRef(pValues);
}
*/

/*
PluginWidgetJava::~PluginWidgetJava()
{
    JNIEnv* env = WTF::GetJavaEnv();

    env->CallVoidMethod(javaObject(), pluginWidgetFWKDestroyMID);
    WTF::CheckAndClearException(env);
}
*/

/* //XXX recheck
PluginView::~PluginView()
{
    notImplemented();
}

void PluginView::paint(GraphicsContext* gc, const IntRect& r)
{
    notImplemented();
}

void PluginView::invalidateRect(const IntRect& r)
{
    notImplemented();
}

void PluginView::setFocus()
{
    notImplemented();
}

void PluginView::show()
{
    notImplemented();
}

void PluginView::hide()
{
    notImplemented();
}

void PluginView::setParent(ScrollView* parent)
{
    notImplemented();
}

void PluginView::setParentVisible(bool visible)
{
    notImplemented();
}
*/

} // namespace WebCore
