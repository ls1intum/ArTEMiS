import { NgModule } from '@angular/core';
import { FeatureToggleDirective } from 'app/feature-toggle/feature-toggle.directive';
import { FeatureToggleService } from 'app/feature-toggle/feature-toggle.service';

@NgModule({
    declarations: [FeatureToggleDirective],
    exports: [FeatureToggleDirective],
})
export class FeatureToggleModule {
    static forRoot() {
        return {
            ngModule: FeatureToggleModule,
            providers: [FeatureToggleService],
        };
    }
}
