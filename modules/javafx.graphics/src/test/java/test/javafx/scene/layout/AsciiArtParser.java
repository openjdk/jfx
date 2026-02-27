/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package test.javafx.scene.layout;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses an ASCII-art representation of a UI layout and extracts
 * named rectangular bounds from it.
 * <p>
 * The ASCII input is interpreted as a grid where contiguous fillable
 * regions represent layout areas. Areas may represent:
 * <ul>
 *   <li>Named boxes (e.g. {@code name: width x height})</li>
 *   <li>Spacers (width x height only)</li>
 *   <li>Margins (four inset values surrounding nested content)</li>
 * </ul>
 * <p>
 * Parsing is performed during construction. After creation, instances
 * are effectively immutable and may be queried for total bounds or
 * individual named bounds.
 * <p>
 * This class is intended primarily for layout verification in tests.
 */
public class AsciiArtParser {

    /**
     * Represents a rectangular region using double-precision coordinates.
     *
     * @param x the minimum x coordinate (left)
     * @param y the minimum y coordinate (top)
     * @param width the width of the region
     * @param height the height of the region
     */
    public record Bounds(double x, double y, double width, double height) {
        boolean isEmpty() {
            return width == 0 || height == 0;
        }
    }

    private record Extent(double start, double length) {
    }

    private record Insets(double top, double left, double right, double bottom) {
    }

    private record Area(String name, Area parent, double width, double height, Insets margin) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final Area MARKED = new Area("--MARKED--", null, 1, 1, null);

    private final char[][] art;
    private final Bounds bounds;
    private final List<Area> areas;
    private final Area[][] areaMap;
    private final int rows;
    private final int cols;
    private final Map<String, Bounds> namedBounds;

    private int unnamedCount;
    private int marginCount;
    private int emptyCount;

    /**
     * Creates a new parser from the given ASCII-art lines.
     * <p>
     * All lines are normalized into a rectangular character grid by
     * right-padding shorter lines with spaces.
     *
     * @param asciiArt the ASCII-art lines describing a layout, cannot be {@code null}
     * @throws NullPointerException if {@code asciiArt} is {@code null}
     */
    public AsciiArtParser(List<String> asciiArt) {
        this(toCharArray(asciiArt));
    }

    AsciiArtParser(char[][] art) {
        this.art = art;
        this.cols = art.length;
        this.rows = art[0].length;
        this.areaMap = new Area[cols][rows];
        this.areas = findAllAreas();
        this.namedBounds = new HashMap<>();

        Map<Area, Extent> leftAreaExtents = new HashMap<>();
        Map<Area, Extent> topAreaExtents = new HashMap<>();

        if (!areas.isEmpty()) {
            double minRow = Double.POSITIVE_INFINITY;
            double minCol = Double.POSITIVE_INFINITY;
            double maxRow = Double.NEGATIVE_INFINITY;
            double maxCol = Double.NEGATIVE_INFINITY;

            for (Area area : areas) {
                Extent leftBound = calculateLeftExtent(area, leftAreaExtents);
                Extent topBound = calculateTopExtent(area, topAreaExtents);

                Bounds b = new Bounds(leftBound.start, topBound.start, leftBound.length, topBound.length);

                if (area.name != null) {
                    namedBounds.put(area.name, b);
                }

                minRow = Math.min(minRow, b.y);
                minCol = Math.min(minCol, b.x);
                maxRow = Math.max(maxRow, b.y + b.height - 1);
                maxCol = Math.max(maxCol, b.x + b.width - 1);
            }

            assert minRow == 0;
            assert minCol == 0;

            this.bounds = new Bounds(0, 0, maxCol + 1, maxRow + 1);
        }
        else {
            this.bounds = new Bounds(0, 0, 0, 0);
        }
    }

    /**
     * Gets the total bounds of the ASCII art container description.
     *
     * @return the total bounds, never {@code null}
     */
    public Bounds getBounds() {
        return bounds;
    }

    /**
     * Gets a named bound in the ASCII art container description. Returns
     * {@code null} if the named bound did not exist.
     *
     * @param name the name of a named bound, cannot be {@code null}
     * @return the bounds with the given name, or {@code null} if not found
     * @throws NullPointerException if {@code name} was {@code null}
     */
    public Bounds getBounds(String name) {
        return namedBounds.get(Objects.requireNonNull(name, "name"));
    }

    public Map<String, Bounds> getAllBounds() {
        return namedBounds;
    }

    /*
     * Creates a new 2d char array extracted from the given 2d array with the given inclusive bounds.
     */
    private static char[][] extractSection(char[][] art, int minCol, int minRow, int maxCol, int maxRow) {
        int rows = maxRow - minRow + 1;
        int cols = maxCol - minCol + 1;

        char[][] section = new char[cols][rows];

        for (int x = 0; x < cols; x++) {
            section[x] = Arrays.copyOfRange(art[minCol + x], minRow, maxRow + 1);
        }

        return section;
    }

    /*
     * Converts lines of strings to a 2d char array box.
     */
    private static char[][] toCharArray(List<String> lines) {
        int rows = lines.size();
        int cols = lines.stream().map(String::length).max(Integer::compare).orElse(0);

        char[][] art = new char[cols][rows];

        for (int y = 0; y < rows; y++) {
            String line = lines.get(y);

            for (int x = 0; x < cols; x++) {
                art[x][y] = line.length() > x ? line.charAt(x) : ' ';
            }
        }

        return art;
    }

    /**
     * Scans the 2d character array for seperate areas. Each area found
     * is returned.
     *
     * @return a list of areas found, never {@code null} but can be empty
     */
    private List<Area> findAllAreas() {
        List<Area> areas = new ArrayList<>();

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (areaMap[x][y] == null) {
                    areas.addAll(extractAreas(x, y));
                }
            }
        }

        return areas;
    }

    /**
     * Extracts areas from the given location. It does this by flood filling the
     * area (to mark an area as completed) and recursively extracting any incomplete
     * areas if the fill did not fill some bounded of inner area.
     * <p>
     * Any found areas are marked in the area map and returned as a list.
     *
     * @param startX the x position to check for unassigned areas
     * @param startY the y position to check for unassigned areas
     * @return a list with any newly found areas, never {@code null} but can be empty
     */
    private List<Area> extractAreas(int startX, int startY) {

        /*
         * Set up vars and structure for a flood fill:
         */

        record Location(int x, int y) {
        }

        Deque<Location> queue = new ArrayDeque<>();

        queue.add(new Location(startX, startY));

        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int maxCol = Integer.MIN_VALUE;

        /*
         * Flood fills the box at (x, y) and sets all touched locations to MARKED:
         */

        while (!queue.isEmpty()) {
            Location current = queue.pop();
            int x = current.x;
            int y = current.y;

            if (x >= 0 && x < cols && y >= 0 && y < rows && areaMap[x][y] == null && isFillable(x, y)) {
                areaMap[x][y] = MARKED;

                queue.push(new Location(x - 1, y));
                queue.push(new Location(x, y - 1));
                queue.push(new Location(x + 1, y));
                queue.push(new Location(x, y + 1));

                minRow = Math.min(minRow, y);
                minCol = Math.min(minCol, x);
                maxRow = Math.max(maxRow, y);
                maxCol = Math.max(maxCol, x);
            }
        }

        if (minRow == Integer.MAX_VALUE) {
            return List.of(); // early return if no area was found
        }

        String text = extractTextAndFinalizeArea();

        /*
         * The fill may have left an inner area unfilled. Recursively parse the area
         * to see if there were any nested areas:
         */

        AsciiArtParser nestedParser = new AsciiArtParser(extractSection(art, minCol, minRow, maxCol, maxRow));
        Bounds bounds = nestedParser.getBounds();

        /*
         * Create either a box or a margin area, depending on whether an inner area existed.
         */

        Area area = text.isEmpty() ? new Area("(empty-" + ++emptyCount + ")", null, 0, 0, null)
            : bounds.isEmpty() ? createBox(text)
                : createMargin(text, bounds);

        replaceArea(MARKED, area);

        /*
         * Transform any areas made by the recursive parser to areas for
         * this parser, and place them into this parser's area map:
         */

        List<Area> discoveredAreas = new ArrayList<>();

        discoveredAreas.add(area);

        for (Area child : nestedParser.areas) {
            // transform recursive parser areas to correct space for this parser:
            Area transformed = new Area(child.name, area, child.width, child.height, child.margin);

            discoveredAreas.add(transformed);

            // brute force find where the child area applied, and set the transformed area into this parser's map:
            for (int y = minRow; y <= maxRow; y++) {
                for (int x = minCol; x <= maxCol; x++) {
                    if (nestedParser.areaMap[x - minCol][y - minRow] == child) {
                        areaMap[x][y] = transformed;
                    }
                }
            }
        }

        return discoveredAreas;
    }

    /**
     * Creates a new box {@link Area} given an extracted piece of text. If the text
     * contained a name and a size, the area gets this name and size. If only a size
     * was found, the area is considered unnamed.
     *
     * @param text the text found, cannot be {@code null}
     * @return a new area, never {@code null}
     */
    private Area createBox(String text) {
        String[] parts = text.split(":");
        String name = parts.length > 1 ? parts[0].trim() : "(unnamed-" + ++unnamedCount + ")";
        String sizes = parts.length > 1 ? parts[1] : parts[0];
        String[] dimension = sizes.trim().split("x");

        if (dimension.length == 2) {
            try {
                double w = Double.parseDouble(dimension[0]);
                double h = Double.parseDouble(dimension[1]);

                return new Area(name, null, w, h, null);
            }
            catch (NumberFormatException e) {
                // Ignore
            }
        }

        throw new IllegalArgumentException("Text could not be parsed as a box or spacer: " + text);
    }

    /**
     * Creates a new margin {@link Area} given an extracted piece of text consisting
     * of four numbers and a bounds. Margins cannot have assigned names but get a
     * auto numbered name of the form "(margin-XX)".
     *
     * @param text the text found, cannot be {@code null}
     * @param bounds the margin's bounds, cannot be {@code null}
     * @return a new area, never {@code null}
     */
    private Area createMargin(String text, Bounds bounds) {
        String[] insets = text.trim().split(" ");

        if (insets.length == 4) {
            try {
                double top = Double.parseDouble(insets[0]);
                double left = Double.parseDouble(insets[1]);
                double right = Double.parseDouble(insets[2]);
                double bottom = Double.parseDouble(insets[3]);
                double w = left + bounds.width + right;
                double h = top + bounds.height + bottom;

                return new Area("(margin-" + ++marginCount + ")", null, w, h, new Insets(top, left, right, bottom));
            }
            catch (NumberFormatException e) {
                // Ignore
            }
        }

        throw new IllegalArgumentException("Text could not be parsed as a margin: " + text + "\nIt must be four numbers representing the top, left, right and bottom inset sizes");
    }

    /**
     * Replaces one area with another.
     *
     * @param old the area to replace
     * @param newArea the new area
     */
    private void replaceArea(Area old, Area newArea) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (areaMap[x][y] == old) {
                    areaMap[x][y] = newArea;
                }
            }
        }
    }

    /**
     * Extracts the text from the MARKED area, then fills the area as completed.
     *
     * @return any text found in the area, never {@code null} but may be empty
     */
    private String extractTextAndFinalizeArea() {
        String text = "";

        /*
         * Scans the filled area for any text within it and extracts it:
         */

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (areaMap[x][y] == MARKED) {
                    char ch = art[x][y];

                    if (ch == ' ') {
                        if (!text.endsWith(" ") && !text.isEmpty()) {
                            text += ' ';
                        }
                    }
                    else if (isFillable(x, y)) {
                        text += ch;
                    }

                    art[x][y] = '+';
                }
            }
        }

        return text.trim();
    }

    /**
     * Checks if the given character in the character map is considered fillable
     * (not a boundary).
     *
     * @param x the x position to check
     * @param y the y position to check
     * @return {@code true} if empty, otherwise {@code false}
     */
    private boolean isFillable(int x, int y) {
        char ch = art[x][y];

        return Character.isLetterOrDigit(ch) || ch == '.' || ch == ' ' || ch == ':' || (ch == '-' && x + 1 < cols && Character.isDigit(art[x + 1][y]));
    }

    private Extent calculateLeftExtent(Area area, Map<Area, Extent> areaExtents) {
        if (areaExtents.containsKey(area)) {
            return areaExtents.get(area);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (areaMap[x][y] == area) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }

        Set<Area> leftAreas = new HashSet<>();

        for (int y = minY; y <= maxY; y++) {
            if (minX - 2 >= 0 && areaMap[minX - 1][y] == null && areaMap[minX - 2][y] != area && areaMap[minX - 2][y] != null) {
                leftAreas.add(areaMap[minX - 2][y]);
            }
        }

        double x = leftAreas.stream()
            .map(a -> {
                Extent e = calculateLeftExtent(a, areaExtents);

                return area.parent == a ? e.start + a.margin.left : e.start + e.length;
            })
            .max(Double::compare)
            .orElse(0.0);

        Extent extent = new Extent(x, area.width);

        areaExtents.put(area, extent);

        return extent;
    }

    private Extent calculateTopExtent(Area area, Map<Area, Extent> areaExtents) {
        if (areaExtents.containsKey(area)) {
            return areaExtents.get(area);
        }

        int minY = Integer.MAX_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;

        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                if (areaMap[x][y] == area) {
                    minY = Math.min(minY, y);
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                }
            }
        }

        Set<Area> topAreas = new HashSet<>();

        for (int x = minX; x <= maxX; x++) {
            if (minY - 2 >= 0 && areaMap[x][minY - 1] == null && areaMap[x][minY - 2] != area && areaMap[x][minY - 2] != null) {
                topAreas.add(areaMap[x][minY - 2]);
            }
        }

        double y = topAreas.stream()
            .map(a -> {
                Extent e = calculateTopExtent(a, areaExtents);

                return area.parent == a ? e.start + a.margin.top : e.start + e.length;
            })
            .max(Double::compare)
            .orElse(0.0);

        Extent extent = new Extent(y, area.height);

        areaExtents.put(area, extent);

        return extent;
    }
}