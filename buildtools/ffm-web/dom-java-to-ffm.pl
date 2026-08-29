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
# Move the checked-in WebKit DOM binding wrappers
# (Source/WebCore/bindings/java/dom3/java/com/sun/webkit/dom/*Impl.java) off JNI and
# onto the flat wkj_* C ABI described in modules/javafx.web/FFM-ABI-CONTRACT.md.
#
# Two outputs:
#
#   1. one FFM facade per DOM type, <Type>Native.java, holding a static final
#      MethodHandle per function and a static wrapper method that marshals to and
#      from it. One class per type so symbol resolution stays lazy per type instead
#      of resolving ~1800 symbols the first time any DOM object is touched
#      (FFM-ABI-CONTRACT.md section 6).
#
#   2. the *Impl.java files, rewritten so that
#          native static String getNameImpl(long peer);
#      becomes
#          static String getNameImpl(long peer) {
#              return AttrNative.getName(peer);
#          }
#      Nothing else in those files changes: every call site, every disposer, every
#      equals/hashCode and the whole `long peer` refcount contract is left alone.
#
# The Java declaration, not the C type, decides what a value is. A jboolean and a
# jint are both int32_t/JAVA_INT in the C ABI, so the only surviving record of which
# int32_t are booleans is the Java signature. Every generated wrapper is therefore
# cross-checked against its spec row for arity and for type-to-layout agreement, and
# the script dies listing every mismatch rather than emitting Java that would
# silently pass a boolean where an int is meant.
#
# Usage:
#   perl buildtools/ffm-web/dom-java-to-ffm.pl [--apply] [--spec FILE] [--java-dir DIR]
#
# Without --apply nothing is written; the script parses, cross-checks and reports.
# It is deterministic and re-runnable: the facades are a pure function of the spec
# and the Java signatures, and an *Impl.java that has already been migrated is
# recognised and re-checked rather than rewritten twice.

use strict;
use warnings;
use Getopt::Long;
use File::Basename;
use File::Spec;

my $ROOT = File::Spec->catdir(dirname(File::Spec->rel2abs($0)), File::Spec->updir(), File::Spec->updir());

my $specFile = File::Spec->catfile($ROOT, 'buildtools', 'ffm-web', 'dom-abi.tsv');
my $javaDir  = File::Spec->catdir($ROOT, qw(modules javafx.web src main native Source WebCore
                                            bindings java dom3 java com sun webkit dom));
my $apply   = 0;
my $verbose = 0;
GetOptions(
    'spec=s'     => \$specFile,
    'java-dir=s' => \$javaDir,
    'apply'      => \$apply,
    'verbose'    => \$verbose,
) or die "bad options\n";

my $YEAR    = 2026;
my $MAXCOL  = 120;
my $PACKAGE = 'com.sun.webkit.dom';

# ---------------------------------------------------------------------------
# Java native declarations that have no row in the DOM ABI spec and are NOT dead.
#
# EventListenerImpl's three twk* methods are implemented in
# Source/WebCore/bindings/java/JavaEventListener.cpp, which is part of the CORE
# slice, not the DOM bindings: dom-abi.tsv is generated only from
# Source/WebKitLegacy/java/DOM/Java*.cpp, so they can never appear in it. They are
# live -- the shipped jfxwebkit exports all three -- and twkCreatePeer is an
# *instance* native whose `jobject self` becomes a WKJHost registry handle whose
# lifetime the C++ side owns (FFM-ABI-CONTRACT.md sections 3 and 4, and
# FFM-AUDIT-wtf-webcore.md section 6.5). Inventing that here would either break
# event dispatch or duplicate a lifetime decision that belongs to the core slice, so
# these three are left as `native` declarations for that slice to migrate.
#
# Every other declaration without a spec row is dead: no C function exists anywhere
# in the tree and the shipped library exports no such symbol, so calling it throws
# UnsatisfiedLinkError today. Those keep throwing, by generated body.
# ---------------------------------------------------------------------------
my %EXTERNAL = map { $_ => 1 } (
    'EventListenerImpl.twkCreatePeer',
    'EventListenerImpl.twkDispatchEvent',
    'EventListenerImpl.twkDisposeJSPeer',
);

# Java type -> the FFM layout the C ABI must use for it, and the exact static type
# the value has at the MethodHandle.invokeExact boundary. `boolean` and `int` share
# JAVA_INT, which is exactly why the Java side has to be the authority.
my %PARAM_LAYOUT = (
    'long'   => ['JAVA_LONG'],
    'int'    => ['JAVA_INT'],
    'boolean'=> ['JAVA_INT'],
    'short'  => ['JAVA_SHORT'],
    'float'  => ['JAVA_FLOAT'],
    'double' => ['JAVA_DOUBLE'],
    'String' => ['ADDRESS', 'JAVA_INT'],       # const uint16_t* s, int32_t s_len
);
my %RET_LAYOUT = (
    'void'   => 'void',
    'long'   => 'JAVA_LONG',
    'int'    => 'JAVA_INT',
    'boolean'=> 'JAVA_INT',
    'short'  => 'JAVA_SHORT',
    'float'  => 'JAVA_FLOAT',
    'double' => 'JAVA_DOUBLE',
    'String' => 'JAVA_INT',                    # a WKJ_STR_* status; see @STRING_RETURN_LAYOUTS
);

# A string-returning function writes into the caller's buffer and returns a status
# (FFM-ABI-CONTRACT.md section 13), so it grows three trailing parameters:
#   uint16_t* result_buf, int32_t result_cap, int32_t* result_length
my @STRING_RETURN_LAYOUTS = ('ADDRESS', 'JAVA_INT', 'ADDRESS');
my %CARRIER = (
    'JAVA_LONG'   => 'long',
    'JAVA_INT'    => 'int',
    'JAVA_SHORT'  => 'short',
    'JAVA_FLOAT'  => 'float',
    'JAVA_DOUBLE' => 'double',
    'ADDRESS'     => 'MemorySegment',
);

# Local variable names the generated bodies use; a parameter with one of these names
# would be shadowed, so it is a hard error rather than a subtle bug.
my %RESERVED = map { $_ => 1 } qw(arena required result resultBuffer resultLength status t);

# ---------------------------------------------------------------------------
# Two Java declarations have never agreed with the C function they call. JNI does not
# check: it resolves by name and lets the calling convention absorb the difference, so
# both have been latent since the bindings were generated, and the shipped library
# exports both symbols. FFM does check, which is how they surfaced. Each is written
# down here with the resolution that preserves today's observable behaviour exactly;
# the cross-check refuses any mismatch that is not in this table.
#
#   KeyboardEvent#initKeyboardEvent
#       Java declares a twelfth parameter, `boolean altGraphKey`, that the C function
#       has never accepted: JavaKeyboardEvent.cpp takes eleven, and the sibling
#       initKeyboardEventEx declares eleven on both sides. WebKit dropped altGraphKey
#       from initKeyboardEvent long ago -- KeyboardEventImpl.getAltGraphKeyImpl is one
#       of the declarations with no implementation at all. The callee never read the
#       argument, so the facade stops passing it; the Java signature and every call
#       site stay as they are.
#
#   MouseEvent#getButton
#       The C function returns int32_t (a widened int16_t); Java declares short, so
#       JNI has always taken the low sixteen bits. The descriptor follows the C ABI
#       and the facade narrows, which is the same value for every button WebKit
#       produces.
# ---------------------------------------------------------------------------
my %FIXUP = (
    'KeyboardEvent#initKeyboardEvent' => {
        dropTrailingArgs => 1,
        note => 'altGraphKey is accepted for source compatibility and not passed on: '
              . 'wkj_dom_KeyboardEvent_initKeyboardEvent has never taken it.',
    },
    'MouseEvent#getButton' => {
        returnLayout => 'JAVA_INT',
        note => 'wkj_dom_MouseEvent_getButton returns int32_t; DOM button values are '
              . 'short, and JNI narrowed here too.',
    },
);

# ---------------------------------------------------------------------------
# 1. The ABI spec. Columns are read by name: the C++ half of this migration owns
#    dom-abi.tsv and has already added one column (BUILT) since the Java half was
#    started, so positional access would be a landmine.
# ---------------------------------------------------------------------------
my (%spec, %order);
{
    open(my $fh, '<', $specFile) or die "$specFile: $!\n";
    my $header = <$fh>;
    defined $header or die "$specFile: empty\n";
    chomp $header;
    $header =~ s/\r$//;
    my @cols = split /\t/, $header;
    my %ix;
    $ix{ $cols[$_] } = $_ for 0 .. $#cols;
    for my $required (qw(SYMBOL RET_LAYOUT PARAM_LAYOUTS TYPE METHOD THROWS FILE)) {
        exists $ix{$required} or die "$specFile: missing required column '$required'\n";
    }
    my $n = 0;
    while (my $line = <$fh>) {
        chomp $line;
        $line =~ s/\r$//;
        next if $line eq '';
        my @c = split /\t/, $line, -1;
        my $row = {
            symbol  => $c[ $ix{SYMBOL} ],
            ret     => $c[ $ix{RET_LAYOUT} ],
            params  => [ grep { $_ ne '' } split /,/, $c[ $ix{PARAM_LAYOUTS} ] ],
            type    => $c[ $ix{TYPE} ],
            method  => $c[ $ix{METHOD} ],
            throws  => ($c[ $ix{THROWS} ] eq 'THROWS' ? 1 : 0),
            built   => (exists $ix{BUILT} ? ($c[ $ix{BUILT} ] ? 1 : 0) : 1),
            file    => $c[ $ix{FILE} ],
            order   => $n++,
        };
        my $key = "$row->{type}#$row->{method}";
        die "$specFile: duplicate spec row for $key\n" if exists $spec{$key};
        $spec{$key} = $row;
        push @{ $order{ $row->{type} } }, $row;
    }
    close $fh;
}
die "$specFile: no rows\n" unless %spec;

# ---------------------------------------------------------------------------
# 2. The Java side. Both forms are parsed so the script can be re-run: a JNI-form
#    `native` declaration ending in `;`, and an already-migrated method whose body
#    is one of the two shapes this script emits. Either way the Java signature -- the
#    only place a boolean is distinguishable from an int -- is checked again.
# ---------------------------------------------------------------------------
my @files = sort glob(File::Spec->catfile($javaDir, '*Impl.java'));
die "no *Impl.java under $javaDir\n" unless @files;

my $MODIFIER = qr/(?:public|private|protected|static|final|synchronized|native)/;

my (%decls, @problems, %stats);
for my $file (@files) {
    my $src = readFile($file);
    my $class = basename($file, '.java');
    (my $type = $class) =~ s/Impl$//;
    my @found;

    while ($src =~ m{
            (^[ \t]*)                                   # 1 indent
            ((?:$MODIFIER[ \t]+)*)                      # 2 modifiers
            ([A-Za-z_][\w.]*)[ \t]+                     # 3 return type
            (\w+)[ \t]*                                 # 4 name
            \(([^)]*)\)[ \t]*                           # 5 parameter list
            (;|\{)                                      # 6 native or bodied
        }gmsx) {
        my ($indent, $mods, $ret, $name, $params, $tail) = ($1, $2, $3, $4, $5, $6);
        # @- and @+ are clobbered by the very next successful match anywhere, so both
        # offsets are taken now, before anything else in this loop body runs.
        my ($start, $after) = ($-[0], $+[0]);
        my $isNative = ($mods =~ /\bnative\b/) ? 1 : 0;
        my $end;
        if ($tail eq ';') {
            next unless $isNative;                      # abstract/interface methods: none here
            $end = $after;
        } else {
            next if $isNative;                          # cannot happen; a native has no body
            $end = matchBrace($src, $after - 1);
            defined $end or next;
            my $body = substr($src, $after, $end - $after);
            # Only bodies this script wrote are re-parsed; every other method in the
            # file (getPeer, equals, the public org.w3c.dom API) is left alone.
            next unless $body =~ /\b\Q$type\ENative\./ || $body =~ /\bUnsatisfiedLinkError\b/;
            $end++;
        }
        push @found, {
            class  => $class,
            type   => $type,
            indent => $indent,
            mods   => $mods,
            ret    => $ret,
            name   => $name,
            params => $params,
            native => $isNative,
            start  => $start,
            end    => $end,
        };
    }

    $stats{files}++;
    for my $d (@found) {
        $stats{declarations}++;
        $stats{migratedAlready}++ unless $d->{native};
        my $args = parseParams($d->{params});
        unless (defined $args) {
            push @problems, "$class.$d->{name}: unparsable parameter list ($d->{params})";
            next;
        }
        $d->{args} = $args;
        for my $a (@$args) {
            push @problems, "$class.$d->{name}: parameter '$a->{name}' shadows a generated local"
                if $RESERVED{ $a->{name} };
        }
        (my $method = $d->{name}) =~ s/Impl$//;         # the 27 dispose functions carry no Impl
        $d->{method} = $method;
        push @{ $decls{$class} }, $d;
    }
    $decls{$class} ||= [];
    $decls{$class} = [ sort { $a->{start} <=> $b->{start} } @{ $decls{$class} } ];
}

# ---------------------------------------------------------------------------
# 3. Classify and cross-check. Nothing is written until every declaration has been
#    accounted for and every bound one agrees with its spec row.
# ---------------------------------------------------------------------------
my %usedSpec;
my (@bound, @unlinked, @external);
for my $class (sort keys %decls) {
    for my $d (@{ $decls{$class} }) {
        my $key = "$d->{type}#$d->{method}";
        my $row = $spec{$key};
        my $qualified = "$class.$d->{name}";

        if ($EXTERNAL{$qualified}) {
            if ($row) {
                push @problems, "$qualified: listed as EXTERNAL but the spec now defines "
                    . "$row->{symbol}; drop it from %EXTERNAL and let it be bound";
                next;
            }
            push @external, $d;
            next;
        }

        if (!$row) {
            # No C function anywhere in the tree: JNI throws UnsatisfiedLinkError on
            # the first call today, and so does the generated body.
            $d->{reason} = 'no wkj_* function exists for it in any jfxwebkit build';
            push @unlinked, $d;
            next;
        }

        $usedSpec{$key}++;

        if (!$row->{built}) {
            # The source file is commented out of Source/WebKitLegacy/PlatformJava.cmake,
            # so the symbol is not in the library. Binding it would make the whole
            # facade's class initializer fail, taking out every method of the type.
            $d->{reason} = "$row->{file} is not compiled into jfxwebkit";
            push @unlinked, $d;
            next;
        }

        my @mismatch = crossCheck($d, $row);
        if (@mismatch) {
            push @problems, map { "$qualified vs $row->{symbol}: $_" } @mismatch;
            next;
        }
        $d->{row} = $row;
        push @bound, $d;
    }
}

for my $key (sort keys %spec) {
    next if $usedSpec{$key};
    push @problems, "spec row $spec{$key}{symbol} has no Java declaration in "
        . "$spec{$key}{type}Impl.java";
}
for my $qualified (sort keys %EXTERNAL) {
    my ($c, $n) = split /\./, $qualified, 2;
    push @problems, "%EXTERNAL lists $qualified, which no longer exists"
        unless grep { $_->{name} eq $n } @{ $decls{$c} || [] };
}

if (@problems) {
    printf STDERR "%d problem(s); nothing written:\n", scalar @problems;
    print STDERR "  $_\n" for @problems;
    exit 1;
}

# ---------------------------------------------------------------------------
# 4. Emit the facades.
# ---------------------------------------------------------------------------
my %boundByType;
push @{ $boundByType{ $_->{type} } }, $_ for @bound;
for my $type (keys %boundByType) {
    $boundByType{$type} = [ sort { $a->{row}{order} <=> $b->{row}{order} } @{ $boundByType{$type} } ];
}

for my $type (sort keys %boundByType) {
    my $path = File::Spec->catfile($javaDir, "${type}Native.java");
    writeGenerated($path, facadeSource($type, $boundByType{$type})) if $apply;
    $stats{facades}++;
    $stats{facadeMethods} += scalar @{ $boundByType{$type} };
}
if (grep { $_->{ret} eq 'String' } @bound) {
    writeGenerated(File::Spec->catfile($javaDir, "DOMStringCodec.java"), stringCodecSource()) if $apply;
    $stats{facades}++;
}

# ---------------------------------------------------------------------------
# 5. Rewrite the *Impl.java files.
# ---------------------------------------------------------------------------
for my $file (@files) {
    my $class = basename($file, '.java');
    my @todo = grep { $_->{native} } @{ $decls{$class} || [] };
    @todo = grep { !$EXTERNAL{"$class.$_->{name}"} } @todo;
    next unless @todo;

    my $src = readFile($file);
    for my $d (reverse @todo) {                          # back to front keeps offsets valid
        substr($src, $d->{start}, $d->{end} - $d->{start}) = implMethod($d);
    }
    $src = bumpCopyright($src);
    writeFile($file, $src) if $apply;
    $stats{implRewritten}++;
}

# ---------------------------------------------------------------------------

printf STDERR "spec rows            : %d (%d built, %d not built)\n",
    scalar(keys %spec),
    scalar(grep { $_->{built} } values %spec),
    scalar(grep { !$_->{built} } values %spec);
printf STDERR "Impl files           : %d\n", $stats{files}            // 0;
printf STDERR "declarations found   : %d (%d already migrated)\n",
    $stats{declarations} // 0, $stats{migratedAlready} // 0;
printf STDERR "bound to the C ABI   : %d\n", scalar @bound;
printf STDERR "unimplemented        : %d\n", scalar @unlinked;
printf STDERR "left to another slice: %d\n", scalar @external;
printf STDERR "facades emitted      : %d holding %d methods\n",
    $stats{facades} // 0, $stats{facadeMethods} // 0;
printf STDERR "Impl files rewritten : %d\n", $stats{implRewritten}    // 0;
printf STDERR "mode                 : %s\n", ($apply ? 'APPLY' : 'dry run');

if (@external) {
    print STDERR "\nstill `native`, migrated by the core slice, not by this script:\n";
    printf STDERR "  %s.%s\n", $_->{class}, $_->{name} for @external;
}
if ($verbose && @unlinked) {
    print STDERR "\nunimplemented (generated body throws UnsatisfiedLinkError, as JNI does today):\n";
    printf STDERR "  %s.%s -- %s\n", $_->{class}, $_->{name}, $_->{reason} for @unlinked;
}
exit 0;

# ===========================================================================
# Parsing helpers
# ===========================================================================

sub readFile {
    my ($path) = @_;
    open(my $fh, '<:raw', $path) or die "$path: $!\n";
    local $/;
    my $s = <$fh>;
    close $fh;
    $s =~ s/\r\n/\n/g;
    return $s;
}

sub writeFile {
    my ($path, $text) = @_;
    $text =~ s/\r\n/\n/g;                                # LF endings (openjfx-conventions)
    open(my $fh, '>:raw', $path) or die "$path: $!\n";
    print $fh $text;
    close $fh;
}

sub writeGenerated {
    my ($path, $text) = @_;
    checkStyle($path, $text);
    writeFile($path, $text);
}

# openjfx-conventions: no tabs, no trailing whitespace, at most 120 columns. Applied to
# everything this script emits, so a wrapping bug is a build-tool failure rather than
# something a reviewer has to notice. The *Impl.java files carry pre-existing long lines
# and trailing whitespace, so only the replacement blocks are checked there.
sub checkStyle {
    my ($what, $text) = @_;
    my $n = 0;
    for my $line (split /\n/, $text, -1) {
        $n++;
        die sprintf("%s: generated line %d is %d columns:\n%s\n", $what, $n, length($line), $line)
            if length($line) > $MAXCOL;
        die "$what: generated line $n contains a tab\n"          if $line =~ /\t/;
        die "$what: generated line $n has trailing whitespace\n" if $line =~ /[ \t]$/;
    }
}

# Offset just past the `}` that closes the `{` at $open, or undef.
sub matchBrace {
    my ($src, $open) = @_;
    my $depth = 0;
    my $len = length $src;
    for (my $i = $open; $i < $len; $i++) {
        my $c = substr($src, $i, 1);
        if ($c eq '{') { $depth++; }
        elsif ($c eq '}') { $depth--; return $i if $depth == 0; }
    }
    return undef;
}

# "long peer, String value" -> [ { type => 'long', name => 'peer' }, ... ].
# Returns undef if anything in the list is not a plain `<type> <name>` pair; the DOM
# wrappers use no generics, arrays, varargs or annotations in these signatures.
sub parseParams {
    my ($params) = @_;
    $params =~ s/\s+/ /g;
    $params =~ s/^ | $//g;
    return [] if $params eq '';
    my @out;
    for my $p (split /\s*,\s*/, $params) {
        return undef unless $p =~ /^([A-Za-z_][\w.]*)\s+(\w+)$/;
        my ($t, $n) = ($1, $2);
        $t =~ s/^java\.lang\.//;
        return undef unless exists $PARAM_LAYOUT{$t};
        push @out, { type => $t, name => $n };
    }
    return \@out;
}

# The whole point of the script: prove that the Java signature and the C signature
# describe the same call, including which int32_t are really booleans. A jboolean and
# a jint are both int32_t/JAVA_INT, so the C side cannot tell them apart and the Java
# declaration is the only authority; every declaration is checked, not sampled.
sub crossCheck {
    my ($d, $row) = @_;
    my $fixup = $FIXUP{"$d->{type}#$d->{method}"} || {};
    my @bad;

    my $ret = $d->{ret};
    $ret =~ s/^java\.lang\.//;
    if (!exists $RET_LAYOUT{$ret}) {
        push @bad, "unsupported Java return type '$d->{ret}'";
        return @bad;
    }
    my $wantRet = $fixup->{returnLayout} || $RET_LAYOUT{$ret};
    push @bad, "returns $ret (needs $wantRet) but the spec returns $row->{ret}"
        if $wantRet ne $row->{ret};

    my $passed = passedArgs($d);
    my @want;
    push @want, @{ $PARAM_LAYOUT{ $_->{type} } } for @$passed;
    push @want, @STRING_RETURN_LAYOUTS if $ret eq 'String';
    my @have = @{ $row->{params} };

    if (scalar @want != scalar @have) {
        push @bad, sprintf('arity: Java (%s) needs %d C parameter(s) [%s], spec has %d [%s]',
            join(', ', map { "$_->{type} $_->{name}" } @{ $d->{args} }),
            scalar @want, join(',', @want), scalar @have, join(',', @have));
        return @bad;
    }
    for my $i (0 .. $#want) {
        push @bad, "parameter layout $i: Java needs $want[$i], spec has $have[$i]"
            if $want[$i] ne $have[$i];
    }
    return @bad;
}

# The Java parameters that actually reach the C function.
sub passedArgs {
    my ($d) = @_;
    my $drop = ($FIXUP{"$d->{type}#$d->{method}"} || {})->{dropTrailingArgs} || 0;
    my @args = @{ $d->{args} };
    pop @args for 1 .. $drop;
    return \@args;
}

# ===========================================================================
# Emission helpers
# ===========================================================================

sub copyrightHeader {
    my ($years) = @_;
    return <<"EOF";
/*
 * Copyright (c) $years, Oracle and/or its affiliates. All rights reserved.
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
EOF
}

# Modified file: the second year becomes 2026, added if the header carries only one.
sub bumpCopyright {
    my ($src) = @_;
    $src =~ s{^(\s*\*\s*Copyright \(c\) )(\d{4})(?:, \d{4})?(, Oracle)}{$1$2, $YEAR$3}m;
    return $src;
}

sub upperSnake {
    my ($name) = @_;
    $name =~ s/([a-z0-9])([A-Z])/$1_$2/g;
    $name =~ s/([A-Z]+)([A-Z][a-z])/$1_$2/g;
    return uc $name;
}

# Wrap `$head$items$tail` onto lines of at most $MAXCOL columns, breaking only between
# items and continuing at $contIndent; if the head plus the first item does not fit, the
# head takes a line of its own. Deterministic for a given input.
sub wrapCall {
    my ($indent, $head, $items, $tail, $contIndent) = @_;
    my $one = $indent . $head . join(', ', @$items) . $tail;
    return ($one) if !@$items || length($one) <= $MAXCOL;

    my @piece = map { $items->[$_] . ($_ == $#{$items} ? $tail : ',') } 0 .. $#{$items};
    my @lines;
    my $current = $indent . $head;
    my $started = 0;
    if (length($current . $piece[0]) > $MAXCOL) {
        push @lines, $current;
        $current = $contIndent;
    }
    for my $p (@piece) {
        my $candidate = $current . ($started ? ' ' : '') . $p;
        if (length($candidate) <= $MAXCOL || !$started) {
            $current = $candidate;
        } else {
            push @lines, $current;
            $current = $contIndent . $p;
        }
        $started = 1;
    }
    push @lines, $current;
    return @lines;
}

# `prefix"one long message"suffix` split across `+ "..."` continuation lines so that no
# line passes $MAXCOL. Splits only at spaces, which every message here has.
sub wrapStringLiteral {
    my ($indent, $prefix, $text, $suffix, $contIndent) = @_;
    my $one = qq{$indent$prefix"$text"$suffix};
    return ($one) if length($one) <= $MAXCOL;

    my @words = split / /, $text;
    my @lines;
    my $open = qq{$indent$prefix"};
    my $chunk = shift @words;
    while (@words) {
        my $word = shift @words;
        my $candidate = "$chunk $word";
        if (length($open . $candidate . '"') > $MAXCOL) {
            push @lines, qq{$open$chunk"};
            $open = qq{$contIndent+ "};
            $chunk = " $word";               # the continuation carries the separating space
        } else {
            $chunk = $candidate;
        }
    }
    push @lines, qq{$open$chunk"$suffix};
    return @lines;
}

# The one piece of the string protocol that is the same in all 357 getters. It lives in
# its own class rather than being inlined 357 times, and it holds no state, so it adds
# no lifetime rule of its own.
sub stringCodecSource {
    my $out = copyrightHeader($YEAR);
    $out .= "\npackage $PACKAGE;\n\n";
    $out .= "import java.lang.foreign.MemorySegment;\n";
    $out .= "import static java.lang.foreign.ValueLayout.JAVA_CHAR;\n";
    $out .= "import static java.lang.foreign.ValueLayout.JAVA_INT;\n";
    $out .= <<'EOF';

/**
 * The caller-provided-buffer string protocol shared by every string-returning DOM downcall
 * (see {@code modules/javafx.web/FFM-ABI-CONTRACT.md} section 13). The library writes UTF-16 code
 * units into a buffer this side allocates and owns, so nothing is returned that could dangle and
 * there is no "valid until the next call" rule for a facade to get wrong.
 * <p>
 * Null and empty are distinguished on the way out, which several {@code org.w3c.dom} getters rely
 * on: {@link #NULL} is Java {@code null}, {@link #OK} with a zero length is {@code ""}. On the way
 * in they collapse, because that is what the JNI bindings did (contract section 11.1).
 * <p>
 * Generated by {@code buildtools/ffm-web/dom-java-to-ffm.pl}. Do not edit; regenerate.
 */
final class DOMStringCodec {

    /** {@code WKJ_STR_OK}: {@code result_length} code units were written. */
    static final int OK = 0;

    /** {@code WKJ_STR_NULL}: the Java-visible value is {@code null}. */
    static final int NULL = 1;

    /** {@code WKJ_STR_OVERFLOW}: nothing was written; {@code result_length} is the capacity needed. */
    static final int OVERFLOW = 2;

    /**
     * The capacity, in UTF-16 code units, of the buffer a string getter offers on its first
     * attempt. Every attribute, tag name, URL and style property in the DOM fits; the
     * {@code innerHTML}, {@code outerHTML} and {@code textContent} shaped getters are the ones that
     * overflow, and they pay one extra downcall for an exactly sized buffer.
     */
    static final int CAPACITY = 256;

    private DOMStringCodec() {
    }

    /**
     * Turns a completed string call into its Java value.
     *
     * @param status the {@code WKJ_STR_*} status the call returned
     * @param buffer the buffer the call wrote into
     * @param length the segment the call wrote the code unit count into
     * @return the string, or {@code null} if the status is {@link #NULL}
     */
    static String decode(int status, MemorySegment buffer, MemorySegment length) {
        if (status == NULL) {
            return null;
        }
        if (status != OK) {
            // The caller has already grown the buffer to the size the library asked for, so a
            // second overflow means the two sides disagree about the protocol.
            throw new IllegalStateException("jfxwebkit returned string status " + status
                    + " for a buffer of the size it asked for");
        }
        int count = length.get(JAVA_INT, 0);
        if (count == 0) {
            return "";
        }
        char[] chars = new char[count];
        MemorySegment.copy(buffer, JAVA_CHAR, 0L, chars, 0, count);
        return new String(chars);
    }
}
EOF
    return $out;
}

# `private static final MethodHandle X = WebKitNative.downcall("sym", FunctionDescriptor...);`
# on as few lines as the 120 column limit allows: one, or two with the descriptor on its
# own line, or three with the layout list broken as well.
sub handleDeclaration {
    my ($name, $row) = @_;
    my $void = ($row->{ret} eq 'void');
    my $ctor = $void ? 'FunctionDescriptor.ofVoid(' : 'FunctionDescriptor.of(';
    my @layouts = $void ? @{ $row->{params} } : ($row->{ret}, @{ $row->{params} });
    my $head = "private static final MethodHandle $name = WebKitNative.downcall(";

    my @lines = wrapCall('    ', $head,
        [ qq{"$row->{symbol}"}, $ctor . join(', ', @layouts) . ')' ], ');', '            ');
    return @lines unless grep { length($_) > $MAXCOL } @lines;

    return (wrapCall('    ', $head, [ qq{"$row->{symbol}"} ], ',', '            '),
            wrapCall('            ', $ctor, \@layouts, '));', '                    '));
}

sub facadeSource {
    my ($type, $methods) = @_;

    my $needsArena   = 0;
    my $needsSegment = 0;
    my %layouts;
    for my $d (@$methods) {
        my $hasStringArg = grep { $_->{type} eq 'String' } @{ passedArgs($d) };
        $needsArena = 1 if $hasStringArg || $d->{ret} eq 'String';
        $needsSegment = 1 if $hasStringArg || $d->{ret} eq 'String';
        $layouts{JAVA_CHAR} = 1 if $d->{ret} eq 'String';   # the caller provided result buffer
        $layouts{$_} = 1 for @{ $d->{row}{params} };
        $layouts{ $d->{row}{ret} } = 1 unless $d->{row}{ret} eq 'void';
    }

    my @imports = ('com.sun.webkit.WebKitNative');
    push @imports, 'java.lang.foreign.Arena' if $needsArena;
    push @imports, 'java.lang.foreign.FunctionDescriptor';
    push @imports, 'java.lang.foreign.MemorySegment' if $needsSegment;
    push @imports, 'java.lang.invoke.MethodHandle';

    my $out = copyrightHeader($YEAR);
    $out .= "\npackage $PACKAGE;\n\n";
    $out .= "import $_;\n" for @imports;
    for my $l (qw(ADDRESS JAVA_CHAR JAVA_DOUBLE JAVA_FLOAT JAVA_INT JAVA_LONG JAVA_SHORT)) {
        $out .= "import static java.lang.foreign.ValueLayout.$l;\n" if $layouts{$l};
    }

    $out .= <<"EOF";

/**
 * FFM facade for the {\@code wkj_dom_${type}_*} functions of the {\@code jfxwebkit} C ABI, used by
 * {\@link ${type}Impl}. One facade per DOM type keeps symbol resolution lazy: the handles below are
 * bound the first time this type is touched, not the first time any DOM object is.
 * <p>
 * Generated by {\@code buildtools/ffm-web/dom-java-to-ffm.pl} from
 * {\@code buildtools/ffm-web/dom-abi.tsv}. Do not edit; regenerate.
 * <p>
 * These downcalls deliberately do not use {\@code Linker.Option.critical(true)}, and no downcall in
 * this layer may. A DOM read or mutation can synchronously run script and fire event listeners,
 * which upcall into Java, and a critical downcall forbids upcalls. The gain would be one avoided
 * copy of a string; the cost would be an intermittent crash.
 *
 * \@see com.sun.webkit.WebKitNative
 */
final class ${type}Native {

EOF

    my %handleName;
    my %handleSeen;
    for my $d (@$methods) {
        my $h = upperSnake($d->{method});
        die "$type: handle name collision on $h\n" if $handleSeen{$h}++;
        $handleName{ $d->{method} } = $h;
    }

    for my $d (@$methods) {
        $out .= join('', map { "$_\n" } handleDeclaration($handleName{ $d->{method} }, $d->{row}));
    }

    $out .= "\n    private ${type}Native() {\n    }\n";
    for my $d (@$methods) {
        $out .= facadeMethod($d, $handleName{ $d->{method} });
    }
    $out .= "}\n";
    return $out;
}

sub facadeMethod {
    my ($d, $handle) = @_;
    my $fixup = $FIXUP{"$d->{type}#$d->{method}"};

    my $out = "\n";
    $out .= join('', map { "$_\n" } wrapText('    // ', $fixup->{note})) if $fixup;
    $out .= join('', map { "$_\n" }
        wrapCall('    ', "static $d->{ret} $d->{method}(",
                 [ map { "$_->{type} $_->{name}" } @{ $d->{args} } ], ') {', '            '));
    $out .= ($d->{ret} eq 'String' ? stringReturnBody($d, $handle) : scalarReturnBody($d, $handle));
    return $out . "    }\n";
}

# Everything except the string returns: at most one downcall, a confined arena only if
# a string goes in, and checkException() outside the catch so a DOMException is not
# turned into an AssertionError on its way out.
sub scalarReturnBody {
    my ($d, $handle) = @_;
    my $row = $d->{row};
    my $ret = $d->{ret};

    my $passed = passedArgs($d);
    my @strings = grep { $_->{type} eq 'String' } @$passed;
    my $needsArena = scalar @strings ? 1 : 0;
    my $post = $row->{throws};
    my $carrier = $ret eq 'void' ? undef : $CARRIER{ $row->{ret} };
    my @callArgs = callArgs($passed);

    my $simple = (!$needsArena && !$post);
    my $body = '';
    $body .= "        $carrier result;\n" if !$simple && defined $carrier;
    if ($needsArena) {
        $body .= "        try (Arena arena = Arena.ofConfined()) {\n";
        $body .= allocStrings(\@strings, '            ');
        $body .= "            try {\n";
    } else {
        $body .= "        try {\n";
    }

    my $callIndent = $needsArena ? '                ' : '            ';
    my $head;
    if ($simple && defined $carrier) {
        $head = 'return ' . finalizeOpen($ret, $carrier) . "($carrier) $handle.invokeExact(";
    } elsif (defined $carrier) {
        $head = "result = ($carrier) $handle.invokeExact(";
    } else {
        $head = "$handle.invokeExact(";
    }
    my $tail = ')' . ($simple && defined $carrier ? finalizeClose($ret) : '') . ';';
    $body .= join('', map { "$_\n" }
        wrapCall($callIndent, $head, \@callArgs, $tail, $callIndent . '        '));

    if ($needsArena) {
        $body .= "            } catch (Throwable t) {\n"
               . "                throw new AssertionError(t);\n"
               . "            }\n"
               . "        }\n";
    } else {
        $body .= "        } catch (Throwable t) {\n"
               . "            throw new AssertionError(t);\n"
               . "        }\n";
    }

    $body .= "        WebKitNative.checkException();\n" if $post;
    $body .= '        return ' . finalizeOpen($ret, $carrier) . 'result' . finalizeClose($ret) . ";\n"
        if !$simple && defined $carrier;
    return $body;
}

# FFM-ABI-CONTRACT.md section 13: the caller owns the buffer, so there is no lifetime
# rule to violate. The function writes at most result_cap code units and returns
# WKJ_STR_OK, WKJ_STR_NULL or WKJ_STR_OVERFLOW; on overflow result_length holds the
# capacity needed and the call is repeated once with a buffer that size.
sub stringReturnBody {
    my ($d, $handle) = @_;
    my $row = $d->{row};
    my $passed = passedArgs($d);
    my @strings = grep { $_->{type} eq 'String' } @$passed;
    my @in = callArgs($passed);

    my $body = "        try (Arena arena = Arena.ofConfined()) {\n";
    $body .= allocStrings(\@strings, '            ');
    $body .= "            MemorySegment resultLength = arena.allocate(JAVA_INT);\n";
    $body .= "            MemorySegment resultBuffer = arena.allocate(JAVA_CHAR, "
           . "DOMStringCodec.CAPACITY);\n";
    $body .= "            int status;\n";
    $body .= "            try {\n";

    my @first = (@in, 'resultBuffer', 'DOMStringCodec.CAPACITY', 'resultLength');
    $body .= join('', map { "$_\n" }
        wrapCall('                ', "status = (int) $handle.invokeExact(", \@first, ');',
                 '                        '));
    $body .= "                if (status == DOMStringCodec.OVERFLOW) {\n";
    $body .= "                    int required = resultLength.get(JAVA_INT, 0);\n";
    $body .= "                    resultBuffer = arena.allocate(JAVA_CHAR, required);\n";
    my @again = (@in, 'resultBuffer', 'required', 'resultLength');
    $body .= join('', map { "$_\n" }
        wrapCall('                    ', "status = (int) $handle.invokeExact(", \@again, ');',
                 '                            '));
    $body .= "                }\n";
    $body .= "            } catch (Throwable t) {\n"
           . "                throw new AssertionError(t);\n"
           . "            }\n";
    $body .= "            WebKitNative.checkException();\n" if $row->{throws};
    $body .= "            return DOMStringCodec.decode(status, resultBuffer, resultLength);\n";
    $body .= "        }\n";
    return $body;
}

sub callArgs {
    my ($passed) = @_;
    my @out;
    for my $a (@$passed) {
        if ($a->{type} eq 'String') {
            push @out, "$a->{name}Segment", "WebKitNative.stringLength($a->{name})";
        } elsif ($a->{type} eq 'boolean') {
            push @out, "$a->{name} ? 1 : 0";
        } else {
            push @out, $a->{name};
        }
    }
    return @out;
}

sub allocStrings {
    my ($strings, $indent) = @_;
    my $out = '';
    for my $s (@$strings) {
        $out .= join('', map { "$_\n" }
            wrapCall($indent, "MemorySegment $s->{name}Segment = WebKitNative.allocString(",
                     ['arena', $s->{name}], ');', $indent . '        '));
    }
    return $out;
}

# The call itself is always `($carrier) HANDLE.invokeExact(...)`, where $carrier is the
# exact static type of the C return -- that is what makes it an invokeExact rather than
# an invoke. These two wrap that expression in whatever turns the carrier into the type
# the DOM wrapper declares: `!= 0` for a boolean, readStringOut for a string, a narrowing
# cast for the one function whose C return is wider than its Java return.
sub finalizeOpen {
    my ($ret, $carrier) = @_;
    return 'WebKitNative.readStringOut(' if $ret eq 'String';
    return "($ret) " if $ret ne 'boolean' && $ret ne $carrier;
    return '';
}

sub finalizeClose {
    my ($ret) = @_;
    return ')' if $ret eq 'String';
    return ' != 0' if $ret eq 'boolean';
    return '';
}

# Wrap a sentence into `// ` comment lines of at most $MAXCOL columns.
sub wrapText {
    my ($prefix, $text) = @_;
    my @lines;
    my $current = $prefix;
    for my $word (split /\s+/, $text) {
        if (length("$current $word") > $MAXCOL && $current ne $prefix) {
            push @lines, $current;
            $current = "$prefix$word";
        } else {
            $current .= ($current eq $prefix ? '' : ' ') . $word;
        }
    }
    push @lines, $current;
    return @lines;
}

# ---------------------------------------------------------------------------
# The *Impl.java rewrite: `native ` disappears, the `;` becomes a body, and the
# modifiers, their order, the parameter list and its line breaks are untouched.
# ---------------------------------------------------------------------------
sub implMethod {
    my ($d) = @_;
    my $head = declarationHead($d);
    my $indent = $d->{indent};
    my $body;

    if (exists $d->{row}) {
        my @args = map { $_->{name} } @{ $d->{args} };
        $body = join("\n", wrapCall("$indent    ",
            ($d->{ret} eq 'void' ? '' : 'return ') . "$d->{type}Native.$d->{method}(",
            \@args, ');', "$indent            "));
    } else {
        # Exactly what JNI does today for these: the method links, and the first call
        # to it throws. See the run summary for the full list and the reasons.
        $body = join("\n", wrapStringLiteral("$indent    ", 'throw new UnsatisfiedLinkError(',
            "$PACKAGE.$d->{class}.$d->{name}: $d->{reason}", ');', "$indent            "));
    }
    checkStyle("$d->{class}.java", "$head {\n$body\n$indent}");
    return "$head {\n$body\n$indent}";
}

# The declaration text with the `native` modifier removed and the trailing `;`
# dropped, preserving everything else byte for byte.
sub declarationHead {
    my ($d) = @_;
    my $mods = $d->{mods};
    $mods =~ s/\bnative[ \t]+//;
    my $head = $d->{indent} . $mods . $d->{ret} . ' ' . $d->{name} . '(' . $d->{params} . ')';
    return $head;
}
