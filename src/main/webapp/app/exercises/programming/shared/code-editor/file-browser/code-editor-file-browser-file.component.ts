import { Component, ElementRef, Input, ViewChild } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { CodeEditorFileBrowserNodeComponent } from 'app/exercises/programming/shared/code-editor/file-browser/code-editor-file-browser-node.component';

@Component({
    selector: 'jhi-code-editor-file-browser-file',
    templateUrl: './code-editor-file-browser-file.component.html',
    providers: [NgbModal],
})
export class CodeEditorFileBrowserFileComponent extends CodeEditorFileBrowserNodeComponent {
    @ViewChild('renamingInput', { static: false }) renamingInput: ElementRef;

    @Input() disableActions: boolean;
}
