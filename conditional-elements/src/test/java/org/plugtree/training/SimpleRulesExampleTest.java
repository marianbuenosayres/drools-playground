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
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;
import org.plugtree.training.model.Artist;
import org.plugtree.training.model.Playlist;
import org.plugtree.training.model.Song;

public class SimpleRulesExampleTest  {

    private KieSession ksession;
    private final List<String> firedRules = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
       

    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write("src/main/resources/rules/SimpleRules.drl", ResourceFactory.newClassPathResource("rules/SimpleRules.drl"));
    	
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
        
        // We can Activate the Runtime Logger to see what is happening inside the Engine
        //ks.getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);

        ksession.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                firedRules.add(event.getMatch().getRule().getName());
            }
        });
    }
    @Test
    public void rulesActivation() {

        Playlist playlist = this.createFullPlaylist();

        ksession.insert(playlist);
        ksession.fireAllRules();

        Assert.assertEquals(2,firedRules.size());
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist with more than two songs AND containing 'Thriller'"));
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist with one song OR containing 'Thriller'"));
        firedRules.clear();

        ksession.insert(createPlaylistWithOneSong());
        ksession.fireAllRules();
        Assert.assertEquals(1,firedRules.size());
        Assert.assertTrue(firedRules.contains("Warn when we have a Playlist with one song OR containing 'Thriller'"));

        ksession.dispose();
    }

    /**
     * Creates a new playlist with 3 songs.
     * @return the created playlist
     */
    private Playlist createFullPlaylist() {
        Playlist playlist = new Playlist();
        playlist.setName("My favorite songs");
        
        playlist.addSong(createThrillerSong());
        playlist.addSong(createAdagioSong());
        playlist.addSong(createTheFinalCountdownSong());

        return playlist;
    }

    /**
     * Creates a new playlist containing just one song: "The final countdown"
     * @return a new playlist containing just one song: "The final countdown"
     */
    private Playlist createPlaylistWithOneSong() {
        Playlist playlist = new Playlist();
        playlist.setName("Single song playlist");
        
        playlist.addSong(createTheFinalCountdownSong());

        return playlist;
    }

    /**
     * Creates a new playlist containing just one song: "Thriller"
     * @return a new playlist containing just one song: "Thriller"
     */
    private Song createThrillerSong(){
        Song song = new Song("Thriller", Song.Genre.POP, 6540, 1982);
        song.addArtist(new Artist("Michael", "Jackson"));
        return song;
    }

    /**
     * Creates a new playlist containing just one song: "Adagio"
     * @return a new playlist containing just one song: "Adagio"
     */
    private Song createAdagioSong(){
        Song song = new Song("Adagio", Song.Genre.CLASSICAL, 2561, 1708);
        song.addArtist(new Artist("Johann Sebastian", "Bach"));
        return song;
    }

    /**
     * Creates a new playlist containing just one song: "The final countdown"
     * @return a new playlist containing just one song: "The final countdown"
     */
    private Song createTheFinalCountdownSong(){
        Song song = new Song("The final countdown", Song.Genre.ROCK, 300, 1985);
        song.addArtist(new Artist("Joey", "Tempest"));
        song.addArtist(new Artist("John", "Norum"));
        return song;
    }
}
