/* tslint:disable max-line-length */
import { ComponentFixture, fakeAsync, inject, TestBed, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArTeMiSTestModule } from '../../../test.module';
import { PointCounterDeleteDialogComponent } from 'app/entities/point-counter/point-counter-delete-dialog.component';
import { PointCounterService } from 'app/entities/point-counter/point-counter.service';

describe('Component Tests', () => {
    describe('PointCounter Management Delete Component', () => {
        let comp: PointCounterDeleteDialogComponent;
        let fixture: ComponentFixture<PointCounterDeleteDialogComponent>;
        let service: PointCounterService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArTeMiSTestModule],
                declarations: [PointCounterDeleteDialogComponent],
            })
                .overrideTemplate(PointCounterDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(PointCounterDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(PointCounterService);
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
