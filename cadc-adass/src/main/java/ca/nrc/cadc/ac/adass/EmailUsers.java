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

import ca.nrc.cadc.net.TransientException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.security.AccessControlException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.log4j.Logger;

public class EmailUsers extends AbstractCommand {

    private static final Logger log = Logger.getLogger(EmailUsers.class);

    private static final List<String> MAIL_PROPS =
        Stream.of(Mailer.MAIL_FROM, Mailer.MAIL_REPLY_TO, Mailer.MAIL_SUBJECT, Mailer.MAIL_BODY)
            .collect(Collectors.toList());

    public static final List<String> SMTP_PROPS =
        Stream.of(Mailer.SMTP_HOST, Mailer.SMTP_PORT).collect(Collectors.toList());

    private final String propertiesFile;
    private final String emailPropertiesFile;
    private final String logFile;
    private final boolean dryRun;
    private PropertyResourceBundle smtpProps;
    private PropertyResourceBundle mailProps;
    private BufferedWriter logWriter;
    private Connection connection;
    private String body;
    private int total;
    private int sent;
    private int skipped;
    private int error;

    public EmailUsers(String propertiesFile, String emailPropertiesFile, String logFile, boolean dryRun)
        throws UsageException {
        this.propertiesFile = propertiesFile;
        this.emailPropertiesFile = emailPropertiesFile;
        this.logFile = logFile;
        this.dryRun = dryRun;
        init();
    }

    /**
     * Can be run without Subject.doAs.
     */
    @Override
    protected void doRun()
        throws AccessControlException, TransientException {
        if (this.dryRun) {
            logMessage("dry run: logging only, no emails will be sent");
        }

        // Get list of emails to send
        List<Proposal> proposals;
        try {
            proposals = AdminUtil.getProposals(this.connection);
            //proposals = AdminUtil.getTestProposals();
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Error getting Proposal list from DB: %s", e.getMessage()));
        }
        this.total = proposals.size();
        logMessage(String.format("emails to send - %s", total));

        List<Proposal> toSend = new ArrayList<>();
        for (Proposal proposal : proposals) {
            try {
                boolean strict = true;
                new InternetAddress(proposal.speaker.email, strict);
                toSend.add(proposal);
            } catch (AddressException e) {
                logMessage(String.format("%s - invalid email address: %s", proposal.code, proposal.speaker.email));
                this.skipped++;
            }
        }

        for (Proposal proposal : toSend) {
            // order: name, type, title, PID/username, password, folderUrl
            String body = String.format(this.body, proposal.speaker.name, proposal.type.getValue(), proposal.title,
                                        proposal.username, proposal.password, proposal.folderUrl);

            if (this.dryRun) {
                logMessage(String.format("%s - %s - email to send", proposal.code, proposal.speaker.email));
                this.sent++;
            } else {
                try {
                    sendEmail(proposal.speaker.email, body);
                    logMessage(String.format("%s - %s - email sent", proposal.code, proposal.speaker.email));
                    this.sent++;
                } catch (MessagingException e) {
                    logMessage(String.format("%s - error sending: %s", proposal.code, e.getMessage()));
                    this.error++;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException("Thread sleep interrupted", e);
                }
            }
        }

        // log results
        logMessage(String.format("  total - %s", total));
        logMessage(String.format("   sent - %s", sent));
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
        this.sent = 0;
        this.skipped = 0;
        this.error = 0;
        this.smtpProps = AdminUtil.getProperties(emailPropertiesFile, SMTP_PROPS);
        this.mailProps = AdminUtil.getProperties(emailPropertiesFile, MAIL_PROPS);
        this.logWriter = AdminUtil.initWriter(logFile);
        this.body = mailProps.getString(Mailer.MAIL_BODY);
        this.connection = initDatabase(this.propertiesFile);
    }

    protected void sendEmail(String email, String body)
        throws MessagingException {

        if (this.dryRun) {
            return;
        }

        Mailer mailer = new Mailer();
        mailer.setSmtpHost(smtpProps.getString(Mailer.SMTP_HOST));
        mailer.setSmtpPort(smtpProps.getString(Mailer.SMTP_PORT));

        mailer.setToList(new String[] { email });
        mailer.setReplyToList(new String[] { mailProps.getString(Mailer.MAIL_REPLY_TO)});
        mailer.setBccList(new String[] { mailProps.getString(Mailer.MAIL_BCC)});
        mailer.setFrom(mailProps.getString(Mailer.MAIL_FROM));
        mailer.setSubject(mailProps.getString(Mailer.MAIL_SUBJECT));
        mailer.setBody(body);

        mailer.setContentType(Mailer.HTML_CONTENT_TYPE);

        boolean authenticated = false;
        mailer.doSend(authenticated);
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
