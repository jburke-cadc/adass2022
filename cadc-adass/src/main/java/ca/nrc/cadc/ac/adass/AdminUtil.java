/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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

import ca.nrc.cadc.util.StringUtil;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;

public class AdminUtil {

    private static final String USER_CONFIG_DIR = System.getProperty("user.home") + "/config/";

    public static  PropertyResourceBundle getProperties(String propsFilename, List<String> propsRequired)
        throws UsageException {

        PropertyResourceBundle resourceBundle;
        try {
            FileReader reader = new FileReader(USER_CONFIG_DIR + propsFilename);
            resourceBundle = new PropertyResourceBundle(reader);
        } catch (FileNotFoundException e) {
            throw new UsageException(String.format("%s - file not found: %s", propsFilename, e.getMessage()));
        } catch (IOException e) {
            throw new UsageException(String.format("%s - unable to read file: %s", propsFilename, e.getMessage()));
        }

        StringBuilder sb = new StringBuilder();
        for (String property : propsRequired) {
            if (!resourceBundle.containsKey(property)) {
                sb.append(String.format("\n%s - %s property missing", propsFilename, property));
            } else {
                String value = resourceBundle.getString(property);
                if (!StringUtil.hasText(value)) {
                    sb.append(String.format("\n%s - %s property has no value", propsFilename, property));
                }
            }
        }
        if (sb.length() > 0) {
            throw new UsageException(String.format("%s is incomplete\n%s", propsFilename, sb));
        }
        return resourceBundle;
    }

    public static BufferedWriter initWriter(String filename)
        throws UsageException {

        Path path = Paths.get(filename);
        BufferedWriter writer;
        try {
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UsageException(
                String.format("unable to write to file - %s: %s", filename, e.getMessage()));
        }
        return writer;
    }

    public static List<Proposal> getProposals(Connection connection)
        throws SQLException {
        List<Proposal> proposals = new ArrayList<>();
        String sql = "SELECT code,type,state,title,abstract,speaker_code,"
            + "speaker_name,speaker_email,username,password,folderUrl FROM cadcmisc.adass2022";
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            while (resultSet.next()) {
                String code = resultSet.getString("code");
                String type = resultSet.getString("type");
                String state = resultSet.getString("state");
                String title = resultSet.getString("title");
                String summary = resultSet.getString("abstract");
                String speaker_code = resultSet.getString("speaker_code");
                String speaker_name = resultSet.getString("speaker_name");
                String speaker_email = resultSet.getString("speaker_email");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String folderUrl = resultSet.getString("folderUrl");
                proposals.add(new Proposal(code, state, type, title, summary, username, password, folderUrl,
                                           new Speaker(speaker_code, speaker_name, speaker_email)));
            }
        }
        return proposals;
    }

    public static List<Proposal> getTestProposals()
        throws SQLException {
        List<Proposal> proposals = new ArrayList<>();

        String code = "code";
        String type = "type";
        String state = "state";
        String title = "title";
        String summary = "abstract";
        String speaker_code = "speaker-code";
        String speaker_name = "speaker-name";
        String speaker_email = "speaker-email";
        String username = "username";
        String password = "password";
        String folderUrl = "folder-url";
        proposals.add(new Proposal(code, state, type, title, summary, username, password, folderUrl,
                                   new Speaker(speaker_code, speaker_name, speaker_email)));

        code = "code";
        type = "type";
        state = "state";
        title = "title";
        summary = "abstract";
        speaker_code = "speaker-code";
        speaker_name = "speaker-name";
        speaker_email = "speaker-email";
        username = "username";
        password = "password";
        folderUrl = "folder-url";
        proposals.add(new Proposal(code, state, type, title, summary, username, password, folderUrl,
                                   new Speaker(speaker_code, speaker_name, speaker_email)));

        return proposals;
    }

}
