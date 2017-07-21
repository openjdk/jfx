/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 */
#pragma once

#if ENABLE(INPUT_TYPE_COLOR)
#include <wtf/java/JavaEnv.h>
#include "ColorChooser.h"

namespace WebCore {
class ColorChooserClient;

class ColorChooserJava final : public ColorChooser {
public:
    ColorChooserJava(JGObject&, ColorChooserClient*, const Color&);
    ColorChooserClient* getClient() { return m_colorChooserClient; }

    ~ColorChooserJava() override { }
    void reattachColorChooser(const Color&) override;
    void setSelectedColor(const Color&) override;
    void endChooser() override;

private:
    ColorChooserClient* m_colorChooserClient;
    JGObject m_colorChooserRef;
};

} // namespace WebCore

#endif  // #if ENABLE(INPUT_TYPE_COLOR)
