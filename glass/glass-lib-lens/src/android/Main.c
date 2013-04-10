/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
#ifdef ANDROID_NDK

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <assert.h>
#include "LensCommon.h"
#include "Main.h"

/*
 * This is the activity context we got from NativeActivity.
 * Stored for later use in glass.
 */
DvkContext context;

ANativeWindow *getAndroidNativeWindow() {
   assert(context);
   if (context->app->activityState == APP_CMD_PAUSE ||
      context->app->activityState == APP_CMD_STOP) {
      return NULL;
   }
   return context->app->window;
}

DvkContext getDvkContext() {
   return context;
}

const char *getExternalDataPath() {
   return context->app->activity->externalDataPath;
}

void android_main(struct android_app *app) {

   app_dummy();

   context = (DvkContext)malloc(sizeof(struct _DvkContext));
   assert(context);
    memset(context, 0, sizeof(struct _DvkContext));

    //save reference to android activity
    context->app = app;

    dvkEventLoop(context); //block until application ends

    free(context);
}

#endif /* ANDROID_NDK */
