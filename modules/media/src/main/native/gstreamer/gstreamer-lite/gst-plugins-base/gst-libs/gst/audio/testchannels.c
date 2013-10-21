/* GStreamer Multichannel Test
 * (c) 2004 Ronald Bultje <rbultje@ronald.bitfreak.net>
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

#include "multichannel.c"
#include "audio-enumtypes.c"

gint
main (gint argc, gchar * argv[])
{
  gchar *str;
  GstCaps *caps;
  GstAudioChannelPosition pos[2] = { GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
    GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT
  };

  /* register multichannel type */
  gst_init (&argc, &argv);
  gst_audio_channel_position_get_type ();

  /* test some caps-string conversions */
  caps = gst_caps_new_simple ("audio/x-raw-int",
      "channels", G_TYPE_INT, 2, NULL);
  str = gst_caps_to_string (caps);
  g_print ("Test caps #1: %s\n", str);
  g_free (str);
  gst_audio_set_channel_positions (gst_caps_get_structure (caps, 0), pos);
  str = gst_caps_to_string (caps);
  g_print ("Test caps #2: %s\n", str);
  g_free (str);
  gst_caps_unref (caps);

  return 0;
}
