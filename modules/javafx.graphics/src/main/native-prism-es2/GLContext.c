/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "PrismES2Defs.h"

void printGLError(GLenum errCode) {
    char const glCString[] = "*** GLError Code = ";
    switch (errCode) {
        case GL_NO_ERROR:
            //fprintf(stderr, "%sGL_NO_ERROR\n", glCString);
            break;
        case GL_INVALID_ENUM:
            fprintf(stderr, "%sGL_INVALID_ENUM\n", glCString);
            break;
        case GL_INVALID_VALUE:
            fprintf(stderr, "%sGL_INVALID_VALUE\n", glCString);
            break;
        case GL_INVALID_OPERATION:
            fprintf(stderr, "%sGL_INVALID_OPERATION\n", glCString);
            break;
        case GL_STACK_OVERFLOW:
            fprintf(stderr, "%sGL_STACK_OVERFLOW\n", glCString);
            break;
        case GL_STACK_UNDERFLOW:
            fprintf(stderr, "%sGL_STACK_UNDERFLOW\n", glCString);
            break;
        case GL_OUT_OF_MEMORY:
            fprintf(stderr, "%sGL_OUT_OF_MEMORY\n", glCString);
            break;
        default:
            fprintf(stderr, "%s*** UNKNOWN ERROR CODE ***\n", glCString);
    }
}

void initializeCtxInfo(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }
    // Initialize structure to all zeros
    memset(ctxInfo, 0, sizeof (ContextInfo));
}

void deleteCtxInfo(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }

    if (ctxInfo->versionStr != NULL) {
        free(ctxInfo->versionStr);
    }
    if (ctxInfo->vendorStr != NULL) {
        free(ctxInfo->vendorStr);
    }
    if (ctxInfo->rendererStr != NULL) {
        free(ctxInfo->rendererStr);
    }
    if (ctxInfo->glExtensionStr != NULL) {
        free(ctxInfo->glExtensionStr);
    }

#ifdef WIN32 /* WIN32 */
    if (ctxInfo->wglExtensionStr != NULL) {
        free(ctxInfo->wglExtensionStr);
    }
    if (ctxInfo->hglrc != NULL) {
        wglDeleteContext(ctxInfo->hglrc);
        ctxInfo->hglrc = NULL;
    }
#endif

#if defined(UNIX) && !defined(IS_EGL)
    if (ctxInfo->glxExtensionStr != NULL) {
        free(ctxInfo->glxExtensionStr);
    }
    if (ctxInfo->context != NULL) {
        glXDestroyContext(ctxInfo->display, ctxInfo->context);
    }
#endif
    /* IS_EGL (Monocle): nothing native to destroy. The eglDestroyContext branch this function had
     * at commit 21d5a654f6 went with the EGL include: the context behind a ContextInfo of
     * es2_context_adopt is the caller's (Java's), and the member is always NULL there anyway. */
    // Initialize structure to all zeros
    memset(ctxInfo, 0, sizeof (ContextInfo));
}

void initState(ContextInfo *ctxInfo) {
    if (ctxInfo == NULL) {
        return;
    }

    glEnable(GL_BLEND);
    glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

    // initialize states and properties to
    // match cached states and properties

    // depthtest is set to false
    // Note: This state is cached in GLContext.java
    ctxInfo->state.depthWritesEnabled = GL_FALSE;
    glDepthMask(ctxInfo->state.depthWritesEnabled);
    glDisable(GL_DEPTH_TEST);

    if (ctxInfo->state.scissorEnabled) {
        ctxInfo->state.scissorEnabled = GL_FALSE;
        glDisable(GL_SCISSOR_TEST);
    }

    ctxInfo->state.clearColor[0] = 0.0;
    ctxInfo->state.clearColor[1] = 0.0;
    ctxInfo->state.clearColor[2] = 0.0;
    ctxInfo->state.clearColor[3] = 0.0;
    glClearColor(ctxInfo->state.clearColor[0], ctxInfo->state.clearColor[1],
            ctxInfo->state.clearColor[2], ctxInfo->state.clearColor[3]);

    ctxInfo->vbFloatData = NULL;
    ctxInfo->vbByteData = NULL;
    ctxInfo->state.fillMode = GL_FILL;
    ctxInfo->state.cullEnable = GL_FALSE;
    ctxInfo->state.cullMode = GL_BACK;
    ctxInfo->state.fbo = 0;
}

void clearBuffers(ContextInfo *ctxInfo,
        GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha,
        GLboolean clearColor, GLboolean clearDepth, GLboolean ignoreScissor) {
    GLbitfield clearBIT = 0;

    if (ctxInfo == NULL) {
        return;
    }

    if (ignoreScissor && ctxInfo->state.scissorEnabled) {
        // glClear() honors the current scissor, so disable it
        // temporarily if ignoreScissor is true
        glDisable(GL_SCISSOR_TEST);
    }

    if (clearColor) {
        clearBIT = GL_COLOR_BUFFER_BIT;
        if ((ctxInfo->state.clearColor[0] != red)
                || (ctxInfo->state.clearColor[1] != green)
                || (ctxInfo->state.clearColor[2] != blue)
                || (ctxInfo->state.clearColor[3] != alpha)) {
            glClearColor(red, green, blue, alpha);
            ctxInfo->state.clearColor[0] = red;
            ctxInfo->state.clearColor[1] = green;
            ctxInfo->state.clearColor[2] = blue;
            ctxInfo->state.clearColor[3] = alpha;
        }
    }

    if (clearDepth) {
        clearBIT |= GL_DEPTH_BUFFER_BIT;
        // also make sure depth writes are enabled for the clear operation
        if (!ctxInfo->state.depthWritesEnabled) {
            glDepthMask(GL_TRUE);
        }
        glClear(clearBIT);
        if (!ctxInfo->state.depthWritesEnabled) {
            glDepthMask(GL_FALSE);
        }
    } else {
        glClear(clearBIT);
    }

    // restore previous state
    if (ignoreScissor && ctxInfo->state.scissorEnabled) {
        glEnable(GL_SCISSOR_TEST);
    }
}

void bindFBO(ContextInfo *ctxInfo, GLuint fboId) {
    if ((ctxInfo == NULL) || (ctxInfo->glBindFramebuffer == NULL)) {
        return;
    }
    ctxInfo->glBindFramebuffer(GL_FRAMEBUFFER, fboId);
    ctxInfo->state.fbo = fboId;
}

int checkFramebufferStatus(ContextInfo *ctxInfo) {
    GLenum status;
    status = ctxInfo->glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (status != GL_FRAMEBUFFER_COMPLETE) {
        switch(status) {
            case GL_FRAMEBUFFER_COMPLETE:
                return GL_FALSE;
                break;
            case GL_FRAMEBUFFER_UNSUPPORTED:
            //Choose different formats
                fprintf(stderr, "Framebuffer object format is unsupported by the video hardware. (GL_FRAMEBUFFER_UNSUPPORTED)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                fprintf(stderr, "Incomplete attachment. (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                fprintf(stderr, "Incomplete missing attachment. (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
                fprintf(stderr, "Incomplete dimensions. (GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
                fprintf(stderr, "Incomplete formats. (GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                fprintf(stderr, "Incomplete draw buffer. (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                fprintf(stderr, "Incomplete read buffer. (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER)(FBO - 820)\n");
                break;
            case GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                fprintf(stderr, "Incomplete multisample buffer. (GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE)(FBO - 820)\n");
                break;
            default:
                //Programming error; will fail on all hardware
                fprintf(stderr, "Some video driver error or programming error occurred. Framebuffer object status is invalid. (FBO - 823)\n");
                break;
        }
        return GL_TRUE;
    }
    return GL_FALSE;
}

GLuint attachRenderbuffer(ContextInfo *ctxInfo, GLuint rbID, GLenum attachment) {
    //GLenum status;
    ctxInfo->glFramebufferRenderbuffer(GL_FRAMEBUFFER, attachment,
            GL_RENDERBUFFER, rbID);
    ctxInfo->glBindRenderbuffer(GL_RENDERBUFFER, 0);
    //status = ctxInfo->glCheckFramebufferStatus(GL_FRAMEBUFFER);
    if (checkFramebufferStatus(ctxInfo)) {
        ctxInfo->glDeleteRenderbuffers(1, &rbID);
        rbID = 0;
        fprintf(stderr, "Error creating render buffer object %d\n", rbID);
    } else {
        // explicitly clear the render buffers, since it may contain
        // garbage after initialization
        clearBuffers(ctxInfo, 0, 0, 0, 0, GL_FALSE, GL_TRUE, GL_TRUE);
    }
    return rbID;
}

GLuint createAndAttachRenderBuffer(ContextInfo *ctxInfo, GLsizei width, GLsizei height, GLsizei msaa, GLenum attachment) {
    GLuint rbID;
    GLenum internalFormat;

    if ((ctxInfo == NULL) || (ctxInfo->glGenRenderbuffers == NULL)
            || (ctxInfo->glBindRenderbuffer == NULL)
            || (ctxInfo->glRenderbufferStorage == NULL)
            || (ctxInfo->glFramebufferRenderbuffer == NULL)
#ifndef IS_EGL
            || (ctxInfo->glRenderbufferStorageMultisample == NULL)
#endif
            || (ctxInfo->glCheckFramebufferStatus == NULL)
            || (ctxInfo->glDeleteRenderbuffers == NULL)) {
        return 0;
    }

    if (attachment == GL_DEPTH_ATTACHMENT) {
#ifdef IS_EGL
        internalFormat = GL_DEPTH_COMPONENT16;
#else
        internalFormat = GL_DEPTH_COMPONENT;
#endif
    } else {
        internalFormat = GL_RGBA8; //TODO verify format on RGBA or RGBA8
    }
    // create a depth buffer
    ctxInfo->glGenRenderbuffers(1, &rbID);
    ctxInfo->glBindRenderbuffer(GL_RENDERBUFFER, rbID);
#ifdef IS_EGL
    ctxInfo->glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
#else
    if (msaa) {
        ctxInfo->glRenderbufferStorageMultisample(GL_RENDERBUFFER, msaa, internalFormat, width, height);
    } else {
        ctxInfo->glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, width, height);
    }
#endif
    return attachRenderbuffer(ctxInfo, rbID, attachment);
}



void setCullMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo) {
    if (mvInfo->cullEnable != ctxInfo->state.cullEnable) {
        if (mvInfo->cullEnable) {
            glEnable(GL_CULL_FACE);
        } else {
            glDisable(GL_CULL_FACE);
        }
        ctxInfo->state.cullEnable = mvInfo->cullEnable;
    }

    if (mvInfo->cullMode != ctxInfo->state.cullMode) {
        glCullFace(mvInfo->cullMode);
        ctxInfo->state.cullMode = mvInfo->cullMode;
    }
}

void setPolyonMode(ContextInfo *ctxInfo, MeshViewInfo *mvInfo) {
#ifndef IS_EGL
    if (mvInfo->fillMode != ctxInfo->state.fillMode) {
        glPolygonMode(GL_FRONT_AND_BACK, mvInfo->fillMode);
        ctxInfo->state.fillMode = mvInfo->fillMode;
    }
#endif
}


