import { Injectable } from '@angular/core';
import { WindowRef } from 'app/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { IntelliJState, JavaDowncallBridge, JavaUpcallBridge } from 'app/intellij/intellij';

/**
 * This is the main interface between an IDE (e.g. IntelliJ) and this webapp. If a student has the Orion plugin
 * installed (https://github.com/ls1intum/Orion), this service will be used for communication between the
 * IDE Artemis itself.
 *
 * The communication itself is bidirectional, meaning that the IDE can call Typescript code
 * using the by the JavaUpcallBridge interface defined methods. On the other side, the Angular app can execute
 * Kotlin/Java coode inside the IDE by calling the JavaDowncallBridge interface.
 *
 * In order to be always available, it is essential that this service gets instantiated right after loading the app
 * in the browser. This service has to always be available in the native window object, so that if an IDE is connected,
 * it can find the object during the integrated IDE browser instantiation.
 */
@Injectable({
    providedIn: 'root',
})
export class JavaBridgeService implements JavaDowncallBridge, JavaUpcallBridge {
    private intellijState: IntelliJState;
    private intellijStateSubject: BehaviorSubject<IntelliJState>;

    constructor(private window: WindowRef) {}

    static initBridge(bridge: JavaBridgeService, win: WindowRef) {
        win.nativeWindow.javaDowncallBridge = bridge;
        bridge.intellijState = { opened: -1, cloning: false, building: false };
        bridge.intellijStateSubject = new BehaviorSubject<IntelliJState>(bridge.intellijState);
    }

    /**
     * Login to the Git client within the IDE with the same credentials as used for Artemis
     *
     * @param username The username of the current user
     * @param password The password of the current user. This is stored safely in the IDE's password safe
     */
    login(username: string, password: string) {
        this.window.nativeWindow.intellij.login(username, password);
    }

    /**
     * "Imports" a project/exercise by cloning th repository on the local machine of the user and opening the new project.
     *
     * @param repository The full URL of the repository of a programming exercise
     * @param exerciseName The name of the programming exercise
     * @param exerciseId The ID of the exercise
     * @param courseId THe ID of the course of the exercise
     */
    clone(repository: string, exerciseName: string, exerciseId: number, courseId: number) {
        this.window.nativeWindow.intellij.clone(repository, exerciseName, exerciseId, courseId);
    }

    /**
     * Submits all changes on the local machine of the user by staging and committing every file. Afterwards, all commits
     * get pushed to the remote master branch
     */
    submit() {
        this.window.nativeWindow.intellij.addCommitAndPushAllChanges();
    }

    /**
     * Get the state object of the IDE. The IDE sets different internal states in this object, e.g. the ID of the currently
     * opened exercise
     *
     * @return An observable containing the internal state of the IDE
     */
    state(): Observable<IntelliJState> {
        return this.intellijStateSubject;
    }

    /**
     * Logs a message to the debug console of the opened IDE
     *
     * @param message The message to log in the development environment
     */
    log(message: string) {
        this.window.nativeWindow.intellij.log(message);
    }

    /**
     * Gets called by the IDE. Informs the Angular app about a newly opened exercise.
     *
     * @param opened The ID of the exercise that was opened by the user.
     */
    onExerciseOpened(opened: number): void {
        this.setIDEStateParameter({ opened });
    }

    /**
     * Notify the IDE that a new build has started
     */
    onBuildStarted() {
        this.window.nativeWindow.intellij.onBuildStarted();
    }

    /**
     * Notify the IDE that a build finished and all results have been sent
     */
    onBuildFinished() {
        this.window.nativeWindow.intellij.onBuildFinished();
    }

    /**
     * Notify the IDE that a build failed. Alternative to onBuildFinished
     *
     * @param message The message containing all compile errors for the current build
     */
    onBuildFailed(message: string) {
        this.window.nativeWindow.intellij.onBuildFailed(message);
    }

    /**
     * Notifies the IDE about a completed test (both positive or negative). In the case of an error,
     * you can also send a message containing some information about why the test failed.
     *
     * @param success True if the test was successful, false otherwise
     * @param message A detail message explaining the test result
     */
    onTestResult(success: boolean, message: string) {
        this.window.nativeWindow.intellij.onTestResult(success, message);
    }

    /**
     * Notifies Artemis if the IDE is currently building (and testing) the checked out exercise
     *
     * @param building True, a building process is currently open, false otherwise
     */
    isBuilding(building: boolean): void {
        this.setIDEStateParameter({ building });
    }

    /**
     * Notifies Artemis if the IDE is in the process of importing (i.e. cloning) an exercise)
     *
     * @param cloning True, if there is a open clone process, false otherwise
     */
    isCloning(cloning: boolean): void {
        this.setIDEStateParameter({ cloning });
    }

    private setIDEStateParameter(patch: Partial<IntelliJState>) {
        Object.assign(this.intellijState, patch);
        this.intellijStateSubject.next(this.intellijState);
    }
}
