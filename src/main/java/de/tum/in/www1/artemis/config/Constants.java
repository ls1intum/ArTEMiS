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

    public static final String DRAG_AND_DROP_BACKGROUND_FILEPATH = "uploads" + File.separator + "images" + File.separator + "drag-and-drop" + File.separator + "backgrounds"
            + File.separator;

    public static final String DRAG_ITEM_FILEPATH = "uploads" + File.separator + "images" + File.separator + "drag-and-drop" + File.separator + "drag-items" + File.separator;

    public static final String COURSE_ICON_FILEPATH = "uploads" + File.separator + "images" + File.separator + "course" + File.separator + "icons" + File.separator;

    public static final String LECTURE_ATTACHMENT_FILEPATH = "uploads" + File.separator + "attachments" + File.separator + "lecture" + File.separator;

    public static final String FILEPATH_ID_PLACHEOLDER = "PLACEHOLDER_FOR_ID";

    public static final String FILEPATH_COMPASS = "compass";

    public static final String RESULT_RESOURCE_API_PATH = "/api/results/";

    public static final String NEW_RESULT_RESOURCE_PATH = "/programming-exercises/new-result";

    public static final String NEW_RESULT_RESOURCE_API_PATH = "/api" + NEW_RESULT_RESOURCE_PATH;

    public static final String TEST_CASE_CHANGED_PATH = "/programming-exercises/test-cases-changed/";

    public static final String TEST_CASE_CHANGED_API_PATH = "/api" + TEST_CASE_CHANGED_PATH;

    public static final String PROGRAMMING_SUBMISSION_RESOURCE_PATH = "/programming-submissions/";

    public static final String PROGRAMMING_SUBMISSION_RESOURCE_API_PATH = "/api" + PROGRAMMING_SUBMISSION_RESOURCE_PATH;

    public static final String SHORT_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9]*";

    public static final Pattern SHORT_NAME_PATTERN = Pattern.compile(SHORT_NAME_REGEX);

    public static final Pattern TITLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]*");

    public static final double COMPASS_SCORE_EQUALITY_THRESHOLD = 0.0001;

    // NOTE: the following values for programming exercises are hard-coded at the moment
    public static final String TEST_REPO_NAME = "tests";

    public static final String ASSIGNMENT_REPO_NAME = "Assignment";

    public static final String ASSIGNMENT_REPO_PATH = "assignment";

    public static final long MAX_COMPLAINT_NUMBER_PER_STUDENT = 3;

    public static final long MAX_NUMBER_OF_LOCKED_SUBMISSIONS_PER_TUTOR = 10;

    private Constants() {
    }
}
