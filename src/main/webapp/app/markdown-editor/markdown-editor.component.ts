import {
    AfterViewInit,
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { AceEditorComponent } from 'ng2-ace-editor';
import 'brace/theme/chrome';
import 'brace/mode/markdown';
import { Command,
        BoldCommand,
        ItalicCommand,
        UnderlineCommand,
        HeadingOneCommand,
        HeadingTwoCommand,
        HeadingThreeCommand,
        CodeCommand,
        LinkCommand,
        AttachmentCommand,
        OrderedListCommand,
        UnorderedListCommand,
        ReferenceCommand,
} from 'app/markdown-editor/commands';
import { ArtemisMarkdown } from 'app/components/util/markdown.service';
import { DomainCommand } from 'app/markdown-editor/domainCommands';

@Component({
    selector: 'jhi-markdown-editor',
    styleUrls: ['./markdown-editor.scss'],
    providers: [ArtemisMarkdown],
    templateUrl: './markdown-editor.component.html'
})

export class MarkdownEditorComponent implements AfterViewInit, OnInit {
    @ViewChild('aceEditor')
    aceEditorContainer: AceEditorComponent;
    aceEditorOptions = {
        autoUpdateContent: true,
        mode: 'markdown',
    };

    /** {string} which is initially displayed in the editor generated and passed on from the parent component*/
    @Input() markdown: string;
    @Output() markdownChange = new EventEmitter<string>();
    @Output() html = new EventEmitter<string>();

    /** {array} containing all default commands accessible for the editor per default */
    defaultCommands: Command[] = [
        new BoldCommand(),
        new ItalicCommand(),
        new UnderlineCommand(),
        new ReferenceCommand(),
        new CodeCommand(),
        new LinkCommand(),
        new AttachmentCommand(),
        new OrderedListCommand(),
        new UnorderedListCommand(),
    ];

    /** {array} containing all header commands accessible for the markdown editor per defaulT*/
    headerCommands: Command[] = [
        new HeadingOneCommand(),
        new HeadingTwoCommand(),
        new HeadingThreeCommand(),
    ];

    /**
     * {domainCommands} containing all domain commands which need to be set by the parent component which contains the markdown editor
     */
    @Input() domainCommands: DomainCommand[];

    /**
     * {textWithDomainCommandFound} emits an {array} of text lines with the corresponding domain command to the parent component which contains the markdown editor
     */
    @Output() textWithDomainCommandFound = new EventEmitter<[string, DomainCommand][]>();

    /**
     * {showPreviewButton} 1. true -> the preview of the editor is used
     *           2. false -> the preview of the parent component is used, parent has to set this value to false with an input
     */
    @Input() showPreviewButton = true;

    /**
     * {previewTextAsHtml} text that is emitted to the parent component if the parent does not use any domain commands
     */
    previewTextAsHtml: string;

    /**
     * {previewMode} when editor is created the preview is set to false, since the edit mode is set active
     */
    previewMode = false;

    /**
     * {previewChild} Is not null when the parent component is responsible for the preview content
     * -> parent component has to implement ng-content and set the showPreviewButton on true through an input
     */
    @ContentChild('preview') previewChild: ElementRef;

    constructor(private artemisMarkdown: ArtemisMarkdown) {
    }

    get previewButtonTranslateString(): string {
        return this.previewMode ? 'entity.action.edit' : 'entity.action.preview';
    }

    get previewButtonIcon(): string {
        return this.previewMode ? 'pencil-alt' : 'eye';
    }

    /** {boolean} true when the plane html view is needed, false when the preview content is needed from the parent */
    get showDefaultPreview(): boolean {
        return this.previewChild == null;
    }

    /**
     * @function addCommand
     * @param command
     * @desc customize the user interface of the markdown editor by adding a command
     */
    addCommand(command: Command) {
        this.defaultCommands.push(command);
    }

    /**
     * @function removeCommand
     * @param typeof Command
     * @desc customize the user interface of the markdown editor by removing a command
     */
    removeCommand(classRef: typeof Command) {
        setTimeout(() =>
            this.defaultCommands = this.defaultCommands.filter(element => !(element instanceof classRef))
        );
    }

    ngAfterViewInit(): void {
        this.setupMarkdownEditor();
    }

    ngOnInit(): void {
        if (this.domainCommands == null || this.domainCommands.length === 0) {
        [...this.defaultCommands, ...this.headerCommands || []].forEach(command => {
            command.setEditor(this.aceEditorContainer);
        });
        } else {
            [...this.defaultCommands, ...this.domainCommands, ...this.headerCommands || []].forEach(command => {
                command.setEditor(this.aceEditorContainer);
            });
        }
    }

    /**
     * @function setupQuestionEditor
     * @desc Initializes the ace editor
     */
    setupMarkdownEditor(): void {
        this.aceEditorContainer.setTheme('chrome');
        this.aceEditorContainer.getEditor().renderer.setShowGutter(false);
        this.aceEditorContainer.getEditor().renderer.setPadding(10);
        this.aceEditorContainer.getEditor().renderer.setScrollMargin(8, 8);
        this.aceEditorContainer.getEditor().setHighlightActiveLine(false);
        this.aceEditorContainer.getEditor().setShowPrintMargin(false);
        this.aceEditorContainer.getEditor().clearSelection();
    }

    /**
     * @function parse
     * @desc Check if domainCommands are contained within the text to decide how to parse the text
     *       1. If no domainCommands are contained parse markdown to HTML and emit the result to the parent component
     *       2. Otherwise divide the text by "[-", a common identifier for all domainCommands,
     *       2a. Call the method parseLineForDomainCommand for each textLine
     *       2b. Emit the result to parent component to assign the value of the array to the right attributes
     */
    parse(): void {
        /** check if domainCommands are contained */
        if (this.domainCommands == null || this.domainCommands.length === 0) {
                this.previewTextAsHtml = this.artemisMarkdown.htmlForMarkdown(this.markdown);

                /** emit to parent component*/
                this.html.emit(this.previewTextAsHtml);
            return;
        } else {
            /** create array with domain command identifiern*/
            const possibleCommandIdentifier = new Array<string>();
            for (const domainCommand of this.domainCommands) {
                 possibleCommandIdentifier.push(domainCommand.getOpeningIdentifier());
            }

            /** create empty array that will be emited to the parent component*/
            const parseArray = [];
            /** create a copy of the markdown text*/
            let copy = this.markdown.slice(0);

            /** create array with the identifiers to use for RegEx by deleting the []*/
            const tagNames = possibleCommandIdentifier.map(tag => tag.replace('[', '').replace(']', '')).join('|');

            /** iterate trough the whole text to find the domainCommand*/
            while (true) {
                const regex = new RegExp(`(?=\\[(${tagNames})\\])`, 'gm');
                /** minimize the copy until no elements are contained*/
                const results = copy.split(regex, 1);
                if (!results || !results.length || !results[0].length){ break;}
                copy = copy.replace(results[0], '');
                const content = this.parseLineForDomainCommand(results[0].trim());
                parseArray.push(content);
            }
            this.textWithDomainCommandFound.emit(parseArray);
        }
    }

    /**
     * @function parseLineForDomainCommand
     * @desc Couple each textLine with the domainCommand identifier to emit that to the parent component for assignment
     *       1. Check which domainCommand identifier is contained within the textLine
     *       2. Remove the domainCommand identifier from the textLine
     *       3. Create an array with first element textLine and second element the domainCommand identifier
     * @param textLine {string} from the parse function
     * @return array of the textLine with the domainCommand identifier
     */
    private parseLineForDomainCommand = (textLine: string): [string, DomainCommand] => {
        for (const domainCommand of this.domainCommands) {
            const possibleOpeningCommandIdentifier = [domainCommand.getOpeningIdentifier(), domainCommand.getOpeningIdentifier().toLowerCase(), domainCommand.getOpeningIdentifier().toUpperCase()];
            // const possibleClosingIdentifier = [domainCommand.getClosingIdentifier(), domainCommand.getClosingIdentifier().toLowerCase(), domainCommand.getClosingIdentifier().toUpperCase()];

            if (possibleOpeningCommandIdentifier.some(identifier => textLine.indexOf(identifier) !== -1)) {
               const trimmedLineWithoutIdentifier = possibleOpeningCommandIdentifier.reduce((line, identifier) => line.replace(identifier, ''), textLine).trim();
               // if (possibleClosingIdentifier.some(identifier => trimmedLineWithoutIdentifier.indexOf(identifier) !== -1)) {
                       // let trimmedLineWithoutIdentifierinTotal = possibleClosingIdentifier.reduce((line, identifier) => line.replace(identifier, ''), trimmedLineWithoutIdentifier).trim();
                       // console.log(trimmedLineWithoutIdentifierinTotal);
                        return [trimmedLineWithoutIdentifier, domainCommand];
                }
           // }
        }
        return [textLine.trim(), null];
    };

    /**
     * @function togglePreview
     * @desc Toggle the preview in the template and parse the text
     */
    togglePreview(): void {
        this.previewMode = !this.previewMode;
        this.parse();
    }

}
