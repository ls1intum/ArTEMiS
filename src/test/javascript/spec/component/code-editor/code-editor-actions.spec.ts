/* tslint:disable:no-unused-expression */
import { ComponentFixture, TestBed, async } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { spy, stub, SinonStub } from 'sinon';
import { Subject } from 'rxjs';
import { isEqual as _isEqual } from 'lodash';

import { AceEditorModule } from 'ng2-ace-editor';
import { CodeEditorActionsComponent } from 'app/code-editor';
import { CommitState, EditorState } from 'app/code-editor/model';
import { CodeEditorRepositoryService, CodeEditorRepositoryFileService } from 'app/code-editor/service/code-editor-repository.service';
import { ArTEMiSTestModule } from '../../test.module';

import { cartesianProduct } from 'app/shared/util/utils';
import { MockCodeEditorRepositoryFileService, MockCodeEditorRepositoryService } from '../../mocks';

chai.use(sinonChai);
const expect = chai.expect;

describe('CodeEditorActionsComponent', () => {
    let comp: CodeEditorActionsComponent;
    let fixture: ComponentFixture<CodeEditorActionsComponent>;
    let debugElement: DebugElement;
    let codeEditorRepositoryFileService: CodeEditorRepositoryFileService;
    let codeEditorRepositoryService: CodeEditorRepositoryService;
    let updateFilesStub: SinonStub;
    let commitStub: SinonStub;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArTEMiSTestModule, AceEditorModule],
            declarations: [CodeEditorActionsComponent],
            providers: [
                { provide: CodeEditorRepositoryService, useClass: MockCodeEditorRepositoryService },
                { provide: CodeEditorRepositoryFileService, useClass: MockCodeEditorRepositoryFileService },
            ],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(CodeEditorActionsComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
                codeEditorRepositoryFileService = debugElement.injector.get(CodeEditorRepositoryFileService);
                updateFilesStub = stub(codeEditorRepositoryFileService, 'updateFiles');
                codeEditorRepositoryService = debugElement.injector.get(CodeEditorRepositoryService);
                commitStub = stub(codeEditorRepositoryService, 'commit');
            });
    });

    afterEach(() => {
        updateFilesStub.restore();
        commitStub.restore();
    });

    it('should show save and submit button without any inputs', () => {
        const saveButton = fixture.debugElement.query(By.css('#save_button'));
        const submitButton = fixture.debugElement.query(By.css('#submit_button'));
        expect(saveButton).to.exist;
        expect(submitButton).to.exist;
    });

    const enableSaveButtonCombinations = cartesianProduct([EditorState.UNSAVED_CHANGES], Object.keys(CommitState).filter(k => k !== CommitState.COMMITTING), [true, false]);
    const enableCommitButtonCombinations = cartesianProduct(
        Object.keys(EditorState).filter(k => k !== EditorState.SAVING),
        [CommitState.UNCOMMITTED_CHANGES, CommitState.CLEAN],
        [false],
    );

    cartesianProduct(Object.keys(EditorState), Object.keys(CommitState), [true, false]).map((combination: [EditorState, CommitState, boolean]) => {
        const enableSaveButton = enableSaveButtonCombinations.some((c: [EditorState, CommitState, boolean]) => _isEqual(combination, c));
        const enableCommitButton = enableCommitButtonCombinations.some((c: [EditorState, CommitState, boolean]) => _isEqual(combination, c));
        return it(`Should ${enableSaveButton ? 'Enable save button' : 'Disable save button'} and ${
            enableCommitButton ? 'Enable commit button' : 'Disable commit button'
        } for this state combination: EditorState.${combination[0]} / CommitState.${combination[1]} / ${combination[2] ? 'is building' : 'is not building'} `, () => {
            const [editorState, commitState, isBuilding] = combination;
            comp.editorState = editorState;
            comp.commitState = commitState;
            comp.isBuilding = isBuilding;
            fixture.detectChanges();
            const saveButton = fixture.debugElement.query(By.css('#save_button'));
            const commitButton = fixture.debugElement.query(By.css('#submit_button'));

            expect(!saveButton.nativeElement.disabled).to.equal(enableSaveButton);
            expect(!commitButton.nativeElement.disabled).to.equal(enableCommitButton);
        });
    });

    it('should update ui when saving', () => {
        const saveButton = fixture.debugElement.query(By.css('#save_button'));
        comp.editorState = EditorState.UNSAVED_CHANGES;
        fixture.detectChanges();
        const saveButtonFeedbackBeforeSave = saveButton.nativeElement.innerHTML;
        comp.editorState = EditorState.SAVING;
        fixture.detectChanges();
        const saveButtonFeedbackAfterSave = saveButton.nativeElement.innerHTML;
        expect(saveButtonFeedbackAfterSave).not.to.be.equal(saveButtonFeedbackBeforeSave);
    });

    it('should update ui when building', () => {
        const commitButton = fixture.debugElement.query(By.css('#submit_button'));
        comp.commitState = CommitState.COMMITTING;
        fixture.detectChanges();
        const commitButtonFeedbackBeforeStartBuild = commitButton.nativeElement.innerHTML;
        comp.isBuilding = true;
        fixture.detectChanges();
        const commitButtonFeedbackAfterStartBuild = commitButton.nativeElement.innerHTML;
        expect(commitButtonFeedbackAfterStartBuild).not.to.be.equal(commitButtonFeedbackBeforeStartBuild);
    });

    it('should call repositoryFileService to save unsavedFiles and emit result on success', () => {
        const unsavedFiles = { fileName: 'lorem ipsum fileContent lorem ipsum' };
        const savedFilesResult: { [fileName: string]: null } = { fileName: null };
        const onSavedFilesSpy = spy(comp.onSavedFiles, 'emit');
        const saveObservable = new Subject<typeof savedFilesResult>();
        comp.editorState = EditorState.UNSAVED_CHANGES;
        comp.isBuilding = false;
        comp.unsavedFiles = unsavedFiles;
        fixture.detectChanges();

        updateFilesStub.returns(saveObservable);

        const saveButton = debugElement.query(By.css('#save_button'));
        expect(saveButton.nativeElement.disabled).to.be.false;
        saveButton.nativeElement.click();

        // wait for save result
        expect(comp.editorState).to.be.equal(EditorState.SAVING);

        fixture.detectChanges();
        expect(saveButton.nativeElement.disabled).to.be.true;

        // receive result for save
        saveObservable.next(savedFilesResult);
        expect(comp.editorState).to.be.equal(EditorState.SAVING);
        expect(updateFilesStub).to.have.been.calledOnceWithExactly([{ fileName: 'fileName', fileContent: unsavedFiles.fileName }]);
        expect(onSavedFilesSpy).to.have.been.calledOnceWith(savedFilesResult);

        fixture.detectChanges();
        expect(saveButton.nativeElement.disabled).to.be.true;
    });

    it('should call repositoryFileService to save unsavedFiles and emit an error on failure', () => {
        const unsavedFiles = { fileName: 'lorem ipsum fileContent lorem ipsum' };
        const errorResponse = { error: 'fatalError' };
        const onErrorSpy = spy(comp.onError, 'emit');
        const saveObservable = new Subject<typeof errorResponse>();
        comp.editorState = EditorState.UNSAVED_CHANGES;
        comp.isBuilding = false;
        comp.unsavedFiles = unsavedFiles;
        fixture.detectChanges();

        updateFilesStub.returns(saveObservable);

        const saveButton = debugElement.query(By.css('#save_button'));
        expect(saveButton.nativeElement.disabled).to.be.false;
        saveButton.nativeElement.click();

        // waiting for save result
        expect(updateFilesStub).to.have.been.calledOnceWithExactly([{ fileName: 'fileName', fileContent: unsavedFiles.fileName }]);
        expect(comp.editorState).to.be.equal(EditorState.SAVING);

        fixture.detectChanges();
        expect(saveButton.nativeElement.disabled).to.be.true;

        // receive error for save
        saveObservable.error(errorResponse);
        expect(onErrorSpy).to.have.been.calledOnceWith(errorResponse.error);
        expect(comp.editorState).to.be.equal(EditorState.UNSAVED_CHANGES);
        fixture.detectChanges();
        expect(saveButton.nativeElement.disabled).to.be.false;
    });

    it('should commit if no unsaved changes exist and update its state on response', () => {
        const commitObservable = new Subject<null>();
        comp.commitState = CommitState.UNCOMMITTED_CHANGES;
        comp.editorState = EditorState.CLEAN;
        comp.isBuilding = false;
        comp.unsavedFiles = {};
        fixture.detectChanges();

        commitStub.returns(commitObservable);

        const commitButton = fixture.debugElement.query(By.css('#submit_button'));
        expect(commitButton.nativeElement.disabled).to.be.false;

        // start commit, wait for result
        commitButton.nativeElement.click();
        expect(commitStub).to.have.been.calledOnceWithExactly();
        expect(comp.isBuilding).to.be.false;
        expect(comp.commitState).to.equal(CommitState.COMMITTING);

        fixture.detectChanges();
        expect(commitButton.nativeElement.disabled).to.be.true;

        // commit result returns
        commitObservable.next(null);
        expect(comp.isBuilding).to.be.true;
        expect(comp.commitState).to.equal(CommitState.CLEAN);

        fixture.detectChanges();
        expect(commitButton.nativeElement.disabled).to.be.true;
    });

    it('should commit if no unsaved changes exist and emit an error on error response', () => {
        const commitObservable = new Subject<void>();
        const onErrorSpy = spy(comp.onError, 'emit');
        comp.commitState = CommitState.UNCOMMITTED_CHANGES;
        comp.editorState = EditorState.CLEAN;
        comp.isBuilding = false;
        comp.unsavedFiles = {};
        fixture.detectChanges();

        commitStub.returns(commitObservable);

        const commitButton = fixture.debugElement.query(By.css('#submit_button'));
        expect(commitButton.nativeElement.disabled).to.be.false;

        // start commit, wait for result
        commitButton.nativeElement.click();
        expect(commitStub).to.have.been.calledOnceWithExactly();
        expect(comp.isBuilding).to.be.false;
        expect(comp.commitState).to.equal(CommitState.COMMITTING);

        fixture.detectChanges();
        expect(commitButton.nativeElement.disabled).to.be.true;

        // commit result returns an error
        commitObservable.error('error!');
        expect(comp.isBuilding).to.be.false;
        expect(comp.commitState).to.equal(CommitState.UNCOMMITTED_CHANGES);
        expect(onErrorSpy).to.have.been.calledOnceWithExactly('commitFailed');

        fixture.detectChanges();
        expect(commitButton.nativeElement.disabled).to.be.false;
    });

    it('should not commit if unsavedFiles exist, instead should save files first and then try to commit', () => {
        const unsavedFiles = { fileName: 'lorem ipsum fileContent lorem ipsum' };
        const commitObservable = new Subject<null>();
        const saveObservable = new Subject<null>();
        const saveChangedFilesStub = stub(comp, 'saveChangedFiles');
        comp.commitState = CommitState.UNCOMMITTED_CHANGES;
        comp.editorState = EditorState.UNSAVED_CHANGES;
        comp.isBuilding = false;

        comp.unsavedFiles = unsavedFiles;
        comp.saveChangedFiles = saveChangedFilesStub;
        fixture.detectChanges();

        commitStub.returns(commitObservable);
        saveChangedFilesStub.returns(saveObservable);

        const commitButton = fixture.debugElement.query(By.css('#submit_button'));
        expect(commitButton.nativeElement.disabled).to.be.false;

        // unsaved changes exist, needs to save files first
        commitButton.nativeElement.click();
        expect(commitStub).to.not.have.been.called;
        expect(saveChangedFilesStub).to.have.been.calledOnce;

        // save completed
        saveObservable.next(null);
        expect(comp.commitState).to.equal(CommitState.COMMITTING);

        // commit result returns
        commitObservable.next(null);
        expect(comp.isBuilding).to.be.true;
        expect(comp.commitState).to.equal(CommitState.CLEAN);

        fixture.detectChanges();
        expect(commitButton.nativeElement.disabled).to.be.true;
    });
});
