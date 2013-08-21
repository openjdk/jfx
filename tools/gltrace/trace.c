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
#include <link.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "os.h"
#include "opengl.h"
#include "iolib.h"

#define trcLevel 0
#define dbgLevel 1

static void *libSelf = NULL;
static void *libGLESv2 = NULL;
static void *libEGL = NULL;
static FILE *out = NULL;
static int tLevel;

/*
 *    dlfcn
 */

extern struct dlfcn_hook {
    void *(*dlopen)(const char *, int, void *);
    int (*dlclose)(void *);
    void *(*dlsym)(void *, const char *, void *);
    void *(*dlvsym)(void *, const char *, const char *, void *);
    char *(*dlerror)(void);
    int (*dladdr)(const void *, Dl_info *);
    int (*dladdr1)(const void *, Dl_info *, void **, int);
    int (*dlinfo)(void *, int, void *, void *);
    void *(*dlmopen)(Lmid_t, const char *, int, void *);
    void *pad[4];
} *_dlfcn_hook;

static void *trace_dlopen(const char*, int, void*);
static int   trace_dlclose(void *);
static void *trace_dlsym(void *, const char *, void *);
static void *trace_dlvsym(void *, const char *, const char *, void *);
static char *trace_dlerror(void);
static int   trace_dladdr(const void *, Dl_info *);
static int   trace_dladdr1(const void *, Dl_info *, void **, int);
static int   trace_dlinfo(void *, int, void *, void *);
static void *trace_dlmopen(Lmid_t, const char *, int, void *);

static struct dlfcn_hook dlfcn_hook = {
    .dlopen   = trace_dlopen,
    .dlclose  = trace_dlclose,
    .dlsym    = trace_dlsym,
    .dlvsym   = trace_dlvsym,
    .dlerror  = trace_dlerror,
    .dladdr   = trace_dladdr,
    .dladdr1  = trace_dladdr1,
    .dlinfo   = trace_dlinfo,
    .dlmopen  = trace_dlmopen,
    .pad      = {0, 0, 0, 0},
};

static struct dlfcn_hook *dlfcn_hook_orig;

#define DLFCN_HOOK_INIT()        dlfcn_hook_orig = _dlfcn_hook;
#define DLFCN_HOOK_POP()        _dlfcn_hook = dlfcn_hook_orig
#define DLFCN_HOOK_PUSH()       _dlfcn_hook = &dlfcn_hook

void    *
trace_dlopen(const char *file, int mode, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlopen(file, mode);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dlclose(void *handle)
{
    DLFCN_HOOK_POP();
    int result = dlclose(handle);
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlsym(void *handle, const char *name, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlsym(libSelf, name);
    if (tLevel >= dbgLevel && result != NULL) {
        fprintf(out, "INTERCEPTION: %p %s = %p\n", handle, name, result);
    }
    if (result == NULL) {
        result = dlsym(handle, name);
    }
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlvsym(void *handle, const char *name, const char *version, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlvsym(libSelf, name, version);
    if (tLevel >= dbgLevel && result != NULL) {
        fprintf(out, "INTERCEPTION: %p %s.%s = %p\n", handle, name, version, result);
    }
    if (result == NULL) {
        result = dlvsym(handle, name, version);
    }
    DLFCN_HOOK_PUSH();
    return result;
}

char *
trace_dlerror(void)
{
    DLFCN_HOOK_POP();
    char *result = dlerror();
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dladdr(const void *address, Dl_info *info)
{
    DLFCN_HOOK_POP();
    int result = dladdr(address, info);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dladdr1(const void *address, Dl_info *info, void **extra, int flags)
{
    DLFCN_HOOK_POP();
    int result = dladdr1(address, info, extra, flags);
    DLFCN_HOOK_PUSH();
    return result;
}

int   
trace_dlinfo(void *handle, int request, void *arg, void *dl_caller)
{
    DLFCN_HOOK_POP();
    int result = dlinfo(handle, request, arg);
    DLFCN_HOOK_PUSH();
    return result;
}

void *
trace_dlmopen(Lmid_t nsid, const char *file, int mode, void *dl_caller)
{
    DLFCN_HOOK_POP();
    void *result = dlmopen(nsid, file, mode);
    DLFCN_HOOK_PUSH();
    return result;
}

/*
 *    Init/fini
 */

static void init() __attribute__ ((constructor));
static void 
init()
{
    out = stderr;
    tLevel = trcLevel;

    Dl_info info;
    if (dladdr(&init, &info)) {
        libSelf = dlopen(info.dli_fname, RTLD_LAZY|RTLD_NOLOAD);
    }

    libGLESv2 = dlopen("libGLESv2.so", RTLD_LAZY);
    libEGL    = dlopen("libEGL.so", RTLD_LAZY);
    if (tLevel >= dbgLevel) {
        fprintf(out, "INTERPOSITION STARTED self = %p (%s) libGLESv2 = %p libEGL = %p\n",
                libSelf, info.dli_fname, libGLESv2, libEGL);
    }

    iolib_init(IO_WRITE, NULL);
    
    DLFCN_HOOK_INIT();
    DLFCN_HOOK_PUSH();
}

static void fini() __attribute__ ((destructor));
static void fini()
{
    DLFCN_HOOK_POP();
    iolib_fini();
    
    if (tLevel >= dbgLevel) {
         fprintf(out, "INTERPOSITION FINISHED\n");
    }
}


/*
 *   OpenGL
 */
 
#define NOT_IMPLEMENTED() \
    { \
        fprintf(out, "FATAL: not implemented %s\n", __FUNCTION__); \
        exit(1); \
    }

#define PROLOG(lib,func) \
    static void *orig = NULL;\
    if (orig == NULL) {\
        DLFCN_HOOK_POP(); \
        orig = dlsym(lib, #func);\
        DLFCN_HOOK_PUSH(); \
        if (tLevel >= dbgLevel) {\
            fprintf(out, "INTERPOSITION dlsym("#func") = %p\n", orig);\
        }\
    }

#define EPILOG() 

#define GLPROLOG(func)  PROLOG(libGLESv2, func)
#define GLEPILOG()      EPILOG()

#define EGLPROLOG(func) PROLOG(libEGL, func)
#define EGLEPILOG()     EPILOG()


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
    fprintf(out, "FATAL: glSizeof: unknown type: 0x%x\n", type);
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

    fprintf(out, "FATAL: glCountof: unknown format: 0x%x\n", format);
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
        fprintf(out, "FATAL: glElementSize: unknown format: 0x%x\n", format);
        exit(1);
        return 0;
    case GL_UNSIGNED_SHORT_4_4_4_4:
    case GL_UNSIGNED_SHORT_5_5_5_1:
    case GL_UNSIGNED_SHORT_5_6_5:
        return 2; /* XXX validate */
    }
    fprintf(out, "FATAL: glElementSize: unknown type: 0x%x\n", type);
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

GL_APICALL void GL_APIENTRY
glActiveTexture (GLenum texture)
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

GL_APICALL void GL_APIENTRY
glAttachShader (GLuint program, GLuint shader)
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

GL_APICALL void GL_APIENTRY
glBindAttribLocation (GLuint program, GLuint index, const GLchar* name)
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

GL_APICALL void GL_APIENTRY 
glBindBuffer (GLenum target, GLuint buffer)
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

GL_APICALL void GL_APIENTRY 
glBindFramebuffer (GLenum target, GLuint framebuffer)
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

GL_APICALL void GL_APIENTRY 
glBindRenderbuffer (GLenum target, GLuint renderbuffer)
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

GL_APICALL void GL_APIENTRY 
glBindTexture (GLenum target, GLuint texture)
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

GL_APICALL void GL_APIENTRY 
glBlendColor (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glBlendEquation ( GLenum mode )
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glBlendEquationSeparate (GLenum modeRGB, GLenum modeAlpha)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glBlendFunc (GLenum sfactor, GLenum dfactor)
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

GL_APICALL void GL_APIENTRY 
glBlendFuncSeparate (GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glBufferData (GLenum target, GLsizeiptr size, const GLvoid* data, GLenum usage)
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

GL_APICALL void GL_APIENTRY 
glBufferSubData (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid* data)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLenum GL_APIENTRY 
glCheckFramebufferStatus (GLenum target)
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

GL_APICALL void GL_APIENTRY 
glClear (GLbitfield mask)
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

GL_APICALL void GL_APIENTRY 
glClearColor (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha)
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

GL_APICALL void GL_APIENTRY 
glClearStencil (GLint s)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glColorMask (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glCompileShader (GLuint shader)
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

GL_APICALL void GL_APIENTRY 
glCompressedTexImage2D (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid* data)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glCompressedTexSubImage2D (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid* data)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glCopyTexImage2D (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glCopyTexSubImage2D (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLuint GL_APIENTRY 
glCreateProgram (void)
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

GL_APICALL GLuint GL_APIENTRY 
glCreateShader (GLenum type)
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

GL_APICALL void GL_APIENTRY 
glCullFace (GLenum mode)
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

GL_APICALL void GL_APIENTRY 
glDeleteBuffers (GLsizei n, const GLuint* buffers)
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

GL_APICALL void GL_APIENTRY 
glDeleteFramebuffers (GLsizei n, const GLuint* framebuffers)
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

GL_APICALL void GL_APIENTRY 
glDeleteProgram (GLuint program)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glDeleteRenderbuffers (GLsizei n, const GLuint* renderbuffers)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glDeleteShader (GLuint shader)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glDeleteTextures (GLsizei n, const GLuint* textures)
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

GL_APICALL void GL_APIENTRY 
glDepthFunc (GLenum func)
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

GL_APICALL void GL_APIENTRY 
glDepthMask (GLboolean flag)
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

GL_APICALL void GL_APIENTRY 
glDepthRangef (GLclampf zNear, GLclampf zFar)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glDetachShader (GLuint program, GLuint shader)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glDisable (GLenum cap)
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

GL_APICALL void GL_APIENTRY 
glDisableVertexAttribArray (GLuint index)
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

GL_APICALL void GL_APIENTRY 
glDrawArrays (GLenum mode, GLint first, GLsizei count)
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

GL_APICALL void GL_APIENTRY 
glDrawElements (GLenum mode, GLsizei count, GLenum type, const GLvoid* indices)
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

GL_APICALL void GL_APIENTRY 
glEnable (GLenum cap)
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

GL_APICALL void GL_APIENTRY 
glEnableVertexAttribArray (GLuint index)
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

GL_APICALL void GL_APIENTRY 
glFinish (void)
{
    GLPROLOG(glFinish);
    
    putCmd(OPC_glFinish);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

GL_APICALL void GL_APIENTRY 
glFlush (void)
{
    GLPROLOG(glFlush);
    
    putCmd(OPC_glFlush);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)();
    uint64_t end = gethrtime();
    
    putTime(bgn, end);

    GLEPILOG();
}    

GL_APICALL void GL_APIENTRY 
glFramebufferRenderbuffer (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer)
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

GL_APICALL void GL_APIENTRY 
glFramebufferTexture2D (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level)
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

GL_APICALL void GL_APIENTRY 
glFrontFace (GLenum mode)
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

GL_APICALL void GL_APIENTRY 
glGenBuffers (GLsizei n, GLuint* buffers)
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

GL_APICALL void GL_APIENTRY 
glGenerateMipmap (GLenum target)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGenFramebuffers (GLsizei n, GLuint* framebuffers)
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

GL_APICALL void GL_APIENTRY 
glGenRenderbuffers (GLsizei n, GLuint* renderbuffers)
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

GL_APICALL void GL_APIENTRY 
glGenTextures (GLsizei n, GLuint* textures)
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

GL_APICALL void GL_APIENTRY 
glGetActiveAttrib (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetActiveUniform (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetAttachedShaders (GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL int  GL_APIENTRY 
glGetAttribLocation (GLuint program, const GLchar* name)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetBooleanv (GLenum pname, GLboolean* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetBufferParameteriv (GLenum target, GLenum pname, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLenum GL_APIENTRY 
glGetError (void)
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

GL_APICALL void GL_APIENTRY 
glGetFloatv (GLenum pname, GLfloat* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetFramebufferAttachmentParameteriv (GLenum target, GLenum attachment, GLenum pname, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetIntegerv (GLenum pname, GLint* params)
{
    GLPROLOG(glGetIntegerv);
    
    putCmd(OPC_glGetIntegerv);
    putInt(pname);
    
    uint64_t bgn = gethrtime();
    (*(void(*)())orig)(pname, params);
    uint64_t end = gethrtime();
    
    putInt(params == NULL ? 0 : *params);
    putTime(bgn, end);

    GLEPILOG();
}    

GL_APICALL void GL_APIENTRY 
glGetProgramiv (GLuint program, GLenum pname, GLint* params)
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

GL_APICALL void GL_APIENTRY 
glGetProgramInfoLog (GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetRenderbufferParameteriv (GLenum target, GLenum pname, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetShaderiv (GLuint shader, GLenum pname, GLint* params)
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

GL_APICALL void GL_APIENTRY 
glGetShaderInfoLog (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetShaderPrecisionFormat (GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetShaderSource (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL const GLubyte* GL_APIENTRY 
glGetString (GLenum name)
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

GL_APICALL void GL_APIENTRY 
glGetTexParameterfv (GLenum target, GLenum pname, GLfloat* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetTexParameteriv (GLenum target, GLenum pname, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetUniformfv (GLuint program, GLint location, GLfloat* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetUniformiv (GLuint program, GLint location, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL int  GL_APIENTRY 
glGetUniformLocation (GLuint program, const GLchar* name)
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

GL_APICALL void GL_APIENTRY 
glGetVertexAttribfv (GLuint index, GLenum pname, GLfloat* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetVertexAttribiv (GLuint index, GLenum pname, GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glGetVertexAttribPointerv (GLuint index, GLenum pname, GLvoid** pointer)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glHint (GLenum target, GLenum mode)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsBuffer (GLuint buffer)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsEnabled (GLenum cap)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsFramebuffer (GLuint framebuffer)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsProgram (GLuint program)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsRenderbuffer (GLuint renderbuffer)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsShader (GLuint shader)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL GLboolean GL_APIENTRY 
glIsTexture (GLuint texture)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glLineWidth (GLfloat width)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glLinkProgram (GLuint program)
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

GL_APICALL void GL_APIENTRY 
glPixelStorei (GLenum pname, GLint param)
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

GL_APICALL void GL_APIENTRY 
glPolygonOffset (GLfloat factor, GLfloat units)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glReadPixels (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glReleaseShaderCompiler (void)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glRenderbufferStorage (GLenum target, GLenum internalformat, GLsizei width, GLsizei height)
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

GL_APICALL void GL_APIENTRY 
glSampleCoverage (GLclampf value, GLboolean invert)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glScissor (GLint x, GLint y, GLsizei width, GLsizei height)
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

GL_APICALL void GL_APIENTRY 
glShaderBinary (GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glShaderSource (GLuint shader, GLsizei count, const GLchar** string, const GLint* length)
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

GL_APICALL void GL_APIENTRY 
glStencilFunc (GLenum func, GLint ref, GLuint mask)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glStencilFuncSeparate (GLenum face, GLenum func, GLint ref, GLuint mask)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glStencilMask (GLuint mask)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glStencilMaskSeparate (GLenum face, GLuint mask)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glStencilOp (GLenum fail, GLenum zfail, GLenum zpass)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glStencilOpSeparate (GLenum face, GLenum fail, GLenum zfail, GLenum zpass)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
#ifdef RASPBERRYPI
glTexImage2D (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
#else
glTexImage2D (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels)
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

GL_APICALL void GL_APIENTRY 
glTexParameterf (GLenum target, GLenum pname, GLfloat param)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glTexParameterfv (GLenum target, GLenum pname, const GLfloat* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glTexParameteri (GLenum target, GLenum pname, GLint param)
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

GL_APICALL void GL_APIENTRY 
glTexParameteriv (GLenum target, GLenum pname, const GLint* params)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glTexSubImage2D (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels)
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

GL_APICALL void GL_APIENTRY 
glUniform1f (GLint location, GLfloat x)
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

GL_APICALL void GL_APIENTRY 
glUniform1fv (GLint location, GLsizei count, const GLfloat* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform1i (GLint location, GLint x)
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

GL_APICALL void GL_APIENTRY 
glUniform1iv (GLint location, GLsizei count, const GLint* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform2f (GLint location, GLfloat x, GLfloat y)
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

GL_APICALL void GL_APIENTRY 
glUniform2fv (GLint location, GLsizei count, const GLfloat* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform2i (GLint location, GLint x, GLint y)
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

GL_APICALL void GL_APIENTRY 
glUniform2iv (GLint location, GLsizei count, const GLint* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform3f (GLint location, GLfloat x, GLfloat y, GLfloat z)
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

GL_APICALL void GL_APIENTRY 
glUniform3fv (GLint location, GLsizei count, const GLfloat* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform3i (GLint location, GLint x, GLint y, GLint z)
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

GL_APICALL void GL_APIENTRY 
glUniform3iv (GLint location, GLsizei count, const GLint* v)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniform4f (GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
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

GL_APICALL void GL_APIENTRY 
glUniform4fv (GLint location, GLsizei count, const GLfloat* v)
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

GL_APICALL void GL_APIENTRY 
glUniform4i (GLint location, GLint x, GLint y, GLint z, GLint w)
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

GL_APICALL void GL_APIENTRY 
glUniform4iv (GLint location, GLsizei count, const GLint* v)
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

GL_APICALL void GL_APIENTRY 
glUniformMatrix2fv (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniformMatrix3fv (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glUniformMatrix4fv (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value)
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

GL_APICALL void GL_APIENTRY 
glUseProgram (GLuint program)
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

GL_APICALL void GL_APIENTRY 
glValidateProgram (GLuint program)
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

GL_APICALL void GL_APIENTRY 
glVertexAttrib1f (GLuint indx, GLfloat x)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib1fv (GLuint indx, const GLfloat* values)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib2f (GLuint indx, GLfloat x, GLfloat y)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib2fv (GLuint indx, const GLfloat* values)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib3f (GLuint indx, GLfloat x, GLfloat y, GLfloat z)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib3fv (GLuint indx, const GLfloat* values)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib4f (GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttrib4fv (GLuint indx, const GLfloat* values)
{
    NOT_IMPLEMENTED();
}    

GL_APICALL void GL_APIENTRY 
glVertexAttribPointer (GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr)
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

GL_APICALL void GL_APIENTRY 
glViewport (GLint x, GLint y, GLsizei width, GLsizei height)
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


/*
 *    libEGL
 */

EGLAPI EGLint EGLAPIENTRY 
eglGetError(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLDisplay EGLAPIENTRY 
eglGetDisplay(EGLNativeDisplayType display_id)
{
    EGLPROLOG(eglGetDisplay);
    
    putCmd(OPC_eglGetDisplay);
    putPtr((void*)display_id);
    
    uint64_t bgn = gethrtime();
    EGLDisplay res = (*(EGLDisplay(*)())orig)(display_id);
    uint64_t end = gethrtime();
    
    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglInitialize(EGLDisplay dpy, EGLint *major, EGLint *minor)
{
    EGLPROLOG(eglInitialize);
    
    putCmd(OPC_eglInitialize);
    putPtr(dpy);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, major, minor);
    uint64_t end = gethrtime();
    
    putInt(major ? *major : 0);
    putInt(minor ? *minor : 0);
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglTerminate(EGLDisplay dpy)
{
    EGLPROLOG(eglTerminate);
    
    putCmd(OPC_eglTerminate);
    putPtr(dpy);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy);
    uint64_t end = gethrtime();

    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI const char * EGLAPIENTRY 
eglQueryString(EGLDisplay dpy, EGLint name)
{
    EGLPROLOG(eglQueryString);
    
    putCmd(OPC_eglQueryString);
    putPtr(dpy);
    putInt(name);
    
    uint64_t bgn = gethrtime();
    const char *res = (*(const char*(*)())orig)(dpy, name);
    uint64_t end = gethrtime();
    
    putString(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglGetConfigs(EGLDisplay dpy, EGLConfig *configs, EGLint config_size, EGLint *num_config)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglChooseConfig(EGLDisplay dpy, const EGLint *attrib_list,
                           EGLConfig *configs, EGLint config_size,
                           EGLint *num_config)
{
    EGLPROLOG(eglChooseConfig);
    
    putCmd(OPC_eglChooseConfig);
    putPtr(dpy);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    putInt(configs == NULL ? 0 : config_size);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, attrib_list, configs, config_size, num_config);
    uint64_t end = gethrtime();
    
    putInt(*num_config);
    int i;
    for (i=0; i<*num_config && i < config_size; ++i) {
        putPtr(configs[i]);
    }
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *value)
{
    EGLPROLOG(eglGetConfigAttrib);
    
    putCmd(OPC_eglGetConfigAttrib);
    putPtr(dpy);
    putPtr(config);
    putInt(attribute);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, config, attribute, value);
    uint64_t end = gethrtime();
    
    putInt(*value);
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreateWindowSurface(EGLDisplay dpy, EGLConfig config, EGLNativeWindowType win, const EGLint *attrib_list)
{
    EGLPROLOG(eglCreateWindowSurface);
    
    putCmd(OPC_eglCreateWindowSurface);
    putPtr(dpy);
    putPtr(config);
    putPtr(win);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    
    uint64_t bgn = gethrtime();
    EGLSurface res = (*(EGLSurface(*)())orig)(dpy, config, win, attrib_list);
    uint64_t end = gethrtime();

    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePbufferSurface(EGLDisplay dpy, EGLConfig config, const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePixmapSurface(EGLDisplay dpy, EGLConfig config,
                                  EGLNativePixmapType pixmap,
                                  const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglDestroySurface(EGLDisplay dpy, EGLSurface surface)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglQuerySurface(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint *value)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglBindAPI(EGLenum api)
{
    EGLPROLOG(eglBindAPI);
    
    putCmd(OPC_eglBindAPI);
    putInt(api);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(api);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLenum EGLAPIENTRY 
eglQueryAPI(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitClient(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglReleaseThread(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglCreatePbufferFromClientBuffer(
              EGLDisplay dpy, EGLenum buftype, EGLClientBuffer buffer,
              EGLConfig config, const EGLint *attrib_list)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSurfaceAttrib(EGLDisplay dpy, EGLSurface surface, EGLint attribute, EGLint value)
{
    EGLPROLOG(eglSurfaceAttrib);
    
    putCmd(OPC_eglSurfaceAttrib);
    putPtr(dpy);
    putPtr(surface);
    putInt(attribute);
    putInt(value);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, surface, attribute, value);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglBindTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglReleaseTexImage(EGLDisplay dpy, EGLSurface surface, EGLint buffer)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSwapInterval(EGLDisplay dpy, EGLint interval)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLContext EGLAPIENTRY 
eglCreateContext(EGLDisplay dpy, EGLConfig config,
                 EGLContext share_context, const EGLint *attrib_list)
{
    EGLPROLOG(eglCreateContext);
    
    putCmd(OPC_eglCreateContext);
    putPtr(dpy);
    putPtr(config);
    putPtr(share_context);
    const EGLint *ptr = attrib_list;
    for (;ptr && *ptr != EGL_NONE; ptr += 2) {
        putInt(ptr[0]);
        putInt(ptr[1]);
    }
    putInt(EGL_NONE);
    
    uint64_t bgn = gethrtime();
    EGLContext res = (*(EGLContext(*)())orig)(dpy, config, share_context, attrib_list);
    uint64_t end = gethrtime();
    
    putPtr(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglDestroyContext(EGLDisplay dpy, EGLContext ctx)
{
    EGLPROLOG(eglDestroyContext);
    
    putCmd(OPC_eglDestroyContext);
    putPtr(dpy);
    putPtr(ctx);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, ctx);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglMakeCurrent(EGLDisplay dpy, EGLSurface draw, EGLSurface read, EGLContext ctx)
{
    EGLPROLOG(eglMakeCurrent);
    
    putCmd(OPC_eglMakeCurrent);
    putPtr(dpy);
    putPtr(draw);
    putPtr(read);
    putPtr(ctx);

    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, draw, read, ctx);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLContext EGLAPIENTRY 
eglGetCurrentContext(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLSurface EGLAPIENTRY 
eglGetCurrentSurface(EGLint readdraw)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLDisplay EGLAPIENTRY 
eglGetCurrentDisplay(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglQueryContext(EGLDisplay dpy, EGLContext ctx, EGLint attribute, EGLint *value)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitGL(void)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglWaitNative(EGLint engine)
{
    NOT_IMPLEMENTED();
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglSwapBuffers(EGLDisplay dpy, EGLSurface surface)
{
    EGLPROLOG(eglSwapBuffers);
    
    putCmd(OPC_eglSwapBuffers);
    putPtr(dpy);
    putPtr(surface);
    
    uint64_t bgn = gethrtime();
    EGLBoolean res = (*(EGLBoolean(*)())orig)(dpy, surface);
    uint64_t end = gethrtime();
    
    putInt(res);
    putTime(bgn, end);

    EGLEPILOG();
    
    return res;
}    

EGLAPI EGLBoolean EGLAPIENTRY 
eglCopyBuffers(EGLDisplay dpy, EGLSurface surface, EGLNativePixmapType target)
{
    NOT_IMPLEMENTED();
}    

