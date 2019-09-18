import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiAlertService } from 'ng-jhipster';
import { ApollonDiagramCreateFormComponent } from './apollon-diagram-create-form.component';
import { ApollonDiagramDetailComponent } from './apollon-diagram-detail.component';
import { ApollonDiagramListComponent } from './apollon-diagram-list.component';
import { apollonDiagramsRoutes } from './apollon-diagrams.route';
import { ApollonQuizExerciseGenerationComponent } from './exercise-generation/apollon-quiz-exercise-generation.component';
import { ArtemisSharedModule } from '../shared';
import { ArtemisResultModule } from '../entities/result';
import { SortByModule } from 'app/components/pipes';

const ENTITY_STATES = [...apollonDiagramsRoutes];

@NgModule({
    imports: [ArtemisSharedModule, RouterModule.forChild(ENTITY_STATES), SortByModule, ArtemisResultModule],
    declarations: [ApollonDiagramCreateFormComponent, ApollonDiagramDetailComponent, ApollonDiagramListComponent, ApollonQuizExerciseGenerationComponent],
    entryComponents: [ApollonDiagramCreateFormComponent, ApollonQuizExerciseGenerationComponent],
    providers: [JhiAlertService],
})
export class ArtemisApollonDiagramsModule {}
