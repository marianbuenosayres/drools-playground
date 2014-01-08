package com.plugtree.training.expert;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.Message;
import org.kie.api.event.KieRuntimeEventManager;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.internal.io.ResourceFactory;

import com.plugtree.training.handsonlabs.enums.ItemType;
import com.plugtree.training.handsonlabs.model.SpecialOffer;
import com.plugtree.training.handsonlabs.model.StockItem;

public class SpreadsheetHandsOnLabsTest  {

    private KieSession ksession;

    @Before
    public void setUp() throws Exception {
    	KieServices ks = KieServices.Factory.get();
    	KieFileSystem kfs = ks.newKieFileSystem();
    	kfs.write("src/main/resources/rules/my-file.drl", ResourceFactory.newClassPathResource("rules/HandsOnLabsRules.drl"));

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
    }
    
    @Test
    public void testExecution() {
    	
    	//Regular prices
    	Double samsung_led_price = 800d;
    	Double samsung_lcd_price = 400d;
    	Double samsung_tube_price = 200d;
    	
    	Double sony_led_price = 900d;
    	Double sony_lcd_price = 500d;
    	
    	
    	//TVs
    	StockItem samsungLEDTV1 = new StockItem(ItemType.LED_TV,"Samsung");
    	StockItem samsungLEDTV2 = new StockItem(ItemType.LED_TV,"Samsung");
    	
    	StockItem samsungLCDTV1 = new StockItem(ItemType.LCD_TV,"Samsung");
    	
    	StockItem samsungTUBETV1 = new StockItem(ItemType.TUBE_TV,"Samsung");
    	
    	StockItem sonyLEDTV1 = new StockItem(ItemType.LED_TV,"Sony");
    	
    	StockItem sonyLCDTV1 = new StockItem(ItemType.LCD_TV,"Sony");
    	StockItem sonyLCDTV2 = new StockItem(ItemType.LCD_TV,"Sony");
    	
    	//PROMOTIONAL PRICES
    	SpecialOffer samsungLCDSpecialOffer = new SpecialOffer(ItemType.LCD_TV, "Samsung", 300d); 
    	
    	SpecialOffer sonyLEDSpecialOffer = new SpecialOffer(ItemType.LED_TV, "Sony", 850d);
    	
    	//Insert promotional prices as globals
    	ksession.setGlobal("samsung_led_price", samsung_led_price);
    	ksession.setGlobal("samsung_lcd_price", samsung_lcd_price);
    	ksession.setGlobal("samsung_tube_price", samsung_tube_price);
    	ksession.setGlobal("sony_led_price", sony_led_price);
    	ksession.setGlobal("sony_lcd_price", sony_lcd_price);
        
    	//Insert Special prices as facts
    	ksession.insert(samsungLCDSpecialOffer);
    	ksession.insert(sonyLEDSpecialOffer);
    	
    	//Insert items as facts
        ksession.insert(samsungLEDTV1);
        ksession.insert(samsungLEDTV2);
        ksession.insert(samsungLCDTV1);
        ksession.insert(samsungTUBETV1);
        ksession.insert(sonyLEDTV1);
        ksession.insert(sonyLCDTV1);
        ksession.insert(sonyLCDTV2);
        
        //fire all rules
        ksession.fireAllRules();
        
        
        //Control
        
        //SAMSUNG
        Assert.assertEquals("",samsung_led_price.doubleValue(), samsungLEDTV1.getPrice(),0.1);
        Assert.assertEquals("",samsung_led_price.doubleValue(), samsungLEDTV2.getPrice(),0.1);

        Assert.assertEquals("",samsungLCDSpecialOffer.getPrice(), samsungLCDTV1.getPrice(),0.1);
        
        Assert.assertEquals("",samsung_tube_price.doubleValue(), samsungTUBETV1.getPrice(),0.1);
        
        //SONY
        Assert.assertEquals("",sonyLEDSpecialOffer.getPrice(), sonyLEDTV1.getPrice(),0.1);
        
        Assert.assertEquals("",sony_lcd_price.doubleValue(), sonyLCDTV1.getPrice(),0.1);
        Assert.assertEquals("",sony_lcd_price.doubleValue(), sonyLCDTV2.getPrice(),0.1);
        
        
        ksession.dispose();
    }
}
