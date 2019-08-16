/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
#ifndef GLASS_GENERAL_H
#define        GLASS_GENERAL_H

#include <jni.h>

#include <stdint.h>
#include <X11/Xlib.h>
#include <gdk/gdk.h>
#include <gdk/gdkx.h>
#include <gtk/gtk.h>

#include "wrapped.h"

#if GTK_CHECK_VERSION(3, 0, 0)
#if ! GTK_CHECK_VERSION(3, 8, 0)
#error GTK development version is not the minimum 3.8
#endif
#define GLASS_GTK3
#endif

#define JLONG_TO_PTR(value) ((void*)(intptr_t)(value))
#define PTR_TO_JLONG(value) ((jlong)(intptr_t)(value))

#define FILE_PREFIX "file://"
#define URI_LIST_COMMENT_PREFIX "#"
#define URI_LIST_LINE_BREAK "\r\n"

extern JNIEnv* mainEnv; // Use only with main loop thread!!!
extern JavaVM* javaVM;

#define GLASS_GDK_KEY_CONSTANT(key) (GDK_KEY_ ## key)

#include <exception>

struct jni_exception: public std::exception {
    jni_exception(jthrowable _th): throwable(_th), message() {
            jclass jc = mainEnv->FindClass("java/lang/Throwable");
            if (mainEnv->ExceptionOccurred()) {
                mainEnv->ExceptionDescribe();
                mainEnv->ExceptionClear();
            }
            jmethodID jmid = mainEnv->GetMethodID(jc, "getMessage", "()Ljava/lang/String;");
            if (mainEnv->ExceptionOccurred()) {
                mainEnv->ExceptionDescribe();
                mainEnv->ExceptionClear();
            }
            jmessage = (jstring)mainEnv->CallObjectMethod(throwable, jmid);
            message = jmessage == NULL ? "" : mainEnv->GetStringUTFChars(jmessage, NULL);
    }
    const char *what() const throw()
    {
        return message;
    }
    ~jni_exception() throw(){
        if (jmessage && message) {
            mainEnv->ReleaseStringUTFChars(jmessage, message);
        }
    }
private:
    jthrowable throwable;
    const char *message;
    jstring jmessage;
};

#define EXCEPTION_OCCURED(env) (check_and_clear_exception(env))

#define CHECK_JNI_EXCEPTION(env) \
        if (env->ExceptionCheck()) {\
            check_and_clear_exception(env);\
            return;\
        }

#define CHECK_JNI_EXCEPTION_RET(env, ret) \
        if (env->ExceptionCheck()) {\
            check_and_clear_exception(env);\
            return ret;\
        }

#define JNI_EXCEPTION_TO_CPP(env) \
        if (env->ExceptionCheck()) {\
            check_and_clear_exception(env);\
            throw jni_exception(env->ExceptionOccurred());\
        }

#define HANDLE_MEM_ALLOC_ERROR(env, nativePtr, message) \
        ((nativePtr == NULL) && glass_throw_oom(env, message))

    gpointer glass_try_malloc0_n(gsize m, gsize n);

    gpointer glass_try_malloc_n(gsize m, gsize n);

    typedef struct {
        jobject runnable;
        int flag;
    } RunnableContext;

    extern char const * const GDK_WINDOW_DATA_CONTEXT;

    GdkCursor* get_native_cursor(int type);

    // JNI global references
    extern jclass jStringCls; // java.lang.String

    extern jclass jByteBufferCls; //java.nio.ByteBuffer
    extern jmethodID jByteBufferArray; //java.nio.ByteBuffer#array()[B
    extern jmethodID jByteBufferWrap; //java.nio.ByteBuffer#wrap([B)Ljava/nio/ByteBuffer;

    extern jclass jRunnableCls; // java.lang.Runnable
    extern jmethodID jRunnableRun; // java.lang.Runnable#run ()V

    extern jclass jArrayListCls; // java.util.ArrayList
    extern jmethodID jArrayListInit; // java.util.ArrayList#<init> ()V
    extern jmethodID jArrayListAdd; // java.util.ArrayList#add (Ljava/lang/Object;)Z
    extern jmethodID jArrayListGetIdx; //java.util.ArryList#get (I)Ljava/lang/Object;

    extern jmethodID jPixelsAttachData; // com.sun.class.ui.Pixels#attachData (J)V
    extern jclass jGtkPixelsCls; // com.sun.class.ui.gtk.GtkPixels
    extern jmethodID jGtkPixelsInit; // com.sun.class.ui.gtk.GtkPixels#<init> (IILjava/nio/ByteBuffer;)V

    extern jclass jScreenCls;   // com.sun.glass.ui.Screen
    extern jmethodID jScreenInit; // com.sun.glass.ui.Screen#<init> ()V
    extern jmethodID jScreenNotifySettingsChanged; // com.sun.glass.ui.Screen#notifySettingsChanged ()V
    extern jmethodID jScreenGetScreenForLocation; //com.sun.glass.ui.Screen#getScreenForLocation(JJ)Lcom.sun.glass.ui.Screen;
    extern jmethodID jScreenGetNativeScreen; //com.sun.glass.ui.Screen#getNativeScreen()J

    extern jmethodID jViewNotifyResize; // com.sun.glass.ui.View#notifyResize (II)V
    extern jmethodID jViewNotifyMouse; // com.sun.glass.ui.View#notifyMouse (IIIIIIIZZ)V
    extern jmethodID jViewNotifyRepaint; // com.sun.glass.ui.View#notifyRepaint (IIII)V
    extern jmethodID jViewNotifyKey; // com.sun.glass.ui.View#notifyKey (II[CI)V
    extern jmethodID jViewNotifyView; //com.sun.glass.ui.View#notifyView (I)V
    extern jmethodID jViewNotifyDragEnter; //com.sun.glass.ui.View#notifyDragEnter (IIIII)I
    extern jmethodID jViewNotifyDragOver; //com.sun.glass.ui.View#notifyDragOver (IIIII)I
    extern jmethodID jViewNotifyDragDrop; //com.sun.glass.ui.View#notifyDragDrop (IIIII)I
    extern jmethodID jViewNotifyDragLeave; //com.sun.glass.ui.View#notifyDragLeave ()V
    extern jmethodID jViewNotifyScroll; //com.sun.glass.ui.View#notifyScroll (IIIIDDIIIIIDD)V
    extern jmethodID jViewNotifyInputMethod; //com.sun.glass.ui.View#notifyInputMethod (Ljava/lang/String;[I[I[BIII)V
    extern jmethodID jViewNotifyInputMethodDraw; //com.sun.glass.ui.gtk.GtkView#notifyInputMethodDraw (Ljava/lang/String;III[B)V
    extern jmethodID jViewNotifyInputMethodCaret; //com.sun.glass.ui.gtk.GtkView#notifyInputMethodCaret (III)V
    extern jmethodID jViewNotifyPreeditMode; //com.sun.glass.ui.gtk.GtkView#notifyPreeditMode (Z)V
    extern jmethodID jViewNotifyMenu; //com.sun.glass.ui.View#notifyMenu (IIIIZ)V
    extern jfieldID  jViewPtr; //com.sun.glass.ui.View.ptr

    extern jmethodID jWindowNotifyResize; // com.sun.glass.ui.Window#notifyResize (III)V
    extern jmethodID jWindowNotifyMove; // com.sun.glass.ui.Window#notifyMove (II)V
    extern jmethodID jWindowNotifyDestroy; // com.sun.glass.ui.Window#notifyDestroy ()V
    extern jmethodID jWindowNotifyClose; // com.sun.glass.ui.Window#notifyClose ()V
    extern jmethodID jWindowNotifyFocus; // com.sun.glass.ui.Window#notifyFocus (I)V
    extern jmethodID jWindowNotifyFocusDisabled; // com.sun.glass.ui.Window#notifyFocusDisabled ()V
    extern jmethodID jWindowNotifyFocusUngrab; // com.sun.glass.ui.Window#notifyFocusUngrab ()V
    extern jmethodID jWindowNotifyMoveToAnotherScreen; // com.sun.glass.ui.Window#notifyMoveToAnotherScreen (Lcom/sun/glass/ui/Screen;)V
    extern jmethodID jWindowNotifyDelegatePtr; //com.sun.glass.ui.Window#notifyDelegatePtr (J)V
    extern jmethodID jWindowNotifyLevelChanged; //com.sun.glass.ui.Window#notifyLevelChanged (I)V

    extern jmethodID jWindowIsEnabled; // com.sun.glass.ui.Window#isEnabled ()Z
    extern jfieldID jWindowPtr; // com.sun.glass.ui.Window#ptr
    extern jfieldID jCursorPtr; // com.sun.glass.ui.Cursor#ptr

    extern jmethodID jGtkWindowNotifyStateChanged; // com.sun.glass.ui.GtkWindow#notifyStateChanged (I)V

    extern jmethodID jClipboardContentChanged; // com.sun.glass.ui.Clipboard#contentChanged ()V

    extern jmethodID jSizeInit; // com.sun.class.ui.Size#<init> ()V

    extern jmethodID jMapGet; // java.util.Map#get(Ljava/lang/Object;)Ljava/lang/Object;
    extern jmethodID jMapKeySet; // java.util.Map#keySet()Ljava/util/Set;
    extern jmethodID jMapContainsKey; // java.util.Map#containsKey(Ljava/lang/Object;)Z

    extern jclass jHashSetCls; // java.util.HashSet
    extern jmethodID jHashSetInit; // java.util.HashSet#<init> ()V

    extern jmethodID jSetAdd; //java.util.Set#add (Ljava/lang/Object;)Z
    extern jmethodID jSetSize; //java.util.Set#size ()I
    extern jmethodID jSetToArray; //java.util.Set#toArray ([Ljava/lang/Object;)[Ljava/lang/Object;

    extern jmethodID jIterableIterator; // java.lang.Iterable#iterator()Ljava/util/Iterator;
    extern jmethodID jIteratorHasNext; // java.util.Iterator#hasNext()Z;
    extern jmethodID jIteratorNext; // java.util.Iterator#next()Ljava/lang/Object;

    extern jclass jApplicationCls; //com.sun.glass.ui.gtk.GtkApplication
    extern jfieldID jApplicationDisplay; //com.sun.glass.ui.gtk.GtkApplication#display
    extern jfieldID jApplicationScreen; //com.sun.glass.ui.gtk.GtkApplication#screen
    extern jfieldID jApplicationVisualID; //com.sun.glass.ui.gtk.GtkApplication#visualID
    extern jmethodID jApplicationReportException; // reportException(Ljava/lang/Throwable;)V
    extern jmethodID jApplicationGetApplication; // GetApplication()()Lcom/sun/glass/ui/Application;
    extern jmethodID jApplicationGetName; // getName()Ljava/lang/String;

#ifdef VERBOSE
#define LOG0(msg) {printf(msg);fflush(stdout);}
#define LOG1(msg, param) {printf(msg, param);fflush(stdout);}
#define LOG2(msg, param1, param2) {printf(msg, param1, param2);fflush(stdout);}
#define LOG3(msg, param1, param2, param3) {printf(msg, param1, param2, param3);fflush(stdout);}
#define LOG4(msg, param1, param2, param3, param4) {printf(msg, param1, param2, param3, param4);fflush(stdout);}
#define LOG5(msg, param1, param2, param3, param4, param5) {printf(msg, param1, param2, param3, param4, param5);fflush(stdout);}

#define LOG_STRING_ARRAY(env, array) dump_jstring_array(env, array);

#define ERROR0(msg) {fprintf(stderr, msg);fflush(stderr);}
#define ERROR1(msg, param) {fprintf(stderr, msg, param);fflush(stderr);}
#define ERROR2(msg, param1, param2) {fprintf(stderr, msg, param1, param2);fflush(stderr);}
#define ERROR3(msg, param1, param2, param3) {fprintf(stderr, msg, param1, param2, param3);fflush(stderr);}
#define ERROR4(msg, param1, param2, param3, param4) {fprintf(stderr, msg, param1, param2, param3, param4);fflush(stderr);}
#else
#define LOG0(msg)
#define LOG1(msg, param)
#define LOG2(msg, param1, param2)
#define LOG3(msg, param1, param2, param3)
#define LOG4(msg, param1, param2, param3, param4)
#define LOG5(msg, param1, param2, param3, param4, param5)

#define LOG_STRING_ARRAY(env, array)

#define ERROR0(msg)
#define ERROR1(msg, param)
#define ERROR2(msg, param1, param2)
#define ERROR3(msg, param1, param2, param3)
#define ERROR4(msg, param1, param2, param3, param4)
#endif

#define LOG_EXCEPTION(env) check_and_clear_exception(env);

    gchar* get_application_name();
    void glass_throw_exception(JNIEnv * env,
            const char * exceptionClass,
            const char * exceptionMessage);
    int glass_throw_oom(JNIEnv * env, const char * exceptionMessage);
    void dump_jstring_array(JNIEnv*, jobjectArray);

    guint8* convert_BGRA_to_RGBA(const int* pixels, int stride, int height);

    gboolean check_and_clear_exception(JNIEnv *env);

    jboolean is_display_valid();

    gsize get_files_count(gchar **uris);

    jobject uris_to_java(JNIEnv *env, gchar **uris, gboolean files);


#ifdef __cplusplus
extern "C" {
#endif

extern jboolean gtk_verbose;

void
glass_widget_set_visual (GtkWidget *widget, GdkVisual *visual);

gint
glass_gdk_visual_get_depth (GdkVisual * visual);

GdkScreen *
glass_gdk_window_get_screen(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab(GdkWindow * gdkWindow);

gboolean
glass_gdk_mouse_devices_grab_with_cursor(GdkWindow * gdkWindow, GdkCursor *cursor, gboolean owner_events);

void
glass_gdk_mouse_devices_ungrab();

void
glass_gdk_master_pointer_grab(GdkEvent *event, GdkWindow *window, GdkCursor *cursor);

void
glass_gdk_master_pointer_ungrab(GdkEvent *event);

void
glass_gdk_master_pointer_get_position(gint *x, gint *y);

gboolean
glass_gdk_device_is_grabbed(GdkDevice *device);

void
glass_gdk_device_ungrab(GdkDevice *device);

GdkWindow *
glass_gdk_device_get_window_at_position(
               GdkDevice *device, gint *x, gint *y);

void
glass_gtk_configure_transparency_and_realize(GtkWidget *window,
                                                  gboolean transparent);

const guchar *
glass_gtk_selection_data_get_data_with_length(
        GtkSelectionData * selectionData,
        gint * length);

void
glass_gtk_window_configure_from_visual(GtkWidget *widget, GdkVisual *visual);

void
glass_gdk_window_get_size(GdkWindow *window, gint *w, gint *h);

void
glass_gdk_display_get_pointer(GdkDisplay* display, gint* x, gint *y);

void
glass_gdk_x11_display_set_window_scale(GdkDisplay *display, gint scale);

gboolean
glass_configure_window_transparency(GtkWidget *window, gboolean transparent);

GdkPixbuf *
glass_pixbuf_from_window(GdkWindow *window,
    gint srcx, gint srcy,
    gint width, gint height);

void
glass_window_apply_shape_mask(GdkWindow *window,
    void* data, uint width, uint height);

void
glass_window_reset_input_shape_mask(GdkWindow *window);

GdkWindow *
glass_gdk_drag_context_get_dest_window (GdkDragContext * context);

guint
glass_settings_get_guint_opt (const gchar *schema_name,
                    const gchar *key_name,
                    int defval);

#ifdef __cplusplus
}
#endif

#endif        /* GLASS_GENERAL_H */
