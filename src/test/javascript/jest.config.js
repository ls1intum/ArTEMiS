const esModules = ['ngx-treeview', 'lodash-es'].join('|');
module.exports = {
    globals: {
        'ts-jest': {
            tsconfig: '<rootDir>/tsconfig.spec.json',
            stringifyContentPathRegex: '\\.html$',
            astTransformers: {
                before: [require.resolve('./InlineHtmlStripStylesTransformer')],
            },
            diagnostics: {
                ignoreCodes: [151001],
            },
        },
    },
    coverageThreshold: {
        global: {
            // TODO: in the future, the following values should be increase to 80%
            statements: 69,
            branches: 45,
            functions: 53,
            lines: 68,
        },
    },
    preset: 'jest-preset-angular',
    setupFilesAfterEnv: ['<rootDir>/src/test/javascript/jest.ts', 'jest-sinon'],
    modulePaths: ['<rootDir>/src/main/webapp/'],
    transformIgnorePatterns: [`/node_modules/(?!${esModules})`],
    rootDir: '../../../',
    modulePathIgnorePatterns: [
        '<rootDir>/src/test/javascript/spec/component/admin/upcoming-exams-and-exercises.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/admin/user-management-update.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/assessment-shared/assessment-complaint-alert.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/assessment-shared/assessment-header.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/assessment-shared/assessment-layout.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/assessment-shared/assessment-locks.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/code-editor/code-editor-ace.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/code-editor/code-editor-build-output.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/code-editor/code-editor-file-browser.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/complaints-for-tutor/complaints-for-tutor.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/complaints/complaints.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/exam/manage/exam-management.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/exam/manage/student-exams/student-exams.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/file-upload-assessment/file-upload-assessment.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/file-upload-exercise/file-upload-exercise-detail.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/file-upload-submission/file-upload-result.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/file-upload-submission/file-upload-submission.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/instructor-dashboard/instructor-exercise-dashboard.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/lecture/lecture-attachments.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/list-of-complaints/list-of-complaints.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/markdown-editor/bold-command.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/markdown-editor/gradingInstruction-command.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/markdown-editor/italic-command.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/markdown-editor/katex-command.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/modeling-assessment-editor/modeling-assessment-editor.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/modeling-submission/modeling-submission.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/course-exams/course-exam-detail.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/course-exams/course-exams.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/course-exercises/course-exercise-row.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/course-questions/course-questions.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/course-statistics/course-statistics.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/exercise-details/exercise-details-student-actions.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/exercise-details/programming-exercise-student-ide-actions.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/overview/overview.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/participation-submission/participation-submission.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/participation/participation.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/plagiarism/modeling-submission-viewer.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-assessment/programming-assessment-inline-feedback.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-assessment/programming-assessment-manual-result.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-configure-grading.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-editable-instruction.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-import.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-instruction-analysis.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-instruction-step-wizard.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-instructor-submission-state.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-test-schedule-picker.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/programming-exercise/programming-exercise-trigger-build-button.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/quiz-statistic/drag-and-drop-statistic.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/quiz-statistic/multiple-choice-question-statistic.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/quiz-statistic/quiz-point-statistic.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/quiz-statistic/quiz-statistic.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/quiz-statistic/short-answer-question-statistic.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/rating/rating-list.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/rating/rating.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/button.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/notification/notification-sidebar.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/notification/system-notification.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/result-detail.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/secured-image.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/shared/updating-result.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/student-questions/student-question-row.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/student-questions/student-question.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/student-questions/student-questions.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/student-questions/student-votes.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/table/table-editable-checkbox.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/table/table-editable-field.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/team/team-update-dialog.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/team/teams.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-editor/text-editor.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-exercise/text-exercise-update.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-submission-assessment/manual-textblock-selection.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-submission-assessment/text-assessment-area.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-submission-assessment/text-feedback-conflicts.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-submission-assessment/text-submission-assessment.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/text-submission-assessment/textblock-assessment-card.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/tutor-dashboard/exercise-assessment-dashboard.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/component/tutor-dashboard/tutor-participation-graph.component.spec.ts',
        '<rootDir>/src/test/javascript/spec/integration/code-editor/code-editor-container.integration.spec.ts',
        '<rootDir>/src/test/javascript/spec/integration/code-editor/code-editor-instructor.integration.spec.ts',
        '<rootDir>/src/test/javascript/spec/integration/code-editor/code-editor-student.integration.spec.ts',
        '<rootDir>/src/test/javascript/spec/service/delete-dialog.service.spec.ts',
        '<rootDir>/src/test/javascript/spec/service/guided-tour.service.spec.ts',
        '<rootDir>/src/test/javascript/spec/service/modeling-exercise.service.spec.ts',
        '<rootDir>/src/test/javascript/spec/service/programming-exercise-instruction-analysis.service.spec.ts',
        '<rootDir>/src/test/javascript/spec/service/programming-exercise.service.spec.ts',
    ],
    testMatch: [
        '<rootDir>/src/test/javascript/spec/component/**/*.ts',
        '<rootDir>/src/test/javascript/spec/directive/**/*.ts',
        '<rootDir>/src/test/javascript/spec/integration/**/*.ts',
        '<rootDir>/src/test/javascript/spec/pipe/**/*.ts',
        '<rootDir>/src/test/javascript/spec/service/**/*.ts',
        '<rootDir>/src/test/javascript/spec/util/**/*.ts',
    ],
    moduleNameMapper: {
        '^app/(.*)': '<rootDir>/src/main/webapp/app/$1',
        'test/(.*)': '<rootDir>/src/test/javascript/spec/$1',
        '@assets/(.*)': '<rootDir>/src/main/webapp/assets/$1',
        '@core/(.*)': '<rootDir>/src/main/webapp/app/core/$1',
        '@env': '<rootDir>/src/main/webapp/environments/environment',
        '@src/(.*)': '<rootDir>/src/src/$1',
        '@state/(.*)': '<rootDir>/src/app/state/$1',
    },
};
