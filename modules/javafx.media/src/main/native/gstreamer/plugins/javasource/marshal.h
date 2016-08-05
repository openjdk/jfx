
#ifndef __source_marshal_MARSHAL_H__
#define __source_marshal_MARSHAL_H__

#include    <glib-object.h>

G_BEGIN_DECLS

/* INT64:INT64 (marshal.in:2) */
extern void source_marshal_INT64__INT64 (GClosure     *closure,
                                         GValue       *return_value,
                                         guint         n_param_values,
                                         const GValue *param_values,
                                         gpointer      invocation_hint,
                                         gpointer      marshal_data);

/* INT:VOID (marshal.in:5) */
extern void source_marshal_INT__VOID (GClosure     *closure,
                                      GValue       *return_value,
                                      guint         n_param_values,
                                      const GValue *param_values,
                                      gpointer      invocation_hint,
                                      gpointer      marshal_data);

/* INT:UINT64,UINT (marshal.in:8) */
extern void source_marshal_INT__UINT64_UINT (GClosure     *closure,
                                             GValue       *return_value,
                                             guint         n_param_values,
                                             const GValue *param_values,
                                             gpointer      invocation_hint,
                                             gpointer      marshal_data);

/* VOID:POINTER,INT (marshal.in:11) */
extern void source_marshal_VOID__POINTER_INT (GClosure     *closure,
                                              GValue       *return_value,
                                              guint         n_param_values,
                                              const GValue *param_values,
                                              gpointer      invocation_hint,
                                              gpointer      marshal_data);

/* INT:INT,INT (marshal.in:14) */
extern void source_marshal_INT__INT_INT (GClosure     *closure,
                                         GValue       *return_value,
                                         guint         n_param_values,
                                         const GValue *param_values,
                                         gpointer      invocation_hint,
                                         gpointer      marshal_data);

G_END_DECLS

#endif /* __source_marshal_MARSHAL_H__ */

