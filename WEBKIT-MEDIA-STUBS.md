# Web Testing

The web project needs a WebKit shared library (`jfxwebkit`) to run tests. The
media libraries it also loads are built from source by this build and must not
be supplied from elsewhere; see the note under **Prebuilt libraries** below.

The WebKit library can be supplied in a number of ways. See sections below.

## Compiled from source

The Maven build in this fork does not compile the WebKit native library from
source (the former Gradle COMPILE_WEBKIT switch). It does compile the Media
libraries: what the Gradle COMPILE_MEDIA switch used to select is now the
default, in the `native-win` / `native-linux` / `native-mac` profiles of
`modules/javafx.media/pom.xml`.

For WebKit there is a GitHub Actions workflow that does it instead:
`.github/workflows/build-webkit.yml` ("Build jfxwebkit"). It is
`workflow_dispatch` only, and drives the WebKit CMake tree through
`modules/javafx.web/src/main/native/Tools/Scripts/build-webkit` on every
supported platform:

| Platform | Runner | Produces |
|---|---|---|
| linux-x64 | `ubuntu-24.04` | `lib/libjfxwebkit.so` |
| linux-aarch64 | `ubuntu-24.04-arm` | `lib/libjfxwebkit.so` |
| macos-x64 | `macos-15-intel` | `lib/libjfxwebkit.dylib` |
| macos-aarch64 | `macos-15` | `lib/libjfxwebkit.dylib` |
| windows-x64 | `windows-2022` | `bin/jfxwebkit.dll` |

Each job verifies that the library exports the `wkj_*` FFM entry points before
publishing, and the run uploads one zip per platform to the repository's
Releases page. Because the archives contain the `bin/` or `lib/` directory
already, they extract straight into `caches/sdk` (see below).

The build takes hours per platform, so run it only when the WebKit native
sources or the FFM ABI change. `ccache` is enabled and cached between runs.

Media needs no such workflow. `mvn install` builds `jfxmedia`,
`gstreamer-lite` and `fxplugins` through CMake into
`modules/javafx.media/target/native/bin`, together with `glib-lite` on Windows
and macOS (Linux links the system GLib instead), `jfxmedia_avf` on macOS, and
`avplugin` on Linux when the system ffmpeg development packages are installed.
`-DskipNative=true` skips that build rather than selecting libraries from
anywhere else. None of the options below apply to Media.


## Prebuilt libraries

> **The Media libraries are now built from source, and a prebuilt `jfxmedia`
> from an older OpenJFX SDK no longer works.** On the `ffm/media` branch
> `javafx.media` calls a plain C ABI (`jfxm_*`) instead of JNI, and
> `modules/javafx.media/pom.xml` builds `jfxmedia`, `gstreamer-lite` and
> `fxplugins` — plus `glib-lite` on Windows and macOS, where the bundled GLib
> subset is used instead of the system one — through CMake like the graphics
> natives (see `modules/javafx.media/FFM-BUILD-PLAN.md`); `-DskipNative=true`
> skips that. A JNI-era `jfxmedia` exports `Java_*` entry points but none of
> the `jfxm_*` symbols, so loading one fails with
> `UnsatisfiedLinkError: missing native symbol: jfxm_abi_version` and the media
> stack reports itself unavailable. Delete any stale `jfxmedia*`,
> `gstreamer-lite*`, `glib-lite*`, `fxplugins*` and `avplugin*` from
> `../caches/sdk/{bin,lib}` rather than letting them shadow the freshly built
> ones — the root pom puts `modules/javafx.media/target/native/bin` first on
> `java.library.path`, but the cache directories are still on it. This note
> does not apply to `jfxwebkit`, which is still supplied prebuilt.

You can manually place the WebKit shared library (`jfxwebkit.dll`,
`libjfxwebkit.so` or `libjfxwebkit.dylib`) in the directory the build already
uses as `java.library.path`:

````
    modules/javafx.graphics/target/native/bin
````

This is the same directory the javafx.graphics native build writes to, and the
one passed as `-Djava.library.path` by both the web module tests
(`modules/javafx.web/pom.xml`) and the system tests (`tests/system/pom.xml`).
The SDK assembly also copies every shared library found there into
`sdk/target/sdk`, so libraries dropped in before `mvn install` end up in the
assembled SDK as well.

The web module loads `jfxwebkit`; the media module loads `jfxmedia` together
with its platform dependencies — `gstreamer-lite` and `fxplugins` on every
platform, `glib-lite` on Windows and macOS, `jfxmedia_avf` on macOS, and
`avplugin` on Linux when it was built — which its own native build has already
written to `modules/javafx.media/target/native/bin`.

The Maven build also puts `../caches/sdk/bin` and `../caches/sdk/lib`
(relative to the repository root) on `java.library.path` for the `javafx.web`
unit tests and the `tests/system` test and worker JVMs (property
`jfx.native.librarypath` in the root pom), so cached libraries are picked up
automatically on the next test run.

## Officially released libraries

Use the Release zip that `.github/workflows/build-webkit.yml` published for
your platform, extracted into `caches/sdk` next to the repository as described
above, or run that workflow on this revision to build one and extract its
artifact the same way. The workflow's windows-x64 job has not published a zip
yet: the fix to that job is unproven until the workflow next runs, so on
Windows running it is the way to get a library today. An officially released
`jfxwebkit` (the `javafx-web` artifact on Maven Central, or the one in an
OpenJFX SDK) does **not** work: it is a JNI build that exports `Java_*` entry
points and none of the `wkj_*` symbols this fork binds, so `WebKitNative`
rejects it with an `UnsatisfiedLinkError` saying it
does not export `wkj_abi_version`. Do **not** take `jfxmedia` from a released
`javafx-media` either: every released `jfxmedia` is JNI-era, so it leaves the
media stack reporting itself unavailable as described above.

## ABI guard and FFM binding tests

`javafx.web` calls `jfxwebkit` through the plain C `wkj_*` ABI, versioned by
`WKJ_ABI_VERSION` (`webkit_java_api.h` and `WebKitNative`, both 1 today).
`WebKitNative` loads the library once, refuses it unless `wkj_abi_version()`
returns that version, and only then installs its callback table with
`wkj_init`. `WebKitLibraryAbiTest` runs with `-Djfx.web.skipTests=false` and
fails in one sentence when the library on `java.library.path` is not
ABI-compatible. The FFM binding tests need no WebKit build at all:
`-Djfx.web.skipFfmTests=false` builds `wkjstub`, a recording stub generated at
build time from the `webkit_java_api*.h` headers
(`modules/javafx.web/src/test/native/wkjstub`, which needs CMake and a C
toolchain), and runs the `ffm`-tagged tests against it; CI does this on every
platform. They select the stub through `-Djavafx.web.nativeLibrary=wkjstub`,
the system property that makes `WebKitNative` load a library other than
`jfxwebkit`.


## Skip Web tests

The web module tests and the WebKit-dependent Robot tests in `tests/system` are excluded by
default (`jfx.web.skipTests=true`). To run the web module tests, pass:

    -Djfx.web.skipTests=false

To run only the WebKit-dependent Robot tests, use:

    mvn -pl tests/system test -DFULL_TEST=true -DUSE_ROBOT=true -Djfx.web.skipTests=false -Dsurefire.includes='test/robot/javafx/web/**/*.java'

Setting `jfx.web.skipTests=false` requires an ABI-compatible `jfxwebkit` rebuilt for the current
source tree. A released binary from another JavaFX revision may load but still have an incompatible
native ABI.

Note that skipping is fine for local work. But a full test *is* required before submitting a PR, see [CONTRIBUTING.md](https://github.com/openjdk/jfx/blob/master/CONTRIBUTING.md).
