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

#ifndef _GNU_SOURCE
#define _GNU_SOURCE // for strcasestr
#endif

#include "LensCommon.h"
#include "input/LensInput.h"

#include "com_sun_glass_events_WindowEvent.h"
#include "com_sun_glass_events_KeyEvent.h"
#include "com_sun_glass_events_MouseEvent.h"
#include "com_sun_glass_events_TouchEvent.h"
#include "com_sun_glass_ui_lens_LensApplication.h"

#include "wm/LensWindowManager.h"

#include <assert.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>

#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <fcntl.h>
#include <dirent.h>
#include <linux/input.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/epoll.h>
#include <termios.h>
#include <libudev.h>
#include <pthread.h>
#include <time.h>
#include <limits.h>


///// MACROS

// BIT handling macros
// TEST_BIT works on arrays of bytes
#define TEST_BIT(bit, array)  (array [bit / 8] & (1 << (bit % 8)))
// IS_BITSET and other similar macros work on arrays of unsigned longs
#define BITS_PER_LONG (sizeof(unsigned long) * 8)
#define NBITS(x) ((((x)-1)/BITS_PER_LONG)+1)
#define IS_BITSET(x,y) (((x)[LONG(y)] & BIT(y)) != 0)
#define SET_BIT(x,y) ((x)[LONG(y)] |= BIT(y))
#define OFF(x)   ((x)%BITS_PER_LONG)
#define LONG(x)  ((x)/BITS_PER_LONG)
#define BIT(x)   (1ul << OFF(x))

// event related macros
#define MAX_NUM_OF_DEVICES_SUPPORTED 20  // max number of concurrent connected devices
#define EVENTS_PER_READ 150 // number of queued events to try to handle at a time


//keyboard macros
#define LENSFB_KEY_PRESSED   1  // when key pressed
#define LENSFB_KEY_RELEASED  0  // when key released
#define LENSFB_KEY_REPEAT    2  // when key switches to repeating after short delay


// The environment variable used to set the test input device
#define LENS_TEST_INPUT "LENS_TEST_INPUT"


////// data structures

// The maximum number of touch points that can be handled. If events with more
// touch points are received, some points will be dropped.
#define LENS_MAX_TOUCH_POINTS 20

typedef struct _LensInputMouseState {
    /* device state */
    int                     x;
    int                     y;
    int                     rel[REL_MAX + 1];
    int                     abs[ABS_MAX + 1];

    /* multitouch points */
    int                     nextTouchID; // ID used for the next new touch point
    // the ID of the point that mouse events will be synthesize from
    int                     touchPrimaryPointID;
    /* existing touch points that have already been sent up to Glass */
    int                     touchPointCount;
    int                     touchIDs[LENS_MAX_TOUCH_POINTS];
    int                     touchXs[LENS_MAX_TOUCH_POINTS];
    int                     touchYs[LENS_MAX_TOUCH_POINTS];
    jboolean                touchIsDragging[LENS_MAX_TOUCH_POINTS];
    /* new touch points that have not yet been sent up to Glass */
    int                     pendingTouchPointCount;
    int                     pendingTouchIDs[LENS_MAX_TOUCH_POINTS];
    int                     pendingTouchXs[LENS_MAX_TOUCH_POINTS];
    int                     pendingTouchYs[LENS_MAX_TOUCH_POINTS];

    /* pending input events that have not yet been reported to the upper stack */
    struct input_event      *pendingInputEvents;
    int                     pendingInputEventCount;
    int                     pendingInputEventCapacity;
    
    int pressedX;
    int pressedY;

} LensInputMouseState;

typedef struct {
    /* eventMask contains a bitset mask of supported event types, from 0 to
     * EV_MAX */
    unsigned long eventMask[NBITS(EV_CNT)];
    /* keybits contains a bitset mask of supported keys, from 0 to KEY_MAX */
    unsigned long keybits[NBITS(KEY_MAX + 1)];
    /* relbits contains a bitset mask of supported relative axes, from 0 to
     * REL_MAX */
    unsigned long relbits[NBITS(REL_MAX + 1)];
    /* absbits contains a bitset mask of supported absoluted axes, from 0 to
     * ABS_MAX */
    unsigned long absbits[NBITS(ABS_MAX + 1)];
    /* absinfo contains data for each axis on which absolute coordinate data
     * can be provided. struct input_absinfo is defined in linux/input.h */
    struct input_absinfo absinfo[ABS_MAX + 1];
} LensInputDeviceCapabilities;

typedef struct _LensInputDeviceInfo {
    char *name;
    char *sysPath; //absolute path  (under /sys),
    char *devNode; //virtual path (under /dev)
    char *productString; //string containing vendorID and productID
    unsigned int vendorId;
    unsigned int productId;

    /* device capabilities */
    LensInputDeviceCapabilities caps;

} LensInputDeviceInfo;

typedef enum _LensInputTouchDeviceProtocols {
    TOUCH_PROTOCOL_NONE,    //this device does not support touch
    TOUCH_PROTOCOL_ST,      //single touch device
    TOUCH_PROTOCOL_MT_A,    //multi touch device with protocol A support
    TOUCH_PROTOCOL_MT_B     //multi touch device with protocol B support
} LensInputTouchDeviceProtocols;

typedef struct _LensInputDevice {

    int deviceIndex;
    int fd;
    int type;
    void *state;
    LensInputDeviceInfo *info;
    jboolean isNotified; // did we notify Glass of this device's capabilities?
    jboolean isEnabled;
    jboolean isKeyboard;
    jboolean isPointer;
    jboolean isTouch;
    LensInputTouchDeviceProtocols touchProtocolType;
    /* isTestDevice is JNI_TRUE for a device created by the test input handler */
    jboolean isTestDevice;

    /* buffer for input events */
    struct input_event      *readInputEvents;
    int                     readOffset;

    struct _LensInputDevice *previousDevice;
    struct _LensInputDevice *nextDevice;

} LensInputDevice;


/** Keybits for 5-way selector */
static const int KEYBITS_ARROWS[] = {
    KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT, 0
};
static const int KEYBITS_SELECT[] = {
    KEY_ENTER, KEY_SELECT, 0
};
/** Keybits for PC keyboard */
static const int KEYBITS_PC[] = {
    KEY_A, KEY_B, KEY_C, KEY_D, KEY_E, KEY_F, KEY_G, KEY_H, KEY_I, KEY_J,
    KEY_K, KEY_L, KEY_M, KEY_N, KEY_O, KEY_P, KEY_Q, KEY_R, KEY_S, KEY_T,
    KEY_U, KEY_V, KEY_W, KEY_X, KEY_Y, KEY_Z,
    KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8, KEY_9, KEY_0,
    KEY_LEFTSHIFT, KEY_TAB, 0
};

//// Global variables


/**
 * Describe the number of devices currently attached.
 */
static int gNumOfAttachedDevices = 0;

//Screen
static int screenWidth = 640;
static int screenHeight = 480;



//Mouse coordinates
static int mousePosX = 0;
static int mousePosY = 0;
static int newMousePosX = 0;
static int newMousePosY = 0;

// Touch
#define LENS_MAX_TAP_RADIUS 1000
static int gTapRadius = 20;//pixels

#define LENS_MAX_MOVE_SENSITIVITY 1000
static int gTouchMoveSensitivity = 20; //pixels

static jboolean gUseMultiTouch = JNI_FALSE;


//JNI
static JNIEnv *gJNIEnv = NULL;

static LensInputDevice *lensInputDevicesList_head = NULL;
static LensInputDevice *lensInputDevicesList_tail = NULL;


static int doLoop = 1;        //controlls the main polling loop

struct udev_monitor *udev_monitor;
/* Either eventLoop, udev monitor or test input monitor can have access to the
 * device list */
static pthread_mutex_t devicesLock = PTHREAD_MUTEX_INITIALIZER;
/* File descriptor used for polling input devices */
static int epollFd;
/* File descriptor used for the test input monitor */
static int testInputFD = -1;

//// Forward declarations


static jboolean lens_input_deviceCheckProperties(LensInputDevice *device,
                                                 const char *key,
                                                 const char *value);
static void lens_input_listAdd(LensInputDevice *device);
static LensResult lens_input_deviceInitCapabilities(LensInputDevice *device);
static LensResult lens_input_deviceOpen(JNIEnv *env, LensInputDevice *device);
static LensResult lens_input_deviceGrab(LensInputDevice *device, int grab);
static LensResult lens_input_mouseStateAllocateAndInit(LensInputDevice *newDevice);
static void lens_input_printDevices();
static void lens_input_deviceRemove(JNIEnv *env, LensInputDevice *device);
void lens_input_eventLoop(JNIEnv *env, void *handle);
static void lens_input_pointerEvents_handleEvent(LensInputDevice *device,
                                                 struct input_event *event);
static void lens_input_keyEvents_handleEvent(LensInputDevice *device,
                                             struct input_event *event);
static void lens_input_pointerEvents_handleSync(LensInputDevice *device);
static void lens_input_pointerEvents_handleRelMotion(LensInputDevice *device,
        struct input_event *pointerEvent);
static void lens_input_pointerEvents_handleAbsMotion(LensInputDevice *device,
        struct input_event *pointerEvent);
static void lens_input_pointerEvents_enqueuePendingEvent(LensInputMouseState *mouseState,
        struct input_event *event);
static void lens_input_deviceRelease(JNIEnv *env, LensInputDevice *device);
static void lens_input_printEvent(struct input_event event);
static LensResult lens_input_testInputHandleEvent(JNIEnv *env);
static void lens_input_deviceInfoRelease(LensInputDevice *device);
static void lens_input_deviceNotify(JNIEnv *env,
                                    LensInputDevice *device, jboolean attach);

// udev functions
static void lens_input_udevFindDevices(JNIEnv *env);
static jboolean lens_input_isUdevDeviceExists(struct udev_device *udev_device,
                                              LensInputDevice **device);
static jboolean lens_input_isDeviceExists(LensInputDevice *device);
static LensInputDevice *lens_input_deviceAllocateAndInit(JNIEnv *env,
        struct udev_device *udev_device);
static jboolean lens_input_udevMonitorStart(JNIEnv *env);
void lens_input_udevMonitorLoop(JNIEnv *env, void *handle);
static void lens_input_udevMonitorHandleEvent(JNIEnv *env);
static LensInputDeviceInfo *lens_input_deviceInfoAllocateAndInit(struct udev_device *udev_device,
        LensInputDevice *device);
static LensResult lens_input_udevParseProductID(struct udev_device *udev_device,
                                                unsigned int *vendorId,
                                                unsigned int *productId);

// test input functions
static void lens_input_testInputMonitorLoop(JNIEnv *env, void *handle);
static LensResult lens_input_testInputRead(void *_p, size_t n);
static LensResult lens_input_testInputReadInt(jint *i);
static LensResult lens_input_testInputReadString(char **pS);
static LensResult lens_input_testInputReadBitSet(unsigned long *bitset, int max);

///// stubs for connectivity with fbInputs - will be removed



///////////////////



//// Initialization section

/**
 * Initialize the input devices and start listening to events
 *
 * @param env
 */
jboolean lens_input_initialize(JNIEnv *env) {

    screenWidth = glass_screen_getMainScreen()->width;
    screenHeight = glass_screen_getMainScreen()->height;

    GLASS_LOG_FINE("screen size=%ix%i", screenWidth, screenHeight);

    //Set tap radius
    const char* className = "com/sun/glass/ui/lens/LensTouchInputSupport";
    jclass lensTouchInputSupport = (*env)->FindClass(env, className);
    if (lensTouchInputSupport != NULL) {
        jfieldID radiusVar = (*env)->GetStaticFieldID(env,lensTouchInputSupport,
                                                      "touchTapRadius", "I");
        jfieldID sensitivityVar = (*env)->GetStaticFieldID(env,
                                                           lensTouchInputSupport,
                                                           "touchMoveSensitivity",
                                                           "I");

        jfieldID useMultiVar = (*env)->GetStaticFieldID(env,
                                                           lensTouchInputSupport,
                                                           "useMultiTouch",
                                                           "Z");
        //try to set tap radius
        if (radiusVar != NULL) {
            int confRadius = (*env)->GetStaticIntField(env,
                                                       lensTouchInputSupport,
                                                       radiusVar);

            if (confRadius >= 0 && confRadius <= LENS_MAX_TAP_RADIUS ) {
                gTapRadius = confRadius;
                
                GLASS_LOG_CONFIG("Tap radius was set to: %d", gTapRadius);
            } else {
                GLASS_LOG_SEVERE("tap radius %d is out of bound (0-%d), "
                                 "using default value %d",
                                 confRadius,
                                 LENS_MAX_TAP_RADIUS,
                                 gTapRadius);
            }
            
        } else {
            GLASS_LOG_SEVERE("Could not find static touchTapRadius field in %s",
                             className);
        }

        //try to set move sensitivity
        if (sensitivityVar != NULL) {
            int confSensitivity = (*env)->GetStaticIntField(env,
                                                       lensTouchInputSupport,
                                                       sensitivityVar);

            if (confSensitivity >= 0 && confSensitivity <= LENS_MAX_MOVE_SENSITIVITY ) {
                gTouchMoveSensitivity = confSensitivity;
                
                GLASS_LOG_CONFIG("Touch move sensitivity was set to: %d",
                                 gTouchMoveSensitivity);
            } else {
                GLASS_LOG_SEVERE("Touch move sensitivity %d is out of bound (0-%d), "
                                 "using default value %d",
                                 confSensitivity,
                                 LENS_MAX_MOVE_SENSITIVITY,
                                 gTouchMoveSensitivity);
            }
            
        } else {
            GLASS_LOG_SEVERE("Could not find static touchMoveSensitivity filed in %s",
                             className);
        }

        //try to set multi-touch enabled property
        if (useMultiVar != NULL) {
            gUseMultiTouch = (*env)->GetStaticBooleanField(env,
                                                           lensTouchInputSupport,
                                                           useMultiVar);
            GLASS_LOG_CONFIG("multitouch usage was set to %s",
                             gUseMultiTouch? "true" : "false");
        } else {
            GLASS_LOG_SEVERE("Could not find static useMultiTouch filed in %s, "
                             "disabling multi touch support",
                             className);
            gUseMultiTouch = JNI_FALSE;
        }

    } else {
        GLASS_LOG_SEVERE("Could not find %s", className);
    }

    mousePosX = screenWidth / 2;
    mousePosY = screenHeight / 2;
    lens_wm_setPointerPosition(mousePosX, mousePosY);

    glass_application_request_native_event_loop(env, &lens_input_eventLoop, NULL);

    return JNI_TRUE;

}

/**
 * Traverse /dev/input for input devices.
 * When supported input device recognized they will be added to
 * LensInputDevicesList
 *
 */
static void lens_input_udevFindDevices(JNIEnv *env) {
    struct udev *udev;
    struct udev_enumerate *enumerate;
    struct udev_list_entry *udev_devices, *udev_device;
    LensInputDevice *device;

    udev = udev_new();
    if (!udev) {
        GLASS_LOG_SEVERE("Can't create udev\n");
        exit(-1);
    }

    GLASS_LOG_CONFIG("Enumerating input devices... start");

    enumerate = udev_enumerate_new(udev);
    /* Create a list of the devices in the 'input' subsystem. */
    udev_enumerate_add_match_subsystem(enumerate, "input");


    udev_enumerate_scan_devices(enumerate);
    udev_devices = udev_enumerate_get_list_entry(enumerate);

    udev_list_entry_foreach(udev_device, udev_devices) {

        const char *syspath = udev_list_entry_get_name(udev_device);
        struct udev_device *udev_device = udev_device_new_from_syspath(udev, syspath);

        GLASS_LOG_FINER("Device syspath = %s", syspath);

        if (!udev_device) {
            GLASS_LOG_FINER("No udev_device, continue");
            continue;
        }

        //check that device support input events
        if (udev_device_get_property_value(udev_device, "ID_INPUT")) {
            //add device if not exists
            if (!lens_input_isUdevDeviceExists(udev_device, NULL)) {
                device = lens_input_deviceAllocateAndInit(env, udev_device);
                if (device) {
                    lens_input_listAdd(device);
                }
            } else {
                GLASS_LOG_FINE("Device %s allready registered",
                               udev_device_get_devpath(udev_device));
            }
        } else {
            GLASS_LOG_FINE("ignoring device without input capabilities [device path %s]",
                           udev_device_get_devpath(udev_device));
        }
        //Free device object
        udev_device_unref(udev_device);
    }

    /* Free the enumerator object */
    udev_enumerate_unref(enumerate);

    /* Free udev object*/
    udev_unref(udev);

    lens_input_printDevices();
    GLASS_LOG_CONFIG("Enumerating input devices... finished");
}

/**
 * Allocate a LensInputDevice and init its fields according the
 * information from the udev_device
 *
 *
 * @param udev_device
 *
 * @return LensInputDevice* NULL if device is not valid, or
 *         error occurred
 */
static LensInputDevice *lens_input_deviceAllocateAndInit(JNIEnv *env,
        struct udev_device *udev_device) {

    const char *key, *value;
    struct udev_list_entry *set, *entry;

    LensInputDevice *device = calloc(1, sizeof(LensInputDevice));
    GLASS_LOG_FINE("Allocated device %p", device);

    const char *path = udev_device_get_devnode(udev_device);
    LensInputDeviceInfo *info;


    jboolean isValidDevice = JNI_FALSE;

    if (!device) {
        GLASS_LOG_SEVERE("Failed to allocate LensInputDevice");
        return NULL;
    }

    device->fd = -1;
    device->readOffset = 0;
    device->readInputEvents = calloc(EVENTS_PER_READ, sizeof(struct input_event));
    device->touchProtocolType = TOUCH_PROTOCOL_NONE;

    if (device->readInputEvents == NULL) {
        GLASS_LOG_SEVERE("Failed to allocate readInputEvents buffer");
        lens_input_deviceRelease(env, device);
        return NULL;
    }


    info = lens_input_deviceInfoAllocateAndInit(udev_device, device);
    if (!info) {
        GLASS_LOG_FINE("Failed to allocate LensInputDeviceInfo");
        lens_input_deviceRelease(env, device);
        return NULL;
    }

    GLASS_LOG_CONFIG("Trying to register %s [%s] as an input device", info->name, info->devNode);

    device->info = info;

    //traverse the device properties
    set = udev_device_get_properties_list_entry(udev_device);

    udev_list_entry_foreach(entry, set) {


        key = udev_list_entry_get_name(entry);
        if (!key) {
            continue;
        }
        value = udev_list_entry_get_value(entry);

        isValidDevice |= lens_input_deviceCheckProperties(device, key, value);
    }

    if (!isValidDevice) {
        GLASS_LOG_CONFIG("Device is not a valid input device (not a keyboard/mouse/touch), skipping");
        lens_input_deviceRelease(env, device);
        return NULL;
    }

    if (lens_input_deviceOpen(env, device)) {
        return NULL;
    }

    return device;
}

static jboolean lens_input_deviceCheckProperties(LensInputDevice *device,
                                                 const char *key,
                                                 const char *value) {
    jboolean isValidDevice = JNI_FALSE;

    GLASS_LOG_FINER("key[%s]=>value[%s]\n", key, value);
    if (!strcmp(key, "ID_INPUT_KEYBOARD")) {
        device->isKeyboard = JNI_TRUE;
        isValidDevice = JNI_TRUE;
        GLASS_LOG_FINE("Device is a keyboard");
    } else if (!strcmp(key, "ID_INPUT_MOUSE")) {
        device->isPointer = JNI_TRUE;
        isValidDevice = JNI_TRUE;
        GLASS_LOG_FINE("Device is a pointer");
    } else if (!strcmp(key, "ID_INPUT_TOUCHSCREEN")) {
        device->isTouch = JNI_TRUE;
        //default touch protocol to ST (single touch), which is always supported 
        //by touch devices. multi touch support protocol is checked in 
        //lens_input_deviceInitCapabilities()
        device->touchProtocolType = TOUCH_PROTOCOL_ST;
        isValidDevice = JNI_TRUE;
        GLASS_LOG_FINE("Device is a touch screen");
    }
    return isValidDevice;
}

/**
 * Allocate a LensInputDeviceInfo, init it from the udev_device
 * information and attach it to a LensInputDevice
 *
 * @param udev_device
 * @param device
 *
 * @return LensInputDeviceInfo*  NULL if device is not valid, or
 *         error occurred
 */
static LensInputDeviceInfo *lens_input_deviceInfoAllocateAndInit(struct udev_device *udev_device,
        LensInputDevice *device) {
    const char *devNode, *sysPath, *product = "", *name = "";
    struct udev_device *parent;
    LensInputDeviceInfo *info;

    unsigned int vendorId, productId;

    device->info = NULL;

    //first get the name of the device, its important to initialize internal device data
    //especially when using virtual devices (uinput)
    parent = udev_device_get_parent(udev_device);
    if (parent) {

        product = udev_device_get_property_value(parent, "PRODUCT");
        if (!product) {
            product = "";
        }

        name = udev_device_get_sysattr_value(parent, "name");
        if (!name) {
            name = udev_device_get_property_value(parent, "NAME");
        }
    }

    if (!name) {
        name = "<unnamed>";
    }

    sysPath = udev_device_get_syspath(udev_device);
    //all devices must have a /sys path
    if (!sysPath || !strcmp(sysPath, "")) {
        GLASS_LOG_FINE("Device dosen't have a valid sys path - skipping");
        return NULL;
    }

    devNode = udev_device_get_devnode(udev_device);

    //Some devices don't have a /dev node,but the ones we are instrested in do
    if (!devNode || !strcmp(devNode, "")) {
        GLASS_LOG_FINE("Device %s dosen't have a valid dev node - skipping", sysPath);
        return NULL;
    }

    info = (LensInputDeviceInfo *)calloc(1, sizeof(LensInputDeviceInfo));
    GLASS_LOG_FINE("Allocated device info %p", info);

    if (!info) {
        return NULL;
    }

    lens_input_udevParseProductID(udev_device, &info->vendorId, &info->productId);

    info->devNode = strdup(devNode);
    info->sysPath = strdup(sysPath);
    info->productString = strdup(product);
    info->name = strdup(name);

    device->info = info;

    if (!info->devNode ||
            !info->sysPath ||
            !info->productString ||
            !info->name) {
        GLASS_LOG_SEVERE("Failed to copy strings\n");
        lens_input_deviceInfoRelease(device);
        device->info = NULL;
        return NULL;
    }

    return info;
}

#define ABS_UNSET   -65535

/**
 * Configure device to be a pointer device. Used for mouse,
 * touch screen, etc.
 *
 * @param device  the device
 *
 * @return LensResult LENS_OK on success
 */
static LensResult lens_input_mouseStateAllocateAndInit(LensInputDevice *device) {

    LensInputDeviceCapabilities *caps = &device->info->caps;
    LensInputMouseState *state;

    if (device->state) {
        GLASS_LOG_FINE("Pointer is already initialized for this device [%s]",
                       device->info->name);
        return LENS_OK;
    }

    GLASS_LOG_CONFIG("Setting up mouse for %s", device->info->name);
    device->state = calloc(1, sizeof(LensInputMouseState));
    GLASS_LOG_FINE("Allocated device pointer state %p", device->state);

    if (!device->state) {
        GLASS_LOG_SEVERE("Failed to allocate LensInputMouseState");
        return LENS_FAILED;
    }

    state = (LensInputMouseState *) device->state;

    state->pressedX = 0;
    state->pressedY = 0;
    state->touchPrimaryPointID = -1; //not set

    return LENS_OK;
}

static LensResult lens_input_deviceOpen(JNIEnv *env, LensInputDevice *device) {
    device->fd = open(device->info->devNode, O_RDONLY|O_NONBLOCK);
    GLASS_LOG_FINE("open(%s) returned %i", device->info->devNode, device->fd);

    if (device->fd == -1) {
        GLASS_LOG_SEVERE("Failed to open %s [%s], %s",
                         device->info->name, device->info->devNode,
                         strerror(errno));
        lens_input_deviceRelease(env, device);
        return LENS_FAILED;
    }
    if (lens_input_deviceInitCapabilities(device)) {
        return LENS_FAILED;
    }
    if (device->isPointer || device->isTouch) {
        if (lens_input_mouseStateAllocateAndInit(device) != LENS_OK) {
            GLASS_LOG_SEVERE("Failed to setup pointer device");
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }
    }

    if (lens_input_deviceGrab(device, 1)) {
        GLASS_LOG_SEVERE("Failed to grab pointer device");
        lens_input_deviceRelease(env, device);
        return LENS_FAILED;
    }

    GLASS_LOG_CONFIG("Device %s registered for inputs", device->info->name);
    lens_input_deviceNotify(env, device, JNI_TRUE);
    return LENS_OK;
}

/**
 * Notify Glass when a device is attached or detached
 */
static void lens_input_deviceNotify(JNIEnv *env,
                                    LensInputDevice *device, jboolean attach) {
    jint flags = 0;
    int i;
    jboolean is5Way;
    jboolean isPCKeyboard;
    if (attach && device->isNotified) {
        return; // already told Glass about this device
    }
    if (!attach && !device->isNotified) {
        return; // don't notify on detachment if we did not notify on attachment
    }
    if (device->isTouch) {
        flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_TOUCH;
    } else if (device->isPointer) {
        flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_POINTER;
    }
    unsigned long *keybits = &device->info->caps.keybits[0];
    is5Way = JNI_TRUE;
    for (i = 0; KEYBITS_ARROWS[i] != 0; i++) {
        int key = KEYBITS_ARROWS[i];
        if (!IS_BITSET(keybits, key)) {
            is5Way = JNI_FALSE;
            GLASS_LOG_CONFIG("Not a 5-way, missing key %i", key);
            break;
        }
    }
    if (is5Way) {
        jboolean hasSelect = JNI_FALSE;
        for (i = 0; KEYBITS_SELECT[i] != 0; i++) {
            int key = KEYBITS_SELECT[i];
            if (IS_BITSET(keybits, key)) {
                GLASS_LOG_CONFIG("Is a 5-way, has arrow keys and key %i", key);
                hasSelect = JNI_TRUE;
                break;
            }
        }
        if (!hasSelect) {
            GLASS_LOG_CONFIG("Not a 5-way, has arrow keys but no select key");
        }
        is5Way = hasSelect;
    }
    if (is5Way) {
        flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_5WAY;
        // a 5-way selector could also be a PC keyboard
        jboolean isPCKeyboard = JNI_TRUE;
        for (i = 0; KEYBITS_PC[i] != 0; i++) {
            int key = KEYBITS_PC[i];
            if (!IS_BITSET(keybits, key)) {
                isPCKeyboard = JNI_FALSE;
                GLASS_LOG_CONFIG("Not a PC keyboard, missing key %i", key);
                break;
            }
        }
        if (isPCKeyboard) {
            GLASS_LOG_CONFIG("Is a PC keyboard");
            flags |= 1 << com_sun_glass_ui_lens_LensApplication_DEVICE_PC_KEYBOARD;
        }
    }
    glass_application_notifyDeviceEvent(env, flags, attach);
    if (attach) {
        device->isNotified = JNI_TRUE; // record that we notified Glass about the device
    } else {
        device->isNotified = JNI_FALSE; // Glass no longer knows about the device
    }
}

/**
 * Close all registered devices that where opened by
 * lens_input_initialize() and free their resources
 */
void lens_input_shutdownDevices(JNIEnv *env) {

    LensInputDevice *device = lensInputDevicesList_head;
    LensInputDevice *nextDevice;
    while (device) {
        nextDevice = device->nextDevice;
        lens_input_deviceRemove(env, device);
        device = nextDevice;
    }
    if (testInputFD >= 0) {
        GLASS_LOG_FINE("close(%i) (test input monitor)", testInputFD);
        close(testInputFD);
    }
}

/**
 * Grabs or releases a device
 *
 * @param grab 1 to grab a device, 0 to release it
 */
LensResult lens_input_deviceGrab(LensInputDevice *device, int grab) {
    if (device->isTestDevice) {
        // this is a test device, we don't need to grab or release it
        return LENS_OK;
    }
    GLASS_LOG_FINER("ioctl(%s, EVIOCGRAB, %i)", device->info->name, grab);
    if (ioctl(device->fd, EVIOCGRAB, grab) < 0) {
        if (grab) {
            GLASS_LOG_SEVERE("Grabbing device [%s] failed - %s",
                             device->info->name, strerror(errno));
        } else {
            GLASS_LOG_WARNING("Ungrabbing device %s [fd-%i] failed - %s",
                              device->info->name, device->fd, strerror(errno));
        }
        return LENS_FAILED;
    } else {
        return LENS_OK;
    }
}

/** Wraps a call to ioctl of type EVIOCGBIT, reporting a SEVERE error if the
 * call fails */
static LensResult eviocgbit(LensInputDevice *device,
                            int type, size_t dstLength, void *dst) {
    GLASS_LOG_FINEST("ioctl(%s, EVIOCGBIT %i)", device->info->name, type);
    if (ioctl(device->fd, EVIOCGBIT(type, dstLength), dst) < 0) {
        GLASS_LOG_CONFIG("%s (%s) -> EVIOCGBIT(%i) error %i: %s",
                         device->info->name,
                         device->info->devNode,
                         type,
                         errno, 
                         strerror(errno));
        return LENS_FAILED;
    } else {
        return LENS_OK;
    }
}

/**
 * Sets up the capabilities of a device
 *
 * On success returns either LENS_OK with the LensInputDeviceCapabilities
 * structure of the given device filled in. On failure, returns LENS_FAILED and
 * logs a SEVERE error.
 */
static LensResult lens_input_deviceInitCapabilities(LensInputDevice *device) {
    if (device->isTestDevice) {
        return LENS_OK;
    } else {
        LensInputDeviceCapabilities *caps = &device->info->caps;
        if (eviocgbit(device, 0 /* EV_ */,
                      sizeof(caps->eventMask), &caps->eventMask)) {
            return LENS_FAILED;
        }
        if (IS_BITSET(caps->eventMask, EV_KEY)) {
            GLASS_LOG_CONFIG("Init keybits");
            if (eviocgbit(device, EV_KEY,
                          sizeof(caps->keybits), &caps->keybits)) {
                return LENS_FAILED;
            }
        }
        if (IS_BITSET(caps->eventMask, EV_REL)) {
            if (eviocgbit(device, EV_REL,
                          sizeof(caps->relbits), &caps->relbits)) {
                return LENS_FAILED;
            }
        }
        if (IS_BITSET(caps->eventMask, EV_ABS)) {
            int axis;
            if (eviocgbit(device, EV_ABS,
                          sizeof(caps->absbits), &caps->absbits)) {
                return LENS_FAILED;
            }
            //used to determine which multi touch protocol supported by the device
            jboolean isProtocol_A_Supported = JNI_FALSE;
            jboolean isProtocol_B_Supported = JNI_FALSE;
            for (axis = 0; axis <= ABS_MAX; axis++) {
                if (IS_BITSET(caps->absbits, axis)) {
                    GLASS_LOG_FINEST("ioctl(%s, EVIOCABS %i)",
                                     device->info->name, axis);
                    if (ioctl(device->fd,
                              EVIOCGABS(axis), &caps->absinfo[axis]) < 0) {
                        GLASS_LOG_SEVERE("EVIOCGABS(%i) error %i: %s",
                                         axis, errno, strerror(errno));
                        return LENS_FAILED;
                    }

                    //check for multi touch events
                    if (axis == ABS_MT_SLOT) {
                        //ABS_MT_SLOT event is unique to multi touch protocol B devices                        
                        isProtocol_B_Supported = JNI_TRUE;
                    }

                    if (axis == ABS_MT_POSITION_X) {
                        //ABS_MT_POSITION_X is used by both protocol A & B multi touch devices                        
                        isProtocol_A_Supported = JNI_TRUE;
                    }

                    GLASS_LOG_CONFIG("Range for axis 0x%02x is %i..%i", axis,
                                     caps->absinfo[axis].minimum,
                                     caps->absinfo[axis].maximum);
                }
            }

            //check the level of multi touch support of the device. If device is
            //single touch it was already markes as such in 
            //lens_input_deviceCheckProperties()
            if (isProtocol_A_Supported) {
                //we are definitely multi touch
                if (isProtocol_B_Supported) {
                    //currently protocol B is not supported, fallback to 
                    //protocol A. (protocol B is implemented on top of protocol 
                    //A)
                    device->touchProtocolType = TOUCH_PROTOCOL_MT_A;
                } else {
                    device->touchProtocolType = TOUCH_PROTOCOL_MT_A;                    
                }

                if (device->touchProtocolType == TOUCH_PROTOCOL_MT_A || 
                    device->touchProtocolType == TOUCH_PROTOCOL_MT_B) {
                    GLASS_LOG_CONFIG("device %s is multi touch",
                                     device->info->name);
                }

            }

        }
        return LENS_OK;
    }
}

//// Initialization section - END


/// epoll functions

/**
 * Remove and disable device notifications
 *
 * @param device the device to remove
 */
void lens_input_epollRemoveDevice(LensInputDevice *device) {

    int ret;

    if (device) {

        //remove from epoll list

        GLASS_LOG_FINE("epollctl(%i, EPOLL_CTL_DEL, fd=%i)",
                       epollFd, device->fd);
        ret = epoll_ctl(epollFd, EPOLL_CTL_DEL, device->fd, NULL);

        if (ret == -1) {
            GLASS_LOG_SEVERE("Failed to EPOLL_CTL_DEL %s to epoll - [errno %i] %s",
                             device->info->name, errno, strerror(errno));
        }

        device->isEnabled = JNI_FALSE;
    }
}

/**
 * Enable notification for a device
 *
 * @param device the device to add into notification pool
 */
void lens_input_epolladdDevice(LensInputDevice *device) {

    struct epoll_event epollEvent;
    int ret;

    if (device) {

        //init
        memset(&epollEvent, 0, sizeof(epollEvent));
        epollEvent.events = EPOLLIN;
        epollEvent.data.ptr = device;

        //add device to epoll list

        GLASS_LOG_FINE("epollctl(%i, EPOLL_CTL_ADD, fd=%i, device=%p)",
                       epollFd, device->fd, device);
        ret = epoll_ctl(epollFd, EPOLL_CTL_ADD, device->fd, &epollEvent);
        if (ret == -1) {
            GLASS_LOG_WARNING("Failed to add %s to epoll, skipping - [errno %i] %s",
                              device->info->name, errno, strerror(errno));
        } else {
            device->isEnabled = JNI_TRUE;
        }
    }
}


///////// event handling

/**
 * The main event loop that polls events from the system and
 * later call the relevant event handlers.
 * A nativeEventLoopCallback() implementation
 *
 * @param env
 * @param handle always NULL at this point, required as
 *               nativeEventLoopCallback signature
 */
void lens_input_eventLoop(JNIEnv *env, void *handle) {

    int numOfEpollEvents;
    struct epoll_event epollEvent;
    struct epoll_event *epollEvents = calloc(MAX_NUM_OF_DEVICES_SUPPORTED,
                                             sizeof(struct epoll_event));
    int i;
    LensInputDevice *device;
    const char *testInputPath = getenv(LENS_TEST_INPUT);
    jboolean useTestInput;
    gJNIEnv = env;

    if (epollEvents == NULL) {
        GLASS_LOG_SEVERE("Failed to alloc epollEvents - [errno %i] %s", errno, strerror(errno));
        exit(-1);
    }

    GLASS_LOG_FINE("Allocated epollEvents %p", epollEvents);

    useTestInput = testInputPath != NULL && strlen(testInputPath) > 0;

    if (!useTestInput) {
        // find and register our devices
        lens_input_udevFindDevices(env);
    }

    //+1 is to make sure we don't call epoll_create(0) that might
    //cause an 'invalid argument' error when no devices are connected/detected
    epollFd =  epoll_create(gNumOfAttachedDevices + 1);

    if (epollFd == -1) {
        GLASS_LOG_SEVERE("Failed to create epoll - [errno %i] %s", errno, strerror(errno));
        exit(-1);
    }

    GLASS_LOG_FINER("epollFd = %i\n", epollFd);

    //register the devices we want to get input events from
    device = lensInputDevicesList_head;
    while (device) {
        lens_input_epolladdDevice(device);
        device = device->nextDevice;
    }

    //grab the lock so the event loop will start before monitor events
    pthread_mutex_lock(&devicesLock);

    //start monitoring hot plug
    if (useTestInput) {
        glass_application_request_native_event_loop(
            env, lens_input_testInputMonitorLoop, (void *) testInputPath);
    } else {
        lens_input_udevMonitorStart(env);
    }

    while (doLoop) {
        int epoll_errno;

        //Before wait release the lock
        GLASS_LOG_FINER("Releasing lock before epoll_wait()");
        pthread_mutex_unlock(&devicesLock);

        numOfEpollEvents = epoll_wait(epollFd, epollEvents,
                                          MAX_NUM_OF_DEVICES_SUPPORTED, -1);

        epoll_errno = errno;

        GLASS_LOG_FINEST("epoll_wait(fd=%i) returned() %i",
                         epollFd, numOfEpollEvents);

        //we got input event(s), process them before udev monitor will chage
        //stuff around
        GLASS_LOG_FINER("Trying to capture lock before reading events");
        pthread_mutex_lock(&devicesLock);
        GLASS_LOG_FINER("lock captured");

        if (numOfEpollEvents == -1) {
            if (epoll_errno == EINTR) {
                //we got interrupted
                GLASS_LOG_FINER("epoll_wait(): %s", strerror(epoll_errno));
            } else {
                GLASS_LOG_WARNING("epoll_wait(): error %i (%s)",
                                  epoll_errno, strerror(epoll_errno));
            }
            continue;
        } else if (numOfEpollEvents == 0) {
            GLASS_LOG_WARNING("0 events should only happens when timer is set, ignoring");
            continue;
        }

        for (i = 0 ; i < numOfEpollEvents ; i++) {
            epollEvent = epollEvents[i];
            device = (LensInputDevice *)epollEvent.data.ptr;
            GLASS_LOG_FINEST("epoll event %i out of %i, device=%p",
                             i, numOfEpollEvents, device);
            if (!lens_input_isDeviceExists(device)) {
                GLASS_LOG_FINE("Device %p doesn't exist anymore, skipping event", device);
                continue;
            }

            GLASS_LOG_FINEST("events=0x%x, device=%p (%s), device->fd=%d",
                             epollEvent.events,
                             device,
                             (device) ? device->info->name : NULL,
                             (device) ? device->fd : -1);
            //error handling
            if ((epollEvent.events & EPOLLERR) ||
                    (epollEvent.events & EPOLLHUP) ||
                    (!(epollEvent.events & EPOLLIN))) {
                int ret;
                /* An error has occurred on this fd, or the socket is not
                   ready for reading (why were we notified then?) */
                GLASS_LOG_FINEST("epoll error");

                /* Explicitly remove the item from the epoll list
                   udev monitor will remove the device from
                   lensInputDevicesList*/
                lens_input_epollRemoveDevice(device);
                continue;
            }

            //handle events
            if (device) {
                int numOfEvents;
                int eventIndex;
                int numOfBytesRead;
                int numOfRemainingBytes;
                char* readBuffer = (char*)(device->readInputEvents);
                const int readBufferSize = (int)sizeof(struct input_event) * EVENTS_PER_READ;

                do {
                    numOfBytesRead = read(device->fd, readBuffer + device->readOffset, 
                                          readBufferSize - device->readOffset);

                    if (numOfBytesRead > 0) {
                        device->readOffset += numOfBytesRead;
                    }

                } while ((device->readOffset < readBufferSize ) && 
                         (numOfBytesRead > 0 || (numOfBytesRead < 0 && errno == EINTR)));


                if (numOfBytesRead < 0 && errno != EINTR && errno != EWOULDBLOCK) {
                    GLASS_LOG_SEVERE("error reading %s, read offset=%i fd=%i, errno=%i (%s)",
                                     device->info->name, device->readOffset, device->fd, 
                                     errno, strerror(errno));
                    lens_input_epollRemoveDevice(device);
                    continue; // of for-loop
                } 

                numOfEvents = device->readOffset / (int)sizeof(struct input_event);

                    GLASS_LOG_FINEST("Got event on %s, count=%i",
                                     device->info->name, numOfEvents);


                for (eventIndex = 0; eventIndex < numOfEvents; eventIndex ++) {

                    lens_input_printEvent(device->readInputEvents[eventIndex]);

                    if (device->isKeyboard) {
                        lens_input_keyEvents_handleEvent(device, &device->readInputEvents[eventIndex]);
                    } else if (device->isPointer || device->isTouch) {
                        lens_input_pointerEvents_handleEvent(device, &device->readInputEvents[eventIndex]);
                    }
                }
           
                numOfRemainingBytes = device->readOffset % (int)sizeof(struct input_event);
                if (numOfRemainingBytes == 0) {
                    // most (if not all) of the times
                    device->readOffset = 0;
                } else {
                    memmove(readBuffer, readBuffer + device->readOffset - numOfRemainingBytes, numOfRemainingBytes);
                    device->readOffset = numOfRemainingBytes;
                }           

            } else {
                GLASS_LOG_WARNING("Null device, skipping event");
                continue;
            }
        }//for
    }//while

    free(epollEvents);
}

////// mouse and touch events handling

/**
 * Service function that translate FB button code into FX code.
 * @return  int com_sun_glass_events_MouseEvent_BUTTON_*
 */

int lens_input_convertButtonToFXButtonCode(int button) {
    switch (button) {
        case 0:
            return com_sun_glass_events_MouseEvent_BUTTON_NONE;
        case BTN_LEFT:
        case BTN_TOUCH:
            return com_sun_glass_events_MouseEvent_BUTTON_LEFT;
        case BTN_MIDDLE:
            return com_sun_glass_events_MouseEvent_BUTTON_OTHER;
        case BTN_RIGHT:
            return com_sun_glass_events_MouseEvent_BUTTON_RIGHT;
        default:
            GLASS_LOG_WARNING("Error: unknown button=%02d return NONE", button);
            return com_sun_glass_events_MouseEvent_BUTTON_NONE;
    }
}





/**
 * Handle pointer device events
 *
 * @param device the device that produced the event
 * @param event the event produced
 */
static void lens_input_pointerEvents_handleEvent(LensInputDevice *device,
                                                 struct input_event *event) {

    switch (event->type) {
        case EV_SYN:
            if (event->code == SYN_REPORT) {
                // this event is complete
                lens_input_pointerEvents_handleSync(device);
            } else {
                // this EV_SYN event is a delimiter within the event, such as
                // SYN_MT_REPORT
                lens_input_pointerEvents_enqueuePendingEvent(
                    (LensInputMouseState *)device->state, event);
            }
            break;
        case EV_KEY:
        case EV_REL:
        case EV_ABS:
            lens_input_pointerEvents_enqueuePendingEvent(
                (LensInputMouseState *)device->state, event);
            break;
        default:
            GLASS_LOG_FINEST("unsupported event Mouse type=0x%x code=%i value=%i"
                             " - skipping", event->type, event->code, event->value);
    }
}

/**
 * Handle pointer absolute coordinates notification
 *
 * @param device the device that produced the event
 * @param pointerEvent the event produced
 */
static void lens_input_pointerEvents_handleAbsMotion(LensInputDevice *device,
        struct input_event *pointerEvent) {
    LensInputMouseState *mouseState = device->state;
    LensInputDeviceCapabilities *caps = &device->info->caps;
    int axis = pointerEvent->code;
    float scalar;

    // Handle absolute coordinate changes
    // This only works for direct touch devices such as touch screens
    // but not devices that need to be converted to relative motion
    // such as a touchpad

    mouseState->abs[axis] = pointerEvent->value;
    if (mouseState->abs[axis] < caps->absinfo[axis].minimum) {
        mouseState->abs[axis] = caps->absinfo[axis].minimum;
    }
    if (mouseState->abs[axis] > caps->absinfo[axis].maximum) {
        mouseState->abs[axis] = caps->absinfo[axis].maximum;
    }
    scalar = ((pointerEvent->value - caps->absinfo[axis].minimum))
             / (float)(caps->absinfo[axis].maximum - caps->absinfo[axis].minimum);
    GLASS_LOG_FINER("Absolute motion on axis 0x%02x, value = %i..%i, value=%i, scalar=%f\n",
                    axis, caps->absinfo[axis].minimum, caps->absinfo[axis].maximum,
                    pointerEvent->value, scalar);
    switch (axis) {
        case ABS_X:
            newMousePosX = (int) roundf(scalar * screenWidth);
            mouseState->pressedX = newMousePosX;
            break;
        case ABS_Y:
            newMousePosY = (int) roundf(scalar * screenHeight);
            mouseState->pressedY = newMousePosY;
            break;
        case ABS_MT_POSITION_X:
            mouseState->pendingTouchXs[mouseState->pendingTouchPointCount] =
                    (int) roundf(scalar * screenWidth);
            break;
        case ABS_MT_POSITION_Y:
            mouseState->pendingTouchYs[mouseState->pendingTouchPointCount] =
                    (int) roundf(scalar * screenHeight);
            break;
    }
    GLASS_LOG_FINER("Pointer absolute axis 0x%02x is now %i, pointer at %i,%i",
                    axis, mouseState->abs[axis], newMousePosX, newMousePosY);
}

/**
 * Handle pointer relative coordinates notification
 *
 * @param device the device that produced the event
 * @param pointerEvent the event produced
 */
static void lens_input_pointerEvents_handleRelMotion(LensInputDevice *device,
        struct input_event *pointerEvent) {
    LensInputMouseState *mouseState = device->state;
    int axis = pointerEvent->code;
    mouseState->rel[axis] += pointerEvent->value;
    switch (axis) {
        case REL_X:
            newMousePosX = mousePosX + pointerEvent->value;
            if (newMousePosX >= screenWidth) {
                newMousePosX = screenWidth - 1;
            } else if (newMousePosX < 0) {
                newMousePosX = 0;
            }
            break;
        case REL_Y:
            newMousePosY = mousePosY + pointerEvent->value;
            if (newMousePosY >= screenHeight) {
                newMousePosY = screenHeight - 1;
            } else if (newMousePosY < 0) {
                newMousePosY = 0;
            }
            break;
    }
    GLASS_LOG_FINER("Pointer relative axis 0x%02x is now %i, pointer at %i,%i",
                    axis, mouseState->rel[axis], newMousePosX, newMousePosY);
}




static void lens_input_pointerEvents_handleKeyEvent(LensInputDevice *device,
        struct input_event *pointerEvent) {

    jboolean isPressed = (pointerEvent->value == 1) ? JNI_TRUE : JNI_FALSE;


    int button = lens_input_convertButtonToFXButtonCode(pointerEvent->code);

    GLASS_LOG_FINE("Notify button event %i %s at %i,%i",
                   button,
                   isPressed ? "pressed" : "released",
                   mousePosX, mousePosY);
    lens_wm_notifyButtonEvent(gJNIEnv, isPressed,
                              button,
                              mousePosX, mousePosY);
}

/**
 * Handle pointer sync notification. The event is complete we
 * can now notify upper layers for pointer event
 *
 * @param device the device that produced the event
 * @param pointerEvent the event produced
 */
static void lens_input_pointerEvents_handleSync(LensInputDevice *device) {
    int i;
    LensInputMouseState *mouseState = device->state;
    int keyEventIndex = -1;
    jboolean reportMouseMove = JNI_FALSE;
    mouseState->pendingTouchPointCount = 0;
    mouseState->pressedX = mouseState->pressedY = -1;
    int numOfMTPoints = 0;
    int touchButtonValue = -1; //not set

    //Pass on the events of this sync
    for (i = 0; i < mouseState->pendingInputEventCount; i++) {
        struct input_event *pointerEvent = &mouseState->pendingInputEvents[i];

        switch (pointerEvent->type) {
            case EV_KEY:
                if (pointerEvent->code == BTN_TOUCH) {
                    touchButtonValue = pointerEvent->value;
                }
                keyEventIndex = i;
                break;
            case EV_REL:
                lens_input_pointerEvents_handleRelMotion(device, pointerEvent);
                reportMouseMove = JNI_TRUE;
                break;
            case EV_ABS:
                lens_input_pointerEvents_handleAbsMotion(device, pointerEvent);
                if (mouseState->pendingTouchPointCount < LENS_MAX_TOUCH_POINTS &&
                    pointerEvent->code == ABS_MT_POSITION_X) {
                    numOfMTPoints++;
                }
                break;
            case EV_SYN:
                if (pointerEvent->code == SYN_MT_REPORT) {
                    if (mouseState->pendingTouchPointCount < LENS_MAX_TOUCH_POINTS) {
                        mouseState->pendingTouchPointCount ++;
                    } else {
                        // We are past how many touch points we expect to be
                        // reported. For n touch points, where n >
                        // LENS_MAX_TOUCH_POINTS, drop the points from
                        // LENS_MAX_TOUCH_POINTS to (n-1). For example, if we
                        // get 30 touch points and can only support 20, record
                        // only points 1-19 and point 30. This is arbitrary,
                        // just because it is easy to code.  We could do
                        // something different here.
                    }
                }
                break;
            default:
                // The queue should not hold other event
                assert(0);
        }
    }

    //if device is ST, convert event to pending touch event.
    //assigning ID and determining state will be done in touch shared code
    //below
    if (device->touchProtocolType == TOUCH_PROTOCOL_ST) {
        GLASS_LOG_FINEST("ST device event, touchButtonValue = %d",
                         touchButtonValue);
        //if BTN_TOUCH was sent it must be honored
        if (touchButtonValue == 1) {
            GLASS_LOG_FINEST("ST - pressed on %d %d",
                             mouseState->pressedX,
                             mouseState->pressedY);
            //we need to record only pressed events
            mouseState->pendingTouchPointCount = 1; //we always have 1 event
            mouseState->pendingTouchXs[0] = mouseState->pressedX;
            mouseState->pendingTouchYs[0] = mouseState->pressedY;
        } else if (touchButtonValue == -1 && mouseState->pressedX != -1 && mouseState->pressedY != -1) {
            GLASS_LOG_FINEST("ST - press event with no button on %d %d",
                             mouseState->pressedX,
                             mouseState->pressedY);
            //we have a touch event without a BTN_TOUCH event
            mouseState->pendingTouchPointCount = 1; //we always have 1 event
            mouseState->pendingTouchXs[0] = mouseState->pressedX;
            mouseState->pendingTouchYs[0] = mouseState->pressedY;            
        } else if (touchButtonValue == 0) {
            //release
             GLASS_LOG_FINEST("ST - RELEASE");

        }
    }

    /**
     * release event can have 3 forms for protocol A devices
     * 1)
     * SYN_MT_REPORT
     * SYN_REPORT
     * 
     * 2)
     * EV_KEY BTN_TOUCH 0
     * EV_SYN_MT_REPORT 0
     * SYN_REPORT
     * 
     * 3)
     * EV_KEY BTN_TOUCH 0
     * SYN_REPORT
     * 
     * As BTN_TOUCH is optional for multi touch devices and we only
     * intersted in the pressed points we need to make sure that
     * pendingTouchPointCount holds the correct number of points
     */
    if (device->touchProtocolType == TOUCH_PROTOCOL_MT_A) {
        if (numOfMTPoints < mouseState->pendingTouchPointCount) {             
             GLASS_LOG_FINEST("MT_A - updating pendingTouchPointCount from "
                              "%d to %d",
                              mouseState->pendingTouchPointCount,
                              numOfMTPoints);
             mouseState->pendingTouchPointCount = numOfMTPoints;
        } else if (numOfMTPoints > mouseState->pendingTouchPointCount) {
            GLASS_LOG_SEVERE("malformed multi touch event - ignoring");
            mouseState->pendingInputEventCount = 0;
            return;
        }
    }

    //at this point ST devices and MT_A devices touch points are registered
    //in mouseState->pending* variables and can use same processing for IDs
    //and states
    GLASS_LOG_FINEST("Number of touch points - pre-existing %d new %d",
                     mouseState->touchPointCount,
                     mouseState->pendingTouchPointCount);
    //assign IDS to touch points 
    if (mouseState->pendingTouchPointCount) {
        // assign IDs to touch points        
        if (mouseState->touchPointCount == 0) {
            // no pre-existing touch points, so assign any IDs
            GLASS_LOG_FINEST("no pre-existing touch points");
            mouseState->nextTouchID = 1;
            for (i = 0; i < mouseState->pendingTouchPointCount; i++) {
                mouseState->pendingTouchIDs[i] = mouseState->nextTouchID++;
            }
        } else if (mouseState->pendingTouchPointCount >= mouseState->touchPointCount) {
            // For each existing touch point, find the closest pending touch
            // point.
            // mapped indices contains 0 for every unmapped pending touch point
            // index  and 1 for every pending touch point index that has
            // already been mapped to an existing touch point.
            int mappedIndices[LENS_MAX_TOUCH_POINTS];
            memset(mappedIndices, 0, sizeof(mappedIndices));
            int mappedIndexCount = 0;
            GLASS_LOG_FINEST("pendingTouchPointCount >= touchPointCount");
            for (i = 0; i < mouseState->touchPointCount; i++) {
                int x = mouseState->touchXs[i];
                int y = mouseState->touchYs[i];
                int j;
                int closestDistanceSquared = INT_MAX;
                int mappedIndex = -1;
                for (j = 0; j < mouseState->pendingTouchPointCount; j++) {
                    if (mappedIndices[j] == 0) {
                        int distanceX = x - mouseState->pendingTouchXs[j];
                        int distanceY = y - mouseState->pendingTouchYs[j];
                        int distanceSquared = distanceX * distanceX + distanceY * distanceY;
                        if (distanceSquared < closestDistanceSquared) {
                            mappedIndex = j;
                            closestDistanceSquared = distanceSquared;
                        }
                    }
                }
                assert(mappedIndex >= 0);
                mouseState->pendingTouchIDs[mappedIndex] = mouseState->touchIDs[i];
                mappedIndexCount ++;
                mappedIndices[mappedIndex] = 1;
                GLASS_LOG_FINEST("Assigning id %d to pendingTouchIDs[%d] from "
                                 "touchIDs[%d]",
                                 mouseState->touchIDs[i],
                                 mappedIndex,
                                 i);
            }
            if (mappedIndexCount < mouseState->pendingTouchPointCount) {
                GLASS_LOG_FINEST("%d points are new",
                                 mouseState->pendingTouchPointCount - mappedIndexCount);
                for (i = 0; i < mouseState->pendingTouchPointCount; i++) {
                    if (mappedIndices[i] == 0) {
                        GLASS_LOG_FINEST("Assigning id %d to pendingTouchIDs[%d]",
                                          mouseState->nextTouchID,
                                          i);
                        mouseState->pendingTouchIDs[i] = mouseState->nextTouchID++;
                    }
                }
            }
        } else {
            // There are more existing touch points than pending touch points.
            // For each pending touch point, find the closest existing touch
            // point.
            // mappedIndices contains 0 for every unmapped pre-existing touch
            // index and 1 for every pre-existing touch index that has already
            // been mapped to a pending touch point
            int mappedIndices[LENS_MAX_TOUCH_POINTS];
            memset(mappedIndices, 0, sizeof(mappedIndices));
            int mappedIndexCount = 0;
            GLASS_LOG_FINEST("pendingTouchPointCount < touchPointCount");
            for (i = 0; i < mouseState->pendingTouchPointCount
                    && mappedIndexCount < mouseState->touchPointCount; i++) {
                int x = mouseState->pendingTouchXs[i];
                int y = mouseState->pendingTouchYs[i];
                int j;
                int closestDistanceSquared = INT_MAX;
                int mappedIndex = -1;
                for (j = 0; j < mouseState->touchPointCount; j++) {
                    if (mappedIndices[j] == 0) {
                        int distanceX = x - mouseState->touchXs[j];
                        int distanceY = y - mouseState->touchYs[j];
                        int distanceSquared = distanceX * distanceX + distanceY * distanceY;
                        if (distanceSquared < closestDistanceSquared) {
                            mappedIndex = j;
                            closestDistanceSquared = distanceSquared;
                        }
                    }
                }
                assert(mappedIndex >= 0);
                mouseState->pendingTouchIDs[i] = mouseState->touchIDs[mappedIndex];
                mappedIndexCount ++;
                mappedIndices[mappedIndex] = 1;
                GLASS_LOG_FINEST("Assigning id %d to pendingTouchIDs[%d] from "
                                 "touchIDs[%d]",
                                 mouseState->touchIDs[mappedIndex],
                                 i,
                                 mappedIndex);
            }
        }
    }

    //process touch points states and prepare data structures for notification
    jint count = 0;
    jint states[LENS_MAX_TOUCH_POINTS];
    jlong ids[LENS_MAX_TOUCH_POINTS];
    int xs[LENS_MAX_TOUCH_POINTS];
    int ys[LENS_MAX_TOUCH_POINTS];
    jboolean needToSavePendingPoints = JNI_TRUE;

    if (mouseState->pendingTouchPointCount) {
        // have touch event(s)
        // Process STATIONARY, MOVE and RELEASED TouchPoints
        for (i = 0; i < mouseState->touchPointCount; i++) {
            int j;
            jlong id = mouseState->touchIDs[i];
            jboolean matched = JNI_FALSE;
            ids[count] = id;
            for (j = 0; j < mouseState->pendingTouchPointCount && !matched; j++) {
                if (mouseState->pendingTouchIDs[j] == id) {
                    int newX = mouseState->pendingTouchXs[j];
                    int newY = mouseState->pendingTouchYs[j];
                    int oldX = mouseState->touchXs[i];
                    int oldY = mouseState->touchYs[i];

                    //delta of each axis
                    int dX = newX - oldX;
                    int dY = newY - oldY;

                    //touch point get a move only when its moved out the tap radius
                    //after first move (dragging) all moves should be reported 
                    //as long as the move event is bigger then gTouchMoveSensitivity
                    //threshold
                    if (mouseState->touchIsDragging[i]) {
                        //we are in 'drag' check if event is outside sensativity bounds
                        if (dX * dX + dY * dY >= gTouchMoveSensitivity * gTouchMoveSensitivity ) {
                            //delta is bigger then threshold - report as MOVE
                            states[count] = com_sun_glass_events_TouchEvent_TOUCH_MOVED;
                            xs[count] = newX;
                            ys[count] = newY;
                            GLASS_LOG_FINEST("point %d sensitivity check -> MOVE", count+1);
                        } else {
                            //delta is smaller then threshold -report as STILL and clamp values
                            states[count] = com_sun_glass_events_TouchEvent_TOUCH_STILL;
                            xs[count] = oldX;
                            ys[count] = oldY;
                            mouseState->pendingTouchXs[j] = oldX;
                            mouseState->pendingTouchYs[j] = oldY;
                            GLASS_LOG_FINEST("point %d sensitivity check -> STILL", count+1);
                        }
                    } else {
                        //first move - check if event is outside the tap radius
                        if (dX * dX + dY * dY <= gTapRadius * gTapRadius) {
                            //clamp the position of the point to the previous 
                            //position to prevent point crawling
                            states[count] = com_sun_glass_events_TouchEvent_TOUCH_STILL;
                            xs[count] = oldX;
                            ys[count] = oldY;
                            mouseState->pendingTouchXs[j] = oldX;
                            mouseState->pendingTouchYs[j] = oldY;
                            GLASS_LOG_FINEST("point %d tap radius check -> STILL", count+1);

                        } else {
                            states[count] = com_sun_glass_events_TouchEvent_TOUCH_MOVED;
                            xs[count] = newX;
                            ys[count] = newY;
                            //mark the pending point as drag
                            mouseState->touchIsDragging[j] = JNI_TRUE;
                            GLASS_LOG_FINEST("point %d tap radius check -> MOVE", count+1);
                        }
                    }
                    matched = JNI_TRUE;
                }
            }
            if (!matched) {
                states[count] = com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
                xs[count] = mouseState->touchXs[i];
                ys[count] = mouseState->touchYs[i];
                GLASS_LOG_FINEST("point %d - no match -> RELEASE", count+1);
                //release the drag
                mouseState->touchIsDragging[i] = JNI_FALSE;
            }
            count ++;
        }
        // Process PRESSED TouchPoints
        for (i = 0; i < mouseState->pendingTouchPointCount; i++) {
            int j;
            jlong id = mouseState->pendingTouchIDs[i];
            jboolean matched = JNI_FALSE;
            for (j = 0; j < mouseState->touchPointCount && !matched; j++) {
                if (mouseState->touchIDs[j] == id) {
                    matched = JNI_TRUE;
                    break;
                }
            }
            if (!matched) {
                ids[count] = id;
                xs[count] = mouseState->pendingTouchXs[i];
                ys[count] = mouseState->pendingTouchYs[i];
                states[count] = com_sun_glass_events_TouchEvent_TOUCH_PRESSED;
                mouseState->touchIsDragging[i] = JNI_FALSE;
                GLASS_LOG_FINEST("point %d - no match -> PRESSED", count+1);
                count ++;
                
            }
        }
    } else if (device->isTouch && mouseState->touchPointCount){
        //no new touch events, but some old ones - release all previous points
        count = mouseState->touchPointCount;
        //com_sun_glass_events_TouchEvent_TOUCH_RELEASED is never registered in
        //MouseState, so all previous touch events are press/move events and need
        // to be released
        GLASS_LOG_FINEST("All points (%d) -> RELEASE", count);
        for (i = 0; i < mouseState->touchPointCount; i++) {
            ids[i] = mouseState->touchIDs[i];
            xs[i] = mouseState->touchXs[i];
            ys[i] = mouseState->touchYs[i];
            states[i] = com_sun_glass_events_TouchEvent_TOUCH_RELEASED;
            mouseState->touchIsDragging[i] = JNI_FALSE;
            //as all points are released there is no need to save them for next
            //event processing
            needToSavePendingPoints = JNI_FALSE;
            
        }
    }

    //notify touch event if needed
    if (count) {
        //if all points are STILL we can ignore this event as nothing happens
        jboolean needToNotify = JNI_FALSE;
        for (i = 0; i < count; i++) {
            if (states[i] != com_sun_glass_events_TouchEvent_TOUCH_STILL ) {
                needToNotify = JNI_TRUE;
                break;
            }
        }
        if (needToNotify) {


            int primaryPointIndex = -1;
            jboolean primaryPointReassigned = JNI_FALSE;

            //Find the primary point in this touch event. Mouse events will be
            //synthesized from it
            if (mouseState->touchPrimaryPointID == -1) {
                //no previous primary point
                for (i = 0; i < count; i++) {
                    if (states[i] == com_sun_glass_events_TouchEvent_TOUCH_PRESSED) {
                        mouseState->touchPrimaryPointID = (int)ids[i];
                        primaryPointIndex = i;
                        GLASS_LOG_FINEST("no previous primary touch point -"
                                         " assigning point (index %d, id %d) as primary point",
                                         i,
                                         (int)ids[i]);
                        break;
                    }
                }
            } else if (mouseState->touchPrimaryPointID > 0) { //Glass id starts from 1
                //we have a previous primary point, try to find it
                for (i = 0; i < count; i++) {
                    if (ids[i] == mouseState->touchPrimaryPointID &&
                        states[i] != com_sun_glass_events_TouchEvent_TOUCH_RELEASED) {
                        primaryPointIndex = i;
                        GLASS_LOG_FINEST("primary point (id %d), found at index %d",
                                     (int)ids[i],
                                     i);
                        break;
                    }
                }

                if (primaryPointIndex == -1) {
                    //previous primary point doesn't exist or released, find a new one
                    for (i = 0; i < count; i++) {
                        if (states[i] != com_sun_glass_events_TouchEvent_TOUCH_RELEASED) {
                            mouseState->touchPrimaryPointID = (int)ids[i];
                            primaryPointIndex = i;
                            GLASS_LOG_FINEST("previous primary point doesn't exist"
                                         " reassign to point[%d], id = %d ",
                                         i,
                                         (int)ids[i]);
                            primaryPointReassigned = JNI_TRUE;
                            break;
                        }
                    }
                }

            } else {
                GLASS_LOG_SEVERE("Illegal indexed touch point state");
            }


            if (primaryPointIndex == -1) {
                 GLASS_LOG_FINEST("primary point not found - release");
                 mouseState->touchPrimaryPointID = -1; //mark as not set
                 //as all points are released there is no need to save them for next
                 //event processing
                 needToSavePendingPoints = JNI_FALSE;

            }

            //check if we can use multi touch events and simulate single touch 
            //screen event, if not.
            //follow primaryPointIndex for notifications
            if (!gUseMultiTouch && 
                device->isTouch && device->touchProtocolType != TOUCH_PROTOCOL_ST ) {                
                if (primaryPointIndex > -1) {
                    GLASS_LOG_FINEST("[multi->single] Using primary point with index"
                                     " %d for notification",
                                     primaryPointIndex);
                    //use index point

                    ids[0] = 1; //always use same point id
                    count = 1;
                    if (primaryPointReassigned && 
                        states[primaryPointIndex] == com_sun_glass_events_TouchEvent_TOUCH_PRESSED) {
                        //avoid double press
                        states[0] = com_sun_glass_events_TouchEvent_TOUCH_MOVED;
                    } else {
                        states[0] = states[primaryPointIndex];
                    }
                    xs[0] = xs[primaryPointIndex];
                    ys[0] = ys[primaryPointIndex];
                    
                    primaryPointIndex = 0;
                } else {
                    //all points were released, just drop the count to 1. The 
                    //coordinates from the first point will be used for the notification
                    GLASS_LOG_FINEST("[multi->single] All points released, using first "
                                     " point for notification");
                    ids[0] = 1;
                    count = 1;
                }
            }//!gUseMultiTouch

            //update the mouse position for future calculations
            if (primaryPointIndex > -1) {
                //update mouse location
                mousePosX = mouseState->pendingTouchXs[primaryPointIndex];
                mousePosY = mouseState->pendingTouchYs[primaryPointIndex];
            }

            GLASS_IF_LOG_FINEST {
                GLASS_LOG_FINEST("lens_wm_notifyMultiTouchEvent() with:");
                for (i = 0; i < count; i++) {
                    const char *isPrimary = primaryPointIndex == i?
                                            "[Primary]":
                                            "";
                    GLASS_LOG_FINEST("point %d / %d id=%d state=%d, x=%d y=%d %s",
                                     i+1,
                                     count,
                                     (int)ids[i],
                                     states[i],
                                     xs[i], ys[i],
                                     isPrimary);
                }
                GLASS_LOG_FINEST(""); //make it easier to read the log
            }
            lens_wm_notifyMultiTouchEvent(gJNIEnv, count, states, ids, xs, ys, primaryPointIndex);
        } else {
            GLASS_LOG_FINEST("all points are STILL - skipping event");
        }
    } else {
        GLASS_LOG_FINEST("no touch points");
    }

    if (!device->isTouch) {
        //handle mouse events

        //update mouse location
        mousePosX = newMousePosX;
        mousePosY = newMousePosY;

        GLASS_LOG_FINEST("device %p x %d y %d reportMove %d keyEventIndex: %d\n",
                     device, mousePosX, mousePosY, reportMouseMove, keyEventIndex);

        if (keyEventIndex >= 0) {
             lens_input_pointerEvents_handleKeyEvent(device,
                                                     &mouseState->pendingInputEvents[keyEventIndex]);
        }

        if (reportMouseMove) {

            //report move
            lens_wm_notifyMotionEvent(gJNIEnv, mousePosX, mousePosY);

            if (mouseState->rel[REL_WHEEL] != 0) {
                //report wheel
                lens_wm_notifyScrollEvent(gJNIEnv, mousePosX, mousePosY,
                                          mouseState->rel[REL_WHEEL]);
            }

            for (i = 0; i < REL_MAX + 1; i++) {
                mouseState->rel[i] = 0;
            }
        }

    }
    mouseState->pendingInputEventCount = 0;

    if (needToSavePendingPoints) {
        // recording pending touch points as existing touch points
        mouseState->touchPointCount = count;
        GLASS_LOG_FINEST("[store points] saving %i touch points",
                         count);
        for (i = 0; i < count; i++) {
            mouseState->touchIDs[i] = ids[i];
            mouseState->touchXs[i] = xs[i];
            mouseState->touchYs[i] = ys[i];
            GLASS_LOG_FINEST("[store points] Touch point %i at %i, %i (id=%i)",
                             i,
                             mouseState->touchXs[i], mouseState->touchYs[i],
                             mouseState->touchIDs[i]);
        }
    } else {
        //all points are released, no need to save
        mouseState->touchPointCount = 0;
        GLASS_LOG_FINEST("[store points] no need to save, no points");
    }
}



/**
 * enqueue tap and button events to be handled when sync
 * notification arrives
 *
 * @param mouseState holds the queue
 * @param event the event produced
 */
static void lens_input_pointerEvents_enqueuePendingEvent(LensInputMouseState *mouseState,
        struct input_event *event) {
    //create queue if required
    if (mouseState->pendingInputEventCapacity == 0) {
        mouseState->pendingInputEvents = calloc(1, sizeof(struct input_event));
        if (mouseState->pendingInputEvents == NULL) {
            GLASS_LOG_SEVERE("Out of memory: skipping an input event");
            return;
        }
        GLASS_LOG_FINE("Allocated pendingInputEvents %p",
                       mouseState->pendingInputEvents);
        mouseState->pendingInputEventCapacity = 1;
    } else if (mouseState->pendingInputEventCount == mouseState->pendingInputEventCapacity) {
        //resize queue if full, by factor of 2
        struct input_event *newArray = calloc(
                                           mouseState->pendingInputEventCapacity * 2,
                                           sizeof(struct input_event));
        GLASS_LOG_FINE("Reallocated pendingInputEvents %p", newArray);
        if (newArray == NULL) {
            GLASS_LOG_SEVERE("Out of memory: skipping an input event");
            return;
        }
        memcpy(newArray, mouseState->pendingInputEvents,
               sizeof(struct input_event) * mouseState->pendingInputEventCapacity);
        GLASS_LOG_FINE("free(%p) (old pendingInputEvents)",
                       mouseState->pendingInputEvents);
        free(mouseState->pendingInputEvents);
        mouseState->pendingInputEvents = newArray;
        mouseState->pendingInputEventCapacity *= 2;
    }
    mouseState->pendingInputEvents[mouseState->pendingInputEventCount++] = *event;
}

///// mouse and touch events handling - END



//// Keyboard events handling
/**
 * Handle and notify for keyboard events
 *
 * @param device the device that produced the event
 * @param event the event produced
 */
static void lens_input_keyEvents_handleEvent(LensInputDevice *device,
                                             struct input_event *event) {

    int jfxKeyCode;
    int eventType;
    NativeWindow window;
    struct input_event keyEvent = *event;
    jboolean isRepeatEvent = JNI_FALSE;


    if (keyEvent.type == EV_KEY) {

        window = glass_window_getFocusedWindow();

        if (window == NULL) {
            GLASS_LOG_FINE("Skipping event, no focused window");
            return;
        }

        GLASS_LOG_FINE("Keyboard raw type=0x%02x code=%d value=%d\n",
                       keyEvent.type, keyEvent.code, keyEvent.value);



        //determine events
        if (keyEvent.value == LENSFB_KEY_PRESSED) {
            eventType = com_sun_glass_events_KeyEvent_PRESS;
        } else if (keyEvent.value == LENSFB_KEY_RELEASED) {
            eventType = com_sun_glass_events_KeyEvent_RELEASE;
        } else if (keyEvent.value == LENSFB_KEY_REPEAT) {
            eventType = com_sun_glass_events_KeyEvent_PRESS;
            isRepeatEvent = JNI_TRUE;
        } else {
            GLASS_LOG_FINE("Skipping event, unsupported event[%d]", keyEvent.value);
            return;
        }

        jfxKeyCode = glass_inputEvents_getJavaKeycodeFromPlatformKeyCode(keyEvent.code);

        GLASS_LOG_FINEST("Notifying key event on windows %d[%p] - "
                         "event type %d, key code %d, is repeat?%s",
                         window->id, window, eventType, jfxKeyCode,
                         (isRepeatEvent ? "yes" : "no"));
        glass_application_notifyKeyEvent(gJNIEnv, window, eventType, jfxKeyCode, isRepeatEvent);


    } else {
        GLASS_LOG_FINEST("Event type[%i] is not a key event, skipping ",
                         keyEvent.type);
    }



}
//// Keyboard events handling - END


//// udev monitor
/**
 * Start the hot plug monitoring and notifications using udev
 *
 * @param env
 *
 * @return jboolean JNI_TRUE on success
 */
static jboolean lens_input_udevMonitorStart(JNIEnv *env) {
    struct udev *udev;
    udev = udev_new();


    if (!udev) {
        GLASS_LOG_SEVERE("failed to create udev");
        return JNI_FALSE;
    }
    udev_monitor = udev_monitor_new_from_netlink(udev, "udev");
    if (!udev_monitor) {
        GLASS_LOG_SEVERE("failed to create udev_monitor\n");
        udev_unref(udev);
        return JNI_FALSE;
    }

    //listen to device changes on /dev/input
    udev_monitor_filter_add_match_subsystem_devtype(udev_monitor, "input", NULL);

    if (udev_monitor_enable_receiving(udev_monitor)) {
        GLASS_LOG_SEVERE("failed to bind the udev monitor");
        udev_unref(udev);
        udev_monitor_unref(udev_monitor);
        return JNI_FALSE;
    }

    glass_application_request_native_event_loop(env, lens_input_udevMonitorLoop , NULL);

    return JNI_TRUE;
}

/**
 * Polling loop for udev notifications
 *
 * @param env
 * @param handle not used
 */
void lens_input_udevMonitorLoop(JNIEnv *env, void *handle) {

    int monitorFD = udev_monitor_get_fd(udev_monitor);
    struct udev *udev = udev_monitor_get_udev(udev_monitor);
    fd_set readFdSet;

    if (monitorFD == -1) {
        udev_monitor_unref(udev_monitor);
        udev_unref(udev);
        GLASS_LOG_SEVERE("Error in udev_monitor_get_fd(), hot plug disabled");
        return;
    }

    GLASS_LOG_FINE("Starting hot plug thread monitoring on fd[%i]\n", monitorFD);

    FD_ZERO(&readFdSet);
    while (1) {
        FD_SET(monitorFD, &readFdSet);

        select(monitorFD + 1, &readFdSet, NULL, NULL, NULL);

        //while handling udev monitor events, prevent input events
        //from been processed
        GLASS_LOG_FINER("Trying to capture lock before processing udev monitor events");
        pthread_mutex_lock(&devicesLock);
        GLASS_LOG_FINER("lock captured");

        if (FD_ISSET(monitorFD, &readFdSet)) {
            lens_input_udevMonitorHandleEvent(env);
        }

        //continue processing input events
        GLASS_LOG_FINER("Releasing lock");
        pthread_mutex_unlock(&devicesLock);
    }
}

/**
 * Handle add, update, and remove notifications from udev
 *
 */
static void lens_input_udevMonitorHandleEvent(JNIEnv *env) {

    struct udev_device *udev_device;
    const char *action;
    LensInputDevice *device = NULL;
    struct epoll_event epollEvent;

    udev_device = udev_monitor_receive_device(udev_monitor);
    if (!udev_device) {
        GLASS_LOG_WARNING("No device found");
        return;
    }

    action = udev_device_get_action(udev_device);

    GLASS_LOG_CONFIG("Got udev event - action = %s", action);
    if (action) {
        if (!strcmp(action, "add") || !strcmp(action, "change")) {

            lens_input_isUdevDeviceExists(udev_device, &device);
            //remove the device on change action
            if (!strcmp(action, "change") && device) {
                lens_input_deviceRemove(env, device);
                device = NULL;
            }

            if (!device) {
                //add the device
                device = lens_input_deviceAllocateAndInit(env, udev_device);
                if (device) {
                    lens_input_listAdd(device);
                    lens_input_epolladdDevice(device);
                    lens_input_printDevices();
                }
            }
        } else if (!strcmp(action, "remove")) {

            if (lens_input_isUdevDeviceExists(udev_device, &device)) {
                //Device was removed, so fd is closed and not valid.
                //mark it to avoid problem when releasing the device
                device->fd = -1;
                lens_input_deviceRemove(env, device);
                lens_input_printDevices();
            } else {
                GLASS_LOG_CONFIG("Device not in the list, skipping remove");
            }
        }
    } else {
        GLASS_LOG_CONFIG("Taking no action on udev event");
    }
    udev_device_unref(udev_device);
    GLASS_LOG_CONFIG("udev event action processing done");
}

//// memory management

/**
 * Add device to the attched devices list [lensInputDevicesList]
 *
 * @param device the device to add
 */
static void lens_input_listAdd(LensInputDevice *device) {

    if (device) {
        //add device to list
        if (!lensInputDevicesList_head) {
            lensInputDevicesList_head = device;
        }
        if (lensInputDevicesList_tail) {
            lensInputDevicesList_tail->nextDevice = device;
        }
        device->previousDevice = lensInputDevicesList_tail;
        lensInputDevicesList_tail = device;

        gNumOfAttachedDevices++;
    }

}


/**
 * Remove device from the attched devices list
 * [lensInputDevicesList]
 *
 * @param device the device to remove
 */
void lens_input_listRemove(LensInputDevice *device) {
    //detach from list
    if (device) {
        if (device->previousDevice) {
            device->previousDevice->nextDevice = device->nextDevice;
        } else {
            lensInputDevicesList_head = device->nextDevice;
        }

        if (device->nextDevice) {
            device->nextDevice->previousDevice = device->previousDevice;
        } else {
            lensInputDevicesList_tail = device->previousDevice;
        }
        gNumOfAttachedDevices--;
    }
}

/**
 * Safe release for LensInputDeviceInfo data structure
 *
 * @param device the device that own the info data
 */
static void lens_input_deviceInfoRelease(LensInputDevice *device) {
    GLASS_LOG_FINE("Release device %p (%s): %s", device,
                   (device->info ? device->info->devNode : NULL),
                   (device->info ? device->info->name : NULL));
    if (device && device->info) {
        if (device->info->devNode)   {
            free(device->info->devNode);
        }
        if (device->info->sysPath)   {
            free(device->info->sysPath);
        }
        if (device->info->name)      {
            free(device->info->name);
        }
        if (device->info->productString) {
            free(device->info->productString);
        }
        GLASS_LOG_FINE("free(%p) (device info)", device->info);
        free(device->info);
        device->info = NULL;
    }
}

/**
 * Safe release for LensMouseState data structure
 *
 * @param device the device that own the mouse state data
 */
void lens_input_mouseStateFree(LensInputDevice *device) {
    LensInputMouseState *mouseState = device->state;

    if (mouseState) {
        if (mouseState->pendingInputEvents) {
            GLASS_LOG_FINE("free(%p) (pendingInputEvents)",
                           mouseState->pendingInputEvents);
            free(mouseState->pendingInputEvents);
            mouseState->pendingInputEvents = NULL;
            mouseState->pendingInputEventCapacity = 0;
            mouseState->pendingInputEventCount = 0;
        }
        GLASS_LOG_FINE("free(%p) (device pointer state)", mouseState);
        free(mouseState);
    }

    device->state = NULL;
}

/**
 * Safe release of all LensInputDevice internal resources and
 * its pointer
 *
 * @param device the device to release
 */
static void lens_input_deviceRelease(JNIEnv *env, LensInputDevice *device) {
    if (device) {

        if (device->fd != -1) {
            lens_input_deviceGrab(device, 0);
            GLASS_LOG_FINER("close(%i)", device->fd);
            close(device->fd);
            device->fd = -1;
        }
        lens_input_deviceNotify(env, device, JNI_FALSE);

        GLASS_LOG_FINER("Freeing mouseState");
        lens_input_mouseStateFree(device);

        GLASS_LOG_FINER("Freeing deviceInfo");
        lens_input_deviceInfoRelease(device);

        if (device->readInputEvents != NULL) {
            free(device->readInputEvents);
        }

        GLASS_LOG_FINE("free(%p) (device)", device);
        free(device);
    }
}



//// utilities
/**
 * Parse the PRODUCT string from udev entry and convert into unsigned int
 *
 * @param udev_device [IN] the device to parse
 * @param vendorId [OUT] usb vendor id number
 * @param productId [OUT] usb product id number
 *
 * @return LensResult LES_OK on success
 */
static LensResult lens_input_udevParseProductID(struct udev_device *udev_device,
                                                unsigned int *vendorId,
                                                unsigned int *productId) {

    struct udev_device *parent = udev_device_get_parent(udev_device);

    LensResult result = LENS_FAILED;

    if (parent) {
        const char *product = udev_device_get_property_value(parent, "PRODUCT");
        int matchedStrings;
        unsigned int _productId, _vendorId;

        if (product) {
            //first try to parse as hex
            matchedStrings = sscanf(product, "%*x/%4x/%4x/%*x", &_vendorId, &_productId);

            if (matchedStrings == 2) {
                *vendorId = _vendorId;
                *productId = _productId;
                result =  LENS_OK;
            } else {
                GLASS_LOG_FINE("Failed to parse PRODUCT [%s]", product);
                *vendorId = 0;
                *productId = 0;
            }
        }
    }
    return result;
}

/**
 * Remove LensInputDevice from the list of deevices and free its
 * resources
 *
 * @param device the device to remove
 */
void lens_input_deviceRemove(JNIEnv *env, LensInputDevice *device) {

    if (device->isEnabled) {
        // On some platforms device will receive EPOLLHUP when disconnected.
        // When it happens the epoll handling will consider this as an error
        // unregister the device, and mark it as disabled.
        //if we are here that didn't happen, so we need to unregister the device
        GLASS_LOG_FINE("Unregistering device from epoll");
        lens_input_epollRemoveDevice(device);
    }

    GLASS_LOG_FINE("Removing device from device list\n");
    lens_input_listRemove(device);
    GLASS_LOG_FINE("Releasing device resources");
    lens_input_deviceRelease(env, device);
}

/**
 * Check if udev_device exists in the attached devices list, and
 * optionally return a reference for that device
 *
 * @param udev_device [IN] the device to check
 * @param device [OUT] reference to existing device, optional
 *
 * @return jboolean JNI_TRUE if device exists
 */
static jboolean lens_input_isUdevDeviceExists(struct udev_device *udev_device,
                                              LensInputDevice **device) {

    LensInputDevice *_device = lensInputDevicesList_head;
    const char *devNode = udev_device_get_devnode(udev_device);


    unsigned int vendorId, productId;

    if (!_device) {
        GLASS_LOG_FINER("Device doesn't exist - Device list empty\n");
    } else {
        if (lens_input_udevParseProductID(udev_device, &vendorId, &productId) == LENS_OK) {
            while (_device) {
                GLASS_LOG_FINER("Comparing udev[%s, %x, %x] with device[%s, %x, %x]",
                                devNode, vendorId, productId,
                                _device->info->devNode,
                                _device->info->vendorId,
                                _device->info->productId);
                if ((_device->info->vendorId  == vendorId &&
                        _device->info->productId == productId) ||
                        (devNode && !(strcmp(_device->info->devNode, devNode)))) {
                    GLASS_LOG_FINER("Device found");
                    if (device) {
                        GLASS_LOG_FINER("referencing device");
                        *device = _device;
                    }
                    return JNI_TRUE;
                }

                _device = _device->nextDevice;
            }
        }
    }
    //no device found....
    if (device) {
        *device = NULL;
    }
    GLASS_LOG_FINER("Device not found");
    return JNI_FALSE;
}

/**
 * Check if device is still on the lensInputDevicesList
 *
 * @param device the device to search
 *
 * @return jboolean JNI_TRUE if exists
 */
static jboolean lens_input_isDeviceExists(LensInputDevice *device) {
    LensInputDevice *_device = lensInputDevicesList_head;

    while (_device) {
        if (_device == device) {
            GLASS_LOG_FINER("Device %p exists", device);
            return JNI_TRUE;
        }
        _device = _device->nextDevice;
    }

    GLASS_LOG_FINER("Device %p was not found", device);
    return JNI_FALSE;

}

////// Printing functions

/**
 * Print the devices that currently monitored
 */
static void lens_input_printDevices() {
    LensInputDevice *device = lensInputDevicesList_head;

    GLASS_IF_LOG_CONFIG {

        GLASS_LOG_CONFIG("Input devices list:");

        if (!device) {
            GLASS_LOG_CONFIG("Device count = 0");
            return;
        }
        while (device) {
            GLASS_LOG_CONFIG("=========================");
            GLASS_LOG_CONFIG("Name: %s", device->info->name);
            GLASS_LOG_CONFIG("Path: %s", device->info->devNode);
            GLASS_LOG_CONFIG("sysPath %s", device->info->sysPath);
            GLASS_LOG_CONFIG("fd: %i", device->fd);
            GLASS_LOG_CONFIG("Product: %s", device->info->productString);
            GLASS_LOG_CONFIG("VendorId: %x", device->info->vendorId);
            GLASS_LOG_CONFIG("ProductId: %x", device->info->productId);

            if (device->isKeyboard) {
                GLASS_LOG_CONFIG("device is keyboard\n");
            }
            if (device->isPointer) {
                GLASS_LOG_CONFIG("device is pointer\n");
            }
            if (device->isTouch) {
                GLASS_LOG_CONFIG("device is touch\n");
            }
            GLASS_LOG_CONFIG("=========================\n");

            device = device->nextDevice;
        }

        GLASS_LOG_CONFIG("Device count = %i", gNumOfAttachedDevices);
    }
}

/**
 * Print input_event parameters in human readable form
 *
 * @param event
 */
static void lens_input_printEvent(struct input_event event) {
    char *tmp;
    
     GLASS_IF_LOG_FINEST {

        switch (event.type) {
            case EV_SYN:
                switch (event.code){
                    case SYN_REPORT:
                        tmp = "SYN_REPORT";
                        break;
                    case SYN_CONFIG:
                        tmp = "SYN_CONFIG";
                        break;
                    case SYN_MT_REPORT:
                        tmp = "SYN_MT_REPORT";
                        break;
                    case SYN_DROPPED:
                        tmp = "SYN_DROPPED";
                        break;
                    default:
                        tmp = NULL;
                        break;
                }
                if (tmp != NULL) {
                    GLASS_LOG_FINEST("EV_SYN %s %i", tmp, event.value);
                } else {
                    GLASS_LOG_FINEST("EV_SYN 0x%x %i", event.code, event.value);
                }
                break;
            case EV_KEY:
                switch (event.code){
                    case BTN_TOUCH:
                        tmp = "BTN_TOUCH";
                        break;
                    case BTN_TOOL_DOUBLETAP:
                        tmp = "BTN_TOOL_DOUBLETAP";
                        break;
                    case BTN_TOOL_TRIPLETAP:
                        tmp = "BTN_TOOL_TRIPLETAP";
                        break;
                    case BTN_TOOL_QUADTAP:
                        tmp = "BTN_TOOL_QUADTAP";
                        break;
                    default:
                        tmp = NULL;
                        break;
                }
                if (tmp != NULL) {
                    GLASS_LOG_FINEST("EV_KEY %s %i", tmp, event.value);
                } else {
                    GLASS_LOG_FINEST("EV_KEY 0x%x %i", event.code, event.value);
                }
                break;
            case EV_REL:
                switch (event.code) {
                    case REL_X:
                        tmp = "REL_X";
                        break;
                    case REL_Y:
                        tmp = "REL_Y";
                        break;
                    case REL_HWHEEL:
                        tmp = "REL_HWHEEL";
                        break;
                    case REL_DIAL:
                        tmp = "REL_DIAL";
                        break;
                    case REL_WHEEL:
                        tmp = "REL_WHEEL";
                        break;
                    case REL_MISC:
                        tmp = "REL_MISC";
                        break;
                    default:
                        tmp = NULL;
                        break;
                }
                if (tmp != NULL) {
                    GLASS_LOG_FINEST("EV_REL %s %i", tmp, event.value);
                } else {
                    GLASS_LOG_FINEST("EV_REL 0x%x %i", event.code, event.value);
                }
                break;
            case EV_ABS:
                switch (event.code) {
                    case ABS_X:
                        tmp = "ABS_X";
                        break;
                    case ABS_Y:
                        tmp = "ABS_Y";
                        break;
                    case ABS_Z:
                        tmp = "ABS_Z";
                        break;
                    case ABS_RX:
                        tmp = "ABS_RX";
                        break;
                    case ABS_RY:
                        tmp = "ABS_RY";
                        break;
                    case ABS_RZ:
                        tmp = "ABS_RZ";
                        break;
                    case ABS_THROTTLE:
                        tmp = "ABS_THROTTLE";
                        break;
                    case ABS_RUDDER:
                        tmp = "ABS_RUDDER";
                        break;
                    case ABS_WHEEL:
                        tmp = "ABS_WHEEL";
                        break;
                    case ABS_GAS:
                        tmp = "ABS_GAS";
                        break;
                    case ABS_BRAKE:
                        tmp = "ABS_BRAKE";
                        break;
                    case ABS_HAT0X:
                        tmp = "ABS_HAT0X";
                        break;
                    case ABS_HAT0Y:
                        tmp = "ABS_HAT0Y";
                        break;
                    case ABS_HAT1X:
                        tmp = "ABS_HAT1X";
                        break;
                    case ABS_HAT1Y:
                        tmp = "ABS_HAT1Y";
                        break;
                    case ABS_HAT2X:
                        tmp = "ABS_HAT2X";
                        break;
                    case ABS_HAT2Y:
                        tmp = "ABS_HAT2Y";
                        break;
                    case ABS_HAT3X:
                        tmp = "ABS_HAT3X";
                        break;
                    case ABS_HAT3Y:
                        tmp = "ABS_HAT3Y";
                        break;
                    case ABS_PRESSURE:
                        tmp = "ABS_PRESSURE";
                        break;
                    case ABS_DISTANCE:
                        tmp = "ABS_DISTANCE";
                        break;
                    case ABS_TILT_X:
                        tmp = "ABS_TILT_X";
                        break;
                    case ABS_TILT_Y:
                        tmp = "ABS_TILT_Y";
                        break;
                    case ABS_MISC:
                        tmp = "ABS_MISC";
                        break;
                    case ABS_MT_SLOT:
                        tmp = "ABS_MT_SLOT";
                        break;
                    case ABS_MT_TOUCH_MAJOR:
                        tmp = "ABS_MT_TOUCH_MAJOR";
                        break;
                    case ABS_MT_TOUCH_MINOR:
                        tmp = "ABS_MT_TOUCH_MINOR";
                        break;
                    case ABS_MT_WIDTH_MAJOR:
                        tmp = "ABS_MT_WIDTH_MAJOR";
                        break;
                    case ABS_MT_WIDTH_MINOR:
                        tmp = "ABS_MT_WIDTH_MINOR";
                        break;
                    case ABS_MT_ORIENTATION:
                        tmp = "ABS_MT_ORIENTATION";
                        break;
                    case ABS_MT_POSITION_X:
                        tmp = "ABS_MT_POSITION_X";
                        break;
                    case ABS_MT_POSITION_Y:
                        tmp = "ABS_MT_POSITION_Y";
                        break;
                    case ABS_MT_TOOL_TYPE:
                        tmp = "ABS_MT_TOOL_TYPE";
                        break;
                    case ABS_MT_BLOB_ID:
                        tmp = "ABS_MT_BLOB_ID";
                        break;
                    case ABS_MT_TRACKING_ID:
                        tmp = "ABS_MT_TRACKING_ID";
                        break;
                    case ABS_MT_PRESSURE:
                        tmp = "ABS_MT_PRESSURE";
                        break;
                    case ABS_MT_DISTANCE:
                        tmp = "ABS_MT_DISTANCE";
                        break;

                    default:
                        tmp = NULL;
                        break;
                }
                if (tmp != NULL) {
                    GLASS_LOG_FINEST("EV_ABS %s %i", tmp, event.value);
                } else {
                    GLASS_LOG_FINEST("EV_ABS 0x%x %i", event.code, event.value);
                }
                break;
            case EV_MSC:
                GLASS_LOG_FINEST("Misc");
                break;
            case EV_LED:
                GLASS_LOG_FINEST("Led");
                break;
            case EV_SND:
                GLASS_LOG_FINEST("Snd");
                break;
            case EV_REP:
                GLASS_LOG_FINEST("Rep");
                break;
            case EV_FF:
                GLASS_LOG_FINEST("FF");
                break;
            default:
                GLASS_LOG_FINEST("Event type=0x%x code=%i value=%i", event.type, event.code, event.value);
                break;
        }

    }
}

/**
 * Utility function that display on the console device
 * properties and supported capabilities
 *
 * @param evtype_b supported events bitmask
 * @param keytype_b the key type (left, right etc. and physical
 *                  properties as well)
 * @param proptype_b input device type (direct, with buttons,
 *                   etc)
 */
static void lens_input_printDeviceProperties(
    u_int8_t *evtype_b, u_int8_t *keytype_b, u_int8_t *proptype_b) {
    GLASS_LOG_CONFIG("Supported device types:");
    int id;

    GLASS_IF_LOG_CONFIG {
        for (id = 0; id < EV_CNT; id++) {
            if (TEST_BIT(id, evtype_b)) {
                /* the bit is set in the event types list */
                switch (id) {
                    case EV_SYN :
                        GLASS_LOG_CONFIG("EV_SYN (0x%02x, Synch Events)", id);
                        break;
                    case EV_KEY :
                        GLASS_LOG_CONFIG("EV_KEY (0x%02x, Keys or Buttons)", id);
                        break;
                    case EV_REL :
                        GLASS_LOG_CONFIG("EV_REL (0x%02x, Relative Axes)", id);
                        break;
                    case EV_ABS :
                        GLASS_LOG_CONFIG("EV_ABS (0x%02x, Absolute Axes)", id);
                        break;
                    case EV_MSC :
                        GLASS_LOG_CONFIG("EV_MSC (0x%02x, Miscellaneous)", id);
                        break;
                    case EV_SW :
                        GLASS_LOG_CONFIG("EV_SW (0x%02x, SW)", id);
                        break;
                    case EV_LED :
                        GLASS_LOG_CONFIG("EV_LED (0x%02x, LEDs)", id);
                        break;
                    case EV_SND :
                        GLASS_LOG_CONFIG("EV_SND (0x%02x, Sounds)", id);
                        break;
                    case EV_REP :
                        GLASS_LOG_CONFIG("EV_REP (0x%02x, Repeat)", id);
                        break;
                    case EV_FF :
                    case EV_FF_STATUS:
                        GLASS_LOG_CONFIG("EV_FF/EV_FF_STATUS (0x%02x, Force Feedback)",
                        id);
                        break;
                    case EV_PWR:
                        GLASS_LOG_CONFIG("EV_PWR (0x%02x, Power Management)", id);
                        break;
                    default:
                        GLASS_LOG_CONFIG("(Unknown event: 0x%04hx)", id);
                }
            }
        }
        for (id = 0; id < KEY_CNT; id++) {
            if (TEST_BIT(id, keytype_b)) {
                switch (id) {
                    case BTN_LEFT:
                        GLASS_LOG_CONFIG("BTN_LEFT");
                        break;
                    case BTN_RIGHT:
                        GLASS_LOG_CONFIG("BTN_RIGHT");
                        break;
                    case BTN_MIDDLE:
                        GLASS_LOG_CONFIG("BTN_MIDDLE");
                        break;
                    case BTN_TOOL_PEN:
                        GLASS_LOG_CONFIG("BTN_TOOL_PEN / BTN_DIGI");
                        break;
                    case BTN_TOOL_RUBBER:
                        GLASS_LOG_CONFIG("BTN_TOOL_RUBBER");
                        break;
                    case BTN_TOOL_BRUSH:
                        GLASS_LOG_CONFIG("BTN_TOOL_BRUSH");
                        break;
                    case BTN_TOOL_PENCIL:
                        GLASS_LOG_CONFIG("BTN_TOOL_PENCIL");
                        break;
                    case BTN_TOOL_AIRBRUSH:
                        GLASS_LOG_CONFIG("BTN_TOOL_AIRBRUSH");
                        break;
                    case BTN_TOOL_FINGER:
                        GLASS_LOG_CONFIG("BTN_TOOL_FINGER");
                        break;
                    case BTN_TOOL_MOUSE:
                        GLASS_LOG_CONFIG("BTN_TOOL_MOUSE");
                        break;
                    case BTN_TOOL_LENS:
                        GLASS_LOG_CONFIG("BTN_TOOL_LENS");
                        break;
#ifdef BTN_TOOL_QUINTTAP
                    case BTN_TOOL_QUINTTAP:
                        GLASS_LOG_CONFIG("BTN_TOOL_QUINTTAP");
                        break;
#endif
                    case BTN_TOUCH:
                        GLASS_LOG_CONFIG("BTN_TOUCH");
                        break;
                    case BTN_STYLUS:
                        GLASS_LOG_CONFIG("BTN_STYLUS");
                        break;
                    case BTN_STYLUS2:
                        GLASS_LOG_CONFIG("BTN_STYLUS2");
                        break;
                    case BTN_TOOL_DOUBLETAP:
                        GLASS_LOG_CONFIG("BTN_TOOL_DOUBLETAP");
                        break;
                    case BTN_TOOL_TRIPLETAP:
                        GLASS_LOG_CONFIG("BTN_TOOL_TRIPLETAP");
                        break;
#ifdef BTN_TOOL_QUADTAP
                    case BTN_TOOL_QUADTAP:
                        GLASS_LOG_CONFIG("BTN_TOOL_QUADTAP");
                        break;
#endif
                    case KEY_ZOOM:
                        GLASS_LOG_CONFIG("KEY_ZOOM");
                        break;
                    default:
                        if (id > 0x100) {
                            GLASS_LOG_CONFIG("(Unknown key: 0x%04hx)", id);
                        } else {
                            GLASS_LOG_FINE("(Unknown key: 0x%04hx)", id);
                        }
                        break;
                }
            }
        }
#ifdef EVIOCGPROP
        for (id = 0; id < INPUT_PROP_CNT; id++) {
            if (TEST_BIT(id, proptype_b)) {
                switch (id) {
                    case INPUT_PROP_POINTER:
                        GLASS_LOG_CONFIG("INPUT_PROP_POINTER");
                        break;
                    case INPUT_PROP_DIRECT:
                        GLASS_LOG_CONFIG("INPUT_PROP_DIRECT");
                        break;
                    case INPUT_PROP_BUTTONPAD:
                        GLASS_LOG_CONFIG("INPUT_PROP_BUTTONPAD");
                        break;
                    case INPUT_PROP_SEMI_MT:
                        GLASS_LOG_CONFIG("INPUT_PROP_SEMI_MT");
                        break;
                    default:
                        GLASS_LOG_CONFIG("(Uknown input property: 0x%04hx)", id);
                        break;
                }
            }
        }
#endif

    }
}

////// Test input device functions

/* The test input device feature reads input device configuration data from a
 * monitor device defined by LENS_TESTINPUT. This allows regression testing of
 * different input peripherals without requiring the actual peripheral hardware
 * to be present. A test suite that defines LENS_TESTINPUT should also create
 * the input monitor device (using mkfifo) before starting JavaFX.
 *
 * Test input data is read from the monitor device with the following format:
 * action: jint: 1 for add, 2 for remove
 * for add:
 *   id: struct input_id
 *   name: zero-terminated string
 *   devNode: zero-terminated string
 *   product: zero-terminated string
 *   events: a list of event types as jints, terminated by -1
 *   keys: a list of key codes as jints, terminated by -1
 *   relativeAxes: a list of axis codes as jints, terminated by -1
 *   absAxes: a list of absolute axis codes as
 *     { jint axis; struct input_absinfo info }
 *     terminated by a single jint of -1
 *   ( key: zero-terminated string
 *     value: zero-terminated string ) *
 *   0: byte
 *
 * for remove:
 *   devNode: zero-terminated string
 *
 * All jints are in host order.
 *
 */

/** Polling loop for test input notifications
 *
 * @param env
 * @param handle the path of the test input device
 */
void lens_input_testInputMonitorLoop(JNIEnv *env, void *handle) {

    fd_set readFdSet;
    const char *testInputPath = (const char *) handle;

    assert(testInputPath);
    testInputFD = open(testInputPath, O_RDONLY | O_SYNC);
    GLASS_LOG_FINE("open(%s) returned %i", testInputPath, testInputFD);
    if (testInputFD < 0) {
        GLASS_LOG_SEVERE("Cannot open test input device %s (Error %i: %s)",
                         testInputPath, errno, strerror(errno));
        return;
    }

    GLASS_LOG_FINE("Starting test input monitoring on fd[%i]\n", testInputFD);

    FD_ZERO(&readFdSet);
    while (1) {
        FD_SET(testInputFD, &readFdSet);
        select(testInputFD + 1, &readFdSet, NULL, NULL, NULL);

        if (FD_ISSET(testInputFD, &readFdSet)) {
            if (lens_input_testInputHandleEvent(env)) {
                GLASS_LOG_SEVERE("Error processing test input stream: disconnecting %s",
                                 testInputPath);
                GLASS_LOG_FINE("close(%i)", testInputFD);
                close(testInputFD);
                testInputFD = -1;
                return;
            }
        }
    }
}


/**
 * Handle add and remove notifications from test input
 */
static LensResult lens_input_testInputHandleEvent(JNIEnv *env) {
    jint action;
    if (lens_input_testInputReadInt(&action)) {
        return LENS_FAILED;
    }
    if (action == 1) {
        LensInputDevice *device;
        LensInputDeviceCapabilities *caps;
        struct input_id id;
        int rc = 0;

        GLASS_LOG_FINE("Adding test device");
        device = calloc(1, sizeof(LensInputDevice));
        GLASS_LOG_FINE("Allocated device %p", device);
        if (device == NULL) {
            GLASS_LOG_SEVERE("Unable to allocate device structure");
            return LENS_FAILED;
        }
        device->info = calloc(1, sizeof(LensInputDeviceInfo));
        if (device->info == NULL) {
            GLASS_LOG_SEVERE("Unable to allocate device info");
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }
        GLASS_LOG_FINE("Allocated device info %p", device->info);
        caps = &device->info->caps;
        device->isTestDevice = JNI_TRUE;

        device->readOffset = 0;
        device->readInputEvents = calloc(EVENTS_PER_READ, sizeof(struct input_event));
        if (device->readInputEvents == NULL) {
            GLASS_LOG_SEVERE("Failed to allocate readInputEvents buffer");
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }

        GLASS_LOG_FINE("Reading device ID");
        if (lens_input_testInputRead(&id, sizeof(id))) {
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }
        device->info->vendorId = (unsigned int) id.vendor;
        device->info->productId = (unsigned int) id.product;
        rc |= lens_input_testInputReadString(&device->info->name);
        rc |= lens_input_testInputReadString(&device->info->devNode);
        rc |= lens_input_testInputReadString(&device->info->productString);
        if (rc) {
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }
        GLASS_LOG_FINEST("Reading event mask");
        rc |= lens_input_testInputReadBitSet(&caps->eventMask[0], EV_MAX);
        GLASS_LOG_FINEST("Reading key bitset");
        rc |= lens_input_testInputReadBitSet(&caps->keybits[0], KEY_MAX);
        GLASS_LOG_FINEST("Reading relative axis bitset");
        rc |= lens_input_testInputReadBitSet(&caps->relbits[0], REL_MAX);
        if (rc) {
            lens_input_deviceRelease(env, device);
            return LENS_FAILED;
        }
        GLASS_LOG_FINEST("Reading absolute axis data");
        do {
            jint i;
            if (lens_input_testInputReadInt(&i)) {
                lens_input_deviceRelease(env, device);
                return LENS_FAILED;
            }
            if (i < 0) {
                break;
            }
            if (i > ABS_MAX) {
                GLASS_LOG_SEVERE("Absolute axis index %i out of range", i);
                lens_input_deviceRelease(env, device);
                return LENS_FAILED;
            }
            SET_BIT(caps->absbits, i);
            lens_input_testInputRead(&caps->absinfo[i], sizeof(struct input_absinfo));
            GLASS_LOG_FINEST("Range on axis %i is %i..%i", i,
                             (int) caps->absinfo[i].minimum,
                             (int) caps->absinfo[i].maximum);
        } while (1);
        jboolean isValidDevice = JNI_FALSE;
        do {
            char *key, *value;
            rc |= lens_input_testInputReadString(&key);
            if (strlen(key) == 0) {
                free(key);
                break;
            }
            rc |= lens_input_testInputReadString(&value);
            if (rc) {
                lens_input_deviceRelease(env, device);
                return LENS_FAILED;
            }
            isValidDevice |= lens_input_deviceCheckProperties(device, key, value);
            free(key);
            free(value);
        } while (1);
        if (isValidDevice) {

            if (device->isTouch && IS_BITSET(device->info->caps.absbits,ABS_MT_POSITION_X)) {
                device->touchProtocolType = TOUCH_PROTOCOL_MT_A;
                GLASS_LOG_FINEST("Test device is multi touch");
            }

            if (lens_input_deviceOpen(env, device)) {
                lens_input_deviceRelease(env, device);
                /* The input device monitor stream is left in a consistent
                 * state, so we return LENS_OK even though there was a failure.
                 * A SEVERE error will be logged by lens_input_deviceOpen on
                 * the failure to open the device. */
                return LENS_OK;
            }
            pthread_mutex_lock(&devicesLock);
            lens_input_listAdd(device);
            lens_input_epolladdDevice(device);
            lens_input_printDevices();
            pthread_mutex_unlock(&devicesLock);
        } else {
            GLASS_LOG_CONFIG("Not a keyboard, mouse or touchscreen - skipping");
            lens_input_deviceRelease(env, device);
        }
    } else if (action == 2) {
        char *devNode;
        GLASS_LOG_FINE("Removing test device");
        if (lens_input_testInputReadString(&devNode)) {
            return LENS_FAILED;
        }
        pthread_mutex_lock(&devicesLock);
        LensInputDevice *device = NULL;
        LensInputDevice *deviceList = lensInputDevicesList_head;
        while (deviceList) {
            if (strcmp(deviceList->info->devNode, devNode) == 0) {
                device = deviceList;
                break;
            }
            deviceList = deviceList->nextDevice;
        }
        if (device) {
            GLASS_LOG_FINE("Removing device %s", devNode);
            lens_input_deviceRemove(env, device);
            lens_input_printDevices();
        } else {
            GLASS_LOG_CONFIG("Device %s not in the list, skipping remove", devNode);
        }
        pthread_mutex_unlock(&devicesLock);
    } else {
        GLASS_LOG_SEVERE("Unknown action %i in test input stream", action);
        return LENS_FAILED;
    }
    return LENS_OK;
}

/** Reads n bytes from the test input monitor device */
static LensResult lens_input_testInputRead(void *_p, size_t n) {
    char *p = (char *) _p;
    size_t bytesRead = 0;
    while (bytesRead < n) {
        int rc = read(testInputFD, p + bytesRead, n - bytesRead);
        if (rc < 0) {
            if (errno == EAGAIN) {
                usleep(1000);
            } else {
                return LENS_FAILED;
            }
        } else {
            bytesRead += rc;
        }
    }
    return LENS_OK;
}

/**
 * Reads a jint (in host order) from the test input monitor device
 */
static LensResult lens_input_testInputReadInt(jint *i) {
    return lens_input_testInputRead((char *) i, sizeof(jint));
}

/**
 * Reads a null-terminated string from the test input device
 */
static LensResult lens_input_testInputReadString(char **pS) {
    char buffer[1024];
    char *p = (char *) buffer;
    char c;
    do {
        if (lens_input_testInputRead(&c, 1)) {
            return LENS_FAILED;
        } else {
            *p++ = c;
        }
    } while (c);
    GLASS_LOG_FINEST("Read test input string '%s'", buffer);
    *pS = strdup(buffer);
    if (*pS == NULL) {
        return LENS_FAILED;
    } else {
        return LENS_OK;
    }
}

/**
 * Reads an unpacked bitset from the input device as a list of jints terminated
 * by -1
 */
static LensResult lens_input_testInputReadBitSet(unsigned long *bitset, int max) {
    jint i;
    do {
        if (lens_input_testInputReadInt(&i)) {
            return LENS_FAILED;
        }
        if (i > max) {
            GLASS_LOG_SEVERE("Bitset value %i out of range", i);
            return LENS_FAILED;
        }
        if (i >= 0) {
            SET_BIT(bitset, i);
        }
    } while (i >= 0);
    return LENS_OK;
}


