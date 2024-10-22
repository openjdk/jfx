/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
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

package com.oracle.demo.richtext.rta;

import java.util.Random;
import jfx.incubator.scene.control.richtext.model.SimpleViewOnlyStyledModel;

/**
 * Large text model for debugging.
 *
 * @author Andy Goryachev
 */
public class LargeTextModel extends SimpleViewOnlyStyledModel {
    private final String STYLE = "-fx-font-size:500%";
    private final Random random = new Random();

    public LargeTextModel(int lineCount) {
        for (int i = 0; i < lineCount; i++) {
            addLine(i);
        }
    }

    private void addLine(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("L").append(n).append(' ');
        int ct;
        if (random.nextFloat() < 0.01f) {
            ct = 200;
        } else {
            ct = random.nextInt(10);
        }

        for (int i = 0; i < ct; i++) {
            sb.append(" ").append(i);
            int len = random.nextInt(10) + 1;
            for (int j = 0; j < len; j++) {
                sb.append('*');
            }
        }
        addWithStyleNames(sb.toString(), STYLE);
        nl();
    }
}
