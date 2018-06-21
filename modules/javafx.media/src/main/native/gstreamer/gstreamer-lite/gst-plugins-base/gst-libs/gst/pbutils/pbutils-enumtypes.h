


#ifndef __PB_UTILS_ENUM_TYPES_H__
#define __PB_UTILS_ENUM_TYPES_H__

#include <gst/gst.h>
#include <gst/pbutils/pbutils-prelude.h>
G_BEGIN_DECLS

/* enumerations from "gstaudiovisualizer.h" */
GST_PBUTILS_API
GType gst_audio_visualizer_shader_get_type (void);
#define GST_TYPE_AUDIO_VISUALIZER_SHADER (gst_audio_visualizer_shader_get_type())

/* enumerations from "gstdiscoverer.h" */
GST_PBUTILS_API
GType gst_discoverer_result_get_type (void);
#define GST_TYPE_DISCOVERER_RESULT (gst_discoverer_result_get_type())
GST_PBUTILS_API
GType gst_discoverer_serialize_flags_get_type (void);
#define GST_TYPE_DISCOVERER_SERIALIZE_FLAGS (gst_discoverer_serialize_flags_get_type())

/* enumerations from "install-plugins.h" */
GST_PBUTILS_API
GType gst_install_plugins_return_get_type (void);
#define GST_TYPE_INSTALL_PLUGINS_RETURN (gst_install_plugins_return_get_type())
G_END_DECLS

#endif /* __PB_UTILS_ENUM_TYPES_H__ */



