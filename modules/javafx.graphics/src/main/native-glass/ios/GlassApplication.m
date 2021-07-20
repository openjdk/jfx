/*
 * Copyright (c) 2010, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import <UIKit/UIKit.h>

#import "GlassMacros.h"
#import "GlassApplication.h"
#import "GlassHelper.h"
#import "GlassStatics.h"
#import "GlassTimer.h"
#import "GlassWindow.h"
#import "GlassScreen.h"

pthread_key_t GlassThreadDataKey = 0;

// Java JNI IDs
JNIEnv *jEnv = NULL;

jclass mat_jIntegerClass = NULL;
jclass mat_jMapClass = NULL;
jclass mat_jBooleanClass = NULL;
jclass mat_jLongClass = NULL;

#if PROTECT_INVOKE_AND_WAIT
static jobject mat_eventThread = 0;
static jclass mat_jThreadClass = 0;
static jmethodID mat_ThreadCurrentThread = 0;
#endif

jmethodID mat_jVectorAddElement = 0;

jclass mat_jScreenClass = NULL;

jclass mat_jViewClass = NULL;

jclass jApplicationClass = NULL;
jmethodID jApplicationReportException = 0;

jmethodID mat_jViewNotifyResize = 0;
jmethodID mat_jViewNotifyRepaint = 0;
jmethodID mat_jViewNotifyKey = 0;
jmethodID mat_jViewNotifyMouse = 0;
jmethodID mat_jViewNotifyMenu = 0;
jmethodID mat_jViewNotifyInputMethod = 0;
jmethodID mat_jViewNotifyView = 0;

jmethodID mat_jMapGetMethod = 0;
jmethodID mat_jBooleanValueMethod = 0;
jmethodID mat_jIntegerValueMethod = 0;
jmethodID mat_jLongValueMethod = 0;

jfieldID mat_jViewWindow = 0;
jfieldID mat_jViewWidth = 0;
jfieldID mat_jViewHeight = 0;
jfieldID mat_jViewPtr = 0;

jclass mat_jWindowClass = NULL;

jfieldID mat_jWindowX = 0;
jfieldID mat_jWindowY = 0;
jfieldID mat_jWindowWidth = 0;
jfieldID mat_jWindowHeight = 0;
jfieldID mat_jWindowView = 0;
jfieldID mat_jWindowPtr = 0;

jmethodID mat_jWindowNotifyClose = 0;
jmethodID mat_jWindowNotifyDestroy = 0;
jmethodID mat_jWindowNotifyFocus = 0;
jmethodID mat_jWindowNotifyFocusDisabled = 0;
jmethodID jWindowNotifyFocusUngrab = 0;
jmethodID mat_jWindowNotifyMove = 0;
jmethodID mat_jWindowNotifyMoveToAnotherScreen = 0;
jmethodID mat_jWindowNotifyResize = 0;

jclass mat_jPixelsClass = NULL;

jfieldID mat_jPixelsWidth = 0;
jfieldID mat_jPixelsHeight = 0;
jfieldID mat_jPixelsBytes = 0;
jfieldID mat_jPixelsInts = 0;

jmethodID mat_jPixelsAttachData = 0;

jclass mat_jCursorClass = NULL;


// a unix pipe which we will use as a runnable queue for posted events.
static int postEventPipe[2];
static int haveIDs = 0;

static BOOL shouldKeepRunningNestedLoop = YES;
static jobject nestedLoopReturnValue = NULL;

//Library entrypoint
JNIEXPORT jint JNICALL
JNI_OnLoad_glass(JavaVM *vm, void *reserved)
{
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}

/*
 * Function to set the context class loader for the main glass event thread.
 * This is necessary because we co-opt the UIKit thread as the glass event
 * thread.
 */
jboolean setContextClassLoader(JNIEnv *env, jobject contextClassLoader)
{
    jclass threadCls = (*env)->FindClass(env, "java/lang/Thread");
    if ((*env)->ExceptionCheck(env) || threadCls == NULL) {
        return JNI_FALSE;
    }
    jmethodID currentThreadMID = (*env)->GetStaticMethodID(env, threadCls,
                                                           "currentThread", "()Ljava/lang/Thread;");
    if ((*env)->ExceptionCheck(env) || currentThreadMID == NULL) {
        return JNI_FALSE;
    }
    jobject jCurrentThread = (*env)->CallStaticObjectMethod(env, threadCls, currentThreadMID);
    if ((*env)->ExceptionCheck(env) || jCurrentThread == NULL) {
        return JNI_FALSE;
    }

    jmethodID setContextClassLoaderMID = (*env)->GetMethodID(env, threadCls,
                                                             "setContextClassLoader", "(Ljava/lang/ClassLoader;)V");
    if ((*env)->ExceptionCheck(env) || setContextClassLoaderMID == NULL) {
        return JNI_FALSE;
    }
    (*env)->CallVoidMethod(env, jCurrentThread, setContextClassLoaderMID, contextClassLoader);
    if ((*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}


/*
 * Function to find a glass class using the context class loader. All glass
 * classes must be looked up using this function rather than FindClass so that
 * the correct ClassLoader is used.
 *
 * Note that the className passed to this function must use "." rather than "/"
 * as a package separator.
 */
jclass classForName(JNIEnv *env, char *className)
{
    jclass threadCls = (*env)->FindClass(env, "java/lang/Thread");
    if ((*env)->ExceptionCheck(env) || threadCls == NULL) {
        return NULL;
    }
    jmethodID currentThreadMID = (*env)->GetStaticMethodID(env, threadCls,
                                                           "currentThread", "()Ljava/lang/Thread;");
    if ((*env)->ExceptionCheck(env) || currentThreadMID == NULL) {
        return NULL;
    }
    jobject jCurrentThread = (*env)->CallStaticObjectMethod(env, threadCls, currentThreadMID);
    if ((*env)->ExceptionCheck(env) || jCurrentThread == NULL) {
        return NULL;
    }

    jmethodID getContextClassLoaderMID = (*env)->GetMethodID(env, threadCls,
                                                             "getContextClassLoader", "()Ljava/lang/ClassLoader;");
    if ((*env)->ExceptionCheck(env) || getContextClassLoaderMID == NULL) {
        return NULL;
    }
    jobject contextClassLoader = (*env)->CallObjectMethod(env, jCurrentThread, getContextClassLoaderMID);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }

    jclass classCls = (*env)->FindClass(env, "java/lang/Class");
    if ((*env)->ExceptionCheck(env) || classCls == NULL) {
        return NULL;
    }
    jmethodID forNameMID = (*env)->GetStaticMethodID(env, classCls,
                                                     "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
    if ((*env)->ExceptionCheck(env) || forNameMID == NULL) {
        return NULL;
    }
    jstring classNameStr = (*env)->NewStringUTF(env, className);
    if ((*env)->ExceptionCheck(env) || classNameStr == NULL) {
        return NULL;
    }
    jclass theCls = (*env)->CallStaticObjectMethod(env, classCls, forNameMID,
                                                   classNameStr, JNI_TRUE, contextClassLoader);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return theCls;
}



@interface GlassRunnable : NSObject
{

}

@property (nonatomic) jobject jRunnable;

- (id)initWithRunnable:(jobject)runnable;
- (void)run;

@end



@implementation GlassRunnable

@synthesize jRunnable;

- (id)initWithRunnable:(jobject)runnable
{
    self.jRunnable = runnable;
    return self;
}


- (void)run
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
        if (jEnv != NULL)
        {
            (*jEnv)->CallVoidMethod(jEnv, self.jRunnable, jRunnableRun);
            GLASS_CHECK_EXCEPTION(jEnv);
        }

        [self release];
    }
    [pool drain];
}


- (void)dealloc
{
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    if (jEnv != NULL)
    {
        (*jEnv)->DeleteGlobalRef(jEnv, self.jRunnable);
    }
    self.jRunnable = NULL;

    [super dealloc];
}

@end



@implementation GlassApplication

@synthesize started;
@synthesize condition;
@synthesize jApplication;
@synthesize jLaunchable;
@synthesize jContextClassLoader;

- (id)initWithEnv:(JNIEnv*)env application:(jobject)application contextClassLoader:(jobject)contextClassLoader launchable:(jobject)launchable
{
    GLASS_LOG("GlassApplication_initWithEnv called.");
    self = [super init];
    if (self != nil)
    {
        self.condition = [[NSCondition alloc] init];
        [self.condition release]; //condition property retains; alloc as well
        self.started = NO;
        (*env)->GetJavaVM(env, &jVM);
        self.jApplication = (*env)->NewGlobalRef(env, application);
        if (launchable != NULL)
        {
            self.jLaunchable = (*env)->NewGlobalRef(env, launchable);
        }
        if (contextClassLoader != NULL)
        {
            self.jContextClassLoader = (*env)->NewGlobalRef(env, contextClassLoader);
        }
    }
    return self;
}

// Called e.g. by GlassViewController when app. user interface changes orientation.
// FX can resize Stage, adjust UI, etc. in response.
- (void)GlassApplicationDidChangeScreenParameters
{
    GLASS_LOG("GlassApplication_GlassApplicationDidChangeScreenParameters");
    GlassScreenDidChangeScreenParameters(jEnv);
}


// Application state changes callbacks to java

- (void)callWillResignActive
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyWillResignActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)callDidResignActive
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyDidResignActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)callDidReceiveMemoryWarning
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyDidReceiveMemoryWarningMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}


- (void)callWillBecomeActive
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyWillBecomeActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)callDidBecomeActive
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyDidBecomeActiveMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}

- (void)callQuit
{
    GET_MAIN_JENV;
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        (*env)->CallVoidMethod(env, self.jApplication, [GlassHelper ApplicationNotifyWillQuitMethod]);
    }
    [pool drain];
    GLASS_CHECK_EXCEPTION(env);
}


- (void)applicationCallback:(SEL)method
{
    if ([[NSThread currentThread] isMainThread] == YES)
    {
        [self performSelector:method];
    }
    else
    {
        [self performSelectorOnMainThread:method withObject:nil waitUntilDone:YES];
    }
}


- (void)applicationWillResignActive:(UIApplication *)application
{
    /*
     Sent when the application is about to move from active to inactive state. This can occur for certain types of temporary interruptions (such as an incoming phone call or SMS message) or when the user quits the application and it begins the transition to the background state.
     Use this method to pause ongoing tasks, disable timers, and throttle down OpenGL ES frame rates. Games should use this method to pause the game.
     */
    GLASS_LOG("GlassApplication:applicationWillResignActive");
    [self applicationCallback:@selector(callWillResignActive)];
}


- (void)applicationDidEnterBackground:(UIApplication *)application {
    GLASS_LOG("GlassApplication:applicationDidEnterBackground");
    [self applicationCallback:@selector(callDidResignActive)];
}

- (void)applicationDidReceiveMemoryWarning:(UIApplication *)application {
    GLASS_LOG("GlassApplication:applicationDidReceiveMemoryWarning");
    [self applicationCallback:@selector(callDidReceiveMemoryWarning)];
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    /*
     Called as part of  transition from the background to the inactive state: here you can undo many of the changes made on entering the background.
     */
    GLASS_LOG("GlassApplication:applicationWillEnterForeground");
    [self applicationCallback:@selector(callWillBecomeActive)];
}


- (void)applicationDidBecomeActive:(UIApplication *)application {
    /*
     Restart any tasks that were paused (or not yet started) while the application was inactive. If the application was previously in the background, optionally refresh the user interface.
     */
    GLASS_LOG("GlassApplication:applicationDidBecomeActive");
    [self applicationCallback:@selector(callDidBecomeActive)];
}


- (void)applicationWillTerminate:(UIApplication *)application {
    GLASS_LOG("GlassApplication:applicationWillTerminate");
    [self applicationCallback:@selector(callQuit)];
}


- (void)applicationWillFinishLaunching:(NSNotification *)aNotification
{
    GLASS_LOG("GlassApplication_applicationWillFinishLaunching");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    {
        if (self.jLaunchable != NULL)
        {
            jclass runnableClass = classForName(jEnv, "java.lang.Runnable");
            if ((*jEnv)->ExceptionCheck(jEnv) == JNI_TRUE)
            {
                (*jEnv)->ExceptionDescribe(jEnv);
            }

            jmethodID runMethod = (*jEnv)->GetMethodID(jEnv, runnableClass, "run", "()V");
            if ((*jEnv)->ExceptionCheck(jEnv) == JNI_TRUE)
            {
                (*jEnv)->ExceptionDescribe(jEnv);
            }

            if ((runnableClass != 0) && (runMethod != 0))
            {
                (*jEnv)->CallVoidMethod(jEnv, self.jLaunchable, runMethod);
                if ((*jEnv)->ExceptionCheck(jEnv) == JNI_TRUE)
                {
                    (*jEnv)->ExceptionDescribe(jEnv);
                }
            }
            else if (runnableClass == 0)
            {
                NSLog(@"ERROR: Glass could not find Runnable class");
            }
            else if (runMethod == 0)
            {
                NSLog(@"ERROR: Glass could not find run() method");
            }
        }

        [self notify];
    }
    [pool drain];

    GLASS_CHECK_EXCEPTION(jEnv);
}


- (void)runLoop:(id)selector
{
    GLASS_LOG("GlassApplication:runLoop");
    NSAssert([[NSThread currentThread] isMainThread] == YES, @"must be on main thread" );
    jint error = (*jVM)->AttachCurrentThread(jVM, (void **)&jEnv, NULL);
    GLASS_LOG("AttachCurrentThread returned %ld",error);


    if (error == 0)
    {
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        {
            if ([[NSThread currentThread] name] == nil) {
                [[NSThread currentThread] setName:@"Main UIKit Thread"];
            }

            GlassApplication *glassApp = (GlassApplication *) selector;

            // Set the context class loader for this thread
            if (!setContextClassLoader(jEnv, glassApp.jContextClassLoader)) {
                NSLog(@"ERROR: can't set the context classloader");
            }
            GLASS_CHECK_EXCEPTION(jEnv);

            // Load IosApplication class using the glass classloader
            jclass cls = [GlassHelper ClassForName:"com.sun.glass.ui.ios.IosApplication" withEnv:jEnv];
            if (!cls)
            {
                NSLog(@"ERROR: can't find the IosApplication class");
            }
            else
            {
                jmethodID setEventThreadMID = (*jEnv)->GetMethodID(jEnv, cls, "setEventThread", "()V");
                if (!setEventThreadMID)
                {
                    NSLog(@"ERROR: can't get IosApplication.setEventThread() method ID");
                }
                else
                {
                    (*jEnv)->CallVoidMethod(jEnv, glassApp->jApplication, setEventThreadMID);
                }
            }
            GLASS_CHECK_EXCEPTION(jEnv);

            //Set self as UIApplicationDelegate so we can pass life cycle notifications from iOS
            //to JavaFX
            [[UIApplication sharedApplication] setDelegate:glassApp];

            [glassApp performSelectorOnMainThread:@selector(applicationWillFinishLaunching:) withObject:NULL waitUntilDone:NO];
            GLASS_CHECK_EXCEPTION(jEnv);
        }
        [pool drain];
    }
    else // attaching to JVM failed
    {
        NSLog(@"ERROR: Glass could not attach to VM, result:%ld", error);
    }
}


+ (jobject)enterNestedEventLoopWithEnv:(JNIEnv*)env
{
    jobject ret = NULL;
    GLASS_LOG("entering nestedEventLoop");

    NSRunLoop *theRL = [NSRunLoop currentRunLoop];
    UIApplication * app = [UIApplication sharedApplication];
    shouldKeepRunningNestedLoop = YES;
    while (shouldKeepRunningNestedLoop && [theRL runMode:NSDefaultRunLoopMode
                                              beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.010]])
    {
        // don't do anything, as long as we should stay here events are forwarded.
    }
    GLASS_LOG("leaving enterNestedEventLoop");

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



- (void)notify
{
    [self.condition lock];
    [self setStarted: YES];
    [self.condition signal];
    [self.condition unlock];
}


-(void)await
{
    [self.condition lock];
    while(self.started == NO)
    {
        GLASS_LOG("<-->waiting for [glass started]");
        [self.condition wait];
    }
    [self.condition unlock];
}


- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver: self];
    self.condition = nil;
    [super dealloc];
}

@end



/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1initIDs
(JNIEnv *env, jclass jClass)
{

    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1initIDs");

    if (haveIDs)
        return;
    haveIDs = 1;

    assert(pthread_key_create(&GlassThreadDataKey, NULL) == 0);

    jApplicationClass = (*env)->NewGlobalRef(env, jClass);
    jApplicationReportException = (*env)->GetStaticMethodID(env, jClass, "reportException", "(Ljava/lang/Throwable;)V");

    mat_jIntegerClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Integer"));
    mat_jMapClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/util/Map"));
    mat_jBooleanClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Boolean"));
    mat_jLongClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Long"));

#if PROTECT_INVOKE_AND_WAIT
    mat_jThreadClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "java/lang/Thread"));
    mat_ThreadCurrentThread = (*env)->GetStaticMethodID(env, mat_jThreadClass, "currentThread", "()Ljava/lang/Thread;");
#endif

    mat_jVectorAddElement = (*env)->GetMethodID(env, (*env)->FindClass(env, "java/util/Vector"), "addElement", "(Ljava/lang/Object;)V");

    mat_jMapGetMethod = (*env)->GetMethodID(env, mat_jMapClass, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    mat_jBooleanValueMethod = (*env)->GetMethodID(env, mat_jBooleanClass, "booleanValue", "()Z");
    mat_jIntegerValueMethod = (*env)->GetMethodID(env, mat_jIntegerClass, "intValue", "()I");
    mat_jLongValueMethod = (*env)->GetMethodID(env, mat_jLongClass, "longValue", "()J");

    jRunnableRun = (*env)->GetMethodID(env, (*env)->FindClass(env, "java/lang/Runnable"), "run", "()V");

    // screen specific
    mat_jScreenClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/Screen"));
    GLASS_CHECK_EXCEPTION(env);

    // view specific
    mat_jViewClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/ios/IosView"));
    mat_jViewNotifyKey = (*env)->GetMethodID(env, mat_jViewClass, "notifyUnicode", "(IIII)V");
    jclass mat_jViewBaseClass = (*env)->FindClass(env, "com/sun/glass/ui/View");
    GLASS_CHECK_EXCEPTION(env);

    mat_jViewNotifyResize = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyResize", "(II)V");
    mat_jViewNotifyRepaint = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyRepaint", "(IIII)V");
    mat_jViewNotifyMouse = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyMouse", "(IIIIIIIZZ)V");
    mat_jViewNotifyMenu = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyMenu", "(IIIIZ)V");
    mat_jViewNotifyInputMethod = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyInputMethod", "(Ljava/lang/String;[I[I[BIII)V");
    mat_jViewNotifyView = (*env)->GetMethodID(env, mat_jViewBaseClass, "notifyView", "(I)V");
    GLASS_CHECK_EXCEPTION(env);


    if (jViewNotifyDragEnter == NULL)
    {
        jViewNotifyDragEnter = (*env)->GetMethodID(env, mat_jViewClass, "notifyDragEnter", "(IIIII)I");
    }

    if (jViewNotifyDragOver == NULL)
    {
        jViewNotifyDragOver = (*env)->GetMethodID(env, mat_jViewClass, "notifyDragOver", "(IIIII)I");
    }

    if (jViewNotifyDragLeave == NULL)
    {
        jViewNotifyDragLeave = (*env)->GetMethodID(env, mat_jViewClass, "notifyDragLeave", "()V");
    }

    if (jViewNotifyDragDrop == NULL)
    {
        jViewNotifyDragDrop = (*env)->GetMethodID(env, mat_jViewClass, "notifyDragDrop", "(IIIII)I");
    }

    if (jViewNotifyDragEnd == NULL)
    {
        jViewNotifyDragEnd = (*env)->GetMethodID(env, mat_jViewClass, "notifyDragEnd", "(I)V");
    }

    GLASS_CHECK_EXCEPTION(env);

    mat_jViewWidth = (*env)->GetFieldID(env, mat_jViewBaseClass, "width","I");
    mat_jViewHeight = (*env)->GetFieldID(env, mat_jViewBaseClass, "height","I");
    mat_jViewWindow = (*env)->GetFieldID(env, mat_jViewBaseClass, "window","Lcom/sun/glass/ui/Window;");
    GLASS_CHECK_EXCEPTION(env);

    mat_jViewPtr = (*env)->GetFieldID(env, mat_jViewBaseClass, "ptr", "J");
    GLASS_CHECK_EXCEPTION(env);

    //window specific
    mat_jWindowClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/ios/IosWindow"));
    jclass mat_jWindowBaseClass = (*env)->FindClass(env, "com/sun/glass/ui/Window");
    GLASS_CHECK_EXCEPTION(env);

    mat_jWindowX = (*env)->GetFieldID(env, mat_jWindowBaseClass, "x", "I");
    mat_jWindowY = (*env)->GetFieldID(env, mat_jWindowBaseClass, "y", "I");
    mat_jWindowWidth = (*env)->GetFieldID(env, mat_jWindowBaseClass, "width", "I");
    mat_jWindowHeight = (*env)->GetFieldID(env, mat_jWindowBaseClass, "height", "I");
    mat_jWindowPtr = (*env)->GetFieldID(env, mat_jWindowBaseClass, "ptr", "J");

    mat_jWindowView = (*env)->GetFieldID(env, mat_jWindowBaseClass, "view", "Lcom/sun/glass/ui/View;");
    GLASS_CHECK_EXCEPTION(env);

    mat_jWindowNotifyMove = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyMove", "(II)V");
    mat_jWindowNotifyResize = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyResize", "(III)V");
    mat_jWindowNotifyMoveToAnotherScreen = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
    mat_jWindowNotifyClose = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyClose", "()V");
    mat_jWindowNotifyFocus = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyFocus", "(I)V");
    mat_jWindowNotifyDestroy = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyDestroy", "()V");
    mat_jWindowNotifyFocusDisabled = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyFocusDisabled", "()V");
    jWindowNotifyFocusUngrab = (*env)->GetMethodID(env, mat_jWindowBaseClass, "notifyFocusUngrab", "()V");
    GLASS_CHECK_EXCEPTION(env);

    //pixels specific
    mat_jPixelsClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/ios/IosPixels"));
    jclass mat_jPixelsBaseClass = (*env)->FindClass(env, "com/sun/glass/ui/Pixels");
    GLASS_CHECK_EXCEPTION(env);

    mat_jPixelsWidth = (*env)->GetFieldID(env, mat_jPixelsBaseClass, "width", "I");
    mat_jPixelsHeight = (*env)->GetFieldID(env, mat_jPixelsBaseClass, "height", "I");
    mat_jPixelsBytes = (*env)->GetFieldID(env, mat_jPixelsBaseClass, "bytes", "Ljava/nio/ByteBuffer;");
    mat_jPixelsInts = (*env)->GetFieldID(env, mat_jPixelsBaseClass, "ints", "Ljava/nio/IntBuffer;");
    GLASS_CHECK_EXCEPTION(env);

    mat_jPixelsAttachData = (*env)->GetMethodID(env, mat_jPixelsBaseClass, "attachData", "(J)V");
    GLASS_CHECK_EXCEPTION(env);

    //cursor specific
    mat_jCursorClass = (*env)->NewGlobalRef(env, (*env)->FindClass(env, "com/sun/glass/ui/ios/IosCursor"));
    GLASS_CHECK_EXCEPTION(env);

    if (pipe(postEventPipe) != 0) {
        mat_JNU_ThrowByName(env, mat_RuntimeException, "Pipe allocation failed");
    }

    // display link timer
    NSObject<GlassTimerDelegate>  *delegate = [[GlassTimer alloc] init];
    [GlassTimer setDelegate: delegate];

    GLASS_LOG("leaving Java_com_sun_glass_ui_ios_IosApplication__1initIDs");
}


#if PROTECT_INVOKE_AND_WAIT
static jobject getCurrentThread(JNIEnv *env) {
    jobject jobj = (*env)->CallStaticObjectMethod(env, mat_jThreadClass, mat_ThreadCurrentThread);
    if ((*env)->ExceptionCheck(env)) {
        return NULL;
    }
    return jobj;
}
#endif


/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _runLoop
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1runLoop
(JNIEnv *env, jobject japplication, jobject jLaunchable, jobject jContextClassLoader)
{
    GLASS_LOG("Entering Java_com_sun_glass_ui_ios_IosApplication__1runLoop !!!");
    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        if ([[NSThread currentThread] isMainThread] == YES)
        {
            NSLog(@"\nWARNING: Glass was started on 1st thread and will block this thread.\nYou most likely do not want to do this - please remove \"-XstartOnFirstThread\" from VM arguments.");
        }
        else
        {
            GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1runLoop - not on main thread 2");

            if ([[NSThread currentThread] name] == nil)
            {
                GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1runLoop - setting name 'Main Java Thread' to current thread ");

                [[NSThread currentThread] setName:@"Main Java Thread"];
            }
        }

        GLASS_LOG("iOSApplication_runloop before glass init ... ");

        const GlassApplication * const glass = [[GlassApplication alloc]
            initWithEnv:env
            application:japplication
            contextClassLoader:jContextClassLoader
            launchable:jLaunchable];
        [glass performSelectorOnMainThread:@selector(runLoop:) withObject:glass waitUntilDone:[[NSThread currentThread] isMainThread]];


        // wait for UIKit to enter its UI runloop
        [glass await];

        // at this point Java main thread is allowed to proceed, but UIKit thread entered its run loop, so the VM will not quit
    }
    [glasspool drain];
    glasspool=nil;
    GLASS_CHECK_EXCEPTION(env);
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1runLoop ... returns");
}


/*
 *  * Class:     com_sun_glass_ui_ios_IosApplication
 *   * Method:    _enterNestedEventLoopImpl
 *    * Signature: ()Ljava/lang/Object;
 *     */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_ios_IosApplication__1enterNestedEventLoopImpl
(JNIEnv *env, jobject japplication)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1enterNestedEventLoopImpl");

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
 *  * Class:     com_sun_glass_ui_ios_IosApplication
 *   * Method:    _leaveNestedEventLoopImpl
 *    * Signature: (Ljava/lang/Object;)V
 *     */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1leaveNestedEventLoopImpl
(JNIEnv *env, jobject japplication, jobject retValue)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication__1leaveNestedEventLoopImpl");

    NSAutoreleasePool *glasspool = [[NSAutoreleasePool alloc] init];
    {
        [GlassApplication leaveNestedEventLoopWithEnv:env retValue:retValue];
    }
    [glasspool drain]; glasspool=nil;
     GLASS_CHECK_EXCEPTION(env);

}



/*
 * Class:     com_sun_glass_ui_Application
 * Method:    _invokeAndWait
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1invokeAndWait
(JNIEnv *env, jobject jApplication, jobject jRunnable)
{
    if (jEnv != NULL)
    {
        GlassRunnable *runnable = [[GlassRunnable alloc] initWithRunnable:(*env)->NewGlobalRef(env, jRunnable)];
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:YES];
        // released in the run method
    }
}


/*
 * Class:     com_sun_glass_ui_Application
 * Method:    _invokeLater
 * Signature: (Ljava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1invokeLater
(JNIEnv *env, jobject jApplication, jobject jRunnable)
{
    if (jEnv != NULL)
    {
        GlassRunnable *runnable = [[GlassRunnable alloc] initWithRunnable:(*env)->NewGlobalRef(env, jRunnable)];
        [runnable performSelectorOnMainThread:@selector(run) withObject:nil waitUntilDone:NO];
        // released in the run method
    }
}


char * mat_RuntimeException = "java/lang/RuntimeException";

void
mat_JNU_ThrowByName(JNIEnv *env, const char *name, const char *msg)
{
    GLASS_LOG("Throwing Exception ... %s",name);
    jclass cls = (*env)->FindClass(env, name);
    // if cls is NULL, an exception has already been thrown
    if (cls != NULL) {
        GLASS_LOG("Throwing Exception ....... %s(%s)",name,msg);
        (*env)->ThrowNew(env, cls, msg);
    } else {
        GLASS_LOG("EXCEPTION: not found %s(%s)", name, msg);
    }
    // free the local ref
    (*env)->DeleteLocalRef(env, cls);
}


/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _setStatusBarHidden
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1setStatusBarHidden
(JNIEnv *env, jclass clazz, jboolean hidden) {
    [UIApplication sharedApplication].statusBarHidden = (hidden == JNI_TRUE);
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _setStatusBarHiddenWithAnimation
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1setStatusBarHiddenWithAnimation
(JNIEnv *env, jclass clazz, jboolean hidden, jint animation) {
    [[UIApplication sharedApplication]
        setStatusBarHidden:hidden == JNI_TRUE
        withAnimation:(UIStatusBarAnimation)animation];
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _setStatusBarOrientationAnimated
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1setStatusBarOrientationAnimated
(JNIEnv *env, jclass clazz, jint interfaceOrientation, jboolean animated) {
    [[UIApplication sharedApplication]
        setStatusBarOrientation:(UIInterfaceOrientation)interfaceOrientation
        animated:animated == JNI_TRUE
    ];
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _setStatusBarStyleAnimated
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosApplication__1setStatusBarStyleAnimated
(JNIEnv *env, jclass clazz, jint statusBarStyle, jboolean animated) {
    [[UIApplication sharedApplication]
        setStatusBarStyle:(UIStatusBarStyle)statusBarStyle
        animated:animated == JNI_TRUE
    ];
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _getStatusBarHidden
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_ios_IosApplication__1getStatusBarHidden
(JNIEnv *env, jclass clazz) {
    return [UIApplication sharedApplication].statusBarHidden == YES ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _getStatusBarStyle
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosApplication__1getStatusBarStyle
(JNIEnv *env, jclass clazz) {
    return [UIApplication sharedApplication].statusBarStyle;
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    _getStatusBarStyle
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosApplication__1getStatusBarOrientation
(JNIEnv *env, jclass clazz) {
    return [UIApplication sharedApplication].statusBarOrientation;
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    staticScreen_getVideoRefreshPeriod
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL
Java_com_sun_glass_ui_ios_IosApplication_staticScreen_1getVideoRefreshPeriod
(JNIEnv *env, jobject jApplication)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication_staticScreen_1getVideoRefreshPeriod");

    double outRefresh = 1.0 / 30.0;     // ability to set frame divider
    return (outRefresh * 1000.0);       // to millis
}

/*
 * Class:     com_sun_glass_ui_ios_IosApplication
 * Method:    staticScreen_getScreens
 * Signature: ([Lcom/sun/glass/ui/Screen;)D
 */
JNIEXPORT jobjectArray JNICALL
Java_com_sun_glass_ui_ios_IosApplication_staticScreen_1getScreens
(JNIEnv *env, jobject jApplication)
{
    GLASS_LOG("Java_com_sun_glass_ui_ios_IosApplication_staticScreen_1getScreens");

    return createJavaScreens(env);
}

