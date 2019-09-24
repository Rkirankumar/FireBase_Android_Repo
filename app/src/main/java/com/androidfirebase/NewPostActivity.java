package com.androidfirebase;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NewPostActivity extends BaseActivity {
    private DatabaseReference mDatabase;
    private EditText mTitleField, mBodyField;
    private FloatingActionButton mSubmitButton;


    //Variables
    private Button ChooseButton;
    private ImageView SelectImage;
    // Creating URI.
    Uri FilePathUri;

    // Creating StorageReference and DatabaseReference object.
    StorageReference storageReference;
    DatabaseReference databaseReference;

    // Image request code for onActivityResult() .
    int Image_Request_Code = 7;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);
        mTitleField = findViewById(R.id.field_title);
        mBodyField = findViewById(R.id.field_body);
        mSubmitButton = findViewById(R.id.fab_submit_post);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Assign FirebaseStorage instance to storageReference.
        storageReference = FirebaseStorage.getInstance().getReference();

          //Initialize Views
        ChooseButton = (Button) findViewById(R.id.btnChoose);
        SelectImage = (ImageView) findViewById(R.id.imgView);

        ChooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitPost();
            }
        });
    }
    private void chooseImage() {
        // Creating intent.
        Intent intent = new Intent();

        // Setting intent type as image to select image from phone storage.
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Please Select Image"), Image_Request_Code);

    }
    private boolean validateForm(String title, String body) {
        if (TextUtils.isEmpty(title)) {
            mTitleField.setError(getString(R.string.required));
            return false;
        } else if (TextUtils.isEmpty(body)) {
            mBodyField.setError(getString(R.string.required));
            return false;
        } else {
            mTitleField.setError(null);
            mBodyField.setError(null);
            return true;
        }
    }

    private void submitPost() {
        final String title = mTitleField.getText().toString().trim();
        final String body = mBodyField.getText().toString().trim();
        final String userId = getUid();

        if (validateForm(title, body)) {
            // Disable button so there are no multi-posts
            setEditingEnabled(false);
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    User user = dataSnapshot.getValue(User.class);
                    if (user == null) {
                        Toast.makeText(NewPostActivity.this, "Error: could not fetch user.", Toast.LENGTH_LONG).show();
                    } else {
                        //writeNewPost(userId, user.username, title, body);
                        UploadImageFileToFirebaseStorage(userId, user.username, title, body);
                    }
                    setEditingEnabled(true);
                    finish();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    setEditingEnabled(true);
                    Toast.makeText(NewPostActivity.this, "onCancelled: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void setEditingEnabled(boolean enabled) {
        mTitleField.setEnabled(enabled);
        mBodyField.setEnabled(enabled);
        if (enabled) {
            mSubmitButton.setVisibility(View.VISIBLE);
        } else {
            mSubmitButton.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Image_Request_Code && resultCode == RESULT_OK && data != null && data.getData() != null) {

            FilePathUri = data.getData();

            try {

                // Getting selected image into Bitmap.
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), FilePathUri);

                // Setting up bitmap selected image into ImageView.
                SelectImage.setImageBitmap(bitmap);

                // After selecting image change choose button above text.
                ChooseButton.setText("Image Selected");

            }
            catch (IOException e) {

                e.printStackTrace();
            }
        }
    }
    // Creating Method to get the selected image file Extension from File Path URI.
    public String GetFileExtension(Uri uri) {

        ContentResolver contentResolver = getContentResolver();

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();

        // Returning the file Extension.
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri)) ;

    }
    // Creating UploadImageFileToFirebaseStorage method to upload image on storage.
    public void  UploadImageFileToFirebaseStorage(final String userId,final String username,final String title,final String body) {

        // Checking whether FilePathUri Is empty or not.
        if (FilePathUri != null) {


            // Creating second StorageReference.
            StorageReference storageReference2nd = storageReference.child(GetFileExtension(FilePathUri));

            // Adding addOnSuccessListener to second StorageReference.
            storageReference2nd.putFile(FilePathUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            // Getting image name from EditText and store into string variable.
                            //  String TempImageName = ImageName.getText().toString().trim();
     // Showing toast message after done uploading.
                            Toast.makeText(getApplicationContext(), "Image Uploaded Successfully ", Toast.LENGTH_LONG).show();
                            // Create new post at /user-posts/$userid/$postid
                            // and at /posts/$postid simultaneously
                            String key = mDatabase.child("posts").push().getKey();
                            String userImage;
                            if (getUserImage() != null) {
                                userImage=getUserImage().toString();
                            }else {
                                userImage="";
                            }
                            Task<Uri> downloadUrl = taskSnapshot.getMetadata().getReference().getDownloadUrl();

                            Post post = new Post(userId, username, title, body,downloadUrl.getResult().toString());
                            Map<String, Object> postValues = post.toMap();

                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/posts/" + key, postValues);
                            childUpdates.put("/user-posts/" + userId + "/" + key, postValues);

                            mDatabase.updateChildren(childUpdates);

                        }
                    })
                    // If something goes wrong .
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {

                            // Showing exception erro message.
                            Toast.makeText(NewPostActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })

                    // On progress change upload time.
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                           }
                    });
        }
        else {

            Toast.makeText(NewPostActivity.this, "Please Select Image or Add Image Name", Toast.LENGTH_LONG).show();

        }
    }
}