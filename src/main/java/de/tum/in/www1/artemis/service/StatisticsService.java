package de.tum.in.www1.artemis.service;

import java.time.*;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.*;

import org.springframework.stereotype.Service;

import de.tum.in.www1.artemis.domain.enumeration.GraphType;
import de.tum.in.www1.artemis.domain.enumeration.SpanType;
import de.tum.in.www1.artemis.repository.StatisticsRepository;

@Service
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    public StatisticsService(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    /**
     * Forwards the request to the repository, which returns a List<Map<String, Object>>. For week, month or year the map from the Repository contains a String with the column name,
     * "day" and "amount" and an Object being the value, either the date in the format "YYYY-MM-DD" or the amount of the findings. For day, the column names are "day" and "amount",
     * which then contains the date in the ZonedDateFormat as Integer and the amount as Long
     * It then collects the amounts in an array, depending on the span value, and returns it
     *
     * @param span DAY,WEEK,MONTH or YEAR depending on the active tab in the view
     * @param periodIndex an index indicating which time period, 0 is current week, -1 is one week in the past, -2 is two weeks in the past ...
     * @param graphType the type of graph the data should be fetched
     * @return an array, containing the values for each bar in the graph
     */
    public Integer[] getChartData(SpanType span, Integer periodIndex, GraphType graphType) {
        ZonedDateTime startDate;
        ZonedDateTime endDate;
        List<Map<String, Object>> outcome;
        ZonedDateTime now = ZonedDateTime.now();
        int lengthOfMonth = YearMonth.of(now.getYear(), now.minusMonths(1 - periodIndex).plusDays(1).getMonth()).lengthOfMonth();
        Integer[] result = new Integer[createSpanMap(lengthOfMonth).get(span)];
        Arrays.fill(result, 0);
        switch (span) {
            case DAY:
                startDate = now.minusDays(-periodIndex).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endDate = now.minusDays(-periodIndex).withHour(23).withMinute(59).withSecond(59);
                outcome = getDataFromDatabase(span, startDate, endDate, graphType);
                return createResultArrayForDay(outcome, result, endDate);
            case WEEK:
                startDate = now.minusWeeks(-periodIndex).minusDays(6).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endDate = now.minusWeeks(-periodIndex).withHour(23).withMinute(59).withSecond(59);
                outcome = getDataFromDatabase(span, startDate, endDate, graphType);
                return createResultArrayForWeek(outcome, result, endDate);
            case MONTH:
                startDate = now.minusMonths(1 - periodIndex).plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endDate = now.minusMonths(-periodIndex).withHour(23).withMinute(59).withSecond(59);
                outcome = getDataFromDatabase(span, startDate, endDate, graphType);
                return createResultArrayForMonth(outcome, result, endDate);
            case QUARTER:
                LocalDateTime localStartDate = now.toLocalDateTime().with(DayOfWeek.MONDAY);
                LocalDateTime localEndDate = now.toLocalDateTime().with(DayOfWeek.SUNDAY);
                ZoneId zone = now.getZone();
                startDate = localStartDate.atZone(zone).minusWeeks(11 + (12 * (-periodIndex))).withHour(0).withMinute(0).withSecond(0).withNano(0);
                endDate = periodIndex != 0 ? localEndDate.atZone(zone).minusWeeks(12 * (-periodIndex)).withHour(23).withMinute(59).withSecond(59)
                        : localEndDate.atZone(zone).withHour(23).withMinute(59).withSecond(59);
                outcome = getDataFromDatabase(span, startDate, endDate, graphType);
                return createResultArrayForQuarter(outcome, result, endDate);
            case YEAR:
                startDate = now.minusYears(1 - periodIndex).plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                lengthOfMonth = YearMonth.of(now.minusYears(-periodIndex).getYear(), now.minusYears(-periodIndex).getMonth()).lengthOfMonth();
                endDate = now.minusYears(-periodIndex).withDayOfMonth(lengthOfMonth).withHour(23).withMinute(59).withSecond(59);
                outcome = getDataFromDatabase(span, startDate, endDate, graphType);
                return createResultArrayForYear(outcome, result, endDate);
            default:
                return null;
        }
    }

    /**
     * A map to manage the spanTypes and the corresponding array length of the result
     */
    private Map<SpanType, Integer> createSpanMap(Integer lengthOfMonth) {
        Map<SpanType, Integer> spanMap = new HashMap<>();
        spanMap.put(SpanType.DAY, 24);
        spanMap.put(SpanType.WEEK, 7);
        spanMap.put(SpanType.MONTH, lengthOfMonth);
        spanMap.put(SpanType.QUARTER, 12);
        spanMap.put(SpanType.YEAR, 12);
        return spanMap;
    }

    /**
     * Handles the Repository calls depending on the graphType for the span "day"
     *
     * @param startDate The startDate of which the data should be fetched
     * @param endDate The endDate of which the data should be fetched
     * @param graphType the type of graph the data should be fetched for (see GraphType.java)
     * @return the return value of the database call
     */
    private List<Map<String, Object>> getDataFromDatabase(SpanType span, ZonedDateTime startDate, ZonedDateTime endDate, GraphType graphType) {
        switch (graphType) {
            case SUBMISSIONS -> {
                return this.statisticsRepository.getTotalSubmissions(startDate, endDate);
            }
            case ACTIVE_USERS -> {
                List<Map<String, Object>> result = this.statisticsRepository.getActiveUsers(startDate, endDate);
                return convertMapList(span, result, startDate, graphType);
            }
            case LOGGED_IN_USERS -> {
                Instant startDateInstant = startDate.toInstant();
                Instant endDateInstant = endDate.toInstant();
                List<Map<String, Object>> result = this.statisticsRepository.getLoggedInUsers(startDateInstant, endDateInstant);
                return convertMapList(span, result, startDate, graphType);
            }
            case RELEASED_EXERCISES -> {
                return this.statisticsRepository.getReleasedExercises(startDate, endDate);
            }
            case EXERCISES_DUE -> {
                return this.statisticsRepository.getExercisesDue(startDate, endDate);
            }
            case CONDUCTED_EXAMS -> {
                return this.statisticsRepository.getConductedExams(startDate, endDate);
            }
            case EXAM_PARTICIPATIONS -> {
                return this.statisticsRepository.getExamParticipations(startDate, endDate);
            }
            case EXAM_REGISTRATIONS -> {
                return this.statisticsRepository.getExamRegistrations(startDate, endDate);
            }
            case ACTIVE_TUTORS -> {
                List<Map<String, Object>> result = this.statisticsRepository.getActiveTutors(startDate, endDate);
                return convertMapList(span, result, startDate, graphType);
            }
            case CREATED_RESULTS -> {
                return this.statisticsRepository.getCreatedResults(startDate, endDate);
            }
            case CREATED_FEEDBACKS -> {
                return this.statisticsRepository.getResultFeedbacks(startDate, endDate);
            }
            default -> {
                return new ArrayList<>();
            }
        }
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This method handles the spanType DAY
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param result the array in which the converted outcome should be inserted
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArrayForDay(List<Map<String, Object>> outcome, Integer[] result, ZonedDateTime endDate) {
        for (Map<String, Object> map : outcome) {
            int hour = ((ZonedDateTime) map.get("day")).getHour();
            Integer amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            for (int i = 0; i < 24; i++) {
                if (hour == endDate.minusHours(i).getHour()) {
                    result[endDate.getHour() - i] += amount;
                }
            }
        }
        return result;
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This method handles the spanType WEEK
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param result the array in which the converted outcome should be inserted
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArrayForWeek(List<Map<String, Object>> outcome, Integer[] result, ZonedDateTime endDate) {
        for (Map<String, Object> map : outcome) {
            ZonedDateTime date = (ZonedDateTime) map.get("day");
            Integer amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            for (int i = 0; i < 7; i++) {
                if (date.getDayOfMonth() == endDate.minusDays(i).getDayOfMonth()) {
                    result[6 - i] += amount;
                }
            }
        }
        return result;
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This method handles the spanType MONTH
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param result the array in which the converted outcome should be inserted
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArrayForMonth(List<Map<String, Object>> outcome, Integer[] result, ZonedDateTime endDate) {
        for (Map<String, Object> map : outcome) {
            ZonedDateTime date = (ZonedDateTime) map.get("day");
            Integer amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            for (int i = 0; i < result.length; i++) {
                if (date.getDayOfMonth() == endDate.minusDays(i).getDayOfMonth()) {
                    result[result.length - 1 - i] += amount;
                }
            }
        }
        return result;
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This method handles the spanType WEEKS_ORDERED
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param result the array in which the converted outcome should be inserted
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArrayForQuarter(List<Map<String, Object>> outcome, Integer[] result, ZonedDateTime endDate) {
        int week;
        for (Map<String, Object> map : outcome) {
            ZonedDateTime date = (ZonedDateTime) map.get("day");
            Integer amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            week = getWeekOfDate(date);
            for (int i = 0; i < result.length; i++) {
                if (week == getWeekOfDate(endDate.minusWeeks(i))) {
                    result[result.length - 1 - i] += amount;
                }
            }
        }
        return result;
    }

    /**
     * Gets a list of maps, each map describing an entry in the database. The map has the two keys "day" and "amount",
     * which map to the date and the amount of the findings. This method handles the spanType YEAR
     *
     * @param outcome A List<Map<String, Object>>, containing the content which should be refactored into an array
     * @param result the array in which the converted outcome should be inserted
     * @param endDate the endDate
     * @return an array, containing the values for each bar in the graph
     */
    private Integer[] createResultArrayForYear(List<Map<String, Object>> outcome, Integer[] result, ZonedDateTime endDate) {
        for (Map<String, Object> map : outcome) {
            ZonedDateTime date = (ZonedDateTime) map.get("day");
            Integer amount = map.get("amount") != null ? ((Long) map.get("amount")).intValue() : 0;
            for (int i = 0; i < 12; i++) {
                if (date.getMonth() == endDate.minusMonths(i).getMonth()) {
                    result[11 - i] += amount;
                }
            }
        }
        return result;
    }

    /**
    * This method handles the duplicity of usernames in the active user call. It gets a List<Map<String, Object>> analogue to previous methods, but instead of numbers in an amount key,
    * it contains a username key with the actual username as value. It then handles all the usernames and returns a List<Map<String, Object>>, but now with the the key "amount"
    * and value the number of users in this interval
    *
    * @param span DAY,WEEK,MONTH or YEAR
    * @param result the result given by the Repository call
    * @param startDate the startDate of the period
    * @return A List<Map<String, Object>> analogue to other database calls
    */
    private List<Map<String, Object>> convertMapList(SpanType span, List<Map<String, Object>> result, ZonedDateTime startDate, GraphType graphType) {
        Map<Object, List<String>> users = new HashMap<>();
        for (Map<String, Object> listElement : result) {
            Object index;
            ZonedDateTime date;
            if (graphType == GraphType.LOGGED_IN_USERS) {
                Instant instant = (Instant) listElement.get("day");
                date = instant.atZone(startDate.getZone());
            }
            else {
                date = (ZonedDateTime) listElement.get("day");
            }
            if (span == SpanType.DAY) {
                index = date.getHour();
            }
            else if (span == SpanType.WEEK || span == SpanType.MONTH) {
                index = date.getDayOfMonth();
            }
            else if (span == SpanType.QUARTER) {
                index = getWeekOfDate(date);
            }
            else {
                index = date.getMonth();
            }
            String username = listElement.get("username").toString();
            List<String> usersInSameSlot = users.get(index);
            // if this index is not yet existing in users
            if (usersInSameSlot == null) {
                usersInSameSlot = new ArrayList<>();
                usersInSameSlot.add(username);
                users.put(index, usersInSameSlot);
            }   // if the value of the map for this index does not contain this username
            else if (!usersInSameSlot.contains(username)) {
                usersInSameSlot.add(username);
            }
        }
        return fillMapList(users, span, startDate);
    }

    /**
     * Helper class for the ConvertMapList method, which takes the users in the same timeslot as well as some attributed needed
     * for calculation to convert these into a Map List which is then returned
     */
    private List<Map<String, Object>> fillMapList(Map<Object, List<String>> users, SpanType span, ZonedDateTime startDate) {
        List<Map<String, Object>> returnList = new ArrayList<>();
        users.forEach((k, v) -> {
            Object start;
            if (span == SpanType.DAY) {
                start = startDate.withHour((Integer) k);
            }
            else if (span == SpanType.WEEK || span == SpanType.MONTH) {
                start = startDate.withDayOfMonth((Integer) k);
            }
            else if (span == SpanType.QUARTER) {
                int year = (Integer) k < getWeekOfDate(startDate) ? startDate.getYear() + 1 : startDate.getYear();
                ZonedDateTime firstDateOfYear = ZonedDateTime.of(year, 1, 1, 0, 0, 0, 0, startDate.getZone());
                start = getWeekOfDate(firstDateOfYear) == 1 ? firstDateOfYear.plusWeeks(((Integer) k) - 1) : firstDateOfYear.plusWeeks((Integer) k);
            }
            else {
                start = startDate.withMonth(getMonthIndex((Month) k));
            }
            Map<String, Object> listElement = new HashMap<>();
            listElement.put("day", start);
            listElement.put("amount", (long) v.size());
            returnList.add(listElement);
        });
        return returnList;
    }

    private Integer getMonthIndex(Month month) {
        return switch (month) {
            case JANUARY -> 1;
            case FEBRUARY -> 2;
            case MARCH -> 3;
            case APRIL -> 4;
            case MAY -> 5;
            case JUNE -> 6;
            case JULY -> 7;
            case AUGUST -> 8;
            case SEPTEMBER -> 9;
            case OCTOBER -> 10;
            case NOVEMBER -> 11;
            case DECEMBER -> 12;
        };
    }

    private Integer getWeekOfDate(ZonedDateTime date) {
        LocalDate localDate = date.toLocalDate();
        TemporalField woy = WeekFields.of(DayOfWeek.MONDAY, 4).weekOfWeekBasedYear();
        return localDate.get(woy);
    }
}
