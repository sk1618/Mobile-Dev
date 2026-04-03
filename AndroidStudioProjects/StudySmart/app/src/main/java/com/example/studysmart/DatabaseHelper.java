package com.example.studysmart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "studysmart.db";
    private static final int DATABASE_VERSION = 8;

    // Tasks table
    public static final String TABLE_TASKS = "tasks";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_PRIORITY = "priority";
    public static final String COLUMN_STATUS = "status";

    // Users table
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_PASSWORD = "password";

    // Reminders table
    public static final String TABLE_REMINDERS = "reminders";
    public static final String COLUMN_REMINDER_ID = "reminder_id";
    public static final String COLUMN_REMINDER_TITLE = "reminder_title";
    public static final String COLUMN_REMINDER_DATE = "reminder_date";
    public static final String COLUMN_REMINDER_TIME = "reminder_time";
    public static final String COLUMN_REMINDER_DONE = "reminder_done";

    // Study sessions table
    public static final String TABLE_STUDY_SESSIONS = "study_sessions";
    public static final String COLUMN_SESSION_ID = "session_id";
    public static final String COLUMN_SESSION_DURATION = "session_duration";
    public static final String COLUMN_SESSION_DATE = "session_date";

    // AI roadmap history table
    public static final String TABLE_AI_HISTORY = "ai_history";
    public static final String COLUMN_AI_ID = "ai_id";
    public static final String COLUMN_AI_TOPIC = "ai_topic";
    public static final String COLUMN_AI_ROADMAP = "ai_roadmap";
    public static final String COLUMN_AI_CREATED_AT = "ai_created_at";

    // Exams table
    public static final String TABLE_EXAMS = "exams";
    public static final String COLUMN_EXAM_ID = "exam_id";
    public static final String COLUMN_EXAM_TITLE = "exam_title";
    public static final String COLUMN_EXAM_DATE = "exam_date";
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTasksTable = "CREATE TABLE " + TABLE_TASKS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_CATEGORY + " TEXT, " +
                COLUMN_PRIORITY + " TEXT, " +
                COLUMN_STATUS + " TEXT)";

        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_EMAIL + " TEXT UNIQUE, " +
                COLUMN_PASSWORD + " TEXT)";

        String createRemindersTable = "CREATE TABLE " + TABLE_REMINDERS + " (" +
                COLUMN_REMINDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_REMINDER_TITLE + " TEXT, " +
                COLUMN_REMINDER_DATE + " TEXT, " +
                COLUMN_REMINDER_TIME + " TEXT, " +
                COLUMN_REMINDER_DONE + " INTEGER DEFAULT 0)";

        String createStudySessionsTable = "CREATE TABLE " + TABLE_STUDY_SESSIONS + " (" +
                COLUMN_SESSION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SESSION_DURATION + " INTEGER, " +
                COLUMN_SESSION_DATE + " TEXT)";

        String createAiHistoryTable = "CREATE TABLE " + TABLE_AI_HISTORY + " (" +
                COLUMN_AI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_AI_TOPIC + " TEXT, " +
                COLUMN_AI_ROADMAP + " TEXT, " +
                COLUMN_AI_CREATED_AT + " TEXT)";

        String createExamsTable = "CREATE TABLE " + TABLE_EXAMS + " (" +
                COLUMN_EXAM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_EXAM_TITLE + " TEXT, " +
                COLUMN_EXAM_DATE + " TEXT)";

        db.execSQL(createTasksTable);
        db.execSQL(createUsersTable);
        db.execSQL(createRemindersTable);
        db.execSQL(createStudySessionsTable);
        db.execSQL(createAiHistoryTable);
        db.execSQL(createExamsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REMINDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDY_SESSIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_AI_HISTORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXAMS);
        onCreate(db);
    }

    // TASK METHODS
    public boolean insertTask(String title, String category, String priority) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_PRIORITY, priority);
        values.put(COLUMN_STATUS, "Pending");
        long result = db.insert(TABLE_TASKS, null, values);
        return result != -1;
    }

    public ArrayList<Task> getTasksByStatus(String status) {
        ArrayList<Task> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_TASKS + " WHERE " + COLUMN_STATUS + " = ? ORDER BY " + COLUMN_ID + " DESC",
                new String[]{status}
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
                String priority = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIORITY));
                String taskStatus = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
                taskList.add(new Task(id, title, category, priority, taskStatus));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return taskList;
    }

    public boolean updateTaskStatus(int taskId, String newStatus) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, newStatus);
        int result = db.update(TABLE_TASKS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
        return result > 0;
    }

    public boolean deleteTask(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_TASKS, COLUMN_ID + " = ?", new String[]{String.valueOf(taskId)});
        return result > 0;
    }

    public int getTaskCountByStatus(String status) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_TASKS + " WHERE " + COLUMN_STATUS + " = ?",
                new String[]{status}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getTotalTaskCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_TASKS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // USER METHODS
    public boolean insertUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_EMAIL, email);
        values.put(COLUMN_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ?",
                new String[]{email}
        );
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public boolean checkUserLogin(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ? AND " + COLUMN_PASSWORD + " = ?",
                new String[]{email, password}
        );
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }

    public String getUserNameByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_NAME + " FROM " + TABLE_USERS + " WHERE " + COLUMN_EMAIL + " = ?",
                new String[]{email}
        );
        String name = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        }
        cursor.close();
        return name;
    }

    public boolean updateUserProfile(String oldEmail, String newName, String newEmail, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, newName);
        values.put(COLUMN_EMAIL, newEmail);
        values.put(COLUMN_PASSWORD, newPassword);
        int result = db.update(TABLE_USERS, values, COLUMN_EMAIL + " = ?", new String[]{oldEmail});
        return result > 0;
    }

    // REMINDER METHODS
    public boolean insertReminder(String title, String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_TITLE, title);
        values.put(COLUMN_REMINDER_DATE, date);
        values.put(COLUMN_REMINDER_TIME, time);
        values.put(COLUMN_REMINDER_DONE, 0);
        long result = db.insert(TABLE_REMINDERS, null, values);
        return result != -1;
    }

    public ArrayList<Reminder> getRemindersByFilter(String filter) {
        ArrayList<Reminder> reminderList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor;

        if (filter.equals("Pending")) {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_REMINDERS + " WHERE " + COLUMN_REMINDER_DONE + " = 0 ORDER BY " + COLUMN_REMINDER_ID + " DESC",
                    null
            );
        } else if (filter.equals("Done")) {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_REMINDERS + " WHERE " + COLUMN_REMINDER_DONE + " = 1 ORDER BY " + COLUMN_REMINDER_ID + " DESC",
                    null
            );
        } else {
            cursor = db.rawQuery(
                    "SELECT * FROM " + TABLE_REMINDERS + " ORDER BY " + COLUMN_REMINDER_ID + " DESC",
                    null
            );
        }

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_ID));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TITLE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_TIME));
                int isDone = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REMINDER_DONE));
                reminderList.add(new Reminder(id, title, date, time, isDone));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return reminderList;
    }

    public boolean updateReminderDoneStatus(int reminderId, int isDone) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_REMINDER_DONE, isDone);
        int result = db.update(TABLE_REMINDERS, values, COLUMN_REMINDER_ID + " = ?", new String[]{String.valueOf(reminderId)});
        return result > 0;
    }

    public boolean deleteReminder(int reminderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_REMINDERS, COLUMN_REMINDER_ID + " = ?", new String[]{String.valueOf(reminderId)});
        return result > 0;
    }

    public int getReminderCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_REMINDERS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getCompletedReminderCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_REMINDERS + " WHERE " + COLUMN_REMINDER_DONE + " = 1",
                null
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    // STUDY SESSION METHODS
    public boolean insertStudySession(int durationMinutes, String sessionDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SESSION_DURATION, durationMinutes);
        values.put(COLUMN_SESSION_DATE, sessionDate);
        long result = db.insert(TABLE_STUDY_SESSIONS, null, values);
        return result != -1;
    }

    public int getTotalStudySessions() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_STUDY_SESSIONS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public int getTotalStudyMinutes() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_SESSION_DURATION + ") FROM " + TABLE_STUDY_SESSIONS, null);
        int total = 0;
        if (cursor.moveToFirst() && !cursor.isNull(0)) {
            total = cursor.getInt(0);
        }
        cursor.close();
        return total;
    }

    public String getLastStudySessionDate() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_SESSION_DATE + " FROM " + TABLE_STUDY_SESSIONS +
                        " ORDER BY " + COLUMN_SESSION_ID + " DESC LIMIT 1",
                null
        );

        String date = null;
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        return date;
    }

    public ArrayList<StudySession> getAllStudySessions() {
        ArrayList<StudySession> sessionList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_STUDY_SESSIONS + " ORDER BY " + COLUMN_SESSION_ID + " DESC",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SESSION_ID));
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SESSION_DURATION));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SESSION_DATE));
                sessionList.add(new StudySession(id, duration, date));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return sessionList;
    }

    // AI HISTORY METHODS
    public boolean insertAiRoadmap(String topic, String roadmap, String createdAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_AI_TOPIC, topic);
        values.put(COLUMN_AI_ROADMAP, roadmap);
        values.put(COLUMN_AI_CREATED_AT, createdAt);
        long result = db.insert(TABLE_AI_HISTORY, null, values);
        return result != -1;
    }

    public ArrayList<AiRoadmap> getAllAiRoadmaps() {
        ArrayList<AiRoadmap> roadmapList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_AI_HISTORY + " ORDER BY " + COLUMN_AI_ID + " DESC",
                null
        );

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AI_ID));
                String topic = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_TOPIC));
                String roadmap = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_ROADMAP));
                String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AI_CREATED_AT));

                roadmapList.add(new AiRoadmap(id, topic, roadmap, createdAt));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return roadmapList;
    }

    public boolean deleteAiRoadmap(int roadmapId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_AI_HISTORY, COLUMN_AI_ID + " = ?", new String[]{String.valueOf(roadmapId)});
        return result > 0;
    }

    // EXAM METHODS
    public boolean insertExam(String title, String examDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXAM_TITLE, title);
        values.put(COLUMN_EXAM_DATE, examDate);
        long result = db.insert(TABLE_EXAMS, null, values);
        return result != -1;
    }

    public int getDistinctSubjectCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT " + COLUMN_CATEGORY + ") FROM " + TABLE_TASKS +
                        " WHERE " + COLUMN_CATEGORY + " IS NOT NULL AND " + COLUMN_CATEGORY + " != ''",
                null
        );

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public Exam getNearestUpcomingExam() {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_EXAMS +
                        " WHERE " + COLUMN_EXAM_DATE + " >= date('now','localtime')" +
                        " ORDER BY " + COLUMN_EXAM_DATE + " ASC LIMIT 1",
                null
        );

        Exam exam = null;

        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EXAM_ID));
            String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXAM_TITLE));
            String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EXAM_DATE));

            exam = new Exam(id, title, date);
        }

        cursor.close();
        return exam;
    }
}
