# Generate implementation stubs for DOM interfaces.
#

use strict;
use File::Basename;

die "Usage: make-dom-stubs IFACE STUB\n" unless $#ARGV == 1;

my ($ifaceFile, $stubFile) = @ARGV;

my %NoStubsFor = (
    EventListener => 1,
    EventTarget => 1,
    NodeFilter => 1,
    );

$ifaceFile =~ /(\w+)\.java$/;
my $stubName = $1;
if ($NoStubsFor{$stubName}) {
    print "Skipping stub $stubName\n";
    exit 0;
}

open IFACE, $ifaceFile or die "Cannot open interface file $ifaceFile\n";
print "Generating stub for $stubName\n";

my $ifaceText = join "", <IFACE>;
close IFACE;

# Don't generate stubs for classes
exit 0  if $ifaceText =~ /public class/;

# Remove all comments
$ifaceText =~ s:/\*.*?\*/::sg;
$ifaceText =~ s://.*\n::g;

# Get package name and remove package declaration
$ifaceText =~ s/package\s+(.*?);//;
my $packageName = $1;
#print "pkg: $packageName\n";

# Change parent interface name if any to the stub parent class
$ifaceText =~ s/extends\s+(\w+)/extends $1Stub/;

# Change interface declaration to class declaration
$ifaceText =~ s/interface\s+(\w+)(.*?){/class $1Stub $2 implements $1 {/;

# Interface name
my $ifaceName = $1;

# Gather the required import statements
my %imported = ();
my $ifaceDir = dirname $ifaceFile;
while ($ifaceText =~ /\b(\p{isUpper}\w*)/g) {
    # Add an import only if we have file in the same dir.
    $imported{$1} = 1 if -r "$ifaceDir/$1.java";
#     $imported{$1} = 1 unless
#         $1 eq "String" or
#         $1 eq "Object" or
#         $1 =~ /_/ or
#         $1 =~ /Stub$/;
}

# Add methods body
$ifaceText =~ s/\)(\s*.*?);/)$1 {
        throw new UnsupportedOperationException("Not supported yet.");
    }/g;

# Start output
open STUB, "> $stubFile" or die "Cannot open stub file $stubFile\n";

print STUB "/* Automatically generated. Do not edit. */\n\n";

# Add package declaration
print STUB "package org.webkit.webcore.dom;\n\n";

# Add collected import statements.
for my $key (sort keys %imported) {
     print STUB "import $packageName.$key;\n";
}

# Add other import statements
while ($ifaceText =~ /^(import\s.*?;\n)/g) {
    print STUB $1;
}

# Dump methods definitions
print STUB $ifaceText;

close STUB;
1;

