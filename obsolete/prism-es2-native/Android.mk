#
#  Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
#  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
# 
#  This code is free software; you can redistribute it and/or modify it
#  under the terms of the GNU General Public License version 2 only, as
#  published by the Free Software Foundation.  Oracle designates this
#  particular file as subject to the "Classpath" exception as provided
#  by Oracle in the LICENSE file that accompanied this code.
# 
#  This code is distributed in the hope that it will be useful, but WITHOUT
#  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
#  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
#  version 2 for more details (a copy is included in the LICENSE file that
#  accompanied this code).
# 
#  You should have received a copy of the GNU General Public License version
#  2 along with this work; if not, write to the Free Software Foundation,
#  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
# 
#  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
#  or visit www.oracle.com if you need additional information or have any
#  questions.
#

LOCAL_PATH := $(call my-dir)/src
include $(CLEAR_VARS)

PREFIX2 := $(LOCAL_PATH)/

ANDROID_PATH := $(LOCAL_PATH)/eglfb
PREFIX1 := $(ANDROID_PATH)/

COMMON_SRC_FILES := $(wildcard $(LOCAL_PATH)/*.c)
ANDROID_SRC_FILES := $(wildcard $(ANDROID_PATH)/*.c)

LOCAL_SRC_FILES := $(COMMON_SRC_FILES:$(PREFIX2)%=%)
LOCAL_SRC_FILES += $(ANDROID_SRC_FILES:$(PREFIX1)%=eglfb/%)

LOCAL_C_INCLUDES := $(LOCAL_PATH)/../build $(LOCAL_PATH)/../build/android $(LOCAL_PATH)/eglfb $(LOCAL_PATH)/GL

LOCAL_LDLIBS += -llog -ldl -lGLESv2 -lEGL
LOCAL_CFLAGS += -DANDROID_NDK -DDEBUG -DIS_EGLFB

LOCAL_MODULE := prism-es2-eglfb

include $(BUILD_SHARED_LIBRARY)
