# NOTES: JavaFX Preview Features

## Overview

These are notes regarding the implementation of JavaFX preview features.

## Changes / additions in the jfx.previewfeature branch

The `jfx.previewfeature` branch includes the following:

1. The [JavaFX Preview Features JEP](PREVIEW-FEATURES.md) and these implementation notes.
2. A sample preview feature, consisting of two preview APIs:
   - `javafx.application.MyPreviewFeature`
   - `javafx.application.Platform.getMyPreviewFeature1()`.
   - `javafx.application.Platform.getMyPreviewFeature2()`.

This branch is based on the [previewfeature](https://github.com/mstr2/jfx/tree/feature/previewfeature) branch, which has the changes needed to support preview features. I will create a PR from that branch to implement [JDK-8349373](https://bugs.openjdk.org/browse/JDK-8349373). Those changes include the `com.sun.javafx.PreviewFeature` utility class to produce warnings when first using a preview feature, as well as changes in `PlatformImpl` and `LauncherImpl` to detect the opt-in specified via the `javafx.enablePreview` system property.

Note that this `jfx.previewfeature` branch is meant to be illustrative in nature. It will _never_ be integrated, so the PR based on this branch will remain in Draft state as long as the PR is open (it will _never_ become `rfr`).