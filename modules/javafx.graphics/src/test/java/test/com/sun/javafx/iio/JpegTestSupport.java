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

package test.com.sun.javafx.iio;

import com.sun.javafx.iio.ImageFrame;
import com.sun.javafx.iio.ImageLoadListener;
import com.sun.javafx.iio.ImageLoader;
import com.sun.javafx.iio.ImageMetadata;
import com.sun.javafx.iio.ImageStorage.ImageType;
import com.sun.javafx.iio.jpeg.JPEGImageLoaderFactory;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Shared fixtures for the JPEG parity tests: corpus synthesis, decode helpers, the recording
 * listener and the one capture routine that both {@link JpegCorpusGenerator} and
 * {@link JpegDecodeParityTest} run.
 * <p>
 * The capture routine lives here on purpose. A golden is only evidence if the value written by the
 * generator and the value compared by the test come out of the same code; two copies of "decode and
 * summarise" drift, and a drifted golden proves nothing about the migration.
 */
public final class JpegTestSupport {

    /** Corpus directory, relative to this class's package on the classpath. */
    static final String RESOURCE_PREFIX = "jpeg/";

    /** Written by the generator, read by the parity tests. */
    static final String GOLDEN_FILE = "jpeg-goldens.txt";

    static final String BASELINE = "baseline-rgb-64x48.jpg";
    static final String GRAY = "gray-32x32.jpg";
    static final String PROGRESSIVE = "progressive-48x32.jpg";
    static final String ODD = "odd-17x13.jpg";
    static final String ICC = "icc-32x24.jpg";
    static final String ICC_INVALID = "icc-invalid-32x24.jpg";
    static final String ADOBE_UNKNOWN = "adobe-unknown-64x48.jpg";
    static final String CMYK = "cmyk-32x24.jpg";
    static final String CORRUPT = "corrupt.jpg";

    /**
     * A derived corpus member, not a file: {@link #BASELINE} cut short inside its entropy-coded
     * data. Derived rather than checked in so the cut stays visibly tied to the file it comes from;
     * the golden hashes the derived bytes, so changing the derivation is a mismatch, not a silent
     * new fixture.
     */
    static final String TRUNCATED = "truncated";

    /** How much entropy-coded data {@link #TRUNCATED} keeps after SOS. */
    static final int TRUNCATED_SCAN_BYTES = 64;

    /**
     * Corpus members the generator always produces. Every one of these must be present for the
     * parity test to run; a missing file is a capture that did not happen, never a skip.
     * <p>
     * {@link #CMYK} is deliberately absent: whether it exists depends on what the running JDK's JPEG
     * writer will emit, so the generator records in the golden file's {@code images=} line which
     * members it actually wrote, and the parity test tests exactly those.
     */
    static final List<String> REQUIRED_IMAGES = List.of(BASELINE, GRAY, PROGRESSIVE, ODD, ICC,
            ICC_INVALID, ADOBE_UNKNOWN, CORRUPT);

    /** Quality used for every synthesised image; fixed so the corpus is reproducible. */
    static final float QUALITY = 0.75f;

    /** ICC APP2, Adobe APP14 and comment markers. */
    private static final int APP2 = 0xE2;
    private static final int APP14 = 0xEE;
    private static final int COM = 0xFE;

    /** {@code "ICC_PROFILE\0"}, the 12-byte tag {@code marker_is_icc} looks for. */
    private static final byte[] ICC_TAG = {'I', 'C', 'C', '_', 'P', 'R', 'O', 'F', 'I', 'L', 'E', 0};

    private JpegTestSupport() {
    }

    // ---------------------------------------------------------------------------------------------
    // Decoding
    // ---------------------------------------------------------------------------------------------

    /**
     * One decode request. {@code width == 0 && height == 0} asks for the natural size, which is the
     * idiom the other iio tests use ({@code loader.load(0, 0, 0, true, ...)}).
     */
    record Variant(String name, double width, double height, boolean preserveAspectRatio,
            boolean smooth) {
    }

    /** Natural size. */
    static final Variant FULL = new Variant("full", 0, 0, true, true);

    /**
     * Exactly half of {@link #BASELINE}'s 64x48. {@code startDecompression} picks
     * {@code scale_denom = 2} for this and libjpeg produces 32x24 directly, so this variant pins the
     * pure-libjpeg downscale with no Java resampling afterwards.
     */
    static final Variant HALF = new Variant("half", 32, 24, true, true);

    /**
     * 40x30 from 64x48: {@code max_scale} is 0.625, so {@code scale_denom} stays 1, libjpeg returns
     * 64x48 and {@code ImageTools.scaleImage} does the rest. Pins the mixed path, both filters.
     */
    static final Variant W40H30_SMOOTH = new Variant("w40h30smooth", 40, 30, true, true);
    static final Variant W40H30_ROUGH = new Variant("w40h30rough", 40, 30, true, false);

    static List<Variant> variantsFor(String image) {
        if (BASELINE.equals(image)) {
            return List.of(FULL, HALF, W40H30_SMOOTH, W40H30_ROUGH);
        }
        return List.of(FULL);
    }

    /** What a successful decode produced, reduced to the things a golden can pin. */
    record Decoded(ImageType type, int width, int height, int stride, byte[] pixels) {
    }

    static ImageLoader newLoader(byte[] jpeg) throws IOException {
        return newLoader(new ByteArrayInputStream(jpeg));
    }

    static ImageLoader newLoader(InputStream stream) throws IOException {
        return JPEGImageLoaderFactory.getInstance().createImageLoader(stream);
    }

    static Decoded decode(byte[] jpeg, Variant variant) throws IOException {
        return decode(new ByteArrayInputStream(jpeg), variant, null);
    }

    /**
     * Runs one decode to completion and disposes the loader, exactly as a caller of
     * {@code ImageStorage} would. The loader is not reusable afterwards - {@code load} disposes it
     * in its own {@code finally} - so every decode gets a fresh loader and a fresh stream.
     */
    static Decoded decode(InputStream stream, Variant variant, ImageLoadListener listener)
            throws IOException {
        ImageLoader loader = newLoader(stream);
        try {
            if (listener != null) {
                loader.addListener(listener);
            }
            ImageFrame frame = loader.load(0, variant.width(), variant.height(),
                    variant.preserveAspectRatio(), variant.smooth(), 1, 1);
            if (frame == null) {
                throw new IOException("load returned null");
            }
            return new Decoded(frame.getImageType(), frame.getWidth(), frame.getHeight(),
                    frame.getStride(), pixels(frame));
        } finally {
            loader.dispose();
        }
    }

    /** The decoded bytes, without disturbing the frame's buffer position. */
    static byte[] pixels(ImageFrame frame) {
        ByteBuffer buffer = ((ByteBuffer) frame.getImageData()).duplicate();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return data;
    }

    /**
     * Decodes {@code jpeg} at {@code variant} and reduces the result - or the failure - to the
     * key/value pairs a golden stores.
     * <p>
     * {@code Throwable} rather than {@code IOException}: the point is to record what the current
     * build does, including a failure the current build is not documented to produce. Nothing in
     * here asserts, so nothing an assertion would raise can be swallowed.
     */
    static Map<String, String> captureVariant(byte[] jpeg, Variant variant) {
        Map<String, String> captured = new LinkedHashMap<>();
        try {
            Decoded decoded = decode(jpeg, variant);
            captured.put("outcome", "ok");
            captured.put("type", decoded.type() == null ? JpegGoldens.NULL : decoded.type().name());
            captured.put("width", Integer.toString(decoded.width()));
            captured.put("height", Integer.toString(decoded.height()));
            captured.put("stride", Integer.toString(decoded.stride()));
            captured.put("length", Integer.toString(decoded.pixels().length));
            captured.put("sha256", sha256Hex(decoded.pixels()));
        } catch (Throwable failure) {
            captured.put("outcome", failure.getClass().getName());
            captured.put("message",
                    failure.getMessage() == null ? JpegGoldens.NULL : failure.getMessage());
        }
        return captured;
    }

    // ---------------------------------------------------------------------------------------------
    // Listener
    // ---------------------------------------------------------------------------------------------

    /**
     * Records every callback in arrival order. Warnings are kept as they arrive, {@code null}
     * included - see {@link JpegWarningOrderTest} for why a {@code null} warning is the current,
     * shipped behaviour and is pinned rather than fixed.
     */
    static final class RecordingListener implements ImageLoadListener {

        private final List<String> events = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<Float> progress = new ArrayList<>();
        private final List<ImageMetadata> metadata = new ArrayList<>();

        @Override
        public void imageLoadProgress(ImageLoader loader, float percentageComplete) {
            progress.add(percentageComplete);
            events.add("P:" + percentageComplete);
        }

        @Override
        public void imageLoadWarning(ImageLoader loader, String message) {
            warnings.add(message);
            events.add("W:" + (message == null ? JpegGoldens.NULL : message));
        }

        @Override
        public void imageLoadMetaData(ImageLoader loader, ImageMetadata data) {
            metadata.add(data);
            events.add("META");
        }

        /** Encoded callbacks in arrival order: {@code META}, {@code W:<text>}, {@code P:<float>}. */
        List<String> events() {
            return List.copyOf(events);
        }

        /** Warnings in arrival order; may contain {@code null} elements. */
        List<String> warnings() {
            return new ArrayList<>(warnings);
        }

        List<Float> progress() {
            return List.copyOf(progress);
        }

        List<ImageMetadata> metadata() {
            return List.copyOf(metadata);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Hashing
    // ---------------------------------------------------------------------------------------------

    static String sha256Hex(byte[] data) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required of every JDK", e);
        }
        return hex(digest.digest(data));
    }

    static String hex(byte[] data) {
        StringBuilder text = new StringBuilder(data.length * 2);
        for (byte b : data) {
            text.append(Character.forDigit((b >> 4) & 0xF, 16));
            text.append(Character.forDigit(b & 0xF, 16));
        }
        return text.toString();
    }

    /** First {@code count} bytes as hex, for a failure message that has to be readable. */
    static String head(byte[] data, int count) {
        int length = Math.min(count, data.length);
        byte[] prefix = new byte[length];
        System.arraycopy(data, 0, prefix, 0, length);
        return hex(prefix) + (length < data.length ? "..." : "");
    }

    // ---------------------------------------------------------------------------------------------
    // Corpus synthesis
    // ---------------------------------------------------------------------------------------------

    /**
     * A horizontal red ramp, a vertical green ramp and a 4x4 blue checker. Deterministic, and busy
     * enough that a wrong IDCT, a wrong upsampling ratio or a wrong colour conversion all change the
     * bytes.
     */
    static BufferedImage rgbPattern(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = x * 255 / Math.max(1, width - 1);
                int green = y * 255 / Math.max(1, height - 1);
                int blue = ((x / 4 + y / 4) % 2 == 0) ? 32 : 224;
                image.setRGB(x, y, (red << 16) | (green << 8) | blue);
            }
        }
        return image;
    }

    /**
     * Grey ramp plus a 3x3 checker, written through the raster so the sample values are exactly
     * these numbers. {@code setRGB} on a {@code TYPE_BYTE_GRAY} image would push them through the
     * sRGB-to-linear-grey conversion and make the corpus depend on the JDK's colour management.
     */
    static BufferedImage grayPattern(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (x * 255 / Math.max(1, width - 1) + y * 255 / Math.max(1, height - 1)) / 2;
                if (((x / 3 + y / 3) & 1) == 0) {
                    value = Math.min(255, value + 40);
                }
                raster.setSample(x, y, 0, value);
            }
        }
        return image;
    }

    /**
     * High-entropy RGB from a fixed linear congruential generator, so a JPEG of it does not compress
     * away to a couple of kilobytes. The stream tests need a file that spans several 4096-byte
     * {@code STREAMBUF_SIZE} fills.
     */
    static BufferedImage noisePattern(int width, int height, long seed) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        long state = seed;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                state = state * 6364136223846793005L + 1442695040888963407L;
                image.setRGB(x, y, (int) (state >>> 24) & 0xFFFFFF);
            }
        }
        return image;
    }

    static byte[] writeJpeg(BufferedImage image, float quality, boolean progressive) {
        return write(new IIOImage(image, null, null), quality, progressive);
    }

    static byte[] writeJpeg(Raster raster, float quality) {
        return write(new IIOImage(raster, null, null), quality, false);
    }

    private static byte[] write(IIOImage image, float quality, boolean progressive) {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("this JDK has no JPEG writer, so the corpus cannot be"
                    + " generated here");
        }
        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            param.setProgressiveMode(
                    progressive ? ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            // MemoryCacheImageOutputStream, not ImageIO.createImageOutputStream: no temp-file cache,
            // so generation does not depend on a writable java.io.tmpdir.
            try (MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bytes)) {
                writer.setOutput(out);
                writer.write(null, image, param);
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            writer.dispose();
        }
    }

    /**
     * A 4-band interleaved raster of synthetic CMYK. Whether the JDK writer accepts it is a property
     * of the JDK, so the generator treats a failure here as "no CMYK member in the corpus" and says
     * so, rather than pretending to have captured one.
     */
    static Raster cmykRaster(int width, int height) {
        WritableRaster raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, width, height, 4,
                new Point(0, 0));
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, x * 255 / Math.max(1, width - 1));
                raster.setSample(x, y, 1, y * 255 / Math.max(1, height - 1));
                raster.setSample(x, y, 2, ((x / 4 + y / 4) % 2 == 0) ? 32 : 224);
                raster.setSample(x, y, 3, (x + y) % 64);
            }
        }
        return raster;
    }

    // ---------------------------------------------------------------------------------------------
    // JPEG byte surgery
    // ---------------------------------------------------------------------------------------------

    /**
     * Inserts {@code segment} directly after the first marker segment of {@code jpeg} - the JFIF
     * APP0 that the JDK writer always emits - so the inserted marker is read before SOF and the JFIF
     * marker keeps its place at the front.
     */
    static byte[] insertAfterFirstSegment(byte[] jpeg, byte[] segment) {
        int at = firstSegmentEnd(jpeg);
        byte[] out = new byte[jpeg.length + segment.length];
        System.arraycopy(jpeg, 0, out, 0, at);
        System.arraycopy(segment, 0, out, at, segment.length);
        System.arraycopy(jpeg, at, out, at + segment.length, jpeg.length - at);
        return out;
    }

    private static int firstSegmentEnd(byte[] jpeg) {
        if (jpeg.length < 6 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            throw new IllegalArgumentException("not a JPEG stream: no SOI");
        }
        if ((jpeg[2] & 0xFF) != 0xFF) {
            throw new IllegalArgumentException("no marker directly after SOI");
        }
        int marker = jpeg[3] & 0xFF;
        if (marker < 0xC0 || marker == 0xD8 || marker == 0xD9 || (marker >= 0xD0 && marker <= 0xD7)) {
            throw new IllegalArgumentException("first marker 0x" + Integer.toHexString(marker)
                    + " has no length field");
        }
        int length = ((jpeg[4] & 0xFF) << 8) | (jpeg[5] & 0xFF);
        return 4 + length;
    }

    /** {@code FF <marker> <length-hi> <length-lo> <payload>}, length inclusive of itself. */
    static byte[] markerSegment(int marker, byte[] payload) {
        int length = payload.length + 2;
        if (length > 0xFFFF) {
            throw new IllegalArgumentException("marker payload too long: " + payload.length);
        }
        byte[] segment = new byte[payload.length + 4];
        segment[0] = (byte) 0xFF;
        segment[1] = (byte) marker;
        segment[2] = (byte) (length >> 8);
        segment[3] = (byte) length;
        System.arraycopy(payload, 0, segment, 4, payload.length);
        return segment;
    }

    /**
     * An Adobe APP14 payload. libjpeg's {@code examine_app14} wants at least 12 bytes starting with
     * {@code "Adobe"}; the transform byte is the last one.
     */
    static byte[] adobeSegment(int transform) {
        byte[] payload = {'A', 'd', 'o', 'b', 'e', 0x00, 0x64, 0, 0, 0, 0, (byte) transform};
        return markerSegment(APP14, payload);
    }

    /** An ICC APP2 chunk: {@code "ICC_PROFILE\0"}, sequence number, chunk count, then profile bytes. */
    static byte[] iccSegment(int sequence, int count, byte[] profile) {
        byte[] payload = new byte[ICC_TAG.length + 2 + profile.length];
        System.arraycopy(ICC_TAG, 0, payload, 0, ICC_TAG.length);
        payload[ICC_TAG.length] = (byte) sequence;
        payload[ICC_TAG.length + 1] = (byte) count;
        System.arraycopy(profile, 0, payload, ICC_TAG.length + 2, profile.length);
        return markerSegment(APP2, payload);
    }

    /**
     * A comment segment of {@code payloadLength} zero bytes. COM is not one of the markers this
     * decoder saves, so libjpeg reaches it through {@code skip_variable}, which is the only route to
     * the source manager's {@code skip_input_data} - and therefore the only way a test can drive a
     * short {@code InputStream.skip}. Make it larger than the 4096-byte stream buffer and the skip
     * cannot be served from the buffer.
     */
    static byte[] commentSegment(int payloadLength) {
        return markerSegment(COM, new byte[payloadLength]);
    }

    /**
     * Truncates {@code jpeg} to {@code keep} bytes. Callers that want the cut to land inside the
     * entropy-coded data should base {@code keep} on {@link #startOfScan}.
     */
    static byte[] truncate(byte[] jpeg, int keep) {
        if (keep < 0 || keep > jpeg.length) {
            throw new IllegalArgumentException("keep out of range: " + keep);
        }
        byte[] out = new byte[keep];
        System.arraycopy(jpeg, 0, out, 0, keep);
        return out;
    }

    /**
     * Offset of the first byte of entropy-coded data, found by walking the marker chain rather than
     * searching for the {@code FF DA} byte pair - that pair occurs inside quantisation and Huffman
     * tables often enough to matter.
     */
    static int startOfScan(byte[] jpeg) {
        if (jpeg.length < 4 || (jpeg[0] & 0xFF) != 0xFF || (jpeg[1] & 0xFF) != 0xD8) {
            throw new IllegalArgumentException("not a JPEG stream: no SOI");
        }
        int at = 2;
        while (at + 4 <= jpeg.length) {
            if ((jpeg[at] & 0xFF) != 0xFF) {
                throw new IllegalArgumentException("lost the marker chain at offset " + at);
            }
            int marker = jpeg[at + 1] & 0xFF;
            if (marker == 0xD8 || (marker >= 0xD0 && marker <= 0xD7) || marker == 0x01) {
                at += 2;
                continue;
            }
            if (marker == 0xD9) {
                throw new IllegalArgumentException("EOI before SOS");
            }
            int length = ((jpeg[at + 2] & 0xFF) << 8) | (jpeg[at + 3] & 0xFF);
            if (marker == 0xDA) {
                return at + 2 + length;
            }
            at += 2 + length;
        }
        throw new IllegalArgumentException("no SOS in this stream");
    }

    // ---------------------------------------------------------------------------------------------
    // Resources
    // ---------------------------------------------------------------------------------------------

    /** The corpus member {@code name}, or {@code null} when it is not on the classpath. */
    static byte[] resourceBytes(String name) {
        try (InputStream in = JpegTestSupport.class.getResourceAsStream(RESOURCE_PREFIX + name)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The corpus member {@code name}, or a failure naming the capture command. */
    static byte[] requireResource(String name) {
        byte[] data = resourceBytes(name);
        if (data == null) {
            throw new AssertionError("corpus member " + RESOURCE_PREFIX + name + " is not on the"
                    + " classpath. Capture it with \"mvn -pl modules/javafx.graphics test"
                    + " -Dtest=JpegCorpusGenerator -Djfx.iio.jpeg.capture=true\" and commit the"
                    + " result.");
        }
        return data;
    }

    /** {@link #BASELINE} cut short inside its entropy-coded data. */
    static byte[] truncatedBaseline(byte[] baseline) {
        return truncate(baseline, startOfScan(baseline) + TRUNCATED_SCAN_BYTES);
    }

    /**
     * Assembles the corpus the way both the generator and the parity test see it: the file-backed
     * members named in {@code fileNames}, in that order, followed by the derived members.
     */
    static Map<String, byte[]> assembleCorpus(List<String> fileNames, Function<String, byte[]> source) {
        Map<String, byte[]> corpus = new LinkedHashMap<>();
        for (String name : fileNames) {
            corpus.put(name, source.apply(name));
        }
        byte[] baseline = corpus.get(BASELINE);
        if (baseline == null) {
            throw new IllegalArgumentException("the corpus must contain " + BASELINE);
        }
        corpus.put(TRUNCATED, truncatedBaseline(baseline));
        return corpus;
    }
}
