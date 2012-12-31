/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.css;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Implementation details behind a {@link ParsedValueImpl}. 
 */
public class ParsedValueImpl<V, T> extends ParsedValue<V,T> {

     /**
     * If value references another property, then the real value needs to
     * be looked up.
     */
    final private boolean lookup;
    public final boolean isLookup() { return lookup; }

    /**
     * If value is itself a ParsedValueImpl or sequence of values, and should any of
     * those values need to be looked up, then this flag is set. This
     * does not mean that this particular value needs to be looked up, but
     * that this value contains a value that needs to be looked up.
     */
    final private boolean containsLookups;
    public final boolean isContainsLookups() { return containsLookups; }

    private static boolean getContainsLookupsFlag(Object obj) {

        // Assume the value does not contain lookups
        boolean containsLookupsFlag = false;

        if (obj instanceof Size) {
            containsLookupsFlag = false;
        }

        else if(obj instanceof ParsedValueImpl) {
            ParsedValueImpl value = (ParsedValueImpl)obj;
            containsLookupsFlag = value.lookup || value.containsLookups;
        }

        else if(obj instanceof ParsedValueImpl[]) {
            ParsedValueImpl[] values = (ParsedValueImpl[])obj;
            for(int v=0;
                // Bail if value contains lookups
                // Continue iterating as long as one of the flags is false
                v<values.length && !containsLookupsFlag;
                v++)
            {
                if (values[v] != null) {
                    containsLookupsFlag =
                           containsLookupsFlag
                        || values[v].lookup
                        || values[v].containsLookups;
                    }
            }

        } else if(obj instanceof ParsedValueImpl[][]) {
            ParsedValueImpl[][] values = (ParsedValueImpl[][])obj;
            for(int l=0;
                l<values.length && !containsLookupsFlag;
                l++)
            {
                if (values[l] != null) {
                    for(int v=0;
                        v<values[l].length && !containsLookupsFlag;
                        v++)
                    {
                        if (values[l][v] != null) {
                            containsLookupsFlag =
                                   containsLookupsFlag
                                || values[l][v].lookup
                                || values[l][v].containsLookups;
                        }
                    }
                }
            }
        }

        return containsLookupsFlag;
    }

    private final boolean needsFont;
    public boolean isNeedsFont() {
        if (resolved != null && resolved != this) {
            return resolved.needsFont;
        }
        return needsFont;
    }

    private static boolean getNeedsFontFlag(Object obj) {

        // Assume the value does not need a font for conversion
        boolean needsFont = false;

        if (obj instanceof Size) {
            needsFont = ((Size)obj).isAbsolute() == false;
        }

        else if(obj instanceof ParsedValueImpl) {
            ParsedValueImpl value = (ParsedValueImpl)obj;
            needsFont = value.needsFont;
        }

        else if(obj instanceof ParsedValueImpl[]) {
            ParsedValueImpl[] values = (ParsedValueImpl[])obj;
            for(int v=0;
                v<values.length && !needsFont;
                v++)
            {
                if (values[v] == null) continue;
                needsFont = values[v].needsFont;
            }

        } else if(obj instanceof ParsedValueImpl[][]) {
            ParsedValueImpl[][] values = (ParsedValueImpl[][])obj;
            for(int l=0;
                l<values.length && !needsFont;
                l++)
            {
                if (values[l] == null) continue;
                for(int v=0;
                    v<values[l].length && !needsFont;
                    v++)
                {
                    if (values[l][v] == null) continue;
                    needsFont = values[l][v].needsFont;
                }
            }
        }

        return needsFont;
    }

    /**
     * Create an instance of ParsedValueImpl where the value type V is converted to
     * the target type T using the given Type converter. If the value needs
     * If type is null, then it is assumed that the value type V and the target
     * type T are the same (do not need converted). If lookup is true, then
     * the value is another property.
     */
    public ParsedValueImpl(V value, StyleConverter<V, T> converter, boolean lookup) {
        super(value, converter);
        this.lookup = lookup;
        this.containsLookups = lookup || getContainsLookupsFlag(value);
        this.needsFont = getNeedsFontFlag(value);
    }

    /**
     * Create an instance of ParsedValueImpl where the value type V is converted to
     * the target type T using the given Type converter. If the value needs
     * If type is null, then it is assumed that the value type V and the target
     * type T are the same (do not need converted).
     */
    public ParsedValueImpl(V value, StyleConverter<V, T> type) {
        this(value, type, false);
    }

    /*
     * If this ParsedValueImpl object has lookups, then resolved is used to hold
     * the resolved lookup value. This avoids having to allocate new
     * ParsedValueImpl objects in StyleHelper.resolveLookups. The resolved value is 
     * nulled out after this ParsedValueImpl object has been converted in the 
     * StyleHelper.lookup method. 
     */
    ParsedValueImpl resolved;

    /*
     * Null out the resolved field after this ParsedValueImpl object has been converted.
     * Called from StyleHelper.lookup.
     */
    void nullResolved() {

        if (resolved == this || resolved == null) return;

        Object obj = resolved.getValue();
        if(obj instanceof ParsedValueImpl[]) {
            ParsedValueImpl[] values = (ParsedValueImpl[])obj;
            for(int v=0; v<values.length; v++) {
                if (values[v] == null) continue;
                values[v].nullResolved();
            }

        } else if(obj instanceof ParsedValueImpl[][]) {
            ParsedValueImpl[][] values = (ParsedValueImpl[][])obj;
            for(int l=0; l<values.length; l++) {
                if (values[l] == null) continue;
                for(int v=0; v<values[l].length; v++)
                {
                    if (values[l][v] == null) continue;
                    values[l][v].nullResolved();
                }
            }
        }

        resolved = null;
        
    }

    public T convert(Font font) {
        // if this ParsedValueImpl has a resolved lookup, then convert that.
        if (resolved != null && resolved != this) {
            return (T)resolved.convert(font);
        }
        return (T)((converter != null) ? converter.convert(this, font) : value);
    }

    private final static String newline;
    static {
        newline = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
            @Override
                public String run() {
                    return System.getProperty("line.separator");
            }
        });
    }

    private static int indent = 0;

    private static String spaces() {
        return new String(new char[indent]).replace('\0', ' ');
    }

    private static void indent() {
        indent += 2;
    }

    private static void outdent() {
        indent = Math.max(0, indent-2);
    }

    @Override public String toString() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append(spaces())
            .append((lookup? "<Value lookup=\"true\">" : "<Value>"))
            .append(newline);
        indent();
        if (value != null) {
            appendValue(sbuf, value, "value");
        } else {
            appendValue(sbuf, "null", "value");
        }
        if (resolved != null && resolved != this) {
            appendValue(sbuf, resolved, "resolved");
        }
        sbuf.append(spaces())
            .append("<converter>")
            .append(converter)
            .append("</converter>")
            .append(newline);
        outdent();
        sbuf.append(spaces()).append("</Value>").append(newline);
        return sbuf.toString();
    }

    private void appendValue(StringBuilder sbuf, Object value, String tag) {
        if (value instanceof ParsedValueImpl[][]) {
            ParsedValueImpl[][] layers = (ParsedValueImpl[][])value;
            sbuf.append(spaces())
                .append('<')
                .append(tag)
                .append(" layers=\"")
                .append(layers.length)
                .append("\">")
                .append(newline);
            indent();
            for (ParsedValueImpl[] layer : layers) {
                sbuf.append(spaces())
                    .append("<layer>")
                    .append(newline);
                indent();
                if (layer == null) {
                    sbuf.append(spaces()).append("null").append(newline);
                    continue;
                }
                for(ParsedValueImpl val : layer) {
                    if (val == null) {
                        sbuf.append(spaces()).append("null").append(newline);
                    } else {
                    sbuf.append(val);
                }
            }
                outdent();
                sbuf.append(spaces())
                    .append("</layer>")
                    .append(newline);
            }
            outdent();
            sbuf.append(spaces()).append("</").append(tag).append('>').append(newline);

        } else if (value instanceof ParsedValueImpl[]) {
            ParsedValueImpl[] values = (ParsedValueImpl[])value;
            sbuf.append(spaces())
                .append('<')
                .append(tag)
                .append(" values=\"")
                .append(values.length)
                .append("\">")
                .append(newline);
            indent();
            for(ParsedValueImpl val : values) {
                if (val == null) {
                    sbuf.append(spaces()).append("null").append(newline);
                } else {
                sbuf.append(val);
            }
            }
            outdent();
            sbuf.append(spaces()).append("</").append(tag).append('>').append(newline);
        } else if (value instanceof ParsedValueImpl) {
            sbuf.append(spaces()).append('<').append(tag).append('>').append(newline);
            indent();
            sbuf.append(value);
            outdent();
            sbuf.append(spaces()).append("</").append(tag).append('>').append(newline);
        } else {
            sbuf.append(spaces()).append('<').append(tag).append('>');
            sbuf.append(value);
            sbuf.append("</").append(tag).append('>').append(newline);
        }
    }

    @Override public boolean equals(Object obj) {

        if (obj == this) return true;

        if (obj instanceof ParsedValueImpl) {

            final ParsedValueImpl other = (ParsedValueImpl)obj;
            if (this.value instanceof ParsedValueImpl[][]) {

                if (!(other.value instanceof ParsedValueImpl[][])) return false;

                final ParsedValueImpl[][] thisValues = (ParsedValueImpl[][])this.value;
                final ParsedValueImpl[][] otherValues = (ParsedValueImpl[][])other.value;

                // this.value and other.value are known to be non-null
                // due to instanceof
                if (thisValues.length != otherValues.length) return false;

                for (int i = 0; i < thisValues.length; i++) {
                    
                    // if thisValues[i] is null, then otherValues[i] must be null
                    // if thisValues[i] is not null, then otherValues[i] must 
                    // not be null
                    if ((thisValues[i] == null) && (otherValues[i] == null)) continue;
                    else if ((thisValues[i] == null) || (otherValues[i] == null)) return false;
                    
                    if (thisValues[i].length != otherValues[i].length) return false;

                    for (int j = 0; j < thisValues[i].length; j++) {

                        final ParsedValueImpl thisValue = thisValues[i][j];
                        final ParsedValueImpl otherValue = otherValues[i][j];

                        if (thisValue != null
                            ? !thisValue.equals(otherValue)
                            : otherValue != null)
                                return false;
                    }
                }
                return true;

            } else if (this.value instanceof ParsedValueImpl[]) {

                if (!(other.value instanceof ParsedValueImpl[])) return false;

                final ParsedValueImpl[] thisValues = (ParsedValueImpl[])this.value;
                final ParsedValueImpl[] otherValues = (ParsedValueImpl[])other.value;

                // this.value and other.value are known to be non-null
                // due to instanceof
                if (thisValues.length != otherValues.length) return false;

                for (int i = 0; i < thisValues.length; i++) {

                    final ParsedValueImpl thisValue = thisValues[i];
                    final ParsedValueImpl otherValue = otherValues[i];

                    if ((thisValue != null)
                        ? !thisValue.equals(otherValue)
                        : otherValue != null)
                        return false;
                }
                return true;

            } else {

                if (other.value instanceof ParsedValueImpl[][] 
                    || other.value instanceof ParsedValueImpl[]) return false;

                // we know other is not null because of the instanceof check
                return (this.value != null
                        ? this.value.equals(other.value)
                        : other.value == null);
            }
//            Converter could be null, but the values could still match.
//            It makes sense that ParsedValueImpl<String,String>("abc", null) should equal
//            ParsedValueImpl<String,String>("abc", StringConverter.getInstance())
//                    (converter == null ? other.converter == null : converter.equals(other.converter));
        }
        return false;
    }

    private int hc = -1;
    @Override public int hashCode() {
        if (hc == -1) {
            hc = 17;
            if (value instanceof ParsedValueImpl[][]) {
                ParsedValueImpl[][] values = (ParsedValueImpl[][])value;
                for (int i = 0; i < values.length; i++) {
                    for (int j = 0; j < values[i].length; j++) {
                        final ParsedValueImpl val = values[i][j];
                        hc = 37 * hc + ((val != null && val.value != null) ? val.value.hashCode() : 0);
                    }
                }
            } else if (value instanceof ParsedValueImpl[]) {
                ParsedValueImpl[] values = (ParsedValueImpl[])value;
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == null || values[i].value == null) continue;
                    final ParsedValueImpl val = values[i];
                    hc = 37 * hc + ((val != null && val.value != null) ? val.value.hashCode() : 0);
                }
            } else {
                hc = 37 * hc + (value != null ? value.hashCode() : 0);
            }

//            Converter could be null, but the values could still match.
//            It makes sense that ParsedValueImpl<String,String>("abc", null) should equal
//            ParsedValueImpl<String,String>("abc", StringConverter.getInstance())
//            hc = 37 * hc + ((converter != null) ? converter.hashCode() : 1237);
        }
        return hc;
    }


    final static private byte NULL_VALUE = 0;
    final static private byte VALUE = 1;
    final static private byte VALUE_ARRAY = 2;
    final static private byte ARRAY_OF_VALUE_ARRAY = 3;
    final static private byte STRING = 4;
    final static private byte COLOR = 5;
    final static private byte ENUM = 6;
    final static private byte BOOLEAN = 7;
    final static private byte URL = 8;
    final static private byte SIZE = 9;


    public void writeBinary(DataOutputStream os, StringStore stringStore)
        throws IOException {

        os.writeBoolean(lookup);

        if (converter instanceof StyleConverterImpl) {
            os.writeBoolean(true);
            ((StyleConverterImpl)converter).writeBinary(os, stringStore);
        } else {
            os.writeBoolean(false);
            if (converter != null) {
                System.err.println("cannot writeBinary " + converter.getClass().getName());
            }
        }

        if (value instanceof ParsedValue) {
            os.writeByte(VALUE);
            final ParsedValue pv = (ParsedValue)value;
            if (pv instanceof ParsedValueImpl) {
                ((ParsedValueImpl)pv).writeBinary(os, stringStore);
            } else {
                final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                impl.writeBinary(os, stringStore);
            }

        } else if (value instanceof ParsedValue[]) {
            os.writeByte(VALUE_ARRAY);
            final ParsedValue[] values = (ParsedValue[])value;
            if (values != null) {
                os.writeByte(VALUE);
            } else {
                os.writeByte(NULL_VALUE);
            }
            final int nValues = (values != null) ? values.length : 0;
            os.writeInt(nValues);
            for (int v=0; v<nValues; v++) {
                if (values[v] != null) {
                    os.writeByte(VALUE);
                    final ParsedValue pv = values[v];
                    if (pv instanceof ParsedValueImpl) {
                        ((ParsedValueImpl)pv).writeBinary(os, stringStore);
                    } else {
                        final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                        impl.writeBinary(os, stringStore);
                    }
                } else {
                    os.writeByte(NULL_VALUE);
                }
            }

        } else if (value instanceof ParsedValue[][]) {
            os.writeByte(ARRAY_OF_VALUE_ARRAY);
            final ParsedValue[][] layers = (ParsedValue[][])value;
            if (layers != null) {
                os.writeByte(VALUE);
            } else {
                os.writeByte(NULL_VALUE);
            }            
            final int nLayers = (layers != null) ? layers.length : 0;
            os.writeInt(nLayers);
            for (int l=0; l<nLayers; l++) {
                final ParsedValue[] values = layers[l];
                if (values != null) {
                    os.writeByte(VALUE);
                } else {
                    os.writeByte(NULL_VALUE);
                }
                final int nValues = (values != null) ? values.length : 0;
                os.writeInt(nValues);
                for (int v=0; v<nValues; v++) {
                    if (values[v] != null) {
                        os.writeByte(VALUE);
                        final ParsedValue pv = values[v];
                        if (pv instanceof ParsedValueImpl) {
                            ((ParsedValueImpl)pv).writeBinary(os, stringStore);
                        } else {
                            final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                            impl.writeBinary(os, stringStore);
                        }
                    } else {
                        os.writeByte(NULL_VALUE);
                    }
                }
            }

        } else if (value instanceof Color) {
            final Color c = (Color)value;
            os.writeByte(COLOR);
            os.writeLong(Double.doubleToLongBits(c.getRed()));
            os.writeLong(Double.doubleToLongBits(c.getGreen()));
            os.writeLong(Double.doubleToLongBits(c.getBlue()));
            os.writeLong(Double.doubleToLongBits(c.getOpacity()));

        } else if (value instanceof Enum) {
            final Enum e = (Enum)value;
            final int nameIndex = stringStore.addString(e.name());
            final int classIndex = stringStore.addString(e.getClass().getName());
            os.writeByte(ENUM);
            os.writeShort(nameIndex);
            os.writeShort(classIndex);

        } else if (value instanceof Boolean) {
            final Boolean b = (Boolean)value;
            os.writeByte(BOOLEAN);
            os.writeBoolean(b);

        } else if (value instanceof Size) {
            final Size size = (Size)value;
            os.writeByte(SIZE);

            final double sz = size.getValue();
            final long val = Double.doubleToLongBits(sz);
            os.writeLong(val);

            final int index = stringStore.addString(size.getUnits().name());
            os.writeShort(index);

        } else if (value instanceof String) {
            os.writeByte(STRING);
            final int index = stringStore.addString((String)value);
            os.writeShort(index);

        } else if (value instanceof URL) {
            os.writeByte(URL);
            final int index = stringStore.addString(((URL)value).toString());
            os.writeShort(index);

        } else if (value == null) {
            os.writeByte(NULL_VALUE);

        } else {
            throw new InternalError("cannot writeBinary " + this);
        }
    }

    public static ParsedValueImpl readBinary(DataInputStream is, String[] strings)
            throws IOException {

        final boolean lookup = is.readBoolean();
        final boolean hasType = is.readBoolean();

        final StyleConverter converter = (hasType) ? StyleConverterImpl.readBinary(is, strings) : null;

        final int valType = is.readByte();

        if (valType == VALUE) {
            final ParsedValueImpl value = ParsedValueImpl.readBinary(is, strings);
            return new ParsedValueImpl(value, converter, lookup);

        } else if (valType == VALUE_ARRAY) {
            int vtype = is.readByte();
            final int nVals = is.readInt();
            final ParsedValueImpl[] values = (vtype != NULL_VALUE)
                    ? new ParsedValueImpl[nVals]
                    : null;
            for (int v=0; v<nVals; v++) {
                vtype = is.readByte();
                if (vtype == VALUE) {
                    values[v] = ParsedValueImpl.readBinary(is, strings);
                } else {
                    values[v] = null;
                }
            }
            return new ParsedValueImpl(values, converter, lookup);

        } else if (valType == ARRAY_OF_VALUE_ARRAY) {
            int vtype = is.readByte();
            final int nLayers = is.readInt();
            final ParsedValueImpl[][] layers = (vtype != NULL_VALUE)
                    ? new ParsedValueImpl[nLayers][]
                    : null;
            for (int l=0; l<nLayers; l++) {
                vtype = is.readByte();
                final int nVals = is.readInt();
                layers[l] = (vtype != NULL_VALUE)
                    ? new ParsedValueImpl[nVals] 
                    : null;
                for (int v=0; v<nVals; v++) {
                    vtype = is.readByte();
                    if (vtype == VALUE) {
                        layers[l][v] = ParsedValueImpl.readBinary(is, strings);
                    } else {
                        layers[l][v] = null;
                    }
                }
            }
            return new ParsedValueImpl(layers, converter, lookup);

        } else if (valType == COLOR) {
            final double r = Double.longBitsToDouble(is.readLong());
            final double g = Double.longBitsToDouble(is.readLong());
            final double b = Double.longBitsToDouble(is.readLong());
            final double a = Double.longBitsToDouble(is.readLong());
            return new ParsedValueImpl<Color,Color>(Color.color(r, g, b, a), converter, lookup);

        } else if (valType == ENUM) {
            final int nameIndex = is.readShort();
            final int classIndex = is.readShort();
            final String ename = strings[nameIndex];
            final String cname = strings[classIndex];
            ParsedValueImpl value = null;
            try {
                Class eclass = Class.forName(cname);
                value = new ParsedValueImpl(Enum.valueOf(eclass, ename), converter, lookup);
            } catch (ClassNotFoundException cnfe) {
                System.err.println(cnfe.toString());
            } catch (IllegalArgumentException iae) {
                System.err.println(iae.toString());
            } catch (NullPointerException npe) {
                System.err.println(npe.toString());
            }
            return value;

        } else if (valType == BOOLEAN) {
            Boolean b = is.readBoolean();
            return new ParsedValueImpl<Boolean,Boolean>(b, converter, lookup);

        } else if (valType == SIZE) {
            double val = Double.longBitsToDouble(is.readLong());
            SizeUnits units = SizeUnits.PX;
            String unitStr = strings[is.readShort()];
            try {
                units = (SizeUnits)Enum.valueOf(SizeUnits.class, unitStr);
            } catch (IllegalArgumentException iae) {
                System.err.println(iae.toString());
            } catch (NullPointerException npe) {
                System.err.println(npe.toString());
            }
            return new ParsedValueImpl<Size,Size>(new Size(val,units), converter, lookup);

        } else if (valType == STRING) {
            String str = strings[is.readShort()];
            return new ParsedValueImpl(str, converter, lookup);

        } else if (valType == URL) {
            String str = strings[is.readShort()];
            try {
                URL url = new URL(str);
                return new ParsedValueImpl(url, converter, lookup);
            } catch (MalformedURLException malf) {
                throw new InternalError("Excpeption in Value.readBinary: " + malf);
            }

        } else if (valType == NULL_VALUE) {
            return new ParsedValueImpl(null, converter, lookup);

        } else {
            throw new InternalError("unknown type: " + valType);
        }
    }

    public String writeJava() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("new ParsedValueImpl("); 

        // ---- Value
        if (value instanceof ParsedValue) {
            final ParsedValue pv = (ParsedValue)value;
            if (pv instanceof ParsedValueImpl) {
                sb.append(((ParsedValueImpl)pv).writeJava());
            } else {
                final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                sb.append(impl.writeJava());
            }
        } else if (value instanceof ParsedValue[]) {
            final ParsedValue[] values = (ParsedValue[])value;
            sb.append("new ParsedValueImpl[] {");
            
            for (int v=0; v<values.length; v++) {                
                if (values[v] != null) {
                    final ParsedValue pv = values[v];
                    if (pv instanceof ParsedValueImpl) {
                        sb.append(((ParsedValueImpl)pv).writeJava());
                    } else {
                        final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                        sb.append(impl.writeJava());
                    }
                } else {
                    sb.append("null");
                }
                
                if (v < (values.length - 1)) {
                    sb.append(", ");
                }
            }
            
            sb.append("}");
        } else if (value instanceof ParsedValue[][]) {
            final ParsedValue[][] layers = (ParsedValue[][])value;
            sb.append("new ParsedValueImpl[][] {");
            
            for (int l=0; l<layers.length; l++) {
                final ParsedValue[] values = layers[l];
                sb.append("new ParsedValueImpl[] {");
                final int nValues = values != null ? values.length : 0;
                for (int v=0; v<nValues; v++) {
                    if (values[v] != null) {
                        final ParsedValue pv = values[v];
                        if (pv instanceof ParsedValueImpl) {
                            sb.append(((ParsedValueImpl)pv).writeJava());
                        } else {
                            final ParsedValueImpl impl = new ParsedValueImpl(pv.getValue(), pv.getConverter());
                            sb.append(impl.writeJava());
                        }
                    } else {
                        sb.append("null");
                    }
                    
                    if (v < (values.length - 1)) {
                        sb.append(", ");
                    }
                }
                
                sb.append("}");
            }
            
            sb.append("}");
        } else if (value instanceof Color) {
            final Color c = (Color)value;
            sb.append("new Color(");
            sb.append(c.getRed());
            sb.append(", ");
            sb.append(c.getGreen());
            sb.append(", ");
            sb.append(c.getBlue());
            sb.append(", ");
            sb.append(c.getOpacity());
            sb.append(")");
        } else if (value instanceof Enum) {
            final Enum e = (Enum)value;
            sb.append(e.getClass().getName());
            sb.append(".");
            sb.append(e);
        } else if (value instanceof Boolean) {
            final Boolean b = (Boolean)value;
            sb.append(b);
        } else if (value instanceof Size) {
            final Size size = (Size)value;
            final double sz = size.getValue();
            sb.append("new Size(");
            sb.append(sz);
            sb.append(", SizeUnits.");
            sb.append(size.getUnits().name());
            sb.append(")");
        } else if (value instanceof String) {
            String str = (String)value;
            if (! str.startsWith("\"")) sb.append("\"");
            sb.append(str);
            if (! str.endsWith("\"")) sb.append("\"");
        } else if (value instanceof URL) {
            URL url = (URL)value;
            String urlString = url.toString();
            
            // convert all escaped slashes (\\) into \, and then convert all
            // backslashes into escaped versions.
            urlString = urlString.replace("\\\\", "\\");
            urlString = urlString.replace("\\", "\\\\");
            
            sb.append("\"");
            sb.append(urlString);
            sb.append("\"");
        } else if (value == null) {
            sb.append("null");
        } else {
            throw new InternalError("cannot writeJava " + this);
        }
        
        // --- Style Converter
        sb.append(", ");
        String styleConverterString = "null";
        StyleConverter converter = getConverter();
        if (converter instanceof StyleConverterImpl) {
            styleConverterString = ((StyleConverterImpl)converter).writeJava();
            
        }
        sb.append(styleConverterString);
        
        // --- IsLookup
        sb.append(", ");
        sb.append(isLookup());
        
        // close ParsedValueImpl constructor
        sb.append(")"); 
        
        return sb.toString();
    }
}
