#ifndef StorageAreaJava_h
#define StorageAreaJava_h

#include "StorageArea.h"

#include <wtf/PassRefPtr.h>
#include <wtf/RefPtr.h>

namespace WebCore {

    class SecurityOrigin;
    class StorageMap;
    //class StorageAreaSync;

    class StorageAreaJava : public StorageArea {
    public:
        static PassRefPtr<StorageAreaJava> create(StorageType, PassRefPtr<SecurityOrigin>, /*PassRefPtr<StorageSyncManager>,*/ unsigned quota);
        virtual ~StorageAreaJava();

        // The HTML5 DOM Storage API (and contains)
        virtual unsigned length() OVERRIDE;
        virtual String key(unsigned index) OVERRIDE;
        virtual String item(const String& key) OVERRIDE;
        virtual void setItem(Frame* sourceFrame, const String& key, const String& value, bool& quotaException) OVERRIDE;
        virtual void removeItem(Frame* sourceFrame, const String& key) OVERRIDE;
        virtual void clear(Frame* sourceFrame) OVERRIDE;
        virtual bool contains(const String& key) OVERRIDE;

        virtual bool canAccessStorage(Frame* sourceFrame) OVERRIDE;
        virtual StorageType storageType() const OVERRIDE;
        virtual size_t memoryBytesUsedByCache() OVERRIDE;

        PassRefPtr<StorageAreaJava> copy();
        void close();

        // Used to clear a StorageArea and close db before backing db file is deleted.
        void clearForOriginDeletion();

        void sync();

    private:
        StorageAreaJava(StorageType, PassRefPtr<SecurityOrigin>, /*PassRefPtr<StorageSyncManager>,*/ unsigned quota);
        StorageAreaJava(StorageAreaJava*);

        void blockUntilImportComplete() const;

        StorageType m_storageType;
        RefPtr<SecurityOrigin> m_securityOrigin;
        RefPtr<StorageMap> m_storageMap;

//        RefPtr<StorageAreaSync> m_storageAreaSync;
//        RefPtr<StorageSyncManager> m_storageSyncManager;

#ifndef NDEBUG
        bool m_isShutdown;
#endif
    };

} // namespace WebCore

#endif // StorageAreaJava_h
