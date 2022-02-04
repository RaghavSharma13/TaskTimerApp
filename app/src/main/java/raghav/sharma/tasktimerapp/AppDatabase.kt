package raghav.sharma.tasktimerapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

private const val TAG = "appDatabase"
private const val DATABASE_NAME = "TaskTimer.db"
private const val DATABASE_VERSION = 5
internal class AppDatabase private constructor(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){
    init{
        Log.d(TAG, "Database Initialized")
    }
    override fun onCreate(db: SQLiteDatabase) {
        Log.d(TAG, "onCreate Starts")
        val sSQL = """CREATE TABLE ${TasksContract.TABLE_NAME} (
            ${TasksContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${TasksContract.Columns.TASK_NAME} TEXT NOT NULL,
            ${TasksContract.Columns.TASK_DESCRIPTION} TEXT,
            ${TasksContract.Columns.SORT_ORDER} INTEGER);""".replaceIndent(" ")
        Log.d(TAG, "SQL script $sSQL")
        db.execSQL(sSQL)

        addTimingsTable(db)

        addTimingView(db)

        addDurationsView(db)

        addParameteriseView(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when(oldVersion){
            1 -> {
                addTimingsTable(db)
                addTimingView(db)
                addDurationsView(db)
                addParameteriseView(db)
             }
            2 -> {
                addTimingView(db)
                addDurationsView(db)
                addParameteriseView(db)
            }
            3->{
                addDurationsView(db)
                addParameteriseView(db)
            }
            4->{
                addParameteriseView(db)
            }
            else -> throw IllegalStateException("onUpgrade with unknown new version $newVersion")
        }
    }

    private fun addTimingsTable(db: SQLiteDatabase){
        val sSQL = """CREATE TABLE ${TimingsContract.TABLE_NAME} (
            ${TimingsContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${TimingsContract.Columns.TIMING_TASK_ID} INTEGER NOT NULL,
            ${TimingsContract.Columns.TIMING_START_TIME} INTEGER,
            ${TimingsContract.Columns.TIMING_DURATION} INTEGER);
        """.replaceIndent(" ")
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        val sSQLTrigger = """CREATE TRIGGER Remove_Task AFTER DELETE ON ${TasksContract.TABLE_NAME}
            FOR EACH ROW BEGIN DELETE FROM ${TimingsContract.TABLE_NAME}
            WHERE ${TimingsContract.Columns.TIMING_TASK_ID} = OLD.${TasksContract.Columns.ID};
            END;""".replaceIndent(" ")
        Log.d(TAG, "trigger: $sSQLTrigger")
        db.execSQL(sSQLTrigger)
    }

    private fun addTimingView(db: SQLiteDatabase){
        val sSQLTimingView = """CREATE VIEW ${CurrentTimingContract.VIEW_NAME}
        AS SELECT ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME}
        FROM ${TimingsContract.TABLE_NAME}
        JOIN ${TasksContract.TABLE_NAME}
        ON ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID} = ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}
        WHERE ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION} = 0
        ORDER BY ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME} DESC;
    """.replaceIndent(" ")
        Log.d(TAG, sSQLTimingView)
        db.execSQL(sSQLTimingView)
    }

    private fun addDurationsView(db: SQLiteDatabase){
        val sqlDurationsView = """CREATE VIEW ${DurationsContract.VIEW_NAME}
            AS SELECT ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_DESCRIPTION},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            DATE(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 'unixepoch', 'localtime') AS ${DurationsContract.Columns.START_DATE},
            SUM(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION}) AS ${DurationsContract.Columns.DURATION}
            FROM ${TasksContract.TABLE_NAME} INNER JOIN ${TimingsContract.TABLE_NAME}
            ON ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} = ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID}
            GROUP BY ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}, ${DurationsContract.Columns.START_DATE};""".replaceIndent(" ")
        Log.d(TAG, sqlDurationsView)
        db.execSQL(sqlDurationsView)
    }

    private fun addParameteriseView(db: SQLiteDatabase){
        var sSQL = """CREATE TABLE ${ParametersContract.TABLE_NAME}
            (${ParametersContract.Columns.ID} INTEGER PRIMARY KEY NOT NULL,
            ${ParametersContract.Columns.VALUE} INTEGER NOT NULL);""".trimMargin()
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        sSQL = "DROP VIEW IF EXISTS ${DurationsContract.VIEW_NAME};"
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        sSQL = """CREATE VIEW ${DurationsContract.VIEW_NAME}
            AS SELECT ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_NAME},
            ${TasksContract.TABLE_NAME}.${TasksContract.Columns.TASK_DESCRIPTION},
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME},
            DATE(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_START_TIME}, 'unixepoch', 'localtime')
            AS ${DurationsContract.Columns.START_DATE},
            SUM(${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION})
            AS ${DurationsContract.Columns.DURATION}
            FROM ${TasksContract.TABLE_NAME} INNER JOIN ${TimingsContract.TABLE_NAME}
            ON ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID} =
            ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_TASK_ID}
            WHERE ${TimingsContract.TABLE_NAME}.${TimingsContract.Columns.TIMING_DURATION} >
                (SELECT ${ParametersContract.TABLE_NAME}.${ParametersContract.Columns.VALUE}
                FROM ${ParametersContract.TABLE_NAME}
                WHERE ${ParametersContract.TABLE_NAME}.${ParametersContract.Columns.ID} = ${ParametersContract.ID_SHORT_TIMING})
            GROUP BY ${TasksContract.TABLE_NAME}.${TasksContract.Columns.ID}, ${DurationsContract.Columns.START_DATE}
            ;""".replaceIndent(" ")
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)

        sSQL = """INSERT INTO ${ParametersContract.TABLE_NAME} VALUES (${ParametersContract.ID_SHORT_TIMING}, 0);"""
        Log.d(TAG, sSQL)
        db.execSQL(sSQL)
    }

    companion object : SingletonHolder<AppDatabase, Context>(::AppDatabase)
}

