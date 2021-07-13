import * as chai from 'chai';
import * as sinonChai from 'sinon-chai';

import { MockRouter } from '../../helpers/mocks/service/mock-route.service';
import { spy, stub } from 'sinon';
import { OrionConnectorService } from 'app/shared/orion/orion-connector.service';
import { ArtemisTestModule } from '../../test.module';
import { ProgrammingExerciseComponent } from 'app/exercises/programming/manage/programming-exercise.component';
import { OrionProgrammingExerciseComponent } from 'app/orion/management/orion-programming-exercise.component';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MockComponent, MockPipe, MockProvider } from 'ng-mocks';
import { OrionModule } from 'app/shared/orion/orion.module';
import { TranslateService } from '@ngx-translate/core';
import { ArtemisTranslatePipe } from 'app/shared/pipes/artemis-translate.pipe';
import { BehaviorSubject } from 'rxjs';

chai.use(sinonChai);
const expect = chai.expect;

describe('OrionProgrammingExerciseComponent', () => {
    let comp: OrionProgrammingExerciseComponent;

    let orionConnectorService: OrionConnectorService;
    const router = new MockRouter();

    const programmingExercise = { id: 456 } as any;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule, OrionModule],
            declarations: [OrionProgrammingExerciseComponent, MockComponent(ProgrammingExerciseComponent), MockPipe(ArtemisTranslatePipe)],
            providers: [
                MockProvider(TranslateService),
                MockProvider(OrionConnectorService),
                { provide: Router, useValue: router },
            ],
        })
            .compileComponents()
            .then(() => {
                comp = TestBed.createComponent(OrionProgrammingExerciseComponent).componentInstance;
                orionConnectorService = TestBed.inject(OrionConnectorService);
                comp.programmingExercises = [programmingExercise];
            });
    });

    afterEach(() => {
        router.navigateSpy.restore();
    });

    it('ngOnInit should subscribe to state', () => {
        const orionStateStub = stub(orionConnectorService, 'state');
        const orionState = { opened: 40, building: false, cloning: false } as any;
        orionStateStub.returns(new BehaviorSubject(orionState));

        comp.ngOnInit();

        expect(orionStateStub).to.have.been.calledOnceWithExactly();
        expect(comp.orionState).to.be.deep.equals(orionState);
    });
    it('editInIde should call connector', () => {
        const editExerciseSpy = spy(orionConnectorService, 'editExercise');

        comp.editInIDE(programmingExercise);

        expect(editExerciseSpy).to.have.been.calledOnceWithExactly(programmingExercise);
    });
    it('openOrionEditor should navigate to orion editor', () => {
        comp.openOrionEditor({ ...programmingExercise, templateParticipation: { id: 1234 } });

        expect(router.navigateSpy).to.have.been.calledOnceWithExactly(['code-editor', 'ide', 456, 'admin', 1234]);
    });
    it('openOrionEditor with error', () => {
        const error = 'test error';
        router.navigateSpy.throws(error);

        comp.openOrionEditor(programmingExercise);

        expect(router.navigateSpy).to.have.been.calledWithExactly(['code-editor', 'ide', 456, 'admin', undefined]);
    });
});
