package com.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.drools.core.io.impl.ClassPathResource;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Assert;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.Message.Level;
import org.kie.api.cdi.KSession;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import com.plugtree.training.model.Artist;
import com.plugtree.training.model.Playlist;
import com.plugtree.training.model.Song;

public class SimpleRulesExampleTest {

    private final List<String> firedRules = new ArrayList<String>();

    @Inject
    @KSession("ksession1")
    KieSession ksession;
   
    @Test
    public void simpleRulesThroughCDI() {
    	Weld w = new Weld();
    	WeldContainer wc = w.initialize();
    	SimpleRulesExampleTest test = wc.instance().select(SimpleRulesExampleTest.class).get();
    	KieSession ksession = test.ksession;
        fire(ksession);
    }
    
    @Test
    public void simpleRules() {
    	KieServices kservices = KieServices.Factory.get();
    	KieRepository krepo = kservices.getRepository();
    	KieFileSystem kfileSystem = kservices.newKieFileSystem();
    	kfileSystem.write(new ClassPathResource("com/plugtree/training/rules.drl"));
    	KieBuilder kbuilder = kservices.newKieBuilder(kfileSystem).buildAll();
    	if (kbuilder.getResults().getMessages(Level.ERROR).size() > 0) {
    		for (Message msg : kbuilder.getResults().getMessages(Level.ERROR)) {
    			System.out.println(msg);
    		}
    		throw new IllegalArgumentException("Couldn't read knowledge base");
    	}
    	
    	KieContainer kcontainer = kservices.newKieContainer(krepo.getDefaultReleaseId());
    	KieBase kbase = kcontainer.newKieBase(null);
    	
    	KieSession ksession = kbase.newKieSession();
    	
        fire(ksession);
    }

	private void fire(KieSession ksession) {
		//We add an AgendaEventListener to keep track of fired rules.
        ksession.addEventListener(new DefaultAgendaEventListener(){
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                firedRules.add(event.getMatch().getRule().getName());
            }
        });

        //Creates a single song.
        ksession.insert(createAdagio());
        ksession.fireAllRules();

        Assert.assertEquals(1,firedRules.size());
        //because there are no playlists, the only activated/fired rule
        //is "Songs by Johann Sebastian Bach"
        Assert.assertTrue(firedRules.contains("Warn when we have Songs by Johann Sebastian Bach"));

        firedRules.clear();

        //creates a playlist 
        ksession.insert(createPlaylist());
        ksession.fireAllRules();

        Assert.assertEquals(2,firedRules.size());
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist with more than one song"));
        Assert.assertTrue(firedRules.contains("Warn when we have a POP songs inside a playlist"));

        //"Warn when we have Songs by Johann Sebastian Bach" is not fired again because no new
        //activation occurs. (i.e. no new Bach's song was inserted/updated).

        //"Warn when we have a POP songs and Playlist" is not fired because the existing POP song
        //is inside the playlist. When you insert a "Complex" object, the
        //objects references that it contains are not inserted.
        
        ksession.dispose();
	}

    /**
     * Creates a new playlist with 2 songs.
     * @return the created playlist
     */
    private Playlist createPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setName("My favorite songs");
        
        playlist.addSong(createThriller());
        playlist.addSong(createAdagio());

        return playlist;
    }

    /**
     * Creates a Michael Jackson song ("Thriller").
     * @return the created song.
     */
    private Song createThriller() {
        Song song = new Song("Thriller", Song.Genre.POP,6540);
        song.addArtist(new Artist("Michael", "Jackson"));
        return song;
    }

    /**
     * Creates a Bach song ("Adagio").
     * @return the created song.
     */
    private Song createAdagio() {
        Song song = new Song("Adagio", Song.Genre.CLASSICAL,2561);
        song.addArtist(new Artist("Johann Sebastian", "Bach"));
        return song;
    }
}
