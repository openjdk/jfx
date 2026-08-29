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
#include "PluginData.h"

namespace WebCore {


void init_plugins(bool, Vector<PluginInfo>*) {
/*
 * The body was a 150-line commented-out block that enumerated NPAPI plugins through
 * com.sun.webkit.plugin.PluginManager and PluginHandler. It named a JNI environment,
 * cached method ids and a Java array of strings, none of which exists any more.
 *
 * FFM-AUDIT-wtf-webcore.md section 7 counts those calls among the tree upcall sites, but
 * they are dead comments, so they get no slot in WKJHostTheme: building a C ABI for code
 * that has been commented out for years would entrench it rather than remove it. The
 * function keeps its empty body and its callers, which are themselves unreachable.
 */
}


class PluginCache {
public:
    PluginCache() : m_loaded(false), m_refresh(false) {}
    ~PluginCache() { reset(false); }

    void reset(bool refresh)
    {
        m_plugins.clear();
        m_loaded = false;
        m_refresh = refresh;
    }

    const Vector<PluginInfo>& plugins()
    {
        if (!m_loaded) {
            init_plugins(m_refresh, &m_plugins);
            m_loaded = true;
            m_refresh = false;
        }
        return m_plugins;
    }

private:
    Vector<PluginInfo> m_plugins;
    bool m_loaded;
    bool m_refresh;
};

/*
static PluginCache& pluginCache()
{
    LazyNeverDestroyed<PluginCache> cache;
    return cache.get();
}

void PluginData::initPlugins(const Page*)
{
    const Vector<PluginInfo>& plugins = pluginCache().plugins();
    for (size_t i = 0; i < plugins.size(); ++i)
        m_plugins.append(plugins[i]);
}

void PluginData::refresh()
{
    pluginCache().reset(true);
    pluginCache().plugins(); // Force the plugins to be reloaded now.
}
*/

}
