/*
 * Copyright (c) 2016, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include <WebCore/WKJDOMUtils.h>

namespace WebCore {

namespace {

/*
 * The process-wide colour chooser callbacks, installed once by
 * wkj_install_color_chooser_callbacks. A chooser belongs to a page but two of the three
 * upcalls are made on the com.sun.webkit.ColorChooser the first one returns rather than on
 * the page, so there is nothing per page to hold.
 */
const WKJColorChooserCallbacks* s_wkjColorChooserCallbacks = nullptr;

} // namespace

// Create the Java ColorChooser and show its dialog
ColorChooserJava::ColorChooserJava(wkj_ref webPage, ColorChooserClient* client, const Color& color)
    : m_colorChooserClient(client)
{
    ASSERT(m_colorChooserClient);

    if (!s_wkjColorChooserCallbacks || !s_wkjColorChooserCallbacks->create_and_show)
        return;

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<uint8_t>>().resolved();
    m_colorChooserRef = WKJHandle(s_wkjColorChooserCallbacks->create_and_show(
        webPage, r, g, b, wkj_from_ptr(this)));
}

void ColorChooserJava::reattachColorChooser(const Color& color)
{
    ASSERT(m_colorChooserClient);

    if (!s_wkjColorChooserCallbacks || !s_wkjColorChooserCallbacks->show)
        return;

    auto [r, g, b, a] = color.toColorTypeLossy<SRGBA<uint8_t>>().resolved();
    s_wkjColorChooserCallbacks->show(m_colorChooserRef.get(), r, g, b);
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
    if (!s_wkjColorChooserCallbacks || !s_wkjColorChooserCallbacks->hide)
        return;

    s_wkjColorChooserCallbacks->hide(m_colorChooserRef.get());
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_install_color_chooser_callbacks(const WKJColorChooserCallbacks* callbacks)
{
    WebCore::WKJCallScope wkjScope;
    WebCore::s_wkjColorChooserCallbacks = callbacks;
}

WKJ_EXPORT void wkj_color_chooser_set_selected(int64_t chooser, int32_t red, int32_t green,
                                               int32_t blue)
{
    WebCore::WKJCallScope wkjScope;
    using namespace WebCore;
    ColorChooserJava* cc = static_cast<ColorChooserJava*>(wkj_to_ptr(chooser));
    if (cc) {
        cc->setSelectedColor(makeFromComponentsClamping<SRGBA<uint8_t>>(red, green, blue));
    }
}

}

#endif // #if ENABLE(INPUT_TYPE_COLOR)
