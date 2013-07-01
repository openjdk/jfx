#ifndef StorageNamespaceJava_h
#define StorageNamespaceJava_h

#include "PlatformString.h"
#include "SecurityOriginHash.h"
#include "StorageArea.h"
#include "StorageNamespace.h"

#include <wtf/HashMap.h>
#include <wtf/RefPtr.h>

namespace WebCore {
    class StorageAreaJava;

    class StorageNamespaceJava : public StorageNamespace {
    public:
        static PassRefPtr<StorageNamespace> localStorageNamespace(const String& path, unsigned quota);
        static PassRefPtr<StorageNamespace> sessionStorageNamespace(unsigned quota);

        virtual ~StorageNamespaceJava();
        virtual PassRefPtr<StorageArea> storageArea(PassRefPtr<SecurityOrigin>);
        virtual PassRefPtr<StorageNamespace> copy();
        virtual void close();
        virtual void unlock();

        // Not removing the origin's StorageArea from m_storageAreaMap because
        // we're just deleting the underlying db file. If an item is added immediately
        // after file deletion, we want the same StorageArea to eventually trigger
        // a sync and for StorageAreaSync to recreate the backing db file.
        virtual void clearOriginForDeletion(SecurityOrigin*);
        virtual void clearAllOriginsForDeletion();
        virtual void sync();
        
    private:
        StorageNamespaceJava(StorageType, const String& path, unsigned quota);

        typedef HashMap<RefPtr<SecurityOrigin>, RefPtr<StorageAreaJava>, SecurityOriginHash> StorageAreaMap;
        StorageAreaMap m_storageAreaMap;

        StorageType m_storageType;

        // Only used if m_storageType == LocalStorage and the path was not "" in our constructor.
        String m_path;
        //RefPtr<StorageSyncManager> m_syncManager;

        unsigned m_quota;  // The default quota for each new storage area.
        bool m_isShutdown;
    };

} // namespace WebCore

#endif // StorageNamespaceJava_h
