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
     * Here we should keep all the mapping between the accessibility roles and implementing classes
     */
    rolesMap = [[NSMutableDictionary alloc] initWithCapacity:4];

    [rolesMap setObject:@"ButtonAccessibility" forKey:@"BUTTON"];
    [rolesMap setObject:@"ButtonAccessibility" forKey:@"DECREMENT_BUTTON"];
    [rolesMap setObject:@"ButtonAccessibility" forKey:@"INCREMENT_BUTTON"];
    [rolesMap setObject:@"ButtonAccessibility" forKey:@"SPLIT_MENU_BUTTON"];

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

- (id)accessibilityValue
{
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue, (jlong)@"AXValue");
    GLASS_CHECK_EXCEPTION(env);
    return variantToID(env, jresult);
}

- (NSString *)accessibilityLabel
{
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NULL;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue, (jlong)@"AXTitle");
    GLASS_CHECK_EXCEPTION(env);
    return variantToID(env, jresult);
}

- (id)accessibilityParent
{
    if (parent == nil) {
        jobject jresult = NULL;
        GET_MAIN_JENV;
        if (env == NULL) return NULL;
        jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue,
                                                  (jlong) @"AXParent");
        GLASS_CHECK_EXCEPTION(env);
        parent = variantToID(env, jresult);
    }
    return parent;
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
    jobject jresult = NULL;
    GET_MAIN_JENV;
    if (env == NULL) return NSZeroRect;
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue, (jlong)@"AXPosition");
    GLASS_CHECK_EXCEPTION(env);
    NSPoint position = [variantToID(env, jresult) pointValue];
    jresult = (jobject)(*env)->CallLongMethod(env, self->jAccessible, jAccessibilityAttributeValue, (jlong)@"AXSize");
    GLASS_CHECK_EXCEPTION(env);
    NSSize size = [variantToID(env, jresult) sizeValue];
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
    if ([accessible respondsToSelector:@selector(clearParent)]) {
        [accessible clearParent];
    }
}
