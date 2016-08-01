/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */

#ifndef DOMException_h
#define DOMException_h

enum DOMExceptionCode {
    INDEX_SIZE_ERR                = 1,
    DOMSTRING_SIZE_ERR            = 2,
    HIERARCHY_REQUEST_ERR         = 3,
    WRONG_DOCUMENT_ERR            = 4,
    INVALID_CHARACTER_ERR         = 5,
    NO_DATA_ALLOWED_ERR           = 6,
    NO_MODIFICATION_ALLOWED_ERR   = 7,
    NOT_FOUND_ERR                 = 8,
    NOT_SUPPORTED_ERR             = 9,
    INUSE_ATTRIBUTE_ERR           = 10,
    INVALID_STATE_ERR             = 11,
    SYNTAX_ERR                    = 12,
    INVALID_MODIFICATION_ERR      = 13,
    NAMESPACE_ERR                 = 14,
    INVALID_ACCESS_ERR            = 15,
    VALIDATION_ERR                = 16,
    TYPE_MISMATCH_ERR             = 17
};

#endif
