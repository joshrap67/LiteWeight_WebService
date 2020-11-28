package models;

import exceptions.InvalidAttributeException;
import interfaces.Model;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import lombok.Data;

@Data
public class RoutineWeek implements Iterable<Integer>, Model {

    private Map<Integer, RoutineDay> days;

    public RoutineWeek() {
        this.days = new HashMap<>();
    }

    public RoutineWeek(Map<String, Object> daysForWeek) {
        this.days = new HashMap<>();
        for (String day : daysForWeek.keySet()) {
            RoutineDay routineDay = new RoutineDay((Map<String, Object>) daysForWeek.get(day));
            this.days.put(Integer.parseInt(day), routineDay);
        }
    }

    public int getNumberOfDays() {
        return this.days.size();
    }

    public RoutineDay getDay(int day) {
        return this.days.get(day);
    }

    public void put(int dayIndex, RoutineDay routineDay) {
        this.days.put(dayIndex, routineDay);
    }

    @Override
    public Map<String, Object> asMap() {
        HashMap<String, Object> retVal = new HashMap<>();
        for (Integer day : this) {
            retVal.put(day.toString(), this.getDay(day).asMap());
        }
        return retVal;
    }

    @Override
    public Iterator<Integer> iterator() {
        return this.days.keySet().iterator();
    }

    @Override
    public Map<String, Object> asResponse() {
        return this.asMap();
    }
}
