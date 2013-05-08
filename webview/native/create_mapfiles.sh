echo "creating Source/WebCore/mapfile-macosx ..."
mapfile_macosx=Source/WebCore/mapfile-macosx
echo "               _JNI_OnLoad" > $mapfile_macosx
echo "               _JNI_OnUnload" >> $mapfile_macosx
echo "               _WTFReportBacktrace" >> $mapfile_macosx
echo "               _WTFReportAssertionFailure" >> $mapfile_macosx
echo "               __ZN3WTF8fastFreeEPv" >> $mapfile_macosx
echo "               __ZN3WTF10fastMallocEm" >> $mapfile_macosx
grep "^JS_EXPORT" -r Source --exclude-from=create_mapfiles.no-files --include *.h -h | sed "s/(/ /g" | sed "s/const//g" | grep -v "extern" | awk -F ' ' '{print "               _" $3}' | sort | uniq > mapfile.bak
grep "Java_" -r WebKitBuild --include *.c* -h | sed "s/(/ /g" | awk -F ' ' '{print "               _" $4}' | sort | uniq >> mapfile.bak
grep "Java_" -r Source --exclude-from=create_mapfiles.no-files --include *.c* -h | sed "s/(/ /g" | awk -F ' ' '{print "               _" $4}' | sort | uniq >> mapfile.bak
grep "^\s*_JS\|^\s*_Java" mapfile.bak >> $mapfile_macosx
rm -f mapfile.bak 
echo "done."

echo "creating Source/WebCore/mapfile-vers ..."
mapfile_vers=Source/WebCore/mapfile-vers
echo "SUNWprivate_1.0 {" > $mapfile_vers
echo "        global:" >> $mapfile_vers
echo "               JNI_OnLoad;" >> $mapfile_vers
echo "               JNI_OnUnload;" >> $mapfile_vers
echo "               WTFReportBacktrace;" >> $mapfile_vers
echo "               WTFReportAssertionFailure;" >> $mapfile_vers
grep "^JS_EXPORT" -r Source --exclude-from=create_mapfiles.no-files --include *.h -h | sed "s/(/ /g" | sed "s/const//g" | grep -v "extern" | awk -F ' ' '{print "               " $3 ";"}' | sort | uniq >> mapfile.bak 
grep "Java_" -r WebKitBuild --include *.c* -h | sed "s/(/ /g" | awk -F ' ' '{print "               " $4 ";"}' | sort | uniq >> mapfile.bak 
grep "Java_" -r Source --exclude-from=create_mapfiles.no-files --include *.c* -h | sed "s/(/ /g" | awk -F ' ' '{print "               " $4 ";"}' | sort | uniq >> mapfile.bak 
grep "^\s*JS\|^\s*Java" mapfile.bak >> $mapfile_vers
echo "        local:" >> $mapfile_vers
echo "                *;" >> $mapfile_vers
echo "};" >> $mapfile_vers
rm -f mapfile.bak 
echo "done."
