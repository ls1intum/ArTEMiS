import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs/Observable';
import { Rating } from 'app/entities/rating.model';
import { SERVER_API_URL } from 'app/app.constants';

@Injectable({
    providedIn: 'root',
})
export class RatingService {
    private ratingResourceUrl = SERVER_API_URL + 'api/rating';

    constructor(private http: HttpClient) {}

    /**
     * Update the student rating for feedback on the server.
     * @param rating - Rating for the result
     */
    setRating(rating: Rating): Observable<Rating> {
        return this.http.post<Rating>(this.ratingResourceUrl, rating);
    }

    /**
     * Get rating for "resultId" Result
     * @param resultId - Id of Result who's rating is received
     */
    getRating(resultId: number): Observable<Rating | null> {
        return this.http.get<Rating | null>(this.ratingResourceUrl + `/result/${resultId}`);
    }

    /**
     * Update rating for "resultId" Result
     * @param rating - Rating for the result
     */
    updateRating(rating: Rating): Observable<Rating> {
        return this.http.put<Rating>(this.ratingResourceUrl, rating);
    }
}
