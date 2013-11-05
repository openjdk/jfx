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

package com.oracle.ipack.packer;

import com.oracle.ipack.blobs.VirtualBlob;
import com.oracle.ipack.blobs.WrapperBlob;
import com.oracle.ipack.macho.CodeSignatureCommand;
import com.oracle.ipack.macho.MachoCommand;
import com.oracle.ipack.macho.MachoHeader;
import com.oracle.ipack.macho.SegmentCommand;
import com.oracle.ipack.signature.CodeDirectoryBlob;
import com.oracle.ipack.signature.EmbeddedSignatureBlob;
import com.oracle.ipack.signature.Requirement;
import com.oracle.ipack.signature.RequirementBlob;
import com.oracle.ipack.signature.RequirementsBlob;
import com.oracle.ipack.signer.Signer;
import com.oracle.ipack.util.DataCopier;
import com.oracle.ipack.util.HashingOutputStream;
import com.oracle.ipack.util.LsbDataInputStream;
import com.oracle.ipack.util.LsbDataOutputStream;
import com.oracle.ipack.util.NullOutputStream;
import com.oracle.ipack.util.PageHashingOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bouncycastle.cms.CMSException;

final class ExecutablePacker {
    private static final int RESERVED_SIGNATURE_BLOB_SIZE = 9000;

    private final ZipOutputStream zipStream;
    private final File baseDir;
    private final String appPath;
    private final String appName;
    private final String appIdentifier;
    private final Signer signer;

    private byte[] codeResourcesHash;
    private byte[] infoPlistHash;

    ExecutablePacker(final ZipOutputStream zipStream,
                     final File baseDir,
                     final String appPath,
                     final String appName,
                     final String appIdentifier,
                     final Signer signer) {
        this.zipStream = zipStream;
        this.baseDir = baseDir;
        this.appPath = appPath;
        this.appName = appName;
        this.appIdentifier = appIdentifier;
        this.signer = signer;
    }

    void setCodeResourcesHash(final byte[] codeResourcesHash) {
        this.codeResourcesHash = codeResourcesHash;
    }

    void setInfoPlistHash(final byte[] infoPlistHash) {
        this.infoPlistHash = infoPlistHash;
    }

    void execute() throws IOException {
        final InputStream execInputStream =
                new BufferedInputStream(new FileInputStream(
                        new File(baseDir, appPath + appName)));
        try {
            final MachoHeader header =
                    MachoHeader.read(new LsbDataInputStream(execInputStream));
            final int oldHeaderSize = header.getSize();

            final SegmentCommand linkeditSegment =
                    header.findSegment("__LINKEDIT");
            if (linkeditSegment == null) {
                throw new IOException("Linkedit segment not found");
            }

            CodeSignatureCommand codeSignatureCommand =
                    (CodeSignatureCommand) header.findCommand(
                                               MachoCommand.LC_CODE_SIGNATURE);
            if (codeSignatureCommand == null) {
                // no previous signature in the executable
                codeSignatureCommand = new CodeSignatureCommand();
                codeSignatureCommand.setDataOffset(
                        linkeditSegment.getFileOffset()
                            + linkeditSegment.getFileSize());
                header.addCommand(codeSignatureCommand);
            }

            final int codeLimit = codeSignatureCommand.getDataOffset();
            final EmbeddedSignatureBlob embeddedSignatureBlob =
                    createEmbeddedSignatureBlob(
                            appIdentifier,
                            signer.getSubjectName(),
                            codeLimit);

            // update the header with information about the new embedded
            // code signature
            final int reservedForEmbeddedSignature =
                    (embeddedSignatureBlob.getSize() + 15) & ~15;
            codeSignatureCommand.setDataSize(reservedForEmbeddedSignature);
            final int newLinkeditSize =
                    codeLimit - linkeditSegment.getFileOffset()
                              + reservedForEmbeddedSignature;
            linkeditSegment.setFileSize(newLinkeditSize);
            linkeditSegment.setVmSize((newLinkeditSize + 0xfff) & ~0xfff);
            final int newHeaderSize = header.getSize();

            final int firstSectionOffset = getFirstSectionFileOffset(header);
            if (newHeaderSize > firstSectionOffset) {
                throw new IOException("Patched header too long");
            }

            // we assume that there is only padding between the header and the
            // first section, so we can skip some of it in the input stream

            execInputStream.skip(newHeaderSize - oldHeaderSize);

            // start the executable zip entry
            final String entryName = appPath + appName;
            System.out.println("Adding " + entryName);
            zipStream.putNextEntry(new ZipEntry(entryName));
            try {
                final PageHashingOutputStream hashingStream =
                        new PageHashingOutputStream(zipStream);

                // store the patched header
                writeHeader(hashingStream, header);

                // copy the rest of the executable up to the codeLimit
                final DataCopier dataCopier = new DataCopier();
                // no need to use buffered stream, because the data is copied in
                // large chunks
                dataCopier.copyStream(hashingStream, execInputStream,
                                      codeLimit - newHeaderSize);

                // finalize the last page hash
                hashingStream.flush();
                hashingStream.commitPageHash();

                // update the code directory blob with hashes
                final byte[] requirementsBlobHash =
                        calculateRequirementsBlobHash(
                            embeddedSignatureBlob.getRequirementsSubBlob());
                updateHashes(embeddedSignatureBlob.getCodeDirectorySubBlob(),
                             hashingStream.getPageHashes(),
                             infoPlistHash,
                             requirementsBlobHash,
                             codeResourcesHash);

                // sign the embedded signature blob
                signEmbeddedSignatureBlob(embeddedSignatureBlob, signer);

                // write the embedded signature blob and padding
                writeEmbeddedSignatureBlob(zipStream, embeddedSignatureBlob,
                                           reservedForEmbeddedSignature);

            } finally {
                zipStream.closeEntry();
            }
        } finally {
            execInputStream.close();
        }

    }

    private static int getFirstSectionFileOffset(final MachoHeader header)
            throws IOException {
        // assumes that the first section in the file is a text section of the
        // text segment

        final SegmentCommand textSegment = header.findSegment("__TEXT");
        if (textSegment == null) {
            System.out.println(header);
            throw new IOException("Text segment not found");
        }

        final SegmentCommand.Section textSection =
                textSegment.findSection("__text");
        if (textSection == null) {
            throw new IOException("Text section not found");
        }

        return textSection.getOffset();
    }


    private static EmbeddedSignatureBlob createEmbeddedSignatureBlob(
            final String appIdentifier,
            final String subjectName,
            final int codeLimit) {
        final CodeDirectoryBlob codeDirectoryBlob =
                new CodeDirectoryBlob(appIdentifier, codeLimit);

        final RequirementsBlob requirementsBlob = new RequirementsBlob(1);
        final RequirementBlob designatedRequirementBlob =
                new RequirementBlob(
                    Requirement.createDefault(appIdentifier, subjectName));
        requirementsBlob.setSubBlob(
                0, RequirementsBlob.KSEC_DESIGNATED_REQUIREMENT_TYPE,
                designatedRequirementBlob);

        final VirtualBlob reservedForSignatureBlob =
                new VirtualBlob(0, RESERVED_SIGNATURE_BLOB_SIZE - 8);

        final EmbeddedSignatureBlob embeddedSignatureBlob =
                new EmbeddedSignatureBlob();
        embeddedSignatureBlob.setCodeDirectorySubBlob(codeDirectoryBlob);
        embeddedSignatureBlob.setRequirementsSubBlob(requirementsBlob);
        embeddedSignatureBlob.setSignatureSubBlob(reservedForSignatureBlob);

        return embeddedSignatureBlob;
    }

    private static void signEmbeddedSignatureBlob(
            final EmbeddedSignatureBlob embeddedSignatureBlob,
            final Signer signer) throws IOException {
        final CodeDirectoryBlob codeDirectoryBlob =
                embeddedSignatureBlob.getCodeDirectorySubBlob();
        final ByteArrayOutputStream bos =
                new ByteArrayOutputStream(codeDirectoryBlob.getSize());
        final DataOutputStream os = new DataOutputStream(bos);
        try {
            codeDirectoryBlob.write(os);
        } finally {
            os.close();
        }

        final byte[] signature;
        try {
            signature = signer.sign(bos.toByteArray());
        } catch (final CMSException e) {
            throw new IOException("Failed to sign executable", e);
        }

        embeddedSignatureBlob.setSignatureSubBlob(
                new WrapperBlob(signature));
    }

    private static void writeHeader(
            final OutputStream dataStream,
            final MachoHeader header) throws IOException {
        final LsbDataOutputStream headerStream =
                new LsbDataOutputStream(new BufferedOutputStream(dataStream));

        try {
            header.write(headerStream);
        } finally {
            headerStream.flush();
        }
    }

    private static void writeEmbeddedSignatureBlob(
            final OutputStream dataStream,
            final EmbeddedSignatureBlob embeddedSignatureBlob,
            final int reservedForEmbeddedSignature) throws IOException {
        final int realEmbeddedSignatureSize =
                embeddedSignatureBlob.getSize();
        if (realEmbeddedSignatureSize > reservedForEmbeddedSignature) {
            throw new IOException("Embedded signature too large");
        }

        final DataOutputStream signatureStream =
                new DataOutputStream(new BufferedOutputStream(dataStream));
        try {
            embeddedSignatureBlob.write(signatureStream);

            // add padding
            for (int i = reservedForEmbeddedSignature
                             - realEmbeddedSignatureSize; i > 0; --i) {
                signatureStream.writeByte(0);
            }
        } finally {
            signatureStream.flush();
        }
    }

    private static void updateHashes(
            final CodeDirectoryBlob codeDirectoryBlob,
            final List<byte[]> pageHashes,
            final byte[] infoPlistHash,
            final byte[] requirementsHash,
            final byte[] codeResourcesHash) {
        int i = 0;
        for (final byte[] pageHash: pageHashes) {
            codeDirectoryBlob.setCodeSlot(i++, pageHash);
        }

        if (infoPlistHash != null) {
            codeDirectoryBlob.setInfoPlistSlot(infoPlistHash);
        }

        if (requirementsHash != null) {
            codeDirectoryBlob.setRequirementsSlot(requirementsHash);
        }

        if (codeResourcesHash != null) {
            codeDirectoryBlob.setCodeResourcesSlot(codeResourcesHash);
        }
    }

    private static byte[] calculateRequirementsBlobHash(
            final RequirementsBlob requirementsBlob) {
        final HashingOutputStream hashingStream =
                new HashingOutputStream(new NullOutputStream());

        try {
            final DataOutputStream dataStream =
                    new DataOutputStream(hashingStream);
            try {
                requirementsBlob.write(dataStream);
            } finally {
                dataStream.close();
            }

            return hashingStream.calculateHash();
        } catch (final IOException e) {
            // won't happen
            return null;
        }
    }
}
