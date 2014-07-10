/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import sun.reflect.misc.MethodUtil;

public abstract class Utilities {

    private static Utilities instance;
    
    public static synchronized void setUtilities(Utilities util) {
        instance = util;
    }

    public static synchronized Utilities getUtilities() {
        return instance;
    }

    protected abstract Pasteboard createPasteboard();
    protected abstract PopupMenu createPopupMenu();
    protected abstract ContextMenu createContextMenu();

    private static String fwkGetMIMETypeForExtension(String ext) {
        return MimeTypeMapHolder.MIME_TYPE_MAP.get(ext);
    }
    
    private static final class MimeTypeMapHolder {
        private static final Map<String, String> MIME_TYPE_MAP =
                createMimeTypeMap();

        private static Map<String,String> createMimeTypeMap() {
            Map<String, String> mimeTypeMap = new HashMap<String, String>(21);
            mimeTypeMap.put("txt", "text/plain");
            mimeTypeMap.put("html", "text/html");
            mimeTypeMap.put("htm", "text/html");
            mimeTypeMap.put("css", "text/css");
            mimeTypeMap.put("xml", "text/xml");
            mimeTypeMap.put("xsl", "text/xsl");
            mimeTypeMap.put("js", "application/x-javascript");
            mimeTypeMap.put("xhtml", "application/xhtml+xml");
            mimeTypeMap.put("svg", "image/svg+xml");
            mimeTypeMap.put("svgz", "image/svg+xml");
            mimeTypeMap.put("gif", "image/gif");
            mimeTypeMap.put("jpg", "image/jpeg");
            mimeTypeMap.put("jpeg", "image/jpeg");
            mimeTypeMap.put("png", "image/png");
            mimeTypeMap.put("tif", "image/tiff");
            mimeTypeMap.put("tiff", "image/tiff");
            mimeTypeMap.put("ico", "image/ico");
            mimeTypeMap.put("cur", "image/ico");
            mimeTypeMap.put("bmp", "image/bmp");
            mimeTypeMap.put("mp3", "audio/mpeg");
            return mimeTypeMap;
        }
    }

    private static Object fwkInvokeWithContext(final Method method,
                                               final Object instance,
                                               final Object[] args,
                                               AccessControlContext acc)
    throws Throwable {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> MethodUtil.invoke(method, instance, args), acc);
        } catch (PrivilegedActionException ex) {
            Throwable cause = ex.getCause();
            if (cause == null)
                cause = ex;
            else if (cause instanceof InvocationTargetException
                && cause.getCause() != null)
                cause = cause.getCause();
            throw cause;
        }
    }
}

