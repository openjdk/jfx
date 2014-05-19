/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class FXOMFxIdMerger {
    
    private final Set<String> existingFxIds = new HashSet<>();
    private final Map<String, String> renamings;
    
    public FXOMFxIdMerger(Collection<String> existingFxIds, Collection<String> importedFxIds) {
        assert existingFxIds != null;
        assert importedFxIds != null;
        
        this.existingFxIds.addAll(existingFxIds);
        this.renamings = makeRenamings(importedFxIds);
    }
    
    public String getRenamedFxId(String importedFxId) {
        return renamings.get(importedFxId);
    }
    
    
    /*
     * Private
     */
    
    private Map<String, String> makeRenamings(Collection<String> importedFxIds) {
        final Map<String, String> result = new HashMap<>();
        
        /*
         * Let's create three sets:
         * 
         * 1) currentFxIds     : fxIds defined in existingFxIds but not in importedFxIds
         * 2) newFxIds         : fxIds defined in importedFxIds but not in existingFxIds
         * 3) conflictingFxIds : fxIds defined in both importedFxIds and existingFxIds
         */
        
        final Set<String> currentFxIds = new HashSet<>();
        currentFxIds.addAll(existingFxIds);
        currentFxIds.removeAll(importedFxIds);
        
        final Set<String> newFxIds = new HashSet<>();
        newFxIds.addAll(importedFxIds);
        newFxIds.removeAll(existingFxIds);
        
        final Set<String> conflictingFxIds = new HashSet<>();
        conflictingFxIds.addAll(existingFxIds);
        conflictingFxIds.retainAll(importedFxIds);
        
        // No renaming for items in newFxIds
        for (String fxId : newFxIds) {
            result.put(fxId, fxId);
        }
        
        // For items in conflictingFxIds, we generate a new name.
        // This new name must not conflict with :
        //  - other fxIds from currentFxIds
        //  - other fxIds from newFxIds
        //  - other generated fxIds
        
        if (conflictingFxIds.isEmpty() == false) {
            final Set<String> nameSpace = new HashSet<>();
            nameSpace.addAll(currentFxIds);
            nameSpace.addAll(newFxIds);
            
            for (String fxId : conflictingFxIds) {
                final String renamedFxId = generateFxId(fxId, nameSpace);
                result.put(fxId, renamedFxId);
                nameSpace.add(renamedFxId);
            }
        }
        
        return result;
    }
    
    
    private String generateFxId(String conflictFxId, Set<String> nameSpace) {
        assert conflictFxId != null;
        assert nameSpace != null;
        assert nameSpace.contains(conflictFxId) == false;
        
        /*
         * We a numeric suffix to conflictFxId and checks that is not 
         * already in nameSpace. We increment the suffix and retry if there is
         * conflict.
         */
        
        int suffix = 1;
        final int conflictFxIdLength = conflictFxId.length();
        final StringBuilder sb = new StringBuilder();
        sb.append(conflictFxId);
        sb.append(suffix);
        while (nameSpace.contains(sb.toString())) {
            sb.delete(conflictFxIdLength, sb.length());
            sb.append(++suffix);
        }
        
        return sb.toString();
    }
}
