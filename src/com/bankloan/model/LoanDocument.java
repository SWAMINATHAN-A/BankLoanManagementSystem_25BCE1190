package com.bankloan.model;

/**
 * Model representing verification documents from LOAN_DOCUMENTS table.
 */
public class LoanDocument {
    private long documentId;
    private long applicationId;
    private String documentName;
    private String documentType;
    private String verificationStatus;
    private String remarks;
    private String verifiedAt;

    public LoanDocument() {}

    public long getDocumentId() { return documentId; }
    public void setDocumentId(long documentId) { this.documentId = documentId; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public String getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(String verifiedAt) { this.verifiedAt = verifiedAt; }
}
