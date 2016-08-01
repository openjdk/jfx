/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism;

/**
 * A tagging interface to be implemented by any Graphics that
 * supports printing.
 * <p>
 * This maybe useful to know that you are printing but its initial purpose
 * is that it should be used to decide whether to do things like caching.
 * <p>
 * Existing code that does caching of shapes, textures
 * or other resources that assumes there is only ever a single
 * destination, and so cache an object that can only be used with
 * a specific graphics pipeline.
 * <p>
 * So if a Graphics is tagged with this interface, do not do cache
 * lookup, nor store in a cache.
 */
public interface PrinterGraphics {
}
