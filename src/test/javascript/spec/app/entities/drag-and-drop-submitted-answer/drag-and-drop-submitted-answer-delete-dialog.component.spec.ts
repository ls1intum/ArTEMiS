/* tslint:disable max-line-length */
import { ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArtemisTestModule } from '../../../test.module';
import { DragAndDropSubmittedAnswerDeleteDialogComponent } from 'app/entities/drag-and-drop-submitted-answer/drag-and-drop-submitted-answer-delete-dialog.component';
import { DragAndDropSubmittedAnswerService } from 'app/entities/drag-and-drop-submitted-answer/drag-and-drop-submitted-answer.service';

describe('Component Tests', () => {
    describe('DragAndDropSubmittedAnswer Management Delete Component', () => {
        let comp: DragAndDropSubmittedAnswerDeleteDialogComponent;
        let fixture: ComponentFixture<DragAndDropSubmittedAnswerDeleteDialogComponent>;
        let service: DragAndDropSubmittedAnswerService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArtemisTestModule],
                declarations: [DragAndDropSubmittedAnswerDeleteDialogComponent],
            })
                .overrideTemplate(DragAndDropSubmittedAnswerDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(DragAndDropSubmittedAnswerDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(DragAndDropSubmittedAnswerService);
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
