package helpers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import models.ExerciseUser;

public class FileReader {

    public static final String DEFAULT_EXERCISES_FILE = "/DefaultExercises.txt";
    public static final String EXERCISE_SPLIT_DELIM = "\\*";
    public static final String FOCUS_DELIM = ",";
    public static final int
        NAME_INDEX = 0,
        VIDEO_INDEX = 1,
        FOCUS_INDEX_FILE = 2;


    public static Map<String, Map<String, Object>> getDefaultExercises() {
        Map<String, Map<String, Object>> retVal = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(
                new InputStreamReader(
                    FileReader.class.getResourceAsStream(DEFAULT_EXERCISES_FILE)));
            String line;
            while ((line = reader.readLine()) != null) {
                String uuid = UUID.randomUUID().toString();
                String name = line.split(EXERCISE_SPLIT_DELIM)[NAME_INDEX];
                String video = line.split(EXERCISE_SPLIT_DELIM)[VIDEO_INDEX];
                String[] focuses = line.split(EXERCISE_SPLIT_DELIM)[FOCUS_INDEX_FILE]
                    .split(FOCUS_DELIM);
                HashMap<String, Boolean> focusMap = new HashMap<>();
                for (String focus : focuses) {
                    focusMap.putIfAbsent(focus, true);
                }
                ExerciseUser exerciseUser = new ExerciseUser(name, video, focusMap);
                retVal.putIfAbsent(uuid, exerciseUser.asMap());
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
