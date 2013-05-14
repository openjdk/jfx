/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.IDN;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of static utility methods dealing with "public suffixes".
 */
final class PublicSuffixes {

    private static final Logger logger =
            Logger.getLogger(PublicSuffixes.class.getName());


    /**
     * Public suffix list rule types.
     */
    private enum Rule {
        SIMPLE_RULE,
        WILDCARD_RULE,
        EXCEPTION_RULE,
    }


    /**
     * The mapping from domain names to public suffix list rules.
     */
    private static final Map<String,Rule> RULES =
            loadRules("effective_tld_names.dat");


    /**
     * The private default constructor. Ensures non-instantiability.
     */
    private PublicSuffixes() {
        throw new AssertionError();
    }


    /**
     * Determines if a domain is a public suffix.
     */
    static boolean isPublicSuffix(String domain) {
        if (domain.length() == 0) {
            return false;
        }
        Rule rule = RULES.get(domain);
        if (rule == Rule.EXCEPTION_RULE) {
            return false;
        } else if (rule == Rule.SIMPLE_RULE || rule == Rule.WILDCARD_RULE) {
            return true;
        } else {
            int pos = domain.indexOf('.') + 1;
            if (pos == 0) {
                pos = domain.length();
            }
            String parent = domain.substring(pos);
            return RULES.get(parent) == Rule.WILDCARD_RULE;
        }
    }

    /**
     * Loads the public suffix list from a given resource.
     */
    private static Map<String,Rule> loadRules(String resourceName) {
        logger.log(Level.FINEST, "resourceName: [{0}]", resourceName);
        Map<String,Rule> result = null;

        InputStream is = PublicSuffixes.class.getResourceAsStream(resourceName);
        if (is != null) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                result = loadRules(reader);
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Unexpected error", ex);
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "Unexpected error", ex);
                }
            }
        } else {
            logger.log(Level.WARNING, "Resource not found: [{0}]",
                    resourceName);
        }

        result = result != null
                ? Collections.unmodifiableMap(result)
                : Collections.<String,Rule>emptyMap();
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "result: {0}", toLogString(result));
        }
        return result;
    }

    /**
     * Loads the public suffix list from a given reader.
     */
    private static Map<String,Rule> loadRules(BufferedReader reader)
        throws IOException
    {
        Map<String,Rule> result = new LinkedHashMap<String, Rule>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.split("\\s+", 2)[0];
            if (line.length() == 0) {
                continue;
            }
            if (line.startsWith("//")) {
                continue;
            }
            Rule rule;
            if (line.startsWith("!")) {
                line = line.substring(1);
                rule = Rule.EXCEPTION_RULE;
            } else if (line.startsWith("*.")) {
                line = line.substring(2);
                rule = Rule.WILDCARD_RULE;
            } else {
                rule = Rule.SIMPLE_RULE;
            }
            try {
                line = IDN.toASCII(line, IDN.ALLOW_UNASSIGNED);
            } catch (Exception ex) {
                logger.log(Level.WARNING, String.format(
                        "Error parsing rule: [%s]", line), ex);
                continue;
            }
            result.put(line, rule);
        }
        return result;
    }

    /**
     * Converts a map of rules to a string suitable for displaying
     * in the log.
     */
    private static String toLogString(Map<String,Rule> rules) {
        if (rules.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,Rule> entry : rules.entrySet()) {
            sb.append(String.format("%n    "));
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        return sb.toString();
    }
}
