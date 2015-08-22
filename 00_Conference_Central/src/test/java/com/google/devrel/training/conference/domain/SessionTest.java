/**
 * 
 */
package com.google.devrel.training.conference.domain;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.devrel.training.conference.form.SessionForm;

/**
 * @author zacharilius
 *
 */
public class SessionTest {
	private static final long ID = 123456L;
	
	private static final String WEBSAFE_CONFERENCE_KEY = "ahNjb25mZXJlbmNlLWFwcC0xMDM2ch4LEgdQcm9maWxlIgEwDAsSCkNvbmZlcmVuY2UYCgw"; 
	
    private static final String NAME = "Best session ever";

    private static final int DURATION = 2;
    
    private static final String TYPE_OF_SESSION = "Lecture";
    
    private static final int START_TIME = 12;
    
    private static final String SPEAKER = "firstname lastName";
    
    private List<String> highlights;
    
    private Date date;
	
    private final LocalServiceTestHelper helper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setDefaultHighRepJobPolicyUnappliedJobPercentage(100));
    
    private SessionForm sessionForm; 

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        date = dateFormat.parse("08/25/2015");
        
        highlights = new ArrayList<>();
        highlights.add("Highlight1");
        highlights.add("Highlight2");
        highlights.add("Highlight3");
        
        sessionForm = new SessionForm(NAME,highlights,SPEAKER, DURATION, TYPE_OF_SESSION, date, START_TIME);

        
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	@Test
	public void test() {
		Session session = new Session(ID, WEBSAFE_CONFERENCE_KEY,sessionForm);
		//fail("Not yet implemented");
	}
    @Test
    public void testSession() throws Exception {
        Session session = new Session(ID, WEBSAFE_CONFERENCE_KEY, sessionForm);
        assertEquals(ID, session.getId());
        assertEquals(NAME, session.getName());
        assertEquals(DURATION, session.getDuration());
        assertEquals(TYPE_OF_SESSION, session.getTypeOfSession());
        assertEquals(START_TIME, session.getStartTime());
        assertEquals(SPEAKER, session.getSpeaker());
        assertEquals(highlights, session.getHighlights());
        assertEquals(date, session.getDate());
    }
}
