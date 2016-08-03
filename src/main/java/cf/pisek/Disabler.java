package cf.pisek;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.jsoup.Connection;
import org.jsoup.Connection.Request;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import cf.pisek.exceptions.LoginException;
import cf.pisek.notifiers.ConsoleNotifier;
import cf.pisek.notifiers.SMTPNotifier;

import static cf.pisek.AnsiColors.*;

public class Disabler {
	
	private static final Logger log = LogManager.getLogger();
	
	public static void main(String[] args) throws InterruptedException, ParseException {
		
		Options options = new Options();
		options.addOption("u", true, "email");
		options.addOption("p", true, "password");
		options.addOption("t", true, "try periodically (in seconds)");
		options.addOption("x", false, "try to disable console (default: only checks if it is possible and notifies)");
		options.addOption("r", true, "retry count - for one connection if failed (default 3)");
		options.addOption("e", false, "notify via email (default via console only)");
		options.addOption("d", false, "debug");
		options.addOption("dd", false, "full debug");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		
		
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
		if (cmd.hasOption("d")) {
			loggerConfig.setLevel(Level.INFO);
		}
		if (cmd.hasOption("dd")) {
			loggerConfig.setLevel(Level.DEBUG);
		}
		ctx.updateLoggers();
		
		Notifier not = new ConsoleNotifier();
		if (cmd.hasOption("e")) {
			not = new SMTPNotifier();
		}
		
		int maxRetry = Integer.parseInt(cmd.getOptionValue("r", "3"));
		
		User user = null;
		if (cmd.hasOption("u") && cmd.hasOption("p")) {
			user = new User(cmd.getOptionValue("u").trim(), cmd.getOptionValue("p").trim());
			boolean tryDisable = cmd.hasOption("x");
		
			if (cmd.hasOption("t")) {
				// continously
				int seconds = Integer.parseInt(cmd.getOptionValue("t"));
				while (true) {
					
					retryCheckDisablingConsole(not, user, tryDisable, maxRetry);

					System.out.println(BG_BLACK + FG_CYAN + "Next checking at " + DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now().plusSeconds(seconds)) + RESET);
					
					Thread.sleep(seconds * 1000);
					
				}
			} else {
				// only once
				
				retryCheckDisablingConsole(not, user, tryDisable, maxRetry);
				
			}
			
		} else {
			
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar psndisabler.jar [options]", options );
			
		}
		
	}

	private static void retryCheckDisablingConsole(Notifier not, User user, boolean tryDisable, int maxRetry)
			throws InterruptedException {
		
		System.out.println(BG_BLACK + FG_CYAN + "==================="+ DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()) +"====================" + RESET);
		
		// retry
		for (int i = 0; i < maxRetry; i++) {
			
			System.out.println(BG_BLACK + FG_CYAN + "Try no. " + (i+1) + RESET);
			
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
		con.header("Host", "account.sonyentertainmentnetwork.com");
		con.header("Connection", "keep-alive");
		con.header("Pragma", "no-cache");
		con.header("Cache-Control", "no-cache");
		con.header("Upgrade-Insecure-Requests", "1");
		con.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		con.header("DNT", "1");
		con.header("Accept-Encoding", "gzip, deflate, sdch, br");
		con.header("Accept-Language", "q=0.8,en-US;q=0.6,en;q=0.4,ko;q=0.2,pt;q=0.2,es;q=0.2,da;q=0.2");
//		con.header("Referer", "https://account.sonyentertainmentnetwork.com/login.action");
		con.header("Upgrade-Insecure-Requests", "1");
		con.validateTLSCertificates(false);
		con.timeout(10000);
		con.cookies(cookies);
		con.followRedirects(false);
		return con;
	}

	private static void checkDisablingTheConsole(Notifier not, User user, boolean tryDisable) throws InterruptedException, FileNotFoundException, IOException, LoginException {
		Connection con;
		Document doc;
		Map<String, String> cookies = new HashMap<> ();
		
		
		boolean isDisablingPossible = true;
		
		try(PrintWriter htmlLog = new PrintWriter("log_"+ DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS").format(LocalDateTime.now()) +".html")){

			// 1. init
			con = generateConnection("https://account.sonyentertainmentnetwork.com/login.action", cookies);
			htmlLog.println("<h1>LOG IN</h1>");
			htmlLog.println(con.get());
			System.out.println(BG_BLACK + FG_CYAN + "Init - getting initial session" + RESET);
			log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
			log.debug(parseHeaders(con.request()));
			log.debug(parseHeaders(con.response()));
			
			finalizeStep(con, cookies);
			
			
//			if (true) return;
			
			
			// 2. log in
			con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_check", cookies);
			con.data("j_username", user.getUser());
			con.data("j_password", user.getPassword());
			con.data("service-entity", "np");
			htmlLog.println("<h1>LOGGED IN - ACCOUNT INFO</h1>");
			doc = con.post();
			htmlLog.println(doc);
			System.out.println(BG_BLACK + FG_CYAN + "Log in" + RESET);
			log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
			log.debug(parseHeaders(con.request()));
			log.debug(parseHeaders(con.response()));

			finalizeStep(con, cookies);
			
			
			// check login status
			System.out.println(BG_BLACK + FG_CYAN + "Checking login status" + RESET);
			if (!cookies.containsKey("rememberSignIn")) {
				throw new LoginException();
			}
			System.out.println(BG_BLACK + FG_GREEN + "Logged in!" + RESET);
			
			
//			if (true) return;
			
			
			try {	// in order to logout afterwards
			
			
				// 3. check disable button/error box
				con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
				htmlLog.println("<h1>DEVICE MEDIA LIST</h1>");
				doc = con.get();
				htmlLog.println(doc);
				System.out.println(BG_BLACK + FG_CYAN + "Checking disable button availability" + RESET);
				log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
				log.debug(parseHeaders(con.request()));
				log.debug(parseHeaders(con.response()));
				
				Element gameMediaDevicesDeactivateSection = doc.getElementById("gameMediaDevicesDeactivateSection");
				Element gameDeviceDescription1 = gameMediaDevicesDeactivateSection.getElementById("gameDeviceDescription1");
				if (gameDeviceDescription1 == null) {	// it there is no gameDeviceDescription1, there is no console connected 
					not.yes(user, "No console connected!");
					return;
				}
				Element errorLabel = gameMediaDevicesDeactivateSection.getElementById("toutLabel");
				if (errorLabel != null) {	// error label says that you have done disabling and you have to wait 6months etc
					isDisablingPossible = false;
				}
				
				finalizeStep(con, cookies);
				
				
				
				if (isDisablingPossible) {
					
					if (tryDisable) {
						
						// 3.1. deactivation confirmation
						con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/account/devices/media-devices-confirm-deactivate.action", cookies);
						htmlLog.println("<h1>DEACTIVATION CONFIRMATION</h1>");
						htmlLog.println(con.get());
						System.out.println(BG_BLACK + FG_CYAN + "Going to confirmation of disabling" + RESET);
						log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
						log.debug(parseHeaders(con.request()));
						log.debug(parseHeaders(con.response()));
						
						finalizeStep(con, cookies);
						
						
						
						// 3.2. fire up the disabling process
						con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/clear-domain.action", cookies);
						htmlLog.println("<h1>DEACTIVATION DONE!</h1>");
						htmlLog.println(con.post());
						System.out.println(BG_BLACK + FG_CYAN + "Disabling..." + RESET);
						log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
						log.debug(parseHeaders(con.request()));
						log.debug(parseHeaders(con.response()));
						
						finalizeStep(con, cookies);
						
						
						// 3.3. check the status of disabling the console (check again disable button/error box)
						con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/cam/devices/device-media-list.action", cookies);
						htmlLog.println("<h1>DEVICE MEDIA LIST</h1>");
						doc = con.get();
						htmlLog.println(doc);
						System.out.println(BG_BLACK + FG_CYAN + "Checking if disabling went OK..." + RESET);
						log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
						log.debug(parseHeaders(con.request()));
						log.debug(parseHeaders(con.response()));
						
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
			
				
			} finally {
				
				// 4. log out (necessary to login again)
				con = generateConnection("https://account.sonyentertainmentnetwork.com/liquid/j_spring_security_logout", cookies);
				htmlLog.println("<h1>LOG OUT</h1>");
				htmlLog.println(con.get());
				System.out.println(BG_BLACK + FG_CYAN + "Log out" + RESET);
				log.info("Cookies: " + Arrays.toString(cookies.entrySet().toArray()));
				log.debug(parseHeaders(con.request()));
				log.debug(parseHeaders(con.response()));
				
				finalizeStep(con, cookies);
				
			}
				
		}
		
	}

	private static void finalizeStep(Connection con, Map<String, String> cookies) throws InterruptedException {
		cookies.putAll(con.response().cookies());
		sleep();
	}

	private static void sleep() throws InterruptedException {
		Thread.sleep(3000 + new Random().nextInt(2000));		
	}
	
	private static String parseHeaders(Connection.Base<?> res) {
		String head = "";
		if (res instanceof Request) {
			head = res.method() + " REQUEST";
		} else if (res instanceof Response) {
			head = res.method() + " RESPONSE";
		}
		StringBuilder sb = new StringBuilder("=================== ").append(head).append(" ===================\n")
				.append("HEADERS:\n");
		for (Entry<String, String> ent : res.headers().entrySet()) {
			sb.append(ent.getKey()).append(": ").append(ent.getValue()).append('\n');
		}
		sb.append("COOKIES:\n");
		for (Entry<String, String> ent : res.cookies().entrySet()) {
			sb.append(ent.getKey()).append(": ").append(ent.getValue()).append('\n');
		}
		return sb.toString();
	}

}
