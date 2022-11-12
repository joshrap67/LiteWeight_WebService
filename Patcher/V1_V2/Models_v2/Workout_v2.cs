namespace Patcher.V1_V2.Models_v2;

public class Workout_v2
{
    public string WorkoutId { get; set; }
    public string WorkoutName { get; set; }
    public string CreationDate { get; set; }
    public string Creator { get; set; }
    public Routine_v2 Routine { get; set; }
    public int CurrentDay { get; set; }
    public int CurrentWeek { get; set; }
}
