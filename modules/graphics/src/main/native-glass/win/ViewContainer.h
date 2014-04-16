/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef VIEWCONTAINER_H
#define VIEWCONTAINER_H

class GlassView;

class ViewContainer {
    private:
        GlassView * m_view;
        BOOL m_bTrackingMouse;
        HKL m_kbLayout;
        UINT m_codePage;
        LANGID m_idLang;
        WPARAM m_deadKeyWParam;

        std::auto_ptr<IDropTarget> m_spDropTarget;

        IManipulationProcessor*             m_manipProc;
        IInertiaProcessor*                  m_inertiaProc;
        _IManipulationEvents*               m_manipEventSink;
        jclass                              m_gestureSupportCls;

        LPARAM m_lastMouseMovePosition; // or -1
        unsigned int m_mouseButtonDownCounter;

        void WmImeComposition(HWND hwnd, WPARAM wParam, LPARAM lParam);
        void WmImeNotify(HWND hwnd, WPARAM wParam, LPARAM lParam);
        void SendInputMethodEvent(jstring text,
            int cClause, int* rgClauseBoundary,
            int cAttrBlock, int* rgAttrBoundary, BYTE *rgAttrValue,
            int commitedTextLength, int caretPos, int visiblePos);
        void GetCandidatePos(LPPOINT curPos);

        void SendViewTypedEvent(int repCount, jchar wChar);

    protected:
        void HandleViewMenuEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewInputLangChange(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewPaintEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewSizeEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewKeyEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewDeadKeyEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewTypedEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        BOOL HandleViewMouseEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        BOOL HandleViewInputMethodEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        void HandleViewTouchEvent(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);
        LRESULT HandleViewGetAccessible(HWND hwnd, WPARAM wParam, LPARAM lParam);
    
        virtual void HandleViewTimerEvent(HWND hwnd, UINT_PTR timerID);

        void InitDropTarget(HWND hwnd);
        void ReleaseDropTarget();

        void InitManipProcessor(HWND hwnd);
        void ReleaseManipProcessor();

        void NotifyCaptureChanged(HWND hwnd, HWND to);

    private:
        ViewContainer(const ViewContainer&);
        ViewContainer& operator = (const ViewContainer&);
    
    public:
        enum {
            IDT_GLASS_ANIMATION_ENTER = 0x101,
            IDT_GLASS_ANIMATION_EXIT,
            IDT_GLASS_INERTIAPROCESSOR,
        };
        
        ViewContainer();

        inline GlassView * GetGlassView() const { return m_view; }
        inline void SetGlassView(GlassView * view) { m_view = view; }
        inline LANGID GetInputLanguage() { return m_idLang; }

        jobject GetView();

        void ResetMouseTracking(HWND hwnd);

        void NotifyViewSize(HWND hwnd);
        
        void NotifyGesturePerformed(HWND hWnd, 
            bool isDirect, bool isInertia,
            FLOAT x, FLOAT y, 
            FLOAT deltaX, FLOAT deltaY,
            FLOAT scaleDelta, FLOAT expansionDelta, FLOAT rotationDelta,
            FLOAT cumulativeDeltaX, FLOAT cumulativeDeltaY,
            FLOAT cumulativeScale, FLOAT cumulativeExpansion, FLOAT cumulativeRotation);
        
        void StartTouchInputInertia(HWND hwnd);
        void StopTouchInputInertia(HWND hwnd);
};

#endif // VIEWCONTAINER_H

