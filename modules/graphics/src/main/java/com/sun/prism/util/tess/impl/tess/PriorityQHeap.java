/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
**
** Author: Eric Veach, July 1994
** Java Port: Pepijn Van Eeckhoudt, July 2003
** Java Port: Nathan Parker Burg, August 2003
*/
package com.sun.prism.util.tess.impl.tess;

class PriorityQHeap extends com.sun.prism.util.tess.impl.tess.PriorityQ {
    com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] nodes;
    com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] handles;
    int size, max;
    int freeList;
    boolean initialized;
    com.sun.prism.util.tess.impl.tess.PriorityQ.Leq leq;

/* really __gl_pqHeapNewPriorityQ */
    public PriorityQHeap(com.sun.prism.util.tess.impl.tess.PriorityQ.Leq leq) {
        size = 0;
        max = com.sun.prism.util.tess.impl.tess.PriorityQ.INIT_SIZE;
        nodes = new com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[com.sun.prism.util.tess.impl.tess.PriorityQ.INIT_SIZE + 1];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = new PQnode();
        }
        handles = new com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[com.sun.prism.util.tess.impl.tess.PriorityQ.INIT_SIZE + 1];
        for (int i = 0; i < handles.length; i++) {
            handles[i] = new PQhandleElem();
        }
        initialized = false;
        freeList = 0;
        this.leq = leq;

        nodes[1].handle = 1;    /* so that Minimum() returns NULL */
        handles[1].key = null;
    }

/* really __gl_pqHeapDeletePriorityQ */
    void pqDeletePriorityQ() {
        handles = null;
        nodes = null;
    }

    void FloatDown(int curr) {
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] n = nodes;
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] h = handles;
        int hCurr, hChild;
        int child;

        hCurr = n[curr].handle;
        for (; ;) {
            child = curr << 1;
            if (child < size && LEQ(leq, h[n[child + 1].handle].key,
                    h[n[child].handle].key)) {
                ++child;
            }

            assert (child <= max);

            hChild = n[child].handle;
            if (child > size || LEQ(leq, h[hCurr].key, h[hChild].key)) {
                n[curr].handle = hCurr;
                h[hCurr].node = curr;
                break;
            }
            n[curr].handle = hChild;
            h[hChild].node = curr;
            curr = child;
        }
    }


    void FloatUp(int curr) {
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] n = nodes;
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] h = handles;
        int hCurr, hParent;
        int parent;

        hCurr = n[curr].handle;
        for (; ;) {
            parent = curr >> 1;
            hParent = n[parent].handle;
            if (parent == 0 || LEQ(leq, h[hParent].key, h[hCurr].key)) {
                n[curr].handle = hCurr;
                h[hCurr].node = curr;
                break;
            }
            n[curr].handle = hParent;
            h[hParent].node = curr;
            curr = parent;
        }
    }

/* really __gl_pqHeapInit */
    boolean pqInit() {
        int i;

        /* This method of building a heap is O(n), rather than O(n lg n). */

        for (i = size; i >= 1; --i) {
            FloatDown(i);
        }
        initialized = true;

        return true;
    }

/* really __gl_pqHeapInsert */
/* returns LONG_MAX iff out of memory */
    int pqInsert(Object keyNew) {
        int curr;
        int free;

        curr = ++size;
        if ((curr * 2) > max) {
            com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] saveNodes = nodes;
            com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] saveHandles = handles;

            /* If the heap overflows, double its size. */
            max <<= 1;
//            pq->nodes = (PQnode *)memRealloc( pq->nodes, (size_t) ((pq->max + 1) * sizeof( pq->nodes[0] )));
            PriorityQ.PQnode[] pqNodes = new PriorityQ.PQnode[max + 1];
            System.arraycopy( nodes, 0, pqNodes, 0, nodes.length );
            for (int i = nodes.length; i < pqNodes.length; i++) {
                pqNodes[i] = new PQnode();
            }
            nodes = pqNodes;
            if (nodes == null) {
                nodes = saveNodes;      /* restore ptr to free upon return */
                return Integer.MAX_VALUE;
            }

//            pq->handles = (PQhandleElem *)memRealloc( pq->handles,(size_t)((pq->max + 1) * sizeof( pq->handles[0] )));
            PriorityQ.PQhandleElem[] pqHandles = new PriorityQ.PQhandleElem[max + 1];
            System.arraycopy( handles, 0, pqHandles, 0, handles.length );
            for (int i = handles.length; i < pqHandles.length; i++) {
                pqHandles[i] = new PQhandleElem();
            }
            handles = pqHandles;
            if (handles == null) {
                handles = saveHandles; /* restore ptr to free upon return */
                return Integer.MAX_VALUE;
            }
        }

        if (freeList == 0) {
            free = curr;
        } else {
            free = freeList;
            freeList = handles[free].node;
        }

        nodes[curr].handle = free;
        handles[free].node = curr;
        handles[free].key = keyNew;

        if (initialized) {
            FloatUp(curr);
        }
        assert (free != Integer.MAX_VALUE);
        return free;
    }

/* really __gl_pqHeapExtractMin */
    Object pqExtractMin() {
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] n = nodes;
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] h = handles;
        int hMin = n[1].handle;
        Object min = h[hMin].key;

        if (size > 0) {
            n[1].handle = n[size].handle;
            h[n[1].handle].node = 1;

            h[hMin].key = null;
            h[hMin].node = freeList;
            freeList = hMin;

            if (--size > 0) {
                FloatDown(1);
            }
        }
        return min;
    }

/* really __gl_pqHeapDelete */
    void pqDelete(int hCurr) {
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQnode[] n = nodes;
        com.sun.prism.util.tess.impl.tess.PriorityQ.PQhandleElem[] h = handles;
        int curr;

        assert (hCurr >= 1 && hCurr <= max && h[hCurr].key != null);

        curr = h[hCurr].node;
        n[curr].handle = n[size].handle;
        h[n[curr].handle].node = curr;

        if (curr <= --size) {
            if (curr <= 1 || LEQ(leq, h[n[curr >> 1].handle].key, h[n[curr].handle].key)) {
                FloatDown(curr);
            } else {
                FloatUp(curr);
            }
        }
        h[hCurr].key = null;
        h[hCurr].node = freeList;
        freeList = hCurr;
    }

    Object pqMinimum() {
        return handles[nodes[1].handle].key;
    }

    boolean pqIsEmpty() {
        return size == 0;
    }
}
