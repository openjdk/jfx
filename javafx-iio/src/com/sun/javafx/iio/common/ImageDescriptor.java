/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.iio.common;

import com.sun.javafx.iio.ImageFormatDescription;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ImageDescriptor implements ImageFormatDescription {
    private final String formatName;
    private final List<String> extensions;
    private final List<Signature> signatures;
//    private String[] MIMETypes;

    public ImageDescriptor(String formatName, String[] extensions, Signature[] signatures) {
        this.formatName = formatName;
        this.extensions = Collections.unmodifiableList(
                                          Arrays.asList(extensions));
        this.signatures = Collections.unmodifiableList(
                                          Arrays.asList(signatures));
//        this.MIMETypes = MIMETypes;
    }

    public String getFormatName() {
        return formatName;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public List<Signature> getSignatures() {
        return signatures;
    }

//    public String[] getMIMETypes() {
//        return MIMETypes;
//    }
}
