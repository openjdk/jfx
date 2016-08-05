/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __GST_ELEMENT_CONTAINER__
#define __GST_ELEMENT_CONTAINER__

#include <gst/gst.h>
#include <map>

enum ElementRole
{
    PIPELINE,
    SOURCE,
    AUDIO_QUEUE,
    AUDIO_PARSER,
    AUDIO_DECODER,
    AUDIO_BALANCE,
    AUDIO_EQUALIZER,
    AUDIO_SPECTRUM,
    AUDIO_VOLUME,
    AUDIO_SINK,
    AV_DEMUXER,
    AUDIO_BIN,
    VIDEO_BIN,
    VIDEO_DECODER,
    VIDEO_SINK,
    VIDEO_QUEUE
};

/*
 * Immutable container for elements necessary for a pipeline.
 */
typedef std::map<ElementRole, GstElement*> ElementsMap;

class GstElementContainer
{
public:
    GstElementContainer();
    GstElementContainer(const GstElementContainer& container);
    ~GstElementContainer();

    void Dispose();

    GstElementContainer& add(ElementRole role, GstElement* element);
    GstElement* operator[](ElementRole role) const;

private:
    ElementsMap m_Map;
};

#endif // __GST_ELEMENT_CONTAINER__
