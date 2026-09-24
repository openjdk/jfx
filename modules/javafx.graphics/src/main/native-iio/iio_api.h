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
 * iio_api.h - the flat C ABI of the javafx_iio library (the bundled IJG libjpeg decoder).
 *
 * This is the only surface com.sun.javafx.iio.jpeg.JPEGNative binds through java.lang.foreign. The
 * library knows nothing about the JVM: every function takes and returns <stdint.h> scalars, an
 * opaque void* decoder handle, caller-owned buffers and one callback table. libjpeg stays entirely
 * behind this boundary, and the decoded samples are byte-identical to the JNI-era decoder because
 * it is the same codec driven the same way.
 *
 * Blocking and upcalls. iio_create, iio_start_decompression and iio_decompress pull compressed
 * bytes from the Java InputStream through the callback table: libjpeg's source manager re-enters
 * Java synchronously, on the thread that made the downcall, from inside the codec - never from a
 * foreign thread. Because these three functions block and call back they must never be bound with
 * Linker.Option.critical, and iio_decompress writes into off-heap memory only.
 *
 * Errors. Nothing is thrown across the boundary. Every fallible function returns an IIO_* status
 * and writes a NUL-terminated ASCII message into the caller's err buffer (err_cap bytes including
 * the NUL; IIO_ERR_BUF_SIZE holds every message). Java throws the exception type the JNI glue
 * threw, with the same text:
 *
 *   IIO_ERR_IO      -> java.io.IOException(err)
 *       any libjpeg error formatted by jpeg_std_error's format_message (for example
 *       "Not a JPEG file: starts with 0xff 0xd9", "Invalid component ID 13 in SOS",
 *       "Premature end of JPEG file"), and the ICC marker checks of iio_create:
 *       "Invalid icc profile: inconsistent num_markers fields",
 *       "Invalid icc profile: bad sequence number",
 *       "Invalid icc profile: duplicate sequence numbers",
 *       "Invalid icc profile: invalid number of icc markers",
 *       "Invalid icc profile: missing sequence number",
 *       "Invalid icc profile: invalid data length",
 *       "Invalid icc profile: found only empty markers".
 *   IIO_ERR_OOM     -> java.lang.OutOfMemoryError(err)
 *       "Initializing Reader"  (iio_create could not allocate the decoder),
 *       "Reading ICC profile"  (iio_create could not allocate the ICC profile copy),
 *       "Reading JPEG Stream"  (iio_decompress: the output geometry overflows int, dst is NULL or
 *                               smaller than width * components * height, or the scanline buffer
 *                               could not be allocated).
 *   IIO_ERR_PENDING -> rethrow the Throwable the upcall stub stashed (err is "")
 *       an upcall returned IIO_READ_ERROR; the decoder unwound through libjpeg's error path
 *       exactly as the JNI ExceptionCheck -> error_exit -> longjmp did, and this status stands
 *       for the exception that was left pending on the JNI thread.
 *
 *   Gone with JNI (no FFM equivalent): OutOfMemoryError("Setting Stream") (NewGlobalRef),
 *   IOException("Get array elements failed") and IOException("Unable to pin icc profile data
 *   array") (array pinning), and the stderr-only GetPrimitiveArrayCritical failure that made
 *   decompressIndirect return false without an exception.
 *
 * Callbacks. Java fills IioJpegCallbacks with upcall stubs; void* user is an opaque Java-assigned
 * id that is handed back unchanged and never dereferenced. iio_create copies the table, so the
 * struct itself may be freed once it returns, but the stubs it points to must stay valid until
 * iio_dispose returns. A NULL slot is tolerated: read acts as an InputStream at end of stream,
 * skip as one that skipped nothing, emit_warning and update_progress as no-ops.
 *
 * Upcall stubs must not throw. When the Java target throws, the stub stashes the Throwable in the
 * loader's pending slot and returns IIO_READ_ERROR (any non-zero value from emit_warning or
 * update_progress is treated the same); the library then aborts through libjpeg's error_exit and
 * the enclosing iio_* call returns IIO_ERR_PENDING. One site is deliberately different: a libjpeg
 * warning (emit_warning with msg != NULL) had its exception cleared and swallowed by the JNI glue
 * (checkAndClearException), so the library ignores that return value and the stub must swallow -
 * not stash - a Throwable raised there. Only the missing-EOI warning (msg == NULL) can abort.
 *
 * Upcall sequences (all on the calling thread; read* means "read whenever the 4 KB buffer runs
 * dry", skip only for markers larger than what the buffer holds):
 *   normal decode:  iio_create: read* [skip*]; iio_start_decompression: read*; iio_decompress:
 *                   for each scanline n = 0 .. output_height-1: update_progress(n), read*; then
 *                   update_progress(output_height) - the JNI decompressIndirect loop, one
 *                   progress upcall per row before that row is read, plus the final one.
 *   truncated file: a read returning 0 or -1 (or a skip returning <= 0) is followed by
 *                   emit_warning(user, NULL) - the former emitWarning(READ_NO_EOI) - and a
 *                   synthetic EOI marker; libjpeg then reports
 *                   emit_warning(user, "Corrupt JPEG data: premature end of data segment") through
 *                   its output_message, and the decode completes normally. The NULL warning is
 *                   repeated for every further read/skip that hits the end (rare: after the
 *                   synthetic EOI libjpeg normally asks for no more input); libjpeg's own warning
 *                   arrives at most once per decoder, because jpeg_std_error's emit_message shows
 *                   only the first warning.
 *   stream throws:  read/skip returns IIO_READ_ERROR -> error_exit -> the iio_* call returns
 *                   IIO_ERR_PENDING with err "" and makes no further upcall.
 *
 * Threading. Nothing is synchronized: one decoder handle belongs to one thread at a time, as the
 * JNI structPointer did (JPEGImageLoader serialises its own use).
 */

#ifndef IIO_API_H
#define IIO_API_H

#include <stdint.h>

#if defined(_WIN32)
#  define IIO_EXPORT __declspec(dllexport)
#else
#  define IIO_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Bumped whenever a symbol, a prototype, a struct layout or a constant changes. Java binds
 * iio_abi_version first and every other symbol eagerly.
 * 2: added iio_sizeof_callbacks. Nothing existing changed, so the bump rests on the rule
 * jfxmedia_api.h records: Java resolves every symbol of this header eagerly, iio_abi_version first
 * of all, so against a library built before the symbol existed the bump turns "missing native
 * symbol: iio_sizeof_callbacks" into the version mismatch this guard exists to report. The two
 * sides do not always ship together - -DskipNative=true reuses whatever an earlier build left in
 * target/native/bin, and a stale copy in ../caches/sdk/bin can shadow it.
 */
#define IIO_ABI_VERSION 2

/* Status codes; see the exception mapping above. */
enum {
    IIO_OK          = 0,
    IIO_ERR_IO      = 1,
    IIO_ERR_OOM     = 2,
    IIO_ERR_PENDING = 3
};

/*
 * Returned by a read/skip/emit_warning/update_progress stub that caught a Java exception and
 * stashed it. Distinct from the InputStream end-of-stream values 0 and -1.
 */
#define IIO_READ_ERROR (-2)

/* Enough for every message this library writes (libjpeg's JMSG_LENGTH_MAX is 200). */
#define IIO_ERR_BUF_SIZE 256

/*
 * The four former JNI upcalls. Each is invoked synchronously on the thread inside iio_create,
 * iio_start_decompression or iio_decompress.
 */
typedef struct IioJpegCallbacks {
    /*
     * InputStream.read(buf, 0, len) (former CallIntMethod read([BII)I): copy up to len bytes into
     * dst and return the count; 0 or -1 means end of stream (a count above len is clamped);
     * IIO_READ_ERROR means the stream threw.
     */
    int32_t (*read)(void* user, uint8_t* dst, int32_t len);
    /*
     * InputStream.skip(n) (former CallLongMethod skip(J)J): return the bytes skipped; <= 0 is
     * treated as end of stream, a short positive skip is accepted as-is; IIO_READ_ERROR means the
     * stream threw.
     */
    int64_t (*skip)(void* user, int64_t n);
    /*
     * JPEGImageLoader.emitWarning(String). msg == NULL is the former emitWarning(READ_NO_EOI) sent
     * when the stream ended before the EOI marker; otherwise msg is a NUL-terminated ASCII libjpeg
     * message (former NewStringUTF + emitWarning). Return 0 to continue; IIO_READ_ERROR aborts the
     * decode, but only when msg == NULL - for msg != NULL the value is ignored (see above).
     */
    int32_t (*emit_warning)(void* user, const char* msg);
    /*
     * JPEGImageLoader.updateImageProgress(int scanline). Return 0 to continue, IIO_READ_ERROR to
     * abort the decode with the stashed exception.
     */
    int32_t (*update_progress)(void* user, int32_t scanline);
} IioJpegCallbacks;

/*
 * Four function pointers and nothing else: 4 * sizeof(void*), 32 bytes on every 64-bit target, no
 * padding. Java builds the table as four ADDRESS slots and verifies against iio_sizeof_callbacks.
 */
#if defined(__cplusplus)
static_assert(sizeof(IioJpegCallbacks) == 4 * sizeof(void*), "IioJpegCallbacks must be 4 pointers");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(IioJpegCallbacks) == 4 * sizeof(void*), "IioJpegCallbacks must be 4 pointers");
#else
/* Pre-C11 (MSVC without /std:c11): see the IioImageInfo check below. */
typedef char iio_jpeg_callbacks_size_check[(sizeof(IioJpegCallbacks) == 4 * sizeof(void*)) ? 1 : -1];
#endif

/*
 * What setInputAttributes(IIIII[B) carried, ICC bytes excepted (see iio_get_icc_profile). Five
 * consecutive int32_t, sizeof == 20, no padding; Java verifies against iio_sizeof_image_info.
 * A tables-only datastream (JPEG_HEADER_TABLES_ONLY) leaves every field 0 - the JNI never called
 * setInputAttributes for it - so width == 0 is the "no image header" signal.
 */
typedef struct IioImageInfo {
    int32_t width;              /* cinfo->image_width */
    int32_t height;             /* cinfo->image_height */
    int32_t jpeg_color_space;   /* cinfo->jpeg_color_space after the Adobe/JFIF fixups */
    int32_t out_color_space;    /* cinfo->out_color_space the decoder will produce by default */
    int32_t num_components;     /* cinfo->num_components */
} IioImageInfo;

#if defined(__cplusplus)
static_assert(sizeof(IioImageInfo) == 20, "IioImageInfo must be 20 bytes");
#elif defined(__STDC_VERSION__) && __STDC_VERSION__ >= 201112L
_Static_assert(sizeof(IioImageInfo) == 20, "IioImageInfo must be 20 bytes");
#else
/* Pre-C11 (MSVC without /std:c11): a negative array size is the compile-time assertion. */
typedef char iio_image_info_size_check[(sizeof(IioImageInfo) == 20) ? 1 : -1];
#endif

/* Guards - Java binds these first. */
IIO_EXPORT int32_t iio_abi_version(void);           /* IIO_ABI_VERSION */
IIO_EXPORT int64_t iio_sizeof_image_info(void);     /* sizeof(IioImageInfo) == 20 */
IIO_EXPORT int64_t iio_sizeof_callbacks(void);      /* sizeof(IioJpegCallbacks) == 4 * sizeof(void*) */

/*
 * Former initDecompressor: allocate a decoder bound to (cb, user), read the JPEG header through the
 * callbacks, apply the colour-space fixups, capture any embedded ICC profile and fill *info. On
 * IIO_OK *handle receives the decoder; on any error *handle is NULL, the decoder is already
 * disposed, and err holds the message (or "" for IIO_ERR_PENDING). info may be NULL.
 */
IIO_EXPORT int32_t iio_create(const IioJpegCallbacks* cb, void* user, void** handle,
                              IioImageInfo* info, char* err, int32_t err_cap);

/*
 * Embedded ICC profile reassembled from the APP2 markers during iio_create (former
 * read_icc_profile). iio_get_icc_profile_length returns the byte count, 0 when the file carries
 * none, -1 for a NULL handle. iio_get_icc_profile copies the whole profile into dst and returns the
 * bytes written (0 when there is none); -1 if handle is NULL or dst_cap is smaller than the length.
 */
IIO_EXPORT int32_t iio_get_icc_profile_length(void* handle);
IIO_EXPORT int32_t iio_get_icc_profile(void* handle, uint8_t* dst, int32_t dst_cap);

/*
 * Former startDecompression: set cinfo->out_color_space, choose the libjpeg scale (1/1, 1/2, 1/4
 * or 1/8 so the output stays at least as large as dest_width x dest_height), jpeg_start_decompress,
 * and report the geometry the former setOutputAttributes(II) carried plus output_components.
 * The out-parameters are written only on IIO_OK and may be NULL.
 */
IIO_EXPORT int32_t iio_start_decompression(void* handle, int32_t out_color_space,
                                           int32_t dest_width, int32_t dest_height,
                                           int32_t* out_width, int32_t* out_height,
                                           int32_t* out_components, char* err, int32_t err_cap);

/*
 * Former decompressIndirect: decode every scanline into dst, row stride output_width *
 * output_components bytes, no padding; dst_bytes must be at least stride * output_height or
 * IIO_ERR_OOM("Reading JPEG Stream") is returned before anything is read. report_progress != 0
 * drives update_progress as described above. Ends with jpeg_finish_decompress. BLOCKS and upcalls.
 */
IIO_EXPORT int32_t iio_decompress(void* handle, int32_t report_progress,
                                  uint8_t* dst, int64_t dst_bytes, char* err, int32_t err_cap);

/* Former disposeNative: jpeg_destroy plus every buffer the handle owns. NULL is ignored. */
IIO_EXPORT void    iio_dispose(void* handle);

#ifdef __cplusplus
}
#endif

#endif /* IIO_API_H */
