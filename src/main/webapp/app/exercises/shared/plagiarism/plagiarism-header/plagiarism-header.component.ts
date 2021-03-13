import { Component, Input } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { PlagiarismStatus } from 'app/exercises/shared/plagiarism/types/PlagiarismStatus';
import { PlagiarismComparison } from 'app/exercises/shared/plagiarism/types/PlagiarismComparison';

// False-positives:
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { TextSubmissionElement } from 'app/exercises/shared/plagiarism/types/text/TextSubmissionElement';
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { ModelingSubmissionElement } from 'app/exercises/shared/plagiarism/types/modeling/ModelingSubmissionElement';
import { SERVER_API_URL } from 'app/app.constants';

@Component({
    selector: 'jhi-plagiarism-header',
    styleUrls: ['./plagiarism-header.component.scss'],
    templateUrl: './plagiarism-header.component.html',
})
export class PlagiarismHeaderComponent {
    @Input() comparison: PlagiarismComparison<TextSubmissionElement | ModelingSubmissionElement>;
    @Input() splitControlSubject: Subject<string>;

    private resourceUrl = SERVER_API_URL + 'api/plagiarism-comparisons';

    constructor(private http: HttpClient) {}

    /**
     * Set the status of the currently selected comparison to CONFIRMED.
     */
    confirmPlagiarism() {
        this.updatePlagiarismStatus(PlagiarismStatus.CONFIRMED);
    }

    /**
     * Set the status of the currently selected comparison to DENIED.
     */
    denyPlagiarism() {
        this.updatePlagiarismStatus(PlagiarismStatus.DENIED);
    }

    /**
     * Update the status of the currently selected comparison.
     * @param status the new status of the comparison
     */
    updatePlagiarismStatus(status: PlagiarismStatus) {
        return this.http
            .put<void>(
                `${this.resourceUrl}/${this.comparison.id}/status`,
                { status },
                {
                    observe: 'response',
                },
            )
            .subscribe(() => {
                this.comparison.status = status;
            });
    }

    expandSplitPane(pane: 'left' | 'right') {
        this.splitControlSubject.next(pane);
    }

    resetSplitPanes() {
        this.splitControlSubject.next('even');
    }
}
