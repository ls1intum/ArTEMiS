package de.tum.in.www1.artemis.domain.enumeration;

public enum RepositoryType {

    TEMPLATE("exercise"), SOLUTION("solution"), TESTS("tests");

    private String name;

    RepositoryType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
