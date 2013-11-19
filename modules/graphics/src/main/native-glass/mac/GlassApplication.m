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
#import "com_sun_glass_ui_Application.h"
#import "com_sun_glass_ui_mac_MacApplication.h"
#import "com_sun_glass_events_KeyEvent.h"


#import "GlassMacros.h"
#import "GlassApplication.h"
#import "GlassHelper.h"
#import "GlassKey.h"
#import "GlassScreen.h"
#import "GlassWindow.h"
#import "GlassTouches.h"
#import "RemoteLayerSupport.h"

#import "ProcessInfo.h"

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


jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
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
        GET_MAIN_JENV;
        if (env != NULL)
        {
            (*env)->CallVoidMethod(env, self->jRunnable, jRunnableRun);
            GLASS_CHECK_EXCEPTION(env);
        }

        [self release];
    }
    [pool drain];
}

- (void)dealloc
{
    GET_MAIN_JENV;
    if (env != NULL)
    {
        (*env)->DeleteGlobalRef(env, self->jRunnable);
    }
    self->jRunnable = NULL;

    [super dealloc];
}

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

    GET_MAIN_JENV;
    GlassScreenDidChangeScreenParameters(env);
}

- (void)applicationWillFinishLaunching:(NSNotification *)aNotification
{
    LOG("GlassApplication:applicationWillFinishLaunching");

    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        if (self->jLaunchable != NULL)
        {
            jclass runnableClass = [GlassHelper ClassForName:"java.lang.Runnable" withEnv:jEnv];
            if ((*env)->ExceptionCheck(env) == JNI_TRUE)
            {
                (*env)->ExceptionDescribe(env);
            }

            jmethodID runMethod = (*env)->GetMethodID(env, runnableClass, "run", "()V");
            if ((*env)->ExceptionCheck(env) == JNI_TRUE)
            {
                (*env)->ExceptionDescribe(env);
            }

            if ((runnableClass != 0) && (runMethod != 0))
            {
                (*env)->CallVoidMethod(env, self->jLaunchable, runMethod);
                if ((*env)->ExceptionCheck(env) == JNI_TRUE)
                {
                    (*env)->ExceptionDescribe(env);
                }
                else
                {
                    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(GlassApplicationDidChangeScreenParameters) name:NSApplicationDidChangeScreenParametersNotification object:nil];
                }
            }
            else if (runnableClass == 0)
            {
                NSLog(@"ERROR: Glass could not find Runnable class\n");
            }
            else //if (runMethod == 0)
            {
                NSLog(@"ERROR: Glass could not find run() method\n");
            }
        }

        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyWillFinishLaunchingMethod]);

        self->started = YES;
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
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
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        NSUInteger count = [filenames count];
        jobjectArray files = (*env)->NewObjectArray(env, (jsize)count, [GlassHelper ClassForName:"java.lang.String" withEnv:env], NULL);
        for (NSUInteger i=0; i<count; i++)
        {
            NSString *file = [filenames objectAtIndex:i];
            if (file != nil)
            {
                (*env)->SetObjectArrayElement(env, files, (jsize)i, (*env)->NewStringUTF(env, [file UTF8String]));
            }
        }
        (*env)->CallVoidMethod(env, self->jApplication, [GlassHelper ApplicationNotifyOpenFilesMethod], files);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);

    [theApplication replyToOpenOrPrint:NSApplicationDelegateReplySuccess];
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

    jint error = (*jVM)->AttachCurrentThread(jVM, (void **)&jEnv, NULL);
    //jint error = (*jVM)->AttachCurrentThreadAsDaemon(jVM, (void **)&jEnv, NULL);
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
                if (setNameMethod != NULL) {
                    (*jEnv)->CallVoidMethod(jEnv, glassApp->jApplication, setNameMethod, jname);
                }
                GLASS_CHECK_EXCEPTION(jEnv);
            }
        }

        // Determine if we're running embedded (in AWT, SWT, elsewhere)
        NSApplication *app = [NSApplication sharedApplication];
        isEmbedded = [app isRunning];

        if (!isEmbedded)
        {
            if (self->jTaskBarApp == JNI_TRUE)
            {
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
                    NSString *overridenPath = [NSString stringWithFormat:@"%s", path];
                    if ([[NSFileManager defaultManager] fileExistsAtPath:overridenPath isDirectory:NO] == YES)
                    {
                        iconPath = overridenPath;
                    }
                }
                if ([[NSFileManager defaultManager] fileExistsAtPath:iconPath isDirectory:NO] == NO)
                {
                    // try again using Java generic icon (this icon might go away eventually ?)
                    iconPath = [NSString stringWithFormat:@"%s", "/System/Library/Frameworks/JavaVM.framework/Resources/GenericApp.icns"];
                }

                NSImage *image = nil;
                {
                    if ([[NSFileManager defaultManager] fileExistsAtPath:iconPath isDirectory:NO] == YES)
                    {
                        image = [[NSImage alloc] initWithContentsOfFile:iconPath];
                    }
                    if (image == nil)
                    {
                        // last resort - if still no icon, then ask for an empty standard app icon, which is guranteed to exist
                        image = [[NSImage imageNamed:@"NSApplicationIcon"] retain];
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
                [app activateIgnoringOtherApps:YES];
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
                else
                {
                    // 10.6 or earlier: applets are not officially supported on 10.6 and earlier
                    // so they will have limited applet functionality (no active windows)
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

            jint err = (*jVM)->DetachCurrentThread(jVM);
            if (err < 0)
            {
                NSLog(@"Unable to detach from JVM. Error code: %d\n", (int)err);
            }

            jEnv = NULL;
        }
        else // event loop is not started
        {
            if ([NSThread isMainThread] == YES) {
                [glassApp applicationWillFinishLaunching: NULL];
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
    isFullScreenExitingLoop = YES;
    GET_MAIN_JENV;
    [GlassApplication enterNestedEventLoopWithEnv:env];
    isFullScreenExitingLoop = NO;
}

+ (void)leaveFullScreenExitingLoopIfNeeded
{
    if (!isFullScreenExitingLoop) {
        return;
    }
    GET_MAIN_JENV;
    [GlassApplication leaveNestedEventLoopWithEnv:env retValue:0L];
}

+ (void)registerKeyEvent:(NSEvent*)event
{
    if (!keyCodeForCharMap) {
        keyCodeForCharMap = [[NSMutableDictionary alloc] initWithCapacity:100];
        // Note: it's never released, just like, say, the jApplication reference...
    }
    [keyCodeForCharMap setObject:[NSNumber numberWithUnsignedShort:[event keyCode]] forKey:[event characters]];
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

@end

#pragma mark --- JNI

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacApplication__1initIDs
(JNIEnv *env, jclass jClass)
{
    LOG("Java_com_sun_glass_ui_mac_MacApplication__1initIDs");

    jApplicationClass = (*env)->NewGlobalRef(env, jClass);

    javaIDs.Application.createPixels = (*env)->GetStaticMethodID(
            env, jClass, "createPixels", "(II[IF)Lcom/sun/glass/ui/Pixels;");

    javaIDs.Application.getScaleFactor = (*env)->GetStaticMethodID(
            env, jClass, "getScaleFactor", "(IIII)F");

    javaIDs.Application.reportException = (*env)->GetStaticMethodID(
            env, jClass, "reportException", "(Ljava/lang/Throwable;)V");

    if (jRunnableRun == NULL)
    {
        jRunnableRun = (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Runnable"), "run", "()V");
    }
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
        [[NSApplication sharedApplication] performSelectorOnMainThread:@selector(stop:) withObject:nil waitUntilDone:NO];
        // wake up the runloop one last time
        [[NSApplication sharedApplication] performSelectorOnMainThread:@selector(hide:) withObject:nil waitUntilDone:NO];
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
        if (isFullScreenExitingLoop) {
            // If we ever hit this point (which is unlikely), then we must switch to
            // using the ui.EventLoop for the internal nested event loop
            fprintf(stderr, "ERROR in Glass: user's enterNestedEventLoop call while an internal nested event loop is spinning. Please file a bug against Glass.\n");
        }
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
        if (isFullScreenExitingLoop) {
            // If we ever hit this point (which is unlikely), then we must switch to
            // using the ui.EventLoop for the internal nested event loop
            fprintf(stderr, "ERROR in Glass: user's leaveNestedEventLoop call while an internal nested event loop is spinning. Please file a bug against Glass.\n");
        }
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
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:NO];
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
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES];
    }
}

/*
 * Class:     com_sun_glass_ui_mac_MacApplication
 * Method:    _getRemoteLayerServerName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacApplication__1getRemoteLayerServerName
(JNIEnv *env, jobject japplication)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getName");

    jstring name = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        static mach_port_t remoteLayerServerPort = MACH_PORT_NULL;
        if (remoteLayerServerPort == MACH_PORT_NULL)
        {
            remoteLayerServerPort = RemoteLayerStartServer();
        }
        NSString *remoteLayerServerName = RemoteLayerGetServerName(remoteLayerServerPort);
        name = (*env)->NewStringUTF(env, [remoteLayerServerName UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return name;
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

