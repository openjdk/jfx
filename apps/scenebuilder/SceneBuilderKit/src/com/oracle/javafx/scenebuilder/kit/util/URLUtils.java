/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.javafx.scenebuilder.kit.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Locale;

/**
 *
 */
public class URLUtils {
    
    public static boolean equals(URL url1, URL url2) {
        boolean result;
        
        if (url1 == url2) {
            result = true;
        } else if ((url1 == null) || (url2 == null)) {
            result = false;
        } else {
            try {
                final URI uri1 = url1.toURI();
                final URI uri2 = url2.toURI();
                result = uri1.equals(uri2);
            } catch(URISyntaxException x) {
                result = false; // Emergency code
            }
        }
        
        return result;
    }
    
    /**
     * Constructs a File instance from a file URI.
     * Returns null if it's not a file URI.
     * 
     * @param uri a URI instance (never null).
     * @return null if uri is not a file URI or a File instance
     */
    public static File getFile(URI uri) {
        assert uri != null;
        
        File result;
        final String scheme = uri.getScheme();
        if ((scheme == null) || ! scheme.toLowerCase(Locale.ROOT).equals("file")) { //NOI18N
            result = null;
        } else {
            try {
                result = new File(uri);
            } catch(IllegalArgumentException x) {
                result = null;
            }
        }
        
        return result;
    }
    
    /**
     * Same as URLUtils.getFile(new URI(urlString)).
     * 
     * @param urlString a URL string (never null)
     * @return null or the matching File instance.
     * @throws URISyntaxException if urlString is not a valid URI.
     */
    public static File getFile(String urlString) throws URISyntaxException {
        return getFile(new URI(urlString));
    }
    
    /**
     * Same as URLUtils.getFile(url.toURI()).
     * 
     * @param url a URL (never null)
     * @return null or the matching File instance.
     * @throws URISyntaxException if url cannot be converted to URI.
     */
    public static File getFile(URL url) throws URISyntaxException {
        return getFile(url.toURI());
    }
}
