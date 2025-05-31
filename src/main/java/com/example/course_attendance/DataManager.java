package com.example.course_attendance;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class DataManager {
    private static final String STUDENT_FILE = "students.json";
    private static final String COURSE_FILE = "courses.json";

    public static void saveStudent(Context context, String name, String id, String selfiePath) {
        try {
            JSONArray students = readJsonArray(context, STUDENT_FILE);
            JSONObject student = new JSONObject();
            student.put("name", name);
            student.put("id", id);
            student.put("selfie", selfiePath);
            students.put(student);
            writeJsonArray(context, STUDENT_FILE, students);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveCourse(Context context, String name, String professor,
                                  String day, String start, String end) {
        try {
            JSONArray courses = readJsonArray(context, COURSE_FILE);
            JSONObject course = new JSONObject();
            course.put("courseName", name);
            course.put("professor", professor);
            course.put("day", day);
            course.put("startTime", start);
            course.put("endTime", end);
            courses.put(course);
            writeJsonArray(context, COURSE_FILE, courses);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONArray readJsonArray(Context context, String filename) {
        try (FileInputStream fis = context.openFileInput(filename)) {
            byte[] data = new byte[fis.available()];
            fis.read(data);
            return new JSONArray(new String(data));
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private static void writeJsonArray(Context context, String filename, JSONArray array) {
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(array.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static JSONArray getAllStudents(Context context) {
        return readJsonArray(context, STUDENT_FILE);
    }

    public static JSONArray getAllCourses(Context context) {
        return readJsonArray(context, COURSE_FILE);
    }

    public static void clearAllData(Context context) {
        try {
            context.deleteFile(STUDENT_FILE);
            context.deleteFile(COURSE_FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}