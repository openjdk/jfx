/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_ui_mac_MacPasteboard.h"
#import "com_sun_glass_ui_Clipboard.h"

#import "GlassMacros.h"
#import "GlassPasteboard.h"
#import "GlassDragSource.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG(MSG, ...) GLASS_LOG(MSG, ## __VA_ARGS__);
#endif

static NSInteger lastDragSesionNumber = 0;

// Dock puts the data to a custom pasteboard, so dragging from it does not work.
// Copy the contents of the sender PBoard to the DraggingPBoard
void copyToDragPasteboardIfNeeded(id<NSDraggingInfo> sender)
{
    NSPasteboard* sourcePasteboard = [sender draggingPasteboard];
    if (![[sourcePasteboard name] isEqualToString:NSDragPboard] &&
        [sender draggingSequenceNumber] != lastDragSesionNumber)
    {
        lastDragSesionNumber = [sender draggingSequenceNumber];

        NSPasteboard* dragPasteboard = [NSPasteboard pasteboardWithName:NSDragPboard];
        [dragPasteboard clearContents];
        for (NSString* type in [sourcePasteboard types])
        {
            [dragPasteboard setData:[sourcePasteboard dataForType:type] forType:type];
        }
    }
}

static inline void DumpPasteboard(NSPasteboard *pasteboard)
{
    NSLog(@"\n");
    NSLog(@"DumpPasteboard");

    NSArray *items = [pasteboard pasteboardItems];
    if ([items count] > 0)
    {
        NSLog(@"---- [items count]: %d", (int)[items count]);
        for (NSUInteger i=0; i<[items count]; i++)
        {
            NSPasteboardItem *item = [items objectAtIndex:i];
            NSArray *types = [item types];
            for (NSUInteger j=0; j<[types count]; j++)
            {
                NSString *type = [types objectAtIndex:j];
                NSLog(@"--------- type: %@", type);

                NSUInteger length = 128;

                NSData *data = [item dataForType:type];
                NSString *string = [item stringForType:type];
                id representation = nil;
                if (string != nil)
                {
                    length = MIN(length, [string length]);
                    representation = [string substringToIndex:length];
                }
                else
                {
                    length = MIN(length, [data length]);
                    representation = [data subdataWithRange:NSMakeRange(0, length)];
                }
                NSLog(@"------------- data: %p [length: %d bytes] [first %d bytes rep: %@]", data, (int)[data length], (int)length, representation);
            }
        }
    }

    NSLog(@"\n");
}

static inline jbyteArray ByteArrayFromPixels(JNIEnv *env, void *data, size_t width, size_t height)
{
    jbyteArray javaArray = NULL;

    if (data != NULL)
    {
        jsize length = 4*(jsize)(width*height);

        javaArray = (*env)->NewByteArray(env, length + 4*(1+1)); // pixels + (width+height)
        GLASS_CHECK_EXCEPTION(env);

        if (javaArray != NULL)
        {
            jbyte *w = (jbyte*)&width;
            (*env)->SetByteArrayRegion(env, javaArray, 0, 1, (jbyte *)&w[3]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 1, 1, (jbyte *)&w[2]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 2, 1, (jbyte *)&w[1]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 3, 1, (jbyte *)&w[0]);
            GLASS_CHECK_EXCEPTION(env);

            jbyte *h = (jbyte*)&height;
            (*env)->SetByteArrayRegion(env, javaArray, 4, 1, (jbyte *)&h[3]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 5, 1, (jbyte *)&h[2]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 6, 1, (jbyte *)&h[1]);
            GLASS_CHECK_EXCEPTION(env);
            (*env)->SetByteArrayRegion(env, javaArray, 7, 1, (jbyte *)&h[0]);
            GLASS_CHECK_EXCEPTION(env);

            (*env)->SetByteArrayRegion(env, javaArray, 8, length, (jbyte *)data);
            GLASS_CHECK_EXCEPTION(env);
        }
    }

    return javaArray;
}

static inline jbyteArray ByteArrayFromNSData(JNIEnv *env, NSData *data)
{
    jbyteArray javaArray = NULL;

    if (data != nil)
    {
        javaArray = (*env)->NewByteArray(env, (jsize)[data length]);
        GLASS_CHECK_EXCEPTION(env);

        if (javaArray != NULL)
        {
            (*env)->SetByteArrayRegion(env, javaArray, 0, (jsize)[data length], (jbyte *)[data bytes]);
            GLASS_CHECK_EXCEPTION(env);
        }
    }

    return javaArray;
}

static inline void SetNSPasteboardItemValueForUtf(JNIEnv *env, NSPasteboardItem *item, jobject jValue, NSString *utf)
{
    BOOL isString = NO;
    if ([utf isEqualToString:NSPasteboardTypeString] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypePDF] == YES)
    {
        isString = YES;
    }
    if ([utf isEqualToString:NSPasteboardTypeRTF] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypeRTFD] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypeHTML] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypeTabularText] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypeMultipleTextSelection] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:NSPasteboardTypeFindPanelSearchOptions] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypeURL] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypeFileURL] == YES)
    {
        isString = YES;
    }

    if (isString == YES)
    {
        NSString *string = nil;
        {
            const jchar *chars = (*env)->GetStringChars(env, jValue, NULL);
            // 'string' must never be nil
            string = [NSString stringWithCharacters:(UniChar *)chars length:(NSUInteger)(*env)->GetStringLength(env, jValue)];
            (*env)->ReleaseStringChars(env, jValue, chars);
        }
        [item setString:string forType:utf];
        //NSLog(@"                SetValue(string): %@, ForUtf: %@", string, utf);
    }
    else
    {
        if ([utf isEqualToString:RAW_IMAGE_MIME] == YES || [utf isEqualToString:DRAG_IMAGE_MIME])
        {
            NSImage *image = NULL;
            (*env)->CallVoidMethod(env, jValue, jPixelsAttachData, ptr_to_jlong(&image));
            if (image != NULL)
            {
                NSData *data = [image TIFFRepresentation];
                [item setData:data forType: [utf isEqualToString:RAW_IMAGE_MIME] ? NSPasteboardTypeTIFF : DRAG_IMAGE_MIME];
                //NSLog(@"                setData: %p, ForUtf: %@", data, utf);
            }
        }
        else if ([utf isEqualToString:DRAG_IMAGE_OFFSET] == YES)
        {
            NSPoint offset = NSZeroPoint;
            jbyte *array =  (*env)->GetByteArrayElements(env, jValue, 0);
            if (array != nil) {
                if (sizeof(array) == sizeof(jint) * 2) {
                    jint x = CFSwapInt32BigToHost(((jint *)array)[0]);
                    jint y = CFSwapInt32BigToHost(((jint *)array)[1]);
                    offset = NSMakePoint((float)x, (float)y);
                }
                (*env)->ReleaseByteArrayElements(env, jValue, array, 0);
            }

            [item setString:NSStringFromPoint(offset) forType:DRAG_IMAGE_OFFSET];
        }
        else
        {
            NSData *data = nil;
            {
                jbyte *bytes = (*env)->GetByteArrayElements(env, jValue, NULL);
                // 'data' must never be nil
                data = [NSData dataWithBytes:bytes length:(NSUInteger)(*env)->GetArrayLength(env, jValue)];
                (*env)->ReleaseByteArrayElements(env, jValue, bytes, 0);
            }
            [item setData:data forType:utf];
            //NSLog(@"                SetValue(data): %p, ForUtf: %@", data, utf);
        }
    }
    GLASS_CHECK_EXCEPTION(env);
}

static inline NSPasteboardItem *NSPasteboardItemFromArray(JNIEnv *env, jobjectArray jArray)
{
    NSPasteboardItem *item = [[[NSPasteboardItem alloc] init] autorelease];
    {
        jsize repsCount = (*env)->GetArrayLength(env, jArray);
        //NSLog(@"        NSPasteboardItemFromArray repsCount: %d", repsCount);
        if (repsCount > 0)
        {
            for (int i=0; i<repsCount; i++)
            {
                jobjectArray jRepresentation = (*env)->GetObjectArrayElement(env, jArray, i);
                if ((*env)->GetArrayLength(env, jRepresentation) == 2)
                {
                    jstring jUtf = (*env)->GetObjectArrayElement(env, jRepresentation, com_sun_glass_ui_mac_MacPasteboard_UtfIndex);
                    jobject jObject = (*env)->GetObjectArrayElement(env, jRepresentation, com_sun_glass_ui_mac_MacPasteboard_ObjectIndex);

                    NSString *utf = nil;
                    {
                        const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
                        jsize length = (*env)->GetStringLength(env, jUtf);
                        if (length > 0)
                        {
                            utf = [NSString stringWithCharacters:(UniChar *)chars length:(NSUInteger)length];
                        }
                        (*env)->ReleaseStringChars(env, jUtf, chars);
                    }
                    SetNSPasteboardItemValueForUtf(env, item, jObject, utf);
                }
                else
                {
                    NSLog(@"Glass error: NSPasteboardItemFromArray found bad item with %d entries", (int)(*env)->GetArrayLength(env, jRepresentation));
                }
            }
        }
    }
    return item;
}

static inline jobject createUTF(JNIEnv *env, NSString *data) {
    jclass jcls = (*env)->FindClass(env, "java/lang/String");
    GLASS_CHECK_EXCEPTION(env);
    jmethodID String_init_ID = (*env)->
                    GetMethodID(env, jcls, "<init>", "([BLjava/lang/String;)V");
    GLASS_CHECK_EXCEPTION(env);
    NSUInteger len = [data lengthOfBytesUsingEncoding:NSUTF8StringEncoding];
    jbyteArray ba = (*env)->NewByteArray(env, len);
    GLASS_CHECK_EXCEPTION(env);
    (*env)->SetByteArrayRegion(env, ba, 0, len, (jbyte *)[data UTF8String]);
    jstring charset = (*env)->NewStringUTF(env, "UTF-8");
    GLASS_CHECK_EXCEPTION(env);
    jobject jdata = (*env)->NewObject(env, jcls, String_init_ID, ba, charset);
    GLASS_CHECK_EXCEPTION(env);
    (*env)->DeleteLocalRef(env, charset);
    (*env)->DeleteLocalRef(env, jcls);
    return jdata;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1initIDs
(JNIEnv *env, jclass jPasteboard)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1initIDs");

//    NSLog(@"NSPasteboardTypeString: %@", NSPasteboardTypeString);
//    NSLog(@"NSPasteboardTypePDF: %@", NSPasteboardTypePDF);
//    NSLog(@"NSPasteboardTypeTIFF: %@", NSPasteboardTypeTIFF);
//    NSLog(@"NSPasteboardTypePNG: %@", NSPasteboardTypePNG);
//    NSLog(@"NSPasteboardTypeRTF: %@", NSPasteboardTypeRTF);
//    NSLog(@"NSPasteboardTypeRTFD: %@", NSPasteboardTypeRTFD);
//    NSLog(@"NSPasteboardTypeHTML: %@", NSPasteboardTypeHTML);
//    NSLog(@"NSPasteboardTypeTabularText: %@", NSPasteboardTypeTabularText);
//    NSLog(@"NSPasteboardTypeFont: %@", NSPasteboardTypeFont);
//    NSLog(@"NSPasteboardTypeColor: %@", NSPasteboardTypeColor);
//    NSLog(@"NSPasteboardTypeSound: %@", NSPasteboardTypeSound);
//    NSLog(@"NSPasteboardTypeMultipleTextSelection: %@", NSPasteboardTypeMultipleTextSelection);
//    NSLog(@"NSPasteboardTypeFindPanelSearchOptions: %@", NSPasteboardTypeFindPanelSearchOptions);
//    NSLog(@"kUTTypeURL: %@", (NSString*)kUTTypeURL);
//    NSLog(@"kUTTypeFileURL: %@", (NSString*)kUTTypeFileURL);
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _createSystemPasteboard
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1createSystemPasteboard
(JNIEnv *env, jobject jPasteboard, jint jType)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1createSystemPasteboard: %d", jType);

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = nil;

        switch (jType)
        {
            case com_sun_glass_ui_mac_MacPasteboard_General:
                pasteboard = [NSPasteboard pasteboardWithName:NSGeneralPboard];
                break;
            case com_sun_glass_ui_mac_MacPasteboard_DragAndDrop:
                pasteboard = [NSPasteboard pasteboardWithName:NSDragPboard];
                break;
        }

        ptr = ptr_to_jlong(pasteboard);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _createUserPasteboard
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1createUserPasteboard
(JNIEnv *env, jobject jPasteboard, jstring jName)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1createUserPasteboard");

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSString *name = nil;
        {
            const jchar *chars = (*env)->GetStringChars(env, jName, NULL);
            jsize length = (*env)->GetStringLength(env, jName);
            if (length > 0)
            {
                name = [NSString stringWithCharacters:(UniChar *)chars length:(NSUInteger)length];
            }
            (*env)->ReleaseStringChars(env, jName, chars);
        }
        NSPasteboard *pasteboard = [[NSPasteboard pasteboardWithName:name] retain];
        ptr = ptr_to_jlong(pasteboard);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getName
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getName");

    jstring name = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        NSString *string = [pasteboard name];
        name = (*env)->NewStringUTF(env, [string UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return name;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getUTFs
 * Signature: (J)[[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getUTFs
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getUTFs");

    jobjectArray utfs = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        //DumpPasteboard(pasteboard);

        NSArray *items = [pasteboard pasteboardItems];
        if ([items count] > 0)
        {
            jclass jcls = (*env)->FindClass(env, "[Ljava/lang/String;");
            GLASS_CHECK_EXCEPTION(env);
            utfs = (*env)->NewObjectArray(env, (jsize)[items count], jcls, NULL);
            GLASS_CHECK_EXCEPTION(env);
            for (NSUInteger i=0; i<[items count]; i++)
            {
                NSPasteboardItem *item = [items objectAtIndex:i];

                NSArray *types = [item types];
                if ([types count] > 0)
                {
                    jcls = (*env)->FindClass(env, "java/lang/String");
                    GLASS_CHECK_EXCEPTION(env);
                    jobjectArray array = (*env)->NewObjectArray(env, (jsize)[types count], jcls, NULL);
                    GLASS_CHECK_EXCEPTION(env);
                    for (NSUInteger j=0; j<[types count]; j++)
                    {
                        NSString *type = [types objectAtIndex:j];
                        //id property = [item stringForType:type];
                        //if (property != nil) // allow null as the platform itself does
                        {
                            (*env)->SetObjectArrayElement(env,
                                         array, (jsize)j, createUTF(env, type));
                            GLASS_CHECK_EXCEPTION(env);
                        }
                    }
                    (*env)->SetObjectArrayElement(env, utfs, (jsize)i, array);
                    GLASS_CHECK_EXCEPTION(env);
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return utfs;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getItemAsRawImage
 * Signature: (JI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getItemAsRawImage
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getItemAsRawImage");

    jbyteArray bytes = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (jIndex >= 0)
        {
            NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
            NSArray *items = [pasteboard pasteboardItems];

            NSPasteboardItem *item = [items objectAtIndex:(NSUInteger)jIndex];
            if (item != nil)
            {
                // since this is a convenience method we'll try to do our best to return an image for this item
                // by trying all the following types if needed
                NSArray *utfs = [NSArray arrayWithObjects:
                                 NSPasteboardTypeTIFF,
                                 NSPasteboardTypePNG,
                                 NSPasteboardTypePDF,
                                 nil];

                NSData *data = nil;
                for (NSUInteger i=0; i<[utfs count]; i++)
                {
                    data = [item dataForType:[utfs objectAtIndex:i]];
                    if (data != nil)
                    {
                        break;
                    }
                }

                NSImage *image = [[[NSImage alloc] initWithData:data] autorelease];
                if (image == nil)
                {
                    // if no image yet, try by referencing file url of this item (if it exists)
                    NSString *file = [item stringForType:(NSString*)kUTTypeURL];
                    if (file != nil)
                    {
                        NSURL *url = [NSURL URLWithString:file];
                        image = [[[NSImage alloc] initByReferencingURL:url] autorelease];
                    }
                }
                if (image == nil)
                {
                    // if no image yet, try by referencing file url of this item (if it exists)
                    NSString *file = [item stringForType:(NSString*)kUTTypeFileURL];
                    if (file != nil)
                    {
                        NSURL *url = [NSURL URLWithString:file];
                        image = [[[NSImage alloc] initByReferencingURL:url] autorelease];
                    }
                }

#if 0
                // last try: if no image yet, try asking the pastebard for one (it's not per item though)
                if ((image == nil) && ([NSImage canInitWithPasteboard:pasteboard]))
                {
                    image = [[[NSImage alloc] initWithPasteboard:pasteboard] autorelease];
                }
#endif

                if (image != nil)
                {
                    CGImageRef cgImage = [image CGImageForProposedRect:NULL context:nil hints:nil];

                    size_t width = CGImageGetWidth(cgImage);
                    size_t height = CGImageGetHeight(cgImage);
                    uint32_t *pixels = malloc(4*width*height);
                    if (pixels != NULL)
                    {
                        CGColorSpaceRef space = CGColorSpaceCreateDeviceRGB();
                        CGContextRef ctx = CGBitmapContextCreate(pixels, width, height, 8, 4*width, space, kCGImageAlphaPremultipliedFirst|kCGBitmapByteOrder32Little);
                        CGContextSetBlendMode(ctx, kCGBlendModeCopy);
                        CGContextDrawImage(ctx, CGRectMake(0, 0, width, height), cgImage);
                        CGContextFlush(ctx);

                        bytes = ByteArrayFromPixels(env, pixels, width, height);

                        CGColorSpaceRelease(space);
                        free(pixels);
                    }
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return bytes;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getItemStringForUTF
 * Signature: (JILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getItemStringForUTF
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex, jstring jUtf)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getItemStringForUTF");

    jstring string = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (jIndex >= 0)
        {
            NSString *utf = nil;
            {
                const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
                jsize length = (*env)->GetStringLength(env, jUtf);
                if (length > 0)
                {
                    utf = [NSString stringWithCharacters:(UniChar *)chars length:(NSUInteger)length];
                }
                (*env)->ReleaseStringChars(env, jUtf, chars);
            }
            if (utf != nil)
            {
                NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
                NSArray *items = [pasteboard pasteboardItems];
                NSPasteboardItem *item = [items objectAtIndex:(NSUInteger)jIndex];
                if (item != nil)
                {
                    NSString *str = [item stringForType:utf];
                    if (str != nil)
                    {
                        string = createUTF(env, str);
                    }
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return string;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getItemBytesForUTF
 * Signature: (JILjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getItemBytesForUTF
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex, jstring jUtf)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getItemBytesForUTF");

    jbyteArray bytes = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        if (jIndex >= 0)
        {
            NSString *utf = nil;
            {
                const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
                jsize length = (*env)->GetStringLength(env, jUtf);
                if (length > 0)
                {
                    utf = [NSString stringWithCharacters:(UniChar *)chars length:(NSUInteger)length];
                }
                (*env)->ReleaseStringChars(env, jUtf, chars);
            }
            if (utf != nil)
            {
                NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
                NSArray *items = [pasteboard pasteboardItems];
                NSPasteboardItem *item = [items objectAtIndex:(NSUInteger)jIndex];

                NSData *data = [item dataForType:utf];
                bytes = ByteArrayFromNSData(env, data);
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return bytes;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _putItemsFromArray
 * Signature: (J[Ljava/lang/Object;I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1putItemsFromArray
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jobjectArray jObjects, jint supportedActions)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1putItemsFromArray");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        seed = [pasteboard clearContents];

        jsize itemCount = (*env)->GetArrayLength(env, jObjects);
        //NSLog(@"Java_com_sun_glass_ui_mac_MacPasteboard__1putItems itemCount: %d", itemCount);
        if (itemCount > 0)
        {
            NSMutableArray *objects = [NSMutableArray arrayWithCapacity:(NSUInteger)itemCount];
            for (int i=0; i<itemCount; i++)
            {
                jobject array = (*env)->GetObjectArrayElement(env, jObjects, i);
                GLASS_CHECK_EXCEPTION(env);
                if (array != NULL)
                {
                    NSPasteboardItem *item = NSPasteboardItemFromArray(env, array);
                    [objects addObject:item];
                }
            }

            // http://developer.apple.com/library/mac/#documentation/cocoa/Conceptual/PasteboardGuide106/Articles/pbCustom.html
            [pasteboard writeObjects:objects];

            if (pasteboard == [NSPasteboard pasteboardWithName:NSDragPboard])
            {
                [GlassDragSource flushWithMask:supportedActions];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _clear
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1clear
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1clear");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        seed = [pasteboard clearContents];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getSeed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getSeed
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getSeed");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        seed = [pasteboard changeCount];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1release
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1release");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        [pasteboard releaseGlobally];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_mac_MacPasteboard
 * Method:    _getAllowedOperation
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_mac_MacPasteboard__1getAllowedOperation
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG("Java_com_sun_glass_ui_mac_MacPasteboard__1getAllowedOperation");

    jint mask = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        NSPasteboard *pasteboard = (NSPasteboard*)jlong_to_ptr(jPtr);
        if (pasteboard == [NSPasteboard pasteboardWithName:NSDragPboard])
        {
            // retrieve the mask for DnD
            mask = [GlassDragSource getSupportedActions];
        }
        else
        {
            // we can always copy from a NSPasteboard
            mask = com_sun_glass_ui_Clipboard_ACTION_COPY;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return mask;
}
