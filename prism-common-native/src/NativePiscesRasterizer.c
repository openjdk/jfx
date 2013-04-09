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

#include <jni.h>
#ifdef ANDROID_NDK
#include <linux/stddef.h>
#endif
#include "com_sun_prism_impl_shape_NativePiscesRasterizer.h"

#include "Renderer.h"
#include "Stroker.h"
#include "Dasher.h"
#include "Transformer.h"
#include "AlphaConsumer.h"

#define SEG(T) com_sun_prism_impl_shape_NativePiscesRasterizer_SEG_ ## T

#define SEG_MOVETO   SEG(MOVETO)
#define SEG_LINETO   SEG(LINETO)
#define SEG_QUADTO   SEG(QUADTO)
#define SEG_CUBICTO  SEG(CUBICTO)
#define SEG_CLOSE    SEG(CLOSE)

#define NPException    "java/lang/NullPointerException"
#define AIOOBException "java/lang/ArrayIndexOutOfBoundsException"
#define IError         "java/lang/InternalError"

#define CheckNPE(env, a)                                \
    do {                                                \
        if (a == NULL) {                                \
            Throw(env, NPException, #a);                \
            return;                                     \
        }                                               \
    } while (0)

#define CheckLen(env, a, len)                           \
    do {                                                \
        if ((*env)->GetArrayLength(env, a) < len) {     \
            Throw(env, AIOOBException, #a);             \
            return;                                     \
        }                                               \
    } while (0)


static void Throw(JNIEnv *env, char *throw_class_name, char *detail) {
    jclass throw_class = (*env)->FindClass(env, throw_class_name);
    if (throw_class != NULL) {
        (*env)->ThrowNew(env, throw_class, detail);
    }
}

static char * feedConsumer
    (JNIEnv *env, PathConsumer *consumer,
     jfloatArray coordsArray, jint coordSize,
     jbyteArray commandsArray, jint numCommands)
{
    char *failure = NULL;
    jfloat *coords;

    coords = (*env)->GetPrimitiveArrayCritical(env, coordsArray, 0);
    if (coords == NULL) {
        failure = "";
    } else {
        jbyte *commands = (*env)->GetPrimitiveArrayCritical(env, commandsArray, 0);
        if (commands == NULL) {
            failure = "";
        } else {
            jint cmdoff, coordoff = 0;
            for (cmdoff = 0; cmdoff < numCommands && failure == NULL; cmdoff++) {
                switch (commands[cmdoff]) {
                    case SEG_MOVETO:
                        if (coordoff + 2 > coordSize) {
                            failure = "[not enough coordinates for moveTo";
                        } else {
                            consumer->moveTo(consumer,
                                             coords[coordoff+0], coords[coordoff+1]);
                            coordoff += 2;
                        }
                        break;
                    case SEG_LINETO:
                        if (coordoff + 2 > coordSize) {
                            failure = "[not enough coordinates for lineTo";
                        } else {
                            consumer->lineTo(consumer,
                                             coords[coordoff+0], coords[coordoff+1]);
                            coordoff += 2;
                        }
                        break;
                    case SEG_QUADTO:
                        if (coordoff + 4 > coordSize) {
                            failure = "[not enough coordinates for quadTo";
                        } else {
                            consumer->quadTo(consumer,
                                             coords[coordoff+0], coords[coordoff+1],
                                             coords[coordoff+2], coords[coordoff+3]);
                            coordoff += 4;
                        }
                        break;
                    case SEG_CUBICTO:
                        if (coordoff + 6 > coordSize) {
                            failure = "[not enough coordinates for curveTo";
                        } else {
                            consumer->curveTo(consumer,
                                              coords[coordoff+0], coords[coordoff+1],
                                              coords[coordoff+2], coords[coordoff+3],
                                              coords[coordoff+4], coords[coordoff+5]);
                            coordoff += 6;
                        }
                        break;
                    case SEG_CLOSE:
                        consumer->closePath(consumer);
                        break;
                    default:
                        failure = "unrecognized Path segment";
                        break;
                }
            }
            (*env)->ReleasePrimitiveArrayCritical(env, commandsArray, commands, JNI_ABORT);
        }
        (*env)->ReleasePrimitiveArrayCritical(env, coordsArray, coords, JNI_ABORT);
        if (failure == NULL) {
            consumer->pathDone(consumer);
        }
    }
    return failure;
}

/*
 * Class:     com_sun_prism_impl_shape_NativePiscesRasterizer
 * Method:    init
 * Signature: (II)V
 */
JNIEXPORT void JNICALL
Java_com_sun_prism_impl_shape_NativePiscesRasterizer_init
    (JNIEnv *env, jclass klass,
     jint subpixelLgPositionsX, jint subpixelLgPositionsY)
{
    Renderer_setup(subpixelLgPositionsX, subpixelLgPositionsY);
}

/*
 * Class:     com_sun_prism_impl_shape_NativePiscesRasterizer
 * Method:    produceFillAlphas
 * Signature: ([F[BIZDDDDDD[I[B)V
 */
JNIEXPORT void JNICALL
Java_com_sun_prism_impl_shape_NativePiscesRasterizer_produceFillAlphas
    (JNIEnv *env, jclass klass,
     jfloatArray coordsArray, jbyteArray commandsArray, jint numCommands, jboolean nonzero,
     jdouble mxx, jdouble mxy, jdouble mxt, jdouble myx, jdouble myy, jdouble myt,
     jintArray boundsArray, jbyteArray maskArray)
{
    jint bounds[4];
    Transformer transformer;
    Renderer renderer;
    PathConsumer *consumer;
    char *failure;
    jint coordSize;

    CheckNPE(env, coordsArray);
    CheckNPE(env, commandsArray);
    CheckNPE(env, boundsArray);
    CheckNPE(env, maskArray);
    CheckLen(env, boundsArray, 4);
    CheckLen(env, commandsArray, numCommands);

    (*env)->GetIntArrayRegion(env, boundsArray, 0, 4, bounds);
    coordSize = (*env)->GetArrayLength(env, coordsArray);
    Renderer_init(&renderer);
    Renderer_reset(&renderer,
                   bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1],
                   nonzero ? WIND_NON_ZERO : WIND_EVEN_ODD);
    consumer = Transformer_init(&transformer, &renderer.consumer,
                                mxx, mxy, mxt, myx, myy, myt);
    failure = feedConsumer(env, consumer,
                           coordsArray, coordSize, commandsArray, numCommands);
    if (failure == NULL) {
        Renderer_getOutputBounds(&renderer, bounds);
        (*env)->SetIntArrayRegion(env, boundsArray, 0, 4, bounds);
        if (bounds[0] < bounds[2] && bounds[1] < bounds[3]) {
            AlphaConsumer ac = {
                bounds[0],
                bounds[1],
                bounds[2] - bounds[0],
                bounds[3] - bounds[1],
            };
            if ((*env)->GetArrayLength(env, maskArray) / ac.width < ac.height) {
                Throw(env, AIOOBException, "maskArray");
            } else {
                ac.alphas = (*env)->GetPrimitiveArrayCritical(env, maskArray, 0);
                if (ac.alphas != NULL) {
                    Renderer_produceAlphas(&renderer, &ac);
                    (*env)->ReleasePrimitiveArrayCritical(env, maskArray, ac.alphas, 0);
                }
            }
        }
    } else if (*failure != 0) {
        if (*failure == '[') {
            Throw(env, AIOOBException, failure + 1);
        } else {
            Throw(env, IError, failure);
        }
    }
    Renderer_destroy(&renderer);
}

/*
 * Class:     com_sun_prism_impl_shape_NativePiscesRasterizer
 * Method:    produceStrokeAlphas
 * Signature: ([F[BIFIIF[FFDDDDDD[I[B)V
 */
JNIEXPORT void JNICALL
Java_com_sun_prism_impl_shape_NativePiscesRasterizer_produceStrokeAlphas
    (JNIEnv *env, jclass klass,
     jfloatArray coordsArray, jbyteArray commandsArray, jint numCommands,
     jfloat linewidth, jint linecap, jint linejoin, jfloat miterlimit,
     jfloatArray dashArray, jfloat dashphase,
     jdouble mxx, jdouble mxy, jdouble mxt, jdouble myx, jdouble myy, jdouble myt,
     jintArray boundsArray, jbyteArray maskArray)
{
    jint bounds[4];
    Stroker stroker;
    Dasher dasher;
    Renderer renderer;
    Transformer transformer;
    PathConsumer *consumer;
    jint coordSize;
    jfloat *dashes;
    char *failure;

    CheckNPE(env, coordsArray);
    CheckNPE(env, commandsArray);
    CheckNPE(env, boundsArray);
    CheckNPE(env, maskArray);
    CheckLen(env, boundsArray, 4);
    CheckLen(env, commandsArray, numCommands);

    (*env)->GetIntArrayRegion(env, boundsArray, 0, 4, bounds);
    coordSize = (*env)->GetArrayLength(env, coordsArray);
    Renderer_init(&renderer);
    Renderer_reset(&renderer,
                   bounds[0], bounds[1], bounds[2] - bounds[0], bounds[3] - bounds[1],
                   WIND_NON_ZERO);
    consumer = Transformer_init(&transformer, &renderer.consumer,
                                mxx, mxy, mxt, myx, myy, myt);
    Stroker_init(&stroker, consumer, linewidth, linecap, linejoin, miterlimit);
    if (dashArray == NULL) {
        dashes = NULL;
        consumer = &stroker.consumer;
    } else {
        jint numdashes = (*env)->GetArrayLength(env, dashArray);
        dashes = (*env)->GetPrimitiveArrayCritical(env, dashArray, 0);
        if (dashes == NULL) {
            return;
        }
        Dasher_init(&dasher, &stroker.consumer, dashes, numdashes, dashphase);
        consumer = &dasher.consumer;
    }
    failure = feedConsumer(env, consumer,
                           coordsArray, coordSize, commandsArray, numCommands);
    if (dashArray != NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, dashArray, dashes, JNI_ABORT);
        Dasher_destroy(&dasher);
    }
    Stroker_destroy(&stroker);
    if (failure == NULL) {
        Renderer_getOutputBounds(&renderer, bounds);
        (*env)->SetIntArrayRegion(env, boundsArray, 0, 4, bounds);
        if (bounds[0] < bounds[2] && bounds[1] < bounds[3]) {
            AlphaConsumer ac = {
                bounds[0],
                bounds[1],
                bounds[2] - bounds[0],
                bounds[3] - bounds[1],
            };
            if ((*env)->GetArrayLength(env, maskArray) / ac.width < ac.height) {
                Throw(env, AIOOBException, "Mask");
            } else {
                ac.alphas = (*env)->GetPrimitiveArrayCritical(env, maskArray, 0);
                if (ac.alphas != NULL) {
                    Renderer_produceAlphas(&renderer, &ac);
                    (*env)->ReleasePrimitiveArrayCritical(env, maskArray, ac.alphas, 0);
                }
            }
        }
    } else if (*failure != 0) {
        if (*failure == '[') {
            Throw(env, AIOOBException, failure + 1);
        } else {
            Throw(env, IError, failure);
        }
    }
    Renderer_destroy(&renderer);
}
