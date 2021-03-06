/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.objectweb.proactive.core.runtime;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.body.UniversalBody;
import org.objectweb.proactive.core.descriptor.data.ProActiveDescriptorInternal;
import org.objectweb.proactive.core.descriptor.data.VirtualNodeInternal;
import org.objectweb.proactive.core.descriptor.services.TechnicalService;
import org.objectweb.proactive.core.filetransfer.FileTransferEngine;
import org.objectweb.proactive.core.jmx.mbean.ProActiveRuntimeWrapperMBean;
import org.objectweb.proactive.core.jmx.notification.GCMRuntimeRegistrationNotificationData;
import org.objectweb.proactive.core.jmx.server.ServerConnector;
import org.objectweb.proactive.core.mop.ConstructorCall;
import org.objectweb.proactive.core.mop.ConstructorCallExecutionFailedException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.process.UniversalProcess;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;


/**
 * <p>
 * A <code>ProActiveRuntime</code> offers a set of services needed by ProActive to work with
 * remote JVM. Each JVM that is aimed to hold active objects must contain
 * one and only one instance of the <code>ProActiveRuntime</code> class. That instance, when
 * created, will register itself to some registry where it is possible to perform a lookup
 * (such as the RMI registry).
 * </p><p>
 * When ProActive needs to interact with a remote JVM, it will lookup for the <code>ProActiveRuntime</code> associated
 * with that JVM (using typically the RMI Registry) and use the remote interface of the <code>ProActiveRuntime</code>
 * to perform the interaction.
 * </p><p>
 * Aside the getter giving information about the VM, the 2 services offered are :
 * <ul>
 * <li>the creation of local node through the method <code>createLocalNode</code></li>
 * <li>the creation of another VM(local or remote) through the method <code>createVM</code></li>
 * </ul>
 * </p>
 *
 * @author The ProActive Team
 * @version 1.0,  2002/08/29
 * @since   ProActive 0.91
 *
 */
public interface ProActiveRuntime {
    static Logger runtimeLogger = ProActiveLogger.getLogger(Loggers.RUNTIME);

    /**
     * Creates a new Node in the same VM as this ProActiveRuntime
     * @param nodeName the name of the node to create localy
     * @param replacePreviousBinding
     * @return the url of the newly created node in the target VM
     * @exception NodeException if the new node cannot be created
     */
    public Node createLocalNode(String nodeName, boolean replacePreviousBinding, String vnName)
            throws NodeException, AlreadyBoundException;

    public Node createGCMNode(String vnName, List<TechnicalService> tsList) throws NodeException, AlreadyBoundException;

    /**
     * Kills all Nodes in this ProActiveRuntime
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void killAllNodes() throws ProActiveException;

    /**
     * Kills the Node of the given name and all Active Objects deployed on it.
     * @param nodeName the name of the node to kill
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void killNode(String nodeName) throws ProActiveException;

    /**
     * Creates a new ProActiveRuntime associated with a new VM on the host defined in the given process.
     * @param remoteProcess the process that will originate the creation of the runtime
     * @exception java.io.IOException if the new VM cannot be created
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void createVM(UniversalProcess remoteProcess) throws java.io.IOException, ProActiveException;

    /**
     * Returns the name of all nodes known by this ProActiveRuntime on this VM
     * @return the name of all nodes known by this ProActiveRuntime on this VM
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public String[] getLocalNodeNames() throws ProActiveException;

    /**
     * Returns the JVM information as one object. This method allows to
     * retrieve all JVM information in one call to optimize performance.
     * note this value is cached using the remote object adapter pattern
     * @return the JVM information as one object
     */
    public VMInformation getVMInformation();

    /**
     * <i><font size="-1" color="#FF0000">**For internal use only** </font></i>
     * Allows this ProActiveRuntime on this VM to register another ProActiveRuntime
     * @param proActiveRuntimeDist the remote ProactiveRuntime to register
     * @param proActiveRuntimeUrl the url of the remote ProActiveRuntime
     * @param creatorID the name of the creator of the remote ProActiveRuntime
     * @param creationProtocol the protocol used to register the remote ProActiveRuntime when created
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void register(ProActiveRuntime proActiveRuntimeDist, String proActiveRuntimeUrl, String creatorID,
            String creationProtocol, String vmName) throws ProActiveException;

    /**
     * <i><font size="-1" color="#FF0000">**For internal use only** </font></i>
     * Allows this ProactiveRuntime on this VM to <b>unregister</b> an already resigesterd
     * ProActiveRuntime.
     * @param proActiveRuntimeDist the remote ProactiveRuntime to <b>unregister</b>.
     * @param proActiveRuntimeUrl the url of the remote ProActiveRuntime
     * @param creatorID the name of the creator of the remote ProActiveRuntime
     * @param creationProtocol the protocol used to register the remote ProActiveRuntime when created
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void unregister(ProActiveRuntime proActiveRuntimeDist, String proActiveRuntimeUrl, String creatorID,
            String creationProtocol, String vmName) throws ProActiveException;

    /**
     * Returns all the ProActiveRuntime registered on this ProActiveRuntime on this VM
     * @return all the ProActiveRuntime registered on this ProActiveRuntime on this VM
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public ProActiveRuntime[] getProActiveRuntimes() throws ProActiveException;

    /**
     * Returns the ProActiveRuntime of specified name
     * @param proActiveRuntimeName the name of the ProActiveruntime to return
     * @return the ProActiveRuntime of specified name
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public ProActiveRuntime getProActiveRuntime(String proActiveRuntimeName) throws ProActiveException;

    /**
     * Kills this ProActiveRuntime and this VM
     * @param softly if false, this Runtime is killed abruptely
     * if true, if that runtime originates the creation of  a rmi registry, it waits until the registry is empty before
     * dying. To be more precise a thread is created to ask periodically the registry if objects are still
     * registered.
     * @exception Exception if a problem occurs when killing this ProActiveRuntime
     */
    public void killRT(boolean softly);

    /**
     * Returns the url of this ProActiveRuntime on the local or remote VM
     * This information is cached using the remote object adapter pattern
     */
    public String getURL();

    /**
     * Returns all Active Objects deployed on the node with the given name on
     * this ProActiveRuntime
     * @param nodeName the name of the node
     * @return List of UniversalBody. The latter contains [body, classname].
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public List<UniversalBody> getActiveObjects(String nodeName) throws ProActiveException;

    /**
     * Returns all Active Objects with the specified class name, deployed on the node with the given name on
     * this ProActiveRuntime
     * @param nodeName the name of the node
     * @param className class of the Active Objects to look for
     * @return Active Objects of the specified class name deployed on this ProactiveRuntime
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public List<UniversalBody> getActiveObjects(String nodeName, String className) throws ProActiveException;

    /**
     * Returns the VirtualNode with the given name
     * @param virtualNodeName the name of the VirtualNode to be acquired
     * @return VirtualNode the virtualnode of the given name or null if such virtualNode
     * does not exist, or has not been yet activated.
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public VirtualNodeInternal getVirtualNode(String virtualNodeName) throws ProActiveException;

    /**
     * Registers the virtualNode of the given name in a registry such RMIRegistry
     * @param virtualNodeName
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void registerVirtualNode(String virtualNodeName, boolean replacePreviousBinding)
            throws ProActiveException, AlreadyBoundException;

    /**
     * Unregisters the VirtualNode of the given name from the local runtime.
     * @param virtualNodeName the virtualNode to unregister.
     * @throws ProActiveException if a problem occurs when trying to unregister the virtualNode
     */
    public void unregisterVirtualNode(String virtualNodeName) throws ProActiveException;

    /**
     * Unregisters all VirtualNodes from the local runtime
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProActiveRuntime
     */
    public void unregisterAllVirtualNodes() throws ProActiveException;

    /**
     * <p>
     * This method is the basis for creating remote active objects.
     * It receives a <code>ConstructorCall</code> that is the constructor call of the body
     * of the active object to create. Inside the parameters of this constructor call is
     * the constructor call of the reified object. Upon execution of the constructor call of the
     * body, the body holding a reference on the reified object will get created and returned.
     * </p><p>
     * The method returns a reference on the RMI stub of the newly created body.
     * </p>
     * @param nodeName the name of the node the newly created active object will be associated to
     * @param bodyConstructorCall the Constructor call allowing to create the body
     * @param isNodeLocal boolean. True if proxy and body are on the same vm, false otherwise
     * @return a stub on the newly created body.
     * @exception ProActiveException if a problem occurs due to the remote nature of this ProactiveRuntime
     * @exception ConstructorCallExecutionFailedException if the constructor call cannot be executed
     * @exception java.lang.reflect.InvocationTargetException if the java constructor execution failed
     */
    public UniversalBody createBody(String nodeName, ConstructorCall bodyConstructorCall, boolean isNodeLocal)
            throws ProActiveException, ConstructorCallExecutionFailedException,
            java.lang.reflect.InvocationTargetException;

    // SECURITY
    public String getVNName(String Nodename) throws ProActiveException;

    /**
     * Looks for class bytecode in the current runtime. Stub generation can be intented
     * to get the class bytecode.
     *
     * @param className name of the class
     * @return the bytecode corresponding to the given class, or null if not found
     */
    public byte[] getClassData(String className);

    /**
     * launch the main method of the main class with parameters
     * @param className
     * @param parameters
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws ProActiveException
     */
    public void launchMain(String className, String[] parameters)
            throws ClassNotFoundException, NoSuchMethodException, ProActiveException;

    /**
     * construct a new instance remotely
     * @param className class to instance
     * @throws ClassNotFoundException
     * @throws ProActiveException
     */
    public void newRemote(String className) throws ClassNotFoundException, ProActiveException;

    /**
     * return the pad matching with the given url or parse it from the file system
     * @param url url of the pad
     * @param isHierarchicalSearch if yes search for the pad hierarchically
     * @return the pad found or a new pad parsed from xmlDescriptorUrl
     * @throws ProActiveException
     * @throws IOException
     */
    public ProActiveDescriptorInternal getDescriptor(String url, boolean isHierarchicalSearch)
            throws IOException, ProActiveException;

    /**
     * Set a property on the specified local node.
     * @param nodeName local node name.
     * @param key the hashtable key.
     * @param value the value corresponding to key.
     * @return the previous value of the specified key in this property list,
     * or <code>null</code> if it did not have one.
     * @throws ProActiveException
     */
    public Object setLocalNodeProperty(String nodeName, String key, String value) throws ProActiveException;

    /**
     * Get a property on the specified local node.
     * @param nodeName local node name.
     * @param key the hashtable key.
     * @return  the value in the local node property list with the specified key
     * value.
     * @throws ProActiveException
     */
    public String getLocalNodeProperty(String nodeName, String key) throws ProActiveException;

    //
    // --- JMX
    //

    /**
     * Starts the JMX ServerConnector.
     */
    public void startJMXServerConnector();

    /**
     * Return the MBean associated to this ProActiveRuntime.
     * @return the MBean associated to this ProActiveRuntime.
     */
    public ProActiveRuntimeWrapperMBean getMBean();

    /**
     * Returns the MBeans Server Name
     * @return the MBeans Server Name
     */
    public String getMBeanServerName();

    /**
     * Returns the JMX Server connector
     * @return the JMX Server connector
     */
    public ServerConnector getJMXServerConnector();

    public void register(GCMRuntimeRegistrationNotificationData event);

    /**
     * @return the FileTransferEngine singleton active object.
     */
    public FileTransferEngine getFileTransferEngine();

    /**
     * Returns the full thread dump of the Runtime's enclosing JVM
     *
     * @return thread dump
     */
    public String getThreadDump();
}
