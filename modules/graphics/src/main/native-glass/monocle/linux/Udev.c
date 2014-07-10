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

#include "com_sun_glass_ui_monocle_linux_Udev.h"
#include "Monocle.h"

#include <errno.h>
#include <stdlib.h>
#include <string.h>
#include <linux/un.h>
#include <netinet/in.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <linux/netlink.h>

/** The udev event header structures are defined here so that we can extract
 * elements from them. There are multiple versions of the header; we determine
 * at run-time which is being used. */
typedef struct {
    char prefix[8];
    unsigned int magic;
    unsigned int _;
    unsigned int propertiesOffset;
    unsigned int propertiesLength;
} udev_event_header_A;

typedef struct {
	char prefix[16];
	unsigned int magic;
	unsigned short propertiesOffset;
	unsigned short propertiesLength;
} udev_event_header_B;

#define UDEV_MONITOR_MAGIC_A htonl(0xfeedcafe)
#define UDEV_MONITOR_MAGIC_B htonl(0xcafe1dea)

typedef enum {
    EVENT_FORMAT_UNKNOWN, EVENT_FORMAT_A, EVENT_FORMAT_B, EVENT_FORMAT_INVALID
} EventFormat;

static EventFormat getEventFormat(void *event) {
    static EventFormat eventFormat = EVENT_FORMAT_UNKNOWN;
    if (eventFormat == EVENT_FORMAT_UNKNOWN) {
        udev_event_header_A *a = (udev_event_header_A *) event;
        if (a->magic == UDEV_MONITOR_MAGIC_A) {
            eventFormat = EVENT_FORMAT_A;
            return eventFormat;
        }
        udev_event_header_B *b = (udev_event_header_B *) event;
        if (b->magic == UDEV_MONITOR_MAGIC_B) {
            eventFormat = EVENT_FORMAT_B;
            return eventFormat;
        }
        eventFormat = EVENT_FORMAT_INVALID;
        uint32_t *u = (uint32_t *) event;
        fprintf(stderr, "Cannot identify udev event format:\n");
        fprintf(stderr, "00 %08x %08x %08x %08x\n", u[0], u[1], u[2], u[3]);
        fprintf(stderr, "10 %08x %08x %08x %08x\n", u[4], u[5], u[6], u[7]);
        fprintf(stderr, "20 %08x %08x %08x %08x\n", u[8], u[9], u[10], u[11]);
    }
    return eventFormat;
}

static void monocle_IOException(JNIEnv *env, const char *msg) {
    char msgBuffer[1024];
    snprintf(msgBuffer, sizeof(msgBuffer),
            "%s (errno=%i, %s)", msg, errno, strerror(errno));
    jclass cls = (*env)->FindClass(env, "java/io/IOException");
    if (cls) {
        (*env)->ThrowNew(env, cls, msgBuffer);
    } else {
        fprintf(stderr, "IOException: %s", msgBuffer);
        exit(1);
    }
}

static void monocle_close(JNIEnv UNUSED(*env), int fd) {
    if (fd > 0) {
        close(fd);
    }
}

 JNIEXPORT jlong JNICALL
 Java_com_sun_glass_ui_monocle_linux_Udev__1open
 (JNIEnv *env, jobject UNUSED(jUdev)) {
    int fd;
    struct sockaddr_nl addrS;
    int bufferSize = 16384;
    memset(&addrS, 0, sizeof(addrS));
    addrS.nl_family = AF_NETLINK;
    addrS.nl_pid = getpid();
    addrS.nl_groups = 2;
    fd = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (fd == -1) {
        monocle_IOException(env, "Cannot create netlink socket");
        return 0;
    }
    setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &bufferSize, sizeof(bufferSize));
    if (bind(fd, (struct sockaddr *) &addrS, sizeof(addrS))) {
        monocle_close(env, fd);
        monocle_IOException(env, "Cannot bind netlink socket");
        return 0l;
     }

     return (jlong) fd;
 }

JNIEXPORT jint JNICALL
Java_com_sun_glass_ui_monocle_linux_Udev__1readEvent
(JNIEnv *env, jobject UNUSED(jUdev), jlong fdL, jobject bufferObj) {
    int fd = (int) fdL;
    char *buffer = (char *) (*env)->GetDirectBufferAddress(env, bufferObj);
    size_t bufferCapacity = (*env)->GetDirectBufferCapacity(env, bufferObj);
    if (fd <= 0) {
        monocle_IOException(env, "Invalid socket descriptor");
    }
    if (buffer == NULL) {
        monocle_IOException(env, "Invalid buffer");
    }
    jint length = (jint) recv(fd, buffer, bufferCapacity, 0);
    if (length <= 0) {
        monocle_IOException(env, "Error receiving event");
        return 0;
    }
    return length;
}

JNIEXPORT void JNICALL
Java_com_sun_glass_ui_monocle_linux_Udev__1close
(JNIEnv *env, jobject UNUSED(jUdev), jlong fdL) {
    monocle_close(env, (int) fdL);
}

JNIEXPORT jint JNICALL
Java_com_sun_glass_ui_monocle_linux_Udev__1getPropertiesOffset
(JNIEnv *env, jobject UNUSED(jUdev), jobject bufferObj) {
    void *buffer = (*env)->GetDirectBufferAddress(env, bufferObj);
    switch (getEventFormat(buffer)) {
        case EVENT_FORMAT_A:
            return (jint) ((udev_event_header_A *) buffer)->propertiesOffset;
            break;
        case EVENT_FORMAT_B:
            return (jint) ((udev_event_header_B *) buffer)->propertiesOffset;
            break;
        default:
            return -1;
    }
}

JNIEXPORT jint JNICALL
Java_com_sun_glass_ui_monocle_linux_Udev__1getPropertiesLength
(JNIEnv *env, jobject UNUSED(jUdev), jobject bufferObj) {
    void *buffer = (*env)->GetDirectBufferAddress(env, bufferObj);
    switch (getEventFormat(buffer)) {
        case EVENT_FORMAT_A:
            return (jint) ((udev_event_header_A *) buffer)->propertiesLength;
            break;
        case EVENT_FORMAT_B:
            return (jint) ((udev_event_header_B *) buffer)->propertiesLength;
            break;
        default:
            return -1;
    }
}

