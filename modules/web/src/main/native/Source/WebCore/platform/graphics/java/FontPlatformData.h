/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef FontPlatformData_h
#define FontPlatformData_h

#include <algorithm>
#include "FontDescription.h"
#include "FontOrientation.h"
#include "JavaEnv.h"
#include "RQRef.h"

namespace WebCore {

class FontPlatformData {
public:
    static PassRefPtr<RQRef> getJavaFont(const FontDescription& fontDescription, const AtomicString& family);

    FontPlatformData() 
        {};

    FontPlatformData(float size, bool bold, bool italic)
        : m_jFont((size <= 1e-2) ? 0 : getJavaFont("Dialog", size, italic, bold))
        , m_size(size)
        {};

    FontPlatformData(const FontDescription& fontDescription, const AtomicString& family)
        : m_jFont(getJavaFont(fontDescription, family))
        , m_size(fontDescription.computedSize())
        {};

    FontPlatformData(RefPtr<RQRef> font, float size)
        : m_jFont(font)
        , m_size(size)
        {};

    FontPlatformData(WTF::HashTableDeletedValueType)
        : m_jFont(WTF::HashTableDeletedValue)
        {};

    FontPlatformData(const FontPlatformData &other)
        : m_jFont(other.m_jFont)
        {};

    void swap(FontPlatformData& other) { std::swap(m_jFont, other.m_jFont); }
    FontOrientation orientation() const { return Horizontal; } // FIXME: Implement.

    static bool init();
    unsigned hash() const;
    float size() const { return m_size; }

    bool isHashTableDeletedValue() const {
        return m_jFont.isHashTableDeletedValue(); 
    }
    bool operator == (const FontPlatformData &) const;
    FontPlatformData& operator = (const FontPlatformData &);

    PassRefPtr<RQRef> nativeFontData() const { return m_jFont; } 
    static jint getJavaFontID(const JLObject &font);

    void setOrientation(FontOrientation orientation) { }

#ifndef NDEBUG
    String description() const;
#endif

private:
    static PassRefPtr<RQRef> getJavaFont(const String& family, float size, bool italic, bool bold);

    RefPtr<RQRef> m_jFont;
    float m_size;  // Point size of the font in pixels.
};

} // namespace WebCore

#endif //FontPlatformData_h
