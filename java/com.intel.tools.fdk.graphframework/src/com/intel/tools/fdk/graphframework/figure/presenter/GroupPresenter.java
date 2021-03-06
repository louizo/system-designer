/*
 * Copyright (C) 2013-2017 Intel Corporation
 *
 * This Program is subject to the terms of the Eclipse Public License, v. 1.0.
 * If a copy of the license was not distributed with this file,
 * you can obtain one at <http://www.eclipse.org/legal/epl-v10.html>
 *
 * SPDX-License-Identifier: EPL-1.0
 */
package com.intel.tools.fdk.graphframework.figure.presenter;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

import com.intel.tools.fdk.graphframework.figure.IGraphFigure;
import com.intel.tools.fdk.graphframework.figure.node.GroupBodyFigure;
import com.intel.tools.fdk.graphframework.graph.IGroup;
import com.intel.tools.fdk.graphframework.graph.INode;

/**
 * Wrap leaf of a group under a {@link GroupBodyFigure} to highlight the group
 */
public class GroupPresenter extends Presenter<IGroup> {

    /** The offset used between the group figure bounds and its child */
    private static final int OFFSET = 60;

    /** Track body movement to keep sub-elements together */
    private final FigureListener childListener = new FigureListener() {
        @Override
        public void figureMoved(final IFigure source) {
            if (!blockEvents) {
                updateBoundsFigure();
            }
        }
    };

    private final GroupBodyFigure boundsFigure;
    private final Set<Presenter<? extends INode>> childrenPresenters = new HashSet<>();

    private boolean blockEvents = false;
    private Point boundsLocation = new Point(0, 0);

    /**
     * @param group
     *            the represented group
     * @param childrenPresenters
     *            presenters of children nodes.
     */
    public GroupPresenter(final IGroup group, final Set<Presenter<? extends INode>> childrenPresenters) {
        super(group);
        boundsFigure = new GroupBodyFigure(group);
        this.childrenPresenters.addAll(childrenPresenters);
        this.childrenPresenters.forEach(this::add);
        boundsFigure.addFigureListener(new FigureListener() {
            @Override
            public void figureMoved(final IFigure source) {
                if (!blockEvents) {
                    blockEvents = true;
                    GroupPresenter.this.childrenPresenters.forEach(
                            p -> p.getBoundsFigure().translate(source.getBounds().x - boundsLocation.x,
                                    source.getBounds().y - boundsLocation.y));
                    boundsLocation.translate(source.getBounds().x - boundsLocation.x,
                            source.getBounds().y - boundsLocation.y);
                    blockEvents = false;
                    updateBoundsFigure();
                }
            }
        });
        updateBoundsFigure();
        getDisplayableFigures().add(boundsFigure);

        // Apply Style
        addLabel(group, boundsFigure);
    }

    /**
     * Recalculate complete union of sub-figures bounds and update dedicated field.
     */
    private void updateBoundsFigure() {
        this.blockEvents = true;
        final Rectangle rectangle = new Rectangle(boundsLocation.x, boundsLocation.y, 0, 0);
        // Make bounds figure empty
        boundsFigure.setBounds(rectangle);
        // Determine new bounds
        this.childrenPresenters.forEach(presenter -> {
            if (rectangle.isEmpty()) {
                rectangle.setBounds(presenter.getBoundsFigure().getBounds());
            } else {
                rectangle.union(presenter.getBoundsFigure().getBounds());
            }
        });
        if (!childrenPresenters.isEmpty()) {
            // If there is some presenters, we put an offset to wrap them correctly
            rectangle.x -= OFFSET / 2;
            rectangle.y -= OFFSET / 2;
        }
        rectangle.width += OFFSET;
        rectangle.height += OFFSET;
        boundsFigure.setBounds(rectangle);
        boundsLocation = boundsFigure.getLocation().getCopy();
        this.blockEvents = false;
    }

    @Override
    public IGraphFigure getNodeBody() {
        return boundsFigure;
    }

    @Override
    public IFigure getBoundsFigure() {
        return boundsFigure;
    }

    /**
     * @param presenter
     *            the child presenter to track
     */
    public void add(final Presenter<? extends INode> presenter) {
        assert getNode().getGroups().contains(presenter.getNode())
                            || getNode().getLeaves().contains(presenter.getNode())
               : "The added presenter is not related to a node of the represented group";

        this.childrenPresenters.add(presenter);
        // Track body movement to keep sub-elements together
        presenter.getNodeBody().addFigureListener(childListener);
        updateBoundsFigure();
    }

    /**
     * @param presenter
     *            the child presenter to forget
     */
    public void remove(final Presenter<? extends INode> presenter) {
        this.childrenPresenters.remove(presenter);
        presenter.getNodeBody().removeFigureListener(childListener);
        updateBoundsFigure();
    }

}
