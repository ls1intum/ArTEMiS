import { Component, Input, OnInit } from '@angular/core';
import { RatingService } from 'app/exercises/shared/rating/rating.service';
import { StarRatingComponent } from 'ng-starrating';
import { Result } from 'app/entities/result.model';
import { Rating } from 'app/entities/rating.model';

@Component({
    selector: 'jhi-rating',
    templateUrl: './rating.component.html',
    styleUrls: ['./rating.component.scss'],
})
export class RatingComponent implements OnInit {
    public rating: Rating;
    public disableRating = false;
    @Input() result: Result;

    constructor(public ratingService: RatingService) {}

    ngOnInit(): void {
        if (!this.result || !this.result.submission) {
            return;
        }
        this.ratingService.getRating(this.result.id).subscribe((rating) => {
            if (rating) {
                this.rating = rating;
            } else {
                this.rating = new Rating(this.result, 0);
            }
        });
    }

    /**
     * Update/Create new Rating for the result
     * @param $event - starRating component that holds new rating value
     */
    onRate($event: { oldValue: number; newValue: number; starRating: StarRatingComponent }) {
        // block rating to prevent double sending of post request
        if (this.disableRating) {
            return;
        }

        // update feedback locally
        this.rating.rating = $event.newValue;

        // set/update feedback on the server
        if (this.rating.id) {
            this.ratingService.updateRating(this.rating).subscribe((rating) => {
                this.rating = rating;
            });
        } else {
            this.disableRating = true;
            this.ratingService.createRating(this.rating).subscribe((rating) => {
                this.rating = rating;
                this.disableRating = false;
            });
        }
    }
}
