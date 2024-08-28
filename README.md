# FixaCalendar - 基于 RoaringBitmap 的工作日/休息日数据结构与计算实现

## 介绍

在 FixaCalendar 中，节假日被处理为时间线上的点，程序通过查询时间线上起始日期和终止日期之间的点的数量来计算两个日期之间的节假日数量，从而进行工作日相关的推导。

那么，为什么不直接用一个列表存储节假日，然后遍历计算节假日数量呢？

作者没有也没打算做性能分析，
因为不管性能是更好了还是更坏了，在大多数使用场景下这点都不够看的。内存消耗也不是什么大的问题，假日办只会在每年 11 月 25 日左右发布明年的假期安排，对于一般应用，原本就不需要把多少数据放在内存里。

作者只是演示下 Bitmap 能拿来这么用而已，你要抠性能可以换成有序数组。

Fixa 是作者用 JetBrains IDEA 随机项目生成器插件使用瑞典家具风格随机生成的项目名字。

## 定义

FixaCalendar 主要用于计算非工作日（DayOff）和工作日（Workday），但 `isXXX` 方法还支持额外的定义：

- `isWeekend`: 是否是周末（与是否为工作日无关）
- `isHoliday`: 是否是节假日（可能同时是周末）
- `isFlexibleWorkday`: 是否是调休产生的工作日（也可能同时是周末）

周末（`Weekend`）是固定的，由 FixaWeekendEnum 枚举类型的变量在初始化时定义。

节假日（`Holiday`）和调休工作日（`FlexibleWorkday`）可以由方法动态添加，但是需要注意添加的顺序，因为节假日和调休工作日的优先级是一样的。

例如先调用 `addHolidays(LocalDate.of(2024,4,1))` 添加了 2024-04-01 为节日，再使用 `addFlexibleWorkday` 将其设置为调休产生的工作日，则对该日期的 `isHoliday` 和 `isFlexibleWorkday` 以及 `isWorkday` 都将返回 `True`，而 `isDayOff` 返回 `False`。

如果先调用 `addFlexibleWorkday` 再调用 `addHolidays`，则 `isWorkday` 返回 `False`，`isDayOff` 返回 `True`。

## 使用方法

你可以通过 Maven 中央仓库获取 FixaCalendar，最新版本可通过 [Sonatype Maven Central Repository](https://central.sonatype.com/artifact/cc.ddrpa.fixa/fixa) 查询，目前发布的版本为 `0.0.1`。

```xml
<dependency>
    <groupId>cc.ddrpa.fixa</groupId>
    <artifactId>fixa</artifactId>
    <version>1.0.0</version>
</dependency>
```

你需要使用 JDK11+ 来运行 Fixa，JDK8 的支持不在考虑范围内。如果你仍在使用 JDK8，应该至少试试看 JDK11。

构造一个 FixaCalendar 实例，从2024年3月9日开始初始化一年的周六周日作为周末，手动添加 2024年的清明节假期调休与工作日：

```java
FixaCalendar calendar = new FixaCalendar(
        FixaWeekendEnum.SATURDAY_AND_SUNDAY,
        LocalDate.of(2024, 3, 9),
        Duration.ofDays(365));
calendar.addHolidays(List.of(
        LocalDate.of(2024, 4, 4),
        LocalDate.of(2024, 4, 5),
        LocalDate.of(2024, 4, 6),
));
calendar.addFlexibleWorkday(LocalDate.of(2024, 4, 7));
```

### 计算两个日期之间的工作日

行为和 Microsoft Excel 的 `NETWORKDAYS(start_date, end_date)` 函数类似。

```java
int workdays = calendar.netWorkdays(start, end);
```

### 计算指定日期后第 N 个工作日的日期

行为和 Microsoft Excel 的 `WORKDAY(start_date, days)` 函数类似。

```java
LocalDate date = calendar.workday(startDate, duration);
```

### 列出指定日期区间内的所有非工作日

```java
List<LocalDate> dayOffs = calendar.dayOffs(startDate, endDate);
```

### 获取给定日期的下一个非工作日

```java
LocalDate nextDayOff = calendar.nextDayOff(startDate);
```

## 如何更新节假日数据

对一个节假日日历来说，最重要的是初始化数据从哪里来。互联网上有许多这样的节假日数据，本项目在单元测试中展示了两种通过互联网更新节假日信息的方案，你可以设置定时任务，在每年的 11月底 / 12月初更新来年的节假日信息。

### 通过 iCalendar 订阅更新

iCalendar（通常使用 `.ics` 扩展名）是一种基于文本的日历信息交换和管理格式。该规范最初由 IETF 制定，标准化为 RFC 5545。它广泛用于各种应用程序和服务，如 Google Calendar、Microsoft Outlook、Apple Calendar。

`cc.ddrpa.fixa.ICSAutoUpdateTests` 展示了使用 Apple Calendar 的中国大陆节假日日历订阅地址获取 iCalendar 格式数据（并解析），用来初始化节假日信息。如果你需要使用其他订阅源，`biweekly.component.VEvent` 的解读可能会略有不同。

### NateScarlet/holiday-cn - GitHub

代码见 `cc.ddrpa.fixa.NateScarletAutoUpdateTests`。

[NateScarlet/holiday-cn - GitHub](https://github.com/NateScarlet/holiday-cn") 使用网页爬虫抓取假日办的网站页面，并将解析出的节假日信息以 JSON 格式发布在 GitHub Repository 上，可以通过 JSDelivr CDN 获取按年份分割的 JSON 数据。

