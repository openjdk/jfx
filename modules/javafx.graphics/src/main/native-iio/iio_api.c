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
 * iio_api.c - the iio_* entry points declared in iio_api.h.
 *
 * The libjpeg source manager, error manager and ICC reassembly are the ones the former JNI glue
 * (jpegloader.c, now removed) had driven since JDK-era imageio, with the JNI taken out: the 4 KB
 * stream buffer is owned here instead of being a pinned Java byte[], InputStream.read/skip and
 * the two JPEGImageLoader callbacks arrive through IioJpegCallbacks, and every ThrowByName became
 * a status plus message. Errors still leave the codec through libjpeg's error_exit -> longjmp to
 * the setjmp established by the entry point.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <setjmp.h>

#include "iio_api.h"

/* headers from libjpeg */
#include <jpeglib.h>

#ifdef __APPLE__
/* JDK-8097189: use setjmp/longjmp versions that do not save/restore the signal mask */
#define longjmp _longjmp
#define setjmp _setjmp
#endif

/*
 * This buffer size was set to 64K in the old classes, 4K by default in the
 * IJG library, with the comment "an efficiently freadable size", and 1K
 * in AWT. If 4K was good enough for the IJG folks, it's good enough for me.
 */
#define IIO_STREAMBUF_SIZE 4096

/*
 * Since an ICC profile can be larger than the maximum size of a JPEG marker
 * (64K), we need provisions to split it into multiple markers.  The format
 * defined by the ICC specifies one or more APP2 markers containing the
 * following data:
 *      Identifying string      ASCII "ICC_PROFILE\0"  (12 bytes)
 *      Marker sequence number  1 for first APP2, 2 for next, etc (1 byte)
 *      Number of markers       Total number of APP2's used (1 byte)
 *      Profile data            (remainder of APP2 data)
 * Decoders should use the marker sequence numbers to reassemble the profile,
 * rather than assuming that the APP2 markers appear in the correct sequence.
 */
#define IIO_ICC_MARKER          (JPEG_APP0 + 2)     /* JPEG marker code for ICC */
#define IIO_ICC_OVERHEAD_LEN    14                  /* size of non-profile data in APP2 */
#define IIO_MAX_BYTES_IN_MARKER 65533               /* maximum data len of a JPEG marker */
#define IIO_MAX_SEQ_NO          255                 /* sufficient since marker numbers are bytes */

/* Error manager: the standard one plus the return point for iio_error_exit. */
struct iio_error_mgr {
    struct jpeg_error_mgr pub;      /* "public" fields */
    jmp_buf setjmp_buffer;          /* for return to caller */
};

/*
 * The decoder behind the opaque handle. cinfo->client_data points back at it so the source manager
 * and the error manager can reach the callbacks, as imageIOData did for the JNI glue.
 */
typedef struct IioDecoder {
    struct jpeg_decompress_struct cinfo;
    struct iio_error_mgr jerr;
    struct jpeg_source_mgr src;
    IioJpegCallbacks cb;
    void* user;
    JOCTET* buf;                    /* IIO_STREAMBUF_SIZE bytes of compressed input */
    uint8_t* icc_data;              /* reassembled ICC profile, or NULL */
    int32_t icc_len;
    JSAMPROW scanline;              /* iio_decompress's one-row buffer; lives here so the setjmp
                                       handler never reads a longjmp-clobbered local */
    int32_t pending;                /* an upcall returned IIO_READ_ERROR: report IIO_ERR_PENDING */
} IioDecoder;

/****************************** helpers ******************************/

static void
iio_set_err(char* err, int32_t err_cap, const char* msg) {
    int32_t i = 0;
    if (err == NULL || err_cap <= 0) {
        return;
    }
    if (msg != NULL) {
        while (i < err_cap - 1 && msg[i] != '\0') {
            err[i] = msg[i];
            i++;
        }
    }
    err[i] = '\0';
}

/*
 * The setjmp handler shared by the entry points: what the JNI did with
 * "if (!ExceptionOccurred) ThrowByName(IOException, format_message())".
 */
static int32_t
iio_error_status(IioDecoder* d, char* err, int32_t err_cap) {
    char buffer[JMSG_LENGTH_MAX];
    if (d->pending) {
        iio_set_err(err, err_cap, "");
        return IIO_ERR_PENDING;
    }
    (*d->cinfo.err->format_message)((j_common_ptr) &d->cinfo, buffer);
    iio_set_err(err, err_cap, buffer);
    return IIO_ERR_IO;
}

/* An upcall reported a stashed Java exception: unwind like the JNI ExceptionCheck -> error_exit. */
static void
iio_abort_pending(IioDecoder* d) {
    d->pending = 1;
    (*d->cinfo.err->error_exit)((j_common_ptr) &d->cinfo);
}

/* emitWarning(READ_NO_EOI) - the JNI passed the int 0 as the String argument, i.e. null. */
static void
iio_warn_no_eoi(IioDecoder* d) {
    if (d->cb.emit_warning != NULL) {
        if ((*d->cb.emit_warning)(d->user, NULL) != 0) {
            iio_abort_pending(d);
        }
    }
}

static void
iio_progress(IioDecoder* d, int32_t scanline) {
    if (d->cb.update_progress != NULL) {
        if ((*d->cb.update_progress)(d->user, scanline) != 0) {
            iio_abort_pending(d);
        }
    }
}

/****************************** error handling ******************************/

/*
 * Replaces the standard error_exit method: return control to the setjmp point.
 */
static void
iio_error_exit(j_common_ptr cinfo) {
    /* cinfo->err really points to an iio_error_mgr struct */
    struct iio_error_mgr* myerr = (struct iio_error_mgr*) cinfo->err;
    longjmp(myerr->setjmp_buffer, 1);
}

/*
 * Overrides the output_message method to send JPEG warnings to emitWarning(String).
 * The JNI glue cleared any exception this upcall raised (checkAndClearException),
 * so the return value is deliberately ignored here.
 */
static void
iio_output_message(j_common_ptr cinfo) {
    char buffer[JMSG_LENGTH_MAX];
    IioDecoder* d = (IioDecoder*) cinfo->client_data;

    /* Create the message */
    (*cinfo->err->format_message)(cinfo, buffer);

    if (cinfo->is_decompressor && d != NULL && d->cb.emit_warning != NULL) {
        (void) (*d->cb.emit_warning)(d->user, buffer);
    }
}

/****************************** source manager ******************************/

/*
 * Initialize source.  This is called by jpeg_read_header() before any
 * data is actually read.  It may leave bytes_in_buffer set to 0 (in which
 * case a fill_input_buffer() call will occur immediately).
 */
static void
iio_init_source(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr* src = cinfo->src;
    src->next_input_byte = NULL;
    src->bytes_in_buffer = 0;
}

/*
 * This is called whenever bytes_in_buffer has reached zero and more
 * data is wanted.  It reads fresh data into the buffer, resets the pointer
 * and count to the start of the buffer, and returns TRUE.  It is not
 * necessary to fill the buffer entirely, only to obtain at least one more
 * byte.  bytes_in_buffer MUST be set to a positive value when TRUE is
 * returned.
 */
static boolean
iio_fill_input_buffer(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr* src = cinfo->src;
    IioDecoder* d = (IioDecoder*) cinfo->client_data;
    int32_t ret = 0;

    /*
     * Now fill a complete buffer, or as much of one as the stream
     * will give us if we are near the end.
     */
    if (d->cb.read != NULL) {
        ret = (*d->cb.read)(d->user, d->buf, IIO_STREAMBUF_SIZE);
        if (ret == IIO_READ_ERROR) {
            iio_abort_pending(d);
        }
    }
    if (ret > IIO_STREAMBUF_SIZE) {
        ret = IIO_STREAMBUF_SIZE;
    }

    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        iio_warn_no_eoi(d);

        d->buf[0] = (JOCTET) 0xFF;
        d->buf[1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->next_input_byte = d->buf;
    src->bytes_in_buffer = (size_t) ret;

    return TRUE;
}

/*
 * Skip num_bytes worth of data.  The buffer pointer and count are
 * advanced over num_bytes input bytes, using the input stream
 * skip method if the skip is greater than the number of bytes
 * in the buffer.  This is used to skip over a potentially large amount of
 * uninteresting data (such as an APPn marker).  bytes_in_buffer will be
 * zero on return if the skip is larger than the current contents of the
 * buffer.
 *
 * A negative skip count is treated as a no-op.
 */
static void
iio_skip_input_data(j_decompress_ptr cinfo, long num_bytes) {
    struct jpeg_source_mgr* src = cinfo->src;
    IioDecoder* d = (IioDecoder*) cinfo->client_data;
    int64_t ret;

    if (num_bytes < 0) {
        return;
    }

    /* First the easy case where we are skipping <= the current contents. */
    ret = (int64_t) src->bytes_in_buffer;
    if (ret >= (int64_t) num_bytes) {
        src->next_input_byte += num_bytes;
        src->bytes_in_buffer -= (size_t) num_bytes;
        return;
    }

    /*
     * We are skipping more than is in the buffer.  We empty the buffer and
     * call the Java skip method.  We always leave the buffer empty, to be
     * filled by the fill method above.
     */
    src->bytes_in_buffer = 0;
    src->next_input_byte = d->buf;

    num_bytes -= (long) ret;

    ret = 0;
    if (d->cb.skip != NULL) {
        ret = (*d->cb.skip)(d->user, (int64_t) num_bytes);
        if (ret == IIO_READ_ERROR) {
            iio_abort_pending(d);
        }
    }

    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        iio_warn_no_eoi(d);

        d->buf[0] = (JOCTET) 0xFF;
        d->buf[1] = (JOCTET) JPEG_EOI;
        src->bytes_in_buffer = 2;
        src->next_input_byte = d->buf;
    }
}

/*
 * Terminate source --- called by jpeg_finish_decompress() after all
 * data for an image has been read.  Also called if resetting state
 * after reading a tables-only image.
 */
static void
iio_term_source(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr* src = cinfo->src;
    if (src->bytes_in_buffer > 0) {
        src->bytes_in_buffer = 0;
    }
}

/****************************** ICC profile support ******************************/
/*
 * The following routines are modified versions of the ICC
 * profile support routines available from the IJG website.
 * The originals were written by Todd Newman
 * <tdn@eccentric.esd.sgi.com> and modified by Tom Lane for
 * the IJG.  They are further modified to fit in the context
 * of the imageio JPEG plug-in.
 */

/*
 * Handy subroutine to test whether a saved marker is an ICC profile marker.
 */
static boolean
iio_marker_is_icc(jpeg_saved_marker_ptr marker) {
    return
        marker->marker == IIO_ICC_MARKER &&
        marker->data_length >= IIO_ICC_OVERHEAD_LEN &&
        /* verify the identifying string */
        GETJOCTET(marker->data[0]) == 0x49 &&
        GETJOCTET(marker->data[1]) == 0x43 &&
        GETJOCTET(marker->data[2]) == 0x43 &&
        GETJOCTET(marker->data[3]) == 0x5F &&
        GETJOCTET(marker->data[4]) == 0x50 &&
        GETJOCTET(marker->data[5]) == 0x52 &&
        GETJOCTET(marker->data[6]) == 0x4F &&
        GETJOCTET(marker->data[7]) == 0x46 &&
        GETJOCTET(marker->data[8]) == 0x49 &&
        GETJOCTET(marker->data[9]) == 0x4C &&
        GETJOCTET(marker->data[10]) == 0x45 &&
        GETJOCTET(marker->data[11]) == 0x0;
}

/*
 * See if there was an ICC profile in the JPEG file being read;
 * if so, reassemble it into d->icc_data / d->icc_len.
 * If there was no ICC profile, icc_len stays 0.
 *
 * If the file contains invalid ICC APP2 markers, IIO_ERR_IO is returned
 * with the message the JNI threw as an IOException.
 */
static int32_t
iio_read_icc_profile(IioDecoder* d, char* err, int32_t err_cap) {
    j_decompress_ptr cinfo = &d->cinfo;
    jpeg_saved_marker_ptr marker;
    int num_markers = 0;
    int num_found_markers = 0;
    int seq_no;
    JOCTET* dst_ptr;
    unsigned int total_length;
    jpeg_saved_marker_ptr icc_markers[IIO_MAX_SEQ_NO + 1];
    int first; /* index of the first marker in the icc_markers array */
    int last;  /* index of the last marker in the icc_markers array */

    /* This first pass over the saved markers discovers whether there are
     * any ICC markers and verifies the consistency of the marker numbering.
     */

    for (seq_no = 0; seq_no <= IIO_MAX_SEQ_NO; seq_no++) {
        icc_markers[seq_no] = NULL;
    }

    for (marker = cinfo->marker_list; marker != NULL; marker = marker->next) {
        if (iio_marker_is_icc(marker)) {
            if (num_markers == 0) {
                num_markers = GETJOCTET(marker->data[13]);
            } else if (num_markers != GETJOCTET(marker->data[13])) {
                iio_set_err(err, err_cap, "Invalid icc profile: inconsistent num_markers fields");
                return IIO_ERR_IO;
            }
            seq_no = GETJOCTET(marker->data[12]);

            /* Some third-party tools produce images with profile chunk
             * numeration started from zero. It is inconsistent with ICC
             * spec, but seems to be recognized by majority of image
             * processing tools, so we should be more tolerant to this
             * departure from the spec.
             */
            if (seq_no < 0 || seq_no > num_markers) {
                iio_set_err(err, err_cap, "Invalid icc profile: bad sequence number");
                return IIO_ERR_IO;
            }
            if (icc_markers[seq_no] != NULL) {
                iio_set_err(err, err_cap, "Invalid icc profile: duplicate sequence numbers");
                return IIO_ERR_IO;
            }
            icc_markers[seq_no] = marker;
            num_found_markers++;
        }
    }

    if (num_markers == 0) {
        return IIO_OK; /* There is no profile */
    }

    if (num_markers != num_found_markers) {
        iio_set_err(err, err_cap, "Invalid icc profile: invalid number of icc markers");
        return IIO_ERR_IO;
    }

    first = icc_markers[0] ? 0 : 1;
    last = num_found_markers + first;

    /* Check for missing markers, count total space needed.
     */
    total_length = 0;
    for (seq_no = first; seq_no < last; seq_no++) {
        unsigned int length;
        if (icc_markers[seq_no] == NULL) {
            iio_set_err(err, err_cap, "Invalid icc profile: missing sequence number");
            return IIO_ERR_IO;
        }
        /* check the data length correctness */
        length = icc_markers[seq_no]->data_length;
        if (IIO_ICC_OVERHEAD_LEN > length || length > IIO_MAX_BYTES_IN_MARKER) {
            iio_set_err(err, err_cap, "Invalid icc profile: invalid data length");
            return IIO_ERR_IO;
        }
        total_length += (length - IIO_ICC_OVERHEAD_LEN);
    }

    if (total_length == 0) {
        iio_set_err(err, err_cap, "Invalid icc profile: found only empty markers");
        return IIO_ERR_IO;
    }

    /* Allocate the assembled profile (the JNI's NewByteArray) */

    d->icc_data = (uint8_t*) malloc(total_length);
    if (d->icc_data == NULL) {
        iio_set_err(err, err_cap, "Reading ICC profile");
        return IIO_ERR_OOM;
    }
    d->icc_len = (int32_t) total_length;

    /* and fill it in */
    dst_ptr = (JOCTET*) d->icc_data;
    for (seq_no = first; seq_no < last; seq_no++) {
        JOCTET FAR* src_ptr = icc_markers[seq_no]->data + IIO_ICC_OVERHEAD_LEN;
        unsigned int length = icc_markers[seq_no]->data_length - IIO_ICC_OVERHEAD_LEN;

        memcpy(dst_ptr, src_ptr, length);
        dst_ptr += length;
    }

    return IIO_OK;
}

/****************************** exported entry points ******************************/

IIO_EXPORT int32_t
iio_abi_version(void) {
    return IIO_ABI_VERSION;
}

IIO_EXPORT int64_t
iio_sizeof_image_info(void) {
    return (int64_t) sizeof(IioImageInfo);
}

IIO_EXPORT int64_t
iio_sizeof_callbacks(void) {
    return (int64_t) sizeof(IioJpegCallbacks);
}

IIO_EXPORT int32_t
iio_create(const IioJpegCallbacks* cb, void* user, void** handle,
           IioImageInfo* info, char* err, int32_t err_cap) {
    IioDecoder* d;
    struct jpeg_decompress_struct* cinfo;
    int jret;
    int h_samp0, h_samp1, h_samp2;
    int v_samp0, v_samp1, v_samp2;
    int32_t status;

    iio_set_err(err, err_cap, "");
    if (info != NULL) {
        memset(info, 0, sizeof(IioImageInfo));
    }
    if (handle == NULL) {
        iio_set_err(err, err_cap, "Invalid JPEG decoder handle");
        return IIO_ERR_IO;
    }
    *handle = NULL;

    /* One allocation for what the JNI malloc'ed separately (cinfo, error mgr, source mgr, data). */
    d = (IioDecoder*) calloc(1, sizeof(IioDecoder));
    if (d == NULL) {
        iio_set_err(err, err_cap, "Initializing Reader");
        return IIO_ERR_OOM;
    }
    d->buf = (JOCTET*) malloc(IIO_STREAMBUF_SIZE);
    if (d->buf == NULL) {
        free(d);
        iio_set_err(err, err_cap, "Initializing Reader");
        return IIO_ERR_OOM;
    }
    if (cb != NULL) {
        d->cb = *cb;
    }
    d->user = user;

    cinfo = &d->cinfo;

    /* We set up the normal JPEG error routines, then override error_exit
     * and output_message. jpeg_create_decompress preserves err and client_data. */
    cinfo->err = jpeg_std_error(&d->jerr.pub);
    d->jerr.pub.error_exit = iio_error_exit;
    d->jerr.pub.output_message = iio_output_message;
    cinfo->client_data = d;

    /* Establish the setjmp return context for iio_error_exit to use. */
    if (setjmp(d->jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while creating the decoder or reading the header. */
        status = iio_error_status(d, err, err_cap);
        iio_dispose(d);
        return status;
    }

    /* Perform library initialization */
    jpeg_create_decompress(cinfo);

    // Set up to keep any APP2 markers, as these might contain ICC profile
    // data
    jpeg_save_markers(cinfo, IIO_ICC_MARKER, 0xFFFF);

    /*
     * Now set up our source.
     */
    cinfo->src = &d->src;
    d->src.bytes_in_buffer = 0;
    d->src.next_input_byte = NULL;
    d->src.init_source = iio_init_source;
    d->src.fill_input_buffer = iio_fill_input_buffer;
    d->src.skip_input_data = iio_skip_input_data;
    d->src.resync_to_restart = jpeg_resync_to_restart; // use default
    d->src.term_source = iio_term_source;

    iio_init_source(cinfo);

    jret = jpeg_read_header(cinfo, FALSE);

    if (jret == JPEG_HEADER_TABLES_ONLY) {
        iio_term_source(cinfo); // Pushback remaining buffer contents
        /* The JNI returned the handle without calling setInputAttributes: *info stays zeroed. */
    } else {
        /*
         * Now adjust the jpeg_color_space variable, which was set in
         * default_decompress_parms, to reflect our differences from IJG
         */

        switch (cinfo->jpeg_color_space) {
            default:
                break;
            case JCS_YCbCr:

                /*
                 * There are several possibilities:
                 *  - we got image with embeded colorspace
                 *     Use it. User knows what he is doing.
                 *  - we got JFIF image
                 *     Must be YCbCr (see http://www.w3.org/Graphics/JPEG/jfif3.pdf, page 2)
                 *  - we got EXIF image
                 *     Must be YCbCr (see http://www.exif.org/Exif2-2.PDF, section 4.7, page 63)
                 *  - something else
                 *     Apply heuristical rules to identify actual colorspace.
                 */

                if (cinfo->saw_Adobe_marker) {
                    if (cinfo->Adobe_transform != 1) {
                        /*
                         * IJG guesses this is YCbCr and emits a warning
                         * We would rather not guess.  Then the user knows
                         * To read this as a Raster if at all
                         */
                        cinfo->jpeg_color_space = JCS_UNKNOWN;
                        cinfo->out_color_space = JCS_UNKNOWN;
                    }
                }
                break;
            case JCS_YCCK:
                if ((cinfo->saw_Adobe_marker) && (cinfo->Adobe_transform != 2)) {
                    /*
                     * IJG guesses this is YCCK and emits a warning
                     * We would rather not guess.  Then the user knows
                     * To read this as a Raster if at all
                     */
                    cinfo->jpeg_color_space = JCS_UNKNOWN;
                    cinfo->out_color_space = JCS_UNKNOWN;
                } else {
                    /* There is no support for YCCK on jfx side, so request RGB output */
                    cinfo->out_color_space = JCS_RGB;
                }
                break;
            case JCS_CMYK:
                /*
                 * IJG assumes all unidentified 4-channels are CMYK.
                 * We assume that only if the second two channels are
                 * not subsampled (either horizontally or vertically).
                 * If they are, we assume YCCK.
                 */
                h_samp0 = cinfo->comp_info[0].h_samp_factor;
                h_samp1 = cinfo->comp_info[1].h_samp_factor;
                h_samp2 = cinfo->comp_info[2].h_samp_factor;

                v_samp0 = cinfo->comp_info[0].v_samp_factor;
                v_samp1 = cinfo->comp_info[1].v_samp_factor;
                v_samp2 = cinfo->comp_info[2].v_samp_factor;

                if (((h_samp1 > h_samp0) && (h_samp2 > h_samp0)) ||
                        ((v_samp1 > v_samp0) && (v_samp2 > v_samp0))) {
                    cinfo->jpeg_color_space = JCS_YCCK;
                    /* Leave the output space as CMYK */
                }

                /* There is no support for CMYK on jfx side, so request RGB output */
                cinfo->out_color_space = JCS_RGB;
                break;
        }

        /* read icc profile data */
        status = iio_read_icc_profile(d, err, err_cap);
        if (status != IIO_OK) {
            iio_dispose(d);
            return status;
        }

        /* What setInputAttributes(IIIII[B) carried, ICC excepted. */
        if (info != NULL) {
            info->width = (int32_t) cinfo->image_width;
            info->height = (int32_t) cinfo->image_height;
            info->jpeg_color_space = (int32_t) cinfo->jpeg_color_space;
            info->out_color_space = (int32_t) cinfo->out_color_space;
            info->num_components = (int32_t) cinfo->num_components;
        }
    }

    *handle = d;
    return IIO_OK;
}

IIO_EXPORT int32_t
iio_get_icc_profile_length(void* handle) {
    IioDecoder* d = (IioDecoder*) handle;
    if (d == NULL) {
        return -1;
    }
    return d->icc_len;
}

IIO_EXPORT int32_t
iio_get_icc_profile(void* handle, uint8_t* dst, int32_t dst_cap) {
    IioDecoder* d = (IioDecoder*) handle;
    if (d == NULL || dst_cap < d->icc_len || (d->icc_len > 0 && dst == NULL)) {
        return -1;
    }
    if (d->icc_len > 0) {
        memcpy(dst, d->icc_data, (size_t) d->icc_len);
    }
    return d->icc_len;
}

IIO_EXPORT int32_t
iio_start_decompression(void* handle, int32_t out_color_space,
                        int32_t dest_width, int32_t dest_height,
                        int32_t* out_width, int32_t* out_height,
                        int32_t* out_components, char* err, int32_t err_cap) {
    IioDecoder* d = (IioDecoder*) handle;
    struct jpeg_decompress_struct* cinfo;
    float x_scale;
    float y_scale;
    float max_scale;

    iio_set_err(err, err_cap, "");
    if (d == NULL) {
        iio_set_err(err, err_cap, "Invalid JPEG decoder handle");
        return IIO_ERR_IO;
    }
    d->pending = 0;
    cinfo = &d->cinfo;

    /* Establish the setjmp return context for iio_error_exit to use. */
    if (setjmp(d->jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while starting decompression. */
        return iio_error_status(d, err, err_cap);
    }

    cinfo->out_color_space = (J_COLOR_SPACE) out_color_space;

    /* decide how much we want to sub-sample the incoming jpeg image.
     * The libjpeg docs say:
     *
     *     unsigned int scale_num, scale_denom
     *
     *     Scale the image by the fraction scale_num/scale_denom.  Default is
     *     1/1, or no scaling.  Currently, the only supported scaling ratios
     *     are 1/1, 1/2, 1/4, and 1/8.  (The library design allows for arbitrary
     *     scaling ratios but this is not likely to be implemented any time soon.)
     *     Smaller scaling ratios permit significantly faster decoding since
     *     fewer pixels need be processed and a simpler IDCT method can be used.
     */

    cinfo->scale_num = 1;

    x_scale = (float) dest_width / (float) cinfo->image_width;
    y_scale = (float) dest_height / (float) cinfo->image_height;
    max_scale = x_scale > y_scale ? x_scale : y_scale;

    if (max_scale > 0.5) {
        cinfo->scale_denom = 1;
    } else if (max_scale > 0.25) {
        cinfo->scale_denom = 2;
    } else if (max_scale > 0.125) {
        cinfo->scale_denom = 4;
    } else {
        cinfo->scale_denom = 8;
    }

    jpeg_start_decompress(cinfo);

    /* What setOutputAttributes(II) carried, plus the former return value. */
    if (out_width != NULL) {
        *out_width = (int32_t) cinfo->output_width;
    }
    if (out_height != NULL) {
        *out_height = (int32_t) cinfo->output_height;
    }
    if (out_components != NULL) {
        *out_components = (int32_t) cinfo->output_components;
    }
    return IIO_OK;
}

/*
 * The decompressIndirect loop: one progress upcall per row before that row is read, the row copied
 * to dst at stride output_width * output_components (no padding), then the final progress upcall
 * and jpeg_finish_decompress. Runs below the setjmp frame of iio_decompress so that no local of
 * that frame is live across the longjmp (gcc -Wclobbered).
 */
static void
iio_read_scanlines(IioDecoder* d, int32_t report_progress, uint8_t* dst) {
    struct jpeg_decompress_struct* cinfo = &d->cinfo;
    size_t bytes_per_row = (size_t) cinfo->output_width * (size_t) cinfo->output_components;
    uint8_t* row = dst;

    while (cinfo->output_scanline < cinfo->output_height) {
        if (report_progress != 0) {
            iio_progress(d, (int32_t) cinfo->output_scanline);
        }

        if (jpeg_read_scanlines(cinfo, &d->scanline, 1) == 1) {
            memcpy(row, d->scanline, bytes_per_row);
            row += bytes_per_row;
        }
    }

    if (report_progress != 0) {
        iio_progress(d, (int32_t) cinfo->output_height);
    }

    jpeg_finish_decompress(cinfo);
}

IIO_EXPORT int32_t
iio_decompress(void* handle, int32_t report_progress,
               uint8_t* dst, int64_t dst_bytes, char* err, int32_t err_cap) {
    IioDecoder* d = (IioDecoder*) handle;
    struct jpeg_decompress_struct* cinfo;
    int64_t width;
    int64_t components;
    int64_t height;
    int64_t bytes_per_row;
    int32_t status;

    iio_set_err(err, err_cap, "");
    if (d == NULL) {
        iio_set_err(err, err_cap, "Invalid JPEG decoder handle");
        return IIO_ERR_IO;
    }
    d->pending = 0;
    cinfo = &d->cinfo;

    /*
     * The JNI SAFE_TO_MULT(a, b) = a > 0 && b >= 0 && 0x7fffffff / a > b, applied to
     * (output_width, output_components) and (bytes_per_row, output_height), then the
     * destination length check; all three failed with OutOfMemoryError("Reading JPEG Stream").
     */
    width = (int64_t) cinfo->output_width;
    components = (int64_t) cinfo->output_components;
    height = (int64_t) cinfo->output_height;
    if (!(width > 0 && components >= 0 && (0x7fffffff / width) > components)) {
        iio_set_err(err, err_cap, "Reading JPEG Stream");
        return IIO_ERR_OOM;
    }
    bytes_per_row = width * components;
    if (!(bytes_per_row > 0 && height >= 0 && (0x7fffffff / bytes_per_row) > height)) {
        iio_set_err(err, err_cap, "Reading JPEG Stream");
        return IIO_ERR_OOM;
    }
    if (dst == NULL || dst_bytes < bytes_per_row * height) {
        iio_set_err(err, err_cap, "Reading JPEG Stream");
        return IIO_ERR_OOM;
    }

    free(d->scanline);
    d->scanline = (JSAMPROW) malloc((size_t) bytes_per_row * sizeof(JSAMPLE));
    if (d->scanline == NULL) {
        iio_set_err(err, err_cap, "Reading JPEG Stream");
        return IIO_ERR_OOM;
    }

    /* Establish the setjmp return context for iio_error_exit to use. */
    if (setjmp(d->jerr.setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while reading. */
        status = iio_error_status(d, err, err_cap);
        free(d->scanline);
        d->scanline = NULL;
        return status;
    }

    iio_read_scanlines(d, report_progress, dst);

    free(d->scanline);
    d->scanline = NULL;
    return IIO_OK;
}

IIO_EXPORT void
iio_dispose(void* handle) {
    IioDecoder* d = (IioDecoder*) handle;
    if (d == NULL) {
        return;
    }
    /* Safe on a decoder whose jpeg_create_decompress never completed: mem is NULL then. */
    jpeg_destroy_decompress(&d->cinfo);
    free(d->scanline);
    free(d->icc_data);
    free(d->buf);
    free(d);
}
