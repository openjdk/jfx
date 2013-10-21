/* ALSA mixer implementation.
 * Copyright (C) 2003 Leif Johnson <leif@ambient.2y.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */


#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gstalsamixerelement.h"
#include "gstalsadeviceprobe.h"

#define DEFAULT_PROP_DEVICE          "default"
#define DEFAULT_PROP_DEVICE_NAME     ""

enum
{
  PROP_0,
  PROP_DEVICE,
  PROP_DEVICE_NAME
};

static void gst_alsa_mixer_element_init_interfaces (GType type);

GST_BOILERPLATE_FULL (GstAlsaMixerElement, gst_alsa_mixer_element,
    GstElement, GST_TYPE_ELEMENT, gst_alsa_mixer_element_init_interfaces);

/* massive macro that takes care of all the GstMixer stuff */
GST_IMPLEMENT_ALSA_MIXER_METHODS (GstAlsaMixerElement, gst_alsa_mixer_element);

static void gst_alsa_mixer_element_get_property (GObject * object,
    guint prop_id, GValue * value, GParamSpec * pspec);
static void gst_alsa_mixer_element_set_property (GObject * object,
    guint prop_id, const GValue * value, GParamSpec * pspec);
static void gst_alsa_mixer_element_finalize (GObject * object);

static GstStateChangeReturn gst_alsa_mixer_element_change_state (GstElement
    * element, GstStateChange transition);

static gboolean
gst_alsa_mixer_element_interface_supported (GstAlsaMixerElement * this,
    GType interface_type)
{
  if (interface_type == GST_TYPE_MIXER) {
    return gst_alsa_mixer_element_supported (this, interface_type);
  }

  g_return_val_if_reached (FALSE);
}

static void
gst_implements_interface_init (GstImplementsInterfaceClass * klass)
{
  klass->supported = (gpointer) gst_alsa_mixer_element_interface_supported;
}

static void
gst_alsa_mixer_element_init_interfaces (GType type)
{
  static const GInterfaceInfo implements_iface_info = {
    (GInterfaceInitFunc) gst_implements_interface_init,
    NULL,
    NULL,
  };
  static const GInterfaceInfo mixer_iface_info = {
    (GInterfaceInitFunc) gst_alsa_mixer_element_interface_init,
    NULL,
    NULL,
  };

  g_type_add_interface_static (type, GST_TYPE_IMPLEMENTS_INTERFACE,
      &implements_iface_info);
  g_type_add_interface_static (type, GST_TYPE_MIXER, &mixer_iface_info);

  gst_alsa_type_add_device_property_probe_interface (type);
}

static void
gst_alsa_mixer_element_base_init (gpointer klass)
{
  gst_element_class_set_details_simple (GST_ELEMENT_CLASS (klass),
      "Alsa mixer", "Generic/Audio",
      "Control sound input and output levels with ALSA",
      "Leif Johnson <leif@ambient.2y.net>");
}

static void
gst_alsa_mixer_element_class_init (GstAlsaMixerElementClass * klass)
{
  GstElementClass *element_class;
  GObjectClass *gobject_class;

  element_class = (GstElementClass *) klass;
  gobject_class = (GObjectClass *) klass;

  gobject_class->finalize = gst_alsa_mixer_element_finalize;
  gobject_class->get_property = gst_alsa_mixer_element_get_property;
  gobject_class->set_property = gst_alsa_mixer_element_set_property;

  g_object_class_install_property (gobject_class, PROP_DEVICE,
      g_param_spec_string ("device", "Device",
          "ALSA device, as defined in an asound configuration file",
          DEFAULT_PROP_DEVICE, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_DEVICE_NAME,
      g_param_spec_string ("device-name", "Device name",
          "Human-readable name of the sound device",
          DEFAULT_PROP_DEVICE_NAME, G_PARAM_READABLE | G_PARAM_STATIC_STRINGS));

  element_class->change_state =
      GST_DEBUG_FUNCPTR (gst_alsa_mixer_element_change_state);
}

static void
gst_alsa_mixer_element_finalize (GObject * obj)
{
  GstAlsaMixerElement *this = GST_ALSA_MIXER_ELEMENT (obj);

  g_free (this->device);

  G_OBJECT_CLASS (parent_class)->finalize (obj);
}

static void
gst_alsa_mixer_element_init (GstAlsaMixerElement * this,
    GstAlsaMixerElementClass * klass)
{
  this->mixer = NULL;
  this->device = g_strdup (DEFAULT_PROP_DEVICE);
}

static void
gst_alsa_mixer_element_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAlsaMixerElement *this = GST_ALSA_MIXER_ELEMENT (object);

  switch (prop_id) {
    case PROP_DEVICE:{
      GST_OBJECT_LOCK (this);
      g_free (this->device);
      this->device = g_value_dup_string (value);
      /* make sure we never set NULL, this is nice when we want to open the
       * device. */
      if (this->device == NULL)
        this->device = g_strdup (DEFAULT_PROP_DEVICE);
      GST_OBJECT_UNLOCK (this);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_alsa_mixer_element_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAlsaMixerElement *this = GST_ALSA_MIXER_ELEMENT (object);

  switch (prop_id) {
    case PROP_DEVICE:{
      GST_OBJECT_LOCK (this);
      g_value_set_string (value, this->device);
      GST_OBJECT_UNLOCK (this);
      break;
    }
    case PROP_DEVICE_NAME:{
      GST_OBJECT_LOCK (this);
      if (this->mixer) {
        g_value_set_string (value, this->mixer->cardname);
      } else {
        g_value_set_string (value, NULL);
      }
      GST_OBJECT_UNLOCK (this);
      break;
    }
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static GstStateChangeReturn
gst_alsa_mixer_element_change_state (GstElement * element,
    GstStateChange transition)
{
  GstStateChangeReturn ret = GST_STATE_CHANGE_SUCCESS;
  GstAlsaMixerElement *this = GST_ALSA_MIXER_ELEMENT (element);

  switch (transition) {
    case GST_STATE_CHANGE_NULL_TO_READY:
      if (!this->mixer) {
        this->mixer = gst_alsa_mixer_new (this->device, GST_ALSA_MIXER_ALL);
        if (!this->mixer)
          goto open_failed;
        _gst_alsa_mixer_set_interface (this->mixer, GST_MIXER (element));
      }
      break;
    default:
      break;
  }

  ret = GST_ELEMENT_CLASS (parent_class)->change_state (element, transition);
  if (ret == GST_STATE_CHANGE_FAILURE)
    return ret;

  switch (transition) {
    case GST_STATE_CHANGE_READY_TO_NULL:
      if (this->mixer) {
        gst_alsa_mixer_free (this->mixer);
        this->mixer = NULL;
      }
      break;
    default:
      break;
  }

  return ret;

  /* ERRORS */
open_failed:
  {
    GST_ELEMENT_ERROR (element, RESOURCE, OPEN_READ_WRITE, (NULL),
        ("Failed to open alsa mixer device '%s'", this->device));
    return GST_STATE_CHANGE_FAILURE;
  }
}
