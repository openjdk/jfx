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

package com.oracle.bundlers;

import com.oracle.bundlers.mac.MacAppStoreBundler;
import com.oracle.bundlers.mac.MacPKGBundler;
import com.sun.javafx.tools.packager.bundlers.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Bundlers {
    
    public static Bundlers createBundlersInstance() {
        return createBundlersInstance(Bundlers.class.getClassLoader());
    }
    
    public static Bundlers createBundlersInstance(ClassLoader servicesClassLoader) {
        Bundlers bundlers = new Bundlers();
        bundlers.loadDefaultBundlers();
        bundlers.loadBundlersFromServices(servicesClassLoader);
        return bundlers;
    }

    private Collection<Bundler> bundlers = new CopyOnWriteArrayList<>();

    public Collection<Bundler> getBundlers() {
        return Collections.unmodifiableCollection(bundlers);
    }

    public Collection<Bundler> getBundlers(BundleType type) {
        if (type == null) return Collections.emptySet();
        switch (type) {
            case NONE:
                return Collections.emptySet();
            case ALL:
                return getBundlers();
            default:
                Collection<Bundler> results = new LinkedHashSet<>();
                for (Bundler bundler : getBundlers()) {
                    if (type.equals(bundler.getBundleType())) {
                        results.add(bundler);
                    }
                }
                return results;
                //return Arrays.asList(
                //    getBundlers().stream()
                //        .filter(b -> type.equals(b.getBundleType()))
                //        .toArray(Bundler[]::new));
        }
    }

    /**
     * A list of the "standard" parameters that bundlers should support
     * or fall back to when their specific parameters are not used.
     * @return an unmodifieable collection of the standard parameters.
     */
    public static Collection<BundlerParamInfo> getStandardParameters() {
        //TODO enumerate the stuff in BundleParams
        return null;
    }

    /**
     * Loads the bundlers common to the JDK.
     * <UL>
     *     <LI>Windows file tree</LI>
     *     <LI>Mac .app</LI>
     *     <LI>Linux file tree</LI>

     *     <LI>Windows MSI</LI>
     *     <LI>Windows EXE</LI>
     *     <LI>Mac DMG</LI>
     *     <LI>Linux DEB</LI>
     *     <LI>Linux RPM</LI>
     *
     * </UL>
     */
    public void loadDefaultBundlers() {
        bundlers.add(new WinAppBundler());
        bundlers.add(new WinExeBundler());
        bundlers.add(new WinMsiBundler());

        bundlers.add(new LinuxAppBundler());
        bundlers.add(new LinuxDebBundler());
        bundlers.add(new LinuxRPMBundler());

        bundlers.add(new MacAppBundler());
        bundlers.add(new MacDMGBundler());
        bundlers.add(new MacPKGBundler());
        bundlers.add(new MacAppStoreBundler());

        //bundlers.add(new JNLPBundler());
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
