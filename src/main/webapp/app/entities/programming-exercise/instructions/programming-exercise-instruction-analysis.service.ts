import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { compose, filter, flatten, map, reduce, uniq } from 'lodash/fp';
import { matchRegexWithLineNumbers, RegExpLineNumberMatchArray } from 'app/utils/global.utils';
import { ProgrammingExerciseTestCase } from 'app/entities/programming-exercise/programming-exercise-test-case.model';
import { ExerciseHint } from 'app/entities/exercise-hint/exercise-hint.model';

export type ProblemStatementAnalysis = Array<{
    lineNumber: number;
    invalidTestCases?: string[];
    invalidHints?: string[];
}>;

export enum ProblemStatementIssue {
    INVALID_TEST_CASES = 'invalidTestCases',
    INVALID_HINTS = 'invalidHints',
}

export type AnalysisItem = [number, string[], ProblemStatementIssue];

@Injectable()
export class ProgrammingExerciseInstructionAnalysisService {
    TEST_CASE_REGEX = new RegExp('.*\\((.*)\\)');
    HINT_REGEX = new RegExp('.*{(.*)}');
    INVALID_TEST_CASE_TRANSLATION = 'artemisApp.programmingExercise.testCaseAnalysis.invalidTestCase';
    INVALID_HINT_TRANSLATION = 'artemisApp.programmingExercise.hintsAnalysis.invalidHint';

    constructor(private translateService: TranslateService) {}

    public analyzeProblemStatement = (problemStatement: string, taskRegex: RegExp, exerciseTestCases: string[], exerciseHints: ExerciseHint[]) => {
        // Look for task regex matches in the problem statement including their line numbers.
        const tasksFromProblemStatement = matchRegexWithLineNumbers(problemStatement, taskRegex);

        const { invalidTestCases, missingTestCases, invalidTestCaseAnalysis } = this.analyzeTestCases(tasksFromProblemStatement, exerciseTestCases);
        const { invalidHints, invalidHintAnalysis } = this.analyzeHints(tasksFromProblemStatement, exerciseHints);

        const completeAnalysis: ProblemStatementAnalysis = this.mergeAnalysis(invalidTestCaseAnalysis, invalidHintAnalysis);
        return { invalidTestCases, missingTestCases, invalidHints, completeAnalysis };
    };

    /**
     * Analyze the test cases for the following criteria:
     * - Are test cases in the problem statement that don't exist for the exercise?
     * - Do test cases exist for this exercise that are not part of the problem statement?
     *
     * Will also set the invalidTestCases & missingTestCases attributes of the component.
     *
     * @param tasksFromProblemStatement to check if they contain test cases.
     */
    public analyzeTestCases = (tasksFromProblemStatement: RegExpLineNumberMatchArray, exerciseTestCases: string[]) => {
        // Extract the testCase list from the task matches.
        const testCasesInMarkdown = this.extractRegexFromTasks(tasksFromProblemStatement, this.TEST_CASE_REGEX);
        // Look for test cases that are not part of the test repository. Could e.g. be typos.
        const invalidTestCaseAnalysis = testCasesInMarkdown
            .map(
                ([lineNumber, testCases]) =>
                    [lineNumber, testCases.filter(testCase => !exerciseTestCases.includes(testCase)), ProblemStatementIssue.INVALID_TEST_CASES] as AnalysisItem,
            )
            .filter(([, testCases]) => testCases.length);
        // Look for test cases that are part of the test repository but not in the problem statement. Probably forgotten to insert.
        const missingTestCases = exerciseTestCases.filter(testCase => !testCasesInMarkdown.some(([, foundTestCases]) => foundTestCases.includes(testCase)));

        const invalidTestCases = compose(
            flatten,
            map(([, testCases]) => testCases),
        )(invalidTestCaseAnalysis);

        return { missingTestCases, invalidTestCases, invalidTestCaseAnalysis };
    };

    /**
     * Analyze the hints for the following criteria:
     * - Are hints in the problem statement that don't exist for the exercise?
     *
     * Will also set the invalidHints attribute of the component.
     *
     * @param tasksFromProblemStatement to check if they contain hints.
     */
    public analyzeHints = (tasksFromProblemStatement: RegExpLineNumberMatchArray, exerciseHints: ExerciseHint[]) => {
        const hintsInMarkdown = this.extractRegexFromTasks(tasksFromProblemStatement, this.HINT_REGEX);
        const invalidHintAnalysis = hintsInMarkdown
            .map(
                ([lineNumber, hints]): AnalysisItem => [
                    lineNumber,
                    hints.filter(hint => !exerciseHints.some(exerciseHint => exerciseHint.id.toString(10) === hint)),
                    ProblemStatementIssue.INVALID_HINTS,
                ],
            )
            .filter(([, hints]) => !!hints.length);

        const invalidHints = compose(
            flatten,
            map(([, testCases]) => testCases),
        )(invalidHintAnalysis);

        return { invalidHints, invalidHintAnalysis };
    };

    /**
     * Merges multiple AnalyseItem[] into one accumulated ProblemStatementAnalysis.
     *
     * @param analysis arbitrary number of analysis objects to be merged into one.
     */
    public mergeAnalysis = (...analysis: Array<AnalysisItem[]>) => {
        return compose(
            reduce((acc, [lineNumber, values, issueType]) => {
                const lineNumberValues = acc[lineNumber];
                const issueValues = lineNumberValues ? lineNumberValues[issueType] || [] : [];
                return { ...acc, [lineNumber]: { ...lineNumberValues, [issueType]: [...issueValues, ...values] } };
            }, {}),
            map(([lineNumber, values, issueType]: AnalysisItem) => [
                lineNumber,
                values.map(id => this.translateService.instant(this.getTranslationByIssueType(issueType), { id })),
                issueType,
            ]),
            flatten,
        )(analysis);
    };

    /**
     * Matches the issueType to a translation. A given translation should have {{id}} as a parameter.
     *
     * @param issueType for which to retrieve the fitting translation.
     */
    private getTranslationByIssueType = (issueType: ProblemStatementIssue) => {
        switch (issueType) {
            case ProblemStatementIssue.INVALID_TEST_CASES:
                return this.INVALID_TEST_CASE_TRANSLATION;
            case ProblemStatementIssue.INVALID_HINTS:
                return this.INVALID_HINT_TRANSLATION;
            default:
                return '';
        }
    };

    private extractRegexFromTasks(tasks: [number, string][], regex: RegExp): [number, string[]][] {
        return compose(
            map(([lineNumber, match]) => {
                const cleanedMatches = compose(
                    uniq,
                    filter(m => !!m),
                    flatten,
                )(match.split(',').map((m: string) => m.trim()));
                return [lineNumber, cleanedMatches];
            }),
            filter(([, testCases]) => !!testCases),
            map(([lineNumber, task]) => {
                const extractedValue = task.match(regex);
                return extractedValue && extractedValue.length > 1 ? [lineNumber, extractedValue[1]] : [lineNumber, null];
            }),
            filter(([, task]) => !!task),
        )(tasks) as [number, string[]][];
    }
}
