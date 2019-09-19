import { ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement } from '@angular/core';
import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';
import { ArtemisTestModule } from '../../test.module';
import { JhiAlertErrorComponent } from 'app/shared';
import { DeleteDialogComponent } from 'app/shared/delete-dialog/delete-dialog.component';
import { By } from '@angular/platform-browser';
import { JhiLanguageHelper } from 'app/core/language/language.helper';
import { JhiAlertService, NgJhipsterModule } from 'ng-jhipster';
import { FormsModule } from '@angular/forms';
import { NgbModule, NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import * as sinon from 'sinon';

chai.use(sinonChai);
const expect = chai.expect;

describe('DeleteDialogComponent', () => {
    let comp: DeleteDialogComponent;
    let fixture: ComponentFixture<DeleteDialogComponent>;
    let debugElement: DebugElement;
    let ngbActiveModal: NgbActiveModal;

    beforeEach(async () => {
        return TestBed.configureTestingModule({
            imports: [TranslateModule.forRoot(), ArtemisTestModule, FormsModule, NgJhipsterModule, NgbModule],
            declarations: [DeleteDialogComponent, JhiAlertErrorComponent],
            providers: [JhiLanguageHelper, JhiAlertService],
        })
            .compileComponents()
            .then(() => {
                fixture = TestBed.createComponent(DeleteDialogComponent);
                comp = fixture.componentInstance;
                debugElement = fixture.debugElement;
                ngbActiveModal = TestBed.get(NgbActiveModal);
            });
    });
    it('Dialog is correctly initialized', fakeAsync(() => {
        let inputFormGroup = debugElement.query(By.css('.form-group'));
        expect(inputFormGroup).to.not.exist;

        const modalTitle = fixture.debugElement.query(By.css('.modal-title'));
        expect(modalTitle).to.exist;
        comp.entityTitle = 'title';
        comp.deleteQuestion = 'artemisApp.exercise.delete.question';
        comp.deleteConfirmationText = 'artemisApp.exercise.delete.typeNameToConfirm';
        fixture.detectChanges();

        const closeButton = fixture.debugElement.query(By.css('.close'));
        expect(closeButton).to.exist;
        const modalDismissSpy = sinon.spy(ngbActiveModal, 'dismiss');
        closeButton.nativeElement.click();
        expect(modalDismissSpy.callCount).to.equal(1);

        const cancelButton = fixture.debugElement.query(By.css('.btn.btn-secondary'));
        expect(cancelButton).to.exist;
        cancelButton.nativeElement.click();
        expect(modalDismissSpy.callCount).to.equal(2);

        inputFormGroup = debugElement.query(By.css('.form-group'));
        expect(inputFormGroup).to.exist;
    }));

    it('Form properly checked before submission', fakeAsync(() => {
        // Form can't be submitted if there is deleteConfirmationText and user didn't input the entity title
        comp.entityTitle = 'title';
        comp.deleteConfirmationText = 'artemisApp.exercise.delete.typeNameToConfirm';
        comp.confirmEntityName = '';
        fixture.detectChanges();
        let submitButton = debugElement.query(By.css('.btn.btn-danger'));
        expect(submitButton.nativeElement.disabled).to.be.true;

        // User entered some title
        comp.confirmEntityName = 'some title';
        fixture.detectChanges();
        submitButton = debugElement.query(By.css('.btn.btn-danger'));
        expect(submitButton.nativeElement.disabled).to.be.true;

        // User entered correct tile
        comp.confirmEntityName = 'title';
        expect(comp.confirmEntityName).to.be.equal('title');
        fixture.detectChanges();
        submitButton = debugElement.query(By.css('.btn.btn-danger'));
        expect(submitButton.nativeElement.disabled).to.be.false;
    }));
});
