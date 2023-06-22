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
 * <p>This package provides means for loading and displaying Web content. Its
 *     functionality is implemented by two core classes:
 *
 * <p>{@link javafx.scene.web.WebEngine} is a non-visual component capable of
 *     loading Web pages, creating DOM objects for them, and running scripts
 *     inside pages.
 *
 * <p>{@link javafx.scene.web.WebView} is a {@link javafx.scene.Node} that
 *     presents a Web page managed by a {@code WebEngine}. Each {@code WebView}
 *     has a {@code WebEngine} associated with it. This association is
 *     established at the time {@code WebView} is instantiated, and cannot be
 *     changed later.
 *
 * <p>Both {@code WebEngine} and {@code WebView} should be created and
 *     manipulated on FX User thread.
 *
 * <p>The code snippet below shows a typical usage scenario:
 *
 * <pre>{@code
 *     WebView webView = new WebView();
 *     WebEngine webEngine = webView.getEngine();
 *     webEngine.load("http://javafx.com");
 *     // add webView to the scene
 * }</pre>
 */
package javafx.scene.web;
