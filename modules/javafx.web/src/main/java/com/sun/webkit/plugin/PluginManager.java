/*
 * Copyright (c) 2011, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.plugin;

import com.sun.javafx.logging.PlatformLogger;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.TreeMap;
import java.util.Vector;

public final class PluginManager {
    private final static PlatformLogger log =
            PlatformLogger.getLogger("com.sun.browser.plugin.PluginManager");

    private static final ServiceLoader<PluginHandler> pHandlers =
        ServiceLoader.load(PluginHandler.class);

    private static final TreeMap<String,PluginHandler> hndMap =
        new TreeMap<String,PluginHandler>();

    private static PluginHandler[] hndArray;

    private static final HashSet<String> disabledPluginHandlers =
        new HashSet<String>();


    private static void updatePluginHandlers() {
        log.fine("Update plugin handlers");

        hndMap.clear();

        Iterator<PluginHandler> iter = pHandlers.iterator();
        while(iter.hasNext()) {
            PluginHandler hnd = iter.next();
            if (hnd.isSupportedPlatform() && !isDisabledPlugin(hnd))
            {
                String [] types = hnd.supportedMIMETypes();
                for (String type : types) {
                    hndMap.put(type, hnd);
                    log.fine(type);
                }
            }
        }
        Collection<PluginHandler> vals = hndMap.values();
        hndArray = vals.toArray(new PluginHandler[vals.size()]);
    }

    static {
        if ("false".equalsIgnoreCase(
                System.getProperty("com.sun.browser.plugin")))
        {
            for(PluginHandler hnd : getAvailablePlugins()) {
                disabledPluginHandlers.add(hnd.getClass().getCanonicalName());
            }
        }

        updatePluginHandlers();
    }

    public static Plugin createPlugin(URL url, String type, String[] pNames,
                                        String[] pValues)
    {
        try {
            PluginHandler hnd =  hndMap.get(type);
            if (hnd == null) {
                return new DefaultPlugin(url, type, pNames, pValues);
            } else {
                Plugin p = hnd.createPlugin(url, type, pNames, pValues);
                if (p == null) {
                    return new DefaultPlugin(url, type, pNames, pValues);
                } else {
                    return p;
                }
            }
        } catch (Throwable ex) {
            log.fine("Cannot create plugin" , ex);
            return new DefaultPlugin(url, type, pNames, pValues);
        }
    }


    private static List<PluginHandler> getAvailablePlugins() {
        Vector<PluginHandler> res = new Vector<PluginHandler>();
        Iterator<PluginHandler> iter = pHandlers.iterator();
        while(iter.hasNext()) {
            PluginHandler hnd = iter.next();
            if (hnd.isSupportedPlatform()) {
                res.add(hnd);
            }
        }
        return res;
    }

    private static PluginHandler getEnabledPlugin(int i) {
        if (i < 0 || i >= hndArray.length) return null;
        return hndArray[i];
    }

    private static int getEnabledPluginCount() {
        return hndArray.length;
    }

    private static void disablePlugin(PluginHandler hnd) {
        disabledPluginHandlers.add(hnd.getClass().getCanonicalName());
        updatePluginHandlers();
    }

    private static void enablePlugin(PluginHandler hnd) {
        disabledPluginHandlers.remove(hnd.getClass().getCanonicalName());
        updatePluginHandlers();
    }

    private static boolean isDisabledPlugin(PluginHandler hnd) {
        return disabledPluginHandlers.contains(
            hnd.getClass().getCanonicalName());
    }

    private static boolean supportsMIMEType(String mimeType) {
        return hndMap.containsKey(mimeType);
    }

    private static String getPluginNameForMIMEType(String mimeType) {
        PluginHandler hnd = hndMap.get(mimeType);
        if (hnd != null) return hnd.getName();
        return "";
    }
}
