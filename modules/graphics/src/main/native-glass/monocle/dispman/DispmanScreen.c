/* Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "com_sun_glass_ui_monocle_dispman_DispmanScreen.h"
#include "Monocle.h"

#include <fcntl.h>

#ifdef USE_DISPMAN
//Broadcom specials
static int bcm_is_loaded=0;

#define WRAPPEDAPI
#include "wrapped_bcm.h"
#endif /* USE_DISPMAN */

void load_bcm_symbols() {
#ifdef USE_DISPMAN
    if (bcm_is_loaded)
        return;
    bcm_is_loaded = 1;

    void *lib = dlopen("libbcm_host.so", RTLD_LAZY|RTLD_GLOBAL);
    if (!lib) {
        return;
    }

    int error = 0;

    if (!(wr_bcm_host_init = dlsym(lib,"bcm_host_init"))) error++;
    if (!(wr_vc_dispmanx_display_close = dlsym(lib,"vc_dispmanx_display_close"))) error++;
    if (!(wr_vc_dispmanx_display_open = dlsym(lib,"vc_dispmanx_display_open"))) error++;
    if (!(wr_vc_dispmanx_display_get_info = dlsym(lib, "vc_dispmanx_display_get_info")))  error++; //
    if (!(wr_vc_dispmanx_element_add = dlsym(lib,"vc_dispmanx_element_add"))) error++;
    if (!(wr_vc_dispmanx_update_start = dlsym(lib,"vc_dispmanx_update_start"))) error++;
    if (!(wr_vc_dispmanx_update_submit_sync = dlsym(lib,"vc_dispmanx_update_submit_sync"))) error++;
    if (!(wr_vc_dispmanx_resource_write_data = dlsym(lib, "vc_dispmanx_resource_write_data"))) error++;
    if (!(wr_vc_dispmanx_resource_read_data = dlsym(lib, "vc_dispmanx_resource_read_data"))) error++;
    if (!(wr_vc_dispmanx_element_remove = dlsym(lib, "vc_dispmanx_element_remove"))) error++;
    if (!(wr_vc_dispmanx_element_change_attributes = dlsym(lib, "vc_dispmanx_element_change_attributes"))) error++;
    if (!(wr_vc_dispmanx_resource_create = dlsym(lib, "vc_dispmanx_resource_create"))) error++;
    if (!(wr_vc_dispmanx_resource_delete = dlsym(lib, "vc_dispmanx_resource_delete"))) error++;
    if (!(wr_vc_dispmanx_snapshot = dlsym(lib, "vc_dispmanx_snapshot"))) error++;
    if (!(wr_vc_dispmanx_element_change_source = dlsym(lib, "vc_dispmanx_element_change_source"))) error++;

    if (error) {
        // handle error conditions better ?
        fprintf(stderr, "failed to load all bcm_host symbols %d\n", error);
        return;
    }
    return;
#else
    return;
#endif /* USE_DISPMAN */
}

JNIEXPORT void JNICALL Java_com_sun_glass_ui_monocle_dispman_DispmanScreen_wrapNativeSymbols
    (JNIEnv *env, jobject obj) {

#ifdef USE_DISPMAN
    load_bcm_symbols();
#else
    return 0l;
#endif /* USE_DISPMAN */
}