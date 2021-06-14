package de.tum.in.www1.artemis.domain;

import javax.persistence.*;

import org.apache.commons.math3.util.Precision;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The grade step of a grading scale
 */
@Entity
@Table(name = "grade_step")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GradeStep extends DomainObject {

    @ManyToOne
    @JsonIgnoreProperties(value = "gradeSteps", allowSetters = true)
    private GradingScale gradingScale;

    @Column(name = "lower_bound_percentage")
    private double lowerBoundPercentage;

    @Column(name = "lower_bound_points")
    private Double lowerBoundPoints;

    @Column(name = "lower_bound_inclusive")
    private boolean lowerBoundInclusive = true; // default

    @Column(name = "upper_bound_percentage")
    private double upperBoundPercentage;

    @Column(name = "upper_bound_points")
    private Double upperBoundPoints;

    @Column(name = "upper_bound_inclusive")
    private boolean upperBoundInclusive = false; // default

    @Column(name = "grade_name")
    private String gradeName;

    @Column(name = "is_passing_grade")
    private boolean isPassingGrade;

    public GradingScale getGradingScale() {
        return gradingScale;
    }

    public void setGradingScale(GradingScale gradingScale) {
        this.gradingScale = gradingScale;
    }

    public double getLowerBoundPercentage() {
        return lowerBoundPercentage;
    }

    public void setLowerBoundPercentage(double lowerBoundPercentage) {
        this.lowerBoundPercentage = lowerBoundPercentage;
    }

    public Double getLowerBoundPoints() {
        return lowerBoundPoints;
    }

    public void setLowerBoundPoints(Double lowerBoundPoints) {
        this.lowerBoundPoints = lowerBoundPoints;
    }

    public boolean isLowerBoundInclusive() {
        return lowerBoundInclusive;
    }

    public void setLowerBoundInclusive(boolean lowerBoundInclusive) {
        this.lowerBoundInclusive = lowerBoundInclusive;
    }

    public double getUpperBoundPercentage() {
        return upperBoundPercentage;
    }

    public void setUpperBoundPercentage(double upperBoundPercentage) {
        this.upperBoundPercentage = upperBoundPercentage;
    }

    public Double getUpperBoundPoints() {
        return upperBoundPoints;
    }

    public void setUpperBoundPoints(Double upperBoundPoints) {
        this.upperBoundPoints = upperBoundPoints;
    }

    public boolean isUpperBoundInclusive() {
        return upperBoundInclusive;
    }

    public void setUpperBoundInclusive(boolean upperBoundInclusive) {
        this.upperBoundInclusive = upperBoundInclusive;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public boolean getIsPassingGrade() {
        return isPassingGrade;
    }

    public void setIsPassingGrade(boolean passingGrade) {
        isPassingGrade = passingGrade;
    }

    @Override
    public String toString() {
        return "GradeStep{" + "lowerBoundPercentage=" + lowerBoundPercentage + ", lowerBoundInclusive=" + lowerBoundInclusive + ", upperBoundPercentage=" + upperBoundPercentage
                + ", upperBoundInclusive=" + upperBoundInclusive + ", gradeName='" + gradeName + "'" + ", isPassingGrade=" + isPassingGrade + "}";
    }

    /**
     * Returns whether the given percentage maps to this grade step
     * - the percentage should be between the lower and upper bounds of the grade step
     * - or on the bound of it if it's inclusive
     *
     * @param percentage the percentage which has to be mapped
     * @return whether the percentage matches this grade step
     */
    public boolean matchingGradePercentage(double percentage) {
        double epsilon = 0.01d;
        if (Precision.equals(percentage, lowerBoundPercentage, epsilon)) {
            return lowerBoundInclusive;
        }
        else if (Precision.equals(percentage, upperBoundPercentage, epsilon)) {
            return upperBoundInclusive;
        }
        else {
            return percentage > lowerBoundPercentage && percentage < upperBoundPercentage;
        }
    }

    /**
     * Checks if the current grade step is valid
     * - the id should be initially null
     * - the grade name should be set and it shouldn't be empty
     * - both bounds should be between 0 and 100 (both inclusive)
     * - the lower bound should be less than or equal to the upper bound
     * - if max points exist, the point values should be set, with similar logic as the percentages
     *
     * @param maxPoints the max points for the course/exam
     * @return true if all conditions are true and false otherwise
     */
    public boolean checkValidity(int maxPoints) {
        boolean validPoints = true;
        boolean validPercentageToPointsConversion = true;
        // if max points exists
        if (maxPoints > 0) {
            // points are between 0 and max points and the lower bound is smaller than the upper bound
            validPoints = lowerBoundPoints != null && lowerBoundPoints >= 0 && upperBoundPoints != null && upperBoundPoints <= maxPoints && lowerBoundPoints < upperBoundPoints;
            double epsilon = 0.01d;
            double convertedLowerBound = (lowerBoundPercentage * maxPoints) / 100;
            double convertedUpperBound = (upperBoundPercentage * maxPoints) / 100;
            // lower and upper bound points are proportionally converted based on the max points and the percentage values
            validPercentageToPointsConversion = Precision.equals(convertedLowerBound, lowerBoundPoints, epsilon)
                    && Precision.equals(convertedUpperBound, upperBoundPoints, epsilon);
        }
        boolean validOrder = lowerBoundPercentage < upperBoundPercentage || lowerBoundPercentage == upperBoundPercentage && lowerBoundInclusive && upperBoundInclusive;
        return getId() == null && !gradeName.isBlank() && lowerBoundPercentage >= 0 && validOrder && upperBoundPercentage <= 100 && validPoints
                && validPercentageToPointsConversion;
    }

    /**
     * Checks whether two grade steps can be considered adjacent
     * - the lower grade step's upper bound should overlap with the upper one's lower bound
     * - and exactly one of the two bounds should be inclusive
     *
     * @param lowerGradeStep the grade step with lower lower bound
     * @param upperGradeStep the grade step with higher lower bound
     * @return true if grade steps are adjacent and false otherwise
     */
    public static boolean checkValidAdjacency(GradeStep lowerGradeStep, GradeStep upperGradeStep) {
        return lowerGradeStep.upperBoundInclusive != upperGradeStep.lowerBoundInclusive && lowerGradeStep.upperBoundPercentage == upperGradeStep.lowerBoundPercentage;
    }
}
