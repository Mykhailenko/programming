/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2010 INRIA/University of
 *              Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2
 * or a different license than the GPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$ACTIVEEON_INITIAL_DEV$$
 */
package org.objectweb.proactive.extensions.ssl;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.jinterop.dcom.test.SysInfoEvents;
import org.objectweb.proactive.core.ProActiveRuntimeException;


/**
 *
 * This class is implemented as a child of SSLEngine instead of a factory because ProActive
 * is not designed to support class injection. It is easier to replace this PASslEngine by
 * another SSLEngine by using inheritance than using an hard coded factory class.
 *
 * @since ProActive 4.4.0
 */
public class PASslEngine extends SSLEngine {
    final private SSLEngine sslEngine;

    public PASslEngine(boolean client, SecureMode secureMode, KeyStore keystore, TrustManager trustManager) {
        try {
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, SslHelpers.DEFAULT_KS_PASSWD.toCharArray());

            // Initialize the SSLContext to work with our key managers.
            SSLContext ctxt = SSLContext.getInstance(SslHelpers.DEFAULT_PROTOCOL);
            ctxt.init(kmf.getKeyManagers(), new TrustManager[] { trustManager }, null);

            this.sslEngine = ctxt.createSSLEngine();
            this.sslEngine.setEnabledProtocols(new String[] { SslHelpers.DEFAULT_PROTOCOL });
            this.sslEngine.setEnableSessionCreation(true);
            this.sslEngine.setEnabledCipherSuites(new String[] { "SSL_DH_anon_WITH_RC4_128_MD5" });
            if (client) {
                this.sslEngine.setUseClientMode(true);
            } else {
                this.sslEngine.setUseClientMode(false);
            }
            switch (secureMode) {
                case CIPHERED_ONLY:
                    this.sslEngine.setNeedClientAuth(false);
                    break;
                case AUTH_AND_CIPHERED:
                    this.sslEngine.setNeedClientAuth(true);
                    break;
                default:
                    throw new ProActiveRuntimeException("Unsupported secure mode: " + secureMode);
            }
        } catch (GeneralSecurityException e) {
            throw new ProActiveRuntimeException("failed to initialize " + this.getClass().getName(), e);
        }
    }

    @Override
    public SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst)
            throws SSLException {
        return this.sslEngine.wrap(srcs, offset, length, dst);
    }

    @Override
    public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length)
            throws SSLException {
        return this.sslEngine.unwrap(src, dsts, offset, length);
    }

    @Override
    public Runnable getDelegatedTask() {
        return this.sslEngine.getDelegatedTask();
    }

    @Override
    public void closeInbound() throws SSLException {
        this.sslEngine.closeInbound();
    }

    @Override
    public boolean isInboundDone() {
        return this.sslEngine.isInboundDone();
    }

    @Override
    public void closeOutbound() {
        this.sslEngine.closeOutbound();
    }

    @Override
    public boolean isOutboundDone() {
        return this.sslEngine.isOutboundDone();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return this.sslEngine.getSupportedCipherSuites();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return this.sslEngine.getEnabledCipherSuites();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        this.sslEngine.setEnabledCipherSuites(suites);
    }

    @Override
    public String[] getSupportedProtocols() {
        return this.sslEngine.getSupportedProtocols();
    }

    @Override
    public String[] getEnabledProtocols() {
        return this.sslEngine.getEnabledProtocols();
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        this.sslEngine.setEnabledProtocols(protocols);
    }

    @Override
    public SSLSession getSession() {
        return this.sslEngine.getSession();
    }

    @Override
    public void beginHandshake() throws SSLException {
        this.sslEngine.beginHandshake();
    }

    @Override
    public HandshakeStatus getHandshakeStatus() {
        return this.sslEngine.getHandshakeStatus();
    }

    @Override
    public void setUseClientMode(boolean mode) {
        this.sslEngine.setUseClientMode(mode);
    }

    @Override
    public boolean getUseClientMode() {
        return this.sslEngine.getUseClientMode();
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        this.sslEngine.setNeedClientAuth(need);
    }

    @Override
    public boolean getNeedClientAuth() {
        return this.sslEngine.getNeedClientAuth();
    }

    @Override
    public void setWantClientAuth(boolean want) {
        this.sslEngine.setWantClientAuth(want);
    }

    @Override
    public boolean getWantClientAuth() {
        return this.sslEngine.getWantClientAuth();
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        this.sslEngine.setEnableSessionCreation(flag);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return this.sslEngine.getEnableSessionCreation();
    }
}