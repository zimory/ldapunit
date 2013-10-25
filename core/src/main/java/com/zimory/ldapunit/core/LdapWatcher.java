package com.zimory.ldapunit.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.primitives.Bytes;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Collections2.filter;
import static com.google.common.collect.Lists.transform;

public final class LdapWatcher extends TestWatcher {

    public static final String LDIF_DIR = "/ldifs";
    public static final String EXPECTED_PREFIX = "expected-";

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapWatcher.class);

    private static final byte[] LINE_SEPARATOR = System.getProperty("line.separator").getBytes(Charsets.UTF_8);
    private static final byte[] ENTRY_SEPARATOR = Bytes.concat(LINE_SEPARATOR, LINE_SEPARATOR);

    private final Supplier<DirectoryServerAccess> ldapServer;

    public LdapWatcher(final Supplier<DirectoryServerAccess> ldapServer) {
        this.ldapServer = ldapServer;
    }

    @Override
    protected void starting(final Description description) {
        final Set<Description> descriptions = Sets.newLinkedHashSet();
        descriptions.addAll(getDescriptionsFromAnnotatedMethods(description, Before.class));
        descriptions.add(description);

        try {
            maybeImportLdifs(filterRelevantDescriptions(descriptions, UsingLdapDataSet.class));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void succeeded(final Description description) {
        final Set<Description> descriptions = Sets.newLinkedHashSet();
        descriptions.add(description);
        descriptions.addAll(getDescriptionsFromAnnotatedMethods(description, After.class));

        try {
            maybeMatchLdifs(filterRelevantDescriptions(descriptions, ShouldMatchLdapDataSet.class));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void maybeImportLdifs(final Collection<Description> descriptions) throws IOException {
        if (descriptions.isEmpty()) {
            return;
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (final Description description : descriptions) {
            final String relativePath = getRelativeLdifPath(description,
                    description.getAnnotation(UsingLdapDataSet.class));

            LOGGER.debug("Importing LDIF from '{}'", relativePath);

            ByteStreams.copy(getLdifFileStreamSupplier(relativePath), out);
            out.write(ENTRY_SEPARATOR);
        }

        ldapServer.get().importLdif(ByteStreams.newInputStreamSupplier(out.toByteArray()));
    }

    private Collection<Description> filterRelevantDescriptions(
            final Collection<Description> descriptions, final Class<? extends Annotation> annotation) {
        return filter(descriptions, new Predicate<Description>() {
            @Override
            public boolean apply(final Description input) {
                return input.getAnnotation(annotation) != null;
            }
        });
    }

    private void maybeMatchLdifs(final Collection<Description> descriptions) throws IOException {
        if (descriptions.isEmpty()) {
            return;
        }

        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        for (final Description description : descriptions) {
            final String relativePath = getRelativeLdifPath(description,
                    description.getAnnotation(ShouldMatchLdapDataSet.class));

            LOGGER.debug("Using LDIF from '{}' to match against LDAP contents", relativePath);

            ByteStreams.copy(getLdifFileStreamSupplier(relativePath), out);
            out.write(ENTRY_SEPARATOR);
        }

        ldapServer.get().compareContents(ByteStreams.newInputStreamSupplier(out.toByteArray()));
    }

    private static InputSupplier<? extends InputStream> getLdifFileStreamSupplier(final String relativePath) {
        final String path = formatLdifPath(LDIF_DIR, relativePath);

        try {
            final URL resource = LdapWatcher.class.getResource(path);
            Preconditions.checkNotNull(resource, "resource not found: %s", path);

            return Files.newInputStreamSupplier(new File(resource.toURI()));
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Collection<? extends Description> getDescriptionsFromAnnotatedMethods(
            final Description description,
            final Class<? extends Annotation> annotation) {
        final TestClass testClass = new TestClass(description.getTestClass());

        return transform(testClass.getAnnotatedMethods(annotation), new Function<FrameworkMethod, Description>() {
            @Override
            public Description apply(final FrameworkMethod input) {
                return Description.createTestDescription(input.getMethod().getDeclaringClass(), input.getName(),
                        input.getAnnotations());
            }
        });
    }

    private static String getRelativeLdifPath(final Description d, final UsingLdapDataSet a) {
        if (!Strings.isNullOrEmpty(a.value())) {
            return a.value();
        }

        return d.getTestClass().getSimpleName() + "/" + d.getMethodName();
    }

    private static String getRelativeLdifPath(final Description d, final ShouldMatchLdapDataSet a) {
        if (!Strings.isNullOrEmpty(a.value())) {
            return a.value();
        }

        return d.getTestClass().getSimpleName() + "/" + EXPECTED_PREFIX + d.getMethodName();
    }

    private static String formatLdifPath(final String baseDir, final String relativePath) {
        final String extension = "ldif".equals(Files.getFileExtension(relativePath)) ? "" : ".ldif";
        return String.format("%s/%s%s", baseDir, relativePath, extension);
    }

}
