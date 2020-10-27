package ${packageName};

import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

/**
 * @author Stephan Krusche (krusche@in.tum.de)
 * @version 3.0 (25.09.2019)
 * <br><br>
 * This test evaluates if the specified methods in the structure oracle are correctly implemented with the expected name, return type, parameter types, visibility modifiers
 * and annotations, based on its definition in the structure oracle (test.json)
 */
public class MethodTest extends StructuralTest {

    /**
     * This method collects the classes in the structure oracle file for which methods are specified.
     * These classes are packed into a list, which represents the test data.
     * @return A list of arrays containing each class' name, package and the respective JSON object defined in the structure oracle.
     */
    @TestFactory
    public DynamicContainer findClasses() throws URISyntaxException {
        List<DynamicNode> tests = new ArrayList<>();

        if (structureOracleJSON == null) {
            fail("The MethodTest test can only run if the structural oracle (test.json) is present. If you do not provide it, delete MethodTest.java!");
        }

        for (int i = 0; i < structureOracleJSON.length(); i++) {
            JSONObject expectedClassJSON = structureOracleJSON.getJSONObject(i);

            // Only test the classes that have methods defined in the structure oracle.
            if (expectedClassJSON.has(JSON_PROPERTY_CLASS) && expectedClassJSON.has(JSON_PROPERTY_METHODS)) {
                JSONObject expectedClassPropertiesJSON = expectedClassJSON.getJSONObject(JSON_PROPERTY_CLASS);
                String expectedClassName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_NAME);
                String expectedPackageName = expectedClassPropertiesJSON.getString(JSON_PROPERTY_PACKAGE);
                ExpectedClassStructure expectedClassStructure = new ExpectedClassStructure(expectedClassName, expectedPackageName, expectedClassJSON);
                tests.add(dynamicTest("testMethods[" + expectedClassName + "]", () -> testMethods(expectedClassStructure)));
            }
        }
        if (tests.size() == 0) {
            fail("No tests for methods available in the structural oracle (test.json). Either provide attributes information or delete MethodTest.java!");
        }
        // Using a custom URI here to workaround surefire rendering the JUnit XML without the correct test names.
        return dynamicContainer(getClass().getName(), new URI(getClass().getName()), tests.stream());
    }

    /**
     * This test loops over the list of the test data generated by the method findClasses(), checks if each class is found
     * at all in the assignment and then proceeds to check its methods.
     * @param expectedClassStructure: The class structure that we expect to find and test against.
     */
    public void testMethods(ExpectedClassStructure expectedClassStructure) {
        String expectedClassName = expectedClassStructure.getExpectedClassName();
        Class<?> observedClass = findClassForTestType(expectedClassStructure, "method");

        if (expectedClassStructure.hasProperty(JSON_PROPERTY_METHODS)) {
            JSONArray methodsJSON = expectedClassStructure.getPropertyAsJsonArray(JSON_PROPERTY_METHODS);
            checkMethods(expectedClassName, observedClass, methodsJSON);
        }
    }

    /**
     * This method checks if a observed class' methods match the expected ones defined in the structure oracle.
     * @param expectedClassName: The simple name of the class, mainly used for error messages.
     * @param observedClass: The class that needs to be checked as a Class object.
     * @param expectedMethods: The information on the expected methods contained in a JSON array. This information consists
     * of the name, parameter types, return type and the visibility modifiers of each method.
     */
    private void checkMethods(String expectedClassName, Class<?> observedClass, JSONArray expectedMethods) {
        for(int i = 0; i < expectedMethods.length(); i++) {
            JSONObject expectedMethod = expectedMethods.getJSONObject(i);
            String expectedName = expectedMethod.getString(JSON_PROPERTY_NAME);
            JSONArray expectedParameters = expectedMethod.has(JSON_PROPERTY_PARAMETERS) ? expectedMethod.getJSONArray(JSON_PROPERTY_PARAMETERS) : new JSONArray();
            JSONArray expectedModifiers = expectedMethod.has(JSON_PROPERTY_MODIFIERS) ? expectedMethod.getJSONArray(JSON_PROPERTY_MODIFIERS) : new JSONArray();
            JSONArray expectedAnnotations = expectedMethod.has(JSON_PROPERTY_ANNOTATIONS) ? expectedMethod.getJSONArray(JSON_PROPERTY_ANNOTATIONS) : new JSONArray();
            String expectedReturnType = expectedMethod.getString(JSON_PROPERTY_RETURN_TYPE);

            boolean nameIsRight = false;
            boolean parametersAreRight = false;
            boolean modifiersAreRight = false;
            boolean returnTypeIsRight = false;
            boolean annotationsAreRight = false;

            for(Method observedMethod : observedClass.getDeclaredMethods()) {
                String observedName = observedMethod.getName();
                Class<?>[] observedParameters = observedMethod.getParameterTypes();
                String[] observedModifiers = Modifier.toString(observedMethod.getModifiers()).split(" ");
                String observedReturntype = observedMethod.getReturnType().getSimpleName();
                Annotation[] observedAnnotations = observedMethod.getAnnotations();

                // If the names don't match, then proceed to the next observed method
                if(!expectedName.equals(observedName)) {
                    //TODO: we should also take wrong case and typos into account
                    //TODO: check if overloading is supported properly
                    continue;
                } else {
                    nameIsRight = true;
                }

                parametersAreRight = checkParameters(observedParameters, expectedParameters);
                modifiersAreRight = checkModifiers(observedModifiers, expectedModifiers);
                annotationsAreRight = checkAnnotations(observedAnnotations, expectedAnnotations);
                returnTypeIsRight = expectedReturnType.equals(observedReturntype);

                // If all are correct, then we found our method and we can break the loop
                if(nameIsRight && parametersAreRight && modifiersAreRight && annotationsAreRight && returnTypeIsRight) {
                    break;
                }
            }

            String expectedMethodInformation = "the expected method '" + expectedName + "' of the class '" + expectedClassName + "' with "
                + ((expectedParameters.length() == 0) ? "no parameters" : "the parameters: " + expectedParameters.toString());

            if (!nameIsRight) {
                fail(expectedMethodInformation + " was not found or is named wrongly.");
            }
            if (!parametersAreRight) {
                fail("The parameters of " + expectedMethodInformation + " are not implemented as expected.");
            }
            if (!modifiersAreRight) {
                fail("The modifiers (access type, abstract, etc.) of " + expectedMethodInformation + " are not implemented as expected.");
            }
            if (!annotationsAreRight) {
                fail("The annotation(s) of " + expectedMethodInformation + " are not implemented as expected.");
            }
            if (!returnTypeIsRight) {
                fail("The return type of " + expectedMethodInformation + " is not implemented as expected.");
            }
            if (!(nameIsRight && parametersAreRight && modifiersAreRight && returnTypeIsRight)) {
                fail("The method '" + expectedName + "' of the class " + expectedClassName + " is not implemented as expected.");
            }
        }
    }
}
