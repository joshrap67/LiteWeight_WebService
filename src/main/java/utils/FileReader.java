package utils;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import models.OwnedExercise;

public class FileReader {

    public static final String DEFAULT_EXERCISES_FILE = "/DefaultExercises.txt";
    public static final String DEFAULT_PROFILE_PICTURE_FILE = "/DefaultProfilePicture.jpg";
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
                new InputStreamReader(FileReader.class.getResourceAsStream(DEFAULT_EXERCISES_FILE)));
            String line;
            while ((line = reader.readLine()) != null) {
                String uuid = UUID.randomUUID().toString();
                String name = line.split(EXERCISE_SPLIT_DELIM)[NAME_INDEX];
                String video = line.split(EXERCISE_SPLIT_DELIM)[VIDEO_INDEX];
                String[] focuses = line.split(EXERCISE_SPLIT_DELIM)[FOCUS_INDEX_FILE].split(FOCUS_DELIM);

                List<String> focusList = new ArrayList<>(Arrays.asList(focuses));
                OwnedExercise ownedExercise = new OwnedExercise(name, video, focusList);
                retVal.putIfAbsent(uuid, ownedExercise.asMap());
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }

//    public static byte[] getDefaultProfilePicture() throws IOException {
//        BufferedImage originalImage = ImageIO.read(FileReader.class.getResource(DEFAULT_PROFILE_PICTURE_FILE));
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(originalImage, "jpg", baos);
//        byte[] imageInByte = baos.toByteArray();
//        baos.close();
//        return imageInByte;
//    }
}
