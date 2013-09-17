/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#define _GNU_SOURCE
#include <errno.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

#if linux
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>
#endif

#if MACOSX
#include <OpenGL/gl.h>
#include <OpenGL/glext.h>
#endif

#include "os.h"
#include "iolib.h"
#include "map.h"

#define DIR_TEXTURES    "TEXTURES"

typedef void             GLvoid;
typedef char             GLchar;

extern const char *glEnum2str(GLenum);
static void readPixels();
static int  savePNG(char *fName, GLenum format, GLenum type, uint32_t width, uint32_t height, const void *data);

static int printFlag = 0;
static int printArrays = 0;
static int execFlag  = 0;
static int dumpTexturesFlag = 0;
static uint64_t printThread = (uint64_t)0;
static uint64_t curThread   = (uint64_t)0;
static int texCount = 0;

/*
 *    StringBuffer
 */
 
#define CHUNKSZ 2048
static char *stringBuffer;
static char *curPtr;
static char *endPtr;

void
sb_enlarge(int delta)
{
    delta += CHUNKSZ - 1;
    delta -= delta % CHUNKSZ;
    int nsize = (endPtr - stringBuffer) + delta;
    int curOffset = curPtr - stringBuffer;
    stringBuffer = realloc(stringBuffer, nsize);
    curPtr = stringBuffer + curOffset;
    endPtr = stringBuffer + nsize;
}

void
sb_appendInt(int val)
{
    if (curPtr + 16 > endPtr) sb_enlarge(16);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%d", val);
}

void
sb_appendLong(uint64_t val)
{
    if (curPtr + 32 > endPtr) sb_enlarge(32);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%llu", val);
}

void
sb_appendBool(int val)
{
    if (curPtr + 8 > endPtr) sb_enlarge(8);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%s", val ? "true" : "false");
}

void
sb_appendFloat(float val)
{
    if (curPtr + 16 > endPtr) sb_enlarge(16);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%f", val);
}

void
sb_appendPtr(const void *val)
{
    if (curPtr + 32 > endPtr) sb_enlarge(32);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%p", val);
}

void
sb_appendStr(const char *str)
{
    int len = strlen(str) + 1;
    if (curPtr + len > endPtr) sb_enlarge(len);
    curPtr += snprintf(curPtr, endPtr - curPtr, "%s", str);
}

void
sb_appendNL()
{
    if (curPtr + 2 > endPtr) sb_enlarge(2);
    *curPtr++ = '\n';
    *curPtr = (char)0;
}

void
sb_reset()
{
    curPtr = stringBuffer;
    *curPtr = (char)0;
}

void
sb_init()
{
    int size = CHUNKSZ;
    stringBuffer = malloc(size);
    curPtr = stringBuffer;
    endPtr = stringBuffer + size;
}

void
sb_fini()
{
    free(stringBuffer);
    stringBuffer = NULL;
    curPtr = NULL;
    endPtr = NULL;
}

/*
 *    FPS tracking
 */
#define INST_FRAME_COUNT 30
#define REPORT_INTERVAL 2000000000ULL

static int fpsFlag = 1;
static uint64_t ts0Recorded;
static uint64_t ts0Actual;
static uint64_t tsRecorded[INST_FRAME_COUNT];
static uint64_t tsActual[INST_FRAME_COUNT];
static int curFrame = 0;
static uint64_t nextReport;

static void
fps_newFrame(int frame, uint64_t ts)
{
    uint64_t actual;
    if (execFlag) {
        actual = gethrtime();
    }
    if (frame == 0) {
        ts0Recorded = ts;
        nextReport = ts + REPORT_INTERVAL;
        if (execFlag) {
            ts0Actual = actual;
        }
    }
    int idx = frame % INST_FRAME_COUNT;
    if (ts > nextReport) {
        uint64_t prev = frame < INST_FRAME_COUNT ? ts0Recorded : tsRecorded[idx];
        int num = frame < INST_FRAME_COUNT ? frame : INST_FRAME_COUNT;
        fprintf(stdout, "FPS(rec): %f", num*1e9/(ts - prev));
        if (execFlag) {
            prev = curFrame < INST_FRAME_COUNT ? ts0Actual : tsActual[idx];
            fprintf(stdout, "    FPS(act): %f", num*1e9/(actual - prev));
        }
        fprintf(stdout, "\n");
        nextReport += REPORT_INTERVAL;
    }
    tsRecorded[idx] = ts;
    if (execFlag) {
        tsActual[idx] = actual;
    }
}

static void
fps_total()
{
    if (--curFrame <= 0) return;
    
    int idx = curFrame % INST_FRAME_COUNT;
    fprintf(stdout, "Total FPS(rec): %f", curFrame*1e9/(tsRecorded[idx] - ts0Recorded));
    if (execFlag) {
        fprintf(stdout, "    Total FPS(act): %f", curFrame*1e9/(tsActual[idx] - ts0Actual));
    }
    fprintf(stdout, "\n");
}


/*
 *    Native window
 */

#ifdef RASPBERRYPI

#include  <bcm_host.h>
 
static EGL_DISPMANX_WINDOW_T nativewindow;
static DISPMANX_ELEMENT_HANDLE_T dispman_element;
static DISPMANX_DISPLAY_HANDLE_T dispman_display;
static DISPMANX_UPDATE_HANDLE_T dispman_update;

static EGLNativeWindowType 
createNativeWindow()
{
    bcm_host_init();

    uint32_t width, height;
    int rc = graphics_get_display_size(0, &width, &height);
    if (rc < 0) {
        fprintf(stderr, "FATAL: can't create native wondow\n");
    }
    
    VC_RECT_T dst_rect;
    VC_RECT_T src_rect;

    dst_rect.x = 0;
    dst_rect.y = 0;
    dst_rect.width = width;
    dst_rect.height = height;

    src_rect.x = 0;
    src_rect.y = 0;
    src_rect.width = width << 16;
    src_rect.height = height << 16;

    dispman_display = vc_dispmanx_display_open(0);
    dispman_update = vc_dispmanx_update_start(0);

    dispman_element = vc_dispmanx_element_add (dispman_update, dispman_display,
       1, &dst_rect, 0, &src_rect, DISPMANX_PROTECTION_NONE, 0, 0, 0);

    nativewindow.element = dispman_element;
    nativewindow.width = width;
    nativewindow.height = height;
    vc_dispmanx_update_submit_sync(dispman_update);

    return &nativewindow;
}

#elif BEAGLEBOARD

static EGLNativeWindowType 
createNativeWindow()
{
    return NULL;
}

#endif

/*
 *     Process recorded GL commands
 */

#define NOT_IMPLEMENTED() {fprintf(stderr, "FATAL: not implemented %d\n", cmd); return;}

/* XXX use glGet(GL_MAX_VERTEX_ATTRIBS) */
#define MAX_VERTEX_ATTRIBS      128     
typedef struct VertexAttrib_t {
    GLboolean   enabled;
    GLint       size;
    GLenum      type;
    GLboolean   normalized;
} VertexAttrib_t;

static VertexAttrib_t vertexAttrib[MAX_VERTEX_ATTRIBS];

static void
getVertexAttrib(int index, int count, const GLvoid *pointer)
{
    if (execFlag) {
        glVertexAttribPointer(index, vertexAttrib[index].size, vertexAttrib[index].type, vertexAttrib[index].normalized, 0, pointer);
    }
}

static GLuint arrayBufferBinding = 0;
static GLuint elementArrayBufferBinding = 0;

static void     *eglSurfaceMap = NULL;
static void     *eglContextMap = NULL;

static void
proc_glActiveTexture(GLenum texture)
{
    if (printFlag) {
        sb_appendStr("glActiveTexture(");
        sb_appendStr(glEnum2str(texture));
        sb_appendStr(")");
    }
    if (execFlag) {
        glActiveTexture(texture);
    }
}

static void
proc_glAttachShader(GLuint program, GLuint shader)
{
    if (printFlag) {
        sb_appendStr("glAttachShader(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendInt(shader);
        sb_appendStr(")");
    }
    if (execFlag) {
        glAttachShader(program, shader);
    }
}

static void
proc_glBindAttribLocation(GLuint program, GLuint index, const GLchar* name)
{
    if (printFlag) {
        sb_appendStr("glBindAttribLocation(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendInt(index);
        sb_appendStr(", ");
        sb_appendStr(name);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindAttribLocation(program, index, name);
    }
}

static void 
proc_glBindBuffer(GLenum target, GLuint buffer)
{
    if (printFlag) {
        sb_appendStr("glBindBuffer(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(buffer);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindBuffer(target, buffer);
    }
}

static void
proc_glBindFramebuffer(GLenum target, GLuint framebuffer)
{
    if (printFlag) {
        sb_appendStr("glBindFramebuffer(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(framebuffer);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindFramebuffer(target, framebuffer);
    }
}

static void
proc_glBindRenderbuffer(GLenum target, GLuint renderbuffer)
{
    if (printFlag) {
        sb_appendStr("glBindRenderbuffer(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(renderbuffer);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindRenderbuffer(target, renderbuffer);
    }
}

static void
proc_glBindTexture(GLenum target, GLuint texture)
{
    if (printFlag) {
        sb_appendStr("glBindTexture(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(texture);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindTexture(target, texture);
    }
}

static void proc_glBlendColor();

static void
proc_glBlendEquation(GLenum mode)
{
    if (printFlag) {
        sb_appendStr("glBlendEquation(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBlendEquation(mode);
    }
    
}

static void
proc_glBlendEquationSeparate(GLenum modeRGB, GLenum modeAlpha)
{
    if (printFlag) {
        sb_appendStr("glBlendEquationSeparate(");
        sb_appendStr(glEnum2str(modeRGB));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(modeAlpha));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBlendEquationSeparate(modeRGB, modeAlpha);
    }
}

static void
proc_glBlendFunc(GLenum sfactor, GLenum dfactor)
{
    if (printFlag) {
        sb_appendStr("glBlendFunc(");
        sb_appendStr(glEnum2str(sfactor));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(dfactor));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBlendFunc(sfactor, dfactor);
    }
}

static void
proc_glBlendFuncSeparate(GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
    if (printFlag) {
        sb_appendStr("glBlendFuncSeparate(");
        sb_appendStr(glEnum2str(srcRGB));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(dstRGB));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(srcAlpha));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(dstAlpha));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
}

static void
proc_glBufferData(GLenum target, GLsizeiptr size, const GLvoid* data, GLenum usage)
{
    if (printFlag) {
        sb_appendStr("glBufferData(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(size);
        sb_appendStr(data ? ", [...], " : ", (null), ");
        sb_appendStr(glEnum2str(usage));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBufferData(target, size, data, usage);
    }
}

static void
proc_glBufferSubData(GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid* data)
{
    if (printFlag) {
        sb_appendStr("glBufferSubData(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(offset);
        sb_appendStr(", ");
        sb_appendInt(size);
        sb_appendStr(data ? ", [...])" : ", (null))");
    }
    if (execFlag) {
        glBufferSubData(target, offset, size, data);
    }
}

static GLenum
proc_glCheckFramebufferStatus(GLenum target)
{
    GLenum res = 0;
    if (printFlag) {
        sb_appendStr("glCheckFramebufferStatus(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = glCheckFramebufferStatus(target);
    }
    return res;
}

static void
proc_glClear(GLbitfield mask)
{
    if (printFlag) {
        sb_appendStr("glClear(");
        sb_appendInt(mask);
        sb_appendStr(")");
    }
    if (execFlag) {
        glClear(mask);
    }
}

static void
proc_glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    if (printFlag) {
        sb_appendStr("glClearColor(");
        sb_appendFloat(red);
        sb_appendStr(", ");
        sb_appendFloat(green);
        sb_appendStr(", ");
        sb_appendFloat(blue);
        sb_appendStr(", ");
        sb_appendFloat(alpha);
        sb_appendStr(")");
    }
    if (execFlag) {
        glClearColor(red, green, blue, alpha);
    }
}

static void
proc_glClearDepthf(GLclampf depth)
{
    if (printFlag) {
        sb_appendStr("glClearDepthf(");
        sb_appendFloat(depth);
        sb_appendStr(")");
    }
#if !MACOSX
    if (execFlag) {
        glClearDepthf(depth);
    }
#endif
}

static void
proc_glClearStencil(GLint s)
{
    if (printFlag) {
        sb_appendStr("glClearStencil(");
        sb_appendInt(s);
        sb_appendStr(")");
    }
    if (execFlag) {
        glClearStencil(s);
    }
}

static void
proc_glColorMask(GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    if (printFlag) {
        sb_appendStr("glColorMask(");
        sb_appendBool(red);
        sb_appendStr(", ");
        sb_appendBool(green);
        sb_appendStr(", ");
        sb_appendBool(blue);
        sb_appendStr(", ");
        sb_appendBool(alpha);
        sb_appendStr(")");
    }
    if (execFlag) {
        glColorMask(red, green, blue, alpha);
    }
}

static void
proc_glCompileShader(GLuint shader)
{
    if (printFlag) {
        sb_appendStr("glCompileShader(");
        sb_appendInt(shader);
        sb_appendStr(")");
    }
    if (execFlag) {
        glCompileShader(shader);
    }
}

static void
proc_glCompressedTexImage2D(GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid* data)
{
    if (printFlag) {
        sb_appendStr("glCompressedTexImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(internalformat);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(", ");
        sb_appendInt(border);
        sb_appendStr(", ");
        sb_appendInt(imageSize);
        sb_appendStr(", ");
        sb_appendStr(data ? "[...]" : "(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
    }
}

static void
proc_glCompressedTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid* data)
{
    if (printFlag) {
        sb_appendStr("glCompressedTexSubImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(xoffset);
        sb_appendStr(", ");
        sb_appendInt(yoffset);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(format));
        sb_appendStr(", ");
        sb_appendInt(imageSize);
        sb_appendStr(", ");
        sb_appendStr(data ? "[...]" : "(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
    }
}

static void
proc_glCopyTexImage2D(GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    if (printFlag) {
        sb_appendStr("glCopyTexImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(internalformat);
        sb_appendStr(", ");
        sb_appendInt(x);
        sb_appendStr(", ");
        sb_appendInt(y);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(", ");
        sb_appendInt(border);
        sb_appendStr(")");
    }
    if (execFlag) {
        glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
    }
}

static void
proc_glCopyTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height)
{
    if (printFlag) {
        sb_appendStr("glCopyTexSubImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(xoffset);
        sb_appendStr(", ");
        sb_appendInt(yoffset);
        sb_appendStr(", ");
        sb_appendInt(x);
        sb_appendStr(", ");
        sb_appendInt(y);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(")");
    }
    if (execFlag) {
        glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }
}

static GLuint
proc_glCreateProgram()
{
    GLuint res = 0;
    if (printFlag) {
        sb_appendStr("glCreateProgram()");
    }
    if (execFlag) {
        res = glCreateProgram();
    }
    return res;
}

static GLuint
proc_glCreateShader(GLenum type)
{
    GLuint res = 0;
    if (printFlag) {
        sb_appendStr("glCreateShader(");
        sb_appendStr(glEnum2str(type));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = glCreateShader(type);
    }
    return res;
}

static void
proc_glCullFace(GLenum mode)
{
    if (printFlag) {
        sb_appendStr("glCullFace(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(")");
    }
    if (execFlag) {
        glCullFace(mode);
    }
}

static void
proc_glDeleteBuffers(GLsizei n, const GLuint* buffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glDeleteBuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(buffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glDeleteBuffers(n, buffers);
    }
}

static void
proc_glDeleteFramebuffers(GLsizei n, const GLuint* framebuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glDeleteFramebuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(framebuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glDeleteFramebuffers(n, framebuffers);
    }
}

static void
proc_glDeleteProgram(GLuint program)
{
    if (printFlag) {
        sb_appendStr("glDeleteProgram(");
        sb_appendInt(program);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDeleteProgram(program);
    }
}

static void
proc_glDeleteRenderbuffers(GLsizei n, const GLuint* renderbuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glDeleteRenderbuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(renderbuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glDeleteRenderbuffers(n, renderbuffers);
    }
}

static void
proc_glDeleteShader(GLuint shader)
{
    if (printFlag) {
        sb_appendStr("glDeleteShader(");
        sb_appendInt(shader);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDeleteShader(shader);
    }
}

static void
proc_glDeleteTextures(GLsizei n, const GLuint* textures)
{
    int i;
    if (printFlag) {
        sb_appendStr("glDeleteTextures(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(textures[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glDeleteTextures(n, textures);
    }
}

static void
proc_glDepthFunc(GLenum func)
{
    if (printFlag) {
        sb_appendStr("glDepthFunc(");
        sb_appendStr(glEnum2str(func));
        sb_appendStr(")");
    }
    if (execFlag) {
        glDepthFunc(func);
    }
}

static void
proc_glDepthMask(GLboolean flag)
{
    if (printFlag) {
        sb_appendStr("glDepthMask(");
        sb_appendBool(flag);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDepthMask(flag);
    }
}

static void
proc_glDepthRangef(GLclampf zNear, GLclampf zFar)
{
    if (printFlag) {
        sb_appendStr("glDepthRangef(");
        sb_appendFloat(zNear);
        sb_appendStr(", ");
        sb_appendFloat(zFar);
        sb_appendStr(")");
    }
#if !MACOSX
    if (execFlag) {
        glDepthRangef(zNear, zFar);
    }
#endif
}

static void
proc_glDetachShader(GLuint program, GLuint shader)
{
    if (printFlag) {
        sb_appendStr("glDetachShader(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendInt(shader);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDetachShader(program, shader);
    }
}

static void 
proc_glDisable(GLenum cap)
{
    if (printFlag) {
        sb_appendStr("glDisable(");
        sb_appendStr(glEnum2str(cap));
        sb_appendStr(")");
    }
    if (execFlag) {
        glDisable(cap);
    }
}

static void
proc_glDisableVertexAttribArray(GLuint index)
{
    if (printFlag) {
        sb_appendStr("glDisableVertexAttribArray(");
        sb_appendInt(index);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDisableVertexAttribArray(index);
    }
    vertexAttrib[index].enabled = 0;
}

static void
proc_glDrawArrays(GLenum mode, GLint first, GLsizei count)
{
    if (printFlag) {
        sb_appendStr("glDrawArrays(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(", ");
        sb_appendInt(first);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDrawArrays(mode, first, count);
    }
}

static void
proc_glDrawElements(GLenum mode, GLsizei count, GLenum type, const GLvoid* indices)
{
    if (printFlag) {
        sb_appendStr("glDrawElements(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(type));
        sb_appendStr(", ");
        sb_appendPtr(indices);
        sb_appendStr(")");
    }
    if (execFlag) {
        glDrawElements(mode, count, type, indices);
    }
}

static void
proc_glEnable(GLenum cap)
{
    if (printFlag) {
        sb_appendStr("glEnable(");
        sb_appendStr(glEnum2str(cap));
        sb_appendStr(")");
    }
    if (execFlag) {
        glEnable(cap);
    }
}

static void
proc_glEnableVertexAttribArray(GLuint index)
{
    if (printFlag) {
        sb_appendStr("glEnableVertexAttribArray(");
        sb_appendInt(index);
        sb_appendStr(")");
    }
    if (execFlag) {
        glEnableVertexAttribArray(index);
    }
    vertexAttrib[index].enabled = 1;
}

static void
proc_glFinish()
{
    if (printFlag) {
        sb_appendStr("glFinish()");
    }
    if (execFlag) {
        glFinish();
    }
}

static void
proc_glFlush()
{
    if (printFlag) {
        sb_appendStr("glFlush()");
    }
    if (execFlag) {
        glFlush();
    }
}

static void
proc_glFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
{
    if (printFlag) {
        sb_appendStr("glFramebufferRenderbuffer(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(attachment));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(renderbuffertarget));
        sb_appendStr(", ");
        sb_appendInt(renderbuffer);
        sb_appendStr(")");
    }
    if (execFlag) {
        glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
    }
}

static void
proc_glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    if (printFlag) {
        sb_appendStr("glFramebufferTexture2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(attachment));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(textarget));
        sb_appendStr(", ");
        sb_appendInt(texture);
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(")");
    }
    if (execFlag) {
        glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }
}

static void
proc_glFrontFace(GLenum mode)
{
    if (printFlag) {
        sb_appendStr("glFrontFace(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(")");
    }
    if (execFlag) {
        glFrontFace(mode);
    }
}

static void
proc_glGenBuffers(GLsizei n, GLuint *_buffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glGenBuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(_buffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        GLuint buffers[n];
        glGenBuffers(n, buffers);
        for (i=0; i<n; ++i) {
            if (_buffers[i] != buffers[i]) {
                fprintf(stderr, "FATAL: glGenBuffers buffers mismatch\n");
                exit(1);
            }
        }
    }
}

static void
proc_glGenerateMipmap(GLenum target)
{
    if (printFlag) {
        sb_appendStr("glGenerateMipmap(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(")");
    }
    if (execFlag) {
        glGenerateMipmap(target);
    }
}

static void
proc_glGenFramebuffers(GLsizei n, GLuint *_framebuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glGenFramebuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(_framebuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        GLuint framebuffers[n];
        glGenFramebuffers(n, framebuffers);
        for (i=0; i<n; ++i) {
            if (_framebuffers[i] != framebuffers[i]) {
                fprintf(stderr, "FATAL: glGenFramebuffers framebuffers mismatch\n");
                exit(1);
            }
        }
    }
}

static void
proc_glGenRenderbuffers(GLsizei n, GLuint* _renderbuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glGenRenderbuffers(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(_renderbuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        GLuint renderbuffers[n];
        glGenRenderbuffers(n, renderbuffers);
        for (i=0; i<n; ++i) {
            if (_renderbuffers[i] != renderbuffers[i]) {
                fprintf(stderr, "FATAL: glGenRenderbuffers renderbuffers mismatch\n");
                exit(1);
            }
        }
    }
}

static void
proc_glGenTextures(GLsizei n, GLuint* _textures)
{
    int i;
    if (printFlag) {
        sb_appendStr("glGenTextures(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(_textures[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        GLuint textures[n];
        glGenTextures(n, textures);
        for (i=0; i<n; ++i) {
            if (_textures[i] != textures[i]) {
                fprintf(stderr, "FATAL: glGenTextures textures mismatch\n");
                exit(1);
            }
        }
    }
}

static void proc_glGetActiveAttrib();

static void
proc_glGetActiveUniform(GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{    
    if (printFlag) {
        sb_appendStr("glGetActiveUniform(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendInt(index);
        sb_appendStr(", ");
        sb_appendInt(bufsize);
        sb_appendStr(", ");
        if (length) sb_appendInt(*length);
        else sb_appendStr("(null)");
        sb_appendStr(", ");
        if (size) sb_appendInt(*size);
        else sb_appendStr("(null)");
        sb_appendStr(", ");
        if (type) sb_appendStr(glEnum2str(*type));
        else sb_appendStr("(null)");
        if (name) sb_appendStr(name);
        else sb_appendStr("(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        glGetActiveUniform(program, index, bufsize, length, size, type, name);
    }
}

static void
proc_glGetAttachedShaders(GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders)
{
    if (printFlag) {
        sb_appendStr("glGetAttachedShaders(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendInt(maxcount);
        sb_appendStr(", ");
        if (count) sb_appendInt(*count);
        else sb_appendStr("(null)");
        sb_appendStr(", ");
        if (shaders) sb_appendStr("[...]");
        else sb_appendStr("(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        glGetAttachedShaders(program, maxcount, count, shaders);
    }
}

static int
proc_glGetAttribLocation(GLuint program, const GLchar* name)
{
    int res = -1;
    if (printFlag) {
        sb_appendStr("glGetAttribLocation(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendStr(name);
        sb_appendStr(")");
    }
    if (execFlag) {
        res = glGetAttribLocation(program, name);
    }
    return res;
}

static void
proc_glGetBooleanv(GLenum pname, GLboolean _params)
{
    if (printFlag) {
        sb_appendStr("glGetBooleanv(");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(_params);
        sb_appendStr(")");
    }
    if (execFlag) {
        GLboolean params[4];
        glGetBooleanv(pname, params);
        if (_params != params[0]) {
            fprintf(stderr, "ERROR: glGetBooleanv params mismatch\n");
        }
    }
}

static void proc_glGetBufferParameteriv();

static GLenum
proc_glGetError()
{
    GLenum res = 0;
    if (printFlag) {
        sb_appendStr("glGetError()");
    }
    if (execFlag) {
        res = glGetError();
    }
    return res;
}

static void
proc_glGetFloatv(GLenum pname, const GLfloat* params)
{
    if (printFlag) {
        sb_appendStr("glGetFloatv(");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        if (params) sb_appendFloat(*params);
        else sb_appendStr("(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        GLfloat _params[4];
        glGetFloatv(pname, params ? _params : NULL);
        if (_params[0] != params[0]) {
            fprintf(stderr, "ERROR: glGetFloatv params mismatch\n");
        }
    }
}

static void
proc_glGetFramebufferAttachmentParameteriv(GLenum target, GLenum attachment, GLenum pname, GLint _params)
{
    if (printFlag) {
        sb_appendStr("glGetFramebufferAttachmentParameteriv(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(attachment));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(_params);
        sb_appendStr(")");
    }
    if (execFlag) {
        GLint params[4];
        glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
        if (_params != params[0]) {
            fprintf(stderr, "ERROR: glGetFramebufferAttachmentParameteriv params mismatch\n");
        }
    }    
}

static void
proc_glGetIntegerv(GLenum pname, const GLint *params)
{
    if (printFlag) {
        sb_appendStr("glGetIntegerv(");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        if (params) sb_appendInt(*params);
        else sb_appendStr("(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        GLint _params[4];
        glGetIntegerv(pname, params ? _params : NULL);
        if (_params[0] != params[0]) {
            fprintf(stderr, "ERROR: glGetIntegerv params mismatch\n");
        }
    }
}

static void
proc_glGetProgramiv(GLuint program, GLenum pname, GLint _params)
{
    if (printFlag) {
        sb_appendStr("glGetProgramiv(");
        sb_appendInt(program);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(_params);
        sb_appendStr(")");
    }
    if (execFlag) {
        GLint params;
        glGetProgramiv(program, pname, &params);
        if (_params != params) {
            fprintf(stderr, "ERROR: glGetProgramiv params mismatch\n");
        }
    }
}

static void proc_glGetProgramInfoLog();

static void
proc_glGetRenderbufferParameteriv(GLenum target, GLenum pname, GLint _params)
{
    if (printFlag) {
        sb_appendStr("glGetRenderbufferParameteriv(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(_params);
        sb_appendStr(")");
    }
    if (execFlag) {
        GLint params;
        glGetRenderbufferParameteriv(target, pname, &params);
        if (_params != params) {
            fprintf(stderr, "ERROR: glGetRenderbufferParameteriv params mismatch\n");
        }
    }
}

static void
proc_glGetShaderiv(GLuint shader, GLenum pname, GLint _params)
{
    if (printFlag) {
        sb_appendStr("glGetShaderiv(");
        sb_appendInt(shader);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(_params);
        sb_appendStr(")");
    }
    if (execFlag) {
        GLint params;
        glGetShaderiv(shader, pname, &params);
        if (_params != params) {
            fprintf(stderr, "ERROR: glGetShaderiv params mismatch\n");
        }
    }
}

static void proc_glGetShaderInfoLog();
static void proc_glGetShaderPrecisionFormat();
static void proc_glGetShaderSource();

static const GLubyte *
proc_glGetString(GLenum name)
{
    const GLubyte *res = NULL;
    if (printFlag) {
        sb_appendStr("glGetString(");
        sb_appendStr(glEnum2str(name));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = glGetString(name);
    }
    return res;
}

static void proc_glGetTexParameterfv();
static void proc_glGetTexParameteriv();
static void proc_glGetUniformfv();
static void proc_glGetUniformiv();

static int
proc_glGetUniformLocation(GLuint program, const GLchar* name)
{
    int res = -1;
    if (printFlag) {
        sb_appendStr("glGetUniformLocation(");
        sb_appendInt(program);
        sb_appendStr(", ");     
        sb_appendStr(name);     
        sb_appendStr(")");      
    }
    if (execFlag) {
        res = glGetUniformLocation(program, name);
    }
    return res;
}

static void proc_glGetVertexAttribfv();
static void proc_glGetVertexAttribiv();
static void proc_glGetVertexAttribPointerv();
static void proc_glHint ();
static void proc_glIsBuffer     ();
static void proc_glIsEnabled    ();
static void proc_glIsFramebuffer();
static void proc_glIsProgram    ();
static void proc_glIsRenderbuffer();
static void proc_glIsShader     ();
static void proc_glIsTexture    ();
static void proc_glLineWidth    ();

static void
proc_glLinkProgram(GLuint program)
{
    if (printFlag) {
        sb_appendStr("glLinkProgram(");
        sb_appendInt(program);
        sb_appendStr(")");      
    }
    if (execFlag) {
        glLinkProgram(program);
    }
}

static void
proc_glPixelStorei(GLenum pname, GLint param)
{
    if (printFlag) {
        sb_appendStr("glPixelStorei(");
        sb_appendInt(pname);
        sb_appendStr(", ");
        sb_appendInt(param);
        sb_appendStr(")");      
    }
    if (execFlag) {
        glPixelStorei(pname, param);
    }
}

static void
proc_glPolygonOffset(GLfloat factor, GLfloat units)
{
    if (printFlag) {
        sb_appendStr("glPolygonOffset(");
        sb_appendFloat(factor);
        sb_appendStr(", ");
        sb_appendFloat(units);
        sb_appendStr(")");
    }
    if (execFlag) {
        glPolygonOffset(factor, units);
    }
}

static void proc_glReadPixels();
static void proc_glReleaseShaderCompiler();

static void
proc_glRenderbufferStorage(GLenum target, GLenum internalformat, GLsizei width, GLsizei height)
{
    if (printFlag) {
        sb_appendStr("glRenderbufferStorage(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(internalformat));
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(")");      
    }
    if (execFlag) {
        glRenderbufferStorage(target, internalformat, width, height);
    }
}

static void proc_glSampleCoverage();

static void
proc_glScissor(GLint x, GLint y, GLsizei width, GLsizei height)
{
    if (printFlag) {
        sb_appendStr("glScissor(");
        sb_appendInt(x);
        sb_appendStr(", ");
        sb_appendInt(y);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(")");      
    }
    if (execFlag) {
        glScissor(x, y, width, height);
    }
}

static void proc_glShaderBinary();

static void
proc_glShaderSource(GLuint shader, GLsizei count, const GLchar** string, const GLint* length)
{
    if (printFlag) {
        sb_appendStr("glShaderSource(");
        sb_appendInt(shader);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", [");
        if (printArrays) {
            int i;
            for (i=0; i<count; ++i) {
                sb_appendStr("\n");
                sb_appendStr(string[i]);
            }
        }
        else {
            sb_appendStr("...");
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glShaderSource(shader, count, string, length);
    }
}

static void proc_glStencilFunc();
static void proc_glStencilFuncSeparate();
static void proc_glStencilMask();
static void proc_glStencilMaskSeparate();
static void proc_glStencilOp    ();
static void proc_glStencilOpSeparate();

static void
proc_glTexImage2D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
{
    if (printFlag) {
        sb_appendStr("glTexImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(internalformat);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(", ");
        sb_appendInt(border);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(format));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(type));
        sb_appendStr(", ");
        sb_appendStr(pixels ? "[...]" : "(null)");
        sb_appendStr(")");      
    }
    if (execFlag) {
        glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
}

static void proc_glTexParameterf();
static void proc_glTexParameterfv();

static void
proc_glTexParameteri(GLenum target, GLenum pname, GLint param)
{
    if (printFlag) {
        sb_appendStr("glTexParameteri(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(pname));
        sb_appendStr(", ");
        sb_appendInt(param);
        sb_appendStr(")");      
    }
    if (execFlag) {
        glTexParameteri(target, pname, param);
    }
}

static void proc_glTexParameteriv();

static void
proc_glTexSubImage2D(GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels)
{
    if (printFlag) {
        sb_appendStr("glTexSubImage2D(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(", ");
        sb_appendInt(xoffset);
        sb_appendStr(", ");
        sb_appendInt(yoffset);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(format));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(type));
        sb_appendStr(", ");
        sb_appendPtr(pixels);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
    }
}

static void
proc_glUniform1f(GLint location, GLfloat x)
{
    if (printFlag) {
        sb_appendStr("glUniform1f(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendFloat(x);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glUniform1f(location, x);
    }
}

static void
proc_glUniform1fv(GLint location, GLsizei count, const GLfloat* v)
{
    if (printFlag) {
        sb_appendStr("glUniform1fv(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", [...])");
    }
    if (execFlag) {
        glUniform1fv(location, count, v);
    }
}

static void
proc_glUniform1i(GLint location, GLint x)
{
    if (printFlag) {
        sb_appendStr("glUniform1i(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(x);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glUniform1i(location, x);
    }
}

static void proc_glUniform1iv();

static void
proc_glUniform2f(GLint location, GLfloat x, GLfloat y)
{
    if (printFlag) {
        sb_appendStr("glUniform2f(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendFloat(x);
        sb_appendStr(", ");
        sb_appendFloat(y);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glUniform2f(location, x, y);
    }
}

static void proc_glUniform2fv();
static void proc_glUniform2i    ();
static void proc_glUniform2iv();

static void
proc_glUniform3f(GLint location, GLfloat x, GLfloat y, GLfloat z)
{
    if (printFlag) {
        sb_appendStr("glUniform3f(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendFloat(x);
        sb_appendStr(", ");
        sb_appendFloat(y);
        sb_appendStr(", ");
        sb_appendFloat(z);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glUniform3f(location, x, y, z);
    }
}

static void proc_glUniform3fv();
static void proc_glUniform3i    ();
static void proc_glUniform3iv();

static void
proc_glUniform4f(GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    if (printFlag) {
        sb_appendStr("glUniform4f(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendFloat(x);
        sb_appendStr(", ");
        sb_appendFloat(y);
        sb_appendStr(", ");
        sb_appendFloat(z);
        sb_appendStr(", ");
        sb_appendFloat(w);
        sb_appendStr(")");
    }   
    if (execFlag) {
        glUniform4f(location, x, y, z, w);
    }
}

static void
proc_glUniform4fv(GLint location, GLsizei count, const GLfloat* v)
{
    if (printFlag) {
        sb_appendStr("glUniform4fv(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", [...])");
    }   
    if (execFlag) {
        glUniform4fv(location, count, v);
    }
}

static void proc_glUniform4i    ();
static void proc_glUniform4iv();

static void
proc_glUniformMatrix2fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    if (printFlag) {
        sb_appendStr("glUniformMatrix2fv(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", ");
        sb_appendBool(transpose);
        sb_appendStr(", [");
        if (printArrays) {
            int i;
            for (i=0; i<count*4; ++i) {
                if (i == 0) sb_appendStr("\n\t");
                else if (i % 2 == 0) sb_appendStr(",\n\t");
                else sb_appendStr(", ");
                sb_appendFloat(value[i]);
            }
        }
        else {
            sb_appendStr("...");
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glUniformMatrix2fv(location, count, transpose, value);
    }
}

static void
proc_glUniformMatrix3fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    if (printFlag) {
        sb_appendStr("glUniformMatrix3fv(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", ");
        sb_appendBool(transpose);
        sb_appendStr(", [");
        if (printArrays) {
            int i;
            for (i=0; i<count*9; ++i) {
                if (i == 0) sb_appendStr("\n\t");
                else if (i % 3 == 0) sb_appendStr(",\n\t");
                else sb_appendStr(", ");
                sb_appendFloat(value[i]);
            }
        }
        else {
            sb_appendStr("...");
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glUniformMatrix3fv(location, count, transpose, value);
    }
}

static void 
proc_glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    if (printFlag) {
        sb_appendStr("glUniformMatrix4fv(");
        sb_appendInt(location);
        sb_appendStr(", ");
        sb_appendInt(count);
        sb_appendStr(", ");
        sb_appendBool(transpose);
        sb_appendStr(", [");
        if (printArrays) {
            int i;
            for (i=0; i<count*16; ++i) {
                if (i == 0) sb_appendStr("\n\t");
                else if (i % 4 == 0) sb_appendStr(",\n\t");
                else sb_appendStr(", ");
                sb_appendFloat(value[i]);
            }
        }
        else {
            sb_appendStr("...");
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glUniformMatrix4fv(location, count, transpose, value);
    }
}

static void
proc_glUseProgram(GLuint program)
{
    if (printFlag) {
        sb_appendStr("glUseProgram(");
        sb_appendInt(program);
        sb_appendStr(")");
    }
    if (execFlag) {
        glUseProgram(program);
    }
}

static void
proc_glValidateProgram(GLuint program)
{
    if (printFlag) {
        sb_appendStr("glValidateProgram(");
        sb_appendInt(program);
        sb_appendStr(")");
    }
    if (execFlag) {
        glValidateProgram(program);
    }
}

static void proc_glVertexAttrib1f();
static void proc_glVertexAttrib1fv();
static void proc_glVertexAttrib2f();
static void proc_glVertexAttrib2fv();
static void proc_glVertexAttrib3f();
static void proc_glVertexAttrib3fv();
static void proc_glVertexAttrib4f();
static void proc_glVertexAttrib4fv();

static void
proc_glVertexAttribPointer(GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr)
{
    if (printFlag) {
        sb_appendStr("glVertexAttribPointer(");
        sb_appendInt(indx);
        sb_appendStr(", ");
        sb_appendInt(size);
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(type));
        sb_appendStr(", ");
        sb_appendBool(normalized);
        sb_appendStr(", ");
        sb_appendInt(stride);
        sb_appendStr(", ");
        sb_appendPtr(ptr);
        sb_appendStr(")");
    }
    if (execFlag) {
        glVertexAttribPointer(indx, size, type, normalized, stride, ptr);
        vertexAttrib[indx].size = size;
        vertexAttrib[indx].type = type;
        vertexAttrib[indx].normalized = normalized;
    }
}

static void
proc_glViewport(GLint x, GLint y, GLsizei width, GLsizei height)
{
    if (printFlag) {
        sb_appendStr("glViewport(");
        sb_appendInt(x);
        sb_appendStr(", ");
        sb_appendInt(y);
        sb_appendStr(", ");
        sb_appendInt(width);
        sb_appendStr(", ");
        sb_appendInt(height);
        sb_appendStr(")");
    }
    if (execFlag) {
        glViewport(x, y, width, height);
    }
}

#if MACOSX
static void
proc_glBegin(GLenum mode)
{
    if (printFlag) {
        sb_appendStr("glBegin(");
        sb_appendStr(glEnum2str(mode));
        sb_appendStr(")");
    }
    if (execFlag) {
        glBegin(mode);
    }
}

static void
proc_glEnd()
{
    if (printFlag) {
        sb_appendStr("glEnd()");
    }
    if (execFlag) {
        glEnd();
    }
}

static GLboolean
proc_glIsRenderbufferEXT(GLuint renderbuffer);
static void
proc_glBindRenderbufferEXT(GLenum target, GLuint renderbuffer);
static void
proc_glDeleteRenderbuffersEXT(GLsizei n, const GLuint *renderbuffers);
static void
proc_glGenRenderbuffersEXT(GLsizei n, GLuint *renderbuffers);
static void
proc_glRenderbufferStorageEXT(GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
static void
proc_glGetRenderbufferParameterivEXT(GLenum target, GLenum pname, GLint *params);
static GLboolean
proc_glIsFramebufferEXT(GLuint framebuffer);

static void
proc_glBindFramebufferEXT(GLenum target, GLuint framebuffer)
{
    if (printFlag) {
        sb_appendStr("glBindFramebufferEXT(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendInt(framebuffer);
        sb_appendStr(")");
    }
    if (execFlag) {
        glBindFramebufferEXT(target, framebuffer);
    }
}

static void
proc_glDeleteFramebuffersEXT(GLsizei n, const GLuint *framebuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glDeleteFramebuffersEXT(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(framebuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        glDeleteFramebuffersEXT(n, framebuffers);
    }
}

static void
proc_glGenFramebuffersEXT(GLsizei n, GLuint *_framebuffers)
{
    int i;
    if (printFlag) {
        sb_appendStr("glGenFramebuffersEXT(");
        sb_appendInt(n);
        sb_appendStr(", [");
        for (i=0; i<n; ++i) {
            if (i!=0) sb_appendStr(", ");
            sb_appendInt(_framebuffers[i]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        GLuint framebuffers[n];
        glGenFramebuffersEXT(n, framebuffers);
        for (i=0; i<n; ++i) {
            if (_framebuffers[i] != framebuffers[i]) {
                fprintf(stderr, "FATAL: glGenFramebuffersEXT framebuffers mismatch\n");
                exit(1);
            }
        }
    }
}

static GLenum
proc_glCheckFramebufferStatusEXT(GLenum target)
{
    GLenum res = 0;
    if (printFlag) {
        sb_appendStr("glCheckFramebufferStatusEXT(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = glCheckFramebufferStatusEXT(target);
    }
    return res;
}

static void
proc_glFramebufferTexture1DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);

static void
proc_glFramebufferTexture2DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    if (printFlag) {
        sb_appendStr("glFramebufferTexture2DEXT(");
        sb_appendStr(glEnum2str(target));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(attachment));
        sb_appendStr(", ");
        sb_appendStr(glEnum2str(textarget));
        sb_appendStr(", ");
        sb_appendInt(texture);
        sb_appendStr(", ");
        sb_appendInt(level);
        sb_appendStr(")");
    }
    if (execFlag) {
        glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
    }
}

static void
proc_glFramebufferTexture3DEXT(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset);
static void
proc_glFramebufferRenderbufferEXT(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
static void
proc_glGetFramebufferAttachmentParameterivEXT(GLenum target, GLenum attachment, GLenum pname, GLint *params);
static void
proc_glGenerateMipmapEXT(GLenum target);
#endif /* MACOSX */

#if linux
extern const char *eglEnum2str(EGLenum);

static EGLDisplay
proc_eglGetDisplay(EGLNativeDisplayType display_id)
{
    EGLDisplay res = NULL;
    if (printFlag) {
        sb_appendStr("eglGetDisplay(");
        sb_appendPtr((void*)display_id);
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglGetDisplay(display_id);
    }
    return res;
}

static EGLBoolean
proc_eglInitialize(EGLDisplay dpy, const EGLint *major, const EGLint *minor)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglInitialize(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        if (major) sb_appendInt(*major);
        else sb_appendStr("(null)");
        sb_appendStr(", ");
        if (minor) sb_appendInt(*minor);
        else sb_appendStr("(null)");
        sb_appendStr(")");
    }
    if (execFlag) {
        EGLint _major, _minor;
        res = eglInitialize(dpy, &_major, &_minor);
        if ((major && (*major != _major)) || (minor && (*minor != _minor))) {
            fprintf(stderr, "ERROR: eglInitialize version mismatch\n");
        }
    }
    return res;
}

static EGLBoolean proc_eglTerminate() { return EGL_FALSE; }

static const char *
proc_eglQueryString(EGLDisplay dpy, EGLint name)
{
    const char *res = NULL;
    if (printFlag) {
        sb_appendStr("eglQueryString(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendStr(eglEnum2str(name));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglQueryString(dpy, name);
    }
    return res;
}

static EGLBoolean proc_eglGetConfigs() { return EGL_FALSE; }

static EGLBoolean 
proc_eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list,
           EGLConfig *_configs, EGLint config_size,
           EGLint _num_config)
{
    EGLBoolean res = EGL_FALSE;
    int i;
    if (printFlag) {
        sb_appendStr("eglChooseConfig(");
        sb_appendPtr(dpy);
        sb_appendStr(", [");
        for (i=0;;) {
            int attr = attrib_list[i++];
            if (attr == EGL_NONE) break;
            if (i > 0) sb_appendStr(",");
            sb_appendStr("\n\t");
            sb_appendStr(eglEnum2str(attr));
            sb_appendStr(", ");
            sb_appendInt(attrib_list[i++]);
        }
        sb_appendStr("],\n\t[");
        for (i=0; i<_num_config; ++i) {
            if (i > 0) sb_appendStr(", ");
            sb_appendPtr(_configs[i]);
        }
        sb_appendStr("], ");
        sb_appendInt(config_size);
        sb_appendStr(", ");
        sb_appendInt(_num_config);
        sb_appendStr(")");
    }
    if (execFlag) {
        EGLConfig configs[config_size];
        EGLint num_config;
        res = eglChooseConfig(dpy, attrib_list, configs, config_size, &num_config);
        if (res) {
            if (num_config != _num_config) {
                fprintf(stderr, "ERROR: eglChooseConfig num_config mismatch\n");
            }
            for (i=0; i<num_config && i<config_size; ++i) {
                if (configs[i] != _configs[i]) {
                    fprintf(stderr, "ERROR: eglChooseConfig configs[] mismatch\n");
                    break;
                }
            }
        }
    }
    return res;
}

static EGLBoolean
proc_eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint _value)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglGetConfigAttrib(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(config);
        sb_appendStr(", ");
        sb_appendStr(eglEnum2str(attribute));
        sb_appendStr(", ");
        sb_appendInt(_value);
        sb_appendStr(")");
    }   
    if (execFlag) {
        EGLint value;
        res = eglGetConfigAttrib(dpy, config, attribute, &value);
        if (res && value != _value) {
            fprintf(stderr, "ERROR: eglGetConfigAttrib value mismatch\n");
        }
    }
    return res;
}

static EGLSurface
proc_eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
    EGLSurface res = NULL;
    int i;
    if (printFlag) {
        sb_appendStr("eglCreateWindowSurface(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(config);
        sb_appendStr(", ");
        sb_appendPtr(win);
        sb_appendStr(", [");
        for (i=0;;) {
            int attr = attrib_list[i++];
            if (attr == EGL_NONE) break;
            if (i > 0) sb_appendStr(",");
            sb_appendStr("\n\t");
            sb_appendStr(eglEnum2str(attr));
            sb_appendStr(", ");
            sb_appendInt(attrib_list[i++]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        EGLNativeWindowType nwin = createNativeWindow();
        res = eglCreateWindowSurface(dpy, config, nwin, attrib_list);
    }
    return res;
}

static EGLBoolean proc_eglCreatePbufferSurface() { return EGL_FALSE; }
static EGLBoolean proc_eglCreatePixmapSurface() { return EGL_FALSE; }
static EGLBoolean proc_eglDestroySurface() { return EGL_FALSE; }
static EGLBoolean proc_eglQuerySurface() { return EGL_FALSE; }

static EGLBoolean 
proc_eglBindAPI(EGLenum api)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglBindAPI(");
        sb_appendStr(eglEnum2str(api));
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglBindAPI(api);
    }
    return res;
}

static EGLBoolean proc_eglQueryAPI() { return EGL_FALSE; }
static EGLBoolean proc_eglWaitClient() { return EGL_FALSE; }
static EGLBoolean proc_eglReleaseThread() { return EGL_FALSE; }
static EGLBoolean proc_eglCreatePbufferFromClientBuffer() { return EGL_FALSE; }

static EGLBoolean
proc_eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglSurfaceAttrib(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(surface);
        sb_appendStr(", ");
        sb_appendStr(eglEnum2str(attribute));
        sb_appendStr(", ");
        sb_appendInt(value);    
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglSurfaceAttrib(dpy, surface, attribute, value);
    }
    return res;
}

static EGLBoolean proc_eglBindTexImage() { return EGL_FALSE; }
static EGLBoolean proc_eglReleaseTexImage() { return EGL_FALSE; }

static EGLContext
proc_eglCreateContext(EGLDisplay dpy, EGLConfig config, EGLContext share_context, const EGLint *attrib_list)
{
    EGLContext res = NULL;
    int i;
    if (printFlag) {
        sb_appendStr("eglCreateContext(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(config);
        sb_appendStr(", ");
        sb_appendPtr(share_context);
        sb_appendStr(", [");
        for (i=0;;) {
            int attr = attrib_list[i++];
            if (attr == EGL_NONE) break;
            if (i > 0) sb_appendStr(",");
            sb_appendStr("\n\t");
            sb_appendStr(eglEnum2str(attr));
            sb_appendStr(", ");
            sb_appendInt(attrib_list[i++]);
        }
        sb_appendStr("])");
    }
    if (execFlag) {
        res = eglCreateContext(dpy, config, share_context, attrib_list);
    }
    return res;
}

static EGLBoolean 
proc_eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglDestroyContext(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(ctx);
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglDestroyContext(dpy, ctx);
    }
    return res;
}

static EGLBoolean
proc_eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglMakeCurrent(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(draw);
        sb_appendStr(", ");
        sb_appendPtr(read);
        sb_appendStr(", ");
        sb_appendPtr(ctx);
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglMakeCurrent(dpy, draw, read, ctx);
    }
    return res;
}

static EGLBoolean proc_eglGetCurrentContext() { return EGL_FALSE; }
static EGLBoolean proc_eglGetCurrentSurface() { return EGL_FALSE; }
static EGLBoolean proc_eglGetCurrentDisplay() { return EGL_FALSE; }
static EGLBoolean proc_eglQueryContext() { return EGL_FALSE; }
static EGLBoolean proc_eglWaitGL() { return EGL_FALSE; }
static EGLBoolean proc_eglWaitNative() { return EGL_FALSE; }

static EGLBoolean
proc_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
    EGLBoolean res = EGL_FALSE;
    if (printFlag) {
        sb_appendStr("eglSwapBuffers(");
        sb_appendPtr(dpy);
        sb_appendStr(", ");
        sb_appendPtr(surface);
        sb_appendStr(")");
    }
    if (execFlag) {
        res = eglSwapBuffers(dpy, surface);
    }
    return res;
}

static EGLBoolean proc_eglCopyBuffers() { return EGL_FALSE; }
#endif

static void
process(int frames)
{
    uint64_t tbgn, tend;

    for (;frames != 0;) {
        sb_reset();
        int cmd = getCmd();

        // prefix commands
        if (cmd == OPC_THREAD) {
            curThread = getPtr();
            if (printFlag && !printThread) {
                fprintf(stdout, "THREAD %llu\n", curThread);
            }
            cmd = getCmd();
        }
        
        switch (cmd) {
        case OPC_MARK: {
            const char *str = getString();
            if (printFlag) {
                sb_appendStr("================ ");
                sb_appendStr(str);
            }
            break;
        }
        case OPC_glActiveTexture: {
            GLenum texture = getInt();
            proc_glActiveTexture(texture);
            break;
        }
        case OPC_glAttachShader: {
            GLuint program = getInt();
            GLuint shader = getInt();
            proc_glAttachShader(program, shader);
            break;
        }
        case OPC_glBindAttribLocation: {
            GLuint program = getInt();
            GLuint index = getInt();
            const GLchar* name = (const GLchar*)getString();
            proc_glBindAttribLocation(program, index, name);
            break;
        }
        case OPC_glBindBuffer: {
            GLenum target = getInt();
            GLuint buffer = getInt();
            proc_glBindBuffer(target, buffer);
            if (target == GL_ARRAY_BUFFER) {
                arrayBufferBinding = buffer;
            }
            else if (target == GL_ELEMENT_ARRAY_BUFFER) {
                elementArrayBufferBinding = buffer;
            }
            break;
        }
        case OPC_glBindFramebuffer: {
            GLenum target = getInt();
            GLuint framebuffer = getInt();
            proc_glBindFramebuffer(target, framebuffer);
            break;
        }
        case OPC_glBindRenderbuffer: {
            GLenum target = getInt();
            GLuint renderbuffer = getInt();
            proc_glBindRenderbuffer(target, renderbuffer);
            break;
        }
        case OPC_glBindTexture: {
            GLenum target = getInt();
            GLuint texture = getInt();
            proc_glBindTexture(target, texture);
            break;
        }
        case OPC_glBlendColor:          NOT_IMPLEMENTED();
        case OPC_glBlendEquation: {
            GLenum mode = getInt();
            proc_glBlendEquation(mode);
            break;
        }
        case OPC_glBlendEquationSeparate: {
            GLenum modeRGB = getInt();
            GLenum modeAlpha = getInt();
            proc_glBlendEquationSeparate(modeRGB, modeAlpha);
            break;
        }
        case OPC_glBlendFunc: {
            GLenum sfactor = getInt();
            GLenum dfactor = getInt();
            proc_glBlendFunc(sfactor, dfactor);
            break;
        }
        case OPC_glBlendFuncSeparate: {
            GLenum srcRGB = getInt();
            GLenum dstRGB = getInt();
            GLenum srcAlpha = getInt();
            GLenum dstAlpha = getInt();
            proc_glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
            break;
        }
        case OPC_glBufferData: {
            GLenum target = getInt();
            GLsizeiptr size = getInt();
            const GLvoid* data = getBytes();
            GLenum usage = getInt();
            proc_glBufferData(target, size, data, usage);
            break;
        }
        case OPC_glBufferSubData: {
            GLenum target = getInt();
            GLintptr offset = getInt();
            GLsizeiptr size = getInt();
            const GLvoid* data = getBytes();
            proc_glBufferSubData(target, offset, size, data);
            break;
        }
        case OPC_glCheckFramebufferStatus: {
            GLenum target = getInt();
            GLenum curVal = proc_glCheckFramebufferStatus(target);
            GLenum oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendStr(glEnum2str(oldVal));
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: glCheckFramebufferStatus return mismatch\n");
            }
            break;
        }
        case OPC_glClear: {
            GLbitfield mask = getInt();
            proc_glClear(mask);
            break;
        }
        case OPC_glClearColor: {
            GLclampf red = getFloat();
            GLclampf green = getFloat();
            GLclampf blue = getFloat();
            GLclampf alpha = getFloat();
            proc_glClearColor(red, green, blue, alpha);
            break;
        }
        case OPC_glClearDepthf: {
            GLclampf depth = getFloat();
            proc_glClearDepthf(depth);
            break;
        }
        case OPC_glClearStencil: {
            GLint s = getInt();
            proc_glClearStencil(s);
            break;
        }
        case OPC_glColorMask: {
            GLboolean red = getInt();
            GLboolean green = getInt();
            GLboolean blue = getInt();
            GLboolean alpha = getInt();
            proc_glColorMask(red, green, blue, alpha);
            break;
        }
        case OPC_glCompileShader: {
            GLuint shader = getInt();
            proc_glCompileShader(shader);
            break;
        }
        case OPC_glCompressedTexImage2D: {
            GLenum target = getInt();
            GLint level = getInt();
            GLenum internalformat = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            GLint border = getInt();
            GLsizei imageSize = getInt();
            const GLvoid* data = getBytes();
            proc_glCompressedTexImage2D(target, level, internalformat, width, height, border, imageSize, data);
            break;
        }
        case OPC_glCompressedTexSubImage2D: {
            GLenum target = getInt();
            GLint level = getInt();
            GLint xoffset = getInt();
            GLint yoffset = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            GLenum format = getInt();
            GLsizei imageSize = getInt();
            const GLvoid* data = getBytes();
            proc_glCompressedTexSubImage2D(target, level, xoffset, yoffset, width, height, format, imageSize, data);
            break;
        }
        case OPC_glCopyTexImage2D:{
            GLenum target = getInt();
            GLint level = getInt();
            GLenum internalformat = getInt();
            GLint x = getInt();
            GLint y = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            GLint border = getInt();
            proc_glCopyTexImage2D(target, level, internalformat, x, y, width, height, border);
            break;
        }
        case OPC_glCopyTexSubImage2D: {
            GLenum target = getInt();
            GLint level = getInt();
            GLint xoffset = getInt();
            GLint yoffset = getInt();
            GLint x = getInt();
            GLint y = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            proc_glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
            break;
        }
        case OPC_glCreateProgram: {
            GLuint curVal = proc_glCreateProgram();
            GLuint oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: glCreateProgram return mismatch\n");
            }
            break;
        }
        case OPC_glCreateShader: {
            GLenum type = getInt();
            GLuint curVal = proc_glCreateShader(type);
            GLuint oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: glCreateShader return mismatch\n");
            }
            break;
        }
        case OPC_glCullFace: {
            GLenum mode = getInt();
            proc_glCullFace(mode);
            break;
        }
        case OPC_glDeleteBuffers: {
            GLsizei n = getInt();
            GLuint buffers[n];
            int i;
            for (i=0; i<n; ++i) {
                buffers[i] = getInt();
            }
            proc_glDeleteBuffers(n, buffers);
            break;
        }
        case OPC_glDeleteFramebuffers: {
            GLsizei n = getInt();
            GLuint framebuffers[n];
            int i;
            for (i=0; i<n; ++i) {
                framebuffers[i] = getInt();
            }
            proc_glDeleteFramebuffers(n, framebuffers);
            break;
        }
        case OPC_glDeleteProgram: {
            GLuint program = getInt();
            proc_glDeleteProgram(program);
            break;
        }
        case OPC_glDeleteRenderbuffers: {
            GLsizei n = getInt();
            GLuint renderbuffers[n];
                int i;
                for (i=0; i<n; ++i) {
                    renderbuffers[i] = getInt();
                }
                proc_glDeleteRenderbuffers(n, renderbuffers);
                break;
        }
        case OPC_glDeleteShader: {
            GLuint shader = getInt();
            proc_glDeleteShader(shader);
            break;
        }
        case OPC_glDeleteTextures: {
            GLsizei n = getInt();
            GLuint textures[n];
            int i;
            for (i=0; i<n; ++i) {
                textures[i] = getInt();
            }
            proc_glDeleteTextures(n, textures);
            break;
        }
        case OPC_glDepthFunc: {
            GLenum func = getInt();
            proc_glDepthFunc(func);
            break;
        }
        case OPC_glDepthMask: {
            GLboolean flag = getInt();
            proc_glDepthMask(flag);
            break;
        }
        case OPC_glDepthRangef: {
            GLclampf zNear = getFloat();
            GLclampf zFar  = getFloat();
            proc_glDepthRangef(zNear, zFar);
            break;
        }
        case OPC_glDetachShader: {
            GLuint program = getInt();
            GLuint shader  = getInt();
            proc_glDetachShader(program, shader);
            break;
        }
        case OPC_glDisable: {
            GLenum cap = getInt();
            proc_glDisable(cap);
            break;
        }
        case OPC_glDisableVertexAttribArray: {
            GLuint index = getInt();
            proc_glDisableVertexAttribArray(index);
            break;
        }
        case OPC_glDrawArrays: {
            GLenum mode = getInt();
            GLint first = getInt();
            GLsizei count = getInt();
            proc_glDrawArrays(mode, first, count);
            break;
        }
        case OPC_glDrawElements: {
            GLenum mode = getInt();
            GLsizei count = getInt();
            GLenum type = getInt();
            const GLvoid* indices = elementArrayBufferBinding ? (const GLvoid*)getPtr() : getBytes();
            if (!arrayBufferBinding) {
                int i;
                for (i=0; i<MAX_VERTEX_ATTRIBS; ++i) {
                    if (!vertexAttrib[i].enabled) continue;
                    const GLvoid *pointer = getBytes(); 
                    getVertexAttrib(i, count, pointer);
                }
            }
            proc_glDrawElements(mode, count, type, indices);
            break;
        }
        case OPC_glEnable: {
            GLenum cap = getInt();
            proc_glEnable(cap);
            break;
        }
        case OPC_glEnableVertexAttribArray: {
            GLuint index = getInt();
            proc_glEnableVertexAttribArray(index);
            break;
        }
        case OPC_glFinish:
            proc_glFinish();
            break;
        case OPC_glFlush: 
            proc_glFlush();
            break;
        case OPC_glFramebufferRenderbuffer: {
            GLenum target = getInt();
            GLenum attachment = getInt();
            GLenum renderbuffertarget = getInt();
            GLuint renderbuffer = getInt();
            proc_glFramebufferRenderbuffer(target, attachment, renderbuffertarget, renderbuffer);
            break;
        }
        case OPC_glFramebufferTexture2D: {
            GLenum target = getInt();
            GLenum attachment = getInt();
            GLenum textarget = getInt();
            GLuint texture = getInt();
            GLint level = getInt();
            proc_glFramebufferTexture2D(target, attachment, textarget, texture, level);
            break;
        }
        case OPC_glFrontFace: {
            GLenum mode = getInt();
            proc_glFrontFace(mode);
            break;
        }
        case OPC_glGenBuffers: {
            GLsizei n = getInt();
            GLuint buffers[n];
            int i;
            for (i=0; i<n; ++i) {
                buffers[i] = getInt();
            }
            proc_glGenBuffers(n, buffers);
            break;
        }
        case OPC_glGenerateMipmap: {
            GLenum target = getInt();
            proc_glGenerateMipmap(target);
            break;
        }
        case OPC_glGenFramebuffers: {
            GLsizei n = getInt();
            GLuint framebuffers[n];
            int i;
            for (i=0; i<n; ++i) {
                framebuffers[i] = getInt();
            }
            proc_glGenFramebuffers(n, framebuffers);
            break;          
        }
        case OPC_glGenRenderbuffers: {
            GLsizei n = getInt();
            GLuint renderbuffers[n];
            int i;
            for (i=0; i<n; ++i) {
                renderbuffers[i] = getInt();
            }
            proc_glGenRenderbuffers(n, renderbuffers);
            break;      
        }
        case OPC_glGenTextures: {
            GLsizei n = getInt();
            GLuint textures[n];
            int i;
            for (i=0; i<n; ++i) {
                textures[i] = getInt();
            }
            proc_glGenTextures(n, textures);
            break;
        }
        case OPC_glGetActiveAttrib:     NOT_IMPLEMENTED();
        case OPC_glGetActiveUniform: {
            GLuint program = getInt();
            GLuint index = getInt();
            GLsizei bufsize = getInt();
            uint64_t lengthPtr = getPtr();
            uint64_t sizePtr = getPtr();
            uint64_t typePtr = getPtr();
            uint64_t namePtr = getPtr();
            GLsizei length = lengthPtr ? getInt() : 0;
            GLint   size = sizePtr ? getInt() : 0;
            GLenum type = typePtr ? getInt() : 0;
            const GLchar *name = namePtr ? getString() : NULL;
            GLsizei lengthOut = length;
            GLint   sizeOut = size;
            GLenum typeOut = type;
            GLchar nameOut[bufsize];
            if (name) strncpy(nameOut, name, bufsize);
            proc_glGetActiveUniform(program, index, bufsize,
                                    lengthPtr ? &lengthOut : NULL,
                                    sizePtr ? &sizeOut : NULL,
                                    typePtr ? &typeOut : NULL,
                                    namePtr ? nameOut : NULL);
            if (execFlag) {
                if (lengthPtr && lengthOut != length) {
                    fprintf(stderr, "ERROR: glGetActiveUniform length mismatch\n");
                }
                if (sizePtr && sizeOut != size) {
                    fprintf(stderr, "ERROR: glGetActiveUniform size mismatch\n");
                }
                if (typePtr && typeOut != type) {
                    fprintf(stderr, "ERROR: glGetActiveUniform type mismatch\n");
                }
                if (strcmp(name, nameOut) != 0) {
                    fprintf(stderr, "ERROR: glGetActiveUniform name mismatch\n");
                }
            }
            break;
        }
        case OPC_glGetAttachedShaders: {
            GLuint program = getInt();
            GLuint maxcount = getInt();
            uint64_t countPtr = getPtr();
            uint64_t shadersPtr = getPtr();
            GLsizei count = countPtr ? getInt() : 0;
            GLsizei countOut = count;
            GLuint shadersOut[maxcount];

            proc_glGetAttachedShaders(program, maxcount,
                                      countPtr ? &countOut : NULL,
                                      shadersPtr ? shadersOut : NULL);
            if (execFlag) {
                if (countPtr) {
                    if(countOut != count) {
                        fprintf(stderr, "ERROR: glGetAttachedShaders count mismatch\n");
                    }
                    if (shadersPtr) {
                        int i;
                        for (i=0; i<count; ++i) {
                            if (shadersOut[i] != getInt()) {
                                fprintf(stderr, "ERROR: glGetAttachedShaders shader %d mismatch\n", i);
                            }
                        }
                    }
                }
            }
            break;
        }
        case OPC_glGetAttribLocation: {
            GLuint program = getInt();
            const GLchar* name = (const GLchar*)getString();
                int curVal = proc_glGetAttribLocation(program, name);
                int oldVal = getInt();
                if (printFlag) {
                    sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
                if (execFlag && curVal != oldVal) {
                    fprintf(stderr, "ERROR: glGetAttribLocation return mismatch\n");
                }
                break;
        }
        case OPC_glGetBooleanv: {
                GLenum pname = getInt();
                GLboolean params = getInt();
                proc_glGetBooleanv(pname, params);
                break;
        }
        case OPC_glGetBufferParameteriv: NOT_IMPLEMENTED();
        case OPC_glGetError: {
            GLuint curVal = proc_glGetError();
            GLuint oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendStr(glEnum2str(oldVal));
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: glGetError return mismatch\n");
            }
            break;
        }
        case OPC_glGetFloatv: {
            GLenum pname = getInt();
            const GLfloat *params = getFloatPtr();
            proc_glGetFloatv(pname, params);
            break;
        }
        case OPC_glGetFramebufferAttachmentParameteriv: {
            GLenum target = getInt();
            GLenum attachment = getInt();
            GLenum pname = getInt();
            GLint params = getInt();
            proc_glGetFramebufferAttachmentParameteriv(target, attachment, pname, params);
            break;
        }
        case OPC_glGetIntegerv: {
            GLenum pname = getInt();
            const GLint *params = getIntPtr();
            proc_glGetIntegerv(pname, params);
            break;
        }
        case OPC_glGetProgramiv: {
            GLuint program = getInt();
            GLenum pname = getInt();
            GLint params = getInt();
            proc_glGetProgramiv(program, pname, params);
            break;
        }
        case OPC_glGetProgramInfoLog:   NOT_IMPLEMENTED();
        case OPC_glGetRenderbufferParameteriv: {
            GLenum target = getInt();
            GLenum pname = getInt();
            GLint  params = getInt();
            proc_glGetRenderbufferParameteriv(target, pname, params);
            break;
        }
        case OPC_glGetShaderiv: {
            GLuint shader = getInt();
            GLenum pname = getInt();
            GLint  params = getInt();
            proc_glGetShaderiv(shader, pname, params);
            break;
        }
        case OPC_glGetShaderInfoLog:    NOT_IMPLEMENTED();
        case OPC_glGetShaderPrecisionFormat: NOT_IMPLEMENTED();
        case OPC_glGetShaderSource:     NOT_IMPLEMENTED();
        case OPC_glGetString: {
            GLenum name = getInt();
            const GLubyte *curVal = proc_glGetString(name);
            const GLubyte *oldVal = getString();
            if (printFlag) {
                sb_appendStr(" = \"");
                sb_appendStr(oldVal);
                sb_appendStr("\"");
            }
            if (execFlag && strcmp(curVal, oldVal) != 0) {
                fprintf(stderr, "ERROR: glGetString return mismatch\n");
            }
            break;
        }
        case OPC_glGetTexParameterfv:   NOT_IMPLEMENTED();
        case OPC_glGetTexParameteriv:   NOT_IMPLEMENTED();
        case OPC_glGetUniformfv:        NOT_IMPLEMENTED();
        case OPC_glGetUniformiv:        NOT_IMPLEMENTED();
        case OPC_glGetUniformLocation: {
            GLuint program = getInt();
            const GLchar* name = (const GLchar*)getString();
            int curVal = proc_glGetUniformLocation(program, name);
            int oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: glGetUniformLocation return mismatch\n");
            }
            break;
        }
        case OPC_glGetVertexAttribfv:   NOT_IMPLEMENTED();
        case OPC_glGetVertexAttribiv:   NOT_IMPLEMENTED();
        case OPC_glGetVertexAttribPointerv: NOT_IMPLEMENTED();
        case OPC_glHint :               NOT_IMPLEMENTED();
        case OPC_glIsBuffer     :       NOT_IMPLEMENTED();
        case OPC_glIsEnabled    :       NOT_IMPLEMENTED();
        case OPC_glIsFramebuffer:       NOT_IMPLEMENTED();
        case OPC_glIsProgram    :       NOT_IMPLEMENTED();
        case OPC_glIsRenderbuffer:      NOT_IMPLEMENTED();
        case OPC_glIsShader     :       NOT_IMPLEMENTED();
        case OPC_glIsTexture    :       NOT_IMPLEMENTED();
        case OPC_glLineWidth    :       NOT_IMPLEMENTED();
        case OPC_glLinkProgram: {
            GLuint program = getInt();
            proc_glLinkProgram(program);
            break;
        }
        case OPC_glPixelStorei: {
            GLenum pname = getInt();
            GLint param = getInt();
            proc_glPixelStorei(pname, param);
            break;
        }
        case OPC_glPolygonOffset: {
            GLfloat factor = getFloat();
            GLfloat units = getFloat();
            proc_glPolygonOffset(factor, units);
            break;
        }
        case OPC_glReadPixels:          NOT_IMPLEMENTED();
        case OPC_glReleaseShaderCompiler: NOT_IMPLEMENTED();
        case OPC_glRenderbufferStorage: {
            GLenum target = getInt();
            GLenum internalformat = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            proc_glRenderbufferStorage(target, internalformat, width, height);
            break;
        }
        case OPC_glSampleCoverage:      NOT_IMPLEMENTED();
        case OPC_glScissor: {
            GLint x = getInt();
            GLint y = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            proc_glScissor(x, y, width, height);
            break;
        }
        case OPC_glShaderBinary:        NOT_IMPLEMENTED();
        case OPC_glShaderSource: {
            GLuint shader = getInt();
            GLsizei count= getInt();
            const GLchar* string[count];
            GLint length[count];
            int i;
            for (i=0; i<count; ++i) {
                int len = getInt();
                if (len > 0) {
                    length[i] = len;
                    string[i] = (GLchar*)getBytes();
                }
                else {
                    length[i] = -1;
                    string[i] = (GLchar*)getString();
                }
            }
            proc_glShaderSource(shader, count, string, length);
            break;
        }
        case OPC_glStencilFunc:         NOT_IMPLEMENTED();
        case OPC_glStencilFuncSeparate: NOT_IMPLEMENTED();
        case OPC_glStencilMask:         NOT_IMPLEMENTED();
        case OPC_glStencilMaskSeparate: NOT_IMPLEMENTED();
        case OPC_glStencilOp    :       NOT_IMPLEMENTED();
        case OPC_glStencilOpSeparate:   NOT_IMPLEMENTED();
        case OPC_glTexImage2D: {
            GLenum target = getInt();
            GLint level = getInt();
            GLint internalformat = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            GLint border = getInt();
            GLenum format = getInt();
            GLenum type = getInt();
            const GLvoid* pixels = getBytes();
            proc_glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);

            if (dumpTexturesFlag) {
                char fName[32];
                snprintf(fName, sizeof(fName), "%s/texture%06d.png", DIR_TEXTURES, ++texCount);
                int err = savePNG(fName, format, type, width, height, pixels);
                printf("%s %s\n", fName, err ? "FAIL" : "OK");
            }
            break;
        }
        case OPC_glTexParameterf:       NOT_IMPLEMENTED();
        case OPC_glTexParameterfv:      NOT_IMPLEMENTED();
        case OPC_glTexParameteri: {
            GLenum target = getInt();
            GLenum pname = getInt();
            GLint param = getInt();
            proc_glTexParameteri(target, pname, param);
            break;
        }
        case OPC_glTexParameteriv:      NOT_IMPLEMENTED();
        case OPC_glTexSubImage2D: {
            GLenum target = getInt();
            GLint level = getInt();
            GLint xoffset = getInt();
            GLint yoffset = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            GLenum format = getInt();
            GLenum type = getInt();
            const GLvoid* pixels = getBytes();
            proc_glTexSubImage2D(target, level, xoffset, yoffset, width, height, format, type, pixels);
            break;
        }
        case OPC_glUniform1f: {
            GLint location = getInt();
            GLfloat x = getFloat();
            proc_glUniform1f(location, x);
            break;
        }
        case OPC_glUniform1fv: {
                GLint location = getInt();
                GLsizei count = getInt();
                const GLfloat* v = (const GLfloat*)getBytes();
                proc_glUniform1fv(location, count, v);
                break;
        }
        case OPC_glUniform1i: {
            GLint location = getInt();
            GLint x = getInt();
            proc_glUniform1i(location, x);
            break;
        }
        case OPC_glUniform1iv:          NOT_IMPLEMENTED();
        case OPC_glUniform2f: {
            GLint location = getInt();
            GLfloat x = getFloat();
            GLfloat y = getFloat();
            proc_glUniform2f(location, x, y);
            break;
        }
        case OPC_glUniform2fv:          NOT_IMPLEMENTED();
        case OPC_glUniform2i    :       NOT_IMPLEMENTED();
        case OPC_glUniform2iv:          NOT_IMPLEMENTED();
        case OPC_glUniform3f: {
            GLint location = getInt();
            GLfloat x = getFloat();
            GLfloat y = getFloat();
            GLfloat z = getFloat();
            proc_glUniform3f(location, x, y, z);
            break;
        }
        case OPC_glUniform3fv:          NOT_IMPLEMENTED();
        case OPC_glUniform3i    :       NOT_IMPLEMENTED();
        case OPC_glUniform3iv:          NOT_IMPLEMENTED();
        case OPC_glUniform4f: {
            GLint location = getInt();
            GLfloat x = getFloat();
            GLfloat y = getFloat();
            GLfloat z = getFloat();
            GLfloat w = getFloat();
            proc_glUniform4f(location, x, y, z, w);
            break;
        }
        case OPC_glUniform4fv: {
            GLint location = getInt();
            GLsizei count = getInt();
            const GLfloat* v = (const GLfloat*)getBytes();
            proc_glUniform4fv(location, count, v);
            break;
        }
        case OPC_glUniform4i    :       NOT_IMPLEMENTED();
        case OPC_glUniform4iv:          NOT_IMPLEMENTED();
        case OPC_glUniformMatrix2fv: {
                GLint location = getInt();
                GLsizei count = getInt();
                GLboolean transpose = getInt();
                const GLfloat* value = (const GLfloat*)getBytes();
                proc_glUniformMatrix2fv(location, count, transpose, value);
                break;
        }
        case OPC_glUniformMatrix3fv: {
                GLint location = getInt();
                GLsizei count = getInt();
                GLboolean transpose = getInt();
                const GLfloat* value = (const GLfloat*)getBytes();
                proc_glUniformMatrix3fv(location, count, transpose, value);
                break;
        }
        case OPC_glUniformMatrix4fv: {
            GLint location = getInt();
            GLsizei count = getInt();
            GLboolean transpose = getInt();
            const GLfloat* value = (const GLfloat*)getBytes();
            proc_glUniformMatrix4fv(location, count, transpose, value);
            break;
        }
        case OPC_glUseProgram: {
            GLuint program = getInt();
            proc_glUseProgram(program);
            break;
        }
        case OPC_glValidateProgram: {
            GLuint program = getInt();
            proc_glValidateProgram(program);
            break;
        }
        case OPC_glVertexAttrib1f:      NOT_IMPLEMENTED();
        case OPC_glVertexAttrib1fv:     NOT_IMPLEMENTED();
        case OPC_glVertexAttrib2f:      NOT_IMPLEMENTED();
        case OPC_glVertexAttrib2fv:     NOT_IMPLEMENTED();
        case OPC_glVertexAttrib3f:      NOT_IMPLEMENTED();
        case OPC_glVertexAttrib3fv:     NOT_IMPLEMENTED();
        case OPC_glVertexAttrib4f:      NOT_IMPLEMENTED();
        case OPC_glVertexAttrib4fv:     NOT_IMPLEMENTED();
        case OPC_glVertexAttribPointer: {
            GLuint indx = getInt();
            GLint size = getInt();
            GLenum type = getInt();
            GLboolean normalized = getInt();
            GLsizei stride = getInt();
            uint64_t ptr = getPtr();
//            if (execFlag && ptr) {
//                fprintf(stderr, "ERROR: glVertexAttribPointer ptr not implemented\n");
//                exit(1);
//            }
            proc_glVertexAttribPointer(indx, size, type, normalized, stride, (void*)ptr);
            break;
        }
        case OPC_glViewport: {
            GLint x = getInt();
            GLint y = getInt();
            GLsizei width = getInt();
            GLsizei height = getInt();
            proc_glViewport(x, y, width, height);
            break;
        }
#if MACOSX
            case OPC_glBegin: {
                GLenum mode = getInt();
                proc_glBegin(mode);
                break;
            }
            case OPC_glEnd:
                proc_glEnd();
                break;
            case OPC_glIsRenderbufferEXT:       NOT_IMPLEMENTED();
            case OPC_glBindRenderbufferEXT:     NOT_IMPLEMENTED();
            case OPC_glDeleteRenderbuffersEXT:  NOT_IMPLEMENTED();
            case OPC_glGenRenderbuffersEXT:     NOT_IMPLEMENTED();
            case OPC_glRenderbufferStorageEXT:  NOT_IMPLEMENTED();
            case OPC_glGetRenderbufferParameterivEXT:   NOT_IMPLEMENTED();
            case OPC_glIsFramebufferEXT:        NOT_IMPLEMENTED();
            case OPC_glBindFramebufferEXT: {
                GLenum target = getInt();
                GLuint framebuffer = getInt();
                proc_glBindFramebufferEXT(target, framebuffer);
                break;
            }
            case OPC_glDeleteFramebuffersEXT: {
                GLsizei n = getInt();
                GLuint framebuffers[n];
                int i;
                for (i=0; i<n; ++i) {
                    framebuffers[i] = getInt();
                }
                proc_glDeleteFramebuffersEXT(n, framebuffers);
                break;
            }
            case OPC_glGenFramebuffersEXT: {
                GLsizei n = getInt();
                GLuint framebuffers[n];
                int i;
                for (i=0; i<n; ++i) {
                    framebuffers[i] = getInt();
                }
                proc_glGenFramebuffersEXT(n, framebuffers);
                break;
            }
            case OPC_glCheckFramebufferStatusEXT: {
                GLenum target = getInt();
                GLenum curVal = proc_glCheckFramebufferStatusEXT(target);
                GLenum oldVal = getInt();
                if (printFlag) {
                    sb_appendStr(" = ");
                    sb_appendStr(glEnum2str(oldVal));
                }
                if (execFlag && curVal != oldVal) {
                    fprintf(stderr, "ERROR: glCheckFramebufferStatusEXT return mismatch\n");
                }
                break;
            }
            case OPC_glFramebufferTexture1DEXT: NOT_IMPLEMENTED();
            case OPC_glFramebufferTexture2DEXT: {
                GLenum target = getInt();
                GLenum attachment = getInt();
                GLenum textarget = getInt();
                GLuint texture = getInt();
                GLint level = getInt();
                proc_glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
                break;
            }
            case OPC_glFramebufferTexture3DEXT: NOT_IMPLEMENTED();
            case OPC_glFramebufferRenderbufferEXT:      NOT_IMPLEMENTED();
            case OPC_glGetFramebufferAttachmentParameterivEXT:  NOT_IMPLEMENTED();
            case OPC_glGenerateMipmapEXT:       NOT_IMPLEMENTED();
                
#endif /* MACOSX */
#if linux
        case OPC_eglGetError:   NOT_IMPLEMENTED();          
        case OPC_eglGetDisplay: {
            EGLDisplay curVal = proc_eglGetDisplay((EGLNativeDisplayType)getPtr());
            EGLDisplay oldVal = (EGLDisplay)getPtr();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendPtr(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglGetDisplay return mismatch\n");
            }
            break;
        }
        case OPC_eglInitialize: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            const GLint *major = getIntPtr();
            const GLint *minor = getIntPtr();
            EGLBoolean curVal = proc_eglInitialize(dpy, major, minor);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendBool(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglInitialize return mismatch\n");
            }
            break;
        }
        case OPC_eglTerminate:          NOT_IMPLEMENTED();
        case OPC_eglQueryString: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLint name = getInt();
            const char *curVal = proc_eglQueryString(dpy, name);
            const char *oldVal = getString();
            if (printFlag) {
                sb_appendStr(" = \"");
                sb_appendStr(oldVal);
                sb_appendStr("\"");
            }
            if (execFlag && strcmp(curVal, oldVal) != 0) {
                fprintf(stderr, "ERROR: eglQueryString return mismatch\n");
            }
            break;
        }
        case OPC_eglGetConfigs:         NOT_IMPLEMENTED();
        case OPC_eglChooseConfig: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLint attrib_list[64]; /* XXX static size */
            int i;
            for (i=0;;) {
                if (i >= sizeof(attrib_list)/sizeof(EGLint)) {
                    fprintf(stderr, "FATAL: eglChooseConfig too many attributes\n");
                    exit(1);
                }
                if ((attrib_list[i++] = getInt()) == EGL_NONE) break;
                attrib_list[i++] = getInt();
            }
            EGLint config_size = getInt();
            EGLint num_config = getInt();
            EGLConfig configs[config_size];
            for (i=0; i<num_config && i<config_size; ++i) {
                configs[i] = (EGLConfig)getPtr();
            }
            EGLBoolean curVal = proc_eglChooseConfig(dpy, attrib_list, configs, config_size, num_config);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendBool(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglChooseConfig return mismatch\n");
            }
            break;
        }
        case OPC_eglGetConfigAttrib: {
            EGLBoolean curVal = proc_eglGetConfigAttrib((EGLDisplay)getPtr(), (EGLConfig)getPtr(), getInt(), getInt());
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendBool(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglGetConfigAttrib return mismatch\n");
            }
            break;
        }
        case OPC_eglCreateWindowSurface: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLConfig config = (EGLConfig)getPtr();
            EGLNativeWindowType win = (EGLNativeWindowType)getPtr();
            EGLint attrib_list[64]; /* XXX static size */
            int i;
            for (i=0;;) {
                if (i >= sizeof(attrib_list)/sizeof(EGLint)) {
                    fprintf(stderr, "FATAL: eglCreateWindowSurface too many attributes\n");
                    exit(1);
                }
                if ((attrib_list[i++] = getInt()) == EGL_NONE) break;
                attrib_list[i++] = getInt();
            }
            EGLSurface curVal = proc_eglCreateWindowSurface(dpy, config, win, attrib_list);
            EGLSurface oldVal = (EGLSurface)getPtr();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendPtr(execFlag ? curVal : oldVal);
            }
            if (execFlag) {
                if (eglSurfaceMap == NULL) eglSurfaceMap = createMap();
                putMap(eglSurfaceMap, oldVal, curVal);
            }
            break;
        }
        case OPC_eglCreatePbufferSurface: NOT_IMPLEMENTED();
        case OPC_eglCreatePixmapSurface: NOT_IMPLEMENTED();
        case OPC_eglDestroySurface:     NOT_IMPLEMENTED();
        case OPC_eglQuerySurface:       NOT_IMPLEMENTED();
        case OPC_eglBindAPI: {
            EGLBoolean curVal = proc_eglBindAPI(getInt());
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendBool(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglGetDisplay return mismatch\n");
            }
            break;
        }
        case OPC_eglQueryAPI:           NOT_IMPLEMENTED();
        case OPC_eglWaitClient:         NOT_IMPLEMENTED();
        case OPC_eglReleaseThread:      NOT_IMPLEMENTED();
        case OPC_eglCreatePbufferFromClientBuffer: NOT_IMPLEMENTED();
        case OPC_eglSurfaceAttrib: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLSurface surface = (EGLSurface)getPtr();
            if (execFlag) surface = (EGLSurface)getMap(eglSurfaceMap, surface);
            EGLint attribute = getInt();
            EGLint value = getInt();
            EGLBoolean curVal = proc_eglSurfaceAttrib(dpy, surface, attribute, value);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendBool(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglSurfaceAttrib return mismatch\n");
            }
            break;
        }
        case OPC_eglBindTexImage:       NOT_IMPLEMENTED();
        case OPC_eglReleaseTexImage:    NOT_IMPLEMENTED();
        case OPC_eglCreateContext: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLConfig config = (EGLConfig)getPtr();
            EGLContext context = (EGLContext)getPtr();
            if (execFlag && context) context = (EGLContext)getMap(eglContextMap, context);
            EGLint attrib_list[64]; /* XXX static size */
            int i;
            for (i=0;;) {
                if (i >= sizeof(attrib_list)/sizeof(EGLint)) {
                    fprintf(stderr, "FATAL: eglCreateContext too many attributes\n");
                    exit(1);
                }
                if ((attrib_list[i++] = getInt()) == EGL_NONE) break;
                attrib_list[i++] = getInt();
            }
            EGLContext curVal = proc_eglCreateContext(dpy, config, context, attrib_list);
            EGLContext oldVal = (EGLContext)getPtr();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendPtr(execFlag ? curVal : oldVal);
            }
            if (execFlag) {
                if (eglContextMap == NULL) eglContextMap = createMap();
                putMap(eglContextMap, oldVal, curVal);
            }
            break;
        }
        case OPC_eglDestroyContext: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLContext context = (EGLContext)getPtr();
            if (execFlag) context = (EGLContext)getMap(eglContextMap, context);
            EGLBoolean curVal = proc_eglDestroyContext(dpy, context);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglDestroyContext return mismatch\n");
            }
            break;
        }
        case OPC_eglMakeCurrent: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLSurface draw = (EGLSurface)getPtr();
            if (execFlag) draw = (EGLSurface)getMap(eglSurfaceMap, draw);
            EGLSurface read = (EGLSurface)getPtr();
            if (execFlag) read = (EGLSurface)getMap(eglSurfaceMap, read);
            EGLContext ctx = (EGLContext)getPtr();
            if (execFlag) ctx = (EGLContext)getMap(eglContextMap, ctx);
            EGLBoolean curVal = proc_eglMakeCurrent(dpy, draw, read, ctx);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglMakeCurrent return mismatch\n");
            }
            break;
        }
        case OPC_eglGetCurrentContext:  NOT_IMPLEMENTED();
        case OPC_eglGetCurrentSurface:  NOT_IMPLEMENTED();
        case OPC_eglGetCurrentDisplay:  NOT_IMPLEMENTED();
        case OPC_eglQueryContext:       NOT_IMPLEMENTED();
        case OPC_eglWaitGL:             NOT_IMPLEMENTED();
        case OPC_eglWaitNative:         NOT_IMPLEMENTED();
        case OPC_eglSwapBuffers: {
            EGLDisplay dpy = (EGLDisplay)getPtr();
            EGLSurface surface = (EGLSurface)getPtr();
            if (execFlag) surface = (EGLSurface)getMap(eglSurfaceMap, surface);
            
            // not working
            if (frames == 1) {
                readPixels();
            }
            
            EGLBoolean curVal = proc_eglSwapBuffers(dpy, surface);
            EGLBoolean oldVal = getInt();
            if (printFlag) {
                sb_appendStr(" = ");
                sb_appendInt(oldVal);
            }
            if (execFlag && curVal != oldVal) {
                fprintf(stderr, "ERROR: eglSwapBuffers return mismatch\n");
            }
            break;
        }
        case OPC_eglCopyBuffers:        NOT_IMPLEMENTED();
#endif /*linux */

        case OPC_NONE:
        case OPC_EOF:
            return;
        default:
            NOT_IMPLEMENTED();
            return;
        }
        
        getTime(&tbgn, &tend);
        if (printFlag) {
            if (printThread == (uint64_t)0 || (printThread == curThread && cmd != OPC_THREAD)) {
                fprintf(stdout, "%llu %10llu\t%s\n", tbgn, tend-tbgn, stringBuffer);
            }
        }
        
        if (cmd == OPC_eglSwapBuffers) {
            if (fpsFlag) {
                fps_newFrame(curFrame, tend);
            }
            ++curFrame;
            --frames;
        }
    }
}

#define ICMD_NONE       0
#define ICMD_QUIT       1
#define ICMD_INVALID    2
#define ICMD_HELP       3
#define ICMD_NEXTFRAME  4
#define ICMD_WRITE      5

#define PIXEL_SIZE 4
static uint32_t wndWidth, wndHeight, wndDataSize;
static void *wndData;

static void
readPixels()
{
    GLint viewPort[4];
    glGetIntegerv(GL_VIEWPORT, viewPort);
    
    wndWidth = viewPort[2];
    wndHeight = viewPort[3];
    GLint nsize = wndWidth * wndHeight * PIXEL_SIZE;
    if (wndDataSize < nsize) {
        wndDataSize = nsize;
        wndData = realloc(wndData, wndDataSize);
        if (wndData == NULL) {
            fprintf(stderr, "ERROR: can't allocate memory for pixels\n");
            return;
        }
    }
     
    glFinish();
    
    glPixelStorei(GL_PACK_ALIGNMENT,1); 
    glReadPixels(viewPort[0], viewPort[1], viewPort[2], viewPort[3], GL_RGBA, GL_UNSIGNED_BYTE, wndData);
    if (glGetError() != GL_NO_ERROR) {
        fprintf(stderr, "ERROR: can't read pixels\n");
        return;
    }
}

#include <png.h>

static void *
convertTexture565(uint32_t width, uint32_t height, const void *data)
{
    int pixcnt = width * height;
    uint8_t *ldata = (uint8_t*)malloc(pixcnt*3);
    if (ldata == NULL) return NULL;

    uint16_t *src = (uint16_t*)data;
    uint8_t  *dst = ldata;
    int i;
    for (i=0; i<pixcnt; ++i) {
        uint16_t pix = *src++;
        *dst++ = (pix & 0xf800) >> 8;
        *dst++ = (pix & 0x07e0) >> 3;
        *dst++ = (pix & 0x001f) << 3;
    }
    return ldata;
}

static int
savePNG(char *fName, GLenum format, GLenum type, uint32_t width, uint32_t height, const void *data)
{
    int png_type = 0;
    int pixel_size = 0;
    void *ldata = NULL;
    switch (format) {
        case GL_RGB:
            if (type == GL_UNSIGNED_SHORT_5_6_5) {
                ldata = convertTexture565(width, height, data);
                if (ldata == NULL) return 1;
                data = ldata;
            }
            else if (type != GL_UNSIGNED_BYTE) {
                fprintf(stderr, "ERROR: savePNG: unknown type %s\n", glEnum2str(type));
                return 1;
            }
            png_type = PNG_COLOR_TYPE_RGB;
            pixel_size = 3;
            break;
        case GL_RGBA:
            png_type = PNG_COLOR_TYPE_RGBA;
            pixel_size = 4;
            break;
        case GL_LUMINANCE:
            png_type = PNG_COLOR_TYPE_GRAY;
            pixel_size = 1;
            break;
        case GL_LUMINANCE_ALPHA:
            png_type = PNG_COLOR_TYPE_GRAY_ALPHA;
            pixel_size = 2;
            break;
        case GL_ALPHA:
            png_type = PNG_COLOR_MASK_ALPHA;
            pixel_size = 1;
            break;
        default:
            fprintf(stderr, "ERROR: savePNG: unknown format %s\n", glEnum2str(format));
            return 1;
    }
    
    FILE *file = fopen(fName, "w+");
    if (file == NULL) {
        fprintf(stderr, "ERROR: can't write to file: %s\n", fName);
        return 1;
    }
    png_structp png = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    if (png == NULL) {
        fprintf(stderr, "ERROR: can't create PNG\n");
        fclose(file);
        return 1;
    }
    
    png_infop info = png_create_info_struct(png);
    if (info == NULL) {
        fprintf(stderr, "ERROR: can't create PNG info\n");
    }
    else if (setjmp(png_jmpbuf(png))) {
        fprintf(stderr, "ERROR: libpng error\n");
    }
    else {
        png_init_io(png, file);
        
        png_set_IHDR(png, info, width, height, 8,
                     png_type, PNG_INTERLACE_NONE,
                     PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
        
        png_byte *rows[height];
        int i;
        for (i=0; i<height; ++i) {
            rows[i] = (png_byte*)data + (height - 1 - i) * width * pixel_size;
        }
        
        png_set_rows(png, info, rows);
        png_write_png(png, info, PNG_TRANSFORM_IDENTITY, NULL);
    }
    
    png_destroy_write_struct(&png, &info);    
    fclose(file);
    if (ldata) free(ldata);
    
    return 0;
}

static void
interact()
{
    for (;;) {
        int repeat = -1;
        int cmd = 0;
        write(1, "> ", 2);
        for (;;) {
            char c;
            if (read(0, &c, sizeof(char)) != sizeof(char)) return;
            if (cmd == ICMD_NONE && c >= '0' && c <= '9') {
                if (repeat < 0) repeat = 0;
                repeat = repeat * 10 + (c-'0');
            }
            else if (c == '\n') {
                break;
            }
            else if (cmd != ICMD_NONE) {
                cmd = ICMD_INVALID;
            }
            else {
                switch (c) {
                    case 'q': cmd = ICMD_QUIT; break;
                    case 'h':
                    case '?': cmd = ICMD_HELP; break;
                    case 'n': cmd = ICMD_NEXTFRAME; break;
                    case 'w': cmd = ICMD_WRITE; break;
                    default:  cmd = ICMD_INVALID; break;
                }
            }
        }
        
        if (repeat == -1 && cmd != ICMD_NONE) repeat = 1;
        
        switch (cmd) {
            case ICMD_NONE:
                if (repeat >= 0) {
                    fprintf(stdout, "ERROR: no command\n");
                }
                break;
            case ICMD_QUIT:
                return;
            case ICMD_HELP:
                fprintf(stdout, "Commands:\n"
                        "\tq - quit\n"
                        "\t? - help\n"
                        "\t[<k>]n - skip <k> frames\n"
                        );
                break;
            case ICMD_NEXTFRAME:
                process(repeat);
                fprintf(stdout, "current frame: %d\n", curFrame);
                break;
            case ICMD_WRITE: {
                char fName[32];
                snprintf(fName, sizeof(fName), "frame%06d.png", curFrame);
                savePNG(fName, GL_RGBA, GL_UNSIGNED_BYTE, wndWidth, wndHeight, wndData);
                fprintf(stdout, "%s written\n", fName);
                break;
            }
            default:
            case ICMD_INVALID:
                fprintf(stderr, "ERROR: unknown command\n");
                break;
        }
    }
}

static void
usage(const char *progname)
{
    fprintf(stdout, "Usage: %s [-print][-replay][-nofps] [file]\n"
                    "\t- if no flags are specified the program enters interactive mode\n",
                    progname);
}

int
main(int argc, const char *argv[])
{
    int i;
    const char *fileName = NULL;
    int interactive = 1;

    for (i=1; i<argc; ++i) {
        const char *arg = argv[i];
        if (*arg == '-') {
            if (strcmp(arg, "-replay") == 0) {
                interactive = 0;
                execFlag = 1;
            }
            else if (strcmp(arg, "-print") == 0) {
                interactive = 0;
                printFlag = 1;
            }
            else if (strcmp(arg, "-printAll") == 0) {
                interactive = 0;
                printFlag = 1;
                printArrays = 1;
            }
            else if (strcmp(arg, "-nofps") == 0) {
                fpsFlag = 0;
            }
            else if (strcmp(arg, "-thread") == 0) {
                if (++i >= argc) {
                    usage(argv[0]);
                    return;
                }
                printThread = atoll(argv[i]);
            }            
            else if (strcmp(arg, "-dumptextures") == 0) {
                interactive = 0;
                dumpTexturesFlag = 1;
                if (mkdir(DIR_TEXTURES, S_IRUSR|S_IWUSR|S_IXUSR|S_IRGRP|S_IXGRP|S_IROTH|S_IXOTH)) {
                    if (errno != EEXIST) {
                        fprintf(stderr, "ERROR: can't create directory %s", DIR_TEXTURES);
                    }
                }
            }
            else {
                usage(argv[0]);
                return;
            }
        }
        else {
            fileName = arg;
            break;
        }
    }

    sb_init();
    iolib_init(IO_READ, fileName);
    
    if (interactive) {
        fpsFlag = 0;
        execFlag = 1;
        
        wndDataSize = 0;
        wndData = NULL;
        
        interact();
    }
    else {
        process(-1);
    }
    
    if (fpsFlag) {
        fps_total();
    }
    iolib_fini();
    sb_fini();
    
    return 0;
}
