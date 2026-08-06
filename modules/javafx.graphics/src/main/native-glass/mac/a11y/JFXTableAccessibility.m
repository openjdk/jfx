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

@interface JFXTableRowPlaceholder : NSAccessibilityElement
@end

@implementation JFXTableRowPlaceholder

- (jobject)getJAccessible
{
    return nil;
}

@end

static const NSInteger ACCESSIBILITY_ROW_WINDOW = 10;
static JFXTableRowPlaceholder *rowPlaceholder;
static dispatch_once_t rowPlaceholderOnce;

static id getRowPlaceholder(void)
{
    dispatch_once(&rowPlaceholderOnce, ^{
        rowPlaceholder = [[JFXTableRowPlaceholder alloc] init];
        [rowPlaceholder setAccessibilityRole:NSAccessibilityUnknownRole];
        [rowPlaceholder setAccessibilityElement:NO];
    });
    return rowPlaceholder;
}

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
    NSInteger count = [self accessibilityArrayCountForAttribute:@"AXRows"];
    if (count < 0) {
        return nil;
    }

    NSMutableArray *rows = [NSMutableArray arrayWithCapacity:(NSUInteger)count];
    id placeholder = getRowPlaceholder();
    for (NSInteger index = 0; index < count; index++) {
        [rows addObject:placeholder];
    }

    if (count > 0) {
        NSInteger firstRequestedRow = 0;
        NSInteger lastRequestedRow = MIN(count - 1, ACCESSIBILITY_ROW_WINDOW - 1);
        NSRange visibleRange = [[self requestNodeAttribute:@"AXVisibleItemRange"] rangeValue];
        if (visibleRange.length > 0 && visibleRange.location < (NSUInteger)count) {
            NSInteger firstVisibleRow = (NSInteger)visibleRange.location;
            NSInteger lastVisibleRow = MIN(count - 1,
                    firstVisibleRow + (NSInteger)visibleRange.length - 1);
            firstRequestedRow = MAX(0, firstVisibleRow - ACCESSIBILITY_ROW_WINDOW);
            lastRequestedRow = MIN(count - 1, lastVisibleRow + ACCESSIBILITY_ROW_WINDOW);
        }
        NSUInteger requestedRowCount = (NSUInteger)(lastRequestedRow - firstRequestedRow + 1);
        NSArray *requestedRows = [self requestNodeArrayAttribute:@"AXRows"
                                                           index:(NSUInteger)firstRequestedRow
                                                        maxCount:requestedRowCount];
        for (NSUInteger index = 0; index < requestedRows.count; index++) {
            [rows replaceObjectAtIndex:(NSUInteger)firstRequestedRow + index
                             withObject:[requestedRows objectAtIndex:index]];
        }
    }
    return rows;
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
