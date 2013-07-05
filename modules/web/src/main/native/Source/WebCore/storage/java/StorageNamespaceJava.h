/*
 * Copyright (c) 2012-2013, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef StorageNamespaceJava_h
#define StorageNamespaceJava_h

#include "SecurityOriginHash.h"
#include "StorageArea.h"
#include "StorageNamespace.h"

#include <wtf/HashMap.h>
#include <wtf/RefPtr.h>
#include <wtf/text/WTFString.h>

namespace WebCore {
    class StorageAreaJava;

    class StorageNamespaceJava : public StorageNamespace {
    public:
        static PassRefPtr<StorageNamespace> localStorageNamespace(PageGroup*);
        static PassRefPtr<StorageNamespace> transientLocalStorageNamespace(PageGroup*, SecurityOrigin*);
        static PassRefPtr<StorageNamespace> sessionStorageNamespace(Page*);
        virtual ~StorageNamespaceJava();

        virtual PassRefPtr<StorageArea> storageArea(PassRefPtr<SecurityOrigin>) OVERRIDE;
        virtual PassRefPtr<StorageNamespace> copy(Page* newPage) OVERRIDE;
        virtual void close() OVERRIDE;

        // Not removing the origin's StorageArea from m_storageAreaMap because
        // we're just deleting the underlying db file. If an item is added immediately
        // after file deletion, we want the same StorageArea to eventually trigger
        // a sync and for StorageAreaSync to recreate the backing db file.
        virtual void clearOriginForDeletion(SecurityOrigin*) OVERRIDE;
        virtual void clearAllOriginsForDeletion() OVERRIDE;
        virtual void sync() OVERRIDE;
        virtual void closeIdleLocalStorageDatabases() OVERRIDE;
        
    private:
        StorageNamespaceJava(StorageType, const String& path, unsigned quota);

        typedef HashMap<RefPtr<SecurityOrigin>, RefPtr<StorageAreaJava> > StorageAreaMap;
        StorageAreaMap m_storageAreaMap;

        StorageType m_storageType;

        // Only used if m_storageType == LocalStorage and the path was not "" in our constructor.
        String m_path;
        //RefPtr<StorageSyncManager> m_syncManager;

        // The default quota for each new storage area.
        unsigned m_quota;

        bool m_isShutdown;
    };

} // namespace WebCore

#endif // StorageNamespaceJava_h
