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
#include "com_sun_glass_ui_gtk_GtkSystemClipboard.h"
#include "glass_gtkcompat.h"
#include "glass_general.h"

#include <gtk/gtk.h>
#include <string.h>
#include <gdk-pixbuf/gdk-pixbuf.h>

static GdkAtom MIME_TEXT_PLAIN_TARGET;

static GdkAtom MIME_TEXT_URI_LIST_TARGET;

static GdkAtom MIME_JAVA_IMAGE;

static GdkAtom FILES_TARGET;
static GdkAtom MIME_FILES_TARGET;

static void init_atoms()
{
    static int initialized = 0;

    if (!initialized) {
        MIME_TEXT_PLAIN_TARGET = gdk_atom_intern_static_string("text/plain");

        MIME_TEXT_URI_LIST_TARGET = gdk_atom_intern_static_string("text/uri-list");

        MIME_JAVA_IMAGE = gdk_atom_intern_static_string("application/x-java-rawimage");

        FILES_TARGET = gdk_atom_intern_static_string("FILE_NAME");
        MIME_FILES_TARGET = gdk_atom_intern_static_string("application/x-java-file-list");
        initialized = 1;
    }
}


static GtkClipboard* clipboard = NULL;

static GtkClipboard *get_clipboard() {
    if (clipboard == NULL) {
        clipboard = gtk_clipboard_get(GDK_SELECTION_CLIPBOARD);
    }
    return clipboard;
}

static void add_target_from_jstring(JNIEnv *env, GtkTargetList *list, jstring string)
{
    const char *gstring = env->GetStringUTFChars(string, NULL);
    if (g_strcmp0(gstring, "text/plain") == 0) {
        gtk_target_list_add_text_targets(list, 0);
    } else if (g_strcmp0(gstring, "application/x-java-rawimage") == 0) {
        gtk_target_list_add_image_targets(list, 0, TRUE);
        return;
    } else if (g_strcmp0(gstring, "application/x-java-file-list") == 0) {
        gtk_target_list_add(list, MIME_TEXT_URI_LIST_TARGET, 0, 0);
        return; // do not add application/x-java-file-list
    }

    gtk_target_list_add(list, gdk_atom_intern(gstring, FALSE), 0, 0);
    env->ReleaseStringUTFChars(string, gstring);

}

static void data_to_targets(JNIEnv *env, jobject data, GtkTargetEntry **targets, gint *ntargets)
{
    jobject keys;
    jobject keysIterator;
    jstring next;

    GtkTargetList *list = gtk_target_list_new(NULL, 0);

    keys = env->CallObjectMethod(data, jMapKeySet, NULL);
    CHECK_JNI_EXCEPTION(env)
    keysIterator = env->CallObjectMethod(keys, jIterableIterator, NULL);
    CHECK_JNI_EXCEPTION(env)

    while (env->CallBooleanMethod(keysIterator, jIteratorHasNext) == JNI_TRUE) {
        next = (jstring) env->CallObjectMethod(keysIterator, jIteratorNext, NULL);
        add_target_from_jstring(env, list, next);
    }
    *targets = gtk_target_table_new_from_list(list, ntargets);
    gtk_target_list_unref(list);

}

static void set_text_data(GtkSelectionData *selection_data, jstring data)
{
    const char *text_data = mainEnv->GetStringUTFChars(data, NULL);
    guint ntext_data = strlen(text_data);

    gtk_selection_data_set_text(selection_data, text_data, ntext_data);
    mainEnv->ReleaseStringUTFChars(data, text_data);
}

static void set_jstring_data(GtkSelectionData *selection_data, GdkAtom target, jstring data)
{
    const char *text_data = mainEnv->GetStringUTFChars(data, NULL);
    guint ntext_data = strlen(text_data);

    //XXX is target == type ??
    gtk_selection_data_set(selection_data, target, 8, (const guchar *)text_data, ntext_data);
    mainEnv->ReleaseStringUTFChars(data, text_data);
}

static void set_bytebuffer_data(GtkSelectionData *selection_data, GdkAtom target, jobject data)
{
    jbyteArray byteArray = (jbyteArray) mainEnv->CallObjectMethod(data, jByteBufferArray);
    CHECK_JNI_EXCEPTION(mainEnv)
    jbyte* raw = mainEnv->GetByteArrayElements(byteArray, NULL);
    jsize nraw = mainEnv->GetArrayLength(byteArray);

    //XXX is target == type ??
    gtk_selection_data_set(selection_data, target, 8, (guchar *)raw, (gint)nraw);

    mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
}

#define FILE_PREFIX "file://"
#define FILE_PREFIX_N 7

static void set_file_uri_data(GtkSelectionData *selection_data, jobjectArray data)
{
    jsize ndata = mainEnv->GetArrayLength(data);
    jsize i;
    jsize string_size;
    jstring string;

    gchar **uris = (gchar**) glass_try_malloc0_n(ndata + 1, sizeof(gchar*));
    if (!uris) {
        glass_throw_oom(mainEnv, "Failed to allocate uri data");
        return;
    }
    
    for (i = 0; i < ndata; ++i) {
        string = (jstring) mainEnv->GetObjectArrayElement(data, i);
        string_size = mainEnv->GetStringUTFLength(string);

        uris[i] = (gchar*) g_try_malloc0(string_size + FILE_PREFIX_N + 1);
        if (!uris[i]) {
            glass_throw_oom(mainEnv, "Failed to allocate uri data");
            for (int k = 0; k < i; k++) {
                g_free(uris[k]);
            }
            g_free(uris);
            return;
        }

        g_strlcpy(uris[i], FILE_PREFIX, FILE_PREFIX_N + 1);
        mainEnv->GetStringUTFRegion(string, 0, string_size, (char*) uris[i] + FILE_PREFIX_N);
    }

    gtk_selection_data_set_uris(selection_data, uris);

    for (i = 0; i < ndata; ++i) {
        g_free(uris[i]);
    }
    g_free(uris);

}

static void set_image_data(GtkSelectionData *selection_data, jobject pixels)
{
    GdkPixbuf *pixbuf = NULL;

    mainEnv->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));
    if (!EXCEPTION_OCCURED(mainEnv)) {
        gtk_selection_data_set_pixbuf(selection_data, pixbuf);
    }

    g_object_unref(pixbuf);
}

static void set_data(GdkAtom target, GtkSelectionData *selection_data, jobject data)
{
    gchar *name = gdk_atom_name(target);
    jstring typeString;
    jobject result;

    if (gtk_targets_include_text(&target, 1)) {
        typeString = mainEnv->NewStringUTF("text/plain");
        result = mainEnv->CallObjectMethod(data, jMapGet, typeString, NULL);
        if (!EXCEPTION_OCCURED(mainEnv) && result != NULL) {
            set_text_data(selection_data, (jstring)result);
        }
    } else if (gtk_targets_include_image(&target, 1, TRUE)) {
        typeString = mainEnv->NewStringUTF("application/x-java-rawimage");
        result = mainEnv->CallObjectMethod(data, jMapGet, typeString, NULL);
        if (!EXCEPTION_OCCURED(mainEnv) && result != NULL) {
            set_image_data(selection_data, result);
        }
    } else if (target == MIME_TEXT_URI_LIST_TARGET) {
        typeString = mainEnv->NewStringUTF(name);
        if (mainEnv->CallBooleanMethod(data, jMapContainsKey, typeString, NULL)) {
            result = mainEnv->CallObjectMethod(data, jMapGet, typeString, NULL);
            if (!EXCEPTION_OCCURED(mainEnv) && result != NULL) {
                set_jstring_data(selection_data, target, (jstring)result);
            }
        } else {
            typeString = mainEnv->NewStringUTF("application/x-java-file-list");
            result = mainEnv->CallObjectMethod(data, jMapGet, typeString, NULL);
            if (!EXCEPTION_OCCURED(mainEnv) && result != NULL) {
                set_file_uri_data(selection_data, (jobjectArray) result);
            }
        }
    } else {
        typeString = mainEnv->NewStringUTF(name);
        result = mainEnv->CallObjectMethod(data, jMapGet, typeString, NULL);
        if (!EXCEPTION_OCCURED(mainEnv) && result != NULL) {
            if (mainEnv->IsInstanceOf(result, jStringCls)) {
                set_jstring_data(selection_data, target, (jstring)result);
            } else if (mainEnv->IsInstanceOf(result, jByteBufferCls)) {
                set_bytebuffer_data(selection_data, target, result);
            }
        }
    }

    g_free(name);
}

static void set_data_func(GtkClipboard *clipboard, GtkSelectionData *selection_data,
        guint info, gpointer user_data)
{
    jobject data = (jobject) user_data; //HashMap
    GdkAtom target;
    target = gtk_selection_data_get_target(selection_data);

    set_data(target, selection_data, data);
    CHECK_JNI_EXCEPTION(mainEnv);
}

static void clear_data_func(GtkClipboard *clipboard, gpointer user_data)
{
    jobject data =(jobject) user_data;
    mainEnv->DeleteGlobalRef(data);
}

static jobject get_data_text(JNIEnv *env)
{
    gchar *data = gtk_clipboard_wait_for_text(get_clipboard());
    if (data == NULL) {
        return NULL;
    }
    jstring jdata = env->NewStringUTF(data);
    g_free(data);
    return jdata;
}

static jobject get_data_file_list(JNIEnv *env)
{
    jobjectArray result = NULL;
    guint i,size;
    jstring str;
    gchar *path;
    gchar **uris;

    uris = gtk_clipboard_wait_for_uris(get_clipboard());
    if (uris == NULL) {
        return NULL;
    }
    size = g_strv_length(uris);
    result = env->NewObjectArray(size, jStringCls, NULL);

    for (i = 0; i < size; ++i) {
        path = uris[i];
        if (g_str_has_prefix(path, "file://")) {
            path += 7;
        }
        str = env->NewStringUTF(path);
        env->SetObjectArrayElement(result, i, str);
    }

    g_strfreev(uris);
    return result;
}

static jobject get_data_image(JNIEnv* env) {
    GdkPixbuf* pixbuf;
    guchar *data;
    jbyteArray data_array;
    jobject buffer, result;
    int w,h,stride;

    pixbuf = gtk_clipboard_wait_for_image(get_clipboard());
    if (pixbuf == NULL) {
        return NULL;
    }

    if (!gdk_pixbuf_get_has_alpha(pixbuf)) {
        GdkPixbuf *tmp_buf = gdk_pixbuf_add_alpha(pixbuf, FALSE, 0, 0, 0);
        g_object_unref(pixbuf);
        pixbuf = tmp_buf;
    }
    w = gdk_pixbuf_get_width(pixbuf);
    h = gdk_pixbuf_get_height(pixbuf);
    stride = gdk_pixbuf_get_rowstride(pixbuf);

    data = gdk_pixbuf_get_pixels(pixbuf);

    //Actually, we are converting RGBA to BGRA, but that's the same operation
    data = (guchar*) convert_BGRA_to_RGBA((int*)data, stride, h);

    data_array = env->NewByteArray(stride*h);
    env->SetByteArrayRegion(data_array, 0, stride*h, (jbyte*)data);

    buffer = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, data_array);
    result = env->NewObject(jGtkPixelsCls, jGtkPixelsInit, w, h, buffer);

    g_free(data);
    g_object_unref(pixbuf);

    return result;

}

static jobject get_data_raw(JNIEnv *env, const char* mime, gboolean string_data)
{
    GtkSelectionData *data;
    const guchar *raw_data;
    jsize length;
    jbyteArray array;
    jobject result = NULL;
    data = gtk_clipboard_wait_for_contents(get_clipboard(), gdk_atom_intern(mime, FALSE));
    if (data != NULL) {
        raw_data = glass_gtk_selection_data_get_data_with_length(data, &length);
        if (string_data) {
            result = env->NewStringUTF((const char*)raw_data);
        } else {
            array = env->NewByteArray(length);
            env->SetByteArrayRegion(array, 0, length, (const jbyte*)raw_data);
            result = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, array);
        }
        gtk_selection_data_free(data);
    }
    return result;
}

static jobject jclipboard = NULL;
static gulong owner_change_handler_id = 0;

static void clipboard_owner_changed_callback(GtkClipboard *clipboard, GdkEventOwnerChange *event, jobject obj)
{
    mainEnv->CallVoidMethod(obj, jClipboardContentChanged);
    CHECK_JNI_EXCEPTION(mainEnv)
}

extern "C" {

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_init
  (JNIEnv *env, jobject obj)
{
    if (jclipboard) {
        ERROR0("GtkSystemClipboard already initiated");
    }
    
    jclipboard = env->NewGlobalRef(obj);
    owner_change_handler_id = g_signal_connect(G_OBJECT(get_clipboard()),
            "owner-change", G_CALLBACK(clipboard_owner_changed_callback), jclipboard);
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_dispose
  (JNIEnv *env, jobject obj)
{
    g_signal_handler_disconnect(G_OBJECT(get_clipboard()), owner_change_handler_id);
    env->DeleteGlobalRef(jclipboard);
    
    owner_change_handler_id = 0;
    jclipboard = NULL;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    isOwner
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_isOwner
  (JNIEnv *env, jobject obj)
{
    return JNI_FALSE; //There's no straightforward way how to check for this in GTK.
                      // Returning false is safe however, as it just affects the case when the same application,
                      // that has clipboard selection, checks for clipboard content. When this is false,
                      // we have to use native X11 calls in this case.
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    pushToSystem
 * Signature: (Ljava/util/HashMap;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_pushToSystem
  (JNIEnv * env, jobject obj, jobject data, jint supported)
{
    GtkTargetEntry* targets = NULL;
    gint ntargets;
    data = env->NewGlobalRef(data);
    init_atoms();
    data_to_targets(env, data, &targets, &ntargets);
    CHECK_JNI_EXCEPTION(env)
    if (targets) {
        gtk_clipboard_set_with_data(get_clipboard(), targets, ntargets, set_data_func, clear_data_func, data);
        gtk_target_table_free(targets, ntargets);
    } else { 
        // targets == NULL means that we want to clear clipboard.
        // Passing NULL as targets parameter to gtk_clipboard_set_with_data will produce Gtk-CRITICAL assertion
        // but passing 0 as n_targets parameter allows to set empty list of available mime types
        GtkTargetEntry dummy_targets = {(gchar*) "MIME_DUMMY_TARGET", 0, 0};
        gtk_clipboard_set_with_data(get_clipboard(), &dummy_targets, 0, set_data_func, clear_data_func, data);
    }
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    pushTargetActionToSystem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_pushTargetActionToSystem
  (JNIEnv * env, jobject obj, jint action)
{
    //Not used for clipboard. DnD only
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    popFromSystem
 * Signature: (Ljava/lang/String;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_popFromSystem
  (JNIEnv * env, jobject obj, jstring mime)
{
    const char* cmime = env->GetStringUTFChars(mime, NULL);
    jobject result;

    init_atoms();
    if (g_strcmp0(cmime, "text/plain") == 0) {
        result = get_data_text(env);
    } else if (g_str_has_prefix(cmime, "text/")) {
        result = get_data_raw(env, cmime, TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-file-list") == 0) {
        result = get_data_file_list(env);
    } else if (g_strcmp0(cmime, "application/x-java-rawimage") == 0 ) {
        result = get_data_image(env);
    } else {
        result = get_data_raw(env, cmime, FALSE);
    }
    LOG_EXCEPTION(env)
    env->ReleaseStringUTFChars(mime, cmime);

    return result;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    supportedSourceActionsFromSystem
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_supportedSourceActionsFromSystem
  (JNIEnv *env, jobject obj)
{
    //Not used for clipboard. DnD only
    return 0;
}

/*
 * Class:     com_sun_glass_ui_gtk_GtkSystemClipboard
 * Method:    mimesFromSystem
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_gtk_GtkSystemClipboard_mimesFromSystem
  (JNIEnv * env, jobject obj)
{
    GdkAtom *targets;
    gint ntargets;
    gint i;
    GdkAtom *convertible;
    GdkAtom *convertible_ptr;
    gchar *name;
    jobjectArray result;
    jstring tmpString;

    init_atoms();

    gtk_clipboard_wait_for_targets(get_clipboard(), &targets, &ntargets);

    convertible = (GdkAtom*) glass_try_malloc_n(ntargets * 2, sizeof(GdkAtom)); //theoretically, the number can double
    if (!convertible) {
        if (ntargets > 0) {
            glass_throw_oom(env, "Failed to allocate mimes");
        }
        g_free(targets);
        return NULL;
    }

    convertible_ptr = convertible;
    for (i = 0; i < ntargets; ++i) {
        //handle text targets
        //if (targets[i] == TEXT_TARGET || targets[i] == STRING_TARGET || targets[i] == UTF8_STRING_TARGET) {
        if (gtk_targets_include_text(targets + i, 1)) {
            *(convertible_ptr++) = MIME_TEXT_PLAIN_TARGET;
        } else if (gtk_targets_include_image(targets + i, 1, TRUE)) {
            *(convertible_ptr++) = MIME_JAVA_IMAGE;
        }
        //TODO text/x-moz-url ?

        //XXX: URI targets are not necessary file targets, can we add the type anyway?
        if (targets[i] == MIME_TEXT_URI_LIST_TARGET) {
            *(convertible_ptr++) = MIME_FILES_TARGET;
        }

        name = gdk_atom_name(targets[i]);
        *(convertible_ptr++) = targets[i];
        g_free(name);
    }

    result = env->NewObjectArray(convertible_ptr - convertible, jStringCls, NULL);
    for (i = 0; convertible + i < convertible_ptr; ++i) {
        name = gdk_atom_name(convertible[i]);
        tmpString = env->NewStringUTF(name);
        env->SetObjectArrayElement(result, (jsize)i, tmpString);
        g_free(name);
    }

    g_free(targets);
    g_free(convertible);
    return result;
}

} // extern "C" {
