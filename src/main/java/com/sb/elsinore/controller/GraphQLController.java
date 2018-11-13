package com.sb.elsinore.controller;

import com.sb.elsinore.graphql.SystemSettingsService;
import com.sb.elsinore.graphql.TempProbeService;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.SchemaPrinter;
import graphql.servlet.internal.GraphQLRequest;
import io.leangen.graphql.GraphQLSchemaGenerator;
import io.leangen.graphql.metadata.strategy.query.AnnotatedResolverBuilder;
import io.leangen.graphql.metadata.strategy.value.jackson.JacksonValueMapperFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@RestController
public class GraphQLController {
    private final GraphQL graphQL;

    private class GraphQLRequestData {
        public GraphQLRequestData(Map<String, Object> request) {
            Object varTemp = request.get("query");

            if (varTemp instanceof String) {
                this.query = (String) varTemp;
            }

            varTemp = request.get("operationName");
            if (varTemp instanceof String) {
                this.operationName = (String) varTemp;
            }
            varTemp = request.get("variables");
            if (varTemp instanceof Map) {
                this.variables = (Map<String, Object>) varTemp;
            }
        }
        public String query = null;
        public String operationName = null;
        public Map<String, Object> variables = null;
    }

    public GraphQLController(TempProbeService tempProbeService, SystemSettingsService systemSettingsService) {
        GraphQLSchema schema = new GraphQLSchemaGenerator()
                .withResolverBuilders(
                        //Resolve by annotations
                        new AnnotatedResolverBuilder())
                .withOperationsFromSingleton(tempProbeService)
                .withOperationsFromSingleton(systemSettingsService)
                .withValueMapperFactory(new JacksonValueMapperFactory())
                .generate();

        updateGraphQLSchema(schema);
        this.graphQL = GraphQL.newGraphQL(schema).build();
    }

    @PostMapping(value = "/graphql", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public Map<String, Object> graphql(@RequestBody Map<String, Object> request, HttpServletRequest raw) {
        GraphQLRequestData requestData = new GraphQLRequestData(request);

        ExecutionResult executionResult = this.graphQL.execute(ExecutionInput.newExecutionInput()
                .query(requestData.query)
                .operationName(requestData.operationName)
                .variables(requestData.variables)
                .context(raw)
                .build());
        return executionResult.toSpecification();
    }

    private void updateGraphQLSchema(GraphQLSchema schema) {

        String schemaString = new SchemaPrinter(
                // Tweak the options accordingly
                SchemaPrinter.Options.defaultOptions()
                        .includeScalarTypes(true)
                        .includeExtendedScalarTypes(true)
                        .includeIntrospectionTypes(true)
                        .includeSchemaDefintion(true)
        ).print(schema);

        try {
            FileWriter fileWriter = new FileWriter("graphql.schema.json");
            fileWriter.write(schemaString);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }
}
