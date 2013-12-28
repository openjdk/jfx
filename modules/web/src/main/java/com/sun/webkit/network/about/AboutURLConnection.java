/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
