/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __JAVA_SOURCE_H__
#define __JAVA_SOURCE_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define JAVA_SOURCE_PLUGIN_NAME "javasource"

#define JAVA_SOURCE_TYPE            (java_source_get_type())
#define JAVA_SOURCE(obj)            (G_TYPE_CHECK_INSTANCE_CAST((obj), JAVA_SOURCE_TYPE, JavaSource))
#define JAVA_SOURCE_CLASS(klass)    (G_TYPE_CHECK_CLASS_CAST((klass), JAVA_SOURCE_TYPE, JavaSourceClass))
#define IS_JAVA_SOURCE(obj)         (G_TYPE_CHECK_INSTANCE_TYPE((obj), JAVA_SOURCE_TYPE))
#define IS_JAVA_SOURCE_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE((klass), JAVA_SOURCE_TYPE))
#define JAVA_SOURCE_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), JAVA_SOURCE_TYPE, JavaSourceClass))

typedef struct _JavaSource      JavaSource;
typedef struct _JavaSourceClass JavaSourceClass;

GType java_source_get_type (void);

gboolean java_source_plugin_init (GstPlugin *plugin);

#define EOS_CODE         -1
#define OTHER_ERROR_CODE -2

G_END_DECLS

#endif // __JAVA_SOURCE_H__
