/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
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
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
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


#ifndef __FOURCC_H__
#define __FOURCC_H__

#include <gst/gst.h>

G_BEGIN_DECLS

#define FOURCC_null     0x0

#define FOURCC_2vuy     GST_MAKE_FOURCC('2','v','u','y')
#define FOURCC_FMP4     GST_MAKE_FOURCC('F','M','P','4')
#define FOURCC_H264     GST_MAKE_FOURCC('H','2','6','4')
#define FOURCC_H265     GST_MAKE_FOURCC('H','2','6','5')
#define FOURCC_MAC3     GST_MAKE_FOURCC('M','A','C','3')
#define FOURCC_MAC6     GST_MAKE_FOURCC('M','A','C','6')
#define FOURCC_MP4V     GST_MAKE_FOURCC('M','P','4','V')
#define FOURCC_PICT     GST_MAKE_FOURCC('P','I','C','T')
#define FOURCC_QDM2     GST_MAKE_FOURCC('Q','D','M','2')
#define FOURCC_SVQ3     GST_MAKE_FOURCC('S','V','Q','3')
#define FOURCC_VP31     GST_MAKE_FOURCC('V','P','3','1')
#define FOURCC_VP80     GST_MAKE_FOURCC('V','P','8','0')
#define FOURCC_WRLE     GST_MAKE_FOURCC('W','R','L','E')
#define FOURCC_XMP_     GST_MAKE_FOURCC('X','M','P','_')
#define FOURCC_XVID     GST_MAKE_FOURCC('X','V','I','D')
#define FOURCC__ART     GST_MAKE_FOURCC(0xa9,'A','R','T')
#define FOURCC_____     GST_MAKE_FOURCC('-','-','-','-')
#define FOURCC___in     GST_MAKE_FOURCC(' ',' ','i','n')
#define FOURCC___ty     GST_MAKE_FOURCC(' ',' ','t','y')
#define FOURCC__alb     GST_MAKE_FOURCC(0xa9,'a','l','b')
#define FOURCC__cpy     GST_MAKE_FOURCC(0xa9,'c','p','y')
#define FOURCC__day     GST_MAKE_FOURCC(0xa9,'d','a','y')
#define FOURCC__des     GST_MAKE_FOURCC(0xa9,'d','e','s')
#define FOURCC__enc     GST_MAKE_FOURCC(0xa9,'e','n','c')
#define FOURCC__gen     GST_MAKE_FOURCC(0xa9, 'g', 'e', 'n')
#define FOURCC__grp     GST_MAKE_FOURCC(0xa9,'g','r','p')
#define FOURCC__inf     GST_MAKE_FOURCC(0xa9,'i','n','f')
#define FOURCC__lyr     GST_MAKE_FOURCC(0xa9,'l','y','r')
#define FOURCC__mp3     GST_MAKE_FOURCC('.','m','p','3')
#define FOURCC__nam     GST_MAKE_FOURCC(0xa9,'n','a','m')
#define FOURCC__req     GST_MAKE_FOURCC(0xa9,'r','e','q')
#define FOURCC__too     GST_MAKE_FOURCC(0xa9,'t','o','o')
#define FOURCC__wrt     GST_MAKE_FOURCC(0xa9,'w','r','t')
#define FOURCC_aART     GST_MAKE_FOURCC('a','A','R','T')
#define FOURCC_ac_3     GST_MAKE_FOURCC('a','c','-','3')
#define FOURCC_agsm     GST_MAKE_FOURCC('a','g','s','m')
#define FOURCC_alac     GST_MAKE_FOURCC('a','l','a','c')
#define FOURCC_fLaC     GST_MAKE_FOURCC('f','L','a','C')
#define FOURCC_dfLa     GST_MAKE_FOURCC('d','f','L','a')
#define FOURCC_alaw     GST_MAKE_FOURCC('a','l','a','w')
#define FOURCC_alis     GST_MAKE_FOURCC('a','l','i','s')
#define FOURCC_appl     GST_MAKE_FOURCC('a','p','p','l')
#define FOURCC_avc1     GST_MAKE_FOURCC('a','v','c','1')
#define FOURCC_avc3     GST_MAKE_FOURCC('a','v','c','3')
#define FOURCC_avcC     GST_MAKE_FOURCC('a','v','c','C')
#define FOURCC_c608     GST_MAKE_FOURCC('c','6','0','8')
#define FOURCC_c708     GST_MAKE_FOURCC('c','7','0','8')
#define FOURCC_ccdp     GST_MAKE_FOURCC('c','c','d','p')
#define FOURCC_cdat     GST_MAKE_FOURCC('c','d','a','t')
#define FOURCC_cdt2     GST_MAKE_FOURCC('c','d','t','2')
#define FOURCC_clcp     GST_MAKE_FOURCC('c','l','c','p')
#define FOURCC_clip     GST_MAKE_FOURCC('c','l','i','p')
#define FOURCC_cmov     GST_MAKE_FOURCC('c','m','o','v')
#define FOURCC_cmvd     GST_MAKE_FOURCC('c','m','v','d')
#define FOURCC_co64     GST_MAKE_FOURCC('c','o','6','4')
#define FOURCC_covr     GST_MAKE_FOURCC('c','o','v','r')
#define FOURCC_cpil     GST_MAKE_FOURCC('c','p','i','l')
#define FOURCC_cprt     GST_MAKE_FOURCC('c','p','r','t')
#define FOURCC_crgn     GST_MAKE_FOURCC('c','r','g','n')
#define FOURCC_ctab     GST_MAKE_FOURCC('c','t','a','b')
#define FOURCC_ctts     GST_MAKE_FOURCC('c','t','t','s')
#define FOURCC_cslg     GST_MAKE_FOURCC('c','s','l','g')
#define FOURCC_d263     GST_MAKE_FOURCC('d','2','6','3')
#define FOURCC_dac3     GST_MAKE_FOURCC('d','a','c','3')
#define FOURCC_damr     GST_MAKE_FOURCC('d','a','m','r')
#define FOURCC_data     GST_MAKE_FOURCC('d','a','t','a')
#define FOURCC_dcom     GST_MAKE_FOURCC('d','c','o','m')
#define FOURCC_desc     GST_MAKE_FOURCC('d','e','s','c')
#define FOURCC_dhlr     GST_MAKE_FOURCC('d','h','l','r')
#define FOURCC_dinf     GST_MAKE_FOURCC('d','i','n','f')
#define FOURCC_disc     GST_MAKE_FOURCC('d','i','s','c')
#define FOURCC_disk     GST_MAKE_FOURCC('d','i','s','k')
#define FOURCC_drac     GST_MAKE_FOURCC('d','r','a','c')
#define FOURCC_dref     GST_MAKE_FOURCC('d','r','e','f')
#define FOURCC_drmi     GST_MAKE_FOURCC('d','r','m','i')
#define FOURCC_drms     GST_MAKE_FOURCC('d','r','m','s')
#define FOURCC_dvcp     GST_MAKE_FOURCC('d','v','c','p')
#define FOURCC_dvc_     GST_MAKE_FOURCC('d','v','c',' ')
#define FOURCC_dv5p     GST_MAKE_FOURCC('d','v','5','p')
#define FOURCC_dv5n     GST_MAKE_FOURCC('d','v','5','n')
#define FOURCC_dva1     GST_MAKE_FOURCC('d','v','a','1')
#define FOURCC_dvav     GST_MAKE_FOURCC('d','v','a','v')
#define FOURCC_dvh1     GST_MAKE_FOURCC('d','v','h','1')
#define FOURCC_dvhe     GST_MAKE_FOURCC('d','v','h','e')
#define FOURCC_dvcC     GST_MAKE_FOURCC('d','v','c','C')
#define FOURCC_edts     GST_MAKE_FOURCC('e','d','t','s')
#define FOURCC_elst     GST_MAKE_FOURCC('e','l','s','t')
#define FOURCC_enda     GST_MAKE_FOURCC('e','n','d','a')
#define FOURCC_esds     GST_MAKE_FOURCC('e','s','d','s')
#define FOURCC_fmp4     GST_MAKE_FOURCC('f','m','p','4')
#define FOURCC_free     GST_MAKE_FOURCC('f','r','e','e')
#define FOURCC_frma     GST_MAKE_FOURCC('f','r','m','a')
#define FOURCC_ftyp     GST_MAKE_FOURCC('f','t','y','p')
#define FOURCC_ftab     GST_MAKE_FOURCC('f','t','a','b')
#define FOURCC_gama     GST_MAKE_FOURCC('g','a','m','a')
#define FOURCC_glbl     GST_MAKE_FOURCC('g','l','b','l')
#define FOURCC_gmhd     GST_MAKE_FOURCC('g','m','h','d')
#define FOURCC_gmin     GST_MAKE_FOURCC('g','m','i','n')
#define FOURCC_gnre     GST_MAKE_FOURCC('g','n','r','e')
#define FOURCC_h263     GST_MAKE_FOURCC('h','2','6','3')
#define FOURCC_hdlr     GST_MAKE_FOURCC('h','d','l','r')
#define FOURCC_hev1     GST_MAKE_FOURCC('h','e','v','1')
#define FOURCC_hint     GST_MAKE_FOURCC('h','i','n','t')
#define FOURCC_hmhd     GST_MAKE_FOURCC('h','m','h','d')
#define FOURCC_hndl     GST_MAKE_FOURCC('h','n','d','l')
#define FOURCC_hnti     GST_MAKE_FOURCC('h','n','t','i')
#define FOURCC_hvc1     GST_MAKE_FOURCC('h','v','c','1')
#define FOURCC_hvcC     GST_MAKE_FOURCC('h','v','c','C')
#define FOURCC_ilst     GST_MAKE_FOURCC('i','l','s','t')
#define FOURCC_ima4     GST_MAKE_FOURCC('i','m','a','4')
#define FOURCC_imap     GST_MAKE_FOURCC('i','m','a','p')
#define FOURCC_s16l     GST_MAKE_FOURCC('s','1','6','l')
#define FOURCC_in24     GST_MAKE_FOURCC('i','n','2','4')
#define FOURCC_in32     GST_MAKE_FOURCC('i','n','3','2')
#define FOURCC_fl64     GST_MAKE_FOURCC('f','l','6','4')
#define FOURCC_fl32     GST_MAKE_FOURCC('f','l','3','2')
#define FOURCC_jp2c     GST_MAKE_FOURCC('j','p','2','c')
#define FOURCC_jpeg     GST_MAKE_FOURCC('j','p','e','g')
#define FOURCC_keyw     GST_MAKE_FOURCC('k','e','y','w')
#define FOURCC_kmat     GST_MAKE_FOURCC('k','m','a','t')
#define FOURCC_kywd     GST_MAKE_FOURCC('k','y','w','d')
#define FOURCC_load     GST_MAKE_FOURCC('l','o','a','d')
#define FOURCC_matt     GST_MAKE_FOURCC('m','a','t','t')
#define FOURCC_mdat     GST_MAKE_FOURCC('m','d','a','t')
#define FOURCC_mdhd     GST_MAKE_FOURCC('m','d','h','d')
#define FOURCC_mdia     GST_MAKE_FOURCC('m','d','i','a')
#define FOURCC_mdir     GST_MAKE_FOURCC('m','d','i','r')
#define FOURCC_mean     GST_MAKE_FOURCC('m','e','a','n')
#define FOURCC_meta     GST_MAKE_FOURCC('m','e','t','a')
#define FOURCC_mhlr     GST_MAKE_FOURCC('m','h','l','r')
#define FOURCC_minf     GST_MAKE_FOURCC('m','i','n','f')
#define FOURCC_moov     GST_MAKE_FOURCC('m','o','o','v')
#define FOURCC_mp3_     GST_MAKE_FOURCC('m','p','3',' ')
#define FOURCC_mp4a     GST_MAKE_FOURCC('m','p','4','a')
#define FOURCC_mp4s     GST_MAKE_FOURCC('m','p','4','s')
#define FOURCC_mp4s     GST_MAKE_FOURCC('m','p','4','s')
#define FOURCC_mp4v     GST_MAKE_FOURCC('m','p','4','v')
#define FOURCC_name     GST_MAKE_FOURCC('n','a','m','e')
#define FOURCC_nclc     GST_MAKE_FOURCC('n','c','l','c')
#define FOURCC_nclx     GST_MAKE_FOURCC('n','c','l','x')
#define FOURCC_nmhd     GST_MAKE_FOURCC('n','m','h','d')
#define FOURCC_opus     GST_MAKE_FOURCC('O','p','u','s')
#define FOURCC_dops     GST_MAKE_FOURCC('d','O','p','s')
#define FOURCC_pasp     GST_MAKE_FOURCC('p','a','s','p')
#define FOURCC_colr     GST_MAKE_FOURCC('c','o','l','r')
#define FOURCC_clap     GST_MAKE_FOURCC('c','l','a','p')
#define FOURCC_tapt     GST_MAKE_FOURCC('t','a','p','t')
#define FOURCC_clef     GST_MAKE_FOURCC('c','l','e','f')
#define FOURCC_prof     GST_MAKE_FOURCC('p','r','o','f')
#define FOURCC_enof     GST_MAKE_FOURCC('e','n','o','f')
#define FOURCC_fiel     GST_MAKE_FOURCC('f','i','e','l')
#define FOURCC_pcst     GST_MAKE_FOURCC('p','c','s','t')
#define FOURCC_pgap     GST_MAKE_FOURCC('p','g','a','p')
#define FOURCC_png      GST_MAKE_FOURCC('p','n','g',' ')
#define FOURCC_pnot     GST_MAKE_FOURCC('p','n','o','t')
#define FOURCC_qt__     GST_MAKE_FOURCC('q','t',' ',' ')
#define FOURCC_qtim     GST_MAKE_FOURCC('q','t','i','m')
#define FOURCC_raw_     GST_MAKE_FOURCC('r','a','w',' ')
#define FOURCC_rdrf     GST_MAKE_FOURCC('r','d','r','f')
#define FOURCC_rle_     GST_MAKE_FOURCC('r','l','e',' ')
#define FOURCC_rmda     GST_MAKE_FOURCC('r','m','d','a')
#define FOURCC_rmdr     GST_MAKE_FOURCC('r','m','d','r')
#define FOURCC_rmra     GST_MAKE_FOURCC('r','m','r','a')
#define FOURCC_rmvc     GST_MAKE_FOURCC('r','m','v','c')
#define FOURCC_rtp_     GST_MAKE_FOURCC('r','t','p',' ')
#define FOURCC_rtsp     GST_MAKE_FOURCC('r','t','s','p')
#define FOURCC_s263     GST_MAKE_FOURCC('s','2','6','3')
#define FOURCC_samr     GST_MAKE_FOURCC('s','a','m','r')
#define FOURCC_sawb     GST_MAKE_FOURCC('s','a','w','b')
#define FOURCC_sbtl     GST_MAKE_FOURCC('s','b','t','l')
#define FOURCC_sdp_     GST_MAKE_FOURCC('s','d','p',' ')
#define FOURCC_sidx     GST_MAKE_FOURCC('s','i','d','x')
#define FOURCC_skip     GST_MAKE_FOURCC('s','k','i','p')
#define FOURCC_smhd     GST_MAKE_FOURCC('s','m','h','d')
#define FOURCC_soaa     GST_MAKE_FOURCC('s','o','a','a')
#define FOURCC_soal     GST_MAKE_FOURCC('s','o','a','l')
#define FOURCC_soar     GST_MAKE_FOURCC('s','o','a','r')
#define FOURCC_soco     GST_MAKE_FOURCC('s','o','c','o')
#define FOURCC_sonm     GST_MAKE_FOURCC('s','o','n','m')
#define FOURCC_sosn     GST_MAKE_FOURCC('s','o','s','n')
#define FOURCC_soun     GST_MAKE_FOURCC('s','o','u','n')
#define FOURCC_sowt     GST_MAKE_FOURCC('s','o','w','t')
#define FOURCC_stbl     GST_MAKE_FOURCC('s','t','b','l')
#define FOURCC_stco     GST_MAKE_FOURCC('s','t','c','o')
#define FOURCC_stpp     GST_MAKE_FOURCC('s','t','p','p')
#define FOURCC_stps     GST_MAKE_FOURCC('s','t','p','s')
#define FOURCC_strf     GST_MAKE_FOURCC('s','t','r','f')
#define FOURCC_strm     GST_MAKE_FOURCC('s','t','r','m')
#define FOURCC_stsc     GST_MAKE_FOURCC('s','t','s','c')
#define FOURCC_stsd     GST_MAKE_FOURCC('s','t','s','d')
#define FOURCC_stss     GST_MAKE_FOURCC('s','t','s','s')
#define FOURCC_stsz     GST_MAKE_FOURCC('s','t','s','z')
#define FOURCC_stts     GST_MAKE_FOURCC('s','t','t','s')
#define FOURCC_styp     GST_MAKE_FOURCC('s','t','y','p')
#define FOURCC_subp     GST_MAKE_FOURCC('s','u','b','p')
#define FOURCC_subt     GST_MAKE_FOURCC('s','u','b','t')
#define FOURCC_text     GST_MAKE_FOURCC('t','e','x','t')
#define FOURCC_tcmi     GST_MAKE_FOURCC('t','c','m','i')
#define FOURCC_tkhd     GST_MAKE_FOURCC('t','k','h','d')
#define FOURCC_tmcd     GST_MAKE_FOURCC('t','m','c','d')
#define FOURCC_tmpo     GST_MAKE_FOURCC('t','m','p','o')
#define FOURCC_trak     GST_MAKE_FOURCC('t','r','a','k')
#define FOURCC_tref     GST_MAKE_FOURCC('t','r','e','f')
#define FOURCC_trkn     GST_MAKE_FOURCC('t','r','k','n')
#define FOURCC_tven     GST_MAKE_FOURCC('t','v','e','n')
#define FOURCC_tves     GST_MAKE_FOURCC('t','v','e','s')
#define FOURCC_tvsh     GST_MAKE_FOURCC('t','v','s','h')
#define FOURCC_tvsn     GST_MAKE_FOURCC('t','v','s','n')
#define FOURCC_twos     GST_MAKE_FOURCC('t','w','o','s')
#define FOURCC_tx3g     GST_MAKE_FOURCC('t','x','3','g')
#define FOURCC_udta     GST_MAKE_FOURCC('u','d','t','a')
#define FOURCC_ulaw     GST_MAKE_FOURCC('u','l','a','w')
#define FOURCC_url_     GST_MAKE_FOURCC('u','r','l',' ')
#define FOURCC_uuid     GST_MAKE_FOURCC('u','u','i','d')
#define FOURCC_v210     GST_MAKE_FOURCC('v','2','1','0')
#define FOURCC_vc_1     GST_MAKE_FOURCC('v','c','-','1')
#define FOURCC_vide     GST_MAKE_FOURCC('v','i','d','e')
#define FOURCC_vmhd     GST_MAKE_FOURCC('v','m','h','d')
#define FOURCC_vp08     GST_MAKE_FOURCC('v','p','0','8')
#define FOURCC_vp09     GST_MAKE_FOURCC('v','p','0','9')
#define FOURCC_vpcC     GST_MAKE_FOURCC('v','p','c','C')
#define FOURCC_xvid     GST_MAKE_FOURCC('x','v','i','d')
#define FOURCC_wave     GST_MAKE_FOURCC('w','a','v','e')
#define FOURCC_wide     GST_MAKE_FOURCC('w','i','d','e')
#define FOURCC_zlib     GST_MAKE_FOURCC('z','l','i','b')
#define FOURCC_lpcm     GST_MAKE_FOURCC('l','p','c','m')
#define FOURCC_av01     GST_MAKE_FOURCC('a','v','0','1')
#define FOURCC_av1C     GST_MAKE_FOURCC('a','v','1','C')
#define FOURCC_av1f     GST_MAKE_FOURCC('a','v','1','f')
#define FOURCC_av1m     GST_MAKE_FOURCC('a','v','1','m')
#define FOURCC_av1s     GST_MAKE_FOURCC('a','v','1','s')
#define FOURCC_av1M     GST_MAKE_FOURCC('a','v','1','M')

#define FOURCC_cfhd     GST_MAKE_FOURCC('C','F','H','D')
#define FOURCC_ap4x     GST_MAKE_FOURCC('a','p','4','x')
#define FOURCC_ap4h     GST_MAKE_FOURCC('a','p','4','h')
#define FOURCC_apch     GST_MAKE_FOURCC('a','p','c','h')
#define FOURCC_apcn     GST_MAKE_FOURCC('a','p','c','n')
#define FOURCC_apco     GST_MAKE_FOURCC('a','p','c','o')
#define FOURCC_apcs     GST_MAKE_FOURCC('a','p','c','s')
#define FOURCC_m1v      GST_MAKE_FOURCC('m','1','v',' ')
#define FOURCC_vivo     GST_MAKE_FOURCC('v','i','v','o')
#define FOURCC_saiz     GST_MAKE_FOURCC('s','a','i','z')
#define FOURCC_saio     GST_MAKE_FOURCC('s','a','i','o')

#define FOURCC_3gg6     GST_MAKE_FOURCC('3','g','g','6')
#define FOURCC_3gg7     GST_MAKE_FOURCC('3','g','g','7')
#define FOURCC_3gp4     GST_MAKE_FOURCC('3','g','p','4')
#define FOURCC_3gp6     GST_MAKE_FOURCC('3','g','p','6')
#define FOURCC_3gr6     GST_MAKE_FOURCC('3','g','r','6')
#define FOURCC_3g__     GST_MAKE_FOURCC('3','g',0,0)
#define FOURCC_isml     GST_MAKE_FOURCC('i','s','m','l')
#define FOURCC_iso2     GST_MAKE_FOURCC('i','s','o','2')
#define FOURCC_isom     GST_MAKE_FOURCC('i','s','o','m')
#define FOURCC_mp41     GST_MAKE_FOURCC('m','p','4','1')
#define FOURCC_mp42     GST_MAKE_FOURCC('m','p','4','2')
#define FOURCC_piff     GST_MAKE_FOURCC('p','i','f','f')
#define FOURCC_titl     GST_MAKE_FOURCC('t','i','t','l')

/* SVQ3 fourcc */
#define FOURCC_SEQH     GST_MAKE_FOURCC('S','E','Q','H')
#define FOURCC_SMI_     GST_MAKE_FOURCC('S','M','I',' ')

/* 3gpp asset meta data fourcc */
#define FOURCC_albm     GST_MAKE_FOURCC('a','l','b','m')
#define FOURCC_auth     GST_MAKE_FOURCC('a','u','t','h')
#define FOURCC_clsf     GST_MAKE_FOURCC('c','l','s','f')
#define FOURCC_dscp     GST_MAKE_FOURCC('d','s','c','p')
#define FOURCC_loci     GST_MAKE_FOURCC('l','o','c','i')
#define FOURCC_perf     GST_MAKE_FOURCC('p','e','r','f')
#define FOURCC_rtng     GST_MAKE_FOURCC('r','t','n','g')
#define FOURCC_yrrc     GST_MAKE_FOURCC('y','r','r','c')

/* misc tag stuff */
#define FOURCC_ID32     GST_MAKE_FOURCC('I', 'D','3','2')

/* ISO Motion JPEG 2000 fourcc */
#define FOURCC_cdef     GST_MAKE_FOURCC('c','d','e','f')
#define FOURCC_cmap     GST_MAKE_FOURCC('c','m','a','p')
#define FOURCC_ihdr     GST_MAKE_FOURCC('i','h','d','r')
#define FOURCC_jp2h     GST_MAKE_FOURCC('j','p','2','h')
#define FOURCC_jp2x     GST_MAKE_FOURCC('j','p','2','x')
#define FOURCC_mjp2     GST_MAKE_FOURCC('m','j','p','2')

/* some buggy hardware's notion of mdhd */
#define FOURCC_mhdr     GST_MAKE_FOURCC('m','h','d','r')

/* Fragmented MP4 */
#define FOURCC_btrt     GST_MAKE_FOURCC('b','t','r','t')
#define FOURCC_mehd     GST_MAKE_FOURCC('m','e','h','d')
#define FOURCC_mfhd     GST_MAKE_FOURCC('m','f','h','d')
#define FOURCC_mfra     GST_MAKE_FOURCC('m','f','r','a')
#define FOURCC_mfro     GST_MAKE_FOURCC('m','f','r','o')
#define FOURCC_moof     GST_MAKE_FOURCC('m','o','o','f')
#define FOURCC_mvex     GST_MAKE_FOURCC('m','v','e','x')
#define FOURCC_mvhd     GST_MAKE_FOURCC('m','v','h','d')
#define FOURCC_ovc1     GST_MAKE_FOURCC('o','v','c','1')
#define FOURCC_owma     GST_MAKE_FOURCC('o','w','m','a')
#define FOURCC_sdtp     GST_MAKE_FOURCC('s','d','t','p')
#define FOURCC_tfhd     GST_MAKE_FOURCC('t','f','h','d')
#define FOURCC_tfra     GST_MAKE_FOURCC('t','f','r','a')
#define FOURCC_traf     GST_MAKE_FOURCC('t','r','a','f')
#define FOURCC_trex     GST_MAKE_FOURCC('t','r','e','x')
#define FOURCC_trun     GST_MAKE_FOURCC('t','r','u','n')
#define FOURCC_wma_     GST_MAKE_FOURCC('w','m','a',' ')

/* MPEG DASH */
#define FOURCC_tfdt     GST_MAKE_FOURCC('t','f','d','t')

/* Xiph fourcc */
#define FOURCC_XdxT     GST_MAKE_FOURCC('X','d','x','T')
#define FOURCC_XiTh     GST_MAKE_FOURCC('X','i','T','h')
#define FOURCC_tCtC     GST_MAKE_FOURCC('t','C','t','C')
#define FOURCC_tCtH     GST_MAKE_FOURCC('t','C','t','H')
#define FOURCC_tCt_     GST_MAKE_FOURCC('t','C','t','#')

/* ilst metatags */
#define FOURCC__cmt     GST_MAKE_FOURCC(0xa9, 'c','m','t')

/* apple tags */
#define FOURCC__mak     GST_MAKE_FOURCC(0xa9, 'm','a','k')
#define FOURCC__mod     GST_MAKE_FOURCC(0xa9, 'm','o','d')
#define FOURCC__swr     GST_MAKE_FOURCC(0xa9, 's','w','r')

/* Chapters reference */
#define FOURCC_chap     GST_MAKE_FOURCC('c','h','a','p')

/* For Microsoft Wave formats embedded in quicktime, the FOURCC is
   'm', 's', then the 16 bit wave codec id */
#define MS_WAVE_FOURCC(codecid)  GST_MAKE_FOURCC( \
        'm', 's', ((codecid)>>8)&0xff, ((codecid)&0xff))

/* MPEG Application Format , Stereo Video */
#define FOURCC_ss01     GST_MAKE_FOURCC('s','s','0','1')
#define FOURCC_ss02     GST_MAKE_FOURCC('s','s','0','2')
#define FOURCC_svmi     GST_MAKE_FOURCC('s','v','m','i')
#define FOURCC_scdi     GST_MAKE_FOURCC('s','c','d','i')

/* Protected streams */
#define FOURCC_encv     GST_MAKE_FOURCC('e','n','c','v')
#define FOURCC_enca     GST_MAKE_FOURCC('e','n','c','a')
#define FOURCC_enct     GST_MAKE_FOURCC('e','n','c','t')
#define FOURCC_encs     GST_MAKE_FOURCC('e','n','c','s')
#define FOURCC_sinf     GST_MAKE_FOURCC('s','i','n','f')
#define FOURCC_frma     GST_MAKE_FOURCC('f','r','m','a')
#define FOURCC_schm     GST_MAKE_FOURCC('s','c','h','m')
#define FOURCC_schi     GST_MAKE_FOURCC('s','c','h','i')

/* Common Encryption */
#define FOURCC_pssh     GST_MAKE_FOURCC('p','s','s','h')
#define FOURCC_tenc     GST_MAKE_FOURCC('t','e','n','c')
#define FOURCC_cenc     GST_MAKE_FOURCC('c','e','n','c')

G_END_DECLS

#endif /* __FOURCC_H__ */
