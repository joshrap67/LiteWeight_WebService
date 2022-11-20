namespace Patcher.V1_V2.Models_v2;

public class RoutineDay_v2
{
    public string Tag { get; set; }
    public IList<RoutineExercise_v2> Exercises { get; set; } = new List<RoutineExercise_v2>();
}