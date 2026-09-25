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

package test.com.sun.scenario.effect;

import com.sun.javafx.geom.transform.BaseTransform;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import test.com.sun.scenario.effect.DecoraBackend.Result;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * The format and reader of the Decora golden: the native {@code decora_sse} output of every
 * {@link DecoraCorpus#rows() golden row} as rendered on Windows, captured once from that library before it was
 * deleted (2026-09).
 * <p>
 * Only the Windows library was captured. The Linux library ({@code libdecora_sse.so}, built with {@code -ffast-math})
 * was measured once more just before the deletion, on JDK 25, against the same Java peers over the same corpus. The
 * largest channel delta, with the share of differing pixels in the worst row: exact, as on Windows, for
 * {@code BoxBlur}, {@code BoxShadow}, the box shadows, the clipped Gaussian shadows, {@code Brightpass} and
 * {@code Blend} {@code SRC_IN}; one step for the Gaussian blur on 9 of its 20 corpus rows (1.011%), both oversized
 * rows (0.423%) and the 1x48 degenerate row (6.349%), where Windows is exact, and on 22 of its 24 clip-matrix rows
 * (4.917%; Windows 6 rows); one step for the unclipped Gaussian shadow only at radius 25 without spread (0.009%;
 * Windows 0.002%), for motion blur on 4 of 8 rows (0.025%; Windows 2 rows) and for the clipped blur rows (0.004%, as
 * on Windows); one step, as on Windows, for every other generated peer, worst in {@code ColorAdjust} (55.632%;
 * Windows 33.855%), {@code Blend} {@code SOFT_LIGHT} (31.152%; Windows 0.293%), {@code Blend} {@code SRC_ATOP}
 * (10.817%; Windows 10.820%), {@code InvertMask} (7.004%), {@code DisplacementMap} (5.566%; Windows 0.006%),
 * {@code PerspectiveTransform} (5.000%; Windows 1.595%) and {@code PhongLighting} {@code POINT} (2.539%; Windows
 * 1.009%); and up to 95 (64x48) and 114 (257x129) for full contrast, on the pixels whose largest channel it zeroes
 * (finding 1; Windows 144 and 232), measured before the {@code ColorAdjust.jsl} fix that sends those pixels to the
 * grey branch. The {@code linux} bounds in {@link DecoraCorpus.Bound} are the ones that measurement was checked
 * against, not tightened to what it found, so some are looser than measured, such as 3 for the clipped Gaussian
 * shadows, which were exact. The library is gone and the golden holds no Linux or macOS frames, so what Linux and
 * macOS rendered before the deletion can no longer be re-proved; macOS was never measured.
 * <p>
 * The golden is two resources next to this class. {@value #INDEX_RESOURCE} is ASCII with LF line ends: {@code #}
 * header lines (format, provenance, input hashes, the md5 and size of the frames file, row and tier counts, and last
 * the md5 of the data lines), then one data line per row, in {@link DecoraCorpus#rows()} order:
 * <pre>
 * effect | params | WxH | x,y,w,h | tx | tier | nativeSha256 | javaSha256 | offset | length | bound | cause
 *        | fullMaxD | fullDiff | explained | sseSelf | kernelSha256
 * </pre>
 * {@code x,y,w,h} are the result bounds both backends returned; {@code tx} is {@code I} for an identity result
 * transform, else its six entries {@code mxx,myx,mxy,myy,mxt,myt}. A frame is a result's pixels as big-endian ARGB
 * ints, row-major, and the two SHA-256 columns hash the native and the Java frame of the capture. {@code bound} is
 * the row's Windows bound, {@code cause} a cause id or {@code -}, {@code fullMaxD} and {@code fullDiff} the largest
 * channel delta and the number of differing pixels between the two frames, {@code explained} the number of differing
 * pixels the cause explained, {@code sseSelf} the native clipped render's largest distance from its own unclipped
 * render (clipped rows, else {@code -}), and {@code kernelSha256} the hash of the {@code Math}-derived kernel
 * inputs (Gaussian weights or the displacement float map, else {@code -}).
 * <p>
 * {@value #FRAMES_RESOURCE} concatenates one zlib stream ({@link Deflater#BEST_COMPRESSION}) per stored row; offset
 * and length locate it. A row is stored in one of three tiers:
 * <ul>
 * <li>{@code P}: the full native frame, each row of it filtered like PNG "Sub" ({@code out[i] = in[i] - in[i-4]}
 * within the row, the first pixel raw). Every 64x48 row, every translated and degenerate row, and every row in
 * {@link DecoraCorpus#FULL_FRAME_ROWS} and of the full-contrast and clipped-blur families.</li>
 * <li>{@code H}: nothing; any other row whose native frame equals its Java frame bit for bit.</li>
 * <li>{@code S}: any other row whose frames differ, as the ascending list of differing pixels, each a big-endian
 * row-major index and the native ARGB; the native frame is the capture's Java frame with those pixels replaced.</li>
 * </ul>
 * A result whose width or height is not positive (a clip disjoint from the result: {@code Rectangle.intersectWith}
 * leaves negative extents) is an empty frame of zero pixels, whatever pixel array the harness allocated for it;
 * its bounds column keeps the negative extents, which a reader compares exactly.
 */
final class DecoraGoldens {

    static final String INDEX_RESOURCE = "decora-sse-win-golden.txt";
    static final String FRAMES_RESOURCE = "decora-sse-win-golden.bin";
    static final int FORMAT = 2;
    static final String FORMAT_LINE = "# decora-sse-win-golden format " + FORMAT;
    static final int COLUMNS = 17;

    private static final HexFormat HEX = HexFormat.of();

    private DecoraGoldens() {
    }

    /** How a row's native frame is stored. */
    enum Tier {
        P, S, H
    }

    /** One data line of the index; {@code offset} and {@code length} are -1 for an H row, {@code sseSelf} for none. */
    record Entry(String effect, String params, String size, int x, int y, int width, int height, String tx, Tier tier,
                 String nativeSha256, String javaSha256, long offset, int length, int bound, String cause,
                 int fullMaxD, long fullDiff, long explained, int sseSelf, String kernelSha256) {

        String key() {
            return effect + DecoraCorpus.KEY_SEPARATOR + params + DecoraCorpus.KEY_SEPARATOR + size;
        }

        String bounds() {
            return x + "," + y + "," + width + "," + height;
        }

        static Entry parse(String line) {
            String[] c = line.split(" \\| ", -1);
            if (c.length != COLUMNS) {
                throw new AssertionError("golden data line has " + c.length + " columns, not " + COLUMNS + ": "
                        + line);
            }
            String[] b = c[3].split(",", -1);
            if (b.length != 4) {
                throw new AssertionError("golden bounds column is not x,y,w,h: " + line);
            }
            Tier tier = Tier.valueOf(c[5]);
            boolean stored = tier != Tier.H;
            return new Entry(c[0], c[1], c[2], Integer.parseInt(b[0]), Integer.parseInt(b[1]),
                    Integer.parseInt(b[2]), Integer.parseInt(b[3]), c[4], tier, c[6], c[7],
                    stored ? Long.parseLong(c[8]) : -1L, stored ? Integer.parseInt(c[9]) : -1,
                    Integer.parseInt(c[10]), c[11], Integer.parseInt(c[12]), Long.parseLong(c[13]),
                    Long.parseLong(c[14]), c[15].equals("-") ? -1 : Integer.parseInt(c[15]), c[16]);
        }
    }

    /** A parsed index: its header lines (with the {@code "# "} prefix) and its entries in file order. */
    record Index(List<String> headers, List<Entry> entries) {

        /** The single header line starting with {@code "# " + prefix + " "}, without that prefix. */
        String header(String prefix) {
            String start = "# " + prefix + " ";
            String found = null;
            for (String h : headers) {
                if (h.startsWith(start)) {
                    if (found != null) {
                        throw new AssertionError("golden header '" + prefix + "' appears twice");
                    }
                    found = h.substring(start.length());
                }
            }
            if (found == null) {
                throw new AssertionError("golden header '" + prefix + "' is missing");
            }
            return found;
        }

        /** Every header line starting with {@code "# " + prefix + " "}, without that prefix. */
        List<String> headers(String prefix) {
            String start = "# " + prefix + " ";
            List<String> found = new ArrayList<>();
            for (String h : headers) {
                if (h.startsWith(start)) {
                    found.add(h.substring(start.length()));
                }
            }
            return found;
        }
    }

    /**
     * Parses an index: ASCII, LF line ends with a final LF, the format line first, header lines before data lines,
     * and the {@code data-md5} header equal to the md5 of the data lines (each with its LF).
     *
     * @throws AssertionError naming what is wrong, {@code data-md5} for an edited data line
     */
    static Index parse(byte[] bytes) {
        for (byte b : bytes) {
            if (b < 0 || b == '\r' || b == '\t') {
                throw new AssertionError("golden index holds a non-ASCII, CR or TAB byte");
            }
        }
        String text = new String(bytes, StandardCharsets.US_ASCII);
        if (!text.endsWith("\n")) {
            throw new AssertionError("golden index does not end with LF");
        }
        List<String> headers = new ArrayList<>();
        List<String> data = new ArrayList<>();
        for (String line : text.substring(0, text.length() - 1).split("\n", -1)) {
            if (line.startsWith("#")) {
                if (!data.isEmpty()) {
                    throw new AssertionError("golden header line after the first data line: " + line);
                }
                headers.add(line);
            } else {
                data.add(line);
            }
        }
        if (headers.isEmpty() || !headers.get(0).equals(FORMAT_LINE)) {
            throw new AssertionError("golden index does not start with '" + FORMAT_LINE + "'");
        }
        Index index = new Index(List.copyOf(headers), List.of());
        String expectedDataMd5 = index.header("data-md5");
        StringBuilder joined = new StringBuilder(text.length());
        for (String line : data) {
            joined.append(line).append('\n');
        }
        String actualDataMd5 = md5(joined.toString().getBytes(StandardCharsets.US_ASCII));
        if (!actualDataMd5.equals(expectedDataMd5)) {
            throw new AssertionError("golden data-md5 mismatch: header " + expectedDataMd5 + ", data lines "
                    + actualDataMd5 + " - a data line was edited");
        }
        List<Entry> entries = new ArrayList<>(data.size());
        for (String line : data) {
            entries.add(Entry.parse(line));
        }
        return new Index(index.headers(), List.copyOf(entries));
    }

    /** The {@code frames} header value for a frames file: {@code "<name> <bytes> B md5 <md5>"}. */
    static String framesHeader(byte[] frames) {
        return FRAMES_RESOURCE + " " + frames.length + " B md5 " + md5(frames);
    }

    /** The golden record of {@code entry} inside {@code frames}. */
    static byte[] record(Entry entry, byte[] frames) {
        if (entry.tier() == Tier.H) {
            throw new IllegalArgumentException("an H row has no record: " + entry.key());
        }
        if (entry.offset() < 0 || entry.length() < 0 || entry.offset() + entry.length() > frames.length) {
            throw new AssertionError("golden record of " + entry.key() + " lies outside the frames file");
        }
        byte[] record = new byte[entry.length()];
        System.arraycopy(frames, (int) entry.offset(), record, 0, entry.length());
        return record;
    }

    /** Pixels of a result frame: zero when the width or the height is not positive. */
    static int pixelCount(int width, int height) {
        return width <= 0 || height <= 0 ? 0 : Math.multiplyExact(width, height);
    }

    /** The frame of a result: its compact pixels, or no pixels when its width or height is not positive. */
    static int[] frame(Result result) {
        int count = pixelCount(result.width(), result.height());
        if (count == 0) {
            return new int[0];
        }
        if (result.pixels().length != count) {
            throw new IllegalStateException("result holds " + result.pixels().length + " pixels for "
                    + result.width() + "x" + result.height());
        }
        return result.pixels();
    }

    /** {@code I} for an identity transform, else {@code mxx,myx,mxy,myy,mxt,myt}. */
    static String tx(BaseTransform transform) {
        return transform.isIdentity() ? "I" : DecoraCorpus.matrix(transform);
    }

    static byte[] bytes(int[] frame) {
        byte[] out = new byte[frame.length * 4];
        for (int i = 0; i < frame.length; i++) {
            putInt(out, i * 4, frame[i]);
        }
        return out;
    }

    static int[] ints(byte[] bytes) {
        if (bytes.length % 4 != 0) {
            throw new AssertionError("frame of " + bytes.length + " bytes is not whole ints");
        }
        int[] frame = new int[bytes.length / 4];
        for (int i = 0; i < frame.length; i++) {
            frame[i] = getInt(bytes, i * 4);
        }
        return frame;
    }

    static String sha256(int[] frame) {
        return sha256(bytes(frame));
    }

    static String sha256(byte[] bytes) {
        return HEX.formatHex(digest("SHA-256").digest(bytes));
    }

    static String md5(byte[] bytes) {
        return HEX.formatHex(digest("MD5").digest(bytes));
    }

    /**
     * The {@code kernelSha256} column of a row rendered on a backend: the hash of the Gaussian pass weights that
     * backend's peers were handed, the float map hash for a {@code DisplacementMap} row, else {@code -} (the other
     * peers compute their {@code Math} per pixel, which cannot be hashed without running them).
     */
    static String kernelSha256(DecoraCorpus.GoldenRow row, List<float[]> gaussianPassWeights) {
        if (!gaussianPassWeights.isEmpty()) {
            return kernelSha256(gaussianPassWeights);
        }
        return row.effect().equals("DisplacementMap") ? floatMapSha256() : "-";
    }

    /**
     * The hash of Gaussian pass weights, in pass order: per pass its index and weight count as big-endian ints,
     * then the raw bits of every weight.
     */
    static String kernelSha256(List<float[]> passWeights) {
        MessageDigest sha = digest("SHA-256");
        byte[] word = new byte[4];
        for (int pass = 0; pass < passWeights.size(); pass++) {
            float[] weights = passWeights.get(pass);
            putInt(word, 0, pass);
            sha.update(word);
            putInt(word, 0, weights.length);
            sha.update(word);
            for (float w : weights) {
                putInt(word, 0, Float.floatToRawIntBits(w));
                sha.update(word);
            }
        }
        return HEX.formatHex(sha.digest());
    }

    /** The hash of the {@code DisplacementMap} recipes' float map samples, raw bits big-endian. */
    static String floatMapSha256() {
        float[] samples = DecoraCorpus.displacementSamples();
        byte[] bytes = new byte[samples.length * 4];
        for (int i = 0; i < samples.length; i++) {
            putInt(bytes, i * 4, Float.floatToRawIntBits(samples[i]));
        }
        return sha256(bytes);
    }

    /** The hash of a source pattern, as the {@code input pattern} header records it. */
    static String patternSha256(int width, int height, long seed) {
        return sha256(DecoraCorpus.pattern(width, height, seed));
    }

    /** The filtered bytes of a P record, before {@link #unfilter}. */
    static byte[] inflateFull(byte[] record, int width, int height) {
        byte[] filtered = inflate(record);
        long expected = 4L * pixelCount(width, height);
        if (filtered.length != expected) {
            throw new AssertionError("P record inflates to " + filtered.length + " bytes, not " + expected);
        }
        return filtered;
    }

    /** Reverses the row filter of a P record into a frame. */
    static int[] unfilter(byte[] filtered, int width, int height) {
        byte[] raw = filtered.clone();
        int rowBytes = pixelCount(width, height) == 0 ? 0 : width * 4;
        for (int start = 0; start < raw.length; start += rowBytes) {
            for (int i = 4; i < rowBytes; i++) {
                raw[start + i] = (byte) (raw[start + i] + raw[start + i - 4]);
            }
        }
        return ints(raw);
    }

    static int[] decodeFull(byte[] record, int width, int height) {
        return unfilter(inflateFull(record, width, height), width, height);
    }

    /** The entries of an S record, validated: whole entries, strictly ascending indices within the frame. */
    static int[][] sparseEntries(byte[] record, int pixelCount) {
        byte[] raw = inflate(record);
        if (raw.length % 8 != 0 || raw.length == 0) {
            throw new AssertionError("S record inflates to " + raw.length + " bytes, not a non-empty list of"
                    + " 8-byte entries");
        }
        int[][] entries = new int[raw.length / 8][2];
        int previous = -1;
        for (int e = 0; e < entries.length; e++) {
            int index = getInt(raw, e * 8);
            if (index <= previous || index >= pixelCount) {
                throw new AssertionError("S record entry " + e + " has index " + index + " after " + previous
                        + " in a frame of " + pixelCount + " pixels");
            }
            entries[e][0] = index;
            entries[e][1] = getInt(raw, e * 8 + 4);
            previous = index;
        }
        return entries;
    }

    /** An S record re-encoded from (possibly modified) entries. */
    static byte[] encodeSparseEntries(int[][] entries) {
        byte[] raw = new byte[entries.length * 8];
        for (int e = 0; e < entries.length; e++) {
            putInt(raw, e * 8, entries[e][0]);
            putInt(raw, e * 8 + 4, entries[e][1]);
        }
        return deflate(raw);
    }

    /** The native frame of an S row: a copy of the Java frame with the record's pixels substituted. */
    static int[] applySparse(byte[] record, int[] javaFrame) {
        int[] nativeFrame = javaFrame.clone();
        for (int[] entry : sparseEntries(record, javaFrame.length)) {
            nativeFrame[entry[0]] = entry[1];
        }
        return nativeFrame;
    }

    static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 2));
            byte[] buffer = new byte[65536];
            while (!deflater.finished()) {
                int n = deflater.deflate(buffer);
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally {
            deflater.end();
        }
    }

    static byte[] inflate(byte[] record) {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(record);
            ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, record.length * 2));
            byte[] buffer = new byte[65536];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && !inflater.finished() && (inflater.needsInput() || inflater.needsDictionary())) {
                    throw new AssertionError("golden record is a truncated zlib stream");
                }
                out.write(buffer, 0, n);
            }
            if (inflater.getRemaining() != 0) {
                throw new AssertionError("golden record has " + inflater.getRemaining() + " bytes after its zlib"
                        + " stream");
            }
            return out.toByteArray();
        } catch (DataFormatException e) {
            throw new AssertionError("golden record is not a zlib stream: " + e.getMessage(), e);
        } finally {
            inflater.end();
        }
    }

    /** Reads a resource next to this class completely; a missing resource fails, it is part of the tree. */
    static byte[] resource(String name) {
        try (InputStream in = DecoraGoldens.class.getResourceAsStream(name)) {
            if (in == null) {
                fail("golden resource " + name + " is missing next to " + DecoraGoldens.class.getName());
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new AssertionError("golden resource " + name + " cannot be read", e);
        }
    }

    static String percent(long part, long total) {
        return String.format(Locale.ROOT, "%.3f", total == 0 ? 0.0 : 100.0 * part / total);
    }

    private static MessageDigest digest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " unavailable", e);
        }
    }

    private static void putInt(byte[] out, int offset, int value) {
        out[offset] = (byte) (value >>> 24);
        out[offset + 1] = (byte) (value >>> 16);
        out[offset + 2] = (byte) (value >>> 8);
        out[offset + 3] = (byte) value;
    }

    private static int getInt(byte[] in, int offset) {
        return ((in[offset] & 0xFF) << 24) | ((in[offset + 1] & 0xFF) << 16) | ((in[offset + 2] & 0xFF) << 8)
                | (in[offset + 3] & 0xFF);
    }
}
