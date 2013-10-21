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

#import "MTObjectProxy.h"
#import <objc/runtime.h>

static int add_arg_type(NSMutableString *destString, const char *type)
{
    int index = 0;
    
    switch (type[index++]) {
        case _C_ID:
            [destString appendString:@"id"];
            break;
        case _C_CLASS:
            [destString appendString:@"Class"];
            break;
        case _C_SEL:
            [destString appendString:@"SEL"];
            break;
        case _C_CHR:
            [destString appendString:@"char"];
            break;
        case _C_UCHR:
            [destString appendString:@"unsigned char"];
            break;
        case _C_SHT:
            [destString appendString:@"short"];
            break;
        case _C_USHT:
            [destString appendString:@"unsigned short"];
            break;
        case _C_INT:
            [destString appendString:@"int"];
            break;
        case _C_UINT:
            [destString appendString:@"unsigned int"];
            break;
        case _C_LNG:
            [destString appendString:@"long"];
            break;
        case _C_ULNG:
            [destString appendString:@"unsigned long"];
            break;
        case _C_LNG_LNG:
            [destString appendString:@"long long"];
            break;
        case _C_ULNG_LNG:
            [destString appendString:@"unsigned long long"];
            break;
        case _C_FLT:
            [destString appendString:@"float"];
            break;
        case _C_DBL:
            [destString appendString:@"double"];
            break;
        case _C_BFLD:
            [destString appendString:@"bitfield"];
            break;
        case _C_BOOL:
            [destString appendString:@"BOOL"];
            break;
        case _C_VOID:
            [destString appendString:@"void"];
            break;
        case _C_UNDEF:
            [destString appendString:@"UNDEF"];
            break;
        case _C_PTR:
            add_arg_type(destString, &type[index]);
            [destString appendString:@" *"];
            break;
        case _C_CHARPTR:
            [destString appendString:@"char*"];
            break;
        case _C_ATOM:
            // ??? is this "atomic xxx" ?
            [destString appendString:@"??? ATOM ???"];
            break;
        case _C_ARY_B:
            [destString appendFormat:@"Array %s", &type[index]];
            break;
        case _C_ARY_E:
            [destString appendString:@"(array_end!)"];
            break;
        case _C_UNION_B:
            [destString appendFormat:@"Union %s", &type[index]];
            break;
        case _C_UNION_E:
            [destString appendString:@"(union_end!)"];
            break;
        case _C_STRUCT_B: {
            // if the next char is '?' then it's an anonymous struct, print the struct contents
            if (type[1] == '?' && type[2] == '=') {
                index += 2;
                int argCount = 0;
                [destString appendString:@"struct {"];
                while (type[index] != '}' && type[index] != '\0') {
                    if (argCount++) [destString appendString:@", "];
                    index += add_arg_type(destString, &type[index]);
                }
                index++; // move past the closing '}'
                [destString appendString:@"}"];
            } else {
                // otherwise it's a known type, just print the type name
                while (type[index] != '=' && type[index] != '}' && type[index] != '\0') {
                    [destString appendFormat:@"%c", type[index++]];
                }
            }
        }
            break;
        case _C_STRUCT_E:
            [destString appendString:@"(struct_end!)"];
            break;
        case _C_VECTOR:
            [destString appendString:@"VECTOR"];
            break;
        case _C_CONST:
            [destString appendString:@"const "];
            add_arg_type(destString, &type[index]);
            break;
        default:
            [destString appendString:@"???"];
            break;
    }
    return index;
}

static NSString *dump_method_signature(SEL selector, NSMethodSignature *sig)
{
    const char *type;
    NSMutableString *desc = [[[NSMutableString alloc] init] autorelease];
    NSString *selString = NSStringFromSelector(selector);
    NSArray *selComps = [selString componentsSeparatedByString:@":"];
    
    type = sig.methodReturnType;
    [desc appendString:@"- ("];
    add_arg_type(desc, type);
    [desc appendFormat:@") %@", [selComps objectAtIndex:0]];

    NSUInteger argCount = sig.numberOfArguments;
    // skip self and selecter hidden args
    if (argCount > 2) {
        NSUInteger index;
        for (index = 2; index < argCount; index++) {
            if (index > 2) {
                [desc appendFormat:@" %@:(", [selComps objectAtIndex:index-2]];
            } else {
                [desc appendString:@":("];
            }
            type = [sig getArgumentTypeAtIndex:index];
            add_arg_type(desc, type);
            [desc appendString:@")"];
        }
    }
    return desc;
}

@implementation MTObjectProxy

@synthesize target;
@synthesize targetThread;
@synthesize logMessages;

+ (MTObjectProxy*) objectProxyWithTarget:(id)obj
{
    return [[[MTObjectProxy alloc] initWithTarget:obj] autorelease];
}

+ (MTObjectProxy*) objectProxyWithTarget:(id)obj inThread:(NSThread*)t
{
    return [[[MTObjectProxy alloc] initWithTarget:obj inThread:t] autorelease];
}

- (id) initWithTarget:(id)obj
{
    target = [obj retain];
    targetThread = nil;
    return self;
}

- (id) initWithTarget:(id)obj inThread:(NSThread*)t
{
    target = [obj retain];
    targetThread = [t retain];
    return self;
}

- (void) dealloc
{
    [target release];
    [targetThread release];
    [super dealloc];
}

- (NSMethodSignature *)methodSignatureForSelector:(SEL)aSelector
{
    if (target)
        return [target methodSignatureForSelector:aSelector];
    return NULL;
}

- (BOOL) respondsToSelector:(SEL)aSelector
{
    if (logMessages) {
        NSLog(@"[P(%p -> %p) respondsToSelector:%@]", self, target, NSStringFromSelector(aSelector));
    }
    if (target)
        return [target respondsToSelector:aSelector];
    return NO;
}

- (void) forwardInvocation:(NSInvocation *)invocation
{
    if (!target) return;
    
    if (logMessages) {
        NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
        NSLog(@"[P(%p -> %p) forwardInvocation:{%@}]",
              self, target,
              dump_method_signature(invocation.selector, invocation.methodSignature)
              );
        [pool drain];
    }
    
    [invocation setTarget:target];
    if (targetThread && ![[NSThread currentThread] isEqualTo:targetThread]) {
        [invocation performSelector:@selector(invoke) onThread:targetThread withObject:nil waitUntilDone:YES];
    } else if (!targetThread && ![NSThread isMainThread]) {
        [invocation performSelectorOnMainThread:@selector(invoke) withObject:nil waitUntilDone:YES];
    } else {
        [invocation invoke];
    }
}

@end
