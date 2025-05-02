#ifndef XML_PARSER_H_PRIVATE__
#define XML_PARSER_H_PRIVATE__

#include <libxml/parser.h>
#include <libxml/xmlversion.h>

/**
 * XML_VCTXT_DTD_VALIDATED:
 *
 * Set after xmlValidateDtdFinal was called.
 */
#define XML_VCTXT_DTD_VALIDATED (1u << 0)
/**
 * XML_VCTXT_USE_PCTXT:
 *
 * Set if the validation context is part of a parser context.
 */
#define XML_VCTXT_USE_PCTXT (1u << 1)

#define XML_INPUT_HAS_ENCODING      (1u << 0)
#define XML_INPUT_AUTO_ENCODING     (7u << 1)
#define XML_INPUT_AUTO_UTF8         (1u << 1)
#define XML_INPUT_AUTO_UTF16LE      (2u << 1)
#define XML_INPUT_AUTO_UTF16BE      (3u << 1)
#define XML_INPUT_AUTO_OTHER        (4u << 1)
#define XML_INPUT_USES_ENC_DECL     (1u << 4)
#define XML_INPUT_ENCODING_ERROR    (1u << 5)
#define XML_INPUT_PROGRESSIVE       (1u << 6)

#define PARSER_STOPPED(ctxt) ((ctxt)->disableSAX > 1)

#define PARSER_PROGRESSIVE(ctxt) \
    ((ctxt)->input->flags & XML_INPUT_PROGRESSIVE)

#define PARSER_IN_PE(ctxt) \
    (((ctxt)->input->entity != NULL) && \
     (((ctxt)->input->entity->etype == XML_INTERNAL_PARAMETER_ENTITY) || \
      ((ctxt)->input->entity->etype == XML_EXTERNAL_PARAMETER_ENTITY)))

#define PARSER_EXTERNAL(ctxt) \
    (((ctxt)->inSubset == 2) || \
     (((ctxt)->input->entity != NULL) && \
      ((ctxt)->input->entity->etype == XML_EXTERNAL_PARAMETER_ENTITY)))

XML_HIDDEN void
xmlCtxtVErr(xmlParserCtxtPtr ctxt, xmlNodePtr node, xmlErrorDomain domain,
            xmlParserErrors code, xmlErrorLevel level,
            const xmlChar *str1, const xmlChar *str2, const xmlChar *str3,
            int int1, const char *msg, va_list ap);
XML_HIDDEN void
xmlCtxtErr(xmlParserCtxtPtr ctxt, xmlNodePtr node, xmlErrorDomain domain,
           xmlParserErrors code, xmlErrorLevel level,
           const xmlChar *str1, const xmlChar *str2, const xmlChar *str3,
           int int1, const char *msg, ...);
XML_HIDDEN void
xmlFatalErr(xmlParserCtxtPtr ctxt, xmlParserErrors error, const char *info);
XML_HIDDEN void LIBXML_ATTR_FORMAT(3,0)
xmlWarningMsg(xmlParserCtxtPtr ctxt, xmlParserErrors error,
              const char *msg, const xmlChar *str1, const xmlChar *str2);
XML_HIDDEN void
xmlCtxtErrIO(xmlParserCtxtPtr ctxt, int code, const char *uri);

XML_HIDDEN void
xmlHaltParser(xmlParserCtxtPtr ctxt);
XML_HIDDEN int
xmlParserGrow(xmlParserCtxtPtr ctxt);
XML_HIDDEN void
xmlParserShrink(xmlParserCtxtPtr ctxt);

XML_HIDDEN void
xmlDetectEncoding(xmlParserCtxtPtr ctxt);
XML_HIDDEN void
xmlSetDeclaredEncoding(xmlParserCtxtPtr ctxt, xmlChar *encoding);
XML_HIDDEN const xmlChar *
xmlGetActualEncoding(xmlParserCtxtPtr ctxt);

XML_HIDDEN xmlParserNsData *
xmlParserNsCreate(void);
XML_HIDDEN void
xmlParserNsFree(xmlParserNsData *nsdb);
/*
 * These functions allow SAX handlers to attach extra data to namespaces
 * efficiently and should be made public.
 */
XML_HIDDEN int
xmlParserNsUpdateSax(xmlParserCtxtPtr ctxt, const xmlChar *prefix,
                     void *saxData);
XML_HIDDEN void *
xmlParserNsLookupSax(xmlParserCtxtPtr ctxt, const xmlChar *prefix);

#define XML_INPUT_BUF_STATIC        (1u << 1)
#define XML_INPUT_BUF_ZERO_TERMINATED    (1u << 2)
#define XML_INPUT_UNZIP            (1u << 3)

/* Internal parser option */
#define XML_PARSE_UNZIP     (1 << 24)

XML_HIDDEN xmlParserInputPtr
xmlNewInputURL(xmlParserCtxtPtr ctxt, const char *url, const char *publicId,
               const char *encoding, int flags);
XML_HIDDEN xmlParserInputPtr
xmlNewInputMemory(xmlParserCtxtPtr ctxt, const char *url,
                  const void *mem, size_t size,
                  const char *encoding, int flags);
XML_HIDDEN xmlParserInputPtr
xmlNewInputString(xmlParserCtxtPtr ctxt, const char *url, const char *str,
                  const char *encoding, int flags);
XML_HIDDEN xmlParserInputPtr
xmlNewInputFd(xmlParserCtxtPtr ctxt, const char *filename, int fd,
              const char *encoding, int flags);
XML_HIDDEN xmlParserInputPtr
xmlNewInputIO(xmlParserCtxtPtr ctxt, const char *url,
              xmlInputReadCallback ioRead,
              xmlInputCloseCallback ioClose,
              void *ioCtxt,
              const char *encoding, int flags);
XML_HIDDEN xmlParserInputPtr
xmlNewInputPush(xmlParserCtxtPtr ctxt, const char *url,
                const char *chunk, int size, const char *encoding);

XML_HIDDEN xmlChar *
xmlExpandEntitiesInAttValue(xmlParserCtxtPtr ctxt, const xmlChar *str,
                            int normalize);

#endif /* XML_PARSER_H_PRIVATE__ */
