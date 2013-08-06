/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ANDROID_WEBVIEW_H
#define	ANDROID_WEBVIEW_H

#ifdef	__cplusplus
extern "C" {
#endif

    void init_ids(JNIEnv *);

    void init_functions(JNIEnv *);

    int create_android_webview();

    void move_and_resize(int id, jint x, int y, int w, int h);

    void set_visible(int id, int visible);

    void move_to_top(int id);


#ifdef	__cplusplus
}
#endif

#endif	/* ANDROID_WEBVIEW_H */

