/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.media.jfxmediaimpl.platform.ios;

import com.sun.media.jfxmedia.MediaError;
import com.sun.media.jfxmedia.locator.Locator;
import com.sun.media.jfxmediaimpl.MediaUtils;
import com.sun.media.jfxmediaimpl.NativeMedia;
import com.sun.media.jfxmediaimpl.platform.Platform;

final class IOSMedia extends NativeMedia {

   /**
    * Handle to the native media player.
    */
   private long refNativeMedia;

   IOSMedia(Locator locator) {
       super(locator);
       init();
   }

   @Override
   public Platform getPlatform() {
       return IOSPlatform.getPlatformInstance();
   }

   private void init() {
       final long[] nativeMediaHandle = new long[1];
       final Locator loc = getLocator();
       final MediaError err = MediaError.getFromCode(iosInitNativeMedia(loc,
               loc.getContentType(), loc.getContentLength(),
               nativeMediaHandle));
       if (err != MediaError.ERROR_NONE) {
           MediaUtils.nativeError(this, err);
       }
       this.refNativeMedia = nativeMediaHandle[0];
   }

   long getNativeMediaRef() {
       return refNativeMedia;
   }

   @Override
   public synchronized void dispose() {
       if (0 != refNativeMedia) {
           iosDispose(refNativeMedia);
           refNativeMedia = 0L;
       }
   }

   private native int iosInitNativeMedia(Locator locator,
                                              String contentType,
                                              long sizeHint,
                                              long[] nativeMediaHandle);

   private native void iosDispose(long refNativeMedia);
}
