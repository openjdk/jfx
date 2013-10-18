/* GStreamer
 * Copyright (C) 1999,2000 Erik Walthinsen <omega@cse.ogi.edu>
 *                    2000 Wim Taymans <wtay@chello.be>
 *
 * gstxml.c: XML save/restore of pipelines
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstxml
 * @short_description: XML save/restore operations of pipelines
 *
 * GStreamer pipelines can be saved to xml files using gst_xml_write_file().
 * They can be loaded back using gst_xml_parse_doc() / gst_xml_parse_file() / 
 * gst_xml_parse_memory().
 * Additionally one can load saved pipelines into the gst-editor to inspect the
 * graph.
 *
 * #GstElement implementations need to override the #GstObjectClass.save_thyself()
 * and #GstObjectClass.restore_thyself() virtual functions of #GstObject.
 *
 * Deprecated: This feature is deprecated pipeline serialization to XML is
 * broken for all but the most simple pipelines. It will most likely be
 * removed in future. Don't use it.
 */

#include "gst_private.h"

#include "gstxml.h"
#include "gstmarshal.h"
#include "gstinfo.h"
#include "gstbin.h"

#ifdef GST_DISABLE_DEPRECATED
#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)
#include <libxml/parser.h>
xmlNodePtr gst_object_save_thyself (const GstObject * object,
    xmlNodePtr parent);
GstObject *gst_object_load_thyself (xmlNodePtr parent);
void gst_object_restore_thyself (GstObject * object, GstXmlNodePtr self);

#define GST_TYPE_XML 		(gst_xml_get_type ())
#define GST_XML(obj) 		(G_TYPE_CHECK_INSTANCE_CAST ((obj), GST_TYPE_XML, GstXML))
#define GST_IS_XML(obj) 	(G_TYPE_CHECK_INSTANCE_TYPE ((obj), GST_TYPE_XML))
#define GST_XML_CLASS(klass) 	(G_TYPE_CHECK_CLASS_CAST ((klass), GST_TYPE_XML, GstXMLClass))
#define GST_IS_XML_CLASS(klass) (G_TYPE_CHECK_CLASS_TYPE ((klass), GST_TYPE_XML))
#define GST_XML_GET_CLASS(obj) 	(G_TYPE_INSTANCE_GET_CLASS ((obj), GST_TYPE_XML, GstXMLClass))

typedef struct _GstXML GstXML;
typedef struct _GstXMLClass GstXMLClass;

struct _GstXML
{
  GstObject object;

  /*< public > */
  GList *topelements;

  xmlNsPtr ns;

  /*< private > */
  gpointer _gst_reserved[GST_PADDING];
};

struct _GstXMLClass
{
  GstObjectClass parent_class;

  /* signal callbacks */
  void (*object_loaded) (GstXML * xml, GstObject * object, xmlNodePtr self);
  void (*object_saved) (GstXML * xml, GstObject * object, xmlNodePtr self);

  gpointer _gst_reserved[GST_PADDING];
};

GType gst_xml_get_type (void);
xmlDocPtr gst_xml_write (GstElement * element);
gint gst_xml_write_file (GstElement * element, FILE * out);
GstXML *gst_xml_new (void);
gboolean gst_xml_parse_doc (GstXML * xml, xmlDocPtr doc, const guchar * root);
gboolean gst_xml_parse_file (GstXML * xml, const guchar * fname,
    const guchar * root);
gboolean gst_xml_parse_memory (GstXML * xml, guchar * buffer, guint size,
    const gchar * root);
GstElement *gst_xml_get_element (GstXML * xml, const guchar * name);
GList *gst_xml_get_topelements (GstXML * xml);
GstElement *gst_xml_make_element (xmlNodePtr cur, GstObject * parent);
#endif
#endif

#if !defined(GST_DISABLE_LOADSAVE) && !defined(GST_REMOVE_DEPRECATED)

enum
{
  OBJECT_LOADED,
  LAST_SIGNAL
};

static void gst_xml_dispose (GObject * object);

static void gst_xml_object_loaded (GstObject * private, GstObject * object,
    xmlNodePtr self, gpointer data);

static GstObjectClass *parent_class = NULL;
static guint gst_xml_signals[LAST_SIGNAL] = { 0 };

G_DEFINE_TYPE (GstXML, gst_xml, GST_TYPE_OBJECT);

static void
gst_xml_class_init (GstXMLClass * klass)
{
  GObjectClass *gobject_class = (GObjectClass *) klass;

  parent_class = g_type_class_peek_parent (klass);

  gobject_class->dispose = gst_xml_dispose;

  /* FIXME G_TYPE_POINTER should be GType of xmlNodePtr
   * (ensonic) can't be fixed, as libxml does not use GObject (unfortunately)
   */
  /**
   * GstXML::object-loaded:
   * @xml: the xml persistence instance
   * @object: the object that has been loaded
   * @xml_node: the related xml_node pointer to the document tree
   *
   * Signals that a new object has been deserialized.
   */
  gst_xml_signals[OBJECT_LOADED] =
      g_signal_new ("object-loaded", G_TYPE_FROM_CLASS (klass),
      G_SIGNAL_RUN_LAST, G_STRUCT_OFFSET (GstXMLClass, object_loaded), NULL,
      NULL, gst_marshal_VOID__OBJECT_POINTER, G_TYPE_NONE, 2, GST_TYPE_OBJECT,
      G_TYPE_POINTER);

}

static void
gst_xml_init (GstXML * xml)
{
  xml->topelements = NULL;
}

static void
gst_xml_dispose (GObject * object)
{
  GstXML *xml = GST_XML (object);

  g_list_foreach (xml->topelements, (GFunc) gst_object_unref, NULL);
  g_list_free (xml->topelements);
  xml->topelements = NULL;

  G_OBJECT_CLASS (parent_class)->dispose (object);
}

/**
 * gst_xml_new:
 *
 * Create a new GstXML parser object.
 *
 * Returns: a pointer to a new GstXML object.
 */
GstXML *
gst_xml_new (void)
{
  return GST_XML (g_object_newv (GST_TYPE_XML, 0, NULL));
}

/**
 * gst_xml_write:
 * @element: The element to write out
 *
 * Converts the given element into an XML presentation.
 *
 * Returns: a pointer to an XML document
 */
xmlDocPtr
gst_xml_write (GstElement * element)
{
  xmlDocPtr doc;
  xmlNodePtr elementnode;
  xmlNsPtr gst_ns;

  doc = xmlNewDoc ((xmlChar *) "1.0");

  doc->xmlRootNode = xmlNewDocNode (doc, NULL, (xmlChar *) "gstreamer", NULL);

  gst_ns =
      xmlNewNs (doc->xmlRootNode,
      (xmlChar *) "http://gstreamer.net/gst-core/1.0/", (xmlChar *) "gst");

  elementnode = xmlNewChild (doc->xmlRootNode, gst_ns, (xmlChar *) "element",
      NULL);

  gst_object_save_thyself (GST_OBJECT (element), elementnode);

  return doc;
}

/**
 * gst_xml_write_file:
 * @element: The element to write out
 * @out: an open file, like stdout
 *
 * Converts the given element into XML and writes the formatted XML to an open
 * file.
 *
 * Returns: number of bytes written on success, -1 otherwise.
 */
gint
gst_xml_write_file (GstElement * element, FILE * out)
{
  xmlDocPtr cur;

#ifdef HAVE_LIBXML2
  xmlOutputBufferPtr buf;
#endif
  const char *encoding;
  xmlCharEncodingHandlerPtr handler = NULL;
  int indent;
  gboolean ret;

  cur = gst_xml_write (element);
  if (!cur)
    return -1;

#ifdef HAVE_LIBXML2
  encoding = (const char *) cur->encoding;

  if (encoding != NULL) {
    xmlCharEncoding enc;

    enc = xmlParseCharEncoding (encoding);

    if (cur->charset != XML_CHAR_ENCODING_UTF8) {
      xmlGenericError (xmlGenericErrorContext,
          "xmlDocDump: document not in UTF8\n");
      return -1;
    }
    if (enc != XML_CHAR_ENCODING_UTF8) {
      handler = xmlFindCharEncodingHandler (encoding);
      if (handler == NULL) {
        xmlFree ((char *) cur->encoding);
        cur->encoding = NULL;
      }
    }
  }

  buf = xmlOutputBufferCreateFile (out, handler);

  indent = xmlIndentTreeOutput;
  xmlIndentTreeOutput = 1;
  ret = xmlSaveFormatFileTo (buf, cur, NULL, 1);
  xmlIndentTreeOutput = indent;
#else
  /* apparently this doesn't return anything in libxml1 */
  xmlDocDump (out, cur);
  ret = 1;
#endif

  return ret;
}

/**
 * gst_xml_parse_doc:
 * @xml: a pointer to a GstXML object
 * @doc: a pointer to an xml document to parse
 * @root: The name of the root object to build
 *
 * Fills the GstXML object with the elements from the
 * xmlDocPtr.
 *
 * Returns: TRUE on success, FALSE otherwise
 */
gboolean
gst_xml_parse_doc (GstXML * xml, xmlDocPtr doc, const guchar * root)
{
  xmlNodePtr field, cur;
  xmlNsPtr ns;

  cur = xmlDocGetRootElement (doc);
  if (cur == NULL) {
    g_warning ("gstxml: empty document\n");
    return FALSE;
  }
  ns = xmlSearchNsByHref (doc, cur,
      (xmlChar *) "http://gstreamer.net/gst-core/1.0/");
  if (ns == NULL) {
    g_warning ("gstxml: document of wrong type, core namespace not found\n");
    return FALSE;
  }
  if (strcmp ((char *) cur->name, "gstreamer")) {
    g_warning ("gstxml: XML file is in wrong format\n");
    return FALSE;
  }

  gst_class_signal_connect (GST_OBJECT_CLASS (G_OBJECT_GET_CLASS (xml)),
      "object_loaded", (gpointer) gst_xml_object_loaded, xml);

  xml->ns = ns;

  field = cur->xmlChildrenNode;

  while (field) {
    if (!strcmp ((char *) field->name, "element") && (field->ns == xml->ns)) {
      GstElement *element;

      element = gst_xml_make_element (field, NULL);

      xml->topelements = g_list_prepend (xml->topelements, element);
    }
    field = field->next;
  }

  xml->topelements = g_list_reverse (xml->topelements);

  return TRUE;
}

/* FIXME 0.9: Why guchar*? */
/**
 * gst_xml_parse_file:
 * @xml: a pointer to a GstXML object
 * @fname: The filename with the xml description
 * @root: The name of the root object to build
 *
 * Fills the GstXML object with the corresponding elements from
 * the XML file fname. Optionally it will only build the element from
 * the element node root (if it is not NULL). This feature is useful
 * if you only want to build a specific element from an XML file
 * but not the pipeline it is embedded in.
 *
 * Pass "-" as fname to read from stdin. You can also pass a URI
 * of any format that libxml supports, including http.
 *
 * Returns: TRUE on success, FALSE otherwise
 */
gboolean
gst_xml_parse_file (GstXML * xml, const guchar * fname, const guchar * root)
{
  xmlDocPtr doc;
  gboolean ret;

  g_return_val_if_fail (fname != NULL, FALSE);

  doc = xmlParseFile ((char *) fname);

  if (!doc) {
    g_warning ("gstxml: XML file \"%s\" could not be read\n", fname);
    return FALSE;
  }

  ret = gst_xml_parse_doc (xml, doc, root);

  xmlFreeDoc (doc);
  return ret;
}

/* FIXME 0.9: guchar* */
/**
 * gst_xml_parse_memory:
 * @xml: a pointer to a GstXML object
 * @buffer: a pointer to the in memory XML buffer
 * @size: the size of the buffer
 * @root: the name of the root objects to build
 *
 * Fills the GstXML object with the corresponding elements from
 * an in memory XML buffer.
 *
 * Returns: TRUE on success
 */
gboolean
gst_xml_parse_memory (GstXML * xml, guchar * buffer, guint size,
    const gchar * root)
{
  xmlDocPtr doc;
  gboolean ret;

  g_return_val_if_fail (buffer != NULL, FALSE);

  doc = xmlParseMemory ((char *) buffer, size);

  ret = gst_xml_parse_doc (xml, doc, (const xmlChar *) root);

  xmlFreeDoc (doc);
  return ret;
}

static void
gst_xml_object_loaded (GstObject * private, GstObject * object, xmlNodePtr self,
    gpointer data)
{
  GstXML *xml = GST_XML (data);

  /* FIXME check that this element was created from the same xmlDocPtr... */
  g_signal_emit (xml, gst_xml_signals[OBJECT_LOADED], 0, object, self);
}

/**
 * gst_xml_get_topelements:
 * @xml: The GstXML to get the elements from
 *
 * Retrieve a list of toplevel elements.
 *
 * Returns: a GList of top-level elements. The caller does not own a copy
 * of the list and must not free or modify the list. The caller also does not
 * own a reference to any of the elements in the list and should obtain its own
 * reference using gst_object_ref() if necessary.
 */
GList *
gst_xml_get_topelements (GstXML * xml)
{
  g_return_val_if_fail (xml != NULL, NULL);

  return xml->topelements;
}

/* FIXME 0.11: why is the arg guchar* instead of gchar*? */
/**
 * gst_xml_get_element:
 * @xml: The GstXML to get the element from
 * @name: The name of element to retrieve
 *
 * This function is used to get a pointer to the GstElement corresponding
 * to name in the pipeline description. You would use this if you have
 * to do anything to the element after loading.
 *
 * Returns: a pointer to a new GstElement, caller owns returned reference.
 */
GstElement *
gst_xml_get_element (GstXML * xml, const guchar * name)
{
  GstElement *element;
  GList *topelements;

  g_return_val_if_fail (xml != NULL, NULL);
  g_return_val_if_fail (name != NULL, NULL);

  GST_DEBUG ("gstxml: getting element \"%s\"", name);

  topelements = gst_xml_get_topelements (xml);

  while (topelements) {
    GstElement *top = GST_ELEMENT (topelements->data);

    GST_DEBUG ("gstxml: getting element \"%s\"", name);
    if (!strcmp (GST_ELEMENT_NAME (top), (char *) name)) {
      return GST_ELEMENT_CAST (gst_object_ref (top));
    } else {
      if (GST_IS_BIN (top)) {
        element = gst_bin_get_by_name (GST_BIN (top), (gchar *) name);

        if (element)
          return element;
      }
    }
    topelements = g_list_next (topelements);
  }
  return NULL;
}

/**
 * gst_xml_make_element:
 * @cur: the xml node
 * @parent: the parent of this object when it's loaded
 *
 * Load the element from the XML description
 *
 * Returns: the new element
 */
GstElement *
gst_xml_make_element (xmlNodePtr cur, GstObject * parent)
{
  xmlNodePtr children = cur->xmlChildrenNode;
  GstElement *element;
  gchar *name = NULL;
  gchar *type = NULL;

  /* first get the needed tags to construct the element */
  while (children) {
    if (!strcmp ((char *) children->name, "name")) {
      name = (gchar *) xmlNodeGetContent (children);
    } else if (!strcmp ((char *) children->name, "type")) {
      type = (gchar *) xmlNodeGetContent (children);
    }
    children = children->next;
  }
  g_return_val_if_fail (name != NULL, NULL);
  g_return_val_if_fail (type != NULL, NULL);

  GST_CAT_INFO (GST_CAT_XML, "loading \"%s\" of type \"%s\"", name, type);

  element = gst_element_factory_make (type, name);

  g_return_val_if_fail (element != NULL, NULL);

  g_free (type);
  g_free (name);

  /* ne need to set the parent on this object bacause the pads */
  /* will go through the hierarchy to link to their peers */
  if (parent) {
    if (GST_IS_BIN (parent)) {
      gst_bin_add (GST_BIN (parent), element);
    } else {
      gst_object_set_parent (GST_OBJECT (element), parent);
    }
  }

  gst_object_restore_thyself (GST_OBJECT (element), cur);

  return element;
}

#else

/* FIXME: keep this dummy _get_type function around for now, so
 * gobject-introspection doesn't fail in the GST_REMOVE_DEPRECATED and
 * GST_DISABLE_LOADSAVE case */
GType gst_xml_get_type (void);

GType
gst_xml_get_type (void)
{
  return g_pointer_type_register_static ("GstXML");
}

#endif
