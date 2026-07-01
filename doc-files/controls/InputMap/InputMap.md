# Public InputMap

Andy Goryachev

30 June 2026



## Summary

Adds the `InputMap` property to the `Control` class, allowing the application code to customize controls
by adding, removing, or modifying the key mappings, at compile time or dynamically.

Adds the `SkinInputMap` class and an optional `BehaviorBase` class to simplify development of skins for the existing
and custom controls.

In the absence of a generic solution to the priority inversion problem [7],
provides a mechanism for guaranteed order
of the event handlers and key mappings between the application and the skin.



## Goals

The goals of this proposal are:

- guarantee the priority of the application handlers and key mappings over those defined by the skin
- enable minor changes to the behavior by altering existing mappings or adding new mappings
- support dynamic modification of key mappings (for example, via user preferences)
- allow for calling the default implementation even when it has been overwritten by the application
- allow for *gradual* migration of the existing controls to use the new `InputMap`
- support stateful and stateless (fully static) behavior implementations



## Non-Goals

It is not the goal of this proposal:

- to require a specific base class for the behavior implementations
- to allow for complete decoupling of the skin from the behavior
- to make the legacy internal `InputMap` class public
- to require making the behavior implementations public



## Motivation

Historically, going outside of what is provided by JavaFX core, such as customizing an existing component or creating a new one, has always been an arduous endeavor due to the rather opaque nature of JavaFX.  Neither `BehaviorBase`, `InputMap`, nor `KeyBinding` classes used by the skins are public; the controls and their skins lack public APIs suitable for behavior customization.  Even a simple customization, such as remapping or adding a key binding, is nearly impossible.

Another problem encountered by the developers is undetermined order of event dispatching to the handlers added by the application and by the skin: since the order of invocation is determined by the order an event handler is added to the control, the default skin always has priority (which many consider to be incorrect).  The order reverses after the skin is replaced, which is unexpected,
as described in [7].

The proposed solution will benefit:

- application developers, by providing a mechanism for controlling the key bindings
- custom control developers, by providing a clear integration point and a convenient public API
- skin developers, by simplifying the registration of event handlers and key mappings provided by the skin



## Description

A new property, represented by the `InputMap` class, is added to the `Control` class.
The `InputMap` provides prioritized storage of the event handlers and the key mappings, registered by the application and the skin.

Before we delve into more details, it might help to clarify the roles of parts that constitute a Control.



### JavaFX Model-View-Controller Paradigm

JavaFX controls are supposed to follow the classic
[Model-View-Controller](https://wiki.openjdk.org/display/OpenJFX/UI+Controls+Architecture)
(MVC) paradigm [0].

In reality, JavaFX does not exhibit a strict adherence to the MVC pattern, for example,
`Control` represents both the model (by hosting various properties) and a part of the View
(by being a `Region` and a part of the scene graph).

To make the separation of concerns clearer, we propose a clear separation of responsibilities between the
constituent parts.



#### Role of the Control

We propose to think of the `Control` as a public _façade_, which exposes the properties and the methods
through which the application interacts with the control.

`Control` is also a `Region`, and therefore is a part of the scene graph and participates in the event handling.
We can think of this `Region` as a container that holds the `Skin`.


#### Role of the Skin

The `Skin` defines the visual representation of the control by providing the `Nodes` representing various surfaces
that render the control content and accept user input.
Note that it is possible to have skins that
provide a completely different set of control surfaces, for example a traditional 2D control may have totally
different look and feel in a virtual 3D world.

The skin adds listeners to the `Control` properties and to the events directed at the `Control` or its input surfaces.
These listeners process the input events and effect a change on the `Control` state by updating its properties,
which in turn result in the corresponding changes in the visuals.

While it is possible to make all the listeners a part of the `Skin`, they are typically organized in a separate
class known as "behavior".


#### Role of the Behavior

Behavior is an internal implementation detail, created by the `Skin`, for the purpose of handling the user input by converting it to updates of the Control's properties, either directly or via APIs published by the `Control`.

Most of the behavior implementations maintain some kind of state required to properly interpret the user input.
For example, mouse events are processed differently depending on whether the `shift` key was pressed or not.

The behavior part of the `Skin` is tied tightly to the visual surfaces defined by it.  Depending on the type of the
`Control`, the behavior may or may not be exposed via a public API.  This proposal does not require the behavior
implementation to be public.  However, it is possible to offer a limited flexibility to customize behavior
without making the behavior a public API, as long as the `Control` provides the properties and public methods that
allow such customization.

For example, a text editor may want to redefine the behavior of the "next word" / "previous word" key mappings,
navigating either to the next whole word (that is, surrounded by whitespace or punctuation), or next / previous part
of a *CamelCase* identifier.  The application requirements might call for such customization to be dependent on
the user preference (loaded at startup), or even on the context.

While it is possible to provide such a customization via event handlers and event filters, it will be much easier 
to use the proposed `InputMap`.


#### Role of the InputMap

The `InputMap` provides a prioritized repository of event handlers and key mappings registered by the application
and the skin, guaranteeing the order in which handlers and mappings are invoked, regardless of when the skin
was created or replaced. 

The `InputMap` provides additional separation between the key mappings and the functions associated with the said
mappings.  This enables the application to map these two aspects separately.  For example, a new function
(like the "next word" described earlier) can be mapped to an existing (platform-specific) mapping, or, alternatively,
a new key can be mapped to an existing function, for instance, mapping `ctrl-D` to the "delete paragraph" function. 

This customization mechanism has been prototyped and validated by the `RichTextArea` incubating module [3], [4].



#### Summary

This table summarizes the role of each part:

|Entity    |Role       |Description      |
|:---------|:----------|-----------------|
|Control   |Model      |A class that provides various properties that map into the "Model" in the MVC pattern.
|Skin      |View       |The skin provides a visual representation of the control, serving a role of "View" in the MVC pattern.
|Behavior  |Controller |The behavior reacts to the user input and updates the "Model" by modifying the control's properties and/or calling public methods in the control.
|Input Map |           |Serves as an integration point between Control and Skin/Behavior by providing a repository for the event handlers and the key mappings.  Guarantees the order in which events are being dispatched to the registered event handlers.





### Examples

The following examples illustrate common real-world scenarios made possible by the **InputMap**.



#### Using the InputMap by the Application

An application can use the InputMap feature to:

- map a key to a custom function
- dynamically redefine an existing function while keeping existing key bindings
- map a new key to an existing function
- invoke the default function after it has been redefined


##### Map a Key to a Custom Function

Application requirements call for a special key binding to bring up an auto-completion popup.

The following code sample uses the `InputMap.registerKey()` method to map the new key to the new function,
either at compile time or dynamically:

```java
    // builds and shows the auto-completion popup
    void showAutoCompletionPopup() { /* ... */ }

    // map shortcut-SPACE to show auto completion popup
    control.getInputMap().registerKey(KeyBinding.shortcut(KeyCode.SPACE), () -> {
        showAutoCompletionPopup();
    });
    // or
    control.getInputMap().registerKey(KeyBinding.shortcut(KeyCode.SPACE), this::showAutoCompletionPopup);
```

This mechanism provides a simpler way for an application to add a key mapping to a `Control` as compared with
adding a key event handler.  The application mapping takes precedence over any existing mappings created by the skin.


##### Redefine an Existing Function While Keeping Existing Key Bindings

Application requirements call for an existing `shortcut-C` binding to copy the text content in a specific format,
rather than that supported by the control.

The following code sample uses the `InputMap.registerFunction()` method to change the behavior without
altering the existing key bindings, and without subclassing:

```java
    // provides alternative 'copy' implementation
    private void customCopy() { /* ... */ }

    // remap existing 'copy' keys to the new function
    getInputMap().registerFunction(Tag.COPY, this::customCopy);
```

##### Map a New Key to an Existing Function

Application requirements call for a new `shortcut-D` key binding to delete the current paragraph in a text component.

Use the `InputMap.registerKey()` method to map the new key to the existing, but unmapped, function:

```java
    // shortcut-D deletes the current paragraph
    control.getInputMap().registerKey(KeyBinding.shortcut(KeyCode.D), Tag.DELETE_PARAGRAPH);
```


##### Invoke the Default Function

The `InputMap` allows to change the behavior of the methods which a particular control made public
via the corresponding function tag.  In some cases, it may be useful for the default implementation to be made
available.

For example, application requirements might call for the `copy()` method to copy the text control in a special format,
unless some runtime flag overrides that and the default implementation of `copy()` should be invoked.

The `Control.executeDefault()` method provides such functionality:

```java
    //  run the default function
    control.executeDefault(Tag.COPY);
```



### Organization

Most of the new classes are in the new package `javafx.scene.control.input`:

- `BehaviorBase`
- `EventCriteria`
- `FunctionTag`
- `InputMap`
- `KeyBinding`
- `SkinInputMap`

The API surface of the proposed change is fairly large; please refer to the pull request [2] for more detail.



### InputMap

The purpose of this class is to store event handlers and key mappings in order to facilitate the following operations:

- map a key binding to a function, provided either by the application or the skin
- unmap a key binding
- map a new function to an existing key binding
- obtain the default function
- add an event handler at a specific priority (applies to application-defined and skin-defined handlers)
- ensure that the application key mappings take priority over mappings created by the skin

The `InputMap` provides an ordered repository of event handlers, working together with `SkinInputMap` supplied by the skin (or static stateless behavior implementation).  Internally, each handler is added with a specific priority according to this table:

|Priority   |Set By      |Method                             |Description   |
|:----------|:-----------|:----------------------------------|:-------------|
|Highest    |Application |InputMap.addHandler()              |Event handlers set by the application 
|           |Application |InputMap.registerKey()             |Key mappings set by the application   
|           |Skin        |SkinInputMap.registerKey()         |Key mappings set by the skin
|Lowest     |Skin        |SkinInputMap.addHandler()          |Event handlers set by the skin    

For key mappings, the InputMap utilizes a two-stage lookup.  First, the key event is matched to a `FunctionTag` which identifies a function provided either by the skin or the associated behavior (the "default" function), or by the application.  When such a mapping exists, the found function tag is matched to a function registered either by the application or by the skin.  This mechanism allows for customizing the key mappings and the underlying functions independently and separately.

An added benefit of such an independent customization is to enable limited customization of the control behavior without subclassing either the skin or the associated behavior classes.

The InputMap also supports dynamic (that is, at runtime) key mapping customization.

The InputMap class provides the following public methods:

- public void **addHandler**(EventType, EventHandler)
- public Set<KeyBinding> **getKeyBindings**()
- public Set<KeyBinding> **getKeyBindingsFor**(FunctionTag);
- public void **register**(KeyBinding, Runnable)
- public void **registerFunction**(FunctionTag, Runnable)
- public void **registerKey**(KeyBinding, FunctionTag)
- public void **removeHandler**(EventType, EventHandler)
- public void **resetKeyBindings**()
- public void **restoreDefaultFunction**(FunctionTag)
- public void **restoreDefaultKeyBinding**(KeyBinding)
- public void **setSkinInputMap**(SkinInputMap)
- public void **removeKeyBindingsFor**(FunctionTag)
- public void **disableKeyBinding**(KeyBinding)



### FunctionTag

A function tag is a public identifier of a method that can be mapped to a key binding.

The following example is taken from `TabPane`:

```java
    public class TabPane extends Control {
        /** Identifiers for methods available for customization via the InputMap. */
        public static final class Tag {
            /** Selects the first tab. */
            public static final FunctionTag SELECT_FIRST_TAB = new FunctionTag();
            /** Selects the last tab. */
            public static final FunctionTag SELECT_LAST_TAB = new FunctionTag();
            /** Selects the left tab: previous in LTR mode, next in RTL mode. */
            public static final FunctionTag SELECT_LEFT_TAB = new FunctionTag();
            /** Selects the next tab. */
            public static final FunctionTag SELECT_NEXT_TAB = new FunctionTag();
            /** Selects the previous tab. */
            public static final FunctionTag SELECT_PREV_TAB = new FunctionTag();
            /** Selects the right tab: next in LTR mode, previous in RTL mode. */
            public static final FunctionTag SELECT_RIGHT_TAB = new FunctionTag();
        }
```

Note: alternatively, the `FunctionTag` can be replaced by a String at the expense of type safety.


### Control

Three new methods are added to the `Control` class:

- public InputMap **getInputMap**()
- public void **execute**(FunctionTag)
- public void **executeDefault**(FunctionTag)

The use of the `InputMap` allows the `Control` to provide public methods for some or all function tags.  These methods allow the application to customize some aspects of behavior without making changes to the public APIs or subclassing.

The following example illustrates a `copy()` method declared at the control level, which delegates to the function provided by the behavior or the application:

```java
    public void copy() {
        execute(Tags.COPY);
    }
```



### KeyBinding

This immutable class represents either a key pressed, a key typed, or a key released, with zero or more modifiers such as `Ctrl`, `Shift`, `Alt`, or `Meta`.  This class is suitable to be put into a map to be matched against a `KeyEvent` by the `InputMap`.

Most `KeyBindings`, corresponding to a `KEY_PRESSED` event, can be constructed with one of the convenient factory methods:

- public static KeyBinding **alt**(KeyCode)
- public static KeyBinding **command**(KeyCode)
- public static KeyBinding **ctrl**(KeyCode)
- public static KeyBinding **ctrlShift**(KeyCode)
- public static KeyBinding **of**(KeyCode)
- public static KeyBinding **option**(KeyCode)
- public static KeyBinding **shift**(KeyCode)
- public static KeyBinding **shiftShortcut**(KeyCode)
- public static KeyBinding **shortcut**(KeyCode)

For more complex modifier combinations, or when the key binding corresponds to a `KEY_TYPED` or `KEY_RELEASED` event, a builder pattern should be used.  

The Builder can be obtained with either of the two methods:

- public static KeyBinding.Builder **builder**(KeyCode)
- public static KeyBinding.Builder **builder**(String character)

The **KeyBinding.Builder** class provides the following methods:

- public Builder **alt**()
- public Builder **alt**(boolean)
- public KeyBinding **build**()
- public Builder **command**()
- public Builder **command**(boolean)
- public Builder **ctrl**()
- public Builder **ctrl**(boolean)
- public Builder **meta**()
- public Builder **meta**(boolean)
- public Builder **onKeyReleased**()
- public Builder **onKeyTyped**()
- public Builder **option**()
- public Builder **option**(boolean)
- public Builder **shift**()
- public Builder **shift**(boolean)
- public Builder **shortcut**()
- public Builder **shortcut**(boolean)

The following are the public instance methods of the KeyBinding class:

- public boolean **isEventAcceptable**(KeyEvent)
- public boolean **isKeyPressed**()
- public boolean **isKeyReleased**()
- public boolean **isKeyTyped**()

Lastly, sometimes it is useful to create a copy of a `KeyBinding` with a different key code and the same set of
modifier keys, in which case there is a utility method:

- public KeyBinding **withNewKeyCode**(KeyCode)



### SkinInputMap

This class provides a secondary repository for the event handlers and key mappings created by the skin.
The skin constructs an instance of this class and then registers it with the control by calling
`InputMap.setSkinInputMap()` inside `Skin.install()`.

Most skins create stateful behavior implementaions, see the
[Control Class Hierarchy](https://github.com/andy-goryachev-oracle/Test/blob/main/doc/Controls/ControlsClassHierarchy.md)
[1].  
Most frequently used skin input map is therefore SkinInputMap.Stateful, which can be obtained by calling `SkinInputMap.create()`.

For skins with stateless behaviors, a single instance of SkinInputMap.Stateless can be used, obtained via `SkinInputMap.createStateless()`.

The base SkinInputMap class provides the following public methods:

- public static SkinInputMap.Stateful **create**()
- public static <C extends Control> SkinInputMap.Stateless<C> **createStateless**()

- public void **addHandler**(EventCriteria, boolean consume, EventHandler)
- public void **addHandler**(EventType, boolean consume, EventHandler)
- public void **duplicateMapping**(KeyBinding, KeyBinding)
- public Set<KeyBinding> **getKeyBindings**()
- public Set<KeyBinding> **getKeyBindingsFor**(FunctionTag)
- public void **registerKey**(KeyBinding, FunctionTag)
- public void **registerKey**(KeyCode, FunctionTag)

A Stateful variant adds the following methods:

- public void **register**(FunctionTag, KeyBinding, Runnable)
- public void **register**(FunctionTag, KeyCode, Runnable)
- public void **registerFunction**(FunctionTag, BooleanSupplier)
- public void **registerFunction**(FunctionTag, Runnable)

A Stateless variant adds the following methods, which use interfaces FHandler<C> and FHandlerConditional<C>
intended to pass the reference to the source Control to the handling code:

- public void **register**(FunctionTag, KeyBinding, FHandler<C>)
- public void **register**(FunctionTag, KeyCode, FHandler<C>)
- public void **registerFunction**(FunctionTag, FHandler<C>)
- public void **registerFunction**(FunctionTag, FHandlerConditional<C>)



### BehaviorBase

This convenience class is intended to simplify creation of stateful behaviors, by maintaining an instance of `SkinInputMap` and adding helpful methods for registering key mappings and event handlers.  It enables easy integration of the default functionality into its owning `Skin` and its `install()` method:

```java
    @Override
    public void install() {
        super.install();
        setSkinInputMap(behavior.getSkinInputMap());
    }
```

BehaviorBase provides the following public method:

- public SkinInputMap<C> **getSkinInputMap**()

It also provides a number of protected methods intended to be called by the behavior implementation in `BehaviorBase.getSkinInputMap()`:

- protected final void **addHandler**(EventCriteria, boolean consume, EventHandler)
- protected final void **addHandler**(EventType, boolean consume, EventHandler)
- protected final void **duplicateMapping**(KeyBinding, KeyBinding)
- protected final C **getControl**()
- protected final boolean **isLinux**()
- protected final boolean **isMac**()
- protected final boolean **isWindows**()
- protected void **populateSkinInputMap**()
- protected final void **register**(FunctionTag, KeyBinding, Runnable)
- protected final void **register**(FunctionTag, KeyCode, Runnable)
- protected final void **registerFunction**(FunctionTag, Runnable)
- protected final void **registerKey**(KeyBinding, FunctionTag)
- protected final void **registerKey**(KeyCode, FunctionTag)
- protected final void **traverseDown**()
- protected final void **traverseLeft**()
- protected final void **traverseNext**()
- protected final void **traversePrevious**()
- protected final void **traverseRight**()
- protected final void **traverseUp**()



#### Stateless (Static) Behaviors

A number of Controls have behavior classes that require no state: examples are `DateCell`, `TabPane`, and a few more [1].  For these situations, a single static `SkinInputMap` instance might be sufficient, eliminating the need for per-instance behavior objects.

This example illustrates the use of a static behavior in the context of `TabPaneSkin`:

```java
    @Override
    public void install() {
        super.install();
        // install stateless behavior
        TabPaneBehavior.install(getSkinnable());
    }

```

The stateless behavior is implemented in the `TabPaneBehavior` (here for illustration purposes only, as it is not part of the public API):

```java
    public class TabPaneBehavior {
        private static final SkinInputMap.Stateless<TabPane> inputMap = createInputMap();
    
        private static SkinInputMap.Stateless<TabPane> createInputMap() {
            SkinInputMap.Stateless<TabPane> m = SkinInputMap.createStateless();
            // register functions
            m.registerFunction(...);
            // register key bindings
            m.registerKey(...);
            // add mouse handler
            m.addHandler(...);
            return m;
        }
    
        public static void install(TabPane control) {
            control.getInputMap().setSkinInputMap(inputMap);
        }
```



#### Checklist For Custom Controls

In order to fully utilize the capability of the `InputMap`, a new `Control` should:

- declare a set of FunctionTags corresponding to the customizable methods
- optionally provide public methods in the new control which delegate to the corresponding FunctionTags using `Control.execute()`
- implement the behavior either in the skin class, or a class created by the skin that extends BehaviorBase, or by a class that provides a stateless behavior
- populate `SkinInputMap` with the default key mappings and the event handlers



## Alternatives

This proposal solves several problems with one elegant *[citation needed]* solution.

The only alternative that currently exists is to employ event filters, which would guarantee the listeners registered
by the application will be invoked first.  This solution, however, does not completely solve the problem for the skin
developers.

Another alternative proposed earlier [8] is to introduce an optional priority for event handler invocations.
I believe this idea suffers from two possible problems: firstly, it changes the `EventTarget` interface, affecting
other areas where we don't have contention between two actors (the application and the skin), and secondly, it is
open for misuse because the application can use the new interface to register a listener at the level allocated
to the skin, `EventHandlerPriority.SYSTEM`, effectively re-introducing the same problem again.



## Testing

A standard set of unit tests for all the new classes should be developed.

Development of a comprehensive behavior test suite (both headless and headful, [6]) might be required
in order to minimize the regression risk while switching to the new `InputMap`.




## Risks and Assumptions

Adding a public method to the `Control` class might create incompatibility where application developers also added a method with the same name.

Migration of the existing core Controls to the new BehaviorBase carries a regression risk because:

- no test suite currently exists that exhaustively exercises every key mapping on every platform
- no headful tests currently exist that go beyond simple features and try to test the system as a whole, complete with focus traversal, popups, etc.
- extensive manual testing might be needed



## Dependencies

None.



## References

- [0] JavaFX Model-View-Controller (MVC) https://wiki.openjdk.org/display/OpenJFX/UI+Controls+Architecture
- [1] Control Class Hierarchy https://github.com/andy-goryachev-oracle/Test/blob/main/doc/Controls/ControlsClassHierarchy.md
- [2] [8314968: Public InputMap (v3)](https://github.com/openjdk/jfx/pull/1495)
- [3] [RichTextArea Control (Incubator)](https://bugs.openjdk.org/browse/JDK-8301121)
- [4] [Public InputMap (Incubator)](https://bugs.openjdk.org/browse/JDK-8343646)
- [5] [JDK-8314968](https://bugs.openjdk.org/browse/JDK-8314968) Public InputMap
- [6] [JDK-8326869](https://bugs.openjdk.org/browse/JDK-8326869) ☂ Develop Behavior Test Suite
- [7] [JDK-8231245](https://bugs.openjdk.org/browse/JDK-8231245) Controls' behavior must not depend on sequence of handler registration
- [8] [Prioritized event handlers #1266](https://github.com/openjdk/jfx/pull/1266)
