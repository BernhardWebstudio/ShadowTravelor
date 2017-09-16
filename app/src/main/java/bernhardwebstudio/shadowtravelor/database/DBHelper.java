package bernhardwebstudio.shadowtravelor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import bernhardwebstudio.shadowtravelor.data.Location;
import bernhardwebstudio.shadowtravelor.data.LocationTimeConnection;
import bernhardwebstudio.shadowtravelor.data.Route;
import bernhardwebstudio.shadowtravelor.data.RouteHistory;

/**
 * Created by Tim on 16.09.2017.
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "ShadowTravelorDB";

    public static final String TABLE_LOCATION = "Location";
    public static final String COLUMN_LONG = "Longitude";
    public static final String COLUMN_LAT = "Latitude";

    public static final String TABLE_LOC_TIME_CON = "LocationTime";
    public static final String COLUMN_VOLUME = "Volume";
    public static final String COLUMN_USAGE = "SmartphoneUsage";
    public static final String COLUMN_TIME = "Time";
    public static final String COLUMN_LOC = "Col_Location";
    public static final String COLUMN_VELOCITY = "Velocity";

    public static final String TABLE_ROUTE = "Route";
    public static final String COLUMN_ID = "ID";
    public static final String COLUMN_SCORE = "Score";
    public static final String COLUMN_DATE = "Date";

    public static final String TABLE_ROUTE_POINTS = "RoutePoints";
    public static final String COLUMN_ID_ROUTE = "ID_Route";
    public static final String COLUMN_ID_POINT = "ID_Point";

    public static final String TABLE_PROFILE = "Profile";

    public static final String TABLE_DAY = "Day";


    public static String createTableLocation =
            "CREATE TABLE " + TABLE_LOCATION + "(" + COLUMN_ID + " Integer Primary Key ," + COLUMN_LAT +
                    " real," + COLUMN_LONG + " real);";
    public static String createTableLocationTime =
            "CREATE TABLE " + TABLE_LOC_TIME_CON + "(" + COLUMN_ID + " Integer Primary Key ," + COLUMN_LOC +
                    " Integer," + COLUMN_VOLUME + " Integer," + COLUMN_USAGE +
                    " Integer," + COLUMN_TIME + " Integer," + COLUMN_VELOCITY + " real, FOREIGN KEY(" + COLUMN_LOC + ") REFERENCES Location(ID));";
    public static String createTableRoute =
            "CREATE TABLE " + TABLE_ROUTE + "(" + COLUMN_ID + " Integer Primary Key ," + COLUMN_SCORE +
                    " real," + COLUMN_DATE + " real);";
    public static String createTableRoutePoints =
            "CREATE TABLE " + TABLE_ROUTE_POINTS + "(" + COLUMN_ID_ROUTE + " Integer," + COLUMN_ID_POINT +
                    " Integer, FOREIGN KEY(" + COLUMN_ID_ROUTE + ") REFERENCES Route(ID), FOREIGN KEY(" + COLUMN_ID_POINT + ") REFERENCES LocationTime(ID));";

    public int oldVersion = 0;
    public static int currentVersion = 1;

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createTableLocation);
        db.execSQL(createTableLocationTime);
        db.execSQL(createTableRoute);
        db.execSQL(createTableRoutePoints);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long insertLocation(Location location, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAT, location.getLatitude());
        values.put(COLUMN_LONG, location.getLongitude());
        long id = db.insert(TABLE_LOCATION, null, values);
        return id;
    }

    public long insertLocationTimeConnection(LocationTimeConnection locTime, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_TIME, locTime.getDatetime().getTimeInMillis());
        values.put(COLUMN_LOC, insertLocation(locTime.getLocation(), db));
        values.put(COLUMN_USAGE, locTime.getUsedSmartphone());
        values.put(COLUMN_VOLUME, locTime.getVolume());
        long id = db.insert(TABLE_LOC_TIME_CON, null, values);
        return id;
    }

    public void addVelocity(double velocity, long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT INTO LocationTime(" + COLUMN_VELOCITY + ") VALUES " + velocity + " WHERE ID = " + id + ";");
        db.close();
    }

    public void insertRoute(Route route) {
        route.calculateVelocity();
        route.calculateScore();
        SQLiteDatabase db = this.getWritableDatabase();
        // zero, insert Route
        ContentValues route_value = new ContentValues();
        route_value.put(COLUMN_SCORE, route.getScore());
        route_value.put(COLUMN_DATE, route.getDate().getTimeInMillis());
        long route_id = db.insert(TABLE_ROUTE, null, route_value);

        // first, insert locationTimes
        for (int i = 0; i < route.getRoute().size(); i++) {
            long point_id = this.insertLocationTimeConnection(route.getRoute().get(i), db);
            ContentValues relation = new ContentValues();
            relation.put(COLUMN_ID_ROUTE, route_id);
            relation.put(COLUMN_ID_POINT, point_id);
            db.insert(TABLE_ROUTE_POINTS, null, relation);
        }

        db.close();
    }

    public LocationTimeConnection getLocationTimeConnectionByDate(GregorianCalendar cal) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_LOC_TIME_CON + "," + TABLE_LOCATION +
                " WHERE LocationTime.Time = " + cal.getTimeInMillis() + " AND " +
                "Location.ID = LocationTime.Col_Location;", null);
        LocationTimeConnection ltc = new LocationTimeConnection();
        while (cursor.moveToNext()) {

            return ltc;
        }
        return null;
    }

    public RouteHistory getRouteHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_ROUTE +
                " ORDER BY " + COLUMN_DATE + " DESC;", null);
        RouteHistory rh = new RouteHistory();
        while (cursor.moveToNext()) {
            Route route = new Route();
            route.setScore(cursor.getDouble(cursor.getColumnIndex(COLUMN_SCORE)));
            GregorianCalendar date = new GregorianCalendar();
            date.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(COLUMN_DATE)));
            route.setDate(date);
            rh.add(route);
        }
        cursor.close();
        return rh;
    }


    public ArrayList<Route> getAllRoutes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Route", null);
        cursor.moveToFirst();
        ArrayList<Route> allRoutes = new ArrayList<Route>();
        for (int i = 0; i < allRoutes.size(); i++) {
            Route r = new Route();
            int id = cursor.getLong(cursor.getColumnIndex(COLUMN_ID));
            GregorianCalendar date = new GregorianCalendar();
            date.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(COLUMN_DATE)));
            r.setDate(date);
            r.setScore(cursor.getDouble(cursor.getColumnIndex(COLUMN_SCORE)));
            Cursor cursor2 = db.rawQuery("SELECT Time, Velocity FROM LocationTime WHERE LocationTime.ID In (SELECT * FROM RoutePoints WHERE ID_Route = " + id + ");", null);
            cursor2.moveToFirst();
            for (int j = 0; j < cursor2.getCount(); j++) {
                GregorianCalendar gC = new GregorianCalendar();
                gC.setTimeInMillis(cursor2.getLong(cursor2.getColumnIndex(COLUMN_DATE)));
                LocationTimeConnection ltc = new LocationTimeConnection();
                ltc.setDatetime(gC);
                ltc.setVelocity(cursor2.getDouble(1));
                r.add(ltc);
            }
            allRoutes.add(r);
        }
        db.close();
        return allRoutes;
    }

    public void resetDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        // query to obtain the names of all tables in your database
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        List<String> tables = new ArrayList<>();

        // iterate over the result set, adding every table name to a list
        while (c.moveToNext()) {
            tables.add(c.getString(0));
        }

        // call DROP TABLE on every table name
        for (String table : tables) {
            String dropQuery = "DROP TABLE IF EXISTS " + table;
            db.execSQL(dropQuery);
        }
    }
}
