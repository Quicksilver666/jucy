package helpers;

public enum SizeEnum {
	B(0), KiB(1), MiB(2), GiB(3), TiB(4), PiB(5);

	public static final String infinity;
	
	static {
		String os = System.getProperty("os.name");
		boolean oldOS = false;
		
		if (os.contains("2000") || os.contains("XP") || os.contains("2003")) {
			oldOS = true;
		}
		infinity = oldOS? "inf": "\u221E"; 
	}
	
	SizeEnum( int powerof1024){
		mult= (long)StrictMath.pow(1024, powerof1024);
	}
	
	private final long mult;
	
	public long getInBytes(long nr){
		return mult * nr;
	}
	
	private static final SizeEnum[] endings = new SizeEnum[]{KiB,MiB,GiB,TiB,PiB}; 
	
	public static String getReadableSize(long value){
		if(value < 1024)
			return value +" "+B;
		
		long a = value*100/1024;
	
		for (int i=0;i < endings.length; i++) {
			if (a < 100000) {
				int x =(int) (a % 100);
				return a/100 +","+ (x<10?"0":"") + x + " "+ endings[i];
			}
			a /= 1024;
		}
		
		return "Too much";
	}
	
	public static String getShortSize(long value){
		if(value < 1024)
			return value +""+B;
		
		long a = value*100/1024;
	
		for (int i=0;i < endings.length; i++) {
			if (a < 100000) {
				return a/100 +""+ endings[i].name().charAt(0);
			}
			a/=1024;
		}
		return "Too much";
	}
	
	public static String getRoundedSize(long value){
		if(value < 1024)
			return value +""+B;
		
		long a = value*100/1024;
	
		for (int i=0;i < endings.length; i++) {
			if (a < 100000) {
				return a/100 +" "+ endings[i].name();
			}
			a/=1024;
		}
		return "Too much";
	}
	
	/**
	 * gets an SizeEnum for the largest size that can be 
	 * used to store the bytes.
	 * 
	 * @param bytesize
	 * @return
	 */
	public static SizeEnum getLargestEnumMatchingByteSize(long bytesize) {
		SizeEnum highest = B;
		if (bytesize <= 0) {
			return B;
		}
		for (SizeEnum se: endings) {
			if (bytesize % se.mult == 0 ) {
				highest = se;
			}
		}
		return highest;
	}
	
	public long getSize(long bytesize) {
		return bytesize / mult ;
	}
	
	public static String getExactSharesize(long value) {
		if (value == 0) {
			return "0 "+B;
		}
		String erg=" "+B;
		long next = value;
		int i;
		while (next >= 1000){
			i = (int)(next % 1000);
			erg = "."+(i < 100 ? (i<10?"00":"0"):"")   +i+erg; 
			next /= 1000;
		}
		erg = next+erg;

		return erg;
	}
	
	
	/**
	 * Compute speed(MiB/s  from Bytes and milliseconds..
	 * @param timeduration milliseconds that should be converted
	 * @param size - the size in total
	 * @return a string with size/time  ending 
	 */
	public static String toSpeedString(long timeduration , long size) {
		if (timeduration == 0) {
			return "0 B/s";
		} else {
			return SizeEnum.getReadableSize( (size*1000) / timeduration )+"/s";
		}
	}
	
	/**
	 * size in KiB/MiB/s  given speed in Byte/s
	 */
	public static String toShortSpeedString(long speed) {
		return SizeEnum.getRoundedSize(speed)+"/s";
	}
	
	
	
	/**
	 * Speedstring in Bits/s or KiBits/s
	 * @param speed the speed in bytes/s
	 * @return
	 */
	public static String toSpeedString(long speed) {
		return SizeEnum.getReadableSize(speed*8)+"its/s";
	}
	

	/**
	 * 
	 * @param seconds  what amount of seconds should be changed to durationstring
	 * @return a String representation of the given seconds
	 */
	public static String toDurationString(long seconds){
		if (seconds == Long.MAX_VALUE) {
			return infinity;//"∞";
		}

		int minutes= (int)((seconds %3600)/60);
		int secs = (int)(seconds %60);
		return (seconds /3600) + ":"+ (minutes < 10?"0"+minutes : minutes) +":"+ (secs < 10?"0"+secs : secs); 
		
	}
	
	public static String timeEstimation(long size,long speed) {
		if (speed == 0) {
			return timeEstimation(Long.MAX_VALUE);
		} else {
			return timeEstimation(size / speed);
		}
	}
	
	public static String timeEstimation(long seconds) {
		if (seconds == Long.MAX_VALUE) {
			return  infinity;
		}
		
		if (seconds < 60) {
			return seconds +"s";
		}
		if ( seconds < 300) {
			return (seconds /60)+"m "+(seconds%60)+"s";
		}
		long minutes = seconds /60;
		
		if (minutes < 60) {
			return minutes +"m";
		}
		if (minutes < 300) {
			return (minutes /60) +"h "+(minutes%60)+"m";
		}
		
		return (minutes/60)+"h";
		
	}
	
}
