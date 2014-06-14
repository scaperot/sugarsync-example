sugarsync-example
====================

Sugar Sync SampleTool modification to access received shared folder (or for the layman Shared Folders)

I wanted to extend the existing tool to troll through receivedShared folders and then paruse them for a specific folder name (added the input argument 'media' to for a folder type.  This basically still has all the same code as the example on sugarsync.com.

Installation: 
If you go to the developer resources at sugarsync.com/developer under Examples download sugarsync-api-sample.zip.  If you follow all the instructions on the webpage for installation with the addition that you add my SampleTool.java instead of the one provided.  The pom.xml and everything should work.

Usage:
The usage is the same as the instructions on sugarsync.com/developer however at the moment the shared folder is hardcoded into the java classes (working on changing that).

TODO: 
strip out the MagicBriefcase from Upload feature
add a required field for the received shared folder.
