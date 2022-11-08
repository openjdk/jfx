/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"
#include "ThemeSupport.h"
#include "RoActivationSupport.h"
#include <windows.ui.viewmanagement.h>

using namespace Microsoft::WRL;
using namespace ABI::Windows::UI;
using namespace ABI::Windows::UI::ViewManagement;

ThemeSupport::ThemeSupport(JNIEnv* env) :
    env_(env),
    mapClass_((jclass)env->FindClass("java/util/Map")),
    colorClass_((jclass)env->FindClass("javafx/scene/paint/Color")),
    booleanClass_((jclass)env->FindClass("java/lang/Boolean"))
{
    putMethod_ = env->GetMethodID(mapClass_, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    rgbMethod_ = env->GetStaticMethodID(colorClass_, "rgb", "(IIID)Ljavafx/scene/paint/Color;");
    trueField_ = env->GetStaticFieldID(booleanClass_, "TRUE", "Ljava/lang/Boolean;");
    falseField_ = env->GetStaticFieldID(booleanClass_, "FALSE", "Ljava/lang/Boolean;");
}

ThemeSupport::~ThemeSupport() {
    env_->DeleteLocalRef(mapClass_);
    env_->DeleteLocalRef(colorClass_);
    env_->DeleteLocalRef(booleanClass_);
}

void ThemeSupport::queryHighContrastScheme(jobject properties) const
{
    HIGHCONTRAST contrastInfo;
    contrastInfo.cbSize = sizeof(HIGHCONTRAST);
    ::SystemParametersInfo(SPI_GETHIGHCONTRAST, sizeof(HIGHCONTRAST), &contrastInfo, 0);
    if (contrastInfo.dwFlags & HCF_HIGHCONTRASTON) {
        putBoolean(properties, "Windows.SPI.HighContrastOn", true);
        putString(properties, "Windows.SPI.HighContrastColorScheme", contrastInfo.lpszDefaultScheme);
    } else {
        putBoolean(properties, "Windows.SPI.HighContrastOn", false);
        putString(properties, "Windows.SPI.HighContrastColorScheme", (const char*)NULL);
    }
}

void ThemeSupport::querySystemColors(jobject properties) const
{
    putColor(properties, "Windows.SysColor.COLOR_3DDKSHADOW", GetSysColor(COLOR_3DDKSHADOW));
    putColor(properties, "Windows.SysColor.COLOR_3DFACE", GetSysColor(COLOR_3DFACE));
    putColor(properties, "Windows.SysColor.COLOR_3DHIGHLIGHT", GetSysColor(COLOR_3DHIGHLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_3DHILIGHT", GetSysColor(COLOR_3DHILIGHT));
    putColor(properties, "Windows.SysColor.COLOR_3DLIGHT", GetSysColor(COLOR_3DLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_3DSHADOW", GetSysColor(COLOR_3DSHADOW));
    putColor(properties, "Windows.SysColor.COLOR_ACTIVEBORDER", GetSysColor(COLOR_ACTIVEBORDER));
    putColor(properties, "Windows.SysColor.COLOR_ACTIVECAPTION", GetSysColor(COLOR_ACTIVECAPTION));
    putColor(properties, "Windows.SysColor.COLOR_APPWORKSPACE", GetSysColor(COLOR_APPWORKSPACE));
    putColor(properties, "Windows.SysColor.COLOR_BACKGROUND", GetSysColor(COLOR_BACKGROUND));
    putColor(properties, "Windows.SysColor.COLOR_BTNFACE", GetSysColor(COLOR_BTNFACE));
    putColor(properties, "Windows.SysColor.COLOR_BTNHIGHLIGHT", GetSysColor(COLOR_BTNHIGHLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_BTNHILIGHT", GetSysColor(COLOR_BTNHILIGHT));
    putColor(properties, "Windows.SysColor.COLOR_BTNSHADOW", GetSysColor(COLOR_BTNSHADOW));
    putColor(properties, "Windows.SysColor.COLOR_BTNTEXT", GetSysColor(COLOR_BTNTEXT));
    putColor(properties, "Windows.SysColor.COLOR_CAPTIONTEXT", GetSysColor(COLOR_CAPTIONTEXT));
    putColor(properties, "Windows.SysColor.COLOR_DESKTOP", GetSysColor(COLOR_DESKTOP));
    putColor(properties, "Windows.SysColor.COLOR_GRADIENTACTIVECAPTION", GetSysColor(COLOR_GRADIENTACTIVECAPTION));
    putColor(properties, "Windows.SysColor.COLOR_GRADIENTINACTIVECAPTION", GetSysColor(COLOR_GRADIENTINACTIVECAPTION));
    putColor(properties, "Windows.SysColor.COLOR_GRAYTEXT", GetSysColor(COLOR_GRAYTEXT));
    putColor(properties, "Windows.SysColor.COLOR_HIGHLIGHT", GetSysColor(COLOR_HIGHLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_HIGHLIGHTTEXT", GetSysColor(COLOR_HIGHLIGHTTEXT));
    putColor(properties, "Windows.SysColor.COLOR_HOTLIGHT", GetSysColor(COLOR_HOTLIGHT));
    putColor(properties, "Windows.SysColor.COLOR_INACTIVEBORDER", GetSysColor(COLOR_INACTIVEBORDER));
    putColor(properties, "Windows.SysColor.COLOR_INACTIVECAPTION", GetSysColor(COLOR_INACTIVECAPTION));
    putColor(properties, "Windows.SysColor.COLOR_INACTIVECAPTIONTEXT", GetSysColor(COLOR_INACTIVECAPTIONTEXT));
    putColor(properties, "Windows.SysColor.COLOR_INFOBK", GetSysColor(COLOR_INFOBK));
    putColor(properties, "Windows.SysColor.COLOR_INFOTEXT", GetSysColor(COLOR_INFOTEXT));
    putColor(properties, "Windows.SysColor.COLOR_MENU", GetSysColor(COLOR_MENU));
    putColor(properties, "Windows.SysColor.COLOR_MENUHILIGHT", GetSysColor(COLOR_MENUHILIGHT));
    putColor(properties, "Windows.SysColor.COLOR_MENUBAR", GetSysColor(COLOR_MENUBAR));
    putColor(properties, "Windows.SysColor.COLOR_MENUTEXT", GetSysColor(COLOR_MENUTEXT));
    putColor(properties, "Windows.SysColor.COLOR_SCROLLBAR", GetSysColor(COLOR_SCROLLBAR));
    putColor(properties, "Windows.SysColor.COLOR_WINDOW", GetSysColor(COLOR_WINDOW));
    putColor(properties, "Windows.SysColor.COLOR_WINDOWFRAME", GetSysColor(COLOR_WINDOWFRAME));
    putColor(properties, "Windows.SysColor.COLOR_WINDOWTEXT", GetSysColor(COLOR_WINDOWTEXT));
}

void ThemeSupport::queryUIColors(jobject properties) const
{
    if (!isRoActivationSupported()) {
        return;
    }

    try {
        ComPtr<IUISettings> settings;
        RO_CHECKED("RoActivateInstance",
                   RoActivateInstance(hstring("Windows.UI.ViewManagement.UISettings"), (IInspectable**)&settings));

        ComPtr<IUISettings3> settings3;
        RO_CHECKED("IUISettings::QueryInterface<IUISettings3>",
                   settings->QueryInterface<IUISettings3>(&settings3));

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

        putColor(properties, "Windows.UIColor.Background", background);
        putColor(properties, "Windows.UIColor.Foreground", foreground);
        putColor(properties, "Windows.UIColor.AccentDark3", accentDark3);
        putColor(properties, "Windows.UIColor.AccentDark2", accentDark2);
        putColor(properties, "Windows.UIColor.AccentDark1", accentDark1);
        putColor(properties, "Windows.UIColor.Accent", accent);
        putColor(properties, "Windows.UIColor.AccentLight1", accentLight1);
        putColor(properties, "Windows.UIColor.AccentLight2", accentLight2);
        putColor(properties, "Windows.UIColor.AccentLight3", accentLight3);
    } catch (RoException const& ex) {
        // If an activation exception occurs, it probably means that we're on a Windows system
        // that doesn't support the UISettings API. This is not a problem, it simply means that
        // we don't report the UISettings properties back to the JavaFX application.
        return;
    }
}

void ThemeSupport::putString(jobject properties, const char* key, const char* value) const
{
    env_->CallObjectMethod(properties, putMethod_,
        env_->NewStringUTF(key),
        value != NULL ? env_->NewStringUTF(value) : NULL);
}

void ThemeSupport::putString(jobject properties, const char* key, const wchar_t* value) const
{
    env_->CallObjectMethod(properties, putMethod_,
        env_->NewStringUTF(key),
        value != NULL ? env_->NewString((jchar*)value, wcslen(value)) : NULL);
}

void ThemeSupport::putBoolean(jobject properties, const char* key, const bool value) const
{
    env_->CallObjectMethod(properties, putMethod_,
        env_->NewStringUTF(key),
        value ? env_->GetStaticObjectField(booleanClass_, trueField_) :
                env_->GetStaticObjectField(booleanClass_, falseField_));
}

void ThemeSupport::putColor(jobject properties, const char* colorName, int colorValue) const
{
    env_->CallObjectMethod(properties, putMethod_,
        env_->NewStringUTF(colorName),
        env_->CallStaticObjectMethod(
            colorClass_, rgbMethod_, GetRValue(colorValue), GetGValue(colorValue), GetBValue(colorValue), 1.0));
}

void ThemeSupport::putColor(jobject properties, const char* colorName, Color colorValue) const
{
    env_->CallObjectMethod(properties, putMethod_,
        env_->NewStringUTF(colorName),
        env_->CallStaticObjectMethod(
            colorClass_, rgbMethod_, colorValue.R, colorValue.G, colorValue.B, (double)colorValue.A / 255.0));
}
