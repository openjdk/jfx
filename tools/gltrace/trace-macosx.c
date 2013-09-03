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
#include <stdio.h>

#include <OpenGL/gl.h>
#include <OpenGL/glext.h>

#include "os.h"
#include "iolib.h"
#include "trace.h"

static int tLevel = trcLevel;

#define INTERPOSE(func) \
extern void gltrace_##func(); \
void* interpose_##func[] __attribute__ ((section("__DATA, __interpose"))) = { gltrace_##func, func }

INTERPOSE(glActiveTexture);
INTERPOSE(glAttachShader);
INTERPOSE(glBindAttribLocation);
INTERPOSE(glBindBuffer);
INTERPOSE(glBindFramebuffer);
INTERPOSE(glBindRenderbuffer);
INTERPOSE(glBindTexture);
INTERPOSE(glBlendColor);
INTERPOSE(glBlendEquation);
INTERPOSE(glBlendEquationSeparate);
INTERPOSE(glBlendFunc);
INTERPOSE(glBlendFuncSeparate);
INTERPOSE(glBufferData);
INTERPOSE(glBufferSubData);
INTERPOSE(glCheckFramebufferStatus);
INTERPOSE(glClear);
INTERPOSE(glClearColor);
#if !MACOSX
INTERPOSE(glClearDepthf);
#endif
INTERPOSE(glClearStencil);
INTERPOSE(glColorMask);
INTERPOSE(glCompileShader);
INTERPOSE(glCompressedTexImage2D);
INTERPOSE(glCompressedTexSubImage2D);
INTERPOSE(glCopyTexImage2D);
INTERPOSE(glCopyTexSubImage2D);
INTERPOSE(glCreateProgram);
INTERPOSE(glCreateShader);
INTERPOSE(glCullFace);
INTERPOSE(glDeleteBuffers);
INTERPOSE(glDeleteFramebuffers);
INTERPOSE(glDeleteProgram);
INTERPOSE(glDeleteRenderbuffers);
INTERPOSE(glDeleteShader);
INTERPOSE(glDeleteTextures);
INTERPOSE(glDepthFunc);
INTERPOSE(glDepthMask);
#if !MACOSX
INTERPOSE(glDepthRangef);
#endif
INTERPOSE(glDetachShader);
INTERPOSE(glDisable);
INTERPOSE(glDisableVertexAttribArray);
INTERPOSE(glDrawArrays);
INTERPOSE(glDrawElements);
INTERPOSE(glEnable);
INTERPOSE(glEnableVertexAttribArray);
INTERPOSE(glFinish);
INTERPOSE(glFlush);
INTERPOSE(glFramebufferRenderbuffer);
INTERPOSE(glFramebufferTexture2D);
INTERPOSE(glFrontFace);
INTERPOSE(glGenBuffers);
INTERPOSE(glGenerateMipmap);
INTERPOSE(glGenFramebuffers);
INTERPOSE(glGenRenderbuffers);
INTERPOSE(glGenTextures);
INTERPOSE(glGetActiveAttrib);
INTERPOSE(glGetActiveUniform);
INTERPOSE(glGetAttachedShaders);
INTERPOSE(glGetAttribLocation);
INTERPOSE(glGetBooleanv);
INTERPOSE(glGetBufferParameteriv);
INTERPOSE(glGetError);
INTERPOSE(glGetFloatv);
INTERPOSE(glGetFramebufferAttachmentParameteriv);
INTERPOSE(glGetIntegerv);
INTERPOSE(glGetProgramiv);
INTERPOSE(glGetProgramInfoLog);
INTERPOSE(glGetRenderbufferParameteriv);
INTERPOSE(glGetShaderiv);
INTERPOSE(glGetShaderInfoLog);
#if !MACOSX
INTERPOSE(glGetShaderPrecisionFormat);
#endif
INTERPOSE(glGetShaderSource);
INTERPOSE(glGetString);
INTERPOSE(glGetTexParameterfv);
INTERPOSE(glGetTexParameteriv);
INTERPOSE(glGetUniformfv);
INTERPOSE(glGetUniformiv);
INTERPOSE(glGetUniformLocation);
INTERPOSE(glGetVertexAttribfv);
INTERPOSE(glGetVertexAttribiv);
INTERPOSE(glGetVertexAttribPointerv);
INTERPOSE(glHint);
INTERPOSE(glIsBuffer);
INTERPOSE(glIsEnabled);
INTERPOSE(glIsFramebuffer);
INTERPOSE(glIsProgram);
INTERPOSE(glIsRenderbuffer);
INTERPOSE(glIsShader);
INTERPOSE(glIsTexture);
INTERPOSE(glLineWidth);
INTERPOSE(glLinkProgram);
INTERPOSE(glPixelStorei);
INTERPOSE(glPolygonOffset);
INTERPOSE(glReadPixels);
#if !MACOSX
INTERPOSE(glReleaseShaderCompiler);
#endif
INTERPOSE(glRenderbufferStorage);
INTERPOSE(glSampleCoverage);
INTERPOSE(glScissor);
#if !MACOSX
INTERPOSE(glShaderBinary);
#endif
INTERPOSE(glShaderSource);
INTERPOSE(glStencilFunc);
INTERPOSE(glStencilFuncSeparate);
INTERPOSE(glStencilMask);
INTERPOSE(glStencilMaskSeparate);
INTERPOSE(glStencilOp);
INTERPOSE(glStencilOpSeparate);
INTERPOSE(glTexImage2D);
INTERPOSE(glTexParameterf);
INTERPOSE(glTexParameterfv);
INTERPOSE(glTexParameteri);
INTERPOSE(glTexParameteriv);
INTERPOSE(glTexSubImage2D);
INTERPOSE(glUniform1f);
INTERPOSE(glUniform1fv);
INTERPOSE(glUniform1i);
INTERPOSE(glUniform1iv);
INTERPOSE(glUniform2f);
INTERPOSE(glUniform2fv);
INTERPOSE(glUniform2i);
INTERPOSE(glUniform2iv);
INTERPOSE(glUniform3f);
INTERPOSE(glUniform3fv);
INTERPOSE(glUniform3i);
INTERPOSE(glUniform3iv);
INTERPOSE(glUniform4f);
INTERPOSE(glUniform4fv);
INTERPOSE(glUniform4i);
INTERPOSE(glUniform4iv);
INTERPOSE(glUniformMatrix2fv);
INTERPOSE(glUniformMatrix3fv);
INTERPOSE(glUniformMatrix4fv);
INTERPOSE(glUseProgram);
INTERPOSE(glValidateProgram);
INTERPOSE(glVertexAttrib1f);
INTERPOSE(glVertexAttrib1fv);
INTERPOSE(glVertexAttrib2f);
INTERPOSE(glVertexAttrib2fv);
INTERPOSE(glVertexAttrib3f);
INTERPOSE(glVertexAttrib3fv);
INTERPOSE(glVertexAttrib4f);
INTERPOSE(glVertexAttrib4fv);
INTERPOSE(glVertexAttribPointer);
INTERPOSE(glViewport);

#if MACOSX
INTERPOSE(glBegin);
INTERPOSE(glEnd);
INTERPOSE(glIsRenderbufferEXT);
INTERPOSE(glBindRenderbufferEXT);
INTERPOSE(glDeleteRenderbuffersEXT);
INTERPOSE(glGenRenderbuffersEXT);
INTERPOSE(glRenderbufferStorageEXT);
INTERPOSE(glGetRenderbufferParameterivEXT);
INTERPOSE(glIsFramebufferEXT);
INTERPOSE(glBindFramebufferEXT);
INTERPOSE(glDeleteFramebuffersEXT);
INTERPOSE(glGenFramebuffersEXT);
INTERPOSE(glCheckFramebufferStatusEXT);
INTERPOSE(glFramebufferTexture1DEXT);
INTERPOSE(glFramebufferTexture2DEXT);
INTERPOSE(glFramebufferTexture3DEXT);
INTERPOSE(glFramebufferRenderbufferEXT);
INTERPOSE(glGetFramebufferAttachmentParameterivEXT);
INTERPOSE(glGenerateMipmapEXT);
INTERPOSE(glGenFencesAPPLE);
INTERPOSE(glDeleteFencesAPPLE);
INTERPOSE(glSetFenceAPPLE);
INTERPOSE(glIsFenceAPPLE);
INTERPOSE(glTestFenceAPPLE);
INTERPOSE(glFinishFenceAPPLE);
INTERPOSE(glTestObjectAPPLE);
INTERPOSE(glFinishObjectAPPLE);
#endif

/*
 *    Init/fini
 */

static void init() __attribute__ ((constructor));
static void
init()
{
    iolib_init(IO_WRITE, NULL);
    if (tLevel >= dbgLevel) {
        fprintf(stderr, "INTERPOSITION STARTED\n");
    }
}

static void fini() __attribute__ ((destructor));
static void fini()
{
    iolib_fini();    
    if (tLevel >= dbgLevel) {
        fprintf(stderr, "INTERPOSITION FINISHED\n");
    }
}
