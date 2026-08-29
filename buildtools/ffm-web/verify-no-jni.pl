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
# The definition-of-done check for the javafx.web JNI removal.
#
# Prints a scoreboard of every remaining JNI artefact in the module and exits
# non-zero while any remains. Run it from the repository root:
#
#   perl buildtools/ffm-web/verify-no-jni.pl [--verbose]
#
# It counts rather than merely greps, so it is useful throughout the migration
# and not only at the end: the numbers are the progress metric.

use strict;
use warnings;
use File::Find;

my $verbose = grep { $_ eq '--verbose' } @ARGV;
my $module  = 'modules/javafx.web';
die "run me from the repository root (no $module here)\n" unless -d $module;

# ---------------------------------------------------------------------------
# The checks. Each is a name, a file set, a pattern, and the count that means
# "done" (always 0 -- but a check may be marked informational).
# ---------------------------------------------------------------------------
my @checks = (
    {
        name    => 'Java `native` declarations (src/main/java)',
        roots   => ["$module/src/main/java"],
        match   => qr/\.java$/,
        pattern => qr/(?:^|[^A-Za-z_])native\s+[A-Za-z_<\[]/,
    },
    {
        name    => 'Java `native` declarations (generated DOM wrappers)',
        roots   => ["$module/src/main/native/Source/WebCore/bindings/java/dom3/java"],
        match   => qr/\.java$/,
        pattern => qr/(?:^|[^A-Za-z_])native\s+[A-Za-z_<\[]/,
    },
    {
        name    => 'C/C++ including <jni.h>',
        roots   => ["$module/src/main/native/Source", "$module/src/main/native/Tools"],
        match   => qr/\.(?:cpp|c|h|hpp|mm|m)$/,
        pattern => qr/#\s*include\s*[<"]jni\.h[>"]/,
    },
    {
        name    => 'C/C++ naming a JNI type',
        roots   => ["$module/src/main/native/Source", "$module/src/main/native/Tools"],
        match   => qr/\.(?:cpp|c|h|hpp|mm|m)$/,
        pattern => qr/\b(?:JNIEnv|JavaVM|jobject|jclass|jstring|jmethodID|jfieldID|jthrowable|jweak)\b/,
    },
    {
        name    => 'C/C++ exporting a JNI entry point',
        roots   => ["$module/src/main/native/Source", "$module/src/main/native/Tools"],
        match   => qr/\.(?:cpp|c|h|hpp|mm|m)$/,
        pattern => qr/\b(?:JNIEXPORT|JNICALL|JNI_OnLoad|JNI_OnUnload|JNI_OnUnLoad)\b/,
    },
    {
        name    => 'C/C++ calling back into Java through JNI',
        roots   => ["$module/src/main/native/Source", "$module/src/main/native/Tools"],
        match   => qr/\.(?:cpp|c|h|hpp|mm|m)$/,
        pattern => qr/\b(?:Call(?:Static)?[A-Za-z]*Method|GetMethodID|GetStaticMethodID|GetFieldID|FindClass|NewGlobalRef|NewWeakGlobalRef|DeleteGlobalRef|AttachCurrentThread|RegisterNatives)\b/,
    },
    {
        name    => 'The JNI environment abstraction itself',
        roots   => ["$module/src/main/native/Source"],
        match   => qr/\.(?:cpp|h)$/,
        pattern => qr/\b(?:GetJavaEnv|CheckAndClearException|AttachThreadToJavaEnv|JLObject|JGObject|JLString|JLClass|JLocalRef|JGlobalRef)\b/,
    },
    {
        name    => 'Build files requiring the JDK headers or libjvm',
        roots   => ["$module/src/main/native"],
        match   => qr/(?:\.cmake|CMakeLists\.txt)$/,
        pattern => qr/JAVA_INCLUDE_PATH|JAVA_JVM_LIBRARY|find_package\s*\(\s*JNI|JAVA_JNI_GENSRC_PATH/,
    },
    {
        name    => 'Linker export maps still listing JNI symbols',
        roots   => ["$module/src/main/native/Source/WebCore"],
        match   => qr/^mapfile-/,
        pattern => qr/Java_com_sun|JNI_OnLoad|JNI_OnUnload/,
        basename_match => 1,
    },
    {
        name    => 'Generated JNI constant headers still included',
        roots   => ["$module/src/main/native/Source", "$module/src/main/native/Tools"],
        match   => qr/\.(?:cpp|h|mm)$/,
        pattern => qr/#\s*include\s*[<"]com_sun_[A-Za-z0-9_]+\.h[>"]/,
    },
    {
        name    => 'A JNI code generator that could re-emit all of it',
        roots   => ["$module/src/main/native/Source/WebCore/bindings/scripts"],
        match   => qr/\.pm$/,
        pattern => qr/JNIEXPORT|JNICALL|Java_com_sun_webkit/,
    },
);

# ---------------------------------------------------------------------------

my $failed = 0;
my $total  = 0;
printf "%-58s %8s  %s\n", 'CHECK', 'HITS', 'STATUS';
printf "%s\n", '-' x 78;

for my $check (@checks) {
    my @files;
    for my $root (@{ $check->{roots} }) {
        next unless -d $root;
        # no_chdir is required: without it File::Find chdirs into each directory,
        # so a -f test against $File::Find::name (a path relative to the starting
        # cwd) fails for every file and the scan silently reports everything clean.
        find({
            no_chdir => 1,
            wanted   => sub {
                return unless -f $File::Find::name;
                my $base = $File::Find::name;
                $base =~ s{.*/}{};
                my $name = $check->{basename_match} ? $base : $File::Find::name;
                push @files, $File::Find::name if $name =~ $check->{match};
            },
        }, $root);
    }

    my $hits = 0;
    my %byFile;
    for my $f (@files) {
        open(my $fh, '<', $f) or next;
        my $n = 0;
        while (my $line = <$fh>) {
            next if $line =~ m{^\s*(?://|\*|/\*)};      # cheap comment skip
            # Strip string literals before matching. Without this, a diagnostic like
            # `"missing native symbol: "` in WebKitNative.java counts as a `native`
            # declaration, and the scoreboard reads one higher than the truth forever.
            $line =~ s{"(?:\\.|[^"\\])*"}{""}g;
            if ($line =~ $check->{pattern}) {
                $n++;
                $hits++;
            }
        }
        close $fh;
        $byFile{$f} = $n if $n;
    }

    $total += $hits;
    $failed++ if $hits;
    printf "%-58s %8d  %s\n", $check->{name}, $hits, ($hits ? 'REMAINS' : 'clean');

    if ($verbose && %byFile) {
        for my $f (sort { $byFile{$b} <=> $byFile{$a} || $a cmp $b } keys %byFile) {
            printf "    %6d  %s\n", $byFile{$f}, $f;
        }
    }
}

printf "%s\n", '-' x 78;
printf "%-58s %8d\n", 'TOTAL remaining JNI artefacts', $total;

if ($failed) {
    printf "\n%d of %d checks still report JNI. Re-run with --verbose for the file list.\n",
        $failed, scalar @checks;
    exit 1;
}
print "\njavafx.web is free of JNI.\n";
exit 0;
