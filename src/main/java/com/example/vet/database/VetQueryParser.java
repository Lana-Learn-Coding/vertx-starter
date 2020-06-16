package com.example.vet.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build a elastic search json query from query param string.
 * <p>
 * Query format
 * [query_type?]=[field_name]:[query_value]
 * [query_type?]=[field_name].[query_field_name]:[query_value]
 * Default query_type is match
 * <p>
 * TODO: support string literal and typing
 */
public class VetQueryParser {
    private enum Operators {
        AND(";(?![^\\(]*\\))"),
        OR(",(?![^\\(]*\\))"),
        NOT("!(?![^\\(]*\\))"),
        BRACKET("(?<=\\().+?(?=\\))");

        public final String pattern;

        Operators(String pattern) {
            this.pattern = pattern;
        }
    }

    private enum Queries {
        SHORTHANDED("^.+(\\..+)?:.+$"),
        FULL("^.+=.+(\\..+)?:.+$");

        public final String pattern;

        Queries(String pattern) {
            this.pattern = pattern;
        }
    }

    public static JsonObject parseQuery(String rawQuery) {
        JsonObject queryTree = new JsonObject();
        boolean isIncludeOR = matchContains(rawQuery, Operators.OR.pattern);
        boolean isIncludeAND = matchContains(rawQuery, Operators.AND.pattern);
        boolean isIncludeNOT = matchContains(rawQuery, Operators.NOT.pattern);
        boolean isIncludeBracket = matchContains(rawQuery, Operators.BRACKET.pattern);

        // no logic operator left
        if (!isIncludeAND && !isIncludeOR && !isIncludeNOT && !isIncludeBracket) {
            return parseQueryClause(rawQuery);
        }

        if (isIncludeOR) {
            return queryTree.put("bool", parseLogicOr(rawQuery));
        }
        if (isIncludeAND) {
            return queryTree.put("bool", parseLogicAnd(rawQuery));
        }
        if (isIncludeNOT) {
            return queryTree.put("bool", parseLogicNot(rawQuery));
        }

        // unwrap bracket
        Pattern pattern = Pattern.compile(Operators.BRACKET.pattern);
        Matcher matcher = pattern.matcher(rawQuery);
        boolean isMatch = matcher.find();
        if (!isMatch) {
            throw new RuntimeException("Malformed Bracket: " + rawQuery);
        }
        return parseQuery(matcher.group());
    }

    private static JsonObject parseQueryClause(String rawQuery) {
        final String DEFAULT_CLAUSE = "match";
        boolean isFullQuery = matchContains(rawQuery, Queries.FULL.pattern);
        boolean isShorthandedQuery = matchContains(rawQuery, Queries.SHORTHANDED.pattern);
        if (!isFullQuery && !isShorthandedQuery) {
            throw new RuntimeException("Malformed query: " + rawQuery);
        }

        String queryClause = DEFAULT_CLAUSE;
        if (isFullQuery) {
            String[] clauseAndValue = rawQuery.split("=");
            queryClause = clauseAndValue[0];
            rawQuery = clauseAndValue[1];
        }

        boolean isNestedQuery = rawQuery.contains(".");
        JsonObject queryClauseTree = new JsonObject();
        // TODO: implement safe parsing
        if (isNestedQuery) {
            // Unsafe key.subkey:value
            String[] values = rawQuery.split("[.:]");
            queryClauseTree.put(values[0], new JsonObject().put(values[1], values[2]));
        } else {
            // Unsafe key:value
            String[] keyAndValue = rawQuery.split(":");
            queryClauseTree.put(keyAndValue[0], keyAndValue[1]);
        }
        return new JsonObject().put(queryClause, queryClauseTree);
    }

    private static JsonObject parseLogicAnd(String rawQuery) {
        JsonObject subTree = new JsonObject();
        String[] rawQueries = rawQuery.split(Operators.AND.pattern);
        JsonArray parsedQueries = new JsonArray();
        for (String query : rawQueries) {
            parsedQueries.add(parseQuery(query));
        }
        return subTree.put("must", parsedQueries);
    }

    private static JsonObject parseLogicOr(String rawQuery) {
        JsonObject subTree = new JsonObject();
        String[] rawQueries = rawQuery.split(Operators.OR.pattern);
        JsonArray parsedQueries = new JsonArray();
        for (String query : rawQueries) {
            parsedQueries.add(parseQuery(query));
        }
        return subTree.put("should", parsedQueries);
    }

    private static JsonObject parseLogicNot(String rawQuery) {
        JsonObject subTree = new JsonObject();
        String[] rawQueries = rawQuery.split(Operators.NOT.pattern);
        if (rawQueries.length != 2) {
            throw new RuntimeException("Malformed operator: " + rawQuery);
        }
        return subTree.put("must_not", parseQuery(rawQueries[1]));
    }

    // match substring
    private static boolean matchContains(String string, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }
}