package net.markz.awscdkstack.constants;

public enum AWSConstants {
    MY_PUBLIC_SSL_CERTIFICATE_ARN("arn:aws:acm:ap-southeast-2:142621353074:certificate/803a9456-16ac-48af-a85d-d8ebcdf5cb94"),
    ACCOUNT("142621353074"),
    REGION("ap-southeast-2");

    private final String str;

    AWSConstants(final String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

}
