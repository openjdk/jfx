/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#include "GraphicsContext.h"
#include "GraphicsContextJava.h"
#include "HTMLPlugInElement.h"
#include "IntSize.h"
#include "JavaEnv.h"
#include "ResourceError.h"
#include "ResourceResponse.h"
#include "ScrollView.h"
#include "Widget.h"
 
#include <wtf/text/WTFString.h>


namespace WebCore {

    class PluginWidgetJava : public Widget {
        PassRefPtr<HTMLPlugInElement> m_element; 
        String m_url;
        String m_mimeType;
        IntSize m_size;
        Vector<String> m_paramNames;
        Vector<String> m_paramValues;

    public:
        PluginWidgetJava(
            jobject wfh, 
            HTMLPlugInElement *element, 
            const IntSize& size, 
            const String& url, 
            const String& mimeType,
            const Vector<String>& paramNames, 
            const Vector<String>& paramValues);
        virtual ~PluginWidgetJava();

        virtual void invalidateRect(const IntRect&);
        virtual void paint(GraphicsContext*, const IntRect&);
        void invalidateWindowlessPluginRect(const IntRect& rect);
        void convertToPage(IntRect& rect);
        void focusPluginElement(bool isFocused);
        bool isVisible() {
            return isSelfVisible() && ( NULL==parent() || parent()->isSelfVisible() );
        }
        virtual void setFrameRect(const IntRect& rect);
        virtual void frameRectsChanged();
        void updatePluginWidget();
        virtual void setCursor(const Cursor& cursor) {}

        //virtual void setFocus();
        //virtual void show();
        //virtual void hide();
        //virtual void paint(GraphicsContext*, const IntRect&);

        // This method is used by plugins on all platforms to obtain a clip rect that includes clips set by WebCore,
        // e.g., in overflow:auto sections.  The clip rects coordinates are in the containing window's coordinate space.
        // This clip includes any clips that the widget itself sets up for its children.
        //IntRect windowClipRect() const;

        virtual void handleEvent(Event*);
        //virtual void setParent(ScrollView*);//postponed init have to be implemented (just on non-null parent)
        //virtual void setParentVisible(bool);//pause in rendering 

        //virtual bool isPluginView() const { return true; }
    };
} // namespace WebCore
