/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import java.net.URL;

/**
 *
 * @author dgrieve
 */
public class CssError {
 
    public final String getMessage() {
        return message;
    }
    
    public CssError(String message) {
        this.message = message;
    }
    
    protected final String message;

    @Override
    public String toString() {
        return "CSS Error: " + message;
    }

    public final static class StylesheetParsingError extends CssError { 
        
        public StylesheetParsingError(URL url, String message) {
            super(message);
            this.url = url;
        }
        
        public URL getURL() {
            return url;
        }
        
        private final URL url;

        @Override
        public String toString() {
            final String path = url != null ? url.toExternalForm() : "?";
            // TBD: i18n
            return "CSS Error parsing " + path + ": " + message;
        }
        
    }
    
    public final static class InlineStyleParsingError extends CssError { 
        
        public InlineStyleParsingError(Styleable styleable, String message) {
            super(message);
            this.styleable = styleable;
        }
        
        public Styleable getStyleable() {
            return styleable;
        }
        
        private final Styleable styleable;
        
        @Override
        public String toString() {
            final String inlineStyle = styleable.getStyle();
            final String source = styleable.toString();
            // TBD: i18n
            return "CSS Error parsing in-line style \'" + inlineStyle + 
                    "\' from " + source + ": " + message;
        }
    } 
    
    public final static class StringParsingError extends CssError { 
        
        public StringParsingError(String style, String message) {
            super(message);
            this.style = style;
        }
        
        public String getStyle() {
            return style;
        }
        
        private final String style;
        
        @Override
        public String toString() {
            // TBD: i18n
            return "CSS Error parsing \'" + style + ": " + message;
        }
    } 
    
    public final static class PropertySetError extends CssError { 
        
        public PropertySetError(StyleableProperty styleableProperty, 
                Styleable styleable, String message) {
            super(message);
            this.styleableProperty = styleableProperty;
            this.styleable = styleable;
        }
        
        public Styleable getStyleable() {
            return styleable;
        }
        
        public StyleableProperty getProperty() {
            return styleableProperty;
        }
        
        private final StyleableProperty styleableProperty;
        private final Styleable styleable;
        
    }
}     

