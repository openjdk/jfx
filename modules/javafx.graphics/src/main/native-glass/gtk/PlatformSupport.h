/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include <jni.h>

class PlatformSupport final
{
public:
    static constexpr const char* observedSettings[] = {
        "notify::gtk-theme-name",
        "notify::gtk-enable-animations"
    };

    PlatformSupport(JNIEnv*, jobject);
    ~PlatformSupport();
    PlatformSupport(PlatformSupport const&) = delete;
    PlatformSupport& operator=(PlatformSupport const&) = delete;

    /**
     * Collect all platform preferences and return them as a new java/util/Map.
     */
    jobject collectPreferences() const;

    /**
     * Collect all platform preferences and notify the JavaFX application when a preference has changed.
     * The change notification includes all preferences, not only the changed preferences.
     */
    void updatePreferences() const;

private:
    JNIEnv* env;
    jobject application;
    mutable jobject preferences;
};
