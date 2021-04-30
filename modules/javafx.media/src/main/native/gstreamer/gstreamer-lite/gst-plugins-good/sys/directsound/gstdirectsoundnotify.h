/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#ifdef GSTREAMER_LITE

#ifndef GSTDIRECTSOUNDNOTIFY_H
#define GSTDIRECTSOUNDNOTIFY_H

#include <mmdeviceapi.h>

typedef void (*GSTDSNotfierCallback)(void*);

#ifdef __cplusplus
extern "C" {
#endif
  void* InitNotificator(GSTDSNotfierCallback pCallback, void *pData);
  void ReleaseNotificator(void *pObject);
#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
class GSTDirectSoundNotify : IMMNotificationClient
{
public:
  GSTDirectSoundNotify();
  ~GSTDirectSoundNotify();

  bool Init(GSTDSNotfierCallback pCallback, void *pData);
  void Dispose();

  // IUnknown
  IFACEMETHODIMP_(ULONG) AddRef();
  IFACEMETHODIMP_(ULONG) Release();

private:
  // IMMNotificationClient
  IFACEMETHODIMP OnDeviceStateChanged(LPCWSTR pwstrDeviceId, DWORD dwNewState) { return S_OK; }
  IFACEMETHODIMP OnDeviceAdded(LPCWSTR pwstrDeviceId) { return S_OK; }
  IFACEMETHODIMP OnDeviceRemoved(LPCWSTR pwstrDeviceId) { return S_OK; }
  IFACEMETHODIMP OnDefaultDeviceChanged(EDataFlow flow, ERole role, LPCWSTR pwstrDefaultDeviceId);
  IFACEMETHODIMP OnPropertyValueChanged(LPCWSTR pwstrDeviceId, const PROPERTYKEY key) { return S_OK; }

  long m_cRef;
  IMMDeviceEnumerator* m_pEnumerator;
  GSTDSNotfierCallback m_pCallback;
  void *m_pData;
  HRESULT m_hrCoInit;

  // IUnknown
  IFACEMETHODIMP QueryInterface(const IID& iid, void** ppUnk);
};
#endif // __cplusplus

#endif // GSTDIRECTSOUNDNOTIFY_H
#endif // GSTREAMER_LITE