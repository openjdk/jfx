/*
 * Copyright (c) 2011, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.annotations.DuplicateInBuilderProperties;
import com.sun.javafx.beans.annotations.Default;
import com.sun.javafx.beans.annotations.NoBuilder;
import com.sun.javafx.beans.annotations.NoInit;
import com.sun.javafx.collections.annotations.ReturnsUnmodifiableCollection;
import java.beans.Introspector;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * <p>This annotation processor (compiler plugin) generates builder classes for the classes being compiled.
 * A builder class is generated if the fully-qualified name of a class matches the javafx.builders.input
 * parameter (specified with -Ajavafx.builders.input=pattern on the command line), and the class is public,
 * has properties, and has a usable public constructor.  A usable public constructor is one where the name
 * and type of each parameter corresponds to the name and type of a property.  A no-arg constructor is
 * always usable.</p>
 *
 * <p>The output class is determined by -Ajavafx.builders.output, which is a replacement pattern where
 * $1 $2 etc refer to corresponding parenthesized groups in javafx.builders.input and $0 refers to the
 * entire class name.  The default javafx.builders.input is {@code (javafx\\..*)} and the default
 * javafx.builders.output is {@code $1Builder}, so each builder goes in the same package as the class
 * it builds, with an extra {@code Builder} on the end of its name. For example
 * the builder for javafx.scene.shape.Rectangle is javafx.scene.shape.RectangleBuilder.</p>
 */
@SupportedAnnotationTypes("*")
@SupportedOptions({
    BuilderProcessor.inputOption, BuilderProcessor.outputOption, BuilderProcessor.verboseOption
})
public class BuilderProcessor extends AbstractProcessor {
    static final String inputOption = "javafx.builders.input";
    static final String outputOption = "javafx.builders.output";
    static final String verboseOption = "javafx.builders.verbose";

    private Types types;
    private Elements elements;
    private NoType voidType;
    private PrimitiveType booleanType;
    private Filer filer;
    private Pattern inputPat = Pattern.compile("(javafx\\..*)");
    private String outputRepl = "$1Builder";
    private boolean verbose;
    private TypeMirror rawCollectionType;
    private TypeMirror rawObservableListType;

    private final Set<String> builderNames = new HashSet<String>();

    private void note(String msg) {
        if (verbose) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

    private void warn(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
    }

    private void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    // We are asserting here that this processor can handle anything that new versions of the language can
    // throw at it.  That's probably true, and even if it isn't we can always rewrite.
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return super.getSupportedOptions();
    }

    // The compiler calls this method after it has analyzed all the *.java files it was given or that it
    // found by following dependencies.  If annotation processors (such as this one) create new *.java files
    // then this method will be called again with all the new top-level classes.  That's why we record the names of all
    // classes we generate in builderNames, so we don't consider generating RectangleBuilderBuilder or
    // whatever.  (In fact, generated builders don't have properties currently, so they would not get their
    // own builders anyway, but we don't take the chance that some future change might add properties.)
    // If another annotation processor generates further *.java files in the second "round" then this
    // method will be called again, and so on until there are no more new *.java files.  At that point
    // the method is called with an empty set of root elements.
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<String, String> options = processingEnv.getOptions();
        if (options.containsKey(verboseOption)) {
            verbose = Boolean.parseBoolean(options.get(verboseOption));
        }
        note("BuilderProcessor runs");
        String input = options.get(inputOption);
        if (input != null) {
            try {
                inputPat = Pattern.compile(input);
            } catch (PatternSyntaxException e) {
                error("Value for option " + inputOption + " is not a valid regular expression: " + e.getMessage());
                return false;
            }
        }
        if (options.containsKey(outputOption)) {
            outputRepl = options.get(outputOption);
        }
        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        voidType = types.getNoType(TypeKind.VOID);
        booleanType = types.getPrimitiveType(TypeKind.BOOLEAN);
        filer = processingEnv.getFiler();
        rawCollectionType = types.getDeclaredType(elements.getTypeElement(Collection.class.getName()));
        TypeElement observableArrayListType = elements.getTypeElement("javafx.collections.ObservableList");
        if (observableArrayListType != null) {
            rawObservableListType = types.getDeclaredType(observableArrayListType);
        }

        processTypes(ElementFilter.typesIn(roundEnv.getRootElements()));
        return false;
    }

    private void processTypes(Set<TypeElement> types) {
        for (TypeElement type : types) {
            if (isCandidateType(type)) {
                processType(type);
            }
        }
    }

    private boolean isCandidateType(TypeElement type) {
        String typeName = type.toString();
        return (!builderNames.contains(typeName) &&
                inputPat.matcher(type.toString()).matches() &&
                type.getKind() == ElementKind.CLASS &&
                type.getModifiers().contains(Modifier.PUBLIC));
    }

    // The result of scanning a class.
    private static class Scanned {
        // The properties that are defined in this class or inherited from the superclass.
        // The name of each property is mapped to its getter.
        final Map<String, ExecutableElement> properties = new TreeMap<String, ExecutableElement>(String.CASE_INSENSITIVE_ORDER);

        // The properties (local or inherited) that are set by the chosen constructor of this class.
        // Each property is mapped to an initializer-expression string specified by @Default, or empty string if none
        final Map<String, String> constructorProperties = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        // Since constructorProperties loses the original order of the constructor arguments, we save them separately.
        // We originally used a LinkedHashMap to keep the original order, but that didn't give us an easy way to ensure
        // case-insensitivity.
        final List<String> constructorParameters = new ArrayList<String>();

        // Which properties (local or inherited) have setters.
        final Set<String> propertiesWithSetters = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        // Which local properties are of type Collection or a subtype, meaning that they can be changed by
        // changing the Collection rather than calling a setter.
        final Set<String> collectionProperties = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        // Which local properties were marked as @ReturnsUnmodifiableCollection.
        // We will only generate a method for such a property in the builder
        // if it is one of the properties set by the constructor.
        final Set<String> unmodifiableCollectionProperties = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);

        // Which properties were marked with @MovedFromSubclass annotation.
        // Methods for these properties will be generated both for the superclass
        // and for subclasses
        final Set<String> movedProperties = new HashSet<String>();

        // The result of scanning the superclass if it exists and is available; otherwise null.
        final Scanned superScanned;

        // True if the class is abstract or "effectively abstract" (has no public constructor).
        boolean isAbstract;

        // The expression to produce an instance of the class, an invocation either of a constructor or a factory method.
        // Does not include arguments, so for example it might be "new javafx.scene.shape.Rectangle".
        String constructorCall;

        Scanned(Scanned superScanned) {
            this.superScanned = superScanned;
        }

        boolean inherits(String property) {
            return inheritedProperties().contains(property);
        }

        Set<String> inheritedProperties() {
            if (superScanned == null) {
                return Collections.emptySet();
            } else {
                return superScanned.properties.keySet();
            }
        }

        Set<String> inheritedSetters() {
            if (superScanned == null) {
                return Collections.emptySet();
            } else {
                return superScanned.propertiesWithSetters;
            }
        }

        // Fills the movedProperties set with properties from
        // @MovedFromSubclass annotation
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

        TypeMirror typeOf(String property) {
            ExecutableElement getter = properties.get(property);
            if (getter == null) {
                return null;
            } else {
                return getter.getReturnType();
            }
        }
    }

    private Map<TypeElement, Scanned> scanMap = new HashMap<TypeElement, Scanned>();

    // Scan the class for properties, determined as follows.  First we look for getters.  A getter is an
    // instance method with no arguments and returning something (not void) and whose name is
    // getFoo for some non-empty Foo, or isFoo with a return type of boolean.  It defines a property
    // called foo.  Then, determine which of those properties can be built.  A property can be built
    // if there is a method setFoo that returns void and has a single argument, with the same type as the getter.
    // It can also be built if it has no setter but the "maximal constructor" includes it.
    // We find a maximal constructor by looking for constructors where each parameter has the same
    // name and type as some immutable property (property that has no setter).
    // Among these, the one with the most immutable-property arguments is the maximal
    // constructor.  If there is more than one such constructor then the one with the fewest arguments
    // over all is the maximal constructor (in other words, the fewest arguments that we could also
    // set with setters).  There could potentially be more than one such constructor, in which case
    // we don't specify which one is chosen.
    // The forSuper parameter indicates that we are only scanning this class because it is an ancestor of
    // the one we're really interested in.  So we won't emit certain warnings, for example.  We might later
    // re-scan it in its own right, which is why we don't look in the scanMap in that case.
    private Scanned scan(TypeElement type, boolean forSuper) {
        if (forSuper && scanMap.containsKey(type)) {
            return scanMap.get(type);
        }
        Scanned scanned = scan1(type, forSuper);
        scanMap.put(type, scanned);
        return scanned;
    }

    private Scanned scan1(TypeElement type, boolean forSuper) {
        Scanned superScanned = null;
        TypeMirror superMirror = type.getSuperclass();
        if (!(superMirror instanceof NoType)) {
            TypeElement superElement = elements.getTypeElement(types.erasure(superMirror).toString());
            if (superElement != null && isCandidateType(superElement)) {
                superScanned = scan(superElement, true);
            }
        }

        final Scanned scanned = new Scanned(superScanned);
        scanned.isAbstract = type.getModifiers().contains(Modifier.ABSTRACT);
        if (superScanned != null) {
            scanned.properties.putAll(superScanned.properties);
            scanned.propertiesWithSetters.addAll(scanned.inheritedSetters());
        }

        final boolean noBuilder = (type.getAnnotation(NoBuilder.class) != null);

        // Find the properties with explicit getters
        final List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.PUBLIC) &&
                    !method.getModifiers().contains(Modifier.STATIC) &&
                    !types.isSameType(voidType, method.getReturnType()) &&
                    method.getParameters().isEmpty() &&
                    method.getTypeParameters().isEmpty() &&
                    method.getAnnotation(NoInit.class) == null) {
                final String name = method.getSimpleName().toString();
                final int len;
                if (name.startsWith("get")) {
                    len = 3;
                } else if (name.startsWith("is") && types.isSameType(method.getReturnType(), booleanType)) {
                    len = 2;
                } else {
                    len = -1;
                }
                if (len > 0 && name.length() > len && !name.equals("getClass")) {
                    String propertyName = Introspector.decapitalize(name.substring(len));
                    if (!scanned.inherits(propertyName)) {
                        TypeMirror propertyType = method.getReturnType();
                        TypeMirror oldType = scanned.typeOf(propertyName);
                        scanned.properties.put(propertyName, method);
                        if (oldType != null && !types.isSameType(oldType, propertyType)) {
                            warn("Property " + name + " has type " + oldType + " via annotations but type " +
                                    propertyType + " via getter");
                            // Could lead to problems with e.g. List<?> which is not equal to itself.
                        }
                        if (method.getAnnotation(ReturnsUnmodifiableCollection.class) != null) {
                            scanned.unmodifiableCollectionProperties.add(propertyName);
                        }
                    }
                }
            }
        }

        // If anyone is foolish enough to define a property that is a Java reserved word, then they won't
        // be able to set it with our generated builder.
        for (String prop : scanned.properties.keySet()) {
            if (javaKeyWords.contains(prop)) {
                if (!noBuilder) {
                    warn("Property " + type + "." + prop + " is a reserved word");
                }
                scanned.properties.remove(prop);
            }
        }

        // Find any setters for the properties found above.
        for (ExecutableElement method : methods) {
            if (method.getModifiers().contains(Modifier.PUBLIC) &&
                    !method.getModifiers().contains(Modifier.STATIC) &&
                    types.isSameType(voidType, method.getReturnType()) &&
                    method.getParameters().size() == 1 &&
                    method.getTypeParameters().isEmpty()) {
                final String name = method.getSimpleName().toString();
                if (name.startsWith("set") && !name.equals("set")) {
                    final String propertyName = Introspector.decapitalize(name.substring(3));
                    TypeMirror getterType = scanned.typeOf(propertyName);
                    TypeMirror setterType = method.getParameters().get(0).asType();
                    if (getterType != null && types.isSameType(getterType, setterType)) {
                        scanned.propertiesWithSetters.add(propertyName);
                    }
                }
            }
        }

        // We also consider that getters for properties that are Collections (such as List or ObservableList) are
        // effectively setters since you can replace the contents of the collection.  But if there is a setter
        // we will prefer that.
        Iterator<String> it = scanned.properties.keySet().iterator();
        while (it.hasNext()) {
            String pName = it.next();
            TypeMirror pType = scanned.typeOf(pName);

            if (types.isSubtype(types.erasure(pType), rawCollectionType)) {
                if (isWildcardType(pType)) {
                    // we ignore wildcard collections (e.g. List<? extend Color>
                    // because there is no way to add an element to them anyway
                    it.remove();
                } else {
                    boolean added = scanned.propertiesWithSetters.add(pName);
                    if (added) {
                        scanned.collectionProperties.add(pName);
                    }
                }
            }
        }

        // Now we want the constructor that defines the most properties with no setters.  In the case
        // of immutable classes, this will be the constructor that defines everything.  In the case
        // of completely-mutable classes, this will typically be the no-arg constructor.  In the case
        // of partly-mutable classes, this will be the constructor that defines all the immutable propeties.
        // If there is a tie between two constructors then we choose the one that has the fewest parameters,
        // which means that it sets the fewest properties that could also be set with setters.
        ExecutableElement chosenConstructor;

        chosenConstructor = chooseConstructor(type, scanned, Modifier.PUBLIC);
        if (chosenConstructor == null && !scanned.propertiesWithSetters.isEmpty()) {
            // If there is no public constructor but there are protected ones then we consider those,
            // but we also mark the builder as abstract.  If the class has no setters (is immutable)
            // with a protected constructor, then we will not make a buider for it.
            chosenConstructor = chooseConstructor(type, scanned, Modifier.PROTECTED);
            if (chosenConstructor != null) {
                scanned.isAbstract = true;
            }
        }

        if (chosenConstructor == null) {
            if (!scanned.isAbstract) {
                if (!forSuper && !noBuilder && scanned.properties.size() > 1) {
                    warn("Cannot make builder for " + type +
                            " because no constructor specifies only properties as parameters");
                }
                return new Scanned(null);
            }
        } else {
            if (chosenConstructor.getKind() == ElementKind.CONSTRUCTOR) {
                scanned.constructorCall = "new " + type;
            } else {
                scanned.constructorCall = type + "." + chosenConstructor.getSimpleName();
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
                scanned.constructorProperties.put(pName, defaultInit);
                scanned.constructorParameters.add(setGet((SortedSet<String>) scanned.properties.keySet(), pName));
            }

            if (verbose && !noBuilder && !missingDefaults.isEmpty()) {
                warn("Constructor does not specify @Default for parameters " + missingDefaults + ": " + chosenConstructor);
            }
        }

        // process properties with @MovedFromSubclass annotation
        scanned.buildMovedProperties(type);

        return scanned;
    }

    // Find the constructor that sets the most immutable properties and the fewest mutable ones.
    private ExecutableElement chooseConstructor(TypeElement type, Scanned scanned, Modifier access) {
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
                if (factory.getModifiers().contains(Modifier.STATIC) &&
                        factory.getSimpleName().toString().equalsIgnoreCase(type.getSimpleName().toString())) {
                    constructors.add(factory);
                }
            }
        }

        for (ExecutableElement constructor : constructors) {
            if (constructor.getModifiers().contains(access) && constructor.getTypeParameters().isEmpty()) {
                int immutables = 0;
                for (VariableElement param : constructor.getParameters()) {
                    final String pName = param.getSimpleName().toString();
                    final TypeMirror pType = scanned.typeOf(pName);
                    if (pType == null ||
                            (!types.isSameType(param.asType(), pType) && !param.asType().toString().equals(pType.toString()))) {
                        // This area is a bit messy. Really we should be determining whether the parameter is a subtype of
                        // the (possibly inherited) property type. But if it is a strict subtype then we need to adjust the
                        // inherited type, so that for example InputEvent's constructor which takes an EventType<? extends InputEvent>
                        // would cause the builder to have a field of that type, even though the corresponding property
                        // inherited from Event is an EventType<? extends Event>. For now, we forgo some possibilities
                        // to generate builders.
                        immutables = -1;
                        break;
                    }
                    if (!scanned.propertiesWithSetters.contains(pName)) {
                        immutables++;
                    }
                }
                final int count = constructor.getParameters().size();
                if (immutables > bestImmutables || (immutables == bestImmutables && count < bestCount) ||
                        (immutables == bestImmutables && count == bestCount && best != null &&
                         best.getKind() == ElementKind.CONSTRUCTOR && constructor.getKind() == ElementKind.METHOD)) {
                    // That last giant conjunct is where we prefer static factory methods to constructors,
                    // all else being equal.
                    best = constructor;
                    bestImmutables = immutables;
                    bestCount = count;
                }
            }
        }

        return best;
    }

    private void processType(TypeElement type) {
        note("BuilderProcessor: process " + type);

        Scanned scanned = scan(type, false);

        Set<String> buildablePropertyNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        buildablePropertyNames.addAll(scanned.propertiesWithSetters);
        buildablePropertyNames.removeAll(scanned.inheritedSetters());
        buildablePropertyNames.removeAll(scanned.unmodifiableCollectionProperties);
        buildablePropertyNames.addAll(scanned.constructorProperties.keySet());
        buildablePropertyNames.addAll(scanned.movedProperties);

        Map<String, ExecutableElement> notBuildable = new TreeMap<String, ExecutableElement>(String.CASE_INSENSITIVE_ORDER);
        notBuildable.putAll(scanned.properties);
        notBuildable.keySet().removeAll(buildablePropertyNames);
        notBuildable.keySet().removeAll(scanned.inheritedProperties());

        if (type.getAnnotation(NoBuilder.class) != null) {
            return;
        }

        // No properties, no builder
        if (buildablePropertyNames.isEmpty() && scanned.inheritedSetters().isEmpty()) {
            // We still want a builder if the parent has properties, since the generated builder will
            // construct the right type of object even though you will only be able to set the inherited properties.
            return;
        }

        try {
            makeBuilder(type, scanned, buildablePropertyNames);
        } catch (IOException e) {
            if (verbose) {
                e.printStackTrace();
            }
            error("Could not make builder for " + type + ": " + e);
        }
    }

    private String builderName(String type) {
        Matcher m = inputPat.matcher(type);
        if (m.matches()) {
            String name = m.replaceAll(outputRepl);
            builderNames.add(name);
            return name;
        } else {
            error("Internal error: pattern does not match " + type);
            return type + "Builder";
        }
    }

    private static final String generated =
            "@" + Generated.class.getName() + "(\"Generated by " + BuilderProcessor.class.getName() + "\")";
    private static final ResourceBundle resources;
    static {
        try {
            resources = new PropertyResourceBundle(BuilderProcessor.class.getResourceAsStream("BuilderProcessor.properties"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    private static final String buildmethodComment =
            new MessageFormat(resources.getString("buildmethod.doc")).format(new Object[] {
                "{@link {rawType}}"
            });
    private static final String initializingmethodComment =
            new MessageFormat(resources.getString("initializingmethod.doc")).format(new Object[] {
                "{@link {rawType}#{getter}() {pName}}"
            });
    private static final String initializinglistmethodComment =
            new MessageFormat(resources.getString("initializinglistmethod.doc")).format(new Object[] {
                "{@link {rawType}#{getter}() {pName}}"
            });
    private static final String deprecatedComment = resources.getString("deprecated.doc");
    private static final String copyrightComment =
            new MessageFormat(resources.getString("copyright.doc")).format(new Object[] {
                Calendar.getInstance().get(Calendar.YEAR)
            });

    // A string of the type parameters of this type, without the enclosing <>. For List<E>, it will be "E";
    // for Integer, it will be ""; for Map<K, V>, it will be "K, V".
    // If there is an "extends" clause it will be included, so for example
    // ValueAxis<T extends Number> will return "T extends Number"
    private static String typeParameterString(TypeElement type) {
        String s = "";
        for (TypeParameterElement p : type.getTypeParameters()) {
            if (!s.isEmpty()) {
                s += ", ";
            }
            s += p;
            String b = p.getBounds().toString();
            if (b.startsWith("[") && b.endsWith("]")) {
                b = b.substring(1, b.length() - 1).trim();
            }
            if (!b.equals("java.lang.Object")) {
                s += " extends " + b;
            }
        }
        return s;
    }

    // A string of the type arguments of this type. Given ValueAxis<T> extends Axis<T>, if we ask about
    // Axis<T> then the returned string will be "T".
    private static String typeArgumentString(TypeMirror type0) {
        DeclaredType type = (DeclaredType) type0;
        String s = "";
        for (TypeMirror p : type.getTypeArguments()) {
            if (!s.isEmpty()) {
                s += ", ";
            }
            s += p;
        }
        return s;
    }

    // Returns parameters of typed create()
    // e.g. for this create:
    // public static <T> ChoiceBoxBuilder<T, ?> create(final Class<T> type1)
    // it returns final Class<T> type1
    private String typedCreateParamString(TypeElement type) {
        String s = "";
        int i = 0;
        for (TypeParameterElement p : type.getTypeParameters()) {
            if (!s.isEmpty()) {
                s += "> type" + i + " , ";
            }
            s += "final Class<";
            s += p;
            i++;
        }
        s += "> type" + i;
        return s;
    }

    // Returns a string of the type arguments for non-typed create()
    // e.g. <?, ?> in
    // public static javafx.scene.control.ChoiceBoxBuilder<?, ?> create()
    private String nonTypedCreateArgumentString(TypeElement type, boolean isFinal) {
        String p = "";
        for (int j = 1; j <= type.getTypeParameters().size(); j++) {
            if (!p.isEmpty()) {
                p += ", ";
            }
            p += "?";
        }
        if (!isFinal) {
            p += ", ?";
        }
        return p;
    }

    private static boolean isWildcardType(TypeMirror type) {
        boolean result = false;
        if (type instanceof DeclaredType) {
            DeclaredType dType = (DeclaredType) type;
            for (TypeMirror typeArg : dType.getTypeArguments()) {
                if (typeArg instanceof WildcardType) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private void makeBuilder(TypeElement type, Scanned scanned, Set<String> buildablePropertyNames) throws IOException {

        if (false) {
            // We do generate builders even if there is only one property.  Usually you'll just want to
            // call the constructor in that case, but the builder does give you the option of naming the
            // parameter, and it can also be inherited by several subclass builders.
            int n = scanned.inheritedSetters().size() + buildablePropertyNames.size();
            if (n < 2) {
                warn("Properties in " + type + ": " + n);
            }
        }

        // Substitution variables that will be used as we construct the output text.  See rewrite method below.
        // If no value is defined for a var then its value is empty; this is error-prone but simplifies the logic
        // a lot.
        final Map<String, Object> vars = new TreeMap<String, Object>();

        final boolean isAbstract = scanned.isAbstract;

        final boolean isFinal = type.getModifiers().contains(Modifier.FINAL);
        // A class can also be "effectively final" if it has no visible constructors, but we've already eliminated
        // such classes since we need a constructor in order to build.

        final boolean inherits = !scanned.inheritedSetters().isEmpty();
        // We only try to inherit from the superclass's builder if it exists and has setters.  If the superclass
        // is immutable then we are potentially missing a chance to reuse its builder's fields and methods, but
        // that case is rather unusual, and would require the builder to access those fields, which are currently
        // private.

        final Map<String, ExecutableElement> buildableProperties =
                new TreeMap<String, ExecutableElement>(String.CASE_INSENSITIVE_ORDER);
        buildableProperties.putAll(scanned.properties);
        buildableProperties.keySet().retainAll(buildablePropertyNames);

        vars.put("copyrightComment", copyrightComment);
        final String builderName = builderName(type.toString());
        vars.put("builderName", builderName);
        final String[] tokens = builderName.split("\\.");
        vars.put("shortBuilderName", tokens[tokens.length - 1]);
        final int lastDot = builderName.lastIndexOf('.');
        if (lastDot >= 0) {
            final String pkg = builderName.substring(0, lastDot);
            vars.put("packageDecl", "package " + pkg + ";");
        }
        vars.put("simpleName", builderName.substring(lastDot + 1));

        if (isAbstract) {
            vars.put("abstract", "abstract ");
            // We might inherit from a public constructor, which for now will be deprecated, so suppress that warning.
        } else {
            vars.put("create1", "/** Creates a new instance of {shortBuilderName}. */");
            vars.put("create2", "@SuppressWarnings({\"deprecation\", \"rawtypes\", \"unchecked\"})");
            vars.put("create3", "public static {createTypeParams}{createType} create({createParams}) {");
            vars.put("create4",     "return new {builderName}();");
            vars.put("create5", "}");
        }
        final String originalTypeParams = typeParameterString(type);
        final String typeArgs = typeArgumentString(type.asType());
        if (originalTypeParams.isEmpty()) {
            vars.put("type", type.toString());
            vars.put("rawType", type.toString());
            vars.put("typeWithoutBounds", type.toString());
        } else {
            vars.put("createTypeParams", "<" + originalTypeParams + "> ");
            vars.put("type", type + "<" + originalTypeParams + ">");
            vars.put("rawType", type.getQualifiedName().toString());
            vars.put("typeWithoutBounds", type + "<" + typeArgs + ">");
            vars.put("createParams", typedCreateParamString(type));
            String nonTypedArgs = nonTypedCreateArgumentString(type, isFinal);
            vars.put("createType2", builderName + "<" + nonTypedArgs + ">");
            if (!isAbstract) {
                vars.put("createB1", "/** Creates a new instance of {shortBuilderName}. */");
                vars.put("createB2", "@SuppressWarnings({\"deprecation\", \"rawtypes\", \"unchecked\"})");
                vars.put("createB3", "@Deprecated");
                vars.put("createB4", "public static {createType2} create() {");
                vars.put("createB5",    "return new {builderName}();");
                vars.put("createB6", "}");
            }
        }
        if (inherits) {
            // Work out the "extends" class for the builder class. For ValueAxis<T extends Number> extends Axis<T>,
            // the full declaration of ValueAxisBuilder is
            // public class ValueAxisBuilder<T extends Number, B extends ValueAxisBuilder<T, B>> extends AxisBuilder<T, B>
            // Here we are computing "extends AxisBuilder<T, B>".
            final String sup = builderName(types.erasure(type.getSuperclass()).toString());
            vars.put("supB", "B");
            final String supTypeParams = typeArgumentString(type.getSuperclass());
            if (supTypeParams.isEmpty()) {
                vars.put("extends", " extends " + sup + "<{supB}>");
            } else {
                vars.put("extends", " extends " + sup + "<" + supTypeParams + ", {supB}>");
            }
        }
        if (!scanned.isAbstract && isAbstract(scanned.superScanned)) {
            vars.put("implements", " implements javafx.util.Builder<{typeWithoutBounds}>");
        }
        final String typeParams;
        if (isFinal) {
            vars.put("final", "final ");
            vars.put("supB", builderName);
            vars.put("constructorAccess", "private");
            if (originalTypeParams.isEmpty()) {
                vars.put("retType", builderName);
            } else {
                vars.put("retType", builderName + "<" + typeArgs + ">");
            }
            vars.put("createType", "{retType}");
            typeParams = originalTypeParams;
        } else {
            vars.put("constructorAccess", "protected");
            if (originalTypeParams.isEmpty()) {
                typeParams = "B extends " + builderName + "<B>";
                vars.put("createType", builderName + "<?>");
            } else {
                typeParams = originalTypeParams + ", B extends " + builderName + "<" + typeArgs + ", B>";
                vars.put("createType", builderName + "<" + typeArgs + ", ?>");
            }
            vars.put("retType", "B");
            vars.put("suppressWarnings", "@SuppressWarnings(\"unchecked\")");
            vars.put("cast", "(B) ");
        }
        if (!typeParams.isEmpty()) {
            vars.put("typeParams", "<" + typeParams + ">");
        }

        final List<String> setters = new ArrayList<String>(buildableProperties.keySet());
        setters.removeAll(scanned.constructorProperties.keySet());

        vars.put("classjavadoc", "Builder class for " + type.getQualifiedName());

        List<String> lines = rewrite(vars,
                "{debug}",
                "{copyrightComment}",
                "{packageDecl}",
                "",
                "/**",
                "   {classjavadoc}",
                "   @see {rawType}",
                "  */",
                generated,
                "public {final}{abstract}class {simpleName}{typeParams}{extends}{implements} {",
                    "protected {simpleName}() {",
                    "}",
                    "",
                    "{create1}",
                    "{create2}",
                    "{create3}",
                    "{create4}",
                    "{create5}",
                    "",
                    "{createB1}",
                    "{createB2}",
                    "{createB3}",
                    "{createB4}",
                    "{createB5}",
                    "{createB6}"
                );

        if (!setters.isEmpty() || inherits) {
            List<String> applyTo = makeApplyTo(vars, setters, scanned, !scanned.inheritedSetters().isEmpty());
            lines.addAll(rewrite(vars, applyTo));
            vars.put("applyTo(x)", "applyTo(x);");
        }

        for (Map.Entry<String, ExecutableElement> entry : buildableProperties.entrySet()) {
            String pName = entry.getKey();
            vars.put("pName", pName);
            ExecutableElement getter = entry.getValue();
            TypeMirror pType = getter.getReturnType();
            vars.put("getter", getter.getSimpleName());
            // If the property is of a collection type like Set<Color> or List<Integer>, then we will set it
            // by modifying an existing Set or List rather than by assignment. That means that we don't require
            // an exact match and can accept Collection<? extends Color> or whatever. However, if the property
            // is set with a constructor parameter then we do require an exact match because we won't be modifying
            // an existing object. In either case, we remember {elementType} so that we can use it to provide
            // a varargs setter as well, which will be Color... or whatever.
            if (scanned.collectionProperties.contains(pName) &&
                    pType instanceof DeclaredType && ((DeclaredType) pType).getTypeArguments().size() == 1) {
                TypeMirror typeArg = ((DeclaredType) pType).getTypeArguments().get(0);
                if (scanned.constructorProperties.containsKey(pName)) {
                    vars.put("pType", pType.toString());
                } else {
                    vars.put("pType", "java.util.Collection<? extends " + typeArg + ">");
                }
                vars.put("elementType", typeArg);
            } else {
                vars.put("pType", pType.toString());
                vars.remove("elementType");
            }
            String init = scanned.constructorProperties.get(pName);
            if (init == null || init.isEmpty()) {
                vars.remove("init");
            } else {
                vars.put("init", " = " + init);
            }
            int i = setters.indexOf(pName); // XXX quadratic
            if (i < 0) {
                vars.remove("record");
            } else {
                vars.put("i", i);
                vars.put("record", "{setBitI}");
            }
            if (pName.startsWith("impl_")) {
                vars.put("treatasprivate", "@treatAsPrivate");
                vars.put("deprecatedDoc", "@deprecated " + deprecatedComment);
                vars.put("deprecatedAnnotation", "@Deprecated");
            } else {
                vars.remove("treatasprivate");
                vars.remove("deprecatedDoc");
                vars.remove("deprecatedAnnotation");
            }
            final boolean collectionProperty = scanned.collectionProperties.contains(pName);
            lines.addAll(rewrite(vars,
                    "private {pType} {pName}{init};",
                    "/**",
                    "  " + (collectionProperty ? initializinglistmethodComment : initializingmethodComment),
                    "   {treatasprivate}",
                    "   {deprecatedDoc}",
                    "  */",
                    "{suppressWarnings} {deprecatedAnnotation}",
                    "public {retType} {pName}({pType} x) {",
                        "this.{pName} = x;",
                        "{record}",
                        "return {cast}this;",
                    "}",
                    ""));
            // Also define convenience varargs method for collections:
            if (vars.containsKey("elementType")) {
                lines.addAll(rewrite(vars,
                        "/**",
                        "  " + initializinglistmethodComment,
                        "   {treatasprivate}",
                        "   {deprecatedDoc}",
                        "*/",
                        "{deprecatedAnnotation}",
                        "public {retType} {pName}({elementType}... x) {",
                        "return {pName}(java.util.Arrays.asList(x));",
                        "}",
                        ""));
            }
        }

        if (!isAbstract) {
            String constructorArgs = "";
            String sep = "";
            for (String p : scanned.constructorParameters) {
                constructorArgs += sep + "this." + p;
                sep = ", ";
            }
            vars.put("constructorCall", scanned.constructorCall);
            if (!originalTypeParams.isEmpty()) {
                vars.put("constructorTypeArgs", "<" + typeArgs + ">");
            }
            vars.put("constructorArgs", constructorArgs);
            lines.addAll(rewrite(vars,
                    "/**",
                    buildmethodComment,
                    "*/",
                    "public {typeWithoutBounds} build() {",
                        "{typeWithoutBounds} x = {constructorCall}{constructorTypeArgs}({constructorArgs});", // new Font(...) or Font.font(...)
                        "{applyTo(x)}",
                        "return x;",
                    "}"));
        }
        lines.add("}");

        makeClass(builderName, type, lines);
    }

    // This complicated method does two things.  First, it updates |vars| with extra definitions
    // that are appropriate when there will be setter calls at construction time.  Second, it returns
    // a list of lines that declare the applyTo method and everything it needs.
    // The returned list of lines will itself be subject to |rewrite| so it can contain {var} references.
    private List<String> makeApplyTo(
            Map<String, Object> vars, List<String> properties, final Scanned scanned, boolean inheritsSetters) {
        int n = properties.size();
        if (n == 0) {
            return Collections.emptyList();
        }

        // O to have local functions!
        class SetterCall {
            String make(String property) {
                String value = "this." + property;
                String cap = cap(property);
                if (scanned.collectionProperties.contains(property)) {
                    TypeMirror type = scanned.typeOf(property);
                    if (rawObservableListType != null && types.isSubtype(types.erasure(type), rawObservableListType)) {
                        return "x.get" + cap + "().addAll(" + value + ");";
                    } else {
                        return "{ x.get" + cap + "().clear(); x.get" + cap + "().addAll(" + value + "); }";
                    }
                } else {
                    return "x.set" + cap + "(" + value + ");";
                }
            }
        }
        final SetterCall setterCall = new SetterCall();

        final List<String> lines = new ArrayList<String>();
        Collections.addAll(lines,
                "{declareBitset}",
                "{setStart}",
                "{setBody}",
                "{setEnd}",
                "public void applyTo({typeWithoutBounds} x) {");
        if (inheritsSetters) {
            lines.add("super.applyTo(x);");
        }

        vars.put("declareBitset", "private {bitset} __set;");
        if (n == 1) {
            // If there is only one property then we only need a boolean to record whether it has been set.
            vars.put("bitset", "boolean");
            vars.put("setBitI", "__set = true;");
            lines.addAll(rewrite(vars, "if (__set) " + setterCall.make(properties.get(0))));
        } else if (n < 8) {
            // With a small number of properties, we can record them in an int bitset, and use a sequence of ifs
            // to determine which of them has been set.
            vars.put("bitset", "int");
            vars.put("setBitI", "__set |= 1 << {i};");
            lines.add("int set = __set;");
            for (int i = 0; i < n; i++) {
                vars.put("i", i);
                vars.put("set", setterCall.make(properties.get(i)));
                lines.addAll(rewrite(vars,
                        "if ((set & (1 << {i})) != 0) {set}"));
            }
        } else {
            // With many properties, it is cheaper in both code size and time to loop over the bits and determine
            // which ones have been set.
            vars.put("setStart", "private void __set(int i) {");
            vars.put("setBody", "{setBitI}");
            vars.put("setEnd", "}");
            vars.put("setBitI", "__set({i});");
            if (n < 64) {
                if (n < 32) {
                    vars.put("bitset", "int");
                    vars.put("one", "1");
                    vars.put("boxedBits", "Integer");
                } else {
                    vars.put("bitset", "long");
                    vars.put("one", "1L");
                    vars.put("boxedBits", "Long");
                }
                vars.put("setBody", "__set |= {one} << i;");
                vars.put("loop1", "while (set != 0) {");
                vars.put("loop2", "int i = {boxedBits}.numberOfTrailingZeros(set);");
                vars.put("loop3", "set &= ~({one} << i);");
            } else {
                // At the time of writing, no classes had more than 64 properties.  (This doesn't include
                // inherited properties, so 64 is really quite a lot.)
                vars.put("bitset", "java.util.BitSet");
                vars.put("declareBitset", "java.util.BitSet __set = new java.util.BitSet();");
                vars.put("setBody", "__set.set(i);");
                vars.put("loop1", "for (int i = -1; (i = set.nextSetBit(i + 1)) >= 0; ) {");
            }
            lines.addAll(rewrite(vars,
                    "{bitset} set = __set;",
                    "{loop1}",
                    "{loop2}",
                    "{loop3}",
                    "switch (i) {"));
            for (int i = 0; i < n; i++) {
                vars.put("i", i);
                vars.put("set", setterCall.make(properties.get(i)));
                lines.addAll(rewrite(vars,
                        "case {i}: {set} break;"));
            }
            lines.add("default: break; // can never happen");
            lines.add("}"); // switch
            lines.add("}"); // loop
        }

        lines.add("}");
        lines.add("");
        return lines;
    }

    // Output a class definition consisting of the given lines.  We apply indentation here rather than trying to
    // track it as part of the content of the lines.  The indentation algorithm is simplistic and assumes that
    // left braces always appear at the end of a line and right braces at the beginning.  (It will also work
    // if matching braces appear within a line.)
    private void makeClass(String className, TypeElement originatingElement, List<String> lines) throws IOException {
        JavaFileObject jfo = filer.createSourceFile(className, originatingElement);
        Writer w = jfo.openWriter();
        PrintWriter pw = new PrintWriter(w);
        int indent = 0;
        final int spacePerIndent = 4;
        for (String line : lines) {
            if (line.startsWith("}")) {
                indent--;
            }
            for (int i = 0; i < indent * spacePerIndent; i++) {
                pw.print(' ');
            }
            pw.println(line.trim());
            if (line.endsWith("{")) {
                indent++;
            }
        }
        pw.close();
    }

    // Pattern to match substitution variables such as {this}.  We disallow spaces in variable names,
    // so that we can use one-line blocks like {foo(); bar();} and not have them substituted to nothing.
    private static final Pattern varPat = Pattern.compile("\\{[^ ;\\}]*\\}");

    // Use the given variable substitutions to rewrite the given lines, so for example "foo{bar}baz" would
    // be rewritten to "foobuhbaz" if the vars defined bar=buh.  The rewritten can itself include variables,
    // which will themselves be rewritten, and so on until there are no more variables.
    //
    // The reason this operates on lines rather than on one big string is mainly that it's such a pain to
    // type \n everywhere.  But it also simplifies indentation.
    //
    // The purpose of this annotation processor is to provide something like JavaFX Script object literals.
    // The purpose of this function in its implementation is to provide something like JavaFX Script strings.
    private static List<String> rewrite(Map<String, Object> vars, List<String> lines) {
        List<String> resultLines = new ArrayList<String>(lines.size());
        for (String line : lines) {
            String resultLine = line;
            boolean anySubs;
            do {
                anySubs = false;
                for (Map.Entry<String, Object> var : vars.entrySet()) {
                    String newResultLine = resultLine.replace("{" + var.getKey() + "}", var.getValue().toString());
                    anySubs |= !newResultLine.equals(resultLine);
                    resultLine = newResultLine;
                }
            } while (anySubs);
            boolean wasEmpty = resultLine.isEmpty();
            resultLine = varPat.matcher(resultLine).replaceAll("");
            if (!resultLine.trim().isEmpty() || wasEmpty) {
                // Delete lines that consisted only of var substitutions that were absent.
                // That allows us to make vars to declare things some of the time, without leaving
                // mysterious blank lines the rest of the time.
                resultLines.add(resultLine);
            }
        }
        return resultLines;
    }

    private static List<String> rewrite(Map<String, Object> vars, String... lines) {
        return rewrite(vars, Arrays.asList(lines));
    }

    private static String cap(String p) {
        return Character.toUpperCase(p.charAt(0)) + p.substring(1);
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

    private static boolean isAbstract(Scanned scanned) {
        return scanned == null || (scanned.isAbstract && isAbstract(scanned.superScanned));
    }
    
    private static final Set<String> javaKeyWords = new TreeSet<String>(Arrays.asList(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
        "continue", "default", "do", "double", "else", "enum", "extends", "false", "final", "finally", "float",
        "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "null", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "void",
        "volatile", "while"));
}
