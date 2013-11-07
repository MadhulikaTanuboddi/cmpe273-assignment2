package edu.sjsu.cmpe.library.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

public class LibraryServiceConfiguration extends Configuration {
    @NotEmpty
    @JsonProperty
    private String stompQueueName;

    @NotEmpty
    @JsonProperty
    private String stompTopicName;

    @NotEmpty
    @JsonProperty
    private String libraryName;
    
    @NotEmpty
    @JsonProperty
    private String apolloUser;
    
    @NotEmpty
    @JsonProperty
    private String apolloPassword;
    
    @NotEmpty
    @JsonProperty
    private String apolloHost;
    
    @NotEmpty
    @JsonProperty
    private String apolloPort;
   
    
    /**
     * @return the stompQueueName
     */
    public String getStompQueueName() {
	return stompQueueName;
    }

    /**
     * @param stompQueueName
     *            the stompQueueName to set
     */
    public void setStompQueueName(String stompQueueName) {
	this.stompQueueName = stompQueueName;
    }

    /**
     * @return the stompTopicName
     */
    public String getStompTopicName() {
	return stompTopicName;
    }

    /**
     * @param stompTopicName
     *            the stompTopicName to set
     */
    public void setStompTopicName(String stompTopicName) {
	this.stompTopicName = stompTopicName;
    }

    /**
     * @return the libraryName
     */
    public String getLibraryName() {
	return libraryName;
    }

    /**
     * @param libraryName
     *            the libraryName to set
     */
    public void setLibraryName(String libraryName) {
	this.libraryName = libraryName;
    }
    
    /**
     * @return the ApolloUserName
     */
    public String getApolloUser() {
	return apolloUser;
    }

    /**
     * @param ApolloUserName
     *            the ApolloUserName to set
     */
    public void setApolloUser(String apolloUser) {
	this.apolloUser = apolloUser;
    }
    
    /**
     * @return the ApolloPassword
     */
    public String getApolloPassword() {
    	return apolloPassword;
        }

        /**
         * @param ApolloPassword
         *            the ApolloPassword to set
         */
    public void setApolloPassword(String apolloPassword) {
	this.apolloPassword = apolloPassword;
    }
    
    /**
     * @return the ApolloHost
     */
    public String getApolloHost() {
    	return apolloHost;
        }

        /**
         * @param apolloHost
         *            the apolloHost to set
         */
    public void setApolloHost(String apolloHost) {
	this.apolloHost = apolloHost;
    }
    
    /**
     * @return the ApolloPort
     */
    public String getApolloPort() {
    	return apolloPort;
        }

        /**
         * @param apolloPort
         *            the apolloPort to set
         */
    public void setApolloPort(String apolloPort) {
	this.apolloPort = apolloPort;
    }
   
   
}
