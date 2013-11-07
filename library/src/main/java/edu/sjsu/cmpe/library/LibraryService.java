
package edu.sjsu.cmpe.library;



import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import edu.sjsu.cmpe.library.api.resources.BookResource;
import edu.sjsu.cmpe.library.api.resources.RootResource;
import edu.sjsu.cmpe.library.config.LibraryServiceConfiguration;
import edu.sjsu.cmpe.library.domain.Book;
import edu.sjsu.cmpe.library.domain.Book.Status;
import edu.sjsu.cmpe.library.repository.BookRepository;
import edu.sjsu.cmpe.library.repository.BookRepositoryInterface;
import edu.sjsu.cmpe.library.ui.resources.HomeResource;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.json.JSONException;


public class LibraryService extends Service<LibraryServiceConfiguration> 
{

    private final Logger log = LoggerFactory.getLogger(getClass());
    //Declaring all the Apollo client variables here
    public static String apolloUser;
    public static String apolloPassword;
    public static String apolloHost;
    public static String apolloPort;
    public static String libraryName;
    public static String queueName;
    public static String topicName;

    public static String user;
	public static String password;
	public static String host;
	public static int port;
	public static String destination;
	public static String destination1;
	
	public static BookRepositoryInterface bookRepository = new BookRepository();
	
//MAIN METHOD
    public static void main(String[] args) throws Exception 
    {
	new LibraryService().run(args);
	
	//Background - listener
	int numThreads = 1;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    Runnable backgroundTask = new Runnable() {
	
    @Override
	public void run() 
    {
		try 
		{
			listener();
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    };
    executor.execute(backgroundTask);
    }

    @Override
    public void initialize(Bootstrap<LibraryServiceConfiguration> bootstrap) {
	bootstrap.setName("library-service");
	bootstrap.addBundle(new ViewBundle());
	bootstrap.addBundle(new AssetsBundle());
    }

    @Override
    public  void run(LibraryServiceConfiguration configuration,
	    Environment environment) throws Exception {
	
    	// This is how you pull the configurations from library_x_config.yml
    	apolloUser = configuration.getApolloUser();
        apolloPassword = configuration.getApolloPassword();
        apolloHost = configuration.getApolloHost();
        apolloPort = configuration.getApolloPort();
        libraryName = configuration.getLibraryName();
        queueName = configuration.getStompQueueName();
	    topicName = configuration.getStompTopicName();
	    log.debug("{} - Queue name is {}. Topic name is {}",
	    		libraryName, queueName, topicName);
	
	
	// TODO: Apollo STOMP Broker URL and login
	    user = env("APOLLO_USER", apolloUser);
		password = env("APOLLO_PASSWORD", apolloPassword);
		host = env("APOLLO_HOST", apolloHost);
		port = Integer.parseInt(env("APOLLO_PORT", apolloPort));
		destination = configuration.getStompQueueName();
		destination1=topicName;


	//Root API 
	environment.addResource(RootResource.class);
	//Books APIs 
    //public static BookRepositoryInterface bookRepository = new BookRepository();
	environment.addResource(new BookResource(bookRepository));

	//UI Resources
	environment.addResource(new HomeResource(bookRepository));
    }
    
    //Listener Method
    public static void listener() throws JMSException, JSONException{
    	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
    	factory.setBrokerURI("tcp://" + host + ":" + port);

    	Connection connection = factory.createConnection(user, password);
    	connection.start();
    	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	Destination dest = new StompJmsDestination(destination1);

    	MessageConsumer consumer = session.createConsumer(dest);
    	System.currentTimeMillis();
    	System.out.println("Waiting for messages (Listener)...");
    	while(true) {
    	    Message msg = consumer.receive();
    	    if( msg instanceof  TextMessage ) 
    	    {
	    		String body = ((TextMessage) msg).getText();
	    		System.out.println("[Topics-to-Listener]Received message = " + body);
	    		
	    		//Parse message
	    		String[] str_array = body.split(":\"");
	    		int isbn = Integer.parseInt(str_array[0]);
				String title = str_array[1].substring(0, str_array[1].length()-1);
				String category = str_array[2].substring(0, str_array[2].length()-1);
				String coverimage = str_array[3].substring(0, str_array[3].length()-1);
				  //String data = isbn + " " + title + " " + category + " " + coverimage;
				  //System.out.println(data);
				
				//Update Library		
				Book book = new Book();
				book.setIsbn((long) isbn);
				book.setCategory(category);
				book.setTitle(title);
				try {
				    book.setCoverimage(new URL(coverimage));
				} catch (MalformedURLException e) {
				    // eat the exception
				}
				
				if (bookRepository.getBookByISBN((long) isbn) == null)
				{		
					bookRepository.saveBookByISBN(book,(long)isbn);
					System.out.println("[Listener-Update] Created new book successfully: isbn -" + isbn);
				} else { 
				     Status status = Status.available;
					bookRepository.getBookByISBN((long)isbn).setStatus(status);
					System.out.println("[Listener-Update] Updated book availability successfully: isbn -" + isbn);
				}
    		}
    	    else if (msg instanceof StompJmsMessage) {
    		StompJmsMessage smsg = ((StompJmsMessage) msg);
    		String body = smsg.getFrame().contentAsString();
    		System.out.println("[Topics-to-Listener]Received message = " + body);
    	    }
    		else {
    		System.out.println("Unexpected message type: "+msg.getClass());
    	    }
    	 }
    	
    	//connection.close();
    	}
    	
    	
    private static String env(String key, String defaultValue) {
    	String rc = System.getenv(key);
    	if( rc== null ) {
    	    return defaultValue;
    	}
    	return rc;
        }
}

