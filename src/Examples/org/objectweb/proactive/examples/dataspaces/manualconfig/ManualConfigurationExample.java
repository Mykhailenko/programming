package org.objectweb.proactive.examples.dataspaces.manualconfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;

import org.objectweb.proactive.api.PALifeCycle;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.extensions.dataspaces.api.DataSpacesFileObject;
import org.objectweb.proactive.extensions.dataspaces.api.PADataSpaces;
import org.objectweb.proactive.extensions.dataspaces.core.BaseScratchSpaceConfiguration;
import org.objectweb.proactive.extensions.dataspaces.core.DataSpacesNodes;
import org.objectweb.proactive.extensions.dataspaces.core.InputOutputSpaceConfiguration;
import org.objectweb.proactive.extensions.dataspaces.core.SpaceInstanceInfo;
import org.objectweb.proactive.extensions.dataspaces.core.naming.NamingService;
import org.objectweb.proactive.extensions.dataspaces.core.naming.NamingServiceDeployer;
import org.objectweb.proactive.extensions.dataspaces.exceptions.ApplicationAlreadyRegisteredException;
import org.objectweb.proactive.extensions.dataspaces.exceptions.WrongApplicationIdException;


/**
 * Simple example of manual configuration of DataSpaces for 1 node (without using GCM deployment).
 */
public class ManualConfigurationExample {
    public static void main(String[] args) throws ApplicationAlreadyRegisteredException,
            WrongApplicationIdException, ProActiveException, URISyntaxException, IOException {
        // @snippet-start DataSpacesManualConfig_StartingNS
        // start Naming Service
        final NamingServiceDeployer namingServiceDeployer = new NamingServiceDeployer();
        final String namingServiceURL = namingServiceDeployer.getNamingServiceURL();
        // @snippet-end DataSpacesManualConfig_StartingNS

        // @snippet-start DataSpacesManualConfig_RegisteringApp
        // need to guarantee uniqueness of application id somehow
        final long applicationId = 1234431;
        // @snippet-bDataSpacesManualConfig_RegisteringApp
        // create set of predefined inputs and outputs - here: default input accessed only through HTTP
        final InputOutputSpaceConfiguration inSpaceConf = InputOutputSpaceConfiguration
                .createInputSpaceConfiguration("http://www.faqs.org/ftp/rfc/rfc2616.txt", null, null,
                        PADataSpaces.DEFAULT_IN_OUT_NAME);
        final SpaceInstanceInfo inSpaceInfo = new SpaceInstanceInfo(applicationId, inSpaceConf);
        final Set<SpaceInstanceInfo> predefinedSpaces = Collections.singleton(inSpaceInfo);

        // access (possibly remote) Naming Service
        final NamingService namingService = NamingService.createNamingServiceStub(namingServiceURL);
        // register application
        namingService.registerApplication(applicationId, predefinedSpaces);
        // @snippet-end DataSpacesManualConfig_RegisteringApp

        // node to configure for DataSpaces
        final Node node = NodeFactory.getHalfBodiesNode();

        // @snippet-start DataSpacesManualConfig_ConfigureNode
        // prepare base scratch configuration - here: using tmp dir,
        // with null url - exposed through automatically started ProActive provider server
        final String tmpPath = System.getProperty("java.io.tmpdir") + File.separator + "scratch";
        // configure node for Data Spaces
        final BaseScratchSpaceConfiguration scratchConf = new BaseScratchSpaceConfiguration(null, tmpPath);
        DataSpacesNodes.configureNode(node, scratchConf);
        // @snippet-end DataSpacesManualConfig_ConfigureNode

        // @snippet-start DataSpacesManualConfig_ConfigureNodeForApp
        // configure node for application
        DataSpacesNodes.configureApplication(node, applicationId, namingServiceURL);
        // @snippet-end DataSpacesManualConfig_ConfigureNodeForApp

        // now we can use PADataSpaces from AO/bodies on that node.
        // in case of half-bodies node, we can use PADataSpaces from non-AO
        final DataSpacesFileObject fo = PADataSpaces.resolveDefaultInput();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(fo.getContent().getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } finally {
            fo.close();
            if (reader != null) {
                reader.close();
            }
        }

        // after using DS, we can clean up
        // (actually, this part should be also implemented as finally, as we should always close DS on node and NamingService)

        // @snippet-start DataSpacesManualConfig_CloseNodeConfig
        DataSpacesNodes.closeNodeConfig(node);
        // @snippet-end DataSpacesManualConfig_CloseNodeConfig

        // @snippet-start DataSpacesManualConfig_UnregisteringApp
        namingService.unregisterApplication(applicationId);
        // @snippet-end DataSpacesManualConfig_UnregisteringApp

        // @snippet-start DataSpacesManualConfig_StoppingNS
        namingServiceDeployer.terminate();
        // @snippet-end DataSpacesManualConfig_StoppingNS

        PALifeCycle.exitSuccess();
    }
}