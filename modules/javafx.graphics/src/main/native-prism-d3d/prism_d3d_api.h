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
 * prism_d3d_api.h - the flat C ABI of the prism_d3d library. Windows only.
 *
 * This is the only surface com.sun.prism.d3d.D3DNative binds. The library knows nothing about the
 * JVM: every function takes and returns <stdint.h> scalars, opaque void* handles (the C++ owns the
 * memory behind them) or caller-provided buffers and structs, and nothing here ever calls back into
 * Java. No exception crosses this boundary. Failures are reported exactly as the JNI entry points
 * reported them: a raw HRESULT as int32_t (FAILED(hr) is hr < 0; S_FALSE == 1 is a success and
 * keeps its meaning, e.g. "render target unchanged" from d3d_context_set_render_target), a NULL
 * handle where the JNI returned 0L, or 1/0 where it returned a Java boolean.
 *
 * Threading: every function runs on the Prism render thread, except the d3d_pipeline_* family,
 * which runs on the thread that initialises D3DPipeline (D3DPipeline.creator). There are no locks.
 *
 * The Direct3D 9Ex COM objects stay behind this boundary; d3d9.dll is loaded by DllMain
 * (D3DPipeline.cc) and never imported. Handles are D3DContext*, D3DResource* (textures, render
 * targets, swap chains), D3DPixelShaderResource*, D3DMesh*, D3DPhongMaterial* and D3DMeshView*.
 *
 * Strings written into D3dDriverInfo are NUL-terminated ANSI copies of the D3DADAPTER_IDENTIFIER9
 * fields, truncated to the array size; an empty string stands for the Java null the JNI left
 * behind when it had nothing to write.
 *
 * Only d3d_context_draw_indexed_quads and d3d_texture_update may be bound with
 * Linker.Option.critical(true): they are short and never wait on the GPU. Every other function
 * may block, allocate GPU resources or show driver UI and must take off-heap memory only.
 */

#ifndef PRISM_D3D_API_H
#define PRISM_D3D_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define PRISM_D3D_EXPORT __declspec(dllexport)
#else
#  define PRISM_D3D_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Bumped whenever a symbol, a prototype, a struct layout or an enum value changes. Java binds
 * d3d_abi_version first and every other symbol eagerly.
 */
#define PRISM_D3D_ABI_VERSION 1

/*
 * Prism-side constants the C maps onto D3D9 values (formerly read from the generated
 * JNI header for D3DContext). D3DNativeTest asserts each equals the Java constant it mirrors.
 */

/* com.sun.prism.d3d.D3DContext.D3DCOMPMODE_* */
enum D3dCompMode {
    D3D_COMPMODE_CLEAR   = 0,
    D3D_COMPMODE_SRC     = 1,
    D3D_COMPMODE_SRCOVER = 2,
    D3D_COMPMODE_DSTOUT  = 3,
    D3D_COMPMODE_ADD     = 4
};

/* com.sun.prism.d3d.D3DContext.CULL_* */
enum D3dCullMode {
    D3D_CULL_BACK  = 110,
    D3D_CULL_FRONT = 111,
    D3D_CULL_NONE  = 112
};

/* == com.sun.prism.PixelFormat.ordinal(); the C names follow PFormat in TextureUploader.h */
enum D3dPixelFormat {
    D3D_PFORMAT_INT_ARGB_PRE  = 0,  /* PixelFormat.INT_ARGB_PRE */
    D3D_PFORMAT_BYTE_RGBA_PRE = 1,  /* PixelFormat.BYTE_BGRA_PRE */
    D3D_PFORMAT_BYTE_RGB      = 2,  /* PixelFormat.BYTE_RGB */
    D3D_PFORMAT_BYTE_GRAY     = 3,  /* PixelFormat.BYTE_GRAY */
    D3D_PFORMAT_BYTE_ALPHA    = 4,  /* PixelFormat.BYTE_ALPHA */
    D3D_PFORMAT_MULTI_YV_12   = 5,  /* PixelFormat.MULTI_YCbCr_420, Java level only */
    D3D_PFORMAT_BYTE_APPL_422 = 6,  /* PixelFormat.BYTE_APPLE_422, unused in D3D */
    D3D_PFORMAT_FLOAT_XYZW    = 7   /* PixelFormat.FLOAT_XYZW */
};

/* Bit flags for d3d_pipeline_init; Java composes them from the PrismSettings statics named here. */
enum D3dInitFlags {
    D3D_INIT_FORCE_GPU = 1,  /* PrismSettings.forceGPU */
    D3D_INIT_VSYNC     = 2,  /* PrismSettings.isVsyncEnabled */
    D3D_INIT_VERBOSE   = 4   /* PrismSettings.verbose */
};

/* Index element type for d3d_mesh_build_geometry. */
enum D3dIndexType {
    D3D_INDEX_16 = 16,  /* uint16_t indices (D3DFMT_INDEX16), formerly nBuildNativeGeometryShort */
    D3D_INDEX_32 = 32   /* uint32_t indices (D3DFMT_INDEX32), formerly nBuildNativeGeometryInt */
};

/*
 * Out-param of d3d_pipeline_get_driver_information; Java copies it into a
 * com.sun.prism.d3d.D3DDriverInformation. sizeof == 1364, no padding (see d3d_sizeof_driver_info).
 */
typedef struct D3dDriverInfo {
    char    device_description[512];  /* D3DADAPTER_IDENTIFIER9.Description (MAX_DEVICE_IDENTIFIER_STRING) */
    char    device_name[32];          /* D3DADAPTER_IDENTIFIER9.DeviceName */
    char    driver_name[512];         /* D3DADAPTER_IDENTIFIER9.Driver (MAX_DEVICE_IDENTIFIER_STRING) */
    char    warning_message[256];     /* CheckForBadHardware message (MAX_WARNING_MESSAGE_LEN); "" means Java null */
    int32_t vendor_id, device_id, subsys_id;
    int32_t product, version, sub_version, build_id;   /* HIWORD/LOWORD of DriverVersion.HighPart/LowPart */
    int32_t ps_version_major, ps_version_minor;        /* 0 when caps.PixelShaderVersion has no version bits */
    int32_t max_samples;
    int32_t os_major, os_minor, os_build;              /* GetVersionEx; 0 when the call fails */
} D3dDriverInfo;

/*
 * Out-param of d3d_context_get_frame_stats; Java copies it into a com.sun.prism.d3d.D3DFrameStats.
 * sizeof == 32.
 */
typedef struct D3dFrameStats {
    int32_t num_triangles_drawn, num_draw_calls, num_buffer_locks, num_texture_locks,
            num_texture_transfer_bytes, num_set_texture, num_set_pixel_shader, num_render_target_switch;
} D3dFrameStats;

/*
 * Out-param of d3d_texture_create and d3d_swapchain_create. sizeof == 24. Replaces the
 * nCreateTexture + nGetTextureWidth + nGetTextureHeight + nIsDefaultPool sequence.
 */
typedef struct D3dTextureInfo {
    void*   handle;           /* D3DResource*; NULL on failure - Java keys on this, not on the HRESULT */
    int32_t width, height;    /* D3DSURFACE_DESC after the pow2/square adjustment */
    int32_t is_default_pool;  /* what nIsDefaultPool returned */
    int32_t reserved;         /* always 0 */
} D3dTextureInfo;

/* ------------------------------------------------------------------------------------------------
 * Guards - Java binds these first
 * ---------------------------------------------------------------------------------------------- */

PRISM_D3D_EXPORT int32_t d3d_abi_version(void);           /* PRISM_D3D_ABI_VERSION */
PRISM_D3D_EXPORT int64_t d3d_sizeof_driver_info(void);    /* sizeof(D3dDriverInfo) */
PRISM_D3D_EXPORT int64_t d3d_sizeof_frame_stats(void);    /* sizeof(D3dFrameStats) */
PRISM_D3D_EXPORT int64_t d3d_sizeof_texture_info(void);   /* sizeof(D3dTextureInfo) */

/* ------------------------------------------------------------------------------------------------
 * Pipeline (D3DPipeline) - D3DPipeline.creator thread
 * ---------------------------------------------------------------------------------------------- */

/* 1 ok, 0 failed (then d3d_pipeline_get_error_message has the reason). Re-callable after dispose. */
PRISM_D3D_EXPORT int32_t d3d_pipeline_init(int32_t flags);                             /* D3dInitFlags */
/* Copies the pipeline's error/warning message into buf (NUL-terminated, truncated to cap - 1);
 * returns the bytes written excluding the NUL, 0 when there is no message (Java: null). */
PRISM_D3D_EXPORT int32_t d3d_pipeline_get_error_message(char* buf, int32_t cap);
PRISM_D3D_EXPORT void    d3d_pipeline_dispose(void);
/* HMONITOR from Screen.getNativeScreen(); D3DADAPTER_DEFAULT (0) when not found or no pipeline. */
PRISM_D3D_EXPORT int32_t d3d_pipeline_get_adapter_ordinal(int64_t hmonitor);
PRISM_D3D_EXPORT int32_t d3d_pipeline_get_adapter_count(void);                        /* 0 without pipeline */
/* 1 filled, 0 unavailable (out is zeroed first). Works before d3d_pipeline_init succeeded. */
PRISM_D3D_EXPORT int32_t d3d_pipeline_get_driver_information(int32_t adapter, D3dDriverInfo* out);
PRISM_D3D_EXPORT int32_t d3d_pipeline_get_max_sample_support(int32_t adapter);          /* 0 when none */

/* ------------------------------------------------------------------------------------------------
 * Context and resources (D3DResourceFactory) - HRESULT returns unless noted
 * ---------------------------------------------------------------------------------------------- */

/* D3DContext*, NULL on failure. Creates the device on first use and resets clip + transform. */
PRISM_D3D_EXPORT void*   d3d_context_get(int32_t adapter);
PRISM_D3D_EXPORT int32_t d3d_context_test_cooperative_level(void* ctx);   /* raw CheckDeviceState result */
PRISM_D3D_EXPORT int32_t d3d_context_reset_device(void* ctx);
PRISM_D3D_EXPORT int32_t d3d_context_get_max_texture_size(void* ctx);    /* -1 for NULL ctx */
/* HRESULT of the D3D create call; out->handle is NULL when it failed (Java keys on the handle
 * exactly as it keyed on 0L). format_hint/usage_hint are PixelFormat/Texture.Usage ordinals. */
PRISM_D3D_EXPORT int32_t d3d_texture_create(void* ctx, int32_t format_hint, int32_t usage_hint, int32_t is_rtt,
                                            int32_t width, int32_t height, int32_t samples, int32_t use_mipmap,
                                            D3dTextureInfo* out);
/* hwnd from PresentableState.getNativeView(); E_FAIL when it is not a window. */
PRISM_D3D_EXPORT int32_t d3d_swapchain_create(void* ctx, int64_t hwnd, int32_t vsync, D3dTextureInfo* out);
/* res is any handle created through this ABI except mesh/material/meshview (those have their own
 * release). D3D_OK for a NULL res, S_FALSE for a NULL ctx. */
PRISM_D3D_EXPORT int32_t d3d_resource_release(void* ctx, void* res);
/* 0 and writes *width/*height (either pointer may be NULL); -1 for a NULL res. */
PRISM_D3D_EXPORT int32_t d3d_resource_get_size(void* res, int32_t* width, int32_t* height);
PRISM_D3D_EXPORT int32_t d3d_resource_is_default_pool(void* res);        /* 1/0; 0 for NULL */
/* One entry point for nUpdateTexture{I,B,F}: format is the D3dPixelFormat of pixels (INT_ARGB_PRE
 * for int[], FLOAT_XYZW for float[], the PixelFormat ordinal for byte[]); pixels_bytes is the
 * byte size of pixels; srcscan is in bytes. critical(true) allowed. */
PRISM_D3D_EXPORT int32_t d3d_texture_update(void* ctx, void* res, const void* pixels, int64_t pixels_bytes,
                                            int32_t format, int32_t dstx, int32_t dsty, int32_t srcx, int32_t srcy,
                                            int32_t srcw, int32_t srch, int32_t srcscan);
/* One entry point for nReadPixels{I,B}: copies w*h A8R8G8B8/X8R8G8B8 pixels of res into dst
 * (dst_bytes >= w*h*4, else E_OUTOFMEMORY). Stalls on GetRenderTargetData - never critical. */
PRISM_D3D_EXPORT int32_t d3d_texture_read_pixels(void* ctx, void* res, void* dst, int64_t dst_bytes,
                                                 int32_t w, int32_t h);

/* ------------------------------------------------------------------------------------------------
 * Pixel shaders (D3DShader)
 * ---------------------------------------------------------------------------------------------- */

/* D3DPixelShaderResource*, NULL on failure. bytecode is the fxc token stream; it is parsed inside
 * CreatePixelShader and never retained. bytecode_bytes is not validated (the JNI had no length). */
PRISM_D3D_EXPORT void*   d3d_shader_create(void* ctx, const void* bytecode, int64_t bytecode_bytes);
PRISM_D3D_EXPORT int32_t d3d_shader_enable(void* ctx, void* shader);     /* E_FAIL for a NULL shader */
PRISM_D3D_EXPORT int32_t d3d_shader_disable(void* ctx);
/* values points at count float4 registers (4 * count floats), already offset by the caller. */
PRISM_D3D_EXPORT int32_t d3d_shader_set_constants_f(void* ctx, int32_t reg, const float* values, int32_t count);

/* ------------------------------------------------------------------------------------------------
 * 2D context state (D3DContext, D3DGraphics, D3DSwapChain)
 * ---------------------------------------------------------------------------------------------- */

PRISM_D3D_EXPORT int32_t d3d_swapchain_present(void* ctx, void* swapchain);     /* blocks on vsync */
/* 1 and fills out (resetting the counters when reset != 0); 0 when PERF_COUNTERS is compiled out. */
PRISM_D3D_EXPORT int32_t d3d_context_get_frame_stats(void* ctx, D3dFrameStats* out, int32_t reset);
/* coords: num_verts * 7 floats (x,y,z,tu1,tv1,tu2,tv2); colors: num_verts * 4 bytes (r,g,b,a).
 * critical(true) allowed. */
PRISM_D3D_EXPORT int32_t d3d_context_draw_indexed_quads(void* ctx, const float* coords, const uint8_t* colors,
                                                        int32_t num_verts);
PRISM_D3D_EXPORT int32_t d3d_context_clear(void* ctx, int32_t color_argb_pre, int32_t clear_depth,
                                           int32_t ignore_scissor);
PRISM_D3D_EXPORT int32_t d3d_context_set_blend_mode(void* ctx, int32_t comp_mode);          /* D3dCompMode */
/* S_FALSE (1) when the target was already current - Java calls resetLastClip only on D3D_OK. */
PRISM_D3D_EXPORT int32_t d3d_context_set_render_target(void* ctx, void* target_res, int32_t depth_buffer,
                                                       int32_t msaa);
/* wrap_mode: D3DContext.D3DTADDRESS_* (0 = leave the address mode alone). */
PRISM_D3D_EXPORT int32_t d3d_context_set_texture(void* ctx, void* tex_res_or_null, int32_t unit, int32_t linear,
                                                 int32_t wrap_mode);
PRISM_D3D_EXPORT int32_t d3d_context_set_camera_position(void* ctx, double x, double y, double z);
/* m16 holds the 16 doubles in the order the JNI took them: m00,m01,m02,m03,m10,...,m33, i.e.
 * GeneralTransform3D.get(0..15) (row-major). E_FAIL for a NULL m16. */
PRISM_D3D_EXPORT int32_t d3d_context_set_proj_view_matrix(void* ctx, int32_t depth_test, const double* m16);
PRISM_D3D_EXPORT int32_t d3d_context_set_transform(void* ctx, const double* m16);
PRISM_D3D_EXPORT int32_t d3d_context_reset_transform(void* ctx);
/* NULL m16 selects the identity (formerly nSetWorldTransformToIdentity). */
PRISM_D3D_EXPORT void    d3d_context_set_world_transform(void* ctx, const double* m16_or_null);
PRISM_D3D_EXPORT int32_t d3d_context_set_clip_rect(void* ctx, int32_t x1, int32_t y1, int32_t x2, int32_t y2);
PRISM_D3D_EXPORT int32_t d3d_context_reset_clip_rect(void* ctx);
PRISM_D3D_EXPORT int32_t d3d_context_set_device_parameters_2d(void* ctx);      /* S_FALSE for NULL ctx */
/* Creates the 51 phong shaders on first call. S_FALSE for NULL ctx. */
PRISM_D3D_EXPORT int32_t d3d_context_set_device_parameters_3d(void* ctx);
/* StretchRect src -> dst (or the current render target when dst_res_or_null is NULL). */
PRISM_D3D_EXPORT void    d3d_context_blit(void* ctx, void* src_res, void* dst_res_or_null,
                                          int32_t sx0, int32_t sy0, int32_t sx1, int32_t sy1,
                                          int32_t dx0, int32_t dy0, int32_t dx1, int32_t dy1);

/* ------------------------------------------------------------------------------------------------
 * 3D (D3DMesh, D3DPhongMaterial, D3DMeshView)
 * ---------------------------------------------------------------------------------------------- */

PRISM_D3D_EXPORT void*   d3d_mesh_create(void* ctx);                       /* D3DMesh*, NULL on failure */
PRISM_D3D_EXPORT void    d3d_mesh_release(void* mesh);
/* vb_len is the number of floats in vb, ib_len the number of indices in ib (element counts, as the
 * JNI vertexBufferLength/indexBufferLength were); index_type is a D3dIndexType. 1 ok, 0 failed.
 * Lock() without DISCARD may wait for the GPU - never critical; copy the arrays off-heap. */
PRISM_D3D_EXPORT int32_t d3d_mesh_build_geometry(void* mesh, const float* vb, int32_t vb_len,
                                                 const void* ib, int32_t ib_len, int32_t index_type);
PRISM_D3D_EXPORT void*   d3d_material_create(void* ctx);                   /* D3DPhongMaterial*, NULL on failure */
PRISM_D3D_EXPORT void    d3d_material_release(void* material);
PRISM_D3D_EXPORT void    d3d_material_set_diffuse_color(void* material, float r, float g, float b, float a);
PRISM_D3D_EXPORT void    d3d_material_set_specular_color(void* material, int32_t set, float r, float g, float b,
                                                         float a);
/* map_type is TextureMap.Type.ordinal() (0 diffuse, 1 specular, 2 bump, 3 self-illumination);
 * texture_res_or_null is the D3DResource handle of the texture - the C reads its IDirect3DTexture9
 * (this folds the former nGetNativeTextureObject). */
PRISM_D3D_EXPORT void    d3d_material_set_map(void* material, int32_t map_type, void* texture_res_or_null);
PRISM_D3D_EXPORT void*   d3d_meshview_create(void* ctx, void* mesh);       /* D3DMeshView*, NULL on failure */
PRISM_D3D_EXPORT void    d3d_meshview_release(void* meshview);
PRISM_D3D_EXPORT void    d3d_meshview_set_culling_mode(void* meshview, int32_t cull_mode);  /* D3dCullMode */
PRISM_D3D_EXPORT void    d3d_meshview_set_material(void* meshview, void* material);
PRISM_D3D_EXPORT void    d3d_meshview_set_wireframe(void* meshview, int32_t wireframe);
PRISM_D3D_EXPORT void    d3d_meshview_set_ambient_light(void* meshview, float r, float g, float b);
/* params18: x,y,z, r,g,b, w, ca,la,qa, isAttenuated, range, dirX,dirY,dirZ, innerAngle,outerAngle,
 * falloff - the 18 floats of the former nSetLight in order. index is 0..2. */
PRISM_D3D_EXPORT void    d3d_meshview_set_light(void* meshview, int32_t index, const float* params18);
PRISM_D3D_EXPORT void    d3d_meshview_render(void* meshview);

#ifdef __cplusplus
}
#endif

#endif /* PRISM_D3D_API_H */
