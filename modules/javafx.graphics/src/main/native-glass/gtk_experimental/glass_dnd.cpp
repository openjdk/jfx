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

#include "glass_dnd.h"
#include "glass_general.h"
#include "glass_evloop.h"

#include "com_sun_glass_events_DndEvent.h"
#include "com_sun_glass_ui_gtk_GtkDnDClipboard.h"

#include <jni.h>
#include <cstring>

#include <gtk/gtk.h>
#include <gdk/gdkx.h>
#include <gdk/gdkkeysyms.h>

/************************* COMMON *********************************************/
static jint translate_gdk_action_to_glass(GdkDragAction action) {
    jint result = 0;
    result |= (action & GDK_ACTION_COPY) ? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_COPY : 0;
    result |= (action & GDK_ACTION_MOVE) ? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_MOVE : 0;
    result |= (action & GDK_ACTION_LINK) ? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_REFERENCE : 0;
    return result;
}

static GdkDragAction translate_glass_action_to_gdk(jint action) {
    int result = 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_COPY) ? GDK_ACTION_COPY : 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_MOVE) ? GDK_ACTION_MOVE : 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_REFERENCE) ? GDK_ACTION_LINK : 0;
    return static_cast<GdkDragAction>(result);
}

static void clear_global_ref(gpointer data) {
    mainEnv->DeleteGlobalRef((jobject) data);
}

static void dnd_set_performed_action(jint performed_action);

static jint dnd_get_performed_action();

enum {
    TARGET_TEXT,
    TARGET_IMAGE,
    TARGET_URI,
    TARGET_RAW
};

/************************* TARGET *********************************************/

static struct {
    GdkDragContext *ctx;
    GtkSelectionData *data;
    gboolean just_entered;
    jobjectArray mimes;
} target_ctx = {NULL, NULL, FALSE, NULL};

gboolean is_dnd_owner = FALSE;
GtkWidget *drag_widget = NULL;

gboolean is_in_drag() {
    return drag_widget != NULL;
}

static void reset_target_ctx() {
    if (target_ctx.mimes != NULL) {
        mainEnv->DeleteGlobalRef(target_ctx.mimes);
    }

    memset(&target_ctx, 0, sizeof(target_ctx));
}

static gboolean dnd_drag_motion_callback(GtkWidget *widget,
                                         GdkDragContext *context,
                                         gint x,
                                         gint y,
                                         guint time,
                                         gpointer user_data) {

    WindowContext *ctx = (WindowContext *) user_data;

    if (target_ctx.ctx == NULL || (target_ctx.ctx != context && !target_ctx.just_entered)) {
        reset_target_ctx();
        is_dnd_owner = is_in_drag();
        target_ctx.ctx = context;
        target_ctx.just_entered = TRUE;
    }

    gint x_abs, y_abs;
    gdk_window_get_origin(gdk_drag_context_get_dest_window(context), &x_abs, &y_abs);

    jmethodID method = target_ctx.just_entered ? jViewNotifyDragEnter : jViewNotifyDragOver;

    GdkDragAction suggested = gdk_drag_context_get_suggested_action(context);
    GdkDragAction result = translate_glass_action_to_gdk(mainEnv->CallIntMethod(ctx->get_jview(), method,
                                                                                (jint) x, (jint) y,
                                                                                (jint) x_abs, (jint) y_abs,
                                                                                translate_gdk_action_to_glass(
                                                                                        suggested)));
    CHECK_JNI_EXCEPTION_RET(mainEnv, FALSE)

    if (target_ctx.just_entered) {
        target_ctx.just_entered = FALSE;
    }

    gdk_drag_status(context, result, GDK_CURRENT_TIME);

    return (gboolean) result;
}

static gboolean dnd_drag_drop_callback(GtkWidget *widget,
                                       GdkDragContext *context,
                                       gint x,
                                       gint y,
                                       guint time,
                                       gpointer user_data) {
    if (target_ctx.ctx == NULL || target_ctx.just_entered) {
        return FALSE; // Do not process drop events if no enter event and subsequent motion event were received
    }

    GdkAtom target = gtk_drag_dest_find_target(widget, context, NULL);

    if (target == GDK_NONE) {
        // used for RAW
        target = gdk_atom_intern_static_string("");
    }

    gtk_drag_get_data(widget, context, target, GDK_CURRENT_TIME);

    return TRUE;
}

static void dnd_on_drag_data_received_callback(GtkWidget *widget,
                                               GdkDragContext *context,
                                               gint x,
                                               gint y,
                                               GtkSelectionData *data,
                                               guint info,
                                               guint time,
                                               gpointer user_data) {
    WindowContext *ctx = (WindowContext *) user_data;

    if (gtk_selection_data_get_length(data) == 0) {
        gtk_drag_finish(context, FALSE, FALSE, GDK_CURRENT_TIME);
        reset_target_ctx();
        return;
    }

    gint x_abs, y_abs;
    gdk_window_get_origin(gdk_drag_context_get_dest_window(context), &x_abs, &y_abs);
    GdkDragAction selected = gdk_drag_context_get_selected_action(context);
    target_ctx.data = data;

    // Delay the notify for when we have the data
    mainEnv->CallIntMethod(ctx->get_jview(), jViewNotifyDragDrop,
                           (jint) x, (jint) y,
                           (jint) x_abs, (jint) y_abs,
                           translate_gdk_action_to_glass(selected));
    LOG_EXCEPTION(mainEnv)

    gtk_drag_finish(context, selected, selected == GDK_ACTION_MOVE, GDK_CURRENT_TIME);
}

void dnd_drag_leave_callback(WindowContext *ctx) {
    mainEnv->CallVoidMethod(ctx->get_jview(), jViewNotifyDragLeave, NULL);
    CHECK_JNI_EXCEPTION(mainEnv)

    reset_target_ctx();
}

void glass_dnd_attach_context(WindowContext *ctx) {
    gtk_drag_dest_set(ctx->get_gtk_widget(), (GtkDestDefaults) 0, NULL, 0,
                      (GdkDragAction)(GDK_ACTION_COPY | GDK_ACTION_MOVE | GDK_ACTION_LINK));

    GtkTargetList *target_list = gtk_target_list_new(NULL, 0);
    gtk_target_list_add_image_targets(target_list, TARGET_IMAGE, TRUE);
    gtk_target_list_add_uri_targets(target_list, TARGET_URI);
    gtk_target_list_add_text_targets(target_list, TARGET_TEXT);
    gtk_target_list_add(target_list, gdk_atom_intern_static_string(""), 0, TARGET_RAW);

    gtk_drag_dest_set_target_list(ctx->get_gtk_widget(), target_list);

    g_signal_connect(ctx->get_gtk_widget(), "drag-motion", G_CALLBACK(dnd_drag_motion_callback), ctx);
    g_signal_connect(ctx->get_gtk_widget(), "drag-drop", G_CALLBACK(dnd_drag_drop_callback), ctx);
    g_signal_connect(ctx->get_gtk_widget(), "drag-data-received", G_CALLBACK(dnd_on_drag_data_received_callback), ctx);
}

static gboolean check_state_in_drag(JNIEnv *env) {
    if (!target_ctx.ctx) {
        jclass jc = env->FindClass("java/lang/IllegalStateException");
        if (!env->ExceptionCheck()) {
            env->ThrowNew(jc,
                          "Cannot get supported actions. Drag pointer haven't entered the application window");
        }
        return TRUE;
    }
    return FALSE;
}

static GdkAtom *get_target_ctx_target_atoms(gint *size) {
    GList *targets = gdk_drag_context_list_targets(target_ctx.ctx);
    gint s = (gint) g_list_length(targets);
    GdkAtom *atoms = (GdkAtom *) g_try_malloc0(sizeof(GdkAtom) * s);

    int i = 0;
    for (; targets != NULL; targets = targets->next) {
        atoms[i++] = (GdkAtom) targets->data;
    }

    *size = s;

    g_list_free(targets);
    return atoms;
}

jobjectArray dnd_target_get_mimes(JNIEnv *env) {
    if (check_state_in_drag(env)) {
        return NULL;
    }

    if (!target_ctx.mimes) {
        jobject set = env->NewObject(jHashSetCls, jHashSetInit, NULL);
        EXCEPTION_OCCURED(env);

        gboolean was_set = FALSE;
        gint size;
        GdkAtom *targets = get_target_ctx_target_atoms(&size);

        if (gtk_targets_include_image(targets, size, TRUE)) {
            jstring jStr = env->NewStringUTF("application/x-java-rawimage");
            EXCEPTION_OCCURED(env);
            env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
            EXCEPTION_OCCURED(env);
            was_set = TRUE;
        }
        if (gtk_targets_include_uri(targets, size)) {
            // it's a possibility
            jstring jStr = env->NewStringUTF("application/x-java-file-list");
            EXCEPTION_OCCURED(env);
            env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
            EXCEPTION_OCCURED(env);

            jstring jStr2 = env->NewStringUTF("text/uri-list");
            EXCEPTION_OCCURED(env);
            env->CallBooleanMethod(set, jSetAdd, jStr2, NULL);
            EXCEPTION_OCCURED(env);
            was_set = TRUE;
        } else if (gtk_targets_include_text(targets, size)) {
            jstring jStr = env->NewStringUTF("text/plain");
            EXCEPTION_OCCURED(env);
            env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
            EXCEPTION_OCCURED(env);
            was_set = TRUE;
        }

        g_free(targets);

        if (!was_set) {
            GdkAtom target = gtk_selection_data_get_target(target_ctx.data);
            gchar *name = gdk_atom_name(target);

            jstring jStr = env->NewStringUTF(name);
            EXCEPTION_OCCURED(env);
            env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
            EXCEPTION_OCCURED(env);
            g_free(name);
        }

        target_ctx.mimes = env->NewObjectArray(env->CallIntMethod(set, jSetSize, NULL),
                                               jStringCls, NULL);
        EXCEPTION_OCCURED(env);
        target_ctx.mimes = (jobjectArray) env->CallObjectMethod(set, jSetToArray, target_ctx.mimes, NULL);
        target_ctx.mimes = (jobjectArray) env->NewGlobalRef(target_ctx.mimes);
    }

    return target_ctx.mimes;
}

jint dnd_target_get_supported_actions(JNIEnv *env) {
    if (check_state_in_drag(env)) {
        return 0;
    }
    return translate_gdk_action_to_glass(gdk_drag_context_get_actions(target_ctx.ctx));
}

static jobject dnd_target_get_string(JNIEnv *env) {
    jobject result = NULL;

    GdkAtom atom = gtk_selection_data_get_data_type(target_ctx.data);
    guchar *data = gtk_selection_data_get_text(target_ctx.data);

    if (data) {
        result = env->NewStringUTF((char *) data);
        EXCEPTION_OCCURED(env);

        g_free(data);
    }

    return result;
}

static jobject dnd_target_get_list(JNIEnv *env, gboolean files) {
    jobject result = NULL;
    GdkAtom atom = gtk_selection_data_get_selection(target_ctx.data);
    gchar **data = gtk_selection_data_get_uris(target_ctx.data);

    if (data) {
        result = uris_to_java(env, data, files);
        // uris_to_java frees it
        //g_strfreev(data);
    }

    return result;
}

static jobject dnd_target_get_image(JNIEnv *env) {
    jobject result = NULL;

    GdkAtom atom = gtk_selection_data_get_selection(target_ctx.data);
    GdkPixbuf *buf = gtk_selection_data_get_pixbuf(target_ctx.data);

    if (buf == NULL) {
        return NULL;
    }

    gint length = gtk_selection_data_get_length(target_ctx.data);

    if (!gdk_pixbuf_get_has_alpha(buf)) {
        GdkPixbuf *tmp_buf = gdk_pixbuf_add_alpha(buf, FALSE, 0, 0, 0);
        g_object_unref(buf);
        buf = tmp_buf;
    }

    gint w, h, stride;
    guchar *cdata;
    jbyteArray data_array;
    jobject buffer;

    w = gdk_pixbuf_get_width(buf);
    h = gdk_pixbuf_get_height(buf);
    stride = gdk_pixbuf_get_rowstride(buf);

    cdata = gdk_pixbuf_get_pixels(buf);

    //Actually, we are converting RGBA to BGRA, but that's the same operation
    cdata = (guchar *) convert_BGRA_to_RGBA((int *) cdata, stride, h);
    data_array = env->NewByteArray(stride * h);
    EXCEPTION_OCCURED(env);
    env->SetByteArrayRegion(data_array, 0, stride * h, (jbyte *) cdata);
    EXCEPTION_OCCURED(env);

    buffer = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, data_array);
    EXCEPTION_OCCURED(env);
    result = env->NewObject(jGtkPixelsCls, jGtkPixelsInit, w, h, buffer);
    EXCEPTION_OCCURED(env);

    g_object_unref(buf);
    g_free(cdata);

    return result;
}

static jobject dnd_target_get_raw(JNIEnv *env, GdkAtom target, gboolean string_data) {
    jobject result = NULL;
    GdkAtom atom = gtk_selection_data_get_selection(target_ctx.data);
    const guchar *data = gtk_selection_data_get_data(target_ctx.data);

    if (string_data) {
        result = env->NewStringUTF((char *) data);
        EXCEPTION_OCCURED(env);
    } else {
        gint length = gtk_selection_data_get_length(target_ctx.data);

        jbyteArray array = env->NewByteArray((jsize) length);
        EXCEPTION_OCCURED(env);
        env->SetByteArrayRegion(array, 0, length, (const jbyte *) data);
        EXCEPTION_OCCURED(env);
        result = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, array);
        EXCEPTION_OCCURED(env);
    }

    return result;
}

jobject dnd_target_get_data(JNIEnv *env, jstring mime) {
    jobject ret = NULL;

    if (check_state_in_drag(env)) {
        return NULL;
    }

    const char *cmime = env->GetStringUTFChars(mime, NULL);

    if (g_strcmp0(cmime, "text/plain") == 0) {
        ret = dnd_target_get_string(env);
    } else if (g_strcmp0(cmime, "text/uri-list") == 0) {
        ret = dnd_target_get_list(env, FALSE);
    } else if (g_str_has_prefix(cmime, "text/")) {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-file-list") == 0) {
        ret = dnd_target_get_list(env, TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-rawimage") == 0) {
        ret = dnd_target_get_image(env);
    } else {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), FALSE);
    }

    LOG_EXCEPTION(env)
    env->ReleaseStringUTFChars(mime, cmime);

    return ret;
}

/************************* SOURCE *********************************************/

static jint dnd_performed_action;

const char *const SOURCE_DND_DATA = "fx-dnd-data";

static void dnd_set_performed_action(jint performed_action) {
    dnd_performed_action = performed_action;
}

static jint dnd_get_performed_action() {
    return dnd_performed_action;
}

static void pixbufDestroyNotifyFunc(guchar *pixels, gpointer) {
    if (pixels != NULL) {
        g_free(pixels);
    }
}

static jobject dnd_source_get_data(GtkWidget *widget, const char *key) {
    jobject data = (jobject) g_object_get_data(G_OBJECT(widget), SOURCE_DND_DATA);
    jstring string = mainEnv->NewStringUTF(key);
    EXCEPTION_OCCURED(mainEnv);
    jobject result = mainEnv->CallObjectMethod(data, jMapGet, string, NULL);

    return (EXCEPTION_OCCURED(mainEnv)) ? NULL : result;
}

static void add_gtk_target_from_jstring(JNIEnv *env, GtkTargetList **list, jstring string, guint flags) {
    const char *gstring = env->GetStringUTFChars(string, NULL);

    if (g_strcmp0(gstring, "text/plain") == 0) {
        gtk_target_list_add_text_targets(*list, TARGET_TEXT);
    } else if (g_strcmp0(gstring, "application/x-java-rawimage") == 0) {
        gtk_target_list_add_image_targets(*list, TARGET_IMAGE, TRUE);
    } else if (g_strcmp0(gstring, "application/x-java-file-list") == 0) {
        gtk_target_list_add_uri_targets(*list, TARGET_URI);
    } else if (g_strcmp0(gstring, "application/x-java-drag-image") == 0
               || g_strcmp0(gstring, "application/x-java-drag-image-offset") == 0) {
        // do nothing - those are DragView information
    } else {
        GdkAtom atom = gdk_atom_intern(gstring, FALSE);
        gtk_target_list_add(*list, atom, flags, TARGET_RAW);
    }

    env->ReleaseStringUTFChars(string, gstring);
}

static GtkTargetList *data_to_gtk_target_list(JNIEnv *env, jobject data) {
    guint flags = GTK_TARGET_OTHER_APP | GTK_TARGET_SAME_APP;

    jobject keys;
    jobject keysIterator;
    jstring next;

    GtkTargetList *tlist = gtk_target_list_new(NULL, 0);

    gint added_count = 0;

    keys = env->CallObjectMethod(data, jMapKeySet, NULL);
    JNI_EXCEPTION_TO_CPP(env)
    keysIterator = env->CallObjectMethod(keys, jIterableIterator, NULL);
    JNI_EXCEPTION_TO_CPP(env)
    while (env->CallBooleanMethod(keysIterator, jIteratorHasNext) == JNI_TRUE) {
        next = (jstring) env->CallObjectMethod(keysIterator, jIteratorNext, NULL);
        JNI_EXCEPTION_TO_CPP(env)
        add_gtk_target_from_jstring(env, &tlist, next, flags);
    }

    return tlist;
}

static gboolean dnd_source_set_string(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom) {
    gboolean is_data_set;

    jstring string = (jstring) dnd_source_get_data(widget, "text/plain");
    if (!string) {
        return FALSE;
    }

    const char *cstring = mainEnv->GetStringUTFChars(string, NULL);
    gint size = strlen(cstring);
    is_data_set = gtk_selection_data_set_text(data, (gchar *) cstring, size);

    mainEnv->ReleaseStringUTFChars(string, cstring);

    return is_data_set;
}

static gboolean dnd_source_set_image(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom) {
    jobject pixels = dnd_source_get_data(widget, "application/x-java-rawimage");
    if (!pixels) {
        g_warning("DND source failed to set image\n");
        return FALSE;
    }

    gchar *buffer;
    gsize size;
    const char *type;
    GdkPixbuf *pixbuf = NULL;
    gboolean is_data_set;

    mainEnv->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));

    if (!EXCEPTION_OCCURED(mainEnv)) {
        is_data_set = gtk_selection_data_set_pixbuf(data, pixbuf);
    }

    g_object_unref(pixbuf);

    return is_data_set;
}

static gboolean dnd_source_set_uri(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom) {
    const gchar *url = NULL;
    jstring jurl = NULL;

    jobjectArray files_array = NULL;
    gsize files_cnt = 0;

    if (jurl = (jstring) dnd_source_get_data(widget, "text/uri-list")) {
        url = mainEnv->GetStringUTFChars(jurl, NULL);
    }

    if (files_array = (jobjectArray) dnd_source_get_data(widget, "application/x-java-file-list")) {
        files_cnt = mainEnv->GetArrayLength(files_array);
    }

    if (!url && !files_cnt) {
        return FALSE;
    }

    gboolean is_data_set;
    GString *res = g_string_new(NULL); //http://www.ietf.org/rfc/rfc2483.txt

    if (files_cnt > 0) {
        for (gsize i = 0; i < files_cnt; ++i) {
            jstring string = (jstring) mainEnv->GetObjectArrayElement(files_array, i);
            EXCEPTION_OCCURED(mainEnv);
            const gchar *file = mainEnv->GetStringUTFChars(string, NULL);
            gchar *uri = g_filename_to_uri(file, NULL, NULL);

            g_string_append(res, uri);
            g_string_append(res, URI_LIST_LINE_BREAK);

            g_free(uri);
            mainEnv->ReleaseStringUTFChars(string, file);
        }
    }
    if (url) {
        g_string_append(res, url);
        g_string_append(res, URI_LIST_LINE_BREAK);
        mainEnv->ReleaseStringUTFChars(jurl, url);
    }

    gchar *uri[2];
    uri[0] = g_string_free(res, FALSE);
    uri[1] = NULL;

    is_data_set = gtk_selection_data_set_uris(data, uri);

    g_free(uri[0]);

    return is_data_set;
}

static gboolean dnd_source_set_raw(GtkWidget *widget, GtkSelectionData *sel_data, GdkAtom atom) {
    gchar *target_name = gdk_atom_name(atom);
    jobject data = dnd_source_get_data(widget, target_name);
    gboolean is_data_set = FALSE;
    if (data) {
        if (mainEnv->IsInstanceOf(data, jStringCls)) {
            const char *cstring = mainEnv->GetStringUTFChars((jstring) data, NULL);
            if (cstring) {
                is_data_set = gtk_selection_data_set_text(sel_data, (gchar *) cstring, strlen(cstring));
                mainEnv->ReleaseStringUTFChars((jstring) data, cstring);
            }
        } else if (mainEnv->IsInstanceOf(data, jByteBufferCls)) {
            jbyteArray byteArray = (jbyteArray) mainEnv->CallObjectMethod(data, jByteBufferArray);
            if (!EXCEPTION_OCCURED(mainEnv)) {
                jbyte *raw = mainEnv->GetByteArrayElements(byteArray, NULL);
                if (raw) {
                    jsize nraw = mainEnv->GetArrayLength(byteArray);
                    gtk_selection_data_set(sel_data, atom, 8, (guchar *) raw, nraw);
                    mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
                    is_data_set = TRUE;
                }
            }
        }
    }

    g_free(target_name);
    return is_data_set;
}

static gboolean dnd_destroy_drag_widget_callback(gpointer) {
    if (drag_widget) {
        gtk_widget_destroy(drag_widget);
        drag_widget = NULL;
    }

    return FALSE;
}

static void dnd_end_callback(GtkWidget *widget,
                             GdkDragContext *context,
                             gpointer user_data) {
    if (drag_widget) {
        GdkDragAction action = gdk_drag_context_get_selected_action(context);
        dnd_set_performed_action(translate_gdk_action_to_glass(action));
    }
    gdk_threads_add_idle((GSourceFunc) dnd_destroy_drag_widget_callback, NULL);
}

static gboolean dnd_drag_failed_callback(GtkWidget *widget,
                                         GdkDragContext *context,
                                         GtkDragResult result,
                                         gpointer user_data) {
    dnd_set_performed_action(com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_NONE);
    gdk_threads_add_idle((GSourceFunc) dnd_destroy_drag_widget_callback, NULL);

    return FALSE;
}

static void dnd_data_get_callback(GtkWidget *widget,
                                  GdkDragContext *context,
                                  GtkSelectionData *data,
                                  guint info,
                                  guint time,
                                  gpointer user_data) {
    GdkAtom atom = gtk_selection_data_get_target(data);

    switch (info) {
        case TARGET_TEXT:
            dnd_source_set_string(widget, data, atom);
            break;
        case TARGET_IMAGE:
            dnd_source_set_image(widget, data, atom);
            break;
        case TARGET_URI:
            dnd_source_set_uri(widget, data, atom);
            break;
        default:
            dnd_source_set_raw(widget, data, atom);
    }
}

static void dnd_drag_begin_callback(GtkWidget *widget,
                                    GdkDragContext *context,
                                    gpointer user_data) {
    DragView::set_drag_view(widget, context);
}

static void dnd_source_push_data(JNIEnv *env, jobject data, jint supported) {
    if (supported == 0) {
        return; // No supported actions, do nothing
    }

    data = env->NewGlobalRef(data);

    GdkDragAction actions = translate_glass_action_to_gdk(supported);

    // this widget is used only to pass events and will
    // be destroyed on drag end
    drag_widget = gtk_invisible_new();
    gtk_widget_show(drag_widget);

    g_object_set_data_full(G_OBJECT(drag_widget), SOURCE_DND_DATA, data, clear_global_ref);

    g_signal_connect(drag_widget, "drag-begin",
                     G_CALLBACK(dnd_drag_begin_callback), NULL);

    g_signal_connect(drag_widget, "drag-failed",
                     G_CALLBACK(dnd_drag_failed_callback), NULL);

    g_signal_connect(drag_widget, "drag-data-get",
                     G_CALLBACK(dnd_data_get_callback), NULL);

    g_signal_connect(drag_widget, "drag-end",
                     G_CALLBACK(dnd_end_callback), NULL);

    GtkTargetList *tlist = data_to_gtk_target_list(env, data);

    GdkDragContext *context;

    gint x, y;
    glass_gdk_master_pointer_get_position(&x, &y);

    is_dnd_owner = TRUE;

    context = gtk_drag_begin(drag_widget, tlist, actions, 1, NULL);

    gtk_target_list_unref(tlist);
}

jint execute_dnd(JNIEnv *env, jobject data, jint supported) {
    try {
        dnd_source_push_data(env, data, supported);
    } catch (jni_exception &) {
        gdk_threads_add_idle((GSourceFunc) dnd_destroy_drag_widget_callback, NULL);
        return com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_NONE;
    }

    while (is_in_drag()) {
        gtk_main_iteration();
    }

    return dnd_get_performed_action();
}

/******************** DRAG VIEW ***************************/
DragView::View *DragView::view = NULL;

gboolean DragView::get_drag_image_offset(GtkWidget *widget, int *x, int *y) {
    gboolean offset_set = FALSE;
    jobject bb = dnd_source_get_data(widget, "application/x-java-drag-image-offset");
    if (bb) {
        jbyteArray byteArray = (jbyteArray) mainEnv->CallObjectMethod(bb, jByteBufferArray);
        if (!EXCEPTION_OCCURED(mainEnv)) {
            jbyte *raw = mainEnv->GetByteArrayElements(byteArray, NULL);
            jsize nraw = mainEnv->GetArrayLength(byteArray);

            if ((size_t) nraw >= sizeof(jint) * 2) {
                jint *r = (jint *) raw;
                *x = BSWAP_32(r[0]);
                *y = BSWAP_32(r[1]);
                offset_set = TRUE;
            }

            mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
        }
    }
    return offset_set;
}

GdkPixbuf *DragView::get_drag_image(GtkWidget *widget, gboolean *is_raw_image, gint *width, gint *height) {
    GdkPixbuf *pixbuf = NULL;
    gboolean is_raw = FALSE;

    jobject drag_image = dnd_source_get_data(widget, "application/x-java-drag-image");

    if (drag_image) {
        jbyteArray byteArray = (jbyteArray) mainEnv->CallObjectMethod(drag_image, jByteBufferArray);
        if (!EXCEPTION_OCCURED(mainEnv)) {

            jbyte *raw = mainEnv->GetByteArrayElements(byteArray, NULL);
            jsize nraw = mainEnv->GetArrayLength(byteArray);

            int w = 0, h = 0;
            int whsz = sizeof(jint) * 2; // Pixels are stored right after two ints
            // in this byteArray: width and height
            if (nraw > whsz) {
                jint *int_raw = (jint *) raw;
                w = BSWAP_32(int_raw[0]);
                h = BSWAP_32(int_raw[1]);

                // We should have enough pixels for requested width and height
                if ((nraw - whsz) / 4 - w * h >= 0) {
                    guchar *data = (guchar *) g_try_malloc0(nraw - whsz);
                    if (data) {
                        memcpy(data, (raw + whsz), nraw - whsz);
                        pixbuf = gdk_pixbuf_new_from_data(data, GDK_COLORSPACE_RGB, TRUE, 8,
                                                          w, h, w * 4, pixbufDestroyNotifyFunc, NULL);
                    }
                }
            }
            mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
        }
    }

    if (!GDK_IS_PIXBUF(pixbuf)) {
        jobject pixels = dnd_source_get_data(widget, "application/x-java-rawimage");
        if (pixels) {
            is_raw = TRUE;
            mainEnv->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));
            CHECK_JNI_EXCEPTION_RET(mainEnv, NULL)
        }
    }

    if (!GDK_IS_PIXBUF(pixbuf)) {
        return NULL;
    }

    int w = gdk_pixbuf_get_width(pixbuf);
    int h = gdk_pixbuf_get_height(pixbuf);

    if (w > DRAG_IMAGE_MAX_WIDTH || h > DRAG_IMAGE_MAX_HEIGH) {
        double rw = DRAG_IMAGE_MAX_WIDTH / (double) w;
        double rh = DRAG_IMAGE_MAX_HEIGH / (double) h;
        double r = MIN(rw, rh);

        int new_w = w * r;
        int new_h = h * r;

        w = new_w;
        h = new_h;

        GdkPixbuf *tmp_pixbuf = gdk_pixbuf_scale_simple(pixbuf, new_w, new_h, GDK_INTERP_TILES);
        g_object_unref(pixbuf);
        if (!GDK_IS_PIXBUF(tmp_pixbuf)) {
            return NULL;
        }
        pixbuf = tmp_pixbuf;
    }

    *is_raw_image = is_raw;
    *width = w;
    *height = h;

    return pixbuf;
}

void DragView::set_drag_view(GtkWidget *widget, GdkDragContext *context) {
    gboolean is_raw_image = FALSE;
    gint w = 0, h = 0;
    GdkPixbuf *pixbuf = get_drag_image(widget, &is_raw_image, &w, &h);

    if (GDK_IS_PIXBUF(pixbuf)) {
        gint offset_x = w / 2;
        gint offset_y = h / 2;

        gboolean is_offset_set = get_drag_image_offset(widget, &offset_x, &offset_y);

        DragView::view = new DragView::View(context, pixbuf, w, h, is_raw_image,
                                            is_offset_set, offset_x, offset_y);
    }
}

static void on_screen_changed(GtkWidget *widget, GdkScreen *previous_screen, gpointer view) {
    (void) widget;
    (void) previous_screen;

    ((DragView::View *) view)->screen_changed();
}

static gboolean on_expose(GtkWidget *widget, GdkEventExpose *event, gpointer view) {
    (void) widget;
    (void) event;

    ((DragView::View *) view)->expose();
    return FALSE;
}

DragView::View::View(GdkDragContext *_context, GdkPixbuf *_pixbuf, gint _width, gint _height,
                     gboolean _is_raw_image, gboolean _is_offset_set, gint _offset_x, gint _offset_y) :
        context(_context),
        pixbuf(_pixbuf),
        width(_width),
        height(_height),
        is_raw_image(_is_raw_image),
        is_offset_set(_is_offset_set),
        offset_x(_offset_x),
        offset_y(_offset_y) {
#ifdef GLASS_GTK3
    gtk_drag_set_icon_pixbuf(context, pixbuf, offset_x, offset_y);
#else
    widget = gtk_window_new(GTK_WINDOW_POPUP);
    gtk_window_set_type_hint(GTK_WINDOW(widget), GDK_WINDOW_TYPE_HINT_DND);
    gtk_widget_set_events(widget, GDK_BUTTON_PRESS_MASK | GDK_BUTTON_RELEASE_MASK);

    screen_changed();

    gtk_widget_realize(widget);

    gtk_widget_set_app_paintable(widget, TRUE);
    g_signal_connect(G_OBJECT(widget), "expose-event", G_CALLBACK(on_expose), this);
    g_signal_connect(G_OBJECT(widget), "screen-changed", G_CALLBACK(on_screen_changed), this);
    gtk_widget_set_size_request(widget, width, height);
    gtk_window_set_decorated(GTK_WINDOW(widget), FALSE);

    gtk_widget_show_all(widget);
    gtk_drag_set_icon_widget(context, widget, offset_x, offset_y);
#endif
}

void DragView::View::screen_changed() {
    GdkScreen *screen = gtk_widget_get_screen(widget);

    glass_configure_window_transparency(widget, true);

    if (!gdk_screen_is_composited(screen)) {
        if (!is_offset_set) {
            offset_x = 1;
            offset_y = 1;
        }
    }
}

void DragView::View::expose() {
#ifdef GLASS_GTK2
    cairo_t *context = gdk_cairo_create(gtk_widget_get_window(widget));

    cairo_surface_t *cairo_surface;

    guchar *pixels = is_raw_image
                     ? (guchar *) convert_BGRA_to_RGBA((const int *) gdk_pixbuf_get_pixels(pixbuf),
                                                       gdk_pixbuf_get_rowstride(pixbuf),
                                                       height)
                     : gdk_pixbuf_get_pixels(pixbuf);

    cairo_surface = cairo_image_surface_create_for_data(
            pixels,
            CAIRO_FORMAT_ARGB32,
            width, height, width * 4);

    cairo_set_source_surface(context, cairo_surface, 0, 0);
    cairo_set_operator(context, CAIRO_OPERATOR_SOURCE);
    cairo_paint(context);

    if (is_raw_image) {
        g_free(pixels);
    }
    cairo_destroy(context);
    cairo_surface_destroy(cairo_surface);
#endif
}
