/*
 * Copyright (c) 2002, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.prism.impl;

import com.sun.javafx.tk.quantum.QuantumToolkit;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.PhantomReference;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 * This class is used for registering and disposing the native
 * data associated with Java objects.
 *
 * The object can register itself by calling one of the addRecord
 * methods and providing either the pointer to the native disposal
 * method or a descendant of the Disposer.Record interface with overridden
 * dispose() method.
 *
 * When the object becomes unreachable, the dispose() method
 * of the associated Disposer.Record object will be called.
 */
public class Disposer {

    private static Disposer disposerInstance;
    private static final int WEAK = 0;
    private static final int PHANTOM = 1;
    private static final int SOFT = 2;
    private static int refType = PHANTOM;

    static {
        String type = PrismSettings.refType;
        if (type != null) {
            if (type.equals("weak")) {
                refType = WEAK;
                if (PrismSettings.verbose) System.err.println("Using WEAK refs");
            } else if (type.equals("soft")) {
                refType = SOFT;
                if (PrismSettings.verbose) System.err.println("Using SOFT refs");
            } else {
                refType = PHANTOM;
                if (PrismSettings.verbose) System.err.println("Using PHANTOM refs");
            }
        }
        disposerInstance = new Disposer();
    }

    private final ReferenceQueue queue = new ReferenceQueue();
    private final Hashtable records = new Hashtable();
    private final LinkedList<Record> disposalQueue = new LinkedList<>();

    /**
     * Private constructor to prevent outside instantiation.
     */
    private Disposer() {
    }

//    /**
//     * Registers the object and the native data for later disposal.
//     * @param target Object to be registered
//     * @param disposeMethod pointer to the native disposal method
//     * @param pData pointer to the data to be passed to the
//     *              native disposal method
//     */
//    public static void addRecord(Object target,
//                                 long disposeMethod, long pData)
//    {
//        disposerInstance.add(target,
//                             new DefaultDisposerRecord(disposeMethod, pData));
//    }

    /**
     * Registers the target object and the native data for later disposal when
     * the target is unreachable.
     * .
     * @param target Object to be registered
     * @param rec the associated DisposerRecord object
     * @see DisposerRecord
     */
    public static void addRecord(Object target, Disposer.Record rec) {
        disposerInstance.add(target, rec);
    }

    /**
     * Add the object to the disposal queue. The object will be disposed
     * the next time cleanup is called.
     *
     * @param rec the DisposerRecord object to be disposed
     */
    public static void disposeRecord(Disposer.Record rec) {
        disposerInstance.addToDisposalQueue(rec);
    }

    /**
     * Disposes all unreachable objects and all objects in the disposal queue.
     * It first polls the reference queue, calling the dispose method of each
     * unreachable object. It then iterates the list of objects in the
     * disposal queue, calling the dispose method of each object.
     *
     * NOTE: This method must only be called from the Render Thread (the
     * thread on which  the resources were created).
     */
    public static void cleanUp() {
        if (!Thread.currentThread().getName().startsWith("QuantumRenderer")) {
            QuantumToolkit.runInRenderThreadAndWait(() -> cleanUp());
            return;
        }
        disposerInstance.disposeUnreachables();
        disposerInstance.processDisposalQueue();
    }

    /**
     * Performs the actual registration of the target object to be disposed.
     * @param target Object to be registered, or if target is an instance
     *               of DisposerTarget, its associated disposer referent
     *               will be the Object that is registered
     * @param rec the associated DisposerRecord object
     * @see DisposerRecord
     */
    synchronized private void add(Object target, Disposer.Record rec) {
        if (target instanceof Disposer.Target) {
            target = ((Disposer.Target)target).getDisposerReferent();
        }
        Reference ref;
        if (refType == PHANTOM) {
            ref = new PhantomReference(target, queue);
        } else if (refType == SOFT) {
            ref = new SoftReference(target, queue);
        } else {
            ref = new WeakReference(target, queue);
        }
        records.put(ref, rec);
    }

    synchronized private void addToDisposalQueue(Disposer.Record rec) {
        disposalQueue.add(rec);
    }

    /**
     * Polls the reference queue to see if there are any unreachable objects
     * to be disposed.  If there is work to be done, this method disposes all
     * unreachable objects in the queue, otherwise it returns immediately.
     */
    synchronized private void disposeUnreachables() {
        Object obj;
        while ((obj = queue.poll()) != null) {
            try {
                ((Reference)obj).clear();
                Disposer.Record rec = (Disposer.Record)records.remove(obj);
                rec.dispose();
                obj = null;
                rec = null;
            } catch (Exception e) {
                System.out.println("Exception while removing reference: " + e);
                e.printStackTrace();
            }
        }
    }

    synchronized private void processDisposalQueue() {
        // disposalQueue is always empty in the case of Windows using the d3d pipe.
        while (!disposalQueue.isEmpty()) {
            disposalQueue.remove().dispose();
        }
    }

    /**
     * This interface is used to hold the resource to be disposed.
     */
    public static interface Record {
        public void dispose();
    }

    /**
     * This is an interface which should be implemented by
     * the classes which use Disposer.
     */
    public static interface Target {
        /**
         * Returns an object which will be
         * used as the referent in the ReferenceQueue.
         */
        public Object getDisposerReferent();
    }
}
