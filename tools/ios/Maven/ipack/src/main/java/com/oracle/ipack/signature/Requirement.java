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

package com.oracle.ipack.signature;

import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class Requirement {
    private static final Requirement APPLE_GENERIC_ANCHOR =
            new Anchor(0xf);
    private static final Match MATCH_EXISTS =
            new Match(0);

    public static Requirement createDefault(
            final String appIdent,
            final String subjectName) {
        final byte[] oid = {
            (byte) 0x2A, (byte) 0x86, (byte) 0x48, (byte) 0x86, (byte) 0xF7,
            (byte) 0x63, (byte) 0x64, (byte) 0x06, (byte) 0x02, (byte) 0x01
        };

        return and(ident(appIdent),
                   and(appleGenericAnchor(),
                       and(certField(0, "subject.CN",
                                     matchEqual(subjectName)),
                           certGeneric(1, oid, matchExists()))));
    }

    public static Requirement and(final Requirement left,
                                  final Requirement right) {
        return new BinaryOp(6, left, right);
    }

    public static Requirement ident(final String value) {
        return new Ident(value);
    }

    public static Requirement appleGenericAnchor() {
        return APPLE_GENERIC_ANCHOR;
    }

    public static Requirement certField(final int certIndex,
                                        final String fielName,
                                        final Match match) {
        return new CertField(certIndex, fielName, match);
    }

    public static Requirement certGeneric(final int certIndex,
                                          final byte[] oid,
                                          final Match match) {
        return new CertGeneric(certIndex, (byte[]) oid.clone(), match);
    }

    public static Match matchExists() {
        return MATCH_EXISTS;
    }

    public static Match matchEqual(final String value) {
        return new MatchEqual(value);
    }

    private Requirement() {
    }

    public final int getSize() {
        return 4 + getPayloadSize();
    }

    public final void write(final DataOutput dataOutput) throws IOException {
        dataOutput.writeInt(getIdent());
        writePayload(dataOutput);
    }

    protected abstract int getIdent();

    protected abstract int getPayloadSize();

    protected abstract void writePayload(DataOutput dataOutput)
            throws IOException;

    private static final class BinaryOp extends Requirement {
        private final int ident;
        private final Requirement left;
        private final Requirement right;

        public BinaryOp(final int ident,
                        final Requirement left,
                        final Requirement right) {
            this.ident = ident;
            this.left = left;
            this.right = right;
        }

        @Override
        protected int getIdent() {
            return ident;
        }

        @Override
        protected int getPayloadSize() {
            return left.getSize() + right.getSize();
        }

        @Override
        protected void writePayload(final DataOutput dataOutput)
                throws IOException {
            left.write(dataOutput);
            right.write(dataOutput);
        }
    }

    private static final class Ident extends Requirement {
        private final StringData value;

        public Ident(final String value) {
            this.value = new StringData(value);
        }

        @Override
        protected int getIdent() {
            return 2;
        }

        @Override
        protected int getPayloadSize() {
            return value.getSize();
        }

        @Override
        protected void writePayload(final DataOutput dataOutput)
                throws IOException {
            value.write(dataOutput);
        }
    }

    private static final class Anchor extends Requirement {
        private final int ident;

        public Anchor(final int ident) {
            this.ident = ident;
        }

        @Override
        protected int getIdent() {
            return ident;
        }

        @Override
        protected int getPayloadSize() {
            return 0;
        }

        @Override
        protected void writePayload(final DataOutput dataOutput)
                throws IOException {
        }
    }

    private static final class CertField extends Requirement {
        private final int certIndex;
        private final StringData fieldName;
        private final Match match;

        public CertField(final int certIndex,
                         final String fieldName,
                         final Match match) {
            this.certIndex = certIndex;
            this.fieldName = new StringData(fieldName);
            this.match = match;
        }

        @Override
        protected int getIdent() {
            return 0xb;
        }

        @Override
        protected int getPayloadSize() {
            return 4 + fieldName.getSize() + match.getSize();
        }

        @Override
        protected void writePayload(final DataOutput dataOutput)
                throws IOException {
            dataOutput.writeInt(certIndex);
            fieldName.write(dataOutput);
            match.write(dataOutput);
        }
    }

    private static final class CertGeneric extends Requirement {
        private final int certIndex;
        private final BinaryData oid;
        private final Match match;

        public CertGeneric(final int certIndex,
                           final byte[] oid,
                           final Match match) {
            this.certIndex = certIndex;
            this.oid = new BinaryData(oid);
            this.match = match;
        }

        @Override
        protected int getIdent() {
            return 0xe;
        }

        @Override
        protected int getPayloadSize() {
            return 4 + oid.getSize() + match.getSize();
        }

        @Override
        protected void writePayload(final DataOutput dataOutput)
                throws IOException {
            dataOutput.writeInt(certIndex);
            oid.write(dataOutput);
            match.write(dataOutput);
        }
    }

    private static class BinaryData {
        private byte[] bytes;

        public BinaryData(final byte[] bytes) {
            this.bytes = bytes;
        }

        public int getSize() {
            final int rawSize = 4 + bytes.length;
            return (rawSize + 3) & ~3;
        }

        public void write(final DataOutput dataOutput) throws IOException {
            dataOutput.writeInt(bytes.length);
            dataOutput.write(bytes);
            final int padding = getSize() - 4 - bytes.length;
            for (int i = 0; i < padding; ++i) {
                dataOutput.write(0);
            }
        }
    }

    private static final class StringData extends BinaryData {
        public StringData(final String value) {
            super(getBytes(value));
        }

        private static byte[] getBytes(final String value) {
            try {
                return value.getBytes("UTF-8");
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static class Match {
        private final int ident;

        private Match(final int ident) {
            this.ident = ident;
        }

        public final int getSize() {
            return 4 + getArgSize();
        }

        public final void write(final DataOutput dataOutput)
                throws IOException {
            dataOutput.writeInt(ident);
            writeArg(dataOutput);
        }

        protected int getArgSize() {
            return 0;
        }

        protected void writeArg(final DataOutput dataOutput)
                throws IOException {
        }
    }

    private static final class MatchEqual extends Match {
        private final StringData arg;

        public MatchEqual(final String arg) {
            super(1);
            this.arg = new StringData(arg);
        }

        @Override
        protected int getArgSize() {
            return arg.getSize();
        }

        @Override
        protected void writeArg(final DataOutput dataOutput)
                throws IOException {
            arg.write(dataOutput);
        }
    }
}
