
#ifndef ____gst_app_marshal_MARSHAL_H__
#define ____gst_app_marshal_MARSHAL_H__

#include    <glib-object.h>

G_BEGIN_DECLS

/* BOOLEAN:UINT64 (gstapp-marshal.list:1) */
extern void __gst_app_marshal_BOOLEAN__UINT64 (GClosure     *closure,
                                               GValue       *return_value,
                                               guint         n_param_values,
                                               const GValue *param_values,
                                               gpointer      invocation_hint,
                                               gpointer      marshal_data);

/* ENUM:BOXED (gstapp-marshal.list:2) */
extern void __gst_app_marshal_ENUM__BOXED (GClosure     *closure,
                                           GValue       *return_value,
                                           guint         n_param_values,
                                           const GValue *param_values,
                                           gpointer      invocation_hint,
                                           gpointer      marshal_data);

/* ENUM:VOID (gstapp-marshal.list:3) */
extern void __gst_app_marshal_ENUM__VOID (GClosure     *closure,
                                          GValue       *return_value,
                                          guint         n_param_values,
                                          const GValue *param_values,
                                          gpointer      invocation_hint,
                                          gpointer      marshal_data);

/* BOXED:VOID (gstapp-marshal.list:4) */
extern void __gst_app_marshal_BOXED__VOID (GClosure     *closure,
                                           GValue       *return_value,
                                           guint         n_param_values,
                                           const GValue *param_values,
                                           gpointer      invocation_hint,
                                           gpointer      marshal_data);

/* VOID:UINT (gstapp-marshal.list:5) */
#define __gst_app_marshal_VOID__UINT    g_cclosure_marshal_VOID__UINT

G_END_DECLS

#endif /* ____gst_app_marshal_MARSHAL_H__ */

