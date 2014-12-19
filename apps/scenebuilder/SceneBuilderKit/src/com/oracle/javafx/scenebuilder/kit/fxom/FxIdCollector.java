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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
class FxIdCollector {
    
    private final Set<String> fxIds = new HashSet<>();
    private Map<String, Integer> nextIndexes ; // Created lazily
    
    public FxIdCollector(Set<String> fxIds) {
        assert fxIds != null;
        this.fxIds.addAll(fxIds);
    }
    
    public FxIdCollector(FXOMDocument fxomDocument) {
        this(fxomDocument.collectFxIds().keySet());
    }
    
    public String importFxId(String sourceFxId) {
        assert sourceFxId != null;
        
        final String result;
        if (fxIds.contains(sourceFxId)) {
            if (nextIndexes == null) {
                createNextIndexes();
                assert nextIndexes != null;
            }
            final PrefixSuffix pf = new PrefixSuffix(sourceFxId);
            final Integer nextIndex = nextIndexes.get(pf.getPrefix());
            assert nextIndex != null;
            result = pf.getPrefix() + nextIndex;
        } else {
            result = sourceFxId;
        }
        
        fxIds.add(result);
        if (nextIndexes != null) {
            updateNextIndexes(result);
        }
        
        return result;
    }
    
    
    /*
     * Private
     */
    
    private void createNextIndexes() {
        nextIndexes = new HashMap<>();
        
        for (String fxId : fxIds) {
            updateNextIndexes(fxId);
        }
    }
    
    
    private void updateNextIndexes(String fxId) {
        assert nextIndexes != null;
        
        final PrefixSuffix pf = new PrefixSuffix(fxId);
        final Integer nextIndex = nextIndexes.get(pf.getPrefix());
        if ((nextIndex == null) || (pf.getSuffix() >= nextIndex)) {
            nextIndexes.put(pf.getPrefix(), pf.getSuffix()+1);
        } 
    }
    
    
    
    private static class PrefixSuffix {
        private final String prefix;
        private final int suffix;
        
        public PrefixSuffix(String fxId) {
            assert fxId != null;
            assert fxId.isEmpty() == false;
            
            int endIndex = fxId.length();
            while ((endIndex >= 1) && Character.isDigit(fxId.charAt(endIndex-1))) {
                endIndex--;
            }
            if (endIndex < fxId.length()) {
                this.prefix = fxId.substring(0, endIndex);
                this.suffix = Integer.parseInt(fxId.substring(endIndex));
            } else {
                this.prefix = fxId;
                this.suffix = -1;
            }
        }

        public String getPrefix() {
            return prefix;
        }

        public int getSuffix() {
            return suffix;
        }
        
        @Override
        public String toString() {
            return (suffix == -1) ? prefix : prefix+suffix;
        }
    }
}
