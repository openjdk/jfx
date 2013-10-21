/* GStreamer
 * Copyright (C) <2008> Thiago Sousa Santos <thiagoss@embedded.ufcg.edu.br>
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
/*
 * Unless otherwise indicated, Source Code is licensed under MIT license.
 * See further explanation attached in License Statement (distributed in the file
 * LICENSE).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#ifndef __FTYP_CC_H__
#define __FTYP_CC_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define FOURCC_ftyp	GST_MAKE_FOURCC('f','t','y','p')
#define FOURCC_isom     GST_MAKE_FOURCC('i','s','o','m')
#define FOURCC_iso2     GST_MAKE_FOURCC('i','s','o','2')
#define FOURCC_mp41     GST_MAKE_FOURCC('m','p','4','1')
#define FOURCC_mp42     GST_MAKE_FOURCC('m','p','4','2')
#define FOURCC_mjp2     GST_MAKE_FOURCC('m','j','p','2')
#define FOURCC_3gp4     GST_MAKE_FOURCC('3','g','p','4')
#define FOURCC_3gp6     GST_MAKE_FOURCC('3','g','p','6')
#define FOURCC_3gg6     GST_MAKE_FOURCC('3','g','g','6')
#define FOURCC_3gr6     GST_MAKE_FOURCC('3','g','r','6')
#define FOURCC_3gg7     GST_MAKE_FOURCC('3','g','g','7')
#define FOURCC_avc1     GST_MAKE_FOURCC('a','v','c','1')
#define FOURCC_qt__     GST_MAKE_FOURCC('q','t',' ',' ')
#define FOURCC_isml     GST_MAKE_FOURCC('i','s','m','l')
#define FOURCC_piff     GST_MAKE_FOURCC('p','i','f','f')

G_END_DECLS

#endif /* __FTYP_CC_H__ */
