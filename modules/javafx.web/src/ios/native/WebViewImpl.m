/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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

#import "WebViewImpl.h"

#include "javafx_scene_web_WebView.h"
#include "javafx_scene_web_WebEngine.h"

#define JAR_PREFIX              @"jar:"
#define JAR_PATH_DELIMITER      @"!"
#define JAR_DIR_PREFIX          @"_"
#define JAR_DIR_SUFFIX          @"_Resources"
#define JAVA_CALL_PREFIX        @"javacall:"

#define PATH_DELIMITER          '/'

jint JNI_OnLoad_webview(JavaVM* vm, void * reserved) {
#ifdef JNI_VERSION_1_8
    //min. returned JNI_VERSION required by JDK8 for builtin libraries
    JNIEnv *env;
    if ((*vm)->GetEnv(vm, (void **)&env, JNI_VERSION_1_8) != JNI_OK) {
        return JNI_VERSION_1_4;
    }
    return JNI_VERSION_1_8;
#else
    return JNI_VERSION_1_4;
#endif
}

jstring createJString(JNIEnv *env, NSString *nsStr) {
    if (nsStr == nil) {
        return NULL;
    }
    const char *cString = [nsStr UTF8String];
    return (*env)->NewStringUTF(env, cString);
}


@implementation WebViewImpl

@synthesize window;     //known as mainWindow in glass
@synthesize windowView; //known as mainWindowHost in glass

- (void)setWidth:(CGFloat)value {
    width = value;
    [self updateWebView];
}

- (void)setHeight:(CGFloat)value {
    height = value;
    [self updateWebView];
}

- (void)loadUrl:(NSString *)value {
    NSURL *homeURL = [NSURL URLWithString:value];
    NSURLRequest *request = [[NSURLRequest alloc] initWithURL:homeURL];

    loadingLabel.text = [NSString stringWithFormat:@"Loading %@",
                        [[request URL] absoluteString]];

    loadingLabel.hidden = YES;

    [self updateWebView];
    [self updateTransform];

    [webView loadRequest:request];
    [request release];
}

- (void)loadContent:(NSString *)content {
    [webView loadHTMLString:content baseURL:nil];
}

- (void)reload {
    [webView reload];
}

- (void)executeScript:(NSMutableDictionary *) info {
    __block NSString *resultString = nil;
    __block BOOL finished = NO;
    [webView evaluateJavaScript:[info objectForKey:@"Script"] completionHandler:^(id result, NSError *error) {
        if (error == nil) {
            if (result != nil) {
                resultString = [NSString stringWithFormat:@"%@", result];
            }
        } else {
            NSLog(@"evaluateJavaScript error in executeScript: %@", error);
        }
        NSMutableDictionary *resultDictionary = [info objectForKey:@"ResultDictionary"];
        [resultDictionary setValue:resultString forKey:@"Result"];
        finished = YES;
    }];
    while (!finished) {
        [[NSRunLoop currentRunLoop] runMode:NSDefaultRunLoopMode beforeDate:[NSDate dateWithTimeIntervalSinceNow:0.1]];
    }
}

- (WebViewImpl *)create:(JNIEnv *)env :(jobject)object {
    self = [super init];
    transform = CATransform3DIdentity;
    (*env)->GetJavaVM(env, &jvm);
    jObject = (*env)->NewGlobalRef(env, object);
    jclass cls = (*env)->GetObjectClass(env, object);
    jmidLoadStarted = (*env)->GetMethodID(env, cls, "notifyLoadStarted", "()V");
    jmidLoadFinished = (*env)->GetMethodID(env, cls, "notifyLoadFinished", "(Ljava/lang/String;Ljava/lang/String;)V");
    jmidLoadFailed = (*env)->GetMethodID(env, cls, "notifyLoadFailed", "()V");
    jmidJavaCall = (*env)->GetMethodID(env, cls, "notifyJavaCall", "(Ljava/lang/String;)V");
    if (jmidLoadStarted == 0 || jmidLoadFinished == 0 || jmidLoadFailed == 0 || jmidJavaCall == 0) {
        NSLog(@"ERROR: could not get jmethodIDs: %d, %d, %d, %d",
                jmidLoadStarted, jmidLoadFinished, jmidLoadFailed, jmidJavaCall);
    }
    return self;
}

- (void) initWebViewImpl {
    CGRect screenBounds = [[UIScreen mainScreen] bounds];

    if (width <= 0) {
        width = screenBounds.size.width;
    }

    if (height <= 0) {
        height = screenBounds.size.height;
    }

    webView = [[WKWebView alloc] initWithFrame:CGRectMake(0, 0, width, height)];
    webView.userInteractionEnabled = YES;
    webView.navigationDelegate = self;
    //[webView.layer setAnchorPoint:CGPointMake(0.0f, 0.0f)];

    loadingLabel = [[UILabel alloc] initWithFrame:CGRectMake(0, height/2, width, 40)];
    loadingLabel.textAlignment = UITextAlignmentCenter;
    //[loadingLabel.layer setAnchorPoint:CGPointMake(0.0f, 0.0f)];

    window = [self getWindow];                          //known as mainWindow in glass
    windowView = [[window rootViewController] view];    //known as mainWindowHost in glass

    if (windowView) {
        [windowView addSubview:webView];
        // [windowView addSubview:loadingLabel];
    } else {
        NSLog(@"WebViewImpl ERROR: main Window is NIL");
    }
}

- (JNIEnv *)getJNIEnv {
    JNIEnv *env = NULL;
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        NSLog(@"ERROR: Cannot get JNIEnv on the thread!");
    }
    return env;
}

- (void)releaseJNIEnv:(JNIEnv *)env {
}

- (WKWebView *)getWebView {
    return webView;
}

- (UILabel *)getLoadingLabel {
    return loadingLabel;
}

- (UIWindow *)getWindow {
    if (!window) {
        UIApplication *app = [UIApplication sharedApplication];
        return [app keyWindow];
    }

    return window;
}

- (void) dealloc {
    webView.navigationDelegate = nil;
    [webView release];
    [loadingLabel release];
    JNIEnv *env = [self getJNIEnv];
    if (env != NULL) {
        (*env)->DeleteGlobalRef(env, jObject);
        [self releaseJNIEnv:env];
    }
    [super dealloc];
}

- (void)webView:(WKWebView *)wv decidePolicyForNavigationAction:(WKNavigationAction *)navigationAction
        decisionHandler:(void (^)(WKNavigationActionPolicy))decisionHandler {

    NSString *url = [navigationAction.request.URL absoluteString];
    if ([url hasPrefix:JAVA_CALL_PREFIX]) {
        JNIEnv *env = [self getJNIEnv];
        if (env != NULL) {
            jstring jUrl = createJString(env, url);
            (*env)->CallVoidMethod(env, jObject, jmidJavaCall, jUrl);
            (*env)->DeleteLocalRef(env, jUrl);
            [self releaseJNIEnv:env];
        }
        decisionHandler(WKNavigationActionPolicyCancel);
    } else {
        decisionHandler(WKNavigationActionPolicyAllow);
    }
}

- (void)webView:(WKWebView *)wv didStartProvisionalNavigation:(WKNavigation *)navigation {
    loadingLabel.hidden = hidden;
    [UIApplication sharedApplication].networkActivityIndicatorVisible = YES;

    JNIEnv *env = [self getJNIEnv];
    if (env != NULL) {
        (*env)->CallVoidMethod(env, jObject, jmidLoadStarted);
        [self releaseJNIEnv:env];
    }
}

- (void)webView:(WKWebView *)wv didFinishNavigation:(WKNavigation *)navigation {
    loadingLabel.hidden = YES;
    [UIApplication sharedApplication].networkActivityIndicatorVisible = NO;
    __block NSString *resultString = nil;
    [wv evaluateJavaScript:@"document.documentElement.innerHTML" completionHandler:^(id result, NSError *error) {
        if (error == nil) {
            if (result != nil) {
                resultString = [NSString stringWithFormat:@"%@", result];
            }
            JNIEnv *env = [self getJNIEnv];
            if (env != NULL) {
                jstring jInner = createJString(env, resultString);
                NSString *currentUrl = [wv.URL absoluteString];
                jstring jUrl = createJString(env, currentUrl);
                (*env)->CallVoidMethod(env, jObject, jmidLoadFinished, jUrl, jInner);
                [self releaseJNIEnv:env];
            }
        } else {
            NSLog(@"evaluateJavaScript error in didFinishNavigation: %@", error);
        }
    }];
}

- (void)webView:(WKWebView *)wv didFailNavigation:(WKNavigation *)navigation withError:(NSError *)error {
    NSLog(@"WebViewImpl ERROR: didFailLoadWithError");
    NSLog(@" this error => %@ ", [error userInfo] );
    JNIEnv *env = [self getJNIEnv];
    if (env != NULL) {
        (*env)->CallVoidMethod(env, jObject, jmidLoadFailed);
        [self releaseJNIEnv:env];
    }
}

- (void) updateWebView {
    CGRect bounds = webView.bounds;
    bounds.origin = CGPointMake(transform.m41, transform.m42);
    bounds.size.width = width;
    bounds.size.height = height;
    [webView setFrame:bounds];
//     [loadingLabel setCenter:center];
//     [loadingLabel setBounds:bounds];
    // add subview again if is not present
    if (![webView isDescendantOfView:windowView]) {
        [windowView addSubview:webView];
    }
}

- (void) updateTransform {
    [CATransaction begin];
    [CATransaction setAnimationDuration: 0];
    [CATransaction setDisableActions: YES];
    [webView.layer setTransform: transform];
    [loadingLabel.layer setTransform: transform];
    [CATransaction commit];
}

- (void) setFXTransform
        :(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
        :(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
        :(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt {

    transform.m11 = mxx;
    transform.m21 = mxy;
    transform.m31 = mxz;
    transform.m41 = mxt;

    transform.m12 = myx;
    transform.m22 = myy;
    transform.m32 = myz;
    transform.m42 = myt;

    transform.m13 = mzx;
    transform.m23 = mzy;
    transform.m33 = mzz;
    transform.m43 = mzt;

    if (webView) {
        [self performSelectorOnMainThread:@selector(updateTransform) withObject:nil waitUntilDone:NO];
    }
}

- (void) setHidden:(BOOL)value {
    hidden = value;
    [self performSelectorOnMainThread:@selector(applyHidden) withObject:nil waitUntilDone:NO];
}

- (void) applyHidden {
    loadingLabel.hidden = hidden;
    webView.hidden = hidden;
}

@end

unsigned int lastIndexOf(char searchChar,NSString * string) {
    NSRange searchRange;
    searchRange.location = (unsigned int) searchChar;
    searchRange.length = 1;

    NSRange foundRange = [string rangeOfCharacterFromSet:
                            [NSCharacterSet characterSetWithRange: searchRange]
                            options: NSBackwardsSearch];
    return foundRange.location;
}

NSString* bundleUrlFromJarUrl(NSString* jarUrlString) {
    NSString *bundlePath = @"";
    NSArray *jarUrlComponents = [jarUrlString componentsSeparatedByString: JAR_PATH_DELIMITER];

    // In the URL there must be exactly 1 exclamation mark, so after split there must be 2 components
    if ([jarUrlComponents count] == 2) {
        NSString *filePath = (NSString *) [jarUrlComponents lastObject];
        NSString *jarPath = (NSString *) [jarUrlComponents objectAtIndex: 0];

        unsigned int lastPathDelimiter = lastIndexOf(PATH_DELIMITER, jarPath);

        NSString *jarFileName = [jarPath substringFromIndex: lastPathDelimiter + 1];
        NSString *jarDirName = [JAR_DIR_PREFIX stringByAppendingString: jarFileName];
        jarDirName = [jarDirName stringByAppendingString: JAR_DIR_SUFFIX];

        filePath = [jarDirName stringByAppendingString: filePath];

        bundlePath = [[[NSBundle mainBundle] resourcePath]
                      stringByAppendingPathComponent: filePath];
    }

    return bundlePath;
}


#ifdef __cplusplus
extern "C" {
#endif
    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _initWebView
     * Signature: ([J)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1initWebView(JNIEnv *env, jobject obj, jlongArray nativeHandle) {
        WebViewImpl *wvi = [[WebViewImpl alloc] create: env : obj];
        [wvi performSelectorOnMainThread:@selector(initWebViewImpl) withObject:nil waitUntilDone:NO];

        jlong handle = ptr_to_jlong(wvi);
        (*env)->SetLongArrayRegion(env, nativeHandle, 0, 1, &handle);
    }

    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _setWidth
     * Signature: (JD)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1setWidth(JNIEnv *env, jobject cl, jlong handle, jdouble w) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi setWidth:w];
        }
    }

    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _setHeight
     * Signature: (JD)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1setHeight(JNIEnv *env, jobject cl, jlong handle, jdouble h) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi setHeight:h];
        }
    }

    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _setVisible
     * Signature: (JZ)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1setVisible(JNIEnv *env, jobject cl, jlong handle, jboolean v) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi setHidden:(v ? NO : YES)];
        }
    }

    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _setTransform
     * Signature: (JDDDDDDDDDDDD)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1setTransform(JNIEnv *env, jobject cl, jlong handle,
                                                 jdouble mxx, jdouble mxy, jdouble mxz, jdouble mxt,
                                                 jdouble myx, jdouble myy, jdouble myz, jdouble myt,
                                                 jdouble mzx, jdouble mzy, jdouble mzz, jdouble mzt) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi setFXTransform
                 :(CGFloat) mxx :(CGFloat) mxy :(CGFloat) mxz :(CGFloat) mxt
                :(CGFloat) myx :(CGFloat) myy :(CGFloat) myz :(CGFloat) myt
                :(CGFloat) mzx :(CGFloat) mzy :(CGFloat) mzz :(CGFloat) mzt];
        }
    }

    /*
     * Class:     javafx_scene_web_WebView
     * Method:    _removeWebView
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebView__1removeWebView(JNIEnv *env, jobject cl, jlong handle) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            UIView *view = [wvi getWebView];
            if (view)
                [view performSelectorOnMainThread:@selector(removeFromSuperview) withObject:nil waitUntilDone:NO];
            view = [wvi getLoadingLabel];
            if (view)
                [view performSelectorOnMainThread:@selector(removeFromSuperview) withObject:nil waitUntilDone:NO];
        }
    }

    /*
     * Class:     javafx_scene_web_WebEngine
     * Method:    _loadUrl
     * Signature: (JLjava/lang/String;)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebEngine__1loadUrl(JNIEnv *env, jobject cl, jlong handle, jstring str) {
        NSString *string = @"";
        if (str!= NULL)
        {
            const jchar* jstrChars = (*env)->GetStringChars(env, str, NULL);
            string = [[[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, str)] autorelease];
            (*env)->ReleaseStringChars(env, str, jstrChars);
        }

        if ([string hasPrefix:JAR_PREFIX]) {
            string = bundleUrlFromJarUrl(string);
        }

        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi performSelectorOnMainThread:@selector(loadUrl:) withObject:string waitUntilDone:NO];
        }
    }

    /*
     * Class:     javafx_scene_web_WebEngine
     * Method:    _loadContent
     * Signature: (JLjava/lang/String;)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebEngine__1loadContent(JNIEnv *env, jobject cl, jlong handle, jstring content) {
        NSString *string = @"";
        if (content!= NULL)
        {
            const jchar* jstrChars = (*env)->GetStringChars(env, content, NULL);
            string = [[[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, content)] autorelease];
            (*env)->ReleaseStringChars(env, content, jstrChars);
        }

        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi performSelectorOnMainThread:@selector(loadContent:) withObject:string waitUntilDone:NO];
        }
    }

    /*
     * Class:     javafx_scene_web_WebEngine
     * Method:    _reload
     * Signature: (J)V
     */
    JNIEXPORT void JNICALL
    Java_javafx_scene_web_WebEngine__1reload(JNIEnv *env, jobject cl, jlong handle) {
        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            [wvi reload];
        }
    }

    /*
     * Class:     javafx_scene_web_WebEngine
     * Method:    _executeScript
     * Signature: (JLjava/lang/String;)Ljava/lang/String;
     */
    JNIEXPORT jstring JNICALL
    Java_javafx_scene_web_WebEngine__1executeScript(JNIEnv *env, jobject cl, jlong handle, jstring script) {
        NSString *string = @"";
        if (script != NULL)
        {
            const jchar* jstrChars = (*env)->GetStringChars(env, script, NULL);
            string = [[[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, script)] autorelease];
            (*env)->ReleaseStringChars(env, script, jstrChars);
        }

        WebViewImpl *wvi = jlong_to_ptr(handle);
        if (wvi) {
            NSMutableDictionary *resultDictionary = [NSMutableDictionary dictionaryWithCapacity:1];
            NSDictionary *info = [NSDictionary dictionaryWithObjectsAndKeys:
                                  resultDictionary, @"ResultDictionary", string, @"Script", nil];
            [wvi performSelectorOnMainThread:@selector(executeScript:) withObject:info waitUntilDone:YES];

            NSString *result = [resultDictionary objectForKey:@"Result"];

            if (result != nil) {
                jsize resLength = [result length];
                jchar resBuffer[resLength];
                [result getCharacters:(unichar *)resBuffer];
                return (*env)->NewString(env, resBuffer, resLength);
            }
        }

        return NULL;
    }

#ifdef __cplusplus
}
#endif
