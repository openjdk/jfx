/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "MIMETypeRegistry.h"
#include <wtf/text/CString.h>

#include <wtf/java/JavaEnv.h>

namespace WebCore {

struct ExtensionMap {
    const char* extension;
    const char* mimeType;
};

static const ExtensionMap extensionMap [] = {
    { "bmp", "image/bmp" },
    { "css", "text/css" },
    { "gif", "image/gif" },
    { "html", "text/html" },
    { "htm", "text/html" },
    { "ico", "image/x-icon" },
    { "jpeg", "image/jpeg" },
    { "jpg", "image/jpeg" },
    { "js", "application/x-javascript" },
    { "pdf", "application/pdf" },
    { "png", "image/png" },
    { "rss", "application/rss+xml" },
    { "svg", "image/svg+xml" },
    { "swf", "application/x-shockwave-flash" },
    { "text", "text/plain" },
    { "txt", "text/plain" },
    { "xbm", "image/x-xbitmap" },
    { "xml", "text/xml" },
    { "xsl", "text/xsl" },
    { "xhtml", "application/xhtml+xml" },
    { "wml", "text/vnd.wap.wml" },
    { "wmlc", "application/vnd.wap.wmlc" },
};

String MIMETypeRegistry::getMIMETypeForExtension(const String &ext)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    ASSERT(env);

    static JGClass cls(env->FindClass("com/sun/webkit/Utilities"));
    ASSERT(cls);

    static jmethodID mid = env->GetStaticMethodID(cls,
        "fwkGetMIMETypeForExtension",
        "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(mid);

    JLString type(static_cast<jstring>(env->CallStaticObjectMethod(cls, mid,
        (jstring)ext.toJavaString(env))));
    CheckAndClearException(env);

    return String(env, type);
}

bool MIMETypeRegistry::isApplicationPluginMIMEType(const String&)
{
    return false;
}

String MIMETypeRegistry::getPreferredExtensionForMIMEType(const String& mimeType)
{
    for (auto& entry : extensionMap) {
        if (equalIgnoringASCIICase(mimeType, entry.mimeType))
            return entry.extension;
    }
    return emptyString();
}

} // namespace WebCore
