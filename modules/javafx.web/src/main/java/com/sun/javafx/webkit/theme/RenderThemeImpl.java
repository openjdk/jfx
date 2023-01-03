/*
 * Copyright (c) 2011, 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.webkit.theme;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Control;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SkinBase;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.javafx.logging.PlatformLogger.Level;
import com.sun.javafx.webkit.Accessor;
import com.sun.webkit.LoadListenerClient;
import com.sun.webkit.graphics.Ref;
import com.sun.webkit.graphics.RenderTheme;
import com.sun.webkit.graphics.WCGraphicsContext;
import com.sun.webkit.graphics.WCSize;
import javafx.application.Application;

public final class RenderThemeImpl extends RenderTheme {
    private final static PlatformLogger log = PlatformLogger.getLogger(RenderThemeImpl.class.getName());

    enum WidgetType {
        TEXTFIELD      (0),
        BUTTON         (1),
        CHECKBOX       (2),
        RADIOBUTTON    (3),
        MENULIST       (4),
        MENULISTBUTTON (5),
        SLIDER         (6),
        PROGRESSBAR    (7),
        METER          (8),
        SCROLLBAR      (9);

        private static final HashMap<Integer, WidgetType> map = new HashMap<>();
        private final int value;

        private WidgetType(int value) { this.value = value; }

        static { for (WidgetType v: values()) map.put(v.value, v); }

        private static WidgetType convert(int index) { return map.get(index); }
    }

    private Accessor accessor;
    private boolean isDefault; // indicates if the instance is used in non-page context

    private Pool<FormControl> pool;

    /**
     * A pool of controls.
     * Based on a hash map where a control is the value and its ID is the key.
     * The pool size is limited. When a new control is added to the pool and
     * the limit is reached, a control which is used most rarely is removed.
     */
    static final class Pool<T extends Widget> {
        private static final int INITIAL_CAPACITY = 100;

        private int capacity = INITIAL_CAPACITY;

        // A map of control IDs used to track the rate of accociated controls
        // based on their "popularity".
        // Maps an ID to an updateContentCycleID corresponding to the cycle
        // at which the control was added to the pool.
        private final LinkedHashMap<Long, Integer> ids = new LinkedHashMap<>();

        // A map b/w the IDs and associated controls.
        // The {@code ids} map is kept in sync with the set of keys.
        private final Map<Long, WeakReference<T>> pool = new HashMap<>();

        private final Notifier<T> notifier;
        private final String type; // used for logging

        /**
         * An interface used to notify the implementor of removal
         * of a control from the pool.
         */
        interface Notifier<T> {
            public void notifyRemoved(T control);
        }

        Pool(Notifier<T> notifier, Class<T> type) {
            this.notifier = notifier;
            this.type = type.getSimpleName();
        }

        T get(long id) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("type: {0}, size: {1}, id: 0x{2}",
                        new Object[] {type, pool.size(), Long.toHexString(id)});
            }
            assert ids.size() == pool.size();

            WeakReference<T> controlRef = pool.get(id);
            if (controlRef == null) {
                return null;
            }

            T control = controlRef.get();
            if (control == null) {
                return null;
            }

            // "Bubble" the id.
            Integer value = ids.remove(Long.valueOf(id));
            ids.put(id, value);

            return control;
        }

        void put(long id, T control, int updateContentCycleID) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("size: {0}, id: 0x{1}, control: {2}",
                        new Object[] {pool.size(), Long.toHexString(id), control.getType()});
            }
            if (ids.size() >= capacity) {
                // Pull a control from the bottom of the map, least used.
                Long _id = ids.keySet().iterator().next();
                Integer cycleID = ids.get(_id);
                // Remove that "unpopular" control in case it wasn't added
                // during the current update cycle.
                if (cycleID != updateContentCycleID) {
                    ids.remove(_id);
                    T _control = pool.remove(_id).get();
                    if (_control != null) {
                        notifier.notifyRemoved(_control);
                    }
                // Otherwise, double the pool capacity.
                } else {
                    capacity = Math.min(capacity, (int)Math.ceil(Integer.MAX_VALUE/2)) * 2;
                }
            }
            ids.put(id, updateContentCycleID);
            pool.put(id, new WeakReference<>(control));
        }

        void clear() {
            if (log.isLoggable(Level.FINE)) {
                log.fine("size: " + pool.size() + ", controls: " + pool.values());
            }
            if (pool.size() == 0) {
                return;
            }
            ids.clear();
            for (WeakReference<T> controlRef : pool.values()) {
                T control = controlRef.get();
                if (control != null) {
                    notifier.notifyRemoved(control);
                }
            }
            pool.clear();
            capacity = INITIAL_CAPACITY;
        }
    }

    static class ViewListener implements InvalidationListener {
        private final Pool pool;
        private final Accessor accessor;
        private LoadListenerClient loadListener;

        ViewListener(Pool pool, Accessor accessor) {
            this.pool = pool;
            this.accessor = accessor;
        }

        @Override public void invalidated(Observable ov) {
            pool.clear(); // clear the pool when WebView changes

            // Add the LoadListenerClient when the page is available.
            if (accessor.getPage() != null && loadListener == null) {
                loadListener = new LoadListenerClient() {
                    @Override
                    public void dispatchLoadEvent(long frame, int state, String url,
                                                  String contentType, double progress, int errorCode)
                    {
                        if (state == LoadListenerClient.PAGE_STARTED) {
                            // An html page with new content is being loaded.
                            // Clear the controls associated with the previous html page.
                            pool.clear();
                        }
                    }
                    @Override
                    public void dispatchResourceLoadEvent(long frame, int state, String url,
                                                          String contentType, double progress,
                                                          int errorCode) {}
                };
                accessor.getPage().addLoadListenerClient(loadListener);
            }
        }
    }

    public RenderThemeImpl(final Accessor accessor) {
        this.accessor = accessor;
        pool = new Pool<>(fc -> {
            // Remove the control from WebView when it's removed from the pool.
            accessor.removeChild(fc.asControl());
        }, FormControl.class);
        accessor.addViewListener(new ViewListener(pool, accessor));
    }

    public RenderThemeImpl() {
        isDefault = true;
    }

    private void ensureNotDefault() {
        if (isDefault) {
            throw new IllegalStateException("the method should not be called in this context");
        }
    }

    @Override
    protected Ref createWidget(
        long id,
        int widgetIndex,
        int state,
        int w, int h,
        int bgColor,
        ByteBuffer extParams)
    {
        ensureNotDefault();

        FormControl fc = pool.get(id);
        WidgetType type = WidgetType.convert(widgetIndex);

        if (fc == null || fc.getType() != type) {
            if (fc  != null) {
                // Remove the unmatching control.
                accessor.removeChild(fc.asControl());
            }
            switch (type) {
                case TEXTFIELD:
                    fc = new FormTextField();
                    break;
                case BUTTON:
                    fc = new FormButton();
                    break;
                case CHECKBOX:
                    fc = new FormCheckBox();
                    break;
                case RADIOBUTTON:
                    fc = new FormRadioButton();
                    break;
                case MENULIST:
                    fc = new FormMenuList();
                    break;
                case MENULISTBUTTON:
                    fc = new FormMenuListButton();
                    break;
                case SLIDER:
                    fc = new FormSlider();
                    break;
                case PROGRESSBAR:
                    fc = new FormProgressBar(WidgetType.PROGRESSBAR);
                    break;
                case METER:
                    fc = new FormProgressBar(WidgetType.METER);
                    break;
                default:
                    log.severe("unknown widget index: {0}", widgetIndex);
                    return null;
            }
            fc.asControl().setFocusTraversable(false);
            pool.put(id, fc, accessor.getPage().getUpdateContentCycleID()); // put or replace the entry
            accessor.addChild(fc.asControl());
        }

        fc.setState(state);
        Control ctrl = fc.asControl();
        if (ctrl.getWidth() != w || ctrl.getHeight() != h) {
            ctrl.resize(w, h);
        }
        if (ctrl.isManaged()) {
            ctrl.setManaged(false);
        }

        if (extParams != null) {
            if (type == WidgetType.SLIDER) {
                Slider slider = (Slider)ctrl;
                extParams.order(ByteOrder.nativeOrder());
                slider.setOrientation(extParams.getInt()==0
                    ? Orientation.HORIZONTAL
                    : Orientation.VERTICAL);
                slider.setMax(extParams.getFloat());
                slider.setMin(extParams.getFloat());
                slider.setValue(extParams.getFloat());
            } else if (type == WidgetType.PROGRESSBAR) {
                ProgressBar progress = (ProgressBar)ctrl;
                extParams.order(ByteOrder.nativeOrder());
                progress.setProgress(extParams.getInt() == 1
                        ? extParams.getFloat()
                        : progress.INDETERMINATE_PROGRESS);
            } else if (type == WidgetType.METER) {
                ProgressBar progress = (ProgressBar) ctrl;
                extParams.order(ByteOrder.nativeOrder());
                progress.setProgress(extParams.getFloat());
                progress.setStyle(getMeterStyle(extParams.getInt()));
            }
        }
        return new FormControlRef(fc);
    }

    private String getMeterStyle(int region) {
        // see GaugeRegion in HTMLMeterElement.h
        switch (region) {
            case 1: // GaugeRegionSuboptimal
                return "-fx-accent: yellow";
            case 2: // GaugeRegionEvenLessGood
                return "-fx-accent: red";
            default: // GaugeRegionOptimum
                return "-fx-accent: green";
        }
    }

    @Override
    public void drawWidget(
        WCGraphicsContext g,
        final Ref widget,
        int x, int y)
    {
        ensureNotDefault();

        FormControl fcontrol = ((FormControlRef) widget).asFormControl();
        if (fcontrol != null) {
            Control control = fcontrol.asControl();
            if (control != null) {
                g.saveState();
                g.translate(x, y);
                Renderer.getRenderer().render(control, g);
                g.restoreState();
            }
        }
    }

    @Override
    public WCSize getWidgetSize(Ref widget) {
        ensureNotDefault();

        FormControl fcontrol = ((FormControlRef)widget).asFormControl();
        if (fcontrol != null) {
            Control control = fcontrol.asControl();
            return new WCSize((float)control.getWidth(), (float)control.getHeight());
        }
        return new WCSize(0, 0);
    }

    @Override
    protected int getRadioButtonSize() {
        String style = Application.getUserAgentStylesheet();
        if (Application.STYLESHEET_MODENA.equalsIgnoreCase(style)) {
            return 20; // 18 + 2; size + focus outline
        } else if (Application.STYLESHEET_CASPIAN.equalsIgnoreCase(style)) {
            return 19; // 16 + 3; size + focus outline
        }
        return 20;
    }

    // TODO: get theme value
    @Override
    protected int getSelectionColor(int index) {
        switch (index) {
            case BACKGROUND: return 0xff0093ff;
            case FOREGROUND: return 0xffffffff;
            default: return 0;
        }
    }

    private static boolean hasState(int state, int mask) {
        return (state & mask) != 0;
    }

    private static final class FormControlRef extends Ref {
        private final WeakReference<FormControl> fcRef;

        private FormControlRef(FormControl fc) {
            this.fcRef = new WeakReference<>(fc);
        }

        private FormControl asFormControl() {
            return fcRef.get();
        }
    }

    interface Widget {
        public WidgetType getType();
    }

    private interface FormControl extends Widget {
        public Control asControl();
        public void setState(int state);
    }

    private static final class FormButton extends Button implements FormControl {

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
            setPressed(hasState(state, RenderTheme.PRESSED));
            if (isPressed()) arm(); else disarm();
        }

        @Override public WidgetType getType() { return WidgetType.BUTTON; }
    }

    private static final class FormTextField extends TextField implements FormControl {

        private FormTextField() {
            setStyle("-fx-display-caret: false");
        }

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setEditable(hasState(state, RenderTheme.READ_ONLY));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
        }

        @Override public WidgetType getType() { return WidgetType.TEXTFIELD; }
    }

    private static final class FormCheckBox extends CheckBox implements FormControl {

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
            setSelected(hasState(state, RenderTheme.CHECKED));
        }

        @Override public WidgetType getType() { return WidgetType.CHECKBOX; }
    }

    private static final class FormRadioButton extends RadioButton implements FormControl {

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
            setSelected(hasState(state, RenderTheme.CHECKED));
        }

        @Override public WidgetType getType() { return WidgetType.RADIOBUTTON; }
    }

    private static final class FormSlider extends Slider implements FormControl {

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
        }

        @Override public WidgetType getType() { return WidgetType.SLIDER; }
    }

    private static final class FormProgressBar extends ProgressBar implements FormControl {
        private final WidgetType type;

        private FormProgressBar(WidgetType type) {
            this.type = type;
        }

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
        }

        @Override public WidgetType getType() { return type; }
    }

    private static final class FormMenuList extends ChoiceBox implements FormControl {

        private FormMenuList() {
            // Adding a dummy item to please ChoiceBox.
            List<String> l = new ArrayList<>();
            l.add("");
            setItems(FXCollections.observableList(l));
        }

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setFocused(hasState(state, RenderTheme.FOCUSED));
            setHover(hasState(state, RenderTheme.HOVERED) && !isDisabled());
        }

        @Override public WidgetType getType() { return WidgetType.MENULIST; }
    }

    private static final class FormMenuListButton extends Button implements FormControl {

        private static final int MAX_WIDTH = 20;
        private static final int MIN_WIDTH = 16;

        @Override public Control asControl() { return this; }

        @Override public void setState(int state) {
            setDisabled(! hasState(state, RenderTheme.ENABLED));
            setHover(hasState(state, RenderTheme.HOVERED));
            setPressed(hasState(state, RenderTheme.PRESSED));
            if (isPressed()) arm(); else disarm();
        }

        private FormMenuListButton() {
            setSkin(new Skin());
            setFocusTraversable(false);
            getStyleClass().add("form-select-button");
        }

        /**
         * @param height is the height of the FormMenuList widget
         * @param width is passed equal to height
         */
        @Override public void resize(double width, double height) {
            width = height > MAX_WIDTH ? MAX_WIDTH : height < MIN_WIDTH ? MIN_WIDTH : height;

            super.resize(width, height);

            // [x] is originally aligned with the right edge of
            // the menulist control, and here we adjust it
            setTranslateX(-width);
        }

        private final class Skin extends SkinBase {
            Skin() {
                super(FormMenuListButton.this);

                Region arrow = new Region();
                arrow.getStyleClass().add("arrow");
                arrow.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
                BorderPane pane = new BorderPane();
                pane.setCenter(arrow);
                getChildren().add(pane);
            }
        }

        @Override public WidgetType getType() { return WidgetType.MENULISTBUTTON; }
    }
}
