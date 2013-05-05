package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;


import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;


import com.google.api.client.http.FileContent;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import edu.mit.media.funf.storage.RemoteFileArchive;

public class GoogleDriveArchive implements RemoteFileArchive {
  private static final String TAG = "GoogleDriveArchive";
  private static Drive service;
  private GoogleAccountCredential credential;
  
  public static final String
  PREFS_GOOGLEDRIVE = "googleDrivePrefs",
  PREF_GOOGLE_ACCOUNT = "__GOOGLE_ACCOUNT_KEY__",
  PREF_GOOGLE_UPLOAD_FOLDER = "GoogleDrivePath", //default folder path on Google Drive
  PREF_GOOGLE_UPLOAD_DB_NAME = "gdUploadDBName", //default key for getting db name
  GOOGLE_UPLOAD_DB_DEFAULT = "SensorData"; //default name for sensor db
  
  // just a fake id for implmenting the interface
  public static final String GOOGLEDRIVE_ID = "googledrive://appinventor/__ID__"; 

  
  private Context mContext;
  private Drive mService;
  private String mAccount;
  private com.google.api.services.drive.model.File gdFolder;
 
  private final SharedPreferences sharedPreferences;

  private String googleDriveFolderName;


  public GoogleDriveArchive(Context context, String GoogleDriveFolderName) {
    
    this.mContext = context;
    sharedPreferences = context.getSharedPreferences(GoogleDrive.PREFS_GOOGLEDRIVE, context.MODE_PRIVATE);
    this.mAccount = sharedPreferences.getString(GoogleDrive.PREF_ACCOUNT_NAME, "");

    this.googleDriveFolderName = GoogleDriveFolderName;
    this.gdFolder = getGoogleDriveFolder();
    
  }
  
  private com.google.api.services.drive.model.File getGoogleDriveFolder(){

    com.google.api.services.drive.model.File folder = null;
    
    if(this.googleDriveFolderName == GoogleDrive.DEFAULT_GD_FOLDER){
      try {
        folder = mService.files().get("root").execute();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      return folder;
    }
    
    String query = "mimeType = 'application/vnd.google-apps.folder' and title = '" 
      + googleDriveFolderName + "' and 'root' in parents" ;
    
    FileList files = ExcuteQuery(query);
    
    if (files == null)
      folder = createGoogleFolder();
      //File file : files.getItems()
    else {
      folder =  files.getItems().get(0);//if the folder exists, it should be the only one
    }
    return folder;
    
  }
  
  @Override
  public boolean add(File file) throws Exception {
    // TODO Auto-generated method stub
    
    return uploadDataFile(mContext, file);
 
  }

  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  private FileList ExcuteQuery(String query){
  	//this is to search a folder name that is passed in by the caller
  	//https://developers.google.com/drive/search-parameters
  	FileList files = null;
      try {
      	
      	files =  mService.files().list().setQ(query).execute();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
      return files;
  }

  private boolean uploadSingleFile(Context context, File file) throws Exception {
    //this method actually does the uploading, only successful upload will return true, else will be exception throws to
    //the caller methods
    String dataPath = file.getAbsolutePath();

    //Need to get Google Drive folder information if an app specifies the folder to upload to
    // 1. get or create folder if not exist\

 
    try {
    	
//    	processGDFile(Drive service, String parentId, File localFile)
    	processGDFile(mService, gdFolder.getId(), file);
 
      Log.i(TAG, "Return From Google Drive Drive sending the file...");
   
      return true;
      
    } catch (FileNotFoundException e) {
      Log.w(TAG, "File not found: " + dataPath);
      throw e;
    } 

    
  }
  
  private com.google.api.services.drive.model.File createGoogleFolder()  {
  	
  	//go to the root, and create a folder and return the handle
  	com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
    body.setTitle(this.googleDriveFolderName);
    body.setMimeType("application/vnd.google-apps.folder");
    body.setParents(Arrays.asList(new ParentReference().setId("root")));
    Log.i(TAG, "We have created a Google Drive Folder with name" + this.googleDriveFolderName);
    
    com.google.api.services.drive.model.File file = null;
    try {
      file = service.files().insert(body).execute();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return file;
 
	}
  
  
  private com.google.api.services.drive.model.File processGDFile(Drive service,
      String parentId, File localFile) throws IOException {

    boolean existed = false;
    com.google.api.services.drive.model.File processedFile;
    //Determine whether the file exists in this folder or not.
    String q = "'" + parentId + "' in parents and" + "title = " + "'"
        + localFile.getName() + "'";

    FileContent mediaContent = new FileContent("", localFile);

    com.google.api.services.drive.model.File gdFile = ExcuteQuery(q).getItems()
        .get(0);
    // File's content.
    try {

      if (gdFile != null) { // if this file already exists in GD
        existed = true;

        processedFile = service.files()
            .update(gdFile.getId(), gdFile, mediaContent).execute();
        // Uncomment the following line to print the File ID.
        Log.i(TAG, "Processed File ID: %s" + processedFile.getId());

      } else {
        // Start the new File's metadata.
        com.google.api.services.drive.model.File body = new com.google.api.services.drive.model.File();
        body.setTitle(localFile.getName());

        // Set the parent folder.
        if (parentId != null && parentId.length() > 0) {
          body.setParents(Arrays.asList(new ParentReference().setId(parentId)));
        }
        processedFile = service.files().insert(body, mediaContent).execute();
        // Uncomment the following line to print the File ID.
        Log.i(TAG, "Processed File ID: %s" + processedFile.getId());

      }
    } catch (IOException e) {
      throw e;
    }
    return processedFile;

  }

	/**
   * Retrieve a authorized service object to send requests to the Google Drive
   * API. On failure to retrieve an access token, a notification is sent to the
   * user requesting that authorization be granted for the
   * {@code https://www.googleapis.com/auth/drive.file} scope.
   * 
   * @return An authorized service object.
   * @throws Exception 
   */
  private Drive getDriveService() throws Exception {
    if (mService == null) {
      try {
        GoogleAccountCredential credential = 
        		GoogleAccountCredential.usingOAuth2(mContext, DriveScopes.DRIVE_FILE);
        credential.setSelectedAccountName(mAccount);
        // Trying to get a token right away to see if we are authorized
        credential.getToken();
        mService = new Drive.Builder(AndroidHttp.newCompatibleTransport(),
        		new GsonFactory(), credential).build();
			} catch (Exception e) {
				Log.e(TAG, "Failed to get token");
				if (e instanceof UserRecoverableAuthException) {
				  // https://developers.google.com/drive/examples/android#authorizing_your_app
					// This should not happen in our case. however, if it happens we will propagate the 
				  // error to UploadService and then to the component
					throw e;
 
				} else {
					e.printStackTrace();
				}
			}
    }
    return mService;
  }
  
  private boolean uploadFolderFiles(Context context, File file) throws Exception {
    // In case when the specified File path is a directory
    // Note that we don't do nested looping through a folder to get all the folders and files

    if(file.isDirectory()){
       File[] listOfFiles = file.listFiles();
       for (File f: listOfFiles) {
         if (uploadSingleFile(context, f))
           ; //if successful, do nothing, else return false
         else
           return false;
       }

    }//only return true if all files in the folder has been succesfully uploaded
    return true;  

  }
  
  public boolean uploadDataFile(Context context, File file) throws Exception {

    mService = getDriveService(); //first init the Drive service
    
    if (file.isDirectory()){
      return uploadFolderFiles(context, file);
      
    }else{
      return uploadSingleFile(context, file);
      
    }
    
   
  }
  
  

}
