import { GuidedTourService } from './guided-tour.service';
import { GuidedTourComponent } from './guided-tour.component';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule, ErrorHandler } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ModuleWithProviders } from '@angular/compiler/src/core';
import { ArTEMiSSharedModule } from 'app/shared';

@NgModule({
    declarations: [GuidedTourComponent],
    imports: [CommonModule, ArTEMiSSharedModule],
    exports: [GuidedTourComponent],
    entryComponents: [GuidedTourComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
})
export class GuidedTourModule {
    public static forRoot(): ModuleWithProviders {
        return {
            ngModule: GuidedTourModule,
            providers: [ErrorHandler, GuidedTourService],
        };
    }
}
