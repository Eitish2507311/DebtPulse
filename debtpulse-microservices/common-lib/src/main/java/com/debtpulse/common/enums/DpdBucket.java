package com.debtpulse.common.enums;

/**
 * Days-Past-Due bucket. The higher the bucket, the more delinquent the account.
 * {@link #classify(int)} maps a raw DPD count to its bucket.
 */
public enum DpdBucket {
    X30,
    X60,
    X90,
    X120,
    X180,
    NPA,
    WRITEOFF;

    /**
     * Classify a raw days-past-due value into its bucket:
     * dpd&le;30 → X30, &le;60 → X60, &le;90 → X90, &le;120 → X120, &le;180 → X180, else NPA.
     */
    public static DpdBucket classify(int dpd) {
        if (dpd <= 30) return X30;
        if (dpd <= 60) return X60;
        if (dpd <= 90) return X90;
        if (dpd <= 120) return X120;
        if (dpd <= 180) return X180;
        return NPA;
    }
}
