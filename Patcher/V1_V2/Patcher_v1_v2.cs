using Amazon.DynamoDBv2.DocumentModel;
using Patcher.V1_V2.Models_v1;
using Patcher.V1_V2.Models_v2;
using System.Text.Json;

namespace Patcher.V1_V2;

public class Patcher_v1_v2 : Patcher
{

    public Patcher_v1_v2() : base()
    {

    }

    public override async Task Patch()
    {
        await PatchWorkouts();
    }

    private async Task PatchWorkouts()
    {
        var serializeOptions = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
            WriteIndented = true
        };

        var workoutsTable = Table.LoadTable(DynamoDBClient, "workouts", true);
        var search = workoutsTable.Scan(new ScanFilter());
        var v1Workouts = new List<Workout_v1>();

        while (!search.IsDone)
        {
            var docList = search.GetNextSetAsync();
            docList.Result.ForEach(async doc =>
            {
                try
                {
                    // backup data in case something blows up
                    var id = doc["workoutId"];
                    var json = doc.ToJson();
                    var path = Directory.GetParent(Environment.CurrentDirectory).Parent.Parent.FullName;
                    await File.WriteAllTextAsync($"{path}/Backups/{id}.json", json);

                    var workout = JsonSerializer.Deserialize<Workout_v1>(json, serializeOptions);
                    v1Workouts.Add(workout);
                }
                catch (Exception ex)
                {
                    Console.WriteLine(ex.Message);
                }
            });
        }

        //return; // uncomment when ready

        // patch the workouts
        foreach (var v1Workout in v1Workouts.ToList())
        {
            var v2Workout = new Workout_v2()
            {
                CreationDate = v1Workout.CreationDate,
                Creator = v1Workout.Creator,
                WorkoutId = v1Workout.WorkoutId,
                WorkoutName = v1Workout.WorkoutName,
                CurrentWeek = v1Workout.CurrentWeek,
                CurrentDay = v1Workout.CurrentDay,
                Routine = MapRoutineToVersion2(v1Workout.Routine)
            };
            var v2WorkoutJson = JsonSerializer.Serialize(v2Workout, serializeOptions);
            var v2WorkoutDoc = Document.FromJson(v2WorkoutJson);
            try
            {
                await workoutsTable.UpdateItemAsync(v2WorkoutDoc);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error for id: {v2Workout.WorkoutId}\n{ex.Message}");
            }

        }
    }

    private static Routine_v2 MapRoutineToVersion2(Dictionary<string, Dictionary<string, Dictionary<string, RoutineExercise_v1>>> v1_routine)
    {
        var v2_routine = new Routine_v2();
        foreach (var weekKey in v1_routine.Keys)
        {
            var v1_week = v1_routine[weekKey];
            var v2_week = new RoutineWeek_v2();
            foreach (var dayKey in v1_week.Keys)
            {
                var v1_day = v1_week[dayKey];
                var v2_day = new RoutineDay_v2();
                foreach (var exerciseKey in v1_day.Keys)
                {
                    var v1_exercise = v1_day[exerciseKey];
                    var v2_exercise = new RoutineExercise_v2
                    {
                        Completed = v1_exercise.Completed,
                        Details = v1_exercise.Details,
                        ExerciseId = v1_exercise.ExerciseId,
                        Reps = v1_exercise.Reps,
                        Sets = v1_exercise.Sets,
                        Weight = v1_exercise.Weight
                    };
                    v2_day.Exercises.Add(v2_exercise);
                }
                v2_week.Days.Add(v2_day);
            }
            v2_routine.Weeks.Add(v2_week);
        }
        return v2_routine;
    }
}
