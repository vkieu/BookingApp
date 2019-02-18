package com;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class SendHTMLEmail {

	public static void main(String[] args) throws Exception {
		System.getenv().forEach((k, v) -> {
			System.out.println(k + " : " + v);
		});
		//send("what a bunch of bolox\n???");
	}

	public static boolean send(String messageText) {
		messageText = messageText.replaceAll("\n", "<br/>");

		AppProperties appProperties = AppProperties.getInstance();

		// Recipient's email ID needs to be mentioned.
		String to = appProperties.getProperty("mail.to");
		String toBcc = appProperties.getProperty("mail.bcc");

		// Sender's email ID needs to be mentioned
		String from = appProperties.getProperty("mail.from");

		String host = appProperties.getProperty("mail.host");

		String subject = appProperties.getProperty("mail.subject");

		Properties props = new Properties();
		props.put("mail.smtp.auth", true);
		props.put("mail.smtp.starttls.enable", "true");
		props.setProperty("mail.smtp.ssl.enable", "true");

		// Get the default Session object.
		Session session = Session.getInstance(props);

		try {
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(from));

			// Set To: header field of the header.
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
			if (toBcc != null) {
				message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(toBcc, false));
			}

			// Set Subject: header field
			message.setSubject(subject);

			// Send the actual HTML message, as big as you like
			message.setContent(messageText, "text/html");

			// Send message

			Transport trnsport = session.getTransport("smtp");
			//keep username password here
			trnsport.connect(host, "minh.kieu@ntlworld.com", "Just4Minh");
			message.saveChanges();
			trnsport.sendMessage(message, message.getAllRecipients());
			trnsport.close();

			System.out.println("Sent message successfully....");
			return true;
		} catch (MessagingException mex) {
			mex.printStackTrace();
			return false;
		}
	}
}
