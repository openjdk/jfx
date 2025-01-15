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

#import "PlatformSupport.h"
#import "GlassMacros.h"
#import <Network/Network.h>

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

@implementation PlatformSupport {
    nw_path_monitor_t pathMonitor;
    jobject currentPreferences;
    bool currentPathConstrained;
    bool currentPathExpensive;
}

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

- (id)initWithEnv:(JNIEnv*)jEnv application:(jobject)jApp {
    if (!(self = [super init])) {
        return nil;
    }

    self->env = jEnv;
    self->application = jApp;
    self->currentPreferences = nil;
    self->currentPathConstrained = false;
    self->currentPathExpensive = false;

    [[NSNotificationCenter defaultCenter]
        addObserver:self
        selector:@selector(platformPreferencesDidChange)
        name:NSPreferredScrollerStyleDidChangeNotification
        object:nil];

    [[NSDistributedNotificationCenter defaultCenter]
        addObserver:self
        selector:@selector(platformPreferencesDidChange)
        name:@"AppleInterfaceThemeChangedNotification"
        object:nil];

    [[NSDistributedNotificationCenter defaultCenter]
        addObserver:self
        selector:@selector(platformPreferencesDidChange)
        name:@"AppleColorPreferencesChangedNotification"
        object:nil];

    [[[NSWorkspace sharedWorkspace] notificationCenter]
        addObserver:self
        selector:@selector(platformPreferencesDidChange)
        name:NSWorkspaceAccessibilityDisplayOptionsDidChangeNotification
        object:nil];

    pathMonitor = nw_path_monitor_create();
    nw_path_monitor_set_update_handler(pathMonitor, ^(nw_path_t path) {
        self->currentPathConstrained = nw_path_is_constrained(path);
        self->currentPathExpensive = nw_path_is_expensive(path);
        [self updatePreferences];
    });

    nw_path_monitor_set_queue(pathMonitor, dispatch_get_main_queue());
    nw_path_monitor_start(pathMonitor);

    return self;
}

- (void)stopEventProcessing {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [[NSDistributedNotificationCenter defaultCenter] removeObserver:self];
    [[[NSWorkspace sharedWorkspace] notificationCenter] removeObserver:self];
    nw_path_monitor_cancel(pathMonitor);
}

- (void)platformPreferencesDidChange {
    // Some dynamic colors like NSColor.controlAccentColor don't seem to be reliably updated
    // at the exact moment AppleColorPreferencesChangedNotification is received.
    // As a workaround, we wait for a short period of time (one second seems sufficient) before
    // we query the updated platform preferences.

    [NSObject cancelPreviousPerformRequestsWithTarget:self
              selector:@selector(updatePreferences)
              object:nil];

    [self performSelector:@selector(updatePreferences)
          withObject:nil
          afterDelay:1.0];
}

- (jobject)collectPreferences {
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
    [self queryNSColors:preferences];
    [NSAppearance setCurrentAppearance:lastAppearance];

    [self putBoolean:preferences
          key:"macOS.NSWorkspace.accessibilityDisplayShouldReduceMotion"
          value:[[NSWorkspace sharedWorkspace] accessibilityDisplayShouldReduceMotion]];

    [self putBoolean:preferences
          key:"macOS.NSWorkspace.accessibilityDisplayShouldReduceTransparency"
          value:[[NSWorkspace sharedWorkspace] accessibilityDisplayShouldReduceTransparency]];

    [self putString:preferences
          key:"macOS.NSScroller.preferredScrollerStyle"
          value:[NSScroller preferredScrollerStyle] == NSScrollerStyleOverlay
              ? "NSScrollerStyleOverlay" : "NSScrollerStyleLegacy"];

    [self putBoolean:preferences
          key:"macOS.NWPathMonitor.currentPathConstrained"
          value:currentPathConstrained];

    [self putBoolean:preferences
          key:"macOS.NWPathMonitor.currentPathExpensive"
          value:currentPathExpensive];

    return preferences;
}

- (void)queryNSColors:(jobject)preferences {
    // Label colors
    [self putColor:preferences key:"macOS.NSColor.labelColor" value:[NSColor labelColor]];
    [self putColor:preferences key:"macOS.NSColor.secondaryLabelColor" value:[NSColor secondaryLabelColor]];
    [self putColor:preferences key:"macOS.NSColor.tertiaryLabelColor" value:[NSColor tertiaryLabelColor]];
    [self putColor:preferences key:"macOS.NSColor.quaternaryLabelColor" value:[NSColor quaternaryLabelColor]];

    // Text colors
    [self putColor:preferences key:"macOS.NSColor.textColor" value:[NSColor textColor]];
    [self putColor:preferences key:"macOS.NSColor.placeholderTextColor" value:[NSColor placeholderTextColor]];
    [self putColor:preferences key:"macOS.NSColor.selectedTextColor" value:[NSColor selectedTextColor]];
    [self putColor:preferences key:"macOS.NSColor.textBackgroundColor" value:[NSColor textBackgroundColor]];
    [self putColor:preferences key:"macOS.NSColor.selectedTextBackgroundColor" value:[NSColor selectedTextBackgroundColor]];
    [self putColor:preferences key:"macOS.NSColor.keyboardFocusIndicatorColor" value:[NSColor keyboardFocusIndicatorColor]];
    [self putColor:preferences key:"macOS.NSColor.unemphasizedSelectedTextColor" value:[NSColor unemphasizedSelectedTextColor]];
    [self putColor:preferences key:"macOS.NSColor.unemphasizedSelectedTextBackgroundColor" value:[NSColor unemphasizedSelectedTextBackgroundColor]];

    // Content colors
    [self putColor:preferences key:"macOS.NSColor.linkColor" value:[NSColor linkColor]];
    [self putColor:preferences key:"macOS.NSColor.separatorColor" value:[NSColor separatorColor]];
    [self putColor:preferences key:"macOS.NSColor.selectedContentBackgroundColor" value:[NSColor selectedContentBackgroundColor]];
    [self putColor:preferences key:"macOS.NSColor.unemphasizedSelectedContentBackgroundColor" value:[NSColor unemphasizedSelectedContentBackgroundColor]];

    // Menu colors
    [self putColor:preferences key:"macOS.NSColor.selectedMenuItemTextColor" value:[NSColor selectedMenuItemTextColor]];

    // Table colors
    [self putColor:preferences key:"macOS.NSColor.gridColor" value:[NSColor gridColor]];
    [self putColor:preferences key:"macOS.NSColor.headerTextColor" value:[NSColor headerTextColor]];
    [self putColors:preferences key:"macOS.NSColor.alternatingContentBackgroundColors" value:[NSColor alternatingContentBackgroundColors]];

    // Control colors
    [self putColor:preferences key:"macOS.NSColor.controlAccentColor" value:[NSColor controlAccentColor]];
    [self putColor:preferences key:"macOS.NSColor.controlColor" value:[NSColor controlColor]];
    [self putColor:preferences key:"macOS.NSColor.controlBackgroundColor" value:[NSColor controlBackgroundColor]];
    [self putColor:preferences key:"macOS.NSColor.controlTextColor" value:[NSColor controlTextColor]];
    [self putColor:preferences key:"macOS.NSColor.disabledControlTextColor" value:[NSColor disabledControlTextColor]];
    [self putColor:preferences key:"macOS.NSColor.selectedControlColor" value:[NSColor selectedControlColor]];
    [self putColor:preferences key:"macOS.NSColor.selectedControlTextColor" value:[NSColor selectedControlTextColor]];
    [self putColor:preferences key:"macOS.NSColor.alternateSelectedControlTextColor" value:[NSColor alternateSelectedControlTextColor]];

    const char* controlTint = nil;
    switch ([NSColor currentControlTint]) {
        case NSDefaultControlTint: controlTint = "NSDefaultControlTint"; break;
        case NSGraphiteControlTint: controlTint = "NSGraphiteControlTint"; break;
        case NSBlueControlTint: controlTint = "NSBlueControlTint"; break;
        case NSClearControlTint: controlTint = "NSClearControlTint"; break;
    }
    if (controlTint != nil) {
        [self putString:preferences key:"macOS.NSColor.currentControlTint" value:controlTint];
    }

    // Window colors
    [self putColor:preferences key:"macOS.NSColor.windowBackgroundColor" value:[NSColor windowBackgroundColor]];
    [self putColor:preferences key:"macOS.NSColor.windowFrameTextColor" value:[NSColor windowFrameTextColor]];
    [self putColor:preferences key:"macOS.NSColor.underPageBackgroundColor" value:[NSColor underPageBackgroundColor]];

    // Highlights and shadows
    [self putColor:preferences key:"macOS.NSColor.findHighlightColor" value:[NSColor findHighlightColor]];
    [self putColor:preferences key:"macOS.NSColor.highlightColor" value:[NSColor highlightColor]];
    [self putColor:preferences key:"macOS.NSColor.shadowColor" value:[NSColor shadowColor]];

    // Adaptable system colors
    [self putColor:preferences key:"macOS.NSColor.systemBlueColor" value:[NSColor systemBlueColor]];
    [self putColor:preferences key:"macOS.NSColor.systemBrownColor" value:[NSColor systemBrownColor]];
    [self putColor:preferences key:"macOS.NSColor.systemGrayColor" value:[NSColor systemGrayColor]];
    [self putColor:preferences key:"macOS.NSColor.systemGreenColor" value:[NSColor systemGreenColor]];
    [self putColor:preferences key:"macOS.NSColor.systemIndigoColor" value:[NSColor systemIndigoColor]];
    [self putColor:preferences key:"macOS.NSColor.systemOrangeColor" value:[NSColor systemOrangeColor]];
    [self putColor:preferences key:"macOS.NSColor.systemPinkColor" value:[NSColor systemPinkColor]];
    [self putColor:preferences key:"macOS.NSColor.systemPurpleColor" value:[NSColor systemPurpleColor]];
    [self putColor:preferences key:"macOS.NSColor.systemRedColor" value:[NSColor systemRedColor]];
    [self putColor:preferences key:"macOS.NSColor.systemTealColor" value:[NSColor systemTealColor]];
    [self putColor:preferences key:"macOS.NSColor.systemYellowColor" value:[NSColor systemYellowColor]];
}

/**
 * Collect all platform preferences and notify the JavaFX application when a preference has changed.
 * The change notification includes all preferences, not only the changed preferences.
 */
- (void)updatePreferences {
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

- (void)putBoolean:(jobject)preferences key:(const char*)key value:(bool)value {
    jobject prefKey = (*env)->NewStringUTF(env, key);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefKey);

    jobject prefValue = (*env)->GetStaticObjectField(env, jBooleanClass, value ? jBooleanTRUE : jBooleanFALSE);
    GLASS_CHECK_NONNULL_EXCEPTION_RETURN(env, prefValue);

    (*env)->CallObjectMethod(env, preferences, jMapPutMethod, prefKey, prefValue);
    GLASS_CHECK_EXCEPTION(env);
}

- (void)putString:(jobject)preferences key:(const char*)key value:(const char*)value {
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

- (void)putColor:(jobject)preferences key:(const char*)colorName value:(NSColor*)color {
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

- (void)putColors:(jobject)preferences key:(const char*)colorName value:(NSArray*)colors {
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
