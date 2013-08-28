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
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "os.h"
#include "iolib.h"
#include "gles.h"

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
                case GL_LUMINANCE:      return 1;
                case GL_LUMINANCE_ALPHA: return 2;
            }
            fprintf(stderr, "FATAL: glElementSize: unknown format: 0x%x\n", format);
            exit(1);
            return 0;
        case GL_UNSIGNED_SHORT_4_4_4_4:
        case GL_UNSIGNED_SHORT_5_5_5_1:
        case GL_UNSIGNED_SHORT_5_6_5:
            return 2;
#if MACOSX
        case GL_UNSIGNED_INT_8_8_8_8_REV:
            return 4;
#endif
    }
    fprintf(stderr, "FATAL: glElementSize: unknown type: 0x%x\n", type);
    exit(1);
    return 0;
}

static GLuint arrayBufferBinding = 0;
static GLuint elementArrayBufferBinding = 0;
static GLuint elementArrayBufferData = 0;
static GLvoid *elementArrayData = NULL;

static GLsizei
getVertexCount(GLsizei count, GLenum type, const GLvoid* indices)
{
#if MT_BUFFER_DATA_FIXED
    uint8_t *ptr = (uint8_t*)indices;
    if (elementArrayBufferBinding) {
        if (elementArrayBufferData != elementArrayBufferBinding) {
            fprintf(out, "FATAL: multiple element arrays not implemented\n");
            exit(1);
        }
        ptr = (uint8_t*)elementArrayData + (long)indices;
    }
    
    GLsizei i, maxval = 0;
    for (i=0; i<count; ++i) {
        int val;
        switch (type) {
            case GL_UNSIGNED_BYTE: val = ptr[i]; break;
            case GL_UNSIGNED_SHORT: val = ((uint16_t*)ptr)[i]; break;
            case GL_UNSIGNED_INT: val = ((uint32_t*)ptr)[i]; break;
            default:
                fprintf(out, "FATAL: glDrawElements: type %d not implemented\n", type);
                exit(1);
                break;
        }
        if (maxval < val) maxval = val;
    }
    return maxval + 1;  // count = max index + 1
#else
    return count/6*4; /* XXX hack for quads only */ 
#endif
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
putVertexAttrib(GLsizei count, GLenum type, const GLvoid* indices)
{
    count = getVertexCount(count, type, indices);
    int maxsz = 0;
    int i;
    for (i=0; i<MAX_VERTEX_ATTRIBS; ++i) {
        if (!vertexAttrib[i].enabled) continue;
        int sz = glSizeof(vertexAttrib[i].type) * vertexAttrib[i].size *count;
        if (maxsz < sz) maxsz = sz;
    }
    char *buf = alloca(maxsz);
    
    for (i=0; i<MAX_VERTEX_ATTRIBS; ++i) {
        if (!vertexAttrib[i].enabled) continue;
        char *src = (char*)vertexAttrib[i].pointer;
        char *dst = buf;
        int elemsz = glSizeof(vertexAttrib[i].type) * vertexAttrib[i].size;
        int j, k;
        for (j=0; j<count; ++j) {
            for (k=0; k<elemsz; ++k) *dst++ = *src++;
            if (vertexAttrib[i].stride > 0) {
                src += vertexAttrib[i].stride - elemsz;
            }
        }
        putBytes(buf, dst - buf);
    }
}


DEF(void, glActiveTexture)(GLenum texture)
{
    GLESPROLOG(glActiveTexture);
    
    putCmd(OPC_glActiveTexture);
    putInt(texture);
    
    uint64_t bgn = gethrtime();
    ORIG(glActiveTexture)(texture);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glAttachShader)(GLuint program, GLuint shader)
{
    GLESPROLOG(glAttachShader);
    
    putCmd(OPC_glAttachShader);
    putInt(program);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    ORIG(glAttachShader)(program, shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBindAttribLocation) (GLuint program, GLuint index, const GLchar* name)
{
    GLESPROLOG(glBindAttribLocation);
    
    putCmd(OPC_glBindAttribLocation);
    putInt(program);
    putInt(index);
    putString(name);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindAttribLocation)(program, index, name);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBindBuffer) (GLenum target, GLuint buffer)
{
    GLESPROLOG(glBindBuffer);
    
    putCmd(OPC_glBindBuffer);
    putInt(target);
    putInt(buffer);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindBuffer)(target, buffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    if (target == GL_ARRAY_BUFFER) {
        arrayBufferBinding = buffer;
    }
    else if (target == GL_ELEMENT_ARRAY_BUFFER) {
        elementArrayBufferBinding = buffer;
    }

    GLESEPILOG();
}    

DEF(void, glBindFramebuffer) (GLenum target, GLuint framebuffer)
{
    GLESPROLOG(glBindFramebuffer);
    
    putCmd(OPC_glBindFramebuffer);
    putInt(target);
    putInt(framebuffer);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindFramebuffer)(target, framebuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBindRenderbuffer) (GLenum target, GLuint renderbuffer)
{
    GLESPROLOG(glBindRenderbuffer);
    
    putCmd(OPC_glBindRenderbuffer);
    putInt(target);
    putInt(renderbuffer);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindRenderbuffer)(target, renderbuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBindTexture) (GLenum target, GLuint texture)
{
    GLESPROLOG(glBindTexture);
    
    putCmd(OPC_glBindTexture);
    putInt(target);
    putInt(texture);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindTexture)(target, texture);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBlendColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    GLESPROLOG(glBlendColor);

    putCmd(OPC_glBlendColor);
    putFloat(red);
    putFloat(green);
    putFloat(blue);
    putFloat(alpha);
    
    uint64_t bgn = gethrtime();
    ORIG(glBlendColor)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glBlendEquation) (GLenum mode)
{
    GLESPROLOG(glBlendEquation);
    
    putCmd(OPC_glBlendEquation);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    ORIG(glBlendEquation)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glBlendEquationSeparate) (GLenum modeRGB, GLenum modeAlpha)
{
    GLESPROLOG(glBlendEquationSeparate);

    putCmd(OPC_glBlendEquationSeparate);
    putInt(modeRGB);
    putInt(modeAlpha);
    
    uint64_t bgn = gethrtime();
    ORIG(glBlendEquationSeparate)(modeRGB, modeAlpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glBlendFunc) (GLenum sfactor, GLenum dfactor)
{
    GLESPROLOG(glBlendFunc);
    
    putCmd(OPC_glBlendFunc);
    putInt(sfactor);
    putInt(dfactor);
    
    uint64_t bgn = gethrtime();
    ORIG(glBlendFunc)(sfactor, dfactor);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBlendFuncSeparate) (GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
    GLESPROLOG(glBlendFuncSeparate);
    
    putCmd(OPC_glBlendFuncSeparate);
    putInt(srcRGB);
    putInt(dstRGB);
    putInt(srcAlpha);
    putInt(dstAlpha);
    
    uint64_t bgn = gethrtime();
    ORIG(glBlendFuncSeparate)(srcRGB, dstRGB, srcAlpha, dstAlpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glBufferData) (GLenum target, GLsizeiptr size, const GLvoid* data, GLenum usage)
{
    GLESPROLOG(glBufferData);
    
    putCmd(OPC_glBufferData);
    putInt(target);
    putInt(size);
    putBytes(data, size);
    putInt(usage);
    
    uint64_t bgn = gethrtime();
    ORIG(glBufferData)(target, size, data, usage);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glBufferSubData) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid* data)
{
    GLESPROLOG(glBufferSubData);

    putCmd(OPC_glBufferSubData);
    putInt(target);
    putInt(offset);
    putInt(size);
    putBytes(data, size);
    
    uint64_t bgn = gethrtime();
    ORIG(glBufferSubData)(target, offset, size, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(GLenum, glCheckFramebufferStatus) (GLenum target)
{
    GLESPROLOG(glCheckFramebufferStatus);
    
    putCmd(OPC_glCheckFramebufferStatus);
    putInt(target);
    
    uint64_t bgn = gethrtime();
    GLenum res = ORIG(glCheckFramebufferStatus)(target);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(void, glClear) (GLbitfield mask)
{
    GLESPROLOG(glClear);
    
    putCmd(OPC_glClear);
    putInt(mask);
    
    uint64_t bgn = gethrtime();
    ORIG(glClear)(mask);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glClearColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    GLESPROLOG(glClearColor);
    
    putCmd(OPC_glClearColor);
    putFloat(red);
    putFloat(green);
    putFloat(blue);
    putFloat(alpha);
    
    uint64_t bgn = gethrtime();
    ORIG(glClearColor)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

#if !MACOSX
DEF(void, glClearDepthf) (GLclampf depth)
{
    GLESPROLOG(glClearDepthf);
    
    putCmd(OPC_glClearDepthf);
    putFloat(depth);
    
    uint64_t bgn = gethrtime();
    ORIG(glClearDepthf)(depth);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}
#endif

DEF(void, glClearStencil) (GLint s)
{
    GLESPROLOG(glClearStencil);
    
    putCmd(OPC_glClearStencil);
    putInt(s);
    
    uint64_t bgn = gethrtime();
    ORIG(glClearStencil)(s);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glColorMask) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    GLESPROLOG(glColorMask);
    
    putCmd(OPC_glColorMask);
    putInt(red);
    putInt(green);
    putInt(blue);
    putInt(alpha);
    
    uint64_t bgn = gethrtime();
    ORIG(glColorMask)(red, green, blue, alpha);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glCompileShader) (GLuint shader)
{
    GLESPROLOG(glCompileShader);
    
    putCmd(OPC_glCompileShader);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    ORIG(glCompileShader)(shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glCompressedTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid* data)
{
    GLESPROLOG(glCompressedTexImage2D);

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
    ORIG(glCompressedTexImage2D)(target, level, internalformat, width, height, border, imageSize, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glCompressedTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid* data)
{
    GLESPROLOG(glCompressedTexSubImage2D);

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
    ORIG(glCompressedTexSubImage2D)(target, level, xoffset, yoffset, width, height, format, imageSize, data);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glCopyTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    GLESPROLOG(glCopyTexImage2D);
    
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
    ORIG(glCopyTexImage2D)(target, level, internalformat, x, y, width, height, border);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glCopyTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLESPROLOG(glCopyTexSubImage2D);
    
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
    ORIG(glCopyTexSubImage2D)(target, level, xoffset, yoffset, x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(GLuint, glCreateProgram) (void)
{
    GLESPROLOG(glCreateProgram);
    
    putCmd(OPC_glCreateProgram);
    
    uint64_t bgn = gethrtime();
    GLuint res = ORIG(glCreateProgram)();
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(GLuint, glCreateShader) (GLenum type)
{
    GLESPROLOG(glCreateShader);
    
    putCmd(OPC_glCreateShader);
    putInt(type);
    
    uint64_t bgn = gethrtime();
    GLuint res = ORIG(glCreateShader)(type);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(void, glCullFace) (GLenum mode)
{
    GLESPROLOG(glCullFace);
    
    putCmd(OPC_glCullFace);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    ORIG(glCullFace)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDeleteBuffers) (GLsizei n, const GLuint* buffers)
{
    GLESPROLOG(glDeleteBuffers);
    
    putCmd(OPC_glDeleteBuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(buffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteBuffers)(n, buffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDeleteFramebuffers) (GLsizei n, const GLuint* framebuffers)
{
    GLESPROLOG(glDeleteFramebuffers);
    
    putCmd(OPC_glDeleteFramebuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(framebuffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteFramebuffers)(n, framebuffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDeleteProgram) (GLuint program)
{
    GLESPROLOG(glDeleteProgram);
    
    putCmd(OPC_glDeleteProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteProgram)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glDeleteRenderbuffers) (GLsizei n, const GLuint* renderbuffers)
{
    GLESPROLOG(glDeleteRenderbuffers);
    
    putCmd(OPC_glDeleteRenderbuffers);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(renderbuffers[i]);
    }
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteRenderbuffers)(n, renderbuffers);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glDeleteShader) (GLuint shader)
{
    GLESPROLOG(glDeleteShader);
    
    putCmd(OPC_glDeleteShader);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteShader)(shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glDeleteTextures) (GLsizei n, const GLuint* textures)
{
    GLESPROLOG(glDeleteTextures);
    
    putCmd(OPC_glDeleteTextures);
    putInt(n);
    int i;
    for (i=0; i<n; ++i) {
        putInt(textures[i]);
    }
    
    uint64_t bgn = gethrtime();
    ORIG(glDeleteTextures)(n, textures);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDepthFunc) (GLenum func)
{
    GLESPROLOG(glDepthFunc);
    
    putCmd(OPC_glDepthFunc);
    putInt(func);
    
    uint64_t bgn = gethrtime();
    ORIG(glDepthFunc)(func);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDepthMask) (GLboolean flag)
{
    GLESPROLOG(glDepthMask);
    
    putCmd(OPC_glDepthMask);
    putInt(flag);
    
    uint64_t bgn = gethrtime();
    ORIG(glDepthMask)(flag);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

#if !MACOSX
DEF(void, glDepthRangef) (GLclampf zNear, GLclampf zFar)
{
    GLESPROLOG(glDepthRangef);
    
    putCmd(OPC_glDepthRangef);
    putFloat(zNear);
    putFloat(zFar);
    
    uint64_t bgn = gethrtime();
    ORIG(glDepthRangef)(zNear, zFar);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}
#endif

DEF(void, glDetachShader) (GLuint program, GLuint shader)
{
    GLESPROLOG(glDetachShader);
    
    putCmd(OPC_glDetachShader);
    putInt(program);
    putInt(shader);
    
    uint64_t bgn = gethrtime();
    ORIG(glDetachShader)(program, shader);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glDisable) (GLenum cap)
{
    GLESPROLOG(glDisable);
    
    putCmd(OPC_glDisable);
    putInt(cap);
    
    uint64_t bgn = gethrtime();
    ORIG(glDisable)(cap);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDisableVertexAttribArray) (GLuint index)
{
    GLESPROLOG(glDisableVertexAttribArray);
    
    putCmd(OPC_glDisableVertexAttribArray);
    putInt(index);
    
    uint64_t bgn = gethrtime();
    ORIG(glDisableVertexAttribArray)(index);
    uint64_t end = gethrtime();
    
    vertexAttrib[index].enabled = 0;
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDrawArrays) (GLenum mode, GLint first, GLsizei count)
{
    GLESPROLOG(glDrawArrays);
    
    putCmd(OPC_glDrawArrays);
    putInt(mode);
    putInt(first);
    putInt(count);
    
    uint64_t bgn = gethrtime();
    ORIG(glDrawArrays)(mode, first, count);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glDrawElements) (GLenum mode, GLsizei count, GLenum type, const GLvoid* indices)
{
    GLESPROLOG(glDrawElements);
    
    putCmd(OPC_glDrawElements);
    putInt(mode);
    putInt(count);
    putInt(type);
    if (elementArrayBufferBinding) {
        putPtr(indices);
    }
    else {
        putBytes(indices, count * glSizeof(type));
    }    
    if (!arrayBufferBinding) {
        putVertexAttrib(count, type, indices);
    }
    
    uint64_t bgn = gethrtime();
    ORIG(glDrawElements)(mode, count, type, indices);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glEnable) (GLenum cap)
{
    GLESPROLOG(glEnable);
    
    putCmd(OPC_glEnable);
    putInt(cap);
    
    uint64_t bgn = gethrtime();
    ORIG(glEnable)(cap);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glEnableVertexAttribArray) (GLuint index)
{
    GLESPROLOG(glEnableVertexAttribArray);
    
    putCmd(OPC_glEnableVertexAttribArray);
    putInt(index);
    
    uint64_t bgn = gethrtime();
    ORIG(glEnableVertexAttribArray)(index);
    uint64_t end = gethrtime();
    
    vertexAttrib[index].enabled = 1;
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glFinish) (void)
{
    GLESPROLOG(glFinish);
    
    putCmd(OPC_glFinish);
    
    uint64_t bgn = gethrtime();
    ORIG(glFinish)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glFlush) (void)
{
    GLESPROLOG(glFlush);
    
    putCmd(OPC_glFlush);
    
    uint64_t bgn = gethrtime();
    ORIG(glFlush)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glFramebufferRenderbuffer) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
{
    GLESPROLOG(glFramebufferRenderbuffer);
    
    putCmd(OPC_glFramebufferRenderbuffer);
    putInt(target);
    putInt(attachment);
    putInt(renderbuffertarget);
    putInt(renderbuffer);
    
    uint64_t bgn = gethrtime();
    ORIG(glFramebufferRenderbuffer)(target, attachment, renderbuffertarget, renderbuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glFramebufferTexture2D) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    GLESPROLOG(glFramebufferTexture2D);
    
    putCmd(OPC_glFramebufferTexture2D);
    putInt(target);
    putInt(attachment);
    putInt(textarget);
    putInt(texture);
    putInt(level);
    
    uint64_t bgn = gethrtime();
    ORIG(glFramebufferTexture2D)(target, attachment, textarget, texture, level);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glFrontFace) (GLenum mode)
{
    GLESPROLOG(glFrontFace);
    
    putCmd(OPC_glFrontFace);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    ORIG(glFrontFace)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGenBuffers) (GLsizei n, GLuint* buffers)
{
    GLESPROLOG(glGenBuffers);
    
    putCmd(OPC_glGenBuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenBuffers)(n, buffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(buffers[i]);
    }
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGenerateMipmap) (GLenum target)
{
    GLESPROLOG(glGenerateMipmap);
    
    putCmd(OPC_glGenerateMipmap);
    putInt(target);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenerateMipmap)(target);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGenFramebuffers) (GLsizei n, GLuint* framebuffers)
{
    GLESPROLOG(glGenFramebuffers);
    
    putCmd(OPC_glGenFramebuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenFramebuffers)(n, framebuffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(framebuffers[i]);
    }
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGenRenderbuffers) (GLsizei n, GLuint* renderbuffers)
{
    GLESPROLOG(glGenRenderbuffers);
    
    putCmd(OPC_glGenRenderbuffers);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenRenderbuffers)(n, renderbuffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(renderbuffers[i]);
    }
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGenTextures) (GLsizei n, GLuint* textures)
{
    GLESPROLOG(glGenTextures);
    
    putCmd(OPC_glGenTextures);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenTextures)(n, textures);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(textures[i]);
    }
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGetActiveAttrib) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    GLESPROLOG(glGetActiveAttrib);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetActiveUniform) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    GLESPROLOG(glGetActiveUniform);

    putCmd(OPC_glGetActiveUniform);
    putInt(program);
    putInt(index);
    putInt(bufsize);
    putPtr(length);
    putPtr(size);
    putPtr(type);
    putPtr(name);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetActiveUniform)(program, index, bufsize, length, size, type, name);
    uint64_t end = gethrtime();
    
    if (length) putInt(*length);
    if (size)   putInt(*size);
    if (type)   putInt(*type);
    if (name)   putString(name);
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGetAttachedShaders) (GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders)
{
    GLESPROLOG(glGetAttachedShaders);
    
    putCmd(OPC_glGetAttachedShaders);
    putInt(program);
    putInt(maxcount);
    putPtr(count);
    putPtr(shaders);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetAttachedShaders)(program, maxcount, count, shaders);
    uint64_t end = gethrtime();

    if (count) {
        putInt(*count);
        if (shaders) {
            int i;
            for (i=0; i<*count; ++i) putInt(shaders[i]);
        }
    }
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(int, glGetAttribLocation) (GLuint program, const GLchar* name)
{
    GLESPROLOG(glGetAttribLocation);
    
    putCmd(OPC_glGetAttribLocation);
    putInt(program);
    putString(name);
    
    uint64_t bgn = gethrtime();
    int res = ORIG(glGetAttribLocation)(program, name);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);
    
    GLESEPILOG();
    
    return res;
}

DEF(void, glGetBooleanv) (GLenum pname, GLboolean* params)
{
    GLESPROLOG(glGetBooleanv);
    
    putCmd(OPC_glGetBooleanv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetBooleanv)(pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGetBufferParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetBufferParameteriv);
    NOT_IMPLEMENTED();
}    

DEF(GLenum, glGetError) (void)
{
    GLESPROLOG(glGetError);
    
    putCmd(OPC_glGetError);
    
    uint64_t bgn = gethrtime();
    GLenum res = ORIG(glGetError)();
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(void, glGetFloatv) (GLenum pname, GLfloat* params)
{
    GLESPROLOG(glGetFloatv);
    
    putCmd(OPC_glGetFloatv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetFloatv)(pname, params);
    uint64_t end = gethrtime();
    
    putFloatPtr(params);
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGetFramebufferAttachmentParameteriv) (GLenum target, GLenum attachment, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetFramebufferAttachmentParameteriv);
    
    putCmd(OPC_glGetFramebufferAttachmentParameteriv);
    putInt(target);
    putInt(attachment);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetFramebufferAttachmentParameteriv)(target, attachment, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGetIntegerv) (GLenum pname, GLint* params)
{
    GLESPROLOG(glGetIntegerv);
    
    putCmd(OPC_glGetIntegerv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetIntegerv)(pname, params);
    uint64_t end = gethrtime();
    
    putIntPtr(params);
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGetProgramiv) (GLuint program, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetProgramiv);
    
    putCmd(OPC_glGetProgramiv);
    putInt(program);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetProgramiv)(program, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGetProgramInfoLog) (GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    GLESPROLOG(glGetProgramInfoLog);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetRenderbufferParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetRenderbufferParameteriv);
    
    putCmd(OPC_glGetRenderbufferParameteriv);
    putInt(target);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetRenderbufferParameteriv)(target, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glGetShaderiv) (GLuint shader, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetShaderiv);
    
    putCmd(OPC_glGetShaderiv);
    putInt(shader);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    ORIG(glGetShaderiv)(shader, pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glGetShaderInfoLog) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    GLESPROLOG(glGetShaderInfoLog);
    NOT_IMPLEMENTED();
}    

#if !MACOSX
DEF(void, glGetShaderPrecisionFormat) (GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision)
{
    GLESPROLOG(glGetShaderPrecisionFormat);
    NOT_IMPLEMENTED();
}
#endif

DEF(void, glGetShaderSource) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source)
{
    GLESPROLOG(glGetShaderSource);
    NOT_IMPLEMENTED();
}    

DEF(const GLubyte*, glGetString) (GLenum name)
{
    GLESPROLOG(glGetString);
    
    putCmd(OPC_glGetString);
    putInt(name);
    
    uint64_t bgn = gethrtime();
    const GLubyte* res = ORIG(glGetString)(name);
    uint64_t end = gethrtime();
    
    putString(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(void, glGetTexParameterfv) (GLenum target, GLenum pname, GLfloat* params)
{
    GLESPROLOG(glGetTexParameterfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetTexParameteriv) (GLenum target, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetTexParameteriv);
    NOT_IMPLEMENTED();
}

DEF(void, glGetUniformfv) (GLuint program, GLint location, GLfloat* params)
{
    GLESPROLOG(glGetUniformfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetUniformiv) (GLuint program, GLint location, GLint* params)
{
    GLESPROLOG(glGetUniformiv);
    NOT_IMPLEMENTED();
}    

DEF(int, glGetUniformLocation) (GLuint program, const GLchar* name)
{
    GLESPROLOG(glGetUniformLocation);
    
    putCmd(OPC_glGetUniformLocation);
    putInt(program);
    putString(name);
    
    uint64_t bgn = gethrtime();
    int res = ORIG(glGetUniformLocation)(program, name);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    GLESEPILOG();
    
    return res;
}    

DEF(void, glGetVertexAttribfv) (GLuint index, GLenum pname, GLfloat* params)
{
    GLESPROLOG(glGetVertexAttribfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetVertexAttribiv) (GLuint index, GLenum pname, GLint* params)
{
    GLESPROLOG(glGetVertexAttribiv);
    NOT_IMPLEMENTED();
}    

DEF(void, glGetVertexAttribPointerv) (GLuint index, GLenum pname, GLvoid** pointer)
{
    GLESPROLOG(glGetVertexAttribPointerv);
    NOT_IMPLEMENTED();
}    

DEF(void, glHint) (GLenum target, GLenum mode)
{
    GLESPROLOG(glHint);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsBuffer) (GLuint buffer)
{
    GLESPROLOG(glIsBuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsEnabled) (GLenum cap)
{
    GLESPROLOG(glIsEnabled);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsFramebuffer) (GLuint framebuffer)
{
    GLESPROLOG(glIsFramebuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsProgram) (GLuint program)
{
    GLESPROLOG(glIsProgram);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsRenderbuffer) (GLuint renderbuffer)
{
    GLESPROLOG(glIsRenderbuffer);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsShader) (GLuint shader)
{
    GLESPROLOG(glIsShader);
    NOT_IMPLEMENTED();
}    

DEF(GLboolean, glIsTexture) (GLuint texture)
{
    GLESPROLOG(glIsTexture);
    NOT_IMPLEMENTED();
}    

DEF(void, glLineWidth) (GLfloat width)
{
    GLESPROLOG(glLineWidth);
    NOT_IMPLEMENTED();
}    

DEF(void, glLinkProgram) (GLuint program)
{
    GLESPROLOG(glLinkProgram);
    
    putCmd(OPC_glLinkProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    ORIG(glLinkProgram)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glPixelStorei) (GLenum pname, GLint param)
{
    GLESPROLOG(glPixelStorei);
    
    putCmd(OPC_glPixelStorei);
    putInt(pname);
    putInt(param);
    
    uint64_t bgn = gethrtime();
    ORIG(glPixelStorei)(pname, param);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glPolygonOffset) (GLfloat factor, GLfloat units)
{
    GLESPROLOG(glPolygonOffset);
    
    putCmd(OPC_glPolygonOffset);
    putFloat(factor);
    putFloat(units);
    
    uint64_t bgn = gethrtime();
    ORIG(glPolygonOffset)(factor, units);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glReadPixels) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    GLESPROLOG(glReadPixels);
    NOT_IMPLEMENTED();
}    

#if !MACOSX
DEF(void, glReleaseShaderCompiler) (void)
{
    GLESPROLOG(glReleaseShaderCompiler);
    NOT_IMPLEMENTED();
}
#endif

DEF(void, glRenderbufferStorage) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height)
{
    GLESPROLOG(glRenderbufferStorage);
    
    putCmd(OPC_glRenderbufferStorage);
    putInt(target);
    putInt(internalformat);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    ORIG(glRenderbufferStorage)(target, internalformat, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glSampleCoverage) (GLclampf value, GLboolean invert)
{
    GLESPROLOG(glSampleCoverage);
    NOT_IMPLEMENTED();
}    

DEF(void, glScissor) (GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLESPROLOG(glScissor);
    
    putCmd(OPC_glScissor);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    ORIG(glScissor)(x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glShaderBinary) (GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length)
{
    GLESPROLOG(glShaderBinary);
    NOT_IMPLEMENTED();
}

DEF(void, glShaderSource) (GLuint shader, GLsizei count, const GLchar** string, const GLint* length)
{
    GLESPROLOG(glShaderSource);
    
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
    ORIG(glShaderSource)(shader, count, string, length);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glStencilFunc) (GLenum func, GLint ref, GLuint mask)
{
    GLESPROLOG(glStencilFunc);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilFuncSeparate) (GLenum face, GLenum func, GLint ref, GLuint mask)
{
    GLESPROLOG(glStencilFuncSeparate);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilMask) (GLuint mask)
{
    GLESPROLOG(glStencilMask);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilMaskSeparate) (GLenum face, GLuint mask)
{
    GLESPROLOG(glStencilMaskSeparate);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilOp) (GLenum fail, GLenum zfail, GLenum zpass)
{
    GLESPROLOG(glStencilOp);
    NOT_IMPLEMENTED();
}    

DEF(void, glStencilOpSeparate) (GLenum face, GLenum fail, GLenum zfail, GLenum zpass)
{
    GLESPROLOG(glStencilOpSeparate);
    NOT_IMPLEMENTED();
}    

#ifdef RASPBERRYPI
DEF(void, glTexImage2D) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
#else
DEF(void, glTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
#endif
{
    GLESPROLOG(glTexImage2D);
    
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
    ORIG(glTexImage2D)(target, level, internalformat, width, height, border, format, type, pixels);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glTexParameterf) (GLenum target, GLenum pname, GLfloat param)
{
    GLESPROLOG(glTexParameterf);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexParameterfv) (GLenum target, GLenum pname, const GLfloat* params)
{
    GLESPROLOG(glTexParameterfv);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexParameteri) (GLenum target, GLenum pname, GLint param)
{
    GLESPROLOG(glTexParameteri);
    
    putCmd(OPC_glTexParameteri);
    putInt(target);
    putInt(pname);
    putInt(param);
    
    uint64_t bgn = gethrtime();
    ORIG(glTexParameteri)(target, pname, param);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glTexParameteriv) (GLenum target, GLenum pname, const GLint* params)
{
    GLESPROLOG(glTexParameteriv);
    NOT_IMPLEMENTED();
}    

DEF(void, glTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels)
{
    GLESPROLOG(glTexSubImage2D);
    
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
    ORIG(glTexSubImage2D)(target, level, xoffset, yoffset, width, height, format, type, pixels);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform1f) (GLint location, GLfloat x)
{
    GLESPROLOG(glUniform1f);
    
    putCmd(OPC_glUniform1f);
    putInt(location);
    putFloat(x);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform1f)(location, x);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform1fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLESPROLOG(glUniform1fv);
    
    putCmd(OPC_glUniform1fv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform1fv)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glUniform1i) (GLint location, GLint x)
{
    GLESPROLOG(glUniform1i);

    putCmd(OPC_glUniform1i);
    putInt(location);
    putInt(x);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform1i)(location, x);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform1iv) (GLint location, GLsizei count, const GLint* v)
{
    GLESPROLOG(glUniform1iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform2f) (GLint location, GLfloat x, GLfloat y)
{
    GLESPROLOG(glUniform2f);

    putCmd(OPC_glUniform2f);
    putInt(location);
    putFloat(x);
    putFloat(y);

    uint64_t bgn = gethrtime();
    ORIG(glUniform2f)(location, x, y);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform2fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLESPROLOG(glUniform2fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform2i) (GLint location, GLint x, GLint y)
{
    GLESPROLOG(glUniform2i);

    putCmd(OPC_glUniform2i);
    putInt(location);
    putInt(x);
    putInt(y);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform2i)(location, x, y);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform2iv) (GLint location, GLsizei count, const GLint* v)
{
    GLESPROLOG(glUniform2iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform3f) (GLint location, GLfloat x, GLfloat y, GLfloat z)
{
    GLESPROLOG(glUniform3f);

    putCmd(OPC_glUniform3f);
    putInt(location);
    putFloat(x);
    putFloat(y);
    putFloat(z);

    uint64_t bgn = gethrtime();
    ORIG(glUniform3f)(location, x, y, z);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform3fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLESPROLOG(glUniform3fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform3i) (GLint location, GLint x, GLint y, GLint z)
{
    GLESPROLOG(glUniform3i);

    putCmd(OPC_glUniform3i);
    putInt(location);
    putInt(x);
    putInt(y);
    putInt(z);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform3i)(location, x, y, z);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform3iv) (GLint location, GLsizei count, const GLint* v)
{
    GLESPROLOG(glUniform3iv);
    NOT_IMPLEMENTED();
}    

DEF(void, glUniform4f) (GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    GLESPROLOG(glUniform4f);

    putCmd(OPC_glUniform4f);
    putInt(location);
    putFloat(x);
    putFloat(y);
    putFloat(z);
    putFloat(w);

    uint64_t bgn = gethrtime();
    ORIG(glUniform4f)(location, x, y, z, w);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform4fv) (GLint location, GLsizei count, const GLfloat* v)
{
    GLESPROLOG(glUniform4fv);

    putCmd(OPC_glUniform4fv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform4fv)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform4i) (GLint location, GLint x, GLint y, GLint z, GLint w)
{
    GLESPROLOG(glUniform4i);

    putCmd(OPC_glUniform4i);
    putInt(location);
    putInt(x);
    putInt(y);
    putInt(z);
    putInt(w);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform4i)(location, x, y, z, w);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniform4iv) (GLint location, GLsizei count, const GLint* v)
{
    GLESPROLOG(glUniform4iv);

    putCmd(OPC_glUniform4iv);
    putInt(location);
    putInt(count);
    putBytes(v, count * sizeof(GLint));
    
    uint64_t bgn = gethrtime();
    ORIG(glUniform4iv)(location, count, v);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUniformMatrix2fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLESPROLOG(glUniformMatrix2fv);
    
    putCmd(OPC_glUniformMatrix2fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, 4 * count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    ORIG(glUniformMatrix2fv)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glUniformMatrix3fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLESPROLOG(glUniformMatrix3fv);
    
    putCmd(OPC_glUniformMatrix3fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, 9 * count * sizeof(GLfloat));
    
    uint64_t bgn = gethrtime();
    ORIG(glUniformMatrix3fv)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glUniformMatrix4fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    GLESPROLOG(glUniformMatrix4fv);
    
    putCmd(OPC_glUniformMatrix4fv);
    putInt(location);
    putInt(count);
    putInt(transpose);
    putBytes(value, count * sizeof(GLfloat) * 16);
    
    uint64_t bgn = gethrtime();
    ORIG(glUniformMatrix4fv)(location, count, transpose, value);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glUseProgram) (GLuint program)
{
    GLESPROLOG(glUseProgram);
    
    putCmd(OPC_glUseProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    ORIG(glUseProgram)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glValidateProgram) (GLuint program)
{
    GLESPROLOG(glValidateProgram);
    
    putCmd(OPC_glValidateProgram);
    putInt(program);
    
    uint64_t bgn = gethrtime();
    ORIG(glValidateProgram)(program);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glVertexAttrib1f) (GLuint indx, GLfloat x)
{
    GLESPROLOG(glVertexAttrib1f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib1fv) (GLuint indx, const GLfloat* values)
{
    GLESPROLOG(glVertexAttrib1fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib2f) (GLuint indx, GLfloat x, GLfloat y)
{
    GLESPROLOG(glVertexAttrib2f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib2fv) (GLuint indx, const GLfloat* values)
{
    GLESPROLOG(glVertexAttrib2fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib3f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z)
{
    GLESPROLOG(glVertexAttrib3f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib3fv) (GLuint indx, const GLfloat* values)
{
    GLESPROLOG(glVertexAttrib3fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib4f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    GLESPROLOG(glVertexAttrib4f);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttrib4fv) (GLuint indx, const GLfloat* values)
{
    GLESPROLOG(glVertexAttrib4fv);
    NOT_IMPLEMENTED();
}    

DEF(void, glVertexAttribPointer) (GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr)
{
    GLESPROLOG(glVertexAttribPointer);
    
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
    ORIG(glVertexAttribPointer)(indx, size, type, normalized, stride, ptr);
    uint64_t end = gethrtime();

    putTime(bgn, end);

    GLESEPILOG();
}    

DEF(void, glViewport) (GLint x, GLint y, GLsizei width, GLsizei height)
{
    GLESPROLOG(glViewport);
    
    putCmd(OPC_glViewport);
    putInt(x);
    putInt(y);
    putInt(width);
    putInt(height);
    
    uint64_t bgn = gethrtime();
    ORIG(glViewport)(x, y, width, height);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLESEPILOG();
}

#if MACOSX
/*
 * Mac OS X extensions
 */

DEF(void, glBegin) (GLenum mode)
{
    GLESPROLOG(glBegin);
    
    putCmd(OPC_glBegin);
    putInt(mode);
    
    uint64_t bgn = gethrtime();
    ORIG(glBegin)(mode);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glEnd) ()
{
    GLESPROLOG(glEnd);
    putCmd(OPC_glEnd);
    
    uint64_t bgn = gethrtime();
    ORIG(glEnd)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(GLboolean, glIsRenderbufferEXT) (GLuint renderbuffer)
{
    GLESPROLOG(glIsRenderbufferEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glBindRenderbufferEXT) (GLenum target, GLuint renderbuffer)
{
    GLESPROLOG(glIsRenderbufferEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glDeleteRenderbuffersEXT) (GLsizei n, const GLuint *renderbuffers)
{
    GLESPROLOG(glDeleteRenderbuffersEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGenRenderbuffersEXT) (GLsizei n, GLuint *renderbuffers)
{
    GLESPROLOG(glGenRenderbuffersEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glRenderbufferStorageEXT) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height)
{
    GLESPROLOG(glRenderbufferStorageEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGetRenderbufferParameterivEXT) (GLenum target, GLenum pname, GLint *params)
{
    GLESPROLOG(glGetRenderbufferParameterivEXT);
    NOT_IMPLEMENTED();
}

DEF(GLboolean, glIsFramebufferEXT) (GLuint framebuffer)
{
    GLESPROLOG(glIsFramebufferEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glBindFramebufferEXT) (GLenum target, GLuint framebuffer)
{
    GLESPROLOG(glBindFramebufferEXT);
    
    putCmd(OPC_glBindFramebufferEXT);
    putInt(target);
    putInt(framebuffer);
    
    uint64_t bgn = gethrtime();
    ORIG(glBindFramebufferEXT)(target, framebuffer);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glDeleteFramebuffersEXT) (GLsizei n, const GLuint *framebuffers)
{
    GLESPROLOG(glDeleteFramebuffersEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGenFramebuffersEXT) (GLsizei n, GLuint *framebuffers)
{
    GLESPROLOG(glGenFramebuffersEXT);
    
    putCmd(OPC_glGenFramebuffersEXT);
    putInt(n);
    
    uint64_t bgn = gethrtime();
    ORIG(glGenFramebuffersEXT)(n, framebuffers);
    uint64_t end = gethrtime();
    
    int i;
    for (i=0; i<n; ++i) {
        putInt(framebuffers[i]);
    }
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(GLenum, glCheckFramebufferStatusEXT) (GLenum target)
{
    GLESPROLOG(glCheckFramebufferStatusEXT);
    
    putCmd(OPC_glCheckFramebufferStatusEXT);
    putInt(target);
    
    uint64_t bgn = gethrtime();
    GLenum res = ORIG(glCheckFramebufferStatusEXT)(target);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);
    
    GLESEPILOG();
    
    return res;
}

DEF(void, glFramebufferTexture1DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    GLESPROLOG(glFramebufferTexture1DEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glFramebufferTexture2DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
{
    GLESPROLOG(glFramebufferTexture2DEXT);
    
    putCmd(OPC_glFramebufferTexture2DEXT);
    putInt(target);
    putInt(attachment);
    putInt(textarget);
    putInt(texture);
    putInt(level);
    
    uint64_t bgn = gethrtime();
    ORIG(glFramebufferTexture2DEXT)(target, attachment, textarget, texture, level);
    uint64_t end = gethrtime();
    
    putTime(bgn, end);
    
    GLESEPILOG();
}

DEF(void, glFramebufferTexture3DEXT) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset)
{
    GLESPROLOG(glFramebufferTexture3DEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glFramebufferRenderbufferEXT) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
{
    GLESPROLOG(glFramebufferRenderbufferEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGetFramebufferAttachmentParameterivEXT) (GLenum target, GLenum attachment, GLenum pname, GLint *params)
{
    GLESPROLOG(glGetFramebufferAttachmentParameterivEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGenerateMipmapEXT) (GLenum target)
{
    GLESPROLOG(glGenerateMipmapEXT);
    NOT_IMPLEMENTED();
}

DEF(void, glGenFencesAPPLE) (GLsizei n, GLint *fences)
{
    GLESPROLOG(glGenFencesAPPLE);
    NOT_IMPLEMENTED();
}

DEF(void, glDeleteFencesAPPLE) (GLsizei n, const GLint *fences)
{
    GLESPROLOG(glDeleteFencesAPPLE);
    NOT_IMPLEMENTED();
}

DEF(void, glSetFenceAPPLE) (GLint fence)
{
    GLESPROLOG(glSetFenceAPPLE);
    NOT_IMPLEMENTED();
}

DEF(GLboolean, glIsFenceAPPLE) (GLint fence)
{
    GLESPROLOG(glIsFenceAPPLE);
    NOT_IMPLEMENTED();
}

DEF(GLboolean, glTestFenceAPPLE) (GLint fence)
{
    GLESPROLOG(glTestFenceAPPLE);
    NOT_IMPLEMENTED();
}

DEF(void, glFinishFenceAPPLE) (GLint fence)
{
    GLESPROLOG(glFinishFenceAPPLE);
    NOT_IMPLEMENTED();
}

DEF(GLboolean, glTestObjectAPPLE) (GLenum object, GLint name)
{
    GLESPROLOG(glTestObjectAPPLE);
    NOT_IMPLEMENTED();
}

DEF(void, glFinishObjectAPPLE) (GLenum object, GLint name)
{
    GLESPROLOG(glFinishObjectAPPLE);
    NOT_IMPLEMENTED();
}

#endif /* MACOSX */

