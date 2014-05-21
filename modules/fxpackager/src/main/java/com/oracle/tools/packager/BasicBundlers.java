/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.tools.packager;

import com.oracle.tools.packager.linux.LinuxRpmBundler;
import com.oracle.tools.packager.mac.MacAppStoreBundler;
import com.oracle.tools.packager.mac.MacDmgBundler;
import com.oracle.tools.packager.mac.MacPkgBundler;
import com.oracle.tools.packager.linux.LinuxAppBundler;
import com.oracle.tools.packager.linux.LinuxDebBundler;
import com.oracle.tools.packager.mac.MacAppBundler;
import com.oracle.tools.packager.windows.WinAppBundler;
import com.oracle.tools.packager.windows.WinExeBundler;
import com.oracle.tools.packager.windows.WinMsiBundler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A basic bundlers collection that loads the OpenJFX default bundlers.
 * Loads the bundlers common to OpenJFX.
 * <UL>
 *     <LI>Windows file image</LI>
 *     <LI>Mac .app</LI>
 *     <LI>Linux file image</LI>
 *     <LI>Windows MSI</LI>
 *     <LI>Windows EXE</LI>
 *     <LI>Mac DMG</LI>
 *     <LI>Mac PKG</LI>
 *     <LI>Linux DEB</LI>
 *     <LI>Linux RPM</LI>
 *
 * </UL>
 */
public class BasicBundlers implements Bundlers {

    boolean defaultsLoaded = false;

    private Collection<Bundler> bundlers = new CopyOnWriteArrayList<>();

    public Collection<Bundler> getBundlers() {
        return Collections.unmodifiableCollection(bundlers);
    }

    public Collection<Bundler> getBundlers(String type) {
        if (type == null) return Collections.emptySet();
        switch (type) {
            case "NONE":
                return Collections.emptySet();
            case "ALL":
                return getBundlers();
            default:
                return Arrays.asList(getBundlers().stream()
                        .filter(b -> type.equals(b.getBundleType()))
                        .toArray(Bundler[]::new));
        }
    }

    /**
     * A list of the "standard" parameters that bundlers should support
     * or fall back to when their specific parameters are not used.
     * @return an unmodifieable collection of the standard parameters.
     */
    public Collection<BundlerParamInfo> getStandardParameters() {
        //TODO enumerate the stuff in BundleParams
        return null;
    }

    /**
     * Loads the bundlers common to OpenJFX.
     * <UL>
     *     <LI>Windows file image</LI>
     *     <LI>Mac .app</LI>
     *     <LI>Linux file image</LI>
     *     <LI>Windows MSI</LI>
     *     <LI>Windows EXE</LI>
     *     <LI>Mac DMG</LI>
     *     <LI>Mac PKG</LI>
     *     <LI>Linux DEB</LI>
     *     <LI>Linux RPM</LI>
     *
     * </UL>
     */
    public void loadDefaultBundlers() {
        if (defaultsLoaded) return;

        bundlers.add(new WinAppBundler());
        bundlers.add(new WinExeBundler());
        bundlers.add(new WinMsiBundler());

        bundlers.add(new LinuxAppBundler());
        bundlers.add(new LinuxDebBundler());
        bundlers.add(new LinuxRpmBundler());

        bundlers.add(new MacAppBundler());
        bundlers.add(new MacDmgBundler());
        bundlers.add(new MacPkgBundler());
        bundlers.add(new MacAppStoreBundler());

        //bundlers.add(new JNLPBundler());

        defaultsLoaded = true;
    }

    /**
     * Loads bundlers from the META-INF/services direct
     */
    public void loadBundlersFromServices(ClassLoader cl) {
        ServiceLoader<Bundler> loader = ServiceLoader.load(Bundler.class, cl);
        for (Bundler aLoader : loader) {
            bundlers.add(aLoader);
        }
    }

    public void loadBundler(Bundler bundler) {
        bundlers.add(bundler);
    }
}
