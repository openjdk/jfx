/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
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

    private final static ResourceBundle BUNDLE =
            ResourceBundle.getBundle("com.sun.webkit.build", Locale.getDefault());

    static String getProperty(String name) {
        return getProperty(name, null);
    }

    private static String getProperty(String name, String defaultValue) {
        return BUNDLE == null || !BUNDLE.containsKey(name)
                ? defaultValue : BUNDLE.getString(name);
    }

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
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    return MethodUtil.invoke(method, instance, args);
                }
            }, acc);
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

