package com.kuta.akka.gateway.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public class MockDefinition {
	 private final String path;
	  private final List<JsonNode> requests;
	  private final List<JsonNode> responses;

	  public MockDefinition(@JsonProperty("path") String path,
	                        @JsonProperty("requests") List<JsonNode> requests,
	                        @JsonProperty("responses") List<JsonNode> responses) {
	    this.path = path;
	    this.requests = requests;
	    this.responses = responses;
	  }

	  public String getPath() {
	    return path;
	  }

	  public List<JsonNode> getRequests() {
	    return requests;
	  }

	  public List<JsonNode> getResponses() {
	    return responses;
	  }
}
