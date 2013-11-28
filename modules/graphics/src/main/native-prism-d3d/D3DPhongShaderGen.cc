/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

#include "D3DPipeline.h"
#include "D3DPhongShader.h"

#define shader(name) ShaderFunction name() { static
#define endshader(trgt,ver) return ShaderFunction(g_##trgt##ver##_main); }

// See Makefile for shader names

shader(vsMtl1_Obj)
#include "hlsl/Mtl1VS_Obj.h"
endshader(vs, 20)

shader(psMtl1)
#include "hlsl/Mtl1PS.h"
endshader(ps, 20)

shader(psMtl1_i)
#include "hlsl/Mtl1PS_i.h"
endshader(ps, 20)

shader(psMtl1_s1n)
#include "hlsl/Mtl1PS_s1n.h"
endshader(ps, 20)

shader(psMtl1_s2n)
#include "hlsl/Mtl1PS_s2n.h"
endshader(ps, 20)

shader(psMtl1_s3n)
#include "hlsl/Mtl1PS_s3n.h"
endshader(ps, 20)

shader(psMtl1_s1t)
#include "hlsl/Mtl1PS_s1t.h"
endshader(ps, 20)

shader(psMtl1_s2t)
#include "hlsl/Mtl1PS_s2t.h"
endshader(ps, 20)

shader(psMtl1_s3t)
#include "hlsl/Mtl1PS_s3t.h"
endshader(ps, 20)

shader(psMtl1_s1c)
#include "hlsl/Mtl1PS_s1c.h"
endshader(ps, 20)

shader(psMtl1_s2c)
#include "hlsl/Mtl1PS_s2c.h"
endshader(ps, 20)

shader(psMtl1_s3c)
#include "hlsl/Mtl1PS_s3c.h"
endshader(ps, 20)

shader(psMtl1_s1m)
#include "hlsl/Mtl1PS_s1m.h"
endshader(ps, 20)

shader(psMtl1_s2m)
#include "hlsl/Mtl1PS_s2m.h"
endshader(ps, 20)

shader(psMtl1_s3m)
#include "hlsl/Mtl1PS_s3m.h"
endshader(ps, 20)

shader(psMtl1_b1n)
#include "hlsl/Mtl1PS_b1n.h"
endshader(ps, 20)

shader(psMtl1_b2n)
#include "hlsl/Mtl1PS_b2n.h"
endshader(ps, 20)

shader(psMtl1_b3n)
#include "hlsl/Mtl1PS_b3n.h"
endshader(ps, 20)

shader(psMtl1_b1t)
#include "hlsl/Mtl1PS_b1t.h"
endshader(ps, 20)

shader(psMtl1_b2t)
#include "hlsl/Mtl1PS_b2t.h"
endshader(ps, 20)

shader(psMtl1_b3t)
#include "hlsl/Mtl1PS_b3t.h"
endshader(ps, 20)

shader(psMtl1_b1c)
#include "hlsl/Mtl1PS_b1c.h"
endshader(ps, 20)

shader(psMtl1_b2c)
#include "hlsl/Mtl1PS_b2c.h"
endshader(ps, 20)

shader(psMtl1_b3c)
#include "hlsl/Mtl1PS_b3c.h"
endshader(ps, 20)

shader(psMtl1_b1m)
#include "hlsl/Mtl1PS_b1m.h"
endshader(ps, 20)

shader(psMtl1_b2m)
#include "hlsl/Mtl1PS_b2m.h"
endshader(ps, 20)

shader(psMtl1_b3m)
#include "hlsl/Mtl1PS_b3m.h"
endshader(ps, 20)

shader(psMtl1_s1ni)
#include "hlsl/Mtl1PS_s1ni.h"
endshader(ps, 20)

shader(psMtl1_s2ni)
#include "hlsl/Mtl1PS_s2ni.h"
endshader(ps, 20)

shader(psMtl1_s3ni)
#include "hlsl/Mtl1PS_s3ni.h"
endshader(ps, 20)

shader(psMtl1_s1ti)
#include "hlsl/Mtl1PS_s1ti.h"
endshader(ps, 20)

shader(psMtl1_s2ti)
#include "hlsl/Mtl1PS_s2ti.h"
endshader(ps, 20)

shader(psMtl1_s3ti)
#include "hlsl/Mtl1PS_s3ti.h"
endshader(ps, 20)

shader(psMtl1_s1ci)
#include "hlsl/Mtl1PS_s1ci.h"
endshader(ps, 20)

shader(psMtl1_s2ci)
#include "hlsl/Mtl1PS_s2ci.h"
endshader(ps, 20)

shader(psMtl1_s3ci)
#include "hlsl/Mtl1PS_s3ci.h"
endshader(ps, 20)

shader(psMtl1_s1mi)
#include "hlsl/Mtl1PS_s1mi.h"
endshader(ps, 20)

shader(psMtl1_s2mi)
#include "hlsl/Mtl1PS_s2mi.h"
endshader(ps, 20)

shader(psMtl1_s3mi)
#include "hlsl/Mtl1PS_s3mi.h"
endshader(ps, 20)

shader(psMtl1_b1ni)
#include "hlsl/Mtl1PS_b1ni.h"
endshader(ps, 20)

shader(psMtl1_b2ni)
#include "hlsl/Mtl1PS_b2ni.h"
endshader(ps, 20)

shader(psMtl1_b3ni)
#include "hlsl/Mtl1PS_b3ni.h"
endshader(ps, 20)

shader(psMtl1_b1ti)
#include "hlsl/Mtl1PS_b1ti.h"
endshader(ps, 20)

shader(psMtl1_b2ti)
#include "hlsl/Mtl1PS_b2ti.h"
endshader(ps, 20)

shader(psMtl1_b3ti)
#include "hlsl/Mtl1PS_b3ti.h"
endshader(ps, 20)

shader(psMtl1_b1ci)
#include "hlsl/Mtl1PS_b1ci.h"
endshader(ps, 20)

shader(psMtl1_b2ci)
#include "hlsl/Mtl1PS_b2ci.h"
endshader(ps, 20)

shader(psMtl1_b3ci)
#include "hlsl/Mtl1PS_b3ci.h"
endshader(ps, 20)

shader(psMtl1_b1mi)
#include "hlsl/Mtl1PS_b1mi.h"
endshader(ps, 20)

shader(psMtl1_b2mi)
#include "hlsl/Mtl1PS_b2mi.h"
endshader(ps, 20)

shader(psMtl1_b3mi)
#include "hlsl/Mtl1PS_b3mi.h"
endshader(ps, 20)
