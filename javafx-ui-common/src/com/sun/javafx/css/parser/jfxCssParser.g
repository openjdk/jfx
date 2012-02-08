// A complete lexer and grammar for CSS 2.1 as defined by the
// W3 specification.
//
// This grammar is free to use providing you retain everything in this header comment
// section.
//
// Author      : Jim Idle, Temporal Wave LLC.
// Contact     : jimi@temporal-wave.com
// Website     : http://www.temporal-wave.com
// License     : ANTLR Free BSD License
//
// Please visit our Web site at http://www.temporal-wave.com and try our commercial
// parsers for SQL, C#, VB.Net and more.
//
// This grammar is free to use providing you retain everything in this header comment
// section.
//
parser grammar jfxCssParser;

options {
    tokenVocab=jfxCssLexer;
}

@header {
    package com.sun.javafx.css.parser;

    import java.util.Map;
    import com.sun.javafx.css.Combinator;
    import com.sun.javafx.css.CompoundSelector;
    import com.sun.javafx.css.Declaration;
    import com.sun.javafx.css.Rule;
    import com.sun.javafx.css.Selector;
    import com.sun.javafx.css.SimpleSelector;
    import com.sun.javafx.css.Stylesheet;
    import com.sun.javafx.css.Value;
}

@members {
    public static boolean DEBUG = false;

    /** Used from CSSParser to force an exit if there was a parsing error if parser is invoked from Css2Bin. */
    public static int exit_status = 0;

    /** All parser errors end up going through reportError, so it is a good place to set exit_status */
    public void reportError(RecognitionException re) {
        exit_status = -1;
        super.reportError(re);
    }

    private Map<String,String> getProperties() {
        return CSSParser.getInstance().getProperties();
    }

    private Stylesheet getStylesheet() {
        return CSSParser.getInstance().getStylesheet();
    }

    private Value valueFor(String property, CSSParser.Term expr)
        throws RecognitionException {
        return CSSParser.getInstance().valueFor(property, expr);
    }

    /** Return false if selector is preceded by a combinator. */
    private boolean esPred() {
        int la = input.LA(1);
        switch (la) {
            case DOT:
            case HASH:
            case LBRACKET:
            case COLON:
                int index = input.index()-1;
                if (index >= 0) {
                    int c = input.get(index).getType();

                    // if c == EOF, then we're at the beginning of the stream
                    // if c is either whitespace or '>', then we have a combinator.
                    // We could have the child combinator surrounded by whitespace,
                    // but it isn't necessary to go back any further than the first whitespace
                    // since we're not looking to match a specific combinator.
                    return (c == EOF || (c != WS && c != GREATER));
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

}            

// -------------
// Main rule.   This is the main entry rule for the parser, the top level
//              grammar rule.
//
// A style sheet consists of an optional character set specification, an optional series
// of imports, and then the main body of style rules.
//
styleSheet 
    :   charSet
        imports*
        bodylist
     EOF
    ;
    
// -----------------
// Character set.   Picks up the user specified character set, should it be present.
//
charSet
    :   CHARSET_SYM STRING SEMI
    |
    ;

// ---------
// Import.  Location of an external style sheet to include in the ruleset.
//
imports
    :   IMPORT_SYM (STRING|URI) (medium (COMMA medium)*)? SEMI
    ;

// ---------
// Media.   Introduce a set of rules that are to be used if the consumer indicates
//          it belongs to the signified medium.
//
media
    : MEDIA_SYM medium (COMMA medium)*
        LBRACE
            ruleSet
        RBRACE
    ;

// ---------    
// Medium.  The name of a medim that are particulare set of rules applies to.
//
medium
    : IDENT 
    ;
    

bodylist
    : bodyset*
    ;
    
bodyset
    : ruleSet
    | media
    | page
    ;   
    
page
    : PAGE_SYM pseudoPage?
        LBRACE
            declaration SEMI (declaration SEMI)*
        RBRACE
    ;
    
pseudoPage
    : COLON IDENT
    ;
    
operator
    : SOLIDUS 
    | COMMA
    |
    ;
    
combinator returns[Combinator value]
    : PLUS
    | GREATER { $value = Combinator.CHILD; }
    | { $value = Combinator.DESCENDANT; }
    ;
    
property
    : IDENT {
            String prop = $IDENT.text;
            getProperties().put(prop, prop);
        }
    ;
    
ruleSet
scope {
    List<Selector> selectors;
    List<Declaration> declarations;
}
@init {
    $ruleSet::selectors = new ArrayList<Selector>();
    $ruleSet::declarations = new ArrayList<Declaration>();
}
@after {
    Rule rule = new Rule($ruleSet::selectors, $ruleSet::declarations);
    if (DEBUG) System.out.println("rule: " + String.valueOf(rule));
    getStylesheet().addRule(rule);
}
    : selector (COMMA selector)*
        LBRACE
            declaration SEMI (declaration SEMI)*
        RBRACE
    ;
    
selector 
    @init {
        List<Combinator> combinators = null;
        List<SimpleSelector> sels = null;
    }
    : ancestor=simpleSelector
      (comb=combinator {
        if (combinators == null) {
            combinators = new ArrayList<Combinator>();
          }
          combinators.add($comb.value);
       }
       sel=simpleSelector {
          if (sels == null) {
              sels = new ArrayList<SimpleSelector>();
              sels.add($ancestor.value);
          }
          sels.add($sel.value);
      })* {
        if (sels == null) {
            $ruleSet::selectors.add(ancestor);
        } else {
            $ruleSet::selectors.add(new CompoundSelector(sels,combinators));
        }
      }
    ;

simpleSelector returns [SimpleSelector value]
    scope {
        String esel; // element selector. default to universal
        String isel; // id selector
        List<String>  csels; // class selector
        List<String> pclasses; // pseudoclasses
    }
    @init {
        $simpleSelector::esel = "*";
        $simpleSelector::isel = null;
        $simpleSelector::csels = new ArrayList<String>();
        $simpleSelector::pclasses = new ArrayList<String>();
    }
    @after {
        $value = new SimpleSelector(
            $simpleSelector::esel,
            $simpleSelector::csels,
            $simpleSelector::pclasses,
            $simpleSelector::isel);
    }

    : elementName ({esPred()}?=>elementSubsequent)*
    | elementSubsequent ({esPred()}?=>elementSubsequent)*
    ;
        
elementSubsequent
    : HASH { $simpleSelector::isel = $HASH.text.substring(1); }
    | cssClass
    | attrib
    | pseudo
    ;
    
cssClass
    : DOT IDENT { $simpleSelector::csels.add($text.substring(1)); }
    ;
    
elementName
    : IDENT{ $simpleSelector::esel = $IDENT.text; }
    | STAR { $simpleSelector::esel = "*"; }
    ;
    
attrib
    : LBRACKET
    
        IDENT
        
            (
                (
                      OPEQ
                    | INCLUDES
                    | DASHMATCH
                )
                (
                      IDENT
                    | STRING
                )       
            )?
    
      RBRACKET
;

pseudo
    : COLON 
            pclass=IDENT
                ( // Function
                
                    LPAREN IDENT? RPAREN
                )?
        { $simpleSelector::pclasses.add($pclass.text); }
    ;

declaration
    : property COLON expr prio? {
        Value value = valueFor($property.text, $expr.value);
        $ruleSet::declarations.add(new Declaration($property.text, value, ($prio.text != null)));
    }
    ;
    
prio
    : IMPORTANT_SYM
    ;
    
expr returns [CSSParser.Term value]
    @init {
        CSSParser.Term curr = null;
    }
    : t0=term 
        {
            curr = $value = $t0.value;
        }
        (operator
             (tN=term
            {
                // comma breaks up sequences of terms.
                if ($operator.start.getType() == COMMA) {
                    // next series of terms chains off the last term in
                    // the current series.
                    curr = curr.nextLayer = $tN.value;
                } else {
                    curr = curr.nextInSeries = $tN.value;
                }
             })
         )*
    ;
    
term returns [CSSParser.Term value]
    @init {
        CSSParser.Term firstArg = null;
        CSSParser.Term arg = null;
    }
    @after {
        $value = new CSSParser.Term($term.start);
        if (firstArg != null) $value.firstArg = firstArg;
    }
    : size
    | STRING
    | IDENT
    | FUNCTION
        t0=term {

                arg = firstArg = $t0.value;
            }
            (operator tN=term {
                // comma breaks up args.
                if ($operator.start.getType() == COMMA) {
                    arg = arg.nextArg = $tN.value;
                } else {
                    arg = arg.nextInSeries = $tN.value;
                }
            }
        )* RPAREN
    | LPAREN
         t0=term {
                arg = firstArg = $t0.value;
            }
            (operator tN=term {
                // comma breaks up args.
                if ($operator.start.getType() == COMMA) {
                    arg = arg.nextArg = $tN.value;
                } else {
                    arg = arg.nextInSeries = $tN.value;
                }
            }
        )* RPAREN

    | hexColor
    ;

hexColor
    : HASH
    ;

size 
    : NUMBER
    | PERCENTAGE
    | EMS
    | EXS
    | PX 
    | CM
    | MM
    | IN
    | PT
    | PC
    ;
