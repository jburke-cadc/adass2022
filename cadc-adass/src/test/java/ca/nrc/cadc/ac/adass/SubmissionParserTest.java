/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 20202
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SubmissionParserTest {

    @Test
    public void testParseSubmissions() throws Exception {
        InputStream is = Files.newInputStream(Paths.get("src/test/resources/submissions.json"));
        SubmissionsParser parser = new SubmissionsParser();
        Submissions submissions = parser.parseSubmissions(is);

        Assert.assertNotNull(submissions);
        Assert.assertEquals(3, submissions.count);
        Assert.assertNotNull(submissions.results);

        List<Result> results = submissions.results;
        Assert.assertEquals(3, results.size());

        Result result = results.get(0);
        Assert.assertEquals("ABCD99", result.code);
        Assert.assertEquals("ABCD99 title", result.title);
        Assert.assertEquals("accepted", result.state);
        Assert.assertNotNull(result.speakers);
        List<Speaker> speakers = result.speakers;
        Assert.assertEquals(1, speakers.size());
        Speaker speaker = speakers.get(0);
        Assert.assertEquals("GMYGLN", speaker.code);
        Assert.assertEquals("Peter Parker", speaker.name);
        Assert.assertEquals("spidey@avengers.org", speaker.email);

        result = results.get(1);
        Assert.assertEquals("R7JNSY", result.code);
        Assert.assertEquals("R7JNSY title", result.title);
        Assert.assertEquals("submitted", result.state);
        Assert.assertNotNull(result.speakers);
        speakers = result.speakers;
        Assert.assertEquals(1, speakers.size());
        speaker = speakers.get(0);
        Assert.assertEquals("CQWEAT", speaker.code);
        Assert.assertEquals("J. Jonah Jameson", speaker.name);
        Assert.assertEquals("jjj@dailybugle.com", speaker.email);

        result = results.get(2);
        Assert.assertEquals("MK3MHL", result.code);
        Assert.assertEquals("MK3MHL title", result.title);
        Assert.assertEquals("submitted", result.state);
        Assert.assertNotNull(result.speakers);
        speakers = result.speakers;
        Assert.assertEquals(2, speakers.size());
        speaker = speakers.get(0);
        Assert.assertEquals("KDLUKX", speaker.code);
        Assert.assertEquals("Green Goblin", speaker.name);
        Assert.assertEquals("green.guy@villians.net", speaker.email);
        speaker = speakers.get(1);
        Assert.assertEquals("WBTSTY", speaker.code);
        Assert.assertEquals("Doctor Octopus", speaker.name);
        Assert.assertEquals("dococt88@gmail.com", speaker.email);
    }

}
