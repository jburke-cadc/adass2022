/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.ac.adass;

import ca.nrc.cadc.ac.Group;
import ca.nrc.cadc.ac.GroupNotFoundException;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.vos.Node;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.Subject;

public class LockUsers extends AbstractCommand {

    private final String propertiesFile;
    private final String logFile;
    private final boolean dryRun;
    private BufferedWriter logWriter;
    private Connection connection;
    private String adassAdmin;
    private Subject adassAdminSubject;
    private Group adassAdminGroup;
    private String vaultRootNode;
    private String vaultUserFolderPrefix;
    private int total;
    private int locked;
    private int skipped;
    private int error;
    public PropertyResourceBundle importProps;
    public VOSpaceClient voSpaceClient;

    public static final List<String> IMPORT_PROPS =
        Stream.of(ImportUsers.VAULT_RESOURCE_ID, ImportUsers.VAULT_ADMIN_USER_CERT,
                  ImportUsers.VAULT_ADMIN_USER, ImportUsers.VAULT_ROOT_NODE).collect(Collectors.toList());

    public LockUsers(String propertiesFile, String logFile, boolean dryRun)
        throws UsageException {
        this.propertiesFile = propertiesFile;
        this.logFile = logFile;
        this.dryRun = dryRun;

        init();
    }

    /**
     * Must be run as ADASS admin.
     */
    protected void doRun() {
        if (this.dryRun) {
            logMessage("dry run: logging only, no folders will be locked");
        }

        // should be in init(), but needs ldap persistence
        this.adassAdminGroup = getAdassAdminGroup();

        // Get list of proposals
        List<Proposal> proposals;
        try {
            proposals = AdminUtil.getProposals(this.connection);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error getting Proposal list from DB: %s", e.getMessage()));
        }
        this.total = proposals.size();
        logMessage(String.format("nodes to lock - %s", total));

        for (Proposal proposal : proposals) {
            lockNodeToAdmin(proposal);
        }

        // log results
        logMessage(String.format("  total - %s", total));
        logMessage(String.format(" locked - %s", locked));
        logMessage(String.format("skipped - %s", skipped));
        logMessage(String.format("  error - %s", error));
        try {
            this.logWriter.close();
        } catch (IOException ignore) {
        }
    }

    protected void init()
        throws UsageException {
        this.total = 0;
        this.locked = 0;
        this.skipped = 0;
        this.error = 0;
        this.logWriter = AdminUtil.initWriter(logFile);
        this.importProps = AdminUtil.getProperties(propertiesFile, IMPORT_PROPS);
        this.adassAdmin = importProps.getString(ImportUsers.VAULT_ADMIN_USER);
        this.vaultRootNode = importProps.getString(ImportUsers.VAULT_ROOT_NODE);
        this.vaultUserFolderPrefix = importProps.getString(ImportUsers.VAULT_USER_FOLDER_PREFIX);

        String vaultResourceID = importProps.getString(ImportUsers.VAULT_RESOURCE_ID);
        URI vaultURI;
        try {
            vaultURI = new URI(vaultResourceID);
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("%s invalid uri: %s",
                                                     ImportUsers.VAULT_RESOURCE_ID, vaultResourceID));
        }
        this.voSpaceClient = new VOSpaceClient(vaultURI);

        String adassCertPath = importProps.getString(ImportUsers.VAULT_ADMIN_USER_CERT);
        this.adassAdminSubject = SSLUtil.createSubject(new File(adassCertPath));

        this.connection = initDatabase(this.propertiesFile);
    }

    protected Group getAdassAdminGroup() {
        // subject code for dev
        //File adminCert = new File("/Volumes/work/cadc/test-certificates/adass2022.pem");
        //Subject subject = SSLUtil.createSubject(adminCert);
        //subject = AuthenticationUtil.augmentSubject(subject);

        // subject code for prod
        Subject subject = AuthenticationUtil.augmentSubject(adassAdminSubject);
        return Subject.doAs(subject, (PrivilegedAction<Group>) () -> {
            try {
                return getGroupPersistence().getGroup(adassAdmin);
            } catch (GroupNotFoundException e) {
                throw new IllegalStateException(String.format("ADASS admin Group '%s' not found", adassAdmin));
            }
        });
    }

    protected void lockNodeToAdmin(Proposal proposal) {
        // VOSpaceClient calls made as adass2022 user.
        if (this.dryRun) {
            logMessage(String.format("%s - node to lock", proposal.code));
            locked++;
        } else {
            Subject.doAs(adassAdminSubject, (PrivilegedAction<Object>) () -> {
                try {
                    String nodeName = String.format("%s/%s%s", vaultRootNode, vaultUserFolderPrefix, proposal.code);
                    Node node = this.voSpaceClient.getNode(nodeName);
                    List<NodeProperty> nodeProperties = new ArrayList<>();
                    setPermissions(nodeProperties, adassAdminGroup);
                    node.setProperties(nodeProperties);
                    this.voSpaceClient.setNode(node);

                    logMessage(String.format("%s - node locked", proposal.code));
                    locked++;
                } catch (NodeNotFoundException e) {
                    logMessage(String.format("%s - node locked", proposal.code));
                    skipped++;
                } catch (IllegalStateException e) {
                    logMessage(String.format("%s - error locking node", proposal.code));
                    error++;
                }
                return null;
            });
        }
    }

    private void setPermissions(List<NodeProperty> nodeProperties, Group adminGroup) {
        // only adass-admin has node permissions
        NodeProperty isPublic = new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, "false");
        nodeProperties.add(isPublic);

        // RO access to adass-admin
        NodeProperty rGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, adminGroup.getID().toString());
        nodeProperties.add(rGroup);

        // RW access to adass-admin
        NodeProperty writeGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, adminGroup.getID().toString());
        nodeProperties.add(writeGroup);
    }

    private void logMessage(String message) {
        this.systemOut.println(message);
        try {
            this.logWriter.write(message);
            this.logWriter.newLine();
            this.logWriter.flush();
        } catch (IOException ignore) {
        }
    }

}
