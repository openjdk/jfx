/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
 */

#pragma once

#include "DataTransfer.h"
#include "DragActions.h"
#include "DragClient.h"
#include "DragImage.h"
#include "Frame.h"
#include "IntPoint.h"
#include <wtf/java/JavaEnv.h>

namespace WebCore {

class DataTransfer;
class DragData;
class Frame;
class Image;
class HTMLImageElement;

class DragClientJava final: public DragClient {
public:
    DragClientJava(const JLObject &webPage);
    ~DragClientJava() override;

    void willPerformDragDestinationAction(DragDestinationAction, const DragData&) override;
    void willPerformDragSourceAction(DragSourceAction, const IntPoint&, DataTransfer& clipboard) override;
    DragDestinationAction actionMaskForDrag(const DragData& data) override;
    //We work in window rather than view coordinates here
    DragSourceAction dragSourceActionMaskForPoint(const IntPoint& windowPoint) override;

    void startDrag(DragImage, const IntPoint& dragImageOrigin, const IntPoint& eventPos, const FloatPoint& dragImageAnchor, DataTransfer&, Frame&, DragSourceAction) override;

    void dragControllerDestroyed() override;
private:
    JGObject m_webPage;
};

} // namespace WebCore
