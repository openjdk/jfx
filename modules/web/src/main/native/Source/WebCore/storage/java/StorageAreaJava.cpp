/*
 * Copyright (c) 2012-2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"
#include "StorageAreaJava.h"

#include "Document.h"
#include "ExceptionCode.h"
#include "Frame.h"
#include "Page.h"
#include "SchemeRegistry.h"
#include "SecurityOrigin.h"
#include "Settings.h"
//#include "StorageAreaSync.h"
//#include "StorageEventDispatcher.h"
#include "StorageMap.h"
//#include "StorageSyncManager.h"
//#include "StorageTracker.h"
#include <wtf/MainThread.h>

namespace WebCore {

StorageAreaJava::~StorageAreaJava()
{
    ASSERT(isMainThread());
}

inline StorageAreaJava::StorageAreaJava(StorageType storageType, PassRefPtr<SecurityOrigin> origin, /*PassRefPtr<StorageSyncManager> syncManager,*/ unsigned quota)
    : m_storageType(storageType)
    , m_securityOrigin(origin)
    , m_storageMap(StorageMap::create(quota))
//    , m_storageSyncManager(syncManager)
#ifndef NDEBUG
    , m_isShutdown(false)
#endif
    , m_accessCount(0)
    , m_closeDatabaseTimer(this, &StorageAreaJava::closeDatabaseTimerFired)
{
    ASSERT(isMainThread());
    ASSERT(m_securityOrigin);
    ASSERT(m_storageMap);
    
    // Accessing the shared global StorageTracker when a StorageArea is created 
    // ensures that the tracker is properly initialized before anyone actually needs to use it.
    //StorageTracker::tracker();
}

PassRefPtr<StorageAreaJava> StorageAreaJava::create(StorageType storageType, PassRefPtr<SecurityOrigin> origin, /*PassRefPtr<StorageSyncManager> syncManager,*/ unsigned quota)
{
    RefPtr<StorageAreaJava> area = adoptRef(new StorageAreaJava(storageType, origin, /*syncManager,*/ quota));

    // FIXME: If there's no backing storage for LocalStorage, the default WebKit behavior should be that of private browsing,
    // not silently ignoring it. https://bugs.webkit.org/show_bug.cgi?id=25894
/*
    if (area->m_storageSyncManager) {
        area->m_storageAreaSync = StorageAreaSync::create(area->m_storageSyncManager, area.get(), area->m_securityOrigin->databaseIdentifier());
        ASSERT(area->m_storageAreaSync);
    }
*/
    return area.release();
}

PassRefPtr<StorageAreaJava> StorageAreaJava::copy()
{
    ASSERT(!m_isShutdown);
    return adoptRef(new StorageAreaJava(this));
}

StorageAreaJava::StorageAreaJava(StorageAreaJava* area)
    : m_storageType(area->m_storageType)
    , m_securityOrigin(area->m_securityOrigin)
    , m_storageMap(area->m_storageMap)
//    , m_storageSyncManager(area->m_storageSyncManager)
#ifndef NDEBUG
    , m_isShutdown(area->m_isShutdown)
#endif
    , m_accessCount(0)
    , m_closeDatabaseTimer(this, &StorageAreaJava::closeDatabaseTimerFired)
{
    ASSERT(isMainThread());
    ASSERT(m_securityOrigin);
    ASSERT(m_storageMap);
    ASSERT(!m_isShutdown);
}

bool StorageAreaJava::canAccessStorage(Frame* frame)
{
    return frame && frame->page();
}

StorageType StorageAreaJava::storageType() const
{
    return m_storageType;
}

unsigned StorageAreaJava::length()
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    return m_storageMap->length();
}

String StorageAreaJava::key(unsigned index)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    return m_storageMap->key(index);
}

String StorageAreaJava::item(const String& key)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    return m_storageMap->getItem(key);
}

void StorageAreaJava::setItem(Frame* sourceFrame, const String& key, const String& value, bool& quotaException)
{
    ASSERT(!m_isShutdown);
    ASSERT(!value.isNull());
    blockUntilImportComplete();

    String oldValue;
    RefPtr<StorageMap> newMap = m_storageMap->setItem(key, value, oldValue, quotaException);
    if (newMap)
        m_storageMap = newMap.release();

    if (quotaException)
        return;

    if (oldValue == value)
        return;
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleItemForSync(key, value);
    dispatchStorageEvent(key, oldValue, value, sourceFrame);
*/
}

void StorageAreaJava::removeItem(Frame* sourceFrame, const String& key)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    String oldValue;
    RefPtr<StorageMap> newMap = m_storageMap->removeItem(key, oldValue);
    if (newMap)
        m_storageMap = newMap.release();

    if (oldValue.isNull())
        return;
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleItemForSync(key, String());
    dispatchStorageEvent(key, oldValue, String(), sourceFrame);
*/
}

void StorageAreaJava::clear(Frame* sourceFrame)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    if (!m_storageMap->length())
        return;

    unsigned quota = m_storageMap->quota();
    m_storageMap = StorageMap::create(quota);
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleClear();
    dispatchStorageEvent(String(), String(), String(), sourceFrame);
*/
}

bool StorageAreaJava::contains(const String& key)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    return m_storageMap->contains(key);
}

void StorageAreaJava::importItems(const HashMap<String, String>& items)
{
    ASSERT(!m_isShutdown);

    m_storageMap->importItems(items);
}

void StorageAreaJava::close()
{
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleFinalSync();
*/
#ifndef NDEBUG
    m_isShutdown = true;
#endif
}

void StorageAreaJava::clearForOriginDeletion()
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();
    
    if (m_storageMap->length()) {
        unsigned quota = m_storageMap->quota();
        m_storageMap = StorageMap::create(quota);
    }
/*
    if (m_storageAreaSync) {
        m_storageAreaSync->scheduleClear();
        m_storageAreaSync->scheduleCloseDatabase();
    }
*/
}
    
void StorageAreaJava::sync()
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleSync();
*/
}

void StorageAreaJava::blockUntilImportComplete() const
{
/*
    if (m_storageAreaSync)
        m_storageAreaSync->blockUntilImportComplete();
*/
}

size_t StorageAreaJava::memoryBytesUsedByCache()
{
    return 0;
}

void StorageAreaJava::incrementAccessCount()
{
    m_accessCount++;

    if (m_closeDatabaseTimer.isActive())
        m_closeDatabaseTimer.stop();
}

void StorageAreaJava::decrementAccessCount()
{
    ASSERT(m_accessCount);
    --m_accessCount;

    if (!m_accessCount) {
        if (m_closeDatabaseTimer.isActive())
            m_closeDatabaseTimer.stop();
        m_closeDatabaseTimer.startOneShot(/*StorageTracker::tracker().storageDatabaseIdleInterval()*/ 0);
    }
}

void StorageAreaJava::closeDatabaseTimerFired(Timer<StorageAreaJava> *)
{
    blockUntilImportComplete();
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleCloseDatabase();
*/
}

void StorageAreaJava::closeDatabaseIfIdle()
{
    if (m_closeDatabaseTimer.isActive()) {
        ASSERT(!m_accessCount);
        m_closeDatabaseTimer.stop();

        closeDatabaseTimerFired(&m_closeDatabaseTimer);
}
}

void StorageAreaJava::dispatchStorageEvent(const String& key, const String& oldValue, const String& newValue, Frame* sourceFrame)
{
/*
    if (m_storageType == LocalStorage)
        StorageEventDispatcher::dispatchLocalStorageEvents(key, oldValue, newValue, m_securityOrigin.get(), sourceFrame);
    else
        StorageEventDispatcher::dispatchSessionStorageEvents(key, oldValue, newValue, m_securityOrigin.get(), sourceFrame);
*/
}

} // namespace WebCore
