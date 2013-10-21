/* GStreamer Color Balance
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * colorbalance.c: image color balance interface design
 *                 virtual class function wrappers
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

#include "colorbalance.h"
#include "interfaces-marshal.h"

/**
 * SECTION:gstcolorbalance
 * @short_description: Interface for adjusting color balance settings
 *
 * <refsect2><para>
 * This interface is implemented by elements which can perform some color
 * balance operation on video frames they process. For example, modifying
 * the brightness, contrast, hue or saturation.
 * </para><para>
 * Example elements are 'xvimagesink' and 'colorbalance'
 * </para>
 * </refsect2>
 */

enum
{
  VALUE_CHANGED,
  LAST_SIGNAL
};

static void gst_color_balance_class_init (GstColorBalanceClass * klass);

static guint gst_color_balance_signals[LAST_SIGNAL] = { 0 };

GType
gst_color_balance_get_type (void)
{
  static GType gst_color_balance_type = 0;

  if (!gst_color_balance_type) {
    static const GTypeInfo gst_color_balance_info = {
      sizeof (GstColorBalanceClass),
      (GBaseInitFunc) gst_color_balance_class_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
    };

    gst_color_balance_type = g_type_register_static (G_TYPE_INTERFACE,
        "GstColorBalance", &gst_color_balance_info, 0);
    g_type_interface_add_prerequisite (gst_color_balance_type,
        GST_TYPE_IMPLEMENTS_INTERFACE);
  }

  return gst_color_balance_type;
}

static void
gst_color_balance_class_init (GstColorBalanceClass * klass)
{
  static gboolean initialized = FALSE;

  if (!initialized) {
    /**
     * GstColorBalance::value-changed:
     * @colorbalance: The GstColorBalance instance
     * @channel: The #GstColorBalanceChannel
     * @value: The new value
     *
     * Fired when the value of the indicated channel has changed.
     */
    gst_color_balance_signals[VALUE_CHANGED] =
        g_signal_new ("value-changed",
        GST_TYPE_COLOR_BALANCE, G_SIGNAL_RUN_LAST,
        G_STRUCT_OFFSET (GstColorBalanceClass, value_changed),
        NULL, NULL,
        gst_interfaces_marshal_VOID__OBJECT_INT,
        G_TYPE_NONE, 2, GST_TYPE_COLOR_BALANCE_CHANNEL, G_TYPE_INT);

    initialized = TRUE;
  }

  klass->balance_type = GST_COLOR_BALANCE_SOFTWARE;

  /* default virtual functions */
  klass->list_channels = NULL;
  klass->set_value = NULL;
  klass->get_value = NULL;
}

/**
 * gst_color_balance_list_channels:
 * @balance: A #GstColorBalance instance
 * 
 * Retrieve a list of the available channels.
 *
 * Returns: A GList containing pointers to #GstColorBalanceChannel objects.
 *          The list is owned by the #GstColorBalance instance and must not
 *          be freed.
 */
const GList *
gst_color_balance_list_channels (GstColorBalance * balance)
{
  GstColorBalanceClass *klass = GST_COLOR_BALANCE_GET_CLASS (balance);

  if (klass->list_channels) {
    return klass->list_channels (balance);
  }

  return NULL;
}

/**
 * gst_color_balance_set_value:
 * @balance: A #GstColorBalance instance
 * @channel: A #GstColorBalanceChannel instance
 * @value: The new value for the channel.
 *
 * Sets the current value of the channel to the passed value, which must
 * be between min_value and max_value.
 * 
 * See Also: The #GstColorBalanceChannel::min_value and
 *         #GstColorBalanceChannel::max_value members of the
 *         #GstColorBalanceChannel object.
 */
void
gst_color_balance_set_value (GstColorBalance * balance,
    GstColorBalanceChannel * channel, gint value)
{
  GstColorBalanceClass *klass = GST_COLOR_BALANCE_GET_CLASS (balance);

  if (klass->set_value) {
    klass->set_value (balance, channel, value);
  }
}

/**
 * gst_color_balance_get_value:
 * @balance: A #GstColorBalance instance
 * @channel: A #GstColorBalanceChannel instance
 *
 * Retrieve the current value of the indicated channel, between min_value
 * and max_value.
 * 
 * See Also: The #GstColorBalanceChannel::min_value and
 *         #GstColorBalanceChannel::max_value members of the
 *         #GstColorBalanceChannel object.
 * 
 * Returns: The current value of the channel.
 */
gint
gst_color_balance_get_value (GstColorBalance * balance,
    GstColorBalanceChannel * channel)
{
  GstColorBalanceClass *klass = GST_COLOR_BALANCE_GET_CLASS (balance);

  if (klass->get_value) {
    return klass->get_value (balance, channel);
  }

  return channel->min_value;
}

/**
 * gst_color_balance_get_balance_type:
 * @balance: The #GstColorBalance implementation
 *
 * Get the #GstColorBalanceType of this implementation.
 *
 * Returns: A the #GstColorBalanceType.
 *
 * Since: 0.10.24
 */
GstColorBalanceType
gst_color_balance_get_balance_type (GstColorBalance * balance)
{
  GstColorBalanceClass *klass = GST_COLOR_BALANCE_GET_CLASS (balance);

  return klass->balance_type;
}

/**
 * gst_color_balance_value_changed:
 * @balance: A #GstColorBalance instance
 * @channel: A #GstColorBalanceChannel whose value has changed
 * @value: The new value of the channel
 *
 * A helper function called by implementations of the GstColorBalance
 * interface. It fires the #GstColorBalance::value-changed signal on the
 * instance, and the #GstColorBalanceChannel::value-changed signal on the
 * channel object.
 */
void
gst_color_balance_value_changed (GstColorBalance * balance,
    GstColorBalanceChannel * channel, gint value)
{
  g_signal_emit (G_OBJECT (balance),
      gst_color_balance_signals[VALUE_CHANGED], 0, channel, value);

  g_signal_emit_by_name (G_OBJECT (channel), "value_changed", value);
}
