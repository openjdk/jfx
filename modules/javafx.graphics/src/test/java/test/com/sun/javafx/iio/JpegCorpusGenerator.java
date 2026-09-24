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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import test.com.sun.javafx.iio.JpegTestSupport.RecordingListener;
import test.com.sun.javafx.iio.JpegTestSupport.Variant;

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Writes the JPEG corpus and captures the goldens the parity tests compare against.
 * <p>
 * Run once, deliberately, against the JNI build, and commit what it writes:
 * <pre>
 *   mvn -pl modules/javafx.graphics test -Dtest=JpegCorpusGenerator -Djfx.iio.jpeg.capture=true
 * </pre>
 * Without {@code -Djfx.iio.jpeg.capture=true} it aborts, so an ordinary build neither regenerates
 * the corpus nor rewrites the evidence.
 * <p>
 * <b>The goldens are captured from the C implementation while the C implementation still exists.</b>
 * That is the whole point: after {@code jpegloader.c} is replaced, the only way to show the
 * replacement decodes the same pixels is to compare against what the C produced before it was
 * touched. Regenerating a golden because a rewritten decoder disagrees with it destroys the only
 * evidence there is - if the values move, the rewrite changed behaviour, and that is the finding.
 * A regeneration therefore needs {@code -Djfx.iio.jpeg.regenerate=true} on top, and is a change to
 * the corpus, never a fix to a test.
 * <p>
 * Everything here is synthesised from code - no hand-authored binary blobs - so any reviewer can
 * reproduce the corpus byte for byte. The one exception is {@code corrupt.jpg}, copied from
 * {@code tests/system/src/test/resources/test/com/sun/javafx/iio/}, where it has been the sole JPEG
 * fixture in the repository.
 */
public class JpegCorpusGenerator {

    private static final String CAPTURE_PROPERTY = "jfx.iio.jpeg.capture";
    private static final String REGENERATE_PROPERTY = "jfx.iio.jpeg.regenerate";

    /** Where the corpus is committed, relative to the module directory. */
    private static final String SOURCE_DIR = "src/test/resources/test/com/sun/javafx/iio/jpeg";

    /**
     * The copy on the test classpath. Written as well as the source copy so that a capture run and
     * the parity tests in the same surefire fork see the same corpus; without it the goldens would
     * only take effect on the next build.
     */
    private static final String CLASSES_DIR = "target/test-classes/test/com/sun/javafx/iio/jpeg";

    /** The repository's only pre-existing JPEG fixture. */
    private static final String CORRUPT_SOURCE =
            "../../tests/system/src/test/resources/test/com/sun/javafx/iio/corrupt.jpg";

    @Test
    void captureCorpusAndGoldens() throws IOException {
        assumeTrue(Boolean.getBoolean(CAPTURE_PROPERTY), "corpus capture is deliberate: run \"mvn -pl"
                + " modules/javafx.graphics test -Dtest=JpegCorpusGenerator"
                + " -Djfx.iio.jpeg.capture=true\" and commit the corpus and the golden file");
        JpegNatives.require();

        Path module = moduleDirectory();
        Path sourceDir = module.resolve(SOURCE_DIR);
        Path classesDir = module.resolve(CLASSES_DIR);
        Path goldenFile = sourceDir.resolve(JpegTestSupport.GOLDEN_FILE);
        if (Files.exists(goldenFile) && !Boolean.getBoolean(REGENERATE_PROPERTY)) {
            fail("a golden file already exists at " + goldenFile + ". It is the record of what the"
                    + " JNI decoder did, so overwriting it would delete the only evidence the"
                    + " migration can be checked against. If the corpus genuinely has to change, add"
                    + " -Djfx.iio.jpeg.regenerate=true and review the resulting diff as a behaviour"
                    + " change.");
        }

        Map<String, byte[]> files = synthesiseCorpus(module);
        Files.createDirectories(sourceDir);
        Files.createDirectories(classesDir);
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Files.write(sourceDir.resolve(file.getKey()), file.getValue());
            Files.write(classesDir.resolve(file.getKey()), file.getValue());
            System.out.println("corpus: " + file.getKey() + " (" + file.getValue().length + " bytes)");
        }

        Map<String, String> golden = capture(files);
        String text = JpegGoldens.format(golden, header());
        Files.writeString(goldenFile, text, StandardCharsets.UTF_8);
        Files.writeString(classesDir.resolve(JpegTestSupport.GOLDEN_FILE), text,
                StandardCharsets.UTF_8);
        System.out.println();
        System.out.println("golden file: " + goldenFile);
        System.out.println(text);
    }

    // ---------------------------------------------------------------------------------------------
    // Corpus
    // ---------------------------------------------------------------------------------------------

    /**
     * Builds every corpus file. Each entry documents how it is produced and which native path it is
     * there to exercise.
     */
    private Map<String, byte[]> synthesiseCorpus(Path module) throws IOException {
        Map<String, byte[]> files = new LinkedHashMap<>();

        // Baseline sequential, 4:2:0, JFIF. The reference case: everything else is a deviation
        // from this one. 64x48 is a whole number of 16x16 MCUs, so no partial-MCU padding.
        byte[] baseline = JpegTestSupport.writeJpeg(JpegTestSupport.rgbPattern(64, 48),
                JpegTestSupport.QUALITY, false);
        files.put(JpegTestSupport.BASELINE, baseline);

        // Single-component JCS_GRAYSCALE: out_color_space stays JCS_GRAYSCALE, one output
        // component, ImageType.GRAY, stride == width.
        files.put(JpegTestSupport.GRAY, JpegTestSupport.writeJpeg(JpegTestSupport.grayPattern(32, 32),
                JpegTestSupport.QUALITY, false));

        // Progressive: multiple scans, so jpeg_start_decompress reads the entire stream before the
        // first scanline comes out. The progress callbacks therefore arrive on a different schedule
        // from the baseline case, which is exactly what makes it worth pinning.
        files.put(JpegTestSupport.PROGRESSIVE, JpegTestSupport.writeJpeg(
                JpegTestSupport.rgbPattern(48, 32), JpegTestSupport.QUALITY, true));

        // 17x13 with 4:2:0 subsampling: two partial MCUs in each direction. The decoder produces
        // full MCU rows internally and has to drop the padding; an off-by-one in that trim changes
        // the last rows and columns and nothing else, which a whole-buffer hash catches.
        files.put(JpegTestSupport.ODD, JpegTestSupport.writeJpeg(JpegTestSupport.rgbPattern(17, 13),
                JpegTestSupport.QUALITY, false));

        // One APP2 ICC chunk, sequence 1 of 1, carrying the JDK's sRGB profile: the success path of
        // read_icc_profile, which assembles the chunks and hands the bytes to setInputAttributes.
        // JPEGImageLoader stores them in its iccData field and never reads it, so the only thing
        // observable from Java is that decoding is unaffected - which is what the golden records.
        byte[] iccBase = JpegTestSupport.writeJpeg(JpegTestSupport.rgbPattern(32, 24),
                JpegTestSupport.QUALITY, false);
        byte[] profile = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
        files.put(JpegTestSupport.ICC,
                JpegTestSupport.insertAfterFirstSegment(iccBase, JpegTestSupport.iccSegment(1, 1,
                        profile)));

        // Two APP2 chunks that both claim to be sequence 1 of 2: read_icc_profile's duplicate
        // detection. A small synthetic payload, because the profile bytes are never reached.
        byte[] chunk = new byte[64];
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = (byte) i;
        }
        files.put(JpegTestSupport.ICC_INVALID, JpegTestSupport.insertAfterFirstSegment(iccBase,
                concat(JpegTestSupport.iccSegment(1, 2, chunk), JpegTestSupport.iccSegment(1, 2,
                        chunk))));

        // The baseline file plus an Adobe APP14 with transform 0. libjpeg still calls the stream
        // YCbCr because of the JFIF marker, and initDecompressor then overrides both
        // jpeg_color_space and out_color_space to JCS_UNKNOWN because the Adobe transform is not 1.
        // jdcolor falls through to null_convert, so three raw components come out untouched and
        // setInputAttributes maps JCS_UNKNOWN with three components to ImageType.RGB. A second
        // out_color_space, reached deterministically, from a 16-byte edit of a file already in the
        // corpus.
        files.put(JpegTestSupport.ADOBE_UNKNOWN,
                JpegTestSupport.insertAfterFirstSegment(baseline, JpegTestSupport.adobeSegment(0)));

        // Four-component CMYK, if this JDK's writer will emit it. Whether it does is a property of
        // the JDK, not of the decoder, so the corpus records what was captured rather than
        // pretending: the images= line in the golden file lists exactly the members that exist, and
        // the parity test tests exactly those.
        try {
            files.put(JpegTestSupport.CMYK,
                    JpegTestSupport.writeJpeg(JpegTestSupport.cmykRaster(32, 24),
                            JpegTestSupport.QUALITY));
        } catch (Throwable e) {
            System.out.println("no CMYK corpus member: this JDK's JPEG writer refused a 4-band"
                    + " raster (" + e + "). The CMYK and YCCK branches of initDecompressor stay"
                    + " uncovered; say so in the migration's coverage notes rather than assuming"
                    + " they are exercised.");
        }

        // The repository's existing corrupt fixture, copied so this module's tests do not reach into
        // tests/system at run time. LoadCorruptJPEGTest only asserts that loading it does not crash;
        // whatever it actually does is recorded here.
        Path corrupt = module.resolve(CORRUPT_SOURCE).normalize();
        if (!Files.isRegularFile(corrupt)) {
            fail("cannot find the existing corrupt JPEG fixture at " + corrupt + ", so the corpus"
                    + " would silently lose the one case the repository already had");
        }
        files.put(JpegTestSupport.CORRUPT, Files.readAllBytes(corrupt));

        return files;
    }

    // ---------------------------------------------------------------------------------------------
    // Capture
    // ---------------------------------------------------------------------------------------------

    private Map<String, String> capture(Map<String, byte[]> files) {
        Map<String, String> golden = new LinkedHashMap<>();
        golden.put("capture.provenance", System.getProperty("java.vm.name") + " "
                + System.getProperty("java.version") + " on " + System.getProperty("os.name") + " "
                + System.getProperty("os.arch"));
        golden.put("images", JpegGoldens.joinList(new ArrayList<>(files.keySet())));

        Map<String, byte[]> corpus = JpegTestSupport.assembleCorpus(List.copyOf(files.keySet()),
                files::get);
        for (Map.Entry<String, byte[]> member : corpus.entrySet()) {
            String name = member.getKey();
            byte[] jpeg = member.getValue();
            golden.put("image." + name + ".length", Integer.toString(jpeg.length));
            golden.put("image." + name + ".sha256", JpegTestSupport.sha256Hex(jpeg));
            for (Variant variant : JpegTestSupport.variantsFor(name)) {
                String prefix = "decode." + name + "." + variant.name() + ".";
                for (Map.Entry<String, String> entry
                        : JpegTestSupport.captureVariant(jpeg, variant).entrySet()) {
                    golden.put(prefix + entry.getKey(), entry.getValue());
                }
            }
            golden.put("events." + name, JpegGoldens.joinList(recordEvents(jpeg)));
        }
        return golden;
    }

    /**
     * Every listener callback of one natural-size decode, in arrival order. Failures are swallowed
     * here and only here: what the decode returned is captured by
     * {@link JpegTestSupport#captureVariant}, and this pass exists to record the callbacks that
     * arrived before it ended, however it ended.
     */
    private static List<String> recordEvents(byte[] jpeg) {
        RecordingListener listener = new RecordingListener();
        try {
            JpegTestSupport.decode(new ByteArrayInputStream(jpeg), JpegTestSupport.FULL, listener);
        } catch (Throwable expected) {
            // Recorded as the decode outcome; see captureVariant.
        }
        return listener.events();
    }

    private static List<String> header() {
        return List.of(
                "JPEG decoder goldens, captured from the JNI implementation before any of it was",
                "rewritten. Generated by test.com.sun.javafx.iio.JpegCorpusGenerator:",
                "",
                "  mvn -pl modules/javafx.graphics test -Dtest=JpegCorpusGenerator \\",
                "      -Djfx.iio.jpeg.capture=true",
                "",
                "These values are evidence, not expectations that may be adjusted. If a rewritten",
                "decoder disagrees with one of them, the rewrite changed observable behaviour: that",
                "is the finding, and regenerating the file would delete it. Regeneration needs",
                "-Djfx.iio.jpeg.regenerate=true as well and must be reviewed as a corpus change.",
                "",
                "Keys",
                "  images                          corpus members that exist as files, in order",
                "  image.<name>.length|sha256      the file itself, so a changed corpus is visible",
                "  decode.<name>.<variant>.*       outcome=ok with type/width/height/stride/length/",
                "                                  sha256 of the decoded bytes, or outcome=<throwable",
                "                                  class> with its message",
                "  events.<name>                   listener callbacks in arrival order:",
                "                                  META, W:<warning>, P:<percent>; " + JpegGoldens.NULL,
                "                                  is a null warning, which the JNI build really does",
                "                                  deliver - see JpegWarningOrderTest");
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Surefire runs with the module directory as its working directory. Checked rather than assumed:
     * a capture that writes the corpus somewhere else looks like it worked and commits nothing.
     */
    private static Path moduleDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        if (!Files.isRegularFile(directory.resolve("pom.xml"))
                || !Files.isDirectory(directory.resolve("src/test/resources"))) {
            fail("expected the working directory to be modules/javafx.graphics but it is " + directory
                    + ", so the corpus would be written somewhere unexpected");
        }
        return directory;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        byte[] joined = new byte[first.length + second.length];
        System.arraycopy(first, 0, joined, 0, first.length);
        System.arraycopy(second, 0, joined, first.length, second.length);
        return joined;
    }
}
