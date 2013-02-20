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

package com.sun.javafx.beans.metadata;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * The base class for all meta-data used for describing JavaBeans, properties,
 * and events.
 *
 * @author Richard
 */
public abstract class MetaData {
    /**
     * A special category name which is used to indicate that this particular
     * JavaBean, Property, or Event is only intended for expert usage. An IDE
     * might group these specially, defaulting them to hidden for example.
     */
    public static final String EXPERT = "Expert";

    /**
     * A special category name which is used to indicate that this particular
     * JavaBean, Property, or Event should be hidden from the user and not
     * exposed via the visual tool.
     */
    public static final String HIDDEN = "Hidden";

    /**
     * A special category name which is used to indicate that this particular
     * JavaBean, Property, or Event is preferred, and should in some way
     * be more prominent.
     */
    public static final String PREFERRED = "Preferred";

    /**
     * The name associated with this JavaBean, Property, or Event. This
     * name is immutable, and <strong>must</strong> be exactly the same
     * as the source file name. This value can only be set by the introspector,
     * and cannot be customized via annotation.
     */
    private String name;

    /**
     * The displayName of the JavaBean, Property, or Event. If prefixed with %,
     * the name will be looked up via a resource bundle.
     * TODO how to spec this? You can have Java based resource bundles, or you
     * can have .properties based resource bundles, or even XML based resource
     * bundles. So the displayName itself might not be useful except with a
     * DesignInfo. Really all this meta-data is design-time related, maybe it
     * should be moved to the design.author package?
     */
    private String displayName;

    /**
     * A short description of the JavaBean, Property, or Event. If prefixed with
     * %, the name will be looked up via a resource bundle.
     */
    private String shortDescription;

    /**
     * The category for this meta-data. This can be any value, including null.
     */
    private String category;

    /**
     * Do not permit anybody outside this package from extending MetaData
     */
    MetaData() { }

    // TODO some additional things from 273: categoryDescription(?),
    // expandByDefault(?). The problem with these is, how to reconcile
    // differences if one property is annotated one way and another property
    // with the same "category" is annotated in another way?

    public final String getName() { return name; }
    public final String getDisplayName() { return displayName; }
    public final String getShortDescription() { return shortDescription; }
    public final String getCategory() { return category; }

    void configure(String name, String displayName,
            String shortDescription, String category) {
        this.name = name;
        this.displayName = displayName;
        this.shortDescription = shortDescription;
        this.category = category;
    }

    /**
     * Utility method to take a string and convert it to normal Java variable
     * name capitalization.  This normally means converting the first
     * character from upper case to lower case, but in the (unusual) special
     * case when there is more than one character and both the first and
     * second characters are upper case, we leave it alone.
     * <p>
     * Thus "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays
     * as "URL".
     *
     * @param name The string to be decapitalized. If null, then null is
     *        returned.
     * @return The decapitalized version of the string.
     */
    static String decapitalize(String name) {
	if (name == null) return name;
        name = name.trim();
        if (name.length() == 0) return name;
	if (name.length() > 1 && Character.isUpperCase(name.charAt(1)) &&
			Character.isUpperCase(name.charAt(0))){
	    return name;
	}
	char chars[] = name.toCharArray();
	chars[0] = Character.toLowerCase(chars[0]);
	return new String(chars);
    }

    /**
     * Takes the given name and formats it for display. Given a camel-case
     * name, it will split the name at each of the upper-case letters, unless
     * multiple uppercase letters are in a series, in which case it treats them
     * as a single name. The initial lower-case letter is upper cased. So a
     * name like "translateX" becomes "Translate X" and a name like "halign"
     * becomes "Halign".
     * <p>
     * Numbers are treated the same as if they were capital letters, such that
     * "MyClass3" would become "My Class 3" and "MyClass23" would become
     * "My Class 23".
     * <p>
     * Underscores are converted to spaces, with the first letter following
     * the underscore converted to upper case. Multiple underscores in a row
     * are treated only as a single space, and any leading or trailing
     * underscores are skipped.
     *
     * @param name
     * @return
     */
    static String toDisplayName(String name) {
	if (name == null) return name;
        // Replace all underscores with empty spaces
        name = name.replace("_", " ");
        // Trim out any leading or trailing space (which also effectively
        // removes any underscores that were leading or trailing, since the
        // above line had converted them all to spaces).
        name = name.trim();
        // If the resulting name is empty, return an empty string
        if (name.length() == 0) return name;
        // There are now potentially spaces already in the name. If, while
        // iterating over all of the characters in the name we encounter a
        // space, then we will simply step past the space and capitalize the
        // following character.
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        StringBuilder builder = new StringBuilder();
        char ch = name.charAt(0);
        builder.append(ch);
        boolean previousWasDigit = Character.isDigit(ch);
        boolean previousWasCapital = !previousWasDigit;
        for (int i=1; i<name.length(); i++) {
            ch = name.charAt(i);
            if ((Character.isUpperCase(ch) && !previousWasCapital) ||
                    (Character.isUpperCase(ch) && previousWasDigit)) {
                builder.append(" ");
                builder.append(ch);
                previousWasCapital = true;
                previousWasDigit = false;
            } else if ((Character.isDigit(ch) && !previousWasDigit) ||
                    (Character.isDigit(ch) && previousWasCapital)) {
                builder.append(" ");
                builder.append(ch);
                previousWasCapital = false;
                previousWasDigit = true;
            } else if (Character.isUpperCase(ch) || Character.isDigit(ch)) {
                builder.append(ch);
            } else if (Character.isWhitespace(ch)) {
                builder.append(" ");
                // There might have been multiple underscores in a row, so
                // we might now have multiple whitespace in a row. Search ahead
                // to the first non-whitespace character.
                ch = name.charAt(++i);
                while (Character.isWhitespace(ch)) {
                    // Note that because we trim the String, it should be
                    // impossible to have trailing whitespace, and thus we
                    // don't have to worry about the ArrayIndexOutOfBounds
                    // condition here.
                    ch = name.charAt(++i);
                }
                builder.append(Character.toUpperCase(ch));
                previousWasDigit = Character.isDigit(ch);
                previousWasCapital = !previousWasDigit;
            } else {
                previousWasCapital = false;
                previousWasDigit = false;
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    /**
     * A utility class which makes it nicer to interact with ResourceBundles.
     * This particular implementation handles looking up resources in the
     * "primary" delegate bundle and then the "secondary" delegate bundle
     * if it didn't find there resource in the primary.
     */
    static final class Resources {
        private ResourceBundle primary;
        private ResourceBundle secondary;
        private String prefix;

        Resources(final Class<?> beanClass) {
            // TODO probably need to make sure these resource bundles NEVER
            // EVER EVER cache their results

            // TODO Need to make sure these resource bundles use the classloader
            // of the beanClass, otherwise things might not be looked up right
            try {
                secondary = ResourceBundle.getBundle(
                        beanClass.getPackage().getName() + ".resources");
            } catch (MissingResourceException ex) { }

            try {
                primary = ResourceBundle.getBundle(
                        beanClass.getCanonicalName() + "Resources");
            } catch (MissingResourceException ex) { }

            prefix = beanClass.getSimpleName() + "_";
        }

        public String get(String key, String defaultValue) {
            // Add some smarts to deal with a leading % correctly
            // If there is a leading %, then we need to strip it off
            // and perform the search differently than otherwise.

            boolean directLookup = key.startsWith("%");
            String k = directLookup ? key.substring(1) : key;

            if (primary != null) {
                try {
                    Object obj = primary.getObject(k);
                    if (obj != null) return obj.toString();
                } catch (MissingResourceException ex) { }
            }

            if (secondary != null) {
                try {
                    Object obj = secondary.getObject(directLookup ? k : prefix + k);
                    return obj.toString();
                } catch (MissingResourceException ex) { }
            }

            return defaultValue;
        }
    }
}
