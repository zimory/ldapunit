package com.zimory.ldapunit.core.it;

final class Constants {

    static final String ROOT_CONTEXT_DN = "dc=zimory,dc=com";
    static final String ROOT_CONTEXT_ENTRY =
            "dn: " + ROOT_CONTEXT_DN + "\n" +
            "dc: zimory\n" +
            "objectClass: dcObject\n" +
            "objectClass: organizationalUnit\n" +
            "ou: zimory.com";

    private Constants() {
        throw new UnsupportedOperationException("Non-instantiable");
    }

}
