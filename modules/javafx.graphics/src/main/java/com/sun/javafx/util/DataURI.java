/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.util;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataURI {

    /**
     * Returns whether the string has the form of a valid data URI, but does not try
     * to decode the URI data. If this method returns {@code true}, parsing the URI
     * with {@link #tryParse(String)} might still fail if the data is invalid.
     */
    public static boolean isValid(String uri) {
        return decode(uri, true) != null;
    }

    public static DataURI tryParse(String uri) {
        return decode(uri, false);
    }

    private static DataURI decode(String uri, boolean checkValidityOnly) {
        if (uri == null || uri.length() < 6) {
            return null;
        }

        if (!"data:".equalsIgnoreCase(uri.substring(0, 5))) {
            return null;
        }

        int dataSeparator = uri.indexOf(',', 5);
        if (dataSeparator < 0) {
            return null;
        }

        String mimeType = "text", mimeSubtype = "plain";
        boolean base64 = false;

        String[] headers = uri.substring(5, dataSeparator).split(";");
        Map<String, String> nameValuePairs = Collections.emptyMap();

        if (headers.length > 0) {
            int start = 0;

            int mimeSeparator = headers[0].indexOf('/');
            if (mimeSeparator > 0) {
                mimeType = headers[0].substring(0, mimeSeparator);
                mimeSubtype = headers[0].substring(mimeSeparator + 1);
                start = 1;
            }

            for (int i = start; i < headers.length; ++i) {
                String header = headers[i];
                int separator = header.indexOf('=');
                if (separator < 0) {
                    if (i < headers.length - 1) {
                        return null;
                    }

                    base64 = "base64".equalsIgnoreCase(headers[headers.length - 1]);
                } else if (!checkValidityOnly) {
                    if (nameValuePairs.isEmpty()) {
                        nameValuePairs = new HashMap<>();
                    }

                    nameValuePairs.put(header.substring(0, separator).toLowerCase(), header.substring(separator + 1));
                }
            }
        }

        if (checkValidityOnly) {
            return VALID_STUB;
        }

        String data = uri.substring(dataSeparator + 1);
        Charset charset = Charset.defaultCharset();

        return new DataURI(
            uri,
            data,
            mimeType,
            mimeSubtype,
            nameValuePairs,
            base64,
            base64 ?
                Base64.getDecoder().decode(data) :
                URLDecoder.decode(data.replace("+", "%2B"), charset).getBytes(charset));
    }

    private static final DataURI VALID_STUB = new DataURI(null, null, null, null, null, false, null);

    private final String originalUri;
    private final String originalData;
    private final String mimeType, mimeSubtype;
    private final Map<String, String> parameters;
    private final boolean base64;
    private final byte[] data;

    private DataURI(
            String originalUri,
            String originalData,
            String mimeType,
            String mimeSubtype,
            Map<String, String> parameters,
            boolean base64,
            byte[] decodedData) {
        this.originalUri = originalUri;
        this.originalData = originalData;
        this.mimeType = mimeType;
        this.mimeSubtype = mimeSubtype;
        this.parameters = parameters;
        this.base64 = base64;
        this.data = decodedData;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getMimeSubtype() {
        return mimeSubtype;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isBase64() {
        return base64;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        if (originalData.length() < 30) {
            return originalUri;
        }

        return originalUri.substring(0, originalUri.length() - originalData.length())
            + originalData.substring(0, 14) + "..." + originalData.substring(originalData.length() - 14);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataURI dataURI = (DataURI)o;
        return base64 == dataURI.base64
            && Objects.equals(mimeType, dataURI.mimeType)
            && Objects.equals(mimeSubtype, dataURI.mimeSubtype)
            && Arrays.equals(data, dataURI.data);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mimeType, mimeSubtype, base64);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

}
