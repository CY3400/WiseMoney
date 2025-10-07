package com.charbel.backend.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import com.charbel.backend.config.AppProps;

@Service
public class EmailService {
    private final JavaMailSender mailSender;
    private final AppProps props;

    public EmailService(JavaMailSender mailSender, AppProps props){
        this.mailSender = mailSender;
        this.props = props;
    }

    public void send(String to, String subject, String text){
        if(to == null || to.isBlank()) return;
        if(subject == null) subject = "";
        if(text == null) text = "";

        var message = new SimpleMailMessage();
        message.setFrom(props.getMail().getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try{
            mailSender.send(message);
        }
        catch(MailException ex){
            System.err.println("[EmailService] Echec envoi mail vers " + to + " : " + ex.getMessage());
        }
    }

    public void sendPasswordResetEmail(String to, String resetUrl) {
        String subject = "Réinitialisation de votre mot de passe";
        String body = "Bonjour,\n\n" +
            "Cliquez sur ce lien pour réinitialiser votre mot de passe : " + resetUrl + "\n\n" +
            "Ce lien est valable 30 minutes.\n\n" +
            "— WiseMoney";
        send(to, subject, body);
    }
}