/* GStreamer X-based Overlay
 * Copyright (C) 2003 Ronald Bultje <rbultje@ronald.bitfreak.net>
 *
 * x-overlay.c: X-based overlay interface design
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
/**
 * SECTION:gstxoverlay
 * @short_description: Interface for setting/getting a Window on elements
 * supporting it
 *
 * <refsect2>
 * <para>
 * The XOverlay interface is used for 2 main purposes :
 * <itemizedlist>
 * <listitem>
 * <para>
 * To get a grab on the Window where the video sink element is going to render.
 * This is achieved by either being informed about the Window identifier that
 * the video sink element generated, or by forcing the video sink element to use
 * a specific Window identifier for rendering.
 * </para>
 * </listitem>
 * <listitem>
 * <para>
 * To force a redrawing of the latest video frame the video sink element
 * displayed on the Window. Indeed if the #GstPipeline is in #GST_STATE_PAUSED
 * state, moving the Window around will damage its content. Application
 * developers will want to handle the Expose events themselves and force the
 * video sink element to refresh the Window's content.
 * </para>
 * </listitem>
 * </itemizedlist>
 * </para>
 * <para>
 * Using the Window created by the video sink is probably the simplest scenario,
 * in some cases, though, it might not be flexible enough for application
 * developers if they need to catch events such as mouse moves and button
 * clicks.
 * </para>
 * <para>
 * Setting a specific Window identifier on the video sink element is the most
 * flexible solution but it has some issues. Indeed the application needs to set
 * its Window identifier at the right time to avoid internal Window creation
 * from the video sink element. To solve this issue a #GstMessage is posted on
 * the bus to inform the application that it should set the Window identifier
 * immediately. Here is an example on how to do that correctly:
 * |[
 * static GstBusSyncReply
 * create_window (GstBus * bus, GstMessage * message, GstPipeline * pipeline)
 * {
 *  // ignore anything but 'prepare-xwindow-id' element messages
 *  if (GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT)
 *    return GST_BUS_PASS;
 *  
 *  if (!gst_structure_has_name (message-&gt;structure, "prepare-xwindow-id"))
 *    return GST_BUS_PASS;
 *  
 *  win = XCreateSimpleWindow (disp, root, 0, 0, 320, 240, 0, 0, 0);
 *  
 *  XSetWindowBackgroundPixmap (disp, win, None);
 *  
 *  XMapRaised (disp, win);
 *  
 *  XSync (disp, FALSE);
 *   
 *  gst_x_overlay_set_window_handle (GST_X_OVERLAY (GST_MESSAGE_SRC (message)),
 *      win);
 *   
 *  gst_message_unref (message);
 *   
 *  return GST_BUS_DROP;
 * }
 * ...
 * int
 * main (int argc, char **argv)
 * {
 * ...
 *  bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
 *  gst_bus_set_sync_handler (bus, (GstBusSyncHandler) create_window, pipeline);
 * ...
 * }
 * ]|
 * </para>
 * </refsect2>
 * <refsect2>
 * <title>Two basic usage scenarios</title>
 * <para>
 * There are two basic usage scenarios: in the simplest case, the application
 * knows exactly what particular element is used for video output, which is
 * usually the case when the application creates the videosink to use
 * (e.g. #xvimagesink, #ximagesink, etc.) itself; in this case, the application
 * can just create the videosink element, create and realize the window to
 * render the video on and then call gst_x_overlay_set_window_handle() directly
 * with the XID or native window handle, before starting up the pipeline.
 * </para>
 * <para>
 * In the other and more common case, the application does not know in advance
 * what GStreamer video sink element will be used for video output. This is
 * usually the case when an element such as #autovideosink or #gconfvideosink
 * is used. In this case, the video sink element itself is created
 * asynchronously from a GStreamer streaming thread some time after the
 * pipeline has been started up. When that happens, however, the video sink
 * will need to know right then whether to render onto an already existing
 * application window or whether to create its own window. This is when it
 * posts a prepare-xwindow-id message, and that is also why this message needs
 * to be handled in a sync bus handler which will be called from the streaming
 * thread directly (because the video sink will need an answer right then).
 * </para>
 * <para>
 * As response to the prepare-xwindow-id element message in the bus sync
 * handler, the application may use gst_x_overlay_set_window_handle() to tell
 * the video sink to render onto an existing window surface. At this point the
 * application should already have obtained the window handle / XID, so it
 * just needs to set it. It is generally not advisable to call any GUI toolkit
 * functions or window system functions from the streaming thread in which the
 * prepare-xwindow-id message is handled, because most GUI toolkits and
 * windowing systems are not thread-safe at all and a lot of care would be
 * required to co-ordinate the toolkit and window system calls of the
 * different threads (Gtk+ users please note: prior to Gtk+ 2.18
 * GDK_WINDOW_XID() was just a simple structure access, so generally fine to do
 * within the bus sync handler; this macro was changed to a function call in
 * Gtk+ 2.18 and later, which is likely to cause problems when called from a
 * sync handler; see below for a better approach without GDK_WINDOW_XID()
 * used in the callback).
 * </para>
 * </refsect2>
 * <refsect2>
 * <title>GstXOverlay and Gtk+</title>
 * <para>
 * |[
 * #include &lt;gtk/gtk.h&gt;
 * #ifdef GDK_WINDOWING_X11
 * #include &lt;gdk/gdkx.h&gt;  // for GDK_WINDOW_XID
 * #endif
 * ...
 * static gulong video_window_xid = 0;
 * ...
 * static GstBusSyncReply
 * bus_sync_handler (GstBus * bus, GstMessage * message, gpointer user_data)
 * {
 *  // ignore anything but 'prepare-xwindow-id' element messages
 *  if (GST_MESSAGE_TYPE (message) != GST_MESSAGE_ELEMENT)
 *    return GST_BUS_PASS;
 *  if (!gst_structure_has_name (message-&gt;structure, "prepare-xwindow-id"))
 *    return GST_BUS_PASS;
 *  
 *  if (video_window_xid != 0) {
 *    GstXOverlay *xoverlay;
 *    
 *    // GST_MESSAGE_SRC (message) will be the video sink element
 *    xoverlay = GST_X_OVERLAY (GST_MESSAGE_SRC (message));
 *    gst_x_overlay_set_window_handle (xoverlay, video_window_xid);
 *  } else {
 *    g_warning ("Should have obtained video_window_xid by now!");
 *  }
 *  
 *  gst_message_unref (message);
 *  return GST_BUS_DROP;
 * }
 * ...
 * static void
 * video_widget_realize_cb (GtkWidget * widget, gpointer data)
 * {
 * #if GTK_CHECK_VERSION(2,18,0)
 *   // This is here just for pedagogical purposes, GDK_WINDOW_XID will call
 *   // it as well in newer Gtk versions
 *   if (!gdk_window_ensure_native (widget->window))
 *     g_error ("Couldn't create native window needed for GstXOverlay!");
 * #endif
 * 
 * #ifdef GDK_WINDOWING_X11
 *   video_window_xid = GDK_WINDOW_XID (video_window->window);
 * #endif
 * }
 * ...
 * int
 * main (int argc, char **argv)
 * {
 *   GtkWidget *video_window;
 *   GtkWidget *app_window;
 *   ...
 *   app_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
 *   ...
 *   video_window = gtk_drawing_area_new ();
 *   g_signal_connect (video_window, "realize",
 *       G_CALLBACK (video_widget_realize_cb), NULL);
 *   gtk_widget_set_double_buffered (video_window, FALSE);
 *   ...
 *   // usually the video_window will not be directly embedded into the
 *   // application window like this, but there will be many other widgets
 *   // and the video window will be embedded in one of them instead
 *   gtk_container_add (GTK_CONTAINER (ap_window), video_window);
 *   ...
 *   // show the GUI
 *   gtk_widget_show_all (app_window);
 * 
 *   // realize window now so that the video window gets created and we can
 *   // obtain its XID before the pipeline is started up and the videosink
 *   // asks for the XID of the window to render onto
 *   gtk_widget_realize (window);
 * 
 *   // we should have the XID now
 *   g_assert (video_window_xid != 0);
 *   ...
 *   // set up sync handler for setting the xid once the pipeline is started
 *   bus = gst_pipeline_get_bus (GST_PIPELINE (pipeline));
 *   gst_bus_set_sync_handler (bus, (GstBusSyncHandler) bus_sync_handler, NULL);
 *   gst_object_unref (bus);
 *   ...
 *   gst_element_set_state (pipeline, GST_STATE_PLAYING);
 *   ...
 * }
 * ]|
 * </para>
 * </refsect2>
 * <refsect2>
 * <title>GstXOverlay and Qt</title>
 * <para>
 * |[
 * #include &lt;glib.h&gt;
 * #include &lt;gst/gst.h&gt;
 * #include &lt;gst/interfaces/xoverlay.h&gt;
 * 
 * #include &lt;QApplication&gt;
 * #include &lt;QTimer&gt;
 * #include &lt;QWidget&gt;
 * 
 * int main(int argc, char *argv[])
 * {
 *   if (!g_thread_supported ())
 *     g_thread_init (NULL);
 * 
 *   gst_init (&argc, &argv);
 *   QApplication app(argc, argv);
 *   app.connect(&app, SIGNAL(lastWindowClosed()), &app, SLOT(quit ()));
 * 
 *   // prepare the pipeline
 * 
 *   GstElement *pipeline = gst_pipeline_new ("xvoverlay");
 *   GstElement *src = gst_element_factory_make ("videotestsrc", NULL);
 *   GstElement *sink = gst_element_factory_make ("xvimagesink", NULL);
 *   gst_bin_add_many (GST_BIN (pipeline), src, sink, NULL);
 *   gst_element_link (src, sink);
 *   
 *   // prepare the ui
 * 
 *   QWidget window;
 *   window.resize(320, 240);
 *   window.show();
 *   
 *   WId xwinid = window.winId();
 *   gst_x_overlay_set_window_handle (GST_X_OVERLAY (sink), xwinid);
 * 
 *   // run the pipeline
 * 
 *   GstStateChangeReturn sret = gst_element_set_state (pipeline,
 *       GST_STATE_PLAYING);
 *   if (sret == GST_STATE_CHANGE_FAILURE) {
 *     gst_element_set_state (pipeline, GST_STATE_NULL);
 *     gst_object_unref (pipeline);
 *     // Exit application
 *     QTimer::singleShot(0, QApplication::activeWindow(), SLOT(quit()));
 *   }
 * 
 *   int ret = app.exec();
 *   
 *   window.hide();
 *   gst_element_set_state (pipeline, GST_STATE_NULL);
 *   gst_object_unref (pipeline);
 * 
 *   return ret;
 * }
 * ]|
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "xoverlay.h"

static void gst_x_overlay_base_init (gpointer g_class);

GType
gst_x_overlay_get_type (void)
{
  static GType gst_x_overlay_type = 0;

  if (!gst_x_overlay_type) {
    static const GTypeInfo gst_x_overlay_info = {
      sizeof (GstXOverlayClass),
      gst_x_overlay_base_init,
      NULL,
      NULL,
      NULL,
      NULL,
      0,
      0,
      NULL,
    };

    gst_x_overlay_type = g_type_register_static (G_TYPE_INTERFACE,
        "GstXOverlay", &gst_x_overlay_info, 0);
    g_type_interface_add_prerequisite (gst_x_overlay_type,
        GST_TYPE_IMPLEMENTS_INTERFACE);
  }

  return gst_x_overlay_type;
}

static void
gst_x_overlay_base_init (gpointer g_class)
{

}

/**
 * gst_x_overlay_set_xwindow_id:
 * @overlay: a #GstXOverlay to set the XWindow on.
 * @xwindow_id: a #XID referencing the XWindow.
 *
 * This will call the video overlay's set_xwindow_id method. You should
 * use this method to tell to a XOverlay to display video output to a
 * specific XWindow. Passing 0 as the xwindow_id will tell the overlay to
 * stop using that window and create an internal one.
 *
 * Deprecated: Use gst_x_overlay_set_window_handle() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
void gst_x_overlay_set_xwindow_id (GstXOverlay * overlay, gulong xwindow_id);
#endif
void
gst_x_overlay_set_xwindow_id (GstXOverlay * overlay, gulong xwindow_id)
{
  GST_WARNING_OBJECT (overlay,
      "Using deprecated gst_x_overlay_set_xwindow_id()");
  gst_x_overlay_set_window_handle (overlay, xwindow_id);
}
#endif

/**
 * gst_x_overlay_set_window_handle:
 * @overlay: a #GstXOverlay to set the XWindow on.
 * @xwindow_id: a #XID referencing the XWindow.
 *
 * This will call the video overlay's set_window_handle method. You
 * should use this method to tell to a XOverlay to display video output to a
 * specific XWindow. Passing 0 as the xwindow_id will tell the overlay to
 * stop using that window and create an internal one.
 *
 * Since: 0.10.31
 */
void
gst_x_overlay_set_window_handle (GstXOverlay * overlay, guintptr handle)
{
  GstXOverlayClass *klass;

  g_return_if_fail (overlay != NULL);
  g_return_if_fail (GST_IS_X_OVERLAY (overlay));

  klass = GST_X_OVERLAY_GET_CLASS (overlay);

  if (klass->set_window_handle) {
    klass->set_window_handle (overlay, handle);
  } else {
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
#define set_xwindow_id set_xwindow_id_disabled
#endif
    if (sizeof (guintptr) <= sizeof (gulong) && klass->set_xwindow_id) {
      GST_WARNING_OBJECT (overlay,
          "Calling deprecated set_xwindow_id() method");
      klass->set_xwindow_id (overlay, handle);
    } else {
      g_warning ("Refusing to cast guintptr to smaller gulong");
    }
#endif
  }
}

/**
 * gst_x_overlay_got_xwindow_id:
 * @overlay: a #GstXOverlay which got a XWindow.
 * @xwindow_id: a #XID referencing the XWindow.
 *
 * This will post a "have-xwindow-id" element message on the bus.
 *
 * This function should only be used by video overlay plugin developers.
 *
 * Deprecated: Use gst_x_overlay_got_window_handle() instead.
 */
#ifndef GST_REMOVE_DEPRECATED
#ifdef GST_DISABLE_DEPRECATED
void gst_x_overlay_got_xwindow_id (GstXOverlay * overlay, gulong xwindow_id);
#endif
void
gst_x_overlay_got_xwindow_id (GstXOverlay * overlay, gulong xwindow_id)
{
  GST_WARNING_OBJECT (overlay,
      "Using deprecated gst_x_overlay_got_xwindow_id()");
  gst_x_overlay_got_window_handle (overlay, xwindow_id);
}
#endif

/**
 * gst_x_overlay_got_window_handle:
 * @overlay: a #GstXOverlay which got a window
 * @handle: a platform-specific handle referencing the window
 *
 * This will post a "have-xwindow-id" element message on the bus.
 *
 * This function should only be used by video overlay plugin developers.
 */
void
gst_x_overlay_got_window_handle (GstXOverlay * overlay, guintptr handle)
{
  GstStructure *s;
  GstMessage *msg;

  g_return_if_fail (overlay != NULL);
  g_return_if_fail (GST_IS_X_OVERLAY (overlay));

  GST_LOG_OBJECT (GST_OBJECT (overlay), "xwindow_id = %p", (gpointer)
      handle);
  s = gst_structure_new ("have-xwindow-id",
      "xwindow-id", G_TYPE_ULONG, (unsigned long) handle,
      "window-handle", G_TYPE_UINT64, (guint64) handle, NULL);
  msg = gst_message_new_element (GST_OBJECT (overlay), s);
  gst_element_post_message (GST_ELEMENT (overlay), msg);
}

/**
 * gst_x_overlay_prepare_xwindow_id:
 * @overlay: a #GstXOverlay which does not yet have an XWindow.
 *
 * This will post a "prepare-xwindow-id" element message on the bus
 * to give applications an opportunity to call 
 * gst_x_overlay_set_xwindow_id() before a plugin creates its own
 * window.
 *
 * This function should only be used by video overlay plugin developers.
 */
void
gst_x_overlay_prepare_xwindow_id (GstXOverlay * overlay)
{
  GstStructure *s;
  GstMessage *msg;

  g_return_if_fail (overlay != NULL);
  g_return_if_fail (GST_IS_X_OVERLAY (overlay));

  GST_LOG_OBJECT (GST_OBJECT (overlay), "prepare xwindow_id");
  s = gst_structure_new ("prepare-xwindow-id", NULL);
  msg = gst_message_new_element (GST_OBJECT (overlay), s);
  gst_element_post_message (GST_ELEMENT (overlay), msg);
}

/**
 * gst_x_overlay_expose:
 * @overlay: a #GstXOverlay to expose.
 *
 * Tell an overlay that it has been exposed. This will redraw the current frame
 * in the drawable even if the pipeline is PAUSED.
 */
void
gst_x_overlay_expose (GstXOverlay * overlay)
{
  GstXOverlayClass *klass;

  g_return_if_fail (overlay != NULL);
  g_return_if_fail (GST_IS_X_OVERLAY (overlay));

  klass = GST_X_OVERLAY_GET_CLASS (overlay);

  if (klass->expose) {
    klass->expose (overlay);
  }
}

/**
 * gst_x_overlay_handle_events:
 * @overlay: a #GstXOverlay to expose.
 * @handle_events: a #gboolean indicating if events should be handled or not.
 *
 * Tell an overlay that it should handle events from the window system. These
 * events are forwared upstream as navigation events. In some window system,
 * events are not propagated in the window hierarchy if a client is listening
 * for them. This method allows you to disable events handling completely
 * from the XOverlay.
 *
 * Since: 0.10.12
 */
void
gst_x_overlay_handle_events (GstXOverlay * overlay, gboolean handle_events)
{
  GstXOverlayClass *klass;

  g_return_if_fail (overlay != NULL);
  g_return_if_fail (GST_IS_X_OVERLAY (overlay));

  klass = GST_X_OVERLAY_GET_CLASS (overlay);

  if (klass->handle_events) {
    klass->handle_events (overlay, handle_events);
  }
}

/**
 * gst_x_overlay_set_render_rectangle:
 * @overlay: a #GstXOverlay
 * @x: the horizontal offset of the render area inside the window
 * @y: the vertical offset of the render area inside the window
 * @width: the width of the render area inside the window
 * @height: the height of the render area inside the window
 *
 * Configure a subregion as a video target within the window set by
 * gst_x_overlay_set_window_handle(). If this is not used or not supported
 * the video will fill the area of the window set as the overlay to 100%.
 * By specifying the rectangle, the video can be overlayed to a specific region
 * of that window only. After setting the new rectangle one should call
 * gst_x_overlay_expose() to force a redraw. To unset the region pass -1 for
 * the @width and @height parameters.
 *
 * This method is needed for non fullscreen video overlay in UI toolkits that
 * do not support subwindows.
 *
 * Returns: %FALSE if not supported by the sink.
 *
 * Since: 0.10.29
 */
gboolean
gst_x_overlay_set_render_rectangle (GstXOverlay * overlay,
    gint x, gint y, gint width, gint height)
{
  GstXOverlayClass *klass;

  g_return_val_if_fail (overlay != NULL, FALSE);
  g_return_val_if_fail (GST_IS_X_OVERLAY (overlay), FALSE);
  g_return_val_if_fail ((width == -1 && height == -1) ||
      (width > 0 && height > 0), FALSE);

  klass = GST_X_OVERLAY_GET_CLASS (overlay);

  if (klass->set_render_rectangle) {
    klass->set_render_rectangle (overlay, x, y, width, height);
    return TRUE;
  }
  return FALSE;
}
