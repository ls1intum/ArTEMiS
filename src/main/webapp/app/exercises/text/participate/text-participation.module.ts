import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { MomentModule } from 'ngx-moment';
import { textEditorRoute } from 'app/exercises/text/participate/text-editor.route';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { ArtemisResultModule } from 'app/exercises/shared/result/result.module';
import { ArtemisComplaintsModule } from 'app/complaints/complaints.module';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { TextEditorComponent } from 'app/exercises/text/participate/text-editor.component';
import { TextEditorScoreCardComponent } from 'app/exercises/text/participate/text-editor-score-card/text-editor-score-card.component';
import { TextResultComponent } from 'app/exercises/text/participate/text-result/text-result.component';
import { ArtemisTeamModule } from 'app/exercises/shared/team/team.module';
import { TextSubmissionTeamSyncComponent } from 'app/exercises/text/participate/text-submission-team-sync.component';

const ENTITY_STATES = [...textEditorRoute];

@NgModule({
    imports: [
        ArtemisSharedModule,
        RouterModule.forChild(ENTITY_STATES),
        ArtemisResultModule,
        ArtemisComplaintsModule,
        ArtemisSharedComponentModule,
        MomentModule,
        ArtemisTeamModule,
    ],
    declarations: [TextEditorComponent, TextEditorScoreCardComponent, TextResultComponent, TextSubmissionTeamSyncComponent],
})
export class ArtemisTextParticipationModule {}
