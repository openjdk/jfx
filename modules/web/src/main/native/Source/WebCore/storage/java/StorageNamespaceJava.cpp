/*
 * Copyright (c) 2012-2013, Oracle and/or its affiliates. All rights reserved.
 */

#include "config.h"
#include "StorageNamespaceJava.h"

#include "GroupSettings.h"
#include "Page.h"
#include "PageGroup.h"
#include "SecurityOriginHash.h"
#include "Settings.h"
#include "StorageAreaJava.h"
#include "StorageMap.h"
//#include "StorageSyncManager.h"
//#include "StorageTracker.h"
#include <wtf/MainThread.h>
#include <wtf/StdLibExtras.h>
#include <wtf/text/StringHash.h>

namespace WebCore {

typedef HashMap<String, StorageNamespace*> LocalStorageNamespaceMap;

static LocalStorageNamespaceMap& localStorageNamespaceMap()
{
    DEFINE_STATIC_LOCAL(LocalStorageNamespaceMap, localStorageNamespaceMap, ());
    return localStorageNamespaceMap;
}

PassRefPtr<StorageNamespace> StorageNamespaceJava::localStorageNamespace(PageGroup* pageGroup)
{
    // Need a page in this page group to query the settings for the local storage database path.
    // Having these parameters attached to the page settings is unfortunate since these settings are
    // not per-page (and, in fact, we simply grab the settings from some page at random), but
    // at this point we're stuck with it.
    Page* page = *pageGroup->pages().begin();
    const String& path = page->settings()->localStorageDatabasePath();
    unsigned quota = pageGroup->groupSettings()->localStorageQuotaBytes();
    const String lookupPath = path.isNull() ? emptyString() : path;

    LocalStorageNamespaceMap::AddResult result = localStorageNamespaceMap().add(lookupPath, 0);
    if (!result.isNewEntry)
        return result.iterator->value;

        RefPtr<StorageNamespace> storageNamespace = adoptRef(new StorageNamespaceJava(LocalStorage, lookupPath, quota));

    result.iterator->value = storageNamespace.get();
        return storageNamespace.release();
    }

PassRefPtr<StorageNamespace> StorageNamespaceJava::sessionStorageNamespace(Page* page)
{
    return adoptRef(new StorageNamespaceJava(SessionStorage, String(), page->settings()->sessionStorageQuota()));
}

PassRefPtr<StorageNamespace> StorageNamespaceJava::transientLocalStorageNamespace(PageGroup* pageGroup, SecurityOrigin*)
{
    // FIXME: A smarter implementation would create a special namespace type instead of just piggy-backing off
    // SessionStorageNamespace here.
    return StorageNamespaceJava::sessionStorageNamespace(*pageGroup->pages().begin());
}

StorageNamespaceJava::StorageNamespaceJava(StorageType storageType, const String& path, unsigned quota)
    : m_storageType(storageType)
    , m_path(path.isolatedCopy())
//    , m_syncManager(0)
    , m_quota(quota)
    , m_isShutdown(false)
{
//    if (m_storageType == LocalStorage && !m_path.isEmpty())
//        m_syncManager = StorageSyncManager::create(m_path);
}

StorageNamespaceJava::~StorageNamespaceJava()
{
    ASSERT(isMainThread());

    if (m_storageType == LocalStorage) {
        ASSERT(localStorageNamespaceMap().get(m_path) == this);
        localStorageNamespaceMap().remove(m_path);
    }

    if (!m_isShutdown)
        close();
}

PassRefPtr<StorageNamespace> StorageNamespaceJava::copy(Page*)
{
    ASSERT(isMainThread());
    ASSERT(!m_isShutdown);
    ASSERT(m_storageType == SessionStorage);

    RefPtr<StorageNamespaceJava> newNamespace = adoptRef(new StorageNamespaceJava(m_storageType, m_path, m_quota));

    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator i = m_storageAreaMap.begin(); i != end; ++i)
        newNamespace->m_storageAreaMap.set(i->key, i->value->copy());
    return newNamespace.release();
}

PassRefPtr<StorageArea> StorageNamespaceJava::storageArea(PassRefPtr<SecurityOrigin> prpOrigin)
{
    ASSERT(isMainThread());
    ASSERT(!m_isShutdown);

    RefPtr<SecurityOrigin> origin = prpOrigin;
    RefPtr<StorageAreaJava> storageArea;
    if ((storageArea = m_storageAreaMap.get(origin)))
        return storageArea.release();

    storageArea = StorageAreaJava::create(m_storageType, origin, /*m_syncManager,*/ m_quota);
    m_storageAreaMap.set(origin.release(), storageArea);
    return storageArea.release();
}

void StorageNamespaceJava::close()
{
    ASSERT(isMainThread());

    if (m_isShutdown)
        return;

    // If we're session storage, we shouldn't need to do any work here.
    if (m_storageType == SessionStorage) {
//        ASSERT(!m_syncManager);
        return;
    }

    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator it = m_storageAreaMap.begin(); it != end; ++it)
        it->value->close();

//    if (m_syncManager)
//        m_syncManager->close();

    m_isShutdown = true;
}

void StorageNamespaceJava::clearOriginForDeletion(SecurityOrigin* origin)
{
    ASSERT(isMainThread());

    RefPtr<StorageAreaJava> storageArea = m_storageAreaMap.get(origin);
    if (storageArea)
        storageArea->clearForOriginDeletion();
}

void StorageNamespaceJava::clearAllOriginsForDeletion()
{
    ASSERT(isMainThread());

    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator it = m_storageAreaMap.begin(); it != end; ++it)
        it->value->clearForOriginDeletion();
}
    
void StorageNamespaceJava::sync()
{
    ASSERT(isMainThread());
    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator it = m_storageAreaMap.begin(); it != end; ++it)
        it->value->sync();
}
    
void StorageNamespaceJava::closeIdleLocalStorageDatabases()
{
    ASSERT(isMainThread());
    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator it = m_storageAreaMap.begin(); it != end; ++it)
        it->value->closeDatabaseIfIdle();
}

} // namespace WebCore
