/* GRegex -- regular expression API wrapper around PCRE.
 *
 * Copyright (C) 1999, 2000 Scott Wimer
 * Copyright (C) 2004, Matthias Clasen <mclasen@redhat.com>
 * Copyright (C) 2005 - 2007, Marco Barisione <marco@barisione.org>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

#include "config.h"

#include <string.h>

#ifdef USE_SYSTEM_PCRE
#include <pcre.h>
#else
#include "pcre/pcre.h"
#endif

#include "gtypes.h"
#include "gregex.h"
#include "glibintl.h"
#include "glist.h"
#include "gmessages.h"
#include "gstrfuncs.h"
#include "gatomic.h"
#include "gthread.h"

/**
 * SECTION:gregex
 * @title: Perl-compatible regular expressions
 * @short_description: matches strings against regular expressions
 * @see_also: [Regular expression syntax][glib-regex-syntax]
 *
 * The g_regex_*() functions implement regular
 * expression pattern matching using syntax and semantics similar to
 * Perl regular expression.
 *
 * Some functions accept a @start_position argument, setting it differs
 * from just passing over a shortened string and setting #G_REGEX_MATCH_NOTBOL
 * in the case of a pattern that begins with any kind of lookbehind assertion.
 * For example, consider the pattern "\Biss\B" which finds occurrences of "iss"
 * in the middle of words. ("\B" matches only if the current position in the
 * subject is not a word boundary.) When applied to the string "Mississipi"
 * from the fourth byte, namely "issipi", it does not match, because "\B" is
 * always false at the start of the subject, which is deemed to be a word
 * boundary. However, if the entire string is passed , but with
 * @start_position set to 4, it finds the second occurrence of "iss" because
 * it is able to look behind the starting point to discover that it is
 * preceded by a letter.
 *
 * Note that, unless you set the #G_REGEX_RAW flag, all the strings passed
 * to these functions must be encoded in UTF-8. The lengths and the positions
 * inside the strings are in bytes and not in characters, so, for instance,
 * "\xc3\xa0" (i.e. "&agrave;") is two bytes long but it is treated as a
 * single character. If you set #G_REGEX_RAW the strings can be non-valid
 * UTF-8 strings and a byte is treated as a character, so "\xc3\xa0" is two
 * bytes and two characters long.
 *
 * When matching a pattern, "\n" matches only against a "\n" character in
 * the string, and "\r" matches only a "\r" character. To match any newline
 * sequence use "\R". This particular group matches either the two-character
 * sequence CR + LF ("\r\n"), or one of the single characters LF (linefeed,
 * U+000A, "\n"), VT vertical tab, U+000B, "\v"), FF (formfeed, U+000C, "\f"),
 * CR (carriage return, U+000D, "\r"), NEL (next line, U+0085), LS (line
 * separator, U+2028), or PS (paragraph separator, U+2029).
 *
 * The behaviour of the dot, circumflex, and dollar metacharacters are
 * affected by newline characters, the default is to recognize any newline
 * character (the same characters recognized by "\R"). This can be changed
 * with #G_REGEX_NEWLINE_CR, #G_REGEX_NEWLINE_LF and #G_REGEX_NEWLINE_CRLF
 * compile options, and with #G_REGEX_MATCH_NEWLINE_ANY,
 * #G_REGEX_MATCH_NEWLINE_CR, #G_REGEX_MATCH_NEWLINE_LF and
 * #G_REGEX_MATCH_NEWLINE_CRLF match options. These settings are also
 * relevant when compiling a pattern if #G_REGEX_EXTENDED is set, and an
 * unescaped "#" outside a character class is encountered. This indicates
 * a comment that lasts until after the next newline.
 *
 * When setting the %G_REGEX_JAVASCRIPT_COMPAT flag, pattern syntax and pattern
 * matching is changed to be compatible with the way that regular expressions
 * work in JavaScript. More precisely, a lonely ']' character in the pattern
 * is a syntax error; the '\x' escape only allows 0 to 2 hexadecimal digits, and
 * you must use the '\u' escape sequence with 4 hex digits to specify a unicode
 * codepoint instead of '\x' or 'x{....}'. If '\x' or '\u' are not followed by
 * the specified number of hex digits, they match 'x' and 'u' literally; also
 * '\U' always matches 'U' instead of being an error in the pattern. Finally,
 * pattern matching is modified so that back references to an unset subpattern
 * group produces a match with the empty string instead of an error. See
 * pcreapi(3) for more information.
 *
 * Creating and manipulating the same #GRegex structure from different
 * threads is not a problem as #GRegex does not modify its internal
 * state between creation and destruction, on the other hand #GMatchInfo
 * is not threadsafe.
 *
 * The regular expressions low-level functionalities are obtained through
 * the excellent
 * [PCRE](http://www.pcre.org/)
 * library written by Philip Hazel.
 */

/* Mask of all the possible values for GRegexCompileFlags. */
#define G_REGEX_COMPILE_MASK (G_REGEX_CASELESS          | \
                              G_REGEX_MULTILINE         | \
                              G_REGEX_DOTALL            | \
                              G_REGEX_EXTENDED          | \
                              G_REGEX_ANCHORED          | \
                              G_REGEX_DOLLAR_ENDONLY    | \
                              G_REGEX_UNGREEDY          | \
                              G_REGEX_RAW               | \
                              G_REGEX_NO_AUTO_CAPTURE   | \
                              G_REGEX_OPTIMIZE          | \
                              G_REGEX_FIRSTLINE         | \
                              G_REGEX_DUPNAMES          | \
                              G_REGEX_NEWLINE_CR        | \
                              G_REGEX_NEWLINE_LF        | \
                              G_REGEX_NEWLINE_CRLF      | \
                              G_REGEX_NEWLINE_ANYCRLF   | \
                              G_REGEX_BSR_ANYCRLF       | \
                              G_REGEX_JAVASCRIPT_COMPAT)

/* Mask of all GRegexCompileFlags values that are (not) passed trough to PCRE */
#define G_REGEX_COMPILE_PCRE_MASK (G_REGEX_COMPILE_MASK & ~G_REGEX_COMPILE_NONPCRE_MASK)
#define G_REGEX_COMPILE_NONPCRE_MASK (G_REGEX_RAW              | \
                                      G_REGEX_OPTIMIZE)

/* Mask of all the possible values for GRegexMatchFlags. */
#define G_REGEX_MATCH_MASK (G_REGEX_MATCH_ANCHORED         | \
                            G_REGEX_MATCH_NOTBOL           | \
                            G_REGEX_MATCH_NOTEOL           | \
                            G_REGEX_MATCH_NOTEMPTY         | \
                            G_REGEX_MATCH_PARTIAL          | \
                            G_REGEX_MATCH_NEWLINE_CR       | \
                            G_REGEX_MATCH_NEWLINE_LF       | \
                            G_REGEX_MATCH_NEWLINE_CRLF     | \
                            G_REGEX_MATCH_NEWLINE_ANY      | \
                            G_REGEX_MATCH_NEWLINE_ANYCRLF  | \
                            G_REGEX_MATCH_BSR_ANYCRLF      | \
                            G_REGEX_MATCH_BSR_ANY          | \
                            G_REGEX_MATCH_PARTIAL_SOFT     | \
                            G_REGEX_MATCH_PARTIAL_HARD     | \
                            G_REGEX_MATCH_NOTEMPTY_ATSTART)

/* we rely on these flags having the same values */
G_STATIC_ASSERT (G_REGEX_CASELESS          == PCRE_CASELESS);
G_STATIC_ASSERT (G_REGEX_MULTILINE         == PCRE_MULTILINE);
G_STATIC_ASSERT (G_REGEX_DOTALL            == PCRE_DOTALL);
G_STATIC_ASSERT (G_REGEX_EXTENDED          == PCRE_EXTENDED);
G_STATIC_ASSERT (G_REGEX_ANCHORED          == PCRE_ANCHORED);
G_STATIC_ASSERT (G_REGEX_DOLLAR_ENDONLY    == PCRE_DOLLAR_ENDONLY);
G_STATIC_ASSERT (G_REGEX_UNGREEDY          == PCRE_UNGREEDY);
G_STATIC_ASSERT (G_REGEX_NO_AUTO_CAPTURE   == PCRE_NO_AUTO_CAPTURE);
G_STATIC_ASSERT (G_REGEX_FIRSTLINE         == PCRE_FIRSTLINE);
G_STATIC_ASSERT (G_REGEX_DUPNAMES          == PCRE_DUPNAMES);
G_STATIC_ASSERT (G_REGEX_NEWLINE_CR        == PCRE_NEWLINE_CR);
G_STATIC_ASSERT (G_REGEX_NEWLINE_LF        == PCRE_NEWLINE_LF);
G_STATIC_ASSERT (G_REGEX_NEWLINE_CRLF      == PCRE_NEWLINE_CRLF);
G_STATIC_ASSERT (G_REGEX_NEWLINE_ANYCRLF   == PCRE_NEWLINE_ANYCRLF);
G_STATIC_ASSERT (G_REGEX_BSR_ANYCRLF       == PCRE_BSR_ANYCRLF);
G_STATIC_ASSERT (G_REGEX_JAVASCRIPT_COMPAT == PCRE_JAVASCRIPT_COMPAT);

G_STATIC_ASSERT (G_REGEX_MATCH_ANCHORED         == PCRE_ANCHORED);
G_STATIC_ASSERT (G_REGEX_MATCH_NOTBOL           == PCRE_NOTBOL);
G_STATIC_ASSERT (G_REGEX_MATCH_NOTEOL           == PCRE_NOTEOL);
G_STATIC_ASSERT (G_REGEX_MATCH_NOTEMPTY         == PCRE_NOTEMPTY);
G_STATIC_ASSERT (G_REGEX_MATCH_PARTIAL          == PCRE_PARTIAL);
G_STATIC_ASSERT (G_REGEX_MATCH_NEWLINE_CR       == PCRE_NEWLINE_CR);
G_STATIC_ASSERT (G_REGEX_MATCH_NEWLINE_LF       == PCRE_NEWLINE_LF);
G_STATIC_ASSERT (G_REGEX_MATCH_NEWLINE_CRLF     == PCRE_NEWLINE_CRLF);
G_STATIC_ASSERT (G_REGEX_MATCH_NEWLINE_ANY      == PCRE_NEWLINE_ANY);
G_STATIC_ASSERT (G_REGEX_MATCH_NEWLINE_ANYCRLF  == PCRE_NEWLINE_ANYCRLF);
G_STATIC_ASSERT (G_REGEX_MATCH_BSR_ANYCRLF      == PCRE_BSR_ANYCRLF);
G_STATIC_ASSERT (G_REGEX_MATCH_BSR_ANY          == PCRE_BSR_UNICODE);
G_STATIC_ASSERT (G_REGEX_MATCH_PARTIAL_SOFT     == PCRE_PARTIAL_SOFT);
G_STATIC_ASSERT (G_REGEX_MATCH_PARTIAL_HARD     == PCRE_PARTIAL_HARD);
G_STATIC_ASSERT (G_REGEX_MATCH_NOTEMPTY_ATSTART == PCRE_NOTEMPTY_ATSTART);

/* These PCRE flags are unused or not exposed publically in GRegexFlags, so
 * it should be ok to reuse them for different things.
 */
G_STATIC_ASSERT (G_REGEX_OPTIMIZE          == PCRE_NO_UTF8_CHECK);
G_STATIC_ASSERT (G_REGEX_RAW               == PCRE_UTF8);

/* if the string is in UTF-8 use g_utf8_ functions, else use
 * use just +/- 1. */
#define NEXT_CHAR(re, s) (((re)->compile_opts & G_REGEX_RAW) ? \
                                ((s) + 1) : \
                                g_utf8_next_char (s))
#define PREV_CHAR(re, s) (((re)->compile_opts & G_REGEX_RAW) ? \
                                ((s) - 1) : \
                                g_utf8_prev_char (s))

struct _GMatchInfo
{
  volatile gint ref_count;      /* the ref count */
  GRegex *regex;                /* the regex */
  GRegexMatchFlags match_opts;  /* options used at match time on the regex */
  gint matches;                 /* number of matching sub patterns */
  gint pos;                     /* position in the string where last match left off */
  gint  n_offsets;              /* number of offsets */
  gint *offsets;                /* array of offsets paired 0,1 ; 2,3 ; 3,4 etc */
  gint *workspace;              /* workspace for pcre_dfa_exec() */
  gint n_workspace;             /* number of workspace elements */
  const gchar *string;          /* string passed to the match function */
  gssize string_len;            /* length of string, in bytes */
};

struct _GRegex
{
  volatile gint ref_count;      /* the ref count for the immutable part */
  gchar *pattern;               /* the pattern */
  pcre *pcre_re;                /* compiled form of the pattern */
  GRegexCompileFlags compile_opts;      /* options used at compile time on the pattern */
  GRegexMatchFlags match_opts;  /* options used at match time on the regex */
  pcre_extra *extra;            /* data stored when G_REGEX_OPTIMIZE is used */
};

/* TRUE if ret is an error code, FALSE otherwise. */
#define IS_PCRE_ERROR(ret) ((ret) < PCRE_ERROR_NOMATCH && (ret) != PCRE_ERROR_PARTIAL)

typedef struct _InterpolationData InterpolationData;
static gboolean  interpolation_list_needs_match (GList *list);
static gboolean  interpolate_replacement        (const GMatchInfo *match_info,
                                                 GString *result,
                                                 gpointer data);
static GList    *split_replacement              (const gchar *replacement,
                                                 GError **error);
static void      free_interpolation_data        (InterpolationData *data);


static const gchar *
match_error (gint errcode)
{
  switch (errcode)
    {
    case PCRE_ERROR_NOMATCH:
      /* not an error */
      break;
    case PCRE_ERROR_NULL:
      /* NULL argument, this should not happen in GRegex */
      g_warning ("A NULL argument was passed to PCRE");
      break;
    case PCRE_ERROR_BADOPTION:
      return "bad options";
    case PCRE_ERROR_BADMAGIC:
      return _("corrupted object");
    case PCRE_ERROR_UNKNOWN_OPCODE:
      return N_("internal error or corrupted object");
    case PCRE_ERROR_NOMEMORY:
      return _("out of memory");
    case PCRE_ERROR_NOSUBSTRING:
      /* not used by pcre_exec() */
      break;
    case PCRE_ERROR_MATCHLIMIT:
      return _("backtracking limit reached");
    case PCRE_ERROR_CALLOUT:
      /* callouts are not implemented */
      break;
    case PCRE_ERROR_BADUTF8:
    case PCRE_ERROR_BADUTF8_OFFSET:
      /* we do not check if strings are valid */
      break;
    case PCRE_ERROR_PARTIAL:
      /* not an error */
      break;
    case PCRE_ERROR_BADPARTIAL:
      return _("the pattern contains items not supported for partial matching");
    case PCRE_ERROR_INTERNAL:
      return _("internal error");
    case PCRE_ERROR_BADCOUNT:
      /* negative ovecsize, this should not happen in GRegex */
      g_warning ("A negative ovecsize was passed to PCRE");
      break;
    case PCRE_ERROR_DFA_UITEM:
      return _("the pattern contains items not supported for partial matching");
    case PCRE_ERROR_DFA_UCOND:
      return _("back references as conditions are not supported for partial matching");
    case PCRE_ERROR_DFA_UMLIMIT:
      /* the match_field field is not used in GRegex */
      break;
    case PCRE_ERROR_DFA_WSSIZE:
      /* handled expanding the workspace */
      break;
    case PCRE_ERROR_DFA_RECURSE:
    case PCRE_ERROR_RECURSIONLIMIT:
      return _("recursion limit reached");
    case PCRE_ERROR_BADNEWLINE:
      return _("invalid combination of newline flags");
    case PCRE_ERROR_BADOFFSET:
      return _("bad offset");
    case PCRE_ERROR_SHORTUTF8:
      return _("short utf8");
    case PCRE_ERROR_RECURSELOOP:
      return _("recursion loop");
    default:
      break;
    }
  return _("unknown error");
}

static void
translate_compile_error (gint *errcode, const gchar **errmsg)
{
  /* Compile errors are created adding 100 to the error code returned
   * by PCRE.
   * If errcode is known we put the translatable error message in
   * erromsg. If errcode is unknown we put the generic
   * G_REGEX_ERROR_COMPILE error code in errcode and keep the
   * untranslated error message returned by PCRE.
   * Note that there can be more PCRE errors with the same GRegexError
   * and that some PCRE errors are useless for us.
   */
  *errcode += 100;

  switch (*errcode)
    {
    case G_REGEX_ERROR_STRAY_BACKSLASH:
      *errmsg = _("\\ at end of pattern");
      break;
    case G_REGEX_ERROR_MISSING_CONTROL_CHAR:
      *errmsg = _("\\c at end of pattern");
      break;
    case G_REGEX_ERROR_UNRECOGNIZED_ESCAPE:
      *errmsg = _("unrecognized character following \\");
      break;
    case G_REGEX_ERROR_QUANTIFIERS_OUT_OF_ORDER:
      *errmsg = _("numbers out of order in {} quantifier");
      break;
    case G_REGEX_ERROR_QUANTIFIER_TOO_BIG:
      *errmsg = _("number too big in {} quantifier");
      break;
    case G_REGEX_ERROR_UNTERMINATED_CHARACTER_CLASS:
      *errmsg = _("missing terminating ] for character class");
      break;
    case G_REGEX_ERROR_INVALID_ESCAPE_IN_CHARACTER_CLASS:
      *errmsg = _("invalid escape sequence in character class");
      break;
    case G_REGEX_ERROR_RANGE_OUT_OF_ORDER:
      *errmsg = _("range out of order in character class");
      break;
    case G_REGEX_ERROR_NOTHING_TO_REPEAT:
      *errmsg = _("nothing to repeat");
      break;
    case 111: /* internal error: unexpected repeat */
      *errcode = G_REGEX_ERROR_INTERNAL;
      *errmsg = _("unexpected repeat");
      break;
    case G_REGEX_ERROR_UNRECOGNIZED_CHARACTER:
      *errmsg = _("unrecognized character after (? or (?-");
      break;
    case G_REGEX_ERROR_POSIX_NAMED_CLASS_OUTSIDE_CLASS:
      *errmsg = _("POSIX named classes are supported only within a class");
      break;
    case G_REGEX_ERROR_UNMATCHED_PARENTHESIS:
      *errmsg = _("missing terminating )");
      break;
    case G_REGEX_ERROR_INEXISTENT_SUBPATTERN_REFERENCE:
      *errmsg = _("reference to non-existent subpattern");
      break;
    case G_REGEX_ERROR_UNTERMINATED_COMMENT:
      *errmsg = _("missing ) after comment");
      break;
    case G_REGEX_ERROR_EXPRESSION_TOO_LARGE:
      *errmsg = _("regular expression is too large");
      break;
    case G_REGEX_ERROR_MEMORY_ERROR:
      *errmsg = _("failed to get memory");
      break;
    case 122: /* unmatched parentheses */
      *errcode = G_REGEX_ERROR_UNMATCHED_PARENTHESIS;
      *errmsg = _(") without opening (");
      break;
    case 123: /* internal error: code overflow */
      *errcode = G_REGEX_ERROR_INTERNAL;
      *errmsg = _("code overflow");
      break;
    case 124: /* "unrecognized character after (?<\0 */
      *errcode = G_REGEX_ERROR_UNRECOGNIZED_CHARACTER;
      *errmsg = _("unrecognized character after (?<");
      break;
    case G_REGEX_ERROR_VARIABLE_LENGTH_LOOKBEHIND:
      *errmsg = _("lookbehind assertion is not fixed length");
      break;
    case G_REGEX_ERROR_MALFORMED_CONDITION:
      *errmsg = _("malformed number or name after (?(");
      break;
    case G_REGEX_ERROR_TOO_MANY_CONDITIONAL_BRANCHES:
      *errmsg = _("conditional group contains more than two branches");
      break;
    case G_REGEX_ERROR_ASSERTION_EXPECTED:
      *errmsg = _("assertion expected after (?(");
      break;
    case 129:
      *errcode = G_REGEX_ERROR_UNMATCHED_PARENTHESIS;
      /* translators: '(?R' and '(?[+-]digits' are both meant as (groups of)
       * sequences here, '(?-54' would be an example for the second group.
       */
      *errmsg = _("(?R or (?[+-]digits must be followed by )");
      break;
    case G_REGEX_ERROR_UNKNOWN_POSIX_CLASS_NAME:
      *errmsg = _("unknown POSIX class name");
      break;
    case G_REGEX_ERROR_POSIX_COLLATING_ELEMENTS_NOT_SUPPORTED:
      *errmsg = _("POSIX collating elements are not supported");
      break;
    case G_REGEX_ERROR_HEX_CODE_TOO_LARGE:
      *errmsg = _("character value in \\x{...} sequence is too large");
      break;
    case G_REGEX_ERROR_INVALID_CONDITION:
      *errmsg = _("invalid condition (?(0)");
      break;
    case G_REGEX_ERROR_SINGLE_BYTE_MATCH_IN_LOOKBEHIND:
      *errmsg = _("\\C not allowed in lookbehind assertion");
      break;
    case 137: /* PCRE does not support \\L, \\l, \\N{name}, \\U, or \\u\0 */
      /* A number of Perl escapes are not handled by PCRE.
       * Therefore it explicitly raises ERR37.
       */
      *errcode = G_REGEX_ERROR_UNRECOGNIZED_ESCAPE;
      *errmsg = _("escapes \\L, \\l, \\N{name}, \\U, and \\u are not supported");
      break;
    case G_REGEX_ERROR_INFINITE_LOOP:
      *errmsg = _("recursive call could loop indefinitely");
      break;
    case 141: /* unrecognized character after (?P\0 */
      *errcode = G_REGEX_ERROR_UNRECOGNIZED_CHARACTER;
      *errmsg = _("unrecognized character after (?P");
      break;
    case G_REGEX_ERROR_MISSING_SUBPATTERN_NAME_TERMINATOR:
      *errmsg = _("missing terminator in subpattern name");
      break;
    case G_REGEX_ERROR_DUPLICATE_SUBPATTERN_NAME:
      *errmsg = _("two named subpatterns have the same name");
      break;
    case G_REGEX_ERROR_MALFORMED_PROPERTY:
      *errmsg = _("malformed \\P or \\p sequence");
      break;
    case G_REGEX_ERROR_UNKNOWN_PROPERTY:
      *errmsg = _("unknown property name after \\P or \\p");
      break;
    case G_REGEX_ERROR_SUBPATTERN_NAME_TOO_LONG:
      *errmsg = _("subpattern name is too long (maximum 32 characters)");
      break;
    case G_REGEX_ERROR_TOO_MANY_SUBPATTERNS:
      *errmsg = _("too many named subpatterns (maximum 10,000)");
      break;
    case G_REGEX_ERROR_INVALID_OCTAL_VALUE:
      *errmsg = _("octal value is greater than \\377");
      break;
    case 152: /* internal error: overran compiling workspace */
      *errcode = G_REGEX_ERROR_INTERNAL;
      *errmsg = _("overran compiling workspace");
      break;
    case 153: /* internal error: previously-checked referenced subpattern not found */
      *errcode = G_REGEX_ERROR_INTERNAL;
      *errmsg = _("previously-checked referenced subpattern not found");
      break;
    case G_REGEX_ERROR_TOO_MANY_BRANCHES_IN_DEFINE:
      *errmsg = _("DEFINE group contains more than one branch");
      break;
    case G_REGEX_ERROR_INCONSISTENT_NEWLINE_OPTIONS:
      *errmsg = _("inconsistent NEWLINE options");
      break;
    case G_REGEX_ERROR_MISSING_BACK_REFERENCE:
      *errmsg = _("\\g is not followed by a braced, angle-bracketed, or quoted name or "
                  "number, or by a plain number");
      break;
    case G_REGEX_ERROR_INVALID_RELATIVE_REFERENCE:
      *errmsg = _("a numbered reference must not be zero");
      break;
    case G_REGEX_ERROR_BACKTRACKING_CONTROL_VERB_ARGUMENT_FORBIDDEN:
      *errmsg = _("an argument is not allowed for (*ACCEPT), (*FAIL), or (*COMMIT)");
      break;
    case G_REGEX_ERROR_UNKNOWN_BACKTRACKING_CONTROL_VERB:
      *errmsg = _("(*VERB) not recognized");
      break;
    case G_REGEX_ERROR_NUMBER_TOO_BIG:
      *errmsg = _("number is too big");
      break;
    case G_REGEX_ERROR_MISSING_SUBPATTERN_NAME:
      *errmsg = _("missing subpattern name after (?&");
      break;
    case G_REGEX_ERROR_MISSING_DIGIT:
      *errmsg = _("digit expected after (?+");
      break;
    case G_REGEX_ERROR_INVALID_DATA_CHARACTER:
      *errmsg = _("] is an invalid data character in JavaScript compatibility mode");
      break;
    case G_REGEX_ERROR_EXTRA_SUBPATTERN_NAME:
      *errmsg = _("different names for subpatterns of the same number are not allowed");
      break;
    case G_REGEX_ERROR_BACKTRACKING_CONTROL_VERB_ARGUMENT_REQUIRED:
      *errmsg = _("(*MARK) must have an argument");
      break;
    case G_REGEX_ERROR_INVALID_CONTROL_CHAR:
      *errmsg = _( "\\c must be followed by an ASCII character");
      break;
    case G_REGEX_ERROR_MISSING_NAME:
      *errmsg = _("\\k is not followed by a braced, angle-bracketed, or quoted name");
      break;
    case G_REGEX_ERROR_NOT_SUPPORTED_IN_CLASS:
      *errmsg = _("\\N is not supported in a class");
      break;
    case G_REGEX_ERROR_TOO_MANY_FORWARD_REFERENCES:
      *errmsg = _("too many forward references");
      break;
    case G_REGEX_ERROR_NAME_TOO_LONG:
      *errmsg = _("name is too long in (*MARK), (*PRUNE), (*SKIP), or (*THEN)");
      break;
    case G_REGEX_ERROR_CHARACTER_VALUE_TOO_LARGE:
      *errmsg = _("character value in \\u.... sequence is too large");
      break;

    case 116: /* erroffset passed as NULL */
      /* This should not happen as we never pass a NULL erroffset */
      g_warning ("erroffset passed as NULL");
      *errcode = G_REGEX_ERROR_COMPILE;
      break;
    case 117: /* unknown option bit(s) set */
      /* This should not happen as we check options before passing them
       * to pcre_compile2() */
      g_warning ("unknown option bit(s) set");
      *errcode = G_REGEX_ERROR_COMPILE;
      break;
    case 132: /* this version of PCRE is compiled without UTF support */
    case 144: /* invalid UTF-8 string */
    case 145: /* support for \\P, \\p, and \\X has not been compiled */
    case 167: /* this version of PCRE is not compiled with Unicode property support */
    case 173: /* disallowed Unicode code point (>= 0xd800 && <= 0xdfff) */
    case 174: /* invalid UTF-16 string */
      /* These errors should not happen as we are using an UTF-8 and UCP-enabled PCRE
       * and we do not check if strings are valid */
    case 170: /* internal error: unknown opcode in find_fixedlength() */
      *errcode = G_REGEX_ERROR_INTERNAL;
      break;

    default:
      *errcode = G_REGEX_ERROR_COMPILE;
    }
}

/* GMatchInfo */

static GMatchInfo *
match_info_new (const GRegex *regex,
                const gchar  *string,
                gint          string_len,
                gint          start_position,
                gint          match_options,
                gboolean      is_dfa)
{
  GMatchInfo *match_info;

  if (string_len < 0)
    string_len = strlen (string);

  match_info = g_new0 (GMatchInfo, 1);
  match_info->ref_count = 1;
  match_info->regex = g_regex_ref ((GRegex *)regex);
  match_info->string = string;
  match_info->string_len = string_len;
  match_info->matches = PCRE_ERROR_NOMATCH;
  match_info->pos = start_position;
  match_info->match_opts = match_options;

  if (is_dfa)
    {
      /* These values should be enough for most cases, if they are not
       * enough g_regex_match_all_full() will expand them. */
      match_info->n_offsets = 24;
      match_info->n_workspace = 100;
      match_info->workspace = g_new (gint, match_info->n_workspace);
    }
  else
    {
      gint capture_count;
      pcre_fullinfo (regex->pcre_re, regex->extra,
                     PCRE_INFO_CAPTURECOUNT, &capture_count);
      match_info->n_offsets = (capture_count + 1) * 3;
    }

  match_info->offsets = g_new0 (gint, match_info->n_offsets);
  /* Set an invalid position for the previous match. */
  match_info->offsets[0] = -1;
  match_info->offsets[1] = -1;

  return match_info;
}

/**
 * g_match_info_get_regex:
 * @match_info: a #GMatchInfo
 *
 * Returns #GRegex object used in @match_info. It belongs to Glib
 * and must not be freed. Use g_regex_ref() if you need to keep it
 * after you free @match_info object.
 *
 * Returns: #GRegex object used in @match_info
 *
 * Since: 2.14
 */
GRegex *
g_match_info_get_regex (const GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info != NULL, NULL);
  return match_info->regex;
}

/**
 * g_match_info_get_string:
 * @match_info: a #GMatchInfo
 *
 * Returns the string searched with @match_info. This is the
 * string passed to g_regex_match() or g_regex_replace() so
 * you may not free it before calling this function.
 *
 * Returns: the string searched with @match_info
 *
 * Since: 2.14
 */
const gchar *
g_match_info_get_string (const GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info != NULL, NULL);
  return match_info->string;
}

/**
 * g_match_info_ref:
 * @match_info: a #GMatchInfo
 *
 * Increases reference count of @match_info by 1.
 *
 * Returns: @match_info
 *
 * Since: 2.30
 */
GMatchInfo       *
g_match_info_ref (GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info != NULL, NULL);
  g_atomic_int_inc (&match_info->ref_count);
  return match_info;
}

/**
 * g_match_info_unref:
 * @match_info: a #GMatchInfo
 *
 * Decreases reference count of @match_info by 1. When reference count drops
 * to zero, it frees all the memory associated with the match_info structure.
 *
 * Since: 2.30
 */
void
g_match_info_unref (GMatchInfo *match_info)
{
  if (g_atomic_int_dec_and_test (&match_info->ref_count))
    {
      g_regex_unref (match_info->regex);
      g_free (match_info->offsets);
      g_free (match_info->workspace);
      g_free (match_info);
    }
}

/**
 * g_match_info_free:
 * @match_info: (nullable): a #GMatchInfo, or %NULL
 *
 * If @match_info is not %NULL, calls g_match_info_unref(); otherwise does
 * nothing.
 *
 * Since: 2.14
 */
void
g_match_info_free (GMatchInfo *match_info)
{
  if (match_info == NULL)
    return;

  g_match_info_unref (match_info);
}

/**
 * g_match_info_next:
 * @match_info: a #GMatchInfo structure
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Scans for the next match using the same parameters of the previous
 * call to g_regex_match_full() or g_regex_match() that returned
 * @match_info.
 *
 * The match is done on the string passed to the match function, so you
 * cannot free it before calling this function.
 *
 * Returns: %TRUE is the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_match_info_next (GMatchInfo  *match_info,
                   GError     **error)
{
  gint prev_match_start;
  gint prev_match_end;

  g_return_val_if_fail (match_info != NULL, FALSE);
  g_return_val_if_fail (error == NULL || *error == NULL, FALSE);
  g_return_val_if_fail (match_info->pos >= 0, FALSE);

  prev_match_start = match_info->offsets[0];
  prev_match_end = match_info->offsets[1];

  if (match_info->pos > match_info->string_len)
    {
      /* we have reached the end of the string */
      match_info->pos = -1;
      match_info->matches = PCRE_ERROR_NOMATCH;
      return FALSE;
    }

  match_info->matches = pcre_exec (match_info->regex->pcre_re,
                                   match_info->regex->extra,
                                   match_info->string,
                                   match_info->string_len,
                                   match_info->pos,
                                   match_info->regex->match_opts | match_info->match_opts,
                                   match_info->offsets,
                                   match_info->n_offsets);
  if (IS_PCRE_ERROR (match_info->matches))
    {
      g_set_error (error, G_REGEX_ERROR, G_REGEX_ERROR_MATCH,
                   _("Error while matching regular expression %s: %s"),
                   match_info->regex->pattern, match_error (match_info->matches));
      return FALSE;
    }

  /* avoid infinite loops if the pattern is an empty string or something
   * equivalent */
  if (match_info->pos == match_info->offsets[1])
    {
      if (match_info->pos > match_info->string_len)
        {
          /* we have reached the end of the string */
          match_info->pos = -1;
          match_info->matches = PCRE_ERROR_NOMATCH;
          return FALSE;
        }

      match_info->pos = NEXT_CHAR (match_info->regex,
                                   &match_info->string[match_info->pos]) -
                                   match_info->string;
    }
  else
    {
      match_info->pos = match_info->offsets[1];
    }

  /* it's possible to get two identical matches when we are matching
   * empty strings, for instance if the pattern is "(?=[A-Z0-9])" and
   * the string is "RegExTest" we have:
   *  - search at position 0: match from 0 to 0
   *  - search at position 1: match from 3 to 3
   *  - search at position 3: match from 3 to 3 (duplicate)
   *  - search at position 4: match from 5 to 5
   *  - search at position 5: match from 5 to 5 (duplicate)
   *  - search at position 6: no match -> stop
   * so we have to ignore the duplicates.
   * see bug #515944: http://bugzilla.gnome.org/show_bug.cgi?id=515944 */
  if (match_info->matches >= 0 &&
      prev_match_start == match_info->offsets[0] &&
      prev_match_end == match_info->offsets[1])
    {
      /* ignore this match and search the next one */
      return g_match_info_next (match_info, error);
    }

  return match_info->matches >= 0;
}

/**
 * g_match_info_matches:
 * @match_info: a #GMatchInfo structure
 *
 * Returns whether the previous match operation succeeded.
 *
 * Returns: %TRUE if the previous match operation succeeded,
 *   %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_match_info_matches (const GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info != NULL, FALSE);

  return match_info->matches >= 0;
}

/**
 * g_match_info_get_match_count:
 * @match_info: a #GMatchInfo structure
 *
 * Retrieves the number of matched substrings (including substring 0,
 * that is the whole matched text), so 1 is returned if the pattern
 * has no substrings in it and 0 is returned if the match failed.
 *
 * If the last match was obtained using the DFA algorithm, that is
 * using g_regex_match_all() or g_regex_match_all_full(), the retrieved
 * count is not that of the number of capturing parentheses but that of
 * the number of matched substrings.
 *
 * Returns: Number of matched substrings, or -1 if an error occurred
 *
 * Since: 2.14
 */
gint
g_match_info_get_match_count (const GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info, -1);

  if (match_info->matches == PCRE_ERROR_NOMATCH)
    /* no match */
    return 0;
  else if (match_info->matches < PCRE_ERROR_NOMATCH)
    /* error */
    return -1;
  else
    /* match */
    return match_info->matches;
}

/**
 * g_match_info_is_partial_match:
 * @match_info: a #GMatchInfo structure
 *
 * Usually if the string passed to g_regex_match*() matches as far as
 * it goes, but is too short to match the entire pattern, %FALSE is
 * returned. There are circumstances where it might be helpful to
 * distinguish this case from other cases in which there is no match.
 *
 * Consider, for example, an application where a human is required to
 * type in data for a field with specific formatting requirements. An
 * example might be a date in the form ddmmmyy, defined by the pattern
 * "^\d?\d(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\d\d$".
 * If the application sees the user's keystrokes one by one, and can
 * check that what has been typed so far is potentially valid, it is
 * able to raise an error as soon as a mistake is made.
 *
 * GRegex supports the concept of partial matching by means of the
 * #G_REGEX_MATCH_PARTIAL_SOFT and #G_REGEX_MATCH_PARTIAL_HARD flags.
 * When they are used, the return code for
 * g_regex_match() or g_regex_match_full() is, as usual, %TRUE
 * for a complete match, %FALSE otherwise. But, when these functions
 * return %FALSE, you can check if the match was partial calling
 * g_match_info_is_partial_match().
 *
 * The difference between #G_REGEX_MATCH_PARTIAL_SOFT and
 * #G_REGEX_MATCH_PARTIAL_HARD is that when a partial match is encountered
 * with #G_REGEX_MATCH_PARTIAL_SOFT, matching continues to search for a
 * possible complete match, while with #G_REGEX_MATCH_PARTIAL_HARD matching
 * stops at the partial match.
 * When both #G_REGEX_MATCH_PARTIAL_SOFT and #G_REGEX_MATCH_PARTIAL_HARD
 * are set, the latter takes precedence.
 *
 * There were formerly some restrictions on the pattern for partial matching.
 * The restrictions no longer apply.
 *
 * See pcrepartial(3) for more information on partial matching.
 *
 * Returns: %TRUE if the match was partial, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_match_info_is_partial_match (const GMatchInfo *match_info)
{
  g_return_val_if_fail (match_info != NULL, FALSE);

  return match_info->matches == PCRE_ERROR_PARTIAL;
}

/**
 * g_match_info_expand_references:
 * @match_info: (nullable): a #GMatchInfo or %NULL
 * @string_to_expand: the string to expand
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Returns a new string containing the text in @string_to_expand with
 * references and escape sequences expanded. References refer to the last
 * match done with @string against @regex and have the same syntax used by
 * g_regex_replace().
 *
 * The @string_to_expand must be UTF-8 encoded even if #G_REGEX_RAW was
 * passed to g_regex_new().
 *
 * The backreferences are extracted from the string passed to the match
 * function, so you cannot call this function after freeing the string.
 *
 * @match_info may be %NULL in which case @string_to_expand must not
 * contain references. For instance "foo\n" does not refer to an actual
 * pattern and '\n' merely will be replaced with \n character,
 * while to expand "\0" (whole match) one needs the result of a match.
 * Use g_regex_check_replacement() to find out whether @string_to_expand
 * contains references.
 *
 * Returns: (nullable): the expanded string, or %NULL if an error occurred
 *
 * Since: 2.14
 */
gchar *
g_match_info_expand_references (const GMatchInfo  *match_info,
                                const gchar       *string_to_expand,
                                GError           **error)
{
  GString *result;
  GList *list;
  GError *tmp_error = NULL;

  g_return_val_if_fail (string_to_expand != NULL, NULL);
  g_return_val_if_fail (error == NULL || *error == NULL, NULL);

  list = split_replacement (string_to_expand, &tmp_error);
  if (tmp_error != NULL)
    {
      g_propagate_error (error, tmp_error);
      return NULL;
    }

  if (!match_info && interpolation_list_needs_match (list))
    {
      g_critical ("String '%s' contains references to the match, can't "
                  "expand references without GMatchInfo object",
                  string_to_expand);
      return NULL;
    }

  result = g_string_sized_new (strlen (string_to_expand));
  interpolate_replacement (match_info, result, list);

  g_list_free_full (list, (GDestroyNotify) free_interpolation_data);

  return g_string_free (result, FALSE);
}

/**
 * g_match_info_fetch:
 * @match_info: #GMatchInfo structure
 * @match_num: number of the sub expression
 *
 * Retrieves the text matching the @match_num'th capturing
 * parentheses. 0 is the full text of the match, 1 is the first paren
 * set, 2 the second, and so on.
 *
 * If @match_num is a valid sub pattern but it didn't match anything
 * (e.g. sub pattern 1, matching "b" against "(a)?b") then an empty
 * string is returned.
 *
 * If the match was obtained using the DFA algorithm, that is using
 * g_regex_match_all() or g_regex_match_all_full(), the retrieved
 * string is not that of a set of parentheses but that of a matched
 * substring. Substrings are matched in reverse order of length, so
 * 0 is the longest match.
 *
 * The string is fetched from the string passed to the match function,
 * so you cannot call this function after freeing the string.
 *
 * Returns: (nullable): The matched substring, or %NULL if an error
 *     occurred. You have to free the string yourself
 *
 * Since: 2.14
 */
gchar *
g_match_info_fetch (const GMatchInfo *match_info,
                    gint              match_num)
{
  /* we cannot use pcre_get_substring() because it allocates the
   * string using pcre_malloc(). */
  gchar *match = NULL;
  gint start, end;

  g_return_val_if_fail (match_info != NULL, NULL);
  g_return_val_if_fail (match_num >= 0, NULL);

  /* match_num does not exist or it didn't matched, i.e. matching "b"
   * against "(a)?b" then group 0 is empty. */
  if (!g_match_info_fetch_pos (match_info, match_num, &start, &end))
    match = NULL;
  else if (start == -1)
    match = g_strdup ("");
  else
    match = g_strndup (&match_info->string[start], end - start);

  return match;
}

/**
 * g_match_info_fetch_pos:
 * @match_info: #GMatchInfo structure
 * @match_num: number of the sub expression
 * @start_pos: (out) (optional): pointer to location where to store
 *     the start position, or %NULL
 * @end_pos: (out) (optional): pointer to location where to store
 *     the end position, or %NULL
 *
 * Retrieves the position in bytes of the @match_num'th capturing
 * parentheses. 0 is the full text of the match, 1 is the first
 * paren set, 2 the second, and so on.
 *
 * If @match_num is a valid sub pattern but it didn't match anything
 * (e.g. sub pattern 1, matching "b" against "(a)?b") then @start_pos
 * and @end_pos are set to -1 and %TRUE is returned.
 *
 * If the match was obtained using the DFA algorithm, that is using
 * g_regex_match_all() or g_regex_match_all_full(), the retrieved
 * position is not that of a set of parentheses but that of a matched
 * substring. Substrings are matched in reverse order of length, so
 * 0 is the longest match.
 *
 * Returns: %TRUE if the position was fetched, %FALSE otherwise. If
 *   the position cannot be fetched, @start_pos and @end_pos are left
 *   unchanged
 *
 * Since: 2.14
 */
gboolean
g_match_info_fetch_pos (const GMatchInfo *match_info,
                        gint              match_num,
                        gint             *start_pos,
                        gint             *end_pos)
{
  g_return_val_if_fail (match_info != NULL, FALSE);
  g_return_val_if_fail (match_num >= 0, FALSE);

  /* make sure the sub expression number they're requesting is less than
   * the total number of sub expressions that were matched. */
  if (match_num >= match_info->matches)
    return FALSE;

  if (start_pos != NULL)
    *start_pos = match_info->offsets[2 * match_num];

  if (end_pos != NULL)
    *end_pos = match_info->offsets[2 * match_num + 1];

  return TRUE;
}

/*
 * Returns number of first matched subpattern with name @name.
 * There may be more than one in case when DUPNAMES is used,
 * and not all subpatterns with that name match;
 * pcre_get_stringnumber() does not work in that case.
 */
static gint
get_matched_substring_number (const GMatchInfo *match_info,
                              const gchar      *name)
{
  gint entrysize;
  gchar *first, *last;
  guchar *entry;

  if (!(match_info->regex->compile_opts & G_REGEX_DUPNAMES))
    return pcre_get_stringnumber (match_info->regex->pcre_re, name);

  /* This code is copied from pcre_get.c: get_first_set() */
  entrysize = pcre_get_stringtable_entries (match_info->regex->pcre_re,
                                            name,
                                            &first,
                                            &last);

  if (entrysize <= 0)
    return entrysize;

  for (entry = (guchar*) first; entry <= (guchar*) last; entry += entrysize)
    {
      gint n = (entry[0] << 8) + entry[1];
      if (match_info->offsets[n*2] >= 0)
        return n;
    }

  return (first[0] << 8) + first[1];
}

/**
 * g_match_info_fetch_named:
 * @match_info: #GMatchInfo structure
 * @name: name of the subexpression
 *
 * Retrieves the text matching the capturing parentheses named @name.
 *
 * If @name is a valid sub pattern name but it didn't match anything
 * (e.g. sub pattern "X", matching "b" against "(?P<X>a)?b")
 * then an empty string is returned.
 *
 * The string is fetched from the string passed to the match function,
 * so you cannot call this function after freeing the string.
 *
 * Returns: (nullable): The matched substring, or %NULL if an error
 *     occurred. You have to free the string yourself
 *
 * Since: 2.14
 */
gchar *
g_match_info_fetch_named (const GMatchInfo *match_info,
                          const gchar      *name)
{
  /* we cannot use pcre_get_named_substring() because it allocates the
   * string using pcre_malloc(). */
  gint num;

  g_return_val_if_fail (match_info != NULL, NULL);
  g_return_val_if_fail (name != NULL, NULL);

  num = get_matched_substring_number (match_info, name);
  if (num < 0)
    return NULL;
  else
    return g_match_info_fetch (match_info, num);
}

/**
 * g_match_info_fetch_named_pos:
 * @match_info: #GMatchInfo structure
 * @name: name of the subexpression
 * @start_pos: (out) (optional): pointer to location where to store
 *     the start position, or %NULL
 * @end_pos: (out) (optional): pointer to location where to store
 *     the end position, or %NULL
 *
 * Retrieves the position in bytes of the capturing parentheses named @name.
 *
 * If @name is a valid sub pattern name but it didn't match anything
 * (e.g. sub pattern "X", matching "b" against "(?P<X>a)?b")
 * then @start_pos and @end_pos are set to -1 and %TRUE is returned.
 *
 * Returns: %TRUE if the position was fetched, %FALSE otherwise.
 *     If the position cannot be fetched, @start_pos and @end_pos
 *     are left unchanged.
 *
 * Since: 2.14
 */
gboolean
g_match_info_fetch_named_pos (const GMatchInfo *match_info,
                              const gchar      *name,
                              gint             *start_pos,
                              gint             *end_pos)
{
  gint num;

  g_return_val_if_fail (match_info != NULL, FALSE);
  g_return_val_if_fail (name != NULL, FALSE);

  num = get_matched_substring_number (match_info, name);
  if (num < 0)
    return FALSE;

  return g_match_info_fetch_pos (match_info, num, start_pos, end_pos);
}

/**
 * g_match_info_fetch_all:
 * @match_info: a #GMatchInfo structure
 *
 * Bundles up pointers to each of the matching substrings from a match
 * and stores them in an array of gchar pointers. The first element in
 * the returned array is the match number 0, i.e. the entire matched
 * text.
 *
 * If a sub pattern didn't match anything (e.g. sub pattern 1, matching
 * "b" against "(a)?b") then an empty string is inserted.
 *
 * If the last match was obtained using the DFA algorithm, that is using
 * g_regex_match_all() or g_regex_match_all_full(), the retrieved
 * strings are not that matched by sets of parentheses but that of the
 * matched substring. Substrings are matched in reverse order of length,
 * so the first one is the longest match.
 *
 * The strings are fetched from the string passed to the match function,
 * so you cannot call this function after freeing the string.
 *
 * Returns: (transfer full): a %NULL-terminated array of gchar *
 *     pointers.  It must be freed using g_strfreev(). If the previous
 *     match failed %NULL is returned
 *
 * Since: 2.14
 */
gchar **
g_match_info_fetch_all (const GMatchInfo *match_info)
{
  /* we cannot use pcre_get_substring_list() because the returned value
   * isn't suitable for g_strfreev(). */
  gchar **result;
  gint i;

  g_return_val_if_fail (match_info != NULL, NULL);

  if (match_info->matches < 0)
    return NULL;

  result = g_new (gchar *, match_info->matches + 1);
  for (i = 0; i < match_info->matches; i++)
    result[i] = g_match_info_fetch (match_info, i);
  result[i] = NULL;

  return result;
}


/* GRegex */

G_DEFINE_QUARK (g-regex-error-quark, g_regex_error)

/**
 * g_regex_ref:
 * @regex: a #GRegex
 *
 * Increases reference count of @regex by 1.
 *
 * Returns: @regex
 *
 * Since: 2.14
 */
GRegex *
g_regex_ref (GRegex *regex)
{
  g_return_val_if_fail (regex != NULL, NULL);
  g_atomic_int_inc (&regex->ref_count);
  return regex;
}

/**
 * g_regex_unref:
 * @regex: a #GRegex
 *
 * Decreases reference count of @regex by 1. When reference count drops
 * to zero, it frees all the memory associated with the regex structure.
 *
 * Since: 2.14
 */
void
g_regex_unref (GRegex *regex)
{
  g_return_if_fail (regex != NULL);

  if (g_atomic_int_dec_and_test (&regex->ref_count))
    {
      g_free (regex->pattern);
      if (regex->pcre_re != NULL)
        pcre_free (regex->pcre_re);
      if (regex->extra != NULL)
        pcre_free (regex->extra);
      g_free (regex);
    }
}

/*
 * @match_options: (inout) (optional):
 */
static pcre *regex_compile (const gchar         *pattern,
                            GRegexCompileFlags   compile_options,
                            GRegexCompileFlags  *compile_options_out,
                            GRegexMatchFlags    *match_options,
                            GError             **error);

/**
 * g_regex_new:
 * @pattern: the regular expression
 * @compile_options: compile options for the regular expression, or 0
 * @match_options: match options for the regular expression, or 0
 * @error: return location for a #GError
 *
 * Compiles the regular expression to an internal form, and does
 * the initial setup of the #GRegex structure.
 *
 * Returns: (nullable): a #GRegex structure or %NULL if an error occured. Call
 *   g_regex_unref() when you are done with it
 *
 * Since: 2.14
 */
GRegex *
g_regex_new (const gchar         *pattern,
             GRegexCompileFlags   compile_options,
             GRegexMatchFlags     match_options,
             GError             **error)
{
  GRegex *regex;
  pcre *re;
  const gchar *errmsg;
  gboolean optimize = FALSE;
  static volatile gsize initialised = 0;

  g_return_val_if_fail (pattern != NULL, NULL);
  g_return_val_if_fail (error == NULL || *error == NULL, NULL);
  g_return_val_if_fail ((compile_options & ~G_REGEX_COMPILE_MASK) == 0, NULL);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, NULL);

  if (g_once_init_enter (&initialised))
    {
      int supports_utf8, supports_ucp;

      pcre_config (PCRE_CONFIG_UTF8, &supports_utf8);
      if (!supports_utf8)
        g_critical (_("PCRE library is compiled without UTF8 support"));

      pcre_config (PCRE_CONFIG_UNICODE_PROPERTIES, &supports_ucp);
      if (!supports_ucp)
        g_critical (_("PCRE library is compiled without UTF8 properties support"));

      g_once_init_leave (&initialised, supports_utf8 && supports_ucp ? 1 : 2);
    }

  if (G_UNLIKELY (initialised != 1))
    {
      g_set_error_literal (error, G_REGEX_ERROR, G_REGEX_ERROR_COMPILE,
                           _("PCRE library is compiled with incompatible options"));
      return NULL;
    }

  /* G_REGEX_OPTIMIZE has the same numeric value of PCRE_NO_UTF8_CHECK,
   * as we do not need to wrap PCRE_NO_UTF8_CHECK. */
  if (compile_options & G_REGEX_OPTIMIZE)
    optimize = TRUE;

  re = regex_compile (pattern, compile_options, &compile_options,
                      &match_options, error);

  if (re == NULL)
    return NULL;

  regex = g_new0 (GRegex, 1);
  regex->ref_count = 1;
  regex->pattern = g_strdup (pattern);
  regex->pcre_re = re;
  regex->compile_opts = compile_options;
  regex->match_opts = match_options;

  if (optimize)
    {
      regex->extra = pcre_study (regex->pcre_re, 0, &errmsg);
      if (errmsg != NULL)
        {
          GError *tmp_error = g_error_new (G_REGEX_ERROR,
                                           G_REGEX_ERROR_OPTIMIZE,
                                           _("Error while optimizing "
                                             "regular expression %s: %s"),
                                           regex->pattern,
                                           errmsg);
          g_propagate_error (error, tmp_error);

          g_regex_unref (regex);
          return NULL;
        }
    }

  return regex;
}

static pcre *
regex_compile (const gchar         *pattern,
               GRegexCompileFlags   compile_options,
               GRegexCompileFlags  *compile_options_out,
               GRegexMatchFlags    *match_options,
               GError             **error)
{
  pcre *re;
  const gchar *errmsg;
  gint erroffset;
  gint errcode;
  GRegexCompileFlags nonpcre_compile_options;
  unsigned long int pcre_compile_options;

  nonpcre_compile_options = compile_options & G_REGEX_COMPILE_NONPCRE_MASK;

  /* In GRegex the string are, by default, UTF-8 encoded. PCRE
   * instead uses UTF-8 only if required with PCRE_UTF8. */
  if (compile_options & G_REGEX_RAW)
    {
      /* disable utf-8 */
      compile_options &= ~G_REGEX_RAW;
    }
  else
    {
      /* enable utf-8 */
      compile_options |= PCRE_UTF8 | PCRE_NO_UTF8_CHECK;

      if (match_options != NULL)
        *match_options |= PCRE_NO_UTF8_CHECK;
    }

  /* PCRE_NEWLINE_ANY is the default for the internal PCRE but
   * not for the system one. */
  if (!(compile_options & G_REGEX_NEWLINE_CR) &&
      !(compile_options & G_REGEX_NEWLINE_LF))
    {
      compile_options |= PCRE_NEWLINE_ANY;
    }

  compile_options |= PCRE_UCP;

  /* PCRE_BSR_UNICODE is the default for the internal PCRE but
   * possibly not for the system one.
   */
  if (~compile_options & G_REGEX_BSR_ANYCRLF)
    compile_options |= PCRE_BSR_UNICODE;

  /* compile the pattern */
  re = pcre_compile2 (pattern, compile_options, &errcode,
                      &errmsg, &erroffset, NULL);

  /* if the compilation failed, set the error member and return
   * immediately */
  if (re == NULL)
    {
      GError *tmp_error;

      /* Translate the PCRE error code to GRegexError and use a translated
       * error message if possible */
      translate_compile_error (&errcode, &errmsg);

      /* PCRE uses byte offsets but we want to show character offsets */
      erroffset = g_utf8_pointer_to_offset (pattern, &pattern[erroffset]);

      tmp_error = g_error_new (G_REGEX_ERROR, errcode,
                               _("Error while compiling regular "
                                 "expression %s at char %d: %s"),
                               pattern, erroffset, errmsg);
      g_propagate_error (error, tmp_error);

      return NULL;
    }

  /* For options set at the beginning of the pattern, pcre puts them into
   * compile options, e.g. "(?i)foo" will make the pcre structure store
   * PCRE_CASELESS even though it wasn't explicitly given for compilation. */
  pcre_fullinfo (re, NULL, PCRE_INFO_OPTIONS, &pcre_compile_options);
  compile_options = pcre_compile_options & G_REGEX_COMPILE_PCRE_MASK;

  /* Don't leak PCRE_NEWLINE_ANY, which is part of PCRE_NEWLINE_ANYCRLF */
  if ((pcre_compile_options & PCRE_NEWLINE_ANYCRLF) != PCRE_NEWLINE_ANYCRLF)
    compile_options &= ~PCRE_NEWLINE_ANY;

  compile_options |= nonpcre_compile_options;

  if (!(compile_options & G_REGEX_DUPNAMES))
    {
      gboolean jchanged = FALSE;
      pcre_fullinfo (re, NULL, PCRE_INFO_JCHANGED, &jchanged);
      if (jchanged)
        compile_options |= G_REGEX_DUPNAMES;
    }

  if (compile_options_out != 0)
    *compile_options_out = compile_options;

  return re;
}

/**
 * g_regex_get_pattern:
 * @regex: a #GRegex structure
 *
 * Gets the pattern string associated with @regex, i.e. a copy of
 * the string passed to g_regex_new().
 *
 * Returns: the pattern of @regex
 *
 * Since: 2.14
 */
const gchar *
g_regex_get_pattern (const GRegex *regex)
{
  g_return_val_if_fail (regex != NULL, NULL);

  return regex->pattern;
}

/**
 * g_regex_get_max_backref:
 * @regex: a #GRegex
 *
 * Returns the number of the highest back reference
 * in the pattern, or 0 if the pattern does not contain
 * back references.
 *
 * Returns: the number of the highest back reference
 *
 * Since: 2.14
 */
gint
g_regex_get_max_backref (const GRegex *regex)
{
  gint value;

  pcre_fullinfo (regex->pcre_re, regex->extra,
                 PCRE_INFO_BACKREFMAX, &value);

  return value;
}

/**
 * g_regex_get_capture_count:
 * @regex: a #GRegex
 *
 * Returns the number of capturing subpatterns in the pattern.
 *
 * Returns: the number of capturing subpatterns
 *
 * Since: 2.14
 */
gint
g_regex_get_capture_count (const GRegex *regex)
{
  gint value;

  pcre_fullinfo (regex->pcre_re, regex->extra,
                 PCRE_INFO_CAPTURECOUNT, &value);

  return value;
}

/**
 * g_regex_get_has_cr_or_lf:
 * @regex: a #GRegex structure
 *
 * Checks whether the pattern contains explicit CR or LF references.
 *
 * Returns: %TRUE if the pattern contains explicit CR or LF references
 *
 * Since: 2.34
 */
gboolean
g_regex_get_has_cr_or_lf (const GRegex *regex)
{
  gint value;

  pcre_fullinfo (regex->pcre_re, regex->extra,
                 PCRE_INFO_HASCRORLF, &value);

  return !!value;
}

/**
 * g_regex_get_max_lookbehind:
 * @regex: a #GRegex structure
 *
 * Gets the number of characters in the longest lookbehind assertion in the
 * pattern. This information is useful when doing multi-segment matching using
 * the partial matching facilities.
 *
 * Returns: the number of characters in the longest lookbehind assertion.
 *
 * Since: 2.38
 */
gint
g_regex_get_max_lookbehind (const GRegex *regex)
{
  gint max_lookbehind;

  pcre_fullinfo (regex->pcre_re, regex->extra,
                 PCRE_INFO_MAXLOOKBEHIND, &max_lookbehind);

  return max_lookbehind;
}

/**
 * g_regex_get_compile_flags:
 * @regex: a #GRegex
 *
 * Returns the compile options that @regex was created with.
 *
 * Depending on the version of PCRE that is used, this may or may not
 * include flags set by option expressions such as `(?i)` found at the
 * top-level within the compiled pattern.
 *
 * Returns: flags from #GRegexCompileFlags
 *
 * Since: 2.26
 */
GRegexCompileFlags
g_regex_get_compile_flags (const GRegex *regex)
{
  g_return_val_if_fail (regex != NULL, 0);

  return regex->compile_opts;
}

/**
 * g_regex_get_match_flags:
 * @regex: a #GRegex
 *
 * Returns the match options that @regex was created with.
 *
 * Returns: flags from #GRegexMatchFlags
 *
 * Since: 2.26
 */
GRegexMatchFlags
g_regex_get_match_flags (const GRegex *regex)
{
  g_return_val_if_fail (regex != NULL, 0);

  return regex->match_opts & G_REGEX_MATCH_MASK;
}

/**
 * g_regex_match_simple:
 * @pattern: the regular expression
 * @string: the string to scan for matches
 * @compile_options: compile options for the regular expression, or 0
 * @match_options: match options, or 0
 *
 * Scans for a match in @string for @pattern.
 *
 * This function is equivalent to g_regex_match() but it does not
 * require to compile the pattern with g_regex_new(), avoiding some
 * lines of code when you need just to do a match without extracting
 * substrings, capture counts, and so on.
 *
 * If this function is to be called on the same @pattern more than
 * once, it's more efficient to compile the pattern once with
 * g_regex_new() and then use g_regex_match().
 *
 * Returns: %TRUE if the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_regex_match_simple (const gchar        *pattern,
                      const gchar        *string,
                      GRegexCompileFlags  compile_options,
                      GRegexMatchFlags    match_options)
{
  GRegex *regex;
  gboolean result;

  regex = g_regex_new (pattern, compile_options, 0, NULL);
  if (!regex)
    return FALSE;
  result = g_regex_match_full (regex, string, -1, 0, match_options, NULL, NULL);
  g_regex_unref (regex);
  return result;
}

/**
 * g_regex_match:
 * @regex: a #GRegex structure from g_regex_new()
 * @string: the string to scan for matches
 * @match_options: match options
 * @match_info: (out) (optional): pointer to location where to store
 *     the #GMatchInfo, or %NULL if you do not need it
 *
 * Scans for a match in @string for the pattern in @regex.
 * The @match_options are combined with the match options specified
 * when the @regex structure was created, letting you have more
 * flexibility in reusing #GRegex structures.
 *
 * Unless %G_REGEX_RAW is specified in the options, @string must be valid UTF-8.
 *
 * A #GMatchInfo structure, used to get information on the match,
 * is stored in @match_info if not %NULL. Note that if @match_info
 * is not %NULL then it is created even if the function returns %FALSE,
 * i.e. you must free it regardless if regular expression actually matched.
 *
 * To retrieve all the non-overlapping matches of the pattern in
 * string you can use g_match_info_next().
 *
 * |[<!-- language="C" -->
 * static void
 * print_uppercase_words (const gchar *string)
 * {
 *   // Print all uppercase-only words.
 *   GRegex *regex;
 *   GMatchInfo *match_info;
 *
 *   regex = g_regex_new ("[A-Z]+", 0, 0, NULL);
 *   g_regex_match (regex, string, 0, &match_info);
 *   while (g_match_info_matches (match_info))
 *     {
 *       gchar *word = g_match_info_fetch (match_info, 0);
 *       g_print ("Found: %s\n", word);
 *       g_free (word);
 *       g_match_info_next (match_info, NULL);
 *     }
 *   g_match_info_free (match_info);
 *   g_regex_unref (regex);
 * }
 * ]|
 *
 * @string is not copied and is used in #GMatchInfo internally. If
 * you use any #GMatchInfo method (except g_match_info_free()) after
 * freeing or modifying @string then the behaviour is undefined.
 *
 * Returns: %TRUE is the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_regex_match (const GRegex      *regex,
               const gchar       *string,
               GRegexMatchFlags   match_options,
               GMatchInfo       **match_info)
{
  return g_regex_match_full (regex, string, -1, 0, match_options,
                             match_info, NULL);
}

/**
 * g_regex_match_full:
 * @regex: a #GRegex structure from g_regex_new()
 * @string: (array length=string_len): the string to scan for matches
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @match_options: match options
 * @match_info: (out) (optional): pointer to location where to store
 *     the #GMatchInfo, or %NULL if you do not need it
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Scans for a match in @string for the pattern in @regex.
 * The @match_options are combined with the match options specified
 * when the @regex structure was created, letting you have more
 * flexibility in reusing #GRegex structures.
 *
 * Setting @start_position differs from just passing over a shortened
 * string and setting #G_REGEX_MATCH_NOTBOL in the case of a pattern
 * that begins with any kind of lookbehind assertion, such as "\b".
 *
 * Unless %G_REGEX_RAW is specified in the options, @string must be valid UTF-8.
 *
 * A #GMatchInfo structure, used to get information on the match, is
 * stored in @match_info if not %NULL. Note that if @match_info is
 * not %NULL then it is created even if the function returns %FALSE,
 * i.e. you must free it regardless if regular expression actually
 * matched.
 *
 * @string is not copied and is used in #GMatchInfo internally. If
 * you use any #GMatchInfo method (except g_match_info_free()) after
 * freeing or modifying @string then the behaviour is undefined.
 *
 * To retrieve all the non-overlapping matches of the pattern in
 * string you can use g_match_info_next().
 *
 * |[<!-- language="C" -->
 * static void
 * print_uppercase_words (const gchar *string)
 * {
 *   // Print all uppercase-only words.
 *   GRegex *regex;
 *   GMatchInfo *match_info;
 *   GError *error = NULL;
 *
 *   regex = g_regex_new ("[A-Z]+", 0, 0, NULL);
 *   g_regex_match_full (regex, string, -1, 0, 0, &match_info, &error);
 *   while (g_match_info_matches (match_info))
 *     {
 *       gchar *word = g_match_info_fetch (match_info, 0);
 *       g_print ("Found: %s\n", word);
 *       g_free (word);
 *       g_match_info_next (match_info, &error);
 *     }
 *   g_match_info_free (match_info);
 *   g_regex_unref (regex);
 *   if (error != NULL)
 *     {
 *       g_printerr ("Error while matching: %s\n", error->message);
 *       g_error_free (error);
 *     }
 * }
 * ]|
 *
 * Returns: %TRUE is the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_regex_match_full (const GRegex      *regex,
                    const gchar       *string,
                    gssize             string_len,
                    gint               start_position,
                    GRegexMatchFlags   match_options,
                    GMatchInfo       **match_info,
                    GError           **error)
{
  GMatchInfo *info;
  gboolean match_ok;

  g_return_val_if_fail (regex != NULL, FALSE);
  g_return_val_if_fail (string != NULL, FALSE);
  g_return_val_if_fail (start_position >= 0, FALSE);
  g_return_val_if_fail (error == NULL || *error == NULL, FALSE);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, FALSE);

  info = match_info_new (regex, string, string_len, start_position,
                         match_options, FALSE);
  match_ok = g_match_info_next (info, error);
  if (match_info != NULL)
    *match_info = info;
  else
    g_match_info_free (info);

  return match_ok;
}

/**
 * g_regex_match_all:
 * @regex: a #GRegex structure from g_regex_new()
 * @string: the string to scan for matches
 * @match_options: match options
 * @match_info: (out) (optional): pointer to location where to store
 *     the #GMatchInfo, or %NULL if you do not need it
 *
 * Using the standard algorithm for regular expression matching only
 * the longest match in the string is retrieved. This function uses
 * a different algorithm so it can retrieve all the possible matches.
 * For more documentation see g_regex_match_all_full().
 *
 * A #GMatchInfo structure, used to get information on the match, is
 * stored in @match_info if not %NULL. Note that if @match_info is
 * not %NULL then it is created even if the function returns %FALSE,
 * i.e. you must free it regardless if regular expression actually
 * matched.
 *
 * @string is not copied and is used in #GMatchInfo internally. If
 * you use any #GMatchInfo method (except g_match_info_free()) after
 * freeing or modifying @string then the behaviour is undefined.
 *
 * Returns: %TRUE is the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_regex_match_all (const GRegex      *regex,
                   const gchar       *string,
                   GRegexMatchFlags   match_options,
                   GMatchInfo       **match_info)
{
  return g_regex_match_all_full (regex, string, -1, 0, match_options,
                                 match_info, NULL);
}

/**
 * g_regex_match_all_full:
 * @regex: a #GRegex structure from g_regex_new()
 * @string: (array length=string_len): the string to scan for matches
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @match_options: match options
 * @match_info: (out) (optional): pointer to location where to store
 *     the #GMatchInfo, or %NULL if you do not need it
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Using the standard algorithm for regular expression matching only
 * the longest match in the @string is retrieved, it is not possible
 * to obtain all the available matches. For instance matching
 * "<a> <b> <c>" against the pattern "<.*>"
 * you get "<a> <b> <c>".
 *
 * This function uses a different algorithm (called DFA, i.e. deterministic
 * finite automaton), so it can retrieve all the possible matches, all
 * starting at the same point in the string. For instance matching
 * "<a> <b> <c>" against the pattern "<.*>;"
 * you would obtain three matches: "<a> <b> <c>",
 * "<a> <b>" and "<a>".
 *
 * The number of matched strings is retrieved using
 * g_match_info_get_match_count(). To obtain the matched strings and
 * their position you can use, respectively, g_match_info_fetch() and
 * g_match_info_fetch_pos(). Note that the strings are returned in
 * reverse order of length; that is, the longest matching string is
 * given first.
 *
 * Note that the DFA algorithm is slower than the standard one and it
 * is not able to capture substrings, so backreferences do not work.
 *
 * Setting @start_position differs from just passing over a shortened
 * string and setting #G_REGEX_MATCH_NOTBOL in the case of a pattern
 * that begins with any kind of lookbehind assertion, such as "\b".
 *
 * Unless %G_REGEX_RAW is specified in the options, @string must be valid UTF-8.
 *
 * A #GMatchInfo structure, used to get information on the match, is
 * stored in @match_info if not %NULL. Note that if @match_info is
 * not %NULL then it is created even if the function returns %FALSE,
 * i.e. you must free it regardless if regular expression actually
 * matched.
 *
 * @string is not copied and is used in #GMatchInfo internally. If
 * you use any #GMatchInfo method (except g_match_info_free()) after
 * freeing or modifying @string then the behaviour is undefined.
 *
 * Returns: %TRUE is the string matched, %FALSE otherwise
 *
 * Since: 2.14
 */
gboolean
g_regex_match_all_full (const GRegex      *regex,
                        const gchar       *string,
                        gssize             string_len,
                        gint               start_position,
                        GRegexMatchFlags   match_options,
                        GMatchInfo       **match_info,
                        GError           **error)
{
  GMatchInfo *info;
  gboolean done;
  pcre *pcre_re;
  pcre_extra *extra;
  gboolean retval;

  g_return_val_if_fail (regex != NULL, FALSE);
  g_return_val_if_fail (string != NULL, FALSE);
  g_return_val_if_fail (start_position >= 0, FALSE);
  g_return_val_if_fail (error == NULL || *error == NULL, FALSE);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, FALSE);

#ifdef PCRE_NO_AUTO_POSSESS
  /* For PCRE >= 8.34 we need to turn off PCRE_NO_AUTO_POSSESS, which
   * is an optimization for normal regex matching, but results in omitting
   * some shorter matches here, and an observable behaviour change.
   *
   * DFA matching is rather niche, and very rarely used according to
   * codesearch.debian.net, so don't bother caching the recompiled RE. */
  pcre_re = regex_compile (regex->pattern,
                           regex->compile_opts | PCRE_NO_AUTO_POSSESS,
                           NULL, NULL, error);

  if (pcre_re == NULL)
    return FALSE;

  /* Not bothering to cache the optimization data either, with similar
   * reasoning */
  extra = NULL;
#else
  /* For PCRE < 8.33 the precompiled regex is fine. */
  pcre_re = regex->pcre_re;
  extra = regex->extra;
#endif

  info = match_info_new (regex, string, string_len, start_position,
                         match_options, TRUE);

  done = FALSE;
  while (!done)
    {
      done = TRUE;
      info->matches = pcre_dfa_exec (pcre_re, extra,
                                     info->string, info->string_len,
                                     info->pos,
                                     regex->match_opts | match_options,
                                     info->offsets, info->n_offsets,
                                     info->workspace, info->n_workspace);
      if (info->matches == PCRE_ERROR_DFA_WSSIZE)
        {
          /* info->workspace is too small. */
          info->n_workspace *= 2;
          info->workspace = g_realloc (info->workspace,
                                       info->n_workspace * sizeof (gint));
          done = FALSE;
        }
      else if (info->matches == 0)
        {
          /* info->offsets is too small. */
          info->n_offsets *= 2;
          info->offsets = g_realloc (info->offsets,
                                     info->n_offsets * sizeof (gint));
          done = FALSE;
        }
      else if (IS_PCRE_ERROR (info->matches))
        {
          g_set_error (error, G_REGEX_ERROR, G_REGEX_ERROR_MATCH,
                       _("Error while matching regular expression %s: %s"),
                       regex->pattern, match_error (info->matches));
        }
    }

#ifdef PCRE_NO_AUTO_POSSESS
  pcre_free (pcre_re);
#endif

  /* set info->pos to -1 so that a call to g_match_info_next() fails. */
  info->pos = -1;
  retval = info->matches >= 0;

  if (match_info != NULL)
    *match_info = info;
  else
    g_match_info_free (info);

  return retval;
}

/**
 * g_regex_get_string_number:
 * @regex: #GRegex structure
 * @name: name of the subexpression
 *
 * Retrieves the number of the subexpression named @name.
 *
 * Returns: The number of the subexpression or -1 if @name
 *   does not exists
 *
 * Since: 2.14
 */
gint
g_regex_get_string_number (const GRegex *regex,
                           const gchar  *name)
{
  gint num;

  g_return_val_if_fail (regex != NULL, -1);
  g_return_val_if_fail (name != NULL, -1);

  num = pcre_get_stringnumber (regex->pcre_re, name);
  if (num == PCRE_ERROR_NOSUBSTRING)
    num = -1;

  return num;
}

/**
 * g_regex_split_simple:
 * @pattern: the regular expression
 * @string: the string to scan for matches
 * @compile_options: compile options for the regular expression, or 0
 * @match_options: match options, or 0
 *
 * Breaks the string on the pattern, and returns an array of
 * the tokens. If the pattern contains capturing parentheses,
 * then the text for each of the substrings will also be returned.
 * If the pattern does not match anywhere in the string, then the
 * whole string is returned as the first token.
 *
 * This function is equivalent to g_regex_split() but it does
 * not require to compile the pattern with g_regex_new(), avoiding
 * some lines of code when you need just to do a split without
 * extracting substrings, capture counts, and so on.
 *
 * If this function is to be called on the same @pattern more than
 * once, it's more efficient to compile the pattern once with
 * g_regex_new() and then use g_regex_split().
 *
 * As a special case, the result of splitting the empty string ""
 * is an empty vector, not a vector containing a single string.
 * The reason for this special case is that being able to represent
 * an empty vector is typically more useful than consistent handling
 * of empty elements. If you do need to represent empty elements,
 * you'll need to check for the empty string before calling this
 * function.
 *
 * A pattern that can match empty strings splits @string into
 * separate characters wherever it matches the empty string between
 * characters. For example splitting "ab c" using as a separator
 * "\s*", you will get "a", "b" and "c".
 *
 * Returns: (transfer full): a %NULL-terminated array of strings. Free
 * it using g_strfreev()
 *
 * Since: 2.14
 **/
gchar **
g_regex_split_simple (const gchar        *pattern,
                      const gchar        *string,
                      GRegexCompileFlags  compile_options,
                      GRegexMatchFlags    match_options)
{
  GRegex *regex;
  gchar **result;

  regex = g_regex_new (pattern, compile_options, 0, NULL);
  if (!regex)
    return NULL;

  result = g_regex_split_full (regex, string, -1, 0, match_options, 0, NULL);
  g_regex_unref (regex);
  return result;
}

/**
 * g_regex_split:
 * @regex: a #GRegex structure
 * @string: the string to split with the pattern
 * @match_options: match time option flags
 *
 * Breaks the string on the pattern, and returns an array of the tokens.
 * If the pattern contains capturing parentheses, then the text for each
 * of the substrings will also be returned. If the pattern does not match
 * anywhere in the string, then the whole string is returned as the first
 * token.
 *
 * As a special case, the result of splitting the empty string "" is an
 * empty vector, not a vector containing a single string. The reason for
 * this special case is that being able to represent an empty vector is
 * typically more useful than consistent handling of empty elements. If
 * you do need to represent empty elements, you'll need to check for the
 * empty string before calling this function.
 *
 * A pattern that can match empty strings splits @string into separate
 * characters wherever it matches the empty string between characters.
 * For example splitting "ab c" using as a separator "\s*", you will get
 * "a", "b" and "c".
 *
 * Returns: (transfer full): a %NULL-terminated gchar ** array. Free
 * it using g_strfreev()
 *
 * Since: 2.14
 **/
gchar **
g_regex_split (const GRegex     *regex,
               const gchar      *string,
               GRegexMatchFlags  match_options)
{
  return g_regex_split_full (regex, string, -1, 0,
                             match_options, 0, NULL);
}

/**
 * g_regex_split_full:
 * @regex: a #GRegex structure
 * @string: (array length=string_len): the string to split with the pattern
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @match_options: match time option flags
 * @max_tokens: the maximum number of tokens to split @string into.
 *   If this is less than 1, the string is split completely
 * @error: return location for a #GError
 *
 * Breaks the string on the pattern, and returns an array of the tokens.
 * If the pattern contains capturing parentheses, then the text for each
 * of the substrings will also be returned. If the pattern does not match
 * anywhere in the string, then the whole string is returned as the first
 * token.
 *
 * As a special case, the result of splitting the empty string "" is an
 * empty vector, not a vector containing a single string. The reason for
 * this special case is that being able to represent an empty vector is
 * typically more useful than consistent handling of empty elements. If
 * you do need to represent empty elements, you'll need to check for the
 * empty string before calling this function.
 *
 * A pattern that can match empty strings splits @string into separate
 * characters wherever it matches the empty string between characters.
 * For example splitting "ab c" using as a separator "\s*", you will get
 * "a", "b" and "c".
 *
 * Setting @start_position differs from just passing over a shortened
 * string and setting #G_REGEX_MATCH_NOTBOL in the case of a pattern
 * that begins with any kind of lookbehind assertion, such as "\b".
 *
 * Returns: (transfer full): a %NULL-terminated gchar ** array. Free
 * it using g_strfreev()
 *
 * Since: 2.14
 **/
gchar **
g_regex_split_full (const GRegex      *regex,
                    const gchar       *string,
                    gssize             string_len,
                    gint               start_position,
                    GRegexMatchFlags   match_options,
                    gint               max_tokens,
                    GError           **error)
{
  GError *tmp_error = NULL;
  GMatchInfo *match_info;
  GList *list, *last;
  gint i;
  gint token_count;
  gboolean match_ok;
  /* position of the last separator. */
  gint last_separator_end;
  /* was the last match 0 bytes long? */
  gboolean last_match_is_empty;
  /* the returned array of char **s */
  gchar **string_list;

  g_return_val_if_fail (regex != NULL, NULL);
  g_return_val_if_fail (string != NULL, NULL);
  g_return_val_if_fail (start_position >= 0, NULL);
  g_return_val_if_fail (error == NULL || *error == NULL, NULL);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, NULL);

  if (max_tokens <= 0)
    max_tokens = G_MAXINT;

  if (string_len < 0)
    string_len = strlen (string);

  /* zero-length string */
  if (string_len - start_position == 0)
    return g_new0 (gchar *, 1);

  if (max_tokens == 1)
    {
      string_list = g_new0 (gchar *, 2);
      string_list[0] = g_strndup (&string[start_position],
                                  string_len - start_position);
      return string_list;
    }

  list = NULL;
  token_count = 0;
  last_separator_end = start_position;
  last_match_is_empty = FALSE;

  match_ok = g_regex_match_full (regex, string, string_len, start_position,
                                 match_options, &match_info, &tmp_error);

  while (tmp_error == NULL)
    {
      if (match_ok)
        {
          last_match_is_empty =
                    (match_info->offsets[0] == match_info->offsets[1]);

          /* we need to skip empty separators at the same position of the end
           * of another separator. e.g. the string is "a b" and the separator
           * is " *", so from 1 to 2 we have a match and at position 2 we have
           * an empty match. */
          if (last_separator_end != match_info->offsets[1])
            {
              gchar *token;
              gint match_count;

              token = g_strndup (string + last_separator_end,
                                 match_info->offsets[0] - last_separator_end);
              list = g_list_prepend (list, token);
              token_count++;

              /* if there were substrings, these need to be added to
               * the list. */
              match_count = g_match_info_get_match_count (match_info);
              if (match_count > 1)
                {
                  for (i = 1; i < match_count; i++)
                    list = g_list_prepend (list, g_match_info_fetch (match_info, i));
                }
            }
        }
      else
        {
          /* if there was no match, copy to end of string. */
          if (!last_match_is_empty)
            {
              gchar *token = g_strndup (string + last_separator_end,
                                        match_info->string_len - last_separator_end);
              list = g_list_prepend (list, token);
            }
          /* no more tokens, end the loop. */
          break;
        }

      /* -1 to leave room for the last part. */
      if (token_count >= max_tokens - 1)
        {
          /* we have reached the maximum number of tokens, so we copy
           * the remaining part of the string. */
          if (last_match_is_empty)
            {
              /* the last match was empty, so we have moved one char
               * after the real position to avoid empty matches at the
               * same position. */
              match_info->pos = PREV_CHAR (regex, &string[match_info->pos]) - string;
            }
          /* the if is needed in the case we have terminated the available
           * tokens, but we are at the end of the string, so there are no
           * characters left to copy. */
          if (string_len > match_info->pos)
            {
              gchar *token = g_strndup (string + match_info->pos,
                                        string_len - match_info->pos);
              list = g_list_prepend (list, token);
            }
          /* end the loop. */
          break;
        }

      last_separator_end = match_info->pos;
      if (last_match_is_empty)
        /* if the last match was empty, g_match_info_next() has moved
         * forward to avoid infinite loops, but we still need to copy that
         * character. */
        last_separator_end = PREV_CHAR (regex, &string[last_separator_end]) - string;

      match_ok = g_match_info_next (match_info, &tmp_error);
    }
  g_match_info_free (match_info);
  if (tmp_error != NULL)
    {
      g_propagate_error (error, tmp_error);
      g_list_free_full (list, g_free);
      return NULL;
    }

  string_list = g_new (gchar *, g_list_length (list) + 1);
  i = 0;
  for (last = g_list_last (list); last; last = g_list_previous (last))
    string_list[i++] = last->data;
  string_list[i] = NULL;
  g_list_free (list);

  return string_list;
}

enum
{
  REPL_TYPE_STRING,
  REPL_TYPE_CHARACTER,
  REPL_TYPE_SYMBOLIC_REFERENCE,
  REPL_TYPE_NUMERIC_REFERENCE,
  REPL_TYPE_CHANGE_CASE
};

typedef enum
{
  CHANGE_CASE_NONE         = 1 << 0,
  CHANGE_CASE_UPPER        = 1 << 1,
  CHANGE_CASE_LOWER        = 1 << 2,
  CHANGE_CASE_UPPER_SINGLE = 1 << 3,
  CHANGE_CASE_LOWER_SINGLE = 1 << 4,
  CHANGE_CASE_SINGLE_MASK  = CHANGE_CASE_UPPER_SINGLE | CHANGE_CASE_LOWER_SINGLE,
  CHANGE_CASE_LOWER_MASK   = CHANGE_CASE_LOWER | CHANGE_CASE_LOWER_SINGLE,
  CHANGE_CASE_UPPER_MASK   = CHANGE_CASE_UPPER | CHANGE_CASE_UPPER_SINGLE
} ChangeCase;

struct _InterpolationData
{
  gchar     *text;
  gint       type;
  gint       num;
  gchar      c;
  ChangeCase change_case;
};

static void
free_interpolation_data (InterpolationData *data)
{
  g_free (data->text);
  g_free (data);
}

static const gchar *
expand_escape (const gchar        *replacement,
               const gchar        *p,
               InterpolationData  *data,
               GError            **error)
{
  const gchar *q, *r;
  gint x, d, h, i;
  const gchar *error_detail;
  gint base = 0;
  GError *tmp_error = NULL;

  p++;
  switch (*p)
    {
    case 't':
      p++;
      data->c = '\t';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'n':
      p++;
      data->c = '\n';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'v':
      p++;
      data->c = '\v';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'r':
      p++;
      data->c = '\r';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'f':
      p++;
      data->c = '\f';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'a':
      p++;
      data->c = '\a';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'b':
      p++;
      data->c = '\b';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case '\\':
      p++;
      data->c = '\\';
      data->type = REPL_TYPE_CHARACTER;
      break;
    case 'x':
      p++;
      x = 0;
      if (*p == '{')
        {
          p++;
          do
            {
              h = g_ascii_xdigit_value (*p);
              if (h < 0)
                {
                  error_detail = _("hexadecimal digit or '}' expected");
                  goto error;
                }
              x = x * 16 + h;
              p++;
            }
          while (*p != '}');
          p++;
        }
      else
        {
          for (i = 0; i < 2; i++)
            {
              h = g_ascii_xdigit_value (*p);
              if (h < 0)
                {
                  error_detail = _("hexadecimal digit expected");
                  goto error;
                }
              x = x * 16 + h;
              p++;
            }
        }
      data->type = REPL_TYPE_STRING;
      data->text = g_new0 (gchar, 8);
      g_unichar_to_utf8 (x, data->text);
      break;
    case 'l':
      p++;
      data->type = REPL_TYPE_CHANGE_CASE;
      data->change_case = CHANGE_CASE_LOWER_SINGLE;
      break;
    case 'u':
      p++;
      data->type = REPL_TYPE_CHANGE_CASE;
      data->change_case = CHANGE_CASE_UPPER_SINGLE;
      break;
    case 'L':
      p++;
      data->type = REPL_TYPE_CHANGE_CASE;
      data->change_case = CHANGE_CASE_LOWER;
      break;
    case 'U':
      p++;
      data->type = REPL_TYPE_CHANGE_CASE;
      data->change_case = CHANGE_CASE_UPPER;
      break;
    case 'E':
      p++;
      data->type = REPL_TYPE_CHANGE_CASE;
      data->change_case = CHANGE_CASE_NONE;
      break;
    case 'g':
      p++;
      if (*p != '<')
        {
          error_detail = _("missing '<' in symbolic reference");
          goto error;
        }
      q = p + 1;
      do
        {
          p++;
          if (!*p)
            {
              error_detail = _("unfinished symbolic reference");
              goto error;
            }
        }
      while (*p != '>');
      if (p - q == 0)
        {
          error_detail = _("zero-length symbolic reference");
          goto error;
        }
      if (g_ascii_isdigit (*q))
        {
          x = 0;
          do
            {
              h = g_ascii_digit_value (*q);
              if (h < 0)
                {
                  error_detail = _("digit expected");
                  p = q;
                  goto error;
                }
              x = x * 10 + h;
              q++;
            }
          while (q != p);
          data->num = x;
          data->type = REPL_TYPE_NUMERIC_REFERENCE;
        }
      else
        {
          r = q;
          do
            {
              if (!g_ascii_isalnum (*r))
                {
                  error_detail = _("illegal symbolic reference");
                  p = r;
                  goto error;
                }
              r++;
            }
          while (r != p);
          data->text = g_strndup (q, p - q);
          data->type = REPL_TYPE_SYMBOLIC_REFERENCE;
        }
      p++;
      break;
    case '0':
      /* if \0 is followed by a number is an octal number representing a
       * character, else it is a numeric reference. */
      if (g_ascii_digit_value (*g_utf8_next_char (p)) >= 0)
        {
          base = 8;
          p = g_utf8_next_char (p);
        }
      G_GNUC_FALLTHROUGH;
    case '1':
    case '2':
    case '3':
    case '4':
    case '5':
    case '6':
    case '7':
    case '8':
    case '9':
      x = 0;
      d = 0;
      for (i = 0; i < 3; i++)
        {
          h = g_ascii_digit_value (*p);
          if (h < 0)
            break;
          if (h > 7)
            {
              if (base == 8)
                break;
              else
                base = 10;
            }
          if (i == 2 && base == 10)
            break;
          x = x * 8 + h;
          d = d * 10 + h;
          p++;
        }
      if (base == 8 || i == 3)
        {
          data->type = REPL_TYPE_STRING;
          data->text = g_new0 (gchar, 8);
          g_unichar_to_utf8 (x, data->text);
        }
      else
        {
          data->type = REPL_TYPE_NUMERIC_REFERENCE;
          data->num = d;
        }
      break;
    case 0:
      error_detail = _("stray final '\\'");
      goto error;
      break;
    default:
      error_detail = _("unknown escape sequence");
      goto error;
    }

  return p;

 error:
  /* G_GSSIZE_FORMAT doesn't work with gettext, so we use %lu */
  tmp_error = g_error_new (G_REGEX_ERROR,
                           G_REGEX_ERROR_REPLACE,
                           _("Error while parsing replacement "
                             "text '%s' at char %lu: %s"),
                           replacement,
                           (gulong)(p - replacement),
                           error_detail);
  g_propagate_error (error, tmp_error);

  return NULL;
}

static GList *
split_replacement (const gchar  *replacement,
                   GError      **error)
{
  GList *list = NULL;
  InterpolationData *data;
  const gchar *p, *start;

  start = p = replacement;
  while (*p)
    {
      if (*p == '\\')
        {
          data = g_new0 (InterpolationData, 1);
          start = p = expand_escape (replacement, p, data, error);
          if (p == NULL)
            {
              g_list_free_full (list, (GDestroyNotify) free_interpolation_data);
              free_interpolation_data (data);

              return NULL;
            }
          list = g_list_prepend (list, data);
        }
      else
        {
          p++;
          if (*p == '\\' || *p == '\0')
            {
              if (p - start > 0)
                {
                  data = g_new0 (InterpolationData, 1);
                  data->text = g_strndup (start, p - start);
                  data->type = REPL_TYPE_STRING;
                  list = g_list_prepend (list, data);
                }
            }
        }
    }

  return g_list_reverse (list);
}

/* Change the case of c based on change_case. */
#define CHANGE_CASE(c, change_case) \
        (((change_case) & CHANGE_CASE_LOWER_MASK) ? \
                g_unichar_tolower (c) : \
                g_unichar_toupper (c))

static void
string_append (GString     *string,
               const gchar *text,
               ChangeCase  *change_case)
{
  gunichar c;

  if (text[0] == '\0')
    return;

  if (*change_case == CHANGE_CASE_NONE)
    {
      g_string_append (string, text);
    }
  else if (*change_case & CHANGE_CASE_SINGLE_MASK)
    {
      c = g_utf8_get_char (text);
      g_string_append_unichar (string, CHANGE_CASE (c, *change_case));
      g_string_append (string, g_utf8_next_char (text));
      *change_case = CHANGE_CASE_NONE;
    }
  else
    {
      while (*text != '\0')
        {
          c = g_utf8_get_char (text);
          g_string_append_unichar (string, CHANGE_CASE (c, *change_case));
          text = g_utf8_next_char (text);
        }
    }
}

static gboolean
interpolate_replacement (const GMatchInfo *match_info,
                         GString          *result,
                         gpointer          data)
{
  GList *list;
  InterpolationData *idata;
  gchar *match;
  ChangeCase change_case = CHANGE_CASE_NONE;

  for (list = data; list; list = list->next)
    {
      idata = list->data;
      switch (idata->type)
        {
        case REPL_TYPE_STRING:
          string_append (result, idata->text, &change_case);
          break;
        case REPL_TYPE_CHARACTER:
          g_string_append_c (result, CHANGE_CASE (idata->c, change_case));
          if (change_case & CHANGE_CASE_SINGLE_MASK)
            change_case = CHANGE_CASE_NONE;
          break;
        case REPL_TYPE_NUMERIC_REFERENCE:
          match = g_match_info_fetch (match_info, idata->num);
          if (match)
            {
              string_append (result, match, &change_case);
              g_free (match);
            }
          break;
        case REPL_TYPE_SYMBOLIC_REFERENCE:
          match = g_match_info_fetch_named (match_info, idata->text);
          if (match)
            {
              string_append (result, match, &change_case);
              g_free (match);
            }
          break;
        case REPL_TYPE_CHANGE_CASE:
          change_case = idata->change_case;
          break;
        }
    }

  return FALSE;
}

/* whether actual match_info is needed for replacement, i.e.
 * whether there are references
 */
static gboolean
interpolation_list_needs_match (GList *list)
{
  while (list != NULL)
    {
      InterpolationData *data = list->data;

      if (data->type == REPL_TYPE_SYMBOLIC_REFERENCE ||
          data->type == REPL_TYPE_NUMERIC_REFERENCE)
        {
          return TRUE;
        }

      list = list->next;
    }

  return FALSE;
}

/**
 * g_regex_replace:
 * @regex: a #GRegex structure
 * @string: (array length=string_len): the string to perform matches against
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @replacement: text to replace each match with
 * @match_options: options for the match
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Replaces all occurrences of the pattern in @regex with the
 * replacement text. Backreferences of the form '\number' or
 * '\g<number>' in the replacement text are interpolated by the
 * number-th captured subexpression of the match, '\g<name>' refers
 * to the captured subexpression with the given name. '\0' refers
 * to the complete match, but '\0' followed by a number is the octal
 * representation of a character. To include a literal '\' in the
 * replacement, write '\\\\'.
 *
 * There are also escapes that changes the case of the following text:
 *
 * - \l: Convert to lower case the next character
 * - \u: Convert to upper case the next character
 * - \L: Convert to lower case till \E
 * - \U: Convert to upper case till \E
 * - \E: End case modification
 *
 * If you do not need to use backreferences use g_regex_replace_literal().
 *
 * The @replacement string must be UTF-8 encoded even if #G_REGEX_RAW was
 * passed to g_regex_new(). If you want to use not UTF-8 encoded stings
 * you can use g_regex_replace_literal().
 *
 * Setting @start_position differs from just passing over a shortened
 * string and setting #G_REGEX_MATCH_NOTBOL in the case of a pattern that
 * begins with any kind of lookbehind assertion, such as "\b".
 *
 * Returns: a newly allocated string containing the replacements
 *
 * Since: 2.14
 */
gchar *
g_regex_replace (const GRegex      *regex,
                 const gchar       *string,
                 gssize             string_len,
                 gint               start_position,
                 const gchar       *replacement,
                 GRegexMatchFlags   match_options,
                 GError           **error)
{
  gchar *result;
  GList *list;
  GError *tmp_error = NULL;

  g_return_val_if_fail (regex != NULL, NULL);
  g_return_val_if_fail (string != NULL, NULL);
  g_return_val_if_fail (start_position >= 0, NULL);
  g_return_val_if_fail (replacement != NULL, NULL);
  g_return_val_if_fail (error == NULL || *error == NULL, NULL);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, NULL);

  list = split_replacement (replacement, &tmp_error);
  if (tmp_error != NULL)
    {
      g_propagate_error (error, tmp_error);
      return NULL;
    }

  result = g_regex_replace_eval (regex,
                                 string, string_len, start_position,
                                 match_options,
                                 interpolate_replacement,
                                 (gpointer)list,
                                 &tmp_error);
  if (tmp_error != NULL)
    g_propagate_error (error, tmp_error);

  g_list_free_full (list, (GDestroyNotify) free_interpolation_data);

  return result;
}

static gboolean
literal_replacement (const GMatchInfo *match_info,
                     GString          *result,
                     gpointer          data)
{
  g_string_append (result, data);
  return FALSE;
}

/**
 * g_regex_replace_literal:
 * @regex: a #GRegex structure
 * @string: (array length=string_len): the string to perform matches against
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @replacement: text to replace each match with
 * @match_options: options for the match
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Replaces all occurrences of the pattern in @regex with the
 * replacement text. @replacement is replaced literally, to
 * include backreferences use g_regex_replace().
 *
 * Setting @start_position differs from just passing over a
 * shortened string and setting #G_REGEX_MATCH_NOTBOL in the
 * case of a pattern that begins with any kind of lookbehind
 * assertion, such as "\b".
 *
 * Returns: a newly allocated string containing the replacements
 *
 * Since: 2.14
 */
gchar *
g_regex_replace_literal (const GRegex      *regex,
                         const gchar       *string,
                         gssize             string_len,
                         gint               start_position,
                         const gchar       *replacement,
                         GRegexMatchFlags   match_options,
                         GError           **error)
{
  g_return_val_if_fail (replacement != NULL, NULL);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, NULL);

  return g_regex_replace_eval (regex,
                               string, string_len, start_position,
                               match_options,
                               literal_replacement,
                               (gpointer)replacement,
                               error);
}

/**
 * g_regex_replace_eval:
 * @regex: a #GRegex structure from g_regex_new()
 * @string: (array length=string_len): string to perform matches against
 * @string_len: the length of @string, in bytes, or -1 if @string is nul-terminated
 * @start_position: starting index of the string to match, in bytes
 * @match_options: options for the match
 * @eval: a function to call for each match
 * @user_data: user data to pass to the function
 * @error: location to store the error occurring, or %NULL to ignore errors
 *
 * Replaces occurrences of the pattern in regex with the output of
 * @eval for that occurrence.
 *
 * Setting @start_position differs from just passing over a shortened
 * string and setting #G_REGEX_MATCH_NOTBOL in the case of a pattern
 * that begins with any kind of lookbehind assertion, such as "\b".
 *
 * The following example uses g_regex_replace_eval() to replace multiple
 * strings at once:
 * |[<!-- language="C" -->
 * static gboolean
 * eval_cb (const GMatchInfo *info,
 *          GString          *res,
 *          gpointer          data)
 * {
 *   gchar *match;
 *   gchar *r;
 *
 *    match = g_match_info_fetch (info, 0);
 *    r = g_hash_table_lookup ((GHashTable *)data, match);
 *    g_string_append (res, r);
 *    g_free (match);
 *
 *    return FALSE;
 * }
 *
 * ...
 *
 * GRegex *reg;
 * GHashTable *h;
 * gchar *res;
 *
 * h = g_hash_table_new (g_str_hash, g_str_equal);
 *
 * g_hash_table_insert (h, "1", "ONE");
 * g_hash_table_insert (h, "2", "TWO");
 * g_hash_table_insert (h, "3", "THREE");
 * g_hash_table_insert (h, "4", "FOUR");
 *
 * reg = g_regex_new ("1|2|3|4", 0, 0, NULL);
 * res = g_regex_replace_eval (reg, text, -1, 0, 0, eval_cb, h, NULL);
 * g_hash_table_destroy (h);
 *
 * ...
 * ]|
 *
 * Returns: a newly allocated string containing the replacements
 *
 * Since: 2.14
 */
gchar *
g_regex_replace_eval (const GRegex        *regex,
                      const gchar         *string,
                      gssize               string_len,
                      gint                 start_position,
                      GRegexMatchFlags     match_options,
                      GRegexEvalCallback   eval,
                      gpointer             user_data,
                      GError             **error)
{
  GMatchInfo *match_info;
  GString *result;
  gint str_pos = 0;
  gboolean done = FALSE;
  GError *tmp_error = NULL;

  g_return_val_if_fail (regex != NULL, NULL);
  g_return_val_if_fail (string != NULL, NULL);
  g_return_val_if_fail (start_position >= 0, NULL);
  g_return_val_if_fail (eval != NULL, NULL);
  g_return_val_if_fail ((match_options & ~G_REGEX_MATCH_MASK) == 0, NULL);

  if (string_len < 0)
    string_len = strlen (string);

  result = g_string_sized_new (string_len);

  /* run down the string making matches. */
  g_regex_match_full (regex, string, string_len, start_position,
                      match_options, &match_info, &tmp_error);
  while (!done && g_match_info_matches (match_info))
    {
      g_string_append_len (result,
                           string + str_pos,
                           match_info->offsets[0] - str_pos);
      done = (*eval) (match_info, result, user_data);
      str_pos = match_info->offsets[1];
      g_match_info_next (match_info, &tmp_error);
    }
  g_match_info_free (match_info);
  if (tmp_error != NULL)
    {
      g_propagate_error (error, tmp_error);
      g_string_free (result, TRUE);
      return NULL;
    }

  g_string_append_len (result, string + str_pos, string_len - str_pos);
  return g_string_free (result, FALSE);
}

/**
 * g_regex_check_replacement:
 * @replacement: the replacement string
 * @has_references: (out) (optional): location to store information about
 *   references in @replacement or %NULL
 * @error: location to store error
 *
 * Checks whether @replacement is a valid replacement string
 * (see g_regex_replace()), i.e. that all escape sequences in
 * it are valid.
 *
 * If @has_references is not %NULL then @replacement is checked
 * for pattern references. For instance, replacement text 'foo\n'
 * does not contain references and may be evaluated without information
 * about actual match, but '\0\1' (whole match followed by first
 * subpattern) requires valid #GMatchInfo object.
 *
 * Returns: whether @replacement is a valid replacement string
 *
 * Since: 2.14
 */
gboolean
g_regex_check_replacement (const gchar  *replacement,
                           gboolean     *has_references,
                           GError      **error)
{
  GList *list;
  GError *tmp = NULL;

  list = split_replacement (replacement, &tmp);

  if (tmp)
  {
    g_propagate_error (error, tmp);
    return FALSE;
  }

  if (has_references)
    *has_references = interpolation_list_needs_match (list);

  g_list_free_full (list, (GDestroyNotify) free_interpolation_data);

  return TRUE;
}

/**
 * g_regex_escape_nul:
 * @string: the string to escape
 * @length: the length of @string
 *
 * Escapes the nul characters in @string to "\x00".  It can be used
 * to compile a regex with embedded nul characters.
 *
 * For completeness, @length can be -1 for a nul-terminated string.
 * In this case the output string will be of course equal to @string.
 *
 * Returns: a newly-allocated escaped string
 *
 * Since: 2.30
 */
gchar *
g_regex_escape_nul (const gchar *string,
                    gint         length)
{
  GString *escaped;
  const gchar *p, *piece_start, *end;
  gint backslashes;

  g_return_val_if_fail (string != NULL, NULL);

  if (length < 0)
    return g_strdup (string);

  end = string + length;
  p = piece_start = string;
  escaped = g_string_sized_new (length + 1);

  backslashes = 0;
  while (p < end)
    {
      switch (*p)
        {
        case '\0':
          if (p != piece_start)
            {
              /* copy the previous piece. */
              g_string_append_len (escaped, piece_start, p - piece_start);
            }
          if ((backslashes & 1) == 0)
            g_string_append_c (escaped, '\\');
          g_string_append_c (escaped, 'x');
          g_string_append_c (escaped, '0');
          g_string_append_c (escaped, '0');
          piece_start = ++p;
          backslashes = 0;
          break;
        case '\\':
          backslashes++;
          ++p;
          break;
        default:
          backslashes = 0;
          p = g_utf8_next_char (p);
          break;
        }
    }

  if (piece_start < end)
    g_string_append_len (escaped, piece_start, end - piece_start);

  return g_string_free (escaped, FALSE);
}

/**
 * g_regex_escape_string:
 * @string: (array length=length): the string to escape
 * @length: the length of @string, in bytes, or -1 if @string is nul-terminated
 *
 * Escapes the special characters used for regular expressions
 * in @string, for instance "a.b*c" becomes "a\.b\*c". This
 * function is useful to dynamically generate regular expressions.
 *
 * @string can contain nul characters that are replaced with "\0",
 * in this case remember to specify the correct length of @string
 * in @length.
 *
 * Returns: a newly-allocated escaped string
 *
 * Since: 2.14
 */
gchar *
g_regex_escape_string (const gchar *string,
                       gint         length)
{
  GString *escaped;
  const char *p, *piece_start, *end;

  g_return_val_if_fail (string != NULL, NULL);

  if (length < 0)
    length = strlen (string);

  end = string + length;
  p = piece_start = string;
  escaped = g_string_sized_new (length + 1);

  while (p < end)
    {
      switch (*p)
        {
        case '\0':
        case '\\':
        case '|':
        case '(':
        case ')':
        case '[':
        case ']':
        case '{':
        case '}':
        case '^':
        case '$':
        case '*':
        case '+':
        case '?':
        case '.':
          if (p != piece_start)
            /* copy the previous piece. */
            g_string_append_len (escaped, piece_start, p - piece_start);
          g_string_append_c (escaped, '\\');
          if (*p == '\0')
            g_string_append_c (escaped, '0');
          else
            g_string_append_c (escaped, *p);
          piece_start = ++p;
          break;
        default:
          p = g_utf8_next_char (p);
          break;
        }
  }

  if (piece_start < end)
    g_string_append_len (escaped, piece_start, end - piece_start);

  return g_string_free (escaped, FALSE);
}
