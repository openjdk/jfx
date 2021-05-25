/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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

#import <UIKit/UIKit.h>
#import <WebKit/WebKit.h>
#import <Foundation/Foundation.h>
#import <QuartzCore/QuartzCore.h>

#include <jni.h>

#ifdef  __LP64__
#define jlong_to_ptr(a) ((void*)(a))
#define ptr_to_jlong(a) ((jlong)(a))
#else
#define jlong_to_ptr(a) ((void*)(int)(a))
#define ptr_to_jlong(a) ((jlong)(int)(a))
#endif

@interface WebViewImpl : NSObject<WKNavigationDelegate> {
    WKWebView *webView;
    UILabel   *loadingLabel;
    CGFloat width;
    CGFloat height;
    CATransform3D transform;
    BOOL hidden;

    JavaVM *jvm;
    jobject jObject;
    jmethodID jmidLoadStarted;
    jmethodID jmidLoadFinished;
    jmethodID jmidLoadFailed;
    jmethodID jmidJavaCall;
}

@property (readwrite, retain) UIWindow *window;
@property (readwrite, retain) UIView   *windowView;

- (WebViewImpl *)create:(JNIEnv *)env :(jobject)object;
- (void)initWebViewImpl;
- (JNIEnv *)getJNIEnv;
- (void)releaseJNIEnv:(JNIEnv *)env;
- (void)setWidth:(CGFloat)value;
- (void)setHeight:(CGFloat)value;
- (void)loadUrl:(NSString *)value;
- (void)loadContent:(NSString *)content;
- (void)reload;
- (void)executeScript:(NSString *)script;
- (WKWebView *)getWebView;
- (UILabel *)getLoadingLabel;
- (UIWindow *)getWindow;
- (void) setFXTransform
        :(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
        :(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
        :(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt;
- (void) updateWebView;
- (void) updateTransform;
- (void) setHidden:(BOOL)value;

@end

