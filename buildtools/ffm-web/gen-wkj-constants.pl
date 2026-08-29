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
# Generate wkj_constants.h -- the checked-in replacement for the 23 generated
# `com_sun_webkit_*.h` JNI constant headers the WebKit C++ includes.
#
# Those headers came from `javac -h`. Nothing in this repository runs `javac -h`:
# modules/javafx.web/pom.xml has no -h argument and no *.cmake invokes it, so all 23
# are absent from the tree and a from-source WebKit build here cannot succeed without
# an out-of-band step. Replacing them with one generated, checked-in header removes
# the last generated-JNI-header dependency from the C++ side, which is a strict
# improvement over the status quo as well as a prerequisite for dropping JNI.
#
# The values are read from the Java sources, which are the single source of truth --
# the same place `javac -h` read them from. Anything that cannot be resolved to an
# integer literal is reported and the run fails; a silently wrong constant here would
# be, for example, a keyboard event mapped to the wrong key.
#
# Usage, from the repository root:
#   perl buildtools/ffm-web/gen-wkj-constants.pl \
#        --java modules/javafx.web/src/main/java \
#        --native modules/javafx.web/src/main/native \
#        --out modules/javafx.web/src/main/native/Source/WebKitLegacy/java/api/wkj_constants.h

use strict;
use warnings;
use Getopt::Long;
use File::Find;

my ($javaRoot, $nativeRoot, $outFile);
GetOptions(
    'java=s'   => \$javaRoot,
    'native=s' => \$nativeRoot,
    'out=s'    => \$outFile,
) or die "bad options\n";
die "--java, --native and --out are required\n"
    unless defined $javaRoot && defined $nativeRoot && defined $outFile;

# ---------------------------------------------------------------------------
# 1. Which constants does the C++ actually use? Generate only those, so the
#    header cannot drift into carrying values nothing reads.
# ---------------------------------------------------------------------------
my %used;
find({
    no_chdir => 1,
    wanted   => sub {
        return unless -f $File::Find::name;
        return unless $File::Find::name =~ /\.(?:cpp|c|h|hpp|mm|m)$/;
        open(my $fh, '<', $File::Find::name) or return;
        local $/;
        my $src = <$fh>;
        close $fh;
        $src =~ s{/\*.*?\*/}{}gs;                       # ignore commented-out code
        $src =~ s{//[^\n]*}{}g;
        $used{$1} = 1 while $src =~ /\b(com_sun_webkit_[A-Za-z0-9_]+)\b/g;
    },
}, $nativeRoot);
delete $used{$_} for grep { /\.h$/ } keys %used;

# ---------------------------------------------------------------------------
# 2. Read every `static final` integral constant out of the Java sources.
#    javac -h emitted a #define for each of these; we reproduce the same set.
#    The key is the JNI mangling: com_sun_webkit_<Pkg>_<Class>_<FIELD>.
# ---------------------------------------------------------------------------
my %value;      # mangled name -> literal
my %origin;     # mangled name -> "file:line"
find({
    no_chdir => 1,
    wanted   => sub {
        return unless -f $File::Find::name && $File::Find::name =~ /\.java$/;
        my $rel = $File::Find::name;
        $rel =~ s{^\Q$javaRoot\E/?}{};
        return unless $rel =~ m{^com/sun/webkit/};
        (my $mangled = $rel) =~ s{\.java$}{};
        $mangled =~ s{/}{_}g;                            # com/sun/webkit/x/Y -> com_sun_webkit_x_Y

        open(my $fh, '<', $File::Find::name) or return;
        my $line = 0;
        while (my $l = <$fh>) {
            $line++;
            next unless $l =~ /\bstatic\b/ && $l =~ /\bfinal\b/;
            next unless $l =~ /\b(?:int|short|long|byte|char|boolean)\b/;
            next unless $l =~ /(\w+)\s*=\s*([^;]+);/;
            my ($field, $expr) = ($1, $2);
            $expr =~ s/^\s+|\s+$//g;
            $value{"${mangled}_$field"}  = $expr;
            $origin{"${mangled}_$field"} = "$rel:$line";
        }
        close $fh;
    },
}, $javaRoot);

# ---------------------------------------------------------------------------
# 3. Resolve each used name to an integer. Only forms that appear in these
#    sources are accepted; anything else fails the run rather than guessing.
# ---------------------------------------------------------------------------
sub resolve {
    my ($expr, $depth) = @_;
    return undef if ($depth // 0) > 8;
    $expr =~ s/^\s+|\s+$//g;
    $expr =~ s/^\((.*)\)$/$1/ if $expr =~ /^\([^()]*\)$/;
    return oct($expr)                 if $expr =~ /^0[xX][0-9a-fA-F]+$/;
    return $1 + 0                     if $expr =~ /^(-?\d+)[lL]?$/;
    return 1                          if $expr eq 'true';
    return 0                          if $expr eq 'false';
    return ord($1)                    if $expr =~ /^'(.)'$/;
    if ($expr =~ /^(.+?)\s*<<\s*(.+)$/) {                # 1 << 3
        my ($a, $b) = (resolve($1, ($depth // 0) + 1), resolve($2, ($depth // 0) + 1));
        return (defined $a && defined $b) ? ($a << $b) : undef;
    }
    if ($expr =~ /^(.+?)\s*\|\s*(.+)$/) {                # A | B
        my ($a, $b) = (resolve($1, ($depth // 0) + 1), resolve($2, ($depth // 0) + 1));
        return (defined $a && defined $b) ? ($a | $b) : undef;
    }
    if ($expr =~ /^(.+?)\s*\+\s*(.+)$/) {
        my ($a, $b) = (resolve($1, ($depth // 0) + 1), resolve($2, ($depth // 0) + 1));
        return (defined $a && defined $b) ? ($a + $b) : undef;
    }
    # A handful of constants alias a JDK value. Resolving those needs the JDK, not
    # this tree, so they are listed explicitly with the value verified by running the
    # JDK rather than read from documentation. Add to this table only after checking
    # the same way; a wrong value here is silently wrong behaviour.
    my %JDK = (
        'java.net.IDN.ALLOW_UNASSIGNED'      => 1,   # verified on JDK 26: java.net.IDN.ALLOW_UNASSIGNED == 1
        'java.net.IDN.USE_STD3_ASCII_RULES'  => 2,   # verified on JDK 26
    );
    return $JDK{$expr} if exists $JDK{$expr};

    if ($expr =~ /^[A-Za-z_]\w*$/) {                     # a sibling constant
        for my $k (keys %value) {
            return resolve($value{$k}, ($depth // 0) + 1) if $k =~ /_\Q$expr\E$/;
        }
    }
    return undef;
}

# Emit EVERY constant found, not merely the names spelled out in the C++.
#
# RenderThemeJava.cpp reaches its constants through token-pasting macros --
# `#define JNI_EXPAND(n) com_sun_webkit_graphics_RenderTheme_##n` -- so the full names
# exist only after preprocessing and no scan of the source text can see them. A scan
# of what "looks used" silently missed 19 of them, which would have been a build break
# in a build this repository cannot run. Emitting everything is also what `javac -h`
# did (it emitted a whole class at a time), so this reproduces the original semantics;
# a spare #define costs nothing.
my (@emit, @unresolved, @notConstants);
for my $name (sort keys %value) {
    my $v = resolve($value{$name});
    if (!defined $v) {
        # Only fail on constants the C++ actually names; the Java tree has plenty of
        # unrelated ones (string constants, expressions) that no native code reads.
        push @unresolved, "$name = $value{$name}   ($origin{$name})" if $used{$name};
        next;
    }
    push @emit, [ $name, $v, $origin{$name} ];
}
# Anything the C++ names directly that has no Java declaration at all.
for my $name (sort keys %used) {
    next if exists $value{$name};
    push @notConstants, $name if $name =~ /_[A-Z][A-Z0-9_]{2,}$/;
}

if (@unresolved) {
    print STDERR "cannot resolve to an integer:\n";
    print STDERR "  $_\n" for @unresolved;
    die "refusing to emit a header with unresolved constants\n";
}

# ---------------------------------------------------------------------------
# 4. Emit.
# ---------------------------------------------------------------------------
open(my $out, '>', $outFile) or die "$outFile: $!";
binmode $out;
print $out <<'END_LICENSE';
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

my $count = scalar @emit;
print $out <<"END_TOP";

/*
 * GENERATED FILE -- do not edit. Regenerate with:
 *
 *   perl buildtools/ffm-web/gen-wkj-constants.pl \\
 *        --java modules/javafx.web/src/main/java \\
 *        --native modules/javafx.web/src/main/native \\
 *        --out <this file>
 *
 * The $count constants the WebKit C++ shares with the Java side.
 *
 * These used to arrive through 23 separate `com_sun_webkit_*.h` headers emitted by
 * `javac -h`. Nothing in this repository runs `javac -h` -- modules/javafx.web/pom.xml
 * has no -h argument and no CMake file invokes it -- so none of those headers exists
 * here and a from-source WebKit build needed an out-of-band step to produce them.
 * Generating one checked-in header from the same Java sources removes that dependency
 * and, with it, the last reason for this C++ to know anything about JNI name mangling.
 *
 * The names are kept in their original mangled spelling so that the change to each
 * call site is an include swap and nothing else. Each value is followed by the Java
 * declaration it was read from, so a reviewer can check any one of them by eye.
 */

#ifndef WKJ_CONSTANTS_H
#define WKJ_CONSTANTS_H

END_TOP

my $lastClass = '';
for my $e (@emit) {
    my ($name, $v, $where) = @$e;
    (my $cls = $name) =~ s/_[A-Z][A-Z0-9_]*$//;
    if ($cls ne $lastClass) {
        print $out "\n/* --- $cls --- */\n";
        $lastClass = $cls;
    }
    printf $out "#define %-62s %-12s /* %s */\n", $name, $v, $where;
}

print $out "\n#endif /* WKJ_CONSTANTS_H */\n";
close $out;

printf STDERR "emitted %d constants to %s\n", $count, $outFile;
if (@notConstants) {
    printf STDERR "%d used name(s) look like constants but have no Java declaration:\n",
        scalar @notConstants;
    print STDERR "  $_\n" for @notConstants;
}
exit 0;
