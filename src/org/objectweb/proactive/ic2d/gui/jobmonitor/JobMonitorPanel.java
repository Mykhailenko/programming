package org.objectweb.proactive.ic2d.gui.jobmonitor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.registry.Registry;
import java.util.Vector;

import javax.swing.*;
import javax.swing.tree.*;

import org.objectweb.proactive.ProActive;
import org.objectweb.proactive.ic2d.gui.IC2DGUIController;
import org.objectweb.proactive.ic2d.gui.jobmonitor.data.DataAssociation;
import org.objectweb.proactive.ic2d.gui.jobmonitor.data.DataModelTraversal;
import org.objectweb.proactive.ic2d.gui.jobmonitor.data.DataTreeModel;
import org.objectweb.proactive.ic2d.gui.jobmonitor.data.DataTreeNode;
import org.objectweb.proactive.ic2d.gui.jobmonitor.switcher.Switcher;


public class JobMonitorPanel extends JPanel implements JobMonitorConstants {
    private static final String VN_VIEW_LABEL = "Job view / Virtual Nodes";
    private static final String JOB_VIEW_LABEL = "Job view / Hosts";
    private static final String HOST_VIEW_LABEL = "Host view";
    private static final String CUSTOM_VIEW_LABEL = "Custom view";
    private static final int DEFAULT_RMI_PORT = Registry.REGISTRY_PORT;
    private static final String EXTRACT_MENU_LABEL = "Extract view to a new window";
    private static final int[] JOB_VIEW_KEYS = { JOB, HOST, JVM, VN, NODE, AO };
    private static final int[] HOST_VIEW_KEYS = { HOST, JOB, JVM, VN, NODE, AO };
    private static final int[] VN_VIEW_KEYS = { JOB, VN, HOST, JVM, NODE, AO };
    private static final int[] CUSTOM_VIEW_KEYS = { JOB, VN, HOST, JVM, NODE, AO };
    private JTabbedPane tabs;
    private Vector frames;
    private DataAssociation asso;
    private NodeExploration explorator;
    private DataTreeModel jobViewModel;
    private DataTreeModel vnViewModel;
    private DataTreeModel hostViewModel;
    private DataTreeModel customViewModel;
    private Vector monitoredHosts;
    private Vector filteredJobs;
    private JPopupMenu popupmenu;
    private Thread refresher;
    private volatile boolean refresh = true;
    private int ttr = 60;

    public JobMonitorPanel(IC2DGUIController _controller) {
    	asso = new DataAssociation();
    	
        setLayout(new GridLayout(1, 1));

        createRefresher();

        monitoredHosts = new Vector();
        filteredJobs = new Vector();
        filteredJobs.add(ProActive.getJobId());

        tabs = new JTabbedPane();
        frames = new Vector();

    	explorator = new NodeExploration(asso, filteredJobs, _controller);
        
        add(tabs);

        final JPopupMenu extractMenu = new JPopupMenu();
        JMenuItem extract = new JMenuItem(EXTRACT_MENU_LABEL);
        extract.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final Component tab = tabs.getSelectedComponent();
                    final String title = tabs.getTitleAt(tabs.getSelectedIndex());

                    final JFrame f = new JFrame(title);
                    f.setSize(tab.getSize());
                    Container c = f.getContentPane();
                    c.setLayout(new GridLayout(1, 1));
                    c.add(tab);

                    f.addWindowListener(new WindowAdapter() {
                            public void windowClosing(WindowEvent e) {
                                tabs.addTab(title, tab);
                                frames.remove(f);
                            }
                        });

                    frames.add(f);

                    f.show();
                }
            });

        extractMenu.add(extract);

        tabs.addTab(JOB_VIEW_LABEL, createJobView());
        tabs.addTab(VN_VIEW_LABEL, createVNView());
        tabs.addTab(HOST_VIEW_LABEL, createHostView());
        tabs.addTab(CUSTOM_VIEW_LABEL, createCustomView());
        
        tabs.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        Component menu = extractMenu.getComponent(0);
                        if (tabs.getTabCount() > 1) {
                            if (!menu.isEnabled()) {
                                menu.setEnabled(true);
                            }
                        } else {
                            if (menu.isEnabled()) {
                                menu.setEnabled(false);
                            }
                        }

                        extractMenu.show(tabs, e.getX(), e.getY());
                    }
                }
            });
    }

    private void createRefresher() {
        refresh = true;
        refresher = new Thread(new Runnable() {
                    public void run() {
                        //				System.out.println ("Start of refresher thread");
                        while (refresh) {
                            try {
                                //						System.out.println ("Waiting for refresh - ttr = " + ttr + " seconds");
                                Thread.sleep(ttr * 1000);
                                //						System.out.println ("Automatic refresh starting");
                                handleHosts();
                            } catch (InterruptedException e) {
                                //						e.printStackTrace();
                            }
                        }

                        //				System.out.println ("Stop of refresher thread");
                    }
                });
    }

    protected void finalize() throws Throwable {
        stopRefreshing();
        super.finalize();
    }

    private void stopRefreshing() {
        if (refresh) {
            //			System.out.println ("Stoppping refresher thread");
            refresh = false;
            refresher.interrupt();
        }
    }

    public int getTtr() {
        return ttr;
    }

    public void setTtr(int _ttr) {
        ttr = _ttr;
        refresher.interrupt();
    }

    public void addMonitoredHost(String host) {
        if (!monitoredHosts.contains(host)) {
            monitoredHosts.add(host);
            if (monitoredHosts.size() == 1) {
                refresher.start();
            }
        }

        //		System.out.println ("There are now " + monitoredHosts.size() + " monitored hosts");
    }

    public void updateHosts() {
        new Thread(new Runnable() {
                public void run() {
                    handleHosts();
                }
            }).start();
    }

    void hideOwnedFrames() {
        //		System.out.println("Hiding frames: " + frames.size());
        hideOrShow(true);
        stopRefreshing();
    }

    void showOwnedFrames() {
        //		System.out.println("Showing frames: " + frames.size());
        if (!monitoredHosts.isEmpty()) {
            createRefresher();
            refresher.start();
        }
        hideOrShow(false);
    }

    private void handleHosts() {
        synchronized (monitoredHosts) {
            asso.clear();

            for (int i = 0, size = monitoredHosts.size(); i < size; ++i) {
                String host = (String) monitoredHosts.get(i);

                //				System.out.println ("\nMonitoring host " + (i + 1) + " / " + size + ": " + host);
                handleHost(host);
            }
        }

        vnViewModel.rebuild();
        jobViewModel.rebuild();
        hostViewModel.rebuild();
        customViewModel.rebuild();
    }

    public void updateHost(final DataTreeNode hostNode) {
        new Thread(new Runnable() {
                public void run() {
                    asso.removeItem(HOST, hostNode.getName());
                    handleHost(hostNode.getName());

                    vnViewModel.rebuild(hostNode);
                    jobViewModel.rebuild(hostNode);
                    hostViewModel.rebuild(hostNode);
                    customViewModel.rebuild(hostNode);
                }
            }).start();
    }

    private void handleHost(String host) {
        String hostname = host;
        int port = DEFAULT_RMI_PORT;
        int pos = host.lastIndexOf(":");
        if (pos != -1) {
            // if the hostname is host:port
            try {
                port = Integer.parseInt(host.substring(1 + pos));
            } catch (NumberFormatException e) {
                port = DEFAULT_RMI_PORT;
            }

            hostname = host.substring(0, pos);
        }

        explorator.exploreHost(hostname, port);
    }

    private void dump(Object o) {
        System.out.println("<object class='" + o.getClass() + "'>");
        System.out.println(o.toString());
        System.out.println("</object>");
        System.out.println();
    }

    private void hideOrShow(boolean hide) {
        for (int i = 0, size = frames.size(); i < size; ++i) {
            JFrame f = (JFrame) frames.get(i);
            if (hide) {
                f.hide();
            } else {
                f.show();
            }
        }
    }

    private boolean constructPopupMenu(final DataTreeNode node) {
        boolean showMenu = false;

        if (popupmenu == null) {
            popupmenu = new JPopupMenu();
        }

        popupmenu.removeAll();

        if (node == null) {
            AbstractAction a = new AbstractAction("Refresh monitoring tree") {
                    public void actionPerformed(ActionEvent e) {
                        //	    			System.out.println("Asking for a global refresh");
                        updateHosts();
                    }
                };

            JMenuItem treeMenu = new JMenuItem(a);
            treeMenu.setEnabled(monitoredHosts.size() > 0);
            popupmenu.add(treeMenu);

            showMenu = true;
        } else {
            int key = node.getKey();

            AbstractAction a = null;
            if (key == HOST) {
                a = new AbstractAction("Refresh host") {
                            public void actionPerformed(ActionEvent e) {
                                //						System.out.println ("Asking for a host refresh: " + node.getName());
                                updateHost(node);
                            }
                        };
            } else if (key == JOB) {
                a = new AbstractAction("Stop monitoring this job") {
                            public void actionPerformed(ActionEvent e) {
                                String job = node.getName();

                                //						System.out.println ("Asking for a job to be added to the filtered jobs list: " + job);
                                filteredJobs.add(job);

                                // remove job from tree
                            }
                        };
            }

            if (a != null) {
                JMenuItem nodeMenu = new JMenuItem(a);
                popupmenu.add(nodeMenu);

                showMenu = true;
            }
        }

        return showMenu;
    }

    private static void addButtons(JPanel panel, final JTree tree) {
        JPanel buttons = new JPanel();
        buttons.setLayout(new FlowLayout());
        panel.add(buttons, BorderLayout.SOUTH);
        JButton expand = new JButton("Expand all");
        expand.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    for (int row = 0; row < tree.getRowCount(); row++)
                        tree.expandRow(row);
                }
            });
        buttons.add(expand);

        JButton collapse = new JButton("Collapse all");
        collapse.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    for (int row = tree.getRowCount() - 1; row >= 0; row--)
                        tree.collapseRow(row);
                }
            });
        buttons.add(collapse);
    }

    private Container createContent(DataTreeModel model, boolean allowExpand) {
        //JSplitPane sp = new JSplitPane ();
        //sp.setOneTouchExpandable (true);
        JPanel left = new JPanel(new BorderLayout());

        //JPanel right = new JPanel ();
        //sp.setLeftComponent (left);
        //sp.setRightComponent (right);
        final JTree j = new JTree(model);
        j.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        j.setCellRenderer(new JobMonitorTreeCellRenderer());

        JScrollPane pane = new JScrollPane(j);
        left.add(pane, BorderLayout.CENTER);

        j.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        TreePath selPath = j.getPathForLocation(e.getX(),
                                e.getY());
                        DataTreeNode node = null;

                        if (selPath != null) {
                            node = (DataTreeNode) selPath.getLastPathComponent();

                            if (node == null) {
                                return;
                            }

                            if ((node == vnViewModel.getRoot()) ||
                                    (node == jobViewModel.getRoot()) ||
                                    (node == hostViewModel.getRoot())) {
                                node = null;
                            }

                            j.setSelectionPath(selPath);
                        }

                        if (constructPopupMenu(node)) {
                            popupmenu.show(j, e.getX(), e.getY());
                        }
                    }
                }
            });

        addButtons(left, j);

        Switcher s = new Switcher(j, allowExpand);
        JPanel switcher = new JPanel(new GridLayout(1, 1));
        switcher.add(s);
        switcher.setBorder(BorderFactory.createEtchedBorder());
        left.add(switcher, BorderLayout.NORTH);

        return left;
    }

    private JPanel createPanel(DataTreeModel model, boolean allowExpand) {
        JPanel p = new JPanel(new GridLayout(1, 1));
        p.add(createContent(model, allowExpand));
        return p;
    }

    private JPanel createJobView() {
        DataModelTraversal traversal = new DataModelTraversal(JOB_VIEW_KEYS);
        jobViewModel = new DataTreeModel(asso, traversal);

        return createPanel(jobViewModel, false);
    }

    private JPanel createHostView() {
        DataModelTraversal traversal = new DataModelTraversal(HOST_VIEW_KEYS);
        hostViewModel = new DataTreeModel(asso, traversal);

        return createPanel(hostViewModel, false);
    }

    private JPanel createVNView() {
        DataModelTraversal traversal = new DataModelTraversal(VN_VIEW_KEYS);
        vnViewModel = new DataTreeModel(asso, traversal);

        return createPanel(vnViewModel, false);
    }
    
    private JPanel createCustomView() {
        DataModelTraversal traversal = new DataModelTraversal(CUSTOM_VIEW_KEYS);
        customViewModel = new DataTreeModel(asso, traversal);

        return createPanel(customViewModel, true);
    }
    
    public NodeExploration getNodeExploration() {
    	return explorator;
    }
}
