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
 
#ifndef __LENS_RFB_H__
#define __LENS_RFB_H__

#include "LensCommon.h"

/**
 * Init RFB and start listen to events
 *
 */
void lens_rfb_init(JNIEnv *env);

/**
 * Mark a regeion of the screen as dirty
 *
 * @param topLeft_X top left corner
 * @param topLeft_Y top left corner
 * @param buttomRight_X buttom right corner
 * @param buttomRight_Y buttom right corner
 */
void lens_rfb_notifyDirtyRegion(int topLeft_X, int topLeft_Y, int buttomRight_X, int buttomRight_Y);

#endif

