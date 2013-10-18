/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

#import "MediaUtils.h"

#include "debug.h"


#define JAR_PREFIX          @"jar:"
#define JAR_PATH_DELIMITER  @"!"
#define JAR_DIR_PREFIX      @"_"
#define JAR_DIR_SUFFIX      @"_Resources"

#define HTTP_PREFIX         @"http://"
#define IPOD_LIB_PREFIX     @"ipod-library://"

#define PATH_DELIMITER      '/'


@implementation MediaUtils


+ (unsigned int) lastIndexOf: (char) searchChar
                    inString: (NSString *) string {

    NSRange searchRange;
    searchRange.location = (unsigned int) searchChar;
    searchRange.length = 1;

    NSRange foundRange = [string rangeOfCharacterFromSet:
                          [NSCharacterSet characterSetWithRange: searchRange]
                                                 options: NSBackwardsSearch];

    return foundRange.location;
}

+ (NSString *) bundleUrlFromJarUrl: (NSString *) jarUrlString {

    NSString *bundlePath = @"";
    NSArray *jarUrlComponents = [jarUrlString componentsSeparatedByString: JAR_PATH_DELIMITER];

    // In the URL there must be exactly 1 exclamation mark, so after split there must be 2 components
    if ([jarUrlComponents count] == 2) {
        NSString *filePath = (NSString *) [jarUrlComponents lastObject];
        NSString *jarPath = (NSString *) [jarUrlComponents objectAtIndex: 0];

        unsigned int lastPathDelimiter = [MediaUtils lastIndexOf: PATH_DELIMITER
                                                        inString: jarPath];

        NSString *jarFileName = [jarPath substringFromIndex: lastPathDelimiter + 1];
        NSString *jarDirName = [JAR_DIR_PREFIX stringByAppendingString: jarFileName];
        jarDirName = [jarDirName stringByAppendingString: JAR_DIR_SUFFIX];

        filePath = [jarDirName stringByAppendingString: filePath];

        bundlePath = [[[NSBundle mainBundle] resourcePath]
                      stringByAppendingPathComponent: filePath];
    }

    return bundlePath;
}

+ (NSString *) resolveFileUrl: (NSString *) urlString {

    NSString *fileUrlString = urlString;

    if ([urlString hasPrefix: JAR_PREFIX]) {
        fileUrlString = [MediaUtils bundleUrlFromJarUrl: urlString];
    }

    return fileUrlString;
}

// the protocol of the locator passed to the Media constructor can only be http, file or jar
+ (NSURL *) urlFromString: (NSString *) urlString {

    NSURL *url = nil;

    if ([urlString hasPrefix: HTTP_PREFIX] || [urlString hasPrefix: IPOD_LIB_PREFIX]) {
        url = [NSURL URLWithString: urlString];
    }
    else {
        urlString = [MediaUtils resolveFileUrl: urlString];
        url = [NSURL fileURLWithPath: urlString];
    }

    return url;
}


@end
