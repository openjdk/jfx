/*
 * Copyright (c) 2011, 2024, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_ui_mac_MacApplication.h"
#import "com_sun_glass_events_KeyEvent.h"


#import "GlassMacros.h"
#import "GlassApplication.h"
#import "GlassHelper.h"
#import "GlassKey.h"
#import "GlassScreen.h"
#import "GlassWindow.h"
#import "GlassTouches.h"
#import "PlatformSupport.h"

#import "ProcessInfo.h"
#import <Security/SecRequirement.h>
#import <Carbon/Carbon.h>

#pragma clang diagnostic ignored "-Wdeprecated-declarations"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

//#define VERBOSE_LOAD

static BOOL shouldKeepRunningNestedLoop = YES;
static jobject nestedLoopReturnValue = NULL;
static BOOL isFullScreenExitingLoop = NO;
static NSMutableDictionary * keyCodeForCharMap = nil;
static BOOL isEmbedded = NO;
static BOOL requiresActivation = NO;
static BOOL triggerReactivation = NO;
static BOOL disableSyncRendering = NO;
static BOOL firstActivation = YES;
static BOOL shouldReactivate = NO;

// Custom NSRunLoopMode constant that matches the one used by AWT in its
// doAWTRunLoopImpl method. This is not formally documented yet, but all
// versions of the JDK use it. We might consider a request to document it.
static NSString* JavaRunLoopMode = @"AWTRunLoopMode";

// List of allowable runLoop modes that Java runnables can run in.
// This is used when calling performSelectorOnMainThread from
// the JNI _invokeAndWait and _submitForLaterInvocation methods.
// We include JavaRunLoopMode in the list of allowable run modes in case
// we are running on the AWT event thread. This will allow JavaFX
// runnables to be scheduled when AWT is running doAWTRunLoopImpl
// on the AppKit thread (which it does for IME callbacks) so that we
// don't deadlock.
static NSArray<NSString*> *runLoopModes = nil;

#ifdef STATIC_BUILD
jint JNICALL JNI_OnLoad_glass(JavaVM *vm, void *reserved)
#else
jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
#endif
{
    pthread_key_create(&GlassThreadDataKey, NULL);

    memset(&javaIDs, 0, sizeof(javaIDs));
    MAIN_JVM = vm;
    return JNI_VERSION_1_4;
}

#pragma mark --- GlassRunnable

@interface GlassRunnable : NSObject
{
    jobject jRunnable;
}

- (id)initWithRunnable:(jobject)runnable;
- (void)run;

@end

@implementation GlassRunnable

- (id)initWithRunnable:(jobject)runnable
{
    self->jRunnable = runnable;
    return self;
}

- (void)run
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        assert(pthread_main_np() == 1);
        JNIEnv *env = jEnv;
        if (env != NULL && self->jRunnable != NULL)
        {
            (*env)->CallVoidMethod(env, self->jRunnable, jRunnableRun);
            GLASS_CHECK_EXCEPTION(env);

            (*env)->DeleteGlobalRef(env, self->jRunnable);
        }

        self->jRunnable = NULL;

        [self release];
    }
    [pool drain];
}

@end

@implementation NSApplicationFX
@end

#pragma mark --- GlassApplication

@implementation GlassApplication

- (id)initWithEnv:(JNIEnv*)env application:(jobject)application launchable:(jobject)launchable taskbarApplication:(jboolean)isTaskbarApplication classLoader:(jobject)classLoader
{
    self = [super init];
    if (self != nil)
    {
        self->started = NO;
        self->jTaskBarApp = isTaskbarApplication;

        self->jApplication = (*env)->NewGlobalRef(env, application);
        if (launchable != NULL)
        {
            self->jLaunchable = (*env)->NewGlobalRef(env, launchable);
        }

        if (classLoader != NULL)
        {
            [GlassHelper SetGlassClassLoader:classLoader withEnv:env];
        }
    }
    return self;
}

#pragma mark --- delegate methods

- (void)GlassApplicationDidChangeScreenParameters
{
    LOG("GlassApplicationDidChangeScreenParameters");

    assert(pthread_main_np() == 1);
    JNIEnv *env = jEnv;
    if (env != NULL)
    {
        GlassScreenDidChangeScreenParameters(env);
    }
}

- (void)platformPreferencesDidChange {
    // Some dynamic colors like NSColor.controlAccentColor don't seem to be reliably updated
    // at the exact moment AppleColorPreferencesChangedNotification is received.
    // As a workaround, we wait for a short period of time (one second seems sufficient) before
    // we query the updated platform preferences.

    [NSObject cancelPreviousPerformRequestsWithTarget:self
              selector:@selector(updatePlatformPreferences)
              object:nil];

    [self performSelector:@selector(updatePlatformPreferences)
          withObject:nil
          afterDelay:1.0];
}

- (void)updatePlatformPreferences {
    [PlatformSupport updatePreferences:self->jApplication];
}

- (void)applicationWillFinishLaunching:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillFinishLaunching");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        // unblock main thread. Glass is started at this point.
        self->started = YES;

        if (self->jLaunchable != NULL)
        {
            jclass runnableClass = [GlassHelper ClassForName:"java.lang.Runnable" withEnv:jEnv];
            if ((*env)->ExceptionCheck(env) == JNI_TRUE)
            {
                (*env)->ExceptionDescribe(env);
                (*env)->ExceptionClear(env);
            }
            if (runnableClass) {
                jmethodID runMethod = (*env)->GetMethodID(env, runnableClass, "run", "()V");
                if ((*env)->ExceptionCheck(env) == JNI_TRUE)
                {
                    (*env)->ExceptionDescribe(env);
                    (*env)->ExceptionClear(env);
                }
                if (runMethod) {
                    (*env)->CallVoidMethod(env, self->jLaunchable, runMethod);
                    if ((*env)->ExceptionCheck(env) == JNI_TRUE)
                    {
                        (*env)->ExceptionDescribe(env);
                        (*env)->ExceptionClear(env);
                    }
                    else
                    {
                        [[NSNotificationCenter defaultCenter] addObserver:self
                                                                 selector:@selector(GlassApplicationDidChangeScreenParameters)
                                                                     name:NSApplicationDidChangeScreenParametersNotification
                                                                   object:nil];

                        [[NSDistributedNotificationCenter defaultCenter] addObserver:self
                                                                         selector:@selector(platformPreferencesDidChange)
                                                                         name:@"AppleInterfaceThemeChangedNotification"
                                                                         object:nil];

                        [[NSDistributedNotificationCenter defaultCenter] addObserver:self
                                                                         selector:@selector(platformPreferencesDidChange)
                                                                         name:@"AppleColorPreferencesChangedNotification"
                                                                         object:nil];

                        [[[NSWorkspace sharedWorkspace] notificationCenter]
                            addObserver:self
                            selector:@selector(platformPreferencesDidChange)
                            name:NSWorkspaceAccessibilityDisplayOptionsDidChangeNotification
                            object:nil];

                        // localMonitor = [NSEvent addLocalMonitorForEventsMatchingMask: NSRightMouseDownMask
                        //                                                      handler:^(NSEvent *incomingEvent) {
                        //                                                          NSEvent *result = incomingEvent;
                        //                                                          NSWindow *targetWindowForEvent = [incomingEvent window];
                        //                                                          LOG("NSRightMouseDownMask local");
                        //                                                          return result;
                        //                                                      }];
                        //
                        // globalMonitor = [NSEvent addGlobalMonitorForEventsMatchingMask: NSRightMouseDownMask
                        //                                                      handler:^(NSEvent *incomingEvent) {
                        //                                                          NSEvent *result = incomingEvent;
                        //                                                          NSWindow *targetWindowForEvent = [incomingEvent window];
                        //                                                          NSWindow *window = [[NSApplication sharedApplication]
                        //                                                                       windowWithWindowNumber:[incomingEvent windowNumber]];
                        //                                                          NSWindow *appWindow = [[NSApplication sharedApplication] mainWindow];
                        //                                                          LOG("NSRightMouseDownMask global: %p num %d win %p appwin %p",
                        //                                                              targetWindowForEvent, [incomingEvent windowNumber], window,
                        //                                                              [[NSApplication sharedApplication] mainWindow]);
                        //                                                     }];
                    }
                } else {
                    NSLog(@"ERROR: Glass could not find run() method\n");
                }
            } else {
                NSLog(@"ERROR: Glass could not find Runnable class\n");
            }
        }

        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillFinishLaunchingMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (BOOL)applicationSupportsSecureRestorableState:(NSApplication *)app {
    return YES;
}

- (void)applicationDidFinishLaunching:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationDidFinishLaunching");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyDidFinishLaunchingMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    if (!NSApp.isActive && requiresActivation) {
        // As of macOS 14, application gets to the foreground,
        // but it doesn't get activated, so this is needed:
        LOG("-> need to active application");
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSApp performSelector: @selector(activate)];
        });
        // TODO: performSelector is used only to avoid a compiler
        // warning with the 13.3 SDK. After updating to SDK 14
        // this can be converted to a standard call.
    }
}

- (void)applicationWillBecomeActive:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillBecomeActive");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillBecomeActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)applicationDidBecomeActive:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationDidBecomeActive");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyDidBecomeActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    if (triggerReactivation && firstActivation) {
        LOG("-> deactivate (hide)  app");
        firstActivation = NO;
        shouldReactivate = YES;
        [NSApp hide:NSApp];
    }
}

- (void)applicationWillResignActive:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillResignActive");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillResignActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)applicationDidResignActive:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationDidResignActive");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyDidResignActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    if (triggerReactivation && shouldReactivate) {
        LOG("-> reactivate  app");
        shouldReactivate = NO;
        [NSApp activateIgnoringOtherApps:YES];
    }
}

- (void)applicationWillHide:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillHide");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillHideMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)applicationDidHide:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationDidHide");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyDidHideMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)applicationWillUnhide:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillUnhide");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillUnhideMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)applicationDidUnhide:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationDidUnhide");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyDidUnhideMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)application:(NSApplication *)theApplication openFiles:(NSArray *)filenames
{
    LOG("GlassApplication:application:openFiles");

    GET_MAIN_JENV;
    NSApplicationDelegateReply reply = NSApplicationDelegateReplySuccess;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        NSUInteger count = [filenames count];
        jclass stringClass = [GlassHelper ClassForName:"java.lang.String" withEnv:env];
        if (!stringClass) {
            return;
        }
        jobjectArray files = (*env)->NewObjectArray(env, (jsize)count, stringClass, NULL);
        GLASS_CHECK_EXCEPTION(env);
        if (files != NULL) {
            for (NSUInteger i=0; i<count; i++)
            {
                NSString *file = [filenames objectAtIndex:i];
                if (file != nil)
                {
                    (*env)->SetObjectArrayElement(env, files, (jsize)i, (*env)->NewStringUTF(env, [file UTF8String]));
                    GLASS_CHECK_EXCEPTION(env);
                }
            }
            (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyOpenFilesMethod], files);
        } else {
            fprintf(stderr, "NewObjectArray failed in GlassApplication_application\n");
            reply = NSApplicationDelegateReplyFailure;
        }
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    [theApplication replyToOpenOrPrint:reply];
}

- (BOOL)application:(NSApplication *)theApplication openFile:(NSString *)filename
{
    LOG("GlassApplication:application:openFile");

    // controlled by Info.plist -NSOpenfileName
    // http://developer.apple.com/library/mac/#documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html
    [self application:theApplication openFiles:[NSArray arrayWithObject:filename]];

    return YES;
}

- (BOOL)application:(id)theApplication openFileWithoutUI:(NSString *)filename
{
    LOG("GlassApplication:application:openFileWithoutUI");

    // programmaticaly called by the client (even though GlassApplication does not currently call it, let's wire it in just in case)
    [self application:theApplication openFiles:[NSArray arrayWithObject:filename]];

    return YES;
}

- (BOOL)application:(NSApplication *)theApplication openTempFile:(NSString *)filename
{
    LOG("GlassApplication:application:openTempFile");

    // controlled by Info.plist -NSOpenTempfileName
    // http://developer.apple.com/library/mac/#documentation/MacOSX/Conceptual/BPRuntimeConfig/Articles/ConfigApplications.html
    // NOP

    return YES;
}

- (BOOL)applicationShouldOpenUntitledFile:(NSApplication *)sender
{
    LOG("GlassApplication:applicationShouldOpenUntitledFile");

    // don't want

    return NO;
}

- (NSApplicationTerminateReply)applicationShouldTerminate:(NSApplication *)sender
{
    LOG("GlassApplication:applicationShouldTerminate");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillQuitMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    return NSTerminateCancel;
}


- (BOOL)applicationOpenUntitledFile:(NSApplication *)theApplication
{
    LOG("GlassApplication:applicationOpenUntitledFile");

    // NOP (should never be called because applicationShouldOpenUntitledFile returns NO)

    return YES;
}

#pragma mark --- Glass support

- (void)runLoop:(id)selector
{
    LOG("GlassApplication:runLoop ENTER");

    NSAutoreleasePool *pool1 = [[NSAutoreleasePool alloc] init];

    jint error = (*jVM)->AttachCurrentThreadAsDaemon(jVM, (void **)&jEnv, NULL);
    if (error == 0)
    {
        NSAutoreleasePool *pool2 = [[NSAutoreleasePool alloc] init];

        if ([[NSThread currentThread] name] == nil)
        {
            [[NSThread currentThread] setName:@"Main Cocoa (UI) Thread"];
        }

        GlassApplication *glassApp = (GlassApplication *)selector;

        // Load MacApplication class using the glass classloader
        jclass cls = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacApplication" withEnv:jEnv];
        if (!cls)
        {
            NSLog(@"ERROR: can't find the MacApplication class");
        }
        else
        {
            jmethodID setEventThreadMID = (*jEnv)->GetMethodID(jEnv, cls, "setEventThread", "()V");
            if (!setEventThreadMID)
            {
                NSLog(@"ERROR: can't get MacApplication.setEventThread() method ID");
            }
            else
            {
                (*jEnv)->CallVoidMethod(jEnv, glassApp->jApplication, setEventThreadMID);
            }
        }
        GLASS_CHECK_EXCEPTION(jEnv);

        NSBundle *mainBundle = [NSBundle mainBundle];
        {
            NSString *appName = [mainBundle objectForInfoDictionaryKey:@"CFBundleDisplayName"];

            if (appName == nil) {
                appName = [mainBundle objectForInfoDictionaryKey:@"CFBundleName"];
            }

            if (appName) {
                // make the name available to Java side, before Launchable.fnishLaunching callback
                jstring jname = (*jEnv)->NewStringUTF(jEnv, [appName UTF8String]);
                jmethodID setNameMethod = (*jEnv)->GetMethodID(jEnv, cls, "setName", "(Ljava/lang/String;)V");
                GLASS_CHECK_EXCEPTION(jEnv);
                if (setNameMethod != NULL) {
                    (*jEnv)->CallVoidMethod(jEnv, glassApp->jApplication, setNameMethod, jname);
                }
                GLASS_CHECK_EXCEPTION(jEnv);
            }
        }

        // List of RunLoopModes in which we will run runnables passed
        // to _invokeAndWait and _submitForLaterInvocation. This includes
        // JavaRunLoopMode to avoid a possible deadlock with AWT.
        // There is no harm always adding it, since it will have no
        // effect if the run loop that receives this message does not
        // specify it.
        runLoopModes = [[NSArray alloc] initWithObjects:
            NSDefaultRunLoopMode,
            NSModalPanelRunLoopMode,
            NSEventTrackingRunLoopMode,
            JavaRunLoopMode,
            nil];

        // Determine if we're running embedded (in AWT, SWT, elsewhere)
        NSApplication *app = [NSApplicationFX sharedApplication];
        isEmbedded = ![app isKindOfClass:[NSApplicationFX class]];

        if (!isEmbedded)
        {
            // Not embedded in another toolkit, so disable automatic tabbing for all windows
            // We use a guarded call to preserve the ability to run on 10.10 or 10.11.
            // Using a guard, instead of reflection, assumes the Xcode used to
            // build includes MacOSX SDK 10.12 or later
            if (@available(macOS 10.12, *)) {
                [NSWindow setAllowsAutomaticWindowTabbing:NO];
            }
            if (self->jTaskBarApp == JNI_TRUE)
            {
                triggerReactivation = YES;

                // The workaround of deactivating and reactivating
                // the application so that the system menu bar works
                // correctly is no longer needed (and no longer works
                // anyway) as of macOS 14
                if (@available(macOS 14.0, *)) {
                    triggerReactivation = NO;
                    requiresActivation = YES;
                }

                // move process from background only to full on app with visible Dock icon
                ProcessSerialNumber psn;
                if (GetCurrentProcess(&psn) == noErr)
                {
                    TransformProcessType(&psn, kProcessTransformToForegroundApplication);
                }

                NSString *CFBundleIconFile = [mainBundle objectForInfoDictionaryKey:@"CFBundleIconFile"];
                NSString *iconPath = nil;
                if (CFBundleIconFile != nil)
                {
                    iconPath = [mainBundle pathForResource:[CFBundleIconFile stringByDeletingPathExtension] ofType:[CFBundleIconFile pathExtension]];
                }

                // -Xdock:icon can override CFBundleIconFile (but only if it actually points to a valid icon)
                NSString *property = [NSString stringWithFormat:@"APP_ICON_%d", [[NSProcessInfo processInfo] processIdentifier]];
                char *path = getenv([property UTF8String]);
                if (path != NULL)
                {
                    BOOL isFolder = NO;
                    NSString *overridenPath = [NSString stringWithFormat:@"%s", path];
                    if ([[NSFileManager defaultManager] fileExistsAtPath:overridenPath isDirectory:&isFolder] && !isFolder)
                    {
                        iconPath = overridenPath;
                    }
                }

                NSImage *image = nil;
                {
                    BOOL isFolder = NO;
                    if ([[NSFileManager defaultManager] fileExistsAtPath:iconPath isDirectory:&isFolder] && !isFolder)
                    {
                        image = [[NSImage alloc] initWithContentsOfFile:iconPath];
                    }
                    if (image == nil)
                    {
                        // last resort - if still no icon, then ask for an empty standard app icon, which is guaranteed to exist
                        image = [[NSImage imageNamed:@"NSImageNameApplicationIcon"] retain];
                    }
                }
                [app setApplicationIconImage:image];
                [image release];

                // Install a hidden Window menu. This allows the dock icon
                // menu to show the list of open windows (NSWindow instances)
                NSMenu *myMenu = [[NSMenu alloc] initWithTitle:@"Window"];
                [app setWindowsMenu:myMenu];
                [myMenu release];

                [app setDelegate:self];

                // [app activateIgnoringOtherApps:YES] won't activate the menu bar on OS X 10.9, so instead we do this:
                [[NSRunningApplication currentApplication] activateWithOptions:(NSApplicationActivateIgnoringOtherApps | NSApplicationActivateAllWindows)];
            }
            else
            {
                // allow background processes to change the cursor (10.8 only API so we'll have to dynamically call it if available)
                {
                    BOOL yes = YES;
                    [GlassHelper InvokeSelectorIfAvailable:@selector(javaSetAllowsCursorSetInBackground:) forClass:[NSCursor class] withArgument:&yes withReturnValue:NULL];
                }

                // http://developer.apple.com/library/ios/#documentation/DeveloperTools/Conceptual/cross_development/Using/using.html
                if (floor(NSAppKitVersionNumber) >= 1138) // NSAppKitVersionNumber10_7
                {
                    // 10.7 or later: move process from background only process to a limited app with active windows,
                    // but no Dock icon
                    ProcessSerialNumber psn;
                    if (GetCurrentProcess(&psn) == noErr)
                    {
                        TransformProcessType(&psn, 4); // kProcessTransformToUIElementApplication
                    }
                }
                [app setDelegate:self];
            }

#if defined(VERBOSE_LOAD)
            jclass BooleanClass = [GlassHelper ClassForName:"java.lang.Boolean" withEnv:jEnv];
            if (BooleanClass != 0)
            {
                jmethodID getBooleanMethod = (*jEnv)->GetStaticMethodID(jEnv, BooleanClass, "getBoolean", "(Ljava/lang/String;)Z");
                if (getBooleanMethod != 0)
                {
                    jstring flag = (*jEnv)->NewStringUTF(jEnv, "glassload.verbose");
                    jboolean verbose = (*jEnv)->CallStaticBooleanMethod(jEnv, BooleanClass, getBooleanMethod, flag);
                    if (verbose == JNI_TRUE)
                    {
                        printLoadedLibraries(stderr);
                        printLoadedFiles(stderr);
                    }
                }
            }
#endif

            // drain the pool before entering runloop
            [pool2 drain];

            // enter runloop, this will not return until terminated
            [NSApp run];

            // Abort listerning to global touch input events
            [GlassTouches terminate];

            GLASS_CHECK_EXCEPTION(jEnv);

            (*jEnv)->CallVoidMethod(jEnv, self->jApplication, javaIDs.MacApplication.notifyApplicationDidTerminate);
            GLASS_CHECK_EXCEPTION(jEnv);

            jEnv = NULL;
        }
        else // event loop is not started
        {
            if ([NSThread isMainThread] == YES) {
                // The NSNotification is ignored but the compiler insists on a non-NULL argument.
                NSNotification* notification = [NSNotification notificationWithName: NSApplicationWillFinishLaunchingNotification
                    object: NSApp
                    userInfo: nil];
                [glassApp applicationWillFinishLaunching: notification];
            } else {
                [glassApp performSelectorOnMainThread:@selector(applicationWillFinishLaunching:) withObject:NULL waitUntilDone:NO];
            }
            GLASS_CHECK_EXCEPTION(jEnv);

            [pool2 drain];
        }
    }
    else // attaching to JVM failed
    {
        NSLog(@"ERROR: Glass could not attach to VM, result:%d\n", (int)error);
    }

    [pool1 drain];

    LOG("GlassApplication:runLoop EXIT");
}

- (BOOL)started
{
    return self->started;
}

+ (jobject)enterNestedEventLoopWithEnv:(JNIEnv*)env
{
    jobject ret = NULL;

    NSRunLoop *theRL = [NSRunLoop currentRunLoop];
    NSApplication * app = [NSApplication sharedApplication];
    shouldKeepRunningNestedLoop = YES;
    // Cannot use [NSDate distantFuture] because the period is big the app could hang in a runloop
    // if the event came before entering the RL
    while (shouldKeepRunningNestedLoop && [theRL runMode:NSDefaultRunLoopMode
                                              beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.010]])
    {
        NSEvent * event = [app nextEventMatchingMask: 0xFFFFFFFF untilDate:nil inMode:NSDefaultRunLoopMode dequeue:YES];

        if (event != nil) {
            [app sendEvent: event];
        }
    }

    if (nestedLoopReturnValue != NULL) {
        ret = (*env)->NewLocalRef(env, nestedLoopReturnValue);
        (*env)->DeleteGlobalRef(env, nestedLoopReturnValue);
        nestedLoopReturnValue = NULL;
    }

    shouldKeepRunningNestedLoop = YES;

    return ret;
}

+ (void)leaveNestedEventLoopWithEnv:(JNIEnv*)env retValue:(jobject)retValue
{
    if (retValue != NULL) {
        nestedLoopReturnValue = (*env)->NewGlobalRef(env, retValue);
    }
    shouldKeepRunningNestedLoop = NO;
}

+ (void)enterFullScreenExitingLoop
{
    if (isFullScreenExitingLoop) {
        return;
    }
    isFullScreenExitingLoop = YES;
    GET_MAIN_JENV;
    (*env)->CallStaticObjectMethod(env, jApplicationClass,
            javaIDs.Application.enterNestedEventLoop);
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
    }
    isFullScreenExitingLoop = NO;
}

+ (void)leaveFullScreenExitingLoopIfNeeded
{
    if (!isFullScreenExitingLoop) {
        return;
    }
    GET_MAIN_JENV;
    (*env)->CallStaticVoidMethod(env, jApplicationClass,
            javaIDs.Application.leaveNestedEventLoop, (jobject)NULL);
}

static void inputDidChangeCallback(CFNotificationCenterRef center, void *observer, CFNotificationName name, const void *object, CFDictionaryRef userInfo)
{
    if (keyCodeForCharMap != nil) {
        [keyCodeForCharMap removeAllObjects];
    }
}

+ (void)registerKeyEvent:(NSEvent*)event
{
    if (!keyCodeForCharMap) {
        keyCodeForCharMap = [[NSMutableDictionary alloc] initWithCapacity:100];
        // Note: it's never released, just like, say, the jApplication reference...
        // To avoid stale entries when the user switches layout
        CFNotificationCenterRef center = CFNotificationCenterGetDistributedCenter();
        CFNotificationCenterAddObserver(center, NULL, inputDidChangeCallback,
                                        kTISNotifySelectedKeyboardInputSourceChanged,
                                        NULL, CFNotificationSuspensionBehaviorCoalesce);
    }

    // Add the character the user typed to the map.
    NSNumber* mapObject = [NSNumber numberWithUnsignedShort:[event keyCode]];
    [keyCodeForCharMap setObject:mapObject forKey:[event characters]];
    // getKeyCodeForChar should not just match against a character the user types
    // directly but any other character printed on the same key.
    NSString* unshifted = GetStringForMacKey(event.keyCode, false);
    if (unshifted != nil) {
        [keyCodeForCharMap setObject:mapObject forKey:unshifted];
    }
    NSString* shifted = GetStringForMacKey(event.keyCode, true);
    if (shifted != nil) {
        [keyCodeForCharMap setObject:mapObject forKey:shifted];
    }
    // On some European keyboards there are useful symbols which are only
    // accessible via the Option key. We don't query for the Option key
    // character because on most layouts just about every key has some
    // random symbol assigned to that modifier which the user probably
    // isn't even aware of. The user can get that character into the map
    // by typing it directly.
}

+ (jint)getKeyCodeForChar:(jchar)c;
{
    id v = [keyCodeForCharMap objectForKey:[NSString stringWithCharacters: (UniChar *)&c length:1]];
    if (!v) {
        return com_sun_glass_events_KeyEvent_VK_UNDEFINED;
    } else {
        return GetJavaKeyCodeFor([v unsignedShortValue]);
    }
}

+ (BOOL)syncRenderingDisabled {
    return disableSyncRendering;
}

@end

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1initIDs
(JNIEnv *env, jclass jClass, jboolean jDisableSyncRendering)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1initIDs");

    // Check minimum OS version
    NSOperatingSystemVersion osVer;
    osVer = [[NSProcessInfo processInfo] operatingSystemVersion];
    NSInteger osVerMajor = osVer.majorVersion;
    NSInteger osVerMinor = osVer.minorVersion;

    // Map 10.16 to 11.0, since macOS will return 10.16 by default (for compatibility)
    if (osVerMajor == 10 && osVerMinor >= 16) {
        // FIXME: if we ever need to know which minor version of macOS 11.x we
        // are running on, we will need to look it up using a similar technique
        // to what the JDK does.
        osVerMajor = 11;
        osVerMinor = 0;
    }

    if (osVerMajor < MACOS_MIN_VERSION_MAJOR ||
            (osVerMajor == MACOS_MIN_VERSION_MAJOR &&
             osVerMinor < MACOS_MIN_VERSION_MINOR))
    {
        NSLog(@"ERROR: macOS version is %d.%d, which is below the minimum of %d.%d",
              (int)osVerMajor, (int)osVerMinor, MACOS_MIN_VERSION_MAJOR, MACOS_MIN_VERSION_MINOR);
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        if (exceptionClass != 0) {
            (*env)->ThrowNew(env, exceptionClass, "Unsupported macOS version");
        }
        return;
    }

    disableSyncRendering = jDisableSyncRendering ? YES : NO;

    jApplicationClass = (*env)->NewGlobalRef(env, jClass);

    javaIDs.Application.createPixels = (*env)->GetStaticMethodID(
            env, jClass, "createPixels", "(II[IFF)Lcom/sun/glass/ui/Pixels;");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.Application.getScaleFactor = (*env)->GetStaticMethodID(
            env, jClass, "getScaleFactor", "(IIII)F");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.Application.reportException = (*env)->GetStaticMethodID(
            env, jClass, "reportException", "(Ljava/lang/Throwable;)V");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.Application.enterNestedEventLoop = (*env)->GetStaticMethodID(
            env, jClass, "enterNestedEventLoop", "()Ljava/lang/Object;");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.Application.leaveNestedEventLoop = (*env)->GetStaticMethodID(
            env, jClass, "leaveNestedEventLoop", "(Ljava/lang/Object;)V");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.MacApplication.notifyApplicationDidTerminate = (*env)->GetMethodID(
            env, jClass, "notifyApplicationDidTerminate", "()V");
    if ((*env)->ExceptionCheck(env)) return;

    javaIDs.MacApplication.notifyPreferencesChanged = (*env)->GetMethodID(
            env, jClass, "notifyPreferencesChanged", "(Ljava/util/Map;)V");
    if ((*env)->ExceptionCheck(env)) return;

    if (jRunnableRun == NULL)
    {
        jclass jcls = (*env)->FindClass(env, "java/lang/Runnable");
        if ((*env)->ExceptionCheck(env)) return;
        jRunnableRun = (*env)->GetMethodID(env, jcls, "run", "()V");
        if ((*env)->ExceptionCheck(env)) return;
    }

    [PlatformSupport initIDs:env];
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _runLoop
 * Signature: (Ljava/lang/ClassLoader;Ljava/lang/Runnable;Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1runLoop
(JNIEnv *env, jobject japplication, jobject classLoader,
 jobject jlaunchable, jboolean isTaskbarApplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1runLoop");

    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        if ([NSThread isMainThread] == YES)
        {
            //            fprintf(stderr, "\nWARNING: Glass was started on 1st thread and will block this thread.\nYou most likely do not want to do this - please remove \"-XstartOnFirstThread\" from VM arguments.\n\n");
        }
        else
        {
            if ([[NSThread currentThread] name] == nil)
            {
                [[NSThread currentThread] setName:@"Main Java Thread"];
            }
        }

        GlassApplication *glass = [[GlassApplication alloc] initWithEnv:env application:japplication launchable:jlaunchable taskbarApplication:isTaskbarApplication classLoader:classLoader];
        if ([NSThread isMainThread] == YES) {
            [glass runLoop: glass];
        } else {
            [glass performSelectorOnMainThread:@selector(runLoop:) withObject:glass waitUntilDone:[NSThread isMainThread]];

            // wait for Cocoa to enter its UI runloop
            while ([glass started] == NO)
            {
                LOG("        waiting for [glass started]");
                usleep(10000);
            }
        }

        // at this point Java main thread is allowed to proceed, but Cocoa's UI thread entered its runloop, so the VM will not quit
    }
    [glasspool drain]; glasspool=nil;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _finishTerminating
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1finishTerminating
(JNIEnv *env, jobject japplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1finishTerminating");

    if (isEmbedded) {
        return;
    }

    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        [NSApp stop:nil];
        [NSApp hide:nil];

        // wake up the runloop one last time so that it can process the stop:
        // request, even if the app is inactive currently
        NSTimeInterval dummyEventTimestamp = [NSProcessInfo processInfo].systemUptime;
        NSEvent* event = [NSEvent otherEventWithType: NSApplicationDefined
                                            location: NSMakePoint(0,0)
                                       modifierFlags: 0
                                           timestamp: dummyEventTimestamp
                                        windowNumber: 0
                                             context: nil
                                             subtype: 0
                                               data1: 0
                                               data2: 0];
        [NSApp postEvent: event atStart: NO];
    }
    [glasspool drain]; glasspool=nil;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _enterNestedEventLoopImpl
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacApplication__1enterNestedEventLoopImpl
(JNIEnv *env, jobject japplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1enterNestedEventLoopImpl");

    jobject ret;

    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        ret = [GlassApplication enterNestedEventLoopWithEnv:env];
    }
    [glasspool drain]; glasspool=nil;
    GLASS_CHECK_EXCEPTION(env);

    return ret;
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _leaveNestedEventLoopImpl
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1leaveNestedEventLoopImpl
(JNIEnv *env, jobject japplication, jobject retValue)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1leaveNestedEventLoopImpl");

    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        [GlassApplication leaveNestedEventLoopWithEnv:env retValue:retValue];
    }
    [glasspool drain]; glasspool=nil;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_Application
 * Method:    _submitForLaterInvocation
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1submitForLaterInvocation
(JNIEnv *env, jobject japplication, jobject jRunnable)
{
    //LOG("Java_com_sun_glass_ui_mac_MacApplication_submitForLaterInvocation");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    if (jEnv != NULL)
    {
        GlassRunnable *runnable = [[GlassRunnable alloc] initWithRunnable:(*env)->NewGlobalRef(env, jRunnable)];
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:NO modes:runLoopModes];
    }
}

/*
 * Class:     com_sun_glass_ui_Application
 * Method:    _invokeAndWait
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1invokeAndWait
(JNIEnv *env, jobject japplication, jobject jRunnable)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1invokeAndWait");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    if (jEnv != NULL)
    {
        GlassRunnable *runnable = [[GlassRunnable alloc] initWithRunnable:(*env)->NewGlobalRef(env, jRunnable)];
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES modes:runLoopModes];
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    staticScreen_getVideoRefreshPeriod
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_glass_ui_mac_MacApplication_staticScreen_1getVideoRefreshPeriod
(JNIEnv *env, jobject jApplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1getVideoRefreshPeriod");

    if (GlassDisplayLink != NULL)
    {
        double outRefresh = CVDisplayLinkGetActualOutputVideoRefreshPeriod(GlassDisplayLink);
        LOG("CVDisplayLinkGetActualOutputVideoRefreshPeriod: %f", outRefresh);
        return (outRefresh * 1000.0); // to millis
    }
    else
    {
        return 0.0;
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    staticScreen_getScreens
 * Signature: ()[Lcom/sun/glass/ui/Screen;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_mac_MacApplication_staticScreen_1getScreens
(JNIEnv *env, jobject jApplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1getScreens");

    jobjectArray screenArray = nil;

    GLASS_POOL_ENTER;
    {
        screenArray = createJavaScreens(env);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return screenArray;
}


/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _supportsSystemMenu
 * Signature: ()Z;
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacApplication__1supportsSystemMenu
(JNIEnv *env, jobject japplication)
{
    return !isEmbedded;
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _isTriggerReactivation
 * Signature: ()Z;
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacApplication__1isTriggerReactivation
(JNIEnv *env, jobject japplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1isTriggerReactivation");
    return triggerReactivation;
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _hide
 * Signature: ()V;
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1hide
(JNIEnv *env, jobject japplication)
{
    [NSApp hide:NSApp];
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _hideOtherApplications
 * Signature: ()V;
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1hideOtherApplications
(JNIEnv *env, jobject japplication)
{
    [NSApp hideOtherApplications:NSApp];
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _unhideAllApplications
 * Signature: ()V;
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1unhideAllApplications
(JNIEnv *env, jobject japplication)
{
    [NSApp unhideAllApplications:NSApp];
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _getDataDirectory
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacApplication__1getDataDirectory
(JNIEnv * env, jobject japplication)
{
    jstring string = nil;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES);
    if (paths && [paths count] > 0) {
        string = (*env)->NewStringUTF(jEnv, [[paths lastObject] UTF8String]);
    }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return string;
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _getMacKey
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacApplication__1getMacKey
(JNIEnv *env, jclass jClass, jint code)
{
    unsigned short macCode = 0;
    GetMacKey(code, &macCode);
    return (macCode & 0xFFFF);
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _getApplicationClassName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacApplication__1getApplicationClassName
(JNIEnv *env, jobject self)
{
    NSString* className = NSStringFromClass([[NSApplicationFX sharedApplication] class]);
    return (*env)->NewStringUTF(env, [className UTF8String]);
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    getPlatformPreferences
 * Signature: ()Ljava/util/Map;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacApplication_getPlatformPreferences
(JNIEnv *env, jobject self)
{
    return [PlatformSupport collectPreferences];
}
