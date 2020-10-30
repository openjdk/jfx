#include <jni.h>
extern long getNativeWindowHandle(const char *v);
extern long getEGLDisplayHandle();
extern jboolean doEglInitialize(void* handle);
extern jboolean doEglBindApi(int api);
extern jlong doEglChooseConfig (long eglDisplay, int* attribs);

extern jlong doEglCreateWindowSurface (jlong eglDisplay, jlong config,
     jlong nativeWindow);

extern jlong doEglCreateContext (jlong eglDisplay, jlong config);

extern jboolean doEglMakeCurrent (jlong eglDisplay, jlong drawSurface, 
     jlong readSurface, jlong eglContext);

extern jboolean doEglSwapBuffers(jlong eglDisplay, jlong eglSurface);
