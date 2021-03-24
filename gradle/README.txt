For more information on the Gradle dependency verification file
(verification-metadata.xml), see the following page:

Verifying dependencies - Gradle User Guide
https://docs.gradle.org/current/userguide/dependency_verification.html

When upgrading an external dependency to a newer version, update the
dependency verification file as follows:

$ gradle --write-verification-metadata sha256 help

This command will cause Gradle to compute the requested checksums
directly from the newly downloaded artifacts and add them to the file.

Optionally verify that the new checksums added to the file are correct.
The User Guide states, "However, if a dependency is compromised in
a repository, it's likely its checksum will be too, so it's a good
practice to get the checksum from a different place, usually the
website of the library itself."

Edit the file to remove the older dependencies no longer in use and run
the command again to test your changes. For alternative ways to update
the file, see "Cleaning up the verification file" at the link above.
