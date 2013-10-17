/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2013 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package functionalTests.multiprotocol;

import java.net.URISyntaxException;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.AbstractBody;
import org.objectweb.proactive.core.body.UniversalBody;
import org.objectweb.proactive.core.remoteobject.RemoteObjectExposer;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.objectweb.proactive.extensions.dataspaces.core.naming.NamingService;
import org.objectweb.proactive.extensions.dataspaces.core.naming.NamingServiceDeployer;


/**
 * AOMultiProtocolSwitch
 *
 * @author The ProActive Team
 */
public class AOMultiProtocolSwitch {

    public AOMultiProtocolSwitch() {

    }

    public BooleanWrapper foo() {
        return new BooleanWrapper(true);
    }

    public BooleanWrapper foo2() {
        longWait();
        return new BooleanWrapper(true);
    }

    public BooleanWrapper autocont() {
        return ((AOMultiProtocolSwitch) PAActiveObject.getStubOnThis()).foo();
    }

    public BooleanWrapper autocont2() {
        longWait();
        return ((AOMultiProtocolSwitch) PAActiveObject.getStubOnThis()).foo2();
    }

    public boolean bar() {
        return true;
    }

    public boolean waitPlease() {
        longWait();
        return true;
    }

    public BooleanWrapper waitPlease2() {
        longWait();
        return new BooleanWrapper(true);
    }

    public boolean throwException() {
        throw new RuntimeException("Expected Exception");
    }

    private void longWait() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public NamingService createNamingService() throws ProActiveException, URISyntaxException {
        NamingServiceDeployer namingServiceDeployer = new NamingServiceDeployer(true);
        return NamingService.createNamingServiceStub(namingServiceDeployer.getNamingServiceURLs());
    }

    public boolean disableProtocol(String protocol) throws ProActiveException {
        AbstractBody myBody = (AbstractBody) PAActiveObject.getBodyOnThis();

        RemoteObjectExposer<UniversalBody> roe = ((AbstractBody) myBody).getRemoteObjectExposer();

        System.out.println("trying to disable " + protocol);

        roe.disableProtocol(protocol);
        return true;
    }
}
