#!/usr/bin/perl -w

my @files = ("../../main/native/gstreamer/3rd_party/glib/glib-2.56.1/build/win32/vs100/glib-lite.def",
             "../../main/native/gstreamer/3rd_party/glib/glib-2.56.1/build/win32/vs100/glib-liteD.def");

foreach $file (@files)
{
	process_file($file);
}

sub process_file
{
	my $infile = shift(@_);
	my %symbols = ();
	my $duplicates = 0;

	print ("Processing file $infile\n");
	open(INFILE, $infile) or die $!;

	while (my $str = <INFILE>)
	{
		$str =~ tr/\r\n//d;

		if ($str !~ /^EXPORTS/ && $str =~ /(\w+)/)
		{
			if (exists( $symbols{$1}))
			{
				$duplicates++;
				$symbols{$1}++;
			}
			else
			{
				$symbols{$1} = 1;
			}
		}
	}
	close(INFILE);

	my ($tmpfile) = $infile . ".tmp";
	print("Found $duplicates duplicates.\nSaving results to: $tmpfile\n");

	my $ordinal = 1;
	open(OUTFILE, ">$tmpfile") or die $!;
	print OUTFILE "EXPORTS\r\n";
	foreach $symbol (sort keys(%symbols))
	{
		print OUTFILE "${symbol}\t\@${ordinal}\tNONAME\r\n";
#		print OUTFILE "${symbol}\r\n";
		$ordinal++;
	}

	close(OUTFILE);

	print("Renaming $tmpfile to $infile\n\n");
	rename($tmpfile, $infile);
}
