import { Question } from '../question';
import { QuizPointStatistic } from '../quiz-point-statistic';
import { Exercise, ExerciseType } from '../exercise';
import { Moment } from 'moment';

export class QuizExercise extends Exercise {

    public id: number;
    public description: string;
    public explanation: string;
    public randomizeQuestionOrder = true;   // default value
    public isVisibleBeforeStart = false;    // default value
    public isOpenForPractice = false;       // default value
    public isPlannedToStart = false;        // default value
    public duration: number;
    public quizPointStatistic: QuizPointStatistic;
    public questions: Question[];
    public status: string;
    public isActiveQuiz = false;            // default value
    public isPracticeModeAvailable = true;  // default value


    //helper attributes
    public adjustedDueDate: Moment;
    public adjustedReleaseDate: Moment;
    public ended: boolean;
    public started: boolean;
    public remainingTime: number;
    public timeUntilPlannedStart: number;

    constructor() {
        super(ExerciseType.QUIZ);
    }
}
