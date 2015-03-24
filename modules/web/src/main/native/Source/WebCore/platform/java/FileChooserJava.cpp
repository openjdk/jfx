/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
 */
#include "config.h"

#include "FileChooser.h"

#include "FileSystem.h"
#include "Icon.h"
#include "LocalizedStrings.h"
#include "StringTruncator.h"

#include "NotImplemented.h"

namespace WebCore {

  /*
String FileChooser::basenameForWidth(const Font& font, int width) const
{
/*
    if (m_filenames.size() == 0) {
        return fileButtonNoFileSelectedLabel();
    }

    // find basename
    String filename = m_filenames[0];
    int i = std::max(filename.reverseFind('/'), filename.reverseFind('\\'));
    String basename = (i == -1) ? filename : filename.substring(i + 1);
    // check if it fits into width
    if (font.floatWidth(basename) <= width) {
        return basename;
    } else {
        // replace middle part with "..."
        int len = basename.length();
        i = len / 2 - 1;
        while ((i > 0) && (i < len / 2)) {
            String toCheck = basename.substring(0, i) + "..." + basename.substring(len - i);
            if (font.floatWidth(toCheck) <= width) {
                return toCheck;
            }
            i--;
        }
    }

    // if width is too small, just return an empty string
    return "";
*
    if (width <= 0)
        return String();

    String string;
    if (m_filenames.isEmpty())
        string = fileButtonNoFileSelectedLabel();
    else if (m_filenames.size() == 1)
        string = pathGetFileName(m_filenames[0]);
    else
        return StringTruncator::rightTruncate(String::number(m_filenames.size()) + " files", width, font);

    return StringTruncator::centerTruncate(string, width, font);
}
*/

}
