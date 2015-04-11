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

package com.sun.javafx.tools.packager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.CodeSigner;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.Timestamp;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.PKCS9Attribute;
import sun.security.pkcs.PKCS9Attributes;
import sun.security.pkcs.ParsingException;
import sun.security.pkcs.SignerInfo;
import sun.security.timestamp.TimestampToken;
import sun.security.x509.AlgorithmId;
import sun.security.x509.X500Name;

/**
 * (Source copied from:
 *      com.sun.deploy.security.JarSignature in Deploy workspace)
 *
 * This class is an abstraction of signature that is currently used
 * for implementation of jar signing as BLOBs.
 *
 * There are 2 modes of use for this class - signing and validation
 * and same instance of JarSignature object can not be reused.
 *
 * Signing mode:
 *   - create new instance using JarSignature.create()
 *   - add entries you want to include into the signature using
 *      updateWithEntry(). (Note the order is important.)
 *   - use getEncoded() to get bytes for the result signature
 *
 * Validation mode:
 *   - create new instance using JarSignature.load()
 *   - add entries using updateWithEntry()
 *   - use isValid() to validate result
 *   - use getCodeSigners() to get list of code signers used
 */
public class JarSignature {
    //name of jar manifest attribute that contains signature
    public static final String BLOB_SIGNATURE = "META-INF/SIGNATURE.BSF";

    private final Signature sig;
    private final X509Certificate certChain[]; // for singing scenarios only
    private final CodeSigner codeSigners[];    // for validation only
    private final SignerInfo signerInfos[];        // for validation only

    /**
     * Loads jar signature from given byte array.
     * If signature could not be reconstructed then exceptions are thrown.
     */
    public static JarSignature load(byte[] rawSignature) throws ParsingException,
            CertificateException, IOException, NoSuchAlgorithmException,
            InvalidKeyException, SignatureException
    {
        PKCS7 pkcs7 = new PKCS7(rawSignature);
        SignerInfo[] infos = pkcs7.getSignerInfos();
        if (infos == null || infos.length != 1) {
            throw new IllegalArgumentException(
                    "BLOB signature currently only support single signer.");
        }
        X509Certificate cert = infos[0].getCertificate(pkcs7);
        PublicKey publicKey = cert.getPublicKey();
        CodeSigner[] signers = extractCodeSigners(infos, pkcs7);
        Signature sig = getSignature(infos[0]);
        sig.initVerify(publicKey);

        return new JarSignature(sig, infos, signers);
    }

    /**
     * Creates new signature for signing.
     *
     * @param privateKey Key to be used for signing.
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public static JarSignature create(
                                PrivateKey privateKey, X509Certificate chain[])
            throws NoSuchAlgorithmException, InvalidKeyException
    {
        Signature signature = getSignature(privateKey.getAlgorithm());
        signature.initSign(privateKey);
        return new JarSignature(signature, chain);
    }

    private JarSignature(Signature signature, X509Certificate chain[]) {
        certChain = chain;
        signerInfos = null;
        codeSigners = null;
        sig = signature;
    }

    private JarSignature(Signature signature, SignerInfo[] infos, CodeSigner[] signers) {
        certChain = null;
        signerInfos = infos;
        codeSigners = signers;
        sig = signature;
    }

    public boolean isValidationMode() {
        return certChain == null;
    }

    /**
     * Get default signature object base on default signature algorithm derived
     * from given key algorithm for use in encoding usage.
     */
    private static Signature getSignature(String keyAlgorithm)
            throws NoSuchAlgorithmException
    {
        if (keyAlgorithm.equalsIgnoreCase("DSA")) {
            return Signature.getInstance("SHA1withDSA");
        } else if (keyAlgorithm.equalsIgnoreCase("RSA")) {
            return Signature.getInstance("SHA256withRSA");
        } else if (keyAlgorithm.equalsIgnoreCase("EC")) {
            return Signature.getInstance("SHA256withECDSA");
        }
        throw new IllegalArgumentException(
                            "Key algorithm should be either DSA, RSA or EC");
    }

    /**
     * Derive Signature from signer info for use in validation.
     */
    private static Signature getSignature(SignerInfo info)
            throws NoSuchAlgorithmException
    {
        String digestAlgorithm = info.getDigestAlgorithmId().getName();
        String keyAlgorithm = info.getDigestEncryptionAlgorithmId().getName();
        String signatureAlgorithm = makeSigAlg(
                digestAlgorithm, keyAlgorithm);
        return Signature.getInstance(signatureAlgorithm);
    }

    String getSignatureAlgorithm() throws NoSuchAlgorithmException {
        return sig.getAlgorithm();
    }

    AlgorithmId getDigestAlgorithm() throws NoSuchAlgorithmException {
        String name = getDigAlgFromSigAlg(sig.getAlgorithm());
        return AlgorithmId.get(name);
    }

    AlgorithmId getKeyAlgorithm() throws NoSuchAlgorithmException {
        String name = getEncAlgFromSigAlg(sig.getAlgorithm());
        return AlgorithmId.get(name);
    }

    private static String makeSigAlg(String digAlg, String encAlg) {
        digAlg = digAlg.replace("-", "").toUpperCase(Locale.ENGLISH);
        if (digAlg.equalsIgnoreCase("SHA")) digAlg = "SHA1";

        encAlg = encAlg.toUpperCase(Locale.ENGLISH);
        if (encAlg.equals("EC")) encAlg = "ECDSA";

        return digAlg + "with" + encAlg;
    }

    private static String getDigAlgFromSigAlg(String signatureAlgorithm) {
        signatureAlgorithm = signatureAlgorithm.toUpperCase(Locale.ENGLISH);
        int with = signatureAlgorithm.indexOf("WITH");
        if (with > 0) {
            return signatureAlgorithm.substring(0, with);
        }
        return null;
    }

    private static String getEncAlgFromSigAlg(String signatureAlgorithm) {
        signatureAlgorithm = signatureAlgorithm.toUpperCase(Locale.ENGLISH);
        int with = signatureAlgorithm.indexOf("WITH");
        String keyAlgorithm = null;
        if (with > 0) {
            int and = signatureAlgorithm.indexOf("AND", with + 4);
            if (and > 0) {
                keyAlgorithm = signatureAlgorithm.substring(with + 4, and);
            } else {
                keyAlgorithm = signatureAlgorithm.substring(with + 4);
            }
            if (keyAlgorithm.equalsIgnoreCase("ECDSA")) {
                keyAlgorithm = "EC";
            }
        }
        return keyAlgorithm;
    }

    /**
     * Returns encoded representation of signature.
     * @throws UnsupportedOperationException if called in validation mode.
     */
    public byte[] getEncoded()
            throws NoSuchAlgorithmException, SignatureException, IOException {
        if (isValidationMode()) {
            throw new UnsupportedOperationException(
                    "Method is not for validation mode.");
        }

        AlgorithmId digestAlgId = getDigestAlgorithm();
        AlgorithmId[] digestAlgIds = {digestAlgId};
        ContentInfo contentInfo = new ContentInfo(ContentInfo.DATA_OID, null);
        Principal issuerName = certChain[0].getIssuerDN();
        BigInteger serialNumber = certChain[0].getSerialNumber();
        byte[] signature = sig.sign();
        SignerInfo signerInfo =
                new SignerInfo((X500Name) issuerName, serialNumber, digestAlgId,
                                getKeyAlgorithm(), signature);

        SignerInfo[] signerInfos = {signerInfo};
        PKCS7 pkcs7 = new PKCS7(digestAlgIds, contentInfo, certChain,
                signerInfos);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        pkcs7.encodeSignedData(bos);
        return bos.toByteArray();
    }

    private class ValidationStream extends InputStream {
        InputStream dataStream = null;

        public ValidationStream(
                InputStream is) {
            dataStream = is;
        }

        public int read() throws IOException {
            int v = dataStream.read();
            if (v > -1) {
                try {
                    JarSignature.this.sig.update((byte) v);
                } catch (SignatureException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return v;
        }

        public int read(byte[] b, int off, int len) throws IOException {
            len = dataStream.read(b, off, len);
            if (len > 0) {
                try {
                    JarSignature.this.sig.update(b, off, len);
                } catch (SignatureException ex) {
                    throw new RuntimeException(ex);
                }
            }
            return len;
        }

        public void close() throws IOException {
            dataStream.close();
        }

    }

    /**
     * Performs partial validation of zip/jar entry with given name and
     * data stream. Return wrapped version of input stream to complete validation.
     * Stream need to be read fully for validation to be complete.
     *
     * @throws SignatureException
     */
    public InputStream updateWithZipEntry(String name, InputStream is)
            throws SignatureException {
        try {
            sig.update(name.getBytes("UTF-8"));
        } catch(UnsupportedEncodingException e) {
            throw new SignatureException(e);
        }
        return new ValidationStream(is);
    }

    public void update(byte[] v) throws SignatureException {
        sig.update(v);
    }

    public boolean isValid() {
        try {
            return sig.verify(signerInfos[0].getEncryptedDigest());
        } catch(Exception e) {
            return false;
        }
    }

    public CodeSigner[] getCodeSigners() {
        CodeSigner[] result = new CodeSigner[codeSigners.length];
        System.arraycopy(codeSigners, 0, result, 0, result.length);
        return result;
    }

    /*
     * Implementation is mostly borrowed from
     *   sun.security.util.SignatureFileVerifier
     */
    private static CodeSigner[] extractCodeSigners(SignerInfo infos[], PKCS7 block)
            throws IOException, NoSuchAlgorithmException, SignatureException,
            CertificateException {
        ArrayList s = new ArrayList();

        CertificateFactory certificateFactory =
                                    CertificateFactory.getInstance("X509");
        for (int i = 0; i < infos.length; i++) {
            SignerInfo info = infos[i];
            ArrayList chain = info.getCertificateChain(block);
            CertPath certPath = certificateFactory.generateCertPath(chain);
            // Append the new code signer
            CodeSigner signer =
                new CodeSigner(certPath, getTimestamp(info, certificateFactory));
//                if (block.getCRLs() != null) {
//                    SharedSecrets.getJavaSecurityCodeSignerAccess().setCRLs(
//                            signer, block.getCRLs());
//                }
            s.add(signer);
        }

        return (CodeSigner[]) s.toArray(new CodeSigner[s.size()]);
    }

    /*
     * Examines a signature timestamp token to generate a timestamp object.
     *
     * Examines the signer's unsigned attributes for a
     * <tt>signatureTimestampToken</tt> attribute. If present,
     * then it is parsed to extract the date and time at which the
     * timestamp was generated.
     *
     * @param info A signer information element of a PKCS 7 block.
     *
     * @return A timestamp token or null if none is present.
     * @throws IOException if an error is encountered while parsing the
     *         PKCS7 data.
     * @throws NoSuchAlgorithmException if an error is encountered while
     *         verifying the PKCS7 object.
     * @throws SignatureException if an error is encountered while
     *         verifying the PKCS7 object.
     * @throws CertificateException if an error is encountered while generating
     *         the TSA's certpath.
     */
    private static Timestamp getTimestamp(SignerInfo info,
            CertificateFactory certificateFactory) throws IOException,
            NoSuchAlgorithmException, SignatureException, CertificateException
    {
        Timestamp timestamp = null;

        // Extract the signer's unsigned attributes
        PKCS9Attributes unsignedAttrs = info.getUnauthenticatedAttributes();
        if (unsignedAttrs != null) {
            PKCS9Attribute timestampTokenAttr =
                    unsignedAttrs.getAttribute("signatureTimestampToken");
            if (timestampTokenAttr != null) {
                PKCS7 timestampToken =
                        new PKCS7((byte[]) timestampTokenAttr.getValue());
                // Extract the content (an encoded timestamp token info)
                byte[] encodedTimestampTokenInfo =
                        timestampToken.getContentInfo().getData();
                // Extract the signer (the Timestamping Authority)
                // while verifying the content
                SignerInfo[] tsa =
                        timestampToken.verify(encodedTimestampTokenInfo);
                // Expect only one signer
                ArrayList chain = tsa[0].getCertificateChain(timestampToken);
                CertPath tsaChain = certificateFactory.generateCertPath(
                        chain);
                // Create a timestamp token info object
                TimestampToken timestampTokenInfo =
                        new TimestampToken(encodedTimestampTokenInfo);
                // Create a timestamp object
                timestamp =
                        new Timestamp(timestampTokenInfo.getDate(), tsaChain);
            }
        }
        return timestamp;
    }

    public interface InputStreamSource {
        InputStream getInputStream() throws IOException;
    }

    public void signJarAsBLOB(InputStreamSource input, ZipOutputStream jos)
            throws IOException, SignatureException, NoSuchAlgorithmException
    {
        byte copyBuf[] = new byte[8000];
        int n;
        ZipEntry e;
        ZipInputStream jis = new ZipInputStream(input.getInputStream());

        try {
            //calculate signature in the first pass
            //consider all entries except directories and old signature file (if any)
            boolean hasManifest = false;
            boolean hasMetaInf = false;
            while ((e = jis.getNextEntry()) != null) {
                if (JarFile.MANIFEST_NAME.equals(
                        e.getName().toUpperCase(Locale.ENGLISH))) {
                    hasManifest = true;
                }
                if ("META-INF/".equals(
                        e.getName().toUpperCase(Locale.ENGLISH))) {
                    hasMetaInf = true;
                }
                if (!BLOB_SIGNATURE.equals(e.getName()) &&
                    !e.getName().endsWith("/")) {
                    readFully(updateWithZipEntry(e.getName(), jis));
                }
            }

            byte[] signature = getEncoded();

            //2 pass: save manifest and other entries

            //reopen input file
            jis.close();
            jis = new ZipInputStream(input.getInputStream());
            while ((e = jis.getNextEntry()) != null) {
                String name = e.getName();

                //special case - jar has no manifest and possibly no META-INF
                // => add META-INF entry once
                //Then output manifest
                if (!hasMetaInf) {
                    ZipEntry ze = new ZipEntry("META-INF/");
                    ze.setTime(System.currentTimeMillis());
                    //NOTE: Do we need CRC32? for this entry?
                    jos.putNextEntry(ze);
                    jos.closeEntry();

                    hasMetaInf = true;
                }
                if (!hasManifest) {
                    addSignatureEntry(signature, jos);
                    hasManifest = true;
                }


                //copy entry unless it is old signature file
                if (!BLOB_SIGNATURE.equals(name)) {
                    // do our own compression
                    ZipEntry e2 = new ZipEntry(name);
                    e2.setMethod(e.getMethod());
                    e2.setTime(e.getTime());
                    e2.setComment(e.getComment());
                    e2.setExtra(e.getExtra());
                    if (e.getMethod() == ZipEntry.STORED) {
                        e2.setSize(e.getSize());
                        e2.setCrc(e.getCrc());
                    }
                    jos.putNextEntry(e2);

                    while ((n = jis.read(copyBuf)) != -1) {
                        jos.write(copyBuf, 0, n);
                    }
                    jos.closeEntry();
                }

                String upperName = name.toUpperCase(Locale.ENGLISH);
                boolean isManifestEntry = JarFile.MANIFEST_NAME.equals(upperName);

                //output signature after manifest
                if (isManifestEntry) {
                    addSignatureEntry(signature, jos);
                }
            }
        } finally {
            jis.close();
            jos.close();
        }
    }

    private void addSignatureEntry(byte[] signature, ZipOutputStream jos) throws IOException {
        ZipEntry ze = new ZipEntry(BLOB_SIGNATURE);
        ze.setSize(signature.length);
        ze.setTime(System.currentTimeMillis());
        //NOTE: Do we need a CRC32?
        jos.putNextEntry(ze);
        jos.write(signature);
        jos.closeEntry();

    }

    private static void readFully(InputStream is) throws IOException {
        byte buf[] = new byte[10000];
        while (is.read(buf) != -1) {}
    }

}
