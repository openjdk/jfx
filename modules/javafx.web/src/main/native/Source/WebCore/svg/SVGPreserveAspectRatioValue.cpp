/*
 * Copyright (C) 2004, 2005, 2008 Nikolas Zimmermann <zimmermann@kde.org>
 * Copyright (C) 2004, 2005, 2006, 2007 Rob Buis <buis@kde.org>
 * Copyright (C) 2010 Dirk Schulze <krit@webkit.org>
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
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

#include "config.h"
#include "SVGPreserveAspectRatioValue.h"

#include "AffineTransform.h"
#include "FloatRect.h"
#include "SVGParserUtilities.h"
#include <wtf/text/StringConcatenateNumbers.h>
#include <wtf/text/StringView.h>

namespace WebCore {

SVGPreserveAspectRatioValue::SVGPreserveAspectRatioValue()
    : m_align(SVG_PRESERVEASPECTRATIO_XMIDYMID)
    , m_meetOrSlice(SVG_MEETORSLICE_MEET)
{
}

SVGPreserveAspectRatioValue::SVGPreserveAspectRatioValue(const String& value)
{
    parse(value);
}

ExceptionOr<void> SVGPreserveAspectRatioValue::setAlign(unsigned short align)
{
    if (align == SVG_PRESERVEASPECTRATIO_UNKNOWN || align > SVG_PRESERVEASPECTRATIO_XMAXYMAX)
        return Exception { NotSupportedError };

    m_align = static_cast<SVGPreserveAspectRatioType>(align);
    return { };
}

ExceptionOr<void> SVGPreserveAspectRatioValue::setMeetOrSlice(unsigned short meetOrSlice)
{
    if (meetOrSlice == SVG_MEETORSLICE_UNKNOWN || meetOrSlice > SVG_MEETORSLICE_SLICE)
        return Exception { NotSupportedError };

    m_meetOrSlice = static_cast<SVGMeetOrSliceType>(meetOrSlice);
    return { };
}

void SVGPreserveAspectRatioValue::parse(const String& value)
{
    auto upconvertedCharacters = StringView(value).upconvertedCharacters();
    const UChar* begin = upconvertedCharacters;
    parseInternal(begin, begin + value.length(), true);
}

bool SVGPreserveAspectRatioValue::parse(const UChar*& currParam, const UChar* end, bool validate)
{
    return parseInternal(currParam, end, validate);
}

bool SVGPreserveAspectRatioValue::parseInternal(const UChar*& currParam, const UChar* end, bool validate)
{
    SVGPreserveAspectRatioType align = SVG_PRESERVEASPECTRATIO_XMIDYMID;
    SVGMeetOrSliceType meetOrSlice = SVG_MEETORSLICE_MEET;

    m_align = align;
    m_meetOrSlice = meetOrSlice;

    if (!skipOptionalSVGSpaces(currParam, end))
        return false;

    if (*currParam == 'd') {
        if (!skipString(currParam, end, "defer")) {
            LOG_ERROR("Skipped to parse except for *defer* value.");
            return false;
        }

        // FIXME: We just ignore the "defer" here.
        if (currParam == end)
            return true;

        if (!skipOptionalSVGSpaces(currParam, end))
            return false;
    }

    if (*currParam == 'n') {
        if (!skipString(currParam, end, "none")) {
            LOG_ERROR("Skipped to parse except for *none* value.");
            return false;
        }
        align = SVG_PRESERVEASPECTRATIO_NONE;
        skipOptionalSVGSpaces(currParam, end);
    } else if (*currParam == 'x') {
        if ((end - currParam) < 8)
            return false;
        if (currParam[1] != 'M' || currParam[4] != 'Y' || currParam[5] != 'M')
            return false;
        if (currParam[2] == 'i') {
            if (currParam[3] == 'n') {
                if (currParam[6] == 'i') {
                    if (currParam[7] == 'n')
                        align = SVG_PRESERVEASPECTRATIO_XMINYMIN;
                    else if (currParam[7] == 'd')
                        align = SVG_PRESERVEASPECTRATIO_XMINYMID;
                    else
                        return false;
                } else if (currParam[6] == 'a' && currParam[7] == 'x')
                    align = SVG_PRESERVEASPECTRATIO_XMINYMAX;
                else
                    return false;
             } else if (currParam[3] == 'd') {
                if (currParam[6] == 'i') {
                    if (currParam[7] == 'n')
                        align = SVG_PRESERVEASPECTRATIO_XMIDYMIN;
                    else if (currParam[7] == 'd')
                        align = SVG_PRESERVEASPECTRATIO_XMIDYMID;
                    else
                        return false;
                } else if (currParam[6] == 'a' && currParam[7] == 'x')
                    align = SVG_PRESERVEASPECTRATIO_XMIDYMAX;
                else
                    return false;
            } else
                return false;
        } else if (currParam[2] == 'a' && currParam[3] == 'x') {
            if (currParam[6] == 'i') {
                if (currParam[7] == 'n')
                    align = SVG_PRESERVEASPECTRATIO_XMAXYMIN;
                else if (currParam[7] == 'd')
                    align = SVG_PRESERVEASPECTRATIO_XMAXYMID;
                else
                    return false;
            } else if (currParam[6] == 'a' && currParam[7] == 'x')
                align = SVG_PRESERVEASPECTRATIO_XMAXYMAX;
            else
                return false;
        } else
            return false;
        currParam += 8;
        skipOptionalSVGSpaces(currParam, end);
    } else
        return false;

    if (currParam < end) {
        if (*currParam == 'm') {
            if (!skipString(currParam, end, "meet")) {
                LOG_ERROR("Skipped to parse except for *meet* or *slice* value.");
                return false;
            }
            skipOptionalSVGSpaces(currParam, end);
        } else if (*currParam == 's') {
            if (!skipString(currParam, end, "slice")) {
                LOG_ERROR("Skipped to parse except for *meet* or *slice* value.");
                return false;
            }
            skipOptionalSVGSpaces(currParam, end);
            if (align != SVG_PRESERVEASPECTRATIO_NONE)
                meetOrSlice = SVG_MEETORSLICE_SLICE;
        }
    }

    if (end != currParam && validate)
        return false;

    m_align = align;
    m_meetOrSlice = meetOrSlice;

    return true;
}

void SVGPreserveAspectRatioValue::transformRect(FloatRect& destRect, FloatRect& srcRect) const
{
    if (m_align == SVG_PRESERVEASPECTRATIO_NONE)
        return;

    FloatSize imageSize = srcRect.size();
    float origDestWidth = destRect.width();
    float origDestHeight = destRect.height();
    switch (m_meetOrSlice) {
    case SVGPreserveAspectRatioValue::SVG_MEETORSLICE_UNKNOWN:
        break;
    case SVGPreserveAspectRatioValue::SVG_MEETORSLICE_MEET: {
        float widthToHeightMultiplier = srcRect.height() / srcRect.width();
        if (origDestHeight > origDestWidth * widthToHeightMultiplier) {
            destRect.setHeight(origDestWidth * widthToHeightMultiplier);
            switch (m_align) {
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMINYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMID:
                destRect.setY(destRect.y() + origDestHeight / 2 - destRect.height() / 2);
                break;
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMINYMAX:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMAX:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMAX:
                destRect.setY(destRect.y() + origDestHeight - destRect.height());
                break;
            default:
                break;
            }
        }
        if (origDestWidth > origDestHeight / widthToHeightMultiplier) {
            destRect.setWidth(origDestHeight / widthToHeightMultiplier);
            switch (m_align) {
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMIN:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMAX:
                destRect.setX(destRect.x() + origDestWidth / 2 - destRect.width() / 2);
                break;
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMIN:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMAX:
                destRect.setX(destRect.x() + origDestWidth - destRect.width());
                break;
            default:
                break;
            }
        }
        break;
    }
    case SVGPreserveAspectRatioValue::SVG_MEETORSLICE_SLICE: {
        float widthToHeightMultiplier = srcRect.height() / srcRect.width();
        // if the destination height is less than the height of the image we'll be drawing
        if (origDestHeight < origDestWidth * widthToHeightMultiplier) {
            float destToSrcMultiplier = srcRect.width() / destRect.width();
            srcRect.setHeight(destRect.height() * destToSrcMultiplier);
            switch (m_align) {
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMINYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMID:
                srcRect.setY(srcRect.y() + imageSize.height() / 2 - srcRect.height() / 2);
                break;
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMINYMAX:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMAX:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMAX:
                srcRect.setY(srcRect.y() + imageSize.height() - srcRect.height());
                break;
            default:
                break;
            }
        }
        // if the destination width is less than the width of the image we'll be drawing
        if (origDestWidth < origDestHeight / widthToHeightMultiplier) {
            float destToSrcMultiplier = srcRect.height() / destRect.height();
            srcRect.setWidth(destRect.width() * destToSrcMultiplier);
            switch (m_align) {
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMIN:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMIDYMAX:
                srcRect.setX(srcRect.x() + imageSize.width() / 2 - srcRect.width() / 2);
                break;
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMIN:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMID:
            case SVGPreserveAspectRatioValue::SVG_PRESERVEASPECTRATIO_XMAXYMAX:
                srcRect.setX(srcRect.x() + imageSize.width() - srcRect.width());
                break;
            default:
                break;
            }
        }
        break;
    }
    }
}

AffineTransform SVGPreserveAspectRatioValue::getCTM(float logicalX, float logicalY, float logicalWidth, float logicalHeight, float physicalWidth, float physicalHeight) const
{
    AffineTransform transform;
    if (!logicalWidth || !logicalHeight || !physicalWidth || !physicalHeight) {
        ASSERT_NOT_REACHED();
        return transform;
    }

    if (m_align == SVG_PRESERVEASPECTRATIO_UNKNOWN)
        return transform;

    double extendedLogicalX = logicalX;
    double extendedLogicalY = logicalY;
    double extendedLogicalWidth = logicalWidth;
    double extendedLogicalHeight = logicalHeight;
    double extendedPhysicalWidth = physicalWidth;
    double extendedPhysicalHeight = physicalHeight;
    double logicalRatio = extendedLogicalWidth / extendedLogicalHeight;
    double physicalRatio = extendedPhysicalWidth / extendedPhysicalHeight;

    if (m_align == SVG_PRESERVEASPECTRATIO_NONE) {
        transform.scaleNonUniform(extendedPhysicalWidth / extendedLogicalWidth, extendedPhysicalHeight / extendedLogicalHeight);
        transform.translate(-extendedLogicalX, -extendedLogicalY);
        return transform;
    }

    if ((logicalRatio < physicalRatio && (m_meetOrSlice == SVG_MEETORSLICE_MEET)) || (logicalRatio >= physicalRatio && (m_meetOrSlice == SVG_MEETORSLICE_SLICE))) {
        transform.scaleNonUniform(extendedPhysicalHeight / extendedLogicalHeight, extendedPhysicalHeight / extendedLogicalHeight);

        if (m_align == SVG_PRESERVEASPECTRATIO_XMINYMIN || m_align == SVG_PRESERVEASPECTRATIO_XMINYMID || m_align == SVG_PRESERVEASPECTRATIO_XMINYMAX)
            transform.translate(-extendedLogicalX, -extendedLogicalY);
        else if (m_align == SVG_PRESERVEASPECTRATIO_XMIDYMIN || m_align == SVG_PRESERVEASPECTRATIO_XMIDYMID || m_align == SVG_PRESERVEASPECTRATIO_XMIDYMAX)
            transform.translate(-extendedLogicalX - (extendedLogicalWidth - extendedPhysicalWidth * extendedLogicalHeight / extendedPhysicalHeight) / 2, -extendedLogicalY);
        else
            transform.translate(-extendedLogicalX - (extendedLogicalWidth - extendedPhysicalWidth * extendedLogicalHeight / extendedPhysicalHeight), -extendedLogicalY);

        return transform;
    }

    transform.scaleNonUniform(extendedPhysicalWidth / extendedLogicalWidth, extendedPhysicalWidth / extendedLogicalWidth);

    if (m_align == SVG_PRESERVEASPECTRATIO_XMINYMIN || m_align == SVG_PRESERVEASPECTRATIO_XMIDYMIN || m_align == SVG_PRESERVEASPECTRATIO_XMAXYMIN)
        transform.translate(-extendedLogicalX, -extendedLogicalY);
    else if (m_align == SVG_PRESERVEASPECTRATIO_XMINYMID || m_align == SVG_PRESERVEASPECTRATIO_XMIDYMID || m_align == SVG_PRESERVEASPECTRATIO_XMAXYMID)
        transform.translate(-extendedLogicalX, -extendedLogicalY - (extendedLogicalHeight - extendedPhysicalHeight * extendedLogicalWidth / extendedPhysicalWidth) / 2);
    else
        transform.translate(-extendedLogicalX, -extendedLogicalY - (extendedLogicalHeight - extendedPhysicalHeight * extendedLogicalWidth / extendedPhysicalWidth));

    return transform;
}

String SVGPreserveAspectRatioValue::valueAsString() const
{
    String alignType;

    switch (m_align) {
    case SVG_PRESERVEASPECTRATIO_NONE:
        alignType = "none";
        break;
    case SVG_PRESERVEASPECTRATIO_XMINYMIN:
        alignType = "xMinYMin";
        break;
    case SVG_PRESERVEASPECTRATIO_XMIDYMIN:
        alignType = "xMidYMin";
        break;
    case SVG_PRESERVEASPECTRATIO_XMAXYMIN:
        alignType = "xMaxYMin";
        break;
    case SVG_PRESERVEASPECTRATIO_XMINYMID:
        alignType = "xMinYMid";
        break;
    case SVG_PRESERVEASPECTRATIO_XMIDYMID:
        alignType = "xMidYMid";
        break;
    case SVG_PRESERVEASPECTRATIO_XMAXYMID:
        alignType = "xMaxYMid";
        break;
    case SVG_PRESERVEASPECTRATIO_XMINYMAX:
        alignType = "xMinYMax";
        break;
    case SVG_PRESERVEASPECTRATIO_XMIDYMAX:
        alignType = "xMidYMax";
        break;
    case SVG_PRESERVEASPECTRATIO_XMAXYMAX:
        alignType = "xMaxYMax";
        break;
    case SVG_PRESERVEASPECTRATIO_UNKNOWN:
        alignType = "unknown";
        break;
    };

    switch (m_meetOrSlice) {
    default:
    case SVG_MEETORSLICE_UNKNOWN:
        return alignType;
    case SVG_MEETORSLICE_MEET:
        return alignType + " meet";
    case SVG_MEETORSLICE_SLICE:
        return alignType + " slice";
    }
}

}
