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

#ifndef PIXELS_H
#define PIXELS_H


class Pixels;

class BaseBitmap {
    public:
        BaseBitmap() : hBitmap(NULL) {}
        virtual ~BaseBitmap() { if (hBitmap) ::DeleteObject(hBitmap); }
        void Attach(HBITMAP hBmp)
        {
            if (hBitmap) ::DeleteObject(hBitmap);
            hBitmap = hBmp;
        }
        HBITMAP Detach() {
            HBITMAP hBmp = hBitmap;
            hBitmap = NULL;
            return hBmp;
        }

        operator HBITMAP() { return hBitmap; }
        operator HGDIOBJ() { return (HGDIOBJ)hBitmap; }
        operator bool() { return NULL != hBitmap; }

        HANDLE GetGlobalDIB();

    private:
        HBITMAP hBitmap;
};

class Bitmap : public BaseBitmap {
    public:
        Bitmap(int width, int height);
        Bitmap(int width, int height, void **data, HDC hdc = NULL);
        Bitmap(Pixels & pixels);
};

class DIBitmap : public BaseBitmap {
    public:
        DIBitmap(Pixels & pixels);
};

class Pixels {
    public:
        static HICON CreateIcon(JNIEnv *env, jobject jPixels, BOOL fIcon = TRUE, jint x = 0, jint y = 0);
        static HCURSOR CreateCursor(JNIEnv *env, jobject jPixels, jint x, jint y)
        {
            return (HCURSOR)CreateIcon(env, jPixels, FALSE, x, y);
        }

        Pixels(JNIEnv *env, jobject jPixels);

        void AttachInt(JNIEnv *env, jint w, jint h, jobject buf, jintArray array, jint offset);
        void AttachByte(JNIEnv *env, jint w, jint h, jobject buf, jbyteArray array, jint offset);

        int GetWidth() { return width; }
        int GetHeight() { return height; }
        void* GetBits();

    private:
        int width, height;
        JBufferArray<jint> ints;
        JBufferArray<jbyte> bytes;
};

#endif //PIXELS_H

