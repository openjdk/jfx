/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.dalvik;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javafxports.android.*;

public class InternalWebView {

    public final static int PAGE_STARTED = 0;
    public final static int PAGE_FINISHED = 1;
    public final static int PAGE_REDIRECTED = 2;
    public final static int LOAD_FAILED = 5;
    public final static int LOAD_STOPPED = 6;
    public final static int CONTENT_RECEIVED = 10;
    public final static int TITLE_RECEIVED = 11;
    public final static int ICON_RECEIVED = 12;
    public final static int CONTENTTYPE_RECEIVED = 13;
    public final static int DOCUMENT_AVAILABLE = 14;
    public final static int RESOURCE_STARTED = 20;
    public final static int RESOURCE_REDIRECTED = 21;
    public final static int RESOURCE_FINISHED = 22;
    public final static int RESOURCE_FAILED = 23;
    public final static int PROGRESS_CHANGED = 30;
    private static final String TAG = "InternalWebView";
    private static List<InternalWebView> views = new ArrayList<InternalWebView>();
    private static int idcounter = 0;
    private boolean isLayedOut = false;
    private boolean initialized = false;
    private int internalID;
    private int x, y, width, height;
    private WebView nativeWebView;
    private String url, content;
    private String contentType = "text/html";
    private String encoding = null;
    private String htmlContent;
    private boolean visible;
    private boolean pageFinished = false;

    public InternalWebView() {
        this.internalID = ++idcounter;
        views.add(0, this);
    }

    public int getInternalID() {
        return this.internalID;
    }


    private void initialize() {
        nativeWebView = new WebView(FXActivity.getInstance()) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setMeasuredDimension(width, height);
            }
        };
        nativeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageFinished = false;
                fireLoadEvent(0, PAGE_STARTED, url, contentType, -1, -1);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (!pageFinished) {
                    nativeWebView.loadUrl("javascript:window.HTMLOUT.processHTML('<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");
                }
                pageFinished = true;
                fireLoadEvent(0, PAGE_FINISHED, url, contentType, -1, -1);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                fireLoadEvent(0, LOAD_FAILED, failingUrl, contentType, -1, errorCode);
            }
        });

        nativeWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                fireLoadEvent(0, PROGRESS_CHANGED, url, contentType, newProgress, -1);
            }
        });

        WebSettings settings = nativeWebView.getSettings();
        settings.setSupportZoom(true);
        settings.setJavaScriptEnabled(true);
        nativeWebView.addJavascriptInterface(new MyJavaScriptInterface(), "HTMLOUT");
        initialized = true;        
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    private void fireLoadEvent(int frameID, int state, String url,
            String content_type, int progress, int errorCode) {
        _fireLoadEvent(this.internalID, frameID, state,
                url == null ? "" : url,
                content_type == null ? "" : content_type,
                progress, errorCode);
    }

    private static int indexOf(long id) {
        int i = 0;
        for (InternalWebView wvp : views) {
            if (id == wvp.internalID) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static InternalWebView getViewByID(int id) {
        for (InternalWebView wvp : views) {
            if (id == wvp.internalID) {
                return wvp;
            }
        }
        throw new RuntimeException("No InternalWebView with id: " + id);
    }

    static void createNew() {
        FXActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                new InternalWebView().getInternalID();
            }
        });
    }

    public static void loadUrl(int id, String url) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        iwv.setContent(null, null);
        iwv.setUrl(url);
        if (iwv.initialized && iwv.isLayedOut) {
            FXActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    int c = FXActivity.getViewGroup().getChildCount();                    
                    iwv.nativeWebView.loadUrl(iwv.url);
                }
            });
        }
    }

    public static String getHtmlContent (int id) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        return iwv.getHtmlContent();
    }

    public static void loadContent(int id, String content, String contentType) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        iwv.setUrl(null);
        iwv.setContent(content, contentType);
        if (iwv.initialized && iwv.isLayedOut) {
            FXActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {                          
                    iwv.nativeWebView.loadData(iwv.content, iwv.contentType, iwv.encoding);
                }
            });
        }
    }

    static void setEncoding(int id, String encoding) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        iwv.setEncoding(encoding);
    }

    public static void moveAndResize(int id, int x, int y, final int w, final int h) {
        final boolean move;
        final boolean resize;        

        if (w == 0 || h == 0) {
            return;
        }

        final InternalWebView iwv = InternalWebView.getViewByID(id);
        if (iwv == null) {
            return;
        }
        if (iwv.x == x
                && iwv.y == y
                && iwv.width == w
                && iwv.height == h) {
            return;
        }

        move = (iwv.x != x || iwv.y != y);
        if (move) {
            iwv.x = x;
            iwv.y = y;
        }
        resize = (iwv.width != w || iwv.height != h);
        if (resize) {
            iwv.width = w;
            iwv.height = h;
        }
        if (!iwv.visible) {
            return;
        }
        
        if (!iwv.isLayedOut) {
            iwv.isLayedOut = true;
            FXActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    if (!iwv.initialized) {
                        iwv.initialize();
                    }
                    FrameLayout.LayoutParams layout = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            Gravity.NO_GRAVITY);
                    layout.leftMargin = iwv.x;
                    layout.topMargin = iwv.y;
//                    iwv.nativeWebView.setTranslationX(iwv.x);
//                    iwv.nativeWebView.setTranslationY(iwv.y);
                    FXActivity.getViewGroup().addView(iwv.nativeWebView, layout);
                    Log.v(TAG, String.format("WebView added to ViewGroup [x: %d, y: %d , w: %d h: %d]",
                            iwv.x, iwv.y, iwv.width, iwv.height));
                    if (iwv.contentType == null || iwv.contentType.length() == 0) {
                        iwv.contentType = "text/html";
                    }
                    if (iwv.url != null && iwv.url.length() > 0) {
                        Log.v(TAG, "Loading url: " + iwv.url);
                        iwv.nativeWebView.loadUrl(iwv.url);
                    } else if (iwv.content != null) {                        
                        Log.v(TAG, String.format("Loading content: %s\ncontent type: %s\nencoding: %s",
                                iwv.content, iwv.contentType, iwv.encoding));
                        iwv.nativeWebView.loadData(iwv.content, iwv.contentType, iwv.encoding);
                    }
                }
            });

        }// end of not initialized
        else {
            FXActivity.getInstance().runOnUiThread(new Runnable() {
                public void run() {
                    if (move) {
                        FrameLayout.LayoutParams layout =
                                (FrameLayout.LayoutParams) iwv.nativeWebView.getLayoutParams();
                        layout.leftMargin = iwv.x;
                        layout.topMargin = iwv.y;
                        FXActivity.getViewGroup().updateViewLayout(iwv.nativeWebView, layout);
//                        iwv.nativeWebView.setTranslationX(iwv.x);
//                        iwv.nativeWebView.setTranslationY(iwv.y);
                    }
                    if (move || resize) {
                        iwv.nativeWebView.invalidate();
                    }                    
                }
            });
        }        
    }

    public static void setVisible(int id, final boolean visible) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        if (iwv == null) {
            return;
        }
        if (!iwv.initialized) {
            iwv.visible = visible;
            return;
        }
        FXActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                iwv.nativeWebView.setVisibility(visible ? View.VISIBLE : View.GONE);
                if (visible) {
                    iwv.nativeWebView.invalidate();
                }
            }
        });
    }

    public static void dispose(int id) {
        final InternalWebView iwv = InternalWebView.getViewByID(id);
        InternalWebView.setVisible(id, false);

        FXActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                iwv.nativeWebView.stopLoading();
                iwv.nativeWebView.destroy();
            }
        });
        views.remove(iwv);
    }

    private void setUrl(String url) {
        this.url = url;
    }

    private void setContent(String content, String contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    private void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    // private native void _fireLoadEvent(int id, int frameID, int state, String url,
    private void _fireLoadEvent(int id, int frameID, int state, String url,
            String contentType, int progress, int errorCode) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Class clazz = Class.forName("com.sun.webkit.NativeWebView", true, cl);
            Method m = clazz.getMethod("fire_load_event", int.class, int.class, int.class, 
                                       String.class, String.class, int.class, int.class);
            m.invoke(null, id, frameID, state, url, contentType, progress, errorCode);
        }
        catch (Exception e) {
            System.out.println ("[JVDBG] Error firing event");
            e.printStackTrace();
        }
    }

    class MyJavaScriptInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html) {
            htmlContent = html;
            fireLoadEvent(0, DOCUMENT_AVAILABLE, url, contentType, -1, -1);
        }
    }


}
