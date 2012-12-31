/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.css;

import com.sun.javafx.css.parser.CSSParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.scene.paint.Color;
import org.junit.Test;
import static org.junit.Assert.*;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DeclarationTest {
    
    private static class Data {
        private final Declaration d1, d2;
        private final boolean expected;
        Data(Declaration d1, Declaration d2, boolean expected){
            this.d1 = d1;
            this.d2 = d2;
            this.expected = expected;
        }
        
        @Override public String toString() {
            return "\"" + d1 + "\" " + (expected ? "==" : "!=") + " \"" + d2 + "\"";
        }
    }
    
    public DeclarationTest(Data data) {
        this.data = data;
    }
    private final Data data;
    

    @Parameters
    public static Collection data() {

        int n = 0;
        final int GI = n++; // green inline
        final int YI = n++; // yellow inline
        final int GA1 = n++; // green author 1 
        final int YA1 = n++; // yellow author 1 
        final int GA2 = n++; // green author 2 
        final int YA2 = n++; // yellow author 2 
        
        final Declaration[] DECLS = new Declaration[n];
        
        Stylesheet inlineSS = new Stylesheet() {
            {
                setOrigin(Origin.INLINE);
                
                DECLS[GI] = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YI] = new Declaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);
    
                Collections.addAll(getRules(),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[GI])),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[YI]))
                );
            }
        };
        
        Stylesheet authorSS_1 = new Stylesheet() {
            {
                setOrigin(Origin.AUTHOR);
                
                DECLS[GA1] = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YA1] = new Declaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);
    
                Collections.addAll(getRules(),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[GA1])),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[YA1]))
                );
            }
        };
        
        Stylesheet authorSS_2 = new Stylesheet() {
            {
                setOrigin(Origin.AUTHOR);
                
                DECLS[GA2] = new Declaration("-fx-base", new ParsedValueImpl<Color,Color>(Color.GREEN, null), false);
                DECLS[YA2] = new Declaration("-fx-color", new ParsedValueImpl<Color,Color>(Color.YELLOW, null), false);
    
                Collections.addAll(getRules(),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[GA2])),
                    new Rule(Arrays.asList(SimpleSelector.getUniversalSelector()), Arrays.asList(DECLS[YA2]))
                );
            }
        };
        
        return Arrays.asList(new Object[] {
            new Object[] { new Data(DECLS[GA1], DECLS[GA2], true) },
            new Object[] { new Data(DECLS[GA1], DECLS[YA1], false) },
            new Object[] { new Data(DECLS[GA1], DECLS[GI],  false) }
        });
    }
    
    @Test
    public void testEquals() {

        Declaration instance = data.d1;
        Declaration obj = data.d2;
        boolean expected = data.expected;
        boolean actual = instance.equals(obj);
        assertTrue(data.toString(), expected == actual);
        
    }

}
