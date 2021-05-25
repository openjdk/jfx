/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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


#if ENABLE(INPUT_TYPE_COLOR)
#include "ColorChooserJava.h"
#include <WebCore/ColorChooserClient.h>
#include <WebCore/Color.h>
#include <WebCore/NotImplemented.h>

namespace WebCore {

// Create ColorChooser JObject and show its dialog
ColorChooserJava::ColorChooserJava(JGObject& webPage, ColorChooserClient* client, const Color& color)
    : m_colorChooserClient(client)
{
    JNIEnv* env = WTF::GetJavaEnv();

    jmethodID mid = env->GetStaticMethodID(
        PG_GetColorChooserClass(env),
        "fwkCreateAndShowColorChooser",
        "(Lcom/sun/webkit/WebPage;IIIJ)Lcom/sun/webkit/ColorChooser;");
    ASSERT(mid);


    m_colorChooserRef = JGObject(env->CallStaticObjectMethod(
        PG_GetColorChooserClass(env),
        mid,
        (jobject) webPage,
        color.red,
        color.green,
        color.blue,
        ptr_to_jlong(this)));

    ASSERT(m_colorChooserClient);

    WTF::CheckAndClearException(env);
}

void ColorChooserJava::reattachColorChooser(const Color& color)
{
    ASSERT(m_colorChooserClient);
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(
        PG_GetColorChooserClass(env),
        "fwkShowColorChooser",
        "(III)V");
    ASSERT(mid);

    env->CallVoidMethod(
        m_colorChooserRef,
        mid,
        color.red,
        color.green,
        color.blue);
    WTF::CheckAndClearException(env);
}

void ColorChooserJava::setSelectedColor(const Color& color)
{
    if (!m_colorChooserClient) {
        return;
    }

    m_colorChooserClient->didChooseColor(color);
}

void ColorChooserJava::endChooser()
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID mid = env->GetMethodID(
        PG_GetColorChooserClass(env),
        "fwkHideColorChooser",
        "()V");
    ASSERT(mid);

    env->CallVoidMethod(
        m_colorChooserRef,
        mid);
    WTF::CheckAndClearException(env);
}

} // namespace WebCore

extern "C" {

JNIEXPORT void JNICALL Java_com_sun_webkit_ColorChooser_twkSetSelectedColor
    (JNIEnv*, jobject, jlong self, jint r, jint g, jint b)
{
    using namespace WebCore;
    ColorChooserJava* cc = static_cast<ColorChooserJava*>jlong_to_ptr(self);
    if (cc) {
        cc->setSelectedColor(clampToComponentBytes<SRGBA>(r, g, b));
    }
}

}

#endif // #if ENABLE(INPUT_TYPE_COLOR)
