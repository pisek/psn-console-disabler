package cf.pisek;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cf.pisek.exceptions.LoginException;
import cf.pisek.notifiers.ConsoleNotifier;
import cf.pisek.notifiers.SMTPNotifier;

public class Disabler {
	
	public static void main(String[] args) throws InterruptedException, ParseException {
		
		Options options = new Options();
		options.addOption("u", true, "email");
		options.addOption("p", true, "password");
		options.addOption("t", true, "try periodically (in seconds)");
		options.addOption("d", false, "try to disable console (default: only checks if it is possible and notifies)");
		options.addOption("r", true, "retry count - for one connection if failed (default 3)");
		options.addOption("e", false, "notify via email (default via console only)");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		Notifier not = new ConsoleNotifier();
		if (cmd.hasOption("e")) {
			not = new SMTPNotifier();
		}
		
		int maxRetry = Integer.parseInt(cmd.getOptionValue("r", "3"));
		
		User user = null;
		if (cmd.hasOption("u") && cmd.hasOption("p")) {
			user = new User(cmd.getOptionValue("u").trim(), cmd.getOptionValue("p").trim());
			boolean tryDisable = cmd.hasOption("d");
		
			if (cmd.hasOption("t")) {
				// continously
				int seconds = Integer.parseInt(cmd.getOptionValue("t"));
				while (true) {
					
					retryCheckDisablingConsole(not, user, tryDisable, maxRetry);
					
					Thread.sleep(seconds * 1000);
					
				}
			} else {
				// only once
				
				retryCheckDisablingConsole(not, user, tryDisable, maxRetry);
				
			}
			
		} else {
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("disabler", options );
			
		}
		
	}

	private static void retryCheckDisablingConsole(Notifier not, User user, boolean tryDisable, int maxRetry)
			throws InterruptedException {
		
		System.out.println("==================="+ DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) +"====================");
		
		// retry
		for (int i = 0; i < maxRetry; i++) {
			
			System.out.println("Try no. " + (i+1));
			
			try {
				checkDisablingTheConsole(not, user, tryDisable);
				break;
			} catch (HttpStatusException e) {
				not.error(user, "Could not access website - possible BAN for too many connections...\n" + e);
			} catch (LoginException e) {
				not.error(user, "Could not login - possible wrong credentials OR captcha!");
			} catch (IOException e) {
				not.error(user, e.toString());
			}
			
			sleep();
			
		}
		
	}
	
	private static Connection generateConnection(String url, Map<String, String> cookies) {
		Connection con = Jsoup.connect(url);
		con.header("User-Agent", "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36");
		con.validateTLSCertificates(false);
		con.timeout(10000);
		con.cookies(cookies);
		con.followRedirects(false);
		System.out.println("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
		return con;
	}

	private static void checkDisablingTheConsole(Notifier not, User user, boolean tryDisable) throws InterruptedException, FileNotFoundException, IOException, LoginException {
		Connection con;
		Document doc;
		Map<String, String> cookies = new HashMap<> ();
		
		
		boolean isDisablingPossible = true;
		
		try(PrintWriter log = new PrintWriter("log_"+ DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()) +".html")){

			// 1. init
			con = generateConnection("https://account.sonyentertainmentnetwork.com/login.action", cookies);
			log.println("<h1>LOG IN</h1>");
			log.println(con.get());
			
			finalizeStep(con, cookies);
			
			
			// 2. log in
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_check", cookies);
			con.data("j_username", user.getUser());
			con.data("j_password", user.getPassword());
			con.data("service-entity", "np");
			log.println("<h1>LOGGED IN - ACCOUNT INFO</h1>");
			doc = con.post();
			log.println(doc);

			finalizeStep(con, cookies);
			
			
			System.out.println("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
			
			
			// check login status
			if (!cookies.containsKey("rememberSignIn")) {
				throw new LoginException();
			}
			
			
//			if (true) return;
			
			
			// 3. check disable button/error box
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
			log.println("<h1>DEVICE MEDIA LIST</h1>");
			doc = con.get();
			log.println(doc);
			
			Element gameMediaDevicesDeactivateSection = doc.getElementById("gameMediaDevicesDeactivateSection");
			Element errorLabel = gameMediaDevicesDeactivateSection.getElementById("toutLabel");
			if (errorLabel != null) {
				isDisablingPossible = false;
			}
			
			finalizeStep(con, cookies);
			
			
			
			if (isDisablingPossible) {
				
				if (tryDisable) {
					
					// 3.1. deactivation confirmation
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action", cookies);
					log.println("<h1>DEACTIVATION CONFIRMATION</h1>");
					log.println(con.get());
					
					finalizeStep(con, cookies);
					
					
					
					// 3.2. fire up the disabling process
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action", cookies);
					log.println("<h1>DEACTIVATION DONE!</h1>");
					log.println(con.post());
					
					finalizeStep(con, cookies);
					
					
					// 3.3. check the status of disabling the console (check again disable button/error box)
					con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
					log.println("<h1>DEVICE MEDIA LIST</h1>");
					doc = con.get();
					log.println(doc);
					
					gameMediaDevicesDeactivateSection = doc.getElementById("gameMediaDevicesDeactivateSection");
					errorLabel = gameMediaDevicesDeactivateSection.getElementById("toutLabel");
					if (errorLabel != null) {
						
						not.yes(user, "Disabling was performed successfully!");
						
					} else {
						
						not.yes(user, "Failed to perform console disabling... Please, try manually, because it is still possible!");
						
					}
					
					finalizeStep(con, cookies);
					
					
				} else {
					
					not.yes(user, "Account is eligible for disabling!");
					
				}
					
			} else {
				
				not.no(user, "Failed: " + errorLabel.text());
				
			}
			
				
				
			// 4. log out (necessary to login again)
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_logout", cookies);
			log.println("<h1>LOG OUT</h1>");
			log.println(con.get());
			
			finalizeStep(con, cookies);
			
		}
		
	}

	private static void finalizeStep(Connection con, Map<String, String> cookies) throws InterruptedException {
		cookies.putAll(con.response().cookies());
		sleep();
	}

	private static void sleep() throws InterruptedException {
		Thread.sleep(3000 + new Random().nextInt(2000));		
	}

}
