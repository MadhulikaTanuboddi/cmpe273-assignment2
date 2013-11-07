package edu.sjsu.cmpe.procurement.jobs;



import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;


import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;







/**
 * This job will run at every 5 minutes which is 300 seconds.
 */
@Every("300s")
public class ProcurementSchedulerJob extends Job {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void doJob() throws JMSException {
	String strResponse = ProcurementService.jerseyClient.resource(
		"http://ip.jsontest.com/").get(String.class);
	log.debug("Response from jsontest.com: {}", strResponse);
	
	StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
	factory.setBrokerURI("tcp://" + ProcurementService.host + ":" + ProcurementService.port);

	Connection connection = factory.createConnection(ProcurementService.user, ProcurementService.password);
	connection.start();
	
	//PROCUREMENT GETS FROM QUEUE (lost books)
	Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	Destination dest = new StompJmsDestination(ProcurementService.destination);
    List<Integer> isbnlist = new ArrayList<Integer>();

	MessageConsumer consumer = session.createConsumer(dest);
	System.out.println("[Procuremnt-from-Queue] Waiting for messages from " + ProcurementService.destination + "...");
	long waitUntil = 5000; // wait for 5 sec
	while(true) {
	    Message msg = consumer.receive(waitUntil);
	    if( msg instanceof  TextMessage ) {
	           String body = ((TextMessage) msg).getText();
	           System.out.println("[Procuremnt-from-Queue] Received message = " + body);
	   		int isbn = Integer.parseInt(body.substring(body.lastIndexOf(':')+1));
			isbnlist.add(isbn);
	    } else if (msg == null) {
	          System.out.println("No new messages. Exiting due to timeout - " + waitUntil / 1000 + " sec");
	          break;	
	    } else {
		System.out.println("Unexpected message type: "+msg.getClass());
	    }   
	}
	
    //Procurement Sends HTTP POST to publisher
	try {
		Client client = Client.create();
		WebResource webResource = client
		   .resource("http://54.215.210.214:9000/orders"); //MASTER
		//WebResource webResource = client
		 //  .resource("http://54.219.156.168:9000/orders"); //SLAVE

		String input = "{\"id\":\"89939\",\"order_book_isbns\":" + isbnlist + "}";
		//System.out.println(input);
		if (!isbnlist.isEmpty())
		{
		ClientResponse response = webResource.type("application/json")
		   .post(ClientResponse.class, input);
		
		/*
		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
			     + response.getStatus());
		}
		*/
		System.out.println("[Procurement-to-Publisher] Output from Server ....");
		String output = response.getEntity(String.class);
		System.out.println(output+"\n");
		}
	  } 
	 catch (Exception e) 
	 {
		e.printStackTrace();
	 }
	//end send to publisher

	
	//PROCUREMENT GETS FROM PUBLISHER
	//Get from publisher starts
	Client client = Client.create();
	WebResource webResource = client
		   .resource("http://54.215.210.214:9000/orders/89939"); //MASTER
	//WebResource webResource = client
	//	   .resource("http://54.219.156.168:9000/orders/89939");
	ClientResponse response = webResource.accept("application/json") //SLAVE
                   .get(ClientResponse.class);
	if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : "
			+ response.getStatus());
	}
	String output = response.getEntity(String.class);
	System.out.println("[Publisher-to-Procurement] Shipped Books from Server .... ");
	System.out.println(output + "\n");
	//Get from publisher ends

	//PROCUREMENT TO TOPICS (pubsub)
	Session session1 = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	//Destination topicdestall = new StompJmsDestination(ProcurementService.topicdestination);
	//MessageProducer producer1 = session1.createProducer(dest1);
	//producer1.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		
	try {
		//Parse shipped books into an array
		JSONObject jsonObj1 = new JSONObject(output);
		JSONArray shippedBooks = jsonObj1.getJSONArray("shipped_books");
		
		for (int i=0; i < shippedBooks.length(); i++) {
			//Parse JSON book object to a string
			int isbn = shippedBooks.getJSONObject(i).getInt("isbn");
			String title = shippedBooks.getJSONObject(i).getString("title");
			String category = shippedBooks.getJSONObject(i).getString("category");
			String coverimage = shippedBooks.getJSONObject(i).getString("coverimage");
			String data = isbn + ":\"" + title + "\":\"" + category + "\":\"" + coverimage + "\"";
			//System.out.println(data);
		
			//Post message to topics - all and specific topic
			Destination topicdestall = new StompJmsDestination(ProcurementService.topicdestination);
			String destcategory = "/topic/89939.book." + category;
			Destination topicdestcategory = new StompJmsDestination(destcategory);

			MessageProducer producer1 = session1.createProducer(topicdestall);
			MessageProducer producercategory = session1.createProducer(topicdestcategory);
			producer1.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
			
			TextMessage msg = session1.createTextMessage(data);
			msg.setLongProperty("id", System.currentTimeMillis());
			
			producer1.send(msg);
			producercategory.send(msg);
			System.out.println("[Procurement-to-Topics] Shipped book sent to topics: isbn " + isbn);
			
		/**
		 * Notify all Listeners to shut down. if you don't signal them, they
		 * will be running forever.
		 */
		
		}
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

	/**
	 * Notify all Listeners to shut down. if you don't signal them, they
	 * will be running forever.
	 */
	
	connection.close();
    }
}
