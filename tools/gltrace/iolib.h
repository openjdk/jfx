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
 
#ifndef OGLTRACE_IOLIB_H
#define OGLTRACE_IOLIB_H

#include <stdint.h>

#define OPC_NONE                                        0
#define OPC_START                                       100
#define OPC_glActiveTexture                             OPC_START+0
#define OPC_glAttachShader                              OPC_START+1
#define OPC_glBindAttribLocation                        OPC_START+2
#define OPC_glBindBuffer                                OPC_START+3
#define OPC_glBindFramebuffer                           OPC_START+4
#define OPC_glBindRenderbuffer                          OPC_START+5
#define OPC_glBindTexture                               OPC_START+6
#define OPC_glBlendColor                                OPC_START+7
#define OPC_glBlendEquation                             OPC_START+8
#define OPC_glBlendEquationSeparate                     OPC_START+9
#define OPC_glBlendFunc                                 OPC_START+10
#define OPC_glBlendFuncSeparate                         OPC_START+11
#define OPC_glBufferData                                OPC_START+12
#define OPC_glBufferSubData                             OPC_START+13
#define OPC_glCheckFramebufferStatus                    OPC_START+14
#define OPC_glClear                                     OPC_START+15
#define OPC_glClearColor                                OPC_START+16
#define OPC_glClearDepthf                               OPC_START+17
#define OPC_glClearStencil                              OPC_START+18
#define OPC_glColorMask                                 OPC_START+19
#define OPC_glCompileShader                             OPC_START+20
#define OPC_glCompressedTexImage2D                      OPC_START+21
#define OPC_glCompressedTexSubImage2D                   OPC_START+22
#define OPC_glCopyTexImage2D                            OPC_START+23
#define OPC_glCopyTexSubImage2D                         OPC_START+24
#define OPC_glCreateProgram                             OPC_START+25
#define OPC_glCreateShader                              OPC_START+26
#define OPC_glCullFace                                  OPC_START+27
#define OPC_glDeleteBuffers                             OPC_START+28
#define OPC_glDeleteFramebuffers                        OPC_START+29
#define OPC_glDeleteProgram                             OPC_START+30
#define OPC_glDeleteRenderbuffers                       OPC_START+31
#define OPC_glDeleteShader                              OPC_START+32
#define OPC_glDeleteTextures                            OPC_START+33
#define OPC_glDepthFunc                                 OPC_START+34
#define OPC_glDepthMask                                 OPC_START+35
#define OPC_glDepthRangef                               OPC_START+36
#define OPC_glDetachShader                              OPC_START+37
#define OPC_glDisable                                   OPC_START+38
#define OPC_glDisableVertexAttribArray                  OPC_START+39
#define OPC_glDrawArrays                                OPC_START+40
#define OPC_glDrawElements                              OPC_START+41
#define OPC_glEnable                                    OPC_START+42
#define OPC_glEnableVertexAttribArray                   OPC_START+43
#define OPC_glFinish                                    OPC_START+44
#define OPC_glFlush                                     OPC_START+45
#define OPC_glFramebufferRenderbuffer                   OPC_START+46
#define OPC_glFramebufferTexture2D                      OPC_START+47
#define OPC_glFrontFace                                 OPC_START+48
#define OPC_glGenBuffers                                OPC_START+49
#define OPC_glGenerateMipmap                            OPC_START+50
#define OPC_glGenFramebuffers                           OPC_START+51
#define OPC_glGenRenderbuffers                          OPC_START+52
#define OPC_glGenTextures                               OPC_START+53
#define OPC_glGetActiveAttrib                           OPC_START+54
#define OPC_glGetActiveUniform                          OPC_START+55
#define OPC_glGetAttachedShaders                        OPC_START+56
#define OPC_glGetAttribLocation                         OPC_START+57
#define OPC_glGetBooleanv                               OPC_START+58
#define OPC_glGetBufferParameteriv                      OPC_START+59
#define OPC_glGetError                                  OPC_START+60
#define OPC_glGetFloatv                                 OPC_START+61
#define OPC_glGetFramebufferAttachmentParameteriv       OPC_START+62
#define OPC_glGetIntegerv                               OPC_START+63
#define OPC_glGetProgramiv                              OPC_START+64
#define OPC_glGetProgramInfoLog                         OPC_START+65
#define OPC_glGetRenderbufferParameteriv                OPC_START+66
#define OPC_glGetShaderiv                               OPC_START+67
#define OPC_glGetShaderInfoLog                          OPC_START+68
#define OPC_glGetShaderPrecisionFormat                  OPC_START+69
#define OPC_glGetShaderSource                           OPC_START+70
#define OPC_glGetString                                 OPC_START+71
#define OPC_glGetTexParameterfv                         OPC_START+72
#define OPC_glGetTexParameteriv                         OPC_START+73
#define OPC_glGetUniformfv                              OPC_START+74
#define OPC_glGetUniformiv                              OPC_START+75
#define OPC_glGetUniformLocation                        OPC_START+76
#define OPC_glGetVertexAttribfv                         OPC_START+77
#define OPC_glGetVertexAttribiv                         OPC_START+78
#define OPC_glGetVertexAttribPointerv                   OPC_START+79
#define OPC_glHint                                      OPC_START+80
#define OPC_glIsBuffer                                  OPC_START+81
#define OPC_glIsEnabled                                 OPC_START+82
#define OPC_glIsFramebuffer                             OPC_START+83
#define OPC_glIsProgram                                 OPC_START+84
#define OPC_glIsRenderbuffer                            OPC_START+85
#define OPC_glIsShader                                  OPC_START+86
#define OPC_glIsTexture                                 OPC_START+87
#define OPC_glLineWidth                                 OPC_START+88
#define OPC_glLinkProgram                               OPC_START+89
#define OPC_glPixelStorei                               OPC_START+90
#define OPC_glPolygonOffset                             OPC_START+91
#define OPC_glReadPixels                                OPC_START+92
#define OPC_glReleaseShaderCompiler                     OPC_START+93
#define OPC_glRenderbufferStorage                       OPC_START+94
#define OPC_glSampleCoverage                            OPC_START+95
#define OPC_glScissor                                   OPC_START+96
#define OPC_glShaderBinary                              OPC_START+97
#define OPC_glShaderSource                              OPC_START+98
#define OPC_glStencilFunc                               OPC_START+99
#define OPC_glStencilFuncSeparate                       OPC_START+100
#define OPC_glStencilMask                               OPC_START+101
#define OPC_glStencilMaskSeparate                       OPC_START+102
#define OPC_glStencilOp                                 OPC_START+103
#define OPC_glStencilOpSeparate                         OPC_START+104
#define OPC_glTexImage2D                                OPC_START+105
#define OPC_glTexParameterf                             OPC_START+106
#define OPC_glTexParameterfv                            OPC_START+107
#define OPC_glTexParameteri                             OPC_START+108
#define OPC_glTexParameteriv                            OPC_START+109
#define OPC_glTexSubImage2D                             OPC_START+110
#define OPC_glUniform1f                                 OPC_START+111
#define OPC_glUniform1fv                                OPC_START+112
#define OPC_glUniform1i                                 OPC_START+113
#define OPC_glUniform1iv                                OPC_START+114
#define OPC_glUniform2f                                 OPC_START+115
#define OPC_glUniform2fv                                OPC_START+116
#define OPC_glUniform2i                                 OPC_START+117
#define OPC_glUniform2iv                                OPC_START+118
#define OPC_glUniform3f                                 OPC_START+119
#define OPC_glUniform3fv                                OPC_START+120
#define OPC_glUniform3i                                 OPC_START+121
#define OPC_glUniform3iv                                OPC_START+122
#define OPC_glUniform4f                                 OPC_START+123
#define OPC_glUniform4fv                                OPC_START+124
#define OPC_glUniform4i                                 OPC_START+125
#define OPC_glUniform4iv                                OPC_START+126
#define OPC_glUniformMatrix2fv                          OPC_START+127
#define OPC_glUniformMatrix3fv                          OPC_START+128
#define OPC_glUniformMatrix4fv                          OPC_START+129
#define OPC_glUseProgram                                OPC_START+130
#define OPC_glValidateProgram                           OPC_START+131
#define OPC_glVertexAttrib1f                            OPC_START+132
#define OPC_glVertexAttrib1fv                           OPC_START+133
#define OPC_glVertexAttrib2f                            OPC_START+134
#define OPC_glVertexAttrib2fv                           OPC_START+135
#define OPC_glVertexAttrib3f                            OPC_START+136
#define OPC_glVertexAttrib3fv                           OPC_START+137
#define OPC_glVertexAttrib4f                            OPC_START+138
#define OPC_glVertexAttrib4fv                           OPC_START+139
#define OPC_glVertexAttribPointer                       OPC_START+140
#define OPC_glViewport                                  OPC_START+141

#define OPC_eglGetError                                 OPC_START+200
#define OPC_eglGetDisplay                               OPC_START+201
#define OPC_eglInitialize                               OPC_START+202
#define OPC_eglTerminate                                OPC_START+203
#define OPC_eglQueryString                              OPC_START+204
#define OPC_eglGetConfigs                               OPC_START+205
#define OPC_eglChooseConfig                             OPC_START+206
#define OPC_eglGetConfigAttrib                          OPC_START+207
#define OPC_eglCreateWindowSurface                      OPC_START+208
#define OPC_eglCreatePbufferSurface                     OPC_START+209
#define OPC_eglCreatePixmapSurface                      OPC_START+210
#define OPC_eglDestroySurface                           OPC_START+211
#define OPC_eglQuerySurface                             OPC_START+212
#define OPC_eglBindAPI                                  OPC_START+213
#define OPC_eglQueryAPI                                 OPC_START+214
#define OPC_eglWaitClient                               OPC_START+215
#define OPC_eglReleaseThread                            OPC_START+216
#define OPC_eglCreatePbufferFromClientBuffer            OPC_START+217
#define OPC_eglSurfaceAttrib                            OPC_START+218
#define OPC_eglBindTexImage                             OPC_START+219
#define OPC_eglReleaseTexImage                          OPC_START+220
#define OPC_eglCreateContext                            OPC_START+221
#define OPC_eglDestroyContext                           OPC_START+222
#define OPC_eglMakeCurrent                              OPC_START+223
#define OPC_eglGetCurrentContext                        OPC_START+224
#define OPC_eglGetCurrentSurface                        OPC_START+225
#define OPC_eglGetCurrentDisplay                        OPC_START+226
#define OPC_eglQueryContext                             OPC_START+227
#define OPC_eglWaitGL                                   OPC_START+228
#define OPC_eglWaitNative                               OPC_START+229
#define OPC_eglSwapBuffers                              OPC_START+230
#define OPC_eglCopyBuffers                              OPC_START+231

#define OPC_EOF                                         OPC_START+500


#define IO_WRITE        0
#define IO_READ         1

void    iolib_init(int mode, const char *fname);
void    iolib_fini();

void    putCmd(int cmd);
void    putInt(int arg);
void    putFloat(float arg);
void    putLongLong(long long arg);
void    putPtr(const void *arg);
void    putString(const char *str);
void    putBytes(const void *data, int size);
void    putTime(uint64_t bgn, uint64_t end);

int             getCmd();
int             getInt();
float           getFloat();
long long       getLongLong();
const void      *getPtr();
const char      *getString();
const void      *getBytes();
void            getTime(uint64_t *bgn, uint64_t *end);

#endif /* OGLTRACE_IOLIB_H */
