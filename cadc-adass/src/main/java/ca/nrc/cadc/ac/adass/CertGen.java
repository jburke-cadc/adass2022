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

import ca.nrc.cadc.exec.BuilderOutputGrabber;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

public class CertGen {
    private static final Logger log = Logger.getLogger(CertGen.class);

    private static final String CMD = "%s --userid=%s --cert=%s --signingCert=%s "
                                    + "--resourceID=%s --server=%s --database=%s";

    private final String exec;
    private final String cert;
    private final String signingCert;
    private final String resourceID;
    private final String server;
    private final String database;

    public CertGen(final String exec, final String cert, final String signingCert,
                   final String resourceID, final String server, final String database) {
        this.exec = exec;
        this.cert = cert;
        this.signingCert = signingCert;
        this.resourceID = resourceID;
        this.server = server;
        this.database = database;
    }

    public String genCert(String userID) {
        Process p;
        try {
            String command = String.format(CMD, exec, userID, cert, signingCert, resourceID, server, database);
            log.debug("cadc-cert-gen cmd: " + command);
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                throw new IllegalStateException("No DN returned for user: " + userID);
            }
            log.debug("cadc-cert-gen output: " + line);
            int index = line.toLowerCase().indexOf("cn=");
            if (index == -1) {
                throw new IllegalStateException("DN starting with CN= not found: " + line);
            }
            String dn = line.substring(index).toLowerCase();
            log.debug("dn: " + dn);
            return dn;
        } catch (IOException e) {
            throw new IllegalStateException("IO error getting DN for " + userID + " because " + e.getMessage());
        } catch (InterruptedException e) {
            throw new IllegalStateException("Thread error getting DN for " + userID + " because " + e.getMessage());
        }
    }

    public String genCertX(String userID) {
        String command = String.format(CMD, exec, userID, cert, signingCert, resourceID, server, database);
        log.debug("cadc-cert-gen cmd: " + command);
        BuilderOutputGrabber outputGrabber = new BuilderOutputGrabber();
        outputGrabber.captureOutput(command);
        if (outputGrabber.getExitValue() != 0) {
            log.debug("cadc-cert-gen output: " + outputGrabber.getOutput());
            String error = outputGrabber.getErrorOutput();
            throw new IllegalStateException("Error getting DN for " + userID + " because " + error);
        }
        return outputGrabber.getOutput();
    }

}
