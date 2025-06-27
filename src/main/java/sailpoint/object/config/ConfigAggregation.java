package sailpoint.object.config;

public class ConfigAggregation {
    private boolean disableOptimization;
    private String objectType;
    private boolean recursive;
    private boolean simulate;
    private int timeout;
    private String[] extension;
    
    public ConfigAggregation(boolean disableOptimization, String objectType, boolean recursive, boolean simulate,
            int timeout, String[] extension) {
        this.disableOptimization = disableOptimization;
        this.objectType = objectType;
        this.recursive = recursive;
        this.simulate = simulate;
        this.timeout = timeout;
        this.extension = extension;
    }

    public boolean isDisableOptimization() {
        return disableOptimization;
    }

    public void setDisableOptimization(boolean disableOptimization) {
        this.disableOptimization = disableOptimization;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public void setSimulate(boolean simulate) {
        this.simulate = simulate;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String[] getExtension() {
        return extension;
    }

    public void setExtension(String[] extension) {
        this.extension = extension;
    }

    
}
