package models;

import interfaces.Model;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SharedWorkoutExercise implements Model {

    public static final String FOCUSES = "focuses";
    public static final String VIDEO_URL = "videoUrl";

    private String videoUrl;
    private List<String> focuses;

    public SharedWorkoutExercise(final Map<String, Object> json) {
        this.videoUrl = (String) json.get(VIDEO_URL);
        this.focuses = (List<String>) json.get(FOCUSES);
    }

    public SharedWorkoutExercise(final OwnedExercise ownedExercise) {
        this.focuses = ownedExercise.getFocuses();
        this.videoUrl = ownedExercise.getVideoUrl();
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        retVal.putIfAbsent(VIDEO_URL, this.videoUrl);
        retVal.putIfAbsent(FOCUSES, this.focuses);
        return retVal;
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
