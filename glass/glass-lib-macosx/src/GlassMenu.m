/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
        self->menu = [[NSMenu allocWithZone:[NSMenu menuZone]] initWithTitle:@"Menubar"];
    }
    return self;
}

- (void)_insert
{
    GlassMenu *glassmenu = (GlassMenu *)jlong_to_ptr(self->_insertjMenuPtr);
    
    [self->menu insertItem:glassmenu->item atIndex:self->_insertjPos];
    [self->menu setSubmenu:glassmenu->menu forItem:glassmenu->item];
    
    [glassmenu->menu setAutoenablesItems:YES];
    
    if ([[glassmenu->item title] compare:@"Apple"] == NSOrderedSame)
    {
        [NSApp performSelector:@selector(setAppleMenu:) withObject:glassmenu->item];
    }
    
    [[NSApp mainMenu] update];
}

- (void)_remove
{
    GlassMenu *glassmenu = (GlassMenu *)jlong_to_ptr(self->_removejMenuPtr);
    
    [self->menu removeItem:glassmenu->item];
    
    [[NSApp mainMenu] update];
}

- (void)dealloc
{
    NSLog(@"GlassMenubar dealloc: %p", self);
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
        NSString* title = [GlassHelper nsStringWithJavaString:jtitle withEnv:env];
        self->item = [[NSMenuItem allocWithZone:[NSMenu menuZone]] initWithTitle:title
                                                                          action:NULL
                                                                   keyEquivalent:@""];
        self->_setEnabled = jenabled;
        [self _setEnabled];
        
        [self->item setTarget:self];
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
        self->item = [[NSMenuItem allocWithZone:[NSMenu menuZone]] initWithTitle:title
                                                                          action:@selector(action:)
                                                                   keyEquivalent:shortcut];
        if (jshortcut != '\0')
        {
            self->_setShortcutShortcut = jshortcut;
            self->_setShortcutModifiers = jmodifiers;
            [self _setShortcut];
        }
        self->_setEnabled = jenabled;
        self->_setChecked = jchecked;
        [self _setEnabled];
        [self _setChecked];

        if (jicon != NULL) {
            self->_setPixels = jicon;
            [self _setPixels];
        }
        [self->item setTarget:self];
    }
    return self;
}

- (void)dealloc
{
    GET_MAIN_JENV;
    
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

        return (glassTargetItem->_setEnabled==JNI_TRUE);
    } 
    return YES;
}

- (void)_insert
{
    if (self->menu == nil)
    {
        self->menu = [[NSMenu allocWithZone:[NSMenu menuZone]] initWithTitle:[self->item title]];
        [self->menu setDelegate: self];
    }
    
    if (self->_insertSubmenuPtr != 0)
    {
        GlassMenu *submenu = (GlassMenu *)jlong_to_ptr(self->_insertSubmenuPtr);
        [self->menu insertItem:submenu->item atIndex:self->_insertPos];
        [self->menu setSubmenu:submenu->menu forItem:submenu->item];
        
        [submenu->menu setAutoenablesItems:YES];
    }
    else
    {
        [self->menu addItem:[NSMenuItem separatorItem]];
    }
}

- (void)_remove
{
    if (self->_removeSubmenuPtr != 0)
    {
        GlassMenu *submenu = (GlassMenu *)jlong_to_ptr(self->_removeSubmenuPtr);
        [self->menu removeItem:submenu->item];
    }
    else
    {
        [self->menu removeItemAtIndex:self->_removePos];
    }
    [[NSApp mainMenu] update];
}

- (void)_setTitle
{
    GET_MAIN_JENV;
    [self->item setTitle:[GlassHelper nsStringWithJavaString:self->_setTitle withEnv:env]];
}

- (void)_setShortcut
{
    NSString *shortcut = @"";
    if (self->_setShortcutShortcut != '\0')
    {
        shortcut = [NSString stringWithFormat:@"%c", self->_setShortcutShortcut];
        LOG("_setShortcut %c", self->_setShortcutShortcut);
    }
    
    NSUInteger modifier = 0;
    if ((self->_setShortcutModifiers & com_sun_glass_events_KeyEvent_MODIFIER_COMMAND) != 0)
    {
        modifier = modifier | NSCommandKeyMask;
    }
    if ((self->_setShortcutModifiers & com_sun_glass_events_KeyEvent_MODIFIER_SHIFT) != 0)
    {
        modifier = modifier | NSShiftKeyMask;
    }
    if ((self->_setShortcutModifiers & com_sun_glass_events_KeyEvent_MODIFIER_CONTROL) != 0)
    {
        modifier = modifier | NSControlKeyMask;
    }
    if ((self->_setShortcutModifiers & com_sun_glass_events_KeyEvent_MODIFIER_OPTION) != 0)
    {
        modifier = modifier | NSAlternateKeyMask;
    }
    if ((self->_setShortcutModifiers & com_sun_glass_events_KeyEvent_MODIFIER_FUNCTION) != 0)
    {
        modifier = modifier | NSFunctionKeyMask;
        if (self->_setShortcutShortcut >= com_sun_glass_events_KeyEvent_VK_F1 &&
            self->_setShortcutShortcut <= com_sun_glass_events_KeyEvent_VK_F12) {
            int delta = self->_setShortcutShortcut - com_sun_glass_events_KeyEvent_VK_F1;
            shortcut = [NSString stringWithFormat:@"%C", (unsigned short)(NSF1FunctionKey + delta)];
        } else if (self->_setShortcutShortcut >= com_sun_glass_events_KeyEvent_VK_F13 &&
                   self->_setShortcutShortcut <= com_sun_glass_events_KeyEvent_VK_F24) {
            int delta = self->_setShortcutShortcut - com_sun_glass_events_KeyEvent_VK_F13;
            shortcut = [NSString stringWithFormat:@"%C", (unsigned short)(NSF13FunctionKey + delta)];
        }
    }
    [self->item setKeyEquivalent:shortcut];
    [self->item setKeyEquivalentModifierMask:modifier];
}

- (void)_setEnabled
{
    if (self->_setEnabled == JNI_TRUE)
    {
        [self->item setEnabled:YES];
    }
    else
    {
        [self->item setEnabled:NO];
    }
}

- (void)_setChecked
{
    if (self->_setChecked == JNI_TRUE)
    {
        [self->item setState:NSOnState];
    }
    else
    {
        [self->item setState:NSOffState];
    }
}

- (void)_setCallback
{
    GET_MAIN_JENV;
    (*env)->DeleteGlobalRef(env, self->jCallback);
    if (self->_setCallback != NULL)
    {
        self->jCallback = (*env)->NewGlobalRef(env, self->_setCallback);
    }
}

- (void)_setPixels
{
    GET_MAIN_JENV;
    if (self->_setPixels != NULL)
    {
        self->_setPixels = (*env)->NewGlobalRef(env, self->_setPixels);
        NSImage *image = NULL;
        (*env)->CallVoidMethod(env, self->_setPixels, jPixelsAttachData, ptr_to_jlong(&image));
        if (image != NULL)
        {
            [self->item setImage: image];
            [image release];
        }
        (*env)->DeleteGlobalRef(env, self->_setPixels);
    }
    else
    {
        [self->item setImage: nil];
    }
}

@end

#pragma mark --- Dispatcher

static jlong Do_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar
(JNIEnv *env, jobject jMenuBarDelegate)
{
    GlassMenubar *menubar = [[GlassMenubar allocWithZone:[NSMenu menuZone]] init];
    GLASS_CHECK_EXCEPTION(env);
    return ptr_to_jlong(menubar);
}

static jlong Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu
(JNIEnv *env, jobject jMenuDelegate, jstring jTitle, jboolean jEnabled)
{
    GlassMenu *menu = [[GlassMenu alloc] initWithJavajdelegate:jMenuDelegate 
                                                        jtitle:jTitle 
                                                      jenabled:jEnabled];
    GLASS_CHECK_EXCEPTION(env);
    return ptr_to_jlong(menu);
}

static jlong Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem
(JNIEnv *env, jobject jMenuDelegate, jstring jTitle, jchar jShortcutKey, jint jShortcutModifiers,
 jobject jIcon, jboolean jEnabled, jboolean jChecked, jobject jCallback)
{
    GlassMenu *menuItem = [[GlassMenu alloc] initWithJavajdelegate:jMenuDelegate
                                                            jtitle:jTitle jshortcut:jShortcutKey
                                                        jmodifiers:jShortcutModifiers jicon:jIcon
                                                          jenabled:jEnabled jchecked:jChecked
                                                         jcallback:jCallback];
    GLASS_CHECK_EXCEPTION(env);
    return ptr_to_jlong(menuItem);
}

@interface GlassMenuDispatcher : NSObject
{
@public
    jobject jMenuBarDelegate;
    jobject jMenuDelegate;
    jstring jTitle;
    jchar jShortcutKey;
    jint jShortcutModifiers;
    jobject jIcon;
    jboolean jEnabled;
    jobject jVisible;
    jboolean jChecked;
    jobject jCallback;
    jlong jlongReturn;
}
@end

@implementation GlassMenuDispatcher

- (void)Do_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar
{
    GET_MAIN_JENV;
    self->jlongReturn = Do_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar(env, self->jMenuBarDelegate);
}

- (void)Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu
{
    GET_MAIN_JENV;
    self->jlongReturn = Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu(env, 
                                                                             self->jMenuDelegate, 
                                                                             self->jTitle, 
                                                                             self->jEnabled);
}

- (void)Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem
{
    GET_MAIN_JENV;
    self->jlongReturn = Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem(env, self->jMenuDelegate,
                                                                                 self->jTitle, self->jShortcutKey,
                                                                                 self->jShortcutModifiers, self->jIcon,
                                                                                 self->jEnabled, self->jChecked,
                                                                                 self->jCallback);
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
        if ([NSThread isMainThread] == YES)
        {
            value = Do_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar(env, jMenuBarDelegate);
        }
        else
        {
            GlassMenuDispatcher *dispatcher = [[GlassMenuDispatcher alloc] autorelease];
            dispatcher->jMenuBarDelegate = jMenuBarDelegate;
            [dispatcher performSelectorOnMainThread:@selector(Do_com_sun_glass_ui_mac_MacMenuBarDelegate__1createMenuBar) withObject:dispatcher waitUntilDone:YES]; // gznote: need to wait for return value
            value = dispatcher->jlongReturn;
        }
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
        menubar->_insertjMenuPtr = jMenuPtr;
        menubar->_insertjPos = jPos;
        if ([NSThread isMainThread] == YES)
        {
            [menubar _insert];
        }
        else
        {
            [menubar performSelectorOnMainThread:@selector(_insert) withObject:nil waitUntilDone:YES];
        }
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
    LOG("Java_com_sun_glass_ui_mac_MacMenuBarDelegate__1remove");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenubar *menubar = (GlassMenubar *)jlong_to_ptr(jMenubarPtr);
        menubar->_removejMenuPtr = jMenuPtr;
        menubar->_removejPos = jPos;
        if ([NSThread isMainThread] == YES)
        {
            [menubar _remove];
        }
        else
        {
            [menubar performSelectorOnMainThread:@selector(_remove) withObject:nil waitUntilDone:YES];
        }
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
    jclass jCallbackClass = (*env)->FindClass(env, "com/sun/glass/ui/MenuItem$Callback");
    jclass jMenuClass = (*env)->FindClass(env, "com/sun/glass/ui/Menu");
    
    jMenuActionMethod  = (*env)->GetMethodID(env, jCallbackClass,   "action",  "()V");
    jMenuValidateMethod = (*env)->GetMethodID(env, jCallbackClass,   "validate",  "()V");
    jMenuOpeningMethod = (*env)->GetMethodID(env, jMenuClass, "notifyMenuOpening", "()V");
    jMenuClosedMethod  = (*env)->GetMethodID(env, jMenuClass, "notifyMenuClosed",  "()V");
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
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu");
    
    jlong value = 0L;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if ([NSThread isMainThread] == YES)
        {
            value = Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu(env, jMenuDelegate, jTitle, jEnabled);
        }
        else
        {
            GlassMenuDispatcher *dispatcher = [[GlassMenuDispatcher alloc] autorelease];
            dispatcher->jMenuDelegate = jMenuDelegate;
            dispatcher->jTitle = jTitle;
            dispatcher->jEnabled = jEnabled;
            [dispatcher performSelectorOnMainThread:@selector(Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenu) withObject:dispatcher waitUntilDone:YES]; // gznote: need to wait for return value
            value = dispatcher->jlongReturn;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
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
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem");
    
    jlong value = 0L;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if ([NSThread isMainThread] == YES)
        {
            value = Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem(env, jMenuDelegate, jTitle, jShortcutKey, jShortcutModifiers, jIcon, jEnabled, jChecked, jCallback);
        }
        else
        {
            GlassMenuDispatcher *dispatcher = [[GlassMenuDispatcher alloc] autorelease];
            dispatcher->jMenuDelegate = jMenuDelegate;
            dispatcher->jTitle = jTitle;
            dispatcher->jShortcutKey = jShortcutKey;
            dispatcher->jShortcutModifiers = jShortcutModifiers;
            dispatcher->jIcon = jIcon;
            dispatcher->jEnabled = jEnabled;
            dispatcher->jChecked = jChecked;
            dispatcher->jCallback = jCallback;
            [dispatcher performSelectorOnMainThread:@selector(Do_com_sun_glass_ui_mac_MacMenuDelegate__1createMenuItem) withObject:dispatcher waitUntilDone:YES]; // gznote: need to wait for return value
            value = dispatcher->jlongReturn;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
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
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1insert");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        menu->_insertSubmenuPtr = jSubmenuPtr;
        menu->_insertPos = jPos;
        if ([NSThread isMainThread] == YES)
        {
            [menu _insert];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_insert) withObject:nil waitUntilDone:YES];
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
    LOG("Java_com_sun_glass_ui_mac_MacMenuDelegate__1remove");
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        GlassMenu *menu = (GlassMenu *)jlong_to_ptr(jMenuPtr);
        menu->_removeSubmenuPtr = jSubmenuPtr;
        menu->_removePos = jPos;
        if ([NSThread isMainThread] == YES)
        {
            [menu _remove];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_remove) withObject:nil waitUntilDone:YES];
        }
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
        menu->_setTitle = jTitle;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setTitle];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setTitle) withObject:nil waitUntilDone:YES];
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
        menu->_setShortcutShortcut = jShortcut;
        menu->_setShortcutModifiers = jModifiers;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setShortcut];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setShortcut) withObject:nil waitUntilDone:YES];
        }
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
        menu->_setEnabled = jEnabled;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setEnabled];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setEnabled) withObject:nil waitUntilDone:YES];
        }
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
        menu->_setChecked = jChecked;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setChecked];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setChecked) withObject:nil waitUntilDone:YES];
        }
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
        menu->_setCallback = jCallback;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setCallback];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setCallback) withObject:nil waitUntilDone:YES];
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
        menu->_setPixels = jPixels;
        if ([NSThread isMainThread] == YES)
        {
            [menu _setPixels];
        }
        else
        {
            [menu performSelectorOnMainThread:@selector(_setPixels) withObject:nil waitUntilDone:YES];
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}
