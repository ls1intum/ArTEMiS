import { Component, Input, OnInit } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { TextUnit } from 'app/entities/lecture-unit/textUnit.model';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { SERVER_API_URL } from 'app/app.constants';

@Component({
    selector: 'jhi-text-unit',
    templateUrl: './text-unit.component.html',
    styleUrls: ['./text-unit.component.scss'],
})
export class TextUnitComponent implements OnInit {
    @Input()
    textUnit: TextUnit;
    isCollapsed = true;

    formattedContent?: SafeHtml;

    constructor(private artemisMarkdown: ArtemisMarkdownService) {}

    ngOnInit(): void {
        if (this.textUnit?.content) {
            this.formattedContent = this.artemisMarkdown.safeHtmlForMarkdown(this.textUnit.content);
        }
    }

    handleCollapse($event: any) {
        $event.stopPropagation();
        this.isCollapsed = !this.isCollapsed;
    }

    openPopup($event: any) {
        $event.stopPropagation();

        const width = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
        const height = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
        const win = window.open('', ' ', 'toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,' + `width=${width},height=${height}`);
        win!.document.write(`<html><head><title>${this.textUnit.name}</title>`);
        win!.document.write(`<link rel="stylesheet" href="${SERVER_API_URL}public/content/github-markdown.css">`);
        win!.document.write('</head><body class="markdown-body">');
        win!.document.write('</body></html>');
        win!.document.close();
        win!.document.body.innerHTML = this.artemisMarkdown.htmlForMarkdown(this.textUnit.content, []);
        win!.focus();
    }
}
