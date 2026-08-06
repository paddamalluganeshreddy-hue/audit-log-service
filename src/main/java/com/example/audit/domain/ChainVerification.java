package com.example.audit.domain;

public record ChainVerification(boolean intact, long checkedRecords, Long firstInconsistentSequence,
                                String violationType, String detail) {
    public static ChainVerification intact(long count) { return new ChainVerification(true, count, null, null, "Chain is intact"); }
    public static ChainVerification broken(long checked, long sequence, String type, String detail) {
        return new ChainVerification(false, checked, sequence, type, detail);
    }
}
