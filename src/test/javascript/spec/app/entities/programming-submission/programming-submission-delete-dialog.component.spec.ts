/* tslint:disable max-line-length */
import { ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArTeMiSTestModule } from '../../../test.module';
import { ProgrammingSubmissionDeleteDialogComponent } from 'app/entities/programming-submission/programming-submission-delete-dialog.component';
import { ProgrammingSubmissionService } from 'app/entities/programming-submission/programming-submission.service';

describe('Component Tests', () => {
    describe('ProgrammingSubmission Management Delete Component', () => {
        let comp: ProgrammingSubmissionDeleteDialogComponent;
        let fixture: ComponentFixture<ProgrammingSubmissionDeleteDialogComponent>;
        let service: ProgrammingSubmissionService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArTeMiSTestModule],
                declarations: [ProgrammingSubmissionDeleteDialogComponent],
            })
                .overrideTemplate(ProgrammingSubmissionDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(ProgrammingSubmissionDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ProgrammingSubmissionService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('confirmDelete', () => {
            it('Should call delete service on confirmDelete', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    spyOn(service, 'delete').and.returnValue(of({}));

                    // WHEN
                    comp.confirmDelete(123);
                    tick();

                    // THEN
                    expect(service.delete).toHaveBeenCalledWith(123);
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalled();
                }),
            ));
        });
    });
});
