my @platforms = ("linux", "solaris", "windows", "mac");
my %stdlibs =
(
#   linux => "xml2",         #have to be system
    solaris => "stlport",
    windows => "libxml2, libxslt"
);

my %iculibs =
(
#   linux => "icudata, icuuc, icui18n",   #have to be system
#   solaris => "icudata, icuuc, icui18n", #have to be system
    windows => "icudt36, icuuc36, icuin36",
);

my %imagelibs =
(
#    linux => "jpeg, png12",          #have to be system
#    solaris => "jpeg, png",        #have to be system
#    windows => "libjpeg, libpng",  #have to be static
);

my %skialibs =
(
    linux => "skia",
    solaris => "skia",
#   windows => "",    #have to be static
    mac => "libfreetype, skia"
);

# dump dynamic library lists
foreach $platform (@platforms) {
    my @libs;
    push @libs, $stdlibs{$platform} if $stdlibs{$platform};
    push @libs, $iculibs{$platform}
        if grep(/ICU_UNICODE=1/, @ARGV) && $iculibs{$platform};
    push @libs, $imagelibs{$platform}
        if !grep(/IMAGEIO=1/, @ARGV) && $imagelibs{$platform};
    push @libs, $skialibs{$platform}
        if grep(/ENABLE_SKIA=1/, @ARGV) && $skialibs{$platform};
    print "$platform.libs = ", join(", ", @libs), "\n";
}

# dump build-time flags
print join("\n", @ARGV), "\n";