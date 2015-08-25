package com.google.devrel.training.conference.servlet;

import static com.google.devrel.training.conference.service.OfyService.ofy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.base.Joiner;
import com.google.devrel.training.conference.Constants;
import com.google.devrel.training.conference.domain.Conference;
import com.google.devrel.training.conference.domain.Session;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;

/**
 * Servlet implementation class AddMultiSpeakerMemcacheServlet
 */
public class AddMultiSpeakerMemcacheServlet extends HttpServlet {
       
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("\n\n\n\tAddMultiSpeakerMemcacheServlet\n\n\n");
		
		// Get the conference Key
		String conferenceKeyString = request.getParameter("conferenceKey");
		// Get the speaker name
		String speakerName = request.getParameter("speakerName");
		// Create a conferenceKey
		if(conferenceKeyString == null || speakerName == null){
			System.out.println("\n\tcaught the nulls\n");
		}
		else{
			Key<Conference> conferenceKey = Key.create(conferenceKeyString);
			
			// Query for all session children of given conference
			Iterable<Session> query = ofy().load().type(Session.class).ancestor(conferenceKey).order("name").filter("speaker =", speakerName);
			
			// List object to store conference names
			List<String> sessionStrings = new ArrayList<>(0);
			for(Session session : query){
				sessionStrings.add(session.getName());
			}

			
			if(sessionStrings.size() > 1){				
				StringBuilder speakerStringBuilder = new StringBuilder(
	                    speakerName + " has the following speaking engagements: ");
	            Joiner joiner = Joiner.on(", ").skipNulls();
	            speakerStringBuilder.append(joiner.join(sessionStrings));
	            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService();
	            memcacheService.put(Constants.MEMCACHE_MORE_THAN_1_SPEAKER_KEY,
	            		speakerStringBuilder.toString());
				System.out.println("\n\nUpdating Memcache\n\n" + speakerStringBuilder.toString());
			}
		}

        response.setStatus(204);
	}
}
