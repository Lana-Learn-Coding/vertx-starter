package com.example.vet.service.impl;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

public class VetESServiceMapper {
    static JsonArray mapToJsonArray(SearchResponse searchResponse) {
        return mapToJsonArray(searchResponse.getHits());
    }

    static JsonArray mapToJsonArray(SearchHits hits) {
        JsonArray array = new JsonArray();
        hits.forEach(hit -> array.add(mapToJsonObject(hit)));
        return array;
    }

    static JsonObject mapToJsonObject(SearchHit hit) {
        JsonObject json = new JsonObject(hit.sourceAsMap());
        json.put("_id", hit.id());
        return json;
    }
}
