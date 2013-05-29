/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network.about;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public final class Handler extends URLStreamHandler {

    public Handler() {
    }


    @Override
    protected URLConnection openConnection(URL url) {
        return new AboutURLConnection(url);
    }
}
