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

static NSRange composedCharacterRangeForIndex(NSString *value, NSInteger index)
{
    if (value == nil) {
        return NSMakeRange(NSNotFound, 0);
    }

    NSUInteger length = [value length];
    if (index < 0 || (NSUInteger)index >= length) {
        return NSMakeRange(NSNotFound, 0);
    }

    return [value rangeOfComposedCharacterSequenceAtIndex:(NSUInteger)index];
}

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

- (BOOL)isAccessibilityEnabled
{
    return TRUE;
}

- (BOOL)isAccessibilityEdited
{
    id value = [self requestNodeAttribute:@"AXEdited"];
    return value != nil ? [value boolValue] : NO;
}

- (NSArray *)accessibilityChildren
{
    return [super accessibilityChildren];
}

- (NSInteger)accessibilityInsertionPointLineNumber
{
    id retVal = [self requestNodeAttribute:@"AXInsertionPointLineNumber"];
    NSInteger lineNumber = retVal != nil ? [retVal integerValue] : 0;
    return lineNumber;
}

- (NSRange)accessibilityRangeForPosition:(NSPoint)point
{
    id parameter = [NSValue valueWithPoint:point];
    NSRange retVal = [[self requestNodeAttribute:@"AXRangeForPosition" forParameter:parameter] rangeValue];
    if (retVal.location == NSNotFound) {
        return retVal;
    }

    return composedCharacterRangeForIndex([self accessibilityValue], retVal.location);
}

- (NSRange)accessibilityRangeForIndex:(NSInteger)index
{
    return composedCharacterRangeForIndex([self accessibilityValue], index);
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
    [self setNodeAttribute:[NSValue valueWithRange:range]
              forAttribute:@"AXSelectedTextRange"];
}

- (id)accessibilitySelectedTextRanges
{
    id retVal = [self requestNodeAttribute:@"AXSelectedTextRange"];
    if (retVal == nil) {
        return nil;
    }

    NSRange range = [retVal rangeValue];
    if (range.location == NSNotFound) {
        return @[];;
    }

    NSArray *ranges = @[[NSValue valueWithRange:range]];
    return ranges;
}

- (void)setAccessibilitySelectedTextRanges:(NSArray<NSValue *> *)ranges
{
    [self setNodeAttribute:ranges forAttribute:@"AXSelectedTextRanges"];
}

- (void)setAccessibilityValue:(NSString *)value
{
    [self setNodeAttribute:value forAttribute:@"AXValue"];
}

- (void)setAccessibilitySelectedText:(NSString *)selectedText
{
    [self setNodeAttribute:selectedText forAttribute:@"AXSelectedText"];
}

- (NSString *)accessibilitySelectedText
{
    id parameter = [NSValue valueWithRange:[self accessibilitySelectedTextRange]];
    NSString * retVal = (NSString *)[self requestNodeAttribute:@"AXStringForRange" forParameter:parameter];
    return retVal;
}

- (NSRange)accessibilitySharedCharacterRange
{
    // JavaFX text controls expose a single text container, so the shared-text
    // range is the full document range for this accessible element.
    NSRange retVal = NSMakeRange(0, [self accessibilityNumberOfCharacters]);
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

- (BOOL)isAccessibilitySelectorAllowed:(SEL)selector
{
    if (selector == @selector(setAccessibilityValue:)) {
        return [self isNodeAttributeSettable:@"AXValue"];
    }
    if (selector == @selector(setAccessibilitySelectedTextRange:)) {
        return [self isNodeAttributeSettable:@"AXSelectedTextRange"];
    }
    if (selector == @selector(setAccessibilitySelectedTextRanges:)) {
        return [self isNodeAttributeSettable:@"AXSelectedTextRanges"];
    }
    return [super isAccessibilitySelectorAllowed:selector];
}

- (BOOL)accessibilityIsAttributeSettable:(NSString *)attribute
{
    return [self isNodeAttributeSettable:attribute];
}

@end
