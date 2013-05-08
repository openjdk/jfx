/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "PluginDataJava.h"

#include "ChromiumBridge.h"

namespace WebCore {

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
            ChromiumBridge::plugins(m_refresh, &m_plugins);
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

static PluginCache pluginCache;

void PluginData::initPlugins(const Page*)
{
    const Vector<PluginInfo>& plugins = pluginCache.plugins();
    for (size_t i = 0; i < plugins.size(); ++i)
        m_plugins.append(plugins[i]);
}

void PluginData::refresh()
{
    pluginCache.reset(true);
    pluginCache.plugins();  // Force the plugins to be reloaded now.
}

String getPluginMimeTypeFromExtension(const String& extension)
{
    const Vector<PluginInfo>& plugins = pluginCache.plugins();
    for (size_t i = 0; i < plugins.size(); ++i) {
        for (size_t j = 0; j < plugins[i].mimes.size(); ++j) {
            const MimeClassInfo& mime = plugins[i].mimes[j];
            const Vector<String>& extensions = mime.extensions;
            for (size_t k = 0; k < extensions.size(); ++k) {
                if (extension == extensions[k])
                    return mime.type;
            }
        }
    }
    return String();
}

} // namespace WebCore
