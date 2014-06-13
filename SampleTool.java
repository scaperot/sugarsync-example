package com.sugarsync.sample.tool;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import com.sugarsync.sample.auth.AccessToken;
import com.sugarsync.sample.auth.RefreshToken;
import com.sugarsync.sample.file.FileCreation;
import com.sugarsync.sample.file.FileDownloadAPI;
import com.sugarsync.sample.file.FileUploadAPI;
import com.sugarsync.sample.userinfo.UserInfo;
import com.sugarsync.sample.util.HttpResponse;
import com.sugarsync.sample.util.SugarSyncHTTPGetUtil;
import com.sugarsync.sample.util.XmlUtil;

/**
 * @file SampleTool.java
 * 
 *       The main class for the sample api tool. The tool has the next features:
 * 
 *       - displays quota information for a developer
 * 
 *       - lists the "Magic Briefcase" folder contents
 * 
 *       - downloads a remote file from the "Magic Briefcase" folder
 * 
 *       - uploads a local file to the "Magic Briefcase" folder
 */
public class SampleTool {
    static{
        //set the logging level to error
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "error");
    }
    
    // used for printing user QUOTA
    private static final Double ONE_GB = 1024.0 * 1024 * 1024;

    // tool parameters and commands
    private static final String userParam = "-user";
    private static final String passParam = "-password";
    private static final String applicationIdParam = "-application";
    private static final String accesskeyParam = "-accesskey";
    private static final String privateaccesskeyParam = "-privatekey";
    

    private static final String quotaCmd = "quota";
    private static final String listCmd = "list";
    private static final String uploadCmd = "upload";
    private static final String downloadCmd = "download";

    /**
     * Returns the value of the parameter value specified as argument
     * 
     * @param param
     *            the parameter for which the value is requested
     * @param argumentList
     *            the arguments passed to main method
     * @return the value of the input parameter
     */
    private static String getParam(String param, List<String> argumentList) {
        int indexOfParam = argumentList.indexOf(param);
        if (indexOfParam == -1) {
            System.out.println("Parameter " + param + " not specified!!!");
            printUsage();
            System.exit(0);
        }
        return argumentList.get(indexOfParam + 1);
    }

    /**
     * Returns the command for the tool
     * 
     * @param argumentList
     *            the arguments passed to main method
     * @return the command which will be run by the tool
     */
    private static String getCommand(List<String> argumentList) {
        String cmd = argumentList.get(argumentList.size() - 1);
        if (Arrays.asList(quotaCmd, listCmd).contains(cmd)) {
            return cmd;
        } else
            return argumentList.get(argumentList.size() - 2);
    }

    // --- SugarSync API calls
    /**
     * Returns a refresh token for the developer with the credentials specified
     * in the method parameters and the application id
     * 
     * @param username
     *            SugarSync username (email address)
     * @param password
     *            SugarSync password
     * @param applicationId
     *            The developer application id
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @return refresh token
     * @throws IOException
     *             if any I/O error if thrown
     */
    private static String getRefreshToken(String username, String password, String applicationId, String accessKey,
            String privateAccessKey) throws IOException {
        HttpResponse httpResponse = null;
        httpResponse = RefreshToken.getAuthorizationResponse(username, password,applicationId, accessKey, privateAccessKey);

        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("Error while getting refresh token!");
            printResponse(httpResponse);
            System.exit(0);
        }

        return httpResponse.getHeader("Location").getValue();
    }
    
    /**
     * Return an access token for the developer keys and a refresh token
     * 
     * @param accessKey
     *            Developer accessKey
     * @param privateAccessKey
     *            Developer privateAccessKey
     * @param refreshToken
     *            Refresh token string returned ass a response from
     *            app-authorization request
     * @return the access token that will be used for all API requests
     * @throws IOException
     *             if any I/O error if thrown
     */
    private static String getAccessToken(String accessKey, String privateAccessKey, String refreshToken) throws IOException {
        HttpResponse httpResponse = AccessToken.getAccessTokenResponse(accessKey, privateAccessKey,
                refreshToken);

        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("Error while getting access token!");
            printResponse(httpResponse);
            System.exit(0);
        }

        return httpResponse.getHeader("Location").getValue();
    }

    /**
     * Returns the account information
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getUserInfo(String accessToken) throws IOException {
        HttpResponse httpResponse = UserInfo.getUserInfo(accessToken);
        validateHttpResponse(httpResponse);
        return httpResponse;
    }

    /**
     * Returns the "Magic Briefcase" SugarSync default folder contents
     * 
     * 1.Get the "Magic Briefcase" folder representation
     * 
     * 2.Extract the folder contents link from the folder representation: parse
     * the xml file and retrieve the <contents> node value
     * 
     * 3.Make a HTTP GET to the previous extracted link
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getMagicBriefcaseFolderContents(String accessToken)
            throws IOException, XPathExpressionException {
        HttpResponse folderRepresentationResponse = getMagicBriefcaseFolderRepresentation(accessToken);
        validateHttpResponse(folderRepresentationResponse);

        String magicBriefcaseFolderContentsLink = XmlUtil.getNodeValues(
                folderRepresentationResponse.getResponseBody(), "/folder/contents/text()").get(0);
        HttpResponse folderContentsResponse = SugarSyncHTTPGetUtil.getRequest(
                magicBriefcaseFolderContentsLink, accessToken);
        validateHttpResponse(folderContentsResponse);

        return folderContentsResponse;
    }

    /**
     * Returns the "Magic Briefcase" SugarSync default folder representation.
     * 
     * 1. Make a HTTP GET call to https://api.sugarsync.com/user for the user
     * information
     * 
     * 2. Extract <magicBriefcase> node value from the xml response
     * 
     * 3. Make a HTTP GET to the previous extracted link
     * 
     * @param accessToken
     *            the access token
     * @return a HttpResponse containing the server's xml response in the
     *         response body
     * @throws IOException
     *             if any I/O error occurs
     */
    private static HttpResponse getMagicBriefcaseFolderRepresentation(String accessToken)
            throws IOException, XPathExpressionException {
        HttpResponse userInfoResponse = getUserInfo(accessToken);

        // get the magicBriefcase folder representation link
        String magicBriefcaseFolderLink = XmlUtil.getNodeValues(userInfoResponse.getResponseBody(),
                "/user/magicBriefcase/text()").get(0);

        // make a HTTP GET to the link extracted from user info
        HttpResponse folderRepresentationResponse = SugarSyncHTTPGetUtil.getRequest(magicBriefcaseFolderLink,
                accessToken);
        validateHttpResponse(folderRepresentationResponse);

        return folderRepresentationResponse;
    }

    // --- End SugarSync API calls

    // --- tool commands method
    /**
     * Handles "quota" tool command. Makes a HTTP GET call to
     * https://api.sugarsync.com/user and displays the quota information from
     * the server xml response.
     * 
     * @param accessToken
     *            the access token
     * @throws IOException
     *             if any I/O error occurs
     * @throws XPathExpressionException
     */
    private static void handleQuotaCommand(String accessToken) throws IOException,
            XPathExpressionException {
        HttpResponse httpResponse = getUserInfo(accessToken);

        // read the <quota> node values
        String limit = XmlUtil.getNodeValues(httpResponse.getResponseBody(), "/user/quota/limit/text()").get(
                0);
        String usage = XmlUtil.getNodeValues(httpResponse.getResponseBody(), "/user/quota/usage/text()").get(
                0);

        DecimalFormat threeDForm = new DecimalFormat("#.###");
        // print quota info
        String storageAvailableInGB = threeDForm.format(Double.valueOf(limit) / ONE_GB);
        String storageUsageInGB = threeDForm.format(Double.valueOf(usage) / ONE_GB);
        String freeStorageInGB = threeDForm.format(((Double.valueOf(limit) - Double.valueOf(usage)) / ONE_GB));
        System.out.println("\n---QUOTA INFO---");
        System.out.println("Total storage available: " + storageAvailableInGB + " GB");
        System.out.println("Storage usage: " + storageUsageInGB + " GB");
        System.out.println("Free storage: " + freeStorageInGB + " GB");
    }

    /**
     * Handles "list" tool command. Makes a HTTP GET request to the
     * "Magic Briefcase" contents link and displays the file and folder names
     * within the folder
     * 
     * @param accessToken
     *            the access token
     * @throws IOException
     * @throws XPathExpressionException
     * @throws TransformerException
     */
    private static void handleListCommand(String accessToken) throws IOException,
            XPathExpressionException, TransformerException {
        HttpResponse folderContentsResponse = getMagicBriefcaseFolderContents(accessToken);

        printFolderContents(folderContentsResponse.getResponseBody());
    }

    /**
     * Prints the files and folders from the xml response
     * 
     * @param responseBody
     *            the xml server response
     */
    private static void printFolderContents(String responseBody) {
        try {
            List<String> folderNames = XmlUtil.getNodeValues(responseBody,
                    "/collectionContents/collection[@type=\"folder\"]/displayName/text()");
            List<String> fileNames = XmlUtil.getNodeValues(responseBody,
                    "/collectionContents/file/displayName/text()");
            System.out.println("\n-Magic Briefcase");
            System.out.println("\t-Folders:");
            for (String folder : folderNames) {
                System.out.println("\t\t" + folder);
            }
            System.out.println("\t-Files:");
            for (String file : fileNames) {
                System.out.println("\t\t" + file);
            }
        } catch (XPathExpressionException e1) {
            System.out.println("Error while printing the folder contents:");
            System.out.println(responseBody);
        }
    }

    /**
     * Handles "download" command.
     * 
     * 1. Get the "Magic Briefcase" contents
     * 
     * 2. Check if the specified file exists in the "Magic Briefcase" folder and
     * retrieve its file data link
     * 
     * 3. Make a HTTP GET request to the previous extracted link and save the
     * response content to a local file
     * 
     * @param accessToken
     *            the access token
     * @param file
     *            the remote file within the "Magic Briefcase" folder
     * @throws IOException
     * @throws XPathExpressionException
     */
    private static void handleDownloadCommand(String accessToken, String file) throws IOException,
            XPathExpressionException {
        HttpResponse magicBriefcaseContents = getMagicBriefcaseFolderContents(accessToken);

        List<String> fileDataLink = XmlUtil.getNodeValues(magicBriefcaseContents.getResponseBody(),
                "collectionContents/file[displayName=\"" + file + "\"]/fileData/text()");
        if (fileDataLink.size() == 0) {
            System.out.println("\nFile " + file + " not found in Magic Briefcase folder");
            System.exit(0);
        }
        HttpResponse fileDownloadResponse = FileDownloadAPI.downloadFileData(fileDataLink.get(0), file,
                accessToken);
        validateHttpResponse(fileDownloadResponse);

        System.out.println("\nDownload completed successfully. The " + file
                + " from \"Magic Briefcase\" was downloaded to the local directory.");
    }

    /**
     * Handles "upload" tool command.
     * 
     * 1. Get the user information
     * 
     * 2. Extract the "Magic Briefcase" folder link from the user information
     * response
     * 
     * 3. Creates a file representation in "Magic Briefcase" folder
     * 
     * 4. Uploads the file data associated to the previously created file
     * representation
     * 
     * @param accessToken
     *            the access token
     * @param file
     *            the local file that will be uploaded in "Magic Briefcase"
     * @throws XPathExpressionException
     * @throws IOException
     */
    private static void handleUploadCommand(String accessToken, String file)
            throws XPathExpressionException, IOException {
        if (!(new File(file).exists())) {
            System.out.println("\nFile " + file + "  doesn not exists in the current directory");
            System.exit(0);
        }
        HttpResponse userInfoResponse = getUserInfo(accessToken);

        String magicBriefcaseFolderLink = XmlUtil.getNodeValues(userInfoResponse.getResponseBody(),
                "/user/magicBriefcase/text()").get(0);

        HttpResponse resp = FileCreation.createFile(magicBriefcaseFolderLink, file, "", accessToken);

        String fileDataUrl = resp.getHeader("Location").getValue() + "/data";
        resp = FileUploadAPI.uploadFile(fileDataUrl, file, accessToken);

        System.out.println("\nUpload completed successfully. Check \"Magic Briefcase\" remote folder");

    }

    // ---Print and validation
    /**
     * Validates the input arguments
     * 
     * @param args
     *            the arguments passed to main method
     */
    private static void validateArgs(List<String> args) {
        if (args.size() != 11 && args.size() != 12) {
            printUsage();
            System.exit(0);
        }
    }

    /**
     * Validates the HTTP response. If HTTP response status code indicates an
     * error the details are printed and the tool exists
     * 
     * @param httpResponse
     *            the HTTP response which will be validated
     */
    private static void validateHttpResponse(HttpResponse httpResponse) {
        if (httpResponse.getHttpStatusCode() > 299) {
            System.out.println("HTTP ERROR!");
            printResponse(httpResponse);
            System.exit(0);
        }
    }

    /**
     * Prints the http response
     * 
     * @param response
     *            the HTTP response
     */
    private static void printResponse(HttpResponse response) {
        System.out.println("STATUS CODE: " + response.getHttpStatusCode());
        // if the response is in xml format try to pretty format it, otherwise
        // leave it as it is
        String responseBodyString = null;
        try {
            responseBodyString = XmlUtil.formatXml(response.getResponseBody());
        } catch (Exception e) {
            responseBodyString = response.getResponseBody();
        }
        System.out.println("RESPONSE BODY:\n" + responseBodyString);
    }

    /**
     * Prints the tool usage
     */
    private static void printUsage() {
        System.out.println("USAGE:");
        System.out.println("java -jar sample-tool.jar " + userParam + " <username> " + passParam
                + " <password> " +applicationIdParam+" <appId> "+ accesskeyParam + " <publicAccessKey> " + privateaccesskeyParam
                + " <privateAccessKey> " + " ( " + quotaCmd + " | " + listCmd + " | " + downloadCmd
                + " <fileToDownload> | " + uploadCmd + " <fileToUpload> )");
        System.out.println("\nWHERE:");
        System.out.println("<username> - SugarSync username (email address)");
        System.out.println("<password> - SugarSync password");
        System.out.println("<appId> - The id of the app created from developer site");
        System.out.println("<publicAccessKey> - Developer accessKey");
        System.out.println("<privateAccessKey> - Developer privateAccessKey");
        System.out.println("<fileToDownload> - The file from default \"Magic Briefcase\" folder that you want to download");
        System.out.println("<fileToUpload> - The file from current directory that you want to upload into default \"Magic Briefcase\" folder ");
        

        System.out.println("\nEXAMPLES:");
        System.out.println("\nDisplaying user quota:");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " +passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + quotaCmd);

        System.out.println("\nListing \"Magic Briefcase\" folder contents:");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + listCmd);

        System.out.println("\nDownloading \"file.txt\" file  from \"Magic Briefcase\"");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + downloadCmd + " file.txt");
        System.out.println("Please not that \"file.txt\" must exists in \"Magic Briefcase\" remote folder");

        System.out.println("\nUploading \"uploadFile.txt\" file  to \"Magic Briefcase\"");
        System.out.println("java -jar sample-tool.jar " + userParam + " user@email.com " + passParam
                + " userpassword " +applicationIdParam+" /sc/10016/3_1640259 "+ accesskeyParam + " MTUzOTEyNjEzMjM4NzEwNDg0MTc " + privateaccesskeyParam
                + " ZmNhMWY2MTZlY2M1NDg4OGJmZDY4OTExMjY5OGUxOWY " + downloadCmd + " file.txt");
        System.out.println("Please not that \"uploadFile.txt\" must exists in the local directory");

    }

    public static void main(String[] args) {
        List<String> argumentList = Arrays.asList(args);

        validateArgs(argumentList);

        String username = getParam(userParam, argumentList);
        String password = getParam(passParam, argumentList);
        String applicationId = getParam(applicationIdParam, argumentList);
        String accessKey = getParam(accesskeyParam, argumentList);
        String privateAccessKey = getParam(privateaccesskeyParam, argumentList);
        

        try {
            String refreshToken = getRefreshToken(username, password, applicationId, accessKey, privateAccessKey);
            
            String accessToken = getAccessToken(accessKey,privateAccessKey,refreshToken);
            
            String command = getCommand(argumentList);

            if (command.equals(quotaCmd)) {
                handleQuotaCommand(accessToken);
            } else if (command.equals(listCmd)) {
                handleListCommand(accessToken);
            } else if (command.equals(downloadCmd)) {
                String file = argumentList.get(argumentList.size() - 1);
                handleDownloadCommand(accessToken, file);
            } else if (command.equals(uploadCmd)) {
                String file = argumentList.get(argumentList.size() - 1);
                handleUploadCommand(accessToken, file);
            } else {
                System.out.println("Uknown command: " + command);
                printUsage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

   

}
