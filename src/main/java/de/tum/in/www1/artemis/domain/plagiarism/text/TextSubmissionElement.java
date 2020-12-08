package de.tum.in.www1.artemis.domain.plagiarism.text;

import jplag.Token;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismSubmissionElement;

public class TextSubmissionElement extends PlagiarismSubmissionElement {

    private int column;

    private int line;

    private String file;

    private int type;

    private int length;

    /**
     * Create a new TextSubmissionElement instance from an existing JPlag Token
     *
     * @param token the JPlag Token to create the TextSubmissionElement from
     * @return a new TextSubmissionElement instance
     */
    public static TextSubmissionElement fromJPlagToken(Token token) {
        TextSubmissionElement textSubmissionElement = new TextSubmissionElement();

        textSubmissionElement.setColumn(token.getColumn());
        textSubmissionElement.setLine(token.getLine());
        textSubmissionElement.setFile(token.file);
        textSubmissionElement.setType(token.type);
        textSubmissionElement.setLength(token.getLength());

        return textSubmissionElement;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
