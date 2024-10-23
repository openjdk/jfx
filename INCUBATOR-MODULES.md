# JavaFX Incubator Modules

## Summary

JavaFX incubator modules are a means of putting non-final APIs in the hands of developers, while the APIs progress towards either finalization or removal in a future release. This JEP builds on [JEP 11](https://openjdk.org/jeps/11), which defines JDK incubator modules.

## Goals

Extends the benefits of [JEP 11](https://openjdk.org/jeps/11) to the JavaFX API.

## Motivation

Some JavaFX APIs would benefit from spending a period of time in a JavaFX release prior to being deemed stable. Being in the mainline `jfx` repository, and thus in downstream binaries such as those at jdk.java.net, makes it easier for interested parties outside of the immediate OpenJDK Community to use the new feature. Experience gained and fed back through the usual channels such as blogs, mailing lists, outreach programs, and conferences can then be acted upon before finalizing, or else removing, the feature in a future release.

This is especially useful for complex features with a large API surface. Such features are nearly impossible to get right the first time, even after an extensive review. Using an incubator module will allow the API to evolve in future releases without the strict compatibility constraints that core JavaFX modules have.

## Description

An incubating feature is an API of non-trivial size, that is under development for eventual inclusion in the core set of JavaFX APIs. The API is not yet sufficiently proven, so it is desirable to defer finalization for a small number of feature releases in order to gain additional experience and feedback.

See  [JEP 11](https://openjdk.org/jeps/11) for a description of incubator modules.

JavaFX incubator modules have a few differences from JDK incubator modules:

- A JavaFX incubator module is identified by the `jfx.incubator.` prefix in its module name.
- A JavaFX incubating API is identified by the `jfx.incubator.` prefix in its exported package names. An incubating API is exported only by an incubator module.
- Incubator modules must export incubating APIs only, i.e., packages in the `jfx.incubator` namespace. Consequently:
    - JavaFX incubator modules must not export core JavaFX APIs in the `javafx.` namespace. This distinguishes incubator modules from core modules such as `javafx.base`.
    - JavaFX non-incubator modules must not specify `requires transitive` dependences upon incubator modules, or otherwise expose types exported from incubator modules in their own exported APIs. In exceptional cases, it may be acceptable for non-incubator modules to specify `requires` dependences (as opposed to `requires transitive`) upon incubator modules.
    - JavaFX incubator modules can specify requires or requires transitive dependences upon other incubator modules.
- A warning must be issued when first loading a class from a publicly exported package in a JavaFX incubator module, even if the module is not jlinked into the JDK. We will provide a utility method in `javafx.base` to facilitate this.
- To either make a JavaFX incubating API final, or to remove it, a new JEP should be submitted, referencing the original incubator JEP.
- By default, a JavaFX feature that is delivered in an incubator module will re-incubate in subsequent versions (the default in the JDK is to drop the feature). If any changes are needed to the API, they will be done with new JBS enhancement along with an associated CSR. However, this is not intended to suggest the possibility of a permanently incubating feature. As with incubating features in the JDK, if an incubating API is not promoted to final status after a reasonably small number of JavaFX feature releases, then it will be dropped: its packages and incubator module will be removed. As a guideline:
    - An incubating API that was not updated in the current shipping feature release and has not been updated in the feature release being developed, is either stable or is not being actively developed. Such an API should either be finalized or dropped.
    - An incubating API that spans beyond a 24-month period (4 feature releases), and is not yet ready to be finalized, will need explicit approval from a Project Lead to remain incubating for some additional period at the discretion of a Project Lead. Otherwise, a Project Lead will submit a removal JEP.
    - The submitter of the original JEP can propose to remove it at any time.

### How to add a new incubator module

Use [this patch](https://github.com/openjdk/jfx/pull/1375.diff) as a starting point for your incubator module. Then do the following:
- Rename `modules/jfx.incubator.myfeature` to the desired name of your module, keeping the `jfx.incubator.` prefix.
- Modify `build.gradle`, `settings.gradle`, and `modules/javafx.base/src/main/java/module-info.java` to update the name of your module. Look for comments of the form `// TODO: incubator template` for where to make the changes.
- Develop your module as you would with any JavaFX module, keeping in mind the rules in this JEP about public exports and dependencies.

FIXME: find a permanent home for the incubator module template patch, possibly in the jfx-sandbox repo.
