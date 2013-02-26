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
#include <stdio.h>
#include <dlfcn.h>

#ifdef USE_DISPMAN
int useDispman = 1;

void load_bcm_symbols();

/*************************************** BROADCOM ******************************************/

#ifndef BCM_HOST_H
#include <bcm_host.h>
#endif

#if !defined(_VC_DISPMANX_H_)
/* for Debian 6.0 libraries */
typedef enum {
   VC_IMAGE_ARGB8888 = 43,  /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
} VC_IMAGE_TYPE_T;
#endif

void (*_bcm_host_init)(void);

static int VCHPOST_ (*_vc_dispmanx_display_close)(DISPMANX_DISPLAY_HANDLE_T display);
static int VCHPOST_ (*_vc_dispmanx_display_get_info)(DISPMANX_DISPLAY_HANDLE_T display, DISPMANX_MODEINFO_T *pinfo);
static DISPMANX_DISPLAY_HANDLE_T VCHPOST_ (*_vc_dispmanx_display_open) (uint32_t device);
static DISPMANX_ELEMENT_HANDLE_T VCHPOST_ (*_vc_dispmanx_element_add) (DISPMANX_UPDATE_HANDLE_T update,
        DISPMANX_DISPLAY_HANDLE_T display,
        int32_t layer, const VC_RECT_T *dest_rect, DISPMANX_RESOURCE_HANDLE_T src,
        const VC_RECT_T *src_rect, DISPMANX_PROTECTION_T protection,
        VC_DISPMANX_ALPHA_T *alpha, DISPMANX_CLAMP_T *clamp, DISPMANX_TRANSFORM_T transform);
static DISPMANX_UPDATE_HANDLE_T VCHPOST_ (*_vc_dispmanx_update_start)( int32_t priority);
static int VCHPOST_ (*_vc_dispmanx_update_submit_sync)(DISPMANX_UPDATE_HANDLE_T update);
static int VCHPOST_ (*_vc_dispmanx_resource_read_data)(DISPMANX_RESOURCE_HANDLE_T handle,
                                                      const VC_RECT_T *p_rect, void *dst_address, uint32_t dst_pitch);
static int VCHPOST_ (*_vc_dispmanx_resource_write_data)(DISPMANX_RESOURCE_HANDLE_T res, VC_IMAGE_TYPE_T src_type,
                     int src_pitch, void *src_address, const VC_RECT_T *rect);
static int VCHPOST_ (*_vc_dispmanx_element_remove)(DISPMANX_UPDATE_HANDLE_T update,
                                                   DISPMANX_ELEMENT_HANDLE_T element);
static int VCHPOST_ (*_vc_dispmanx_element_change_attributes)(DISPMANX_UPDATE_HANDLE_T update,
                                                             DISPMANX_ELEMENT_HANDLE_T element,
                                                             uint32_t change_flags,
                                                             int32_t layer, uint8_t opacity,
                                                             const VC_RECT_T *dest_rect,
                                                             const VC_RECT_T *src_rect,
                                                             DISPMANX_RESOURCE_HANDLE_T mask,
                                                             VC_IMAGE_TRANSFORM_T transform);
static DISPMANX_RESOURCE_HANDLE_T VCHPOST_ (*_vc_dispmanx_resource_create)(VC_IMAGE_TYPE_T type,
                                                                          uint32_t width, uint32_t height,
                                                                          uint32_t *native_image_handle);
static int VCHPOST_ (*_vc_dispmanx_resource_delete)( DISPMANX_RESOURCE_HANDLE_T res );
static int VCHPOST_ (*_vc_dispmanx_snapshot)(DISPMANX_DISPLAY_HANDLE_T display,
                                             DISPMANX_RESOURCE_HANDLE_T snapshot_resource,
                                             VC_IMAGE_TRANSFORM_T transform );

static int VCHPOST_ (*_vc_dispmanx_element_change_source)( DISPMANX_UPDATE_HANDLE_T update, DISPMANX_ELEMENT_HANDLE_T element,
                                                        DISPMANX_RESOURCE_HANDLE_T src );




void bcm_host_init(void) {
    if (!_bcm_host_init) {
        load_bcm_symbols();
    }
    return (*_bcm_host_init)();
}

int VCHPOST_ vc_dispmanx_display_close(DISPMANX_DISPLAY_HANDLE_T display) {
    return (*_vc_dispmanx_display_close)(display);
}

int VCHPOST_ vc_dispmanx_display_get_info(DISPMANX_DISPLAY_HANDLE_T display, DISPMANX_MODEINFO_T *pinfo) {
    return (*_vc_dispmanx_display_get_info)(display, pinfo);
}

DISPMANX_DISPLAY_HANDLE_T VCHPOST_ vc_dispmanx_display_open(uint32_t device) {
    return (*_vc_dispmanx_display_open)(device);
}

DISPMANX_ELEMENT_HANDLE_T VCHPOST_ vc_dispmanx_element_add (DISPMANX_UPDATE_HANDLE_T update,
                                                            DISPMANX_DISPLAY_HANDLE_T display,
                                                            int32_t layer, const VC_RECT_T *dest_rect,
                                                            DISPMANX_RESOURCE_HANDLE_T src,
                                                            const VC_RECT_T *src_rect,
                                                            DISPMANX_PROTECTION_T protection,
                                                            VC_DISPMANX_ALPHA_T *alpha,
                                                            DISPMANX_CLAMP_T *clamp,
                                                            DISPMANX_TRANSFORM_T transform) {

    return (*_vc_dispmanx_element_add) (update, display, layer, dest_rect, src, src_rect, 
        protection, alpha, clamp, transform);
}

DISPMANX_UPDATE_HANDLE_T VCHPOST_ vc_dispmanx_update_start( int32_t priority) {
    return (*_vc_dispmanx_update_start)(priority);
}

int VCHPOST_ vc_dispmanx_update_submit_sync(DISPMANX_UPDATE_HANDLE_T update) {
    return (*_vc_dispmanx_update_submit_sync)(update);
}

int VCHPOST_ vc_dispmanx_resource_write_data(DISPMANX_RESOURCE_HANDLE_T res, VC_IMAGE_TYPE_T src_type,
                         int src_pitch, void *src_address, const VC_RECT_T *rect ) {
    return (*_vc_dispmanx_resource_write_data)(res, src_type, src_pitch, src_address, rect);
}

int VCHPOST_ vc_dispmanx_resource_read_data(DISPMANX_RESOURCE_HANDLE_T handle,
                                            const VC_RECT_T *p_rect, void *dst_address, uint32_t dst_pitch) {
    return (*_vc_dispmanx_resource_read_data)(handle, p_rect, dst_address, dst_pitch);
}

int VCHPOST_ vc_dispmanx_element_remove(DISPMANX_UPDATE_HANDLE_T update,
                                                    DISPMANX_ELEMENT_HANDLE_T element) {
    return (*_vc_dispmanx_element_remove)(update, element);
}

int VCHPOST_ vc_dispmanx_element_change_attributes(DISPMANX_UPDATE_HANDLE_T update,
                                                   DISPMANX_ELEMENT_HANDLE_T element, uint32_t change_flags,
                                                   int32_t layer, uint8_t opacity,
                                                   const VC_RECT_T *dest_rect,
                                                   const VC_RECT_T *src_rect,
                                                   DISPMANX_RESOURCE_HANDLE_T mask,
                                                   VC_IMAGE_TRANSFORM_T transform) {
    return (*_vc_dispmanx_element_change_attributes)(update, element, change_flags, layer, opacity, dest_rect,
                                                     src_rect, mask, transform);
}

DISPMANX_RESOURCE_HANDLE_T VCHPOST_ vc_dispmanx_resource_create(VC_IMAGE_TYPE_T type,
                                                                uint32_t width, uint32_t height,
                                                                uint32_t *native_image_handle) {
    return (*_vc_dispmanx_resource_create)(type, width, height, native_image_handle);
}

int VCHPOST_ vc_dispmanx_resource_delete( DISPMANX_RESOURCE_HANDLE_T res ) {
    return (*_vc_dispmanx_resource_delete)(res);
}

int VCHPOST_ vc_dispmanx_snapshot(DISPMANX_DISPLAY_HANDLE_T display,
                                   DISPMANX_RESOURCE_HANDLE_T snapshot_resource,
                                   VC_IMAGE_TRANSFORM_T transform) {
    return (*_vc_dispmanx_snapshot)(display, snapshot_resource, transform);
}

int VCHPOST_ vc_dispmanx_element_change_source(DISPMANX_UPDATE_HANDLE_T update, 
                                               DISPMANX_ELEMENT_HANDLE_T element,
                                               DISPMANX_RESOURCE_HANDLE_T src )
{
   return  (*_vc_dispmanx_element_change_source)(update,element,src);
}


void load_bcm_symbols() {
    int error = 0;
    void *lib = dlopen("libbcm_host.so", RTLD_NOW); // there is a couple of choices?

    if (_bcm_host_init) {
        fprintf(stderr,"BCM symbols already loaded\n");
        // already loaded
        return;
    }

    if (!lib) {
        useDispman = 0;
        return;
    } else {
        useDispman = 1;
    }

    if (!(_bcm_host_init = dlsym(lib,"bcm_host_init"))) error++;
    if (!(_vc_dispmanx_display_close = dlsym(lib,"vc_dispmanx_display_close"))) error++;
    if (!(_vc_dispmanx_display_get_info = dlsym(lib,"vc_dispmanx_display_get_info"))) error++;
    if (!(_vc_dispmanx_display_open = dlsym(lib,"vc_dispmanx_display_open"))) error++;
    if (!(_vc_dispmanx_element_add = dlsym(lib,"vc_dispmanx_element_add"))) error++;
    if (!(_vc_dispmanx_update_start = dlsym(lib,"vc_dispmanx_update_start"))) error++;
    if (!(_vc_dispmanx_update_submit_sync = dlsym(lib,"vc_dispmanx_update_submit_sync"))) error++;
    if (!(_vc_dispmanx_resource_write_data = dlsym(lib, "vc_dispmanx_resource_write_data"))) error++;
    if (!(_vc_dispmanx_resource_read_data = dlsym(lib, "vc_dispmanx_resource_read_data"))) error++;
    if (!(_vc_dispmanx_element_remove = dlsym(lib, "vc_dispmanx_element_remove"))) error++;
    if (!(_vc_dispmanx_element_change_attributes = dlsym(lib, "vc_dispmanx_element_change_attributes"))) error++;
    if (!(_vc_dispmanx_resource_create = dlsym(lib, "vc_dispmanx_resource_create"))) error++;
    if (!(_vc_dispmanx_resource_delete = dlsym(lib, "vc_dispmanx_resource_delete"))) error++;
    if (!(_vc_dispmanx_snapshot = dlsym(lib, "vc_dispmanx_snapshot"))) error++;
    if (!(_vc_dispmanx_element_change_source = dlsym(lib, "vc_dispmanx_element_change_source"))) error++;

    

    if (error) {
        // handle error conditions better ?
        fprintf(stderr,"failed to load all bcm_host symbols %d\n",error);
    }
}
#else
int useDispman = 0;

void load_bcm_symbols() {
}
#endif /* USE_DISPMAN */
