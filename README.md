# FixaCalendar

基于 RoaringBitmap 的工作日计算库，提供工作日/休息日判定、区间工作日统计、工作日推算等功能。

## 设计思路

FixaCalendar 将日期映射为时间线上的整数点（epoch day），使用位图数据结构存储非工作日集合。计算两个日期之间的工作日数量时，只需查询区间内的非工作日基数（cardinality），无需遍历。

核心数据结构：

- **weekendMap**: 存储周末日期
- **holidayMap**: 存储法定节假日
- **flexibleWorkdayMap**: 存储调休产生的工作日
- **dayOffMap**: 实际非工作日集合，计算公式为 `weekend + holiday - flexibleWorkday`

选用 RoaringBitmap 是因为其在稀疏位图场景下具有良好的压缩率和查询性能。对于日历数据这类分布相对稀疏的场景，RoaringBitmap 的 `rangeCardinality` 操作可在常数时间内完成区间统计。

## 术语定义

| 术语 | 说明 |
|------|------|
| Weekend | 周末，由 `FixaWeekendEnum` 在初始化时定义，不可动态修改 |
| Holiday | 法定节假日，可通过 `addHoliday` 系列方法添加 |
| FlexibleWorkday | 调休工作日，即原本是周末但需要上班的日期 |
| DayOff | 非工作日，包括未被调休覆盖的周末和节假日 |
| Workday | 工作日，即非 DayOff 的日期 |

`isXXX` 方法返回的是日期的属性标记，而非互斥状态。例如，某个周六被标记为调休工作日后：

- `isWeekend()` 返回 `true`（仍是周六）
- `isFlexibleWorkday()` 返回 `true`
- `isDayOff()` 返回 `false`
- `isWorkday()` 返回 `true`

### 添加顺序的影响

节假日和调休工作日的添加顺序会影响最终的 `dayOffMap` 状态：

```java
// 先添加节假日，再标记为调休工作日
calendar.addHoliday(LocalDate.of(2024, 4, 1));
calendar.addFlexibleWorkday(LocalDate.of(2024, 4, 1));
// 结果：isDayOff() = false, isWorkday() = true

// 先标记调休工作日，再添加节假日
calendar.addFlexibleWorkday(LocalDate.of(2024, 4, 1));
calendar.addHoliday(LocalDate.of(2024, 4, 1));
// 结果：isDayOff() = true, isWorkday() = false
```

## 安装

通过 Maven 中央仓库获取，当前稳定版本为 `1.0.2`：

```xml
<dependency>
    <groupId>cc.ddrpa.fixa</groupId>
    <artifactId>fixa</artifactId>
    <version>1.0.2</version>
</dependency>
```

最新版本可在 [Maven Central](https://central.sonatype.com/artifact/cc.ddrpa.fixa/fixa) 查询。

**系统要求**: JDK 11+

## 快速开始

### 创建日历实例

使用 `FixaCalendarBuilder` 构建日历实例：

```java
FixaCalendar calendar = new FixaCalendarBuilder()
    .setWeekendType(FixaWeekendEnum.SATURDAY_AND_SUNDAY)
    .startWeekendCalcAfter(LocalDate.of(2024, 1, 1))
    .setWeekendCalcDuration(Duration.ofDays(365 * 3))
    .registerDateLoader(new ICSDateLoader(
        URI.create("https://example.com/holidays.ics"),
        "holiday-calendar.ics"))
    .build();
```

Builder 方法说明：

| 方法 | 默认值 | 说明 |
|------|--------|------|
| `setWeekendType` | `SATURDAY_AND_SUNDAY` | 周末类型，支持单休和双休的多种组合 |
| `startWeekendCalcAfter` | `LocalDate.now()` | 周末计算的起始日期 |
| `setWeekendCalcDuration` | 5 年 | 预计算周末的时间跨度 |
| `registerDateLoader` | `NopeDateLoader` | 节假日数据加载器 |

### 手动添加节假日和调休

```java
// 添加单个节假日
calendar.addHoliday(LocalDate.of(2024, 10, 1));

// 添加连续节假日
calendar.addHolidays(LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 7));

// 添加节假日列表
calendar.addHolidays(List.of(
    LocalDate.of(2024, 1, 1),
    LocalDate.of(2024, 2, 10)
));

// 添加调休工作日
calendar.addFlexibleWorkday(LocalDate.of(2024, 10, 12));
```

## API 参考

### 日期判定

```java
boolean isWorkday(LocalDate date)      // 是否为工作日
boolean isDayOff(LocalDate date)       // 是否为非工作日
boolean isWeekend(LocalDate date)      // 是否为周末
boolean isHoliday(LocalDate date)      // 是否为节假日
boolean isFlexibleWorkday(LocalDate date)  // 是否为调休工作日
```

### 工作日统计

计算两个日期之间的工作日数量（含首尾），行为与 Excel `NETWORKDAYS` 函数一致：

```java
int workdays = calendar.netWorkdays(
    LocalDate.of(2024, 4, 1),
    LocalDate.of(2024, 4, 30)
);
```

### 工作日推算

计算指定日期后第 N 个工作日，行为与 Excel `WORKDAY` 函数一致：

```java
LocalDate date = calendar.workday(
    LocalDate.of(2024, 4, 1),
    Duration.ofDays(10)
);
```

计算指定日期前第 N 个工作日：

```java
LocalDate date = calendar.reverseWorkday(
    LocalDate.of(2024, 4, 30),  // 截止日期
    Duration.ofDays(5),          // 向前推算 5 个工作日
    true                         // 若截止日期非工作日，先向前推至最近工作日
);
```

### 非工作日查询

```java
// 列出区间内所有非工作日
List<LocalDate> dayOffs = calendar.dayOffs(startDate, endDate);

// 获取下一个非工作日（不含当日）
LocalDate nextDayOff = calendar.nextDayOff(startDate);
```

## 节假日数据更新

FixaCalendar 通过 `IFixaDateLoader` 接口支持从外部数据源加载节假日信息。

### 内置加载器

**ICSDateLoader**

从 ICS 格式的日历文件加载节假日数据：

```java
// 指定日历 URL 和本地缓存文件名
new ICSDateLoader(
    URI.create("https://example.com/holidays.ics"),
    "holiday-calendar.ics")

// 自定义缓存策略：缓存有效期 300 天，12 月和 1 月有效期 2 天
new ICSDateLoader(
    URI.create("https://example.com/holidays.ics"),
    "holiday-calendar.ics",
    300,  // 缓存有效期（天数）
    2)    // 12 月和 1 月的缓存有效期（天数）
```

该加载器会将 `.ics` 文件缓存至本地，并根据以下策略判断是否过期：

- 12 月和 1 月：缓存有效期 2 天（默认）
- 其他月份：缓存有效期 300 天（默认）

### 手动更新

```java
calendar.update(2025);  // 更新 2025 年的节假日数据
```

### 自定义加载器

实现 `IFixaDateLoader` 接口：

```java
public interface IFixaDateLoader {
    boolean load(FixaCalendar calendarInstance);
    boolean update(int year, FixaCalendar calendarInstance);
    boolean isOutdated();
}
```

示例：使用 [NateScarlet/holiday-cn](https://github.com/NateScarlet/holiday-cn) 数据源：

```java
String url = String.format(
    "https://fastly.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%d.json",
    year
);
HttpResponse<String> response = HttpClient.newHttpClient()
    .send(HttpRequest.newBuilder().uri(URI.create(url)).GET().build(),
          HttpResponse.BodyHandlers.ofString());

JsonArray days = JsonParser.parseString(response.body())
    .getAsJsonObject()
    .getAsJsonArray("days");

for (JsonElement day : days) {
    JsonObject obj = day.getAsJsonObject();
    LocalDate date = LocalDate.parse(obj.get("date").getAsString());
    if (obj.get("isOffDay").getAsBoolean()) {
        calendar.addHoliday(date);
    } else {
        calendar.addFlexibleWorkday(date);
    }
}
```

完整实现参见 `cc.ddrpa.fixa.NateScarletAutoUpdateTests`。

## 周末类型

`FixaWeekendEnum` 支持以下周末定义：

**双休**

| 枚举值 | 说明 |
|--------|------|
| `SATURDAY_AND_SUNDAY` | 周六、周日 |
| `SUNDAY_AND_MONDAY` | 周日、周一 |
| `FRIDAY_AND_SATURDAY` | 周五、周六 |
| ... | 其他连续两天组合 |

**单休**

| 枚举值 | 说明 |
|--------|------|
| `SUNDAY_ONLY` | 仅周日 |
| `SATURDAY_ONLY` | 仅周六 |
| ... | 其他单日 |

## 技术细节

### 日期存储

日期使用 `LocalDate.toEpochDay()` 转换为整数存储。epoch day 是从 1970-01-01 开始计算的天数，可表示的日期范围覆盖公元前后数百万年。

### RoaringBitmap 操作

主要使用的 RoaringBitmap 方法：

- `contains(int)`: O(1) 判断日期是否在集合中
- `rangeCardinality(long, long)`: O(1) 统计区间内元素数量
- `add(int[])`: 批量添加日期
- `andNot(RoaringBitmap)`: 集合差运算，用于从 dayOffMap 中移除调休工作日

### 线程安全

`FixaCalendar` 实例不是线程安全的。如需在多线程环境使用，建议：

- 在应用启动时完成所有节假日配置
- 运行时仅调用只读方法（`isXXX`、`netWorkdays` 等）
- 或使用外部同步机制

## 许可证

Apache License 2.0
