/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * Defines the Java packager tool, javapackager.
 *
 * <p>The javapackager is a tool for generating bundles for self-contained applications.
 * It can be located under the name {@code "javapackager"} using the {@link ToolProvider}, for example:
 * <pre>{@code
 * ToolProvider javaPackager = ToolProvider.findFirst("javapackager").orElseThrow(...);
 * javaPackager.run(...);
 * }</pre>
 *
 * @moduleGraph
 * @since 9
 */
@SuppressWarnings("removal")
module jdk.packager {
    requires jdk.jlink;

    requires java.xml;
    requires java.desktop;
    requires java.logging;

    exports com.oracle.tools.packager;
    exports com.sun.javafx.tools.packager;
    exports com.sun.javafx.tools.packager.bundlers;
    exports com.sun.javafx.tools.resource;

    uses com.oracle.tools.packager.Bundler;
    uses com.oracle.tools.packager.Bundlers;

    provides com.oracle.tools.packager.Bundlers with
        com.oracle.tools.packager.BasicBundlers;

    provides com.oracle.tools.packager.Bundler with
        com.oracle.tools.packager.jnlp.JNLPBundler,
        com.oracle.tools.packager.linux.LinuxAppBundler,
        com.oracle.tools.packager.linux.LinuxDebBundler,
        com.oracle.tools.packager.linux.LinuxRpmBundler,
        com.oracle.tools.packager.mac.MacAppBundler,
        com.oracle.tools.packager.mac.MacAppStoreBundler,
        com.oracle.tools.packager.mac.MacDmgBundler,
        com.oracle.tools.packager.mac.MacPkgBundler,
        com.oracle.tools.packager.windows.WinAppBundler,
        com.oracle.tools.packager.windows.WinExeBundler,
        com.oracle.tools.packager.windows.WinMsiBundler;

    provides java.util.spi.ToolProvider
        with jdk.packager.internal.JavaPackagerToolProvider;
}
