package org.sunbird.util;

public class PassbookParserHandler {

    CompetencyPassbookParser competencyPassbookParser = new CompetencyPassbookParser();

    public PassbookParser getPassbookParser(String typeName) {
        switch (typeName) {
            case "competency":
                return competencyPassbookParser;
            default:
                return null;
        }
    }
}