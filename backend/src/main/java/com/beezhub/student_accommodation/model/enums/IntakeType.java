package com.beezhub.student_accommodation.model.enums;

/**
 * How an institution splits its academic year (GH-494). A year holds one Annual intake, two
 * Semesters or three Trimesters, or any mix the institution needs.
 */
public enum IntakeType {
    ANNUAL("Annual", 1),
    SEMESTER("Semester", 2),
    TRIMESTER("Trimester", 3);

    private final String label;
    private final int partsPerYear;

    IntakeType(String label, int partsPerYear) {
        this.label = label;
        this.partsPerYear = partsPerYear;
    }

    /** How many intakes of this type a pattern makes in one year. */
    public int getPartsPerYear() {
        return partsPerYear;
    }

    /** The name a pattern gives the {@code part}th intake (1-based): "Annual", "Semester 2". */
    public String nameFor(int part) {
        return partsPerYear == 1 ? label : label + " " + part;
    }
}
