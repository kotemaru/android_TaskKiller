package org.kotemaru.android.taskkiller.persistent;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class Database extends SQLiteOpenHelper {
    static final String DB_NAME = "TaskKiller.db";
    static final int VERSION = 100;

    public interface Column {
        public String name();

        public String type();
    }

    // テーブル定義
    private static final String PACKAGE_TABLE = "PACKAGE_TABLE";

    public enum PackageCols implements Column {
        _ID("text"), // package name
        KILL_ON_SLEEP("integer"); // 0=false, 1=true

        // --- 以下、定形 (enumは継承が出来ないので) ---
        private String mType;
        private String mWhere;

        PackageCols(String type) {
            mType = type;
            mWhere = name() + "=?";
        }

        // @formatter:off
        public String type() {return mType;}
        public String where() {return mWhere;}
        public long getLong(Cursor cursor) {return cursor.getLong(cursor.getColumnIndex(name()));}
        public int getInt(Cursor cursor) {return cursor.getInt(cursor.getColumnIndex(name()));}
        public String getString(Cursor cursor) {return cursor.getString(cursor.getColumnIndex(name()));}
        public void put(ContentValues values, long val) {values.put(name(), val);}
        public void put(ContentValues values, int val) {values.put(name(), val);}
        public void put(ContentValues values, String val) {values.put(name(), val);}
        // @formatter:on
    }


    public Database(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(getCreateTableDDL(PACKAGE_TABLE, PackageCols.values()));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DELETE TABLE " + PACKAGE_TABLE + ";");
        onCreate(db);
    }

    public Map<String,Integer> getMap() {
        Map<String,Integer> map = new HashMap<String,Integer>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(PACKAGE_TABLE, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            String packageName = PackageCols._ID.getString(cursor);
            int killOnSleep = PackageCols.KILL_ON_SLEEP.getInt(cursor);
            map.put(packageName, killOnSleep);
        }
        return map;
    }

    public long put(String packageName, int killOnSleep) {
        ContentValues values = new ContentValues();
        PackageCols._ID.put(values, packageName);
        PackageCols.KILL_ON_SLEEP.put(values, killOnSleep);
        SQLiteDatabase db = getWritableDatabase();
        long id = db.replace(PACKAGE_TABLE, null, values);
        return id;
    }

    private String getCreateTableDDL(String table, Column[] columns) {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("CREATE TABLE ").append(table).append('(');
        for (Column column : columns) {
            sbuf.append(column.name()).append(' ').append(column.type()).append(',');
        }
        sbuf.setLength(sbuf.length() - 1);
        sbuf.append(");");
        return sbuf.toString();
    }
}