/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.text;

import java.lang.annotation.Native;
import java.text.Normalizer;

final class TextNormalizer {

    // Text normalization forms
    @Native private static final int FORM_NFC = 0;
    @Native private static final int FORM_NFD = 1;
    @Native private static final int FORM_NFKC = 2;
    @Native private static final int FORM_NFKD = 3;

    private static String normalize(String data, int type) {
        Normalizer.Form form;
        switch (type) {
        case FORM_NFC:  form = Normalizer.Form.NFC; break;
        case FORM_NFD:  form = Normalizer.Form.NFD; break;
        case FORM_NFKC: form = Normalizer.Form.NFKC; break;
        case FORM_NFKD: form = Normalizer.Form.NFKD; break;
        default:
            throw new IllegalArgumentException("invalid type: " + type);
        }

        return Normalizer.normalize(data, form);
    }
}
