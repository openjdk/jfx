/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "glass_general.h"

#include <jni.h>
#include <gtk/gtk.h>

char const * const GDK_WINDOW_DATA_CONTEXT = "glass_window_context";

jclass jStringCls;
jclass jByteBufferCls;
jmethodID jByteBufferArray;
jmethodID jByteBufferWrap;

jclass jRunnableCls;
jmethodID jRunnableRun;

jclass jArrayListCls;
jmethodID jArrayListInit;
jmethodID jArrayListAdd;
jmethodID jArrayListGetIdx;

jmethodID jPixelsAttachData;

jclass jGtkPixelsCls;
jmethodID jGtkPixelsInit;

jclass jScreenCls;
jmethodID jScreenInit;
jmethodID jScreenNotifySettingsChanged;

jmethodID jViewNotifyResize;
jmethodID jViewNotifyMouse;
jmethodID jViewNotifyRepaint;
jmethodID jViewNotifyKey;
jmethodID jViewNotifyView;
jmethodID jViewNotifyDragEnter;
jmethodID jViewNotifyDragOver;
jmethodID jViewNotifyDragDrop;
jmethodID jViewNotifyDragLeave;
jmethodID jViewNotifyScroll;
jmethodID jViewNotifyInputMethod;
jmethodID jViewNotifyInputMethodDraw;
jmethodID jViewNotifyInputMethodCaret;
jmethodID jViewNotifyPreeditMode;
jmethodID jViewNotifyMenu;
jfieldID  jViewPtr;

jmethodID jWindowNotifyResize;
jmethodID jWindowNotifyMove;
jmethodID jWindowNotifyDestroy;
jmethodID jWindowNotifyClose;
jmethodID jWindowNotifyFocus;
jmethodID jWindowNotifyFocusDisabled;
jmethodID jWindowNotifyFocusUngrab;
jmethodID jWindowNotifyMoveToAnotherScreen;
jmethodID jWindowNotifyLevelChanged;
jmethodID jWindowIsEnabled;
jmethodID jWindowNotifyDelegatePtr;
jfieldID jWindowPtr;
jfieldID jCursorPtr;

jmethodID jGtkWindowNotifyStateChanged;

jmethodID jClipboardContentChanged;

jmethodID jSizeInit;

jmethodID jMapGet;
jmethodID jMapKeySet;
jmethodID jMapContainsKey;

jclass jHashSetCls;
jmethodID jHashSetInit;

jmethodID jSetAdd;
jmethodID jSetSize;
jmethodID jSetToArray;

jmethodID jIterableIterator;
jmethodID jIteratorHasNext;
jmethodID jIteratorNext;

jclass jApplicationCls;
jfieldID jApplicationDisplay;
jfieldID jApplicationScreen;
jfieldID jApplicationVisualID;
jmethodID jApplicationReportException;
jmethodID jApplicationGetApplication;
jmethodID jApplicationGetName;

static jboolean displayValid = JNI_FALSE;

jboolean
is_display_valid() {
    return displayValid;
}

JavaVM* javaVM;

#ifdef STATIC_BUILD
extern "C" {
#endif
JNIEXPORT jint JNICALL
#ifdef STATIC_BUILD
JNI_OnLoad_glassgtk3(JavaVM *jvm, void *reserved)
#else
JNI_OnLoad(JavaVM *jvm, void *reserved)
#endif
{
    (void)reserved;

    JNIEnv *env;
    jclass clazz;
    Display* display;

    javaVM = jvm;
    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6)) {
         return JNI_ERR; /* JNI version not supported */
    }

    clazz = env->FindClass("java/lang/String");
    if (env->ExceptionCheck()) return JNI_ERR;
    jStringCls = (jclass) env->NewGlobalRef(clazz);

    clazz = env->FindClass("java/nio/ByteBuffer");
    if (env->ExceptionCheck()) return JNI_ERR;
    jByteBufferCls = (jclass) env->NewGlobalRef(clazz);
    jByteBufferArray = env->GetMethodID(jByteBufferCls, "array", "()[B");
    if (env->ExceptionCheck()) return JNI_ERR;
    jByteBufferWrap = env->GetStaticMethodID(jByteBufferCls, "wrap", "([B)Ljava/nio/ByteBuffer;");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/lang/Runnable");
    if (env->ExceptionCheck()) return JNI_ERR;

    jRunnableRun = env->GetMethodID(clazz, "run", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/util/ArrayList");
    if (env->ExceptionCheck()) return JNI_ERR;
    jArrayListCls = (jclass) env->NewGlobalRef(clazz);
    jArrayListInit = env->GetMethodID(jArrayListCls, "<init>", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jArrayListAdd = env->GetMethodID(jArrayListCls, "add", "(Ljava/lang/Object;)Z");
    if (env->ExceptionCheck()) return JNI_ERR;
    jArrayListGetIdx = env->GetMethodID(jArrayListCls, "get", "(I)Ljava/lang/Object;");
    if (env->ExceptionCheck()) return JNI_ERR;
    clazz = env->FindClass("com/sun/glass/ui/Pixels");
    if (env->ExceptionCheck()) return JNI_ERR;
    jPixelsAttachData = env->GetMethodID(clazz, "attachData", "(J)V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkPixels");
    if (env->ExceptionCheck()) return JNI_ERR;

    jGtkPixelsCls = (jclass) env->NewGlobalRef(clazz);
    jGtkPixelsInit = env->GetMethodID(jGtkPixelsCls, "<init>", "(IILjava/nio/ByteBuffer;)V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/Screen");
    if (env->ExceptionCheck()) return JNI_ERR;
    jScreenCls = (jclass) env->NewGlobalRef(clazz);
    jScreenInit = env->GetMethodID(jScreenCls, "<init>", "(JIIIIIIIIIIIIIIIFFFF)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jScreenNotifySettingsChanged = env->GetStaticMethodID(jScreenCls, "notifySettingsChanged", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/View");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyResize = env->GetMethodID(clazz, "notifyResize", "(II)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyMouse = env->GetMethodID(clazz, "notifyMouse", "(IIIIIIIZZ)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyRepaint = env->GetMethodID(clazz, "notifyRepaint", "(IIII)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyKey = env->GetMethodID(clazz, "notifyKey", "(II[CI)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyView = env->GetMethodID(clazz, "notifyView", "(I)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyDragEnter = env->GetMethodID(clazz, "notifyDragEnter", "(IIIII)I");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyDragOver = env->GetMethodID(clazz, "notifyDragOver", "(IIIII)I");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyDragDrop = env->GetMethodID(clazz, "notifyDragDrop", "(IIIII)I");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyDragLeave = env->GetMethodID(clazz, "notifyDragLeave", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyScroll = env->GetMethodID(clazz, "notifyScroll", "(IIIIDDIIIIIDD)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyInputMethod = env->GetMethodID(clazz, "notifyInputMethod", "(Ljava/lang/String;[I[I[BIII)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyMenu = env->GetMethodID(clazz, "notifyMenu", "(IIIIZ)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewPtr = env->GetFieldID(clazz, "ptr", "J");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkView");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyInputMethodDraw = env->GetMethodID(clazz, "notifyInputMethodDraw", "(Ljava/lang/String;III[B)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyInputMethodCaret = env->GetMethodID(clazz, "notifyInputMethodCaret", "(III)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jViewNotifyPreeditMode = env->GetMethodID(clazz, "notifyPreeditMode", "(Z)V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/Window");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyResize = env->GetMethodID(clazz, "notifyResize", "(III)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyMove = env->GetMethodID(clazz, "notifyMove", "(II)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyDestroy = env->GetMethodID(clazz, "notifyDestroy", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyClose = env->GetMethodID(clazz, "notifyClose", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyFocus = env->GetMethodID(clazz, "notifyFocus", "(I)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyFocusDisabled = env->GetMethodID(clazz, "notifyFocusDisabled", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyFocusUngrab = env->GetMethodID(clazz, "notifyFocusUngrab", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyMoveToAnotherScreen = env->GetMethodID(clazz, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyLevelChanged = env->GetMethodID(clazz, "notifyLevelChanged", "(I)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowIsEnabled = env->GetMethodID(clazz, "isEnabled", "()Z");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowNotifyDelegatePtr = env->GetMethodID(clazz, "notifyDelegatePtr", "(J)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jWindowPtr = env->GetFieldID(clazz, "ptr", "J");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkWindow");
    if (env->ExceptionCheck()) return JNI_ERR;
    jGtkWindowNotifyStateChanged =
            env->GetMethodID(clazz, "notifyStateChanged", "(I)V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/Clipboard");
    if (env->ExceptionCheck()) return JNI_ERR;
    jClipboardContentChanged = env->GetMethodID(clazz, "contentChanged", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/Cursor");
    if (env->ExceptionCheck()) return JNI_ERR;
    jCursorPtr = env->GetFieldID(clazz, "ptr", "J");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/Size");
    if (env->ExceptionCheck()) return JNI_ERR;
    jSizeInit = env->GetMethodID(clazz, "<init>", "(II)V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/util/Map");
    if (env->ExceptionCheck()) return JNI_ERR;
    jMapGet = env->GetMethodID(clazz, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    if (env->ExceptionCheck()) return JNI_ERR;
    jMapKeySet = env->GetMethodID(clazz, "keySet", "()Ljava/util/Set;");
    if (env->ExceptionCheck()) return JNI_ERR;
    jMapContainsKey = env->GetMethodID(clazz, "containsKey", "(Ljava/lang/Object;)Z");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/util/HashSet");
    if (env->ExceptionCheck()) return JNI_ERR;
    jHashSetCls = (jclass) env->NewGlobalRef(clazz);
    jHashSetInit = env->GetMethodID(jHashSetCls, "<init>", "()V");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/util/Set");
    if (env->ExceptionCheck()) return JNI_ERR;
    jSetAdd = env->GetMethodID(clazz, "add", "(Ljava/lang/Object;)Z");
    if (env->ExceptionCheck()) return JNI_ERR;
    jSetSize = env->GetMethodID(clazz, "size", "()I");
    if (env->ExceptionCheck()) return JNI_ERR;
    jSetToArray = env->GetMethodID(clazz, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/lang/Iterable");
    if (env->ExceptionCheck()) return JNI_ERR;
    jIterableIterator = env->GetMethodID(clazz, "iterator", "()Ljava/util/Iterator;");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("java/util/Iterator");
    if (env->ExceptionCheck()) return JNI_ERR;
    jIteratorHasNext = env->GetMethodID(clazz, "hasNext", "()Z");
    if (env->ExceptionCheck()) return JNI_ERR;
    jIteratorNext = env->GetMethodID(clazz, "next", "()Ljava/lang/Object;");
    if (env->ExceptionCheck()) return JNI_ERR;

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkApplication");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationCls = (jclass) env->NewGlobalRef(clazz);
    jApplicationDisplay = env->GetStaticFieldID(jApplicationCls, "display", "J");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationScreen = env->GetStaticFieldID(jApplicationCls, "screen", "I");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationVisualID = env->GetStaticFieldID(jApplicationCls, "visualID", "J");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationReportException = env->GetStaticMethodID(
        jApplicationCls, "reportException", "(Ljava/lang/Throwable;)V");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationGetApplication = env->GetStaticMethodID(
        jApplicationCls, "GetApplication", "()Lcom/sun/glass/ui/Application;");
    if (env->ExceptionCheck()) return JNI_ERR;
    jApplicationGetName = env->GetMethodID(jApplicationCls, "getName", "()Ljava/lang/String;");
    if (env->ExceptionCheck()) return JNI_ERR;

    return JNI_VERSION_1_6;
}

#ifdef STATIC_BUILD
}
#endif

void
glass_throw_exception(JNIEnv * env,
                      const char * exceptionClass,
                      const char * exceptionMessage) {
    jclass throwableClass = env->FindClass(exceptionClass);
    if (check_and_clear_exception(env)) return;
    env->ThrowNew(throwableClass, exceptionMessage);
    check_and_clear_exception(env);
}

int
glass_throw_oom(JNIEnv * env, const char * message) {
    glass_throw_exception(env, "java/lang/OutOfMemoryError", message);
    // must return a non-zero value, see HANDLE_MEM_ALLOC_ERROR
    return 1;
}


guint8* convert_BGRA_to_RGBA(const int* pixels, int stride, int height) {
  guint8* new_pixels = (guint8*) g_malloc(height * stride);
  int i = 0;

  for (i = 0; i < height * stride; i += 4) {
      new_pixels[i] = (guint8)(*pixels >> 16);
      new_pixels[i + 1] = (guint8)(*pixels >> 8);
      new_pixels[i + 2] = (guint8)(*pixels);
      new_pixels[i + 3] = (guint8)(*pixels >> 24);
      pixels++;
  }

  return new_pixels;
}


void dump_jstring_array(JNIEnv* env, jobjectArray arr) {
    if (arr == NULL) {
        LOG0("dump: Array is null\n")
        return;
    }
    jsize len = env->GetArrayLength(arr);
    LOG1("dump: length = %d\n", len)
    int i = 0;
    jboolean isCopy;
    for(i = 0; i < len; i++) {
        jstring jstr = (jstring) env->GetObjectArrayElement(arr, i);
        check_and_clear_exception(env);
        const char* str = env->GetStringUTFChars(jstr, &isCopy);
        LOG2("dump: s[%d]: %s\n", i, str)
    }
}

gboolean check_and_clear_exception(JNIEnv *env) {
    jthrowable t = env->ExceptionOccurred();
    if (t) {
        env->ExceptionClear();
        env->CallStaticVoidMethod(jApplicationCls, jApplicationReportException, t);
        //Clear in case our reporting upcall failed too!
        env->ExceptionClear();
        return TRUE;
    }
    return FALSE;
}

// The returned string should be freed with g_free().
gchar* get_application_name() {
    gchar* ret = NULL;

    jobject japp = mainEnv->CallStaticObjectMethod(jApplicationCls, jApplicationGetApplication);
    CHECK_JNI_EXCEPTION_RET(mainEnv, NULL);
    jstring jname = (jstring) mainEnv->CallObjectMethod(japp, jApplicationGetName);
    CHECK_JNI_EXCEPTION_RET(mainEnv, NULL);
    if (const gchar *name = mainEnv->GetStringUTFChars(jname, NULL)) {
        ret = g_strdup(name);
        mainEnv->ReleaseStringUTFChars(jname, name);
    }
    return ret;
}

gpointer glass_try_malloc_n(gsize m, gsize n,
        gboolean zer0 /* initialized to 0 if true*/) {
    if (n > 0 && m > G_MAXSIZE / n) {
        return NULL;
    }
    return (zer0)
            ? g_try_malloc0(m * n)
            : g_try_malloc(m * n);
}

/*
 * Since we support glib 2.18 we can't use g_try_malloc_n and g_try_malloc0_n
 * which was introduced in 2.24.
 * glass_try_malloc_n and glass_try_malloc0_n is replacement for those functions
 */
gpointer glass_try_malloc0_n(gsize m, gsize n) {
    return glass_try_malloc_n(m, n, TRUE);
}

gpointer glass_try_malloc_n(gsize m, gsize n) {
    return glass_try_malloc_n(m, n, FALSE);
}

gsize get_files_count(gchar **uris) {
    if (!uris) {
        return 0;
    }

    guint size = g_strv_length(uris);
    guint files_cnt = 0;

    for (guint i = 0; i < size; ++i) {
        if (g_str_has_prefix(uris[i], FILE_PREFIX)) {
            files_cnt++;
        }
    }
    return files_cnt;
}

// Note: passed uris will be freed by this function
jobject uris_to_java(JNIEnv *env, gchar **uris, gboolean files) {
    if (uris == NULL) {
        return NULL;
    }

    jobject result = NULL;

    guint size = g_strv_length(uris);
    guint files_cnt = get_files_count(uris);

    if (files) {
        if (files_cnt) {
            result = env->NewObjectArray(files_cnt, jStringCls, NULL);
            check_and_clear_exception(env);

            for (gsize i = 0; i < size; ++i) {
                if (g_str_has_prefix(uris[i], FILE_PREFIX)) {
                    gchar* path = g_filename_from_uri(uris[i], NULL, NULL);
                    jstring str = env->NewStringUTF(path);
                    check_and_clear_exception(env);
                    env->SetObjectArrayElement((jobjectArray) result, i, str);
                    check_and_clear_exception(env);
                    g_free(path);
                }
            }
        }
    } else if (size - files_cnt) {
        GString* str = g_string_new(NULL); //http://www.ietf.org/rfc/rfc2483.txt

        for (guint i = 0; i < size; ++i) {
            if (!g_str_has_prefix(uris[i], FILE_PREFIX)
                    && !g_str_has_prefix(uris[i], URI_LIST_COMMENT_PREFIX)) {
                g_string_append(str, uris[i]);
                g_string_append(str, URI_LIST_LINE_BREAK);
            }
        }

        if (str->len > 2) {
            g_string_erase(str, str->len - 2, 2);
        }

        result = env->NewStringUTF(str->str);
        check_and_clear_exception(env);

        g_string_free(str, TRUE);
    }
    g_strfreev(uris);
    return result;
}

//***************************************************************************

typedef struct _DeviceGrabContext {
    GdkWindow * window;
    gboolean grabbed;
} DeviceGrabContext;

gboolean disableGrab = FALSE;
static gboolean configure_transparent_window(GtkWidget *window);
static void configure_opaque_window(GtkWidget *window);

static void grab_mouse_device(GdkDevice *device, DeviceGrabContext *context);
static void ungrab_mouse_device(GdkDevice *device);

gint glass_gdk_visual_get_depth (GdkVisual * visual)
{
    // gdk_visual_get_depth is GTK 2.2 +
    return gdk_visual_get_depth(visual);
}

GdkScreen * glass_gdk_window_get_screen(GdkWindow * gdkWindow)
{
#ifdef GLASS_GTK3
        GdkVisual * gdkVisual = gdk_window_get_visual(gdkWindow);
        return gdk_visual_get_screen(gdkVisual);
#else
        return gdk_window_get_screen(gdkWindow);
#endif
}

gboolean
glass_gdk_mouse_devices_grab(GdkWindow *gdkWindow) {
#ifdef GLASS_GTK3_DISABLED
//this GTK 3 approach has synchronization issues covered in JDK-8176844
// As the approach is also deprecated in GTK 3.20+, revert back to using GTK 2 mechanism

        if (disableGrab) {
            return TRUE;
        }
        DeviceGrabContext context;
        GList *devices = gdk_device_manager_list_devices (
                             gdk_display_get_device_manager(
                                 gdk_display_get_default()),
                                 GDK_DEVICE_TYPE_MASTER);

        context.window = gdkWindow;
        context.grabbed = FALSE;
        g_list_foreach(devices, (GFunc) grab_mouse_device, &context);
        g_list_free(devices);

        return context.grabbed;
#else
    return glass_gdk_mouse_devices_grab_with_cursor(gdkWindow, NULL, TRUE);
#endif
}

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow *gdkWindow, GdkCursor *cursor, gboolean owner_events) {
    if (disableGrab) {
        return TRUE;
    }
    GdkGrabStatus status = gdk_pointer_grab(gdkWindow, owner_events, (GdkEventMask)
                                            (GDK_POINTER_MOTION_MASK
                                                | GDK_POINTER_MOTION_HINT_MASK
                                                | GDK_BUTTON_MOTION_MASK
                                                | GDK_BUTTON1_MOTION_MASK
                                                | GDK_BUTTON2_MOTION_MASK
                                                | GDK_BUTTON3_MOTION_MASK
                                                | GDK_BUTTON_PRESS_MASK
                                                | GDK_BUTTON_RELEASE_MASK),
                                            NULL, cursor, GDK_CURRENT_TIME);

    return (status == GDK_GRAB_SUCCESS) ? TRUE : FALSE;
}

void
glass_gdk_mouse_devices_ungrab() {
#ifdef GLASS_GTK3_DISABLED
//this GTK 3 approach has synchronization issues covered in JDK-8176844
// As the approach is also deprecated in GTK 3.20+, revert back to using GTK 2 mechanism
        GList *devices = gdk_device_manager_list_devices(
                             gdk_display_get_device_manager(
                                 gdk_display_get_default()),
                                 GDK_DEVICE_TYPE_MASTER);
        g_list_foreach(devices, (GFunc) ungrab_mouse_device, NULL);
        g_list_free(devices);
#else
        gdk_pointer_ungrab(GDK_CURRENT_TIME);
#endif
}

void
glass_gdk_master_pointer_get_position(gint *x, gint *y) {
#ifdef GLASS_GTK3
        gdk_device_get_position(gdk_device_manager_get_client_pointer(
                                    gdk_display_get_device_manager(
                                        gdk_display_get_default())), NULL, x, y);
#else
        gdk_display_get_pointer(gdk_display_get_default(), NULL, x, y, NULL);
#endif
}

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device) {
#ifdef GLASS_GTK3
        return gdk_display_device_is_grabbed(gdk_display_get_default(), device);
#else
        (void) device;
        return gdk_display_pointer_is_grabbed(gdk_display_get_default());
#endif
}

void
glass_gdk_device_ungrab(GdkDevice *device) {
#ifdef GLASS_GTK3
        gdk_device_ungrab(device, GDK_CURRENT_TIME);
#else
        (void) device;
        gdk_pointer_ungrab(GDK_CURRENT_TIME);
#endif
}

GdkWindow *
glass_gdk_device_get_window_at_position(GdkDevice *device, gint *x, gint *y) {
#ifdef GLASS_GTK3
        return gdk_device_get_window_at_position(device, x, y);
#else
        (void) device;
        return gdk_display_get_window_at_pointer(gdk_display_get_default(), x, y);
#endif
}

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                             gboolean transparent) {
        gboolean isTransparent = glass_configure_window_transparency(window, transparent);
        gtk_widget_realize(window);
}

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual) {
    glass_widget_set_visual(widget, visual);
}

static gboolean
configure_transparent_window(GtkWidget *window) {
    GdkScreen *default_screen = gdk_screen_get_default();
    GdkDisplay *default_display = gdk_display_get_default();

#ifdef GLASS_GTK3
        GdkVisual *visual = gdk_screen_get_rgba_visual(default_screen);
        if (visual
                && gdk_display_supports_composite(default_display)
                && gdk_screen_is_composited(default_screen)) {
            glass_widget_set_visual(window, visual);
            return TRUE;
        }
#else
        GdkColormap *colormap = gdk_screen_get_rgba_colormap(default_screen);
        if (colormap
                && gdk_display_supports_composite(default_display)
                && gdk_screen_is_composited(default_screen)) {
            gtk_widget_set_colormap(window, colormap);
            return TRUE;
        }
#endif

    return FALSE;
}

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h) {
    *w = gdk_window_get_width(window);
    *h = gdk_window_get_height(window);
}

void
glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y) {
#ifdef GLASS_GTK3
        gdk_device_get_position(
            gdk_device_manager_get_client_pointer(
                gdk_display_get_device_manager(display)), NULL , x, y);
#else
        gdk_display_get_pointer(display, NULL, x, y, NULL);
#endif
}


const guchar*
glass_gtk_selection_data_get_data_with_length(
        GtkSelectionData * selectionData,
        gint * length) {
    if (selectionData == NULL) {
        return NULL;
    }

    *length = gtk_selection_data_get_length(selectionData);
    return gtk_selection_data_get_data(selectionData);
}

static void
configure_opaque_window(GtkWidget *window) {
    (void) window;
/* We need to pick a visual that really is glx compatible
 * instead of using the default visual
 */
 /* see: JDK-8087516 for why this is commented out
    glass_widget_set_visual(window,
                          gdk_screen_get_system_visual(
                              gdk_screen_get_default()));
  */
}

gboolean
glass_configure_window_transparency(GtkWidget *window, gboolean transparent) {
    if (transparent) {
        if (configure_transparent_window(window)) {
            return TRUE;
        }

        fprintf(stderr,"Can't create transparent stage, because your screen doesn't"
               " support alpha channel."
               " You need to enable XComposite extension.\n");
        fflush(stderr);
    }

    configure_opaque_window(window);
    return FALSE;
}

static void
grab_mouse_device(GdkDevice *device, DeviceGrabContext *context) {
    GdkInputSource source = gdk_device_get_source(device);
    if (source == GDK_SOURCE_MOUSE) {
#ifdef GLASS_GTK3
        GdkGrabStatus status = gdk_device_grab(device,
                                               context->window,
                                               GDK_OWNERSHIP_NONE,
                                               TRUE,
                                               GDK_FILTERED_EVENTS_MASK,
                                               NULL,
                                               GDK_CURRENT_TIME);
#else
        GdkGrabStatus status = GDK_GRAB_SUCCESS;
/* FIXME reachable by 2?
        GdkGrabStatus status = gdk_device_grab(device,
                                               context->window,
                                               GDK_OWNERSHIP_NONE,
                                               TRUE,
                                               GDK_FILTERED_EVENTS_MASK,
                                               NULL,
                                               GDK_CURRENT_TIME);
                                       */
#endif
        if (status == GDK_GRAB_SUCCESS) {
            context->grabbed = TRUE;
        }
    }
}

static void
ungrab_mouse_device(GdkDevice *device) {
#ifdef GLASS_GTK3
    GdkInputSource source = gdk_device_get_source(device);
    if (source == GDK_SOURCE_MOUSE) {
        gdk_device_ungrab(device, GDK_CURRENT_TIME);
    }
#else
    (void) device;
    // not used on the GTK2 path
#endif
}

GdkPixbuf *
glass_pixbuf_from_window(GdkWindow *window,
    gint srcx, gint srcy,
    gint width, gint height)
{
    GdkPixbuf * ret = NULL;

#ifdef GLASS_GTK3
        ret = gdk_pixbuf_get_from_window (window, srcx, srcy, width, height);
#else
        ret = gdk_pixbuf_get_from_drawable (NULL,
            window,
            NULL,
            srcx, srcy,
            0, 0,
            width, height);
#endif
    return ret;
}

void
glass_window_apply_shape_mask(GdkWindow *window,
    void* data, uint width, uint height)
{
#ifdef GLASS_GTK3
    (void) window;
    (void) data;
    (void) width;
    (void) height;
#else
        GdkPixbuf* pixbuf = gdk_pixbuf_new_from_data((guchar *) data,
                GDK_COLORSPACE_RGB, TRUE, 8, width, height, width * 4, NULL, NULL);

        if (GDK_IS_PIXBUF(pixbuf)) {
            GdkBitmap* mask = NULL;
            gdk_pixbuf_render_pixmap_and_mask(pixbuf, NULL, &mask, 128);

            gdk_window_input_shape_combine_mask(window, mask, 0, 0);

            g_object_unref(pixbuf);
            if (mask) {
                g_object_unref(mask);
            }
        }
#endif
}

void
glass_window_reset_input_shape_mask(GdkWindow *window)
{
#ifdef GLASS_GTK3
        gdk_window_input_shape_combine_region(window, NULL, 0, 0);
#else
        gdk_window_input_shape_combine_mask(window, NULL, 0, 0);
#endif
}

GdkWindow *
glass_gdk_drag_context_get_dest_window (GdkDragContext * context)
{
    return ((context != NULL) ? gdk_drag_context_get_dest_window(context) : NULL);
}


void glass_gdk_x11_display_set_window_scale (GdkDisplay *display,
                          gint scale)
{
#ifdef GLASS_GTK3
    // Optional call, if it does not exist then GTK3 is not yet
    // doing automatic scaling of coordinates so we do not need
    // to override it.
    wrapped_gdk_x11_display_set_window_scale(display, scale);
#else
    (void) display;
    (void) scale;
#endif
}

//-------- Glass utility ----------------------------------------

void
glass_widget_set_visual(GtkWidget *widget, GdkVisual *visual)
{
#ifdef GLASS_GTK3
        gtk_widget_set_visual (widget, visual);
#else
        GdkColormap *colormap = gdk_colormap_new(visual, TRUE);
        gtk_widget_set_colormap (widget, colormap);
#endif
}

guint glass_settings_get_guint_opt (const gchar *schema_name,
                    const gchar *key_name,
                    int defval)
{
    GSettingsSchemaSource *default_schema_source =
            wrapped_g_settings_schema_source_get_default();
    if (default_schema_source == NULL) {
        if (gtk_verbose) {
            fprintf(stderr, "No schema source dir found!\n");
        }
        return defval;
    }
    GSettingsSchema *the_schema =
            wrapped_g_settings_schema_source_lookup(default_schema_source, schema_name, TRUE);
    if (the_schema == NULL) {
        if (gtk_verbose) {
            fprintf(stderr, "schema '%s' not found!\n", schema_name);
        }
        return defval;
    }
    if (!wrapped_g_settings_schema_has_key(the_schema, key_name)) {
        if (gtk_verbose) {
            fprintf(stderr, "key '%s' not found in schema '%s'!\n", key_name, schema_name);
        }
        return defval;
    }
    if (gtk_verbose) {
        fprintf(stderr, "found schema '%s' and key '%s'\n", schema_name, key_name);
    }

    GSettings *gset = g_settings_new(schema_name);

    wrapped_g_settings_schema_unref(the_schema);

    return g_settings_get_uint(gset, key_name);
}
