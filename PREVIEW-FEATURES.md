# Preview features

## Introduction

Preview features are features whose design, specification, and implementation are complete, but which
would benefit from a period of broad exposure and evaluation before either achieving final and permanent
status in JavaFX or else being refined or removed.

A feature should meet two criteria in order to be considered complete:

1. **Readiness**: The feature should be finished in the next release of JavaFX.
   APIs with an exceptionally large surface area, or features that deeply affect the semantics or usage
   of the JavaFX platform, may require additional rounds of feedback and revision.
2. **Stability**: The feature could credibly achieve final and permanent status with no further changes.
   This implies an extremely high degree of confidence in the concepts which underpin the feature, but
   does not completely rule out making syntactic or semantic changes in response to feedback.

The key properties of a preview feature are:

1. **High quality**: A preview feature must display the same level of technical excellence and finesse as
   a final and permanent feature of JavaFX. For example, a preview feature must respect traditional Java
   principles such as readability and compatibility.

2. **Not experimental**: A preview feature must not be experimental, risky, incomplete, or unstable.
   For the purpose of comparison, if an experimental feature is considered 25% "done", then a preview
   feature should be at least 90% "done".

3. **Opt-in:** The use of preview features must be explicitly enabled by application developers by
   setting a system property. The implementation must detect the opt-in, and fail at runtime when the
   application has not opted into the use of preview features. All preview features have equal status
   in any given JavaFX release and can not be enabled individually.

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
