# LdapUnit

Set up and expect LDAP server contents.

## Example usage

### Test
```
import com.google.common.base.Supplier;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import org.junit.Rule
import org.junit.Test;

import com.zimory.ldapunit.core.ShouldMatchLdapDataSet;
import com.zimory.ldapunit.core.UsingLdapDataSet;
import com.zimory.ldapunit.core.LdapWatcher;
import com.zimory.ldapunit.core.DirectoryServerAccess;
import com.zimory.ldapunit.core.InMemoryDirectoryServerAccess;

public class SomeIT extends AbstractIT {

    private static final String ROOT_CONTEXT_DN = "dc=example,dc=com";

    @Inject
    private InMemoryDirectoryServer ldapServer;

    @Rule
    public LdapWatcher ldapWatcher = new LdapWatcher(new Supplier<DirectoryServerAccess>() {
        @Override
        public DirectoryServerAccess get() {
            return new InMemoryDirectoryServerAccess(ldapServer, ROOT_CONTEXT_DN);
        }
    });

    @Test
    @UsingLdapDataSet
    @ShouldMatchLdapDataSet
    public void findAndAddNewEntry() throws Exception {
        ldapServer.assertEntryExists("cn=SomeUser," + ROOT_CONTEXT_DN);
        ldapServer.add(new Entry("cn=SomeOtherUser," + ROOT_CONTEXT_DN, new Attribute("objectClass", "top")));
    }

}
```

### Datasets
#### /ldifs/findAndAddNewEntry.ldif
```
dn: cn=SomeUser,dc=example,dc=com
cn: SomeUser
objectClass: top
```
#### /ldifs/expected-findAndAddNewEntry.ldif
```
dn: cn=SomeUser,dc=example,dc=com
cn: SomeUser
objectClass: top

dn: cn=SomeOtherUser,dc=example,dc=com
cn: SomeOtherUser
objectClass: top
```
## LDAP servers

### Using com.unboundid.ldap.listener.InMemoryDirectoryServer
Just instantiate ```com.zimory.ldapunit.core.InMemoryDirectoryServerAccess``` as shown in the example above and pass it to the LdapWatcher rule.
### Using some other LDAP server implementation
Provide an implementation of the ```com.zimory.ldapunit.core.DirectoryServerAccess``` interface yourself.

## Distribution
```
<dependency>
    <groupId>com.zimory.ldapunit</groupId>
    <artifactId>ldapunit-core</artifactId>
    <version>1.0.0</version>
</dependency>
```
