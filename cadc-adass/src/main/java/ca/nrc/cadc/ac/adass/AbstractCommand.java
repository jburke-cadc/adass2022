/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2014.                            (c) 2014.
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
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.ac.adass;

import ca.nrc.cadc.ac.server.GroupPersistence;
import ca.nrc.cadc.ac.server.UserPersistence;
import ca.nrc.cadc.net.TransientException;
import java.io.PrintStream;
import java.security.AccessControlException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provide attributes and methods that apply to all commands.
 * @author yeunga
 *
 */
public abstract class AbstractCommand implements PrivilegedAction<Object> {

    public static final String DATABASE_URL = "database.url";
    public static final String DATABASE_USERNAME = "database.username";
    public static final String DATABASE_PASSWORD = "database.password";

    public static final List<String> DB_PROPS =
        Stream.of(DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD).collect(Collectors.toList());

    protected PrintStream systemOut = System.out;
    protected PrintStream systemErr = System.err;

    private UserPersistence userPersistence;
    private GroupPersistence groupPersistence;

    protected abstract void doRun()
            throws UsageException, AccessControlException, TransientException;

    /**
     * Set the system out.
     * @param printStream       The stream to write System.out to .
     */
    public void setSystemOut(PrintStream printStream) {
        this.systemOut = printStream;
    }

    /**
     * Set the system err.
     * @param printStream       The stream to write System.err to.
     */
    public void setSystemErr(PrintStream printStream) {
        this.systemErr = printStream;
    }

    @Override
    public Object run() {
        try {
            this.doRun();
        } catch (UsageException | AccessControlException e) {
            this.systemErr.println("ERROR: " + e.getMessage());
            e.printStackTrace(systemErr);
        } catch (TransientException e) {
            String message = "Internal Transient Error: " + e.getMessage();
            this.systemErr.println("ERROR: " + message);
            e.printStackTrace(systemErr);
        }
        return null;
    }

    protected void setUserPersistence(final UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    public UserPersistence getUserPersistence() {
        return this.userPersistence;
    }

    protected void setGroupPersistence(final GroupPersistence groupPersistence) {
        this.groupPersistence = groupPersistence;
    }

    public GroupPersistence getGroupPersistence() {
        return this.groupPersistence;
    }

    protected  Connection initDatabase(String propertiesFile)
        throws UsageException {
        PropertyResourceBundle dbProps = AdminUtil.getProperties(propertiesFile, DB_PROPS);
        String dbURL = dbProps.getString(DATABASE_URL);
        String username = dbProps.getString(DATABASE_USERNAME);
        String password = dbProps.getString(DATABASE_PASSWORD);
        try {
            return DriverManager.getConnection(dbURL, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error connecting to %s - %s", dbURL, e.getMessage()));
        }
    }

}
