/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "PlatformJavaClasses.h"
#include "PluginData.h"

namespace WebCore {


void init_plugins(bool, Vector<PluginInfo>*) {
/*
    JNIEnv* env = WTF::GetJavaEnv();

    jclass clsPluginManager
        = env->FindClass("com/sun/webkit/plugin/PluginManager");
    ASSERT(clsPluginManager);
    if (!clsPluginManager) {    // for safety
        WTF::CheckAndClearException(env);
        return;
    }


    static jmethodID midGetCount = 0;
    if (!midGetCount) {
        midGetCount = env->GetStaticMethodID(clsPluginManager,
            "getEnabledPluginCount", "()I");

        ASSERT(midGetCount);
        if (!midGetCount) { // for safety
            WTF::CheckAndClearException(env);
            return;
        }
    }

    jint count = 0;
    count = env->CallStaticIntMethod(clsPluginManager, midGetCount);

    if (count > 0) {
        static jmethodID midGetPlugin = 0;
        if (!midGetPlugin) {
            midGetPlugin = env->GetStaticMethodID(clsPluginManager,
                "getEnabledPlugin", "(I)Lcom/sun/webkit/plugin/PluginHandler;");

            ASSERT(midGetPlugin);
            if (!midGetPlugin) {    // for safety
                WTF::CheckAndClearException(env);
                return;
            }
        }

        jclass clsPlugin = env->FindClass("com/sun/webkit/plugin/PluginHandler");
        ASSERT(clsPlugin);
        if (!clsPlugin) {   // for safety
            WTF::CheckAndClearException(env);
            return;
        }

        static jmethodID midGetName = 0;
        if (!midGetName) {
            midGetName = env->GetMethodID(clsPlugin,
                "getName", "()Ljava/lang/String;");

            ASSERT(midGetName);
            if (!midGetName) {
                WTF::CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetFileName = 0;
        if (!midGetFileName) {
            midGetFileName = env->GetMethodID(clsPlugin,
                "getFileName", "()Ljava/lang/String;");

            ASSERT(midGetFileName);
            if (!midGetFileName) {
                WTF::CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetDescr = 0;
        if (!midGetDescr) {
            midGetDescr = env->GetMethodID(clsPlugin,
                "getDescription", "()Ljava/lang/String;");

            ASSERT(midGetDescr);
            if (!midGetDescr) {
                WTF::CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetMimeTypes = 0;
        if (!midGetMimeTypes) {
            midGetMimeTypes = env->GetMethodID(clsPlugin,
                "supportedMIMETypes", "()[Ljava/lang/String;");

            ASSERT(midGetMimeTypes);
            if (!midGetMimeTypes) {
                WTF::CheckAndClearException(env);
                //return; don't return!
            }
        }

        // am: TODO: add getSupportedExtensions

        for (jint i=0; i<count; i++) {
            jobject plugin = NULL;
            plugin = env->CallStaticObjectMethod(clsPluginManager, midGetPlugin, i);
            if (plugin) {
                PluginInfo *info = new PluginInfo();

                jstring jstrName = NULL;
                jstrName = (jstring)env->CallObjectMethod(plugin, midGetName);
                if (jstrName) {
                    info->name = String(env, jstrName);
                    env->DeleteLocalRef(jstrName);
                } else {
                    info->name = "unknown";
                }

                jstring jstrFileName = NULL;
                jstrFileName = (jstring)env->CallObjectMethod(plugin, midGetFileName);
                if (jstrFileName) {
                    info->file = String(env, jstrFileName);
                    env->DeleteLocalRef(jstrFileName);
                } else {
                    info->file = "unknown";
                }

                jstring jstrDescr = NULL;
                jstrDescr = (jstring)env->CallObjectMethod(plugin, midGetDescr);
                if (jstrDescr) {
                    info->desc = String(env, jstrDescr);
                    env->DeleteLocalRef(jstrDescr);
                } else {
                    //info->desc = "n/a";
                }

                jobjectArray jMimes = NULL;
                jMimes = (jobjectArray)env->CallObjectMethod(plugin, midGetMimeTypes);
                if (jMimes) {
                    jint n = env->GetArrayLength(jMimes);
                    for (jint j=0; j<n; j++) {
                        jstring jstrMime = (jstring)env->GetObjectArrayElement(jMimes, j);
                        if (jstrMime) {
                            MimeClassInfo *mime = new MimeClassInfo;
                            mime->type = String(env, jstrMime);
                            mime->desc = "--mime type description--";
                            //mime->suffixes = ;
                            //mime->plugin = info;

                            info->mimes.append(mime);

                            env->DeleteLocalRef(jstrMime);
                        }
                    }
                }

                m_plugins.append(info);
            }
        }
    }
    WTF::CheckAndClearException(env);
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
