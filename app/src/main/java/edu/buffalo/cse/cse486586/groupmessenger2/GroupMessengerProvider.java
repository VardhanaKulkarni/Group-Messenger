package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.Selection;
import android.util.Log;
import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;


/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    static final String TAG = GroupMessengerProvider.class.getSimpleName();
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        //Android Developer site has been used as a referance
        String name = values.getAsString("key");
        String msg = values.getAsString("value");

        Log.e(name,msg+"check");
        Context context = this.getContext();
        try {
            FileOutputStream fos;
            fos = context.openFileOutput(name, context.MODE_PRIVATE);
            fos.write(msg.getBytes());
            fos.close();
            Log.v("insert", values.toString());
        }catch (IOException e) {
            Log.e(TAG, "IOException");
        }
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Context context = getContext();
        MatrixCursor cursor = new MatrixCursor(new String[]{"key","value"});
        String Path = context.getFilesDir().getAbsolutePath();
        File file = new File(Path,selection);

        try {
            BufferedReader readFile = new BufferedReader(new FileReader(file));
            String value = readFile.readLine();
            readFile.close();

            Log.e(TAG, selection);
            Log.e(TAG, value);

            cursor.addRow(new Object[]{selection, value});

            int keyIndex = cursor.getColumnIndex(selection);
            Log.v("vardhana",Integer.toString(keyIndex));
            Log.v("query", selection);
        }catch (IOException e){
            Log.e(TAG, " IOException "+ e.toString());
        }
        return cursor;
    }
}
