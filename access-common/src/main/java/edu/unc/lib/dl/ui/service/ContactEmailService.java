package edu.unc.lib.dl.ui.service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class ContactEmailService {

	private JavaMailSender mailSender;
	private Configuration freemarkerConfiguration;

	private String defaultSubjectLine;
	private String fromAddress;
	private String fromName;
	private List<String> emailRecipients;
	private Template htmlTemplate;
	private Template textTemplate;
	
	public void sendContactEmail(Map<String, Object> model) {
		this.sendContactEmail(this.defaultSubjectLine, this.emailRecipients, model);
	}

	public void sendContactEmail(String subjectLine, List<String> emailRecipients, Map<String, Object> model) {
		try {
			StringWriter sw;
			String html, text;
			if (htmlTemplate != null) {
				sw = new StringWriter();
				htmlTemplate.process(model, sw);
				html = sw.toString();
			} else {
				html = null;
			}
			
			if (textTemplate != null) {
				sw = new StringWriter();
				textTemplate.process(model, sw);
				text = sw.toString();
			} else {
				text = null;
			}

			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper message = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED);

			for (String address : emailRecipients) {
				message.addTo(address);
			}

			message.setSubject(subjectLine);

			if (this.fromName != null)
				message.setFrom(this.fromAddress, this.fromName);
			else
				message.setFrom(this.fromAddress);

			if (html != null && text != null) {
				message.setText(text, html);
			} else if (html != null){
				message.setText(html, true);
			} else {
				message.setText(text);
			}

			this.mailSender.send(mimeMessage);
		} catch (IOException e) {
			throw new Error("Unable to load email template for Ingest Success", e);
		} catch (TemplateException e) {
			throw new Error("There was a problem loading FreeMarker templates for email notification", e);
		} catch (MessagingException e) {
			throw new Error("Unable to send contact email", e);
		}
	}

	public void setMailSender(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void setDefaultSubjectLine(String defaultSubjectLine) {
		this.defaultSubjectLine = defaultSubjectLine;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public void setEmailRecipients(List<String> emailRecipients) {
		this.emailRecipients = emailRecipients;
	}

	public void setHtmlTemplatePath(String path) throws IOException {
		htmlTemplate = this.freemarkerConfiguration.getTemplate(path, Locale.getDefault(), "utf-8");
	}
	
	public void setTextTemplatePath(String path) throws IOException {
		textTemplate = this.freemarkerConfiguration.getTemplate(path, Locale.getDefault(), "utf-8");
	}

	public void setHtmlTemplate(Template htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
	}

	public void setTextTemplate(Template textTemplate) {
		this.textTemplate = textTemplate;
	}

	public void setFreemarkerConfiguration(Configuration freemarkerConfiguration) {
		this.freemarkerConfiguration = freemarkerConfiguration;
	}
}
