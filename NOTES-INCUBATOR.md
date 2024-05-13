# NOTES: JavaFX Incubator Modules

## Overview

These are notes regarding the implementation of JavaFX incubator modules.

## Changes / additions in the javafx.incubator branch

The `javafx.incubator` branch has three main types of changes:

1. The [JavaFX Incubator Modules JEP](INCUBATOR-MODULES.md) and these implementation notes.
2. Changes needed to support incubator modules. These include: build scripts (`build.gradle`, `settings.gradle`), qualified exports from `javafx.base`, and a utility class to produce warnings when first using an incubator module.
3. A sample incubator module, `jfx.incubator.myfeature`.

Note that this branch is meant to be illustrative in nature. It will _never_ be integrated, so the PR based on this branch will remain in Draft state as long as the PR is open (it will _never_ become `rfr`).

## RFE: Support incubator modules

The changes reflected in item 2 in the previous section need to be integrated into mainline ahead of the first incubator module, so a separate PR will be made from a branch that has just those changes. In support of this, the needed changes have been highlighted using comments of the following pattern:

```
// RFE: incubator dependency
```

These identify the blocks that are needed as part of the RFE to add the needed dependencies. The PR that includes them will remove the `RFE: incubator dependency` line, since that is only there to differentiate those changes needed to support incubator modules in general from those changes that add the specific sample incubator module (`jfx.incubator.myfeature`). The sample incubator module changes must be reverted in the branch that will be used to add the dependencies.

Some of the incubator dependency changes include comments about where to add build logic when creating an incubator module. Those comment will be part of the incubator dependency PR. For example:

```
        // RFE: incubator dependency
        // TODO: incubator: Add entry for each incubator module here
        // BEGIN: incubator placeholder
        //'incubator.mymod',
        // END: incubator placeholder
```

When proposing the incubator dependency PR, the `// RFE: incubator dependency` line will be removed (since it only exists to help identify the needed dependency), but the rest of the comment block (starting with `TODO: incubator: ...`) will be part of the PR.
