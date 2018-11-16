package de.tum.in.www1.artemis.config;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Application constants.
 */
public final class Constants {

    // Regex for acceptable logins
    public static final String LOGIN_REGEX = "^[_'.@A-Za-z0-9-]*$";

    public static final String SYSTEM_ACCOUNT = "system";
    public static final String ANONYMOUS_USER = "anonymoususer";
    public static final String DEFAULT_LANGUAGE = "en";

    public static final int QUIZ_GRACE_PERIOD_IN_SECONDS = 2;

    public static final String TEMP_FILEPATH = "uploads" + File.separator + "images" + File.separator + "temp" + File.separator;
    public static final String DRAG_AND_DROP_BACKGROUND_FILEPATH = "uploads" + File.separator + "images" + File.separator + "drag-and-drop" + File.separator + "backgrounds" + File.separator;
    public static final String DRAG_ITEM_FILEPATH = "uploads" + File.separator + "images" + File.separator + "drag-and-drop" + File.separator + "drag-items" + File.separator;
    public static final String FILEPATH_ID_PLACHEOLDER = "PLACEHOLDER_FOR_ID";
    public static final String FILEPATH_COMPASS = "compass";

    public static final String RESULT_RESOURCE_API_PATH = "/api/results/";
    public static final String PROGRAMMING_SUBMISSION_RESOURCE_API_PATH = "/api/programming-submissions/";
    public static final String TEST_CASE_CHANGED_API_PATH = "/api/programming-exercises/test-cases-changed/";

    public static final String shortNameRegex = "^[a-zA-Z][a-zA-Z0-9]*";
    public static final Pattern shortNamePattern = Pattern.compile(shortNameRegex);

    private Constants() {
    }
}
