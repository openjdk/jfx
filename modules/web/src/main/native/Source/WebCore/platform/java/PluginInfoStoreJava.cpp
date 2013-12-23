/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include <config.h>

#include "JavaEnv.h"
#include "MIMETypeRegistry.h"
#include "PluginData.h"
//#include "PluginInfoStore.h"

namespace WebCore {

/*
PluginInfo* PluginInfoStore::createPluginInfoForPluginAtIndex(unsigned i)
{
    JNIEnv* env = WebCore_GetJavaEnv();
 
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
    CheckAndClearException(env);
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
    CheckAndClearException(env);

    static jmethodID pluginHndGetFileNameMID = 0;
    if (!pluginHndGetFileNameMID) {
        pluginHndGetFileNameMID = env->GetMethodID(
            cls, "getFileName", "()Ljava/lang/String;");
    }
    ASSERT(pluginHndGetFileNameMID);

    jstring pluginFile = (jstring) env->CallObjectMethod(
        hnd, pluginHndGetFileNameMID);
    CheckAndClearException(env);

    static jmethodID pluginHndGetDescription = 0;
    if (!pluginHndGetDescription) {
        pluginHndGetDescription = env->GetMethodID(
            cls, "getDescription", "()Ljava/lang/String;");
    }
    ASSERT(pluginHndGetDescription);

    jstring pluginDescr = (jstring) env->CallObjectMethod(
        hnd, pluginHndGetDescription);
    CheckAndClearException(env);

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
    CheckAndClearException(env);

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

    CheckAndClearException(env);
    return info;
}
*/

/*
unsigned PluginInfoStore::pluginCount() const
{
    JNIEnv* env = WebCore_GetJavaEnv();
 
    jclass cls = env->FindClass("com/sun/webkit/plugin/PluginManager");

    static jmethodID pluginMgrGetEnabledPluginCount = 0;
    if (!pluginMgrGetEnabledPluginCount) {
        pluginMgrGetEnabledPluginCount = env->GetStaticMethodID(
            cls, "getEnabledPluginCount", "()I");
    }
    ASSERT(pluginMgrGetEnabledPluginCount);


    jint res = env->CallStaticIntMethod(cls, pluginMgrGetEnabledPluginCount); 
    CheckAndClearException(env);

    return res;
}
*/

/*
String PluginInfoStore::pluginNameForMIMEType(const String& mimeType)
{
    JNIEnv* env = WebCore_GetJavaEnv();
 
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
    CheckAndClearException(env);

    String res = String(env, name);

    return res;
}
*/
    
/*
bool PluginInfoStore::supportsMIMEType(const String& mimeType)
{
    JNIEnv* env = WebCore_GetJavaEnv();
 
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
     CheckAndClearException(env);

    return res == JNI_TRUE;
}
*/

}
