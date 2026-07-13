/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

#import "JFXTableAccessibility.h"

@implementation JFXTableAccessibility
- (NSAccessibilityRole)accessibilityRole
{
    return NSAccessibilityTableRole;
}

- (NSString *)accessibilityLabel
{
    return [super accessibilityLabel];
}

- (BOOL)isAccessibilityEnabled
{
    id retVal = [self requestNodeAttribute:@"AXEnabled"];
    if (retVal == NULL) {
        return YES;
    }
    return [retVal boolValue];
}

- (NSArray *)accessibilityChildren
{
    return [super accessibilityChildren];
}

- (id)accessibilityParent
{
    return [super accessibilityParent];
}

- (NSRect)accessibilityFrame
{
    return [super accessibilityFrame];
}

- (id)accessibilityHeader
{
    return [self requestNodeAttribute:@"AXHeader"];
}

- (NSInteger)accessibilityColumnCount
{
    id retVal = [self requestNodeAttribute:@"AXColumnCount"];
    if (retVal == NULL) {
        return [self accessibilityArrayCountForAttribute:@"AXColumns"];
    }
    return [retVal integerValue];
}

- (NSInteger)accessibilityRowCount
{
    id retVal = [self requestNodeAttribute:@"AXRowCount"];
    if (retVal == NULL) {
        return [self accessibilityArrayCountForAttribute:@"AXRows"];
    }
    return [retVal integerValue];
}

- (NSArray *)accessibilityColumns
{
    return [self accessibilityArrayForAttribute:@"AXColumns"];
}

- (NSArray *)accessibilityRows
{
    return [self accessibilityArrayForAttribute:@"AXRows"];
}

- (NSArray *)accessibilitySelectedRows
{
    return [self requestNodeAttribute:@"AXSelectedRows"];
}

- (void)setAccessibilitySelectedRows:(NSArray *)selectedRows
{
    [self setNodeAttribute:selectedRows forAttribute:@"AXSelectedRows"];
}

- (NSArray *)accessibilitySelectedCells
{
    return [self requestNodeAttribute:@"AXSelectedCells"];
}

- (void)setAccessibilitySelectedCells:(NSArray *)selectedCells
{
    [self setNodeAttribute:selectedCells forAttribute:@"AXSelectedCells"];
}

- (id)accessibilityCellForColumn:(NSInteger)column row:(NSInteger)row
{
    NSArray *parameter = @[[NSNumber numberWithInteger:column],
                           [NSNumber numberWithInteger:row]];
    return [self requestNodeAttribute:@"AXCellForColumnAndRow" forParameter:parameter];
}

- (NSArray *)accessibilityArrayForAttribute:(NSString *)attribute
{
    NSInteger count = [self accessibilityArrayCountForAttribute:attribute];
    if (count < 0) {
        return nil;
    }
    if (count == 0) {
        return @[];
    }
    NSArray *result = [self requestNodeArrayAttribute:attribute index:0 maxCount:(NSUInteger)count];
    if (result == nil) {
        result = [super accessibilityArrayAttributeValues:attribute index:0 maxCount:(NSUInteger)count];
    }
    return result;
}

- (NSInteger)accessibilityArrayCountForAttribute:(NSString *)attribute
{
    NSInteger count = [self requestNodeArrayAttributeCount:attribute];
    if (count < 0) {
        count = [super accessibilityArrayAttributeCount:attribute];
    }
    return count;
}

@end
