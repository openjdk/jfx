/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

/*
 * SEED HEADER - SCAFFOLDING, NOT THE ABI.
 *
 * This file exists only so that the wkjstub generator and its CMake build can
 * be developed and verified before the real
 * Source/WebKitLegacy/java/api/webkit_java_api.h exists. It carries the CORE
 * half of the contract - the export macro, the ABI version, wkj_ref, the
 * exception slot and a WKJHost upcall table - shaped as
 * modules/javafx.web/FFM-ABI-CONTRACT.md sections 2.2, 3, 4 and 5 describe.
 * The host sub-structs below are placeholders chosen to cover the type space
 * the generator has to handle (wkj_ref, pointer, int, float, double returns
 * and parameters); they are NOT a specification of the real host table, which
 * the owner of webkit_java_api.h defines.
 *
 * No DOM function is declared here, and none may be. The DOM half of the ABI
 * is never guessed: it comes from buildtools/ffm-web/dom-abi.tsv, which
 * dom-cpp-to-ffm.pl derives from the DOM binding sources, and the stub bodies
 * for it are generated from that spec.
 *
 * The CMake build prefers the real header and falls back to this one only when
 * the real header is absent, printing a warning when it does. Delete this file
 * once the real header lands.
 */

#ifndef WEBKIT_JAVA_API_H
#define WEBKIT_JAVA_API_H

#include <stdint.h>

#if defined(_MSC_VER)
#  define WKJ_EXPORT __declspec(dllexport)
#else
#  define WKJ_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define WKJ_ABI_VERSION 1u

/* 0 is the null reference. */
typedef uint64_t wkj_ref;

/* Exception types; numbering follows JavaDOMUtils.h. */
#define WKJ_EXC_NONE       0
#define WKJ_EXC_DOM        1
#define WKJ_EXC_EVENT      2
#define WKJ_EXC_RANGE      3
#define WKJ_EXC_UNDEFINED  4
#define WKJ_EXC_TYPE       5

typedef struct WKJExceptionSlot {
    int32_t         type;
    int32_t         code;
    const uint16_t* message;
    int32_t         message_length;
} WKJExceptionSlot;

/* One slot per calling thread; never NULL. */
WKJ_EXPORT WKJExceptionSlot* wkj_exception_slot(void);

/* ---------------------------------------------------------------- host table */

typedef struct WKJHostCore {
    void    (*retain)(wkj_ref self);
    void    (*release)(wkj_ref self);
    void    (*log)(int32_t level, const uint16_t* message, int32_t message_length);
    int32_t (*is_alive)(wkj_ref self);
} WKJHostCore;

typedef struct WKJHostWebPage {
    void    (*fire_load_event)(wkj_ref self, int64_t frame, int32_t state,
                              const uint16_t* url, int32_t url_length,
                              const uint16_t* content_type, int32_t content_type_length,
                              double progress, int32_t error_code);
    void    (*repaint)(wkj_ref self, int32_t x, int32_t y, int32_t w, int32_t h);
    int32_t (*permit_navigate)(wkj_ref self, const uint16_t* url, int32_t url_length);
    double  (*zoom_factor)(wkj_ref self, int32_t text_only);
    wkj_ref (*create_window)(wkj_ref self, int32_t features);
} WKJHostWebPage;

typedef struct WKJHostNetwork {
    void  (*did_receive_data)(wkj_ref self, const uint8_t* data, int32_t data_length,
                              int64_t total);
    void  (*did_finish)(wkj_ref self);
    float (*progress)(wkj_ref self);
} WKJHostNetwork;

typedef struct WKJHost {
    WKJHostCore    core;
    WKJHostWebPage webpage;
    WKJHostNetwork network;
} WKJHost;

/* Returns 0 on success, negative on version or size mismatch. */
WKJ_EXPORT int32_t  wkj_init(const WKJHost* host, int32_t host_size, uint32_t abi_version);
WKJ_EXPORT uint32_t wkj_abi_version(void);

#ifdef __cplusplus
}
#endif

#endif /* WEBKIT_JAVA_API_H */
