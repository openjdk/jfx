/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef SearchPopupMenuJava_h
#define SearchPopupMenuJava_h

#include "PopupMenuJava.h"
#include "SearchPopupMenu.h"

namespace WebCore {

class SearchPopupMenuJava : public SearchPopupMenu {
public:
    SearchPopupMenuJava(PopupMenuClient*);

    PopupMenu* popupMenu() override;
    void saveRecentSearches(const AtomicString& name, const Vector<RecentSearch>&) override;
    void loadRecentSearches(const AtomicString& name, Vector<RecentSearch>&) override;
    bool enabled() override;

private:
    RefPtr<PopupMenuJava> m_popup;
};

}

#endif // SearchPopupMenuJava_h
