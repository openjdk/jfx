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

#import "ThemeSupport.h"
#import "GlassMacros.h"

@implementation ThemeSupport {
    JNIEnv* env_;
    jclass mapClass_;
    jclass colorClass_;
    jclass booleanClass_;
    jmethodID putMethod_;
    jmethodID rgbMethod_;
    jfieldID trueField_;
    jfieldID falseField_;
    bool initialized_;
}

- (id)initWithEnv:(JNIEnv*)env {
    self = [super init];
    env_ = env;
    initialized_ =
        ((mapClass_ = (jclass)(*env)->FindClass(env, "java/util/Map")) != nil) &&
        ((colorClass_ = (jclass)(*env)->FindClass(env, "javafx/scene/paint/Color")) != nil) &&
        ((booleanClass_ = (jclass)(*env)->FindClass(env, "java/lang/Boolean")) != nil) &&
        ((putMethod_ = (*env)->GetMethodID(env, mapClass_, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")) != 0) &&
        ((rgbMethod_ = (*env)->GetStaticMethodID(env, colorClass_, "rgb", "(IIID)Ljavafx/scene/paint/Color;")) != 0) &&
        ((trueField_ = (*env)->GetStaticFieldID(env, booleanClass_, "TRUE", "Ljava/lang/Boolean;")) != 0) &&
        ((falseField_ = (*env)->GetStaticFieldID(env, booleanClass_, "FALSE", "Ljava/lang/Boolean;")) != 0);
    GLASS_CHECK_EXCEPTION(env);
    return self;
}

- (void)dealloc {
    if (mapClass_ != nil) {
        (*env_)->DeleteLocalRef(env_, mapClass_);
    }

    if (colorClass_ != nil) {
        (*env_)->DeleteLocalRef(env_, colorClass_);
    }

    if (booleanClass_ != nil) {
        (*env_)->DeleteLocalRef(env_, booleanClass_);
    }

    [super dealloc];
}

- (void)queryProperties:(jobject)properties {
    if (!initialized_) {
        return;
    }

    // Label colors
    [self putColor:properties key:"macOS.NSColor.labelColor" value:[NSColor labelColor]];
    [self putColor:properties key:"macOS.NSColor.secondaryLabelColor" value:[NSColor secondaryLabelColor]];
    [self putColor:properties key:"macOS.NSColor.tertiaryLabelColor" value:[NSColor tertiaryLabelColor]];
    [self putColor:properties key:"macOS.NSColor.quaternaryLabelColor" value:[NSColor quaternaryLabelColor]];

    // Text colors
    [self putColor:properties key:"macOS.NSColor.textColor" value:[NSColor textColor]];
    [self putColor:properties key:"macOS.NSColor.placeholderTextColor" value:[NSColor placeholderTextColor]];
    [self putColor:properties key:"macOS.NSColor.selectedTextColor" value:[NSColor selectedTextColor]];
    [self putColor:properties key:"macOS.NSColor.textBackgroundColor" value:[NSColor textBackgroundColor]];
    [self putColor:properties key:"macOS.NSColor.selectedTextBackgroundColor" value:[NSColor selectedTextBackgroundColor]];
    [self putColor:properties key:"macOS.NSColor.keyboardFocusIndicatorColor" value:[NSColor keyboardFocusIndicatorColor]];
    [self putColor:properties key:"macOS.NSColor.unemphasizedSelectedTextColor" value:[NSColor unemphasizedSelectedTextColor]];
    [self putColor:properties key:"macOS.NSColor.unemphasizedSelectedTextBackgroundColor" value:[NSColor unemphasizedSelectedTextBackgroundColor]];

    // Content colors
    [self putColor:properties key:"macOS.NSColor.linkColor" value:[NSColor linkColor]];
    [self putColor:properties key:"macOS.NSColor.separatorColor" value:[NSColor separatorColor]];
    [self putColor:properties key:"macOS.NSColor.selectedContentBackgroundColor" value:[NSColor selectedContentBackgroundColor]];
    [self putColor:properties key:"macOS.NSColor.unemphasizedSelectedContentBackgroundColor" value:[NSColor unemphasizedSelectedContentBackgroundColor]];

    // Menu colors
    [self putColor:properties key:"macOS.NSColor.selectedMenuItemTextColor" value:[NSColor selectedMenuItemTextColor]];

    // Table colors
    [self putColor:properties key:"macOS.NSColor.gridColor" value:[NSColor gridColor]];
    [self putColor:properties key:"macOS.NSColor.headerTextColor" value:[NSColor headerTextColor]];
    [self putColors:properties key:"macOS.NSColor.alternatingContentBackgroundColors" value:[NSColor alternatingContentBackgroundColors]];

    // Control colors
    [self putColor:properties key:"macOS.NSColor.controlAccentColor" value:[NSColor controlAccentColor]];
    [self putColor:properties key:"macOS.NSColor.controlColor" value:[NSColor controlColor]];
    [self putColor:properties key:"macOS.NSColor.controlBackgroundColor" value:[NSColor controlBackgroundColor]];
    [self putColor:properties key:"macOS.NSColor.controlTextColor" value:[NSColor controlTextColor]];
    [self putColor:properties key:"macOS.NSColor.disabledControlTextColor" value:[NSColor disabledControlTextColor]];
    [self putColor:properties key:"macOS.NSColor.selectedControlColor" value:[NSColor selectedControlColor]];
    [self putColor:properties key:"macOS.NSColor.selectedControlTextColor" value:[NSColor selectedControlTextColor]];
    [self putColor:properties key:"macOS.NSColor.alternateSelectedControlTextColor" value:[NSColor alternateSelectedControlTextColor]];

    const char* controlTint = nil;
    switch ([NSColor currentControlTint]) {
        case NSDefaultControlTint: controlTint = "NSDefaultControlTint"; break;
        case NSGraphiteControlTint: controlTint = "NSGraphiteControlTint"; break;
        case NSBlueControlTint: controlTint = "NSBlueControlTint"; break;
        case NSClearControlTint: controlTint = "NSClearControlTint"; break;
    }
    if (controlTint != nil) {
        [self putString:properties key:"macOS.NSColor.currentControlTint" value:controlTint];
    }

    // Window colors
    [self putColor:properties key:"macOS.NSColor.windowBackgroundColor" value:[NSColor windowBackgroundColor]];
    [self putColor:properties key:"macOS.NSColor.windowFrameTextColor" value:[NSColor windowFrameTextColor]];
    [self putColor:properties key:"macOS.NSColor.underPageBackgroundColor" value:[NSColor underPageBackgroundColor]];

    // Highlights and shadows
    [self putColor:properties key:"macOS.NSColor.findHighlightColor" value:[NSColor findHighlightColor]];
    [self putColor:properties key:"macOS.NSColor.highlightColor" value:[NSColor highlightColor]];
    [self putColor:properties key:"macOS.NSColor.shadowColor" value:[NSColor shadowColor]];

    // Adaptable system colors
    [self putColor:properties key:"macOS.NSColor.systemBlueColor" value:[NSColor systemBlueColor]];
    [self putColor:properties key:"macOS.NSColor.systemBrownColor" value:[NSColor systemBrownColor]];
    [self putColor:properties key:"macOS.NSColor.systemGrayColor" value:[NSColor systemGrayColor]];
    [self putColor:properties key:"macOS.NSColor.systemGreenColor" value:[NSColor systemGreenColor]];
    [self putColor:properties key:"macOS.NSColor.systemIndigoColor" value:[NSColor systemIndigoColor]];
    [self putColor:properties key:"macOS.NSColor.systemOrangeColor" value:[NSColor systemOrangeColor]];
    [self putColor:properties key:"macOS.NSColor.systemPinkColor" value:[NSColor systemPinkColor]];
    [self putColor:properties key:"macOS.NSColor.systemPurpleColor" value:[NSColor systemPurpleColor]];
    [self putColor:properties key:"macOS.NSColor.systemRedColor" value:[NSColor systemRedColor]];
    [self putColor:properties key:"macOS.NSColor.systemTealColor" value:[NSColor systemTealColor]];
    [self putColor:properties key:"macOS.NSColor.systemYellowColor" value:[NSColor systemYellowColor]];
}

- (void)putString:(jobject)properties key:(const char*)key value:(const char*)value {
    (*env_)->CallObjectMethod(env_, properties, putMethod_,
        (*env_)->NewStringUTF(env_, key),
        value != nil ? (*env_)->NewStringUTF(env_, value) : nil);
}

- (void)putColor:(jobject)properties key:(const char*)colorName value:(NSColor*)color {
    NSColor* c = [color colorUsingColorSpace:[NSColorSpace deviceRGBColorSpace]];
    (*env_)->CallObjectMethod(env_, properties, putMethod_,
        (*env_)->NewStringUTF(env_, colorName),
        (*env_)->CallStaticObjectMethod(
            env_, colorClass_, rgbMethod_,
            (int)([c redComponent] * 255.0f),
            (int)([c greenComponent] * 255.0f),
            (int)([c blueComponent] * 255.0f),
            (double)[c alphaComponent]));
}

- (void)putColors:(jobject)properties key:(const char*)colorName value:(NSArray*)colors {
    int count = [colors count];
    jobjectArray res = (*env_)->NewObjectArray(env_, count, colorClass_, nil);

    for (int i = 0; i < count; ++i) {
        NSColor* c = [colors[i] colorUsingColorSpace:[NSColorSpace deviceRGBColorSpace]];
        jobject fxcolor = (*env_)->CallStaticObjectMethod(
            env_, colorClass_, rgbMethod_,
            (int)([c redComponent] * 255.0f),
            (int)([c greenComponent] * 255.0f),
            (int)([c blueComponent] * 255.0f),
            (double)[c alphaComponent]);

        (*env_)->SetObjectArrayElement(env_, res, i, fxcolor);
    }

    (*env_)->CallObjectMethod(env_, properties, putMethod_, (*env_)->NewStringUTF(env_, colorName), res);
}

@end