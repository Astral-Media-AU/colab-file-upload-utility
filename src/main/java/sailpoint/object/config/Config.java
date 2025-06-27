package sailpoint.object.config;

/**
 * Class to represent the config JSON.
 */
public class Config {
    private boolean verbose;
    private String[] files;
    private ConfigTenant tenant;
    private ConfigProxy proxy;
    private ConfigAggregation aggregation;
    private String ks;
    private String iv;
    
    public boolean isVerbose() {
        return verbose;
    }
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    public String[] getFiles() {
        return files;
    }
    public void setFiles(String[] files) {
        this.files = files;
    }
    public ConfigTenant getTenant() {
        return tenant;
    }
    public void setTenant(ConfigTenant tenant) {
        this.tenant = tenant;
    }
    public ConfigProxy getProxy() {
        return proxy;
    }
    public void setProxy(ConfigProxy proxy) {
        this.proxy = proxy;
    }
    public ConfigAggregation getAggregation() {
        return aggregation;
    }
    public void setAggregation(ConfigAggregation aggregation) {
        this.aggregation = aggregation;
    }
	public String getKs() {
		return ks;
	}
	public void setKs(String ks) {
		this.ks = ks;
	}
	public String getIv() {
		return iv;
	}
	public void setIv(String iv) {
		this.iv = iv;
	}

    
}