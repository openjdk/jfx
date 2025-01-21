/* garcbox.c: Atomically reference counted data
 *
 * Copyright 2018  Emmanuele Bassi
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#include "config.h"

#include "grcboxprivate.h"

#include "gmessages.h"
#include "grefcount.h"

#ifdef ENABLE_VALGRIND
#include "valgrind.h"
#endif

#include "glib_trace.h"

#include <string.h>

#define G_ARC_BOX(p)            (GArcBox *) (((char *) (p)) - G_ARC_BOX_SIZE)

/**
 * g_atomic_rc_box_alloc:
 * @block_size: the size of the allocation, must be greater than 0
 *
 * Allocates @block_size bytes of memory, and adds atomic
 * reference counting semantics to it.
 *
 * The data will be freed when its reference count drops to
 * zero.
 *
 * The allocated data is guaranteed to be suitably aligned for any
 * built-in type.
 *
 * Returns: (transfer full) (not nullable): a pointer to the allocated memory
 *
 * Since: 2.58
 */
gpointer
g_atomic_rc_box_alloc (gsize block_size)
{
  g_return_val_if_fail (block_size > 0, NULL);

  return g_rc_box_alloc_full (block_size, STRUCT_ALIGNMENT, TRUE, FALSE);
}

/**
 * g_atomic_rc_box_alloc0:
 * @block_size: the size of the allocation, must be greater than 0
 *
 * Allocates @block_size bytes of memory, and adds atomic
 * reference counting semantics to it.
 *
 * The contents of the returned data is set to zero.
 *
 * The data will be freed when its reference count drops to
 * zero.
 *
 * The allocated data is guaranteed to be suitably aligned for any
 * built-in type.
 *
 * Returns: (transfer full) (not nullable): a pointer to the allocated memory
 *
 * Since: 2.58
 */
gpointer
g_atomic_rc_box_alloc0 (gsize block_size)
{
  g_return_val_if_fail (block_size > 0, NULL);

  return g_rc_box_alloc_full (block_size, STRUCT_ALIGNMENT, TRUE, TRUE);
}

/**
 * g_atomic_rc_box_new:
 * @type: the type to allocate, typically a structure name
 *
 * A convenience macro to allocate atomically reference counted
 * data with the size of the given @type.
 *
 * This macro calls g_atomic_rc_box_alloc() with `sizeof (@type)` and
 * casts the returned pointer to a pointer of the given @type,
 * avoiding a type cast in the source code.
 *
 * Returns: (transfer full) (not nullable): a pointer to the allocated
 *   memory, cast to a pointer for the given @type
 *
 * Since: 2.58
 */

/**
 * g_atomic_rc_box_new0:
 * @type: the type to allocate, typically a structure name
 *
 * A convenience macro to allocate atomically reference counted
 * data with the size of the given @type, and set its contents
 * to zero.
 *
 * This macro calls g_atomic_rc_box_alloc0() with `sizeof (@type)` and
 * casts the returned pointer to a pointer of the given @type,
 * avoiding a type cast in the source code.
 *
 * Returns: (transfer full) (not nullable): a pointer to the allocated
 *   memory, cast to a pointer for the given @type
 *
 * Since: 2.58
 */

/**
 * g_atomic_rc_box_dup:
 * @block_size: the number of bytes to copy, must be greater than 0
 * @mem_block: (not nullable): the memory to copy
 *
 * Allocates a new block of data with atomic reference counting
 * semantics, and copies @block_size bytes of @mem_block
 * into it.
 *
 * Returns: (transfer full) (not nullable): a pointer to the allocated
 *   memory
 *
 * Since: 2.58
 */
gpointer
(g_atomic_rc_box_dup) (gsize         block_size,
                       gconstpointer mem_block)
{
  gpointer res;

  g_return_val_if_fail (block_size > 0, NULL);
  g_return_val_if_fail (mem_block != NULL, NULL);

  res = g_rc_box_alloc_full (block_size, STRUCT_ALIGNMENT, TRUE, FALSE);
  memcpy (res, mem_block, block_size);

  return res;
}

/**
 * g_atomic_rc_box_acquire:
 * @mem_block: (not nullable): a pointer to reference counted data
 *
 * Atomically acquires a reference on the data pointed by @mem_block.
 *
 * Returns: (transfer full) (not nullable): a pointer to the data,
 *   with its reference count increased
 *
 * Since: 2.58
 */
gpointer
(g_atomic_rc_box_acquire) (gpointer mem_block)
{
  GArcBox *real_box = G_ARC_BOX (mem_block);

  g_return_val_if_fail (mem_block != NULL, NULL);
#ifndef G_DISABLE_ASSERT
  g_return_val_if_fail (real_box->magic == G_BOX_MAGIC, NULL);
#endif

  g_atomic_ref_count_inc (&real_box->ref_count);

  TRACE (GLIB_RCBOX_ACQUIRE (mem_block, 1));

  return mem_block;
}

/**
 * g_atomic_rc_box_release:
 * @mem_block: (transfer full) (not nullable): a pointer to reference counted data
 *
 * Atomically releases a reference on the data pointed by @mem_block.
 *
 * If the reference was the last one, it will free the
 * resources allocated for @mem_block.
 *
 * Since: 2.58
 */
void
g_atomic_rc_box_release (gpointer mem_block)
{
  g_atomic_rc_box_release_full (mem_block, NULL);
}

/**
 * g_atomic_rc_box_release_full:
 * @mem_block: (transfer full) (not nullable): a pointer to reference counted data
 * @clear_func: (not nullable): a function to call when clearing the data
 *
 * Atomically releases a reference on the data pointed by @mem_block.
 *
 * If the reference was the last one, it will call @clear_func
 * to clear the contents of @mem_block, and then will free the
 * resources allocated for @mem_block.
 *
 * Note that implementing weak references via @clear_func is not thread-safe:
 * clearing a pointer to the memory from the callback can race with another
 * thread trying to access it as @mem_block already has a reference count of 0
 * when the callback is called and will be freed.
 *
 * Since: 2.58
 */
void
g_atomic_rc_box_release_full (gpointer       mem_block,
                              GDestroyNotify clear_func)
{
  GArcBox *real_box = G_ARC_BOX (mem_block);

  g_return_if_fail (mem_block != NULL);
#ifndef G_DISABLE_ASSERT
  g_return_if_fail (real_box->magic == G_BOX_MAGIC);
#endif

  if (g_atomic_ref_count_dec (&real_box->ref_count))
    {
      char *real_mem = (char *) real_box - real_box->private_offset;

      TRACE (GLIB_RCBOX_RELEASE (mem_block, 1));

      if (clear_func != NULL)
        clear_func (mem_block);

      TRACE (GLIB_RCBOX_FREE (mem_block));
      g_free (real_mem);
    }
}

/**
 * g_atomic_rc_box_get_size:
 * @mem_block: (not nullable): a pointer to reference counted data
 *
 * Retrieves the size of the reference counted data pointed by @mem_block.
 *
 * Returns: the size of the data, in bytes
 *
 * Since: 2.58
 */
gsize
g_atomic_rc_box_get_size (gpointer mem_block)
{
  GArcBox *real_box = G_ARC_BOX (mem_block);

  g_return_val_if_fail (mem_block != NULL, 0);
#ifndef G_DISABLE_ASSERT
  g_return_val_if_fail (real_box->magic == G_BOX_MAGIC, 0);
#endif

  return real_box->mem_size;
}
