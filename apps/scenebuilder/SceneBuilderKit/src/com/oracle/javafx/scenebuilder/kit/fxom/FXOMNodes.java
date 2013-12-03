/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.javafx.scenebuilder.kit.fxom;

import com.oracle.javafx.scenebuilder.kit.metadata.util.PropertyName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class groups static utility methods which operate on FXOMNode and
 * subclasses (a bit like Collection and Collections).
 * 
 * 
 */
public class FXOMNodes {
    
    
    /**
     * Sorts the specified set of objects according their location in
     * the fxom document. Objets are sorted according depth first order.
     * In particular, if objects all have the same parent, then the resulting 
     * list will be sorted by indexes.
     * 
     * @param objects a set of fxom objects (never null but possibly empty)
     * @return the list of objets sorted by position in the fxom document
     */
    public static List<FXOMObject> sort(Set<FXOMObject> objects) {
        final List<FXOMObject> result;
        
        assert objects != null;
        
        if (objects.isEmpty()) {
            result = Collections.emptyList();
        } else if (objects.size() == 1) {
            result = Collections.singletonList(objects.iterator().next());
        } else {
            final FXOMObject object0 = objects.iterator().next();
            final FXOMDocument fxomDocument = object0.getFxomDocument();
            assert fxomDocument != null;
            result = new ArrayList<>();
            sort(fxomDocument.getFxomRoot(), objects, result);
        }
        
        return result;
    }
    
    
    /**
     * Flattens a set of fxom objects.
     * A set of fxom objects is declared "flat" if each object member 
     * of the set has no ancestor member of the set.
     * 
     * @param objects a set of fxom objects (never null)
     * @return the flat set of objects.
     */
    public static Set<FXOMObject> flatten(Set<FXOMObject> objects) {
        final Set<FXOMObject> result = new HashSet<>();
        
        assert objects != null;
        
        for (FXOMObject o : objects) {
            if (lookupAncestor(o, objects) == null) {
                result.add(o);
            }
        }
        
        return result;
    }
    
    
    /**
     * Returns null or the first ancestor of "obj" which belongs to "candidates".
     * @param obj an fxom object (never null)
     * @param candidates a set of fxom object (not null and not empty)
     * @return null or the first ancestor of "obj" which belongs to "candidates".
     */
    public static FXOMObject lookupAncestor(FXOMObject obj, Set<FXOMObject> candidates) {
        assert obj != null;
        assert candidates != null;
        assert candidates.isEmpty() == false;
        
        FXOMObject result = obj.getParentObject();
        while ((result != null) && (candidates.contains(result) == false)) {
            result = result.getParentObject();
        }
        
        return result;
    }
    
    
    
    /*
     * Private
     */
    
    
    private static void sort(FXOMObject from, 
            Set<FXOMObject> objects, List<FXOMObject> result) {
        
        if (objects.contains(from)) {
            result.add(from);
        }
        
        if (from instanceof FXOMCollection) {
            final FXOMCollection collection = (FXOMCollection) from;
            for (FXOMObject item : collection.getItems()) {
                sort(item, objects, result);
            }
        } else if (from instanceof FXOMInstance) {
            final FXOMInstance instance = (FXOMInstance) from;
            final List<PropertyName> propertyNames 
                    = new ArrayList<>(instance.getProperties().keySet());
            Collections.sort(propertyNames);
            for (PropertyName name : propertyNames) {
                final FXOMProperty property = instance.getProperties().get(name);
                assert property != null;
                if (property instanceof FXOMPropertyC) {
                    final FXOMPropertyC propertyC = (FXOMPropertyC) property;
                    for (FXOMObject v : propertyC.getValues()) {
                        sort(v, objects, result);
                    }
                }
            }
        } else {
            assert from instanceof FXOMIntrinsic
                    : "Unexpected FXOMObject subclass " + from.getClass();
        }
    }

    
    public static FXOMNode newNode(FXOMNode source) {
        final FXOMNode result;
        
        if (source instanceof FXOMObject) {
            result = newObject((FXOMObject) source);
        } else if (source instanceof FXOMProperty) {
            result = newProperty((FXOMProperty) source);
        } else {
            throw new IllegalArgumentException(FXOMNodes.class.getSimpleName()
                    + " needs some additional implementation"); //NOI18N
        }
        
        return result;
    }
    
    public static FXOMObject newObject(FXOMObject source) {
        final FXOMObject result;
        
        if (source instanceof FXOMCollection) {
            result = FXOMCollection.newInstance((FXOMCollection) source);
        } else if (source instanceof FXOMInstance) {
            result = FXOMInstance.newInstance((FXOMInstance) source);
        } else if (source instanceof FXOMIntrinsic) {
            result = FXOMIntrinsic.newInstance((FXOMIntrinsic) source);
        } else {
            throw new IllegalArgumentException(FXOMNodes.class.getSimpleName()
                    + " needs some additional implementation"); //NOI18N
        }
        
        return result;
    }
    
    public static FXOMProperty newProperty(FXOMProperty source) {
        final FXOMProperty result;
        
        if (source instanceof FXOMPropertyC) {
            result = FXOMPropertyC.newInstance((FXOMPropertyC) source);
        } else if (source instanceof FXOMPropertyT) {
            result = FXOMPropertyT.newInstance((FXOMPropertyT) source);
        }else {
            throw new IllegalArgumentException(FXOMNodes.class.getSimpleName()
                    + " needs some additional implementation");
        }
        
        return result;
    }
    
    public static FXOMDocument newDocument(FXOMObject source) {
        final FXOMObject copy = FXOMNodes.newObject(source);
        
        if (copy instanceof FXOMInstance) {
            trimStaticProperties((FXOMInstance) copy);
        }
        
        final FXOMDocument result = new FXOMDocument();
        result.beginUpdate();
        result.setLocation(source.getFxomDocument().getLocation());
        copy.moveToFxomDocument(result);
        result.setFxomRoot(copy);
        result.endUpdate();
        
        return result;
    }
    
    
    
    /*
     * Private
     */
    
    private static void trimStaticProperties(FXOMInstance fxomInstance) {
        final List<FXOMProperty> properties = 
                new ArrayList<>(fxomInstance.getProperties().values());
        for (FXOMProperty p : properties) {
            if (p.getName().getResidenceClass() != null) {
                // This is a static property : we remove it.
                p.removeFromParentInstance();
            }
        }
    }
}
