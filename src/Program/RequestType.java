package Program;

//Creates basic priority associations and assign attributes
public enum RequestType {
    SECURITY        (1, SecurityLevel.TOPLEVEL, "Security issue (password reset, phishing, …)"),
    NETWORK         (2, SecurityLevel.BASE,     "Network outage / connectivity"),
    SOFTWARE_INSTALL(3, SecurityLevel.BASE,     "Software / app installation"),
    NEW_PC          (4, SecurityLevel.BASE,     "New computer configuration"),
    OTHER           (4, SecurityLevel.BASE,     "Anything else");

    private final int defaultPriority;
    private final SecurityLevel defaultLevel;
    private final String niceLabel;

    RequestType(int p, SecurityLevel lvl, String lbl) {
        this.defaultPriority = p;  this.defaultLevel = lvl;  this.niceLabel = lbl;
    }
    public int getDefaultPriority()     { return defaultPriority; }
    public SecurityLevel getDefaultLvl(){ return defaultLevel;    }
    @Override public String toString()  { return name() + " – " + niceLabel; }
}
