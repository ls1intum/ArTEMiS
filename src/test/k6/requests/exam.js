import {
    EXAMS,
    EXERCISE_GROUPS,
    TEXT_EXERCISE,
    EXAM_STUDENTS,
    GENERATE_STUDENT_EXAMS,
    START_EXERCISES,
    EXAM_CONDUCTION,
    STUDENT_EXAMS,
    STUDENT_EXAM_WORKINGTIME,
    EVALUATE_QUIZ_EXAM,
} from './endpoints.js';
import { nextAlphanumeric } from '../util/utils.js';
import { fail } from 'k6';

export function newExam(artemis, course) {
    const currentDate = new Date();
    const visibleDate = new Date(currentDate.getTime() + 30000); // Visible in 30 secs
    const startDate = new Date(currentDate.getTime() + 60000); // Starting in 30 secs
    const endDate = new Date(currentDate.getTime() + 120000); // Ending in 120 secs

    const exam = {
        course: course,
        visibleDate: visibleDate,
        startDate: startDate,
        endDate: endDate,
        maxPoints: 10,
        numberOfExercisesInExam: 4,
        randomizeExerciseOrder: false,
        started: false,
        title: 'exam' + nextAlphanumeric(5),
        visible: false,
    };

    const res = artemis.post(EXAMS(course.id), exam);
    if (res[0].status !== 201) {
        console.log('ERROR when creating a new exam. Response headers:');
        for (let [key, value] of Object.entries(res[0].headers)) {
            console.log(`${key}: ${value}`);
        }
        fail('ERROR: Could not create exam (status: ' + res[0].status + ')! response: ' + res[0].body);
    }
    console.log('SUCCESS: Generated new exam');

    return JSON.parse(res[0].body);
}

export function newExerciseGroup(artemis, exam, mandatory = true) {
    const exerciseGroup = {
        exam: exam,
        isMandatory: mandatory,
        title: 'group' + nextAlphanumeric(5),
    };

    const res = artemis.post(EXERCISE_GROUPS(exam.course.id, exam.id), exerciseGroup);
    if (res[0].status !== 201) {
        console.log('ERROR when creating a new exercise group. Response headers:');
        for (let [key, value] of Object.entries(res[0].headers)) {
            console.log(`${key}: ${value}`);
        }
        fail('ERROR: Could not create exercise group (status: ' + res[0].status + ')! response: ' + res[0].body);
    }
    console.log('SUCCESS: Generated new exercise group');

    return JSON.parse(res[0].body);
}

export function newTextExercise(artemis, exerciseGroup) {
    const textExercise = {
        exerciseGroup: exerciseGroup,
        maxScore: 1,
        title: 'text' + nextAlphanumeric(5),
        type: 'text',
        mode: 'INDIVIDUAL',
    };

    const res = artemis.post(TEXT_EXERCISE, textExercise);
    if (res[0].status !== 201) {
        console.log('ERROR when creating a new text exercise. Response headers:');
        for (let [key, value] of Object.entries(res[0].headers)) {
            console.log(`${key}: ${value}`);
        }
        fail('ERROR: Could not create text exercise (status: ' + res[0].status + ')! response: ' + res[0].body);
    }
    console.log('SUCCESS: Generated new text exercise');

    return JSON.parse(res[0].body);
}

export function addUserToStudentsInExam(artemis, username, exam) {
    const res = artemis.post(EXAM_STUDENTS(exam.course.id, exam.id, username));
    console.log('Add user ' + username + ' to students in exam ' + exam.id + ' status: ' + res[0].status);
}

export function generateExams(artemis, exam) {
    const res = artemis.post(GENERATE_STUDENT_EXAMS(exam.course.id, exam.id));
    console.log('Generated student exams in exam ' + exam.id + ' status: ' + res[0].status);
}

export function startExercises(artemis, exam) {
    const res = artemis.post(START_EXERCISES(exam.course.id, exam.id));
    console.log('Start exercises for exam ' + exam.id + ' status: ' + res[0].status);
}

export function getExamForUser(artemis, courseId, examId) {
    const res = artemis.get(EXAM_CONDUCTION(courseId, examId));
    console.log('Retrieved student exam for exam ' + examId + ' status: ' + res[0].status);

    return JSON.parse(res[0].body);
}

export function getStudentExams(artemis, exam) {
    const res = artemis.get(STUDENT_EXAMS(exam.course.id, exam.id));
    console.log('Retrieved student exams for exam ' + exam.id + ' status: ' + res[0].status);

    return JSON.parse(res[0].body);
}

export function updateWorkingTime(artemis, exam, studentExam, workingTime) {
    const res = artemis.patch(STUDENT_EXAM_WORKINGTIME(exam.course.id, exam.id, studentExam.id), workingTime);
    console.log('Updated student Exam for exam ' + exam.id + ' status: ' + res[0].status);

    return JSON.parse(res[0].body);
}

export function evaluateQuizzes(artemis, courseId, examId) {
    const res = artemis.post(EVALUATE_QUIZ_EXAM(courseId, examId));
    console.log('Evaluated quiz exercises in exam ' + examId + ' status: ' + res[0].status);
}
