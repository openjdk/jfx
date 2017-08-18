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
    { "cur", "image/x-icon" },
    { "gif", "image/gif" },
    { "html", "text/html" },
    { "htm", "text/html" },
    { "ico", "image/x-icon" },
    { "jpeg", "image/jpeg" },
    { "jpg", "image/jpeg" },
    { "js", "application/x-javascript" },
    { "mp3", "audio/mpeg"},
    { "pdf", "application/pdf" },
    { "png", "image/png" },
    { "rss", "application/rss+xml" },
    { "svg", "image/svg+xml" },
    { "svgz", "image/svg+xml" },
    { "swf", "application/x-shockwave-flash" },
    { "text", "text/plain" },
    { "tif", "image/tiff" },
    { "tiff", "image/tiff" },
    { "txt", "text/plain" },
    { "xbm", "image/x-xbitmap" },
    { "xml", "text/xml" },
    { "xsl", "text/xsl" },
    { "xht", "application/xhtml+xml" },
    { "xhtml", "application/xhtml+xml" },
    { "wml", "text/vnd.wap.wml" },
    { "wmlc", "application/vnd.wap.wmlc" },
};

String MIMETypeRegistry::getMIMETypeForExtension(const String& extension)
{
    for (auto& entry : extensionMap) {
        if (equalIgnoringASCIICase(extension, entry.extension))
            return entry.mimeType;
    }
    return String();
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
