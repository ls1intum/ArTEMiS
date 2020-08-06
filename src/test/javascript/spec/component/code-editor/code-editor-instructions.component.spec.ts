import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MockComponent } from 'ng-mocks';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { AceEditorModule } from 'ng2-ace-editor';
import { ArtemisTestModule } from '../../test.module';
import { MockCodeEditorGridService } from '../../helpers/mocks/service/mock-code-editor-grid.service';
import { CodeEditorRepositoryFileService } from 'app/exercises/programming/shared/code-editor/service/code-editor-repository.service';
import { CodeEditorInstructionsComponent } from 'app/exercises/programming/shared/code-editor/instructions/code-editor-instructions.component';
import { ProgrammingExerciseInstructionComponent } from 'app/exercises/programming/shared/instructions-render/programming-exercise-instruction.component';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { ProgrammingExerciseEditableInstructionComponent } from 'app/exercises/programming/manage/instructions-editor/programming-exercise-editable-instruction.component';
import { ArtemisMarkdownEditorModule } from 'app/shared/markdown-editor/markdown-editor.module';
import { MockCodeEditorRepositoryFileService } from '../../helpers/mocks/service/mock-code-editor-repository-file.service';

chai.use(sinonChai);
const expect = chai.expect;

describe('CodeEditorInstructionsComponent', () => {
    let comp: CodeEditorInstructionsComponent;
    let fixture: ComponentFixture<CodeEditorInstructionsComponent>;
    let debugElement: DebugElement;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [ArtemisTestModule, TranslateModule.forRoot(), AceEditorModule, ArtemisMarkdownEditorModule, NgbModule],
            declarations: [CodeEditorInstructionsComponent, MockComponent(ProgrammingExerciseInstructionComponent), MockComponent(ProgrammingExerciseEditableInstructionComponent)],
            providers: [
                { provide: CodeEditorRepositoryFileService, useClass: MockCodeEditorRepositoryFileService },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(CodeEditorInstructionsComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
            });
    });

    /*it('should not show markdown-editor nor other elements that are restricted to edit mode', () => {
        const problemStatement = 'lorem ipsum';
        const exercise = { id: 1, problemStatement } as ProgrammingExercise;
        comp.exercise = exercise;
        fixture.detectChanges();

        const saveInstructionsButton = debugElement.query(By.css('#save-instructions-button'));
        expect(saveInstructionsButton).not.to.exist;
        const instructionsLoadingIndicator = debugElement.query(By.css('.instructions-status'));
        expect(instructionsLoadingIndicator).not.to.exist;
        const instructionCompElem = debugElement.query(By.css('jhi-programming-exercise-instructions'));
        expect(instructionCompElem).to.exist;
        const editableInstructionComp = debugElement.query(By.css('jhi-programming-exercise-editable-instructions'));
        expect(editableInstructionComp).not.to.exist;
    });*/
});
