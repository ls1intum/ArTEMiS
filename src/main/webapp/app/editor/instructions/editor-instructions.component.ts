import { Participation } from '../../entities/participation';
import { JhiAlertService } from 'ng-jhipster';
import { TranslateService } from '@ngx-translate/core';
import { AfterViewInit, Component, ElementRef, Input, OnChanges, OnDestroy, OnInit, Renderer2, SimpleChanges } from '@angular/core';
import { WindowRef } from '../../shared/websocket/window.service';
import { RepositoryFileService, RepositoryService } from '../../entities/repository/repository.service';
import { EditorComponent } from '../editor.component';
import { EditorService } from '../editor.service';
import { JhiWebsocketService } from '../../shared';
import { ParticipationResultService, Result, ResultService } from '../../entities/result';
import { Feedback } from '../../entities/feedback';
import { EditorInstructionsResultDetailComponent } from './editor-instructions-result-detail';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import * as interact from 'interactjs';
import * as Remarkable from 'remarkable';

@Component({
    selector: 'jhi-editor-instructions',
    templateUrl: './editor-instructions.component.html',
    providers: [
        JhiAlertService,
        WindowRef,
        RepositoryService,
        ResultService,
        ParticipationResultService,
        EditorService
    ]
})

export class EditorInstructionsComponent implements OnInit, AfterViewInit, OnChanges, OnDestroy {

    isLoading = false;
    haveDetailsBeenLoaded = false;
    markDown: Remarkable;
    readMeFileRawContent: string;
    readMeFileRenderedContent: string;
    resultDetails: Feedback[];
    steps = [];

    /** Resizable sizing constants **/
    initialInstructionsWidth: number;
    minInstructionsWidth: number;

    // Can be used to remove the click listeners for result details
    listenerRemoveFunctions: Function[];

    @Input() participation: Participation;
    @Input() latestResult: Result;

    constructor(private parent: EditorComponent,
                private $window: WindowRef,
                private jhiWebsocketService: JhiWebsocketService,
                private translateService: TranslateService,
                private repositoryService: RepositoryService,
                private repositoryFileService: RepositoryFileService,
                private resultService: ResultService,
                private editorService: EditorService,
                private modalService: NgbModal,
                private elRef: ElementRef,
                private renderer: Renderer2) {}

    /**
     * @function ngOnInit
     * @desc Initializes the Remarkable object and loads the repository README.md file
     */
    ngOnInit(): void {
        // Initialize array for listener remove functions
        this.listenerRemoveFunctions = [];
        this.setupMarkDown();
        this.loadReadme();
    }

    /**
     * @function ngAfterViewInit
     * @desc After the view was initialized, we create an interact.js resizable object,
     *       designate the edges which can be used to resize the target element and set min and max values.
     *       The 'resizemove' callback function processes the event values and sets new width and height values for the element.
     */
    ngAfterViewInit(): void {
        this.initialInstructionsWidth = this.$window.nativeWindow.screen.width - 300 / 2;
        this.minInstructionsWidth = 300;
        interact('.resizable-instructions')
            .resizable({
                // Enable resize from left edge; triggered by class rg-left
                edges: { left: '.rg-left', right: false, bottom: false, top: false },
                // Set maximum width
                restrictSize: {
                    min: { width: this.minInstructionsWidth },
                    max: { width: this.initialInstructionsWidth }
                },
                inertia: true,
            }).on('resizemove', function(event) {
            const target = event.target;
            // Update element size
            target.style.width  = event.rect.width + 'px';
            target.style.height = event.rect.height + 'px';
        });
    }

    /**
     * @function ngOnChanges
     * @desc New participation received => initialize new Remarkable object and load new README.md file
     *       New latestResult received => load details for result
     * @param {SimpleChanges} changes
     */
    ngOnChanges(changes: SimpleChanges): void {
        if (changes.participation && this.participation) {
            // Initialize array for listener remove functions
            this.listenerRemoveFunctions = [];
            this.setupMarkDown();
            this.loadReadme();
        }

        if (changes.latestResult && changes.latestResult.currentValue && !this.isLoading) {
            // New result available
            this.loadResultsDetails();
        }
    }

    /**
     * @function loadReadme
     * @desc Gets the README.md file from our repository and starts the rendering process
     */
    loadReadme() {
        // Only do this if we already received a participation object from parent
        if (this.participation) {
            this.repositoryFileService.get(this.participation.id, 'README.md').subscribe( fileObj => {
                this.readMeFileRawContent = fileObj.fileContent;
                this.renderReadme();
            }, err => {
                console.log('Error while getting README.md file!', err);
            });
        }
    }

    /**
     * @function loadResultDetails
     * @desc Fetches details for the result (if we received one) => Input latestResult
     */
    loadResultsDetails() {
        if (!this.latestResult) {
            return;
        }
        this.isLoading = true;

        this.resultService.details(this.latestResult.id).subscribe( resultDetails => {
            this.resultDetails = resultDetails.body;
            this.haveDetailsBeenLoaded = true;
            this.renderReadme();
            this.isLoading = false;
        }, err => {
            console.log('Error while loading result details!', err);
        });
    }

    /**
     * @function setupMarkDown
     * @desc Initializes the Remarkable object and registers our custom parsing and rendering rules
     * Information regarding the syntax for the parser and stateBlock: https://github.com/jonschlinkert/remarkable/tree/master/docs
     */
    setupMarkDown() {
        this.markDown = new Remarkable();
        this.markDown.inline.ruler.before('text', 'testsStatus', this.remarkableTestsStatusParser.bind(this), {});
        this.markDown.block.ruler.before('paragraph', 'plantUml', this.remarkablePlantUmlParser.bind(this), {});
        this.markDown.renderer.rules['testsStatus'] = this.remarkableTestsStatusRenderer.bind(this);
        this.markDown.renderer.rules['plantUml'] = this.remarkablePlantUmlRenderer.bind(this);
    }

    /**
     * @function renderReadme
     * @desc Prepares and starts the rendering process of the README.md file
     */
    renderReadme() {
        this.isLoading = true;
        // Reset steps array
        this.steps = [];
        // Render README.md file via Remarkable
        this.readMeFileRenderedContent = this.markDown.render(this.readMeFileRawContent);
        this.isLoading = false;

        // Detach test status click listeners if already initialized; if not, set it empty
        if (this.listenerRemoveFunctions && this.listenerRemoveFunctions.length) {
            this.removeTestStatusClickListeners();
        } else {
            // Making sure the array is initialized and empty
            this.listenerRemoveFunctions = [];
        }

        // Since our rendered markdown file gets inserted into the DOM after compile time, we need to register click events for test cases manually
        const testStatusDOMElements = this.elRef.nativeElement.querySelectorAll('.test-status');
        testStatusDOMElements.forEach( element => {
            const listenerRemoveFunction = this.renderer.listen(element, 'click', event => {
                // Extract the data attribute for tests and open the details popup with it
                const tests = event.target.parentElement.getAttribute('data-tests');
                this.showDetailsForTests(this.latestResult, tests);
            });
            this.listenerRemoveFunctions.push(listenerRemoveFunction);
        });

        if (!this.haveDetailsBeenLoaded) {
            this.loadResultsDetails();
        }
    }

    /**
     * @function removeTestStatusClickListeners
     * @desc Detaches all click listeners for test status links
     */
    removeTestStatusClickListeners() {
        for (const listenerRemoveFunction of this.listenerRemoveFunctions) {
            // Call each removal function to detach the click listener from DOM
            listenerRemoveFunction();
        }
        // Set function array to empty
        this.listenerRemoveFunctions = [];
    }

    /**
     * @function remarkablePlantUmlParser
     * @desc Parser rule for Remarkable custom token PlantUml
     * @param state
     * @param startLine
     * @param endLine
     * @param silent
     */
    remarkablePlantUmlParser = function(state, startLine, endLine, silent) {

        /**
         * Excerpt from the remarkable documentation regarding the stateBlock (param state):
         * src: the complete string the parser is currently working on
         * parser: The current block parser (here to make nested calls easier)
         * env: a namespaced data key-value store to allow core rules to exchange data
         * tokens: the tokens generated by the parser up to now, you will emit new tokens by calling push(newToken) on this
         * bMarks: a collection marking for each line the position of its start in src
         * eMarks: a collection marking for each line the position of its end in src
         * tShift: a collection marking for each line, how much spaces were used to indent it
         * level: the nested level for the current block
         */

        const shift = state.tShift[startLine];
        const max = state.eMarks[startLine];
        let pos = state.bMarks[startLine];
        pos += shift;

        if (shift > 3 || pos + 2 >= max) { return false; }
        if (state.src.charCodeAt(pos) !== 0x40/* @ */) { return false; }

        const char = state.src.charCodeAt(pos + 1);

        // Is the current char a 's'?
        if (char === 0x73) {
            // Probably start or end of tag
            if (char === 0x73/* \ */) {
                // opening tag
                const match = state.src.slice(pos, max).match(/^@startuml/);
                if (!match) { return false; }
            }
            if (silent) { return true; }
        } else {
            return false;
        }

        // If we are here - we detected PlantUML block.
        // Let's roll down till empty line (block end).
        let nextLine = startLine + 1;
        while (nextLine < state.lineMax && !state.src.slice(state.bMarks[nextLine], state.bMarks[nextLine + 1]).match(/^@enduml/)) {
            nextLine++;
        }

        state.line = nextLine + 1;
        // Insert the plantUml token to our rendered tokens
        state.tokens.push({
            type: 'plantUml',
            level: state.level,
            lines: [ startLine, state.line ],
            content: state.getLines(startLine, state.line, 0, true)
        });

        return true;
    };

    /**
     * @function remarkablePlantUmlRenderer
     * @desc Renderer rule for Remarkable custom token PlantUml
     * @param tokens the tokens generated by the parser up to now
     * @param id
     */
    remarkablePlantUmlRenderer(tokens, id) {
        let plantUml = tokens[id].content;

        plantUml = plantUml.replace('@startuml', '@startuml\nskinparam shadowing false\nskinparam classBorderColor black\nskinparam classArrowColor black\nskinparam DefaultFontSize 14\nskinparam ClassFontStyle bold\nskinparam classAttributeIconSize 0\nhide empty members\n');

        // Provide this reference inside replace callback function
        const that = this;
        plantUml = plantUml.replace(/testsColor\(([^)]+)\)/g, function(match, capture) {
            const tests = capture.split(',');
            const status = that.statusForTests(tests);
            return status['done'] ? 'green' : 'red';
        });

        /**
         * Explanation: This call fetches the plantUml png as base64 string; the function returns and inserts an empty img tag with a placeholder
         * When the promise is fulfilled, the src-attribute of the img element is being set with the returned value
         */
        this.editorService.getPlantUmlImage(plantUml).subscribe( plantUmlSrcAttribute => {
            // Assign plantUmlSrcAttribute as src attribute to our img element
            document.getElementById('plantUml' + id).setAttribute('src', 'data:image/jpeg;base64,' + plantUmlSrcAttribute);
        }, err => {
            console.log('Error getting plantUmlImage', err);
        });

        return "<img id='plantUml" + id + "' alt='plantUml'" + id + " '/>";
    }

    /**
     * @function remarkableTestsStatusParser
     * @desc Parser rule for Remarkable custom token TestStatus
     * @param state
     * @param silent
     */
    remarkableTestsStatusParser(state, silent: boolean) {

        const regex = /^✅\[([^\]]*)\]\s*\(([^)]+)\)/;

        // It is surely not our rule, so we can stop early
        if (state.src[state.pos] !== '✅') { return false; }

        const match = regex.exec(state.src.slice(state.pos));
        if (!match) { return false; }

        // In silent mode it shouldn't output any tokens or modify pending
        if (!silent) {
            // Insert the testsStatus token to our rendered tokens
            state.push({
                type: 'testsStatus',
                title: match[1],
                tests: match[2].split(','),
                level: state.level,
            });
        }

        // Every rule should set state.pos to a position after token's contents
        state.pos += match[0].length;

        return true;
    }

    /**
     * @function remarkableTestsStatusRenderer
     * @desc Renderer rule for Remarkable custom token TestStatus
     * Builds the raw html for test case status; also inserts the test details link
     * @param tokens
     * @param id
     * @param options
     * @param env
     */
    remarkableTestsStatusRenderer(tokens, id: number, options, env) {
        const tests = tokens[0].tests;
        const status = this.statusForTests(tests);

        let text = '<strong>';

        text += status['done'] ?
            '<i class="fa fa-lg fa-check-circle-o text-success" style="font-size: 1.7em;"></i>' :
            '<i class="fa fa-lg fa-times-circle-o text-danger" style="font-size: 1.7em;"></i>';
        text += ' ' + tokens[0].title;
        text += '</strong>: ';
        // If the test is not done, we set the 'data-tests' attribute to the a-element, which we later use for the details dialog
        text += status['done'] ?
            ' <span class="text-success">' + status['label'] + '</span>' :
            '<a data-tests="' + tests.toString() + '" class="test-status"><span class="text-danger">' + status['label'] + '</span></a>';
        text += '<br />';

        this.steps.push({
            title: tokens[0].title,
            done: status['done']
        });

        return text;
    }

    /**
     * @function statusForTests
     * @desc Callback function for renderers to set the appropiate test status
     * @param tests
     */
    statusForTests(tests): object {
        const translationBasePath = 'arTeMiSApp.editor.testStatusLabels.';
        let done = false;
        let label = this.translateService.instant('arTeMiSApp.editor.testStatusLabels.noResult');
        const totalTests = tests.length;

        if (this.resultDetails && this.resultDetails.length > 0) {
            let failedTests = 0;
            for (const test of tests) {
                for (const result of this.resultDetails) {
                    if (result.text === test) {
                        failedTests++;
                    }
                }
            }

            // Exercise is done if it was completed successfully or no tests have failed
            done = (this.latestResult && this.latestResult.successful) || failedTests === 0;
            if (totalTests === 1) {
                if (done) {
                    label = this.translateService.instant(translationBasePath + 'testPassing');
                } else {
                    label = this.translateService.instant(translationBasePath + 'testFailing');
                }
            } else {
                if (done) {
                    label = this.translateService.instant(translationBasePath + 'totalTestsPassing', {totalTests});
                } else {
                    label = this.translateService.instant(translationBasePath + 'totalTestsFailing', {totalTests, failedTests});
                }
            }

        } else if (this.latestResult && this.latestResult.successful) {
            done = true;
            label = this.translateService.instant(translationBasePath + 'testPassing');
        }

        return {
            done,
            label
        };
    }

    /**
     * @function showDetailsForTests
     * @desc Opens the ResultDetailComponent as popup; displays test results
     * @param result {Result} Result object, mostly latestResult
     * @param tests {string} Identifies the testcase
     */
    showDetailsForTests(result: Result, tests: string) {
        if (!result) {
            return;
        }
        const modalRef = this.modalService.open(EditorInstructionsResultDetailComponent, {keyboard: true, size: 'lg'});
        modalRef.componentInstance.result = result;
        modalRef.componentInstance.tests = tests;
    }

    /**
     * @function toggleEditorCollapse
     * @desc Calls the parent (editorComponent) toggleCollapse method
     * @param $event
     * @param {boolean} horizontal
     */
    toggleEditorCollapse($event: any, horizontal: boolean) {
        this.parent.toggleCollapse($event, horizontal);
    }

    /**
     * @function ngOnDestroy
     * @desc Removes all click listener when destroying the component to avoid performance issues
     */
    ngOnDestroy(): void {
        this.removeTestStatusClickListeners();
    }
}
