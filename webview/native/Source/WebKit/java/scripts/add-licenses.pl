# Add or replace license header to the WebKit source files.

use strict;

use File::Basename;
use File::Find;
use File::Temp;

use Getopt::Long;

# Filepattern-to-license map
my @MAP = ();

my $LOG_LEVEL = 0;

my $showHelp = undef;
GetOptions('map|m=s{2}' => \@MAP,
           'verbose|v+' => \$LOG_LEVEL,
           'help' => \$showHelp,
    ) or usage(1);

usage(0) if ($showHelp);
usage(1) unless (@MAP > 0 and @ARGV > 0);

sub usage {
    my $exitCode = shift;

    print STDERR <<EOM;
Usage: $0 
          { --map|-m regex filename }+
          { --verbose|-v }*
          [ --help ]
          { srcroot|filename }+
EOM

    exit($exitCode);
}

readLicenses();

for my $arg (@ARGV) {
    if (-d $arg) {
        logMessage(1, "Processing directory $arg\n");
        File::Find::find({ wanted => \&findCheckPath, no_chdir => 1 }, $arg);
    } else {
        logMessage(1, "Processing file $arg\n");
        checkPath(File::Basename::basename($arg), $arg,
                  File::Basename::dirname($arg));
    }
}

exit(0);

# Change filenames to license texts in the map.
sub readLicenses {
    logMessage(2, "MAP last index: $#MAP\n");
    for (my $i = 1; $i < $#MAP + 1; $i += 2) {
        my $fileName = $MAP[$i];
        logMessage(1, "Reading license file $fileName\n");
        $MAP[$i] = soakFile($fileName, 'license');
        logMessage(2, "Read @{[length($MAP[$i])]} characters\n");
    }
}

sub findCheckPath {
    checkPath(File::Basename::basename($_), $File::Find::name, $File::Find::dir);
}

sub checkPath {
    my ($local, $name, $dir) = @_;

    logMessage(2, "checkPath: local=$local name=$name dir=$dir\n");

    for (my $i = 0; $i < $#MAP + 1; $i += 2) {
        if ($local =~ /$MAP[$i]/) {
            logMessage(2, "Got match $MAP[$i] for $local\n");
            checkLicense($name, $dir, $MAP[$i + 1]);
        }
    }
}

sub checkLicense {
    my ($fileName, $dirName, $licenseText) = @_;

    my ($currentLicenseText, $theRest) = readLicense($fileName);

    # only add/replace a license if the texts differ.

    addLicense($fileName, $dirName, $licenseText, $theRest)
        if ($currentLicenseText ne $licenseText);
}

sub readLicense {
    my $fileName = shift;

    my $fileData = soakFile($fileName, 'source');

    # consider license text to be the first comment block IF it
    # appears at the very beginning of the file AND immediately
    # followed by newline AND starts with the word "Copyright", any
    # case. Otherwise the license text is empty.

    if ($fileData =~ m:^(/\*[^\w]*copyright\b.*?\*/\r?\n)(.*)$:si) {
        return ($1, $2);
    } else {
        return ('', $fileData);
    }
}
    
sub addLicense {
    my ($fileName, $dirName, $licenseText, $theRest) = @_;

    logMessage(0, "Adding/changing license for $fileName...\n");

    my ($fh, $tempName) = File::Temp::tempfile(DIR => $dirName);
    print $fh ($licenseText, $theRest);
    close($fh);

    rename($tempName, $fileName)
        or die("Could not rename $tempName to $fileName: $!\n");
}

sub logMessage {
    my ($level, $message) = @_;

    print STDERR ($message) if ($level <= $LOG_LEVEL);
}

sub soakFile {
    my ($fileName, $tag) = @_;

    open(FH, $fileName)
        or die("Could not open $tag file $fileName: $!\n");
    local($/);
    undef($/);
    my $fileData = <FH>;
    close(FH);

    return $fileData;
}

