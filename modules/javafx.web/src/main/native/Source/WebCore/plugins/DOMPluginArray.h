/*
    Copyright (C) 2008 Nokia Corporation and/or its subsidiary(-ies)
    Copyright (C) 2008 Apple Inc. All rights reserved.

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Library General Public
    License as published by the Free Software Foundation; either
    version 2 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Library General Public License for more details.

    You should have received a copy of the GNU Library General Public License
    along with this library; see the file COPYING.LIB.  If not, write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA 02110-1301, USA.
*/

#pragma once

#include "DOMPlugin.h"
#include "DOMWindowProperty.h"
#include "Navigator.h"
#include "ScriptWrappable.h"
#include <wtf/RefCounted.h>

namespace WebCore {

class PluginData;

class DOMPluginArray final : public ScriptWrappable, public RefCounted<DOMPluginArray> {
    WTF_MAKE_ISO_ALLOCATED(DOMPluginArray);
public:
    static Ref<DOMPluginArray> create(Navigator& navigator) { return adoptRef(*new DOMPluginArray(navigator)); }
    ~DOMPluginArray();

    unsigned length() const;
    RefPtr<DOMPlugin> item(unsigned index);
    RefPtr<DOMPlugin> namedItem(const AtomString& propertyName);
    Vector<AtomString> supportedPropertyNames();

    void refresh(bool reloadPages);

    Navigator* navigator() { return m_navigator.get(); }

private:
    explicit DOMPluginArray(Navigator&);

    PluginData* pluginData() const;
    Frame* frame() const { return m_navigator ? m_navigator->frame() : nullptr; }

    WeakPtr<Navigator> m_navigator;
};

} // namespace WebCore
