package ${packageName};

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 2.0 (24.02.2019)
 *
 * This test evaluates if the specified constructors in the structure oracle
 * are correctly implemented with the expected parameter types
 * (in case these are specified).
 */
@RunWith(Parameterized.class)
public class ConstructorTest extends StructuralTest {

    public ConstructorTest(String expectedClassName, String expectedPackageName, JSONObject expectedClassJSON) {
        super(expectedClassName, expectedPackageName, expectedClassJSON);
    }

    /**
     * This method collects the classes in the structure oracle file for which constructors are specified.
     * These classes are packed into a list, which represents the test data.
     * @return A list of arrays containing each class' name, package and the respective JSON object defined in the structure oracle.
     */
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> findClasses() {
        List<Object[]> testData = new ArrayList<Object[]>();

        for (int i = 0; i < structureOracleJSON.length(); i++) {
            JSONObject expectedClassJSON = structureOracleJSON.getJSONObject(i);

            // Only test the constructors if they are specified in the structure diff
            if (expectedClassJSON.has("class") && expectedClassJSON.has("constructors")) {
                JSONObject expectedClassPropertiesJSON = expectedClassJSON.getJSONObject("class");
                String expectedClassName = expectedClassPropertiesJSON.getString("name");
                String expectedPackageName = expectedClassPropertiesJSON.getString("package");
                testData.add(new Object[] { expectedClassName, expectedPackageName, expectedClassJSON });
            }
        }
        return testData;
    }

    /**
     * This test loops over the list of the test data generated by the method findClasses(), checks if each class is found
     * at all in the assignment and then proceeds to check its constructors.
     */
    @Test(timeout = 1000)
    public void testConstructors() {
        Class<?> actualClass = findClassForTestType("constructor");

        if (expectedClassJSON.has("constructors")) {
            JSONArray expectedConstructors = expectedClassJSON.getJSONArray("constructors");

            checkConstructors(actualClass, expectedConstructors);
        }
    }

    /**
     * This method checks if a observed class' constructors match the expected ones defined in the structure oracle.
     * @param observedClass: The class that needs to be checked as a Class object.
     * @param expectedConstructors: The information on the expected constructors contained in a JSON array. This information consists
     * of the parameter types and the visibility modifiers.
     */
    private void checkConstructors(Class<?> observedClass, JSONArray expectedConstructors) {
        for (int i = 0; i < expectedConstructors.length(); i++) {
            JSONObject expectedConstructor = expectedConstructors.getJSONObject(i);
            JSONArray expectedParameters = expectedConstructor.getJSONArray("parameters");
            JSONArray expectedModifiers = expectedConstructor.getJSONArray("modifiers");

            boolean parametersAreRight = false;
            boolean modifiersAreRight = false;

            for (Constructor<?> observedConstructor : observedClass.getDeclaredConstructors()) {
                Class<?>[] observedParameters = observedConstructor.getParameterTypes();
                String[] observedModifiers = Modifier.toString(observedConstructor.getModifiers()).split(" ");

                // First check the parameters
                parametersAreRight = checkParameters(observedParameters, expectedParameters);

                // Then the modifiers
                modifiersAreRight = checkModifiers(observedModifiers, expectedModifiers);

                // If both are correct, then we found our constructor and we can break the loop
                if (parametersAreRight && modifiersAreRight) {
                    break;
                }
            }

            String expectedConstructorInformation = "the expected constructor of the class '" + expectedClassName + "' with "
                + ((expectedParameters.length() == 0) ? "no parameters" : "the parameters: " + expectedParameters.toString());

            assertTrue("Problem: the parameters of " + expectedConstructorInformation + " are not implemented as expected.",
                parametersAreRight);

            assertTrue("Problem: the access modifiers of " + expectedConstructorInformation + " are not implemented as expected.",
                modifiersAreRight);

            assertTrue("Problem: the constructor of the class " + expectedClassName + " is not implemented as expected.",
                parametersAreRight && modifiersAreRight);
        }
    }

}
