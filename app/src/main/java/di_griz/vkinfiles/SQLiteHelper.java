package di_griz.vkinfiles;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "vkUser.db";
    private static final int DATABASE_VERSION = 1;
    static final String TABLE_NAME = "MediaBase";

    static final String COLUMN_ID = "_id";
    static final String COLUMN_TYPE = "type";
    static final String COLUMN_MESSAGE_ID = "message_id";
    static final String COLUMN_TITLE = "title";
    static final String COLUMN_URL = "url";


    SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_MESSAGE_ID + " INTEGER, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_URL + " TEXT)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
