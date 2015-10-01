package com.rittie.andy.testing;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
//Andy Rittie
    ArrayList<User> users;
    EditText nameTxt;
    EditText emailTxt;
    EditText passwordTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            String destPath = "/data/data/" + getPackageName() + "/databases/ArousalDB";
            File f = new File(destPath);
            if (!f.exists()) {
                CopyDB( getBaseContext().getAssets().open("mydb"),
                        new FileOutputStream(destPath));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        DBAdapter db = new DBAdapter(this);
        users = new ArrayList<User>();
        db.open();
        Cursor c = db.getAllUserRecords();
        int iUserID = c.getColumnIndex(DBAdapter.USER_ID);
        int iUserName = c.getColumnIndex(DBAdapter.USER_NAME);
        int iUserEmail = c.getColumnIndex(DBAdapter.USER_EMAIL);
        int iUserPassword = c.getColumnIndex(DBAdapter.USER_PASSWORD);
        int iUserAvgHR = c.getColumnIndex(DBAdapter.AVERAGE_HEART_RATE);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            users.add(new User(c.getLong(iUserID), c.getString(iUserName), c.getString(iUserEmail), c.getString(iUserPassword), c.getString(iUserAvgHR)));
        }
        db.close();

        ArrayAdapter<User> itemsAdapter =
                new ArrayAdapter<User>(this, android.R.layout.simple_list_item_1, users);
        ListView listView = (ListView) findViewById(R.id.lvUsers);
        listView.setAdapter(itemsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id){
                final User u = (User) parent.getItemAtPosition(position);
                Intent in = new Intent(MainActivity.this, UserHomeActivity.class);
                in.putExtra("user", u);
                startActivity(in);
            }
        });


    }

    public void newUser(View view) {
        nameTxt = (EditText)findViewById(R.id.editText);
        emailTxt = (EditText)findViewById(R.id.editText2);
        passwordTxt = (EditText)findViewById(R.id.editText3);

        DBAdapter db = new DBAdapter(this);

        //---add a user to Database---
        db.open();
        long id = db.insertNewUser(nameTxt.getText().toString(), emailTxt.getText().toString(), passwordTxt.getText().toString());
        db.close();
        User new_user = new User(id, nameTxt.getText().toString(),emailTxt.getText().toString(), passwordTxt.getText().toString());

        users.add(new_user);

        Intent in = new Intent(this, UserHomeActivity.class);
        in.putExtra("user", new_user);
        startActivity(in);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ArrayAdapter<User> itemsAdapter =
                new ArrayAdapter<User>(this, android.R.layout.simple_list_item_1, users);
        ListView listView = (ListView) findViewById(R.id.lvUsers);
        listView.setAdapter(itemsAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void CopyDB(InputStream inputStream, OutputStream outputStream)
            throws IOException {
        //---copy 1K bytes at a time---
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }
        inputStream.close();
        outputStream.close();
    }
}
