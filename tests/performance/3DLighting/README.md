## 3DLighting Application

See the main class `LightingApplication` for usage instructions.

The project uses the main Gradle wrapper, so Gradle needs to be invoked from the root dir. For example:
* If you're in the project dir, run `..\..\..\gradlew <task>`.
* If you're in the root dir, run `gradlew -p tests\performance\3DLighting <task>`.

### Build
The application depends on the JavaFX sdk. Build it with `gradlew sdk` (from the root). Any changes to the native files
will require rerunning the sdk build.

If you intend to run the jar using the CLI, create it with this project's `jar` task. It will be created under
`build/libs` in this project. If you run through the IDE or Gradle, they take care of it for you.

### Run
**IDE**

Run `app.LightingApplication` with the following VM options (which are pre-configured in `build.gradle`):
```properties
-Djava.library.path=..\..\..\modules\javafx.graphics\build\module-lib
--add-modules=javafx.controls,javafx.swing
-Djavafx.animation.fullspeed=true
```

**Gradle**

Run with this project's `run` task.
