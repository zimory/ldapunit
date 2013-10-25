package com.zimory.ldapunit.core;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.InputSupplier;

/**
 * Provides uniform access to a directory server.
 */
public interface DirectoryServerAccess {

    /**
     * Imports the contents of the given LDIF into the directory server.
     * @param ldif supplier for the {@link java.io.InputStream} of the LDIF to import
     */
    void importLdif(InputSupplier<? extends InputStream> ldif) throws IOException;

    /**
     * Compares the contents of the given LDIF to the contents of the server.
     * @param expectedLdif a supplier for the {@link java.io.InputStream} of the expected LDIFs
     * @throws AssertionError if the contents don't match
     */
    void compareContents(InputSupplier<? extends InputStream> expectedLdif) throws IOException;

}
