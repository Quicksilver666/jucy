package uc.protocols;

public enum CPType {

	
	
	NMDC(false,true,"NMDC"),NMDCS(true,true,"NMDCS"),ADC(false,false,"ADC/1.0"),ADCS(true,false,"ADCS/0.10");
	
	private final boolean encrypted;
	private final boolean nmdc;



	private final String protocol;
	
	CPType(boolean encrypted,boolean nmdc,String protocol) {
		this.encrypted = encrypted;
		this.protocol = protocol;
		this.nmdc = nmdc;
	}
	
	public String toString() {
		return protocol;
	}
	
	/**
	 * 
	 * @param s - the protocol string
	 * @return Protocol matching requested..
	 * if none found IllegalStateEception is thrown
	 */
	public static CPType fromString(String s) {
		for (CPType c: values()) {
			if (c.protocol.equals(s)) {
				return c;
			}
		}
		throw new IllegalStateException();
	}
	
	public static CPType get(boolean encryption,boolean nmdc) {
		for (CPType type:values()) {
			if (type.encrypted == encryption && type.nmdc == nmdc) {
				return type;
			}
		}
		throw new IllegalStateException();
	}
	
	public boolean isEncrypted() {
		return encrypted;
	}
	
	public boolean isNmdc() {
		return nmdc;
	}
}
