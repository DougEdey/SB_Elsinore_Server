package com.sb.elsinore.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity

public class SystemSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private boolean recorderEnabled = true;
    private String scale = "C";
    private Boolean restoreState = false;
    private String owfsServer = "localhost";
    private int owfsPort = 4304;
    private Boolean owfsEnabled = false;
    private String breweryName = "Elsinore";
    private int serverPort = 8080;
    private String theme = "default";

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isRecorderEnabled() {
        return this.recorderEnabled;
    }

    public void setRecorderEnabled(boolean recorderEnabled) {
        this.recorderEnabled = recorderEnabled;
    }

    public String getScale() {
        return this.scale;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public boolean isRestoreState() {
        return this.restoreState;
    }

    public void setRestoreState(boolean restoreState) {
        this.restoreState = restoreState;
    }

    public String getOwfsServer() {
        return this.owfsServer;
    }

    public void setOwfsServer(String owfsServer) {
        this.owfsServer = owfsServer;
    }

    public int getOwfsPort() {
        return this.owfsPort;
    }

    public void setOwfsPort(int owfsPort) {
        this.owfsPort = owfsPort;
    }

    public boolean isOwfsEnabled() {
        return this.owfsEnabled;
    }

    public void setOwfsEnabled(boolean owfsEnabled) {
        this.owfsEnabled = owfsEnabled;
    }

    public String getBreweryName() {
        return this.breweryName;
    }

    public void setBreweryName(String breweryName) {
        this.breweryName = breweryName;
    }

    public int getServerPort() {
        return this.serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public String getTheme() {
        return this.theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
