package com.zimory.ldapunit.core.it;

import java.io.ByteArrayInputStream;

import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.zimory.ldapunit.core.DirectoryServerAccess;
import com.zimory.ldapunit.core.InMemoryDirectoryServerAccess;
import com.zimory.ldapunit.core.LdapWatcher;
import com.zimory.ldapunit.core.ShouldMatchLdapDataSet;
import com.zimory.ldapunit.core.UsingLdapDataSet;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class LdapWatcherIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapWatcherIT.class);

    private static InMemoryDirectoryServer ldapServer;

    @BeforeClass
    public static void setUpClass() throws Exception {
        final InMemoryDirectoryServerConfig cfg = new InMemoryDirectoryServerConfig(Constants.ROOT_CONTEXT_DN);
        cfg.addAdditionalBindCredentials("cn=Manager," + Constants.ROOT_CONTEXT_DN, "opensesame");

        final int ldapPort = Ports.getRandomUnusedPort();

        final InMemoryListenerConfig listenerCfg = new InMemoryListenerConfig(
                "test-ldap-listener", null, ldapPort, null, null, null);

        cfg.setListenerConfigs(listenerCfg);
        cfg.setSchema(null);

        ldapServer = new InMemoryDirectoryServer(cfg);
        ldapServer.startListening();

        final InputSupplier<ByteArrayInputStream> ldif = ByteStreams.newInputStreamSupplier(
                Constants.ROOT_CONTEXT_ENTRY.getBytes(Charsets.UTF_8));

        final InMemoryDirectoryServerAccess serverAccess = new InMemoryDirectoryServerAccess(
                ldapServer, Constants.ROOT_CONTEXT_DN);

        LOGGER.debug("Importing LDIF containing the root entry");

        serverAccess.importLdif(ldif);
    }

    @AfterClass
    public static void tearDownClass() {
        ldapServer.shutDown(true);
        ldapServer = null;
    }

    public static final class InnerTest {

        @Rule
        public LdapWatcher ldapWatcher = new LdapWatcher(new Supplier<DirectoryServerAccess>() {
            @Override
            public DirectoryServerAccess get() {
                return new InMemoryDirectoryServerAccess(ldapServer, Constants.ROOT_CONTEXT_DN);
            }
        });

        @Test
        @UsingLdapDataSet
        @ShouldMatchLdapDataSet
        public void findAndAddNewEntry() throws Exception {
            ldapServer.assertEntryExists("cn=SomeUser," + Constants.ROOT_CONTEXT_DN);
            ldapServer.add(new Entry("cn=SomeOtherUser," + Constants.ROOT_CONTEXT_DN, new Attribute("objectClass", "top")));
        }

    }

    @Test
    public void pretendTest() throws Exception {
        final Result result = JUnitCore.runClasses(InnerTest.class);

        if (!result.getFailures().isEmpty()) {
            for (final Failure failure : result.getFailures()) {
                failure.getException().printStackTrace();
            }
        }

        assertThat(result.wasSuccessful(), equalTo(true));
    }

}
