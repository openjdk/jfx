/* ALSA mixer interface implementation.
 * Copyright (C) 2003 Leif Johnson <leif@ambient.2y.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */


#ifndef __GST_ALSA_MIXER_ELEMENT_H__
#define __GST_ALSA_MIXER_ELEMENT_H__


#include "gstalsa.h"
#include "gstalsamixer.h"

G_BEGIN_DECLS

#define GST_ALSA_MIXER_ELEMENT(obj)             (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_ALSA_MIXER_ELEMENT,GstAlsaMixerElement))
#define GST_ALSA_MIXER_ELEMENT_CLASS(klass)     (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_ALSA_MIXER_ELEMENT,GstAlsaMixerElementClass))
#define GST_IS_ALSA_MIXER_ELEMENT(obj)          (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_ALSA_MIXER_ELEMENT))
#define GST_IS_ALSA_MIXER_ELEMENT_CLASS(klass)  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_ALSA_MIXER_ELEMENT))
#define GST_TYPE_ALSA_MIXER_ELEMENT             (gst_alsa_mixer_element_get_type())

typedef struct _GstAlsaMixerElement GstAlsaMixerElement;
typedef struct _GstAlsaMixerElementClass GstAlsaMixerElementClass;

/**
 * GstAlsaMixerElement
 *
 * Opaque datastructure.
 */
struct _GstAlsaMixerElement {
  GstElement            parent;

  GstAlsaMixer          *mixer;
  gchar                 *device;
};

struct _GstAlsaMixerElementClass {
  GstElementClass       parent;
};


GType           gst_alsa_mixer_element_get_type         (void);


G_END_DECLS


#endif /* __GST_ALSA_MIXER_ELEMENT_H__ */
