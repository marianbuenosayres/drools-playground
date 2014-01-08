package org.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.event.KieRuntimeEventManager;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.plugtree.training.model.Artist;
import org.plugtree.training.model.Playlist;
import org.plugtree.training.model.Song;

public class EvalRulesExampleTest {

    private KieSession ksession;
    private final List<String> firedRules = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write("src/main/resources/rules/EvalRules.drl", ResourceFactory.newClassPathResource("rules/EvalRules.drl"));
    	
    	KieBuilder kbuilder = ks.newKieBuilder(kfs);
    	System.out.println("Compiling resources");
    	kbuilder.buildAll();

        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            System.err.println(kbuilder.getResults());
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KieModule kmodule = kbuilder.getKieModule();
        KieContainer kcontainer = ks.newKieContainer(kmodule.getReleaseId());
        
        ksession = kcontainer.newKieSession();
        ks.getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);

        ksession.addEventListener(new DefaultAgendaEventListener() {
        	@Override
        	public void afterMatchFired(AfterMatchFiredEvent event) {
        		firedRules.add(event.getMatch().getRule().getName());
        	}
        });
    }
    @Test
    public void rulesActivation() {

        ksession.insert(this.createLongPlaylist());
        ksession.insert(this.createShortPlaylist());
        ksession.fireAllRules();

        Assert.assertEquals(2, firedRules.size());
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist with more than two songs AND containing 'Thriller'"));
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist longer than 9000 seconds"));

        ksession.dispose();
    }

    /**
     * Creates a new playlist with 3 songs.
     * @return the created playlist
     */
    private Playlist createLongPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setName("My favorite songs");


        Song song = new Song("Thriller", Song.Genre.POP, 6540, 1982);
        song.addArtist(new Artist("Michael", "Jackson"));
        playlist.addSong(song);

        song = new Song("Adagio", Song.Genre.CLASSICAL, 2561, 1708);
        song.addArtist(new Artist("Johann Sebastian", "Bach"));
        playlist.addSong(song);

        song = new Song("The final countdown", Song.Genre.ROCK, 300, 1985);
        song.addArtist(new Artist("Joey", "Tempest"));
        song.addArtist(new Artist("John", "Norum"));
        playlist.addSong(song);



        return playlist;
    }

    private Playlist createShortPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setName("Rock songs");

        Song song = new Song("The final countdown", Song.Genre.ROCK, 300, 1985);
        song.addArtist(new Artist("Joey", "Tempest"));
        song.addArtist(new Artist("John", "Norum"));
        playlist.addSong(song);

        return playlist;
    }
}
