/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#ifndef RQRef_h
#define RQRef_h

#include <jni.h>
#include <JavaEnv.h>
#include <wtf/RefCounted.h>
#include <wtf/PassRef.h>
#include <wtf/RefPtr.h>

namespace WebCore {

class RQRef : public RefCounted<RQRef> {
public:
    inline static PassRefPtr<RQRef> create(const JLObject &obj)
    {
        return obj ? adoptRef(new RQRef(obj)) : 0;
    }
    operator jint();
    operator jobject() {return m_ref;}
    JLObject cloneLocalCopy() const {
        return m_ref;
    }
    ~RQRef();

private:
    RQRef(const JLObject &obj)
        : m_ref(obj)
        , m_refID(-1)
    {}

    JGObject m_ref;
    jint m_refID;
};

}
#endif
