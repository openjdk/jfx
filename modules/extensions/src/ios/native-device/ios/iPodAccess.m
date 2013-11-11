/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

        // probably replace this table with a trivial switch/case/case... statement
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

// instance methods

- (id) init {
    self = [super init];
    return self;
}

- (void) createQuery {
    MPMediaQuery* newQuery = [[MPMediaQuery alloc] init];
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

// note: method IDs should be created outside of this method so that it's not repeated over and over
- (jobject) createDate: (NSDate *) date
                JNIEnv: (JNIEnv *) env {

    jclass calendarClass = (*env)->FindClass(env, "java/util/GregorianCalendar");
    jmethodID constructorID = (*env)->GetMethodID(env, calendarClass, "<init>", "(III)V");

    NSDateComponents *components = [[NSCalendar currentCalendar]
                                    components: NSDayCalendarUnit |
                                                NSMonthCalendarUnit |
                                                NSYearCalendarUnit
                                    fromDate: date];

    jint day = (jint) [components day];
    // Java Calendar counts months from zero!
    jint month = (jint) ([components month] - 1);
    jint year = (jint) [components year];

    jobject jDate = (*env)->NewObject(env, calendarClass, constructorID, year, month, day);

    (*env)->DeleteLocalRef(env, calendarClass);

    return jDate;
}

// note: method IDs should be created outside of this method so that it's not repeated over and over
- (jobject) createDuration: (NSNumber *) seconds
                    JNIEnv: (JNIEnv *) env {
    jclass durationClass = (*env)->FindClass(env, "javafx/util/Duration");
    jmethodID constructorID = (*env)->GetMethodID(env, durationClass, "<init>", "(D)V");

    double millis = 1000.0 * [seconds doubleValue];
    jobject jDuration = (*env)->NewObject(env, durationClass, constructorID, (jdouble) millis);

    (*env)->DeleteLocalRef(env, durationClass);

    return jDuration;
}

// note: get all the method IDs outside of this method, so that it's not repeated for every media item
- (jobject) createMediaItem: (MPMediaItem *) item
                     JNIEnv: (JNIEnv *) env {
    jclass classMediaItem = (*env)->FindClass(env, CLASS_MEDIA_ITEM);
    jobject refMediaItem = (*env)->NewGlobalRef(env, classMediaItem);
    jmethodID midConstructor = (*env)->GetMethodID(env, refMediaItem, "<init>", "()V");
    jobject objMediaItem = (*env)->NewObject(env, refMediaItem, midConstructor);

    // set playback duration (Duration)
    jmethodID midSetPlaybackDuration = (*env)->GetMethodID(env, refMediaItem, "setPlaybackDuration", "(Ljavafx/util/Duration;)V");
    NSNumber *nsDuration = (NSNumber *) [item valueForProperty: MPMediaItemPropertyPlaybackDuration];
    jobject jDuration = [self createDuration: nsDuration JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetPlaybackDuration, jDuration);

    // set release date (Date)
    jmethodID midSetReleaseDate = (*env)->GetMethodID(env, refMediaItem, "setReleaseDate", "(Ljava/util/Calendar;)V");
    NSDate *nsDate = (NSDate *) [item valueForProperty: MPMediaItemPropertyReleaseDate];
    if (nsDate != nil) {
        jobject jDate = [self createDate: nsDate JNIEnv: env];
        (*env)->CallVoidMethod(env, objMediaItem, midSetReleaseDate, jDate);
    }
    // set album track number (int)
    jmethodID midSetAlbumTrackNumber = (*env)->GetMethodID(env, refMediaItem, "setAlbumTrackNumber", "(I)V");
    NSNumber *nsAlbumTrackNumber = (NSNumber *) [item valueForProperty: MPMediaItemPropertyAlbumTrackNumber];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTrackNumber, (jint) [nsAlbumTrackNumber intValue]);
    // set album track count (int)
    jmethodID midSetAlbumTrackCount = (*env)->GetMethodID(env, refMediaItem, "setAlbumTrackCount", "(I)V");
    NSNumber *nsAlbumTrackCount = (NSNumber *) [item valueForProperty: MPMediaItemPropertyAlbumTrackCount];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTrackCount, (jint) [nsAlbumTrackCount intValue]);
    // set disc number (int)
    jmethodID midSetDiscNumber = (*env)->GetMethodID(env, refMediaItem, "setDiscNumber", "(I)V");
    NSNumber *nsDiscNumber = (NSNumber *) [item valueForProperty: MPMediaItemPropertyDiscNumber];
    (*env)->CallVoidMethod(env, objMediaItem, midSetDiscNumber, (jint) [nsDiscNumber intValue]);
    // set disc count (int)
    jmethodID midSetDiscCount = (*env)->GetMethodID(env, refMediaItem, "setDiscCount", "(I)V");
    NSNumber *nsDiscCount = (NSNumber *) [item valueForProperty: MPMediaItemPropertyDiscCount];
    (*env)->CallVoidMethod(env, objMediaItem, midSetDiscCount, (jint) [nsDiscCount intValue]);
    // set BPM (int)
    jmethodID midSetBPM = (*env)->GetMethodID(env, refMediaItem, "setBeatsPerMinute", "(I)V");
    NSNumber *nsBPM = (NSNumber *) [item valueForProperty: MPMediaItemPropertyBeatsPerMinute];
    (*env)->CallVoidMethod(env, objMediaItem, midSetBPM, (jint) [nsBPM intValue]);
    // set Is Compilation (boolean)
    jmethodID midSetIsCompilation = (*env)->GetMethodID(env, refMediaItem, "setIsCompilation", "(Z)V");
    NSNumber *nsIsCompilation = (NSNumber *) [item valueForProperty: MPMediaItemPropertyIsCompilation];
    (*env)->CallVoidMethod(env, objMediaItem, midSetIsCompilation, (jboolean) [nsIsCompilation boolValue]);
    // set title (String)
    jmethodID midSetTitle = (*env)->GetMethodID(env, refMediaItem, "setTitle", "(Ljava/lang/String;)V");
    jstring jTitle = [self javaStringForProperty: MPMediaItemPropertyTitle MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetTitle, jTitle);
    // set album title (String)
    jmethodID midSetAlbumTitle = (*env)->GetMethodID(env, refMediaItem, "setAlbumTitle", "(Ljava/lang/String;)V");
    jstring jAlbumTitle = [self javaStringForProperty: MPMediaItemPropertyAlbumTitle MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumTitle, jAlbumTitle);
    // set artist (String)
    jmethodID midSetArtist = (*env)->GetMethodID(env, refMediaItem, "setArtist", "(Ljava/lang/String;)V");
    jstring jArtist = [self javaStringForProperty: MPMediaItemPropertyArtist MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetArtist, jArtist);
    // set album artist (String)
    jmethodID midSetAlbumArtist = (*env)->GetMethodID(env, refMediaItem, "setAlbumArtist", "(Ljava/lang/String;)V");
    jstring jAlbumArtist = [self javaStringForProperty: MPMediaItemPropertyAlbumArtist MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetAlbumArtist, jAlbumArtist);
    // set genre (String)
    jmethodID midSetGenre = (*env)->GetMethodID(env, refMediaItem, "setGenre", "(Ljava/lang/String;)V");
    jstring jGenre = [self javaStringForProperty: MPMediaItemPropertyGenre MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetGenre, jGenre);
    // set composer (String)
    jmethodID midSetComposer = (*env)->GetMethodID(env, refMediaItem, "setComposer", "(Ljava/lang/String;)V");
    jstring jComposer = [self javaStringForProperty: MPMediaItemPropertyComposer MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetComposer, jComposer);
    // set lyrics (String)
    jmethodID midSetLyrics = (*env)->GetMethodID(env, refMediaItem, "setLyrics", "(Ljava/lang/String;)V");
    jstring jLyrics = [self javaStringForProperty: MPMediaItemPropertyLyrics MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetLyrics, jLyrics);
    // set comments (String)
    jmethodID midSetComments = (*env)->GetMethodID(env, refMediaItem, "setComments", "(Ljava/lang/String;)V");
    jstring jComments = [self javaStringForProperty: MPMediaItemPropertyComments MediaItem: item JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetComments, jComments);
    // set URL (String)
    jmethodID midSetURL = (*env)->GetMethodID(env, refMediaItem, "setURL", "(Ljava/lang/String;)V");
    NSURL *nsURL = (NSURL *) [item valueForProperty: MPMediaItemPropertyAssetURL];
    jstring jURL = [self createJavaString: [nsURL absoluteString] JNIEnv: env];
    (*env)->CallVoidMethod(env, objMediaItem, midSetURL, jURL);
    // set media type (int)
    jmethodID midSetMediaType = (*env)->GetMethodID(env, refMediaItem, "setMediaType", "(I)V");
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

        // note: this can be done in the constructor, let's not waste time here
        jobject jMediaQuery = (*env)->NewGlobalRef(env, obj);
        jclass klass = (*env)->GetObjectClass(env, obj);

        jmethodID midAddMediaItem = (*env)->GetMethodID(env,
                                                        klass,
                                                        "addMediaItem",
                                                        "(Lcom/sun/javafx/ext/device/ios/ipod/MediaItem;)V");

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
    jclass listClass = (*env)->FindClass(env, "java/util/LinkedList");
        jmethodID constructorID = (*env)->GetMethodID(env, listClass, "<init>", "()V");

        jobject jList = (*env)->NewObject(env, listClass, constructorID);

        (*env)->DeleteLocalRef(env, listClass);

    return jList;
}

- (void) addItemToList: (jobject) item
                  list: (jobject) list
                JNIEnv: (JNIEnv *) env {

    jclass klass = (*env)->GetObjectClass(env, list);

    // note: initialize all method IDs at startup
    jmethodID midAddToList = (*env)->GetMethodID(env,
                                                 klass,
                                                 "add",
                                                 "(Ljava/lang/Object;)Z");

    (*env)->CallVoidMethod(env,
                           list,
                           midAddToList,
                           item);

}

- (void) fillCollectionsOfMediaQuery: (jobject) obj
                              jniEnv: (JNIEnv *) env {
    NSArray *collections = [[self query] collections];

    if ([collections count] != 0) {

        // note: this can be done in the constructor, ..
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
