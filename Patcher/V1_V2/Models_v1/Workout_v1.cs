namespace Patcher.V1_V2.Models_v1;

public class Workout_v1
{
    public string WorkoutId { get; set; }
    public string WorkoutName { get; set; }
    public string CreationDate { get; set; }
    public string MostFrequentFocus { get; set; }
    public string Creator { get; set; }
    public Dictionary<string, Dictionary<string, Dictionary<string, RoutineExercise_v1>>> Routine { get; set; }
    public int CurrentDay { get; set; }
    public int CurrentWeek { get; set; }
}
