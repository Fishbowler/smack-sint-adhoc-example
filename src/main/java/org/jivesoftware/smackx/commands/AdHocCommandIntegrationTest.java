package org.jivesoftware.smackx.commands;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdHocCommandIntegrationTest extends AbstractSmackIntegrationTest {

    private final AdHocCommandManager adHocCommandManagerForAdmin;
    private final AbstractXMPPConnection adminConnection;

    private final List<FormField.Type> NON_STRING_FORM_FIELD_TYPES = Arrays.asList(
        FormField.Type.jid_multi,
        FormField.Type.list_multi
    );

    private static final String EDIT_BLOCKED_LIST = "http://jabber.org/protocol/admin#edit-blacklist";

    public AdHocCommandIntegrationTest(SmackIntegrationTestEnvironment environment)
        throws InvocationTargetException, InstantiationException, IllegalAccessException, SmackException, IOException, XMPPException, InterruptedException {
        super(environment);

        adminConnection = environment.connectionManager.getDefaultConnectionDescriptor().construct(sinttestConfiguration);
        adminConnection.connect();
        adminConnection.login(sinttestConfiguration.adminAccountUsername,
            sinttestConfiguration.adminAccountPassword);

        adHocCommandManagerForAdmin = AdHocCommandManager.getAddHocCommandsManager(adminConnection);
    }


    private AdHocCommandData executeCommandSimple(String commandNode, Jid jid) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        return command.execute().getResponse();
    }
    private AdHocCommandData executeCommandWithArgs(String commandNode, Jid jid, String... args) throws Exception {
        AdHocCommand command = adHocCommandManagerForAdmin.getRemoteCommand(jid, commandNode);
        AdHocCommandResult.StatusExecuting result = command.execute().asExecutingOrThrow();
        FillableForm form = result.getFillableForm();

        for (int i = 0; i < args.length; i += 2) {
            FormField field = form.getField(args[i]);
            if (field == null) {
                throw new IllegalStateException("Field " + args[i] + " not found in form");
            }
            if (NON_STRING_FORM_FIELD_TYPES.contains(field.getType())){
                form.setAnswer(args[i], Stream.of(args[i + 1]
                    .split(",", -1))
                    .map(String::trim)
                    .collect(Collectors.toList()));
            } else {
                form.setAnswer(args[i], args[i + 1]);
            }
        }

        SubmitForm submitForm = form.getSubmitForm();

        return command.
            complete(submitForm).getResponse();
    }

    private void assertFormFieldEquals(String fieldName, String expectedValue, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(expectedValue, field.getFirstValue());
    }

    private void assertFormFieldEquals(String fieldName, List<String> expectedValues, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        List<String> fieldValues = field.getValues().stream().map(CharSequence::toString).collect(Collectors.toList());
        assertEquals(fieldValues.size(), expectedValues.size());
        assertTrue(fieldValues.containsAll(expectedValues));
    }

    private void assertFormFieldExists(String fieldName, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertNotNull(field);
    }

    private void assertFormFieldHasValues(String fieldName, int valueCount, AdHocCommandData data) {
        FormField field = data.getForm().getField(fieldName);
        assertEquals(valueCount, field.getValues().size());
    }

    private void assertNoteType(AdHocCommandNote.Type expectedType, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertEquals(expectedType, note.getType());
    }

    private void assertNoteEquals(String expectedValue, AdHocCommandData data) {
        AdHocCommandNote note = data.getNotes().get(0);
        assertTrue(note.getValue().contains(expectedValue));
    }

    //node="http://jabber.org/protocol/admin#edit-blacklist" name="Edit Blocked List"
    @SmackIntegrationTest
    public void testEditBlackList() throws Exception {
        final String BLACKLIST_DOMAIN = "xmpp.someotherdomain.org";

        // Pretend it's a 1-stage command initially, so that we can check that the current list of Blocked Users is populated
        AdHocCommandData result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());
        assertFormFieldHasValues("blacklistjids", 0, result);

        result = executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
            "blacklistjids", BLACKLIST_DOMAIN
        );

        assertNoteType(AdHocCommandNote.Type.info, result);
        assertNoteEquals("Operation finished successfully", result);

        // Pretend it's a 1-stage command again, so that we can check that the new list of Blocked Users is correct
        result = executeCommandSimple(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid());
        assertFormFieldEquals("blacklistjids", BLACKLIST_DOMAIN, result);

        //Clean-up
        //TODO: FIND A WAY TO RETURN THE BLACKLIST TO AN EMPTY STATE
        
        //executeCommandWithArgs(EDIT_BLOCKED_LIST, adminConnection.getUser().asEntityBareJid(),
        //    "blacklistjids", null
        //);
    }
}
