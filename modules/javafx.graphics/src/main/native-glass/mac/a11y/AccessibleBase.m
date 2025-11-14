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

#import "AccessibleBase.h"
#import "GlassMacros.h"
#import "GlassAccessible.h"
#import "com_sun_glass_ui_mac_MacAccessible.h"
#import "com_sun_glass_ui_mac_MacVariant.h"
#import "common.h"

static NSMutableDictionary * rolesMap;

@implementation AccessibleBase

+ (void) initializeRolesMap {
    /*
     * Here we should keep all the mapping between the accessibility roles and implementing classes.
     * All JavaFX roles and corresponding available properties are defined in
     * enum javafx.scene.AccessibleRole
     */
    rolesMap = [[NSMutableDictionary alloc] initWithCapacity:22];

    [rolesMap setObject:@"JFXButtonAccessibility" forKey:@"BUTTON"];
    [rolesMap setObject:@"JFXButtonAccessibility" forKey:@"DECREMENT_BUTTON"];
    [rolesMap setObject:@"JFXButtonAccessibility" forKey:@"INCREMENT_BUTTON"];
    [rolesMap setObject:@"JFXButtonAccessibility" forKey:@"SPLIT_MENU_BUTTON"];
    [rolesMap setObject:@"JFXRadiobuttonAccessibility" forKey:@"RADIO_BUTTON"];
    [rolesMap setObject:@"JFXRadiobuttonAccessibility" forKey:@"TAB_ITEM"];
    [rolesMap setObject:@"JFXRadiobuttonAccessibility" forKey:@"PAGE_ITEM"];
    [rolesMap setObject:@"JFXCheckboxAccessibility" forKey:@"CHECK_BOX"];
    [rolesMap setObject:@"JFXCheckboxAccessibility" forKey:@"TOGGLE_BUTTON"];
    [rolesMap setObject:@"JFXStaticTextAccessibility" forKey:@"TEXT"];
    [rolesMap setObject:@"JFXStepperAccessibility" forKey:@"SPINNER"];
    [rolesMap setObject:@"JFXSliderAccessibility" forKey:@"SLIDER"];
    [rolesMap setObject:@"JFXProgressIndicatorAccessibility" forKey:@"PROGRESS_INDICATOR"];
    [rolesMap setObject:@"JFXImageAccessibility" forKey:@"IMAGE"];
    [rolesMap setObject:@"JFXImageAccessibility" forKey:@"IMAGE_VIEW"];
    [rolesMap setObject:@"JFXTabGroupAccessibility" forKey:@"TAB_PANE"];
    [rolesMap setObject:@"JFXTabGroupAccessibility" forKey:@"PAGINATION"];
    [rolesMap setObject:@"JFXMenuItemAccessibility" forKey:@"MENU"];
    [rolesMap setObject:@"JFXMenuItemAccessibility" forKey:@"MENU_ITEM"];
    [rolesMap setObject:@"JFXMenuItemAccessibility" forKey:@"RADIO_MENU_ITEM"];
    [rolesMap setObject:@"JFXMenuItemAccessibility" forKey:@"CHECK_MENU_ITEM"];
    [rolesMap setObject:@"JFXMenuBarAccessibility" forKey:@"MENU_BAR"];

}

+ (Class) getComponentAccessibilityClass:(NSString *)role
{
    if (rolesMap == nil) {
        [self initializeRolesMap];
    }

    NSString *className = [rolesMap objectForKey:role];
    if (className != nil) {
        return NSClassFromString(className);
    }
    return [GlassAccessible class];
}

- (id)initWithEnv:(JNIEnv*)env accessible:(jobject)acc
{
    self = [super init];
    if (self != nil) {
        self->jAccessible = (*env)->NewGlobalRef(env, acc);
    }
    self->parent = nil;
    return self;
}

- (void)dealloc
{
    GET_MAIN_JENV_NOWARN;
    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, jAccessible);
        GLASS_CHECK_EXCEPTION(env);
    }
    jAccessible = NULL;
    [super dealloc];
}

- (jobject)getJAccessible
{
    return self->jAccessible;
}

/*
 * Request accessibility attribute by name from JavaFX Node. Returns attribute value
 * converted to the native format or NULL if attribute with that name does not exist.
 * Code that uses this function needs to convert NULL to the default value of a certain type where required.
 */
- (id)requestNodeAttribute:(NSString *)attribute
{
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jobject jresult = (jobject)(*env)->CallLongMethod(env, [self getJAccessible],
                                              jAccessibilityAttributeValue, (jlong)attribute);
    GLASS_CHECK_EXCEPTION(env);
    return variantToID(env, jresult);
}

- (id)accessibilityValue
{
    return [self requestNodeAttribute:@"AXValue"];
}

- (id)accessibilityMinValue
{
    return [self requestNodeAttribute:@"AXMinValue"];
}

- (id)accessibilityMaxValue
{
    return [self requestNodeAttribute:@"AXMaxValue"];
}

- (NSString *)accessibilityLabel
{
    // Use the same value that is set for accessibilityTitle - some components
    // do not have titles and request it as a label
    return [self accessibilityTitle];
}

- (id)accessibilityParent
{
    if (parent == nil) {
        parent = [self requestNodeAttribute:@"AXParent"];
    }
    return parent;
}

- (id)accessibilityTitle
{
    return [self requestNodeAttribute:@"AXTitle"];
}

- (id)accessibilityTitleUIElement
{
    return [self requestNodeAttribute:@"AXTitleUIElement"];
}

- (NSArray *)accessibilityChildren
{
    return [self requestNodeAttribute:@"AXChildren"];
}

- (id)accessibilityRoleDescription
{
    return [self requestNodeAttribute:@"AXRoleDescription"];
}


// Actions support
- (BOOL)performAccessibleAction:(NSString *)action
{
    GET_MAIN_JENV;
    if (env != NULL) {
        BOOL result = TRUE;
        (*env)->CallVoidMethod(env, self->jAccessible, jAccessibilityPerformAction, (jlong)action);
        if ((*env)->ExceptionCheck(env)) {
            result = FALSE;
        }
        GLASS_CHECK_EXCEPTION(env);
        return result;
    }
    return FALSE;
}


- (NSRect)accessibilityFrame
{
    id p = [self requestNodeAttribute:@"AXPosition"];
    id s = [self requestNodeAttribute:@"AXSize"];
    if (p == NULL || s == NULL) {
        return NSZeroRect;
    }
    NSPoint position = [p pointValue];
    NSSize size = [s sizeValue];
    return NSMakeRect(position.x, position.y, size.width, size.height);
}


- (BOOL)isAccessibilityElement
{
    return YES;
}

- (void)clearParent
{
    parent = nil;
}

- (BOOL)isAccessibilityFocused
{
    id retval = [self requestNodeAttribute:@"AXFocused"];
    if (retval == NULL) {
        return NO;
    } else {
        return [retval boolValue];
    }
}

- (void)setAccessibilityFocused:(BOOL)value
{
    GET_MAIN_JENV;
    if (env == NULL) return;
    (*env)->CallVoidMethod(env, self->jAccessible, jAccessibilitySetValue,
                           (jlong)[NSNumber numberWithBool:value],
                           (jlong)@"AXFocused");
    GLASS_CHECK_EXCEPTION(env);
}

@end

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _createAccessiblePeer
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1createAccessiblePeer
        (JNIEnv *env, jobject jAccessible, jstring forRole)
{
    NSString *roleName = jStringToNSString(env, forRole);
    Class classType = [AccessibleBase getComponentAccessibilityClass:roleName];
    NSObject* accessible = NULL;
    accessible = [[classType alloc] initWithEnv: env accessible: jAccessible];
    return ptr_to_jlong(accessible);
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _destroyAccessiblePeer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1destroyAccessiblePeer
(JNIEnv *env, jobject jAccessible, jlong macAccessible)
{
    NSObject* accessible = (NSObject*)jlong_to_ptr(macAccessible);
    if ([accessible respondsToSelector:@selector(release)]) {
        [accessible release];
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    NSAccessibilityUnignoredAncestor
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacAccessible_NSAccessibilityUnignoredAncestor
        (JNIEnv *env, jclass jClass, jlong macAccessible)
{
    NSObject* accessible = (NSObject*)jlong_to_ptr(macAccessible);
    return ptr_to_jlong(NSAccessibilityUnignoredAncestor(accessible));
}

/*
 * Class:     com_sun_glass_ui_mac_MacAccessible
 * Method:    _invalidateParent
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacAccessible__1invalidateParent
(JNIEnv *env, jobject jAccessible, long macAccessible)
{
    NSObject* accessible = (NSObject*)jlong_to_ptr(macAccessible);
    if ([accessible isKindOfClass: [AccessibleBase class]]) {
        [((AccessibleBase*) accessible) clearParent];
    }
}
