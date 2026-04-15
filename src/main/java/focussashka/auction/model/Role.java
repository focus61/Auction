package focussashka.auction.model;

public enum Role {
    ADMIN("Администратор"),
    SELLER("Продавец"),
    BIDDER("Участник торгов");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
