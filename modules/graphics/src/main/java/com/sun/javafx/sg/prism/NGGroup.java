/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.sg.prism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.sun.javafx.geom.DirtyRegionContainer;
import com.sun.javafx.geom.RectBounds;
import com.sun.javafx.geom.Rectangle;
import com.sun.javafx.geom.transform.BaseTransform;
import com.sun.javafx.geom.transform.GeneralTransform3D;
import com.sun.javafx.scene.NodeHelper;
import com.sun.prism.Graphics;
import com.sun.scenario.effect.Blend;
import com.sun.scenario.effect.Blend.Mode;
import com.sun.scenario.effect.FilterContext;
import com.sun.scenario.effect.ImageData;
import com.sun.scenario.effect.impl.prism.PrDrawable;
import com.sun.scenario.effect.impl.prism.PrEffectHelper;
import javafx.scene.Node;

/**
 */
public class NGGroup extends NGNode {
    /**
     * The blend mode to use with this group.
     */
    private Blend.Mode blendMode = Blend.Mode.SRC_OVER;
    // NOTE I need a special array list here where all nodes added can have
    // their parent set correctly, and all nodes removed have it cleared correctly.
    // Actually, if a node is removed, I probably don't have to worry about
    // clearing it because as soon as it is added to another parent it will be set
    // and there is no magic listener foo going on here.
    private List<NGNode> children = new ArrayList<>(1);
    private List<NGNode> unmod = Collections.unmodifiableList(children);
    private List<NGNode> removed;

    /**
     * The viewOrderChildren is a list children sorted in decreasing viewOrder
     * order if it is not empty. Its size should always be equal to
     * children.size(). If viewOrderChildren is empty it implies that the
     * rendering order of the children is the same as the order in the children
     * list.
     */
    private final List<NGNode> viewOrderChildren = new ArrayList<>(1);

    /**
     * This mask has all bits that mark that a region intersects this group.
     * Which means it looks like this: 00010101010101010101010101010101 (first bit for sign)
     */
    private static final int REGION_INTERSECTS_MASK = 0x15555555;

    /***************************************************************************
     *                                                                         *
     * Implementation of the PGGroup interface                                 *
     *                                                                         *
     **************************************************************************/

    /**
     * Gets an unmodifiable list of the current children on this group
     */
    public List<NGNode> getChildren() { return unmod; }

    /**
     * Adds a node to the given index. An index of -1 means "append", for legacy
     * reasons (it was easier than asking for the number of children, iirc).
     * @param index -1, or <= node.size()
     * @param node
     */
    public void add(int index, NGNode node) {
        // Validate the arguments
        if ((index < -1) || (index > children.size())) {
            throw new IndexOutOfBoundsException("invalid index");
        }

        // NOTE: We used to do checks here to make sure that a node
        // being added didn't already have another parent listed as
        // its parent. Now we just silently accept them. The FX side
        // is already doing this check, and implementing this check
        // properly would require that the "clear" implementation visit
        // all nodes and clear this flag, which is really just wasted work.
        NGNode child = node;

        // When a new node is added, we need to make sure the new node has this
        // group registered as its parent. We also need to make sure I invalidate
        // this group's cache and mark it dirty. Note that we don't have to worry
        // about notifying the other parent that it has lost a node: the FX
        // scene graph will be sure to send a "remove" notification to the other
        // parent, so we don't have to be concerned with the other parent
        // having to be marked dirty or whatnot.
        child.setParent(this);
        childDirty = true;
        if (index == -1) {
            children.add(node);
        } else {
            children.add(index, node);
        }
        child.markDirty();
        markTreeDirtyNoIncrement();
        geometryChanged();
    }

    public void clearFrom(int fromIndex) {
        if (fromIndex < children.size()) {
            children.subList(fromIndex, children.size()).clear();
            geometryChanged();
            childDirty = true;
            markTreeDirtyNoIncrement();
        }
    }

    public List<NGNode> getRemovedChildren() {
        return removed;
    }

    public void addToRemoved(NGNode n) {
        if (removed == null) removed = new ArrayList<>();
        if (dirtyChildrenAccumulated > DIRTY_CHILDREN_ACCUMULATED_THRESHOLD) {
            return;
        }

        removed.add(n);
        dirtyChildrenAccumulated++;

        if (dirtyChildrenAccumulated > DIRTY_CHILDREN_ACCUMULATED_THRESHOLD) {
            removed.clear(); //no need to store anything in this case
        }
    }

    @Override
    protected void clearDirty() {
        super.clearDirty();
        if (removed != null) removed.clear();
    }

    public void remove(NGNode node) {
        // We just remove the node and mark this group as being dirty. Really, if we
        // supported sub-regions within the group, we'd only have to mark the
        // sub-region that had been occupied by the node as dirty, but we do not
        // as yet have this optimization (mostly because we didn't have it in
        // Scenario, mostly because it was hard to optimize correctly).
        children.remove(node);
        geometryChanged();
        childDirty = true;
        markTreeDirtyNoIncrement();
    }

    public void remove(int index) {
        children.remove(index);
        geometryChanged();
        childDirty = true;
        markTreeDirtyNoIncrement();
    }

    public void clear() {
        children.clear();
        childDirty = false;
        geometryChanged();
        markTreeDirtyNoIncrement();
    }

    // Call this method if children view order is needed for rendering.
    // The returned list should be treated as read only.
    private List<NGNode> getOrderedChildren() {
        if (!viewOrderChildren.isEmpty()) {
            return viewOrderChildren;
        }
        return children;
    }

    // NOTE: This method is called on the FX application thread with the
    // RenderLock held.
    public void setViewOrderChildren(List<Node> sortedChildren) {
        viewOrderChildren.clear();
        for (Node child : sortedChildren) {
            NGNode childPeer = NodeHelper.getPeer(child);
            viewOrderChildren.add(childPeer);
        }

        // Mark visual dirty
        visualsChanged();
    }

    /**
     * Set by the FX scene graph.
     * @param blendMode cannot be null
     */
    public void setBlendMode(Object blendMode) {
        // Verify the arguments
        if (blendMode == null) {
            throw new IllegalArgumentException("Mode must be non-null");
        }
        // If the blend mode has changed, mark this node as dirty and
        // invalidate its cache
        if (this.blendMode != blendMode) {
            this.blendMode = (Blend.Mode)blendMode;
            visualsChanged();
        }
    }

    @Override
    public void renderForcedContent(Graphics gOptional) {
        List<NGNode> orderedChildren = getOrderedChildren();
        if (orderedChildren == null) {
            return;
        }
        for (int i = 0; i < orderedChildren.size(); i++) {
            orderedChildren.get(i).renderForcedContent(gOptional);
        }
    }

    @Override
    protected void renderContent(Graphics g) {
        List<NGNode> orderedChildren = getOrderedChildren();
        if (orderedChildren == null) {
            return;
        }

        NodePath renderRoot = g.getRenderRoot();
        int startPos = 0;
        if (renderRoot != null) {
            if (renderRoot.hasNext()) {
                renderRoot.next();
                startPos = orderedChildren.indexOf(renderRoot.getCurrentNode());

                for (int i = 0; i < startPos; ++i) {
                    orderedChildren.get(i).clearDirtyTree();
                }
            } else {
                g.setRenderRoot(null);
            }
        }

        if (blendMode == Blend.Mode.SRC_OVER ||
                orderedChildren.size() < 2) {  // Blend modes only work "between" siblings

            for (int i = startPos; i < orderedChildren.size(); i++) {
                NGNode child;
                try {
                    child = orderedChildren.get(i);
                } catch (Exception e) {
                    child = null;
                }
                // minimal protection against concurrent update of the list.
                if (child != null) {
                    child.render(g);
                }
            }
            return;
        }

        Blend b = new Blend(blendMode, null, null);
        FilterContext fctx = getFilterContext(g);

        ImageData bot = null;
        boolean idValid = true;
        do {
            // TODO: probably don't need to wrap the transform here... (RT-26981)
            BaseTransform transform = g.getTransformNoClone().copy();
            if (bot != null) {
                bot.unref();
                bot = null;
            }
            Rectangle rclip = PrEffectHelper.getGraphicsClipNoClone(g);
            for (int i = startPos; i < orderedChildren.size(); i++) {
                NGNode child = orderedChildren.get(i);
                ImageData top = NodeEffectInput.
                    getImageDataForNode(fctx, child, false, transform, rclip);
                if (bot == null) {
                    bot = top;
                } else {
                    ImageData newbot =
                        b.filterImageDatas(fctx, transform, rclip, null, bot, top);
                    bot.unref();
                    top.unref();
                    bot = newbot;
                }
            }
            if (bot != null && (idValid = bot.validate(fctx))) {
                Rectangle r = bot.getUntransformedBounds();
                PrDrawable botimg = (PrDrawable)bot.getUntransformedImage();
                g.setTransform(bot.getTransform());
                g.drawTexture(botimg.getTextureObject(),
                        r.x, r.y, r.width, r.height);
            }
        } while (bot == null || !idValid);

        if (bot != null) {
            bot.unref();
        }
    }

    @Override
    protected boolean hasOverlappingContents() {
        if (blendMode != Mode.SRC_OVER) {
            // All other modes are flattened so there are no overlapping issues
            return false;
        }
        List<NGNode> orderedChildren = getOrderedChildren();
        int n = (orderedChildren == null ? 0 : orderedChildren.size());
        if (n == 1) {
            return orderedChildren.get(0).hasOverlappingContents();
        }
        return (n != 0);
    }

    public boolean isEmpty() {
        return children == null || children.isEmpty();
    }

    @Override
    protected boolean hasVisuals() {
        return false;
    }


    @Override
    protected boolean needsBlending() {
        Blend.Mode mode = getNodeBlendMode();
        // TODO: If children are all SRC_OVER then we can pass on SRC_OVER too
        // (RT-26981)
        return (mode != null);
    }

    /***************************************************************************
     *                                                                         *
     *                     Culling Related Methods                             *
     *                                                                         *
     **************************************************************************/
    @Override
    protected RenderRootResult computeRenderRoot(NodePath path, RectBounds dirtyRegion, int cullingIndex, BaseTransform tx,
                                       GeneralTransform3D pvTx) {

        // If the NGGroup is completely outside the culling area, then we don't have to traverse down
        // to the children yo.
        if (cullingIndex != -1) {
            final int bits = cullingBits >> (cullingIndex*2);
            if ((bits & DIRTY_REGION_CONTAINS_OR_INTERSECTS_NODE_BOUNDS) == 0) {
                return RenderRootResult.NO_RENDER_ROOT;
            }
            if ((bits & DIRTY_REGION_CONTAINS_NODE_BOUNDS) != 0) {
                cullingIndex = -1; // Do not check culling in children,
                                   // as culling bits are not set for fully interior groups
            }
        }

        if (!isVisible()) {
            return RenderRootResult.NO_RENDER_ROOT;
        }

        if (getOpacity() != 1.0 || (getEffect() != null && getEffect().reducesOpaquePixels()) || needsBlending()) {
            return RenderRootResult.NO_RENDER_ROOT;
        }

        if (getClipNode() != null) {
            final NGNode clip = getClipNode();
            RectBounds clipBounds = clip.getOpaqueRegion();
            if (clipBounds == null) {
                return RenderRootResult.NO_RENDER_ROOT;
            }
            TEMP_TRANSFORM.deriveWithNewTransform(tx).deriveWithConcatenation(getTransform()).deriveWithConcatenation(clip.getTransform());
            if (!checkBoundsInQuad(clipBounds, dirtyRegion, TEMP_TRANSFORM, pvTx)) {
                return RenderRootResult.NO_RENDER_ROOT;
            }
        }

        // An NGGroup itself never draws pixels, so we don't have to call super. Just visit
        // each child, starting with the top-most.
        double mxx = tx.getMxx();
        double mxy = tx.getMxy();
        double mxz = tx.getMxz();
        double mxt = tx.getMxt();

        double myx = tx.getMyx();
        double myy = tx.getMyy();
        double myz = tx.getMyz();
        double myt = tx.getMyt();

        double mzx = tx.getMzx();
        double mzy = tx.getMzy();
        double mzz = tx.getMzz();
        double mzt = tx.getMzt();
        final BaseTransform chTx = tx.deriveWithConcatenation(getTransform());

        // We need to keep a reference to the result of calling computeRenderRoot on each child
        RenderRootResult result = RenderRootResult.NO_RENDER_ROOT;
        // True if every child _after_ the the found render root is clean
        boolean followingChildrenClean = true;
        // Iterate over all children, looking for a render root.
        List<NGNode> orderedChildren = getOrderedChildren();
        for (int resultIdx = orderedChildren.size() - 1; resultIdx >= 0; resultIdx--) {
            // Get the render root result from the child
            final NGNode child = orderedChildren.get(resultIdx);
            result = child.computeRenderRoot(path, dirtyRegion, cullingIndex, chTx, pvTx);
            // Update this flag, which if true means that this child and all subsequent children
            // of this group are all clean.
            followingChildrenClean &= child.isClean();

            if (result == RenderRootResult.HAS_RENDER_ROOT) {
                // If we have a render root and it is dirty, then we don't really care whether
                // followingChildrenClean is true or false, we just add this group to the
                // path and we're done.
                path.add(this);
                break;
            } else if (result == RenderRootResult.HAS_RENDER_ROOT_AND_IS_CLEAN) {
                path.add(this);
                // If we have a result which is itself reporting that it is clean, but
                // we have some following children which are dirty, then we need to
                // switch the result for this Group to be HAS_RENDER_ROOT.
                if (!followingChildrenClean) {
                    result = RenderRootResult.HAS_RENDER_ROOT;
                }
                break;
            }
        }
        // restore previous transform state
        tx.restoreTransform(mxx, mxy, mxz, mxt, myx, myy, myz, myt, mzx, mzy, mzz, mzt);
        return result;
    }

    @Override
    protected void markCullRegions(
            DirtyRegionContainer drc,
            int cullingRegionsBitsOfParent,
            BaseTransform tx,
            GeneralTransform3D pvTx) {

        //set culling bits for this group first.
        super.markCullRegions(drc, cullingRegionsBitsOfParent, tx, pvTx);

        //cullingRegionsBits == 0 group is outside all dirty regions
        // we can cull all children otherwise check children.
        // If none of the regions intersect this group, skip pre-culling
        if (cullingBits == -1 || (cullingBits != 0 && (cullingBits & REGION_INTERSECTS_MASK) != 0)) {
            //save current transform
            double mxx = tx.getMxx();
            double mxy = tx.getMxy();
            double mxz = tx.getMxz();
            double mxt = tx.getMxt();

            double myx = tx.getMyx();
            double myy = tx.getMyy();
            double myz = tx.getMyz();
            double myt = tx.getMyt();

            double mzx = tx.getMzx();
            double mzy = tx.getMzy();
            double mzz = tx.getMzz();
            double mzt = tx.getMzt();
            BaseTransform chTx = tx.deriveWithConcatenation(getTransform());

            NGNode child;
            List<NGNode> orderedChildren = getOrderedChildren();
            for (int chldIdx = 0; chldIdx < orderedChildren.size(); chldIdx++) {
                child = orderedChildren.get(chldIdx);
                child.markCullRegions(
                        drc,
                        cullingBits,
                        chTx,
                        pvTx);
            }
            // restore previous transform state
            tx.restoreTransform(mxx, mxy, mxz, mxt, myx, myy, myz, myt, mzx, mzy, mzz, mzt);
        }
    }

    @Override
    public void drawDirtyOpts(final BaseTransform tx, final GeneralTransform3D pvTx,
                              Rectangle clipBounds, int[] countBuffer, int dirtyRegionIndex) {
        super.drawDirtyOpts(tx, pvTx, clipBounds, countBuffer, dirtyRegionIndex);
        // Not really efficient but this code is only executed during debug. This makes sure
        // that the source transform (tx) is not modified.
        BaseTransform clone = tx.copy();
        clone = clone.deriveWithConcatenation(getTransform());
        List<NGNode> orderedChildren = getOrderedChildren();
        for (int childIndex = 0; childIndex < orderedChildren.size(); childIndex++) {
            final NGNode child = orderedChildren.get(childIndex);
            child.drawDirtyOpts(clone, pvTx, clipBounds, countBuffer, dirtyRegionIndex);
        }
    }

}
