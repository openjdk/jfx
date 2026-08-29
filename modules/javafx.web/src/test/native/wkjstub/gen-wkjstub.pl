#!/usr/bin/perl
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
# gen-wkjstub.pl - generates the recording stub implementation of the wkj_* C
# ABI from the ABI header itself.
#
#   perl gen-wkjstub.pl --header <webkit_java_api.h> \
#                       --spec <buildtools/ffm-web/dom-abi.tsv> \
#                       --out <wkjstub_generated.c>
#
# Two inputs, because the ABI has two halves and they have different sources of
# truth:
#
#   --header  the CORE ABI header (exception slot, WKJHost, wkj_init, ...). Its
#             WKJ_EXPORT declarations, struct definitions and host table are
#             parsed out of the C.
#   --spec    the DOM ABI spec, buildtools/ffm-web/dom-abi.tsv, which
#             dom-cpp-to-ffm.pl derives mechanically from the DOM binding
#             sources. The DOM half is NEVER read from a hand-written header:
#             a guessed signature produces a stub that agrees with a wrong
#             FunctionDescriptor, which is silent memory corruption rather than
#             a clean failure - precisely what these tests exist to catch.
#             The generator additionally cross-checks each row's RET_LAYOUT and
#             PARAM_LAYOUTS columns against the kinds it derives from the C
#             types, and dies on disagreement.
#
# Emits, for every WKJ_EXPORT function in the header and every row in the spec:
#   * a stub body that records the call (name and one entry per parameter,
#     with UTF-16 string and primitive-array payloads copied) and returns a
#     safe default or a value programmed from Java through wkjstub_set_return_*;
# and, for the header as a whole:
#   * wkjstub_symbol_table  - every exported symbol with a compact signature
#                             string that a Java test compares against the
#                             FunctionDescriptor the facade binds;
#   * wkjstub_struct_table  - sizeof/offsetof for every struct in the header,
#                             plus wkjstub_sizeof_X / wkjstub_offsetof_X_y;
#   * wkjstub_host_slot_table and wkjstub_fire_host_slot - the flattened WKJHost
#     upcall table and a typed dispatcher, so a Java test can fire any host
#     callback by name.
#
# The generator is deliberately strict: an unrecognised C type is a fatal
# error rather than a guess, because a wrong guess would produce a stub that
# silently disagrees with the real library. The ABI is required to use
# <stdint.h> types (FFM-ABI-CONTRACT.md section 2).
#
# Exit status: 0 on success, non-zero with a message on stderr otherwise.
#

use strict;
use warnings;

my $header;
my $spec;
my $dom_include = 'webkit_java_api_dom.h';
my $out;
my @skip = qw(wkj_init wkj_abi_version wkj_exception_slot);

while (@ARGV) {
    my $arg = shift @ARGV;
    if ($arg eq '--header') { $header = shift @ARGV; }
    elsif ($arg eq '--spec') { $spec = shift @ARGV; }
    elsif ($arg eq '--dom-include') { $dom_include = shift @ARGV; }
    elsif ($arg eq '--out') { $out = shift @ARGV; }
    elsif ($arg eq '--skip') { push @skip, shift @ARGV; }
    else { die "gen-wkjstub: unknown option '$arg'\n"; }
}
die "gen-wkjstub: --header is required\n" unless defined $header;
die "gen-wkjstub: --out is required\n" unless defined $out;
die "gen-wkjstub: header not found: $header\n" unless -f $header;
die "gen-wkjstub: spec not found: $spec\n" if defined $spec && !-f $spec;

my %skip = map { $_ => 1 } @skip;

# ---------------------------------------------------------------- read input

# The core header is the entry point, but the ABI is spread over area headers it
# includes (_page, _platform, _theme, _bridge, _events, _wtf, _pal). Reading only the
# core header is how the stub came to model WKJHost with 13 groups while C declared
# 15: sizeof disagreed, and wkj_init would have rejected the real library with
# WKJ_INIT_ERR_HOST_SIZE. So follow the quoted includes, once each, in order.
# The generated DOM header is deliberately not followed -- the DOM half comes from
# the spec, which is the authority for it.
# Two include directions exist. _platform.h, _theme.h, _wtf.h, _pal.h and _events.h are
# pulled in BY the core header, because their structs are members of WKJHost and must be
# complete mid-header. _page.h and _bridge.h include the core instead, because their
# tables are installed separately and are not WKJHost members. Following includes alone
# therefore finds the first group and misses the second, so the siblings are globbed too.
my @headerIncludes;
my @headerQueue = ($header);
{
    my $dir = $header;
    $dir =~ s{[^/\\]+$}{};
    push @headerQueue, sort grep { !m{\Q$dom_include\E$} } glob("${dir}webkit_java_api*.h");
}
my %headerSeen;
my $src = '';
my $headerDir = $header;
$headerDir =~ s{[^/\\]+$}{};
while (my $h = shift @headerQueue) {
    my $path = ($h =~ m{[/\\]}) ? $h : "$headerDir$h";
    next if $headerSeen{$path}++;
    next unless -f $path;
    open(my $fh, '<', $path) or die "gen-wkjstub: cannot read $path: $!\n";
    my $text = do { local $/; <$fh> };
    close $fh;
    while ($text =~ /^[ \t]*#[ \t]*include[ \t]*"([^"]+)"/gm) {
        my $inc = $1;
        next if $inc eq $dom_include;          # the spec is the authority for the DOM half
        push @headerQueue, $inc;
    }
    $src .= "\n" . $text;
    my $base = $path;
    $base =~ s{.*[/\\]}{};
    push @headerIncludes, $base;
}
die "gen-wkjstub: read no ABI headers starting from $header\n" unless length $src;
printf STDERR "gen-wkjstub: read %d ABI header(s)\n", scalar keys %headerSeen;

# Strip comments, then preprocessor lines. Keep nothing from the preprocessor:
# the stub includes the header itself, so macros are the compiler's business.
$src =~ s{/\*.*?\*/}{ }gs;
$src =~ s{//[^\n]*}{}g;
$src =~ s{^[ \t]*#[^\n]*(?:\\\n[^\n]*)*$}{}gm;
$src =~ s{\bextern\s+"C"\s*\{}{ }g;

# --------------------------------------------------------- type dictionaries

my $IDENT = qr/[A-Za-z_][A-Za-z0-9_]*/;

# Scalar typedefs: typedef <base> <name>; The base repetition is non-greedy so
# that "typedef uint64_t wkj_ref" splits after the type, not inside the name.
my $SCALAR_TYPEDEF = qr/\btypedef\s+($IDENT(?:[ \t]+$IDENT)*?)[ \t]*(\**)[ \t]*($IDENT)[ \t]*;/;

my %alias;
while ($src =~ /$SCALAR_TYPEDEF/g) {
    my ($base, $stars, $name) = ($1, $2, $3);
    next if $base =~ /\b(?:struct|union|enum)\b/;
    $alias{$name} = $base . $stars;
}

# enum typedefs are int-sized on every platform this ABI targets
while ($src =~ /\btypedef\s+enum\s*(?:$IDENT)?\s*\{[^{}]*\}\s*($IDENT)\s*;/g) {
    $alias{$1} = 'int32_t';
}

# Struct typedefs, in declaration order.
my @structs;
my %struct_body;
{
    my $scan = $src;
    while ($scan =~ /\btypedef\s+struct\s*(?:$IDENT)?\s*\{([^{}]*)\}\s*($IDENT)\s*;/gs) {
        my ($body, $name) = ($1, $2);
        push @structs, $name;
        $struct_body{$name} = $body;
    }
}

sub is_struct { my ($t) = @_; return exists $struct_body{$t}; }

# Maps a C type spelling onto a one-character kind. Dies on anything the ABI
# is not allowed to use.
sub kind_of {
    my ($type, $context) = @_;
    die "gen-wkjstub: no type to classify" . (defined $context ? " for $context" : "")
      . " -- the declaration parser produced nothing, which means the ABI header uses a\n"
      . "  shape this generator does not understand. Fix the parser, do not guess a kind.\n"
        unless defined $type && $type =~ /\S/;
    my $t = $type;
    $t =~ s/\bconst\b//g;
    $t =~ s/\bvolatile\b//g;
    $t =~ s/\s+/ /g;
    $t =~ s/^\s+|\s+$//g;
    return 'p' if $t =~ /\*/;
    for (my $i = 0; $i < 10 && exists $alias{$t}; $i++) {
        $t = $alias{$t};
        $t =~ s/^\s+|\s+$//g;
        return 'p' if $t =~ /\*/;
    }
    return 'v' if $t eq 'void';
    return 'f' if $t eq 'float';
    return 'd' if $t eq 'double';
    return 'b' if $t =~ /^(?:int8_t|uint8_t|char|signed char|unsigned char)$/;
    return 'h' if $t =~ /^(?:int16_t|uint16_t|short|short int|unsigned short|unsigned short int)$/;
    return 'i' if $t =~ /^(?:int32_t|uint32_t|int|signed|signed int|unsigned|unsigned int)$/;
    return 'l' if $t =~ /^(?:int64_t|uint64_t|long long|unsigned long long|intptr_t|uintptr_t)$/;
    return 'S' if is_struct($t);
    die "gen-wkjstub: unsupported type '$type' in the ABI header.\n"
      . "  The C ABI must use <stdint.h> types only (see FFM-ABI-CONTRACT.md section 2);\n"
      . "  'long', 'size_t' and platform types have no stable FFM layout.\n";
}

sub c_type_of_kind {
    my ($k) = @_;
    return 'void'    if $k eq 'v';
    return 'int8_t'  if $k eq 'b';
    return 'int16_t' if $k eq 'h';
    return 'int32_t' if $k eq 'i';
    return 'int64_t' if $k eq 'l';
    return 'float'   if $k eq 'f';
    return 'double'  if $k eq 'd';
    return 'void*'   if $k eq 'p';
    die "gen-wkjstub: no C type for kind '$k'\n";
}

sub kind_macro {
    my ($k) = @_;
    my %m = (v => 'WKJSTUB_KIND_VOID', b => 'WKJSTUB_KIND_BYTE', h => 'WKJSTUB_KIND_SHORT',
             i => 'WKJSTUB_KIND_INT', l => 'WKJSTUB_KIND_LONG', f => 'WKJSTUB_KIND_FLOAT',
             d => 'WKJSTUB_KIND_DOUBLE', p => 'WKJSTUB_KIND_POINTER');
    die "gen-wkjstub: no kind macro for '$k'\n" unless exists $m{$k};
    return $m{$k};
}

# Splits a parameter list on top-level commas.
sub split_params {
    my ($text) = @_;
    $text =~ s/\s+/ /g;
    $text =~ s/^\s+|\s+$//g;
    return () if $text eq '' || $text eq 'void';
    die "gen-wkjstub: function-pointer parameters are not supported in the exported ABI: '$text'\n"
        if $text =~ /\(\s*\*/;
    return split(/\s*,\s*/, $text);
}

# Splits "const uint16_t* value" into ('const uint16_t*', 'value').
sub split_declarator {
    my ($decl, $index) = @_;
    $decl =~ s/\s+/ /g;
    $decl =~ s/^\s+|\s+$//g;
    # A fixed-size array member: "uint16_t message[WKJ_EXC_MESSAGE_MAX]".
    if ($decl =~ /^(.*?[\*\s])\s*([A-Za-z_][A-Za-z0-9_]*)\s*\[([^\]]*)\]$/) {
        my ($type, $name, $extent) = ($1, $2, $3);
        $type =~ s/\s+$//;
        return ($type, $name, $extent);
    }
    if ($decl =~ /^(.*?[\*\s])\s*([A-Za-z_][A-Za-z0-9_]*)$/) {
        my ($type, $name) = ($1, $2);
        $type =~ s/\s+$//;
        # "unsigned char" and friends: the trailing word is a type, not a name.
        unless ($name =~ /^(?:int|char|short|long|float|double|void|signed|unsigned)$/) {
            return ($type, $name, undef);
        }
    }
    return ($decl, "wkjstub_p$index", undef);
}

# ------------------------------------------------------ exported declarations

my @functions;
while ($src =~ /\bWKJ_EXPORT\s+(.*?)\b([A-Za-z_][A-Za-z0-9_]*)\s*\(([^;]*?)\)\s*;/gs) {
    my ($ret, $name, $params) = ($1, $2, $3);
    $ret =~ s/\s+/ /g;
    $ret =~ s/^\s+|\s+$//g;
    my @decls = split_params($params);
    my @args;
    my $i = 0;
    for my $d (@decls) {
        my ($type, $pname, $pextent) = split_declarator($d, $i);
        # An array-style parameter (const float x[4]) decays to a pointer in C. Dropping
        # the extent silently turned "const float* in_xywh" into "const float in_xywh",
        # i.e. a stub whose signature disagreed with the header it was generated from --
        # exactly the mismatch this library exists to catch.
        $type .= "*" if defined $pextent;
        # Name the function in the diagnostic. Reading several headers turned a silent
        # "uninitialized value" warning into the only clue about which declaration the
        # parser could not handle, which is not enough to act on.
        die "gen-wkjstub: cannot parse parameter " . ($i + 1) . " of $name: '$d'\n"
            unless defined $type && $type =~ /\S/;
        push @args, { type => $type, name => $pname, kind => kind_of($type) };
        $i++;
    }
    die "gen-wkjstub: $name has " . scalar(@args) . " parameters; raise WKJSTUB_MAX_ARGS\n"
        if scalar(@args) > 24;
    push @functions, { name => $name, ret => $ret, ret_kind => kind_of($ret), args => \@args,
                       throws => 0, source => 'core' };
}

die "gen-wkjstub: no WKJ_EXPORT declarations found in $header\n" unless @functions;

my $core_count = scalar(@functions);

# ---------------------------------------------- the DOM half, from the spec

# The layout vocabulary of dom-abi.tsv, mapped onto this generator's kinds.
my %layout_kind = (
    'void'        => 'v',
    'JAVA_BYTE'   => 'b',
    'JAVA_SHORT'  => 'h',
    'JAVA_INT'    => 'i',
    'JAVA_LONG'   => 'l',
    'JAVA_FLOAT'  => 'f',
    'JAVA_DOUBLE' => 'd',
    'ADDRESS'     => 'p',
);

my $dom_count = 0;
if (defined $spec) {
    open(my $sfh, '<', $spec) or die "gen-wkjstub: cannot read $spec: $!\n";
    my $head = <$sfh>;
    chomp $head;
    my @columns = split(/\t/, $head, -1);
    my %column;
    for (my $i = 0; $i < scalar(@columns); $i++) {
        $column{ $columns[$i] } = $i;
    }
    for my $required (qw(SYMBOL RET PARAMS RET_LAYOUT PARAM_LAYOUTS THROWS)) {
        die "gen-wkjstub: $spec has no '$required' column\n" unless exists $column{$required};
    }
    my %seen = map { $_->{name} => 1 } @functions;
    my $unbuilt = 0;
    while (my $line = <$sfh>) {
        chomp $line;
        next if $line eq '';
        my @f = split(/\t/, $line, -1);
        my $name = $f[ $column{SYMBOL} ];
        # BUILT=0 rows come from DOM sources that are not compiled into the
        # library, so it does not export them and neither may the stub -
        # otherwise symbol resolution passes here and fails against the real
        # jfxwebkit, which is the exact failure this stub exists to prevent.
        if (exists $column{BUILT} && $f[ $column{BUILT} ] eq '0') {
            $unbuilt++;
            next;
        }
        my $ret = $f[ $column{RET} ];
        my $params = $f[ $column{PARAMS} ];
        my $ret_layout = $f[ $column{RET_LAYOUT} ];
        my $param_layouts = $f[ $column{PARAM_LAYOUTS} ];
        my $throws = ($f[ $column{THROWS} ] eq 'THROWS') ? 1 : 0;

        die "gen-wkjstub: $name is declared both in $header and in $spec\n" if $seen{$name};
        $seen{$name} = 1;

        my @decls = split_params($params);
        my @args;
        my $i = 0;
        for my $d (@decls) {
            my ($type, $pname, $pextent) = split_declarator($d, $i);
        # An array-style parameter (const float x[4]) decays to a pointer in C. Dropping
        # the extent silently turned "const float* in_xywh" into "const float in_xywh",
        # i.e. a stub whose signature disagreed with the header it was generated from --
        # exactly the mismatch this library exists to catch.
        $type .= "*" if defined $pextent;
            push @args, { type => $type, name => $pname, kind => kind_of($type) };
            $i++;
        }
        die "gen-wkjstub: $name has " . scalar(@args) . " parameters; raise WKJSTUB_MAX_ARGS\n"
            if scalar(@args) > 24;

        # The spec states the FFM layouts independently of the C types; the two
        # must agree, or the Java facade and the library disagree silently.
        my $ret_kind = kind_of($ret);
        my $want_ret = $layout_kind{$ret_layout};
        die "gen-wkjstub: $name: RET_LAYOUT '$ret_layout' is not a known layout\n"
            unless defined $want_ret;
        die "gen-wkjstub: $name: return type '$ret' is kind '$ret_kind' but RET_LAYOUT says "
          . "'$ret_layout' (kind '$want_ret')\n" unless $ret_kind eq $want_ret;
        my @want = ($param_layouts eq '') ? () : split(/,/, $param_layouts);
        die "gen-wkjstub: $name: " . scalar(@args) . " parameters but " . scalar(@want)
          . " PARAM_LAYOUTS\n" unless scalar(@want) == scalar(@args);
        for (my $k = 0; $k < scalar(@args); $k++) {
            my $wk = $layout_kind{ $want[$k] };
            die "gen-wkjstub: $name: PARAM_LAYOUTS[$k] '$want[$k]' is not a known layout\n"
                unless defined $wk;
            die "gen-wkjstub: $name: parameter $k '$args[$k]->{type}' is kind "
              . "'$args[$k]->{kind}' but PARAM_LAYOUTS says '$want[$k]' (kind '$wk')\n"
                unless $args[$k]->{kind} eq $wk;
        }

        push @functions, { name => $name, ret => $ret, ret_kind => $ret_kind, args => \@args,
                           throws => $throws, source => 'dom' };
        $dom_count++;
    }
    close $sfh;
    die "gen-wkjstub: $spec contained no rows\n" unless $dom_count;
    printf STDERR "gen-wkjstub: skipped %d BUILT=0 symbol(s) the library does not export\n",
        $unbuilt if $unbuilt;
}

# ------------------------------------------------------------ struct members

# Returns a list of { name, type, kind } for one struct body.
sub struct_members {
    my ($name) = @_;
    my $body = $struct_body{$name};
    my @members;
    for my $stmt (split(/;/, $body)) {
        $stmt =~ s/\s+/ /g;
        $stmt =~ s/^\s+|\s+$//g;
        next if $stmt eq '';
        if ($stmt =~ /^(.*?)\(\s*\*\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)\s*\((.*)\)$/) {
            my ($ret, $fname, $params) = ($1, $2, $3);
            $ret =~ s/\s+$//;
            my @decls = split_params($params);
            my @args;
            my $i = 0;
            for my $d (@decls) {
                my ($type, $pname, $pextent) = split_declarator($d, $i);
        # An array-style parameter (const float x[4]) decays to a pointer in C. Dropping
        # the extent silently turned "const float* in_xywh" into "const float in_xywh",
        # i.e. a stub whose signature disagreed with the header it was generated from --
        # exactly the mismatch this library exists to catch.
        $type .= "*" if defined $pextent;
                push @args, { type => $type, name => $pname, kind => kind_of($type) };
                $i++;
            }
            push @members, { name => $fname, kind => 'p', callback => 1,
                             ret => $ret, ret_kind => kind_of($ret), args => \@args };
            next;
        }
        my ($type, $mname, $extent) = split_declarator($stmt, scalar(@members));
        push @members, { name => $mname, type => $type, kind => kind_of($type), callback => 0,
                         array => defined($extent) ? 1 : 0 };
    }
    return @members;
}

my %members_of;
for my $s (@structs) {
    $members_of{$s} = [ struct_members($s) ];
}

# ----------------------------------------------------- flatten the host table

my @host_slots;
sub flatten_host {
    my ($struct, $prefix, $offset_expr) = @_;
    for my $m (@{ $members_of{$struct} }) {
        my $path = ($prefix eq '') ? $m->{name} : "$prefix.$m->{name}";
        my $off = ($offset_expr eq '') ? "offsetof($struct, $m->{name})"
                                       : "$offset_expr + offsetof($struct, $m->{name})";
        if ($m->{callback}) {
            my $sig = $m->{ret_kind} . join('', map { $_->{kind} } @{ $m->{args} });
            push @host_slots, { name => $path, offset => $off, signature => $sig,
                                ret_kind => $m->{ret_kind}, ret => $m->{ret},
                                args => $m->{args} };
        } elsif ($m->{kind} eq 'S') {
            my $t = $m->{type};
            $t =~ s/\bconst\b//g;
            $t =~ s/\s+//g;
            flatten_host($t, $path, $off);
        }
    }
}
flatten_host('WKJHost', '', '') if exists $struct_body{'WKJHost'};

# ------------------------------------------------------------------- emitting

my @o;
sub emit { push @o, @_; }

emit(<<'HEADER');
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

/* GENERATED by modules/javafx.web/src/test/native/wkjstub/gen-wkjstub.pl - do not edit. */

#include "wkjstub.h"
#include "webkit_java_api.h"

#include <string.h>

static int64_t wkjstub_bits_of_float(float v)
{
    uint32_t bits;
    memcpy(&bits, &v, sizeof(bits));
    return (int64_t) (int32_t) bits;
}

static int64_t wkjstub_bits_of_double(double v)
{
    int64_t bits;
    memcpy(&bits, &v, sizeof(bits));
    return bits;
}

HEADER

# Include every header the declarations were read from, not just the core one.
# _page.h and _bridge.h include the core rather than being included by it, so their
# types (WKJLiveConnectHost, WKJPageCallbacks, ...) are otherwise undeclared here and
# the generated file will not compile.
for my $inc (@headerIncludes) {
    next if $inc eq 'webkit_java_api.h';        # already included by the preamble
    emit("#include \"$inc\"\n");
}
emit("\n") if @headerIncludes > 1;
emit("#include \"$dom_include\"\n\n") if defined $spec;
emit("/* core ABI header: $header */\n");
emit("/* DOM ABI spec   : $spec */\n") if defined $spec;
emit("\n");

# ---- function stubs

my $generated = 0;
for my $f (@functions) {
    next if $skip{ $f->{name} };
    $generated++;
    my @args = @{ $f->{args} };
    my $n = scalar(@args);

    # Convention detection, documented in the generated output so that the
    # result is reviewable.
    my @note;
    my @is_string = (0) x $n;
    my @is_array  = (0) x $n;
    my @elem_type = ('') x $n;
    for (my $i = 0; $i + 1 < $n; $i++) {
        next unless $args[$i]->{kind} eq 'p' && $args[$i + 1]->{kind} eq 'i';
        my $t = $args[$i]->{type};
        next unless $t =~ /^const\s+([A-Za-z_][A-Za-z0-9_]*)\s*\*$/;
        my $elem = $1;
        if ($elem eq 'uint16_t') {
            $is_string[$i] = 1;
            push @note, "arg $i is a UTF-16 string, length in arg " . ($i + 1);
        } else {
            my $k = eval { kind_of($elem) };
            next unless defined $k && $k =~ /^[bhilfd]$/;
            $is_array[$i] = 1;
            $elem_type[$i] = $elem;
            push @note, "arg $i is a $elem array, length in arg " . ($i + 1);
        }
        $i++;
    }

    die "gen-wkjstub: $f->{name} returns 'const uint16_t*'. The library-owned string return\n"
      . "  was withdrawn from the contract; a string comes out through\n"
      . "  (uint16_t* result_buf, int32_t result_cap, int32_t* result_length) with a\n"
      . "  WKJ_STR_OK / WKJ_STR_NULL / WKJ_STR_OVERFLOW status return.\n"
        if $f->{ret} =~ /uint16_t\s*\*/;

    # The caller-provides-the-buffer string return (contract 2, superseded by 13):
    #   int32_t f(..., uint16_t* result_buf, int32_t result_cap, int32_t* result_length)
    my $string_return = 0;
    my $buf_param = '';
    my $cap_param = '';
    my $out_len_param = '';
    if ($f->{ret_kind} eq 'i' && $n >= 3
            && $args[$n - 3]->{type} =~ /^uint16_t\s*\*$/
            && $args[$n - 2]->{kind} eq 'i'
            && $args[$n - 1]->{type} =~ /^int32_t\s*\*$/
            && $args[$n - 2]->{name} =~ /cap/i
            && $args[$n - 1]->{name} =~ /len(gth)?/i) {
        $string_return = 1;
        $buf_param = $args[$n - 3]->{name};
        $cap_param = $args[$n - 2]->{name};
        $out_len_param = $args[$n - 1]->{name};
        push @note, "returns a UTF-16 string into '$buf_param' (capacity '$cap_param'),"
                  . " length through '$out_len_param', status WKJ_STR_*";
    }

    my $out_array = 0;
    my $out_param = '';
    my $out_cap_param = '';
    if (!$string_return && $f->{ret_kind} eq 'i' && $n >= 2
            && $args[$n - 1]->{kind} eq 'i'
            && $args[$n - 2]->{type} =~ /^([A-Za-z_][A-Za-z0-9_]*)\s*\*$/
            && $args[$n - 1]->{name} =~ /cap/i) {
        my $elem = $1;
        my $k = eval { kind_of($elem) };
        if (defined $k && $k =~ /^[bhilfd]$/) {
            $out_array = 1;
            $out_param = $args[$n - 2]->{name};
            $out_cap_param = $args[$n - 1]->{name};
            push @note, "fills the '$out_param' array, capacity '$out_cap_param', returns the count";
        }
    }

    my $sig = $f->{ret_kind} . join('', map { $_->{kind} } @args);
    my $decl_args = $n ? join(', ', map { "$_->{type} $_->{name}" } @args) : 'void';

    emit("/* $sig");
    emit("; " . join('; ', @note)) if @note;
    emit(" */\n");
    emit("WKJ_EXPORT $f->{ret} $f->{name}($decl_args)\n{\n");
    if ($n > 0) {
        emit("    WKJStubArg wkjstub_args[$n];\n");
    }
    if ($string_return) {
        emit("    int32_t wkjstub_status;\n    int32_t wkjstub_ret_length = 0;\n");
    } elsif ($out_array) {
        emit("    int64_t wkjstub_value = 0;\n    int32_t wkjstub_count;\n");
    } elsif ($f->{ret_kind} eq 'f' || $f->{ret_kind} eq 'd') {
        emit("    double wkjstub_value = 0.0;\n");
    } elsif ($f->{ret_kind} ne 'v') {
        emit("    int64_t wkjstub_value = 0;\n");
    }
    emit("\n") if $n > 0 || $f->{ret_kind} ne 'v';

    for (my $i = 0; $i < $n; $i++) {
        my $a = $args[$i];
        my $slot = "&wkjstub_args[$i]";
        if ($is_string[$i]) {
            emit("    wkjstub_arg_string($slot, $a->{name}, $args[$i + 1]->{name});\n");
        } elsif ($is_array[$i]) {
            emit("    wkjstub_arg_array($slot, $a->{name}, $args[$i + 1]->{name},"
               . " (int32_t) sizeof($elem_type[$i]));\n");
        } elsif ($a->{kind} eq 'p') {
            emit("    wkjstub_arg_pointer($slot, (const void*) $a->{name});\n");
        } elsif ($a->{kind} eq 'f') {
            emit("    wkjstub_arg_scalar($slot, " . kind_macro('f')
               . ", wkjstub_bits_of_float($a->{name}));\n");
        } elsif ($a->{kind} eq 'd') {
            emit("    wkjstub_arg_scalar($slot, " . kind_macro('d')
               . ", wkjstub_bits_of_double($a->{name}));\n");
        } else {
            emit("    wkjstub_arg_scalar($slot, " . kind_macro($a->{kind})
               . ", (int64_t) $a->{name});\n");
        }
    }
    if ($n > 0) {
        emit("    wkjstub_record(\"$f->{name}\", wkjstub_args, $n);\n");
    } else {
        emit("    wkjstub_record(\"$f->{name}\", NULL, 0);\n");
    }

    if ($string_return) {
        emit("    wkjstub_status = wkjstub_programmed_out_string(\"$f->{name}\", $buf_param,"
           . " $cap_param, &wkjstub_ret_length);\n");
        emit("    if ($out_len_param != NULL) {\n        *$out_len_param = wkjstub_ret_length;\n    }\n");
        emit("    return wkjstub_status;\n");
    } elsif ($out_array) {
        emit("    wkjstub_count = wkjstub_programmed_fill(\"$f->{name}\", $out_param,"
           . " $out_cap_param, (int32_t) sizeof(*$out_param));\n");
        emit("    if (wkjstub_count >= 0) {\n        return wkjstub_count;\n    }\n");
        emit("    wkjstub_programmed_i64(\"$f->{name}\", &wkjstub_value);\n");
        emit("    return (int32_t) wkjstub_value;\n");
    } elsif ($f->{ret_kind} eq 'v') {
        # nothing to return
    } elsif ($f->{ret_kind} eq 'f') {
        emit("    wkjstub_programmed_f64(\"$f->{name}\", &wkjstub_value);\n");
        emit("    return (float) wkjstub_value;\n");
    } elsif ($f->{ret_kind} eq 'd') {
        emit("    wkjstub_programmed_f64(\"$f->{name}\", &wkjstub_value);\n");
        emit("    return wkjstub_value;\n");
    } elsif ($f->{ret_kind} eq 'p') {
        emit("    wkjstub_programmed_i64(\"$f->{name}\", &wkjstub_value);\n");
        emit("    return ($f->{ret}) (intptr_t) wkjstub_value;\n");
    } else {
        emit("    wkjstub_programmed_i64(\"$f->{name}\", &wkjstub_value);\n");
        emit("    return ($f->{ret}) wkjstub_value;\n");
    }
    emit("}\n\n");
}

# ---- symbol table

emit("const WKJStubSymbol wkjstub_symbol_table[] = {\n");
for my $f (@functions) {
    my $sig = $f->{ret_kind} . join('', map { $_->{kind} } @{ $f->{args} });
    emit("    { \"$f->{name}\", \"$sig\", $f->{throws} },\n");
}
emit("};\n");
emit("const int32_t wkjstub_symbol_table_size =\n");
emit("        (int32_t) (sizeof(wkjstub_symbol_table) / sizeof(wkjstub_symbol_table[0]));\n\n");

# ---- struct tables plus the individual sizeof/offsetof exports

for my $s (@structs) {
    my @m = @{ $members_of{$s} };
    emit("WKJSTUB_EXPORT int64_t wkjstub_sizeof_$s(void)\n{\n    return (int64_t) sizeof($s);\n}\n\n");
    for my $m (@m) {
        emit("WKJSTUB_EXPORT int64_t wkjstub_offsetof_${s}_$m->{name}(void)\n{\n"
           . "    return (int64_t) offsetof($s, $m->{name});\n}\n\n");
    }
    emit("static const WKJStubField wkjstub_fields_$s\[] = {\n");
    for my $m (@m) {
        # `elements` is 1 for a scalar member and the extent for an array member,
        # so a Java layout can build sequenceLayout(elements, <kind>) from it.
        my $elements = $m->{array}
            ? "(int32_t) (sizeof(((const $s*) 0)->$m->{name}) / sizeof($m->{type}))"
            : '1';
        emit("    { \"$m->{name}\", (int32_t) offsetof($s, $m->{name}),"
           . " (int32_t) sizeof(((const $s*) 0)->$m->{name}), '$m->{kind}', $elements },\n");
    }
    emit("};\n\n");
}

emit("const WKJStubStruct wkjstub_struct_table[] = {\n");
for my $s (@structs) {
    my $count = scalar(@{ $members_of{$s} });
    emit("    { \"$s\", (int32_t) sizeof($s), $count, wkjstub_fields_$s },\n");
}
emit("};\n");
emit("const int32_t wkjstub_struct_table_size =\n");
emit("        (int32_t) (sizeof(wkjstub_struct_table) / sizeof(wkjstub_struct_table[0]));\n\n");

# ---- host table

emit("const WKJStubHostSlot wkjstub_host_slot_table[] = {\n");
for my $s (@host_slots) {
    emit("    { \"$s->{name}\", (int32_t) ($s->{offset}), \"$s->{signature}\" },\n");
}
emit("    { NULL, 0, NULL }\n") unless @host_slots;
emit("};\n");
emit("const int32_t wkjstub_host_slot_table_size = " . scalar(@host_slots) . ";\n\n");

emit(<<'FIRE');
int32_t wkjstub_fire_host_slot(int32_t slot, const int64_t* argv, int32_t argc, int64_t* out_ret)
{
    const unsigned char* wkjstub_host = (const unsigned char*) wkjstub_host_bytes();

    if (wkjstub_host == NULL) {
        return -4;
    }
    if (argv == NULL && argc != 0) {
        return -3;
    }
    switch (slot) {
FIRE

for (my $i = 0; $i < scalar(@host_slots); $i++) {
    my $s = $host_slots[$i];
    my @args = @{ $s->{args} };
    my $n = scalar(@args);
    my $fn_params = $n ? join(', ', map { $_->{type} } @args) : 'void';
    emit("    case $i: { /* $s->{name} : $s->{signature} */\n");
    emit("        typedef $s->{ret} (*wkjstub_fn_t)($fn_params);\n");
    emit("        wkjstub_fn_t wkjstub_fn;\n");
    for (my $k = 0; $k < $n; $k++) {
        if ($args[$k]->{kind} eq 'f') {
            emit("        float wkjstub_a$k;\n        uint32_t wkjstub_a${k}_bits;\n");
        } elsif ($args[$k]->{kind} eq 'd') {
            emit("        double wkjstub_a$k;\n        int64_t wkjstub_a${k}_bits;\n");
        }
    }
    if ($s->{ret_kind} eq 'f') {
        emit("        float wkjstub_result;\n");
    } elsif ($s->{ret_kind} eq 'd') {
        emit("        double wkjstub_result;\n");
    } elsif ($s->{ret_kind} ne 'v') {
        emit("        $s->{ret} wkjstub_result;\n");
    }
    emit("\n");
    emit("        if (argc != $n) {\n            return -3;\n        }\n");
    emit("        memcpy(&wkjstub_fn, wkjstub_host + ($s->{offset}), sizeof(wkjstub_fn));\n");
    emit("        if (wkjstub_fn == NULL) {\n            return -2;\n        }\n");
    for (my $k = 0; $k < $n; $k++) {
        if ($args[$k]->{kind} eq 'f') {
            emit("        wkjstub_a${k}_bits = (uint32_t) argv[$k];\n");
            emit("        memcpy(&wkjstub_a$k, &wkjstub_a${k}_bits, sizeof(wkjstub_a$k));\n");
        } elsif ($args[$k]->{kind} eq 'd') {
            emit("        wkjstub_a${k}_bits = argv[$k];\n");
            emit("        memcpy(&wkjstub_a$k, &wkjstub_a${k}_bits, sizeof(wkjstub_a$k));\n");
        }
    }
    my @call;
    for (my $k = 0; $k < $n; $k++) {
        my $a = $args[$k];
        if ($a->{kind} eq 'f' || $a->{kind} eq 'd') {
            push @call, "wkjstub_a$k";
        } elsif ($a->{kind} eq 'p') {
            push @call, "($a->{type}) (intptr_t) argv[$k]";
        } else {
            push @call, "($a->{type}) argv[$k]";
        }
    }
    my $call = "wkjstub_fn(" . join(', ', @call) . ")";
    if ($s->{ret_kind} eq 'v') {
        emit("        $call;\n        *out_ret = 0;\n");
    } else {
        emit("        wkjstub_result = $call;\n");
        if ($s->{ret_kind} eq 'f') {
            emit("        *out_ret = wkjstub_bits_of_float(wkjstub_result);\n");
        } elsif ($s->{ret_kind} eq 'd') {
            emit("        *out_ret = wkjstub_bits_of_double(wkjstub_result);\n");
        } elsif ($s->{ret_kind} eq 'p') {
            emit("        *out_ret = (int64_t) (intptr_t) wkjstub_result;\n");
        } else {
            emit("        *out_ret = (int64_t) wkjstub_result;\n");
        }
    }
    emit("        return 0;\n    }\n");
}

emit("    default:\n        return -1;\n    }\n}\n");

# --------------------------------------------------------------- write output

my $text = join('', @o);
$text =~ s/\r\n/\n/g;

open(my $ofh, '>', $out) or die "gen-wkjstub: cannot write $out: $!\n";
binmode($ofh);
print $ofh $text;
close $ofh;

printf STDERR "gen-wkjstub: %s (%d core) + %s (%d DOM) -> %s\n",
    $header, $core_count, (defined $spec) ? $spec : '(no spec)', $dom_count, $out;
printf STDERR "gen-wkjstub: %d exported symbols, %d generated stubs, %d structs, %d host slots\n",
    scalar(@functions), $generated, scalar(@structs), scalar(@host_slots);
exit 0;
