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
 * webkit_java_api_platform.h - the graphics, network and media third of the jfxwebkit C ABI.
 *
 * Scope, exactly (FFM-ABI-CONTRACT.md section 12.1):
 *
 *   Source/WebCore/platform/graphics/java    the WCRenderQueue command buffer, WCPath,
 *                                            WCFont / WCTextRun, WCImage, WCImageDecoder
 *   Source/WebCore/platform/network/java      URLLoader, SocketStreamHandle, CookieJar
 *
 * It carries two things:
 *
 *   1. the WKJHostGraphics / WKJHostNetwork / WKJHostMedia callback tables, which replace the
 *      90 cached jmethodID upcall sites of those two directories;
 *   2. the 22 wkj_ entry points that replace their 22 JNIEXPORT functions.
 *
 * The shared constants those two directories use are NOT here: they live in the generated
 * wkj_constants.h, which replaces all 23 com_sun_webkit_*.h headers and keeps their mangled
 * names, so each call site changed only its include.
 *
 * ------------------------------------------------------------------------------------------
 * INTEGRATION - one edit is still owed in webkit_java_api.h, by that header's owner
 * ------------------------------------------------------------------------------------------
 * webkit_java_api.h still carries placeholders for the three groups defined here:
 *
 *     typedef struct WKJHostGraphics { void (*reserved)(void); } WKJHostGraphics;
 *     typedef struct WKJHostNetwork  { void (*reserved)(void); } WKJHostNetwork;
 *     typedef struct WKJHostMedia    { void (*reserved)(void); } WKJHostMedia;
 *
 * Those three lines must be deleted and replaced by
 *
 *     #include "webkit_java_api_platform.h"
 *
 * at the same place - above the WKJHost definition, below WKJHostCore - and WKJ_ABI_VERSION
 * bumped. The structs below are the real definitions of those three types, not additional
 * types, so the two states cannot coexist in one translation unit.
 *
 * Note the direction of the include, which is the opposite of webkit_java_api_dom.h and
 * webkit_java_api_page.h: the master includes this header, and this header does not include
 * the master. It cannot. The master needs these three definitions in the MIDDLE of its own
 * body (WKJHost has them as members, so they must be complete types by then), and a mutual
 * include cannot deliver that - whichever header the translation unit names first sets its
 * own guard, so the second include is a no-op and one of the two bodies is processed with the
 * other's types still missing. One-way is the only arrangement that compiles, and it costs
 * nothing: everything this header needs - wkj_ref, WKJ_EXPORT, the WKJ_STR_* codes - is
 * declared above the include point.
 *
 * Verified with cl.exe 14.44.35207 at /W4 /WX, as C (/TC) and as C++ (/TP /EHsc), against a
 * copy of webkit_java_api.h carrying that edit.
 *
 * ------------------------------------------------------------------------------------------
 * CONVENTIONS - all inherited from webkit_java_api.h; only the additions are restated
 * ------------------------------------------------------------------------------------------
 * Strings         UTF-16, "const uint16_t* s, int32_t s_len" inbound (s == NULL means the
 *                 Java value was null, and collapses to the empty WTF::String exactly as
 *                 wtf/java/StringJava.cpp always did); caller-provided buffer outbound,
 *                 returning WKJ_STR_OK / WKJ_STR_NULL / WKJ_STR_OVERFLOW (contract 13).
 * Booleans        int32_t, 0 or 1. FFM has no boolean layout.
 * Java objects    wkj_ref (contract 3). A slot that RETURNS a wkj_ref returns a NEW id that
 *                 the library owns and must release exactly once - hold it in a WKJHandle.
 *                 A wkj_ref PARAMETER is borrowed for the duration of the call.
 * Native objects  int64_t peers, converted with wkj_to_ptr / wkj_from_ptr.
 * NULL slots      every callback pointer may be NULL. The library tests it before every call
 *                 and falls back to the default documented on the slot.
 * Upcall failure  a Java exception never propagates. Where the JNI code branched on
 *                 WTF::CheckAndClearException the library still calls
 *                 wkj_host->core.check_and_clear_exception() in the same place, and where the
 *                 JNI code only cleared, the library only clears. That swallowing is
 *                 deliberate and must not be "fixed" here.
 * Threading       everything in this header is reached from the WebKit main thread, with two
 *                 exceptions called out on the slots themselves: graphics.ref_deref (RQRef's
 *                 destructor, which the JNI code already guarded for a detached thread) and
 *                 the WCImageDecoder slots (BitmapImage decoding also runs on WorkQueue
 *                 threads). Every upcall stub therefore lives in one Arena.ofShared().
 *
 * JAVA-SIDE CONSTRAINT, stated here so the binding author cannot miss it:
 * wkj_socket_did_receive_data and wkj_url_loader_did_receive_data MUST NOT be bound with
 * Linker.Option.critical(true). Both re-enter arbitrary WebKit work - SharedBuffer creation,
 * ResourceHandleClient dispatch, script execution - and a critical downcall that re-enters
 * the JVM is undefined behaviour. The same prohibition applies to every wkj_media_notify_
 * entry point, each of which can run script through MediaPlayer's event dispatch.
 */

#ifndef WEBKIT_JAVA_API_PLATFORM_H
#define WEBKIT_JAVA_API_PLATFORM_H

/*
 * Included by webkit_java_api.h, not the other way round - see INTEGRATION above. Naming this
 * header directly is a mistake worth catching, because the three struct definitions below
 * would then be processed before wkj_ref and WKJ_EXPORT exist.
 */
#ifndef WEBKIT_JAVA_API_H
#error "include webkit_java_api.h; it includes this header at the right point"
#endif

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/* ======================================================================================== */
/* One constant this ABI invents                                                            */
/* ======================================================================================== */

/*
 * The separator used by media.get_supported_types to return a String[] as one string.
 * U+000A cannot appear in a MIME type, so the encoding is lossless.
 *
 * Every other constant this slice needs - the GraphicsDecoder opcodes, WCPath's winding
 * rules, WCRenderQueue.MAX_QUEUE_SIZE, WCMediaPlayer's state and preload values and
 * LoadListenerClient's error codes - lives in wkj_constants.h, which is generated from the
 * Java declarations and replaces all 23 of the com_sun_webkit_*.h headers javac -h used to
 * emit. The call sites include <wkj_constants.h> and keep the original mangled names, so
 * nothing is duplicated here.
 */
#define WKJ_MEDIA_TYPE_SEPARATOR 0x000A

/* ======================================================================================== */
/* WKJHostGraphics - Source/WebCore/platform/graphics/java                                  */
/* ======================================================================================== */

/*
 * Replaces the 78 cached-jmethodID upcall sites against com.sun.webkit.graphics.
 * {WCGraphicsManager, Ref, WCRenderQueue, WCPath, WCFont, WCFontCustomPlatformData,
 * WCTextRun, WCImage, WCImageDecoder, WCImageFrame}. PL_GetGraphicsManager() disappears with
 * the table: the manager is a process singleton on the Java side, so the slots that were
 * instance methods on it take no target ref.
 */
typedef struct WKJHostGraphics {

    /* --- WCGraphicsManager (the singleton; no target ref) --------------------------- */

    /* createWCPath(). Returns a new WCPath id, or 0. Default when NULL: 0. */
    wkj_ref (*create_path)(void);

    /* createWCPath(WCPath). Returns a new WCPath id, or 0. Default when NULL: 0. */
    wkj_ref (*copy_path)(wkj_ref path);

    /* createRTImage(int, int). Returns a new WCImage id, or 0. Default when NULL: 0. */
    wkj_ref (*create_rt_image)(int32_t width, int32_t height);

    /* createBufferedContextRQ(WCImage). Returns a new WCRenderQueue id. Default: 0. */
    wkj_ref (*create_buffered_context_rq)(wkj_ref image);

    /* createTransform(double x 6). Returns a new WCTransform id. Default: 0. */
    wkj_ref (*create_transform)(double m00, double m10, double m01,
                                double m11, double m02, double m12);

    /*
     * getWCFont(String name, boolean bold, boolean italic, float size). The argument order is
     * the Java one; note that FontPlatformDataJava's own helper takes (family, size, italic,
     * bold) and reorders at the call, as it always did. Returns a new WCFont id. Default: 0.
     */
    wkj_ref (*get_font)(const uint16_t* family, int32_t family_len,
                        int32_t bold, int32_t italic, float size);

    /* getImageDecoder(). Returns a new WCImageDecoder id. Default: 0. */
    wkj_ref (*get_image_decoder)(void);

    /*
     * fwkCreateFontCustomPlatformData(SharedBuffer). Returns a new WCFontCustomPlatformData
     * id. Default: 0.
     */
    wkj_ref (*create_font_custom_platform_data)(wkj_ref shared_buffer);

    /*
     * com.sun.webkit.SharedBuffer.fwkCreate(long) - a static factory, not a WCGraphicsManager
     * method. It lives here only because its one caller is
     * graphics/java/FontCustomPlatformData.cpp; if webkit_java_api_page.h ends up owning
     * com.sun.webkit.SharedBuffer, this slot moves there and this comment is the trail.
     * Returns a new SharedBuffer id. Default: 0.
     */
    wkj_ref (*create_shared_buffer)(int64_t buffer);

    /*
     * fwkLoadFromResource(String, long). Reached only from the !USE(IMAGEIO) branch of
     * BitmapImage::createFromName, which this build does not compile. Kept so that the branch
     * stays translatable. Default when NULL: no-op.
     */
    void (*load_from_resource)(const uint16_t* name, int32_t name_len, int64_t buffer);

    /*
     * createFrame(int, int, ByteBuffer). Also !USE(IMAGEIO) only. "data" is the address of
     * "length" bytes owned by the caller and valid for the duration of the call; Java wraps
     * it. Returns a new WCImageFrame id. Default: 0.
     */
    wkj_ref (*create_frame)(int32_t width, int32_t height, void* data, int64_t length);

    /* --- com.sun.webkit.graphics.Ref --------------------------------------------------- */

    /*
     * Ref.getID(). The id is assigned by WCGraphicsManager.createID() on the Java side and is
     * what the command buffer carries, so no registry entry is minted for it here.
     * Default when NULL: -1, which is RQRef's "not yet resolved" value.
     */
    int32_t (*ref_get_id)(wkj_ref ref);

    /* Ref.ref(). Adds the object to WCGraphicsManager.refMap. Default when NULL: no-op. */
    void (*ref_ref)(wkj_ref ref);

    /*
     * Ref.deref(). Called from ~RQRef, which can run on ANY thread - the JNI version guarded
     * on GetJavaEnv() returning null after a VM detach and skipped the call. There is no
     * equivalent condition here, so the slot is simply called; the Java side must be safe on
     * any thread. Default when NULL: no-op.
     */
    void (*ref_deref)(wkj_ref ref);

    /* --- com.sun.webkit.graphics.WCRenderQueue ----------------------------------------- */

    /* fwkFlush(). Default when NULL: no-op. */
    void (*rq_flush)(wkj_ref rq);

    /* fwkDisposeGraphics(). Called from ~RenderingQueue. Default when NULL: no-op. */
    void (*rq_dispose_graphics)(wkj_ref rq);

    /*
     * fwkAddBuffer(ByteBuffer). "address" points at "length" bytes of command-buffer memory
     * owned by the C++ WebCore::ByteBuffer; Java wraps it without copying, exactly as
     * NewDirectByteBuffer did. The returned id names that Java buffer object and is held by
     * WebCore::ByteBuffer::m_nio_holder until the buffer is destroyed by wkj_rq_release, so
     * the Java object cannot be collected while the queue still refers to it.
     * Default when NULL: 0.
     */
    wkj_ref (*rq_add_buffer)(wkj_ref rq, void* address, int32_t length);

    /* refIntArr(int[]). Returns the slot id the command buffer stores. Default: 0. */
    int32_t (*rq_ref_int_array)(wkj_ref rq, const int32_t* data, int32_t count);

    /* refFloatArr(float[]). Returns the slot id the command buffer stores. Default: 0. */
    int32_t (*rq_ref_float_array)(wkj_ref rq, const float* data, int32_t count);

    /* --- com.sun.webkit.graphics.WCPath ------------------------------------------------ */

    void (*path_move_to)(wkj_ref path, double x, double y);
    void (*path_add_line_to)(wkj_ref path, double x, double y);
    void (*path_add_quad_curve_to)(wkj_ref path, double cx, double cy, double x, double y);
    void (*path_add_bezier_curve_to)(wkj_ref path, double c1x, double c1y,
                                     double c2x, double c2y, double x, double y);
    void (*path_add_arc_to)(wkj_ref path, double c1x, double c1y,
                            double c2x, double c2y, double radius);
    void (*path_add_arc)(wkj_ref path, double cx, double cy, double radius,
                         double start_angle, double end_angle, int32_t clockwise);
    void (*path_add_ellipse)(wkj_ref path, double x, double y, double width, double height);
    void (*path_add_rect)(wkj_ref path, double x, double y, double width, double height);
    void (*path_add_path)(wkj_ref path, wkj_ref other);
    void (*path_close_subpath)(wkj_ref path);

    /* isEmpty(). Default when NULL: 0. */
    int32_t (*path_is_empty)(wkj_ref path);

    void (*path_transform)(wkj_ref path, double a, double b, double c,
                           double d, double e, double f);

    /* contains(int rule, double, double), rule is WKJ_PATH_RULE_. Default when NULL: 0. */
    int32_t (*path_contains)(wkj_ref path, int32_t rule, double x, double y);

    /*
     * strokeContains(double x, double y, double thickness, double miterLimit, int lineCap,
     * int lineJoin, double dashOffset, double[] dashes). "dashes" may be NULL with dash_count
     * 0, which is what a solid stroke passed as a zero-length array before.
     * Default when NULL: 0.
     */
    int32_t (*path_stroke_contains)(wkj_ref path, double x, double y, double thickness,
                                    double miter_limit, int32_t line_cap, int32_t line_join,
                                    double dash_offset, const double* dashes,
                                    int32_t dash_count);

    /*
     * getBounds(), formerly a WCRectangle whose four float fields the C++ read back with
     * GetFieldID/GetFloatField. Writes x, y, w, h into out_xywh and returns 1; returns 0 and
     * writes nothing when Java returned null, which is the case PathJava turned into an empty
     * FloatRect. Default when NULL: 0.
     */
    int32_t (*path_get_bounds)(wkj_ref path, float out_xywh[4]);

    /* --- com.sun.webkit.graphics.WCFont ------------------------------------------------ */

    float (*font_get_x_height)(wkj_ref font);
    float (*font_get_cap_height)(wkj_ref font);
    float (*font_get_ascent)(wkj_ref font);
    float (*font_get_descent)(wkj_ref font);
    float (*font_get_line_spacing)(wkj_ref font);
    float (*font_get_line_gap)(wkj_ref font);

    /* hasUniformLineMetrics(). Default when NULL: 0. */
    int32_t (*font_has_uniform_line_metrics)(wkj_ref font);

    /* getGlyphWidth(int) returns a Java double; the caller narrows to float as it always did. */
    double (*font_get_glyph_width)(wkj_ref font, int32_t glyph);

    /*
     * getGlyphBoundingBox(int), formerly a float[4]. Writes x, y, width, height and returns
     * 1; returns 0 and writes nothing when Java returned null. The JNI version did not test
     * for null and dereferenced the array unconditionally, so this closes a crash rather than
     * changing a result. Default when NULL: 0.
     */
    int32_t (*font_get_glyph_bounding_box)(wkj_ref font, int32_t glyph, float out_xywh[4]);

    /*
     * getGlyphCodes(char[]). Writes one int32_t glyph per input code unit into out_glyphs and
     * returns the number written, or -1 when Java returned null (which GlyphPage::fill treats
     * as "no glyphs"). out_cap is char_count. Default when NULL: -1.
     */
    int32_t (*font_get_glyph_codes)(wkj_ref font, const uint16_t* chars, int32_t char_count,
                                    int32_t* out_glyphs, int32_t out_cap);

    /* deriveFont(float). Returns a new WCFont id. Default: 0. */
    wkj_ref (*font_derive)(wkj_ref font, float size);

    /* equals(Object) between two WCFonts. Default when NULL: 0. */
    int32_t (*font_equals)(wkj_ref font, wkj_ref other);

    /* hashCode(). Default when NULL: 0. */
    int32_t (*font_hash_code)(wkj_ref font);

    /*
     * getTextRuns(String) -> WCTextRun[]. Writes at most out_cap new WCTextRun ids into
     * out_runs and returns the TOTAL number of runs, so a caller whose buffer was too small
     * can retry with the reported size. When the total exceeds out_cap NOTHING is written and
     * no id is minted, so a retry cannot leak. Returns -1 when Java returned null, which
     * ComplexTextController turns into a missing-glyph run. The caller owns every id written.
     * Default when NULL: -1.
     */
    int32_t (*font_get_text_runs)(wkj_ref font, const uint16_t* text, int32_t text_len,
                                  wkj_ref* out_runs, int32_t out_cap);

    /* --- com.sun.webkit.graphics.WCFontCustomPlatformData ------------------------------ */

    /* createFont(int size, boolean bold, boolean italic). Returns a new WCFont id. Default: 0. */
    wkj_ref (*font_custom_data_create_font)(wkj_ref data, int32_t size,
                                            int32_t bold, int32_t italic);

    /* --- com.sun.webkit.graphics.WCTextRun --------------------------------------------- */

    int32_t (*text_run_is_left_to_right)(wkj_ref run);
    int32_t (*text_run_get_glyph_count)(wkj_ref run);
    int32_t (*text_run_get_start)(wkj_ref run);
    int32_t (*text_run_get_end)(wkj_ref run);
    int32_t (*text_run_get_char_offset)(wkj_ref run, int32_t glyph_index);
    int32_t (*text_run_get_glyph)(wkj_ref run, int32_t glyph_index);

    /*
     * getGlyphPosAndAdvance(int), formerly a float[4]. Writes x, y, advance width, advance
     * height and returns 1; returns 0 and writes nothing when Java returned null. As with
     * font_get_glyph_bounding_box the JNI version dereferenced unconditionally.
     * Default when NULL: 0.
     */
    int32_t (*text_run_get_glyph_pos_and_advance)(wkj_ref run, int32_t glyph_index,
                                                  float out_xywh[4]);

    /* --- com.sun.webkit.graphics.WCImage ----------------------------------------------- */

    /*
     * toData(String mimeType) -> byte[]. Follows the contract-13 buffer protocol on bytes:
     * WKJ_STR_OK with *out_length bytes written, WKJ_STR_NULL when Java returned null, or
     * WKJ_STR_OVERFLOW with *out_length set to the size required and nothing written. A retry
     * re-encodes the image on the Java side. Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*image_to_data)(wkj_ref image, const uint16_t* mime_type, int32_t mime_type_len,
                             uint8_t* out_buf, int32_t out_cap, int32_t* out_length);

    /*
     * getPixelBuffer() -> ByteBuffer. Returns the address of the image's BGRA backing store
     * and writes its capacity in bytes through out_capacity, or returns NULL with
     * *out_capacity = 0. The memory belongs to the Java image, not to this call, and stays
     * valid for as long as the image does - which is the same lifetime the JNI version relied
     * on when it let the local ref to the ByteBuffer die while keeping the address.
     * Default when NULL: NULL.
     */
    void* (*image_get_pixel_buffer)(wkj_ref image, int64_t* out_capacity);

    /* drawPixelBuffer(). Default when NULL: no-op. */
    void (*image_draw_pixel_buffer)(wkj_ref image);

    /* --- com.sun.webkit.graphics.WCImageDecoder ---------------------------------------- */
    /*
     * These ten are the only slots in this table also reached from decoder threads
     * (BitmapImage drives ImageDecoderJava from WorkQueue as well as from the main thread),
     * so their upcall stubs must come from a shared arena and their Java targets must be
     * thread-safe. That was already true of the JNI version, which used a global ref.
     */

    /* destroy(). Called from ~ImageDecoderJava. Default when NULL: no-op. */
    void (*image_decoder_destroy)(wkj_ref decoder);

    /*
     * addImageData(byte[]). "data == NULL" with length 0 is the end-of-stream call the JNI
     * code made by passing a null array. Default when NULL: no-op.
     */
    void (*image_decoder_add_image_data)(wkj_ref decoder, const uint8_t* data, int32_t length);

    /*
     * getImageSize() -> int[2]. Writes width, height and returns 1; returns 0 and writes
     * nothing when Java returned null. Default when NULL: 0.
     */
    int32_t (*image_decoder_get_image_size)(wkj_ref decoder, int32_t out_wh[2]);

    /* getFrameCount(). Default when NULL: 0 (the caller clamps to at least 1). */
    int32_t (*image_decoder_get_frame_count)(wkj_ref decoder);

    /* getFrame(int) -> WCImageFrame. Returns a new id, or 0. Default: 0. */
    wkj_ref (*image_decoder_get_frame)(wkj_ref decoder, int32_t index);

    /* getFrameDuration(int), in milliseconds. Default when NULL: 0. */
    int32_t (*image_decoder_get_frame_duration)(wkj_ref decoder, int32_t index);

    /*
     * getFrameSize(int) -> int[2]. Writes width, height and returns 1; returns 0 and writes
     * nothing when Java returned null, which the caller turns into the whole-image size.
     * Default when NULL: 0.
     */
    int32_t (*image_decoder_get_frame_size)(wkj_ref decoder, int32_t index, int32_t out_wh[2]);

    /* getFrameCompleteStatus(int). Default when NULL: 0. */
    int32_t (*image_decoder_get_frame_complete)(wkj_ref decoder, int32_t index);

    /*
     * getFilenameExtension() -> String, contract-13 protocol. WKJ_STR_NULL and WKJ_STR_OK
     * with length 0 both become the empty WTF::String, because String(env, jstring) always
     * collapsed a null jstring to StringImpl::empty(). Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*image_decoder_get_filename_extension)(wkj_ref decoder, uint16_t* out_buf,
                                                    int32_t out_cap, int32_t* out_length);

    /* --- com.sun.webkit.graphics.WCImageFrame ------------------------------------------ */

    /* getSize() -> int[2]. Writes width, height and returns 1, or returns 0. Default: 0. */
    int32_t (*image_frame_get_size)(wkj_ref frame, int32_t out_wh[2]);

} WKJHostGraphics;

/* ======================================================================================== */
/* WKJHostNetwork - Source/WebCore/platform/network/java                                    */
/* ======================================================================================== */

/*
 * Replaces the 12 upcall sites against com.sun.webkit.network.{NetworkContext, URLLoaderBase,
 * FormDataElement, SocketStreamHandle, CookieJar}. All are made on the WebKit main thread:
 * URLLoader::load runs from ResourceHandle and SocketStreamHandleImpl from the WebSocket
 * channel, both main-thread by WebCore contract. The completions come back the other way, as
 * the wkj_url_loader_ / wkj_socket_ downcalls below, which URLLoader.java already routes
 * through Invoker.invokeOnEventThread.
 */
typedef struct WKJHostNetwork {

    /*
     * NetworkContext.fwkLoad(WebPage, boolean asynchronous, String url, String method,
     * String headers, FormDataElement[] elements, long data) -> URLLoaderBase.
     *
     * "form_elements" may be NULL with count 0, which is how the JNI version passed a null
     * array for a request with no body. "target" is the WebCore::URLLoader::Target* the
     * completion downcalls carry back. Returns a new URLLoaderBase id, or 0. Default: 0.
     */
    wkj_ref (*url_loader_load)(wkj_ref web_page, int32_t asynchronous,
                               const uint16_t* url, int32_t url_len,
                               const uint16_t* method, int32_t method_len,
                               const uint16_t* headers, int32_t headers_len,
                               const wkj_ref* form_elements, int32_t form_element_count,
                               int64_t target);

    /* URLLoaderBase.fwkCancel(). Default when NULL: no-op. */
    void (*url_loader_cancel)(wkj_ref loader);

    /* FormDataElement.fwkCreateFromByteArray(byte[]). Returns a new id. Default: 0. */
    wkj_ref (*form_data_create_from_bytes)(const uint8_t* data, int32_t length);

    /*
     * FormDataElement.fwkCreateFromFile(String). Used both for a real file element and, as
     * the JNI code did, for a blob element's URL string. Returns a new id. Default: 0.
     */
    wkj_ref (*form_data_create_from_file)(const uint16_t* path, int32_t path_len);

    /*
     * SocketStreamHandle.fwkCreate(String host, int port, boolean ssl, WebPage, long data) ->
     * SocketStreamHandle. "handle" is the WebCore::SocketStreamHandleImpl* the wkj_socket_
     * downcalls carry back. Returns a new id. Default: 0.
     */
    wkj_ref (*socket_create)(const uint16_t* host, int32_t host_len, int32_t port,
                             int32_t ssl, wkj_ref web_page, int64_t handle);

    /*
     * fwkSend(byte[]) -> int. The caller distinguishes "the upcall threw" from a real result
     * by calling wkj_host->core.check_and_clear_exception() immediately afterwards, exactly
     * where the JNI code called WTF::CheckAndClearException and returned nullopt.
     * Default when NULL: 0.
     */
    int32_t (*socket_send)(wkj_ref socket, const uint8_t* data, int32_t length);

    /* fwkClose(). Default when NULL: no-op. */
    void (*socket_close)(wkj_ref socket);

    /* fwkNotifyDisposed(). Called from ~SocketStreamHandleImpl. Default when NULL: no-op. */
    void (*socket_notify_disposed)(wkj_ref socket);

    /*
     * CookieJar.fwkGet(String url, boolean includeHttpOnlyCookies) -> String, contract-13
     * protocol. WKJ_STR_NULL becomes the empty string, which is what the JNI code produced
     * for a null return. Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*cookie_jar_get)(const uint16_t* url, int32_t url_len,
                              int32_t include_http_only, uint16_t* out_buf,
                              int32_t out_cap, int32_t* out_length);

    /* CookieJar.fwkPut(String url, String cookie). Default when NULL: no-op. */
    void (*cookie_jar_put)(const uint16_t* url, int32_t url_len,
                           const uint16_t* value, int32_t value_len);

    /*
     * NetworkContext.fwkGetMaximumHTTPConnectionCountPerHost(). Reached only from
     * initializeMaximumHTTPConnectionCountPerHost, which sits inside "#if 0" in
     * ResourceRequestJava.cpp; the slot exists so that block stays translatable and so that
     * removing the cached ids did not silently delete the last reference to the Java method.
     * Default when NULL: 0.
     */
    int32_t (*get_max_http_connection_count_per_host)(void);

} WKJHostNetwork;

/* ======================================================================================== */
/* WKJHostMedia - MediaPlayerPrivateJava                                                    */
/* ======================================================================================== */

/*
 * Replaces the 16 upcall sites of MediaPlayerPrivateJava.cpp. The first two are methods on
 * WCGraphicsManager rather than on WCMediaPlayer and so take no target ref; they live here
 * rather than in WKJHostGraphics because MediaPlayerPrivateJava is their only caller and
 * because the media surface is what they belong to. Every slot runs on the WebKit main
 * thread: MediaPlayerPrivateInterface is main-thread by WebKit contract.
 */
typedef struct WKJHostMedia {

    /*
     * WCGraphicsManager.fwkCreateMediaPlayer(long) -> WCMediaPlayer. "player" is the
     * WebCore::MediaPlayerPrivate* that the wkj_media_notify_ downcalls carry back.
     * Returns a new WCMediaPlayer id, or 0. Default: 0.
     */
    wkj_ref (*create_player)(int64_t player);

    /*
     * WCGraphicsManager.getSupportedMediaTypes() -> String[], returned as one string whose
     * elements are separated by WKJ_MEDIA_TYPE_SEPARATOR. Contract-13 buffer protocol. An
     * empty array is WKJ_STR_OK with length 0. Default when NULL: WKJ_STR_NULL.
     */
    int32_t (*get_supported_types)(uint16_t* out_buf, int32_t out_cap, int32_t* out_length);

    /* fwkDispose(). Called from ~MediaPlayerPrivate. Default when NULL: no-op. */
    void (*dispose)(wkj_ref player);

    /*
     * fwkLoad(String url, String userAgent). "user_agent == NULL" is the null the JNI code
     * passed when the user agent was empty; do not normalise it to the empty string.
     * Default when NULL: no-op.
     */
    void (*load)(wkj_ref player, const uint16_t* url, int32_t url_len,
                 const uint16_t* user_agent, int32_t user_agent_len);

    void (*cancel_load)(wkj_ref player);
    void (*prepare_to_play)(wkj_ref player);
    void (*play)(wkj_ref player);
    void (*pause)(wkj_ref player);

    /* fwkGetCurrentTime(), in seconds. Default when NULL: 0.0f. */
    float (*get_current_time)(wkj_ref player);

    void (*seek)(wkj_ref player, float time);
    void (*set_rate)(wkj_ref player, float rate);
    void (*set_preserves_pitch)(wkj_ref player, int32_t preserve);
    void (*set_volume)(wkj_ref player, float volume);
    void (*set_mute)(wkj_ref player, int32_t mute);
    void (*set_size)(wkj_ref player, int32_t width, int32_t height);

    /* fwkSetPreload(int), one of WKJ_MEDIA_PRELOAD_. Default when NULL: no-op. */
    void (*set_preload)(wkj_ref player, int32_t preload);

} WKJHostMedia;

/* ======================================================================================== */
/* Downcalls: graphics                                                                      */
/* ======================================================================================== */

/*
 * Java_com_sun_webkit_graphics_WCGraphicsManager_append.
 *
 * Appends "count" bytes to the WebCore::SharedBufferBuilder named by "builder", which Java
 * received as the second argument of graphics.load_from_resource. The JNI version released
 * the critical array with JNI_ABORT, i.e. read-only, so passing a heap segment under
 * Linker.Option.critical(true) is safe here - this is one of the few entry points in this
 * header for which critical is appropriate.
 */
WKJ_EXPORT void wkj_shared_buffer_builder_append(int64_t builder,
                                                 const uint8_t* data, int32_t count);

/*
 * Java_com_sun_webkit_graphics_WCRenderQueue_twkRelease.
 *
 * Drops the library's reference to each command buffer named by its address, which is what
 * graphics.rq_add_buffer handed to Java. "buffer_addrs" is an array of "count" addresses; a 0
 * entry is ignored, matching the null-element handling of the JNI loop.
 *
 * Must be called on the event thread, for the reason the JNI version documented: destroying a
 * ByteBuffer dereferences the RQRefs it holds, and JavaScript may be touching the same
 * resources. That constraint is unchanged.
 */
WKJ_EXPORT void wkj_rq_release(const int64_t* buffer_addrs, int32_t count);

/* ======================================================================================== */
/* Downcalls: media - com.sun.webkit.graphics.WCMediaPlayer notifications                    */
/* ======================================================================================== */

/*
 * "player" is the WebCore::MediaPlayerPrivate* that WKJHostMedia.create_player was given.
 * Every one of these can run script (MediaPlayer forwards to the HTMLMediaElement's event
 * loop), so none may be bound with Linker.Option.critical(true).
 */

/* state is WKJ_MEDIA_NETWORK_STATE_...; an unrecognised value is ignored, as before. */
WKJ_EXPORT void wkj_media_notify_network_state(int64_t player, int32_t state);

/* state is WKJ_MEDIA_READY_STATE_...; an unrecognised value is ignored, as before. */
WKJ_EXPORT void wkj_media_notify_ready_state(int64_t player, int32_t state);

WKJ_EXPORT void wkj_media_notify_paused(int64_t player, int32_t paused);

/*
 * The ready_state argument is accepted and ignored, exactly as the JNI function did - it
 * named its third parameter in a comment and never read it. Dropping it from the ABI would be
 * a Java-side change, so it stays.
 */
WKJ_EXPORT void wkj_media_notify_seeking(int64_t player, int32_t seeking, int32_t ready_state);

WKJ_EXPORT void wkj_media_notify_finished(int64_t player);

/* Also reports the duration, but only when duration >= 0, as the JNI function did. */
WKJ_EXPORT void wkj_media_notify_ready(int64_t player, int32_t has_video,
                                       int32_t has_audio, float duration);

/* Reports only when the value actually differs from the player's current duration. */
WKJ_EXPORT void wkj_media_notify_duration_changed(int64_t player, float duration);

WKJ_EXPORT void wkj_media_notify_size_changed(int64_t player, int32_t width, int32_t height);

WKJ_EXPORT void wkj_media_notify_new_frame(int64_t player);

/*
 * "ranges" holds "count" floats read in (start, end) pairs; count is the number of floats,
 * not the number of pairs, matching GetArrayLength on the float[] the JNI version received.
 */
WKJ_EXPORT void wkj_media_notify_buffer_changed(int64_t player, const float* ranges,
                                                int32_t count, int32_t bytes_loaded);

/* ======================================================================================== */
/* Downcalls: network - com.sun.webkit.network.URLLoaderBase completions                     */
/* ======================================================================================== */

/*
 * "target" is the WebCore::URLLoader::Target* that WKJHostNetwork.url_loader_load was given.
 * URLLoader.java marshals every one of these onto the event thread for an asynchronous load
 * and calls them inline for a synchronous one; that is unchanged.
 */

WKJ_EXPORT void wkj_url_loader_did_send_data(int64_t target, int64_t total_bytes_sent,
                                             int64_t total_bytes_to_be_sent);

/*
 * twkWillSendRequest and twkDidReceiveResponse carry the same seven values and both build a
 * WebCore::ResourceResponse from them. They are kept as two flat parameter lists rather than
 * the WkjResponseInfo struct the audit sketched: contract 12 rules that a struct crossing
 * this boundary needs a Java StructLayout and per-call allocation, while flat (pointer,
 * length) pairs need neither, and the flat form is what all 1796 DOM entry points use.
 *
 * A NULL string is the Java null; content_type and content_encoding are both defaulted inside
 * the library exactly as setupResponse always did.
 */
WKJ_EXPORT void wkj_url_loader_will_send_request(int64_t target, int32_t status,
                                                 const uint16_t* content_type,
                                                 int32_t content_type_len,
                                                 const uint16_t* content_encoding,
                                                 int32_t content_encoding_len,
                                                 int64_t content_length,
                                                 const uint16_t* headers, int32_t headers_len,
                                                 const uint16_t* url, int32_t url_len);

WKJ_EXPORT void wkj_url_loader_did_receive_response(int64_t target, int32_t status,
                                                    const uint16_t* content_type,
                                                    int32_t content_type_len,
                                                    const uint16_t* content_encoding,
                                                    int32_t content_encoding_len,
                                                    int64_t content_length,
                                                    const uint16_t* headers,
                                                    int32_t headers_len,
                                                    const uint16_t* url, int32_t url_len);

/*
 * "data" is the base address of the direct ByteBuffer the JNI version read with
 * GetDirectBufferAddress; "position" and "remaining" keep their meaning unchanged.
 *
 * MUST NOT be bound with Linker.Option.critical(true): the body creates a SharedBuffer and
 * dispatches into ResourceHandleClient, which reaches arbitrary WebKit work including script.
 */
WKJ_EXPORT void wkj_url_loader_did_receive_data(int64_t target, const uint8_t* data,
                                                int32_t position, int32_t remaining);

WKJ_EXPORT void wkj_url_loader_did_finish_loading(int64_t target);

WKJ_EXPORT void wkj_url_loader_did_fail(int64_t target, int32_t error_code,
                                        const uint16_t* url, int32_t url_len,
                                        const uint16_t* message, int32_t message_len);

/* ======================================================================================== */
/* Downcalls: network - com.sun.webkit.network.SocketStreamHandle completions                */
/* ======================================================================================== */

/* "handle" is the WebCore::SocketStreamHandleImpl* WKJHostNetwork.socket_create was given. */

WKJ_EXPORT void wkj_socket_did_open(int64_t handle);

/*
 * MUST NOT be bound with Linker.Option.critical(true): the body calls
 * SocketStreamHandleClient::didReceiveSocketStreamData, which delivers the frame to the
 * WebSocket channel and from there to script.
 */
WKJ_EXPORT void wkj_socket_did_receive_data(int64_t handle, const uint8_t* data,
                                            int32_t length);

WKJ_EXPORT void wkj_socket_did_fail(int64_t handle, int32_t error_code,
                                    const uint16_t* description, int32_t description_len);

WKJ_EXPORT void wkj_socket_did_close(int64_t handle);

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_PLATFORM_H */
