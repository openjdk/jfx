/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include <wtf/java/JavaEnv.h>
#include <jni.h>

namespace WebCore {

jclass PG_GetFontClass(JNIEnv* env);
jclass PG_GetFontCustomPlatformDataClass(JNIEnv* env);
jclass PG_GetGraphicsImageDecoderClass(JNIEnv* env);
jclass PG_GetGraphicsContextClass(JNIEnv* env);
jclass PG_GetGraphicsManagerClass(JNIEnv* env);
jclass PG_GetImageClass(JNIEnv* env);
jclass PG_GetImageFrameClass(JNIEnv* env);
jclass PG_GetMediaPlayerClass(JNIEnv* env);
jclass PG_GetPathClass(JNIEnv* env);
jclass PG_GetPathIteratorClass(JNIEnv* env);
jclass PG_GetRectangleClass(JNIEnv* env);
jclass PG_GetRefClass(JNIEnv* env);
jclass PG_GetRenderQueueClass(JNIEnv* env);
jclass PG_GetTransformClass(JNIEnv* env);
jclass PG_GetWebPageClass(JNIEnv* env);
jclass PG_GetColorChooserClass(JNIEnv* env);
JLObject PL_GetGraphicsManager(JNIEnv* env);
jclass getTimerClass(JNIEnv* env);
jclass PG_GetRenderThemeClass(JNIEnv* env);
JLObject PG_GetRenderThemeObjectFromPage(JNIEnv* env, JLObject page);

} // namespace
