/*
 * ################################################################
 *
 * ProActive: The Java(TM) library for Parallel, Distributed,
 *            Concurrent computing with Security and Mobility
 *
 * Copyright (C) 1997-2009 INRIA/University of 
 * 						   Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org
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
 * If needed, contact us to obtain a release under GPL Version 2. 
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */
package functionalTests.activeobject.migration.loopmigration;

import static junit.framework.Assert.assertFalse;

import org.objectweb.proactive.api.PAActiveObject;

import functionalTests.GCMFunctionalTestDefaultNodes;


/**
 * Test AO loop migration
 */
public class TestLoopMigration extends GCMFunctionalTestDefaultNodes {
    public TestLoopMigration() {
        super(2, 1);
    }

    @org.junit.Test
    public void action() throws Exception {

        String node1 = super.getANode().getNodeInformation().getURL();
        String node2 = super.getANode().getNodeInformation().getURL();
        A a = PAActiveObject.newActive(A.class, new Object[] { node1, node2 }, node1);

        assertFalse(a.isException());
    }
}
