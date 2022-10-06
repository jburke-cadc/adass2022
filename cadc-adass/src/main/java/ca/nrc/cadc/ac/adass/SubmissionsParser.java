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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class SubmissionsParser {

    private static final Logger log = Logger.getLogger(SubmissionsParser.class);

    public SubmissionsParser() {
    }

    public Submissions parseSubmissions(InputStream is)
        throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(is));
        int count = 0;
        List<Result> results = null;
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String nextName = reader.nextName();
                if (nextName.equals("count")) {
                    count = reader.nextInt();
                } else if (nextName.equals("results") && reader.peek() != JsonToken.NULL) {
                    results = parseResults(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } finally {
            reader.close();
        }
        return new Submissions(count, results);
    }

    protected List<Result> parseResults(JsonReader reader)
        throws IOException {
        List<Result> results = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            results.add(parseResult(reader));
        }
        reader.endArray();
        return results;
    }

    protected Result parseResult(JsonReader reader)
        throws IOException {
        String code = null;
        String state = null;
        String type = null;
        String title = null;
        String summary = null;
        List<Speaker> speakers = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String nextName = reader.nextName();
            switch (nextName) {
                case "speakers":
                    if (reader.peek() != JsonToken.NULL) {
                        speakers = parseSpeakers(reader);
                    } else {
                        reader.skipValue();
                    }
                    break;
                case "code":
                    code = reader.nextString();
                    break;
                case "state":
                    state = reader.nextString();
                    break;
                case "submission_type":
                    type = parseSubmissionType(reader);
                    break;
                case "title":
                    title = reader.nextString().replace("\t", "");
                    break;
                case "abstract":
                    summary = reader.nextString().replace("\t", "");
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        return new Result(code, state, type, title, summary, speakers);
    }

    protected String parseSubmissionType(JsonReader reader)
        throws IOException {
        String type = null;
        reader.beginObject();
        while (reader.hasNext()) {
            if (reader.nextName().equals("en")) {
                type = reader.nextString();
                break;
            }
        }
        reader.endObject();
        return type;
    }

    protected List<Speaker> parseSpeakers(JsonReader reader)
        throws IOException {
        List<Speaker> speakers = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            speakers.add(parseSpeaker(reader));
        }
        reader.endArray();
        return speakers;
    }

    protected Speaker parseSpeaker(JsonReader reader)
        throws IOException {
        String code = null;
        String name = null;
        String email = null;
        reader.beginObject();
        while (reader.hasNext()) {
            String nextName = reader.nextName();
            switch (nextName) {
                case "code":
                    code = reader.nextString();
                    break;
                case "name":
                    name = reader.nextString();
                    break;
                case "email":
                    email = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
        if (code == null || name == null || email == null) {
            log.debug(String.format("null speaker parameter - code=%s, name=%s, email=%s", code, name, email));
        }
        return new Speaker(code, name, email);
    }

}
