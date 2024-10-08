package com.linkedin.metadata.dao.search;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.metadata.dao.BaseSearchWriterDAO;
import com.linkedin.metadata.dao.utils.RecordUtils;
import javax.annotation.Nonnull;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.xcontent.XContentType;


/**
 * A {@link BaseSearchWriterDAO} that uses ElasticSearch's bulk update API.
 */
public final class ESBulkWriterDAO<DOCUMENT extends RecordTemplate> extends BaseSearchWriterDAO<DOCUMENT> {
  private static final int MAX_RETRIES = 3;

  private final BulkProcessor _bulkProcessor;
  private final String _indexName;

  /**
   * Constructor.
   *
   * @param documentClass schema of the class to index
   * @param bulkProcessor the bulk process to use to write to ES
   * @param indexName the name of the index to write updates to
   */
  public ESBulkWriterDAO(@Nonnull Class<DOCUMENT> documentClass, @Nonnull BulkProcessor bulkProcessor,
      @Nonnull String indexName) {
    super(documentClass);
    _bulkProcessor = bulkProcessor;
    _indexName = indexName;
  }

  @Override
  public void upsertDocument(@Nonnull DOCUMENT document, @Nonnull String docId) {
    final String documentJson = RecordUtils.toJsonString(document);
    final IndexRequest indexRequest = new IndexRequest(_indexName).id(docId).source(documentJson, XContentType.JSON);
    final UpdateRequest updateRequest = new UpdateRequest(_indexName, docId).doc(documentJson, XContentType.JSON)
        .detectNoop(false)
        .upsert(indexRequest)
        .retryOnConflict(MAX_RETRIES);
    _bulkProcessor.add(updateRequest);
  }

  @Override
  public void deleteDocument(@Nonnull String docId) {
    _bulkProcessor.add(new DeleteRequest(_indexName).id(docId));
  }

  @Override
  public void close() {
    _bulkProcessor.close();
  }
}
