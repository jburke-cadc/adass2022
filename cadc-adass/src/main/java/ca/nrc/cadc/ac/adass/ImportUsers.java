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
import ca.nrc.cadc.ac.GroupAlreadyExistsException;
import ca.nrc.cadc.ac.GroupNotFoundException;
import ca.nrc.cadc.ac.PersonalDetails;
import ca.nrc.cadc.ac.User;
import ca.nrc.cadc.ac.UserAlreadyExistsException;
import ca.nrc.cadc.ac.UserNotFoundException;
import ca.nrc.cadc.ac.UserRequest;
import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.auth.HttpPrincipal;
import ca.nrc.cadc.auth.SSLUtil;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceAlreadyExistsException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.vos.ContainerNode;
import ca.nrc.cadc.vos.NodeNotFoundException;
import ca.nrc.cadc.vos.NodeProperty;
import ca.nrc.cadc.vos.VOS;
import ca.nrc.cadc.vos.VOSURI;
import ca.nrc.cadc.vos.client.VOSpaceClient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.opencadc.gms.GroupURI;

public class ImportUsers extends AbstractCommand {

    public static final String PRETALX_API_URL = "pretalx.apiUrl";
    public static final String PRETALX_TOKEN = "pretalx.token";
    public static final String  CADC_CERT_GEN_EXEC = "cadc-cert-gen.exec";
    public static final String CADC_CERT_GEN_CALLING_CERT = "cadc-cert-gen.callingCert";
    public static final String  CADC_CERT_GEN_SIGNING_CERT = "cadc-cert-gen.signingCert";
    public static final String  CADC_CERT_GEN_RESOURCE_ID = "cadc-cert-gen.resourceID";
    public static final String  CADC_CERT_GEN_SERVER = "cadc-cert-gen.server";
    public static final String  CADC_CERT_GEN_DATABASE = "cadc-cert-gen.database";
    public static final String  VAULT_RESOURCE_ID = "vault.resourceID";
    public static final String VAULT_ADMIN_USER = "vault.adminUser";
    public static final String VAULT_ADMIN_USER_CERT = "vault.adminUser.cert";
    public static final String VAULT_ROOT_NODE = "vault.rootNode";
    public static final String VAULT_ADASS_URI = "vault.adassURI";
    public static final String VAULT_ADASS_URL = "vault.adassURL";
    public static final String VAULT_USER_FOLDER_PREFIX = "vault.userFolderPrefix";

    public static final List<String> IMPORT_PROPS =
        Stream.of(PRETALX_API_URL, PRETALX_TOKEN, CADC_CERT_GEN_EXEC, CADC_CERT_GEN_CALLING_CERT,
                  CADC_CERT_GEN_SIGNING_CERT, CADC_CERT_GEN_RESOURCE_ID, CADC_CERT_GEN_SERVER,
                  CADC_CERT_GEN_DATABASE, VAULT_RESOURCE_ID, VAULT_ADMIN_USER, VAULT_ADMIN_USER_CERT,
                  VAULT_ADASS_URI, VAULT_ADASS_URL, VAULT_USER_FOLDER_PREFIX)
            .collect(Collectors.toList());

    private static final String[] passwordChars = new String[] {
        "abcdefghijkmnopqrstuvwxyz",
        "ABCDEFGHJKLMNOPQRSTUVWXYZ",
        "0123456789"
    };

    private BufferedWriter logWriter;
    private CertGen certGen;
    private final String propertiesFile;
    private final String logFile;
    private final boolean dryRun;
    private URL apiURL;
    private HttpPrincipal callerHttpPrincipal;
    private Subject adminUserSubject;
    private Group adassAdminGroup;
    private String vaultAdassUri;
    private String vaultAdassUrl;
    private String vaultUserFolderPrefix;
    private URI groupResourceId;
    private int total;
    private int imported;
    private int skipped;
    private int error;

    private Connection connection;
    public VOSpaceClient voSpaceClient;
    public PropertyResourceBundle importProps;

    public ImportUsers(String propertiesFile, String logFile, boolean dryRun)
        throws UsageException {
        this.propertiesFile = propertiesFile;
        this.logFile = logFile;
        this.dryRun = dryRun;
        init();
    }

    @Override protected void doRun() {
        if (this.dryRun) {
            logMessage("dry run: logging only, no user will be imported");
        }

        // Calling subject, expected to be cadcops
        this.callerHttpPrincipal = getCallerHttpPrincipal();

        // should be in init(), but needs ldap persistence
        this.adassAdminGroup = getAdassAdminGroup();

        // Get the list of User's.
        Submissions submissions = getSubmissions();
        this.total = submissions.results.size();
        logMessage(String.format("users to import - %s", total));

        // distinct list of proposal codes to eliminate duplicates
        Set<String> proposals = new TreeSet<>();

        // Create a user and group for each user
        List<Result> results = submissions.results;
        Speaker speaker;
        for (Result result : results) {
            Proposal proposal = new Proposal(result);
            logMessage(String.format("proposal: %s", proposal.code));

            if (proposal.type.equals("Poster block") ||
                proposal.type.equals("Goodbye") ||
                proposal.type.equals("Welcome")) {
                logMessage(String.format("%s - skipping submissionType: %s",
                                         proposal.code, proposal.type));
                skipped++;
                continue;
            }

            // do not process proposal's with state = withdrawn
            if (proposal.state.equals("withdrawn")) {
                logMessage(String.format("%s - proposal state=withdrawn, skipping", proposal.code));
                skipped++;
                continue;
            }

            // returns false is set contains username
            if (!proposals.add(proposal.code)) {
                logMessage(String.format("%s - duplicate proposal code, skipping", proposal.code));
                skipped++;
                continue;
            }
            try {
                // check if proposal has already been processed (user,group,folder created)
                if (isExistingProposal(proposal)) {
                    logMessage(String.format("%s - existing proposal, skipping", proposal.code));
                    skipped++;
                    continue;
                }

                // Create DN by calling cadc-cert-gen
                String dn = getDistinguishedName(proposal.code);
                logMessage(String.format("dn: %s", dn));

                // Create and approve UserRequest
                proposal.username = createUsername(proposal);
                proposal.password = createPassword();
                logMessage(String.format("pw: %s", proposal.password));
                User user = createUser(proposal, dn);

                // Create user group for vault permissions
                Group adassUserGroup = getAdassUserGroup(user);

                // Create vault folder
                proposal.folderUrl = createVaultFolder(user, adassUserGroup);
                logMessage(String.format("folder URL: %s", proposal.folderUrl));

                // write proposal to the DB
                saveProposal(proposal);
                logMessage(String.format("%s - imported proposal", proposal.code));
                this.imported++;
            } catch (IllegalStateException e) {
                logMessage(String.format("%s - error importing proposal: %s",
                                         proposal.code, e.getMessage()));
                this.error++;
            } catch (SQLException e) {
                logMessage(String.format("%s - database error: %s",
                                         proposal.code, e.getMessage()));
                this.error++;
            }
        }

        // log results
        logMessage(String.format("   total - %s", total));
        logMessage(String.format("imported - %s", imported));
        logMessage(String.format(" skipped - %s", skipped));
        logMessage(String.format("   error - %s", error));
        try {
            this.logWriter.close();
        } catch (IOException ignore) {
        }
    }

    protected void init()
        throws UsageException {
        this.total = 0;
        this.imported = 0;
        this.skipped = 0;
        this.error = 0;
        this.logWriter = AdminUtil.initWriter(logFile);

        this.importProps = AdminUtil.getProperties(propertiesFile, IMPORT_PROPS);
        String apiUrl = importProps.getString(PRETALX_API_URL);
        try {
            this.apiURL = new URL(apiUrl);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("config property %s is not a valid URL: %s",
                                                             PRETALX_API_URL, apiUrl));
        }

        String exec = importProps.getString(CADC_CERT_GEN_EXEC);
        String callingCert = importProps.getString(CADC_CERT_GEN_CALLING_CERT);
        String signingCert = importProps.getString(CADC_CERT_GEN_SIGNING_CERT);
        String credResourceID = importProps.getString(CADC_CERT_GEN_RESOURCE_ID);
        String server = importProps.getString(CADC_CERT_GEN_SERVER);
        String database = importProps.getString(CADC_CERT_GEN_DATABASE);
        this.certGen = new CertGen(exec, callingCert, signingCert, credResourceID, server, database);

        String vaultResourceID = importProps.getString(ImportUsers.VAULT_RESOURCE_ID);
        URI vaultURI;
        try {
            vaultURI = new URI(vaultResourceID);
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("%s invalid uri: %s",
                                                     VAULT_RESOURCE_ID, vaultResourceID));
        }
        this.voSpaceClient = new VOSpaceClient(vaultURI);

        String adminUserCert = importProps.getString(VAULT_ADMIN_USER_CERT);
        this.adminUserSubject = SSLUtil.createSubject(new File(adminUserCert));
        this.vaultAdassUri = importProps.getString(VAULT_ADASS_URI);
        this.vaultAdassUrl = importProps.getString(VAULT_ADASS_URL);
        this.vaultUserFolderPrefix = importProps.getString(VAULT_USER_FOLDER_PREFIX);

        this.groupResourceId = URI.create("ivo://cadc.nrc.ca/gms");
        this.connection = initDatabase(propertiesFile);
    }

    protected HttpPrincipal getCallerHttpPrincipal() {
        if (this.dryRun) {
            return new HttpPrincipal("dryrun");
        } else {
            Subject subject = AuthenticationUtil.getCurrentSubject();
            Subject caller = AuthenticationUtil.augmentSubject(subject);
            Set<HttpPrincipal> callerPrincipals = caller.getPrincipals(HttpPrincipal.class);
            if (callerPrincipals.isEmpty()) {
                throw new RuntimeException("HttpPrincipal not found for --cert subject");
            }
            return callerPrincipals.iterator().next();
        }
    }

    protected Group getAdassAdminGroup() {
        if (this.dryRun) {
            return new Group(new GroupURI(URI.create("ivo://dry/run?please")));
        }

        // subject code for dev
        //File adminCert = new File("/Volumes/work/cadc/test-certificates/adass2022.pem");
        //Subject subject = SSLUtil.createSubject(adminCert);
        //subject = AuthenticationUtil.augmentSubject(subject);

        // subject code for prod
        Subject subject = AuthenticationUtil.augmentSubject(adminUserSubject);

        return Subject.doAs(subject, (PrivilegedAction<Group>) () -> {
            String adminUser = importProps.getString(VAULT_ADMIN_USER);
            try {
                return getGroupPersistence().getGroup(adminUser);
            } catch (GroupNotFoundException e) {
                throw new RuntimeException(String.format("ADASS admin Group not found: %s", adminUser));
            }
        });
    }

    protected Submissions getSubmissions() {
        logMessage(String.format("pretalx url: %s", this.apiURL));
        HttpGet get = new HttpGet(this.apiURL, true);
        String token = importProps.getString(PRETALX_TOKEN);
        get.setRequestProperty("Authorization", token);
        try {
            get.prepare();
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException(String.format("API endpoint %s not found because %s",
                                                     apiURL, e.getMessage()));
        } catch (ResourceAlreadyExistsException | IOException | InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        }

        Submissions submissions;
        SubmissionsParser parser = new SubmissionsParser();
        try {
            submissions = parser.parseSubmissions(get.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error downloading submissions: %s", e.getMessage()));
        }
        return submissions;
    }

    protected boolean isExistingProposal(Proposal proposal)
        throws SQLException {
        String sql = String.format("select code from cadcmisc.adass2022 where code = '%s'", proposal.code);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            return resultSet.next();
        } catch (SQLException e) {
            throw e;
        }
    }

    protected String getDistinguishedName(String userID) {
        if (this.dryRun) {
            return "cn=dryrun";
        }
        return certGen.genCert(userID);
    }

    protected String createUsername(Proposal proposal)
    throws SQLException {
        String prefix = getTypePrefix(proposal.type);
        if (prefix == null) {
            throw new IllegalStateException(String.format("%s - unknown submissionType: %s",
                                                          proposal.code, proposal.type));
        }
        String sql = "select username from cadcmisc.adass2022 where username like '"
            + prefix + "%' order by username desc";
        logMessage("sql: " + sql);
        try (Statement statement = connection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(sql);
            int nextNum = 1;
            if (resultSet.next()) {
                String username = resultSet.getString(1);
                nextNum = Integer.parseInt(username.substring(1));
                nextNum++;
            }
            String ret = String.format("%s%02d", prefix, nextNum);
            logMessage("username: " + ret);
            return ret;
        }
    }

    protected String createPassword() {
        int length = 8;
        StringBuilder password = new StringBuilder(length);
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < length; i++) {
            String charCategory = passwordChars[random.nextInt(passwordChars.length)];
            int position = random.nextInt(charCategory.length());
            password.append(charCategory.charAt(position));
        }
        return new String(password);
    }

    protected User createUser(Proposal proposal, String distinguishedName) {
        if (this.dryRun) {
            return new User();
        }

        HttpPrincipal userHttpPrincipal = new HttpPrincipal(proposal.code);
        Subject userSubject = new Subject();
        userSubject.getPrincipals().add(userHttpPrincipal);
        userSubject.getPublicCredentials().add(AuthMethod.PASSWORD);

        // Check if user already exists
        User user = Subject.doAs(userSubject, (PrivilegedAction<User>) () -> {
            User existing = null;
             try {
                 existing = getUserPersistence().getUser(new HttpPrincipal(proposal.code));
             } catch (UserNotFoundException e) {
                 // ignore
             }
             return existing;
        });
        if (user != null) {
            return user;
        }

        // Create new UserRequest
        UserRequest userRequest = createUserRequest(proposal, distinguishedName);
        try {
            getUserPersistence().addUserRequest(userRequest, callerHttpPrincipal);
        } catch (UserNotFoundException e) {
            throw new IllegalStateException(String.format("Adding UserRequest, username %s not found: %s",
                                                          proposal.code, e.getMessage()));
        } catch (UserAlreadyExistsException e) {
            // ignore
        }
        logMessage("Created UserRequest");

        // Approve the UserRequest, done as the user.
        user = Subject.doAs(userSubject, (PrivilegedAction<User>) () -> {
            try {
                return getUserPersistence().approveUserRequest(userHttpPrincipal);
            } catch (UserNotFoundException e) {
                throw new IllegalStateException(String.format("Approving UserRequest, username %s not found: %s",
                                                              proposal.code, e.getMessage()));
            }
        });
        logMessage("Approved UserRequest");
        return user;
    }

    protected UserRequest createUserRequest(Proposal proposal, String distinguishedName) {
        String[] name = proposal.speaker.name.split("\\s+", 2);
        PersonalDetails personalDetails = new PersonalDetails(name[0], name[1]);
        personalDetails.email = String.format("%s@adass2022.ca", proposal.code);
        personalDetails.address = proposal.speaker.email;
        User user = new User();
        user.personalDetails = personalDetails;
        HttpPrincipal httpPrincipal = new HttpPrincipal(proposal.code);
        X500Principal x500Principal = new X500Principal(distinguishedName);
        user.getIdentities().add(httpPrincipal);
        user.getIdentities().add(x500Principal);
        UserRequest userRequest = new UserRequest(user, proposal.password.toCharArray());
        logMessage(String.format("UserRequest: %s", userRequest));
        return userRequest;
    }

    protected Group getAdassUserGroup(User user) {
        if (this.dryRun) {
            return new Group(new GroupURI(URI.create("ivo://adass/group?name")));
        }

        // Create user group for vault permissions
        String groupID = String.format("%s%s", vaultUserFolderPrefix, user.posixDetails.getUsername());
        GroupURI groupURI = new GroupURI(this.groupResourceId, groupID);
        Group vaultGroup = new Group(groupURI);
        vaultGroup.getUserMembers().add(user);
        try {
            vaultGroup = this.getGroupPersistence().addGroup(vaultGroup);
        } catch (GroupAlreadyExistsException e) {
            // ignore
        } catch (GroupNotFoundException e) {
            throw new IllegalStateException(String.format("Creating vault Group, group %s not found: %s",
                                                          groupID, e.getMessage()));
        } catch (UserNotFoundException e) {
            throw new IllegalStateException(String.format("Creating vault Group, user %s not found: %s",
                                                          user.posixDetails.getUsername(), e.getMessage()));
        }
        return vaultGroup;
    }

    protected String createVaultFolder(User user, Group adassUserGroup) {
        if (this.dryRun) {
            return "ivo:dryrun";
        }

        String folderName = user.posixDetails.getUsername();

        // VOSpaceClient calls made as adass2022 user.
        String folderUrl = Subject.doAs(adminUserSubject, (PrivilegedAction<String>) () -> {
            // Before completion, directory is visible in adass2022 directory,
            // but not readable except by adass-admin and calling user's group
            List<NodeProperty> nodeProperties = new ArrayList<>();
            setPermissions(nodeProperties, adassAdminGroup, adassUserGroup);

            String folderUri = String.format("%s/%s", vaultAdassUri, folderName);
            VOSURI folderURI;
            try {
                folderURI = new VOSURI(new URI(folderUri));
            } catch (URISyntaxException e) {
                throw new IllegalStateException(String.format("Invalid vault folder URI: %s because %s",
                                                              folderUri, e.getMessage()));
            }

            ContainerNode newNode = new ContainerNode(folderURI, nodeProperties);
            try {
                this.voSpaceClient.getNode(newNode.getUri().getPath());
            } catch (NodeNotFoundException e) {
                this.voSpaceClient.createNode(newNode);
            }
            return String.format("%s/%s", vaultAdassUrl, folderName);
        });
        logMessage(String.format("Created vault folder: %s", folderUrl));
        return folderUrl;
    }

    private void setPermissions(List<NodeProperty> nodeProperties, Group adminGroup, Group userGroup) {
        // Before completion, directory is visible in adass2022 directory,
        // but not readable except by adass-admin and user's group
        NodeProperty isPublic = new NodeProperty(VOS.PROPERTY_URI_ISPUBLIC, "false");
        nodeProperties.add(isPublic);

        // RO access to adass-admin
        NodeProperty rGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPREAD, adminGroup.getID().toString());
        nodeProperties.add(rGroup);

        // RW access to folder group
        NodeProperty writeGroup = new NodeProperty(VOS.PROPERTY_URI_GROUPWRITE, userGroup.getID().toString());
        nodeProperties.add(writeGroup);
    }

    protected void saveProposal(Proposal proposal)
        throws SQLException {
        String sql = "INSERT INTO cadcmisc.adass2022 "
            + "(code,type,state,title,abstract,speaker_code,speaker_name,"
            + "speaker_email,username,password,folderUrl) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            statement.setString(1,proposal.code);
            statement.setString(2,proposal.type);
            statement.setString(3,proposal.state);
            statement.setString(4,proposal.title);
            statement.setString(5,proposal.summary);
            statement.setString(6,proposal.speaker.code);
            statement.setString(7,proposal.speaker.name);
            statement.setString(8,proposal.speaker.email);
            statement.setString(9,proposal.username);
            statement.setString(10,proposal.password);
            statement.setString(11,proposal.folderUrl);
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            logMessage(String.format("%s - error inserting proposal: %s",
                                    proposal.code, e.getMessage()));
            if (connection != null) {
                try {
                    logMessage("rolling back transaction");
                    connection.rollback();
                } catch (SQLException ex) {
                    logMessage(String.format("%s - error rolling back transaction: %s",
                                            proposal.code, e.getMessage()));
                }
            }
            throw e;
        }
    }

    protected String getTypePrefix(String type) {
        String prefix = null;
        switch(type) {
            case "Birds of a Feather":
                prefix = "B";
                break;
            case "Contributed talk":
                prefix = "C";
                break;
            case "Focus Demo":
                prefix = "F";
                break;
            case "Invited Talk":
                prefix = "I";
                break;
            case "Poster":
                prefix = "P";
                break;
            case "Tutorial":
                prefix = "T";
                break;
        }
        return prefix;
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
