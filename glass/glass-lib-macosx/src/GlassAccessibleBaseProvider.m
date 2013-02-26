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

#import "GlassAccessibleBaseProvider.h"
#import "GlassAccessibleRoot.h"
#import "GlassMacros.h"
#import <AppKit/AppKit.h>
#import "com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider.h"
#import "GlassWindow.h"

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
static jmethodID midGetParent = 0;
static jmethodID midGetRoot = 0;
static jmethodID midBoundingRectangle = 0;
static jmethodID midSetFocus = 0;

// PTB: Probably should move this stuff into another file.
//
// Collections used to convert between integer IDs and NSAccessibility's NSString IDs.
// The enums on the Java side need to be kept in sync with these collections.
//
// PTB: Is there a better way to code the initialization of these collections?

@implementation GlassAccessibleBaseProvider

NSDictionary* attributeIds = nil;
NSArray* eventIds = nil;
NSArray* roleIds = nil;
NSMutableArray* attributes = nil;
NSArray* settableAttribute = nil ;

static inline NSString* getNSString(JNIEnv* env, jstring jstring)
{
    NSString *string = nil;
    if (jstring != NULL) {
        const jchar* jstrChars = (*env)->GetStringChars(env, jstring, NULL);
        string = [[[NSString alloc] initWithCharacters:jstrChars
                                    length:(*env)->GetStringLength(env, jstring)] autorelease];
        (*env)->ReleaseStringChars(env, jstring, jstrChars);
    }
    return string;
}

+ (void) initialize {
    if (self == [GlassAccessibleBaseProvider class]) {
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
        
        attributes = [[NSMutableArray arrayWithObjects:
        NSAccessibilityChildrenAttribute,
        NSAccessibilityEnabledAttribute,
        NSAccessibilityFocusedAttribute,
        // NSAccessibilityFocusedUIElementAttribute,
        NSAccessibilityParentAttribute,
        NSAccessibilityPositionAttribute,
        NSAccessibilityRoleAttribute,
        NSAccessibilityRoleDescriptionAttribute,
        NSAccessibilitySizeAttribute,
        // NSAccessibilitySubroleAttribute,
        NSAccessibilityTitleAttribute, // Accessible name
        NSAccessibilityTopLevelUIElementAttribute,
        NSAccessibilityWindowAttribute,
        NSAccessibilityEnabledAttribute,
        nil] retain];
        settableAttribute =
            [[NSArray arrayWithObjects:NSAccessibilityFocusedAttribute,
                      // NSAccessibilityFocusedUIElementAttribute,
                      nil] retain];
    }
    
}

- (id)initWithEnv:(JNIEnv*)env baseProvider:(jobject)baseProvider {
    LOG("GlassAccessibleBaseProvider:initWithEnv:baseProvider:");
    self = [super init];
    if (self != nil) {
        self->jBaseProvider = (*env)->NewGlobalRef(env, baseProvider);
    }
    return self;
}

// PTB: Is this needed?
- (void)dealloc {
    GET_MAIN_JENV;
    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, self->jBaseProvider);
    }
    self->jBaseProvider = NULL;
    [super dealloc];
}

//
// NSAccessibility protocol
//

//////////
// General
//////////

- (id)accessibilityHitTest:(NSPoint)point {
    LOG("GlassAccessibleBaseProvider:accessibilityHitTest:");
    return self;
   // return NSAccessibilityUnignoredAncestor(self);
}

- (BOOL)accessibilityIsIgnored {
    LOG("GlassAccessibleBaseProvider:accessibilityIsIgnored");
    return NO;
}

- (id)accessibilityFocusedUIElement {
    LOG("GlassAccessibleBaseProvider:accessibilityFocusedUIElement");
    return nil;
}

- (void)setAttributeValue:(int)value {
    LOG("GlassAccessibleBaseProvider:setAttributeValue:");
    LOG("  Not implemented");
}

- (BOOL)accessibilityIsAttributeSettable:(NSString *)attribute
{
    LOG("GlassAccessibleBaseProvider:accessibilityIsAttributeSettable:");
    LOG("  attribute: %s, self: %p", [attribute UTF8String], self);
    // Determine if the attribute being asked about is settable.
    if ([settableAttribute containsObject:attribute]) {
        LOG("  attribute: %s is settable",[attribute UTF8String]);
        return YES;
    } else {
        LOG("  attribute: %s is NOT settable",[attribute UTF8String]);
        return NO;
    }
}

- (void)accessibilitySetValue:(id)value forAttribute:(NSString *)attribute
{
    LOG("GlassAccessibleBaseProvider:accessibilitySetValue:");
    LOG("  attribute: %s, self: %p", [attribute UTF8String], self);
    // Determine if the attribute being asked about is settable
    if ([settableAttribute containsObject:attribute]) {
        // Call the Java method to set the attribute’s value.
        if ([attribute isEqualToString:NSAccessibilityFocusedAttribute]) {
            GET_MAIN_JENV;
            (*env)->CallVoidMethod(env, jBaseProvider, midSetFocus);
            GLASS_CHECK_EXCEPTION(env);
        }
    }
}

/////////////
// Attributes
/////////////

- (NSArray *)accessibilityAttributeNames {
    // LOG("GlassAccessibleBaseProvider:accessibilityAttributeNames");
    return attributes;
}

- (id)accessibilityAttributeValue:(NSString *)attribute {
    LOG("GlassAccessibleBaseProvider:accessibilityAttributeValue:");
    LOG("  attribute: %s, self: %p", [attribute UTF8String], self);
    GET_MAIN_JENV;
    
    ///// Booleans /////
    if ( [attribute isEqualToString:NSAccessibilityFocusedAttribute] ||
         [attribute isEqualToString:NSAccessibilityEnabledAttribute] ) {
         //[attribute isEqualToString:NSAccessibilityFocusedUIElementAttribute] ) {
        jint attributeId = [[attributeIds valueForKey:attribute] intValue];
        // upcall to get Focused state and return it
        jobject javaBoolean =
            (*env)->CallObjectMethod(env, jBaseProvider, midGetPropertyValue, attributeId);
        GLASS_CHECK_EXCEPTION(env);
        if (javaBoolean != NULL) {
            // get java.lang.Boolean class
            jclass cls = (*env)->GetObjectClass(env, javaBoolean);
            GLASS_CHECK_EXCEPTION(env);
            if (cls != NULL) {
                jmethodID midBooleanValue =
                    (*env)->GetMethodID(env, cls, "booleanValue", "()Z");
                GLASS_CHECK_EXCEPTION(env);
                if (midBooleanValue != NULL) {
                    // get value
                    jboolean value =
                        (*env)->CallBooleanMethod(env, javaBoolean, midBooleanValue);
                    GLASS_CHECK_EXCEPTION(env);
                    BOOL focused;
                    if (value == JNI_TRUE) {
                        focused = YES;
                        LOG("  returning YES");
                    } else {
                        focused = NO;
                        LOG("  returning NO");
                    }
                    return [NSNumber numberWithBool: focused];
                }
            }
        }
        LOG("  returning nil");
        return nil;
    
    ///// Children /////
    ///// Visible Chidren ///// PTB: Change later when we support visible vs non-visible
    } else if ( ([attribute isEqualToString:NSAccessibilityChildrenAttribute]) ||
                ([attribute isEqualToString:NSAccessibilityVisibleChildrenAttribute]) ) {
        return [self accessibilityArrayAttributeValues:attribute index:0 maxCount:-1];
        
    ///// Parent /////
    } else if ([attribute isEqualToString:NSAccessibilityParentAttribute]) {
        // PTB: Is there a better idea about dealing with root vs base?
        id ptr = nil;
        jlong root = 0;
        jlong parent = (*env)->CallLongMethod(env, jBaseProvider, midGetParent);
        if (parent == -1) {
            root = (*env)->CallLongMethod(env, jBaseProvider, midGetRoot);
            if (root != 0) {
                ptr = (GlassAccessibleRoot*)jlong_to_ptr(root);
            }
        } else if (parent != 0) {
            ptr = (GlassAccessibleBaseProvider*)jlong_to_ptr(parent);
        }
        LOG("  returning %p", ptr);
        return ptr;
        
    ///// Position /////
    } else if ([attribute isEqualToString:NSAccessibilityPositionAttribute]) {
        // get the screen with keyboard focus
        NSScreen* mainScreen = [NSScreen mainScreen];
        // get the screen height
        NSRect screenRect = [mainScreen frame];
        CGFloat screenHeight = screenRect.size.height;
        // get the rectangle
        jobject jRectangle = (*env)->CallObjectMethod(env, jBaseProvider, midBoundingRectangle);
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
        
    ///// Role and Role Description /////
    } else if ( [attribute isEqualToString:NSAccessibilityRoleAttribute] ||
                [attribute isEqualToString:NSAccessibilityRoleDescriptionAttribute] ) {
        jint attributeId = [[attributeIds valueForKey:NSAccessibilityRoleAttribute] intValue];
        // upcall to get role and return it
        jobject javaInteger =
            (*env)->CallObjectMethod(env, jBaseProvider, midGetPropertyValue, attributeId);
        GLASS_CHECK_EXCEPTION(env);
        if (javaInteger != NULL) {
            // get java.lang.Integer class
            jclass cls = (*env)->GetObjectClass(env, javaInteger);
            GLASS_CHECK_EXCEPTION(env);
            // get method ID
            jmethodID jIntValue = (*env)->GetMethodID(env, cls, "intValue", "()I");
            GLASS_CHECK_EXCEPTION(env);
            if (jIntValue == NULL) {
                LOG("  returning nil");
                return nil;
            } else {
                // get value
                jint role = (*env)->CallIntMethod(env, javaInteger, jIntValue);
                GLASS_CHECK_EXCEPTION(env);
                if ([attribute isEqualToString:NSAccessibilityRoleDescriptionAttribute]) {
                    LOG( "  returning: %s",
                         [NSAccessibilityRoleDescription([roleIds objectAtIndex:role], nil) UTF8String] );
                    return NSAccessibilityRoleDescription([roleIds objectAtIndex:role], nil);
                } else {
                    LOG("  returning: %s", [[roleIds objectAtIndex:role] UTF8String]);
                    return [roleIds objectAtIndex:role];
                }
            }
        } else {
            LOG("  returning nil");
            return nil;
        }
    
    ///// Size /////
    } else if ([attribute isEqualToString:NSAccessibilitySizeAttribute]) {
        jobject jRectangle = (*env)->CallObjectMethod(env, jBaseProvider, midBoundingRectangle);
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
    
    ///// Title /////
    } else if ([attribute isEqualToString:NSAccessibilityTitleAttribute]) {
        jint attributeId = [[attributeIds valueForKey:attribute] intValue];
        // upcall to get Title and return it
        jstring javaString =
            (*env)->CallObjectMethod(env, jBaseProvider, midGetPropertyValue, attributeId);
        GLASS_CHECK_EXCEPTION(env);
        if (javaString != NULL) {
            NSString *strTitle = getNSString(env, javaString);
            LOG("  returning: %s", [strTitle UTF8String]);
            return strTitle;
        } else {
            LOG("  returning nil");
            return nil;
        }
        
    ///// Window /////
    // PTB: This returns the same thing for Top Level UI Element which may or may not be right
    } else if ( [attribute isEqualToString:NSAccessibilityWindowAttribute] ||
                [attribute isEqualToString:NSAccessibilityTopLevelUIElementAttribute] ) {
        // walk up the tree to the root and get its parent
        GlassAccessibleRoot *root = nil;
        GlassAccessibleBaseProvider *current = nil;
        jlong rootRef = 0;
        jlong parentRef = 0;
        jobject jCurrent = jBaseProvider;
        // find the root
        for (;;) {
            parentRef = (*env)->CallLongMethod(env, jCurrent, midGetParent);
            if (parentRef == 0) {
                return nil;
            } else if (parentRef == -1) {
                rootRef = (*env)->CallLongMethod(env, jCurrent, midGetRoot);
                if (rootRef == 0) {
                    return nil;
                }
                root = (GlassAccessibleRoot*)jlong_to_ptr(rootRef);
                LOG("  returning root's parent: %p", root->parent);
                return root->parent;
            } else {
                current = (GlassAccessibleBaseProvider*)jlong_to_ptr(parentRef);
                jCurrent = current->jBaseProvider;
                LOG("  looping; current: %p, jCurrent: %p", current, jCurrent);
            }
        }
      
    ///// Not Handled /////
    } else {
        LOG("  attribute not handled");
        return nil;
    }
}

- (NSArray *)accessibilityArrayAttributeValues:(NSString *)attribute
                                         index:(NSUInteger)index
                                      maxCount:(NSUInteger)maxCount {
    LOG("GlassAccessibleBaseProvider:accessibilityArrayAttributeValues:index:maxCount:");
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
            (*env)->CallObjectMethod(env, jBaseProvider, midGetChildren, index, maxCount);
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
    LOG("GlassAccessibleBaseProvider:accessibilityActionNames");
    LOG("  returning empty array");
    return [NSArray array];
}

@end

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1initIDs(JNIEnv *env, jclass cls)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1initIDs");
    midGetPropertyValue  =
        (*env)->GetMethodID(env, cls, "getPropertyValue", "(I)Ljava/lang/Object;");
    GLASS_CHECK_EXCEPTION(env);
    midGetChildren = (*env)->GetMethodID(env, cls, "getChildren", "(II)[J");
    GLASS_CHECK_EXCEPTION(env);
    midGetParent = (*env)->GetMethodID(env, cls, "getParent", "()J");
    GLASS_CHECK_EXCEPTION(env);
    midGetRoot = (*env)->GetMethodID(env, cls, "getRoot", "()J");
    GLASS_CHECK_EXCEPTION(env);
    midBoundingRectangle =
        (*env)->GetMethodID( env, cls, "boundingRectangle",
                             "()Lcom/sun/javafx/accessible/utils/Rect;" );
    GLASS_CHECK_EXCEPTION(env);
    midSetFocus = (*env)->GetMethodID(env, cls, "setFocus", "()V");
    GLASS_CHECK_EXCEPTION(env);
    
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider
 * Method:    _createAccessible
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1createAccessible(
    JNIEnv *env, jobject jBaseProvider)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1createAccessible");
    GlassAccessibleBaseProvider *acc =
        [[[GlassAccessibleBaseProvider alloc] initWithEnv:env baseProvider:jBaseProvider] retain];
    LOG("  returning: %p:", acc);
    return ptr_to_jlong(acc);
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider
 * Method:    _destroyAccessible
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1destroyAccessible(
    JNIEnv *env, jobject jBaseProvider, jlong acc)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1destroyAccessible");
    GlassAccessibleBaseProvider* accessible = (GlassAccessibleBaseProvider*)jlong_to_ptr(acc);
    LOG("  accessible: %p", accessible);
    if (accessible) {
        [accessible dealloc];
    }
}

/*
 * Class:     com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider
 * Method:    _fireEvent
 * Signature: (JI)V
 */
JNIEXPORT void JNICALL
Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1fireEvent(
    JNIEnv *env, jobject jBaseProvider, jlong acc, jint jEventId)
{
    LOG("Java_com_sun_glass_ui_accessible_mac_MacAccessibleBaseProvider__1fireEvent");
    GlassAccessibleBaseProvider* accessible = (GlassAccessibleBaseProvider*)jlong_to_ptr(acc);
    LOG("  acc: %p", accessible);
    LOG("  jeventID: %d", jEventId);
    // Use Window to fire this event as NSObject is not observable, as VO cannot observe it
    // It cannot see a change to it.
    GlassWindow *window =
        (GlassWindow*)[accessible accessibilityAttributeValue:NSAccessibilityWindowAttribute];
    if (window) {
        NSString *strEvent = [eventIds objectAtIndex:jEventId];
        LOG("  Posting %s to window: %p", [strEvent UTF8String], window);
        [window accessibilityPostEvent:strEvent focusElement:accessible];
    } else {
        LOG("  Not posting event, window is nil");
    }
}

