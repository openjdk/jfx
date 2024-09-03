/*
 * Copyright (c) 2009, 2024, Oracle and/or its affiliates. All rights reserved.
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
#include <stdlib.h>
#include <setjmp.h>
#include <assert.h>
#include <string.h>

#include "jni.h"

#include "com_sun_javafx_iio_jpeg_JPEGImageLoader.h"

/* headers from libjpeg */
#include <jpeglib.h>
#include <jerror.h>

#if defined (_LP64) || defined(_WIN64)
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

#ifdef __APPLE__

#include <TargetConditionals.h>

/* RT-37125: use setjmp/longjmp versions that do not save/restore the signal mask */
#define longjmp _longjmp
#define setjmp _setjmp

#endif

static jboolean checkAndClearException(JNIEnv *env) {
    if (!(*env)->ExceptionCheck(env)) {
        return JNI_FALSE;
    }
    (*env)->ExceptionClear(env);
    return JNI_TRUE;
}

/***************** Begin verbatim copy from jni_util.c ***************/

/**
 * Throw a Java exception by name. Similar to SignalError.
 */
JNIEXPORT void JNICALL
ThrowByName(JNIEnv *env, const char *name, const char *msg) {
    jclass cls = (*env)->FindClass(env, name);
    if (!(*env)->ExceptionCheck(env) && cls != 0) {/* Otherwise an exception has already been thrown */
        (*env)->ThrowNew(env, cls, msg);
    }
}

JNIEXPORT void * JNICALL
GetEnv(JavaVM *vm, jint version) {
    void *env;
    (*vm)->GetEnv(vm, &env, version);
    return env;
}

/***************** Begin verbatim copy from jni_util.c ***************/

#undef MAX
#define MAX(a,b)        ((a) > (b) ? (a) : (b))

/* Cached Java method IDs */
static jmethodID InputStream_readID;
static jmethodID InputStream_skipID;
static jmethodID JPEGImageLoader_setInputAttributesID;
static jmethodID JPEGImageLoader_setOutputAttributesID;
static jmethodID JPEGImageLoader_updateImageProgressID;
static jmethodID JPEGImageLoader_emitWarningID;

/* Initialize the Java VM instance variable when the library is
   first loaded */
static JavaVM *jvm;

#ifdef STATIC_BUILD

JNIEXPORT jint JNICALL
JNI_OnLoad_javafx_iio(JavaVM *vm, void *reserved) {
    jvm = vm;
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
            return JNI_VERSION_1_2;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_2;
#endif
}

#else

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    jvm = vm;
    return JNI_VERSION_1_2;
}

#endif // STATIC_BUILD


/*
 * The following sets of defines must match the warning messages in the
 * Java code.
 */

/* Loader warnings */
#define READ_NO_EOI          0

/* Saver warnings */

/* Return codes for various ops */
#define OK     1
#define NOT_OK 0

/*
 * First we define two objects, one for the stream and buffer and one
 * for pixels.  Both contain references to Java objects and pointers to
 * pinned arrays.  These objects can be used for either input or
 * output.  Pixels can be accessed as either INT32s or bytes.
 * Every I/O operation will have one of each these objects, one for
 * the stream and the other to hold pixels, regardless of the I/O direction.
 */

/******************** StreamBuffer definition ************************/

typedef struct streamBufferStruct {
    jobject stream; // ImageInputStream or ImageOutputStream
    jbyteArray hstreamBuffer; // Handle to a Java buffer for the stream
    JOCTET *buf; // Pinned buffer pointer */
    int bufferOffset; // holds offset between unpin and the next pin
    int bufferLength; // Allocated, nut just used
    int suspendable; // Set to true to suspend input
    long remaining_skip; // Used only on input
} streamBuffer, *streamBufferPtr;

/*
 * This buffer size was set to 64K in the old classes, 4K by default in the
 * IJG library, with the comment "an efficiently freadable size", and 1K
 * in AWT.
 * Unlike in the other Java designs, these objects will persist, so 64K
 * seems too big and 1K seems too small.  If 4K was good enough for the
 * IJG folks, it's good enough for me.
 */
#define STREAMBUF_SIZE 4096

/*
 * Used to signal that no data need be restored from an unpin to a pin.
 * I.e. the buffer is empty.
 */
#define NO_DATA -1

// Forward reference
static void resetStreamBuffer(JNIEnv *env, streamBufferPtr sb);

/*
 * Initialize a freshly allocated StreamBuffer object.  The stream is left
 * null, as it will be set from Java by setSource, but the buffer object
 * is created and a global reference kept.  Returns OK on success, NOT_OK
 * if allocating the buffer or getting a global reference for it failed.
 */
static int initStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    /* Initialize a new buffer */
    jbyteArray hInputBuffer = (*env)->NewByteArray(env, STREAMBUF_SIZE);
    if (hInputBuffer == NULL) {
        return NOT_OK;
    }
    sb->bufferLength = (*env)->GetArrayLength(env, hInputBuffer);
    sb->hstreamBuffer = (*env)->NewGlobalRef(env, hInputBuffer);
    if (sb->hstreamBuffer == NULL) {
        return NOT_OK;
    }


    sb->stream = NULL;

    sb->buf = NULL;

    resetStreamBuffer(env, sb);

    return OK;
}

/*
 * Free all resources associated with this streamBuffer.  This must
 * be called to dispose the object to avoid leaking global references, as
 * resetStreamBuffer does not release the buffer reference.
 */
static void destroyStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    resetStreamBuffer(env, sb);
    if (sb->hstreamBuffer != NULL) {
        (*env)->DeleteGlobalRef(env, sb->hstreamBuffer);
    }
}

// Forward reference
static void unpinStreamBuffer(JNIEnv *env,
        streamBufferPtr sb,
        const JOCTET *next_byte);

/*
 * Resets the state of a streamBuffer object that has been in use.
 * The global reference to the stream is released, but the reference
 * to the buffer is retained.  The buffer is unpinned if it was pinned.
 * All other state is reset.
 */
static void resetStreamBuffer(JNIEnv *env, streamBufferPtr sb) {
    if (sb->stream != NULL) {
        (*env)->DeleteGlobalRef(env, sb->stream);
        sb->stream = NULL;
    }
    unpinStreamBuffer(env, sb, NULL);
    sb->bufferOffset = NO_DATA;
    sb->suspendable = FALSE;
    sb->remaining_skip = 0;
}

/*
 * Pins the data buffer associated with this stream.  Returns OK on
 * success, NOT_OK on failure, as GetPrimitiveArrayCritical may fail.
 */
static int pinStreamBuffer(JNIEnv *env,
        streamBufferPtr sb,
        const JOCTET **next_byte) {
    if (sb->hstreamBuffer != NULL) {
        assert(sb->buf == NULL);
        sb->buf =
                (JOCTET *) (*env)->GetPrimitiveArrayCritical(env,
                sb->hstreamBuffer,
                NULL);
        if (sb->buf == NULL) {
            return NOT_OK;
        }
        if (sb->bufferOffset != NO_DATA) {
            *next_byte = sb->buf + sb->bufferOffset;
        }
    }
    return OK;
}

/*
 * Unpins the data buffer associated with this stream.
 */
static void unpinStreamBuffer(JNIEnv *env,
        streamBufferPtr sb,
        const JOCTET *next_byte) {
    if (sb->buf != NULL) {
        assert(sb->hstreamBuffer != NULL);
        if (next_byte == NULL) {
            sb->bufferOffset = NO_DATA;
        } else {
            sb->bufferOffset = next_byte - sb->buf;
        }
        (*env)->ReleasePrimitiveArrayCritical(env,
                sb->hstreamBuffer,
                sb->buf,
                0);
        sb->buf = NULL;
    }
}

/*
 * Clear out the streamBuffer.  This just invalidates the data in the buffer.
 */
static void clearStreamBuffer(streamBufferPtr sb) {
    sb->bufferOffset = NO_DATA;
}

/*************************** end StreamBuffer definition *************/

/*************************** Pixel Buffer definition ******************/

typedef struct pixelBufferStruct {
    jobject hpixelObject; // Usually a DataBuffer bank as a byte array

    union pixptr {
        INT32 *ip; // Pinned buffer pointer, as 32-bit ints
        unsigned char *bp; // Pinned buffer pointer, as bytes
    } buf;
} pixelBuffer, *pixelBufferPtr;

/*
 * Initialize a freshly allocated PixelBuffer.  All fields are simply
 * set to NULL, as we have no idea what size buffer we will need.
 */
static void initPixelBuffer(pixelBufferPtr pb) {
    pb->hpixelObject = NULL;
    pb->buf.ip = NULL;
}

/*
 * Set the pixelBuffer to use the given buffer, acquiring a new global
 * reference for it.  Returns OK on success, NOT_OK on failure.
 */
static int setPixelBuffer(JNIEnv *env, pixelBufferPtr pb, jobject obj) {
    pb->hpixelObject = (*env)->NewGlobalRef(env, obj);

    if (pb->hpixelObject == NULL) {
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Setting Pixel Buffer");
        return NOT_OK;
    }
    return OK;
}

// Forward reference
static void unpinPixelBuffer(JNIEnv *env, pixelBufferPtr pb);

/*
 * Resets a pixel buffer to its initial state.  Unpins any pixel buffer,
 * releases the global reference, and resets fields to NULL.  Use this
 * method to dispose the object as well (there is no destroyPixelBuffer).
 */
static void resetPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {
    if (pb->hpixelObject != NULL) {
        unpinPixelBuffer(env, pb);
        (*env)->DeleteGlobalRef(env, pb->hpixelObject);
        pb->hpixelObject = NULL;
    }
}

/*
 * Pins the data buffer.  Returns OK on success, NOT_OK on failure.
 */
static int pinPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {
    if (pb->hpixelObject != NULL) {
        assert(pb->buf.ip == NULL);
        pb->buf.bp = (unsigned char *) (*env)->GetPrimitiveArrayCritical
                (env, pb->hpixelObject, NULL);
        if (pb->buf.bp == NULL) {
            return NOT_OK;
        }
    }
    return OK;
}

/*
 * Unpins the data buffer.
 */
static void unpinPixelBuffer(JNIEnv *env, pixelBufferPtr pb) {

    if (pb->buf.ip != NULL) {
        assert(pb->hpixelObject != NULL);
        (*env)->ReleasePrimitiveArrayCritical(env,
                pb->hpixelObject,
                pb->buf.ip,
                0);
        pb->buf.ip = NULL;
    }
}

/********************* end PixelBuffer definition *******************/

/********************* ImageIOData definition ***********************/

#define MAX_BANDS 4
#define JPEG_BAND_SIZE 8
#define NUM_BAND_VALUES (1<<JPEG_BAND_SIZE)
#define MAX_JPEG_BAND_VALUE (NUM_BAND_VALUES-1)
#define HALF_MAX_JPEG_BAND_VALUE (MAX_JPEG_BAND_VALUE>>1)

/* The number of possible incoming values to be scaled. */
#define NUM_INPUT_VALUES (1 << 16)

/*
 * The principal imageioData object, opaque to I/O direction.
 * Each JPEGImageReader will have associated with it a
 * jpeg_decompress_struct, and similarly each JPEGImageWriter will
 * have associated with it a jpeg_compress_struct.  In order to
 * ensure that these associations persist from one native call to
 * the next, and to provide a central locus of imageio-specific
 * data, we define an imageioData struct containing references
 * to the Java object and the IJG structs.  The functions
 * that manipulate these objects know whether input or output is being
 * performed and therefore know how to manipulate the contents correctly.
 * If for some reason they don't, the direction can be determined by
 * checking the is_decompressor field of the jpegObj.
 * In order for lower level code to determine a
 * Java object given an IJG struct, such as for dispatching warnings,
 * we use the client_data field of the jpeg object to store a pointer
 * to the imageIOData object.  Maintenance of this pointer is performed
 * exclusively within the following access functions.  If you
 * change that, you run the risk of dangling pointers.
 */
typedef struct imageIODataStruct {
    j_common_ptr jpegObj; // Either struct is fine
    jobject imageIOobj; // A JPEGImageLoader

    streamBuffer streamBuf; // Buffer for the stream
    pixelBuffer pixelBuf; // Buffer for pixels

    jboolean abortFlag; // Passed down from Java abort method
} imageIOData, *imageIODataPtr;

/*
 * Allocate and initialize a new imageIOData object to associate the
 * jpeg object and the Java object.  Returns a pointer to the new object
 * on success, NULL on failure.
 */
static imageIODataPtr initImageioData(JNIEnv *env,
        j_common_ptr cinfo,
        jobject obj) {
    imageIODataPtr data = (imageIODataPtr) malloc(sizeof (imageIOData));
    if (data == NULL) {
        return NULL;
    }

    data->jpegObj = cinfo;
    cinfo->client_data = data;

#ifdef DEBUG_IIO_JPEG
    printf("new structures: data is %p, cinfo is %p\n", data, cinfo);
#endif

    data->imageIOobj = (*env)->NewWeakGlobalRef(env, obj);
    if (data->imageIOobj == NULL) {
        free(data);
        return NULL;
    }
    if (initStreamBuffer(env, &data->streamBuf) == NOT_OK) {
        (*env)->DeleteWeakGlobalRef(env, data->imageIOobj);
        free(data);
        return NULL;
    }
    initPixelBuffer(&data->pixelBuf);

    data->abortFlag = JNI_FALSE;

    return data;
}

/*
 * Resets the imageIOData object to its initial state, as though
 * it had just been allocated and initialized.
 */
static void resetImageIOData(JNIEnv *env, imageIODataPtr data) {
    resetStreamBuffer(env, &data->streamBuf);
    resetPixelBuffer(env, &data->pixelBuf);
    data->abortFlag = JNI_FALSE;
}

/*
 * Releases all resources held by this object and its subobjects,
 * frees the object, and returns the jpeg object.  This method must
 * be called to avoid leaking global references.
 * Note that the jpeg object is not freed or destroyed, as that is
 * the client's responsibility, although the client_data field is
 * cleared.
 */
static j_common_ptr destroyImageioData(JNIEnv *env, imageIODataPtr data) {
    j_common_ptr ret = data->jpegObj;
    (*env)->DeleteWeakGlobalRef(env, data->imageIOobj);
    destroyStreamBuffer(env, &data->streamBuf);
    resetPixelBuffer(env, &data->pixelBuf);
    ret->client_data = NULL;
    free(data);
    return ret;
}

/******************** end ImageIOData definition ***********************/

/******************** Java array pinning and unpinning *****************/

/* We use Get/ReleasePrimitiveArrayCritical functions to avoid
 * the need to copy array elements for the above two objects.
 *
 * MAKE SURE TO:
 *
 * - carefully insert pairs of RELEASE_ARRAYS and GET_ARRAYS around
 *   callbacks to Java.
 * - call RELEASE_ARRAYS before returning to Java.
 *
 * Otherwise things will go horribly wrong. There may be memory leaks,
 * excessive pinning, or even VM crashes!
 *
 * Note that GetPrimitiveArrayCritical may fail!
 */

/*
 * Release (unpin) all the arrays in use during a read.
 */
static void RELEASE_ARRAYS(JNIEnv *env, imageIODataPtr data, const JOCTET *next_byte) {
    unpinStreamBuffer(env, &data->streamBuf, next_byte);

    unpinPixelBuffer(env, &data->pixelBuf);

}

/*
 * Get (pin) all the arrays in use during a read.
 */
static int GET_ARRAYS(JNIEnv *env, imageIODataPtr data, const JOCTET **next_byte) {
    if (pinStreamBuffer(env, &data->streamBuf, next_byte) == NOT_OK) {
        return NOT_OK;
    }

    if (pinPixelBuffer(env, &data->pixelBuf) == NOT_OK) {
        RELEASE_ARRAYS(env, data, *next_byte);
        return NOT_OK;
    }
    return OK;
}

/****** end of Java array pinning and unpinning ***********/

/****** Error Handling *******/

/*
 * Set up error handling to use setjmp/longjmp.  This is the third such
 * setup, as both the AWT jpeg decoder and the com.sun... JPEG classes
 * setup thier own.  Ultimately these should be integrated, as they all
 * do pretty much the same thing.
 */

struct sun_jpeg_error_mgr {
    struct jpeg_error_mgr pub; /* "public" fields */

    jmp_buf setjmp_buffer; /* for return to caller */
};

typedef struct sun_jpeg_error_mgr * sun_jpeg_error_ptr;

/*
 * Here's the routine that will replace the standard error_exit method:
 */

METHODDEF(void)
sun_jpeg_error_exit(j_common_ptr cinfo) {
    /* cinfo->err really points to a sun_jpeg_error_mgr struct */
    sun_jpeg_error_ptr myerr = (sun_jpeg_error_ptr) cinfo->err;

    /* For Java, we will format the message and put it in the error we throw. */

    /* Return control to the setjmp point */
    longjmp(myerr->setjmp_buffer, 1);
}

/*
 * Error Message handling
 *
 * This overrides the output_message method to send JPEG messages
 *
 */

METHODDEF(void)
sun_jpeg_output_message(j_common_ptr cinfo) {
    char buffer[JMSG_LENGTH_MAX];
    jstring string;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    JNIEnv *env = (JNIEnv *) GetEnv(jvm, JNI_VERSION_1_2);
    jobject theObject;
    j_decompress_ptr dinfo;

    /* Create the message */
    (*cinfo->err->format_message) (cinfo, buffer);

    if (cinfo->is_decompressor) {
        dinfo = (j_decompress_ptr)cinfo;
        RELEASE_ARRAYS(env, data, dinfo->src->next_input_byte);
    }
    // Create a new java string from the message
    string = (*env)->NewStringUTF(env, buffer);

    theObject = data->imageIOobj;

    if (cinfo->is_decompressor) {
        (*env)->CallVoidMethod(env, theObject,
                JPEGImageLoader_emitWarningID,
                string);
        checkAndClearException(env);
        if (!GET_ARRAYS(env, data, &(dinfo->src->next_input_byte))) {
            cinfo->err->error_exit(cinfo);
        }
    }
}

/* End of verbatim copy from jpegdecoder.c */

/*************** end of error handling *********************/

/*************** Shared utility code ***********************/

static void imageio_set_stream(JNIEnv *env,
        j_common_ptr cinfo,
        imageIODataPtr data,
        jobject stream) {
    streamBufferPtr sb;
    sun_jpeg_error_ptr jerr;

    sb = &data->streamBuf;

    resetStreamBuffer(env, sb); // Removes any old stream

    /* Now we need a new global reference for the stream */
    if (stream != NULL) { // Fix for 4411955
        sb->stream = (*env)->NewGlobalRef(env, stream);
        if (sb->stream == NULL) {
            ThrowByName(env,
                    "java/lang/OutOfMemoryError",
                    "Setting Stream");
            return;
        }
    }

    /* And finally reset state */
    data->abortFlag = JNI_FALSE;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while aborting. */
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) (cinfo,
                    buffer);
            ThrowByName(env, "java/io/IOException", buffer);
        }
        return;
    }

    jpeg_abort(cinfo); // Frees any markers, but not tables

}

static void imageio_dispose(j_common_ptr info) {

    if (info != NULL) {
        if (info->is_decompressor) {
            j_decompress_ptr dinfo = (j_decompress_ptr) info;
            free(dinfo->src);
            dinfo->src = NULL;
        } else {
            j_compress_ptr cinfo = (j_compress_ptr) info;
            free(cinfo->dest);
            cinfo->dest = NULL;
        }
        jpeg_destroy(info);
        free(info->err);
        info->err = NULL;
        free(info);
    }
}

static void imageio_abort(JNIEnv *env, jobject this,
        imageIODataPtr data) {
    data->abortFlag = JNI_TRUE;
}

static void disposeIIO(JNIEnv *env, imageIODataPtr data) {
    j_common_ptr info = destroyImageioData(env, data);
    imageio_dispose(info);
}

/*************** end of shared utility code ****************/

/********************** Loader Support **************************/

/********************** Source Management ***********************/

/*
 * INPUT HANDLING:
 *
 * The JPEG library's input management is defined by the jpeg_source_mgr
 * structure which contains two fields to convey the information in the
 * buffer and 5 methods which perform all buffer management.  The library
 * defines a standard input manager that uses stdio for obtaining compressed
 * jpeg data, but here we need to use Java to get our data.
 *
 * We use the library jpeg_source_mgr but our own routines that access
 * imageio-specific information in the imageIOData structure.
 */

/*
 * Initialize source.  This is called by jpeg_read_header() before any
 * data is actually read.  Unlike init_destination(), it may leave
 * bytes_in_buffer set to 0 (in which case a fill_input_buffer() call
 * will occur immediately).
 */

GLOBAL(void)
imageio_init_source(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr *src = cinfo->src;
    src->next_input_byte = NULL;
    src->bytes_in_buffer = 0;
}

/*
 * This is called whenever bytes_in_buffer has reached zero and more
 * data is wanted.  In typical applications, it should read fresh data
 * into the buffer (ignoring the current state of next_input_byte and
 * bytes_in_buffer), reset the pointer & count to the start of the
 * buffer, and return TRUE indicating that the buffer has been reloaded.
 * It is not necessary to fill the buffer entirely, only to obtain at
 * least one more byte.  bytes_in_buffer MUST be set to a positive value
 * if TRUE is returned.  A FALSE return should only be used when I/O
 * suspension is desired (this mode is discussed in the next section).
 */

/*
 * Note that with I/O suspension turned on, this procedure should not
 * do any work since the JPEG library has a very simple backtracking
 * mechanism which relies on the fact that the buffer will be filled
 * only when it has backed out to the top application level.  When
 * suspendable is turned on, imageio_fill_suspended_buffer will
 * do the actual work of filling the buffer.
 */

GLOBAL(boolean)
imageio_fill_input_buffer(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *) GetEnv(jvm, JNI_VERSION_1_2);
    int ret;

    /* This is where input suspends */
    if (sb->suspendable) {
        return FALSE;
    }

#ifdef DEBUG_IIO_JPEG
    printf("Filling input buffer, remaining skip is %ld, ",
            sb->remaining_skip);
    printf("Buffer length is %d\n", sb->bufferLength);
#endif

    /*
     * Definitively skips.  Could be left over if we tried to skip
     * more than a buffer's worth but suspended when getting the next
     * buffer.  Now we aren't suspended, so we can catch up.
     */
    if (sb->remaining_skip) {
        src->skip_input_data(cinfo, 0);
    }

    /*
     * Now fill a complete buffer, or as much of one as the stream
     * will give us if we are near the end.
     */
    RELEASE_ARRAYS(env, data, src->next_input_byte);
    ret = (*env)->CallIntMethod(env,
            sb->stream,
            InputStream_readID,
            sb->hstreamBuffer, 0,
            sb->bufferLength);
    if (ret > sb->bufferLength) ret = sb->bufferLength;
    if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
        cinfo->err->error_exit((j_common_ptr) cinfo);
    }

#ifdef DEBUG_IIO_JPEG
    printf("Buffer filled. ret = %d\n", ret);
#endif
    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        jobject reader = data->imageIOobj;
#ifdef DEBUG_IIO_JPEG
        printf("YO! Early EOI! ret = %d\n", ret);
#endif
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env, reader,
                JPEGImageLoader_emitWarningID,
                READ_NO_EOI);
        if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }

        sb->buf[0] = (JOCTET) 0xFF;
        sb->buf[1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->next_input_byte = sb->buf;
    src->bytes_in_buffer = ret;

    return TRUE;
}

/*
 * With I/O suspension turned on, the JPEG library requires that all
 * buffer filling be done at the top application level, using this
 * function.  Due to the way that backtracking works, this procedure
 * saves all of the data that was left in the buffer when suspension
 * occured and read new data only at the end.
 */

GLOBAL(void)
imageio_fill_suspended_buffer(j_decompress_ptr cinfo) {
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *) GetEnv(jvm, JNI_VERSION_1_2);
    jint ret;
    int offset, buflen;

    /*
     * The original (jpegdecoder.c) had code here that called
     * InputStream.available and just returned if the number of bytes
     * available was less than any remaining skip.  Presumably this was
     * to avoid blocking, although the benefit was unclear, as no more
     * decompression can take place until more data is available, so
     * the code would block on input a little further along anyway.
     * ImageInputStreams don't have an available method, so we'll just
     * block in the skip if we have to.
     */

    if (sb->remaining_skip) {
        src->skip_input_data(cinfo, 0);
    }

    /* Save the data currently in the buffer */
    offset = src->bytes_in_buffer;
    if (src->next_input_byte > sb->buf) {
        memcpy(sb->buf, src->next_input_byte, offset);
    }
    RELEASE_ARRAYS(env, data, src->next_input_byte);
    buflen = sb->bufferLength - offset;
    if (buflen <= 0) {
        if (!GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        return;
    }

    ret = (*env)->CallIntMethod(env, sb->stream,
            InputStream_readID,
            sb->hstreamBuffer,
            offset, buflen);
    if (ret > buflen) ret = buflen;
    if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
        cinfo->err->error_exit((j_common_ptr) cinfo);
    }
    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        jobject reader = data->imageIOobj;
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env, reader,
                JPEGImageLoader_emitWarningID,
                READ_NO_EOI);
        if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }

        sb->buf[offset] = (JOCTET) 0xFF;
        sb->buf[offset + 1] = (JOCTET) JPEG_EOI;
        ret = 2;
    }

    src->next_input_byte = sb->buf;
    src->bytes_in_buffer = ret + offset;

    return;
}

/*
 * Skip num_bytes worth of data.  The buffer pointer and count are
 * advanced over num_bytes input bytes, using the input stream
 * skipBytes method if the skip is greater than the number of bytes
 * in the buffer.  This is used to skip over a potentially large amount of
 * uninteresting data (such as an APPn marker).  bytes_in_buffer will be
 * zero on return if the skip is larger than the current contents of the
 * buffer.
 *
 * A negative skip count is treated as a no-op.  A zero skip count
 * skips any remaining skip from a previous skip while suspended.
 *
 * Note that with I/O suspension turned on, this procedure does not
 * call skipBytes since the JPEG library has a very simple backtracking
 * mechanism which relies on the fact that the application level has
 * exclusive control over actual I/O.
 */

GLOBAL(void)
imageio_skip_input_data(j_decompress_ptr cinfo, long num_bytes) {
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    streamBufferPtr sb = &data->streamBuf;
    JNIEnv *env = (JNIEnv *) GetEnv(jvm, JNI_VERSION_1_2);
    jlong ret;
    jobject reader;

    if (num_bytes < 0) {
        return;
    }
    num_bytes += sb->remaining_skip;
    sb->remaining_skip = 0;

    /* First the easy case where we are skipping <= the current contents. */
    ret = src->bytes_in_buffer;
    if (ret >= num_bytes) {
        src->next_input_byte += num_bytes;
        src->bytes_in_buffer -= num_bytes;
        return;
    }

    /*
     * We are skipping more than is in the buffer.  We empty the buffer and,
     * if we aren't suspended, call the Java skipBytes method.  We always
     * leave the buffer empty, to be filled by either fill method above.
     */
    src->bytes_in_buffer = 0;
    src->next_input_byte = sb->buf;

    num_bytes -= (long) ret;
    if (sb->suspendable) {
        sb->remaining_skip = num_bytes;
        return;
    }

    RELEASE_ARRAYS(env, data, src->next_input_byte);
    ret = (*env)->CallLongMethod(env,
            sb->stream,
            InputStream_skipID,
            (jlong) num_bytes);
    if ((*env)->ExceptionOccurred(env)
            || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
        cinfo->err->error_exit((j_common_ptr) cinfo);
    }

    /*
     * If we have reached the end of the stream, then the EOI marker
     * is missing.  We accept such streams but generate a warning.
     * The image is likely to be corrupted, though everything through
     * the end of the last complete MCU should be usable.
     */
    if (ret <= 0) {
        reader = data->imageIOobj;
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        (*env)->CallVoidMethod(env,
                reader,
                JPEGImageLoader_emitWarningID,
                READ_NO_EOI);

        if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        sb->buf[0] = (JOCTET) 0xFF;
        sb->buf[1] = (JOCTET) JPEG_EOI;
        src->bytes_in_buffer = 2;
        src->next_input_byte = sb->buf;
    }
}

/*
 * Terminate source --- called by jpeg_finish_decompress() after all
 * data for an image has been read.  In our case pushes back any
 * remaining data, as it will be for another image and must be available
 * for java to find out that there is another image.  Also called if
 * reseting state after reading a tables-only image.
 */

GLOBAL(void)
imageio_term_source(j_decompress_ptr cinfo) {
    // To pushback, just seek back by src->bytes_in_buffer
    struct jpeg_source_mgr *src = cinfo->src;
    imageIODataPtr data = (imageIODataPtr) cinfo->client_data;
    JNIEnv *env = (JNIEnv *) GetEnv(jvm, JNI_VERSION_1_2);
    jobject reader = data->imageIOobj;
    if (src->bytes_in_buffer > 0) {
        RELEASE_ARRAYS(env, data, src->next_input_byte);

        if ((*env)->ExceptionOccurred(env)
                || !GET_ARRAYS(env, data, &(src->next_input_byte))) {
            cinfo->err->error_exit((j_common_ptr) cinfo);
        }
        src->bytes_in_buffer = 0;
        //src->next_input_byte = sb->buf;
    }
}

/********************* end of source manager ******************/

/********************* ICC profile support ********************/
/*
 * The following routines are modified versions of the ICC
 * profile support routines available from the IJG website.
 * The originals were written by Todd Newman
 * <tdn@eccentric.esd.sgi.com> and modified by Tom Lane for
 * the IJG.  They are further modified to fit in the context
 * of the imageio JPEG plug-in.
 */

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

#define ICC_MARKER  (JPEG_APP0 + 2)     /* JPEG marker code for ICC */
#define ICC_OVERHEAD_LEN  14            /* size of non-profile data in APP2 */
#define MAX_BYTES_IN_MARKER  65533      /* maximum data len of a JPEG marker */
#define MAX_DATA_BYTES_IN_ICC_MARKER  (MAX_BYTES_IN_MARKER - ICC_OVERHEAD_LEN)

/*
 * Handy subroutine to test whether a saved marker is an ICC profile marker.
 */

static boolean
marker_is_icc(jpeg_saved_marker_ptr marker) {
    return
    marker->marker == ICC_MARKER &&
            marker->data_length >= ICC_OVERHEAD_LEN &&
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
 * if so, reassemble and return the profile data as a new Java byte array.
 * If there was no ICC profile, return NULL.
 *
 * If the file contains invalid ICC APP2 markers, we throw an IIOException
 * with an appropriate message.
 */

jbyteArray
read_icc_profile(JNIEnv *env, j_decompress_ptr cinfo) {
    jpeg_saved_marker_ptr marker;
    int num_markers = 0;
    int num_found_markers = 0;
    int seq_no;
    JOCTET *icc_data;
    JOCTET *dst_ptr;
    unsigned int total_length;
#define MAX_SEQ_NO  255         // sufficient since marker numbers are bytes
    jpeg_saved_marker_ptr icc_markers[MAX_SEQ_NO + 1];
    int first; // index of the first marker in the icc_markers array
    int last; // index of the last marker in the icc_markers array
    jbyteArray data = NULL;

    /* This first pass over the saved markers discovers whether there are
     * any ICC markers and verifies the consistency of the marker numbering.
     */

    for (seq_no = 0; seq_no <= MAX_SEQ_NO; seq_no++)
        icc_markers[seq_no] = NULL;


    for (marker = cinfo->marker_list; marker != NULL; marker = marker->next) {
        if (marker_is_icc(marker)) {
            if (num_markers == 0)
                num_markers = GETJOCTET(marker->data[13]);
            else if (num_markers != GETJOCTET(marker->data[13])) {
                ThrowByName(env, "java/io/IOException",
                        "Invalid icc profile: inconsistent num_markers fields");
                return NULL;
            }
            seq_no = GETJOCTET(marker->data[12]);

            /* Some third-party tools produce images with profile chunk
             * numeration started from zero. It is inconsistent with ICC
             * spec, but seems to be recognized by majority of image
             * processing tools, so we should be more tolerant to this
             * departure from the spec.
             */
            if (seq_no < 0 || seq_no > num_markers) {
                ThrowByName(env, "java/io/IOException",
                        "Invalid icc profile: bad sequence number");
                return NULL;
            }
            if (icc_markers[seq_no] != NULL) {
                ThrowByName(env, "java/io/IOException",
                        "Invalid icc profile: duplicate sequence numbers");
                return NULL;
            }
            icc_markers[seq_no] = marker;
            num_found_markers++;
        }
    }

    if (num_markers == 0)
        return NULL; // There is no profile

    if (num_markers != num_found_markers) {
        ThrowByName(env, "java/io/IOException",
                "Invalid icc profile: invalid number of icc markers");
        return NULL;
    }

    first = icc_markers[0] ? 0 : 1;
    last = num_found_markers + first;

    /* Check for missing markers, count total space needed.
     */
    total_length = 0;
    for (seq_no = first; seq_no < last; seq_no++) {
        unsigned int length;
        if (icc_markers[seq_no] == NULL) {
            ThrowByName(env, "java/io/IOException",
                    "Invalid icc profile: missing sequence number");
            return NULL;
        }
        /* check the data length correctness */
        length = icc_markers[seq_no]->data_length;
        if (ICC_OVERHEAD_LEN > length || length > MAX_BYTES_IN_MARKER) {
            ThrowByName(env, "java/io/IOException",
                    "Invalid icc profile: invalid data length");
            return NULL;
        }
        total_length += (length - ICC_OVERHEAD_LEN);
    }

    if (total_length <= 0) {
        ThrowByName(env, "java/io/IOException",
                "Invalid icc profile: found only empty markers");
        return NULL;
    }

    /* Allocate a Java byte array for assembled data */

    data = (*env)->NewByteArray(env, total_length);
    if (data == NULL) {
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Reading ICC profile");
        return NULL;
    }

    icc_data = (JOCTET *) (*env)->GetPrimitiveArrayCritical(env,
            data,
            NULL);
    if (icc_data == NULL) {
        ThrowByName(env, "java/io/IOException",
                "Unable to pin icc profile data array");
        return NULL;
    }

    /* and fill it in */
    dst_ptr = icc_data;
    for (seq_no = first; seq_no < last; seq_no++) {
        JOCTET FAR *src_ptr = icc_markers[seq_no]->data + ICC_OVERHEAD_LEN;
        unsigned int length =
                icc_markers[seq_no]->data_length - ICC_OVERHEAD_LEN;

        memcpy(dst_ptr, src_ptr, length);
        dst_ptr += length;
    }

    /* finally, unpin the array */
    (*env)->ReleasePrimitiveArrayCritical(env,
            data,
            icc_data,
            0);


    return data;
}

/********************* end of ICC profile support *************/

/********************* Loader JNI calls ***********************/

JNIEXPORT void JNICALL Java_com_sun_javafx_iio_jpeg_JPEGImageLoader_initJPEGMethodIDs
(JNIEnv *env, jclass cls, jclass InputStreamClass) {
    // InputStream methods.
    InputStream_readID = (*env)->GetMethodID(env,
            InputStreamClass,
            "read",
            "([BII)I");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    InputStream_skipID = (*env)->GetMethodID(env,
            InputStreamClass,
            "skip",
            "(J)J");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    // JPEGImageLoader IDs.
    JPEGImageLoader_setInputAttributesID = (*env)->GetMethodID(env,
            cls,
            "setInputAttributes",
            "(IIIII[B)V");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    JPEGImageLoader_setOutputAttributesID = (*env)->GetMethodID(env,
            cls,
            "setOutputAttributes",
            "(II)V");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    JPEGImageLoader_updateImageProgressID = (*env)->GetMethodID(env,
            cls,
            "updateImageProgress",
            "(I)V");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

    JPEGImageLoader_emitWarningID = (*env)->GetMethodID(env,
            cls,
            "emitWarning",
            "(Ljava/lang/String;)V");
    if ((*env)->ExceptionCheck(env)) {
        return;
    }

}

JNIEXPORT void JNICALL Java_com_sun_javafx_iio_jpeg_JPEGImageLoader_disposeNative
(JNIEnv *env, jclass cls, jlong ptr) {
    imageIODataPtr data = (imageIODataPtr) jlong_to_ptr(ptr);
    disposeIIO(env, data);
}

#define JPEG_APP1  (JPEG_APP0 + 1)  /* EXIF APP1 marker code  */

/*
 * For EXIF images, the APP1 will appear immediately after the SOI,
 * so it's safe to only look at the first marker in the list.
 * (see http://www.exif.org/Exif2-2.PDF, section 4.7, page 58)
 */
#define IS_EXIF(c) \
    (((c)->marker_list != NULL) && ((c)->marker_list->marker == JPEG_APP1))

JNIEXPORT jlong JNICALL Java_com_sun_javafx_iio_jpeg_JPEGImageLoader_initDecompressor
(JNIEnv *env, jobject this, jobject stream) {
    imageIODataPtr data;
    struct sun_jpeg_error_mgr *jerr_mgr;

    /* This struct contains the JPEG decompression parameters and pointers to
     * working space (which is allocated as needed by the JPEG library).
     */
    struct jpeg_decompress_struct *cinfo;
    int jret;
    int h_samp0, h_samp1, h_samp2;
    int v_samp0, v_samp1, v_samp2;
    struct jpeg_source_mgr *src;
    sun_jpeg_error_ptr jerr;
    jbyteArray profileData = NULL;

    cinfo = malloc(sizeof (struct jpeg_decompress_struct));
    if (cinfo == NULL) {
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Initializing Reader");
        return 0;
    }

    /* We use our private extension JPEG error handler.
     */
    jerr_mgr = malloc(sizeof (struct sun_jpeg_error_mgr));
    if (jerr_mgr == NULL) {
        free(cinfo);
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Initializing Reader");
        return 0;
    }

    /* We set up the normal JPEG error routines, then override error_exit. */
    cinfo->err = jpeg_std_error(&(jerr_mgr->pub));
    jerr_mgr->pub.error_exit = sun_jpeg_error_exit;
    /* We need to setup our own print routines */
    jerr_mgr->pub.output_message = sun_jpeg_output_message;
    /* Now we can setjmp before every call to the library */

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    if (setjmp(jerr_mgr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error. */
        char buffer[JMSG_LENGTH_MAX];
        (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                buffer);
        free(cinfo->err);
        free(cinfo);
        ThrowByName(env, "java/io/IOException", buffer);
        return 0;
    }

    /* Perform library initialization */
    jpeg_create_decompress(cinfo);

    // Set up to keep any APP2 markers, as these might contain ICC profile
    // data
    jpeg_save_markers(cinfo, ICC_MARKER, 0xFFFF);

    /*
     * Now set up our source.
     */
    cinfo->src =
            (struct jpeg_source_mgr *) malloc(sizeof (struct jpeg_source_mgr));
    if (cinfo->src == NULL) {
        imageio_dispose((j_common_ptr) cinfo);
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Initializing Reader");
        return 0;
    }
    cinfo->src->bytes_in_buffer = 0;
    cinfo->src->next_input_byte = NULL;
    cinfo->src->init_source = imageio_init_source;
    cinfo->src->fill_input_buffer = imageio_fill_input_buffer;
    cinfo->src->skip_input_data = imageio_skip_input_data;
    cinfo->src->resync_to_restart = jpeg_resync_to_restart; // use default
    cinfo->src->term_source = imageio_term_source;

    /* set up the association to persist for future calls */
    data = initImageioData(env, (j_common_ptr) cinfo, this);
    if (data == NULL) {
        imageio_dispose((j_common_ptr) cinfo);
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Initializing Reader");
        return 0;
    }

    imageio_set_stream(env, (j_common_ptr) cinfo, data, stream);

    if ((*env)->ExceptionCheck(env)) {
        disposeIIO(env, data);
        return 0;
    }

    imageio_init_source((j_decompress_ptr) cinfo);

    src = cinfo->src;
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while reading the header. */
        RELEASE_ARRAYS(env, data, src->next_input_byte);
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                    buffer);
            ThrowByName(env, "java/io/IOException", buffer);
        }
        disposeIIO(env, data);
        return 0;
    }

    if (GET_ARRAYS(env, data, &src->next_input_byte) == NOT_OK) {
        ThrowByName(env,
                "java/io/IOException",
                "Array pin failed");
        disposeIIO(env, data);
        return 0;
    }

    jret = jpeg_read_header(cinfo, FALSE);

    if (jret == JPEG_HEADER_TABLES_ONLY) {
        imageio_term_source(cinfo); // Pushback remaining buffer contents
#ifdef DEBUG_IIO_JPEG
        printf("just read tables-only image; q table 0 at %p\n",
                cinfo->quant_tbl_ptrs[0]);
#endif
        RELEASE_ARRAYS(env, data, src->next_input_byte);
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
#ifdef YCCALPHA
            case JCS_YCC:
                cinfo->out_color_space = JCS_YCC;
                break;
#endif
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

                if ((h_samp1 > h_samp0) && (h_samp2 > h_samp0) ||
                        (v_samp1 > v_samp0) && (v_samp2 > v_samp0)) {
                    cinfo->jpeg_color_space = JCS_YCCK;
                    /* Leave the output space as CMYK */
                }

                /* There is no support for CMYK on jfx side, so request RGB output */
                cinfo->out_color_space = JCS_RGB;
        }
        RELEASE_ARRAYS(env, data, src->next_input_byte);

        /* read icc profile data */
        profileData = read_icc_profile(env, cinfo);

        if ((*env)->ExceptionCheck(env)) {
            disposeIIO(env, data);
            return 0;
        }

        (*env)->CallVoidMethod(env, this,
                JPEGImageLoader_setInputAttributesID,
                cinfo->image_width,
                cinfo->image_height,
                cinfo->jpeg_color_space,
                cinfo->out_color_space,
                cinfo->num_components,
                profileData);
        if ((*env)->ExceptionCheck(env)) {
            disposeIIO(env, data);
            return 0;
        }
    }

    return ptr_to_jlong(data);
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_iio_jpeg_JPEGImageLoader_startDecompression
(JNIEnv *env, jobject this, jlong ptr, jint outCS, jint dest_width, jint dest_height) {
    imageIODataPtr data = (imageIODataPtr) jlong_to_ptr(ptr);
    j_decompress_ptr cinfo = (j_decompress_ptr) data->jpegObj;
    struct jpeg_source_mgr *src = cinfo->src;
    sun_jpeg_error_ptr jerr;

    jfloat x_scale;
    jfloat y_scale;
    jfloat max_scale;

    if (GET_ARRAYS(env, data, &cinfo->src->next_input_byte) == NOT_OK) {
        ThrowByName(env,
                "java/io/IOException",
                "Array pin failed");
        return JCS_UNKNOWN;
    }

    cinfo = (j_decompress_ptr) data->jpegObj;

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while initializing compression. */
        RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                    buffer);
            ThrowByName(env, "java/io/IOException", buffer);
        }
        return JCS_UNKNOWN;
    }

    cinfo->out_color_space = outCS;

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

    x_scale = (jfloat) dest_width / (jfloat) cinfo->image_width;
    y_scale = (jfloat) dest_height / (jfloat) cinfo->image_height;
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

    RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
    (*env)->CallVoidMethod(env, this,
            JPEGImageLoader_setOutputAttributesID,
            cinfo->output_width,
            cinfo->output_height);

    return cinfo->output_components;
}

#define SAFE_TO_MULT(a, b) (((a) > 0) && ((b) >= 0) && ((0x7fffffff / (a)) > (b)))
#define SAFE_FREE(PTR)  \
    if ((PTR) != NULL) {  \
        free(PTR);     \
        (PTR) = NULL;     \
    }

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_iio_jpeg_JPEGImageLoader_decompressIndirect
(JNIEnv *env, jobject this, jlong ptr, jboolean report_progress, jbyteArray barray) {
    imageIODataPtr data = (imageIODataPtr) jlong_to_ptr(ptr);
    j_decompress_ptr cinfo = (j_decompress_ptr) data->jpegObj;
    struct jpeg_source_mgr *src = cinfo->src;
    sun_jpeg_error_ptr jerr;
    int bytes_per_row = cinfo->output_width * cinfo->output_components;
    int offset = 0;
    JSAMPROW scanline_ptr = NULL;

    if (!SAFE_TO_MULT(cinfo->output_width, cinfo->output_components) ||
        !SAFE_TO_MULT(bytes_per_row, cinfo->output_height) ||
        ((*env)->GetArrayLength(env, barray) <
         (bytes_per_row * cinfo->output_height)))
     {
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Reading JPEG Stream");
        return JNI_FALSE;
    }

    if (GET_ARRAYS(env, data, &cinfo->src->next_input_byte) == NOT_OK) {
        ThrowByName(env,
                "java/io/IOException",
                "Array pin failed");
        return JNI_FALSE;
    }

    /* Establish the setjmp return context for sun_jpeg_error_exit to use. */
    jerr = (sun_jpeg_error_ptr) cinfo->err;

    if (setjmp(jerr->setjmp_buffer)) {
        /* If we get here, the JPEG code has signaled an error
           while reading. */
        if (!(*env)->ExceptionOccurred(env)) {
            char buffer[JMSG_LENGTH_MAX];
            (*cinfo->err->format_message) ((struct jpeg_common_struct *) cinfo,
                    buffer);
            ThrowByName(env, "java/io/IOException", buffer);
        }
        SAFE_FREE(scanline_ptr);
        RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
        return JNI_FALSE;
    }

    scanline_ptr = (JSAMPROW) malloc(bytes_per_row * sizeof(JSAMPLE));
    if (scanline_ptr == NULL) {
        RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
        ThrowByName(env,
                "java/lang/OutOfMemoryError",
                "Reading JPEG Stream");
        return JNI_FALSE;
    }

    while (cinfo->output_scanline < cinfo->output_height) {
        int num_scanlines;
        if (report_progress == JNI_TRUE) {
            RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
            (*env)->CallVoidMethod(env, this,
                    JPEGImageLoader_updateImageProgressID,
                    cinfo->output_scanline);
            if ((*env)->ExceptionCheck(env)) {
                SAFE_FREE(scanline_ptr);
                return JNI_FALSE;
            }
            if (GET_ARRAYS(env, data, &cinfo->src->next_input_byte) == NOT_OK) {
                SAFE_FREE(scanline_ptr);
                ThrowByName(env,
                          "java/io/IOException",
                          "Array pin failed");
                return JNI_FALSE;
            }
        }

        num_scanlines = jpeg_read_scanlines(cinfo, &scanline_ptr, 1);
        if (num_scanlines == 1) {
            jbyte *body = (*env)->GetPrimitiveArrayCritical(env, barray, NULL);
            if (body == NULL) {
                RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
                fprintf(stderr, "decompressIndirect: GetPrimitiveArrayCritical returns NULL: out of memory\n");
                SAFE_FREE(scanline_ptr);
                return JNI_FALSE;
            }
            memcpy(body+offset,scanline_ptr, bytes_per_row);
            (*env)->ReleasePrimitiveArrayCritical(env, barray, body, JNI_ABORT);
            offset += bytes_per_row;
        }
    }
    SAFE_FREE(scanline_ptr);

    if (report_progress == JNI_TRUE) {
        RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
        (*env)->CallVoidMethod(env, this,
                JPEGImageLoader_updateImageProgressID,
                cinfo->output_height);
        if ((*env)->ExceptionCheck(env)) {
            return JNI_FALSE;
        }
        if (GET_ARRAYS(env, data, &cinfo->src->next_input_byte) == NOT_OK) {
            ThrowByName(env,
                "java/io/IOException",
                "Array pin failed");
            return JNI_FALSE;
        }
    }

    jpeg_finish_decompress(cinfo);

    RELEASE_ARRAYS(env, data, cinfo->src->next_input_byte);
    return JNI_TRUE;
}
