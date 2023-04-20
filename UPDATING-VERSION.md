# Updating the JavaFX Release Version

Here are the instructions for updating the JavaFX release version number
for a feature release or security (dot-dot) release.
See [JDK-8226365](https://bugs.openjdk.org/browse/JDK-8226365)
for a recent example.

## Incrementing the feature version

Here are the steps to increment the JavaFX release version number to a new
feature version (for example, from 13 to 14).

* In `.jcheck/conf`, modify the `version` property in the `[general]`
section to increment the JBS version number from `jfx$N` to `jfx$N+1`.

* In `build.properties`, modify the following properties to increment the
feature version number from `N` to `N+1`:

```
    jfx.release.major.version
    javadoc.title
    javadoc.header
```

* In
`modules/javafx.base/src/test/java/test/com/sun/javafx/runtime/VersionInfoTest.java`,
modify the testMajorVersion method to increment the feature version number
from `N` to `N+1`.

## Incrementing the security version

Here are the steps to increment the JavaFX release version number to a new
security version (for example, from 13 to 13.0.1).

* In `.jcheck/conf`, modify the `version` property in the `[general]`
section to increment the JBS version number from `jfx$N` to `jfx$N.0.1`
or from `jfx$N.0.M` to `jfx$N.0.$M+1`.

* In `build.properties`, modify the `jfx.release.security.version` property
to increment the security version number from `M` to `M+1`.
