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
# Transform the checked-in WebKit DOM JNI bindings
# (Source/WebKitLegacy/java/DOM/Java*.cpp) to the flat wkj_* C ABI described in
# modules/javafx.web/FFM-ABI-CONTRACT.md.
#
# The transformation is purely mechanical and is safe only because the surface was
# measured first: the 108 files contain no direct `env->` calls, no JNI overload
# suffixes, no `_1` name escapes and no object types -- every use of `env` is one of
# six known constructs. This script REFUSES to transform anything it does not
# recognise rather than emitting silently-wrong C++; unrecognised input is reported
# and the file is left alone.
#
# Usage:
#   perl dom-cpp-to-ffm.pl --dir <DOM dir> [--apply] [--spec <out.tsv>] [--only <file>]
#
# Without --apply nothing is written; the script reports what it would do. With
# --spec it writes the machine-readable ABI spec consumed by the header generator,
# the linker mapfile generator, the wkjstub test library and the Java facades.

use strict;
use warnings;
use Getopt::Long;
use File::Basename;

my $dir;
my $apply = 0;
my $specFile;
my $only;
my $verbose = 0;
GetOptions(
    'dir=s'   => \$dir,
    'apply'   => \$apply,
    'spec=s'  => \$specFile,
    'only=s'  => \$only,
    'verbose' => \$verbose,
) or die "bad options\n";
die "--dir is required\n" unless defined $dir;

# ---------------------------------------------------------------------------
# Type mapping (FFM-ABI-CONTRACT.md section 2). jboolean deliberately becomes
# int32_t, not int8_t: the FFM API has no boolean layout, so the width is chosen
# explicitly and matched on the Java side with JAVA_INT.
# ---------------------------------------------------------------------------
my %SCALAR = (
    'void'     => 'void',
    'jlong'    => 'int64_t',
    'jint'     => 'int32_t',
    'jshort'   => 'int16_t',
    'jboolean' => 'int32_t',
    'jfloat'   => 'float',
    'jdouble'  => 'double',
);

my %JAVA_LAYOUT = (
    'void'     => 'void',
    'jlong'    => 'JAVA_LONG',
    'jint'     => 'JAVA_INT',
    'jshort'   => 'JAVA_SHORT',
    'jboolean' => 'JAVA_INT',
    'jfloat'   => 'JAVA_FLOAT',
    'jdouble'  => 'JAVA_DOUBLE',
);

my @files = sort glob("$dir/Java*.cpp");
@files = grep { basename($_) eq $only } @files if defined $only;
die "no files found under $dir\n" unless @files;

# ---------------------------------------------------------------------------
# Which DOM sources does the library actually compile? Two of them are commented
# out of Source/WebKitLegacy/PlatformJava.cmake (JavaDOMSelection.cpp and
# JavaWheelEvent.cpp, 35 exports between them), and their symbols are absent from
# the shipped library. They are still transformed -- leaving one JNI-shaped file
# behind would be worse -- but they are marked BUILT=0 in the spec so that the
# header, the mapfile and above all the Java facade skip them. Binding a symbol
# that is not in the library turns into an UnsatisfiedLinkError the first time the
# class initialises, which would take out every user of MouseEventImpl,
# DOMSelectionImpl and WheelEventImpl.
# ---------------------------------------------------------------------------
my %BUILT;
{
    my $cmake = "$dir/../../PlatformJava.cmake";
    if (-f $cmake) {
        open(my $cf, '<', $cmake) or die "$cmake: $!";
        while (my $l = <$cf>) {
            next if $l =~ /^\s*#/;                       # commented out of the build
            $BUILT{$1} = 1 if $l =~ m{java/DOM/(Java\w+\.cpp)};
        }
        close $cf;
    } else {
        warn "build list $cmake not found; assuming every file is compiled\n";
        $BUILT{ basename($_) } = 1 for @files;
    }
}

my $BRACE = chr(123);
our %CAN_RAISE;
my @spec;          # rows for the ABI spec
my @problems;      # anything not understood
my %stats;

for my $file (@files) {
    open(my $fh, '<', $file) or die "$file: $!";
    local $/;
    my $src = <$fh>;
    close $fh;

    # Every Java*.cpp in this directory is a DOM binding. Four of them
    # (JavaCDATASection, JavaCSSUnknownRule, JavaComment, JavaEntityReference)
    # export no functions but still include the JNI headers, so they are
    # transformed too rather than skipped.
    $stats{files}++;
    $stats{noExports}++ unless $src =~ /JNIEXPORT/;

    my $out = $src;
    my @fileProblems;

    # Which functions can raise? Determined from the ORIGINAL body, before any
    # rewriting: split the file at each JNIEXPORT so every chunk is one function,
    # then look for the exception raisers. The Java facade emits a
    # WebKitNative.checkException() after exactly these calls and nowhere else, so
    # the common path costs nothing.
    for my $chunk (split /(?=JNIEXPORT)/, $src) {
        next unless $chunk =~ /^JNIEXPORT\s+\S+\s+JNICALL\s+(Java_com_sun_webkit_dom_\w+)/;
        my $s = $1;
        $CAN_RAISE{$s} = ($chunk =~ /\braiseOnDOMError\b|\braiseDOMErrorException\b|\braiseTypeErrorException\b|\braiseNotSupportedErrorException\b/) ? 1 : 0;
    }

    # -- commented-out code ------------------------------------------------
    # JavaMouseEvent.cpp:123,129 hold two JNIEXPORT functions inside a /* */ block
    # ("the corresponding apis have been removed"). A raw grep counts them; the
    # compiler does not, and they are absent from the shipped library. Record the
    # comment spans so the signature rewrite leaves them alone and, crucially, so
    # they never reach the ABI spec.
    my @commentSpans;
    while ($src =~ m{/\*.*?\*/}gs) {
        push @commentSpans, [ $-[0], $+[0] ];
    }
    my $inComment = sub {
        my ($pos) = @_;
        for my $s (@commentSpans) {
            return 1 if $pos >= $s->[0] && $pos < $s->[1];
        }
        return 0;
    };

    # -- each exported function -------------------------------------------
    # Signature only; bodies are handled by substitution afterwards, which is
    # sound because no body contains a nested JNIEXPORT. Offsets from $-[0] are
    # offsets into $src, which is why this runs before the include rewrite.
    # The opening brace is consumed too, so that the clear-on-entry declaration can be
    # injected as the first statement of every exported body. webkit_java_api.h rule 4
    # requires it: 48 of the 124 throwing DOM functions return void, so a missed check
    # on the Java side would otherwise leave the slot dirty and the *next* unrelated
    # call on that thread would throw an exception belonging to the previous one.
    # Clearing on entry bounds a missed check to the call that caused it.
    $out =~ s{
        JNIEXPORT \s+ (\S+) \s+ JNICALL \s+ (Java_com_sun_webkit_dom_\w+) \s* \( ([^)]*) \)
        ( \s* ) \{
    }{
        my ($ret, $sym, $params, $gap) = ($1, $2, $3, $4);
        # $BRACE rather than a literal: Perl's s{}{}e delimiter matching counts
        # braces inside the replacement, escaped or not, and a literal one here
        # terminates the substitution early.
        if ($inComment->($-[0])) {
            $stats{commentedOut}++;
            "JNIEXPORT $ret JNICALL $sym($params)" . $gap . $BRACE;
        } else {
            my $sig = rewriteSignature($file, $ret, $sym, $params, \@fileProblems,
                                       $BUILT{ basename($file) } ? 1 : 0);
            $stats{scopes}++;
            $sig . $gap . $BRACE . "\n    WKJCallScope wkjScope;";
        }
    }gsex;

    # -- includes ----------------------------------------------------------
    $out =~ s{\#include <WebCore/JavaDOMUtils\.h>}{\#include <WebCore/WKJDOMUtils.h>}g;
    # The short spelling is the one that resolves everywhere. The CMake edits put
    # ${WEBKITLEGACY_DIR}/java/api on the include path of WTF, WebCore and
    # WebKitLegacy alike, whereas <WebKitLegacy/java/api/...> only resolves where
    # ${CMAKE_SOURCE_DIR}/Source is on the path -- true for WTF, false for the two
    # that matter here.
    $out =~ s{\#include <wtf/java/JavaEnv\.h>}{\#include <webkit_java_api.h>}g;

    # -- body substitutions ------------------------------------------------
    # 1. String-returning helper: always in `return JavaReturn<String>(env, EXPR);`.
    #    The caller supplies the buffer, so there is no lifetime rule to get wrong:
    #    the result is copied into result_buf before the call returns and nothing
    #    outlives it. See FFM-ABI-CONTRACT.md section 13.
    $out =~ s{JavaReturn<(?:WTF::)?String>\(env,}{WKJReturnString(result_buf, result_cap, result_length,}g;
    # 2. Peer-returning helper for every other type.
    $out =~ s{JavaReturn<([^>]+)>\(env,\s*}{WKJReturnPeer<$1>(}g;
    # 3. jstring -> WTF::String conversion; every argument is a plain parameter name.
    $out =~ s{\bString\(env,\s*(\w+)\)}{WKJString($1, ${1}_length)}g;
    # 4-6. Exception raisers lose their env argument.
    $out =~ s{\braiseOnDOMError\(env,\s*}{raiseOnDOMError(}g;
    $out =~ s{\braiseTypeErrorException\(env\)}{raiseTypeErrorException()}g;
    $out =~ s{\braiseNotSupportedErrorException\(env\)}{raiseNotSupportedErrorException()}g;
    # 7. The single direct call from one binding to another
    #    (NamedNodeMapImpl_setNamedItemNSImpl -> setNamedItemImpl). The JNI call
    #    passes (env, clazz, peer, node); BOTH leading arguments have to go, not
    #    just env, or the result is a call to an undeclared `clazz` with the wrong
    #    arity -- which compiles nowhere and which nothing in this repository would
    #    catch, since the WebKit tree is never built here.
    $out =~ s{\b(Java_com_sun_webkit_dom_(\w+?)Impl_(\w+?)(?:Impl)?)\(\s*env\s*,\s*clazz\s*,\s*}{
        "wkj_dom_$2_$3("
    }ge;
    $out =~ s{\b(Java_com_sun_webkit_dom_(\w+?)Impl_(\w+?)(?:Impl)?)\(\s*env\s*,\s*}{
        "wkj_dom_$2_$3("
    }ge;
    # 8. JNI-era spellings that survive in bodies: the peer cast macros and the
    #    jboolean constants. Renamed rather than kept so no jni.h name remains.
    $out =~ s{\bjlong_to_Nodeptr\b}{wkj_to_Nodeptr}g;
    $out =~ s{\bjlong_to_ptr\b}{wkj_to_ptr}g;
    $out =~ s{\bptr_to_jlong\b}{wkj_from_ptr}g;
    $out =~ s{\bJNI_FALSE\b}{0}g;
    $out =~ s{\bJNI_TRUE\b}{1}g;

    # -- verify nothing JNI-shaped survived --------------------------------
    for my $bad (qw(JNIEnv jclass jobject jstring jlong jint jboolean jshort jfloat jdouble
                    JNIEXPORT JNICALL JavaReturn JNI_FALSE JNI_TRUE jlong_to_ptr ptr_to_jlong
                    jlong_to_Nodeptr JavaDOMUtils JavaEnv)) {
        # Check the code, not the comments: the dead block in JavaMouseEvent.cpp is
        # deliberately left JNI-shaped, and flagging it would mask a real residual.
        (my $code = $out) =~ s{/\*.*?\*/}{}gs;
        if ($code =~ /\b\Q$bad\E\b/) {
            my @lines;
            my $n = 0;
            for my $l (split /\n/, $code) {
                $n++;
                push @lines, "$n: $l" if $l =~ /\b\Q$bad\E\b/;
            }
            push @fileProblems, "residual '$bad' after transform:\n    " . join("\n    ", @lines[0 .. ($#lines > 4 ? 4 : $#lines)]);
        }
    }
    (my $codeOnly = $out) =~ s{/\*.*?\*/}{}gs;

    # The JNI parameter names must be gone from the code, not merely from the
    # signatures. `clazz` in particular is the one that bit: substitution 7 used to
    # eat only `(env,` and leave `clazz` behind as a call argument, producing a call
    # to an undeclared identifier with the wrong arity. Nothing in this repository
    # compiles the WebKit tree, so a check here is the only thing standing between
    # that and a commit.
    for my $ident (qw(env clazz)) {
        next unless $codeOnly =~ /\b\Q$ident\E\b/;
        my @lines;
        my $n = 0;
        for my $l (split /\n/, $codeOnly) {
            $n++;
            push @lines, "$n: $l" if $l =~ /\b\Q$ident\E\b/;
        }
        push @fileProblems, "residual JNI parameter '$ident' after transform:\n    "
            . join("\n    ", @lines[0 .. ($#lines > 4 ? 4 : $#lines)]);
    }

    # Every helper the substitutions introduce must be one the new headers declare.
    # A typo here becomes an undefined symbol at link time in a build this repo
    # cannot run, so spell the expected set out and fail on anything else.
    my %KNOWN_HELPER = map { $_ => 1 } qw(
        WKJString WKJReturnString WKJReturnPeer wkj_to_ptr wkj_from_ptr wkj_to_Nodeptr
        raiseOnDOMError raiseTypeErrorException raiseNotSupportedErrorException
        WKJDOMUtils WKJHandle
        WKJCallScope WKJClearPendingException WKJSetPendingException
    );
    while ($codeOnly =~ /\b(WKJ[A-Za-z_]*|wkj_[a-z_]*)\b/g) {
        my $h = $1;
        next if $h =~ /^wkj_dom_/;      # the generated entry points themselves
        next if $h eq 'WKJ_EXPORT';
        next if $KNOWN_HELPER{$h};
        push @fileProblems, "unknown helper '$h' introduced by the transform";
    }

    if (@fileProblems) {
        push @problems, map { basename($file) . ": $_" } @fileProblems;
        $stats{skipped}++;
        next;
    }

    $stats{transformed}++;
    if ($apply) {
        open(my $wfh, '>', $file) or die "$file: $!";
        binmode $wfh;
        $out =~ s/\r\n/\n/g;    # keep LF endings (openjfx-conventions)
        print $wfh $out;
        close $wfh;
    }
}

# ---------------------------------------------------------------------------

sub rewriteSignature {
    my ($file, $ret, $sym, $params, $problems, $built) = @_;

    # Symbol: Java_com_sun_webkit_dom_<Type>Impl_<method>[Impl] -> wkj_dom_<Type>_<method>
    my ($type, $method);
    if ($sym =~ /^Java_com_sun_webkit_dom_(\w+?)Impl_(\w+)$/) {
        ($type, $method) = ($1, $2);
        $method =~ s/Impl$//;
    } else {
        push @$problems, "unrecognised symbol shape: $sym";
        return "JNIEXPORT $ret JNICALL $sym($params)";
    }
    my $newSym = "wkj_dom_${type}_${method}";

    # Return type.
    my $newRet;
    my $stringReturn = 0;
    if ($ret eq 'jstring') {
        # Caller-provided buffer, not a library-owned pointer: returns
        # WKJ_STR_OK / WKJ_STR_NULL / WKJ_STR_OVERFLOW and writes the length (or
        # the required capacity, on overflow) through result_length.
        $newRet = 'int32_t';
        $stringReturn = 1;
    } elsif (exists $SCALAR{$ret}) {
        $newRet = $SCALAR{$ret};
    } else {
        push @$problems, "unrecognised return type '$ret' on $sym";
        return "JNIEXPORT $ret JNICALL $sym($params)";
    }

    # Parameters.
    my @in = split /\s*,\s*/, $params;
    my @outParams;
    my @layouts;
    my $anon = 0;
    for my $p (@in) {
        $p =~ s/^\s+|\s+$//g;
        next if $p eq '';
        next if $p =~ /^JNIEnv\s*\*/;      # dropped
        next if $p =~ /^jclass\b/;         # dropped
        next if $p =~ /^jobject\b/;        # receiver, dropped (does not occur in DOM)

        my ($ptype, $pname);
        if ($p =~ /^(\w+)\s+(\w+)$/) {
            ($ptype, $pname) = ($1, $2);
        } elsif ($p =~ /^(\w+)$/) {
            ($ptype, $pname) = ($1, 'arg' . $anon++);   # unused parameter in a stub body
        } else {
            push @$problems, "unrecognised parameter '$p' on $sym";
            return "JNIEXPORT $ret JNICALL $sym($params)";
        }

        if ($ptype eq 'jstring') {
            push @outParams, "const uint16_t* $pname", "int32_t ${pname}_length";
            push @layouts, 'ADDRESS', 'JAVA_INT';
        } elsif (exists $SCALAR{$ptype}) {
            push @outParams, "$SCALAR{$ptype} $pname";
            push @layouts, $JAVA_LAYOUT{$ptype};
        } else {
            push @$problems, "unrecognised parameter type '$ptype' on $sym";
            return "JNIEXPORT $ret JNICALL $sym($params)";
        }
    }
    if ($stringReturn) {
        push @outParams, 'uint16_t* result_buf', 'int32_t result_cap', 'int32_t* result_length';
        push @layouts, 'ADDRESS', 'JAVA_INT', 'ADDRESS';
    }

    my $sig = join(', ', @outParams);
    $sig = 'void' if $sig eq '';

    push @spec, join("\t",
        $newSym,
        $newRet,
        $sig,
        ($stringReturn ? 'JAVA_INT' : $JAVA_LAYOUT{$ret}),
        join(',', @layouts),
        $type,
        $method,
        ($CAN_RAISE{$sym} ? "THROWS" : "-"),
        ($built ? 1 : 0),
        basename($file));
    $stats{functions}++;
    $stats{notBuilt}++ unless $built;

    return "WKJ_EXPORT $newRet $newSym($sig)";
}

# ---------------------------------------------------------------------------

if (defined $specFile) {
    open(my $sfh, '>', $specFile) or die "$specFile: $!";
    print $sfh join("\t", qw(SYMBOL RET PARAMS RET_LAYOUT PARAM_LAYOUTS TYPE METHOD THROWS BUILT FILE)), "\n";
    print $sfh "$_\n" for @spec;
    close $sfh;
}

printf STDERR "files seen        : %d\n", $stats{files}       // 0;
printf STDERR "files transformed : %d\n", $stats{transformed} // 0;
printf STDERR "files skipped     : %d\n", $stats{skipped}     // 0;
printf STDERR "functions mapped  : %d\n", $stats{functions}   // 0;
printf STDERR "mode              : %s\n", ($apply ? 'APPLY' : 'dry run');

if (@problems) {
    printf STDERR "\n%d problem(s) -- these files were NOT transformed:\n", scalar @problems;
    print STDERR "  $_\n" for @problems;
    exit 1;
}
exit 0;
