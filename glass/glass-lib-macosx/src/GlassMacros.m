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

#import "GlassMacros.h"

#if defined (GLASS_USE_FILE_LOG)

FILE *log_file = NULL;

#endif

#if defined (GLASS_USE_WINDOW_LOG)

@implementation GlassLogWindow : NSWindow

- (id)initWithContentRect:(NSRect)contentRect styleMask:(NSUInteger)aStyle backing:(NSBackingStoreType)bufferingType defer:(BOOL)flag
{
self = [super initWithContentRect:contentRect styleMask:aStyle backing:bufferingType defer:flag];
if (self != nil)
{
    self->_textView = [[NSTextView alloc] initWithFrame:NSMakeRect(0.0f, 0.0f, contentRect.size.width, contentRect.size.height)];
    [self setContentView:self->_textView];
}
return self;
}

-(void)update:(NSString*)string
{
    [[[self->_textView textStorage] mutableString] appendString:string];
    
    // scroll old lines up
    NSRect frame = [self->_textView frame];
    frame.origin.y = 0.0f;
    [self->_textView setFrame:frame];
}

@end

NSWindow *window_log = nil;

#endif

NSDate *date = nil;
NSTimeInterval intervalLast = 0;
pthread_mutex_t LOCK = PTHREAD_MUTEX_INITIALIZER;
