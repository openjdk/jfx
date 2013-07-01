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
 

#ifndef __RFB_SKIN_
#define __RFB_SKIN_

#include <rfb/rfb.h>

#ifndef PORTRFBAPI
#define PORTRFBAPI extern
#endif

PORTRFBAPI rfbScreenInfoPtr(*_rfbGetScreen)(int *argc, char **argv,
                                            int width, int height, int bitsPerSample, int samplesPerPixel,
                                            int bytesPerPixel);
#define rfbGetScreen(argc,argv,w,h,bps,spp,bpp) (*_rfbGetScreen)(argc,argv,w,h,bps,spp,bpp)

PORTRFBAPI void (*_rfbInitServer)(rfbScreenInfoPtr rfbScreen);
#define rfbInitServer(scr) (*_rfbInitServer)(scr)

PORTRFBAPI void (*_rfbShutdownServer)(rfbScreenInfoPtr rfbScreen, rfbBool disconnectClients);
#define rfbShutdownServer(scr,dis) (*_rfbShutdownServer)(scr,dis)

PORTRFBAPI void (*_rfbNewFramebuffer)(rfbScreenInfoPtr rfbScreen, char *framebuffer,
                                      int width, int height, int bitsPerSample, int samplesPerPixel,
                                      int bytesPerPixel);
#define rfbNewFramebuffer(scr,fb,w,h,bps,spb,bpp) (*_rfbNewFramebuffer)(scr,fb,w,h,bps,spb,bpp)

PORTRFBAPI void (*_rfbRunEventLoop)(rfbScreenInfoPtr screenInfo, long usec, rfbBool runInBackground);
#define rfbRunEventLoop(scr,usec,runInBackground) (*_rfbRunEventLoop)(scr,usec,runInBackground)

PORTRFBAPI void (*_rfbMarkRectAsModified)(rfbScreenInfoPtr rfbScreen, int x1, int y1, int x2, int y2);
#define rfbMarkRectAsModified(scr,x1,y1,x2,y2) (*_rfbMarkRectAsModified)(scr,x1,y1,x2,y2)

PORTRFBAPI rfbBool(*_rfbProcessEvents)(rfbScreenInfoPtr screenInfo, long usec);
#define rfbProcessEvents(scr,usec) (*_rfbProcessEvents)(scr,usec)

PORTRFBAPI rfbBool(*_rfbIsActive)(rfbScreenInfoPtr screenInfo);
#define rfbIsActive(scr) (*_rfbIsActive)(scr)
#endif

