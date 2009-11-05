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
package functionalTests.activeobject.request;

import org.junit.Before;
import org.objectweb.proactive.api.PAActiveObject;

import functionalTests.FunctionalTest;

import static junit.framework.Assert.assertTrue;


/**
 * Test blocking request, and calling void, int returned type and object returned type method
 */

public class Test extends FunctionalTest {
    A activeA;
    A javaA;
    int counterActiveA;
    int counterA;

    @Before
    public void action() throws Exception {
        activeA = PAActiveObject.newActive(A.class, new Object[0]);
        activeA.method1();
        javaA = activeA.method2();
        counterA = javaA.method3();
        counterActiveA = activeA.method3();
    }

    @org.junit.Test
    public void postConditions() {
        assertTrue(counterA == 1);
        assertTrue(counterActiveA == 3);
    }
}
