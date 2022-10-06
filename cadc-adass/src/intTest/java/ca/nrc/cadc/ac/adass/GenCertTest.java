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

import java.security.SecureRandom;
import java.util.PropertyResourceBundle;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class GenCertTest {
    private static final Logger log = Logger.getLogger(GenCertTest.class);

    private static final String[] charCategories = new String[] {
        "abcdefghijkmnopqrstuvwxyz",
        "ABCDEFGHJKLMNOPQRSTUVWXYZ",
        "0123456789"
    };

    @Test
    public void certGenTest() {
        try {
            String propertiesFilename = "cadc-adass.properties";
            PropertyResourceBundle props = AdminUtil.getProperties(propertiesFilename, ImportUsers.IMPORT_PROPS);

            CertGen testSubject = new CertGen(props.getString(ImportUsers.CADC_CERT_GEN_EXEC),
                                              props.getString(ImportUsers.CADC_CERT_GEN_CALLING_CERT),
                                              props.getString(ImportUsers.CADC_CERT_GEN_SIGNING_CERT),
                                              props.getString(ImportUsers.CADC_CERT_GEN_RESOURCE_ID),
                                              props.getString(ImportUsers.CADC_CERT_GEN_SERVER),
                                              props.getString(ImportUsers.CADC_CERT_GEN_DATABASE));

            String userID = "adass" + System.currentTimeMillis();
            String dn = testSubject.genCert(userID);
            Assert.assertNotNull(dn);
            Assert.assertTrue(dn.length() > 0);
            Assert.assertTrue(dn.contains(userID));
        } catch (Exception e) {
            Assert.fail("exception not expected; " + e.getMessage());
        }
    }

    @Test
    public void passwordTest() {
        try {
            for (int i = 0; i < 10; i++) {
                System.out.println(generate(8));
            }
        } catch (Exception e) {
            Assert.fail("exception not expected; " + e.getMessage());
        }
    }

    public static String generate(int length) {
        StringBuilder password = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            String charCategory = charCategories[random.nextInt(charCategories.length)];
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }

}
