import { NgModule } from '@angular/core';

import { ArtemisSharedModule } from 'app/shared';
import { DifficultyPickerComponent } from 'app/components/exercise/difficulty-picker/difficulty-picker.component';

@NgModule({
    imports: [ArtemisSharedModule],
    declarations: [DifficultyPickerComponent],
    exports: [DifficultyPickerComponent],
})
export class ArtemisDifficultyPickerModule {}
