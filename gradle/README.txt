For more information on the Gradle dependency verification file
(verification-metadata.xml), see the following page:

Verifying dependencies - Gradle User Guide
https://docs.gradle.org/current/userguide/dependency_verification.html

Recreate the dependency verification file as follows:

1. Remove the existing file on Linux.

   $ rm gradle/verification-metadata.xml

2. Run the following command on Linux.

   $ gradle -PCOMPILE_WEBKIT=true -PBUILD_LIBAV_STUBS=true \
     --write-verification-metadata sha256 help

3. Copy the file on Linux to macOS and run the command again to pick up
   the 'org.eclipse.swt.cocoa.macosx.x86_64' library.

4. Copy the file on macOS to Windows and run the command again to pick
   up the 'org.eclipse.swt.win32.win32.x86_64' library. Convert the
   newline format of the file back to single-character line feeds.

   $ dos2unix gradle/verification-metadata.xml
   dos2unix: converting file gradle/verification-metadata.xml to Unix format...

   $ file gradle/verification-metadata.xml
   gradle/verification-metadata.xml: XML 1.0 document, ASCII text

5. Use the file generated on Linux, macOS, and Windows in the Oracle
   builds to pick up the internal tools and development kits in the
   'javafx' component group.

6. Commit the final version of the file to the repository.

These commands will cause Gradle to compute the requested checksums
directly from the newly downloaded artifacts and add them to the file.

Optionally verify that the new checksums added to the file are correct.
The User Guide states, "However, if a dependency is compromised in
a repository, it's likely its checksum will be too, so it's a good
practice to get the checksum from a different place, usually the
website of the library itself." Even without this extra verification,
having the checksums in the default build allows for a distributed
consensus on their correct values and will report any discrepancies.

When upgrading an external dependency to a newer version, update the
dependency verification file in a similar manner. Edit the file to
remove the older dependencies no longer in use and run the commands
again to test your changes. For alternative ways to update the file,
see "Cleaning up the verification file" at the link above.
