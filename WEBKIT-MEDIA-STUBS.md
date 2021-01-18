# Web Testing

The web project needs WebKit and Media shared libraries to run tests.

These can be supplied in a number of ways. See sections below.

## Compiled from source

Specify these Gradle properties to enable building of WebKit and Media libraries from source:

    -PCOMPILE_WEBKIT=true -PCOMPILE_MEDIA=true

Note that these require additional build tooling and take some time to build.

If you are not actively working on these sources, you may want to cache the output by copying it to one of the folders mentioned below.


## Cached libraries

You can manually place WebKit and Media shared libraries in these folders:

* Unix libraries (*.so or *.dylib files)
````
    $projectDir/../caches/sdk/lib
````

* Windows libraries (*.dll files)
````
    $projectDir/../caches/sdk/bin
````

## Officially released libraries

Gradle has a task to automate downloading officially released libraries from MavenCentral.

You can enable the task by specifying this Gradle property:

    -PSTUB_RUNTIME_OPENJFX="15-ea+4"

Note that these libraries may not be compatible with the source tree you are working with. Always use the [latest version](https://search.maven.org/search?q=g:org.openjfx%20AND%20a:javafx); this may improve your chances of compatibility.


## Skip Web tests

You can also skip the web module tests.

Specify these options to Gradle

    -x :web:test

Note that this is fine for local work. But a full test *is* required before submitting a PR, see [CONTRIBUTING.md](https://github.com/openjdk/jfx/blob/master/CONTRIBUTING.md).
