#!/usr/bin/perl
# Classify Monocle baseline results across N surefire report directories.
# usage: perl s0-classify.pl <dir1> <dir2> ... > baseline.tsv
# For every test case seen in any run: PASS/FAIL/ERROR/SKIP per run, then a verdict:
#   persistent  - failed or errored in every run it executed
#   flaky       - failed in some runs, passed in others
#   stable-pass - passed in every run
#   skipped     - skipped in every run
use strict;
use warnings;

my @dirs = @ARGV or die "usage: s0-classify.pl <report-dir>...\n";
my %res;      # "class.name" -> [ per-run status ]
my %cause;    # "class.name" -> first failure type/message
my %classes;  # class -> 1

for my $i (0 .. $#dirs) {
    my $dir = $dirs[$i];
    for my $f (glob("$dir/TEST-*.xml")) {
        local $/;
        open my $fh, '<', $f or die "$f: $!";
        my $xml = <$fh>;
        close $fh;
        while ($xml =~ m{<testcase\s+name="([^"]*)"\s+classname="([^"]*)"[^>]*?(/>|>(.*?)</testcase>)}gs) {
            my ($name, $class, $selfclose, $body) = ($1, $2, $3, $4 // '');
            $class =~ s/.*\.//;
            $classes{$class} = 1;
            my $key = "$class.$name";
            my $status = 'PASS';
            if ($body =~ m{<skipped}) {
                $status = 'SKIP';
            } elsif ($body =~ m{<(failure|error)(?:\s+message="([^"]*)")?\s+type="([^"]*)"}s) {
                $status = uc $1;
                my ($type, $msg) = ($3, $2 // '');
                $msg =~ s/\s+/ /g;
                $msg =~ s/&apos;/'/g; $msg =~ s/&lt;/</g; $msg =~ s/&gt;/>/g; $msg =~ s/&quot;/"/g; $msg =~ s/&amp;/&/g;
                $cause{$key} //= "$type: " . substr($msg, 0, 120);
            }
            $res{$key}[$i] = $status;
        }
    }
}

my (%count, %perclass);
print join("\t", 'test', (map { "run" . ($_ + 1) } 0 .. $#dirs), 'verdict', 'cause'), "\n";
for my $key (sort keys %res) {
    my @s = map { $res{$key}[$_] // 'NA' } 0 .. $#dirs;
    my @ran = grep { $_ ne 'SKIP' && $_ ne 'NA' } @s;
    my $bad = grep { $_ eq 'FAILURE' || $_ eq 'ERROR' } @ran;
    my $verdict = !@ran ? 'skipped'
                : $bad == 0 ? 'stable-pass'
                : $bad == @ran ? 'persistent'
                : 'flaky';
    $count{$verdict}++;
    my ($class) = $key =~ /^([^.]+)\./;
    $perclass{$class}{$verdict}++;
    print join("\t", $key, @s, $verdict, $cause{$key} // ''), "\n";
}
print STDERR "verdicts: ", join(', ', map { "$_=$count{$_}" } sort keys %count), "\n";
print STDERR "classes with persistent or flaky results:\n";
for my $c (sort keys %perclass) {
    my $p = $perclass{$c}{persistent} // 0;
    my $f = $perclass{$c}{flaky} // 0;
    printf STDERR "  %-34s persistent=%-3d flaky=%-3d stable-pass=%-4d skipped=%d\n",
        $c, $p, $f, $perclass{$c}{'stable-pass'} // 0, $perclass{$c}{skipped} // 0 if $p || $f;
}
