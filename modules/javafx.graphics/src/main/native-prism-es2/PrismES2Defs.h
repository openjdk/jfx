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

#ifndef _Prism_es2_defs_h_
#define _Prism_es2_defs_h_

#include <stdint.h>

#if defined(SOLARIS) || defined(LINUX) /* SOLARIS || LINUX */
#define GLX_GLEXT_PROTOTYPES
#define GLX_GLXEXT_PROTOTYPES
#define UNIX
#if defined(IS_EGLX11) || defined(IS_EGLFB)
#define IS_EGL
#else
#define IS_GLX
#endif

#include <limits.h>

/* The Monocle build (IS_EGLFB, libprism_es2_monocle.so) compiles the generic sources against the
 * GL headers of this directory alone: no X11 / GLX development package, and no EGL - the EGL
 * include and the eglWrapper of commit 21d5a654f6 are gone; Java owns every EGL object and hands
 * the current context to es2_context_adopt. */
#ifndef IS_EGLFB
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#endif

#include <GL/gl.h>
#include <GL/glext.h>

#ifndef __USE_GNU
#define __USE_GNU
#endif
#include <dlfcn.h>

/* Max lenght of value, attr pair plus a None */
#define  MAX_GLX_ATTRS_LENGTH 50

#endif /* SOLARIS || LINUX */

#ifdef __APPLE__
#include <TargetConditionals.h>

#if TARGET_OS_MAC /* MacOSX */
/* Include the OpenGL headers */
#include <GL/gl.h>
#include <GL/glext.h>
#include <OpenGL/OpenGL.h>
#include <OpenGL/CGLTypes.h>
#include "macosx-window-system.h"

/* Max length of value, attr pair plus a None */
#define MAX_PF_ATTRS_LENGTH 50
#endif /* MacOSX */
#endif /* __APPLE__ */

#ifdef WIN32 /* WIN32 */
#include <windows.h>
#include <GL/gl.h>
#include <GL/wglext.h>
#include <GL/glext.h>

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0500
#endif
#endif /* WIN32 */

/* Typedef for pixelformat properties struct */
typedef struct PixelFormatInfoRec PixelFormatInfo;

/* define the structure to hold the resources and proerties of pixelformat */
struct PixelFormatInfoRec {
#ifdef WIN32 /* WIN32 */
    int pixelFormat;
    HWND dummyHwnd;
    HDC dummyHdc;
    LPCTSTR dummySzAppName;
#endif /* WIN32 */

#ifdef UNIX /* LINUX || SOLARIS */
#ifdef IS_EGLFB
    /* Monocle creates no pixel format (es2_pixel_format_create is a stub there: Java owns the EGL
     * config). One member keeps the struct non-empty. */
    void *unused;
#else
    Display *display;
    GLXFBConfig fbConfig;
    Window dummyWin;
    Colormap dummyCmap;
#endif
#endif

#ifdef MACOSX /* MACOSX */
    intptr_t pixelFormat;
#endif
};

/* Typedef for drawable properties struct */
typedef struct DrawableInfoRec DrawableInfo;

/* define the structure to hold the resources and proerties of drawable */
struct DrawableInfoRec {
    GLboolean onScreen;

#ifdef WIN32 /* WIN32 */
    HDC hdc;
    HWND hwnd;
#endif /* WIN32 */

#ifdef UNIX /* LINUX || SOLARIS */
#ifdef IS_EGL
    /* EGLDisplay / EGLSurface as opaque pointers: no EGL header is included any more (Java owns
     * every EGL object) and nothing in the Monocle build ever sets them. */
    void *egldisplay;
    void *eglsurface;
#endif
#ifndef IS_EGLFB
    Display *display;
    Window win;
#endif
#endif

#ifdef __APPLE__
    intptr_t win;
#endif /* __APPLE__ */
};

/* Typedef for state properties struct */
typedef struct StateInfoRec StateInfo;

/* define the structure to hold the states of context */
struct StateInfoRec {
    /* For state caching */
    GLboolean depthWritesEnabled;
    GLboolean scissorEnabled;
    GLclampf clearColor[4];
    GLboolean vSyncEnabled;

    /* For 3d state caching */
    GLboolean cullEnable;
    GLenum cullMode;
    GLenum fillMode;

    /* Currently bound fbo */
    GLuint fbo;
};

/* Typedef for context properties struct */
typedef struct ContextInfoRec ContextInfo;

/* define the structure to hold the properties of graphics context */
struct ContextInfoRec {
#ifdef WIN32 /* WIN32 */
    HGLRC hglrc;
#endif /* WIN32 */

#ifdef UNIX /* LINUX || SOLARIS */
#ifndef IS_EGLFB
    Display *display;
#endif
#ifdef IS_EGL
    /* EGLContext / EGLDisplay / EGLSurface as opaque pointers, always NULL on the ContextInfo
     * es2_context_adopt builds: the caller (Java) owns the context. */
    void *context;
    void *egldisplay;
    void *eglsurface;
#else
     GLXContext context;
#endif

#if defined(IS_GLX) || defined( IS_EGLX11)
    /*
     * display screen and visualID are cached
     * for Factory to pass to Glass
     */
    int screen;
    int visualID;
#endif
#endif

#ifdef __APPLE__
    intptr_t context;
#endif /* __APPLE__ */

    /* version and extension info */
    char *versionStr;
    char *vendorStr;
    char *rendererStr;
    char *glExtensionStr;
    int versionNumbers[2];

    /* platform specific extension string and function pointers */
#ifdef WIN32 /* WIN32 */
    char *wglExtensionStr;
    PFNWGLSWAPINTERVALEXTPROC wglSwapIntervalEXT;
#endif /* WIN32 */

#if defined(UNIX) && !defined(IS_EGL) /* LINUX || SOLARIS, GLX only */
    char *glxExtensionStr;
    PFNGLXSWAPINTERVALSGIPROC glXSwapIntervalSGI;
#endif /* LINUX || SOLARIS, GLX only */

    /* gl function pointers */
    PFNGLACTIVETEXTUREPROC glActiveTexture;
    PFNGLATTACHSHADERPROC glAttachShader;
    PFNGLBINDATTRIBLOCATIONPROC glBindAttribLocation;
    PFNGLBINDFRAMEBUFFERPROC glBindFramebuffer;
    PFNGLBINDRENDERBUFFERPROC glBindRenderbuffer;
    PFNGLCHECKFRAMEBUFFERSTATUSPROC glCheckFramebufferStatus;
    PFNGLCOMPILESHADERPROC glCompileShader;
    PFNGLCREATEPROGRAMPROC glCreateProgram;
    PFNGLCREATESHADERPROC glCreateShader;
    PFNGLDELETEBUFFERSPROC glDeleteBuffers;
    PFNGLDELETEFRAMEBUFFERSPROC glDeleteFramebuffers;
    PFNGLDELETEPROGRAMPROC glDeleteProgram;
    PFNGLDELETESHADERPROC glDeleteShader;
    PFNGLDELETERENDERBUFFERSPROC glDeleteRenderbuffers;
    PFNGLDETACHSHADERPROC glDetachShader;
    PFNGLDISABLEVERTEXATTRIBARRAYPROC glDisableVertexAttribArray;
    PFNGLENABLEVERTEXATTRIBARRAYPROC glEnableVertexAttribArray;
    PFNGLFRAMEBUFFERRENDERBUFFERPROC glFramebufferRenderbuffer;
    PFNGLFRAMEBUFFERTEXTURE2DPROC glFramebufferTexture2D;
    PFNGLGENFRAMEBUFFERSPROC glGenFramebuffers;
    PFNGLGENRENDERBUFFERSPROC glGenRenderbuffers;
    PFNGLGETPROGRAMIVPROC glGetProgramiv;
    PFNGLGETSHADERIVPROC glGetShaderiv;
    PFNGLGETUNIFORMLOCATIONPROC glGetUniformLocation;
    PFNGLLINKPROGRAMPROC glLinkProgram;
    PFNGLRENDERBUFFERSTORAGEPROC glRenderbufferStorage;
    PFNGLSHADERSOURCEPROC glShaderSource;
    PFNGLGETSHADERINFOLOGPROC glGetShaderInfoLog;
    PFNGLGETPROGRAMINFOLOGPROC glGetProgramInfoLog;
    PFNGLBUFFERSUBDATAPROC glBufferSubData;
    PFNGLUNIFORM1FPROC glUniform1f;
    PFNGLUNIFORM2FPROC glUniform2f;
    PFNGLUNIFORM3FPROC glUniform3f;
    PFNGLUNIFORM4FPROC glUniform4f;
    PFNGLUNIFORM4FVPROC glUniform4fv;
    PFNGLUNIFORM1IPROC glUniform1i;
    PFNGLUNIFORM2IPROC glUniform2i;
    PFNGLUNIFORM3IPROC glUniform3i;
    PFNGLUNIFORM4IPROC glUniform4i;
    PFNGLUNIFORM4IVPROC glUniform4iv;
    PFNGLUNIFORMMATRIX4FVPROC glUniformMatrix4fv;
    PFNGLUSEPROGRAMPROC glUseProgram;
    PFNGLVALIDATEPROGRAMPROC glValidateProgram;
    PFNGLVERTEXATTRIBPOINTERPROC glVertexAttribPointer;

    PFNGLGENBUFFERSPROC glGenBuffers;
    PFNGLBINDBUFFERPROC glBindBuffer;
    PFNGLBUFFERDATAPROC glBufferData;
    PFNGLTEXIMAGE2DMULTISAMPLEPROC glTexImage2DMultisample;
    PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC glRenderbufferStorageMultisample;
    PFNGLBLITFRAMEBUFFERPROC glBlitFramebuffer;

    /* For state caching */
    StateInfo state;

    /* this pointers represent cached values of glVertexAttribPointer values */
    /* they should be properly updated in case of glVertexAttribPointer call */
    /* see setVertexAttributePointers */
    float *vbFloatData;
    char  *vbByteData;
    GLboolean gl2;

    /* Caching properties passed down from Java */
    GLboolean vSyncRequested;
};

// extern declarations for core functions
extern int isExtensionSupported(const char *allExtensions, const char *extension);
extern void extractVersionInfo(char *versionStr, int *numbers);
extern void initializeCtxInfo(ContextInfo *ctxInfo);
extern void initializePixelFormatInfo(PixelFormatInfo *pfInfo);
extern void initState(ContextInfo *ctxInfo);
extern void deletePixelFormatInfo(PixelFormatInfo *pfInfo);

/* Define 3D Primitive data type */
/* define constants and structures to hold the resources of 3D primitive and rendering
 * attributes */
#define VC_3D_INDEX 0
#define TC_3D_INDEX 1
#define NC_3D_INDEX 2
#define VC_3D_SIZE 3  /* x, y, z */
#define TC_3D_SIZE 2  /* tu, tv */
#define NC_3D_SIZE 4  /* nx, ny, nz, nw */
#define VERT_3D_SIZE (VC_3D_SIZE + TC_3D_SIZE + NC_3D_SIZE)
#define VERT_3D_STRIDE (sizeof(GLfloat) * VERT_3D_SIZE)

#define MESH_VERTEXBUFFER 0
#define MESH_INDEXBUFFER 1
#define MESH_MAX_BUFFERS 2

typedef struct MeshInfoRec MeshInfo;
struct MeshInfoRec {
    // vboIDArray[MESH_VERTEXBUFFER] used to store interleave points and tex. coords.
    // vboIDArray[MESH_INDEXBUFFER] used to store element indices
    GLuint vboIDArray[MESH_MAX_BUFFERS];
    GLuint indexBufferSize;
    GLenum indexBufferType;
};

typedef struct PhongMaterialInfoRec PhongMaterialInfo;
struct PhongMaterialInfoRec {
   GLfloat diffuseColor[4]; // in the order of rgba
   GLuint maps[4];
};

typedef struct MeshViewInfoRec MeshViewInfo;
struct MeshViewInfoRec {
    MeshInfo *meshInfo;
    PhongMaterialInfo *phongMaterialInfo;
    GLfloat ambientLightColor[3];
    GLuint lightIndex;
    GLfloat lightColor[3];
    GLfloat lightPosition[3];
    GLfloat lightWeight;
    GLfloat lightAttenuation[4];
    GLfloat lightMaxRange;
    GLfloat lightDir[3];
    GLfloat lightInnerAngle;
    GLfloat lightOuterAngle;
    GLfloat lightFalloff;
    GLboolean cullEnable;
    GLenum cullMode;
    GLenum fillMode;
};

/*
 * General purpose assertion macro
 */
#define PRISMES2_ASSERT(expr) \
    if (!(expr)) { \
        fprintf(stderr, \
            "\nAssertion failed in module '%s' at line %d\n", \
            __FILE__, __LINE__); \
        fprintf(stderr, "\t%s\n\n", #expr);     \
    }

#endif /* _Prism_es2_defs_h_ */
