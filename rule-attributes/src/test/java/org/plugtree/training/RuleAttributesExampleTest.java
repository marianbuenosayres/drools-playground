package org.plugtree.training;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
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

public class RuleAttributesExampleTest {

    private KieSession createKieSession(String drlFile, final List<String> firedRules) throws Exception {
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write("src/main/resources/rules/my-file.drl", ResourceFactory.newClassPathResource(drlFile));
    	
    	KieBuilder kbuilder = ks.newKieBuilder(kfs);
    	System.out.println("Compiling resources");
    	kbuilder.buildAll();

        if (kbuilder.getResults().hasMessages(Message.Level.ERROR)) {
            System.err.println(kbuilder.getResults());
            throw new IllegalArgumentException("Could not parse knowledge.");
        }
        KieModule kmodule = kbuilder.getKieModule();
        KieContainer kcontainer = ks.newKieContainer(kmodule.getReleaseId());
        
        KieSession ksession = kcontainer.newKieSession();
        ks.getLoggers().newConsoleLogger((KieRuntimeEventManager) ksession);
    	
        if (firedRules != null) {
            ksession.addEventListener(new DefaultAgendaEventListener() {
                @Override
                public void afterMatchFired(AfterMatchFiredEvent event) {
                    firedRules.add(event.getMatch().getRule().getName());
                }
            });
        }
        return ksession;
    }
    
    @Test
    public void noLoop() throws Exception {

        Playlist pl = this.createFullPlaylist();

        KieSession ksession = this.createKieSession("rules/NoLoopRules.drl", null);

        ksession.setGlobal("index", new AtomicInteger(1));

        for (Song song : pl.getSongs()) {
            System.out.println(song.getName());
            ksession.insert(song);
        }


        ksession.fireAllRules();

        for (Song song : pl.getSongs()) {
            System.out.println(song.getName());
        }

        ksession.dispose();

    }
    @Test
    public void lockOnActive() throws Exception {

        Playlist pl = this.createFullPlaylist();

        KieSession ksession = this.createKieSession("rules/LockOnActiveRules.drl", null);

        ksession.setGlobal("index", new AtomicInteger(1));

        for (Song song : pl.getSongs()) {
            System.out.println(song.getName());
            ksession.insert(song);
        }


        ksession.fireAllRules();

        for (Song song : pl.getSongs()) {
            System.out.println(song.getName());
        }

        ksession.dispose();

    }
    @Test
    public void agendaGroup() throws Exception {


        List<String> firedRules = new ArrayList<String>();

        KieSession ksession = this.createKieSession("rules/AgendaGroupRules.drl", firedRules);

        Playlist playlist = new Playlist("Good playlist");
        ksession.setGlobal("index", new AtomicInteger(1));

        ksession.insert(playlist);
        ksession.insert(createThrillerSong());
        ksession.insert(createAdagioSong());
        ksession.insert(createTheFinalCountdownSong());

        ksession.fireAllRules();

        //No rules were fired because the MAIN agenda-group is empty. We
        //need to manually set the focus in an agenda-group.
        Assert.assertTrue(firedRules.isEmpty());

        Assert.assertTrue(playlist.isEmpty());
        Assert.assertFalse(playlist.isPlaying());

        //This way we set the focus in an agenda-group
        ksession.getAgenda().getAgendaGroup("Create Playlist").setFocus();
        ksession.fireAllRules();

        //3 rules x 3 songs = 9 + "Playlist ready" + "Play playlist" = 11
        Assert.assertEquals(11,firedRules.size());
        //The salience of the rules guarantees the excecution order
        Assert.assertTrue(firedRules.get(0).equals("Classify Songs"));
        Assert.assertTrue(firedRules.get(1).equals("Classify Songs"));
        Assert.assertTrue(firedRules.get(2).equals("Classify Songs"));
        Assert.assertTrue(firedRules.get(3).equals("Number Songs"));
        Assert.assertTrue(firedRules.get(4).equals("Number Songs"));
        Assert.assertTrue(firedRules.get(5).equals("Number Songs"));
        Assert.assertTrue(firedRules.get(6).equals("Fill playlist"));
        Assert.assertTrue(firedRules.get(7).equals("Fill playlist"));
        Assert.assertTrue(firedRules.get(8).equals("Fill playlist"));
        Assert.assertTrue(firedRules.get(9).equals("Playlist ready"));
        Assert.assertTrue(firedRules.get(10).equals("Play playlist"));

        Assert.assertEquals(3, playlist.getSongs().size());
        Assert.assertTrue(playlist.isPlaying());


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

    private Song createThrillerSong() {
        Song song = new Song("Thriller", Song.Genre.POP, 6540, 1982);
        song.addArtist(new Artist("Michael", "Jackson"));


        return song;


    }

    private Song createAdagioSong() {
        Song song = new Song("Adagio", Song.Genre.CLASSICAL, 2561, 1708);
        song.addArtist(new Artist("Johann Sebastian", "Bach"));


        return song;


    }

    private Song createTheFinalCountdownSong() {
        Song song = new Song("The final countdown", Song.Genre.ROCK, 300, 1985);
        song.addArtist(new Artist("Joey", "Tempest"));
        song.addArtist(new Artist("John", "Norum"));


        return song;

    }
}
