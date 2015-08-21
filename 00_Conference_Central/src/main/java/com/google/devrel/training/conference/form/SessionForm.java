package com.google.devrel.training.conference.form;

import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.annotation.Id;


/**
 * POJO class for the Session object
 * @author zacharilius
 *
 */
public class SessionForm {
	
    // The name of the session.
    private String name;
    
    // The session highlights
    private List<String> highlights;
    
    // The speaker's name
    private String speaker;
    
    // The duration in hours of the session
    private int duration;
    
    // The type of session
    private String typeOfSession;
    
    // The date the session occurs
    private Date date;
    
    // The start time of the session
    private int startTime;
    
    public SessionForm(){}
	/**
	 * @param name
	 * @param highlights
	 * @param speaker
	 * @param duration
	 * @param type
	 * @param date
	 * @param startTime
	 */
	public SessionForm(String name, List<String> highlights, String speaker, 
			int duration, String typeOfSession, Date date, int startTime) {
		this.name = name;
		this.highlights = highlights;
		this.speaker = speaker;
		this.duration = duration;
		this.typeOfSession = typeOfSession;
		this.date = date;
		this.startTime = startTime;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the highlights
	 */
	public List<String> getHighlights() {
		return highlights;
	}
	
	/**
	 * @ return the session speaker's name
	 */
	public String getSpeaker(){
		return speaker;
	}
	/**
	 * @return the duration
	 */
	public int getDuration() {
		return duration;
	}

	/**
	 * @return the type
	 */
	public String getTypeOfSession() {
		return typeOfSession;
	}

	/**
	 * @return the date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @return the startTime
	 */
	public int getStartTime() {
		return startTime;
	}



}
