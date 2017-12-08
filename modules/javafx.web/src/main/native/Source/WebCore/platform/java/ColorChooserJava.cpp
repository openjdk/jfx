/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

#if ENABLE(INPUT_TYPE_COLOR)
#include "ColorChooserJava.h"
#include "ColorChooserClient.h"
#include "Color.h"
#include "NotImplemented.h"

namespace WebCore {

// Create ColorChooser JObject and show its dialog
ColorChooserJava::ColorChooserJava(JGObject& webPage, ColorChooserClient* client, const Color& color)
    : m_colorChooserClient(client)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    jmethodID mid = env->GetStaticMethodID(
        PG_GetColorChooserClass(env),
        "fwkCreateAndShowColorChooser",
        "(Lcom/sun/webkit/WebPage;IIIJ)Lcom/sun/webkit/ColorChooser;");
    ASSERT(mid);

    m_colorChooserRef = JGObject(env->CallStaticObjectMethod(
        PG_GetColorChooserClass(env),
        mid,
        (jobject) webPage,
        color.red(),
        color.green(),
        color.blue(),
        ptr_to_jlong(this)));

    ASSERT(m_colorChooserClient);

    CheckAndClearException(env);
}

void ColorChooserJava::reattachColorChooser(const Color& color)
{
    ASSERT(m_colorChooserClient);
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetMethodID(
        PG_GetColorChooserClass(env),
        "fwkShowColorChooser",
        "(III)V");
    ASSERT(mid);

    env->CallVoidMethod(
        m_colorChooserRef,
        mid,
        color.red(),
        color.green(),
        color.blue());
    CheckAndClearException(env);
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
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID mid = env->GetMethodID(
        PG_GetColorChooserClass(env),
        "fwkHideColorChooser",
        "()V");
    ASSERT(mid);

    env->CallVoidMethod(
        m_colorChooserRef,
        mid);
    CheckAndClearException(env);
}

} // namespace WebCore

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL Java_com_sun_webkit_ColorChooser_twkSetSelectedColor
    (JNIEnv*, jobject, jlong self, jint r, jint g, jint b)
{
    ColorChooserJava* cc = static_cast<ColorChooserJava*>jlong_to_ptr(self);
    if (cc) {
        cc->setSelectedColor(Color(r, g, b));
    }
}

#ifdef __cplusplus
}
#endif

#endif // #if ENABLE(INPUT_TYPE_COLOR)
