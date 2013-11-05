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

package com.oracle.ipack.signer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

public final class Signer {
    private final CMSSignedDataGenerator signatureGenerator;
    private final String subjectName;

    private Signer(final CMSSignedDataGenerator signatureGenerator,
                   final String subjectName) {
        this.signatureGenerator = signatureGenerator;
        this.subjectName = subjectName;
    }

    public static Signer create(
            final File keystoreFile,
            final String keystorePassword,
            final String signingAlias,
            final String keyPassword) throws IOException,
                                             KeyStoreException,
                                             NoSuchAlgorithmException,
                                             CertificateException,
                                             UnrecoverableKeyException,
                                             OperatorCreationException,
                                             CMSException,
                                             InvalidNameException {
        final KeyStore jksKeyStore = KeyStore.getInstance("JKS");
        final InputStream is = new FileInputStream(keystoreFile);
        try {
            jksKeyStore.load(is, keystorePassword.toCharArray());
        } finally {
            is.close();
        }

        final PrivateKey privateKey =
                (PrivateKey) jksKeyStore.getKey(signingAlias,
                                                keyPassword.toCharArray());
        final Certificate[] certChain =
                jksKeyStore.getCertificateChain(signingAlias);
        if (certChain == null) {
            throw new CertificateException(
                    "Certificate chain not found under \"" + signingAlias
                                                           + "\"");
        }

        final X509Certificate signingCert = (X509Certificate) certChain[0];
        final String subjectName = getSubjectName(signingCert);

        final Store certs = new JcaCertStore(Arrays.asList(certChain));
        final CMSSignedDataGenerator signatureGenerator =
                new CMSSignedDataGenerator();

        signatureGenerator.addSignerInfoGenerator(
                new JcaSimpleSignerInfoGeneratorBuilder()
                            .setProvider("BC")
                            .build("SHA1withRSA", privateKey, signingCert));
        signatureGenerator.addCertificates(certs);

        return new Signer(signatureGenerator, subjectName);
    }

    public byte[] sign(final byte[] data) throws CMSException, IOException {
        final CMSTypedData typedData = new CMSProcessableByteArray(data);
        final CMSSignedData signedData = signatureGenerator.generate(typedData);

        return signedData.getEncoded();
    }

    public String getSubjectName() {
        return subjectName;
    }

    private static String getSubjectName(final X509Certificate cert)
            throws InvalidNameException {
        final String fullSubjectDn = cert.getSubjectX500Principal().getName();
        final LdapName fullSubjectLn = new LdapName(fullSubjectDn);
        for (final Rdn rdn: fullSubjectLn.getRdns()) {
            if ("CN".equalsIgnoreCase(rdn.getType())) {
                return rdn.getValue().toString();
            }
        }

        throw new InvalidNameException("Common name not found");
    }
}
