/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
    result |= (action & GDK_ACTION_COPY)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_COPY : 0;
    result |= (action & GDK_ACTION_MOVE)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_MOVE : 0;
    result |= (action & GDK_ACTION_LINK)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_REFERENCE : 0;
    return result;
}

static GdkDragAction translate_glass_action_to_gdk(jint action) {
    int result = 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_COPY)? GDK_ACTION_COPY : 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_MOVE)? GDK_ACTION_MOVE : 0;
    result |= (action & com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_REFERENCE)? GDK_ACTION_LINK : 0;
    return static_cast<GdkDragAction>(result);
}

static gboolean target_atoms_initialized = FALSE;
static GdkAtom TARGET_UTF8_STRING_ATOM;
static GdkAtom TARGET_MIME_TEXT_PLAIN_ATOM;
static GdkAtom TARGET_COMPOUND_TEXT_ATOM;
static GdkAtom TARGET_STRING_ATOM;
static GdkAtom TARGET_MIME_URI_LIST_ATOM;
static GdkAtom TARGET_MIME_PNG_ATOM;
static GdkAtom TARGET_MIME_JPEG_ATOM;
static GdkAtom TARGET_MIME_TIFF_ATOM;
static GdkAtom TARGET_MIME_BMP_ATOM;

static void init_target_atoms()
{
    if (target_atoms_initialized) {
        return;
    }
    TARGET_UTF8_STRING_ATOM = gdk_atom_intern_static_string("UTF8_STRING");
    TARGET_MIME_TEXT_PLAIN_ATOM = gdk_atom_intern_static_string("text/plain");
    TARGET_COMPOUND_TEXT_ATOM = gdk_atom_intern_static_string("COMPOUND_TEXT");
    TARGET_STRING_ATOM = gdk_atom_intern_static_string("STRING");

    TARGET_MIME_URI_LIST_ATOM = gdk_atom_intern_static_string("text/uri-list");

    TARGET_MIME_PNG_ATOM = gdk_atom_intern_static_string("image/png");
    TARGET_MIME_JPEG_ATOM = gdk_atom_intern_static_string("image/jpeg");
    TARGET_MIME_TIFF_ATOM = gdk_atom_intern_static_string("image/tiff");
    TARGET_MIME_BMP_ATOM = gdk_atom_intern_static_string("image/bmp");

    target_atoms_initialized = TRUE;
}

static gboolean target_is_text(GdkAtom target) {
    init_target_atoms();

    return (target == TARGET_UTF8_STRING_ATOM ||
            target == TARGET_STRING_ATOM ||
            target == TARGET_MIME_TEXT_PLAIN_ATOM/* ||
            target == TARGET_COMPOUND_TEXT_ATOM*/);
}

static gboolean target_is_uri(GdkAtom target) {
    init_target_atoms();
    return target == TARGET_MIME_URI_LIST_ATOM;
}

static gboolean target_is_image(GdkAtom target) {
    init_target_atoms();
    return (target == TARGET_MIME_PNG_ATOM ||
            target == TARGET_MIME_JPEG_ATOM ||
            target == TARGET_MIME_TIFF_ATOM ||
            target == TARGET_MIME_BMP_ATOM);
}

static void clear_global_ref(gpointer data) {
    mainEnv->DeleteGlobalRef((jobject)data);
}

/************************* TARGET *********************************************/
struct selection_data_ctx {
    gboolean received;
    guchar *data;
    GdkAtom type;
    gint format;
    gint length;
};

static gboolean dnd_target_receive_data(JNIEnv *env, GdkAtom target, selection_data_ctx *selection_ctx);

static struct {
    GdkDragContext *ctx;
    gboolean just_entered;
    jobjectArray mimes;
    gint dx, dy;
} enter_ctx = {NULL, FALSE, NULL, 0, 0};

gboolean is_dnd_owner = FALSE;

static void reset_enter_ctx() {
    if (enter_ctx.mimes != NULL) {
        mainEnv->DeleteGlobalRef(enter_ctx.mimes);
    }
    memset(&enter_ctx, 0, sizeof(enter_ctx));
}

static void process_dnd_target_drag_enter(WindowContext *ctx, GdkEventDND *event) {
    reset_enter_ctx();
    enter_ctx.ctx = event->context;
    enter_ctx.just_entered = TRUE;
    gdk_window_get_origin(ctx->get_gdk_window(), &enter_ctx.dx, &enter_ctx.dy);
    is_dnd_owner = is_in_drag();
}

static void process_dnd_target_drag_motion(WindowContext *ctx, GdkEventDND *event) {
    if (!enter_ctx.ctx) {
        gdk_drag_status(event->context, static_cast<GdkDragAction>(0), GDK_CURRENT_TIME);
        return; // Do not process motion events if no enter event was received
    }

    jmethodID method = enter_ctx.just_entered ? jViewNotifyDragEnter : jViewNotifyDragOver;
    GdkDragAction suggested = gdk_drag_context_get_suggested_action(event->context);
    GdkDragAction result = translate_glass_action_to_gdk(mainEnv->CallIntMethod(ctx->get_jview(), method,
            (jint)event->x_root - enter_ctx.dx, (jint)event->y_root - enter_ctx.dy,
            (jint)event->x_root, (jint)event->y_root,
            translate_gdk_action_to_glass(suggested)));
    CHECK_JNI_EXCEPTION(mainEnv)

    if (enter_ctx.just_entered) {
        enter_ctx.just_entered = FALSE;
    }

    gdk_drag_status(event->context, result, GDK_CURRENT_TIME);
}

static void process_dnd_target_drag_leave(WindowContext *ctx, GdkEventDND *event) {
    (void)event;

    mainEnv->CallVoidMethod(ctx->get_jview(), jViewNotifyDragLeave, NULL);
    CHECK_JNI_EXCEPTION(mainEnv)
}

static void wait_for_selection_data_hook(GdkEvent * event, void * data) {
    selection_data_ctx *ctx = (selection_data_ctx*)data;
    GdkWindow *dest = glass_gdk_drag_context_get_dest_window(enter_ctx.ctx);
    if (event->type == GDK_SELECTION_NOTIFY &&
            event->selection.window == dest) {
        if (event->selection.property) { // if 0, that we received negative response
            ctx->length = gdk_selection_property_get(dest, &(ctx->data), &(ctx->type), &(ctx->format));
        }
        ctx->received = TRUE;
    }
}

static void process_dnd_target_drop_start(WindowContext *ctx, GdkEventDND *event) {
    if (!enter_ctx.ctx || enter_ctx.just_entered) {
        gdk_drop_finish(event->context, FALSE, GDK_CURRENT_TIME);
        gdk_drop_reply(event->context, FALSE, GDK_CURRENT_TIME);
        return; // Do not process drop events if no enter event and subsequent motion event were received
    }

    GdkDragAction selected = gdk_drag_context_get_selected_action(event->context);
    mainEnv->CallIntMethod(ctx->get_jview(), jViewNotifyDragDrop,
            (jint)event->x_root - enter_ctx.dx, (jint)event->y_root - enter_ctx.dy,
            (jint)event->x_root, (jint)event->y_root,
            translate_gdk_action_to_glass(selected));
    LOG_EXCEPTION(mainEnv)

    gdk_drop_finish(event->context, TRUE, GDK_CURRENT_TIME);
    gdk_drop_reply(event->context, TRUE, GDK_CURRENT_TIME);
}

static gboolean check_state_in_drag(JNIEnv *env) {
    if (!enter_ctx.ctx) {
        jclass jc = env->FindClass("java/lang/IllegalStateException");
        if (!env->ExceptionCheck()) {
            env->ThrowNew(jc,
                    "Cannot get supported actions. Drag pointer haven't entered the application window");
        }
        return TRUE;
    }
    return FALSE;
}

// Events coming from application that are related to us being a DnD target
void process_dnd_target(WindowContext *ctx, GdkEventDND *event) {
    switch (event->type) {
        case GDK_DRAG_ENTER:
            process_dnd_target_drag_enter(ctx, event);
            break;
        case GDK_DRAG_MOTION:
            process_dnd_target_drag_motion(ctx, event);
            break;
        case GDK_DRAG_LEAVE:
            process_dnd_target_drag_leave(ctx, event);
            break;
        case GDK_DROP_START:
            process_dnd_target_drop_start(ctx, event);
            break;
        default:
            break;
    }
}

jobjectArray dnd_target_get_mimes(JNIEnv *env) {
    if (check_state_in_drag(env)) {
        return NULL;
    }
    if (!enter_ctx.mimes) {
        GList* targets = gdk_drag_context_list_targets(enter_ctx.ctx);
        jobject set = env->NewObject(jHashSetCls, jHashSetInit, NULL);
        EXCEPTION_OCCURED(env);

        while (targets) {
            GdkAtom target = GDK_POINTER_TO_ATOM(targets->data);
            gchar *name = gdk_atom_name(target);

            if (target_is_text(target)) {
                jstring jStr = env->NewStringUTF("text/plain");
                EXCEPTION_OCCURED(env);
                env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
                EXCEPTION_OCCURED(env);
            }

            if (target_is_image(target)) {
                jstring jStr = env->NewStringUTF("application/x-java-rawimage");
                EXCEPTION_OCCURED(env);
                env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
                EXCEPTION_OCCURED(env);
            }

            if (target_is_uri(target)) {
                selection_data_ctx ctx;
                if (dnd_target_receive_data(env, TARGET_MIME_URI_LIST_ATOM, &ctx)) {
                    gchar** uris = g_uri_list_extract_uris((gchar *) ctx.data);
                    guint size = g_strv_length(uris);
                    guint files_cnt = get_files_count(uris);
                    if (files_cnt) {
                        jstring jStr = env->NewStringUTF("application/x-java-file-list");
                        EXCEPTION_OCCURED(env);
                        env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
                        EXCEPTION_OCCURED(env);
                    }
                    if (size - files_cnt) {
                        jstring jStr = env->NewStringUTF("text/uri-list");
                        EXCEPTION_OCCURED(env);
                        env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
                        EXCEPTION_OCCURED(env);
                    }
                    g_strfreev(uris);
                }
                g_free(ctx.data);
            } else {
                jstring jStr = env->NewStringUTF(name);
                EXCEPTION_OCCURED(env);
                env->CallBooleanMethod(set, jSetAdd, jStr, NULL);
                EXCEPTION_OCCURED(env);
            }

            g_free(name);
            targets = targets->next;
        }
        enter_ctx.mimes = env->NewObjectArray(env->CallIntMethod(set, jSetSize, NULL),
                jStringCls, NULL);
        EXCEPTION_OCCURED(env);
        enter_ctx.mimes = (jobjectArray)env->CallObjectMethod(set, jSetToArray, enter_ctx.mimes, NULL);
        enter_ctx.mimes = (jobjectArray)env->NewGlobalRef(enter_ctx.mimes);
    }
    return enter_ctx.mimes;
}

jint dnd_target_get_supported_actions(JNIEnv *env) {
    if (check_state_in_drag(env)) {
        return 0;
    }
    return translate_gdk_action_to_glass(gdk_drag_context_get_actions(enter_ctx.ctx));
}

static gboolean dnd_target_receive_data(JNIEnv *env, GdkAtom target, selection_data_ctx *selection_ctx) {
    GevlHookRegistration hookReg;

    gdk_selection_convert(glass_gdk_drag_context_get_dest_window(enter_ctx.ctx),
                          gdk_drag_get_selection(enter_ctx.ctx),
                          target,
                          GDK_CURRENT_TIME);

    memset(selection_ctx, 0, sizeof(selection_data_ctx));

    hookReg = glass_evloop_hook_add((GevlHookFunction) wait_for_selection_data_hook,
                                  selection_ctx);
    if (HANDLE_MEM_ALLOC_ERROR(env, hookReg, "Failed to allocate event hook")) {
      return TRUE;
    }

    do {
        gtk_main_iteration();
    } while (!(selection_ctx->received));

    glass_evloop_hook_remove(hookReg);
    return selection_ctx->data != NULL;
}

static jobject dnd_target_get_string(JNIEnv *env) {
    jobject result = NULL;
    selection_data_ctx ctx;

    if (dnd_target_receive_data(env, TARGET_UTF8_STRING_ATOM, &ctx)) {
        result = env->NewStringUTF((char *)ctx.data);
        EXCEPTION_OCCURED(env);
        g_free(ctx.data);
    }
    if (!result && dnd_target_receive_data(env, TARGET_MIME_TEXT_PLAIN_ATOM, &ctx)) {
        result = env->NewStringUTF((char *)ctx.data);
        EXCEPTION_OCCURED(env);
        g_free(ctx.data);
    }
    // TODO find out how to convert from compound text
    // if (!result && dnd_target_receive_data(env, TARGET_COMPOUND_TEXT_ATOM, &ctx)) {
    // }
    if (!result && dnd_target_receive_data(env, TARGET_STRING_ATOM, &ctx)) {
        gchar *str;
        str = g_convert( (gchar *)ctx.data, -1, "UTF-8", "ISO-8859-1", NULL, NULL, NULL);
        if (str != NULL) {
            result = env->NewStringUTF(str);
            EXCEPTION_OCCURED(env);
            g_free(str);
        }
        g_free(ctx.data);
    }
    return result;
}

static jobject dnd_target_get_list(JNIEnv *env, gboolean files) {
    jobject result = NULL;
    selection_data_ctx ctx;

    if (dnd_target_receive_data(env, TARGET_MIME_URI_LIST_ATOM, &ctx)) {
        result = uris_to_java(env, g_uri_list_extract_uris((gchar *)ctx.data), files);
        g_free(ctx.data);
    }

    return result;
}

static jobject dnd_target_get_image(JNIEnv *env) {
    GdkPixbuf *buf;
    GInputStream *stream;
    jobject result = NULL;
    GdkAtom targets[] = {
        TARGET_MIME_PNG_ATOM,
        TARGET_MIME_JPEG_ATOM,
        TARGET_MIME_TIFF_ATOM,
        TARGET_MIME_BMP_ATOM,
        0};
    GdkAtom *cur_target = targets;
    selection_data_ctx ctx;

    while(*cur_target != 0 && result == NULL) {
        if (dnd_target_receive_data(env, *cur_target, &ctx)) {
            stream = g_memory_input_stream_new_from_data(ctx.data, ctx.length * (ctx.format / 8),
                    (GDestroyNotify)g_free);
            buf = gdk_pixbuf_new_from_stream(stream, NULL, NULL);
            if (buf) {
                int w;
                int h;
                int stride;
                guchar *data;
                jbyteArray data_array;
                jobject buffer;

                if (!gdk_pixbuf_get_has_alpha(buf)) {
                    GdkPixbuf *tmp_buf = gdk_pixbuf_add_alpha(buf, FALSE, 0, 0, 0);
                    g_object_unref(buf);
                    buf = tmp_buf;
                }

                w = gdk_pixbuf_get_width(buf);
                h = gdk_pixbuf_get_height(buf);
                stride = gdk_pixbuf_get_rowstride(buf);
                data = gdk_pixbuf_get_pixels(buf);

                //Actually, we are converting RGBA to BGRA, but that's the same operation
                data = (guchar*) convert_BGRA_to_RGBA((int*) data, stride, h);
                data_array = env->NewByteArray(stride * h);
                EXCEPTION_OCCURED(env);
                env->SetByteArrayRegion(data_array, 0, stride*h, (jbyte*) data);
                EXCEPTION_OCCURED(env);

                buffer = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, data_array);
                EXCEPTION_OCCURED(env);
                result = env->NewObject(jGtkPixelsCls, jGtkPixelsInit, w, h, buffer);
                EXCEPTION_OCCURED(env);

                g_object_unref(buf);
                g_free(data); // data from convert_BGRA_to_RGBA
            }
            g_object_unref(stream);
        }
        ++cur_target;
    }
    return result;
}

static jobject dnd_target_get_raw(JNIEnv *env, GdkAtom target, gboolean string_data) {
    selection_data_ctx ctx;
    jobject result = NULL;
    if (dnd_target_receive_data(env, target, &ctx)) {
        if (string_data) {
             result = env->NewStringUTF((char *)ctx.data);
             EXCEPTION_OCCURED(env);
        } else {
            jsize length = ctx.length * (ctx.format / 8);
            jbyteArray array = env->NewByteArray(length);
            EXCEPTION_OCCURED(env);
            env->SetByteArrayRegion(array, 0, length, (const jbyte*)ctx.data);
            EXCEPTION_OCCURED(env);
            result = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, array);
            EXCEPTION_OCCURED(env);
        }
    }
    g_free(ctx.data);
    return result;
}

jobject dnd_target_get_data(JNIEnv *env, jstring mime) {
    if (check_state_in_drag(env)) {
        return NULL;
    }

    const char *cmime = env->GetStringUTFChars(mime, NULL);
    jobject ret = NULL;

    init_target_atoms();

    if (g_strcmp0(cmime, "text/plain") == 0) {
        ret = dnd_target_get_string(env);
    } else if (g_strcmp0(cmime, "text/uri-list") == 0) {
        ret = dnd_target_get_list(env, FALSE);
    } else if (g_str_has_prefix(cmime, "text/")) {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-file-list") == 0) {
        ret = dnd_target_get_list(env, TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-rawimage") == 0 ) {
        ret = dnd_target_get_image(env);
    } else {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), FALSE);
    }
    LOG_EXCEPTION(env)
    env->ReleaseStringUTFChars(mime, cmime);

    return ret;
}

/************************* SOURCE *********************************************/
static bool in_drag = false;
const char * const SOURCE_DND_CONTEXT = "fx-dnd-context";
const char * const SOURCE_DND_DRAG_VIEW = "fx-dnd-drag-view";

gboolean is_in_drag() {
    return in_drag;
}

static void determine_actions(DragSourceContext *ctx, uint state, GdkDragAction *action,
                              GdkDragAction *possible_actions) {
    GdkDragAction suggested = ctx->actions;
    if (state & (GDK_SHIFT_MASK | GDK_CONTROL_MASK)) {
        if ((state & GDK_CONTROL_MASK) && (state & GDK_SHIFT_MASK) && (suggested & GDK_ACTION_LINK)) {
            *action = *possible_actions = GDK_ACTION_LINK;
            return;
        } else if ((state & GDK_SHIFT_MASK) && (suggested & GDK_ACTION_MOVE)) {
            *action = *possible_actions = GDK_ACTION_MOVE;
            return;
        } else if (suggested & GDK_ACTION_COPY) {
            *action = *possible_actions = GDK_ACTION_COPY;
            return;
        }
    }

    *possible_actions = suggested;

    if (suggested & GDK_ACTION_COPY) {
        *action = GDK_ACTION_COPY;
    } else if (suggested & GDK_ACTION_MOVE) {
        *action = GDK_ACTION_MOVE;
    } else if (suggested & GDK_ACTION_LINK) {
        *action = GDK_ACTION_LINK;
    } else {
        *action = static_cast<GdkDragAction>(0);
    }
}

static jobject dnd_source_get_data(jobject obj, const char *key) {
    jstring string = mainEnv->NewStringUTF(key);
    EXCEPTION_OCCURED(mainEnv);
    jobject result = mainEnv->CallObjectMethod(obj, jMapGet, string, NULL);

    return (EXCEPTION_OCCURED(mainEnv)) ? NULL : result;
}

static gboolean dnd_source_set_utf8_string(DragSourceContext *ctx, GdkWindow *requestor, GdkAtom property) {
    jstring string = (jstring)dnd_source_get_data(ctx->data, "text/plain");
    if (!string) {
        return FALSE;
    }

    const char *cstring = mainEnv->GetStringUTFChars(string, NULL);
    if (!cstring) {
        return FALSE;
    }
    gint size = strlen(cstring);

    gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
            8, GDK_PROP_MODE_REPLACE, (guchar *)cstring, size);

    mainEnv->ReleaseStringUTFChars(string, cstring);
    return TRUE;
}

static gboolean dnd_source_set_string(DragSourceContext *ctx, GdkWindow *requestor, GdkAtom property) {
    jstring string = (jstring)dnd_source_get_data(ctx->data, "text/plain");
    if (!string) {
        return FALSE;
    }

    gboolean is_data_set = FALSE;
    const char *cstring = mainEnv->GetStringUTFChars(string, NULL);
    if (cstring) {
        gchar *res_str = g_convert((gchar *)cstring, -1, "ISO-8859-1", "UTF-8", NULL, NULL, NULL);

        if (res_str) {
            gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                    8, GDK_PROP_MODE_REPLACE, (guchar *)res_str, strlen(res_str));
            g_free(res_str);
            is_data_set = TRUE;
        }

        mainEnv->ReleaseStringUTFChars(string, cstring);
    }
    return is_data_set;
}

static gboolean dnd_source_set_image(DragSourceContext *ctx, GdkWindow *requestor, GdkAtom property, GdkAtom target) {
    jobject pixels = dnd_source_get_data(ctx->data, "application/x-java-rawimage");
    if (!pixels) {
        return FALSE;
    }

    gchar *buffer;
    gsize size;
    const char * type;
    GdkPixbuf *pixbuf = NULL;
    gboolean result = FALSE;

    if (target == TARGET_MIME_PNG_ATOM) {
        type = "png";
    } else if (target == TARGET_MIME_JPEG_ATOM) {
        type = "jpeg";
    } else if (target == TARGET_MIME_TIFF_ATOM) {
        type = "tiff";
    } else if (target == TARGET_MIME_BMP_ATOM) {
        type = "bmp";
    } else {
        return FALSE;
    }

    mainEnv->CallVoidMethod(pixels, jPixelsAttachData, PTR_TO_JLONG(&pixbuf));

    if (!EXCEPTION_OCCURED(mainEnv)
            && gdk_pixbuf_save_to_buffer(pixbuf, &buffer, &size, type, NULL, NULL)) {
        gdk_property_change(requestor, property, target,
                8, GDK_PROP_MODE_REPLACE, (guchar *)buffer, size);
        result = TRUE;
    }
    g_object_unref(pixbuf);
    return result;
}

static gboolean dnd_source_set_uri_list(DragSourceContext *ctx, GdkWindow *requestor, GdkAtom property) {
    const gchar* url = NULL;
    jstring jurl = NULL;

    jobjectArray files_array = NULL;
    gsize files_cnt = 0;

    if (jurl = (jstring) dnd_source_get_data(ctx->data, "text/uri-list")) {
        url = mainEnv->GetStringUTFChars(jurl, NULL);
    }

    if (files_array = (jobjectArray) dnd_source_get_data(ctx->data, "application/x-java-file-list")) {
        files_cnt = mainEnv->GetArrayLength(files_array);
    }
    if (!url && !files_cnt) {
        return FALSE;
    }

    GString* res = g_string_new (NULL); //http://www.ietf.org/rfc/rfc2483.txt

    if (files_cnt > 0) {
        for (gsize i = 0; i < files_cnt; ++i) {
            jstring string = (jstring) mainEnv->GetObjectArrayElement(files_array, i);
            EXCEPTION_OCCURED(mainEnv);
            const gchar* file = mainEnv->GetStringUTFChars(string, NULL);
            gchar* uri = g_filename_to_uri(file, NULL, NULL);

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

    gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
            8, GDK_PROP_MODE_REPLACE, (guchar *) res->str, res->len);

    g_string_free(res, TRUE);
    return TRUE;
}

static gboolean dnd_source_set_raw(DragSourceContext *ctx, GdkWindow *requestor, GdkAtom property, GdkAtom target) {
    gchar *target_name = gdk_atom_name(target);
    jobject data = dnd_source_get_data(ctx->data, target_name);
    gboolean is_data_set = FALSE;
    if (data) {
        if (mainEnv->IsInstanceOf(data, jStringCls)) {
            const char *cstring = mainEnv->GetStringUTFChars((jstring)data, NULL);
            if (cstring) {
                gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                        8, GDK_PROP_MODE_REPLACE, (guchar *) cstring, strlen(cstring));

                mainEnv->ReleaseStringUTFChars((jstring)data, cstring);
                is_data_set = TRUE;
            }
        } else if (mainEnv->IsInstanceOf(data, jByteBufferCls)) {
            jbyteArray byteArray = (jbyteArray)mainEnv->CallObjectMethod(data, jByteBufferArray);
            if (!EXCEPTION_OCCURED(mainEnv)) {
                jbyte* raw = mainEnv->GetByteArrayElements(byteArray, NULL);
                if (raw) {
                    jsize nraw = mainEnv->GetArrayLength(byteArray);

                    gdk_property_change(requestor, property, target,
                            8, GDK_PROP_MODE_REPLACE, (guchar *) raw, nraw);

                    mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
                    is_data_set = TRUE;
                }
            }
        }
    }

    g_free(target_name);
    return is_data_set;
}

static void process_dnd_source_selection_req(DragSourceContext *ctx, GdkEvent *gdkEvent) {
    GdkEventSelection *event = &gdkEvent->selection;

#ifdef GLASS_GTK3
    GdkWindow *requestor = (event->requestor);
#else
    GdkWindow *requestor =
        gdk_x11_window_foreign_new_for_display(gdk_display_get_default(), event->requestor);
#endif

    gboolean is_data_set = FALSE;
    if (event->target == TARGET_UTF8_STRING_ATOM
            || event->target == TARGET_MIME_TEXT_PLAIN_ATOM) {
        is_data_set = dnd_source_set_utf8_string(ctx, requestor, event->property);
    } else if (event->target == TARGET_STRING_ATOM) {
        is_data_set = dnd_source_set_string(ctx, requestor, event->property);
//    } else if (event->target == TARGET_COMPOUND_TEXT_ATOM) { // XXX compound text
    } else if (target_is_image(event->target)) {
        is_data_set = dnd_source_set_image(ctx, requestor, event->property, event->target);
    } else if (event->target == TARGET_MIME_URI_LIST_ATOM) {
        is_data_set = dnd_source_set_uri_list(ctx, requestor, event->property);
    } else {
        is_data_set = dnd_source_set_raw(ctx, requestor, event->property, event->target);
    }

    gdk_selection_send_notify(event->requestor, event->selection, event->target,
                               (is_data_set) ? event->property : GDK_NONE, event->time);
}

static gboolean in_drag_end(gpointer data) {
    in_drag = false;
    return G_SOURCE_REMOVE;
}

//static void send_drop_finished(GdkWindow *window) {
//    GdkEvent* event = gdk_event_new(GDK_DROP_FINISHED);
//    event->any.window = g_object_ref(window);
//
//    gdk_display_put_event(gdk_display_get_default(), event);
//}

static void process_dnd_source_grab_broken(DragSourceContext *ctx, GdkEvent *event) {
    GdkEventGrabBroken *gb_event = &event->grab_broken;

    // grabbed the same window
    if (gb_event->implicit || gb_event->grab_window == ctx->dnd_window) {
        return;
    }

    gdk_drag_abort(ctx->dnd_ctx, GDK_CURRENT_TIME);
    gdk_threads_add_idle_full(G_PRIORITY_DEFAULT, in_drag_end, ctx, NULL);
//    send_drop_finished(ctx->dnd_window);
}

static void process_dnd_source_mouse_release(DragSourceContext *ctx, GdkEvent *event) {
    GdkDragAction selected = gdk_drag_context_get_selected_action(ctx->dnd_ctx);

    if (selected) {
        ctx->performed_action = translate_gdk_action_to_glass(selected);
        gdk_drag_drop(ctx->dnd_ctx, GDK_CURRENT_TIME);
    } else {
        gdk_drag_abort(ctx->dnd_ctx, GDK_CURRENT_TIME);
    }

    // the GDK_BUTTON_RELEASE will be put at the end of the queue bacause
    // WindowContext mouse release would stop DND before it's completed
    gdk_display_put_event(gdk_display_get_default(), event);
    gdk_threads_add_idle_full(G_PRIORITY_DEFAULT, in_drag_end, ctx, NULL);
//    send_drop_finished(ctx->dnd_window);
}

static void process_drag_motion(DragSourceContext *ctx, gint x_root, gint y_root, guint state) {
    GdkWindow *dest_window;
    GdkDragProtocol prot;

    if (ctx->drag_view) {
        ctx->drag_view->move(x_root, y_root);
    }

    gdk_drag_find_window_for_screen(ctx->dnd_ctx, NULL, gdk_screen_get_default(),
            x_root, y_root, &dest_window, &prot);

    if (prot != GDK_DRAG_PROTO_NONE) {
        GdkDragAction action, possible_actions;
        determine_actions(ctx, state, &action, &possible_actions);
        gdk_drag_motion(ctx->dnd_ctx, dest_window, prot, x_root, y_root,
                action, possible_actions, GDK_CURRENT_TIME);
    }
}

static void process_dnd_source_mouse_motion(DragSourceContext *ctx, GdkEvent *event) {
    GdkEventMotion *eventMotion = &event->motion;
    process_drag_motion(ctx, eventMotion->x_root, eventMotion->y_root, eventMotion->state);
}

static void process_dnd_source_key_press_release(DragSourceContext *ctx, GdkEvent *event) {
    GdkEventKey *eventKey = &event->key;

    if (eventKey->is_modifier) {
        guint state = eventKey->state;
        guint new_mod = 0;
        gint x,y;
        if (eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Control_L) ||
                eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Control_R)) {
            new_mod = GDK_CONTROL_MASK;
        } else if (eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Alt_L) ||
                eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Alt_R)) {
            new_mod = GDK_MOD1_MASK;
        } else if (eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Shift_L) ||
                eventKey->keyval == GLASS_GDK_KEY_CONSTANT(Shift_R)) {
            new_mod = GDK_SHIFT_MASK;
        }

        if (eventKey->type == GDK_KEY_PRESS) {
            state |= new_mod;
        } else {
            state ^= new_mod;
        }

        glass_gdk_master_pointer_get_position(&x, &y);
        process_drag_motion(ctx, x, y, state);
    }
}

static void process_dnd_source_drag_status(DragSourceContext *ctx, GdkEvent *event) {
    GdkEventDND *eventDnd = &event->dnd;
    GdkDragAction selected = gdk_drag_context_get_selected_action(eventDnd->context);
    GdkCursor* cursor;

    if (selected & GDK_ACTION_COPY) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-copy");
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "copy");
        }
    } else if (selected & (GDK_ACTION_MOVE | GDK_ACTION_PRIVATE)) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-move");

        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "pointer-move");
        }
    } else if (selected & GDK_ACTION_LINK) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-link");

        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "link");
        }

        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "alias");
        }
    } else {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-no-drop");

        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "no-drop");
        }
    }

    if (cursor == NULL) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-none");

        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "grabbing");
        }
    }

    gdk_pointer_grab(ctx->dnd_window, FALSE, GDK_MOUSE_EVENTS_MASK, NULL, cursor, GDK_CURRENT_TIME);
}

static void add_target_from_jstring(JNIEnv *env, GList **list, jstring string) {
    const char *gstring = env->GetStringUTFChars(string, NULL);
    if (g_strcmp0(gstring, "text/plain") == 0) {
        *list = g_list_append(*list, TARGET_UTF8_STRING_ATOM);
        *list = g_list_append(*list, TARGET_MIME_TEXT_PLAIN_ATOM);
        *list = g_list_append(*list, TARGET_STRING_ATOM);
        *list = g_list_append(*list, TARGET_COMPOUND_TEXT_ATOM);
    } else if (g_strcmp0(gstring, "application/x-java-rawimage") == 0) {
        *list = g_list_append(*list, TARGET_MIME_PNG_ATOM);
        *list = g_list_append(*list, TARGET_MIME_JPEG_ATOM);
        *list = g_list_append(*list, TARGET_MIME_TIFF_ATOM);
        *list = g_list_append(*list, TARGET_MIME_BMP_ATOM);
    } else if (g_strcmp0(gstring, "application/x-java-file-list") == 0) {
        *list = g_list_append(*list, TARGET_MIME_URI_LIST_ATOM);
    } else {
        *list = g_list_append(*list, gdk_atom_intern(gstring, FALSE));
    }
    env->ReleaseStringUTFChars(string, gstring);
}

static GList* data_to_targets(JNIEnv *env, jobject data) {
    jobject keys;
    jobject keysIterator;
    jstring next;

    GList *list = NULL;

    init_target_atoms();

    keys = env->CallObjectMethod(data, jMapKeySet, NULL);
    JNI_EXCEPTION_TO_CPP(env)
    keysIterator = env->CallObjectMethod(keys, jIterableIterator, NULL);
    JNI_EXCEPTION_TO_CPP(env)
    while (env->CallBooleanMethod(keysIterator, jIteratorHasNext) == JNI_TRUE) {
        next = (jstring)env->CallObjectMethod(keysIterator, jIteratorNext, NULL);
        JNI_EXCEPTION_TO_CPP(env)
        add_target_from_jstring(env, &list, next);
    }
    return list;
}

static void dnd_source_push_data(JNIEnv *env, jobject data, jint supported, DragSourceContext *ctx) {
    GList *targets;
    if (WindowContextBase:: sm_mouse_drag_window == NULL) {
        ERROR0("Drag not started on source window.");
        return;
    }

    in_drag = true;
    ctx->dnd_window = WindowContextBase:: sm_mouse_drag_window->get_gdk_window();

    if (supported == 0) {
        return; // No supported actions, do nothing
    }

    targets = data_to_targets(env, data);
    ctx->data = env->NewGlobalRef(data);

    GdkDragAction actions = translate_glass_action_to_gdk(supported);
    ctx->actions = actions;
    ctx->dnd_ctx = gdk_drag_begin(ctx->dnd_window, targets);
    if (!gdk_pointer_is_grabbed()) {
        // Grab will start on glass_window, unless disabled
        gdk_pointer_grab(ctx->dnd_window, FALSE, GDK_MOUSE_EVENTS_MASK, NULL, NULL, GDK_CURRENT_TIME);
    }

    g_object_set_data(G_OBJECT(ctx->dnd_window), SOURCE_DND_CONTEXT, ctx);
    g_list_free(targets);

    gboolean is_raw_image = FALSE;
    gint w = 0, h = 0;
    GdkPixbuf* pixbuf = DragView::get_drag_image(data, &is_raw_image, &w, &h);
    gint offset_x = -2;
    gint offset_y = -2;

    if (GDK_IS_PIXBUF(pixbuf)) {
        offset_x = w / 2;
        offset_y = h / 2;

        DragView::get_drag_image_offset(data, &offset_x, &offset_y);
    } else {
        GError *error = NULL;
        GtkIconTheme *icon_theme = gtk_icon_theme_get_default();
        gtk_icon_size_lookup(GTK_ICON_SIZE_DND, &w, &h);
        pixbuf = gtk_icon_theme_load_icon(icon_theme,
                                         "text-x-generic", w,
                                         GTK_ICON_LOOKUP_USE_BUILTIN,
                                         &error);
    }

    if (GDK_IS_PIXBUF(pixbuf)) {
        ctx->drag_view = new DragView(pixbuf, is_raw_image, offset_x, offset_y);
    }
}

//true = continue
bool process_dnd_source(GdkEvent *event) {
    if (!in_drag) {
        return true;
    }

    GdkWindow* window = event->any.window;

    if (!window) {
        return true;
    }

    DragView *drag_view = (DragView *) g_object_get_data(G_OBJECT(window), SOURCE_DND_DRAG_VIEW);
    if (drag_view != NULL && event->type == GDK_EXPOSE &&
        window == drag_view->get_window()) {
        drag_view->expose();
        return true;
    }

    DragSourceContext *ctx = (DragSourceContext*)
        g_object_get_data(G_OBJECT(window), SOURCE_DND_CONTEXT);

    if (ctx == NULL) {
        return true;
    }

    if (event->type == GDK_DELETE && window == ctx->dnd_window) {
        gdk_drag_abort(ctx->dnd_ctx, GDK_CURRENT_TIME);
        in_drag = false;
        return true;
    }

    if (window != ctx->dnd_window) {
        return true;
    }

    switch(event->type) {
        case GDK_GRAB_BROKEN:
            process_dnd_source_grab_broken(ctx, event);
            break;
        case GDK_MOTION_NOTIFY:
            process_dnd_source_mouse_motion(ctx, event);
            break;
        case GDK_BUTTON_RELEASE:
            process_dnd_source_mouse_release(ctx, event);
            return false;
        case GDK_KEY_PRESS:
        case GDK_KEY_RELEASE:
            process_dnd_source_key_press_release(ctx, event);
            break;
        case GDK_DRAG_ENTER:
            gdk_selection_owner_set(ctx->dnd_window, gdk_drag_get_selection(ctx->dnd_ctx),
                                    GDK_CURRENT_TIME, FALSE);
            break;
        case GDK_DRAG_STATUS:
            process_dnd_source_drag_status(ctx, event);
            break;
        case GDK_SELECTION_REQUEST:
            process_dnd_source_selection_req(ctx, event);
            break;
        default:
            break;
    }

    return true;
}

jint execute_dnd(JNIEnv *env, jobject data, jint supported) {
    DragSourceContext *ctx = new DragSourceContext();
    ctx->performed_action = com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_NONE;

    try {
        dnd_source_push_data(env, data, supported, ctx);
    } catch (jni_exception&) {
        goto end;
    }

    while (is_in_drag()) {
        gtk_main_iteration();
    }

    end:
    jint performed_action = ctx->performed_action;
    delete ctx;

    return performed_action;
}

/******************** DRAG VIEW **************************/

gboolean DragView::get_drag_image_offset(jobject obj, int* x, int* y) {
    gboolean offset_set = FALSE;
    jobject bb = dnd_source_get_data(obj, "application/x-java-drag-image-offset");
    if (bb) {
        jbyteArray byteArray = (jbyteArray)mainEnv->CallObjectMethod(bb, jByteBufferArray);
        if (!EXCEPTION_OCCURED(mainEnv)) {
            jbyte* raw = mainEnv->GetByteArrayElements(byteArray, NULL);
            jsize nraw = mainEnv->GetArrayLength(byteArray);

            if ((size_t) nraw >= sizeof(jint) * 2) {
                jint* r = (jint*) raw;
                *x = BSWAP_32(r[0]);
                *y = BSWAP_32(r[1]);
                offset_set = TRUE;
            }

            mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
        }
    }
    return offset_set;
}

static void pixbufDestroyNotifyFunc(guchar *pixels, gpointer) {
    if (pixels != NULL) {
        g_free(pixels);
    }
}

GdkPixbuf* DragView::get_drag_image(jobject obj, gboolean* is_raw_image, gint* width, gint* height) {
    GdkPixbuf *pixbuf = NULL;
    gboolean is_raw = FALSE;

    jobject drag_image = dnd_source_get_data(obj, "application/x-java-drag-image");

    if (drag_image) {
        jbyteArray byteArray = (jbyteArray) mainEnv->CallObjectMethod(drag_image, jByteBufferArray);
        if (!EXCEPTION_OCCURED(mainEnv)) {

            jbyte* raw = mainEnv->GetByteArrayElements(byteArray, NULL);
            jsize nraw = mainEnv->GetArrayLength(byteArray);

            int w = 0, h = 0;
            int whsz = sizeof(jint) * 2; // Pixels are stored right after two ints
                                         // in this byteArray: width and height
            if (nraw > whsz) {
                jint* int_raw = (jint*) raw;
                w = BSWAP_32(int_raw[0]);
                h = BSWAP_32(int_raw[1]);

                // We should have enough pixels for requested width and height
                if ((nraw - whsz) / 4 - w * h >= 0 ) {
                    guchar* data = (guchar*) g_try_malloc0(nraw - whsz);
                    if (data) {
                        memcpy(data, (raw + whsz), nraw - whsz);

                        if (is_raw_image) {
                            guchar* origdata = data;
                            data = (guchar*) convert_BGRA_to_RGBA((const int*) data, w * 4, h);
                            g_free(origdata);
                        }

                        pixbuf = gdk_pixbuf_new_from_data(data, GDK_COLORSPACE_RGB, TRUE, 8,
                                                          w, h, w * 4, pixbufDestroyNotifyFunc, NULL);
                    }
                }
            }
            mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
        }
    }

    if (!GDK_IS_PIXBUF(pixbuf)) {
        jobject pixels = dnd_source_get_data(obj, "application/x-java-rawimage");
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
        double rw = DRAG_IMAGE_MAX_WIDTH / (double)w;
        double rh =  DRAG_IMAGE_MAX_HEIGH / (double)h;
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

DragView::DragView(GdkPixbuf* _pixbuf, gboolean _is_raw_image,
                   gint _offset_x, gint _offset_y) :
        pixbuf(_pixbuf),
        is_raw_image(_is_raw_image),
        offset_x(_offset_x),
        offset_y(_offset_y) {
    width = gdk_pixbuf_get_width(pixbuf);
    height = gdk_pixbuf_get_height(pixbuf);

    GdkScreen* screen = gdk_screen_get_default();
    GdkWindowAttr attrs;

    attrs.width = width;
    attrs.height = height;
    attrs.wclass = GDK_INPUT_OUTPUT;
    attrs.window_type = GDK_WINDOW_TEMP;
    attrs.type_hint = GDK_WINDOW_TYPE_HINT_DND;
    attrs.visual = gdk_screen_get_rgba_visual(screen);
    attrs.event_mask = GDK_EXPOSURE_MASK;

    if (!attrs.visual) {
        attrs.visual = gdk_screen_get_system_visual(screen);
    }

    int mask = GDK_WA_X | GDK_WA_Y | GDK_WA_VISUAL | GDK_WA_TYPE_HINT;
    glass_gdk_master_pointer_get_position(&attrs.x, &attrs.y);

    attrs.x -= offset_x;
    attrs.y -= offset_y;

    window = gdk_window_new(gdk_screen_get_root_window(screen), &attrs, mask);
    g_object_set_data(G_OBJECT(window), SOURCE_DND_DRAG_VIEW, this);

#ifdef GLASS_GTK3
    gdk_window_set_opaque_region(window, NULL);
    GdkRGBA rgba = {0, 0, 0, 0};
    gdk_window_set_background_rgba(window, &rgba);
#else
    gdk_window_set_opacity(window, .7);
#endif
}

void DragView::expose() {
#ifdef GLASS_GTK3
    cairo_region_t *region = gdk_window_get_clip_region(window);
    gdk_window_begin_paint_region(window, region);
#endif

    cairo_t *cr = gdk_cairo_create(window);
    cairo_set_operator(cr, CAIRO_OPERATOR_SOURCE);
    gdk_cairo_set_source_pixbuf(cr, pixbuf, 0, 0);
    cairo_paint(cr);
    cairo_destroy(cr);

#ifdef GLASS_GTK3
    gdk_window_end_paint(window);
    cairo_region_destroy(region);
#endif
}

void DragView::move(gint x, gint y) {
    gdk_window_move(window, x - offset_x, y - offset_y);

    if (!gdk_window_is_visible(window)) {
        gdk_window_show(window);
        gdk_window_raise(window);
    }
}

GdkWindow* DragView::get_window() {
    return window;
}

DragView::~DragView() {
    if (window) {
        gdk_window_destroy(window);
        window = NULL;
    }

    if (pixbuf) {
        g_object_unref(pixbuf);
        pixbuf = NULL;
    }
}