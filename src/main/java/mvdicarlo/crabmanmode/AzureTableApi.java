package mvdicarlo.crabmanmode;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AzureTableApi {
    private final OkHttpClient httpClient;
    private final String sasUrl;
    private final Gson gson;
    private Integer pageSize;

    public AzureTableApi(String sasUrl, Gson gson, OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.sasUrl = sasUrl;
        this.gson = gson;
        this.pageSize = null; // No limit by default
    }

    /**
     * Sets the page size for list operations. Useful for testing pagination.
     * @param pageSize The maximum number of entities to return per page, or null for no limit
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Gets the current page size setting.
     * @return The page size, or null if no limit is set
     */
    public Integer getPageSize() {
        return pageSize;
    }

    private static class AzureTableResponse {
        List<Map<String, Object>> value;
    }

    private String buildUrl(String path, String queryParams) {
        String baseUrl = sasUrl.split("\\?")[0];
        String existingParams = sasUrl.split("\\?")[1];
        return baseUrl + path + "?" + existingParams + (queryParams.isEmpty() ? "" : "&" + queryParams);
    }

    private static class RequestResult {
        final String body;
        final String nextPartitionKey;
        final String nextRowKey;

        RequestResult(String body, String nextPartitionKey, String nextRowKey) {
            this.body = body;
            this.nextPartitionKey = nextPartitionKey;
            this.nextRowKey = nextRowKey;
        }

        boolean hasContinuation() {
            return nextPartitionKey != null && nextRowKey != null;
        }
    }

    private String sendRequest(Request request) throws Exception {
        return sendRequestWithContinuation(request).body;
    }

    private RequestResult sendRequestWithContinuation(Request request) throws Exception {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String body = response.body().string();
                String nextPartitionKey = response.header("x-ms-continuation-NextPartitionKey");
                String nextRowKey = response.header("x-ms-continuation-NextRowKey");
                return new RequestResult(body, nextPartitionKey, nextRowKey);
            } else {
                throw new Exception("Request failed: " + response.body().string());
            }
        }
    }

    private List<UnlockedItemEntity> parseJsonListResponse(String jsonResponse) {
        Type type = new TypeToken<AzureTableResponse>() {
        }.getType();
        AzureTableResponse response = gson.fromJson(jsonResponse, type);
        return response.value.stream()
                .map(UnlockedItemEntity::fromMap)
                .collect(Collectors.toList());
    }

    private UnlockedItemEntity parseJsonResponse(String jsonResponse) {
        Map<String, Object> map = gson.fromJson(jsonResponse, new TypeToken<Map<String, Object>>() {
        }.getType());
        return UnlockedItemEntity.fromMap(map);
    }

    public UnlockedItemEntity getEntity(String partitionKey, String rowKey) throws Exception {
        String url = buildUrl("(PartitionKey='" + partitionKey + "',RowKey='" + rowKey + "')", "");
        Request request = createRequestBuilder(url)
                .get()
                .build();
        String jsonResponse = sendRequest(request);
        UnlockedItemEntity entity = parseJsonResponse(jsonResponse);
        return entity;
    }

    public List<UnlockedItemEntity> listEntities(String query) throws Exception {
        List<UnlockedItemEntity> allEntities = new ArrayList<>();
        String nextPartitionKey = null;
        String nextRowKey = null;

        do {
            StringBuilder queryParams = new StringBuilder("$filter=" + query);
            if (pageSize != null) {
                queryParams.append("&$top=").append(pageSize);
            }
            if (nextPartitionKey != null && nextRowKey != null) {
                queryParams.append("&NextPartitionKey=").append(nextPartitionKey);
                queryParams.append("&NextRowKey=").append(nextRowKey);
            }

            String url = buildUrl("", queryParams.toString());
            Request request = createRequestBuilder(url)
                    .get()
                    .build();
            RequestResult result = sendRequestWithContinuation(request);
            allEntities.addAll(parseJsonListResponse(result.body));

            nextPartitionKey = result.nextPartitionKey;
            nextRowKey = result.nextRowKey;
        } while (nextPartitionKey != null && nextRowKey != null);

        return allEntities;
    }

    public List<UnlockedItemEntity> listEntities() throws Exception {
        List<UnlockedItemEntity> allEntities = new ArrayList<>();
        String nextPartitionKey = null;
        String nextRowKey = null;

        do {
            StringBuilder queryParams = new StringBuilder();
            if (pageSize != null) {
                queryParams.append("$top=").append(pageSize);
            }
            if (nextPartitionKey != null && nextRowKey != null) {
                if (queryParams.length() > 0) {
                    queryParams.append("&");
                }
                queryParams.append("NextPartitionKey=").append(nextPartitionKey);
                queryParams.append("&NextRowKey=").append(nextRowKey);
            }

            String url = buildUrl("", queryParams.toString());
            Request request = createRequestBuilder(url)
                    .get()
                    .build();
            RequestResult result = sendRequestWithContinuation(request);
            allEntities.addAll(parseJsonListResponse(result.body));

            nextPartitionKey = result.nextPartitionKey;
            nextRowKey = result.nextRowKey;
        } while (nextPartitionKey != null && nextRowKey != null);

        return allEntities;
    }

    public void deleteEntity(String partitionKey, String rowKey) throws Exception {
        String url = buildUrl("(PartitionKey='" + partitionKey + "',RowKey='" + rowKey + "')", "");
        Request request = createRequestBuilder(url)
                .delete()
                .header("If-Match", "*")
                .build();
        sendRequest(request);
    }

    public void insertEntity(UnlockedItemEntity entity) throws Exception {
        String url = buildUrl("", "");
        String jsonPayload = gson.toJson(entity.toMap());
        RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonPayload);
        Request request = createRequestBuilder(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build();
        sendRequest(request);
    }

    private Request.Builder createRequestBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", "CrabManModePlugin");
    }
}