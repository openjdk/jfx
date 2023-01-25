/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "MIMETypeRegistry.h"
#include <wtf/text/CString.h>

#include "PlatformJavaClasses.h"

namespace WebCore {

struct ExtensionMap {
    ASCIILiteral extension;
    ASCIILiteral mimeType;
};

static const ExtensionMap extensionMap [] = {
    { "bmp"_s, "image/bmp"_s },
    { "css"_s, "text/css"_s },
    { "cur"_s, "image/x-icon"_s },
    { "gif"_s, "image/gif"_s },
    { "html"_s, "text/html"_s },
    { "htm"_s, "text/html"_s },
    { "ico"_s, "image/x-icon"_s },
    { "jpeg"_s, "image/jpeg"_s },
    { "jpg"_s, "image/jpeg"_s },
    { "js"_s, "application/x-javascript"_s },
    { "mp3"_s, "audio/mpeg"_s },
    { "pdf"_s, "application/pdf"_s },
    { "png"_s, "image/png"_s },
    { "rss"_s, "application/rss+xml"_s },
    { "svg"_s, "image/svg+xml"_s },
    { "svgz"_s, "image/svg+xml"_s },
    { "swf"_s, "application/x-shockwave-flash"_s },
    { "text"_s, "text/plain"_s },
    { "tif"_s, "image/tiff"_s },
    { "tiff"_s, "image/tiff"_s },
    { "txt"_s, "text/plain"_s },
    { "xbm"_s, "image/x-xbitmap"_s },
    { "xml"_s, "text/xml"_s },
    { "xsl"_s, "text/xsl"_s },
    { "xht"_s, "application/xhtml+xml"_s },
    { "xhtml"_s, "application/xhtml+xml"_s },
    { "wml"_s, "text/vnd.wap.wml"_s },
    { "wmlc"_s, "application/vnd.wap.wmlc"_s },
};

String MIMETypeRegistry::mimeTypeForExtension(const StringView extension)
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

String MIMETypeRegistry::preferredExtensionForMIMEType(const String& mimeType)
{
    for (auto& entry : extensionMap) {
        if (equalIgnoringASCIICase(mimeType, entry.mimeType))
            return entry.extension;
    }
    return emptyString();
}

Vector<String> MIMETypeRegistry::extensionsForMIMEType(const String&)
{
    ASSERT_NOT_IMPLEMENTED_YET();
    return { };
}

} // namespace WebCore
