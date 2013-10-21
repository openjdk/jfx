/*
 * Copyright (C) 2001 CodeFactory AB
 * Copyright (C) 2001 Thomas Nyberg <thomas@codefactory.se>
 * Copyright (C) 2001-2002 Andy Wingo <apwingo@eos.ncsu.edu>
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
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


#ifndef __GST_ALSA_H__
#define __GST_ALSA_H__


#define ALSA_PCM_NEW_HW_PARAMS_API
#define ALSA_PCM_NEW_SW_PARAMS_API

#include <alsa/asoundlib.h>
#include <alsa/control.h>
#include <alsa/error.h>
#include <gst/gst.h>

#define GST_CHECK_ALSA_VERSION(major,minor,micro) \
    (SND_LIB_MAJOR > (major) || \
     (SND_LIB_MAJOR == (major) && SND_LIB_MINOR > (minor)) || \
     (SND_LIB_MAJOR == (major) && SND_LIB_MINOR == (minor) && \
      SND_LIB_SUBMINOR >= (micro)))

GST_DEBUG_CATEGORY_EXTERN (alsa_debug);
#define GST_CAT_DEFAULT alsa_debug

snd_pcm_t * gst_alsa_open_iec958_pcm (GstObject * obj);

GstCaps * gst_alsa_probe_supported_formats (GstObject      * obj,
                                            snd_pcm_t      * handle,
                                            const GstCaps  * template_caps);

gchar   * gst_alsa_find_device_name (GstObject        * obj,
                                     const gchar      * device,
                                     snd_pcm_t        * handle,
                                     snd_pcm_stream_t   stream);

gchar *   gst_alsa_find_card_name   (GstObject        * obj,
                                     const gchar      * devcard,
                                     snd_pcm_stream_t   stream);


#endif /* __GST_ALSA_H__ */
