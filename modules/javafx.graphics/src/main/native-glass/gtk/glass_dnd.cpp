/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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
#include "com_sun_glass_ui_Clipboard.h"

#include <cstring>

#include <gtk/gtk.h>
#include <gdk/gdkx.h>
#include <gdk/gdkkeysyms.h>

/************************* COMMON *********************************************/
static int32_t translate_gdk_action_to_glass(GdkDragAction action)
{
    int32_t result = 0;
    result |= (action & GDK_ACTION_COPY)? com_sun_glass_ui_Clipboard_ACTION_COPY : 0;
    result |= (action & GDK_ACTION_MOVE)? com_sun_glass_ui_Clipboard_ACTION_MOVE : 0;
    result |= (action & GDK_ACTION_LINK)? com_sun_glass_ui_Clipboard_ACTION_REFERENCE : 0;
    return result;
}

static GdkDragAction translate_glass_action_to_gdk(int32_t action)
{
    int result = 0;
    result |= (action & com_sun_glass_ui_Clipboard_ACTION_COPY)? GDK_ACTION_COPY : 0;
    result |= (action & com_sun_glass_ui_Clipboard_ACTION_MOVE)? GDK_ACTION_MOVE : 0;
    result |= (action & com_sun_glass_ui_Clipboard_ACTION_REFERENCE)? GDK_ACTION_LINK : 0;
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

static void dnd_set_performed_action(int32_t performed_action);
static int32_t dnd_get_performed_action();

/************************* TARGET *********************************************/
struct selection_data_ctx {
    gboolean received;
    guchar *data;
    GdkAtom type;
    gint format;
    gint length;
};

static gboolean dnd_target_receive_data(GdkAtom target, selection_data_ctx *selection_ctx, int32_t *oom_reports);

static struct {
    GdkDragContext *ctx;
    gboolean just_entered;
    gboolean dropped;
    gint dx, dy;
    // The mimes of this drag as ggtk_dnd_target_get_mimes answers them (NULL until the first call): every
    // string the JNI added to its HashSet, in order, NUL-terminated back to back
    GString *mime_adds;
    int32_t mime_add_count;
    int64_t mimes_generation;
} enter_ctx = {NULL, FALSE, FALSE, 0, 0, NULL, 0, 0};

// Numbers the mime lists ggtk_dnd_target_get_mimes computes
static int64_t mimes_generation_counter = 0;

gboolean is_dnd_owner = FALSE;

GtkWidget *drag_widget = NULL;

gboolean is_in_drag() {
    return drag_widget != NULL;
}

static void reset_enter_ctx() {
    if (enter_ctx.mime_adds != NULL) {
        g_string_free(enter_ctx.mime_adds, TRUE);
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
    auto notify_drag = enter_ctx.just_entered ? glass_dnd_cb.notify_drag_enter : glass_dnd_cb.notify_drag_over;
    // A NULL slot makes no call: action stays 0 and the drag is answered with no action
    int32_t action = 0;
    if (notify_drag) {
        // jview is not checked here: without a view the id is 0 (see glass_gtk_api.h, IDENTITY)
        GdkDragAction suggested = gdk_drag_context_get_suggested_action(event->context);
        if (notify_drag(ctx->get_view_id(),
                (int32_t)event->x_root - enter_ctx.dx, (int32_t)event->y_root - enter_ctx.dy,
                (int32_t)event->x_root, (int32_t)event->y_root,
                translate_gdk_action_to_glass(suggested), &action)) {
            return;
        }
    }
    GdkDragAction result = translate_glass_action_to_gdk(action);

    if (enter_ctx.just_entered) {
        enter_ctx.just_entered = FALSE;
    }
    gdk_drag_status(event->context, result, GDK_CURRENT_TIME);
}

static void process_dnd_target_drag_leave(WindowContext *ctx, GdkEventDND *event)
{
    (void)event;

    // if there is a drop going on do not report drag leave to java because
    // it will reset the drag gesture.
    if (!enter_ctx.dropped) {
        if (glass_dnd_cb.notify_drag_leave) {
            // CHECK_JNI_EXCEPTION site at the end of the function: nothing follows, the status is ignored
            glass_dnd_cb.notify_drag_leave(ctx->get_view_id());
        }
    }
}

static void process_dnd_target_drop_start(WindowContext *ctx, GdkEventDND *event)
{
    // Do not process drop events if no enter event and subsequent motion event were received
    gboolean accepted = enter_ctx.ctx != NULL && !enter_ctx.just_entered;
    if (accepted) {
        GdkDragAction selected = gdk_drag_context_get_selected_action(event->context);
        enter_ctx.dropped = TRUE;
        if (glass_dnd_cb.notify_drag_drop) {
            // LOG_EXCEPTION site: the status is ignored
            glass_dnd_cb.notify_drag_drop(ctx->get_view_id(),
                    (int32_t)event->x_root - enter_ctx.dx, (int32_t)event->y_root - enter_ctx.dy,
                    (int32_t)event->x_root, (int32_t)event->y_root,
                    translate_gdk_action_to_glass(selected));
        }
    }

    gdk_drop_finish(event->context, accepted, GDK_CURRENT_TIME);
    gdk_drop_reply(event->context, accepted, GDK_CURRENT_TIME);

    // The drag ends here, accepted or rejected. Commit 033187ad90 left enter_ctx.ctx set, so the exports
    // guarded by it - ggtk_dnd_target_get_mimes, ggtk_dnd_target_get_data and
    // ggtk_dnd_target_get_supported_actions, the check_state_in_drag of that commit - still passed the in-drag
    // check after the drop and worked on a GdkDragContext GDK may already have released. This single exit
    // clears it at the end of every drop: after the notify_drag_drop upcall, which runs a nested main loop in
    // which the target legitimately reads mimes and data, and at the very end of the function so the drop is
    // finished first. Only the drop's own context is cleared: a second drag whose GDK_DRAG_ENTER is dispatched
    // inside that nested main loop must keep the context that enter installed. enter_ctx.dropped keeps its
    // value, as at that commit, so that a GDK_DRAG_LEAVE arriving after the drop still suppresses the
    // drag-leave upcall (process_dnd_target_drag_leave). A GDK_DRAG_LEAVE without a drop deliberately leaves
    // the context set, as at that commit: the drag is still alive while it continues in other windows, and
    // clearing there would turn reads after a leave into errors (the context itself may already be released
    // then - a carried defect of that commit, kept for behaviour-neutrality).
    if (enter_ctx.ctx == event->context) {
        enter_ctx.ctx = NULL;
    }
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

// Appends one mime to the list the JNI built its HashSet from (the argument of one Set.add call)
static void dnd_target_add_mime(GString *adds, int32_t *count, const gchar *mime)
{
    g_string_append_len(adds, mime, (gssize) strlen(mime) + 1);
    (*count)++;
}

// The walk of dnd_target_get_mimes at commit 033187ad90 over the targets of the drag, with the arguments of its
// Set.add calls collected in order (repeats included) instead of added to a HashSet. Answers them in a GString.
static GString* dnd_target_collect_mimes(int32_t *count, int32_t *oom_reports)
{
    GList* targets = gdk_drag_context_list_targets(enter_ctx.ctx);
    GString* adds = g_string_new(NULL);
    *count = 0;

    while (targets) {
        GdkAtom target = GDK_POINTER_TO_ATOM(targets->data);
        gchar *name = gdk_atom_name(target);

        if (target_is_text(target)) {
            dnd_target_add_mime(adds, count, "text/plain");
        }

        if (target_is_image(target)) {
            dnd_target_add_mime(adds, count, "application/x-java-rawimage");
        }

        if (target_is_uri(target)) {
            selection_data_ctx ctx;
            if (dnd_target_receive_data(TARGET_MIME_URI_LIST_ATOM, &ctx, oom_reports)) {
                gchar** uris = g_uri_list_extract_uris((gchar *) ctx.data);
                guint size = g_strv_length(uris);
                guint files_cnt = get_files_count(uris);
                if (files_cnt) {
                    dnd_target_add_mime(adds, count, "application/x-java-file-list");
                }
                if (size - files_cnt) {
                    dnd_target_add_mime(adds, count, "text/uri-list");
                }
                g_strfreev(uris);
            }
            g_free(ctx.data);
        } else {
            dnd_target_add_mime(adds, count, name);
        }

        g_free(name);
        targets = targets->next;
    }
    return adds;
}

extern "C" int32_t ggtk_dnd_target_get_mimes(char** out_strings, int32_t* out_count, int64_t* out_generation)
{
    *out_strings = NULL;
    *out_count = 0;
    *out_generation = 0;
    if (!enter_ctx.ctx) { // check_state_in_drag of commit 033187ad90
        return GGTK_ERR_NOT_IN_DRAG;
    }
    int32_t oom_reports = 0;
    if (!enter_ctx.mime_adds) {
        int32_t count = 0;
        GString *adds = dnd_target_collect_mimes(&count, &oom_reports);
        // The walk may have run nested main loops; store into the drag state as it is now, as the JNI stored
        // its String[] into enter_ctx after the walk.
        if (enter_ctx.mime_adds != NULL) {
            g_string_free(enter_ctx.mime_adds, TRUE);
        }
        enter_ctx.mime_adds = adds;
        enter_ctx.mime_add_count = count;
        enter_ctx.mimes_generation = ++mimes_generation_counter;
    }
    // At least one byte, so that the block is never NULL
    gsize len = enter_ctx.mime_adds->len;
    *out_strings = (char *) g_malloc(len > 0 ? len : 1);
    memcpy(*out_strings, enter_ctx.mime_adds->str, len);
    *out_count = enter_ctx.mime_add_count;
    *out_generation = enter_ctx.mimes_generation;
    return oom_reports;
}

extern "C" int32_t ggtk_dnd_target_get_supported_actions(int32_t* out_actions)
{
    *out_actions = 0;
    if (!enter_ctx.ctx) { // check_state_in_drag of commit 033187ad90
        return GGTK_ERR_NOT_IN_DRAG;
    }
    *out_actions = translate_gdk_action_to_glass(gdk_drag_context_get_actions(enter_ctx.ctx));
    return GGTK_OK;
}

static void wait_for_selection_data_hook(GdkEvent * event, void * data)
{
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

// *oom_reports counts the OutOfMemoryError the JNI reported (HANDLE_MEM_ALLOC_ERROR) when the hook could not be
// allocated; the caller reports them.
static gboolean dnd_target_receive_data(GdkAtom target, selection_data_ctx *selection_ctx, int32_t *oom_reports)
{
    GevlHookRegistration hookReg;

    memset(selection_ctx, 0, sizeof(selection_data_ctx));

    gdk_selection_convert(glass_gdk_drag_context_get_dest_window(enter_ctx.ctx), gdk_drag_get_selection(enter_ctx.ctx), target,
                          GDK_CURRENT_TIME);

    hookReg =
            glass_evloop_hook_add(
                    (GevlHookFunction) wait_for_selection_data_hook,
                    selection_ctx);
    if (hookReg == NULL) {
        // The hook is what fills selection_ctx, so nothing was received. Commit 033187ad90 answered TRUE
        // here (HANDLE_MEM_ALLOC_ERROR, then return TRUE), which told every caller the data had arrived and
        // left them decoding the zeroed selection_data_ctx; answer FALSE, as the normal exit below does when
        // the conversion produced no data. The OutOfMemoryError that commit reported for the failed
        // allocation still reaches Java through *oom_reports.
        (*oom_reports)++;
        return FALSE;
    }

    do {
        gtk_main_iteration();
    } while (!(selection_ctx->received));


    glass_evloop_hook_remove(hookReg);
    return selection_ctx->data != NULL;
}

// A string value: the bytes the JNI handed to NewStringUTF, which `data` (a g_malloc'd block) now owns
static void dnd_target_set_string(GgtkDndTargetValue *out, gchar *data)
{
    out->kind = GGTK_DND_VALUE_STRING;
    out->count = (int32_t) strlen(data);
    out->data = data;
}

static void dnd_target_get_string(GgtkDndTargetValue *out, int32_t *oom_reports)
{
    selection_data_ctx ctx;

    if (dnd_target_receive_data(TARGET_UTF8_STRING_ATOM, &ctx, oom_reports)) {
        if (ctx.data != NULL) { // NewStringUTF((char *) NULL) answered null
            dnd_target_set_string(out, (gchar *) ctx.data);
            ctx.data = NULL;
        }
        g_free(ctx.data);
    }
    if (out->kind == GGTK_DND_VALUE_NONE && dnd_target_receive_data(TARGET_MIME_TEXT_PLAIN_ATOM, &ctx, oom_reports)) {
        if (ctx.data != NULL) {
            dnd_target_set_string(out, (gchar *) ctx.data);
            ctx.data = NULL;
        }
        g_free(ctx.data);
    }
    // TODO find out how to convert from compound text
    // if (!result && dnd_target_receive_data(env, TARGET_COMPOUND_TEXT_ATOM, &ctx)) {
    // }
    if (out->kind == GGTK_DND_VALUE_NONE && dnd_target_receive_data(TARGET_STRING_ATOM, &ctx, oom_reports)) {
        gchar *str;
        str = g_convert( (gchar *)ctx.data, -1, "UTF-8", "ISO-8859-1", NULL, NULL, NULL);
        if (str != NULL) {
            dnd_target_set_string(out, str);
        }
        g_free(ctx.data);
    }
}

// A file-list record of GGTK_DND_VALUE_FILES (see glass_gtk_api.h): index, byte length (-1 for a NULL path),
// the bytes and their NUL, zero padding to a multiple of 4
static void dnd_target_add_file_record(GByteArray *records, int32_t index, const gchar *path)
{
    static const guint8 padding[4] = {0, 0, 0, 0};
    int32_t len = (path != NULL) ? (int32_t) strlen(path) : -1;

    g_byte_array_append(records, (const guint8 *) &index, sizeof(index));
    g_byte_array_append(records, (const guint8 *) &len, sizeof(len));
    if (path != NULL) {
        g_byte_array_append(records, (const guint8 *) path, (guint) len + 1);
        guint rest = ((guint) len + 1) % 4;
        if (rest != 0) {
            g_byte_array_append(records, padding, 4 - rest);
        }
    }
}

// glass_general.cpp uris_to_java at commit 033187ad90 without its Java objects (uris is freed here, as there)
static void dnd_target_uris_to_value(gchar **uris, gboolean files, GgtkDndTargetValue *out)
{
    if (uris == NULL) {
        return;
    }

    guint size = g_strv_length(uris);
    guint files_cnt = get_files_count(uris);

    if (files) {
        if (files_cnt) {
            // new String[files_cnt]: the file uris in order, the n-th of them at index n. Commit
            // 033187ad90 passed the position in the whole uri list as the index instead, so a file uri
            // that followed a non-file one was stored past the end of the array and left a null behind.
            GByteArray *records = g_byte_array_new();
            int32_t record_count = 0;

            for (gsize i = 0; i < size; ++i) {
                if (g_str_has_prefix(uris[i], FILE_PREFIX)) {
                    gchar* path = g_filename_from_uri(uris[i], NULL, NULL);
                    dnd_target_add_file_record(records, record_count, path);
                    record_count++;
                    g_free(path);
                }
            }
            out->kind = GGTK_DND_VALUE_FILES;
            out->count = (int32_t) files_cnt;
            out->records = record_count;
            out->data = g_byte_array_free(records, FALSE);
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

        dnd_target_set_string(out, g_string_free(str, FALSE));
    }
    g_strfreev(uris);
}

static void dnd_target_get_list(gboolean files, GgtkDndTargetValue *out, int32_t *oom_reports)
{
    selection_data_ctx ctx;

    if (dnd_target_receive_data(TARGET_MIME_URI_LIST_ATOM, &ctx, oom_reports)) {
        dnd_target_uris_to_value(g_uri_list_extract_uris((gchar *)ctx.data), files, out);
        g_free(ctx.data);
    }
}

static void dnd_target_get_image(GgtkDndTargetValue *out, int32_t *oom_reports)
{
    GdkPixbuf *buf;
    GInputStream *stream;
    GdkAtom targets[] = {
        TARGET_MIME_PNG_ATOM,
        TARGET_MIME_JPEG_ATOM,
        TARGET_MIME_TIFF_ATOM,
        TARGET_MIME_BMP_ATOM,
        0};
    GdkAtom *cur_target = targets;
    selection_data_ctx ctx;

    for (; *cur_target != 0 && out->kind == GGTK_DND_VALUE_NONE; ++cur_target) {
        if (dnd_target_receive_data(*cur_target, &ctx, oom_reports)) {
            const gint fmtDiv8 = ctx.format / 8;
            if (ctx.length <= 0 || fmtDiv8 <= 0 || ctx.length >= INT_MAX / fmtDiv8) {
                // g_memory_input_stream_new_from_data below is what takes ownership of ctx.data (g_free as
                // its GDestroyNotify), so on this path the block has no owner. Commit 033187ad90 leaked it
                // here; free it, as the same check in dnd_target_get_raw does.
                g_free(ctx.data);
                continue;
            }
            stream = g_memory_input_stream_new_from_data(ctx.data, ctx.length * fmtDiv8,
                    (GDestroyNotify)g_free);
            buf = gdk_pixbuf_new_from_stream(stream, NULL, NULL);
            if (buf) {
                int w;
                int h;
                int stride;
                guchar *data;

                if (!gdk_pixbuf_get_has_alpha(buf)) {
                    GdkPixbuf *tmp_buf = gdk_pixbuf_add_alpha(buf, FALSE, 0, 0, 0);
                    g_object_unref(buf);
                    buf = tmp_buf;
                }

                w = gdk_pixbuf_get_width(buf);
                h = gdk_pixbuf_get_height(buf);
                stride = gdk_pixbuf_get_rowstride(buf);

                if (h <= 0 || stride <= 0 || h >= INT_MAX / stride) {
                    g_object_unref(buf);
                    g_object_unref(stream);
                    continue;
                }

                data = gdk_pixbuf_get_pixels(buf);

                //Actually, we are converting RGBA to BGRA, but that's the same operation
                data = (guchar*) convert_BGRA_to_RGBA((int*) data, stride, h);
                if (!data) {
                    g_object_unref(buf);
                    g_object_unref(stream);
                    continue;
                }

                // new GtkPixels(w, h, ByteBuffer.wrap(the stride * h bytes of data)); the block from
                // convert_BGRA_to_RGBA now belongs to the value
                out->kind = GGTK_DND_VALUE_IMAGE;
                out->count = stride * h;
                out->data = data;
                out->width = w;
                out->height = h;

                g_object_unref(buf);
            }
            g_object_unref(stream);
        }
    }
}

static void dnd_target_get_raw(GdkAtom target, gboolean string_data, GgtkDndTargetValue *out,
                               int32_t *oom_reports)
{
    selection_data_ctx ctx;
    if (dnd_target_receive_data(target, &ctx, oom_reports)) {
        if (string_data) {
            if (ctx.data != NULL) { // NewStringUTF((char *) NULL) answered null
                dnd_target_set_string(out, (gchar *) ctx.data);
                ctx.data = NULL;
            }
        } else {
            const gint fmtDiv8 = ctx.format / 8;
            if (ctx.length <= 0 || fmtDiv8 <= 0 || ctx.length >= INT_MAX / fmtDiv8) {
                g_free(ctx.data);
                return;
            }
            // ByteBuffer.wrap(the first ctx.length * fmtDiv8 bytes of ctx.data)
            out->kind = GGTK_DND_VALUE_BYTES;
            out->count = ctx.length * fmtDiv8;
            out->data = ctx.data;
            ctx.data = NULL;
        }
    }
    g_free(ctx.data);
}

extern "C" int32_t ggtk_dnd_target_get_data(const char* mime, GgtkDndTargetValue* out)
{
    memset(out, 0, sizeof(*out));
    if (!enter_ctx.ctx) { // check_state_in_drag of commit 033187ad90
        return GGTK_ERR_NOT_IN_DRAG;
    }
    const char *cmime = mime;
    int32_t oom_reports = 0;

    init_target_atoms();

    if (g_strcmp0(cmime, "text/plain") == 0) {
        dnd_target_get_string(out, &oom_reports);
    } else if (g_strcmp0(cmime, "text/uri-list") == 0) {
        dnd_target_get_list(FALSE, out, &oom_reports);
    } else if (g_str_has_prefix(cmime, "text/")) {
        dnd_target_get_raw(gdk_atom_intern(cmime, FALSE), TRUE, out, &oom_reports);
    } else if (g_strcmp0(cmime, "application/x-java-file-list") == 0) {
        dnd_target_get_list(TRUE, out, &oom_reports);
    } else if (g_strcmp0(cmime, "application/x-java-rawimage") == 0 ) {
        dnd_target_get_image(out, &oom_reports);
    } else {
        dnd_target_get_raw(gdk_atom_intern(cmime, FALSE), FALSE, out, &oom_reports);
    }

    return oom_reports;
}

/************************* SOURCE *********************************************/

static int32_t dnd_performed_action;

static void dnd_set_performed_action(int32_t performed_action)
{
    dnd_performed_action = performed_action;
}

static int32_t dnd_get_performed_action()
{
    return dnd_performed_action;
}

static void pixbufDestroyNotifyFunc(guchar *pixels, gpointer)
{
    if (pixels != NULL) {
        g_free(pixels);
    }
}

// dnd_source_get_data of commit 033187ad90 (Map.get on the drag source's data map) together with the conversion
// its caller applied to the value: the value `key` has in the data map of the drag in progress, converted as `as`
// says (see GgtkDndCallbacks.source_get_data). `out` is zero-filled first, so a slot that answers nothing reads
// as GGTK_DND_DATA_NONE. The key crosses as the bytes NewStringUTF decoded. Every caller makes no call when the
// slot is NULL.
typedef int32_t (*dnd_source_pull_t)(const char*, int32_t, int32_t, GgtkDndData*);

static int32_t dnd_source_pull(dnd_source_pull_t pull, const char *key, int32_t as, GgtkDndData *out)
{
    memset(out, 0, sizeof(*out));
    return pull(key, (key != NULL) ? (int32_t) strlen(key) : 0, as, out);
}

// The drag targets of one key of the drag source's data map; gstring is the key as GetStringUTFChars gave it
static void add_gtk_target_for_mime(GtkTargetList **list, const char *gstring, guint flags)
{
    if (g_strcmp0(gstring, "text/plain") == 0) {
        gtk_target_list_add(*list, TARGET_UTF8_STRING_ATOM, flags, 0);
        gtk_target_list_add(*list, TARGET_MIME_TEXT_PLAIN_ATOM, flags, 0);
        gtk_target_list_add(*list, TARGET_STRING_ATOM, flags, 0);
        //gtk_target_list_add(*list, TARGET_COMPOUND_TEXT_ATOM, flags, ??);
    } else if (g_strcmp0(gstring, "application/x-java-rawimage") == 0) {
        gtk_target_list_add(*list, TARGET_MIME_PNG_ATOM, flags, 0);
        gtk_target_list_add(*list, TARGET_MIME_JPEG_ATOM, flags, 0);
        gtk_target_list_add(*list, TARGET_MIME_TIFF_ATOM, flags, 0);
        gtk_target_list_add(*list, TARGET_MIME_BMP_ATOM, flags, 0);
    } else if (g_strcmp0(gstring, "application/x-java-file-list") == 0) {
        gtk_target_list_add(*list, TARGET_MIME_URI_LIST_ATOM, flags, 0);
    } else if (g_strcmp0(gstring, "application/x-java-drag-image") == 0
        || g_strcmp0(gstring, "application/x-java-drag-image-offset") == 0) {
        // do nothing - those are DragView information
    } else {
        GdkAtom atom = gdk_atom_intern(gstring, FALSE);
        gtk_target_list_add(*list, atom, flags, 0);
    }
}

// The keys of the drag source's data map arrive as a list (see ggtk_dnd_push_to_system): key_count NUL-terminated
// strings back to back, in the order commit 033187ad90 walked Map.keySet() here.
static GtkTargetList* data_to_gtk_target_list(const char *keys, int32_t key_count)
{
    guint flags = GTK_TARGET_OTHER_APP | GTK_TARGET_SAME_APP;

    GtkTargetList *tlist = gtk_target_list_new (NULL, 0);

    init_target_atoms();

    gint added_count = 0;

    const char *key = keys;
    for (int32_t i = 0; i < key_count; ++i) {
        add_gtk_target_for_mime(&tlist, key, flags);
        key += strlen(key) + 1;
    }

    return tlist;
}

static gboolean dnd_source_set_string(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom)
{
    gboolean is_data_set;

    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90, with the String's GetStringUTFChars bytes from the slot
        GgtkDndData value;
        dnd_source_pull(pull, "text/plain", GGTK_DND_AS_STRING, &value);
        if (value.kind != GGTK_DND_DATA_STRING) {
            g_free(value.data);
            return FALSE;
        }

        const char *cstring = (const char *) value.data;
        if (cstring) {
            if (atom == TARGET_MIME_TEXT_PLAIN_ATOM) {
                gchar *res_str = g_convert((gchar *) cstring, -1, "ISO-8859-1", "UTF-8", NULL, NULL, NULL);
                if (res_str) {
                    is_data_set = gtk_selection_data_set_text(data, res_str, strlen(res_str));
                    g_free(res_str);
                }
            } else {
                gint size = strlen(cstring);
                is_data_set = gtk_selection_data_set_text(data, (gchar *) cstring, size);
            }
        }

        g_free(value.data);

        return is_data_set;
    }
    return FALSE; // no slot, no call: no data
}

static gboolean dnd_source_set_image(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom)
{
    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90; Pixels.attachData ran inside the slot and wrote value.pixbuf
        GgtkDndData value;
        int32_t status = dnd_source_pull(pull, "application/x-java-rawimage", GGTK_DND_AS_PIXBUF, &value);
        if (value.kind != GGTK_DND_DATA_PIXBUF) {
            return FALSE;
        }

        GdkPixbuf *pixbuf = (GdkPixbuf *) value.pixbuf;
        gboolean is_data_set;

        if (status == GGTK_UPCALL_OK) { // EXCEPTION_OCCURED site: a throw of attachData skips the set
            is_data_set = gtk_selection_data_set_pixbuf(data, pixbuf);
        }

        g_object_unref(pixbuf);

        return is_data_set;
    }
    return FALSE; // no slot, no call: no data
}

static gboolean dnd_source_set_uri(GtkWidget *widget, GtkSelectionData *data, GdkAtom atom)
{
    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90, in the same order: the URI string, then the file names (the
        // String[] arrives as `count` NUL-terminated strings back to back)
        GgtkDndData url_value;
        GgtkDndData files_value;
        dnd_source_pull(pull, "text/uri-list", GGTK_DND_AS_STRING, &url_value);
        dnd_source_pull(pull, "application/x-java-file-list", GGTK_DND_AS_STRINGS, &files_value);

        const gchar* url = (url_value.kind == GGTK_DND_DATA_STRING) ? (const gchar*) url_value.data : NULL;
        gsize files_cnt = (files_value.kind == GGTK_DND_DATA_STRINGS) ? files_value.count : 0;

        if (!url && !files_cnt) {
            g_free(url_value.data);
            g_free(files_value.data);
            return FALSE;
        }

        gboolean is_data_set;
        GString* res = g_string_new (NULL); //http://www.ietf.org/rfc/rfc2483.txt

        if (files_cnt > 0) {
            const gchar* file = (const gchar*) files_value.data;
            for (gsize i = 0; i < files_cnt; ++i) {
                gchar* uri = g_filename_to_uri(file, NULL, NULL);

                g_string_append(res, uri);
                g_string_append(res, URI_LIST_LINE_BREAK);

                g_free(uri);
                file += strlen(file) + 1;
            }
        }
        if (url) {
            g_string_append(res, url);
            g_string_append(res, URI_LIST_LINE_BREAK);
        }
        g_free(url_value.data);
        g_free(files_value.data);

        gchar *uri[2];
        uri[0] = g_string_free(res, FALSE);
        uri[1] = NULL;

        is_data_set = gtk_selection_data_set_uris(data, uri);

        g_free(uri[0]);

        return is_data_set;
    }
    return FALSE; // no slot, no call: no data
}

static gboolean dnd_source_set_raw(GtkWidget *widget, GtkSelectionData *sel_data, GdkAtom atom)
{
    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90: a String is set as text, a ByteBuffer's whole backing array as
        // format-8 data
        gchar *target_name = gdk_atom_name(atom);
        GgtkDndData value;
        int32_t status = dnd_source_pull(pull, target_name, GGTK_DND_AS_RAW, &value);
        gboolean is_data_set = FALSE;
        if (value.kind == GGTK_DND_DATA_STRING) {
            const char *cstring = (const char *) value.data;
            if (cstring) {
                is_data_set = gtk_selection_data_set_text(sel_data, (gchar *) cstring, strlen(cstring));
            }
        } else if (value.kind == GGTK_DND_DATA_BYTES && status == GGTK_UPCALL_OK) {
            // EXCEPTION_OCCURED site: a throw of ByteBuffer.array() sets nothing
            guchar *raw = (guchar *) value.data;
            if (raw) {
                gtk_selection_data_set(sel_data, atom, 8, raw, value.count);
                is_data_set = TRUE;
            }
        }

        g_free(value.data);
        g_free(target_name);
        return is_data_set;
    }
    return FALSE; // no slot, no call: no data
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
                             gpointer user_data)
{
    if (drag_widget) {
        GdkDragAction action = gdk_drag_context_get_selected_action(context);
        dnd_set_performed_action(translate_gdk_action_to_glass(action));
    }
    gdk_threads_add_idle((GSourceFunc) dnd_destroy_drag_widget_callback, NULL);
}

static gboolean dnd_drag_failed_callback(GtkWidget *widget,
                                     GdkDragContext *context,
                                     GtkDragResult result,
                                     gpointer user_data)
{
    dnd_set_performed_action(com_sun_glass_ui_Clipboard_ACTION_NONE);
    gdk_threads_add_idle((GSourceFunc) dnd_destroy_drag_widget_callback, NULL);

    return FALSE;
}

static void dnd_data_get_callback(GtkWidget *widget,
                                  GdkDragContext *context,
                                  GtkSelectionData *data,
                                  guint info,
                                  guint time,
                                  gpointer user_data)
{
    GdkAtom atom = gtk_selection_data_get_target(data);

    if (target_is_text(atom)) {
        dnd_source_set_string(widget, data, atom);
    } else if (target_is_image(atom)) {
        dnd_source_set_image(widget, data, atom);
    } else if (target_is_uri(atom)) {
        dnd_source_set_uri(widget, data, atom);
    } else {
        dnd_source_set_raw(widget, data, atom);
    }
}

static void dnd_drag_begin_callback(GtkWidget *widget,
                                    GdkDragContext *context,
                                    gpointer user_data)
{
    DragView::set_drag_view(widget, context);
}

static void dnd_source_push_data(const char *keys, int32_t key_count, int32_t supported)
{
    if (supported == 0) {
        return; // No supported actions, do nothing
    }

    GdkDragAction actions = translate_glass_action_to_gdk(supported);

    // this widget is used only to pass events and will
    // be destroyed on drag end
    drag_widget = gtk_window_new(GTK_WINDOW_POPUP);
    gtk_window_resize(GTK_WINDOW(drag_widget), 1, 1);
    gtk_window_move(GTK_WINDOW(drag_widget), -200, -200);
    gtk_widget_show(drag_widget);

    g_signal_connect(drag_widget, "drag-begin",
        G_CALLBACK(dnd_drag_begin_callback), NULL);

    g_signal_connect(drag_widget, "drag-failed",
        G_CALLBACK(dnd_drag_failed_callback), NULL);

    g_signal_connect(drag_widget, "drag-data-get",
        G_CALLBACK(dnd_data_get_callback), NULL);

    g_signal_connect(drag_widget, "drag-end",
        G_CALLBACK(dnd_end_callback), NULL);

    GtkTargetList *tlist = data_to_gtk_target_list(keys, key_count);

    GdkDragContext *context;

    gint x, y;
    glass_gdk_master_pointer_get_position(&x, &y);

    is_dnd_owner = TRUE;

    context = gtk_drag_begin(drag_widget, tlist, actions, 1, NULL);

    gtk_target_list_unref(tlist);
}

// execute_dnd of commit 033187ad90 (glass_dnd.cpp), which GtkDnDClipboard.pushToSystemImpl now reaches through
// GtkGlassNative. Its catch (jni_exception&) - a throw of the JNI walk of the data map's keys - has no source any
// more: Java walks the keys before the call (see glass_gtk_api.h).
extern "C" int32_t ggtk_dnd_push_to_system(const char* keys, int32_t key_count, int32_t supported)
{
    dnd_source_push_data(keys, key_count, supported);

    while (is_in_drag()) {
        gtk_main_iteration();
    }

    return dnd_get_performed_action();
}

 /******************** DRAG VIEW ***************************/
 DragView::View* DragView::view = NULL;

 gboolean DragView::get_drag_image_offset(GtkWidget *widget, int* x, int* y)
 {
    gboolean offset_set = FALSE;

    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90, with the ByteBuffer's whole backing array from the slot
        GgtkDndData value;
        int32_t status = dnd_source_pull(pull, "application/x-java-drag-image-offset", GGTK_DND_AS_BYTES,
                &value);
        if (value.kind == GGTK_DND_DATA_BYTES && status == GGTK_UPCALL_OK) { // EXCEPTION_OCCURED site
            const int32_t* r = (const int32_t*) value.data;
            int32_t nraw = value.count;

            if ((size_t) nraw >= sizeof(int32_t) * 2) {
                *x = BSWAP_32(r[0]);
                *y = BSWAP_32(r[1]);
                offset_set = TRUE;
            }
        }
        g_free(value.data);
    }
    return offset_set;
}

GdkPixbuf* DragView::get_drag_image(GtkWidget *widget, gboolean* is_raw_image, gint* width, gint* height)
{
    GdkPixbuf *pixbuf = NULL;
    gboolean is_raw = FALSE;

    dnd_source_pull_t pull = glass_dnd_cb.source_get_data;
    if (pull) {
        // The JNI steps of commit 033187ad90, with the ByteBuffer's whole backing array from the slot
        GgtkDndData value;
        int32_t status = dnd_source_pull(pull, "application/x-java-drag-image", GGTK_DND_AS_BYTES, &value);
        if (value.kind == GGTK_DND_DATA_BYTES && status == GGTK_UPCALL_OK) { // EXCEPTION_OCCURED site
            const guchar* raw = (const guchar*) value.data;
            int32_t nraw = value.count;

            int w = 0, h = 0;
            int whsz = sizeof(int32_t) * 2; // Pixels are stored right after two ints
            // in this byteArray: width and height
            if (nraw > whsz) {
                const int32_t* int_raw = (const int32_t*) raw;
                w = BSWAP_32(int_raw[0]);
                h = BSWAP_32(int_raw[1]);

                // We should have enough pixels for requested width and height
                if (w > 0 && h > 0 &&  w < (INT_MAX / 4) / h &&
                        (nraw - whsz) / 4 - w * h >= 0) {
                    guchar* data = (guchar*) g_try_malloc0(nraw - whsz);
                    if (data) {
                        memcpy(data, (raw + whsz), nraw - whsz);

                        if (is_raw_image) {
                            guchar* origdata = data;
                            data = (guchar*) convert_BGRA_to_RGBA((const int*) data, w * 4, h);
                            g_free(origdata);
                        }

                        if (data) {
                            pixbuf = gdk_pixbuf_new_from_data(data, GDK_COLORSPACE_RGB, TRUE, 8,
                                                              w, h, w * 4, pixbufDestroyNotifyFunc, NULL);
                        }
                    }
                }
            }
        }
        g_free(value.data);
    }

    if (!GDK_IS_PIXBUF(pixbuf) && pull) {
        // The fallback of the JNI steps of commit 033187ad90; Pixels.attachData ran inside the slot and wrote
        // value.pixbuf
        GgtkDndData value;
        int32_t status = dnd_source_pull(pull, "application/x-java-rawimage", GGTK_DND_AS_PIXBUF, &value);
        if (value.kind == GGTK_DND_DATA_PIXBUF) {
            is_raw = TRUE;
            pixbuf = (GdkPixbuf *) value.pixbuf;
            if (status != GGTK_UPCALL_OK) { // CHECK_JNI_EXCEPTION_RET site
                return NULL;
            }
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

void DragView::set_drag_view(GtkWidget *widget, GdkDragContext *context)
{
    gboolean is_raw_image = FALSE;
    gint w = 0, h = 0;
    GdkPixbuf* pixbuf = get_drag_image(widget, &is_raw_image, &w, &h);

    if (GDK_IS_PIXBUF(pixbuf)) {
        gint offset_x = w / 2;
        gint offset_y = h / 2;

        gboolean is_offset_set = get_drag_image_offset(widget, &offset_x, &offset_y);

        DragView::view = new DragView::View(context, pixbuf, w, h, is_raw_image,
            is_offset_set, offset_x, offset_y);
    }
}

static void on_screen_changed(GtkWidget *widget, GdkScreen *previous_screen, gpointer view)
{
    (void)widget;
    (void)previous_screen;

    ((DragView::View*) view)->screen_changed();
}

static gboolean on_expose(GtkWidget *widget, GdkEventExpose *event, gpointer view)
{
    (void)widget;
    (void)event;

    ((DragView::View*) view)->expose();
    return FALSE;
}

DragView::View::View(GdkDragContext* _context, GdkPixbuf* _pixbuf, gint _width, gint _height,
                     gboolean _is_raw_image, gboolean _is_offset_set, gint _offset_x, gint _offset_y) :
    context(_context),
    pixbuf(_pixbuf),
    width(_width),
    height(_height),
    is_raw_image(_is_raw_image),
    is_offset_set(_is_offset_set),
    offset_x(_offset_x),
    offset_y(_offset_y)
{
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

void DragView::View::screen_changed()
{
    GdkScreen *screen = gtk_widget_get_screen(widget);

    glass_configure_window_transparency(widget, true);

    if (!gdk_screen_is_composited(screen)) {
        if (!is_offset_set) {
            offset_x = 1;
            offset_y = 1;
        }
    }
}

void DragView::View::expose()
{
    cairo_t *context = gdk_cairo_create(gtk_widget_get_window(widget));

    gdk_cairo_set_source_pixbuf(context, pixbuf, 0, 0);
    cairo_paint(context);

    cairo_destroy(context);
}
