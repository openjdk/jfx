/*
 * Copyright (C) 2006-2018 Apple Inc. All rights reserved.
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

#pragma once

#include "DragImage.h"
#include "PasteboardItemInfo.h"
#include <wtf/HashMap.h>
#include <wtf/ListHashSet.h>
#include <wtf/Noncopyable.h>
#include <wtf/URL.h>
#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

#if PLATFORM(IOS_FAMILY)
OBJC_CLASS NSString;
#endif

#if PLATFORM(COCOA)
OBJC_CLASS NSArray;
#endif

#if PLATFORM(WIN)
#include "COMPtr.h"
#include "WCDataObject.h"
#include <objidl.h>
#include <windows.h>
typedef struct HWND__* HWND;
#endif

#if PLATFORM(JAVA)
#include "DataObjectJava.h"
#endif

// FIXME: This class uses the DOM and makes calls to Editor.
// It should be divested of its knowledge of the frame and editor.

namespace WebCore {

class DocumentFragment;
class DragData;
class Element;
class Frame;
class PasteboardStrategy;
class Range;
class SelectionData;
class SharedBuffer;

enum class WebContentReadingPolicy { AnyType, OnlyRichTextTypes };
enum ShouldSerializeSelectedTextForDataTransfer { DefaultSelectedTextType, IncludeImageAltTextForDataTransfer };

// For writing to the pasteboard. Generally sorted with the richest formats on top.

struct PasteboardWebContent {
#if PLATFORM(COCOA)
    WEBCORE_EXPORT PasteboardWebContent();
    WEBCORE_EXPORT ~PasteboardWebContent();
    String contentOrigin;
    bool canSmartCopyOrDelete;
    RefPtr<SharedBuffer> dataInWebArchiveFormat;
    RefPtr<SharedBuffer> dataInRTFDFormat;
    RefPtr<SharedBuffer> dataInRTFFormat;
    RefPtr<SharedBuffer> dataInAttributedStringFormat;
    String dataInHTMLFormat;
    String dataInStringFormat;
    Vector<String> clientTypes;
    Vector<RefPtr<SharedBuffer>> clientData;
#endif
#if PLATFORM(GTK)
    bool canSmartCopyOrDelete;
    String text;
    String markup;
#endif
#if USE(LIBWPE)
    String text;
    String markup;
#endif
};

struct PasteboardURL {
    URL url;
    String title;
#if PLATFORM(MAC)
    String userVisibleForm;
#endif
#if PLATFORM(GTK)
    String markup;
#endif
};

struct PasteboardImage {
    WEBCORE_EXPORT PasteboardImage();
    WEBCORE_EXPORT ~PasteboardImage();
    RefPtr<Image> image;
#if PLATFORM(MAC)
    RefPtr<SharedBuffer> dataInWebArchiveFormat;
#endif
#if !PLATFORM(WIN)
    PasteboardURL url;
#endif
#if !(PLATFORM(GTK) || PLATFORM(WIN) || PLATFORM(JAVA))
    RefPtr<SharedBuffer> resourceData;
    String resourceMIMEType;
    Vector<String> clientTypes;
    Vector<RefPtr<SharedBuffer>> clientData;
#endif
    String suggestedName;
    FloatSize imageSize;
};

// For reading from the pasteboard.

class PasteboardWebContentReader {
public:
    String contentOrigin;

    virtual ~PasteboardWebContentReader() = default;

#if PLATFORM(COCOA)
    virtual bool readWebArchive(SharedBuffer&) = 0;
    virtual bool readFilePath(const String&, PresentationSize preferredPresentationSize = { }, const String& contentType = { }) = 0;
    virtual bool readFilePaths(const Vector<String>&) = 0;
    virtual bool readHTML(const String&) = 0;
    virtual bool readRTFD(SharedBuffer&) = 0;
    virtual bool readRTF(SharedBuffer&) = 0;
    virtual bool readImage(Ref<SharedBuffer>&&, const String& type, PresentationSize preferredPresentationSize = { }) = 0;
    virtual bool readURL(const URL&, const String& title) = 0;
    virtual bool readDataBuffer(SharedBuffer&, const String& type, const String& name, PresentationSize preferredPresentationSize = { }) = 0;
#endif
    virtual bool readPlainText(const String&) = 0;
};

struct PasteboardPlainText {
    String text;
#if PLATFORM(COCOA)
    bool isURL;
#endif
};

struct PasteboardFileReader {
    virtual ~PasteboardFileReader() = default;
    virtual void readFilename(const String&) = 0;
    virtual void readBuffer(const String& filename, const String& type, Ref<SharedBuffer>&&) = 0;
};

// FIXME: We need to ensure that the contents of sameOriginCustomData are not accessible across different origins.
struct PasteboardCustomData {
    String origin;
    Vector<String> orderedTypes;
    HashMap<String, String> platformData;
    HashMap<String, String> sameOriginCustomData;

    WEBCORE_EXPORT Ref<SharedBuffer> createSharedBuffer() const;
    WEBCORE_EXPORT static PasteboardCustomData fromSharedBuffer(const SharedBuffer&);

#if PLATFORM(COCOA)
    WEBCORE_EXPORT static const char* cocoaType();
#endif
};

class Pasteboard {
    WTF_MAKE_NONCOPYABLE(Pasteboard); WTF_MAKE_FAST_ALLOCATED;
public:
    Pasteboard();
    virtual ~Pasteboard();

#if PLATFORM(GTK)
    explicit Pasteboard(const String& name);
    explicit Pasteboard(SelectionData&);
#endif

#if PLATFORM(WIN)
    explicit Pasteboard(IDataObject*);
    explicit Pasteboard(WCDataObject*);
    explicit Pasteboard(const DragDataMap&);
#endif

    WEBCORE_EXPORT static std::unique_ptr<Pasteboard> createForCopyAndPaste();

    static bool isSafeTypeForDOMToReadAndWrite(const String&);
    static bool canExposeURLToDOMWhenPasteboardContainsFiles(const String&);

    virtual bool isStatic() const { return false; }

    virtual WEBCORE_EXPORT bool hasData();
    virtual WEBCORE_EXPORT Vector<String> typesSafeForBindings(const String& origin);
    virtual WEBCORE_EXPORT Vector<String> typesForLegacyUnsafeBindings();
    virtual WEBCORE_EXPORT String readOrigin();
    virtual WEBCORE_EXPORT String readString(const String& type);
    virtual WEBCORE_EXPORT String readStringInCustomData(const String& type);
    virtual WEBCORE_EXPORT Vector<String> readAllStrings(const String& type);

    virtual WEBCORE_EXPORT void writeString(const String& type, const String& data);
    virtual WEBCORE_EXPORT void clear();
    virtual WEBCORE_EXPORT void clear(const String& type);

    virtual WEBCORE_EXPORT void read(PasteboardPlainText&);
    virtual WEBCORE_EXPORT void read(PasteboardWebContentReader&, WebContentReadingPolicy = WebContentReadingPolicy::AnyType);
    virtual WEBCORE_EXPORT void read(PasteboardFileReader&);

    virtual WEBCORE_EXPORT void write(const Color&);
    virtual WEBCORE_EXPORT void write(const PasteboardURL&);
    virtual WEBCORE_EXPORT void writeTrustworthyWebURLsPboardType(const PasteboardURL&);
    virtual WEBCORE_EXPORT void write(const PasteboardImage&);
    virtual WEBCORE_EXPORT void write(const PasteboardWebContent&);

    virtual WEBCORE_EXPORT void writeCustomData(const PasteboardCustomData&);

    enum class FileContentState { NoFileOrImageData, InMemoryImage, MayContainFilePaths };
    virtual WEBCORE_EXPORT FileContentState fileContentState();
    virtual WEBCORE_EXPORT bool canSmartReplace();

    virtual WEBCORE_EXPORT void writeMarkup(const String& markup);
    enum SmartReplaceOption { CanSmartReplace, CannotSmartReplace };
    virtual WEBCORE_EXPORT void writePlainText(const String&, SmartReplaceOption); // FIXME: Two separate functions would be clearer than one function with an argument.

#if PLATFORM(JAVA)
    RefPtr<DataObjectJava> dataObject() const { return m_dataObject; }
#endif

#if ENABLE(DRAG_SUPPORT)
    WEBCORE_EXPORT static std::unique_ptr<Pasteboard> createForDragAndDrop();
    WEBCORE_EXPORT static std::unique_ptr<Pasteboard> createForDragAndDrop(const DragData&);

    virtual void setDragImage(DragImage, const IntPoint& hotSpot);
#endif

#if PLATFORM(WIN) || PLATFORM(JAVA)
    RefPtr<DocumentFragment> documentFragment(Frame&, Range&, bool allowPlainText, bool& chosePlainText); // FIXME: Layering violation.
    void writeImage(Element&, const URL&, const String& title); // FIXME: Layering violation.
    void writeSelection(Range&, bool canSmartCopyOrDelete, Frame&, ShouldSerializeSelectedTextForDataTransfer = DefaultSelectedTextType); // FIXME: Layering violation.
#endif

#if PLATFORM(GTK)
    const SelectionData& selectionData() const;
    static std::unique_ptr<Pasteboard> createForGlobalSelection();
#endif

#if PLATFORM(IOS_FAMILY)
    explicit Pasteboard(long changeCount);
    explicit Pasteboard(const String& pasteboardName);

    static NSArray *supportedWebContentPasteboardTypes();
    static String resourceMIMEType(NSString *mimeType);
#endif

#if PLATFORM(MAC)
    explicit Pasteboard(const String& pasteboardName, const Vector<String>& promisedFilePaths = { });
#endif

#if PLATFORM(COCOA)
    static bool shouldTreatCocoaTypeAsFile(const String&);
    WEBCORE_EXPORT static NSArray *supportedFileUploadPasteboardTypes();
    const String& name() const { return m_pasteboardName; }
    long changeCount() const;
    const PasteboardCustomData& readCustomData();
#endif

#if PLATFORM(WIN)
    COMPtr<IDataObject> dataObject() const { return m_dataObject; }
    void setExternalDataObject(IDataObject*);
    const DragDataMap& dragDataMap() const { return m_dragDataMap; }
    void writeURLToWritableDataObject(const URL&, const String&);
    COMPtr<WCDataObject> writableDataObject() const { return m_writableDataObject; }
    void writeImageToDataObject(Element&, const URL&); // FIXME: Layering violation.
#endif

private:
#if PLATFORM(IOS_FAMILY)
    bool respectsUTIFidelities() const;
    void readRespectingUTIFidelities(PasteboardWebContentReader&, WebContentReadingPolicy);

    enum class ReaderResult {
        ReadType,
        DidNotReadType,
        PasteboardWasChangedExternally
    };
    ReaderResult readPasteboardWebContentDataForType(PasteboardWebContentReader&, PasteboardStrategy&, NSString *type, const PasteboardItemInfo&, int itemIndex);
#endif

#if PLATFORM(WIN)
    void finishCreatingPasteboard();
    void writeRangeToDataObject(Range&, Frame&); // FIXME: Layering violation.
    void writeURLToDataObject(const URL&, const String&);
    void writePlainTextToDataObject(const String&, SmartReplaceOption);
#endif

#if PLATFORM(JAVA)
    Pasteboard(RefPtr<DataObjectJava>, bool copyPasteMode);
    static std::unique_ptr<Pasteboard> create(RefPtr<DataObjectJava>);
#endif

#if PLATFORM(COCOA)
    Vector<String> readFilePaths();
    Vector<String> readPlatformValuesAsStrings(const String& domType, long changeCount, const String& pasteboardName);
    static void addHTMLClipboardTypesForCocoaType(ListHashSet<String>& resultTypes, const String& cocoaType);
    String readStringForPlatformType(const String&);
    Vector<String> readTypesWithSecurityCheck();
    RefPtr<SharedBuffer> readBufferForTypeWithSecurityCheck(const String&);
#endif

#if PLATFORM(GTK)
    void writeToClipboard();
    void readFromClipboard();
    Ref<SelectionData> m_selectionData;
    String m_name;
#endif

#if PLATFORM(COCOA)
    String m_pasteboardName;
    long m_changeCount;
    Optional<PasteboardCustomData> m_customDataCache;
#endif

#if PLATFORM(MAC)
    Vector<String> m_promisedFilePaths;
#endif

#if PLATFORM(WIN)
    HWND m_owner;
    COMPtr<IDataObject> m_dataObject;
    COMPtr<WCDataObject> m_writableDataObject;
    DragDataMap m_dragDataMap;
#endif

#if PLATFORM(JAVA)
    RefPtr<DataObjectJava> m_dataObject;
    bool m_copyPasteMode;
#endif
};

#if PLATFORM(IOS_FAMILY)
extern NSString *WebArchivePboardType;
extern NSString *UIColorPboardType;
#endif

#if PLATFORM(MAC)
extern const char* const WebArchivePboardType;
extern const char* const WebURLNamePboardType;
extern const char* const WebURLsWithTitlesPboardType;
#endif

#if !PLATFORM(GTK)

inline Pasteboard::~Pasteboard()
{
}

#endif

} // namespace WebCore
