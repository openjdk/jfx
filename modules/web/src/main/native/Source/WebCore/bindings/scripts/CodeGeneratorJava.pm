# Copyright (C) Oracle. All rights reserved.

package CodeGeneratorJava;

use File::Path;
use constant FileNamePrefix => "Java";

# Global Variables
my %domExtension = (
    HTMLOptionsCollection => 1,
    KeyboardEvent => 1,
    WheelEvent => 1,
    DOMSelection => 1,
);

my %ambiguousType = (
    Rect => 1, # conflict with MAC base type 
);

my %class2pkg = (
    Map => "java.util",
    HashMap => "java.util",
    ReferenceQueue => "java.lang.ref",
    Attr => "org.w3c.dom",
    CDATASection => "org.w3c.dom",
    CharacterData => "org.w3c.dom",
    Comment => "org.w3c.dom",
    Document => "org.w3c.dom",
    DocumentFragment => "org.w3c.dom",
    DocumentType => "org.w3c.dom",
    DOMConfiguration => "org.w3c.dom",
    DOMError => "org.w3c.dom",
    DOMErrorHandler => "org.w3c.dom",
    DOMException => "org.w3c.dom",
    DOMImplementation => "org.w3c.dom",
    DOMImplementationList => "org.w3c.dom",
    DOMImplementationSource => "org.w3c.dom",
    DOMLocator => "org.w3c.dom",
    DOMStringList => "org.w3c.dom",
    Element => "org.w3c.dom",
    Entity => "org.w3c.dom",
    EntityReference => "org.w3c.dom",
    NamedNodeMap => "org.w3c.dom",
    NameList => "org.w3c.dom",
    Node => "org.w3c.dom",
    NodeList => "org.w3c.dom",
    Notation => "org.w3c.dom",
    ProcessingInstruction => "org.w3c.dom",
    Text => "org.w3c.dom",
    TypeInfo => "org.w3c.dom",
    UserDataHandler => "org.w3c.dom",
    DOMImplementationRegistry => "org.w3c.dom.bootstrap",
    Counter => "org.w3c.dom.css",
    CSS2Properties => "org.w3c.dom.css",
    CSSCharsetRule => "org.w3c.dom.css",
    CSSFontFaceRule => "org.w3c.dom.css",
    CSSImportRule => "org.w3c.dom.css",
    CSSMediaRule => "org.w3c.dom.css",
    CSSPageRule => "org.w3c.dom.css",
    CSSPrimitiveValue => "org.w3c.dom.css",
    CSSRule => "org.w3c.dom.css",
    CSSRuleList => "org.w3c.dom.css",
    CSSStyleDeclaration => "org.w3c.dom.css",
    CSSStyleRule => "org.w3c.dom.css",
    CSSStyleSheet => "org.w3c.dom.css",
    CSSUnknownRule => "org.w3c.dom.css",
    CSSValue => "org.w3c.dom.css",
    CSSValueList => "org.w3c.dom.css",
    DocumentCSS => "org.w3c.dom.css",
    DOMImplementationCSS => "org.w3c.dom.css",
    ElementCSSInlineStyle => "org.w3c.dom.css",
    Rect => "org.w3c.dom.css",
    RGBColor => "org.w3c.dom.css",
    ViewCSS => "org.w3c.dom.css",
    DocumentEvent => "org.w3c.dom.events",
    Event => "org.w3c.dom.events",
    EventException => "org.w3c.dom.events",
    EventListener => "org.w3c.dom.events",
    EventTarget => "org.w3c.dom.events",
    MouseEvent => "org.w3c.dom.events",
    MutationEvent => "org.w3c.dom.events",
    UIEvent => "org.w3c.dom.events",
    HTMLAnchorElement => "org.w3c.dom.html",
    HTMLAppletElement => "org.w3c.dom.html",
    HTMLAreaElement => "org.w3c.dom.html",
    HTMLBaseElement => "org.w3c.dom.html",
    HTMLBaseFontElement => "org.w3c.dom.html",
    HTMLBodyElement => "org.w3c.dom.html",
    HTMLBRElement => "org.w3c.dom.html",
    HTMLButtonElement => "org.w3c.dom.html",
    HTMLCollection => "org.w3c.dom.html",
    HTMLDirectoryElement => "org.w3c.dom.html",
    HTMLDivElement => "org.w3c.dom.html",
    HTMLDListElement => "org.w3c.dom.html",
    HTMLDocument => "org.w3c.dom.html",
    HTMLDOMImplementation => "org.w3c.dom.html",
    HTMLElement => "org.w3c.dom.html",
    HTMLFieldSetElement => "org.w3c.dom.html",
    HTMLFontElement => "org.w3c.dom.html",
    HTMLFormElement => "org.w3c.dom.html",
    HTMLFrameElement => "org.w3c.dom.html",
    HTMLFrameSetElement => "org.w3c.dom.html",
    HTMLHeadElement => "org.w3c.dom.html",
    HTMLHeadingElement => "org.w3c.dom.html",
    HTMLHRElement => "org.w3c.dom.html",
    HTMLHtmlElement => "org.w3c.dom.html",
    HTMLIFrameElement => "org.w3c.dom.html",
    HTMLImageElement => "org.w3c.dom.html",
    HTMLInputElement => "org.w3c.dom.html",
    HTMLIsIndexElement => "org.w3c.dom.html",
    HTMLLabelElement => "org.w3c.dom.html",
    HTMLLegendElement => "org.w3c.dom.html",
    HTMLLIElement => "org.w3c.dom.html",
    HTMLLinkElement => "org.w3c.dom.html",
    HTMLMapElement => "org.w3c.dom.html",
    HTMLMenuElement => "org.w3c.dom.html",
    HTMLMetaElement => "org.w3c.dom.html",
    HTMLModElement => "org.w3c.dom.html",
    HTMLObjectElement => "org.w3c.dom.html",
    HTMLOListElement => "org.w3c.dom.html",
    HTMLOptGroupElement => "org.w3c.dom.html",
    HTMLOptionElement => "org.w3c.dom.html",
    HTMLParagraphElement => "org.w3c.dom.html",
    HTMLParamElement => "org.w3c.dom.html",
    HTMLPreElement => "org.w3c.dom.html",
    HTMLQuoteElement => "org.w3c.dom.html",
    HTMLScriptElement => "org.w3c.dom.html",
    HTMLSelectElement => "org.w3c.dom.html",
    HTMLStyleElement => "org.w3c.dom.html",
    HTMLTableCaptionElement => "org.w3c.dom.html",
    HTMLTableCellElement => "org.w3c.dom.html",
    HTMLTableColElement => "org.w3c.dom.html",
    HTMLTableElement => "org.w3c.dom.html",
    HTMLTableRowElement => "org.w3c.dom.html",
    HTMLTableSectionElement => "org.w3c.dom.html",
    HTMLTextAreaElement => "org.w3c.dom.html",
    HTMLTitleElement => "org.w3c.dom.html",
    HTMLUListElement => "org.w3c.dom.html",
    DOMImplementationLS => "org.w3c.dom.ls",
    LSException => "org.w3c.dom.ls",
    LSInput => "org.w3c.dom.ls",
    LSLoadEvent => "org.w3c.dom.ls",
    LSOutput => "org.w3c.dom.ls",
    LSParser => "org.w3c.dom.ls",
    LSParserFilter => "org.w3c.dom.ls",
    LSProgressEvent => "org.w3c.dom.ls",
    LSResourceResolver => "org.w3c.dom.ls",
    LSSerializer => "org.w3c.dom.ls",
    LSSerializerFilter => "org.w3c.dom.ls",
    DocumentRange => "org.w3c.dom.ranges",
    Range => "org.w3c.dom.ranges",
    RangeException => "org.w3c.dom.ranges",
    DocumentStyle => "org.w3c.dom.stylesheets",
    LinkStyle => "org.w3c.dom.stylesheets",
    MediaList => "org.w3c.dom.stylesheets",
    StyleSheet => "org.w3c.dom.stylesheets",
    StyleSheetList => "org.w3c.dom.stylesheets",
    DocumentTraversal => "org.w3c.dom.traversal",
    NodeFilter => "org.w3c.dom.traversal",
    NodeIterator => "org.w3c.dom.traversal",
    TreeWalker => "org.w3c.dom.traversal",
    AbstractView => "org.w3c.dom.views",
    DocumentView => "org.w3c.dom.views",
    XPathEvaluator => "org.w3c.dom.xpath",
    XPathException => "org.w3c.dom.xpath",
    XPathExpression => "org.w3c.dom.xpath",
    XPathNamespace => "org.w3c.dom.xpath",
    XPathNSResolver => "org.w3c.dom.xpath",
    XPathResult => "org.w3c.dom.xpath"
);

my %classData = (
    Attr => {
        imports => [ "org.w3c.dom.TypeInfo" ],
        stubs => [
            "TypeInfo getSchemaTypeInfo()"
        ],
        javaMethodRename => {
            "getIsId" => "isId"
        }
    },
    CSSRule => {
        createFabric =>
                "        switch (CSSRuleImpl.getTypeImpl(peer)) {\n" .
                "        case STYLE_RULE: return new CSSStyleRuleImpl(peer);\n" .
                "        case CHARSET_RULE: return new CSSCharsetRuleImpl(peer);\n" .
                "        case IMPORT_RULE: return new CSSImportRuleImpl(peer);\n" .
                "        case MEDIA_RULE: return new CSSMediaRuleImpl(peer);\n" .
                "        case FONT_FACE_RULE: return new CSSFontFaceRuleImpl(peer);\n" .
                "        case PAGE_RULE: return new CSSPageRuleImpl(peer);\n" .
                "        }\n"
    },
    CSSFontFaceRule => {
        includes => [ "CSSValue.h" ],
    },
    CSSStyleSheet => {
        includes => [ "CSSImportRule.h" ],
    },
    CSSValue => {
        createFabric =>
                "        switch (CSSValueImpl.getCssValueTypeImpl(peer)) {\n" .
                "        case CSS_PRIMITIVE_VALUE: return new CSSPrimitiveValueImpl(peer);\n" .
                "        case CSS_VALUE_LIST: return new CSSValueListImpl(peer);\n" .
                "        }\n"
    },
    DOMImplementation => {
        stubs => [
            "Object getFeature(String feature, String version)"
        ],
    },
    DOMWindow => {
        imports => [ "org.w3c.dom.views.DocumentView" ],
        stubs => [
            "DocumentView getDocument()"
        ],
        skip => {
            "requestAnimationFrame" => 1,
            "webkitRequestAnimationFrame" => 1, 
            "webkitRequestFileSystem" => 1,
            "webkitResolveLocalFileSystemURL" => 1
        },
        # W3C error: https://www.w3.org/Bugs/Public/show_bug.cgi?id=17434
        javaMethodRename => {
            "getDocument" => "getDocumentEx"
        },
        nativeCPPBody => {
            "addEventListener" => "<ordinal>",
            "removeEventListener" => "<ordinal>"
        }
    },
    Document => {
        imports => [ "org.w3c.dom.DOMConfiguration",
                     "org.w3c.dom.events.DocumentEvent",
                     "org.w3c.dom.views.DocumentView",
                     "org.w3c.dom.xpath.XPathEvaluator"],
        stubs => [
            "boolean getStrictErrorChecking()",
            "void setStrictErrorChecking(boolean strictErrorChecking)",
            "Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException",
            "DOMConfiguration getDomConfig()",
            "void normalizeDocument()",
            # document.documentURI was writable in DOM3 Core, but is read-only in DOM4
            # (see http://www.w3.org/TR/2011/WD-dom-20110915/#document).
            "void setDocumentURI(String documentURI)"
        ],
        skip => {
            "webkitExitPointerLock" => 1,
            "webkitPointerLockElement" => 1
        },
    },
    Element => {
        imports => [ "org.w3c.dom.TypeInfo" ],
        stubs => [
            "void setIdAttribute(String name, boolean isId) throws DOMException",
            "void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException",
            "TypeInfo getSchemaTypeInfo()",
            "void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException"
        ],
        skip => {
            "webkitRequestPointerLock" => 1
        },
    },
    Entity => {
        stubs => [
            "String getInputEncoding()",
            "String getXmlVersion()",
            "String getXmlEncoding()"
        ],
    },
    Event => {
        checkDynamicType => [
# children first (RTTI sequence)!
# an alternative way - call the CPP interface [boolean isUIEvent()], [boolean isMouseEvent()]
# - not work for Mutation and etc event
            "WheelEvent",
            "MouseEvent",
            "KeyboardEvent",
            "UIEvent",
            "MutationEvent"
        ],
        createFabric =>
                "        switch (EventImpl.getCPPTypeImpl(peer)) {\n" .
                "        case TYPE_MouseEvent: return new MouseEventImpl(peer);\n" .
                "        case TYPE_KeyboardEvent: return new KeyboardEventImpl(peer);\n" .
                "        case TYPE_WheelEvent: return new WheelEventImpl(peer);\n" .
                "        case TYPE_UIEvent: return new UIEventImpl(peer);\n" .
                "        case TYPE_MutationEvent: return new MutationEventImpl(peer);\n" .
                "        }\n"
    },
    Node => {
        imports => [ "org.w3c.dom.UserDataHandler" ],
        stubs => [
            "Object getUserData(String key)",
            "Object setUserData(String key, Object data, UserDataHandler handler)",
            "Object getFeature(String feature, String version)"
        ],
        nativeCPPBody => {
            "insertBefore" =>
                "Node *pnewChild = static_cast<Node*>(jlong_to_ptr(newChild));\n" .
                "    return JavaReturn<Node>(env, static_cast<Node*>(jlong_to_ptr(peer))->\n" .
                "        insertBefore(pnewChild\n" .
                "            , static_cast<Node*>(jlong_to_ptr(refChild))\n" .
                "            , JavaException(env, JavaDOMException))\n" .
                "        ? pnewChild : NULL);\n",
            "replaceChild" =>
                "Node *poldChild = static_cast<Node*>(jlong_to_ptr(oldChild));\n" .
                "    return JavaReturn<Node>(env, static_cast<Node*>(jlong_to_ptr(peer))->\n" .
                "        replaceChild(static_cast<Node*>(jlong_to_ptr(newChild))\n" .
                "            , poldChild\n" .
                "            , JavaException(env, JavaDOMException))\n" .
                "        ? poldChild : NULL);\n",
            "removeChild"  =>
                "Node *poldChild = static_cast<Node*>(jlong_to_ptr(oldChild));\n" .
                "    return JavaReturn<Node>(env, static_cast<Node*>(jlong_to_ptr(peer))->\n" .
                "        removeChild(poldChild\n" .
                "            , JavaException(env, JavaDOMException))\n" .
                "        ? poldChild : NULL);\n",
            "appendChild"  =>
                "Node *pnewChild = static_cast<Node*>(jlong_to_ptr(newChild));\n" .
                "    return JavaReturn<Node>(env, static_cast<Node*>(jlong_to_ptr(peer))->\n" .
                "        appendChild(pnewChild\n" .
                "            , JavaException(env, JavaDOMException))\n" .
                "        ? pnewChild : 0L);\n",
        },
        createFabric =>
                "        switch (NodeImpl.getNodeTypeImpl(peer)) {\n" .
                "        case ELEMENT_NODE :\n" .
                "               if( !ElementImpl.isHTMLElementImpl(peer))\n" .
                "                   return new ElementImpl(peer);\n" .
                "               else {\n" .
                "                   String tagName = ElementImpl.getTagNameImpl(peer).toUpperCase();\n" .
                "                   if (\"A\".equals(tagName)) return new HTMLAnchorElementImpl(peer);\n" .
                "                   if (\"APPLET\".equals(tagName)) return new HTMLAppletElementImpl(peer);\n" .
                "                   if (\"AREA\".equals(tagName)) return new HTMLAreaElementImpl(peer);\n" .
                "                   if (\"BASE\".equals(tagName)) return new HTMLBaseElementImpl(peer);\n" .
                "                   if (\"BASEFONT\".equals(tagName)) return new HTMLBaseFontElementImpl(peer);\n" .
                "                   if (\"BODY\".equals(tagName)) return new HTMLBodyElementImpl(peer);\n" .
                "                   if (\"BR\".equals(tagName)) return new HTMLBRElementImpl(peer);\n" .
                "                   if (\"BUTTON\".equals(tagName)) return new HTMLButtonElementImpl(peer);\n" .
                "                   if (\"DIR\".equals(tagName)) return new HTMLDirectoryElementImpl(peer);\n" .
                "                   if (\"DIV\".equals(tagName)) return new HTMLDivElementImpl(peer);\n" .
                "                   if (\"DL\".equals(tagName)) return new HTMLDListElementImpl(peer);\n" .
                "                   if (\"FIELDSET\".equals(tagName)) return new HTMLFieldSetElementImpl(peer);\n" .
                "                   if (\"FONT\".equals(tagName)) return new HTMLFontElementImpl(peer);\n" .
                "                   if (\"FORM\".equals(tagName)) return new HTMLFormElementImpl(peer);\n" .
                "                   if (\"FRAME\".equals(tagName)) return new HTMLFrameElementImpl(peer);\n" .
                "                   if (\"FRAMESET\".equals(tagName)) return new HTMLFrameSetElementImpl(peer);\n" .
                "                   if (\"HEAD\".equals(tagName)) return new HTMLHeadElementImpl(peer);\n" .
                "                   if (tagName.length() == 2 && tagName.charAt(0)==\'H\' && tagName.charAt(1) >= \'1\' && tagName.charAt(1) <= \'6\') return new HTMLHeadingElementImpl(peer);\n" .
                "                   if (\"HR\".equals(tagName)) return new HTMLHRElementImpl(peer);\n" .
                "                   if (\"IFRAME\".equals(tagName)) return new HTMLIFrameElementImpl(peer);\n" .
                "                   if (\"IMG\".equals(tagName)) return new HTMLImageElementImpl(peer);\n" .
                "                   if (\"INPUT\".equals(tagName)) return new HTMLInputElementImpl(peer);\n" .
                "                   if (\"LABEL\".equals(tagName)) return new HTMLLabelElementImpl(peer);\n" .
                "                   if (\"LEGEND\".equals(tagName)) return new HTMLLegendElementImpl(peer);\n" .
                "                   if (\"LI\".equals(tagName)) return new HTMLLIElementImpl(peer);\n" .
                "                   if (\"LINK\".equals(tagName)) return new HTMLLinkElementImpl(peer);\n" .
                "                   if (\"MAP\".equals(tagName)) return new HTMLMapElementImpl(peer);\n" .
                "                   if (\"MENU\".equals(tagName)) return new HTMLMenuElementImpl(peer);\n" .
                "                   if (\"META\".equals(tagName)) return new HTMLMetaElementImpl(peer);\n" .
                "                   if (\"INS\".equals(tagName) || \"DEL\".equals(tagName)) return new HTMLModElementImpl(peer);\n" .
                "                   if (\"OBJECT\".equals(tagName)) return new HTMLObjectElementImpl(peer);\n" .
                "                   if (\"OL\".equals(tagName)) return new HTMLOListElementImpl(peer);\n" .
                "                   if (\"OPTGROUP\".equals(tagName)) return new HTMLOptGroupElementImpl(peer);\n" .
                "                   if (\"OPTION\".equals(tagName)) return new HTMLOptionElementImpl(peer);\n" .
                "                   if (\"P\".equals(tagName)) return new HTMLParagraphElementImpl(peer);\n" .
                "                   if (\"PARAM\".equals(tagName)) return new HTMLParamElementImpl(peer);\n" .
                "                   if (\"PRE\".equals(tagName)) return new HTMLPreElementImpl(peer);\n" .
                "                   if (\"Q\".equals(tagName)) return new HTMLQuoteElementImpl(peer);\n" .
                "                   if (\"SCRIPT\".equals(tagName)) return new HTMLScriptElementImpl(peer);\n" .
                "                   if (\"SELECT\".equals(tagName)) return new HTMLSelectElementImpl(peer);\n" .
                "                   if (\"STYLE\".equals(tagName)) return new HTMLStyleElementImpl(peer);\n" .
                "                   if (\"CAPTION\".equals(tagName)) return new HTMLTableCaptionElementImpl(peer);\n" .
                "                   if (\"TD\".equals(tagName)) return new HTMLTableCellElementImpl(peer);\n" .
                "                   if (\"COL\".equals(tagName)) return new HTMLTableColElementImpl(peer);\n" .
                "                   if (\"TABLE\".equals(tagName)) return new HTMLTableElementImpl(peer);\n" .
                "                   if (\"TR\".equals(tagName)) return new HTMLTableRowElementImpl(peer);\n" .
                "                   if (\"THEAD\".equals(tagName) || \"TFOOT\".equals(tagName) || \"TBODY\".equals(tagName)) return new HTMLTableSectionElementImpl(peer);\n" .
                "                   if (\"TEXTAREA\".equals(tagName)) return new HTMLTextAreaElementImpl(peer);\n" .
                "                   if (\"TITLE\".equals(tagName)) return new HTMLTitleElementImpl(peer);\n" .
                "                   if (\"UL\".equals(tagName)) return new HTMLUListElementImpl(peer);\n" .
                "               }\n" .
                "               return new HTMLElementImpl(peer);\n" .
                "        case ATTRIBUTE_NODE: return new AttrImpl(peer);\n" .
                "        case TEXT_NODE: return new TextImpl(peer);\n" .
                "        case CDATA_SECTION_NODE: return new CDATASectionImpl(peer);\n" .
                "        case ENTITY_REFERENCE_NODE: return new EntityReferenceImpl(peer);\n" .
                "        case ENTITY_NODE: return new EntityImpl(peer);\n" .
                "        case PROCESSING_INSTRUCTION_NODE: return new ProcessingInstructionImpl(peer);\n" .
                "        case COMMENT_NODE: return new CommentImpl(peer);\n" .
                "        case DOCUMENT_NODE:\n" .
                "               if( DocumentImpl.isHTMLDocumentImpl(peer))\n" .
                "                   return new HTMLDocumentImpl(peer);\n" .
                "               return new DocumentImpl(peer);\n" .
                "        case DOCUMENT_TYPE_NODE: return new DocumentTypeImpl(peer);\n" .
                "        case DOCUMENT_FRAGMENT_NODE: return new DocumentFragmentImpl(peer);\n" .
                "        case NOTATION_NODE: return new NotationImpl(peer);\n" .
                "        }\n"
    },
    Range => {
        nativeCPPBody => {
            "compareBoundaryPoints" =>
                "return static_cast<Range*>(jlong_to_ptr(peer))->\n" .
                "    compareBoundaryPoints(\n" .
                "          static_cast<Range::CompareHow>(how)\n" .
                "        , static_cast<Range*>(jlong_to_ptr(sourceRange))\n" .
                "        , JavaException(env, JavaDOMException));\n"
        }
    },
    CSSStyleDeclaration => {
        nativeCPPBody => {
            "getPropertyCSSValue" => "<ordinal>"
        }
    },
    StyleSheet => {
        checkDynamicType => [
# children first (RTTI sequence)!
# an alternative way - call the CPP interface [boolean isCSSStyleSheet()]
            "CSSStyleSheet",
        ],
        createFabric =>
                "        switch (StyleSheetImpl.getCPPTypeImpl(peer)) {\n" .
                "        case TYPE_CSSStyleSheet: return new CSSStyleSheetImpl(peer);\n" .
                "        }\n"
    },
    Text => {
        stubs => [ "boolean isElementContentWhitespace()" ],
    },
    HTMLCollection => {
        checkDynamicType => [
# children first (RTTI sequence)!
            "HTMLOptionsCollection",
        ],
        createFabric =>
                "        switch (HTMLCollectionImpl.getCPPTypeImpl(peer)) {\n" .
                "        case TYPE_HTMLOptionsCollection: return new HTMLOptionsCollectionImpl(peer);\n" .
                "        }\n",
        nativeCPPBody => {
            "namedItem" => "<ordinal>"
        }
    },
    HTMLDocument => {
        nativeCPPBody => {
            "open" => "<ordinal>",
            "write" => "<ordinal>",
            "writeln" => "<ordinal>"
        }
    },
    HTMLAppletElement => {
        javaStringAttribute => {
            "vspace" => 1,
            "hspace" => 1
        }
    },
    HTMLBaseFontElement => {
        javaStringAttribute => {
            "size" => 1
        }
    },
    HTMLImageElement => {
        javaStringAttribute => {
            "width" => 1,
            "height" => 1,
            "vspace" => 1,
            "hspace" => 1
        },
        stubs => [
            "void setLowSrc(String lowSrc)",
            "String getLowSrc()",
        ]
    },
    HTMLInputElement => {
        javaStringAttribute => {
            "size" => 1
        }
    },
    HTMLObjectElement=> {
        javaStringAttribute => {
            "vspace" => 1,
            "hspace" => 1
        }
    },
);

my %hash_def = ();
my %importsJava = ();
my %includesCPP = ();
my %methodSig = ();
my @headerJava = ();
my @contentJava = ();
my @headerCPP = ();
my @contentCPP = ();


my $module = "";

# Default constructor
sub new
{
    my $object = shift;
    my $reference = { };

    $codeGenerator = shift;
    shift; # $useLayerOnTop
    shift; # $preprocessor
    shift; # $writeDependencies

    bless($reference, $object);
    return $reference;
}

# Params: 'idlDocument' struct
sub GenerateModule
{
    my $object = shift;
    my $dataNode = shift;

    $module = $dataNode->module;
}

# Params: 'domClass' struct
sub GenerateInterface
{
    my $object = shift;
    my $interface = shift;
    my $defines = shift;

    $codeGenerator->LinkOverloadedFunctions($interface);

    # Start actual generation
    # if (!$interface->isCallback) {
    # Start actual generation.
        $object->Generate($interface, $defines);
    # }
}

sub GetTransferTypeName
{
    my $name = shift;

    # special cases
    return "String" if $codeGenerator->IsStringType($name) or $name eq "SerializedScriptValue";
    return "JSObject" if $name eq "DOMObject";
    return "long" if $name eq "DOMTimeStamp";
    return "boolean" if $name eq "boolean";
    return $name if $codeGenerator->IsPrimitiveType($name);

    return "${name}Impl";
}

sub GetJavaInterface
{
    my $interfaceName = shift;
    # W3C spec error
    return "AbstractView" if $interfaceName eq "DOMWindow";
    return $interfaceName;
}

sub GetCPPInterface
{
    my $interfaceName = shift;
    # W3C spec error
    return "DOMWindow" if $interfaceName eq "AbstractView";
    $interfaceName =~ s/Impl$//;
    return $interfaceName;
}


sub ForbiddenTransferTypeName
{
    my $name = shift;

    # special cases
    return 0 if $codeGenerator->IsStringType($name) or $name eq "SerializedScriptValue";
    return 0 if $name eq "DOMTimeStamp";
    return 0 if $name eq "boolean";
    return 0 if $codeGenerator->IsPrimitiveType($name);
    return 0 if $class2pkg{GetJavaInterface($name)};
    return 0 if $domExtension{$name};

    return 1;
}


sub IsReadonly
{
    my $attribute = shift;
    return $attribute->isReadOnly && !$attribute->signature->extendedAttributes->{"Replaceable"};
}

sub IsJavaPimitive
{
    my $type = shift;
    return 1 if $codeGenerator->IsPrimitiveType($type) or ($type eq "String");
    return 0;
}

sub GetJavaParamClassName
{
    my $name = shift;

    # special cases
    $name = "boolean" if $name eq "boolean";

    $name = "short" if $name eq "unsigned short";
    $name = "short" if $name eq "CompareHow";
    $name = "int"   if $name eq "long";
    $name = "int"   if $name eq "unsigned long";
    $name = "long"  if $name eq "unsigned int";
    $name = "long"  if $name eq "long long";
    $name = "long"  if $name eq "Date" or $name eq "DOMTimeStamp";

    $name = "String" if $codeGenerator->IsStringType($name) or $name eq "SerializedScriptValue";
    $name = "JSObject" if $name eq "DOMObject";
    $name = "DOMException" if $name eq "DOMExceptionJSC";
    if (!IsJavaPimitive($name)) {
        $includesCPP{$name . ".h"} = 1;
        $name = GetJavaInterface($name);
        my $pkg = $class2pkg{$name};
        if ($pkg) {
            $importsJava{"${pkg}.${name}"} = 1;
        } else {
            $name = GetTransferTypeName($name);
        }
    }
    return $name;
}

sub GetParent
{
    my $dataNode = shift;
    my $numParents = @{$dataNode->parents};

    my $parent;
    if ($numParents eq 0) {
        $parent = "";
    } elsif ($numParents eq 1) {
        my $parentName = $dataNode->parents(0);
        $parent = $parentName;
    } else {
        my @parents = @{$dataNode->parents};
        my $firstParent = shift(@parents);
        $parent = $firstParent;
    }
    return "$parent";
}

sub isSkipSignature
{
    my $interfaceName = shift;
    my $signatureName = shift;

    return 1 if $classData{$interfaceName}->{skip} and $classData{$interfaceName}->{skip}{$signatureName};
    return 0;
}


sub ShouldSkipType
{
    my $interfaceName = shift;
    my $typeInfo = shift;

    return 1 if isSkipSignature($interfaceName, $typeInfo->signature->name);
    return 1 if $typeInfo->signature->extendedAttributes->{"Custom"} and !GetCustomCPPMethodBody($interfaceName, $typeInfo->signature->name);

    return 1 if $typeInfo->signature->extendedAttributes->{"CustomArgumentHandling"}
             or $typeInfo->signature->extendedAttributes->{"CustomGetter"}
             or $typeInfo->signature->extendedAttributes->{"CPPCustom"};

    # FIXME: We don't generate bindings for SVG related interfaces yet
    return 1 if $typeInfo->signature->name =~ /getSVGDocument/;

    return 1 if $typeInfo->signature->name =~ /Constructor/;
    return 1 if ForbiddenTransferTypeName($typeInfo->signature->type);
    return 0;
}

sub GetExceptionSuffix
{
    my $exceptionArray = shift;
    my $exceptionSuffix = "";
    if (@{$exceptionArray}) {
        foreach my $ex (@{$exceptionArray}) {
            $exceptionSuffix .= ", " if $exceptionSuffix ne "";
            $exceptionSuffix .= GetJavaParamClassName($ex);
        }
        $exceptionSuffix = " throws " . $exceptionSuffix;
    }
    return $exceptionSuffix;
}

sub GetExceptionCPPParam
{
    my $exceptionArray = shift;
    my $exceptionSuffix = "";
    if (@{$exceptionArray}) {
        foreach my $ex (@{$exceptionArray}) {
            $exceptionSuffix .= ", " if $exceptionSuffix ne "";
            $exceptionSuffix .= "Java" . GetJavaParamClassName($ex);
        }
        $exceptionSuffix = "JavaException(env, "
                . $exceptionSuffix
                . ")";
    }
    return $exceptionSuffix;
}


sub GetJavaToNativeType
{
    my $argType = shift;

    # numbers + Strings
    return $argType if IsJavaPimitive($argType);

    #interface
    return "long";
}

sub GetJavaToNativeArgValue
{
    my $argType = GetCPPInterface(shift);
    my $argValue = shift;
    return $argValue if IsJavaPimitive($argType);
    return "EventListenerImpl.getPeer($argValue)" if $argType eq "EventListener";

    #interface wrapper
    #FIXME: EventTarget could be out of DOM/DOMWindow
    return "NodeImpl.getPeer((NodeImpl)${argValue})" if $argType eq "EventTarget";
    return GetTransferTypeName($argType) . ".getPeer(${argValue})";
}

sub GetJavaToNativeReturnValue
{
    my $returnType = GetCPPInterface(shift);
    my $twkFunctionCall = shift;

    return $twkFunctionCall if IsJavaPimitive($returnType);

    # interface
    my $interfaceCast = "";
    if ($returnType eq "EventTarget") {
        $returnType = "Node";
        $interfaceCast = "(EventTarget)";
    }
    if ($returnType eq "ProcessingInstruction") {
        $interfaceCast = "(ProcessingInstruction)";
    }
    return $interfaceCast . GetTransferTypeName($returnType) . ".getImpl("
           . $twkFunctionCall
           . ")";
}

sub isStringAttribute
{
    my $interfaceName = shift;
    my $attributeName = shift;

    return 1 if $classData{$interfaceName}->{javaStringAttribute} and $classData{$interfaceName}->{javaStringAttribute}{$attributeName};
    return 0;
}

sub GetJavaCPPNativeType
{
    my $argType = shift;

    return "void" if $argType eq "void";
    return "jstring" if $argType eq "String";
    return "j${argType}" if IsJavaPimitive($argType);

    # interface
    return "jlong";
}

sub GetJavaCPPNativeArgValue
{
    my $argType = shift;
    my $argValue = shift;

    return "String(env, $argValue)" if $argType eq "String";
    return $argValue if IsJavaPimitive($argType);

    # interface
    $argType = GetCPPInterface($argType);
    return "static_cast<${argType}*>(jlong_to_ptr(${argValue}))";
}

sub GetJavaCPPNativeReturnValue
{
    my $returnType = shift;
    my $nativeFunctionCall = shift;

    #return $nativeFunctionCall . ".toJavaString(env).releaseLocal()" if $returnType eq "String";
    return $nativeFunctionCall if IsJavaPimitive($returnType) and $returnType ne "String";

    # interface
    $returnType = GetCPPInterface($returnType);
    return "JavaReturn<$returnType>(env, " . $nativeFunctionCall . ")";
}

sub GetJavaTWKCallName
{
    my $interfaceCallName = shift;

    # return "twk" . ucfirst($interfaceCallName);
    return $interfaceCallName . "Impl";
}

sub IsDefinedInCommandLine
{
    my $conditional = shift;
    if ($conditional) {
        my $operator = ($conditional =~ /&/ ? '&' : ($conditional =~ /\|/ ? '|' : ''));
        if ($operator) {
            # Avoid duplicated conditions.
            my %conditions;
            map { $conditions{$_} = 1 } split('\\' . $operator, $conditional);
            my $exeCond = '$hash_def{\"ENABLE_'
                . join(
                    "\"} $operator$operator"
                    . '$hash_def{\"ENABLE_',
                    sort keys %conditions)
                . '\"}';
            print "eval: ${exeCond}";
            return eval($exeCond);
        }
        if (!$hash_def{"ENABLE_${conditional}"}) {
            print "skip, ENABLE_${conditional} undefined\n";
        }
        return ($hash_def{"ENABLE_${conditional}"} ? 1 : 0);
    }
    return 1;
}

sub GetJavaMethodName
{
    my $interfaceName = shift;
    my $methodName = shift;

    if ($classData{$interfaceName}->{javaMethodRename}) {
        my $newMethodName = $classData{$interfaceName}->{javaMethodRename}{$methodName};
        $methodName = $newMethodName if $newMethodName;
    }
    $methodName .= "Ex" if $methodSig{$methodName};
    return $methodName;
}

sub GetCustomCPPMethodBody
{
    my $interfaceName = shift;
    my $methodName = shift;

    if ($classData{$interfaceName}->{nativeCPPBody}) {
        return $classData{$interfaceName}->{nativeCPPBody}{$methodName};
    }
    return undef;
}


sub Generate
{
    my $object = shift;
    my $dataNode = shift;
    my $defines = shift;
    %hash_def = map { split(/=/, $_) } split(/ +/, $defines);

    # print the hash

    # while ( my ($key, $val) = each %hash_def ) {
    #   print "$key->$val\n";
    # }

    # with module name
    my $interfaceName = $dataNode->name;
    my $javaInterfaceName = GetJavaInterface($dataNode->name);
    my $javaClassName = GetTransferTypeName($dataNode->name);

    my $javaParentClassName = GetParent($dataNode);

    my $numConstants = @{$dataNode->constants};
    my $numAttributes = @{$dataNode->attributes};
    my $numFunctions = @{$dataNode->functions};

    # package setup
    push(@headerJava, "package com.sun.webkit.dom;\n\n");

    # define CPP-gen consts
    my $functionNamePrefix = "JNICALL Java_com_sun_webkit_dom_";
    my $functionExportPrefix = "JNIEXPORT";
    my $functionStdParams = "JNIEnv* env, jclass clazz, jlong peer";
    my $functionStdSelfParams = "JNIEnv* env, jobject self, jlong peer";

    # imports
    my $isBaseClass = $javaParentClassName eq "";
    if ($isBaseClass) {
        $importsJava{"com.sun.webkit.Disposer"} = 1;
        $importsJava{"com.sun.webkit.DisposerRecord"} = 1;
    } else {
        $javaParentClassName = GetTransferTypeName($javaParentClassName);
    }
    my $isBaseEventTarget = ($isBaseClass and $dataNode->extendedAttributes->{"EventTarget"});
    if ($isBaseEventTarget) {
        $importsJava{"com.sun.webkit.dom.JSObject"} = 1;
        $importsJava{"org.w3c.dom.events.EventTarget"} = 1;
        $includesCPP{"EventTarget.h"} = 1;
        $javaParentClassName = "JSObject";
    }
    if ($javaClassName eq "CharacterDataImpl" || $javaClassName eq "ProcessingInstructionImpl") {
        $importsJava{"org.w3c.dom.Node"} = 1;
    }

    my $pkg = $class2pkg{$javaInterfaceName};
    $importsJava{"${pkg}.${javaInterfaceName}"} = 1 if $pkg;
    $includesCPP{$interfaceName . ".h"} = 1;
    map { $includesCPP{$_} = 1; } @{$classData{$interfaceName}->{includes}};
    map { $importsJava{$_} = 1; } @{$classData{$interfaceName}->{imports}};

    push(@contentJava, "public class ${javaClassName}");
    push(@contentJava, " extends ${javaParentClassName}") if $javaParentClassName;
    push(@contentJava, " implements ${javaInterfaceName}") if $pkg;
    push(@contentJava, ", EventTarget") if $isBaseEventTarget and $pkg;
    push(@contentJava, ", XPathEvaluator, DocumentView, DocumentEvent") if $interfaceName eq "Document";
    push(@contentJava, " {\n");

    # Common header
    my $instanceType = GetJavaParamClassName($interfaceName);
    # adjust the inatnce type for know exceptions

    if ($isBaseClass) {
        if ($isBaseEventTarget) {
            map { $includesCPP{$_} = 1; } @{[
                "APICast.h",
                "com_sun_webkit_dom_JSObject.h"
           ]};

            push(@contentJava, "    // We use a custom hash-table rather than java.util.HashMap,\n");
            push(@contentJava, "    // because the latter requires 2 extra objects for each entry:\n");
            push(@contentJava, "    // a Long for the key plus a Map.Entry.  Since we have a 'next'\n");
            push(@contentJava, "    // field already in the SelfDisposer, we can use it as the entry.\n");
            push(@contentJava, "    private static SelfDisposer[] hashTable = new SelfDisposer[64];\n");
            push(@contentJava, "    private static int hashCount;\n\n");

            push(@contentJava, "    private static int hashPeer(long peer) {\n");
            push(@contentJava, "        return (int) (~peer ^ (peer >> 7)) & (hashTable.length-1);\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    private static ${instanceType} getCachedImpl(long peer) {\n");
            push(@contentJava, "        if (peer == 0)\n");
            push(@contentJava, "            return null;\n");
            push(@contentJava, "        int hash = hashPeer(peer);\n");
            push(@contentJava, "        SelfDisposer head = hashTable[hash];\n");
            push(@contentJava, "        SelfDisposer prev = null;\n");
            push(@contentJava, "        for (SelfDisposer disposer = head; disposer != null;) {\n");
            push(@contentJava, "            SelfDisposer next = disposer.next;\n");
            push(@contentJava, "            if (disposer.peer == peer) {\n");
            push(@contentJava, "                $javaClassName node = ($javaClassName) disposer.get();\n");
            push(@contentJava, "                if (node != null) {\n");
            push(@contentJava, "                    // the peer need to be deref'ed!\n");
            push(@contentJava, "                    $javaClassName.dispose(peer);\n");
            push(@contentJava, "                    return node;\n");
            push(@contentJava, "                }\n");
            push(@contentJava, "                if (prev != null)\n");
            push(@contentJava, "                    prev.next = next;\n");
            push(@contentJava, "                else\n");
            push(@contentJava, "                    hashTable[hash] = next;\n");
            push(@contentJava, "                break;\n");
            push(@contentJava, "            }\n");
            push(@contentJava, "            prev = disposer;\n");
            push(@contentJava, "            disposer = next;\n");
            push(@contentJava, "        }\n");
            push(@contentJava, "        ${javaClassName} node = (${javaClassName})createInterface(peer);\n");
            push(@contentJava, "        SelfDisposer disposer = new SelfDisposer(node, peer);\n");
            push(@contentJava, "        disposer.next = head;\n");
            push(@contentJava, "        hashTable[hash] = disposer;\n");
            push(@contentJava, "        if (3 * hashCount >= 2 * hashTable.length)\n");
            push(@contentJava, "            rehash();\n");
            push(@contentJava, "        hashCount++;\n");
            push(@contentJava, "        return node;\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    private static void rehash() {\n");
            push(@contentJava, "        SelfDisposer[] oldTable = hashTable;\n");
            push(@contentJava, "        int oldLength = oldTable.length;\n");
            push(@contentJava, "        SelfDisposer[] newTable = new SelfDisposer[2*oldLength];\n");
            push(@contentJava, "        hashTable = newTable;\n");
            push(@contentJava, "        for (int i = oldLength; --i >= 0; ) {\n");
            push(@contentJava, "            for (SelfDisposer disposer = oldTable[i];\n");
            push(@contentJava, "                    disposer != null;) {\n");
            push(@contentJava, "                SelfDisposer next = disposer.next;\n");
            push(@contentJava, "                int hash = hashPeer(disposer.peer);\n");
            push(@contentJava, "                disposer.next = newTable[hash];\n");
            push(@contentJava, "                newTable[hash] = disposer;\n");
            push(@contentJava, "                disposer = next;\n");
            push(@contentJava, "            }\n");
            push(@contentJava, "        }\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    private static final class SelfDisposer extends Disposer.WeakDisposerRecord {\n");
            push(@contentJava, "        private final long peer;\n");
            push(@contentJava, "        SelfDisposer next;\n");
            push(@contentJava, "        SelfDisposer(Object referent, final long _peer) {\n");
            push(@contentJava, "            super(referent);\n");
            push(@contentJava, "            peer = _peer;\n");
            push(@contentJava, "        }\n\n");

            push(@contentJava, "        public void dispose() {\n");
            push(@contentJava, "            int hash = hashPeer(peer);\n");
            push(@contentJava, "            SelfDisposer head = hashTable[hash];\n");
            push(@contentJava, "            SelfDisposer prev = null;\n");
            push(@contentJava, "            for (SelfDisposer disposer = head; disposer != null;) {\n");
            push(@contentJava, "                SelfDisposer next = disposer.next;\n");
            push(@contentJava, "                if (disposer.peer == peer) {\n");
            push(@contentJava, "                    disposer.clear();\n");
            push(@contentJava, "                    if (prev != null)\n");
            push(@contentJava, "                        prev.next = next;\n");
            push(@contentJava, "                    else\n");
            push(@contentJava, "                        hashTable[hash] = next;\n");
            push(@contentJava, "                    hashCount--;\n");
            push(@contentJava, "                    break;\n");
            push(@contentJava, "                }\n");
            push(@contentJava, "                prev = disposer;\n");
            push(@contentJava, "                disposer = next;\n");
            push(@contentJava, "            }\n");
            push(@contentJava, "            $javaClassName.dispose(peer);\n");
            push(@contentJava, "        }\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    $javaClassName(long peer) {\n");
            push(@contentJava, ($interfaceName eq "DOMWindow")
                            ?  "        super(peer, JS_DOM_WINDOW_OBJECT);\n"
                            :  "        super(peer, JS_DOM_NODE_OBJECT);\n" );
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    static ${instanceType} createInterface(long peer) {\n");
            push(@contentJava, "        if (peer == 0L) return null;\n");
            push(@contentJava, $classData{$interfaceName}->{createFabric}) if $classData{$interfaceName}->{createFabric};
            push(@contentJava, "        return new ${javaClassName}(peer);\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    static ${instanceType} create(long peer) {\n");
            push(@contentJava, "        return getCachedImpl(peer);\n");
            push(@contentJava, "    }\n\n");
        } else {
            push(@contentJava, "    private static class SelfDisposer implements DisposerRecord {\n");
            push(@contentJava, "        private final long peer;\n");
            push(@contentJava, "        SelfDisposer(final long peer) {\n");
            push(@contentJava, "            this.peer = peer;\n");
            push(@contentJava, "        }\n");
            push(@contentJava, "        public void dispose() {\n");
            push(@contentJava, "            $javaClassName.dispose(peer);\n");
            push(@contentJava, "        }\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    $javaClassName(long peer) {\n");
            push(@contentJava, "        this.peer = peer;\n");
            push(@contentJava, "        Disposer.addRecord(this, new SelfDisposer(peer));\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    static ${instanceType} create(long peer) {\n");
            push(@contentJava, "        if (peer == 0L) return null;\n");
            push(@contentJava, $classData{$interfaceName}->{createFabric}) if $classData{$interfaceName}->{createFabric};
            push(@contentJava, "        return new ${javaClassName}(peer);\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    private final long peer;\n\n");

            push(@contentJava, "    long getPeer() {\n");
            push(@contentJava, "        return peer;\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    public boolean equals(Object that) {\n");
            push(@contentJava, "        return (that instanceof $javaClassName) && (peer == (($javaClassName)that).peer);\n");
            push(@contentJava, "    }\n\n");

            push(@contentJava, "    public int hashCode() {\n");
            push(@contentJava, "        long p = peer;\n");
            push(@contentJava, "        return (int) (p ^ (p >> 17));\n");
            push(@contentJava, "    }\n\n");
        }

        # common part for base Java class
        push(@contentJava, "    static long getPeer(${instanceType} arg) {\n");
        push(@contentJava, "        return (arg == null) ? 0L : ((${javaClassName})arg).getPeer();\n");
        push(@contentJava, "    }\n\n");

        push(@contentJava, "    native private static void dispose(long peer);\n\n");
        # CPP native static dispose
        push(@contentCPP,  "${functionExportPrefix} void ${functionNamePrefix}${javaClassName}_dispose(${functionStdParams}) {\n");
        push(@contentCPP,  "    static_cast<${interfaceName}*>(jlong_to_ptr(peer))->deref();\n");
        push(@contentCPP,  "}\n\n");

        # checkDynamicType implementation
        if ($classData{$interfaceName}->{checkDynamicType}) {
            push(@contentCPP,  "${functionExportPrefix} jint ${functionNamePrefix}${javaClassName}_getCPPTypeImpl(${functionStdParams}) {\n");

            my $enumIndex = 1;
            foreach my $cppType (@{$classData{$interfaceName}->{checkDynamicType}}) {
                push(@contentCPP,  "    if (dynamic_cast<${cppType}*>(static_cast<${interfaceName}*>(jlong_to_ptr(peer))))\n");
                push(@contentCPP,  "        return ${enumIndex};\n");

                $includesCPP{$cppType . ".h"} = 1;

                push(@contentJava, "    private static final int TYPE_${cppType} = ${enumIndex};\n");
                ++$enumIndex;
            }

            push(@contentCPP,  "    return 0;\n");
            push(@contentCPP,  "}\n\n");

            push(@contentJava, "    native private static int getCPPTypeImpl(long peer);\n\n");
        }
    } else {
        push(@contentJava, "    $javaClassName(long peer) {\n");
        push(@contentJava, "        super(peer);\n");
        push(@contentJava, "    }\n\n");
    }

    my $getImplType = $instanceType;
    
    if ($javaClassName eq "CharacterDataImpl" || $javaClassName eq "ProcessingInstructionImpl") {
        $getImplType = "Node";
    }

    push(@contentJava, "    static ${getImplType} getImpl(long peer) {\n");
    push(@contentJava, "        return (${getImplType})create(peer);\n");
    push(@contentJava, "    }\n\n");

    # customization
    if (($javaInterfaceName eq "Document") or ($javaInterfaceName eq "Element")) {
        my $funcName = "isHTML" . $javaInterfaceName . "Impl";
        # Java
        push(@contentJava, "    native static boolean ${funcName}(long peer);\n\n");
        if ($javaInterfaceName eq "Document") {
            push(@contentJava, "    public Object evaluate(String expression, Node contextNode, XPathNSResolver resolver, short type, Object result) throws DOMException {\n");
            push(@contentJava, "        return evaluate(expression, contextNode, resolver, type, (XPathResult)result);\n");
            push(@contentJava, "    }\n\n");
        }

        # CPP
        push(@contentCPP,  "${functionExportPrefix} jboolean ${functionNamePrefix}${javaClassName}_${funcName}(${functionStdParams}) {\n");
        push(@contentCPP,  "    return static_cast<${interfaceName}*>(jlong_to_ptr(peer))->isHTML${javaInterfaceName}()");
        push(@contentCPP,         " || static_cast<${interfaceName}*>(jlong_to_ptr(peer))->isXHTML${javaInterfaceName}()") if $javaInterfaceName eq "Document";
        push(@contentCPP, ";\n}\n\n");
    }

    if ($javaInterfaceName eq "XPathExpression") {
        # Java
        push(@contentJava, "    public Object evaluate(Node contextNode, short type, Object result) throws DOMException {\n");
        push(@contentJava, "        return evaluate(contextNode, type, (XPathResult)result);\n");
        push(@contentJava, "    }\n");
    }

    # constants
    if ($numConstants > 0) {
        my @constants = @{$dataNode->constants};
        my @headerConstants = ();

        foreach my $constant (@constants) {
            next if !IsDefinedInCommandLine($constant->extendedAttributes->{"Conditional"});
            my $constantName = $constant->name;
            my $constantValue = $constant->value;
            push(@headerConstants, "    public static final int $constantName = $constantValue;\n");
        }

        if (@headerConstants > 0) {
            push(@contentJava, "\n//constants\n");
            push(@contentJava, @headerConstants);
        }
    }

    # attributes
    if ($numAttributes > 0) {
        my @headerAttributes = ();
        my @cppAttributes = ();
        foreach my $attribute (@{$dataNode->attributes}) {
            next if ShouldSkipType($interfaceName, $attribute) or !IsDefinedInCommandLine($attribute->signature->extendedAttributes->{"Conditional"});

            my $attributeName = $attribute->signature->name;
            my $attributeType = GetJavaParamClassName($attribute->signature->type);

            my $attributeIsReadonly = IsReadonly($attribute);
            my $isStringAttr = isStringAttribute($interfaceName, $attributeName);

            my $camelName = ucfirst($attributeName);
            my $getterName = "get" . $camelName;

            my $getterExceptionSuffix = "";
	    if ($attribute->signature->extendedAttributes->{"GetterRaisesException"} ||
                $attribute->signature->extendedAttributes->{"RaisesException"})
	    {
                GetJavaParamClassName("DOMExceptionJSC"); # call this just to have DOMException added to the list of imports
		$getterExceptionSuffix = " throws DOMException";
	    }

            my $javaGetterName = GetJavaMethodName($interfaceName, $getterName);
            my $property
                       = "    public ";
            $property .= $isStringAttr ? "String" : $attributeType;
            $property .= " ${javaGetterName}()${getterExceptionSuffix} {\n";
            $property .= "        return " . GetJavaToNativeReturnValue(
                    $attributeType,
                    GetJavaTWKCallName($javaGetterName) . "(getPeer())");
            $property .= "+\"\"" if $isStringAttr;
            $property .= ";\n";

            $property .= "    }\n";
            $property .= "    native static "
                . GetJavaToNativeType($attributeType) . " "
                . GetJavaTWKCallName($javaGetterName) . "(long peer);\n\n";

            # CPP getter
            my ($functionName, @arguments) = $codeGenerator->GetterExpression(\%includesCPP, $interfaceName, $attribute);
	    if ($attribute->signature->extendedAttributes->{"GetterRaisesException"} ||
		$attribute->signature->extendedAttributes->{"RaisesException"})
	    {
		push(@arguments, "JavaException(env, JavaDOMException)");
	    }

            my $functionCPPAltName = $attribute->signature->extendedAttributes->{"ImplementedAs"};
            my $functionCPPName = $functionCPPAltName ? $functionCPPAltName : $functionName;

            my $cppBody = "${functionExportPrefix} "
                . GetJavaCPPNativeType($attributeType)
                . " ${functionNamePrefix}${javaClassName}_"
                . GetJavaTWKCallName($javaGetterName)
                . "(${functionStdParams}) {\n"
                . "    return "
                . GetJavaCPPNativeReturnValue(
                    $attributeType,
                    GetJavaCPPNativeArgValue($interfaceName, "peer")
                    . "->${functionCPPName}("
                    . join(", ", @arguments)
                    .")")
                . ";\n";
            $cppBody .= "}\n";
            $methodSig{$javaGetterName} = 1;

            if (!$attributeIsReadonly and !$attribute->signature->extendedAttributes->{"Replaceable"}) {
                my $setterName = "set" . $camelName;
                my $javaSetterName = GetJavaMethodName($interfaceName, $setterName);
		my $setterExceptionSuffix = "";
		if ($attribute->signature->extendedAttributes->{"SetterRaisesException"} ||
		    $attribute->signature->extendedAttributes->{"RaisesException"})
		{
                    GetJavaParamClassName("DOMExceptionJSC"); # call this just to have DOMException added to the list of imports
		    $setterExceptionSuffix = " throws DOMException";
		}

                $property .= "    public void ${javaSetterName}(";
                $property .= $isStringAttr ? "String" : $attributeType;
                $property .= " value)${setterExceptionSuffix} {\n";
                $property .= "        "
                    . GetJavaTWKCallName(${javaSetterName}) . "(getPeer(), "
                    . GetJavaToNativeArgValue($attributeType, $isStringAttr ? "Integer.parseInt(value)" : "value")
                    . ");\n";
                $property .= "    }\n";
                $property .= "    native static void "
                    . GetJavaTWKCallName(${javaSetterName}) . "(long peer, "
                    . GetJavaToNativeType($attributeType) . " value"
                    . ");\n\n";

                # CPP setter
                my ($functionName, @arguments) = $codeGenerator->SetterExpression(\%includesCPP, $interfaceName, $attribute);
		my $setterExceptionCPPParam;
		if ($attribute->signature->extendedAttributes->{"SetterRaisesException"} ||
		    $attribute->signature->extendedAttributes->{"RaisesException"})
		{
		    $setterExceptionCPPParam = "JavaException(env, JavaDOMException)";
		}

                push(@arguments, GetJavaCPPNativeArgValue($attributeType, "value"));
                push(@arguments, $setterExceptionCPPParam) if $setterExceptionCPPParam;

                my $functionCPPAltName = $attribute->signature->extendedAttributes->{"ImplementedAs"};
		if (defined $functionCPPAltName) {
		    $functionCPPAltName = "set" . ucfirst($functionCPPAltName);
		}
                my $functionCPPName = $functionCPPAltName ? $functionCPPAltName : $functionName;

                $cppBody .= "${functionExportPrefix} void "
                    . "${functionNamePrefix}${javaClassName}_"
                    . GetJavaTWKCallName(${javaSetterName})
                    . "(${functionStdParams}, "
                    . GetJavaCPPNativeType($attributeType)
                    . " value) {\n    "
                    . GetJavaCPPNativeArgValue($interfaceName, "peer")
                    . "->${functionCPPName}("
                    . join(", ", @arguments)
                    . ");\n";
                $cppBody .= "}\n";
                $methodSig{$javaSetterName} = 1;
            }
            $cppBody .= "\n";

            push(@headerAttributes, $property);
            push(@cppAttributes, $cppBody);
        }

        if (@headerAttributes > 0) {
            push(@contentJava, "\n//attributes\n");
            push(@contentJava, @headerAttributes);

            push(@contentCPP, "\n//attributes\n");
            push(@contentCPP, @cppAttributes);
        }
    }

    # functions
    if ($numFunctions > 0) {
        my @headerFunctions = ();
        my @cppFunctions = ();
        foreach my $function (@{$dataNode->functions}) {
            next if ShouldSkipType($interfaceName, $function) or !IsDefinedInCommandLine($function->signature->extendedAttributes->{"Conditional"});
            my $functionName = $function->signature->name;
            my $javaFunctionName = GetJavaMethodName($interfaceName, $functionName);
            my $returnType = GetJavaParamClassName($function->signature->type);
            my $exceptionSuffix = "";
	    if ($function->signature->extendedAttributes->{"RaisesException"}) {
                GetJavaParamClassName("DOMExceptionJSC"); # call this just to have DOMException added to the list of imports
		$exceptionSuffix = " throws DOMException";
	    }

            my $numberOfParameters = @{$function->parameters};

            my $javaInterface     = "    public $returnType $javaFunctionName(";
            my $javaTWKCall       = "    native static "
                . GetJavaToNativeType($returnType) . " "
                . GetJavaTWKCallName($javaFunctionName)
                . "(long peer";
            my $javaTWKCallParams = "getPeer()";

            my $functionExceptionCPPParam = "";
	    if ($function->signature->extendedAttributes->{"RaisesException"}) {
		$functionExceptionCPPParam = "JavaException(env, JavaDOMException)";
	    }
            my $CPPTWKCall        = "${functionExportPrefix} "
                . GetJavaCPPNativeType($returnType)
                . " ${functionNamePrefix}${javaClassName}_"
                . GetJavaTWKCallName($javaFunctionName)
                ."(${functionStdParams}";

            my $functionCPPAltName = $function->signature->extendedAttributes->{"ImplementedAs"};
            my $functionCPPName =$functionCPPAltName ? $functionCPPAltName : $functionName;

            my $CPPInterfaceCall  = GetJavaCPPNativeArgValue($interfaceName, "peer")
                . "->\n        ${functionCPPName}(";

            my $parameterIndex = 0;
            foreach my $param (@{$function->parameters}) {
                my $paramName = $param->name;
                my $paramType = GetJavaParamClassName($param->type);

                $javaInterface     .= "\n        , " if $parameterIndex > 0;
                $javaInterface     .= "$paramType $paramName";
                $javaTWKCall       .= "\n        , " . GetJavaToNativeType($paramType) . " " . $paramName;
                $javaTWKCallParams .= "\n            , " . GetJavaToNativeArgValue($paramType, $paramName);

                $CPPInterfaceCall  .= "\n            , " if $parameterIndex > 0;
                $CPPInterfaceCall  .= GetJavaCPPNativeArgValue($paramType, $paramName);
                $CPPTWKCall        .= "\n    , " . GetJavaCPPNativeType($paramType) . " " . $paramName;

                ++$parameterIndex;
            }

            $javaInterface .= ")${exceptionSuffix}\n    {\n"
                . "        "
                . ($returnType eq "void" ? "" : "return ")
                . GetJavaToNativeReturnValue(
                    $returnType,
                    GetJavaTWKCallName($javaFunctionName) . "(" . $javaTWKCallParams .")")
                . ";\n";
            $javaInterface .= "    }\n";
            $javaTWKCall .= ");\n\n\n";

            if ($functionExceptionCPPParam) {
                $CPPInterfaceCall .= "\n            , " if $parameterIndex > 0;
                $CPPInterfaceCall .= $functionExceptionCPPParam
            }
            $CPPInterfaceCall .= ")";

            my $customCPPBody = GetCustomCPPMethodBody($interfaceName, $functionName);
            $customCPPBody = undef if $customCPPBody and $customCPPBody eq "<ordinal>";

            $CPPTWKCall .= ")\n{\n"
                . "    "
                . ($customCPPBody
                    ? $customCPPBody
                    : (($returnType eq "void" ? "" : "return ") . GetJavaCPPNativeReturnValue($returnType, $CPPInterfaceCall) . ";\n"));
            $CPPTWKCall .= "}\n\n\n";

            push(@headerFunctions, $javaInterface);
            push(@headerFunctions, $javaTWKCall);

            push(@cppFunctions, $CPPTWKCall);
            $methodSig{$javaFunctionName} = 1;
        }

        if (@headerFunctions > 0) {
            push(@contentJava, "\n//functions\n");
            push(@contentJava, @headerFunctions);

            push(@contentCPP, "\n//functions\n");
            push(@contentCPP, @cppFunctions);
        }
    }

    # stubs
    if ($classData{$interfaceName}->{stubs}) {
        my @stubs = @{$classData{$interfaceName}->{stubs}};
        push(@contentJava, "\n//stubs\n");
        foreach my $stubSig (@stubs) {
            push(@contentJava, "    public " . $stubSig . " {\n");
            push(@contentJava, "        throw new UnsupportedOperationException(\"Not supported yet.\");\n");
            push(@contentJava, "    }\n");
        }
    }


    #end-of-class declaration
    push(@contentJava, "}\n\n");

    foreach my $import (sort keys(%importsJava)) {
        push(@headerJava, "import ${import};\n");
    }
    foreach my $incl (sort keys(%includesCPP)) {
        push(@headerCPP, "#include \"${incl}\"\n");
	my $CPPtype = $incl;
	$CPPtype =~ s{.*/}{};      # removes path  
	$CPPtype =~ s{\.[^.]+$}{}; # removes extension
        push(@headerCPP, "#define ${CPPtype} WebCore::${CPPtype}\n") if $ambiguousType{$CPPtype};
    }

    push(@headerJava, "\n");
}

# Internal helper
sub WriteData
{
    my $object = shift;
    my $interface = shift;
    my $outputDir = shift;

    my $name = $interface->name;

    my $javapath = "${outputDir}/java/com/sun/webkit/dom";
    mkpath($javapath);

    # Open files for writing...
    my $javaFileName = "${javapath}/" . GetTransferTypeName($name) . ".java";
    my $cppFileName = "${outputDir}/" . FileNamePrefix . $name . ".cpp";

    # Update a .java file if the contents are changed.
    my $contentsJava = "// Automatically generated by CodeGeneratorJava.pm. Do not edit.\n\n";
    $contentsJava .= join "", @headerJava;
    $contentsJava .= join "", @contentJava;
    $codeGenerator->UpdateFile($javaFileName, $contentsJava);

    # Update a .cpp file if the contents are changed.

    my $contentsCPP = "// Automatically generated by CodeGeneratorJava.pm. Do not edit.\n\n";
    $contentsCPP .= "#include \"config.h\"\n"
                  . "#include \"wtf/RefPtr.h\"\n\n";

    $contentsCPP .= join "", @headerCPP;

    $contentsCPP .= "\n"
                  ."#include \"JavaDOMUtils.h\"\n"
                  ."#include \"JavaEnv.h\"\n\n"
                  ."using namespace WebCore;\n\n"
                  ."extern \"C\" {\n\n";

    $contentsCPP .= join "", @contentCPP;

    $contentsCPP .= "}\n";

    $codeGenerator->UpdateFile($cppFileName, $contentsCPP);

    %importsJava = ();
    %includesCPP = ();
    %methodSig = ();
    @headerJava = ();
    @contentJava = ();
    @headerCPP = ();
    @contentCPP = ();
}

1;
