#!/usr/bin/perl
#
# Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# Emit, from the machine-readable ABI spec produced by dom-cpp-to-ffm.pl:
#   --header  <path>   the generated C declarations for the DOM half of the ABI
#   --mapfile <path>   linker version-script fragment (ELF, --version-script)
#   --maposx  <path>   linker export list fragment (Mach-O, -exported_symbols_list)
#
# The mapfile outputs matter: Source/WebKitLegacy/PlatformJava.cmake passes an
# explicit export map to the linker on Linux and macOS, so a wkj_* function that
# is not listed there is NOT exported however it is annotated in C, and every
# SymbolLookup.find() for it fails at runtime with no build error.

use strict;
use warnings;
use Getopt::Long;

my ($specFile, $headerFile, $mapFile, $mapOsxFile);
GetOptions(
    'spec=s'    => \$specFile,
    'header=s'  => \$headerFile,
    'mapfile=s' => \$mapFile,
    'maposx=s'  => \$mapOsxFile,
) or die "bad options\n";
die "--spec is required\n" unless defined $specFile;

open(my $fh, '<', $specFile) or die "$specFile: $!";
my $head = <$fh>;
my @rows;
my $skipped = 0;
while (my $line = <$fh>) {
    chomp $line;
    next if $line eq '';
    my @f = split /\t/, $line, -1;
    # BUILT=0 rows come from DOM sources commented out of
    # Source/WebKitLegacy/PlatformJava.cmake, so their symbols are not in the
    # library. Declaring, exporting or binding them would turn into an
    # UnsatisfiedLinkError at class-initialisation time, so they are dropped here
    # exactly as they must be dropped from the Java facades.
    if (defined $f[8] && $f[8] eq '0') { $skipped++; next; }
    push @rows, { symbol => $f[0], ret => $f[1], params => $f[2], type => $f[5], method => $f[6] };
}
close $fh;
die "spec is empty\n" unless @rows;
printf STDERR "skipped %d symbol(s) whose source is not compiled (BUILT=0)\n", $skipped if $skipped;

my $functionCount = scalar @rows;
my %types = map { $_->{type} => 1 } @rows;
my $typeCount = scalar keys %types;

my $LICENSE = <<'END_LICENSE';
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
END_LICENSE

if (defined $headerFile) {
    open(my $out, '>', $headerFile) or die "$headerFile: $!";
    binmode $out;
    print $out $LICENSE;
    print $out <<"END_TOP";

/*
 * GENERATED FILE -- do not edit.
 *
 * Produced by buildtools/ffm-web/spec-to-header.pl from the ABI spec that
 * buildtools/ffm-web/dom-cpp-to-ffm.pl derives from the DOM binding sources.
 * Regenerate rather than editing:
 *
 *   perl buildtools/ffm-web/dom-cpp-to-ffm.pl \\
 *        --dir modules/javafx.web/src/main/native/Source/WebKitLegacy/java/DOM \\
 *        --spec buildtools/ffm-web/dom-abi.tsv
 *   perl buildtools/ffm-web/spec-to-header.pl --spec buildtools/ffm-web/dom-abi.tsv \\
 *        --header <this file>
 *
 * The DOM half of the flat C ABI: $functionCount functions over $typeCount DOM types.
 * Every function takes the node pointer as int64_t and uses only <stdint.h> types.
 *
 * Strings are UTF-16 in both directions and are never library-owned, so there is no
 * lifetime rule to get wrong (FFM-ABI-CONTRACT.md section 13):
 *
 *   in   const uint16_t* s, int32_t s_len
 *        A NULL pointer is Java null. Note that the library collapses null and the
 *        empty string to WTF::emptyString(), exactly as String(JNIEnv*, jstring) did.
 *
 *   out  uint16_t* result_buf, int32_t result_cap, int32_t* result_length
 *        The caller supplies the buffer; the function returns WKJ_STR_OK,
 *        WKJ_STR_NULL (Java null) or WKJ_STR_OVERFLOW, and writes the length -- or,
 *        on overflow, the required capacity -- through result_length. Null and empty
 *        ARE distinguished here: WKJ_STR_NULL versus WKJ_STR_OK with length 0.
 *
 * Every function clears the calling thread's exception slot on entry, so a missed
 * check on the Java side cannot leak an exception into an unrelated later call.
 */

#ifndef WEBKIT_JAVA_API_DOM_H
#define WEBKIT_JAVA_API_DOM_H

#include "webkit_java_api.h"

#ifdef __cplusplus
extern "C" {
#endif

END_TOP

    my $lastType = '';
    for my $r (@rows) {
        if ($r->{type} ne $lastType) {
            print $out "\n/* --- $r->{type} --- */\n";
            $lastType = $r->{type};
        }
        printf $out "WKJ_EXPORT %s %s(%s);\n", $r->{ret}, $r->{symbol}, $r->{params};
    }

    print $out <<'END_BOTTOM';

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* WEBKIT_JAVA_API_DOM_H */
END_BOTTOM
    close $out;
    printf STDERR "header : %s (%d declarations)\n", $headerFile, scalar @rows;
}

if (defined $mapFile) {
    open(my $out, '>', $mapFile) or die "$mapFile: $!";
    binmode $out;
    print $out "# GENERATED FILE -- do not edit; see buildtools/ffm-web/spec-to-header.pl\n";
    print $out "# ELF version script fragment: the DOM half of the wkj_* ABI.\n";
    print $out "        $_->{symbol};\n" for @rows;
    close $out;
    printf STDERR "mapfile: %s (%d symbols)\n", $mapFile, scalar @rows;
}

if (defined $mapOsxFile) {
    open(my $out, '>', $mapOsxFile) or die "$mapOsxFile: $!";
    binmode $out;
    print $out "# GENERATED FILE -- do not edit; see buildtools/ffm-web/spec-to-header.pl\n";
    print $out "# Mach-O export list fragment: the DOM half of the wkj_* ABI.\n";
    print $out "_$_->{symbol}\n" for @rows;
    close $out;
    printf STDERR "maposx : %s (%d symbols)\n", $mapOsxFile, scalar @rows;
}

exit 0;
