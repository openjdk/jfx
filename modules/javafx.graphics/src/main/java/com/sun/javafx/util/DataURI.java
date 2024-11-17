/*
 * Copyright (c) 2021, 2023, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DataURI {

    /**
     * Determines whether the specified URI uses the "data" scheme.
     */
    public static boolean matchScheme(String uri) {
        if (uri == null || uri.length() < 6) {
            return false;
        }

        uri = uri.stripLeading();

        return uri.length() > 5 && "data:".equalsIgnoreCase(uri.substring(0, 5));
    }

    /**
     * Parses the specified URI if it uses the "data" scheme.
     *
     * @return a {@link DataURI} instance if {@code uri} uses the "data" scheme, {@code null} otherwise
     * @throws IllegalArgumentException if the URI is malformed
     */
    public static DataURI tryParse(String uri) {
        if (!matchScheme(uri)) {
            return null;
        }

        uri = uri.trim();

        int dataSeparator = uri.indexOf(',', 5);
        if (dataSeparator < 0) {
            throw new IllegalArgumentException("Invalid URI: " + uri);
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
                        throw new IllegalArgumentException("Invalid URI: " + uri);
                    }

                    base64 = "base64".equalsIgnoreCase(headers[headers.length - 1]);
                } else {
                    if (nameValuePairs.isEmpty()) {
                        nameValuePairs = new HashMap<>();
                    }

                    nameValuePairs.put(header.substring(0, separator).toLowerCase(), header.substring(separator + 1));
                }
            }
        }

        String data = uri.substring(dataSeparator + 1);

        return new DataURI(
            uri,
            data,
            mimeType,
            mimeSubtype,
            nameValuePairs,
            base64,
            base64 ? Base64.getDecoder().decode(data) : decodePercentEncoding(data));
    }

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

    /**
     * Returns the MIME type that was specified in the URI.
     * If no MIME type was specified, returns "text".
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Returns the MIME subtype that was specified in the URI.
     * If no MIME subtype was specified, returns "plain".
     */
    public String getMimeSubtype() {
        return mimeSubtype;
    }

    /**
     * Returns the key-value parameter pairs that were specified in the URI.
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    /**
     * Returns whether the data in the URI is Base64-encoded.
     * If {@code false}, the data is implied to be URL-encoded.
     */
    public boolean isBase64() {
        return base64;
    }

    /**
     * Returns the data that is encoded in this URI.
     * <p>Note that repeated calls to this method will return the same array instance.
     */
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        if (originalData.length() < 32) {
            return originalUri;
        }

        return originalUri.substring(0, originalUri.length() - originalData.length())
            + originalData.substring(0, 14) + "..." + originalData.substring(originalData.length() - 14);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataURI)) return false;
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

    /**
     * Decodes percent-encoded text as specified by RFC 3986, section 2.1
     * This method does not make any assumptions about the allowed character set.
     *
     * @param input the input string
     * @return the decoded byte array
     */
    private static byte[] decodePercentEncoding(String input) {
        enum ExpectedCharacter {
            DEFAULT,
            FIRST_HEX_DIGIT,
            SECOND_HEX_DIGIT
        }

        ExpectedCharacter expectedCharacter = ExpectedCharacter.DEFAULT;
        byte[] output = new byte[computePayloadSize(input)];
        int firstDigit = 0;

        for (int i = 0, j = 0; i < input.length(); ++i) {
            char c = input.charAt(i);

            expectedCharacter = switch (expectedCharacter) {
                case DEFAULT -> {
                    if (c == '%') {
                        yield ExpectedCharacter.FIRST_HEX_DIGIT;
                    } else {
                        output[j++] = (byte)c;
                        yield ExpectedCharacter.DEFAULT;
                    }
                }

                case FIRST_HEX_DIGIT -> {
                    firstDigit = hexDigit(c);
                    yield ExpectedCharacter.SECOND_HEX_DIGIT;
                }

                case SECOND_HEX_DIGIT -> {
                    output[j++] = (byte)(firstDigit << 4 | hexDigit(c));
                    yield ExpectedCharacter.DEFAULT;
                }
            };
        }

        if (expectedCharacter != ExpectedCharacter.DEFAULT) {
            throw new IllegalArgumentException("Incomplete character escape sequence");
        }

        return output;
    }

    /**
     * Computes the payload size of the percent-encoded string.
     *
     * @param input the input string
     * @return the payload size in bytes
     */
    private static int computePayloadSize(String input) {
        int count = 0;

        for (int i = 0, max = input.length(); i < max; ++i) {
            if (input.charAt(i) == '%') {
                i += 2;
            }

            ++count;
        }

        return count;
    }

    private static int hexDigit(char c) {
        int digit = Character.digit(c, 16);
        if (digit < 0) {
            throw new IllegalArgumentException("Invalid symbol in character escape sequence");
        }

        return digit;
    }

}
