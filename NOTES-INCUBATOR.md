# NOTES: JavaFX Incubator Modules

## Overview

These are notes regarding the implementation of JavaFX incubator modules.

## Changes / additions in the jfx.incubator branch

The `jfx.incubator` branch has three main types of changes:

1. The [JavaFX Incubator Modules JEP](INCUBATOR-MODULES.md) and these implementation notes.
2. Changes needed to support incubator modules. These include: build scripts (`build.gradle`, `settings.gradle`), qualified exports from `javafx.base`, and a utility class to produce warnings when first using an incubator module.
3. A sample incubator module, `jfx.incubator.myfeature`.

Note that this branch is meant to be illustrative in nature. It will _never_ be integrated, so the PR based on this branch will remain in Draft state as long as the PR is open (it will _never_ become `rfr`).

## RFE: Support incubator modules

The changes reflected in item 2 in the previous section need to be integrated into mainline ahead of the first incubator module These changes have been separated out into their own branch, `8309381-incubator`, from which I will create a PR. This branch, `jfx.incubator`, is now based on that branch and only has the changes for the `jfx.incubator.myfeature` sample module (item 3) and the JEP documentation (item 1).
