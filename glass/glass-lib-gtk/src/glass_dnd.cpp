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
#include "glass_dnd.h"
#include "glass_gtkcompat.h"
#include "glass_general.h"
#include "glass_evloop.h"

#include "com_sun_glass_events_DndEvent.h"
#include "com_sun_glass_ui_gtk_GtkDnDClipboard.h"

#include <jni.h>
#include <cstring>

#include <gtk/gtk.h>
#include <gdk/gdkx.h>

/************************* COMMON *********************************************/
static jint translate_gdk_action_to_glass(GdkDragAction action)
{
    jint result = 0;
    result |= (action & GDK_ACTION_COPY)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_COPY : 0;
    result |= (action & GDK_ACTION_MOVE)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_MOVE : 0;
    result |= (action & GDK_ACTION_LINK)? com_sun_glass_ui_gtk_GtkDnDClipboard_ACTION_REFERENCE : 0;
    return result;
}

static GdkDragAction translate_glass_action_to_gdk(jint action)
{
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

static gboolean target_is_text(GdkAtom target)
{
    init_target_atoms();
    
    return (target == TARGET_UTF8_STRING_ATOM ||
            target == TARGET_STRING_ATOM ||
            target == TARGET_MIME_TEXT_PLAIN_ATOM/* ||
            target == TARGET_COMPOUND_TEXT_ATOM*/);
}

static gboolean target_is_uri(GdkAtom target)
{
    init_target_atoms();
    return target == TARGET_MIME_URI_LIST_ATOM;
}

static gboolean target_is_image(GdkAtom target)
{
    init_target_atoms();
    return (target == TARGET_MIME_PNG_ATOM ||
            target == TARGET_MIME_JPEG_ATOM ||
            target == TARGET_MIME_TIFF_ATOM || 
            target == TARGET_MIME_BMP_ATOM);
}

static void clear_global_ref(gpointer data)
{
    mainEnv->DeleteGlobalRef((jobject)data);
}

static void dnd_set_performed_action(jint performed_action);
static jint dnd_get_performed_action();

/************************* TARGET *********************************************/
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

static void process_dnd_target_drag_enter(WindowContext *ctx, GdkEventDND *event)
{
    reset_enter_ctx();
    enter_ctx.ctx = event->context;
    enter_ctx.just_entered = TRUE;
    gdk_window_get_origin(ctx->get_gdk_window(), &enter_ctx.dx, &enter_ctx.dy);
    is_dnd_owner = is_in_drag();
}

static void process_dnd_target_drag_motion(WindowContext *ctx, GdkEventDND *event)
{
    if (!enter_ctx.ctx) {
        gdk_drag_status(event->context, static_cast<GdkDragAction>(0), GDK_CURRENT_TIME);
        return; // Do not process motion events if no enter event was received
    }
    jmethodID method = enter_ctx.just_entered ? jViewNotifyDragEnter : jViewNotifyDragOver;
    GdkDragAction suggested = GLASS_GDK_DRAG_CONTEXT_GET_SUGGESTED_ACTION(event->context);
    GdkDragAction result = translate_glass_action_to_gdk(mainEnv->CallIntMethod(ctx->get_jview(), method,
            (jint)event->x_root - enter_ctx.dx, (jint)event->y_root - enter_ctx.dy,
            (jint)event->x_root, (jint)event->y_root,
            translate_gdk_action_to_glass(suggested)));
    if (result != suggested && result != GDK_ACTION_COPY) {
        result = static_cast<GdkDragAction>(0);
    }
    if (enter_ctx.just_entered) {
        enter_ctx.just_entered = FALSE;
    }
    gdk_drag_status(event->context, result, GDK_CURRENT_TIME);
}

static void process_dnd_target_drag_leave(WindowContext *ctx, GdkEventDND *event)
{
    mainEnv->CallVoidMethod(ctx->get_jview(), jViewNotifyDragLeave, NULL);
}

static void process_dnd_target_drop_start(WindowContext *ctx, GdkEventDND *event)
{
    if (!enter_ctx.ctx || enter_ctx.just_entered) {
        gdk_drop_finish(event->context, FALSE, GDK_CURRENT_TIME);
        gdk_drop_reply(event->context, FALSE, GDK_CURRENT_TIME);
        return; // Do not process drop events if no enter event and subsequent motion event were received
    }
    GdkDragAction selected = GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(event->context);

    mainEnv->CallIntMethod(ctx->get_jview(), jViewNotifyDragDrop,
            (jint)event->x_root - enter_ctx.dx, (jint)event->y_root - enter_ctx.dy,
            (jint)event->x_root, (jint)event->y_root,
            translate_gdk_action_to_glass(selected));

    gdk_drop_finish(event->context, TRUE, GDK_CURRENT_TIME);
    gdk_drop_reply(event->context, TRUE, GDK_CURRENT_TIME);
}

static gboolean check_state_in_drag(JNIEnv *env)
{
    if (!enter_ctx.ctx) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),
                "Cannot get supported actions. Drag pointer haven't entered the application window");
        return TRUE;
    }
    return FALSE;
}

// Events coming from application that are related to us being a DnD target
void process_dnd_target(WindowContext *ctx, GdkEventDND *event)
{
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

jobjectArray dnd_target_get_mimes(JNIEnv *env)
{
    if (check_state_in_drag(env)) {
        return NULL;
    }
    if (!enter_ctx.mimes) {
        GList* targets = GLASS_GDK_DRAG_CONTEXT_LIST_TARGETS(enter_ctx.ctx);
        jobject set = env->NewObject(jHashSetCls, jHashSetInit, NULL);
        
        while (targets) {
            GdkAtom target = GDK_POINTER_TO_ATOM(targets->data);
            gchar *name = gdk_atom_name(target);
            
            if (target_is_text(target)) {
                env->CallBooleanMethod(set, jSetAdd, env->NewStringUTF("text/plain"), NULL);
            }
            
            if (target_is_uri(target)) {
                //TODO may not contain files
                env->CallBooleanMethod(set, jSetAdd, env->NewStringUTF("application/x-java-file-list"), NULL);
            }
            
            if (target_is_image(target)) {
                env->CallBooleanMethod(set, jSetAdd, env->NewStringUTF("application/x-java-rawimage"), NULL);
            }
            
            env->CallBooleanMethod(set, jSetAdd, env->NewStringUTF(name), NULL);
            
            g_free(name);
            targets = targets->next;
        }
        enter_ctx.mimes = env->NewObjectArray(env->CallIntMethod(set, jSetSize, NULL),
                jStringCls, NULL);
        enter_ctx.mimes = (jobjectArray)env->CallObjectMethod(set, jSetToArray, enter_ctx.mimes, NULL);
        enter_ctx.mimes = (jobjectArray)env->NewGlobalRef(enter_ctx.mimes);
    }
    return enter_ctx.mimes;
}

jint dnd_target_get_supported_actions(JNIEnv *env)
{
    if (check_state_in_drag(env)) {
        return 0;
    }
    return translate_gdk_action_to_glass(GLASS_GDK_DRAG_CONTEXT_GET_ACTIONS(enter_ctx.ctx));
}

struct selection_data_ctx{
    gboolean received;
    guchar *data;
    GdkAtom type;
    gint format;
    gint length;
} ;

static void wait_for_selection_data_hook(GdkEvent * event, void * data)
{
    selection_data_ctx *ctx = (selection_data_ctx*)data;
    GdkWindow *dest = GLASS_GDK_DRAG_CONTEXT_GET_DEST_WINDOW(enter_ctx.ctx);
    if (event->type == GDK_SELECTION_NOTIFY &&
            event->selection.window == dest) {
        if (event->selection.property) { // if 0, that we received negative response
            ctx->length = gdk_selection_property_get(dest, &(ctx->data), &(ctx->type), &(ctx->format));
        }
        ctx->received = TRUE;
    }
}

static gboolean dnd_target_receive_data(JNIEnv *env, GdkAtom target, selection_data_ctx *selection_ctx)
{
    GevlHookRegistration hookReg;
    
    memset(selection_ctx, 0, sizeof(selection_data_ctx));

    gdk_selection_convert(GLASS_GDK_DRAG_CONTEXT_GET_DEST_WINDOW(enter_ctx.ctx), gdk_drag_get_selection(enter_ctx.ctx), target,
                          GDK_CURRENT_TIME);

    hookReg = 
            glass_evloop_hook_add(
                    (GevlHookFunction) wait_for_selection_data_hook, 
                    selection_ctx);
    if (HANDLE_MEM_ALLOC_ERROR(env, hookReg,
                               "Failed to allocate event hook")) {
        return TRUE;
    }

    do {
        gtk_main_iteration();
    } while (!(selection_ctx->received));


    glass_evloop_hook_remove(hookReg);
    return selection_ctx->data != NULL;
}

static jobject dnd_target_get_string(JNIEnv *env)
{
    jobject result = NULL;
    selection_data_ctx ctx;
    
    if (dnd_target_receive_data(env, TARGET_UTF8_STRING_ATOM, &ctx)) {
        result = env->NewStringUTF((char *)ctx.data);
    }
    if (!result && dnd_target_receive_data(env, TARGET_MIME_TEXT_PLAIN_ATOM, &ctx)) {
        result = env->NewStringUTF((char *)ctx.data);
    }
    // TODO find out how to convert from compound text
    // if (!result && dnd_target_receive_data(env, TARGET_COMPOUND_TEXT_ATOM, &ctx)) {
    // }
    if (!result && dnd_target_receive_data(env, TARGET_STRING_ATOM, &ctx)) {
        gchar *str;
        str = g_convert( (gchar *)ctx.data, -1, "UTF-8", "ISO-8859-1", NULL, NULL, NULL);
        if (str != NULL) {
            result = env->NewStringUTF(str);
            g_free(str);
        }
    }
    g_free(ctx.data);
    return result;
}

static jobject dnd_target_get_list(JNIEnv *env)
{
    gchar **strv, *path;
    jsize len, i;
    jobjectArray result = NULL;
    jstring str;
    selection_data_ctx ctx;

    if (dnd_target_receive_data(env, TARGET_MIME_URI_LIST_ATOM, &ctx)) {
        strv = g_uri_list_extract_uris((gchar *)ctx.data);
        len = g_strv_length(strv);
        result = (jobjectArray)env->NewObjectArray(len, jStringCls, NULL);

        for (i = 0; i < len; ++i) {
            path = strv[i];
            if (g_str_has_prefix(path, "file://")) {
                path += 7;
            }
            str = env->NewStringUTF(path);
            env->SetObjectArrayElement(result, i, str);
        }
        g_strfreev(strv);
    }
    g_free(ctx.data);
    return (jobject)result;
}

static jobject dnd_target_get_image(JNIEnv *env)
{
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
                env->SetByteArrayRegion(data_array, 0, stride*h, (jbyte*) data);

                buffer = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, data_array);
                result = env->NewObject(jGtkPixelsCls, jGtkPixelsInit, w, h, buffer);

                g_object_unref(buf);
                g_free(data); // data from convert_BGRA_to_RGBA
            }
            g_object_unref(stream);
        }
        ++cur_target;
    }
    return result;
}

static jobject dnd_target_get_raw(JNIEnv *env, GdkAtom target, gboolean string_data)
{
    selection_data_ctx ctx;
    jobject result = NULL;
    if (dnd_target_receive_data(env, target, &ctx)) {
        if (string_data) {
             result = env->NewStringUTF((char *)ctx.data);
        } else {
            jsize length = ctx.length * (ctx.format / 8);
            jbyteArray array = env->NewByteArray(length);
            env->SetByteArrayRegion(array, 0, length, (const jbyte*)ctx.data);
            result = env->CallStaticObjectMethod(jByteBufferCls, jByteBufferWrap, array);
        }
    }
    g_free(ctx.data);
    return result;
}

jobject dnd_target_get_data(JNIEnv *env, jstring mime)
{
    if (check_state_in_drag(env)) {
        return NULL;
    }
    const char *cmime = env->GetStringUTFChars(mime, NULL);
    jobject ret = NULL;
    
    init_target_atoms();

    if (g_strcmp0(cmime, "text/plain") == 0) {
        ret = dnd_target_get_string(env);
    } else if (g_str_has_prefix(cmime, "text/")) {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), TRUE);
    } else if (g_strcmp0(cmime, "application/x-java-file-list") == 0) {
        ret = dnd_target_get_list(env);
    } else if (g_strcmp0(cmime, "application/x-java-rawimage") == 0 ) {
        ret = dnd_target_get_image(env);
    } else {
        ret = dnd_target_get_raw(env, gdk_atom_intern(cmime, FALSE), FALSE);
    }

    env->ReleaseStringUTFChars(mime, cmime);

    return ret;
}

/************************* SOURCE *********************************************/


static GdkWindow *dnd_window = NULL;
static jint dnd_performed_action;

const char * const SOURCE_DND_CONTEXT = "fx-dnd-context";
const char * const SOURCE_DND_DATA = "fx-dnd-data";
const char * const SOURCE_DND_ACTIONS = "fx-dnd-actions";

static GdkWindow* get_dnd_window()
{
    if (dnd_window == NULL) {
        GdkWindowAttr attr;
        memset(&attr, 0, sizeof (GdkWindowAttr));
        attr.override_redirect = TRUE;
        attr.window_type = GDK_WINDOW_TEMP;
        attr.type_hint = GDK_WINDOW_TYPE_HINT_UTILITY;
        attr.wclass = GDK_INPUT_OUTPUT;
        attr.event_mask = GDK_ALL_EVENTS_MASK;
        dnd_window = gdk_window_new(NULL, &attr, GDK_WA_NOREDIR | GDK_WA_TYPE_HINT);
        
        gdk_window_move(dnd_window, -100, -100);
        gdk_window_resize(dnd_window, 1, 1);
        gdk_window_show(dnd_window);
    }
    return dnd_window;
}

static void dnd_set_performed_action(jint performed_action) {
    dnd_performed_action = performed_action;
}

static jint dnd_get_performed_action() {
    return dnd_performed_action;
}

static void dnd_pointer_grab(GdkCursor *cursor)
{
    glass_gdk_master_pointer_grab(dnd_window, cursor);
}

static GdkDragContext *get_drag_context() {
    GdkDragContext *ctx;
    ctx = (GdkDragContext*)g_object_get_data(G_OBJECT(dnd_window), SOURCE_DND_CONTEXT);
    return ctx;
}

static gboolean dnd_finish_callback() {
    if (dnd_window) {
        dnd_set_performed_action(
                translate_gdk_action_to_glass(
                    GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(
                        get_drag_context())));

        gdk_window_destroy(dnd_window);
        dnd_window = NULL;
    }
    
    return FALSE;
}

gboolean is_in_drag()
{
    return dnd_window != NULL;
}

static void determine_actions(guint state, GdkDragAction *action, GdkDragAction *possible_actions)
{
    GdkDragAction suggested = static_cast<GdkDragAction>(GPOINTER_TO_INT(g_object_get_data(G_OBJECT(dnd_window), SOURCE_DND_ACTIONS)));
    
    if (state & (GDK_SHIFT_MASK | GDK_CONTROL_MASK)) {
        if ((state & GDK_CONTROL_MASK) && (state & GDK_SHIFT_MASK) && (suggested & GDK_ACTION_LINK)) {
            *action = *possible_actions = GDK_ACTION_LINK;
            return;
        } else if ((state & GDK_SHIFT_MASK) && (suggested & GDK_ACTION_MOVE)) {
            *action = *possible_actions = GDK_ACTION_MOVE;
            return;
        } else if (suggested & GDK_ACTION_COPY){
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

static jobject dnd_source_get_data(const char *key)
{
    jobject data = (jobject)g_object_get_data(G_OBJECT(dnd_window), SOURCE_DND_DATA);
    jstring string = mainEnv->NewStringUTF(key);
    
    return mainEnv->CallObjectMethod(data, jMapGet, string, NULL);
    
}

static gboolean dnd_source_set_utf8_string(GdkWindow *requestor, GdkAtom property)
{
    jstring string = (jstring)dnd_source_get_data("text/plain");
    if (!string) {
        return FALSE;
    }

    const char *cstring = mainEnv->GetStringUTFChars(string, NULL);
    gint size = strlen(cstring);
    
    gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
            8, GDK_PROP_MODE_REPLACE, (guchar *)cstring, size);
    
    mainEnv->ReleaseStringUTFChars(string, cstring);
    return TRUE;
}

static gboolean dnd_source_set_string(GdkWindow *requestor, GdkAtom property)
{
    jstring string = (jstring)dnd_source_get_data("text/plain");
    if (!string) {
        return FALSE;
    }
    
    gboolean is_data_set = FALSE;
    const char *cstring = mainEnv->GetStringUTFChars(string, NULL);
    gchar *res_str = g_convert((gchar *)cstring, -1, "ISO-8859-1", "UTF-8", NULL, NULL, NULL);
    
    if (res_str) {
        gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                8, GDK_PROP_MODE_REPLACE, (guchar *)res_str, strlen(res_str));
        g_free(res_str);
        is_data_set = TRUE;
    }
    
    mainEnv->ReleaseStringUTFChars(string, cstring);
    return is_data_set;
}

static gboolean dnd_source_set_image(GdkWindow *requestor, GdkAtom property, GdkAtom target)
{
    jobject pixels = dnd_source_get_data("application/x-java-rawimage");
    if (!pixels) {
        return FALSE;
    }
    
    gchar *buffer;
    gsize size;
    const char * type;
    GdkPixbuf *pixbuf;

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
    
    if (gdk_pixbuf_save_to_buffer(pixbuf, &buffer, &size, type, NULL, NULL)) {
        gdk_property_change(requestor, property, target,
                8, GDK_PROP_MODE_REPLACE, (guchar *)buffer, size);
    } else {
        return FALSE;
    }
    return TRUE;
}

#define FILE_PREFIX "file://"
#define FILE_PREFIX_N 7

static void dnd_source_set_uri_file_list(GdkWindow *requestor, GdkAtom property, jobjectArray array)
{
    jsize ndata = mainEnv->GetArrayLength(array);
    jsize i;
    jsize string_size;
    jstring string;
    gchar *data, *data_ptr;
    gint data_size = 0;
    for (i = 0; i < ndata; ++i) {
        string = (jstring)mainEnv->GetObjectArrayElement(array, i);
        data_size += mainEnv->GetStringUTFLength(string) + FILE_PREFIX_N + 1;
    }
    
    data_ptr = data = new gchar[data_size];
    
    for (i = 0; i < ndata; ++i) {
        string = (jstring)mainEnv->GetObjectArrayElement(array, i);
        string_size = mainEnv->GetStringUTFLength(string);
        
        g_strlcpy(data_ptr, FILE_PREFIX, FILE_PREFIX_N + 1);
        mainEnv->GetStringUTFRegion(string, 0, string_size, data_ptr + FILE_PREFIX_N);
        *(data_ptr += FILE_PREFIX_N + string_size) = '\n';
        ++data_ptr;
    }
    if (ndata > 0) {
        *(data_ptr - 1) = 0;
    }
    
    gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                8, GDK_PROP_MODE_REPLACE, (guchar *) data, data_size);
    
    delete[] data;
    
}

static gboolean dnd_source_set_uri_list(GdkWindow *requestor, GdkAtom property)
{
    jobject data;
    gboolean is_data_set = FALSE;
    if (data = dnd_source_get_data("text/uri-list")) {
        const char *cstring = mainEnv->GetStringUTFChars((jstring)data, NULL);
        gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                8, GDK_PROP_MODE_REPLACE, (guchar *) cstring, strlen(cstring));
        
        mainEnv->ReleaseStringUTFChars((jstring)data, cstring);
        is_data_set = TRUE;
    } else if (data = dnd_source_get_data("application/x-java-file-list")) {
        dnd_source_set_uri_file_list(requestor, property, (jobjectArray)data);
        is_data_set = TRUE;
    }

    return is_data_set;
}

static gboolean dnd_source_set_raw(GdkWindow *requestor, GdkAtom property, GdkAtom target)
{
    gchar *target_name = gdk_atom_name(target);
    jobject data = dnd_source_get_data(target_name);
    gboolean is_data_set = FALSE;
    if (data) {
        if (mainEnv->IsInstanceOf(data, jStringCls)) {
            const char *cstring = mainEnv->GetStringUTFChars((jstring)data, NULL);

            gdk_property_change(requestor, property, GDK_SELECTION_TYPE_STRING,
                    8, GDK_PROP_MODE_REPLACE, (guchar *) cstring, strlen(cstring));
        
            mainEnv->ReleaseStringUTFChars((jstring)data, cstring);
            is_data_set = TRUE;
        } else if (mainEnv->IsInstanceOf(data, jByteBufferCls)) {
            jbyteArray byteArray = (jbyteArray)mainEnv->CallObjectMethod(data, jByteBufferArray);
            jbyte* raw = mainEnv->GetByteArrayElements(byteArray, NULL);
            jsize nraw = mainEnv->GetArrayLength(byteArray);
    
            gdk_property_change(requestor, property, target,
                    8, GDK_PROP_MODE_REPLACE, (guchar *) raw, nraw);
            
            mainEnv->ReleaseByteArrayElements(byteArray, raw, JNI_ABORT);
            is_data_set = TRUE;
        }
    }
    
    g_free(target_name);
    return is_data_set;
}

static void process_dnd_source_selection_req(GdkWindow *window, GdkEventSelection* event)
{ 
    GdkWindow *requestor = GLASS_GDK_SELECTION_EVENT_GET_REQUESTOR(event);

    gboolean is_data_set = FALSE;
    if (event->target == TARGET_UTF8_STRING_ATOM
            || event->target == TARGET_MIME_TEXT_PLAIN_ATOM) {
        is_data_set = dnd_source_set_utf8_string(requestor, event->property);
    } else if (event->target == TARGET_STRING_ATOM) {
        is_data_set = dnd_source_set_string(requestor, event->property);
//    } else if (event->target == TARGET_COMPOUND_TEXT_ATOM) { // XXX compound text
    } else if (target_is_image(event->target)) {
        is_data_set = dnd_source_set_image(requestor, event->property, event->target);
    } else if (event->target == TARGET_MIME_URI_LIST_ATOM) {
        is_data_set = dnd_source_set_uri_list(requestor, event->property);
    } else {
        is_data_set = dnd_source_set_raw(requestor, event->property, event->target);
    }

    gdk_selection_send_notify(event->requestor, event->selection, event->target, 
                               (is_data_set) ? event->property : GDK_NONE, event->time);
}

static void process_dnd_source_mouse_release(GdkWindow *window, GdkEventButton *event) {
    glass_gdk_master_pointer_ungrab();
    
    if (GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(get_drag_context())) {
        gdk_drag_drop(get_drag_context(), GDK_CURRENT_TIME);
    } else {
        gdk_drag_abort(get_drag_context(), GDK_CURRENT_TIME);
        /* let the gdk_drag_abort messages handled before finish */
        gdk_threads_add_idle((GSourceFunc) dnd_finish_callback, NULL);
    }
}

static void process_drag_motion(gint x_root, gint y_root, guint state)
{
    GdkWindow *dest_window;
    GdkDragProtocol prot;

    gdk_drag_find_window_for_screen(get_drag_context(), NULL, gdk_screen_get_default(),
            x_root, y_root, &dest_window, &prot);
    
    if (prot != GDK_DRAG_PROTO_NONE) {
        GdkDragAction action, possible_actions;
        determine_actions(state, &action, &possible_actions);
        gdk_drag_motion(get_drag_context(), dest_window, prot, x_root, y_root,
                action, possible_actions, GDK_CURRENT_TIME);
    }
}

static void process_dnd_source_mouse_motion(GdkWindow *window, GdkEventMotion *event)
{
    process_drag_motion(event->x_root, event->y_root, event->state);
}

static void process_dnd_source_key_press_release(GdkWindow *window, GdkEventKey *event)
{
    if (event->is_modifier) {
        guint state = event->state;
        guint new_mod = 0;
        gint x,y;
        if (event->keyval == GLASS_GDK_KEY_CONSTANT(Control_L) ||
                event->keyval == GLASS_GDK_KEY_CONSTANT(Control_R)) {
            new_mod = GDK_CONTROL_MASK;
        } else if (event->keyval == GLASS_GDK_KEY_CONSTANT(Alt_L) ||
                event->keyval == GLASS_GDK_KEY_CONSTANT(Alt_R)) {
            new_mod = GDK_MOD1_MASK;
        } else if (event->keyval == GLASS_GDK_KEY_CONSTANT(Shift_L) ||
                event->keyval == GLASS_GDK_KEY_CONSTANT(Shift_R)) {
            new_mod = GDK_SHIFT_MASK;
        }
        
        if (event->type == GDK_KEY_PRESS) {
            state |= new_mod;
        } else {
            state ^= new_mod;
        }
        
        glass_gdk_master_pointer_get_position(&x, &y);
        process_drag_motion(x, y, state);
        
    }
}

static void process_dnd_source_drag_status(GdkWindow *window, GdkEventDND *event)
{
    GdkDragAction selected = GLASS_GDK_DRAG_CONTEXT_GET_SELECTED_ACTION(event->context);
    GdkCursor* cursor;
    
    if (selected & GDK_ACTION_COPY) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-copy");
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "copy");
        }
    } else if (selected & (GDK_ACTION_MOVE | GDK_ACTION_PRIVATE)) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-move");
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "move");
        }
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "closedhand");
        }
    } else if (selected & GDK_ACTION_LINK) {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-link");
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "link");
        }
    } else {
        cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-no-drop");
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "no-drop");
        }
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "not-allowed");
        }
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "forbidden");
        }
        if (cursor == NULL) {
            cursor = gdk_cursor_new_from_name(gdk_display_get_default(), "dnd-none");
        }
    }
    if (cursor == NULL) {
        cursor = gdk_cursor_new(GDK_LEFT_PTR);
    }
    
    dnd_pointer_grab(cursor);
}

static void process_dnd_source_drop_finished(GdkWindow *window, GdkEventDND *event)
{
    gdk_threads_add_idle((GSourceFunc) dnd_finish_callback, NULL);
}

void process_dnd_source(GdkWindow *window, GdkEvent *event) {
    switch(event->type) {
        case GDK_MOTION_NOTIFY:
            process_dnd_source_mouse_motion(window, &event->motion);
            break;
        case GDK_BUTTON_RELEASE:
            process_dnd_source_mouse_release(window, &event->button);
            break;
        case GDK_DRAG_STATUS:
            process_dnd_source_drag_status(window, &event->dnd);
            break;
        case GDK_DROP_FINISHED:
            process_dnd_source_drop_finished(window, &event->dnd);
            break;
        case GDK_KEY_PRESS:
        case GDK_KEY_RELEASE:
            process_dnd_source_key_press_release(window, &event->key);
            break;
        case GDK_DRAG_ENTER:
            gdk_selection_owner_set(dnd_window, gdk_drag_get_selection(get_drag_context()), GDK_CURRENT_TIME, FALSE);
            break;
        case GDK_SELECTION_REQUEST:
            process_dnd_source_selection_req(window, &event->selection);
            break;
        default:
            break;
    }
}

static void add_target_from_jstring(JNIEnv *env, GList **list, jstring string)
{
    const char *gstring = env->GetStringUTFChars(string, NULL);
    if (g_strcmp0(gstring, "text/plain") == 0) {
        *list = g_list_append(*list, TARGET_UTF8_STRING_ATOM);
        *list = g_list_append(*list, TARGET_MIME_TEXT_PLAIN_ATOM);
        *list = g_list_append(*list, TARGET_STRING_ATOM);
        //*list = g_list_append(list, TARGET_COMPOUND_TEXT_ATOM);
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

static GList* data_to_targets(JNIEnv *env, jobject data)
{
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

static void dnd_source_push_data(JNIEnv *env, jobject data, jint supported)
{
    GdkWindow *src_window = get_dnd_window();
    GList *targets;
    GdkDragContext *ctx;
    
    if (supported == 0) {
        return; // No supported actions, do nothing
    }
    
    targets = data_to_targets(env, data);
    
    data = env->NewGlobalRef(data);
    
    g_object_set_data_full(G_OBJECT(src_window), SOURCE_DND_DATA, data, clear_global_ref);
    g_object_set_data(G_OBJECT(src_window), SOURCE_DND_ACTIONS, (gpointer)translate_glass_action_to_gdk(supported));
    
    ctx = gdk_drag_begin(src_window, targets);
    
    g_list_free(targets);
    
    g_object_set_data(G_OBJECT(src_window), SOURCE_DND_CONTEXT, ctx);
    
    dnd_pointer_grab(NULL);

    is_dnd_owner = TRUE;
}

jint execute_dnd(JNIEnv *env, jobject data, jint supported) {
    try {
        dnd_source_push_data(env, data, supported);
    } catch (jni_exception&) {
        return 0;
    }

    while (is_in_drag()) {
        gtk_main_iteration();
    }

    return dnd_get_performed_action();
}

