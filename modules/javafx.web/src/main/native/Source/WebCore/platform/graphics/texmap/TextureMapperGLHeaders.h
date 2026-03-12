/*
 * Copyright (C) 2017 Igalia S.L.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#if USE(LIBEPOXY)
#include <epoxy/gl.h>
#else
#if !PLATFORM(JAVA)
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#endif
#endif

#if PLATFORM(JAVA)
    using GLenum = unsigned int;
    using GLuint = unsigned int;
    using GLint  = int;
    using GLsizei = int;
    using GLfloat = float;
    using GLsizeiptr = ptrdiff_t;
    #define GL_RGBA 0x1908
    #define GL_DEPTH_COMPONENT16 0x81A5
    constexpr GLenum GL_MAX_TEXTURE_SIZE = 0x0D33u;
    constexpr GLenum GL_ARRAY_BUFFER         = 0x8892;
    constexpr GLenum GL_ELEMENT_ARRAY_BUFFER = 0x8893;
    constexpr GLenum GL_STATIC_DRAW          = 0x88E4;
    constexpr GLenum GL_DYNAMIC_DRAW         = 0x88E8;
    constexpr GLenum GL_UNSIGNED_BYTE = 0x1401;
    constexpr GLenum GL_UNSIGNED_SHORT = 0x1403;
    constexpr GLenum GL_UNSIGNED_INT   = 0x1405;
#endif

#ifndef GL_BGRA
#define GL_BGRA 0x80E1
#endif

#ifndef GL_TEXTURE_RECTANGLE_ARB
#define GL_TEXTURE_RECTANGLE_ARB 0x84F5
#endif

#ifndef GL_UNPACK_ROW_LENGTH
#define GL_UNPACK_ROW_LENGTH 0x0CF2
#endif

#ifndef GL_UNPACK_SKIP_ROWS
#define GL_UNPACK_SKIP_ROWS 0x0CF3
#endif

#ifndef GL_UNPACK_SKIP_PIXELS
#define GL_UNPACK_SKIP_PIXELS 0x0CF4
#endif

#ifndef GL_TEXTURE_EXTERNAL_OES
#define GL_TEXTURE_EXTERNAL_OES 0x8D65
#endif
