package ${packageName};

import static org.junit.Assert.*;

import java.util.List;
import java.lang.reflect.InvocationTargetException;
import java.text.*;
import java.util.*;

import org.junit.*;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 2.0 (24.02.2019)
 */
public class SortingExampleBehaviorTest extends BehaviorTest {

    private List<Date> dates;
    private List<Date> datesWithCorrectOrder;

    @Before
    public void setup() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        Date date1 = dateFormat.parse("08.11.2018");
        Date date2 = dateFormat.parse("15.04.2017");
        Date date3 = dateFormat.parse("15.02.2016");
        Date date4 = dateFormat.parse("15.09.2017");

        this.dates = Arrays.asList(date1, date2, date3, date4);
        this.datesWithCorrectOrder = Arrays.asList(date3, date2, date4, date1);
    }

    @Test(timeout = 1000)
    public void testBubbleSort() {
        BubbleSort bubbleSort = new BubbleSort();
        bubbleSort.performSort(dates);
        assertEquals("Problem: BubbleSort does not sort correctly", datesWithCorrectOrder, dates);
    }

    @Test(timeout = 1000)
    public void testMergeSort() {
        MergeSort mergeSort = new MergeSort();
        mergeSort.performSort(dates);
        assertEquals("Problem: MergeSort does not sort correctly", datesWithCorrectOrder, dates);
    }

    @Test(timeout = 1000)
    public void testUseMergeSortForBigList() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException, ClassNotFoundException {
        List<Date> bigList = new ArrayList<Date>();
        for (int i = 0; i < 11; i++) {
            bigList.add(new Date());
        }
        Object chosenSortStrategy = configurePolicyAndContext(bigList);
        assertTrue("Problem: The sort algorithm of Context was not MergeSort for a list with more than 10 dates.", chosenSortStrategy instanceof MergeSort);
    }

    @Test(timeout = 1000)
    public void testUseBubbleSortForSmallList()  throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException, ClassNotFoundException {
        List<Date> smallList = new ArrayList<Date>();
        for (int i = 0; i < 3; i++) {
            smallList.add(new Date());
        }
        Object chosenSortStrategy = configurePolicyAndContext(smallList);
        assertTrue("Problem: The sort algorithm of Context was not BubbleSort for a list with less or equal than 10 dates.", chosenSortStrategy instanceof BubbleSort);
    }

    private Object configurePolicyAndContext(List<Date> dates) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, InstantiationException, ClassNotFoundException {

        Object context = newInstance("${packageName}.Context");
        invokeMethod(context, getMethod(context, "setDates", List.class), dates);

        Object policy = newInstance("${packageName}.Policy", context);
        invokeMethod(policy, getMethod(policy, "configure"));

        Object chosenSortStrategy = invokeMethod(context, getMethod(context, "getSortAlgorithm"));

        return chosenSortStrategy;
    }
}
