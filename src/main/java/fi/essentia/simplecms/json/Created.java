package fi.essentia.simplecms.json;

/**
 */
public class Created extends Result {
    long documentId;

    public Created(long documentId) {
        super(true);
        this.documentId = documentId;
    }

    public long getDocumentId() {
        return documentId;
    }
}