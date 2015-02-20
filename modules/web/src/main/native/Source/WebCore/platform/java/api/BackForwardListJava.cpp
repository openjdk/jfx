/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Frame.h"
#include "FrameLoader.h"
#include "BackForwardList.h"
#include "BackForwardController.h"
#include "HistoryItem.h"
#include "Image.h"
#include "JavaEnv.h"
#include "Page.h"
#include "WebPage.h"

using namespace WebCore;

#ifdef __cplusplus
extern "C" {
#endif

static Page* getPage(jlong jpage)
{
    return WebPage::pageFromJLong(jpage);
}

static BackForwardList* getBfl(jlong jpage)
{
    return static_cast<WebCore::BackForwardList*>(getPage(jpage)->backForward().client());
}

static HistoryItem* getItem(jlong jitem)
{
    return static_cast<HistoryItem*>(jlong_to_ptr(jitem));
}

static jmethodID initMethod(JNIEnv* env, jclass cls, char* name, char* signature)
{
    jmethodID mid = env->GetMethodID(cls, name, signature);
    ASSERT(mid);
    return mid;
}

static jmethodID initCtor(JNIEnv* env, jclass cls, char* signature)
{
    return initMethod(env, cls, "<init>", signature);
}

// ENTRY-RELATED METHODS

static jclass getJEntryClass()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static JGClass jEntryClass(env->FindClass("com/sun/webkit/BackForwardList$Entry"));
    ASSERT(jEntryClass);

    return jEntryClass;
}

static jclass getJBFLClass()
{
    JNIEnv* env = WebCore_GetJavaEnv();

    static JGClass jBFLClass(env->FindClass("com/sun/webkit/BackForwardList"));
    ASSERT(jBFLClass);

    return jBFLClass;
}

static JLObject createEntry(HistoryItem* item, jlong jpage)
{

    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID entryCtorMID = initCtor(env, getJEntryClass(), "(JJ)V");

    JLObject jEntry(env->NewObject(getJEntryClass(), entryCtorMID, ptr_to_jlong(item), jpage));
    CheckAndClearException(env);

    item->setHostObject(jEntry);

    return jEntry;
}

static void notifyHistoryItemChangedImpl(HistoryItem* item) {
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID notifyItemChangedMID = initMethod(env, getJEntryClass(), "notifyItemChanged", "()V");
    if (item->hostObject()) {
        env->CallVoidMethod(item->hostObject(), notifyItemChangedMID);
        CheckAndClearException(env);
    }
}

void notifyHistoryItemDestroyed(const JLObject &host)
{
    JNIEnv* env = WebCore_GetJavaEnv();
    static jmethodID notifyItemDestroyedMID =
            initMethod(env, getJEntryClass(), "notifyItemDestroyed", "()V");
    if (host) {
        env->CallVoidMethod(host, notifyItemDestroyedMID);
        CheckAndClearException(env);
    }
}

// entry.getURL()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetURL(JNIEnv* env, jclass z, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    String urlString = item->urlString();
    return urlString.toJavaString(env).releaseLocal();
}

// entry.getTitle()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetTitle(JNIEnv* env, jclass z, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    String title = item->title();
    return title.toJavaString(env).releaseLocal();
}

// entry.getIcon()
JNIEXPORT jobject JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetIcon(JNIEnv* env, jclass z, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
/*
    if (item != NULL) {
	// TODO: crashes with DRT
        return *WebCore::iconDatabase().synchronousIconForPageURL(item->url(), WebCore::IntSize(16, 16))->nativeImageForCurrentFrame();
        Image* icon = item->icon();
        if (icon != NULL) {
            return *icon->javaImage();
        }
    }
*/
    return NULL;
}

// entry.getLastVisited()
JNIEXPORT jlong JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetLastVisitedDate(JNIEnv* env, jclass z, jlong jitem)
{
//    HistoryItem* item = getItem(jitem);
//    double lastVisitedDate = item->lastVisitedTime();
//    return (jlong) (lastVisitedDate * 1000);
    return 0; // todo tav where is lastVisitedDate field?
}

// entry.isTargetItem()
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_BackForwardList_bflItemIsTargetItem(JNIEnv* env, jclass z, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    return (jboolean)item->isTargetItem();
}

// entry.getTarget()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetTarget(JNIEnv* env, jclass z, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    String target = item->target();
    if (!target.isEmpty()) {
        return target.toJavaString(env).releaseLocal();
    } else {
        return NULL;
    }
}

// entry.getChildren()
JNIEXPORT jobjectArray JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetChildren(JNIEnv* env, jclass z, jlong jitem, jlong jpage)
{
    HistoryItem* item = getItem(jitem);
    if (!item->hasChildren()) {
        return NULL;
    }
    jobjectArray children = env->NewObjectArray(item->children().size(), getJEntryClass(), NULL);
    int i = 0;
    for (HistoryItemVector::const_iterator it = item->children().begin();
	 it != item->children().end();
	 ++it)
    {
        env->SetObjectArrayElement(children, i++, (jobject)createEntry(&**it, jpage));
    }
    return children;
}

// BACKFORWARDLIST METHODS

static int getSize(BackForwardList* bfl)
{
    int size = 0;
    if (bfl->currentItem())
        size = bfl->forwardListCount() + bfl->backListCount() + 1;
    return size;
}

// BackForwardList.size()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflSize(JNIEnv* env, jclass z, jlong jpage)
{
    return getSize(getBfl(jpage));
}

// BackForwardList.getMaximumSize()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflGetMaximumSize(JNIEnv* env, jclass z, jlong jpage)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    return bfl->capacity();
}

// BackForwardList.setMaximumSize()
JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetMaximumSize(JNIEnv* env, jclass z, jlong jpage, jint size)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    bfl->setCapacity(size);
}

// BackForwardList.getCurrentIndex()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflGetCurrentIndex(JNIEnv* env, jclass z, jlong jpage)
{
    BackForwardList* bfl = getBfl(jpage);
    return bfl->currentItem() ? bfl->backListCount() : -1;
}

// BackForwardList.setEnabled()
JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetEnabled(JNIEnv* env, jclass z, jlong jpage, jboolean flag)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    bfl->setEnabled(flag);
}

// BackForwardList.isEnabled()
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_BackForwardList_bflIsEnabled(JNIEnv* env, jclass z, jlong jpage)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    return bfl->enabled();
}

static HistoryItem* itemAtIndex(BackForwardList* bfl, int index)
{
    // Note: WebKit counts from the *current* position
    return bfl->itemAtIndex(index - bfl->backListCount());
}

// BackForwardList.get()
JNIEXPORT jobject JNICALL Java_com_sun_webkit_BackForwardList_bflGet(JNIEnv* env, jclass z, jlong jpage, jint index)
{
    BackForwardList* bfl = getBfl(jpage);
    HistoryItem* item = itemAtIndex(bfl, index);
    if (!item)
        return 0;
    JLObject host(item->hostObject());
    if (!host) {
        host = createEntry(item, jpage);
    }
    return host.releaseLocal();
}

// BackForwardList.setCurrentIndex()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflSetCurrentIndex(JNIEnv* env, jclass z, jlong jpage, jint index)
{
    Page* page = getPage(jpage);
    BackForwardList* bfl = static_cast<BackForwardList*>(page->backForward().client());
    if (index < 0 || index >= getSize(bfl))
        return -1;
    int distance = index - bfl->backListCount();
    page->backForward().goBackOrForward(distance);
    return index;
}

// BackForwardList.get[Last]IndexOf()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflIndexOf(JNIEnv* env, jclass z, jlong jpage, jlong jitem, jboolean reverse)
{
    if (!jitem)
        return -1;
    BackForwardList* bfl = getBfl(jpage);
    int size = getSize(bfl);
    int start = reverse ? size - 1 : 0;
    int end = reverse ? -1 : size;
    int inc = reverse ? -1 : 1;
    HistoryItem* item = static_cast<HistoryItem*>(jlong_to_ptr(jitem));
    for (int i = start; i != end; i += inc)
        if (item == itemAtIndex(bfl, i))
            return i;
    return -1;
}

JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetHostObject(JNIEnv* env, jclass z, jlong jpage, jobject host)
{
    BackForwardList* bfl = getBfl(jpage);
    bfl->setHostObject(JLObject(host, true));

    notifyHistoryItemChanged = notifyHistoryItemChangedImpl;
}

#ifdef __cplusplus
}
#endif

namespace WebCore {

// ChangeListener support
void notifyBackForwardListChanged(const JLObject &host)
{
    JNIEnv* env = WebCore_GetJavaEnv();

    if (!host) {
        return;
    }

    static jmethodID notifyChangedMID = initMethod(
        env,
	getJBFLClass(),
        "notifyChanged",
        "()V");
    ASSERT(notifyChangedMID);

    env->CallVoidMethod(host, notifyChangedMID);
    CheckAndClearException(env);
}

} // namespace WebCore
