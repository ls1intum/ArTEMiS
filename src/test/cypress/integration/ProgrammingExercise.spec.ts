/// <reference types="cypress" />

import { beVisible } from '../support/constants';
import { CourseManagementPage } from '../support/pageobjects/CourseManagementPage';
import { NavigationBar } from '../support/pageobjects/NavigationBar';
import { ArtemisRequests } from '../support/requests/ArtemisRequests';
import { generateUUID } from '../support/utils';

// Environmental variables
const adminUsername = Cypress.env('adminUsername');
const adminPassword = Cypress.env('adminPassword');
let username = Cypress.env('username');
let password = Cypress.env('password');
if (Cypress.env('isCi')) {
    username = username.replace('USERID', '1');
    password = password.replace('USERID', '1');
}

// Requests
var artemisRequests: ArtemisRequests;

// PagegObjects
var courseManagementPage: CourseManagementPage;
var navigationBar: NavigationBar;

// Common primitives
var uid: string;
var courseName: string;
var courseShortName: string;
var programmingExerciseName: string;
var programmingExerciseShortName: string;
const packageName = 'de.test';

// Selectors
const fieldTitle = '#field_title';
const shortName = '#field_shortName';
const saveEntity = '#save-entity';
const datepickerButtons = '.owl-dt-container-control-button';

describe('Programming Exercise Management', () => {
    var course: any;

    beforeEach(() => {
        courseManagementPage = new CourseManagementPage();
        navigationBar = new NavigationBar();
        registerQueries();
        uid = generateUUID();
        courseName = 'Cypress course' + uid;
        courseShortName = 'cypress' + uid;
        programmingExerciseName = 'Cypress programming exercise ' + uid;
        programmingExerciseShortName = courseShortName;
        artemisRequests = new ArtemisRequests();
        cy.login(adminUsername, adminPassword, '/');
        artemisRequests.courseManagement
            .createCourse(courseName, courseShortName, uid)
            .its('body')
            .then((body) => {
                expect(body).property('id').to.be.a('number');
                course = body;
            });
    });

    describe('Programming exercise creation', () => {
        var programmingExerciseId: number;

        it('Creates a new programming exercise', function () {
            navigationBar.openCourseManagement();
            courseManagementPage.openExercisesOfCourse(courseName, courseShortName);
            cy.get('#jh-create-entity').click();
            cy.url().should('include', '/programming-exercises/new');
            cy.log('Filling out programming exercise info...');
            cy.get(fieldTitle).type(programmingExerciseName);
            cy.get(shortName).type(programmingExerciseShortName);
            cy.get('#field_packageName').type(packageName);
            cy.get('[label="artemisApp.exercise.releaseDate"] > :nth-child(1) > .btn').should(beVisible).click();
            cy.get(datepickerButtons).wait(500).eq(1).should(beVisible).click();
            cy.get('.test-schedule-date.ng-pristine > :nth-child(1) > .btn').click();
            cy.get('.owl-dt-control-arrow-button').eq(1).click();
            cy.get('.owl-dt-day-3').eq(2).click();
            cy.get(datepickerButtons).eq(1).should(beVisible).click();
            cy.get('#field_points').type('100');
            cy.get('#field_allowOnlineEditor').check();
            cy.get(saveEntity).click();
            cy.wait('@createProgrammingExerciseQuery')
                .its('response.body')
                .then((body) => {
                    expect(body).property('id').to.be.a('number');
                    programmingExerciseId = body.id;
                });
            cy.url().should('include', '/exercises');
            cy.contains(programmingExerciseName).should(beVisible);
        });
    });

    describe('Programming exercise deletion', () => {
        beforeEach(() => {
            artemisRequests.courseManagement.createProgrammingExercise(course, programmingExerciseName, programmingExerciseShortName, packageName).its('status').should('eq', 201);
        });

        it('Deletes an existing programming exercise', function () {
            navigationBar.openCourseManagement();
            courseManagementPage.openExercisesOfCourse(courseName, courseShortName);
            cy.get('[deletequestion="artemisApp.programmingExercise.delete.question"]').click();
            // Check all checkboxes to get rid of the git repositories and build plans
            cy.get('.modal-body')
                .find('[type="checkbox"]')
                .each(($el) => {
                    cy.wrap($el).check();
                });
            cy.get('[type="text"], [name="confirmExerciseName"]').type(programmingExerciseName).type('{enter}');
            cy.wait('@deleteProgrammingExerciseQuery');
            cy.contains('No Programming Exercises').should(beVisible);
        });
    });

    afterEach(() => {
        if (course != null) artemisRequests.courseManagement.deleteCourse(course.id).its('status').should('eq', 200);
    });
});

/**
 * Sets all the necessary cypress request hooks.
 */
function registerQueries() {
    cy.intercept('DELETE', '/api/programming-exercises/*').as('deleteProgrammingExerciseQuery');
    cy.intercept('POST', '/api/programming-exercises/setup').as('createProgrammingExerciseQuery');
}
