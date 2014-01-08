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

public class ExistsRulesExampleTest  {

    private KieSession ksession;
    private final List<String> firedRules = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
    	
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write("src/main/resources/rules/ExistsRules.drl", ResourceFactory.newClassPathResource("rules/ExistsRules.drl"));
    	
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
        
        ksession.addEventListener(new DefaultAgendaEventListener() {
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                firedRules.add(event.getMatch().getRule().getName());
            }
        });
    }
    @Test
    public void testRulesActivation() {

        ksession.insert(this.createLongPlaylist());
        ksession.insert(this.createShortPlaylist());
        ksession.fireAllRules();

        Assert.assertEquals(3, firedRules.size());

        //Even when we have 2 playlists with Michael Jackson's songs this rule
        //is activated just once. 
        Assert.assertTrue(firedRules.contains("Warn when we have a Michael Jackson's song is in a playlist"));

        Assert.assertTrue(firedRules.contains("Remove playlists with more than two songs"));

        //This rule was fired after "Remove playlists with more than two songs"
        //retracted the 3 songs playlist.
        Assert.assertTrue(firedRules.contains("Warn when there are no playlists with more than two songs in my world"));
        
        ksession.dispose();
    }

    /**
     * Creates a new playlist with 2 songs.
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

        song = new Song("Thriller", Song.Genre.POP, 6540, 1982);
        song.addArtist(new Artist("Michael", "Jackson"));
        playlist.addSong(song);

        return playlist;
    }
}
