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

import ca.nrc.cadc.ac.GroupNotFoundException;
import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.auth.HttpPrincipal;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;

public class DeleteUsers extends AbstractCommand {

    private static final Logger log = Logger.getLogger(DeleteUsers.class);

    private final String propertiesFile;
    private final String logFile;
    private final boolean dryRun;
    private BufferedWriter logWriter;
    private Connection connection;
    private int total;
    private int usersDeleted;
    private int usersSkipped;
    private int usersError;
    private int groupsDeleted;
    private int groupsSkipped;
    private int groupsError;

    public DeleteUsers(String propertiesFile, String logFile, boolean dryRun)
        throws UsageException{
        this.propertiesFile = propertiesFile;
        this.logFile = logFile;
        this.dryRun = dryRun;
        init();
    }

    /**
     * Must be run as cadcops.
     */
    @Override
    protected void doRun() {
        if (this.dryRun) {
            logMessage("dry run: logging only, no users will be deleted");
        }

        List<Proposal> proposals;
        try {
            proposals = AdminUtil.getProposals(this.connection);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error getting Proposal list from DB: %s", e.getMessage()));
        }
        this.total = proposals.size();
        logMessage(String.format("users to delete - %s", this.total));

        for (Proposal proposal : proposals) {
            if (this.dryRun) {
                logMessage(String.format("%s - user & group to delete", proposal.code));
                groupsDeleted++;
                usersDeleted++;
            } else {
                // delete the user's primary group
                try {
                    this.getGroupPersistence().deleteGroup(proposal.code);
                    logMessage(String.format("%s - deleted group", proposal.code));
                    groupsDeleted++;
                } catch (GroupNotFoundException e) {
                    logMessage(String.format("%s - group not found", proposal.code));
                    groupsSkipped++;
                } catch (Exception e) {
                    logMessage(String.format("%s - error deleting group: %s", proposal.code, e.getMessage()));
                    groupsError++;
                }

                // TODO delete the user's adass group for vault permissions
                //String groupID = String.format("%s%s", vaultUserFolderPrefix, user.posixDetails.getUsername());
                //try {
                //    this.getGroupPersistence().deleteGroup(proposal.code);
                //    logMessage(String.format("%s - deleted group", proposal.code));
                //    groupsDeleted++;
                //} catch (GroupNotFoundException e) {
                //    logMessage(String.format("%s - group not found", proposal.code));
                //    groupsSkipped++;
                //} catch (Exception e) {
                //    logMessage(String.format("%s - error deleting group: %s", proposal.code, e.getMessage()));
                //    groupsError++;
                //}

                // delete the user
                try {
                    this.getUserPersistence().deleteUser(new HttpPrincipal(proposal.code));
                    logMessage(String.format("%s - deleted user", proposal.code));
                    usersDeleted++;
                } catch (UserNotFoundException e) {
                    logMessage(String.format("%s - user not found", proposal.code));
                    usersSkipped++;
                } catch (Exception e) {
                    logMessage(String.format("%s - error deleting user: %s", proposal.code, e.getMessage()));
                    usersError++;
                }
            }
        }

        // log results
        logMessage(String.format("  total - %s", total));
        logMessage("users");
        logMessage(String.format("deleted - %s", usersDeleted));
        logMessage(String.format("skipped - %s", usersSkipped));
        logMessage(String.format("  error - %s", usersError));
        logMessage("groups");
        logMessage(String.format("deleted - %s", groupsDeleted));
        logMessage(String.format("skipped - %s", groupsSkipped));
        logMessage(String.format("  error - %s", groupsError));
        try {
            this.logWriter.close();
        } catch (IOException ignore) {
        }
    }

    protected void init()
        throws UsageException{
        this.total = 0;
        this.usersDeleted = 0;
        this.usersSkipped = 0;
        this.usersError = 0;
        this.groupsDeleted = 0;
        this.groupsSkipped = 0;
        this.groupsError = 0;
        this.logWriter = AdminUtil.initWriter(logFile);
        this.connection = initDatabase(this.propertiesFile);
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
