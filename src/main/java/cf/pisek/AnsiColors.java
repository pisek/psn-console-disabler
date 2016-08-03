package cf.pisek;

public interface AnsiColors {
	public static final String RESET = "\u001B[0m";

	public static final String FG_BLACK = "\u001B[30m";
	public static final String FG_RED = "\u001B[31;1m";
	public static final String FG_GREEN = "\u001B[32;1m";
	public static final String FG_YELLOW = "\u001B[33;1m";
	public static final String FG_BLUE = "\u001B[34;1m";
	public static final String FG_PURPLE = "\u001B[35;1m";
	public static final String FG_CYAN = "\u001B[36;1m";
	public static final String FG_WHITE = "\u001B[37;1m";
	
	public static final String BG_BLACK = "\u001B[40m";
	public static final String BG_RED = "\u001B[41;1m";
	public static final String BG_GREEN = "\u001B[42;1m";
	public static final String BG_YELLOW = "\u001B[44;1m";
	public static final String BG_BLUE = "\u001B[44;1m";
	public static final String BG_PURPLE = "\u001B[45;1m";
	public static final String BG_CYAN = "\u001B[46;1m";
	public static final String BG_WHITE = "\u001B[47;1m";
}
