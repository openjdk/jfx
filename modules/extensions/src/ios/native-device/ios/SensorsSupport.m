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
#import <jni.h>
#import <CoreMotion/CoreMotion.h>
#import <CoreLocation/CoreLocation.h>
#import <UIKit/UIKit.h>

extern JNIEnv *jEnv;
#define GET_MAIN_JENV \
if (jEnv == NULL) NSLog(@"ERROR: Java has been detached already, but someone is still trying to use it at %s:%s:%d\n", __FUNCTION__, __FILE__, __LINE__);\
JNIEnv *env = jEnv;


jclass jIOSMotionManagerClass = NULL;
jmethodID jIOSMotionManagerDidAccelerate = NULL;
jmethodID jIOSMotionManagerDidRotate = NULL;

jclass jIOSLocationManagerClass = NULL;
jmethodID jIOSLocationManagerDidUpdateHeading = NULL;
jmethodID jIOSLocationManagerDidUpdateLocation = NULL;

CMMotionManager * motionManager = NULL;


@interface MyCLLocationManagerDelegate : NSObject<CLLocationManagerDelegate>
@end


@implementation MyCLLocationManagerDelegate

- (void)locationManager:(CLLocationManager *)manager
       didUpdateHeading:(CLHeading *)newHeading
{
    GET_MAIN_JENV;
        (*env)->CallStaticVoidMethod(
        env,
        jIOSLocationManagerClass,
        jIOSLocationManagerDidUpdateHeading,

        (jdouble)newHeading.magneticHeading ,
        (jdouble)newHeading.trueHeading,
        (jdouble)newHeading.x,
        (jdouble)newHeading.y,
        (jdouble)newHeading.z
    );
}


- (void)locationManager:(CLLocationManager *)manager
    didUpdateToLocation:(CLLocation *)newLocation
    fromLocation:(CLLocation *)oldLocation
{
    GET_MAIN_JENV;
        (*env)->CallStaticVoidMethod(
        env,
        jIOSLocationManagerClass,
        jIOSLocationManagerDidUpdateLocation,

        (jdouble)newLocation.coordinate.latitude,
        (jdouble)newLocation.coordinate.longitude,
        (jdouble)newLocation.altitude,
        (jdouble)newLocation.course,
        (jdouble)newLocation.speed
    );
}

@end



JNIEXPORT jboolean JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSMotionManager_isAccelerometerAvailable
(JNIEnv *env, jclass jClass)
{
    return motionManager.accelerometerAvailable == YES ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSMotionManager_isGyroAvailable
(JNIEnv *env, jclass jClass)
{
    return motionManager.gyroAvailable == YES ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSMotionManager
 * Method:    _initIDs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSMotionManager__1init
(JNIEnv *env, jclass jClass)
{
    if (jIOSMotionManagerClass == NULL)
    {
        jIOSMotionManagerClass = (*env)->NewGlobalRef(env, jClass);
    }

    if (jIOSMotionManagerDidAccelerate == NULL)
    {
        jIOSMotionManagerDidAccelerate =
            (*env)->GetStaticMethodID(
                env, jIOSMotionManagerClass,
                "didAccelerate", "(FFF)V");
    }


    if (jIOSMotionManagerDidRotate == NULL)
    {
        jIOSMotionManagerDidRotate =
            (*env)->GetStaticMethodID(
                env, jIOSMotionManagerClass,
                "didRotate", "(FFF)V");
    }


    motionManager = [[CMMotionManager alloc] init];
    if (motionManager.accelerometerAvailable)
    {
        if (motionManager.isAccelerometerActive == NO)
        {
            motionManager.accelerometerUpdateInterval = 0.05; // 20Hz
            [motionManager startAccelerometerUpdates];
            [motionManager startAccelerometerUpdatesToQueue:[NSOperationQueue currentQueue]
                                                withHandler:^(CMAccelerometerData *accelerometerData, NSError *error) {

                const CMAcceleration acceleration = accelerometerData.acceleration;
                GET_MAIN_JENV;
                (*env)->CallStaticVoidMethod(
                    env,
                    jIOSMotionManagerClass,
                    jIOSMotionManagerDidAccelerate,

                    (jfloat)acceleration.x,
                    (jfloat)acceleration.y,
                    (jfloat)acceleration.z
                );
            }];
        }
    }


    if (motionManager.isGyroAvailable)
    {
        /* Start the gyroscope if it is not active already */
        if (motionManager.isGyroActive == NO)
        {
            motionManager.gyroUpdateInterval = 0.1f;

            [motionManager startGyroUpdatesToQueue:[NSOperationQueue mainQueue]
                                       withHandler:^(CMGyroData *gyroData, NSError *error)
            {
                GET_MAIN_JENV;
                const CMRotationRate rotationRate = gyroData.rotationRate;
                (*env)->CallStaticVoidMethod(
                    env,
                    jIOSMotionManagerClass,
                    jIOSMotionManagerDidRotate,

                    (jfloat)rotationRate.x ,
                    (jfloat)rotationRate.y,
                    (jfloat)rotationRate.z
                );
            }];
        }
        else NSLog(@"Gyro is already active");
    }
    else NSLog(@"Gyro not available!");
}



JNIEXPORT jboolean JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSLocationManager_isHeadingAvailable
(JNIEnv *env, jclass jClass)
{
    return [CLLocationManager headingAvailable] == YES ? JNI_TRUE : JNI_FALSE;
}


/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSLocationManager
 * Method:    _init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSLocationManager__1init
(JNIEnv *env, jclass jClass)
{
    if (jIOSLocationManagerClass == NULL)
    {
        jIOSLocationManagerClass = (*env)->NewGlobalRef(env, jClass);
    }

    if (jIOSLocationManagerDidUpdateHeading == NULL)
    {
        jIOSLocationManagerDidUpdateHeading =
            (*env)->GetStaticMethodID(
                env, jIOSLocationManagerClass,
                "didUpdateHeading", "(DDDDD)V");
    }

    if (jIOSLocationManagerDidUpdateLocation == NULL)
    {
        jIOSLocationManagerDidUpdateLocation =
            (*env)->GetStaticMethodID(
                env, jIOSLocationManagerClass,
                "didUpdateLocation", "(DDDDD)V");
    }

    CLLocationManager *const locationManager = [[CLLocationManager alloc] init];
    locationManager.delegate = [[MyCLLocationManagerDelegate alloc] init];

    if ([CLLocationManager headingAvailable]) {
        locationManager.headingFilter = 5; //degrees
        [locationManager startUpdatingHeading];
    }
    if ([CLLocationManager locationServicesEnabled]) {
        locationManager.desiredAccuracy = kCLLocationAccuracyKilometer;
        locationManager.distanceFilter = 10;
        [locationManager startUpdatingLocation];
    }
}



/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSDevice
 * Method:    setProximityMonitoringEnabled
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice_setProximityMonitoringEnabled
(JNIEnv *env, jobject jObject, jboolean value)
{
    [UIDevice currentDevice].proximityMonitoringEnabled = value == JNI_TRUE ? YES : NO;
}


/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSDevice
 * Method:    isProximityMonitoringEnabled
 * Signature: (V)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice_isProximityMonitoringEnabled
(JNIEnv *env, jobject jObject)
{
    return [UIDevice currentDevice].proximityMonitoringEnabled == YES ? JNI_TRUE : JNI_FALSE;
}


/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSDevice
 * Method:    getProximityState
 * Signature: (V)Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice_getProximityState
(JNIEnv *env, jobject jObject)
{
    return [UIDevice currentDevice].proximityState == YES ? JNI_TRUE : JNI_FALSE;
}



/*
 * Class:     com_sun_javafx_ext_device_ios_sensors_IOSDevice
 * Method:    setBatteryMonitoringEnabled
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice_setBatteryMonitoringEnabled
(JNIEnv *env, jobject jObject, jboolean value)
{
    [UIDevice currentDevice].batteryMonitoringEnabled = value == JNI_TRUE ? YES : NO;
}


JNIEXPORT jfloat JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice_getBatteryLevel
(JNIEnv *env, jobject jObject)
{
    return [UIDevice currentDevice].batteryLevel;
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_ext_device_ios_sensors_IOSDevice__1getBatteryState
(JNIEnv *env, jobject jObject)
{
    return [UIDevice currentDevice].batteryState;
}
