package cf.pisek;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

public class Disabler {

	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		options.addOption("u", true, "email");
		options.addOption("p", true, "password");
		options.addOption("t", true, "try periodically (seconds)");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		User user = null;
		if (cmd.hasOption("u") && cmd.hasOption("p")) {
			user = new User(cmd.getOptionValue("u"), cmd.getOptionValue("p"));
		
			if (cmd.hasOption("t")) {
				// continously
				int seconds = Integer.parseInt(cmd.getOptionValue("t"));
				while (true) {
					
					tryDisablingTheConsole(user);
					
					Thread.sleep(seconds * 1000);
				}
			} else {
				// only once
				
				tryDisablingTheConsole(user);
				
			}
			
		} else {
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("disabler", options );
			
		}
		
	}

	private static void tryDisablingTheConsole(User user) throws IOException {
		Response res;
		Connection con;

		con = Jsoup.connect("https://account.sonyentertainmentnetwork.com/login.action?category=psn&displayNavigation=false");
		con.header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
		con.get();
		
		res = con.response();
//		System.out.println(Arrays.toString(res.cookies().entrySet().toArray()));
		
		con = Jsoup.connect("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_check");
		con.cookies(res.cookies());
		con.data("j_username", user.getUser());
		con.data("j_password", user.getPassword());
		con.data("service-entity", "np");
		System.out.println(con.post());
		
		res = con.response();
//		System.out.println(Arrays.toString(res.cookies().entrySet().toArray()));
		
		//already logged in
		
		// GET https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action
		con = Jsoup.connect("https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action");
		con.cookies(res.cookies());
		System.out.println(con.get());
		
		res = con.response();
//		System.out.println(Arrays.toString(res.cookies().entrySet().toArray()));
		
		// sprawdzamy czy istnieje button pozwalajacy na wylaczenie konsol
		
		// odpalamy wylaczenie
		
		// POST https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action
		con = Jsoup.connect("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action");
		con.cookies(res.cookies());
		System.out.println(con.post());
		
		res = con.response();
		
	}

}
