package com.plugtree.training.backend.server;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.jboss.errai.bus.server.annotations.Service;

import com.plugtree.training.api.shared.DemoServiceEntryPoint;

@Service
@ApplicationScoped
public class DemoServiceEntryPointImpl implements DemoServiceEntryPoint {

    private List<String> messages = new ArrayList<String>();
    
    @PostConstruct
    public void init() {
        
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }

    @Override
    public void sendMessage(String message) {
        messages.add(message);
    }
}
