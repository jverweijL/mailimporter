package com.liferay.mail.custom;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.lang.StringUtils;


public class MailImporter {
	
	private static final Log _log = LogFactoryUtil.getLog(MailImporter.class);
	private static Properties properties = PropsUtil.getProperties();	
	
	static String DEFAULT_MAILHOST = properties.getProperty("com.liferay.mail.custom.mailimporter.mailhost", "");
	static String DEFAULT_MAILPROTOCOL = properties.getProperty("com.liferay.mail.custom.mailimporter.mailprotocol","pop3s");
	static String DEFAULT_MAILPORT = properties.getProperty("com.liferay.mail.custom.mailimporter.port","995");
	static String USERNAME = properties.getProperty("com.liferay.mail.custom.mailimporter.username","");
	static String PASSWORD = properties.getProperty("com.liferay.mail.custom.mailimporter.password","");
	
	static String GROUPID = properties.getProperty("com.liferay.mail.custom.mailimporter.groupid","");
	static String COMPANYID = properties.getProperty("com.liferay.mail.custom.mailimporter.companyid","");
	static String USERID = properties.getProperty("com.liferay.mail.custom.mailimporter.userid","");
	static String WEBCONTENTFOLDERID = properties.getProperty("com.liferay.mail.custom.mailimporter.webcontentfolderid","");
	
	static String DDMSTRUCTUREKEY = properties.getProperty("com.liferay.mail.custom.mailimporter.ddmstructurekey","BASIC-WEB-CONTENT");
	static String DDMTEMPLATEKEY = properties.getProperty("com.liferay.mail.custom.mailimporter.ddmtemplatekey","BASIC-WEB-CONTENT");
	static String CONTENTTEMPLATE = properties.getProperty("com.liferay.mail.custom.mailimporter.contenttemplate","<root available-locales=\"en_US\" default-locale=\"en_US\"><dynamic-element name=\"content\" type=\"text_area\" index-type=\"text\" instance-id=\"tzol\"><dynamic-content language-id=\"en_US\"><![CDATA[<p>$CONTENT$</p>]]></dynamic-content></dynamic-element></root>");
	
	static String ADDATTACHMENT = properties.getProperty("com.liferay.mail.custom.mailimporter.addattachment","false");
	static String ADDCONTENT = properties.getProperty("com.liferay.mail.custom.mailimporter.addcontent","false");
	
    
    public static void checkMail() {
	      try {
	    	  _log.debug("Check mail...");
		      //Set property values
		      Properties mailproperties = new Properties();
		      mailproperties.put("mail.pop3.host", DEFAULT_MAILHOST);
		      mailproperties.put("mail.pop3.port", DEFAULT_MAILPORT);
		      mailproperties.put("mail.pop3.starttls.enable", "true");
		      Session emailSessionObj = Session.getDefaultInstance(mailproperties);  
		      //Create POP3 store object and connect with the server
		      Store storeObj = emailSessionObj.getStore(DEFAULT_MAILPROTOCOL);
		      storeObj.connect(DEFAULT_MAILHOST, USERNAME, PASSWORD);
		      //Create folder object and open it in read-only mode
		      Folder emailFolderObj = storeObj.getFolder("INBOX");
		      emailFolderObj.open(Folder.READ_ONLY);
		      //Fetch messages from the folder and print in a loop
		      Message[] messageobjs = emailFolderObj.getMessages(); 
		 
		      for (int i = 0, n = messageobjs.length; i < n; i++) {
		         Message msg = messageobjs[i];
		         _log.debug("Printing individual messages");
		         _log.debug("No# " + (i + 1));
		         _log.debug("Email Subject: " + msg.getSubject());
		         _log.debug("To: " + msg.getAllRecipients()[0]);
		         _log.debug("Sender: " + msg.getFrom()[0]);
		         _log.debug("Content: " + msg.getContent().toString());
		         
		         
		         if (Boolean.parseBoolean(ADDCONTENT)) {
		        	 addEmail(msg);
		         }
	        	 if (Boolean.parseBoolean(ADDATTACHMENT)) {
	        		 addDocument(msg);
	        	 }
		      }
		      //Now close all the objects
		      emailFolderObj.close(false);
		      storeObj.close();
	      } catch (NoSuchProviderException exp) {
	         exp.printStackTrace();
	      } catch (MessagingException exp) {
	         exp.printStackTrace();
	      } catch (Exception exp) {
	         exp.printStackTrace();
	      }
	   }
    
    private static ServiceContext getServiceContext() {
    	ServiceContext serviceContext = new ServiceContext();

		//serviceContext.setAddGuestPermissions(true);
		serviceContext.setScopeGroupId(Long.parseLong(GROUPID));
		serviceContext.setCompanyId(Long.parseLong(COMPANYID));
		serviceContext.setUserId(Long.parseLong(USERID));
		
		return serviceContext;
    }

	public static void addDocument(Message msg) throws IOException, MessagingException, PortalException {

	    Multipart multipart = (Multipart) msg.getContent();

	    for (int i = 0; i < multipart.getCount(); i++) {
	        BodyPart bodyPart = multipart.getBodyPart(i);
	        if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
	               StringUtils.isBlank(bodyPart.getFileName())) {
	            continue; // dealing with attachments only
	        }
	        
	        _log.debug("Adding file: " + bodyPart.getFileName());
	        
			DLAppLocalServiceUtil.addFileEntry(Long.parseLong(USERID), Long.parseLong(GROUPID), 0, bodyPart.getFileName(), bodyPart.getContentType(), bodyPart.getFileName() + " - " + Instant.now().toEpochMilli(), "no description", "no changelog", bodyPart.getInputStream(), bodyPart.getSize(), getServiceContext());
	    }		
	}
	
	public static void addEmail(Message msg) throws PortalException, MessagingException, IOException {

		_log.debug("Adding mail: " + msg.getSubject());
		
		long userId = Long.parseLong(USERID);
		long groupId = Long.parseLong(GROUPID);
		long folderId = Long.parseLong(WEBCONTENTFOLDERID);
		long classNameId = 0;
		long classPK = 0;
		String articleId = "";
		boolean autoArticleId = true;
		double version = 1.0;
		String content = CONTENTTEMPLATE.replaceAll("\\$CONTENT\\$", getTextFromMessage(msg));
		String ddmStructureKey = DDMSTRUCTUREKEY;
		String ddmTemplateKey = DDMTEMPLATEKEY;
		String layoutUuid = "";
		int displayDateMonth = 1;
		int displayDateDay = 1; 
		int displayDateYear = 2017;
		int displayDateHour = 1;
		int displayDateMinute = 1;
		int expirationDateMonth = 1;
		int expirationDateDay = 1;
		int expirationDateYear = 2017;
		int expirationDateHour = 1;
		int expirationDateMinute = 1;
		boolean neverExpire = true;
		int reviewDateMonth = 1;
		int reviewDateDay = 1;
		int reviewDateYear = 2017;
		int reviewDateHour = 1;
		int reviewDateMinute = 1;
		boolean neverReview = true;
		boolean indexable = false;
		boolean smallImage = false;
		String smallImageURL = null;
		File smallImageFile = null;
		Map<String,byte[]> images = null;
		String articleURL = null;	
		
		Map<Locale, String> titleMap = new HashMap<Locale, String>();
		titleMap.put(Locale.US,msg.getSubject());
		
		Map<Locale, String> descriptionMap = new HashMap<Locale, String>();
		descriptionMap.put(Locale.US,"store here what you like");
		
		JournalArticleLocalServiceUtil.addArticle(userId, 
												  groupId, 
												  folderId, 
												  classNameId, 
												  classPK, 
												  articleId, 
												  autoArticleId, 
												  version, 
												  titleMap, 
												  descriptionMap, 
												  content, 
												  ddmStructureKey, 
												  ddmTemplateKey, 
												  layoutUuid,
												  displayDateMonth, 
												  displayDateDay, 
												  displayDateYear, 
												  displayDateHour, 
												  displayDateMinute, 
												  expirationDateMonth, 
												  expirationDateDay, 
												  expirationDateYear, 
												  expirationDateHour, 
												  expirationDateMinute, 
												  neverExpire, 
												  reviewDateMonth, 
												  reviewDateDay, 
												  reviewDateYear, 
												  reviewDateHour, 
												  reviewDateMinute, 
												  neverReview, 
												  indexable, 
												  smallImage, 
												  smallImageURL, 
												  smallImageFile, 
												  images, 
												  articleURL, 
												  getServiceContext());
	}
	
	private static String getTextFromMessage(Message message)  throws MessagingException, IOException{
		String result = "";
		if (message.isMimeType("text/plain")) {
	        result = message.getContent().toString();
	    } else if (message.isMimeType("multipart/*")) {
	        MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
	        result = getTextFromMimeMultipart(mimeMultipart);
	    }
	    return result;
	}
	
	private static String getTextFromMimeMultipart(
	        MimeMultipart mimeMultipart)  throws MessagingException, IOException{
	    String result = "";
	    int count = mimeMultipart.getCount();
	    for (int i = 0; i < count; i++) {
	        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
	        if (bodyPart.isMimeType("text/plain")) {
	            result = result + "\n" + bodyPart.getContent();
	            break; // without break same text appears twice in my tests
	        } else if (bodyPart.isMimeType("text/html")) {
	            String html = (String) bodyPart.getContent(); 
	            result = result + "\n" + html;
	        } else if (bodyPart.getContent() instanceof MimeMultipart){
	            result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
	        }
	    }
	    return result;
	}
}
