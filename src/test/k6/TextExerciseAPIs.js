import { sleep } from 'k6';
import { addUserToInstructorsInCourse, newCourse } from './requests/course.js';
import { startExercise, getExercise } from './requests/exercises.js';
import { newTextExercise, submitRandomTextAnswerExam } from './requests/text.js';
import { login } from './requests/requests.js';
import { createUsersIfNeeded } from './requests/user.js';
import { TEXT_EXERCISE } from './requests/endpoints';

export let options = {
    maxRedirects: 0,
    iterations: __ENV.ITERATIONS,
    vus: __ENV.ITERATIONS,
    rps: 5,
    setupTimeout: '480s',
    teardownTimeout: '240s',
};

const adminUsername = __ENV.ADMIN_USERNAME;
const adminPassword = __ENV.ADMIN_PASSWORD;
let baseUsername = __ENV.BASE_USERNAME;
let basePassword = __ENV.BASE_PASSWORD;
let userOffset = parseInt(__ENV.USER_OFFSET);
const onlyPrepare = __ENV.ONLY_PREPARE === true || __ENV.ONLY_PREPARE === 'true';

export function setup() {
    console.log('__ENV.CREATE_USERS: ' + __ENV.CREATE_USERS);
    console.log('__ENV.TIMEOUT_PARTICIPATION: ' + __ENV.TIMEOUT_PARTICIPATION);
    console.log('__ENV.TIMEOUT_EXERCISE: ' + __ENV.TIMEOUT_EXERCISE);
    console.log('__ENV.ITERATIONS: ' + __ENV.ITERATIONS);
    console.log('__ENV.USER_OFFSET: ' + __ENV.USER_OFFSET);
    console.log('__ENV.ONLY_PREPARE: ' + onlyPrepare);

    let courseId;
    let exerciseId;
    let artemis;
    let exercise;
    const iterations = parseInt(__ENV.ITERATIONS);

    if (parseInt(__ENV.COURSE_ID) === 0 || parseInt(__ENV.EXERCISE_ID) === 0) {
        console.log('Creating new exercise as no parameters are given');

        // Create course
        const artemisAdmin = login(adminUsername, adminPassword);

        const course = newCourse(artemisAdmin);
        courseId = course.id;
        console.log('Create users with ids starting from ' + userOffset + ' and up to ' + (userOffset + iterations));
        createUsersIfNeeded(artemisAdmin, baseUsername, basePassword, adminUsername, adminPassword, course, userOffset);
        console.log('Create users with ids starting from ' + (userOffset + iterations) + ' and up to ' + (userOffset + iterations + iterations));
        createUsersIfNeeded(artemisAdmin, baseUsername, basePassword, adminUsername, adminPassword, course, userOffset + iterations, true);

        // Create course instructor
        const instructorUsername = baseUsername.replace('USERID', '1');
        const instructorPassword = basePassword.replace('USERID', '1');

        console.log('Assigning ' + instructorUsername + 'to course ' + course.id + ' as the instructor');
        addUserToInstructorsInCourse(artemisAdmin, instructorUsername, course.id);

        // Login to Artemis
        artemis = login(instructorUsername, instructorPassword);

        // it might be necessary that the newly created groups or accounts are synced with the version control and continuous integration servers, so we wait for 1 minute
        const timeoutExercise = parseFloat(__ENV.TIMEOUT_EXERCISE);
        if (timeoutExercise > 0) {
            console.log('Wait ' + timeoutExercise + 's before creating the exercise so that the setup can finish properly');
            sleep(timeoutExercise);
        }

        // Create new exercise
        exercise = newTextExercise(artemis, undefined, course.id);
        exerciseId = exercise.id;
        console.log('Created text exercise with id ' + exerciseId);

        sleep(2);
    } else {
        exerciseId = parseInt(__ENV.EXERCISE_ID);
        courseId = parseInt(__ENV.COURSE_ID);
        artemis = login(adminUsername, adminPassword);
        console.log('Getting text exercise');
        exercise = getExercise(artemis, exerciseId, TEXT_EXERCISE(exerciseId));
    }

    for (let i = 1; i <= iterations; i++) {
        console.log(userOffset);
        const userId = parseInt(__VU) + userOffset + i;
        const currentUsername = baseUsername.replace('USERID', userId);
        const currentPassword = basePassword.replace('USERID', userId);
        console.log('Logging in as user ' + currentUsername);
        artemis = login(currentUsername, currentPassword);
        // Delay so that not all users start at the same time, batches of 3 users per second
        const delay = Math.floor(__VU / 3);
        sleep(delay * 3);

        console.log('Starting exercise ' + exerciseId);
        let participation = startExercise(artemis, courseId, exerciseId);
        if (participation) {
            const submissionId = participation.submissions[0].id;
            console.log('Submitting submission ' + submissionId);
            submitRandomTextAnswerExam(artemis, exercise, submissionId);
        }
        sleep(1);
    }

    sleep(2);

    console.log('Using existing course ' + courseId + ' and exercise ' + exerciseId);
    return { exerciseId, courseId };
}
