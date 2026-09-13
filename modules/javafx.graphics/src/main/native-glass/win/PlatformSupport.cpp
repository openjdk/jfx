/*
 * Copyright (c) 2023, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include "PlatformSupport.h"
#include "RoActivationSupport.h"
#include <tuple>

using namespace Microsoft::WRL;
using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::UI;
using namespace ABI::Windows::UI::ViewManagement;
using namespace ABI::Windows::Networking::Connectivity;

PlatformSupport::PlatformSupport(JNIEnv* env, jobject application)
    : env(env), application(application), initialized(false), networkInformation(NULL), preferences(NULL)
{
    javaClasses.Object = (jclass)env->FindClass("java/lang/Object");
    if (CheckAndClearException(env)) return;

    javaIDs.Object.equals = env->GetMethodID(javaClasses.Object, "equals", "(Ljava/lang/Object;)Z");
    if (CheckAndClearException(env)) return;

    javaClasses.Collections = (jclass)env->FindClass("java/util/Collections");
    if (CheckAndClearException(env)) return;

    javaIDs.Collections.unmodifiableMap = env->GetStaticMethodID(
        javaClasses.Collections, "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;");
    if (CheckAndClearException(env)) return;

    javaClasses.Map = (jclass)env->FindClass("java/util/Map");
    if (CheckAndClearException(env)) return;

    javaIDs.Map.put = env->GetMethodID(
        javaClasses.Map, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if (CheckAndClearException(env)) return;

    javaClasses.HashMap = (jclass)env->FindClass("java/util/HashMap");
    if (CheckAndClearException(env)) return;

    javaIDs.HashMap.init = env->GetMethodID(javaClasses.HashMap, "<init>", "()V");
    if (CheckAndClearException(env)) return;

    javaClasses.Color = (jclass)env->FindClass("javafx/scene/paint/Color");
    if (CheckAndClearException(env)) return;

    javaIDs.Color.rgb = env->GetStaticMethodID(javaClasses.Color, "rgb", "(IIID)Ljavafx/scene/paint/Color;");
    if (CheckAndClearException(env)) return;

    javaClasses.Boolean = (jclass)env->FindClass("java/lang/Boolean");
    if (CheckAndClearException(env)) return;

    javaIDs.Boolean.trueID = env->GetStaticFieldID(javaClasses.Boolean, "TRUE", "Ljava/lang/Boolean;");
    if (CheckAndClearException(env)) return;

    javaIDs.Boolean.falseID = env->GetStaticFieldID(javaClasses.Boolean, "FALSE", "Ljava/lang/Boolean;");
    if (CheckAndClearException(env)) return;

    // Mandatory fields are now initialized, after this point we will initialize optional APIs.
    initialized = true;

    tryInitializeRoActivationSupport();

    if (!isRoActivationSupported()) {
        return;
    }

    try {
        RO_CHECKED("RoActivateInstance",
                   RoActivateInstance(hstring("Windows.UI.ViewManagement.UISettings"), (IInspectable**)&settings));

        ComPtr<IUISettings5> settings5;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings5>",
                   settings->QueryInterface<IUISettings5>(&settings5));

        EventRegistrationToken token;
        settings5->add_AutoHideScrollBarsChanged(
            Callback<ITypedEventHandler<UISettings*, UISettingsAutoHideScrollBarsChangedEventArgs*>>(
                [this](IUISettings*, IUISettingsAutoHideScrollBarsChangedEventArgs*) {
                    updatePreferences(PT_UI_SETTINGS);
                    return S_OK;
                }).Get(),
            &token);
    } catch (RoException const&) {
        // If an activation exception occurs, it probably means that we're on a Windows system
        // that doesn't support the UISettings API. This is not a problem, it simply means that
        // we don't report the UISettings properties back to the JavaFX application.
    }

    try {
        IActivationFactory* activationFactory = NULL;

        RO_CHECKED("RoGetActivationFactory",
                   RoGetActivationFactory(hstring("Windows.Networking.Connectivity.NetworkInformation"),
                                          IID_IActivationFactory,
                                          (void**)&activationFactory));

        RO_CHECKED("IActivationFactory::QueryInterface<INetworkInformationStatics>",
                   activationFactory->QueryInterface(&networkInformation));

        EventRegistrationToken token;
        networkInformation->add_NetworkStatusChanged(
            Callback<INetworkStatusChangedEventHandler>(
                [this](IInspectable*) {
                    updatePreferences(PT_NETWORK_INFORMATION);
                    return S_OK;
                }).Get(),
            &token);
    } catch (RoException const&) {
        // If an activation exception occurs, it probably means that we're on a Windows system
        // that doesn't support the NetworkInformation API.
    }
}

PlatformSupport::~PlatformSupport()
{
    settings = nullptr;
    uninitializeRoActivationSupport();
}

jobject PlatformSupport::collectPreferences(PreferenceType preferenceType) const
{
    if (!initialized) {
        return NULL;
    }

    jobject prefs = env->NewObject(javaClasses.HashMap, javaIDs.HashMap.init);
    if (CheckAndClearException(env)) return NULL;

    if (preferenceType & PT_SYSTEM_COLORS) {
        querySystemColors(prefs);
    }

    if (preferenceType & PT_SYSTEM_PARAMS) {
        querySystemParameters(prefs);
    }

    if (preferenceType & PT_UI_SETTINGS) {
        queryUISettings(prefs);
    }

    if (preferenceType & PT_NETWORK_INFORMATION) {
        queryNetworkInformation(prefs);
    }

    return prefs;
}

bool PlatformSupport::updatePreferences(PreferenceType preferenceType) const
{
    if (!initialized) {
        return false;
    }

    jobject newPreferences = collectPreferences(preferenceType);

    jboolean preferencesChanged =
        newPreferences != NULL &&
        !env->CallBooleanMethod(newPreferences, javaIDs.Object.equals, preferences);

    if (!CheckAndClearException(env) && preferencesChanged) {
        preferences = newPreferences;
        jobject unmodifiablePreferences = env->CallStaticObjectMethod(
            javaClasses.Collections, javaIDs.Collections.unmodifiableMap, newPreferences);

        if (!CheckAndClearException(env)) {
            env->CallVoidMethod(application, javaIDs.Application.notifyPreferencesChangedMID, unmodifiablePreferences);
            env->DeleteLocalRef(unmodifiablePreferences);
            env->DeleteLocalRef(newPreferences);
            CheckAndClearException(env);
            return true;
        }
    }

    env->DeleteLocalRef(newPreferences);
    CheckAndClearException(env);
    return false;
}

bool PlatformSupport::onSettingChanged(WPARAM wParam, LPARAM lParam) const
{
    switch ((UINT)wParam) {
        case SPI_SETHIGHCONTRAST:
            return updatePreferences(PreferenceType(PT_SYSTEM_PARAMS | PT_UI_SETTINGS));

        case SPI_SETCLIENTAREAANIMATION:
            return updatePreferences(PT_SYSTEM_PARAMS);
    }

    if (lParam != NULL && wcscmp(LPCWSTR(lParam), L"ImmersiveColorSet") == 0) {
        return updatePreferences(PT_UI_SETTINGS);
    }

    return false;
}

void PlatformSupport::querySystemParameters(jobject properties) const
{
    HIGHCONTRAST contrastInfo;
    contrastInfo.cbSize = sizeof(HIGHCONTRAST);
    if (::SystemParametersInfo(SPI_GETHIGHCONTRAST, sizeof(HIGHCONTRAST), &contrastInfo, 0)) {
        // Property names need to be kept in sync with WinApplication.java:
        if (contrastInfo.dwFlags & HCF_HIGHCONTRASTON) {
            putBoolean(properties, "Windows.SPI.HighContrast", true);
            putString(properties, "Windows.SPI.HighContrastColorScheme", contrastInfo.lpszDefaultScheme);
        } else {
            putBoolean(properties, "Windows.SPI.HighContrast", false);
            putString(properties, "Windows.SPI.HighContrastColorScheme", (const char*)NULL);
        }
    }

    BOOL value;
    if (::SystemParametersInfo(SPI_GETCLIENTAREAANIMATION, 0, &value, 0)) {
        putBoolean(properties, "Windows.SPI.ClientAreaAnimation", value);
    }
}

void PlatformSupport::querySystemColors(jobject properties) const
{
    // Property names need to be kept in sync with WinApplication.java:
    putColor(properties, "Windows.SysColor.COLOR_3DFACE", GetSysColor(COLOR_3DFACE));
    putColor(properties, "Windows.SysColor.COLOR_BTNTEXT", GetSysColor(COLOR_BTNTEXT));
    putColor(properties, "Windows.SysColor.COLOR_GRAYTEXT", GetSysColor(COLOR_GRAYTEXT));
    putColor(properties, "Windows.SysColor.COLOR_HIGHLIGHT", GetSysColor(COLOR_HIGHLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_HIGHLIGHTTEXT", GetSysColor(COLOR_HIGHLIGHTTEXT));
    putColor(properties, "Windows.SysColor.COLOR_HOTLIGHT", GetSysColor(COLOR_HOTLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_WINDOW", GetSysColor(COLOR_WINDOW));
    putColor(properties, "Windows.SysColor.COLOR_WINDOWTEXT", GetSysColor(COLOR_WINDOWTEXT));
}

void PlatformSupport::queryUISettings(jobject properties) const
{
    if (!this->settings) {
        return;
    }

    try {
        ComPtr<IUISettings3> settings3;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings3>",
                   this->settings->QueryInterface<IUISettings3>(&settings3));

        Color background, foreground, accentDark3, accentDark2, accentDark1, accent,
              accentLight1, accentLight2, accentLight3;

        settings3->GetColorValue(UIColorType::UIColorType_Background, &background);
        settings3->GetColorValue(UIColorType::UIColorType_Foreground, &foreground);
        settings3->GetColorValue(UIColorType::UIColorType_AccentDark3, &accentDark3);
        settings3->GetColorValue(UIColorType::UIColorType_AccentDark2, &accentDark2);
        settings3->GetColorValue(UIColorType::UIColorType_AccentDark1, &accentDark1);
        settings3->GetColorValue(UIColorType::UIColorType_Accent, &accent);
        settings3->GetColorValue(UIColorType::UIColorType_AccentLight1, &accentLight1);
        settings3->GetColorValue(UIColorType::UIColorType_AccentLight2, &accentLight2);
        settings3->GetColorValue(UIColorType::UIColorType_AccentLight3, &accentLight3);

        // Property names need to be kept in sync with WinApplication.java:
        putColor(properties, "Windows.UIColor.Background", background);
        putColor(properties, "Windows.UIColor.Foreground", foreground);
        putColor(properties, "Windows.UIColor.AccentDark3", accentDark3);
        putColor(properties, "Windows.UIColor.AccentDark2", accentDark2);
        putColor(properties, "Windows.UIColor.AccentDark1", accentDark1);
        putColor(properties, "Windows.UIColor.Accent", accent);
        putColor(properties, "Windows.UIColor.AccentLight1", accentLight1);
        putColor(properties, "Windows.UIColor.AccentLight2", accentLight2);
        putColor(properties, "Windows.UIColor.AccentLight3", accentLight3);
    } catch (RoException const&) {
        return;
    }

    try {
        ComPtr<IUISettings4> settings4;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings4>",
                   this->settings->QueryInterface<IUISettings4>(&settings4));

        unsigned char value;
        settings4->get_AdvancedEffectsEnabled(&value);
        putBoolean(properties, "Windows.UISettings.AdvancedEffectsEnabled", value);
    } catch (RoException const&) {
        return;
    }

    try {
        ComPtr<IUISettings5> settings5;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings5>",
                   this->settings->QueryInterface<IUISettings5>(&settings5));

        unsigned char value;
        settings5->get_AutoHideScrollBars(&value);
        putBoolean(properties, "Windows.UISettings.AutoHideScrollBars", value);
    } catch (RoException const&) {
        return;
    }
}

void PlatformSupport::queryNetworkInformation(jobject properties) const
{
    if (!this->networkInformation) {
        return;
    }

    try {
        ComPtr<IConnectionProfile> connectionProfile;
        ComPtr<IConnectionCost> connectionCost;
        NetworkCostType networkCostType;
        const char* internetCostType = NULL;

        RO_CHECKED("INetworkInformation::GetInternetConnectionProfile",
                   this->networkInformation->GetInternetConnectionProfile(&connectionProfile));

        if (connectionProfile) {
            RO_CHECKED("IConnectionProfile::GetConnectionCost",
                       connectionProfile->GetConnectionCost(&connectionCost));

            RO_CHECKED("IConnectionCost::get_NetworkCostType",
                       connectionCost->get_NetworkCostType(&networkCostType));

            switch (networkCostType) {
                case NetworkCostType_Unrestricted: internetCostType = "Unrestricted"; break;
                case NetworkCostType_Variable: internetCostType = "Variable"; break;
                case NetworkCostType_Fixed: internetCostType = "Fixed"; break;
            }
        }

        putString(properties, "Windows.NetworkInformation.InternetCostType",
                  internetCostType != NULL ? internetCostType : "Unknown");
    } catch (RoException const&) {
    }
}

void PlatformSupport::putString(jobject properties, const char* key, const char* value) const
{
    jobject prefKey = env->NewStringUTF(key);
    if (CheckAndClearException(env)) return;

    jobject prefValue = NULL;
    if (value != NULL) {
        prefValue = env->NewStringUTF(value);
        if (CheckAndClearException(env)) return;
    }

    env->CallObjectMethod(properties, javaIDs.Map.put, prefKey, prefValue);
    CheckAndClearException(env);
}

void PlatformSupport::putString(jobject properties, const char* key, const wchar_t* value) const
{
    jobject prefKey = env->NewStringUTF(key);
    if (CheckAndClearException(env)) return;

    jobject prefValue = NULL;
    if (value != NULL) {
        prefValue = env->NewString((jchar*)value, (jsize)wcslen(value));
        if (CheckAndClearException(env)) return;
    }

    env->CallObjectMethod(properties, javaIDs.Map.put, prefKey, prefValue);
    CheckAndClearException(env);
}

void PlatformSupport::putBoolean(jobject properties, const char* key, const bool value) const
{
    jobject prefKey = env->NewStringUTF(key);
    if (CheckAndClearException(env)) return;

    jobject prefValue = value ?
        env->GetStaticObjectField(javaClasses.Boolean, javaIDs.Boolean.trueID) :
        env->GetStaticObjectField(javaClasses.Boolean, javaIDs.Boolean.falseID);
    if (CheckAndClearException(env)) return;

    env->CallObjectMethod(properties, javaIDs.Map.put, prefKey, prefValue);
    CheckAndClearException(env);
}

void PlatformSupport::putColor(jobject properties, const char* colorName, int colorValue) const
{
    jobject prefKey = env->NewStringUTF(colorName);
    if (CheckAndClearException(env)) return;

    jobject prefValue = env->CallStaticObjectMethod(
        javaClasses.Color, javaIDs.Color.rgb,
        GetRValue(colorValue), GetGValue(colorValue), GetBValue(colorValue), 1.0);
    if (CheckAndClearException(env)) return;

    env->CallObjectMethod(properties, javaIDs.Map.put, prefKey, prefValue);
    CheckAndClearException(env);
}

void PlatformSupport::putColor(jobject properties, const char* colorName, Color colorValue) const
{
    jobject prefKey = env->NewStringUTF(colorName);
    if (CheckAndClearException(env)) return;

    jobject prefValue = env->CallStaticObjectMethod(
        javaClasses.Color, javaIDs.Color.rgb,
        colorValue.R, colorValue.G, colorValue.B, (double)colorValue.A / 255.0);
    if (CheckAndClearException(env)) return;

    env->CallObjectMethod(properties, javaIDs.Map.put, prefKey, prefValue);
    CheckAndClearException(env);
}
