/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"
#include "JavaEnv.h"
#include "TestRunner.h"
#include "WorkQueue.h"
#include "WorkQueueItem.h"

#include <wtf/java/JavaRef.h>
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>

extern JSGlobalContextRef gContext;

jstring JSStringRef_to_jstring(JSStringRef ref, JNIEnv* env)
{
    size_t size = JSStringGetLength(ref);
    const JSChar* jschars = JSStringGetCharactersPtr(ref);
    return env->NewString((const jchar*)jschars, (jsize)size);
}

JSStringRef jstring_to_JSStringRef(jstring str, JNIEnv* env)
{
    jsize size = env->GetStringLength(str);
    const jchar* chars = env->GetStringCritical(str, NULL);
    JSStringRef ref = JSStringCreateWithCharacters((const JSChar*)chars, size);
    env->ReleaseStringCritical(str, chars);
    return ref;
}

bool LoadHTMLStringItem::invoke() const
{
    return false;
}


TestRunner::~TestRunner()
{
    // FIXME: implement
}

void TestRunner::addDisallowedURL(JSStringRef url)
{
    // FIXME: implement
}

void TestRunner::clearAllDatabases()
{
    // FIXME: implement
}

void TestRunner::clearBackForwardList()
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    env->CallStaticVoidMethod(getDumpRenderTreeClass(), getClearBackForwardListMID());
    CheckAndClearException(env);
}

void TestRunner::clearPersistentUserStyleSheet()
{
    // FIXME: implement
}

JSRetainPtr<JSStringRef> TestRunner::copyDecodedHostName(JSStringRef name)
{
    // FIXME: implement
    return 0;
}

JSRetainPtr<JSStringRef> TestRunner::copyEncodedHostName(JSStringRef name)
{
    // FIXME: implement
    return 0;
}

void TestRunner::dispatchPendingLoadRequests()
{
    // FIXME: implement
}

void TestRunner::display()
{
    // FIXME: implement
}

void TestRunner::displayAndTrackRepaints()
{
    // FIXME: implement
}

void TestRunner::execCommand(JSStringRef name, JSStringRef value)
{
    // FIXME: implement
}

bool TestRunner::isCommandEnabled(JSStringRef name)
{
    // FIXME: implement
    return false;
}

void TestRunner::keepWebHistory()
{
    // FIXME: implement
}

void TestRunner::notifyDone()
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    env->CallStaticVoidMethod(getDumpRenderTreeClass(), getNotifyDoneMID());
    CheckAndClearException(env);
}

void TestRunner::overridePreference(JSStringRef key, JSStringRef value)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    JLString jRelKey(JSStringRef_to_jstring(key, env));
    JLString jRelValue(JSStringRef_to_jstring(value, env));
    env->CallStaticVoidMethod(getDumpRenderTreeClass(), getOverridePreferenceMID(), (jstring)jRelKey, (jstring)jRelValue);
    CheckAndClearException(env);
}

void TestRunner::removeAllVisitedLinks()
{
    // FIXME: implement
}

JSRetainPtr<JSStringRef> TestRunner::pathToLocalResource(JSContextRef context, JSStringRef url)
{
    // Function introduced in r28690. This may need special-casing on Windows.
    return url; // Do nothing on Unix.
}

size_t TestRunner::webHistoryItemCount()
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    jint count = env->CallStaticIntMethod(getDumpRenderTreeClass(), getGetBackForwardItemCountMID());
    CheckAndClearException(env);
    return (size_t)count;
}

void TestRunner::queueLoad(JSStringRef url, JSStringRef target)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    JLString jRelUrl(JSStringRef_to_jstring(url, env));
    JLString jAbsUrl((jstring)env->CallStaticObjectMethod(getDumpRenderTreeClass(), getResolveURLMID(), (jstring)jRelUrl));
    CheckAndClearException(env);
    JSStringRef absUrlRef = jstring_to_JSStringRef((jstring)jAbsUrl, env);
    DRT::WorkQueue::singleton().queue(new LoadItem(absUrlRef, target));
}

void TestRunner::setAcceptsEditing(bool newAcceptsEditing)
{
    // FIXME: implement
}

void TestRunner::setAppCacheMaximumSize(unsigned long long quota)
{
    // FIXME: implement
}

void TestRunner::setAllowUniversalAccessFromFileURLs(bool)
{
    // FIXME: implement
}

void TestRunner::setAuthorAndUserStylesEnabled(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setCacheModel(int model)
{
    // FIXME: implement
}

void TestRunner::setCustomPolicyDelegate(bool setDelegate, bool permissive)
{
    // FIXME: implement
}

void TestRunner::setDatabaseQuota(unsigned long long quota)
{
    // FIXME: implement
}

void TestRunner::setIconDatabaseEnabled(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setMainFrameIsFirstResponder(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setMockGeolocationPosition(double latitude, double longitude, double accuracy, bool providesAltitude, double altitude, bool providesAltitudeAccuracy, double altitudeAccuracy, bool providesHeading, double heading, bool providesSpeed, double speed, bool providesFloorLevel, double floorLevel)
{
    // FIXME: implement
}

void TestRunner::setPersistentUserStyleSheetLocation(JSStringRef path)
{
    // FIXME: implement
}

void TestRunner::setPopupBlockingEnabled(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setPrivateBrowsingEnabled(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setXSSAuditorEnabled(bool enabled)
{
    // FIXME: implement
}

void TestRunner::setTabKeyCyclesThroughElements(bool cycles)
{
    // FIXME: implement
}

void TestRunner::setUserStyleSheetEnabled(bool flag)
{
    // FIXME: implement
}

void TestRunner::setUserStyleSheetLocation(JSStringRef path)
{
    // FIXME: implement
}

void TestRunner::waitForPolicyDelegate()
{
    // FIXME: implement
}

/*
unsigned TestRunner::workerThreadCount() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    static jmethodID workerThreadCountMID = env->GetStaticMethodID(getDRTClass(env), "getWorkerThreadCount", "()I");
    ASSERT(workerThreadCountMID);
    jint count = env->CallStaticIntMethod(getDRTClass(env), workerThreadCountMID);
    CheckAndClearException(env);
    return count;
}
*/

int TestRunner::windowCount()
{
    // FIXME: implement
    return 1;
}

void TestRunner::setWaitToDump(bool waitUntilDone)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    if (!waitUntilDone) {
        // FIXME: implement
        return;
    }

    env->CallStaticVoidMethod(getDumpRenderTreeClass(), getWaitUntillDoneMethodId());
    CheckAndClearException(env);
}

void TestRunner::setWindowIsKey(bool windowIsKey)
{
    // FIXME: implement
}

void TestRunner::setAlwaysAcceptCookies(bool alwaysAcceptCookies)
{
    // FIXME: implement
}


void TestRunner::addUserScript(JSStringRef source, bool runAtStart, bool allFrames)
{
    // FIXME: implement
}

void TestRunner::addUserStyleSheet(JSStringRef source, bool allFrames)
{
    // FIXME: implement
}

/*
void TestRunner::whiteListAccessFromOrigin(JSStringRef sourceOrigin,
                                                     JSStringRef destinationProtocol, JSStringRef destinationHost,
                                                     bool allowDestinationSubdomains)
{
    // FIXME: implement
}
*/

void TestRunner::showWebInspector()
{
    // FIXME: implement
}

void TestRunner::closeWebInspector()
{
    // FIXME: implement
}


void TestRunner::evaluateInWebInspector(JSStringRef script)
{
    // FIXME: implement
}

JSRetainPtr<JSStringRef> TestRunner::inspectorTestStubURL()
{
    // FIXME: Implement this to support Web Inspector tests using `protocol-test.js`.
    return nullptr;
}

void TestRunner::evaluateScriptInIsolatedWorld(unsigned worldId, JSObjectRef globalObject, JSStringRef script)
{
    // FIXME: implement
}

void TestRunner::abortModal()
{
    //FIXME: implement
}

void TestRunner::addOriginAccessAllowListEntry(
    JSStringRef sourceOrigin,
    JSStringRef destinationProtocol,
    JSStringRef destinationHost,
    bool allowDestinationSubdomains)
{
    //FIXME: implement
}

void TestRunner::apiTestGoToCurrentBackForwardItem()
{
    //FIXME: implement
}

void TestRunner::apiTestNewWindowDataLoadBaseURL(
    JSStringRef utf8Data,
    JSStringRef baseURL)
{
    //FIXME: implement
}

// Simulate a request an embedding application could make, populating per-session credential storage.
void TestRunner::authenticateSession(
    JSStringRef url,
    JSStringRef username,
    JSStringRef password)
{
    //FIXME: implement
}

bool TestRunner::callShouldCloseOnWebView()
{
    // FIXME: Implement for testing fix for https://bugs.webkit.org/show_bug.cgi?id=27481
    return false;
}

void TestRunner::removeOriginAccessAllowListEntry(JSStringRef, JSStringRef, JSStringRef,bool) {
    //FIXME: implement
}

void TestRunner::setAllowFileAccessFromFileURLs(bool) {
    //FIXME: implement
}

void TestRunner::setDomainRelaxationForbiddenForURLScheme(bool,JSStringRef) {
    //FIXME: implement
}

void TestRunner::setJavaScriptCanAccessClipboard(bool enable) {
    JSStringRef webkitJavaScriptCanAccessClipboard = JSStringCreateWithUTF8CString("WebKitJavaScriptCanAccessClipboardPreferenceKey");
    JSStringRef value = JSStringCreateWithUTF8CString(enable ? "1" : "0");
    overridePreference(webkitJavaScriptCanAccessClipboard, value);
    JSStringRelease(webkitJavaScriptCanAccessClipboard);
    JSStringRelease(value);
}

void TestRunner::setPluginsEnabled(bool) {
    //FIXME: implement
}

void TestRunner::setScrollbarPolicy(JSStringRef,JSStringRef) {
    //FIXME: implement
}

void TestRunner::setSpatialNavigationEnabled(bool) {
    //FIXME: implement
}

void TestRunner::setWebViewEditable(bool) {
    //FIXME: implement
}

bool TestRunner::findString(JSContextRef context, JSStringRef target, JSObjectRef optionsArray)
{
    //FIXME: implement
    return false;
}

void TestRunner::setSerializeHTTPLoads(bool)
{
    // FIXME: Implement if needed for https://bugs.webkit.org/show_bug.cgi?id=50758.
}

void TestRunner::clearAllApplicationCaches()
{
    // FIXME: implement to support Application Cache quotas.
}

void TestRunner::setGeolocationPermission(bool allow)
{
    // FIXME: Implement for Geolocation layout tests.
}

void TestRunner::setMockDeviceOrientation(bool canProvideAlpha, double alpha, bool canProvideBeta, double beta, bool canProvideGamma, double gamma)
{
    // FIXME: Implement for DeviceOrientation layout tests.
    // See https://bugs.webkit.org/show_bug.cgi?id=30335.
}

int TestRunner::numberOfPendingGeolocationPermissionRequests()
{
    // FIXME: Implement for Geolocation layout tests.
    return -1;
}

bool TestRunner::isGeolocationProviderActive()
{
    // FIXME: Implement for Geolocation layout tests.
    return false;
}


JSValueRef TestRunner::originsWithApplicationCache(JSContextRef context)
{
    // FIXME: Implement to get origins that contain application caches.
    return JSValueMakeUndefined(context);
}

void TestRunner::clearApplicationCacheForOrigin(JSStringRef origin)
{
    // FIXME: Implement to support deleting all application cache for an origin.
}

void TestRunner::setValueForUser(JSContextRef context, JSValueRef element, JSStringRef value)
{
    // FIXME: implement
}

void TestRunner::addChromeInputField()
{
}

void TestRunner::focusWebView()
{
}

void TestRunner::goBack()
{
}

void TestRunner::removeChromeInputField()
{
}

void TestRunner::setBackingScaleFactor(double)
{
}

void TestRunner::setDefersLoading(bool)
{
}

void TestRunner::setTextDirection(OpaqueJSString *)
{
}

long long TestRunner::applicationCacheDiskUsageForOrigin(JSStringRef)
{
    return 0;
}

void TestRunner::evaluateScriptInIsolatedWorldAndReturnValue(unsigned int,OpaqueJSValue *,OpaqueJSString *)
{
}

void TestRunner::resetPageVisibility()
{
}

void TestRunner::setAutomaticLinkDetectionEnabled(bool)
{
}

void TestRunner::setPageVisibility(char const *)
{
}

void TestRunner::setStorageDatabaseIdleInterval(double)
{
}

void TestRunner::setMockGeolocationPositionUnavailableError(JSStringRef message)
{
}

void TestRunner::simulateLegacyWebNotificationClick(JSStringRef title)
{
}

void TestRunner::closeIdleLocalStorageDatabases()
{
}

void TestRunner::grantWebNotificationPermission(JSStringRef origin)
{
}

void TestRunner::denyWebNotificationPermission(JSStringRef jsOrigin)
{
}

void TestRunner::removeAllWebNotificationPermissions()
{
}

void TestRunner::simulateWebNotificationClick(JSValueRef jsNotification)
{
}

JSContextRef TestRunner::mainFrameJSContext()
{
    return gContext;
}

void TestRunner::setViewSize(double width, double height)
{
    fprintf(testResult, "ERROR: TestRunner::setViewSize() not implemented\n");
}

void TestRunner::setSpellCheckerLoggingEnabled(bool enabled)
{
    fprintf(testResult, "ERROR: TestRunner::setSpellCheckerLoggingEnabled() not implemented\n");
}

void TestRunner::setNeedsStorageAccessFromFileURLsQuirk(bool needsQuirk)
{
    fprintf(testResult, "ERROR: TestRunner::setNeedsStorageAccessFromFileURLsQuirk() not implemented\n");
}

unsigned TestRunner::imageCountInGeneralPasteboard() const
{
    fprintf(testResult, "ERROR: TestRunner::imageCountInGeneralPasteboard() not implemented\n");
    return 0;
}

void TestRunner::forceImmediateCompletion()
{
    notifyDone();
}

void TestRunner::setOnlyAcceptFirstPartyCookies(bool)
{
    fprintf(testResult, "ERROR: TestRunner::setOnlyAcceptFirstPartyCookies() not implemented\n");
}
