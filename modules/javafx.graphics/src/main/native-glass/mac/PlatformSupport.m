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

#import "PlatformSupport.h"
#import "GlassMacros.h"

#define INIT_CLASS(CLS, NAME)\
    if (CLS == nil) {\
        jclass cls = (*env)->FindClass(env, NAME);\
        if ((*env)->ExceptionCheck(env)) {\
            GLASS_CHECK_EXCEPTION(env);\
            return;\
        }\
        CLS = (*env)->NewGlobalRef(env, cls);\
    }

#define INIT_METHOD(CLS, METHOD, NAME, SIG)\
    if (METHOD == nil) {\
        METHOD = (*env)->GetMethodID(env, CLS, NAME, SIG);\
        if ((*env)->ExceptionCheck(env)) {\
            GLASS_CHECK_EXCEPTION(env);\
            return;\
        }\
    }

#define INIT_STATIC_METHOD(CLS, METHOD, NAME, SIG)\
    if (METHOD == nil) {\
        METHOD = (*env)->GetStaticMethodID(env, CLS, NAME, SIG);\
        if ((*env)->ExceptionCheck(env)) {\
            GLASS_CHECK_EXCEPTION(env);\
            return;\
        }\
    }

#define INIT_STATIC_FIELD(CLS, FIELD, NAME, SIG)\
    if (FIELD == nil) {\
        FIELD = (*env)->GetStaticFieldID(env, CLS, NAME, SIG);\
        if ((*env)->ExceptionCheck(env)) {\
            GLASS_CHECK_EXCEPTION(env);\
            return;\
        }\
    }

static jobject currentPreferences = nil;

@implementation PlatformSupport

+ (void)initIDs:(JNIEnv*)env {
    INIT_CLASS(jMapClass, "java/util/Map");
    INIT_METHOD(jMapClass, jMapPutMethod, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

    INIT_CLASS(jHashMapClass, "java/util/HashMap");
    INIT_METHOD(jHashMapClass, jHashMapInitMethod, "<init>", "()V");

    INIT_CLASS(jBooleanClass, "java/lang/Boolean");
    INIT_STATIC_FIELD(jBooleanClass, jBooleanTRUE, "TRUE", "Ljava/lang/Boolean;");
    INIT_STATIC_FIELD(jBooleanClass, jBooleanFALSE, "FALSE", "Ljava/lang/Boolean;");

    INIT_CLASS(jCollectionsClass, "java/util/Collections");
    INIT_STATIC_METHOD(jCollectionsClass, jCollectionsUnmodifiableMapMethod, "unmodifiableMap", "(Ljava/util/Map;)Ljava/util/Map;");

    INIT_CLASS(jObjectClass, "java/lang/Object");
    INIT_METHOD(jObjectClass, jObjectEqualsMethod, "equals", "(Ljava/lang/Object;)Z");

    INIT_CLASS(jColorClass, "javafx/scene/paint/Color");
    INIT_STATIC_METHOD(jColorClass, jColorRgbMethod, "rgb", "(IIID)Ljavafx/scene/paint/Color;");
}

+ (jobject)collectPreferences {
    GET_MAIN_JENV;

    jobject preferences = (*env)->NewObject(env, jHashMapClass, jHashMapInitMethod);
    GLASS_CHECK_EXCEPTION(env);
    if (preferences == nil) {
        return nil;
    }

    // The current appearance is set to the system appearance when the application is started.
    // Since the system appearance can change while the application is running, we need to set
    // the current appearance to the application's effective appearance before querying system
    // colors.
    NSAppearance* lastAppearance = [NSAppearance currentAppearance];
    [NSAppearance setCurrentAppearance:[NSApp effectiveAppearance]];
    [PlatformSupport queryNSColors:preferences];
    [NSAppearance setCurrentAppearance:lastAppearance];

    return preferences;
}

+ (void)queryNSColors:(jobject)preferences {
    // Label colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.labelColor" value:[NSColor labelColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.secondaryLabelColor" value:[NSColor secondaryLabelColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.tertiaryLabelColor" value:[NSColor tertiaryLabelColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.quaternaryLabelColor" value:[NSColor quaternaryLabelColor]];

    // Text colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.textColor" value:[NSColor textColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.placeholderTextColor" value:[NSColor placeholderTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedTextColor" value:[NSColor selectedTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.textBackgroundColor" value:[NSColor textBackgroundColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedTextBackgroundColor" value:[NSColor selectedTextBackgroundColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.keyboardFocusIndicatorColor" value:[NSColor keyboardFocusIndicatorColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.unemphasizedSelectedTextColor" value:[NSColor unemphasizedSelectedTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.unemphasizedSelectedTextBackgroundColor" value:[NSColor unemphasizedSelectedTextBackgroundColor]];

    // Content colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.linkColor" value:[NSColor linkColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.separatorColor" value:[NSColor separatorColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedContentBackgroundColor" value:[NSColor selectedContentBackgroundColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.unemphasizedSelectedContentBackgroundColor" value:[NSColor unemphasizedSelectedContentBackgroundColor]];

    // Menu colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedMenuItemTextColor" value:[NSColor selectedMenuItemTextColor]];

    // Table colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.gridColor" value:[NSColor gridColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.headerTextColor" value:[NSColor headerTextColor]];
    [PlatformSupport putColors:preferences key:"macOS.NSColor.alternatingContentBackgroundColors" value:[NSColor alternatingContentBackgroundColors]];

    // Control colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.controlAccentColor" value:[NSColor controlAccentColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.controlColor" value:[NSColor controlColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.controlBackgroundColor" value:[NSColor controlBackgroundColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.controlTextColor" value:[NSColor controlTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.disabledControlTextColor" value:[NSColor disabledControlTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedControlColor" value:[NSColor selectedControlColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.selectedControlTextColor" value:[NSColor selectedControlTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.alternateSelectedControlTextColor" value:[NSColor alternateSelectedControlTextColor]];

    const char* controlTint = nil;
    switch ([NSColor currentControlTint]) {
        case NSDefaultControlTint: controlTint = "NSDefaultControlTint"; break;
        case NSGraphiteControlTint: controlTint = "NSGraphiteControlTint"; break;
        case NSBlueControlTint: controlTint = "NSBlueControlTint"; break;
        case NSClearControlTint: controlTint = "NSClearControlTint"; break;
    }
    if (controlTint != nil) {
        [PlatformSupport putString:preferences key:"macOS.NSColor.currentControlTint" value:controlTint];
    }

    // Window colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.windowBackgroundColor" value:[NSColor windowBackgroundColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.windowFrameTextColor" value:[NSColor windowFrameTextColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.underPageBackgroundColor" value:[NSColor underPageBackgroundColor]];

    // Highlights and shadows
    [PlatformSupport putColor:preferences key:"macOS.NSColor.findHighlightColor" value:[NSColor findHighlightColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.highlightColor" value:[NSColor highlightColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.shadowColor" value:[NSColor shadowColor]];

    // Adaptable system colors
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemBlueColor" value:[NSColor systemBlueColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemBrownColor" value:[NSColor systemBrownColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemGrayColor" value:[NSColor systemGrayColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemGreenColor" value:[NSColor systemGreenColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemIndigoColor" value:[NSColor systemIndigoColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemOrangeColor" value:[NSColor systemOrangeColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemPinkColor" value:[NSColor systemPinkColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemPurpleColor" value:[NSColor systemPurpleColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemRedColor" value:[NSColor systemRedColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemTealColor" value:[NSColor systemTealColor]];
    [PlatformSupport putColor:preferences key:"macOS.NSColor.systemYellowColor" value:[NSColor systemYellowColor]];
}

+ (void)updatePreferences:(jobject)application {
    GET_MAIN_JENV;

    jobject newPreferences = [self collectPreferences];
    if (newPreferences == nil) {
        return;
    }

    jboolean preferencesChanged = !(*env)->CallBooleanMethod(
        env, newPreferences, jObjectEqualsMethod, currentPreferences);
    GLASS_CHECK_EXCEPTION(env);

    if (preferencesChanged) {
        if (currentPreferences != nil) {
            (*env)->DeleteGlobalRef(env, currentPreferences);
        }

        currentPreferences = (*env)->NewGlobalRef(env, newPreferences);

        jobject unmodifiablePreferences = (*env)->CallStaticObjectMethod(
            env, jCollectionsClass, jCollectionsUnmodifiableMapMethod, newPreferences);
        GLASS_CHECK_EXCEPTION(env);

        if (unmodifiablePreferences != nil) {
            (*env)->CallVoidMethod(
                env, application,
                javaIDs.MacApplication.notifyPreferencesChanged,
                unmodifiablePreferences);
            GLASS_CHECK_EXCEPTION(env);

            (*env)->DeleteLocalRef(env, unmodifiablePreferences);
        }
    }

    (*env)->DeleteLocalRef(env, newPreferences);
}

+ (void)putString:(jobject)preferences key:(const char*)key value:(const char*)value {
    GET_MAIN_JENV;

    jobject prefKey = (*env)->NewStringUTF(env, key);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefKey);

    jobject prefValue = nil;
    if (value != nil) {
        prefValue = (*env)->NewStringUTF(env, value);
        GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefValue);
    }

    (*env)->CallObjectMethod(env, preferences, jMapPutMethod, prefKey, prefValue);
    GLASS_CHECK_EXCEPTION(env);
}

+ (void)putColor:(jobject)preferences key:(const char*)colorName value:(NSColor*)color {
    GET_MAIN_JENV;

    jobject prefKey = (*env)->NewStringUTF(env, colorName);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefKey);

    NSColor* c = [color colorUsingColorSpace:[NSColorSpace sRGBColorSpace]];
    jobject prefValue = (*env)->CallStaticObjectMethod(
        env, jColorClass, jColorRgbMethod,
        (int)([c redComponent] * 255.0f),
        (int)([c greenComponent] * 255.0f),
        (int)([c blueComponent] * 255.0f),
        (double)[c alphaComponent]);
    GLASS_CHECK_EXCEPTION_RETURN(env);

    (*env)->CallObjectMethod(env, preferences, jMapPutMethod, prefKey, prefValue);
    GLASS_CHECK_EXCEPTION(env);
}

+ (void)putColors:(jobject)preferences key:(const char*)colorName value:(NSArray*)colors {
    GET_MAIN_JENV;

    jobject prefKey = (*env)->NewStringUTF(env, colorName);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefKey);

    int count = [colors count];
    jobjectArray prefValue = (*env)->NewObjectArray(env, count, jColorClass, nil);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefValue);

    for (int i = 0; i < count; ++i) {
        NSColor* c = [colors[i] colorUsingColorSpace:[NSColorSpace sRGBColorSpace]];
        jobject fxcolor = (*env)->CallStaticObjectMethod(
            env, jColorClass, jColorRgbMethod,
            (int)([c redComponent] * 255.0f),
            (int)([c greenComponent] * 255.0f),
            (int)([c blueComponent] * 255.0f),
            (double)[c alphaComponent]);
        GLASS_CHECK_EXCEPTION_RETURN(env);

        (*env)->SetObjectArrayElement(env, prefValue, i, fxcolor);
        GLASS_CHECK_EXCEPTION_RETURN(env);
    }

    (*env)->CallObjectMethod(env, preferences, jMapPutMethod, prefKey, prefValue);
    GLASS_CHECK_EXCEPTION(env);
}

@end
