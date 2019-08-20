import { MarkdownEditorComponent } from './markdown-editor.component';
import { NgModule } from '@angular/core';
import { ArtemisSharedModule } from '../shared';
import { AceEditorModule } from 'ng2-ace-editor';
import { FormsModule } from '@angular/forms';
import { ArtemisColorSelectorModule } from 'app/components/color-selector/color-selector.module';

@NgModule({
    imports: [ArtemisSharedModule, AceEditorModule, FormsModule, ArtemisColorSelectorModule],
    declarations: [MarkdownEditorComponent],
    exports: [MarkdownEditorComponent],
})
export class ArtemisMarkdownEditorModule {}
