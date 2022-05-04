package net.markz.awscdkstack.constants;

public enum NetworkConstants {
    MY_IP_1("222.153.125.27/32"),
    MY_IP_2("122.58.130.82/32"),
    WEBSCRAPER_SERVICE_DOMAIN_NAME("api.markz-portfolio.uk"),
    WEBSCRAPER_SERVICE_PATH_PATTERN("/webscraper-api"),
    PORTFOLIO_DOMAIN_NAME("markz-portfolio.uk");

    private final String str;

    NetworkConstants(final String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

}
