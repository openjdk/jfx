/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import "common.h"
#import "com_sun_glass_events_KeyEvent.h"
#import "com_sun_glass_ui_mac_MacMenuBarDelegate.h"
#import "com_sun_glass_ui_mac_MacMenuDelegate.h"

#import "GlassMacros.h"
#import "GlassMenu.h"
#import "GlassHelper.h"
#import "GlassKey.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static jmethodID jMenuActionMethod = 0;
static jmethodID jMenuOpeningMethod = 0;
static jmethodID jMenuClosedMethod = 0;
static jmethodID jMenuValidateMethod = 0;
static jfieldID  jDelegateMenuField = 0;

@interface NSMenuItem (SPI)

// Apple's SPI
- setAppleMenu:(NSMenuItem*)item;

@end

@implementation GlassMenubar

- (id)init
{
    self = [super init];
    if (self != nil)
    {
        self->menu = [[NSMenu allocWithZone:NSDefaultMallocZone()] initWithTitle:@"Menubar"];
    }
    return self;
}

- (void)dealloc
{
    LOG("GlassMenubar dealloc: %p", self);
    [super dealloc];
}

@end

@implementation GlassMenu

- (id)initWithJavajdelegate:(jobject)jdelegate jtitle:(jstring)jtitle jenabled:(jboolean)jenabled
{
    self = [super init];
    if (self != nil)
    {
        GET_MAIN_JENV;
        self->jDelegate = (*env)->NewGlobalRef(env, jdelegate);
        NSString *title = [GlassHelper nsStringWithJavaString:jtitle withEnv:env];
        LOG("initWithJavajdelegate: jdelegate %p jtitle %s",
            jdelegate, [title UTF8String]);

        self->item = [[NSMenuItem alloc] initWithTitle:title
                                                action:NULL
                                         keyEquivalent:@""];
        [self->item setEnabled:(BOOL)jenabled];
        [self->item setTarget:self];

        self->menu = [[NSMenu alloc] initWithTitle:[self->item title]];
        [self->menu setDelegate: self];
    }
    return self;
}

- (id)initWithJavajdelegate:(jobject)jdelegate jtitle:(jstring)jtitle
                  jshortcut:(jchar)jshortcut jmodifiers:(int)jmodifiers jicon:(jobject)jicon
                   jenabled:(jboolean)jenabled jchecked:(jboolean)jchecked jcallback:(jobject)jcallback;
{
    self = [super init];
    if (self != nil)
    {
        GET_MAIN_JENV;
        self->jDelegate = (*env)->NewGlobalRef(env, jdelegate);
        if (jcallback != NULL)
        {
            self->jCallback = (*env)->NewGlobalRef(env, jcallback);
        }

        NSString *shortcut = @"";
        NSString *title = [GlassHelper nsStringWithJavaString:jtitle withEnv:env];
        LOG("initWithJavajdelegate: jdelegate %p jcallback %p jtitle %s",
            jdelegate, jcallback, [title UTF8String]);

        self->item = [[NSMenuItem alloc] initWithTitle:title
                                                action:@selector(action:)
                                         keyEquivalent:shortcut];
        if (jshortcut != '\0')
        {
            [self _setShortcut:jshortcut modifiers:jmodifiers];
        }
        [self->item setEnabled:(BOOL)jenabled];
        [self _setChecked:(BOOL)jchecked];

        if (jicon != NULL) {
            [self _setPixels:jicon];
        }
        [self->item setTarget:self];
    }
    return self;
}

- (void)dealloc
{
    GET_MAIN_JENV_NOWARN;

    if (env != NULL)
    {
            if (self->jDelegate != NULL)
        {
            (*env)->DeleteGlobalRef(env, self->jDelegate);
            self->jDelegate = NULL;
        }
            if (self->jCallback != NULL)
        {
            (*env)->DeleteGlobalRef(env, self->jCallback);
            self->jCallback = NULL;
        }
    }

    [self->item release];

    [super dealloc];
}

- (void)action:(id)sender
{
    if (self->jCallback != NULL)
    {
        GET_MAIN_JENV;
        if (env != NULL)
        {
            (*env)->CallVoidMethod(env, self->jCallback, jMenuActionMethod, NULL);
        }
    }
}

// RT-37304: do not use menuNeedsUpdate here, even though Cocoa prohibits
// changing the menu structure during menuWillOpen...
- (void)menuWillOpen: (NSMenu *)menu
{
    GET_MAIN_JENV;
    if (env != NULL)
    {
        jobject jmenu = (*env)->GetObjectField(env, jDelegate, jDelegateMenuField);
        (*env)->CallVoidMethod(env, jmenu, jMenuOpeningMethod, NULL);
    }
}

- (void)menuDidClose: (NSMenu *)menu
{
    GET_MAIN_JENV;
    if (env != NULL)
    {
        jobject jmenu = (*env)->GetObjectField(env, jDelegate, jDelegateMenuField);
        (*env)->CallVoidMethod(env, jmenu, jMenuClosedMethod, NULL);
    }
}

#pragma mark NSMenuValidation

- (BOOL)validateMenuItem:(NSMenuItem *)menuItem
{
    LOG("validateMenuItem: %s action: %p", [[menuItem title] UTF8String], [menuItem action]);
    GET_MAIN_JENV;
    if (env != NULL)
    {
        GlassMenu *glassTargetItem = (GlassMenu *)[menuItem target];
        (*env)->CallVoidMethod(env, self->jCallback, jMenuValidateMethod, NULL);

        return ([glassTargetItem->item isEnabled]);
    }
    return YES;
}

- (void)_setShortcut:(jchar)jshortcut modifiers:(jint)jmodifiers
{
    NSString *shortcut = GetStringForJavaKey(jshortcut);
    LOG("_setShortcut %c -> %s", jshortcut, [shortcut UTF8String]);

    NSUInteger modifier = 0;
    if ((jmodifiers & com_sun_glass_events_KeyEvent_MODIFIER_COMMAND) != 0)
    {
        modifier = modifier | NSCommandKeyMask;
    }
    if ((jmodifiers & com_sun_glass_events_KeyEvent_MODIFIER_SHIFT) != 0)
    {
        modifier = modifier | NSShiftKeyMask;
    }
    if ((jmodifiers & com_sun_glass_events_KeyEvent_MODIFIER_CONTROL) != 0)
    {
        modifier = modifier | NSControlKeyMask;
    }
    if ((jmodifiers & com_sun_glass_events_KeyEvent_MODIFIER_OPTION) != 0)
    {
        modifier = modifier | NSAlternateKeyMask;
    }
    if ((jmodifiers & com_sun_glass_events_KeyEvent_MODIFIER_FUNCTION) != 0)
    {
        modifier = modifier | NSFunctionKeyMask;
        if (jshortcut >= com_sun_glass_events_KeyEvent_VK_F1 &&
            jshortcut <= com_sun_glass_events_KeyEvent_VK_F12) {
            int delta = jshortcut - com_sun_glass_events_KeyEvent_VK_F1;
            shortcut = [NSString stringWithFormat:@"%C", (unsigned short)(NSF1FunctionKey + delta)];
        } else if (jshortcut >= com_sun_glass_events_KeyEvent_VK_F13 &&
                   jshortcut <= com_sun_glass_events_KeyEvent_VK_F24) {
            int delta = jshortcut - com_sun_glass_events_KeyEvent_VK_F13;
            shortcut = [NSString stringWithFormat:@"%C", (unsigned short)(NSF13FunctionKey + delta)];
        }
    }
    [self->item setKeyEquivalent:shortcut];
    [self->item setKeyEquivalentModifierMask:modifier];
}

- (void)_setChecked:(BOOL)checked
{
    [self->item setState:(checked ? NSOnState : NSOffState)];
}

- (void)_setPixels:(jobject)pixels
{
    GET_MAIN_JENV;
    if (pixels != NULL)
    {
        pixels = (*env)->NewGlobalRef(env, pixels);
        NSImage *image = NULL;
        (*env)->CallVoidMethod(env, pixels, jPixelsAttachData, ptr_to_jlong(&image));
        if (image != NULL)
        {
            [self->item setImage: image];
            [image release];
        }
        (*env)->DeleteGlobalRef(env, pixels);
    }
    else
    {
        [self->item setImage: nil];
    }
}

@end

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacMenuBarDelegate
 * Method:    _createMenuBar
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar
(JNIEnv *env, jobject jMenuBarDelegate)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar");

    jlong value = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenubar *menubar = [[GlassMenubar alloc] init];
        value = ptr_to_jlong(menubar);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return value;
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuBarDelegate
 * Method:    _insert
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1insert
(JNIEnv *env, jobject jMenuDelegate, jlong jMenubarPtr, jlong jMenuPtr, jint jPos)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1insert");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenubar *menubar = (GlassMenubar *)jlong_to_ptr(jMenubarPtr);
        GlassMenu *glassmenu = (GlassMenu *)jlong_to_ptr(jMenuPtr);

        [menubar->menu insertItem:glassmenu->item atIndex:jPos];
        [menubar->menu setSubmenu:glassmenu->menu forItem:glassmenu->item];

        [glassmenu->menu setAutoenablesItems:YES];

        if ([[glassmenu->item title] compare:@"Apple"] == NSOrderedSame)
        {
            LOG("calling setAppleMenu");
            [NSApp performSelector:@selector(setAppleMenu:) withObject:glassmenu->item];
        }

        [[NSApp mainMenu] update];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuBarDelegate
 * Method:    _remove
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1remove
(JNIEnv *env, jobject jMenuDelegate, jlong jMenubarPtr, jlong jMenuPtr, jint jPos)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1remove del %p mb %p mp %p pos %d",
        jMenuDelegate, jMenubarPtr, jMenuPtr, jPos);

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenubar *menubar = (GlassMenubar *)jlong_to_ptr(jMenubarPtr);
        GlassMenu *glassmenu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        if ([menubar->menu indexOfItem: glassmenu->item] != -1) {
            [menubar->menu removeItem:glassmenu->item];
        }
        [[NSApp mainMenu] update];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1initIDs
(JNIEnv *env, jclass jMenuDelegateClass)
{
    jclass jCallbackClass = [GlassHelper ClassForName:"com.sun.glass.ui.MenuItem$Callback" withEnv:env];
    if (!jCallbackClass) {
        return;
    }
    jclass jMenuClass = [GlassHelper ClassForName:"com.sun.glass.ui.Menu" withEnv:env];
    if (!jMenuClass) {
        return;
    }

    jMenuActionMethod  = (*env)->GetMethodID(env, jCallbackClass,   "action",  "()V");
    if ((*env)->ExceptionCheck(env)) return;
    jMenuValidateMethod = (*env)->GetMethodID(env, jCallbackClass,   "validate",  "()V");
    if ((*env)->ExceptionCheck(env)) return;
    jMenuOpeningMethod = (*env)->GetMethodID(env, jMenuClass, "notifyMenuOpening", "()V");
    if ((*env)->ExceptionCheck(env)) return;
    jMenuClosedMethod  = (*env)->GetMethodID(env, jMenuClass, "notifyMenuClosed",  "()V");
    if ((*env)->ExceptionCheck(env)) return;
    jDelegateMenuField = (*env)->GetFieldID(env,  jMenuDelegateClass, "menu", "Lcom/sun/glass/ui/Menu;");
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _createMenu
 * Signature: (Ljava/lang/String;Z)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu
(JNIEnv *env, jobject jMenuDelegate, jstring jTitle, jboolean jEnabled)
{
    jlong value = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = [[GlassMenu alloc] initWithJavajdelegate:jMenuDelegate
                                                            jtitle:jTitle
                                                          jenabled:jEnabled];
        value = ptr_to_jlong(menu);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    NSString* title = [GlassHelper nsStringWithJavaString:jTitle withEnv:env];
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu md %p title %s --> jMenuPtr %p ",
        jMenuDelegate, [title UTF8String], value);

    return value;
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _createMenuItem
 * Signature: (Ljava/lang/String;CILcom/sun/glass/ui/Pixels;ZZLcom/sun/glass/ui/MenuItem$Callback;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem
(JNIEnv *env, jobject jMenuDelegate, jstring jTitle, jchar jShortcutKey, jint jShortcutModifiers,
 jobject jIcon, jboolean jEnabled, jboolean jChecked, jobject jCallback)
{
    jlong value = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menuItem = [[GlassMenu alloc] initWithJavajdelegate:jMenuDelegate
                                                                jtitle:jTitle jshortcut:jShortcutKey
                                                            jmodifiers:jShortcutModifiers jicon:jIcon
                                                              jenabled:jEnabled jchecked:jChecked
                                                             jcallback:jCallback];
        value = ptr_to_jlong(menuItem);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    NSString* title = [GlassHelper nsStringWithJavaString:jTitle withEnv:env];
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem md %p title %s --> %p, value",
        jMenuDelegate, [title UTF8String], value);

    return value;
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _insert
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1insert
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jlong jSubmenuPtr, jint jPos)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1insert del %p mp %p smp %p pos %d",
        jMenuDelegate, jMenuPtr, jSubmenuPtr, jPos);

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);

        if (jSubmenuPtr != 0)
        {
            GlassMenu *submenu = (GlassMenu *)jlong_to_ptr(jSubmenuPtr);
            [menu->menu insertItem:submenu->item atIndex:jPos];
            [menu->menu setSubmenu:submenu->menu forItem:submenu->item];

            [submenu->menu setAutoenablesItems:YES];
        }
        else
        {
            [menu->menu addItem:[NSMenuItem separatorItem]];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _remove
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jlong jSubmenuPtr, jint jPos)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove del %p mp %p smp %p pos %d",
        jMenuDelegate, jMenuPtr, jSubmenuPtr, jPos);

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        if (jSubmenuPtr != 0)
        {
            GlassMenu *submenu = (GlassMenu *)jlong_to_ptr(jSubmenuPtr);
            LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove: submenu %p subitem %p subindex %d",
                submenu, submenu->item, [menu->menu indexOfItem: submenu->item]);

            if ([menu->menu indexOfItem: submenu->item] != -1) {
                [menu->menu removeItem:submenu->item];
            }
        }
        else
        {
            LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove: at index %d", jPos);
            [menu->menu removeItemAtIndex:jPos];
        }
        [[NSApp mainMenu] update];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setTitle
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setTitle
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jstring jTitle)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setTitle");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        NSInteger index = [[NSApp mainMenu] indexOfItem: menu->item];
        NSString *title = [GlassHelper nsStringWithJavaString:jTitle withEnv:env];
        if (index != -1) {
            [[[[NSApp mainMenu] itemAtIndex:index] submenu] setTitle: title];
        } else {
            [menu->item setTitle:title];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setShortcut
 * Signature: (JCI)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setShortcut
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jchar jShortcut, jint jModifiers)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setShortcut");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        [menu _setShortcut:jShortcut modifiers:jModifiers];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setEnabled
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setEnabled
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jboolean jEnabled)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setEnabled");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        [menu->item setEnabled:(BOOL)jEnabled];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setChecked
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setChecked
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jboolean jChecked)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setChecked");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        [menu _setChecked:(BOOL)jChecked];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setCallback
 * Signature: (JLcom/sun/glass/ui/MenuItem$Callback;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setCallback
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jobject jCallback)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setCallback");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        GET_MAIN_JENV;
        (*env)->DeleteGlobalRef(env, menu->jCallback);
        if (jCallback != NULL)
        {
            menu->jCallback = (*env)->NewGlobalRef(env, jCallback);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacMenuDelegate
 * Method:    _setPixels
 * Signature: (JLcom/sun/glass/ui/Pixels;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacMenuDelegate__1setPixels
(JNIEnv *env, jobject jMenuDelegate, jlong jMenuPtr, jobject jPixels)
{
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1setPixels");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        [menu _setPixels:jPixels];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}
