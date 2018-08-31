import { BaseEntity } from './../../shared';
import { Course } from '../course';
import { Participation } from '../participation';

export const enum DifficultyLevel {
    EASY = 'EASY',
    MEDIUM = 'MEDIUM',
    HARD = 'HARD'
}

// IMPORTANT NOTICE: The following strings have to be consistent with the ones defined in Exercise.java
export const enum ExerciseType {
    PROGRAMMING = 'programming',
    MODELING = 'modeling',
    QUIZ = 'quiz',
    TEXT = 'text',
    FILE_UPLOAD = 'file-upload'
}

export const enum ParticipationStatus {
    QUIZ_UNINITIALIZED = 'quiz-uninitialized',
    QUIZ_ACTIVE = 'quiz-active',
    QUIZ_SUBMITTED = 'quiz-submitted',
    QUIZ_NOT_STARTED = 'quiz-not-started',
    QUIZ_NOT_PARTICIPATED = 'quiz-not-participated',
    QUIZ_FINISHED = 'quiz-finished',
    MODELING_EXERCISE = 'modeling-exercise',
    UNINITIALIZED = 'uninitialized',
    INITIALIZED = 'initialized',
    INACTIVE = 'inactive'
}

export abstract class Exercise implements BaseEntity {

    public id: number;
    public problemStatement: string;
    public gradingInstructions: string;
    public title: string;
    public releaseDate: any;
    public dueDate: any;
    public maxScore: number;
    public difficulty: DifficultyLevel;
    public categories: string[];
    public participations: Participation[];
    public course: Course;
    public openForSubmission: boolean;
    public participationStatus: ParticipationStatus;
    public loading: boolean;
    public isAtLeastTutor: boolean;
    public type: ExerciseType;

    constructor(type: ExerciseType) {
        this.type = type;
    }
}
