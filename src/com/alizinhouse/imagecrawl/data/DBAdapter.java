package com.alizinhouse.imagecrawl.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * See: http://www.devx.com/wireless/Article/40842/1954
 * */
public class DBAdapter {
	public static final String KEY_ROWID = "id";
	public static final String KEY_NAME = "name";
	public static final String KEY_DESCRIPTION = "description";
	public static final String KEY_THUMBNAIL = "thumbnail";
	private static final String TAG = "DBAdapter";
	private static final String DATABASE_NAME = "imagecrawler";
	private static final String DATABASE_TABLE = "domains";
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_CREATE = "create table domains (id integer primary key autoincrement, "
			+ "name text not null, description text not null, thumbnail blob null);";

	private final Context context;
	private DatabaseHelper DBHelper;
	private SQLiteDatabase db;

	public DBAdapter(Context ctx) {
		this.context = ctx;
		DBHelper = new DatabaseHelper(context);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS domains");
			onCreate(db);
		}
	}

	// ---opens the database---
	public DBAdapter open() throws SQLException {
		db = DBHelper.getWritableDatabase();
		return this;
	}

	// ---closes the database---
	public void close() {
		DBHelper.close();
	}

	// ---insert a domain into the database---
	public long insertDomain(String name, String description) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(KEY_NAME, name);
		initialValues.put(KEY_DESCRIPTION, description);
		return db.insert(DATABASE_TABLE, null, initialValues);
	}

	// ---deletes a particular domain---
	public boolean deleteDomain(long rowId) {
		return db.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
	}

	// ---retrieves all the domains---
	public Cursor getAllDomains() {
		return db.query(DATABASE_TABLE, new String[] { KEY_ROWID, KEY_NAME,
				KEY_DESCRIPTION, KEY_THUMBNAIL }, null, null, null, null, null);
	}

	// ---retrieves a particular domain---
	public Cursor getDomain(long rowId) throws SQLException {
		Cursor mCursor = db.query(true, DATABASE_TABLE, new String[] {
				KEY_ROWID, KEY_NAME, KEY_DESCRIPTION },
				KEY_ROWID + "=" + rowId, null, null, null, null, null);
		if (mCursor != null) {
			mCursor.moveToFirst();
		}
		return mCursor;
	}

	// ---updates a domain---
	public boolean updateDomain(long rowId, String name, String descrption) {
		ContentValues args = new ContentValues();
		args.put(KEY_NAME, name);
		args.put(KEY_DESCRIPTION, descrption);
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}

	public boolean updateThumbnail(long rowId, byte[] thumbnail) {
		ContentValues args = new ContentValues();
		args.put(KEY_THUMBNAIL, thumbnail);
		return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
	}
}