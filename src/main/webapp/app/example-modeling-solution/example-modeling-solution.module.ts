import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiAlertService } from 'ng-jhipster';
import { ArtemisModelingEditorModule } from 'app/modeling-editor/modeling-editor.module';
import { ExampleModelingSolutionComponent } from 'app/example-modeling-solution/example-modeling-solution.component';
import { exampleModelingSolutionRoute } from 'app/example-modeling-solution/example-modeling-solution.route';
import { ArtemisSharedModule } from 'app/shared/shared.module';

const ENTITY_STATES = [...exampleModelingSolutionRoute];

@NgModule({
    imports: [ArtemisSharedModule, RouterModule.forChild(ENTITY_STATES), ArtemisModelingEditorModule],
    declarations: [ExampleModelingSolutionComponent],
    providers: [JhiAlertService],
})
export class ArtemisExampleModelingSolutionModule {}
