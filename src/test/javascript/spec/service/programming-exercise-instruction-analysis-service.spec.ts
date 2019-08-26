import { problemStatement } from '../sample/problemStatement.json';
import { ProgrammingExerciseInstructionAnalysisService } from 'app/entities/programming-exercise/instructions/instructions-editor';
import { MockTranslateService } from '../mocks/mock-translate.service';
import { ExerciseHint } from 'app/entities/exercise-hint/exercise-hint.model';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';

chai.use(sinonChai);
const expect = chai.expect;

describe('ProgrammingExerciseInstructionAnalysisService', () => {
    const taskRegex = /\[task\](.*)/g;

    let analysisService: ProgrammingExerciseInstructionAnalysisService;

    beforeEach(() => {
        // @ts-ignore
        analysisService = new ProgrammingExerciseInstructionAnalysisService(new MockTranslateService());
    });

    it('should analyse problem statement without any issues correctly', () => {
        const exerciseHints = [{ id: 33 }, { id: 44 }] as ExerciseHint[];
        const testCases = ['testMergeSort', 'testBubbleSort'];
        const { invalidHints, invalidTestCases, missingTestCases, completeAnalysis } = analysisService.analyzeProblemStatement(
            problemStatement,
            taskRegex,
            testCases,
            exerciseHints,
        );

        expect(invalidHints).to.be.empty;
        expect(invalidTestCases).to.be.empty;
        expect(missingTestCases).to.be.empty;
        expect(completeAnalysis).to.be.empty;
    });

    it('should analyse problem statement with issues correctly', () => {
        const exerciseHints = [{ id: 33 }, { id: 45 }] as ExerciseHint[]; // Typo in hint id (44 vs 45).
        const testCases = ['testBubbleSortNew']; // test name was changed, the new test name is missing.
        const expectedAnalysis = {
            '0': { invalidTestCases: ['artemisApp.programmingExercise.testCaseAnalysis.invalidTestCase'] },
            '2': {
                invalidHints: ['artemisApp.programmingExercise.hintsAnalysis.invalidHint'],
                invalidTestCases: ['artemisApp.programmingExercise.testCaseAnalysis.invalidTestCase'],
            },
        };
        const { invalidHints, invalidTestCases, missingTestCases, completeAnalysis } = analysisService.analyzeProblemStatement(
            problemStatement,
            taskRegex,
            testCases,
            exerciseHints,
        );

        expect(invalidHints).to.deep.equal(['44']);
        expect(invalidTestCases).to.deep.equal(['testBubbleSort', 'testMergeSort']);
        expect(missingTestCases).to.deep.equal(['testBubbleSortNew']);
        expect(completeAnalysis).to.deep.equal(expectedAnalysis);
    });
});
