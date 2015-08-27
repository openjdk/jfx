/* Copyright (C) 2001 CodeFactory AB
 * Copyright (C) 2001 Thomas Nyberg <thomas@codefactory.se>
 * Copyright (C) 2001-2002 Andy Wingo <apwingo@eos.ncsu.edu>
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 * Copyright (C) 2005 Tim-Philipp Müller <tim centricular net>
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
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA.
 */

/* FIXME 0.11: suppress warnings for deprecated API such as GValueArray
 * with newer GLib versions (>= 2.31.0) */
#define GLIB_DISABLE_DEPRECATION_WARNINGS

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstalsadeviceprobe.h"

#if 0
G_LOCK_DEFINE_STATIC (probe_lock);

static const GList *
gst_alsa_device_property_probe_get_properties (GstPropertyProbe * probe)
{
  GObjectClass *klass = G_OBJECT_GET_CLASS (probe);
  static GList *list = NULL;

  G_LOCK (probe_lock);

  if (!list) {
    GParamSpec *pspec;

    pspec = g_object_class_find_property (klass, "device");
    list = g_list_append (NULL, pspec);
  }

  G_UNLOCK (probe_lock);

  return list;
}

static GList *
gst_alsa_get_device_list (snd_pcm_stream_t stream)
{
  snd_ctl_t *handle;
  int card, dev;
  snd_ctl_card_info_t *info;
  snd_pcm_info_t *pcminfo;
  gboolean mixer = (stream == -1);
  GList *list = NULL;

  if (stream == -1)
    stream = 0;

  snd_ctl_card_info_malloc (&info);
  snd_pcm_info_malloc (&pcminfo);
  card = -1;

  if (snd_card_next (&card) < 0 || card < 0) {
    /* no soundcard found */
    GST_WARNING ("No soundcard found");
    goto beach;
  }

  while (card >= 0) {
    gchar name[32];

    g_snprintf (name, sizeof (name), "hw:%d", card);
    if (snd_ctl_open (&handle, name, 0) < 0) {
      goto next_card;
    }
    if (snd_ctl_card_info (handle, info) < 0) {
      snd_ctl_close (handle);
      goto next_card;
    }

    if (mixer) {
      list = g_list_append (list, g_strdup (name));
    } else {
      dev = -1;
      while (1) {
        gchar *gst_device;

        snd_ctl_pcm_next_device (handle, &dev);

        if (dev < 0)
          break;
        snd_pcm_info_set_device (pcminfo, dev);
        snd_pcm_info_set_subdevice (pcminfo, 0);
        snd_pcm_info_set_stream (pcminfo, stream);
        if (snd_ctl_pcm_info (handle, pcminfo) < 0) {
          continue;
        }

        gst_device = g_strdup_printf ("hw:%d,%d", card, dev);
        list = g_list_append (list, gst_device);
      }
    }
    snd_ctl_close (handle);
  next_card:
    if (snd_card_next (&card) < 0) {
      break;
    }
  }

beach:
  snd_ctl_card_info_free (info);
  snd_pcm_info_free (pcminfo);

  return list;
}

static void
gst_alsa_device_property_probe_probe_property (GstPropertyProbe * probe,
    guint prop_id, const GParamSpec * pspec)
{
  if (!g_str_equal (pspec->name, "device")) {
    G_OBJECT_WARN_INVALID_PROPERTY_ID (probe, prop_id, pspec);
  }
}

static gboolean
gst_alsa_device_property_probe_needs_probe (GstPropertyProbe * probe,
    guint prop_id, const GParamSpec * pspec)
{
  /* don't cache probed data */
  return TRUE;
}

static GValueArray *
gst_alsa_device_property_probe_get_values (GstPropertyProbe * probe,
    guint prop_id, const GParamSpec * pspec)
{
  GstElementClass *klass;
  const GList *templates;
  snd_pcm_stream_t mode = -1;
  GValueArray *array;
  GValue value = { 0, };
  GList *l, *list;

  if (!g_str_equal (pspec->name, "device")) {
    G_OBJECT_WARN_INVALID_PROPERTY_ID (probe, prop_id, pspec);
    return NULL;
  }

  klass = GST_ELEMENT_GET_CLASS (GST_ELEMENT (probe));

  /* I'm pretty sure ALSA has a good way to do this. However, their cool
   * auto-generated documentation is pretty much useless if you try to
   * do function-wise look-ups. */
  /* we assume one pad template at max [zero=mixer] */
  templates = gst_element_class_get_pad_template_list (klass);
  if (templates) {
    if (GST_PAD_TEMPLATE_DIRECTION (templates->data) == GST_PAD_SRC)
      mode = SND_PCM_STREAM_CAPTURE;
    else
      mode = SND_PCM_STREAM_PLAYBACK;
  }

  list = gst_alsa_get_device_list (mode);

  if (list == NULL) {
    GST_LOG_OBJECT (probe, "No devices found");
    return NULL;
  }

  array = g_value_array_new (g_list_length (list));
  g_value_init (&value, G_TYPE_STRING);
  for (l = list; l != NULL; l = l->next) {
    GST_LOG_OBJECT (probe, "Found device: %s", (gchar *) l->data);
    g_value_take_string (&value, (gchar *) l->data);
    l->data = NULL;
    g_value_array_append (array, &value);
  }
  g_value_unset (&value);
  g_list_free (list);

  return array;
}

static void
gst_alsa_property_probe_interface_init (GstPropertyProbeInterface * iface)
{
  iface->get_properties = gst_alsa_device_property_probe_get_properties;
  iface->probe_property = gst_alsa_device_property_probe_probe_property;
  iface->needs_probe = gst_alsa_device_property_probe_needs_probe;
  iface->get_values = gst_alsa_device_property_probe_get_values;
}

void
gst_alsa_type_add_device_property_probe_interface (GType type)
{
  static const GInterfaceInfo probe_iface_info = {
    (GInterfaceInitFunc) gst_alsa_property_probe_interface_init,
    NULL,
    NULL,
  };

  g_type_add_interface_static (type, GST_TYPE_PROPERTY_PROBE,
      &probe_iface_info);
}
#endif
