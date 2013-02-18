/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.beans.annotations.NoBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
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
    AnnotationUtils.inputOption, AnnotationUtils.outputOption, AnnotationUtils.verboseOption
})
public class BuilderProcessor extends AbstractProcessor {
    private AnnotationUtils utils;

    private final Set<String> builderNames = new HashSet<String>();

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
        utils = new AnnotationUtils(processingEnv);
        utils.note("BuilderProcessor runs");

        for (TypeElement type : ElementFilter.typesIn(roundEnv.getRootElements())) {
            if (isCandidateType(type)) {
                processType(type);
            }
        }
        return false;
    }

    private boolean isCandidateType(TypeElement type) {
        String typeName = type.toString();
        return (!builderNames.contains(typeName) &&
                utils.inputPat.matcher(type.toString()).matches() &&
                !typeName.endsWith("BuilderExtension") &&
                type.getKind() == ElementKind.CLASS &&
                type.getModifiers().contains(Modifier.PUBLIC));
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

        Scanned superScanned = null;
        TypeMirror superMirror = type.getSuperclass();
        if (!(superMirror instanceof NoType)) {
            TypeElement superElement = utils.elements.getTypeElement(utils.types.erasure(superMirror).toString());
            if (superElement != null && isCandidateType(superElement)) {
                superScanned = scan(superElement, true);
            }
        }

        Scanned scanned = Scanned.createScanned(superScanned, type, forSuper, utils);
        scanMap.put(type, scanned);
        return scanned;
    }

    private void processType(TypeElement type) {
        utils.note("BuilderProcessor: process " + type);

        Scanned scanned = scan(type, false);
        Set<String> buildablePropertyNames = scanned.getBuildablePropertyNames();

        if (type.getAnnotation(NoBuilder.class) != null) {
            return;
        }

        // No properties, no builder
        if (buildablePropertyNames.isEmpty() &&
                !scanned.hasInheritedSetters() &&
                !scanned.hasBuilderExtension()) {
            // We still want a builder if the parent has properties, since the generated builder will
            // construct the right type of object even though you will only be able to set the inherited properties.
            //
            // Also the builder methods may be specified by BuilderExtension
            // so we want to construct the builder every time we have a BuilderExtension            
            return;
        }

        try {
            makeBuilder(type, scanned, buildablePropertyNames);
        } catch (IOException e) {
            if (utils.verbose) {
                e.printStackTrace();
            }
            utils.error("Could not make builder for " + type + ": " + e);
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

    private void makeBuilder(TypeElement type, Scanned scanned, Set<String> buildablePropertyNames) throws IOException {
        // Substitution variables that will be used as we construct the output text.  See rewrite method below.
        // If no value is defined for a var then its value is empty; this is error-prone but simplifies the logic
        // a lot.
        final Map<String, Object> vars = new TreeMap<String, Object>();

        final boolean isAbstract = scanned.isAbstract;

        final boolean isFinal = type.getModifiers().contains(Modifier.FINAL);
        // A class can also be "effectively final" if it has no visible constructors, but we've already eliminated
        // such classes since we need a constructor in order to build.

        final boolean inherits = scanned.hasInheritedSetters();
        // We only try to inherit from the superclass's builder if it exists and has setters.  If the superclass
        // is immutable then we are potentially missing a chance to reuse its builder's fields and methods, but
        // that case is rather unusual, and would require the builder to access those fields, which are currently
        // private.

        final Map<String, ExecutableElement> buildableProperties =
                new TreeMap<String, ExecutableElement>(String.CASE_INSENSITIVE_ORDER);
        buildableProperties.putAll(scanned.properties);
        buildableProperties.keySet().retainAll(buildablePropertyNames);

        vars.put("copyrightComment", copyrightComment);
        final String builderName = utils.getBuilderName(type.toString());
        builderNames.add(builderName);
        vars.put("builderName", builderName);
        final String[] tokens = builderName.split("\\.");
        vars.put("shortBuilderName", tokens[tokens.length - 1]);
        final int lastDot = builderName.lastIndexOf('.');
        if (lastDot >= 0) {
            final String pkg = builderName.substring(0, lastDot);
            vars.put("packageDecl", "package " + pkg + ";");
        }
        vars.put("simpleName", builderName.substring(lastDot + 1));

        // we need to know name of builderExtension even for classes without one
        // if their parent class has one
        // we need to put parent builder extension into construtor
        vars.put("builderExtensionName", scanned.getBuilderExtensionName());
        vars.put("builderExtensionAccess","private"); // private for now
        if (scanned.hasBuilderExtension() || scanned.hasInheritedBuilderExtension()) {
            vars.put("createBuilderExtension", "new {builderExtensionName}()");
        }
        if (scanned.hasBuilderExtension() && !scanned.hasInheritedBuilderExtension()) {
            // has builder extension so we need to declare it
            // Disabled for now: (but it is not inherited)
            vars.put("declareBuilderExtension", "{builderExtensionAccess} final {builderExtensionName} be;");
            vars.put("accessBuilderExtension", "be");
        } else {
            vars.put("accessBuilderExtension", "(({builderExtensionName})be)");
        }

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
        final String originalTypeParams = AnnotationUtils.typeParameterString(type);
        final String typeArgs = AnnotationUtils.typeArgumentString(type.asType());
        if (originalTypeParams.isEmpty()) {
            vars.put("type", type.toString());
            vars.put("rawType", type.toString());
            vars.put("typeWithoutBounds", type.toString());
        } else {
            vars.put("createTypeParams", "<" + originalTypeParams + "> ");
            vars.put("type", type + "<" + originalTypeParams + ">");
            vars.put("rawType", type.getQualifiedName().toString());
            vars.put("typeWithoutBounds", type + "<" + typeArgs + ">");
            vars.put("createParams", AnnotationUtils.typedCreateParamString(type));
            String nonTypedArgs = AnnotationUtils.nonTypedCreateArgumentString(type, isFinal);
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
            final String sup = utils.getBuilderName(utils.types.erasure(type.getSuperclass()).toString());
            vars.put("supB", "B");
            final String supTypeParams = AnnotationUtils.typeArgumentString(type.getSuperclass());
            if (supTypeParams.isEmpty()) {
                vars.put("extends", " extends " + sup + "<{supB}>");
            } else {
                vars.put("extends", " extends " + sup + "<" + supTypeParams + ", {supB}>");
            }
        }
        if (!scanned.isAbstract && Scanned.isAbstract(scanned.superScanned)) {
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

        if (scanned.hasBuilderExtension() || scanned.hasInheritedBuilderExtension()) {
            vars.put("constructorBuilderExtension1", "{builderExtensionAccess} {simpleName}({builderExtensionName} be) {");
            if (scanned.hasInheritedBuilderExtension()) {
                vars.put("constructorBuilderExtension2", "super(be);");
                vars.put("assingBuilderExtension", "this({createBuilderExtension});");
            } else {
                vars.put("constructorBuilderExtension2", "this.be = be;");
                vars.put("assingBuilderExtension", "this.be = {createBuilderExtension};");
            }
            vars.put("constructorBuilderExtension3", "}");
        }

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
                 // should be
                 // "{constructorAccess} {simpleName}() {",
                    "{assingBuilderExtension}",
                    "}",
                    "{declareBuilderExtension}",
                    "{constructorBuilderExtension1}",
                    "{constructorBuilderExtension2}",
                    "{constructorBuilderExtension3}",
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
                
        if (scanned.hasBuilderExtension()) {
            List<String> be = processBuilderExtension(vars, scanned);
            lines.addAll(rewrite(vars, be));
        }

        if (!setters.isEmpty() || inherits || scanned.hasBuilderExtension()) {
            List<String> applyTo = makeApplyTo(vars, setters, scanned, inherits);
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
                    AnnotationUtils.hasOneArgument(pType)) {
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
    
    private List<String> processBuilderExtension(Map<String, Object> vars, Scanned scanned) {
        final List<String> lines = new ArrayList<String>();
        
        for (ExecutableElement method : scanned.builderExtensionMethods) {
            final String name = method.getSimpleName().toString();
            String javadoc = utils.elements.getDocComment(method);
            vars.put("javadoc", javadoc != null ? javadoc : "");
            vars.put("methodName", name);
            vars.put("methodTypes", AnnotationUtils.methodParameterString(method));
            vars.put("methodArgs", AnnotationUtils.methodCallParameterString(method));

            if (scanned.builderExtensionProperties.contains(name) &&
                scanned.constructorParameters.contains(name)) {
                    // Special case when we want to change the behavior of 
                    // otherwise constructed method
                    // eg. StageBuilder.style(StageStyle x)
                    // we know it has exactly 1 argument otherwise it would not 
                    // be put into builderExtensionProperties
                    VariableElement el = method.getParameters().get(0);
                    String init = scanned.constructorProperties.get(name);
                    
                    if (init == null || init.isEmpty()) {
                        vars.remove("init");
                    } else {
                        vars.put("init", " = " + init);
                    }
                    vars.put("argName", el.getSimpleName());
                    vars.put("pType", el.asType());
                    vars.put("pName", name);
                    vars.put("constructorPropertyDef", "private {pType} {pName}{init};");
                    vars.put("constructorPropertyAssign", "this.{pName} = {argName};");
            } else {
                vars.remove("constructorPropertyDef");
                vars.remove("constructorPropertyAssign");
            }
            lines.addAll(rewrite(vars,
                    "{constructorPropertyDef}", // for constructor args
                    "/**",
                    " {javadoc}",
                    " */",
                    "{suppressWarnings}",
                    "public B {methodName}({methodTypes}) {",
                        "{constructorPropertyAssign}", // for constructor args
                        "{accessBuilderExtension}.{methodName}({methodArgs});",
                        "return {cast}this;",
                    "}",
                    ""
                    ));
        }
        
        return lines;
    }

    // This complicated method does two things.  First, it updates |vars| with extra definitions
    // that are appropriate when there will be setter calls at construction time.  Second, it returns
    // a list of lines that declare the applyTo method and everything it needs.
    // The returned list of lines will itself be subject to |rewrite| so it can contain {var} references.
    private List<String> makeApplyTo(
            Map<String, Object> vars, List<String> properties, final Scanned scanned, boolean inheritsSetters) {
        int n = properties.size();
        if (n == 0  && !scanned.hasBuilderExtension()) {
            return Collections.emptyList();
        }

        // O to have local functions!
        class SetterCall {
            String make(String property) {
                String value = "this." + property;
                String cap = cap(property);
                if (scanned.collectionProperties.contains(property)) {
                    TypeMirror type = scanned.typeOf(property);
                    if (utils.isObservableList(type)) {
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
        
        // only call applyTo for builderExtension for the top class 
        // in the hierarchy of inherited builders, otherwise it would
        // be called more than once (as builder.applyTo calls super.applyTo())
        if (scanned.hasBuilderExtension() && !scanned.hasInheritedBuilderExtension()) {
            lines.add("be.applyTo(x);");
        }

        if (n > 0) { // empty applyTo for builder extension
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
        JavaFileObject jfo = processingEnv.getFiler().createSourceFile(className, originatingElement);
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
}
