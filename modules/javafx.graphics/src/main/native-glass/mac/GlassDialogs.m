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

#pragma mark --- Dispatcher

@interface DialogDispatcher : NSObject
{
    NSSavePanel *panel;
    NSWindow    *owner;
    NSInteger    button;
    jobject      eventLoop;
    NSArray     *m_filters;
}

- initWithPanel:(NSSavePanel*)panel owner:(NSWindow*)owner;
- (void)runModally;
- (NSInteger)getButton;

- (void)applyExtensions:(jobjectArray)jExtensionFilters withDefaultIndex:(jint)index withEnv:(JNIEnv*)env;
- (void)extensionFilterChanged:(NSPopUpButton*)sender;
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

    jclass eventLoopCls = [GlassHelper ClassForName:"com.sun.glass.ui.EventLoop" withEnv:env];
    if (!eventLoopCls) {
        return;
    }
    jobject jobj = (*env)->NewObject(env,
            eventLoopCls,
            javaIDs.EventLoop.init);
    if ((*env)->ExceptionCheck(env)) return;

    dd->eventLoop = (*env)->NewGlobalRef(env, jobj);

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

- (void)applyExtensions:(jobjectArray)jExtensionFilters withDefaultIndex:(jint)index withEnv:(JNIEnv*)env
{
    if (jExtensionFilters != NULL)
    {
        NSUInteger itemCount = (*env)->GetArrayLength(env, jExtensionFilters);
        if (itemCount > 0)
        {
            NSMutableArray *filters = [NSMutableArray arrayWithCapacity:itemCount];
            NSMutableArray *filterNames = [NSMutableArray arrayWithCapacity:itemCount];
            for (int i = 0; i < itemCount; i++)
            {
                jobject jFilter = (*env)->GetObjectArrayElement(env, jExtensionFilters, i);

                jstring jDescription = (*env)->CallObjectMethod(env, jFilter, javaIDs.ExtensionFilter.getDescription);
                [filterNames addObject:[GlassHelper nsStringWithJavaString:jDescription withEnv:env]];

                jobjectArray jExtensions = (jobjectArray)(*env)->CallObjectMethod(env, jFilter, javaIDs.ExtensionFilter.extensionsToArray);
                NSUInteger extensionCount = (NSUInteger)(*env)->GetArrayLength(env, jExtensions);
                NSMutableArray* extensions = [NSMutableArray arrayWithCapacity:extensionCount];
                for (int j = 0; j < extensionCount; j++)
                {
                    NSString* extension = [GlassHelper nsStringWithJavaString:(*env)->GetObjectArrayElement(env, jExtensions, j)
                                                                       withEnv:env];
                    [extensions addObject:[extension pathExtension]];
                }
                [filters addObject:extensions];
            }

            GLASS_CHECK_EXCEPTION(env);

            self->m_filters = filters;

            NSPopUpButton* filterView = [[[NSPopUpButton alloc] initWithFrame:NSZeroRect pullsDown:NO] autorelease];
            [self->panel setAccessoryView:filterView];
            [filterView setTarget:self];
            [filterView setAction:@selector(extensionFilterChanged:)];
            [filterView addItemsWithTitles:filterNames];
            [filterView selectItemAtIndex:index];
            [self extensionFilterChanged:filterView];
            [filterView sizeToFit];
        }
    }

}

-(void)extensionFilterChanged:(NSPopUpButton*)sender
{
    NSInteger index = [sender indexOfSelectedItem];
    if (index >= 0)
    {
        NSArray* extensions = [self->m_filters objectAtIndex:index];
        if ([extensions count] == 0 || [extensions containsObject:@"*"])
        {
            [self->panel setAllowedFileTypes:nil];
            //Clean up the added extensions when resetting to any file type.
            NSString* fileName = [[self->panel nameFieldStringValue] stringByDeletingPathExtension];
            [self->panel setNameFieldStringValue:fileName];
        }
        else
        {
            [self->panel setAllowedFileTypes:extensions];
        }
        [self->panel validateVisibleColumns];
    }
}

@end

static jobject convertNSURLtoFile(JNIEnv *env, NSURL *url)
{
#ifdef VERBOSE
    NSLog(@"   url: %@", [url path]);
#endif // VERBOSE
    NSData *data = [[url path] dataUsingEncoding:NSUTF16LittleEndianStringEncoding];
    jstring path = (*env)->NewString(env, (jchar *)[data bytes], data.length/2);

    jobject ret = NULL;

    // Make sure the class is initialized before using the methodIDs
    const jclass MacCommonDialogsCls = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacCommonDialogs" withEnv:env];
    if (!MacCommonDialogsCls) return NULL;

    // Performance doesn't matter here, so call the method every time
    jboolean result = (*env)->CallStaticBooleanMethod(env, MacCommonDialogsCls,
                javaIDs.MacCommonDialogs.isFileNSURLEnabled);
    GLASS_CHECK_EXCEPTION(env);
    if (result)
    {
        [url retain]; //NOTE: an app must call MacFileURL.dispoes() to release it

        const jclass MacFileNSURLCls = [GlassHelper ClassForName:"com.sun.glass.ui.mac.MacFileNSURL" withEnv:env];
        if (!MacFileNSURLCls) return NULL;
        ret = (*env)->NewObject(env,
                MacFileNSURLCls,
                javaIDs.MacFileNSURL.init, path, ptr_to_jlong(url));
        GLASS_CHECK_EXCEPTION(env);
        (*env)->DeleteLocalRef(env, MacFileNSURLCls);
    }
    else
    {
        jclass jcls = (*env)->FindClass(env, "java/io/File");
        GLASS_CHECK_EXCEPTION(env);
        ret = (*env)->NewObject(env,
                jcls,
                javaIDs.File.init, path);
        GLASS_CHECK_EXCEPTION(env);
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
 * Signature: (JJ)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1getBookmark
(JNIEnv *env, jobject jMacFileNSURL, jlong ptr, jlong baseDocumentPtr)
{
    NSURL * url = (NSURL*)jlong_to_ptr(ptr);
    NSURL * baseUrl = (NSURL*)jlong_to_ptr(baseDocumentPtr); // May be 0L
    jbyteArray data = NULL;

    GLASS_POOL_ENTER;
    {
        NSError *error = nil;

        NSData *nsData = [url bookmarkDataWithOptions:NSURLBookmarkCreationWithSecurityScope
                       includingResourceValuesForKeys:nil relativeToURL:baseUrl error:&error];

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
 * Signature: ([BJ)Lcom/sun/glass/ui/mac/MacFileNSURL;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacFileNSURL__1createFromBookmark
(JNIEnv *env, jclass cls, jbyteArray data, jlong baseDocumentPtr)
{
    jobject jMacFileNSURL = NULL;
    NSURL * baseUrl = (NSURL*)jlong_to_ptr(baseDocumentPtr); // May be 0L

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
                relativeToURL:baseUrl bookmarkDataIsStale:&isStale error:&error];

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
    if ((*env)->ExceptionCheck(env)) return;

    cls = [GlassHelper ClassForName:"com.sun.glass.ui.EventLoop" withEnv:env];
    if (!cls) {
        return;
    }
    javaIDs.EventLoop.init  = (*env)->GetMethodID(env, cls, "<init>", "()V");
    if ((*env)->ExceptionCheck(env)) return;
    javaIDs.EventLoop.enter = (*env)->GetMethodID(env, cls, "enter", "()Ljava/lang/Object;");
    if ((*env)->ExceptionCheck(env)) return;
    javaIDs.EventLoop.leave = (*env)->GetMethodID(env, cls, "leave", "(Ljava/lang/Object;)V");
    if ((*env)->ExceptionCheck(env)) return;

    initJavaIDsList(env);
    initJavaIDsArrayList(env);
    initJavaIDsFile(env);

    cls = [GlassHelper ClassForName:"com.sun.glass.ui.CommonDialogs$ExtensionFilter" withEnv:env];
    if (!cls) {
        return;
    }
    javaIDs.ExtensionFilter.getDescription = (*env)->GetMethodID(env, cls, "getDescription", "()Ljava/lang/String;");
    if ((*env)->ExceptionCheck(env)) return;
    javaIDs.ExtensionFilter.extensionsToArray  = (*env)->GetMethodID(env, cls, "extensionsToArray", "()[Ljava/lang/String;");
    if ((*env)->ExceptionCheck(env)) return;

    cls = [GlassHelper ClassForName:"com.sun.glass.ui.CommonDialogs$FileChooserResult" withEnv:env];
    if (!cls) {
        return;
    }
    javaIDs.FileChooserResult.init = (*env)->GetMethodID(env, cls, "<init>", "(Ljava/util/List;Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;)V");
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFileOpenChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;Z[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;I)Lcom.sun.glass.ui.CommonDialogs$FileChooserResult;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileOpenChooser
(JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jTitle, jboolean jMultipleMode, jobjectArray jExtensionFilters, jint defaultIndex)
{
    LOG("Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileOpenChooser");

    jobject result = NULL;

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

        jobject chosenFiles = NULL;
        jobject chosenFilter = NULL;

        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:(NSWindow*)jlong_to_ptr(owner)];
        [dispatcher applyExtensions:jExtensionFilters withDefaultIndex:defaultIndex withEnv:env];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:nil waitUntilDone:YES];
            NSArray *urls = [panel URLs];

            jclass jcls = (*env)->FindClass(env, "java/util/ArrayList");
            GLASS_CHECK_EXCEPTION(env);
            chosenFiles = (*env)->NewObject(env, jcls, javaIDs.ArrayList.init);
            GLASS_CHECK_EXCEPTION(env);

            if (([dispatcher getButton] == NSModalResponseOK) && ([urls count] > 0))
            {
                for (NSUInteger i=0; i<[urls count]; i++)
                {
                    NSURL *url = [urls objectAtIndex:i];
                    (*env)->CallBooleanMethod(env, chosenFiles, javaIDs.List.add, convertNSURLtoFile(env, url));
                    GLASS_CHECK_EXCEPTION(env);
                }
            }

            if (jExtensionFilters != NULL && (*env)->GetArrayLength(env, jExtensionFilters) > 0)
            {
                chosenFilter = (*env)->GetObjectArrayElement(env, jExtensionFilters,
                                                             [(NSPopUpButton*)[panel accessoryView] indexOfSelectedItem]);
                GLASS_CHECK_EXCEPTION(env);
            }
        }
        [dispatcher release];

        cls = [GlassHelper ClassForName:"com.sun.glass.ui.CommonDialogs$FileChooserResult" withEnv:env];
        if (!cls) return NULL;
        result = (*env)->NewObject(env, cls, javaIDs.FileChooserResult.init, chosenFiles, chosenFilter);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return result;
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFileSaveChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;I)Lcom.sun.glass.ui.CommonDialogs$FileChooserResult;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileSaveChooser
(JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jFilename, jstring jTitle, jobjectArray jExtensionFilters, jint defaultIndex)
{
    LOG("Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFileSaveChooser");

    jobject result = NULL;

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

        jobject chosenFile = NULL;
        jobject chosenFilter = NULL;

        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:(NSWindow*)jlong_to_ptr(owner)];
        [dispatcher applyExtensions:jExtensionFilters withDefaultIndex:defaultIndex withEnv:env];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:nil waitUntilDone:YES];
            NSURL *url = [panel URL];

            jclass jcls = (*env)->FindClass(env, "java/util/ArrayList");
            GLASS_CHECK_EXCEPTION(env);
            chosenFile = (*env)->NewObject(env, jcls, javaIDs.ArrayList.init);
            GLASS_CHECK_EXCEPTION(env);
            if (([dispatcher getButton] == NSModalResponseOK) && (url != nil))
            {
                (*env)->CallBooleanMethod(env, chosenFile, javaIDs.List.add, convertNSURLtoFile(env, url));
                GLASS_CHECK_EXCEPTION(env);
            }

            if (jExtensionFilters != NULL && (*env)->GetArrayLength(env, jExtensionFilters) > 0)
            {
                chosenFilter = (*env)->GetObjectArrayElement(env, jExtensionFilters,
                                                             [(NSPopUpButton*)[panel accessoryView] indexOfSelectedItem]);
                GLASS_CHECK_EXCEPTION(env);
            }
        }
        [dispatcher release];

        cls = [GlassHelper ClassForName:"com.sun.glass.ui.CommonDialogs$FileChooserResult" withEnv:env];
        if (!cls) return NULL;
        result = (*env)->NewObject(env, cls, javaIDs.FileChooserResult.init, chosenFile, chosenFilter);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return result;
}

/*
 * Class:     com_sun_glass_ui_mac_MacCommonDialogs
 * Method:    _showFolderChooser
 * Signature: (JLjava/lang/String;Ljava/lang/String;)Ljava/io/File;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_mac_MacCommonDialogs__1showFolderChooser
(JNIEnv *env, jclass cls, jlong owner, jstring jFolder, jstring jTitle)
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

        DialogDispatcher *dispatcher = [[DialogDispatcher alloc] initWithPanel:panel owner:(NSWindow*)jlong_to_ptr(owner)];
        {
            [dispatcher performSelectorOnMainThread:@selector(runModally) withObject:panel waitUntilDone:YES];
            NSArray *urls = [panel URLs];
            if (([dispatcher getButton] == NSModalResponseOK) && ([urls count] >= 1))
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
