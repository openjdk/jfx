# JavaFX Incubator Modules

## Overview

In [JEP 11](https://openjdk.org/jeps/11), the JDK provides incubator modules as a means of putting non-final API in the hands of developers, while the API progresses towards either finalization or removal in a future release.

Similarly, some JavaFX APIs would benefit from spending a period of time in a JavaFX release prior to being deemed stable. Being in the mainline `jfx` repository, and thus in downstream binaries such as those at jdk.java.net, makes it easier for interested parties outside of the immediate OpenJDK Community to use the new feature. Experience gained and fed back through the usual channels such as blogs, mailing lists, outreach programs, and conferences can then be acted upon before finalizing, or else removing, the feature in a future release.

This is especially useful for complex features with a large API surface. Such features are nearly impossible to get right the first time, even after an extensive review. Using an incubator module will allow the API to evolve in future releases without the strict compatibility constraints that core JavaFX modules have.

## Description

An incubating feature is an API of non-trivial size, that is under development for eventual inclusion in the core set of JavaFX APIs. The API is not yet sufficiently proven, so it is desirable to defer finalization for a small number of feature releases in order to gain additional experience and feedback.

See  [JEP 11](https://openjdk.org/jeps/11) for a description of incubator modules.

JavaFX incubator modules have a few differences from JDK incubator modules:

- A JavaFX incubator module is identified by the `javafx.incubator.` prefix in its module name.
- A JavaFX incubating API is identified by the `javafx.incubator.` prefix in its exported package names. An incubating API is exported only by an incubator module.
- A warning must be issued when first loading a class from a publicly exported package in a JavaFX incubator module, even if the module is not jlinked into the JDK. We will provide a utility method in `javafx.base` to faciliate this.
- By default, a JavaFX feature that is delivered in an incubator module will re-incubate in subsequent versions (the default in the JDK is to drop the feature). If any changes are needed to the API, they will be done with new JBS enhancement along with an associated CSR. However, this is not intended to suggest the possibility of a permantently incubating feature. As with incubating features in the JDK, if an incubating API is not promoted to final status after a reaonably small number of JavaFX feature releases, then it will be dropped: its packages and incubator module will be removed.

## How to add a new incubator modules

In addition to creating the new modules under `modules/javafx.incubator.myfeature`, you need to udpate `build.gradle` and `settings.gradle` to add the new module.

FIXME: finish this
