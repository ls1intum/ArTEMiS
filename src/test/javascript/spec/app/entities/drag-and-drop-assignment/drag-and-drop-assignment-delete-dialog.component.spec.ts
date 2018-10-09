/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArTEMiSTestModule } from '../../../test.module';
import { DragAndDropAssignmentDeleteDialogComponent } from 'app/entities/drag-and-drop-assignment/drag-and-drop-assignment-delete-dialog.component';
import { DragAndDropAssignmentService } from 'app/entities/drag-and-drop-assignment/drag-and-drop-assignment.service';

describe('Component Tests', () => {
    describe('DragAndDropAssignment Management Delete Component', () => {
        let comp: DragAndDropAssignmentDeleteDialogComponent;
        let fixture: ComponentFixture<DragAndDropAssignmentDeleteDialogComponent>;
        let service: DragAndDropAssignmentService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArTEMiSTestModule],
                declarations: [DragAndDropAssignmentDeleteDialogComponent]
            })
                .overrideTemplate(DragAndDropAssignmentDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(DragAndDropAssignmentDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(DragAndDropAssignmentService);
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
                })
            ));
        });
    });
});
