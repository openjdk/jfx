/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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


#include "PopupMenuJava.h"
#include "WebPage.h"
#include <WebCore/Color.h>
#include <WebCore/Font.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameView.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/PopupMenuClient.h>

#include <wtf/text/WTFString.h>

#include "com_sun_webkit_PopupMenu.h"

static jclass getJPopupMenuClass()
{
    JNIEnv* env = WTF::GetJavaEnv();
    static JGClass jPopupMenuClass(env->FindClass("com/sun/webkit/PopupMenu"));
    ASSERT(jPopupMenuClass);
    return (jclass)jPopupMenuClass;
}

static void setSelectedItem(jobject popup, jint index)
{
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID setSelectedItemMID = env->GetMethodID(
        getJPopupMenuClass(), "fwkSetSelectedItem", "(I)V");
    ASSERT(setSelectedItemMID);

    env->CallVoidMethod(popup, setSelectedItemMID, index);
    WTF::CheckAndClearException(env);
}

namespace WebCore {

PopupMenuJava::PopupMenuJava(PopupMenuClient* client)
    : m_popupClient(client),
      m_popup(0)
{
}

PopupMenuJava::~PopupMenuJava()
{
    if (!m_popup)
        return;

    WC_GETJAVAENV_CHKRET(env);

    static jmethodID mid = env->GetMethodID(getJPopupMenuClass(),
        "fwkDestroy", "()V");
    ASSERT(mid);

    env->CallVoidMethod(m_popup, mid);
    WTF::CheckAndClearException(env);
}

void PopupMenuJava::createPopupMenuJava(Page*)
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetStaticMethodID(getJPopupMenuClass(),
        "fwkCreatePopupMenu", "(J)Lcom/sun/webkit/PopupMenu;");
    ASSERT(mid);

    JLObject jPopupMenu(env->CallStaticObjectMethod(getJPopupMenuClass(), mid, ptr_to_jlong(this)));
    ASSERT(jPopupMenu);
    WTF::CheckAndClearException(env);

    m_popup = jPopupMenu;
}

void PopupMenuJava::populate()
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJPopupMenuClass(),
        "fwkAppendItem", "(Ljava/lang/String;ZZZIILcom/sun/webkit/graphics/WCFont;)V");
    ASSERT(mid);

    for (int i = 0; i < client()->listSize(); i++) {
        String itemText = client()->itemText(i);
        JLString itemTextJ(itemText.toJavaString(env));
        ASSERT(itemTextJ);
        PopupMenuStyle style = client()->itemStyle(i);
        auto [r1, g1, b1, a1] = style.backgroundColor().toColorTypeLossy<SRGBA<uint8_t>>().resolved();
        auto [r2, g2, b2, a2] = style.foregroundColor().toColorTypeLossy<SRGBA<uint8_t>>().resolved();
        env->CallVoidMethod(m_popup, mid, (jstring)itemTextJ,
                            bool_to_jbool(client()->itemIsLabel(i)),
                            bool_to_jbool(client()->itemIsSeparator(i)),
                            bool_to_jbool(client()->itemIsEnabled(i)),
                            (jint)(a1 << 24 | r1 << 16 | g1 << 8 | b1),
                            (jint)(a2 << 24 | r2 << 16 | g2 << 8 | b2),
                            (jobject)*style.font().primaryFont().platformData().nativeFontData());
        WTF::CheckAndClearException(env);
    }
}

void PopupMenuJava::show(const IntRect& r, LocalFrameView* frameView, int index)
{
    JNIEnv* env = WTF::GetJavaEnv();

    ASSERT(frameView->frame().page());

    createPopupMenuJava(frameView->frame().page());
    populate();
    setSelectedItem(m_popup, index);

    // r is in contents coordinates, while popup menu expects window coordinates
    IntRect wr = frameView->contentsToWindow(r);

    static jmethodID mid = env->GetMethodID(
            getJPopupMenuClass(),
            "fwkShow",
            "(Lcom/sun/webkit/WebPage;III)V");
    ASSERT(mid);

    env->CallVoidMethod(
            m_popup,
            mid,
            (jobject) WebPage::jobjectFromPage(frameView->frame().page()),
            wr.x(),
            wr.y() + wr.height(),
            wr.width());
    WTF::CheckAndClearException(env);
}

void PopupMenuJava::hide()
{
    JNIEnv* env = WTF::GetJavaEnv();

    static jmethodID mid = env->GetMethodID(getJPopupMenuClass(), "fwkHide", "()V");
    ASSERT(mid);

    env->CallVoidMethod((jobject)m_popup, mid);
    WTF::CheckAndClearException(env);
}

void PopupMenuJava::updateFromElement()
{
    client()->setTextFromItem(client()->selectedIndex());
    if (!m_popup) {
        return;
    }
    setSelectedItem(m_popup, client()->selectedIndex());
}

void PopupMenuJava::disconnectClient()
{
    m_popupClient = 0;
}

} // namespace WebCore

JNIEXPORT void JNICALL Java_com_sun_webkit_PopupMenu_twkSelectionCommited
    (JNIEnv*, jobject, jlong pdata, jint index)
{
    using namespace WebCore;
    if (!pdata) {
        return;
    }

    PopupMenuJava* pPopupMenu = static_cast<PopupMenuJava*>(jlong_to_ptr(pdata));
    ASSERT(pPopupMenu);

    if (pPopupMenu->client()) {
        pPopupMenu->client()->valueChanged(index);
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_PopupMenu_twkPopupClosed
    (JNIEnv*, jobject, jlong pdata)
{
    using namespace WebCore;
    if (!pdata) {
        return;
    }

    PopupMenuJava* pPopupMenu = static_cast<PopupMenuJava*>(jlong_to_ptr(pdata));
    ASSERT(pPopupMenu);

    if (pPopupMenu->client()) {
        pPopupMenu->client()->popupDidHide();
    }
}
