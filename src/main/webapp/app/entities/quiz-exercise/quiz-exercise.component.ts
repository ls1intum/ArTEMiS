import { Component, Input } from '@angular/core';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { JhiAlertService, JhiEventManager } from 'ng-jhipster';

import { QuizExercise } from './quiz-exercise.model';
import { QuizExerciseService } from './quiz-exercise.service';
import { AccountService } from '../../core';
import { ActivatedRoute } from '@angular/router';
import * as moment from 'moment';
import { CourseService } from '../course';
import { ExerciseComponent } from 'app/entities/exercise/exercise.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'jhi-quiz-exercise',
    templateUrl: './quiz-exercise.component.html',
})
export class QuizExerciseComponent extends ExerciseComponent {
    QuizStatus = {
        HIDDEN: 'Hidden',
        VISIBLE: 'Visible',
        ACTIVE: 'Active',
        CLOSED: 'Closed',
        OPEN_FOR_PRACTICE: 'Open for Practice',
    };

    @Input() quizExercises: QuizExercise[] = [];

    constructor(
        private quizExerciseService: QuizExerciseService,
        private jhiAlertService: JhiAlertService,
        private accountService: AccountService,
        courseService: CourseService,
        translateService: TranslateService,
        eventManager: JhiEventManager,
        route: ActivatedRoute,
    ) {
        super(courseService, translateService, route, eventManager);
    }

    protected loadExercises(): void {
        this.quizExerciseService.findForCourse(this.courseId).subscribe(
            (res: HttpResponse<QuizExercise[]>) => {
                this.quizExercises = res.body!;
                // reconnect exercise with course
                this.quizExercises.forEach(exercise => {
                    exercise.course = this.course;
                    exercise.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(exercise.course);
                    exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(exercise.course);
                });
                this.emitExerciseCount(this.quizExercises.length);
                this.setQuizExercisesStatus();
            },
            (res: HttpErrorResponse) => this.onError(res),
        );
    }

    trackId(index: number, item: QuizExercise) {
        return item.id;
    }

    protected getChangeEventName(): string {
        return 'quizExerciseListModification';
    }

    private onError(error: HttpErrorResponse) {
        this.jhiAlertService.error(error.message);
    }

    /**
     * Checks if the quiz exercise is over
     * @param quizExercise The quiz exercise we want to know if it's over
     * @returns {boolean} true if the quiz exercise is over, false if not.
     */
    quizIsOver(quizExercise: QuizExercise) {
        if (quizExercise.isPlannedToStart) {
            const plannedEndMoment = moment(quizExercise.releaseDate!).add(quizExercise.duration, 'seconds');
            return plannedEndMoment.isBefore(moment());
            // the quiz is over
        }
        // the quiz hasn't started yet
        return false;
    }

    /**
     * Convert seconds to full minutes
     * @param seconds {number} the number of seconds
     * @returns {number} the number of full minutes
     */
    fullMinutesForSeconds(seconds: number) {
        return Math.floor(seconds / 60);
    }

    /**
     * Set the quiz open for practice
     *
     * @param quizExerciseId the quiz exercise id to start
     */
    openForPractice(quizExerciseId: number) {
        this.quizExerciseService.openForPractice(quizExerciseId).subscribe(
            () => {
                this.loadOne(quizExerciseId);
            },
            (res: HttpErrorResponse) => {
                this.onError(res);
                this.loadOne(quizExerciseId);
            },
        );
    }

    setQuizExercisesStatus() {
        this.quizExercises.forEach(quizExercise => (quizExercise.status = this.quizExerciseService.statusForQuiz(quizExercise)));
    }

    /**
     * Start the given quiz-exercise immediately
     *
     * @param quizExerciseId the quiz exercise id to start
     */
    startQuiz(quizExerciseId: number) {
        this.quizExerciseService.start(quizExerciseId).subscribe(
            () => {
                this.loadOne(quizExerciseId);
            },
            (res: HttpErrorResponse) => {
                this.onError(res);
                this.loadOne(quizExerciseId);
            },
        );
    }

    /**
     * Do not load all quizExercise if only one has changed
     *
     * @param quizExerciseId
     */
    private loadOne(quizExerciseId: number) {
        this.quizExerciseService.find(quizExerciseId).subscribe((res: HttpResponse<QuizExercise>) => {
            const index = this.quizExercises.findIndex(quizExercise => quizExercise.id === quizExerciseId);
            const exercise = res.body!;
            exercise.isAtLeastTutor = this.accountService.isAtLeastTutorInCourse(exercise.course!);
            exercise.isAtLeastInstructor = this.accountService.isAtLeastInstructorInCourse(exercise.course!);
            exercise.status = this.quizExerciseService.statusForQuiz(exercise);
            if (index === -1) {
                this.quizExercises.push(exercise);
            } else {
                this.quizExercises[index] = exercise;
            }
        });
    }

    /**
     * Exports questions for the given quiz exercise in json file
     * @param quizExerciseId The quiz exercise id we want to export
     * @param exportAll If true exports all questions, else exports only those whose export flag is true
     */
    exportQuizById(quizExerciseId: number, exportAll: boolean) {
        this.quizExerciseService.find(quizExerciseId).subscribe((res: HttpResponse<QuizExercise>) => {
            const exercise = res.body!;
            this.quizExerciseService.exportQuiz(exercise.quizQuestions, exportAll);
        });
    }

    /**
     * Make the given quiz-exercise visible to students
     *
     * @param quizExerciseId the quiz exercise id to start
     */
    showQuiz(quizExerciseId: number) {
        this.quizExerciseService.setVisible(quizExerciseId).subscribe(
            () => {
                this.loadOne(quizExerciseId);
            },
            (res: HttpErrorResponse) => {
                this.onError(res);
                this.loadOne(quizExerciseId);
            },
        );
    }

    callback() {}
}
