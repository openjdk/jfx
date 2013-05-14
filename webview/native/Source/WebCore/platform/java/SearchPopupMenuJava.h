/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef SearchPopupMenuJava_h
#define SearchPopupMenuJava_h

#include "PopupMenuJava.h"
#include "SearchPopupMenu.h"

namespace WebCore {

class SearchPopupMenuJava : public SearchPopupMenu {
public:
    SearchPopupMenuJava(PopupMenuClient*);

    virtual PopupMenu* popupMenu();
    virtual void saveRecentSearches(const AtomicString& name, const Vector<String>& searchItems);
    virtual void loadRecentSearches(const AtomicString& name, Vector<String>& searchItems);
    virtual bool enabled();

private:
    RefPtr<PopupMenuJava> m_popup;
};

}

#endif // SearchPopupMenuJava_h
