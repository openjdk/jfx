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

shader(psMtl1_s1a)
#include "hlsl/Mtl1PS_s1a.h"
endshader(ps, 20)

shader(psMtl1_s2a)
#include "hlsl/Mtl1PS_s2a.h"
endshader(ps, 20)

shader(psMtl1_s3a)
#include "hlsl/Mtl1PS_s3a.h"
endshader(ps, 20)

shader(psMtl1_s1s)
#include "hlsl/Mtl1PS_s1s.h"
endshader(ps, 20)

shader(psMtl1_s2s)
#include "hlsl/Mtl1PS_s2s.h"
endshader(ps, 20)

shader(psMtl1_s3s)
#include "hlsl/Mtl1PS_s3s.h"
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

shader(psMtl1_b1a)
#include "hlsl/Mtl1PS_b1a.h"
endshader(ps, 20)

shader(psMtl1_b2a)
#include "hlsl/Mtl1PS_b2a.h"
endshader(ps, 20)

shader(psMtl1_b3a)
#include "hlsl/Mtl1PS_b3a.h"
endshader(ps, 20)

shader(psMtl1_b1s)
#include "hlsl/Mtl1PS_b1s.h"
endshader(ps, 20)

shader(psMtl1_b2s)
#include "hlsl/Mtl1PS_b2s.h"
endshader(ps, 20)

shader(psMtl1_b3s)
#include "hlsl/Mtl1PS_b3s.h"
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

shader(psMtl1_s1ai)
#include "hlsl/Mtl1PS_s1ai.h"
endshader(ps, 20)

shader(psMtl1_s2ai)
#include "hlsl/Mtl1PS_s2ai.h"
endshader(ps, 20)

shader(psMtl1_s3ai)
#include "hlsl/Mtl1PS_s3ai.h"
endshader(ps, 20)

shader(psMtl1_s1si)
#include "hlsl/Mtl1PS_s1si.h"
endshader(ps, 20)

shader(psMtl1_s2si)
#include "hlsl/Mtl1PS_s2si.h"
endshader(ps, 20)

shader(psMtl1_s3si)
#include "hlsl/Mtl1PS_s3si.h"
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

shader(psMtl1_b1ai)
#include "hlsl/Mtl1PS_b1ai.h"
endshader(ps, 20)

shader(psMtl1_b2ai)
#include "hlsl/Mtl1PS_b2ai.h"
endshader(ps, 20)

shader(psMtl1_b3ai)
#include "hlsl/Mtl1PS_b3ai.h"
endshader(ps, 20)

shader(psMtl1_b1si)
#include "hlsl/Mtl1PS_b1si.h"
endshader(ps, 20)

shader(psMtl1_b2si)
#include "hlsl/Mtl1PS_b2si.h"
endshader(ps, 20)

shader(psMtl1_b3si)
#include "hlsl/Mtl1PS_b3si.h"
endshader(ps, 20)
