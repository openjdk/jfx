/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include <wtf/java/JavaRef.h>
#include <wtf/Vector.h>

namespace WebCore {

class ContextMenuItem;
class ContextMenuController;
class IntPoint;

class ContextMenuJava {
  private:
    JGObject m_contextMenu;
  public:
    ContextMenuJava(const Vector<ContextMenuItem>&);
    void show(ContextMenuController*, jobject page, const IntPoint& loc) const;
};
}  // namespace WebCore

