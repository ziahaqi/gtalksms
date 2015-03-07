package com.googlecode.gtalksms.tools;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.BASE64EncoderStream;

import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

public class GoogleMail {
    private Session session;
    private static Context sContext;

    private static final String MAIL_GOOGLE_COM = "https://mail.google.com";
    private static final String GMAIL_COMPOSE = "https://www.googleapis.com/auth/gmail.compose";
    private static final String GMAIL_MODIFY = "https://www.googleapis.com/auth/gmail.modify";
    private final String OAUTH2_URL = "oauth2:" + GMAIL_COMPOSE + " " + GMAIL_MODIFY + " " + MAIL_GOOGLE_COM;

    public GoogleMail(Context context) {
        sContext = context;
    }

    private Account getFirstAccount() throws Exception {
        Account[] accounts = AccountManager.get(sContext).getAccountsByType("com.google");

        if (accounts.length == 0) {
            throw new Exception("No google account found.");
        } else {
            return accounts[0];
        }
    }

    private String getAuthenticationToken(Account account) throws Exception {
        return AccountManager.get(sContext).blockingGetAuthToken(account, OAUTH2_URL, true);
    }

    private SMTPTransport connect(String host, int port, String userEmail, String oauthToken, boolean debug) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.sasl.enable", "false");
        session = Session.getInstance(props);
        session.setDebug(debug);

        final URLName unusedUrlName = null;
        SMTPTransport transport = new SMTPTransport(session, unusedUrlName);

        // If the password is null, SMTP tries to do AUTH LOGIN.
        final String emptyPassword = null;
        transport.connect(host, port, userEmail, emptyPassword);

        byte[] response = String.format("user=%s\1auth=Bearer %s\1\1", userEmail, oauthToken).getBytes();
        Log.i(new String(response));
        response = BASE64EncoderStream.encode(response);

        Log.i("SMTP Connection: " + transport.isConnected());

        transport.issueCommand("AUTH XOAUTH2 " + new String(response), 235);

        return transport;
    }


    public synchronized void send(String subject, String body, String recipients) throws Exception {
        Account account = getFirstAccount();

        SMTPTransport smtpTransport = connect("smtp.gmail.com", 587, account.name, getAuthenticationToken(account), true);

        MimeMessage message = new MimeMessage(session);
        DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));
        message.setSender(new InternetAddress(account.name));
        message.setSubject(subject);
        message.setDataHandler(handler);
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));

        smtpTransport.sendMessage(message, message.getAllRecipients());
    }
}