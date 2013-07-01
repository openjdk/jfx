/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "JavaEnv.h"
#include "LayoutTestController.h"
#include "WorkQueue.h"
#include "WorkQueueItem.h"

#include <wtf/java/JavaRef.h>
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>

jclass getDRTClass(JNIEnv* env)
{
    static JGClass cls(env->FindClass("com/sun/javafx/webkit/drt/DumpRenderTree"));
    ASSERT(cls);
    return cls;
}

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


LayoutTestController::~LayoutTestController()
{
    // FIXME: implement
}

void LayoutTestController::addDisallowedURL(JSStringRef url)
{
    // FIXME: implement
}

void LayoutTestController::clearAllDatabases()
{
    // FIXME: implement
}

void LayoutTestController::clearBackForwardList()
{
    // FIXME: implement
}

void LayoutTestController::clearPersistentUserStyleSheet()
{
    // FIXME: implement
}

JSStringRef LayoutTestController::copyDecodedHostName(JSStringRef name)
{
    // FIXME: implement
    return 0;
}

JSStringRef LayoutTestController::copyEncodedHostName(JSStringRef name)
{
    // FIXME: implement
    return 0;
}

void LayoutTestController::disableImageLoading()
{
    // FIXME: implement
}

void LayoutTestController::dispatchPendingLoadRequests()
{
    // FIXME: implement
}

void LayoutTestController::display()
{
    // FIXME: implement
}

void LayoutTestController::execCommand(JSStringRef name, JSStringRef value)
{
    // FIXME: implement
}

bool LayoutTestController::isCommandEnabled(JSStringRef name)
{
    // FIXME: implement
    return false;
}

void LayoutTestController::keepWebHistory()
{
    // FIXME: implement
}

void LayoutTestController::notifyDone()
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    static jmethodID notifyDoneMID = env->GetStaticMethodID(getDRTClass(env), "notifyDone", "()V");
    ASSERT(notifyDoneMID);
    env->CallStaticVoidMethod(getDRTClass(env), notifyDoneMID);
    CheckAndClearException(env);
}

void LayoutTestController::overridePreference(JSStringRef key, JSStringRef value)
{
    // FIXME: implement
}

void LayoutTestController::removeAllVisitedLinks()
{
    // FIXME: implement
}

JSStringRef LayoutTestController::pathToLocalResource(JSContextRef context, JSStringRef url)
{
    // Function introduced in r28690. This may need special-casing on Windows.
    return url; // Do nothing on Unix.
}

size_t LayoutTestController::webHistoryItemCount()
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();
    static jmethodID getBackForwardItemCountMID = env->GetStaticMethodID(getDRTClass(env), "getBackForwardItemCount", "()I");
    ASSERT(getBackForwardItemCountMID);
    jint count = env->CallStaticIntMethod(getDRTClass(env), getBackForwardItemCountMID);
    CheckAndClearException(env);
    return (size_t)count;
}

void LayoutTestController::queueLoad(JSStringRef url, JSStringRef target)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    JLString jRelUrl(JSStringRef_to_jstring(url, env));

    static jmethodID resolveUrlMID = env->GetStaticMethodID(getDRTClass(env), "resolveURL", "(Ljava/lang/String;)Ljava/lang/String;");
    ASSERT(resolveUrlMID);

    JLString jAbsUrl((jstring)env->CallStaticObjectMethod(getDRTClass(env), resolveUrlMID, (jstring)jRelUrl));
    CheckAndClearException(env);

    JSStringRef absUrlRef = jstring_to_JSStringRef((jstring)jAbsUrl, env);

    WorkQueue::shared()->queue(new LoadItem(absUrlRef, target));
}

void LayoutTestController::setAcceptsEditing(bool newAcceptsEditing)
{
    // FIXME: implement
}

void LayoutTestController::setAppCacheMaximumSize(unsigned long long quota)
{
    // FIXME: implement
}

void LayoutTestController::setAllowUniversalAccessFromFileURLs(bool)
{
    // FIXME: implement
}

void LayoutTestController::setAuthorAndUserStylesEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setCacheModel(int model)
{
    // FIXME: implement
}

void LayoutTestController::setCustomPolicyDelegate(bool setDelegate, bool permissive)
{
    // FIXME: implement
}

void LayoutTestController::setDatabaseQuota(unsigned long long quota)
{
    // FIXME: implement
}

void LayoutTestController::setIconDatabaseEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setMainFrameIsFirstResponder(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setMockGeolocationPosition(double latitude, double longitude, double accuracy)
{
    // FIXME: implement
}

void LayoutTestController::setMockGeolocationError(int code, JSStringRef message)
{
    // FIXME: implement
}

void LayoutTestController::setPersistentUserStyleSheetLocation(JSStringRef path)
{
    // FIXME: implement
}

void LayoutTestController::setPopupBlockingEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setPrivateBrowsingEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setXSSAuditorEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setSelectTrailingWhitespaceEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setSmartInsertDeleteEnabled(bool enabled)
{
    // FIXME: implement
}

void LayoutTestController::setTabKeyCyclesThroughElements(bool cycles)
{
    // FIXME: implement
}

void LayoutTestController::setUseDashboardCompatibilityMode(bool flag)
{
    // FIXME: implement
}

void LayoutTestController::setUserStyleSheetEnabled(bool flag)
{
    // FIXME: implement
}

void LayoutTestController::setUserStyleSheetLocation(JSStringRef path)
{
    // FIXME: implement
}

void LayoutTestController::waitForPolicyDelegate()
{
    // FIXME: implement
}

unsigned LayoutTestController::workerThreadCount() const
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    static jmethodID workerThreadCountMID = env->GetStaticMethodID(getDRTClass(env), "getWorkerThreadCount", "()I");
    ASSERT(workerThreadCountMID);
    jint count = env->CallStaticIntMethod(getDRTClass(env), workerThreadCountMID);
    CheckAndClearException(env);
    return count;
}

int LayoutTestController::windowCount()
{
    // FIXME: implement
    return 1;
}

void LayoutTestController::setWaitToDump(bool waitUntilDone)
{
    JNIEnv* env = DumpRenderTree_GetJavaEnv();

    if (!waitUntilDone) {
        // FIXME: implement
        return;
    }

    static jmethodID notifyDoneMID = env->GetStaticMethodID(getDRTClass(env), "waitUntilDone", "()V");
    ASSERT(notifyDoneMID);
    env->CallStaticVoidMethod(getDRTClass(env), notifyDoneMID);
    CheckAndClearException(env);
}

void LayoutTestController::setWindowIsKey(bool windowIsKey)
{
    // FIXME: implement
}

void LayoutTestController::setAlwaysAcceptCookies(bool alwaysAcceptCookies)
{
    // FIXME: implement
}

bool LayoutTestController::pauseAnimationAtTimeOnElementWithId(JSStringRef animationName, double time, JSStringRef elementId)
{
    // FIXME: implement
    return false;
}

bool LayoutTestController::pauseTransitionAtTimeOnElementWithId(JSStringRef propertyName, double time, JSStringRef elementId)
{
    // FIXME: implement
    return false;
}

unsigned LayoutTestController::numberOfActiveAnimations() const
{
    // FIXME: implement
    return 0;
}

void LayoutTestController::addUserScript(JSStringRef source, bool runAtStart, bool allFrames)
{
    // FIXME: implement
}

void LayoutTestController::addUserStyleSheet(JSStringRef source, bool allFrames)
{
    // FIXME: implement
}

/*
void LayoutTestController::whiteListAccessFromOrigin(JSStringRef sourceOrigin,
                                                     JSStringRef destinationProtocol, JSStringRef destinationHost,
                                                     bool allowDestinationSubdomains)
{
    // FIXME: implement
}
*/

void LayoutTestController::showWebInspector()
{
    // FIXME: implement
}

void LayoutTestController::closeWebInspector()
{
    // FIXME: implement
}


void LayoutTestController::evaluateInWebInspector(long callId, JSStringRef script)
{
    // FIXME: implement
}

void LayoutTestController::evaluateScriptInIsolatedWorld(unsigned worldId, JSObjectRef globalObject, JSStringRef script)
{
    // FIXME: implement
}

void LayoutTestController::abortModal() 
{
    //FIXME: implement
}

void LayoutTestController::addOriginAccessWhitelistEntry(
    JSStringRef sourceOrigin, 
    JSStringRef destinationProtocol, 
    JSStringRef destinationHost, 
    bool allowDestinationSubdomains)
{
    //FIXME: implement
}

void LayoutTestController::apiTestGoToCurrentBackForwardItem() 
{
    //FIXME: implement
}

void LayoutTestController::apiTestNewWindowDataLoadBaseURL(
    JSStringRef utf8Data, 
    JSStringRef baseURL)
{
    //FIXME: implement
}

// Simulate a request an embedding application could make, populating per-session credential storage.
void LayoutTestController::authenticateSession(
    JSStringRef url, 
    JSStringRef username, 
    JSStringRef password)
{
    //FIXME: implement
}

bool LayoutTestController::callShouldCloseOnWebView() 
{
    // FIXME: Implement for testing fix for https://bugs.webkit.org/show_bug.cgi?id=27481
    return false;
}

JSValueRef LayoutTestController::computedStyleIncludingVisitedInfo(JSContextRef context, JSValueRef)
{
    //FIXME: implement
    return JSValueMakeUndefined(context);
}

JSRetainPtr<JSStringRef> LayoutTestController::layerTreeAsText() const 
{
    //FIXME: implement
    return NULL;
}

JSRetainPtr<JSStringRef> LayoutTestController::markerTextForListItem(JSContextRef,JSValueRef) const 
{
    //FIXME: implement
    return NULL;
}

int LayoutTestController::numberOfPages(float,float) 
{
    //FIXME: implement
    return -1;
}

int LayoutTestController::pageNumberForElementById(JSStringRef,float,float) 
{
    //FIXME: implement
    return -1;
}

JSRetainPtr<JSStringRef> LayoutTestController::pageProperty(char const *,int) const 
{
    //FIXME: implement
    return NULL;
}

JSRetainPtr<JSStringRef> LayoutTestController::pageSizeAndMarginsInPixels(int,int,int,int,int,int,int) const 
{
    //FIXME: implement
    return NULL;
}

void LayoutTestController::removeOriginAccessWhitelistEntry(JSStringRef, JSStringRef, JSStringRef,bool) {
    //FIXME: implement
}

void LayoutTestController::setAllowFileAccessFromFileURLs(bool) {
    //FIXME: implement
}

void LayoutTestController::setDomainRelaxationForbiddenForURLScheme(bool,JSStringRef) {
    //FIXME: implement
}

void LayoutTestController::setFrameFlatteningEnabled(bool) {
    //FIXME: implement
}

void LayoutTestController::setJavaScriptCanAccessClipboard(bool) {
    //FIXME: implement
}

void LayoutTestController::setPluginsEnabled(bool) {
    //FIXME: implement
}

void LayoutTestController::setScrollbarPolicy(JSStringRef,JSStringRef) {
    //FIXME: implement
}

void LayoutTestController::setSpatialNavigationEnabled(bool) {
    //FIXME: implement
}

void LayoutTestController::setWebViewEditable(bool) {
    //FIXME: implement
}

bool LayoutTestController::findString(JSContextRef context, JSStringRef target, JSObjectRef optionsArray)
{
    //FIXME: implement
    return false;
}

void LayoutTestController::setSerializeHTTPLoads(bool)
{
    // FIXME: Implement if needed for https://bugs.webkit.org/show_bug.cgi?id=50758.
}

void LayoutTestController::addMockSpeechInputResult(JSStringRef result, double confidence, JSStringRef language)
{
    // FIXME: Implement for speech input layout tests.
    // See https://bugs.webkit.org/show_bug.cgi?id=39485.
}

void LayoutTestController::clearAllApplicationCaches()
{
    // FIXME: implement to support Application Cache quotas.
}


//void LayoutTestController::dumpConfigurationForViewport(int /*deviceDPI*/, int /*deviceWidth*/, int /*deviceHeight*/, int /*availableWidth*/, int /*availableHeight*/)
//{
//    // FIXME: Implement this.
//}

void LayoutTestController::setApplicationCacheOriginQuota(unsigned long long quota)
{
    // FIXME: implement to support Application Cache quotas.
}

void LayoutTestController::setAsynchronousSpellCheckingEnabled(bool)
{
    // FIXME: Implement this.
}

void LayoutTestController::setGeolocationPermission(bool allow)
{
    // FIXME: Implement for Geolocation layout tests.
}

void LayoutTestController::setMinimumTimerInterval(double minimumTimerInterval)
{
    // FIXME: Implement this.
}

void LayoutTestController::setMockDeviceOrientation(bool canProvideAlpha, double alpha, bool canProvideBeta, double beta, bool canProvideGamma, double gamma)
{
    // FIXME: Implement for DeviceOrientation layout tests.
    // See https://bugs.webkit.org/show_bug.cgi?id=30335.
}

void LayoutTestController::setViewModeMediaFeature(JSStringRef mode)
{
    // FIXME: implement
}

int LayoutTestController::numberOfPendingGeolocationPermissionRequests()
{
    // FIXME: Implement for Geolocation layout tests.
    return -1;
}

JSValueRef LayoutTestController::originsWithApplicationCache(JSContextRef context)
{
    // FIXME: Implement to get origins that contain application caches.
    return JSValueMakeUndefined(context);
}

JSValueRef LayoutTestController::originsWithLocalStorage(JSContextRef context)
{
    // FIXME: implement
    return JSValueMakeUndefined(context);
}

void LayoutTestController::clearApplicationCacheForOrigin(JSStringRef origin)
{
    // FIXME: Implement to support deleting all application cache for an origin.
}

void LayoutTestController::deleteAllLocalStorage()
{
    // FIXME: Implement.
}

void LayoutTestController::deleteLocalStorageForOrigin(JSStringRef URL)
{
    // FIXME: Implement.
}

void LayoutTestController::observeStorageTrackerNotifications(unsigned number)
{
    // FIXME: Implement.
}

void LayoutTestController::setAutofilled(JSContextRef context, JSValueRef nodeObject, bool isAutofilled)
{
    // FIXME: Implement.
}

void LayoutTestController::setValueForUser(JSContextRef context, JSValueRef element, JSStringRef value)
{
    // FIXME: implement
}

void LayoutTestController::syncLocalStorage()
{
    // FIXME: Implement.
}

void LayoutTestController::addChromeInputField()
{
}

void LayoutTestController::focusWebView()
{
}

void LayoutTestController::goBack()
{
}

void LayoutTestController::removeChromeInputField()
{
}

void LayoutTestController::setBackingScaleFactor(double)
{
}

void LayoutTestController::setDefersLoading(bool)
{
}

void LayoutTestController::setTextDirection(OpaqueJSString *)
{
}

long long LayoutTestController::applicationCacheDiskUsageForOrigin(JSStringRef)
{
    return 0;
}

long long LayoutTestController::localStorageDiskUsageForOrigin(JSStringRef)
{
    // FIXME: Implement to support getting disk usage in bytes for an origin.
    return 0;
}

bool LayoutTestController::elementDoesAutoCompleteForElementWithId(JSStringRef)
{
    return false;
}

void LayoutTestController::deliverWebIntent(OpaqueJSString *,OpaqueJSString *,OpaqueJSString *) 
{
}

void LayoutTestController::evaluateScriptInIsolatedWorldAndReturnValue(unsigned int,OpaqueJSValue *,OpaqueJSString *)
{
}

void LayoutTestController::resetPageVisibility()
{
}

void LayoutTestController::sendWebIntentResponse(OpaqueJSString *)
{
}

void LayoutTestController::setAutomaticLinkDetectionEnabled(bool)
{
}

void LayoutTestController::setMockSpeechInputDumpRect(bool)
{
}

void LayoutTestController::setPageVisibility(char const *)
{
}

void LayoutTestController::simulateDesktopNotificationClick(OpaqueJSString *)
{
}

void LayoutTestController::setStorageDatabaseIdleInterval(double)
{
}
