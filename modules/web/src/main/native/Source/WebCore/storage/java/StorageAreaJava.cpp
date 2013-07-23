/*
 * Copyright (c) 2012-2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "Document.h"

#include "SchemeRegistry.h"
#include "StorageAreaJava.h"

#include "ExceptionCode.h"
#include "Frame.h"
#include "Page.h"
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
{
    ASSERT(isMainThread());
    ASSERT(m_securityOrigin);
    ASSERT(m_storageMap);
    ASSERT(!m_isShutdown);
}

static bool privateBrowsingEnabled(Frame* sourceFrame)
{
    return sourceFrame->page() && sourceFrame->page()->settings()->privateBrowsingEnabled();
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

    if (privateBrowsingEnabled(sourceFrame)) {
        quotaException = true;
        return;
    }

    String oldValue;
    RefPtr<StorageMap> newMap = m_storageMap->setItem(key, value, oldValue, quotaException);
    if (newMap)
        m_storageMap = newMap.release();

/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleItemForSync(key, value);
    StorageEventDispatcher::dispatch(key, oldValue, value, m_storageType, m_securityOrigin.get(), sourceFrame);
*/
}

void StorageAreaJava::removeItem(Frame* sourceFrame, const String& key)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    if (privateBrowsingEnabled(sourceFrame))
        return;

    String oldValue;
    RefPtr<StorageMap> newMap = m_storageMap->removeItem(key, oldValue);
    if (newMap)
        m_storageMap = newMap.release();

/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleItemForSync(key, String());
    StorageEventDispatcher::dispatch(key, oldValue, String(), m_storageType, m_securityOrigin.get(), sourceFrame);
*/
}

void StorageAreaJava::clear(Frame* sourceFrame)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    if (privateBrowsingEnabled(sourceFrame))
        return;

    if (!m_storageMap->length())
        return;

    unsigned quota = m_storageMap->quota();
    m_storageMap = StorageMap::create(quota);
/*
    if (m_storageAreaSync)
        m_storageAreaSync->scheduleClear();
    StorageEventDispatcher::dispatch(String(), String(), String(), m_storageType, m_securityOrigin.get(), sourceFrame);
*/
}

bool StorageAreaJava::contains(const String& key)
{
    ASSERT(!m_isShutdown);
    blockUntilImportComplete();

    return m_storageMap->contains(key);
}

bool StorageAreaJava::canAccessStorage(Frame* sourceFrame)
{
    if (!sourceFrame->page() || !sourceFrame->page()->settings()->privateBrowsingEnabled())
        return false;
    if (m_storageType != LocalStorage)
        return true;
    return !SchemeRegistry::allowsLocalStorageAccessInPrivateBrowsing(sourceFrame->document()->securityOrigin()->protocol());
}


StorageType StorageAreaJava::storageType() const
{
    return m_storageType;
}

size_t StorageAreaJava::memoryBytesUsedByCache()
{
    return 0;
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

}
