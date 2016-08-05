/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package test.com.sun.javafx.scene.control.inputmap;

import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static javafx.scene.input.KeyCode.*;

/**
 * Thoughts / Considerations (search for [1], etc in comments below):
 *  [1] It is possible that there are multiple mappings for a single key. This
 *      could for example happen when there are a hierarchy of InputMaps.
 *      Should lookupMapping return just the best (most specific) result, or a
 *      list of all applicable mappings? A case where a list is useful is if you
 *      want to remove all mappings of a certain type.
 *  [2] If we add an event for, say, MouseEvent.MOUSE_PRESSED, and lookup for
 *      mappings for MOUSE_EVENT.ANY, should we get the MOUSE_PRESSED mapping
 *      or not?
 */

public class InputMapTest {

    @Test public void dummy() {
        // no-op
    }

//    private Thread.UncaughtExceptionHandler exceptionHandler;
//    private int exceptionCount;
//
//    private int counter = 0;
//
//    private InputMap<Node> createDummyInputMap() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        return inputMap;
//    }
//
//    private Region createInputMapOnNode() {
//        Region dummy = new Region();
//        dummy.setInputMap(new InputMap<>(dummy));
//        return dummy;
//    }
//
//    private void installExceptionHandler(Class<? extends Exception> expected) {
//        // get the current exception handler before replacing with our own,
//        // as ListListenerHelp intercepts the exception otherwise
//        exceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
//        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
//            exceptionCount++;
//            if (e.getClass() != expected) {
//                fail("We don't expect any exceptions in this test!");
//            }
//        });
//    }
//
//    private int removeExceptionHandler() {
//        Thread.currentThread().setUncaughtExceptionHandler(exceptionHandler);
//        return exceptionCount;
//    }
//
//    @Before public void setup() {
//        counter = 0;
//        exceptionCount = 0;
//    }
//
//    private MouseEvent createMouseEvent(Node target, EventType<MouseEvent> evtType) {
//        double x, y, screenX, screenY;
//        x = y = screenX = screenY = 0;
//        int clickCount = 0;
//        MouseButton button = MouseButton.PRIMARY;
//        final PickResult pickResult = new PickResult(target, 0, 0);
//
//        MouseEvent evt = new MouseEvent(
//                target,
//                target,
//                evtType,
//                x, y,
//                screenX, screenY,
//                button,
//                clickCount,
//                false,                             // shiftDown
//                false,                             // ctrlDown
//                false,                             // altDown
//                false,                             // metaData
//                button == MouseButton.PRIMARY,     // primary button
//                button == MouseButton.MIDDLE,      // middle button
//                button == MouseButton.SECONDARY,   // secondary button
//                false,                             // synthesized
//                button == MouseButton.SECONDARY,   // is popup trigger
//                true,                              // still since pick
//                pickResult);                       // pick result
//
//        return evt;
//    }
//
//    private KeyEvent createKeyEvent(Node target, EventType<KeyEvent> evtType, KeyCode keyCode) {
//        return new KeyEvent(null,
//                target,                            // EventTarget
//                evtType,                           // eventType
//                evtType == KeyEvent.KEY_TYPED ? keyCode.getChar() : null,  // Character (unused unless evtType == KEY_TYPED)
//                keyCode.getChar(),            // text
//                keyCode,                           // KeyCode
//                false,                             // shiftDown
//                false,                             // ctrlDown
//                false,                             // altDown
//                false                              // metaData
//        );
//    }
//
//    /***************************************************************************
//     *
//     * Misc
//     *
//     **************************************************************************/
//
//    @Test public void testNodeIsSet() {
//        assertNotNull(createDummyInputMap().getNode());
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testNullNodeIsException() {
//        new InputMap<>(null);
//    }
//
//    @Test public void testNodeHasNullInputMap() {
//        Region r = new Region();
//        assertNull(r.getInputMap());
//    }
//
//
//    /***************************************************************************
//     *
//     * Child InputMap
//     *
//     **************************************************************************/
//
//    @Test public void testDefault_childMapIsNotNullAndEmpty() {
//        InputMap<?> map = createDummyInputMap();
//        assertNotNull(map.getChildInputMaps());
//        assertTrue(map.getChildInputMaps().isEmpty());
//    }
//
//    @Test public void testWrite_childInputMap_sameNode() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//
//        inputMap.getChildInputMaps().add(childMap);
//        assertFalse(inputMap.getChildInputMaps().isEmpty());
//        assertEquals(1, inputMap.getChildInputMaps().size());
//        assertEquals(childMap, inputMap.getChildInputMaps().get(0));
//    }
//
//    @Test public void testWrite_childInputMap_differentNode() {
//        installExceptionHandler(IllegalArgumentException.class);
//        Rectangle dummy = new Rectangle();
//        Rectangle dummy2 = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy2);
//        inputMap.getChildInputMaps().add(childMap);
//
//        // we expect an exception, and we also expect the bad child input map
//        // to be removed
//        assertTrue(inputMap.getChildInputMaps().isEmpty());
//
//        removeExceptionHandler();
//        assertEquals(1, exceptionCount);
//    }
//
//
//    /***************************************************************************
//     *
//     * Mappings
//     *
//     **************************************************************************/
//
//    @Test public void testDefault_mappingsIsNotNullAndEmpty() {
//        InputMap<?> map = createDummyInputMap();
//        assertNotNull(map.getMappings());
//        assertTrue(map.getMappings().isEmpty());
//    }
//
//    @Test public void testWrite_mappings() {
//        InputMap<?> map = createDummyInputMap();
//
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        map.getMappings().add(mapping);
//        assertFalse(map.getMappings().isEmpty());
//        assertEquals(1, map.getMappings().size());
//        assertEquals(mapping, map.getMappings().get(0));
//    }
//
//    @Test public void testWrite_mappings_nullMapping() {
//        installExceptionHandler(IllegalArgumentException.class);
//        InputMap<?> map = createDummyInputMap();
//        map.getMappings().add(null);
//
//        // we expect an exception, and we also expect the bad mapping to be removed
//        assertTrue(map.getMappings().isEmpty());
//
//        removeExceptionHandler();
//        assertEquals(1, exceptionCount);
//    }
//
//
//
//    /***************************************************************************
//     *
//     * Interceptor
//     *
//     **************************************************************************/
//
//    @Test public void testDefault_interceptorIsNull() {
//        InputMap<?> map = createDummyInputMap();
//        assertNull(map.getInterceptor());
//    }
//
//    @Test public void testWrite_interceptors() {
//        InputMap<?> map = createDummyInputMap();
//
//        Predicate<Event> p = e -> {
//            System.out.println("Hello Interceptor");
//            return true;
//        };
//        map.setInterceptor(p);
//        assertEquals(p, map.getInterceptor());
//    }
//
//
//
//    /***************************************************************************
//     *
//     * Mapping lookup
//     *
//     **************************************************************************/
//
//    @Test public void testLookup_nullObject() {
//        InputMap<?> map = createDummyInputMap();
//        Optional<InputMap.Mapping<?>> returnedMapping = map.lookupMapping(null);
//        assertNotNull(returnedMapping);
//        assertFalse(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_mappingThatIsNotInstalled() {
//        InputMap<?> map = createDummyInputMap();
//        Optional<InputMap.Mapping<?>> returnedMapping = map.lookupMapping(J);
//        assertNotNull(returnedMapping);
//        assertFalse(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_keyMapping_mappingThatIsInstalled() {
//        InputMap<?> map = createDummyInputMap();
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        map.getMappings().add(mapping);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = map.lookupMapping(new KeyBinding(J));
//        assertNotNull(returnedMapping);
//        assertTrue(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_mouseMapping_mappingThatIsInstalled_usingSpecificEventType() {
//        InputMap<?> map = createDummyInputMap();
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> { });
//        map.getMappings().add(mapping);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = map.lookupMapping(MouseEvent.MOUSE_PRESSED);
//        assertNotNull(returnedMapping);
//        assertTrue(returnedMapping.isPresent());
//    }
//
//    // TODO [1], [2]
////    @Test public void testLookup_mouseMapping_mappingThatIsInstalled_usingAnyEventType() {
////        InputMap<?> map = createDummyInputMap();
////        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> { });
////        map.getMappings().add(mapping);
////
////        Optional<InputMap.Mapping<?>> returnedMapping = map.lookupMapping(MouseEvent.ANY);
////        assertNotNull(returnedMapping);
////        assertTrue(returnedMapping.isPresent());
////    }
//
//    @Test public void testLookup_keyMapping_mappingThatIsInstalledOnChildMap() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        childMap.getMappings().add(mapping);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J));
//        assertNotNull(returnedMapping);
//        assertTrue(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_keyMapping_mappingThatIsInstalledOnChildMap_thenDetachedFromParent_lookAtParent() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        childMap.getMappings().add(mapping);
//
//        inputMap.getChildInputMaps().remove(childMap);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J));
//        assertNotNull(returnedMapping);
//        assertFalse(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_keyMapping_mappingThatIsInstalledOnChildMap_thenDetachedFromParent_lookAtChild() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        childMap.getMappings().add(mapping);
//
//        inputMap.getChildInputMaps().remove(childMap);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = childMap.lookupMapping(new KeyBinding(J));
//        assertNotNull(returnedMapping);
//        assertTrue(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_keyMapping_mappingThatIsInstalledOnParentMapShouldNotBeVisibleToChildMap() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(J, e -> { });
//        inputMap.getMappings().add(mapping);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = childMap.lookupMapping(new KeyBinding(J));
//        assertNotNull(returnedMapping);
//        assertFalse(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_getMostSpecificMapping_inParent() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(new KeyBinding(J).shift(), e -> { });
//        inputMap.getMappings().add(mapping1);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(J, e -> { });
//        childMap.getMappings().add(mapping2);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J).shift());
//        assertEquals(mapping1, returnedMapping.get());
//    }
//
//    @Test public void testLookup_getMostSpecificMapping_inChild() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(J, e -> { });
//        inputMap.getMappings().add(mapping1);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(new KeyBinding(J).shift(), e -> { });
//        childMap.getMappings().add(mapping2);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J).shift());
//        assertEquals(mapping2, returnedMapping.get());
//    }
//
//    @Test public void testLookup_getMostSpecificMapping_inParent_butMappingDisabled() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(new KeyBinding(J).shift(), e -> { });
//        mapping1.setDisabled(true);
//        inputMap.getMappings().add(mapping1);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(J, e -> { });
//        childMap.getMappings().add(mapping2);
//
//        // parent mapping is disabled, and child mapping doesn't match entirely
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J).shift());
//        assertFalse(returnedMapping.isPresent());
//    }
//
//    @Test public void testLookup_getMostSpecificMapping_inChild_butMappingDisabled() {
//        Rectangle dummy = new Rectangle();
//        InputMap<Node> inputMap = new InputMap<>(dummy);
//        InputMap<Node> childMap = new InputMap<>(dummy);
//        inputMap.getChildInputMaps().add(childMap);
//
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(J, e -> { });
//        inputMap.getMappings().add(mapping1);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(new KeyBinding(J).shift(), e -> { });
//        mapping2.setDisabled(true);
//        childMap.getMappings().add(mapping2);
//
//        Optional<InputMap.Mapping<?>> returnedMapping = inputMap.lookupMapping(new KeyBinding(J).shift());
//        assertFalse(returnedMapping.isPresent());
//    }
//
//
//
//    /***************************************************************************
//     *
//     * Event Handling
//     *
//     **************************************************************************/
//
//    @Test public void testEventHandlerIsCreatedAndRemovedOnNode_mouseMapping() {
//        Node n = createInputMapOnNode();
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event again, counter should increment
//        n.fireEvent(event);
//        assertEquals(1, counter);
//
//        // remove mapping, fire event, counter should not increment any longer
//        n.getInputMap().getMappings().remove(mapping);
//        n.fireEvent(event);
//        assertEquals(1, counter);
//    }
//
//    @Test public void testEventHandlerIsCreatedAndRemovedOnNode_mouseMapping_onChildMap() {
//        Node n = createInputMapOnNode();
//        InputMap childInputMap = new InputMap<>(n);
//        n.getInputMap().getChildInputMaps().add(childInputMap);
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping to child input map
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        childInputMap.getMappings().add(mapping);
//
//        // fire event again, counter should increment
//        n.fireEvent(event);
//        assertEquals(1, counter);
//
//        // remove mapping, fire event, counter should not increment any longer
//        childInputMap.getMappings().remove(mapping);
//        n.fireEvent(event);
//        assertEquals(1, counter);
//    }
//
//    @Test public void testInterceptorBlocksMapping_mouseMapping() {
//        Node n = createInputMapOnNode();
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        n.getInputMap().getMappings().add(mapping);
//        n.getInputMap().setInterceptor(new InputMap.MouseMappingInterceptor(MouseEvent.MOUSE_PRESSED));
//
//        // fire event again, counter should not increment
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // remove mapping, fire event, counter should still not increment
//        n.getInputMap().getMappings().remove(mapping);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//    }
//
//    @Test public void testInterceptorBlocksMapping_keyMapping() {
//        Node n = createInputMapOnNode();
//
//        // test before the mapping is added, counter will not increment
//        KeyEvent event = createKeyEvent(n, KeyEvent.KEY_PRESSED, KeyCode.J);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        n.getInputMap().getMappings().add(mapping);
//        n.getInputMap().setInterceptor(new InputMap.KeyMappingInterceptor(new KeyBinding(J)));
//
//        // fire event again, counter should not increment
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // remove mapping, fire event, counter should still not increment
//        n.getInputMap().getMappings().remove(mapping);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//    }
//
//    @Test public void testDisabledMapping_mouseMapping() {
//        Node n = createInputMapOnNode();
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        mapping.setDisabled(true);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event again, counter should not increment
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // remove mapping, fire event, counter should still not increment
//        n.getInputMap().getMappings().remove(mapping);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//    }
//
//    @Test public void testEventHandler_autoConsumeIsTrue() {
//        Node n = createInputMapOnNode();
//
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        assertFalse(event.isConsumed());
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        mapping.setAutoConsume(true);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event, counter should increment, and event should be consumed
//        n.fireEvent(event);
//        assertEquals(1, counter);
//        assertTrue(event.isConsumed());
//    }
//
//    @Test public void testEventHandler_autoConsumeIsFalse() {
//        Node n = createInputMapOnNode();
//
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        assertFalse(event.isConsumed());
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        mapping.setAutoConsume(false);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event, counter should increment, and event should be consumed
//        n.fireEvent(event);
//        assertEquals(1, counter);
//        assertFalse(event.isConsumed());
//    }
//
//    @Test public void testEventHandlerDisposesProperly_noChildMappings_mouseMapping() {
//        Node n = createInputMapOnNode();
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event again, counter should increment
//        n.fireEvent(event);
//        assertEquals(1, counter);
//
//        // dispose of input map, fire event, counter should not increment any longer
//        n.getInputMap().dispose();
//        n.fireEvent(event);
//        assertEquals(1, counter);
//    }
//
//    @Test public void testEventHandlerDisposesProperly_withChildMappings_mouseMapping() {
//        Node n = createInputMapOnNode();
//        InputMap childInputMap = new InputMap<>(n);
//        n.getInputMap().getChildInputMaps().add(childInputMap);
//
//        // test before the mapping is added, counter will not increment
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        n.fireEvent(event);
//        assertEquals(0, counter);
//
//        // add mapping
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        n.getInputMap().getMappings().add(mapping);
//
//        // fire event again, counter should increment
//        n.fireEvent(event);
//        assertEquals(1, counter);
//
//        // dispose of input map, fire event, counter should not increment any longer
//        n.getInputMap().dispose();
//        n.fireEvent(event);
//        assertEquals(1, counter);
//    }
//
//
//    /***************************************************************************
//     *
//     * MouseMapping
//     *
//     **************************************************************************/
//
//    private final int mouseEvent_maxSpecificity = 9;
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testMouseMapping_nullEventTypeIsIllegal() {
//        new InputMap.MouseMapping(null, e -> counter++);
//    }
//
//    @Test public void testMouseMapping_mappingKey() {
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertEquals(MouseEvent.MOUSE_PRESSED, mapping.getMappingKey());
//    }
//
//    @Test public void testMouseMapping_hashCode() {
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertTrue(mapping.hashCode() != 0);
//    }
//
//    @Test public void testMouseMapping_eventType() {
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertEquals(MouseEvent.MOUSE_PRESSED, mapping.getEventType());
//    }
//
//    @Test public void testMouseMapping_interceptors() {
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertNull(mapping.getInterceptor());
//    }
//
//    @Test public void testMouseMappingSpecificity() {
//        Node n = createInputMapOnNode();
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//
//        assertEquals(mouseEvent_maxSpecificity, mapping.getSpecificity(event));
//    }
//
//    @Test public void testMouseMappingSpecificity_disabledMapping() {
//        Node n = createInputMapOnNode();
//        MouseEvent event = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        mapping.setDisabled(true);
//
//        assertEquals(0, mapping.getSpecificity(event));
//    }
//
//    @Test public void testMouseMappingSpecificity_nullEvent() {
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertEquals(0, mapping.getSpecificity(null));
//    }
//
//    @Test public void testMouseMappingSpecificity_notMouseEvent() {
//        Node n = createInputMapOnNode();
//        KeyEvent keyEvent = createKeyEvent(n, KeyEvent.KEY_PRESSED, J);
//        InputMap.MouseMapping mapping = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        assertEquals(0, mapping.getSpecificity(keyEvent));
//    }
//
//    @Test public void testMouseMapping_equals_1() {
//        InputMap.MouseMapping mapping1 = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        InputMap.MouseMapping mapping2 = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter--);
//        assertEquals(mapping1, mapping2);
//    }
//
//    @Test public void testMouseMapping_equals_2() {
//        InputMap.MouseMapping mapping1 = new InputMap.MouseMapping(MouseEvent.MOUSE_PRESSED, e -> counter++);
//        InputMap.MouseMapping mapping2 = new InputMap.MouseMapping(MouseEvent.MOUSE_RELEASED, e -> counter++);
//        assertFalse(mapping1.equals(mapping2));
//    }
//
//
//
//    /***************************************************************************
//     *
//     * KeyMapping
//     *
//     **************************************************************************/
//
//    private final int keyEvent_maxSpecificity = 6;
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testKeyMapping_nullKeyBindingIsIllegal() {
//        new InputMap.KeyMapping((KeyBinding)null, e -> counter++);
//    }
//
//    @Test public void testKeyMapping_hashCode() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J, KeyEvent.KEY_RELEASED), e -> counter++);
//        assertTrue(mapping.hashCode() != 0);
//    }
//
//    @Test public void testKeyMapping_mappingKey() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J, KeyEvent.KEY_RELEASED), e -> counter++);
//        assertEquals(new KeyBinding(J, KeyEvent.KEY_RELEASED), mapping.getMappingKey());
//    }
//
//    @Test public void testKeyMapping_eventType_defaultIsKeyPressed() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        assertEquals(KeyEvent.KEY_PRESSED, mapping.getEventType());
//    }
//
//    @Test public void testKeyMapping_eventType() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J, KeyEvent.KEY_RELEASED), e -> counter++);
//        assertEquals(KeyEvent.KEY_RELEASED, mapping.getEventType());
//    }
//
//    @Test public void testKeyMapping_interceptor() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J, KeyEvent.KEY_RELEASED), e -> counter++);
//        assertNull(mapping.getInterceptor());
//    }
//
//    @Test public void testKeyMappingSpecificity() {
//        Node n = createInputMapOnNode();
//        KeyEvent event = createKeyEvent(n, KeyEvent.KEY_PRESSED, KeyCode.J);
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//
//        assertEquals(keyEvent_maxSpecificity, mapping.getSpecificity(event));
//    }
//
//    @Test public void testKeyMappingSpecificity_disabledMapping() {
//        Node n = createInputMapOnNode();
//        KeyEvent event = createKeyEvent(n, KeyEvent.KEY_PRESSED, KeyCode.J);
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        mapping.setDisabled(true);
//
//        assertEquals(0, mapping.getSpecificity(event));
//    }
//
//    @Test public void testKeyMappingSpecificity_nullEvent() {
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        assertEquals(0, mapping.getSpecificity(null));
//    }
//
//    @Test public void testKeyMappingSpecificity_notKeyEvent() {
//        Node n = createInputMapOnNode();
//        MouseEvent mouseEvent = createMouseEvent(n, MouseEvent.MOUSE_PRESSED);
//        InputMap.KeyMapping mapping = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        assertEquals(0, mapping.getSpecificity(mouseEvent));
//    }
//
//    @Test public void testKeyMapping_equals_1() {
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(new KeyBinding(J), e -> counter--);
//        assertEquals(mapping1, mapping2);
//    }
//
//    @Test public void testKeyMapping_equals_2() {
//        InputMap.KeyMapping mapping1 = new InputMap.KeyMapping(new KeyBinding(J, KeyEvent.KEY_RELEASED), e -> counter++);
//        InputMap.KeyMapping mapping2 = new InputMap.KeyMapping(new KeyBinding(J), e -> counter++);
//        assertFalse(mapping1.equals(mapping2));
//    }
//
//
//    /***************************************************************************
//     *
//     * Control InputMap population / removal
//     *
//     **************************************************************************/
//
//    // These tests are located in javafx.scene.control.InputMapTest, due to
//    // module visibility (the graphics module can not instantiate UI controls).
}
