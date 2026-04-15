package focussashka.auction.model;

public enum LotStatus {
    OPEN("Открыт"),
    CLOSED("Завершен");

    private final String displayName;

    LotStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
