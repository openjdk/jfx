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

package com.sun.webkit.network.about;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;


/**
 * Implementation of the about: protocol handler
 */
final class AboutURLConnection extends URLConnection {
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final String DEFAULT_MIMETYPE = "text/html";

    private final AboutRecord record;

    AboutURLConnection(URL url) {
        super(url);
        record = new AboutRecord("");
    }

    @Override
    public void connect() throws IOException {
        if (connected) {
            return;
        }
        connected = (record != null);
        if (connected) {
            record.content.reset();
            return;
        }
        throw new ProtocolException("The URL is not valid and cannot be loaded.");
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        connect();
        return record.content;
    }

    @Override
    public String getContentType()
    {
        try {
            connect();
            if (record.contentType != null) {
                return record.contentType;
            }
        } catch (IOException ex) {
        }
        return DEFAULT_MIMETYPE;
    }

    @Override
    public String getContentEncoding()
    {
        try {
            connect();
            if (record.contentEncoding != null) {
                return record.contentEncoding;
            }
        } catch (IOException ex) {
        }
        return DEFAULT_CHARSET;
    }

    @Override
    public int getContentLength()
    {
        try {
            connect();
            return record.contentLength;
        } catch (IOException ex) {
            //returning -1 means 'unknown length'
            return -1;
        }
    }

    private static final class AboutRecord {
        private final InputStream content;
        private final int contentLength;
        private final String contentEncoding;
        private final String contentType;

        private AboutRecord(String info) {
            byte[] bytes = new byte[0];
            try {
                bytes = info.getBytes(AboutURLConnection.DEFAULT_CHARSET);
            } catch (UnsupportedEncodingException ex) {
                //never happens
            }
            this.content = new ByteArrayInputStream(bytes);
            this.contentLength = bytes.length;
            this.contentEncoding = AboutURLConnection.DEFAULT_CHARSET;
            this.contentType = AboutURLConnection.DEFAULT_MIMETYPE;
        }
    }
}
