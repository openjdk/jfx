/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.text;

import java.text.Normalizer;

final class TextNormalizer {

    // Text normalization forms
    private static final int FORM_NFC = 0;
    private static final int FORM_NFD = 1;
    private static final int FORM_NFKC = 2;
    private static final int FORM_NFKD = 3;

    private static String normalize(String data, int type) {
        Normalizer.Form form;
        switch (type) {
        case FORM_NFC:  form = Normalizer.Form.NFC; break;
        case FORM_NFD:  form = Normalizer.Form.NFD; break;
        case FORM_NFKC: form = Normalizer.Form.NFKC; break;
        case FORM_NFKD: form = Normalizer.Form.NFKD; break;
        default:
            throw new IllegalArgumentException("invalid type: " + type);
        }
            
        return Normalizer.normalize(data, form);
    }
}
