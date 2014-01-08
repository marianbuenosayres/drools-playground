package org.plugtree.training;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.event.KnowledgeRuntimeEventManager;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.logger.KnowledgeRuntimeLoggerFactory;
import org.plugtree.training.model.Artist;
import org.plugtree.training.model.Playlist;
import org.plugtree.training.model.Song;
import org.plugtree.training.model.util.HibernateUtil;

public class AdvancedFromRulesExampleTest {

    private KieSession ksession;

    @Before
    public void setUp() throws Exception {

        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        kfs.write("src/main/resources/AdvancedFromRules.drl", ResourceFactory.newClassPathResource("rules/AdvancedFromRules.drl"));
        KieBuilder kbuilder = ks.newKieBuilder(kfs);
        
        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            System.err.println(kbuilder.getResults());
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KieModule kmodule = kbuilder.getKieModule();
        KieContainer kcontainer = ks.newKieContainer(kmodule.getReleaseId());
        
        ksession = kcontainer.newKieSession();
        KnowledgeRuntimeLoggerFactory.newConsoleLogger((KnowledgeRuntimeEventManager) ksession);
    }
    
    @Test
    public void rulesActivation() {
        List<Playlist> playlists = this.createPlaylists();
        
        Session session = HibernateUtil.getSession();
        session.beginTransaction().begin();

        //Persists the playlists and songs
        for (Playlist playlist : playlists) {
            session.save(playlist);
        }

        //adds Hibernate Session as a global
        ksession.setGlobal("session", session);
        
        //inserts only the playlists.
        for (Playlist playlist : playlists) {
            ksession.insert(playlist);
        }
        
        //adds the two lists needed
        List<String> playListsContainingSongsWithLetterA = new ArrayList<String>();
        ksession.setGlobal("list", playListsContainingSongsWithLetterA);
        List<String> songsFromThe80s = new ArrayList<String>();
        ksession.setGlobal("songsFromThe80s", songsFromThe80s);

        ksession.fireAllRules();

        Assert.assertEquals(1, playListsContainingSongsWithLetterA.size());
        Assert.assertTrue(playListsContainingSongsWithLetterA.contains("A playlist"));

        Assert.assertEquals(3, songsFromThe80s.size());
        Assert.assertTrue(songsFromThe80s.contains("Thriller"));
        Assert.assertTrue(songsFromThe80s.contains("The final countdown"));
        Assert.assertTrue(songsFromThe80s.contains("No For The Inocent"));

        ksession.dispose();
    }

    /**
     * @return the created playlists
     */
    private List<Playlist> createPlaylists() {

        List<Playlist> playlists = new ArrayList<Playlist>();

        //playlist A
        Playlist playlistA = new Playlist();
        playlistA.setName("A playlist");
        playlistA.addSong(this.createTheFinalCountdownSong());
        playlistA.addSong(this.createAdagioSong());
        playlists.add(playlistA);

        //playlist B (the titles of its songs don't contain the 'a' letter
        Playlist playlistB = new Playlist();
        playlistB.setName("B playlist");
        playlistB.addSong(this.createThrillerSong());
        playlistB.addSong(this.createNoForTheInocentSong());
        playlists.add(playlistB);

        return playlists;
    }

    private Song createThrillerSong(){
        Song song = new Song("Thriller", Song.Genre.POP, 6540, 1982);
        song.addArtist(new Artist("Michael", "Jackson"));
        return song;
    }

    private Song createAdagioSong(){
        Song song = new Song("Adagio", Song.Genre.CLASSICAL, 2561, 1708);
        song.addArtist(new Artist("Johann Sebastian", "Bach"));
        return song;
    }

    private Song createTheFinalCountdownSong(){
        Song song = new Song("The final countdown", Song.Genre.ROCK, 300, 1985);
        song.addArtist(new Artist("Joey", "Tempest"));
        song.addArtist(new Artist("John", "Norum"));
        return song;
    }
    
    private Song createNoForTheInocentSong(){
        Song song = new Song("No For The Inocent", Song.Genre.ROCK, 263,1983);
        song.addArtist(new Artist("Gene ", "Simmons"));
        song.addArtist(new Artist("Vinnie", "Vincent"));
        return song;
    }
}
