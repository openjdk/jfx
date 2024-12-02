# NOTES: JavaFX Incubator Modules

## Overview

These are notes regarding the implementation of JavaFX incubator modules.

## Changes / additions in the jfx.incubator branch

The `jfx.incubator` branch includes the following:

1. The [JavaFX Incubator Modules JEP](INCUBATOR-MODULES.md) and these implementation notes.
2. A sample incubator module, `jfx.incubator.myfeature`.

This branch is based on the [8309381-incubator](https://github.com/kevinrushforth/jfx/tree/8309381-incubator) branch, which has the changes needed to support incubator modules. I will create a PR from that branch to implement [JDK-8309381](https://bugs.openjdk.org/browse/JDK-8309381). Those changes include: build scripts (`build.gradle`, `settings.gradle`), qualified exports from `javafx.base`, and a utility class to produce warnings when first using an incubator module.

Note that this `jfx.incubator` branch is meant to be illustrative in nature. It will _never_ be integrated, so the PR based on this branch will remain in Draft state as long as the PR is open (it will _never_ become `rfr`).
