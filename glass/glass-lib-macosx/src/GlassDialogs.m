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
#import "com_sun_glass_ui_mac_MacCommonDialogs.h"

#import "GlassMacros.h"
#import "GlassDialogs.h"
#import "GlassApplication.h"
#import "GlassHelper.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

// NSSavePanel is the super class for NSLoadPanel
static inline void applyExtensions(JNIEnv* env, jobjectArray jExtensions, NSSavePanel *panel)
{
    if (jExtensions != NULL)
    {
        int itemCount = (*env)->GetArrayLength(env, jExtensions);
        if (itemCount > 0)
        {
            NSMutableArray *types = [NSMutableArray arrayWithCapacity:(NSUInteger)itemCount];
            for (int i=0; i<itemCount; i++)
            {
                jstring type = (*env)->GetObjectArrayElement(env, jExtensions, i);
                NSString *string = [GlassHelper nsStringWithJavaString:type withEnv:env];
                [types addObject:[string pathExtension]];
            }
            [panel setAllowedFileTypes:types];
        }
    }
}

#pragma mark --- Dispatcher

@interface DialogDispatcher : NSObject
{
    NSSavePanel *panel;
    NSWindow    *owner;
    NSInteger   button;
    jobject     eventLoop;
}

- initWithPanel:(NSSavePanel*)panel owner:(NSWindow*)owner;
- (void)runModally;
- (NSInteger)getButton;

@end

@implementation DialogDispatcher

- (id)initWithPanel:(NSSavePanel*)p owner:(NSWindow*)o
{
    self = [super init];

    self->panel = p;
    self->owner = o;

    return self;
}

- (void)exitModalWithEnv:(JNIEnv*)env result:(NSInteger)result
{
    self->button = result;
    (*env)->CallVoidMethod(env, self->eventLoop, javaIDs.EventLoop.leave, NULL);
    GLASS_CHECK_EXCEPTION(env);
}

- (void)runModally
{
    GET_MAIN_JENV;
    DialogDispatcher *dd = self;

    dd->eventLoop = (*env)->NewGlobalRef(env, (*env)->NewObject(env,
            [GlassHelper ClassForName:"com.sun.glass.ui.EventLoop" withEnv:env],
            javaIDs.EventLoop.init));


    if (owner) {
        [panel beginSheetModalForWindow: owner completionHandler:^(NSInteger result)
        {
            [dd exitModalWithEnv:env result:result];
        }
        ];
    } else {
        [panel beginWithCompletionHandler:^(NSInteger result)
        {
            [dd exitModalWithEnv:env result:result];
        }
        ];
    }

    (*env)->CallObjectMethod(env, dd->eventLoop, javaIDs.EventLoop.enter);
    GLASS_CHECK_EXCEPTION(env);

    (*env)->DeleteGlobalRef(env, dd->eventLoop);
}

- (NSInteger)getButton
{
    return self->button;
}

@end

static jobject convertNSURLtoFile(JNIEnv *env, NSURL *url)
{
    LOG("   url: %s", [[url path] UTF8String]);
    jstring path = (*env)->NewStringUTF(env, [[url path] UTF8String]);

    jobject ret = NULL;

    // Make sure the class is initialized before using the methodIDs
    const jclass MacCommonDialogsCls = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacCommonDialogs" withEnv:env];

    // Performance doesn't matter here, so call the method every time
    if ((*env)->CallStaticBooleanMethod(env,
                MacCommonDialogsCls,
                javaIDs.MacCommonDialogs.isFileNSURLEnabled))
    {
        [url retain]; //NOTE: an app must call MacFileURL.dispoes() to release it

        const jclass MacFileNSURLCls = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacFileNSURL" withEnv:env];
        ret = (*env)->NewObject(env,
                MacFileNSURLCls,
                javaIDs.MacFileNSURL.init, path, ptr_to_jlong(url));
        (*env)->DeleteLocalRef(env, MacFileNSURLCls);
    }
    else
    {
        ret = (*env)->NewObject(env,
                (*env)->FindClass(env, "java/io/File"),
                javaIDs.File.init, path);
    }

    (*env)->DeleteLocalRef(env, MacCommonDialogsCls);
    return ret;
}

#pragma mark --- JNI

/* *********** MacFileNSURL *********** */

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1initIDs
(JNIEnv *env, jclass cls)
{
    javaIDs.MacFileNSURL.init = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/lang/String;J)V");
}

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _dispose
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1dispose
(JNIEnv *env, jobject jMacFileNSURL, jlong ptr)
{
    NSURL * url = (NSURL*)jlong_to_ptr(ptr);
    [url release];
}

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _startAccessingSecurityScopedResource
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1startAccessingSecurityScopedResource
(JNIEnv *env, jobject jMacFileNSURL, jlong ptr)
{
    NSURL * url = (NSURL*)jlong_to_ptr(ptr);
    return [url startAccessingSecurityScopedResource] ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _stopAccessingSecurityScopedResource
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1stopAccessingSecurityScopedResource
(JNIEnv *env, jobject jMacFileNSURL, jlong ptr)
{
    NSURL * url = (NSURL*)jlong_to_ptr(ptr);
    [url stopAccessingSecurityScopedResource];
}

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _getBookmark
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1getBookmark
(JNIEnv *env, jobject jMacFileNSURL, jlong ptr)
{
    NSURL * url = (NSURL*)jlong_to_ptr(ptr);
    jbyteArray data = NULL;
    
    GLASS_POOL_ENTER;
    {
        NSError *error = nil;

        NSData *nsData = [url bookmarkDataWithOptions:NSURLBookmarkCreationWithSecurityScope
                       includingResourceValuesForKeys:nil relativeToURL:nil error:&error];

        if (error) {
            NSLog(@"ERROR in Glass calling bookmarkDataWithOptions: %@", error);
        } else {
            const jsize len = (jsize)[nsData length];

            data = (*env)->NewByteArray(env, len);
            if (data && len) {
                void *pData = (*env)->GetPrimitiveArrayCritical(env, data, 0);
                if (pData) {
                    memcpy(pData, [nsData bytes], len);
                    (*env)->ReleasePrimitiveArrayCritical(env, data, pData, 0);
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return data;
}

/*
 * Class:     com_sun_glass_ui_mac_MacFileNSURL
 * Method:    _createFromBookmark
 * Signature: ([B)Lcom/sun/glass/ui/mac/MacFileNSURL;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1createFromBookmark
(JNIEnv *env, jclass cls, jbyteArray data)
{
    jobject jMacFileNSURL = NULL;

    GLASS_POOL_ENTER;
    {
        const jsize len = (*env)->GetArrayLength(env, data);

        NSData *nsData = NULL;

        void *pData = (*env)->GetPrimitiveArrayCritical(env, data, 0);
        if (pData) {
            nsData = [NSData dataWithBytes:pData length:len];
            (*env)->ReleasePrimitiveArrayCritical(env, data, pData, 0);
        }

        if (nsData) {
            BOOL isStale = NO;
            NSError *error = nil;

            NSURL *url = [NSURL URLByResolvingBookmarkData:nsData
                options:(NSURLBookmarkResolutionWithoutUI | NSURLBookmarkResolutionWithSecurityScope)
                relativeToURL:nil bookmarkDataIsStale:&isStale error:&error];

            if (isStale) {
                NSLog(@"URLByResolvingBookmarkData isStale=%d", isStale);
            }

            if (error) {
                NSLog(@"ERROR in Glass calling URLByResolvingBookmarkData: %@", error);
            } else {
                jMacFileNSURL = convertNSURLtoFile(env, url);
            } 
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return jMacFileNSURL;
}


/* *********** MacCommonDialogs *********** */

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1initIDs
(JNIEnv *env, jclass cls)
{
    javaIDs.MacCommonDialogs.isFileNSURLEnabled = (*env)->GetStaticMethodID(env, cls, "isFileNSURLEnabled", "()Z");

    cls = [GlassHelper ClassForName:"com.sun.glass.ui.EventLoop" withEnv:env];
    javaIDs.EventLoop.init  = (*env)->GetMethodID(env, cls, "<init>", "()V");
    javaIDs.EventLoop.enter = (*env)->GetMethodID(env, cls, "enter", "()Ljava/lang/Object;");
    javaIDs.EventLoop.leave = (*env)->GetMethodID(env, cls, "leave", "(Ljava/lang/Object;)V");

    initJavaIDsList(env);
    initJavaIDsArrayList(env);
    initJavaIDsFile(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFileOpenChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;Z[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileOpenChooser
(JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jTitle, jboolean jMultipleMode, jobjectArray jExtensionFilters)
{
    LOG("Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileOpenChooser");
    
    jobject chosen = NULL;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSOpenPanel *panel = [NSOpenPanel openPanel];
        [panel setAllowsMultipleSelection:(jMultipleMode==JNI_TRUE)];
        [panel setTitle:[GlassHelper nsStringWithJavaString:jTitle withEnv:env]];
        NSString *folder = [GlassHelper nsStringWithJavaString:jFolder withEnv:env];
        if ([folder length] > 0)
        {
            [panel setDirectoryURL:[NSURL fileURLWithPath:folder isDirectory:YES]];
        }
        
        [panel setResolvesAliases:YES];
        [panel setCanChooseFiles:YES];
        [panel setCanChooseDirectories:NO];
        [panel setShowsHiddenFiles:YES];
        [panel setExtensionHidden:NO];
        [panel setCanSelectHiddenExtension:YES];
        [panel setAllowsOtherFileTypes:NO];
        [panel setCanCreateDirectories:NO];
        applyExtensions(env, jExtensionFilters, panel);
        
        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:(NSWindow*)jlong_to_ptr(owner)];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:nil waitUntilDone:YES];
            NSArray *urls = [panel URLs];

            chosen = (*env)->NewObject(env, (*env)->FindClass(env, "java/util/ArrayList"), javaIDs.ArrayList.init);
            if (([dispatcher getButton] == NSFileHandlingPanelOKButton) && ([urls count] > 0))
            {
                for (NSUInteger i=0; i<[urls count]; i++)
                {
                    NSURL *url = [urls objectAtIndex:i];
                    (*env)->CallBooleanMethod(env, chosen, javaIDs.List.add, convertNSURLtoFile(env, url));
                }
            }
        }
        [dispatcher release];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return chosen;
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFileSaveChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;)Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileSaveChooser
(JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jFilename, jstring jTitle, jobjectArray jExtensionFilters)
{
    LOG("Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileSaveChooser");
    
    jobject chosen = NULL;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSSavePanel *panel = [NSSavePanel savePanel];
        [panel setTitle:[GlassHelper nsStringWithJavaString:jTitle withEnv:env]];
        NSString *folder = [GlassHelper nsStringWithJavaString:jFolder withEnv:env];
        if ([folder length] > 0)
        {
            [panel setDirectoryURL:[NSURL fileURLWithPath:folder isDirectory:YES]];
        }

        NSString *filename = [GlassHelper nsStringWithJavaString:jFilename withEnv:env];
        if ([filename length] > 0) {
            [panel setNameFieldStringValue:filename];
        }
        
        [panel setShowsHiddenFiles:YES];
        [panel setExtensionHidden:NO];
        [panel setCanSelectHiddenExtension:YES];
        [panel setAllowsOtherFileTypes:NO];
        [panel setCanCreateDirectories:YES];
        applyExtensions(env, jExtensionFilters, panel);
        
        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:(NSWindow*)jlong_to_ptr(owner)];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:nil waitUntilDone:YES];
            NSURL *url = [panel URL];

            chosen = (*env)->NewObject(env, (*env)->FindClass(env, "java/util/ArrayList"), javaIDs.ArrayList.init);
            if (([dispatcher getButton] == NSFileHandlingPanelOKButton) && (url != nil))
            {
                (*env)->CallBooleanMethod(env, chosen, javaIDs.List.add, convertNSURLtoFile(env, url));
            }
        }
        [dispatcher release];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return chosen;
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFolderChooser
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/io/File;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFolderChooser
(JNIEnv *env, jclass cls, jstring jFolder, jstring jTitle)
{
    LOG("Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFolderChooser");
    
    jobject chosen = NULL;
    
    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSOpenPanel *panel = [NSOpenPanel openPanel];
        [panel setTitle:[GlassHelper nsStringWithJavaString:jTitle withEnv:env]];
        NSString *folder = [GlassHelper nsStringWithJavaString:jFolder withEnv:env];
        if ([folder length] > 0)
        {
            [panel setDirectoryURL:[NSURL fileURLWithPath:folder isDirectory:YES]];
        }
        
        [panel setAllowsMultipleSelection:NO];
        [panel setResolvesAliases:YES];
        [panel setCanChooseFiles:NO];
        [panel setCanChooseDirectories:YES];
        [panel setShowsHiddenFiles:NO];
        [panel setExtensionHidden:YES];
        [panel setCanSelectHiddenExtension:NO];
        [panel setAllowsOtherFileTypes:NO];
        [panel setCanCreateDirectories:YES];
        
        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:nil];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:panel waitUntilDone:YES];
            NSArray *urls = [panel URLs];
            if (([dispatcher getButton] == NSFileHandlingPanelOKButton) && ([urls count] >= 1))
            {
                chosen = convertNSURLtoFile(env, [urls objectAtIndex:0]);
            }
        }
        [dispatcher release];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
    
    return chosen;
}
