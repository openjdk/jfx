#include "gst/gstconfig.h" 

#ifndef __gst_interfaces_marshal_MARSHAL_H__
#define __gst_interfaces_marshal_MARSHAL_H__

#include	<glib-object.h>

G_BEGIN_DECLS

/* VOID:OBJECT,BOOLEAN (..\..\gst-libs\gst\interfaces\interfaces-marshal.list:1) */
extern void gst_interfaces_marshal_VOID__OBJECT_BOOLEAN (GClosure     *closure,
                                                         GValue       *return_value,
                                                         guint         n_param_values,
                                                         const GValue *param_values,
                                                         gpointer      invocation_hint,
                                                         gpointer      marshal_data);

/* VOID:OBJECT,POINTER (..\..\gst-libs\gst\interfaces\interfaces-marshal.list:2) */
extern void gst_interfaces_marshal_VOID__OBJECT_POINTER (GClosure     *closure,
                                                         GValue       *return_value,
                                                         guint         n_param_values,
                                                         const GValue *param_values,
                                                         gpointer      invocation_hint,
                                                         gpointer      marshal_data);

/* VOID:OBJECT,STRING (..\..\gst-libs\gst\interfaces\interfaces-marshal.list:3) */
extern void gst_interfaces_marshal_VOID__OBJECT_STRING (GClosure     *closure,
                                                        GValue       *return_value,
                                                        guint         n_param_values,
                                                        const GValue *param_values,
                                                        gpointer      invocation_hint,
                                                        gpointer      marshal_data);

/* VOID:OBJECT,ULONG (..\..\gst-libs\gst\interfaces\interfaces-marshal.list:4) */
extern void gst_interfaces_marshal_VOID__OBJECT_ULONG (GClosure     *closure,
                                                       GValue       *return_value,
                                                       guint         n_param_values,
                                                       const GValue *param_values,
                                                       gpointer      invocation_hint,
                                                       gpointer      marshal_data);

/* VOID:OBJECT,INT (..\..\gst-libs\gst\interfaces\interfaces-marshal.list:5) */
extern void gst_interfaces_marshal_VOID__OBJECT_INT (GClosure     *closure,
                                                     GValue       *return_value,
                                                     guint         n_param_values,
                                                     const GValue *param_values,
                                                     gpointer      invocation_hint,
                                                     gpointer      marshal_data);

G_END_DECLS

#endif /* __gst_interfaces_marshal_MARSHAL_H__ */

