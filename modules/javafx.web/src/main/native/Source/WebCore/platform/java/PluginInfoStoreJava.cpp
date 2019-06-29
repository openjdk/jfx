/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <config.h>

#include "PlatformJavaClasses.h"
#include "MIMETypeRegistry.h"
#include "PluginData.h"
//#include "PluginInfoStore.h"

namespace WebCore {

/*
PluginInfo* PluginInfoStore::createPluginInfoForPluginAtIndex(unsigned i)
{
    JNIEnv* env = WTF::GetJavaEnv();

    jclass cls = env->FindClass(
        "com/sun/webkit/plugin/PluginManager");
    ASSERT(cls);

    static jmethodID pluginMgrGetEnabledPlugin = 0;
    if (!pluginMgrGetEnabledPlugin) {
        pluginMgrGetEnabledPlugin = env->GetStaticMethodID(cls,
            "getEnabledPlugin",
            "(I)Lcom/sun/webkit/plugin/PluginHandler;");
    }
    ASSERT(pluginMgrGetEnabledPlugin);

    jobject hnd = env->CallStaticObjectMethod(
        cls, pluginMgrGetEnabledPlugin, (jint)i);
    WTF::CheckAndClearException(env);
    ASSERT(hnd);

    cls = env->FindClass("com/sun/webkit/plugin/PluginHandler");
    ASSERT(cls);

    static jmethodID pluginHndGetNameMID = 0;
    if (!pluginHndGetNameMID) {
        pluginHndGetNameMID = env->GetMethodID(
            cls, "getName", "()Ljava/lang/String;");
    }
    ASSERT(pluginHndGetNameMID);

    jstring pluginName = (jstring) env->CallObjectMethod(
        hnd, pluginHndGetNameMID);
    WTF::CheckAndClearException(env);

    static jmethodID pluginHndGetFileNameMID = 0;
    if (!pluginHndGetFileNameMID) {
        pluginHndGetFileNameMID = env->GetMethodID(
            cls, "getFileName", "()Ljava/lang/String;");
    }
    ASSERT(pluginHndGetFileNameMID);

    jstring pluginFile = (jstring) env->CallObjectMethod(
        hnd, pluginHndGetFileNameMID);
    WTF::CheckAndClearException(env);

    static jmethodID pluginHndGetDescription = 0;
    if (!pluginHndGetDescription) {
        pluginHndGetDescription = env->GetMethodID(
            cls, "getDescription", "()Ljava/lang/String;");
    }
    ASSERT(pluginHndGetDescription);

    jstring pluginDescr = (jstring) env->CallObjectMethod(
        hnd, pluginHndGetDescription);
    WTF::CheckAndClearException(env);

    PluginInfo* info = new PluginInfo();
    info->name = String(env, pluginName);
    info->file = String(env, pluginFile);
    info->desc = String(env, pluginDescr);


    static jmethodID pluginHndSupportedMIMETypes = 0;
    if (!pluginHndSupportedMIMETypes) {
        pluginHndSupportedMIMETypes = env->GetMethodID(
            cls, "supportedMIMETypes", "()[Ljava/lang/String;");
    }
    ASSERT(pluginHndSupportedMIMETypes);

    jobjectArray mTypes = (jobjectArray) env->CallObjectMethod(
        hnd, pluginHndSupportedMIMETypes);
    WTF::CheckAndClearException(env);

    jint tCount = env->GetArrayLength(mTypes);

    for (jint i = 0; i < tCount; i++) {

        jstring mType = (jstring) env->GetObjectArrayElement(mTypes, i);
        MimeClassInfo* mime = new MimeClassInfo;
        mime->type = String(env, mType);
        mime->desc = "MimeClassInfo desc";
        Vector<String> exts = MIMETypeRegistry::getExtensionsForMIMEType(
            mime->type);

        if (exts.size() > 0) {
            mime->suffixes += exts[0];
            for(int j = 1; j < exts.size(); j++) {
                mime->suffixes += "," + exts[j];
            }
        }
        mime->plugin = info;
        info->mimes.append(mime);
    }

    WTF::CheckAndClearException(env);
    return info;
}
*/

/*
unsigned PluginInfoStore::pluginCount() const
{
    JNIEnv* env = WTF::GetJavaEnv();

    jclass cls = env->FindClass("com/sun/webkit/plugin/PluginManager");

    static jmethodID pluginMgrGetEnabledPluginCount = 0;
    if (!pluginMgrGetEnabledPluginCount) {
        pluginMgrGetEnabledPluginCount = env->GetStaticMethodID(
            cls, "getEnabledPluginCount", "()I");
    }
    ASSERT(pluginMgrGetEnabledPluginCount);


    jint res = env->CallStaticIntMethod(cls, pluginMgrGetEnabledPluginCount);
    WTF::CheckAndClearException(env);

    return res;
}
*/

/*
String PluginInfoStore::pluginNameForMIMEType(const String& mimeType)
{
    JNIEnv* env = WTF::GetJavaEnv();

    jclass cls = env->FindClass("com/sun/webkit/plugin/PluginManager");
    ASSERT(cls);

    static jmethodID pluginMgrGetPluginNameForMIMEType = 0;
    if (!pluginMgrGetPluginNameForMIMEType) {
        env->GetStaticMethodID(cls, "getPluginNameForMIMEType",
            "(Ljava/lang/String;)Ljava/lang/String;");
    }
    ASSERT(pluginMgrGetPluginNameForMIMEType);

    jstring name = (jstring)env->CallStaticObjectMethod(
        cls, pluginMgrGetPluginNameForMIMEType, mimeType.toJavaString(env));
    WTF::CheckAndClearException(env);

    String res = String(env, name);

    return res;
}
*/

/*
bool PluginInfoStore::supportsMIMEType(const String& mimeType)
{
    JNIEnv* env = WTF::GetJavaEnv();

    jclass cls = env->FindClass("com/sun/webkit/plugin/PluginManager");
    ASSERT(cls);

    static jmethodID pluginMgrSupportsMIMEType = 0;
    if (!pluginMgrSupportsMIMEType) {
        pluginMgrSupportsMIMEType = env->GetStaticMethodID(
            cls, "supportsMIMEType", "(Ljava/lang/String;)Z");
    }
    ASSERT(pluginMgrSupportsMIMEType);

    jboolean res = env->CallStaticBooleanMethod(
        cls, pluginMgrSupportsMIMEType, mimeType.toJavaString(env));
     WTF::CheckAndClearException(env);

    return res == JNI_TRUE;
}
*/

}
