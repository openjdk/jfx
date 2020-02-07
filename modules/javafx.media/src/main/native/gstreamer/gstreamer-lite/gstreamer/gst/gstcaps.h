/* GStreamer
 * Copyright (C) 2003 David A. Schleef <ds@schleef.org>
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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#ifndef __GST_CAPS_H__
#define __GST_CAPS_H__

#include <gst/gstconfig.h>
#include <gst/gstminiobject.h>
#include <gst/gststructure.h>
#include <gst/gstcapsfeatures.h>
#include <gst/glib-compat.h>

G_BEGIN_DECLS

#ifndef GSTREAMER_LITE
GST_API GType _gst_caps_type;
#else // GSTREAMER_LITE
GST_EXPORT GType _gst_caps_type;
#endif // GSTREAMER_LITE

#define GST_TYPE_CAPS             (_gst_caps_type)
#define GST_IS_CAPS(obj)          (GST_IS_MINI_OBJECT_TYPE((obj), GST_TYPE_CAPS))
#define GST_CAPS_CAST(obj)        ((GstCaps*)(obj))
#define GST_CAPS(obj)             (GST_CAPS_CAST(obj))

#ifndef GSTREAMER_LITE
#define GST_TYPE_STATIC_CAPS      (gst_static_caps_get_type())
#endif // GSTREAMER_LITE

/**
 * GstCapsFlags:
 * @GST_CAPS_FLAG_ANY: Caps has no specific content, but can contain
 *    anything.
 *
 * Extra flags for a caps.
 */
typedef enum {
  GST_CAPS_FLAG_ANY = (GST_MINI_OBJECT_FLAG_LAST << 0)
} GstCapsFlags;

/**
 * GstCapsIntersectMode:
 * @GST_CAPS_INTERSECT_ZIG_ZAG  : Zig-zags over both caps.
 * @GST_CAPS_INTERSECT_FIRST    : Keeps the first caps order.
 *
 * Modes of caps intersection
 *
 * @GST_CAPS_INTERSECT_ZIG_ZAG tries to preserve overall order of both caps
 * by iterating on the caps' structures as the following matrix shows:
 * |[
 *          caps1
 *       +-------------
 *       | 1  2  4  7
 * caps2 | 3  5  8 10
 *       | 6  9 11 12
 * ]|
 * Used when there is no explicit precedence of one caps over the other. e.g.
 * tee's sink pad getcaps function, it will probe its src pad peers' for their
 * caps and intersect them with this mode.
 *
 * @GST_CAPS_INTERSECT_FIRST is useful when an element wants to preserve
 * another element's caps priority order when intersecting with its own caps.
 * Example: If caps1 is [A, B, C] and caps2 is [E, B, D, A], the result
 * would be [A, B], maintaining the first caps priority on the intersection.
 */
typedef enum {
  GST_CAPS_INTERSECT_ZIG_ZAG            =  0,
  GST_CAPS_INTERSECT_FIRST              =  1
} GstCapsIntersectMode;

/**
 * GST_CAPS_ANY:
 *
 * Means that the element/pad can output 'anything'. Useful for elements
 * that output unknown media, such as filesrc. This macro returns a singleton and
 * should not be unreffed.
 */
#define GST_CAPS_ANY              _gst_caps_any
/**
 * GST_CAPS_NONE:
 *
 * The opposite of %GST_CAPS_ANY: it means that the pad/element outputs an
 * undefined media type that can not be detected. This macro returns a singleton
 * and should not be unreffed.
 */
#define GST_CAPS_NONE             _gst_caps_none

/**
 * GST_STATIC_CAPS_ANY:
 *
 * Creates a new #GstCaps static caps that matches anything.
 * This can be used in pad templates.
 */
#define GST_STATIC_CAPS_ANY       GST_STATIC_CAPS("ANY")
/**
 * GST_STATIC_CAPS_NONE:
 *
 * Creates a new #GstCaps static caps that matches nothing.
 * This can be used in pad templates.
 */
#define GST_STATIC_CAPS_NONE      GST_STATIC_CAPS("NONE")

/**
 * GST_CAPS_IS_SIMPLE:
 * @caps: the #GstCaps instance to check
 *
 * Convenience macro that checks if the number of structures in the given caps
 * is exactly one.
 */
#define GST_CAPS_IS_SIMPLE(caps) (gst_caps_get_size(caps) == 1)

/**
 * GST_STATIC_CAPS:
 * @string: the string describing the caps
 *
 * Creates a new #GstCaps static caps from an input string.
 * This can be used in pad templates.
 */
#define GST_STATIC_CAPS(string) \
{ \
  /* caps */ NULL, \
  /* string */ string, \
  GST_PADDING_INIT \
}

typedef struct _GstCaps GstCaps;
typedef struct _GstStaticCaps GstStaticCaps;

#ifndef GSTREAMER_LITE
GST_API GstCaps * _gst_caps_any;
#else // GSTREAMER_LITE
GST_EXPORT GstCaps * _gst_caps_any;
#endif // GSTREAMER_LITE

#ifndef GSTREAMER_LITE
GST_API GstCaps * _gst_caps_none;
#else // GSTREAMER_LITE
GST_EXPORT GstCaps * _gst_caps_none;
#endif // GSTREAMER_LITE

/**
 * GST_CAPS_FLAGS:
 * @caps: a #GstCaps.
 *
 * A flags word containing #GstCapsFlags flags set on this caps.
 */
#define GST_CAPS_FLAGS(caps)                    GST_MINI_OBJECT_FLAGS(caps)

/* refcount */
/**
 * GST_CAPS_REFCOUNT:
 * @caps: a #GstCaps
 *
 * Get access to the reference count field of the caps
 */
#define GST_CAPS_REFCOUNT(caps)                 GST_MINI_OBJECT_REFCOUNT(caps)
/**
 * GST_CAPS_REFCOUNT_VALUE:
 * @caps: a #GstCaps
 *
 * Get the reference count value of the caps.
 */
#define GST_CAPS_REFCOUNT_VALUE(caps)           GST_MINI_OBJECT_REFCOUNT_VALUE(caps)

/**
 * GST_CAPS_FLAG_IS_SET:
 * @caps: a #GstCaps.
 * @flag: the #GstCapsFlags to check.
 *
 * Gives the status of a specific flag on a caps.
 */
#define GST_CAPS_FLAG_IS_SET(caps,flag)        GST_MINI_OBJECT_FLAG_IS_SET (caps, flag)
/**
 * GST_CAPS_FLAG_SET:
 * @caps: a #GstCaps.
 * @flag: the #GstCapsFlags to set.
 *
 * Sets a caps flag on a caps.
 */
#define GST_CAPS_FLAG_SET(caps,flag)           GST_MINI_OBJECT_FLAG_SET (caps, flag)
/**
 * GST_CAPS_FLAG_UNSET:
 * @caps: a #GstCaps.
 * @flag: the #GstCapsFlags to clear.
 *
 * Clears a caps flag.
 */
#define GST_CAPS_FLAG_UNSET(caps,flag)         GST_MINI_OBJECT_FLAG_UNSET (caps, flag)

/* refcounting */
/**
 * gst_caps_ref:
 * @caps: the #GstCaps to reference
 *
 * Add a reference to a #GstCaps object.
 *
 * From this point on, until the caller calls gst_caps_unref() or
 * gst_caps_make_writable(), it is guaranteed that the caps object will not
 * change. This means its structures won't change, etc. To use a #GstCaps
 * object, you must always have a refcount on it -- either the one made
 * implicitly by e.g. gst_caps_new_simple(), or via taking one explicitly with
 * this function.
 *
 * Returns: the same #GstCaps object.
 */
static inline GstCaps *
gst_caps_ref (GstCaps * caps)
{
  return (GstCaps *) gst_mini_object_ref (GST_MINI_OBJECT_CAST (caps));
}

/**
 * gst_caps_unref:
 * @caps: a #GstCaps.
 *
 * Unref a #GstCaps and and free all its structures and the
 * structures' values when the refcount reaches 0.
 */
static inline void
gst_caps_unref (GstCaps * caps)
{
  gst_mini_object_unref (GST_MINI_OBJECT_CAST (caps));
}

/**
 * gst_clear_caps: (skip)
 * @caps_ptr: a pointer to a #GstCaps reference
 *
 * Clears a reference to a #GstCaps.
 *
 * @caps_ptr must not be %NULL.
 *
 * If the reference is %NULL then this function does nothing. Otherwise, the
 * reference count of the caps is decreased and the pointer is set to %NULL.
 *
 * Since: 1.16
 */
static inline void
gst_clear_caps (GstCaps ** caps_ptr)
{
  gst_clear_mini_object ((GstMiniObject **) caps_ptr);
}

/* copy caps */
GST_API
GstCaps * gst_caps_copy (const GstCaps * caps);

#define gst_caps_copy(caps) GST_CAPS (gst_mini_object_copy (GST_MINI_OBJECT_CAST (caps)))

/**
 * gst_caps_is_writable:
 * @caps: a #GstCaps
 *
 * Tests if you can safely modify @caps. It is only safe to modify caps when
 * there is only one owner of the caps - ie, the object is writable.
 */
#define         gst_caps_is_writable(caps)     gst_mini_object_is_writable (GST_MINI_OBJECT_CAST (caps))

/**
 * gst_caps_make_writable:
 * @caps: (transfer full): a #GstCaps
 *
 * Returns a writable copy of @caps.
 *
 * If there is only one reference count on @caps, the caller must be the owner,
 * and so this function will return the caps object unchanged. If on the other
 * hand there is more than one reference on the object, a new caps object will
 * be returned. The caller's reference on @caps will be removed, and instead the
 * caller will own a reference to the returned object.
 *
 * In short, this function unrefs the caps in the argument and refs the caps
 * that it returns. Don't access the argument after calling this function. See
 * also: gst_caps_ref().
 *
 * Returns: (transfer full): a writable caps which may or may not be the
 *     same as @caps
 */
#define         gst_caps_make_writable(caps)   GST_CAPS_CAST (gst_mini_object_make_writable (GST_MINI_OBJECT_CAST (caps)))

/**
 * gst_caps_replace:
 * @old_caps: (inout) (transfer full) (nullable): pointer to a pointer
 *     to a #GstCaps to be replaced.
 * @new_caps: (transfer none) (allow-none): pointer to a #GstCaps that will
 *     replace the caps pointed to by @old_caps.
 *
 * Modifies a pointer to a #GstCaps to point to a different #GstCaps. The
 * modification is done atomically (so this is useful for ensuring thread safety
 * in some cases), and the reference counts are updated appropriately (the old
 * caps is unreffed, the new is reffed).
 *
 * Either @new_caps or the #GstCaps pointed to by @old_caps may be %NULL.
 *
 * Returns: %TRUE if @new_caps was different from @old_caps
 */
static inline gboolean
gst_caps_replace (GstCaps **old_caps, GstCaps *new_caps)
{
    return gst_mini_object_replace ((GstMiniObject **) old_caps, (GstMiniObject *) new_caps);
}

/**
 * gst_caps_take:
 * @old_caps: (inout) (transfer full): pointer to a pointer to a #GstCaps to be
 *     replaced.
 * @new_caps: (transfer full) (allow-none): pointer to a #GstCaps that will
 *     replace the caps pointed to by @old_caps.
 *
 * Modifies a pointer to a #GstCaps to point to a different #GstCaps. This
 * function is similar to gst_caps_replace() except that it takes ownership
 * of @new_caps.
 *
 * Returns: %TRUE if @new_caps was different from @old_caps
 */
static inline gboolean
gst_caps_take (GstCaps **old_caps, GstCaps *new_caps)
{
    return gst_mini_object_take ((GstMiniObject **) old_caps, (GstMiniObject *) new_caps);
}

/**
 * GstCaps:
 * @mini_object: the parent type
 *
 * Object describing media types.
 */
struct _GstCaps {
  GstMiniObject mini_object;
};

/**
 * GstStaticCaps:
 * @caps: the cached #GstCaps
 * @string: a string describing a caps
 *
 * Datastructure to initialize #GstCaps from a string description usually
 * used in conjunction with GST_STATIC_CAPS() and gst_static_caps_get() to
 * instantiate a #GstCaps.
 */
struct _GstStaticCaps {
  /*< public >*/
  GstCaps *caps;
  const char *string;

  /*< private >*/
  gpointer _gst_reserved[GST_PADDING];
};

/**
 * GstCapsForeachFunc:
 * @features: the #GstCapsFeatures
 * @structure: the #GstStructure
 * @user_data: user data
 *
 * A function that will be called in gst_caps_foreach(). The function may
 * not modify @features or @structure.
 *
 * Returns: %TRUE if the foreach operation should continue, %FALSE if
 * the foreach operation should stop with %FALSE.
 *
 * Since: 1.6
 */
typedef gboolean (*GstCapsForeachFunc) (GstCapsFeatures *features,
                                        GstStructure    *structure,
                                        gpointer         user_data);

/**
 * GstCapsMapFunc:
 * @features: the #GstCapsFeatures
 * @structure: the #GstStructure
 * @user_data: user data
 *
 * A function that will be called in gst_caps_map_in_place(). The function
 * may modify @features and @structure.
 *
 * Returns: %TRUE if the map operation should continue, %FALSE if
 * the map operation should stop with %FALSE.
 */
typedef gboolean (*GstCapsMapFunc)     (GstCapsFeatures *features,
                                        GstStructure    *structure,
                                        gpointer         user_data);

/**
 * GstCapsFilterMapFunc:
 * @features: the #GstCapsFeatures
 * @structure: the #GstStructure
 * @user_data: user data
 *
 * A function that will be called in gst_caps_filter_and_map_in_place().
 * The function may modify @features and @structure, and both will be
 * removed from the caps if %FALSE is returned.
 *
 * Returns: %TRUE if the features and structure should be preserved,
 * %FALSE if it should be removed.
 */
typedef gboolean (*GstCapsFilterMapFunc) (GstCapsFeatures *features,
                                          GstStructure    *structure,
                                          gpointer user_data);


GST_API
GType             gst_caps_get_type                (void);

GST_API
GstCaps *         gst_caps_new_empty               (void);

GST_API
GstCaps *         gst_caps_new_any                 (void);

GST_API
GstCaps *         gst_caps_new_empty_simple        (const char    *media_type) G_GNUC_WARN_UNUSED_RESULT;

GST_API
GstCaps *         gst_caps_new_simple              (const char    *media_type,
                                                    const char    *fieldname,
                                                    ...) G_GNUC_NULL_TERMINATED G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_new_full                (GstStructure  *struct1,
                                                    ...) G_GNUC_NULL_TERMINATED G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_new_full_valist         (GstStructure  *structure,
                                                    va_list        var_args) G_GNUC_WARN_UNUSED_RESULT;
#ifndef GSTREAMER_LITE
GST_API
GType             gst_static_caps_get_type         (void);
#endif // GSTREAMER_LITE
GstCaps *         gst_static_caps_get              (GstStaticCaps *static_caps);

GST_API
void              gst_static_caps_cleanup          (GstStaticCaps *static_caps);

/* manipulation */

GST_API
void              gst_caps_append                  (GstCaps       *caps1,
                                                    GstCaps       *caps2);
GST_API
void              gst_caps_append_structure        (GstCaps       *caps,
                                                    GstStructure  *structure);
GST_API
void              gst_caps_append_structure_full   (GstCaps       *caps,
                                                    GstStructure  *structure,
                                                    GstCapsFeatures *features);
GST_API
void              gst_caps_remove_structure        (GstCaps       *caps, guint idx);

GST_API
GstCaps *         gst_caps_merge                   (GstCaps       *caps1,
                                                    GstCaps       *caps2) G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_merge_structure         (GstCaps       *caps,
                                                    GstStructure  *structure) G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_merge_structure_full    (GstCaps       *caps,
                                                    GstStructure  *structure,
                                                    GstCapsFeatures *features) G_GNUC_WARN_UNUSED_RESULT;

GST_API
guint             gst_caps_get_size                (const GstCaps *caps);

GST_API
GstStructure *    gst_caps_get_structure           (const GstCaps *caps,
                                                    guint          index);
GST_API
GstStructure *    gst_caps_steal_structure         (GstCaps       *caps,
                                                    guint          index) G_GNUC_WARN_UNUSED_RESULT;
GST_API
void              gst_caps_set_features            (GstCaps *caps,
                                                    guint          index,
                                                    GstCapsFeatures * features);
GST_API
void              gst_caps_set_features_simple        (GstCaps *caps,
                                                       GstCapsFeatures * features);

GST_API
GstCapsFeatures * gst_caps_get_features            (const GstCaps *caps,
                                                    guint          index);
GST_API
GstCaps *         gst_caps_copy_nth                (const GstCaps *caps, guint nth) G_GNUC_WARN_UNUSED_RESULT;

GST_API
GstCaps *         gst_caps_truncate                (GstCaps       *caps) G_GNUC_WARN_UNUSED_RESULT;

GST_API
void              gst_caps_set_value               (GstCaps       *caps,
                                                    const char    *field,
                                                    const GValue  *value);
GST_API
void              gst_caps_set_simple              (GstCaps       *caps,
                                                    const char    *field, ...) G_GNUC_NULL_TERMINATED;
GST_API
void              gst_caps_set_simple_valist       (GstCaps       *caps,
                                                    const char    *field,
                                                    va_list        varargs);
GST_API
gboolean          gst_caps_foreach                 (const GstCaps       *caps,
                                                    GstCapsForeachFunc   func,
                                                    gpointer             user_data);
GST_API
gboolean          gst_caps_map_in_place            (GstCaps        *caps,
                                                    GstCapsMapFunc  func,
                                                    gpointer        user_data);
GST_API
void              gst_caps_filter_and_map_in_place (GstCaps              *caps,
                                                    GstCapsFilterMapFunc  func,
                                                    gpointer              user_data);

/* tests */

GST_API
gboolean          gst_caps_is_any                  (const GstCaps *caps);

GST_API
gboolean          gst_caps_is_empty                (const GstCaps *caps);

GST_API
gboolean          gst_caps_is_fixed                (const GstCaps *caps);

GST_API
gboolean          gst_caps_is_always_compatible    (const GstCaps *caps1,
                                                    const GstCaps *caps2);
GST_API
gboolean          gst_caps_is_subset       (const GstCaps *subset,
                const GstCaps *superset);
GST_API
gboolean          gst_caps_is_subset_structure     (const GstCaps *caps,
                                                    const GstStructure *structure);
GST_API
gboolean          gst_caps_is_subset_structure_full (const GstCaps *caps,
                                                     const GstStructure *structure,
                                                     const GstCapsFeatures *features);
GST_API
gboolean          gst_caps_is_equal      (const GstCaps *caps1,
                const GstCaps *caps2);
GST_API
gboolean          gst_caps_is_equal_fixed          (const GstCaps *caps1,
                const GstCaps *caps2);
GST_API
gboolean          gst_caps_can_intersect           (const GstCaps * caps1,
                const GstCaps * caps2);
GST_API
gboolean          gst_caps_is_strictly_equal     (const GstCaps *caps1,
                const GstCaps *caps2);


/* operations */

GST_API
GstCaps *         gst_caps_intersect               (GstCaps *caps1,
                GstCaps *caps2) G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_intersect_full          (GstCaps *caps1,
                GstCaps *caps2,
                                                    GstCapsIntersectMode mode) G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_subtract      (GstCaps *minuend,
                GstCaps *subtrahend) G_GNUC_WARN_UNUSED_RESULT;
GST_API
GstCaps *         gst_caps_normalize               (GstCaps *caps) G_GNUC_WARN_UNUSED_RESULT;

GST_API
GstCaps *         gst_caps_simplify                (GstCaps *caps) G_GNUC_WARN_UNUSED_RESULT;

GST_API
GstCaps *         gst_caps_fixate                  (GstCaps *caps) G_GNUC_WARN_UNUSED_RESULT;

/* utility */

GST_API
gchar *           gst_caps_to_string               (const GstCaps *caps) G_GNUC_MALLOC;

GST_API
GstCaps *         gst_caps_from_string             (const gchar   *string) G_GNUC_WARN_UNUSED_RESULT;

#ifdef G_DEFINE_AUTOPTR_CLEANUP_FUNC
G_DEFINE_AUTOPTR_CLEANUP_FUNC(GstCaps, gst_caps_unref)
#endif

G_END_DECLS

#endif /* __GST_CAPS_H__ */
