package cc.chenhe.weargallery.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class HideDbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "db_main";
    public static final String TABLE_HADE_NAME = "hide_folder";

    /*这里存需要隐藏的路径，结尾不加/*/
    public static final String KEY_PARENT_PATH = "parentPath";

    private static HideDbHelper instance;

    public static HideDbHelper getInstance(Context appContext) {
        if (instance == null) {
            synchronized (HideDbHelper.class) {
                if (instance == null) {
                    instance = new HideDbHelper(appContext);
                }
            }
        }
        return instance;
    }

    private HideDbHelper(Context context) {
        super(context, DB_NAME, null, 2);
    }

    private void creatHideTable(SQLiteDatabase db) {
        String sql = "create table if not exists " + TABLE_HADE_NAME + "(" + KEY_PARENT_PATH + " TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        creatHideTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1) {
            if (isTableExt("hade", db)) {
                creatHideTable(db);
                Cursor cursor = db.query("hade", null, null, null, null, null, null);
                if (cursor != null) {
                    cursor.moveToPrevious();
                    while (cursor.moveToNext()) {
                        String p = cursor.getString(cursor.getColumnIndex(KEY_PARENT_PATH));
                        ContentValues cv = new ContentValues();
                        cv.put(KEY_PARENT_PATH, p);
                        db.insert(TABLE_HADE_NAME, null, cv);
                    }
                    cursor.close();
                }
                String sql = "DROP TABLE hade";
                db.execSQL(sql);
            }
        }
    }

    /**
     * 清空数据库
     */
    public void clear() {
        if (!isTableExt(TABLE_HADE_NAME, null)) {
            return;
        }
        SQLiteDatabase database = this.getWritableDatabase();
        // 删除表
        String sql = "DROP TABLE " + TABLE_HADE_NAME;
        database.execSQL(sql);
        creatHideTable(database);
        database.close();
    }

    /**
     * 数据表是否存在
     *
     * @return
     */
    private boolean isTableExt(String tableName, final SQLiteDatabase db) {
        SQLiteDatabase database;
        if (db != null) {
            database = db;
        } else {
            database = getWritableDatabase();
        }
        String sql = "select count(*) as c from sqlite_master where type='table' and name='" + tableName + "';";
        Cursor cursor = database.rawQuery(sql, null);

        if (cursor.moveToNext()) {
            int c = cursor.getInt(0);
            cursor.close();
            if (db == null)
                database.close();
            return c > 0;
        } else {
            cursor.close();
            if (db == null)
                database.close();
            return false;
        }
    }
}
