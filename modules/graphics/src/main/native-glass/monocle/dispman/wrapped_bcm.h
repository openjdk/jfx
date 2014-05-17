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
#ifndef __WRAPPED_BCM__
#define __WRAPPED_BCM__

#ifndef WRAPPEDAPI
#define WRAPPEDAPI extern
#endif

#ifdef USE_DISPMAN

#ifndef BCM_HOST_H
#include <bcm_host.h>
#endif

#if !defined(_VC_DISPMANX_H_)
/* for Debian 6.0 libraries */
typedef enum {
   VC_IMAGE_ARGB8888 = 43,  /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
} VC_IMAGE_TYPE_T;
#endif

#define vc_dispmanx_display_close(display) (*wr_vc_dispmanx_display_close)(display)

#define vc_dispmanx_display_open(device) (*wr_vc_dispmanx_display_open)(device)

#define vc_dispmanx_element_add(update, display, layer, dest_rect, src, src_rect, protection, alpha, clamp, transform) (*wr_vc_dispmanx_element_add)(update, display, layer, dest_rect, src, src_rect, protection, alpha, clamp, transform)

#define vc_dispmanx_update_start(priority) (*wr_vc_dispmanx_update_start)(priority)

#define vc_dispmanx_update_submit_sync(update) (*wr_vc_dispmanx_update_submit_sync)(update)

#define vc_dispmanx_resource_read_data(handle, p_rect, dst_address, dst_pitch) (*wr_vc_dispmanx_resource_read_data) (handle, p_rect, dst_address, dst_pitch)

#define vc_dispmanx_resource_write_data(res, src_type, src_pitch, src_address, rect) (*wr_vc_dispmanx_resource_write_data)(res, src_type,src_pitch, src_address, rect)

#define vc_dispmanx_element_remove(update, element) (*wr_vc_dispmanx_element_remove) (update, element)

#define vc_dispmanx_element_change_attributes(update, element, change_flags, layer, opacity, dest_rect, src_rect, mask, transform) (*wr_vc_dispmanx_element_change_attributes) (update, element, change_flags, layer, opacity, dest_rect, src_rect, mask, transform)

#define vc_dispmanx_resource_create(type, width, height, native_image_handle) (*wr_vc_dispmanx_resource_create) (type, width, height, native_image_handle)

#define vc_dispmanx_resource_delete(res) (*wr_vc_dispmanx_resource_delete)(res )

#define vc_dispmanx_snapshot(display, snapshot_resource, transform) (*wr_vc_dispmanx_snapshot) (display, snapshot_resource, transform )

#define vc_dispmanx_element_change_source(update, element, src) (*wr_vc_dispmanx_element_change_source) (update, element, src )

/* wrapped method declarations */

WRAPPEDAPI int (*wr_vc_dispmanx_display_close)(DISPMANX_DISPLAY_HANDLE_T display);

WRAPPEDAPI DISPMANX_DISPLAY_HANDLE_T (*wr_vc_dispmanx_display_open)
                                      (uint32_t device);

WRAPPEDAPI DISPMANX_ELEMENT_HANDLE_T (*wr_vc_dispmanx_element_add) 
                                      (DISPMANX_UPDATE_HANDLE_T update,
                                       DISPMANX_DISPLAY_HANDLE_T display,
                                       int32_t layer, const VC_RECT_T *dest_rect, 
                                       DISPMANX_RESOURCE_HANDLE_T src,
                                       const VC_RECT_T *src_rect, 
                                       DISPMANX_PROTECTION_T protection,
                                       VC_DISPMANX_ALPHA_T *alpha, 
                                       DISPMANX_CLAMP_T *clamp, 
                                       DISPMANX_TRANSFORM_T transform);

WRAPPEDAPI DISPMANX_UPDATE_HANDLE_T (*wr_vc_dispmanx_update_start)
                                     (int32_t priority);

WRAPPEDAPI int (*wr_vc_dispmanx_update_submit_sync)
                (DISPMANX_UPDATE_HANDLE_T update);

WRAPPEDAPI int (*wr_vc_dispmanx_resource_read_data)
                (DISPMANX_RESOURCE_HANDLE_T handle,
                 const VC_RECT_T *p_rect, void *dst_address, uint32_t dst_pitch);

WRAPPEDAPI int (*wr_vc_dispmanx_resource_write_data)
                (DISPMANX_RESOURCE_HANDLE_T res, VC_IMAGE_TYPE_T src_type,
                 int src_pitch, void *src_address, const VC_RECT_T *rect);

WRAPPEDAPI int (*wr_vc_dispmanx_element_remove)
                (DISPMANX_UPDATE_HANDLE_T update,
                 DISPMANX_ELEMENT_HANDLE_T element);

WRAPPEDAPI int (*wr_vc_dispmanx_element_change_attributes)
                (DISPMANX_UPDATE_HANDLE_T update,
                 DISPMANX_ELEMENT_HANDLE_T element, uint32_t change_flags,
                 int32_t layer, uint8_t opacity, const VC_RECT_T *dest_rect,
                 const VC_RECT_T *src_rect, DISPMANX_RESOURCE_HANDLE_T mask,
                 VC_IMAGE_TRANSFORM_T transform);

WRAPPEDAPI DISPMANX_RESOURCE_HANDLE_T (*wr_vc_dispmanx_resource_create)
                                       (VC_IMAGE_TYPE_T type, uint32_t width, 
                                        uint32_t height, 
                                        uint32_t *native_image_handle);

WRAPPEDAPI int (*wr_vc_dispmanx_resource_delete)
                (DISPMANX_RESOURCE_HANDLE_T res);

WRAPPEDAPI int (*wr_vc_dispmanx_snapshot) (DISPMANX_DISPLAY_HANDLE_T display, 
                DISPMANX_RESOURCE_HANDLE_T snapshot_resource,
                VC_IMAGE_TRANSFORM_T transform);

WRAPPEDAPI int (*wr_vc_dispmanx_element_change_source)
                (DISPMANX_UPDATE_HANDLE_T update, 
                 DISPMANX_ELEMENT_HANDLE_T element,
                 DISPMANX_RESOURCE_HANDLE_T src);

WRAPPEDAPI int (*wr_vc_dispmanx_display_get_info)(DISPMANX_DISPLAY_HANDLE_T display, DISPMANX_MODEINFO_T *pinfo);

WRAPPEDAPI void (*wr_bcm_host_init)(void);

WRAPPEDAPI void load_bcm_symbols();

#endif // __WRAPPED_BCM__
#endif // USE_DISPMAN
