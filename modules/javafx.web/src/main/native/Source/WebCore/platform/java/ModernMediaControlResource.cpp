/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
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

#include "ModernMediaControlResource.h"

ModernMediaControlResource::ModernMediaControlResource() {
     /*
     imageMap {IconName , base64 code }
     The below map for icon images along with Base64 codes were generated from the resources
     in WebCore/Modules/modern-media-controls/images/adwaita/ using the Tools/Scripts/generate_icon_resource.py
    */
    imageMap = {
    {"Play"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTZweCIgdmlld0JveD0iMCAwIDE2IDE2IiB3aWR0aD0iMTZweCI+PHBhdGggZD0ibSA0IDIuOTk2MDk0IHYgMTAgaCAxIGMgMC4xNzU3ODEgMCAwLjM0NzY1NiAtMC4wMzkwNjMgMC41IC0wLjEyNSBsIDcgLTQgYyAwLjMxMjUgLTAuMTcxODc1IDAuNDY4NzUgLTAuNTIzNDM4IDAuNDY4NzUgLTAuODc1IGMgMCAtMC4zNTE1NjMgLTAuMTU2MjUgLTAuNzAzMTI1IC0wLjQ2ODc1IC0wLjg3NSBsIC03IC00IGMgLTAuMTUyMzQ0IC0wLjA4NTkzOCAtMC4zMjQyMTkgLTAuMTI1IC0wLjUgLTAuMTI1IHogbSAwIDAiIGZpbGw9IiMyZTM0MzYiLz48L3N2Zz4K"_s},
    {"PipIn"_s ,"PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTZweCIgdmlld0JveD0iMCAwIDE2IDE2IiB3aWR0aD0iMTZweCI+PGcgZmlsbD0iIzJlMzQzNiI+PHBhdGggZD0ibSAxIDIuMDA3ODEyIGMgLTAuNTUwNzgxIDAgLTEgMC40NDkyMTkgLTEgMSB2IDkgYyAwIDAuNTUwNzgyIDAuNDQ5MjE5IDEgMSAxIGggNCB2IC0yIGggLTMgdiAtNyBoIDEwIHYgMyBoIDIgdiAtNCBjIDAgLTAuNTUwNzgxIC0wLjQ0OTIxOSAtMSAtMSAtMSB6IG0gMCAwIiBmaWxsLW9wYWNpdHk9IjAuMzUiLz48cGF0aCBkPSJtIDkgMTAgYyAtMC41NTA3ODEgMCAtMSAwLjQ0OTIxOSAtMSAxIHYgNC4wMDc4MTIgYyAwIDAuNTUwNzgyIDAuNDQ5MjE5IDEgMSAxIGggNiBjIDAuNTUwNzgxIDAgMSAtMC40NDkyMTggMSAtMSB2IC00LjAwNzgxMiBjIDAgLTAuNTUwNzgxIC0wLjQ0OTIxOSAtMSAtMSAtMSB6IG0gMSAyIGggNCB2IDIuMDA3ODEyIGggLTQgeiBtIDAgMCIvPjxwYXRoIGQ9Im0gMy4xMzI4MTIgNS4xNDA2MjUgYyAwLjE3MTg3NiAtMC4xNjQwNjMgMC40OTYwOTQgLTAuMTg3NSAwLjc1NzgxMyAwLjAxNTYyNSBsIDMuMTA5Mzc1IDMuMDg5ODQ0IHYgMC43NTM5MDYgaCAtMC43NTM5MDYgbCAtMy4xMDkzNzUgLTMuMDg5ODQ0IGMgLTAuMTkxNDA3IC0wLjE5MTQwNiAtMC4xNzk2ODggLTAuNTk3NjU2IC0wLjAwMzkwNyAtMC43Njk1MzEgeiBtIDAgMCIvPjxwYXRoIGQ9Im0gNCA5IGggNCB2IDEgaCAtNCB6IG0gMCAwIi8+PHBhdGggZD0ibSA3IDYgaCAxIHYgNCBoIC0xIHogbSAwIDAiLz48L2c+PC9zdmc+Cg=="_s},
    {"EnterFullscreen"_s , "PD94bWwgdmVyc2lvbj0nMS4wJyBlbmNvZGluZz0nVVRGLTgnIHN0YW5kYWxvbmU9J25vJz8+Cjxzdmcgd2lkdGg9JzE2JyBoZWlnaHQ9JzE2JyB2ZXJzaW9uPScxLjEnIHhtbG5zPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZycgeG1sbnM6c3ZnPSdodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2Zyc+CjxkZWZzIGlkPSdkZWZzNTknIC8+CjxnIGNvbG9yPSclMjMwMDAnIGZpbGw9JyUyM0ZGRkZGRicgaWQ9J2c1MycgdHJhbnNmb3JtPSdtYXRyaXgoLTEsMCwwLDEsMTUuOTk4MDM0LDApJz4KPHBhdGgKICAgZD0nTSAxLjk4NCw4Ljk4NiBBIDEsMSAwIDAgMCAxLDEwIHYgNCBhIDEsMSAwIDAgMCAxLDEgSCA2IEEgMSwxIDAgMSAwIDYsMTMgSCAzIFYgMTAgQSAxLDEgMCAwIDAgMS45ODQsOC45ODYgWicKICAgc3R5bGU9J2xpbmUtaGVpZ2h0Om5vcm1hbDtmb250LXZhcmlhbnQtbGlnYXR1cmVzOm5vcm1hbDtmb250LXZhcmlhbnQtcG9zaXRpb246bm9ybWFsO2ZvbnQtdmFyaWFudC1jYXBzOm5vcm1hbDtmb250LXZhcmlhbnQtbnVtZXJpYzpub3JtYWw7Zm9udC12YXJpYW50LWFsdGVybmF0ZXM6bm9ybWFsO2ZvbnQtZmVhdHVyZS1zZXR0aW5nczpub3JtYWw7dGV4dC1pbmRlbnQ6MDt0ZXh0LWFsaWduOnN0YXJ0O3RleHQtZGVjb3JhdGlvbi1saW5lOm5vbmU7dGV4dC1kZWNvcmF0aW9uLXN0eWxlOnNvbGlkO3RleHQtdHJhbnNmb3JtOm5vbmU7dGV4dC1vcmllbnRhdGlvbjptaXhlZDtzaGFwZS1wYWRkaW5nOjA7aXNvbGF0aW9uOmF1dG87bWl4LWJsZW5kLW1vZGU6bm9ybWFsO21hcmtlcjpub25lJwogICBmb250LXdlaWdodD0nNDAwJwogICBmb250LWZhbWlseT0nc2Fucy1zZXJpZicKICAgd2hpdGUtc3BhY2U9J25vcm1hbCcKICAgb3ZlcmZsb3c9J3Zpc2libGUnCiAgIGlkPSdwYXRoMzknIC8+CjxwYXRoCiAgIGQ9J00gNi40OCw4LjQ5IEEgMSwxIDAgMCAwIDUuNzkzLDguNzkzIGwgLTQuNSw0LjUgYSAxLDEgMCAxIDAgMS40MTQsMS40MTQgbCA0LjUsLTQuNSBBIDEsMSAwIDAgMCA2LjQ4LDguNDkgWicKICAgc3R5bGU9J2xpbmUtaGVpZ2h0Om5vcm1hbDtmb250LXZhcmlhbnQtbGlnYXR1cmVzOm5vcm1hbDtmb250LXZhcmlhbnQtcG9zaXRpb246bm9ybWFsO2ZvbnQtdmFyaWFudC1jYXBzOm5vcm1hbDtmb250LXZhcmlhbnQtbnVtZXJpYzpub3JtYWw7Zm9udC12YXJpYW50LWFsdGVybmF0ZXM6bm9ybWFsO2ZvbnQtZmVhdHVyZS1zZXR0aW5nczpub3JtYWw7dGV4dC1pbmRlbnQ6MDt0ZXh0LWFsaWduOnN0YXJ0O3RleHQtZGVjb3JhdGlvbi1saW5lOm5vbmU7dGV4dC1kZWNvcmF0aW9uLXN0eWxlOnNvbGlkO3RleHQtdHJhbnNmb3JtOm5vbmU7dGV4dC1vcmllbnRhdGlvbjptaXhlZDtzaGFwZS1wYWRkaW5nOjA7aXNvbGF0aW9uOmF1dG87bWl4LWJsZW5kLW1vZGU6bm9ybWFsO21hcmtlcjpub25lJwogICBmb250LXdlaWdodD0nNDAwJwogICBmb250LWZhbWlseT0nc2Fucy1zZXJpZicKICAgd2hpdGUtc3BhY2U9J25vcm1hbCcKICAgb3ZlcmZsb3c9J3Zpc2libGUnCiAgIGlkPSdwYXRoNDEnIC8+CjxwYXRoCiAgIGQ9J20gMSwxNCBoIDEgdiAxIEggMSBaJwogICBzdHlsZT0nbWFya2VyOm5vbmUnCiAgIG92ZXJmbG93PSd2aXNpYmxlJwogICBpZD0ncGF0aDQzJyAvPgo8cGF0aAogICBkPSdtIDEwLDEgYSAxLDEgMCAxIDAgMCwyIGggMyB2IDMgYSAxLDEgMCAxIDAgMiwwIFYgMiBBIDEsMSAwIDAgMCAxNCwxIFonCiAgIHN0eWxlPSdsaW5lLWhlaWdodDpub3JtYWw7Zm9udC12YXJpYW50LWxpZ2F0dXJlczpub3JtYWw7Zm9udC12YXJpYW50LXBvc2l0aW9uOm5vcm1hbDtmb250LXZhcmlhbnQtY2Fwczpub3JtYWw7Zm9udC12YXJpYW50LW51bWVyaWM6bm9ybWFsO2ZvbnQtdmFyaWFudC1hbHRlcm5hdGVzOm5vcm1hbDtmb250LWZlYXR1cmUtc2V0dGluZ3M6bm9ybWFsO3RleHQtaW5kZW50OjA7dGV4dC1hbGlnbjpzdGFydDt0ZXh0LWRlY29yYXRpb24tbGluZTpub25lO3RleHQtZGVjb3JhdGlvbi1zdHlsZTpzb2xpZDt0ZXh0LXRyYW5zZm9ybTpub25lO3RleHQtb3JpZW50YXRpb246bWl4ZWQ7c2hhcGUtcGFkZGluZzowO2lzb2xhdGlvbjphdXRvO21peC1ibGVuZC1tb2RlOm5vcm1hbDttYXJrZXI6bm9uZScKICAgZm9udC13ZWlnaHQ9JzQwMCcKICAgZm9udC1mYW1pbHk9J3NhbnMtc2VyaWYnCiAgIHdoaXRlLXNwYWNlPSdub3JtYWwnCiAgIG92ZXJmbG93PSd2aXNpYmxlJwogICBpZD0ncGF0aDQ1JyAvPgo8cGF0aAogICBkPSdtIDE0LDEgaCAxIHYgMSBoIC0xIHonCiAgIHN0eWxlPSdtYXJrZXI6bm9uZScKICAgb3ZlcmZsb3c9J3Zpc2libGUnCiAgIGlkPSdwYXRoNDcnIC8+CjxwYXRoCiAgIGQ9J20gMTMuOTg0LDAuOTkgYSAxLDEgMCAwIDAgLTAuNjksMC4zMDEgbCAtNC41LDQuNDY5IGEgMS4wMDAyMDU2LDEuMDAwMjA1NiAwIDEgMCAxLjQxMSwxLjQxOCBsIDQuNSwtNC40NjkgQSAxLDEgMCAwIDAgMTMuOTg1LDAuOTkgWicKICAgc3R5bGU9J2xpbmUtaGVpZ2h0Om5vcm1hbDtmb250LXZhcmlhbnQtbGlnYXR1cmVzOm5vcm1hbDtmb250LXZhcmlhbnQtcG9zaXRpb246bm9ybWFsO2ZvbnQtdmFyaWFudC1jYXBzOm5vcm1hbDtmb250LXZhcmlhbnQtbnVtZXJpYzpub3JtYWw7Zm9udC12YXJpYW50LWFsdGVybmF0ZXM6bm9ybWFsO2ZvbnQtZmVhdHVyZS1zZXR0aW5nczpub3JtYWw7dGV4dC1pbmRlbnQ6MDt0ZXh0LWFsaWduOnN0YXJ0O3RleHQtZGVjb3JhdGlvbi1saW5lOm5vbmU7dGV4dC1kZWNvcmF0aW9uLXN0eWxlOnNvbGlkO3RleHQtdHJhbnNmb3JtOm5vbmU7dGV4dC1vcmllbnRhdGlvbjptaXhlZDtzaGFwZS1wYWRkaW5nOjA7aXNvbGF0aW9uOmF1dG87bWl4LWJsZW5kLW1vZGU6bm9ybWFsO21hcmtlcjpub25lJwogICBmb250LXdlaWdodD0nNDAwJwogICBmb250LWZhbWlseT0nc2Fucy1zZXJpZicKICAgd2hpdGUtc3BhY2U9J25vcm1hbCcKICAgb3ZlcmZsb3c9J3Zpc2libGUnCiAgIGlkPSdwYXRoNDknIC8+CjxwYXRoCiAgIGQ9J20gMSw5IGggMSB2IDEgSCAxIFogbSA1LDUgaCAxIHYgMSBIIDYgWiBtIDgsLTggaCAxIFYgNyBIIDE0IFogTSA5LDEgaCAxIFYgMiBIIDkgWicKICAgc3R5bGU9J21hcmtlcjpub25lJwogICBvdmVyZmxvdz0ndmlzaWJsZScKICAgaWQ9J3BhdGg1MScvPgo8L2c+PC9zdmc+Cg=="_s},
    {"VolumeMuted"_s , "PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHdpZHRoPScxNicgaGVpZ2h0PScxNic+IFwKICAgICAgICAgIDxnIGZpbGw9JyUyM0ZGRkZGRic+IFwKICAgICAgICAgICAgPHBhdGggZD0nTTExIDExaDEuMzc1bDEuMTI1IDEuMDk0TDE0LjU5NCAxMUgxNnYxLjQ2OWwtMS4wOTQgMS4wNjJMMTYgMTQuNTk0VjE2aC0xLjQzOEwxMy41IDE0LjkzNyAxMi40MzcgMTZIMTF2LTEuNDA2bDEuMDYyLTEuMDYzTDExIDEyLjQ3ek0wIDVoMi40ODRsMi45Ny0zSDZ2MTJoLS40NzVsLTMuMDQtM0gweicgc3R5bGU9J21hcmtlcjpub25lJyBjb2xvcj0nJTIzYmViZWJlJyBvdmVyZmxvdz0ndmlzaWJsZScvPiBcCiAgICAgICAgICAgIDxwYXRoIGQ9J00xMSAxdjEuNDhDMTIuMjY1IDQgMTMgNS43IDEzIDhjMCAuNzIzLS4wODUgMS4zODItLjIyOSAyaDIuMDM0Yy4xMjQtLjY0NS4xOTUtMS4zMTQuMTk1LTIgMC0yLjgxNC0xLTUuMTcyLTIuNTg2LTd6JyBzdHlsZT0nbWFya2VyOm5vbmUnIGNvbG9yPSclMjMwMDAnIG92ZXJmbG93PSd2aXNpYmxlJyBvcGFjaXR5PScuMzUnLz4gXAogICAgICAgICAgICA8cGF0aCBkPSdNOSAzdjJjLjYwNy43ODkgMSAxLjc1OSAxIDNzLS4zOTMgMi4yMi0xIDN2Mmgxdi0zaDEuNzVjLjE1OC0uNjI2LjI1LTEuMjk3LjI1LTIgMC0yLjE2Ny0uNzM5LTQuMDItMi01eicgc3R5bGU9J21hcmtlcjpub25lJyBjb2xvcj0nJTIzMDAwJyBvdmVyZmxvdz0ndmlzaWJsZScgb3BhY2l0eT0nLjM1Jy8+IFwKICAgICAgICAgICAgPHBhdGggZD0nTTkgOGMwLTEuMjU3LS4zMTItMi4yMTYtMS0zSDd2NmgxYy42NzItLjgzNyAxLTEuNzQyIDEtM3onIHN0eWxlPSdsaW5lLWhlaWdodDpub3JtYWw7LWlua3NjYXBlLWZvbnQtc3BlY2lmaWNhdGlvbjpTYW5zO3RleHQtaW5kZW50OjA7dGV4dC1hbGlnbjpzdGFydDt0ZXh0LWRlY29yYXRpb24tbGluZTpub25lO3RleHQtdHJhbnNmb3JtOm5vbmU7bWFya2VyOm5vbmUnIGNvbG9yPSclMjMwMDAnIGZvbnQtd2VpZ2h0PSc0MDAnIGZvbnQtZmFtaWx5PSdTYW5zJyBvdmVyZmxvdz0ndmlzaWJsZScgb3BhY2l0eT0nLjM1Jy8+IFwKICAgICAgICAgIDwvZz4gXAogICAgICAgIDwvc3ZnPg=="_s},
    {"VolumeMuted-RTL"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgd2lkdGg9IjE2IgogICBoZWlnaHQ9IjE2IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmcxMiIKICAgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIgogICB4bWxuczpzdmc9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48ZGVmcwogICBpZD0iZGVmczE2IiAvPgo8ZwogICBmaWxsPSIlMjNGRkZGRkYiCiAgIGlkPSJnMTAiCiAgIHRyYW5zZm9ybT0ibWF0cml4KC0xLDAsMCwxLDE2LDApIj4gXAogICAgICAgICAgICA8cGF0aAogICBkPSJtIDExLDExIGggMS4zNzUgTCAxMy41LDEyLjA5NCAxNC41OTQsMTEgSCAxNiB2IDEuNDY5IEwgMTQuOTA2LDEzLjUzMSAxNiwxNC41OTQgViAxNiBIIDE0LjU2MiBMIDEzLjUsMTQuOTM3IDEyLjQzNywxNiBIIDExIFYgMTQuNTk0IEwgMTIuMDYyLDEzLjUzMSAxMSwxMi40NyBaIE0gMCw1IEggMi40ODQgTCA1LjQ1NCwyIEggNiBWIDE0IEggNS41MjUgTCAyLjQ4NSwxMSBIIDAgWiIKICAgc3R5bGU9Im1hcmtlcjpub25lIgogICBjb2xvcj0iJTIzYmViZWJlIgogICBvdmVyZmxvdz0idmlzaWJsZSIKICAgaWQ9InBhdGgyIiAvPgogXAogICAgICAgICAgICA8cGF0aAogICBkPSJNIDExLDEgViAyLjQ4IEMgMTIuMjY1LDQgMTMsNS43IDEzLDggYyAwLDAuNzIzIC0wLjA4NSwxLjM4MiAtMC4yMjksMiBoIDIuMDM0IEMgMTQuOTI5LDkuMzU1IDE1LDguNjg2IDE1LDggMTUsNS4xODYgMTQsMi44MjggMTIuNDE0LDEgWiIKICAgc3R5bGU9Im1hcmtlcjpub25lIgogICBjb2xvcj0iJTIzMDAwIgogICBvdmVyZmxvdz0idmlzaWJsZSIKICAgb3BhY2l0eT0iMC4zNSIKICAgaWQ9InBhdGg0IiAvPgogXAogICAgICAgICAgICA8cGF0aAogICBkPSJtIDksMyB2IDIgYyAwLjYwNywwLjc4OSAxLDEuNzU5IDEsMyAwLDEuMjQxIC0wLjM5MywyLjIyIC0xLDMgdiAyIGggMSB2IC0zIGggMS43NSBDIDExLjkwOCw5LjM3NCAxMiw4LjcwMyAxMiw4IDEyLDUuODMzIDExLjI2MSwzLjk4IDEwLDMgWiIKICAgc3R5bGU9Im1hcmtlcjpub25lIgogICBjb2xvcj0iJTIzMDAwIgogICBvdmVyZmxvdz0idmlzaWJsZSIKICAgb3BhY2l0eT0iMC4zNSIKICAgaWQ9InBhdGg2IiAvPgogXAogICAgICAgICAgICA8cGF0aAogICBkPSJNIDksOCBDIDksNi43NDMgOC42ODgsNS43ODQgOCw1IEggNyB2IDYgSCA4IEMgOC42NzIsMTAuMTYzIDksOS4yNTggOSw4IFoiCiAgIHN0eWxlPSJsaW5lLWhlaWdodDpub3JtYWw7LWlua3NjYXBlLWZvbnQtc3BlY2lmaWNhdGlvbjpTYW5zO3RleHQtaW5kZW50OjA7dGV4dC1hbGlnbjpzdGFydDt0ZXh0LWRlY29yYXRpb24tbGluZTpub25lO3RleHQtdHJhbnNmb3JtOm5vbmU7bWFya2VyOm5vbmUiCiAgIGNvbG9yPSIlMjMwMDAiCiAgIGZvbnQtd2VpZ2h0PSI0MDAiCiAgIGZvbnQtZmFtaWx5PSJTYW5zIgogICBvdmVyZmxvdz0idmlzaWJsZSIKICAgb3BhY2l0eT0iMC4zNSIKICAgaWQ9InBhdGg4IiAvPgogXAogICAgICAgICAgPC9nPgogXAogICAgICAgIDwvc3ZnPgo="_s},
    {"Overflow"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgd2lkdGg9IjE2cHgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8cGF0aCBkPSJtIDcuNzUgMCBjIC0wLjQxNDA2MiAwIC0wLjc1IDAuMzM1OTM4IC0wLjc1IDAuNzUgdiAxLjMzMjAzMSBjIC0wLjU4OTg0NCAwLjEwMTU2MyAtMS4xNDg0MzggMC4yODkwNjMgLTEuNjY3OTY5IDAuNTQ2ODc1IGwgLTAuNzg1MTU2IC0xLjA4MjAzMSBjIC0wLjE1MjM0NCAtMC4yMTA5MzcgLTAuMzkwNjI1IC0wLjMxNjQwNiAtMC42MzI4MTMgLTAuMzA4NTk0IGMgLTAuMTQ0NTMxIDAuMDAzOTA3IC0wLjI4OTA2MiAwLjA1MDc4MSAtMC40MTQwNjIgMC4xNDQ1MzEgbCAtMC40MDYyNSAwLjI5Mjk2OSBjIC0wLjMzNTkzOCAwLjI0NjA5NCAtMC40MTAxNTYgMC43MTA5MzggLTAuMTY0MDYyIDEuMDQ2ODc1IGwgMC43ODkwNjIgMS4wODIwMzIgYyAtMC40MTAxNTYgMC40MTc5NjggLTAuNzU3ODEyIDAuODk0NTMxIC0xLjAzMTI1IDEuNDE3OTY4IGwgLTEuMjczNDM4IC0wLjQxNDA2MiBjIC0wLjA3NDIxOCAtMC4wMjM0MzggLTAuMTQ4NDM3IC0wLjAzNTE1NiAtMC4yMjI2NTYgLTAuMDM1MTU2IGMgLTAuMzIwMzEyIC0wLjAwNzgxMyAtMC42MTcxODcgMC4xOTUzMTIgLTAuNzIyNjU2IDAuNTE5NTMxIGwgLTAuMTU2MjUgMC40NzI2NTYgYyAtMC4xMjUgMC4zOTQ1MzEgMC4wODU5MzggMC44MTY0MDYgMC40ODA0NjkgMC45NDUzMTMgbCAxLjI2OTUzMSAwLjQxNDA2MiBjIC0wLjAzOTA2MiAwLjI4NTE1NiAtMC4wNjI1IDAuNTc4MTI1IC0wLjA2MjUgMC44NzUgcyAwLjAyMzQzOCAwLjU4OTg0NCAwLjA2MjUgMC44Nzg5MDYgbCAtMS4yNjk1MzEgMC40MTAxNTYgYyAtMC4zOTQ1MzEgMC4xMjg5MDcgLTAuNjA1NDY5IDAuNTUwNzgyIC0wLjQ4MDQ2OSAwLjk0NTMxMyBsIDAuMTU2MjUgMC40NzY1NjMgYyAwLjEyODkwNiAwLjM5NDUzMSAwLjU1MDc4MSAwLjYwOTM3NCAwLjk0NTMxMiAwLjQ4MDQ2OCBsIDEuMjczNDM4IC0wLjQxNDA2MiBjIDAuMjczNDM4IDAuNTE5NTMxIDAuNjIxMDk0IDEgMS4wMzEyNSAxLjQxNzk2OCBsIC0wLjc4OTA2MiAxLjA4MjAzMiBjIC0wLjI0NjA5NCAwLjMzNTkzNyAtMC4xNzE4NzYgMC44MDA3ODEgMC4xNjQwNjIgMS4wNDY4NzUgbCAwLjQwNjI1IDAuMjk2ODc1IGMgMC4zMzU5MzggMC4yNDIxODcgMC44MDA3ODEgMC4xNjc5NjggMS4wNDY4NzUgLTAuMTY3OTY5IGwgMC43ODUxNTYgLTEuMDgyMDMxIGMgMC41MTk1MzEgMC4yNTc4MTIgMS4wNzgxMjUgMC40NDE0MDYgMS42Njc5NjkgMC41NDI5NjggdiAxLjMzNTkzOCBjIDAgMC40MTQwNjIgMC4zMzU5MzggMC43NSAwLjc1IDAuNzUgaCAwLjUgYyAwLjQxNDA2MiAwIDAuNzUgLTAuMzM1OTM4IDAuNzUgLTAuNzUgdiAtMS4zMzU5MzggYyAwLjU4OTg0NCAtMC4xMDE1NjIgMS4xNDg0MzggLTAuMjg1MTU2IDEuNjY3OTY5IC0wLjU0Mjk2OCBsIDAuNzg1MTU2IDEuMDgyMDMxIGMgMC4yNDYwOTQgMC4zMzU5MzcgMC43MTA5MzcgMC40MTAxNTYgMS4wNDY4NzUgMC4xNjc5NjkgbCAwLjQwNjI1IC0wLjI5Mjk2OSBjIDAuMzM1OTM4IC0wLjI0NjA5NCAwLjQxMDE1NiAtMC43MTQ4NDQgMC4xNjQwNjIgLTEuMDUwNzgxIGwgLTAuNzg5MDYyIC0xLjA4MjAzMiBjIDAuNDEwMTU2IC0wLjQxNzk2OCAwLjc1NzgxMiAtMC44OTg0MzcgMS4wMzEyNSAtMS40MTc5NjggbCAxLjI3MzQzOCAwLjQxNDA2MiBjIDAuMzk0NTMxIDAuMTI4OTA2IDAuODE2NDA2IC0wLjA4NTkzNyAwLjk0NTMxMiAtMC40ODA0NjggbCAwLjE1NjI1IC0wLjQ3NjU2MyBjIDAuMTI1IC0wLjM5NDUzMSAtMC4wODU5MzggLTAuODE2NDA2IC0wLjQ4MDQ2OSAtMC45NDUzMTMgbCAtMS4yNzM0MzcgLTAuNDEwMTU2IGMgMC4wNDI5NjggLTAuMjg5MDYyIDAuMDY2NDA2IC0wLjU4MjAzMSAwLjA2NjQwNiAtMC44Nzg5MDYgcyAtMC4wMjM0MzggLTAuNTg5ODQ0IC0wLjA2NjQwNiAtMC44NzUgbCAxLjI3MzQzNyAtMC40MTQwNjIgYyAwLjM5NDUzMSAtMC4xMjg5MDcgMC42MDU0NjkgLTAuNTUwNzgyIDAuNDgwNDY5IC0wLjk0NTMxMyBsIC0wLjE1NjI1IC0wLjQ3MjY1NiBjIC0wLjEwNTQ2OSAtMC4zMjQyMTkgLTAuNDAyMzQ0IC0wLjUyNzM0NCAtMC43MjI2NTYgLTAuNTE5NTMxIGMgLTAuMDc0MjE5IDAgLTAuMTQ4NDM4IDAuMDExNzE4IC0wLjIyMjY1NiAwLjAzNTE1NiBsIC0xLjI3MzQzOCAwLjQxNDA2MiBjIC0wLjI3MzQzOCAtMC41MjM0MzcgLTAuNjIxMDk0IC0xIC0xLjAzMTI1IC0xLjQxNzk2OCBsIDAuNzg5MDYyIC0xLjA4MjAzMiBjIDAuMjQ2MDk0IC0wLjMzNTkzNyAwLjE3MTg3NiAtMC44MDA3ODEgLTAuMTY0MDYyIC0xLjA0Njg3NSBsIC0wLjQwNjI1IC0wLjI5Mjk2OSBjIC0wLjE0ODQzOCAtMC4xMDkzNzQgLTAuMzIwMzEyIC0wLjE1NjI1IC0wLjQ4ODI4MSAtMC4xNDQ1MzEgYyAtMC4yMTQ4NDQgMC4wMTE3MTkgLTAuNDIxODc1IDAuMTIxMDk0IC0wLjU1ODU5NCAwLjMwODU5NCBsIC0wLjc4NTE1NiAxLjA4NTkzNyBjIC0wLjUxOTUzMSAtMC4yNjE3MTggLTEuMDc4MTI1IC0wLjQ0NTMxMiAtMS42Njc5NjkgLTAuNTQ2ODc0IHYgLTEuMzM1OTM4IGMgMCAtMC40MTQwNjIgLTAuMzM1OTM4IC0wLjc1IC0wLjc1IC0wLjc1IHogbSAwLjI1IDQgYyAwLjgwODU5NCAwIDEuNTU4NTk0IDAuMjM0Mzc1IDIuMTgzNTk0IDAuNjQ0NTMxIGwgMC4zMjQyMTggMC4yMzA0NjkgYyAwLjU1ODU5NCAwLjQ0OTIxOSAwLjk5MjE4OCAxLjA0Mjk2OSAxLjI0MjE4OCAxLjczMDQ2OSBjIDAuMDAzOTA2IDAuMDAzOTA2IDAuMDA3ODEyIDAuMDA3ODEyIDAuMDA3ODEyIDAuMDE1NjI1IGwgMC4wODU5MzggMC4yNjE3MTggYyAwLjEwMTU2MiAwLjM1NTQ2OSAwLjE1NjI1IDAuNzMwNDY5IDAuMTU2MjUgMS4xMTcxODggcyAtMC4wNTQ2ODggMC43NjE3MTkgLTAuMTU2MjUgMS4xMTcxODggbCAtMC4wODU5MzggMC4yNjE3MTggdiAwLjAwMzkwNiBjIC0wLjI1MzkwNiAwLjY5MTQwNyAtMC42ODc1IDEuMjg5MDYzIC0xLjI1IDEuNzQyMTg4IGwgLTAuMzI0MjE4IDAuMjM0Mzc1IGMgLTAuNjI1IDAuNDA2MjUgLTEuMzc1IDAuNjQwNjI1IC0yLjE4MzU5NCAwLjY0MDYyNSBjIC0wLjgwNDY4OCAwIC0xLjU1NDY4OCAtMC4yMzQzNzUgLTIuMTc5Njg4IC0wLjYzNjcxOSBsIC0wLjMyODEyNCAtMC4yMzgyODEgYyAtMC41NTQ2ODggLTAuNDQ1MzEyIC0wLjk4ODI4MiAtMS4wMzUxNTYgLTEuMjM4MjgyIC0xLjcxNDg0NCBsIC0wLjEwNTQ2OCAtMC4zMTY0MDYgYyAtMC4wOTc2NTcgLTAuMzQ3NjU2IC0wLjE0ODQzOCAtMC43MTQ4NDQgLTAuMTQ4NDM4IC0xLjA5Mzc1IHMgMC4wNTA3ODEgLTAuNzQ2MDk0IDAuMTQ4NDM4IC0xLjA4OTg0NCBsIDAuMTA5Mzc0IC0wLjMyODEyNSBjIDAuMjUgLTAuNjc1NzgxIDAuNjgzNTk0IC0xLjI2MTcxOSAxLjIzNDM3NiAtMS43MDMxMjUgbCAwLjMyODEyNCAtMC4yMzgyODEgYyAwLjYyNSAtMC40MDYyNSAxLjM3NSAtMC42NDA2MjUgMi4xNzk2ODggLTAuNjQwNjI1IHogbSAwIDAiIGZpbGw9IiMyZTM0MzYiLz4KPC9zdmc+Cg=="_s},
    {"invalid-placard@2x"_s , "iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAYAAAA7MK6iAAAACXBIWXMAAB2HAAAdhwGP5fFlAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAAptJREFUSIm9171PFEEYx/Hvs7wUYED/DA3uYdi7RUsTY2OrnYnBaGKwIdpJZWejFr5giHbaWBvFGLEx3suC3Hpo0MJGE6OFRGNC4G5/Ft7hCVxudzl8utmZ2U92njwzsyaJ4ezomNChMCielRTxH8Iynj8mNA04oF+YrQIgvmB2pyeq3gqCYK3TsCMx+gcFsH7EHsQeYC/S9TW6Fl3PP95p2ABzs7mbiHNtRj6PajbxZi4fdgSWhJnFwyEC7rPafbFcfvl12zCAmVkmm6tI7Isxb1lwuVe1G2nzvw7Xccf1cmVgKNZs8UFwKQwKD7cFp8IhVf43wanxhPl3mhtDvp8BkBSFQXEY7F0C2AFO0ltdcrP+hOd5PbHhrhqPMiOjo3W8FgaFIaCSAAfYbXC1Xf3/s9SZrP9DIIvsaHkun4fUy94kbJ1/Z/M4BnD0JOMd9OHvspvZ21SwOOw4mhvO5m54ntfXEq7HIBbNNOG1cqngpsahW9j4mnXdbgfvBA5wwsycdvBO4cSBO4oLHjTO+zhwJ/A1wdWfu/rGGw/iwgCDsuhpc52XS4X9tK1ze2amA2GpcOHj7OxKGhiDATmaacKjMChmWuBLjjhWLuWPLBSLixs7E8FJcEmvFuaKj1u9JzHcwLfYZDLCXqyPMTu138tNN8qnI3A9NuY8ehMUDmOsbxIGY63w7cAYfKYr6m+0JSksFcfj4N0pxe+KuBKt/LpWqVRWm7skyczG3WyOxh2ujmNmZxp1nBSuGrrnVKuT8/Pz31oNioMngO0Z1CYWSqVY53M7PA78XmIyDPKJL3R1/Lzr5XqB0w3cHfFrG+FljGp91ieTTXdTm9rOL4ykyMzOuNncav3LBdHrLS97OxFm5rgj/hSmQrlUuPsby7elIcz3PMUAAAAASUVORK5CYII="_s},
    {"SkipBack15"_s , "PHN2ZyBoZWlnaHQ9IjE2IiB3aWR0aD0iMTYiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8cGF0aCBzdHlsZT0iZmlsbDojMmUzNDM2IiBkPSJtNS4xMTUgOC45OC0uMTU0IDMuNDQyLjEzMy0uMDI2YTYuNDkgNi40OSAwIDAgMSAxLjE4LS4xMjVjLjQ1MiAwIC44LjA5IDEuMDI5LjI0My4yMjguMTUyLjM0Mi4zNi4zNDIuNjQ0IDAgLjI1OC0uMDk1LjQ0My0uMjc4LjU3OC0uMTgzLjEzNS0uNDYzLjIxNS0uODI2LjIxNWEzLjMyOCAzLjMyOCAwIDAgMS0xLjQzNy0uMzM0bC0uMTA4LS4wNTMtLjM3MyAxLjA0NS4wODIuMDQzYy41Ni4yOTMgMS4xNjUuNDUgMS44MjguNDUuNzI0IDAgMS4zMzMtLjE5MyAxLjc2Ni0uNTUuNDMyLS4zNTUuNjgxLS44NzYuNjgxLTEuNTAzIDAtLjU3NS0uMjExLTEuMDUtLjYwMy0xLjM3MS0uNzYxLS40NzgtMS4zMzMtLjQ2OC0yLjA3OC0uNDE4bC4wNDktMS4xM2gyLjMzOFY4Ljk4Wk04LjI1IDFhLjc5OC43OTggMCAwIDAtLjM3NS4xMTdsLTMuNzUgMi4yNUEuNzQ4Ljc0OCAwIDAgMCAzLjc5NyA0YzAgLjI1LjExNy41LjMyOC42MzdsMy43NSAyLjI1Yy4xMTMuMDcuMjQyLjExLjM3NS4xMTNIOVY1YTMuOTk1IDMuOTk1IDAgMCAxIDMuODE2IDIuOCAzLjk5NCAzLjk5NCAwIDAgMS0xLjUyMyA0LjQ4MSAxLjAwOCAxLjAwOCAwIDAgMC0uNDMuOTA2IDEuMDAxIDEuMDAxIDAgMCAwIDEuNTc4LjczMSA2LjAxMyA2LjAxMyAwIDAgMCAyLjI4Ni02LjcxOUE2LjAxNyA2LjAxNyAwIDAgMCA5IDNWMVpNMi4yODEgOC45ODhsLTIuMDcgMS4xOTYuNTYyIDEuMDc0LjkwMy0uNDY5djIuOTNILjUxNnYxLjI4NWgzLjcyNnYtMS4yODVIMy4xNzZ2LTQuNzNaIi8+Cjwvc3ZnPgo="_s},
    {"SkipForward15"_s , "PHN2ZyBoZWlnaHQ9IjE2IiB3aWR0aD0iMTYiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8cGF0aCBzdHlsZT0iZmlsbDojMmUzNDM2IiBkPSJtMTEuMTI3IDguOTg0LS4xNTQgMy40NDIuMTMzLS4wMjZhNi40OSA2LjQ5IDAgMCAxIDEuMTgtLjEyNWMuNDUyIDAgLjguMDkgMS4wMjkuMjQzLjIyOC4xNTIuMzQyLjM2LjM0Mi42NDQgMCAuMjU4LS4wOTUuNDQzLS4yNzguNTc4LS4xODMuMTM1LS40NjMuMjE1LS44MjYuMjE1YTMuMzI4IDMuMzI4IDAgMCAxLTEuNDM3LS4zMzRsLS4xMDgtLjA1My0uMzczIDEuMDQ1LjA4Mi4wNDNjLjU2LjI5MyAxLjE2NS40NSAxLjgyOC40NS43MjQgMCAxLjMzMy0uMTkzIDEuNzY2LS41NS40MzItLjM1NS42ODEtLjg3Ni42ODEtMS41MDMgMC0uNTc1LS4yMTEtMS4wNS0uNjAzLTEuMzcxLS43NjEtLjQ3OC0xLjMzMy0uNDY4LTIuMDc4LS40MThsLjA0OS0xLjEzaDIuMzM4di0xLjE1Wk03IDF2MmE2LjAwOCA2LjAwOCAwIDAgMC01LjcyMyA0LjIgNi4wMDUgNi4wMDUgMCAwIDAgMi4yODIgNi43MTggMSAxIDAgMSAwIDEuMTQ4LTEuNjM3IDMuOTk0IDMuOTk0IDAgMCAxLTEuNTIzLTQuNDhBMy45OTUgMy45OTUgMCAwIDEgNyA1djJoLjc1YS43NTYuNzU2IDAgMCAwIC4zNzUtLjExN2wzLjc1LTIuMjVBLjc0My43NDMgMCAwIDAgMTIuMjAzIDRhLjc0My43NDMgMCAwIDAtLjMyOC0uNjMzbC0zLjc1LTIuMjVBLjc1Ni43NTYgMCAwIDAgNy43NSAxWm0xLjI4MSA3Ljk4NC0yLjA3IDEuMTk2LjU2MiAxLjA3NC45MDMtLjQ2OXYyLjkzNGgtMS4xNlYxNWgzLjcyNnYtMS4yODFIOS4xNzZWOC45ODRaIi8+Cjwvc3ZnPgo="_s},
    {"Volume0"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgaGVpZ2h0PSIxNnB4IgogICB2aWV3Qm94PSIwIDAgMTYgMTYiCiAgIHdpZHRoPSIxNnB4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc0IgogICB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciCiAgIHhtbG5zOnN2Zz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgogIDxkZWZzCiAgICAgaWQ9ImRlZnM4IiAvPgogIDxwYXRoCiAgICAgZD0iTSA3LDEuMDA3ODEyIEMgNi43MDMxMjUsMS4wMDM5MDYgNi40MjE4NzUsMS4xMzI4MTIgNi4yMzA0NjksMS4zNTkzNzUgTCAzLDUgSCAyIEMgMC45MDYyNSw1IDAsNS44NDM3NSAwLDcgdiAyIGMgMCwxLjA4OTg0NCAwLjkxMDE1NiwyIDIsMiBoIDEgbCAzLjIzMDQ2OSwzLjY0MDYyNSBDIDYuNDQxNDA2LDE0Ljg5NDUzMSA2LjcyMjY1NiwxNS4wMDM5MDYgNywxNSBaIgogICAgIGZpbGw9IiMyZTM0MzYiCiAgICAgaWQ9InBhdGgyIiAvPgo8L3N2Zz4K"_s},
    {"Volume1"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgd2lkdGg9IjE2cHgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8cGF0aCBkPSJtIDcgMS4wMDc4MTIgYyAtMC4yOTY4NzUgLTAuMDAzOTA2IC0wLjU3ODEyNSAwLjEyNSAtMC43Njk1MzEgMC4zNTE1NjMgbCAtMy4yMzA0NjkgMy42NDA2MjUgaCAtMSBjIC0xLjA5Mzc1IDAgLTIgMC44NDM3NSAtMiAyIHYgMiBjIDAgMS4wODk4NDQgMC45MTAxNTYgMiAyIDIgaCAxIGwgMy4yMzA0NjkgMy42NDA2MjUgYyAwLjIxMDkzNyAwLjI1MzkwNiAwLjQ5MjE4NyAwLjM2MzI4MSAwLjc2OTUzMSAwLjM1OTM3NSB6IG0gMi45NTcwMzEgMi45ODA0NjkgYyAtMC4xOTkyMTkgMC4wMTE3MTkgLTAuMzk0NTMxIDAuMDc0MjE5IC0wLjU2MjUgMC4yMDMxMjUgYyAtMC40NDE0MDYgMC4zMzIwMzIgLTAuNTMxMjUgMC45NjA5MzggLTAuMTk1MzEyIDEuNDAyMzQ0IGMgMS4wNzQyMTkgMS40MjU3ODEgMS4wNzQyMTkgMy4zODY3MTkgMCA0LjgxMjUgYyAtMC4zMzU5MzggMC40NDE0MDYgLTAuMjQ2MDk0IDEuMDcwMzEyIDAuMTk1MzEyIDEuNDAyMzQ0IGMgMC40NDE0MDcgMC4zMzIwMzEgMS4wNjY0MDcgMC4yNDIxODcgMS4zOTg0MzggLTAuMTk1MzEzIGMgMC44MDQ2ODcgLTEuMDcwMzEyIDEuMjA3MDMxIC0yLjMzOTg0MyAxLjIwNzAzMSAtMy42MTMyODEgcyAtMC40MDIzNDQgLTIuNTQyOTY5IC0xLjIwNzAzMSAtMy42MTMyODEgYyAtMC4xODM1OTQgLTAuMjQ2MDk0IC0wLjQ2NDg0NCAtMC4zODI4MTMgLTAuNzUzOTA3IC0wLjM5ODQzOCBjIC0wLjAyNzM0MyAwIC0wLjA1NDY4NyAwIC0wLjA4NTkzNyAwIHogbSAwIDAiIGZpbGw9IiMyZTM0MzYiLz4KPC9zdmc+Cg=="_s},
    {"Volume2"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgd2lkdGg9IjE2cHgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8ZyBmaWxsPSIjMmUzNDM2Ij4KICAgICAgICA8cGF0aCBkPSJtIDcgMS4wMDc4MTIgYyAtMC4yOTY4NzUgLTAuMDAzOTA2IC0wLjU3ODEyNSAwLjEyNSAtMC43Njk1MzEgMC4zNTE1NjMgbCAtMy4yMzA0NjkgMy42NDA2MjUgaCAtMSBjIC0xLjA5Mzc1IDAgLTIgMC44NDM3NSAtMiAyIHYgMiBjIDAgMS4wODk4NDQgMC45MTAxNTYgMiAyIDIgaCAxIGwgMy4yMzA0NjkgMy42NDA2MjUgYyAwLjIxMDkzNyAwLjI1MzkwNiAwLjQ5MjE4NyAwLjM2MzI4MSAwLjc2OTUzMSAwLjM1OTM3NSB6IG0gMy4wMzkwNjIgMi45ODA0NjkgYyAtMC4yMjI2NTYgLTAuMDA3ODEyIC0wLjQ1MzEyNCAwLjA1ODU5NCAtMC42NDQ1MzEgMC4yMDMxMjUgYyAtMC4yNjE3MTkgMC4xOTkyMTkgLTAuMzk0NTMxIDAuNSAtMC4zOTQ1MzEgMC44MDQ2ODggdiAwLjA2NjQwNiBjIDAuMDExNzE5IDAuMTg3NSAwLjA3ODEyNSAwLjM3MTA5NCAwLjE5OTIxOSAwLjUyNzM0NCBjIDEuMDc0MjE5IDEuNDI5Njg3IDEuMDc0MjE5IDMuMzkwNjI1IDAgNC44MTY0MDYgYyAtMC4xMjEwOTQgMC4xNjAxNTYgLTAuMTg3NSAwLjM0Mzc1IC0wLjE5OTIxOSAwLjUzMTI1IHYgMC4wNjY0MDYgYyAwIDAuMzA0Njg4IDAuMTMyODEyIDAuNjA1NDY5IDAuMzk0NTMxIDAuODA0Njg4IGMgMC40NDE0MDcgMC4zMzIwMzEgMS4wNjY0MDcgMC4yNDIxODcgMS4zOTg0MzggLTAuMTk5MjE5IGMgMC44MDQ2ODcgLTEuMDY2NDA2IDEuMjA3MDMxIC0yLjMzNTkzNyAxLjIwNzAzMSAtMy42MDkzNzUgcyAtMC40MDIzNDQgLTIuNTQyOTY5IC0xLjIwNzAzMSAtMy42MTMyODEgYyAtMC4xODM1OTQgLTAuMjQ2MDk0IC0wLjQ2NDg0NCAtMC4zODI4MTMgLTAuNzUzOTA3IC0wLjM5ODQzOCB6IG0gMCAwIi8+CiAgICAgICAgPHBhdGggZD0ibSAxMy40NjA5MzggMS45Njg3NSBjIC0wLjE5MTQwNyAtMC4wMDM5MDYgLTAuMzg2NzE5IDAuMDU0Njg4IC0wLjU1ODU5NCAwLjE2Nzk2OSBjIC0wLjQ1NzAzMiAwLjMxMjUgLTAuNTc4MTI1IDAuOTMzNTkzIC0wLjI2OTUzMiAxLjM5MDYyNSBjIDEuODI0MjE5IDIuNzA3MDMxIDEuODI0MjE5IDYuMjM4MjgxIDAgOC45NDUzMTIgYyAtMC4zMDg1OTMgMC40NTcwMzIgLTAuMTg3NSAxLjA3ODEyNSAwLjI2OTUzMiAxLjM5MDYyNSBjIDAuNDU3MDMxIDAuMzA4NTk0IDEuMDc4MTI1IDAuMTg3NSAxLjM5MDYyNSAtMC4yNjk1MzEgYyAxLjEzNjcxOSAtMS42OTE0MDYgMS43MDcwMzEgLTMuNjQwNjI1IDEuNzA3MDMxIC01LjU5Mzc1IHMgLTAuNTcwMzEyIC0zLjkwMjM0NCAtMS43MDcwMzEgLTUuNTkzNzUgYyAtMC4xOTUzMTMgLTAuMjg1MTU2IC0wLjUxMTcxOSAtMC40Mzc1IC0wLjgzMjAzMSAtMC40Mzc1IHogbSAwIDAiIGZpbGwtb3BhY2l0eT0iMC4zNDkwMiIvPgogICAgPC9nPgo8L3N2Zz4K"_s},
    {"Volume3"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyBoZWlnaHQ9IjE2cHgiIHZpZXdCb3g9IjAgMCAxNiAxNiIgd2lkdGg9IjE2cHgiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgICA8cGF0aCBkPSJtIDcgMS4wMDc4MTIgYyAtMC4yOTY4NzUgLTAuMDAzOTA2IC0wLjU3ODEyNSAwLjEyNSAtMC43Njk1MzEgMC4zNTE1NjMgbCAtMy4yMzA0NjkgMy42NDA2MjUgaCAtMSBjIC0xLjA5Mzc1IDAgLTIgMC44NDM3NSAtMiAyIHYgMiBjIDAgMS4wODk4NDQgMC45MTAxNTYgMiAyIDIgaCAxIGwgMy4yMzA0NjkgMy42NDA2MjUgYyAwLjIxMDkzNyAwLjI1MzkwNiAwLjQ5MjE4NyAwLjM2MzI4MSAwLjc2OTUzMSAwLjM1OTM3NSB6IG0gNi40NjA5MzggMC45NjA5MzggYyAtMC4xOTE0MDcgLTAuMDAzOTA2IC0wLjM4NjcxOSAwLjA1NDY4OCAtMC41NTg1OTQgMC4xNjc5NjkgYyAtMC40NTcwMzIgMC4zMTI1IC0wLjU3ODEyNSAwLjkzMzU5MyAtMC4yNjk1MzIgMS4zOTA2MjUgYyAxLjgyNDIxOSAyLjcwNzAzMSAxLjgyNDIxOSA2LjIzODI4MSAwIDguOTQ1MzEyIGMgLTAuMzA4NTkzIDAuNDU3MDMyIC0wLjE4NzUgMS4wNzgxMjUgMC4yNjk1MzIgMS4zOTA2MjUgYyAwLjQ1NzAzMSAwLjMwODU5NCAxLjA3ODEyNSAwLjE4NzUgMS4zOTA2MjUgLTAuMjY5NTMxIGMgMS4xMzY3MTkgLTEuNjkxNDA2IDEuNzA3MDMxIC0zLjY0MDYyNSAxLjcwNzAzMSAtNS41OTM3NSBzIC0wLjU3MDMxMiAtMy45MDIzNDQgLTEuNzA3MDMxIC01LjU5Mzc1IGMgLTAuMTk1MzEzIC0wLjI4NTE1NiAtMC41MTE3MTkgLTAuNDM3NSAtMC44MzIwMzEgLTAuNDM3NSB6IG0gLTMuNDIxODc2IDIuMDE5NTMxIGMgLTAuMjIyNjU2IC0wLjAwNzgxMiAtMC40NTMxMjQgMC4wNTg1OTQgLTAuNjQ0NTMxIDAuMjAzMTI1IGMgLTAuMjYxNzE5IDAuMTk5MjE5IC0wLjM5NDUzMSAwLjUgLTAuMzk0NTMxIDAuODA0Njg4IHYgMC4wNTg1OTQgYyAwLjAxMTcxOSAwLjE5MTQwNiAwLjA3NDIxOSAwLjM3NSAwLjE5OTIxOSAwLjUzNTE1NiBjIDEuMDc0MjE5IDEuNDI5Njg3IDEuMDc0MjE5IDMuMzkwNjI1IDAgNC44MTY0MDYgYyAtMC4xMjUgMC4xNjQwNjIgLTAuMTg3NSAwLjM0NzY1NiAtMC4xOTkyMTkgMC41MzUxNTYgdiAwLjA2MjUgYyAwIDAuMzA0Njg4IDAuMTMyODEyIDAuNjA1NDY5IDAuMzk0NTMxIDAuODA0Njg4IGMgMC40NDE0MDcgMC4zMzIwMzEgMS4wNjY0MDcgMC4yNDIxODcgMS4zOTg0MzggLTAuMTk5MjE5IGMgMC44MDQ2ODcgLTEuMDY2NDA2IDEuMjA3MDMxIC0yLjMzNTkzNyAxLjIwNzAzMSAtMy42MDkzNzUgcyAtMC40MDIzNDQgLTIuNTQyOTY5IC0xLjIwNzAzMSAtMy42MTMyODEgYyAtMC4xODM1OTQgLTAuMjQ2MDk0IC0wLjQ2NDg0NCAtMC4zODI4MTMgLTAuNzUzOTA3IC0wLjM5ODQzOCB6IG0gMCAwIiBmaWxsPSIjMmUzNDM2Ii8+Cjwvc3ZnPgo="_s},
    {"Volume0-RTL"_s, "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgaGVpZ2h0PSIxNnB4IgogICB2aWV3Qm94PSIwIDAgMTYgMTYiCiAgIHdpZHRoPSIxNnB4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc0IgogICB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciCiAgIHhtbG5zOnN2Zz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgogIDxkZWZzCiAgICAgaWQ9ImRlZnM4IiAvPgogIDxwYXRoCiAgICAgZD0ibSA5LjAwNTUxNSwxLjA1ODY0MzEgYyAwLjI5Njg3NSwtMC4wMDM5IDAuNTc4MTI1LDAuMTI1IDAuNzY5NTMxLDAuMzUxNTYzIGwgMy4yMzA0NjksMy42NDA2MjUgaCAxIGMgMS4wOTM3NSwwIDIsMC44NDM3NSAyLDIgdiAyIGMgMCwxLjA4OTg0MzkgLTAuOTEwMTU2LDEuOTk5OTk5OSAtMiwxLjk5OTk5OTkgaCAtMSBsIC0zLjIzMDQ2OSwzLjY0MDYyNSBjIC0wLjIxMDkzNywwLjI1MzkwNiAtMC40OTIxODcsMC4zNjMyODEgLTAuNzY5NTMxLDAuMzU5Mzc1IHoiCiAgICAgZmlsbD0iIzJlMzQzNiIKICAgICBpZD0icGF0aDIiIC8+Cjwvc3ZnPgo="_s},
    {"Volume1-RTL"_s, "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgaGVpZ2h0PSIxNnB4IgogICB2aWV3Qm94PSIwIDAgMTYgMTYiCiAgIHdpZHRoPSIxNnB4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmcxODYiCiAgIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIKICAgeG1sbnM6c3ZnPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CiAgPGRlZnMKICAgICBpZD0iZGVmczE5MCIgLz4KICA8cGF0aAogICAgIGQ9Im0gOSwxLjAwNzgxMiBjIDAuMjk2ODc1LC0wLjAwMzkxIDAuNTc4MTI1LDAuMTI1IDAuNzY5NTMxLDAuMzUxNTYzIEwgMTMsNSBoIDEgYyAxLjA5Mzc1LDAgMiwwLjg0Mzc1IDIsMiB2IDIgYyAwLDEuMDg5ODQ0IC0wLjkxMDE1NiwyIC0yLDIgSCAxMyBMIDkuNzY5NTMxLDE0LjY0MDYyNSBDIDkuNTU4NTk0LDE0Ljg5NDUzMSA5LjI3NzM0NCwxNS4wMDM5MDYgOSwxNSBaIE0gNi4wNDI5NjksMy45ODgyODEgQyA2LjI0MjE4OCw0IDYuNDM3NSw0LjA2MjUgNi42MDU0NjksNC4xOTE0MDYgYyAwLjQ0MTQwNiwwLjMzMjAzMiAwLjUzMTI1LDAuOTYwOTM4IDAuMTk1MzEyLDEuNDAyMzQ0IC0xLjA3NDIxOSwxLjQyNTc4MSAtMS4wNzQyMTksMy4zODY3MTkgMCw0LjgxMjUgMC4zMzU5MzgsMC40NDE0MDYgMC4yNDYwOTQsMS4wNzAzMTIgLTAuMTk1MzEyLDEuNDAyMzQ0IEMgNi4xNjQwNjIsMTIuMTQwNjI1IDUuNTM5MDYyLDEyLjA1MDc4MSA1LjIwNzAzMSwxMS42MTMyODEgNC40MDIzNDQsMTAuNTQyOTY5IDQsOS4yNzM0MzggNCw4IDQsNi43MjY1NjIgNC40MDIzNDQsNS40NTcwMzEgNS4yMDcwMzEsNC4zODY3MTkgNS4zOTA2MjUsNC4xNDA2MjUgNS42NzE4NzUsNC4wMDM5MDYgNS45NjA5MzgsMy45ODgyODEgYyAwLjAyNzM0LDAgMC4wNTQ2OSwwIDAuMDg1OTQsMCB6IG0gMCwwIgogICAgIGZpbGw9IiMyZTM0MzYiCiAgICAgaWQ9InBhdGgxODQiIC8+Cjwvc3ZnPgo="_s},
    {"Volume2-RTL"_s, "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgaGVpZ2h0PSIxNnB4IgogICB2aWV3Qm94PSIwIDAgMTYgMTYiCiAgIHdpZHRoPSIxNnB4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc4IgogICB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciCiAgIHhtbG5zOnN2Zz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgogIDxkZWZzCiAgICAgaWQ9ImRlZnMxMiIgLz4KICA8ZwogICAgIGZpbGw9IiMyZTM0MzYiCiAgICAgaWQ9Imc2IgogICAgIHRyYW5zZm9ybT0ibWF0cml4KC0xLDAsMCwxLDE2LDApIj4KICAgIDxwYXRoCiAgICAgICBkPSJNIDcsMS4wMDc4MTIgQyA2LjcwMzEyNSwxLjAwMzkwNiA2LjQyMTg3NSwxLjEzMjgxMiA2LjIzMDQ2OSwxLjM1OTM3NSBMIDMsNSBIIDIgQyAwLjkwNjI1LDUgMCw1Ljg0Mzc1IDAsNyB2IDIgYyAwLDEuMDg5ODQ0IDAuOTEwMTU2LDIgMiwyIGggMSBsIDMuMjMwNDY5LDMuNjQwNjI1IEMgNi40NDE0MDYsMTQuODk0NTMxIDYuNzIyNjU2LDE1LjAwMzkwNiA3LDE1IFogbSAzLjAzOTA2MiwyLjk4MDQ2OSBDIDkuODE2NDA2LDMuOTgwNDY5IDkuNTg1OTM4LDQuMDQ2ODc1IDkuMzk0NTMxLDQuMTkxNDA2IDkuMTMyODEyLDQuMzkwNjI1IDksNC42OTE0MDYgOSw0Ljk5NjA5NCBWIDUuMDYyNSBjIDAuMDExNzE5LDAuMTg3NSAwLjA3ODEyNSwwLjM3MTA5NCAwLjE5OTIxOSwwLjUyNzM0NCAxLjA3NDIxOSwxLjQyOTY4NyAxLjA3NDIxOSwzLjM5MDYyNSAwLDQuODE2NDA2IEMgOS4wNzgxMjUsMTAuNTY2NDA2IDkuMDExNzE5LDEwLjc1IDksMTAuOTM3NSB2IDAuMDY2NDEgYyAwLDAuMzA0Njg4IDAuMTMyODEyLDAuNjA1NDY5IDAuMzk0NTMxLDAuODA0Njg4IDAuNDQxNDA3LDAuMzMyMDMxIDEuMDY2NDA3LDAuMjQyMTg3IDEuMzk4NDM4LC0wLjE5OTIxOSBDIDExLjU5NzY1NiwxMC41NDI5NjkgMTIsOS4yNzM0MzggMTIsOCAxMiw2LjcyNjU2MiAxMS41OTc2NTYsNS40NTcwMzEgMTAuNzkyOTY5LDQuMzg2NzE5IDEwLjYwOTM3NSw0LjE0MDYyNSAxMC4zMjgxMjUsNC4wMDM5MDYgMTAuMDM5MDYyLDMuOTg4MjgxIFogbSAwLDAiCiAgICAgICBpZD0icGF0aDIiIC8+CiAgICA8cGF0aAogICAgICAgZD0ibSAxMy40NjA5MzgsMS45Njg3NSBjIC0wLjE5MTQwNywtMC4wMDM5MSAtMC4zODY3MTksMC4wNTQ2ODggLTAuNTU4NTk0LDAuMTY3OTY5IC0wLjQ1NzAzMiwwLjMxMjUgLTAuNTc4MTI1LDAuOTMzNTkzIC0wLjI2OTUzMiwxLjM5MDYyNSAxLjgyNDIxOSwyLjcwNzAzMSAxLjgyNDIxOSw2LjIzODI4MSAwLDguOTQ1MzEyIC0wLjMwODU5MywwLjQ1NzAzMiAtMC4xODc1LDEuMDc4MTI1IDAuMjY5NTMyLDEuMzkwNjI1IDAuNDU3MDMxLDAuMzA4NTk0IDEuMDc4MTI1LDAuMTg3NSAxLjM5MDYyNSwtMC4yNjk1MzEgQyAxNS40Mjk2ODgsMTEuOTAyMzQ0IDE2LDkuOTUzMTI1IDE2LDggMTYsNi4wNDY4NzUgMTUuNDI5Njg4LDQuMDk3NjU2IDE0LjI5Mjk2OSwyLjQwNjI1IDE0LjA5NzY1NiwyLjEyMTA5NCAxMy43ODEyNSwxLjk2ODc1IDEzLjQ2MDkzOCwxLjk2ODc1IFogbSAwLDAiCiAgICAgICBmaWxsLW9wYWNpdHk9IjAuMzQ5MDIiCiAgICAgICBpZD0icGF0aDQiIC8+CiAgPC9nPgo8L3N2Zz4K"_s},
    {"Volume3-RTL"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+CjxzdmcKICAgaGVpZ2h0PSIxNnB4IgogICB2aWV3Qm94PSIwIDAgMTYgMTYiCiAgIHdpZHRoPSIxNnB4IgogICB2ZXJzaW9uPSIxLjEiCiAgIGlkPSJzdmc0IgogICB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciCiAgIHhtbG5zOnN2Zz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPgogIDxkZWZzCiAgICAgaWQ9ImRlZnM4IiAvPgogIDxwYXRoCiAgICAgZD0ibSA5LDEuMDA3ODEyIGMgMC4yOTY4NzUsLTAuMDAzOTEgMC41NzgxMjUsMC4xMjUgMC43Njk1MzEsMC4zNTE1NjMgTCAxMyw1IGggMSBjIDEuMDkzNzUsMCAyLDAuODQzNzUgMiwyIHYgMiBjIDAsMS4wODk4NDQgLTAuOTEwMTU2LDIgLTIsMiBIIDEzIEwgOS43Njk1MzEsMTQuNjQwNjI1IEMgOS41NTg1OTQsMTQuODk0NTMxIDkuMjc3MzQ0LDE1LjAwMzkwNiA5LDE1IFogTSAyLjUzOTA2MiwxLjk2ODc1IGMgMC4xOTE0MDcsLTAuMDAzOTEgMC4zODY3MTksMC4wNTQ2ODggMC41NTg1OTQsMC4xNjc5NjkgMC40NTcwMzIsMC4zMTI1IDAuNTc4MTI1LDAuOTMzNTkzIDAuMjY5NTMyLDEuMzkwNjI1IC0xLjgyNDIxOSwyLjcwNzAzMSAtMS44MjQyMTksNi4yMzgyODEgMCw4Ljk0NTMxMiAwLjMwODU5MywwLjQ1NzAzMiAwLjE4NzUsMS4wNzgxMjUgLTAuMjY5NTMyLDEuMzkwNjI1IEMgMi42NDA2MjUsMTQuMTcxODc1IDIuMDE5NTMxLDE0LjA1MDc4MSAxLjcwNzAzMSwxMy41OTM3NSAwLjU3MDMxMiwxMS45MDIzNDQgMCw5Ljk1MzEyNSAwLDggMCw2LjA0Njg3NSAwLjU3MDMxMiw0LjA5NzY1NiAxLjcwNzAzMSwyLjQwNjI1IDEuOTAyMzQ0LDIuMTIxMDk0IDIuMjE4NzUsMS45Njg3NSAyLjUzOTA2MiwxLjk2ODc1IFogTSA1Ljk2MDkzOCwzLjk4ODI4MSBDIDYuMTgzNTk0LDMuOTgwNDY5IDYuNDE0MDYyLDQuMDQ2ODc1IDYuNjA1NDY5LDQuMTkxNDA2IDYuODY3MTg4LDQuMzkwNjI1IDcsNC42OTE0MDYgNyw0Ljk5NjA5NCB2IDAuMDU4NTk0IGMgLTAuMDExNzE5LDAuMTkxNDA2IC0wLjA3NDIxOSwwLjM3NSAtMC4xOTkyMTksMC41MzUxNTYgLTEuMDc0MjE5LDEuNDI5Njg3IC0xLjA3NDIxOSwzLjM5MDYyNSAwLDQuODE2NDA2IDAuMTI1LDAuMTY0MDYyIDAuMTg3NSwwLjM0NzY1NiAwLjE5OTIxOSwwLjUzNTE1NiB2IDAuMDYyNSBDIDcsMTEuMzA4NTk0IDYuODY3MTg4LDExLjYwOTM3NSA2LjYwNTQ2OSwxMS44MDg1OTQgNi4xNjQwNjIsMTIuMTQwNjI1IDUuNTM5MDYyLDEyLjA1MDc4MSA1LjIwNzAzMSwxMS42MDkzNzUgNC40MDIzNDQsMTAuNTQyOTY5IDQsOS4yNzM0MzggNCw4IDQsNi43MjY1NjIgNC40MDIzNDQsNS40NTcwMzEgNS4yMDcwMzEsNC4zODY3MTkgNS4zOTA2MjUsNC4xNDA2MjUgNS42NzE4NzUsNC4wMDM5MDYgNS45NjA5MzgsMy45ODgyODEgWiBtIDAsMCIKICAgICBmaWxsPSIjMmUzNDM2IgogICAgIGlkPSJwYXRoMiIgLz4KPC9zdmc+Cg=="_s},
    {"Pause"_s , "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiPz4KPHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIGhlaWdodD0iMTZweCIgdmlld0JveD0iMCAwIDE2IDE2IiB3aWR0aD0iMTZweCI+PGcgZmlsbD0iIzJlMzQzNiI+PHBhdGggZD0ibSAzIDIgaCA0IHYgMTIgaCAtNCB6IG0gMCAwIi8+PHBhdGggZD0ibSA5IDIgaCA0IHYgMTIgaCAtNCB6IG0gMCAwIi8+PC9nPjwvc3ZnPgo="_s},
    {"MediaSelector"_s , "PHN2ZyB4bWxucz0naHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmcnIHdpZHRoPScxNicgaGVpZ2h0PScxNic+IFwKICAgICAgICAgIDxwYXRoIGQ9J00zLjUgMkEyLjQ5NSAyLjQ5NSAwIDAgMCAxIDQuNXY1YzAgMS4zODUgMS4xMTUgMi41MiAyLjUgMi41aDYuMzc1TDEzIDE1di0zLjA2M0EyLjQ4NiAyLjQ4NiAwIDAgMCAxNSA5LjV2LTVDMTUgMy4xMTUgMTMuODg1IDIgMTIuNSAyek0zIDdoNHYxSDN6bTUgMGg1djFIOHpNMyA5aDJ2MUgzem0zIDBoNXYxSDZ6bTYgMGgxdjFoLTF6JyBmaWxsPSclMjNGRkZGRkYnLz4gXAogICAgICAgIDwvc3ZnPg=="_s}
    };
}

String ModernMediaControlResource::getValue(const String &resource_key) {
     HashMap<String, String>::iterator it = imageMap.find(resource_key);
     return it != imageMap.end() ? it->value : "Key not found"_s;
}