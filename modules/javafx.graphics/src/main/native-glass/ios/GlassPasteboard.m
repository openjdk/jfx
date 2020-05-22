/*
 * Copyright (c) 2012, 2014, Oracle and/or its affiliates. All rights reserved.
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
#import "com_sun_glass_ui_ios_IosPasteboard.h"
#import "com_sun_glass_ui_Clipboard.h"

#import "GlassMacros.h"
#import "GlassPasteboard.h"
#import "GlassDragDelegate.h"

//#define VERBOSE
#ifndef VERBOSE
    #define LOG(MSG, ...)
#else
    #define LOG NSLog
#endif

#define RAW_IMAGE_MIME @"application.x-java-rawimage"

#ifdef VERBOSE

/**
 * Dumps content of the pasteboard.
 */
static inline void DumpPasteboard(UIPasteboard *pasteboard)
{

    NSLog(@"\n");
    NSLog(@"DumpPasteboard");

    NSArray *items = [pasteboard items];
    if ([items count] > 0)
    {
        NSLog(@"---- [items count]: %d", (int)[items count]);
        for (int i=0; i<[items count]; i++)
        {
            NSDictionary *item = [items objectAtIndex:i];
            NSArray *types = [item allKeys];

            for (int j=0; j<[types count]; j++)
            {
                NSString *type = [types objectAtIndex:j];
                NSLog(@"--------- type: %@", type);

                NSUInteger length = 128;
                NSObject *data = [item valueForKey:type];
                NSString *string = nil;
                if ([data isKindOfClass:[NSString class]] == YES) {
                    string = (NSString *) data;
                }
                id representation = nil;
                if (string != nil)
                {
                    length = MIN(length, [string length]);
                    representation = [string substringToIndex:length];
                    NSLog(@"------------- data (NSString):[ %@]", representation);
                }
                else if ([data isKindOfClass:[NSData class]] == YES)
                {
                    NSData *nsData = (NSData*)data;
                    length = MIN(length, [nsData length]);
                    representation = [nsData subdataWithRange:NSMakeRange(0, length)];
                    NSLog(@"------------- data: %p [length: %d bytes] [first %d bytes rep: %@]", nsData, (int)[nsData length], (int)length, representation);
                }
            }
        }
    }

    NSLog(@"\n");
}
#endif //VERBOSE

static inline jbyteArray ByteArrayFromPixels(JNIEnv *env, void *data, int width, int height)
{
    jbyteArray javaArray = NULL;

    if ((data != NULL) && (width > 0) && (height > 0))
    {
        javaArray = (*env)->NewByteArray(env, 4*(width*height) + 4*(1+1));
        GLASS_CHECK_EXCEPTION(env);

        if (javaArray != NULL)
        {
            jbyte *w = (jbyte*)&width;
            (*env)->SetByteArrayRegion(env, javaArray, 0, 1, (jbyte *)&w[3]);
            (*env)->SetByteArrayRegion(env, javaArray, 1, 1, (jbyte *)&w[2]);
            (*env)->SetByteArrayRegion(env, javaArray, 2, 1, (jbyte *)&w[1]);
            (*env)->SetByteArrayRegion(env, javaArray, 3, 1, (jbyte *)&w[0]);

            jbyte *h = (jbyte*)&height;
            (*env)->SetByteArrayRegion(env, javaArray, 4, 1, (jbyte *)&h[3]);
            (*env)->SetByteArrayRegion(env, javaArray, 5, 1, (jbyte *)&h[2]);
            (*env)->SetByteArrayRegion(env, javaArray, 6, 1, (jbyte *)&h[1]);
            (*env)->SetByteArrayRegion(env, javaArray, 7, 1, (jbyte *)&h[0]);

            (*env)->SetByteArrayRegion(env, javaArray, 8, 4*(width*height), (jbyte *)data);
        }
    }

    return javaArray;
}

static inline jbyteArray ByteArrayFromNSData(JNIEnv *env, NSData *data)
{
    jbyteArray javaArray = NULL;

    if (data != nil)
    {
        javaArray = (*env)->NewByteArray(env, [data length]);
        GLASS_CHECK_EXCEPTION(env);

        if (javaArray != NULL)
        {
            (*env)->SetByteArrayRegion(env, javaArray, 0, [data length], (jbyte *)[data bytes]);
        }
    }

    return javaArray;
}

/**
 * Copy jValue data into pasteboard. Type of data is determined by utf.
 */
static inline void SetUIPasteboardItemValueForUtf(JNIEnv *env, UIPasteboard *pasteboard, jobject jValue, NSString *utf)
{
    BOOL isString = NO;
    // Check known pasteboard-types.
    if ([utf isEqualToString:(NSString*)kUTTypeUTF8PlainText] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypePDF] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypeRTF] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypeRTFD] == YES)
    {
        isString = YES;
    }
    else if ([utf isEqualToString:(NSString*)kUTTypeHTML] == YES)
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

    if (isString == YES) // jValue data can be stored as NSString for given pasteboard type
    {
        NSString *string = nil;
        {
            const jchar *chars = (*env)->GetStringChars(env, jValue, NULL);
            string = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jValue)];
            (*env)->ReleaseStringChars(env, jValue, chars);
        }
        [pasteboard setValue:string forPasteboardType:utf];
    }
    else  // jValueData are of unknown pasteboard-type. Store them as plain NSData.
    {
        NSData *data = nil;
        {
            jbyte *bytes = (*env)->GetByteArrayElements(env, jValue, NULL);
            data = [NSData dataWithBytes:bytes length:(*env)->GetArrayLength(env, jValue)];
            (*env)->ReleaseByteArrayElements(env, jValue, bytes, 0);
        }
        [pasteboard setData:data forPasteboardType:utf];
    }
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _createSystemPasteboard
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1createSystemPasteboard
(JNIEnv *env, jobject jPasteboard, jint jType)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1createSystemPasteboard: %ld", jType);

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = nil;

        switch (jType)
        {
            case com_sun_glass_ui_ios_IosPasteboard_General:
                pasteboard = [UIPasteboard generalPasteboard];
                break;
        }

        ptr = ptr_to_jlong(pasteboard);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/* Creates UIPasteboard with name.
 *
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _createUserPasteboard
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1createUserPasteboard
(JNIEnv *env, jobject jPasteboard, jstring jName)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1createUserPasteboard");

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        const jchar *chars = (*env)->GetStringChars(env, jName, NULL);
        NSString *name = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jName)];
        (*env)->ReleaseStringChars(env, jName, chars);

        UIPasteboard *pasteboard = [[UIPasteboard pasteboardWithName:name create:YES] retain];
        ptr = ptr_to_jlong(pasteboard);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/*
 * Returns pasteboards name.
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getName
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getName");

    jstring name = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSString *string = [pasteboard name];
        name = (*env)->NewStringUTF(env, [string UTF8String]);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return name;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getUTFs
 * Signature: (J)[[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getUTFs
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getUTFs");

    jobjectArray utfs = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);

#ifdef VERBOSE
        DumpPasteboard(pasteboard);
#endif
        NSArray *items = [pasteboard items];
        if ([items count] > 0)
        {
            utfs = (*env)->NewObjectArray(env, (jsize)[items count], (*env)->FindClass(env, "[Ljava/lang/String;"), NULL);
            for (int i=0; i<[items count]; i++)
            {
                NSDictionary *item = [items objectAtIndex:i];

                NSArray *keys = [item allKeys];
                if ([keys count] > 0)
                {
                    jobjectArray array = (*env)->NewObjectArray(env, (jsize)[keys count], (*env)->FindClass(env, "java/lang/String"), NULL);
                    for (int j=0; j<[keys count]; j++)
                    {
                        NSString *type = [keys objectAtIndex:j];
                        (*env)->SetObjectArrayElement(env, array, j, (*env)->NewStringUTF(env, [type UTF8String]));
                    }
                    (*env)->SetObjectArrayElement(env, utfs, (jsize)i, array);
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return utfs;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getItemAsRawImage
 * Signature: (JI)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getItemAsRawImage
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getItemAsRawImage");

    jbyteArray bytes = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSArray *items = [pasteboard items];
        NSDictionary *item = [items objectAtIndex:jIndex];

        NSData *data = [item objectForKey:(NSString*)kUTTypeImage];

        UIImage *image = [[[UIImage alloc] initWithData:data] autorelease];

        if (image != nil)
        {
            CGImageRef cgImage = [image CGImage];

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
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return bytes;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getItemAsString
 * Signature: (JI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getItemAsString
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getItemAsString");

    jstring string = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSArray *items = [pasteboard items];
        NSDictionary *item = [items objectAtIndex:jIndex];

        NSString *str = [item objectForKey:(NSString*)kUTTypeText];
        if (str != nil)
        {
            string = (jobject)(*env)->NewStringUTF(env, [str UTF8String]);

        }

        if (string == nil)
        {
            // if no string yet, try by referencing the item's url (if it exists)
            NSString *file = [item objectForKey:(NSString*)kUTTypeURL];
            if (file != nil)
            {
                NSURL *url = [NSURL URLWithString:file];
                str = [NSString stringWithContentsOfURL:url encoding:NSUnicodeStringEncoding error:nil];
                if (str != nil)
                {
                    string = (jobject)(*env)->NewStringUTF(env, [str UTF8String]);
                }
            }
        }

        if (string == nil)
        {
            // if no string yet, try by referencing the item's url (if it exists)
            NSString *file = [item objectForKey:(NSString*)kUTTypeFileURL];
            if (file != nil)
            {
                NSURL *url = [NSURL URLWithString:file];
                str = [NSString stringWithContentsOfURL:url encoding:NSUnicodeStringEncoding error:nil];
                if (str != nil)
                {
                    string = (jobject)(*env)->NewStringUTF(env, [str UTF8String]);
                }
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return string;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getItemStringForUTF
 * Signature: (JILjava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getItemStringForUTF
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex, jstring jUtf)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getItemStringForUTF");

    jstring string = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
        NSString *utf = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jUtf)];
        (*env)->ReleaseStringChars(env, jUtf, chars);

        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSArray *items = [pasteboard items];
        NSDictionary *item = [items objectAtIndex:jIndex];

        NSString *str = [item objectForKey:utf];
        if (str != nil)
        {
            string = (jobject)(*env)->NewStringUTF(env, [str UTF8String]);
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return string;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getItemBytesForUTF
 * Signature: (JILjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getItemBytesForUTF
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex, jstring jUtf)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getItemBytesForUTF");

    jbyteArray bytes = NULL;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
        NSString *utf = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jUtf)];
        (*env)->ReleaseStringChars(env, jUtf, chars);

        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSArray *items = [pasteboard items];
        NSDictionary *item = [items objectAtIndex:jIndex];

        NSData *data = [item objectForKey:utf];
        bytes = ByteArrayFromNSData(env, data);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return bytes;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getItemForUTF
 * Signature: (JILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getItemForUTF
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jint jIndex, jstring jUtf)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getItemForUTF");

    jlong ptr = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
        NSString *utf = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jUtf)];
        (*env)->ReleaseStringChars(env, jUtf, chars);

        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        NSArray *items = [pasteboard items];
        NSDictionary *item = [items objectAtIndex:jIndex];

        id property = [[item objectForKey:utf] retain]; // notice we retain
        ptr = ptr_to_jlong(property);
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return ptr;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _putItemsFromArray
 * Signature: (J[Ljava/lang/Object;I)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1putItemsFromArray
(JNIEnv *env, jobject jPasteboard, jlong jPtr, jobjectArray jObjects, jint supportedActions)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1putItemsFromArray");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        pasteboard.items = nil;
        seed = [pasteboard changeCount];

        jsize itemCount = (*env)->GetArrayLength(env, jObjects);

        pasteboard.items = [NSMutableArray arrayWithCapacity:(itemCount > 0 ? itemCount : 1)];

        LOG(@"Java_com_sun_glass_ui_mac_IosPasteboard__1putItems itemCount: %ld", itemCount);
        if (itemCount > 0)
        {
            for (int i=0; i<itemCount; i++)
            {
                jobject array = (*env)->GetObjectArrayElement(env, jObjects, i);
                if (array != NULL)
                {
                    jsize repsCount = (*env)->GetArrayLength(env, array);
                    LOG(@"NSPasteboardItemFromArray repsCount: %d", repsCount);
                    if (repsCount > 0)
                    {
                        for (int i=0; i<repsCount; i++)
                        {
                            jobjectArray jRepresentation = (*env)->GetObjectArrayElement(env, array, i);
                            if ((*env)->GetArrayLength(env, jRepresentation) == 2)
                            {
                                jstring jUtf = (*env)->GetObjectArrayElement(env, jRepresentation, com_sun_glass_ui_ios_IosPasteboard_UtfIndex);
                                jobject jObject = (*env)->GetObjectArrayElement(env, jRepresentation, com_sun_glass_ui_ios_IosPasteboard_ObjectIndex);

                                const jchar *chars = (*env)->GetStringChars(env, jUtf, NULL);
                                NSString *utf = [NSString stringWithCharacters:(UniChar *)chars length:(*env)->GetStringLength(env, jUtf)];
                                (*env)->ReleaseStringChars(env, jUtf, chars);

                                SetUIPasteboardItemValueForUtf(env, pasteboard, jObject, utf);
                            }
                            else
                            {
                                NSLog(@"Glass error: NSPasteboardItemFromArray found bad item with %d entries", (int)(*env)->GetArrayLength(env, jRepresentation));
                            }
                        }
                    }
                }
            }

            seed = [pasteboard changeCount];

            if (pasteboard == [UIPasteboard pasteboardWithName:@"DND" create:NO])
            {
                [GlassDragDelegate flushWithMask:supportedActions];
            }
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _clear
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1clear
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1clear");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        pasteboard.items = nil;
        seed = [pasteboard changeCount];
        pasteboard.items = [NSMutableArray arrayWithCapacity:1];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;

}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getSeed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getSeed
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getSeed");

    jlong seed = 0L;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        seed = [pasteboard changeCount];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return seed;
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _release
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1release
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1release");

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        [pasteboard release];
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);
}

/*
 * Class:     com_sun_glass_ui_ios_IosPasteboard
 * Method:    _getAllowedOperation
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_ios_IosPasteboard__1getAllowedOperation
(JNIEnv *env, jobject jPasteboard, jlong jPtr)
{
    LOG(@"Java_com_sun_glass_ui_ios_IosPasteboard__1getAllowedOperation");

    jint mask = 0;

    GLASS_ASSERT_MAIN_JAVA_THREAD(env);
    GLASS_POOL_ENTER;
    {
        UIPasteboard *pasteboard = (UIPasteboard*)jlong_to_ptr(jPtr);
        if (pasteboard == [UIPasteboard pasteboardWithName:@"DnD" create:NO])
        {
            // retrieve the mask for DnD
            mask = [GlassDragDelegate getMask];
        }
        else
        {
            // we can always copy from a UIPasteboard
            mask = com_sun_glass_ui_Clipboard_ACTION_COPY;
        }
    }
    GLASS_POOL_EXIT;
    GLASS_CHECK_EXCEPTION(env);

    return mask;
}
