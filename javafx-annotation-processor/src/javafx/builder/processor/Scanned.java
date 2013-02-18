/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package javafx.builder.processor;

import com.sun.javafx.beans.annotations.Default;
import com.sun.javafx.beans.annotations.DuplicateInBuilderProperties;
import com.sun.javafx.beans.annotations.NoBuilder;
import com.sun.javafx.beans.annotations.NoInit;
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import java.beans.Introspector;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

// The result of scanning a class.
final class Scanned {
    // The properties that are defined in this class or inherited from the superclass.
    // The name of each property is mapped to its getter.
    public final Map<String, ExecutableElement> properties = new TreeMap<String, ExecutableElement>(String.CASE_INSENSITIVE_ORDER);
    // The properties (local or inherited) that are set by the chosen constructor of this class.
    // Each property is mapped to an initializer-expression string specified by @Default, or empty string if none
    public final Map<String, String> constructorProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    // Since constructorProperties loses the original order of the constructor arguments, we save them separately.
    // We originally used a LinkedHashMap to keep the original order, but that didn't give us an easy way to ensure
    // case-insensitivity.
    public final List<String> constructorParameters = new ArrayList<String>();
    // Which local properties are of type Collection or a subtype, meaning that they can be changed by
    // changing the Collection rather than calling a setter.
    public final Set<String> collectionProperties = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    // Contains methods from builder extension
    public final List<ExecutableElement> builderExtensionMethods = new ArrayList<ExecutableElement>();
    // Contains methods from builder extension which hide the properties of the builder
    public final Set<String> builderExtensionProperties = new HashSet<String>();
    // The result of scanning the superclass if it exists and is available; otherwise null.
    public final Scanned superScanned;
    // True if the class is abstract or "effectively abstract" (has no public constructor).
    public boolean isAbstract;
    // The expression to produce an instance of the class, an invocation either of a constructor or a factory method.
    // Does not include arguments, so for example it might be "new javafx.scene.shape.Rectangle".
    public String constructorCall;

    // TypeElement of the scanned class
    private TypeElement type;
    // Contains class representing builder extension if there is one.
    // Builder extension are looked up in com.sun... packages and
    // have their name consist of the name of processed class
    // with "BuilderExtension" suffix
    private TypeElement builderExtension;
    // Which local properties were marked as @ReturnsUnmodifiableCollection.
    // We will only generate a method for such a property in the builder
    // if it is one of the properties set by the constructor.
    private final Set<String> unmodifiableCollectionProperties = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    // Which properties (local or inherited) have setters.
    private final Set<String> propertiesWithSetters = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    // Which properties were marked with @DuplicateInBuilderProperties annotation.
    // Methods for these properties will be generated both for the superclass
    // and for subclasses
    private final Set<String> movedProperties = new HashSet<String>();
    private boolean noBuilder;
    private AnnotationUtils utils;
    private static final Set<String> javaKeyWords = new TreeSet<String>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void",
            "volatile", "while"));

    private Scanned(Scanned superScanned, TypeElement type, AnnotationUtils utils) {
        this.superScanned = superScanned;
        this.utils = utils;
        this.type = type;
        this.noBuilder = (type.getAnnotation(NoBuilder.class) != null);
    }

    private Set<String> inheritedProperties() {
        if (superScanned == null) {
            return Collections.emptySet();
        } else {
            return superScanned.properties.keySet();
        }
    }

    private Set<String> inheritedSetters() {
        if (superScanned == null) {
            return Collections.emptySet();
        } else {
            return superScanned.propertiesWithSetters;
        }
    }

    private boolean inherits(String property) {
        return inheritedProperties().contains(property);
    }

    boolean hasInheritedSetters() {
        if (superScanned == null) {
            return false;
        }

        return !superScanned.propertiesWithSetters.isEmpty();
    }

    private void buildPropertiesWithGetters(List<ExecutableElement> methods) {
        // Find the properties with explicit getters
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.PUBLIC)
                    && !method.getModifiers().contains(Modifier.STATIC)
                    && !utils.isVoidType(method.getReturnType())
                    && method.getParameters().isEmpty()
                    && method.getTypeParameters().isEmpty()
                    && method.getAnnotation(NoInit.class) == null) {
                final String name = method.getSimpleName().toString();
                final int len;
                if (name.startsWith("get")) {
                    len = 3;
                } else if (name.startsWith("is") && utils.isBooleanType(method.getReturnType())) {
                    len = 2;
                } else {
                    len = -1;
                }
                if (len > 0 && name.length() > len && !name.equals("getClass")) {
                    String propertyName = Introspector.decapitalize(name.substring(len));
                    if (!inherits(propertyName)) {
                        TypeMirror propertyType = method.getReturnType();
                        TypeMirror oldType = typeOf(propertyName);
                        properties.put(propertyName, method);
                        if (oldType != null && !utils.isSameType(oldType, propertyType)) {
                            utils.warn("Property " + name + " has type " + oldType + " via annotations but type "
                                    + propertyType + " via getter");
                            // Could lead to problems with e.g. List<?> which is not equal to itself.
                        }
                        if (method.getAnnotation(ReturnsUnmodifiableCollection.class) != null) {
                            unmodifiableCollectionProperties.add(propertyName);
                        }
                    }
                }
            }
        }

        // If anyone is foolish enough to define a property that is a Java reserved word, then they won't
        // be able to set it with our generated builder.
        for (String prop : properties.keySet()) {
            if (javaKeyWords.contains(prop)) {
                if (!noBuilder) {
                    utils.warn("Property " + type + "." + prop + " is a reserved word");
                }
                properties.remove(prop);
            }
        }
    }

    private void buildPropertiesWithSetters(List<ExecutableElement> methods) {
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.PUBLIC)
                    && !method.getModifiers().contains(Modifier.STATIC)
                    && utils.isVoidType(method.getReturnType())
                    && method.getParameters().size() == 1
                    && method.getTypeParameters().isEmpty()) {
                final String name = method.getSimpleName().toString();
                if (name.startsWith("set") && !name.equals("set")) {
                    final String propertyName = Introspector.decapitalize(name.substring(3));
                    TypeMirror getterType = typeOf(propertyName);
                    TypeMirror setterType = method.getParameters().get(0).asType();
                    if (getterType != null && utils.isSameType(getterType, setterType)) {
                        propertiesWithSetters.add(propertyName);
                    }
                }
            }
        }
    }

    private void buildCollectionProperties() {
       // We also consider that getters for properties that are Collections (such as List or ObservableList) are
       // effectively setters since you can replace the contents of the collection.  But if there is a setter
       // we will prefer that.
       Iterator<String> it = properties.keySet().iterator();
       while (it.hasNext()) {
           String pName = it.next();
           TypeMirror pType = typeOf(pName);

           if (utils.isCollection(pType)) {
               if (AnnotationUtils.isWildcardType(pType)) {
                   // we ignore wildcard collections (e.g. List<? extend Color>
                   // because there is no way to add an element to them anyway
                   it.remove();
               } else {
                   boolean added = propertiesWithSetters.add(pName);
                   if (added) {
                       collectionProperties.add(pName);
                   }
               }
           }
       }
    }

    // Finds the constructor that defines the most properties with no setters.  In the case
    // of immutable classes, this will be the constructor that defines everything.  In the case
    // of completely-mutable classes, this will typically be the no-arg constructor.  In the case
    // of partly-mutable classes, this will be the constructor that defines all the immutable propeties.
    // If there is a tie between two constructors then we choose the one that has the fewest parameters,
    // which means that it sets the fewest properties that could also be set with setters.
    private boolean buildConstructor(boolean forSuper) {
        ExecutableElement chosenConstructor;

        chosenConstructor = chooseConstructor(Modifier.PUBLIC);
        if (chosenConstructor == null && !propertiesWithSetters.isEmpty()) {
            // If there is no public constructor but there are protected ones then we consider those,
            // but we also mark the builder as abstract.  If the class has no setters (is immutable)
            // with a protected constructor, then we will not make a buider for it.
            chosenConstructor = chooseConstructor(Modifier.PROTECTED);
            if (chosenConstructor != null) {
                isAbstract = true;
            }
        }

        if (chosenConstructor == null) {
            if (!isAbstract) {
                if (!forSuper && !noBuilder && properties.size() > 1) {
                    utils.warn("Cannot make builder for " + type
                            + " because no constructor specifies only properties as parameters");
                }
                return false;
            }
        } else {
            if (chosenConstructor.getKind() == ElementKind.CONSTRUCTOR) {
                constructorCall = "new " + type;
            } else {
                constructorCall = type + "." + chosenConstructor.getSimpleName();
            }

            List<String> missingDefaults = new ArrayList<String>();
            for (VariableElement param : chosenConstructor.getParameters()) {
                String pName = param.getSimpleName().toString();
                Default def = param.getAnnotation(Default.class);
                String defaultInit;
                if (def == null) {
                    missingDefaults.add(pName);
                    defaultInit = "";
                } else {
                    defaultInit = def.value();
                }
                constructorProperties.put(pName, defaultInit);
                constructorParameters.add(setGet((SortedSet<String>) properties.keySet(), pName));
            }

            if (utils.verbose && !noBuilder && !missingDefaults.isEmpty()) {
                utils.warn("Constructor does not specify @Default for parameters " + missingDefaults + ": " + chosenConstructor);
            }
        }

        return true;
    }

    // Fills the movedProperties set with properties from
    // @DuplicateInBuilderProperties annotation
    // Also adds these properties into appropriate collections for this
    // scan class as they need to be processed when making the builder
    void buildMovedProperties(TypeElement type) {
        if (superScanned != null) {
             DuplicateInBuilderProperties annotation = type.getAnnotation(DuplicateInBuilderProperties.class);
             if (annotation != null) {
                 for (int i = 0; i < annotation.properties().length; i++) {
                    String propertyName = annotation.properties()[i];
                    if (superScanned.propertiesWithSetters.contains(propertyName)) {
                        propertiesWithSetters.add(propertyName);
                        movedProperties.add(propertyName);
                    }
                    if (superScanned.collectionProperties.contains(propertyName)) {
                        collectionProperties.add(propertyName);
                        movedProperties.add(propertyName);
                    }
                    if (superScanned.constructorProperties.containsKey(propertyName)) {
                        constructorProperties.put(propertyName, superScanned.constructorProperties.get(propertyName));
                        movedProperties.add(propertyName);
                    }
                }
            }
        }
    }

    private void processBuilderExtension() {
        if (superScanned != null) {
            hasInheritedBuilderExtension = superScanned.hasBuilderExtension()
                    || superScanned.hasInheritedBuilderExtension();
        }

        builderExtension = utils.getBuilderExtension(type);
        if (builderExtension == null) {
            return;
        }

        final List<ExecutableElement> methods =
                ElementFilter.methodsIn(builderExtension.getEnclosedElements());
        for (ExecutableElement method : methods) {
            // we process only public non-static method and omit applyTo() method
            if (method.getModifiers().contains(Modifier.PUBLIC)
                    && !method.getModifiers().contains(Modifier.STATIC)
                    && !"applyTo".equals(method.getSimpleName().toString())) {
                final String name = method.getSimpleName().toString();
                builderExtensionMethods.add(method);
                ExecutableElement prop = properties.get(name);
                // check whether given method would be generated by the builder processor
                // in such case, we need to tell it not to generate this method
                // as it is copied from builder extension
                if (prop != null && method.getParameters().size() == 1) {
                    TypeMirror beType = method.getParameters().get(0).asType();
                    TypeMirror pType = prop.getReturnType();
                    if (utils.isSameType(pType, beType)) {
                        // standard property
                        builderExtensionProperties.add(name);
                    } else if (collectionProperties.contains(name) && AnnotationUtils.hasOneArgument(pType)) {
                        // collection method - we check whether the type
                        // of the collection is the same as in the builder
                        TypeMirror typeArg = ((DeclaredType) pType).getTypeArguments().get(0);
                        if (AnnotationUtils.hasOneArgument(beType)) {
                            TypeMirror methodTypeArg = utils.types.erasure(((DeclaredType) beType).getTypeArguments().get(0));
                            if (utils.isSameType(typeArg, methodTypeArg)) {
                                builderExtensionProperties.add(name);
                            }
                        }
                    }
                }
            }
        }
    }

    TypeMirror typeOf(String property) {
        ExecutableElement getter = properties.get(property);
        if (getter == null) {
            return null;
        } else {
            return getter.getReturnType();
        }
    }

    // Find the constructor that sets the most immutable properties and the fewest mutable ones.
    private ExecutableElement chooseConstructor(Modifier access) {
        ExecutableElement best = null;
        int bestImmutables = -1;
        int bestCount = -1;

        final List<ExecutableElement> constructors =
                new ArrayList<ExecutableElement>(ElementFilter.constructorsIn(type.getEnclosedElements()));

        // It turns out that this is not appropriate, so it's disabled for now.  We may at some future point
        // restore support for factories, probably with an explicit @Factory annotation.
        if (false) {
            // We rather hackily consider that static methods with the same name as the class they are in,
            // ignoring case, are factory methods equivalent to constructors, as in Font.font and Color.color.
            // If both a factory method and a constructor are "best", then we prefer the factory method on the
            // grounds that it may be able to reuse instances and the like.
            for (ExecutableElement factory : ElementFilter.methodsIn(type.getEnclosedElements())) {
                if (factory.getModifiers().contains(Modifier.STATIC)
                        && factory.getSimpleName().toString().equalsIgnoreCase(type.getSimpleName().toString())) {
                    constructors.add(factory);
                }
            }
        }

        for (ExecutableElement constructor : constructors) {
            if (constructor.getModifiers().contains(access) && constructor.getTypeParameters().isEmpty()) {
                int immutables = 0;
                for (VariableElement param : constructor.getParameters()) {
                    final String pName = param.getSimpleName().toString();
                    final TypeMirror pType = typeOf(pName);
                    if (pType == null
                            || (!utils.isSameType(param.asType(), pType) && !param.asType().toString().equals(pType.toString()))) {
                        // This area is a bit messy. Really we should be determining whether the parameter is a subtype of
                        // the (possibly inherited) property type. But if it is a strict subtype then we need to adjust the
                        // inherited type, so that for example InputEvent's constructor which takes an EventType<? extends InputEvent>
                        // would cause the builder to have a field of that type, even though the corresponding property
                        // inherited from Event is an EventType<? extends Event>. For now, we forgo some possibilities
                        // to generate builders.
                        immutables = -1;
                        break;
                    }
                    if (!propertiesWithSetters.contains(pName)) {
                        immutables++;
                    }
                }
                final int count = constructor.getParameters().size();
                if (immutables > bestImmutables || (immutables == bestImmutables && count < bestCount)
                        || (immutables == bestImmutables && count == bestCount && best != null
                        && best.getKind() == ElementKind.CONSTRUCTOR && constructor.getKind() == ElementKind.METHOD)) {
                    // That last giant conjunct is where we prefer static factory methods to constructors,
                    // all else being equal.
                    best = constructor;
                    bestImmutables = immutables;
                    bestCount = count;
                }
            }
        }
        if (bestCount != bestImmutables) {
            System.out.println("bestCount " + bestCount + "immutables " + bestImmutables + " for type " + type);
        }

        return best;
    }

    public static Scanned createScanned(Scanned superScanned,
            TypeElement type,
            boolean forSuper,
            AnnotationUtils utils) {
        final Scanned scanned = new Scanned(superScanned, type, utils);
        scanned.isAbstract = type.getModifiers().contains(Modifier.ABSTRACT);
        if (superScanned != null) {
            scanned.properties.putAll(superScanned.properties);
            scanned.propertiesWithSetters.addAll(scanned.inheritedSetters());
        }

        final List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());

        // Find the properties with explicit getters
        scanned.buildPropertiesWithGetters(methods);

        // Find any setters for the properties found above.
        scanned.buildPropertiesWithSetters(methods);

        // We also consider that getters for properties that are Collections (such as List or ObservableList) are
        // effectively setters since you can replace the contents of the collection.  But if there is a setter
        // we will prefer that.
        scanned.buildCollectionProperties();

        // Now we want the constructor that defines the most properties with no setters.  In the case
        // of immutable classes, this will be the constructor that defines everything.  In the case
        // of completely-mutable classes, this will typically be the no-arg constructor.  In the case
        // of partly-mutable classes, this will be the constructor that defines all the immutable propeties.
        // If there is a tie between two constructors then we choose the one that has the fewest parameters,
        // which means that it sets the fewest properties that could also be set with setters.
        if (!scanned.buildConstructor(forSuper)) {
            return new Scanned(null, type, utils);
        }

        // process properties with @DuplicateInBuilderProperties annotation
        scanned.buildMovedProperties(type);

        // try to find builder extension
        scanned.processBuilderExtension();

        return scanned;
    }

    public boolean hasBuilderExtension() {
        return builderExtension != null;
    }


    private boolean hasInheritedBuilderExtension = false;
    boolean hasInheritedBuilderExtension() {
        return false; // Inheritance of builder extensions is disabled for now
        // return hasInheritedBuilderExtension;
    }
    
    private Set<String> buildablePropertyNames;
    public Set<String> getBuildablePropertyNames() {
        if (buildablePropertyNames == null) {
            buildablePropertyNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

            buildablePropertyNames.addAll(propertiesWithSetters);
            buildablePropertyNames.removeAll(inheritedSetters());
            buildablePropertyNames.removeAll(unmodifiableCollectionProperties);
            buildablePropertyNames.addAll(constructorProperties.keySet());
            buildablePropertyNames.addAll(movedProperties);
            buildablePropertyNames.removeAll(builderExtensionProperties);
        }

        return buildablePropertyNames;
    }

    public static boolean isAbstract(Scanned scanned) {
        return scanned == null || (scanned.isAbstract && isAbstract(scanned.superScanned));
    }

    public String getBuilderExtensionName() {
        if (builderExtension != null) {
            return builderExtension.getQualifiedName().toString();
        } else {
            Scanned sc = superScanned;
            while (sc != null) {
                if (sc.builderExtension != null) {
                    return sc.builderExtension.getQualifiedName().toString();
                }
                sc = sc.superScanned;
            }
        }
        return "";
    }

    // Return the element of the given set that is equal to e according to the set's comparator.
    // This can be used to get the canonical spelling wrt case when the comparator is String.CASE_INSENSITIVE_ORDER
    // for example.
    private static <E> E setGet(SortedSet<E> set, E e) {
        Comparator<? super E> cmp = set.comparator();
        for (E x : set) {
            if (cmp.compare(x, e) == 0) {
                return x;
            }
        }
        return null;
    }

}
