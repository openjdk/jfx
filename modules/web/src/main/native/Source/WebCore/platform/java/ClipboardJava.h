/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ClipboardJava_h
#define ClipboardJava_h

#include "Clipboard.h"
#include "DataObjectJava.h"
#include "CachedResourceClient.h"

namespace WTF {
    class String;
}
using WTF::String;

namespace WebCore {

    class CachedImage;
    class Element;
    class Frame;
    class Image;
    class URL;
    class Range;

    class ClipboardJava : public Clipboard, public CachedResourceClient {
        WTF_MAKE_FAST_ALLOCATED;
        friend class DragClientJava;
    public:
        static PassRefPtr<Clipboard> create(
            ClipboardAccessPolicy policy,
            ClipboardType type,
            PassRefPtr<DataObjectJava> data,
            Frame* frame)
        {
            return adoptRef(new ClipboardJava(policy, type, data, frame));
        }

        virtual ~ClipboardJava();


        virtual void clearData(const String& type);
        virtual void clearData();
        virtual String getData(const String& type) const;
        virtual bool setData(const String& type, const String& data);

        // extensions beyond IE's API
        virtual ListHashSet<String> types() const;
        virtual PassRefPtr<FileList> files() const;

        virtual void setDragImage(CachedImage*, const IntPoint&);
        virtual void setDragImageElement(Node*, const IntPoint&);

        //Provides the DOM specified
        virtual void declareAndWriteDragImage(Element*, const URL&, const String& title, Frame*);
        virtual void writeURL(const URL&, const String&, Frame*);
        virtual void writeRange(Range*, Frame*);
        virtual void writePlainText(const String&);

        virtual bool hasData();
    private:
        virtual ListHashSet<String> typesPrivate() const;
    private:
        ClipboardJava(
            ClipboardAccessPolicy policy,
            ClipboardType type,
            PassRefPtr<DataObjectJava> dataObject,
            Frame* frame);

        void setDragImage(CachedImage* image, Node* node, const IntPoint& loc);
        RefPtr<DataObjectJava> m_dataObject;
        Frame* m_frame;
    };

} // namespace WebCore

#endif // !ClipboardJava_h
