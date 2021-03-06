/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.userstore.ldap.connector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.security.caas.user.core.bean.Group;
import org.wso2.carbon.security.caas.user.core.bean.User;
import org.wso2.carbon.security.caas.user.core.config.IdentityConnectorConfig;
import org.wso2.carbon.security.caas.user.core.constant.UserCoreConstants;
import org.wso2.carbon.security.caas.user.core.exception.CredentialStoreException;
import org.wso2.carbon.security.caas.user.core.exception.GroupNotFoundException;
import org.wso2.carbon.security.caas.user.core.exception.IdentityStoreException;
import org.wso2.carbon.security.caas.user.core.exception.UserNotFoundException;
import org.wso2.carbon.security.caas.user.core.store.connector.IdentityStoreConnector;
import org.wso2.carbon.userstore.ldap.constant.ConnectorConstants;
import org.wso2.carbon.userstore.ldap.datasource.LDAPConstants;
import org.wso2.carbon.userstore.ldap.datasource.beans.LDAPConnectionContext;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.StartTlsRequest;
import javax.naming.ldap.StartTlsResponse;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.*;


import static org.wso2.carbon.userstore.ldap.constant.ConnectorConstants.USER_SEARCH_BASE;




/**
 * LDAP based implementation for identity store connector.
 */
public class LDAPIdentityStoreConnector implements IdentityStoreConnector {


    private static Logger log = LoggerFactory.getLogger(LDAPIdentityStoreConnector.class);

    private IdentityConnectorConfig identityConnectorConfig;
    private String identityStoreId;
    DataSource dataSource;
    java.util.Properties properties;
    LDAPConnectionContext connectionSource;




    /*
    @Override
    public void init(String s, IdentityStoreConfig identityStoreConfig) throws IdentityStoreException {

    }
    */

    @Override
    public void init(String storeId, IdentityConnectorConfig identityConnectorConfig) throws IdentityStoreException {


        if (log.isDebugEnabled()) {
            log.debug("Initialization Started " + System.currentTimeMillis());
        }



        this.identityConnectorConfig = identityConnectorConfig;
        this.identityStoreId = identityStoreId;
        properties = identityConnectorConfig.getStoreProperties();
        LDAPConnectionContext context;

        Hashtable env = new Hashtable(11);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://localhost:389/dc=wso2,dc=com");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "cn=admin,dc=wso2,dc=com");
        env.put(Context.SECURITY_CREDENTIALS, "admin");


        try {
            // Create initial context
            LdapContext ctx = new InitialLdapContext(env, null);
            System.out.println("Connection established");
            // Start TLS

            StartTlsResponse tls =
                    (StartTlsResponse) ctx.extendedOperation(new StartTlsRequest());


            tls.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            tls.negotiate();


            // Stop TLS
            tls.close();

            // Close the context when we're done


        }
        catch (NamingException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public String getIdentityStoreId() {

        return identityStoreId;
    }

    @Override
    public User.UserBuilder getUserFromId(String s) throws IdentityStoreException {
        return null;
    }

    @Override
    public User.UserBuilder getUser(String s) throws UserNotFoundException, IdentityStoreException {
        return null;
    }

    public User.UserBuilder getUserFromId(String s, DirContext ctx) throws IdentityStoreException, IOException, NamingException {

        List<User.UserBuilder> userList=new ArrayList<>();

        String  searchFilter = "(&(objectClass=user)(uid =" + s + ")";
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration results=ctx.search(USER_SEARCH_BASE,searchFilter,searchControls);
        SearchResult searchResult = null;
        if(results.hasMoreElements()){
            searchResult=(SearchResult) results.nextElement();

        }
        return (User.UserBuilder) userList;
    }

    public User.UserBuilder getUser_Copy(String s, DirContext ctx)
            throws UserNotFoundException, IdentityStoreException, IOException {

        return null;
    }

    @Override
    public User.UserBuilder getUser(Callback[] callbacks) throws UserNotFoundException, IdentityStoreException {

        for (Callback callback : callbacks)  {
            if (callback instanceof NameCallback) {
                String username = ((NameCallback) callback).getName();
                return this.getUser(username);

            }
        }

        throw new IdentityStoreException("No name callback present in the callback array.");
    }

    @Override
    public int getUserCount() throws IdentityStoreException {
        return 0;
    }

    public int getUserCount_Copy(DirContext ctx, Name ldapSearchBase, String accountName)
            throws IdentityStoreException, NamingException {
        int count = 0;

        String searchFilter = "(&(objectClass=user))";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        NamingEnumeration<SearchResult> results = ctx.search(ldapSearchBase, searchFilter, searchControls);
        SearchResult searchResult = null;
        if (results.hasMoreElements()) {
            searchResult = (SearchResult) results.nextElement();
            count++;

            //make sure there is not another item available, there should be only 1 match
            if (results.hasMoreElements()) {
                return 0;
            }
        }

        return count;

    }

    @Override
    public List<User.UserBuilder> listUsers(String s, int i, int i1) throws IdentityStoreException {

        return null;
    }


    public List<User.UserBuilder> listUsers_Copy(String s, int i, int i1, DirContext ctx)
            throws IdentityStoreException {

        List<User.UserBuilder> userList = new ArrayList<>();


        String searchFilter = "(&(objectClass=user)" + s + ")";

        try {
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration<SearchResult> results = ctx.search(s, searchFilter, searchControls);


            SearchResult searchResult = null;

            if (results.hasMoreElements()) {
                searchResult = (SearchResult) results.nextElement();

                if (results.hasMoreElements()) {
                    System.err.println("Multched multiple users with the same searchControl");
                }
            }

            return (List<User.UserBuilder>) searchResult;


        } catch (NamingException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Map<String, String> getUserAttributeValues(String userName) throws IdentityStoreException {


        String userAttributeSeparator = ",";
        String userDN = null;
        String[] propertyNames = new String[0];


        Map<String, String> values = new HashMap<String, String>();
        // if user name contains domain name, remove domain name
        String[] userNames = userName.split(LDAPConstants.DOMAIN_SEPARATOR);
        if (userNames.length > 1) {
            userName = userNames[1];
        }

        DirContext dirContext = null;
        try {
            dirContext = connectionSource.getContext();
        } catch (CredentialStoreException e) {
            e.printStackTrace();
        }
        String  searchFilter = "(&(objectClass=user)(uid =" + userName + ")";;


        NamingEnumeration<?> answer = null;
        NamingEnumeration<?> attrs = null;
        try {
            if (userDN != null) {
                SearchControls searchCtls = new SearchControls();
                searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                if (propertyNames != null && propertyNames.length > 0) {
                    searchCtls.setReturningAttributes(propertyNames);
                }
                if (log.isDebugEnabled()) {
                    try {
                        log.debug("Searching for user with SearchFilter: " + searchFilter + " in SearchBase: " + dirContext.getNameInNamespace());
                    } catch (NamingException e) {
                        log.debug("Error while getting DN of search base", e);
                    }
                    if (propertyNames == null) {
                        log.debug("No attributes requested");
                    } else {
                        for (String attribute : propertyNames) {
                            log.debug("Requesting attribute :" + attribute);
                        }
                    }
                }
                try {
                    answer = dirContext.search(escapeDNForSearch(userDN), searchFilter, searchCtls);
                } catch (PartialResultException e) {
                    // can be due to referrals in AD. so just ignore error
                    String errorMessage = "Error occurred while searching directory context for user : " + userDN + " searchFilter : " + searchFilter;

                } catch (NamingException e) {
                    String errorMessage = "Error occurred while searching directory context for user : " + userDN + " searchFilter : " + searchFilter;
                    if (log.isDebugEnabled()) {
                        log.debug(errorMessage, e);
                    }

                }
            } else {
                answer = this.searchForUser(searchFilter, propertyNames, dirContext);
            }
            while (answer.hasMoreElements()) {
                SearchResult sr = (SearchResult) answer.next();
                Attributes attributes = sr.getAttributes();
                if (attributes != null) {
                    for (String name : propertyNames) {
                        if (name != null) {
                            Attribute attribute = attributes.get(name);
                            if (attribute != null) {
                                StringBuffer attrBuffer = new StringBuffer();
                                for (attrs = attribute.getAll(); attrs.hasMore(); ) {
                                    Object attObject = attrs.next();
                                    String attr = null;
                                    if (attObject instanceof String) {
                                        attr = (String) attObject;
                                    }


                                    String value = attrBuffer.toString();

                                /*
                                 * Length needs to be more than userAttributeSeparator.length() for a valid
                                 * attribute, since we
                                 * attach userAttributeSeparator
                                 */
                                    if (value != null && value.trim().length() > userAttributeSeparator.length()) {
                                        value = value.substring(0, value.length() - userAttributeSeparator.length());
                                        values.put(name, value);
                                    }

                                }
                            }
                        }
                    }
                }
            }

        } catch (NamingException e) {
            String errorMessage = "Error occurred while getting user property values for user : " + userName;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new IdentityStoreException(errorMessage, e);
        }

        return values;


    }

    @Override
    public Map<String, String> getUserAttributeValues(String s, List<String> list) throws IdentityStoreException {
        return null;
    }

    @Override
    public Group.GroupBuilder getGroupById(String s) throws IdentityStoreException {
        return null;
    }


    public Group.GroupBuilder getGroupById(String s,DirContext ctx) throws IdentityStoreException, NamingException {

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String searchFilter = "(objectClass=group)";

        NamingEnumeration results = null;
        results = ctx.search(ConnectorConstants.PROVIDER_PATH, "(objectClass =" + s + ")", controls);

        List<Attributes> listGroupByID = null;
        while (results.hasMore()) {

            SearchResult result = (SearchResult) results.next();
            Attributes att = result.getAttributes();


            listGroupByID = new ArrayList<Attributes>();
            listGroupByID.add(att);
        }

        return null;
    }

    @Override
    public Group.GroupBuilder getGroup(String s) throws GroupNotFoundException, IdentityStoreException {

        SearchControls controls = new SearchControls();

        return null;
    }

    @Override
    public int getGroupCount() throws IdentityStoreException {
        return 0;
    }

    public Group.GroupBuilder getGroup(String s,DirContext ctx) throws GroupNotFoundException, IdentityStoreException {

        NamingEnumeration results = null;
        try {

            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String searchFilter= "(&(objectClass=user)(objectClass=group))";

            results = ctx.search("ou=path,dc=*,dc=*", "(objectClass=" + s + ")", controls);

            // Go through each item in list
            while (results.hasMore()) {
                SearchResult nc = (SearchResult) results.next();
                Attributes att = nc.getAttributes();
                String groupName = "Group Name " + att.get("cn").get(0);

            }
        } catch (NameNotFoundException e) {
            System.out.println("Error : " + e);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        return null;

    }


    public int getGroupCount(DirContext ctx) throws IdentityStoreException {

        int count = 0;
        NamingEnumeration results = null;
        try{
            SearchControls controls = new SearchControls();
            controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String searchFilter="(&(objectClass = user)(objectClass = group))";
            results = ctx.search("ou=path,dc=*,dc=*", searchFilter,controls);

            while(results.hasMore()){
                SearchResult res = (SearchResult) results.next();
                Attributes attr = res.getAttributes();
                count ++;
            }
        }

        catch (NamingException e)

        {
            e.printStackTrace();
        }
        return count;
    }

    @Override
    public List<Group.GroupBuilder> listGroups(String s, int i, int i1) throws IdentityStoreException {

        DirContext ctx = null;
        List<Group> groupList = new ArrayList<>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String searchFilter = "(objectClass=group)";

        try {
            NamingEnumeration results = ctx.search("ou=path,dc=*,dc=*",searchFilter,controls);

            while(results.hasMore()){
                SearchResult res = (SearchResult) results.next();
                Attributes attr = res.getAttributes();
                groupList.add((Group) attr);

            }

        } catch (NamingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override

    public Map<String, String> getGroupAttributeValues(String s) throws IdentityStoreException {

    return null;
    }



    public Map<String, String> getGroupAttributeValues(String s,DirContext ctx) throws IdentityStoreException, NamingException {

        //Create the search controls
        SearchControls userSearchCtls = new SearchControls();

        userSearchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);

        //specify the LDAP search filter to find the user in question
        String userSearchFilter = "(objectClass=group ";

        //paceholder for an LDAP filter that will store SIDs of the groups the user belongs to
        StringBuffer groupsSearchFilter = new StringBuffer();
        groupsSearchFilter.append("(|");
        //Specify the Base for the search
        String userSearchBase = s;

        String userReturnedAtts[] =
                {"tokenGroups"
                };
        userSearchCtls.setReturningAttributes(userReturnedAtts);
        NamingEnumeration userAnswer = ctx.search(userSearchBase, userSearchFilter, userSearchCtls);

        //Loop through the search results
        while (userAnswer.hasMoreElements()) {

            SearchResult sr = (SearchResult)userAnswer.next();
            Attributes attrs = sr.getAttributes();

            if (attrs != null) {

                try {
                    for (NamingEnumeration ae = attrs.getAll();ae.hasMore();) {
                        Attribute attr = (Attribute)ae.next();
                        for (NamingEnumeration e = attr.getAll();e.hasMore();) {

                            byte[] sid = (byte[])e.next();
                            groupsSearchFilter.append("(objectSid=" + sid + ")");

                        }
                        groupsSearchFilter.append(")");
                    }

                }
                catch (NamingException e) {
                    System.err.println("Problem listing membership: " + e);
                }
            }
        }


        // Search for groups the user belongs to in order to get their names
        //Create the search controls

        return null;
    }

    protected NamingEnumeration<SearchResult> searchForUser(String searchFilter,
                                                            String[] returnedAtts,
                                                            DirContext dirContext)
            throws IdentityStoreException {
        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String searchBases = realmConfig.getUserStoreProperty(ConnectorConstants.USER_SEARCH_BASE);
        if (returnedAtts != null && returnedAtts.length > 0) {
            searchCtls.setReturningAttributes(returnedAtts);
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Searching for user with SearchFilter: " + searchFilter + " in SearchBase: " + dirContext.getNameInNamespace());
            } catch (NamingException e) {
                log.debug("Error while getting DN of search base", e);
            }
            if (returnedAtts == null) {
                log.debug("No attributes requested");
            } else {
                for (String attribute : returnedAtts) {
                    log.debug("Requesting attribute :" + attribute);
                }
            }
        }

        String[] searchBaseAraay = searchBases.split("#");
        NamingEnumeration<SearchResult> answer = null;

        try {
            for (String searchBase : searchBaseAraay) {
                answer = dirContext.search(escapeDNForSearch(searchBase), searchFilter, searchCtls);
                if (answer.hasMore()) {
                    return answer;
                }
            }
        } catch (PartialResultException e) {
            // can be due to referrals in AD. so just ignore error
            String errorMessage ="Error occurred while search user for filter : " + searchFilter;

        } catch (NamingException e) {
            String errorMessage ="Error occurred while search user for filter : " + searchFilter;
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new IdentityStoreException(errorMessage, e);
        }
        return answer;
    }


    @Override
    public Map<String, String> getGroupAttributeValues(String s, List<String> list) throws IdentityStoreException {

 return null;

    }

    @Override
    public List<Group.GroupBuilder> getGroupsOfUser(String s) throws IdentityStoreException {
        return null;
    }

    @Override
    public List<User.UserBuilder> getUsersOfGroup(String s) throws IdentityStoreException {

        return null;
    }

    @Override
    public boolean isUserInGroup(String s, String s1) throws IdentityStoreException {
        return false;
    }

    @Override
    public boolean isReadOnly() throws IdentityStoreException {
        return true;
    }

    @Override
    public IdentityConnectorConfig getIdentityStoreConfig()
    {
        return identityConnectorConfig;
    }

    private int getMaxRowRetrievalCount() {

        int length;

        String maxValue = identityConnectorConfig.getStoreProperties().getProperty(ConnectorConstants.MAX_ROW_LIMIT);

        if (maxValue == null) {
            length = Integer.MAX_VALUE;
        } else {
            length = Integer.parseInt(maxValue);
        }

        return length;
    }



    private String escapeDNForSearch(String dn){
        boolean replaceEscapeCharacters = true;

        String replaceEscapeCharactersAtUserLoginString = realmConfig
                .getUserStoreProperty(UserCoreConstants.RealmConfig.PROPERTY_REPLACE_ESCAPE_CHARACTERS_AT_USER_LOGIN);

        if (replaceEscapeCharactersAtUserLoginString != null) {
            replaceEscapeCharacters = Boolean
                    .parseBoolean(replaceEscapeCharactersAtUserLoginString);
            if (log.isDebugEnabled()) {
                log.debug("Replace escape characters configured to: "
                        + replaceEscapeCharactersAtUserLoginString);
            }
        }
        if (replaceEscapeCharacters) {
            return dn.replace("\\\\", "\\\\\\").replace("\\\"", "\\\\\"");
        } else {
            return dn;
        }
    }
}


