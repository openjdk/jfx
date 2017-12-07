/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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
