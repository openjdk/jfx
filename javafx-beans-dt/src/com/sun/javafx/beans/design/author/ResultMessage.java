/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.design.author;

import com.sun.javafx.beans.design.DisplayItem;
import static com.sun.javafx.beans.design.author.ResultMessageType.*;
import javafx.scene.image.Image;


/**
 * <p>A ResultMessage object represents a single message to a user about an operation that was just
 * completed (or failed).  ResultMessage objects are created and added to Result objects when
 * returning from an operation.</p>
 *
 * @author Joe Nuxoll
 * @version 1.0
 * @see Result
 */
public class ResultMessage implements DisplayItem {
    protected ResultMessageType type;
    protected String displayName;
    protected String description;
    protected Image smallIcon;
    protected Image largeIcon;
    protected String helpKey;

    /**
     * TODO Missing javadoc
     */
    public ResultMessage(final ResultMessageType type, final String displayName,
        final String description) {
        if ((type == INFORMATION) || (type == WARNING) || (type == CRITICAL)) {
            this.type = type;
        } else {
            throw new IllegalArgumentException(
                "Message type must be TYPE_INFORMATION (0), TYPE_WARNING (1), or TYPE_CRITICAL (2)"); // NOI18N
        }

        this.type = type;
        this.displayName = displayName;
        this.description = description;
    }

    public ResultMessage(final ResultMessageType type, final String displayName,
        final String description, final Image smallIcon) {
        this(type, displayName, description);
        this.smallIcon = smallIcon;
    }

    /**
     * Creates a new ResultMessage object with the specified type, displayName, and description.
     *
     * @param type The desired type of the message: {@link ResultMessageType#INFORMATION},
     *  {@link ResultMessageType#WARNING}, or {@link ResultMessageType#CRITICAL}.
     * @param displayName The desired display name of the message
     * @param description The desired description of the message
     * @return A newly created ResultMessage object
     */
    public static ResultMessage create(final ResultMessageType type, final String displayName,
        final String description) {
        return new ResultMessage(type, displayName, description);
    }

    /**
     * Creates a new ResultMessage object with the specified type, displayName, description, and icon.
     *
     * @param type The desired type of the message: {@link ResultMessageType#INFORMATION},
     *  {@link ResultMessageType#WARNING}, or {@link ResultMessageType#CRITICAL}.
     * @param displayName The desired display name of the message
     * @param description The desired description of the message
     * @param smallIcon The desired image icon for the message
     * @return A newly created ResultMessage object
     */
    public static ResultMessage create(final ResultMessageType type, final String displayName,
        final String description, final Image smallIcon) {
        return new ResultMessage(type, displayName, description, smallIcon);
    }

    public void setMessageType(final ResultMessageType type) {
        if ((type == INFORMATION) || (type == WARNING) || (type == CRITICAL)) {
            this.type = type;
        } else {
            throw new IllegalArgumentException(
                "Message type must be one of ResultMessageType.{INFORMATION, WARNING, CRITICAL}"); // NOI18N
        }
    }

    public ResultMessageType getMessageType() {
        return type;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setSmallIcon(final Image smallIcon) {
        this.smallIcon = smallIcon;
    }

    public Image getSmallIcon() {
        return smallIcon;
    }

    public void setLargeIcon(final Image largeIcon) {
        this.largeIcon = largeIcon;
    }

    public Image getLargeIcon() {
        return largeIcon;
    }

    public void setHelpKey(final String helpKey) {
        this.helpKey = helpKey;
    }

    public String getHelpKey() {
        return helpKey;
    }
}
