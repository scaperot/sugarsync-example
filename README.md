sugarsync-example
====================

Sugar Sync SampleTool modification to access received shared folder (or for the layman Shared Folders)

I wanted to extend the existing sample tool (found at sugarsync.com/developer) to interact with SugarSync Shared Folders instead of the MagicBriefcase because...well...I don't use the MagicBriefcase.

Installation: 
If you go to the developer resources at sugarsync.com/developer under Examples download sugarsync-api-sample.zip.  If you follow all the instructions on the webpage for installation with the addition that you replace SampleTool.java from this repo with the default one provided.  The pom.xml and everything should work for creating binaries, etc.

Usage:
The usage is the same as the instructions on sugarsync.com/developer however at the moment the shared folder is hardcoded into the java classes (working on changing that).

TODO: 
strip out the MagicBriefcase from Upload feature
add a required argument to specify the received shared folder.
