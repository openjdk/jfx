/*
 * Copyright (C) 2005, 2006 Apple Inc.  All rights reserved.
 * Copyright (C) 2008 Torch Mobile Inc. All rights reserved. (http://www.torchmobile.com/)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "BackForwardList.h"

#include <WebCore/BackForwardCache.h>
#include <WebCore/BackForwardController.h>
#include <WebCore/Frame.h>
#include <WebCore/FrameLoader.h>
#include <WebCore/FrameLoaderClient.h>
#include <WebCore/HistoryItem.h>
#include <WebCore/Logging.h>
#include <WebCore/Page.h>
#include <WebCore/SerializedScriptValue.h>

#include "BackForwardList.h"
#include "WebPage.h"
#include "PlatformJavaClasses.h"

static const unsigned DefaultCapacity = 100;
static const unsigned NoCurrentItemIndex = UINT_MAX;

using namespace WebCore;

extern "C" {

namespace {

Page* getPage(jlong jpage)
{
    return WebPage::pageFromJLong(jpage);
}

BackForwardList* getBfl(jlong jpage)
{
    return &static_cast<BackForwardList&>(getPage(jpage)->backForward().client());
}

HistoryItem* getItem(jlong jitem)
{
    return static_cast<HistoryItem*>(jlong_to_ptr(jitem));
}

jmethodID initMethod(JNIEnv* env, jclass cls, const char* name, const char* signature)
{
    jmethodID mid = env->GetMethodID(cls, name, signature);
    ASSERT(mid);
    return mid;
}

jmethodID initCtor(JNIEnv* env, jclass cls, const char* signature)
{
    return initMethod(env, cls, "<init>", signature);
}

// ENTRY-RELATED METHODS

jclass getJEntryClass()
{
    JNIEnv* env = WTF::GetJavaEnv();

    static JGClass jEntryClass(env->FindClass("com/sun/webkit/BackForwardList$Entry"));
    ASSERT(jEntryClass);

    return jEntryClass;
}

jclass getJBFLClass()
{
    JNIEnv* env = WTF::GetJavaEnv();

    static JGClass jBFLClass(env->FindClass("com/sun/webkit/BackForwardList"));
    ASSERT(jBFLClass);

    return jBFLClass;
}

static JLObject createEntry(HistoryItem* item, jlong jpage)
{

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID entryCtorMID = initCtor(env, getJEntryClass(), "(JJ)V");

    JLObject jEntry(env->NewObject(getJEntryClass(), entryCtorMID, ptr_to_jlong(item), jpage));
    WTF::CheckAndClearException(env);

    item->setHostObject(jEntry);

    return jEntry;
}

void historyItemChangedImpl(HistoryItem& item) {
    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID notifyItemChangedMID = initMethod(env, getJEntryClass(), "notifyItemChanged", "()V");
    if (item.hostObject()) {
        env->CallVoidMethod(item.hostObject(), notifyItemChangedMID);
        WTF::CheckAndClearException(env);
    }
}

// BACKFORWARDLIST METHODS
int getSize(BackForwardList* bfl)
{
    int size = 0;
    if (bfl->currentItem())
        size = bfl->forwardListCount() + bfl->backListCount() + 1;
    return size;
}

HistoryItem* itemAtIndex(BackForwardList* bfl, int index)
{
    // Note: WebKit counts from the *current* position
    return bfl->itemAtIndex(index - bfl->backListCount()).get();
}

// ChangeListener support
void notifyBackForwardListChanged(const JLObject &host)
{
    JNIEnv* env = WTF::GetJavaEnv();

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
    WTF::CheckAndClearException(env);
}
} // namespace

void notifyHistoryItemDestroyed(const JLObject &host)
{
    WC_GETJAVAENV_CHKRET(env);
    static jmethodID notifyItemDestroyedMID =
            initMethod(env, getJEntryClass(), "notifyItemDestroyed", "()V");
    if (host) {
        env->CallVoidMethod(host, notifyItemDestroyedMID);
        WTF::CheckAndClearException(env);
    }
}

// entry.getURL()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetURL(JNIEnv* env, jclass, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    String urlString = item->urlString();
    return urlString.toJavaString(env).releaseLocal();
}

// entry.getTitle()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetTitle(JNIEnv* env, jclass, jlong jitem)
{
    String title= ""_s;
    return title.toJavaString(env).releaseLocal();

}

// entry.getIcon()
JNIEXPORT jobject JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetIcon(JNIEnv*, jclass, jlong)
{
/*
    HistoryItem* item = getItem(jitem);
    if (item != nullptr) {
    // TODO: crashes with DRT
        return *WebCore::iconDatabase().synchronousIconForPageURL(item->url(), WebCore::IntSize(16, 16))->nativeImageForCurrentFrame();
        Image* icon = item->icon();
        if (icon != nullptr) {
            return *icon->javaImage();
        }
    }
*/
    return nullptr;
}

// entry.getLastVisited()
JNIEXPORT jlong JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetLastVisitedDate(JNIEnv*, jclass, jlong)
{
//    HistoryItem* item = getItem(jitem);
//    double lastVisitedDate = item->lastVisitedTime();
//    return (jlong) (lastVisitedDate * 1000);
    return 0; // todo tav where is lastVisitedDate field?
}

// entry.isTargetItem()
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_BackForwardList_bflItemIsTargetItem(JNIEnv*, jclass, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    return (jboolean)item->isTargetItem();
}

// entry.getTarget()
JNIEXPORT jstring JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetTarget(JNIEnv* env, jclass, jlong jitem)
{
    HistoryItem* item = getItem(jitem);
    String target = item->target();
    if (!target.isEmpty()) {
        return target.toJavaString(env).releaseLocal();
    } else {
        return nullptr;
    }
}

JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflClearBackForwardListForDRT(JNIEnv*, jclass, jlong jpage)
{
    BackForwardList* bfl = getBfl(jpage);
    RefPtr<HistoryItem> current = bfl->currentItem();
    int capacity = bfl->capacity();
    bfl->setCapacity(0);
    bfl->setCapacity(capacity);
    bfl->addItem(*current);
    bfl->goToItem(*current);
}

// entry.getChildren()
JNIEXPORT jobjectArray JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetChildren(JNIEnv* env, jclass, jlong jitem, jlong jpage)
{
    HistoryItem* item = getItem(jitem);
    if (!item->hasChildren()) {
        return nullptr;
    }
    jobjectArray children = env->NewObjectArray(item->children().size(), getJEntryClass(), nullptr);
    int i = 0;
    for (const auto& it : item->children()) {
        env->SetObjectArrayElement(children, i++, (jobject)createEntry(&it.get(), jpage));
    }
    return children;
}

// BackForwardList.size()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflSize(JNIEnv*, jclass, jlong jpage)
{
    return getSize(getBfl(jpage));
}

// BackForwardList.getMaximumSize()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflGetMaximumSize(JNIEnv*, jclass, jlong jpage)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    return bfl->capacity();
}

// BackForwardList.setMaximumSize()
JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetMaximumSize(JNIEnv*, jclass, jlong jpage, jint size)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    bfl->setCapacity(size);
}

// BackForwardList.getCurrentIndex()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflGetCurrentIndex(JNIEnv*, jclass, jlong jpage)
{
    BackForwardList* bfl = getBfl(jpage);
    return bfl->currentItem() ? bfl->backListCount() : -1;
}

// BackForwardList.setEnabled()
JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetEnabled(JNIEnv*, jclass, jlong jpage, jboolean flag)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    bfl->setEnabled(flag);
}

// BackForwardList.isEnabled()
JNIEXPORT jboolean JNICALL Java_com_sun_webkit_BackForwardList_bflIsEnabled(JNIEnv*, jclass, jlong jpage)
{
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(jpage));
    return bfl->enabled();
}

// BackForwardList.get()
JNIEXPORT jobject JNICALL Java_com_sun_webkit_BackForwardList_bflGet(JNIEnv*, jclass, jlong jpage, jint index)
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
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflSetCurrentIndex(JNIEnv*, jclass, jlong jpage, jint index)
{
    Page* page = getPage(jpage);
    BackForwardList* bfl = &static_cast<BackForwardList&>(page->backForward().client());
    if (index < 0 || index >= getSize(bfl))
        return -1;
    int distance = index - bfl->backListCount();
    page->backForward().goBackOrForward(distance);
    return index;
}

// BackForwardList.get[Last]IndexOf()
JNIEXPORT jint JNICALL Java_com_sun_webkit_BackForwardList_bflIndexOf(JNIEnv*, jclass, jlong jpage, jlong jitem, jboolean reverse)
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

JNIEXPORT void JNICALL Java_com_sun_webkit_BackForwardList_bflSetHostObject(JNIEnv*, jclass, jlong jpage, jobject host)
{
    BackForwardList* bfl = getBfl(jpage);
    bfl->setHostObject(JLObject(host, true));

    //notifyHistoryItemChanged = historyItemChangedImpl;//Check 4ef4b65d33f45734ad3c6cbc7f2fe0dda17051bc for more details
}

}

BackForwardList::BackForwardList()
    : m_current(NoCurrentItemIndex)
    , m_capacity(DefaultCapacity)
    , m_closed(true)
    , m_enabled(true)
{
}

BackForwardList::~BackForwardList()
{
    ASSERT(m_closed);
}

void BackForwardList::addItem(Ref<HistoryItem>&& newItem)
{
    if (!m_capacity || !m_enabled)
        return;

    // Toss anything in the forward list
    if (m_current != NoCurrentItemIndex) {
        unsigned targetSize = m_current + 1;
        while (m_entries.size() > targetSize) {
            Ref<HistoryItem> item = m_entries.takeLast();
            m_entryHash.remove(item.ptr());
            BackForwardCache::singleton().remove(item);
        }
    }

    // Toss the first item if the list is getting too big, as long as we're not using it
    // (or even if we are, if we only want 1 entry).
    if (m_entries.size() == m_capacity && (m_current || m_capacity == 1)) {
        Ref<HistoryItem> item = WTFMove(m_entries[0]);
        m_entries.remove(0);
        m_entryHash.remove(item.ptr());
        BackForwardCache::singleton().remove(item);
        --m_current;
    }

    m_entryHash.add(newItem.ptr());
    m_entries.insert(m_current + 1, WTFMove(newItem));
    ++m_current;

    notifyBackForwardListChanged(m_hostObject);
}

void BackForwardList::goBack()
{
    ASSERT(m_current > 0);
    if (m_current > 0) {
        m_current--;
    }
}

void BackForwardList::goForward()
{
    ASSERT(m_current < m_entries.size() - 1);
    if (m_current < m_entries.size() - 1) {
        m_current++;
    }
}

void BackForwardList::goToItem(HistoryItem& item)
{
    if (!m_entries.size())
        return;

    unsigned int index = 0;
    for (; index < m_entries.size(); ++index)
        if (m_entries[index].ptr() == &item)
            break;
    if (index < m_entries.size()) {
        m_current = index;
    }

    notifyBackForwardListChanged(m_hostObject);
}

RefPtr<HistoryItem> BackForwardList::backItem()
{
    if (m_current && m_current != NoCurrentItemIndex)
        return m_entries[m_current - 1].copyRef();
    return nullptr;
}

RefPtr<HistoryItem> BackForwardList::currentItem()
{
    if (m_current != NoCurrentItemIndex)
        return m_entries[m_current].copyRef();
    return nullptr;
}

RefPtr<HistoryItem> BackForwardList::forwardItem()
{
    if (m_entries.size() && m_current < m_entries.size() - 1)
        return m_entries[m_current + 1].copyRef();
    return nullptr;
}

void BackForwardList::backListWithLimit(int limit, Vector<Ref<HistoryItem>>& list)
{
    list.clear();
    if (m_current != NoCurrentItemIndex) {
        unsigned first = std::max(static_cast<int>(m_current) - limit, 0);
        for (; first < m_current; ++first)
            list.append(m_entries[first].get());
    }
}

void BackForwardList::forwardListWithLimit(int limit, Vector<Ref<HistoryItem>>& list)
{
    ASSERT(limit > -1);
    list.clear();
    if (!m_entries.size())
        return;

    unsigned lastEntry = m_entries.size() - 1;
    if (m_current < lastEntry) {
        int last = std::min(m_current + limit, lastEntry);
        limit = m_current + 1;
        for (; limit <= last; ++limit)
            list.append(m_entries[limit].get());
    }
}

int BackForwardList::capacity()
{
    return m_capacity;
}

void BackForwardList::setCapacity(int size)
{
    while (size < static_cast<int>(m_entries.size())) {
        Ref<HistoryItem> item = m_entries.takeLast();
        m_entryHash.remove(item.ptr());
        BackForwardCache::singleton().remove(item);
    }

    if (!size)
        m_current = NoCurrentItemIndex;
    else if (m_current > m_entries.size() - 1) {
        m_current = m_entries.size() - 1;
    }
    m_capacity = size;

    notifyBackForwardListChanged(m_hostObject);
}

bool BackForwardList::enabled()
{
    return m_enabled;
}

void BackForwardList::setEnabled(bool enabled)
{
    m_enabled = enabled;
    if (!enabled) {
        int capacity = m_capacity;
        setCapacity(0);
        setCapacity(capacity);
    }
}

unsigned BackForwardList::backListCount() const
{
    return m_current == NoCurrentItemIndex ? 0 : m_current;
}

unsigned BackForwardList::forwardListCount() const
{
    return m_current == NoCurrentItemIndex ? 0 : m_entries.size() - m_current - 1;
}

RefPtr<HistoryItem> BackForwardList::itemAtIndex(int index)
{
    // Do range checks without doing math on index to avoid overflow.
    if (index < -static_cast<int>(m_current))
        return nullptr;

    if (index > static_cast<int>(forwardListCount()))
        return nullptr;

    return m_entries[index + m_current].copyRef();
}

Vector<Ref<HistoryItem>>& BackForwardList::entries()
{
    return m_entries;
}

void BackForwardList::close()
{
    m_entries.clear();
    m_entryHash.clear();
    m_closed = true;
}

bool BackForwardList::closed()
{
    return m_closed;
}

void BackForwardList::removeItem(HistoryItem& item)
{

    for (unsigned i = 0; i < m_entries.size(); ++i) {
        if (m_entries[i].ptr() == std::addressof(item)) {
            m_entries.remove(i);
            m_entryHash.remove(const_cast<HistoryItem*>(&item));
            if (m_current == NoCurrentItemIndex || m_current < i)
                break;
            if (m_current > i)
                m_current--;
            else {
                size_t count = m_entries.size();
                if (m_current >= count)
                    m_current = count ? count - 1 : NoCurrentItemIndex;
            }
            break;
        }
    }

    notifyBackForwardListChanged(m_hostObject);
}

bool BackForwardList::containsItem(const HistoryItem& entry) const
{
    return m_entryHash.contains(const_cast<HistoryItem*>(&entry));
}
