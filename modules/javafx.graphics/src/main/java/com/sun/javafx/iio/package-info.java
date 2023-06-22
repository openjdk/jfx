/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Provides image loading capability for Java FX.
 *
 * <p>A plugin for loading a given format is added by creating an
 * <code>ImageFormatDescription</code> to provide the principal attributes for
 * recognizing images stored in the format, an <code>ImageLoader</code> which
 * performs the actual loading of the image data and metadata, and an
 * <code>ImageLoaderFactory</code> which is able to create an
 * <code>ImageLoader</code> for a stream of image data stored in the format.
 * The <code>ImageLoaderFactory</code> is registered with the
 * <code>ImageStorage</code> object which manages all <code>ImageLoader</code>s
 * and which also supplies convenience loading methods for application use.
 */
package com.sun.javafx.iio;
