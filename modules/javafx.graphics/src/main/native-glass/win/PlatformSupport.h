/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This flag gives us function prototypes without the __declspec(dllimport) storage class specifier.
 * RoActivationSupport defines symbols locally, and having the dllimport specifier would trigger LNK4217.
 */
#define _ROAPI_

#include <common.h>
#include <wrl.h>
#include <windows.ui.viewmanagement.h>
#include <windows.networking.connectivity.h>

class PlatformSupport final
{
public:
    enum PreferenceType {
        PT_SYSTEM_COLORS = 1,
        PT_SYSTEM_PARAMS = 2,
        PT_UI_SETTINGS = 4,
        PT_NETWORK_INFORMATION = 8,
        PT_ALL = PT_SYSTEM_COLORS | PT_SYSTEM_PARAMS | PT_UI_SETTINGS | PT_NETWORK_INFORMATION
    };

    PlatformSupport(JNIEnv*, jobject application);
    ~PlatformSupport();
    PlatformSupport(PlatformSupport const&) = delete;
    PlatformSupport& operator=(PlatformSupport const&) = delete;

    /**
     * Collect the specified platform preferences and return them as a new java/util/Map.
     */
    jobject collectPreferences(PreferenceType) const;

    /**
     * Collect the specified platform preferences and notify the JavaFX application when a preference has changed.
     * The change notification includes all specified preferences, not only the changed preferences.
     */
    bool updatePreferences(PreferenceType) const;

    /**
     * Handles the WM_SETTINGCHANGE message.
    */
    bool onSettingChanged(WPARAM, LPARAM) const;

private:
    JNIEnv* env;
    jobject application;
    bool initialized;
    Microsoft::WRL::ComPtr<ABI::Windows::UI::ViewManagement::IUISettings> settings;
    ABI::Windows::Networking::Connectivity::INetworkInformationStatics* networkInformation;
    mutable JGlobalRef<jobject> preferences;

    struct {
        JGlobalRef<jclass> Boolean;
        JGlobalRef<jclass> Object;
        JGlobalRef<jclass> Collections;
        JGlobalRef<jclass> Map;
        JGlobalRef<jclass> HashMap;
        JGlobalRef<jclass> Color;
    } javaClasses;

    void querySystemColors(jobject properties) const;
    void querySystemParameters(jobject properties) const;
    void queryUISettings(jobject properties) const;
    void queryNetworkInformation(jobject properties) const;

    void putString(jobject properties, const char* key, const char* value) const;
    void putString(jobject properties, const char* key, const wchar_t* value) const;
    void putBoolean(jobject properties, const char* key, const bool value) const;
    void putColor(jobject properties, const char* colorName, int colorValue) const;
    void putColor(jobject properties, const char* colorName, ABI::Windows::UI::Color colorValue) const;
};
