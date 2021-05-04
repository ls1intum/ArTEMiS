import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ArtemisTestModule } from '../../test.module';
import { ModelingExerciseDetailComponent } from 'app/exercises/modeling/manage/modeling-exercise-detail.component';
import { ModelingExercise } from 'app/entities/modeling-exercise.model';
import { ModelingExerciseService } from 'app/exercises/modeling/manage/modeling-exercise.service';
import { TranslateService } from '@ngx-translate/core';
import { MockComponent, MockProvider } from 'ng-mocks';
import { NonProgrammingExerciseDetailCommonActionsComponent } from 'app/exercises/shared/exercise-detail-common-actions/non-programming-exercise-detail-common-actions.component';
import { JhiEventManager } from 'ng-jhipster';
import * as sinonChai from 'sinon-chai';
import * as chai from 'chai';
import * as sinon from 'sinon';

chai.use(sinonChai);
const expect = chai.expect;

describe('ModelingExercise Management Detail Component', () => {
    let comp: ModelingExerciseDetailComponent;
    let fixture: ComponentFixture<ModelingExerciseDetailComponent>;
    let modelingExerciseService: ModelingExerciseService;
    let eventManager: JhiEventManager;

    const model = { element: { id: '33' } };
    const modelingExercise = { id: 123, sampleSolutionModel: JSON.stringify(model) } as ModelingExercise;
    const route = ({ params: of({ exerciseId: modelingExercise.id }) } as any) as ActivatedRoute;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            declarations: [ModelingExerciseDetailComponent, MockComponent(NonProgrammingExerciseDetailCommonActionsComponent)],
            providers: [{ provide: ActivatedRoute, useValue: route }, MockProvider(TranslateService)],
        })
            .overrideTemplate(ModelingExerciseDetailComponent, '')
            .compileComponents();
        fixture = TestBed.createComponent(ModelingExerciseDetailComponent);
        comp = fixture.componentInstance;
        modelingExerciseService = fixture.debugElement.injector.get(ModelingExerciseService);
        eventManager = fixture.debugElement.injector.get(JhiEventManager);
        fixture.detectChanges();
    });

    it('Should load exercise on init', fakeAsync(() => {
        // GIVEN
        const headers = new HttpHeaders().append('link', 'link;link');
        const findStub = sinon.stub(modelingExerciseService, 'find').returns(
            of(
                new HttpResponse({
                    body: modelingExercise,
                    headers,
                }),
            ),
        );

        // WHEN
        comp.ngOnInit();

        // THEN
        expect(findStub).to.have.been.called;
        expect(comp.modelingExercise).to.deep.equal(modelingExercise);
        expect(eventManager.subscribe).to.have.been.calledWith('modelingExerciseListModification');
        tick();
        expect(comp.sampleSolutionUML).to.deep.equal(model);
    }));

    it('should destroy event manager on destroy', () => {
        comp.ngOnDestroy();
        expect(eventManager.destroy).to.have.been.called;
    });
});
