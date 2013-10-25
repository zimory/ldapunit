package com.zimory.ldapunit.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.InputSupplier;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.ldif.LDIFWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Provides access to an {@link com.unboundid.ldap.listener.InMemoryDirectoryServer}.
 */
public class InMemoryDirectoryServerAccess implements DirectoryServerAccess {

    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryDirectoryServerAccess.class);

    private final InMemoryDirectoryServer server;
    private final String rootContextDn;

    public InMemoryDirectoryServerAccess(final InMemoryDirectoryServer server, final String rootContextDn) {
        this.server = server;
        this.rootContextDn = rootContextDn;
    }

    @Override
    public void importLdif(final InputSupplier<? extends InputStream> ldif) throws IOException {
        LOGGER.debug("Importing LDIF into LDAP server");

        final BufferedReader reader = new BufferedReader(new InputStreamReader(ldif.getInput()));
        Preconditions.checkNotNull(reader, "reader cannot be null");

        try {
            server.importFromLDIF(false, new LDIFReader(reader));
        } catch (final LDAPException e) {
            throw new RuntimeException(e);
        } finally {
            reader.close();
        }
    }

    @Override
    public void compareContents(InputSupplier<? extends InputStream> expectedLdif) throws IOException {
        LOGGER.debug("Matching expected LDIF against LDAP contents");

        assertEntriesMatch(readEntries(expectedLdif.getInput()), readEntriesFromServer());
    }

    private static List<Entry> readEntries(final InputStream in) throws IOException {
        try {
            return LDIFReader.readEntries(in);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            in.close();
        }
    }

    private List<Entry> readEntriesFromServer() {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            server.exportToLDIF(new LDIFWriter(out), true, true, true);

            return withoutRootContextEntry(LDIFReader.readEntries(new ByteArrayInputStream(out.toByteArray())));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void assertEntriesMatch(final List<Entry> expectedEntries, final List<Entry> actualEntries) {
        sortEntries(expectedEntries);
        sortEntries(actualEntries);

        final int actualSize = actualEntries.size();
        final int expectedSize = expectedEntries.size();

        assertThat(String.format("Number of entries differs: %n" +
                "\tExpected entries: %n\t\t%s%n" +
                "\tActual entries: %n\t\t%s",
                Joiner.on("\n\t\t").join(Lists.transform(expectedEntries, Functions.toStringFunction())),
                Joiner.on("\n\t\t").join(Lists.transform(actualEntries, Functions.toStringFunction()))),
                actualSize,
                equalTo(expectedSize));

        for (int i = 0; i < expectedSize; i++) {
            final Entry actual = actualEntries.get(i);
            final Entry expected = expectedEntries.get(i);

            assertThat(String.format("Entries' DNs differ at index %d", i), actual.getDN(), equalTo(expected.getDN()));

            final List<Attribute> actualAttributes = Lists.newArrayList(actual.getAttributes());
            final List<Attribute> expectedAttributes = Lists.newArrayList(expected.getAttributes());

            assertAttributesMatch(i, actualAttributes, expectedAttributes);
        }
    }

    private static void assertAttributesMatch(
            final int entryIdx, final List<Attribute> actualAttributes, final List<Attribute> expectedAttributes) {
        sortAttributes(actualAttributes);
        sortAttributes(expectedAttributes);

        final int actualSize = actualAttributes.size();
        final int expectedSize = expectedAttributes.size();

        assertThat(String.format("Number of attributes differs for entry at index %d: %n" +
                "\tExpected attributes: %s%n" +
                "\tActual attributes: %s",
                entryIdx,
                actualAttributes,
                expectedAttributes), actualSize, equalTo(expectedSize));

        for (int i = 0; i < expectedSize; i++) {
            final Attribute actualAttribute = actualAttributes.get(i);
            final Attribute expectedAttribute = expectedAttributes.get(i);

            assertThat("Attributes differ for entry at index " + entryIdx, actualAttribute, equalTo(expectedAttribute));
        }
    }

    private static void sortEntries(final List<Entry> entries) {
        Collections.sort(entries, new Comparator<Entry>() {
            @Override
            public int compare(final Entry o1, final Entry o2) {
                return o1.getDN().compareTo(o2.getDN());
            }
        });
    }

    private static void sortAttributes(final List<Attribute> attributes) {
        Collections.sort(attributes, new Comparator<Attribute>() {
            @Override
            public int compare(final Attribute o1, final Attribute o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    private List<Entry> withoutRootContextEntry(final List<Entry> entries) {
        return Lists.newArrayList(Iterables.filter(entries, new Predicate<Entry>() {
            @Override
            public boolean apply(final Entry input) {
                return !rootContextDn.equals(input.getDN());
            }
        }));
    }

}
