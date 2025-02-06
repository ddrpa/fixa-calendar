package cc.ddrpa.fixa.loader;

import cc.ddrpa.fixa.FixaCalendar;

public class NopeDateLoader implements IFixaDateLoader {

    @Override
    public boolean load(FixaCalendar calendarInstance) {
        return true;
    }

    @Override
    public boolean update(int year, FixaCalendar calendarInstance) {
        return true;
    }

    @Override
    public boolean isOutdated() {
        return false;
    }
}