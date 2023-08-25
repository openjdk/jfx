# Behavior/InputMap Redesign Proposal

Andy Goryachev <andy.goryachev@oracle.com>


## Summary

Currently, it is nearly impossible to customize or extend FX control behavior, as neither BehaviorBase nor InputMap are public.

The current non-public implementation does not allow for an easy customization by the user, such as changing the key bindings, remapping an existing key binding to a different function, customizing the function mapped to a key binding, or reverting such mappings to the initial state at run time.



## Goals

The goal of this proposal is to enable customization of stock Controls as well as to simplify implementation of default skin/behavior.



## Non-Goals

It is not the goal of this proposal to require behaviors for all existing Controls be made public at the same time since a gradual transition is possible.
Neither does this proposal introduce or require an equivalent of Swing Actions.



## Motivation

The main motivation behind this proposal is to provide a mechanism for extending functionality of FX Controls and offering a greater flexibility in working with the key bindings.

Several JBS tickets ask for making the behavior and related classes public and other related functionality:
- [JDK-8091189](https://bugs.openjdk.org/browse/JDK-8091189) Move BehaviorBase into public API
- [JDK-8092211](https://bugs.openjdk.org/browse/JDK-8092211) Promote all skin and behavior classes of the default controls to the public API
- [JDK-8186137](https://bugs.openjdk.org/browse/JDK-8186137) [JavaFX 9] TextFieldSkin, MenuButtonSkinBase - behavior can't be passed

In addition to making the behavior-related classes public, this proposal enables a wide range of operations with key mappings, including runtime remapping and reverting to the default behavior.



## Description

The first public API being introduced is **InputMap**, via Control.getInputMap().  An InputMap maps the user input events to methods in the control's behavior class or methods defined by the user.

The purpose of InputMap is to enable a wide range of operations performed by both the skin and the user:
- map a key binding to a function, either default one or supplied by the user
- un-map a key binding
- map a new function to an existing key binding
- obtain the default behavior function
- ensure that user-defined mappings overwrite default ones and survive a skin change

To achieve that, the InputMap utilizes a two-stage lookup.  First, the key binding (or input even in general) is mapped to a **FunctionTag** - a method identifier declared by the corresponding Control.  Then, if such a mapping exists, the actual function (a Runnable) is obtained and executed.  This approach allows to customize the key bindings separately from customizing the behavior.

InputMap provides the following public methods:
- void **regKey**(KeyBinding, FunctionTag)
- void **regFunc**(FunctionTag, Runnable)
- void **addAlias**(KeyBinding, KeyBinding)
- Runnable **getFunction**(FunctionTag)
- Runnable **getFunction**(KeyBinding)
- Runnable **getDefaultFunction**(FunctionTag)
- Runnable **getDefaultFunction**(KeyBinding)
- Set<KeyBinding> **getKeyBindings**()
- void **resetKeyBindings**()
- void **restoreDefaultKeyBinding**(KeyBinding)
- void **restoreDefaultFunction**(FunctionTag)
- void **unbind**(KeyBinding)
- void **unregister**(BehaviorBase)

All behavior must extend the **BaseBehavior** class.  It is expected that behavior classes are instantiated by the Skin.  The lifecycle of a behavior starts with BaseBehavior.install(Skin) called from Skin.install() and terminates with BaseBehavior.dispose() called from Skin.dispose().

During installation, an actual behavior registers event mappings that are specific to that behavior.  It is important to note that any user-defined mappings added at the Control level (since InputMap is a property of the Control) take precedence over behavior-specific mappings, so a null skin, or changing a skin has no effect on the user-defined mappings.  All mappings added by the install() method will be removed by the dispose().

BaseBehavior provides the following public methods: 
- public void **install**(Skin)
- public void **dispose**()

as well as a number of protected methods intended to be called by the behavior implementation in BehaviorBase.install():
- protected void **regFunc**(FunctionTag, Runnable)
- protected void **regKey**(KeyBinding, FunctionTag)
- protected void **regKey** (KeyCode, FunctionTag)
- protected void **addHandler**(EventCriteria, boolean, EventHandler)
- protected void **addHandler** (EventType, boolean, EventHandler)
- protected void **addHandler** (EventType, EventHandler)
- protected void **addHandlerTail**(EventType, boolean, EventHandler)
- protected void **addHandlerTail**(EventType, EventHandler)
- protected void **setOnKeyEventEnter**(Runnable)
- protected void **setOnKeyEventExit**(Runnable)

Finally, the **Control** base class declares two new methods:
- public InputMap **getInputMap**()
- protected void **execute**(FunctionTag)

We now can describe a recommended (but not required) structure of programmatic access to the behavior APIs. 

Each Control declares public static final FunctionTags serving as descriptors for the functionality provided by the behavior.  Control also declares public methods for each FunctionTag.  Typically, these methods do not contain actual implementation of the required functionality, but rather invoke Control.execute(FunctionTag).

The actual functionality is provided by the corresponding Behavior.  A concrete Behavior implementation would declare methods corresponding to the Control's FunctionTags, thus providing the default behavior.

This design might have an unexpected side effect of the basic functionality expressed by the public methods in Control not being available with a null Skin.  Why Skin is allowed to be null for a graphical user interface component is unclear.




## Alternatives

An application developer that needs a custom control either needs to craft a completely new implementation, or jerry-rig special event filters and handlers, hoping that their custom code won't interfere with the rest of the Control behavior and functionality (accessibility, focus traversal, etc.)



## Risks and Assumptions

The major risk is that the new behaviors that extend BehaviorBase might introduce a regression because:
a) no test suite currently exists that exhaustively exercises every key mapping on every platform.
b) no headful tests currently exist that go beyond simple features and try to test system as a whole, complete with focus traversal, popups, and so on.
c) extensive manual testing will be needed
d) default functions will not be available with a null Skin.

The reason the default functions will not be available with a null Skin is that the default functionality is implemented by the control behavior, which is instantiated by the Skin.  A Skin is allowed to be null, so naturally all the key mappings and event handlers will be unregistered.  What use is for a Control with no Skin in the context of an application with a UI poses a philosophical question outside of the scope of this proposal.



## Dependencies

None.






