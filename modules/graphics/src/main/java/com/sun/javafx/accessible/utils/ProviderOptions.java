/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.accessible.utils;

/**
 *
 */
public enum ProviderOptions {
    // Summary:
    //     The UI Automation provider is a client-side provider.
    CLIENT_SIDE_PROVIDER,
    //
    // Summary:
    //     The UI Automation provider is a server-side provider.
    SERVER_SIDE_PROVIDER,
    //
    // Summary:
    //     The UI Automation provider is a non-client-area provider.
    NON_CLIENT_AREA_PROVIDER,
    //
    // Summary:
    //     The UI Automation provider overrides another provider.
    OVERRIDE_PROVIDER,
    //
    // Summary:
    //     The UI Automation provider handles its own focus, and does not want UI Automation
    //     to set focus to the nearest window on its behalf when System.Windows.Automation.AutomationElement.SetFocus
    //     is called. This option is typically used by providers for windows that appear
    //     to take focus without actually receiving Win32 focus, such as menus and drop-down
    //     menus.
    PROVIDER_OWNS_SETFOUCS,
  
}
