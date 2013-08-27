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
#include <alloca.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include "os.h"
#include "iolib.h"
#include "trace.h"

static int tLevel = trcLevel;
static void *libGLESv2 = NULL;

/*
 *    Init/fini
 */

static void init() __attribute__ ((constructor));
static void 
init()
{
    libGLESv2 = dlopen("libGLESv2.so", RTLD_LAZY);
}


/*
 *   OpenGL ES
 */

#ifndef GL_BGRA
#  ifdef GL_BGRA_EXT
#    define GL_BGRA GL_BGRA_EXT
#  else
#    define GL_BGRA 0x80E1
#  endif
#endif

typedef void             GLvoid;
typedef char             GLchar;

static int
glSizeof(GLenum type)
{
    switch (type) {
    case GL_BYTE:               return sizeof(GLbyte);
    case GL_UNSIGNED_BYTE:      return sizeof(GLubyte);
    case GL_SHORT:              return sizeof(GLshort);
    case GL_UNSIGNED_SHORT:     return sizeof(GLushort);
    case GL_INT:                return sizeof(GLint);
    case GL_UNSIGNED_INT:       return sizeof(GLuint);
    case GL_FLOAT:              return sizeof(GLfloat);
    }
    fprintf(stderr, "FATAL: glSizeof: unknown type: 0x%x\n", type);
    exit(1);
    
    return 0;
}

static int
glCountof(GLenum format)
{
    switch (format) {
    case GL_ALPHA:      return 1;
    case GL_RGB:        return 3;
    case GL_RGBA:       return 4;
    case GL_BGRA:       return 4;
    }

    fprintf(stderr, "FATAL: glCountof: unknown format: 0x%x\n", format);
    exit(1);
    return 0;
}

static int
glElementSize(GLenum format, GLenum type)
{
    switch (type) {
    case GL_UNSIGNED_BYTE:
        switch (format) {
        case GL_ALPHA:          return 1;
        case GL_RGB:            return 3;
        case GL_RGBA:
        case GL_BGRA:           return 4;
        case GL_LUMINANCE:      return 1;       /* XXX validate */
        case GL_LUMINANCE_ALPHA: return 2;      /* XXX validate */
        }
        fprintf(stderr, "FATAL: glElementSize: unknown format: 0x%x\n", format);
        exit(1);
        return 0;
    case GL_UNSIGNED_SHORT_4_4_4_4:
    case GL_UNSIGNED_SHORT_5_5_5_1:
    case GL_UNSIGNED_SHORT_5_6_5:
        return 2; /* XXX validate */
    }
    fprintf(stderr, "FATAL: glElementSize: unknown type: 0x%x\n", type);
    exit(1);
    return 0;
}

/* XXX use glGet(GL_MAX_VERTEX_ATTRIBS) */
#define MAX_VERTEX_ATTRIBS      128     
typedef struct VertexAttrib_t {
    GLboolean   enabled;
    GLint       size;
    GLenum      type;
    GLboolean   normalized;
    GLsizei     stride;
    const void  *pointer;
} VertexAttrib_t;

static VertexAttrib_t vertexAttrib[MAX_VERTEX_ATTRIBS];

static void
putVertexAttrib(int count)
{
    count = count/6 * 4; /* XXX hack for quads only */
    char *buf = alloca(count * 4 * sizeof(float));
    
    int i, j, k;
    for (i=0; i<MAX_VERTEX_ATTRIBS; ++i) {
        if (!vertexAttrib[i].enabled) continue;
        char *src = (char*)vertexAttrib[i].pointer;
        char *dst = buf;
        int elemsz = glSizeof(vertexAttrib[i].type) * vertexAttrib[i].size;
        for (j=0; j<count; ++j) {
            for (k=0; k<elemsz; ++k) *dst++ = *src++;
            if (vertexAttrib[i].stride > 0) {
                src += vertexAttrib[i].stride - elemsz;
            }
        }
        putBytes(buf, dst - buf);
    }
}

static GLuint arrayBufferBinding = 0;
static GLuint elementArrayBufferBinding = 0;

DEF(void, glActiveTexture)(GLenum texture)
{
    GLPROLOG(glActiveTexture);
    
    putCmd(OPC_glActiveTexture);
    putInt(texture);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(texture);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glAttachShader) (GLuint program, GLuint shader)
{
    GLPROLOG(glAttachShader);
    
    putCmd(OPC_glAttachShader);
    putInt(program);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBindAttribLocation) (GLuint program, GLuint index, const GLchar* name)
{
    GLPROLOG(glBindAttribLocation);
    
    putCmd(OPC_glBindAttribLocation);
    putInt(program);
    putInt(index);
    putString(name);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, index, name);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBindBuffer) (GLenum target, GLuint buffer)
{
    GLPROLOG(glBindBuffer);
    
    putCmd(OPC_glBindBuffer);
    putInt(target);
    putInt(buffer);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, buffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    if (target == GL_ARRAY_BUFFER) {
        arrayBufferBinding = buffer;
    }
    else if (target == GL_ELEMENT_ARRAY_BUFFER) {
        elementArrayBufferBinding = buffer;
    }

    GLEPILOG();
}    

DEF(void, glBindFramebuffer) (GLenum target, GLuint framebuffer)
{
    GLPROLOG(glBindFramebuffer);
    
    putCmd(OPC_glBindFramebuffer);
    putInt(target);
    putInt(framebuffer);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, framebuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBindRenderbuffer) (GLenum target, GLuint renderbuffer)
{
    GLPROLOG(glBindRenderbuffer);
    
    putCmd(OPC_glBindRenderbuffer);
    putInt(target);
    putInt(renderbuffer);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, renderbuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBindTexture) (GLenum target, GLuint texture)
{
    GLPROLOG(glBindTexture);
    
    putCmd(OPC_glBindTexture);
    putInt(target);
    putInt(texture);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, texture);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBlendColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    GLPROLOG(glBlendColor);

    putCmd(OPC_glBlendColor);
    putFloat(red);
    putFloat(green);
    putFloat(blue);
    putFloat(alpha);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLclampf, GLclampf, GLclampf, GLclampf))orig)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glBlendEquation) (GLenum mode)
{
    GLPROLOG(glBlendEquation);
    
    putCmd(OPC_glBlendEquation);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glBlendEquationSeparate) (GLenum modeRGB, GLenum modeAlpha)
{
    GLPROLOG(glBlendEquationSeparate);

    putCmd(OPC_glBlendEquationSeparate);
    putInt(modeRGB);
    putInt(modeAlpha);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(modeRGB, modeAlpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glBlendFunc) (GLenum sfactor, GLenum dfactor)
{
    GLPROLOG(glBlendFunc);
    
    putCmd(OPC_glBlendFunc);
    putInt(sfactor);
    putInt(dfactor);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(sfactor, dfactor);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBlendFuncSeparate) (GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
    GLPROLOG(glBlendFuncSeparate);
    
    putCmd(OPC_glBlendFuncSeparate);
    putInt(srcRGB);
    putInt(dstRGB);
    putInt(srcAlpha);
    putInt(dstAlpha);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(srcRGB, dstRGB, srcAlpha, dstAlpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glBufferData) (GLenum target, GLsizeiptr size, const GLvoid* data, GLenum usage)
{
    GLPROLOG(glBufferData);
    
    putCmd(OPC_glBufferData);
    putInt(target);
    putInt(size);
    putBytes(data, size);
    putInt(usage);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, size, data, usage);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glBufferSubData) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid* data)
{
    GLPROLOG(glBufferSubData);

    putCmd(OPC_glBufferSubData);
    putInt(target);
    putInt(offset);
    putInt(size);
    putBytes(data, size);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, offset, size, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(GLenum, glCheckFramebufferStatus) (GLenum target)
{
    GLPROLOG(glCheckFramebufferStatus);
    
    putCmd(OPC_glCheckFramebufferStatus);
    putInt(target);
    
    uint64_t bgn = gethrtime();
    GLenum res = (*(GLenum(*)())orig)(target);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(void, glClear) (GLbitfield mask)
{
    GLPROLOG(glClear);
    
    putCmd(OPC_glClear);
    putInt(mask);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mask);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glClearColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    GLPROLOG(glClearColor);
    
    putCmd(OPC_glClearColor);
    putFloat(red);
    putFloat(green);
    putFloat(blue);
    putFloat(alpha);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLclampf, GLclampf, GLclampf, GLclampf))orig)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

GL_APICALL void GL_APIENTRY 
glClearDepthf (GLclampf depth)
{
    GLPROLOG(glClearDepthf);
    
    putCmd(OPC_glClearDepthf);
    putFloat(depth);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLclampf))orig)(depth);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glClearStencil) (GLint s)
{
    GLPROLOG(glClearStencil);
    
    putCmd(OPC_glClearStencil);
    putInt(s);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(s);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glColorMask) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    GLPROLOG(glColorMask);
    
    putCmd(OPC_glColorMask);
    putInt(red);
    putInt(green);
    putInt(blue);
    putInt(alpha);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glCompileShader) (GLuint shader)
{
    GLPROLOG(glCompileShader);
    
    putCmd(OPC_glCompileShader);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glCompressedTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid* data)
{
    GLPROLOG(glCompressedTexImage2D);

    putCmd(OPC_glCompressedTexImage2D);
    putInt(target);
    putInt(level);
    putInt(internalformat);
    putInt(width);
    putInt(height);
    putInt(border);
    putInt(imageSize);
    putBytes(data, imageSize);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, internalformat, width, height, border, imageSize, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glCompressedTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid* data)
{
    GLPROLOG(glCompressedTexSubImage2D);

    putCmd(OPC_glCompressedTexSubImage2D);
    putInt(target);
    putInt(level);
    putInt(xoffset);
    putInt(yoffset);
    putInt(width);
    putInt(height);
    putInt(format);
    putInt(imageSize);
    putBytes(data, imageSize);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, xoffset, yoffset, width, height, format, imageSize, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glCopyTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    GLPROLOG(glCopyTexImage2D);
    
    putCmd(OPC_glCopyTexImage2D);
    putInt(target);
    putInt(level);
    putInt(internalformat);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    putInt(border);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, internalformat, x, y, width, height, border);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glCopyTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLPROLOG(glCopyTexSubImage2D);
    
    putCmd(OPC_glCopyTexSubImage2D);
    putInt(target);
    putInt(level);
    putInt(xoffset);
    putInt(yoffset);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, xoffset, yoffset, x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(GLuint, glCreateProgram) (void)
{
    GLPROLOG(glCreateProgram);
    
    putCmd(OPC_glCreateProgram);
    
    uint64_t bgn = gethrtime();
    GLuint res = (*(GLuint(*)())orig)();
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(GLuint, glCreateShader) (GLenum type)
{
    GLPROLOG(glCreateShader);
    
    putCmd(OPC_glCreateShader);
    putInt(type);
    
    uint64_t bgn = gethrtime();
    GLuint res = (*(GLuint(*)())orig)(type);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(void, glCullFace) (GLenum mode)
{
    GLPROLOG(glCullFace);
    
    putCmd(OPC_glCullFace);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDeleteBuffers) (GLsizei n, const GLuint* buffers)
{
    GLPROLOG(glDeleteBuffers);
    
    putCmd(OPC_glDeleteBuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(buffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, buffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDeleteFramebuffers) (GLsizei n, const GLuint* framebuffers)
{
    GLPROLOG(glDeleteFramebuffers);
    
    putCmd(OPC_glDeleteFramebuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(framebuffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, framebuffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDeleteProgram) (GLuint program)
{
    GLPROLOG(glDeleteProgram);
    
    putCmd(OPC_glDeleteProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glDeleteRenderbuffers) (GLsizei n, const GLuint* renderbuffers)
{
    GLPROLOG(glDeleteRenderbuffers);
    
    putCmd(OPC_glDeleteRenderbuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(renderbuffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, renderbuffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glDeleteShader) (GLuint shader)
{
    GLPROLOG(glDeleteShader);
    
    putCmd(OPC_glDeleteShader);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glDeleteTextures) (GLsizei n, const GLuint* textures)
{
    GLPROLOG(glDeleteTextures);
    
    putCmd(OPC_glDeleteTextures);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(textures[i]);
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, textures);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDepthFunc) (GLenum func)
{
    GLPROLOG(glDepthFunc);
    
    putCmd(OPC_glDepthFunc);
    putInt(func);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(func);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDepthMask) (GLboolean flag)
{
    GLPROLOG(glDepthMask);
    
    putCmd(OPC_glDepthMask);
    putInt(flag);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(flag);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDepthRangef) (GLclampf zNear, GLclampf zFar)
{
    GLPROLOG(glDepthRangef);
    
    putCmd(OPC_glDepthRangef);
    putFloat(zNear);
    putFloat(zFar);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLclampf, GLclampf))orig)(zNear, zFar);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glDetachShader) (GLuint program, GLuint shader)
{
    GLPROLOG(glDetachShader);
    
    putCmd(OPC_glDetachShader);
    putInt(program);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glDisable) (GLenum cap)
{
    GLPROLOG(glDisable);
    
    putCmd(OPC_glDisable);
    putInt(cap);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(cap);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDisableVertexAttribArray) (GLuint index)
{
    GLPROLOG(glDisableVertexAttribArray);
    
    putCmd(OPC_glDisableVertexAttribArray);
    putInt(index);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(index);
    uint64_t end = gethrtime();
    
    vertexAttrib[index].enabled = 0;
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDrawArrays) (GLenum mode, GLint first, GLsizei count)
{
    GLPROLOG(glDrawArrays);
    
    putCmd(OPC_glDrawArrays);
    putInt(mode);
    putInt(first);
    putInt(count);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mode, first, count);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glDrawElements) (GLenum mode, GLsizei count, GLenum type, const GLvoid* indices)
{
    GLPROLOG(glDrawElements);
    
    putCmd(OPC_glDrawElements);
    putInt(mode);
    putInt(count);
    putInt(type);
    putBytes(indices, count * glSizeof(type));
    if (!arrayBufferBinding) {
        putVertexAttrib(count);
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mode, count, type, indices);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glEnable) (GLenum cap)
{
    GLPROLOG(glEnable);
    
    putCmd(OPC_glEnable);
    putInt(cap);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(cap);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glEnableVertexAttribArray) (GLuint index)
{
    GLPROLOG(glEnableVertexAttribArray);
    
    putCmd(OPC_glEnableVertexAttribArray);
    putInt(index);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(index);
    uint64_t end = gethrtime();
    
    vertexAttrib[index].enabled = 1;
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glFinish) (void)
{
    GLPROLOG(glFinish);
    
    putCmd(OPC_glFinish);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glFlush) (void)
{
    GLPROLOG(glFlush);
    
    putCmd(OPC_glFlush);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glFramebufferRenderbuffer) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
{
    GLPROLOG(glFramebufferRenderbuffer);
    
    putCmd(OPC_glFramebufferRenderbuffer);
    putInt(target);
    putInt(attachment);
    putInt(renderbuffertarget);
    putInt(renderbuffer);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, attachment, renderbuffertarget, renderbuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glFramebufferTexture2D) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    GLPROLOG(glFramebufferTexture2D);
    
    putCmd(OPC_glFramebufferTexture2D);
    putInt(target);
    putInt(attachment);
    putInt(textarget);
    putInt(texture);
    putInt(level);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, attachment, textarget, texture, level);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glFrontFace) (GLenum mode)
{
    GLPROLOG(glFrontFace);
    
    putCmd(OPC_glFrontFace);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGenBuffers) (GLsizei n, GLuint* buffers)
{
    GLPROLOG(glGenBuffers);
    
    putCmd(OPC_glGenBuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, buffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(buffers[i]);
    }
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGenerateMipmap) (GLenum target)
{
    GLPROLOG(glGenerateMipmap);
    
    putCmd(OPC_glGenerateMipmap);
    putInt(target);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glGenFramebuffers) (GLsizei n, GLuint* framebuffers)
{
    GLPROLOG(glGenFramebuffers);
    
    putCmd(OPC_glGenFramebuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, framebuffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(framebuffers[i]);
    }
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGenRenderbuffers) (GLsizei n, GLuint* renderbuffers)
{
    GLPROLOG(glGenRenderbuffers);
    
    putCmd(OPC_glGenRenderbuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, renderbuffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(renderbuffers[i]);
    }
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGenTextures) (GLsizei n, GLuint* textures)
{
    GLPROLOG(glGenTextures);
    
    putCmd(OPC_glGenTextures);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(n, textures);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(textures[i]);
    }
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGetActiveAttrib) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    GLPROLOG(glGetActiveAttrib);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetActiveUniform) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    GLPROLOG(glGetActiveUniform);

    putCmd(OPC_glGetActiveUniform);
    putInt(program);
    putInt(index);
    putInt(bufsize);
    putPtr(length);
    putPtr(size);
    putPtr(type);
    putPtr(name);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, index, bufsize, length, size, type, name);
    uint64_t end = gethrtime();
    
    if (length) putInt(*length);
    if (size)   putInt(*size);
    if (type)   putInt(*type);
    if (name)   putString(name);
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glGetAttachedShaders) (GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders)
{
    GLPROLOG(glGetAttachedShaders);
    
    putCmd(OPC_glGetAttachedShaders);
    putInt(program);
    putInt(maxcount);
    putPtr(count);
    putPtr(shaders);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, maxcount, count, shaders);
    uint64_t end = gethrtime();

    if (count) {
        putInt(*count);
        if (shaders) {
            int i;
            for (i=0; i<*count; ++i) putInt(shaders[i]);
        }
    }
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(int, glGetAttribLocation) (GLuint program, const GLchar* name)
{
    GLPROLOG(glGetAttribLocation);
    
    putCmd(OPC_glGetAttribLocation);
    putInt(program);
    putString(name);
    
    uint64_t bgn = gethrtime();
    int res = (*(int(*)())orig)(program, name);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);
    
    GLEPILOG();
    
    return res;
}

DEF(void, glGetBooleanv) (GLenum pname, GLboolean* params)
{
    GLPROLOG(glGetBooleanv);
    
    putCmd(OPC_glGetBooleanv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glGetBufferParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLPROLOG(glGetBufferParameteriv);
    NOT_IMPLEMENTED();
}    

DEF(GLenum, glGetError) (void)
{
    GLPROLOG(glGetError);
    
    putCmd(OPC_glGetError);
    
    uint64_t bgn = gethrtime();
    GLenum res = (*(GLenum(*)())orig)();
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(void, glGetFloatv) (GLenum pname, GLfloat* params)
{
    GLPROLOG(glGetFloatv);
    
    putCmd(OPC_glGetFloatv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLenum, GLfloat*))orig)(pname, params);
    uint64_t end = gethrtime();
    
    putFloatPtr(params);
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glGetFramebufferAttachmentParameteriv) (GLenum target, GLenum attachment, GLenum pname, GLint* params)
{
    GLPROLOG(glGetFramebufferAttachmentParameteriv);
    
    putCmd(OPC_glGetFramebufferAttachmentParameteriv);
    putInt(target);
    putInt(attachment);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, attachment, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glGetIntegerv) (GLenum pname, GLint* params)
{
    GLPROLOG(glGetIntegerv);
    
    putCmd(OPC_glGetIntegerv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(pname, params);
    uint64_t end = gethrtime();
    
    putIntPtr(params);
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGetProgramiv) (GLuint program, GLenum pname, GLint* params)
{
    GLPROLOG(glGetProgramiv);
    
    putCmd(OPC_glGetProgramiv);
    putInt(program);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGetProgramInfoLog) (GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    GLPROLOG(glGetProgramInfoLog);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetRenderbufferParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLPROLOG(glGetRenderbufferParameteriv);
    
    putCmd(OPC_glGetRenderbufferParameteriv);
    putInt(target);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLEPILOG();
}

 DEF(void, glGetShaderiv) (GLuint shader, GLenum pname, GLint* params)
{
    GLPROLOG(glGetShaderiv);
    
    putCmd(OPC_glGetShaderiv);
    putInt(shader);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(shader, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glGetShaderInfoLog) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    GLPROLOG(glGetShaderInfoLog);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetShaderPrecisionFormat) (GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision)
{
    GLPROLOG(glGetShaderPrecisionFormat);
    NOT_IMPLEMENTED();
}

DEF(void, glGetShaderSource) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source)
{
    GLPROLOG(glGetShaderSource);
    NOT_IMPLEMENTED();
}    

DEF(const GLubyte*, glGetString) (GLenum name)
{
    GLPROLOG(glGetString);
    
    putCmd(OPC_glGetString);
    putInt(name);
    
    uint64_t bgn = gethrtime();
    const GLubyte* res = (*(const GLubyte*(*)())orig)(name);
    uint64_t end = gethrtime();
    
    putString(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(void, glGetTexParameterfv) (GLenum target, GLenum pname, GLfloat* params)
{
    GLPROLOG(glGetTexParameterfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetTexParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLPROLOG(glGetTexParameteriv);
    NOT_IMPLEMENTED();
}

DEF(void, glGetUniformfv) (GLuint program, GLint location, GLfloat* params)
{
    GLPROLOG(glGetUniformfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetUniformiv) (GLuint program, GLint location, GLint* params)
{
    GLPROLOG(glGetUniformiv);
    NOT_IMPLEMENTED();
}    

DEF(int, glGetUniformLocation) (GLuint program, const GLchar* name)
{
    GLPROLOG(glGetUniformLocation);
    
    putCmd(OPC_glGetUniformLocation);
    putInt(program);
    putString(name);
    
    uint64_t bgn = gethrtime();
    int res = (*(int(*)())orig)(program, name);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLEPILOG();
    
    return res;
}    

DEF(void, glGetVertexAttribfv) (GLuint index, GLenum pname, GLfloat* params)
{
    GLPROLOG(glGetVertexAttribfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetVertexAttribiv) (GLuint index, GLenum pname, GLint* params)
{
    GLPROLOG(glGetVertexAttribiv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetVertexAttribPointerv) (GLuint index, GLenum pname, GLvoid** pointer)
{
    GLPROLOG(glGetVertexAttribPointerv);
    NOT_IMPLEMENTED();
}    

DEF(void, glHint) (GLenum target, GLenum mode)
{
    GLPROLOG(glHint);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsBuffer) (GLuint buffer)
{
    GLPROLOG(glIsBuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsEnabled) (GLenum cap)
{
    GLPROLOG(glIsEnabled);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsFramebuffer) (GLuint framebuffer)
{
    GLPROLOG(glIsFramebuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsProgram) (GLuint program)
{
    GLPROLOG(glIsProgram);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsRenderbuffer) (GLuint renderbuffer)
{
    GLPROLOG(glIsRenderbuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsShader) (GLuint shader)
{
    GLPROLOG(glIsShader);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsTexture) (GLuint texture)
{
    GLPROLOG(glIsTexture);
    NOT_IMPLEMENTED();
}    

DEF(void, glLineWidth) (GLfloat width)
{
    GLPROLOG(glLineWidth);
    NOT_IMPLEMENTED();
}    

DEF(void, glLinkProgram) (GLuint program)
{
    GLPROLOG(glLinkProgram);
    
    putCmd(OPC_glLinkProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glPixelStorei) (GLenum pname, GLint param)
{
    GLPROLOG(glPixelStorei);
    
    putCmd(OPC_glPixelStorei);
    putInt(pname);
    putInt(param);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(pname, param);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glPolygonOffset) (GLfloat factor, GLfloat units)
{
    GLPROLOG(glPolygonOffset);
    
    putCmd(OPC_glPolygonOffset);
    putFloat(factor);
    putFloat(units);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLfloat, GLfloat))orig)(factor, units);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glReadPixels) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    GLPROLOG(glReadPixels);
    NOT_IMPLEMENTED();
}    

DEF(void, glReleaseShaderCompiler) (void)
{
    GLPROLOG(glReleaseShaderCompiler);
    NOT_IMPLEMENTED();
}

DEF(void, glRenderbufferStorage) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height)
{
    GLPROLOG(glRenderbufferStorage);
    
    putCmd(OPC_glRenderbufferStorage);
    putInt(target);
    putInt(internalformat);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, internalformat, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glSampleCoverage) (GLclampf value, GLboolean invert)
{
    GLPROLOG(glSampleCoverage);
    NOT_IMPLEMENTED();
}    

DEF(void, glScissor) (GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLPROLOG(glScissor);
    
    putCmd(OPC_glScissor);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glShaderBinary) (GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length)
{
    GLPROLOG(glShaderBinary);
    NOT_IMPLEMENTED();
}

DEF(void, glShaderSource) (GLuint shader, GLsizei count, const GLchar** string, const GLint* length)
{
    GLPROLOG(glShaderSource);
    
    putCmd(OPC_glShaderSource);
    putInt(shader);
    putInt(count);
    int i;
    for (i=0; i<count; ++i) {
        int len = length == NULL ? 0 : length[i];
        putInt(len);
        if (len > 0) {
            putBytes(string[i], len);
        }
        else {
            putString(string[i]);
        }
    }
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(shader, count, string, length);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glStencilFunc) (GLenum func, GLint ref, GLuint mask)
{
    GLPROLOG(glStencilFunc);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilFuncSeparate) (GLenum face, GLenum func, GLint ref, GLuint mask)
{
    GLPROLOG(glStencilFuncSeparate);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilMask) (GLuint mask)
{
    GLPROLOG(glStencilMask);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilMaskSeparate) (GLenum face, GLuint mask)
{
    GLPROLOG(glStencilMaskSeparate);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilOp) (GLenum fail, GLenum zfail, GLenum zpass)
{
    GLPROLOG(glStencilOp);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilOpSeparate) (GLenum face, GLenum fail, GLenum zfail, GLenum zpass)
{
    GLPROLOG(glStencilOpSeparate);
    NOT_IMPLEMENTED();
}    

#ifdef RASPBERRYPI
DEF(void, glTexImage2D) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
#else
DEF(void, glTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
#endif
{
    GLPROLOG(glTexImage2D);
    
    putCmd(OPC_glTexImage2D);
    putInt(target);
    putInt(level);
    putInt(internalformat);
    putInt(width);
    putInt(height);
    putInt(border);
    putInt(format);
    putInt(type);
    putBytes(pixels, width * height * glElementSize(format, type));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, internalformat, width, height, border, format, type, pixels);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glTexParameterf) (GLenum target, GLenum pname, GLfloat param)
{
    GLPROLOG(glTexParameterf);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexParameterfv) (GLenum target, GLenum pname, const GLfloat* params)
{
    GLPROLOG(glTexParameterfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexParameteri) (GLenum target, GLenum pname, GLint param)
{
    GLPROLOG(glTexParameteri);
    
    putCmd(OPC_glTexParameteri);
    putInt(target);
    putInt(pname);
    putInt(param);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, pname, param);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glTexParameteriv) (GLenum target, GLenum pname, const GLint* params)
{
    GLPROLOG(glTexParameteriv);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels)
{
    GLPROLOG(glTexSubImage2D);
    
    putCmd(OPC_glTexSubImage2D);
    putInt(target);
    putInt(level);
    putInt(xoffset);
    putInt(yoffset);
    putInt(width);
    putInt(height);
    putInt(format);
    putInt(type);
    putBytes(pixels, width * height * glElementSize(format, type));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(target, level, xoffset, yoffset, width, height, format, type, pixels);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform1f) (GLint location, GLfloat x)
{
    GLPROLOG(glUniform1f);
    
    putCmd(OPC_glUniform1f);
    putInt(location);
    putFloat(x);
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLint, GLfloat))orig)(location, x);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform1fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLPROLOG(glUniform1fv);
    
    putCmd(OPC_glUniform1fv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    (*(void(*)(GLint, GLsizei, const GLfloat*))orig)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glUniform1i) (GLint location, GLint x)
{
    GLPROLOG(glUniform1i);

    putCmd(OPC_glUniform1i);
    putInt(location);
    putInt(x);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, x);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform1iv) (GLint location, GLsizei count, const GLint* v)
{
    GLPROLOG(glUniform1iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform2f) (GLint location, GLfloat x, GLfloat y)
{
    GLPROLOG(glUniform2f);

    putCmd(OPC_glUniform2f);
    putInt(location);
    putFloat(x);
    putFloat(y);

    uint64_t bgn = gethrtime();
    (*(void(*)(GLint, GLfloat, GLfloat))orig)(location, x, y);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform2fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLPROLOG(glUniform2fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform2i) (GLint location, GLint x, GLint y)
{
    GLPROLOG(glUniform2i);

    putCmd(OPC_glUniform2i);
    putInt(location);
    putInt(x);
    putInt(y);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, x, y);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform2iv) (GLint location, GLsizei count, const GLint* v)
{
    GLPROLOG(glUniform2iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform3f) (GLint location, GLfloat x, GLfloat y, GLfloat z)
{
    GLPROLOG(glUniform3f);

    putCmd(OPC_glUniform3f);
    putInt(location);
    putFloat(x);
    putFloat(y);
    putFloat(z);

    uint64_t bgn = gethrtime();
    (*(void(*)(GLint, GLfloat, GLfloat, GLfloat))orig)(location, x, y, z);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform3fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLPROLOG(glUniform3fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform3i) (GLint location, GLint x, GLint y, GLint z)
{
    GLPROLOG(glUniform3i);

    putCmd(OPC_glUniform3i);
    putInt(location);
    putInt(x);
    putInt(y);
    putInt(z);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, x, y, z);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform3iv) (GLint location, GLsizei count, const GLint* v)
{
    GLPROLOG(glUniform3iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform4f) (GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    GLPROLOG(glUniform4f);

    putCmd(OPC_glUniform4f);
    putInt(location);
    putFloat(x);
    putFloat(y);
    putFloat(z);
    putFloat(w);

    uint64_t bgn = gethrtime();
    (*(void(*)(GLint, GLfloat, GLfloat, GLfloat, GLfloat))orig)(location, x, y, z, w);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform4fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLPROLOG(glUniform4fv);

    putCmd(OPC_glUniform4fv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform4i) (GLint location, GLint x, GLint y, GLint z, GLint w)
{
    GLPROLOG(glUniform4i);

    putCmd(OPC_glUniform4i);
    putInt(location);
    putInt(x);
    putInt(y);
    putInt(z);
    putInt(w);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, x, y, z, w);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniform4iv) (GLint location, GLsizei count, const GLint* v)
{
    GLPROLOG(glUniform4iv);

    putCmd(OPC_glUniform4iv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLint));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUniformMatrix2fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLPROLOG(glUniformMatrix2fv);
    
    putCmd(OPC_glUniformMatrix2fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, 4 * count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glUniformMatrix3fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLPROLOG(glUniformMatrix3fv);
    
    putCmd(OPC_glUniformMatrix3fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, 9 * count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLEPILOG();
}

DEF(void, glUniformMatrix4fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLPROLOG(glUniformMatrix4fv);
    
    putCmd(OPC_glUniformMatrix4fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, count * sizeof(GLfloat) * 16);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glUseProgram) (GLuint program)
{
    GLPROLOG(glUseProgram);
    
    putCmd(OPC_glUseProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glValidateProgram) (GLuint program)
{
    GLPROLOG(glValidateProgram);
    
    putCmd(OPC_glValidateProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glVertexAttrib1f) (GLuint indx, GLfloat x)
{
    GLPROLOG(glVertexAttrib1f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib1fv) (GLuint indx, const GLfloat* values)
{
    GLPROLOG(glVertexAttrib1fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib2f) (GLuint indx, GLfloat x, GLfloat y)
{
    GLPROLOG(glVertexAttrib2f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib2fv) (GLuint indx, const GLfloat* values)
{
    GLPROLOG(glVertexAttrib2fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib3f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z)
{
    GLPROLOG(glVertexAttrib3f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib3fv) (GLuint indx, const GLfloat* values)
{
    GLPROLOG(glVertexAttrib3fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib4f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    GLPROLOG(glVertexAttrib4f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib4fv) (GLuint indx, const GLfloat* values)
{
    GLPROLOG(glVertexAttrib4fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttribPointer) (GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr)
{
    GLPROLOG(glVertexAttribPointer);
    
    putCmd(OPC_glVertexAttribPointer);
    putInt(indx);
    putInt(size);
    putInt(type);
    putInt(normalized);
    putInt(stride);
    putPtr(ptr);
    vertexAttrib[indx].size = size;
    vertexAttrib[indx].type = type;
    vertexAttrib[indx].normalized = normalized;
    vertexAttrib[indx].stride = stride;
    vertexAttrib[indx].pointer = ptr;
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(indx, size, type, normalized, stride, ptr);
    uint64_t end = gethrtime();

    putTime(bgn, end);

    GLEPILOG();
}    

DEF(void, glViewport) (GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLPROLOG(glViewport);
    
    putCmd(OPC_glViewport);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    
