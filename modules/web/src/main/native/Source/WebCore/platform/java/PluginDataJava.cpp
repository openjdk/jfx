/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "JavaEnv.h"
#include "PluginData.h"

namespace WebCore {


void init_plugins(bool refresh, Vector<PluginInfo> *plugins) {
/*
    JNIEnv* env = WebCore_GetJavaEnv();
 
    jclass clsPluginManager
        = env->FindClass("com/sun/webkit/plugin/PluginManager");
    ASSERT(clsPluginManager);
    if (!clsPluginManager) {    // for safety
        CheckAndClearException(env);
        return;
    }


    static jmethodID midGetCount = 0;
    if (!midGetCount) {
        midGetCount = env->GetStaticMethodID(clsPluginManager,
            "getEnabledPluginCount", "()I");

        ASSERT(midGetCount);
        if (!midGetCount) { // for safety
            CheckAndClearException(env);
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
                CheckAndClearException(env);
                return;
            }
        }

        jclass clsPlugin = env->FindClass("com/sun/webkit/plugin/PluginHandler");
        ASSERT(clsPlugin);
        if (!clsPlugin) {   // for safety
            CheckAndClearException(env);
            return;
        }

        static jmethodID midGetName = 0;
        if (!midGetName) {
            midGetName = env->GetMethodID(clsPlugin,
                "getName", "()Ljava/lang/String;");

            ASSERT(midGetName);
            if (!midGetName) {
                CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetFileName = 0;
        if (!midGetFileName) {
            midGetFileName = env->GetMethodID(clsPlugin,
                "getFileName", "()Ljava/lang/String;");

            ASSERT(midGetFileName);
            if (!midGetFileName) {
                CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetDescr = 0;
        if (!midGetDescr) {
            midGetDescr = env->GetMethodID(clsPlugin,
                "getDescription", "()Ljava/lang/String;");

            ASSERT(midGetDescr);
            if (!midGetDescr) {
                CheckAndClearException(env);
                //return; don't return!
            }
        }

        static jmethodID midGetMimeTypes = 0;
        if (!midGetMimeTypes) {
            midGetMimeTypes = env->GetMethodID(clsPlugin,
                "supportedMIMETypes", "()[Ljava/lang/String;");

            ASSERT(midGetMimeTypes);
            if (!midGetMimeTypes) {
                CheckAndClearException(env);
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
    CheckAndClearException(env);
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

static PluginCache& pluginCache()
{
    DEFINE_STATIC_LOCAL(PluginCache, cache, ());
    return cache;
}

/*
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
