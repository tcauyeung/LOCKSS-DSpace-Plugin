1. Add the plugin code to /[dspace source code
folder]/dspace-oai/dspace-oai-api/src/main/java/org/dspace/app/oai/
2. Build the source
3. Update via Ant or whatever the current approach the site uses
4. Add to oaicat.properties (found in /[dspace application folder]/config
this line to the section on supported metdataPrefixes:

Crosswalks.oai_lockss=org.dspace.app.oai.LOCKSS2Crosswalk

5. Restart Tomcat/Java application container