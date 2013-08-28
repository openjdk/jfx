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
 
#ifndef GLTRACE_GLES_H
#define GLTRACE_GLES_H


#if linux
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include "trace.h"

#define DEF(type, func) type func
#define ORIG(func) orig_##func

#define NOT_IMPLEMENTED() \
    { \
        fprintf(stderr, "FATAL: not implemented %s\n", __FUNCTION__); \
        exit(1); \
    }

#define GLESPROLOG(func) \
    static proto_##func orig_##func = NULL; \
    if (orig_##func == NULL) { \
        DLFCN_HOOK_POP(); \
        orig_##func = dlsym(libGLESv2, #func); \
        DLFCN_HOOK_PUSH(); \
    }

#define GLESEPILOG() 

typedef void(*proto_glActiveTexture)(GLenum texture);
typedef void(*proto_glAttachShader) (GLuint program, GLuint shader);
typedef void(*proto_glBindAttribLocation) (GLuint program, GLuint index, const GLchar* name);
typedef void(*proto_glBindBuffer) (GLenum target, GLuint buffer);
typedef void(*proto_glBindFramebuffer) (GLenum target, GLuint framebuffer);
typedef void(*proto_glBindRenderbuffer) (GLenum target, GLuint renderbuffer);
typedef void(*proto_glBindTexture) (GLenum target, GLuint texture);
typedef void(*proto_glBlendColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
typedef void(*proto_glBlendEquation) (GLenum mode);
typedef void(*proto_glBlendEquationSeparate) (GLenum modeRGB, GLenum modeAlpha);
typedef void(*proto_glBlendFunc) (GLenum sfactor, GLenum dfactor);
typedef void(*proto_glBlendFuncSeparate) (GLenum srcRGB, GLenum dstRGB, GLenum srcAlpha, GLenum dstAlpha);
typedef void(*proto_glBufferData) (GLenum target, GLsizeiptr size, const GLvoid* data, GLenum usage);
typedef void(*proto_glBufferSubData) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid* data);
typedef GLenum(*proto_glCheckFramebufferStatus) (GLenum target);
typedef void(*proto_glClear) (GLbitfield mask);
typedef void(*proto_glClearColor) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
typedef void(*proto_glClearDepthf) (GLclampf depth);
typedef void(*proto_glClearStencil) (GLint s);
typedef void(*proto_glColorMask) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
typedef void(*proto_glCompileShader) (GLuint shader);
typedef void(*proto_glCompressedTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid* data);
typedef void(*proto_glCompressedTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid* data);
typedef void(*proto_glCopyTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
typedef void(*proto_glCopyTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
typedef GLuint(*proto_glCreateProgram) (void);
typedef GLuint(*proto_glCreateShader) (GLenum type);
typedef void(*proto_glCullFace) (GLenum mode);
typedef void(*proto_glDeleteBuffers) (GLsizei n, const GLuint* buffers);
typedef void(*proto_glDeleteFramebuffers) (GLsizei n, const GLuint* framebuffers);
typedef void(*proto_glDeleteProgram) (GLuint program);
typedef void(*proto_glDeleteRenderbuffers) (GLsizei n, const GLuint* renderbuffers);
typedef void(*proto_glDeleteShader) (GLuint shader);
typedef void(*proto_glDeleteTextures) (GLsizei n, const GLuint* textures);
typedef void(*proto_glDepthFunc) (GLenum func);
typedef void(*proto_glDepthMask) (GLboolean flag);
typedef void(*proto_glDepthRangef) (GLclampf zNear, GLclampf zFar);
typedef void(*proto_glDetachShader) (GLuint program, GLuint shader);
typedef void(*proto_glDisable) (GLenum cap);
typedef void(*proto_glDisableVertexAttribArray) (GLuint index);
typedef void(*proto_glDrawArrays) (GLenum mode, GLint first, GLsizei count);
typedef void(*proto_glDrawElements) (GLenum mode, GLsizei count, GLenum type, const GLvoid* indices);
typedef void(*proto_glEnable) (GLenum cap);
typedef void(*proto_glEnableVertexAttribArray) (GLuint index);
typedef void(*proto_glFinish) (void);
typedef void(*proto_glFlush) (void);
typedef void(*proto_glFramebufferRenderbuffer) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
typedef void(*proto_glFramebufferTexture2D) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
typedef void(*proto_glFrontFace) (GLenum mode);
typedef void(*proto_glGenBuffers) (GLsizei n, GLuint* buffers);
typedef void(*proto_glGenerateMipmap) (GLenum target);
typedef void(*proto_glGenFramebuffers) (GLsizei n, GLuint* framebuffers);
typedef void(*proto_glGenRenderbuffers) (GLsizei n, GLuint* renderbuffers);
typedef void(*proto_glGenTextures) (GLsizei n, GLuint* textures);
typedef void(*proto_glGetActiveAttrib) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name);
typedef void(*proto_glGetActiveUniform) (GLuint program, GLuint index, GLsizei bufsize, GLsizei* length, GLint* size, GLenum* type, GLchar* name);
typedef void(*proto_glGetAttachedShaders) (GLuint program, GLsizei maxcount, GLsizei* count, GLuint* shaders);
typedef int(*proto_glGetAttribLocation) (GLuint program, const GLchar* name);
typedef void(*proto_glGetBooleanv) (GLenum pname, GLboolean* params);
typedef void(*proto_glGetBufferParameteriv) (GLenum target, GLenum pname, GLint* params);
typedef GLenum(*proto_glGetError) (void);
typedef void(*proto_glGetFloatv) (GLenum pname, GLfloat* params);
typedef void(*proto_glGetFramebufferAttachmentParameteriv) (GLenum target, GLenum attachment, GLenum pname, GLint* params);
typedef void(*proto_glGetIntegerv) (GLenum pname, GLint* params);
typedef void(*proto_glGetProgramiv) (GLuint program, GLenum pname, GLint* params);
typedef void(*proto_glGetProgramInfoLog) (GLuint program, GLsizei bufsize, GLsizei* length, GLchar* infolog);
typedef void(*proto_glGetRenderbufferParameteriv) (GLenum target, GLenum pname, GLint* params);
typedef void(*proto_glGetShaderiv) (GLuint shader, GLenum pname, GLint* params);
typedef void(*proto_glGetShaderInfoLog) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* infolog);
typedef void(*proto_glGetShaderPrecisionFormat) (GLenum shadertype, GLenum precisiontype, GLint* range, GLint* precision);
typedef void(*proto_glGetShaderSource) (GLuint shader, GLsizei bufsize, GLsizei* length, GLchar* source);
typedef const GLubyte*(*proto_glGetString) (GLenum name);
typedef void(*proto_glGetTexParameterfv) (GLenum target, GLenum pname, GLfloat* params);
typedef void(*proto_glGetTexParameteriv) (GLenum target, GLenum pname, GLint* params);
typedef void(*proto_glGetUniformfv) (GLuint program, GLint location, GLfloat* params);
typedef void(*proto_glGetUniformiv) (GLuint program, GLint location, GLint* params);
typedef int(*proto_glGetUniformLocation) (GLuint program, const GLchar* name);
typedef void(*proto_glGetVertexAttribfv) (GLuint index, GLenum pname, GLfloat* params);
typedef void(*proto_glGetVertexAttribiv) (GLuint index, GLenum pname, GLint* params);
typedef void(*proto_glGetVertexAttribPointerv) (GLuint index, GLenum pname, GLvoid** pointer);
typedef void(*proto_glHint) (GLenum target, GLenum mode);
typedef GLboolean(*proto_glIsBuffer) (GLuint buffer);
typedef GLboolean(*proto_glIsEnabled) (GLenum cap);
typedef GLboolean(*proto_glIsFramebuffer) (GLuint framebuffer);
typedef GLboolean(*proto_glIsProgram) (GLuint program);
typedef GLboolean(*proto_glIsRenderbuffer) (GLuint renderbuffer);
typedef GLboolean(*proto_glIsShader) (GLuint shader);
typedef GLboolean(*proto_glIsTexture) (GLuint texture);
typedef void(*proto_glLineWidth) (GLfloat width);
typedef void(*proto_glLinkProgram) (GLuint program);
typedef void(*proto_glPixelStorei) (GLenum pname, GLint param);
typedef void(*proto_glPolygonOffset) (GLfloat factor, GLfloat units);
typedef void(*proto_glReadPixels) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid* pixels);
typedef void(*proto_glReleaseShaderCompiler) (void);
typedef void(*proto_glRenderbufferStorage) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
typedef void(*proto_glSampleCoverage) (GLclampf value, GLboolean invert);
typedef void(*proto_glScissor) (GLint x, GLint y, GLsizei width, GLsizei height);
typedef void(*proto_glShaderBinary) (GLsizei n, const GLuint* shaders, GLenum binaryformat, const GLvoid* binary, GLsizei length);
typedef void(*proto_glShaderSource) (GLuint shader, GLsizei count, const GLchar** string, const GLint* length);
typedef void(*proto_glStencilFunc) (GLenum func, GLint ref, GLuint mask);
typedef void(*proto_glStencilFuncSeparate) (GLenum face, GLenum func, GLint ref, GLuint mask);
typedef void(*proto_glStencilMask) (GLuint mask);
typedef void(*proto_glStencilMaskSeparate) (GLenum face, GLuint mask);
typedef void(*proto_glStencilOp) (GLenum fail, GLenum zfail, GLenum zpass);
typedef void(*proto_glStencilOpSeparate) (GLenum face, GLenum fail, GLenum zfail, GLenum zpass);
typedef void(*proto_glTexImage2D) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid* pixels);
typedef void(*proto_glTexParameterf) (GLenum target, GLenum pname, GLfloat param);
typedef void(*proto_glTexParameterfv) (GLenum target, GLenum pname, const GLfloat* params);
typedef void(*proto_glTexParameteri) (GLenum target, GLenum pname, GLint param);
typedef void(*proto_glTexParameteriv) (GLenum target, GLenum pname, const GLint* params);
typedef void(*proto_glTexSubImage2D) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid* pixels);
typedef void(*proto_glUniform1f) (GLint location, GLfloat x);
typedef void(*proto_glUniform1fv) (GLint location, GLsizei count, const GLfloat* v);
typedef void(*proto_glUniform1i) (GLint location, GLint x);
typedef void(*proto_glUniform1iv) (GLint location, GLsizei count, const GLint* v);
typedef void(*proto_glUniform2f) (GLint location, GLfloat x, GLfloat y);
typedef void(*proto_glUniform2fv) (GLint location, GLsizei count, const GLfloat* v);
typedef void(*proto_glUniform2i) (GLint location, GLint x, GLint y);
typedef void(*proto_glUniform2iv) (GLint location, GLsizei count, const GLint* v);
typedef void(*proto_glUniform3f) (GLint location, GLfloat x, GLfloat y, GLfloat z);
typedef void(*proto_glUniform3fv) (GLint location, GLsizei count, const GLfloat* v);
typedef void(*proto_glUniform3i) (GLint location, GLint x, GLint y, GLint z);
typedef void(*proto_glUniform3iv) (GLint location, GLsizei count, const GLint* v);
typedef void(*proto_glUniform4f) (GLint location, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
typedef void(*proto_glUniform4fv) (GLint location, GLsizei count, const GLfloat* v);
typedef void(*proto_glUniform4i) (GLint location, GLint x, GLint y, GLint z, GLint w);
typedef void(*proto_glUniform4iv) (GLint location, GLsizei count, const GLint* v);
typedef void(*proto_glUniformMatrix2fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
typedef void(*proto_glUniformMatrix3fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
typedef void(*proto_glUniformMatrix4fv) (GLint location, GLsizei count, GLboolean transpose, const GLfloat* value);
typedef void(*proto_glUseProgram) (GLuint program);
typedef void(*proto_glValidateProgram) (GLuint program);
typedef void(*proto_glVertexAttrib1f) (GLuint indx, GLfloat x);
typedef void(*proto_glVertexAttrib1fv) (GLuint indx, const GLfloat* values);
typedef void(*proto_glVertexAttrib2f) (GLuint indx, GLfloat x, GLfloat y);
typedef void(*proto_glVertexAttrib2fv) (GLuint indx, const GLfloat* values);
typedef void(*proto_glVertexAttrib3f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z);
typedef void(*proto_glVertexAttrib3fv) (GLuint indx, const GLfloat* values);
typedef void(*proto_glVertexAttrib4f) (GLuint indx, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
typedef void(*proto_glVertexAttrib4fv) (GLuint indx, const GLfloat* values);
typedef void(*proto_glVertexAttribPointer) (GLuint indx, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid* ptr);
typedef void(*proto_glViewport) (GLint x, GLint y, GLsizei width, GLsizei height);

#endif /*linux */


#if MACOSX

#include <OpenGL/gl.h>
#include <OpenGL/glext.h>

#define DEF(type, func) type gltrace_##func
#define ORIG(func) func

#define NOT_IMPLEMENTED() \
{ \
fprintf(stderr, "FATAL: not implemented %s\n", __FUNCTION__); \
exit(1); \
}

#define GLESPROLOG(func)
#define GLESEPILOG()

#endif /* MACOSX */

#endif /* GLTRACE_GLES_H */
