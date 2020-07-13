package com.xa.shushu.upload.datasource.service.log;

import com.xa.shushu.upload.datasource.service.log.condition.EmailCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Service
@Conditional(EmailCondition.class)
public class EmailErrorUploaderImpl implements ErrorUploader {
    //emailManagers 账号分割符号
    private static final String SPLIT = ";";

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${xa.config.errorUploader.managers:}")
    private String emailManagers;

    @Value("${xa.config.errorUploader.name:数数-数据上报}")
    private String name;

    @Value("${spring.mail.username:}")
    private String sender;

    public void sendMail(String subject, String content, String... to) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            for (String s : to) {
                helper.addTo(s);
            }
            helper.setSubject(subject);
            helper.setText(content);
            helper.setFrom(sender);
            javaMailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    //给管理员发送邮件
    public void sendMail2Managers(String subject, String content) {
        sendMail(subject, content, emailManagers.split(SPLIT));
    }

    @Override
    public void uploadError(String content) {
        sendMail2Managers(name, content);
    }
}
