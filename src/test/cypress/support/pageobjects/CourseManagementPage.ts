/**
 * A class which encapsulates UI selectors and actions for the course management page.
 */
export class CourseManagementPage {
    openCourseCreation() {
        return cy.get('.create-course').click();
    }

    /**
     * @returns Returns the cypress chainable containing the root element of the course card of our created course.
     * This can be used to find specific elements within this course card.
     */
    getCourseCard(courseName: string, courseShortName: string) {
        return cy.contains(this.courseSelector(courseName, courseShortName)).parent().parent().parent();
    }

    /**
     * Opens the exercises (of the first found course).
     */
    openExercisesOfCourse(courseName: string, courseShortName: string) {
        this.getCourseCard(courseName, courseShortName).find('.card-footer').eq(0).children().eq(0).click();
        cy.url().should('include', '/exercises');
    }

    /**
     * Opens the students overview page of a course.
     * @param courseName
     * @param courseShortName
     */
    openStudentOverviewOfCourse(courseName: string, courseShortName: string) {
        // TODO: Generify the selector
        this.getCourseCard(courseName, courseShortName).contains('0 Students').click();
    }

    /**
     * Opens a course.
     * @param courseName
     * @param courseShortName
     */
    openCourse(courseName: string, courseShortName: string) {
        return cy.contains(this.courseSelector(courseName, courseShortName)).parent().parent().click();
    }

    /**
     * @param courseShortName
     * @returns the title element of the course card.
     */
    courseSelector(courseName: string, courseShortName: string) {
        return `${courseName} (${courseShortName})`;
    }
}
