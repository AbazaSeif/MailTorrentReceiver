/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

/**
 *
 * @author giamat
 */
public class MailClient {

    private String userName;
    private String password;
    private String sendingHost;
    private int sendingPort;
    private String from;
    private String to;
    private String subject;
    private String text;
    private String receivingHost;
    private int receivingPort;
    private boolean usesmtpAuth, useimapAuth;
    private boolean usesmtpSSL, useimapSSL;
    String destFilePath;

    public void setAccountDetails(String sendingHost, int sendingPort, boolean usesmtpAuth, boolean usesmtpSSL, String receivingHost, int receivingPort, boolean useimapAuth, boolean useimapSSL, String userName, String password, String destFilePath) {

        this.userName = userName;
        this.password = password;
        this.sendingHost = sendingHost;
        this.sendingPort = sendingPort;
        this.usesmtpAuth = usesmtpAuth;
        this.usesmtpSSL = usesmtpSSL;
        this.receivingHost = receivingHost;
        this.receivingPort = receivingPort;
        this.useimapSSL = useimapSSL;
        this.useimapAuth = useimapAuth;
        this.destFilePath = destFilePath;

    }

    public boolean sendMail(String from, String to, String subject, String text) {
        Transport transport;
        Properties props;
        Session session1;
        Message simpleMessage;
        //MIME stands for Multipurpose Internet Mail Extensions
        InternetAddress fromAddress = null;
        InternetAddress toAddress = null;

        this.from = from;
        this.to = to;
        this.subject = subject;
        this.text = text;

        // For a Gmail account--sending mails-- host and port shold be as follows
        //this.sendingHost="smtp.gmail.com";
        //this.sendingPort=465;
        props = new Properties();

        props.put("mail.smtp.host", this.sendingHost);
        props.put("mail.smtp.port", String.valueOf(this.sendingPort));
        props.put("mail.smtp.user", this.userName);
        props.put("mail.smtp.password", this.password);
        props.put("mail.imap.starttls.enable", "true");
        if (usesmtpAuth) {
            props.put("mail.smtp.auth", "true");
        }
        if (usesmtpSSL) {
            props.put("mail.smtp.socketFactory.port", String.valueOf(this.sendingPort));
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
        }

        session1 = Session.getDefaultInstance(props);

        simpleMessage = new MimeMessage(session1);

        try {

            fromAddress = new InternetAddress(this.from);
            toAddress = new InternetAddress(this.to);

        } catch (AddressException e) {

            e.printStackTrace();

            return false;

        }

        try {

            simpleMessage.setFrom(fromAddress);

            simpleMessage.setRecipient(RecipientType.TO, toAddress);

            // to add CC or BCC use
            // simpleMessage.setRecipient(RecipientType.CC, new InternetAddress("CC_Recipient@any_mail.com"));
            // simpleMessage.setRecipient(RecipientType.BCC, new InternetAddress("CBC_Recipient@any_mail.com"));
            simpleMessage.setSubject(this.subject);

            simpleMessage.setText(this.text);

            //sometimes Transport.send(simpleMessage); is used, but for gmail it's different
            if (usesmtpSSL) {
                transport = session1.getTransport("smtps");
            } else {
                transport = session1.getTransport("smtp");
            }

            transport.connect(this.sendingHost, sendingPort, this.userName, this.password);

            transport.sendMessage(simpleMessage, simpleMessage.getAllRecipients());

            transport.close();

            return true;

        } catch (MessagingException e) {

            e.printStackTrace();

            return false;

        }

    }

    public void readMail(String subjectMon, String from) {
        this.from = from;
        /*this will print subject of all messages in the inbox of sender@gmail.com*/
        //this.receivingHost = "imap.gmail.com";//for imap protocol
        Properties props2;
        Store store;
        Session session2;
        Folder folder;

        props2 = new Properties();
        props2.put("mail.imap.host", this.receivingHost);
        props2.put("mail.imap.port", String.valueOf(this.receivingPort));
        props2.put("mail.imap.user", this.userName);
        props2.put("mail.imap.password", this.password);
        props2.put("mail.imap.starttls.enable", "true");
        if (useimapAuth) {
            props2.put("mail.imap.auth", "true");
        }
        props2.setProperty("mail.store.protocol", "imap");
        if (useimapSSL) {
            props2.put("mail.imap.socketFactory.port", String.valueOf(this.receivingPort));
            props2.put("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props2.put("mail.imap.socketFactory.fallback", "false");
        }

        session2 = Session.getDefaultInstance(props2, null);

        try {
            store = session2.getStore("imap");

            store.connect(this.receivingHost, this.userName, this.password);

            folder = store.getFolder("INBOX");//get inbox

            folder.open(Folder.READ_WRITE);//open folder only to read

            Message message[] = folder.search(new FlagTerm(new Flags(
                    Flags.Flag.SEEN), false));

            for (int i = 0; i < message.length; i++) {
                if (message[i].getSubject() == null) {
                    continue;
                }
                if (!message[i].getSubject().equalsIgnoreCase(subjectMon)) {
                    continue;
                }
                if (getAttachments(message[i])) {
                    this.sendMail(this.from, message[i].getFrom()[0].toString(), "Files acquired successfully", "Files are downloaded in the watchdir");
                    message[i].setFlag(Flags.Flag.SEEN, true);
                } else {
                    this.sendMail(this.from, message[i].getFrom()[0].toString(), "Mail content error", "Incorrect mail content.");
                    message[i].setFlag(Flags.Flag.SEEN, true);
                }
            }

            //close connections
            folder.close(true);

            store.close();

        } catch (Exception e) {

            System.out.println(e.toString());

        }

    }

    public boolean getAttachments(Message message) throws Exception {
        //Creo un oggetto generico object che poi successivamente controllerò che sia una instanza di tipo Multipart
        Object msgContent;
        Multipart multipart;
        BodyPart bodypart;
        String disposition;
        DataHandler handler;
        String destFilePath;
        boolean trovato = false;
        msgContent = message.getContent();
        if (!(msgContent instanceof Multipart)) {
            return trovato;
        }
        multipart = (Multipart) msgContent;
        for (int i = 0; i < multipart.getCount(); i++) {
            bodypart = multipart.getBodyPart(i);
            disposition = bodypart.getDisposition();
            if (disposition != null && disposition.equalsIgnoreCase("ATTACHMENT")) {
                if (bodypart.getFileName().endsWith(".torrent")) {
                    destFilePath = this.destFilePath + bodypart.getFileName();
                    FileOutputStream output = new FileOutputStream(destFilePath);
                    InputStream input = bodypart.getInputStream();
                    byte[] buffer = new byte[4096];
                    int byteRead;
                    while ((byteRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, byteRead);
                    }
                    output.close();
                    input.close();
                    trovato = true;
                }
            }
        }
        return trovato;
    }

}
