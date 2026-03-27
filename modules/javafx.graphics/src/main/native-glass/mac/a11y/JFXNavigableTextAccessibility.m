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

#import "JFXNavigableTextAccessibility.h"

@implementation JFXNavigableTextAccessibility
- (NSAccessibilityRole)accessibilityRole
{
    if ([@"TEXT_FIELD" isEqualToString:[self getJavaRole]]) {
        return NSAccessibilityTextFieldRole;
    }
    return NSAccessibilityTextAreaRole;
}

- (NSAccessibilitySubrole)accessibilitySubrole
{
    id retVal = [self requestNodeAttribute:@"AXSubrole"];
    if (retVal == NULL) {
        return nil;
    }
    return NSAccessibilitySecureTextFieldSubrole;
}

- (id)accessibilityParent
{
    return [super accessibilityParent];
}

- (BOOL)isAccessibilityEnabled
{
    return TRUE;
}

- (BOOL)isAccessibilityEdited {
    return TRUE;
}

- (NSArray *)accessibilityChildren
{
    return [super accessibilityChildren];
}

- (NSRect)accessibilityFrame
{
    return [super accessibilityFrame];
}

- (NSString *)accessibilityValue
{
    return [super accessibilityValue];
}

- (NSString *)accessibilityTitle
{
    return [self requestNodeAttribute:@"AXTitle"];
}

- (nullable NSString *)accessibilityStringForRange:(NSRange)range
{
    id parameter = [NSValue valueWithRange:range];
    NSString * retVal = (NSString *)[self requestNodeAttribute:@"AXStringForRange" forParameter:parameter];
    return retVal;
}

- (NSInteger)accessibilityLineForIndex:(NSInteger)index
{
    id parameter = [NSNumber numberWithInteger:index];
    NSInteger retVal = [[self requestNodeAttribute:@"AXLineForIndex" forParameter:parameter] integerValue];
    return retVal;
}

- (NSRange)accessibilityRangeForLine:(NSInteger)lineNumber
{
    id parameter = [NSNumber numberWithInteger:lineNumber];
    NSRange retVal = [[self requestNodeAttribute:@"AXRangeForLine" forParameter:parameter] rangeValue];
    return retVal;
}

- (NSRect)accessibilityFrameForRange:(NSRange)range
{
    id parameter = [NSValue valueWithRange:range];
    NSRect retVal = [[self requestNodeAttribute:@"AXBoundsForRange" forParameter:parameter] rectValue];
    return retVal;
}

- (NSInteger)accessibilityNumberOfCharacters
{
    id retVal = [self requestNodeAttribute:@"AXNumberOfCharacters"];
    return [retVal integerValue];
}

- (NSRange)accessibilitySelectedTextRange
{
    NSRange retVal = [[self requestNodeAttribute:@"AXSelectedTextRange"] rangeValue];
    return retVal;
}

- (void)setAccessibilitySelectedTextRange:(NSRange)range {
// We do not need to do anything here. We just have to have it overridden.
}

- (id)accessibilitySelectedTextRanges
{
    return nil; //For now. Once we have multiselection text area we might revisit it.
}

- (NSString *)accessibilitySelectedText
{
    id parameter = [NSValue valueWithRange:[self accessibilitySelectedTextRange]];
    NSString * retVal = (NSString *)[self requestNodeAttribute:@"AXStringForRange" forParameter:parameter];
    return retVal;
}

- (NSRange)accessibilityVisibleCharacterRange
{
    NSRange retVal = [[self requestNodeAttribute:@"AXVisibleCharacterRange"] rangeValue];
    return retVal;
}

- (NSAttributedString *) accessibilityAttributedStringForRange:(NSRange)range
{
    id parameter = [NSValue valueWithRange:range];
    NSAttributedString * retVal = (NSAttributedString *)
            [self requestNodeAttribute:@"AXAttributedStringForRange" forParameter:parameter];
    return retVal;
}

@end
