#include "config.h"
#include "StorageNamespaceJava.h"

#include "SecurityOriginHash.h"
#include "StorageMap.h"
#include "StorageNamespace.h"
#include "StorageNamespaceJava.h"
#include "StorageArea.h"
#include "StorageAreaJava.h"
//#include "StorageSyncManager.h"
//#include "StorageTracker.h"
#include <wtf/MainThread.h>
#include <wtf/StdLibExtras.h>
#include <wtf/text/StringHash.h>

namespace WebCore {

PassRefPtr<StorageNamespace> StorageNamespace::localStorageNamespace(const String& path, unsigned quota)
{
    return StorageNamespaceJava::localStorageNamespace(path, quota);
}

// The page argument is only used by the Chromium port.
PassRefPtr<StorageNamespace> StorageNamespace::sessionStorageNamespace(Page*, unsigned quota)
{
    return StorageNamespaceJava::sessionStorageNamespace(quota);
}


typedef HashMap<String, StorageNamespace*> LocalStorageNamespaceMap;

static LocalStorageNamespaceMap& localStorageNamespaceMap()
{
    DEFINE_STATIC_LOCAL(LocalStorageNamespaceMap, localStorageNamespaceMap, ());
    return localStorageNamespaceMap;
}

PassRefPtr<StorageNamespace> StorageNamespaceJava::localStorageNamespace(const String& path, unsigned quota)
{
    const String lookupPath = path.isNull() ? String("") : path;
    LocalStorageNamespaceMap::iterator it = localStorageNamespaceMap().find(lookupPath);
    if (it == localStorageNamespaceMap().end()) {
        RefPtr<StorageNamespace> storageNamespace = adoptRef(new StorageNamespaceJava(LocalStorage, lookupPath, quota));
        localStorageNamespaceMap().set(lookupPath, storageNamespace.get());
        return storageNamespace.release();
    }

    return it->second;
}

PassRefPtr<StorageNamespace> StorageNamespaceJava::sessionStorageNamespace(unsigned quota)
{
    return adoptRef(new StorageNamespaceJava(SessionStorage, String(), quota));
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

PassRefPtr<StorageNamespace> StorageNamespaceJava::copy()
{
    ASSERT(isMainThread());
    ASSERT(!m_isShutdown);
    ASSERT(m_storageType == SessionStorage);

    RefPtr<StorageNamespaceJava> newNamespace = adoptRef(new StorageNamespaceJava(m_storageType, m_path, m_quota));

    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator i = m_storageAreaMap.begin(); i != end; ++i)
        newNamespace->m_storageAreaMap.set(i->first, i->second->copy());
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
        it->second->close();

//    if (m_syncManager)
//        m_syncManager->close();

    m_isShutdown = true;
}

void StorageNamespaceJava::unlock()
{
    // Because there's a single event loop per-process, this is a no-op.
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
        it->second->clearForOriginDeletion();
}
    
void StorageNamespaceJava::sync()
{
    ASSERT(isMainThread());
    StorageAreaMap::iterator end = m_storageAreaMap.end();
    for (StorageAreaMap::iterator it = m_storageAreaMap.begin(); it != end; ++it)
        it->second->sync();
}

} // namespace WebCore
