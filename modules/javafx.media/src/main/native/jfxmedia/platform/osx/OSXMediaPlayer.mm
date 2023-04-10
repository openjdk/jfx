/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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

#import "OSXMediaPlayer.h"
#import "OSXPlayerProtocol.h"
#import "com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer.h"
#import <Utils/JObjectPeers.h>
#import <Utils/JavaUtils.h>
#import <CoreAudio/CoreAudio.h>
#import <jni/Logger.h>
#import <jni/JniUtils.h>
#import <jni/JavaInputStreamCallbacks.h>
#import <Locator/Locator.h>
#import <Locator/LocatorStream.h>
#import <jfxmedia_errors.h>

#import <objc/runtime.h>

#define USE_WEAK_REFS 0

// Don't access directly, use the OSXMediaPlayer static methods to ensure thread safe access
static JObjectPeers *gMediaPlayerPeers = nil;
static Class gMediaPlayerClass = nil;

@implementation OSXMediaPlayer

+ (void) initialize
{
    gMediaPlayerPeers = [[JObjectPeers alloc] init];
}

+ (OSXMediaPlayer*) peerForPlayer:(jobject)javaPlayer andEnv:(JNIEnv*)javaEnv
{
    return [gMediaPlayerPeers peerForJObject:javaPlayer javaEnv:javaEnv];
}

+ (void) setPeer:(OSXMediaPlayer*)player forJavaPlayer:(jobject)javaPlayer andEnv:(JNIEnv*)javaEnv
{
    [gMediaPlayerPeers setPeer:player forJObject:javaPlayer javaEnv:javaEnv];
}

+ (void) removePlayerPeers:(OSXMediaPlayer*)player
{
    [gMediaPlayerPeers removePeer:player];
}

+ (BOOL) initPlayerPlatform
{
    BOOL enableAVF = YES;

    // Check environment to see if platforms are enabled
    char *value = getenv("JFXMEDIA_AVF");
    if (value ? strncasecmp(value, "yes", 3) != 0 : NO) {
        enableAVF = NO;
    }

    // Determine if we can use OSX native player libs, without linking directly
    Class klass;

    if (enableAVF) {
        klass = objc_getClass("AVFMediaPlayer");
        if (klass) {
            if ([klass conformsToProtocol:@protocol(OSXPlayerProtocol)]) {
                if ([klass respondsToSelector:@selector(playerAvailable)] ? [klass playerAvailable] : YES) {
                    gMediaPlayerClass = klass;
                    return YES;
                }
            }
        }
    }

    return NO;
}

- (id) init
{
    if ((self = [super init]) != nil) {
    }
    return self;
}

- (id) initWithURL:(NSURL *)source javaPlayer:(jobject)jp andEnv:(JNIEnv*)env eventHandler:(CJavaPlayerEventDispatcher*)hdlr locatorStream:(CLocatorStream*)ls
{
    if (!gMediaPlayerClass) {
        // No player class available, abort
        return nil;
    }

    if ((self = [super init]) != nil) {
        movieURL = [source retain];

        env->GetJavaVM(&javaPlayerVM);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            return nil;
        }
#if USE_WEAK_REFS
        javaPlayer = env->NewWeakGlobalRef(jp);
#else
        javaPlayer = env->NewGlobalRef(jp);
#endif
        // set up the peer association
        [OSXMediaPlayer setPeer:self forJavaPlayer:javaPlayer andEnv:env];

        eventHandler = hdlr;

        // create the player object
        player = [[gMediaPlayerClass alloc] initWithURL:movieURL eventHandler:eventHandler locatorStream:ls];
    }
    return self;
}

- (id) initWithURL:(NSURL *)source eventHandler:(CJavaPlayerEventDispatcher*)hdlr locatorStream:(CLocatorStream*)ls
{
    // stub initWithURL message to satisfy the protocol requirements, this should
    // never be called
    return nil;
}

- (void) dealloc
{
    [self dispose]; // just in case
    [movieURL release];
    [super dealloc];
}

- (void) dispose
{
    @synchronized(self) {
        [player dispose];
        [player release];
        player = nil;

        if (eventHandler) {
            eventHandler->Dispose();
            delete eventHandler;
        }
        eventHandler = NULL;

        [OSXMediaPlayer removePlayerPeers:self];
        if (javaPlayerVM && javaPlayer) {
            BOOL attached = NO;
            JNIEnv *env = GetJavaEnvironment(javaPlayerVM, &attached);

            // remove peer association
            [OSXMediaPlayer removePlayerPeers:self];
#if USE_WEAK_REFS
            env->DeleteWeakGlobalRef(javaPlayer);
#else
            env->DeleteGlobalRef(javaPlayer);
#endif
            if (attached) {
                javaPlayerVM->DetachCurrentThread();
            }
        }
        javaPlayer = 0;
        javaPlayerVM = NULL;
    }
}

- (id<OSXPlayerProtocol>) player
{
    return [[player retain] autorelease];
}

- (CAudioEqualizer*) audioEqualizer
{
    return player.audioEqualizer;
}

- (CAudioSpectrum*) audioSpectrum
{
    return player.audioSpectrum;
}

- (int64_t) audioSyncDelay
{
    return player.audioSyncDelay;
}

- (void) setAudioSyncDelay:(int64_t)audioSyncDelay
{
    player.audioSyncDelay = audioSyncDelay;
}

- (double) duration
{
    return player.duration;
}

- (float) rate
{
    return player.rate;
}

- (void) setRate:(float)rate
{
    player.rate = rate;
}

- (double) currentTime
{
    return player.currentTime;
}

- (void) setCurrentTime:(double)currentTime
{
    player.currentTime = currentTime;
}

- (BOOL) mute
{
    return player.mute;
}

- (void) setMute:(BOOL)mute
{
    player.mute = mute;
}

- (float) volume
{
    return player.volume;
}

- (void) setVolume:(float)volume
{
    player.volume = volume;
}

- (float) balance
{
    return player.balance;
}

- (void) setBalance:(float)balance
{
    player.balance = balance;
}

- (void) play
{
    [player play];
}

- (void) pause
{
    [player pause];
}

- (void) finish
{
    [player finish];
}

- (void) stop
{
    [player stop];
}

@end

#pragma mark -
#pragma mark JNI Methods

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxCreatePlayer
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxCreatePlayer
    (JNIEnv *env, jobject playerObject, jobject jLocator, jstring jContentType,
    jlong jSizeHint)
{
    CLocatorStream *locatorStream = NULL;
    jstring jSourceURI = CLocator::LocatorGetStringLocation(env, jLocator);
    char *pjSourceURI = NULL;
    char *pjContent = NULL;

    // create the event dispatcher, init later
    CJavaPlayerEventDispatcher *eventHandler = new CJavaPlayerEventDispatcher();
    eventHandler->Init(env, playerObject, NULL);

    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    NSString *sourceURIString = NSStringFromJavaString(env, jSourceURI);
    if (!sourceURIString) {
        LOGGER_ERRORMSG("OSXMediaPlayer: Unable to create sourceURIString\n");
        ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                           "OSXMediaPlayer: Unable to create sourceURIString");
        return;
    }

    NSURL *mediaURL = [[NSURL alloc] initWithString:sourceURIString];
    if (!mediaURL) {
        LOGGER_WARNMSG("OSXMediaPlayer: Unable to create mediaURL\n");
        ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                           "OSXMediaPlayer: Unable to create mediaURL");
        return;
    }

    // Check if we need to use Locator to read data. For FILE/HTTP/HTTPS
    // AVFoundation will read data directly. For JAR/JRT we will use Locator to
    // read data.
    NSString *scheme = [mediaURL scheme];
    if ([scheme caseInsensitiveCompare:@"jar"] == NSOrderedSame ||
        [scheme caseInsensitiveCompare:@"jrt"] == NSOrderedSame) {
        CJavaInputStreamCallbacks *callbacks = new (nothrow) CJavaInputStreamCallbacks();
        if (callbacks == NULL) {
            [mediaURL release];
            LOGGER_WARNMSG("OSXMediaPlayer: Unable to create CJavaInputStreamCallbacks\n");
            ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                               "OSXMediaPlayer: Unable to create CJavaInputStreamCallbacks");
            return;
        }

        if (!callbacks->Init(env, jLocator)) {
            [mediaURL release];
            delete callbacks;
            LOGGER_WARNMSG("OSXMediaPlayer: callbacks->Init() failed\n");
            ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                               "OSXMediaPlayer: callbacks->Init() failed");
            return;
        }

        pjContent = (char*)env->GetStringUTFChars(jContentType, NULL);
        pjSourceURI = (char*)env->GetStringUTFChars(jSourceURI, NULL);
        if (pjContent == NULL || pjSourceURI == NULL) {
            [mediaURL release];
            delete callbacks;
            if (pjContent != NULL) {
                env->ReleaseStringUTFChars(jContentType, pjContent);
            }
            if (pjSourceURI != NULL) {
                env->ReleaseStringUTFChars(jSourceURI, pjSourceURI);
            }
            LOGGER_WARNMSG("OSXMediaPlayer: memory allocation failed\n");
            ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                                    "OSXMediaPlayer: memory allocation failed");
            return;
        }

        locatorStream = new(nothrow) CLocatorStream(callbacks, pjContent, pjSourceURI, jSizeHint);
        env->ReleaseStringUTFChars(jContentType, pjContent);
        env->ReleaseStringUTFChars(jSourceURI, pjSourceURI);
    }

    OSXMediaPlayer *player = [[OSXMediaPlayer alloc] initWithURL:mediaURL javaPlayer:playerObject andEnv:env eventHandler:eventHandler locatorStream:locatorStream];
    if (!player) {
        LOGGER_WARNMSG("OSXMediaPlayer: Unable to create player\n");
        ThrowJavaException(env, "com/sun/media/jfxmedia/MediaException",
                           "OSXMediaPlayer: Unable to create player");
        [mediaURL release];
        return;
    }

    [player release]; // The player peer list retains for us
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetAudioEqualizerRef
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetAudioEqualizerRef
(JNIEnv *env, jobject playerObject) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    CAudioEqualizer *eq = 0;
    if (player) {
        eq = player.audioEqualizer;
    }
    [pool drain];
    return ptr_to_jlong(eq);
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetAudioSpectrumRef
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetAudioSpectrumRef
(JNIEnv *env, jobject playerObject) {
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    CAudioSpectrum *spectrum = NULL;
    if (player) {
        spectrum = player.audioSpectrum;
    }
    [pool drain];
    return ptr_to_jlong(spectrum);
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetAudioSyncDelay
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetAudioSyncDelay
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jlong asd = 0;
    if (player) {
        asd = (jlong)player.audioSyncDelay;
    }
    [pool drain];
    return asd;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSetAudioSyncDelay
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSetAudioSyncDelay
    (JNIEnv *env, jobject playerObject, jlong delay)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.audioSyncDelay = (int64_t)delay;
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxPlay
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxPlay
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        [player play];
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxStop
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxStop
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        [player stop];
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxPause
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxPause
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        [player pause];
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxFinish
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxFinish
(JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        [player finish];
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetRate
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetRate
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jfloat rc = 0.0;
    if (player) {
        rc = (jfloat)player.rate;
    }
    [pool drain];
    return rc;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSetRate
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSetRate
    (JNIEnv *env, jobject playerObject, jfloat rate)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.rate = (double)rate;
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetPresentationTime
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetPresentationTime
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jdouble rc = 0.0;
    if (player) {
        rc = player.currentTime;
    }
    [pool drain];
    return rc;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetMute
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetMute
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jboolean rc = JNI_FALSE;
    if (player) {
        rc = (player.mute != NO);
    }
    [pool drain];
    return rc;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSetMute
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSetMute
    (JNIEnv *env, jobject playerObject, jboolean mute)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.mute = (mute != JNI_FALSE);
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetVolume
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetVolume
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jfloat rc = 0.0;
    if (player) {
        rc = (jfloat)player.volume;
    }
    [pool drain];
    return rc;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSetVolume
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSetVolume
    (JNIEnv *env, jobject playerObject, jfloat volume)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.volume = (double)volume;
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetBalance
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetBalance
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    jfloat rc = 0.0;
    if (player) {
        rc = (jfloat)player.balance;
    }
    [pool drain];
    return rc;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSetBalance
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSetBalance
    (JNIEnv *env, jobject playerObject, jfloat balance)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.balance = (double)balance;
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxGetDuration
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxGetDuration
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    double duration = -1.0;
    if (player) {
        duration = (jdouble)player.duration;
    }
    [pool drain];
    return duration;
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxSeek
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxSeek
    (JNIEnv *env, jobject playerObject, jdouble time)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        player.currentTime = (double)time;
    }
    [pool drain];
}

/*
 * Class:     com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer
 * Method:    osxDispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_media_jfxmediaimpl_platform_osx_OSXMediaPlayer_osxDispose
    (JNIEnv *env, jobject playerObject)
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
    OSXMediaPlayer *player = [OSXMediaPlayer peerForPlayer:playerObject andEnv:env];
    if (player) {
        [player dispose];

        // This should pop the last retain, aside from the autoreleased reference...
        [OSXMediaPlayer removePlayerPeers:player];
    }
    [pool drain];
}
