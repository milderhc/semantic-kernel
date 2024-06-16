// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.samples.syntaxexamples.memory;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.KeyCredential;
import com.azure.core.util.ClientOptions;
import com.azure.core.util.MetricsOptions;
import com.azure.core.util.TracingOptions;
import com.azure.search.documents.SearchDocument;
import com.azure.search.documents.indexes.SearchIndexClientBuilder;
import com.microsoft.semantickernel.aiservices.openai.textembedding.OpenAITextEmbeddingGenerationService;
import com.microsoft.semantickernel.connectors.memory.azureaisearch.AzureAISearchVectorRecordStore;
import com.microsoft.semantickernel.connectors.memory.azureaisearch.AzureAISearchVectorStoreOptions;
import com.microsoft.semantickernel.memory.VectorStoreRecordMapper;
import com.microsoft.semantickernel.memory.recorddefinition.VectorStoreRecordDataField;
import com.microsoft.semantickernel.memory.recorddefinition.VectorStoreRecordDefinition;
import com.microsoft.semantickernel.memory.recorddefinition.VectorStoreRecordKeyField;
import com.microsoft.semantickernel.memory.recorddefinition.VectorStoreRecordVectorField;
import com.microsoft.semantickernel.memory.recordoptions.GetRecordOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Demonstrate two examples about SK Semantic Memory:
 *
 * 1. Memory using Azure Cognitive Search.
 * 2. Memory using a custom embedding generator and vector engine.
 *
 * Semantic Memory allows to store your data like traditional DBs,
 * adding the ability to query it using natural language.
 * <p>
 * You must <a href=
 * "https://learn.microsoft.com/en-us/azure/search/search-create-service-portal">
 * create an Azure Cognitive Search service in the portal</a> to run this example.
 * <p>
 * Refer to the <a href=
 * "https://github.com/microsoft/semantic-kernel/blob/experimental-java/java/samples/sample-code/README.md">
 * README</a> for configuring your environment to run the examples.
 */
public class Example14_SemanticMemory {
    private static final String MEMORY_COLLECTION_NAME = "SKGitHub";
    private static final String CLIENT_KEY = System.getenv("CLIENT_KEY");
    private static final String AZURE_CLIENT_KEY = System.getenv("AZURE_CLIENT_KEY");

    // Only required if AZURE_CLIENT_KEY is set
    private static final String CLIENT_ENDPOINT = System.getenv("CLIENT_ENDPOINT");
    private static final String MODEL_ID = System.getenv()
            .getOrDefault("MODEL_ID", "gpt-35-turbo");

    public static void main(String[] args) throws Exception {
        System.out.println("==============================================================");
        System.out.println("======== Semantic Memory using Azure Cognitive Search ========");
        System.out.println("==============================================================");

        OpenAIAsyncClient client;

        if (AZURE_CLIENT_KEY != null) {
            client = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(AZURE_CLIENT_KEY))
                    .endpoint(CLIENT_ENDPOINT)
                    .buildAsyncClient();

        } else {
            client = new OpenAIClientBuilder()
                    .credential(new KeyCredential(CLIENT_KEY))
                    .buildAsyncClient();
        }

        /* This example leverages Azure Cognitive Search to provide SK with Semantic Memory.
         *
         * Azure Cognitive Search automatically indexes your data semantically, so you don't
         * need to worry about embedding generation.
         */

        var searchClient = new SearchIndexClientBuilder()
                .endpoint(System.getenv("AZURE_AISEARCH_ENDPOINT"))
                .credential(new AzureKeyCredential(System.getenv("AZURE_AISEARCH_KEY")))
                .clientOptions(clientOptions())
                .buildAsyncClient();

        var azureAISearch = new AzureAISearchVectorRecordStore<>(searchClient, AzureAISearchVectorStoreOptions.<AzureAISearchRecord>builder()
                .withDefaultCollectionName("skgithub")
                .withRecordClass(AzureAISearchRecord.class)
//                .withRecordDefinition(VectorStoreRecordDefinition.create(
//                        List.of(
//                                new VectorStoreRecordKeyField(AzureAISearchRecord.ID),
//                                new VectorStoreRecordDataField(AzureAISearchRecord.TEXT),
//                                new VectorStoreRecordDataField(AzureAISearchRecord.DESCRIPTION, true, AzureAISearchRecord.EMBEDDING),
//                                new VectorStoreRecordVectorField(AzureAISearchRecord.EMBEDDING),
//                                new VectorStoreRecordDataField(AzureAISearchRecord.ADDITIONAL_METADATA),
//                                new VectorStoreRecordDataField(AzureAISearchRecord.EXTERNAL_SOURCE_NAME),
//                                new VectorStoreRecordDataField(AzureAISearchRecord.IS_REFERENCE)
//                        )
//                ))
                .withVectorStoreRecordMapper(VectorStoreRecordMapper.<AzureAISearchRecord, SearchDocument>builder()
                        .toStorageModelMapper(record -> {
                            SearchDocument searchDocument = new SearchDocument();
                            searchDocument.put(
                                    AzureAISearchRecord.ID,
                                    AzureAISearchRecord.encodeId(
                                            record.getId()));
                            searchDocument.put(
                                    AzureAISearchRecord.TEXT,
                                    record.getText());
                            searchDocument.put(
                                    AzureAISearchRecord.DESCRIPTION,
                                    record.getDescription());
                            searchDocument.put(
                                    AzureAISearchRecord.EMBEDDING,
                                    record.getEmbedding());
                            searchDocument.put(
                                    AzureAISearchRecord.ADDITIONAL_METADATA,
                                    record.getAdditionalMetadata());
                            searchDocument.put(
                                    AzureAISearchRecord.EXTERNAL_SOURCE_NAME,
                                    record.getExternalSourceName());
                            searchDocument.put(
                                    AzureAISearchRecord.IS_REFERENCE,
                                    record.isReference());
                            return searchDocument;
                        })
                        .build())
                .build());

        var embeddingGeneration = OpenAITextEmbeddingGenerationService.builder()
                .withOpenAIAsyncClient(client)
                .withModelId("text-embedding-ada-002")
                .build();

//        azureAISearch.deleteAsync("id_1", null).block();
//
        var id = "aHR0cHM6Ly9naXRodWIuY29tL21pY3Jvc29mdC9zZW1hbnRpYy1rZXJuZWwvYmxvYi9tYWluL1JFQURNRS5tZA==";
        var result = azureAISearch.getAsync(id, null).block();
//
//        storeData(azureAISearch, embeddingGeneration, sampleData());
//        storeData(azureAISearch, embeddingGeneration, sampleDataWithNoMapping());
    }

    private static Collection<String> storeData(
            AzureAISearchVectorRecordStore<AzureAISearchRecord> azureAISearch,
            OpenAITextEmbeddingGenerationService embeddingGeneration,
            Map<String, String> data) {

        List<String> ids = new ArrayList<>();

        data.entrySet().forEach(entry -> {
            System.out.println("Save '" + entry.getKey() + "' to memory.");

            var embedding = embeddingGeneration.generateEmbeddingsAsync(Collections.singletonList(entry.getValue())).block();
            var saved = azureAISearch.upsertAsync(new AzureAISearchRecord(
                    entry.getKey(),
                    entry.getValue(),
                    entry.getValue(),
                    null,
                    embedding.get(0).getVector(),
                    "GitHub",
                    false), null).block();

            ids.add(saved);
        });

        return ids;
    }

    private static ClientOptions clientOptions() {
        return new ClientOptions()
                .setTracingOptions(new TracingOptions())
                .setMetricsOptions(new MetricsOptions())
                .setApplicationId("Semantic-Kernel");
    }

    private static Map<String, String> sampleData() {
        return Arrays.stream(new String[][]{
                {"https://github.com/microsoft/semantic-kernel/blob/main/README.md", "README: Installation, getting started, and how to contribute"},
                {"https://github.com/microsoft/semantic-kernel/blob/main/samples/notebooks/dotnet/02-running-prompts-from-file.ipynb", "Jupyter notebook describing how to pass prompts from a file to a semantic skill or function"},
                {"https://github.com/microsoft/semantic-kernel/blob/main/samples/notebooks/dotnet/00-getting-started.ipynb", "Jupyter notebook describing how to get started with the Semantic Kernel"},
                {"https://github.com/microsoft/semantic-kernel/tree/main/samples/skills/ChatSkill/ChatGPT", "Sample demonstrating how to create a chat skill interfacing with ChatGPT"},
                {"https://github.com/microsoft/semantic-kernel/blob/main/dotnet/src/SemanticKernel/Memory/VolatileMemoryStore.cs", "C# class that defines a volatile embedding store"},
                {"https://github.com/microsoft/semantic-kernel/blob/main/samples/dotnet/KernelHttpServer/README.md", "README: How to set up a Semantic Kernel Service API using Azure Function Runtime v4"},
                {"https://github.com/microsoft/semantic-kernel/blob/main/samples/apps/chat-summary-webapp-react/README.md", "README: README associated with a sample chat summary react-based webapp"},
        }).collect(Collectors.toMap(element -> element[0], element -> element[1]));
    }

    private static Map<String, String> sampleDataWithNoMapping() {
        return Arrays.stream(new String[][]{
                {"id_1", "This is test 1"},
                {"id_2", "This is test 2"},
        }).collect(Collectors.toMap(element -> element[0], element -> element[1]));
    }
}
