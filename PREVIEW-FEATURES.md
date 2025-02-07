# JavaFX Preview Features

## Summary

JavaFX preview features are a means of putting non-final APIs in the hands of developers, while the APIs progress towards either finalization or removal in a future release. This JEP builds on [JEP 12](https://openjdk.org/jeps/12), which defines JDK preview features.

## Goals

Extend the benefits of [JEP 12](https://openjdk.org/jeps/12) to the JavaFX API.

## Motivation

Some JavaFX APIs would benefit from spending a period of time in a JavaFX release prior to being deemed stable. Being in the mainline `jfx` repository, and thus in downstream binaries such as those at jdk.java.net, makes it easier for interested parties outside of the immediate OpenJDK Community to use the new feature. Experience gained and fed back through the usual channels such as blogs, mailing lists, outreach programs, and conferences can then be acted upon before finalizing, or else removing, the feature in a future release.

This is especially useful for complex features with a large API surface. Such features are nearly impossible to get right the first time, even after an extensive review. Using a preview feature will allow the API to evolve in future releases without the strict compatibility constraints that core JavaFX APIs have.

## Description

A preview feature is an API of non-trivial size, that is under development for eventual inclusion in the core set of JavaFX APIs. The API is not yet sufficiently proven, so it is desirable to defer finalization to the next feature release in order to gain additional experience and feedback.

In contrast to incubator modules, preview features affect existing APIs or semantics of the JavaFX platform, and thus cannot be shipped as independent modules.

See [JEP 12](https://openjdk.org/jeps/12) for a description of JDK preview features.

JavaFX preview features have a few differences from JDK preview features:

- A JavaFX preview feature must not be experimental, risky, incomplete, or unstable. However, we accept JavaFX preview features in an earlier state of completion than JDK preview features. For the purpose of comparison, if an experimental feature is considered 25% "done", then a preview
  feature should be at least 90% "done" (in contrast, a JDK preview feature should be at least 95% "done").
- A JavaFX preview feature should usually be completed within one release; that is, a preview feature introduced in JavaFX N should be finalized in JavaFX N+1. This does not preclude features with an exceptionally large API surface, or features that deeply affect the semantics or usage of the JavaFX platform, to preview for additional rounds of feedback or revision.
- JDK preview features are enabled with compiler and launcher flags. Since this is not possible for JavaFX, applications need to specify the `javafx.enablePreview=true` system property to opt into the usage of preview features. The system property should be passed on the command line, and not with a call to `System.setProperty()` since it is read early in the initialization of the JavaFX platform.

## Adding a preview feature

There are several steps that are required to add a new preview feature to JavaFX:

1. Identify all relevant API elements of the feature, and annotate them with `@Deprecated`.
   The `since` element of the annotation must be set to the same value as the corresponding `@since`
   javadoc tag of the API element.
2. Add the following javadoc tag to each of the previously deprecated API elements:<p>
   `@deprecated This is a preview feature which may be changed or removed in a future release.`
3. Add a new constant to the `com.sun.javafx.PreviewFeature` enumeration, and choose a human-readable name
   for the preview feature. This name will be used in warning and error messages when the preview feature
   is used by application developers.
4. Add runtime checks in appropriate places by invoking `com.sun.javafx.PreviewFeature.<FEATURE>.checkEnabled()`.
