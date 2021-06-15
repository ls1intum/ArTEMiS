import { REPOSITORY } from 'app/exercises/programming/manage/code-editor/code-editor-instructor-base-container.component';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { Observable } from 'rxjs';
import { Annotation } from 'app/exercises/programming/shared/code-editor/ace/code-editor-ace.component';

export interface OrionState {
    opened: number;
    cloning: boolean;
    building: boolean;
    view: ExerciseView;
}

/**
 * Enumeration defining the view options for an exercise,
 * as a student (participation), tutor (assessment), or instructor (to edit the exercise).
 */
export enum ExerciseView {
    STUDENT = 'STUDENT',
    TUTOR = 'TUTOR',
    INSTRUCTOR = 'INSTRUCTOR',
}

export interface ArtemisOrionConnector extends ArtemisClientConnector, OrionConnectorFacade {
    /**
     * Returns an Observable of Orion State.
     */
    state(): Observable<OrionState>;
}

export interface OrionSharedUtilConnector {
    /**
     * Method to perform the login.
     * @param username of the user.
     * @param password of the user.
     */
    login(username: string, password: string): void;

    /**
     * Method to log a specific message.
     * @param message The text to be logged.
     */
    log(message: string): void;
}

export interface OrionExerciseConnector {
    /**
     * Edit an exercise.
     * @param exerciseJson Exercise in a Json string.
     */
    editExercise(exerciseJson: string): void;

    /**
     * Assess an exercise. See {@link OrionConnectorService} for details.
     * @param exerciseJson Exercise in a Json string.
     */
    assessExercise(exerciseJson: string): void;

    /**
     * Downloads a submission into the opened tutor project. See {@link OrionConnectorService} for details.
     * @param submissionId id of the submission, used to navigate to the corresponding URL
     * @param correctionRound correction round, also needed to navigate to the correct URL
     * @param downloadURL URL of the zip file containing the student's repository
     */
    downloadSubmission(submissionId: string, correctionRound: string, downloadURL: string): void;

    /**
     * Import a participation. See {@link OrionConnectorService} for details.
     * @param repository Repository name as string.
     * @param exerciseJson Exercise in a Json string.
     */
    importParticipation(repository: string, exerciseJson: string): void;
}

export interface OrionVCSConnector {
    /**
     * Select a specific repository. See {@link OrionConnectorService} for details.
     * @param repository The repository to be selected.
     */
    selectRepository(repository: REPOSITORY): void;

    /**
     * Code to provide the submit functionality. See {@link OrionConnectorService} for details.
     */
    submit(): void;
}

export interface OrionBuildConnector {
    /**
     * Perform a build and test locally. See {@link OrionConnectorService} for details.
     */
    buildAndTestLocally(): void;

    /**
     * To be executed when build has started. See {@link OrionConnectorService} for details.
     * @param problemStatement The problem statement string.
     */
    onBuildStarted(problemStatement: string): void;

    /**
     * To be executed when build is finished. See {@link OrionConnectorService} for details.
     */
    onBuildFinished(): void;

    /**
     * To be executed when the build failed. See {@link OrionConnectorService} for details.
     * @param buildLogsJsonString The Json string of the build logs.
     */
    onBuildFailed(buildLogsJsonString: string): void;

    /**
     * Executed when the result of the test is out. See {@link OrionConnectorService} for details.
     * @param success Whether the test was successful or not.
     * @param testName The name of the test.
     * @param message The message to display.
     */
    onTestResult(success: boolean, testName: string, message: string): void;
}

export interface OrionConnectorFacade {
    /**
     * Method to perform the login. See {@link OrionConnectorService} for details.
     * @param username of the user.
     * @param password of the user.
     */
    login(username: string, password: string): void;

    /**
     * Method to log a specific message. See {@link OrionConnectorService} for details.
     * @param message The text to be logged.
     */
    log(message: string): void;

    /**
     * Edit a particular exercise. See {@link OrionConnectorService} for details.
     * @param exercise The programming exercise to be edited.
     */
    editExercise(exercise: ProgrammingExercise): void;

    /**
     * Import a specific participation. See {@link OrionConnectorService} for details.
     * @param repositoryUrl The URL of the repository of the participation.
     * @param exercise The programming exercise of the participation.
     */
    importParticipation(repositoryUrl: string, exercise: ProgrammingExercise): void;

    /**
     * Code to provide the submit functionality. See {@link OrionConnectorService} for details.
     */
    submit(): void;

    /**
     * Select a specific repository. See {@link OrionConnectorService} for details.
     * @param repository The repository to be selected.
     */
    selectRepository(repository: REPOSITORY): void;

    /**
     * Perform a build and test locally. See {@link OrionConnectorService} for details.
     */
    buildAndTestLocally(): void;

    /**
     * To be executed when build has started. See {@link OrionConnectorService} for details.
     * @param problemStatement The problem statement string.
     */
    onBuildStarted(problemStatement: string): void;

    /**
     * To be executed when build is finished. See {@link OrionConnectorService} for details.
     */
    onBuildFinished(): void;

    /**
     * To be executed when the build failed. See {@link OrionConnectorService} for details.
     * @param buildErrors All compile errors for the current build
     */
    onBuildFailed(buildErrors: Array<Annotation>): void;

    /**
     * Executed when the result of the test is out. See {@link OrionConnectorService} for details.
     * @param success Whether the test was successful or not.
     * @param testName The name of the test.
     * @param message The message to display.
     */
    onTestResult(success: boolean, testName: string, message: string): void;
}

export interface ArtemisClientConnector {
    /**
     * Executed on exercise opening.
     * @param opened
     * @param view
     */
    onExerciseOpened(opened: number, view: string): void;

    /**
     * Sets the status whether is cloning or not.
     * @param cloning Boolean value specifying whether cloning is active or not.
     */
    isCloning(cloning: boolean): void;

    /**
     * Sets the status whether is building or not.
     * @param building Boolean value specifying whether building is active or not.
     */
    isBuilding(building: boolean): void;

    /**
     * Starts build for an exercise in a course.
     * @param courseId The course id.
     * @param exerciseId The exercise id.
     */
    startedBuildInOrion(courseId: number, exerciseId: number): void;
}

export interface OrionWindow {
    orionExerciseConnector: OrionExerciseConnector;
    orionSharedUtilConnector: OrionSharedUtilConnector;
    orionBuildConnector: OrionBuildConnector;
    orionVCSConnector: OrionVCSConnector;
    artemisClientConnector: ArtemisClientConnector;
}

export const isOrion = window.navigator.userAgent.includes('Orion') || window.navigator.userAgent.includes('IntelliJ');
