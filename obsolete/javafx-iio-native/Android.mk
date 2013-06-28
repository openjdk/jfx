# 
# Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

include $(CLEAR_VARS)
LOCAL_PATH := src
PREFIX := $(LOCAL_PATH)/

JPEG_SOURCES := $(wildcard $(LOCAL_PATH)/libjpeg7/*.c)
LOCAL_MODULE := javafx-iio

LOCAL_SRC_FILES := $(JPEG_SOURCES:$(PREFIX)%=%)
LOCAL_SRC_FILES += jpegloader.c

LOCAL_C_INCLUDES := $(LOCAL_PATH) \
                    $(LOCAL_PATH)/libjpeg7 \
                    $(LOCAL_PATH)/../build/android

$(info ===> LOCAL_SRC_FILES=$(LOCAL_SRC_FILES))
$(info ===> LOCAL_C_INCLUDES=$(LOCAL_C_INCLUDES)) 

LOCAL_CFLAGS += -std=c99
LOCAL_LDLIBS := -llog -ldl
include $(BUILD_SHARED_LIBRARY)
