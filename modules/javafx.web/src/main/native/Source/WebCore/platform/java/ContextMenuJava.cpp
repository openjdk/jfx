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

#include "config.h"
#include <wkj_constants.h>
#include "ContextMenuJava.h"
#include "ContextMenu.h"
#include "ContextMenuController.h"
#include "ContextMenuItem.h"
#include "PlatformJavaClasses.h"
#include "WKJDOMUtils.h"

namespace WebCore {

static WKJHandle createJavaMenuItem()
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->context_menu_item_create)
        return WKJHandle();

    WKJHandle item(cb->context_menu_item_create());
    wkjCheckAndClearException();
    return item;
}

class ContextMenuItemJava {
  private:
    WKJHandle m_menuItem;
  public:
    ContextMenuItemJava()
      : m_menuItem(createJavaMenuItem()) {
    }

    void setType(ContextMenuItemType type)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_type) {
            return;
        }

        int32_t jtype = com_sun_webkit_ContextMenuItem_ACTION_TYPE;
        if (ContextMenuItemType::Separator == type) {
            jtype = com_sun_webkit_ContextMenuItem_SEPARATOR_TYPE;
        } else if (ContextMenuItemType::Submenu == type) {
            jtype = com_sun_webkit_ContextMenuItem_SUBMENU_TYPE;
        }
        cb->context_menu_item_set_type(m_menuItem.get(), jtype);
        wkjCheckAndClearException();
    }

    void setAction(ContextMenuAction action)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_action) {
            return;
        }

        cb->context_menu_item_set_action(m_menuItem.get(), static_cast<int32_t>(action));
        wkjCheckAndClearException();
    }

    void setTitle(const String& title)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_title) {
            return;
        }

        /*
         * An EMPTY title is passed as null, not as the empty string: the JNI call read
         * `title.isEmpty() ? NULL : title.toJavaString(env)`, and isEmpty() is true for the
         * null String as well. Feeding a null String to WKJStringArg reproduces both cases
         * with one expression, because it yields (nullptr, 0).
         */
        WKJStringArg titleArg(title.isEmpty() ? String() : title);
        cb->context_menu_item_set_title(m_menuItem.get(), titleArg.data(), titleArg.length());
        wkjCheckAndClearException();
    }

    void setSubMenu(wkj_ref submenu)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_submenu) {
            return;
        }

        cb->context_menu_item_set_submenu(m_menuItem.get(), submenu);
        wkjCheckAndClearException();
    }

    void setChecked(bool checked)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_checked) {
            return;
        }

        cb->context_menu_item_set_checked(m_menuItem.get(), checked ? 1 : 0);
        wkjCheckAndClearException();
    }

    void setEnabled(bool enabled)
    {
        const WKJHostTheme* cb = wkjTheme();
        if (!m_menuItem || !cb || !cb->context_menu_item_set_enabled) {
            return;
        }

        cb->context_menu_item_set_enabled(m_menuItem.get(), enabled ? 1 : 0);
        wkjCheckAndClearException();
    }

    /* Borrowed: ownership stays with this object, exactly as the cast to a raw ref was. */
    wkj_ref ref() const { return m_menuItem.get(); }
};

static WKJHandle createJavaContextMenu()
{
    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->context_menu_create)
        return WKJHandle();

    WKJHandle contextMenu(cb->context_menu_create());
    ASSERT(contextMenu);
    wkjCheckAndClearException();

    return contextMenu;
}

ContextMenuJava::ContextMenuJava(const Vector<ContextMenuItem>& items)
    : m_contextMenu(createJavaContextMenu())
{
    if (!m_contextMenu) {
        return;
    }

    const WKJHostTheme* cb = wkjTheme();

    for (const auto& item : items) {
        if (item.isNull() ||
              (item.type() != ContextMenuItemType::Separator && item.title().isEmpty())) {
            continue;
        }
        ContextMenuItemJava menuItem;
        menuItem.setType(item.type());
        menuItem.setAction(item.action());
        menuItem.setTitle(item.title());
        menuItem.setEnabled(item.enabled());
        menuItem.setChecked(item.checked());
        // Call recursively. The temporary submenu lives to the end of the full expression,
        // which is exactly as long as the borrowed id has to stay valid.
        menuItem.setSubMenu(ContextMenuJava(item.subMenuItems()).m_contextMenu.get());
        if (cb && cb->context_menu_append_item) {
            cb->context_menu_append_item(m_contextMenu.get(), menuItem.ref());
            wkjCheckAndClearException();
        }
    }
}

void ContextMenuJava::show(ContextMenuController* ctrl, wkj_ref page, const IntPoint& loc) const
{
    if (!m_contextMenu) {
        return;
    }

    const WKJHostTheme* cb = wkjTheme();
    if (!cb || !cb->context_menu_show) {
        return;
    }

    cb->context_menu_show(m_contextMenu.get(), page, wkj_from_ptr(ctrl), loc.x(), loc.y());
    wkjCheckAndClearException();
}

} // namespace WebCore

extern "C" {

WKJ_EXPORT void wkj_context_menu_item_selected(int64_t menuCtrlPData, int32_t itemAction)
{
    using namespace WebCore;
    WKJCallScope wkjScope;
    ContextMenuController* cmc = static_cast<ContextMenuController*>(wkj_to_ptr(menuCtrlPData));
    cmc->contextMenuItemSelected((ContextMenuAction)itemAction, "aux"_s);
}

}
