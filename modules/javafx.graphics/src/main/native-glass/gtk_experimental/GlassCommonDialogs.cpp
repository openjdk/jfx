/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

#include <gdk/gdk.h>
#include <gtk/gtk.h>

#include <cstring>
#include <cstdlib>

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

static GtkWindow *gdk_window_handle_to_gtk(jlong handle) {
    return  (handle != 0)
                ? ((WindowContext*)JLONG_TO_PTR(handle))->get_gtk_window()
                : NULL;
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

extern "C" {

JNIEXPORT jobject JNICALL Java_com_sun_glass_ui_gtk_GtkCommonDialogs__1showFileChooser
  (JNIEnv *env, jclass clazz, jlong parent, jstring folder, jstring name, jstring title,
   jint type, jboolean multiple, jobjectArray jFilters, jint default_filter_index) {
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

    GtkWidget* chooser = gtk_file_chooser_dialog_new(chooser_title, gdk_window_handle_to_gtk(parent),
            static_cast<GtkFileChooserAction>(chooser_type),
            "_Cancel",
            GTK_RESPONSE_CANCEL,
            (chooser_type == GTK_FILE_CHOOSER_ACTION_OPEN ? "_Open" : "_Save"),
            GTK_RESPONSE_ACCEPT,
            NULL);

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
            for (guint i = 0; i < fnames_list_len; i++) {
                filename = (char*)g_slist_nth(fnames_gslist, i)->data;
                LOG1("Add [%s] into returned filenames\n", filename)
                jfilename = env->NewStringUTF(filename);
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
  (JNIEnv *env, jclass clazz, jlong parent, jstring folder, jstring title) {
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

    GtkWidget* chooser = gtk_file_chooser_dialog_new(
            chooser_title,
            gdk_window_handle_to_gtk(parent),
            GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER,
            "_Cancel",
            GTK_RESPONSE_CANCEL,
            "_Open",
            GTK_RESPONSE_ACCEPT,
            NULL);

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
