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
#include <WebCore/WKJDOMUtils.h>
#include <webkit_java_api_page.h>

static const unsigned DefaultCapacity = 100;
static const unsigned NoCurrentItemIndex = UINT_MAX;

using namespace WebCore;

extern "C" {

namespace {

/*
 * The process-wide back/forward callbacks, installed once by wkj_bfl_set_callbacks. The
 * list is created during page creation, before the page exists, so it cannot be reached
 * through wkj_page_set_callbacks; and list_changed is called with the id
 * wkj_bfl_set_host was given rather than with the page, so nothing per page is needed.
 */
const WKJBackForwardCallbacks* s_wkjBackForwardCallbacks = nullptr;

Page* getPage(int64_t page)
{
    return WebPage::pageFromPeer(page);
}

BackForwardList* getBfl(int64_t page)
{
    return &static_cast<BackForwardList&>(getPage(page)->backForward().client());
}

HistoryItem* getItem(int64_t item)
{
    return static_cast<HistoryItem*>(wkj_to_ptr(item));
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

/*
 * Still JNI, and deliberately so.
 *
 * createEntry constructs the Java BackForwardList$Entry and parks it in
 * HistoryItem::m_hostObject, which is a JGObject in the upstream header
 * Source/WebCore/history/HistoryItem.h:298. HistoryItem.cpp:78 reads it back from the
 * destructor to fire notifyItemDestroyed, so entry lifetime cannot move to a wkj_ref
 * without changing files outside this slice. That is why bflGet and bflItemGetChildren
 * below, and notifyHistoryItemDestroyed, keep their JNI form while everything else in
 * this file is wkj_*.
 */
static JLObject createEntry(HistoryItem* item, jlong jpage)
{

    JNIEnv* env = WTF::GetJavaEnv();
    static jmethodID entryCtorMID = initCtor(env, getJEntryClass(), "(JJ)V");

    JLObject jEntry(env->NewObject(getJEntryClass(), entryCtorMID, ptr_to_jlong(item), jpage));
    WTF::CheckAndClearException(env);

    item->setHostObject(jEntry);

    return jEntry;
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
void notifyBackForwardListChanged(wkj_ref host)
{
    if (!host) {
        return;
    }

    if (s_wkjBackForwardCallbacks && s_wkjBackForwardCallbacks->list_changed)
        s_wkjBackForwardCallbacks->list_changed(host);
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
WKJ_EXPORT int32_t wkj_bfl_item_url(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                    int32_t* result_length)
{
    WKJCallScope wkjScope;
    HistoryItem* historyItem = getItem(item);
    String urlString = historyItem->urlString();
    return WKJReturnString(result_buf, result_cap, result_length, urlString);
}

// entry.getTitle()
WKJ_EXPORT int32_t wkj_bfl_item_title(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                      int32_t* result_length)
{
    WKJCallScope wkjScope;
    HistoryItem* historyItem = getItem(item);
    String title = historyItem->title();
    return WKJReturnString(result_buf, result_cap, result_length, title);
}

/*
 * entry.getIcon() has no wkj_* counterpart on purpose. ENABLE(ICONDATABASE) is never
 * defined for this port, so the body below is "return nullptr" for every input and the
 * native-necessity triage rules it PURE with exact parity: it becomes a Java "return null"
 * in its own, behaviour-affecting commit rather than being re-plumbed through the C ABI.
 */
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

// entry.isTargetItem()
WKJ_EXPORT int32_t wkj_bfl_item_is_target(int64_t item)
{
    WKJCallScope wkjScope;
    HistoryItem* historyItem = getItem(item);
    return historyItem->isTargetItem() ? 1 : 0;
}

// entry.getTarget()
WKJ_EXPORT int32_t wkj_bfl_item_target(int64_t item, uint16_t* result_buf, int32_t result_cap,
                                       int32_t* result_length)
{
    WKJCallScope wkjScope;
    HistoryItem* historyItem = getItem(item);
    String target = historyItem->target();
    /* An empty target was reported as a null jstring, and stays WKJ_STR_NULL. */
    if (target.isEmpty()) {
        if (result_length)
            *result_length = 0;
        return WKJ_STR_NULL;
    }
    return WKJReturnString(result_buf, result_cap, result_length, target);
}

WKJ_EXPORT void wkj_bfl_clear_for_drt(int64_t page)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = getBfl(page);
    RefPtr<HistoryItem> current = bfl->currentItem();
    int capacity = bfl->capacity();
    bfl->setCapacity(0);
    bfl->setCapacity(capacity);
    bfl->addItem(*current);
    bfl->goToItem(*current);
}

/*
 * entry.getChildren() keeps its JNI form: it builds Java Entry objects through
 * createEntry, whose HistoryItem::m_hostObject dependency is described above.
 */
JNIEXPORT jobjectArray JNICALL Java_com_sun_webkit_BackForwardList_bflItemGetChildren(JNIEnv* env, jclass, jlong jitem, jlong jpage)
{
    HistoryItem* item = getItem(jitem);
    jobjectArray children = env->NewObjectArray(item->children().size(), getJEntryClass(), nullptr);
    int i = 0;
    for (const auto& it : item->children()) {
        env->SetObjectArrayElement(children, i++, (jobject)createEntry(&it.get(), jpage));
    }
    return children;
}

// BackForwardList.size()
WKJ_EXPORT int32_t wkj_bfl_size(int64_t page)
{
    WKJCallScope wkjScope;
    return getSize(getBfl(page));
}

// BackForwardList.getMaximumSize()
WKJ_EXPORT int32_t wkj_bfl_get_capacity(int64_t page)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(page));
    return static_cast<int32_t>(bfl->capacity());
}

// BackForwardList.setMaximumSize()
WKJ_EXPORT void wkj_bfl_set_capacity(int64_t page, int32_t capacity)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(page));
    bfl->setCapacity(capacity);
}

// BackForwardList.getCurrentIndex()
WKJ_EXPORT int32_t wkj_bfl_current_index(int64_t page)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = getBfl(page);
    return bfl->currentItem() ? static_cast<int32_t>(bfl->backListCount()) : -1;
}

// BackForwardList.setEnabled()
WKJ_EXPORT void wkj_bfl_set_enabled(int64_t page, int32_t enabled)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(page));
    bfl->setEnabled(enabled);
}

// BackForwardList.isEnabled()
WKJ_EXPORT int32_t wkj_bfl_is_enabled(int64_t page)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = static_cast<BackForwardList *>(getBfl(page));
    return bfl->enabled() ? 1 : 0;
}

/*
 * BackForwardList.get() keeps its JNI form for the same reason as bflItemGetChildren: it
 * hands back the Java Entry cached in HistoryItem::m_hostObject.
 */
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
WKJ_EXPORT int32_t wkj_bfl_set_current_index(int64_t page, int32_t index)
{
    WKJCallScope wkjScope;
    Page* p = getPage(page);
    BackForwardList* bfl = &static_cast<BackForwardList&>(p->backForward().client());
    if (index < 0 || index >= getSize(bfl))
        return -1;
    int distance = index - bfl->backListCount();
    p->backForward().goBackOrForward(distance);
    return index;
}

// BackForwardList.get[Last]IndexOf()
WKJ_EXPORT int32_t wkj_bfl_index_of(int64_t page, int64_t item, int32_t reverse)
{
    WKJCallScope wkjScope;
    if (!item)
        return -1;
    BackForwardList* bfl = getBfl(page);
    int size = getSize(bfl);
    int start = reverse ? size - 1 : 0;
    int end = reverse ? -1 : size;
    int inc = reverse ? -1 : 1;
    HistoryItem* historyItem = static_cast<HistoryItem*>(wkj_to_ptr(item));
    for (int i = start; i != end; i += inc)
        if (historyItem == itemAtIndex(bfl, i))
            return i;
    return -1;
}

WKJ_EXPORT void wkj_bfl_set_host(int64_t page, wkj_ref back_forward_list)
{
    WKJCallScope wkjScope;
    BackForwardList* bfl = getBfl(page);
    /*
     * The JNI version took a global reference to the Java BackForwardList; the handle
     * retains the id in the same place and releases it when it is replaced or the list
     * goes away.
     */
    bfl->setHostObject(WKJHandle::retained(back_forward_list));
}

WKJ_EXPORT void wkj_bfl_set_callbacks(const WKJBackForwardCallbacks* callbacks)
{
    WKJCallScope wkjScope;
    s_wkjBackForwardCallbacks = callbacks;
}

}

BackForwardList::BackForwardList()
    : m_current(NoCurrentItemIndex)
    , m_provisional(NoCurrentItemIndex)
    , m_capacity(DefaultCapacity)
    , m_closed(true)
    , m_enabled(true)
{
}

BackForwardList::~BackForwardList()
{
    ASSERT(m_closed);
}

void  BackForwardList::setChildItem(WebCore::BackForwardFrameItemIdentifier parentFrameID, Ref<WebCore::HistoryItem>&&)
{
       /*Get the current top-level HistoryItem.
       Access its main frame item.
       Traverse its child frame items to find the one matching parentFrameID.
       If found, update that frame item with a new child item state */
}

void  BackForwardList::goToProvisionalItem(const WebCore::HistoryItem& item)
{
    m_provisional = m_current;
    goToItem(const_cast<HistoryItem&>(item));

}

void  BackForwardList::clearProvisionalItem(const WebCore::HistoryItem&)
{
   if (m_provisional != NoCurrentItemIndex)
        m_current = std::exchange(m_provisional, NoCurrentItemIndex);
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
        Ref<HistoryItem> item = WTF::move(m_entries[0]);
        m_entries.removeAt(0);
        m_entryHash.remove(item.ptr());
        BackForwardCache::singleton().remove(item);
        --m_current;
    }

    m_entryHash.add(newItem.ptr());
    m_entries.insert(m_current + 1, WTF::move(newItem));
    ++m_current;

    notifyBackForwardListChanged(m_hostObject.get());
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

    notifyBackForwardListChanged(m_hostObject.get());
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

    notifyBackForwardListChanged(m_hostObject.get());
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

RefPtr<HistoryItem> BackForwardList::itemAtIndex(int index, WebCore::FrameIdentifier frameIdentifier)
{
    // Do range checks without doing math on index to avoid overflow.
    if (index < -static_cast<int>(m_current))
        return nullptr;

    if (index > static_cast<int>(forwardListCount()))
        return nullptr;

    return m_entries[index + m_current].copyRef();
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
            m_entries.removeAt(i);
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

    notifyBackForwardListChanged(m_hostObject.get());
}

bool BackForwardList::containsItem(const HistoryItem& entry) const
{
    return m_entryHash.contains(const_cast<HistoryItem*>(&entry));
}
