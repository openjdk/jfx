/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
package javafxports.android;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import java.util.concurrent.CountDownLatch;

public class FXFragment extends Fragment {

    private Activity activity;
    private String fxAppClassName;

    private static final String TAG = "FXFragment";
    private static CountDownLatch cdlEvLoopFinished;

    private static Launcher launcher;

    static {
        System.loadLibrary("activity");
    }
    private FXDalvikEntity fxDalvikEntity;
    private SurfaceView mView;

    protected FXFragment() {
        activity = getActivity();
    }

    public void setName(String appname) {
        fxAppClassName = appname;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle metadata) {
        activity = getActivity();
        if (metadata == null) {
            metadata = new Bundle();
        }
        metadata.putSerializable(FXDalvikEntity.META_DATA_MAIN_CLASS, fxAppClassName);
        fxDalvikEntity = new FXDalvikEntity(metadata, activity);
        mView = fxDalvikEntity.createView();
        return mView;
    }

}
