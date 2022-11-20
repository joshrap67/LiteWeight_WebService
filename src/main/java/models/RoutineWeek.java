package models;

import interfaces.Model;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RoutineWeek implements Iterable<RoutineDay>, Model {

    public static final String DAYS = "days";

    private List<RoutineDay> days;

    public RoutineWeek() {
        this.days = new ArrayList<>();
    }

    public RoutineWeek(Map<String, Object> json) {
        this.days = new ArrayList<>();

        List<Object> jsonDays = (List<Object>) json.get(DAYS);
        for (Object day : jsonDays) {
            RoutineDay routineDay = new RoutineDay((Map<String, Object>) day);
            this.days.add(routineDay);
        }
    }

    public int getNumberOfDays() {
        return this.days.size();
    }

    public RoutineDay getDay(int day) {
        return this.days.get(day);
    }

    public void appendDay(RoutineDay routineDay) {
        this.days.add(routineDay);
    }

    public void put(int dayIndex, RoutineDay routineDay) {
        this.days.set(dayIndex, routineDay);
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        List<Object> jsonDays = new ArrayList<>();
        for (RoutineDay day : this) {
            jsonDays.add(day.asMap());
        }
        retVal.put(DAYS, jsonDays);

        return retVal;
    }

    @Override
    public Iterator<RoutineDay> iterator() {
        return this.days.iterator();
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
