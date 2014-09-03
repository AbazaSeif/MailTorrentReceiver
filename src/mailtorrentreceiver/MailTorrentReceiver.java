/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailtorrentreceiver;

import encryption.DesEncrypter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import mail.MailClient;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author giamat
 */
public class MailTorrentReceiver {

    static String mailFrom = "sender@gmail.com";
    //Sender must be a Gmail Account holder
    static String mailTo = "giacomo.mattiuzzi@gmail.com";
    //but here you can send to any type of mail account
    static String Password = "1234";
    static String UserName = "sender";
    //Mention your email subject and content
    static String mailSubject = "Testing Mail";
    static MailClient newMailClient;
    static String smtpHost;
    static int smtpPort;
    static String imapHost;
    static String watchdir;
    static int imapPort;
    static boolean usesmtpAuth, useimapAuth, usesmtpSSL, useimapSSL;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        retriveParms();

        //Create a GmailClient object
        newMailClient = new MailClient();

        //Setting up account details
        newMailClient.setAccountDetails(smtpHost, smtpPort, usesmtpAuth, usesmtpSSL, imapHost, imapPort, useimapAuth, useimapSSL, UserName, Password, watchdir);

        //Send mail
        //newMailClient.sendMail(mailFrom, mailTo, mailSubject, mailText);
        //Receive mails
        newMailClient.readMail(mailSubject, mailFrom);

    }

    public static void retriveParms() {
        //Recupero l'xml di configurazione
        SAXBuilder builder = new SAXBuilder();
        Element rootElement;
        Element mailConf;
        Document doc;
        try {
            doc = builder.build(new File("./settings/settings.xml"));
        } catch (Exception ex) {
            System.out.println("Filed to get mail settings from xml");
            System.out.println(ex.toString());
            return;
        }
        //Ottengo la root del documento xml
        rootElement = doc.getRootElement();
        mailConf = rootElement.getChild("mail-config");
        mailFrom = mailConf.getChildTextTrim("mail-address-from");
        mailSubject = mailConf.getChildTextTrim("mail-subject-monitorig");
        String encPassword = mailConf.getChildTextTrim("password");
        UserName = mailConf.getChildTextTrim("user");
        smtpHost = mailConf.getChildTextTrim("smtp-server");
        smtpPort = Integer.parseInt(mailConf.getChildTextTrim("smtp-port"));
        if (mailConf.getChildTextTrim("use-smtp-SSL").equalsIgnoreCase("true")) {
            usesmtpSSL = true;
        } else {
            usesmtpSSL = false;
        }
        if (mailConf.getChildTextTrim("use-imap-SSL").equalsIgnoreCase("true")) {
            useimapSSL = true;
        } else {
            useimapSSL = false;
        }
        if (mailConf.getChildTextTrim("use-smtp-auth").equalsIgnoreCase("true")) {
            usesmtpAuth = true;
        } else {
            usesmtpAuth = false;
        }
        if (mailConf.getChildTextTrim("use-imap-auth").equalsIgnoreCase("true")) {
            useimapAuth = true;
        } else {
            useimapAuth = false;
        }
        imapHost = mailConf.getChildTextTrim("imap-server");
        imapPort = Integer.parseInt(mailConf.getChildTextTrim("imap-port"));
        //Watch-dir
        watchdir = rootElement.getChildTextTrim("watch-dir");
        Password = encPassword;
        //decrypto password
        File file = new File("./settings/.secret.bin");
        if (file.exists() && !file.canRead()) {
            System.out.println("Read file permission Error \n Now Exit");
            return;
        }
        if (!file.exists()) {
            System.out.println("File not exists \n Now Exit");
            return;
        }
        FileInputStream inputfile;
        byte[] keyBytes;
        SecretKey key;
        try {
            inputfile = new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            return;
        }
        byte[] buffer = new byte[4096];
        int byteRead;
        try {
            keyBytes = getBytesFromInputStream(inputfile);
        } catch (IOException ex) {
            return;
        }
        try {
            inputfile.close();
        } catch (IOException ex) {
            return;
        }
        key = new SecretKeySpec(keyBytes, "DES");
        DesEncrypter encrypter;
        try {
            encrypter = new DesEncrypter(key);
        } catch (Exception ex) {
            return;
        }
        try {
            Password = encrypter.decrypt(encPassword);
        } catch (Exception ex) {
            return;
        }
    }

    public static byte[] getBytesFromInputStream(FileInputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];

        for (int len; (len = is.read(buffer)) != -1;) {
            os.write(buffer, 0, len);
        }

        os.flush();

        return os.toByteArray();
    }
}
