/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "iPodAccess.h"

@implementation IPodAccess


@synthesize query;


static NSDictionary *dictPredicateKeys;
static NSDictionary *dictGroupingKeys;

// initialize static immutable look-up tables that associates Java predicate keys to
// MPMediaProperty strings and Java grouping types to MPMediaGrouping

// dictionary values must correspond to their Java counterparts!

+ (void) initialize {

    // we need to initialize the dictionary exactly once
    if (self == [IPodAccess class]) {

        dictPredicateKeys = [[NSDictionary alloc] initWithObjectsAndKeys:

                             MPMediaItemPropertyMediaType, [NSNumber numberWithInt: 0],
                             MPMediaItemPropertyTitle, [NSNumber numberWithInt: 1],
                             MPMediaItemPropertyAlbumTitle, [NSNumber numberWithInt: 2],
                             MPMediaItemPropertyArtist, [NSNumber numberWithInt: 3],
                             MPMediaItemPropertyAlbumArtist, [NSNumber numberWithInt: 4],
                             MPMediaItemPropertyGenre, [NSNumber numberWithInt: 5],
                             MPMediaItemPropertyComposer, [NSNumber numberWithInt: 6],
                             MPMediaItemPropertyIsCompilation, [NSNumber numberWithInt: 7],

                             nil];

        dictGroupingKeys = [[NSDictionary alloc] initWithObjectsAndKeys:

                            [NSNumber numberWithInt: MPMediaGroupingTitle], [NSNumber numberWithInt: 0],
                            [NSNumber numberWithInt: MPMediaGroupingAlbum], [NSNumber numberWithInt: 1],
                            [NSNumber numberWithInt: MPMediaGroupingArtist], [NSNumber numberWithInt: 2],
                            [NSNumber numberWithInt: MPMediaGroupingAlbumArtist], [NSNumber numberWithInt: 3],
                            [NSNumber numberWithInt: MPMediaGroupingComposer], [NSNumber numberWithInt: 4],
                            [NSNumber numberWithInt: MPMediaGroupingGenre], [NSNumber numberWithInt: 5],
                            [NSNumber numberWithInt: MPMediaGroupingPlaylist], [NSNumber numberWithInt: 6],
                            [NSNumber numberWithInt: MPMediaGroupingPodcastTitle], [NSNumber numberWithInt: 7],

                            nil];
    }
}

// here come the instance methods

- (id) init {

    self = [super init];
    return self;
}

- (void) createQuery {

    MPMediaQuery* newQuery = [[MPMediaQuery alloc] init];

    if (newQuery == nil) {
        // TODO: report error (throw a Java exception ?)
        // http://javafx-jira.kenai.com/browse/RT-27005
    }

    [self setQuery: newQuery];
}

- (NSString *) predicateKeyToMediaItemProperty: (int) predicateKey {

    NSNumber *key = [NSNumber numberWithInt: predicateKey];
    return (NSString *) [dictPredicateKeys objectForKey: key];
}

- (MPMediaGrouping) groupingKeyToMediaGrouping: (int) groupingKey {

    NSNumber *key = [NSNumber numberWithInt: groupingKey];
    NSNumber *value = (NSNumber *) [dictGroupingKeys objectForKey: key];

    return [value integerValue];
}

- (void) addNumberPredicateForKey: (int) predicateKey
                            value: (int) predicateValue {

    NSString *propertyName = [self predicateKeyToMediaItemProperty: predicateKey];
    NSNumber *propertyValue = [NSNumber numberWithInt: predicateValue];

    MPMediaPropertyPredicate *predicate =
    [MPMediaPropertyPredicate predicateWithValue: propertyValue
                                     forProperty: propertyName];

    [[self query] addFilterPredicate: predicate];
}

- (void) addStringPredicateForKey: (int) predicateKey
                            value: (NSString *) predicateValue {

    NSString *propertyName = [self predicateKeyToMediaItemProperty: predicateKey];

    MPMediaPropertyPredicate *predicate =
    [MPMediaPropertyPredicate predicateWithValue: predicateValue
                                     forProperty: propertyName];

    [[self query] addFilterPredicate: predicate];
}

- (jstring) createJavaString: (NSString *) nsString
                      JNIEnv: (JNIEnv *) env {

    const char *cString = [nsString UTF8String];
    return (jstring) (*env)->NewStringUTF(env, cString);
}

- (jstring) javaStringForProperty: (NSString *) propertyName
                        MediaItem: (MPMediaItem *) item
                           JNIEnv: (JNIEnv *) env {

    NSString* nsString = (NSString *) [item valueForProperty: propertyName];
    return [self createJavaString: nsString
                           JNIEnv: env];
}

- (jobject) createDate: (NSDate *) date
                JNIEnv: (JNIEnv *) env {

    static jmethodID constructorID = NULL;

    jclass calendarClass = (*env)->FindClass(env, "java/util/GregorianCalendar");
    if (!constructorID)
        constructorID = (*env)->GetMethodID(env, calendarClass, "<init>", "(III)V");

    NSDateComponents *components = [[NSCalendar currentCalendar]
                                    components: NSDayCalendarUnit | NSMonthCalendarUnit | NSYearCalendarUnit
                                    fromDate: date];

    jint day = (jint) [components day];
    // Java Calendar counts months from zero!
    jint month = (jint) ([components month] - 1);
    jint year = (jint) [components year];

    jobject jDate = (*env)->NewObject(env, calendarClass, constructorID, year, month, day);
    (*env)->DeleteLocalRef(env, calendarClass);
    return jDate;
}

- (jobject) createDuration: (NSNumber *) seconds
                    JNIEnv: (JNIEnv *) env {

    jmethodID constructorID = NULL;

    jclass durationClass = (*env)->FindClass(env, "javafx/util/Duration");

    if (!constructorID)
        constructorID = (*env)->GetMethodID(env, durationClass, "<init>", "(D)V");

    double millis = 1000.0 * [seconds doubleValue];
    jobject jDuration = (*env)->NewObject(env, durationClass, constructorID, (jdouble) millis);

    (*env)->DeleteLocalRef(env, durationClass);

    return jDuration;
}

- (jobject) createMediaItem: (MPMediaItem *) item
                     JNIEnv: (JNIEnv *) env {

    static jmethodID midConstructor = NULL;
    static jmethodID midSetPlaybackDuration = NULL;
    static jmethodID midSetReleaseDate = NULL;
    static jmethodID midSetAlbumTrackNumber = NULL;
    static jmethodID midSetAlbumTrackCount = NULL;
    static jmethodID midSetDiscNumber = NULL;
    static jmethodID midSetDiscCount = NULL;
    static jmethodID midSetBPM = NULL;
    static jmethodID midSetIsCompilation = NULL;
    static jmethodID midSetTitle = NULL;
    static jmethodID midSetAlbumTitle = NULL;
    static jmethodID midSetArtist = NULL;
    static jmethodID midSetAlbumArtist = NULL;
    static jmethodID midSetGenre = NULL;
    static jmethodID midSetComposer = NULL;
    static jmethodID midSetLyrics = NULL;
    static jmethodID midSetComments = NULL;
    static jmethodID midSetURL = NULL;
    static jmethodID midSetMediaType = NULL;

    jclass classMediaItem = (*env)->FindClass(env, CLASS_MEDIA_ITEM);
    jobject refMediaItem = (*env)->NewGlobalRef(env, classMediaItem);

    if (!midConstructor) // Init methods all at once
    {
        midConstructor = (*env)->GetMethodID(env, refMediaItem, "<init>", "()V");
        midSetPlaybackDuration = (*env)->GetMethodID(env, refMediaItem, "setPlaybackDuration", "(Ljavafx/util/Duration;)V");
        midSetReleaseDate = (*env)->GetMethodID(env, refMediaItem, "setReleaseDate", "(Ljava/util/Calendar;)V");
        midSetAlbumTrackNumber = (*env)->GetMethodID(env, refMediaItem, "setAlbumTrackNumber", "(I)V");
        midSetAlbumTrackCount = (*env)->GetMethodID(env, refMediaItem, "setAlbumTrackCount", "(I)V");
        midSetDiscNumber = (*env)->GetMethodID(env, refMediaItem, "setDiscNumber", "(I)V");
        midSetDiscCount = (*env)->GetMethodID(env, refMediaItem, "setDiscCount", "(I)V");
        midSetBPM = (*env)->GetMethodID(env, refMediaItem, "setBeatsPerMinute", "(I)V");
        midSetIsCompilation = (*env)->GetMethodID(env, refMediaItem, "setIsCompilation", "(Z)V");
        midSetTitle = (*env)->GetMethodID(env, refMediaItem, "setTitle", "(Ljava/lang/String;)V");
        midSetAlbumTitle = (*env)->GetMethodID(env, refMediaItem, "setAlbumTitle", "(Ljava/lang/String;)V");
        midSetArtist = (*env)->GetMethodID(env, refMediaItem, "setArtist", "(Ljava/lang/String;)V");
        midSetAlbumArtist = (*env)->GetMethodID(env, refMediaItem, "setAlbumArtist", "(Ljava/lang/String;)V");
        midSetGenre = (*env)->GetMethodID(env, refMediaItem, "setGenre", "(Ljava/lang/String;)V");
        midSetComposer = (*env)->GetMethodID(env, refMediaItem, "setComposer", "(Ljava/lang/String;)V");
        midSetLyrics = (*env)->GetMethodID(env, refMediaItem, "setLyrics", "(Ljava/lang/String;)V");
        midSetComments = (*env)->GetMethodID(env, refMediaItem, "setComments", "(Ljava/lang/String;)V");
        midSetURL = (*env)->GetMethodID(env, refMediaItem, "setURL", "(Ljava/lang/String;)V");
        midSetMediaType = (*env)->GetMethodID(env, refMediaItem, "setMediaType", "(I)V");
    }

    jobject objMediaItem = (*env)->NewObject(env, refMediaItem, midConstructor);

    // set playback duration (Duration)
    NSNumber *nsDuration = (NSNumber *) [item valueForProperty: MPMediaItemPropertyPlaybackDuration];
    jobject jDuration = [self createDuration: nsDuration JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetPlaybackDuration, jDuration);

    // set release date (Date)
    NSDate *nsDate = (NSDate *) [item valueForProperty: MPMediaItemPropertyReleaseDate];
    jobject jDate = [self createDate: nsDate JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetReleaseDate, jDate);

    // set album track number (int)
    NSNumber *nsAlbumTrackNumber = (NSNumber *) [item valueForProperty: MPMediaItemPropertyAlbumTrackNumber];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTrackNumber, (jint) [nsAlbumTrackNumber intValue]);

    // set album track count (int)
    NSNumber *nsAlbumTrackCount = (NSNumber *) [item valueForProperty: MPMediaItemPropertyAlbumTrackCount];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTrackCount, (jint) [nsAlbumTrackCount intValue]);

    // set disc number (int)
    NSNumber *nsDiscNumber = (NSNumber *) [item valueForProperty: MPMediaItemPropertyDiscNumber];
    (*env)->CallVoidMethod(env, objMediaItem, midSetDiscNumber, (jint) [nsDiscNumber intValue]);

    // set disc count (int)
    NSNumber *nsDiscCount = (NSNumber *) [item valueForProperty: MPMediaItemPropertyDiscCount];
    (*env)->CallVoidMethod(env, objMediaItem, midSetDiscCount, (jint) [nsDiscCount intValue]);

    // set BPM (int)
    NSNumber *nsBPM = (NSNumber *) [item valueForProperty: MPMediaItemPropertyBeatsPerMinute];
    (*env)->CallVoidMethod(env, objMediaItem, midSetBPM, (jint) [nsBPM intValue]);

    // set Is Compilation (boolean)
    NSNumber *nsIsCompilation = (NSNumber *) [item valueForProperty: MPMediaItemPropertyIsCompilation];
    (*env)->CallVoidMethod(env, objMediaItem, midSetIsCompilation, (jboolean) [nsIsCompilation boolValue]);

    // set title (String)
    jstring jTitle = [self javaStringForProperty: MPMediaItemPropertyTitle MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetTitle, jTitle);

    // set album title (String)
    jstring jAlbumTitle = [self javaStringForProperty: MPMediaItemPropertyAlbumTitle MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTitle, jAlbumTitle);

    // set artist (String)
    jstring jArtist = [self javaStringForProperty: MPMediaItemPropertyArtist MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetArtist, jArtist);

    // set album artist (String)
    jstring jAlbumArtist = [self javaStringForProperty: MPMediaItemPropertyAlbumArtist MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumArtist, jAlbumArtist);

    // set genre (String)
    jstring jGenre = [self javaStringForProperty: MPMediaItemPropertyGenre MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetGenre, jGenre);

    // set composer (String)
    jstring jComposer = [self javaStringForProperty: MPMediaItemPropertyComposer MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetComposer, jComposer);

    // set lyrics (String)
    jstring jLyrics = [self javaStringForProperty: MPMediaItemPropertyLyrics MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetLyrics, jLyrics);

    // set comments (String)
    jstring jComments = [self javaStringForProperty: MPMediaItemPropertyComments MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetComments, jComments);

    // set URL (String)
    NSURL *nsURL = (NSURL *) [item valueForProperty: MPMediaItemPropertyAssetURL];
    jstring jURL = [self createJavaString: [nsURL absoluteString] JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetURL, jURL);

    // set media type (int)
    NSNumber *nsMediaType = (NSNumber *) [item valueForProperty: MPMediaItemPropertyMediaType];
    (*env)->CallVoidMethod(env, objMediaItem, midSetMediaType, (jint) [nsMediaType intValue]);

    // cleanup
    (*env)->DeleteGlobalRef(env, refMediaItem);
    (*env)->DeleteLocalRef(env, classMediaItem);

    return objMediaItem;
}

- (void) fillItemListOfMediaQuery: (jobject) obj
                           jniEnv: (JNIEnv *) env {

    NSArray *items = [[self query] items];

    if ([items count] != 0) {

        // TODO: this can be done in the constructor, let's not waste time here
        // http://javafx-jira.kenai.com/browse/RT-27005
        jobject jMediaQuery = (*env)->NewGlobalRef(env, obj);
        jclass klass = (*env)->GetObjectClass(env, obj);

        jmethodID midAddMediaItem = (*env)->GetMethodID(env,
                                                        klass,
                                                        "addMediaItem",
                                                        "(Ljavafx/ext/ios/ipod/MediaItem;)V");

        NSEnumerator *enumerator = [items objectEnumerator];

        id item;
        while (item = [enumerator nextObject]) {
            jobject newMediaItem = [self createMediaItem: (MPMediaItem *) item
                                                  JNIEnv: env];
            (*env)->CallVoidMethod(env,
                                   jMediaQuery,
                                   midAddMediaItem,
                                   newMediaItem);
        }

        (*env)->DeleteLocalRef(env, klass);
        (*env)->DeleteGlobalRef(env, jMediaQuery);
    }
}

- (jobject) createJavaListWithEnv: (JNIEnv *) env {

    static jmethodID constructorID = NULL;

    jclass listClass = (*env)->FindClass(env, "java/util/LinkedList");

    if (!constructorID)
        constructorID = (*env)->GetMethodID(env, listClass, "<init>", "()V");

    jobject jList = (*env)->NewObject(env, listClass, constructorID);
    (*env)->DeleteLocalRef(env, listClass);

    return jList;
}

- (void) addItemToList: (jobject) item
                  list: (jobject) list
                JNIEnv: (JNIEnv *) env {

    static jmethodID midAddToList = NULL;

    if (!midAddToList)
    {
        jclass klass = (*env)->GetObjectClass(env, list);
        (*env)->GetMethodID(env, klass, "add", "(Ljava/lang/Object;)Z");
        (*env)->DeleteLocalRef(env, klass);
    }

    (*env)->CallVoidMethod(env, list, midAddToList, item);
}

- (void) fillCollectionsOfMediaQuery: (jobject) obj
                              jniEnv: (JNIEnv *) env {

    NSArray *collections = [[self query] collections];

    if ([collections count] != 0) {

        // TODO: this can be done in the constructor, let's not waste time here
        // http://javafx-jira.kenai.com/browse/RT-27005
        jobject jMediaQuery = (*env)->NewGlobalRef(env, obj);
        jclass klass = (*env)->GetObjectClass(env, obj);

        jmethodID midAddCollection = (*env)->GetMethodID(env,
                                                         klass,
                                                         "addCollection",
                                                         "(Ljava/util/List;)V");

        for (MPMediaItemCollection *collection in collections) {

            jobject list = [self createJavaListWithEnv: env];
            NSArray *items = [collection items];

            for (MPMediaItem *item in items) {
                jobject newMediaItem = [self createMediaItem: item
                                                      JNIEnv: env];
                [self addItemToList: newMediaItem
                               list: list
                             JNIEnv: env];
            }

            (*env)->CallVoidMethod(env,
                                   jMediaQuery,
                                   midAddCollection,
                                   list);
        }

        (*env)->DeleteLocalRef(env, klass);
        (*env)->DeleteGlobalRef(env, jMediaQuery);

    }
}

- (void) setGroupingType: (int) type {

    MPMediaGrouping groupingType = [self groupingKeyToMediaGrouping: type];
    [[self query] setGroupingType: groupingType];
}

- (void) disposeQuery {

    [[self query] release];
}


@end
