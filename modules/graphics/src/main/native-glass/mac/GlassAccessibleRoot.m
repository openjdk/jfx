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

// TODO: Might be able to get rid of some of these imports
#import "GlassAccessibleRoot.h"
#import "GlassAccessibleBaseProvider.h"
#import "GlassMacros.h"
#import "GlassWindow.h"
#import <AppKit/AppKit.h>
#import "com_sun_glass_ui_accessible_mac_MacAccessibleRoot.h"

// Pull in the NSAccessibility Informal Protocol.
// It defines the NSAccessibility Category which adds the NSAccessibility methods to NSObject
#import <AppKit/NSAccessibility.h>

//#define VERBOSE
#ifndef VERBOSE
#define LOG(MSG, ...)
#else
#define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static jmethodID midGetPropertyValue = 0;
static jmethodID midGetChildren = 0;
static jmethodID midBoundingRectangle = 0;
static jmethodID midElementProviderFromPoint = 0;

// TODO: Probably should move this stuff into another file.
//
// Collections used to convert between integer IDs and NSAccessibility's NSString IDs.
// The enums on the Java side need to be kept in sync with these collections.
//
// TODO: Is there a better way to code the initialization of these collections?

static NSDictionary* attributeIds = nil;
static NSArray* eventIds = nil;
static NSArray* roleIds = nil;
static NSArray* attributes = nil;
static NSArray* settableAttribute = nil ;

@implementation GlassAccessibleRoot

+ (void) initialize {
    if (self == [GlassAccessibleRoot class]) {
        attributeIds = [[NSDictionary dictionaryWithObjectsAndKeys:
        // Standard attributes in value, key order
        [NSNumber numberWithInt:0], NSAccessibilityChildrenAttribute,
        [NSNumber numberWithInt:1], NSAccessibilityContentsAttribute,
        [NSNumber numberWithInt:2], NSAccessibilityDescriptionAttribute,
        [NSNumber numberWithInt:3], NSAccessibilityEnabledAttribute,
        [NSNumber numberWithInt:4], NSAccessibilityFocusedAttribute,
        [NSNumber numberWithInt:5], NSAccessibilityHelpAttribute,
        [NSNumber numberWithInt:6], NSAccessibilityMaxValueAttribute,
        [NSNumber numberWithInt:7], NSAccessibilityMinValueAttribute,
        [NSNumber numberWithInt:8], NSAccessibilityParentAttribute,
        [NSNumber numberWithInt:9], NSAccessibilityPositionAttribute,
        [NSNumber numberWithInt:10], NSAccessibilityRoleAttribute,
        [NSNumber numberWithInt:11], NSAccessibilityRoleDescriptionAttribute,
        [NSNumber numberWithInt:12], NSAccessibilitySelectedChildrenAttribute,
        [NSNumber numberWithInt:13], NSAccessibilityShownMenuAttribute,
        [NSNumber numberWithInt:14], NSAccessibilitySizeAttribute,
        [NSNumber numberWithInt:15], NSAccessibilitySubroleAttribute,
        [NSNumber numberWithInt:16], NSAccessibilityTitleAttribute,
        [NSNumber numberWithInt:17], NSAccessibilityTopLevelUIElementAttribute,
        [NSNumber numberWithInt:18], NSAccessibilityValueAttribute,
        [NSNumber numberWithInt:19], NSAccessibilityValueDescriptionAttribute,
        [NSNumber numberWithInt:20], NSAccessibilityVisibleChildrenAttribute,
        [NSNumber numberWithInt:21], NSAccessibilityWindowAttribute,
        // Text-specific attributes
        [NSNumber numberWithInt:22], NSAccessibilityInsertionPointLineNumberAttribute,
        [NSNumber numberWithInt:23], NSAccessibilityNumberOfCharactersAttribute,
        [NSNumber numberWithInt:24], NSAccessibilitySelectedTextAttribute,
        [NSNumber numberWithInt:25], NSAccessibilitySelectedTextRangeAttribute,
        [NSNumber numberWithInt:26], NSAccessibilitySelectedTextRangesAttribute,
        [NSNumber numberWithInt:27], NSAccessibilitySharedCharacterRangeAttribute,
        [NSNumber numberWithInt:28], NSAccessibilitySharedTextUIElementsAttribute,
        [NSNumber numberWithInt:29], NSAccessibilityVisibleCharacterRangeAttribute,
        // Text-specific parameterized attributes
        [NSNumber numberWithInt:30], NSAccessibilityAttributedStringForRangeParameterizedAttribute,
        [NSNumber numberWithInt:31], NSAccessibilityBoundsForRangeParameterizedAttribute,
        [NSNumber numberWithInt:32], NSAccessibilityLineForIndexParameterizedAttribute,
        [NSNumber numberWithInt:33], NSAccessibilityRTFForRangeParameterizedAttribute,
        [NSNumber numberWithInt:34], NSAccessibilityRangeForIndexParameterizedAttribute,
        [NSNumber numberWithInt:35], NSAccessibilityRangeForLineParameterizedAttribute,
        [NSNumber numberWithInt:36], NSAccessibilityRangeForPositionParameterizedAttribute,
        [NSNumber numberWithInt:37], NSAccessibilityStringForRangeParameterizedAttribute,
        [NSNumber numberWithInt:38], NSAccessibilityStyleRangeForIndexParameterizedAttribute,
        // Attributes used with attributed strings
        [NSNumber numberWithInt:39], NSAccessibilityAttachmentTextAttribute,
        [NSNumber numberWithInt:40], NSAccessibilityBackgroundColorTextAttribute,
        [NSNumber numberWithInt:41], NSAccessibilityFontFamilyKey,
        [NSNumber numberWithInt:42], NSAccessibilityFontNameKey,
        [NSNumber numberWithInt:43], NSAccessibilityFontSizeKey,
        [NSNumber numberWithInt:44], NSAccessibilityFontTextAttribute,
        [NSNumber numberWithInt:45], NSAccessibilityForegroundColorTextAttribute,
        [NSNumber numberWithInt:46], NSAccessibilityLinkTextAttribute,
        [NSNumber numberWithInt:47], NSAccessibilityMisspelledTextAttribute,
        [NSNumber numberWithInt:48], NSAccessibilityShadowTextAttribute,
        [NSNumber numberWithInt:49], NSAccessibilityStrikethroughColorTextAttribute,
        [NSNumber numberWithInt:50], NSAccessibilityStrikethroughTextAttribute,
        [NSNumber numberWithInt:51], NSAccessibilitySuperscriptTextAttribute,
        [NSNumber numberWithInt:52], NSAccessibilityUnderlineColorTextAttribute,
        [NSNumber numberWithInt:53], NSAccessibilityUnderlineTextAttribute,
        [NSNumber numberWithInt:54], NSAccessibilityVisibleNameKey,
        // Window-specific attributes
        [NSNumber numberWithInt:55], NSAccessibilityCancelButtonAttribute,
        [NSNumber numberWithInt:56], NSAccessibilityCloseButtonAttribute,
        [NSNumber numberWithInt:57], NSAccessibilityDefaultButtonAttribute,
        [NSNumber numberWithInt:58], NSAccessibilityGrowAreaAttribute,
        [NSNumber numberWithInt:59], NSAccessibilityMainAttribute,
        [NSNumber numberWithInt:60], NSAccessibilityMinimizeButtonAttribute,
        [NSNumber numberWithInt:61], NSAccessibilityMinimizedAttribute,
        [NSNumber numberWithInt:62], NSAccessibilityModalAttribute,
        [NSNumber numberWithInt:63], NSAccessibilityProxyAttribute,
        [NSNumber numberWithInt:64], NSAccessibilityToolbarButtonAttribute,
        [NSNumber numberWithInt:65], NSAccessibilityZoomButtonAttribute,
        // Application-specific attributes
        [NSNumber numberWithInt:66], NSAccessibilityClearButtonAttribute,
        [NSNumber numberWithInt:67], NSAccessibilityColumnTitlesAttribute,
        [NSNumber numberWithInt:68], NSAccessibilityFocusedUIElementAttribute,
        [NSNumber numberWithInt:69], NSAccessibilityFocusedWindowAttribute,
        [NSNumber numberWithInt:70], NSAccessibilityFrontmostAttribute,
        [NSNumber numberWithInt:71], NSAccessibilityHiddenAttribute,
        [NSNumber numberWithInt:72], NSAccessibilityMainWindowAttribute,
        [NSNumber numberWithInt:73], NSAccessibilityMenuBarAttribute,
        [NSNumber numberWithInt:74], NSAccessibilityOrientationAttribute,
        [NSNumber numberWithInt:75], NSAccessibilitySearchButtonAttribute,
        [NSNumber numberWithInt:76], NSAccessibilitySearchMenuAttribute,
        [NSNumber numberWithInt:77], NSAccessibilityWindowsAttribute,
        // Grid view attributes
        [NSNumber numberWithInt:78], NSAccessibilityColumnCountAttribute,
        [NSNumber numberWithInt:79], NSAccessibilityOrderedByRowAttribute,
        [NSNumber numberWithInt:80], NSAccessibilityRowCountAttribute,
        // Table view and outline view attributes
        [NSNumber numberWithInt:81], NSAccessibilityColumnHeaderUIElementsAttribute,
        [NSNumber numberWithInt:82], NSAccessibilityColumnsAttribute,
        [NSNumber numberWithInt:83], NSAccessibilityRowHeaderUIElementsAttribute,
        [NSNumber numberWithInt:84], NSAccessibilityRowsAttribute,
        [NSNumber numberWithInt:85], NSAccessibilitySelectedColumnsAttribute,
        [NSNumber numberWithInt:86], NSAccessibilitySelectedRowsAttribute,
        [NSNumber numberWithInt:87], NSAccessibilitySortDirectionAttribute,
        [NSNumber numberWithInt:88], NSAccessibilityVisibleColumnsAttribute,
        [NSNumber numberWithInt:89], NSAccessibilityVisibleRowsAttribute,
        // Outline view attributes
        [NSNumber numberWithInt:90], NSAccessibilityDisclosedByRowAttribute,
        [NSNumber numberWithInt:91], NSAccessibilityDisclosedRowsAttribute,
        [NSNumber numberWithInt:92], NSAccessibilityDisclosingAttribute,
        [NSNumber numberWithInt:93], NSAccessibilityDisclosureLevelAttribute,
        // Cell-based table attributes
        [NSNumber numberWithInt:94], NSAccessibilitySelectedCellsAttribute,
        [NSNumber numberWithInt:95], NSAccessibilityVisibleCellsAttribute,
        // Cell-based table parameterized attributes
        [NSNumber numberWithInt:96], NSAccessibilityCellForColumnAndRowParameterizedAttribute,
        // Cell attributes
        [NSNumber numberWithInt:97], NSAccessibilityRowIndexRangeAttribute,
        [NSNumber numberWithInt:98], NSAccessibilityColumnIndexRangeAttribute,
        // Layout area attributes
        [NSNumber numberWithInt:99], NSAccessibilityHorizontalUnitsAttribute,
        [NSNumber numberWithInt:100], NSAccessibilityVerticalUnitsAttribute,
        [NSNumber numberWithInt:101], NSAccessibilityHorizontalUnitDescriptionAttribute,
        [NSNumber numberWithInt:102], NSAccessibilityVerticalUnitDescriptionAttribute,
        // Layout area parameterized attributes
        [NSNumber numberWithInt:103], NSAccessibilityLayoutPointForScreenPointParameterizedAttribute,
        [NSNumber numberWithInt:104], NSAccessibilityLayoutSizeForScreenSizeParameterizedAttribute,
        [NSNumber numberWithInt:105], NSAccessibilityScreenPointForLayoutPointParameterizedAttribute,
        [NSNumber numberWithInt:106], NSAccessibilityScreenSizeForLayoutSizeParameterizedAttribute,
        // Slider attributes
        [NSNumber numberWithInt:107], NSAccessibilityAllowedValuesAttribute,
        [NSNumber numberWithInt:108], NSAccessibilityLabelUIElementsAttribute,
        [NSNumber numberWithInt:109], NSAccessibilityLabelValueAttribute,
        // Screen matte attributes
        [NSNumber numberWithInt:110], NSAccessibilityMatteContentUIElementAttribute,
        [NSNumber numberWithInt:111], NSAccessibilityMatteHoleAttribute,
        // Ruler view attributes
        [NSNumber numberWithInt:112], NSAccessibilityMarkerGroupUIElementAttribute,
        [NSNumber numberWithInt:113], NSAccessibilityMarkerTypeAttribute,
        [NSNumber numberWithInt:114], NSAccessibilityMarkerTypeDescriptionAttribute,
        [NSNumber numberWithInt:115], NSAccessibilityMarkerUIElementsAttribute,
        [NSNumber numberWithInt:116], NSAccessibilityMarkerValuesAttribute,
        [NSNumber numberWithInt:117], NSAccessibilityUnitDescriptionAttribute,
        [NSNumber numberWithInt:118], NSAccessibilityUnitsAttribute,
        // Ruler marker type values
        [NSNumber numberWithInt:119], NSAccessibilityCenterTabStopMarkerTypeValue,
        [NSNumber numberWithInt:120], NSAccessibilityDecimalTabStopMarkerTypeValue,
        [NSNumber numberWithInt:121], NSAccessibilityFirstLineIndentMarkerTypeValue,
        [NSNumber numberWithInt:122], NSAccessibilityHeadIndentMarkerTypeValue,
        [NSNumber numberWithInt:123], NSAccessibilityLeftTabStopMarkerTypeValue,
        [NSNumber numberWithInt:124], NSAccessibilityRightTabStopMarkerTypeValue,
        [NSNumber numberWithInt:125], NSAccessibilityTailIndentMarkerTypeValue,
        [NSNumber numberWithInt:126], NSAccessibilityUnknownMarkerTypeValue,
        // Linkage elements
        [NSNumber numberWithInt:127], NSAccessibilityLinkedUIElementsAttribute,
        [NSNumber numberWithInt:128], NSAccessibilityServesAsTitleForUIElementsAttribute,
        [NSNumber numberWithInt:129], NSAccessibilityTitleUIElementAttribute,
        // Miscellaneous attributes
        [NSNumber numberWithInt:130], NSAccessibilityDecrementButtonAttribute,
        [NSNumber numberWithInt:131], NSAccessibilityDocumentAttribute,
        [NSNumber numberWithInt:132], NSAccessibilityEditedAttribute,
        [NSNumber numberWithInt:133], NSAccessibilityExpandedAttribute,
        [NSNumber numberWithInt:134], NSAccessibilityFilenameAttribute,
        [NSNumber numberWithInt:135], NSAccessibilityHeaderAttribute,
        [NSNumber numberWithInt:136], NSAccessibilityHorizontalScrollBarAttribute,
        [NSNumber numberWithInt:137], NSAccessibilityIncrementButtonAttribute,
        [NSNumber numberWithInt:138], NSAccessibilityIndexAttribute,
        [NSNumber numberWithInt:139], NSAccessibilityNextContentsAttribute,
        [NSNumber numberWithInt:140], NSAccessibilityOverflowButtonAttribute,
        [NSNumber numberWithInt:141], NSAccessibilityPreviousContentsAttribute,
        [NSNumber numberWithInt:142], NSAccessibilitySelectedAttribute,
        [NSNumber numberWithInt:143], NSAccessibilitySplittersAttribute,
        [NSNumber numberWithInt:144], NSAccessibilityTabsAttribute,
        [NSNumber numberWithInt:145], NSAccessibilityURLAttribute,
        [NSNumber numberWithInt:146], NSAccessibilityVerticalScrollBarAttribute,
        [NSNumber numberWithInt:147], NSAccessibilityWarningValueAttribute,
        [NSNumber numberWithInt:148], NSAccessibilityCriticalValueAttribute,
        [NSNumber numberWithInt:149], NSAccessibilityPlaceholderValueAttribute,
        nil] retain];

        eventIds = [[NSArray arrayWithObjects:
        // Focus-change notifications
        NSAccessibilityMainWindowChangedNotification,
        NSAccessibilityFocusedWindowChangedNotification,
        NSAccessibilityFocusedUIElementChangedNotification,
        // Window-change notifications
        NSAccessibilityWindowCreatedNotification,
        NSAccessibilityWindowDeminiaturizedNotification,
        NSAccessibilityWindowMiniaturizedNotification,
        NSAccessibilityWindowMovedNotification,
        NSAccessibilityWindowResizedNotification,
        // Application notifications
        NSAccessibilityApplicationActivatedNotification,
        NSAccessibilityApplicationDeactivatedNotification,
        NSAccessibilityApplicationHiddenNotification,
        NSAccessibilityApplicationShownNotification,
        // Drawer and sheet notifications
        NSAccessibilityDrawerCreatedNotification,
        NSAccessibilitySheetCreatedNotification,
        // Element notifications
        NSAccessibilityCreatedNotification,
        NSAccessibilityMovedNotification,
        NSAccessibilityResizedNotification,
        NSAccessibilityTitleChangedNotification,
        NSAccessibilityUIElementDestroyedNotification,
        NSAccessibilityValueChangedNotification,
        // Miscellaneous notifications
        NSAccessibilityHelpTagCreatedNotification,
        NSAccessibilityRowCountChangedNotification,
        NSAccessibilitySelectedChildrenChangedNotification,
        NSAccessibilitySelectedColumnsChangedNotification,
        NSAccessibilitySelectedRowsChangedNotification,
        NSAccessibilitySelectedTextChangedNotification,
        NSAccessibilityRowExpandedNotification,
        NSAccessibilityRowCollapsedNotification,
        NSAccessibilitySelectedCellsChangedNotification,
        NSAccessibilityUnitsChangedNotification,
        NSAccessibilitySelectedChildrenMovedNotification,
        nil] retain];

        roleIds =  [[NSArray arrayWithObjects:
        NSAccessibilityApplicationRole,
        NSAccessibilityBrowserRole,
        NSAccessibilityBusyIndicatorRole,
        NSAccessibilityButtonRole,
        NSAccessibilityCellRole,
        NSAccessibilityCheckBoxRole,
        NSAccessibilityColorWellRole,
        NSAccessibilityColumnRole,
        NSAccessibilityComboBoxRole,
        NSAccessibilityDisclosureTriangleRole,
        NSAccessibilityDrawerRole,
        NSAccessibilityGridRole,
        NSAccessibilityGroupRole,
        NSAccessibilityGrowAreaRole,
        NSAccessibilityHandleRole,
        NSAccessibilityHelpTagRole,
        NSAccessibilityImageRole,
        NSAccessibilityIncrementorRole,
        NSAccessibilityLayoutAreaRole,
        NSAccessibilityLayoutItemRole,
        NSAccessibilityLinkRole,
        NSAccessibilityListRole,
        NSAccessibilityLevelIndicatorRole,
        NSAccessibilityMatteRole,
        NSAccessibilityMenuBarRole,
        NSAccessibilityMenuButtonRole,
        NSAccessibilityMenuItemRole,
        NSAccessibilityMenuRole,
        NSAccessibilityOutlineRole,
        NSAccessibilityPopUpButtonRole,
        NSAccessibilityProgressIndicatorRole,
        NSAccessibilityRadioButtonRole,
        NSAccessibilityRadioGroupRole,
        NSAccessibilityRelevanceIndicatorRole,
        NSAccessibilityRowRole,
        NSAccessibilityRulerMarkerRole,
        NSAccessibilityRulerRole,
        NSAccessibilityScrollAreaRole,
        NSAccessibilityScrollBarRole,
        NSAccessibilitySheetRole,
        NSAccessibilitySliderRole,
        //NSAccessibilitySortButtonRole, // Deprecated
        NSAccessibilitySplitGroupRole,
        NSAccessibilitySplitterRole,
        NSAccessibilityStaticTextRole,
        NSAccessibilitySystemWideRole,
        NSAccessibilityTabGroupRole,
        NSAccessibilityTableRole,
        NSAccessibilityTextAreaRole,
        NSAccessibilityTextFieldRole,
        NSAccessibilityToolbarRole,
        NSAccessibilityUnknownRole,
        NSAccessibilityValueIndicatorRole,
        NSAccessibilityWindowRole,
        nil] retain];
    
        attributes = [[NSArray arrayWithObjects:
        NSAccessibilityChildrenAttribute,
        NSAccessibilityEnabledAttribute,
        // NSAccessibilityFocusedAttribute,
        // NSAccessibilityFocusedUIElementAttribute,
        NSAccessibilityParentAttribute,
        NSAccessibilityPositionAttribute,
        NSAccessibilityRoleAttribute,
        NSAccessibilityRoleDescriptionAttribute,
        // NSAccessibilitySelectedChildrenAttribute,
        NSAccessibilitySizeAttribute,
        // NSAccessibilitySubroleAttribute,
        // NSAccessibilityTitleAttribute, // Accessible name
        NSAccessibilityTopLevelUIElementAttribute,
        // NSAccessibilityVisibleChildrenAttribute,
        NSAccessibilityWindowAttribute,
        nil] retain];
        
        settableAttribute = [[NSArray arrayWithObjects:
        NSAccessibilityFocusedUIElementAttribute,
        nil] retain];
    }
    
}

- (id)initWithEnv:(JNIEnv*)env root:(jobject)root {
    LOG("GlassAccessibleRoot:initWithEnv");
    self = [super init];
    if (self != nil) {
        jRoot = (*env)->NewGlobalRef(env, root);
    }
    return self;
}

// TODO: Is this needed?
- (void)dealloc {
    GET_MAIN_JENV;
    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, jRoot);
    }
    jRoot = NULL;
    [super dealloc];
}

//
// NSAccessibility protocol
//

//////////
// General
//////////

- (id)accessibilityHitTest:(NSPoint)point {
    LOG("GlassAccessibleRoot:accessibilityHitTest:point");
    // For Mac, the y is the bottom left corner
    // get the screen with keyboard focus
    NSScreen* mainScreen = [NSScreen mainScreen];
    // get the screen height
    NSRect screenRect = [mainScreen frame];
    CGFloat screenHeight = screenRect.size.height;
    
    double y = point.y;
    // flip the value of the y coordinate since OS X uses cartesian coordinates
    y = screenHeight - y;
    GET_MAIN_JENV;
    jlong ptr =
        (*env)->CallLongMethod(env, jRoot, midElementProviderFromPoint, point.x, y);
    GLASS_CHECK_EXCEPTION(env);
    if (ptr == 0) {
        LOG("  No provider at this point");
        return nil;
    } else {
        LOG("  Provider at this point: %p", jlong_to_ptr(ptr));
        GlassAccessibleBaseProvider *ptrGlass =
            (GlassAccessibleBaseProvider *)jlong_to_ptr(ptr);
        return ptrGlass;
    }
}

- (BOOL)accessibilityIsIgnored {
    LOG("GlassAccessibleRoot:accessibilityIsIgnored");
    return NO;
}

- (id)accessibilityFocusedUIElement {
    LOG("GlassAccessibleRoot:accessibilityFocusedUIElement");
    return nil;
}

/////////////
// Attributes
/////////////

- (NSArray *)accessibilityAttributeNames {
    LOG("GlassAccessibleRoot:accessibilityAttributeNames");
    return attributes;
}

- (id)accessibilityAttributeValue:(NSString *)attribute {
    LOG("GlassAccessibleRoot:accessibilityAttributeValue");
    LOG("  attribute: %s self: %p", [attribute UTF8String], self);
    GET_MAIN_JENV;
    
    ///// Children /////
    ///// Visible Chidren ///// TODO: Change later when we support visible vs non-visible
    if (([attribute isEqualToString:NSAccessibilityChildrenAttribute]) ||
        ([attribute isEqualToString:NSAccessibilityVisibleChildrenAttribute])) {
        return [self accessibilityArrayAttributeValues:attribute index:0 maxCount:-1];
        
    ///// Enabled /////
    } else if ([attribute isEqualToString:NSAccessibilityEnabledAttribute]) {
        return [NSNumber numberWithBool:NO];
        
    ///// Parent /////
    } else if ([attribute isEqualToString:NSAccessibilityParentAttribute]) {
        LOG("  returning: %p", parent);
        return parent;
        
    ///// Position /////
    } else if ([attribute isEqualToString:NSAccessibilityPositionAttribute]) {
        // get the screen with keyboard focus
        NSScreen* mainScreen = [NSScreen mainScreen];
        // get the screen height
        NSRect screenRect = [mainScreen frame];
        CGFloat screenHeight = screenRect.size.height;
        // get the rectangle
        jobject jRectangle = (*env)->CallObjectMethod(env, jRoot, midBoundingRectangle);
        GLASS_CHECK_EXCEPTION(env);
        if (jRectangle != NULL) {
            // get javafx.accessibility.Rect class
            jclass cls = (*env)->GetObjectClass(env, jRectangle);
            GLASS_CHECK_EXCEPTION(env);
            // init the method IDs
            jmethodID midGetX = (*env)->GetMethodID(env, cls, "getMinX", "()D");
            GLASS_CHECK_EXCEPTION(env);
            jmethodID midGetY = (*env)->GetMethodID(env, cls, "getMinY", "()D");
            GLASS_CHECK_EXCEPTION(env);
            jmethodID midGetHeight = (*env)->GetMethodID(env, cls, "getMaxY", "()D");
            // get values
            jdouble x = (*env)->CallDoubleMethod(env, jRectangle, midGetX);
            GLASS_CHECK_EXCEPTION(env);
            jdouble y = (*env)->CallDoubleMethod(env, jRectangle, midGetY);
            GLASS_CHECK_EXCEPTION(env);
            jdouble height = (*env)->CallDoubleMethod(env, jRectangle, midGetHeight);
            GLASS_CHECK_EXCEPTION(env);
            // For Mac, return y as the bottom left corner
            y = y + height;
            // flip the value of the y coordinate since OS X uses cartesian coordinates
            y = screenHeight + (y - (2.0 * y));
            LOG("  returning x: %f, y: %f", x, y);
            // return NSValue 
            return [NSValue valueWithPoint:NSMakePoint(x, y)];
        } else {
            return nil;
        }
        
    ///// Role /////
    } else if ([attribute isEqualToString:NSAccessibilityRoleAttribute]) {
        LOG("  returning: %s", [NSAccessibilityGroupRole UTF8String]);
        return NSAccessibilityGroupRole;
        
    ///// Role Description /////
    } else if ([attribute isEqualToString:NSAccessibilityRoleDescriptionAttribute]) {
        LOG( " returning: %s",
             [NSAccessibilityRoleDescription(NSAccessibilityGroupRole, nil) UTF8String] );
        return NSAccessibilityRoleDescription(NSAccessibilityGroupRole, nil);
        
    ///// Size /////
    } else if ([attribute isEqualToString:NSAccessibilitySizeAttribute]) {
        jobject jRectangle = (*env)->CallObjectMethod(env, jRoot, midBoundingRectangle);
        GLASS_CHECK_EXCEPTION(env);
        if (jRectangle != NULL) {
            // get javafx.accessibility.Rect class
            jclass cls = (*env)->GetObjectClass(env, jRectangle);
            GLASS_CHECK_EXCEPTION(env);
            jmethodID midGetWidth = (*env)->GetMethodID(env, cls, "getMaxX", "()D");
            GLASS_CHECK_EXCEPTION(env);
            jmethodID midGetHeight = (*env)->GetMethodID(env, cls, "getMaxY", "()D");
            GLASS_CHECK_EXCEPTION(env);
            // get values
            jdouble width = (*env)->CallDoubleMethod(env, jRectangle, midGetWidth);
            GLASS_CHECK_EXCEPTION(env);
            jdouble height = (*env)->CallDoubleMethod(env, jRectangle, midGetHeight);
            GLASS_CHECK_EXCEPTION(env);
            LOG("  returning width: %f, height: %f", width, height);
            // return NSValue
            return [NSValue valueWithSize:NSMakeSize(width, height)];
        } else {
            return nil;
        }
        
    ///// SubRole /////
    } else if ([attribute isEqualToString:NSAccessibilitySubroleAttribute]) {
        LOG("  returning: %s", [[NSAccessibilityUnknownSubrole description] UTF8String]);
        return NSAccessibilityUnknownSubrole;
        
    ///// Top Level UI Element /////
    } else if ([attribute isEqualToString:NSAccessibilityTopLevelUIElementAttribute]) {
        return parent;  // TODO: This may not be correct
        
    ///// Window /////
    } else if ([attribute isEqualToString:NSAccessibilityWindowAttribute]) {
        return parent;
        
    ///// Not Handled /////
    } else {
        LOG("  attribute not handled");
        return nil;
    }
}

- (BOOL)accessibilityIsAttributeSettable:(NSString *)attribute {
    LOG("GlassAccessibleRoot:accessibilityIsAttributeSettable");
    return NO;
}

- (NSArray *)accessibilityArrayAttributeValues:(NSString *)attribute
                                         index:(NSUInteger)index
                                      maxCount:(NSUInteger)maxCount {
    LOG("GlassAccessibleRoot:accessibilityArrayAttributeValues:index:maxCount:");
    LOG("  attribute: %s, self: %p", [attribute UTF8String], self);
    LOG("  index: %d, maxCount: %d", index, maxCount);
    if (maxCount == 0) {
        return [NSArray array]; // return empty array
    }
    GET_MAIN_JENV;
    
    ///// Children /////
    ///// Visible Chidren ///// PTB: Change later when we support visible vs non-visible
    if (([attribute isEqualToString:NSAccessibilityChildrenAttribute]) ||
        ([attribute isEqualToString:NSAccessibilityVisibleChildrenAttribute])) {
        jlongArray children =
            (*env)->CallObjectMethod(env, jRoot, midGetChildren, index, maxCount);
        GLASS_CHECK_EXCEPTION(env);
        if (children == NULL) {
            LOG("  no children, returning empty array");
            return [NSArray array];
        }
        jint size = (*env)->GetArrayLength(env, children);
        GLASS_CHECK_EXCEPTION(env);
        if (size != 0) {
            GlassAccessibleBaseProvider* providers[size];
            jlong* childElements = (*env)->GetLongArrayElements(env, children, JNI_FALSE);
            GLASS_CHECK_EXCEPTION(env);
            for (int i = 0; i < size; i++) {
                providers[i] =
                    (GlassAccessibleBaseProvider*)jlong_to_ptr(childElements[i]);
                LOG("  child: %p", providers[i]);
            }
            NSArray* macChildren = [[NSArray alloc] initWithObjects:providers count:size];
            (*env)->ReleaseLongArrayElements(env, children, childElements, JNI_FALSE);
            GLASS_CHECK_EXCEPTION(env);
            return macChildren;
        } else {
            LOG("  no children, returning empty array");
            return [NSArray array];
        }
        
    ///// Not Handled /////
    } else {
        LOG("  attribute not handled");
        return nil;
    }
}

//////////
// Actions
//////////

- (NSArray *)accessibilityActionNames {
    LOG("GlassAccessibleRoot:accessibilityActionNames");
    LOG("  returning empty array");
    return [NSArray array];
}

@end

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleRoot
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1initIDs(
    JNIEnv *env, jclass cls)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1initIDs");
    midGetPropertyValue  =
        (*env)->GetMethodID(env, cls, "getPropertyValue", "(I)Ljava/lang/Object;");
    GLASS_CHECK_EXCEPTION(env);
    midGetChildren = (*env)->GetMethodID(env, cls, "getChildren", "(II)[J");
    GLASS_CHECK_EXCEPTION(env);
    midBoundingRectangle =
        (*env)->GetMethodID( env, cls, "boundingRectangle",
                             "()Lcom/sun/javafx/accessible/utils/Rect;" );
    GLASS_CHECK_EXCEPTION(env);
    midElementProviderFromPoint =
        (*env)->GetMethodID(env, cls, "elementProviderFromPoint", "(DD)J");    
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleRoot
 * Method:    _setAccessibilityInitIsComplete
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1setAccessibilityInitIsComplete(
    JNIEnv *env, jobject jRoot, jlong jPtr, jlong jAcc)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1setAccessibilityInitIsComplete");
    LOG("  jAcc: %p", jAcc);
    LOG("  jPtr: %p", jPtr);

    // TODO: Are the threading macros needed, i.e. ASSERT, ENTER, EXIT?
    //       This was moved from GlassWindow.m where they were used.
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (jPtr != 0) {
            NSWindow* nsWindow = (NSWindow*)jlong_to_ptr(jPtr);
            GlassWindow* window = (GlassWindow*)[nsWindow delegate];
            [window setAccessibilityInitIsComplete:(GlassAccessibleRoot *)jlong_to_ptr(jAcc)];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleRoot
 * Method:    _createAccessible
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1createAccessible(
    JNIEnv *env, jobject jRoot)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1createAccessible");
    // PTB: Is the dealloc below in _destroyAccessible enough with respect to releasing
    // the following retain?
    GlassAccessibleRoot *acc =
        [[[GlassAccessibleRoot alloc] initWithEnv:env root:jRoot] retain];
    LOG("  returning: %p:", acc);
    return ptr_to_jlong(acc);
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleRoot
 * Method:    _destroyAccessible
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1destroyAccessible(
    JNIEnv *env, jobject jRoot, jlong acc)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1destroyAccessible");
    GlassAccessibleRoot* accessible = (GlassAccessibleRoot*)jlong_to_ptr(acc);
    LOG("  accessible: %p", accessible);
    if (accessible) {
        [accessible dealloc];
    }
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleRoot
 * Method:    _fireEvent
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1fireEvent(
    JNIEnv *env, jobject jRoot, jlong acc, jint jEventId)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleRoot__1fireEvent");
    GlassAccessibleRoot* accessible = (GlassAccessibleRoot*)jlong_to_ptr(acc);
    LOG("  acc: %p", accessible);
    LOG("  jeventID: %d", jEventId);
    LOG("  event: %s", [[eventIds objectAtIndex:jEventId] UTF8String]);
    NSAccessibilityPostNotification(accessible, [eventIds objectAtIndex:jEventId]);
}



