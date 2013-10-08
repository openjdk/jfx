/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void *reserved)
{
    JNIEnv *env;
    jclass clazz;

    if (jvm->GetEnv((void **)&env, JNI_VERSION_1_6)) {
         return JNI_ERR; /* JNI version not supported */
     }

    jStringCls = (jclass) env->NewGlobalRef(env->FindClass("java/lang/String"));
    jByteBufferCls = (jclass) env->NewGlobalRef(env->FindClass("java/nio/ByteBuffer"));
    jByteBufferArray = env->GetMethodID(jByteBufferCls, "array", "()[B");
    jByteBufferWrap = env->GetStaticMethodID(jByteBufferCls, "wrap", "([B)Ljava/nio/ByteBuffer;");

    clazz = env->FindClass("java/lang/Runnable");
    jRunnableRun = env->GetMethodID(clazz, "run", "()V");

    jArrayListCls = (jclass) env->NewGlobalRef(env->FindClass("java/util/ArrayList"));
    jArrayListInit = env->GetMethodID(jArrayListCls, "<init>", "()V");
    jArrayListAdd = env->GetMethodID(jArrayListCls, "add", "(Ljava/lang/Object;)Z");
    jArrayListGetIdx = env->GetMethodID(jArrayListCls, "get", "(I)Ljava/lang/Object;");

    clazz = env->FindClass("com/sun/glass/ui/Pixels");
    jPixelsAttachData = env->GetMethodID(clazz, "attachData", "(J)V");

    jGtkPixelsCls = (jclass) env->NewGlobalRef(env->FindClass("com/sun/glass/ui/gtk/GtkPixels"));
    jGtkPixelsInit = env->GetMethodID(jGtkPixelsCls, "<init>", "(IILjava/nio/ByteBuffer;)V");

    jScreenCls = (jclass) env->NewGlobalRef(env->FindClass("com/sun/glass/ui/Screen"));
    jScreenInit = env->GetMethodID(jScreenCls, "<init>", "(JIIIIIIIIIIIF)V");
    jScreenNotifySettingsChanged = env->GetStaticMethodID(jScreenCls, "notifySettingsChanged", "()V");

    clazz = env->FindClass("com/sun/glass/ui/View");
    jViewNotifyResize = env->GetMethodID(clazz, "notifyResize", "(II)V");
    jViewNotifyMouse = env->GetMethodID(clazz, "notifyMouse", "(IIIIIIIZZ)V");
    jViewNotifyRepaint = env->GetMethodID(clazz, "notifyRepaint", "(IIII)V");
    jViewNotifyKey = env->GetMethodID(clazz, "notifyKey", "(II[CI)V");
    jViewNotifyView = env->GetMethodID(clazz, "notifyView", "(I)V");
    jViewNotifyDragEnter = env->GetMethodID(clazz, "notifyDragEnter", "(IIIII)I");
    jViewNotifyDragOver = env->GetMethodID(clazz, "notifyDragOver", "(IIIII)I");
    jViewNotifyDragDrop = env->GetMethodID(clazz, "notifyDragDrop", "(IIIII)I");
    jViewNotifyDragLeave = env->GetMethodID(clazz, "notifyDragLeave", "()V");
    jViewNotifyScroll = env->GetMethodID(clazz, "notifyScroll", "(IIIIDDIIIIIDD)V");
    jViewNotifyInputMethod = env->GetMethodID(clazz, "notifyInputMethod", "(Ljava/lang/String;[I[I[BIII)V");
    jViewNotifyMenu = env->GetMethodID(clazz, "notifyMenu", "(IIIIZ)V");
    jViewPtr = env->GetFieldID(clazz, "ptr", "J");

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkView");
    jViewNotifyInputMethodDraw = env->GetMethodID(clazz, "notifyInputMethodDraw", "(Ljava/lang/String;III)V");
    jViewNotifyInputMethodCaret = env->GetMethodID(clazz, "notifyInputMethodCaret", "(III)V");

    clazz = env->FindClass("com/sun/glass/ui/Window");
    jWindowNotifyResize = env->GetMethodID(clazz, "notifyResize", "(III)V");
    jWindowNotifyMove = env->GetMethodID(clazz, "notifyMove", "(II)V");
    jWindowNotifyDestroy = env->GetMethodID(clazz, "notifyDestroy", "()V");
    jWindowNotifyClose = env->GetMethodID(clazz, "notifyClose", "()V");
    jWindowNotifyFocus = env->GetMethodID(clazz, "notifyFocus", "(I)V");
    jWindowNotifyFocusDisabled = env->GetMethodID(clazz, "notifyFocusDisabled", "()V");
    jWindowNotifyFocusUngrab = env->GetMethodID(clazz, "notifyFocusUngrab", "()V");
    jWindowNotifyMoveToAnotherScreen = env->GetMethodID(clazz, "notifyMoveToAnotherScreen", "(Lcom/sun/glass/ui/Screen;)V");
    jWindowIsEnabled = env->GetMethodID(clazz, "isEnabled", "()Z");
    jWindowNotifyDelegatePtr = env->GetMethodID(clazz, "notifyDelegatePtr", "(J)V");
    jWindowPtr = env->GetFieldID(clazz, "ptr", "J");

    clazz = env->FindClass("com/sun/glass/ui/gtk/GtkWindow");
    jGtkWindowNotifyStateChanged =
            env->GetMethodID(clazz, "notifyStateChanged", "(I)V");

    clazz = env->FindClass("com/sun/glass/ui/Clipboard");
    jClipboardContentChanged = env->GetMethodID(clazz, "contentChanged", "()V");

    clazz = env->FindClass("com/sun/glass/ui/Cursor");
    jCursorPtr = env->GetFieldID(clazz, "ptr", "J");

    clazz = env->FindClass("com/sun/glass/ui/Size");
    jSizeInit = env->GetMethodID(clazz, "<init>", "(II)V");

    clazz = env->FindClass("java/util/Map");
    jMapGet = env->GetMethodID(clazz, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
    jMapKeySet = env->GetMethodID(clazz, "keySet", "()Ljava/util/Set;");
    jMapContainsKey = env->GetMethodID(clazz, "containsKey", "(Ljava/lang/Object;)Z");

    jHashSetCls = (jclass) env->NewGlobalRef(env->FindClass("java/util/HashSet"));
    jHashSetInit = env->GetMethodID(jHashSetCls, "<init>", "()V");

    clazz = env->FindClass("java/util/Set");
    jSetAdd = env->GetMethodID(clazz, "add", "(Ljava/lang/Object;)Z");
    jSetSize = env->GetMethodID(clazz, "size", "()I");
    jSetToArray = env->GetMethodID(clazz, "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");

    clazz = env->FindClass("java/lang/Iterable");
    jIterableIterator = env->GetMethodID(clazz, "iterator", "()Ljava/util/Iterator;");

    clazz = env->FindClass("java/util/Iterator");
    jIteratorHasNext = env->GetMethodID(clazz, "hasNext", "()Z");
    jIteratorNext = env->GetMethodID(clazz, "next", "()Ljava/lang/Object;");

    jApplicationCls = (jclass) env->NewGlobalRef(env->FindClass("com/sun/glass/ui/gtk/GtkApplication"));
    jApplicationDisplay = env->GetStaticFieldID(jApplicationCls, "display", "J");
    jApplicationScreen = env->GetStaticFieldID(jApplicationCls, "screen", "I");
    jApplicationVisualID = env->GetStaticFieldID(jApplicationCls, "visualID", "J");
    jApplicationReportException = env->GetStaticMethodID(
        jApplicationCls, "reportException", "(Ljava/lang/Throwable;)V");

    g_thread_init(NULL);
    gdk_threads_init();
    gdk_threads_enter();
    gtk_init(NULL, NULL);

    return JNI_VERSION_1_6;
}

void
glass_throw_exception(JNIEnv * env,
                      const char * exceptionClass,
                      const char * exceptionMessage) {
    jclass throwableClass = env->FindClass(exceptionClass);
    env->ThrowNew(throwableClass, exceptionMessage);
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
        const char* str = env->GetStringUTFChars(jstr, &isCopy);
        LOG2("dump: s[%d]: %s\n", i, str)
    }
}

gboolean check_and_clear_exception(JNIEnv *env) {
    jthrowable t = env->ExceptionOccurred();
    if (t) {
        env->ExceptionClear();
        env->CallStaticVoidMethod(jApplicationCls, jApplicationReportException, t);
        return TRUE;
    }
    return FALSE;
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

            for (gsize i = 0; i < size; ++i) {
                if (g_str_has_prefix(uris[i], FILE_PREFIX)) {
                    gchar* path = g_filename_from_uri(uris[i], NULL, NULL);
                    jstring str = env->NewStringUTF(path);
                    env->SetObjectArrayElement((jobjectArray) result, i, str);
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

        g_string_free(str, TRUE);
    }
    g_strfreev(uris);
    return result;
}
