package cc.ddrpa.fixa.loader;

import cc.ddrpa.fixa.FixaCalendar;

/**
 * Do nothing loader
 */
public interface IFixaDateLoader {

    /**
     * 加载所有节假日信息
     *
     * @return
     */
    boolean load(FixaCalendar calendarInstance);

    /**
     * 更新指定年份的节假日信息
     *
     * @param year
     * @return
     */
    boolean update(int year, FixaCalendar calendarInstance);


    /**
     * 判断数据是否过时
     *
     * @return
     */
    boolean isOutdated();
}