import { AfterViewInit, Component, Input, OnInit } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { Exercise } from 'app/entities/exercise';
import { ArtemisMarkdown } from 'app/components/util/markdown.service';
import interact from 'interactjs';

@Component({
    selector: 'jhi-assessment-instructions',
    templateUrl: './assessment-instructions.component.html',
    styleUrls: ['./assessment-instructions.scss'],
})
export class AssessmentInstructionsComponent implements OnInit, AfterViewInit {
    @Input() exercise: Exercise;
    @Input() collapsed = false;

    formattedProblemStatement: SafeHtml | null;
    formattedGradingCriteria: SafeHtml | null;

    constructor(private artemisMarkdown: ArtemisMarkdown) {}

    /**
     * Assigns formatted problem statement and formatted grading criteria on component initialization
     */
    ngOnInit() {
        this.formattedProblemStatement = this.artemisMarkdown.safeHtmlForMarkdown(this.exercise.problemStatement);
        this.formattedGradingCriteria = this.artemisMarkdown.safeHtmlForMarkdown(this.exercise.gradingInstructions);
    }

    /**
     * Configures interact to make instructions expandable
     */
    ngAfterViewInit(): void {
        interact('.expanded-instructions')
            .resizable({
                edges: { left: '.draggable-left', right: false, bottom: false, top: false },
                modifiers: [
                    // Set maximum width
                    interact.modifiers!.restrictSize({
                        min: { width: 215, height: 0 },
                        max: { width: 1000, height: 2000 },
                    }),
                ],
                inertia: true,
            })
            .on('resizestart', function(event: any) {
                event.target.classList.add('card-resizable');
            })
            .on('resizeend', function(event: any) {
                event.target.classList.remove('card-resizable');
            })
            .on('resizemove', function(event: any) {
                const target = event.target;
                target.style.width = event.rect.width + 'px';
            });
    }
}
