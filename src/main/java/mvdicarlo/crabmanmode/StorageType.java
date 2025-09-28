package mvdicarlo.crabmanmode;

public enum StorageType {
    AZURE("Azure Storage Account"),
    FIREBASE("Firebase Realtime Database");

    StorageType(String displayName) {
        this.displayName = displayName;
    }

    private final String displayName;
    public String getDisplayName() {
        return displayName;
    }
}