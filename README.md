# smack-sint-adhoc-example

An example test that demonstrates an inability to set an jid-multi to an empty list

## Run tests

This is designed to be run against a locally installed Openfire. Specifically, at time of writing:

* This PR: https://github.com/igniterealtime/Openfire/pull/2381
* Which comes from this branch: https://github.com/guusdk/Openfire/tree/OF-284_Additional-commands-for-XEP-0133
* Or this commit: https://github.com/guusdk/Openfire/commit/dd92409fcf5fa8256e4975ba774cca97ee505562

Run it locally (or in Docker) using the `openfire.sh` script and the `-demoboot` option.

To run tests:

* set your hosts file to map example.org to 127.0.0.1
* check the pom.xml, that the `<configuration>` section of the `exec-maven-plugin` plugin matches your settings
* run `mvn exec:java`

This is an example Run/Debug configuration (which you can use in Intellij):

- *Main class*: `org.igniterealtime.smack.inttest.SmackIntegrationTestFramework`
- *VM options*: `-Dsinttest.service=example.org -Dsinttest.adminAccountUsername=admin -Dsinttest.adminAccountPassword=admin -Dsinttest.securityMode=disabled -Dsinttest.enabledTests=AdHocCommandIntegrationTest`
