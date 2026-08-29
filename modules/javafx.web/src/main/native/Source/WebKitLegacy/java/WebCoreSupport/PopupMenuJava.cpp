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


#include "PopupMenuJava.h"
#include "WebPage.h"
#include <WebCore/Color.h>
#include <WebCore/Font.h>
#include <WebCore/Frame.h>
#include "FrameInlines.h"
#include "DocumentPage.h"
#include <WebCore/FrameView.h>
#include <WebCore/NotImplemented.h>
#include <WebCore/Page.h>
#include <WebCore/PlatformJavaClasses.h>
#include <WebCore/PopupMenuClient.h>
#include <WebCore/WKJDOMUtils.h>

#include <wtf/text/WTFString.h>

namespace WebCore {

namespace {

/*
 * The process-wide popup menu callbacks, installed once by wkj_install_popup_callbacks.
 * `create` is a static Java method and the other five are made on the PopupMenu it returns,
 * so nothing here is addressed by page and there is nothing per page to hold.
 */
const WKJPopupCallbacks* s_wkjPopupCallbacks = nullptr;

void setSelectedItem(wkj_ref popup, int32_t index)
{
    if (s_wkjPopupCallbacks && s_wkjPopupCallbacks->set_selected_item)
        s_wkjPopupCallbacks->set_selected_item(popup, index);
}

} // namespace

PopupMenuJava::PopupMenuJava(PopupMenuClient* client)
    : m_popupClient(client)
{
}

PopupMenuJava::~PopupMenuJava()
{
    if (!m_popup)
        return;

    if (s_wkjPopupCallbacks && s_wkjPopupCallbacks->destroy)
        s_wkjPopupCallbacks->destroy(m_popup.get());
}

void PopupMenuJava::createPopupMenuJava(Page*)
{
    if (!s_wkjPopupCallbacks || !s_wkjPopupCallbacks->create)
        return;

    /* The id is retained here, as the global reference the JNI code took was. */
    m_popup = WKJHandle(s_wkjPopupCallbacks->create(wkj_from_ptr(this)));
    ASSERT(m_popup);
}

void PopupMenuJava::populate()
{
    if (!s_wkjPopupCallbacks || !s_wkjPopupCallbacks->append_item)
        return;

    for (int i = 0; i < client()->listSize(); i++) {
        String itemText = client()->itemText(i);
        WKJStringArg itemTextArg(itemText);
        PopupMenuStyle style = client()->itemStyle(i);
        auto [r1, g1, b1, a1] = style.backgroundColor().toColorTypeLossy<SRGBA<uint8_t>>().resolved();
        auto [r2, g2, b2, a2] = style.foregroundColor().toColorTypeLossy<SRGBA<uint8_t>>().resolved();

        /*
         * The WCFont the JNI code passed as a raw Java reference straight out of nativeFontData() is
         * the same object, now named by its registry id.
         */
        RefPtr<RQRef> fontData = style.font().primaryFont().get().platformData().nativeFontData();
        wkj_ref font = fontData ? static_cast<wkj_ref>(*fontData) : 0;

        s_wkjPopupCallbacks->append_item(m_popup.get(),
                            itemTextArg.data(), itemTextArg.length(),
                            client()->itemIsLabel(i) ? 1 : 0,
                            client()->itemIsSeparator(i) ? 1 : 0,
                            client()->itemIsEnabled(i) ? 1 : 0,
                            (int32_t)(a1 << 24 | r1 << 16 | g1 << 8 | b1),
                            (int32_t)(a2 << 24 | r2 << 16 | g2 << 8 | b2),
                            font);
    }
}

void PopupMenuJava::show(const IntRect& r,  LocalFrameView& frameView, int selectedIndex)
{
    ASSERT(frameView.frame().page());

    createPopupMenuJava(frameView.frame().page());
    populate();
    setSelectedItem(m_popup.get(), selectedIndex);

    // r is in contents coordinates, while popup menu expects window coordinates
    IntRect wr = frameView.contentsToWindow(r);

    if (!s_wkjPopupCallbacks || !s_wkjPopupCallbacks->show)
        return;

    WKJHandle page = WebPage::jobjectFromPage(frameView.frame().page());
    s_wkjPopupCallbacks->show(
            m_popup.get(),
            page.get(),
            wr.x(),
            wr.y() + wr.height(),
            wr.width());
}

void PopupMenuJava::hide()
{
    if (s_wkjPopupCallbacks && s_wkjPopupCallbacks->hide)
        s_wkjPopupCallbacks->hide(m_popup.get());
}

void PopupMenuJava::updateFromElement()
{
    client()->setTextFromItem(client()->popupSelectedIndex());
    if (!m_popup) {
        return;
    }
    setSelectedItem(m_popup.get(), client()->popupSelectedIndex());
}

void PopupMenuJava::disconnectClient()
{
    m_popupClient = 0;
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_install_popup_callbacks(const WKJPopupCallbacks* callbacks)
{
    WebCore::WKJCallScope wkjScope;
    WebCore::s_wkjPopupCallbacks = callbacks;
}

WKJ_EXPORT void wkj_popup_selection_committed(int64_t popup, int32_t index)
{
    WebCore::WKJCallScope wkjScope;
    using namespace WebCore;
    if (!popup) {
        return;
    }

    PopupMenuJava* pPopupMenu = static_cast<PopupMenuJava*>(wkj_to_ptr(popup));
    ASSERT(pPopupMenu);

    if (pPopupMenu->client()) {
        pPopupMenu->client()->valueChanged(index);
    }
}

WKJ_EXPORT void wkj_popup_closed(int64_t popup)
{
    WebCore::WKJCallScope wkjScope;
    using namespace WebCore;
    if (!popup) {
        return;
    }

    PopupMenuJava* pPopupMenu = static_cast<PopupMenuJava*>(wkj_to_ptr(popup));
    ASSERT(pPopupMenu);

    if (pPopupMenu->client()) {
        pPopupMenu->client()->popupDidHide();
    }
}

}
