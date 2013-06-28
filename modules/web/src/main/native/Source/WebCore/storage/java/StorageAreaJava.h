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
        virtual unsigned length(Frame* sourceFrame) const;
        virtual String key(unsigned index, Frame* sourceFrame) const;
        virtual String getItem(const String& key, Frame* sourceFrame) const;
        virtual void setItem(const String& key, const String& value, ExceptionCode& ec, Frame* sourceFrame);
        virtual void removeItem(const String& key, Frame* sourceFrame);
        virtual void clear(Frame* sourceFrame);
        virtual bool contains(const String& key, Frame* sourceFrame) const;

        virtual bool disabledByPrivateBrowsingInFrame(const Frame* sourceFrame) const;

        PassRefPtr<StorageAreaJava> copy();
        void close();

        // Only called from a background thread.
        void importItem(const String& key, const String& value);

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
