/*
 * Copyright (c) 2023, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "GlassApplication.h"
#include <tuple>

using namespace Microsoft::WRL;
using namespace ABI::Windows::Foundation;
using namespace ABI::Windows::UI;
using namespace ABI::Windows::UI::ViewManagement;
using namespace ABI::Windows::Networking::Connectivity;

/*
 * ---- the flat C ABI half of this file (glass_win_api.h) ----
 *
 * The preferences callback table. Written once by gwin_prefs_set_callbacks, from Java, on the
 * launcher thread before the Glass toolkit thread exists; read by updatePreferences, which runs on
 * the toolkit thread and on whatever thread WinRT dispatches the two sinks on. No lock, by design -
 * see gwin_prefs_set_callbacks in glass_win_api.h for why that is safe and what breaks if the
 * installation is made lazy instead.
 */
static GwinPrefsCallbacks g_prefsCallbacks = { NULL };
static void* g_prefsUser = NULL;

/*
 * updatePreferences hands its PreferenceType straight to preferences_changed as `types`, so the two
 * enums have to be the same numbers. This is the only place both are visible; glass_win_api.cpp
 * cannot see PlatformSupport::PreferenceType and PlatformSupport.h does not spell the ABI values.
 */
static_assert((int)PlatformSupport::PT_SYSTEM_COLORS == GWIN_PT_SYSTEM_COLORS
        && (int)PlatformSupport::PT_SYSTEM_PARAMS == GWIN_PT_SYSTEM_PARAMS
        && (int)PlatformSupport::PT_UI_SETTINGS == GWIN_PT_UI_SETTINGS
        && (int)PlatformSupport::PT_NETWORK_INFORMATION == GWIN_PT_NETWORK_INFORMATION
        && (int)PlatformSupport::PT_ALL == GWIN_PT_ALL,
        "GwinPreferenceType must mirror PlatformSupport::PreferenceType");

/*
 * gwin_app_create's instance: WinRT activation only. The JNI constructor, which also looked up
 * the Java classes the preferences map was built from, is gone.
 */
PlatformSupport::PlatformSupport()
    : networkInformation(NULL)
{
    InitializeWinRT();
}

void PlatformSupport::InitializeWinRT()
{
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

bool PlatformSupport::updatePreferences(PreferenceType preferenceType) const
{
    /*
     * Java owns the whole collect / compare / notify cycle: WinPreferences reads GetSysColor and
     * SystemParametersInfoW itself and calls gwin_prefs_query_ui_settings / gwin_prefs_query_network
     * for the WinRT half, so no java/util/Map is ever built in C (the JNI arm that built one is
     * gone). With no table installed nothing is delivered and the answer is false. The callback
     * returns 1 when the preferences changed; see GwinPrefsCallbacks in glass_win_api.h for the exact
     * mapping - notably a notify that throws still counts as changed.
     */
    return g_prefsCallbacks.preferences_changed != NULL
        && g_prefsCallbacks.preferences_changed(g_prefsUser, (int32_t)preferenceType) != 0;
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

/*
 * The former queryUISettings without the JNI: same three try blocks, same order, same three
 * ignored HRESULTs. Each block's "valid" flag stands for the map keys that block used to put, so a 0 flag
 * means those keys are absent - the C dropped every later key too once one block threw, and returning early here
 * reproduces that. The one deliberate deviation is that `out` and the locals feeding it start zeroed,
 * where the C read uninitialised stack when an ignored HRESULT failed.
 */
void PlatformSupport::collectUISettings(GwinUiSettings& out) const
{
    ::memset(&out, 0, sizeof(GwinUiSettings));

    if (!this->settings) {
        return;
    }

    out.available = 1;

    try {
        ComPtr<IUISettings3> settings3;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings3>",
                   this->settings->QueryInterface<IUISettings3>(&settings3));

        // The order of the former queryUISettings, and the order WinPreferences names the keys in.
        static const UIColorType colorTypes[GWIN_UI_COLOR_COUNT] = {
            UIColorType::UIColorType_Background,
            UIColorType::UIColorType_Foreground,
            UIColorType::UIColorType_AccentDark3,
            UIColorType::UIColorType_AccentDark2,
            UIColorType::UIColorType_AccentDark1,
            UIColorType::UIColorType_Accent,
            UIColorType::UIColorType_AccentLight1,
            UIColorType::UIColorType_AccentLight2,
            UIColorType::UIColorType_AccentLight3
        };

        for (int i = 0; i < GWIN_UI_COLOR_COUNT; ++i) {
            Color color = {};
            // The HRESULT is ignored, as it was in queryUISettings; `color` is zeroed, so a failure
            // yields transparent black rather than the stack garbage the C would have published.
            settings3->GetColorValue(colorTypes[i], &color);
            out.colors[i] = ((uint32_t)color.A << 24) | ((uint32_t)color.R << 16)
                          | ((uint32_t)color.G << 8) | (uint32_t)color.B;
        }

        out.colors_valid = 1;
    } catch (RoException const&) {
        return;
    }

    try {
        ComPtr<IUISettings4> settings4;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings4>",
                   this->settings->QueryInterface<IUISettings4>(&settings4));

        unsigned char value = 0;
        settings4->get_AdvancedEffectsEnabled(&value);   // HRESULT ignored, as in the former queryUISettings
        out.advanced_effects_enabled = value != 0;       // the former putBoolean(const bool): nonzero is true
        out.advanced_effects_valid = 1;
    } catch (RoException const&) {
        return;
    }

    try {
        ComPtr<IUISettings5> settings5;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings5>",
                   this->settings->QueryInterface<IUISettings5>(&settings5));

        unsigned char value = 0;
        settings5->get_AutoHideScrollBars(&value);       // HRESULT ignored, as in the former queryUISettings
        out.auto_hide_scroll_bars = value != 0;          // the former putBoolean(const bool): nonzero is true
        out.auto_hide_valid = 1;
    } catch (RoException const&) {
        return;
    }
}

/*
 * The former queryNetworkInformation without the JNI. available is set exactly where the C
 * reached its putString, so available == 0 means "no key" and available == 1 with GWIN_NET_COST_UNKNOWN
 * means the key was written with the value "Unknown" - either there is no internet connection profile, or the profile
 * reported a cost the switch does not name. The GwinNetworkCost values are this ABI's, not WinRT's.
 */
void PlatformSupport::collectNetworkInfo(GwinNetworkInfo& out) const
{
    ::memset(&out, 0, sizeof(GwinNetworkInfo));

    if (!this->networkInformation) {
        return;
    }

    try {
        ComPtr<IConnectionProfile> connectionProfile;
        ComPtr<IConnectionCost> connectionCost;
        NetworkCostType networkCostType = NetworkCostType_Unknown;
        int32_t costType = GWIN_NET_COST_UNKNOWN;

        RO_CHECKED("INetworkInformation::GetInternetConnectionProfile",
                   this->networkInformation->GetInternetConnectionProfile(&connectionProfile));

        if (connectionProfile) {
            RO_CHECKED("IConnectionProfile::GetConnectionCost",
                       connectionProfile->GetConnectionCost(&connectionCost));

            RO_CHECKED("IConnectionCost::get_NetworkCostType",
                       connectionCost->get_NetworkCostType(&networkCostType));

            switch (networkCostType) {
                case NetworkCostType_Unrestricted: costType = GWIN_NET_COST_UNRESTRICTED; break;
                case NetworkCostType_Variable: costType = GWIN_NET_COST_VARIABLE; break;
                case NetworkCostType_Fixed: costType = GWIN_NET_COST_FIXED; break;
                default: break;   // the C left internetCostType NULL and put "Unknown"
            }
        }

        out.available = 1;
        out.cost_type = costType;
    } catch (RoException const&) {
    }
}

/* ---- glass_win_api.h exports. Definitions take C linkage from that header's extern "C" block. ---- */

extern "C" {

int32_t gwin_prefs_set_callbacks(const GwinPrefsCallbacks* cb, void* user)
{
    if (cb == NULL) {
        g_prefsCallbacks.preferences_changed = NULL;
        g_prefsUser = NULL;
    } else {
        g_prefsCallbacks = *cb;   // by value: this library never retains the caller's struct
        g_prefsUser = user;
    }
    return GWIN_OK;
}

int32_t gwin_prefs_query_ui_settings(GwinUiSettings* out)
{
    if (out == NULL) {
        return GWIN_ERR_INVALID_ARG;
    }

    PlatformSupport* support = GlassApplication::GetPlatformSupport();
    if (support == NULL) {
        // No toolkit window, so nothing was ever activated. Same "no keys" as collectUISettings.
        ::memset(out, 0, sizeof(GwinUiSettings));
        return GWIN_OK;
    }

    support->collectUISettings(*out);
    return GWIN_OK;
}

int32_t gwin_prefs_query_network(GwinNetworkInfo* out)
{
    if (out == NULL) {
        return GWIN_ERR_INVALID_ARG;
    }

    PlatformSupport* support = GlassApplication::GetPlatformSupport();
    if (support == NULL) {
        ::memset(out, 0, sizeof(GwinNetworkInfo));
        return GWIN_OK;
    }

    support->collectNetworkInfo(*out);
    return GWIN_OK;
}

} // extern "C"
