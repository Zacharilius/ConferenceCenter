package com.google.devrel.training.conference.domain;

import java.util.Date;
import java.util.List;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.search.checkers.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.devrel.training.conference.form.SessionForm;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
/**
 * Class for storing session information
 * @author zacharilius
 *
 */
@Entity
@Cache
public class Session {
	private static final List<String> DEFAULT_HIGHLIGHTS = ImmutableList.of("Default", "Highlight");
	
	
	// Id assigned through app engine
	@Id private long id;

    // The userId of the session speaker.
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private String sessionConferenceId;
    
    // The parent key
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Conference> conferenceKey;
    
    
    
    
    // The name of the session.
    private String name;
    
    // The session highlights
    private List<String> highlights;

    // The name of the session's speaker
    private String speaker;
    
    // The duration in hours of the session
    private int duration;
    
    // The type of session
    private String typeOfSession;
    
    // The date the session occurs
    private Date date;
    
    // The start time of the session
    private int startTime;
     
	
	public Session(){}
	
	public Session(final long id, final String sessionConferenceId, 
			final SessionForm sessionForm){
		Preconditions.checkNotNull(sessionForm.getName(), "Session name required");
		this.id = id;
		this.sessionConferenceId = sessionConferenceId;
		updateWithSessionForm(sessionForm);
		
	}
	public void updateWithSessionForm(SessionForm sessionForm){
		this.name = sessionForm.getName();
		
		List<String> highlights = sessionForm.getHighlights();
		this.highlights = highlights == null || highlights.isEmpty() ? DEFAULT_HIGHLIGHTS : highlights;	
		
		this.speaker = sessionForm.getSpeaker();
		
		this.duration = sessionForm.getDuration();
		
		this.typeOfSession = sessionForm.getTypeOfSession();
		
		Date date = sessionForm.getDate();
		this.date = date == null ? null : new Date(date.getTime());
		
		this.startTime = sessionForm.getStartTime();		
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
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
	 * @return the sessionConferenceId
	 */
	public String getSessionConferenceId() {
		return sessionConferenceId;
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

	/**
	 * @return the conferenceKey
	 */
	public Key<Conference> getConferenceKey() {
		return conferenceKey;
	}
}
