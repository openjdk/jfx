/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef ColorChooserJava_h
#define ColorChooserJava_h

#if ENABLE(INPUT_TYPE_COLOR)
#include "JavaEnv.h"
#include "ColorChooser.h"

namespace WebCore {
class ColorChooserClient;

class ColorChooserJava : public ColorChooser {
public:
    ColorChooserJava(JGObject&, ColorChooserClient*, const Color&);
    ColorChooserClient* getClient() { return m_colorChooserClient; }

    ~ColorChooserJava() { }
    void reattachColorChooser(const Color&) override;
    void setSelectedColor(const Color&) override;
    void endChooser() override;

private:
    ColorChooserClient* m_colorChooserClient;
    JGObject m_colorChooserRef;
};

} // namespace WebCore

#endif  // #if ENABLE(INPUT_TYPE_COLOR)

#endif  // ColorChooserJava_h
