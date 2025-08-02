/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css.media;

public enum SizeQueryType {

    WIDTH("width", MediaQueryContext::getWidth),
    HEIGHT("height", MediaQueryContext::getHeight),
    ASPECT_RATIO("aspect-ratio", context -> context.getWidth() / context.getHeight());

    SizeQueryType(String featureName, SizeSupplier supplier) {
        this.featureName = featureName;
        this.supplier = supplier;
    }

    public static SizeQueryType of(String name) {
        for (SizeQueryType value : VALUES) {
            if (value.featureName.equals(name)) {
                return value;
            }
        }

        throw new IllegalArgumentException(String.format("Unknown media feature <%s>", name));
    }

    private final String featureName;
    private final SizeSupplier supplier;

    public String getFeatureName() {
        return featureName;
    }

    public SizeSupplier getSupplier() {
        return supplier;
    }

    private static final SizeQueryType[] VALUES = values();
}
