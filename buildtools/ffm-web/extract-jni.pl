#!/usr/bin/perl
# Extract every JNIEXPORT function signature from the javafx.web WebKit tree into a TSV
# spec: file, return type, C symbol, parameter list (types + names).
# Read-only analysis tool; emits TSV on stdout.
use strict;
use warnings;
use File::Find;

my @roots = @ARGV ? @ARGV : ('.');
my @files;
find(sub { push @files, $File::Find::name if /\.(cpp|mm|c)$/ }, @roots);

print join("\t", qw(FILE RET SYMBOL PARAMS)), "\n";
for my $f (sort @files) {
    open(my $fh, '<', $f) or next;
    local $/;
    my $src = <$fh>;
    close $fh;
    next unless $src =~ /JNIEXPORT/;
    # Match: JNIEXPORT <ret> JNICALL <name>(<params>)
    while ($src =~ /JNIEXPORT\s+([A-Za-z_][A-Za-z0-9_]*(?:\s*\*)?)\s+JNICALL\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)/gs) {
        my ($ret, $sym, $params) = ($1, $2, $3);
        $ret =~ s/\s+/ /g; $ret =~ s/^\s+|\s+$//g;
        $params =~ s/\s+/ /g; $params =~ s/^\s+|\s+$//g;
        (my $rel = $f) =~ s{^\./}{};
        print join("\t", $rel, $ret, $sym, $params), "\n";
    }
}
