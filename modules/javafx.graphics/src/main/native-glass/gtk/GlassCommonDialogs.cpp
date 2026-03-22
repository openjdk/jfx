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
#include <com_sun_glass_ui_gtk_GtkCommonDialogs.h>
#include "glass_general.h"
#include "glass_window.h"
#include "filechooser_portal.h"

#include <gdk/gdk.h>
#include <gtk/gtk.h>

#include <cstring>
#include <cstdlib>
#include <vector>
#include <string>

static GSList* setup_GtkFileFilters(GtkFileChooser*, JNIEnv*, jobjectArray, int default_filter_index);

static void free_fname(char* fname, gpointer unused) {
    (void)unused;

    g_free(fname);
}

static gboolean jstring_to_utf_get(JNIEnv *env, jstring jstr,
                                   const char **cstr) {
    const char *newstr;

    if (jstr == NULL) {
        *cstr = NULL;
        return TRUE;
    }

    newstr = env->GetStringUTFChars(jstr, NULL);
    if (newstr != NULL) {
        *cstr = newstr;
        return TRUE;
    }

    return FALSE;
}

static void jstring_to_utf_release(JNIEnv *env, jstring jstr,
                                   const char *cstr) {
    if (cstr != NULL) {
        env->ReleaseStringUTFChars(jstr, cstr);
    }
}

static GdkWindow *get_gdk_window(jlong handle) {
    return  (handle != 0)
                ? ((WindowContext*)JLONG_TO_PTR(handle))->get_gdk_window()
                : NULL;
}

static void on_dialog_realize_set_parent(GtkWidget *dialog, gpointer user_data) {
    GdkWindow *parent_gdk_window = (GdkWindow *) user_data;
    GdkWindow *dialog_gdk_window = gtk_widget_get_window(dialog);

    if (dialog_gdk_window && parent_gdk_window) {
        gdk_window_set_transient_for(dialog_gdk_window, parent_gdk_window);
    }
}

static jobject create_empty_result() {
    jclass jFileChooserResult = (jclass) mainEnv->FindClass("com/sun/glass/ui/CommonDialogs$FileChooserResult");
    if (EXCEPTION_OCCURED(mainEnv)) return NULL;
    jmethodID jFileChooserResultInit = mainEnv->GetMethodID(jFileChooserResult, "<init>", "()V");
    if (EXCEPTION_OCCURED(mainEnv)) return NULL;
    jobject jResult = mainEnv->NewObject(jFileChooserResult, jFileChooserResultInit);
    if (EXCEPTION_OCCURED(mainEnv)) return NULL;
    return jResult;
}

/**
 * Convert a file:// URI to a local filesystem path.
 * Returns an allocated string that must be freed with g_free(), or NULL on failure.
 */
static gchar *uri_to_path(const char *uri) {
    return g_filename_from_uri(uri, NULL, NULL);
}

/**
 * Extract Java ExtensionFilter[] into a vector of PortalFileFilter for the portal API.
 */
static std::vector<PortalFileFilter> extract_portal_filters(JNIEnv *env, jobjectArray extFilters) {
    std::vector<PortalFileFilter> result;
    if (extFilters == NULL) return result;

    jclass jcls = env->FindClass("com/sun/glass/ui/CommonDialogs$ExtensionFilter");
    if (EXCEPTION_OCCURED(env)) return result;
    jmethodID jgetDescription = env->GetMethodID(jcls, "getDescription", "()Ljava/lang/String;");
    if (EXCEPTION_OCCURED(env)) return result;
    jmethodID jextensionsToArray = env->GetMethodID(jcls, "extensionsToArray", "()[Ljava/lang/String;");
    if (EXCEPTION_OCCURED(env)) return result;

    jsize size = env->GetArrayLength(extFilters);
    for (jsize i = 0; i < size; i++) {
        PortalFileFilter filter;
        jobject jfilter = env->GetObjectArrayElement(extFilters, i);
        EXCEPTION_OCCURED(env);

        jstring jdesc = (jstring) env->CallObjectMethod(jfilter, jgetDescription);
        const char *desc = env->GetStringUTFChars(jdesc, NULL);
        filter.name = desc ? desc : "";
        env->ReleaseStringUTFChars(jdesc, desc);

        jobjectArray jextensions = (jobjectArray) env->CallObjectMethod(jfilter, jextensionsToArray);
        jsize extSize = env->GetArrayLength(jextensions);
        for (jsize j = 0; j < extSize; j++) {
            jstring jext = (jstring) env->GetObjectArrayElement(jextensions, j);
            EXCEPTION_OCCURED(env);
            const char *ext = env->GetStringUTFChars(jext, NULL);
            filter.patterns.push_back(std::string(ext ? ext : "*"));
            env->ReleaseStringUTFChars(jext, ext);
        }

        result.push_back(filter);
    }

    return result;
}

/**
 * Build the Java FileChooserResult from a PortalFileChooserResult.
 */
static jobject build_portal_file_chooser_result(JNIEnv *env,
                                            const PortalFileChooserResult &portalResult,
                                            jobjectArray jFilters) {
    jobjectArray jFileNames = NULL;

    if (portalResult.accepted && !portalResult.uris.empty()) {
        jsize count = (jsize) portalResult.uris.size();
        jFileNames = env->NewObjectArray(count, jStringCls, NULL);
        EXCEPTION_OCCURED(env);
        const jmethodID bytesInit = env->GetMethodID(jStringCls, "<init>", "([B)V");
        EXCEPTION_OCCURED(env);

        for (jsize i = 0; i < count; i++) {
            gchar *path = uri_to_path(portalResult.uris[i].c_str());
            if (!path) continue;
            int len = strlen(path);
            jbyteArray bytes = env->NewByteArray(len);
            EXCEPTION_OCCURED(env);
            env->SetByteArrayRegion(bytes, 0, len, (jbyte *) path);
            EXCEPTION_OCCURED(env);
            jstring jfilename = (jstring) env->NewObject(jStringCls, bytesInit, bytes);
            EXCEPTION_OCCURED(env);
            env->DeleteLocalRef(bytes);
            env->SetObjectArrayElement(jFileNames, i, jfilename);
            EXCEPTION_OCCURED(env);
            g_free(path);
        }
    }

    if (!jFileNames) {
        jFileNames = env->NewObjectArray(0, jStringCls, NULL);
        EXCEPTION_OCCURED(env);
    }

    int index = portalResult.filterIndex;

    jclass jCommonDialogs = (jclass) env->FindClass("com/sun/glass/ui/CommonDialogs");
    EXCEPTION_OCCURED(env);
    jmethodID jCreateFileChooserResult = env->GetStaticMethodID(jCommonDialogs,
            "createFileChooserResult",
            "([Ljava/lang/String;[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;I)Lcom/sun/glass/ui/CommonDialogs$FileChooserResult;");
    EXCEPTION_OCCURED(env);

    jobject result = env->CallStaticObjectMethod(jCommonDialogs, jCreateFileChooserResult,
                                                 jFileNames, jFilters, index);
    LOG_EXCEPTION(env)

    return result;
}

/**
 * Try to show a file chooser via the portal.
 * Returns a valid jobject on success (even if user cancelled), or NULL if the portal is unavailable.
 */
static jobject portal_show_file_chooser(JNIEnv *env, jlong parent,
                                      const char *folder, const char *name, const char *title,
                                      jint type, jboolean multiple,
                                      jobjectArray jFilters, jint default_filter_index,
                                      jboolean usePortal) {
    if (!usePortal) {
        LOG0("Portal file chooser disabled via glass.gtk.disablePortalFileChooser=true\n")
        return NULL;
    }

    PortalFileChooser portal;
    portal.setParentWindow(get_gdk_window(parent));
    portal.setTitle(title);
    portal.setCurrentFolder(folder);
    portal.setCurrentName(name);
    portal.setMultiple(JNI_TRUE == multiple);
    portal.setDefaultFilterIndex(default_filter_index);

    std::vector<PortalFileFilter> filters = extract_portal_filters(env, jFilters);
    portal.setFilters(filters);

    PortalFileChooserResult portalResult = (type == 0)
            ? portal.openFile()
            : portal.saveFile();

    if (portalResult.failed) {
        LOG0("Portal file chooser failed, falling back to GTK dialog\n")
        return NULL;
    }

    return build_portal_file_chooser_result(env, portalResult, jFilters);
}

/**
 * Try to show a folder chooser via the portal.
 * Returns a jstring path on success, (jstring) -1 as sentinel if portal unavailable, or NULL if user cancelled.
 */
#define PORTAL_FOLDER_UNAVAILABLE ((jstring)(intptr_t)-1)

static jstring portal_show_folder_chooser(JNIEnv *env, jlong parent,
                                        const char *folder, const char *title,
                                        jboolean usePortal) {
    if (!usePortal) {
        LOG0("Portal folder chooser disabled via glass.gtk.disablePortalFileChooser=true\n")
        return PORTAL_FOLDER_UNAVAILABLE;
    }

    PortalFileChooser portal;
    portal.setParentWindow(get_gdk_window(parent));
    portal.setTitle(title);
    portal.setCurrentFolder(folder);

    PortalFileChooserResult portalResult = portal.openFolder();

    if (portalResult.failed) {
        LOG0("Portal folder chooser failed, falling back to GTK dialog\n")
        return PORTAL_FOLDER_UNAVAILABLE;
    }

    if (portalResult.accepted && !portalResult.uris.empty()) {
        gchar *path = uri_to_path(portalResult.uris[0].c_str());
        if (path) {
            jstring jfilename = env->NewStringUTF(path);
            LOG1("Portal selected folder: %s\n", path);
            g_free(path);
            return jfilename;
        }
    }

    return NULL;
}

extern "C" {

JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_gtk_GtkCommonDialogs__1showFileChooser
  (JNIEnv *env, jclass clazz, jlong parent, jstring folder, jstring name, jstring title,
   jint type, jboolean multiple, jobjectArray jFilters, jint default_filter_index,
   jboolean usePortal) {
    (void)clazz;

    jobjectArray jFileNames = NULL;
    char* filename;
    jstring jfilename;

    const char* chooser_folder;
    const char* chooser_filename;
    const char* chooser_title;
    const int chooser_type = type == 0 ? GTK_FILE_CHOOSER_ACTION_OPEN : GTK_FILE_CHOOSER_ACTION_SAVE;

    if (!jstring_to_utf_get(env, folder, &chooser_folder)) {
        return create_empty_result();
    }

    if (!jstring_to_utf_get(env, title, &chooser_title)) {
        jstring_to_utf_release(env, folder, chooser_folder);
        return create_empty_result();
    }

    if (!jstring_to_utf_get(env, name, &chooser_filename)) {
        jstring_to_utf_release(env, folder, chooser_folder);
        jstring_to_utf_release(env, title, chooser_title);
        return create_empty_result();
    }

    // Try portal first
    jobject portalResult = portal_show_file_chooser(env, parent,
            chooser_folder, chooser_filename, chooser_title,
            type, multiple, jFilters, default_filter_index, usePortal);

    if (portalResult != NULL) {
        jstring_to_utf_release(env, folder, chooser_folder);
        jstring_to_utf_release(env, title, chooser_title);
        jstring_to_utf_release(env, name, chooser_filename);
        return portalResult;
    }

    // Fallback to GTK file chooser dialog
    LOG0("Using GTK file chooser dialog (fallback)\n")

    GtkWidget* chooser = gtk_file_chooser_dialog_new(chooser_title, NULL,
            static_cast<GtkFileChooserAction>(chooser_type),
            GTK_STOCK_CANCEL,
            GTK_RESPONSE_CANCEL,
            (chooser_type == GTK_FILE_CHOOSER_ACTION_OPEN ? GTK_STOCK_OPEN : GTK_STOCK_SAVE),
            GTK_RESPONSE_ACCEPT,
            NULL);

    g_signal_connect(chooser, "realize", G_CALLBACK(on_dialog_realize_set_parent), get_gdk_window(parent));

    if (chooser_type == GTK_FILE_CHOOSER_ACTION_SAVE) {
        gtk_file_chooser_set_current_name(GTK_FILE_CHOOSER(chooser), chooser_filename);
        gtk_file_chooser_set_do_overwrite_confirmation(GTK_FILE_CHOOSER (chooser), TRUE);
    }

    gtk_file_chooser_set_select_multiple(GTK_FILE_CHOOSER(chooser), (JNI_TRUE == multiple));
    gtk_file_chooser_set_current_folder(GTK_FILE_CHOOSER(chooser), chooser_folder);
    GSList* filters = setup_GtkFileFilters(GTK_FILE_CHOOSER(chooser), env, jFilters, default_filter_index);

    if (gtk_dialog_run(GTK_DIALOG(chooser)) == GTK_RESPONSE_ACCEPT) {
        GSList* fnames_gslist = gtk_file_chooser_get_filenames(GTK_FILE_CHOOSER(chooser));
        guint fnames_list_len = g_slist_length(fnames_gslist);
        LOG1("FileChooser selected files: %d\n", fnames_list_len)

        if (fnames_list_len > 0) {
            jFileNames = env->NewObjectArray((jsize)fnames_list_len, jStringCls, NULL);
            EXCEPTION_OCCURED(env);
            const jmethodID bytesInit = env->GetMethodID(jStringCls, "<init>", "([B)V");
            EXCEPTION_OCCURED(env);
            for (guint i = 0; i < fnames_list_len; i++) {
                filename = (char*)g_slist_nth(fnames_gslist, i)->data;
                LOG1("Add [%s] into returned filenames\n", filename)
                int len = strlen(filename);
                jbyteArray bytes = env->NewByteArray(len);
                EXCEPTION_OCCURED(env);
                env->SetByteArrayRegion(bytes, 0, len, (jbyte *)filename);
                EXCEPTION_OCCURED(env);
                jfilename = (jstring) env->NewObject(jStringCls, bytesInit, bytes);
                EXCEPTION_OCCURED(env);
                env->DeleteLocalRef(bytes);
                EXCEPTION_OCCURED(env);
                env->SetObjectArrayElement(jFileNames, (jsize)i, jfilename);
                EXCEPTION_OCCURED(env);
            }
            g_slist_foreach(fnames_gslist, (GFunc) free_fname, NULL);
            g_slist_free(fnames_gslist);
        }
    }

    if (!jFileNames) {
        jFileNames = env->NewObjectArray(0, jStringCls, NULL);
        EXCEPTION_OCCURED(env);
    }

    int index = g_slist_index(filters, gtk_file_chooser_get_filter(GTK_FILE_CHOOSER(chooser)));

    jclass jCommonDialogs = (jclass) env->FindClass("com/sun/glass/ui/CommonDialogs");
    EXCEPTION_OCCURED(env);
    jmethodID jCreateFileChooserResult = env->GetStaticMethodID(jCommonDialogs,
            "createFileChooserResult",
            "([Ljava/lang/String;[Lcom/sun/glass/ui/CommonDialogs$ExtensionFilter;I)Lcom/sun/glass/ui/CommonDialogs$FileChooserResult;");

    EXCEPTION_OCCURED(env);

    jobject result =
            env->CallStaticObjectMethod(jCommonDialogs, jCreateFileChooserResult, jFileNames, jFilters, index);
    LOG_EXCEPTION(env)

    g_slist_free(filters);
    gtk_widget_destroy(chooser);

    jstring_to_utf_release(env, folder, chooser_folder);
    jstring_to_utf_release(env, title, chooser_title);
    jstring_to_utf_release(env, name, chooser_filename);

    LOG_STRING_ARRAY(env, jFileNames);
    return result;
}

JNIEXPORT jstring JNICALL Java_com_sun_glass_ui_gtk_GtkCommonDialogs__1showFolderChooser
  (JNIEnv *env, jclass clazz, jlong parent, jstring folder, jstring title, jboolean usePortal) {
    (void)clazz;

    jstring jfilename = NULL;
    const char *chooser_folder;
    const char *chooser_title;

    if (!jstring_to_utf_get(env, folder, &chooser_folder)) {
        return NULL;
    }

    if (!jstring_to_utf_get(env, title, &chooser_title)) {
        jstring_to_utf_release(env, folder, chooser_folder);
        return NULL;
    }

    // Try portal first
    jstring portalResult = portal_show_folder_chooser(env, parent, chooser_folder, chooser_title, usePortal);

    if (portalResult != PORTAL_FOLDER_UNAVAILABLE) {
        jstring_to_utf_release(env, folder, chooser_folder);
        jstring_to_utf_release(env, title, chooser_title);
        return portalResult;
    }

    // Fallback to GTK folder chooser dialog
    LOG0("Using GTK folder chooser dialog (fallback)\n")

    GtkWidget* chooser = gtk_file_chooser_dialog_new(
            chooser_title,
            NULL,
            GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
            GTK_STOCK_CANCEL,
            GTK_RESPONSE_CANCEL,
            GTK_STOCK_OPEN,
            GTK_RESPONSE_ACCEPT,
            NULL);

    g_signal_connect(chooser, "realize", G_CALLBACK(on_dialog_realize_set_parent), get_gdk_window(parent));

    if (chooser_folder != NULL) {
        gtk_file_chooser_set_current_folder(GTK_FILE_CHOOSER(chooser),
                                            chooser_folder);
    }

    if (gtk_dialog_run(GTK_DIALOG(chooser)) == GTK_RESPONSE_ACCEPT) {
        gchar* filename = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(chooser));
        jfilename = env->NewStringUTF(filename);
        LOG1("Selected folder: %s\n", filename);
        g_free(filename);
    }

    jstring_to_utf_release(env, folder, chooser_folder);
    jstring_to_utf_release(env, title, chooser_title);

    gtk_widget_destroy(chooser);
    return jfilename;
}

} // extern "C"

/**
 *
 * @param env
 * @param extFilters ExtensionFilter[]
 * @return
 */
static GSList* setup_GtkFileFilters(GtkFileChooser* chooser, JNIEnv* env, jobjectArray extFilters, int default_filter_index) {
    int i;
    LOG0("Setup filters\n")
    //setup methodIDs
    jclass jcls = env->FindClass("com/sun/glass/ui/CommonDialogs$ExtensionFilter");
    if (EXCEPTION_OCCURED(env)) return NULL;
    jmethodID jgetDescription = env->GetMethodID(jcls,
                                         "getDescription", "()Ljava/lang/String;");
    if (EXCEPTION_OCCURED(env)) return NULL;
    jmethodID jextensionsToArray = env->GetMethodID(jcls,
                                         "extensionsToArray", "()[Ljava/lang/String;");
    if (EXCEPTION_OCCURED(env)) return NULL;

    jsize jfilters_size = env->GetArrayLength(extFilters);
    LOG1("Filters: %d\n", jfilters_size)
    if (jfilters_size == 0) return NULL;

    GSList* filter_list = NULL;

    for(i = 0; i<jfilters_size; i++) {
        GtkFileFilter* ffilter = gtk_file_filter_new();
        jobject jfilter = env->GetObjectArrayElement(extFilters, i);
        EXCEPTION_OCCURED(env);

        //setup description
        jstring jdesc = (jstring)env->CallObjectMethod(jfilter, jgetDescription);
        const char * description = env->GetStringUTFChars(jdesc, NULL);
        LOG2("description[%d]: %s\n", i, description)
        gtk_file_filter_set_name(ffilter, (gchar*)const_cast<char*>(description));
        env->ReleaseStringUTFChars(jdesc, description);

        //add patterns
        jobjectArray jextensions = (jobjectArray)env->CallObjectMethod(jfilter, jextensionsToArray);
        jsize jextarray_size = env->GetArrayLength(jextensions);
        LOG1("Patterns: %d\n", jextarray_size)
        int ext_idx;
        for(ext_idx = 0; ext_idx < jextarray_size; ext_idx++) {
            jstring jext = (jstring)env->GetObjectArrayElement(jextensions, ext_idx);
            EXCEPTION_OCCURED(env);
            const char * ext = env->GetStringUTFChars(jext, NULL);
            LOG2("pattern[%d]: %s\n", ext_idx, ext)
            gtk_file_filter_add_pattern(ffilter, (gchar*)const_cast<char*>(ext));
            env->ReleaseStringUTFChars(jext, ext);
        }
        LOG0("Filter ready\n")
        gtk_file_chooser_add_filter(chooser, ffilter);

        if (default_filter_index == i) {
            gtk_file_chooser_set_filter(chooser, ffilter);
        }

        filter_list = g_slist_append(filter_list, ffilter);
    }
    return filter_list;
}
